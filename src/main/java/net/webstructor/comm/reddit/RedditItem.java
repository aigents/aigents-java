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


import java.util.Date;

import javax.json.JsonObject;

import net.webstructor.al.Time;
import net.webstructor.util.JSON;
import net.webstructor.util.Str;

class RedditItem {
	String subreddit_type;//like "public"
	boolean is_robot_indexable;
	String typed_id;//typed id, like "t3_eglml"
	String untyped_id;//untyped id, like "eglml"
	//String link_id = JSON.getJsonString(item, "link_id");//like "t3_e5bx6b", points to origin of commet if comment
	String subreddit;//like "aigents"
	String title;//subject fr post
	String selftext;//body for post
	String body;//body for comment
	//String selftext_html;//body html for post?
	//String body_html;//body html for comment?
	//int ups;//number of upvotes
	//int downs;//number of downvotes
	int score;//=ups-downs
	int comments;
	String thumbnail;//image
	String author;//like "akolonin"
	String permalink;//add "https://www.reddit.com" to "/r/artificial/comments/eg7lml/ai_article_index_from_peter_voss/
	String url;//if link
	Date time;//decimal Unix time, seconds
	Date date;//day
	String text;
	String uri;//permlink uri
	
	RedditItem(JsonObject item){
		subreddit_type = JSON.getJsonString(item, "subreddit_type");//like "public"
		is_robot_indexable = JSON.getJsonBoolean(item, "is_robot_indexable", true);
		typed_id = JSON.getJsonString(item, "name");//typed id, like "t3_eglml"
		untyped_id = JSON.getJsonString(item, "id");//untyped id, like "eglml"
		//String link_id = JSON.getJsonString(item, "link_id");//like "t3_e5bx6b", points to origin of commet if comment
		subreddit = JSON.getJsonString(item, "subreddit");//like "aigents"
		title = JSON.getJsonString(item, "title");//subject fr post
		selftext = JSON.getJsonString(item, "selftext");//body for post
		body = JSON.getJsonString(item, "body");//body for comment
		//String selftext_html = JSON.getJsonString(item, "selftext_html");//body html for post?
		//String body_html = JSON.getJsonString(item, "body_html");//body html for comment?

		//https://www.reddit.com/r/announcements/comments/28hjga/reddit_changes_individual_updown_vote_counts_no/
		//ups = JSON.getJsonInt(item, "ups");//number of upvotes
		//downs = JSON.getJsonInt(item, "downs");//number of downvotes
		score = JSON.getJsonInt(item, "score");//=ups-downs
		
		comments = JSON.getJsonInt(item, "num_comments");
		thumbnail = JSON.getJsonString(item, "thumbnail");//image
		author = JSON.getJsonString(item, "author");//like "akolonin"
		permalink = JSON.getJsonString(item, "permalink");//add "https://www.reddit.com" to "/r/artificial/comments/eg7lml/ai_article_index_from_peter_voss/
		url = JSON.getJsonString(item, "url");//if link
		time = JSON.getJsonDateFromUnixTime(item, "created");//decimal Unix time, seconds
		date = Time.date(time);//day
		StringBuilder content = new StringBuilder();
		Str.append(content, title);
		Str.append(content, selftext);
		Str.append(content, body);
		text = content.toString();
		uri = Reddit.home_url + permalink; 
	}
}
