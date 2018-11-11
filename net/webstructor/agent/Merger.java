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
package net.webstructor.agent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;

public class Merger {

	protected Storager storager;
	protected Body body;
	
	public Merger(Body body, Storager storager) {
		this.body = body;
		this.storager = storager;
	}
	
	
	//TODO: move the following to Merger class
	public void merge(Collection mergees) throws Exception {
		HashMap twinthings = new HashMap();
		//for each thing
		for (Iterator it = mergees.iterator(); it.hasNext();) {
			Thing thing = (Thing)it.next();
			//find twins
			Collection twins = storager.get(thing,Schema.keys);
			if (AL.empty(twins)) { //must have at least itself
				//throw new Mistake("no thing for "+Writer.toString(thing));
//System.out.println(thing+" : 0 - ERROR!");//TODO:clear				
				continue;//TODO: what if no keys for merging?			
			} else
			if (twins.size() == 1) { //itself
//System.out.println(thing+" : 1 - SAME");//TODO:clear				
				continue;
			} else {
				;
//if (thing.toString().indexOf("name aigents") != -1)
//System.out.println(thing+" : "+twins);//TODO:clear				
			}
			
			//TODO:ensure only couple twins can appear and not multiple
			//if (twins.size() > 2) //multiplicates can not have place normally
				//throw new Exception("multiple things for "+Writer.toString(thing));

			//among the twins, find the one which is native
			Thing lucky = null;
			for (Iterator twit = twins.iterator(); twit.hasNext();) {
				Thing twin = (Thing)twit.next();
				if (!mergees.contains(twin)) {
					lucky = twin;//merge to the first native
					break;
				}
			}
			if (lucky == null) {
				//TODO: can't happen under normal conditions!
//System.out.println("NO NATIVE FOR MERGE+"+thing);//TODO:clear	
				lucky = (Thing)twins.iterator().next();//mere left to the first
			}

			for (Iterator twit = twins.iterator(); twit.hasNext();) {
				Thing twin = (Thing)twit.next();
				if (twin != lucky)
					twinthings.put(twin, lucky);
			}
		}
		
		//translate all what can be translated
		if (twinthings.size() > 0)
			replace(twinthings);
		
		//finally check for self-split
		Thing self = null; 
		HashSet selves = new HashSet();
		//for multiple classes of 'self'
		Collection classes = storager.getNamed(Schema.self);
		for (Iterator it = classes.iterator(); it.hasNext();) {
			//for all instances of the class
			Collection instances = storager.getByName(AL.is,it.next());
			for (Iterator ii = instances.iterator(); ii.hasNext();) {
				Thing thing = (Thing)ii.next();
				//consider native the first one
				if (self == null && !mergees.contains(thing))
					self = thing;
				else
					selves.add(thing); 
			}
		}
		twinthings.clear();
		for (Iterator it = selves.iterator(); it.hasNext();)
			twinthings.put(it.next(), self);
		//translate the selves
		if (twinthings.size() > 0)
			replace(twinthings);
	}

	void replace(Map twinthings) {
		// re-link linkers of doomed twins with their successors
		// for all known properties
		String[] names = storager.getNames();//mapmap.getKeyStrings();
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			// for every Thing property
			if (storager.isThing(name)) {
				//get all objects referenced by that property
				Object[] things = storager.getObjects(name);//mapmap.getSubKeyObjects(name);
				if (!AL.empty(things))
				for (int j = 0; j < things.length; j++) {
					// for any object referenced by this property
					Object o = things[j];
					if (!(o instanceof Thing)) {
						body.error("Fatal error: mistype "+name+" "+o,null);
						continue;
					}
					Thing twin = (Thing)things[j];
					Thing lucky = (Thing)twinthings.get(twin);
					if (lucky != null) { //if there is a translation needed
						HashSet victims = (HashSet)storager.get(name,twin);//mapmap.getObject(name, twin, false);
						if (!AL.empty(victims)) {
							victims = new HashSet( victims ); // copy to avoid concurrency
							for (Iterator it = victims.iterator(); it.hasNext();) {
								// for each such victim, translate twin to thing
								Thing victim = (Thing)it.next();
								victim.delThing(name, twin);
								Thing translated = (Thing)twinthings.get(victim);
								if (translated != null)
									victim = translated;
								victim.addThing(name, lucky);
							}
						}
					}
				}
			}
		}
		/*
		int present = getThings().size();
		int translations = twinthings.keySet().size();
		*/
		// finally, just update twins and eliminate their replaced origins
		Collection things = twinthings.keySet();
		for (Iterator it = things.iterator(); it.hasNext();) {
			Thing twin = (Thing)it.next();
			Thing lucky = (Thing)twinthings.get(twin);
			if (lucky != null) {
				
				//TODO: why did we avoid cloning keys?
				//update thing from twin, except keys
				//lucky.update(twin,Array.sub(twin.getNamesAvailable(),Schema.keys));//before 20161002

				lucky.update(twin,null);//after 20161002
				
				//self-destroy
				boolean deleted = twin.del();
				if (!deleted)
					body.debug("NOT DELETED: "+twin);//TODO:clear	
			}
		}		
		//int remains = getThings().size();
		//System.out.println("FINAL:"+present+"-"+translations+"="+remains);
	}
	
	
}
