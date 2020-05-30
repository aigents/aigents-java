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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Period;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.Crawler;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.data.ContentLocator;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;
import net.webstructor.util.MapMap;

//TODO split Siter into Siter framework and WebCrawler   
//TODO Siter.newSpider => WebCrawler.crawl
//TODO make Redditer, Twitter, Discourse => implement Crawler 
//TODO make WebCrawler to implement Crawler
//TODO move sitecacher = new GraphCacher("www", this); under the umbrella of the WebCrawler
public class Siter {

	public static final int DEFAULT_RANGE = 1;//TODO: make configurable
	public static String punctuation = null; //".";//to maintain the sentence structure
	
	enum Mode { SMART, TRACK, FIND };
	
	Body body;
	private String thingname;
	String rootPath;
	
	Date realTime; // real time to use for page refreshment
	Date timeDate; // = Time.day(Time.today); // database time to use for storage
	boolean forced;//forcing peer: if not null - re-read unconditionally, if null - read only if content is changed
	boolean strict;//true - follow same-domain links only, false - follow all links
	int range;//TODO unify with PathFinder's hopLimit
	Mode mode;//mode - [smart|track|find] - whether to use existing path if present only (track) or always explore new paths (find) or track first and go to find if nothing is found (default - smart)

	long tillTime;
	long newsLimit;
	
	Collection allThings;
	MapMap thingPaths; //thing->path->instance
	MapMap thingTexts; //thing->text->instance	
	ContentLocator imager;
	ContentLocator linker;//using Imager to keep positions of links
	ContentLocator titler;
	Matcher matcher;

	public Siter(Body body,String path) {
		this.body = body;
		this.rootPath = path;
		matcher = body.getMatcher();
		thingPaths = new MapMap();
		thingTexts = new MapMap();
		imager = new ContentLocator();
		linker = new ContentLocator();
		titler = new ContentLocator();
	}
	
	public Siter init(String thingname,Date time,boolean forced,long tillTime,int range,int limit,boolean strict,String mode) {
		this.thingname = thingname;
		this.realTime = time;
		this.timeDate = Time.date(time);
		this.forced = forced;
		this.strict = strict;
		this.tillTime = tillTime;
		this.range = range < 0 ? 0 : range;
		this.newsLimit = limit;
		this.mode = "track".equalsIgnoreCase(mode) ? Mode.TRACK : "find".equalsIgnoreCase(mode) ? Mode.FIND : Mode.SMART;
		
		//first, try to get things specified by thing name name (if explicitly provided)
		allThings = body.storager.getNamed(thingname);
		if (AL.empty(allThings)) {
			//next, try to find all things people are aware of:
			//build list of things that are known to and trusted by users of given path
			allThings = new HashSet();
			//TODO: make sites readable not being listed?
			java.util.Set sites = body.storager.get(AL.sites,rootPath);
			java.util.Set peers = !AL.empty(sites)? new HashSet(sites) : null;
			java.util.Set peerSiteTrusts = body.storager.get(AL.trusts,rootPath);
			if (!AL.empty(peers) && !forced){//get things from peers only trusting this site
				if (AL.empty(peerSiteTrusts))
					peers.clear();
				else
					peers.retainAll(peerSiteTrusts);
			}
			if (!AL.empty(peers)){
				Date since = Time.today(-body.attentionDays());
				for (Iterator it = peers.iterator(); it.hasNext();){
					Thing peer = (Thing)it.next();
					int news_limit = StringUtil.toIntOrDefault(peer.getString(Peer.news_limit),10,3);
					this.newsLimit = Math.max(this.newsLimit, news_limit);
					Date activityTime = (Date)peer.get(Peer.activity_time);
					Collection topics = peer.getThings(AL.topics);
					if (activityTime != null && activityTime.compareTo(since) >= 0 && !AL.empty(topics)){
						Collection peerThings = new HashSet(topics);
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
							Set peerThingTrusts = body.storager.get(AL.trusts,thing);
							if (!AL.empty(peerThingTrusts))//if there is at least one peer trusting the thing
								if (forced || Array.intersect(peerThingTrusts,peerSiteTrusts)){
									allThings.add(thing);
								}
						}
					}
				}
			}
		}
		return this;
	}
	
	boolean expired(){
		boolean expired = tillTime > 0 && System.currentTimeMillis() > tillTime;
		if (expired)
			body.debug("Site crawling time out:"+rootPath);
		return expired;
	}
	
	public boolean read() {
		body.filecacher.clearContext();
		Collection topics = !AL.empty(thingname) ? body.storager.getNamed(thingname) : allThings;
		body.debug("Site crawling root begin "+rootPath+".");
		long start = System.currentTimeMillis(); 

		boolean ok = false;
		for (Crawler s : body.getCrawlers())//try channel-readers first
			if (s.crawl(rootPath, topics, realTime, thingPaths) >= 0) {
				ok = true;
				break;
			}
		if (!ok)//use no channel-reader responded, try site-reader as fallback
			ok = crawl(topics) > 0;
			

		if (ok)//send updates on success
			ok = update() > 0;
			
		long stop = System.currentTimeMillis(); 
		body.debug("Site crawling root end "+(ok ? "found" : "missed")+" "+rootPath+", took "+Period.toHours(stop-start));
		return ok;
	}
	
	private void index(String path,Date time,Iter iter,ArrayList links){
		if (body.sitecacher != null){
			//TODO: actual time of the page
			Graph g = body.sitecacher.getGraph(Time.day(time));//daily graph
			//index links
			if (!AL.empty(links))
			for (Iterator it = links.iterator(); it.hasNext();){
				String[] link = (String[])it.next();
				String linkUrl = HttpFileReader.alignURL(path,link[0],false);//relaxed, let any "foreign" links
				//String linkText = link[1];//TODO: use to evaluate source name as the best-weighted link!?
				if (!AL.empty(linkUrl) && g.getValue(path, linkUrl, "links") == null){
					g.addValue(path, linkUrl, "links", 1);
					g.addValue(linkUrl, path, "linked", 1);
				}
			}
			//index words (site->words->word, word->worded->site)
//TODO: check configuration if need word index!?
//TODO prevent double-triple-etc indexing on repeated reads!?
//TODO unify with path in in-text ...
			for (iter.pos(0); iter.has();){
				Object o = iter.next();
				//g.addValue(path, o, "words", 1);
				g.addValue(o, path, "worded", 1);
			}			
		}
	}
	
	//TODO public class WebCrawler implements Crawler
	//TODO Override Crawler.crawl
	private int crawl(Collection topics) {
		int hits = 0;
		for (Object topic : topics){
			Thing t = (Thing)topic;
			if (expired())
				break;
			Collection goals = new ArrayList(1);
			goals.add(t);
			String name = t.getName();
			body.reply("Site crawling thing begin "+name+" in "+rootPath+".");
			boolean found = new PathTracker(this,goals,range).run(rootPath);
			body.reply("Site crawling thing end "+(found ? "found" : "missed")+" "+name+" in "+rootPath+".");
			if (found)
				hits++;
		}
		return hits;
	}
	
	boolean linkMatch(String text,Seq patseq) {
		Iter iter = new Iter(Parser.parse(text));
		try {
			if (Reader.read(iter, patseq, null))
				return true;
		} catch (Throwable e) {
			body.error("Siter linkMatch error pattern "+patseq, e);
		}
		return false;
	}

	boolean readPage(String path,ArrayList links,Collection things) {
		Thread.yield();
		boolean result = false;
		boolean skipped = false;
		boolean failed = false;
		String text = null;
		
		body.reply("Site crawling page begin "+path+".");
		if (!AL.isURL(path)) // if not http url, parse the entire text
			//result = match(storager,path,time,null,things);
			result = match(new Iter(Parser.parse(path)),null,timeDate,null,things);//with no positions
		else
		//TODO: distinguish skipped || failed in readIfUpdated ?
		if (!AL.empty(text = body.filecacher.readIfUpdated(path,links,imager.getMap(path),linker.getMap(path),titler.getMap(path),forced,realTime))) {
			ArrayList positions = new ArrayList();
			//Iter iter = new Iter(Parser.parse(text,positions));//build with original text positions preserved for image matching
			Iter iter = new Iter(Parser.parse(text,null,false,true,true,false,punctuation,positions));//build with original text positions preserved for image matching
			result = match(iter,positions,timeDate,path,things);
			index(path,timeDate,iter,links);
			//TODO: add source name as page title by default?
		} else {
			skipped = true;//if not read 
			failed = true;
		}
		if (skipped || failed)
			body.reply("Site crawling page end "+(failed? "failed": "skipped")+" "+path+".");
		else
			body.reply("Site crawling page end "+(result? "found": "missed")+" "+path+".");
		return result;
	}

	//get all things for the thing name
	private boolean match(Iter iter,ArrayList positions,Date time,String path,Collection things) {
		int matches = 0;
		//if (text != null) {
		if (iter != null && iter.size() > 0) {
			if (!AL.empty(things)) {
				for (Iterator it = things.iterator();it.hasNext();)
					matches += matcher.match(iter,positions,(Thing)it.next(),time,path, thingTexts, thingPaths, imager, linker, titler);
			}
		}
		return matches > 0;
	}

	private int update(){
		int hits = update(body,rootPath,timeDate,thingPaths,forced,null);
		thingPaths.clear();//help gc
		thingTexts.clear();
		body.filecacher.clearContext();
		return hits;
	}
	
	//TODO: make the following non-static
	
	static public int update(Body body,String rootPath,Date time,MapMap thingPaths,boolean forced,Thing context){
		Object[] things = thingPaths.getKeyObjects();
		if (AL.empty(things))
			return 0;
		int hits = 0;
		Date now = Time.date(time);
		HashMap<Thing,Thing> existings = new HashMap<Thing,Thing>(); 
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
					
//TODO: use "forced" consistently						
//TODO: make sure if GLOBAL novelty is required, indeed... 
//TODO: use "newly existing" logic same as used in archiver.exists!?
//TODO: use either existing = existing(... thing or instance to update snapshots on the next round below????
						Thing existing = existing(body,thing,instance,null,false,text);
						if (existing != null) {//new path-less identity
							existings.put(instance,existing);
							continue;
						}
						//checking for existence before today, not just today... 
						Date date = null;//instance.getDate(AL.times,null);
						if (!forced && body.archiver.exists(thingName,text,date))//check LTM
							continue;
					
						hits++;
						instance.store(body.storager);
						if (!AL.empty(path))
							try {
								body.storager.add(instance, AL.sources, path);
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
			body.getPublisher().update(thing,collector,rootPath,context);
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
						Thing existing = existings.get(instance);
//TODO: use "forced" consistently						
						if (!forced && existing != null)//new path-less identity
							existing.set(AL.times,now);//update temporal snapshot in-memory
//TODO don't need not-a-news-item things hanging in memory?
						//if (existing == null) {
						//	instance.store(body.storager);
						//	instance.set(AL.times,now);
						//}
						
						body.archiver.update(thingName,text,now);//update temporal snapshot in LTM
					}
				}
			}
		}
		return hits;
	}

	//TODO: move the following out to time-specific framework!?
	
	/*private Thing existingNonPeiodic(Body body, Thing thing, Thing instance, String path, boolean debug, String text) {
		Collection coll;
		try {
			All query = new All(new Object[]{
					new Seq(new Object[] {AL.is,thing}),
					new Seq(new Object[] {AL.text,text})});
			coll = body.storager.get(query,(Thing)null);
			if (!AL.empty(coll))
				return (Thing)coll.iterator().next();
		} catch (Exception e) {
			body.error("Spidering existence check failed ",e);
		}
		return null;
	}*/
	
	static Thing existing(Body body, Thing thing, Thing instance, String path, boolean debug, String text) {
		Collection coll = latest(body.storager,thing,path);
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
				if (body.storager.match(latest, instance)) {
					if (debug)
						body.debug("novel   :false");
					return latest;
				}
			}
		if (debug)
			body.debug("novel   :true");
		return null;
	}

	static Collection latest(Storager storager, Thing is, String path) {
		HashSet found = new HashSet();
		long latest = 0;
		Collection instances = storager.get(AL.is,is);
		if (!AL.empty(instances)) {
			//TODO: do we really need the clone here to prevent ConcurrentModificationException? 
			for (Iterator it = new ArrayList(instances).iterator(); it.hasNext();) {
				Thing instance = (Thing)it.next();
//String debug_text = Writer.toPrefixedString(null, null, instance);				
				if (path != null && !storager.has(instance,AL.sources,path))
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

	//TODO moved the following to Updater/Notifier/Publisher
	
	/**
	 * Returns array of [subject,content]
	 * @param thing
	 * @param path
	 * @param news
	 * @return
	 */
	/*
	static String[] digest(Body body, Thing thing, String path, Collection news, boolean verbose){
		if (AL.empty(news))//no news - no digest
			return null;
		//StringBuilder subject = new StringBuilder();
		StringBuilder content = new StringBuilder();
		String[] unneededNames = new String[]{AL.is,AL.times,AL.sources,AL.text};
		//String best = "";
		if (!AL.empty(path))
			content.append(path).append('\n');
		//TODO: group by real path under common root path and have only one path per same-real-path group in digest
		//(assuming the news list is already pre-grouped by real paths - which is true given the entiere implmementation of "The Siter")
		String currentPath = null;
		for (Iterator it = news.iterator(); it.hasNext();) {
			Thing t = (Thing)it.next();
			String nl_text = t.getString(AL.text);

			//TODO:more intelligent approach for subject ("title") formation?

			//real path
			Collection sources = t.getThings(AL.sources);
			if (!AL.empty(sources)){
				String source = ((Thing)sources.iterator().next()).getName();
				if (!AL.empty(source) && !source.equals(path)){
					if (currentPath == null || !currentPath.equals(source))
						content.append(source).append('\n');
					currentPath = source;
				}
			}
				
			content.append('\"').append(nl_text).append('\"').append('\n');
			if (verbose){
				String[] names = Array.sub(t.getNamesAvailable(), unneededNames);
				if (!AL.empty(names)){
					Writer.toString(content, t, null, names, true);//as a form
					content.append('\n');
				}
			}
		}
		return new String[]{thing.getName(),content.toString()};
	}
	
	//TODO: if forcer is given, don't update others
	//- send updates (push notifications)
	//-- Selfer: for a news for thing, send email for all its users (not logged in?) 
	static void update(Body body, Storager storager, Thing thing,Collection news,String path,Thing group) {	
		Object[] topics = {AL.topics,thing};
		Object[] sites = {AL.sites,path};
		Object[] topics_trusts = {AL.trusts,thing};
		Object[] sites_trusts = {AL.trusts,path};
		All query = group != null ? new All(new Object[]{new Seq(topics),new Seq(topics_trusts),new Seq(new Object[]{AL.groups,group})})
				: !AL.empty(path) ? new All(new Object[]{new Seq(topics),new Seq(sites),new Seq(topics_trusts),new Seq(sites_trusts)})
				: new All(new Object[]{new Seq(topics),new Seq(topics_trusts)});//TODO: more restrictive!?
		try {
			Collection peers = storager.get(query,(Thing)null);//forcer?
			if (!AL.empty(peers))
				update(body,storager,thing,news,path,peers,group == null);//verbose digests only for sites!?
		} catch (Exception e) {
			body.error("Spidering update failed ",e);
		}
	}

	public static void update(Body body, Storager storager, Thing thing,Collection news,String path,Collection peers,boolean verbose) throws IOException {
//TODO: make digest individual for peers, generated inside the peer-specific update() method
		String[] digest = digest(body,thing,path,news,verbose);
		if (AL.empty(digest))
			return;
		for (Iterator it = peers.iterator(); it.hasNext();) {
			Thing peer = (Thing)it.next();
			update(body,storager,peer,thing,news,digest[0],digest[1],body.signature());
			Collection allSharesTos = Peer.getSharesTos(storager,peer);
			if (!AL.empty(allSharesTos)) for (Iterator tit = allSharesTos.iterator(); tit.hasNext();)
				update(body,storager,(Thing)tit.next(),thing,news,digest[0],digest[1],signature(body,peer));
		}
	}
	
	private static String signature(Body body,Thing peer){
		return peer.getTitle(Peer.title_email)+" at "+body.site();
	}
	
	static void update(Body body, Storager storager, Thing peer, Thing thing, Collection news, String subject, String content, String signature) throws IOException {
		for (Iterator it = news.iterator(); it.hasNext();) {
			Thing t = (Thing)it.next();
//TODO: eliminate duplicated !!!untrusted things here on peer-specific basis!!!???
			t.set(AL._new, AL._true, peer);
		}
		body.update(peer, subject, content, signature);
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
	
	//update all trusting peers being shared
	public static Actioner getUpdater(){
		return new Actioner(){
			@Override
			public boolean act(Environment env, Storager storager, Thing context, Thing target) {
				Body body = (Body)env;
				String signature = signature(body,context);
				Collection is = target.getThings(AL.is);
				Collection sources = target.getThings(AL.sources);
				String subject = AL.empty(is) ? null : ((Thing)is.iterator().next()).getString(AL.name);
				String url = AL.empty(sources) ? null : ((Thing)sources.iterator().next()).getString(AL.name);
				String text = target.getString(AL.text);
				String content = Str.join(new String[]{url,text}, "\n");
				Collection allSharesTos = Peer.getSharesTos(storager,context);
				if (!AL.empty(allSharesTos)) for (Iterator pit = allSharesTos.iterator(); pit.hasNext();){
					Thing peer = (Thing)pit.next();
					target.set(AL._new, AL._true, peer);
					try {
						body.update(peer, subject, content, signature);
					} catch (IOException e) {
						body.error("Siter updating "+subject+" "+text+" "+signature,e);
					}
				}
				return true;
			}
		};
	}
	 */	
}
