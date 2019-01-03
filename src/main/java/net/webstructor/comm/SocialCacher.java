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
package net.webstructor.comm;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.data.GraphCacher;

public abstract class SocialCacher extends Socializer {
	protected String url;
	protected String name; //kind of Ethereum or Steemit/Golos? 
	protected int period;

	//TODO: make private for better locking/guarding
	protected GraphCacher cacher;
	//private GraphCacher cacher;
	private ReentrantLock busy = new ReentrantLock();
	
	public SocialCacher(Body body,String name,String url)
	{
		super(body);
		cacher = new GraphCacher(name,body);
		this.name = name;
		this.url = body != null ? body.self().getString(name+" url",url) : url;
		this.period = body != null ? new Period(body.self().getString(name+" period","4"),Period.DAY).getDays() : 4;
	}

	public String getUrl(){
		return url;
	}
	
	public int getPeriod(){
		return period;
	}
	
	public String getName(){
		return name;
	}
	
	//TODO:@Override
	public String provider(){
		return name;
	}
	
	//TODO:@Override
	public boolean opendata() {
		return true;
	}
	
	public void updateGraph(Date date, Graph graph, long age){
		cacher.updateGraph(date, graph, age);
	}

	//TODO:@Override
	//TODO:synchronize!?
	//TODO:eliminate in favor of getGraphCacher
	public Graph getGraph(Date date){
		return cacher.getGraph(date);
	}

	public GraphCacher getGraphCacher(){
		return cacher;
	}

	//API method forcing forgetting
	//TODO:@Override
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
	//TODO:@Override
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
	
}
