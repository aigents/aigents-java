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

package net.webstructor.nlp.lg;

import java.util.ArrayList;

public class Rule {
	private ArrayList<String> words;
	private ArrayList<Disjunct> disjuncts;
	
	public Rule() {
		words = new ArrayList();
		disjuncts = new ArrayList();
	}
	
	public Rule(ArrayList<String> words, ArrayList<Disjunct> disjuncts) {
		this.words = words;
		this.disjuncts = disjuncts;
	}
	
	public void addWord(String word) {
		words.add(word);
		Disjunct d = new Disjunct();
		for (String connector : word.split(" & ")) {
			d.addConnector(connector);
		}
		addDisjunct(d);
	}
	
	public void addDisjunct(Disjunct disjunct) {
		disjuncts.add(disjunct);
	}
	
	public void updateWords(ArrayList<String> words) {
		this.words = words;
	}
	
	public void updateDisjuncts(ArrayList<Disjunct> disjuncts) {
		this.disjuncts = disjuncts;
	}
	
	public ArrayList<String> getWords() {	return words;	}
	
	public ArrayList<Disjunct> getDisjuncts() {	return disjuncts;	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < words.size() - 1; i++) {
			s.append("(" + words.get(i) + ") or ");
		}
		s.append("(" + words.get(words.size() - 1) + ")");
		return s.toString();
	}
}
