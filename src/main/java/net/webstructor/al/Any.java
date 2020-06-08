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

public class Any extends Set {
	protected int max;
	public Any(Object[] set) {
		this.max = set != null ? set.length : 0;//unlimited OR-set?
		this.set = set; 
	}
	public Any(int max,Object[] set) {
		this.max = max;
		this.set = set; 
	}
	public Set compact() {
		return compact(false);
	}
	public Set compact(boolean recursive) {//TODO do we need this recursion option?
		if (recursive)
			for (int i = 0; i < set.length; i++)
				if (set[i] instanceof Set)
					((Set)set[i]).compact();
		if (set.length < 2)
			return this;
		boolean[] valids = new boolean[set.length];
		valids[0] = true;
		int valid = set.length;//not a duplicates
		for (int o = 1; o < set.length; o++) {
			valids[o] = true;
			for (int t = 0; t < o; t++) if (valids[t] && compare(set[t],set[o]) == 0) {
				valids[o] = false;
				valid--;
				break;
			}
		}
		if (valid == set.length)
			return this;
		//eliminate duplicates
		Object[] set = new Object[valid];
		set[0] = this.set[0];
		int v = 1;
		for (int i = 1; i < this.set.length; i++)
			if (valids[i])
				set[v++] = this.set[i];
		this.set = set;
		return this;
	}
	public Any merge(Any other) {
		if (AL.empty(other))
			return this;
		boolean[] valids = new boolean[other.size()];
		int valid = other.size();//not a duplicates
		for (int o = 0; o < other.size(); o++) {
			valids[o] = true;
			for (int t = 0 ; t < this.size(); t++) if (compare(this.set[t],other.set[o]) == 0) {
				valids[o] = false;
				valid--;
				break;
			}
		}
		if (valid == 0)
			return this;
		Object[] set = new Object[size() + valid];
		int i=0;
		for (; i<size(); i++)
			set[i] = this.set[i];
		for (int j = 0; j < other.set.length; j++)
			if (valids[j])
				set[i++] = other.set[j];
		return new Any(set);
	}
}
