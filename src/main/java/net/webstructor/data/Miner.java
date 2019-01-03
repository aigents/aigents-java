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

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Map;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Parser;
import net.webstructor.al.Set;
import net.webstructor.al.Writer;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Environment;
import net.webstructor.main.Mainer;
import net.webstructor.util.Array;

/**
LTM storage - forward:
./storage/$sourceName(dir)/$propertyName(HashMap<$targetName,Value>)
LTM storage - backward:
./storage/$propertyName(dir)/$targetName(HashMap<$sourceName,Value>)

STM-LTM architecture
- STM - hashmaps
- STMLTM - HashMap<name,last use time if in memory> 
- LTM - file storage

STM forgetting principle (removal from hashtables oldest ones based on memory state)?
LTM forgetting principle (removal of long ago not read files based on disk usage)?

Object types:
U - User
D - Document
F - feature
W - Word

Problems:
DW => DF - from document words, create document features
DF => DC,CF - from document features, create document categories and category features
UD,DC => UC - from user documents and document categories, create user categories   
UD,DF => UF - from user documents and document features, create user features
UD,DC => UC - from user documents and document categories, create user categories   
UF => UC,CF - from user features, create user categories and category features
...
 
Miner architecture:
interface Linker(FeatureVector)
  class Counter(HashMap<Object,Integer>)
  class Weighter(HashMap<Object,Float>)
 
 
Clustering:
- Have set of documents with features
- Have top features across documents, exceeding theshold X%
- Create clusters of feature sets, linking documents to them
- Repeat
-- Create cluster-to cluster similarity measures
-- If there is similarity measure greater than Y% (X%), merge the clusters
- Until no similarity measure greater than Y% (X%)
Test data:
https://www.diigo.com/user/Obaskov/semantic


Pattern formation:

мама мыла раму
мама мыла руку
мама шила шубу
мама шила шарф

мама<4> мыла<2> раму<1>
мама<4> мыла<2> руку<1>
мама<4> шила<2> шубу<1>
мама<4> шила<2> шарф<1>

мама мыла $x{раму руку}
мама шила $y{шубу шарф}

мама $z{ [мыла $x{раму руку}] [шила $y{шубу шарф}] }

мама { [мыла {раму руку}] [шила {шубу шарф}] }
==================================================
{[мама мыла раму][мама мыла руку][мама шила шубу][мама шила шарф]}
мама<4> мыла<2> шила<2> раму<1> руку<1> шубу<1> шарф<1>
12
=>
мама { [мыла раму] [мыла руку] [шила шубу] [шила шарф] }
мама<1> мыла<2> шила<2> раму<1> руку<1> шубу<1> шарф<1>
9
=>
мама { [мыла {раму руку}] [шила {шубу шарф}] }
мама<1> мыла<1> шила<1> раму<1> руку<1> шубу<1> шарф<1>
7
==================================================
Algorithm:
- get list of texts as ordered frames.
- ...
==================================================
{[tuna is a fish][cat is a mammal][cat has a tail][tuna has a fin]}
tuna<2> cat<2> is<2> has<2> a<4> fish<1> mammal<1> tail<1> fin<1>
16

{[{[tuna is] [cat is]} a {fish mammal}] [{[tuna has] [cat has]} a {tail fin}]}
14
[[{tuna cat} is] a {fish mammal}] [[{tuna cat} has] a {tail fin}]
10 ({tuna cat} is a node)

HOW TO SPLIT!? 
*/

public class Miner {
	HttpFileReader reader;
	Environment env;
	//TODO:move exclusions, languages, excluded and toGraph to TextMiner
	HashSet exclusions = null;
	LangPack languages;
	boolean debug = false;

	public Miner(Environment env, LangPack languages, boolean debug) {
		this.env = env;
		this.languages = languages;
		this.debug = debug;
		this.reader = new HttpFileReader(env,Body.http_user_agent);
	}
	
	/**
	 * Symmetic overlap measure 0..1.0
	 * @param that
	 * @param other
	 * @return
	 */
	public double crossOverlap(Linker that, Linker other){
		HashMap crossnorm = new HashMap();
		for (Iterator it = that.keys().iterator(); it.hasNext();){
			Object key = it.next();
			Integer thisI = (Integer)that.value(key);
			Integer otherI = (Integer)other.value(key);
			if (otherI == null)
				otherI = new Integer(0);
			crossnorm.put(key, new Integer[]{
				new Integer(Math.min(thisI.intValue(),otherI.intValue())),
				new Integer(Math.max(thisI.intValue(),otherI.intValue()))}); 
		}
		//for symmetricity, count other in denominator 
		for (Iterator ot = other.keys().iterator(); ot.hasNext();){
			Object key = ot.next();
			Integer otherI = (Integer)other.value(key);
			if (!crossnorm.containsKey(key))
				crossnorm.put(key, new Integer[]{new Integer(0),otherI}); 
		}
		float num = 0, den = 0;
		for (Iterator it = crossnorm.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Integer[] data = (Integer[])crossnorm.get(key);
			num += data[0].floatValue(); 
			den += data[1].floatValue();
		}
		return den == 0 ? 0 : num / den;
	}

	/*
	//TODO: all specific al.Set cases!
	//TODO: move to Set?
	Any merge(java.util.Set set) {
		ArrayList list = new ArrayList();
		for (Iterator it = set.iterator(); it.hasNext();){
			Object o = it.next();
			if (o instanceof java.util.Set){
				for (Iterator jit = ((java.util.Set)o).iterator(); jit.hasNext();)
					list.add(jit.next());
			}else 
			if (o instanceof Set){
				for (int i = 0; i < ((Set)o).size(); i++)
					list.add(((Set)o).get(i));
			}else
				list.add(o);
		}
		Arrays.sort(list.toArray(new String[]{}));
	}
	*/
	
	/**
	 * Cluster sources by targets and return by-cluster sources and targets
	 * Example: cluster(documentFeatures) => categoryDocuments, categoryFeatures
	 * @param sourceTargets
	 * @return Linker[] sources, Linker[] targets  
	 */
	Map[] cluster(Map sourceTargets,int similarityThreshold,int featureVolume,int maxCategories,long tillTime) {
	//Linker[][] cluster(Object[] sources,Linker[] sourceTargets,int sourceThreshold,int targetThreshold) {
		//- Have set of documents with features
		//- Create clusters of feature sets, linking documents to them
		//- Have top features across documents, exceeding theshold X%
		
		//TODO: sort out data structure
		HashMap allTargetSources = new HashMap();//all original features to be merged 
		HashMap targetSources = new HashMap();//currently merged features
		
		Counter total = new Counter();
		for (Iterator sources = sourceTargets.keySet().iterator(); sources.hasNext();) {
			Object source = sources.next(); 
			Linker linker = (Linker)sourceTargets.get(source);
			for (Iterator it = linker.keys().iterator(); it.hasNext();) {
				Object key = it.next();
				//TODO:float values!?
				//int value = linker.value(key).intValue();
				total.count(key);
				
				//TODO: sort out data structure
				Linker reverse = (Linker)allTargetSources.get(key);
				if (reverse == null)
					allTargetSources.put(key, reverse = new Counter());
				//reverse.count(source,value);
				reverse.count(source);
			}
		}

		//TODO: have this done on basis of TFIDF norm, not relative (100-0) norm
		//total.normalizeBy(languages.words(), 1);
		//Object[][] toRanked = total.toRanked();
		Object[][] toRanked = total.toRanked(sourceTargets.size());
			
		int[] bounds = Counter.getBounds(toRanked,featureVolume,maxCategories);
		if (bounds == null){
			//TODO: how can that be possible!?
			env.error("Clustering with no bounds " + sourceTargets.toString(), new Exception());
			return new HashMap[]{targetSources,new HashMap()};
		}
		
		int upperThreshold = bounds[0];
		int lowerThreshold = bounds[1];

		if (debug ){
			for (int i = 0; i < toRanked.length; i++)
				env.debug(toRanked[i][0]+":"+toRanked[i][1]);
			env.debug("Range : "+upperThreshold+" - "+lowerThreshold);
		}
		
		for (int i = 0; i < toRanked.length; i++) {
			if (!allTargetSources.containsKey(toRanked[i][0]))
				continue;
			int value = ((Integer)toRanked[i][1]).intValue();
			if (debug)
				env.debug(toRanked[i][0]+":"+toRanked[i][1]);
			if (upperThreshold >= value && value >= lowerThreshold){
				targetSources.put(new OrderedStringSet((String)toRanked[i][0]),allTargetSources.get(toRanked[i][0]));
				allTargetSources.remove(toRanked[i][0]);
			}
		}
		
		//Now targetSources contain top linkers to their documents 
		
		//- Repeat
		//-- Create cluster-to cluster similarity measures
		//-- If there is similarity measure greater than Y% (X%), merge the clusters
		//- Until no similarity measure greater than Y% (X%)
		int iteration = 0;
		for (;;) {
			//break on timeout and left clusters in targetSources as is
			if (tillTime > 0 && System.currentTimeMillis() > tillTime){
				env.debug("Clustering timeout "+System.currentTimeMillis());
				break;
			}
			if (debug)
				env.debug("Iteration "+(++iteration));
			
			//TODO: sort out data structure
			//HashMap sourceSources = new HashMap();
			
			//ArrayList mergeSets = new ArrayList();
			HashSet mergeSets = new HashSet();

			//TODO: avoid double count!?
			//calculate all similarity measures
			int max = -1;
			//for (Iterator i1 = new HashSet(targetSources.keySet()).iterator(); i1.hasNext();) {
			//	Object k1 = i1.next();
			Object[] targetSourcesKeys = targetSources.keySet().toArray();
			if (!AL.empty(targetSourcesKeys))
			for (int i1 = 0 ; i1 < targetSourcesKeys.length; i1++){
				Object k1 = targetSourcesKeys[i1];
				Linker l1 = (Linker)targetSources.get(k1);

				if (debug)
					env.debug(k1.toString());
				for (Iterator il = l1.keys().iterator(); il.hasNext();) {
					Object source = il.next();
					if (debug)
						env.debug("   "+source+":"+l1.value(source));
				}
				
				//for (Iterator i2 = new HashSet(targetSources.keySet()).iterator(); i2.hasNext();) {
				//	Object k2 = i2.next();
				for (int i2 = i1 + 1; i2 < targetSourcesKeys.length; i2++){
					Object k2 = targetSourcesKeys[i2];
					Linker l2 = (Linker)targetSources.get(k2);
					if (!k1.equals(k2)) {

						//Linker mutual = (Linker)sourceSources.get(k1);
						//if (mutual == null)
						//	sourceSources.put(k1, mutual = new Counter());
						int closeness = (int)Math.round(crossOverlap(l1,l2)*100);
						if (closeness > similarityThreshold) {
							if (debug)
								env.debug("    "+k2+":"+closeness);
							//mutual.count(k2,closeness);//TODO: not needed?
							if (max <= closeness) {
								if (max < closeness) {
									max = closeness;
									mergeSets.clear();//start over
								}
								HashSet mergees = new HashSet();
								mergees.add(k1);
								mergees.add(k2);
								mergeSets.add(mergees);
							} 
						}
					}
				}				
			}
			
			if (mergeSets.isEmpty()){
				//TODO:magic
				if (targetSources.size() <= maxCategories || similarityThreshold == 0)
					break;
				else {
					similarityThreshold /= 2;
					continue;
				}
			}

			if (debug)
				env.debug("merge list before merge:");
			for (Iterator it = mergeSets.iterator(); it.hasNext();) {
				HashSet mergees = (HashSet)it.next();
				if (debug)
					env.debug(mergees.toString());
			}			
			//join chains in merge lists
			boolean repeat;
			do {
				repeat = false;
/*				
				for (Iterator i1 = mergeSets.iterator(); i1.hasNext();) {
					HashSet m1 = (HashSet)i1.next();
					for (Iterator i2 = mergeSets.iterator(); i2.hasNext();) {
						HashSet m2 = (HashSet)i2.next();
						if (!m2.equals(m1) && Array.intersect(m1, m2)) {
*/
/**/
				Object[] mergeSetsArray = mergeSets.toArray();
				if (AL.empty(mergeSetsArray))
				for (int i1 = 0; i1 < mergeSetsArray.length; i1++){
					HashSet m1 = (HashSet)mergeSetsArray[i1];
					for (int i2 = i1 + 1; i2 < mergeSetsArray.length; i2++){
						HashSet m2 = (HashSet)mergeSetsArray[i2];
						if (Array.intersect(m1, m2)) {
/**/
							mergeSets.remove(m1);
							mergeSets.remove(m2);
							m1.addAll(m2);
							mergeSets.add(m1);
							repeat = true;
							break;
						}			
					}
					if (repeat)
						break;
				}
			} while (repeat);

			if (debug)
				env.debug("merge list after merge:");
			for (Iterator it = mergeSets.iterator(); it.hasNext();) {
				HashSet mergees = (HashSet)it.next();
				if (debug)
					env.debug(mergees.toString());
			}			
			
			//TODO:perform merges in targetSources
			HashMap targetSourcesNew = new HashMap();
			for (Iterator it = mergeSets.iterator(); it.hasNext();) {
				HashSet mergees = (HashSet)it.next();
				Counter merged = new Counter();
				for (Iterator jt = mergees.iterator(); jt.hasNext();) {
					Object target = jt.next();
					Linker mergee = (Linker)targetSources.get(target);
if (mergee == null)//TODO:remove???
	continue;//env.debug("OOPS:"+target.toString());
					for (Iterator kt = mergee.keys().iterator(); kt.hasNext();) {
						Object source = kt.next();
						Number weight = mergee.value(source);
						merged.count(source, weight.intValue());
					}
					targetSources.remove(target);//remove merged items
				}
				//targetSourcesNew.put(mergees.toString(), merged);
				targetSourcesNew.put(OrderedStringSet.mergeAllSorted(mergees), merged);
			}		
			//TODO: merge remaining unmerged items into new
			targetSourcesNew.putAll(targetSources);
			targetSources = targetSourcesNew;
		}
		
		//build caterory features per cluster
		HashMap categoryTargets = new HashMap();
		total.clear();
		for (Iterator cit = targetSources.keySet().iterator(); cit.hasNext();) {
			Object category = cit.next();
			if (debug)
				env.debug(category.toString());
			Linker sources = (Linker)targetSources.get(category);
			Linker linker = new Counter();
			for (Iterator sit = sources.keys().iterator(); sit.hasNext();) {
				Object source = sit.next();
				int sourceValue = sources.value(source).intValue();
				if (debug)
					env.debug("  "+source.toString()+":"+sourceValue);
				Linker targets = (Linker)sourceTargets.get(source);
				for (Iterator tit = targets.keys().iterator(); tit.hasNext();) {
					Object target = tit.next();
					int targetValue = targets.value(target).intValue();
					if (debug)
						env.debug("    "+target.toString()+":"+targetValue);
					linker.count(target, sourceValue * targetValue);
					total.count(target);
				}
			}
			if (linker.size() > 0)
				categoryTargets.put(category, linker);
		}
		
		//TODO: build unique names!?
		/**/
		if (debug)
			env.debug("Category names:");
		HashMap renames = new HashMap();
		for (Iterator cit = categoryTargets.keySet().iterator(); cit.hasNext();) {
			OrderedStringSet category = (OrderedStringSet)cit.next();
			Counter linker = (Counter)categoryTargets.get(category);
			if (debug)
				env.debug("  "+category);
			if (debug)
				env.debug("      "+linker.toString());
			/*
			HashSet uniqueTargets = new HashSet();
			//keep adding features gradually from most unique to least unique till have something
			//for (int uniqueness = 1; uniqueTargets.isEmpty(); uniqueness++){
			//	for (Iterator tit = linker.keys().iterator(); tit.hasNext();){
			//		Object target = tit.next();
			//		if (linker.value(target).intValue() == uniqueness)
			//			uniqueTargets.add(target);
			//	}
			//}
			Object[][] selfRank = linker.toRanked();
			//Object[][] totalRank = total.toRanked();
			//linker = linker.normalizeBy(total);
			//Object[][] normRank = linker.toRanked(); 
			java.util.Set uniqueTargets = linker.getBest(); 
			if (debug){
				env.debug("    "+selfRank.toString());
				//env.debug("    "+totalRank.toString());
				//env.debug("    "+normRank.toString());
				//env.debug("    "+uniqueTargets);
			}
			if (uniqueTargets.size() != linker.size()){
				renames.put(category, new OrderedStringSet(uniqueTargets).sort());
			}
			*/
			if (category.size() > 7){//magic number
				java.util.Set best = linker.cloneFor(category).getBest(7);				
				if (best.size() != linker.size()){
					renames.put(category, new OrderedStringSet(best).sort());
				}
			}
		}
		if (!renames.isEmpty()){
			for (Iterator rit = renames.keySet().iterator(); rit.hasNext();){
				Object category = rit.next(); 
				OrderedStringSet newCategory = (OrderedStringSet)renames.get(category); 
				Linker linker = (Linker)categoryTargets.get(category);
				Linker sourceLinker = (Linker)targetSources.get(category);
				categoryTargets.remove(category);
				targetSources.remove(category);
				if (newCategory.size() > 0){
					categoryTargets.put(newCategory,linker);
					targetSources.put(newCategory, sourceLinker);
				}
			}
		}
				
		return new HashMap[]{targetSources,categoryTargets};
	}
	
	/**
	 * Infer connection between sources and targets using, induction, decuction or abduction
	 * Example: infer(userDocuments,documentFeatures) => userFeatures
	 * @param sources
	 * @param targets
	 * @return Linker[] sourceTargets
	 */
	Linker[] infer(Linker[] sources, Linker[] targets) {
		return null;
	}

	/**
	 * Generalizes original sources to more high-level associations 
	 * Example generalize(documentWords) => documentFeatures
	 * @param original
	 * @return Linker[] reduced
	 */
	Linker[] generalize(Linker[] original) {
		return null;
	}

	boolean excluded(String word) {
		if (word.length() < 2)
			return true;
		if (exclusions != null && exclusions.contains(word))
			return true;
		if (languages != null && languages.scrub(word))
			return true;
		return false;
	}
	
	public Map toGraph(String[] texts) {
		return toGraph(texts,texts,null);
	}

	public Map toGraph(String[] keys, String[] texts) {
		return toGraph(keys,texts,null);
	}

	public Map toGraph(String[] texts,java.util.Set words) {
		return toGraph(texts,null,words);
	}

	public Map toGraph(String[] keys, String[] texts, java.util.Set vocabulary) {
		HashMap graph = new HashMap();
		for (int i = 0; i < texts.length && i < keys.length; i++) {
			String key = keys[i];
			String text = texts[i];
			Linker linker = (Linker)graph.get(key);
			if (linker == null)
				linker = new Counter();
			//if (AL.isURL(text) && text.indexOf(' ') == -1 && reader.canReadDoc(text) && reader.canReadDoc(text)){//redindant read!?
			if (AL.isURL(text) && text.indexOf(' ') == -1 && reader.allowedForRobots(text) && reader.canReadDoc(text)){
				try {
					text = HtmlStripper.convert(reader.readDocData(text," "),HtmlStripper.block_breaker,null).toLowerCase();
				} catch (Exception e) {
					env.error("Can't read "+text, e);
					text = null;
				}
			}
			if (!AL.empty(text)){
				//TODO: formation of frames 
				
				//punctuation, no regexp, lowercase, no quiting, no urls
				//TODO: sort out punctuation together with Social Feeder!!!
				Set tokens = Parser.parse(text,AL.punctuation+AL.spaces,false,true,false,true);
				if (tokens != null) {
					for (int j = 0; j < tokens.size(); j++){
						String word = (String)tokens.get(j); 
						if (AL.isURL(word))
							continue;
						if (languages != null)
							word = languages.lowertrim(word);
						//TODO: use real freqs for scrubs, because some of them may be used in patterns
						if (vocabulary != null){//inclusion based on "best words"
							if (!vocabulary.contains(word))
								continue;
						} else {//inclusion based on "scrub list" fo exclusions
							if (excluded(word))
								continue;
						}
						linker.count(word);
					}
				}
				graph.put(key, linker);
			}
		}
		return graph;
	}

	public static String toString(Map graph, String type, String childBreaker, String parentBreaker) {
		StringBuilder sb = new StringBuilder();
		TreeSet pkeys = new TreeSet(graph.keySet()); //graph.keySet() 
		for (Iterator pit = pkeys.iterator(); pit.hasNext();){
			Object parent = pit.next();
			if (sb.length() > 0)
				sb.append(parentBreaker).append(' ');
			Linker children = (Linker)graph.get(parent);
			if (children.size() > 0) {
				sb.append('\'').append(Writer.toString(new Any(((OrderedStringSet)parent).toArray()))).append('\'')
					.append(' ').append(type).append(' ');
				boolean firstChild = true;
				TreeSet ckeys = new TreeSet(children.keys()); //children.keys()
				for (Iterator cit = ckeys.iterator(); cit.hasNext();){
					if (!firstChild)
						sb.append(childBreaker).append(' ');
					firstChild = false;
					sb.append(cit.next().toString());
				}
			}
		}
		if (sb.length() > 0)
			sb.append('.');
		return sb.toString();
	}

	public static void test(Environment env,boolean debug,String docs[]) {
		TextMiner m;
		m = new TextMiner(env,null,debug).setDocuments(docs).cluster();
		env.debug("Input:");
		for (int i = 0; i < docs.length; i++)
			env.debug(docs[i]);
		env.debug("Output:");
		env.debug(Writer.toString(m.getCategoryNames()));
		env.debug(toString(m.getCategoryDocuments(),"documents",",",";\n"));
		env.debug(toString(m.getCategoryFatures(),"features",",",";\n"));
		env.debug("");
	}
	
	public static void main(String args[]) {
		Environment env = new Mainer();
		/**
		test(env,false,new String[]{
				"one two three",
				"one two four",
				"one is number",
				"one is digit"});
		test(env,false,new String[]{
				"one two three",
				"one two many",
				"single is little",
				"single is unmarried"});
		test(env,false,new String[]{
			"one two three",
			"one two three four",
			"one is not many",
			"one hundred is many"});
		test(env,false,new String[]{
				"тунец это рыба",
				"кошка это млекопитающее",
				"петя работает программистом",
				"маша работает бухгалтером"});
		test(env,false,new String[]{
				"chinese live in china",
				"eagle is a bird",
				"fly is an insect",
				"french live in france",
				"snake is a reptile"});
		test(env,false,new String[]{
				"http://localtest.com/test/cat/chinese.html",
				"http://localtest.com/test/cat/eagle.html",
				"http://localtest.com/test/cat/fly.html",
				"http://localtest.com/test/cat/french.html",
				"http://localtest.com/test/cat/snake.html"});
		test(env,false,new String[]{
				"http://localtest.com/test/cat/fly.html",
				"http://localtest.com/test/cat/eagle.html",
				"http://localtest.com/test/cat/snake.html",
				"tuna is a fish",
				"cat is a mammal",
				"http://localtest.com/test/cat/french.html",
				"http://localtest.com/test/cat/chinese.html",
				"germans live in germany",
				"russians live in russia",
				"spaniards live in spain"});
		**/
		//TODO: make this working prperly my means of key feaure distribution/histogram clustering?
		/*
		test(env,false,new String[]{
				"тунец это рыба",
				"рыба плавает в море",
				"в море ходят корабли",
				"зайцы прячутся в кусты",
				"в лесу водятся волки",
				"кусты бывают в лесу",
				"в дом ведет дверь",
				"дверь имеет ручку",
				"дом стоит на земле"});
		*/
		test(env,false,new String[]{
				"тунец это рыба",
				"рыба плавает в море",
				"в море ходят корабли",
				"зайцы прячутся в кусты",
				"в лесу водятся волки",
				"кусты бывают в лесу",
				"в дом ведет дверь",
				"дверь имеет ручку",
				"дом стоит на земле"});
	}
}
