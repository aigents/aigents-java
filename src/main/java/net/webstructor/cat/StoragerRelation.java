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
	
//public class StoragerRelation implements Relation, Comparable<Relation>
public class StoragerRelation implements Relation, Comparable
{
        public int m_type = 0;
        public int m_id = 0;
        public int[] m_ids = null;
        public String m_name = null;
        public double m_posEvidence = 0;
        public double m_negEvidence = 0;
        public int m_confirmation = Types.NOTCONFIRMED;

        public StoragerRelation()
        {
        }

        public StoragerRelation(int type,int[] ids)
        {
            m_type = type;
            m_ids = ids;
        }

        public StoragerRelation(int type,int[] ids,int confirm)
        {
            m_type = type;
            m_ids = ids;
            m_confirmation = confirm;
        }

        public StoragerRelation(int type,String name)
        {
            m_type = type;
            m_name = name;
        }

        public StoragerRelation(int type,String name,int confirm)
        {
            m_type = type;
            m_name = name;
            m_confirmation = confirm;
        }

        public StoragerRelation(int type,int[] ids,String name,int confirm)
        {
            m_type = type;
            m_ids = ids;
            m_name = name;
            m_confirmation = confirm;
        }

        public StoragerRelation(Relation rel)
        {
            m_type = rel.getType();
            m_id = rel.getId();
            m_ids = rel.getIds()==null? null: (int[])rel.getIds().clone();
            m_name = rel.getName();
            assign(rel);
        }

        public void assign(Relation rel)
        {
            m_posEvidence = rel.getPosEvidence();
            m_negEvidence = rel.getNegEvidence();
            m_confirmation = rel.getConfirmation();
        }

        public boolean equals(Relation rel)
        {
            if (rel==null || m_type!=rel.getType())
                return false;
            // important - this is STRICT identity, 
            // NOT LOGICAL identity
            if (m_id!=rel.getId()) 
                return false;
            int[] ids=rel.getIds();
            if (ids.length!=m_ids.length)
                return false;
            for (int i=0;i<m_ids.length;i++)
            {
                if (ids[i]!=m_ids[i])
                    return false;
            }
            return true;
        }

        public int getType()
        {
            return m_type;
        }

        public int getId()
        {
            return m_id;
        }

        public String getName()
        {
            return m_name;
        }

        /*
        public String getData()
        {
            return null;
        }
        */

        public int getArity()
        {
            return (m_ids == null)? 0: m_ids.length;
        }

        public int[] getIds()
        {
            return m_ids;
        }

        public double getPosEvidence()
        {
            return m_posEvidence;
        }

        public double getNegEvidence()
        {
            return m_negEvidence;
        }

        public int getConfirmation()
        {
            return m_confirmation;
        }

        //@Override
        //public int compareTo(Relation obj)
        public int compareTo(Object o)
        {
        	Relation obj = (Relation)o;
            String objName = obj.getName();
            if (m_name!=null && objName!=null)
                return m_name.compareTo(objName);
            if (m_name==objName)
                return 0; // both null
            return m_name!=null? +1: -1;
        }

        //TODO
        //@Override
        public boolean equals(Object obj)
        {
            return compareTo(obj) == 0;
        }

        //@Override 
        public int hashCode()
        {
            int hash = m_type ^ m_id;    
            if (m_ids!=null && m_ids.length>0) for (int i=0;i<m_ids.length;i++)
                hash = hash ^ m_ids[i];
            return hash;
        }

}
