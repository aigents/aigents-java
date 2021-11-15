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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Vector;

import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Parser;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.gram.core.Item;
import net.webstructor.gram.core.MemoryStore;
import net.webstructor.gram.core.StringItem;
import net.webstructor.gram.main.LexStructor;
import net.webstructor.gram.util.Format;
import net.webstructor.gram.util.StringUtil;

public class NgramTreeParser implements GrammarParser {
	String storePath = "./data/default/ngramtree.txt";//./data/<default|en|ru>/ngramtree.txt
	MemoryStore store = new MemoryStore(null);
	
	public NgramTreeParser() {
    	File fi = new File(storePath);
        if (fi.exists() && fi.isFile())
        	store.load(storePath);
	}
	
	public void save() {
		//TODO save on exit
		store.save(storePath,true);
	}
	
	@Override
	public Set parse(Seq tokens,Map<String,String> params) {
		//TODO parameters?
		//boolean incremental = Array.contains(args, "incremental");
		//String skip = Str.arg(args, "skip", null);

		StringItem ngram = LexStructor.seq2Ngram(store, tokens, false/*bWalls*/, tokens.toString());//don't generate LEFT-WALL|RIGHT-WALL

//TODO replace with List 
		Vector toReplaceWithList = new Vector();
		toReplaceWithList.add(ngram);

//TODO learnNgramsFromVector(v,store,2,2,true,true,false,incremental);//all=true|false(left)
		LexStructor.learnNgramsFromVector(toReplaceWithList,store,2,2,true,true,false,0,null);
		
		if (toReplaceWithList.size() > 0) {
			ArrayList words = new ArrayList();
			ArrayList links = new ArrayList();
			Item item = (Item)toReplaceWithList.get(0);
			//get DTree (Dependency Tree or Dependency Grammar - DG) from CTree (Constituent Tree or Phrase Structure Grammar - PSG) 
			if (item.getArity() > 0) {
//TODO: use less frequent word to be the link source instead of the RHS
				LexStructor.getParseWords(store,item,words,links);//use Right Hand Side (RHS) words in a tree to link
				Collections.sort(links, new Comparator(){
					@Override
					public int compare(Object o1, Object o2) {
						int[] l1 = (int[])o1;
						int[] l2 = (int[])o2;
						return l1[0] < l2[0] ? -1 : l1[0] > l2[0] ? 1 : l1[1] < l2[1] ? -1 : l1[1] > l2[1] ? 1 : 0;
					}});
				ArrayList grams = new ArrayList();
				for (int l = 0; l < links.size(); l++){
					int[] link = (int[])links.get(l);
					String w0 = (String)words.get(link[0]);
					String w1 = (String)words.get(link[1]);
					grams.add(new Seq(new String[]{w0,w1}));
				}
System.out.println(StringUtil.toString(item.getIds(),store,Format.m_openArrayDelim,Format.m_closeArrayDelim));
//TODO remove saving here, save on quit/exit
//TODO make working!!!
				save();
				return new All(grams.toArray(new Seq[]{}));
			}
		}
		return null;
	}
	
	public String parse(String text) {
		Seq tokens = Parser.parse(text,AL.commas+AL.periods+AL.spaces,false,true,true,true);
		Set parse = parse(tokens,null);
		return parse.toString();
	}
	
	public static void main(String args[]) {
		//test
		NgramTreeParser ntp = new NgramTreeParser();
		System.out.println(ntp.parse("a fish"));
		System.out.println(ntp.parse("is a fish"));
		System.out.println(ntp.parse("tuna is a fish"));
		
	}
}
