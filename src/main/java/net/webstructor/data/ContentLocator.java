/*
 * MIT License
 * 
 * Copyright (c) 2018-2020 by Anton Kolonin, Aigents®
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
import java.util.TreeMap;
import java.util.Map.Entry;

import net.webstructor.al.AL;
import net.webstructor.comm.HTTP;

public class ContentLocator {
	HashMap pathMaps = new HashMap();
	public TreeMap getMap(String path){
		TreeMap map = (TreeMap)pathMaps.get(path);
		if (map == null)
			pathMaps.put(path,map = new TreeMap());
		return map;
	}
	
	private Entry getClosestUp(TreeMap map, Integer pos){
		return map != null ? map.floorEntry(pos) : null;
	}

	private Entry getClosest(TreeMap map, Integer pos){
		if (map == null)
			return null;
		//Strategy 1:
		//Entry e = map.floorEntry(pos);
		//return e != null ? e : map.ceilingEntry(pos);
		//Strategy 2:
		Entry e1 = map.floorEntry(pos);
		Entry e2 = map.ceilingEntry(pos);
		if (e1 == null)
			return e2;
		if (e2 == null)
			return e1;
		Integer v1 = (Integer)e1.getKey();
		Integer v2 = (Integer)e2.getKey();
		int d1 = pos.intValue() - v1.intValue();
		int d2 = v2.intValue() - pos.intValue();
		return d1 <= d2 ? e1 : e2;
	}
	
	private Entry getClosest(TreeMap map, Integer pos, int range){
		if (range <= 0)
			range = Integer.MAX_VALUE;
		if (map == null)
			return null;
		Entry e1 = map.floorEntry(pos);
		Entry e2 = map.ceilingEntry(pos);
		if (e1 == null && e2 == null)
			return null;
		if (e2 == null)
			return Math.abs(pos.intValue() - ((Integer)e1.getKey()).intValue()) <= range? e1 : null;
		if (e1 == null)
			return Math.abs(pos.intValue() - ((Integer)e2.getKey()).intValue()) <= range? e2 : null;
		int d1 = Math.abs(pos.intValue() - ((Integer)e1.getKey()).intValue());
		int d2 = Math.abs(pos.intValue() - ((Integer)e2.getKey()).intValue());
		return Math.min(d1, d2) > range ? null : d1 <= d2 ? e1 : e2;	
	}
		
	/**
	 * Returns content source closest to position
	 * @param pos position of the image
	 * @return
	 */
	String get(String path, Integer pos){
		TreeMap map = (TreeMap)pathMaps.get(path);
		Entry e = getClosest(map,pos);
		return e == null ? null : (String)e.getValue();
	}

	/**
	 * Returns image source closest to position, with cleanup of unavailable entries along the way
	 * @param pos position of the image
	 * @return
	 */
	public String getAvailable(String path, Integer pos){
		TreeMap map = (TreeMap)pathMaps.get(path);
		if (map == null)
			return null;
		while (map.size() > 0){
			Entry e = getClosest(map,pos);
			String value = (String)e.getValue();
			if (!AL.empty(value) && HTTP.accessible(value))
				return value;
			map.remove(e.getKey());
		}
		return null;
	}

	/**
	 * Returns contents closest to position with NO cleanup of unavailable entries
	 * @param pos position of the image
	 * @return
	 */
	public String getAvailableUp(String path, Integer pos){
		TreeMap map = (TreeMap)pathMaps.get(path);
		if (map == null)
			return null;
		if (map.size() > 0){
			Entry e = getClosestUp(map,pos);
			if (e != null){
				String value = (String)e.getValue();
				if (!AL.empty(value))
					return value;
			}
		}
		return null;
	}
	
	/**
	 * Returns image source closest to position ONLY if within specified range, with NO cleanup of unavailable entries
	 * @param pos position of the image
	 * @return
	 */
	public String getAvailableInRange(String path, Integer pos, int range){
		TreeMap map = (TreeMap)pathMaps.get(path);
		if (map == null)
			return null;
		if (map.size() > 0){
			Entry e = getClosest(map,pos,range);
			if (e != null){
				String value = (String)e.getValue();
				//TODO: check accessibility unless banned or check robots.txt when trying?
				//if (!AL.empty(value) && HTTP.accessible(value))
				if (!AL.empty(value))
					return value;
			}
		}
		return null;
	}
	
}
