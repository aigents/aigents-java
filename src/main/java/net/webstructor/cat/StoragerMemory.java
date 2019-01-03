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
package net.webstructor.cat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

public class StoragerMemory
{
        public int m_type = 0;
        public HashMap m_byPrimaryId = null;
        public HashMap[] m_byForeignIds = null;
        public HashMap m_byNames = null;
        public int[] m_types = null;

		public StoragerMemory(int type,int[] types)
		{
            int arity = types==null? 0: types.length;
            m_type = type;
            m_byPrimaryId = new HashMap();
            if (arity>0)
            {
                m_byForeignIds = new HashMap[arity];
                for (int i=0;i<arity;i++)
                    m_byForeignIds[i] = new HashMap();
            }
            if (type==Types.TOKEN // no token duplication allowed 
                || type==Types.DOCUMENT // no documents over doc sources can be duplicated
                //|| type==Types.CATEGORY // namesake categories may exist in different domains
                )
                m_byNames = new HashMap();
            m_types = types; 
		}

        public int getArity()
        {
            return m_types==null? 0: m_types.length;
        }

        public int[] getTypes()
        {
            return m_types;
        }

        public int delRel(int pkId)
        {
            //return delRelMemory((Relation)m_byPrimaryId[pkId],-1/*nothing to ignore*/);
            return delRelMemory((Relation)m_byPrimaryId.get(new Integer(pkId)),-1/*nothing to ignore*/);
        }

        public Relation getRel(int objId) throws Exception
        {
            return (Relation)m_byPrimaryId.get(new Integer(objId));
        }
        
        int delRelMemory(Relation victim,int dimToIgnore)
        {
            //delete relations by id in primary HashMap
            //m_byPrimaryId.Remove(victim.getId());
            m_byPrimaryId.remove(new Integer(victim.getId()));
            //delete relations by id in Arrays in Hashtables other than dim
            for (int d=0;d<m_byForeignIds.length;d++)
            {
                if (d!=dimToIgnore)
                {
                    //IEnumerator e = m_byForeignIds[d].Values.GetEnumerator();
                    Iterator e = m_byForeignIds[d].values().iterator();                    
                    while (e.hasNext())
                    {
                        ArrayList a = ((ArrayList)e.next());
                        if (a!=null) {
                            //a.remove(victim);
                        	for (int ai = 0; ai < a.size(); ai++) {
                        		if (((Relation)a.get(ai)).getId() == victim.getId()) {
                        			a.remove(ai);
                        			break;
                        		} 
                        	}
                        }
                    }
                }
            }
            //delete relations by id in Names HashMap
            if (m_byNames!=null)
            {      
                m_byNames.remove(victim.getName());        
            }
            return 1;
        }

        public int delRels(int fkId,int dim,boolean purgeConfirmed) throws Exception
        {
            if (dim>=m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            //get list of relations to delete given the "dim" slot by "fkId"
            ArrayList todo = (ArrayList)(m_byForeignIds[dim].get(new Integer(fkId)));
            if (todo!=null) {
            	ArrayList confirmedList = null;
            	if (!purgeConfirmed) {
            		confirmedList = new ArrayList();
            	}
                for (int i=0;i<todo.size();i++) {
                    Relation victim = (Relation)todo.get(i);
                    if (!purgeConfirmed && victim.getConfirmation() != Types.NOTCONFIRMED)
                    	confirmedList.add(victim);
                    else
                    	delRelMemory(victim,dim);
                }
                //delete list entry for the "dim" HashMap
            	if (purgeConfirmed || confirmedList.size() == 0)
            		m_byForeignIds[dim].remove(new Integer(fkId));
            	else
            		m_byForeignIds[dim].put(new Integer(fkId),confirmedList);
            }
            return 1;
        }

        public Relation getRel(String name,boolean ignoreCase)   
        {
            if (m_byNames!=null && !ignoreCase)//20060927
            {
                return (Relation)m_byNames.get(name);
            }
            else
            {
                if (ignoreCase)
                    name = name.toLowerCase();
                Iterator e = m_byPrimaryId.values().iterator();
                while (e.hasNext())
                {   
                    Relation rel = (Relation)(e.next());
                    if (rel.getName() == null)
                    	System.out.println("TODO:clean empty document names");//TODO
                    else
                    if (ignoreCase)
                    {
                        if (rel.getName().toLowerCase().equals(name))
                            return rel;
                    }
                    else
                    {
                        if (rel.getName().equals(name))
                            return rel;
                    }
                }
                return null;
            }
        }

        public int getId(String name,boolean ignoreCase)   
        {
            Relation rel = getRel(name,ignoreCase);
            return rel==null? 0: rel.getId();
        }

        public int addRel(StoragerRelation rel) throws Exception
        {
            String name = rel.getName(); 
            if (name!=null && m_byNames!=null)
            {
                if (m_byNames.get(name)!=null)
                    throw(new Exception("Name duplication ("+name+")"));
            }
            if (m_byPrimaryId!=null)//20070323
            	m_byPrimaryId.put(new Integer(rel.getId()),rel);
            int relArity = rel.getArity();
            int memArity = (m_byForeignIds==null)?0:m_byForeignIds.length;
            if (memArity>0)
            {
                if (memArity!=relArity)
                    throw(new Exception("Arity mismatch"));
                int[] ids = rel.getIds();
                for (int i=0;i<memArity;i++)
                {
                    HashMap h = m_byForeignIds[i];
                    ArrayList a = (ArrayList)h.get(new Integer(ids[i]));
                    if (a==null)
                    {
                        a = new ArrayList();
                        m_byForeignIds[i].put(new Integer(ids[i]),a);
                    }
                    a.add(rel);
                    //a.Sort();//20060927 sorting in batch operations is not acceptable
                }
            }
            if (name!=null && m_byNames!=null)
                m_byNames.put(name,rel);
            return rel.m_id;
        }
 /*       
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
        	return new IteratorEnumeration(m_byPrimaryId.values().iterator());
        }
*/
        public Iterator getIterator()
        {
        	return m_byPrimaryId.values().iterator();
        }

        public int getRelCnt(int fkId,int dim) throws Exception
        {
            if (dim>=m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            ArrayList a = ((ArrayList)(m_byForeignIds[dim].get(new Integer(fkId))));
            return a==null? 0: a.size();
        }

        public Relation[] getRels(int fkId,int dim) throws Exception
        {
            if (dim>=m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            return toRelations((ArrayList)(m_byForeignIds[dim].get(new Integer(fkId))));
        }

        public Relation[] getRels(int fkId,int dim,int from,int to) throws Exception
        {
        	/*
            if (dim>=m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            ArrayList a = ((ArrayList)(mem.m_byForeignIds[dim][fkId])).GetRange(from,to-from+1);
            return BasicProcessor.toRelations(a);
            */
            if (dim>=m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            return toRelations((ArrayList)(m_byForeignIds[dim].get(new Integer(fkId))),from,to);
        }

        static Relation[] toRelations(ArrayList a)
        {
            if (a!=null && a.size()>0)
            {
                Relation[] rels = new Relation[a.size()]; 
                for (int i=0;i<a.size();i++)
                    rels[i]=(Relation)(a.get(i));
                return rels;
            }
            return null;
        }

        static Relation[] toRelations(ArrayList a,int from,int to)
        {
            if (a!=null && a.size()>0 && from<=to)
            {
                Relation[] rels = new Relation[to-from+1]; 
                for (int i=from,j=0;i<a.size() && i<=to;i++,j++)
                    rels[j]=(Relation)(a.get(i));
                return rels;
            }
            return null;
        }

        public Relation getRel(int[] fkIds) throws Exception
        {
            if (fkIds==null || fkIds.length<m_byForeignIds.length)
                throw(new Exception("Arity underflow"));
            if (fkIds.length>m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            ArrayList a = ((ArrayList)(m_byForeignIds[0].get(new Integer(fkIds[0]))));
            if (a!=null)
                for (int i=0;i<a.size();i++)
                {
                    Relation rel = (Relation)(a.get(i)); 
                    int[] ids = rel.getIds();
                    if (ids.length==fkIds.length)
                    {
                        //check if arrays of ids are identical
                        int j=1;
                        for (;j<fkIds.length;j++)
                        {
                            if (ids[j]!=fkIds[j])
                                break;
                        }
                        if (j==fkIds.length)
                            return rel;
                    }
                }
            return null;
        }

        public Relation[] getRels() throws Exception
        {
            if (m_byPrimaryId.size()>0)
            {
                Relation[] rels = new Relation[m_byPrimaryId.size()];
                Iterator e = m_byPrimaryId.values().iterator();
                int i=0;
                while (e.hasNext())
                {   
                    rels[i++] = (Relation)(e.next());
                }
                return rels;
            }
            return null;
        }
        
}
