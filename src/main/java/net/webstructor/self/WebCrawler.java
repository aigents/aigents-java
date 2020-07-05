/*
 * MIT License
 * 
 * Copyright (c) 2018-2020 by Anton Kolonin, Aigents®
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
//import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.comm.Crawler;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Profiler;

public class WebCrawler implements Crawler { 
	Body body;
	Matcher matcher;

	public WebCrawler(Body body){
		this.body = body;
		matcher = body.getMatcher();
	}

//TODO eliminate all extra arguments 
	@Override
	public int crawl(Siter siter) {
		if (AL.empty(siter.rootPath) || AL.isIMG(siter.rootPath))//don't check AL.isURL because it can expectedly handle plain texts 
			return -1;
		int hits = 0;
		for (Object topic : siter.targetTopics){
			Thing t = (Thing)topic;
			if (siter.expired())
				break;
			Collection goals = new ArrayList(1);
			goals.add(t);
			String name = t.name();
			siter.body.debug("Site crawling root "+siter.rootPath+" "+name+" in "+siter.rootPath+".");
			try {
				boolean found = new PathTracker(siter,goals,siter.range).run(siter.rootPath);
				siter.body.debug("Site crawling start root "+siter.rootPath+" "+name+" "+(found ? "found" : "missed")+".");
				if (found)
					hits++;
			} catch (Throwable e) {
				siter.body.error("Site crawling stop root "+siter.rootPath+" "+name+" falied",e);
			}
		}
		return hits;
	}

//TODO eliminate all extra arguments 
	@Override
	public boolean scalp(Siter siter,String path,ArrayList links,Collection topics) {
		boolean result = false;
		boolean skipped = false;
		boolean failed = false;
		String text = null;
		
		body.reply("Site crawling page begin "+path+".");
		if (!AL.isURL(path)) // if not http url, parse the entire text
			result = match(siter,new Iter(Parser.parse(path)),null,siter.timeDate,null,topics);//with no positions
		else
		//TODO: distinguish skipped || failed in readIfUpdated ?
		if (!AL.empty(text = body.filecacher.readIfUpdated(path,links,siter.imager.getMap(path),siter.linker.getMap(path),siter.titler.getMap(path),siter.forced,siter.realTime))) {
			ArrayList positions = new ArrayList();
			Iter iter = new Iter(Parser.parse(text,null,false,true,true,false,Siter.punctuation,positions));//build with original text positions preserved for image matching
			result = match(siter,iter,positions,siter.timeDate,path,topics);
			siter.index(path,siter.timeDate,iter,links);
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
	private boolean match(Siter siter, Iter iter,ArrayList positions,Date time,String path,Collection topics) {
		int matches = 0;
		if (iter != null && iter.size() > 0) {
			if (!AL.empty(topics)) {
				for (Iterator it = topics.iterator();it.hasNext();)
					matches += matcher.match(iter,positions,(Thing)it.next(),time,path, siter.thingTexts, siter.thingPaths, siter.imager, siter.linker, siter.titler);
			}
		}
		return matches > 0;
	}
	
	@Override
	public String name() {
		return "www";
	}

	@Override
	public SocialFeeder getFeeder(String id, String token, String key, Date since, Date until, String[] areas) throws IOException {
		return null;
	}

	@Override
	public Profiler getProfiler(Thing peer) {
		return null;
	}
}




