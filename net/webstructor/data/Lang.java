/*
 * MIT License
 * 
 * Copyright (c) 2005-2018 by Anton Kolonin, Aigents
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
package net.webstructor.data;

public class Lang {
	String prefix;
	String name;
	String vowels;
	String consonants;
	String spec;
	String[] scrubs;
	public Lang(String prefix,String name,String vowels,String consonants,String spec, String[] scrubs){
		this.prefix = prefix;
		this.name = name;
		this.vowels = vowels;
		this.consonants = consonants;
		this.spec = spec;
		this.scrubs = scrubs;
	}
	static char obfuscate(String s,char c){
		int i = s.indexOf(c);
		return i == -1 ? 0 : s.charAt( s.length() - 1 - i ); 
	}
	char obfuscate(char c){
		char r = obfuscate(vowels,c);
		return r != 0 ? r : obfuscate(consonants,c);
	}
	boolean has(char c){
		return vowels.indexOf(c) != -1 || consonants.indexOf(c) != -1 || spec.indexOf(c) != -1;
	}
}
