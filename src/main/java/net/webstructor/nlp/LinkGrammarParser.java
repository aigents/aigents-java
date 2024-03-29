/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents®
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
package net.webstructor.nlp;

import java.util.ArrayList;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.nlp.lg.LgEnParser;

public class LinkGrammarParser implements GrammarParser {
		
	private LgEnParser parser=null;
	
	public LinkGrammarParser(){
		try {
			parser=new LgEnParser();
			parser.loadDict();
		}catch(Exception e) {
			System.err.println("LgEnParser construction error!");
		}
	}

	@Override
	public
	Set parse(Seq tokens,Map<String,String> params) {		
		
		if (AL.empty(tokens))
			return new All(new Seq[]{});
		
		ArrayList<String> trees=parser.parseSentence(tokens,1); // Get only one tree
		
        if(trees.size()==0) {
        	return new All(new Seq[]{});
        }else {	
        	ArrayList grams = new ArrayList();
        	String tree=trees.get(0);
        	String[] links = tree.split(" ");
        	for(int i=0; i<links.length; i++) {
        		String link=links[i].replace("[", "");
        		String pureLink=link.replace("]", "");
        		String[] words = pureLink.split("-");
        		grams.add(new Seq(new String[]{words[0],words[1]}));
        	}
        	return new All(grams.toArray(new Seq[]{}));
        }	
	}


    /*
    Demo stub
    say("You parse type link, text 'tuna is a fish'!");
    get("There text tuna is a fish, tokens [tuna is a fish], grams ([tuna is] [is fish] [a fish]).");
    */
	public Set parseDemoStub(Seq tokens,Map<String,String> params) {
		//below is just a demo stub
		if (AL.empty(tokens))
			return new All(new Seq[]{});
		ArrayList grams = new ArrayList();
		int len = tokens.size();
		int from_max = len - 1;
		for (int from = 0; from < from_max; from++) {
			for (int to = from + 1; to < len; to++) {
				if (net.webstructor.util.Array.contains(new String[] {"a","the"},(String)tokens.get(to)))
					continue;//skip determiners
				grams.add(new Seq(new String[]{(String)tokens.get(from),(String)tokens.get(to)}));
				break;
			}
		}
		return new All(grams.toArray(new Seq[]{}));
	}
}
