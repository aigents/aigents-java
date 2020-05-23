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
package net.webstructor.self;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Seq;
import net.webstructor.al.Statement;
import net.webstructor.al.Time;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.cat.HttpFileContext;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.util.Array;


public class Cacher implements net.webstructor.data.Cacher {//TODO: move to data package
	
	private Body body;
	private Thing self;
	private Storager storager;
	private HttpFileReader reader;
	private HashMap pathTexts; //read and parsed page texts
	private HashMap pathTried; //actually read and updated pages
	private HashMap<String,Date> pathTimes; //timestamps
	
	public Cacher(String name,Body body,Storager storager){
		this.body = body;
		this.self = body.self();
		this.storager = storager;
		reader = new HttpFileReader(body,Body.http_user_agent);
		pathTexts = new HashMap();
		pathTried = new HashMap();
		pathTimes = new HashMap<String,Date>();
		body.register(name, this);
	}
	
	synchronized void clearTried(){
		pathTried.clear();
	}

	@Override
	public void free(){
		//TODO: if cache is made persistent, clear memory only, not the persistent data  
		clear(false,null);
	}
	
	public void clear(Date till) {
		clear(till !=null ? false : true,till);
	}
	
	@Override
	public void clear(boolean everything, Date till) {
		if (everything) {
body.debug("Cacher clearing everything");
			pathTexts.clear();
			pathTimes.clear();
		} else synchronized(this) {
			Object[] paths = pathTexts.keySet().toArray(new String[] {});
			for (Object path : paths) {
				if (till == null || pathTimes.get(path).compareTo(till) < 0) {
body.debug("Cacher clearing "+path);
					pathTexts.remove(path);
					pathTimes.remove(path);
				}
			}
		}
	}
		
	/**
	 * Need to cache single-site pages per multi-thing site spidering so no-need to re-spider pages for every thing. 
	 * @param path
	 * @param links
	 * @return
	 */
	//TODO: if cached and date in cache is less than current date
	private String readCached(String path,long time,ArrayList links,Map images,Map linkPositions, Map titles){
		Date date = pathTimes.get(path);
		if (date != null && date.getTime() >= time){//if not expired
			Object[] cached = (Object[])pathTexts.get(path);
			if (cached == null)
				return null;
			if ((ArrayList)cached[1] != null)
				links.addAll((ArrayList)cached[1]);
			if ((Map)cached[2] != null)
				linkPositions.putAll((Map)cached[2]);
			if ((Map)cached[3] != null)
				images.putAll((Map)cached[3]);
			if ((Map)cached[3] != null)
				titles.putAll((Map)cached[4]);
			return (String)cached[0];
		}
//TODO: find more clever way to check readability instead of such DoS attack - simulation, use context.conn
		HttpFileContext context = new HttpFileContext();
		if (reader.allowedForRobots(path) && reader.canReadDocContext(path,context)) { //still, here we need to try it first in order to get encoding	
			try {
				//TODO: if breaking with blocks
				//do this intelligently (hierarchically) 
				//otherwise some (say chinese) things do not work
				String html = reader.readDocData(path," ",context);
				String text = AL.empty(html) ? null :
						HtmlStripper.convert(html,HtmlStripper.block_breaker,links,images,linkPositions,titles,path).toLowerCase();
				if (text != null) {
					pathTexts.put(path, new Object[]{text,links,linkPositions,images,titles});
					pathTimes.put(path, new Date());
				}
				return text;
			} catch (Exception e) {
				pathTexts.put(path, null);
				pathTimes.put(path, new Date());
				body.error(e.toString(), e);
			}
		}
		return null;
	}

//TODO: synchronization
	/**
	 * Read web document as HTML or PDF (converted to text with optional positioned links and images) 
	 * from cache (if found and if not older than required time) or from the web (otherwise)
	 * then parse it, store in memory and return as plain text along with filled links and images
	 * @param path - path to document on the web
	 * @param links - array of web links to fill
	 * @param images - map of image positions to images
	 * @param linkPositions - map of link positions to links
	 * @param forced - return text even of it is not different from the text read, parsed and stored earlier
	 * @param time - !!!???
	 * @return
	 */
	protected String readIfUpdated(String path,ArrayList links,Map images,Map linkPositions,Map titles,boolean forced,Date realTime){
		//pathTried - means "doable", indicates that file can be processed repeatedly for different things!!!
		//if known as ignored or not ignored, return from cache
		Boolean tried = (Boolean)pathTried.get(path);
		if (tried != null)
			return tried.booleanValue() ? readCached(path,realTime.getTime(),links,images,linkPositions, titles) : null;
		//if ignorance is unknown, figure it owt
		String text = readCached(path,realTime.getTime(),links,images,linkPositions,titles);
		if (AL.empty(text)) {
			pathTried.put(path, new Boolean(true));//ignore as error
		}else {
			boolean todo = false;
			//need to determine presence of updates
			try {
				//AL: what is <path> text, times?
				Statement query = new Statement(new Object[]{
						new All(new Object[]{new Seq(new String[]{AL.is,path})}),
						new String[]{AL.text,AL.times}});
				//if not found or found with newer text
				Collection sites = new Query(body,storager,self).getThings(query,self);
				Thing site = AL.empty(sites) ? null : (Thing)sites.iterator().next();
				Date timeDate = Time.date(realTime);
				String oldtext = site == null ? null : site.getString(AL.text);
				if (AL.empty(oldtext))
					oldtext = body.archiver.get(path);
				
				String diff = oldtext == null ? null : Array.firstdiff(oldtext,text);
				if (oldtext == null || diff != null || forced) {
					if (site == null) {
						site = new Thing().store(storager);
						storager.add(site, AL.is, path);
					} else {
						sites = storager.get(site);//real stored site, not proxy of resultset
						//TODO: if 0 or many
						site = (Thing)sites.iterator().next();
					}
					todo = true;
					site.set(AL.text,text).set(AL.times,timeDate);			
					body.archiver.put(path,text);
				}
				else {
					site.set(AL.times,timeDate);//ensure last check time is set to latest check	
					text = null;
				}
			} catch (Exception e) {
				body.error(e.toString(), e);
			}	
			pathTried.put(path, new Boolean(todo));
		}
		return text;
	}

}
