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
package net.webstructor.comm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.data.DataLogger;
import net.webstructor.data.Graph;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.ReputationSystem;
import net.webstructor.data.Transcoder;
import net.webstructor.peer.Grouper;
import net.webstructor.peer.Reputationer;

public abstract class SocialCacher extends Socializer {
	protected String url;
	protected String name; //kind of Ethereum or Steemit/Golos? 
	protected int period;

	//TODO: make private for better locking/guarding
	protected GraphCacher cacher;
	private ReentrantLock busy = new ReentrantLock();
	
	public SocialCacher(Body body,String name,String url)
	{
		super(body);
		cacher = new GraphCacher(name,body);
		this.name = name;
		this.url = url;
		this.period = super.getPeriod();
		if (body != null){
			this.url = body.self().getString(name+" url",this.url);
			this.period = new Period(body.self().getString(name+" period",String.valueOf(this.period)),Period.DAY).getDays();
		}
	}

	public String getUrl(){
		return url.endsWith("/") ? url.substring(0, url.length()-1) : url;
	}
	
	@Override
	public int getPeriod(){
		return period;
	}
	
	@Override
	public String name(){
		return name;
	}
	
	@Override
	public boolean opendata() {
		return true;
	}
	
	public void updateGraph(Date date, Graph graph, long age){
		cacher.updateGraph(date, graph, age);
	}

	//TODO:synchronize!?
	//TODO:eliminate in favor of getGraphCacher
	@Override
	public Graph getGraph(Date date){
		return cacher.getGraph(date);
	}

	public GraphCacher getGraphCacher(){
		return cacher;
	}

	//API method forcing forgetting
	@Override
	public void forget() {
		super.forget();
		try {
			if (busy.tryLock(10L, TimeUnit.SECONDS)) {
				cacher.clear(false);//don't touch LTM
			    busy.unlock();
			} else
				body.debug(Writer.capitalize(name)+" forgetting skip");
		} catch (Exception e) {
			body.error(Writer.capitalize(name)+" crawling resync error", e);
		}		
	}
	
	//virtual overriddable
	protected abstract void updateGraphs(long block, Date since, Date until);

	//API method forcing resync
	//virtual, applies for blockchain-s only
	@Override
	public void resync(long block) {
		//TODO: enable but ensure no syncing conflict is experienced
		try {
			if (busy.tryLock(10L, TimeUnit.SECONDS)) {
				updateGraphs(block, Time.today(-period),Time.today(+1));
			    busy.unlock();
			} else
				body.debug("Ethereum crawling skip");
		} catch (Exception e) {
			body.error("Ethereum crawling resync error", e);
		}		
	}
	
	public void alert(Date time, long block, String type, String from, String to, String value, String source){
		String key = name+" id";
		Collection froms = body.storager.get(new Thing(key,from));
		Collection tos = body.storager.get(new Thing(key,to));
		if (!AL.empty(froms) || !AL.empty(tos)){
			Thing alert = new Thing();
			//alert.set(AL.text, from + (value == null ? " calls " : " pays "+value+" to ")+to);
			alert.set(AL.text, from + " " + type + " " + (value == null ? "" : value + " to ") + to);
			alert.set(AL.times, Time.date(time));
			Collection alls = body.storager.get(alert);
			if (!AL.empty(alls))
				alert = (Thing)alls.iterator().next();
			else {
				alert.store(body.storager);
				alert.addThing(AL.is, body.storager.getThing(name));
				alert.addThing(AL.sources, body.storager.getThing(source));
			}
			if (!AL.empty(froms))
			for (Iterator it = froms.iterator(); it.hasNext();){
				Thing p = (Thing)it.next();
				if (!p.hasThing(AL.news, alert))
					p.addThing(AL.news, alert);
			}
			if (!AL.empty(tos))
			for (Iterator it = tos.iterator(); it.hasNext();){
				Thing p = (Thing)it.next();
				if (!p.hasThing(AL.news, alert))
					p.addThing(AL.news, alert);
			}
		}
	}

	@Override
	public Object[][] getReputation(String user_id, Date since, Date until){
		//TODO SocialCacher.getReputationer!!!!
		//TODO use local stater!!!!
//TODO MAKE sure the same Reputationer is re-used in multiple requests
		ReputationSystem r = getReputationSystem(body, name());
		ArrayList data = new ArrayList();
//TODO average reputation instead of the latest one!?
		for (Date date = Time.date(until); since.compareTo(date) <= 0; date = Time.addDays(date,-1)) {
			int res = r.get_ranks(date, null,null,null,false,0,0, data);
			if (res == 0 && data.size() > 0)
				break;
		}
		//list all peers across all my communtities
		Set<String> community = this instanceof Grouper ? ((Grouper)this).getGroup(user_id) : null;
		Transcoder transcoder = this instanceof Transcoder ? (Transcoder)this : null;
		if (data.size() > 0) {
			ArrayList norm = new ArrayList();
			for (int i = 0; i < data.size(); i++){
				Object item[] = (Object[])data.get(i);
				Object id = item[0];
				if (community != null && !community.contains(id))//don't restrict to community if no group defined
					continue;
				if (transcoder != null)
					id = transcoder.transcode(id);
				norm.add( new Object[]{new Integer(((Number)item[1]).intValue()),id} );
			}
			return (Object[][])norm.toArray(new Object[][]{});
		}
		return null;
	}

	@Override
	public Graph getGraph(String user_id, Date since, Date until){
		if (AL.empty(user_id))
			return null;
//TODO: configure limits and pass them as parameters
		int range = 1;
		int threshold = 0; 
		int limit = 1000;
		int period = Period.daysdiff(since, until);
		Set<String> community = this instanceof Grouper ? ((Grouper)this).getGroup(user_id) : null;
		GraphCacher grapher = getGraphCacher();
		return grapher.getSubgraph(new String[] {user_id}, until, period, range, threshold, limit, null, community, Socializer.links);
	}
	
	public static void write(DataLogger logger, String name, Date time, long block, String type, String from, String to, String value, String unit, String child, String parent, String title, String input, String tags, String format){
		//network,timestamp,from,to,value,unit,type,input,title,parent,child,tags,format
		logger.write(name+"/"+name+"_"+Time.day(time,false)+".tsv",
				new Object[]{name,Time.linux(time),type,from,to,value,unit,child,parent,title,input,tags,format,new Long(block)});
	}
	
	//TODO replace this with Farm.getReputationer(network) for the purpose of Body-controlled memory management!?
	public static ReputationSystem getReputationSystem(Environment env, String network){
		synchronized (Reputationer.class) {
			ReputationSystem r = Reputationer.get(network);
			if (r == null) {
				Socializer grpaher = env instanceof Body ? ((Body)env).getSocializer(network) : null;
				if (grpaher != null && grpaher instanceof SocialCacher)
					r = new Reputationer(env,network,null,((SocialCacher)grpaher).getGraphCacher());
				else
					r = new Reputationer(env,network,null,true);
				Reputationer.add(network, r);
			}
			return r;
		}
	}
	
}
