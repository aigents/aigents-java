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
import net.webstructor.core.Mistake;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;

class Answerer extends Searcher {	

	@Override
	public String name() {
		return "answer";
	}
	
	@Override
	public boolean handleIntent(final Session session){
		try {

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

		if (!AL.empty(res)) {
			Thing t = (Thing)res.iterator().next();
			String text = t.getString(AL.text);
			String source = t.getString(AL.sources);
			if (!AL.empty(source))
				text += ' ' + source;
			session.outputWithEmotions(text);
		} else
			//session.output(session.no());
			return false;

		} catch (Throwable e) {
			session.output(session.no()+" "+Responser.statement(e));
			if (!(e instanceof Mistake))
				session.sessioner.body.error("Answerer error " + e.toString(), e);
		}
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
						if (maxMatches > textWords.size())
							continue;
						if (maxMatches < textWords.size()) {
							textMatches.clear();
							maxMatches = textWords.size();
						}
						textMatches.put(thing, textWords);
					}
				}
				if (maxMatches > 0) {// && textMatches.size() > 0) {
					for (Thing t : textMatches.keySet()) {
						Counter textWords = textMatches.get(t);
						{//if (maxMatches == textWords.size()) {
							Thing found = new Thing();
//TODO: find fragments - build summary
							String text = t.getString(AL.text);
							String summary = Summarizer.summarize(textWords.keySet(),text,languages);
							String src = t.getString(AL.sources);
							if (AL.empty(src))
								src = t.getString(AL.is);
							if (AL.empty(summary)) {
								session.sessioner.body.error("Answerer no summary "+words+" "+src+" "+text, null);
								continue;
							}
							found.setString(AL.text, summary);
							found.setString(AL.sources, src);
							res.add(found);
						}
					}
				}
				if (!AL.empty(res))
					return res;
			}
		} catch (Throwable e) {
			session.sessioner.body.error("Answerer error "+session.input(), e);
		}
		return null;
	}
	
}
