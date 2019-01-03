/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.core;

import java.util.Vector;

public class BasicRelation extends BasicCoreRelation implements Relation 
{
    public float m_posEvidence = 0;
    //public float m_negEvidence = 0;
    public int m_confirmation = Id.NOTCONFIRMED;
    
    /** hacky context-specific relevance 
     * cached for performance reasons only 
     */
    public transient float m_relevance = 0;
    /** hacky context-specific relevance 
     * caching flag for performance reasons only 
     */
    public transient boolean m_cached = false; 
    

    public BasicRelation()
    {
    }
    
    public BasicRelation(int type,int[] ids,int confirm)
    {
        super(type,ids);
        m_confirmation = confirm;
    }

    public BasicRelation(int type,String name,int confirm)
    {
        super(type,name);
        m_confirmation = confirm;
    }

    public BasicRelation(int type,int[] ids,String name,int confirm)
    {
        super(type,ids,name);
        m_confirmation = confirm;
    }

    public BasicRelation(int type,int[] ids,String name,float posEvidence,int confirm)
    {
        super(type,ids,name);
        m_posEvidence = posEvidence; 
        m_confirmation = confirm;
    }

    public BasicRelation(Relation rel)
    {
    	super(rel);
        assign(rel);
    }

    public BasicRelation(CoreRelation rel)
    {
    	super(rel);
    }

    public void assign(Relation rel)
    {
        m_posEvidence = rel.getPosEvidence();
        //m_negEvidence = rel.getNegEvidence();
        m_confirmation = rel.getConfirmation();
    }

    public float getPosEvidence()
    {
        return m_posEvidence;
    }

    public float getNegEvidence()
    {
        return 0;//m_negEvidence;
    }

    public int getConfirmation()
    {
        return m_confirmation;
    }
    
    public float getConfirmedEvidence()
    {
        //int c = rel.getConfirmation();
        //float e = (c==Id.NOTCONFIRMED)? rel.getPosEvidence(): c;
        return m_posEvidence;
    }

    public boolean equals(Object obj)
    {
    	//return equals(((Relation)obj));
    	return ((CoreRelation)obj).equals(this);
    }

    public static Relation[] toArray(Vector vect)
    {
    	int len;
    	if (vect==null || (len=vect.size())==0)
    		return null;
    	Relation[] rels = new Relation[len]; 
    	for (int i=0;i<len;i++)
    		rels[i]=(Relation)vect.get(i);
    	return rels;
    }
}

