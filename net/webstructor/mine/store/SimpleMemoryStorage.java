/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.store;

import net.webstructor.mine.core.CoreRelation;
import net.webstructor.mine.core.Relation;
import net.webstructor.mine.core.BasicRelation;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter; 
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import net.webstructor.mine.util.StringUtil;
import net.webstructor.mine.util.ReadWriteLock;

public class SimpleMemoryStorage implements Storage {

	ReadWriteLock m_lock = new ReadWriteLock(); 
	int m_topId = 0;
	String m_path;
	Hashtable m_byId = new Hashtable(); // id (Unique, PK) => data
	Hashtable m_byId0andName = new Hashtable(); // id0+name (Unique, Named Access) => id 
	Hashtable m_byId1andId0andId2andName = new Hashtable(); // id1+id0+id2+name (Unique, From) => id 
	Hashtable m_byId2andId0andId1andName = new Hashtable(); // id2+id0+id1+name (Unique, To) => id 
	
	public String getName(int objId,int propId) throws StoreException {
    	Hashtable t1 = (Hashtable)m_byId1andId0andId2andName.get(new Integer(objId));
    	if (t1 != null) {
    		Hashtable t2 = (Hashtable)t1.get(new Integer(propId));
    		if (t2 != null) {
    			Enumeration e = t2.keys();
    			if (e.hasMoreElements()) {
    				Integer target = ((Integer)e.nextElement());
    				return getRel(target.intValue()).getName();
    			}
    		}
    	}
    	return null;
    }
    
    public CoreRelation[] getRels(int typeId) throws StoreException {
    	Object byName = m_byId0andName.get(new Integer(typeId));
    	if (byName instanceof Hashtable) {
    		Hashtable table = (Hashtable)byName; 
    		int size = table.size();
    		if (size>0) {
    			Relation[] results = new Relation[size]; 
    			Enumeration e = table.elements();
    			size = 0;
    			while (e.hasMoreElements()) {
    				results[size] = (Relation)e.nextElement();
    				size++;
    			}
    			return results;
    		}
    	}
    	return null;
    }
    
    void addRel(Hashtable chain, Integer i0, String s, CoreRelation rel) throws Exception {
		if (s == null)
			chain.put(i0,rel);
		else
		{
			Hashtable next = (Hashtable)chain.get(i0);
			if (next == null)
				chain.put(i0,next = new Hashtable());
			next.put(s, rel);
		}
    }
    
    void addRel(Hashtable chain, Integer i0, Integer i1, String s, CoreRelation rel) throws Exception {
    	//TODO don't add hashtables for the 3rd level if there is one link only  
		Object next = chain.get(i0);
		if (next == null)
			chain.put(i0,next = new Hashtable());
    	addRel((Hashtable)next,i1,s,rel);
    }
    
    void addRel(Hashtable chain, Integer i0, Integer i1, Integer i2, String s, CoreRelation rel) throws Exception {
		Object next = chain.get(i0);
		if (next == null)
			chain.put(i0,next = new Hashtable());
    	addRel((Hashtable)next,i1,i2,s,rel);
    }
    
    public int getId(CoreRelation r) throws StoreException {
    	String name = r.getName();
    	if (name != null) {
        	CoreRelation rel = getRel(r.getType(),r.getName(),false);
        	if (rel != null)
        		return rel.getId();
    	}
    	if (r.getArity()==2) {
    		CoreRelation rel = getRel(r.getType(), r.getIds());
        	if (rel != null)
        		return rel.getId();
    	}
    	return 0;
    }
    
    public int addRel(CoreRelation r) throws StoreException {
    	int i = getId(r);
    	if (i != 0)
    		return i;
    	try {
	    	BasicRelation rel = new BasicRelation(r);
	        if (rel.m_id==0)
	        	rel.m_id = ++m_topId;
	        else
	        	if (m_topId < rel.m_id)
	        		m_topId = rel.m_id;
	    	Integer id = new Integer(rel.getId());
	    	Integer id0 = new Integer(rel.getType());
	    	Integer id1 = new Integer(rel.getId(0));
	    	Integer id2 = new Integer(rel.getId(1));
	    	String name = rel.getName();
	    	m_byId.put(id, rel);
	    	if (name!=null) {
	    		Hashtable byName = (Hashtable)m_byId0andName.get(id0);
	    		if (byName == null)
	    			m_byId0andName.put(id0,byName = new Hashtable());
	    		byName.put(name,rel);
	    	}
	    	if (rel.getArity()==2)
	    	{
	    		addRel(m_byId1andId0andId2andName,id1,id0,id2,name,rel);
	    		addRel(m_byId2andId0andId1andName,id2,id0,id1,name,rel);
	    	}
	    	return rel.m_id;
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw(new StoreException(e.getMessage()));
    	}
    }
    
    void loadFromFile(String fileName)
    {
    	int lineno = 0;
    	try {
            BufferedReader sr = 
                new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = sr.readLine())!=null)
            {
            	lineno++;
	            if (line.length()>0)
	            {
	                CoreRelation rel = StringUtil.parseRelation(line);
	                int id = rel.getId();
	                if (id>0)
	                {
	                    //count top id!
	                    if (m_topId<id)
	                    {
	                        m_topId=id;
	                    }
	                    addRel(rel);
	                }
	            }
            }
            sr.close();
    	} catch (Exception e) {
    		System.out.println(e+", file "+m_path+", line:"+lineno);
    		e.printStackTrace();
    	}
    }

    boolean validate(Schema schema) throws StoreException {
    	CoreRelation[] rels = schema.getRelations();
    	if (rels==null)
    		return false;
    	for (int i=0;i<rels.length;i++)
    	{
    		CoreRelation rel = getRel(rels[i].getId());
    		if (rel == null)
    			addRel(rels[i]);
    		else
    		if (!rels[i].equals(rel))
    			return false;
    	}
    	return true;
    }
    
    public void startUp(String path,Schema schema) throws StoreException {
    	loadFromFile(m_path = path);
    	if (!validate(schema)) {
    		throw new StoreException("Invalid Schema");
    	}
    }
    
    public String tempFileName(String fileName) throws Exception
    {
    	int i = fileName.indexOf('.');
        return ((i>=0)? fileName.substring(0,i): fileName) +".tmp";
    }
    
    public void saveToFile(String fileName) throws Exception
    {
        String tempFileName = tempFileName(fileName);
        BufferedWriter sw = 
            new BufferedWriter(new FileWriter(tempFileName));
        Enumeration e = m_byId.elements();
        while (e.hasMoreElements())
        {
        	sw.write(StringUtil.toString((BasicRelation)(e.nextElement()),";",","));
            sw.newLine(); 
        }
        sw.close();
        java.io.File fi = new java.io.File(fileName);
        fi.delete();
        fi = new java.io.File(tempFileName);
        fi.renameTo(new java.io.File(fileName));
    }
    
    public void shutDown() throws StoreException {
        commit();
    }
    
    public void commit() throws StoreException {
    	try {
    		saveToFile(m_path);
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new StoreException(e+", file "+m_path); 
    	}
    }
    
    public boolean isPresent(int pkId) throws StoreException {
    	return false;
    }
    public int getId(int typeId,String name,boolean ignoreCase) throws StoreException {
    	return 0;
    }
    public String getName(int pkId) throws StoreException {
    	return null;
    }
    public CoreRelation getRel(int pkId) throws StoreException {
    	return (CoreRelation)m_byId.get(new Integer(pkId));
    }
    public CoreRelation getRel(int typeId,String name,boolean ignoreCase) throws StoreException {
		Hashtable byName = (Hashtable)m_byId0andName.get(new Integer(typeId));
		if (byName != null)
			return (CoreRelation)byName.get(name);
    	return null;
    }
    public CoreRelation getRel(int typeId, int[] fkIds) throws StoreException {
    	Hashtable t1 = (Hashtable)m_byId1andId0andId2andName.get(new Integer(fkIds[0]));
    	if (t1 != null) {
    		Hashtable t2 = (Hashtable)t1.get(new Integer(typeId));
    		if (t2 != null) {
    			Object x = t2.get(new Integer(fkIds[1]));
    			if (x instanceof CoreRelation)
    				return (CoreRelation)x;
    		}
    	}
    	return null;
    }
    public int setRel(CoreRelation rel) throws StoreException {
    	return 0;
    }
}
