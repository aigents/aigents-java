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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.core.Thing;
import net.webstructor.data.Counter;
import net.webstructor.data.Linker;
import net.webstructor.data.Graph;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Peer;

/*
- sites
-- importance важность
- news
-- relevance актуальность
- peer
-- similarity сходство
-- authority авторитет
-- closeness близость 
-- adherence приверженность

*/

/**
 * Aggregates Graph and Counters on Graph content dimensions (thought vectors).
 */
class Thought {
	Graph graph = new Graph();
	Thinker thinker; 
	Thing context;
	Thought(Thinker thinker, Thing context) {
		this.thinker = thinker;
		this.context = context;
	}
	Number value(Object target, String property) {
		return graph.getValue(context, target, property);
	}
	boolean update(String[] properties, boolean force){
		HashMap linkers = graph.getLinkers(context,true);
		for (int i = 0; i < properties.length; i++){
			Linker linker = (Linker) linkers.get(properties[i]);
			if (linker == null || force) {
				Reasoner reasoner = thinker.getReasoner(context,properties[i]);
				if (reasoner != null){
					linker = reasoner.getLinker();
					linkers.put(properties[i], linker);
				}
			}
		}
		return true;
	}
}

/**
 * Produces "thought vector" (as Linker) for given thinkable dimension (property).  
 * @author akolonin
 */
abstract class Reasoner {
	Thinker thinker;
	Thing context;
	Reasoner(Thinker thinker,Thing context){
		this.thinker = thinker;
		this.context = context;
	}
	abstract Linker getLinker();
}

class TextFeaturer extends Reasoner {
	TextFeaturer(Thinker thinker,Thing context){
		super(thinker,context);
	}
	Linker getLinker() {
		//TODO: feature analysis
		Counter norm = thinker.body.languages != null && !AL.empty(thinker.body.languages.words())
				? thinker.body.languages.words() : null;
		Counter words = new Counter();
		String text = context.getString(AL.text);
		SocialFeeder.countWords(thinker.body.languages,text,words);
		if (norm != null)
			words.normalizeBy(norm,1);
		return words;
	}
}

class PeerNewsRelevancer extends Reasoner {
	Counter norm;
	
	PeerNewsRelevancer(Thinker thinker,Thing context){
		super(thinker,context);
		norm = thinker.body.languages != null && !AL.empty(thinker.body.languages.words())
			? thinker.body.languages.words() : null;
	}
	
	//TODO: move out to separate thinker like TextFeaturer
	Counter getPeerWordsLinker(Thing peer) {
		//get all user preferences
		//TODO: get 'words' thought as dependent 'thought'
		Collection trusts = peer.getThings(AL.trusts);
		Counter words = new Counter();
		if (!AL.empty(trusts)){
			for (Iterator tit = trusts.iterator(); tit.hasNext();){
				Thing trust = (Thing)tit.next();
				String text = trust.getString(AL.text);
				if (!AL.empty(text)){
					SocialFeeder.countWords(thinker.body.languages,text,words);
					//Thought t = thinker.getThought(trust, new String[]{"feature"}, false);
					//t.value(trust, "feature");
				}
			}
		}
		//if no words from liked texts, get words from trusted things
		if (AL.empty(words) && !AL.empty(trusts)){
			Collection knows = peer.getThings(AL.knows);
			if (!AL.empty(knows)){
				knows = new HashSet(knows);
				knows.retainAll(trusts);
			}
			if (!AL.empty(knows)){
				for (Iterator kit = knows.iterator(); kit.hasNext();){
					Thing know = (Thing)kit.next();
					String text = know.getString(AL.name);
					//TODO: get child patterns
					if (!AL.empty(text)){
						SocialFeeder.countWords(thinker.body.languages,text,words);
						//Thought t = thinker.getThought(trust, new String[]{"feature"}, false);
						//t.value(trust, "feature");
					}
				}
			}
		}
		if (norm != null)
			words.normalizeBy(norm,1);
		return words;
	}
	
	//TODO: Linker -> Linker
	/** 
	 * Normalize relevances on across categories basis.
	 * @param words providing relevance context
	 * @return
	 */
	Counter getNewsRelevances(Counter words){
		//get all user news
		Collection news = context.getThings(AL.news);
		//for each of the news
		Counter relevances = new Counter();
		if (!AL.empty(news))
		for (Iterator it = new ArrayList(news).iterator(); it.hasNext();){
			Thing item = (Thing)it.next();
			String text = item.getString(AL.text);
			if (!AL.empty(text))
				countRelevances(item, text, words, relevances, false);
		}
		relevances.normalize();
		return relevances;
	}

	//TODO: Linker -> Linker
	/** 
	 * Normalize relevances on per-category basis.
	 * @param words providing relevance context
	 * @return
	 */
	Counter getThingSpecificNewsRelevances(Counter words,boolean multiplied){
		Counter allRelevances = new Counter();
		//get all user news
		Collection news = context.getThings(AL.news);
		//for each of the news, count to thing-specific map
		if (!AL.empty(news)){
			HashMap thingCounters = new HashMap(); 
			for (Iterator it = new ArrayList(news).iterator(); it.hasNext();){
				Thing item = (Thing)it.next();
				String text = item.getString(AL.text);
				if (AL.empty(text))
					continue;
				Collection ises = (item.getThings(AL.is));
				if (!AL.empty(ises)){
					Thing is = (Thing)ises.iterator().next();
					Counter byThing = (Counter)thingCounters.get(is);
					if (byThing == null)
						thingCounters.put(is, byThing = new Counter());
					countRelevances(item, text, words, byThing, multiplied);
				} else
					countRelevances(item, text, words, allRelevances, multiplied);
			}
			allRelevances.normalize();
			for (Iterator it = thingCounters.values().iterator(); it.hasNext();){
				Counter byThing = (Counter)it.next();
				byThing.normalize();
				allRelevances.mergeMax(byThing);
			}
		}
		return allRelevances;
	}

	Counter getPeerRelevances(Counter words){
		//for each of the peers
		Counter peersRelevances = new Counter();
		try {
			//Collection peers = thinker.body.storager.getByName(AL.is, Peer.peer);
			Collection peers = context.getThings(AL.friends);//use all friends even if not trusted
			if (!AL.empty(peers)){
				/*
				Collection trusted = context.getThings(AL.trusts);				
				if (!AL.empty(trusted)){
					peers = new HashSet(peers);
					peers.retainAll(trusted);
					if (!AL.empty(peers)){
					*/
						for (Iterator it = peers.iterator(); it.hasNext();){
							Thing peerItem = (Thing)it.next();
							Counter peerWords = getPeerWordsLinker(peerItem);
							Object[] cross = words.crossOverlap(peerWords);
							Number value = (Number)cross[0];
							peersRelevances.count(peerItem,value.doubleValue());
						}
						peersRelevances.normalize();
						/*
					}
				}
				*/
			}
		} catch (Exception e) {
			thinker.body.error("Thinking peer on peer", e);
		}
		return peersRelevances;
	}

	void countRelevances(Object item, String text, Counter words, Counter relevances, boolean multiplied){
		Counter textWords = new Counter();
		SocialFeeder.countWords(thinker.body.languages,text,textWords);
		if (norm != null)
			textWords.normalizeBy(norm,1);
		Object[] cross = multiplied ? words.crossMultiplied(textWords) : words.crossOverlap(textWords);
		Number value = (Number)cross[0];
		relevances.count(item,value.doubleValue());
	}
	
	Counter getKnowsRelevances(Counter words){
		//for each of the peers
		Counter relevances = new Counter();
		Collection knows = context.getThings(AL.knows);
		if (!AL.empty(knows))
		for (Iterator it = knows.iterator(); it.hasNext();){
			Thing item = (Thing)it.next();
			String text = item.getString(AL.name);
			if (!AL.empty(text))
				countRelevances(item, text, words, relevances, false);
			//TODO: add patterns of knows
		}
		relevances.normalize();
		return relevances;
	}

	Counter getSitesRelevances(Counter words){
		Counter relevances = new Counter();
		Collection sites = context.getThings(AL.sites);
		if (!AL.empty(sites))
		for (Iterator it = new ArrayList(sites).iterator(); it.hasNext();){
			Thing item = (Thing)it.next();
			Collection instances = thinker.body.storager.get(AL.is, item);
			if (!AL.empty(instances))
			for (Iterator ins = instances.iterator(); ins.hasNext();){
				Thing inst = (Thing)ins.next();
				String text = inst.getString(AL.text);
				if (!AL.empty(text))
					countRelevances(item, text, words, relevances, false);
			}
		}
		relevances.normalize();
		return relevances;
	}
	
	Linker getLinker() {		
		Counter words = getPeerWordsLinker(context);
		
		//Counter relevances = getNewsRelevances(words);
		Counter relevances = getThingSpecificNewsRelevances(words,false);
		Counter peersRelevances = getPeerRelevances(words);
		Counter knowsRelevances = getKnowsRelevances(words);
		Counter sitesRelevances = getSitesRelevances(words);
		
		relevances.mergeMax(peersRelevances);
		relevances.mergeMax(knowsRelevances);
		relevances.mergeMax(sitesRelevances);
		
		return relevances;
	}
}


//TODO: don't extend but re-use underlying Relevancers with cascading Thoughts
class PeerNewsSocialRelevancer extends PeerNewsRelevancer { // Reasoner {
//class PeerNewsSocialRelevancer extends Reasoner {

	PeerNewsSocialRelevancer(Thinker thinker, Thing context) {
		super(thinker, context);
	}

	Linker getLinker() {
		// TODO Auto-generated method stub
		
		//--- get list of my news
		Collection news = context.getThings(AL.news);
		if (AL.empty(news))
			return null;
		
		/*
		//--- get list of my trusts
		Collection trusts = context.getThings(AL.trusts);
		if (AL.empty(trusts))
			return null;
		
		//--- get list of peers that I trust to
		Collection peers = new ArrayList();
		try {
			Collection all = (Collection)thinker.body.storager.getByName(AL.is,Schema.peer);
			for (Iterator it = all.iterator(); it.hasNext();){
				Thing peer = (Thing)it.next();
				if (trusts.contains(peer))
					peers.add(peer);
			}
		} catch (Exception e) {
			thinker.body.error("Thinking "+context.getTitle(Schema.keys), e);
		}
		*/
		Collection peers = context.getThings(AL.friends);//use all friends even if not trusted
		if (AL.empty(peers))
			return null;
		
		Counter relevances = new Counter();
		
		//try explicit social relevance
		//- for each of my news, count peer's trusts to each of the news
		for (Iterator it = news.iterator(); it.hasNext();){
			Thing item = (Thing)it.next();
			for (Iterator pit = peers.iterator(); pit.hasNext();){
				Thing peer = (Thing)pit.next();
				if (peer.hasThing(AL.trusts, item))
					relevances.count(item);
			}
		}
		//- if at least one found, calculate the relevance
		if (relevances.size() > 0){
			relevances.normalize();
			return relevances;
		}
		
		//--- if none is found 
		//--- try implicit social relevance
		//---- for my peers, calculate word vector of the words they like
		Counter peerWordRelevances = new Counter();
		for (Iterator it = peers.iterator(); it.hasNext();){
			Thing peerItem = (Thing)it.next();
			Counter peerWords = getPeerWordsLinker(peerItem);
			peerWordRelevances.mergeSum(peerWords);
		}
		//TODO no need to normalize, sure?

		//---- for each of my news, count their word vectors
		//---- calculate similarity between the vectors
		relevances = getThingSpecificNewsRelevances(peerWordRelevances,true);
		return relevances;
	}
}


//TODO: thread-safety
/**
 * Contains cache of context-specific (eg. owner-specific) thoughts. 
 * @author akolonin
 */
public class Thinker {
	
	Body body;
	HashMap thoughts = new HashMap();

	public Thinker(Body body) {
		this.body = body;
	}
	
	Reasoner getReasoner(Thing context,String thinkable){
		if (Peer.social_relevance.equals(thinkable))
			return new PeerNewsSocialRelevancer(this,context);
		if (Peer.relevance.equals(thinkable))
			return new PeerNewsRelevancer(this,context);
		if ("feature".equals(thinkable))
			return new TextFeaturer(this,context);
		return null;
	}
	
	//TODO: unify with either Schema.thinkables or this.getReasoner
	String[] getThinkables(){
		return new String[]{Peer.relevance,Peer.social_relevance};
	}
	
	//TODO: remove useless thoughts
	boolean unfocus(){
		return false;
	}
	
	//TODO get cached thought or put it in cache first
	Thought getThought(Thing context, String[] thinkables, boolean update) {
		Thought thought = (Thought)thoughts.get(context);
		if (thought == null){
			thought = new Thought(this,context);
			thought.update(thinkables,true);
			thoughts.put(context, thought);
		} else {
			thought.update(thinkables,update);
		}
		return thought;
	}
	
	public String[] thinkables(String[] properties) {
		ArrayList thinkable = new ArrayList();
		for (int i = 0; i < properties.length; i++)
			if (Schema.thinkable(properties[i]))
				thinkable.add(properties[i]);
		return (String[])thinkable.toArray(new String[]{});
	}
	
	public boolean think(Thing thinker) {
		getThought(thinker,getThinkables(),true);
		return true;
	}
	
	public boolean think(Thing thing, Thing clone, String[] properties, Thing viewer) {
		if (AL.empty(properties))
			return false;
		String[] thinkables = thinkables(properties);
		if (AL.empty(thinkables))
			return false;
		Thought thought = getThought(viewer,thinkables,false);//false means don't force update
		if (thought != null) {
			for (int i = 0; i < thinkables.length; i++) {
				Number value = thought.value(thing,thinkables[i]);
				if (value != null){
					//thing.set(thinkables[i], value);//TODO: handle Number-s as thinkable properties
					clone.set(thinkables[i], value.toString());
				}
			}
		}
		return true;
	}

	public Object[][] think(Collection things, String property, Thing viewer) {
		if (AL.empty(property) || AL.empty(things))
			return null;
		String[] thinkables = thinkables(new String[]{property});
		if (AL.empty(thinkables))//should contain only the property
			return null;
		Thought thought = getThought(viewer,thinkables,false);//false means don't force update
		if (thought == null)
			return null;
		Object res[][] = new Object[things.size()][];
		int i = 0;
		for (Iterator it = things.iterator(); it.hasNext();) {
			Thing thing = (Thing)it.next();
			Number value = thought.value(thing,property);
			res[i++] = new Object[]{thing,value != null ? value : new Integer(0)};
		}
		return res;
	}
}
