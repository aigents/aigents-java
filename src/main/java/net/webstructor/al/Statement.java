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
package net.webstructor.al;

public class Statement extends Seq {
	int mood;
	public Statement(Object[] set) {
		super(set);
		this.mood = AL.declaration;
		is(true);//not a negation by default
	}
	public Statement(int mood,boolean is,Object[] set) {
		super(set);
		this.mood = mood;
		is(is);//not a negation
	}
	public boolean complex0() {
		if (size() > 1)
			return true;
		Object first = get(0);
		if (first instanceof Seq) {//just a key-value pair
			/*Seq seq = (Seq)first;
			if (seq.size() > 1){
				Object name = seq.get(0);
				if (!(AL.name.equals(name) || AL.is.equals(name)))
					return true;
			}*/
			return false;
		}
		else //All/Any
			return true;
	}
	public boolean complex() {
		if (size() < 1)
			return false;
		Object first = get(0);
		if (first instanceof Seq) {//just a key-value pair
			if (size() < 2)
				return false;
			if (size() > 2)
				return true;
			Seq seq = (Seq)first;
			Object second = get(1);
			if (seq.size() == 2 && AL.name.equals(seq.get(0)) && second instanceof String[] && ((String[])second).length == 1)
				return false;
			return true;
		}
		else //All/Any
			return true;
	}
}

