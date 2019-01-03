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
	
public class BasicInteractor implements Interactor
{
        Server m_server;
		public BasicInteractor(Server server)
		{
            m_server = server;
        }

        Item[] toItems(Relation[] rels)
        {
            return toItems(rels,-1,-1);
        }

        Item[] toItems(Relation[] rels,int from,int to)
        {
            Item[] items = null;
            if (rels!=null && rels.length>0)
            {   
                items = new InteractorItem[rels.length];
                for (int i=0;i<rels.length;i++)
                    items[i] = new InteractorItem(rels[i].getId(),rels[i].getName());
                java.util.Arrays.sort(items);
                // apply range cutoff (too late but easy, don't need to worry about sorting)
                if (from>=0 && to>=0)
                {
                    if (to>=items.length)
                        to = items.length-1;
                    Item[] all = items;
                    int size = to-from+1;
                    items = new InteractorItem[size];
                    System.arraycopy(all,from,items,0,size);
                    all = null; // help gc
                }
            }
            return items;    
        }

        RelevantItem[] toRelevantItems(Relation[] rels,int dim) throws Exception
        {
            RelevantItem[] items = null;
            if (rels!=null && rels.length>0)
            {   
                Processor pro = m_server.getProcessor();
                items = new InteractorRelevantItem[rels.length];
                for (int i=0;i<rels.length;i++)
                {
                    String name = null; 
                    if (rels[i].getType()==Types.DOCFEATURE
                        ||rels[i].getType()==Types.CATFEATURE)
                        {
                            Relation[] toks = pro.getFeatureTokens(rels[i].getIds()[1]);
                            if (toks!=null && toks.length>0)
                                name = pro.getName(Types.TOKEN,toks[0].getIds()[1]);
                        }
                    else
                    if (rels[i].getType()==Types.DOCCAT)
                    {
                        name = dim == 0?
                            pro.getName(Types.DOCUMENT,rels[i].getIds()[0]):
                            pro.getName(Types.CATEGORY,rels[i].getIds()[1]);
                    }
                    else
                    {
                        name = rels[i].getName();
                    }
                    items[i] = new InteractorRelevantItem(
                        rels[i].getId(),
                        rels[i].getIds()[dim],
                        pro.getRelevance(rels[i].getType(),rels[i].getId()),
                        rels[i].getConfirmation(),
                        name );
                }
                java.util.Arrays.sort(items);
            }
            return items;    
        }

        public Item[] getCats(int catSysId,int from,int to) throws Exception//returns list of categories in the category system, sorted alphabetically
        {
            return toItems(m_server.getProcessor().getCats(catSysId),from,to);
        }
        public int getDocCnt(int srcId) throws Exception
        {
            return m_server.getProcessor().getDocCnt(srcId);
        }
        public Item[] getDocs(int srcId) throws Exception
        {
            return toItems(m_server.getProcessor().getDocs(srcId));
        }

        public Item[] getDocs(int srcId, int from, int to) throws Exception
        {
            return toItems(m_server.getProcessor().getDocs(srcId,from,to));
        }

        public int addDoc(int srcId, String docName,boolean ignoreDups,boolean lazyTokens) throws Exception
        {
            return m_server.getProcessor().addDoc(srcId,docName,ignoreDups,lazyTokens);
        }

        public int delDoc(int docId) throws Exception
        {
            return m_server.getProcessor().delDoc(docId);
        }

        public int updateDoc(int docId,boolean lazyTokens) throws Exception                   //returns 1 if document with supplied Id is added and then added, retaining the same Id, successfully, 0 otherwise
        {
            return m_server.getProcessor().updateDoc(docId,lazyTokens);
        }

        public int updateCat(int catId, boolean purgeConfirmed) throws Exception
        {
            return m_server.getProcessor().updateCat(catId, purgeConfirmed);
        }

        public RelevantItem[] getDocFeatures(int docId, int confirm) throws Exception
        {
            return toRelevantItems(m_server.getProcessor().getDocFeatures(docId, confirm),1);
        }

        public RelevantItem[] getCatDocFeatures(int catDocId,int confirm) throws Exception
        {
            Relation dc = m_server.getProcessor().getRel(Types.DOCCAT,catDocId);
            return (dc==null)? null: getDocFeatures(dc.getIds()[0],confirm);
        }
        
        public int addDocFeature(int docId,String token,int confirm) throws Exception
        {
            return m_server.getProcessor().addDocFeature(docId,token,confirm);
        }

        public int setDocFeature(int relId,int confirm) throws Exception
        {
            return m_server.getProcessor().setDocFeature(relId,confirm);
        }

        public RelevantItem[] getCatFeatures(int catId,int confirm) throws Exception
        {
            return toRelevantItems(m_server.getProcessor().getCatFeatures(catId,confirm),1);
        }

        public int addCatFeature(int catId,String token,int confirm) throws Exception
        {
            return m_server.getProcessor().addCatFeature(catId,token,confirm);
        }

        public int setCatFeature(int relId,int confirm) throws Exception
        {
            return m_server.getProcessor().setCatFeature(relId,confirm);
        }

        public RelevantItem[] getDocCats(int docId,int confirm,int domain) throws Exception
        {
            return toRelevantItems(m_server.getProcessor().getDocCats(docId,confirm,domain),1);
        }
        
        public RelevantItem[] addDocGetCats(int srcId,int domain,String docName) throws Exception
        {
        	Processor pr = m_server.getProcessor();
        	int docId = pr.addDoc(srcId,docName,true,false);
        	RelevantItem[] items = toRelevantItems(pr.getDocCats(docId,0,domain),1);
        	pr.delDoc(docId);
        	return items;
        }

        public RelevantItem[] getCatDocs(int catId,int confirm, int srcId) throws Exception
        {
            return toRelevantItems(m_server.getProcessor().getCatDocs(catId,confirm,srcId),0);
        }

        public String getDocData(int docId) throws Exception
        {
            return m_server.getProcessor().getDocData(docId);
        }

        public String getCatDocData(int docCatId) throws Exception
        {
            return m_server.getProcessor().getCatDocData(docCatId);
        }

        public String getCatDocHighlight(int docCatId) throws Exception         //returns original binary representation of the document in document-category relation with specified relation Id
        {
            return m_server.getProcessor().getCatDocHighlight(docCatId);
        }

        public int addCat(int catSysId, String catName) throws Exception
        {
            return m_server.getProcessor().addCat(catSysId,catName,true);
        }

        public int delCat(int catId) throws Exception
        {
            return m_server.getProcessor().delCat(catId);
        }

        public int addDocCat(int docId,int catId,int confirm) throws Exception
        {
            return m_server.getProcessor().addDocCat(docId,catId,confirm);
        }

        public int setDocCat(int docId,int confirm) throws Exception
        {
            return m_server.getProcessor().setDocCat(docId,confirm);
        }

        public String[] getDocTokenNames(int docId) throws Exception
        {
            return m_server.getProcessor().getDocTokenNames(docId);
        }

        public Item[] getDomains() throws Exception
        {
            return toItems(m_server.getProcessor().getRels(Types.DOMAIN));
        }

        public Item[] getSources() throws Exception
        {
            return toItems(m_server.getProcessor().getRels(Types.DOCSOURCE));
        }

        public String getDocHighlight(int docId, int catId) throws Exception//return a highlighting markup html
        {
            return m_server.getProcessor().getDocHighlight(docId, catId);
        }

}
