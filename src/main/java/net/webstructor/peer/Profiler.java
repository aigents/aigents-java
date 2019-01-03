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
package net.webstructor.peer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Thing;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;
import net.webstructor.util.Array;

public class Profiler {
	Body body;
	Thing peer;
	Socializer provider;
	SocialFeeder feeder = null;
	String id;
	String token;
	String key;//used for refresh_token for Google+
	int maxKnows;// = Body.MAX_TRUSTED_LINKS;
	int maxSites;// = Body.MAX_TRUSTED_LINKS;
	
	private String[][] feederPeerNames = null;
	private String[] bodyStoragerNames;
	
	public Profiler(Body body,Socializer provider,Thing peer,String id_name,String token_name,String key_name){
		this.provider = provider;
		this.body = body;
		this.peer = peer;
		id = peer.getString(id_name);
		token = peer.getString(token_name);
		key = peer.getString(key_name);
		bodyStoragerNames = body.storager.getNames();
		maxKnows = StringUtil.toIntOrDefault(peer.getString(Peer.trusts_limit),10,Body.PEER_TRUSTS_LIMIT);
		maxSites = StringUtil.toIntOrDefault(peer.getString(Peer.trusts_limit),10,Body.PEER_TRUSTS_LIMIT);
	}

	public Socializer provider(){
		return provider;
	}
	
	public String id(){
		return id;
	}
	
	public String token(){
		return token;
	}
	
	public String key(){
		return key;
	}
	
	public boolean applies(){
		return provider != null && !AL.empty(id) && !AL.empty(token);
	}
	
	public String profile(Thing peer,boolean fresh) throws IOException {
		if (peer == null)
			return null;
			
		String language = peer.getString(Peer.language); 
    	String name = peer.getString(AL.name);
    	String surname = peer.getString(Peer.surname);
    	String report = null;
		if (!AL.empty(id) && (provider.opendata() || !AL.empty(token))){
			//TODO: two different feeders for short-term and long-term?
			//TODO: incremental profiling "re-sync" since last facebook sync time?
			//TODO: retain current sync time?
			//int days = body.retentionDays();
			//year back till tomorrow to account for possible time difference 
			body.debug("Spidering peer "+provider.provider()+" id "+id()+" token "+token+", name "+name+", surname "+surname);
//TODO: include areas of interest
//TODO: use user-specific retrospection period instead of automatically identified one 
			//feeder = provider.getFeeder(id, token, key, Body.RETROSPECTION_PERIOD_DAYS, Body.RETROSPECTION_RETRIES, null);
			HashMap feeds = new HashMap();
			feeder = provider.getFeeder(id, token, key, 0, null, fresh, feeds);//default period, no areas, fresh
			if (feeder != null){
				body.debug("Spidering peer "+provider.provider()+" id "+id()+" news "+feeder.getNewsCount()+", name "+name+", surname "+surname);
				feeder.cluster(Body.MIN_RELEVANT_FEATURE_THRESHOLD_PERCENTS);
				profile();
				//default options
				//TODO: options from peer profile
				report = provider().cacheReport(feeder, feeds, id(), token(), key(), name, surname, language, "html", new HashSet(), 20);
			}
		}
		if (AL.empty(report))
			body.debug("Spidering peer "+provider.provider()+" id "+id()+" failed");
		return report;
	}
	
	public boolean skipWord(String text){
		return text == null || text.length() < 3 || Array.contains(AL.grammar, text)
				|| Array.contains(bodyStoragerNames,text)
				|| Array.contains(feederPeerNames,text);
	}
	
	public String cleanPattern(OrderedStringSet cat){
		if (AL.empty(cat))
			return null;
		ArrayList any = new ArrayList();
		for (int i = 0; i < cat.size(); i++){
			String s = (String)cat.get(i);
			if (!skipWord(s))
				any.add(s);
		}
		return AL.empty(any) ? null : Writer.toString(new Any(any.toArray()));
	}
	
	//TODO: move to Thing ans use in Self.clear? 
	private int getTrustedCount(Thing peer, String name){
		Collection links = peer.getThings(name);
		Collection trusts = peer.getThings(AL.trusts);
		if (!AL.empty(links) && !AL.empty(trusts)){
			links = new HashSet(links);
			links.retainAll(trusts);
			return links.size();
		}
		return 0;
	}
	
	public void profile() throws IOException {		
		int peerSites = getTrustedCount(peer,AL.sites);
		int peerKnows = getTrustedCount(peer,AL.knows);
		
		//TODO: real intervals
		//--- do faceboook profiling since last facebook sync time
		//---- retain current sync time
		if (feeder != null) {
			feederPeerNames = feeder.getPeersNames();
			//---- get all words liked by me
			//----- add words to things, trust if not present  
			//----- TODO: create patterns of words!?
			//TODO: get rid of hardcoded limit!?
			int max = 10;
			int[] thresholds = new int[]{100, 99, 95, 90, 80, 60, 25, 0};
			for (int i = 0; i < thresholds.length; i++){
				//new String[]{"Rank,%","Word","My Likes","Likes","Comments","Posts","Count","(My Likes)/Posts"},
				//TODO: getBestWords vs. getMyWords
				Object[][] data = feeder.getBestWordsLikedAndCommentedByAll(max,thresholds[i]);
				if (!AL.empty(data)){
					for (int j = 0; j < data.length; j++){
						String text = (String)data[j][1];
						Integer mylikes = (Integer)data[j][2];
						if (mylikes.intValue() > 0 && !skipWord(text)){
							peerKnows = updateLinks(AL.knows,text, true, peerKnows, maxKnows);
						}
					}
					break;
				}
			}
			
			//create patterns from categories
			if (feeder.getTextsMiner() != null){
				//TODO:use sorted clusters, best ones - first for possible cutoff! 
				/*
				java.util.Set cats = feeder.getTextsMiner().getCategories();
				if (!AL.empty(cats)){
					for (Iterator it = cats.iterator(); it.hasNext();){
						String text = cleanPattern((OrderedStringSet)it.next());
						if (!AL.empty(text)){
							peerKnows = updateLinks(AL.knows, text, true, peerKnows, maxKnows);
						}
					}
				}*/
				Object[][] bestCats = feeder.getNewsCats();
				if (!AL.empty(bestCats)){
					 for (int i = 0; i < bestCats.length; i++){
						String text = cleanPattern((OrderedStringSet)bestCats[i][0]);
						if (!AL.empty(text))
							peerKnows = updateLinks(AL.knows, text, true, peerKnows, maxKnows);
					 }
				}
			}
					
			//---- get all liked posts
			//----- add links to sites, trust if not present
			//new String[]{"Rank,%","Like","Likes","Comments","Date","Text","Links"},
			Object[][] data = feeder.getNews();	
			if (!AL.empty(data)){
				Date since = Time.today(-body.retentionDays());
				for (int i = 0; i < data.length; i++){
					Boolean like = (Boolean)data[i][1];
					Date time = (Date)data[i][4];
					String text = (String)data[i][5];
					String[] sites = (String[])data[i][6];
					//dont' add old news
					if (!AL.empty(sites) && time.compareTo(since) > 0) {
						for (int j = 0; j < sites.length; j++){
							boolean trust = like.booleanValue();
							peerSites = updateLinks(AL.sites,sites[j],trust,peerSites,maxSites);
						}
						//update news assuming sites are already added 
//TODO: this not if liked but if matched thing patterns!!!						
						if (like.booleanValue())
							updateNews(text,sites,time);
					}
				}
			}
			
			profilePeers();
		}
	}
	
	//TODO:
	void profilePeers(){
		String name = provider.provider() + " id";
		Object[][] similarPeers = feeder.getSimilarPeers(true,false,true);  
		if (!AL.empty(similarPeers)){
			for (int i = 0; i < similarPeers.length; i++){
				Object[] data = similarPeers[i];
				if (data == null || data.length < 8)
					continue;
				boolean myLikes = data[2] == null && ((Number)data[2]).intValue() > 0;
				boolean theirLikes = data[3] != null && ((Number)data[3]).intValue() > 0;
				boolean theirComments = data[4] != null && ((Number)data[4]).intValue() > 0;
				int similarity = data[0] == null ? 0 : ((Number)data[0]).intValue();
				String peerId = (String)data[7];
				if (id.equals(peerId))//don't count self
					continue;
				//if my likes or their likes or comments or event just similar, make trust
				if (myLikes || theirLikes || theirComments || similarity > 50){
					try {
						Collection peers = body.storager.getByName(name, peerId);
						if (!AL.empty(peers)){
							for (Iterator it = peers.iterator(); it.hasNext();){
								Thing other = (Thing)it.next();
								//add if not in friends and not in ignores 
								if (peer.hasThing(AL.friends, other) || peer.hasThing(AL.ignores, other))
									continue;
								peer.addThing(AL.friends, other);
								if (myLikes)//if like them, trust them
									peer.addThing(AL.trusts, other);
								if (theirLikes || theirComments)//if their likes or comments, also share to them
									peer.addThing(AL.shares, other);
							}
						}
					} catch (Exception e) {
						body.error("Profiling peer's peers", e);
					}
				}
			}
		}
	}
	
	void updateNews(String text, String[] sites, Date time){
		//TODO:handle multiple sources/sites per news
		if (AL.empty(text))
			return;
		for (int i = 0; i < sites.length; i++){
			Collection siteThings = body.storager.getNamed(sites[i]);
			if (!AL.empty(siteThings) && siteThings.size() == 1){
				//check if news is not present at all (don't make not-a-new news new again from feeded)
				Thing newone = new Thing();
				newone.addThing(AL.sources,(Thing)(siteThings.iterator().next()));
				newone.set(AL.text, text);
				newone.set(AL.times, Time.date(time));
				Collection news = body.storager.get(newone);
				if (AL.empty(news)){
					newone.store(body.storager);
//TODO: if this is done before 'store', it is not working! 					
					newone.set(AL._new, AL._true, peer);
					newone.set(AL.trust, AL._true, peer);
				}
				break;
			}
		}
	}
	
	int updateLinks(String name, String site, boolean trust, int count, int limit){
		//get only anonymous things!!!
		Collection things = body.storager.getNamed(site,null);
		if (AL.empty(things)) {
			Thing t = new Thing(site);
			t.store(body.storager);
			peer.addThing(name, t);
			if (trust && count < limit){
				peer.addThing(AL.trusts, t);
				count++;
			}
		} else {
			for (Iterator it = things.iterator(); it.hasNext();){
				Thing t = (Thing)it.next();
				if (peer.hasThing(name, t) || peer.hasThing(AL.ignores, t))
					continue;
				peer.addThing(name, t);
				if (trust && count < limit){
					peer.addThing(AL.trusts, t);
					count++;
				}
			}
		}
		return count;
	}
	
	public String toString() {
		return provider.provider() + " " + id + " " + token + " " + key;
	}
	
}
