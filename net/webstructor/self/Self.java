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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.cat.StringUtil;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;

public class Self {
	public static final String[] reading = new String[] {"reading","read"};
	public static final String[] saving = new String[] {"save","saving"};
	public static final String[] loading = new String[] {"load","loading"};
	public static final String[] parsing = new String[] {"parse","parsing"};
	public static final String[] profiling = new String[] {"profiling","profile"};
	
	//TODO: move to Peer 2017-01-30
	private static void trashPeerLinks(Body body, Thing peer, String name, Collection trusts) {
		Collection links = peer.getThings(name);
		if (AL.empty(links))
			return;
		//think all items and sort by relevance
		Object[][] pairs = body.thinker.think(links, Peer.relevance, peer);
		Arrays.sort(pairs,new Comparator(){
			public int compare(Object arg0, Object arg1) {
				Number n1 = ((Number)((Object[])arg0)[1]);
				Number n2 = ((Number)((Object[]) arg1)[1]);
				return n2.intValue() - n1.intValue();
			}
		});
		
		int trustedMax = StringUtil.toIntOrDefault(peer.getString(Peer.trusts_limit),10,Body.PEER_TRUSTS_LIMIT);
		int linkedMax = StringUtil.toIntOrDefault(peer.getString(Peer.items_limit),10,Body.PEER_ITEMS_LIMIT);
		int trusted = 0;
		int linked = 0;
		//go down relevance and count
		//if count exceeded, remove trust for trusted and remove item for untrusted
		for (int i = 0; i < pairs.length; i++){
			Thing t = (Thing)(pairs[i][0]);
			if (!AL.empty(trusts) && trusts.contains(t))//untrust extra trusts
				if (trusted < trustedMax)
					trusted++;
				else
					peer.delThing(AL.trusts, t);
			if (linked < linkedMax)
				linked++;
			else
				peer.delThing(name, t);
		}
	}
	
	private static void trashPeerLinks(Body body, Thing peer) {
		body.thinker.think(peer);
		Collection trusts = peer.getThings(AL.trusts);
		trashPeerLinks(body,peer,AL.knows,trusts);
		trashPeerLinks(body,peer,AL.sites,trusts);
	}
	
	private static void clear(Body body, Thing peer) {
		//1) for each of the peers, remove more than 999 oldest untrusted news
		int maxNews = Body.MAX_UNTRUSTED_NEWS;//TODO: make configurable
		Collection news = peer.getThings(AL.news);
		if (!AL.empty(news) && news.size() > maxNews){
			news = new HashSet(news);
			Collection trusts = peer.getThings(AL.trusts);
			if (!AL.empty(trusts))
				news.removeAll(trusts);
			if (news.size() > maxNews) {
				ArrayList all = new ArrayList(news);
				Collections.sort(all,new Comparator(){
					public int compare(Object arg0, Object arg1) {
						Thing t0 = (Thing)arg0;
						Thing t1 = (Thing)arg1;
						Object d0 = t0.get(AL.times); 
						Object d1 = t1.get(AL.times); 
						return d0 instanceof Date && d1 instanceof Date ? ((Date)d1).compareTo((Date)d0) : 0;
					}});
				for (Iterator ex = all.subList(maxNews, all.size()).iterator(); ex.hasNext();)
					peer.delThing(AL.news, (Thing)ex.next());
			}
		}
		
		//2) remove junk 'knows' and 'ignores'
		HashSet peerLinks = new HashSet();
		Collection knows = peer.getThings(AL.knows);
		Collection ignores = peer.getThings(AL.ignores);
		if (!AL.empty(knows))
			peerLinks.addAll(knows);
		if (!AL.empty(ignores))
			peerLinks.addAll(ignores);
		for (Iterator tit = peerLinks.iterator(); tit.hasNext();){
			Thing t = (Thing)tit.next();
			String name = t.getName();
			if (body.languages.scrub(name) || Array.contains(body.storager.getNames(),name)){
				peer.delThing(AL.knows, t);
				peer.delThing(AL.trusts, t);
				peer.delThing(AL.ignores, t);
			}
		}
		
		//TODO: enable or disable auto clearing
		//clearTrusts(body, peer, AL.knows);
		//clearTrusts(body, peer, AL.sites);
		trashPeerLinks(body, peer);
	}
	
	public static void clear(Body body,String[] exceptions) {
		//1) first, forget timed things
		//TODO: consider, for non-free version, retain trusted things
		int days_to_retain = body.retentionDays();
		Date[] days = new Date[days_to_retain];
		for (int i = 0; i < days_to_retain; i++) {
			days[i] = Time.today(-i);
		}
		Collection olds = body.storager.get(AL.times,days,false);
		body.storager.del(olds,true);
			
		//2) for each of the peers, remove more than 999 oldest untrusted news
		Collection peers;
		try {
			peers = (Collection)body.storager.getByName(AL.is,Schema.peer);
			if (!AL.empty(peers))
				for (Iterator it = peers.iterator(); it.hasNext();)
					clear(body,(Thing)it.next());
			
		} catch (Exception e) {
			body.error("Forgetting", e);
		}
			
		//3) finally, do plain garbage collection
		Collection news = body.storager.get(AL.times,days,true);
		body.storager.clear(exceptions, AL.empty(news) ? null : new HashSet(news) );
		
		//4) if memory is low, do Java GC
		if (body.checkMemory() > 90)
			System.gc();
	}
	
	public static boolean save(Body body,String path) {
		try {
			new Streamer(body).write(path);
			return true;
		} catch (Exception e) {
			body.error("Saving error",e);
			return false;
		}
	}
	
	public static boolean load(Body body,String path) {
		try {
			Thing trueself = body.self();
			new Streamer(body).read(path);
			
			//ensure there is no self personality split!
			Collection selfs = new ArrayList((Collection)body.storager.getByName(AL.is,Schema.self));
			for (Iterator it = selfs.iterator(); it.hasNext();) {
				Thing anotherself = (Thing)it.next();
				if (!anotherself.equals(trueself))
					//update real self from other with eferything
					trueself.update(anotherself,anotherself.getNamesAvailable());
			}
			//remove doubles
			for (Iterator it = selfs.iterator(); it.hasNext();) {
				Thing anotherself = (Thing)it.next();
				if (!anotherself.equals(trueself))
					//self-destroy other
					anotherself.del();
			}
			
			return true;
		} catch (Exception e) {
			body.error(e.toString(),e);
			return false;
		}
	}

	//TODO: async invocation, ensuring no tests fail
	//public static boolean read(Body body, Thing target) {
	//	return body.act("read", target);
	//}
	
}
