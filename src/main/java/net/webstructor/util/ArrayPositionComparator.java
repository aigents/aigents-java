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
package net.webstructor.util;

import java.util.Comparator;

public class ArrayPositionComparator implements Comparator {
	private int pos = 0;
	private int pos2 = -1;
	boolean asc = true;
	public ArrayPositionComparator(int pos,boolean asc){
		this.pos = pos;
		this.asc = asc;
	}
	public ArrayPositionComparator(int pos){
		this.pos = pos;
	}
	public ArrayPositionComparator(int pos,int pos2){
		this.pos = pos;
		this.pos2 = pos2;
	}
	private int cmp(Object o0, Object o1){
		float d = 
				(o0 instanceof String && o1 instanceof String) ? //asc
					d = ((String)o0).compareTo((String)o1) :
				(o0 instanceof Integer && o1 instanceof Integer) ?
					d = ((Integer)o1).intValue() - ((Integer)o0).intValue() : //desc
				(o0 instanceof Float && o1 instanceof Float) ?
					d = ((Float)o1).floatValue() - ((Float)o0).floatValue() : //desc
				0;
		int i = d < 0 ? -1 : d > 0 ? 1 : 0;
		return asc ? i : -i;
	}
	public int compare(Object arg0, Object arg1) {
		Object[] a0 = (Object[])arg0;
		Object[] a1 = (Object[])arg1;
		int d = cmp(a0[pos],a1[pos]);
		if (d != 0)
			return d;
		if (pos2 != -1) //breaking ties?
			d = cmp(a0[pos2],a1[pos2]);
		return asc ? d : -d;
	}
}
