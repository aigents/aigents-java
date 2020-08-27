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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.core.Property;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.util.Array;

public abstract class Responser implements Intenter {
	
	static final protected Set cancel_pattern = Reader.patterns(null,null,"{no not login logout [my login] [my logout] bye}");
	
//TODO: initialize list of intenters in Body/Farm and rearrange their order in Session 
	private static Intenter[] intenters = new Intenter[]{
				new Searcher(),
				new ControlledResponser(),
				new PatternResponser()
				,new Finder()
				,new Answerer()
	};

	@Override
	public String name() {
		return "system";
	}

	@Override
	public boolean handleIntent(final Session session) {
//TODO user "controlled" OR "natural" in order according to Peer/Session "conversation" property?
		Thing peer = session.getStoredPeer();
		String conversation = peer != null ? peer.getString(AL.conversation) : null;
		if (!AL.empty(conversation))
			for (Intenter i : getIntenters())
				if (conversation.contains(i.name()) && i.handleIntent(session))
					return true;
		for (Intenter i : getIntenters())
			if (i.handleIntent(session))
				return true;
		return false;
	}

	public Intenter[] getIntenters() {
		return intenters;
	}
	
	public abstract boolean process(Session session);
	static protected String statement(Throwable e) {
		String message = e.getMessage();
		//return Writer.capitalize(message != null ? message : e.toString())+".";		
		return Writer.capitalize(message != null ? message : e.getClass().getSimpleName())+".";		
	}	

	static boolean noSecretQuestion(Session session) {
		//if a non-secret question from anonymous, answer it
		//int mood = session.reader.readMood(session.input);
		//String[] disallowed_words = new String[]{AL.you[0],Schema.peer,Schema.self,Schema.self,/*Body.email,*/Body.email_password,Peer.secret_answer};
		//if (mood == AL.interrogation && Array.containsAnyAsSubstring(disallowed_words,session.input) == null)
		String[] allowed_questions = new String[]{
				"What new true sources, text, times, trust, relevance, social relevance, image, is, sentiment?",
				"What new true sources, text, times, trust, relevance, social relevance, image, is?",
				"What new true sources, text, times, trust, relevance, social relevance, image?",
				"What new true sources, text, times, trust, relevance, social relevance?",
				"What new true sources, text, times, trust, relevance?",
				"What new true sources, text, times, trust?",
				"What my sites name, trust, relevance, positive, negative?",
				"What my sites name, trust, relevance, sentiment?",
				"What my sites name, trust, relevance?",
				"What my areas?",
				"What my sites name, trust?",
				"What my topics name, trust, relevance, positive, negative?",
				"What my topics name, trust, relevance, sentiment?",
				"What my topics name, trust, relevance?",
				"What my topics name, trust?"}; 
		if (session.mood == AL.interrogation && Array.containsIgnoreCase(allowed_questions, session.input()))
			return true;
		return false;
	}

	//TODO: do this via generic functionality such as temporary proxy peer?
	static boolean setProperties(Session session) {
		Thing temp = new Thing();
		if (Reader.read(session.input(), Reader.pattern(AL.i_my,temp,new String[]{AL.areas}))){
			String area = temp.getString(AL.areas);
			if (session.peer == null)
				session.peer = new Thing();
			session.peer.delThings(AL.areas);
			if (!AL.empty(area)){
				session.peer.addThing(AL.areas, session.sessioner.body.storager.getThing(area));
			}
			session.output(session.ok());
			return true;
		}
		if (Reader.read(session.input(), Reader.pattern(AL.i_my,temp,new String[]{Peer.language}))){
			if (session.peer == null)
				session.peer = new Thing();
			session.peer.setString(Peer.language,temp.getString(Peer.language));
			session.output(session.ok());
			return true;
		}
		return false;
	}

	static Thing getSessionAreaPeer(Session session){
		Thing peer = session.peer != null && session.authenticated() ? session.getStoredPeer() : null;
		
		//for authenticated session with some session context 
		//TODO: for the time being, get "opinion leader" for the first of session "areas", if specified
		if (peer == null)
			peer = session.getAreaPeer();
		
		if (peer == null) {
			//TODO: replace with self - with "collective" belief inferred from peers
			//TODO: cleanup because this is aready done in getAreaPeer?
			Collection trusts = (Collection)session.getBody().self().get(AL.trusts);
			if (!AL.empty(trusts))
				peer = (Thing)trusts.iterator().next();
		}
		return peer;
	}
	
	static String format(String format, Session session, Thing peer, Seq query, Collection coll){
		//dump query to output
		if (AL.empty(format) && peer != null)
			format = peer.getString("format");//TODO: move to ontology
		return "json".equalsIgnoreCase(format) ? Writer.toJSON(coll,null) 
			: "html".equalsIgnoreCase(format) ? Writer.toHTML(coll,null)
			: Writer.toPrefixedString(
					session,//TODO: fix hack!
					query, coll);
	}
	
	static Collection queryFilter(Session session, Thing peer, Seq query, Query.Filter filter) throws Throwable {
		//execute query: [thing,thing,thing,...,set]
		Collection clones = new Query(session.sessioner.body,session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker)
			.getThings(query,peer);
		if (AL.empty(clones))
			return null;
		if (filter != null){
			Collection filtered = new ArrayList(clones.size());
			for (Iterator it = clones.iterator(); it.hasNext();){
				Thing t = (Thing)it.next();
				if (filter.passed(t))
					filtered.add(t);
			}
			if (AL.empty(filtered))
				return null;
			clones = filtered;
		}
		return clones;
	}

	static String queryFilterFormat(Session session, Thing peer, Seq query) throws Throwable {
		//execute query: [thing,thing,thing,...,set]
		Collection clones = queryFilter(session, peer, query, null);
		if (clones == null)//TODO: fix regression hack!?
			return null;
		//dump query to output
		return format(null, session, peer, query, clones);
	}
	
	boolean answer(Session session) {
		if (handleIntent(session))//session.query is expected ot get filled by controlledResponser!  
			return false;
		session.output(session.no());
		return false;
	}

	//TODO: Intenter
	boolean tryRSS(Storager storager, Session session) {
		//"What areas $area rss?" => "$area rss"
		//Get list of areas in the system
		String[] areas = storager.getValuesNames(AL.areas);
		if (!AL.empty(areas)){
			//Build "$area rss" template
			Thing arg = new Thing();
			if (/*(session.read(new Seq(new Object[]{"areas",new Any(1,areas),"rss"})) &&
				session.read(new Seq(new Object[]{"areas",new Property(arg,"area"),"rss"})))||*/
				(session.read(new Seq(new Object[]{"rss",new Any(1,areas)})) &&
				session.read(new Seq(new Object[]{"rss",new Property(arg,"area")})))){
				String area = arg.getString("area");
				if (!AL.empty(area)){
					//Get peer for matched area
					Thing peer = session.getAreaPeer(area,true);
					//if not found, return
					if (peer != null){
						//get news feed for the peer
						Seq query;
						try {
							query = session.reader.parseStatement(session,"What new true, times today or yesterday is, sources, text, image, times, title?",peer);
							//execute query: [thing,thing,thing,...,set]
							Collection clones = new Query(session.sessioner.body,session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker).getThings(query,peer); 
							//format news feed
							session.output(createRSS(clones,area,session.sessioner.body.site(),Writer.capitalize(session.sessioner.body.name())));
						} catch (Exception e) {
							session.output(statement(e));
							session.sessioner.body.error(e.toString(), e);
						}
						return true;
					}
				}
			}
			//if not matched, return
		}
		return false;
	}
	
	//https://validator.w3.org/feed/docs/rss2.html#ltauthorgtSubelementOfLtitemgt
	String createRSS(Collection things,String area,String url,String author){
		area = Writer.capitalize(area);
		String authorarea = author + " on " + area;
		StringBuilder feed = new StringBuilder();
		feed.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		feed.append("<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n\n");
		feed.append("<channel>\n");
		feed.append("  <title>").append(authorarea).append("</title>\n");
		feed.append("  <link>").append(url).append("</link>\n");
		feed.append("  <description>Aigents RSS feed about ").append(area).append("</description>\n");
		if (!AL.empty(things)){
			for (Iterator i = things.iterator(); i.hasNext();){
				Thing t = (Thing)i.next();
				Collection iss = t.getThings(AL.is);
				Collection sources = t.getThings(AL.sources);
				String text = t.getString(AL.text);
				String title = t.getString(AL.title);
				String image = t.getString(AL.image);
				Date time = (Date)t.get(AL.times);
				if (!AL.empty(sources) && !AL.empty(text)){
					String link = ((Thing)sources.iterator().next()).name();
					String topic = AL.empty(iss) ? null : HtmlStripper.encodeHTML(((Thing)iss.iterator().next()).name());
					title = HtmlStripper.encodeHTML(title);
					text = HtmlStripper.encodeHTML(text);
					link = HtmlStripper.encodeHTML(link);
					if (AL.empty(title))
						title = !AL.empty(topic) ? topic : text;
					String description = topic == null ? text : "topic: \"" + topic + "\".\n" + text;
					feed.append("  <item>\n");
					feed.append("    <title>").append(title).append("</title>\n");
					feed.append("    <link>").append(link).append("</link>\n");
					feed.append("    <description xml:space=\"preserve\">").append(description).append("</description>\n");
					if (!AL.empty(image))
						//https://www.w3schools.com/xml/tryrss.asp?filename=rss_ex_enclosure
						feed.append("    <enclosure url=\"").append(HtmlStripper.encodeHTML(image)).append("\" type=\"image\" />\n");
					if (time != null)
						feed.append("    <pubDate>").append(Time.rfc822(time)).append("</pubDate>\n");
					if (!AL.empty(topic))
						feed.append("    <category>").append(topic).append("</category>\n");
					if (!AL.empty(author))
						feed.append("    <dc:creator><![CDATA[").append(authorarea).append("]]></dc:creator>\n");//https://www.aitrends.com/feed/
					//https://validator.w3.org/feed/docs/rss2.html#ltguidgtSubelementOfLtitemgt
//TODO: <guid isPermaLink="true">http://inessential.com/2002/09/01.php#a2</guid>
					feed.append("  </item>\n");
				}
			}
		}
		feed.append("</channel>\n\n</rss>\n");
		return feed.toString();
	}
}
