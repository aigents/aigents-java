/*
 * MIT License
 * 
 * Copyright (c) 2015-2020 by Anton Kolonin, Aigents®
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

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Seq;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Actioner;
import net.webstructor.core.Environment;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;
import net.webstructor.util.MapMap;
import net.webstructor.util.Str;

public class Publisher { 
	protected Body body;
	protected Storager storager;
	
	private static final String[] text_date = new String[] {AL.text,AL.date};

	public Publisher(Body body){
		this.body = body;
		storager = body.storager;
	}

	public int update(String rootPath,Date time,MapMap thingPaths,boolean forced,Thing context){
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
			Collection latest = latest(thing,null);//assuming we can ignore the same things with different paths 
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
						String thingName = thing.name();
						Date date = instance.getDate(AL.times,null);
						if (!date.equals(now) && !AL.empty(body.storager.get(instance,text_date)))//ignore old copies
							continue;
					
//TODO: use "forced" consistently						
//TODO: make sure if GLOBAL novelty is required, indeed... 
//TODO: use "newly existing" logic same as used in archiver.exists!?
						
						Thing existing = existing(latest,instance,false,text);
						if (existing != null) {//new path-less identity
							existings.put(instance,existing);
							continue;
						}
						//checking for existence before today, not just today... 
						boolean exists = body.archiver.exists(thingName,text,null);//ignore date
						if (!forced && exists)//check LTM
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
				//TODO: real path here!!??
				//update(storager,thing,instances,rootPath);
			}
			body.getPublisher().update(thing,collector,rootPath,context);
		}		
		//memorize everything known and novel in STM AND LTM snapshots
//TODO: optimization to avoid doing extra stuff below!!!		
		for (int j = 0; j < things.length; j++){
			Thing thing = (Thing)things[j];
			Object[] paths = thingPaths.getSubKeyObjects(thing);
//boolean debug = "precognition".equals(thing.getName());
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
						String thingName = thing.name();
						Thing existing = existings.get(instance);
						
//if (debug)
//body.debug("Publisher update instance "+instance.getString(AL.text));
//if (debug)
//body.debug("Publisher update existing STM "+existing.getString(AL.text));

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

	/*
	private Thing existingNonPeriodic(Thing thing, Thing instance, String path, boolean debug, String text) {
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
	}
	*/

	protected Thing existing(Collection latest, Thing instance, boolean debug, String text) {
		if (debug)
			body.debug("instance:"+Writer.toString(instance));
		if (!AL.empty(latest))
			for (Iterator it = latest.iterator(); it.hasNext();) {
				Thing t = (Thing)it.next();
				if (debug)
					body.debug("latest  :"+t);
				String latestText = t.getString(AL.text);
				if (!AL.empty(text) && !AL.empty(latestText)){//TODO: validate by text!!!??? 
					if (text.equals(latestText)){
						if (debug)
							body.debug("novel   :false");
						return t;
					}
				} else
				if (body.storager.match(t, instance)) {
					if (debug)
						body.debug("novel   :false");
					return t;
				}
			}
		if (debug)
			body.debug("novel   :true");
		return null;
	}
	
	protected Thing existing(Thing thing, Thing instance, String path, boolean debug, String text) {
		return existing(latest(thing,path), instance, debug, text);
	}

	protected Collection latest(Thing is, String path) {
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
	
	/**
	 * Returns array of [subject,content]
	 * @param thing
	 * @param path
	 * @param news
	 * @return
	 */
	protected String[] digest(Thing thing, String path, Collection news, boolean verbose){
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
				String source = ((Thing)sources.iterator().next()).name();
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
		return new String[]{thing.name(),content.toString()};
	}
	
	//TODO: if forcer is given, don't update others
	//- send updates (push notifications)
	//-- Selfer: for a news for thing, send email for all its users (not logged in?) 
	protected void update(Thing thing,Collection news,String path,Thing group) {	
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
				update(thing,news,path,peers,group == null);//verbose digests only for sites!?
		} catch (Exception e) {
			body.error("Spidering update failed ",e);
		}
	}

	public void update(Thing thing,Collection news,String path,Collection peers,boolean verbose) throws IOException {
//TODO: make digest individual for peers, generated inside the peer-specific update() method
		String[] digest = digest(thing,path,news,verbose);
		if (AL.empty(digest))
			return;
		for (Iterator it = peers.iterator(); it.hasNext();) {
			Thing peer = (Thing)it.next();
			update(peer,thing,news,digest[0],digest[1],body.signature());
			Collection allSharesTos = Peer.getSharesTos(storager,peer);
			if (!AL.empty(allSharesTos)) for (Iterator tit = allSharesTos.iterator(); tit.hasNext();)
				update((Thing)tit.next(),thing,news,digest[0],digest[1],signature(body,peer));
		}
	}
	
	protected String signature(Body body,Thing peer){
		return peer.getTitle(Peer.title_email)+" at "+body.site();
	}
	
	protected void update(Thing peer, Thing thing, Collection news, String subject, String content, String signature) throws IOException {
		for (Iterator it = news.iterator(); it.hasNext();) {
			Thing t = (Thing)it.next();
//TODO: eliminate duplicated !!!untrusted things here on peer-specific basis!!!???
			t.set(AL._new, AL._true, peer);
		}
		body.update(peer, null, subject, content, signature);
	}

	//get count of news not trusted by the 1st peer trusted by self
	public int pendingNewsCount() {
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
	public Actioner getUpdater(){
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
						body.update(peer, null, subject, content, signature);
					} catch (IOException e) {
						body.error("Siter updating "+subject+" "+text+" "+signature,e);
					}
				}
				return true;
			}
		};
	}
	
}




