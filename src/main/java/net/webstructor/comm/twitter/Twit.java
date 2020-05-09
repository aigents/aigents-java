/*
 * MIT License
 * 
 * Copyright (c) 2019-2020 by Anton Kolonin, Aigents®
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
package net.webstructor.comm.twitter;

import java.util.Date;

import javax.json.JsonArray;
import javax.json.JsonObject;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.util.JSON;

class Twit {
	Date created_at;
	String id_str;
	String in_reply_to_status_id_str;
	String in_reply_to_user_id_str;
	String in_reply_to_screen_name;
	String text;
	String lang;
	int retweet_count;
	int favorite_count;
	boolean retweeted;
	boolean favorited;
	String user_id_str = null;
	String user_name = null;
	String user_screen_name = null;
	String user_profile_image = null;
	String url;
	int score;
	boolean scored;
	OrderedStringSet links = new OrderedStringSet();
	OrderedStringSet media = new OrderedStringSet();
	String image = null;

	public Twit(JsonObject tweet) {
		created_at = Time.time(JSON.getJsonString(tweet,"created_at"),"E MMM dd HH:mm:ss Z yyyy");//Mon May 04 06:06:10 +0000 2020
		id_str = JSON.getJsonString(tweet, "id_str");
		in_reply_to_status_id_str = JSON.getJsonString(tweet, "in_reply_to_status_id_str");
		in_reply_to_user_id_str = JSON.getJsonString(tweet, "in_reply_to_user_id_str");
		in_reply_to_screen_name = JSON.getJsonString(tweet, "in_reply_to_screen_name");
		text = tweet.containsKey("full_text") ? JSON.getJsonString(tweet, "full_text") : JSON.getJsonString(tweet, "text");
		lang = JSON.getJsonString(tweet, "lang");
		retweet_count = JSON.getJsonInt(tweet, "retweet_count");
		favorite_count = JSON.getJsonInt(tweet, "favorite_count");
		retweeted = JSON.getJsonBoolean(tweet, "retweeted", false);
		favorited = JSON.getJsonBoolean(tweet, "favorited", false);
		JsonObject user = JSON.getJsonObject(tweet, "user");
		if (user != null) {
			user_id_str = JSON.getJsonString(user, "id_str");
			user_name = JSON.getJsonString(user, "name");
			user_screen_name = JSON.getJsonString(user, "screen_name");
			user_profile_image = JSON.getJsonString(user, "profile_image_url_https");
		}
		JsonObject ent = JSON.getJsonObject(tweet, "entities");
		if (ent != null) {
			JsonArray urls = JSON.getJsonArray(ent, "urls");
			if (urls != null) for (int u = 0; u < urls.size(); u++) {
				JsonObject o = urls.getJsonObject(u);
				if (o != null) {
					String url = JSON.getJsonString(o, "expanded_url");
					if (AL.empty(url))
						url = JSON.getJsonString(o, "url");
					if (!AL.empty(url))
						this.links.add(url);
				}
			}
			JsonArray media = JSON.getJsonArray(ent, "media");
			if (media != null) for (int m = 0; m < media.size(); m++) {
				JsonObject o = media.getJsonObject(m);
				if (o != null) {
					String url = JSON.getJsonString(o, "media_url_https");
					if (AL.empty(url))
						url = JSON.getJsonString(o, "expanded_url");//TODO media_url?
					if (!AL.empty(url))
						this.media.add(url);
				}
			}
		}
		url = String.format("https://twitter.com/%s/status/%s", user_screen_name, id_str);//https://twitter.com/aigents/status/1257358818285142018
		score = favorite_count - (favorited ? 1 : 0) + retweet_count - (retweeted ? 1 : 0);
		scored = favorited | retweeted;
		OrderedStringSet image_candidates = !AL.empty(media) ? media : links;
		for (int l = 0; l < image_candidates.size(); l++) {
			String link = (String)image_candidates.get(l);
			if (AL.isIMG(link)) {
				image = link;
				break;
			}
		}
	}
}

