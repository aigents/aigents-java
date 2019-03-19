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
package net.webstructor.comm;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URLEncoder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.peer.Session;

public class Slacker extends Communicator {

	public Slacker(Body body) {
		super(body);
	}
	
	/**
	 * @param args - array with url, header, request
	 * @return true if handled
	 * @throws IOException
	 */
	public boolean handleHTTP(HTTPeer parent, String url, String header, String request) throws IOException {
		if (url.contains("slack")){
			if (!AL.empty(request)){
		    	String response = "";
		    	
				String code = null;
				String state = null;
				String token = null;
				String challenge = null;
				String type = null;
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
				        text = HTTP.getJsonString(event, "text");
				        user = HTTP.getJsonString(event, "user");
				        channel = HTTP.getJsonString(event, "channel");
				        channel_type = HTTP.getJsonString(event, "channel_type");
						
					} //else {//verification
					//https://api.slack.com/events-api#events_api_request_urls
					token = HTTP.getJsonString(json, "token");
					challenge = HTTP.getJsonString(json, "challenge");
					type = HTTP.getJsonString(json, "type");
					//}
				}

				//TODO: remove
				String debug = "code="+code+" state="+state+" type="+type+" user="+user+" channel="+channel+" channel_type="+channel_type;
				System.out.println(debug);
				body.debug(debug);
				
				
				//TODO: https://api.slack.com/bot-users
				
				//verification
				if (!AL.empty(challenge) && "url_verification".equals(type))
					response = challenge;
				
				//TODO: actual response?
		    	OutputStream os = parent.socket.getOutputStream();
		    	BufferedOutputStream bos = new BufferedOutputStream(os);
		    	
		    	byte[] bytes = response.getBytes("UTF-8");
	        	String hdrStr = "HTTP/1.0 200 Ok\nContent-Type: text/plain\nContent-Length: "+bytes.length+"\n\n";
	        	bos.write(hdrStr.getBytes("UTF-8"));
		    	bos.write(bytes);
			    bos.flush();
			    os.flush();
				
				return true;
			}
		}
		return false;
	}
	
	public void output(Session session, String message) throws IOException {
		// TODO Auto-generated method stub
		
	}

}

