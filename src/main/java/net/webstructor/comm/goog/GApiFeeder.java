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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Environment;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;

class GApiFeeder extends SocialFeeder {

	public static final String TIME_FORMAT = "yyyy-MM-dd"; 

	protected GApi gapi;
	
	public GApiFeeder(GApi gapi, Environment body, String user_id, LangPack langPack, Date since, Date until) {
		super(body, user_id, langPack, false, since, until);
		this.gapi = gapi;
	}
	
	public void processComment(Object[] comment,Date time){
		//{id,name,content,new Boolean(mylike),new Integer(likers.size())};
		String id = (String)comment[0];
		String name = (String)comment[1];
		//TODO: word proximity
		String message = (String)comment[2];
		boolean like = ((Boolean)comment[3]).booleanValue();
		if (like)
			countMyLikes(id,name);
		countComments(id,name,message,time);
	}
	
	public void processComments(Object[][] comments,Date time){
		if (!AL.empty(comments))
			for (int i = 0; i < comments.length; i++)
				if (!AL.empty(comments[i]))
					processComment(comments[i],time);
	}
		
	private String processActivity(Date times,String id,String uri,String html,
		Object[][]comments,boolean mylike,HashMap likers,OrderedStringSet allLinks){
		if (html != null){
			ArrayList collectedLinks = new ArrayList();
			//parse link tags from html with stripped anchors
			String text = HtmlStripper.convert(html," ",collectedLinks);//.toLowerCase();
			for (int l = 0; l < collectedLinks.size(); l++) //translate url+text pairs to single urls
				collectedLinks.set(l, ((String[])collectedLinks.get(l))[0] );
			for (int i = 0; i < collectedLinks.size(); i++)
				allLinks.add(collectedLinks.get(i));
			
			Boolean like = new Boolean(mylike);
			Integer likes = new Integer(likers.size());
			Integer commentsCount = new Integer(comments == null ? 0 : comments.length);
			
			Counter period = getWordsPeriod(getPeriodKey(times));
			countPeriod(times,likers.size(),countCommentsFromOthers(comments));
		
			String[] sources = extractUrls(text,null,like,likes,commentsCount,period);
			if (sources != null)
				for (int i = 0; i < sources.length; i++)
					allLinks.add(sources[i]);
			Object[] news_item = new Object[]{like,likes,commentsCount,times,text,(String[])allLinks.toArray(new String[]{})};
			news.add(news_item);
			
			processLikes(likers,times);
			processComments(comments,times);
			return text;
		}		
		return null;
	}
	
	public void getFeed(String access_token, String refresh_token, Date since, Date until, StringBuilder detail) throws IOException {
		this.detail = detail;
		String request = "";
		String response = "";
		try {
			String token = null;
			//TODO: move this out to refreshToken?
			//TODO: get short-term access_token by refresh_token:
			//https://developers.google.com/identity/protocols/OAuth2WebServer
			//http://stackoverflow.com/questions/8942340/get-refresh-token-google-api
			//http://stackoverflow.com/questions/10827920/not-receiving-google-oauth-refresh-token/10857806#10857806
			//https://developers.google.com/identity/protocols/OAuth2WebServer#offline
			//https://security.google.com/settings/u/0/security/permissions
			if (!AL.empty(refresh_token)) {
				//TODO:move peer.refresh_token to peer.appSecret?
				String appId = ((Body)body).self().getString(Body.google_id);
				String appSecret = ((Body)body).self().getString(Body.google_key);
				
				String url = "https://accounts.google.com/o/oauth2/token";
				String par = "refresh_token=" + URLEncoder.encode(refresh_token,"UTF-8")
					+ "&client_id=" + URLEncoder.encode(appId,"UTF-8")
					+ "&client_secret=" + URLEncoder.encode(appSecret,"UTF-8")
					+ "&grant_type=refresh_token";
				request = url+ "/"+par;
				//body.debug("Google+ feeder request: " + request);
				String auth = gapi.sendPost(url,par);
				response = auth;
				//body.debug("Google+ feeder response: " + auth);
				JsonReader jr = Json.createReader(new StringReader(auth));
				JsonObject jauth = jr.readObject();
				if (jauth.containsKey("access_token"))	
					token = jauth.getString("access_token");
				jr.close();
			}
			if (AL.empty(token)) {
				body.error("Google+ feeder can't refresh token "+user_id,null);
				//TODO: why do we need this? fallback for refresh_token not working?  
				token = access_token;
			}
			
			token = URLEncoder.encode(token,"UTF-8");
			String base = "https://www.googleapis.com/plus/v1/people/me/activities/public?maxResults=100&access_token="+token;
			String page = "";
			for (;;){
				String url = base + page;
				//body.debug("Google+ feeder request: " + url);
				request = url;
				String out = HTTP.simpleGet(url);
				response = out;
				//body.debug("Google+ feeder response: " + out);
				JsonReader jsonReader = Json.createReader(new StringReader(out));
				JsonObject feed = jsonReader.readObject();
				JsonArray items = feed.getJsonArray("items");
				boolean days_over = false;
				for (int i = 0; i < items.size(); i++){
					JsonObject item = items.getJsonObject(i);
					String date = HTTP.getJsonString(item,"published");
					Date date_day = Time.time(date,TIME_FORMAT);
					if (date_day.compareTo(since) < 0){
						days_over = true;
						break;
					}

					//String verb = getJsonString(item,"verb");
					String id = HTTP.getJsonString(item,"id");
					String uri = HTTP.getJsonString(item,"url");
					
					JsonObject object = item.getJsonObject("object");
					String content = HTTP.getJsonString(object,"content","");
					//TODO: join content and annotation if both?
					if (AL.empty(content))
						content = HTTP.getJsonString(object,"annotation","");
					
					Object[][] comments = GApi.processComments(object,token,user_id);

					HashMap likers = new HashMap();
					boolean mylike = GApi.processLikes(object,token,user_id,"plusoners",likers)
						|| GApi.processLikes(object,token,user_id,"resharers",likers);

					JsonArray attachments = object.getJsonArray("attachments");
					OrderedStringSet links = new OrderedStringSet();
					if (attachments != null){
						for (int j = 0; j < attachments.size(); j++){
							JsonObject attachment = attachments.getJsonObject(j);
							String type = HTTP.getJsonString(attachment,"objectType","");
							if (!AL.empty(content) && (type.equals("photo") || type.equals("video")))
								continue;
							String attachment_content = HTTP.getJsonString(attachment,"content","");
							if (AL.empty(attachment_content))
								attachment_content = GApi.getJsonString(attachment,"displayName","");
							if (AL.empty(content))
								content = attachment_content;
							String link = HtmlStripper.stripHtmlAnchor(GApi.getJsonString(attachment,"url",""));
							links.add(link);
						}
					}
					links.sort();
					
					String text = processActivity(date_day,id,uri,content,comments,mylike,likers,links);

					reportDetail(detail, null, uri, id, text, date_day, comments, links, likers, likers.size(), (mylike?1:0), (comments!=null?comments.length:0));
				}
				if (days_over)
					break;
				String nextPageToken = HTTP.getJsonString(feed,"nextPageToken");
				if (AL.empty(nextPageToken))
					break;
				page = "&pageToken="+nextPageToken;
			}
			addPerCommentWords();//add per-user-comment word counts to per-post word counts
						
		} catch (Exception e) {
			body.error("Spidering peer Google+ feeder user "+user_id+" request "+request+" response"+response,e);
		}
	}
	
}
