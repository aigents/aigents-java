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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.SocialCacher;
import net.webstructor.core.Thing;
import net.webstructor.data.DataLogger;
import net.webstructor.data.Graph;
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

	//TODO: merge the two
	private transient HashMap<String,String> user_names = new HashMap<String,String>();
	private transient HashMap<String,Long> user_offsets = new HashMap<String,Long>();

	//TODO: incapsulated cache class/object
	private transient HashMap<Long,HashMap<Long,DiscourseItem>> topic_posts = new HashMap<Long,HashMap<Long,DiscourseItem>>();
	private transient Date earliest = null;
	
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

	Date getEarliest(){
		synchronized (topic_posts) {
			return earliest;
		}
	}

	Collection<DiscourseItem> getReplies(long topic_id){
		synchronized (topic_posts) {
			HashMap<Long,DiscourseItem> map = topic_posts.get(topic_id);
			return map == null ? null : map.values();
		}
	}

	DiscourseItem getPost(long topic_id, long post_id){
		synchronized (topic_posts) {
			HashMap<Long,DiscourseItem> map = topic_posts.get(topic_id);
			return map == null ? null : map.get(post_id);
		}
	}
	
	boolean addReply(DiscourseItem post){
//TODO: optimize storage and update policy
		synchronized (topic_posts) {
			HashMap<Long,DiscourseItem> map = topic_posts.get(post.topic_id);
			if (map == null)
				topic_posts.put(post.topic_id, map = new HashMap<Long,DiscourseItem>());
			if (!map.containsKey(post.post_id)) {
				map.put((long)(post.post_number == 0 ? 1 : post.post_number), post);
				if (earliest == null || earliest.compareTo(post.created_at) > 0)
					earliest = post.created_at;
				return true;
			}
		}
		return false;
	}
	
	//TODO: move out and reuse with Reddit
	public String simpleRetry(String url,String urlParameters,String method,String cType,String[][] props) throws IOException, InterruptedException {
		String response = null;
		for (int retry = 1; retry <= 10; retry++){
			try {
				response = HTTP.simple(url,urlParameters,method,1000,cType,props);
			} catch (java.net.SocketTimeoutException e) {
				body.debug("Discourse crawling timeout");
			}
			if (AL.empty(response) || !(response.startsWith("{") || response.startsWith("["))){
				body.debug("Discourse crawling throttling "+response);
				Thread.sleep(1000 * retry);
			} else 
				break;
		}
		return response;
	}
	
	long syncPosts(Date since, long before) {
		String api_url = getUrl();
		try {
			for (boolean days_over = false; !days_over;) {
				String url = api_url + (before == 0 ? "/posts.json" : "/posts.json?before=" + before);
				if (debug) body.debug("Discourse crawling posts request "+url);
				String response = simpleRetry(url,null,"GET",null,null);
				//if (debug) body.debug("Discourse crawling posts response "+response);
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
				}
				before--;
			}
		} catch (Exception e) {
			body.error("Discourse crawling posts request "+url,e);
		}
		return before;
	}
	
	protected void getUsers() {
		//Option 1 (have admin user id and api key): 
		//Get list of user and iterate all users - need user id and api key
		String key = body.self().getString(Body.discourse_key);
		String aid = body.self().getString(Body.discourse_id);
		if (aid == null) {//if id not counfugured for server, use id of a trusted peer
			Collection trusts = (Collection)body.self().get(AL.trusts);
			if (!AL.empty(trusts)) for (Object trust : trusts)
				if ((aid = ((Thing)trust).getString(Body.discourse_id)) != null)//TODO: ensure it is a peer
					break;
		}
		if (AL.empty(aid))
			aid = this.appId;
		if (AL.empty(key))
			key = this.appSecret;
		if (AL.empty(aid) || AL.empty(key))
			return;
		
		String[][] hdr = new String[][] {new String[] {"Api-Key",key},new String[] {"Api-Username",aid}};
		String api_url = getUrl();
		try {
			for (int page = 0;; page++) {
				String url = api_url + (page == 0 ? "/admin/users/list/active.json" : "/admin/users/list/active.json?page=" + page);
				if (debug) body.debug("Discourse crawling users request "+url);
				String response = simpleRetry(url,null,"GET",null,hdr);
				//if (debug) body.debug("Discourse crawling users response "+response);
				if (AL.empty(response))
					break;
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonArray us = jsonReader.readArray();
				if (us == null || us.size() == 0)
					break;
				for (int i = 0; i < us.size(); i++) {
					JsonObject u = us.getJsonObject(i);
					String name = JSON.getJsonString(u, "name");
					String uid = JSON.getJsonString(u, "username");
					user_names.put(uid, name);
					//avatar_template = user_avatar/community.singularitynet.io/akolonin/{size}/146_2.png
				}
			}
		} catch (Exception e) {
			body.error("Discourse crawling users request "+url,e);
		}
		
		//Option 2 (DO NOT have admin user id and api key): 
		//Get all posts for much longer period, extract likes from posts, get users from likes
		//get posts:
		//curl https://community.singularitynet.io/posts.json?before=1370 posts_1370.json
		//get post likes:
		//curl 'https://community.singularitynet.io/post_action_users?id=1370&post_action_type_id=2' -H 'Accept: application/json' > post_likes_1370.json
	}
	
	//start iterate from known offset and iterate till the date is the same and till the age is not reached
	//return latest age and younges offset known
//TODO consider what if new data appears and offests change
	public long[] updateUser(String user_id, long offset, Date date, long old_age, ArrayList<Object[]> log) {
		long new_age = 0;
		try {
			String api_url = getUrl();
			for (boolean time_over = false; !time_over;) {
				String url = api_url + "/user_actions.json?username="+user_id+"&filter=1,5,7&offset=" + offset;
				if (debug) body.debug("Discourse crawling user request "+url);
				String response = simpleRetry(url,null,"GET",null,null);
				if (debug) body.debug("Discourse crawling user response "+user_id+" "+response);
				if (AL.empty(response))
					break;
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray actions = JSON.getJsonArray(json,"user_actions");
				if (actions == null || actions.size() == 0)
					break;
				int i = 0;
				for (; i < actions.size(); i++) {
					JsonObject o = actions.getJsonObject(i);
					DiscourseItem a = new DiscourseItem(o);
					if (!a.visible)
						continue;
					long age = a.created_at.getTime();
					if (age <= old_age || age < date.getTime()){
						time_over = true;
						break;
					}
					if (new_age < age)
						new_age = age;
					Integer length = new Integer(AL.empty(a.text) ? 1 : a.text.length());
					switch (a.action_type) {
//TODO: sentiment analysis  
					case 1: //1 - liked by me
						log.add(new Object[] {a.acting_username,"vote",a.username,null,a.created_at});
						break;
					case 5: //5 - my reply posts
//TODO: actual usernames from posts  
						DiscourseItem p = getPost(a.topic_id, a.reply_to_post_number == 0 ? 1 : a.reply_to_post_number);
						if (p != null)
							log.add(new Object[] {a.acting_username,"comment",p.username,length,a.created_at});
						break;
					case 7: //7 - mentions of me
						log.add(new Object[] {a.acting_username,"mention",a.target_username,length,a.created_at});
						break;
					}
				}
				offset += i;//increment offset for the next trial
			}
		} catch (Exception e) {
			body.error("Discourse crawling user "+user_id, e);
		}
		return new long[] {new_age,offset};
	}

	@Override
	protected void updateGraphs(long block, Date since, Date until) {
		//1. Get the latest old_age from graphs and adjust the "until"
		//2. Do the following from until day till since
		//3. Get latest timestamp in the current graph day
		//4. For every day, iterate all users and get all their actions one-way (from user) and keep them in temp cache, stop on timestamp 
		//5. For every day, flush temp cache to graphs, save graphs and go to another day
				
		Date latest_known_date = since; 
		for (Date date = until; date.compareTo(since) >= 0; date = Time.addDays(date, -1)){
			Graph graph = getGraph(date);
			synchronized ((graph )) {
				long age = graph.getAge();
				if (age > 0) {
					latest_known_date = Time.date(new Date(age),0);
					break;
				}
			}
		}
		
		long start = System.currentTimeMillis(); 
		body.debug("Discourse crawling start");
		
		getUsers();//get all users
		
		syncPosts(since, 0);//TODO set since to sync posts for limited period of time (e.g. last week or month or such)

		long sync = System.currentTimeMillis(); 
		if (debug) body.debug("Discourse crawling synced, users "+user_names.size()+", topics "+topic_posts.size()+", took "+Period.toHours(sync-start));
		
		DataLogger logger = new DataLogger(body,Writer.capitalize(name)+" crawling");
		for (Date date = until; date.compareTo(latest_known_date) >= 0; date = Time.addDays(date, -1)){
			long day_start = System.currentTimeMillis(); 
			if (debug) body.debug("Discourse crawling start day "+date);
			Graph graph = getGraph(date);
			ArrayList<Object[]> log = new ArrayList<Object[]>();
			synchronized (graph) {
				long old_age = graph.getAge();
				long new_age = 0;
				log.clear();
				for (String user_id : user_names.keySet()) {
					long offset = user_offsets.containsKey(user_id) ? user_offsets.get(user_id) : 0;
					long[] age_offset = updateUser(user_id, offset, date, old_age, log);
					if (new_age < age_offset[0])
						new_age = age_offset[0];
					user_offsets.put(user_id, age_offset[1]);
				}
				//TODO update graph
				if (log.size() != 0) {
					for (Object[] l : log) {
						if (debug) body.debug("Discourse crawling log "+Writer.toString(l));
						String from = (String)l[0]; 
						String type = (String)l[1]; 
						String to = (String)l[2];
						long stamp = ((Date)l[4]).getTime();
						if (l[1].equals("vote")) {
							write(logger, name, date, stamp, type, from, to, "1", null, null, null, null, null, null, null);
							alert(date,0,"votes",from,to,"1",getUrl());
							graph.addValue(from,to, "votes", 1);//out
							graph.addValue(to,from, "voted", 1);//in
						}else
						if (l[1].equals("comment")) {
							int logvalue = 1 + (int)Math.round(Math.log10(((Number)l[3]).doubleValue()));//should be non-zero
							write(logger, name, date, stamp, type, from, to, String.valueOf(l[3]), null, null, null, null, null, null, null);
							alert(date,0,"comments",from,to,"1",getUrl());
							graph.addValue(from,to, "comments", logvalue);//out
							graph.addValue(to,from, "commented", logvalue);//in
						}else
						if (l[1].equals("mention")) {
							int logvalue = 1 + (int)Math.round(Math.log10(((Number)l[3]).doubleValue()));//should be non-zero
							write(logger, name, date, stamp, type, from, to, String.valueOf(l[3]), null, null, null, null, null, null, null);
							alert(date,0,"mentions",from,to,"1",getUrl());
							graph.addValue(from,to, "mentions", logvalue);//out
							graph.addValue(to,from, "mentioned", logvalue);//in
						}
					}
					updateGraph(date,graph,new_age);
				}
			}
			long day_stop = System.currentTimeMillis(); 
			if (debug) body.debug("Discourse crawling stop day "+date+", took "+Period.toHours(day_stop-day_start));
		}
		long stop = System.currentTimeMillis(); 
		body.debug("Discourse crawling stop, took "+Period.toHours(stop-start));
		
		try {logger.close();} catch (IOException e) {body.error("Discourse crawling logger",e);}//TODO: move it to updateGraphs in SocialCacher
	}
	
	
	public static void main(String args[]) {
		if (args.length < 4)
			return;
		Body b = new Body(true,0) {//TODO fix hack with fake body
			@Override
			public void updateStatus(boolean now) {}
			@Override
			public void updateStatus(Thing peer) {}
			@Override
			public void updateStatusRarely() {}
		};
		Discourse d = new Discourse(b, args[0], args[1], args[2], args[3]);
		d.resync(0);
	}
}