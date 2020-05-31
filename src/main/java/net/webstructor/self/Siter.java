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
import net.webstructor.cat.HttpFileReader;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.Crawler;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.data.ContentLocator;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;
import net.webstructor.util.MapMap;

//TODO split Siter into Siter framework and WebCrawler   
//TODO Siter.crawl => WebCrawler.crawl
//TODO make WebCrawler to implement Crawler
//TODO move sitecacher = new GraphCacher("www", this); under the umbrella of the WebCrawler
public class Siter {

	public static final int DEFAULT_RANGE = 1;//TODO: make configurable
	
//TODO: get rid of this as it is not actually used (or used only to make "search" and "crawl" results consistent)!?	
	public static String punctuation = null; //".";//to maintain the sentence structure
	
	enum Mode { SMART, TRACK, FIND };
	
	Body body;
	protected Matcher matcher;
	
	String rootPath;//search path
	Collection targetTopics;//search goals
	long tillTime;// allocated time
	long newsLimit;// limits number of findings
	Date realTime;// real time to use for page refreshment
	Date timeDate;// = Time.day(Time.today); // database time to use for storage
	boolean forced;//forcing peer: if not null - re-read unconditionally, if null - read only if content is changed
	boolean strict;//true - follow same-domain links only, false - follow all links
	int range;//TODO unify with PathFinder's hopLimit
	Mode mode;//mode - [smart|track|find] - whether to use existing path if present only (track) or always explore new paths (find) or track first and go to find if nothing is found (default - smart)
	
	MapMap thingPaths; //current findings: thing->path->instance
	MapMap thingTexts; //current findings: thing->text->instance	
	ContentLocator imager;//positions of images
	ContentLocator linker;//positions of links
	ContentLocator titler;//positions of titles/headers

	Crawler crawler;//Crawler that is currently being tried //TODO fix hack!?

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
		this.realTime = time;
		this.timeDate = Time.date(time);
		this.forced = forced;
		this.strict = strict;
		this.tillTime = tillTime;
		this.range = range < 0 ? 0 : range;
		this.newsLimit = limit;
		this.mode = "track".equalsIgnoreCase(mode) ? Mode.TRACK : "find".equalsIgnoreCase(mode) ? Mode.FIND : Mode.SMART;
		
		//first, try to get things specified by thing name name (if explicitly provided)
		targetTopics = body.storager.getNamed(thingname);
		if (AL.empty(targetTopics)) {
			//next, try to find all things people are aware of:
			//build list of things that are known to and trusted by users of given path
			targetTopics = new HashSet();
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
									targetTopics.add(thing);
								}
						}
					}
				}
			}
		}
		return this;
	}
	
	protected boolean expired(){
		boolean expired = tillTime > 0 && System.currentTimeMillis() > tillTime;
		if (expired)
			body.debug("Site crawling time out:"+rootPath);
		return expired;
	}
	
	public boolean read() {
		body.filecacher.clearContext();
		body.debug("Site crawling root begin "+rootPath+".");
		long start = System.currentTimeMillis(); 

		boolean ok = false;
		for (Crawler c : body.getCrawlers())//try channel-readers first
			//set crawler in the Siter context so it can be referred by PathFinder/PathTracker 
			if ((this.crawler = c).crawl(this, rootPath, targetTopics, realTime, thingPaths) >= 0) {
				ok = true;
				break;
			}
/*
//TODO use conventional Crawler here!!!???
		if (!ok)//use no channel-reader responded, try site-reader as fallback
			//ok = crawl(targetTopics) > 0;
//TODO move to registry
			ok = (crawler = new WebCrawler(body)).crawl(this, rootPath, targetTopics, realTime, thingPaths) > 0;
*/			

		if (ok) {//send updates on success
			int hits = body.getPublisher().update(rootPath,timeDate,thingPaths,forced,null);
			ok = hits > 0;
		}

		thingPaths.clear();//help gc
		thingTexts.clear();
		body.filecacher.clearContext();
			
		long stop = System.currentTimeMillis(); 
		body.debug("Site crawling root end "+(ok ? "found" : "missed")+" "+rootPath+", took "+Period.toHours(stop-start));
		return ok;
	}
	
	protected void index(String path,Date time,Iter iter,ArrayList links){
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
	
	/*
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
	*/
	
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

	/*
	boolean readPage(String path,ArrayList links,Collection things) {
		Thread.yield();
		boolean result = false;
		boolean skipped = false;
		boolean failed = false;
		String text = null;
		
		body.reply("Site crawling page begin "+path+".");
		if (!AL.isURL(path)) // if not http url, parse the entire text
			result = match(new Iter(Parser.parse(path)),null,timeDate,null,things);//with no positions
		else
		//TODO: distinguish skipped || failed in readIfUpdated ?
		if (!AL.empty(text = body.filecacher.readIfUpdated(path,links,imager.getMap(path),linker.getMap(path),titler.getMap(path),forced,realTime))) {
			ArrayList positions = new ArrayList();
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
		if (iter != null && iter.size() > 0) {
			if (!AL.empty(things)) {
				for (Iterator it = things.iterator();it.hasNext();)
					matches += matcher.match(iter,positions,(Thing)it.next(),time,path, thingTexts, thingPaths, imager, linker, titler);
			}
		}
		return matches > 0;
	}
	*/

}
