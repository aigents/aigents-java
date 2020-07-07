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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Period;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Time;
import net.webstructor.core.Property;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.ContentLocator;
import net.webstructor.util.MapMap;
import net.webstructor.util.Str;

public class Matcher { 
	protected Body body;
	protected Storager storager;
	
	public Matcher(Body body) {
		this.body = body;
		this.storager = body !=  null ? body.storager : null;
	}
	
	//TODO: move to other place
	public Seq relaxPattern(Thing instance, String context, Seq patseq, String about) {
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
	public boolean readAutoPatterns(Iter iter, Seq patseq, Thing instance, StringBuilder summary) {
		if (!Property.containedIn(patseq)){
			if (Reader.read(iter, relaxPattern(instance,"context",patseq,"about"), summary))
				return true;
			if (Reader.read(iter, relaxPattern(instance,null,patseq,"about"), summary))
				return true;
			if (Reader.read(iter, relaxPattern(instance,"context",patseq,null), summary))
				return true;
		}
		return Reader.read(iter, patseq, summary);
	}

	public void matchPeersText(Collection things, String text, Date time, String permlink, String imgurl){
		MapMap thingPaths = new MapMap();//collector
		int matches = matchThingsText(things,text,time,permlink,imgurl,thingPaths);
		if (matches > 0)
			body.getPublisher().update(null,time,thingPaths,false,null);//forced=false, because may be retrospective
	}
	
	//TODO: move to other place!?
	public int matchThingsText(Collection allThings, String text, Date time, String permlink, String imgurl, MapMap thingPaths){
		return matchThingsText(allThings, text, time, permlink, imgurl, thingPaths, null, null);
	}
	public int matchThingsText(Collection allThings, String text, Date time, String permlink, String imgurl, MapMap thingPaths, ContentLocator titler, ContentLocator imager){
//TODO: actual image positions based on text MD/HTML parsing!? 
			if (!AL.empty(imgurl)) {
				if (imager == null)
					imager = new ContentLocator();
				TreeMap tm = imager.getMap(permlink);
       			tm.put(new Integer(0), imgurl);
			}
			Iter parse = new Iter(Parser.parse(text));
			int matches = 0;
			long start = System.currentTimeMillis();  
			body.debug("Siter matching start "+permlink);
			for (Object thing: allThings) {
				int match = match(parse, null, (Thing)thing, time, permlink, null, thingPaths, imager, null, titler);
				if (match > 0) {
					body.debug("Siter matching found "+((Thing)thing).name()+" in "+permlink);
					matches += match;
				}
			}
			long stop = System.currentTimeMillis();  
			body.debug("Siter matching stop "+permlink+", took "+Period.toHours(stop-start));
			return matches;
	}

	//match one Pattern for one Thing for one Site
	public int match(String patstr, Iter iter, Thing thing, Date time, String path, ArrayList positions, MapMap thingTexts, MapMap thingPaths, ContentLocator imager, ContentLocator linker, ContentLocator titler) {
		Date now = Time.date(time);
		int matches = 0;
		//TODO:optimization so pattern with properties is not rebuilt every time?
		iter.pos(0);//reset 
//TODO: cleanup here and below
//if ("precognition".equals(thing.getName()) || (path != null && path.contains("scientificexploration")))
//matches = matches;
		for (;;) {
			Thing instance = new Thing();
			Seq patseq = Reader.pattern(storager,instance, patstr);
			
			StringBuilder summary = new StringBuilder();
			boolean read = readAutoPatterns(iter,patseq,instance,summary);
			if (!read)
				break;
			
			//plain text before "times" and "is" added
			String nl_text = summary.toString();

			//TODO check in mapmap by text now!!!
			//TODO if matched, get the "longer" source path!!!???
//if ("precognition".equals(thing.getName()))
//nl_text = nl_text;
			if (thingTexts != null && thingTexts.getObject(thing, nl_text, false) != null)//already adding this
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
			instance.set(AL.times, now);
			instance.setString(AL.text,nl_text);
			Integer textPos = positions == null ? new Integer(0) : (Integer)positions.get(iter.cur() - 1);
			//try to get title from the structure or generate it from the text
			String title_text = title(path, nl_text, textPos, titler);
			if (!AL.empty(title_text))
				instance.setString(AL.title,title_text);
			if (imager != null){
				String image = imager.getAvailable(path,textPos);
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
			if (thingTexts != null)
				thingTexts.putObject(thing, nl_text, instance);
			if (thingPaths != null)
				thingPaths.putObjects(thing, !AL.empty(link)? link : path == null ? "" : path, instance);
			
			matches++;
		}
		return matches;
	}

	//match all Patterns of one Thing for one Site and send updates to subscribed Peers
	//TODO: Siter extends Matcher (MapMap thingTexts, MapMap thingPaths, Imager imager, Imager linker)
	public int match(Iter iter,ArrayList positions,Thing thing,Date time,String path, MapMap thingTexts, MapMap thingPaths, ContentLocator imager, ContentLocator linker, ContentLocator titler) {
		//TODO: re-use iter building it one step above
		//ArrayList positions = new ArrayList();
		//Iter iter = new Iter(Parser.parse(text,positions));//build with original text positions preserved for image matching
		int matches = 0;
		//first, try to get patterns for the thing
		Collection patterns = (Collection)thing.get(AL.patterns);
		//next, if none, create the pattern for the thing name manually
		if (AL.empty(patterns))
			//auto-pattern from thing name split apart
			matches += match(thing.name(),iter,thing,time,path,positions, thingTexts, thingPaths, imager, linker, titler);
		if (!AL.empty(patterns)) {
			for (Iterator it = patterns.iterator(); it.hasNext();){				
                matches += match(((Thing)it.next()).name(),iter,thing,time,path,positions, thingTexts, thingPaths, imager, linker, titler);
			}
		}
		return matches;
	}
	
	public String title(String path, String nl_text, int pos, ContentLocator titler) {
		if (titler == null)
			return shortTitle(nl_text);
		String title_text = titler.getAvailableUp(path,0);
		String header_text = titler.getAvailableUp(path,pos);
		if (AL.empty(title_text) && AL.empty(header_text))
			return shortTitle(nl_text);
		if (!AL.empty(title_text) && !AL.empty(header_text)) {
			if (title_text.contentEquals(header_text))
				return title_text;
			double t = Str.simpleTokenizedProximity(nl_text,title_text,AL.punctuation+AL.spaces);
			double h = Str.simpleTokenizedProximity(nl_text,header_text,AL.punctuation+AL.spaces);
			return h > t ? header_text : title_text;
		}
		return AL.empty(header_text) ? title_text : header_text;
	}
	
	public String shortTitle(String text) {
		if(text.matches("(?![0-9]).*["+AL.punctuation+"](?![0-9]).*")) {
			String[] tokens = text.split("["+AL.punctuation+"]");
			for(String s : tokens) {
				while(s.endsWith(" "))
					s = s.substring(0, s.length()-1);
				while(s.startsWith(" "))
					s = s.substring(1, s.length());
				if(s.contains(" "))
					return s;
			}
		}
		return text;
	}
}
