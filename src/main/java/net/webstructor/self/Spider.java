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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;

public class Spider {
	Body body;
	
	public Spider(Body body) {
		this.body = body;
	}

	public void spider(long tillTime) 
	{
		try {
			//get all user sites and things
			
			//TODO: spider only active peers
			//-get all peers
			//--who is still active
			//---get their sites
			//----that are trusted
			HashSet sites = new HashSet();
			Collection peers = (Collection)body.storager.getByName(AL.is,Schema.peer);
			
			//TODO:?
			//Collection activePeers = new ArrayList();
			
			if (!AL.empty(peers)){
				Date since = Time.today(-body.retentionDays());
				for (Iterator it = peers.iterator(); it.hasNext();){
					Thing peer = (Thing)it.next();
					Date activityTime = (Date)peer.get(Peer.activity_time);
					if (activityTime != null && activityTime.compareTo(since) >= 0){
						Collection peerSites = peer.getThings(AL.sites);
						Collection peerTrusts = peer.getThings(AL.trusts);
						if (!AL.empty(peerSites) && !AL.empty(peerTrusts)){
							peerSites = new HashSet(peerSites);
							peerSites.retainAll(peerTrusts);
							for (Iterator jt = peerSites.iterator(); jt.hasNext();){
								Thing site = (Thing)jt.next();
								sites.add(site.getName());
							}
						}
					}
				}
			}
			//TODO: same as above to things of peers!?
			
			if (!AL.empty(sites)) {			
				Date time = Time.day(Time.today);
				int remainingSites = sites.size();
				for (Iterator it = sites.iterator(); it.hasNext();){
					//do reading for all user sites and things //TODO:may optimize skipping some sites for some users
					//TODO: how to update the site
					//send notifications to users (accordingly to their check cycle settings)
					//new Siter(body,body.storager,null,sites[i]).spider();
					long currentTime = System.currentTimeMillis();
					if (tillTime != 0 && currentTime > tillTime){
						body.debug("Spidering sites time out");
						break;
					}
					long timePerSite = tillTime == 0 ? 0 : (tillTime - currentTime) / remainingSites;
					int siteRange = 0; //TODO: configure
					int newsLimit = 0; //TODO: configure
					spider((String)it.next(), null, time, timePerSite == 0 ? 0 : currentTime + timePerSite, false, siteRange, newsLimit);
					remainingSites--;
				}
				
				//TODO: should agglomerating be rather incremental!?
				//TODO: make agglomeration working with
				//TODO: agglomerate in "batch" in Selfer.run upon completion of all spidereres
				// -- "scalping" news not loast
				// -- redundant "bursty" news are compacted
				//!!! Commented out as the above did not work...
				//agglomerate(time);
				//body.updateStatus();
				
				if (body.sitecacher != null)
					body.sitecacher.updateGraph(time, body.sitecacher.getGraph(time), System.currentTimeMillis());
			}
		} catch (Exception e) {
			body.error("Spidering sites "+e.toString(),e);
		}
	}

	/*
	private long started = 0;
	private long completed = 0;
	private long spent = 0;
	HashSet tasks = new HashSet();
	
	synchronized long tasks() {
		return started - completed;
	}
	
	synchronized String taskNames() {
		StringBuilder sb = new StringBuilder();
		for (Iterator i = tasks.iterator(); i.hasNext();) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(i.next());
		}
		return sb.toString();
	}
	
	synchronized long timePerTask() {
		return completed == 0 ? 0 : spent / completed;
	}
	
	synchronized void startTask(long time,String name) {
		if (started == completed) {
			started = completed = spent = 0;
			body.reply("Spidering all begin site "+name+".");
		}
		tasks.add(name);
		started++;
	}
	
	synchronized void stopTask(long beginTime, long endTime,String name) {
		tasks.remove(name);
		completed++;
		long time = endTime - beginTime;
		spent += time;
		if (started == completed) {
			body.reply("Spidering all end site "+name+", spent "+time+"/"+completed+"="+timePerTask()+".");
			//TODO: agglomeration and status update should be triggered asynchronously here!!!
		}
	}
	*/
	
	//Deal with network I/O hangups, just skip failed sites
	//http://stackoverflow.com/questions/5715235/java-set-timeout-on-a-certain-block-of-code
	//http://www.javacoffeebreak.com/articles/network_timeouts/
	//http://mrfeinberg.com/blog/archives/000016.html
	public boolean spider(final String site, final String thingname, final Date time, final long tillTime, final boolean forced, final int range,final int limit) {
		final Callable task = new Callable() {
		    //public void run() { /* Do stuff here. */
			public Object call() throws Exception {
		    	//long startTime = System.currentTimeMillis();
		    	//startTask(startTime,site);
		    	Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		    	boolean ok = false;
		    	try {
		    		ok = new Siter(body,body.storager,thingname,site,time,forced,tillTime,range,limit).read();
		    	} catch (Throwable t){
					body.error("Spidering site failed unknown "+site+" "+t.toString()+",",t);
		    	}
		    	//stopTask(startTime,System.currentTimeMillis(),site);
				return new Boolean(ok);
			}
		};
//TODO: self.addThing("readings",site);
		long startTime = System.currentTimeMillis();
		body.reply("Spidering site "+site+", started "+new Date(startTime)+".");
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(task);
		executor.shutdown(); // This does not cancel the already-scheduled task.
		//TODO: make site reading timeout configurable
		//TODO: make this working for "slow" sites like zakupki with huge delay in robots.txt
		boolean ok = false;
		try {
			//wait for one minute or till requested
			long timeout = tillTime <= 0 ? Period.MINUTE : tillTime - startTime;
			Boolean result = (Boolean)future.get(timeout, TimeUnit.MILLISECONDS);
			if (result != null)
				ok = result.booleanValue();//if waited till he end, get result
		}
		catch (InterruptedException ie) {  
			body.error("Spidering site failed interrupted "+site+" "+ie.toString()+",",ie);
		}
		catch (ExecutionException ee) {  
			body.error("Spidering site failed execution "+site+" "+ee.toString()+",",ee);
		}
		catch (TimeoutException te) {
			future.cancel(true);//let it be dead//TODO:make sure if it is working
			body.error("Spidering site failed timeout "+site+" "+te.toString()+",",te);
			ok = true;//if not waited, be optimistic, assume success
		}
		catch (Throwable t) {  
			body.error("Spidering site failed unknown "+site+" "+t.toString()+",",t);
		}
		if (!executor.isTerminated())
		    executor.shutdownNow(); // If you want to stop the code that hasn't finished.
		long endTime = System.currentTimeMillis();
		body.reply("Spidering site "+site+" "+(ok?"succeeded":"failed")+" "+new Date(endTime)+" took "+new Period(endTime-startTime).toMinutes()+".");
//TODO: self.delThing("readings",site);
		return ok;
	}

	//TODO:
	void agglomerate(final Date time) {
		//--- for all news of reading time
		//---- compare news each-to each
		//----- if have variables, compare on variables (TODO later?)
		//----- if have no variables, compare on text
		//----- if matched, aggregate the two news, having sources agglomerated
		ArrayList news = new ArrayList(body.storager.get(AL.times, time));
		HashSet merged = new HashSet();
		//for (Iterator i = news.iterator(); i.hasNext();)
			//body.debug(i.next().toString());
		for (int i1 = 0; i1 < news.size(); i1++) {
			Thing t1 = (Thing)news.get(i1);
			String text1 = t1.getString(AL.text);
			for (int i2 = i1 + 1; i2 < news.size(); i2++) {
				Thing t2 = (Thing)news.get(i2);
				if (merged.contains(t2))
					continue;
				//TODO:if have variables, compare on variables
				String text2 = t2.getString(AL.text);
				if (text1.equals(text2)) {
					merge(t1,t2);
					merged.add(t2);
					body.debug("Merged "+t1+" and "+t2);
				}
			}
		}
		/*
		//delete what has not merged
		for (Iterator i = merged.iterator(); i.hasNext();) {
			Thing t = (Thing)i.next();
			//TODO: cleanup trusts and news for peers
			if (!t.del()) //TODO: what if not deleted
				body.debug("No delete "+t);
		}
		*/
	}
	
	void merge(Thing merger,Thing mergee) {
		//copy all except trust, new, text, times 
		String all[] = mergee.getNamesAvailable();
		final String special[] = new String[]{AL.text,AL._new,AL.trust,AL.times};
		String names[] = Array.sub(all, special);
		merger.update(mergee, names);
		// union all who trusts to it
		union(merger,mergee,AL.trusts);
		// intersect all to who it is new
		intersect(merger,mergee,AL.news);
		clear(mergee,AL.news);
		clear(mergee,AL.trusts);
		if (!mergee.del())
			body.debug("No delete "+mergee);
	}

	void clear(Thing victim, String name) {
		Set set = body.storager.get(name, victim);
		if (!AL.empty(set))
			for (Iterator i = set.iterator(); i.hasNext();)
				((Thing)i.next()).delThing(name, victim);
	}
	
	void intersect(Thing merger, Thing mergee, String name) {
		Set mergerSet = body.storager.get(name, merger);
		Set mergeeSet = body.storager.get(name, mergee);
		if (AL.empty(mergeeSet)) {
			if (!AL.empty(mergerSet))
				clear(merger,name);
		} else {
			if (!AL.empty(mergerSet))
				for (Iterator i = mergerSet.iterator(); i.hasNext();) {
					Thing peer = (Thing)i.next();
					if (!mergeeSet.contains(peer))
						peer.delThing(name, merger);
				}
		}
	}
	
	void union(Thing merger, Thing mergee, String name) {
		Set mergeeSet = body.storager.get(name, mergee);
		if (!AL.empty(mergeeSet)) {
			//associate all mergee things with merger
			for (Iterator i = mergeeSet.iterator(); i.hasNext();)
				((Thing)i.next()).addThing(name, merger);
		}
	}
	
}
