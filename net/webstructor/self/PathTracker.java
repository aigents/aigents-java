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
package net.webstructor.self;

import java.util.ArrayList;
import java.util.Collection;

import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Thing;

public class PathTracker extends PathFinder {

	PathTracker(Siter siter, Collection goals, int hopLimit) {
		super(siter, goals, hopLimit);
	}

	boolean run(String path) {
		//parse pattern for pathset
		//TODO: what if many goals of the same name?
		if (!AL.empty(goals) && goals.size() == 1) {
			Thing goal = (Thing)goals.iterator().next();
			String pathStr = goal.getString(AL.path);
			if (!AL.empty(pathStr)) {
				//TODO: now pattern scan Seq/s only, must be Sets!
				//TODO: what if 'owner = null' strikes?
				Thing dummyPatternConext = new Thing();//TODO: make use of it or make it not needed?
				Set set = Reader.patterns(siter.storager,dummyPatternConext,pathStr);
				if (!(set instanceof Seq))
					set = new Seq(new Object[]{set});
				//TODO: consider splitting multiple paths in path set into many
//System.out.println(Writer.toString(new StringBuilder(),set,true));
				if (run(path,(Seq)set))
					return true;
				//check results (wait for spawned threads, if any)
				//if results present, then return
				//if no results, then spawn PathFinder   
			}
		}
		return new PathFinder(this.siter,goals,hopLimit).run(path);
	}

	//possible inputs:
	//[a b c d]
	//[{a b} [c d]}]
	//[{[a b] [c d]}]
	boolean run(String path,Seq pathSeq) {
	  	//get the goal
	 	//get the “page context”
		//get all “link contexts”
		//check the “page context” 
		//	if “page context” is matching the goal
		//		retain the results
		//		depending on “rapid” or “exhaustive” modality, do either:
		//			“rapid” modality: stop (terminating spawned threads, if any)
		//			“exhaustive” modality: continue
		readPaths.add(path);
		ArrayList links = new ArrayList();
		if (siter.readPage(path,siter.time,links,goals)) {
			return true;
		}
		
		//get “path set” leading to goal
		//check if it is not ended
		if (AL.empty(pathSeq))
			return false;
		//evaluate “path set” items against the “link contexts” starting with the shortest path in “path set”
		//recurse for paths in the set
		if (pathSeq.size() == 1 && pathSeq.get(0) instanceof Any)
			return run(path,links,(Any)pathSeq.get(0));
		//recurse 
		return run(path,links,pathSeq);
	}

	
	//split path set of alternatives into multiple paths 
	//TODO: considering the most "energetic" paths first
	boolean run(String path,ArrayList links,Any pathSet) {
		int cnt = 0;
		for (int i = 0; i < pathSet.size(); i++) {
			Object pathItem = pathSet.get(i);
			boolean found = 
				pathItem instanceof Any ? run(path,links,(Any)pathItem) : 
				pathItem instanceof Seq ? run(path,links,(Seq)pathItem) : 
				pathItem instanceof String ? run(path,links,new Seq(new Object[]{pathItem})) :
				false;
			if (found) {
				if (!exhaustive) //if found and "rapid" mode, then stop
					return true;
				cnt++;
			}
		}
		return cnt > 0;
	}
	
	//evaluate specific path sequence against specific set of links in context of given path
	boolean run(String path,ArrayList links,Seq pathSeq) {
		//	for each “link context” matching “path set” item
		//			reduce “path set” excluding the item and change “page context” (“path burnout”)
		//			recurse Path Tracker (spawn thread) with same goal, reduced “path set” and new “page context”
		//			on successful return, depending on “rapid” or “exhaustive” modality, do either:
		//				“rapid” modality: stop (terminating spawned threads, if any)
		//				“exhaustive” modality: continue		
		int cnt=0; 
		for (int i=0;i<links.size();i++){
			//{link,text}
			String[] link = (String[])links.get(i);
			String linkUrl = HttpFileReader.alignURL(path,link[0],siter.strict);
			String linkText = link[1];
			Seq patSeq = pathSeq.get(0) instanceof Seq ? (Seq)pathSeq.get(0) : new Seq(new Object[]{pathSeq.get(0)});
			if (!AL.empty(linkUrl) && !AL.empty(linkText) && !readPaths.contains(linkUrl) && siter.linkMatch(linkText,patSeq)) {
				Seq newPath = pathSeq.reduceHead();
				boolean found = run(linkUrl,newPath);
				if (found) {
					if (!exhaustive) //if found and "rapid" mode, then stop
						return true;
					cnt++;
				}
			}
		}
		return cnt > 0;
	}
	
}