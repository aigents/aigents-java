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
package net.webstructor.comm;//TODO move to paypal

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.peer.Session;
import net.webstructor.util.Code;
import net.webstructor.util.Str;

//Connect with PayPal
//https://developer.paypal.com/developer/applications/
//https://developer.paypal.com/docs/connect-with-paypal/#user-experience
//https://developer.paypal.com/docs/connect-with-paypal/integrate/
//https://developer.paypal.com/docs/connect-with-paypal/reference/button-js-builder/#


//List Payments
//https://developer.paypal.com/docs/api/get-an-access-token-curl/

public class PayPaler extends SocialBinder implements HTTPHandler {
	public PayPaler(Body body) {
		super(body,"paypal");
	}
	
	/**
	 * @param args - array with url, header, request
	 * @return true if handled
	 * @throws IOException
	 */
	public boolean handleHTTP(HTTPeer parent, String url, String header, String request, String cookie) throws IOException {
		if (url.startsWith("/paypal")) try {
			if (!AL.empty(request)){
				String paypal_url = body.self().getString(Body.paypal_url,"https://api.paypal.com");
				body.debug("PayPal input "+request);
				//https://developer.paypal.com/docs/connect-with-paypal/integrate/#6-get-access-token
//TODO: get access token and redirect back to original UI
				Map<String,String> keyvalues = Parser.splitToMap(request, "&","=");
				String code = keyvalues.get("code");
				String scope = keyvalues.get("scope");
				if (!AL.empty(code) && !AL.empty(scope)) {
					String api_url = paypal_url+"/v1/oauth2/token";
					//curl -X POST https://api.sandbox.paypal.com/v1/oauth2/token \
					//	-H 'Authorization: Basic {Your Base64-encoded ClientID:Secret}=' \
					//	-d 'grant_type=authorization_code&code={authorization_code}'
					String params = "grant_type=authorization_code&code="+code;
					String client_id = body.self().getString(Body.paypal_id);
					String client_secret = body.self().getString(Body.paypal_key);
					String client_id_secret = client_id+":"+client_secret;
					String auth_base64 = "Basic "+Code.str2b64(client_id_secret,false)+"=";
					body.debug("PayPal client_id_secret "+client_id_secret);
					body.debug("PayPal params "+params);
					body.debug("PayPal auth "+auth_base64);
					String response = HTTP.simple(api_url,params,"POST",timeout,null,new String[][] {
						new String[] {"Authorization",auth_base64}
						});
					body.debug("PayPal response "+response);
					if (!AL.empty(response)) {
						//https://developer.paypal.com/docs/connect-with-paypal/integrate/#6-get-access-token
						//{
						//	   "token_type": "Bearer",
						//	   "expires_in": "28800",
						//	   "refresh_token": {refresh_token},
						//	   "access_token": {access_token}
						//}
						JsonReader jsonReader = Json.createReader(new StringReader(response));
						JsonObject json = jsonReader.readObject();
						String refresh_token = HTTP.getJsonString(json,"refresh_token");
						String access_token = HTTP.getJsonString(json,"access_token");
						
//TODO don't refresh access_token here because it is overkill
						//curl -X POST https://api.sandbox.paypal.com/v1/oauth2/token \
						//	-H 'Authorization: Basic {Your Base64-encoded ClientID:Secret}=' \
						//	-d 'grant_type=refresh_token&refresh_token={refresh token}'
						params = "grant_type=refresh_token&refresh_token="+refresh_token;
						body.debug("PayPal request "+params+" "+auth_base64);
						response = HTTP.simple(api_url,params,"POST",timeout,null,new String[][] {new String[] {"Authorization",auth_base64}});
						body.debug("PayPal response "+response);
						if (!AL.empty(response)) {
							//https://developer.paypal.com/docs/connect-with-paypal/integrate/#7-exchange-refresh_token-for-access_token
							//{
							//	   "token_type": "Bearer",
							//	   "expires_in": "28800",
							//	   "access_token": {access_token}
							//}
							jsonReader = Json.createReader(new StringReader(response));
							json = jsonReader.readObject();
//TODO sort out why it is not working so far, reporting {"error":"invalid_refresh_token","error_description":"No consent were granted"}
							if (json.containsKey("access_token"))
								access_token = HTTP.getJsonString(json,"access_token");
						}
						
						if (!AL.empty(access_token)) {
							//https://developer.paypal.com/docs/api/identity/v1/#userinfo_get
							//curl -v -X GET https://api.sandbox.paypal.com/v1/identity/oauth2/userinfo?schema=paypalv1.1 \
							//	-H "Content-Type: application/json" \
							//	-H "Authorization: Bearer Access-Token"
							api_url = paypal_url+"/v1/identity/oauth2/userinfo?schema=paypalv1.1";
							response = HTTP.simple(api_url,null,"GET",timeout,null,new String[][] {
								new String[] {"Content-Type","application/json"},
								new String[] {"Authorization","Bearer "+access_token}
								});
							body.debug("PayPal response "+response);
							if (!AL.empty(response)) {
								//TODO:
								//{
								//	  "user_id": "https://www.paypal.com/webapps/auth/identity/user/mWq6_1sU85v5EG9yHdPxJRrhGHrnMJ-1PQKtX6pcsmA",
								//	  "name": "identity test",
								//	  "given_name": "identity",
								//	  "family_name": "test",
								//	  "payer_id": "WDJJHEBZ4X2LY",
								//	  "verified_account": "true",
								//	  "emails": [
								//	    {
								//	      "value": "user1@example.com",
								//	      "primary": true
								//	    }
								//	  ]
								//	}
								jsonReader = Json.createReader(new StringReader(response));
								json = jsonReader.readObject();
								String user_id = HTTP.getJsonString(json,"user_id");
								String full_name = HTTP.getJsonString(json,"name");
								String given_name = HTTP.getJsonString(json,"given_name");
								String family_name = HTTP.getJsonString(json,"family_name");
								String payer_id = HTTP.getJsonString(json,"payer_id");
								String verified_account = HTTP.getJsonString(json,"verified_account");
								HashSet<String> emails = new HashSet<String>();
								JsonArray emailsJson = json.containsKey("emails") ? json.getJsonArray("emails") : null;
								if (emailsJson != null) {
									for (int i = 0; i < emailsJson.size(); i++) {
										JsonObject emailJson = emailsJson.getJsonObject(i);
										String email = HTTP.getJsonString(emailJson,"value");
										if (!AL.empty(email))
											emails.add(email);
									}
								}
								body.debug("PayPal data user_id="+user_id+" full_name="+full_name+" given_name="+given_name+" family_name="+family_name+" payer_id="+payer_id+" verified="+verified_account+" emails="+emails);
								String paypal_id = !AL.empty(payer_id) ? payer_id : user_id;
								String[] names  = Str.splitName(full_name, given_name, family_name, false);
								String email = bindUserEmail(name,paypal_id,names[0],names[1],emails);
								if (!AL.empty(email)) {
//TODO update name & surname only if missed!?
//TODO if session is authenticated, don't bind it - just set missed attributes? 
					            	net.webstructor.peer.Session session = body.sessioner.getSession(parent == null ? this : parent, cookie);//use parent session type for cookie
					            	session.bind(name, paypal_id, refresh_token, email, names[0], names[1]);
					            	if (!AL.empty(session.output()))
					            		body.debug("PayPal bind "+session.output());
					            	else
					            		body.debug("PayPal bind failed");
								}
							}
						}
					}
				}
//TODO: figure out how to delver welcome message to chat view on the Web 
				parent.respond("<html><body>Return to<a href=\""+body.site()+"\">"+body.site()+"</a></body></html>","302 Found\nLocation: "+body.site(),"text/plain");
				return true;
			}
		} catch (Exception e) {
			body.error("PayPal error", e);
		}
		return false;
	}
	
	protected String getEmail(String id) {
		return id + "@paypal.com";
	}
	
	@Override
	public void output(Session session, String message) throws IOException {
		//TODO should we disregard PayPaler as a Communicator so no need to have the fake output(...)? 
	}

}

