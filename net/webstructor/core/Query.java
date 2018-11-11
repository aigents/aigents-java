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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Any;
import net.webstructor.al.Ref;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.peer.Peer;
import net.webstructor.self.Thinker;
import net.webstructor.util.Array;

public class Query
{
	private Storager storager;
	private Thinker thinker;
	private Thing self;

	public Query(Storager storager,Thing self,Thinker thinker) {
		this.storager = storager;
		this.thinker = thinker;
		this.self = self;
	}
	
	public Query(Storager storager) {
		this(storager,null,null);
	}

	public Query(Storager storager,Thing self) {
		this(storager,self,null);
	}

	private Collection getAccessible(Collection all,Thing viewer) throws Exception {
		HashSet set = new HashSet();
		for (Iterator it = all.iterator();it.hasNext();) {
			Thing t = (Thing)it.next();					
			if (!accessible(t,viewer,null,false))//skipping not accessibles
				continue;
			set.add(t);
		}
		return set;
	}

	//if the chain set is representing non-ordered strings
	private String[] getTerminals(Object chain){
		return (chain instanceof All || chain instanceof Any) ? ((Set)chain).toStrings() : null;
	}
	
	private Thing clone(Thing source, String[] args,Thing viewer){
		Thing clone = source.clone(args,viewer);
		if (thinker != null)
			thinker.think(source, clone, args, viewer);
		return clone;
	}
	
	private Object getCurrent(Seq query, int i, Object current, String[] args,Thing viewer) throws Exception {
		if (current instanceof Thing) {
			if (!accessible((Thing)current,viewer,args,false))
				throw new Mistake(Mistake.no_right);
			if (args.length == 1 && storager.isThing(args[0]) 
//TODO: optimize this check that this is the chain link preceding the terminal  
				&& i+1 < query.size() && (query.get(i+1) instanceof String[] || getTerminals(query.get(i+1)) != null))
				//if taking single thing and its one property as thing followed by thing's properties:
				current = ((Thing)current).get(args[0]);
			else {
				//TODO: enable plain name listing
				if (thinker != null && !AL.empty(thinker.thinkables(args)))
					current = clone((Thing)current,args,viewer);
				else
					current = ((Thing)current).clone(args,viewer);
			}
		} else
		if (current instanceof Collection) {
			//extract values from each of the currents 	
			HashSet set = new HashSet(((Collection)current).size());
			Iterator it = ((Collection)current).iterator();
			while (it.hasNext()) {
				Thing t = (Thing)it.next();					
				if (!accessible(t,viewer,args,false))//skipping not accessibles
					continue;
				Thing clone = clone(t, args,viewer);
				if (clone != null)
					set.add(clone);
			}
			current = set;
		}
		return current;
	}
	
	public Collection getThings(Seq query,Thing viewer) throws Exception {
		Object current = null;
		for (int i=0; i<query.size(); i++) {
			Object chain = query.get(i);
			boolean last_chain = i == (query.size() - 1);//last chain
			String[] terminals = last_chain ? getTerminals(chain) : null;
			if (chain instanceof Thing) {
				if (current != null)
					throw new Exception("things messed up");//TODO:apply restrictive condition to current
				Thing thing = (Thing) chain;								
				if (thing.stored()) {//if stored, use it
					current = thing;
					if (last_chain && !accessible(thing,viewer,null,false))
						throw new Mistake(Mistake.no_right);
				} else { //if not stored, find Collection
					current = storager.get(thing);
					if (last_chain)
						current = getAccessible((Collection)current,viewer);
				}
			}
			else 
			//if (chain instanceof Set) {
			if (chain instanceof Set && terminals == null) {
				Collection things = storager.get((Set)chain,viewer);
				if (!AL.empty(things)) {
					if (things.size() == 1) {
						current = things.iterator().next();
						if (last_chain && !accessible((Thing)current,viewer,null,false))
							throw new Mistake(Mistake.no_right);
					} else {
						//TODO:deal with collection of objects or query further when considering scope 
						//throw new Exception("ambigious"); 
						current = things; 
						if (last_chain)
							current = getAccessible((Collection)current,viewer);
					}
				}
			}
			else
			if (!last_chain && chain instanceof String) {
				if (current == null)
					throw new Mistake(Mistake.no_thing);
				//get value(s) from single current or merge values from multiple currents and turn it into current	
//TODO:support multiple currents and merge them into current	
				current = getCurrent(query,i,current,new String[]{(String)chain},viewer);
//TODO:next, be able to apply restrictive condition to current
			}
			else
			if (chain instanceof String[] || (terminals != null || chain instanceof String)) {
				//TODO: consider if there is a cleaner way to get the last "unclosed" path chain 
				if (terminals != null)
					chain = terminals; // determined above
				if (chain instanceof String)
					chain = new String[]{(String)chain};				
				current = getCurrent(query,i,current,(String[])chain,viewer);
				/*
				String[] args = (String[])chain;
				if (current instanceof Thing) {
					if (!accessible((Thing)current,viewer,args,false))
						throw new Mistake(Mistake.no_right);
					if (args.length == 1 && storager.isThing(args[0]) 
						&& i+1 < query.size() && query.get(i+1) instanceof String[])
						//if taking single thing and its one property as thing followed by thing's properties: 
						current = ((Thing)current).get(args[0]);
					else
						//TODO: enable plain name listing
						current = ((Thing)current).clone(args,viewer);
				} else
				if (current instanceof Collection) {
					//extract values from each of the currents 	
					HashSet set = new HashSet(((Collection)current).size());
					Iterator it = ((Collection)current).iterator();
					while (it.hasNext()) {
						Thing t = (Thing)it.next();					
						if (!accessible(t,viewer,args,false))//skipping not accessibles
							continue;
						Thing clone = t.clone(args,viewer);
						if (clone != null)
							set.add(clone);
					}
					current = set;
				}
				*/
			}
		}
		if (current != null && !(current instanceof Collection)) {
			HashSet set = new HashSet(1);
			set.add(current);
			current = set;
		}			
		return (Collection)current;
	}

	//TODO:sort out
	private String property(Set seq){
		if (seq instanceof Seq && seq.size() == 2 && seq.get(0) instanceof String)
			return (String)seq.get(0);
		return null;
	}

	private String[] properties(All all){
		String[] properties = new String[all.size()];
		for (int i = 0; i < all.size(); i++)
			properties[i] = all.get(i) instanceof Seq ? property((Seq)all.get(i)) : null;
		return properties;
	}
	
	public void setThings(Thing thing, Seq seq, Thing setter) throws Exception {
		if (!accessible(thing,setter,new String[]{property(seq)},true))
			throw new Mistake(Mistake.no_right);
		if (seq.size() != 2) //TODO: sort this out
			throw new Exception("invalid properties");
		setThings(thing,(String)seq.get(0),seq.get(1),setter);
	}

	protected void setThings(Thing thing, All all, Thing setter) throws Exception {
		if (!accessible(thing,setter,properties(all),true))
			throw new Mistake(Mistake.no_right);
		for (int j=0;j<all.size();j++) {
			Object obj = all.get(j);
			if (!(obj instanceof Seq))
				throw new Exception("no property");//TODO:apply restrictive condition to current
			setThings(thing,(Seq)obj,setter);
		}
	}
	
	public void setThings(Seq query, Thing setter) throws Exception {
		Object current = null;
		for (int i=0; i<query.size(); i++) {
			Object chain = query.get(i);
			if (chain instanceof Thing) { // single root thing to start with (AKA SUBJECT)
				if (current != null)
					throw new Exception("things messed up");//TODO:apply restrictive condition to current
				Thing thing = (Thing) chain;							
				if (thing.stored()) //if stored, use it
					current = thing;
				else { //if not stored, find Collection
					//current = get(thing);
					Collection coll = storager.get(thing);

					//TODO:Fix the following hack getting rid of "there"-s!
					if (AL.empty(coll) && query.get(i+1) instanceof Set) {
						Set q = (Set)query.get(i+1);
						coll = storager.get(q,setter);
						if (!AL.empty(coll))
							i++;//jump over the next step
					}
										
					if (AL.empty(coll)) {
						thing.store(storager);
						current = thing;
					}
					else
					if (coll.size() == 1)
						current = (Thing)coll.iterator().next();
					else
					if (coll.size() > 1)
						//TODO:clone to enable concurrent changes!?
						current = coll; 
				}
			}
			else
			if (current == null && chain instanceof Set) {//get object by qualifier
				//TODO:
				Collection coll = storager.get((Set)chain,setter);
				if (AL.empty(coll)) {
					//TODO: create!? thing.store(storager);
					throw new Mistake(Mistake.no_thing);
				}
				else
				if (coll.size() == 1)
					current = (Thing)coll.iterator().next();
				else
				if (coll.size() > 1)
					current = new HashSet(coll);//clone to enable concurrent changes
			}
			else
			if (chain instanceof All) { // (list of all AKA PREDICATE-OBJECT pair)
				if (current == null || !(current instanceof Thing || current instanceof Collection))
					throw new Mistake(Mistake.no_thing);//TODO:apply restrictive condition to current	
				if (current instanceof Thing)
					setThings((Thing)current,(All)chain,setter);					
				if (current instanceof Collection) {
					for (Iterator it = ((Collection)current).iterator(); it.hasNext();)
						setThings((Thing)it.next(),(All)chain,setter);					
				}
				/*
				if (!accessible((Thing)current,setter))
					throw new Mistake(Mistake.no_right);
				All all = (All)chain;
				for (int j=0;j<all.size();j++) {
					Object obj = all.get(j);
					if (!(obj instanceof Seq))
						throw new Exception("no property");//TODO:apply restrictive condition to current
					if (current instanceof Thing)
						setThings((Thing)current,(Seq)obj,setter);
					else 
					if (current instanceof Collection)
						for (Iterator it = ((Collection)current).iterator(); it.hasNext();)
							setThings((Thing)it.next(),(Seq)obj,setter);
				}
				*/
			}
			else
			if (chain instanceof Seq) { // (pair of one AKA PREDICATE-OBJECT pair)
				if (current == null)
					throw new Mistake(Mistake.no_thing);//TODO:apply restrictive condition to current
				if (current instanceof Thing)
					setThings((Thing)current,(Seq)chain,setter);
				else 
				if (current instanceof Collection) {
					//TODO: this improvement fails unit tests - why?
					//HashSet coll = new HashSet(((Collection)current).size());//clone to avoid concurrent access
					//for (Iterator it = ((Collection)coll).iterator(); it.hasNext();)
					for (Iterator it = ((Collection)current).iterator(); it.hasNext();)
						setThings((Thing)it.next(),(Seq)chain,setter);
				}
				else
					throw new Mistake(Mistake.no_thing);
			}
		}
	}

	//set Thing property, strictly assuming isThing=>isMultiple
	public void setThing(Thing thing,String name,Object val,boolean is,Thing setter) throws Exception {
		if (storager.isThing(name)) {
			//TODO: now assuming isMultiple==isThing==true, other possibilities?
			if (is) { // add things by name string
				Collection coll = storager.getNamed((String)val);
				if (AL.empty(coll)) { // create Thing object if not found 
					Thing query = new Thing((String)val); // create query from name string
					query.store(storager);
					thing.addThing(name, query);
				}
				else { // copy Thing objects if found 
					Iterator it = coll.iterator();
					while (it.hasNext()) {
						Object obj = it.next();
						if (!(obj instanceof Thing))
							throw new Exception(name+" not thing");
						thing.addThing(name,((Thing)obj));
					}
				}
			} else { 
				Collection coll = storager.getNamed((String)val);
				if (!AL.empty(coll))
					for (Iterator it = coll.iterator(); it.hasNext();)
						thing.delThing(name, (Thing)it.next());
			}
		}	
		else {
			//TODO: now assuming isMultiple==isThing==false, other possibilities?
			if (is && Schema.unique(name) && !AL.empty(storager.getByName(name, val)))
				throw new Mistake(name+" "+val+" is owned");
			thing.set(name, is? val: null, setter);
		}
	}
	
	public void setThings(Thing thing,String name,Object arg,Thing setter) throws Exception {
		// could be list, string
		if (arg instanceof String || arg instanceof Date) {
			setThing(thing,name,arg,true,setter);
		}
		else
		if (arg instanceof Ref) {
			Ref ref = (Ref)arg;
			setThing(thing,name,(String)ref.get(0),ref.is(),setter);
		}
		else
		if (arg instanceof All) {//list of items
			All all = (All)arg;
			for (int l=0; l<all.size(); l++) {
				Object term = all.get(l);
				if (term instanceof String || term instanceof Date || term instanceof Ref) {
					setThings(thing,name,term,setter);
				}
				else //TODO: query qualifier
					throw new Exception("invalid "+name);
			}
		}					
	}

	/**
	 * Linkable/reversibel attributes that don't require real writing
	 */
	boolean writeable(String args[]) {
		if (!AL.empty(args)){
			int count = 0;
			for (int i = 0; i < args.length; i++)
				if (Schema.reverse(args[i]) != null)
					count++;
			return count == args.length;
		}
		return false;
	}
	
	/**
	 * Any keys and linkable/reversible attributes.
	 */
	boolean readable(String args[]) {
		if (!AL.empty(args)){
			int count = 0;
			for (int i = 0; i < args.length; i++)
				if (Array.contains(Schema.keys, args[i]) || Array.contains(Schema.thinkable, args[i])|| Schema.reverse(args[i]) != null)
					count++;
			return count == args.length;
		}
		return false;
	}
	
	//newer version, bans access only to 
	// - self body, unless trusted 
	// - peers with authorization setup, unless trusted
	boolean accessible(Thing thing, Thing peer, String args[], boolean write) {
		boolean readable = readable(args);
		if (write == false && readable)
			return true;
		//TODO: if peer == self return true as system call!?
		if (peer == null)//TODO:why and what?
			return false;
		if (thing.equals(peer))//enable change peer self
			return true;
		//TODO:cleanup?
		//boolean root = self.get(AL.trust,peer).equals(AL._true) && peer.get(AL.trust,self).equals(AL._true);		
		Collection trusts = (Collection)self.get(AL.trusts);
		boolean root = !AL.empty(trusts) && trusts.contains(peer);
		if (thing.equals(self))//non super-user can't do anything to self besides keys reading
			return root;
		//if writing to true non-reversible attributes
		if (!readable || (write && !writeable(args))){
			if (Peer.registered(thing))//if registered peer, ban any access
				return false;
			//TODO: make more restricted on basis of peer trusting (that makes "friending" impossible)?
			//if (!thing.get(AL.trust,peer).equals(AL._true))
				//return false;
		}
		return true;
	}
	
}
