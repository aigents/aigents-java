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
package net.webstructor.gram.core;

import java.util.Arrays;

//Unique integer N-gram based name identifier 
public class Ngram {
    public int[] m_ints;

    //shallow copy
    public Ngram(int[] ints) {
    	m_ints = ints;
    }

    //deep copy
    public Ngram(Ngram ints) {
    	m_ints = new int[ints.m_ints.length];
		System.arraycopy(ints.m_ints, 0, m_ints, 0, m_ints.length);
    }

    public boolean equals(Object o) {
    	if (this == o) 
        	return true;
        if (o == null || getClass() != o.getClass()) 
        	return false;
        return (Arrays.equals(m_ints, ((Ngram)o).m_ints));
    }

    public int hashCode() {
        return m_ints != null ? Arrays.hashCode(m_ints) : 0;
    }
    
    public int[] translate(int ids[], int id) {
    	//first pass
    	//find "count" of "chances" and keep them in "findings"
    	int chances = ids.length - m_ints.length + 1;
    	int findings[] = new int[chances];
    	int count = 0;
    	for (int i=0;i<chances;) {
    		int j=0;
    		for (;j<m_ints.length;j++) {
    			if (m_ints[j]!=ids[i+j])
    				break;
    		}
    		if (j==m_ints.length) {
    			findings[count++]=i;
    			i+=m_ints.length;
    		}
    		else
    			i++;
    	}
    	if (count == 0)
    		return null;
    	int length = ids.length - count * (m_ints.length - 1);
    	int tid[] = new int[length];
    	int c=0,i=0;
    	for (int t=0;t<length;t++) {
    		if (i==findings[c] && c<count) {
    			tid[t]=id;
    			i+=m_ints.length;
    			c++;
    		}
    		else
    			tid[t]=ids[i++];
    	}
    	if (c!=count || i!=ids.length)
    		return null;
    	return tid;
    }
    
}