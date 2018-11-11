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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.webstructor.al.AL;

public class MapMap {
	
	//static final boolean debug = false;
	
	private HashMap maps = new HashMap();

	private HashMap getMap(Object key,boolean lazyAdd) {
		HashMap map;
		synchronized (maps) {
			map = (HashMap) maps.get(key);
			if (map == null && lazyAdd) {
				map = new HashMap();
				maps.put(key, map);
			}
		}
		return map;
	}

	private void println(String string) {
		System.out.println(string);
	}
	
	public void clear() {
		synchronized (maps) {
			maps.clear();
		}
	}
	
	public String[] getKeyStrings() {
		return (String[])maps.keySet().toArray(new String[]{});
	}
	
	public Object[] getKeyObjects() {
		return maps.keySet().toArray();
	}
	
	public String[] getSubKeyStrings(String key) {
		HashMap map = getMap(key,false);
		if (map != null) {
			Collection values;
			synchronized (map) {
				values = map.keySet();
			}
			return (String[])values.toArray(new String[]{});
		}
		return null;
	}
	
	public Object[] getSubKeyObjects(Object key) {
		HashMap map = getMap(key,false);
		if (map != null) {
			Collection values;
			synchronized (map) {
				values = map.keySet();
			}
			return values.toArray(new Object[]{});
		}
		return null;
	}
	
	public Object getObject(Object key1,Object key2,boolean lazyAdd) {
		HashMap map = getMap(key1,lazyAdd);
		if (map != null) 
			synchronized (map) {
				Object o = map.get(key2);
				return o;
			}		
		return null;
	}

	public Collection getObjects(Object key1,Object key2) {
		Object objects = getObject(key1,key2,false);
		return objects != null && objects instanceof Collection ? (Collection)objects : null;
	}

	public void putObjects(Object key1,Object key2,Object obj) {
		Object objects = getObject(key1,key2,true);
		java.util.Set set = null;
		if (objects != null && objects instanceof Set)
			set = (java.util.Set)objects;
		else
			putObject(key1,key2,set = new HashSet());
		set.add(obj);
	}
	
	//get collection of subkey values for all subkeys included or excluded by rule
	public Collection getObjects(Object key1,Object[] options,boolean rule) {
		ArrayList list = new ArrayList();
		HashMap map = getMap(key1,false);
		if (map != null) { 
			Collection key2s;
			synchronized (map) {
				key2s = map.keySet();
			}
			if (AL.empty(options)) { //if no options/exceptions
				if (!rule) // if negative rule, include all 
					for (Iterator it = key2s.iterator(); it.hasNext();)
						list.add(map.get(it.next()));
			} else { // there are options/exceptions to consider
				for (Iterator it = key2s.iterator(); it.hasNext();) {
					Object key2 = it.next();
					if (( rule &&  Array.contains(options, key2)) || //include all matches
						(!rule && !Array.contains(options, key2))  ) //include all non-matches
						list.add(map.get(key2));
				}						
			}
		}
		return list;
	}

	public void putObject(Object key1,Object key2,Object obj) {
		if (key1 == null || key2 == null || obj == null) //TODO:Exception
			System.out.println("Error: putObject null");
		HashMap map = getMap(key1,true);
		synchronized (map) {
			//TODO: assert previous value is the same as new value and throw exception otherwise
			map.put(key2, obj);
		}		
	}

	public boolean delKey(Object key1,Object key2) {
		HashMap map = getMap(key1,false);
		if (map != null) 
			synchronized (map) {
				map.remove(key2);
				if (map.size() == 0)
					maps.remove(key1);
				return true;
			}		
		return false;
	}

	//TODO:never use!
	//careful - may be very slow
	public Collection getObjects(Class cls,Set debugTrap) {
		if (debugTrap == null) {
			HashSet objects = new HashSet();
			synchronized (maps) {
				Iterator it = maps.values().iterator();
				while (it.hasNext()) {
					HashMap map = (HashMap)it.next();
					Iterator i = map.values().iterator();
					while (i.hasNext()) {
						Object obj = i.next();
						//TODO:makes no sense as obj is a Set?
						if (cls == null || cls.isInstance(obj))
							objects.add(obj);
					}
				}
			} //synchronized (maps)
			return objects;
		}
		else { //debug
			HashSet objects = new HashSet();
			synchronized (maps) {
				Iterator it = maps.keySet().iterator();
				while (it.hasNext()) {
					Object key1 = it.next();
					HashMap map = (HashMap)maps.get(key1);
					Iterator i = map.keySet().iterator();
					while (i.hasNext()) {
						Object key2 = i.next();
						Object obj = map.get(key2);
						HashSet targets = new HashSet((Set)obj);
						for (Iterator t = targets.iterator(); t.hasNext();){
							Object target = t.next();
							if (debugTrap.contains(target)) 
								println("Checking: "+key1+":"+key2+":"+target);
						}
						//TODO:makes no sense as obj is a Set?
						if (obj != null && (cls == null || cls.isInstance(obj)))
							objects.add(obj);
					}
				}
			}
			return objects;
		}
	}

	/**/
	public int getKey2Count(Object target) {
		int count = 0;
		synchronized (maps) {
			Iterator it = maps.values().iterator();
			while (it.hasNext()) {
				HashMap map = (HashMap)it.next();
				Iterator i = map.keySet().iterator();
				while (i.hasNext()) {
					Object obj = i.next();
					//TODO:equals
					if (target == null || target == obj) {
						HashSet set = (HashSet)map.get(obj);
						count += set.size();
					}
				}
			}
		} //synchronized (maps)
		return count;
	}
	/*
	public int getKey2Count(Object target) {//TODO: purge debug!!!
		int count = 0;
		Iterator it = maps.keySet().iterator();
		while (it.hasNext()) {
			String name = (String)it.next();
			HashMap map = (HashMap)maps.get(name);
			Iterator i = map.keySet().iterator();
			while (i.hasNext()) {
				Object obj = i.next();
				//TODO:equals
				if (target == null || target == obj) {
					HashSet set = (HashSet)map.get(obj);
					count += set.size();
if (set.size() > 0)
System.out.println("Link:"
		+net.webstructor.al.Writer.toString(((net.webstructor.core.Thing)set.iterator().next()))					
		+" "+name+" "
		+net.webstructor.al.Writer.toString(((net.webstructor.core.Thing)obj)));					
				}
			}
		}
		System.out.println("Count:"+count);		
		return count;
	}
	*/
	
	public Map getKey2Counts(Class cls) {
		HashMap counts = new HashMap();
		synchronized (maps) {
			Iterator it = maps.values().iterator();
			while (it.hasNext()) {
				HashMap map = (HashMap)it.next();
				Iterator i = map.keySet().iterator();
				while (i.hasNext()) {
					Object obj = i.next();
					if (cls == null || cls.isInstance(obj)) {
						HashSet set = (HashSet)map.get(obj);
						if (set == null)
							System.out.println("Error: "+obj+" set null");//TODO:Exception
						int addon = set == null ? 0 : set.size();
						Integer count = (Integer)counts.get(obj);
						count = new Integer((count == null ? 0 : count.intValue()) + addon);
						counts.put(obj,count);
					}
				}
			}
		}
		return counts;
	}
/*
	//from PREDICATE-OBJECT-SUBJECT triplet, return all OBJECT instances of given class, 
	//or all of them (if class cls is null)
	public Collection getValues(Class cls) {
		HashSet values = new HashSet();
		Iterator it = maps.values().iterator();
		while (it.hasNext()) {
			HashMap map = (HashMap)it.next();
			Iterator i = map.keySet().iterator();
			while (i.hasNext()) {
				Object obj = i.next();
				if (cls == null || cls.isInstance(obj))
					values.add(obj);
			}
		}
		return values;
	}
*/	

	//transposes VERB-OBJECT-SUBJECT index into OBJECT-SUBJECT-VERB 
	//transposes PROPERTY-VALUE-OBJECT index into VALUE-OBJECT-PROPERTY
	public MapMap getShifted() {
		MapMap shifted = new MapMap();
		synchronized (maps) {
			for (Iterator ip = maps.keySet().iterator(); ip.hasNext();) {
				String propertyName = (String)ip.next();
				HashMap map = (HashMap)maps.get(propertyName);
				for (Iterator iv = map.keySet().iterator(); iv.hasNext();) {
					Object valueObject = iv.next();
					HashSet set = (HashSet)map.get(valueObject);
					for (Iterator it = set.iterator(); it.hasNext();) {
						//TODO: move out this lind of logic out of here to Storager level?
						Object owner = it.next();
						HashSet newset = (HashSet) shifted.getObject(valueObject, owner, true);
						if (newset == null)
							shifted.putObject(valueObject, owner, newset = new HashSet());
						newset.add(propertyName);
					}
				}
			}		
		}//synchronized (maps)
		return shifted;
	}
	
}
