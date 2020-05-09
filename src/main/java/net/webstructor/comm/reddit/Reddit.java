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
package net.webstructor.comm.reddit;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Profiler;
import net.webstructor.self.Siter;
import net.webstructor.util.JSON;
import net.webstructor.util.MapMap;
import net.webstructor.util.Str;

//TODO: merge Reddit+Redditer and FB+Messenger? 
public class Reddit extends Socializer {
	String appId;
	String appSecret;
	protected HttpFileReader reader;//TODO: move up to HTTP or fix native HTTP
	boolean debug = true; 
	
	public static final String oauth_url = "https://www.reddit.com/api/v1/access_token";
	public static final String home_url = "https://www.reddit.com";

	public Reddit(Body body, String appId, String appSecret) {
		super(body);
		this.appId = appId;
		this.appSecret = appSecret;
		this.reader = new HttpFileReader(body);
	}

	@Override
	public String provider(){
		return "reddit";
	}
	
	@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		RedditFeeder feeder = new RedditFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}
	
	String refresh_token(String client_id,String client_secret,String refresh_token) throws IOException {
		String params = "grant_type=refresh_token&refresh_token="+refresh_token;
		String auth_base64 = auth_base64(client_id,client_secret);
		body.debug("Reddit refresh request "+params+" "+auth_base64);
		String response = HTTP.simple(Reddit.oauth_url,params,"POST",timeout,null,new String[][] {new String[] {"Authorization",auth_base64}});
		body.debug("Reddit refresh response "+response);
		if (!AL.empty(response)) {
			JsonReader jsonReader = Json.createReader(new StringReader(response));
			JsonObject json = jsonReader.readObject();
			if (json.containsKey("access_token"))
				return HTTP.getJsonString(json,"access_token");
		}
		return null;
	}
	
	String refresh_token(String token) throws IOException {
		return refresh_token(appId,appSecret,token);
	}	

	public int readChannel(String uri, Collection topics, MapMap thingPathsCollector){
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
			response = HTTP.simple(Reddit.oauth_url,params,"POST",timeout,null,new String[][] {new String[] {"Authorization",auth_base64}});
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
	}

	@Override
	public Profiler getProfiler(Thing peer) {
		return new Profiler(body,this,peer,Body.reddit_id,Body.reddit_token,Body.reddit_key);
	}
}