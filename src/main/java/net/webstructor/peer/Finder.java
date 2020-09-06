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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Set;
import net.webstructor.al.Writer;
import net.webstructor.core.Mistake;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.data.Translator;
import net.webstructor.util.Array;


class FinderContext {
	Collection res = new HashSet();
	HashMap<String,Integer> attributes = new HashMap<String,Integer>();
	String keyAttribute = null;
	java.util.Set options = null;
	String question = null;
	int retries = 0;
}

class Finder implements Intenter {	

	@Override
	public String name() {
		return "objective";
	}

	public boolean searchable(String name){
		return AL.name.equals(name) || !Array.contains(AL.foundation, name);
	}
	
	@Override
	public boolean handleIntent(final Session session){
		try {
			Thing peer = session.getStoredPeer();
			if (peer == null)//TODO make working anonymously?
				return false;
			//Storager storager = session.getStorager();
			LangPack languages = session.getBody().languages;
			Translator trans = session.getBody().translator(session.language());
			String yes = trans.loc("yes");
			String no = trans.loc("no");
			String input = session.input().trim().toLowerCase();

//TODO: keep this in session or context object/array
			FinderContext fc = (FinderContext)session.context(name());
			if (fc == null)
				session.context(name(), fc = new FinderContext());
			
			///////////////// start finding /////////////////
			if (AL.empty(fc.res)) {
//TODO: if not found, fallback to fuzzy search based on letter bigrams or levenstain difference			
				if (!initialSearch(session, trans, fc, input, peer)) {
					return false;
				} else if (fc.res.size() == 1) {
					askConfirmation(session,(Thing)fc.res.iterator().next());
				} else {
					if (!askQuestion(session,trans,fc)) {
//TODO: handle!				
						reset(session);
						return false;
					}
				}
				
			///////////////// keep finding /////////////////
			} else if (fc.res.size() > 1) {
				
				if (input.equals(no)) {
					session.output(session.so());
					reset(session);
					return true;
				} else
				if (fc.options != null && fc.options.size() == 1 && input.equals(yes)) {//use confirmed prompted option
					input = fc.options.iterator().next().toString();
				}
				
				//search for other attributes spec
				HashSet<Thing> textMatches = new HashSet<Thing>();
				HashSet<String> values = new HashSet<String>(); 
				Counter words = new Counter();
				LangPack.countWords(languages, words, input, null, 1, true);
				words.normalizeBy(languages.words(), 1);
				int maxMatches = 0;
				for (Object r : fc.res) {
					Thing t = (Thing)r;
					String s = t.getString(fc.keyAttribute);//TODO: other types!?
					if (AL.empty(s))
						continue;
					Set tokens = Parser.parse(s,AL.punctuation+AL.spaces,false,true,false,true);
					Counter textWords = new Counter(); 
					for (int i = 0; i < tokens.size(); i++) {
						String token = (String)tokens.get(i);
						Number n = words.value(token);
						if (n != null)
							textWords.count(token, n.doubleValue()); 
					}
					if (textWords.size() == 0 || textWords.size() < maxMatches)//have better matches already
						continue;
					if (maxMatches < textWords.size()) {//just found better matches
						maxMatches = textWords.size();
						textMatches.clear();
						values.clear();
					}
					textMatches.add(t);//keep counting matches
					values.add(s);
				}
				if (values.size() > 0)
					fc.attributes.put(fc.keyAttribute, values.size());
				if (textMatches.size() == 1) {//single match
					fc.res.retainAll(textMatches);
					askConfirmation(session,(Thing)fc.res.iterator().next());
				} else
				if (textMatches.size() > 1) {//multiple matches
					fc.res.retainAll(textMatches);
					if (!askQuestion(session,trans,fc)) {
						session.output(session.so());
					}
				} else {//no matches
					if (!askQuestion(session,trans,fc)) {
						session.output(session.so());
					}
				}
				
			} else {//res.size() == 1
//TODO: start over
				if (input.startsWith(yes)) {
					session.output("Ok.");
					reset(session);
				} else
				if (input.startsWith(no)) {
					session.output(session.so());
					reset(session);
 				} else {
 					if (fc.retries++ <= 1)
 						askConfirmation(session,(Thing)fc.res.iterator().next());
 					else {
 						reset(session);
 						return false;
 					}
				}
			}
			
		} catch (Throwable e) {
			session.output(session.no()+" "+Responser.statement(e));
			if (!(e instanceof Mistake))
				session.sessioner.body.error("Finder error " + e.toString(), e);
			reset(session);
		}
		return true;
	}

	void reset(Session session) {
		session.context(name(),null);
	}

	boolean initialSearch(Session session, Translator trans, FinderContext fc, String input, Thing peer) throws Exception {
		LangPack languages = session.getBody().languages;
		Storager storager = session.getStorager();

//TODO: extra search for "is" class being searched and keep it in context?
		
//TODO: get searchable attributes only, have them app-configured before
					
//TODO: explicitly say "name is text" to identify Full-Text-Searchable attributes only?
					
		String[] names = storager.getNames();
		//String[] has =storager.getNames(AL.has);

		//find matches in attributed strings
		Counter words = new Counter();
		LangPack.countWords(languages, words, input, null);
		words.normalizeBy(languages.words(), 1);
		HashMap<String,Counter> textMatches = new HashMap<String,Counter>();
		int maxMatches = 0;
		for (String name : names) {
			if (!searchable(name))//skip special attributes
				continue;
			Object values[] = storager.getObjects(name);
			if (!AL.empty(values)) for (Object v : values) {
//TODO dates, numbers, Things with names 
				if (!(v instanceof String))
					continue;
				String s = (String)v;
				if (AL.empty(s))
					continue;
				Collection coll = storager.getByName(name, s);
				if (AL.empty(coll))
					continue;
				HashSet things = new HashSet();
				for (Object c : coll)
					if (c instanceof Thing)
						if (((Thing)c).get(AL.has) == null && ((Thing)c).get(AL.is) != null)//if pure instance
							if (Query.accessible((Thing)c, (Thing)session.sessioner.body.getSelf(), peer, null, false))
								things.addAll(coll);
				if (things.size() == 0)
					continue;
				//Set tokens = Parser.parse(s,AL.punctuation+AL.spaces,false,true,false,true);//no quoting
				Set tokens = Parser.parse(s,AL.punctuation+AL.spaces,false,true,true,true);//quoting to avoid searching patterns 
				Counter textWords = new Counter(); 
				for (int i = 0; i < tokens.size(); i++) {
					String token = (String)tokens.get(i);
					Number n = words.value(token);
					if (n != null)
						textWords.count(token, n.doubleValue()); 
				}
				if (textWords.size() == 0 || textWords.size() < maxMatches)//have better matches already
					continue;
				if (maxMatches < textWords.size()) {//just found better matches
					maxMatches = textWords.size();
					textMatches.clear();
					fc.attributes.clear();
					fc.res.clear();
				}
				fc.res.addAll(things);
				textMatches.put(s, textWords);//keep counting matches
//TODO: use things, not value
				//fc.attributes.put(name, things.size());
				fc.attributes.put(name, values.length);
			}
		}
		return !AL.empty(fc.res);
	}
	
	boolean askQuestion(Session session, Translator trans, FinderContext fc) {
		if (askQuestionInternal(session, trans, fc))
			return true;
		for (;;) {
			String specific = null;
			int min = Integer.MAX_VALUE;
			for (String attr : fc.attributes.keySet()) {
				int cardinality = fc.attributes.get(attr);
				if (cardinality > 1 && min > cardinality) {
					specific = attr;
					min = cardinality;
				}
			}
			if (specific == null)
				break;
			fc.attributes.remove(specific);
			if (askQuestionInternal(session, trans, fc))
				return true;
		}
		//reset(session);
		//return false;
		//fallback to use the single element
		while (fc.res.size() > 1)
			fc.res.remove(fc.res.iterator().next());
		askConfirmation(session,(Thing)fc.res.iterator().next());
		return true;
	}
	
	boolean askQuestionInternal(Session session, Translator trans, FinderContext fc) {
		//find the most cardinal attribute
		HashMap<String,java.util.Set> nameValues = new HashMap<String,java.util.Set>();
		Counter thingsByAttr = new Counter();
		for (Object r : fc.res) {
			if (r instanceof Thing) {
				Thing t = (Thing)r;
				String[] t_names = t.getNamesAvailable();
				for (String name : t_names) {
					if (fc.attributes.containsKey(name))//skip resolved attribute
						continue;
					if (!searchable(name))//skip special attributes
						continue;
					String v = t.getString(name);
					if (!AL.empty(v)) {
						java.util.Set s = nameValues.get(name);
						if (s == null) {
							nameValues.put(name, s = new java.util.TreeSet());//need them sorted
						}
						s.add(v);//count unique value;
						thingsByAttr.count(name);
					}
				}
			}
		}
		thingsByAttr.normalize();//normalize to see which one covers more objects of the question
		fc.keyAttribute = null;
		int min = 0;
		fc.options = null;
		for (String n : nameValues.keySet()) {
			if (thingsByAttr.value(n).intValue() != 100)
				continue;
			java.util.Set s = nameValues.get(n);
			if (s.size() == 1)
				fc.attributes.put(fc.keyAttribute,1);
			else
			if (min == 0 || s.size() < min) {
				fc.keyAttribute = n;
				min = s.size();
				fc.options = s;
			}
		}
		if (!AL.empty(fc.options)) {
			String or = trans.loc("or");
			StringBuilder sb = new StringBuilder();
			
			sb.append(fc.keyAttribute).append(" ");
			int c = 0;
			for (Object o : fc.options) {
				if (c > 0)
					sb.append(" ").append(or).append(" ");
				c++;
				sb.append(o);
			}
			sb.append("?");
			session.output(Writer.capitalize(sb.toString()));
			return true;
		}
		return false;
	}
	
	void askConfirmation(Session session, Thing thing) {
		StringBuilder sb = new StringBuilder();
		String[] t_names = thing.getNamesAvailable();
		for (String name : t_names) {
			if (!searchable(name))//skip special attributes
				continue;
			String v = thing.getString(name);
			if (!AL.empty(v)) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(name).append(' ').append(v);
			}
		}
//TODO sb.length() == 0
		sb.append('?');
		session.output(Writer.capitalize(sb.toString()));
		//expect yes/да or no/нет
	}
	
}
