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

public class StringItem extends Item {
    private Ngram m_ids;
   
	public StringItem(int id, float evidence, Ngram ids)
	{
		m_posEvidence = evidence;
		m_id = id;		
		m_ids = new Ngram(ids);
	}
	
	public Object getName() {
		return new Ngram(m_ids);
	}

	public int getType() {
    	return MemoryType.STRING;
	}

	public int getArity() {
		return m_ids.m_ints.length;
	}

	public int[] getIds() {
		return m_ids.m_ints;
	}

	public Item clone(int id) {
		return new StringItem(id, 0, m_ids);
	}

    public boolean equals(Object o) {
    	return o.getClass() == getClass() && ((StringItem)o).m_ids.equals(m_ids); 
    }

    public String toString(MemoryStore store, String open, String inner, String close) {
		StringBuffer sb = new StringBuffer();
		if (open!=null)
			sb.append(open);
		if (m_ids!=null)
			for (int i=0;i<m_ids.m_ints.length;i++)
			{
				if (i!=0 && inner!=null)
					sb.append(inner);
				sb.append(store.getItem(m_ids.m_ints[i]).toString(store,open,inner,close));
			}
		if (close!=null)
			sb.append(close);
		return sb.toString();
	}

}
