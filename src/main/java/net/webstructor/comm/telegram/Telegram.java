/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents®
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
import java.util.ArrayList;
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
import net.webstructor.comm.Socializer;
import net.webstructor.comm.InteractionItem;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.data.Transcoder;
import net.webstructor.data.DataLogger;
import net.webstructor.data.Graph;
import net.webstructor.data.LangPack;
import net.webstructor.data.Linker;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.peer.Grouper;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Profiler;
import net.webstructor.util.ReportWriter;

class TelegramFeeder extends SocialFeeder {
	Telegram api;
	public TelegramFeeder(Environment body, Telegram api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		this.api = api;
	}
	public void getFeed(Date since, Date until, final StringBuilder detail) throws IOException {
		body.debug("Telegram crawling graph for "+user_id);

		final Set<String> groups = api.getGroupIds(user_id);//get groups of the user
		
		for (Date date = until; date.compareTo(since) >= 0; date = Time.date(date,-1)){
			body.debug("Telegram crawling graph "+user_id+" at "+date+", memory "+body.checkMemory());
			Graph graph = api.getGraph(date);
			if (graph == null)
				continue;//skip unknown dates
			
			//get user groups
			final Set<String> peers = api.getGroupPeerIds(userId());
			if (SocialCacher.load(api.logger, api.name(), date, new DataLogger.StringConsumer() {
				@Override
				public boolean read(String text) {
					InteractionItem ii = new InteractionItem(text);
					if ("comment".equals(ii.type) && !AL.empty(ii.child)) {
						String[] ids = ii.child.split("/");
						if (groups.contains(ids[0])) {
							Date time = new Date(ii.timestamp);
							OrderedStringSet urls = new OrderedStringSet();
							//String url = !AL.empty(ii.title) ? "https://t.me/" + ii.title + "/" + ids[1] : null;
							String url = Telegrammer.getMessageLink(ii.title, null, ids[0], ids[1]);
							if (!AL.empty(url))
								urls.add(url);
							if (user_id.equals(ii.from)) {
								processItem(time,ii.from,ii.input,urls,null,0,false);
								String img = urls.getImage();
								String imghtml = img != null ? ReportWriter.img(url != null ? url : img, img) : null;
								reportDetail(detail,
										ii.from,//getUserName(from),
										url,//uri
										ii.child,//id
										ii.input,
										time,
										null,//comments,
										urls,
										null,//getLikers(permlink),//likers,
										0,// snews_likes[1],//likes_count-user_likes,
										0,//news_likes[0],//user_likes,
										0,//othersComments,
										imghtml);//image HTML
							} else if (peers.contains(ii.from)) {//if user is visible
								countComment(ii.from,api.getPeerName(ii.from),ii.input,time);
							}
						}
					}
					return true;
				}}))
				;//success!
		
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
						countLikes(key,api.getPeerName(key),date,amount);//mention me => like me 
						countPeriod(date,amount,0);
					}
				if (commented != null)
					for (Iterator it = commented.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int amount = commented.value(key).intValue();
//TODO: use only one way to count comments
						countComment(key,api.getPeerName(key),null,date,amount);//reply to me => comment on me
						countPeriod(date,0,amount);
					}
//TODO: split my mentions and my comments - later in report form!
				if (mentions != null)
					for (Iterator it = mentions.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int v = mentions.value(key).intValue();//TODO: this properly, expected payments can not be zero
						countMyLikes(key,api.getPeerName(key),v > 0 ? v : 1);//my mention => my like
					}
				if (comments != null)
					for (Iterator it = comments.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int v = comments.value(key).intValue();//TODO: this properly, expected values of smart contract calls are always zero
						countMyLikes(key,api.getPeerName(key),v > 0 ? v : 1);//my reply => my like
					}
			}
		}
		body.debug("Telegram crawling graph completed, memory "+body.checkMemory());
	}

	@Override
	public Socializer getSocializer() {
		return api;
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
			try {logger.close();} catch (IOException e) {body.error("Telegram crawling logger",e);}//TODO: move it to updateGraphs in SocialCacher
		}
	}

	protected void updateInteraction(Date date, String group_id, String group_name, String group_title, String message_id, String reply_to_message_id, String from_id, String reply_to_from_id, java.util.Set<String> mention_ids, String text, String link) {
		if (debug)
//TODO: debug
			body.debug("Telegram crawling from "+from_id+" to "+reply_to_from_id+" mentions "+mention_ids);

		text = text.replace('\n', ' ');
		
		String to_id = !AL.empty(reply_to_from_id) ? reply_to_from_id : group_id;
		int intvalue = text != null ? text.length() : 1;//empty comment still counts
		String weight = text != null ? String.valueOf(intvalue) : null;
		//https://t.me/agirussia/8855 (public - agirussia, 8855)
		//https://t.me/c/1410910487/75 (private -1001410910487, 75)
		//String url = !AL.empty(group_name) ? "https://t.me/" + group_name + "/" + message_id : null; 
		String permlink = group_id + "/" + message_id;
		String parent_permlink = AL.empty(reply_to_message_id) ? null : group_id + "/" + reply_to_message_id;
		
		//comments
		//mentions
		int logvalue = 1 + (AL.empty(text) ? 0 : (int)Math.round(Math.log10(text.length())));
		if (logger != null)
			SocialCacher.write(logger, name, date, date.getTime(), "comment", from_id, to_id, weight, null, permlink, parent_permlink, group_name/*title*/, text, null, null);
		if (!AL.empty(reply_to_from_id)) {
			updateInteraction(date,"comments",from_id,reply_to_from_id,logvalue);//update from->reply_to_from
		}
		if (mention_ids != null) for (String mention_id : mention_ids) {
			if (logger != null)
				SocialCacher.write(logger, name, date, date.getTime(), "mention", from_id, to_id, weight, null, null, null, null, null, null, null);
			if (!AL.empty(mention_id))
				updateInteraction(date,"mentions",from_id,mention_id,logvalue);// update from->mentions
		}
		
//TODO abstract graph DB layer: time + chat_id + message_id -> type -> value !!!???
		
		ArrayList<String[]> links = new ArrayList<String[]>();
		links.add(new String[] {permlink,text,"text"});
		//"posts"
		links.add(new String[] {group_id,permlink,"posts"});//group-posts->post
		//"replies"
		if (!AL.empty(parent_permlink))
			links.add(new String[] {permlink,parent_permlink,"replies"});
		//"authors"
		//links.add(new String[] {from_id,permlink,"authors"});//peer-authors->post
		links.add(new String[] {permlink,from_id,"authored"});//post-authored(by)->peer
		//"tags"
		if (mention_ids != null) for (String mention_id : mention_ids)
			links.add(new String[] {permlink,mention_id,"tags"});//post-tags->peer
		//"url"
		if (AL.isURL(link))
			links.add(new String[] {permlink,link,"sources"});//post-url->url
		updateGraph(date,links,1);
		
	}

	private void updateGraph(Date date, ArrayList<String[]> links, int value) {
		Date day = Time.date(date);
		synchronized (this) {
			if (this.date != day) {
				if (graph != null)
					updateGraph(this.date, graph, System.currentTimeMillis());
				graph = getGraph(day);
				this.date = day;
//TODO: auto-save pending graphs on exit!? 
			}
			for (String[] link : links) {
				graph.addValue(link[0], link[1], link[2], value);
				String reverse = reverse(link[2]);
				if (!AL.empty(reverse))
					graph.addValue(link[1], link[0], reverse, value);
			}
		}
	}
	
	//TODO: move to Mediator/SocialCacher to handle all messengers
	private void updateInteraction(Date date, String type, String from_id, String to_id, int value) {
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
			if (!AL.empty(type)) {
				graph.addValue(from_id,to_id, type, value);//out
				String reverse = reverse(type);
				if (!AL.empty(reverse))
					graph.addValue(to_id,from_id, reverse, value);//in
			}
		}
	}

	@Override
	public String getPeerName(String id) {
		String name = transcode(id).toString();
		if (!AL.empty(name))
			return name;
		try {
			Collection peers = body.storager.getByName(Body.telegram_id, id);
			if (peers != null) for (Object peer : peers)
				name = ((Thing)peer).getTitle(Peer.title);
		} catch (Exception e) {}
		if (!AL.empty(name))
			return name;
		return super.getPeerName(id);
	}
	
	@Override
	public Set<String> getGroupIds(String user_id) {
		//get all groups of id and get all mambers of all these groups
		HashSet<String> res = new HashSet<String>();
		try {
			Collection users = body.storager.getByName(Body.telegram_id, user_id);
			if (!AL.empty(users)) for (Object user : users) {
				Collection groups = ((Thing)user).getThings(AL.groups);
				if (!AL.empty(groups)) for (Object group : groups) {
					String group_id = ((Thing)group).getString(Body.telegram_id);
					if (!AL.empty(group_id))
						res.add(group_id);
				}
			}
		} catch (Exception e) {
			body.error("Telegram crawling group for "+user_id,e);
		}
		return res;
	}

	@Override
	public Set<String> getGroupPeerIds(String user_id) {
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
			body.error("Telegram crawling group peers for "+user_id,e);
		}
		return res;
	}

	@Override
	public Object transcode(Object source) {
		if (source == null)
			return null;
		Set res = body.storager.get(Body.telegram_id, source);
		Thing t = !AL.empty(res) ? ((Thing)res.iterator().next()) : null;
		if (t == null)
			return source;
//TODO this in recovercode?
		Object out = t.get(source instanceof String && ((String)source).charAt(0) == '-' ? AL.name : Body.telegram_name);//group -> name, peer -> telegram_name
		return out != null ? out : source;
	}
	
	@Override
	public Object recovercode(Object source) {
		Set res = body.storager.get(Body.telegram_name, source);
		Object out = !AL.empty(res) ? ((Thing)res.iterator().next()).get(Body.telegram_id) : null;
		return out != null ? out : source;
	}
	
}//class

