/*
 * MIT License
 * 
 * Copyright (c) 2015-2019 by Anton Kolonin, Aigents®
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

import java.util.Comparator;
import java.util.Date;

import net.webstructor.al.AL;
import net.webstructor.core.Thing;

public class ThingComparator implements Comparator {
	private String name = AL.name;
	private boolean asc = true;
	public ThingComparator(String name,boolean asc){
		this.name = name;
		this.asc = asc;
	}

	private int cmp(Object o0, Object o1){
		float d =
				(o0 == null && o1 == null) ? 0 : (o0 == null) ? -1 : (o1 == null) ? 1 : //asc? 
				(o0 instanceof String && o1 instanceof String) ? ((String)o0).compareTo((String)o1) : //asc
				(o0 instanceof Integer && o1 instanceof Integer) ? ((Integer)o1).compareTo((Integer)o0) : //desc
				(o0 instanceof Float && o1 instanceof Float) ? ((Float)o1).compareTo((Float)o0) : //desc
				(o0 instanceof Date && o1 instanceof Date) ? ((Date)o0).compareTo((Date)o1) : //asc
				o0.toString().compareTo(o1.toString());
		int i = d < 0 ? -1 : d > 0 ? 1 : 0;
		return asc ? i : -i;
	}

	public int compare(Object arg0, Object arg1) {
		Object a0 = ((Thing)arg0).get(name);
		Object a1 = ((Thing)arg1).get(name);
		return cmp(a0,a1);
		//TODO: multi-key sorting
		/*
		if (d != 0)
			return d;
		if (pos2 != -1) //breaking ties?
			d = cmp(a0[pos2],a1[pos2]);
		*/
	}
}
