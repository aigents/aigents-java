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
package net.webstructor.agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.comm.Emailer;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.Socializer;
import net.webstructor.comm.TCPListener;
import net.webstructor.comm.HTTPListener;
import net.webstructor.comm.CmdLiner;
import net.webstructor.comm.eth.Ethereum;
import net.webstructor.comm.fb.FB;
import net.webstructor.comm.goog.GApi;
import net.webstructor.comm.steemit.Steemit;
import net.webstructor.comm.vk.VK;
import net.webstructor.core.Anything;
import net.webstructor.core.Thing;
import net.webstructor.data.GraphCacher;
import net.webstructor.peer.Conversationer;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Profiler;
import net.webstructor.self.Selfer;
import net.webstructor.util.Array;

//This is simple command-line runner 
public class Farm extends Body {

	Selfer selfer = null;
	Updater peerThinker = new PeerThinker();
	
	//TODO:configuration on-line
	private boolean email;
	private boolean web;
	private boolean telnet;
	private boolean social;
	
	public Farm(String[] args,boolean logger,boolean console,boolean email,boolean web,boolean telnet,boolean social,int conversationers) {
		super(logger,conversationers);
		
		String argstring = Array.toString(args);
		
		/*if (args != null){
			for (int i = 0; i < args.length; i++)
				System.out.println("arg "+i+":"+args[i]);
			System.out.println("args:"+argstring);
		}*/
		
		//check custom options override
		Thing opts = new Thing();
		Reader.read(argstring,Reader.pattern(null,opts,new String[]{"console"}));
		//TODO: standardize
		if (Array.contains(new String[]{"false","off","no","not","none"}, opts.getString("console")))
			console = false;
		
		this.console = console;
		this.email = email;
		this.web = web;
		this.telnet = telnet;
		this.social = social;

		Reader.read(argstring,Reader.pattern(null,self(),properties));
		reply(Writer.toPrefixedString(null,new Seq(new Object[]{self()}),self()));
	}
	
	public void start() {
		//load from the path happens in Selfer constructor
		selfer = new Selfer(this);
		selfer.start();
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    public void run() {
                        save();
                    }
                }
            );
		
        HTTP.init();
        
		//TODO: start or stop communicators based on config
        if (telnet)
        	new TCPListener(this).start();
        if (web)
        	new HTTPListener(this).start();		
		if (email)
			new Emailer(this).start();	
		if (console)
			new CmdLiner(this).start();
		if (social) {
			//TODO: this in other place, changeable online?
			String fb_id = self().getString(facebook_id);
			String fb_key = self().getString(facebook_key);
			if (!AL.empty(fb_id) && !AL.empty(fb_key))
				fb = new FB(this,fb_id,fb_key);
			
			String goog_id = self().getString(google_id);
			String goog_key = self().getString(google_key);
			if (!AL.empty(goog_id) && !AL.empty(goog_key))
				gapi = new GApi(this,goog_id,goog_key);
			else
				gapi = new GApi(this,null,null);//fake instance for testing
			
			String vk_id = self().getString(vkontakte_id);
			String vk_key = self().getString(vkontakte_key);
			if (!AL.empty(vk_id) && !AL.empty(vk_key))
				vk = new VK(this,vk_id,vk_key);
			
			String st_url = self().getString(steemit_url);
			if (!AL.empty(st_url))
				steemit = new Steemit(this,"steemit",st_url);
			
			String go_url = self().getString(golos_url);
			if (!AL.empty(go_url))//Golos.io is clone/fork of Steemit
				golos = new Steemit(this,"golos",go_url);

			String eth_url = self().getString(ethereum_url);
			String eth_key = self().getString(ethereum_key);
			if (!AL.empty(eth_url) && !AL.empty(eth_key))
				ethereum = new Ethereum(this, "ethereum", eth_url, eth_key);
		}
		sitecacher = new GraphCacher("www", this);
	}

	public boolean act(String name, Anything argument) {
		if ("read".equalsIgnoreCase(name) && selfer != null)
			return selfer.spider((Thing)argument);
		if ("profile".equalsIgnoreCase(name) && selfer != null)
			return selfer.profile();
		return false;
	}
	
	public void save() {
		if (selfer != null)
			selfer.save(System.currentTimeMillis());
	}
	
	//TODO: make the Updater generic class for all Spidereres, Saver, etc.
	abstract class Updater {
		long period = Period.HOUR;
		long scheduled = 0;
		void setPeriod(long period){
			this.period = period;
		}
		//TODO: make synchronized
		void schedule(){
			scheduled = 0;
		}
		abstract protected void update();
		void check(boolean force) {
			long time = System.currentTimeMillis();
			if (force || scheduled < time){
				update();
				scheduled = (scheduled == 0 ? time : scheduled) + period;
			}
		}
	}
	
	class PeerThinker extends Updater {
		protected void update() {
			//re-think every active user - after spidering their news and social networks
			long start = System.currentTimeMillis();
			debug("Thinking peers start "+new Date(start)+".");
			try {
				Collection peers;
				peers = (Collection)storager.getByName(AL.is,Schema.peer);
				if (!AL.empty(peers)){
					Date since = Time.today(-retentionDays());//was Time.today(0);
					peers = new ArrayList(peers);
					for (Iterator it = peers.iterator(); it.hasNext();){
						Thing peer = (Thing)it.next();
						Date activityTime = (Date)peer.get(Peer.activity_time);
						if (activityTime != null && activityTime.compareTo(since) >= 0){
							Peer.trashPeerNews(Farm.this,peer);//this does think along the way!
						}
					}
				}
			} catch (Exception e) {
				error("Thinking peers error ",e);
			}
			long end = System.currentTimeMillis();
			debug("Thinking peers end "+new Date(end)+", took "+new Period(end-start).toHours()+".");
		}
	}
	
	public void updateStatus(boolean now) {
		//TODO: may have to evaluate amount of memory and connected users to display
		//TODO: maybe move out of attention focus what is not done in Self.clear
		peerThinker.check(now);
	}

	public void updateStatusRarely() {
		//TODO forget more systematically and in more uniform way?
		Socializer feeders[] = new Socializer[]{ethereum,fb,gapi,vk,steemit,golos};
		for (int i = 0; i < feeders.length; i++)
			if (feeders[i] != null){
				feeders[i].forget();
				feeders[i].resync(0);
			}
		//update all user profiles on user-specific basis
		try {
			//TODO: to other place, separate "socializer" class?
			Collection peers = (Collection)storager.getByName(AL.is,Schema.peer);
			if (!AL.empty(peers)){
				Date since = Time.today(-retentionDays());
				peers = new ArrayList(peers);
				int i = 0;
				for (Iterator it = peers.iterator(); it.hasNext();){
					Thing peer = (Thing)it.next();
					Date activityTime = (Date)peer.get(Peer.activity_time);
					if (activityTime != null && activityTime.compareTo(since) >= 0){
						//TODO: this is separate "socializer" class
						updateStatus(peer,profilers(peer),false);//may be not fresh because of cleanup up front
						debug("Spidering peer "+peer.getString(AL.email)+" "+i+"/"+peers.size()+" completed "+new Date(System.currentTimeMillis())+".");
					} else 
						debug("Spidering peer "+peer.getString(AL.email)+" "+i+"/"+peers.size()+" skipped.");
					i++;
				}
			}
		} catch (Exception e) {
			error("Spidering peers update error ",e);
		}
	}
	
	private Profiler[] profilers(Thing peer) {
		if (fb != null || gapi != null || vk != null) {
			final Profiler fb_profiler = new Profiler(this,fb,peer,Body.facebook_id,Body.facebook_token,Body.facebook_key);
			final Profiler go_profiler = new Profiler(this,gapi,peer,Body.google_id,Body.google_token,Body.google_key);
			final Profiler vk_profiler = new Profiler(this,vk,peer,Body.vkontakte_id,Body.vkontakte_token,Body.google_key);
			Profiler[] profilers = new Profiler[]{fb_profiler,go_profiler,vk_profiler};
			return fb_profiler.applies() || go_profiler.applies() || vk_profiler.applies() ? profilers : null;
		}
		return null;
	}
	
	//TODO: to other place?
	private void updateStatus(final Thing peer, final Profiler[] profilers, boolean fresh) {
		if (profilers == null)
			return;
    	for (int i = 0; i < profilers.length; i++) {
    		Profiler profiler = profilers[i];
    	    try	{
    	    	if (profiler.applies()) {
    	    		long start = System.currentTimeMillis();
					debug("Spidering peer "+profiler.provider().provider()+" start "+peer.getString(AL.email)+" "+new Date(start)+".");
    	    		profiler.profile(peer,fresh);
    	    		long end = System.currentTimeMillis();
					debug("Spidering peer "+profiler.provider().provider()+" end "+peer.getString(AL.email)+" "+new Date(end)+", took "+new Period(end-start).toHours()+".");
    	    	}
    	    } catch (Exception e) {
    	    	error("Spidering peer error "+profiler.provider().provider()+" "+peer.getString(AL.email), e);
    	   	}
    	}
	}
	
	public void updateStatus(final Thing peer) {
		//TODO: do this all in better and specific place
		//TODO: do this in the same thread pooling framework as spidering etc.
		//TODO: the same but in better way
		//TODO: handle exceptions consistently
		//TODO: do this for all profilers possible (FB, VK, G+, Twitter, etc.)
		final Profiler[] profilers = profilers(peer);
		if (profilers != null) {
			Timer timer = new Timer();
			final Runnable task = new TimerTask() {
			    public void run() {
			    	updateStatus(peer, profilers, true);//have to be fresh
			    	//thinker.think(peer);
					Peer.trashPeerNews(Farm.this,peer);//this does think along the way!
			    }
			};
			//TODO: all threads for profiles and spidereres to be run under the same Executor with given number of threads in the pool 
			//((Thread)task).start();
			timer.schedule((TimerTask)task, 5000);//need time for language to update
		}
	}
		
	public static void main(String[] args) {
		new Farm(args,true,true,true,true,true,true,Conversationer.WORKERS).start();//all services
	}	
}