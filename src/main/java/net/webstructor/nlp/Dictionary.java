/*
 * MIT License
 * 
 * Copyright (c) 2020-Present by Vignav Ramesh and Anton Kolonin, Aigents®
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
import java.util.HashSet;

public class Dictionary {
	private HashSet<Word> words;
	private static final String versionNumber = "V5v8v0+";
	private static final String locale = "EN4us+";
	
	public Dictionary() {
		words = new HashSet<>();
	}
	
	public Dictionary(HashSet<Word> words) {
		this.words = words;
	}
	
	public void addWord(Word word) {
		words.add(word);
	}
	
	public void updateWords(HashSet<Word> words) {
		this.words = words;
	}
	
	public ArrayList<Rule> getRule(String word) {
		ArrayList<Rule> rules = new ArrayList<>();
		for (Word w : words) {
			if (w.getWord().equals(word)) {
				if (!word.equals("human") && !word.equals("cake")) rules.add(w.getRule());
				else {
					if (word.equals("human") && w.getSubscript().equals("n")) rules.add(w.getRule());
					if (word.equals("cake") && w.getSubscript().equals("n-u")) rules.add(w.getRule());
				}
			}
		}
		return rules;
	}
	
	public Rule getRule(String word, boolean SmallGrammarGen) {
		for (Word w : words) {
			if (w.getWord().equals(word)) {
				return w.getRule();
			}
		}
		return null;
	}
	
	public ArrayList<String> getSubscript(String word) {
		ArrayList<String> subs = new ArrayList<>();
		for (Word w : words) {
			if (w.getWord().equals(word) && !w.getSubscript().isEmpty()) {
				subs.add(w.getSubscript());
			}
		}
		return subs;
	}
	
	public String getVersionNumber() {	return versionNumber;	}
	
	public String getLocale() {	return locale;	}
	
	public HashSet<Word> getWords() {	return words;	}
}