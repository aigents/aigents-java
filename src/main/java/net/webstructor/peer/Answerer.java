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

import net.webstructor.al.AL;
import net.webstructor.core.Thing;

class Answerer extends Searcher {	

	@Override
	public String name() {
		return "answer";
	}
	
	@Override
	public boolean handleIntent(final Session session){
		//if (session.mood != AL.interrogation)
		//	return false;
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

//TODO: tokenize, unscrub, turn into disjuction?
		String query = session.input();
		SearchContext sc = new SearchContext(query, session.getPeer(), "any");
		session.getPeer();
		Collection res = null;
		if (AL.empty(res))
			res = searchSTM(session, sc);
		if (AL.empty(res))
			res = searchLTM(session, sc);//it creates persistent objects!!!???
		if (AL.empty(res) && sc.peer != null && Peer.paid(sc.peer))
			res = searchEngine(session, sc);

//TODO a lot!
		
		if (res != null) {
			Thing t = (Thing)res.iterator().next();
			String text = t.getString(AL.text);
			String source = t.getString(AL.sources);
			//String image = t.getString(AL.image);
			if (!AL.empty(source))
				text += ' ' + source;
			session.outputWithEmotions(text);
		} else
			session.output(session.no());
		return true;
	}

}
