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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.al.Writer;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Thing;

public class PathFinder {
	int hopLimit;
	boolean exhaustive;
	Siter siter;
	HashSet readPaths = new HashSet();
	ArrayList pathSeqs = new ArrayList();
	protected Collection goals;

	PathFinder(Siter siter, Collection goals, int hopLimit) {
		this.siter = siter;
		this.goals = goals;
		this.hopLimit = hopLimit;
		this.exhaustive = siter.newsLimit > 1;//TODO: fix hack!?
	}

	//TODO: move this out of specific Parser-based functionality!? 
	static Object chain(String text) {
		//TODO: link text as Seq!
		Iter iter = new Iter(Parser.parse(text));
		if (iter.size() <= 1)
			return text;
		//TODO: move to other place?
		Object[] seq = new Object[iter.size()];
		for (int i = 0; i < iter.size(); i++)
			seq[i] = iter.next();
		return new Seq(seq);
	}
	
	//append the tail
	public Seq extendTail(Seq seq, String text) {
		int size = seq == null ? 0 : seq.size(); 
		Object[] ext = new Object[size + 1];
		for (int i = 0; i < size; i++)
			ext[i] = seq.get(i);
		ext[size] = chain(text);
		return new Seq(ext);
	}
	
	boolean run(String path) {
		//if (run(path,null)) {
		if (runParallel(path,null)) {
			//check results (wait for spawned threads, if any)
			//get “path set” leading to goal
			//merge proven “path sets” with existing “path set”
			Any pathSet = new Any(pathSeqs.toArray(new Seq[]{}));
			//attach path to goal
			//TODO: what if multiple goals and multiple paths for the goals!?
			if (!AL.empty(goals))
			for (Iterator it = goals.iterator(); it.hasNext();) {
				Thing goal = (Thing)it.next();
				//intelligent merge
				String pathStr = goal.getString(AL.path);
				if (!AL.empty(pathStr)) {
					Set set = Reader.patterns(siter.storager,null,pathStr);
					//ensure old paths are not overwritten - with merge
					pathSet = pathSet.merge(set instanceof Any ? (Any)set : new Any(new Object[]{set}));
				}
				//TODO:keep path sets in better way?
//System.out.println(Writer.toString(new StringBuilder(),pathSet,true));
				goal.set(AL.path,Writer.toString(new StringBuilder(),pathSet,true).toString().toLowerCase());
			}
			return true;
		}
		return false;
	}
	
	boolean run(String path,Seq pathSeq) {
		if (siter.expired())
			return false;
		//get the goal
		//get the “page context”
		//check the “page context” 
		//	if “page context” is matching the goal
		//		retain the results
		//		retain the current hypothetical “path set” as proven
		//		depending on “rapid” or “exhaustive” modality, do either:
		//			“rapid” modality: stop (terminating spawned threads, if any)
		//			“exhaustive” modality: continue
		//TODO: thread safety in concurrent mode!
		readPaths.add(path);
		ArrayList links = new ArrayList();
		if (siter.readPage(path,siter.time,links,goals)) {
			pathSeqs.add(pathSeq);
			return true;
		}
		//get all “link contexts”
		//explore all possibilities
		//	for each “link context”
		//		add current “link context” to new hypothetical “path set” (create new one if not on recursion or add to cloned copy of incoming if on recursion)
		//		recurse PathFinder (spawn thread) with same goal, current hypothetical “path set” and new “page context”
		//		on successful return, depending on “rapid” or “exhaustive” modality, do either:
		//			“rapid” modality: stop (terminating spawned threads, if any)
		//			“exhaustive” modality: continue
		int cnt=0; 
		for (int i=0;i<links.size();i++){
			if (siter.expired())
				return cnt > 0;
			//{link,text}
			String[] link = (String[])links.get(i);
			String linkUrl = HttpFileReader.alignURL(path,link[0],siter.strict);
			String linkText = link[1];
			if (!AL.empty(linkUrl) && !AL.empty(linkText) && !readPaths.contains(linkUrl)) {
				Seq extPath = extendTail(pathSeq,linkText);
				boolean found = run(linkUrl,extPath);
				if (found) {
					if (!exhaustive) //if found and "rapid" mode, then stop
						return true;
					cnt++;
				}
			}
		}
		return cnt > 0;
	}
	
	boolean runParallel(String pathBase,Seq basePath) {
		if (siter.expired())
			return false;
		
		Queue q = new LinkedList();
		q.add(new Object[]{pathBase,basePath});
		readPaths.add(pathBase);
		int cnt=0;
		
		while (q.size() > 0) {
			if (siter.expired())
				return cnt > 0;
				
			Object[] args = (Object[])q.poll();
			String path = (String)args[0];
			Seq pathSeq = (Seq)args[1];
			//get the goal
			//get the “page context”
			//check the “page context” 
			//	if “page context” is matching the goal
			//		retain the results
			//		retain the current hypothetical “path set” as proven
			//		depending on “rapid” or “exhaustive” modality, do either:
			//			“rapid” modality: stop (terminating spawned threads, if any)
			//			“exhaustive” modality: continue
			//TODO: thread safety in concurrent mode!
			
			//restrict dive depth by number of levels
			//if (!AL.empty(pathSeq) && pathSeq.size()+1 >= hopLimit)
			//	continue;
			
			ArrayList links = new ArrayList();
			if (siter.readPage(path,siter.time,links,goals)) {
				if (pathSeq != null)//TODO: should we add "head of the trail" as a separate path?
					pathSeqs.add(pathSeq);
				cnt++;
				if (!exhaustive) //if found and "rapid" mode, then stop
					break;
				//continue;//TODO: stop digging in on finding the possible best path?
			}

			//TODO: do it above or here just to pass unit test!?
			//restrict dive depth by number of levels
			if (!AL.empty(pathSeq) && pathSeq.size() >= hopLimit)
				continue;

			//get all “link contexts”
			//explore all possibilities
			//	for each “link context”
			//		add current “link context” to new hypothetical “path set” (create new one if not on recursion or add to cloned copy of incoming if on recursion)
			//		recurse PathFinder (spawn thread) with same goal, current hypothetical “path set” and new “page context”
			//		on successful return, depending on “rapid” or “exhaustive” modality, do either:
			//			“rapid” modality: stop (terminating spawned threads, if any)
			//			“exhaustive” modality: continue
			if (siter.expired())
				return cnt > 0;
			for (int i=0;i<links.size();i++){
				//{link,text}
				String[] link = (String[])links.get(i);
				String linkUrl = HttpFileReader.alignURL(path,link[0],siter.strict);
				String linkText = link[1];
				//linkText may be null because of graphics!
				if (!AL.empty(linkUrl) && linkText != null && !readPaths.contains(linkUrl)) {
					Seq extPath = extendTail(pathSeq,linkText);
					q.add(new Object[]{linkUrl,extPath});
					readPaths.add(linkUrl);
				}
			}
		}//while have something to do
		return cnt > 0;
	}
	
}
