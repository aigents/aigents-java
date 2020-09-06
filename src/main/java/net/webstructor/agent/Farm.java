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
import net.webstructor.comm.RSSer;
import net.webstructor.comm.SocialCacher;
import net.webstructor.comm.CmdLiner;
import net.webstructor.comm.Crawler;
import net.webstructor.comm.Telegrammer;
import net.webstructor.comm.discourse.Discourse;
import net.webstructor.comm.eth.Ethereum;
import net.webstructor.comm.fb.FB;
import net.webstructor.comm.fb.Messenger;
import net.webstructor.comm.goog.GApi;
import net.webstructor.comm.paypal.PayPal;
import net.webstructor.comm.reddit.Reddit;
import net.webstructor.comm.steemit.Steemit;
import net.webstructor.comm.telegram.Telegram;
import net.webstructor.comm.twitter.Twitter;
import net.webstructor.comm.vk.VK;
import net.webstructor.core.Anything;
import net.webstructor.core.Thing;
import net.webstructor.core.Scheduler;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.ReputationSystem;
import net.webstructor.peer.Conversationer;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Profiler;
import net.webstructor.serp.Serper;
import net.webstructor.self.Selfer;
import net.webstructor.self.WebCrawler;
import net.webstructor.self.Aigents;
import net.webstructor.util.Array;

//TODO: make it extending Cell and let Cell be re-used by mobile clients
//This is simple command-line runner 
public class Farm extends Body {

	Selfer selfer = null;
	Scheduler peerThinker = new PeerThinker();
	
	//TODO:configuration on-line
	private boolean email;
	private boolean web;
	private boolean telnet;
	private boolean social;

	public Farm(String[] args,boolean logger,boolean console,boolean email,boolean web,boolean telnet,boolean social,int conversationers) {
		super(logger,conversationers);
		
		String argstring = Array.toString(args);
		
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
	
	/**
	 * Main methond to start the Aigents body - override it if you extend Body or Farm and call super.start() if needed. 
	 */
	public void start() {
		super.start();
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
		
		if (!AL.empty(self().getString(telegram_token)))
			new Telegrammer(this).start();//this is polling, so need to start it
		if (!AL.empty(self().getString(facebook_token)))//TODO: remove this because it is initialized in Listener already!?
			new Messenger(this);//this is hook-based, so no need to to start it
	
		if (social)
			socialize();

		filecacher = new net.webstructor.self.Cacher("pages",this,storager);
		sitecacher = new GraphCacher("www", this);
		this.register("update", getPublisher().getUpdater());
		
		for (Serper s : Serper.getDefaultSerpers(this))
			searchers.put(s.name(), s);
		
		/*for (Intenter i : Responser.getDefaultIntenters())
			intenters.put(i.name(), i);*/
	}

	//TODO: have this done in fresh new Farm class diverged from old Farm class renamed to Cell 
	//TODO: this in other "factory pool" place, changeable online?
	protected void socialize() {
		crawlers.put(name(), new Aigents(this));
		
		String tg_id = self().getString(telegram_token);
		if (!AL.empty(tg_id))
			crawlers.put("telegram", new Telegram(this));
		
		String fb_id = self().getString(facebook_id);
		String fb_key = self().getString(facebook_key);
		if (!AL.empty(fb_id) && !AL.empty(fb_key))
			crawlers.put("facebook", new FB(this,fb_id,fb_key));
		
		String goog_id = self().getString(google_id);
		String goog_key = self().getString(google_key);
		if (!AL.empty(goog_id) && !AL.empty(goog_key))
			crawlers.put("google", new GApi(this,goog_id,goog_key));
		else
			crawlers.put("google", new GApi(this,null,null));//fake instance for testing
		
		String vk_id = self().getString(vkontakte_id);
		String vk_key = self().getString(vkontakte_key);
		if (!AL.empty(vk_id) && !AL.empty(vk_key))
			crawlers.put("vkontakte", new VK(this,vk_id,vk_key));
		
		//TODO: merge Reddit+Redditer and FB+Messenger? 
		String r_id = self().getString(reddit_id);
		String r_key = self().getString(reddit_key);
		if (!AL.empty(r_id) && !AL.empty(r_key))
			crawlers.put("reddit", new Reddit(this,r_id,r_key));

		String t_key = self().getString(twitter_key);
		String t_secret = self().getString(twitter_key_secret);
		if (!AL.empty(t_key) && !AL.empty(t_secret))
			crawlers.put("twitter", new Twitter(this,t_key,t_secret));
		
		//TODO: make it possible to have many Discourse instances per farm 
		String d_id = self().getString(discourse_id);
		String d_key = self().getString(discourse_key);
		String d_url = self().getString(discourse_url);
		if (!AL.empty(d_url))// && !AL.empty(d_id) && !AL.empty(d_key))//TODO:? force key-based discourse authentication?
			crawlers.put("discourse", new Discourse(this,"discourse",d_url,d_id,d_key));
		
		//TODO: merge PayPal+PayPaler? 
		String p_id = self().getString(paypal_id);
		String p_key = self().getString(paypal_key);
		if (!AL.empty(p_id) && !AL.empty(p_key))
			crawlers.put("paypal", new PayPal(this,p_id,p_key));
		
		String st_url = self().getString(steemit_url);
		if (!AL.empty(st_url))
			crawlers.put("steemit", new Steemit(this,"steemit",st_url));
		
		String go_url = self().getString(golos_url);
		if (!AL.empty(go_url))//Golos.io is clone/fork of Steemit
			crawlers.put("golos", new Steemit(this,"golos",go_url));

		String eth_url = self().getString(ethereum_url);
		String eth_key = self().getString(ethereum_key);
		if (!AL.empty(eth_url) && !AL.empty(eth_key))
			crawlers.put("ethereum", new Ethereum(this, "ethereum", eth_url, eth_key));
	
		for (Serper s : Serper.getDefaultSerpers(this))//register search engines as crawlers
			crawlers.put(s.name(), s);
		
		crawlers.put("rss",new RSSer(this));
		crawlers.put("www",new WebCrawler(this));//should be the last in the list!?
	}
	
	public boolean act(String name, Anything argument) {
		if ("read".equalsIgnoreCase(name) && selfer != null)
			return selfer.spider((Thing)argument);
		if ("profile".equalsIgnoreCase(name) && selfer != null) {
			Collection peers;
			if (argument != null && argument instanceof Thing && (peers = ((Thing)argument).getThings("peers")) !=null) {
				String network = argument.getString("network");
				for (Object peer : peers)
					updateStatus((Thing)peer,network);//update individual peer asynchronously
				return true;
			}
			//TODO: profile ALL peers for specified "network" ONLY
			return selfer.profile();
		}
		if ("reputation update".equalsIgnoreCase(name)) {
			String network;
			if (argument != null && argument instanceof Thing && (network = ((Thing)argument).getString("network")) !=null)
				return updateReputation(network);
			else
				return updateReputation();
		}
		return false;
	}
	
	public void save() {
		if (selfer != null)
			selfer.save(System.currentTimeMillis());
	}
	
	class PeerThinker extends Scheduler {
		@Override
		protected void run() {
			//re-think every active user - after spidering their news and social networks
			long start = System.currentTimeMillis();
			debug("Thinking peers start "+new Date(start)+".");
			try {
				Collection peers;
				peers = (Collection)storager.getByName(AL.is,Schema.peer);
				if (!AL.empty(peers)){
					Date since = Time.today(-attentionDays());//was Time.today(0);
					peers = new ArrayList(peers);
					for (Iterator it = peers.iterator(); it.hasNext();){
						Thing peer = (Thing)it.next();
						Date activityTime = (Date)peer.get(Peer.activity_time);
						if (activityTime != null && activityTime.compareTo(since) >= 0){
							Peer.rethink(Farm.this,peer);//this does think along the way!
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

	public boolean updateReputation() {
		String network = self().getString(reputation_system);
		boolean res = updateReputation(network);
		// update all social cachers which have reputationers, except the default one updated right above 
		for (Crawler s : this.getCrawlers()) if (s instanceof SocialCacher) {
			String name = s.name();
			if (!name.equals(network)) {

//TODO make MEMORY_THRESHOLD parameter of body/self
//TODO built-into cacheholder?
				int memory = checkMemory();
				if (checkMemory() > GraphCacher.MEMORY_THRESHOLD) {
					cacheholder.free();
					debug("Selfer free, memory "+memory+" to "+checkMemory());
				}

				updateReputation(name);
			}
		}
		return res;
	}
	
	public boolean updateReputation(String network) {
		ReputationSystem r = !AL.empty(network) ? SocialCacher.getReputationSystem(this,network) : null;
		if (r != null) {
			long start_time = System.currentTimeMillis();
			debug("Reputation crawling start "+network+" "+new Date(start_time)+".");
			String p[] = new String[] {
					"conservatism", this.getSelf().getString(Body.reputation_conservatism,"0.9"),
					"decayed", this.getSelf().getString(Body.reputation_decayed,"0.5"),
					"default", this.getSelf().getString(Body.reputation_default,"0.5")};
			r.set_parameters(p); 
			Date last_day = Time.today(-1);
			int rs = r.get_ranks(last_day, null, null, null, false, 0, 0, null);
			if (rs != 0) {
				// if not present, go back in time till retention period to find the last day when the ranks were present 
				Date since = last_day;
				int period = self().getInt(Body.attention_period, 31);//TODO: rewtnetion period!?
				int days;
				for (days = 1; days <= period; days++){
					since = Time.addDays(last_day, -days);
					rs = r.get_ranks(since, null, null, null, false, 0, 0, null);
					if (rs == 0)
						break;
				}
				// go from the last day till today and update ranks incrementally
				for (Date date = Time.addDays(since,1); date.compareTo(last_day) <= 0; date = Time.addDays(date, 1)){
					debug("Reputation crawling updating "+network+" "+date);
					rs = r.update_ranks(date, null);
					debug("Reputation crawling updated "+network+" "+date+" result "+rs);
				}
			}
			ArrayList results = new ArrayList();
			rs = r.get_ranks(last_day, null, null, null, false, 0, 0, results);
			if (rs != 0)
				debug("Reputation crawling update "+network+" failed "+rs);
			// set reputations to peers
			String name_id = network + " id";
			for (int i = 0; i < results.size(); i++) {
				Object[] item = (Object[]) results.get(i);
				try {
					Collection c;
					c = this.storager.getByName(name_id, item[0]);
					if (c != null && c.size() == 1) {
						((Thing)c.iterator().next()).set(AL.reputation,((Number)item[1]).toString());
					}
				} catch (Exception e) {
					error("Reputation crawling update "+network+" peer",e);
				}
			}
			long end_time = System.currentTimeMillis();
			debug("Reputation crawling stop "+network+" "+new Date(end_time)+", took "+new Period(end_time-start_time).toHours()+".");
			return true;
		}
		return false;
	}
	
	public void updateStatusRarely() {
		for (Crawler s : crawlers.values()) if (s instanceof Socializer) {
			Socializer feeder = (Socializer)s;
			feeder.forget();
			feeder.resync(0);
		}
		updateReputation();
		//update all user profiles on user-specific basis
		try {
			long start_time = System.currentTimeMillis();
			debug("Peers crawling start "+new Date(start_time)+".");
			//TODO: to other place, separate "socializer" class?
			Collection peers = (Collection)storager.getByName(AL.is,Schema.peer);
			if (!AL.empty(peers)){
				Date since = Time.today(-attentionDays());
				peers = new ArrayList(peers);
				int i = 0;
				for (Iterator it = peers.iterator(); it.hasNext();){
					Thing peer = (Thing)it.next();
					Date activityTime = (Date)peer.get(Peer.activity_time);
					if (activityTime != null && activityTime.compareTo(since) >= 0){
						//TODO: this is separate "socializer" class
			    		long start = System.currentTimeMillis();
			    		debug("Peer crawling start "+peer.getString(AL.email)+" "+i+"/"+peers.size()+".");
						updateStatus(peer,profilers(peer,null),false);//may be not fresh because of cleanup up front
			    		long end = System.currentTimeMillis();
			    		debug("Peer crawling stop "+peer.getString(AL.email)+" "+i+"/"+peers.size()+", took "+new Period(end-start).toHours()+".");
					} else 
						debug("Peer crawling skip "+peer.getString(AL.email)+" "+i+"/"+peers.size()+".");
					i++;
				}
			}
			long end_time = System.currentTimeMillis();
			debug("Peers crawling stop  "+new Date(end_time)+", took "+new Period(end_time-start_time).toHours()+".");
		} catch (Exception e) {
			error("Peers crawling update error ",e);
		}
	}
	
	private Profiler[] profilers(Thing peer,String network) {
		ArrayList<Profiler> profilers = new ArrayList(crawlers.size()); 
		for (Crawler s : crawlers.values()) {
			if (!AL.empty(network) && !s.name().equalsIgnoreCase(network))
				continue;
			Profiler p = s.getProfiler(peer);
			if (p != null)
				profilers.add(p);
		}
		return profilers.toArray(new Profiler[] {});
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
					debug("Peer crawling start "+profiler.provider().name()+" "+peer.getString(AL.email)+".");
    	    		profiler.profile(peer,fresh);
    	    		long end = System.currentTimeMillis();
					debug("Peer crawling stop "+profiler.provider().name()+" "+peer.getString(AL.email)+", took "+new Period(end-start).toHours()+".");
    	    	}
    	    } catch (Exception e) {
    	    	error("Peer crawling error "+profiler.provider().name()+" "+peer.getString(AL.email), e);
    	   	}
    	}
	}
	
	@Override
	public void updateStatus(final Thing peer,String network) {
		//TODO: do this all in better and specific place
		//TODO: do this in the same thread pooling framework as spidering etc.
		//TODO: the same but in better way
		//TODO: handle exceptions consistently
		//TODO: do this for all profilers possible (FB, VK, G+, Twitter, etc.)
		final Profiler[] profilers = profilers(peer,network);
		if (profilers != null) {
			Timer timer = new Timer();
			final Runnable task = new TimerTask() {
			    public void run() {
			    	try {
				    	updateStatus(peer, profilers, true);//have to be fresh
				    	//thinker.think(peer);
						Peer.rethink(Farm.this,peer);//this does think along the way!
			    	} catch (Throwable e) {
			    		error("Farm updateStatus "+peer.getTitle(Peer.title),e);
			    	}
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