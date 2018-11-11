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

import java.util.Collection;
import java.util.Iterator;

import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.al.Writer;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.core.Mistake;
import net.webstructor.core.Property;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.util.Array;

public abstract class Mode {
	
	static final protected Set cancel_pattern = Reader.patterns(null,null,"{no not login logout [my login] [my logout]}");
	
	public abstract boolean process(Session session);
	static protected String statement(Exception e) {
		String message = e.getMessage();
		return Writer.capitalize(message != null ? message : e.toString())+".";		
	}	

	boolean noSecretQuestion(Session session) {
		//if a non-secret question from anonymous, answer it
		//int mood = session.reader.readMood(session.input);
		//String[] disallowed_words = new String[]{AL.you[0],Schema.peer,Schema.self,Schema.self,/*Body.email,*/Body.email_password,Peer.secret_answer};
		//if (mood == AL.interrogation && Array.containsAnyAsSubstring(disallowed_words,session.input) == null)
		String[] allowed_questions = new String[]{
				"What new true sources, text, times, trust, relevance, social relevance, image?",
				"What new true sources, text, times, trust, relevance, social relevance?",
				"What new true sources, text, times, trust, relevance?",
				"What new true sources, text, times, trust?",
				"What my sites name, trust, relevance?",
				"What my areas?",
				"What my sites name, trust?",
				"What my knows name, trust, relevance?",
				"What my knows name, trust?"}; 
		if (session.mood == AL.interrogation && Array.containsIgnoreCase(allowed_questions, session.input))
			return true;
		return false;
	}

	//TODO: remove this!
	protected boolean trusted(Session session) {
		/*
		Collection trusts = (Collection)session.getBody().self().get(AL.trusts);
		if (!AL.empty(trusts) && trusts.contains(session.getStoredPeer()))
			return true;
		return false;
		*/
		return session.trusted();
	}

	//TODO: do this via generic functionality such as temporary proxy peer?
	boolean setProperties(Session session) {
		Thing temp = new Thing();
		if (Reader.read(session.input, Reader.pattern(AL.i_my,temp,new String[]{AL.areas}))){
			String area = temp.getString(AL.areas);
			if (session.peer == null)
				session.peer = new Thing();
			session.peer.delThings(AL.areas);
			if (!AL.empty(area)){
				session.peer.addThing(AL.areas, session.sessioner.body.storager.getThing(area));
			}
			session.output = "Ok.";
			return true;
		}
		if (Reader.read(session.input, Reader.pattern(AL.i_my,temp,new String[]{Peer.language}))){
			if (session.peer == null)
				session.peer = new Thing();
			session.peer.setString(Peer.language,temp.getString(Peer.language));
			session.output = "Ok.";
			return true;
		}
		return false;
	}

	Thing getSessionAreaPeer(Session session){
		Thing peer = session.peer != null && session.authenticated ? session.getStoredPeer() : null;
		
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
	
	boolean answer(Session session) {
		Thing peer = getSessionAreaPeer(session);
		try {
			Seq query = session.reader.parseStatement(session,session.input,peer);
			session.sessioner.body.output("Int:"+Writer.toString(query)+"?");			
			//execute query: [thing,thing,thing,...,set]
			Collection clones = new Query(session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker)
				.getThings(query,peer); 
			//dump query to output
			if (peer != null){
				//TODO: move to ontology
				String format = peer.getString("format");
				if (!AL.empty(format) && format.equalsIgnoreCase("json")){
					session.output = Writer.toJSON(clones);
					return false;
				}
			}
			session.output = Writer.toPrefixedString(
					session,//TODO: fix hack!
					query, clones);
			return false;						
		} catch (Exception e) {
			session.output = statement(e);
			if (!(e instanceof Mistake))
				session.sessioner.body.error(e.toString(), e);
			return false;
		}
	}

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
					Thing peer = session.getAreaPeer(area);
					//if not found, return
					if (peer != null){
						//get news feed for the peer
						Seq query;
						try {
							query = session.reader.parseStatement(session,"What new true, times today is, sources, text?",peer);
							//execute query: [thing,thing,thing,...,set]
							Collection clones = new Query(session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker).getThings(query,peer); 
							//format news feed
							session.output = createRSS(clones,area,session.sessioner.body.site());
						} catch (Exception e) {
							session.output = statement(e);
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
	
	String createRSS(Collection things,String area,String url){
		StringBuilder feed = new StringBuilder();
		feed.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		feed.append("<rss version=\"2.0\">\n\n");
		feed.append("<channel>\n");
		feed.append("  <title>Aigents on ").append(area).append("</title>\n");
		feed.append("  <link>").append(url).append("</link>\n");
		feed.append("  <description>Aigents RSS feed about ").append(area).append("</description>\n");
		if (!AL.empty(things)){
			for (Iterator i = things.iterator(); i.hasNext();){
				Thing t = (Thing)i.next();
				Collection iss = t.getThings(AL.is);
				Collection sources = t.getThings(AL.sources);
				if (!AL.empty(iss) && !AL.empty(sources)){
					String topic = ((Thing)iss.iterator().next()).getName();
					String link = ((Thing)sources.iterator().next()).getName();
					String text = t.getString(AL.text);
					topic = HtmlStripper.encodeHTML(topic);
					text = HtmlStripper.encodeHTML(text);
					//TODO:encode with function
					link = link.replaceAll("&", "&amp;");
					String description = topic+" :\n"+text;
					feed.append("  <item>\n");
					feed.append("    <title>").append(text).append("</title>\n");
					feed.append("    <link>").append(link).append("</link>\n");
					feed.append("    <description>").append(description).append("</description>\n");
					feed.append("  </item>\n");
				}
			}
		}
		feed.append("</channel>\n\n</rss>\n");
		return feed.toString();
	}
}
