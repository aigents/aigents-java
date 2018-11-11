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

public class LinkItem extends Item 
{
	private Long m_ids;
	
	public static Long getLong(int id1,int id2)
	{
		return new Long( (((long)id1)<<32)|id2 );
	}
	
	public LinkItem(int id, float evidence, Long ids)
	{
		m_posEvidence = evidence;
		m_id = id;		
		m_ids = ids; 
	}
	
	public LinkItem(int id, float evidence, int id1, int id2)
	{
		this(id,evidence,getLong(id1,id2));
	}
	
	public Object getName()
	{
		return m_ids;
	}

	public String toString(MemoryStore store,String open,String inner,String close)
	{
    	long l = m_ids.longValue();
		int id1 = (int)(l>>32);
		int id2 = (int)(l&0xFFFFFFFF);
		StringBuffer sb = new StringBuffer();
		if (open!=null)
			sb.append(open);
		sb.append(store.getItem(id1).toString(store,open,inner,close));
		if (inner!=null)
			sb.append(inner);
		sb.append(store.getItem(id2).toString(store,open,inner,close));
		if (close!=null)
			sb.append(close);
		return sb.toString();
	}

    public int getType()
    {
    	return MemoryType.LINK;
    }
    
    public int getArity()
    {
    	return 2;
    }
    
    public int[] getIds()
    {
    	long l = m_ids.longValue();
    	return new int[] {(int)(l>>32),(int)(l&0xFFFFFFFF)};
    }
    
    public Item clone(int id)
    {
    	return new LinkItem(id, 0, m_ids);
    }
    
    public boolean equals(Object o) {
    	return o.getClass() == getClass() && ((LinkItem)o).m_ids.equals(m_ids); 
    }
}
