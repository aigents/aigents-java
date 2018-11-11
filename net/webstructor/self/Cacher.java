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
import net.webstructor.cat.HtmlStripper;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.util.Array;

class Cacher {
	
	private Body body;
	private Thing self;
	private Storager storager;
	private boolean forced;//forcing peer: if not null - re-read unconditionally, if null - read only if content is changed
	private Date time;
	private HttpFileReader reader;
	private HashMap pathTexts; //read and parsed page texts
	private HashMap pathTodos; //actually read and updated pages
	
	Cacher(Body body,Storager storager,boolean forced,Date time){
		this.body = body;
		this.self = body.self();
		this.storager = storager;
		this.time = time;
		this.forced = forced;
		reader = new HttpFileReader(body,Body.http_user_agent);
		pathTexts = new HashMap();
		pathTodos = new HashMap();
	}
	
	void clear(){
		pathTexts.clear();
		pathTodos.clear();
	}
	
	/**
	 * Need to cache single-site pages per multi-thing site spidering so no-need to re-spider pages for every thing. 
	 * @param path
	 * @param links
	 * @return
	 */
	//TODO: if cached and date in cache is less than current date
	private String readCached(String path,ArrayList links,Map images,Map linkPositions){
		Object[] cached = (Object[])pathTexts.get(path);
		if (cached != null){
			ArrayList cachedLinks = (ArrayList)cached[1];
			links.addAll(cachedLinks);
			return (String)cached[0];
		}
		//TODO: find more clever way to check readability instead of such DoS attack - simulation
		if (reader.allowedForRobots(path) && reader.canReadDoc(path)) { //still, here we need to try it first in order to get encoding	
			try {
				//TODO: if breaking with blocks
				//do this intelligently (hierarchically) 
				//otherwise some (say chinese) things do not work
				String html = reader.readDocData(path," ");
				String text = AL.empty(html) ? null :
						HtmlStripper.convert(html,HtmlStripper.block_breaker,links,images,linkPositions,path).toLowerCase();
				if (text != null)
					pathTexts.put(path, new Object[]{text,links});
				return text;
			} catch (Exception e) {
				body.error(e.toString(), e);
			}
		}
		return null;
	}

	protected String readIfUpdated(String path,ArrayList links,Map images,Map linkPositions){
		//if known as ignored or not ignored, return from cache
		Boolean todos = (Boolean)pathTodos.get(path);
		if (todos != null)
			return todos.booleanValue() ? readCached(path,links,images,linkPositions) : null;
		//if ignorance is unknown, figure it owt
		String text = readCached(path,links,images,linkPositions);
		if (AL.empty(text))
			pathTodos.put(path, new Boolean(true));//ignore as error
		else {
			boolean todo = false;
			//need to determine presence of updates
			try {
				//AL: what is <path> text, times?
				Statement query = new Statement(new Object[]{
						new All(new Object[]{new Seq(new String[]{AL.is,path})}),
						new String[]{AL.text,AL.times}});
				//if not found or found with newer text
				Collection sites = new Query(storager,self).getThings(query,self);
				Thing site = AL.empty(sites) ? null : (Thing)sites.iterator().next();
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
					site.set(AL.text,text).set(AL.times,time);			
					body.archiver.put(path,text);
				}
				else {
					site.set(AL.times,time);//ensure last check time is set to latest check	
					text = null;
				}
			} catch (Exception e) {
				body.error(e.toString(), e);
			}	
			pathTodos.put(path, new Boolean(todo));
		}
		return text;
	}
		
}
