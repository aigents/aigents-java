/*
 * MIT License
 * 
 * Copyright (c) 2021-Present by Eugene Bochkov, Vignav Ramesh and Anton Kolonin, Aigents®
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

package net.webstructor.nlp;

import java.util.ArrayList;
import java.util.HashSet;

public class Linkage {
	
	public ArrayList<Link> linkList=new ArrayList<Link>();
		
	public void addLink(Link link) {
		linkList.add(link);
	}
	
	public int length(){
		return linkList.size();
	}
	
	public String toString() {
		String str="";
		for(Link link: linkList) {
			str+="["+link.w1Index+"-"+link.w2Index+"]" ;
		}	
		return str;
	}
	
	public boolean isAllWordsCanBeConnected(int sentenceLen) {
		boolean result=false;
		HashSet<Integer> indexes = new HashSet<>();
		for(Link link: linkList) {
			indexes.add(link.w1Index);
			indexes.add(link.w2Index);
		}
		if(indexes.size()==sentenceLen)
			result=true;
		
		return result;
	}
}
