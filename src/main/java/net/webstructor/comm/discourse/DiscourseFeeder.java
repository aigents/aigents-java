/*
 * MIT License
 * 
 * Copyright (c) 2005-2020 by Anton Kolonin, Aigents®
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.webstructor.comm.discourse;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.al.AL;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Environment;
import net.webstructor.data.LangPack;
import net.webstructor.data.SocialFeederHelper;
import net.webstructor.util.JSON;

class DiscourseFeeder extends SocialFeederHelper {
	Discourse api;
	boolean debug = true;

	public DiscourseFeeder(Environment body, Discourse api, String user_id, LangPack langPack, Date since, Date until, String[] areas) {
		super(body,api,user_id,langPack,since,until,areas,0);
		this.api = api;
	}
	
	public boolean opendata() {
		return true;
	}

	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		api.sync(since, until);
		try {
			int offset = 0;//offset - index starting from 0, in chunks by 30	
			String api_url = api.getUrl();
			if (!api_url.endsWith("/"))
				api_url += "/";
			
			for (boolean days_over = false; !days_over;) {
				String url = api_url + "user_actions.json?username="+user_id+"&filter=1,2,3,4,5,6,7&offset=" + offset;
				//if (debug) body.debug("Spidering peer discourse request "+url);
				String response = HTTP.simple(url,null,"GET",0,null,null);
				//if (debug) body.debug("Spidering peer discourse response "+user_id+" "+response);
				if (AL.empty(response))
					break;
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray actions = JSON.getJsonArray(json,"user_actions");
				if (actions == null || actions.size() == 0)
					break;
				offset += actions.size();//increment offset for the next trial
				for (int i = 0; i < actions.size(); i++) {
					DiscourseItem a = new DiscourseItem(actions.getJsonObject(i));
					if (!a.visible)
						continue;
					if (a.created_at.compareTo(since) < 0){
						days_over = true;
						break;
					}
//TODO: user names
//TODO: ensure dates are counted according to like dates, not post dates for Steemit as well!?
					switch (a.action_type) {
					case 1: //1 - liked by me
						//this.countMyLikes(a.target_username, a.target_name);
						processVote(a.permlink,user_id,a.target_username);
						break;
					case 2: //2 - liked by other
						//this.countLikes(a.acting_username, a.acting_name, a.created_at);
						processVote(a.permlink,a.acting_username,user_id);
						break;
					case 3: //3 - unknown TODO?
						break;
					case 4: //4 - my topic posts
						//ArrayList links = new ArrayList();
						//text = parsePost(a.title,a.excerpt,links);
						processPost(a.created_at,a.permlink,a.acting_username,a.parent_permlink,null,a.title,a.text);
						if (debug) body.debug("Spidering peer discourse post "+a.acting_username+":"+a.text);
						Collection<DiscourseItem> replies = api.getReplies(a.topic_id);
						if (replies != null) for (DiscourseItem p : replies){
							if (p.created_at.compareTo(since) < 0)
								break;
							if (!p.acting_username.equals(a.acting_username))//if not my own
								processPost(p.created_at,p.permlink,p.acting_username,a.permlink,a.acting_username,null,a.text);
						}
						break;
					case 5: //5 - my reply posts
						//text = parsePost(null,a.excerpt,null);
						processPost(a.created_at,a.permlink,a.acting_username,a.parent_permlink,a.target_username,null,a.text);
						if (debug) body.debug("Spidering peer discourse post "+a.acting_username+":"+a.text);
						break;
					case 6: //6 - reply posts on my reply posts (except reply posts on my topic post) TODO workaround?
						//this.countComments(id, name, message, time);
						processPost(a.created_at,a.permlink,a.acting_username,a.parent_permlink,a.target_username,null,a.text);
						if (debug) body.debug("Spidering peer discourse post "+a.acting_username+":"+a.text);
						break;
//TODO: mentions for Steemit as well 
					case 7: //7 - mentions of me
						//text = parsePost(null,a.excerpt,null);
						//topics.add(a.topic_id);
						break;
					}
				}
			}
		} catch (Exception e) {
			body.error("Spidering peer discourse "+user_id, e);
		}
		
		this.detail = detail;//TODO fix hack needed to details to appear, need to StringBuilder detail from SocialFeeder!
		postProcessPosts(api.getUrl());
	}

	@Override
	protected String base_url() {
		return api.getUrl();
	}
	
	@Override
	protected String permlink_url(String base_url, String parent_permlink, String author, String permlink) {
		//https://community.singularitynet.io/t/towards-heternet-and-hetermedia/2831/2
		return base_url + "t/" + permlink;
	}
}
