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

import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;

import net.webstructor.gram.util.StringUtil;
import net.webstructor.gram.util.Format;

public class MemoryStore 
{
	/* autoincrementing top id */
	private int m_topId = 0;
	
	private HashMap m_byName = new HashMap();
	private HashMap m_byId = new HashMap();
	
	public MemoryStore(String path)
	{
		load(path);
	}
	
	public Item getItem(Object name)
	{
		return (Item)m_byName.get(name);
	}

	public Item getItem(int id)
	{
		return (Item)m_byId.get(new Integer(id));
	}
	
	public Item addItem(Item i)
	{
		m_byId.put(new Integer(i.getId()),i);
		m_byName.put(i.getName(),i);
		return i;
	}
	
	public int encounterCharacter(char character)
	{
		Character c = new Character(character);
		Item i = getItem(c);
		if (i==null)
		{
			++m_topId;
			i = addItem(new CharacterItem(m_topId,0,c));
		}
		i.incEvidence();
		return i.getId();
	}

	public Item getLinkItem(int id1, int id2)
	{
		Long l = LinkItem.getLong(id1,id2);
		return getItem(l);
	}
	
	public int getLinkId(int id1, int id2)
	{
		Long l = LinkItem.getLong(id1,id2);
		Item i = getItem(l);
		return i==null? 0: i.getId();
	}
	
	public int encounterLink(int id1, int id2)
	{
		Long l = LinkItem.getLong(id1,id2);
		Item i = getItem(l);
		if (i==null)
		{
			++m_topId;
			i = addItem(new LinkItem(m_topId,0,l));
		}
		i.incEvidence();
		return i.getId();
	}
	
	public int encounterNgram(Ngram ngram,float evidence)
	{
		Item i = getItem(ngram);
		if (i==null)
		{
			++m_topId;
			i = addItem(new StringItem(m_topId,0,ngram));
		}
		i.incEvidence(evidence);
		return i.getId();
	}
	
    class IteratorEnumeration implements Enumeration
    {
    	private Iterator m_it;
    	IteratorEnumeration(Iterator it)
    	{
    		m_it = it;
    	}
    	public boolean hasMoreElements()
    	{
    		return m_it.hasNext();
    	}
    	public Object nextElement()
    	{
    		return m_it.next();
    	}
    }
    
    public Enumeration getEnumeration()
    {
    	return new IteratorEnumeration(m_byId.values().iterator());
    }
	
	public void load(String path)
    {
		if (path == null)
			return;
    	int lineno = 0;
    	try {
            BufferedReader sr = new BufferedReader(new FileReader(path));
            String line;
            while ((line = sr.readLine())!=null)
            {
            	lineno++;
	            if (line.length()>0)
	            {
	                Item it = StringUtil.parseItem(line);
	                int id = it.getId();
	                if (id>0)
	                {
	                    //count top id!
	                    if (m_topId<id)
	                    {
	                        m_topId=id;
	                    }
	                    addItem(it);
	                }
	            }
            }
            sr.close();
    	} catch (Exception e) {
    		System.out.println(e+", loadFromFile, line:"+lineno);
    		e.printStackTrace();
    	}
    }

	/**
	 * 
	 * @param path
	 */
    public void save(String path,boolean extendedNames)
    {
    	try {
	        String tempFileName = path.substring(0,path.indexOf('.'))+".tmp";
	        BufferedWriter sw = new BufferedWriter(new FileWriter(tempFileName));
	        Enumeration e = getEnumeration();
	        while (e.hasMoreElements())
	        {
	        	if (extendedNames)
	                sw.write(StringUtil.toString((Item)(e.nextElement()),this,Format.defaultFormat));
	        	else
	                sw.write(StringUtil.toString((Item)(e.nextElement()),this,";",","));
	            sw.newLine(); 
	        }
	        sw.close();
	        java.io.File fi = new java.io.File(path);
	        fi.delete();
	        fi = new java.io.File(tempFileName);
	        fi.renameTo(new java.io.File(path));
    	} catch (Exception e) {
    		System.out.println(e+", save");
    		e.printStackTrace();
    	}
    }

    public Item[] getTopItems(int thresholdPercent)
    {
    	Enumeration e;
        // find max
    	float evidenceMax = 0;
        e = getEnumeration();
        while (e.hasMoreElements())
        {
        	float evidence = ((Item)e.nextElement()).getEvidence();
        	if (evidenceMax < evidence)
        		evidenceMax = evidence;
        }
        // count max elements
        int totalCount = 0;
        int maxCount = 0;
        e = getEnumeration();
        while (e.hasMoreElements())
        {
        	totalCount++;
        	if (evidenceMax == ((Item)e.nextElement()).getEvidence())
        		maxCount++;
        }
        if (maxCount == 0 || (int)(((float)maxCount)/totalCount) > thresholdPercent)
        	return null;
        Item top[] = new Item[maxCount];
        int i = 0;
        e = getEnumeration();
        while (e.hasMoreElements())
        {
        	Item it = (Item)e.nextElement();
        	if (evidenceMax == it.getEvidence())
        		top[i++] = it;
        }
        return top;
    }

    /**
     * Import the content of the item array ignoring item evidence and id
     * changes the content of export array to the imported items
     * @param imported
     */
    public void importItems(Item[] export)
    {
    	for (int i=0;i<export.length;i++)
    	{
    		Item it = getItem(export[i].getName());
    		export[i] = (it != null)? it : addItem(export[i].clone(++m_topId));
    	}
    }

    /**
     * Import the content of the other item store ignoring evidence and id
     * not affecting the contents of the exporter
     * @param imported
     */
    public void importItems(MemoryStore exporter)
    {
    	Enumeration e = getEnumeration();
        while (e.hasMoreElements())
        {
    		Item exit = (Item)e.nextElement();
    		Item it = getItem(exit.getName());
    		if (it == null)
    			addItem(exit.clone(++m_topId));
    	}
    }
    
}
