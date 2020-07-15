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
import java.util.HashMap;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Set;
import net.webstructor.al.Writer;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.main.Tester;

class Answerer extends Searcher {	

	@Override
	public String name() {
		return "answer";
	}
	
	@Override
	public boolean handleIntent(final Session session){
		//if (session.mood != AL.interrogation)
		//	return false;
		String query = session.input();
		SearchContext sc = new SearchContext(query, session.getPeer(), "any",1);
		session.getPeer();
		Collection res = null;
		if (AL.empty(res))
			res = searchSTM(session, sc);
		if (AL.empty(res))
			res = searchLTM(session, sc);//it creates persistent objects!!!???
		
		if (AL.empty(res)) {
			sc.days = session.getBody().self().getInt(Body.attention_period, 10);
			res = searchSTMwords(session, sc);
		}
		
		if (AL.empty(res) && sc.peer != null && Peer.paid(sc.peer))
			res = searchEngine(session, sc);

		if (res != null) {
			Thing t = (Thing)res.iterator().next();
			String text = t.getString(AL.text);
			String source = t.getString(AL.sources);
			if (!AL.empty(source))
				text += ' ' + source;
			session.outputWithEmotions(text);
		} else
			session.output(session.no());
		return true;
	}

	Collection searchSTMwords(Session session, final SearchContext sc) {
		/*
 		- Run the following for every searcher, until non-empty response is given
			- Tokenize list of words
			- Weight the words with their surprisingness (1/count)
			- Find the text most relevant to the question
				- Count like in Searcher?
				- Iterate over weighted surprising wors
					- Iterate over texts with every word 
					- "count" every text matching every word using surprisingness as a double (!!!) count value 
					- if no texts found, keep iterating with less surprising word
					- if one text is found - return it
					- if more than one text is found, select the most counted
				- Have the above as a function and compare performance 
			- If more than one text, compute the dual relevance
				- maintain Summator dialogInputSTM, dialogOutputSTM in as session with decay "dialog decay" ... of the older terms 
				- The most relevant to the context of the outer input in Session's Summator dialogInputSTM
				- The least relevant to the context of the innet output in Session's Summator dialogOutputSTM
		 */
		LangPack languages = session.getBody().languages;
		Counter words = new Counter();
		LangPack.countWords(languages, words, sc.topic, null);
		words.normalizeBy(languages.words(), 1);
		if (words.size() < 1)
			return null;
		try {
			Storager storager = session.getStorager();
			java.util.Set texts = storager.getValuesSet(AL.text);
//TODO eliminate full text search with index search on daily updated graph!? 
			if (!AL.empty(texts)){
				Collection res = new ArrayList();
				//iterate over collection of texts, count all matches
				HashMap<Thing,Counter> textMatches = new HashMap<Thing,Counter>();
				int maxMatches = 0;
				for (Iterator it = texts.iterator(); it.hasNext();){
					String text = (String)it.next();
					Collection things = storager.getByName(AL.text, text);
					if (!AL.empty(things)) {	
						Thing thing = (Thing)things.iterator().next();
						Set tokens = Parser.parse(text,AL.punctuation+AL.spaces,false,true,false,true);
						Counter textWords = new Counter(); 
						for (int i = 0; i < tokens.size(); i++) {
							String token = (String)tokens.get(i);
							Number n = words.value(token);
							if (n != null)
								textWords.count(token, n.doubleValue()); 
						}
						textMatches.put(thing, textWords);
						if (maxMatches < textWords.size())
							maxMatches = textWords.size();
					}
				}
				if (maxMatches > 0 && textMatches.size() > 0) {
					for (Thing t : textMatches.keySet()) {
						Counter textWords = textMatches.get(t);
						if (maxMatches == textWords.size()) {
							Thing found = new Thing();
							String src = t.getString(AL.sources);
							if (AL.empty(src))
								src = t.getString(AL.is);
							found.setString(AL.sources, src);
//TODO: find fragments - build summary
							found.setString(AL.text,summarize(textWords.keySet(),t.getString(AL.text)));
							res.add(found);
						}
					}
				}
				if (!AL.empty(res))
					return res;
			}
		} catch (Throwable e) {
			session.sessioner.body.error("Searcher "+session.input(), e);
		}
		return null;
	}
	
//TODO remove hack		
	String summarize1(java.util.Set words, String text) {
		return text;
	}

	public static String summarize(java.util.Set words, String text) {
		//IDEA A:
		//1) create list of "seeds" for every postions of "words", with map position->missedSeeds
		//2) start expanding every seed to right and left over tokens till any seed has zero missedSeeds
		//3) get the first seeds with zero missedSeeds and expand it to left and right sentence breaks
		//4) glue up the results into text

		//IDEA B
		//1) compute multiplicative heat map based on distribution of n_words matched words over n_tokens;
		//Set tokens = Parser.parse(text,AL.punctuation+AL.spaces,false,true,false,true);
		Set tokens = Parser.parse(text);
		double total_heats[] = null;
		int n_tokens = tokens.size();
		int n_words = 0;//words.size();
		for (Object w : words) {
			boolean heated = false;
			double sub_heats[] = new double[tokens.size()];
			int i;
			for (i = 0; i < n_tokens; i++)
				sub_heats[i] = 0;
			for (i = 0; i < n_tokens; i++) {//check every token
				String token = (String)tokens.get(i);
//TODO: consider levenstain difference for measure
				if (token.equals(w)) {
					heated = true; 
					sub_heats[i] = 1;
					for (int iminus = 1; i - iminus >= 0; iminus++)
						sub_heats[i - iminus] += 1.0 / (1 + iminus);
					for (int iplus = 1; i + iplus < n_tokens; iplus++)
						sub_heats[i + iplus] += 1.0 / (1 + iplus);
				}
			}
			if (heated) {
				n_words++;
				if (total_heats == null)
					total_heats = sub_heats;
				else
					for (int h = 0; h < n_tokens; h++)
						total_heats[h] *= sub_heats[h];
			}
		}
		if (n_words == 0)
			return null;
		//2) find the first greatest hot spot
		int start = -1; 
		int end = -1;
		double max = 0;
		for (int i = 0; i < n_tokens; i++) {
			if (max < total_heats[i]) {
				max = total_heats[i];
				start = i;
			}
		}
		end = start; 
		//3) find n_words-1 hottest spots nearby the greatest hot spot
		int new_start = start;
		int new_end = end;
		for (int found = 1; found < n_words; ) {
			//try left
			if (new_start > 0) {
				new_start--;
				//searching for extremum on the left 
				if (total_heats[new_start] >= total_heats[new_start + 1] && (new_start - 1 < 0 || total_heats[new_start] >= total_heats[new_start - 1])) {
					if (++found == n_words) {
						start = new_start;
						break;
					}
				}
			}
			//try right
			if (new_end < n_tokens - 1) {
				new_end++;
				//searching for extremum on the right 
				if (total_heats[new_end] >= total_heats[new_end - 1] && (new_end + 1 >= n_tokens || total_heats[new_end] >= total_heats[new_end + 1])) {
					if (++found == n_words) {
						end = new_end;
						break;
					}
				}
			}
		}
		//4) well-form the sentence
		for (; start > 0 && AL.periods.indexOf((String)tokens.get(start - 1)) == -1; start--);
		for (; end < n_tokens - 1 && AL.periods.indexOf((String)tokens.get(end)) == -1; end++);
		//6) glue things up
		StringBuilder sb = new StringBuilder();
		boolean capitalize = true;
		for (int i = start; i <= end; i++) {
			String t = (String)tokens.get(i);
			boolean delimiter = t.length() == 1 && AL.delimiters.indexOf(t) != -1;
			if (i != start && !delimiter)
				sb.append(" ");
			sb.append(capitalize ? Writer.capitalize(t) : t);
			capitalize = t.length() == 1 && AL.periods.indexOf(t) != -1;
		}
		return sb.toString();
	}

	public static void main(String args[]) {
		Tester t = new Tester();
		java.util.Set w = new java.util.HashSet();
		w.add("aliens");
		w.add("homeland");
		String s;
		s = summarize(w,"The universe is the homeland of aliens who came to earth.");
		t.assume(s, "The universe is the homeland of aliens who came to earth.");
		s = summarize(w,"Here we go. The universe is the homeland of aliens who came to earth. Here we are.");
		t.assume(s, "The universe is the homeland of aliens who came to earth.");
		s = summarize(w,"Here is the universe. The universe is the homeland of aliens who came to earth. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of aliens who came to earth.");
		s = summarize(w,"Here are the aliens. Here is their homeland. Here you go.");
		t.assume(s, "Here are the aliens. Here is their homeland.");
		s = summarize(w,"Everyone has homeland. The universe is the homeland of aliens. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of aliens.");
		s = summarize(w,"Here are the humans. Here is their homeland. Here you go.");
		t.assume(s, "Here is their homeland.");
		s = summarize(w,"Here are the humans. Here is their home. Here you go.");
		t.assume(s, null);
		s = summarize(w,"Here are the alients. Here is their homeland. Here you go.");//TODO: fuzzy word matching with bigrams or levenstain distance!?
		t.assume(s, "Here is their homeland.");
		s = summarize(w,"Here is the home of animals. Here is the home of aliens. That's it.");
		t.assume(s, "Here is the home of aliens.");
		s = summarize(w,"The universe is the homeland of life. Like aliens, for instance. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of life. Like aliens, for instance.");
		s = summarize(w,"The universe is the homeland of life. Aliens, for instance. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of life. Aliens, for instance.");
		s = summarize(w,"We live in the universe. The universe is the homeland of the alien forms of life. These are called aliens. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of the alien forms of life. These are called aliens.");
		
		w.clear();
		w.add("россию");
		w.add("душат");
		s = summarize(w,"246 россию душат столетиями! Соловьев в шоке! Резонансное выступление жириновского.");
		t.assume(s, "246 россию душат столетиями!");
		
		//System.out.println(s);
		
		t.check();
	}
	
}
