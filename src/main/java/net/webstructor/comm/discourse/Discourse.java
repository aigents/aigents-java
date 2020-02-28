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
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.SocialCacher;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Profiler;
import net.webstructor.util.JSON;

//https://github.com/aigents/aigents-java/issues/7
//public class Discourse extends Socializer {
public class Discourse extends SocialCacher {
	String appId;
	String appSecret;
	protected HttpFileReader reader;//TODO: move up to HTTP or fix native HTTP
	boolean debug = true; 
	
	public Discourse(Body body, String name, String url, String appId, String appSecret) {
		super(body,name,url);
		this.appId = appId;
		this.appSecret = appSecret;
		this.reader = new HttpFileReader();
	}

	@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		url = body.self().getString(Body.discourse_url);//refresh url
//TODO refresh id (username) and api key if needed 
		DiscourseFeeder feeder = new DiscourseFeeder(body,this,id,body.languages,since,until,areas);
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}
	
	/*public int readChannel(String uri, Collection topics, MapMap thingPathsCollector){
		if (AL.empty(uri) || AL.empty(appId) || AL.empty(appSecret))
			return -1;
		
		String subreddit = Str.parseBetween(uri, "reddit.com/r/", "/", false);
		String user = Str.parseBetween(uri, "reddit.com/user/", "/", false);
		
		if (AL.empty(subreddit) && AL.empty(user))
			return -1;
		
		//https://github.com/reddit-archive/reddit/wiki/OAuth2
//TODO: mobile/tablet/desktop case
		//https://oauth.reddit.com/grants/installed_client:
		//	Installed app types (as these apps are considered "non-confidential", have no secret, and thus, are ineligible for client_credentials grant.
		//	Other apps acting on behalf of one or more "logged out" users.
		//web-case
		//client_credentials:
		//	Confidential clients (web apps / scripts) not acting on behalf of one or more logged out users.
		//https://www.reddit.com/api/v1/access_token
		//For client_credentials grants include the following information in your POST data (NOT as part of the URL)
		//grant_type=client_credentials
		//You must supply your OAuth2 client's credentials via HTTP Basic Auth for this request. The "user" is the client_id, the "password" is the client_secret.
		String params = "grant_type=client_credentials";
		String auth_base64 = auth_base64(appId,appSecret);
		String response;
		try {
			body.debug("Reddit read channel "+uri+" request "+params+" "+auth_base64);
			response = HTTP.simple(Discourse.oauth_url,params,"POST",timeout,null,new String[][] {new String[] {"Authorization",auth_base64}});
			body.debug("Reddit read channel "+uri+" response "+response);
			if (!AL.empty(response)) {
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				if (json.containsKey("access_token")) {
					String access_token = HTTP.getJsonString(json,"access_token");
					String[][] hdr = new String[][] {new String[] {"Authorization","bearer "+access_token}};
					String after = null;
					//Date since = Time.today(-body.self().getInt(Body.attention_period,14));
					Date since = Time.today(-1);
					int matches = 0;
					for (boolean days_over = false; !days_over;) {
//TODO: throttling based on header info or invalid replies 
						//https://www.reddit.com/dev/api#GET_new
						//https://www.reddit.com/dev/api#GET_user_{username}_{where}
						String api_url = !AL.empty(subreddit) ? "https://oauth.reddit.com/r/"+subreddit+"/new" : "https://oauth.reddit.com/user/"+user+"/submitted";
						params = "limit=100" + (after == null ? "" : "&after="+after);
						if (debug) body.debug("Reddit read channel request "+uri+" "+api_url+" "+params);
						response = HTTP.simple(api_url+"?"+params,null,"GET",0,null,hdr);
						if (debug) body.debug("Reddit read channel response "+uri+" "+response);
						if (AL.empty(response))
							break;
						jsonReader = Json.createReader(new StringReader(response));
						json = jsonReader.readObject();
						JsonObject data = JSON.getJsonObject(json,"data");
						if (data == null)
							break;
						JsonArray children = JSON.getJsonArray(data, "children");
						if (children != null) for (int i = 0; i < children.size(); i++) {
							JsonObject item = children.getJsonObject(i);
							//String type = JSON.getJsonString(item, "type");//t1_Comment,t2_Account,t3_Link,t4_Message,t5_Subreddit,t6_Award
							item = JSON.getJsonObject(item,"data");
							RedditItem ri = new RedditItem(item);
							if (!ri.is_robot_indexable)// || !"public".equals(ri.subreddit_type))
								continue;
							if (ri.date.compareTo(since) < 0){
								days_over = true;
								break;
							}
							String text = HtmlStripper.convert(ri.text," ",null);
							text = HtmlStripper.convertMD(text, null, null);
//TODO: consider if we want to consider links and images same way as we do that for Siter's web pages  
							matches += Siter.matchThingsText(body,topics,text,ri.date,ri.uri,AL.isURL(ri.thumbnail)?ri.thumbnail:null,thingPathsCollector);
						}
						after = JSON.getJsonString(data, "after");
						if (after == null)
							break;
					}
					return matches;
				}
			}
		} catch (IOException e) {
			body.error("Reddit read channel error "+uri,e);
		}
		return -1;
	}*/

	@Override
	public Profiler getProfiler(Thing peer) {
		return new Profiler(body,this,peer,Body.discourse_id);
	}

	private transient HashMap<Long,HashMap<Long,DiscourseItem>> childrenCache = new HashMap<Long,HashMap<Long,DiscourseItem>>();
	private Date earliest = null;
	
	Date getEarliest(){
		synchronized (childrenCache) {
			return earliest;
		}
	}

	Collection<DiscourseItem> getReplies(long topic_id){
		synchronized (childrenCache) {
			HashMap<Long,DiscourseItem> map = childrenCache.get(topic_id);
			return map == null ? null : map.values();
		}
	}
	
	boolean addReply(DiscourseItem post){
//TODO: optimize storage and update policy
		synchronized (childrenCache) {
			HashMap<Long,DiscourseItem> map = childrenCache.get(post.topic_id);
			if (map == null)
				childrenCache.put(post.topic_id, map = new HashMap<Long,DiscourseItem>());
			if (!map.containsKey(post.post_id)) {
				map.put(post.post_id,post);
				if (earliest == null || earliest.compareTo(post.created_at) > 0)
					earliest = post.created_at;
				return true;
			}
		}
		return false;
	}
	
	//TODO official resync override
	void sync(Date since, Date until) {
		long before = 0;	
		String api_url = getUrl();
		if (!api_url.endsWith("/"))
			api_url += "/";
		
		try {
			for (boolean days_over = false; !days_over;) {
				String url = api_url + (before == 0 ? "posts.json" : api_url + "posts.json?before=1370=" + before);
				//if (debug) body.debug("Spidering posts discourse request "+url);
				String response;
					response = HTTP.simple(url,null,"GET",0,null,null);
				//if (debug) body.debug("Spidering posts discourse response "+response);
				if (AL.empty(response))
					break;
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray posts = JSON.getJsonArray(json,"latest_posts");
				if (posts == null || posts.size() == 0)
					break;
				for (int i = 0; i < posts.size(); i++) {
					JsonObject o = posts.getJsonObject(i);
					DiscourseItem a = new DiscourseItem(o);
					if (!a.visible)
						continue;
					if (a.created_at.compareTo(since) < 0){
						days_over = true;
						break;
					}
					boolean added = addReply(a);
					if (!added && getEarliest().compareTo(since) <= 0) {//if known earliest time for the cache is earlier that requested
						days_over = true;
						break;
					}
					before = a.post_id;		
//TODO: break on earliest/latest
				}
				before--;
			}
		} catch (IOException e) {
			body.error("Spidering posts discourse request "+url,e);
		}
		
	}
	
	@Override
	protected void updateGraphs(long block, Date since, Date until) {
		//Option 1: Get list of user and iterate all users - need user id and api key
		
		//Option 2: Get list of user (need self discourse id) and iterate all users
		
		//Option 3: Get all posts for much longer period and likes from posts, get users from posts and then likes on per-user basis (one-way-per-user)
		//get topic posts:
		//curl https://community.singularitynet.io/t/475.json > topic_475.json
		//get posts:
		//curl https://community.singularitynet.io/posts.json?before=1370 posts_1370.json
		//get post likes:
		//curl 'https://community.singularitynet.io/post_action_users?id=1370&post_action_type_id=2' -H 'Accept: application/json' > post_likes_1370.json
		//get user actions
		//curl "https://community.singularitynet.io/user_actions.json?username=akolonin&filter=1,2,3,4,5,6,7" > akolonin_0.json
	}
}