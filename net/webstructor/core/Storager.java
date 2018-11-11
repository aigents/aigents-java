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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Any;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;
import net.webstructor.util.MapMap;

public class Storager {

	public static final String things_count = "things count";
	private final static String[] multiples = new String[]{
		AL.is, 
		AL.has, 
		AL.knows,
		AL.sites,
		AL.areas,
		AL.trusts,
		AL.ignores,
		AL.shares,
		AL.friends,
		AL.news,
		AL.things,
		AL.patterns,
		AL.sources
	}; 
	
	private Environment env;
	private MapMap mapmap = new MapMap();

	public Storager(Environment env){
		this.env = env;
	}
	
	private long update_time = 0;
	public long getUpdate() {
		return update_time;
	}
	public void setUpdate() {
		update_time = System.currentTimeMillis();
	}

    //TODO: make Storager implementor of Anything?
	public Object get(String name) {
		if (name.equals(things_count))
			return String.valueOf(getThings().size());
		else
		if (name.equals(AL.things))
			return getThings();
		return null;
	} 
	
	protected Thing put(String name,Object value,Thing thing) {
		setUpdate();
		if (isTime(name) && value instanceof String)
			value = Time.day((String)value);
		HashSet set = (HashSet)mapmap.getObject(name, value, true);
		if (set == null) {
			set = new HashSet();
			mapmap.putObject(name, value, set);
		}
		set.add(thing);
		return thing;
	}
	
	protected void del(String name,Object value,Thing thing) {
		setUpdate();
		if (isTime(name) && value instanceof String)
			value = Time.day((String)value);
		HashSet set = (HashSet)mapmap.getObject(name, value, false);
		if (set != null) {
//TODO: remove hanging links pointing to this thing from index
			set.remove(thing);
			if (set.size() == 0)
				mapmap.delKey(name, value);
		}
	}
	
	//TODO: get rid of in favor of getByName !!!???
	public java.util.Set get(String type,Object name) {
		HashSet set = (HashSet)mapmap.getObject(type,name,false);
		return set;
	}
	
	//checks if there is a thing that has thing with given name as property of given type
	public boolean has(String type,String name) {
		Collection things = getNamed(name);
		if (!AL.empty(things))
			for (Iterator it = things.iterator(); it.hasNext();) {
				java.util.Set set = (HashSet)mapmap.getObject(type,it.next(),false);
				if (!AL.empty(set))
					return true;
			}
		return false;
	}
	
	//get union of all things that have thing with given name as property of given type
	public java.util.Set get(String type,String name) {
		HashSet set = null;
		Collection things = getNamed(name);
		if (!AL.empty(things)) {
			for (Iterator it = things.iterator(); it.hasNext();) {
				HashSet nextset = (HashSet)mapmap.getObject(type,it.next(),false);
				if (set == null)
					set = nextset;
				else
					set.addAll(nextset);
			}
		}
		return set;
	}
	
	//get union of value sets for values included or excluded by rule	
	public Collection get(String type,Object[] options,boolean rule) {
		ArrayList union = new ArrayList();
		Collection sets = mapmap.getObjects(type,options,rule);
		for (Iterator it = sets.iterator(); it.hasNext();) {
			Collection set = (Collection)it.next();
			union.addAll(set);
		}
		return union;
	}
	
	//get matching things by pattern
	public Collection get(Thing pattern) {
		return get(pattern,null);
	}
	
	//get matching things by pattern's keys
	public Collection get(Thing pattern, String[] keys) {
		String[] names = pattern.getNamesAvailable();
		HashSet res = null;
		for (int i=0;i<names.length;i++) {
			String name = (String)names[i];
			if (!AL.empty(keys) && !Array.contains(keys,name))
				continue;
			Object value = pattern.get(name);
			if (value != null) {
				if (value instanceof Collection) {
					for (Iterator it = ((Collection)value).iterator(); it.hasNext();) {
						Object arg = it.next();
						HashSet set = (HashSet)mapmap.getObject(name,arg,false);
						if (set == null)
							return null;
						if (res == null)
							res = (HashSet)set.clone();
						else
							res.retainAll(set);
					}
				} else {
					if (isTime(name) && value instanceof String)
						value = Time.day((String)value);
					HashSet set = (HashSet)mapmap.getObject(name,value,false);
					if (set == null)
						return null;
					if (res == null)
						res = (HashSet)set.clone();
					else
						res.retainAll(set);
				}
				if (res.size() == 0)
					return null;
			}
		}
		return res;
	}

	//check if left is representing right (left share all properties of right)
	public boolean match(Thing left, Thing right) {
		String[] names = right.getNamesAvailable();
		for (int i=0;i<names.length;i++) {
			String name = (String)names[i];
			Object lvalue = left.get(name);
			Object rvalue = right.get(name);
			//TODO: assert if can be really null
			if (lvalue == null || rvalue == null || lvalue.getClass() != rvalue.getClass())
				return false;
			if (rvalue instanceof java.util.Set) {
				java.util.Set lset = (java.util.Set)left.get(name);
				java.util.Set rset = (java.util.Set)right.get(name);			
				if (!lset.containsAll(rset))
					return false;
			} else { //String or Date
				if (!lvalue.equals(rvalue))
					return false;
			}
		}
		return true;
	}
	
	//TODO: getByName(String name,Object arg, HashSet) - to accumulate results and avoid extra cloning
	public Collection getByName(String name,Object arg) throws Exception {
		Collection res = null;
		if (arg != null && arg instanceof Thing && isThing((String)name)) {
			res = get(name,arg);
		}
		else
		if (arg instanceof String && !AL.empty((String)arg)) { //e.g. name john
			if (isThing((String)name)) {
				//arg = new Thing((String)arg);
				Collection things = getNamed((String)arg);
				if (!AL.empty(things)) {
					if (things.size() == 1)
						res = get(name,things.iterator().next());//TODO:clone too?
					else
						//TODO: normally, this should not happen!?
						for (Iterator it = things.iterator(); it.hasNext();) {	
							Collection args = get(name,it.next());
							if (args != null) {
								if (res == null)
									res = new HashSet(args);
								else
									res.addAll(args);
							}
						}
				}
			} else { 
				if (isTime((String)name))
					arg = Time.day((String)arg);
				res = get((String)name,arg);
			}
		}
		if (arg instanceof Set) { //e.g. is (person, adult)
			Set set = (Set)arg;
			if (set.size() > 0)
				if (set.size() > 1)
					res = get(set,(String)name);//TODO:what, implicit AND!?
				else
					res = get((String)name,set.get(0));
		}
		return res;
	}
		
	public Collection get(Seq seq,Thing getter) throws Exception {
		Collection res = null;
		if (seq.size() == 1 && seq.get(0) instanceof Set)
			res = get((Set)seq.get(0),getter);
		else
		if (seq.size() == 2 && seq.get(0) instanceof Thing && ((Thing)seq.get(0)).getName() == null) {
			//TODO:remove hack for anonymous 'there'
			res = get((Set)seq.get(1),getter);
		}
		else 
		if (seq.size() != 2)
			throw new Exception("invalid expression "+Writer.toString(seq));//TODO:what!?
		else
		if (seq.size() == 2) {
			Object name = seq.get(0);
			if (name instanceof String && !AL.empty((String)name)) {
				String reverse = Schema.reverse((String)name);
				if (reverse != null && getter != null) //subjective attribute
					res = seq.get(1).equals(AL._true) ? (Collection)getter.get(reverse) : null;
				else
					res = getByName((String)name,seq.get(1));
			}
			else
				throw new Exception("invalid expression "+Writer.toString(seq));//TODO:what!?
		}
		return res;
	}
	
	public Collection get(Any any, String name) throws Exception {
		HashSet res = null;
		if (!AL.empty(any)) {
			for (int i=0;i<any.size();i++) {
				Object one = any.get(i);
				HashSet set = (HashSet) (
					one instanceof All ? get((All)one,(Thing)null) : //TODO:getter?
					one instanceof Any ? get((Any)one,(Thing)null) : //TODO:getter?
					one instanceof Seq ? get((Seq)one,(Thing)null) : //TODO:getter?
					one instanceof String && name != null ?	
						getByName(name,one) :
						//(get(name, isThing(name)? new Thing((String)one) 
						//	: isTime(name) ? Time.day((String)one) : one)) :
					null );
				if (AL.empty(set))
					continue;
				if (res == null)
					res = (HashSet)set.clone();
				else
					res.addAll(set);
			}
		}
		return res;
	}

	//TODO:intelligent query execution plan builder with accoutn to cardinalities
	public Collection get(All all,Thing getter) throws Exception {
		HashSet res = null;
		if (!AL.empty(all)) {
			for (int i=0;i<all.size();i++) {
				Object one = all.get(i);
				if (one instanceof Set) {
					HashSet set = (HashSet)get((Set)one,getter);
					if (AL.empty(set))
						return null;
					if (res == null)
						res = (HashSet)set.clone();
					else
						res.retainAll(set);
				}
				else
					;//TODO:what else?				
				if (res.size() == 0)
					return null;
			}
		}
		return res;
	}

	public Collection get(Set set,Thing getter) throws Exception {
		return  
			set instanceof All ? get((All)set,getter) :
			set instanceof Any ? get((Any)set, (String)null) :
			set instanceof Seq ? get((Seq)set,getter) :
			null;
	}

	public Collection get(Set arg, String name) throws Exception {
		return  
			//arg instanceof All ? get((All)arg,name) ://TODO!!!
			arg instanceof Any ? get((Any)arg,name) :
			null;
	}
	
	//helper: get matching things by "name"
	public Collection getNamed(String value) {
		return (Collection) mapmap.getObject(AL.name,value,false);
	}

	public Collection getNamed(String value,String is) {
		Collection allIs = AL.empty(is)? null : (Collection)mapmap.getObject(AL.is,is,false);
		Collection allNamed = (Collection)mapmap.getObject(AL.name,value,false);
		if (AL.empty(allNamed))
			return allNamed;
		Collection namedIs = new ArrayList();
		for (Iterator it = allNamed.iterator(); it.hasNext();){
			Thing t = (Thing)it.next();
			//if either no class requested and thing has no class at all
			//OR
			//class is requested and thing does belong to it
			if (allIs == null){
				if (AL.empty(t.getThings(AL.is)))
					namedIs.add(t);	
			} else {
				if (Array.overlap(t.getThings(AL.is),allIs))
					namedIs.add(t);	
			}
		}
		return namedIs;
	}

	//TODO: handle case with multiples
	public Thing getThing(String value) {
		Collection values = getNamed(value);
		if (!AL.empty(values))
			return (Thing)values.iterator().next();
		return new Thing(value).store(this);
	}
	
	//helper: add property Thing, create if missed
	public void add(Thing thing, String property, String value) throws Exception {
		if (!thing.stored())
			throw new Exception("not stored add");
		Collection values = getNamed(value);
		if (AL.empty(values))
			thing.addThing(property,new Thing(value).store(this));
		else
			for (Iterator vit = values.iterator(); vit.hasNext();)
				thing.addThing(property,(Thing)vit.next());
	}
	
	public void add(String name, String property, String value) throws Exception {
		Collection things = getNamed(name);
		//TODO:if missed!?
		for (Iterator it = things.iterator(); it.hasNext();)
			add((Thing)it.next(), property, value);
	}
	
	public int del(Collection things, boolean force) {
		int count = 0;
		if (force) { //if need to kill potentially dangling links
			MapMap shifted = mapmap.getShifted();
			for (Iterator it = things.iterator(); it.hasNext();) {
				Thing targetThing = (Thing)it.next();
//System.out.println("Target: "+net.webstructor.al.Writer.toString(targetThing));				
				Object[] ownerThings = shifted.getSubKeyObjects(targetThing);
				if (!AL.empty(ownerThings))
					for (int i = 0; i < ownerThings.length; i++) {
//System.out.println("  Owner: "+net.webstructor.al.Writer.toString(ownerThings[i]));				
						Collection propertyNames = (Collection) shifted.getObject(targetThing, ownerThings[i], false);
						if (!AL.empty(propertyNames))
							for (Iterator in = propertyNames.iterator(); in.hasNext();) {
								String propertyName = (String)in.next();
//System.out.println("    Property: "+propertyName);				
								((Thing)ownerThings[i]).delThing(propertyName, targetThing);
							}
				}
			}
		}
		for (Iterator it = things.iterator(); it.hasNext();)
			if (((Thing)it.next()).del()) //if not skipped because of ref counting 
				count++;		
		return count;
	}
	
	protected boolean del(Thing thing) {
		if (mapmap.getKey2Count(thing) > 0)
			return false;
		String[] names = thing.getNamesAvailable();
		for (int i=0;i<names.length;i++) {
			String name = (String)names[i];
			Object value = thing.get(name);
			if (value != null) {
				if (value instanceof String || value instanceof Date)
					del(name, value, thing);
				if (value instanceof Collection) {
					for (Iterator it = ((Collection)value).iterator();it.hasNext();)
						del(name,it.next(),thing);
				}
			}
		}
		return true;
	}
	
	//put thing as is
	protected Thing put(Thing thing) {
		String[] names = thing.getNamesAvailable();
		for (int i=0;i<names.length;i++) {
			String name = (String)names[i];
			Object value = thing.get(name);
			if (value != null) {
				if (value instanceof String || value instanceof Date)
					put(name, value, thing);
				if (value instanceof Collection) {
					for (Iterator it = ((Collection)value).iterator();it.hasNext();)
						put(name,it.next(),thing);
				}
			}
		}
		return thing;
	}
	
	//TODO: cache for better performance!?
	public String[] getNames() {
		String[] actual = mapmap.getKeyStrings();
		String[] possible = getNames(AL.has);
		return Array.union(new String[][]{
			AL.foundation, actual, possible, Schema.thinkable
		});
	}
	
	//TODO:cache "all things" and "all names" of all sorts, invalidate on all updates
	//private HashSet all_set = null;//cache of all things
	//TODO:never use!
	//careful - may be very slow
	public Collection getThings() {return getThings(null);}
	synchronized public Collection getThings(java.util.Set debug) {
		//if (!all_ok) {
			HashSet all_set = new HashSet();
			Collection sets = mapmap.getObjects(null,debug);
			Iterator it = sets.iterator();
			while (it.hasNext()) {
				HashSet set = (HashSet)(it.next());
				Iterator i = set.iterator();			
				while (i.hasNext()) {
					Object obj = i.next();
					all_set.add(obj);
				}
			}
			//TODO: all_ok = true;
		//}
		return all_set;
	}
	
	private boolean is(Thing thing, String[] exceptions) {
		if (Array.contains(exceptions,thing.getName()))
				return true;
		Collection is = (Collection)thing.get(AL.is);
		if (is !=  null)
			for (Iterator it = is.iterator(); it.hasNext();)	
				if (Array.contains(exceptions, ((Thing)it.next()).getName()))
					return true;
		return false;
	}
	
	/**
	 * Removes unlinked things except listed classes and instances. 
	 * @param exceptions - classes
	 * @param instances
	 * @return
	 */
	//TODO: take this out of Storager implementation!?
	public int clear(String[] exceptions, java.util.Set instances) {
		HashSet deletees = new HashSet();
		int all = 0;
		for (;;) {
			int deleted = 0;
			Map counts = mapmap.getKey2Counts(Thing.class);
			//delete all things that has no reverse index
			Iterator it = getThings().iterator();
			while (it.hasNext()) {
				Thing thing = (Thing)it.next();
				if (instances != null && instances.contains(thing))//don't remove unremovables
					continue;
				Integer count = (Integer)counts.get(thing);
				if (count == null || count.intValue() == 0) {
					if (AL.empty(exceptions) || !is(thing,exceptions)) {
						env.debug("Clearing ("+thing+"): "+thing.hashCode() );
						if (deletees.contains(thing)){
							//TODO:handle this as fatal?
							env.debug("Clearing error - repeated: ("+thing+"): "+thing.hashCode() );
							continue;
						}
						deletees.add(thing);
						//TODO: postpone and do via todo list later?
						if (thing.del())
							deleted++;
						else 
							env.debug("Clearing error - undeleteable: ("+thing+"): "+thing.hashCode() );
					}
				}
			}
			if (deleted == 0)
				break;
			all += deleted;
		}
		return all;
	}
	
	//Unsafe, becase can encounter Thing, not String
	public String[] getNames(String name) {
		if (name == null)
			return null;
		if (isThing(name)) {
			//TODO: major optimization
			Object[] things = mapmap.getSubKeyObjects(name);
			if (!AL.empty(things)) {
				String[] names = new String[things.length];
				for (int i=0; i<names.length; i++)
					names[i] = ((Thing)things[i]).getName();
				return names;
			}
			return null;
		}
		else
			return mapmap.getSubKeyStrings(name);
	}
	
	public Object[] getObjects(String name) {
		return mapmap.getSubKeyObjects(name);
	}
	
	//TODO:ontology/metadata
	//private boolean isMultiple(String name) {
	//	return isThing(name);		
	//}
	
	//TODO:ontology/metadata
	public boolean isThing(String name) {
		return Array.contains(multiples, name)? true: false;		
	}

	//TODO:ontology/metadata
	public boolean isTime(String name) {
		return name.equals(AL.times) || name.equals(Peer.activity_time) || name.equals(Peer.login_time) || name.equals(Peer.activity_time);		
	}
	
	//TODO: use everywhere
	protected boolean isValue(Object obj) {
		return obj instanceof String || obj instanceof Date;
	}

	//TODO: mere with exiting code or repurpose?
	/**
	 * In graph A-B->C, C-D->E, get all possible values of C for B across A
	 * optonally making sure each D has value of E 
	 * @param property - B from A-B->C 
	 * @param required - D from C-D->E
	 * @return
	 */
	public Object[] getValues(String property,String required){
		Object[] all = mapmap.getSubKeyObjects(property);
		if (!AL.empty(required))
			return all;
		ArrayList filtered = new ArrayList(all.length);
		for (int i = 0; i < all.length; i++){
			if (all[i] instanceof Thing && ((Thing)all[i]).get(required) != null)
				filtered.add(all[i]);
		}
		return filtered.toArray(new Object[]{}); 
	}

	/**
	 * In graph A-B->C, C-D->E, get all names of possible named values of C for B across A
	 * @param property - B from A->B->C 
	 * @return
	 */
	public String[] getValuesNames(String property){
		Object[] all = getValues(property,AL.name);
		if (all == null)
			return null;
		ArrayList names = new ArrayList(all.length);
		for (int i = 0; i < all.length; i++){
			names.add(((Thing)all[i]).get(AL.name));
		}
		return (String[])names.toArray(new String[]{}); 
	}
}
