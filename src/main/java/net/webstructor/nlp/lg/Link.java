/*
 * MIT License
 * 
 * Copyright (c) 2021-2021 by Eugene Bochkov, Vignav Ramesh and Anton Kolonin, Aigents®
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


public class Link {
	
	public int w1Index = 0;
	public int w2Index = 0;
	
	public Link(int wi1, int wi2) {
		w1Index=wi1;
		w2Index=wi2;
	}
			
	@Override            
	public boolean equals(Object me) {
		Link link = (Link)me;
		if((this.w1Index==link.w1Index) && (this.w2Index==link.w2Index))
			return true;
		else
			return false;
	}
	
	@Override            
	public int hashCode() {
		return (this.w1Index<<16) | (this.w2Index);
	}
	
	@Override            
	public String toString() {
		return w1Index+"-"+w2Index;
	}
}

