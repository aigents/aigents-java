/*
 * MIT License
 * 
 * Copyright (c) 2019 by Anton Kolonin, Aigents
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

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Session;
import net.webstructor.util.Str;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.HTTPHandler;
import net.webstructor.comm.HTTPListener;
import net.webstructor.comm.HTTPeer;
import net.webstructor.comm.Mediator;
import net.webstructor.core.Thing;

//https://medium.com/@nadeem.manzoor0/facebook-messenger-platform-web-hook-setup-in-php-893ead06746b#.clhcea94c
//https://developers.facebook.com/docs/messenger-platform/send-api-reference#request
//https://developers.facebook.com/docs/messenger-platform/reference/send-api/
public class Messenger extends Mediator implements HTTPHandler {
	HTTPListener cacheHolder;

	public Messenger(Body env) {
		super(env,"facebook");
	}

	//TODO class HttpBotter extends net.webstructor.comm.Communicator implements HTTPHandler
	public String key(String chat_id,String from_id){
		return (new StringBuilder(name).append(':').append(chat_id).append(':').append(from_id)).toString();
	}
	
	public boolean handleHTTP(HTTPeer parent, String url, String header, String request, String cookie) throws IOException {
		if (!url.startsWith("/facebook"))
			return false;
		if (url.startsWith("/facebook/report_")){
			String key = Str.parseBetween(url, "/facebook/report_", ".html",true);
			String data = parent.parent().retrieve(key);
			body.debug("Facebook report retrieved "+key+" "+(data == null ? "false" : "true"));
			if (data == null)
				parent.respond("","410 Gone","text/plain");
			else
				parent.respond(data,"200 Ok","text/html");
			return true;
		}
		cacheHolder = parent.parent();
		if (!AL.empty(request)){
			//verify URL
			/*if (isset($_GET['hub_verify_token'])) { 
			    if ($_GET['hub_verify_token'] === $verify_token) {
			        echo $_GET['hub_challenge'];
			        return;
			    } else {
			        echo 'Invalid Verify Token';
			        return;
			    }
			}*/
			//body.reply("F:"+url+":"+header+":"+request);
			//Expect:
			//curl -X GET http:/localtest.com:1180/facebook?hub_verify_token=1&hub_challenge=2
			//Actually get:
			//hub.mode=subscribe&hub.challenge=<challenge>&hub.verify_token=<token>
			//Hint:
			//https://stackoverflow.com/questions/20099130/dots-in-urls-replaced-by-underscore
			String get_data = request.replaceAll("\\.", "_");
			String hub_verify_token = Str.parseBetween(get_data, "hub_verify_token=", "&",false);
			String hub_challenge = Str.parseBetween(get_data, "hub_challenge=", "&",false);
			if (!AL.empty(hub_verify_token) && !AL.empty(hub_challenge)){
				body.debug("Facebook verify "+hub_verify_token+" challenge "+hub_challenge);
				String response = hub_verify_token.equals(body.self().getString(Body.facebook_challenge)) ? hub_challenge : "Invalid Verify Token";
				body.debug("Facebook response "+Str.first(response,200));
				parent.respond(response);
				return true;
			}
			
			//handle request 
			/*$input = json_decode(file_get_contents('php://input'), true);
			if (isset($input['entry'][0]['messaging'][0]['sender']['id'])) {

			    $sender = $input['entry'][0]['messaging'][0]['sender']['id']; //sender facebook id
			    $message = $input['entry'][0]['messaging'][0]['message']['text']; //text that user sent

			    if (!empty($message)) {
			        if (strcasecmp($message,'hi') == 0){
			            $message = 'Hi! Welcome to Aigents!';
			            do_send($sender,$message);
			        }else
			        if (strcasecmp($message,'help') == 0){
			            $message = 'Ask one of the three - "news", "sites" or "topics" and get responce for today.';
			            do_send($sender,$message);
			        }else{
			          if (strcasecmp($message,'news') == 0)
			            $message = 'What new true sources, text, times, trust?';
			          else
			          if (strcasecmp($message,'topics') == 0 || strcasecmp($message,'topics') == 0)
			            $message = 'What my topics name, trust?';
			          else
			          if (strcasecmp($message,'sites') == 0)
			            $message = 'What my sites name, trust?';
			          //do_send($sender,'"'.$message.'"='.strlen($message));

			          $reply = get_web_page($sender,"http://aigents.com/al/?" . urlencode($message),"facebook".$sender);
			          */
			body.debug("Facebook request "+request);
			JsonReader jsonReader = Json.createReader(new StringReader(request));
			JsonObject json = jsonReader.readObject();
			/*{"object":"page","entry":[{"id":"271067646410919","time":1553269104824,
			"messaging":[{"sender":{"id":"271067646410919"},"recipient":{"id":"1150212155077420"},
			"timestamp":1553269104377,"message":{"is_echo":true,"app_id":763733953664689,
			"mid":"b00_PuiYIU1wExoW514RM4IQWwICcrPWvbRK1fo6g-bB9Y0vDV3jftF8_yDoGR3D3qlfiayPfEM-zN-Ro2LXvg",
			"seq":66380,"text":"Your name johnny."}}]}]}*/
			//curl --data '{"object":"page","entry":[{"id":"271067646410919","time":1553269104824,"messaging":[{"sender":{"id":"271067646410919"},"recipient":{"id":"1150212155077420"},"timestamp":1553269104377,"message":{"is_echo":true,"app_id":763733953664689,"mid":"b00_PuiYIU1wExoW514RM4IQWwICcrPWvbRK1fo6g-bB9Y0vDV3jftF8_yDoGR3D3qlfiayPfEM-zN-Ro2LXvg","seq":66380,"text":"Your name johnny."}}]}]}' http://localtest.com:1180/facebook
			if (json.containsKey("entry")){
				JsonObject entry = json.getJsonArray("entry").getJsonObject(0);
				if (entry.containsKey("messaging")){
					JsonObject messaging = entry.getJsonArray("messaging").getJsonObject(0);
					String sender = messaging.containsKey("sender") ? messaging.getJsonObject("sender").getString("id") : null;
					String recipient = messaging.containsKey("recipient") ? messaging.getJsonObject("recipient").getString("id") : null;
//					String message = messaging.containsKey("message") ? messaging.getJsonObject("message").getString("text") : null;
					JsonObject messageObj = messaging.containsKey("message") ? messaging.getJsonObject("message") : null;
					String message = messageObj != null && messageObj.containsKey("text") ? messageObj.getString("text") : null;
				    if (!AL.empty(message) && !AL.empty(sender)) {
				    	String sessionKey = key(sender,recipient);//facebook:12345ABCDE
						body.debug("Facebook sender "+sender+" message "+message);
						//TODO: self-authenticate facebook Messenger sessions
		            	net.webstructor.peer.Session session = body.sessioner.getSession(this,sessionKey);
		            	body.conversationer.handle(this, session, message);
		    			parent.respond("");
						return true;
				    }
				}
			}
			//TODO: indicate error?
			parent.respond("Invalid Request");
			return true;
		}//not facebook
		return false;
	}
	
	public void outputFile(String psid, String file) throws IOException {
		//TODO:Facebook can't send files, so need to create retrievable URL
		//put message in session-based storage
		String uuid  = cacheHolder.store(file);
		//return url to base_url/al/facebook/report.html
		String http_base = body.self().getString(Body.http_url,"https://aigents.com/al");
		output(psid,http_base+"/facebook/report_"+uuid+".html");
		body.debug("Facebook report stored "+uuid);
	}

	public void output(Session session, String message) throws IOException {
		//String facebook_id = session.getPeer().getString(Body.facebook_id);
		//if (!AL.empty(facebook_id))
		String sessionKey = session.getKey();
		String psid = Parser.split(sessionKey,":")[1];
		if (message.startsWith("<html>")){
			outputFile(psid,message);
		}else{
			output(psid,message);
		}
	}

	private void output(String psid, String message) throws IOException {
		String url = Feeder.api_url+"me/messages?access_token="+body.self().getString(Body.facebook_token);
		try {
			String data;
			if (!message.startsWith("<html>")) {
			    if (message.length() > 640)
			        message = message.substring(0,637) + "...";
			    //$jsonData = '{"recipient":{"id":"' . $recipient . '"},"message":{"text":' . $message . '}}';
			    JsonObject json = Json.createObjectBuilder()
			    		.add("recipient", Json.createObjectBuilder().add("id", psid))
			    		.add("message", Json.createObjectBuilder().add("text", message))
			    	    .build();
			    data = json.toString();
				body.debug("Facebook response "+data);
				String res = HTTP.simple(url,data,"POST",timeout,"application/json");
				body.debug("Facebook result "+res);
			} else {
				//data = "{\"recipient\":{\"id\":"+psid+"},\"message\":{\"text\":\"Can't do that now... :-(\"}}";
				outputFile(psid,message);
			}
		} catch (Exception e){
			body.error("Facebook error",e);
		}
	}

	//TODO class HttpBotter extends net.webstructor.comm.Communicator implements HTTPHandler
	@Override
	public boolean update(Thing peer, String sessionKey, String subject, String content, String signature) throws IOException {
		String facebook_id = peer.getString(Body.facebook_id);
		String psid = null;
		if (!AL.empty(facebook_id)){//TODO: don't use that because that is Facebook network id, not Facebook Messendger id
			/*
			String login_token = peer.getString(Peer.login_token);
			//body.debug("Facebook updating id "+facebook_id+" session "+login_token+" text "+content);
			if (!AL.empty(login_token)){
				String[] ids = Parser.split(login_token,":");
				if (ids != null && name.equals(ids[0]) && ids.length == 3)//facebook:psid:psuid
					psid = ids[1];
			}
			*/
			psid = getTokenSegment(peer.getString(Peer.login_token),1);
		}
		if (psid == null)
			psid = getPeerSession(peer);
		if (!AL.empty(psid)){
			StringBuilder sb = new StringBuilder();
			if (!AL.empty(subject))
				sb.append(subject).append('\n');
			sb.append(content);
			//if (!AL.empty(signature))
			//	sb.append('\n').append(signature);
			body.debug("Facebook update id "+facebook_id+" psid "+psid+" text "+content);
			output(psid, sb.toString());
			return true;
		}
		return false;
	}
}
