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
import net.webstructor.util.Reporter;
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
	
	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		try {
			//TODO:
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
				body.debug("Spidering peer reddit "+user_id+" request "+api_url+" "+params);
				String response = HTTP.simple(api_url+"?"+params,null,"GET",0,null,hdr);
				if (debug) body.debug("Spidering peer reddit "+user_id+" response "+response);
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
					String subreddit_type = JSON.getJsonString(item, "subreddit_type");//like "public"
					boolean is_robot_indexable = JSON.getJsonBoolean(item, "is_robot_indexable", true);
					if (!is_robot_indexable || !"public".equals(subreddit_type))
						continue;
					Date date = JSON.getJsonDateFromUnixTime(item, "created");//decimal Unix time, seconds
					if (date.compareTo(since) < 0){
						days_over = true;
						break;
					}								
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
					int comments = JSON.getJsonInt(item, "num_comments");
					String thumbnail = JSON.getJsonString(item, "thumbnail");//image
					String author = JSON.getJsonString(item, "author");//like "akolonin"
					String permalink = JSON.getJsonString(item, "permalink");//add "https://www.reddit.com" to "/r/artificial/comments/eg7lml/ai_article_index_from_peter_voss/
					String url = JSON.getJsonString(item, "url");//if link
			
					StringBuilder content = new StringBuilder();
					Str.append(content, title);
					Str.append(content, selftext);
					Str.append(content, body);
					String uri = Reddit.home_url + permalink; 
					OrderedStringSet links = new OrderedStringSet();
					if (!AL.empty(url)) {
						if (!AL.isURL(url))
							url = Reddit.home_url + url;
						links.add(url);
					}
					String imghtml = null;
					if (!AL.empty(thumbnail)) {
						if (AL.isIMG(thumbnail))
							imghtml = Reporter.img(uri, null, thumbnail);
						else {
							if (AL.isURL(thumbnail))
								links.add(thumbnail);
						}
					}

//TODO: remember it and later request all comments in batch (!!!) using get_info
//and the call reportDetail in separate loop with all comments filled!!! 
					Object[][] commenters = null;//see VK
					
					String text = content.toString();
					text = processItem(date,author,text,links,commenters,ups-downs,true);
					reportDetail(detail,author,uri,typed_id,text,date,null,links,null,ups-downs,0,comments,imghtml);

					api.matchPeerText(user_id, text, date, uri, thumbnail);
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
