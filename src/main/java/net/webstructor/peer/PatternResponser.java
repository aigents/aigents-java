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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import net.webstructor.al.AL;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Writer;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;

class PatternResponser implements Intenter {	

	@Override
	public String name() {
		return "natural";
	}
	
	@Override
	public boolean handleIntent(final Session session){
		//TODO:
		// PATTERN<=M:M=>THING<=M:M=>RESPONSE
		// Idealistic scenario:
		//1) get all things that have responses AND patterns
		//2) get all possible patterns for these things
		//3) match session input against all patterns
		//4) find most suitable patterns
		//5) pick random patterns across multiple suitable patterns
		//6) find most suitable things for the picked pattern
		//7) get all responses for the most suitable things 
		//8) find most suitable responses for the most suitable things
		//9) pick random response across multiple suitable respnoses
		//10) fill the variables in response
		//11) return the response
//TODO: use safer operations and clone collections
		//1-2
		Storager storager = session.getStorager();
		java.util.Set responses = storager.getValuesSet(AL.responses);
		HashSet all_patterns = new HashSet();
		if (!AL.empty(responses)) for (Iterator it = responses.iterator(); it.hasNext();){
			Object response = it.next();
			Set reactions = storager.get(AL.responses, response);
			if (!AL.empty(reactions)) for (Iterator rit = reactions.iterator(); rit.hasNext();){
				Thing ra = (Thing)rit.next();
				Collection ps = ra.getThings(AL.patterns);
				if (!AL.empty(ps)) for (Iterator pit = ps.iterator(); pit.hasNext();){
					Thing p = (Thing)pit.next();
					all_patterns.add(p);
				}
			}
		}
		//3
		//HashSet all_responses = new HashSet();
		ArrayList all_responses = new ArrayList();
		int matchlen = 0;
		Iter iter = new Iter(Parser.parse(session.input()));//build with original text positions preserved for image matching
		if (!AL.empty(all_patterns)) for (Iterator it = all_patterns.iterator(); it.hasNext();){
			iter.pos(0);//reset 
			Thing instance = new Thing();
			Thing patthing = ((Thing)it.next());
			Seq patseq = Reader.pattern(storager,instance,patthing.name());
			StringBuilder sbtmp = new StringBuilder();
			boolean read = Reader.read(iter, patseq, sbtmp);
			if (read){
				session.sessioner.body.debug(session.toString()+" X "+patthing.name()+" = "+sbtmp.toString());
			}
			if (read && sbtmp.length() > matchlen){//if found better match
				matchlen = sbtmp.length();
				all_responses.clear();
				java.util.Set reactions = storager.get(AL.patterns, patthing);
				if (!AL.empty(reactions)) for (Iterator rit = reactions.iterator(); rit.hasNext();){
					Thing ra = (Thing)rit.next();
					Collection rs = ra.getThings(AL.responses);
					if (!AL.empty(rs)) for (Iterator rsit = rs.iterator(); rsit.hasNext();){
						Thing r = (Thing)rsit.next();
						all_responses.add(r);
					}
				}
			}
		}
		//for (Iterator it = all_responses.iterator(); it.hasNext();){
		//	Thing r = (Thing)it.next();
		if (all_responses.size() > 0){
			int i = new Random().nextInt(all_responses.size());
			Thing r = (Thing)all_responses.get(i);
			String s = r.name();
			if (!AL.empty(s)){
				s = Writer.capitalize(s);
				if (!AL.periods.contains(s.substring(s.length()-1)))
					s = s + ".";
				session.outputWithEmotions(s);
				return true;
			}
		}
		return false;
	}

}
