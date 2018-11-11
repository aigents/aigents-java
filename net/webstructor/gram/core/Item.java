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

/**
 * "Never increase, beyond what is necessary, the number of entities required to explain anything" --- William of Ockham (1285-1349)
 *
 * @author Anton
 *
 */
public abstract class Item 
{
    protected int m_id;
    protected float m_posEvidence = 0; 
    public int getId()
    {
    	return m_id;
    }
    public float getEvidence()
    {
    	return m_posEvidence;
    }
    public float incEvidence()
    {
    	return ++m_posEvidence;
    }
    public float incEvidence(float delta)
    {
    	return m_posEvidence+=delta;
    }
    public abstract Object getName();
    //public abstract String getLiteralName();
    public abstract int getType();
    public abstract int getArity();
    public abstract int[] getIds();
    public abstract Item clone(int id);
	public abstract String toString(MemoryStore store,String open,String inner,String close);
}
