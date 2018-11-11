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

import java.io.IOException;
import java.util.ArrayList;

public class BasicProcessor implements Processor
{
        Server m_server;
        public BasicProcessor(Server server)
		{
            m_server = server;
        }

        String getReadData(String location) throws IOException
        {
            Reader[] rdrs = m_server.getReaders();
            for (int i=0;i<rdrs.length;i++)
            {
                if (rdrs[i].canReadDoc(location))
                {   
                    String data = rdrs[i].readDocData(location);
                    //TODO: Handle different formats using same pattern with different formatters
                    return HtmlStripper.cleanHtmlRegExp(data);
                }
            }
            return null;
        }

        boolean filterPassed(Relation rel,int confirm,int dim,int dimType,int[] filter) throws Exception
        {
            if (confirm>0 && rel.getConfirmation()<=0)
                return false;
            if (filter!=null)
            {
                int[] ids = rel.getIds(); // apply filter to relation itself
                if (dim!=-1 && dim<ids.length)
                    ids = getRel(dimType,ids[dim]).getIds();
                for (int fi=0;fi<filter.length&&fi<ids.length;fi++)
                {
                    if (filter[fi]!=0 && filter[fi]!=ids[fi])
                    {
                        return false;
                    }
                }
            }
            return true;
        }

        Relation[] filterConfirmed(Relation[] rels,int confirm) throws Exception
        {
            return filterConfirmed(rels,confirm,-1,-1,null);
        }

        Relation[] filterConfirmed(Relation[] rels,int confirm,int dim,int dimType,int[] filter) throws Exception
        {
            if (rels!=null && (confirm>0 || filter!=null))
            {
                int cnt = 0;
                for (int i=0;i<rels.length;i++)
                    if (filterPassed(rels[i],confirm,dim,dimType,filter))
                        cnt++;
                if (cnt==0)
                    rels = null;
                else
                if (cnt<rels.length)
                {
                    Relation[] filt = new Relation[cnt];
                    cnt = 0;
                    for (int i=0;i<rels.length;i++)
                    {
                        if (filterPassed(rels[i],confirm,dim,dimType,filter))
                            filt[cnt++]=rels[i];
                    }
                    rels = filt;
                }
            }
            return rels;
        }

        //public static Relation[] toRelations(ArrayList<Relation> a)
        public static Relation[] toRelations(ArrayList a)
        {
            if (a!=null && a.size()>0)
            {
                Relation[] rels = new Relation[a.size()]; 
                for (int i=0;i<a.size();i++)
                    rels[i]=(Relation)a.get(i);
                return rels;
            }
            return null;
        }

        public static Relation[] filter(Relation[] rels,int fkId,int dim)
        {
            if (rels!=null)
            {
                int cnt = 0;
                for (int i=0;i<rels.length;i++)
                {
                    Relation rel = rels[i];
                    if (rel.getArity()<dim && rel.getIds()[dim]==fkId)
                        cnt++;
                }
                if (cnt==0)
                    rels = null;
                else
                if (cnt<rels.length)
                {
                    Relation[] filt = new Relation[cnt];
                    cnt = 0;
                    for (int i=0;i<rels.length;i++)
                    {
                        Relation rel = rels[i];
                        if (rel.getArity()<dim && rel.getIds()[dim]==fkId)
                            filt[cnt++]=rel;
                    }
                    rels = filt;
                }
            }
            return rels;
        }

        double calcBase(Relation[] rels)
        {   
            double sum = 0;
            if (rels!=null) for (int i=0;i<rels.length;i++)
            {
                int c = rels[i].getConfirmation();
                double e = (c==Types.NOTCONFIRMED)? rels[i].getPosEvidence(): c; 
                sum += e;
            }
            return sum;
        }

        int[] getIds(int typeId,int fkId,int dim) throws Exception
        {
            int[] ints = null;
            Relation[] rels = m_server.getStorager().getRels(typeId,fkId,dim);
            if (rels!=null && rels.length>0)
            {
                ints = new int[rels.length];
                for (int i=0;i<rels.length;i++)
                    ints[i]=rels[i].getId();
            }
            return ints;
        }

        int[] getIds(int typeId,int fkId,int dim,int from,int to) throws Exception
        {
            int[] ints = null;
            Relation[] rels = m_server.getStorager().getRels(typeId,fkId,dim,from,to);
            if (rels!=null && rels.length>0)
            {
                ints = new int[rels.length];
                for (int i=0;i<rels.length;i++)
                    ints[i]=rels[i].getId();
            }
            return ints;
        }

        public String getName(int typeId, int relId) throws Exception              // returns the name of the relation
        {
        	Relation r = getRel(typeId,relId);
        	if (r == null) {
        		throw new Exception("Missed relation "+typeId+":"+relId);
        	}
            return r.getName();
        }

        public String[] getNames(int typeId,int[] ids) throws Exception
        {
            if (ids!=null && ids.length>0)
            {
                String[] names = new String[ids.length];
                for (int i=0;i<ids.length;i++)
                    names[i] = getName(typeId,ids[i]);
                return names;
            }
            return null;
        }

        int posEvidenceRel(int type,int[] ids,double evidence) throws Exception
        {
            Relation oldrel = m_server.getStorager().getRel(type,ids);
            StoragerRelation newrel = (oldrel==null)
                ? new StoragerRelation(type,ids)
                : new StoragerRelation(oldrel);
            newrel.m_posEvidence += evidence; 
            return m_server.getStorager().setRel(newrel);
        }

        int posEvidenceConfirmRel(int type,int[] ids,double evidence,int confirm) throws Exception
        {
            Relation oldrel = m_server.getStorager().getRel(type,ids);
            StoragerRelation newrel = (oldrel==null)
                ? new StoragerRelation(type,ids)
                : new StoragerRelation(oldrel);
            newrel.m_posEvidence += evidence; 
            newrel.m_confirmation = confirm; 
            return m_server.getStorager().setRel(newrel);
        }

        int confirmRel(int type,int[] ids,int confirm) throws Exception
        {
            Relation oldrel = m_server.getStorager().getRel(type,ids);
            StoragerRelation newrel = (oldrel==null)
                ? new StoragerRelation(type,ids)
                : new StoragerRelation(oldrel);
            newrel.m_confirmation = confirm; 
            return m_server.getStorager().setRel(newrel);
        }

        int confirmRel(int type,int id,int confirm) throws Exception
        {
            Relation oldrel = m_server.getStorager().getRel(type,id);
            if (oldrel==null)
                return 0;
            StoragerRelation newrel = new StoragerRelation(oldrel); 
            newrel.m_confirmation = confirm; 
            return m_server.getStorager().setRel(newrel);
        }

        int getAddTokenFeatureId(String token,int featureTypeId,int confirm) throws Exception
        {
            Storager st = m_server.getStorager();
            int tokenId = st.getIdByName(Types.TOKEN,token,true);
            if (tokenId==0)
                tokenId = st.addRel(new StoragerRelation(Types.TOKEN,token));
            return tokenId==0?tokenId:getAddTokenFeatureId(tokenId,featureTypeId,confirm);
        }

        // assure there is a "Keyword" feature exists for the token
        int getAddTokenFeatureId(int tokenId,int featureTypeId,int confirm) throws Exception
        {
            Storager st = m_server.getStorager();
            int featureId = 0;
            Relation[] fts = st.getRels(Types.FEATURETOKEN,tokenId,1);
            if (fts!=null && fts.length>0)    
            {
                for (int i=0;i<fts.length;i++)
                {
                    Relation f = st.getRel(Types.FEATURE,fts[i].getIds()[0]);
                    if (f==null)
                        throw(new Exception("Missed feature for keyword"));
                    if (f.getIds()[0]==featureTypeId)
                    {
                        featureId = f.getId();
                        break;
                    }
                }
            }
            if (featureId==0)
            {
                featureId = st.addRel(new StoragerRelation(Types.FEATURE,new int[]{featureTypeId},confirm));
                if (featureId==0)
                    throw(new Exception("Can not create feature"));
                st.addRel(new StoragerRelation(Types.FEATURETOKEN,new int[]{featureId,tokenId},confirm));
            }
            return featureId;
        }

        public Relation[] getCats(int catSysId) throws Exception
        {
            return m_server.getStorager().getRels(Types.CATEGORY,catSysId,0/*dimension*/);
        }

        public int addCat(int catSysId, String catName, boolean ignoreDups) throws Exception
        {
        	Storager st = m_server.getStorager(); 
        	if (ignoreDups) 
        	{
        		int id = st.getIdByName(Types.CATEGORY,catName,true);
        		if (id != 0 && st.getRel(Types.CATEGORY, id).getIds()[0] == catSysId)
        			return id;
        	}
            Relation rel = new StoragerRelation(Types.CATEGORY,new int[]{catSysId},catName,1);            
            return st.addRel(rel);
        }

        public int delCat(int catId) throws Exception
        {
            Storager st = m_server.getStorager();
            st.delRels(Types.CATFEATURE,catId,0,true);
            st.delRels(Types.DOCCAT,catId,1,true);
            return st.delRel(Types.CATEGORY,catId);
        }

        public int getDocCnt(int srcId) throws Exception
        {
            return m_server.getStorager().getRelCnt(Types.DOCUMENT,srcId,0/*dimension*/);
        }

        public Relation[] getDocs(int srcId) throws Exception
        {
            return m_server.getStorager().getRels(Types.DOCUMENT,srcId,0/*dimension*/);
        }

        public Relation[] getDocs(int srcId, int from, int to) throws Exception
        {
            return m_server.getStorager().getRels(Types.DOCUMENT,srcId,0/*dimension*/,from,to);
        }

        public int delDoc(int docId) throws Exception
        {
            Storager st = m_server.getStorager();
            st.delRels(Types.DOCFEATURE,docId,0,true);
            st.delRels(Types.DOCCAT,docId,0,true);
            return st.delRel(Types.DOCUMENT,docId);
        }

        public int updateDoc(int docId, boolean lazyTokens) throws Exception
        {
            int featureHits = 0;
            //delete all document features (but leave feature tokens)
            m_server.getStorager().delRels(Types.DOCFEATURE,docId,0,true);
            String data = getDocData(docId);
            // create ALL features for the document
            if (data!=null)
            {
                // extract all tokens for the document   
                int[] tokens = m_server.getTokenizer().getTokens(data.toLowerCase(),false,lazyTokens);
                if (tokens!=null)
                {
                    for (int i=0;i<tokens.length;i++)
                    {
                        //check if there is a Keyword (4,1000) feature exist for the token
                        int featureId = getAddTokenFeatureId(tokens[i],Types.KEYWORD,1);
                        //bump positive evidence
                        posEvidenceConfirmRel(Types.DOCFEATURE,new int[]{docId,featureId},1,1);
                        featureHits++;
                    }
                }
            }
            // infer categories for the document
            updateDocCats(docId);
            return featureHits;
        }

        int updateDocCats(int docId) throws Exception
        {
            Storager st = m_server.getStorager();
            int catHits = 0;
            // don't delete category features, but set the relevance values to 0
            st.setRels(Types.DOCCAT,docId,0,0.0);
            // for each feature in the document
            Relation[] rDF = st.getRels(Types.DOCFEATURE,docId,0);
            if (rDF!=null) for (int df=0;df<rDF.length;df++)
            {
                // calculate document-feature relevance
                double Rdf = getRelevance(Types.DOCFEATURE,rDF[df].getId());
                // for each category for the feature
                Relation[] rCF = st.getRels(Types.CATFEATURE,rDF[df].getIds()[1],1);
                if (rCF!=null) for (int cf=0;cf<rCF.length;cf++)
                {
                    // calculate category-feature relevance
                    double Rcf = getRelevance(Types.CATFEATURE,rCF[cf].getId());
                    // get existent category for the document or add a new one if not present
                    // bump feature evidence as DC relevance * CF relevance
                    posEvidenceRel(Types.DOCCAT,new int[]{docId,rCF[cf].getIds()[0]},Rdf*Rcf);
                    catHits++;
                }
            }
            return catHits;
        }

        public int updateCat(int catId,boolean purgeConfirmed) throws Exception
        {
            Storager st = m_server.getStorager();
            int featureHits = 0;
            //delete all category features (but leave feature tokens)
            st.delRels(Types.CATFEATURE,catId,0,purgeConfirmed);

            // for each document in the category
            Relation[] catdocs = st.getRels(Types.DOCCAT,catId,1);
            if (catdocs!=null) for (int i=0;i<catdocs.length;i++)
            {
                // calculate document-category relevance
                double Rdc = getRelevance(Types.DOCCAT,catdocs[i].getId());
                // for each feature in the document
                Relation[] docfeats = st.getRels(Types.DOCFEATURE,catdocs[i].getIds()[0],0);
                if (docfeats!=null) for (int j=0;j<docfeats.length;j++)
                {
                    double Rdf = getRelevance(Types.DOCFEATURE,docfeats[j].getId());
                    // add feature to the category or get existent feature id present
                    // bump feature evidence as DC relevance * DF relevance
                    posEvidenceRel(Types.CATFEATURE,new int[]{catId,docfeats[j].getIds()[1]},Rdf*Rdc);
                    featureHits++;
                }
            }

            return featureHits;
        }

        public Relation[] getDocFeatures(int docId,int confirm) throws Exception
        {
            return filterConfirmed(m_server.getStorager().getRels(Types.DOCFEATURE,docId,0),confirm);
        }

        public Relation[] getFeatureTokens(int featureId) throws Exception
        {
            return m_server.getStorager().getRels(Types.FEATURETOKEN,featureId,0);
        }

        public int addDocFeature(int docId,String token,int confirm) throws Exception
        {
            int featId = getAddTokenFeatureId(token,Types.KEYWORD,confirm);
            return featId==0?featId:confirmRel(Types.DOCFEATURE,new int[]{docId,featId},confirm);
        }

        public int setDocFeature(int relId,int confirm) throws Exception
        {
            return confirmRel(Types.DOCFEATURE,relId,confirm);
        }

        public Relation[] getCatFeatures(int catId,int confirm) throws Exception
        {
            return filterConfirmed(m_server.getStorager().getRels(Types.CATFEATURE,catId,0),confirm);
        }

        public int addCatFeature(int catId,String token,int confirm) throws Exception
        {
            int featId = getAddTokenFeatureId(token,Types.KEYWORD,confirm);
            return featId==0?featId:confirmRel(Types.CATFEATURE,new int[]{catId,featId},confirm);
        }

        public int setCatFeature(int relId,int confirm) throws Exception
        {
            return confirmRel(Types.CATFEATURE,relId,confirm);
        }

        public Relation[] getDocCats(int docId,int confirm,int domain) throws Exception
        {
            return filterConfirmed(m_server.getStorager().getRels(Types.DOCCAT,docId,0),confirm,1,Types.CATEGORY,new int[]{domain});
        }

        public Relation[] getCatDocs(int catId,int confirm, int srcId) throws Exception
        {
        	//Relation rels[] = filterConfirmed(m_server.getStorager().getRels(Types.DOCCAT,catId,1),confirm);
        	return (srcId == 0)
            	? filterConfirmed(m_server.getStorager().getRels(Types.DOCCAT,catId,1),confirm)
        		: filterConfirmed(m_server.getStorager().getRels(Types.DOCCAT,catId,1),confirm,0,Types.DOCUMENT,new int[]{srcId});
        }

        public Relation getRel(int typeId,int pkId) throws Exception	// get a relation if exists
        {
        	Storager st = m_server.getStorager();        	
            return st.getRel(typeId,pkId);
        }

        public String getCatDocData(int docCatId) throws Exception
        {
            Relation rel = m_server.getStorager().getRel(Types.DOCCAT,docCatId);
            return rel==null? null: getDocData(rel.getIds()[0]);
        }

        public String getCatDocHighlight(int docCatId) throws Exception         //returns original binary representation of the document in document-category relation with specified relation Id
        {
            Relation rel = m_server.getStorager().getRel(Types.DOCCAT,docCatId);
            return rel==null? null: getDocHighlight(rel.getIds()[0],rel.getIds()[1]);
        }

        public String getDocData(int docId) throws Exception
        {
            Relation rel = m_server.getStorager().getRel(Types.DOCUMENT,docId);
            return getReadData(rel.getName());
        }

        public int addDocCat(int docId,int catId,int confirm) throws Exception
        {
            return confirmRel(Types.DOCCAT,new int[]{docId,catId},confirm);
        }

        public int setDocCat(int relId,int confirm) throws Exception
        {
            return confirmRel(Types.DOCCAT,relId,confirm);
        }

        public String[] getDocTokenNames(int docId) throws Exception
        {
            Storager st = m_server.getStorager();
            String data = getDocData(docId);
            if (data!=null)
            {   
                int[] tokenIds = m_server.getTokenizer().getTokens(data.toLowerCase(),false,false);
                if (tokenIds!=null && tokenIds.length>0)
                {
                    String[] toks = new String[tokenIds.length];
                    for (int i=0;i<tokenIds.length;i++)
                        toks[i] = st.getName(Types.TOKEN,tokenIds[i]);
                    return toks;
                }
            }
            return null;
        }

        //20061104
        public String getDocHighlight(int docId,int catId) throws Exception
        {
            Storager st = m_server.getStorager();
            String data = null;
            data = getDocData(docId);
            if (data!=null)
            {   
                // get list of document token names unuppercased
                String[] chunks = StringUtil.toTokens(data,StringUtil.DELIMITERS);
                if (chunks!=null && chunks.length>0)
                {
                    //assume no text weights by default, all = 0
                    int[] values = new int[chunks.length];
                    double[] dvalues = new double[chunks.length];
                    for (int i=0;i<chunks.length;i++)
                        dvalues[i] = 0;
                    //get list of all doc relations
                    Relation[] feats = getDocFeatures(docId,0);
                    if (feats!=null && feats.length>0)
                    {
                        for (int j=0;j<feats.length;j++)
                        {
                            double r = getRelevance(Types.DOCFEATURE,feats[j].getId());
                            int featureId = feats[j].getIds()[1];
                            Relation[] toks = getFeatureTokens(feats[j].getIds()[1]);
                            if (toks!=null && toks.length>0)
                            {
                                String upperName = getName(Types.TOKEN,toks[0].getIds()[1]).toUpperCase();
                                if (catId!=0)
                                {
                                    Relation rel = st.getRel(Types.CATFEATURE,new int[]{catId,featureId});
                                    if (rel!=null)
                                        r *= getRelevance(Types.CATFEATURE,rel.getId());
                                }
                                for (int i=0;i<chunks.length;i++)
                                    if (upperName.equals(chunks[i].toUpperCase()))
                                        if (dvalues[i] < r)
                                            dvalues[i] = r;
                            }
                        }
                    }
                    double maxR = 0;
                    for (int k=0;k<dvalues.length;k++)
                        if (maxR < dvalues[k])
                            maxR = dvalues[k];
                    for (int l=0;l<chunks.length;l++)
                        values[l] = (int)(maxR>0? dvalues[l]*100/maxR: 0);
                    return StringUtil.toHtml(chunks,values);
                }

                
/*
                int[] tokenIds = m_server.getTokenizer().getTokens(data);
                if (tokenIds!=null && tokenIds.length>0)
                {
                    String[] toks = new String[tokenIds.length];
                    int[] values = new int[tokenIds.length];
                    double[] dvalues = new double[tokenIds.length];
                    double maxR = 0;
                    for (int j=0;j<tokenIds.length;j++)
                    {
need features, not tokens
                        dvalues[j] = getRelevance(Types.DOCFEATURE,tokenIds[j]);
                        //if (catId==0)
                            //multiply by feature relevance
                        if (maxR < dvalues[j])
                            maxR = dvalues[j];
                    }
                    for (int i=0;i<tokenIds.length;i++)
                    {
                        toks[i] = st.getName(Types.TOKEN,tokenIds[i]);
                        values[i] = (int)(maxR>0? values[i]/maxR: 0);
                    }
                    return StringUtil.toHtml(toks,values);
                }
*/
            }
            return null;
        }

        /* create and read document, translate it into ordered vector of tokens 
        (words and symbols, with Reader.canReadDoc and Reader.readDocTokens), 
        perform feature extraction (with Calculator.extractDocFeatures), 
        given the current feature metrics, suggest the categories for document 
        (with Calculator.suggestDocCats) */
        public int addDoc(int srcId, String docName,boolean ignoreDups, boolean lazyTokens) throws Exception
        {
        	Storager st = m_server.getStorager();
            // find an appropriate reader
            Reader[] rdrs = m_server.getReaders();
            for (int i=0;i<rdrs.length;i++)
            {
                if (rdrs[i].canReadDoc(docName))
                {   
                    // only add document relation to the database
                    Relation rel = new StoragerRelation(Types.DOCUMENT,new int[]{srcId},docName,1); 
                    int id;
                    // if duplicates allowed and if document of the same document source exists, just return it
                    if (ignoreDups) 
                    {
                    	id = st.getIdByName(Types.DOCUMENT, docName, true);
                    	if (id != 0 && st.getRel(Types.DOCUMENT, id).getIds()[0] == srcId)
                    		return id;
                    }
                    id = st.addRel(rel);
                    // full processing of the document
                    updateDoc(id,lazyTokens);                    
                    return id;
                }
            }
            return 0;
        }

        public double getRelevance(int typeId, int relId) throws Exception         // calculates the relevance value for the relation
        {
            Storager st = m_server.getStorager();
            Relation rel = st.getRel(typeId,relId);
            int[] ids = rel.getIds();
            //int[] types = st.getTypes(typeId);

            int c = rel.getConfirmation();
            double e = (c==Types.NOTCONFIRMED)? rel.getPosEvidence(): c; 

            Relation[] rb0 = st.getRels(typeId,ids[0],0);
            Relation[] rb1 = st.getRels(typeId,ids[1],1);
            
            double b0 = calcBase(rb0);
            double b1 = calcBase(rb1);
            double r = (b0==0||b1==0)? 0: e*e/(b0*b1);
            
            return r;
        }

        public int addDocsBatch(int srcId, String batchName) throws Exception
        {
            int cnt = 0;
            String batch = getReadData(batchName);
            if (batch!=null)
            {
                String[] toks = StringUtil.toTokens(batch,"\n\r");
                for (int i=0;i<toks.length;i++)
                {
                    addDoc(srcId,toks[i],false,false);
                    cnt++;
                }
            }
            return cnt;
        }

        public int addCatsBatch(int domainId, String batchName) throws Exception
        {
            int cnt = 0;
            String batch = getReadData(batchName);
            if (batch!=null)
            {
                String[] cats = StringUtil.toTokens(batch,"\n\r");
                for (int i=0;i<cats.length;i++)
                {
                    String[] toks = StringUtil.toTokens(cats[i],StringUtil.DELIMITERS);
                    if (toks!=null && toks.length>0)
//TODO: we should check the unique category within the Domain (CatSystem) only
                    //if (st.getIdByName(Types.CATEGORY,cats[i],true)==0)
                    { 
                    	boolean byId = StringUtil.isInt(toks[0]);//first token is category id
                        int catId;
                        if (byId) 
                        {
                        	catId = StringUtil.toInt(toks[0]);
                        	if (getRel(Types.CATEGORY, catId) == null)
                        		throw new Exception("Invalid category id="+catId);
                        }
                        else 
                        	catId = addCat(domainId,cats[i],true);
                        for (int j = byId ? 1 : 0; j<toks.length; j++)
                            addCatFeature(catId,toks[j].toLowerCase(),1);
                        cnt++;
                    }
                }
            }
            return cnt;
        }

        public Relation[] getRels(int typeId) throws Exception
        {
            return m_server.getStorager().getRels(typeId);
        }

}
