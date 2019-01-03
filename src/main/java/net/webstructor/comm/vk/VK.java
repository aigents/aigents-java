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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.Socializer;
import net.webstructor.data.SocialFeeder;

/*
https://vk.com/support

http://aigents.com/index_test.html
https://vk.com/editapp?id=5212732&section=options
https://vk.com/dev/methods
https://vk.com/dev/permissions
...
it works:
https://api.vk.com/method/users.get?user_id=49817193&v=5.44&access_token=<token>
https://api.vk.com/method/wall.get?user_id=49817193&v=5.44&access_token=<token>
...

P.S.
http://vk.com/dev/openapi
http://habrahabr.ru/post/213163/ (!!!)
https://vk.com/dev/auth_sites (redirect)
http://vk.com/topic-1_24428376?offset=2440 (oauth)


TODO:
get feed:
https://api.vk.com/method/newsfeed.get?user_id=49817193&v=5.44&access_token=<token>

https://oauth.vk.com/access_token?client_id=4965500&client_secret=<secret>&v=5.42&grant_type=client_credentials

https://api.vk.com/method/newsfeed.get?access_token=<token>

From Server - app should be native :
http://syswerke.com/projects/blog/vk-com-secure-checktoken-returns-access-denied-application-should-be-native/
https://toster.ru/q/78024
https://api.vk.com/oauth/access_token?v=5.21&client_id=5212732&client_secret=<secret>&grant_type=client_credentials

https://api.vk.com/oauth/access_token?v=5.21&client_id=5212732&client_secret=<secret>&grant_type=client_credentials
https://api.vk.com/method/secure.checkToken?v=5.21&token=<user_token>&client_secret=<secret>&access_token=<server_token>
*/

public class VK extends Socializer {
	private String appId;
	private String appSecret;
	protected HttpFileReader reader;//TODO: move up to HTTP or fix native HTTP
	
	//TODO: have shared parent class Socializer for FB and GApi and VK
	public VK(Body body, String appId, String appSecret) {
		super(body);
		this.appId = appId;
		this.appSecret = appSecret;
		this.reader = new HttpFileReader();
	}

	//TODO:@Override
	public String provider(){
		return "vkontakte";
	}
	
	//TODO:@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		VKFeeder feeder = new VKFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}

	public String[] verifyRedirect(String input) {
		body.debug(provider()+" redirect: "+input);
		String vkontakte_id = Socializer.parseBetween(input, "vkontakte_id=", "&");
		String code = Socializer.parseBetween(input, "code=", "&");
		String redirect_uri = Socializer.parseBetween(input, "state=", null);
		String ensti[] = verifyCode(vkontakte_id, code, redirect_uri);
		return ensti;
	}
	
	//http://vk.com/dev/auth_sites
	public String[] verifyCode(String userId,String code,String redirect_uri) {
		String url = "https://oauth.vk.com/access_token?client_id=" + appId +
				"&client_secret=" + appSecret + "&v=5.44"+
				"&redirect_uri=" + redirect_uri + "&code="+code;
		try {
			body.debug(provider()+" request: " + url);
			String auth = simpleGet(url);				
			body.debug(provider()+" response: " + auth);
			JsonReader jr = Json.createReader(new StringReader(auth));
			JsonObject jauth = jr.readObject();
			String access_token = jauth.getString("access_token");
			int user_id = jauth.getInt("user_id");
			jr.close();
			if (userId.equals(String.valueOf(user_id)))
//TODO: do true verifyToken
				return getUserInfo(userId,access_token);
		} catch (Exception e) {
			body.error(provider()+" server code validation ", e);
			return null;
		}
		return null;
	}

	//TODO: append access token to request to get email too
	public String[] getUserInfo(String userId,String access_token) {
		String url = "https://api.vk.com/method/users.get?v=5.44&user_ids=" + userId + "&fields=first_name,last_name,email&access_token=" + access_token;
		try {
			body.debug(provider()+" request: " + url);
			String user = simpleGet(url);				
			body.debug(provider()+" response: " + user);
			JsonReader jr = Json.createReader(new StringReader(user));
			JsonObject info = jr.readObject();
			JsonArray objects = info.getJsonArray("response");
			JsonObject object = objects.getJsonObject(0);
			String name = getJsonString(object,"first_name",userId);
			String surname = getJsonString(object,"last_name",userId);
			String email = getJsonString(object,"email",null);
			body.debug(provider()+" user: " + name + " " + surname + " " + email);
			jr.close();
			return new String[]{email,name,surname,access_token,userId};
		} catch (Exception e) {
			body.error(provider()+" user info ", e);
			return null;
		}
	}
	
	//TODO:@Override
	public String[] verifyToken(String userId,String code) {
		//TODO:
		//check if have server token
body.debug(provider()+" verifyToken: " + appSecret);
		String server_token = body.self().getString(Body.vkontakte_token);
		if (AL.empty(server_token)){
			//if not, get it and save
			//https://vk.com/dev/auth_server
			//https://oauth.vk.com/access_token?client_id=...&client_secret=...&v=5.44&grant_type=client_credentials
			String url = "https://oauth.vk.com/access_token?client_id=" + appId +
				"&client_secret=" + appSecret + "&v=5.44&grant_type=client_credentials";
			try {
				body.debug(provider()+" request: " + url);
				String auth = simpleGet(url);				
				body.debug(provider()+" response: " + auth);
				//{"access_token":"...","expires_in":0}
				JsonReader jr = Json.createReader(new StringReader(auth));
				JsonObject jauth = jr.readObject();
				server_token = jauth.getString("access_token");
				jr.close();
				body.debug(provider()+" server_token: " + server_token);
				body.self().setString(Body.vkontakte_token,server_token);				
			} catch (IOException e) {
				body.error(provider()+" server token validation ", e);
				return null;
			}
		}
		//validate client token
		//https://toster.ru/q/78024
		//https://api.vk.com/method/secure.checkToken?v=5.44&token=<user_token>&ip=<user_ip>&client_secret=<app_secret>&access_token=<access_token>
		String url = "https://api.vk.com/method/secure.checkToken?v=5.44&token=" + code + 
			//TODO: "&ip=<user_ip>" + 
			"&client_secret=" + appSecret + "&access_token=" + server_token;
		try {
			body.debug(provider()+" request: " + url);
			String auth = simpleGet(url);
			body.debug(provider()+" response: " + auth);
			//{"response":{"success":1,"user_id":49817193,"date":1454865203,"expire":1454872403}}
			JsonReader jr = Json.createReader(new StringReader(auth));
			JsonObject jauth = jr.readObject();
			JsonObject response = jauth.getJsonObject("response");
			int success = response.getInt("success");
			int user_id = response.getInt("user_id");
			jr.close();
			if (success == 1 && userId.equals(String.valueOf(user_id))){
				//TODO: try to get email here too
				return getUserInfo(userId,code);
				/*
				url = "https://api.vk.com/method/users.get?user_ids=" + userId +
					"&fields=first_name,last_name,email";
				try {
					body.debug(provider()+" request: " + url);
					String user = simpleGet(url);				
					body.debug(provider()+" response: " + user);
					jr = Json.createReader(new StringReader(user));
					JsonObject info = jr.readObject();
					JsonArray objects = info.getJsonArray("response");
					JsonObject object = objects.getJsonObject(0);
					String name = getJsonString(object,"first_name",userId);
					String surname = getJsonString(object,"last_name",userId);
					String email = getJsonString(object,"email",null);
					body.debug(provider()+" user: " + name + " " + surname + " " + email);
					jr.close();
					return new String[]{email,name,surname,code};
				} catch (IOException e) {
					body.error(provider()+" user info ", e);
					return null;
				}
				*/
			}
		} catch (IOException e) {
			body.error(provider()+" client token validation ", e);
			return null;
		}					
		return null;
	}
    

	public static void main(String[] args) {
		ArrayList collectedLinks = new ArrayList();
		String html = "Статья про Язык Агентов с конференции ЗОНТ-2015 <br>https://aigents.com/papers/2015/ZONT-2015-Agent-Language-Kolonin.pdf со слайдами http://aigents.com/papers/2015/ZONT-2015-Agent-Language-Kolonin-slides.pdf";
		String text = HtmlStripper.convert(html," ",collectedLinks);//.toLowerCase();
		System.out.println(text);
	}
	
}//class
