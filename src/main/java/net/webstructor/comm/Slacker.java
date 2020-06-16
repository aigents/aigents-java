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
package net.webstructor.comm;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.core.Anything;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Session;
import net.webstructor.util.Str;

//https://api.slack.com/docs/oauth
//https://api.slack.com/methods/oauth.access
//https://api.slack.com/tutorials/app-creation-and-oauth
//https://api.slack.com/bot-users
//https://api.slack.com/docs/slack-button#add_the_slack_button
//https://api.slack.com/tutorials/your-first-slash-command

public class Slacker extends Mediator implements HTTPHandler {

	private HashMap groups = new HashMap();
	
	public Slacker(Body body) {
		super(body,"slack");
	}
	
	//TODO: move to Mediator but make sure about 'facebook_id' clash?
	public void login(Session session, Anything peer) {
		peer.setString(Body.slack_id, ids(session.getKey())[2]);
	}
	
	//https://api.slack.com/methods/channels.info
	private String groupName(String id){
		String n = null;
		synchronized(groups){
			n = (String)groups.get(id);
		}
		if (AL.empty(n)){
			try {
				String url = "https://slack.com/api/channels.info";
				String token = body.self().getString(Body.slack_token);
				String par = "token="+token+"&channel="+id;
				body.debug("Slack channel request "+par);
				String res = HTTP.simple(url, par, "POST", timeout);
				body.debug("Slack channel response "+res);
				JsonReader jsonReader = Json.createReader(new StringReader(res));
				JsonObject json = jsonReader.readObject();
				if (json.containsKey("channel")){
					JsonObject channel = json.getJsonObject("channel");
			        n = HTTP.getJsonString(channel, "name");
					synchronized(groups){
						groups.put(id,n);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				body.error("Slack channel error",e);
			}
		}
		return n;
	}
	
	/**
	 * @param args - array with url, header, request
	 * @return true if handled
	 * @throws IOException
	 */
	public boolean handleHTTP(HTTPeer parent, String url, String header, String request, String cookie) throws IOException {
		if (url.startsWith("/slack")){
			if (!AL.empty(request)){
				body.debug("Slack input "+request);
				String code = null;
				String state = null;
				String challenge = null;
				String type = null;
				String subtype = null;
				String user = null;
				String channel = null;
				String text = null;
				String channel_type = null;
				String[] params = Parser.split(request, "&");
				for (int i = 0; i < params.length; i++){
					String[] keyvalue = Parser.split(params[i], "=");
					if (keyvalue.length == 2){
						if (keyvalue[0].equals("code"))
							code = keyvalue[1];
						if (keyvalue[0].equals("state"))
							state = keyvalue[1];
					}
				}
				if (header.startsWith("POST")){
					JsonReader jsonReader = Json.createReader(new StringReader(request));
					JsonObject json = jsonReader.readObject();
					if (json.containsKey("event")){
						JsonObject event = json.getJsonObject("event");
				        type = HTTP.getJsonString(event, "type");
				        subtype = HTTP.getJsonString(event, "subtype");
				        text = HTTP.getJsonString(event, "text");
				        user = HTTP.getJsonString(event, "user");
				        channel = HTTP.getJsonString(event, "channel");
				        channel_type = HTTP.getJsonString(event, "channel_type");
						
					} //else {//verification
					//https://api.slack.com/events-api#events_api_request_urls
					//token = HTTP.getJsonString(json, "token");
					challenge = HTTP.getJsonString(json, "challenge");
					type = HTTP.getJsonString(json, "type");
					//}
				}

				String debug = "type="+type+" challenge="+challenge+" code="+code+" state="+state+" user="+user+" channel="+channel+" channel_type="+channel_type+" text="+text;
				body.debug("Slack debug "+debug);
				
				if (!AL.empty(challenge) && "url_verification".equals(type)){//url verification
					parent.respond(challenge);
					return true;
				}else
				//if (!AL.empty(code) && !AL.empty(state)){//user authentication
				if (!AL.empty(code)){//user authentication
					//https://api.slack.com/docs/slack-button
					//https://api.slack.com/tutorials/app-creation-and-oauth
					//https://api.slack.com/methods/oauth.access
					//https://api.slack.com/docs/oauth
//TODO:check "state", according to
					//https://api.slack.com/slack-apps#direct_install
					String api = "https://slack.com/api/oauth.access";
					String client_id = body.self().getString(Body.slack_id);
					String client_secret = body.self().getString(Body.slack_key);
					String par = "code="+code+"&client_id="+client_id+"&client_secret="+client_secret;
					//String res = HTTP.simple(api, par, "POST", 0, "application/x-www-form-urlencoded");
					body.debug("Slack input "+request);
					body.debug("Slack auth request "+par);
					String res = HTTP.simple(api, par, "POST", timeout);
					body.debug("Slack auth response "+res);
					JsonReader jsonReader = Json.createReader(new StringReader(res));
					JsonObject json = jsonReader.readObject();
					if (json.getBoolean("ok")){
//TODO: identify user on basis of the code!?
						;
						//String token = HTTP.getJsonString(json, "access_token");
						//body.self().setString(Body.slack_token, token);
					}
					parent.respond("<html><body>Return to<a href=\""+body.site()+"\">"+body.site()+"</a></body></html>","301 Moved Permanently\nLocation: "+body.site(),"text/plain");
					return true;
				}else
				if (AL.empty(type) || AL.empty(user) || AL.empty(text)){//ignore what we dont' understand
					parent.respond("");
					return true;
				} else
				if ("im".equals(channel_type)
						&& AL.empty(subtype)) //discard subtype==file_share, because it may have not a bot user for bot uploads!?
				{//private channel messages
//TODO: translate "commands" at conversational level
					if (AL.news.equalsIgnoreCase(text))
						text = "What new true sources, text, times, trust?";
					else
					if (AL.topics.equalsIgnoreCase(text))
						text = "What my topics name, trust?";
					else
					if (AL.sites.equalsIgnoreCase(text))
						text = "What my sites name, trust?";
					else
						text = decodeEmail(text);
					parent.respond("");
	            	net.webstructor.peer.Session session = body.sessioner.getSession(this,key(channel,user));
	            	body.conversationer.handle(this, session, text);
					return true;
				}else

//TODO: how to treat channel with multiple users - as "channel_type":"channel" or "channel_type":"im"
					
				if ("channel".equals(channel_type)){//TODO: if message to public channel
					parent.respond("");//respond first, to avoid resend
					
					/*					
					{"token":"XXXXXXXXXXXX","team_id":"YYYY","api_app_id":"ZZZZ",
					"event":{"client_msg_id":"AAAAAAAAAAAA","type":"message",
					"text":"hifromsnet1","user":"BBBBBB","ts":"1556455265.001000","channel":"CGGNK2VGE",
					"event_ts":"1556455265.001000","channel_type":"channel"},"type":"event_callback",
					"event_id":"DDDDDDD","event_time":1556455265,"authed_users":["ZZZZZZ"]}
					*/
					//TODO:1) get channel name
					String channel_name = groupName(channel);
					//TODO:2) check if user is bot
					boolean is_bot = false;//TODO check if is bot
					updateGroup(channel, channel_name, user, user/*name*/, true, is_bot, text);
					
//TODO: be able to do unauthorized group conversations and enable chat message handling!
					
					return true;
				}else{//ignore everything else
					parent.respond("");
					return true;
				}
			}
		}
		return false;
	}
	
	String decodeEmail(String text){
		for (;;){
			int email_pos = text.indexOf("<mailto:");
			if (email_pos == -1)
				break;
		    int email_mid = text.indexOf("|",email_pos);
		    int email_end = text.indexOf(">",email_pos);
		    if (email_mid > email_pos && email_end > email_mid){
		    	int pos = email_pos+8;
		    	String email = text.substring(pos,email_mid);
			    text = text.substring(0,email_pos) + email + text.substring(email_end+1);
			    body.debug("Slack email:"+email+":"+text);
		    }
		}
		return text;
	}
	
	
	public void output(Session session, String message) throws IOException {
		String[] ids = ids(session.getKey());
		if (!AL.empty(ids)){
			//TODO: support group conversations
			output(ids[1], body.self().getString(Body.slack_token), message);
		}
	}

	public void output(String channel, String token, String message) throws IOException {
		try {
			if (!message.startsWith("<html>")) {
				String url = "https://slack.com/api/chat.postMessage";
//TODO limit slack messages or send them as files!!!
				//if (message.length() > 4096)
				//	message = message.substring(0,4093) + "...";
				//send text message
				String content = URLEncoder.encode(message,"UTF-8");
				String par = "token="+token+"&channel="+channel+"&text="+content;
				body.debug("Slack chat request "+par);
				String res = HTTP.simple(url, par, "POST", timeout);
				body.debug("Slack chat response "+res);
//TODO: slack reports
			} else {
				//https://api.slack.com/methods/files.upload
				String url = "https://slack.com/api/files.upload";
				String title = Str.parseBetween(message, "<title>", "</title>");
				if (AL.empty(title))
					title = "Report";
				
				String boundary = "--bndry--";
				String par = "\r\n"
					+"--"+boundary+"\r\n"
		  			+"Content-Disposition: form-data; name=\"token\"\r\n\r\n"+token+"\r\n"
					+"--"+boundary+"\r\n"
		  			+"Content-Disposition: form-data; name=\"channels\"\r\n\r\n"+channel+"\r\n"
					+"--"+boundary+"\r\n"
		  			//+"Content-Disposition: form-data; name=\"title\"\r\n\r\n"+title+"\r\n"
		  			+"Content-Disposition: form-data; name=\"initial_comment\"\r\n\r\n"+title+"\r\n"
					+"--"+boundary+"\r\n"
		  			+"Content-Disposition: form-data; name=\"filetype\"\r\n\r\nhtml\r\n"
					+"--"+boundary+"\r\n"
		  			+"Content-Disposition: form-data; name=\"filename\"\r\n\r\nreport.html\r\n"
					+"--"+boundary+"\r\n"
		  			+"Content-Disposition: form-data; name=\"content\"\r\n"
		            +"Content-Type: text/html; charset=utf-8\r\n"
		            //+"Content-Length: "+message.length()+"\r\n"
		            +"\r\n"
		            +message+"\r\n"
					+"--"+boundary+"--\r\n";
				
				body.debug("Slack report request channel "+par);
				String response = HTTP.simple(url,par,"POST",timeout,"multipart/form-data; boundary="+boundary);
				
				/*
				title= URLEncoder.encode(title,"UTF-8");
				String content = URLEncoder.encode(message,"UTF-8");
				//String par = "token="+token+"&channels="+channel+"&title="+title+"&filetype=HTML&filename=report.html&content="+content;
				String par = "token="+token+"&channels="+channel+"&filetype=html&filename=report.html&content="+content;
				body.debug("Slack chat request "+par);
				String response = HTTP.simple(url, par, "POST", timeout);
				*/
				
				body.debug("Slack report response "+Str.first(response,200));
				JsonReader jr = Json.createReader(new StringReader(response));
				JsonObject result = jr.readObject();
				if (!HTTP.getJsonBoolean(result, "ok", false))
					throw new Exception("Slack error files.upload: "+result.toString());
			}
		} catch (Exception e){
			body.error("Slack chat error",e);
		}
	}

	@Override
	public boolean update(Thing peer, String sessionKey, String subject, String content, String signature) throws IOException {
		String self_lack_token = body.self().getString(Body.slack_token);
		String psid = null;
		if (!AL.empty(self_lack_token)){
			/*String login_token = peer.getString(Peer.login_token);
			if (!AL.empty(login_token)){
				String[] ids = Parser.split(login_token,":");
				if (ids != null && name.equals(ids[0]) && ids.length == 3)//facebook:psid:psuid
					psid = ids[1];
			}*/
			psid = getTokenSegment(peer.getString(Peer.login_token),1);
		}
		if (psid == null)
			psid = getPeerSession(peer);
		body.debug("Slack updating psid "+psid+" text "+content);
		if (!AL.empty(psid)){
			StringBuilder sb = new StringBuilder();
			if (!AL.empty(subject))
				sb.append(subject).append('\n');
			sb.append(content);
			//if (!AL.empty(signature))
			//	sb.append('\n').append(signature);
			body.debug("Slack update psid "+psid+" text "+content);
			output(psid, self_lack_token, sb.toString());
			return true;
		}
		return false;
	}
}

