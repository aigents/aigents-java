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
package net.webstructor.comm.reddit;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.al.AL;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Environment;
import net.webstructor.data.LangPack;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;
import net.webstructor.util.JSON;
import net.webstructor.util.Str;

/*
TODO:
Clients connecting via OAuth2 may make up to 60 requests per minute. Monitor the following response headers to ensure that you're not exceeding the limits:
X-Ratelimit-Used: Approximate number of requests used in this period
X-Ratelimit-Remaining: Approximate number of requests left to use
X-Ratelimit-Reset: Approximate number of seconds to end of period
Change your client's User-Agent string to something unique and descriptive, including the target platform, a unique application identifier, a version string, and your username as contact information, in the following format:
<platform>:<app ID>:<version string> (by /u/<reddit username>)
Example: User-Agent: android:com.example.myredditapp:v1.2.3 (by /u/kemitche)
Many default User-Agents (like "Python/urllib" or "Java") are drastically limited to encourage unique and descriptive user-agent strings.
Including the version number and updating it as you build your application allows us to safely block old buggy/broken versions of your app.
NEVER lie about your user-agent. This includes spoofing popular browsers and spoofing other bots. We will ban liars with extreme prejudice.

TODO:
- report 
	https://www.reddit.com/dev/api/
		GET /user/username/where history rss support
		→ /user/username/overview - all posts and comments?
		→ /user/username/submitted - all posts ?
		→ /user/username/comments - all comments ?
		→ /user/username/upvoted - ?
		→ /user/username/downvoted - ?
		→ /user/username/hidden - ?
		→ /user/username/saved - ?
		→ /user/username/gilded - ?
- push content to feed with alerts
		→ /user/username/upvoted - ?
- count likes/comments on per-post basis
	https://www.reddit.com/dev/api/#GET_api_info
- extract and monitor individual reddits  
*/


class RedditFeeder extends SocialFeeder {
	Reddit api;
	boolean debug = true;

	public RedditFeeder(Environment body, Reddit api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		this.api = api;
	}

//TODO: remove or rewrite
	/*
	private Object[][] processComments(String token,int id) throws IOException{
		//TODO:process all comments, not just first 100 as now
		String url = "";//TODO:
		sleep(1000);
		String out = HTTP.simpleGet(url);
		JsonReader jr = Json.createReader(new StringReader(out));
		JsonObject jobj = jr.readObject();
		JsonArray comments = jobj.getJsonArray("items");
		ArrayList collected = new ArrayList();
		if (comments != null)
		for (int i = 0; i < comments.size(); i++){
			JsonObject comment = comments.getJsonObject(i);
			String from_id = String.valueOf(comment.getInt("from_id"));
			String text = comment.getString("text");
			JsonObject likes = comment.getJsonObject("likes");
			int user_likes = likes.getInt("user_likes");
			int likes_count = likes.getInt("count");
			Date date_day = new Date( ((long)comment.getInt("date")) * 1000 );
			Counter period = getWordsPeriod(getPeriodKey(date_day));
			countPeriod(date_day,likes_count - user_likes,0);
			
			String name;
			if (from_id.equals(user_id)){//my own texts
				//name = user_name;
				if (likes_count > 0){//count likes on me if liked by others
					HashMap likers = new HashMap();
					//user_likes = extractLikes(token, "comment", comment.getInt("cid"), likers);
					processLikes(likers,date_day);//count who liked my comment
				}
				extractUrls(text, null, new Boolean(user_likes > 0),new Integer(likes_count - user_likes), new Integer(0), period);
			}else{//texts by others
				//TODO: process "unfamiliar" users (obtained with "extended=1" which is now not working with VK) 
				Object[] user = getUser(from_id,null);
				if (user == null)
					continue;
				name = getUserName(user);
				body.debug("Spidering peer vkontakte "+user_id+" other user text:"+from_id+" "+name);
				//if (user_likes == 0)//maybe just VK bug
				//	user_likes = extractLikes(token, "comment", comment.getInt("cid"), null);				
				countComments(from_id,name,text,date_day);
				if (user_likes > 0){//if likedb by me, count links on author and acquire liked words
					countMyLikes(from_id,name);
					extractUrls(text, null, new Boolean(user_likes > 0),new Integer(likes_count - user_likes), new Integer(0), period);
				}
			}
			//collected.add(new Object[]{from_id,name,text,new Boolean(user_likes == 1),new Integer(likes_count - user_likes)});
		}
		jr.close();
		return (Object[][]) collected.toArray(new Object[][]{});
	}
	
//TODO: remove or rewrite
	private void processContent(JsonObject item,StringBuilder content,OrderedStringSet links){
		body.debug("Spidering peer vkontakte item content "+item);
		String text = HTTP.getJsonString(item,"text",null);
		JsonArray reposts = item.containsKey("copy_history") ? item.getJsonArray("copy_history") : null;
		if (!AL.empty(text)){
			body.debug("Spidering peer vkontakte item text "+text);
			content.append(text);
		}
		if (reposts != null){
			body.debug("Spidering peer vkontakte item reposts "+reposts);
			for (int j = 0; j < reposts.size(); j++)
				processContent(reposts.getJsonObject(j),content,links);
		}
	}
	*/
	
	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		try {
			//TODO:
			String access_token = api.refresh_token(token);
			if (AL.empty(access_token))
				return;
			
			String[][] hdr = new String[][] {new String[] {"Authorization","bearer "+access_token}};
			String after = null;
			for (boolean days_over = false; !days_over;) {
//TODO: throttling
				//https://www.reddit.com/dev/api#GET_user_{username}_{where}
				String api_url = "https://oauth.reddit.com/user/"+this.user_id+"/submitted";
				String params = "limit=100" + (after == null ? "" : "&after="+after);
				body.debug("Spidering peer reddit "+user_id+" request "+api_url+" "+params);
				String response = HTTP.simple(api_url+"?"+params,null,"GET",0,null,hdr);
				if (debug) body.debug("Spidering peer reddit "+user_id+" response "+response);
				if (AL.empty(response))
					break;
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonObject data = JSON.getJsonObject(json,"data");
				if (data == null)
					break;
				JsonArray children = JSON.getJsonArray(data, "children");
				if (children != null) for (int i = 0; i < children.size(); i++) {
					JsonObject item = children.getJsonObject(i);
					//String type = JSON.getJsonString(item, "type");//t1_Comment,t2_Account,t3_Link,t4_Message,t5_Subreddit,t6_Award
					item = JSON.getJsonObject(item,"data");
					String typed_id = JSON.getJsonString(item, "name");//typed id, like "t3_eglml"
					//String untyped_id = JSON.getJsonString(item, "url");//untyped id, like "eglml"
					//String link_id = JSON.getJsonString(item, "link_id");//like "t3_e5bx6b", points to origin of commet if comment
					//String subreddit = JSON.getJsonString(item, "subreddit");//like "aigents"
					String title = JSON.getJsonString(item, "title");//subject fr post
					String selftext = JSON.getJsonString(item, "selftext");//body for post
					String body = JSON.getJsonString(item, "body");//body for comment
					//String selftext_html = JSON.getJsonString(item, "selftext_html");//body html for post?
					//String body_html = JSON.getJsonString(item, "body_html");//body html for comment?
					int ups = JSON.getJsonInt(item, "ups");//number of upvotes
					int downs = JSON.getJsonInt(item, "downs");//number of downvotes
					String thumbnail = JSON.getJsonString(item, "thumbnail");//image
					Date date = JSON.getJsonDateFromUnixTime(item, "created");//decimal Unix time, seconds
					String author = JSON.getJsonString(item, "author");//like "akolonin"
					String permalink = JSON.getJsonString(item, "permalink");//add "https://www.reddit.com" to "/r/artificial/comments/eg7lml/ai_article_index_from_peter_voss/
					String url = JSON.getJsonString(item, "url");//if link
			
					if (date.compareTo(since) < 0){
						days_over = true;
						break;
					}								
					
					StringBuilder content = new StringBuilder();
					Str.append(content, title);
					Str.append(content, selftext);
					Str.append(content, body);
					String uri = Reddit.home_url + permalink; 
					OrderedStringSet links = new OrderedStringSet();
					//if (!AL.empty(uri))//no need for permlik uri to get extra linked? 
					//	links.add(uri);
					if (!AL.empty(url))
						links.add(url);
					
					//TODO: regular reporting and profiling - test
					
					//TODO: thumbail to report detail!?
					
					//TODO: alert with thumbnail for news feed!?
					//TODO: if public only!!!???
					
					//TODO: remember id and later request all comments in batch (!!!) using get_info
					//and the call reportDetail in separate loop with all comments filled!!! 
		
					String text = content.toString();
					Object[][] commenters = null;//see VK
					
					text = processItem(date,author,text,links,commenters,ups-downs,true);
					reportDetail(detail,author,uri,typed_id,text,date,null,links,null,ups-downs,0,0);
				}
				after = JSON.getJsonString(data, "after");
				if (after == null)
					break;
			}
		} catch (Exception e) {
			body.error("Spidering peer reddit "+user_id, e);
		}
	}
}
