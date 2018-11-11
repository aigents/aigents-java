/*
 * MIT License
 * 
 * Copyright (c) 2005-2018 by Anton Kolonin, Aigents
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
package net.webstructor.comm.fb;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.al.Time;
import net.webstructor.al.Period;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Environment;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.data.SocialFeeder;

class Feeder extends SocialFeeder {

	static String api_url = "https://graph.facebook.com/v3.0/";
	static final int MAX_CALLS_PER_HOUR = 200;
	
	HttpFileReader reader = new HttpFileReader();
	int calls = 0;
	String token = null;
	
	
	public Feeder(Environment body, String user_id, LangPack langPack, boolean obfuscate, Date since, Date until) {
		super(body, user_id,langPack,obfuscate,since,until);
	}

	String callAPI(String url) throws IOException, InterruptedException {
		String out = null;
		for (int retry = 2; retry <= 64; retry*=2){
			try {
				calls++;
				out = reader.readDocData(url);
				if (!out.startsWith("{\"error\":{\"message\":\"(#4) Application request limit reached"))
					break;
				Thread.sleep(retry * Period.SECOND);
			} catch (IOException e) {
				throw e;
			}
		}
		return out;
	}
	
	//returns true if liked by current user
	public boolean getLike(JsonObject like,Date time,int count){
		String id = FB.getJsonString(like,"id");
		String name = FB.getJsonString(like,"name");
		countLikes(id,name,time,count);
		if (user_id.equals(id))
			return true;
		//TODO: move to countLikes across system!?
		countPeriod(time,count,0);//count like by other
		return false;
	}

	//TODO: remove as deprecated since API v2.6
	//TODO: get properly with
	//https://developers.facebook.com/docs/graph-api/reference/v2.5/object/likes
	//returns true if liked by current user
	public Object[] getLikes(JsonObject likes,Date time){
		boolean user_likes = false;
		int like_count = 0;
		if (likes != null){
			JsonArray data = likes.getJsonArray("data");
			if (data != null){
				for (int i = 0; i < data.size(); i++){
					if (getLike(data.getJsonObject(i),time,1))
						user_likes = true;
				}
				like_count = data.size();
			}
		}
		//JsonObject paging = likes.getJsonObject("paging");
		return new Object[]{new Boolean(user_likes),new Integer(like_count)};
	}

	//https://developers.facebook.com/docs/graph-api/reference/v3.0/object/likes
	//TODO: proper paging for more than 1000 likes?
	public Object[] getLikes(String id,Date time){
		boolean user_likes = false;
		int like_count = 0;
		String url, out = "";
		try {
			url = api_url + id + "/likes/?limit=1000&summary=true&access_token=" + token;
			//body.debug("Facebook feeding object likes "+url);
			out = callAPI(url);
			JsonReader jsonReader = Json.createReader(new StringReader(out));
			JsonObject json = jsonReader.readObject();
			JsonObject summary = json.getJsonObject("summary");
			user_likes = summary.containsKey("has_liked") ? summary.getBoolean("has_liked") : false;
			like_count = summary.containsKey("total_count") ? summary.getInt("total_count") : 0;
			JsonArray data = json.getJsonArray("data");
			int anonymous = like_count;
			if (data != null){
				for (int i = 0; i < data.size(); i++)
					if (getLike(data.getJsonObject(i),time,1))
						user_likes = true;
				anonymous -= data.size();
			}
			if (anonymous > 0){
				countLikes("anonymous","Anonymous",time,anonymous);
				countPeriod(time,anonymous,0);//count like by other
			}
			//JsonObject paging = json.getJsonObject("paging");
		} catch (Exception e) {
			body.error("Facebook feeding object likes error "+out,e);
		}
		return new Object[]{new Boolean(user_likes),new Integer(like_count)};
	}
	
	public void getComment(JsonObject comment,Date time){
		JsonObject from = comment.getJsonObject("from");
		String id = FB.getJsonString(from,"id");
		String name = FB.getJsonString(from,"name");
		//TODO: word proximity
		String message = FB.getJsonString(comment,"message");
		boolean like = comment.containsKey("user_likes") ? comment.getBoolean("user_likes") : false;
		if (like)
			countMyLikes(id,name);
		countComments(id,name,message,time);
		//TODO:move to countComments across system!?
		if (!user_id.equals(id) && getUser(id,name) != null)
			countPeriod(time,0,1);//count comment by other
	}

	
	//TODO: get properly with
	//https://developers.facebook.com/docs/graph-api/reference/v2.2/object/comments
	public int getComments(JsonObject comments,Date time){
		if (comments == null)
			return 0;
		JsonArray data = comments.getJsonArray("data");
		if (data == null)
			return 0;
		for (int i = 0; i < data.size(); i++)
			getComment(data.getJsonObject(i),time);
		//JsonObject paging = likes.getJsonObject("paging");
		return data.size();
	}
	
	public static final String TIME_FORMAT = "yyyy-MM-dd"; 
	public void getItem(JsonObject item){
		String message = FB.getJsonString(item,"message");//in the post
		String description = FB.getJsonString(item,"description");//in the origin of the post
		String time = FB.getJsonString(item,"created_time");//"created_time": "2015-12-01T18:20:49+0000",
		Date times = Time.time(time,TIME_FORMAT);
		String id = FB.getJsonString(item,"id");
		String link = FB.getJsonString(item,"link");//may be facebook link, use if if no link found in the message
		String text = message != null ? message : description;
//TODO: if no text, and link is not to FB, get text from URL!?

//TODO: fix getting likes avoiding being blocked out 		
		Object[] likeInfo = getLikes(id,times);
		Boolean like = (Boolean)likeInfo[0]; 
		Integer likes = (Integer)likeInfo[1]; 
		//Boolean like = new Boolean(false); 
		//Integer likes = new Integer(0); 

//TODO: remove if not supported anymore or fix!?
		JsonObject commentsObject = item.getJsonObject("comments");//who commented on this
		int commentsCount = getComments(commentsObject,times);
		
		//TODO: sort out why feeds of other users are not displayed
		//TODO: support multiple sources per post

		//TODO: cleanup \ slashes before special characters?
		if (text != null){
			text = text.replace('\n', ' ');
			//text.replace('\r', ' ');
			
			Counter period = getWordsPeriod(getPeriodKey(times));
			//countPeriod(times,likes.intValue() - (like.booleanValue()? 1 : 0), commentsCount);
			
			Integer comments = new Integer(commentsCount);
			String[] sources = extractUrls(text,new String[]{link},like,likes,comments,period);
			Object[] news_item = new Object[]{like,likes,comments,times,text,sources};
			news.add(news_item);
		}		
	}
	
	/*
	 TODO:Submit for review, when user_posts is working for admin user:
1. Ensure there is at least few posts with some text and web link liked by user on their Facebook Timeline - within the last week.
2. Go to https://aigents.com/ site
3. Click Facebook icon for registration/login via Facebook (on the right-top of the screen) and confirm registration/login
4. Wait few minutes so the data is updated behind the scene
5. Go to 'Topics' view and see keywords of your preferences extracted from the posts (using user_posts permission) that you like and sites that your like (using user_likes permission) detected by machine learning, check/uncheck them to adjust your interests, add or check ones that you are interested about
6. Go to 'Sites' tab and see links to the sites of your interest, extracted from the posts that you like and sites that your like, check/uncheck ones according to your interests, add or check ones that you are interested about 
7. Wait 1 day and go to 'News' tab - you will see that relevance of news items is indicated by width of blue bar, this is detected with use of machine learning based on your preferences and activity (user_posts and user_likes permissions) while the width of green bar indicates potential relevance of the same news items for your friends that you trust to (obtained via user_friends permission).
P.S. Note, that I am making the same submission third time in a row - the first two submissions were rejected without of explanation of the reason. So if you reject this submission again, please explain the reasons.
P.P.S. Also note, that at step 3 made by admin (!!!) user, there is a message popped up: "Invalid Scopes: user_posts. This message is only shown to developers. Users of your app will ignore these permissions if present. Please read the documentation for valid permissions at: https://developers.facebook.com/docs/facebook-login/permissions", respectively, when I request user posts by the app, empty data is returned - this is weird because there is no clear way to create submission around this error message and empty data returned on API call. If you reject this submission again please explain how to workaround the behaviour. 	  
	 */
	public void getFeed(String token, Date since, Date until) throws IOException {
		errorMessage = "Facebook API for user feed is not working at the moment, so no reports can be made.";
		
		//do main work
		//TODO: later, when/if the user_posts is approved
		/*
		getFeedPosts(token,since,until);
		*/
		
		//TODO: later, when the rest is approved
		/*
		//try to use other permissions
		String url, out = "";
		//https://developers.facebook.com/docs/graph-api/reference/v3.0/user/likes
		try {
			url = api_url + user_id + "/likes/?access_token=" + token;
			body.debug("Facebook feeding likes "+url);
			out = callAPI(url);
		} catch (Exception e) {
			body.error("Facebook feeding likes error "+out,e);
		}
		//https://developers.facebook.com/docs/graph-api/reference/v3.0/user/friends
		try {	
			url = api_url + user_id + "/friends/?access_token=" + token;
			body.debug("Facebook feeding friends "+url);
			out = callAPI(url);
		} catch (Exception e) {
			body.error("Facebook feeding friends error "+out,e);
		}
		*/
		body.debug("Facebook feeding peer "+user_id+" calls "+calls);
	}

	//TODO: fix for 3.0
	//https://developers.facebook.com/tools/explorer
	//https://developers.facebook.com/docs/facebook-login/permissions/
	//https://developers.facebook.com/docs/graph-api/reference/v3.0/user/feed
	public void getFeedPosts(String token, Date since, Date until) throws IOException {
		this.token = token;
		String url = api_url + user_id + "/feed/?"
			+ (since != null ? "since="+String.valueOf(since.getTime()/1000)+"&" : "" )
			+ (until != null ? "until="+String.valueOf(until.getTime()/1000)+"&" : "" )
			+ "limit=100&"
			//+ "fields=id,created_time,likes,message,description,link,comments{id,from,message,likes,user_likes}&access_token=" + token;
			+ "fields=id,created_time,link,message&access_token=" + token;
		String out = "";
		try {
			for (;;){
				body.debug("Facebook feeding feed "+url);
				out = callAPI(url);
				JsonReader jsonReader = Json.createReader(new StringReader(out));
				JsonObject json = jsonReader.readObject();
				JsonArray data = json.getJsonArray("data");
				JsonObject paging = json.getJsonObject("paging");
				if (data == null || data.size() == 0)
					break;
				body.debug("Facebook feeding feeds "+data.size()+", calls "+calls);
				for (int i = 0; i < data.size(); i++){
					getItem(data.getJsonObject(i));
					//TODO:remove or add to other social networks
					//total++;
					//if (body.getMaxNews() != 0 && total >= body.getMaxNews())
					//	return;
				}
				if (paging == null)
					break;
				url = FB.getJsonString(paging, "next");
				if (url == null)
					break;
			}
			addPerCommentWords();//add per-user-comment word counts to per-post word counts
		} catch (Exception e) {
			body.error("Facebook feeding feed error "+out,e);
			if (e instanceof IOException)
				throw (IOException)e;
		}
	}
	
}
