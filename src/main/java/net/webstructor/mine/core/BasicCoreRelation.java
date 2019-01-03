/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.core;

public class BasicCoreRelation implements CoreRelation, Comparable {

    public int m_type = 0;
    public int m_id = 0;
    public int[] m_ids = null;
    public String m_name = null;
    
    public BasicCoreRelation()
    {
    }
    
    public BasicCoreRelation(int type,int id,String name)
    {
        m_type = type;
        m_id = id;
        m_name = name;
    }

    public BasicCoreRelation(int type,int[] ids)
    {
        m_type = type;
        m_ids = ids;
    }

    public BasicCoreRelation(int type,String name)
    {
        m_type = type;
        m_name = name;
    }

    public BasicCoreRelation(int type,int[] ids,String name)
    {
        m_type = type;
        m_ids = ids;
        m_name = name;
    }
    
    public BasicCoreRelation(int type,int id,int[] ids,String name)
    {
        m_type = type;
        m_id = id;
        m_ids = ids;
        m_name = name;
    }
    
    public BasicCoreRelation(CoreRelation rel)
    {
        m_type = rel.getType();
        m_id = rel.getId();
        m_ids = rel.getIds()==null? null: (int[])rel.getIds().clone();
        m_name = rel.getName();
    }
    
    public int getType()
    {
        return m_type;
    }

    public int getId()
    {
        return m_id;
    }

    public int	getId(int idx)
    {
    	return m_ids==null || idx<0 || idx>=m_ids.length ? 0: m_ids[idx]; 
    }
    
    public String getName()
    {
        return m_name;
    }

    public int getArity()
    {
        return (m_ids == null)? 0: m_ids.length;
    }

    public int[] getIds()
    {
        return m_ids;
    }

    public int hashCode()
    {
        int hash = m_type ^ m_id;    
        if (m_ids!=null && m_ids.length>0) for (int i=0;i<m_ids.length;i++)
            hash = hash ^ m_ids[i];
        return hash;
    }

    public int compareTo(Object obj)
    {
        String objName = ((CoreRelation)obj).getName();
        if (m_name!=null && objName!=null)
            return m_name.compareTo(objName);
        if (m_name!=objName)
            return m_name!=null? +1: -1; // one name is not null
        return 0; // both names null
    }

    public boolean equals(CoreRelation rel)
    {
        if (rel==null || m_type!=rel.getType())
            return false;
        // important - this is STRICT identity, 
        // NOT LOGICAL identity
        if (m_id!=rel.getId()) 
            return false;
        int arity = getArity();
        if (arity != rel.getArity())
            return false;
        int[] ids = rel.getIds(); 
        for (int i=0;i<arity;i++)
        {
            if (ids[i]!=m_ids[i])
                return false;
        }
        return true;
    }

}
