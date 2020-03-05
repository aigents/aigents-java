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

import java.util.Date;

import javax.json.JsonObject;

import net.webstructor.al.Time;
import net.webstructor.util.JSON;

//User actions:
//curl https://community.singularitynet.io/user_actions.json?username=akolonin
//https://github.com/discourse/discourse_api/blob/master/lib/discourse_api/api/user_actions.rb
//1 - liked by me
//2 - liked by other
//3 - unknown TODO? 
//4 - my topic posts
//5 - my reply posts
//6 - reply posts on my reply posts (except reply posts on my topic post) TODO workaround?
//7 - mentions of me

class DiscourseItem {
	//basic:
	Date created_at;//"created_at":"2019-06-11T11:04:13.969Z",
	int action_type;//"action_type":6,
	boolean visible;
	private boolean deleted;//"deleted":false,
	private boolean hidden;//"hidden":false,
	private boolean closed;//"closed":false,
	private boolean archived;//":false
	int category_id;//"category_id":19,
	long topic_id;//"topic_id":475,
	int post_number;//"post_number":6, - starting from 1, ordinal index of comments/replies within the topic; action_type=4 => post_number=1 (first post in topic thread)
	int reply_to_post_number;//"reply_to_post_number":2,
	int post_type;//"post_type":1,
	long post_id;//"post_id":7485,
	String text;//"excerpt":"Thanks! Bookmarked",
	String title;//"title":"Unsupervised Language Learning",
	String slug;//slug":"unsupervised-language-learning",
	//"truncated":true,
	//"action_code":null,
	//"avatar_template":"/user_avatar/community.singularitynet.io/lwflouisa/{size}/2086_2.png",
	//"acting_avatar_template":"/user_avatar/community.singularitynet.io/lwflouisa/{size}/2086_2.png",
	String target_username;//"target_username":"akolonin",
	String target_name;//"target_name":"Anton Kolonin",
	//"target_user_id":24,
	String username;//"username":"LWFlouisa",
	String name;//"name":"Sarah Weaver",
	//"user_id":932,
	String acting_username;//"acting_username":"LWFlouisa",	- can discourse user change the username? no - this seems persistent
	String acting_name;//"acting_name":"Sarah Weaver", - full name
	//int acting_user_id;//"acting_user_id":932,
	//derived:
	String permlink = null;
	String parent_permlink = null;
	
	DiscourseItem(JsonObject item) {
		created_at = Time.date(Time.time(JSON.getJsonString(item,"created_at"),"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		action_type = JSON.getJsonInt(item,"action_type");
		deleted = JSON.getJsonBoolean(item,"deleted",false);
		hidden = JSON.getJsonBoolean(item,"hidden",false);
		closed = JSON.getJsonBoolean(item,"closed",false);
		archived = JSON.getJsonBoolean(item,"archived",false);
		category_id = JSON.getJsonInt(item,"category_id");
		topic_id = JSON.getJsonLong(item,"topic_id");
		post_number = JSON.getJsonInt(item,"post_number");
		reply_to_post_number = JSON.getJsonInt(item,"reply_to_post_number");
		post_type = JSON.getJsonInt(item,"post_type");
		post_id = item.containsKey("post_id") ? JSON.getJsonLong(item,"post_id") : JSON.getJsonLong(item,"id");
		text = item.containsKey("raw") ? JSON.getJsonString(item,"raw") : JSON.getJsonString(item,"excerpt");
		title = JSON.getJsonString(item,"title");
		slug = item.containsKey("slug") ? JSON.getJsonString(item,"slug") : JSON.getJsonString(item,"topic_slug");
		target_username = JSON.getJsonString(item,"target_username");
		target_name = JSON.getJsonString(item,"target_name");
		username = JSON.getJsonString(item,"username");
		name = JSON.getJsonString(item,"name");
		if (item.containsKey("acting_username")) {
			acting_username = JSON.getJsonString(item,"acting_username");
			acting_name = JSON.getJsonString(item,"acting_name");
		} else {
			acting_username = JSON.getJsonString(item,"username");
			acting_name = JSON.getJsonString(item,"name");
		}
		
		visible = !(deleted || hidden || closed || archived);
		
if (post_number == 0) 
	System.out.println(item);//TODO ensure post_number can't be null or skipped

		switch (action_type) {
		case 4: case 5: case 6: case 7: 
			if (post_number > 1 && reply_to_post_number == 0)
				reply_to_post_number = 1;
		}

		permlink = slug + '/' + topic_id + '/' + post_number;
		parent_permlink = slug + '/' + topic_id + (reply_to_post_number > 0 ? ("/" + reply_to_post_number) : "");
	}
}

