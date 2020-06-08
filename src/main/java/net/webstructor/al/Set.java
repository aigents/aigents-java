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

import java.util.Collection;
import java.util.Iterator;

public abstract class Set extends Term implements Comparable {
	protected Object[] set;
	public int size() {
		return set.length;
	}
	public Object get(int i) {
		return set[i];
	}
	public void set(int i, Object o) {
		set[i] = o;
	}
	public String[] toStrings() {
		if (set == null)
			return null;
		String[] strings = new String[set.length];
		for (int i = 0; i < set.length; i++){
			Object o = set[i];
			if (!(o instanceof String))
				return null;
			strings[i] = (String)o;
		}
		return strings;
	}
	public static Object[] toArray(Collection coll) {
		Object[] a = new Object[coll.size()];
		Iterator it = (Iterator)coll.iterator();
		int c = 0;
		while (it.hasNext()) 
			a[c++] = it.next();
		return a;
	}
	@Override
	public int compareTo(Object o) {
		if (o == null)
			return 1;//TODO: -1 ?
		if (!(o instanceof Set))
			return this.toString().compareTo(o.toString());
		Set other = (Set)o;
		int thisS = this.size();
		int otherS = other.size();
		if (thisS != otherS)
			return thisS - otherS;//TODO: otherS - thisS ? 
		for (int i = 0; i < thisS && i < otherS; i++){
			Object thisO = this.get(i);
			Object otherO = other.get(i);
			int c = compare(thisO,otherO);
			if (c != 0)
				return c;
		}
		return 0;
	}
	public static int compare(Object thisO, Object otherO) {
		if (thisO == otherO)
			return 0;
		if (otherO == null)
			return 1;//TODO: -1 ?
		if (thisO == null)
			return -1;//TODO: +1 ?
		int c = thisO instanceof Set ? ((Set)thisO).compareTo(otherO) :
			otherO instanceof Set ? -((Set)otherO).compareTo(thisO) :
				thisO.toString().compareTo(otherO.toString());
		return c;
	}
//TODO: make this 
	public Set compact() {
		return this;//no action by default
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		//if (!AL.empty(set))
			sb.append(this instanceof Seq ? '[' : this instanceof Any ? '{' : '(');
		if (!AL.empty(set))
		for (int i = 0 ; i < set.length; i++){
			if (i > 0)
				sb.append(' ');
			Object term = set[i];
//TODO make sure why empty terms can be possible if it they shouyld be quotable properly!?
			sb.append(term instanceof String ? Writer.toString(term) : term.toString());
		}
		//if (!AL.empty(set))
			sb.append(this instanceof Seq ? ']' : this instanceof Any ? '}' : ')');
		return sb.toString();
	}
	public Set toLower(){
		for (int i = 0 ; i < set.length; i++){
			Object o = set[i];
			if (o instanceof String)
				set[i] = ((String)o).toLowerCase();
			else
			if (o instanceof Set)
				((Set)o).toLower();
		}
		return this;
	}
}
