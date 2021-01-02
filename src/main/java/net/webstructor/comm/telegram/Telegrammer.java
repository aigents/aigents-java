/*
 * MIT License
 * 
 * Copyright (c) 2005-2020 by Anton Kolonin, Aigents®
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
package net.webstructor.comm.telegram;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.Mediator;
import net.webstructor.comm.Socializer;
import net.webstructor.comm.telegram.Telegram;
import net.webstructor.core.Anything;
import net.webstructor.core.Thing;
import net.webstructor.data.Emotioner;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Session;
import net.webstructor.util.JSON;
import net.webstructor.util.Str;

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
public class Telegrammer extends Mediator {
	
	public static final long PERIOD_MIN = 1*Period.SECOND; 
	public static final long PERIOD_MAX = 16*Period.SECOND; 
	
	protected Thing self;
	protected String base_url = "https://api.telegram.org/bot";
	
	protected static final String[] key_names = new String[] {Body.telegram_id,Body.telegram_name};
	

	java.util.Set<String> getIdsByUsernames(java.util.Set<String> usernames) {
		if (usernames == null)
			return null;
		java.util.Set<String> ids = new HashSet<String>();
		for (String username : usernames) {
			String id = getIdByUsername(username);
			if (!AL.empty(id))
				ids.add(id);
		}
		return ids;
	}

	String getIdByUsername(String username) {
		String id = null;
		if (AL.empty(id)) {
			try {
				Collection by_name = body.storager.getByName(Body.telegram_name,username);
				if (AL.single(by_name)) for (Object o : by_name)
					return ((Thing)o).getString(Body.telegram_id);
			} catch (Exception e) {}
		}
		//https://ru.stackoverflow.com/questions/906172/%D0%9A%D0%B0%D0%BA-%D0%B2-telegram-%D1%83%D0%B7%D0%BD%D0%B0%D1%82%D1%8C-username-%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D1%82%D0%B5%D0%BB%D1%8F-%D0%B8%D0%BC%D0%B5%D1%8F-id
		//https://stackoverflow.com/questions/37644603/is-it-possible-to-get-userid-using-user-nickname
		//users.getFullUser ?
		return id;
	}
	
	//update usernames on the fly because users may change them!!!
	Thing putIdByUsername(String username, String id) {
		Thing peer = null;
		try {
			Collection by_id = body.storager.getByName(Body.telegram_id,id);
			if (AL.single(by_id)) for (Object o : by_id)
				(peer = (Thing)o).setString(Body.telegram_name,username);
//TODO: if we do this, how do we register/bind them later?
			else {
				peer = new Thing(body.storager.getNamed(Schema.peer),null,null);
				peer.setString(Body.telegram_name, username);
				peer.setString(Body.telegram_id, id);
				peer.storeNew(body.storager);
			}
		} catch (Throwable e) {
			body.error("Telegram error",e);
		}
		return peer;
	}
	
	public Telegrammer(Body env) {
		super(env,"telegram");
		self = body.self();
	}
	
	private String chatId(String sessionKey,Thing peer) {
		String chat_id = null;
		String[] ids;
		if (sessionKey != null && (ids = ids(sessionKey)) != null)
			//support group conversations
			chat_id = ids[1];//chat_id
			//chat_id = ids[2];//from_id
		if (AL.empty(chat_id) && peer != null)
			chat_id = peer.getString(Body.telegram_id);
		return chat_id;
	}
	
	public void output(Session session, String message) throws IOException {
		String chat_id = chatId(session.getKey(),session.getPeer());
		if (!AL.empty(chat_id)) {
			String reply_to_msg_id = (String)session.context("reply_to_msg_id");
			output(chat_id, reply_to_msg_id, message);
		}
	}
	
	private void output(String chat_id, String message) throws IOException {
		output(chat_id, null, message);
	}
	private void output(String chat_id, String reply_to_msg_id, String message) throws IOException {
		String token = self.getString(Body.telegram_token,null);
		try {
			if (!message.startsWith("<html>")) {
				if (message.length() > 4096)
					message = message.substring(0,4093) + "...";
				//send text message
				//https://core.telegram.org/bots/api
				String url = base_url+token+"/sendMessage";
				String par = "chat_id="+chat_id+"&text="+URLEncoder.encode(message,"UTF-8");
				if (!AL.empty(reply_to_msg_id))
//TODO: make silent an optional argument
					par = "reply_to_message_id="+reply_to_msg_id+"&disable_notification=true&" + par;
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
		} catch (Throwable e){
			body.error("Telegram error",e);
		}
	}

	private void forward(String forward_chat_or_user_id, String from_chat_id, String msg_id) throws IOException {
		String token = self.getString(Body.telegram_token,null);
		try {
			//https://core.telegram.org/bots/api#forwardmessage
			String url = base_url+token+"/forwardMessage";
			String par = "chat_id="+forward_chat_or_user_id+"&from_chat_id="+from_chat_id+"&message_id="+msg_id;
			String response = HTTP.simple(url,par,"POST",timeout);
			body.debug("Telegram report response "+response);
		} catch (Throwable e){
			body.error("Telegram error",e);
		}
	}
	
	private void report(String chat_username, String chat_title, String chat_id, String msg_id) throws IOException {
		String token = self.getString(Body.telegram_token,null);
		try {
			//https://core.telegram.org/bots/api#getchatadministrators
			String url = base_url+token+"/getChatAdministrators";
			String par = "chat_id="+chat_id;
			String res = HTTP.simple(url,par,"POST",timeout);
			if (!AL.empty(res)) {
				JsonReader jr = Json.createReader(new StringReader(res));
				JsonObject o = jr.readObject();
				JsonArray array = o.getJsonArray("result");
				if (array.size() > 0) {
					for (int i = 0; i < array.size(); i++) {
						JsonObject admin = array.getJsonObject(i);
						JsonObject user = admin.getJsonObject("user");
						String user_id = JSON.getJsonLongString(user, "id", null);
						boolean is_bot = HTTP.getJsonBoolean(user, "is_bot", false);
						if (!is_bot && !AL.empty(user_id)) {
							forward(user_id,chat_id,msg_id);
							output(user_id,messageLink(chat_username, chat_title, chat_id, msg_id));
						}
					}
				}
			}
		} catch (Throwable e){
			body.error("Telegram error",e);
		}
	}
	
	//https://core.telegram.org/bots/api#deletemessage
	private void delete(String chat_id, String msg_id) throws IOException {
		String token = self.getString(Body.telegram_token,null);
		try {
			String url = base_url+token+"/deleteMessage";
			String par = "chat_id="+chat_id+"&message_id="+msg_id;
			HTTP.simple(url,par,"POST",timeout);
		} catch (Throwable e){
			body.error("Telegram error",e);
		}
	}

	protected static String getMessageLink(String group_usename, String group_title, String group_id, String message_id) {
		//https://stackoverflow.com/questions/51065460/link-message-by-message-id-via-telegram-bot
		if (!AL.empty(group_usename)) {
			//https://t.me/agirussia/8855 (public - agirussia, 8855)
			return "https://t.me/"+group_usename+"/"+message_id;
		}
		if (group_id.startsWith("-100")) {
			//https://t.me/c/1410910487/75 (private -1001410910487, 75)
			group_id = group_id.substring(4);
			return "https://t.me/c/"+group_id+"/"+message_id;
		}
		return null;
	}
	
	@Override
	protected String messageLink(String group_usename, String group_title, String group_id, String message_id) {
		String link = getMessageLink(group_usename, group_title, group_id, message_id);
		if (!AL.empty(link))
			return link;
		return super.messageLink(group_usename, group_title, group_id, message_id);
	}
	
	private long handle(String response) throws IOException{
		String botname = body.getSelf().getString(Body.telegram_name);
		Socializer socializer = body.getSocializer(name);
		Telegram telegram =  socializer != null && socializer instanceof Telegram? (Telegram)socializer : null;
		
		int rudeness_threshold = ((Thing)body.getSelf()).getInt(Body.rudeness_threshold, 30);// 30;
		int sentiment_threshold = ((Thing)body.getSelf()).getInt(Body.sentiment_threshold, 90);// 90;
		
		long offset = -1;
		JsonReader jr = Json.createReader(new StringReader(response));
		JsonObject result = jr.readObject();
		JsonArray items;
		if (HTTP.getJsonBoolean(result, "ok", false) && (items = HTTP.getJsonArray(result,"result")) != null && items.size() > 0){
			for (int i = 0; i < items.size(); i++){
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
				JsonObject reply_to = HTTP.getJsonObject(m, "reply_to_message");
				long unix = m.getInt("date");
				String text = HTTP.getJsonString(m, "text", null);
				if (AL.empty(text))
					text = HTTP.getJsonString(m, "caption", null);
				String message_id = JSON.getJsonLongString(m, "message_id", "");
				String chat_title = chat.containsKey("title")? chat.getString("title") : null;
				String chat_username = chat.containsKey("username")? chat.getString("username") : null;
				if (from == null || chat == null || unix == 0 || AL.empty(text))
					continue;
				
//TODO: handle group in/out events even if AL.empty(text), make sure if memebers are identified correctly!
				
				Date date = new Date( unix * 1000 );
				String from_id = JSON.getJsonLongString(from, "id", null);
				boolean from_bot = HTTP.getJsonBoolean(from, "is_bot", false);
				String from_username = HTTP.getJsonString(from, "username", null);
				
//TODO: use for auth/registration and account binding
				//String from_name = HTTP.getJsonString(from, "first_name", null);
				//String from_surname = HTTP.getJsonString(from, "last_name", null);
//TODO: use for set-language
				String from_language = HTTP.getJsonString(from, "language_code", null);
				String chat_id = String.valueOf(HTTP.getJsonLong(chat, "id", 0L));
//TODO: distinguish private and group chats
				//String chat_type = HTTP.getJsonString(chat, "type", "private");//TODO:private by default!?

				//if (AL.empty(from_username) || AL.empty(from_id))
				if (AL.empty(from_id))//from_username may be null!?
					continue;
				
				if (!AL.empty(from_username))
					putIdByUsername(from_username,from_id);
				
//TODO handle bots (original messages are not delivered yet)?				
				if (from_bot)//skipping replies from bots so far
					continue;
				
//TODO TelegramItem class constructor for
/*
			"reply_to_message":{
				"message_id":511,
				"from":{
					"id":189518891,
					"is_bot":false,
					"first_name":"POST",
					"last_name":"HUMAN",
					"username":"Antropocosmist"
				},
				"chat":{"id":-1001410910487,"title":"AigentsTest","type":"supergroup"},
				"date":1608909457,
				"text":"Рад стараться ради победы доброго ИИ над глупостью и невежеством!))"
			}

*/
				String reply_to_from_id = null;
				//Date reply_to_date = null;
				//String reply_to_text = null;
				String reply_to_from_username = null;
				boolean reply_to_from_bot = false;
				String reply_to_message_id = null;
				if (reply_to != null) {
					reply_to_message_id = JSON.getJsonLongString(reply_to, "message_id", null);
					//reply_to_date = new Date( reply_to.getInt("date") * 1000 );
					//reply_to_text = HTTP.getJsonString(reply_to, "text", null);
					JsonObject reply_to_from = HTTP.getJsonObject(reply_to, "from");
					if (reply_to_from != null) {
						reply_to_from_bot = HTTP.getJsonBoolean(reply_to_from, "is_bot", false);
						reply_to_from_id = JSON.getJsonLongString(reply_to_from, "id", null);
						reply_to_from_username = HTTP.getJsonString(reply_to_from, "username");
						if (AL.empty(reply_to_from_id))
							reply_to_from_id = getIdByUsername(reply_to_from_username);
						else
							putIdByUsername(reply_to_from_username,reply_to_from_id);
//TODO handle bots (original messages are not delivered yet)?				
						if (reply_to_from_bot) {
							reply_to_from_id = null;
							reply_to_from_username = null;
							reply_to_message_id = null;
						}
					}
				}				
				
//TODO: autoregister:
				//telegram id = from_id
				//name, surname, username@telegram.org
				body.debug("Telegram date "+date+" from_id "+from_id+" from_username "+from_username+" text "+text);			
				
				HashSet<String> mention_usernames = getMentions(text);//get mentions
				if (mention_usernames != null && !AL.empty(botname) && mention_usernames.contains(botname))//remove mention to bot self from text
					text = text.replace("@"+botname, "");

				if (!from_id.equals(chat_id)){
					updateGroup(message_id, chat_id, chat_username, chat_title, from_id, true, from_bot, text);
					
					//process group interactions
					if (telegram != null) {
//TODO reuse message_link in updateGroup for Telegram and Slack
						String message_link = messageLink(chat_username, chat_title, chat_id, message_id);
//TODO: update from->text for profiling and reporting
						telegram.updateInteraction(date,chat_id,chat_username,chat_title,message_id,reply_to_message_id,from_id,reply_to_from_id,getIdsByUsernames(mention_usernames),text,message_link);
					}
					
					if (botname.equals(reply_to_from_username) || (mention_usernames != null && mention_usernames.contains(botname)))
						;//address group message to bot
					else {
						//group message
						if (!from_bot) {
							String emotions = "";
							int sentiment = body.languages.sentiment(text)[2];
							if (Math.abs(sentiment) >= sentiment_threshold)
								emotions = Emotioner.emotion(sentiment);
							int rudeness = body.languages.rudeness(text,null);
							if (rudeness > 0) {
								emotions += Emotioner.flushed;
								report(chat_username, chat_title, chat_id, message_id);
							}
							if (rudeness >= rudeness_threshold)
								delete(chat_id, message_id);
							String s = "\u200A";//Hair Space https://emptycharacter.com/
							if (!AL.empty(emotions))
								output(chat_id, message_id, s+emotions);
						}
						continue;//skip message
					}
				} 

				body.debug("Telegram handling "+date+" from_id "+from_id+" chat_id "+chat_id+" from_username "+from_username+" reply_to_from_username "+reply_to_from_username+" mention_usernames "+mention_usernames+" text "+text);			
				
				//from_id - for private authenticated sessions
				//chat_id - for public anonymous sessions
            	Session session = body.sessioner.getSession(this,key(chat_id,from_id));
            	session.language = body.languages.getName(from_language);

            	if (!from_id.equals(chat_id)) {//group chat
	            	session.context("reply_to_msg_id",message_id);
                	if (!session.authenticated()) {//first user encounter
                		Session privateSession = body.sessioner.getSession(this,key(from_id,from_id));
                		if (!privateSession.authenticated())
                			session = privateSession;//force authentication privately, don't try to authenticate other users in public anonymous sessions
                		else
                			session.clone(privateSession);
                	}
            	}
//            	if (session.authenticated())
//            		session.getStoredPeer().set(Body.telegram_name, from_username);

            	body.conversationer.handle(this, session, text);
			}
			if (telegram != null)//auto-save updated graph, if modified
				telegram.save();
		}
		return offset;
	}
	
	protected void updateContent() {
		
	}
	
	protected static HashSet<String> getMentions(String text) {
		HashSet<String> found = null;
		int len = text.length();
		int fromIndex = 0;
		for (;;) {
			int pos = text.indexOf('@', fromIndex);
			if (pos == -1)
				break;
			int end = pos + 1;
			for (; end < len;) {
				char c = text.charAt(end);//A-z (case-insensitive), 0-9 and underscores
				if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9') || c == '_')
					end++;
				else 
					break;
			}
			if ((end - pos) > 1) {
				if (found == null)
					found = new HashSet<String>();
				
				found.add(text.substring(pos+1, end));
			}
			fromIndex = ++end;
		}
		return found;
	}
	
	public void run() {
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
						body.debug("Telegram deleteWebhook "+token+": "+Str.first(response,200));
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
					period *= 2;
					body.error("Telegrammer error:",e);
				}
			}
			body.debug("Telegrammer stopped.");
		} catch (Exception e) {
			body.error("Telegrammer error:",e);
		}
	}

	//TODO: move to Mediator but make sure about 'facebook_id' clash?
	@Override
	public void login(Session session, Anything peer) {
		String id = ids(session.getKey())[2];
		try {
			Collection peers = body.storager.getByName(Body.telegram_id, id);
			if (AL.empty(peers))//newly registered peer
				peer.setString(Body.telegram_id, id);
			else if (peers.size() > 1)//too many peers
				body.error("Telegrammer merge duplicate id "+id,null);
			else {
				Thing mergee = (Thing)peers.iterator().next();
				mergePeer(((Thing)peer),mergee,key_names);
			}
		} catch (Exception e) {
			body.error("Telegrammer merge error id "+id,e);
		}
		
	}
	
	//TODO class HttpBotter extends net.webstructor.comm.Communicator implements HTTPHandler
	public boolean update(Thing peer, String sessionKey, String subject, String content, String signature) throws IOException {
		String from_id = chatId(sessionKey,peer);
//TODO eliminate this in favor of chatId()
		if (AL.empty(from_id))
			from_id = peer.getString(Body.telegram_id);
		if (AL.empty(from_id)){//backup path - get from id from the session
			//TODO: use peer session here using getTokenSegment and getPeerSession
			String login_token = peer.getString(Peer.login_token);
			if (!AL.empty(login_token)){
				String[] ids = login_token == null ? null : ids(login_token);
				if (ids != null && name.equals(ids[0]) && ids[1].equals(ids[2]))//if there is a private conversation session
					from_id = ids[2];
			}
		}
//TODO eliminate the above in favor of chatId()
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
