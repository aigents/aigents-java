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
import net.webstructor.util.ReportWriter;
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
	
	Object[][] getComments(String[][] hdr,String subreddit,String untyped_id) throws IOException {
		Object[][] comments = null;
		//https://www.reddit.com/dev/api/#GET_comments_{article}
		String api_url = "https://oauth.reddit.com/r/"+subreddit+"/comments/"+untyped_id;
		//String params = "limit=100" + (after == null ? "" : "&after="+after);
		if (debug) body.debug("Spidering peer reddit "+user_id+" request "+api_url);
		String response = HTTP.simple(api_url,null,"GET",0,null,hdr);
		if (debug) body.debug("Spidering peer reddit "+user_id+" response "+Str.first(response,200));
//(new Filer(this.body)).save("comments_"+ri.typed_id+".json", response);
		JsonReader jsonReader = Json.createReader(new StringReader(response));
		JsonArray jsona = jsonReader.readArray();
		if (jsona.size() > 1) {
			JsonObject json = jsona.getJsonObject(1);
			JsonObject data = JSON.getJsonObject(json,"data");
			if (data != null) {
				JsonArray children = JSON.getJsonArray(data, "children");
				if (children != null && children.size() > 0) {
					comments = new Object[children.size()][];
					for (int i = 0; i < children.size(); i++) {
						JsonObject item = children.getJsonObject(i);
						item = JSON.getJsonObject(item,"data");
						RedditItem ri = new RedditItem(item);
						countComment(ri.author,ri.author,ri.text,ri.time);
						comments[i] = new Object[]{ri.author,ri.author,ri.text,false,new Integer(ri.score)};
					}
				}
			}
		}
		return comments;
	}
	
	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		try {
			String access_token = api.refresh_token(token);
			if (AL.empty(access_token))
				return;

			String[][] hdr = new String[][] {new String[] {"Authorization","bearer "+access_token}};
			String after = null;
			for (boolean days_over = false; !days_over;) {
//TODO: throttling based on header info or invalid replies 
				//https://www.reddit.com/dev/api#GET_user_{username}_{where}
				String api_url = "https://oauth.reddit.com/user/"+this.user_id+"/submitted";
				String params = "limit=100" + (after == null ? "" : "&after="+after);
				if (debug) body.debug("Spidering peer reddit request "+user_id+" "+api_url+" "+params);
				String response = HTTP.simple(api_url+"?"+params,null,"GET",0,null,hdr);
				if (debug) body.debug("Spidering peer reddit response "+user_id+" "+Str.first(response,200));
				if (AL.empty(response))
					break;
//(new Filer(body)).save("reddit.json",response);
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
					RedditItem ri = new RedditItem(item);
					if (!ri.is_robot_indexable || !"public".equals(ri.subreddit_type))
						continue;
					if (ri.date.compareTo(since) < 0){
						days_over = true;
						break;
					}
					
					OrderedStringSet links = new OrderedStringSet();
					if (!AL.empty(ri.url)) {
						if (!AL.isURL(ri.url))
							ri.url = Reddit.home_url + ri.url;
						links.add(ri.url);
					}
					String imghtml = null;
					if (!AL.empty(ri.thumbnail)) {
						if (AL.isIMG(ri.thumbnail))
							imghtml = ReportWriter.img(ri.uri, null, ri.thumbnail);
						else {
							if (AL.isURL(ri.thumbnail))
								links.add(ri.thumbnail);
						}
					}

					//Reddit makes it impossible to count individual ups and downs, only the overall score is presented :-( 
					//https://www.reddit.com/r/redditdev/comments/6yd9bo/how_to_get_a_list_of_people_who_upvoted_or_down/
					//https://www.reddit.com/r/redditdev/comments/854mye/get_number_of_upvotes/
					countLikes(anonymous,Anonymous,ri.date,ri.score);
					
					Object[][] comments = getComments(hdr,ri.subreddit,ri.untyped_id);
					
					String text = processItem(ri.date,ri.author,ri.text,links,comments,ri.score,true);
					reportDetail(detail,ri.author,ri.uri,ri.typed_id,text,ri.date,comments,links,null,ri.score,0,ri.comments,imghtml);

//TODO: or not todo	- update for today ad yesterday only 
					//if (today.compareTo(ri.date)>=0)
					api.matchPeerText(user_id, text, ri.date, ri.uri, ri.thumbnail);
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
