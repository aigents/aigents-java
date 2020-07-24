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
package net.webstructor.peer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Statement;
import net.webstructor.al.Writer;
import net.webstructor.al.Time;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.Emailer;
import net.webstructor.core.Agent;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.serp.Serper;
import net.webstructor.util.Array;

public class Peer extends Agent {
	public static final String
		default_news_limit = "3";
	public static final String
		peer = "peer",
		secret_question = "secret question", 
		secret_answer = "secret answer",
		phone =	"phone",
		surname ="surname",
		birth_date = "birth date",
		sensitivity_threshold = "sensitivity threshold", 
		update_time = "update time",
		check_cycle = "check cycle", 
		news_limit = "news limit",
		items_limit = "items limit",
		trusts_limit = "trusts limit",
		last_check = "last check",
		email_notification = "email notification",
		slack_notification = "slack notification",
		facebook_notification = "facebook notification",
		telegram_notification = "telegram notification",
		registration_time = "registration time",
		activity_time = "activity time",
		login_time = "login time",
		login_count = "login count",
		login_token = "login token",
		language = "language",
		relevance = "relevance",
		social_relevance = "social relevance",
		paid_term = "paid term";
	public static final String[] properties = new String[] {
		AL.email,
		phone,
		surname,
		birth_date,
		sensitivity_threshold, 
		update_time,
		check_cycle, 
		Body.retention_period,
		news_limit,
		items_limit,
		trusts_limit,
		secret_question,
		secret_answer,
		email_notification,
		slack_notification,
		facebook_notification,
		telegram_notification,
		language,
		login_count,login_token,login_time,registration_time,activity_time,
		paid_term,
//TODO: make format and conversation per-session property (or make AL clients to understand JSON)!!!
		AL.format,
		AL.conversation,
		AL.currency,
		//AL.format,//TODO: fix unit test in agent_sites.php (not trivial!)?
		Body.facebook_id, Body.facebook_token,
		Body.vkontakte_id, Body.vkontakte_token,
		Body.paypal_id, Body.paypal_token,
		Body.reddit_id, Body.reddit_token, Body.reddit_image,
		Body.twitter_id, Body.twitter_token, Body.twitter_token_secret, Body.twitter_image, 
		Body.discourse_id,
		Body.telegram_id,
		Body.telegram_name,
		Body.google_id, Body.google_token,
		Body.steemit_id, Body.golos_id, Body.ethereum_id,
		AL.sites,
		AL.topics,
		AL.trusts,
		AL.ignores,
		AL.areas,
		AL.share,
		AL.shares,
		AL.friend,
		AL.friends,
		AL.queries,
		AL.clicks,
		AL.selections,
		AL.copypastes,
		AL.news
		};
	public static final String[] editables = new String[] {
		AL.name,
		surname,
		birth_date,
		AL.email,
		secret_question,
		secret_answer,
		check_cycle
	};
	public static final String[] title = new String[] {AL.name, Peer.surname};
	public static final String[] title_email = new String[] {AL.name, Peer.surname, AL.email};
	
	private static final String[] default_script = {
			"My topics 'новосибирск $number °c'.",
			"My trusts 'новосибирск $number °c'.",
			"My sites https://www.gismeteo.ru/city/daily/4690/.",
			"My trusts https://www.gismeteo.ru/city/daily/4690/.",
			"My topics 'москва $number °c'.",
			"My sites https://www.gismeteo.ru/city/daily/4368/.",

			"My topics 'вспышка балла $word'.",
			"My trusts 'вспышка балла $word'.",
			"My topics 'сегодня на солнце $number {вспышка вспышек}'.",
			"My trusts 'сегодня на солнце $number {вспышка вспышек}'.",
			"My sites http://www.tesis.lebedev.ru/sun_flares.html.",
			"My trusts http://www.tesis.lebedev.ru/sun_flares.html.",
		
			"My topics '{mission release} $info'.",
			"My trusts '{mission release} $info'.",
			"My sites http://aigents.com/en/.",
			"My sites http://aigents.com/en/contacts.html.",
			"My trusts http://aigents.com/en/.",
			"My trusts http://aigents.com/en/contacts.html.",

			"My topics '{google apple} $info', '$buyer {buys bought acquires acquired} $subject'.",
			"My trusts '{google apple} $info', '$buyer {buys bought acquires acquired} $subject'.",
			"My sites http://wired.com.",
			"My sites http://reuters.com.",
			//"My trusts http://wired.com.",
			//"My trusts http://reuters.com.",
		
			"My topics 'доллар руб. $number руб. $number'.",
			"My trusts 'доллар руб. $number руб. $number'.",
			"My topics 'евро руб. $number руб. $number'.",
			"My trusts 'евро руб. $number руб. $number'.",
			"My sites http://cbr.ru.",
			"My trusts http://cbr.ru.",
		
			"My topics '关注的城市 $info'.",
			"My trusts '关注的城市 $info'.",
			"My sites http://m.tianqi.com/beijing.",
			"My sites http://beijing.tianqi.com."
	};
	
	//TODO: purge as not used?
    public String[] getNamesPossible() {
    	return Array.union(super.getNamesPossible(), properties);
    }
    
	static void populateContent(Session session,boolean testPeer) throws Exception {
		Thing thisPeer = session.getStoredPeer();
		Collection topics = (Collection) thisPeer.get(AL.topics);
		Collection sites = (Collection) thisPeer.get(AL.sites);
		if (AL.empty(topics) && AL.empty(sites)) {//if it knows nothing AND sites nothing so presumably first time here 
			//TODO: all defaults, including email notification false 
			//thisPeer.setString(Peer.check_cycle,"3 hours");//TODO: if set here, breaks unit tests
			thisPeer.setString(Peer.news_limit,"10");
			thisPeer.setString(Peer.items_limit,String.valueOf(Body.PEER_ITEMS_LIMIT));
			thisPeer.setString(Peer.trusts_limit,String.valueOf(Body.PEER_TRUSTS_LIMIT));
			//execute the script adding initial content
			if (!testPeer){
				//if area is specified and area is represented by a peer or by a first "root" user, copy contents for the peer
				session.sessioner.body.debug("Populating content to "+thisPeer);
				Thing areaPeer = session.getAreaPeer();
				if (areaPeer != null){
					session.sessioner.body.debug("Populating content from "+areaPeer.name());
					areaPeer.copyTo(thisPeer,new String[]{AL.topics,AL.sites,AL.news},null,true);
				}else {
					//otherwise populate with default data
					for (int i = 0; i < default_script.length; i++) {
						Statement query = session.reader.parseStatement(session,default_script[i],thisPeer);
						session.sessioner.body.output("Dec:"+Writer.toString(query)+".");			
						new Query(session.sessioner.body,session.getStorager()).setThings(query,thisPeer);
					}
				}
				//TODO: sources http://aigents.com, new true, text 'Welcome...', times today
				Thing welcome = new Thing();
				Thing origin_site = session.getStorager().getThing(Body.ORIGINSITE);
				welcome.store(session.getStorager());
				welcome.set(AL._new, AL._true, thisPeer);
				welcome.set(AL.times, Time.today(1), thisPeer);
				welcome.set(AL.text, session.getBody().languages.translate(thisPeer.getString(Peer.language), "$greeting"));
				welcome.addThing(AL.sources, origin_site);
			}
			Thing self = session.getBody().self(); 
			Collection peers = new ArrayList((Collection)session.getStorager().getByName(AL.is,Schema.peer));
			Thing firstPeer = (Thing)peers.iterator().next();
			if (peers.size() == 1 && thisPeer.equals(firstPeer)) {//if only one user and it is the first user
				//give the first peer acces to the self body
				self.set(AL.trust, AL._true, thisPeer);
				//also make the first peer trusted by the self
				thisPeer.set(AL.trust, AL._true, self);					
				//set the self email same as email as first owner
				if (!testPeer) //TODO: why so? just to make tests passing?
					if (AL.empty(self.getString(AL.email)) && AL.empty(self.getString(Body.email_login))) {
						//using peer email for self email breaks email uniqueness!
						//self.setString(AL.email,thisPeer.getString(AL.email));
						//self.setString(Body.email_login,thisPeer.getString(AL.email));
						self.setString(AL.email,Emailer.DEFAULT_EMAIL);
						self.setString(Body.email_login,Emailer.DEFAULT_EMAIL);
					}
			}
		}
	}

	public static boolean registered(Thing peer) {
		return (!AL.empty(peer.getString(Peer.secret_question)) && !AL.empty(peer.getString(Peer.secret_answer)))
				|| !AL.empty(peer.getString(Body.telegram_id))
				|| !AL.empty(peer.getString(Body.slack_id))
				|| !AL.empty(peer.getString(Body.paypal_id))
				|| !AL.empty(peer.getString(Body.reddit_id))
				|| !AL.empty(peer.getString(Body.google_id))
				|| !AL.empty(peer.getString(Body.facebook_id))
				|| !AL.empty(peer.getString(Body.vkontakte_id));
	}
	
	public static String qualifier(String text) {
		String[] values = text.split(" ");
		int email_index = -1;
		for (int i = 0; i < values.length; i++)
			if (Emailer.valid(values[i]))
				email_index = i;
		String email = email_index >= 0 ? values[email_index] : "-"; // any matching email
		String name = email_index > 0 ? values[0] : values.length > 1 ? values[1] : "-"; // first or second
		String surname = email_index > 1 ? values[1] : values.length > 2 ? values[2] : "-"; // second or third	
		return AL.buildQualifier(Peer.title_email,new String[]{name,surname,email},0);
	}

	//TODO: do this in same place with Self.clear(body,peer)
	public static void rethink(Body body, Thing peer){
		body.debug("Thinking peer "+peer.getString(AL.email)+".");
		int news_limit = StringUtil.toIntOrDefault(peer.getString(Peer.news_limit),10,10);
		Collection origins = body.storager.getNamed(Body.ORIGINSITE);
		Thing origin = AL.empty(origins) ? null : (Thing)origins.iterator().next();

//TODO assigne sentiments as chained (pre-chained in given case in fact) thinking process relying on user custom sentiment models!?
		assignSentiments(body,peer);//TODO: in other place, make sure how it works on mobiles
		
		body.thinker.think(peer);
 		
		//get unchecked daily news with relevance less than 100
		//leave only MAX of them with topmost relevance or equal to the least relevance under MAX
		//unreference all the rest for the peer
		Collection allNews = (Collection)peer.getThings(AL.news);
		if (AL.empty(allNews))
			return;
		Collection trusts = (Collection)peer.getThings(AL.trusts);
//TODO: make sure forgetting does not remove authored news without topics (by 'authors' link) 
		Collection authors = Peer.peerFriendsTrusts(peer,true);
		Collection topics = Peer.peerTopics(authors);//topics across peer and all their authorities
		ArrayList news = new ArrayList();
		ArrayList dels = new ArrayList();
		Date today = Time.today(0);
		int days = body.attentionDays();
		if (!AL.empty(trusts)) for (Iterator it = allNews.iterator(); it.hasNext();){
			Thing t = (Thing)it.next();
			Date day = t.getDate(AL.times,null);
			int daysdiff = Period.daysdiff(day, today);
			//if (day != null && !(day.equals(today) || day.equals(yesterday)))
			if (day != null && daysdiff > days)//ignore items with undefined date or out of attention period
				continue;
			if (!trusts.contains(t) && !t.hasThing(AL.sources, origin)) {
				boolean relevant = false;  
				if (topics != null) {
					Collection classes = t.getThings(AL.is);
					if (classes != null) for (Object i : classes) {
						if (topics.contains(i)) {
							relevant = true; 
							break;
						}
					}
				}
				(relevant ? news : dels).add(t);
			}
		}
		for (Object del : dels)
			peer.delThing(AL.news, (Thing)del);
		if (news.size() > news_limit){
			Object[][] pairs = body.thinker.think(news, Peer.relevance, peer);
			Arrays.sort(pairs,new Comparator(){
				public int compare(Object arg0, Object arg1) {
					Object[] p1 = (Object[]) arg0;
					Object[] p2 = (Object[]) arg1;
					Number n1 = ((Number)p1[1]);
					Number n2 = ((Number)p2[1]);
					return n2.intValue() - n1.intValue();
				}
			});
			
			//re-sort with account to ordered numbers per thing - order by per_is_thing_order asc, relevance desc
			Object[][] triples = new Object[pairs.length][];//thing,relevance,per_is_thing_order
			HashMap things_counts = new HashMap();
			for (int i = 0; i < pairs.length; i++){
				triples[i] = new Object[3];
				triples[i][0] = pairs[i][0];
				triples[i][1] = pairs[i][1];
				Object is = ((Thing)pairs[i][0]).getFirst(AL.is); 
				Integer count = (Integer)things_counts.get(is);
				if (count == null)
					count = new Integer(0);
				triples[i][2] = count;				
				things_counts.put(is,new Integer(count.intValue() + 1));
			}
			Arrays.sort(triples,new Comparator(){
				public int compare(Object arg0, Object arg1) {
					Object[] p1 = (Object[]) arg0;
					Object[] p2 = (Object[]) arg1;
					//first, sort by per-thing order number asc
					Integer i1 = ((Integer)p1[2]);
					Integer i2 = ((Integer)p2[2]);
					if (!i1.equals(i2))
						return i1.intValue() - i2.intValue();
					//next, sort by peer-thing order number desc
					Number n1 = ((Number)p1[1]);
					Number n2 = ((Number)p2[1]);
					return n2.intValue() - n1.intValue();
				}
			});
			//compute per-thing thresholds
			HashMap things_thesholds = new HashMap();
			for (int i = 0; i < news_limit; i++){
				Object is = ((Thing)triples[i][0]).getFirst(AL.is); 
				things_thesholds.put(is, ((Number)triples[i][1]));
			}

			//delete news with relevance below than minimum passing relevance
			for (int i = news_limit; i < triples.length; i++){
				Thing n = (Thing)triples[i][0];
				Object is = ((Thing)n).getFirst(AL.is);
				Number limit = (Number)things_thesholds.get(is);
				int valueOnLimit = limit == null ? 0 : limit.intValue();
				if (((Number)triples[i][1]).intValue() < valueOnLimit)
					peer.delThing(AL.news, n);
			}
		}

		//assing images fo imageless news items for paid users
		assignImages(body,peer);//TODO: in other place, make sure how it works on mobiles
	}

	public static Set peerFriendsTrusts(Thing peer, boolean self) {
		HashSet result = new HashSet();
		Collection friends = peer.getThings(AL.friends);
		Collection trusts = peer.getThings(AL.trusts);
		if (!AL.empty(friends) && !AL.empty(trusts)){//keep trusted topics only
			result.addAll(friends);
			result.retainAll(trusts);
		}
		if (self)
			result.add(peer);
		return result;
	}

	public static Set peerTopics(Thing peer) {
		Collection topics = peer.getThings(AL.topics);
		Collection trusts = peer.getThings(AL.trusts);
		if (!AL.empty(topics) && !AL.empty(trusts)){//keep trusted topics only
			HashSet result = new HashSet(topics);
			result.retainAll(trusts);
			return result;
		}
		return null;
	}
	
	public static Collection getSharesTos(Storager storager, Thing peer){
		Set trustingPeers = storager.get(AL.trusts,peer);//all who trusts us (subscribers)
		Collection allShares = (Collection)peer.get(AL.shares);//all who we share to plus our shared areas
		if (!AL.empty(allShares) && !AL.empty(trustingPeers)) {//we need to share something plus should have someone who trusts us
			trustingPeers = new HashSet(trustingPeers);
			//check if this peer is public
			Collection areas = (Collection)peer.get(AL.areas);
			if (!AL.empty(areas)) {
				areas = new HashSet(areas);
				areas.retainAll(allShares);
			}
			if (AL.empty(areas))//if have public areas, return all trustees, otherwise:
				trustingPeers.retainAll(allShares);//leave only the peers who are explicitly shared
			return trustingPeers;
		}
		return null;
	}
	
	public static Set peerTopics(Collection peers) {
		HashSet allTopics = new HashSet();
		for (Object peer : peers) {
			Set topics = peerTopics((Thing)peer);
			if (!AL.empty(topics))
				allTopics.addAll(topics);
		}
		return allTopics;
	}

	public static boolean paid(Thing peer) {
		Date term = peer.getDate(Peer.paid_term, null);
		return term == null || term.compareTo(Time.today()) < 0 ? false : true;
	}
	
	public static void assignSentiments(Body body, Thing peer) {
		Collection news = (Collection)peer.getThingsClone(AL.news);
		if (news != null) for (Object t : news)
			assignSentiment(body, (Thing) t);
	}

	public static void assignSentiment(Body body, Thing thing) {
		if (thing.getString(AL.sentiment) == null) {//do repeated image searches only for non-searched missed images
			String text = thing.getString(AL.text);
			if (!AL.empty(text)) {
				int[] pns = body.languages.sentiment(text);
				int s = pns[2];
				thing.set(AL.sentiment, String.valueOf(s));
			}
		}
	}
	
	public static void assignImages(Body body, Thing peer) {
		if (!paid(peer))
			return;
		Collection news = peer.getThingsClone(AL.news);
		if (news != null) for (Object t : news)
			assignImage(body, (Thing) t);
	}
	
	public static void assignImage(Body body, Thing thing) {
		//if (AL.empty(thing.getString(AL.image))) {
		if (thing.getString(AL.image) == null) {//do repeated image searches only for non-searched missed images
			String text = thing.getString(AL.text);
			if (!AL.empty(text)) for (Serper sr : body.getSerpers()) {
				Collection<Thing> ts = sr.search("image", text, null, 1);
				if (ts != null) for (Thing t : ts) {
					String image = t.getString(AL.image);
					if (!AL.empty(image)) {
						thing.setString(AL.image, image);
						return;
					}
				}
			}
			thing.setString(AL.image, "");//mark image as searched but not found
		}
	}
	
}
