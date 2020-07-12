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

import java.io.IOException;
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

public class GoogleSearch extends Serper {

	public GoogleSearch(Environment env){
		super(env);
	}
	
	@Override
	public String name() {
		return "google";
	}

	@Override
	public String api_key() {
		return env.getSelf() == null ? null : env.getSelf().getString(Body.googlesearch_key);
	}
	
	@Override
	public Collection<Thing> search(String type, String text, String lang, int limit) {
		//https://stackoverflow.com/questions/34035422/google-image-search-says-api-no-longer-available
		//"template": "https://www.googleapis.com/customsearch/v1?q={searchTerms}&num={count?}&start={startIndex?}&lr={language?}&safe={safe?}&cx={cx?}&sort={sort?}&filter={filter?}&gl={gl?}&cr={cr?}&googlehost={googleHost?}&c2coff={disableCnTwTranslation?}&hq={hq?}&hl={hl?}&siteSearch={siteSearch?}&siteSearchFilter={siteSearchFilter?}&exactTerms={exactTerms?}&excludeTerms={excludeTerms?}&linkSite={linkSite?}&orTerms={orTerms?}&relatedSite={relatedSite?}&dateRestrict={dateRestrict?}&lowRange={lowRange?}&highRange={highRange?}&searchType={searchType}&fileType={fileType?}&rights={rights?}&imgSize={imgSize?}&imgType={imgType?}&imgColorType={imgColorType?}&imgDominantColor={imgDominantColor?}&alt=json"

		String api_key = api_key();
		//String cx = env.getSelf() == null ? null : env.getSelf().getString(Body.googlesearch_code);//TODO: not needed?
		if (AL.empty(api_key) || AL.empty(text))
			return null;
		String searchType = type == null ? null : (type = type.toLowerCase()).startsWith("image") ? "image" : type.startsWith("video") ? "video" : null;
		//String hl = lang == null ? null : (lang = lang.toLowerCase()).startsWith("en") ? "en" : lang.startsWith("ru") ? "ru" : null;
		try {
			String request = "https://www.googleapis.com/customsearch/v1?key="+api_key+"&q="+URLEncoder.encode(text,"UTF-8");
			if (limit > 0) {
				if (limit > 10)
					limit = 10;
//TODO: pagination
				request += "&num="+String.valueOf(limit);
			}
			//if (!AL.empty(cx))
			//	request += "&cx="+cx;
			if (!AL.empty(searchType))
				request += "&searchType="+String.valueOf(searchType);
					
			if (debug) env.debug("Googlesearch crawling request "+request);
			String response = checkCached(request);
			if (response == null) {
				response = HTTP.simple(request,null,"GET",0,null,null);
				putCached(request,response);
			}
			
			if (debug) env.debug("Googlesearch crawling response "+Str.first(response,200));
			if (!AL.empty(response)) {
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray items = JSON.getJsonArray(json,"items");
				if (items != null) {
					ArrayList<Thing> things = new ArrayList<Thing>();
					for (int i = 0; i < items.size(); i++) {
						JsonObject item = items.getJsonObject(i);
						Thing t = new Thing();
						String title = JSON.getJsonString(item, "title", null);
						String snippet = JSON.getJsonString(item, "snippet", null);
						String link = null;
						String image = null;
//TODO video
						if ("image".equals(type)) {
							image = JSON.getJsonString(item, "link", null);
							JsonObject imageobj = JSON.getJsonObject(item, "image");
							if (imageobj != null)
								link = JSON.getJsonString(imageobj,"contextLink");
						}
						else {
							link = JSON.getJsonString(item, "link", null);
							JsonObject pagemap = JSON.getJsonObject(item, "pagemap");
							if (pagemap != null) {
								JsonArray csr_image = JSON.getJsonArray(pagemap,"cse_image");
								if (csr_image != null && csr_image.size() > 0)
									image = JSON.getJsonString(csr_image.getJsonObject(0),"src");
							}
						}
						t.setString("title",title);
						t.setString("text",!AL.empty(snippet) ? snippet : title);
						t.setString("sources",link);
						if (!AL.empty(image) && AL.isURL(image))//isIMG may not work
							t.setString(AL.image,image);
						things.add(t);
					}
					return things;
				}
			}
			
		} catch (IOException e) {
			if (debug) env.error("Googlesearch crawling "+text,e);
		}
		
		return null;
	}
}

