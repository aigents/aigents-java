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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.al.AL;

//TODO: move ArrayList inside to dispable side effects
public class OrderedStringSet extends ArrayList implements Comparable {
	private static final long serialVersionUID = -5117640205010493911L;
	private HashSet set = new HashSet(); 
	
	public OrderedStringSet(){
	}
	
	public OrderedStringSet(String seed){
		add(seed);
	}
	
	public OrderedStringSet(java.util.Set set){
		add(set);
	}
	
	public OrderedStringSet(String[] strings){
		if (!AL.empty(strings))
			for (int i = 0; i < strings.length; i++)
				add(strings[i]);
	}
	
	public boolean add(Object obj) {
		return obj instanceof String ? add((String)obj) : false;
	}
	
	public static OrderedStringSet fromStrings(Collection all){
		OrderedStringSet newOne = new OrderedStringSet();
		for (Iterator it = all.iterator(); it.hasNext();){
			String item = (String)it.next();
			newOne.add(item);
		}
		newOne.sort();
		return newOne;
	}
	
	public static OrderedStringSet mergeAllSorted(Collection all){
		OrderedStringSet newOne = new OrderedStringSet();
		for (Iterator it = all.iterator(); it.hasNext();){
			OrderedStringSet item = (OrderedStringSet)it.next();
			newOne.add(item);
		}
		newOne.sort();
		return newOne;
	}
	
	public void add(OrderedStringSet other) {
		for (Iterator it = other.iterator(); it.hasNext();)
			add((String)it.next());
	}
	
	public void add(java.util.Set other) {
		for (Iterator it = other.iterator(); it.hasNext();)
			add((String)it.next());
	}
	
	boolean	add(String str) {
		if (set.contains(str))
			return false;
		set.add(str);
		return super.add(str);
	}
	
	public OrderedStringSet sort(){
		Collections.sort(this);
		return this;
	}
	
	public int hashCode(Object o) {
		//http://stackoverflow.com/questions/113511/best-implementation-for-hashcode-method
		int hashCode = 1;
		Iterator i = this.iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
		}
		return hashCode;
	}
	
	public boolean equals(Object o) {
		return compareTo(o) == 0;
	}
	
	public String toString() {
		return toString(", ");
	}
	
	public String toString(String delimiter) {
		StringBuilder sb = new StringBuilder();
		for (Iterator it = iterator(); it.hasNext();){
			if (sb.length() != 0)
				sb.append(delimiter);
			sb.append(it.next());
		}
		return sb.toString();
	}
	
	public int compareTo(Object o) {
		if (o == null)
			return 1;//TODO: -1 ?
		if (!(o instanceof OrderedStringSet))
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		OrderedStringSet other = (OrderedStringSet)o;
		int thisS = this.size();
		int otherS = other.size();
		for (int i = 0; i < thisS && i < otherS; i++){
			int c = ((String)this.get(i)).compareTo((String)other.get(i));
			if (c != 0)
				return c;
		}
		return thisS - otherS;//TODO: otherS - thisS ? 
	}

}

