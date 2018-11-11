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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.Emailer;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Property;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;
import net.webstructor.util.MapMap;

//TODO: move out
class Imager {
	HashMap pathMaps = new HashMap();
	TreeMap getMap(String path){
		TreeMap map = (TreeMap)pathMaps.get(path);
		if (map == null)
			pathMaps.put(path,map = new TreeMap());
		return map;
	}
	
	private Entry getClosest(TreeMap map, Integer pos){
		if (map == null)
			return null;
		//Strategy 1:
		//Entry e = map.floorEntry(pos);
		//return e != null ? e : map.ceilingEntry(pos);
		//Strategy 2:
		Entry e1 = map.floorEntry(pos);
		Entry e2 = map.ceilingEntry(pos);
		if (e1 == null)
			return e2;
		if (e2 == null)
			return e1;
		Integer v1 = (Integer)e1.getKey();
		Integer v2 = (Integer)e2.getKey();
		int d1 = pos.intValue() - v1.intValue();
		int d2 = v2.intValue() - pos.intValue();
		return d1 <= d2 ? e1 : e2;
	}
	
	private Entry getClosest(TreeMap map, Integer pos, int range){
		if (range <= 0)
			range = Integer.MAX_VALUE;
		if (map == null)
			return null;
		Entry e1 = map.floorEntry(pos);
		Entry e2 = map.ceilingEntry(pos);
		if (e1 == null && e2 == null)
			return null;
		if (e2 == null)
			return Math.abs(pos.intValue() - ((Integer)e1.getKey()).intValue()) <= range? e1 : null;
		if (e1 == null)
			return Math.abs(pos.intValue() - ((Integer)e2.getKey()).intValue()) <= range? e2 : null;
		int d1 = Math.abs(pos.intValue() - ((Integer)e1.getKey()).intValue());
		int d2 = Math.abs(pos.intValue() - ((Integer)e2.getKey()).intValue());
		return Math.min(d1, d2) > range ? null : d1 <= d2 ? e1 : e2;	
	}
		
	/**
	 * Returns image source closest to position
	 * @param pos position of the image
	 * @return
	 */
	String getImage(String path, Integer pos){
		TreeMap map = (TreeMap)pathMaps.get(path);
		Entry e = getClosest(map,pos);
		return e == null ? null : (String)e.getValue();
	}

	/**
	 * Returns image source closest to position, with cleanup of unavailable entries along the way
	 * @param pos position of the image
	 * @return
	 */
	String getAvailableImage(String path, Integer pos){
		TreeMap map = (TreeMap)pathMaps.get(path);
		if (map == null)
			return null;
		while (map.size() > 0){
			Entry e = getClosest(map,pos);
			String value = (String)e.getValue();
			if (!AL.empty(value) && HTTP.accessible(value))
				return value;
			map.remove(e.getKey());
		}
		return null;
	}
	
	/**
	 * Returns image source closest to position ONLY if within specified range, with NO cleanup of unavailable entries
	 * @param pos position of the image
	 * @return
	 */
	String getAvailableInRange(String path, Integer pos, int range){
		TreeMap map = (TreeMap)pathMaps.get(path);
		if (map == null)
			return null;
		if (map.size() > 0){
			Entry e = getClosest(map,pos,range);
			if (e != null){
				String value = (String)e.getValue();
				//TODO: check accessibility unless banned or check robots.txt when trying?
				//if (!AL.empty(value) && HTTP.accessible(value))
				if (!AL.empty(value))
					return value;
			}
		}
		return null;
	}
	
}


public class Siter {

	public static final int DEFAULT_RANGE = 3;//TODO: make configurable
	
	Body body;
	Thing self;
	Storager storager;
	private String thingname;
	String rootPath;
	Date time; // = Time.day(Time.today);
	boolean forced;//forcing peer: if not null - re-read unconditionally, if null - read only if content is changed
	boolean strict;//true - follow same-domain links only, false - follow all links
	Collection focusThings;
	Collection allThings;
	MapMap thingPaths; //thing->path->instance
	MapMap thingTexts; //thing->text->instance	
	Imager imager;
	Imager linker;//using Imager to keep positions of links
	Cacher cacher;
	long tillTime;
	long newsLimit;
	int range;
	
	public Siter(Body body,Storager storager,String thingname,String path,Date time,boolean forced,long tillTime,int range,int limit) {
		this.body = body;
		this.self = body.self();
		this.storager = storager;
		this.thingname = thingname;
		this.rootPath = path;
		this.time = time;
		this.forced = forced;
		this.strict = true;
		thingPaths = new MapMap();
		thingTexts = new MapMap();
		imager = new Imager();
		linker = new Imager();
		this.cacher = new Cacher(body,storager,forced,time);
		this.tillTime = tillTime;
		this.range = range > 0 ? range : DEFAULT_RANGE;
		this.newsLimit = limit;
		
		//first, try to get things specified by thing name name (if explicitly provided)
		allThings = storager.getNamed(thingname);
		if (AL.empty(allThings)) {
			//next, try to find all things people are aware of:
			//build list of things that are known to and trusted by users of given path
			allThings = new HashSet();
			//TODO: make sites readable not being listed?
			java.util.Set sites = storager.get(AL.sites,rootPath);
			java.util.Set peers = !AL.empty(sites)? new HashSet(sites) : null;
			if (!forced){//get things from peers only trusting this site
				Collection peerSiteTrusts = storager.get(AL.trusts,rootPath);
				if (AL.empty(peerSiteTrusts))
					peers.clear();
				else
					peers.retainAll(peerSiteTrusts);
			}
			java.util.Set peersSite = storager.get(AL.trusts,rootPath);
			if (!AL.empty(peers)){
				Date since = Time.today(-body.retentionDays());
				for (Iterator it = peers.iterator(); it.hasNext();){
					Thing peer = (Thing)it.next();
					int news_limit = StringUtil.toIntOrDefault(peer.getString(Peer.news_limit),10,3);
					this.newsLimit = Math.max(this.newsLimit, news_limit);
					Date activityTime = (Date)peer.get(Peer.activity_time);
					Collection knows = peer.getThings(AL.knows);
					if (activityTime != null && activityTime.compareTo(since) >= 0 && !AL.empty(knows)){
						Collection peerThings = new HashSet(knows);
						if (!forced){
							Collection peerTrusts = peer.getThings(AL.trusts);
							if (AL.empty(peerTrusts))
								peerThings.clear();
							else
								peerThings.retainAll(peerTrusts);
						}
						for (Iterator jt = peerThings.iterator(); jt.hasNext();){
							Thing thing = (Thing)jt.next();
//TODO: optimize this							
							Set peersThing = storager.get(AL.trusts,thing);
							if (!AL.empty(peersThing))//if there is at least one peer trusting the thing
								if (forced || Array.intersect(peersThing,peersSite)){
									allThings.add(thing);
								}
						}
					}
				}
			}
		}
	}

	boolean expired(){
		boolean expired = tillTime > 0 && System.currentTimeMillis() > tillTime;
		if (expired)
			body.debug("Spidering site time out:"+rootPath);
		return expired;
	}
	
	//TODO: hopLimit = 0 does not work in PathFinder now!?
	private int newSpider(int hopLimit) {
		cacher.clear();
		int hits = 0;
		for (Iterator it = allThings.iterator(); it.hasNext();){
			Thing t = (Thing)it.next();
			if (expired())
				break;
			Collection goals = new ArrayList(1);
			goals.add(t);
			String name = t.getName();
			body.reply("Spidering site thing begin "+name+" in "+rootPath+".");
			boolean found = new PathTracker(this,goals,hopLimit).run(rootPath);
			body.reply("Spidering site thing end "+(found ? "found" : "missed")+" "+name+" in "+rootPath+".");
			if (found)
				hits++;
		}
		return hits;
	}
	
	//TODO: make configurable hopLimit
	public boolean read() {
		boolean ok = false;
		body.reply("Spidering site root begin "+rootPath+".");
		if (!AL.empty(thingname)){
			Collection goals = storager.getNamed(thingname);
			ok = new PathTracker(this,goals,range).run(rootPath);
		} else {
			//ok = newSpider(HOP_LIMIT) > 0;
			//TODO: 0 is needed by agent_basic.php (10,100 is not working, 0 is working - why!?)
			ok = newSpider(0) > 0;
		}
		if (ok)
			ok = update() > 0;
		body.reply("Spidering site root end "+(ok ? "found" : "missed")+" "+rootPath+".");
		return ok;
	}
	
	private int update(){
		Object[] things = thingPaths.getKeyObjects();
		if (AL.empty(things))
			return 0;
		int hits = 0;
		Date now = Time.date(time);
		//update whatever is novel compared to snapshot in STM or LTM
		for (int p = 0; p < things.length; p++){
			Thing thing = (Thing)things[p];
			Object[] paths = thingPaths.getSubKeyObjects(thing);
			ArrayList collector = new ArrayList();
			for (int i = 0; i < paths.length; i++){
				String path = (String)paths[i];
				Collection instances = thingPaths.getObjects(thing, path);
				for (Iterator it = instances.iterator(); it.hasNext();){
					Thing instance = (Thing)it.next();
					//Collection ises = instance.getThings(AL.is);
					String text = instance.getString(AL.text);
					//if (!AL.empty(ises) && !AL.empty(text)){
						//String thingName = ((Thing)ises.iterator().next()).getName();
					if (!AL.empty(text)){
						String thingName = thing.getName();
					
//TODO: use "forcer" consistently						
						//TODO:make sure if GLOBAL novelty is required, indeed... 
						//if ((existing = existing(thing,instance,path,false,null) != null)//old version
						//if ((existing = existing(thing,instance,path,false,nl_text) != null)//new path-full identity
						if (existing(thing,instance,null,false,text) != null)//new path-less identity
							continue;
						if (!forced && body.archiver.exists(thingName,text))//check LTM
							continue;
					
						hits++;
						instance.store(storager);
						if (!AL.empty(path))
							try {
								storager.add(instance, AL.sources, path);
							} catch (Exception e) {
								body.error(e.toString(), e);
							}
						collector.add(instance);
					}
				}
				//update(storager,thing,instances,rootPath);
				//TODO: real path here!!??
				//update(storager,thing,instances,path);
			}
			update(storager,thing,collector,rootPath);
		}		
		//memorize everything known and novel in STM AND LTM snapshots
//TODO: optimization to avoid doing extra stuff below!!!		
		for (int j = 0; j < things.length; j++){
			Thing thing = (Thing)things[j];
			Object[] paths = thingPaths.getSubKeyObjects(thing);
			for (int i = 0; i < paths.length; i++){
				String path = (String)paths[i];
				Collection instances = thingPaths.getObjects(thing, path);
				for (Iterator it = instances.iterator(); it.hasNext();){
					Thing instance = (Thing)it.next();
					//Collection ises = instance.getThings(AL.is);
					String text = instance.getString(AL.text);
					//if (!AL.empty(ises) && !AL.empty(text)){
						//String thingName = ((Thing)ises.iterator().next()).getName();
					if (!AL.empty(text)){
						String thingName = thing.getName();
						Thing existing;
//TODO: use "forcer" consistently						
						//if ((existing = existing(thing,instance,null,false,text)) != null)//new path-less identity
						if (!forced && (existing = existing(thing,instance,null,false,text)) != null)//new path-less identity
							existing.set(AL.times,now);//update temporal snapshot in-memory
						body.archiver.update(thingName,text,now);//update temporal snapshot in LTM
					}
				}
			}
		}
		thingPaths.clear();//help gc
		thingTexts.clear();
		cacher.clear();
		return hits;
	}
	
	boolean linkMatch(String text,Seq patseq) {
		Iter iter = new Iter(Parser.parse(text));
		if (Reader.read(iter, patseq, null))
			return true;
		return false;
	}
	
	/*
	private boolean linkMatch(String text,Collection things) {
		if (!AL.empty(things)) {
			for (Iterator it = things.iterator();it.hasNext();)
				if (linkMatch(storager,text,(Thing)it.next()))
					return true;
		}
		return false;
	}

	private boolean linkMatch(Storager storager,String text,Thing thing) {
		Iter iter = new Iter(Parser.parse(text));
		//first, try to get patterns for the thing
		Collection patterns = (Collection)thing.get(AL.patterns);
		//next, if none, create the pattern for the thing name manually
		if (AL.empty(patterns)) {
			//auto-pattern from thing name split apart
			if (linkMatch(storager,thing.getName(),iter))
				return true;
		} 
		else { //if (!AL.empty(patterns)) {
			for (Iterator it = patterns.iterator(); it.hasNext();)				
				if (linkMatch(storager,((Thing)it.next()).getName(),iter))
					return true;
		}
		return false;	
	}

	private boolean linkMatch(Storager storager,String patstr, Iter iter) {
		iter.pos(0);//reset 
		Thing instance = new Thing();
		Seq patseq = Reader.pattern(storager,instance, patstr);
		//StringBuilder summary = new StringBuilder();
		if (Reader.read(iter, patseq, null))
			return true;
		return false;
	}
	*/
	
	boolean readPage(String path,Date time,ArrayList links,Collection things) {
		Thread.yield();
		boolean result = false;
		boolean skipped = false;
		boolean failed = false;
		String text = null;
		
		body.reply("Spidering site page begin "+path+".");
		if (!AL.isURL(path)) // if not http url, parse the entire text
			result = match(storager,path,time,null,things);
		else
		//TODO: distinguish skipped || failed in readIfUpdated ?
		if (!AL.empty(text = cacher.readIfUpdated(path,links,imager.getMap(path),linker.getMap(path)))) {
			result = match(storager,text,time,path,things);
			if (!AL.empty(links) && body.sitecacher != null){
				//TODO: actual time of the page
				Graph g = body.sitecacher.getGraph(Time.day(time));//daily graph
				for (Iterator it = links.iterator(); it.hasNext();){
					String[] link = (String[])it.next();
					String linkUrl = HttpFileReader.alignURL(path,link[0],false);//relaxed, let any "foreign" links
					//String linkText = link[1];//TODO: use to evaluate source name as the best-weighted link!?
					if (!AL.empty(linkUrl) && g.getValue(path, linkUrl, "links") == null){
						g.addValue(path, linkUrl, "links", 1);
						g.addValue(linkUrl, path, "linked", 1);
					}
				}
			}
			//TODO: add source name as page title by default?
		} else {
			skipped = true;//if not read
			failed = true;
		}
		if (skipped || failed)
			body.reply("Spidering site page end "+(failed? "failed": "skipped")+" "+path+".");
		else
			body.reply("Spidering site page end "+(result? "found": "missed")+" "+path+".");
		return result;
	}

	//get all things for the thing name
	private boolean match(Storager storager,String text,Date time,String path,Collection things) {
		int matches = 0;
		if (text != null) {
			if (!AL.empty(things)) {
				for (Iterator it = things.iterator();it.hasNext();)
					matches += match(storager,text,(Thing)it.next(),time,path);
			}
		}
		return matches > 0;
	}
	
	//match all Patterns of one Thing for one Site and send updates to subscribed Peers
	private int match(Storager storager,String text,Thing thing,Date time,String path) {
		//TODO: re-use iter building it one step above
		ArrayList positions = new ArrayList();
		Iter iter = new Iter(Parser.parse(text,positions));//build with original text positions preserved for image matching
		int matches = 0;
		//first, try to get patterns for the thing
		Collection patterns = (Collection)thing.get(AL.patterns);
		//next, if none, create the pattern for the thing name manually
		if (AL.empty(patterns))
			//auto-pattern from thing name split apart
			matches += match(storager,thing.getName(),iter,thing,time,path,positions);
		if (!AL.empty(patterns)) {
			for (Iterator it = patterns.iterator(); it.hasNext();){				
				matches += match(storager,((Thing)it.next()).getName(),iter,thing,time,path,positions); 
			}
		}
		//TODO: cleanup
		if (matches > 0){
			/*
			Collection collected = thingPaths.getObjects(thing, path == null ? "" : path);
			for (Iterator it = collected.iterator(); it.hasNext();){
				Thing instance = (Thing)it.next();
				instance.store(storager);
				try {
					if (path != null)
						storager.add(instance, AL.sources, path);
				} catch (Exception e) {
					body.error(e.toString(), e);
				}
			}
			*/
			/*
			Object[] texts = thingTexts.getSubKeyObjects(thing);
			Collection collected2 = new ArrayList();
			for (int i = 0; i < texts.length; i++){
				Thing instance = (Thing)thingTexts.getObject(thing, texts[i], false);
				instance.store(storager);
				try {
					if (path != null)
						storager.add(instance, AL.sources, path);
				} catch (Exception e) {
					body.error(e.toString(), e);
				}
				collected2.add(instance);
			}
			
			update(storager,thing,collected2,rootPath);
			
			thingPaths.clear();
			thingTexts.clear();
			*/
		}
		return matches;
	}
	
	//TODO: move out?
	boolean has(Thing thing, String property, String name) {
		Collection named = storager.getNamed(name);
		Object props = thing.get(property);
		if (props instanceof Collection && ((Collection)props).containsAll(named))
			return true;
		return false;
	}
	
	//TODO: move out to time-specific framework!?
	Collection latest(Thing is, String path) {
		HashSet found = new HashSet();
		long latest = 0;
		Collection instances = storager.get(AL.is,is);
		if (!AL.empty(instances)) {
			//TODO: do we really need the clone here to prevent ConcurrentModificationException? 
			for (Iterator it = new ArrayList(instances).iterator(); it.hasNext();) {
				Thing instance = (Thing)it.next();
//String debug_text = Writer.toPrefixedString(null, null, instance);				
				if (path != null && !has(instance,AL.sources,path))
					continue;
				Date date = Time.day(instance.get(AL.times));
				long time = date == null ? 0 : date.getTime();
				if (time >= latest) {
					if (time > latest)
						found.clear();
					found.add(instance);
					latest = time;
				}
			}
		}
		return found;
	}

	Thing existing(Thing thing, Thing instance, String path, boolean debug, String text) {
		Collection coll = latest(thing,path);
		if (debug) {
			body.debug("thing   :"+Writer.toString(thing));
			body.debug("instance:"+Writer.toString(instance));
			body.debug("path    :"+path);
			body.debug("coll    :");
		}
		if (!AL.empty(coll))
			for (Iterator it = coll.iterator(); it.hasNext();) {
				Thing latest = (Thing)it.next();
				if (debug)
					body.debug("latest  :"+latest);
				String latestText = latest.getString(AL.text);
				if (!AL.empty(text) && !AL.empty(latestText)){//TODO: validate by text!!!??? 
					if (text.equals(latestText)){
						if (debug)
							body.debug("novel   :false");
						return latest;
					}
				} else
				if (storager.match(latest, instance)) {
					if (debug)
						body.debug("novel   :false");
					return latest;
				}
			}
		if (debug)
			body.debug("novel   :true");
		return null;
	}
	
	private Seq relaxPattern(Thing instance, String context, Seq patseq, String about) {
		if (AL.empty(patseq))
			return patseq;
		Object pat[] = new Object[patseq.size() + (context == null ? 0 : 1) + (about == null ? 0 : 1)];
		int i = 0;
		if (context != null)
			pat[i++] = new Property(storager,instance,context);
		for (int j = 0; j < patseq.size(); j++)
			pat[i++] = patseq.get(j);
		if (about != null)
			pat[i++] = new Property(storager,instance,about);
		return new Seq(pat);
	}
	
	//TODO: make smarter patterns like "[?prefix patseq ?postfix]" and make them supported by matcher!?
	private boolean readAutoPatterns(Iter iter, Seq patseq, Thing instance, StringBuilder summary) {
		if (Reader.read(iter, relaxPattern(instance,"context",patseq,"about"), summary))
			return true;
		if (Reader.read(iter, relaxPattern(instance,null,patseq,"about"), summary))
			return true;
		if (Reader.read(iter, relaxPattern(instance,"context",patseq,null), summary))
			return true;
		return Reader.read(iter, patseq, summary);
	}
	
	//match one Pattern for one Thing for one Site
	int match(Storager storager,String patstr, Iter iter, Thing thing, Date time, String path, ArrayList positions) {
		//Date now = Time.date(time);
		int matches = 0;
		//TODO:optimization so pattern with properties is not rebuilt every time?
		iter.pos(0);//reset 
		for (;;) {
			Thing instance = new Thing();
			Seq patseq = Reader.pattern(storager,instance, patstr);
			
			StringBuilder summary = new StringBuilder();
			//boolean read = isRigidPattern(patseq) 
			boolean read = !Property.containedIn(patseq) 
				? readAutoPatterns(iter,patseq,instance,summary)
				: Reader.read(iter, patseq, summary);
			if (!read)
				break;
			
			//plain text before "times" and "is" added
			String nl_text = summary.toString();
			
			/*Thing existing;
			//if ((existing = existing(thing,instance,path,false,null) != null)//old version
			//if ((existing = existing(thing,instance,path,false,nl_text) != null)//new path-full identity
			if ((existing = existing(thing,instance,null,false,nl_text)) != null){//new path-less identity
				if (forcer == null) {
					existing.set(AL.times,now);//update temporal snapshot in-memory
					body.archiver.update(patstr,nl_text,now);//update temporal snapshot in LTM
				}
				continue;
			}
			*/
			
			//TODO check in mapmap by text now!!!
			//TODO if matched, get the "longer" source path!!!???
			if (thingTexts.getObject(thing, nl_text, false) != null)//already adding this
				continue;

			/*
			//TODO: ensure if such GLOBAL "path-less" novelty (ditto above) is required
			//check if latest in LTM
			if (forcer == null && body.archiver.exists(patstr,nl_text)){
				body.archiver.update(patstr,nl_text,now);//update temporal snapshot in LTM
				continue;
			}
			*/
			
//TODO: move all that validation and serialization to updater!!!
//first, add new entities
//next, memorize new snapshot
//also, don't add "is thing" to not memorized instance? 
			
			instance.addThing(AL.is, thing);
			instance.set(AL.times, time);
			instance.setString(AL.text,nl_text);
			Integer textPos = (Integer)positions.get(iter.cur() - 1);
			if (imager != null){
				String image = imager.getAvailableImage(path,textPos);
				if (!AL.empty(image))
					instance.setString(AL.image,image);
			}
			String link = null;
			if (linker != null){
				//measure link pos as link_pos = (link_beg+link_end)/2
				//associate link with text if (text_pos - link_pos) < text_legth/2, where text_pos = (text_beg - text_end)/2
				int range = nl_text.length()/2;
				int text_pos = textPos.intValue() - range;//compute position of text as its middle
				link = linker.getAvailableInRange(path,new Integer(text_pos),range);
			}
			thingTexts.putObject(thing, nl_text, instance);
			thingPaths.putObjects(thing, !AL.empty(link)? link : path == null ? "" : path, instance);
			
			matches++;
		}
		return matches;
	}

	/**
	 * Returns array of [subject,content]
	 * @param thing
	 * @param path
	 * @param news
	 * @return
	 */
	String[] digest(Thing thing, String path, Collection news){
		StringBuilder subject = new StringBuilder();
		StringBuilder content = new StringBuilder();
		String[] unneededNames = new String[]{AL.is,AL.times,AL.sources,AL.text};
		String best = "";
		if (!AL.empty(path))
			content.append(path).append('\n');
		//TODO: group by real path under common root path and have only one path per same-real-path group in digest
		//(assuming the news list is already pre-grouped by real paths - which is true given the entiere implmementation of "The Siter")
		String currentPath = null;
		for (Iterator it = news.iterator(); it.hasNext();) {
			Thing t = (Thing)it.next();
			String nl_text = t.getString(AL.text);
			//TODO:more intelligent approach for subject formation?
			if (best.length() == 0 || (nl_text.length() > best.length() && nl_text.length() <100))
				best = nl_text;
			
			//real path
			Collection sources = t.getThings(AL.sources);
			if (!AL.empty(sources)){
				String source = ((Thing)sources.iterator().next()).getName();
				if (!AL.empty(source) && !path.equals(source)){
					if (currentPath == null || !currentPath.equals(source))
						content.append(source).append('\n');
					currentPath = source;
				}
			}
				
			content.append('\"').append(nl_text).append('\"').append('\n');
			String[] names = Array.sub(t.getNamesAvailable(), unneededNames);
			if (!AL.empty(names)){
				Writer.toString(content, t, null, names, true);//as a form
				content.append('\n');
			}
		}
		subject.append(best).append(" (").append(thing.getName()).append(")");
		return new String[]{subject.toString(),content.toString()};
	}
	
	//TODO: if forcer is given, don't update others
	//- send updates (push notifications)
	//-- Selfer: for a news for thing, send email for all its users (not logged in?) 
	void update(Storager storager, Thing thing,Collection news,String path) {	
		Object[] knows = {AL.knows,thing};
		Object[] sites = {AL.sites,path};
		Object[] knows_trusts = {AL.trusts,thing};
		Object[] sites_trusts = {AL.trusts,path};
		All query = new All(new Object[]{new Seq(knows),new Seq(sites),new Seq(knows_trusts),new Seq(sites_trusts)});
		try {
			Collection peers = storager.get(query,(Thing)null);//forcer?
			String[] digest = digest(thing,path,news);
			if (!AL.empty(peers))
			for (Iterator it = peers.iterator(); it.hasNext();) {
				Thing peer = (Thing)it.next();
				update(storager,peer,thing,news,digest[0],digest[1],"\n"+body.signature());
				
				//TODO: make this more efficient
				//get list of peers trusted by the peer:
				//1) get all peers that are marked to share to by this peer
				Collection allSharesTos = (Collection)peer.get(AL.shares);
				if (!AL.empty(allSharesTos)) {
					//TODO: test if it works
					Set trustingPeers = storager.get(AL.trusts,peer);
					if (!AL.empty(trustingPeers)){
						trustingPeers = new HashSet(trustingPeers);
						//2) restrict all peers with those who have the shares
						trustingPeers.retainAll(allSharesTos);
						for (Iterator tit = trustingPeers.iterator(); tit.hasNext();)
							update(storager,(Thing)tit.next(),thing,news,digest[0],digest[1],"\n"+peer.getTitle(Peer.title_email)+" at "+body.site());
					}
				}
			}
		} catch (Exception e) {
			body.error("Spidering failed ",e);
		}
	}
	
	void update(Storager storager, Thing peer, Thing thing, Collection news, String subject, String content, String signature) throws IOException {
		for (Iterator it = news.iterator(); it.hasNext();) {
			Thing t = (Thing)it.next();
			t.set(AL._new, AL._true, peer);
		}
		
		String email = peer.getString(AL.email);
		if (!AL.empty(email) && !Body.testEmail(email) && Emailer.valid(email)) {
//TODO: from body
			String notify = peer.getString(Peer.email_notification);
			if (AL._true.equals(notify))			
				Emailer.getEmailer().email(peer, subject, content+signature);
		} else {
			String phone = peer.getString(Peer.phone);
//TODO: via Texter Communicator
			if (!AL.empty(phone))
				body.textMessage(phone, subject, content+signature);
		}
	}

	//get count of news not trusted by the 1st peer trusted by self
	public static int pendingNewsCount(Body body) {
		int untrusted = 0;
		Storager storager = body.storager;
		//say for Android, display count of news specific to self owner (1st one trusted peer) 
		Collection trusts = storager.get(AL.trusts, body.self());
		if (!AL.empty(trusts)) {
			Thing peer = (Thing)trusts.iterator().next();
			Collection news = (Collection)peer.get(AL.news, peer);
			if (!AL.empty(news)) {
				for (Iterator it = news.iterator(); it.hasNext();) {
					Thing t = (Thing) it.next();
					Object trust = t.get(AL.trust,peer);
					if (trust == null || !trust.equals(AL._true))
						untrusted++;
				}
			}
		}
		return untrusted;
	}
	
	/*
	//TODO: all that is nt working good so far...
	//... it is intended to create strings from patternless sites
	void count(HashMap map, String token) {
		Object o = map.get(token);
		if (o == null)
			map.put(token,new Integer(1));
		else
			map.put(token,new Integer( ((Integer)o).intValue() + 1 ));
	}
	int summarize(Storager storager, String text, Date time, String path) {
		//TODO: re-use iter building it one step above
		Iter iter = new Iter(Parser.parse(text));
		final int block = 20; //magic average block length
		int blocks = (iter.size() + block - 1) / block;
		if (blocks > 1) {
			HashMap overall = new HashMap();
			HashMap[] phrases = new HashMap[blocks];
			for (int b = 0; b < blocks; b++)
				phrases[b] = new HashMap();
			for (iter.pos(0); iter.has(); iter.next()) {
				count(overall,(String)iter.get());
				count(phrases[iter.cur() / block],(String)iter.get());
			}
			int best = 0;
			for (Iterator it = overall.keySet().iterator(); it.hasNext();) {
				String token = (String)it.next();
				int total = ((Integer)overall.get(token)).intValue();
				int finds = 0;
				for (int b = 0; b < blocks; b++) {
					Object found = phrases[b].get(token);
					if (found != null)
						finds++;					
				}
				int score = total/finds;
				overall.put(token,new Integer(score));
				if (best < score)
					best = score;
			}
			StringBuilder buf = new StringBuilder();
			for (Iterator it = overall.keySet().iterator(); it.hasNext();) {
				String token = (String)it.next();
				int score = ((Integer)overall.get(token)).intValue();
				if (score >= best) {
					if (buf.length() > 0)
						buf.append(' ');
					buf.append(token);
				}
			}
			if (buf.length() > 0) {
				Thing instance = new Thing();
				instance.setString(AL.text,buf.toString());
				instance.set(AL.times, time);

				boolean found = false;
				Collection already = storager.get(instance);
				if (already != null) {
					if (path == null)
						found = true;
					else
						for (Iterator it = already.iterator(); it.hasNext();)
							if  (Array.contains(
								(Collection)((Thing)it.next()).get(AL.sources),
								storager.getNamed(path))) {
								found = true;
								break; // found instance for the same site	
							}
				}
				if (!found)
				{
					instance.store(storager);
					try {
						if (path != null)
							storager.add(instance, AL.sources, path);
					} catch (Exception e) {
						body.error(e.toString(), e);
					}
					return 1;
				}
			}
		}
		return 0;
	}
	*/
	
}
