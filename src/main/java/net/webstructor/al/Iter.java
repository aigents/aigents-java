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
package net.webstructor.al;

public class Iter {
	Set set;
	int pos;
	public Iter(Set set) {
		pos = 0;
		this.set = set;
	}
	public void pos(int pos) {
		this.pos = pos;//TODO: validation
	}
	public int cur() {
		return pos;
	}
	public Object get() {//TODO:can be String or Set, right?
		return has() ? set.get(pos): null;
	}
	public Object next() {//TODO:can be String or Set, right?
		return has() ? set.get(pos++): null;
	}
	public boolean has() {
		return pos < set.size();
	}
	public int size() {
		return set.size();
	}
	public String toString() {
		return set != null && pos < set.size() ? set.get(pos).toString() : "null";
	}
}
