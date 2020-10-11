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
package net.webstructor.comm.telegram;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.comm.SocialCacher;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.data.Transcoder;
import net.webstructor.data.DataLogger;
import net.webstructor.data.Graph;
import net.webstructor.data.LangPack;
import net.webstructor.data.Linker;
import net.webstructor.peer.Grouper;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Profiler;

class TelegramFeeder extends SocialFeeder {
	Telegram api;
	public TelegramFeeder(Environment body, Telegram api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		this.api = api;
	}
	public void getFeed(Date since, Date until, StringBuilder detail) throws IOException {
		body.debug("Telegram crawling graph for "+user_id);
		
		for (Date date = until; date.compareTo(since) >= 0; date = Time.date(date,-1)){
			body.debug("Telegram crawling graph "+user_id+" at "+date+", memory "+body.checkMemory());
			Graph graph = api.getGraph(date);
			if (graph == null)
				continue;//skip unknown dates
			
			//TODO: calculate similarity based on comments/mentions correspondents AND texts 
			
//TODO: optimize use of api.userName(key)!?
			synchronized (graph) {
				Linker mentioned = graph.getLinker(user_id, "mentioned", false);
				Linker mentions = graph.getLinker(user_id, "mentions", false);
				Linker commented = graph.getLinker(user_id, "commented", false);
				Linker comments = graph.getLinker(user_id, "comments", false);
				if (mentioned != null)
					for (Iterator it = mentioned.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int amount = mentioned.value(key).intValue();
						countLikes(key,api.userName(key),date,amount);
						countPeriod(date,amount,0);
					}
				if (commented != null)
					for (Iterator it = commented.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int amount = commented.value(key).intValue();
						countComments(key,api.userName(key),null,date,amount);
						countPeriod(date,0,amount);
					}
//TODO: split my mentions and my comments
				if (mentions != null)
					for (Iterator it = mentions.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int v = mentions.value(key).intValue();//TODO: this properly, expected payments can not be zero
						countMyLikes(key,api.userName(key),v > 0 ? v : 1);
					}
				if (comments != null)
					for (Iterator it = comments.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int v = comments.value(key).intValue();//TODO: this properly, expected values of smart contract calls are always zero
						countMyLikes(key,api.userName(key),v > 0 ? v : 1);
					}
			}
		}
		body.debug("Telegram crawling graph completed, memory "+body.checkMemory());
	}
	
}


public class Telegram extends SocialCacher implements Transcoder, Grouper {
	protected DataLogger logger;
	protected boolean debug = true;
	
	Graph graph = null;
	Date date = null;
	
	public Telegram(Body body) {
		super(body,"telegram",null);
		logger = new DataLogger(body,Writer.capitalize(name)+" crawling");//TODO: do we need data logger for Telegram?
	}

	public DataLogger getLogger() {
		return logger;
	}
	
	@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		TelegramFeeder feeder = new TelegramFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(since, until, new StringBuilder());
		return feeder;
	}
	
	@Override
	protected void updateGraphs(long block, Date since, Date until){
		//no need to do anything, because Telegram graph is updated on the fly  
	}

	@Override
	public Profiler getProfiler(Thing peer) {
//TODO: graph-based profiler
		return null;
	}

	//TODO: make this protected once Telegrammer is moced to net.webstructor.comm.telegram
	public void save() {
		synchronized (this) {
			if (graph != null)
				updateGraph(this.date, graph, System.currentTimeMillis());
		}
	}

	protected void updateInteraction(Date date, String group_id, String message_id, String from_id, String reply_to_from_id, java.util.Set<String> mention_ids, String text) {
		//mentions
		//comments
		int logvalue = 1 + (AL.empty(text) ? 0 : (int)Math.round(Math.log10(text.length())));
		if (!AL.empty(reply_to_from_id))
			updateInteraction(date,"comments",from_id,reply_to_from_id,logvalue);//update from->reply_to_from
		if (mention_ids != null) for (String mention_id : mention_ids) {
			if (!AL.empty(mention_id))
				updateInteraction(date,"mentions",from_id,mention_id,logvalue);// update from->mentions
		}
		/*//TODO abstract graph DB layer: time + chat_id + message_id -> type -> value
		String id = group_id + ":" + message_id; 
		ArrayList<Object[]> links = new ArrayList<Object[]>();
		//links.add(new Object[] {from_id,id,"authors"});//author authors messages
		links.add(new Object[] {id,from_id,"authored"});//message authored by author
		links.add(new Object[] {id,text,"text"});
		links.add(new Object[] {group_id,id,"groups"});//group groups messages
		//links.add(new Object[] {id,group_id,"grouped"});//messages grouped by group
		//TODO sources
		//TODO: comments - reply_to
		//TODO: mentions
		//TODO times
		updateGraph(date,links,1);*/
	}

	/*private void updateGraph(Date date, ArrayList<Object[]> links, int value) {
		Date day = Time.date(date);
		synchronized (this) {
			if (this.date != day) {
				if (graph != null)
					updateGraph(this.date, graph, System.currentTimeMillis());
				graph = getGraph(day);
				this.date = day;
//TODO: auto-save pending graphs on exit!? 
			}
			for (Object[] link : links)
				graph.addValue(link[0], link[1], link[2], value);
		}
	}*/
	
	//TODO: move to Mediator/SocialCacher to handle all messengers
	private void updateInteraction(Date date, String type, String from_id, String to_id, int value) {
		if (debug)
			body.debug("Telegram crawling "+from_id+" "+type+" "+to_id);

		if (logger != null)
			SocialCacher.write(logger, name, date, date.getTime(), type, from_id, to_id, String.valueOf(value), null, null, null, null, null, null, null);
//TODO: alert on comments and mentions in Telegram?
		//alert(date,0,"comments",from_id,to_id,"1",getUrl());
		
		Date day = Time.date(date);
		synchronized (this) {
			if (this.date != day) {
				if (graph != null)
					updateGraph(this.date, graph, System.currentTimeMillis());
				graph = getGraph(day);
				this.date = day;
//TODO: auto-save pending graphs on exit!? 
			}
			String reverse = reverse(type);
			if (!AL.empty(type))
				graph.addValue(from_id,to_id, type, value);//out
			if (!AL.empty(reverse))
				graph.addValue(to_id,from_id, reverse, value);//in
		}
	}

	String userName(String id) {
		String name = null;
		try {
			Collection peers = body.storager.getByName(Body.telegram_id, id);
			if (peers != null) for (Object peer : peers)
				name = ((Thing)peer).getTitle(Peer.title);
		} catch (Exception e) {}
		return !AL.empty(name) ? name : transcode(id).toString();
	}
	
	@Override
	public Set<String> getGroup(String user_id) {
		//get all groups of id and get all mambers of all these groups
		HashSet<String> res = new HashSet<String>();
		try {
			Collection users = body.storager.getByName(Body.telegram_id, user_id);
			if (!AL.empty(users)) for (Object user : users) {
				Collection groups = ((Thing)user).getThings(AL.groups);
				if (!AL.empty(groups)) for (Object group : groups) {
					Collection members = ((Thing)group).getThings(AL.members);
					if (!AL.empty(members)) for (Object member : members) {
						String peer_id = ((Thing)member).getString(Body.telegram_id);
						if (!AL.empty(peer_id))
							res.add(peer_id);
					}
				}
			}
		} catch (Exception e) {
			body.error("Telegram crawling group for "+user_id,e);
		}
		return res;
	}

	@Override
	public Object transcode(Object source) {
		Set res = body.storager.get(Body.telegram_id, source);
		Object out = !AL.empty(res) ? ((Thing)res.iterator().next()).get(Body.telegram_name) : null;
		return out != null ? out : source;
	}
	
	@Override
	public Object recovercode(Object source) {
		Set res = body.storager.get(Body.telegram_name, source);
		Object out = !AL.empty(res) ? ((Thing)res.iterator().next()).get(Body.telegram_id) : null;
		return out != null ? out : source;
	}
	
}//class

