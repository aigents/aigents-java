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
package net.webstructor.comm.paypal;

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
import net.webstructor.comm.HTTP;
import net.webstructor.comm.HTTPHandler;
import net.webstructor.comm.HTTPeer;
import net.webstructor.comm.SocialBinder;
import net.webstructor.peer.Session;
import net.webstructor.util.Code;
import net.webstructor.util.Str;

//Connect with PayPal
//https://developer.paypal.com/developer/applications/
//https://developer.paypal.com/docs/connect-with-paypal/#user-experience
//https://developer.paypal.com/docs/connect-with-paypal/integrate/
//https://developer.paypal.com/docs/connect-with-paypal/reference/button-js-builder/#

//Make Payments
//https://developer.paypal.com/docs/archive/checkout/how-to/server-integration/
//https://www.paypal.com/apex/developer/expressCheckout/getAccessToken
	
//List Payments
//https://developer.paypal.com/docs/api/get-an-access-token-curl/
//https://developer.paypal.com/docs/api/payments/v1/#payment_list
	
public class PayPaler extends SocialBinder implements HTTPHandler {
	
	transient private String access_token = null;//cache access token between the calls
	
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
			String client_id = body.self().getString(Body.paypal_id);
			String client_secret = body.self().getString(Body.paypal_key);
			if (AL.empty(client_id) || AL.empty(client_secret))
				return false;
			body.debug("PayPal input "+url+" "+request);
			String paypal_url = body.self().getString(Body.paypal_url,"https://api.paypal.com");
			String api_url = paypal_url+"/v1/oauth2/token";
			if (url.startsWith("/paypal/create-payment/")) {
				String type = Str.parseBetween(request, "type=", "&",false);
				String total = Str.parseBetween(request, "total=", "&",false);
				String currency = Str.parseBetween(request, "currency=", "&",false);
				if (AL.empty(total) || AL.empty(currency) || AL.empty(type))
					return false;
				
				//https://www.paypal.com/apex/developer/expressCheckout/getAccessToken
				/*String auth_base64 = HTTP.auth_base64(client_id,client_secret);
				body.debug("PayPal request grant_type=client_credentials "+auth_base64);
				String response = HTTP.simple(api_url,"grant_type=client_credentials","POST",timeout,null,new String[][] {
					{"Accept", "application/json"},
					{"Accept-language", "en_US"},
					{"Authorization",auth_base64},
					{"Content-Type", "application/x-www-form-urlencoded"}
					});
				body.debug("PayPal response "+response);
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				access_token = HTTP.getJsonString(json,"access_token");*/
				access_token = PayPal.token(body, api_url, timeout, client_id, client_secret);
				
				//https://www.paypal.com/apex/developer/expressCheckout/createPayment
				//https://developer.paypal.com/docs/archive/checkout/how-to/server-integration/#1-set-up-your-client-to-call-your-server
				String site = body.site();
				String params = "{\"intent\": \"sale\",\"note_to_payer\":\""+type+"\",\"payer\": {\"payment_method\": \"paypal\"},\"transactions\": [{\"amount\":{ \"total\": \""+total+"\", \"currency\": \""+currency+"\"},\"description\": \""+type+"\"}],\"redirect_urls\":{\"return_url\": \""+site+"\",\"cancel_url\": \""+site+"\"}}";
				body.debug("PayPal request create "+params);
				String response = HTTP.simple(paypal_url+"/v1/payments/payment",params,"POST",timeout,null,new String[][] {
					{"Accept", "application/json"},
					{"Accept-language", "en_US"},
					{"Authorization","Bearer "+access_token},
					{"Content-Type", "application/json"}
					});
				body.debug("PayPal response create "+response);
				parent.respond(response);
				return true;
			} else
			if (url.startsWith("/paypal/execute-payment/")) {
				String payment_id = Str.parseBetween(request, "payment=", "&",false);
				String payer_id = Str.parseBetween(request, "payer=", "&",false);
				if (AL.empty(payment_id) || AL.empty(payer_id))
					return false;
				//https://www.paypal.com/apex/developer/expressCheckout/executeApprovedPayment				
				String params = "{\"payer_id\": \""+payer_id+"\"}";
				body.debug("PayPal request execute "+payment_id+" "+payer_id);
				String response = HTTP.simple(paypal_url+"/v1/payments/payment/"+payment_id+"/execute",params,"POST",timeout,null,new String[][] {
					{"Accept", "application/json"},
					{"Accept-language", "en_US"},
					{"Authorization","Bearer "+access_token},
					{"Content-Type", "application/json"}
					});
				body.debug("PayPal response execute "+response);
				if (!AL.empty(response)) {
//TODO: resync profile
					//{"id":"PAYID-LYU34MA8AR79209583584949","intent":"sale","state":"approved",...,"create_time":"2020-01-23T15:39:28Z","update_time":"2020-01-23T15:40:10Z",
					JsonObject json = Json.createReader(new StringReader(response)).readObject();
					PayPalItem item = new PayPalItem(json);
					PayPal paypal = (PayPal)body.getSocializer(name);
					if (paypal != null && item.valid()) {
						paypal.updatePeerTerm(item.payer_id, item.date, item.description);
						body.debug("PayPal updated peer "+item.payer_id+" "+item.date+" "+item.description);
//TODO: send message (but do it asyc to avoid HTTP session hangup)!!!
					}
				}
				parent.respond(response);
				return true;
			} else
			if (!AL.empty(request)){
				//https://developer.paypal.com/docs/connect-with-paypal/integrate/#6-get-access-token
//TODO: get access token and redirect back to original UI
				Map<String,String> keyvalues = Parser.splitToMap(request, "&","=");
				String code = keyvalues.get("code");
				String scope = keyvalues.get("scope");
				if (!AL.empty(code) && !AL.empty(scope)) {
					//curl -X POST https://api.sandbox.paypal.com/v1/oauth2/token \
					//	-H 'Authorization: Basic {Your Base64-encoded ClientID:Secret}=' \
					//	-d 'grant_type=authorization_code&code={authorization_code}'
					String params = "grant_type=authorization_code&code="+code;
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

