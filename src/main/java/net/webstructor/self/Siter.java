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
	int newsLimit;// limits number of findings
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
	
	public Siter init(String topic,Date time,boolean forced,long tillTime,int range,int limit,boolean strict,String mode) {
		this.realTime = time;
		this.timeDate = Time.date(time);
		this.forced = forced;
		this.strict = strict;
		this.tillTime = tillTime;
		this.range = range < 0 ? 0 : range;
		this.newsLimit = limit;
		this.mode = "track".equalsIgnoreCase(mode) ? Mode.TRACK : "find".equalsIgnoreCase(mode) ? Mode.FIND : Mode.SMART;
		
		//first, try to get things specified by thing name name (if explicitly provided)
		targetTopics = body.storager.getNamed(topic);
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
	
	public String getRootPath() {
		return rootPath;
	}
	
	public Collection getTopics() {
		return targetTopics;
	}
	
	public Date getTime() {
		return realTime;
	}
	
	public MapMap getPathBasedCollector() {
		return thingPaths;
	}
	
	public int getLimit() {
		return newsLimit;
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
//TODO we do so because in case of "non-url source queries" the targetTopics are formed inside the crawlers based on the queries...
		if (!AL.empty(targetTopics) || (!forced && !AL.isURL(rootPath)))
		for (Crawler c : body.getCrawlers())//try channel-readers first
			//set crawler in the Siter context so it can be referred by PathFinder/PathTracker 
			if ((this.crawler = c).crawl(this) >= 0) {
				//int hits = body.getPublisher().update(rootPath,timeDate,thingPaths,forced,null);
//TODO consider hack - setting rootPath for non-url queries so can have unit tests passed for text being parsed instead of being searched
//TODO for this purpose, add textCrawler as separate plugin, added after the "Serper-s"
				int hits = body.getPublisher().update(forced || AL.isURL(rootPath) ? rootPath : null,timeDate,thingPaths,forced,null);
				ok = hits > 0;
				break;
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
	
	boolean linkMatch(String text,Seq patseq) {
		Iter iter = new Iter(Parser.parse(text));
		try {
			if (Reader.read(iter, patseq, null))
				return true;
		} catch (Throwable e) {
			body.error("Siter error link "+text+" pattern "+patseq+":", e);
		}
		return false;
	}

}
