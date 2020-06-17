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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Reader;
import net.webstructor.al.Time;
import net.webstructor.comm.Crawler;
import net.webstructor.core.Anything;
import net.webstructor.core.Environment;
import net.webstructor.core.Property;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.main.Mainer;
import net.webstructor.peer.Profiler;
import net.webstructor.self.Matcher;
import net.webstructor.self.Siter;
import net.webstructor.util.MapMap;

//TODO Gigablast
//https://www.gigablast.com/api.html

public abstract class Serper implements Crawler {

	public static final int LIMIT = 100;//TODO: eliminate limit or have it configured at Siter/Spider level
	
	protected Environment env;
	protected boolean debug = true;

	public Serper(Environment env){
		this.env = env;
	}

	abstract public String name();
	abstract String api_key();
	abstract public Collection<Thing> search(String type, String text, String lang, int limit);

	@Override
	public int crawl(Siter siter) {
		String query = siter.getRootPath();
		if (AL.empty(api_key()) || AL.empty(query) || AL.isURL(query))
			return -1;
		Collection topics = ((Body)env).storager.getNamed(query);//precise topics overriding siter.getTopics() 
		query = Property.toWordList(Reader.patterns(null,null,query));//transform query pattern to query words
		if (AL.empty(query) || AL.empty(topics))
			return -1;
//TODO actual limit
		int limit = Math.min(siter.getLimit(), LIMIT);
		Collection<Thing> things = search("text", query, null, limit);
		if (things == null)
			return -1;
//TODO iterate and match
		Date time = siter.getTime();
		MapMap collector = siter.getPathBasedCollector();
		Matcher matcher = ((Body)env).getMatcher();
		int matches = 0;
		for (Thing t : things) {
			String title = t.getString(AL.title);
			String text = t.getString(AL.text);
			if (!AL.empty(title))
				text = title.endsWith(".") ? title + " " + text : title + " . " + text;
			text = text.toLowerCase();
			String image = t.getString(AL.image);
			String sources = t.getString(AL.sources);
			if (!AL.empty(sources))
				matches += matcher.matchThingsText(topics,text,Time.date(time),sources,image,collector,null,null);
		}
		return matches;
	}

	@Override
	public boolean scalp(Siter siter, String path, ArrayList links, Collection topics) {
		return false;//can't scalp
	}

	@Override
	public SocialFeeder getFeeder(String id, String token, String key, Date since, Date until, String[] areas) throws IOException {
		return null;//can't feed
	}

	@Override
	public Profiler getProfiler(Thing peer) {
		return null;//can't profile
	}

	public String checkCached(String path){
		return env instanceof Body ? ((Body)env).filecacher.checkCachedRaw(path) : null;
	}
	
	public void putCached(String path,String data){
		if (env instanceof Body)
			((Body)env).filecacher.putCachedRaw(path, data);
	}
	
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
