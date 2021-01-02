/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents®
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
package net.webstructor.self;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Environment;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.LangPack;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Profiler;


class AigentsFeeder extends SocialFeeder {
	Aigents api;
	public AigentsFeeder(Environment body, Aigents api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		this.api = api;
	}
	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		if (body instanceof Body) try {
			Storager storager = ((Body) body).storager; 
			Collection peers = storager.getByName(api.getPeerIdName(), user_id);
			if (!AL.empty(peers)) for (Object p : peers) {
				Thing peer = (Thing)p;
				Collection news = peer.getThings(AL.trusts);
				if (!AL.empty(news)) for (Object n : news) {
					Thing t = (Thing)n;
					Date date = t.getDate(AL.times, null);
					String newstext = t.getString(AL.text);
					Collection sources = t.getThings(AL.sources);
					if (!AL.empty(newstext) && !AL.empty(sources) && date != null && since.compareTo(date) <= 0 && date.compareTo(until) <=0) {
						OrderedStringSet links = new OrderedStringSet();
						for (Object s : sources)
							links.add(((Thing)s).getString(AL.name));//add links from sources 
//TODO: total trusts by others
						int trusts = 0;
//TODO: authors
//TODO: likers by author
						//String text = 
						processItem(date,user_id,newstext,links,null,trusts,true);
//TODO: sorted details
						//reportDetail(detail,ri.author,ri.uri,ri.typed_id,text,ri.date,comments,links,null,ri.score,0,ri.comments,imghtml);
					}
				}
			}
		} catch (Exception e) {
			body.error("Aigents crawling peer "+user_id, e);
		}
	}
	@Override
	public Socializer getSocializer() {
		return api;
	}
}

//TODO: 
public class Aigents extends Socializer {
	boolean debug = true; 
	
	public Aigents(Body body) {
		super(body);
	}

	@Override
	public String name(){
		return body.name();
	}
	
	@Override
	public String getPeerIdName() {
		return AL.email;
	}
	
	@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		AigentsFeeder feeder = new AigentsFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}
	
	@Override
	public Profiler getProfiler(Thing peer) {
		return new Profiler(body,this,peer,getPeerIdName());//TODO:use email as id
	}
}
