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
package net.webstructor.comm.goog;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.Socializer;
import net.webstructor.data.SocialFeeder;

/*
https://developers.google.com/apis-explorer/#p/plus/v1/plus.people.get
https://developers.google.com/identity/sign-in/web/server-side-flow
https://developers.google.com/identity/sign-in/web/server-side-flow
https://developers.google.com/+/web/api/rest/
https://developers.google.com/+/web/samples/javascript
https://console.developers.google.com/apis/credentials?project=aigents-web-server
https://www.googleapis.com/plus/v1/people
http://stackoverflow.com/questions/13851157/oauth2-and-google-api-access-token-expiration-time

activites by user:
https://developers.google.com/+/web/api/rest/latest/activities/list

https://www.googleapis.com/plus/v1/activities?query=aigents&access_token=
https://www.googleapis.com/plus/v1/activities?orderBy=recent&maxResults=20&query=activityFeed&access_token=
https://www.googleapis.com/plus/v1/people/113174676192873877221/activities/public?access_token=

*/
public class GApi extends Socializer {
	private String appId;
	private String appSecret;
	protected HttpFileReader reader;//TODO: move up to HTTP or fix native HTTP
	
	//https://developers.facebook.com/docs/facebook-login/access-tokens/#apptokens
	public GApi(Body body, String appId, String appSecret) {
		super(body);
		this.appId = appId;
		this.appSecret = appSecret;
		this.reader = new HttpFileReader();
	}

	public String provider(){
		return "google";
	}

	public SocialFeeder getFeeder(String id, String token, String key, Date since, Date until, String[] areas) throws IOException{
		if (appId == null)//fake instance for testing 
			return null;
		GApiFeeder feeder = new GApiFeeder(this,body,id,body.languages,since,until);
		feeder.getFeed(token, key, since, until, new StringBuilder());
		return feeder;
	}
	
	static Object[][] processComments(JsonObject object, String token, String user_id) throws IOException{
		JsonObject replies = object.getJsonObject("replies");
		int replies_count = replies.getInt("totalItems");
		String repliesLink = getJsonString(replies,"selfLink",null);
		if (replies_count > 0 && repliesLink != null){
			Object[][] comments = new Object[replies_count][];
			String rurl = repliesLink+"?access_token="+token;
			String rstr = simpleGet(rurl);
			JsonReader jsonReader = Json.createReader(new StringReader(rstr));
			JsonObject feed = jsonReader.readObject();
			JsonArray items = feed.getJsonArray("items");
			if (items.size() != replies_count){
				//TODO: handle the case when comments are in the other place like youtube
				return null;
			}
			for (int i = 0; i < items.size(); i++){
				JsonObject item = items.getJsonObject(i);
				JsonObject actor = item.getJsonObject("actor");
				String id = getJsonString(actor,"id","");
				String name = getJsonString(actor,"displayName","");
				String content = getJsonString(item.getJsonObject("object"),"content","");
				HashMap likers = new HashMap();
//TODO: get likes on comments - so far, it is not possible:
//https://developers.google.com/+/web/api/rest/latest/comments#resource-representations
				boolean mylike = processLikes(item,token,user_id,"plusoners",likers);
				comments[i] = new Object[]{id,name,content,new Boolean(mylike),new Integer(likers.size())};
			}
			jsonReader.close();
			return comments;
		}	
		return null;
	}

	//https://developers.google.com/+/web/api/rest/latest/comments/get#examples
	static boolean processLikes(JsonObject object, String token, String user_id, String set, HashMap likers) throws IOException{
		boolean mylike = false;
		JsonObject likes = object.getJsonObject(set);
		int likes_count = likes.getInt("totalItems");
		String likesLink = getJsonString(likes,"selfLink",null);
		if (likes_count > 0 && likesLink != null){
			String rurl = likesLink+"?access_token="+token;
//System.out.println(rurl);
			String rstr = simpleGet(rurl);
//System.out.println(rstr);
			JsonReader jsonReader = Json.createReader(new StringReader(rstr));
			JsonObject feed = jsonReader.readObject();
			JsonArray items = feed.getJsonArray("items");
			for (int i = 0; i < items.size(); i++){
				JsonObject item = items.getJsonObject(i);
				String id = getJsonString(item,"id",null);
				String name = getJsonString(item,"displayName",null);
				if (id != null && name != null){
					if (id.equals(user_id))
						mylike = true;
					else
						likers.put(id, name);
				}
			}
			jsonReader.close();
		}
		return mylike;
	}
	
	public static final String TIME_FORMAT = "yyyy-MM-dd";
	//TODO:
	//- limit by period
	//- count my likes
	//- count likers and commenters
	//- count words
	public String reportOld(String user_id,String access_token) {
		Date since = Time.today(-365);
		StringWriter writer = new StringWriter();
		writer.write("<html><body>Stay tuned to get your <b>Google+</b> report!<br>For the time being, here is just brief summary of your activity:<br>");
		try {
			String token = URLEncoder.encode(access_token,"UTF-8");
			//String myurl = "https://www.googleapis.com/plus/v1/people/me?access_token="+token;
			//String me = simpleGet(myurl);
			//writer.write(me);
			writer.write("<br>");
			
			String base = "https://www.googleapis.com/plus/v1/people/me/activities/public?maxResults=100&access_token="+token;
			String page = "";
			for (;;){
				String url = base + page;
//System.out.println(url);
				String out = simpleGet(url);
				JsonReader jsonReader = Json.createReader(new StringReader(out));
				JsonObject feed = jsonReader.readObject();
				JsonArray items = feed.getJsonArray("items");
				boolean days_over = false;
				for (int i = 0; i < items.size(); i++){
					JsonObject item = items.getJsonObject(i);
					String date = getJsonString(item,"published");
					Date date_day = Time.time(date,TIME_FORMAT);
					if (date_day.compareTo(since) < 0){
						days_over = true;
						break;
					}

					//String verb = getJsonString(item,"verb");
					String id = getJsonString(item,"id");
					String uri = getJsonString(item,"url");
					
					JsonObject object = item.getJsonObject("object");
					String content = getJsonString(object,"content","");
					//TODO: join content and annotation if both?
					if (AL.empty(content))
						content = getJsonString(object,"annotation","");
					
					Object[][] comments = processComments(object,token,user_id);
					int replies_count = comments == null ? 0 : comments.length;

					HashMap likers = new HashMap();
					boolean mylike = processLikes(object,token,user_id,"plusoners",likers)
						|| processLikes(object,token,user_id,"resharers",likers);

					String[] links = null;
					if (AL.empty(content)){
						JsonArray attachments = object.getJsonArray("attachments");
						links = new String[attachments.size()];
						for (int j = 0; j < attachments.size(); j++){
							JsonObject attachment = attachments.getJsonObject(j);
							String attachment_content = getJsonString(attachment,"content","");
							if (AL.empty(attachment_content))
								attachment_content = getJsonString(attachment,"displayName","");
							if (AL.empty(content))
								content = attachment_content;
							String link = getJsonString(attachment,"url","");
							links[j] = link;
						}
					}
	//System.out.println(date+" : "+content);
					writer.write(date+" <a target=\"_blank\" href=\""+uri+"\">"+id+"</a>"
						+" likes:"+(mylike ? "I+":"+")+likers.size()+" comments:"+replies_count+"<br>"
						+content+"<br>");
					if (links != null){
						for (int j = 0; j < links.length; j++){
							writer.write("<u><i><a href=\""+links[j]+"\" target=\"_blank\">"+links[j]+"</a></i></u><br>");
						}
					}
					if (comments != null){
						for (int j = 0; j < comments.length; j++){
							writer.write("<i><small>&gt;&gt;"
								+comments[j][1] //peer
								+" likes:"+(((Boolean)comments[j][3]).booleanValue() ? "I+":"+")+"+"+comments[j][4] //likes
								+":"+comments[j][2] //content
								+"</small></i><br>");
						}
					}
					if (likers.size() != 0){
						for (Iterator it = likers.values().iterator(); it.hasNext();){
							writer.write("<small>+"+it.next()+"</small><br>");
						}
					}
				}
				//writer.write(out);
				if (days_over)
					break;
				String nextPageToken = getJsonString(feed,"nextPageToken");
				if (AL.empty(nextPageToken))
					break;
				page = "&pageToken="+nextPageToken;
			}
						
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.write("</body></html>");
		return writer.toString();
	}
	
	/**
	 * Validates userId and appId against accessToken
	 * @param userId
	 * @param accessToken
	 * @return true if userId is validated, false otherwise
	 * @throws Exception
	 */
	public String[] verifyToken(String userId,String code) {
		if (userId.startsWith("test"))
			return new String[]{"test@gmail.com","tesname","testsurname",code,userId,code};
		String url = "";
		String out = "";
		try {
			//NOTE!!! refresh_token here is received only upon the first authorization!!!
			//NOTE!!! to re-get refresh_token, need to remove the app from app settings:
			//https://security.google.com/settings/u/0/security/permissions
			//see more:
			//https://developers.google.com/identity/protocols/OAuth2WebServer
			//http://stackoverflow.com/questions/8942340/get-refresh-token-google-api
			//http://stackoverflow.com/questions/10827920/not-receiving-google-oauth-refresh-token/10857806#10857806
			//https://developers.google.com/identity/protocols/OAuth2WebServer#offline
			
			url = "https://accounts.google.com/o/oauth2/token";
			String par = "code=" + URLEncoder.encode(code,"UTF-8")
				+ "&redirect_uri=postmessage&client_id=" + URLEncoder.encode(appId,"UTF-8")
				+ "&client_secret=" + URLEncoder.encode(appSecret,"UTF-8")
				+ "&grant_type=authorization_code";
			
			//body.debug("Google+ request: " + url+ "/"+par);
			out = sendPost(url,par);
			//body.debug("Google+ response: " + out);
			JsonReader jr = Json.createReader(new StringReader(out));
			JsonObject jauth = jr.readObject();
			String access_token = jauth.getString("access_token");
			String refresh_token = jauth.containsKey("refresh_token") ? jauth.getString("refresh_token") : null;
			//int expires_in_seconds = jauth.getInt("expires_in");
			jr.close();

			url = "https://www.googleapis.com/plus/v1/people/me?access_token="+URLEncoder.encode(access_token,"UTF-8");
			//body.debug("Google+ request: " + url);
			out = simpleGet(url);
			//body.debug("Google+ response: " + out);
			
			jr = Json.createReader(new StringReader(out));
			JsonObject jme = jr.readObject();
			String id = jme.getString("id");
			JsonObject nameobj = jme.getJsonObject("name");
			String name = nameobj.getString("givenName");
			String surname = nameobj.getString("familyName");
			if (AL.empty(name) || AL.empty(surname)){
				String displayName = jme.getString("displayName");
				String[] names = displayName.split(" ");
				if (!AL.empty(names)){
					name = names[0];
					surname = names.length > 1 ? displayName.substring(name.length()).trim(): names[0];
				}
			}
			String email = id+"@google.com";
			JsonArray emails = jme.getJsonArray("emails");
    		for (int i = 0; i < emails.size(); i++){
    			JsonObject e = emails.getJsonObject(i);
    			String type = e.getString("type");
    			if (type.equals("account")){
    				email = e.getString("value").toString();
    				break;
    			}
    		}
			jr.close();
			//TODO: remove hack replacing access_token with refresh_token!?
			//return new String[]{email,name,surname,access_token,String.valueOf(id),refresh_token};
			return new String[]{email,name,surname,access_token,String.valueOf(id),refresh_token};
		} catch (Exception e) {
			body.error("Can't verify Google+ code url "+url+" out "+out, e);
			return null;
		}
	}
	
}
