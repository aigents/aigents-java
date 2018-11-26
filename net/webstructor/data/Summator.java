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

import net.webstructor.al.AL;
import net.webstructor.core.Environment;
import net.webstructor.util.ArrayPositionComparator;

public class Summator extends HashMap implements Linker {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2442648004211475042L;

	public Summator(){
	}
	
	public Summator(Environment env, String path){
		load(env,path);
	}
	
	public void count(Object key,int count){
		Double counter = (Double)get(key);
		put(key, new Double(count + (counter == null ? 0 : counter.doubleValue() )));
	}

	public void count(Object key, double value, double def){
		Double counter = (Double)get(key);
		put(key, new Double(value + (counter == null ? def : counter.doubleValue() )));
	}
	
	public void count(Object key){
		count(key,1);
	}

	public java.util.Set keys(){
		return keySet();
	}

	public Number value(Object key){
		return (Number)get(key);
	}

	public Number value(Object key,int def){
		return value(key, (double)def);
	}

	public Number value(Object key,double def){
		Number value = value(key);
		return value == null ? new Double(def) : value;
	}

	public void normalize(){
		normalize(false,false);//no log10, no zero
	}

	public void normalize(boolean log10,boolean zero){
		double max = 0;
		double min = Double.MAX_VALUE;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Object val = get(key);
			double f = ((Double)val).doubleValue();
			if (log10){
				f = f < 0 ? -Math.log10(1 - f) : Math.log10(1 + f);
				put(key, new Double(f));
			}
			if (f > 0 && max < f)
				max = f;
			if (zero && min > f)
				min = f;
		}
		if (max == 0)
			return;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Object val = get(key);
			double newval = zero ? (((Double)val).doubleValue() - min) / (max - min) : ((Double)val).doubleValue() / max; 
			put(key, new Double(newval * 100));
		}
	}
	
	double stddev(Summator other){
		double sum2 = 0;
		double n = 0;
		double disp = 0;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			double a = value(key).doubleValue();
			double b = other.value(key,0).doubleValue();
			sum2 += a;
			sum2 += b;
			n++;
			double ab = a-b;
			disp += ab*ab;
		}
		if (n == 0)//empty graph
			return 0;
		double stddev = Math.sqrt(disp/n);
		double avg = sum2 / (2 * n);
		return stddev / avg;
	}
	
	public double pearson(Summator other){
		double avga = 0;
		double avgb = 0;
		double n = 0;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			double a = value(key).doubleValue();
			if (other.value(key) == null)//skip misalignments
				continue;
			double b = other.value(key).doubleValue();
			avga += a;
			avgb += b;
			n++;
		}
		if (n == 0)
			return 0;
		avga /= n;
		avgb /= n;
		double ab = 0; 
		double aa = 0; 
		double bb = 0; 
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			double a = value(key).doubleValue();
			if (other.value(key) == null)//skip misalignments
				continue;
			double b = other.value(key).doubleValue();
			double da = a - avga;
			double db = b - avgb;
			ab += da * db;
			aa += da * da;
			bb += db * db;
		}
		return ab/Math.sqrt(aa*bb);
	}
	
	//count average, average-other, accuracy
	public double[] accuracy(Summator other){
		double avga = 0;
		double avgb = 0;
		double matches = 0;
		double n = 0;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			double a = value(key).doubleValue();
			if (other.value(key) == null)//skip misalignments
				continue;
			double b = other.value(key).doubleValue();
			avga += a;
			avgb += b;
			n++;
		}
		if (n == 0)
			return null;
		avga /= n;
		avgb /= n;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			double a = value(key).doubleValue();
			if (other.value(key) == null)//skip misalignments
				continue;
			double b = other.value(key).doubleValue();
			if (a == avga && b == avgb)
				matches++;
			else if (a > avga && b > avgb)
				matches++;
			else if (a < avga && b < avgb)
				matches++;
		}
		return new double[]{n,avga,avgb,matches/n};
	}
	
	/**
	 * @return array of key-value pairs with value as Integer in range 0..100
	 */
	public Object[][] toRanked(){
		ArrayList a = new ArrayList();
		double max = toArrayOfValueCounts(a);
		Object[][] v = (Object[][])a.toArray(new Object[][]{});
		for (int i = 0; i < v.length; i++){
			v[i][1] = new Double( Math.round((((Double)v[i][1]).doubleValue() * 100 / max)) );
		}
		Arrays.sort(v,new ArrayPositionComparator(1));
		return v;
	}

	public double toArrayOfValueCounts(ArrayList a){
		double max = 0;
		for (Iterator it = keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Object val = get(key);
			double f = ((Double)val).doubleValue();
			if (f > 0){
				a.add(new Object[]{key,val});
				if (max < f)
					max = f;
			}
		}
		return max;
	}
	
	public Summator blend(Linker other, double otherfactor, int thisDefault, int otherDefault){
		double thisfactor = 1 - otherfactor;
		HashSet all = new HashSet(keys());
		all.addAll(other.keys());
		for (Iterator it = all.iterator(); it.hasNext();){
			Object key = it.next();
			double thisvalue = value(key,thisDefault).doubleValue();
			double othervalue = other.value(key,otherDefault).doubleValue();
			thisvalue = (thisvalue * thisfactor + othervalue * otherfactor);
			put(key,new Double(thisvalue));
		}
		return this;
	}

	public void load(Environment env, String path){
		if (!AL.empty(path)){
			DataLogger dl = new DataLogger(env, "Summator");
			dl.load(path, new DataLogger.StringConsumer() {					
				public boolean read(String text) {
					if (AL.empty(text))
						return false;
					String[] tokens = text.split("\t");
					if (AL.empty(tokens) || tokens.length < 2)
						return false;
					Double value = new Double(tokens[1]);
					put(tokens[0],value);
					return true;
				}
			});
		}
	}
}
