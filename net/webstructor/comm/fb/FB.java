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
package net.webstructor.comm.fb;

//http://stackoverflow.com/questions/2591098/how-to-parse-json-in-java
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Environment;
import net.webstructor.data.LangPack;
import net.webstructor.data.SocialFeeder;
import net.webstructor.main.Mainer;
import net.webstructor.util.Reporter;

/*
https://developers.facebook.com/support/bugs/create/
https://developers.facebook.com/docs/graph-api/advanced/rate-limiting

https://developers.facebook.com/docs/facebook-login/login-flow-for-web/v2.3
https://developers.facebook.com/docs/facebook-login/manually-build-a-login-flow/v2.3#checktoken

TODO:
- fb auth
-- exclude dob from required parameters
-- move email to top for web version
-- on "my facebook id 123, facebook token 456, name a, surname c, email b" verify token vs id 
- peer adding - ensure added peers can log in later
- fb peers  
- fb review submission for mining and writes : https://developers.facebook.com/docs/facebook-login/permissions#categories
- fb mining
- fb posting

 */
public class FB extends Socializer {
	private String appId;
	private String appSecret;
	protected HttpFileReader reader;//TODO: move up to HTTP or fix native HTTP
	
	//https://developers.facebook.com/docs/facebook-login/access-tokens/#apptokens
	public FB(Body body, String appId, String appSecret) {
		super(body);
		this.appId = appId;
		this.appSecret = appSecret;
		this.reader = new HttpFileReader();
	}

	public String provider(){
		return "facebook";
	}
	
	public SocialFeeder getFeeder(String id, String token, String key, Date since, Date until, String[] areas) throws IOException{
		Feeder feeder = new Feeder(body,id,body.languages,false,since,until);
		//TODO: fill detail in?
		feeder.getFeed(token, since, until);
		return feeder;
	}
	
	/**
	 * Validates userId and appId against accessToken
	 * @param userId
	 * @param accessToken
	 * @return true if userId is validated, false otherwise
	 * @throws Exception
	 */
	//https://developers.facebook.com/docs/facebook-login/manually-build-a-login-flow/v2.3#checktoken
	public String verifyToken(String userId,String accessToken) {
		String out;
		try {
			//TODO: use POST?

			//https://developers.facebook.com/docs/facebook-login/access-tokens#apptokens
			//String app_token = appId+"|"+appSecret;//400 returned
			String app_token_request = "https://graph.facebook.com/oauth/access_token?client_id="+appId+"&client_secret="+appSecret+"&grant_type=client_credentials";
			String app_token = send(app_token_request,"","GET");	
			body.debug("Facebook app token request: " + app_token_request);
			JsonReader app_token_reader = Json.createReader(new StringReader(app_token));
			JsonObject app_token_obj = app_token_reader.readObject();
			
			//new API!?
			//app_token = parseBetween(app_token,"access_token=",null);
			if (app_token_obj.containsKey("access_token"))
				app_token = app_token_obj.getJsonString("access_token").getString();
			else {
				body.debug("Can't get Facebook app access_token for " + app_token_request);
				return null;
			}
			
			//TODO: store app_token and use stored if valid
			body.debug("Facebook app token: " + app_token);
			String s = "https://graph.facebook.com/debug_token?input_token="+accessToken+"&access_token="+app_token;
			body.debug("Facebook request: " + s);
			//TODO:returns 400 - fix?
			//out = send("https://graph.facebook.com/debug_token?input_token="+accessToken+"&access_token="+app_token,"","GET");
			out = reader.readDocData(s);
			body.debug("Facebook verify: " + out);
			
			JsonReader jsonReader = Json.createReader(new StringReader(out));
			JsonObject object = jsonReader.readObject();
			JsonObject data = object.getJsonObject("data");
			String user_id = data.getString("user_id");
			String app_id = data.getString("app_id");
			boolean is_valid = data.getBoolean("is_valid");
			jsonReader.close();
			boolean ok = is_valid && userId.equals(user_id) && appId.equals(app_id);
			body.debug("Facebook data: " + out + "=>" + ok);
			
			//exchange short-lived token for long-lived one
			accessToken = send("https://graph.facebook.com/oauth/access_token?grant_type=fb_exchange_token&client_id="+appId+"&client_secret="+appSecret+"&fb_exchange_token="+accessToken,"","GET");
			JsonReader access_token_reader = Json.createReader(new StringReader(accessToken));
			JsonObject access_token_obj = access_token_reader.readObject();

			//new API!?
			//accessToken = parseBetween(accessToken,"access_token=","&expires=",false);
			if (access_token_obj.containsKey("access_token"))
				accessToken = access_token_obj.getJsonString("access_token").getString();
			else {
				body.debug("Can't get Facebook app access_token for " + accessToken);
				return null;
			}
			
			body.debug("Facebook long-lived token: " + accessToken);
				
			return accessToken;
		} catch (Exception e) {
			body.error("Can't verify Facebook token", e);
			return null;
		}
	}
	
	public String[] getMe(String accessToken) {
		String out;
		try {
			out = reader.readDocData("https://graph.facebook.com/me?access_token="+accessToken);
			body.debug("Facebook getMe: " + out );
			JsonReader jsonReader = Json.createReader(new StringReader(out));
			JsonObject data = jsonReader.readObject();
			String email = getJsonString(data,"email");
			String name = getJsonString(data,"name");
			String firstname = getJsonString(data,"first_name");
			String lastname = getJsonString(data,"last_name");
			if (AL.empty(firstname) && !AL.empty(name)){
				String[] s = name.split(" ");
				firstname = s[0];
				if (s.length > 1)
					lastname = s[1];
			}
			jsonReader.close();
			return new String[]{email,firstname,lastname};
		} catch (IOException e) {
			body.error("Can't get Facebook user data", e);
			return null;
		}
	}

	//for test only
	public static void main(String[] args) {
		if (args.length < 2)
			return;
		String user_id = args[0];
		String token = args[1];
		Date since = args.length > 2 ? Time.day(args[2]) : null;
		Date until = args.length > 3 ? Time.day(args[3]) : null;
		Environment env = new Mainer();
		Feeder feeder = new Feeder(env,user_id,new LangPack(),false,since,until);
		try {
			feeder.getFeed(token, since, until);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//TODO: make sure it is still working after refactoring
		Reporter rep = new Reporter(env,user_id+".html");
		rep.initReport("Aigents Report for Facebook (beta)",since,until);
		rep.initPeer(user_id, null, null, null, since, until);
		
		//TODO: exporter to csv

		/*
		rep.table("Who are similar to me (simple function)",
				new String[]{"Rank,%","Friend","Crosses","My Likes","Likes","Comments","Words Cross/Friend"},
				feeder.getSimilarPeers(false,false,false),10);
		rep.table("Who are similar to me (overlapped cross function)",
				new String[]{"Rank,%","Friend","Crosses","My Likes","Likes","Comments","Words Cross/Friend"},
				feeder.getSimilarPeers(true,false,false),10);
		*/
		rep.table("dummy","Who are similar to me (normalized overlapped cross function)",
				new String[]{"Rank,%","Friend","Crosses","My Likes","Likes","Comments","Words Cross/Friend"},
				feeder.getSimilarPeers(true,false,true),20,0);//is the best!!!
		/*
		rep.table("Who are similar to me (multipled cross function)",
				new String[]{"Rank,%","Friend","Crosses","My Likes","Likes","Comments","Words Cross/Friend"},
				feeder.getSimilarPeers(false,true,false),10);

		rep.table("Who are similar to me (normalized multipled cross function)",
				new String[]{"Rank,%","Friend","Crosses","My Likes","Likes","Comments","Words Cross/Friend"},
				feeder.getSimilarPeers(false,true,true),10);
		*/

		rep.table("dummy","Who like and comment me",
				new String[]{"Rank,%","Friend","My Likes","Likes","Comments"},
				feeder.getPeers(),20,0);
		
		rep.table("dummy","Who are liked by me",
				new String[]{"Rank,%","Friend","My Likes","Likes","Comments"},
				feeder.getLikedPeers(),20,0);
		
		//TODO: normalize!
		rep.table("dummy","What are words of my interest",
				new String[]{"Period","Words"},
				feeder.getWordsPeriods(),20,0);
		
		rep.table("dummy","Which my posts are liked and commented",
				new String[]{"Rank,%","Like","Likes","Comments","Date","Text","Links"},
				feeder.getNews(),0,0);	
		/*
		//csv-style export
		System.out.println("\nLike, Likes, Comments, Date, Text, Links:");
		Object[][] news = feeder.getNews();
		for (int i = 0; i < news.length; i++){
			Object[] item = news[i];
			System.out.println(Writer.toString(item,null,"","|",""));
		}
		*/
		
		rep.table("dummy","What my words are liked and commented",
				new String[]{"Rank,%","Word","My Likes","Likes","Comments","Posts","Count","(Likes+Comments)/Posts"},
				feeder.getWordsLikedAndCommentedByOthers(500),20,0);
		
		rep.table("dummy","What words are liked by me",
				new String[]{"Rank,%","Word","My Likes","Likes","Comments","Posts","Count","(My Likes)/Posts"},
				feeder.getWordsLikedByMe(500,1),0,0);

		/*
		//csv-style export
		System.out.println("\nWord, Likes, Comments, Posts, Count, Likes/Posts, Comments/Posts:");
		Object[][] words = feeder.getWords(100);
		for (int i = 0; i < words.length; i++){
			Object[] item = words[i];
			System.out.println(Writer.toString(item,null,"","|",""));
		}
		*/
		
		rep.closePeer();
		rep.closeReport();
	}
}
