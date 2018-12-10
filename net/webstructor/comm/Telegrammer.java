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
package net.webstructor.comm;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body; 
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.core.Thing;
import net.webstructor.peer.Session;

public class Telegrammer extends Communicator {
	
	protected Thing self;
	protected int timeout = 0;
	protected String base_url = "https://api.telegram.org/bot";
	
	public Telegrammer(Body env) {
		super(env);
		self = body.self();
	}

	public void output(Session session, String message) throws IOException {
		String token = self.getString(Body.telegram_token,null);
		String url = base_url+token+"/sendMessage";
		String par = "chat_id="+session.getKey()+"&text="+URLEncoder.encode(message,"UTF-8");
		HTTP.simple(url,par,"POST",timeout);		
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
				JsonObject m = HTTP.getJsonObject(o,"message");
				if (m == null)
					continue;
				//"message_id":11,
	            //"from":{  },
	            //"chat":{  },
	            //"date":1544370858,
	            //"text":"111"
				JsonObject from = HTTP.getJsonObject(m, "from");
				JsonObject chat = HTTP.getJsonObject(m, "chat");
				long unix = m.getInt("date");
				String text = HTTP.getJsonString(m, "text", null);
				if (from == null || chat == null || unix == 0 || AL.empty(text))
					continue;
				Date date = new Date( unix * 1000 );
				String from_id = String.valueOf(HTTP.getJsonLong(from, "id", 0L));
				boolean from_bot = HTTP.getJsonBoolean(from, "is_bot", false);
				String from_username = HTTP.getJsonString(from, "username", null);
				/*
				String from_name = HTTP.getJsonString(from, "first_name", null);
				String from_surname = HTTP.getJsonString(from, "last_name", null);
				String from_language = HTTP.getJsonString(from, "language_code", null);
				String chat_id = String.valueOf(HTTP.getJsonLong(chat, "id", 0L));
				String chat_type = HTTP.getJsonString(chat, "type", "private");//TODO:private by default!?
				*/
				if (AL.empty(from_username) || AL.empty(from_id) || from_bot)
					continue;
				//TODO: autoregister:
				//telegram id = from_id
				//name, surname, username@telegram.org
				body.debug("date "+date+" from_username "+from_username+" text "+text);
				//TODO: use for session id either
				//from_id - for private authenticated sessions
				//chat_id - for public anonymous sessions
				//TODO: dom't try to authenticate other users in public anonymous sessions 
            	net.webstructor.peer.Session session = body.sessioner.getSession(this,from_id);
            	body.conversationer.handle(this, session, text);
			}
		}
		return offset;
	}
	
	public void run(  ) {
		try {
			body.debug("Telegrammer started.");
			long offset = AL.integer(self.getString(Body.telegram_offset,"-1"),-1L);
			while (alive()) {
				//TODO: configurable period
				long period = 5*Period.SECOND;
				try {
					Thread.sleep(period);
					String token = self.getString(Body.telegram_token,null);
					if (token == null)
						continue;
					String url = base_url+token+"/getUpdates";
					//parameter=value&also=another
					String par = "offset="+Long.toString(offset);
					String response = HTTP.simple(url,par,"POST",timeout);
					long new_offset = handle(response);
					if (new_offset != -1)
						self.setString(Body.telegram_offset,Long.toString(offset = ++new_offset));
				} catch (Exception e) {		
					body.error("Telegrammer error:",e);
				}
			}
			body.debug("Telegrammer stopped.");
		} catch (Exception e) {
			body.error("Telegrammer error:",e);
		}
	}
}
