/*
 * MIT License
 * 
 * Copyright (c) 2005-2020 by Anton Kolonin, Aigents®
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import net.webstructor.al.AL;
import net.webstructor.core.Environment;
import net.webstructor.util.ArrayPositionComparator;

class CountedBucket {
	int count;
	int rank;
	double importance;
}

public class Counter extends HashMap implements Linker { 

	private static final long serialVersionUID = 1L;
	
	public Counter() {
	}
	public Counter(Linker other) {
		count(other);
	}
	public Counter(Environment env, String path) {
		this(env,path,"[ \t]",null);
	}
	public Counter(Environment env, String path, String splitRegExp, Integer def) {
		File file = env != null ? env.getFile(path) : new File(path);
		if (!file.exists() || !file.isFile() || !file.canRead())
			return;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			for (;;){
				String line = null;
				try {
					line = reader.readLine();
					if (line == null)
						break;
					String pair[] = line.split(splitRegExp);//was [ \t], but lexicon loader will use [\t] to keep phrases
					if (!AL.empty(pair) && !AL.empty(pair[0]) && ((pair.length > 1 && !AL.empty(pair[1])) || def != null)){
						String key = pair[0].toLowerCase().trim();
						Integer val = pair.length > 1 && !AL.empty(pair[1]) ? Integer.valueOf(pair[1].trim()) : def;
						put(key,val);
					}
				} catch (IOException e) {
					env.error("Counter error loading"+path, e);
					break;
				}
			}
			try {
				reader.close();
			} catch (IOException e) {
				env.error("Counter error closing"+path, e);
			}
		} catch (FileNotFoundException e1) {
			env.error("Counter error finding "+path, e1);
		}
	}
	//TODO:fix to round properly!!!
	//TODO:move to other place
	public static int round(Number value){
		//return value.intValue();
		return value instanceof Integer || value instanceof Long || value instanceof Short ? value.intValue() : Math.round(value.floatValue());
	}
	public Linker count(Linker other){
		if (other != null)
			for (Iterator it = other.keys().iterator(); it.hasNext();){
				Object key = it.next();
				count(key,round(other.value(key)));
			}
		return this;
	}
	public void count(Object key,ComplexNumber[] cn){
		Object counter = get(key);
		if (counter == null)
			this.put(key, cn);
		else
			this.put(key, ComplexNumber.add((ComplexNumber[])counter,cn));
	}
	public void count(Object key,int count){
		Integer counter = (Integer)get(key);
		this.put(key, new Integer(count + (counter == null ? 0 : counter.intValue() )));
	}
	public void count(Object key,double weight){
		Number counter = (Number)get(key);
		this.put(key, new Double(weight + (counter == null ? 0 : counter.doubleValue() )));
	}
	public void change(Object key,Number value){
		this.put(key, value);
	}
	public void count(Object key){
		count(key,1);
	}
	public java.util.Set keys() {
		return keySet();
	}
	public Number value(Object key) {
		Object o = get(key);
		return o instanceof Number ? (Number)get(key) : o instanceof ComplexNumber[] ? ComplexNumber.toNumber((ComplexNumber[])o) : null;
	}
	public Number value(Object key,int def){
		Number value = value(key);
		return value == null ? new Integer(def) : value;
	}
	//TODO: remove second loop? denominate resulting values? 
	//TODO: account for zeroes!!!
	public Object[] crossOverlap(Counter other){
		HashMap crossnorm = new HashMap();
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Integer thisI = (Integer)get(key);
			Integer otherI = (Integer)other.get(key);
			if (otherI == null)
				otherI = new Integer(0);
			crossnorm.put(key, new Integer[]{
				new Integer(Math.min(thisI.intValue(),otherI.intValue())),
				new Integer(Math.max(thisI.intValue(),otherI.intValue()))}); 
		}
		float num = 0, den = 0;
		Counter norm = new Counter();
		for (Iterator it = crossnorm.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Integer[] data = (Integer[])crossnorm.get(key);
			num += data[0].floatValue(); 
			den += data[1].floatValue();
			norm.count(key, ((Integer)data[0]).intValue() ); 
		}
		return new Object[]{new Float( den == 0 ? 0 : num / den ),norm};
	}

	//TODO: remove second loop? denominate resulting values? 
	public Object[] crossMultiplied(Counter other){
		HashMap crossnorm = new HashMap();
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Integer i1 = (Integer)get(key);
			Integer i2 = (Integer)other.get(key);
			if (i2 == null)
				i2 = new Integer(0);
			if (i2 != null)
				crossnorm.put(key, new Integer[]{mul(i1,i2),maxsqr(i1,i2)}); 
		}
		float num = 0, den = 0;
		Counter norm = new Counter();
		for (Iterator it = crossnorm.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Integer[] data = (Integer[])crossnorm.get(key);
			num += data[0].floatValue(); 
			den += data[1].floatValue();
			norm.count(key, (int)(Math.round( Math.sqrt(data[0].floatValue()) )) ); 
		}
		return new Object[]{new Float( den == 0 ? 0 : num / den ),norm};
	}

	//automatically determine two integer bunds
	public static int[] getBounds(Object[][] toRanked,int percentageNeeded,int maxCategories){
		if (AL.empty(toRanked))
			return null;
		int allRanked = toRanked.length;
		int nowRanked = allRanked;
		//TODO: sort out min and max
		int maxRanked = Math.max(allRanked * percentageNeeded / 100, allRanked / maxCategories);
		int minRanked = maxRanked / 2;//Math.min(allRanked * percentageNeeded / 100, allRanked / maxCategories);
		int upperIndex = 0;
		int lowerIndex = toRanked.length - 1;
		int upperThreshold = ((Integer)toRanked[upperIndex][1]).intValue(); 
		int lowerThreshold = ((Integer)toRanked[lowerIndex][1]).intValue();
		int direction = -1;//start from the bottom rank
		if (percentageNeeded < 100)//TODO: fix hack with checking percentage before decreasing and making sure that unit tests still pass! 
		for(;;){
			if (direction == 1)
			{
				//lower upper threshold while the same value
				for(;;){
					if ((upperIndex + 1) > lowerIndex)
						break;
					if (((Integer)toRanked[upperIndex][1]).intValue() == upperThreshold){
						upperIndex++;
						nowRanked--;
					} else {
						upperThreshold = ((Integer)toRanked[upperIndex][1]).intValue();
						break;
					}
				}
				direction = -1;
			} 
			else
			{
				//raise lower threshold while the same value
				for(;;){
					if ((lowerIndex - 1) < upperIndex)
						break;
					if (((Integer)toRanked[lowerIndex][1]).intValue() == lowerThreshold){
						lowerIndex--;
						nowRanked--;
					} else {
						lowerThreshold = ((Integer)toRanked[lowerIndex][1]).intValue();
						break;
					}
				}
				direction = 1;
			}
			//break if EITHER
			//- have only one value cluster
			if (upperIndex == lowerIndex)
				break;
			//- have minimum?
			if (nowRanked < minRanked)
				break;
			//- not a scrub and have enough?
			if (50 >= upperThreshold && nowRanked < maxRanked)
				break;
		}
		return new int[]{upperThreshold,lowerThreshold};
	}
	
	public static int[] getThresholds(Object[][] toRanked,int percentageNeeded){
		if (AL.empty(toRanked))
			return null;//TODO: exception!?
		//get min and max
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (Object[] r : toRanked) {
			int rank = ((Number)r[1]).intValue();
			if (min > rank)
				min = rank;
			if (max < rank)
				max = rank;
		}
		if (min >= max)
			return null;//TODO: exception!?
		//compute importances
		TreeMap<Integer,CountedBucket> importances = new TreeMap<Integer,CountedBucket>();
		for (Object[] r : toRanked) {
			int rank = ((Number)r[1]).intValue();
			double importance = rank*rank*(max-rank+min);
			CountedBucket cb;
			if ((cb = importances.get(rank)) == null)
				importances.put(rank, cb = new CountedBucket()); 
			cb.count += 1;
			cb.rank = rank;
			cb.importance = importance;
		}
		CountedBucket[] buckets = importances.values().toArray(new CountedBucket[] {});
		Arrays.sort(buckets,new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				CountedBucket b1 = (CountedBucket)o1;
				CountedBucket b2 = (CountedBucket)o2;
				double d = b2.importance - b1.importance;
				return d < 0 ? -1 : d > 0 ? 1 : 0;
			}});
		//find enough feature buckets
		int featuresNeeded = toRanked.length * percentageNeeded / 100;
		int featuresFound = 0;
		int from = Integer.MAX_VALUE;
		int to = Integer.MIN_VALUE;
		for (CountedBucket cb : buckets) {
			if (featuresFound > 0 && featuresFound + cb.count > featuresNeeded)
				break;
			featuresFound += cb.count;
			if (from > cb.rank)
				from = cb.rank;
			if (to < cb.rank)
				to = cb.rank;
		}
		if (from > to)
			return null;//TODO: exception!?
		return new int[] {from,to};
	}

	public final Counter normalizeBy(Counter denominator, float defaultValue){
		Counter counter = this;
		Counter norm = new Counter();
		for (Iterator it = counter.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			float val = ((Integer)counter.get(key)).floatValue();
			Object denVal = denominator == null ? null : denominator.get(key);
			float den = denVal != null ? ((Integer)denVal).floatValue() : defaultValue;
			norm.put(key, new Integer(Math.round( val * 100 / den )));
		}
		return norm;
	}
	
	final public void mergeMax(Counter other){
		for (Iterator it = other.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Object otherVal = other.get(key);			
			Object thisVal = this.get(key);
			if (thisVal == null)
				put(key,otherVal);
			else{
				int otherInt = ((Number)otherVal).intValue();
				int thisInt = ((Number)thisVal).intValue();
				if (otherInt > thisInt)
					put(key,otherVal);
			}
		}
	}
	
	final public void mergeSum(Counter other){
		for (Iterator it = other.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Object otherVal = other.get(key);			
			Object thisVal = this.get(key);
			if (thisVal == null)
				put(key,otherVal);
			else
				put(key,sum((Integer)thisVal,(Integer)otherVal));
		}
	}
	
	public final void normalize(){
		Counter counter = this;
		double max = 0;
		for (Iterator it = counter.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			double val = ((Number)counter.get(key)).doubleValue();
			if (max < val)
				max = val;
		}
		for (Iterator it = counter.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			double val = ((Number)counter.get(key)).doubleValue();
			int normalized = (int)Math.round( val * 100 / max );
			if (normalized == 0 && val != 0)
				normalized = 1;
			counter.put(key, new Integer( normalized ));
		}
	}
	
	/**
	 * Return at least specified count or all ties
	 * @param min
	 * @return
	 */
	final java.util.Set getBest(int min){
		HashSet allBest = new HashSet();
		for(;;){
			HashSet best = new HashSet();
			int max = 0;
			for (Iterator it = keySet().iterator(); it.hasNext();){
				Object key = it.next();
				if (allBest.contains(key))//already counted
					continue;
				int val = value(key).intValue();		
				if (max <= val){
					if (max < val)
						best.clear();
					best.add(key);
					max = val;
				}
			}
			if (!allBest.isEmpty() && allBest.size() + best.size() > min)//if have at least some while more is too much
				break;
			allBest.addAll(best);
			if (allBest.size() == keySet().size())//if can't have more
				break;
		}
		return allBest;
	}

	final Counter cloneFor(Collection keys){
		Counter clone = new Counter();
		for (Iterator it = keys.iterator(); it.hasNext();){
			Object key = it.next();
			Number val = value(key);
			if (val != null)
				clone.put(key, val);
		}
		return clone;
	}
	
	public int toArrayOfValueCounts(ArrayList a){
		int max = 0;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Object val = get(key);
			int f = ((Integer)val).intValue();
			if (f > 0){
				a.add(new Object[]{key,val});
				if (max < f)
					max = f;
			}
		}
		return max;
	}
		
	//relative normalization to [100-0] range
	public Object[][] toRanked(){
		return toRanked(-1);
	}
	
	//absolute normaliztion given external norm (global maximum)
	public Object[][] toRanked(int max){
		ArrayList a = new ArrayList();
		int localMax = toArrayOfValueCounts(a);
		if (max == -1)
			max = localMax;
		Object[][] v = (Object[][])a.toArray(new Object[][]{});
		for (int i = 0; i < v.length; i++){
			v[i][1] = new Integer( Math.round((((Integer)v[i][1]).floatValue() * 100 / max)) );
		}
		Arrays.sort(v,new ArrayPositionComparator(1));
		return v;
	}
	
	public Object[][] toData(){
		Object[][] v = new Object[size()][];
		int i = 0;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			v[i] = new Object[2];
			Object key = it.next();
			Object val = get(key);
			v[i][0] = key;
			v[i][1] = val;
			i++;
		}
		return v;
	}
	
	public Object[][] toRankedWithZeroes(){
		Object[][] v = new Object[keySet().size()][];
		Iterator it = keySet().iterator();
		float max = 0;
		for (int i = 0; i < v.length; i++){
			Object key = it.next();
			Object val = get(key);
			v[i] = new Object[]{key,val};
			if (max < ((Integer)val).floatValue())
				max = ((Integer)val).floatValue();
		}
		for (int i = 0; i < v.length; i++){
			v[i][1] = new Integer( Math.round((((Integer)v[i][1]).floatValue() * 100 / max)) );
		}
		Arrays.sort(v,new ArrayPositionComparator(1));
		return v;
	}
	public String toString(){
		//return toString(keySet());
		StringBuilder sb = new StringBuilder();
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(key).append('<').append(get(key)).append('>');
		}
		return sb.toString();
	}
	public static String toString(Collection keySet){
		StringBuilder sb = new StringBuilder();
		for (Iterator it = keySet.iterator(); it.hasNext();){
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(it.next());
		}
		return sb.toString();
	}
	public static Integer maxsqr(Integer a, Integer b){
		int max = Math.max(a.intValue(),b.intValue());
		return new Integer(max*max);
	}
	public static Integer sum(Integer a, Integer b){
		if (b == null)
			return a;
		return new Integer(a.intValue() + b.intValue());
	}
	public static Integer mul(Integer a, Integer b){
		return new Integer(a.intValue() * b.intValue());
	}

	public static void main(String[] args) {
		Object[][] r = new Object[][] {
				{"0",new Long(100)},
				{"1",new Long( 60)},
				{"2",new Long( 40)},
				{"3",new Long( 30)},
				{"4",new Long( 25)},
				{"5",new Long( 21)},
				{"6",new Long( 18)},
				{"7",new Long( 13)},
				{"8",new Long( 11)},
				{"9",new Long( 10)}
		};
		int[] t = getThresholds(r,50);
		System.out.println(t);
	}
}
