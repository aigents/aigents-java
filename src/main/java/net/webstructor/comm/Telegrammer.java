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
package net.webstructor.comm;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body; 
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Period;
import net.webstructor.core.Anything;
import net.webstructor.core.Thing;
import net.webstructor.core.Updater;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Session;
import net.webstructor.self.Siter;
import net.webstructor.util.MapMap;

/*
TODO:
- OAuth for Site
	https://medium.com/@alexandershogenov/%D0%B4%D0%B5%D0%BB%D0%B0%D0%B5%D0%BC-oauth-%D0%B0%D0%B2%D1%82%D0%BE%D1%80%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%8E-telegram-%D0%BD%D0%B0-%D1%81%D0%B2%D0%BE%D1%91%D0%BC-%D1%81%D0%B0%D0%B9%D1%82%D0%B5-74d0d63095b0
	https://core.telegram.org/widgets/login
- Bots
	Functions (same as for Slack!?)
		- get messages across chats/groups
		- build reports across chats/groups
		- build social graphs across chats/groups (each is a hyperlink linking its members)
		- study dynamics of topics
		- study reputations implicitly/probabilistically (because of no explicit ratings)
		- answer questions in groups ("anonymous" no-auth mode)
		- provide alerts in groups ("anonymous" no-auth mode)
			https://www.shellhacks.com/ru/telegram-api-send-message-personal-notification-bot/
	https://tlgrm.ru/docs/bots/api
	https://core.telegram.org/bots
		- TODO use in groups
		- TODO web login
		- TODO inline mode
			https://core.telegram.org/bots/inline
				https://core.telegram.org/bots/api#inline-mode
 Resources:
	https://core.telegram.org/bots/api#senddocument
	https://core.telegram.org/bots/api#sending-files
	https://philsturgeon.uk/api/2016/01/04/http-rest-api-file-uploads/
 */
public class Telegrammer extends Communicator implements Updater {
	
	public static final long PERIOD_MIN = 1*Period.SECOND; 
	public static final long PERIOD_MAX = 16*Period.SECOND; 
	
	protected Thing self;
	protected int timeout = 0;
	protected String base_url = "https://api.telegram.org/bot";
	
	public Telegrammer(Body env) {
		super(env,"telegram");
		self = body.self();
		env.register(name, this);
		body.debug("Telegram registered.");
	}
	
//TODO: move to Grouper parent abstract class for this or Slack and/or move to Conversation scope 
// - adding session attributes?
// - adding dedicated unauthorized chat sessions?
	protected void updateGroup(String group_id, String group_name, String peer_id, String peer_name, boolean is_in, boolean is_bot, String text){
		try {
			//1) get group by id (eg. "telegram_id")
			String name_id = name+" id";
			String full_group_name = name + ":" + group_name;
			Collection g = body.storager.getByName(name_id, group_id);
			Thing group;
			if (!AL.empty(g)){
				group = (Thing)g.iterator().next();
				group.setString(AL.name, full_group_name);
			}else{
				//2) if missed, create group with name
				group = new Thing(full_group_name);
				group.setString(name_id,group_id);
				group.storeNew(body.storager);
			}
body.debug("Telegram name_id "+name_id+" group_name "+group_name+" group "+group.toString());//TODO: remove debug
			//3) if (!is_bot), add/remove peer to/from group
			if (!is_bot){//TODO: hadle bots as well!?
				//4) get peer by id (eg. "telegram_id")
				Collection p = body.storager.getByName(name_id, peer_id);
				Thing peer;
				if (!AL.empty(p)){
					peer = (Thing)p.iterator().next();
					if (is_in){
						group.addThing(AL.members, peer);
						peer.addThing(AL.groups, group);
					}else{ 
						group.delThing(AL.members, peer);
						peer.delThing(AL.groups, group);
					}
				}else{
					;//TODO: add new peers dynamically
				}
			}
			//5) get all group users, do for each:
			Collection m = group.getThings(AL.members);
			if (!AL.empty(m)) for (Iterator mit = m.iterator(); mit.hasNext();){
				Thing p = (Thing)mit.next();
				//6) get all user topics, do for each
				Collection k = p.getThings(AL.knows);
				Collection t = p.getThings(AL.trusts);
				if (!AL.empty(k) && !AL.empty(t)){//keep trusted topics only
					t = new HashSet(t);
					t.retainAll(k);
				}
				//7) match text against topic
				MapMap thingPaths = new MapMap();//collector
				Date today = Time.today(0);
				Iter parse = new Iter(Parser.parse(text));
				if (!AL.empty(t)) for (Iterator tit = t.iterator(); tit.hasNext();)
					Siter.match(body.storager, parse, null, (Thing)tit.next(), today, full_group_name, null, thingPaths, null, null);
				//8) send update if topic is matched
//TODO: exclude sender in the news update
				Siter.update(body,null,today,thingPaths,true,group);//forced
			}
		} catch (Exception e) {
			body.error("Group "+name+" error", e);
		}
	}
	
	public void output(Session session, String message) throws IOException {
		String chat_id = null;
		String[] ids = ids(session.getKey());
		if (ids != null)
			//TODO: support group conversations
			chat_id = ids[1];//chat_id
			//chat_id = ids[2];//from_id
		if (AL.empty(chat_id))
			chat_id = session.getPeer().getString(Body.telegram_id);
		if (!AL.empty(chat_id))
			output(chat_id, message);
	}
	
	private void output(String chat_id, String message) throws IOException {
		String token = self.getString(Body.telegram_token,null);
		try {
			if (!message.startsWith("<html>")) {
				if (message.length() > 4096)
					message = message.substring(0,4093) + "...";
				//send text message
				String url = base_url+token+"/sendMessage";
				String par = "chat_id="+chat_id+"&text="+URLEncoder.encode(message,"UTF-8");
				HTTP.simple(url,par,"POST",timeout);
			} else {
				//https://habr.com/sandbox/103022/
				String boundary = "--bndry--";
				String url = base_url+token+"/sendDocument";
				String par = "\r\n"
					+"--"+boundary+"\r\n"
		  			+"Content-Disposition: form-data; name=\"chat_id\"\r\n"
		            +"\r\n"
		            +chat_id+"\r\n"
					+"--"+boundary+"\r\n"
		  			+"Content-Disposition: form-data; name=\"document\"; filename=\"report.html\"\r\n"
		            +"Content-Type: text/html; charset=utf-8\r\n"
		            +"\r\n"
		            +message+"\r\n"
					+"--"+boundary+"--\r\n";
				String response = HTTP.simple(url,par,"POST",timeout,"multipart/form-data; boundary="+boundary);
				JsonReader jr = Json.createReader(new StringReader(response));
				JsonObject result = jr.readObject();
				if (!HTTP.getJsonBoolean(result, "ok", false))
					throw new Exception("Telegram error sendDocument: "+result.toString());
			}
		} catch (Exception e){
			body.error("Telegram error",e);
		}
	}
	
	private long handle(String response) throws IOException{
		long offset = -1;
		JsonReader jr = Json.createReader(new StringReader(response));
		JsonObject result = jr.readObject();
		if (HTTP.getJsonBoolean(result, "ok", false)){
			JsonArray items = HTTP.getJsonArray(result,"result");
			if (items != null) for (int i = 0; i < items.size(); i++){
				JsonObject o = items.getJsonObject(i);
				long update_id = HTTP.getJsonLong(o,"update_id",-1);
				if (update_id == -1)
					continue;//TODO: what?
				if (offset < update_id)
					offset = update_id;
				
				//TODO: track new members in the group 
				//https://stackoverflow.com/questions/34567920/in-python-telegram-bot-how-to-get-all-participants-of-the-group
				//https://core.telegram.org/bots/api#message
				//new_chat_members, left_chat_member
				
				JsonObject m = HTTP.getJsonObject(o,"message");
				if (m == null)
					continue;
body.debug("Telegram message "+m.toString());//TODO: remove debug
				
				//{"message_id":151,
				//"from":{"id":302910826,"is_bot":false,"first_name":"Anton","last_name":"Kolonin","username":"akolonin","language_code":"ru"},
				//"chat":{"id":-1001115260760,"title":"Aigents","type":"supergroup"},
				//"date":1553863575,"text":"test5"}	
				JsonObject from = HTTP.getJsonObject(m, "from");
				JsonObject chat = HTTP.getJsonObject(m, "chat");
				long unix = m.getInt("date");
				String text = HTTP.getJsonString(m, "text", null);
				String chat_title = chat.containsKey("title")? chat.getString("title") : null;
				if (from == null || chat == null || unix == 0 || AL.empty(text))
					continue;
				
				//TODO: handle group in/out events even if AL.empty(text)
				
				Date date = new Date( unix * 1000 );
				String from_id = String.valueOf(HTTP.getJsonLong(from, "id", 0L));
				boolean from_bot = HTTP.getJsonBoolean(from, "is_bot", false);
				String from_username = HTTP.getJsonString(from, "username", null);
//TODO: use for auth/registration and account binding
				//String from_name = HTTP.getJsonString(from, "first_name", null);
				//String from_surname = HTTP.getJsonString(from, "last_name", null);
//TODO: use for set-language
				//String from_language = HTTP.getJsonString(from, "language_code", null);
				String chat_id = String.valueOf(HTTP.getJsonLong(chat, "id", 0L));
//TODO: distinguish private and group chats
				//String chat_type = HTTP.getJsonString(chat, "type", "private");//TODO:private by default!?
				if (AL.empty(from_username) || AL.empty(from_id) || from_bot)
					continue;
				//TODO: autoregister:
				//telegram id = from_id
				//name, surname, username@telegram.org
				body.debug("Telegram date "+date+" from_username "+from_username+" text "+text);
				
				if (!from_id.equals(chat_id)){
					boolean is_bot = from.containsKey("is_bot")? from.getBoolean("is_bot") : false; 
					updateGroup(chat_id, chat_title, from_id, from_username, true, is_bot, text);
//TODO: be able to do unauthorized group conversations and enable chat message handling!
					continue;
				} 
				
				//TODO: use for session id either
				//from_id - for private authenticated sessions
				//chat_id - for public anonymous sessions
				//TODO: don't try to authenticate other users in public anonymous sessions
            	net.webstructor.peer.Session session = body.sessioner.getSession(this,key(chat_id,from_id));
            	body.conversationer.handle(this, session, text);
			}
		}
		return offset;
	}
	
	public void run(  ) {
		try {
			body.debug("Telegrammer started.");
			long offset = AL.integer(self.getString(Body.telegram_offset,"-1"),-1L);
			long period = PERIOD_MIN;
			String current_token = "";
			while (alive()) {
				//TODO: configurable period
				try {
					Thread.sleep(period);
					String token = self.getString(Body.telegram_token,null);
					if (token == null)
						continue;
					if (!token.equals(current_token)){
						String response = HTTP.simple(base_url+token+"/deleteWebhook","","POST",timeout);
						body.debug("Telegram deleteWebhook "+token+": "+response);
						current_token = token;
					}
					String url = base_url+token+"/getUpdates";
					//parameter=value&also=another
					String par = "offset="+Long.toString(offset);
					String response = HTTP.simple(url,par,"POST",timeout);
					long new_offset = handle(response);
					if (new_offset != -1){
						self.setString(Body.telegram_offset,Long.toString(offset = ++new_offset));
						if (period > PERIOD_MIN)
							period /= 2;
					}else{
						if (period < PERIOD_MAX)
							period *= 2;
					}
				} catch (Exception e) {		
					body.error("Telegrammer error:",e);
				}
			}
			body.debug("Telegrammer stopped.");
		} catch (Exception e) {
			body.error("Telegrammer error:",e);
		}
	}

	public void login(Session session, Anything peer) {
		peer.setString(Body.telegram_id, ids(session.getKey())[2]);
	}
	
	//TODO class HttpBotter extends net.webstructor.comm.Communicator implements HTTPHandler
	public boolean update(Thing peer, String subject, String content, String signature) throws IOException {
		String from_id = peer.getString(Body.telegram_id);
		if (AL.empty(from_id)){//backup path - get from id from the session
			String login_token = peer.getString(Peer.login_token);
			if (!AL.empty(login_token)){
				String[] ids = login_token == null ? null : ids(login_token);
				if (ids != null && name.equals(ids[0]) && ids[1].equals(ids[2]))//if there is a private conversation session
					from_id = ids[2];
			}
		}
		if (!AL.empty(from_id)){
			StringBuilder sb = new StringBuilder();
			if (!AL.empty(subject))
				sb.append(subject).append('\n');
			sb.append(content);
			//if (!AL.empty(signature))
			//	sb.append('\n').append(signature);
			output(from_id, sb.toString());
			return true;
		}
		return false;
	}
}
