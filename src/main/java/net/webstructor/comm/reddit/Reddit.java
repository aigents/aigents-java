/*
 * MIT License
 * 
 * Copyright (c) 2005-2019 by Anton Kolonin, Aigents
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
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.Socializer;
import net.webstructor.data.SocialFeeder;
import net.webstructor.util.Code;

//TODO: merge Reddit+Redditer and FB+Messenger? 
public class Reddit extends Socializer {
	String appId;
	String appSecret;
	protected HttpFileReader reader;//TODO: move up to HTTP or fix native HTTP
	
	public static final String oauth_url = "https://www.reddit.com/api/v1/access_token";
	public static final String home_url = "https://www.reddit.com";

	public Reddit(Body body, String appId, String appSecret) {
		super(body);
		this.appId = appId;
		this.appSecret = appSecret;
		this.reader = new HttpFileReader();
	}

	@Override
	public String provider(){
		return "reddit";
	}
	
	@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		RedditFeeder feeder = new RedditFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}
	
	String auth_base64(String client_id,String client_secret) {
		String client_id_secret = client_id+":"+client_secret;
		body.debug("Reddit client_id_secret "+client_id_secret);
		return "Basic "+Code.str2b64(client_id_secret,false)+"=";
	}

	String refresh_token(String client_id,String client_secret,String refresh_token) throws IOException {
		String params = "grant_type=refresh_token&refresh_token="+refresh_token;
		String auth_base64 = auth_base64(client_id,client_secret);
		body.debug("Reddit refresh request "+params+" "+auth_base64);
		String response = HTTP.simple(Reddit.oauth_url,params,"POST",timeout,null,new String[][] {new String[] {"Authorization",auth_base64}});
		body.debug("Reddit refresh response "+response);
		if (!AL.empty(response)) {
			JsonReader jsonReader = Json.createReader(new StringReader(response));
			JsonObject json = jsonReader.readObject();
			if (json.containsKey("access_token"))
				return HTTP.getJsonString(json,"access_token");
		}
		return null;
	}
	
	String refresh_token(String token) throws IOException {
		return refresh_token(appId,appSecret,token);
	}	
}