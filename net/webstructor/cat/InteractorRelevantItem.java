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

public class InteractorRelevantItem implements RelevantItem
{
        int m_id;
        int m_relId;
        double m_relevance;
        int m_confirmation;
        String m_name;
        public InteractorRelevantItem(int id,int relId,double rel,int conf,String name)
        {
            m_id = id;
            m_relId = relId;
            m_relevance = rel;
            m_confirmation = conf;
            m_name = name;
        }

        public int getId()
        {
            return m_id;
        }

        public int getRelevantId()
        {
            return m_relId;
        }

        public double getRelevance()
        {
            return m_relevance;
        }

        public int getConfirmation()
        {
            return m_confirmation;
        }

        public String getRelevantName()
        {
            return m_name;
        }

		//@Override
		//public int compareTo(RelevantItem i) {
		public int compareTo(Object o) {
			RelevantItem i = (RelevantItem)o;
            //int c = -getRelevance().compareTo(i.getRelevance()); // descending
            int c = (getRelevance() > i.getRelevance()) ? -1 : (getRelevance() < i.getRelevance()) ? +1 : 0;
        	if (c!=0)
                return c;
            return getRelevantName().compareTo(i.getRelevantName()); //ascending
		}
}
