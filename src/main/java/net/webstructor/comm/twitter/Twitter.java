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

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.comm.Crawler;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.data.LangPack;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Profiler;
import net.webstructor.util.MapMap;
import net.webstructor.util.Reporter;
import net.webstructor.util.Str;

/*
https://developer.twitter.com/en/docs/basics/rate-limiting															Limit per 15 min
https://developer.twitter.com/en/docs/tweets/timelines/api-reference/get-statuses-user_timeline						900-1500
https://developer.twitter.com/en/docs/tweets/timelines/api-reference/get-statuses-home_timeline						15
https://developer.twitter.com/en/docs/tweets/timelines/api-reference/get-statuses-mentions_timeline					75
https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-friends-list		15
https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-followers-list	15
https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-favorites-list						75
https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-statuses-retweets_of_me				75
https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-statuses-retweeters-ids				75-300
https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-statuses-retweets-id					75-300
 */

class TwitterFeeder extends SocialFeeder {
	Twitter api;
	public TwitterFeeder(Environment body, Twitter api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		this.api = api;
	}
	public void getFeed(String id, String token, String token_secret, Date since, Date until, StringBuilder detail) throws IOException {
		body.debug("Twitter crawling "+user_id);
		
		//https://developer.twitter.com/en/docs/tweets/timelines/guides/working-with-timelines
		//https://developer.twitter.com/en/docs/tweets/timelines/api-reference/get-statuses-home_timeline
		//Up to 800 Tweets are obtainable on the home timeline. It is more volatile for users that follow many users or follow users who Tweet frequently.
		int days = Period.daysdiff(since, until); 
		int count = days * Twitter.DAILY_NEWS;
		
		try {
//TODO deal with rate limits, if needed
//TODO if date limit is not hit, iterate with max_id-1
			String[][] params = new String[][] {
				//new String [] {"user_id","563516882"}, 
				//new String [] {"screen_name","bengoertzel"}, 
				new String [] {"count",String.valueOf(count)},
				//new String [] {"since_id","false"},
				//new String [] {"max_id","1257053173916659712"},
				new String [] {"tweet_mode", "extended"},
				new String [] {"trim_user","false"},
				new String [] {"exclude_replies","false"},
				new String [] {"include_rts","true"}
			};
			String result = Twitterer.request("https://api.twitter.com/1.1/statuses/user_timeline.json","GET",params,api.consumer_key,api.consumer_key_secret,token,token_secret);
			if (AL.empty(result) || !result.startsWith("[")) {
				body.error("Twitter crawling error "+id+" response "+result,null);
				return;
			}
			body.debug("Twitter crawling "+id+" response "+result);
			JsonReader jsonReader = Json.createReader(new StringReader(result));
			JsonArray tweets = jsonReader.readArray();
			for (int i = 0; i < tweets.size(); i++) {
				JsonObject j = tweets.getJsonObject(i);
				Twit t = new Twit(j);
				body.debug(String.format("Twitter crawling %s %tc %s %s %s %s %s %s %s %s\n", id, t.created_at, t.id_str, t.user_screen_name, t.in_reply_to_screen_name, t.retweet_count, t.favorite_count, t.retweeted, t.favorited, t.text));
				if (since.compareTo(t.created_at) > 0)
					break;
				Date date = Time.date(t.created_at);
				
				//getUser(t.user_id_str,t.user_name,t.user_screen_name,t.user_profile_image);
				getUser(t.user_id_str,t.user_screen_name);

//TODO: consider rate limits
//https://developer.twitter.com/en/docs/basics/rate-limiting

//TODO: count retweets as likes
//https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-statuses-retweets_of_me

//TODO: count likes/comments cached in graph on the following data being crawlied daily as SocailGrapher
//- get followers
//https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-followers-list
//- get friends (followed)
//https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-friends-list
//- get top 20 likes by users
//https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-favorites-list	
				countLikes(anonymous,Anonymous,date,t.favorite_count+t.retweet_count);
				
//TODO count comments	
//https://developer.twitter.com/en/docs/tweets/timelines/api-reference/get-statuses-mentions_timeline
//https://developer.twitter.com/en/docs/tweets/timelines/api-reference/get-statuses-home_timeline
				Object[][] comments = null;
				int comments_count = 0;
				
//TODO extract mentions
				if (AL.empty(t.links))
					t.links.add(t.url);
				String text = processItem(date,t.user_id_str,t.text,t.links,comments,t.score,t.scored);
				
				String imghtml = t.image != null ? Reporter.img(t.url, "height:auto;width:140px;", t.image) : 
					!AL.empty(t.user_profile_image) ? Reporter.img(t.url, null, t.user_profile_image) : null;
				
				reportDetail(detail,t.user_id_str,t.url,t.id_str,t.text,date,comments,t.links,null,t.score,(t.scored ? 1 : 0),comments_count,imghtml);

//TODO: or not todo	- update for today ad yesterday only 
				//if (today.compareTo(ri.date)>=0)
				api.matchPeerText(user_id, text, date, t.url, imghtml);
				
			}
		} catch (Exception e) {
			body.error("Twitter crawling error "+id,e);
		}
	}
}

public class Twitter extends Socializer implements Crawler {
	protected static final String content_url = "https://twitter.com/";
	protected static final int DAILY_NEWS = 24;////assume 10 tweets per day as a default portion, TODO configuration
	
	String consumer_key;
	String consumer_key_secret;

	public Twitter(Body body, String consumer_key, String consumer_key_secret) {
		super(body);
		this.consumer_key = consumer_key;
		this.consumer_key_secret = consumer_key_secret;
	}

	@Override
	public String name() {
		return "twitter";
	}

	@Override
	public SocialFeeder getFeeder(String id, String token, String token_secret, Date since, Date until, String[] areas) throws IOException {
		TwitterFeeder feeder = new TwitterFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(id, token, token_secret, since, until, new StringBuilder());
		return feeder;
	}

	@Override
	public Profiler getProfiler(Thing peer) {
		return new Profiler(body,this,peer,Body.twitter_id,Body.twitter_token,Body.twitter_token_secret);
	}
	
	@Override
	public String getTokenSecret(Thing peer) {
		return peer.getString(Body.twitter_token_secret);
	}

	@Override
	public int crawl(String url, Collection topics, Date time, MapMap thingPathsCollector){
		String screen_name;
		if (AL.empty(url) || AL.empty(screen_name = Str.parseBetween(url, content_url, "/", false)) || AL.empty(consumer_key) || AL.empty(consumer_key_secret))
			return -1;
		String token = body.getSelf().getString(Body.twitter_token);
		String token_secret = body.getSelf().getString(Body.twitter_token_secret);
		if (AL.empty(token) || AL.empty(token_secret))
			return -1;

		try {
			Date since = Time.today(-1);
//TODO iterate till daily data is exhausted or "latest known time" is reached
			String[][] params = new String[][] {
				new String [] {"screen_name",screen_name}, 
				new String [] {"count",String.valueOf(DAILY_NEWS)},
				//new String [] {"since_id","false"},
				//new String [] {"max_id","1257053173916659712"},
				new String [] {"tweet_mode", "extended"},
				new String [] {"trim_user","false"},
				new String [] {"exclude_replies","true"},
				new String [] {"include_rts","true"}
			};
			String result = Twitterer.request("https://api.twitter.com/1.1/statuses/user_timeline.json","GET",params,consumer_key,consumer_key_secret,token,token_secret);
			if (AL.empty(result) || !result.startsWith("[")) {
				body.error("Twitter crawling error "+screen_name+" response "+result,null);
				return 0;
			}
			body.debug("Twitter crawling "+screen_name+" response "+result);
			JsonReader jsonReader = Json.createReader(new StringReader(result));
			JsonArray tweets = jsonReader.readArray();
			int matches = 0;
			for (int i = 0; i < tweets.size(); i++) {
				JsonObject j = tweets.getJsonObject(i);
				Twit t = new Twit(j);
				body.debug(String.format("Twitter crawling %s %tc %s %s %s %s %s %s %s %s\n", screen_name, t.created_at, t.id_str, t.user_screen_name, t.in_reply_to_screen_name, t.retweet_count, t.favorite_count, t.retweeted, t.favorited, t.text));
				if (t.created_at.compareTo(since) < 0)
					break;
				String text = HtmlStripper.convert(t.text," ",null);
				text = HtmlStripper.convertMD(text, null, null);
//TODO: consider if we want to consider links and images same way as we do that for Siter's web pages 
				matches += matcher.matchThingsText(topics,text,Time.date(t.created_at),t.url,t.image,thingPathsCollector);
			}
			return matches;
		} catch (Exception e) {
			body.error("Twitter crawling error "+screen_name,e);
		}
		return -1;
	}
}
