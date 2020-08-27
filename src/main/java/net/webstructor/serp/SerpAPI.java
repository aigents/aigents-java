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
package net.webstructor.serp;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.util.JSON;
import net.webstructor.util.Str;

public class SerpAPI extends Serper {
	
	public SerpAPI(Environment env){
		super(env);
	}
	
	@Override
	public String name() {
		return "serpapi";
	}
	
	@Override
	public String api_key() {
		return env.getSelf() == null ? null : env.getSelf().getString(Body.serpapi_key);
	}
	
	@Override
	public Collection<Thing> search(String type, String text, String lang, int limit) {
		//https://serpapi.com/search-api
		String api_key = api_key();
		if (AL.empty(api_key) || AL.empty(text))
			return null;
		String tbm = type == null ? null : (type = type.toLowerCase()).startsWith("image") ? "isch" : type.startsWith("video") ? "vid" : null;
		String hl = lang == null ? null : (lang = lang.toLowerCase()).startsWith("en") ? "en" : lang.startsWith("ru") ? "ru" : null;
		//String gl = "us";//TODO country
		//String location = "Novosibirsk";//TODO location

//TODO: loop over multiple results		
		try {
			StringBuilder url = new StringBuilder("https://serpapi.com/search");
			url.append("?api_key=").append(api_key);
			url.append("&q=").append(URLEncoder.encode(text,"UTF-8"));
			url.append("&google_domain=").append("google.com");//https://serpapi.com/google-domains
			if (limit > 0)
				url.append("&num=").append(String.valueOf(limit));
			if (!AL.empty(tbm))
				url.append("&tbm=").append(tbm);
			if (!AL.empty(hl))
				url.append("&hl=").append(hl);
			url.append("&source").append("java");
			url.append("&output").append("json");
			url.append("&engine").append("google");
			String request = url.toString();
			if (debug) env.debug("Serpapi crawling request "+request);
			String response = checkCached(request);
			if (response == null) {
				response = HTTP.simple(request,null,"GET",0,"application/json",null);
			}
			if (debug) env.debug("Serpapi crawling response "+Str.first(response,200));
			if (!AL.empty(response)) {
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray results = "isch".equals(tbm) ? JSON.getJsonArray(json,"images_results")
						: "vid".equals(tbm) ? JSON.getJsonArray(json,"video_results")
						: JSON.getJsonArray(json,"organic_results");
				if (results != null) {
					putCached(request,response);//if parsed results successfully!
					ArrayList<Thing> things = new ArrayList<Thing>();
					for (int i = 0; i < results.size(); i++) {
						JsonObject item = results.getJsonObject(i);
						Thing t = new Thing();
						if ("vid".equals(tbm)) {
							String title = JSON.getJsonString(item, "title", null);
							String link = JSON.getJsonString(item, "link", null);
							String snippet = JSON.getJsonString(item, "snippet", null);
							String thumbnail = JSON.getJsonString(item, "thumbnail", null);
							t.setString("title",title);
							t.setString("text",snippet);
							t.setString("sources",link);
							if (!AL.empty(thumbnail) && AL.isIMG(thumbnail))
								t.setString(AL.image,thumbnail);
							things.add(t);
						} else if ("isch".equals(tbm)) {// image
							String title = JSON.getJsonString(item, "title", null);
							String link = JSON.getJsonString(item, "link", null);
							String snippet = JSON.getJsonString(item, "snippet", null);
							String thumbnail = JSON.getJsonString(item, "original", null);
							if (!AL.empty(snippet)) {
								t.setString("title",title);
								t.setString("text",snippet);
							} else {
								//TODO: surrogate title?
								t.setString("text",title);
							}
							t.setString("sources",link);
							//if (!AL.empty(thumbnail) && AL.isIMG(thumbnail))//this does not pass URL-s like https://i0.wp.com/blocksplain.com/wp-content/uploads/2018/02/Screen-Shot-2018-02-21-at-5.19.43-PM.png?resize=1280%2C816&ssl=1
							if (!AL.empty(thumbnail) && AL.isURL(thumbnail))
								t.setString(AL.image,thumbnail);
							things.add(t);
						} else {// text
							String title = JSON.getJsonString(item, "title", null);
							String snippet = JSON.getJsonString(item, "snippet", null);
							if (AL.empty(snippet))
								snippet = title;
							if (AL.empty(snippet))
								continue;
							String link = JSON.getJsonString(item, "link", null);
							String thumbnail = JSON.getJsonString(item, "thumbnail", null);
							t.setString("title",title);
							t.setString("text",snippet);
							t.setString("sources",link);
							if (!AL.empty(thumbnail) && AL.isIMG(thumbnail))
								t.setString(AL.image,thumbnail);
							things.add(t);
						}
					}
					return things;
				}
			}
			
		} catch (Exception e) {
			if (debug) env.error("Serpapi crawling "+text,e);
		}
		
		return null;
	}
}
