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
package net.webstructor.comm.vk;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.al.AL;
import net.webstructor.al.Writer;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Environment;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;

class VKFeeder extends SocialFeeder {
	//private HTTP api;
	private HashMap groupNames = new HashMap();//TODO:externalize?
	private String user_name = null;

	boolean vkdebug;
	//TODO: see https://vk.com/dev/versions
	public static final String vkversion = "4.0";//"5.21";//5.44?
	
	public VKFeeder(Environment body, HTTP api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		//this.api = api;
		vkdebug = user_id.equals("1216128") || user_id.equals("49817193") ? true : false;	
	}

	void sleep(){
		//https://vk.com/dev/api_requests => 5/second?
		//try {Thread.sleep(250);} catch (InterruptedException e) {}//1/4 second - fails?		
		//try {Thread.sleep(350);} catch (InterruptedException e) {}//1/3 second - fails?
		//try {Thread.sleep(500);} catch (InterruptedException e) {}//1/2 second - works!
		try {Thread.sleep(1000);} catch (InterruptedException e) {}//1 second - twice to account for concurrent requests!!!
	}
	
	private String nameByAuthor(int id){
		return id < 0 ? (String)groupNames.get(String.valueOf(-id)) : getUserName(String.valueOf(id));
	}

	public String processItem(Date times,String from,String html, OrderedStringSet links, Object[][] commenters, int otherLikesCount, boolean myLike){
		if (html != null){
			int commentsCount = countCommentsFromOthers(commenters);
			Counter period = getWordsPeriod(getPeriodKey(times));
			countPeriod(times,otherLikesCount,commentsCount);
			
			//TODO: promote this code to be generic for all sources!?
			ArrayList collectedLinks = new ArrayList();
			//parse link tags from html with stripped anchors
			String text = HtmlStripper.convert(html," ",collectedLinks);//.toLowerCase();
			for (int l = 0; l < collectedLinks.size(); l++) //translate url+text pairs to single urls
				collectedLinks.set(l, ((String[])collectedLinks.get(l))[0] );
			for (int i = 0; i < collectedLinks.size(); i++)
				links.add(collectedLinks.get(i));
			
			Integer comments = new Integer(commentsCount);
			Integer likes = new Integer(otherLikesCount);
			Boolean like = new Boolean(myLike); 
			String[] sources = extractUrls(text,null,like,likes,comments,period);
			if (sources != null)
				for (int i = 0; i < sources.length; i++)
					links.add(sources[i]);
			if (!from.equals(user_id)){//treat posts of others as comments
				if (myLike)
					countMyLikes(from,null);
				//TODO: fix null
				countComments(from,null,text,times);
			}else{
				sources = (String[])links.toArray(new String[]{});
				Object[] news_item = new Object[]{like,likes,comments,times,text,sources};
				news.add(news_item);
			}
			return text;
		}
		return null;
	}

	Object[][] processComments(String token,int id) throws IOException{
		//TODO:process all comments, not just first 100 as now
		String url = "https://api.vk.com/method/wall.getComments?v="+vkversion+"&post_id="+id+"&need_likes=1&sort=desc&count=100"
				+"&access_token="+token;
if (vkdebug) body.debug("Spidering peer vkontakte url:"+url);		
		sleep();
		String out = HTTP.simpleGet(url);
if (vkdebug) body.debug("Spidering peer vkontakte out:"+out);		
		JsonReader jr = Json.createReader(new StringReader(out));
		JsonObject jobj = jr.readObject();
		JsonArray comments = jobj.getJsonArray("response");
		ArrayList collected = new ArrayList();
		if (comments != null)
		for (int i = 1; i < comments.size(); i++){//first is length
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
				name = user_name;
				if (likes_count > 0){//count likes on me if liked by others
					HashMap likers = new HashMap();
					user_likes = extractLikes(token, "comment", comment.getInt("cid"), likers);
					processLikes(likers,date_day);//count who liked my comment
				}
				extractUrls(text, null, new Boolean(user_likes > 0),new Integer(likes_count - user_likes), new Integer(0), period);
			}else{//texts by others
				//TODO: process "unfamiliar" users (obtained with "extended=1" which is now not working with VK) 
				Object[] user = getUser(from_id,null);
				if (user == null)
					continue;
				name = getUserName(user);
if (vkdebug)
body.debug("Spidering peer vkontakte "+user_id+" other user text:"+from_id+" "+name);
				if (user_likes == 0)//maybe just VK bug
					user_likes = extractLikes(token, "comment", comment.getInt("cid"), null);				
				countComments(from_id,name,text,date_day);
				if (user_likes > 0){//if likedb by me, count links on author and acquire liked words
					countMyLikes(from_id,name);
					extractUrls(text, null, new Boolean(user_likes > 0),new Integer(likes_count - user_likes), new Integer(0), period);
				}
			}
			collected.add(new Object[]{from_id,name,text,new Boolean(user_likes == 1),new Integer(likes_count - user_likes)});
		}
		jr.close();
		return (Object[][]) collected.toArray(new Object[][]{});
	}
	
	private int extractLikes(String token, String type, int id, Map likers) throws IOException{
		String url = "https://api.vk.com/method/likes.getList?v="+vkversion+"&type="+type
				+"&extended=1&item_id="+id
				+"&access_token="+token;
		sleep();
if (vkdebug) body.debug("Spidering peer vkontakte url:"+url);		
		String out = HTTP.simpleGet(url);
if (vkdebug) body.debug("Spidering peer vkontakte out:"+out);		
		JsonReader jr = Json.createReader(new StringReader(out));
		JsonObject jobj = jr.readObject();
		if (jobj == null || !jobj.containsKey("response"))
			return 0;
		jobj = jobj.getJsonObject("response");
		if (jobj == null || !jobj.containsKey("items"))
			return 0;
		JsonArray users = jobj.getJsonArray("items");
		int user_likes = 0;
		for (int i = 0; i < users.size(); i++){
			JsonObject user = users.getJsonObject(i);
			String uid = String.valueOf(user.getInt("uid"));
			String name = HTTP.getJsonString(user,"first_name",uid);
			String surname = HTTP.getJsonString(user,"last_name",uid);
			if (uid.equals(user_id))
				user_likes = 1;
			else
			if (likers != null){
				getUser(uid,name,surname,null);//TODO:skip the fact that have no image in such case?
				likers.put(uid,getUserName(uid));
if (vkdebug) body.debug("Spidering peer vkontakte likes "+user_id+" other user like after :"+uid+" "+name+" "+surname+" => "+getUserName(uid));
			}
		}
		jr.close();
		return user_likes;
	}
	
	void extractAndCountLikes(String token,String from,int id,Map likers,Date time) throws IOException{
		extractLikes(token, "post", id, likers);
		if (likers.size() > 0 && from.equals(user_id))
			for (Iterator it = likers.keySet().iterator(); it.hasNext();)
				countLikes((String)it.next(),null,time);
	}
	
	//from the wall
	//https://vk.com/dev/wall.get
	//TODO: friend walls
	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
	  try {
		this.detail = detail;
		
		//String base = "https://api.vk.com/method/newsfeed.get?v=5.44&access_token=" + token;
		//TODO: use v=5.44 when fixed (now it is not working due to "Validation required: please open redirect_uri in browser")
		String base = "https://api.vk.com/method/wall.get?v="+vkversion+"&extended=1&count=100&access_token=" + token;
		String url = "";
		String out = "";
		int offset = 0;
		for (boolean days_over = false; !days_over; offset += 100){
			url = base + "&offset="+offset;
			sleep();
if (vkdebug) body.debug("Spidering peer vkontakte url:"+url);		
			out = HTTP.simpleGet(url);
if (vkdebug) body.debug("Spidering peer vkontakte out:" + (out != null && out.length() > 100 ? out.substring(0,100) : out));	
							
			JsonReader jr = Json.createReader(new StringReader(out));
			JsonObject jauth = jr.readObject();

			//TODO: handle "response:{"error":{"error_code":5,"error_msg":"User authorization failed: user revoke access for this token."
			if (jauth == null || jauth.containsKey("error"))
				break;
			
if (vkdebug) body.debug("Spidering peer vkontakte noerror1");		
			
			if (jauth == null || !jauth.containsKey("response"))
				break;
			JsonObject response = jauth.getJsonObject("response");
			
			//TODO: handle "response:{"error":{"error_code":5,"error_msg":"User authorization failed: user revoke access for this token."
			if (response == null || response.containsKey("error"))
				break;

if (vkdebug) body.debug("Spidering peer vkontakte noerror2");		
			
			if (response == null || !response.containsKey("wall"))//was "wall"
				break;
			JsonArray items = response.getJsonArray("wall");//was "wall"
			if (items == null || items.size() <= 1)//because first item contains count in API v 3.0
				break;

if (vkdebug) body.debug("Spidering peer vkontakte has items");		
			
			//build map of groups for referencing 
			JsonArray groups = response.getJsonArray("groups");
			for (int i = 0; i < groups.size(); i++){
				JsonObject group = groups.getJsonObject(i);
				String id = String.valueOf(group.getInt("gid"));
				String name = HTTP.getJsonString(group,"name",id);
				groupNames.put(id,name);
if (vkdebug) body.debug("Spidering peer vkontakte group "+id+" "+name);		
			}
			
if (vkdebug) body.debug("Spidering peer vkontakte has groups");		
			
			//build map of users for referencing 
			JsonArray users = response.getJsonArray("profiles");
			for (int i = 0; i < users.size(); i++){
				JsonObject user = users.getJsonObject(i);
				String id = String.valueOf(user.getInt("uid"));
				String name = HTTP.getJsonString(user,"first_name",id);
				String surname = HTTP.getJsonString(user,"last_name",id);
				String image = HTTP.getJsonString(user,"photo",null);
				if (id != null){
					if (id.equals(user_id))
						user_name = name + ' ' + surname;
					else
						getUser(id,name,surname,image);
				}
if (vkdebug) body.debug("Spidering peer vkontakte user "+id+" "+name+" "+surname);		
			}

if (vkdebug) body.debug("Spidering peer vkontakte items "+user_id+" items:"+(items.size()-1));
			
			for (int i = 1; i < items.size(); i++){//NOTE: skip 0 as it contains N of elements in API v 3.0
				JsonObject item = items.getJsonObject(i);
				StringBuilder content = new StringBuilder();
				long timestamp = item.getInt("date");
				Date date = new Date( timestamp * 1000 );
				
if (vkdebug) body.debug("Spidering peer vkontakte since "+user_id+" since:"+since+" post:"+date);
if (vkdebug) body.debug("Spidering peer vkontakte item "+item);
		
				if (date.compareTo(since) < 0){
					days_over = true;
					break;
				}								
				int from_id = item.getInt("from_id");
				String from = String.valueOf(from_id);
				//TODO: skip group news for now, handle them later
				if (from_id < 0)
					continue;

				//TODO: skip others for now, deal with them later somehow
				//if (!user_id.equals(from))
				//	continue;
if (vkdebug) body.debug("Spidering peer vkontakte item_text "+item);
				
				String text = HTTP.getJsonString(item,"text",null);
				if (!AL.empty(text))
					content.append(text);

				JsonObject comments = item.getJsonObject("comments");
				int comments_count = comments == null ? 0 : comments.getInt("count");
				JsonObject likes = item.getJsonObject("likes");
				int likes_count = likes == null ? 0 : likes.getInt("count");
				int user_likes = likes == null ? 0 : likes.getInt("user_likes");
				
				//TODO: how to count reposts - via user-based overlay with likes?
				/*JsonObject reposts = item.getJsonObject("reposts");
				int reposts_count = reposts == null ? 0 : reposts.getInt("count");
				int user_reposted = reposts == null ? 0 : reposts.getInt("user_reposted");*/
				
				//https://vk.com/dev/attachments_w
				OrderedStringSet links = new OrderedStringSet();
				JsonObject attachment = item.containsKey("attachment") ? item.getJsonObject("attachment") : null;
				JsonArray attachments = item.containsKey("attachments") ? item.getJsonArray("attachments") : null;
				if (attachment != null)
					processAttachment(attachment,content,links);
				if (attachments != null){
					for (int j = 0; j < attachments.size(); j++){
						attachment = attachments.getJsonObject(j);
						processAttachment(attachment,content,links);
					}
				}
				links.sort();

				//extract likers for reports
				HashMap likers = new HashMap();
				int id = item.getInt("id");
				if (likes_count > 0)
					extractAndCountLikes(token,from,id,likers,date);
				
				Object[][] commenters = comments_count == 0 ? null : processComments(token,id);
				
				//TODO: resharers?
				text = processItem(date,from,content.toString(),links,commenters,likes_count-user_likes,user_likes==1?true:false);

				//dump to html buffer for report details section
				reportDetail(detail,getUserName(from),null,null,text,date,commenters,links,likers,
					likes_count-user_likes,user_likes,comments_count);
				
			}//individual item
		}//blocks X 100
		if (news.isEmpty())
			body.debug("Spidering peer vkontakte "+user_id+" no results, request:"+url+", response:"+out);
		else
			addPerCommentWords();//add per-user-comment word counts to per-post word counts
		cleanNonReferencedPeers();//clear peer loaded by profile
if (vkdebug)
for (Iterator it = users.values().iterator(); it.hasNext();)
body.debug("Spidering peer vkontakte "+user_id+" peer:"+Writer.toString(it.next()));

	  } catch (Exception e) {
		  body.error("Spidering peer vkontakte "+user_id, e);
	  }
	}

	void processAttachment(JsonObject attachment,StringBuilder content,OrderedStringSet links){
		String type = HTTP.getJsonString(attachment,"type","");
		if (type.equals("link") || type.equals("video") || type.equals("photo")){
			JsonObject link = attachment.getJsonObject(type);//link|video|photo
			if (content.length() == 0){
				String text = HTTP.getJsonString(link,"text",null);
				String title = HTTP.getJsonString(link,"title",null);
				String description = HTTP.getJsonString(link,"description",null);
				if (!AL.empty(text))
					appendSpaced(content,text);
				if (!AL.empty(title))
					appendSpaced(content,title);
				if (!AL.empty(description))
					appendSpaced(content,description);
				if (content.length() == 0 && link.containsKey("owner_id"))
					content.append(nameByAuthor(link.getInt("owner_id")));
			}
			//TODO: sort out if there is a way to get true url to video 
			//... now it seems like video is available only by id and just image is given for thumb-nail 
			if (link.containsKey("url"))
				links.add(HtmlStripper.stripHtmlAnchor(VK.getJsonString(link,"url","")));
			else
			if (link.containsKey("src"))
				links.add(HtmlStripper.stripHtmlAnchor(VK.getJsonString(link,"src","")));
			else
			if (link.containsKey("image"))
				links.add(HtmlStripper.stripHtmlAnchor(VK.getJsonString(link,"image","")));
		}
	}

	/*
	https://vk.com/dev/newsfeed.get
	https://api.vk.com/method/newsfeed.get?v=5.44&access_token=<token>
	https://vk.com/dev/likes.getList
	//TODO: since this is not supported for sites, maybe use it when possible for stand-alone/mobile flow
	//TODO: use start_from instead of offset, using v=5.44, which is not working on server side (if version is specified)
	*//*
	public void getNewsFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		if (token != null)
			return;
		//TODO: other than filters=post
		String base = "https://api.vk.com/method/newsfeed.get?filters=post&access_token=" + token;
		//String from = null;
		int offset = -1;
		for (;;){
			//TODO: use start_from when fixed
			//String url = from == null ? base : base + "&start_from="+from;
			String url = offset == -1 ? base : base + "&offset="+offset;
//System.out.println("vkontakte url:"+url);
			sleep();
			String out = HTTP.simpleGet(url);
//System.out.println("vkontakte out:"+out);
//System.out.println("vkontakte out:OK");
			JsonReader jr = Json.createReader(new StringReader(out));
			JsonObject jauth = jr.readObject();
			JsonObject response = jauth.getJsonObject("response");
			JsonArray items = response.getJsonArray("items");
			for (int i = 0; i < items.size(); i++){
				StringBuilder content = new StringBuilder();
				
				JsonObject item = items.getJsonObject(i);
				long timestamp = item.getInt("date");
				Date date = new Date( timestamp * 1000 ); 
				String text = HTTP.getJsonString(item,"text",null);
				if (!AL.empty(text))
					content.append(text);
				
				JsonObject comments = item.getJsonObject("comments");
				int comments_count = comments == null ? 0 : comments.getInt("count");
				JsonObject likes = item.getJsonObject("likes");
				int likes_count = likes == null ? 0 : likes.getInt("count");
				int user_likes = likes == null ? 0 : likes.getInt("user_likes");
				JsonObject reposts = item.getJsonObject("reposts");
				int reposts_count = reposts == null ? 0 : reposts.getInt("count");
				int user_reposted = reposts == null ? 0 : reposts.getInt("user_reposted");
				
				//https://vk.com/dev/attachments_w
				HashSet links = new HashSet();
				JsonObject attachment = item.containsKey("attachment") ? item.getJsonObject("attachment") : null;
				JsonArray attachments = item.containsKey("attachments") ? item.getJsonArray("attachments") : null;
				if (attachment != null)
					processAttachment(attachment,content,links);
				if (attachments != null){
					for (int j = 0; j < attachments.size(); j++){
						attachment = attachments.getJsonObject(j);
					}
				}
			}
			//from = HTTP.getJsonString(response,"new_from",null);
			//if (AL.empty(from))
			//	from = HTTP.getJsonString(response,"next_from",null);
			offset = response.isNull("new_offset") ? -1 : response.getInt("new_offset");
			jr.close();
			if (items.size() == 0)
				break;
		}
	}*/

}
