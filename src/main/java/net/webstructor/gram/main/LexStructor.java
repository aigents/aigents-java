/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents
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
package net.webstructor.gram.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Vector;//deprecate
import java.util.ArrayList;
import java.util.Comparator;

import net.webstructor.main.Mainer; 
import net.webstructor.al.Parser;
import net.webstructor.al.Seq;
import net.webstructor.util.Array;
import net.webstructor.util.Str;
import net.webstructor.gram.core.MemoryStore; 
import net.webstructor.gram.core.Item; 
import net.webstructor.gram.core.Ngram; 
import net.webstructor.gram.core.StringItem; 
import net.webstructor.gram.lang.Character;
import net.webstructor.gram.util.StringUtil;
import net.webstructor.gram.util.Format;

/**
 * 
 * Input: <current_folder> <lexicon_file> <database_file>
 * Output: <database_file>
 * 
 * @author Anton Kolonin
 *
 */
public class LexStructor extends Mainer
{
	public static int[] ngram(MemoryStore st,String line, boolean bWalls){
		if (line == null || line.length() == 0)
			return null;
		int length = line.length();
		int ids[];
		if (bWalls)
		{
			ids = new int[length+2];
			ids[0] = st.encounterCharacter(Character.LEFT_WALL);
			for (int i=0;i<length;i++)
				ids[i+1] = st.encounterCharacter(line.charAt(i));
			ids[length+1] = st.encounterCharacter(Character.RIGHT_WALL);
		} else {
			ids = new int[length];
			for (int i=0;i<length;i++)
				ids[i] = st.encounterCharacter(line.charAt(i));
		}
		return ids;
	}
	
	public static Vector readWordsToCharacterVector(BufferedReader br,MemoryStore st,boolean bWalls)
	{
		int count = 0;
		Vector v = new Vector();
		for (;;)
		{
			String tokens[];
			String line = null;
			float evidence = 1;
			try {
				line = br.readLine();
				tokens = StringUtil.toTokens(line, " \t", false);
				line = tokens[0];
				//if (line.length() > 1)
				//	evidence = StringUtil.toFloat(tokens[1], 0);					
			} catch (Exception e) { ; } // simply break on error
			if (line==null)
				break;
			line = line.toLowerCase().trim();
			int length = line.length();  
			if (length>0)
				v.add(new StringItem(0,evidence,new Ngram(ngram(st,line,bWalls))));
			count++;
			if (count%1000 == 0)
				println("Read "+count+" lines");
		}
		return v;
	}

	
	public static StringItem seq2Ngram(MemoryStore st, Seq tokens, boolean bWalls, String line) {
		float evidence = 1;

		int length = tokens.size();
		if (length == 0){//TODO: what to do in case of 1
			//v.add(new StringItem(0,evidence,new Ngram(new int[]{})));
			//if (sources != null)
			//	sources.add(line);
			return new StringItem(0,evidence,new Ngram(new int[]{}));
		}else
		if (length == 1){
//TODO: what to do in case of 1, count unigram!? 
			return new StringItem(0,evidence,new Ngram(new int[]{}));
		}else{//if (length > 1){
			if (bWalls){//TODO:add LEFT-WALL
				;
			} else {
				;
				//TODO: cleanup this unless we explicitly want to suppress periods
				//if (((String)tokens.get(length - 1)).equals("."))
				//	length--;
			}
			boolean skip = false;
			for (int i = 0; i < length; i++){
				int[] ngram = ngram(st,(String)tokens.get(i),bWalls);
				if (ngram == null){
					println("ERROR:"+line);//TODO: what?
					skip = true;
					break;
				}
			}
			if (skip)
				//continue;
				return null;
			int ids[] = new int[length];
			for (int i = 0; i < length; i++){
				int[] ngram = ngram(st,(String)tokens.get(i),bWalls);
				if (ngram == null){
					println("ERROR:"+line);//TODO: what?
					continue;
				}
				//ids[i] = st.encounterNgram(new Ngram(ngram(st,tokens[i],bWalls)),(float)evidence);
				ids[i] = st.encounterNgram(new Ngram(ngram),(float)evidence);
			}
			//v.add(new StringItem(0,evidence,new Ngram(ids)));
			//if (sources != null)
			//	sources.add(line);
			return new StringItem(0,evidence,new Ngram(ids));
		}
	}
	
	
	public static Vector readSentencesToWordVector(BufferedReader br,MemoryStore st,Vector sources,boolean bWalls,String skip1chars)
	{
		if (skip1chars != null)//TODO: consider array of tokens? 
			skip1chars = skip1chars.toLowerCase();
		int count = 0;
		Vector v = new Vector();
		for (;;)
		{
			String line = null;
			//float evidence = 1;
			try {
				line = br.readLine();
				if (line==null)
					break;
				line = line.toLowerCase().trim();
			} catch (Exception e) { ; } // simply break on error
			if (line.length() == 0)
				continue;
			
			//TODO: parse with/without LW and period
			//String tokens[] = StringUtil.toTokens(line, " \t", false);
			//int length = tokens.length;
			//Seq tokens = Parser.parse(line);//quoting=true by default
			//Seq tokens = Parser.parse(line,null,false,true,false,true,null);//quoting=false
			Seq tokens = new Seq(Parser.split(line," ",skip1chars));

			/*int length = tokens.size();
			if (length == 0){//TODO: what to do in case of 1
				v.add(new StringItem(0,evidence,new Ngram(new int[]{})));
				if (sources != null)
					sources.add(line);
			}else
			if (length > 0){//TODO: what to do in case of 1
			//if (length > 1){
				if (bWalls){//TODO:add LEFT-WALL
					;
				} else {
					;
					//TODO: cleanup this unless we explicitly want to suppress periods
					//if (((String)tokens.get(length - 1)).equals("."))
					//	length--;
				}
				boolean skip = false;
				for (int i = 0; i < length; i++){
					int[] ngram = ngram(st,(String)tokens.get(i),bWalls);
					if (ngram == null){
						println("ERROR:"+line);//TODO: what?
						skip = true;
						break;
					}
				}
				if (skip)
					continue;
				int ids[] = new int[length];
				for (int i = 0; i < length; i++){
					int[] ngram = ngram(st,(String)tokens.get(i),bWalls);
					if (ngram == null){
						println("ERROR:"+line);//TODO: what?
						continue;
					}
					//ids[i] = st.encounterNgram(new Ngram(ngram(st,tokens[i],bWalls)),(float)evidence);
					ids[i] = st.encounterNgram(new Ngram(ngram),(float)evidence);
				}
				v.add(new StringItem(0,evidence,new Ngram(ids)));
				if (sources != null)
					sources.add(line);
			}*/

			StringItem si = seq2Ngram(st,tokens,bWalls,line);
			if (si == null)
				continue;
			v.add(si);
			if (sources != null)
				sources.add(line);
			
			count++;
			if (count%1000 == 0)
				println("Read "+count+" lines");
		}
		return v;
	}

	// called by processVectorV1 (not called)
	public static int[] translateWithBigrams(int relationship[],MemoryStore ltm)
	{
		if (relationship==null || relationship.length<2)
			return relationship; 
		// count size of new relationship
		int linkCount = 0;
//println(StringUtil.toString(relationship,ltm));
		for (int i=1;i<relationship.length;i++)
		{
			int linkId = ltm.getLinkId(relationship[i-1],relationship[i]); 
			if (linkId != 0)
			{
//println(StringUtil.toString(ltm.getItem(linkId).getIds(),ltm));
				linkCount++;
				i++;
			}	
		}
		if (linkCount<1)
			return relationship; 
		int translated[] = new int[relationship.length-linkCount];
		linkCount = 0;
		for (int i=1;i<relationship.length;i++)
		{
			int linkId = ltm.getLinkId(relationship[i-1],relationship[i]); 
			if (linkId != 0)
			{
				translated[linkCount++] = linkId;
				i++;
			}	
			else
			{
				translated[linkCount++] = relationship[i-1];
			}
			if (i == relationship.length-1)
				translated[linkCount++] = relationship[i];
		}
		return translated;
	}
	
	/**
	 * @param input
	 * @param ltm
	 */
	// Not called!
	public static void processVectorV1(Vector input,MemoryStore ltm)
	{
		int iteration = 0;
		for (;;)
		{
			MemoryStore stm = new MemoryStore(null);
			// create hypothetical links/bi-grams
			for (int i=0;i<input.size();i++)
				createHypotheticalBigrams(((StringItem)input.elementAt(i)).getIds(),stm);
			// find most frequent bi-grams in STM
			Item top[] = stm.getTopItems(50);
			// if no most frequent bi-grams found, exit
			if (top == null)
				break;
			// export items from STM to LTM
			ltm.importItems(top);
			// rebuild sequence with most frequent bi-grams
			for (int i=0;i<input.size();i++)
			{
				input.setElementAt(translateWithBigrams(((StringItem)input.elementAt(i)).getIds(),ltm),i);
			}
			println("Iteration: "+(++iteration));
		}
	}

	static void createHypotheticalBigrams(int relationship[],MemoryStore stm)
	{
		for (int i=1;i<relationship.length;i++)
		{
			stm.encounterLink(relationship[i-1],relationship[i]);
		}
	}

	static ArrayList createHypotheticalNgrams(int relationship[],boolean all,int min,int max)
	{
		if (min<1)
			min=1;
		if (max>=relationship.length)
			max=relationship.length;
		ArrayList ngrams = new ArrayList();  
		for (int w=min;w<=max;w++)
		{
			for (int i=0;i+w<=relationship.length;i++)
			{
				int ngram[] = new int[w];
				System.arraycopy(relationship,i,ngram,0,w);
				ngrams.add(new Ngram(ngram));
				if (!all)//do the first only
					break;
			}
		}
		return ngrams;
	}
	
	public static void vectorNgramsEncounter(Vector input,MemoryStore m,int min,int max,int current_arity,int input_arities[])
	{
		// create hypothetical links/bi-grams
		for (int i=0;i<input.size();i++)
		{
			Object o = input.elementAt(i);
			StringItem it = (StringItem)o;
			if (current_arity > 0 && input_arities[i] != current_arity)
				continue;
			ArrayList ngrams = createHypotheticalNgrams(it.getIds(),true,min,max);
			for (int n=0;n<ngrams.size();n++){
				m.encounterNgram((Ngram)(ngrams.get(n)),it.getEvidence());
			}
		}
	}
		
	//called from main
	public static void learnBigramsFromVector(Vector input,MemoryStore ltm)
	{
		int iteration = 0;
		for (;;)
		{
			MemoryStore stm = new MemoryStore(null);
			// create hypothetical links/bi-grams in STM
			for (int i=0;i<input.size();i++)
				createHypotheticalBigrams(((StringItem)input.elementAt(i)).getIds(),stm);
			
			// for each word, find most apparent bigram in STM, 
			// replace it and store it LTM
			int translatedCount = 0;
			for (int i=0;i<input.size();i++)
			{
				int relationship[] = ((StringItem)input.elementAt(i)).getIds();
				if (relationship.length>1)
				{
					float topEvidence = 0;
					int topItemIdx = 0;
					for (int j=1;j<relationship.length;j++)
					{
						Item it = stm.getLinkItem(relationship[j-1],relationship[j]);
						if (topEvidence < it.getEvidence())
						{
							topEvidence = it.getEvidence();
							topItemIdx = j;
						}
					}
					if (topItemIdx != 0)
					{
						int j;
						int translated[] = new int[relationship.length-1];
						for (j=0;j<(topItemIdx-1);j++)
							translated[j] = relationship[j];
						translated[j] = ltm.encounterLink(relationship[j],relationship[j+1]);
						j++;
						for (;j<translated.length;j++)
							translated[j] = relationship[j+1];
						//TODO
						input.setElementAt(translated,i);
						translatedCount++;
//println(StringUtil.toString(translated,ltm));
					}
				}
			}
			println("Iteration: "+(++iteration));
			if (translatedCount == 0)
				break;
		}
	}

	//called from main
	public static void learnNgramsFromVector(Vector input,MemoryStore ltm,int min,int max,boolean all,boolean denominate,boolean addmissed,boolean incremental){
		if (!incremental)
			learnNgramsFromVector(input,ltm,min,max,all,denominate,addmissed,0,null);
		else {
			int arities[] = new int[input.size()];
			int max_arity = 0;
			for (int i = 0; i < arities.length; i++){
				int arity = ((StringItem)input.elementAt(i)).getArity();
				arities[i] = arity;
				if (max_arity < arity)
					max_arity = arity;
			}
			for (int arity = 2;arity <= max_arity;arity++){
				println("Arity: "+arity);
				learnNgramsFromVector(input,ltm,min,max,all,denominate,addmissed,arity,arities);
				int processed = 0;
				for (int i = 0; i < arities.length; i++)
					if (arities[i] == arity)
						processed++;
				println("Arity "+arity+" count:"+processed);
				//ltm.save("tmp"+arity+".adb",true);
			}
		}
	}
	
	//TODO: fix hack!?
	public static void learnNgramsFromVector(Vector input,MemoryStore ltm,int min,int max,boolean all,boolean denominate,boolean addmissed,int current_arity,int input_arities[])
	{
		for (int iteration = 1;;iteration++)
		{
			println("Iteration: "+iteration);
			MemoryStore stm = new MemoryStore(null);
			// create hypothetical n-grams in STM
			vectorNgramsEncounter(input,stm,min,max,current_arity,input_arities);

			// for each word, find most apparent n-gram in STM, 
			// replace it and store it LTM
			int translatedCount = 0;
			for (int i=0;i<input.size();i++){
				StringItem thisItem = (StringItem)input.elementAt(i);
				//if (current_arity > 0 && input_arities[i] != current_arity)
				//	continue;
				float topValue = -1;
				Item topItem = null;
				int relationship[] = thisItem.getIds();
				if (relationship.length > 1) {
					String sword = StringUtil.toString(relationship, ltm, "", ""); 
					boolean debug = sword.equals("яростно") ? true : false;// "ярости" "яростно"
					if (debug)
						println(StringUtil.toString(relationship, ltm, "[", "]"));
					ArrayList ngrams = createHypotheticalNgrams(relationship,all,min,max);					
					for (int n=0;n<ngrams.size();n++)
					{
						Ngram ngram = (Ngram)ngrams.get(n);
						Item it = stm.getItem(ngram);
						if (it == null)
							continue;
						
						float value = ((float)it.getEvidence()) * it.getArity();
						if (denominate && ngram.m_ints != null && ngram.m_ints.length > 0){
							//TODO: note that with denomination arity does not matter with POC-English!!!
							value = (float)ltm.denominate(value,ngram.m_ints);
						}
						
						if (debug)
							println(StringUtil.toString(ngram.m_ints, ltm, "[", "]")+"="+value);
						//TODO: which policy is the best?
						if (topValue < value)//best choice first
						//if (topValue > value || topValue == -1)//worse choice first
						{
							topValue = value;
							topItem = it;
						}
					}
					if (topItem != null && !topItem.equals(thisItem))
					{
						Ngram ngram = (Ngram)topItem.getName();

						if (debug) {
							println(StringUtil.toString(ngram.m_ints, ltm, "[", "]"));
							sword = null;
						}
						
						int newId = ltm.encounterNgram(ngram,thisItem.getEvidence());
						
						int translated[] = ngram.translate(relationship,newId);
						input.setElementAt(new StringItem(0,thisItem.getEvidence(),new Ngram(translated)),i);
						translatedCount++;
					}
					else //item is it not translated yet but not present in LTM 
					if (topItem != null && addmissed && ltm.getItem((Ngram)topItem.getName())==null)
					{
						Ngram ngram = (Ngram)topItem.getName();
						ltm.encounterNgram(ngram,thisItem.getEvidence());
						translatedCount++;
					}
				}
			}
			if (translatedCount == 0)
				break;
		}
	}


	public static Item printBigrams(MemoryStore stm,MemoryStore ltm,int[] n,int lastId) 
	{
		if (n == null || n.length == 0)			
			return null;
		Item bestItem = null;
		if (n.length == 1)
			if (n[0] == lastId)
				println(""); 
		else
		if (n.length == 2)			
			bestItem=  stm.getLinkItem(n[0],n[1]);
		else {
			float bestRank = 0;
			int last = n.length - 1;
			for (int i = 1;i<last;i++)
			{
				int i11 = 0, i12 = i-1, i21 = i, i22 = last;
				int l1 = i12-i11+1, l2 = i22-i21+1;
				int[] n1= new int[l1];
				int[] n2= new int[l2];						
				System.arraycopy(n,i11,n1,0,l1);
				System.arraycopy(n,i21,n2,0,l2);
				Item it1 = findBestBigramItem(stm,ltm,n1);//recursion left
				Item it2 = findBestBigramItem(stm,ltm,n2);//recursion right
				float rank = it1.getEvidence() + it2.getEvidence();
				if (bestRank < rank) {
					bestRank = rank;
					int newId = ltm.encounterLink(it1.getId(),it2.getId());//TODO: actual evidence?
					bestItem = ltm.getItem(newId);
				}
			}
		}
		bestItem.getIds();
		return bestItem;
	}
	
	public static Item findBestBigramItem(MemoryStore stm,MemoryStore ltm,int[] n) 
	{
		if (n == null || n.length == 0)			
			return null;
		Item bestItem = null;
		if (n.length == 1)
			bestItem = stm.getItem(n[0]); 
		else
		if (n.length == 2)			
			bestItem=  stm.getLinkItem(n[0],n[1]);
		else {
			float bestRank = 0;
			int last = n.length - 1;
			for (int i = 1;i<last;i++)
			{
				int i11 = 0, i12 = i-1, i21 = i, i22 = last;
				int l1 = i12-i11+1, l2 = i22-i21+1;
				int[] n1= new int[l1];
				int[] n2= new int[l2];						
				System.arraycopy(n,i11,n1,0,l1);
				System.arraycopy(n,i21,n2,0,l2);
				Item it1 = findBestBigramItem(stm,ltm,n1);//recursion left
				Item it2 = findBestBigramItem(stm,ltm,n2);//recursion right
				float rank = it1.getEvidence() + it2.getEvidence();
				if (bestRank < rank) {
					bestRank = rank;
					int newId = ltm.encounterLink(it1.getId(),it2.getId());//TODO: actual evidence?
					bestItem = ltm.getItem(newId);
				}
			}
		}
		bestItem.getIds();
		return bestItem;
	}
	
	//called from main
	public static void learnBestBigramsFromVector(Vector input,MemoryStore ltm,int min,int max,boolean all)
	{
		MemoryStore stm = new MemoryStore(null);
		// create hypothetical bigrams in STM
		for (int i=0;i<input.size();i++)
			createHypotheticalBigrams(((StringItem)input.elementAt(i)).getIds(),stm);

		// for each word, find most competitive n-gram in STM and store it LTM
		for (int i=0;i<input.size();i++){
			int ids[] = ((StringItem)input.elementAt(i)).getIds();
			printBigrams(stm, ltm, ids, ids[ids.length-1] );
			//Item newItem = findBestBigramItem(stm, ltm, ((StringItem)input.elementAt(i)).getIds() );
			//input.setElementAt(newItem,i);
			//TODO: copy the winner from stm to ltm
		}
	}

	
	public static void save(String path,Vector output,MemoryStore store, String delim)
	{
    	try {
	        BufferedWriter sw = new BufferedWriter(new FileWriter(path));
			for (int i=0;i<output.size();i++)
			{
				int r[] = ((Item)output.elementAt(i)).getIds();
				StringBuilder sb = new StringBuilder(StringUtil.toString(r,store,null,null,delim))
					.append('\t')
					.append(StringUtil.toString(r,store,Format.m_openArrayDelim,Format.m_closeArrayDelim));
				sw.write(sb.toString());
            	sw.newLine();
			}
	        sw.close();
    	} catch (Exception e) {
    		System.out.println(e+", save");
    		e.printStackTrace();
    	}
	}

	public static int getParseWords(MemoryStore store,Item item,ArrayList words,ArrayList links){
		int[] ids = item.getIds();
		int[] indexes = new int[ids.length];
		for (int i = 0; i < ids.length; i++){
			Item child = store.getItem(ids[i]);
			if (child.getIds() == null){
				String word = item.toString(store,null,null,null);
				words.add(word);
				return words.size() - 1;
			}
			indexes[i] = getParseWords(store,child,words,links);
			if (i > 0)
				links.add(new int[]{indexes[i-1],indexes[i]});
		}
		//TODO: return less evident member for linkage?
		/*
		int id = 0;
		float min = Float.MAX_VALUE;
		for (int i = 0; i < indexes.length; i++){
			float e = store.getItem(indexes[i]).getEvidence();
			if (min > e){
				min = e;
				id = indexes[i];
			}
		}
		return id;
		*/
		return indexes[ids.length / 2];
	}

	/**
	 * Save parses to OpenCog-s Unsupervised Language Learning ULL parse format
	 * @param path
	 * @param output
	 * @param store
	 */
	public static void saveULL(String path,Vector output,Vector sources,MemoryStore store){
    	try {
	        BufferedWriter sw = new BufferedWriter(new FileWriter(path));
			for (int i=0;i<output.size();i++)
			{
				Item item = (Item)output.elementAt(i);
				ArrayList words = new ArrayList();
				ArrayList links = new ArrayList();
				//get DTree (Dependency Tree or Dependency Grammar - DG) from CTree (Constituent Tree or Phrase Structure Grammar - PSG) 
				if (item.getArity() > 0)
					getParseWords(store,item,words,links);//use Right Hand Side (RHS) words in a tree to link
				if (sources != null) // write source
					sw.write((String)sources.get(i));
				else //write all words
				for (int w = 0; w < words.size(); w++){
					if (w != 0)
						sw.write(" ");
					sw.write(words.get(w).toString());
				}
            	sw.newLine();
				Collections.sort(links, new Comparator(){
					@Override
					public int compare(Object o1, Object o2) {
						int[] l1 = (int[])o1;
						int[] l2 = (int[])o2;
						return l1[0] < l2[0] ? -1 : l1[0] > l2[0] ? 1 : l1[1] < l2[1] ? -1 : l1[1] > l2[1] ? 1 : 0;
					}});
				for (int l = 0; l < links.size(); l++){
					int[] link = (int[])links.get(l);
					String w0 = (String)words.get(link[0]);
					String w1 = (String)words.get(link[1]);
					sw.write((link[0]+1)+" "+w0+" "+(link[1]+1)+" "+w1);
	            	sw.newLine();
				}
            	sw.newLine();
			}
	        sw.close();
    	} catch (Exception e) {
    		System.out.println(e+", save");
    		e.printStackTrace();
    	}
	}
	
	public static void processWords(MemoryStore store, BufferedReader breader, String dataPath, String outPath){
		Vector v = readWordsToCharacterVector(breader,store,false);//walls=true|false(plain),
		println("Processing input "+dataPath);
		  
		//old code		
		//learnBigramsFromVector(v,store);
		
		//new code
		learnNgramsFromVector(v,store,2,100,true,false,false,false);//all=true|false(left)
		//very new code
		//learnBestBigramsFromVector(v,store,2,2,true);//all=true|false(left)
		
		//very old code
		//analyzeVector(v,store);
		  
		//save rebuilt sequence
		println("Saving output "+outPath);
		save(outPath,v,store,null);
	}
	
	public static void processSentencesDir(MemoryStore store, String dataPath, File dir, String outPath, String ullPath, boolean incremental, String skip){
		Vector all = new Vector();
		Vector sources = new Vector();
		
		println("Reading input dir "+dataPath);
		String[] children = dir.list();
		int[] sizes = new int[children.length];
		for (int i=0; i<children.length; i++) {
			File file = new File(dir,children[i]);
			BufferedReader breader = Mainer.getReader(file.getAbsolutePath());
			Vector v = readSentencesToWordVector(breader,store,sources,false,skip);//walls=true|false(plain),
			sizes[i] = v.size();
			all.addAll(v);
			println("Read input file "+children[i]+" "+v.size());
		}
		  
		println("Processing input");
		learnNgramsFromVector(all,store,2,2,true,true,false,incremental);//all=true|false(left)
		  
		//save rebuilt sequence
		println("Saving output");
		File outDir = new File(outPath);
		File ullDir = new File(ullPath);
		outDir.mkdir();
		ullDir.mkdir();
		int start = 0;
		Vector v, s;
		for (int i=0; i<children.length; i++) {
			v = new Vector();
			s = sources != null ? new Vector(v) : null;
			for (int j = 0; j < sizes[i]; j++){
				v.add(all.get(start+j));
				if (sources != null)
					s.add(sources.get(start+j));
			}
			start += sizes[i];
			save((new File(outDir,children[i])).getAbsolutePath()+".out",v,store," ");
			saveULL((new File(ullDir,children[i])).getAbsolutePath()+".ull",v,s,store);
		}
	}
	
	public static void processSentencesFile(MemoryStore store, BufferedReader breader, String dataPath, String outPath, boolean incremental, String skip){
		Vector sources = new Vector();
		Vector v = readSentencesToWordVector(breader,store,sources,false,skip);//walls=true|false(plain),
		println("Processing input "+dataPath);
		  
		//new code
		learnNgramsFromVector(v,store,2,2,true,true,false,incremental);//all=true|false(left)
		//learnNgramsFromVector(v,store,2,2,true,true);//all=true|false(left)
		//learnNgramsFromVector(v,store,2,100,true);//all=true|false(left)
		  
		//save rebuilt sequence
		println("Saving output "+outPath);
		save(outPath,v,store," ");
		
		//TODO: store parses as ULL
		saveULL(dataPath.substring(0,dataPath.lastIndexOf('.'))+".ull",v,sources,store);
	}
	
	public static void main(String args[])
	{
		if (args.length<3)
		{
			println("Usage:"
					+"\tInput: <current_folder> <lexicon_file> <database_file> [words|sentences]"
					+"\tOutput: <database_file>");
			return;
		}
	  
		if (args[0] != null)
			setCurrentDirectory(args[0]);

		String dataPath = new File(args[1]).getAbsolutePath();	
		String basePath = new File(args[2]).getAbsolutePath();							

		File dir = new File(dataPath);
		BufferedReader breader = null;
		String outPath = null;
		
		//println("Reading file "+dataPath);
		//MemoryStore store = new MemoryStore(basePath);
		MemoryStore store = new MemoryStore(null);
		
		if (!dir.isDirectory()) {
			breader = Mainer.getReader(dataPath);
			if (breader == null){
				println("Illegal input file "+dataPath);
				return;
			}
			outPath = dataPath.substring(0,dataPath.indexOf('.'))+".out"; 
		}

		boolean incremental = Array.contains(args, "incremental");
		String skip = Str.arg(args, "skip", null);
		
		if (args.length < 4 || args[3].equals("words"))
			processWords(store, breader, dataPath, outPath);
		else
		if (args[3].equals("sentences") && !dir.isDirectory())
			processSentencesFile(store, breader, dataPath, outPath, incremental, skip);
		else
		if (args[3].equals("sentences") && dir.isDirectory())
			processSentencesDir(store, dataPath, dir, dataPath+"_out", dataPath+"_ull", incremental, skip);
			
		try {
			//println("Saving database "+basePath);
			store.save(basePath,true);
		} catch (Exception e) {
			System.out.println(e);
		}
		println("Done");
	}	
}
