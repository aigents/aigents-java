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
package net.webstructor.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.al.Set;
import net.webstructor.al.Any;
import net.webstructor.al.All;
import net.webstructor.core.Environment;

public class TextMiner extends Miner {
	private Map documentFeatures = null;
	private Map categoryFeatures = null;
	private Map categoryDocuments = null;
	long tillTime = 0;
	public TextMiner(Environment env, LangPack languages, boolean debug) {
		this(env, languages, 0, debug);
	}
	public TextMiner(Environment env, LangPack languages, long tillTime, boolean debug) {
		super(env, languages, debug);
		this.tillTime = tillTime;
	}
	public TextMiner setDocuments(String[] documents, String[][] exclusions){
		setExclusions(exclusions);
		documentFeatures = toGraph(documents,documents);
		return this;
	}
	public TextMiner setDocuments(String[] documents){
		documentFeatures = toGraph(documents,documents);
		return this;
	}
	public TextMiner setDocuments(String[] documents,java.util.Set words){
		documentFeatures = toGraph(documents,documents,words);
		return this;
	}
	public TextMiner setDocuments(String[] names, String[] documents,java.util.Set vocabulary){
		documentFeatures = toGraph(names,documents,vocabulary);
		return this;
	}
	private void setExclusions(String[][] exclusions){
		this.exclusions = new HashSet();
		if (!AL.empty(exclusions)){
			for (int i = 0; i < exclusions.length; i++)
				if (!AL.empty(exclusions[i]))
					for (int j = 0; j < exclusions[i].length; j++)
						if (!AL.empty(exclusions[i][j]))
							this.exclusions.add(exclusions[i][j]);
		}
	}
	public TextMiner cluster() {
		Map[] out = cluster(documentFeatures,25,50,7,tillTime);
		categoryDocuments = out[0];
		categoryFeatures = out[1];
		return this;
	}
	public Map getCategoryDocuments(){
		return categoryDocuments;
	}
	public Map getCategoryFatures(){
		return categoryFeatures;
	}
	public java.util.Set getCategoryDocuments(Object category){
		return ((Linker)categoryDocuments.get(category)).keys();
	}
	public java.util.Set getCategoryFatures(Object category){
		return ((Linker)categoryFeatures.get(category)).keys();
	}
	public java.util.Set getCategories(){
		return categoryFeatures.keySet();
	}
	public Set getCategoryNames(){
		ArrayList names = new ArrayList();
		java.util.Set categories = categoryFeatures.keySet();
		for (Iterator it = categories.iterator(); it.hasNext();){
			Object category = it.next();
			names.add(new Any(((OrderedStringSet)category).toArray()));
		}
		Any[] sets = (Any[])names.toArray(new Any[]{});
		Arrays.sort(sets);
		return new All(sets);
	}
	
	//TODO: create patterns;
	//not working yet becase features in clustert name are not "flat"!!!???
	public TextMiner patternize(){
		//for each of categories
		//remove all its members that area lso members of other categories
		//replace them in all graphs
		Map newCategoryFatures = new HashMap();
		for (Iterator it = categoryFeatures.keySet().iterator(); it.hasNext();){
			Object cat = it.next();
			Linker features = (Linker)categoryFeatures.get(cat);
			Linker documents = (Linker)categoryDocuments.get(cat);
			if (cat instanceof String){
				newCategoryFatures.put(cat,features);//don't process single strings
			} else
			if (cat instanceof java.util.Set){
				java.util.Set catSet = (java.util.Set)cat;
				java.util.Set newCatSet = new HashSet(catSet);//clone
				boolean processed = false;
				for (Iterator jit = categoryFeatures.keySet().iterator(); jit.hasNext();){
					Object otherCat = jit.next();
					if (!otherCat.equals(cat)){
						if (otherCat instanceof String){
							newCatSet.remove((String)otherCat);
							processed = true;
						}else
						if (otherCat instanceof java.util.Set){
							newCatSet.removeAll((java.util.Set)otherCat);
							processed = true;
						}else{
							env.debug("Wrong category identity");
							processed = false;
							break;
						}
					}
				}
				//TODO:check duplication
				if (processed){
					if (newCatSet.isEmpty()){
						env.debug("Empty category identity");
						processed = false;
					}
					else
					if (newCategoryFatures.containsKey(newCatSet)){
						env.debug("Duplicate category identity");
						processed = false;
					}
					else{
						newCategoryFatures.put(newCatSet,features);
						documentFeatures.remove(cat);
						documentFeatures.put(newCatSet, documents);
					}
				}
				if (!processed){
					newCategoryFatures.put(cat,features);//use existing
				}
			}
			else {
				env.debug("Wrong category identity");
				newCategoryFatures.put(cat,features);
			}
		}
		categoryFeatures = newCategoryFatures;
		return this;
	}
}
