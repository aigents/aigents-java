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
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.StringUtil;
import net.webstructor.util.Array;

public class Thing extends Anything implements Named { // implements ORObject

	private static final String[] names = new String[] {//TODO have this in Schema, not here and in AL
		AL.is,
		AL.id,
		AL.has,
		AL.name,
		AL.does,
		AL.times,
		AL.sources,
		AL.patterns,
		AL.responses,//TODO make is so thing does not need all imaginable properties declared explicitly
		AL.trust,
		AL.query,//TODO: move this out to configurable schema and/or class properties with class specified in query!?
		AL.click,//TODO: move all of the following out to news or name?
		AL.selection,
		AL.copypaste,
		AL._new,
		AL.text
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
			if (obj instanceof String || obj instanceof Date || obj instanceof Number) // singular
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

	@Override
	public String name() {
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

	private void delete(){
		//cascade delete
		Collection extensions = null;
		try { extensions = storager.getByName(AL.is, this);} catch (Exception e) {}//getThings(AL.is);
		if (!AL.empty(extensions)) for (Iterator it = extensions.iterator();it.hasNext();)
			((Thing)it.next()).delete();
		if (storager != null)
			storager.delete(this);
		if (properties != null) {
			String[] names = this.getNamesAvailable();
			for (int i=0; i<names.length; i++) {
				Object obj = get(names[i]);
				if (obj instanceof HashSet)
					((HashSet)obj).clear();
			}
			properties.clear();
		}
	}

	//TODO: build "deletion plan" first and delete accordingly to the plan next 
	private boolean deleteable(Thing except) {
		//cascade delete check
		Collection extensions = null;
		try { extensions = storager.getByName(AL.is, this);} catch (Exception e) {}//getThings(AL.is);
		if (!AL.empty(extensions)) for (Iterator it = extensions.iterator();it.hasNext();){
			Thing t = (Thing)it.next();
			if (!t.deleteable(this))//let them point back to this
				return false;
		}
		if (storager != null && !storager.deleteable(this,extensions))
			return false;
		return true;
	}
	
	//to be called by storager to cleanup
	public boolean del() {
		if (deleteable(null)){//cascade
			delete();//cascade
			return true;
		}
		return false;
		/*if (storager != null){
			if (!storager.deleteable(this,null))
				return false;
			storager.delete(this);
		}
		if (properties != null) {
			String[] names = this.getNamesAvailable();
			for (int i=0; i<names.length; i++) {
				Object obj = get(names[i]);
				if (obj instanceof HashSet)
					((HashSet)obj).clear();
			}
			properties.clear();
		}
		return true;*/
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

	public boolean is(String[] exceptions) {
		if (AL.empty(exceptions))
			return false;
		if (Array.contains(exceptions,name()))
			return true;
		Collection is = (Collection)get(AL.is);
		if (is !=  null)
			for (Iterator it = is.iterator(); it.hasNext();)	
				if (Array.contains(exceptions, ((Thing)it.next()).name()))
					return true;
		return false;
	}
	
	public boolean hasAny(String name, Collection things) {
		if (AL.empty(things))
			return false;
		if (!AL.empty(things) && properties != null) {
			HashSet set = (HashSet)properties.get(name);
			if (set != null) for (Iterator it = things.iterator(); it.hasNext();)
				if (set.contains(it.next()))
					return true;
		}
		return false;
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

    public boolean empty() {
    	return properties == null || properties.isEmpty();
    }
	
	//actual properties that thing possess
    public String[] getNamesAvailable() {
    	if (properties == null) 
    		return new String[]{};
    	String[] names = (String[])properties.keySet().toArray(new String[]{});
    	Arrays.sort(names);//TODO:avoid doing to save performance?
    	return names;
    }

    //TODO: use this instead of the earlier one
    public java.util.Set<String> getNamesPossible(java.util.Set<String> set) {
       	Object has = get(AL.has);
    	if (has instanceof Collection) {
    		Collection properties = (Collection)has;
      		for (Object p : properties)
    			set.add(((Thing)p).name());
    	}
       	Object is = get(AL.is);
    	if (is instanceof Collection) {
    		Collection classes = (Collection)is;
      		for (Object c : classes)
      			((Thing)c).getNamesPossible(set);
    	}
    	for (String n : names)//TODO: eliminate this hack, do this my means of Schema and inheritance
    		set.add(n);
    	return set;
    }
    
    public String[] getNamesPossible() {
    	ArrayList has_names = new ArrayList();
       	Object has = get(AL.has);
    	if (has instanceof Collection) {
    		Collection coll = (Collection)has;
      		for (Iterator it = coll.iterator();it.hasNext();)
    			has_names.add(((Thing)it.next()).name());
    	}
       	Object is = get(AL.is);
    	if (is instanceof Collection) {
    		Collection coll = (Collection)is;
      		for (Iterator it = coll.iterator();it.hasNext();) {
      			Object ishas = ((Thing)it.next()).get(AL.has);
      	    	if (ishas instanceof Collection) {
      	    		Collection iscoll = (Collection)ishas;
      	      		for (Iterator isit = iscoll.iterator();isit.hasNext();)
      	    			has_names.add(((Thing)isit.next()).name());
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
	
	public final boolean getBoolean(String name) {
		String str = getString(name);
		return AL._true.equals(str);
	}
	
	public final Date getDate(String name,Date def) {
		Object o = get(name);
		if (o instanceof Date)
			return (Date)o;
		if (o instanceof String)
			return Time.date((String)o);
		return def == null ? new Date(0) : def;//TODO should null return null?
	}
	
	public final int getInt(String name,int def) {
		Object o = get(name);
		if (o instanceof Number)
			return ((Number)o).intValue();
		if (o instanceof String)
			return StringUtil.toIntOrDefault((String)o,10,def);
		return def;
	}
	
	public final String getString(String name) {
		Object o = get(name);
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		if (o instanceof java.util.Set){
			StringBuilder sb = new StringBuilder();
			for (Iterator it = ((java.util.Set) o).iterator(); it.hasNext();){
				Object e = it.next();
				if (sb.length()>0)
					sb.append(", ");//TODO: unhack the hack!?
				sb.append(e instanceof Thing ? ((Thing)e).name() : e.toString());
			}
			return sb.toString();
		}
		return o.toString();
	}

	public Collection getCollection(String name) {
		Object o = get(name);
		if (o == null || o instanceof Collection)
			return (Collection)o;
		Collection c = new ArrayList(1);
		c.add(o);
		return c;
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

	public final Collection getThingsClone(String name) {
		Object o = get(name);
		if (o != null && o instanceof Collection) synchronized (o) {
			return new ArrayList((Collection)o);
		}
		return null;
	}

	public final Anything set(String name,Object value) {
		return set(name, value, null);
	}
	
	//TODO: make type conversion more consistent!?
	public static Object cast(String name,Object value) {
		Class cls = Schema.cls(name);
		if (value != null && cls != value.getClass() && cls == Date.class && value instanceof String){
			Date date = Time.date((String)value);
			if (date != null)
				value = date;
		}
		return value;
	}
	
	public final Anything set(String name,Object value,Thing setter) {
		//TODO: make type conversion more consistent!?
		/*Class cls = Schema.cls(name);
		if (value != null && cls != value.getClass() && cls == Date.class && value instanceof String){
			Date date = Time.date((String)value);
			if (date != null)
				value = date;
		}*/
		value = cast(name,value);
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
