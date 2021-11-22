/*
 * MIT License
 * 
 * Copyright (c) 2020-2021 by Vignav Ramesh and Anton Kolonin, Aigents®
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

public class Word {
	private String word, subscript;
	private Rule rule;
	
	public Word(String word, Rule rule) {
		this.word = word;
		this.rule = rule;
		this.subscript = "";
	}
	
	public Word(String word, Rule rule, String subscript) {
		this.word = word;
		this.rule = rule;
		this.subscript = subscript;
	}
	
	public Word(String word) {
		this.word = word;
		this.subscript = "";
		rule = new Rule();
	}
	
	public Word(String word, String subscript) {
		this.word = word;
		this.subscript = subscript;
		rule = new Rule();
	}
	
	public void addRule(String rule) {
		this.rule.addWord(rule);
	}
	
	public boolean containsRule(String rule) {
		return this.rule.getWords().contains(rule);
	}
	
	public void updateRule(Rule rule) {
		this.rule = rule;
	}
	
	public String getWord() {	return word;	}
	
	public Rule getRule() {		return rule;	}
	
	public String getSubscript() {	return subscript;	}
	
	@Override
	public String toString() {
		return word + (subscript.equals("")? "" : "." + subscript) + ": " + rule;
	}
}