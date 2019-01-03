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

import java.util.ArrayList;

public class BasicTokenizer implements Tokenizer
{
        Server m_server;

        public BasicTokenizer(Server server)
		{
            m_server = server;
        }

        public int[] getTokens(String docData,boolean ignoreCase,boolean lasyAdd) throws Exception
        {
            Storager st = m_server.getStorager();
            String[] chunks = StringUtil.toTokens(docData,StringUtil.DELIMITERS);
            if (chunks!=null && chunks.length>0)
            {
                int cnt = 0;
                int[] tmp = new int[chunks.length];
                for (int i=0;i<chunks.length;i++)
                {
                    int id = st.getIdByName(Types.TOKEN,chunks[i],ignoreCase);
                    if (id==0 && lasyAdd) //lazy add token
                    {
                        id = st.addRel(new StoragerRelation(Types.TOKEN,chunks[i]));
                    }
                    if (id!=0)
                    {
                        tmp[cnt++] = id;
                    }
                }
                if (cnt>0)
                {
                    int[] ids = new int[cnt];
                    //Array.Copy(tmp,ids,cnt);
                    System.arraycopy(tmp, 0, ids, 0, cnt); 
                    return ids;
                }
            }
            return null;
        }

        // use the original list of tokens to return all patterns known to the system
        // optionalDomains - used to restrict search only to patterns specific to certain domains
        public Relation[] getPatterns(int[] tokens,int[] optionalDomains) throws Exception
        {
            //ArrayList<Relation> pats = new ArrayList<Relation>();
            ArrayList pats = new ArrayList();
            Storager st = m_server.getStorager();
            // for each start position in the list of tokens
            for (int i=0;i<tokens.length;i++)
            {
                // for each element, starting with the start position
                int dim = 0;
                Relation[] rels = null; // list of mined relations
                for (int j=i;j<tokens.length;j++)
                {
                    if (dim==0)
                        rels = st.getRels(Types.PATTERN,tokens[j],dim);
                    else
                        rels = BasicProcessor.filter(rels,tokens[j],dim);
                    // terminate if no more patterns can be found
                    if (rels.length==0)
                        break;
                    dim++;
                    // for found patterns, retain those which have the arity matching the dimension
                    for (int k=0;k<rels.length;k++)
                    {
                        if (rels[k].getArity()==dim) 
                            pats.add(rels[k]);
                    }
                    if (dim>=Types.MAXPATTERNLENGTH)
                        break;
                }
            }
            return BasicProcessor.toRelations(pats);
        }

}
