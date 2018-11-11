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

public class CharacterItem extends Item 
{
	private Character m_character;
	
	public CharacterItem(int id, float evidence, Character character)
	{
		m_id = id;
		m_posEvidence = evidence;
		m_character = character; 
	}
	
	public Object getName()
	{
		return m_character;
	}

    //public String getLiteralName()
    //{
    //	return String.valueOf(m_character);
    //}
    
	public String toString(MemoryStore store,String open,String inner,String close)
	{
		return String.valueOf(m_character);
	}
	
    public int getType()
    {
    	return MemoryType.CHARACTER;
    }
    
    public int getArity()
    {
    	return 0;
    }
    
    public int[] getIds()
    {
    	return null;
    }

    public Item clone(int id)
    {
    	return new CharacterItem(id, 0, m_character);
    }
    
    public boolean equals(Object o) {
    	return o.getClass() == getClass() && ((CharacterItem)o).m_character.equals(m_character); 
    }
}
