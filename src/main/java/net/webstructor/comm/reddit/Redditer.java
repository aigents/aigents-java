/*
 * MIT License
 * 
 * Copyright (c) 2019 by Anton Kolonin, Aigents®
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
import java.util.HashSet;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.HTTPHandler;
import net.webstructor.comm.HTTPeer;
import net.webstructor.comm.SocialBinder;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Session;
import net.webstructor.util.Str;

//https://www.reddit.com/prefs/apps
//https://github.com/reddit-archive/reddit/wiki/OAuth2
//https://www.reddit.com/dev/api/
//https://github.com/reddit-archive/reddit/wiki/API

public class Redditer extends SocialBinder implements HTTPHandler {

	public Redditer(Body body) {
		super(body,"reddit");
	}
	
	/**
	 * @param args - array with url, header, request
	 * @return true if handled
	 * @throws IOException
	 */
	public boolean handleHTTP(HTTPeer parent, String url, String header, String request, String cookie) throws IOException {
		if (!url.startsWith("/reddit"))
			return false;
		Reddit reddit = (Reddit)body.getSocializer("reddit");//TODO: validate type or make type-less 
		if (reddit == null)
			return false;
		try {
			if (!AL.empty(request)){
				//Reddit authorization
				//https://github.com/reddit-archive/reddit/wiki/OAuth2
				Map<String,String> keyvalues = Parser.splitToMap(request,"&","=");
				String error = keyvalues.get("error");
				String code = keyvalues.get("code");//A one-time use code that may be exchanged for a bearer token.
				String state = keyvalues.get("state");//This value should be the same as the one sent in the initial authorization request, and your app should verify that it is, in fact, the same.
				body.debug("Reddit request "+request+" cookie "+cookie);
				//assert cookie (from parent HTTP session) = state (passed by redirect)
				if (AL.empty(error) && !AL.empty(cookie) && !AL.empty(code) && cookie.equals(state)) {
	            	net.webstructor.peer.Session session = body.sessioner.getSession(parent == null ? this : parent,cookie);
					//get access token and redirect back to original UI
					if (session != null) {
						if (session.getPeer() != null)
							body.debug("Reddit session peer "+session.getPeer().getTitle(Peer.title));
						if (session.getStoredPeer() != null)
							body.debug("Reddit session stored peer "+session.getStoredPeer().getTitle(Peer.title));
						String client_id = body.self().getString(Body.reddit_id);
						String client_secret = body.self().getString(Body.reddit_key);
						String redirect_uri = body.self().getString(Body.reddit_redirect);
						String params = "grant_type=authorization_code&code="+code+"&redirect_uri="+redirect_uri;
						//String client_id_secret = client_id+":"+client_secret;
						//String auth_base64 = "Basic "+Code.str2b64(client_id_secret,false)+"=";
						String auth_base64 = HTTP.auth_base64(client_id,client_secret);
						body.debug("Reddit auth request "+params+" "+auth_base64);
						String response = HTTP.simple(Reddit.oauth_url,params,"POST",timeout,null,new String[][] {new String[] {"Authorization",auth_base64}});
						body.debug("Reddit auth response "+Str.first(response,200));
						if (!AL.empty(response)) {
							JsonReader jsonReader = Json.createReader(new StringReader(response));
							JsonObject json = jsonReader.readObject();
							String refresh_token = HTTP.getJsonString(json,"refresh_token");
							String access_token = HTTP.getJsonString(json,"access_token");
							String scope = HTTP.getJsonString(json,"scope");
							body.debug("Reddit scope "+scope+" refresh_token "+refresh_token+" access_token"+access_token);
							if (!AL.empty(access_token)) {
								//https://www.reddit.com/dev/api/#GET_api_v1_me
								String api_url = "https://oauth.reddit.com/api/v1/me";
								response = HTTP.simple(api_url,null,"GET",timeout,null,new String[][] {new String[] {"Authorization","bearer "+access_token}});
								body.debug("Reddit response "+Str.first(response,200));
								if (!AL.empty(response)) {
									jsonReader = Json.createReader(new StringReader(response));
									json = jsonReader.readObject();
									String username = HTTP.getJsonString(json,"name");
									String oid = HTTP.getJsonString(json, "oauth_client_id");
									String image = Str.parseTill(HTTP.getJsonString(json,"icon_img"),"?");
									String title = json.containsKey("subreddit")?
											HTTP.getJsonString(json.getJsonObject("subreddit"),"title",null) : null;
				            		body.debug("Reddit oid="+oid+" username="+username+" title="+title);
									if (!AL.empty(username)) {
//TODO:make sure aboout id
										boolean success = false;
										String id = username;//oid?
										if (session.authenticated() && !session.isSecurityLocal()) {
											String ok = session.bindAuthenticated(Body.reddit_id,id,Body.reddit_token,refresh_token);
											success = ok.equals(session.ok());
						            		body.debug("Reddit bind autheticated id="+id+" name="+username+" token="+refresh_token+" result="+ok);
										}else{
											String lastname = username = username.toLowerCase();//silly defaults
											if (!AL.empty(title)) {//try to split title into names
												String[] names = title.trim().split(" ");
												if (names.length == 1)
													lastname = names[0];
												else {
													username = names[0];
													for (int i = 1; i < names.length; i++)
														if (i == 1)
															lastname = names[1];
														else 
															lastname += " " + names[i];
												}
											}
//TODO:eliminate email mining redundacy
											HashSet<String> emails = new HashSet<String>();
											emails.add(id+"@reddit.com");
											String email = bindUserEmail(name,id,username,lastname,emails);
						            		body.debug("Reddit bind non-authenticated oid="+oid+" id="+id+" name="+username+" surname="+lastname+" email="+email);
											if (!AL.empty(email)) {
								            	session.bind(name, id, refresh_token, email, username, lastname);
								            	if (!AL.empty(session.output())) {
								            		body.debug("Reddit bind result="+session.output());
								            		success = true;
								            	} else
								            		body.debug("Reddit bind failed");
											}
										}
					            		if (success && !AL.empty(image))
					            			session.getStoredPeer().set(Body.reddit_image, image);
									}
								}
							}
						}
					}
					
				}//if no error on user acceptace of Reddit form
				
//TODO: figure out how to delver welcome message to chat view on the Web 
				parent.respond("<html><body>Return to<a href=\""+body.site()+"\">"+body.site()+"</a></body></html>","302 Found\nLocation: "+body.site(),"text/plain");
				return true;
			}
		} catch (Exception e) {
			body.error("Reddit error", e);
		}
		return false;
	}
	
	protected String getEmail(String id) {
		return id + "@reddit.com";
	}
	
	@Override
	public void output(Session session, String message) throws IOException {
		//TODO should we disregard Redditer as a Communicator so no need to have the fake output(...)? 
	}

}
