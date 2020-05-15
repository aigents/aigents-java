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
import net.webstructor.core.Anything;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.main.Mainer;
import net.webstructor.util.JSON;
import net.webstructor.util.Str;

//TODO Gigablast
//https://www.gigablast.com/api.html

class GoogleSearch extends Serper {
	
	public GoogleSearch(Environment env){
		super(env);
	}
	
	@Override
	public String name() {
		return "google";
	}
	
	@Override
	public Collection<Thing> search(String type, String text, String lang, int limit) {
		//https://stackoverflow.com/questions/34035422/google-image-search-says-api-no-longer-available
		//"template": "https://www.googleapis.com/customsearch/v1?q={searchTerms}&num={count?}&start={startIndex?}&lr={language?}&safe={safe?}&cx={cx?}&sort={sort?}&filter={filter?}&gl={gl?}&cr={cr?}&googlehost={googleHost?}&c2coff={disableCnTwTranslation?}&hq={hq?}&hl={hl?}&siteSearch={siteSearch?}&siteSearchFilter={siteSearchFilter?}&exactTerms={exactTerms?}&excludeTerms={excludeTerms?}&linkSite={linkSite?}&orTerms={orTerms?}&relatedSite={relatedSite?}&dateRestrict={dateRestrict?}&lowRange={lowRange?}&highRange={highRange?}&searchType={searchType}&fileType={fileType?}&rights={rights?}&imgSize={imgSize?}&imgType={imgType?}&imgColorType={imgColorType?}&imgDominantColor={imgDominantColor?}&alt=json"

		String api_key = env.getSelf() == null ? null : env.getSelf().getString(Body.googlesearch_key);
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
			String response = HTTP.simple(request,null,"GET",0,null,null);
			
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
						t.setString("text",snippet);
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

class SerpAPI extends Serper {
	
	public SerpAPI(Environment env){
		super(env);
	}
	
	@Override
	public String name() {
		return "serpapi";
	}
	
	@Override
	public Collection<Thing> search(String type, String text, String lang, int limit) {
		//https://serpapi.com/search-api
		String api_key = env.getSelf() == null ? null : env.getSelf().getString(Body.serpapi_key);
		if (AL.empty(api_key) || AL.empty(text))
			return null;
		String tbm = type == null ? null : (type = type.toLowerCase()).startsWith("image") ? "isch" : type.startsWith("video") ? "vid" : null;
		String hl = lang == null ? null : (lang = lang.toLowerCase()).startsWith("en") ? "en" : lang.startsWith("ru") ? "ru" : null;
		//String gl = "us";//TODO country
		//String location = "Novosibirsk";//TODO location

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
			String response = HTTP.simple(request,null,"GET",0,"application/json",null);
			if (debug) env.debug("Serpapi crawling response "+Str.first(response,200));
			if (!AL.empty(response)) {
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray results = "isch".equals(tbm) ? JSON.getJsonArray(json,"images_results")
						: "vid".equals(tbm) ? JSON.getJsonArray(json,"video_results")
						: JSON.getJsonArray(json,"organic_results");
				if (results != null) {
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
						} else {
							;//TODO text
							String title = JSON.getJsonString(item, "title", null);
							String link = JSON.getJsonString(item, "link", null);
							String snippet = JSON.getJsonString(item, "snippet", null);
							String thumbnail = JSON.getJsonString(item, "thumbnail", null);
							t.setString("title",title);
							t.setString("text",snippet);
							t.setString("sources",link);
							if (!AL.empty(thumbnail) && AL.isIMG(thumbnail))
								t.setString(AL.image,thumbnail);
						}
						things.add(t);
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

public abstract class Serper {
	protected Environment env;
	protected boolean debug = true;

	public Serper(Environment env){
		this.env = env;
	}

	abstract public String name();
	abstract public Collection<Thing> search(String type, String text, String lang, int limit);

	public static Serper[] getDefaultSerpers(Environment e) {
		return new Serper[]{new GoogleSearch(e),new SerpAPI(e)};
	}

	public static void main(String args[]) {
		if (args.length > 0) {
			final Thing context = new Thing();
			context.setString(Body.serpapi_key, args[0]);
			if (args.length > 1)
				context.setString(Body.googlesearch_key, args[1]);
			Mainer m = new Mainer() {
				@Override
				public Anything getSelf(){
					return context;
				}
			};
			//Serper[] serpers = getDefaultSerpers(m);
			//String[] types = new String[] {"text","image"};
			Serper[] serpers = new Serper[] {new SerpAPI(m)};
			String[] types = new String[] {"text"};
			for (Serper s : serpers) {
				for (String type : types) {
					Collection<Thing> results = s.search(type, "Aigents", null, 1);
					System.out.println(s.getClass().getSimpleName()+ (AL.empty(results) ? " not working" : " found: "));
					if (!AL.empty(results)) {
						for (Thing t : results)
							System.out.println(t);
					}
				}
			}
		}	
	}
}
