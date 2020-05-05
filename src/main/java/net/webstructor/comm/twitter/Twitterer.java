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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.HTTPHandler;
import net.webstructor.comm.HTTPeer;
import net.webstructor.comm.SocialBinder;
import net.webstructor.core.Thing;
import net.webstructor.peer.Session;
import net.webstructor.util.Array;
import net.webstructor.util.Code;
import net.webstructor.util.JSON;
import net.webstructor.util.Str;

//https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/obtaining-user-access-tokens
//https://developer.twitter.com/en/docs/basics/authentication/guides/log-in-with-twitter
	

public class Twitterer extends SocialBinder implements HTTPHandler {

	public Twitterer(Body body) {
		super(body,"twitter");
	}
	
	HashMap<String,String> token_secrets = new HashMap<String,String>();
	
	/**
	 * @param args - array with url, header, request
	 * @return true if handled
	 * @throws IOException
	 */
	public boolean handleHTTP(HTTPeer parent, String url, String header, String request, String cookie) throws IOException {
		if (!url.startsWith("/twitter") || AL.empty(request))
			return false;
		try {
			Thing self = body.self();
			String oauth_consumer_key = self.getString(Body.twitter_key);
			String oauth_сonsumer_secret = self.getString(Body.twitter_key_secret);
			String oauth_token = self.getString(Body.twitter_token);
			String oauth_token_secret = self.getString(Body.twitter_token_secret);
			if (AL.empty(oauth_consumer_key) || AL.empty(oauth_сonsumer_secret) || AL.empty(oauth_token) || AL.empty(oauth_token_secret))
				return false;
			
			//https://developer.twitter.com/en/docs/basics/authentication/guides/log-in-with-twitter
			//https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/obtaining-user-access-tokens
			body.debug("Twitter input "+url+" "+request);
			if (request.contains("login")){//step 1
				//https://developer.twitter.com/en/docs/basics/authentication/api-reference/request_token
				String[][] params = new String[][] {new String [] {"oauth_callback","https%3A%2F%2Faigents.com%2Fal%2Ftwitter"}};
				String result = request("https://api.twitter.com/oauth/request_token", "POST",params,oauth_consumer_key,oauth_сonsumer_secret,oauth_token,oauth_token_secret);
				body.debug("Twitter login token "+result);
				String token = Str.parseBetween(result, "oauth_token=", "&");
				String secret = Str.parseBetween(result, "oauth_token_secret=", "&");
				synchronized (token_secrets) {
					token_secrets.put(token,secret);
				}
				String redirect = "https://api.twitter.com/oauth/authenticate?oauth_token="+token;
				parent.respond("<html><body>Proceed to<a href=\""+redirect+"\">"+body.site()+"</a></body></html>","302 Found\nLocation: "+redirect,"text/plain");
				return true;
			} else
			if (request.contains("oauth_token")){//step 3
				//https://developer.twitter.com/en/docs/basics/authentication/api-reference/access_token
				String token = Str.parseBetween(request, "oauth_token=", "&", false);
				String verifier = Str.parseBetween(request, "oauth_verifier=", "&", false);
				String secret;
				synchronized (token_secrets) {
					secret = token_secrets.get(token);
				}
				body.debug("Twitter login approved "+token+" "+verifier);
				String[][] params = new String[][] {
					new String [] {"oauth_token",token},
					new String [] {"oauth_verifier",verifier}
				};
				String result = request("https://api.twitter.com/oauth/access_token?oauth_token="+token+"&oauth_verifier="+verifier,
						"POST",params,oauth_consumer_key,oauth_сonsumer_secret,token,secret);
				body.debug("Twitter login verification "+result);
				String new_token = Str.parseBetween(result, "oauth_token=", "&");
				String new_secret = Str.parseBetween(result, "oauth_token_secret=", "&");
				String sname = Str.parseBetween(result, "screen_name=", "&", false);
				body.debug("Twitter login succeeded "+sname+" "+new_token+" "+new_secret);

				params = new String[][] {
					new String [] {"include_entities","true"},
					new String [] {"skip_status","true"},
					new String [] {"include_email","true"}
				};
				result = request("https://api.twitter.com/1.1/account/verify_credentials.json","GET",params,oauth_consumer_key,oauth_сonsumer_secret,new_token,new_secret);
				body.debug("Twitter login completed "+result);
				JsonReader jsonReader = Json.createReader(new StringReader(result));
				JsonObject json = jsonReader.readObject();
				String id = JSON.getJsonString(json, "id_str");
				String uname = JSON.getJsonString(json, "name");
				sname = JSON.getJsonString(json, "screen_name");
				String email = JSON.getJsonString(json, "email");
				String image = JSON.getJsonString(json, "profile_image_url_https");
				body.debug("Twitter login processed "+String.format("%s %s %s %s %s",id,uname,sname,email,image));
				Thing peer = bindPeer(parent, cookie, id, uname, sname, email, new_token);
				if (peer != null) {
					peer.setString(Body.twitter_token_secret, new_secret);
					if (!AL.empty(image) && AL.isIMG(image))
						peer.setString(Body.twitter_image, image);
				}

//TODO: figure out how to delver welcome message to chat view on the Web 
				parent.respond("<html><body>Return to<a href=\""+body.site()+"\">"+body.site()+"</a></body></html>","302 Found\nLocation: "+body.site(),"text/plain");
				return true;
			}
		} catch (Exception e) {
			body.error("Twitter error", e);
		}
		return false;
	}
	
	protected String getEmail(String id) {
		return id + "@twitter.com";
	}
	
	@Override
	public void output(Session session, String message) throws IOException {
		//TODO should we disregard Twitter as a Communicator so no need to have the fake output(...)?
		//TODO post direct messages by Twitter
	}

	//https://stackoverflow.com/questions/1609899/java-equivalent-to-phps-hmac-sha1
	public static String hash_hmac(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException {
	    SecretKey secretKey = null;
	    byte[] keyBytes = keyString.getBytes();
	    secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");
	    Mac mac = Mac.getInstance("HmacSHA1");
	    mac.init(secretKey);
	    byte[] text = baseString.getBytes();
	    //return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
	    String str2b64 = Code.str2b64(mac.doFinal(text),false);
	    return str2b64;
	}
	
	//https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/percent-encoding-parameters
	public static String percent_encoding(String input) {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (('0' <=c && c <= '9') || ('A' <=c && c <= 'Z') || ('a' <=c && c <= 'z') || c == '-' || c == '.' || c == '_' || c == '~')
				output.append(c);
			else
				//https://stackoverflow.com/questions/4477714/how-to-convert-a-char-from-alphabetical-character-to-hexadecimal-number-in-java
				output.append('%').append(Integer.toHexString((int) c).toUpperCase());
		}
		return output.toString();
	}
	
	public static String create_parameter_string(String[][] header) {
		String[][] encoded = new String[header.length][];
		for (int i = 0; i < header.length; i++)
			encoded[i] = new String[] {percent_encoding(header[i][0]), percent_encoding(header[i][1])};
		Arrays.sort(encoded,new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				String[] s1 = (String[]) o1;
				String[] s2 = (String[]) o2;
				int c = s1[0].compareTo(s2[0]);
				return c != 0 ? c : s1[1].compareTo(s2[1]);
			}});
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < encoded.length; i++) {
			if (i > 0)
				output.append("&");
			output.append(encoded[i][0]).append("=").append(encoded[i][1]);
		}
		return output.toString();
	}

	public static String create_params(String[][] params) throws UnsupportedEncodingException {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < params.length; i++) {
			if (i > 0)
				output.append("&");
			output.append(params[i][0]).append("=").append(URLEncoder.encode(params[i][1],"UTF-8"));
		}
		return output.toString();
	}
	
	public static String create_oauth_header_string(String[][] header) {
		StringBuilder output = new StringBuilder("OAuth ");
		for (int i = 0; i < header.length; i++) {
			if (i > 0)
				output.append(", ");
			output.append(percent_encoding(header[i][0])).append("=\"").append(percent_encoding(header[i][1])).append("\"");
		}
		return output.toString();
	}
	
	//https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/creating-a-signature
	public static String sign_request(String method, String url, String[][] header, String сonsumer_secret, String oauth_token_secret) throws UnsupportedEncodingException, GeneralSecurityException {
		String parameter_string = create_parameter_string(header);
		String signature_base_string = method + "&" + percent_encoding(url) + "&" + percent_encoding(parameter_string);
		//String сonsumer_secret = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
		//String oauth_token_secret = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
		String signing_key = сonsumer_secret+"&"+oauth_token_secret;
		String signature = hash_hmac(signature_base_string,signing_key);
		return signature;
	}
	
	public static String request(String url,String method,String[][] params,
			String oauth_consumer_key,String сonsumer_secret,String oauth_token,String oauth_token_secret) throws GeneralSecurityException, IOException {
		String[][] oauth = new String[][] {
		new String [] {"oauth_consumer_key",oauth_consumer_key},
		new String [] {"oauth_nonce",HTTP.nonce()},
		new String [] {"oauth_signature_method","HMAC-SHA1"},
		new String [] {"oauth_timestamp",String.valueOf(System.currentTimeMillis()/1000)},
		new String [] {"oauth_token",oauth_token},
		new String [] {"oauth_version","1.0"}
		};
		String[][] forsign = Array.add(params, oauth);
		String signature = sign_request(method,url,forsign,сonsumer_secret,oauth_token_secret);
		String[][] oauth_data = Array.add(oauth, new String[][] {{"oauth_signature",signature}});
		String oauth_header = create_oauth_header_string(oauth_data);
		System.out.println(oauth_header);
		
		String params_string = create_params(params);
		
		//hack around the stupid HTTP.simple
		if ("GET".equals(method) && !url.contains("?")) {
			url = url + "?" + params_string;
			params_string = null;
		}
//TODO: fix HTTP.simple so it does not write params of GET but adds them to the url
		String result = HTTP.simple(url, AL.empty(params_string) ? null : params_string, method, 0, null, new String[][] {
			new String[] {"Authorization",oauth_header}
		});
		return result;
	}
	
	
	public static void main(String args[]) {
		try {
			/*
			{//Test percent encoding
				String pe = percent_encoding("tnnArxj06cWHq44gCs1OSKk/jLY=");
				if ("tnnArxj06cWHq44gCs1OSKk%2FjLY%3D".equals(pe))
					System.out.println(pe);
			}
			{//Test request signing
				String method = "POST";
				String url = "https://api.twitter.com/1.1/statuses/update.json";
				String сonsumer_secret = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
				String oauth_token_secret = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
				String[][] header = new String[][] {
				new String [] {"status","Hello Ladies + Gentlemen, a signed OAuth request!"},
				new String [] {"include_entities","true"},
				new String [] {"oauth_consumer_key","xvz1evFS4wEEPTGEFPHBog"},
				new String [] {"oauth_nonce","kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg"},
				new String [] {"oauth_signature_method","HMAC-SHA1"},
				new String [] {"oauth_timestamp","1318622958"},
				new String [] {"oauth_token","370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb"},
				new String [] {"oauth_version","1.0"}
				};
				String signature = sign_request(method,url,header,сonsumer_secret,oauth_token_secret);
				if (signature.equals("hCtSmYh+iHYCEqBWrE7C7hYmtUk="))
					System.out.println(signature);
			}
			{//Test request authorizing with data from tutorial
				//https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/authorizing-a-request
				String method = "POST";
				String url = "https://api.twitter.com/1.1/statuses/update.json";
				String сonsumer_secret = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
				String oauth_token_secret = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
				
				String[][] params = new String[][] {
				new String [] {"status","Hello Ladies + Gentlemen, a signed OAuth request!"},
				new String [] {"include_entities","true"}
				};
				String[][] oauth = new String[][] {
				new String [] {"oauth_consumer_key","xvz1evFS4wEEPTGEFPHBog"},
				new String [] {"oauth_nonce","kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg"},
				new String [] {"oauth_signature_method","HMAC-SHA1"},
				new String [] {"oauth_timestamp","1318622958"},
				new String [] {"oauth_token","370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb"},
				new String [] {"oauth_version","1.0"}
				};
				String[][] forsign = Array.add(params, oauth);
				String signature = sign_request(method,url,forsign,сonsumer_secret,oauth_token_secret);
				if (signature.equals("hCtSmYh+iHYCEqBWrE7C7hYmtUk="))
					System.out.println(signature);

				//signature from the document
				String[][] oauth_data = Array.add(oauth, new String[][] {{"oauth_signature","tnnArxj06cWHq44gCs1OSKk/jLY="}});
				String oauth_header = create_oauth_header_string(oauth_data);
				System.out.println(oauth_header);
				
				//actual signature from the above
				oauth_data = Array.add(oauth, new String[][] {{"oauth_signature",signature}});
				oauth_header = create_oauth_header_string(oauth_data);
				System.out.println(oauth_header);
				
				String params_string = create_params(params);
				
				String result = HTTP.simple(url, params_string, "POST", 0, null, new String[][] {
					new String[] {"Authorization",oauth_header}
				});
				System.out.println(result);//{"errors":[{"code":89,"message":"Invalid or expired token."}]}
			}
			
			if (args.length >= 4) {//Test request authorizing with real app
				//https://developer.twitter.com/en/docs/basics/authentication/oauth-1-0a/authorizing-a-request
				String method = "POST";
				String url = "https://api.twitter.com/1.1/statuses/update.json";
				String oauth_consumer_key = args[0];
				String сonsumer_secret = args[1];
				String oauth_token = args[2];
				String oauth_token_secret = args[3];
				
				String[][] params = new String[][] {
				new String [] {"status","Aigents are testing Twitter OAuth at "+(new Date())+"!"},
				new String [] {"include_entities","true"}
				};
				String result = request(url,method,params,oauth_consumer_key,сonsumer_secret,oauth_token,oauth_token_secret);
				System.out.println(result);//it works - creates status for a user!!!
			}
			
			if (args.length >= 4){//Test getting actual token
				//https://developer.twitter.com/en/docs/basics/authentication/api-reference/request_token
				String method = "POST";
				String url = "https://api.twitter.com/oauth/request_token";
				String oauth_consumer_key = args[0];
				String сonsumer_secret = args[1];
				String oauth_token = args[2];
				String oauth_token_secret = args[3];
				
				String[][] params = new String[][] {
				new String [] {"oauth_callback","https%3A%2F%2Faigents.com%2Fal%2Ftwitter"}
				};
				String result = request(url,method,params,oauth_consumer_key,сonsumer_secret,oauth_token,oauth_token_secret);
				System.out.println(result);//it works - returns the token!!!
				String token = Str.parseBetween(result, "oauth_token=", "&");
				String secret = Str.parseBetween(result, "oauth_token_secret=", "&");
				System.out.println(token+" "+secret);
			}
			if (args.length >= 4){//Test verifying credentials
				String method = "GET";
				String url = "https://api.twitter.com/1.1/account/verify_credentials.json";
				String oauth_consumer_key = args[0];
				String сonsumer_secret = args[1];
				String oauth_token = args[2];
				String oauth_token_secret = args[3];
				
				String[][] params = new String[][] {
					new String [] {"include_entities","true"},
					new String [] {"skip_status","true"},
					new String [] {"include_email","true"}
				};
				String result = request(url,method,params,oauth_consumer_key,сonsumer_secret,oauth_token,oauth_token_secret);
				System.out.println(result);//it works - creates status for a user!!!
				
				JsonReader jsonReader = Json.createReader(new StringReader(result));
				JsonObject json = jsonReader.readObject();
				String id = JSON.getJsonString(json, "id_str");
				String name = JSON.getJsonString(json, "name");
				String sname = JSON.getJsonString(json, "screen_name");
				String email = JSON.getJsonString(json, "email");
				String image = JSON.getJsonString(json, "profile_image_url_https");
				System.out.format("%s %s %s %s %s",id,name,sname,email,image);//it works - creates status for a user!!!
			}
			
			if (args.length >= 4){//Test getting status - returns current day only
				String method = "GET";
				String url = "https://api.twitter.com/1.1/statuses/home_timeline.json";
				String oauth_consumer_key = args[0];
				String сonsumer_secret = args[1];
				String oauth_token = args[2];
				String oauth_token_secret = args[3];
				
				String[][] params = new String[][] {
					new String [] {"count","100"},//200
					//new String [] {"since_id","false"},
					//new String [] {"max_id","1257053173916659712"},
					new String [] {"trim_user","false"},
					new String [] {"exclude_replies","false"},
					new String [] {"include_entities","false"}//true?
				};
				String result = request(url,method,params,oauth_consumer_key,сonsumer_secret,oauth_token,oauth_token_secret);
				System.out.println(result);//it works - creates status for a user!!!
				
				JsonReader jsonReader = Json.createReader(new StringReader(result));
				JsonArray tweets = jsonReader.readArray();
				System.out.println(tweets.size());
				for (int i = 0; i < tweets.size(); i++) {
					Twit t = new Twit(tweets.getJsonObject(i));
					System.out.format("%tc %s %s %s %s %s %s %s %s\n", t.created_at, t.id_str, t.user_screen_name, t.in_reply_to_screen_name, t.retweet_count, t.favorite_count, t.retweeted, t.favorited, t.text);
				}
			}
			*/
			if (args.length >= 4){//Test getting status - returns current day only
				String method = "GET";
				String url = "https://api.twitter.com/1.1/statuses/user_timeline.json";
				String oauth_consumer_key = args[0];
				String сonsumer_secret = args[1];
				String oauth_token = args[2];
				String oauth_token_secret = args[3];
				
				String[][] params = new String[][] {
					//new String [] {"user_id","563516882"}, 
					//new String [] {"screen_name","bengoertzel"}, 
					new String [] {"count","200"},//200
					//new String [] {"since_id","false"},
					//new String [] {"max_id","1257053173916659712"},
					new String [] {"trim_user","false"},
					new String [] {"exclude_replies","false"},
					new String [] {"include_rts","true"}
				};
				String result = request(url,method,params,oauth_consumer_key,сonsumer_secret,oauth_token,oauth_token_secret);
				System.out.println(result);//it works - creates status for a user!!!
				
				JsonReader jsonReader = Json.createReader(new StringReader(result));
				JsonArray tweets = jsonReader.readArray();
				System.out.println(tweets.size());
				for (int i = 0; i < tweets.size(); i++) {
					Twit t = new Twit(tweets.getJsonObject(i));
					System.out.format("%tc %s %s %s %s %s %s %s %s\n", t.created_at, t.id_str, t.user_screen_name, t.in_reply_to_screen_name, t.retweet_count, t.favorite_count, t.retweeted, t.favorited, t.text);
				}
			}
			/**/
			
			//- get followers
			//https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-followers-list
			
			//- get friends (followed)
			//https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-friends-list
			
			//- get top 20 likes by users
			//https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/get-favorites-list
			
			
			
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (GeneralSecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

