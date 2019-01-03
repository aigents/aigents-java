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

import net.webstructor.main.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
//import java.io.FileReader;
//import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.URL;
//import java.net.URLConnection;
import java.util.Iterator;

public class BasicStorager implements Storager
{
        int m_topId = 0;
        String m_fileName;
        StoragerMemory[] m_memories = null;

		public BasicStorager()
		{
            m_memories = new StoragerMemory[] {
                new StoragerMemory(Types.TYPE,null),
                new StoragerMemory(Types.DOCSOURCE,null),
                new StoragerMemory(Types.DOMAIN,null),
                new StoragerMemory(Types.FEATURETYPE,null),
                new StoragerMemory(Types.TOKEN,null),
                // Unary relations (arity = 1, Parent)
                new StoragerMemory(Types.DOCUMENT,new int[]{Types.DOCSOURCE}),
                new StoragerMemory(Types.CATEGORY,new int[]{Types.DOMAIN}),
                new StoragerMemory(Types.FEATURE,new int[]{Types.FEATURETYPE}),
                // Binary relations (arity = 2) 	
                new StoragerMemory(Types.CATDOMAIN,new int[]{Types.CATEGORY,Types.DOMAIN}),//this probably be ternary, adding the "semantic role argument"
                new StoragerMemory(Types.DOCCAT,new int[]{Types.DOCUMENT,Types.CATEGORY}),
                new StoragerMemory(Types.DOCCAT,new int[]{Types.DOCUMENT,Types.CATEGORY}),
                new StoragerMemory(Types.DOCCAT,new int[]{Types.DOCUMENT,Types.CATEGORY}),
                new StoragerMemory(Types.DOCTOKEN,new int[]{Types.DOCUMENT,Types.TOKEN}),
                new StoragerMemory(Types.DOCFEATURE,new int[]{Types.DOCUMENT,Types.FEATURE}),
                new StoragerMemory(Types.FEATURETOKEN,new int[]{Types.FEATURE,Types.TOKEN}),
                new StoragerMemory(Types.CATFEATURE,new int[]{Types.CATEGORY,Types.FEATURE}),
            };
        }

        StoragerMemory getMemory(int type) throws Exception 
        {
            if (type!=0)
                for (int i=0;i<m_memories.length;i++) 
                    if (m_memories[i].m_type==type)
                        return m_memories[i];
            throw (new Exception("Illegal memory type "+type));
        }
        
        public void loadFromFile(String fileName) throws Exception
        {
        	//int nLine = 0; 
        	/*
            StreamReader sr = 
                new StreamReader(
                new BufferedStream(
                new FileStream(fileName,FileMode.Open)
                ));
            String line;
            while ((line = sr.ReadLine())!=null)
            if (line.length>0)
            {
                Relation rel = StringUtil.parseRelation(line);
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
            sr.Close();
            */
            BufferedReader br = null;
    		try {
    			String line;
    			//br = new BufferedReader(new FileReader(fileName));
    			br = Mainer.getReader(fileName);
    			while ((line = br.readLine()) != null) {
    				//nLine++;
		            if (line.length()>0)
		            {
		                Relation rel = StringUtil.parseRelation(line);
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
        		br.close();
    		} catch (IOException e) {    			
    			e.printStackTrace();
    		} finally {
    			try {
    				if (br != null)br.close();
    			} catch (IOException ex) {
    				ex.printStackTrace();
    			}
    		}
        }

        // import names from file and create nullary relations of given type
        public int addNamesBatch(int typeId,String fileName,int column) throws Exception
        {
            int cnt=0;
            BufferedReader sr = Mainer.getReader(fileName);
            if (sr!=null)
            {
                String line;
                while ((line = sr.readLine())!=null)
                {
                    if (line.length()>0)
                    {
                        try {
	                        line = line.toLowerCase();
	                        String[] input = StringUtil.toTokens( line, " \t;" );
	                        if (column >= input.length)
	                        	column = input.length - 1;
	                        String name = input[column];
	                        if (StringUtil.isWord(name)) {
	                        	if (getIdByName(typeId,name,false)==0) {// if not added yet
		                            if (addRel(new StoragerRelation(typeId,name,1))!=0)
		                                cnt++;
	                        	}
	                        }
                        } catch (Exception e) {
                        	throw e;
                        }
                    }
                }
                sr.close();
            }
            return cnt;
        }

        public void saveToFile(String fileName) throws IOException
        {
        	/*
            String tempFileName = m_fileName.Substring(0,m_fileName.IndexOf('.'));
            StreamWriter sw = 
                new StreamWriter(
                new BufferedStream(
                new FileStream(tempFileName,FileMode.Create)
                ));
            for (int i=0;i<m_memories.length;i++)
            {
                IEnumerator e = m_memories[i].m_byPrimaryId.Values.GetEnumerator();
                while (e.MoveNext())
                {
                    sw.WriteLine(StringUtil.toString((Relation)(e.Current),";",","));
                }
            }
            sw.Close();
            FileInfo fi = new FileInfo(m_fileName);
            fi.Delete();
            fi = new FileInfo(tempFileName);
            fi.MoveTo(m_fileName);
            */
            String tempFileName = fileName.substring(0,fileName.indexOf('.'))+".tmp";
            //BufferedWriter sw = new BufferedWriter(new FileWriter(tempFileName));
            BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(
            	    new FileOutputStream(tempFileName), "UTF-8"));
            for (int i=0;i<m_memories.length;i++)
            {
                //Enumeration e = m_memories[i].getEnumeration();
                Iterator it = m_memories[i].getIterator();
                while (it.hasNext())
                {
                    sw.write(StringUtil.toString((Relation)(it.next()),";",","));
                    sw.newLine(); 
                }
            }
            sw.close();
            java.io.File fi = new java.io.File(fileName);
            fi.delete();
            fi = new java.io.File(tempFileName);
            fi.renameTo(new java.io.File(fileName));            
        }

        public void startUp(String connectionPath) throws Exception
        {
            m_fileName = connectionPath;
            loadFromFile(m_fileName);
        }

        public int commit() throws Exception
        {
            saveToFile(m_fileName);
            return 1;
        }

        public void shutDown() throws Exception
        {
            saveToFile(m_fileName);
            // then, terminate somehow....
        }

        public Relation getRel(int typeId, int objId) throws Exception
        {
            //return (Relation)getMemory(typeId).m_byPrimaryId[objId];
            return getMemory(typeId).getRel(objId);
        }

        public int getIdByName(int typeId,String name,boolean ignoreCase) throws Exception
        {
            return getMemory(typeId).getId(name,ignoreCase);
        }

        public String getName(int typeId, int objId) throws Exception
        {
            //Relation rel = (Relation)getMemory(typeId).m_byPrimaryId[objId];
            Relation rel = getMemory(typeId).getRel(objId);
            return (rel==null)? null: rel.getName();
        }

        public boolean getId(int typeId, int objId) throws Exception
        {
            Relation rel = (Relation)getMemory(typeId).getRel(objId);
            return (rel!=null);
        }

        public String getData(int typeId, int objId)
        {
            // TODO:  Add PilotStorager.getData implementation
            return null;
        }

        public int getRelCnt(int typeId,int fkId,int dim) throws Exception
        {
        	/*
            StoragerMemory mem = getMemory(typeId);
            if (dim>=mem.m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            ArrayList a = ((ArrayList)(mem.m_byForeignIds[dim][fkId]));
            return a==null? 0: a.Count;
            */
            return getMemory(typeId).getRelCnt(fkId,dim);
        }

        public Relation[] getRels(int typeId,int fkId,int dim) throws Exception
        {
        	/*
            StoragerMemory mem = getMemory(typeId);
            if (dim>=mem.m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            return BasicProcessor.toRelations((ArrayList)(mem.m_byForeignIds[dim][fkId]));
            */
            return getMemory(typeId).getRels(fkId,dim);
        }

        /*
        public Relation[] getRels(int typeId)	// retrieve all relations
        {
            StoragerMemory mem = getMemory(typeId);
            if (mem.m_byPrimaryId.size()>0)
            {
                Relation[] rels = new Relation[mem.m_byPrimaryId.size()];
                IEnumerator e = mem.m_byPrimaryId.Values.GetEnumerator();
                int i=0;
                while (e.MoveNext())
                {   
                    rels[i++] = (Relation)(e.Current);
                }
                return rels;
            }
            return null;
        }
        */
        public Relation[] getRels(int typeId) throws Exception
        {
            return getMemory(typeId).getRels();
        }

        public int delRels(int typeId,int fkId,int dim,boolean purgeConfirmed) throws Exception
        {
            return getMemory(typeId).delRels(fkId,dim,purgeConfirmed);
        }

        public int setRels(int typeId,int fkId,int dim,double posEvidence) throws Exception
        {
            Relation[] rels = getRels(typeId,fkId,dim);
            if (rels!=null) for (int i=0;i<rels.length;i++)
            {
                if (!rels[i].getClass().equals(StoragerRelation.class))
                    throw(new Exception("Relation type mismatch"));
                else 
                    ((StoragerRelation)rels[i]).m_posEvidence = posEvidence;
            }
            return 1;
        }

        public Relation getRel(int typeId, int[] fkIds) throws Exception
        {
        	/*
            StoragerMemory mem = getMemory(typeId);
            if (fkIds==null || fkIds.length<mem.m_byForeignIds.length)
                throw(new Exception("Arity underflow"));
            if (fkIds.length>mem.m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            ArrayList a = ((ArrayList)(mem.m_byForeignIds[0][fkIds[0]]));
            if (a!=null)
                for (int i=0;i<a.Count;i++)
                {
                    Relation rel = (Relation)(a[i]); 
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
            */
            return getMemory(typeId).getRel(fkIds);
        }

        public Relation[] getRels(int typeId,int fkId,int dim,int from,int to) throws Exception
        {
        	/*
            StoragerMemory mem = getMemory(typeId);
            if (dim>=mem.m_byForeignIds.length)
                throw(new Exception("Arity overflow"));
            ArrayList a = ((ArrayList)(mem.m_byForeignIds[dim][fkId])).GetRange(from,to-from+1);
            return BasicProcessor.toRelations(a);
            */
        	return getMemory(typeId).getRels(fkId,dim,from,to);
        }

        public int addRel(Relation irel) throws Exception
        {
            StoragerRelation rel = new StoragerRelation(irel);
            if (rel.m_id==0)
                rel.m_id = ++m_topId; 
                
            StoragerMemory mem = getMemory(rel.getType());
            return mem.addRel(rel);
        }

        public int setRel(Relation rel) throws Exception
        {
            int id = rel.getId();
            if (id==0)
                return addRel(rel);
            else
            {
                // update the existing relation in place
                Relation oldrel = getRel(rel.getType(),id);
                if (!rel.equals(oldrel))
                    throw (new Exception("Set Relation mismatch"));
                oldrel.assign(rel);
                return id;
            }
        }

        public int delRel(int typeId,int pkId) throws Exception
        {
            return getMemory(typeId).delRel(pkId);
        }

        public int[] getTypes(int typeId) throws Exception            // returns types of the relation arguments
        {
            return getMemory(typeId).getTypes();
        }

}
