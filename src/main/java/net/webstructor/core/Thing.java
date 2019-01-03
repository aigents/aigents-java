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
package net.webstructor.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Writer;
import net.webstructor.util.Array;

public class Thing extends Anything { // implements ORObject

	private static final String[] names = new String[] {
		AL.is,
		AL.id,
		AL.has,
		AL.name,
		AL.does,
		AL.times,
		AL.sources,
		AL.patterns,
		AL.trust,
		AL._new,
		AL.text // TODO: move this out to news or name?
	};
	
	private HashMap properties = null;
	private Storager storager = null;

	public Thing() {
	}

	public Thing(String name) {
		setString(AL.name,name);
	}

	public Thing(String key, String value) {
		setString(key,value);
	}

	//TODO: shouldn't it be more like copy constructor kinda Thing(thing, null)?
	public Thing(Thing is) {
		addThing(AL.is,is);
	}

	public Thing(Collection is, Thing template, String[] names) {
		this(template,names);
		for (Iterator it = is.iterator(); it.hasNext();)
			addThing(AL.is,(Thing)it.next());
	}
	
	public Thing(Thing template, String[] names) {
		update(template,names);
	}

	public void update(Thing template, String[] names) {
		if (template != null) {
			if (names == null)
				names = template.getNamesAvailable();
			//this is rather heavyweight!!!
			for (int i=0; i<names.length; i++) {
				Object obj = template.get(names[i]);
				if (obj instanceof String || obj instanceof Date) // singular
					set(names[i], obj);
				if (obj instanceof HashSet) {
					Collection things = (Collection)obj;
					for (Iterator it = things.iterator(); it.hasNext();)
						addThing(names[i],(Thing)it.next());
				}
			}
		}
	}

	//copies contents to other thing
	public void copyTo(Thing clone, String[] names,Thing viewer,boolean withTrusts) {
		Set trusts = withTrusts ? (Set)get(AL.trusts) : null;
		for (int i=0; i<names.length; i++) {
			Object obj = get(names[i],viewer);
			if (obj == null)
				//return null;//TODO: can we return incomplete clones?
				continue;
			if (obj instanceof String || obj instanceof Date) // singular
				clone.set(names[i], obj);
			if (obj instanceof Collection) {
				Collection things = (Collection)obj;
				for (Iterator it = things.iterator(); it.hasNext();){
					Thing thing = (Thing)it.next();
					clone.addThing(names[i],thing);
					if (trusts != null && trusts.contains(thing))
						clone.addThing(AL.trusts,thing);
				}
			}
		}
	}

	//returns copy of the thing only if all names are provided
	public Thing clone(String[] names,Thing viewer) {
		Thing clone = new Thing();
		copyTo(clone,names,viewer,false);
		return clone;
	}

	public String getTitle(String names[]) {
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < names.length; i++) {
    		String v = getString(names[i]);
    		if (!AL.empty(v)) {
    			if (sb.length() > 0)
    				sb.append(' ');
    			sb.append(Writer.capitalize(v));
     		}
    	}
		return sb.toString();
	}
	
	public String getName() {
		//TODO: ensure it is unique
		return getString(AL.name);
	}
/**
	public int hashCode() {
		String name = getName();
		return name != null ? name.hashCode() : super.hashCode();
	}
	
	//http://docs.oracle.com/javase/7/docs/api/java/util/HashSet.html
	//remove(Object o) removes an element e such that (o==null ? e==null : o.equals(e))
	//so that equals mean this have everything it has equal to other but not the opposite
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Thing))
			return false;
		String[] names = this.getNamesAvailable();
		for (int i=0; i<names.length; i++) {
			String value = getString(names[i]);
			if (value != null) { //consider only strings
				String others = ((Thing)other).getString(names[i]);
				if (!value.equals(others))
					return false;
			}
		}
		return true;
	}
**/
	//to be called by storager to cleanup
	public boolean del() {
		if (storager != null)
			if (!storager.del(this))
				return false;
		if (properties != null) {
			String[] names = this.getNamesAvailable();
			for (int i=0; i<names.length; i++) {
				Object obj = get(names[i]);
				if (obj instanceof HashSet)
					((HashSet)obj).clear();
			}
			properties.clear();
		}
		return true;
	}
	
	public Anything addThing(String name, Thing value) {
		//TODO: ensure storage constraint?
		//if (storager != value.storager)
		//	throw new Exception(" not stored "+value.toString());
		if (properties == null)
			properties = new HashMap();
		HashSet set = (HashSet)properties.get(name);
		if (set == null)
			properties.put(name, set = new HashSet());
		if (!set.contains(value)) {
			set.add(value);
			if (storager != null)
				storager.put(name, value, this);//update storager index
		}
		return this; 
	}

	public boolean hasThing(String name, Thing value) {
		if (properties != null) {
			HashSet set = (HashSet)properties.get(name);
			if (set != null)
				return set.contains(value);
		}
		return false;
	}
	
	public Anything delThings(String name) {
		Collection things = getThings(name);
		if (!AL.empty(things))
			for (Iterator it = new ArrayList(things).iterator(); it.hasNext();)
				delThing(name,(Thing)it.next());
		return this;
	}
	
	public Anything delThing(String name, Thing value) {
		//TODO: ensure storage constraint?
		//if (storager != value.storager)
		//	throw new Exception(" not stored "+value.toString());
		if (properties != null) {
			HashSet set = (HashSet)properties.get(name);
			if (set != null) {
				//boolean removed = 
				set.remove(value);
				if (set.size() == 0)
					properties.remove(name);
			}
		}
		if (storager != null)
			storager.del(name, value, this);//update storager index
		return this; 
	}
	
	//actual properties that thing possess
    public String[] getNamesAvailable() {
    	if (properties == null) 
    		return new String[]{};
    	String[] names = (String[])properties.keySet().toArray(new String[]{});
    	Arrays.sort(names);//TODO:avoid doing to save performance?
    	return names;
    }

    /*
    public String[] getNamesPossible1() {
    	Object has = get(AL.has);
    	if (has instanceof Collection) {
    		Collection coll = (Collection)has;
    		String[] has_names = new String[coll.size()];
    		int i = 0;
    		for (Iterator it = coll.iterator();it.hasNext();)
    			has_names[i++] = ((Thing)it.next()).getName();
    		return Array.union(new String[][]{getNamesAvailable(), names, has_names});
    	}
    	else
    		return Array.union(getNamesAvailable(), names);
    }
    */

    public String[] getNamesPossible() {
    	ArrayList has_names = new ArrayList();
       	Object has = get(AL.has);
    	if (has instanceof Collection) {
    		Collection coll = (Collection)has;
      		for (Iterator it = coll.iterator();it.hasNext();)
    			has_names.add(((Thing)it.next()).getName());
    	}
       	Object is = get(AL.is);
    	if (is instanceof Collection) {
    		Collection coll = (Collection)is;
      		for (Iterator it = coll.iterator();it.hasNext();) {
      			Object ishas = ((Thing)it.next()).get(AL.has);
      	    	if (ishas instanceof Collection) {
      	    		Collection iscoll = (Collection)ishas;
      	      		for (Iterator isit = iscoll.iterator();isit.hasNext();)
      	    			has_names.add(((Thing)isit.next()).getName());
      	    	}
      		}
    	}
    	if (has_names.size() > 0) 
    		return Array.union(new String[][]{getNamesAvailable(), names, (String[])has_names.toArray(new String[]{})});
    	else
    		return Array.union(getNamesAvailable(), names);
    }
    
    
    //String or Thing or Collection of Strings or Things
	public Object get(String name) {
		return get(name, null);
	}
	
	public Object get(String name, Thing viewer) {
		String reverse = Schema.reverse(name);
		if (reverse != null) {
			if (storager != null && viewer != null && viewer.storager != null) {
				try {
					java.util.Set reverses = storager.get(reverse,this);
					return reverses != null && reverses.contains(viewer)? AL._true : AL._false;
				} catch (Exception e) {
					;//TODO: what?
				}
			}
			//TODO: leave the way to set it just for shallow cloning purposes?
		}
		Object obj = properties == null? null: properties.get(name);
		return obj != null ? obj : storager != null ? storager.get(name) : null;
	}
	
	public final String getString(String name) {
		Object o = get(name);
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		return null;//o.toString();//TODO:what?
	}

	public final Object getFirst(String name) {
		Collection iss = getThings(name);
		return AL.empty(iss) ? null : (Thing)iss.iterator().next(); 
	}
	
	public final Collection getThings(String name) {
		Object o = get(name);
		if (o != null && o instanceof Collection)
			return (Collection)o;
		return null;
	}

	public final Anything set(String name,Object value) {
		return set(name, value, null);
	}
	
	public final Anything set(String name,Object value,Thing setter) {
		String reverse = Schema.reverse(name);
		if (reverse != null) {
			if (storager != null && setter != null && setter.storager != null) {
				if (value.equals(AL._true))
					setter.addThing(reverse,this);
				else
					setter.delThing(reverse,this);
				return this;
			}
			//TODO: leave the way to set it just for shallow cloning purposes?
		}
		Object old;		
		if (properties == null) {
			properties = new HashMap();
			old = null;
		} else {
			old = properties.get(name);
		}
		if (old != null) {
			if (old.equals(value))//don't store same twice
				return this;
			if (storager != null)
				storager.del(name, old, this);//update storager index
			properties.remove(name);//TODO:remove after, not before - so to be found in map!?
		}
		if (value != null) {
			properties.put(name, value);
			if (storager != null)
				storager.put(name, value, this);//update storager index
		}
		return this;
	}
	
	//TODO: get rid of this?
	public final Anything setString(String name,String value) {
		return set(name,value);
	}

	protected final boolean stored() {
		return this.storager != null;
	}

	//TODO: remove one of the two implementations!
	//TODO: this one does not "store" binary recipprocal links such as new and true!!! 					
	public final Thing store(Storager storager) {
		if (this.storager != null) 
			return null;//TODO: throw?
		storager.put(this);
		this.storager = storager;
		return this;
	}

	//TODO:if this is too smart to be used?
	public final Thing storeNew(Storager storager) {
		if (this.storager != null) 
			return null;//TODO: throw?
		String[] names = getNamesAvailable();
		HashMap things = new HashMap();
		for (int i=0;i<names.length;i++) {
			String name = (String)names[i];
			Object value = get(name);
			if (value != null) {
				if (value instanceof Date || value instanceof String){
					if (storager.isThing(name) && value instanceof String){
						properties.remove(name);
						if (!AL.empty((String)value))
							things.put(name, value);
					}else
						storager.put(name, value, this);
				}else
				if (value instanceof Collection) {
					for (Iterator it = ((Collection)value).iterator();it.hasNext();){
						Object item = it.next();
						storager.put(name,item,this);
					}
				}
			}
		}
		this.storager = storager;
		for (Iterator it = things.keySet().iterator(); it.hasNext();) {
			String name = (String)it.next();
			Object value = things.get(name);
			Thing thing = storager.getThing((String)value);
			addThing(name,thing);
		}
		return this;
	}

	public String toString() {
		return Writer.toString(this);
	}
	
	public static String[] toStrings(Collection collection, String name) {
		ArrayList objs = new ArrayList();
		for (Iterator it = collection.iterator(); it.hasNext();){
			Object o = it.next();
			if (o instanceof Thing){
				String value = ((Thing)o).getString(name);
				if (!AL.empty(value))
					objs.add(value);
			}
		}
		return (String[])objs.toArray(new String[]{});
	}
}
