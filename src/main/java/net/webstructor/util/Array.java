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
package net.webstructor.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.al.AL;
import net.webstructor.al.Set;

class LengthComparator implements Comparator {
	boolean order = true;
	public LengthComparator(boolean order) {
		this.order = order;
	}
	public int compare(Object o1, Object o2) {
		int res = 0;
		if (o1 instanceof String && o2 instanceof String)
			res = ((String)o1).length() - ((String)o2).length();
		if (o1 instanceof Set && o2 instanceof Set)
			res = ((Set)o1).size() - ((Set)o2).size();
		if (!order);
			res = -res;
		return res;
	}
	public boolean equals(Object obj) {
		return obj instanceof LengthComparator && order == ((LengthComparator)obj).order;
	}
}

public class Array {

	//TODO:purge as not used
	public static String[] merge(String[] left,String[] right) {
		String[] res = new String[(left != null ? left.length : 0) + (right != null ? right.length : 0)];
		int k = 0;
		for (int i=0;i<left.length;i++)
			res[k++] = left[i];
		for (int i=0;i<right.length;i++)
			res[k++] = right[i];		
		return res; 
	}

	public static String toString(String[] args) {
		StringBuilder sb = new StringBuilder();
		if (args != null)
			for (int i=0; i<args.length; i++) {
				if (i>0)
					sb.append(AL.space);
				//Writer.toString(sb,args[i]);
				sb.append(args[i]);
			}
		return sb.toString();
	}
	
	public static HashSet toSet(HashSet set,Object[] array) {
		if (set != null && array != null)
			for (int i=0; i<array.length; i++)
				set.add(array[i]);
		return set;
	}
	
	public static HashSet toSet(Object[] array) {
		return toSet(new HashSet(), array);
	}
	
	public static String[] union(String[] left,String[] right) {
		HashSet set = new HashSet();
		toSet(set,left);
		toSet(set,right);
		return (String[])set.toArray(new String[]{}); 
	}
	
	public static String[] union(String[][] all) {
		HashSet set = new HashSet();
		if (all != null)
			for (int i=0;i<all.length;i++)
				if (all[i] != null)
					toSet(set,all[i]);
		return (String[])set.toArray(new String[]{}); 
	}
	
	public static String[] sub(String[] left,String[] right) {
		ArrayList a = new ArrayList();
		for (int l = 0; l < left.length; l++) {
			int r = 0;
			for (; r < right.length; r++)
				if (left[l].equalsIgnoreCase(right[r]))
					break;
			if (r == right.length)
				a.add(left[l]);
		}
		return (String[])a.toArray(new String[]{});
	}

	public static boolean contains(String[] samples,String string) {
		if (samples != null && string != null)
			for (int i=0; i<samples.length; i++)
				if (samples[i].equals(string))
					return true;
		return false;
	}

	public static boolean containsIgnoreCase(String[] samples,String string) {
		if (samples != null && string != null)
			for (int i=0; i<samples.length; i++)
				if (samples[i].equalsIgnoreCase(string))
					return true;
		return false;
	}

	public static boolean contains(String[][] samples,String string) {
		if (samples != null && string != null)
			for (int i=0; i<samples.length; i++)
				if (contains(samples[i],string))
					return true;
		return false;
	}

	public static boolean contains(String sample,String chars) {
		if (sample != null && chars != null)
			for (int i=0; i<chars.length(); i++)
				if (sample.indexOf(chars.charAt(i)) != -1)
					return true;
		return false;
	}

	public static int index(String[] samples,String string) {
		return index(samples,string,0);
	}
	
	public static int index(String[] samples,String string,int pos) {
		if (samples != null && string != null)
			for (int i=pos; i<samples.length; i++)
				if (samples[i].equals(string))
					return i;
		return -1;
	}
	
	public static int indexOfCase(String string, int from, int to, String pattern) {
		pattern = pattern.toLowerCase();
		if (AL.empty(string) || AL.empty(pattern))
			return -1;
		int patternMax = pattern.length();
		int stringMax = to - patternMax;
		for (int i = from; i < stringMax; i++) {
			for (int j = 0; j < patternMax; j++)
				if (Character.toLowerCase(string.charAt(i+j)) != pattern.charAt(j))
					break;
				else
					if (j == (patternMax - 1))
						return i;
		}
		return -1;
	}
	
	public static boolean containsOnly(String string,String chars) {
		if (AL.empty(string))
			return true;
		if (AL.empty(chars))
			return false;
		int count = 0;
		for (int i=0; i<string.length(); i++)
			if (chars.indexOf(string.charAt(i)) != -1)
				count++;
		return count == string.length();
	}
	
	public static int indexOf(String string,char[] samples) {
		int index = -1;
		if (samples != null && string != null)
			for (int i=0; i<samples.length; i++){
				int p = string.indexOf(samples[i]);
				if (p != -1 && (p < index || index == -1))
					index = p;
			}
		return index;
	}
	
	public static int indexOf(String string, int pos, char[] samples) {
		if (samples != null && string != null)
			for (int i = pos; i < string.length(); i++)
				for (int j = 0; j < samples.length; j++)
					if (string.charAt(i) == samples[j])
						return i;
		return -1;
	}
	
	public static int indexOf(String string, int pos, String sample, int till) {
		if (sample != null && string != null)
			for (int i = pos, count = 0; count < till && i < string.length(); i++, count++)
				if (startsWith(string,i,sample))
					return i;
		return -1;
	}
	
	public static int[] indexOf(String string, int pos, String[] samples, int till) {
		if (samples != null && string != null)
			for (int i = pos, count = 0; count < till && i < string.length(); i++, count++)
				for (int j = 0; j < samples.length; j++) {
					if (startsWith(string,i,samples[j]))
						return new int[]{i,j};
				}
		return null;
	}
	
	public static boolean startsWith(String string,int from,String sample) {
		int j = 0;
		for (int i = from; i < string.length() && j < sample.length(); i++, j++)
			if (string.charAt(i) != sample.charAt(j))
				return false;
		return j == sample.length();
	}
	
	public static String startsWith(String string,String[] samples) {
		for (int i=0; i<samples.length; i++)
			if (string.startsWith(samples[i])) 
				return samples[i];
		return null;
	}

	public static int startsWithAfter(String string,String samples,String spaces) {
		if (string != null && !string.isEmpty()){
			int len = string.length();
			for (int pos = 0; pos < len; pos++){
				char ch = string.charAt(pos);
				if (samples.indexOf(ch) != -1)//matched sample
					return pos;
				if (spaces.indexOf(ch) == -1)//not matched white space
					break;
			}
		}
		return -1;
	}
	
	public static String startingWord(String string,String[] samples) {
		for (int i=0; i<samples.length; i++)
			if (samples[i].startsWith(string) && 
				(samples[i].length() == string.length() || samples[i].charAt(string.length()) == ' '))
				return samples[i];
		return null;
	}

	public static String prefix(String[] samples,String string) {
		if (string == null)
			return null;
		for (int i=0; i<samples.length; i++)
			if (string.startsWith(samples[i]))
				return samples[i];
		return null;
	}

	public static String suffix(String[] samples,String string) {
		for (int i=0; i<samples.length; i++)
			if (string.endsWith(samples[i]))
				return samples[i];
		return null;
	}
	
	public static String containsAnyAsSubstring(String[] samples,String string) {
		string = string.toLowerCase();
		for (int i=0; i<samples.length; i++)
			if (string.indexOf(samples[i]) != -1)
				return samples[i];
		return null;
	}

	public static void sortByLength(Object[] strings, boolean order) {
		Arrays.sort(strings,new LengthComparator(false));
	}

	public static String[] sortedByLength(String[] strings, boolean order) {
		String[] clone = new String[strings.length];
		for (int i = 0; i < strings.length; i++)
			clone[i] = strings[i].toLowerCase();
		Arrays.sort(clone,new LengthComparator(false));
		return clone;
	}
	
	/**
	 * Returns true if right is contained in left, false otherwise
	 */
	public static boolean contains(Object[] left, Object right) {
		for (int i = 0; i < left.length; i++)
			if (right.equals(left[i]))
				return true;
		return false;
	}
	
	/**
	 * Returns true if any in right are contained in left, false otherwise
	 */
	public static boolean contains(Object[] left, Object[] right) {
		for (int i = 0; i < right.length; i++)
			if (!contains(left,right[i]))
				return false;
		return true;
	}
	
	//TODO:refactor to use Collecton.contains
	public static boolean overlap(Collection left, Object[] right) {
		for (Iterator it = left.iterator(); it.hasNext();) {
			Object obj = it.next();
			for (int i = 0; i < right.length; i++)
				if (obj.equals(right[i]))
					return true;
		}
		return false;
	}

	/**
	 * Checks if two collections have any overlap
	 * @param left
	 * @param right
	 * @return
	 */
	public static boolean overlap(Collection left, Collection right) {
		if (AL.empty(left) || AL.empty(right))
			return false;
		for (Iterator itl = left.iterator(); itl.hasNext();) {
			if (right.contains(itl.next()))
				return true;
		}
		return false;
	}
	
	public static boolean contains(Collection left, Collection right) {
		int finds = 0; 
		if (left != null && right != null)			
			for (Iterator itl = left.iterator(); itl.hasNext();) {
				Object obj = itl.next(); 
				for (Iterator itr = right.iterator(); itr.hasNext();)
					if (obj.equals(itr.next()))
						if (++finds == right.size())
							return true;
						else
							break;
			}
		return false;
	}
	
	public static String[] toLower(String[] strings) {
		if (strings == null)
			return strings;
		String[] lower = new String[strings.length];
		for (int i = 0; i < strings.length; i++)
			lower[i] = strings[i].toLowerCase();
		return lower;		
	}
	
	public static String firstdiff(String oldtext, String newtext) {
		int oldlen = oldtext.length();
		int newlen = newtext.length();
	    for (int i = 0; i < oldlen && i < newlen; i++) {
	    	if (oldtext.charAt(i) != newtext.charAt(i))
	    		return newtext.substring(i);
	    }
	    if (oldlen > newlen)
	    	return oldtext.substring(newlen);
	    if (newlen > oldlen)
	    	return newtext.substring(oldlen);
	    return null;
	}
		
	public static String replace(String str, String[] what, String to[]) {
		for (int i=0; i<what.length; i++)
			str = str.replace(what[i], to[i]);
		return str;
	}

	public static String replace(String str, String[] what, char to[]) {
		for (int i=0; i<what.length; i++)
			str = str.replace(what[i], String.valueOf(to[i]));
		return str;
	}

    public static String parseBetween(String source, int from, String pre, String post) {
    	if (source != null) {
    		int beg = source.indexOf(pre,from);
    		if (beg != -1){
    			beg += pre.length();
    			if (post == null)
    				return source.substring(beg);
    			int end = source.indexOf(post, beg);
    			if (end != -1) {
    				return source.substring(beg,end);
    			}
    		}
    	}
    	return null;
    }
    
	/*
	 * Removes one (first) "data" element (physical match) from "values" array and returns new array
	 * or returns original array if element not found.   
	 */
	public static Object[][] remove(Object[][] values, Object data[]) {
		if (values != null) {
			for (int i = 0; i < values.length; i++)
				if (values[i] == data) {
					Object[][] temp = new Object[values.length - 1][];
					//copy everything before i
					for (int j = 0; j < i; j++)
						temp[j] = values[j];
					//copy everything after i
					for (int k = i + 1; k < values.length; k++)
						temp[k-1] = values[k];
					return temp;
				}
			return values;
		}
		return null;
	}

	public static boolean intersect(java.util.Set s1, java.util.Set s2) {
		for (Iterator i = s1.iterator(); i.hasNext();)
			if (s2.contains(i.next()))
				return true;
		return false;
	}
	
	public static long min(long[] mins){
		long min = 0;
		if (mins != null)
			for (int i = 0; i < mins.length; i++)
				if (min == 0 || min > mins[i])
					min = mins[i];
		return min;
	}

}
