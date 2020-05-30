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

//import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.webstructor.al.AL;
//import net.webstructor.core.Thing;
import net.webstructor.util.MapMap;

//TODO: make this actually usable in Spider and Siter context
public class WebCrawler { 
	Siter siter;

	WebCrawler(Siter siter){
		this.siter = siter;
	}

	public int crawl(String rootPath, Collection topics, Date time, MapMap thingPathsCollector) {
		if (!AL.isURL(rootPath) || AL.isIMG(rootPath))
			return -1;
		int hits = 0;
		/*
		for (Object topic : topics){
			Thing t = (Thing)topic;
			if (siter.expired())
				break;
			Collection goals = new ArrayList(1);
			goals.add(t);
			String name = t.getName();
			
			siter.body.reply("Site crawling thing begin "+name+" in "+rootPath+".");
			boolean found = new PathTracker(siter,goals,siter.range).run(rootPath);
			siter.body.reply("Site crawling thing end "+(found ? "found" : "missed")+" "+name+" in "+rootPath+".");
			if (found)
				hits++;
		}
		*/
		return hits;
	}
}




