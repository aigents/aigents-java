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
package net.webstructor.data;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Environment;
import net.webstructor.core.Filer;
import net.webstructor.main.Mainer;
import net.webstructor.util.Array;

public class GraphCacher {

	//TODO: make parameter
	public static final int MEMORY_THRESHOLD = 75;
	
	private HashMap graphs = new HashMap(); //date-based graphs
	private Filer filer;
	private Environment env;
	private String nameCapital;
	private String pathDir;
	private String pathPrefix;
	private long age = 0;
	
	public GraphCacher(String name, Environment env, String path){
		filer = new Filer(env);
		this.nameCapital = Writer.capitalize(name);
		this.pathDir = (AL.empty(path) ? "" : path + (path.endsWith("/") ? "" : "/")) + name;
		this.pathPrefix = pathDir+"/"+name+"_";//"ethereum/ethereum_"
		this.env = env;
	}

	public GraphCacher(String name, Environment env){
		this(name, env, null);
	}
	
	public void setAge(long age){
		this.age = age;
	}
	
	public void clear(boolean everything){
		synchronized (graphs){
			env.debug(nameCapital+" graphs forget memory "+env.checkMemory());
			clearUnsync(null);
			env.debug(nameCapital+" graphs forgot memory "+env.checkMemory());
			if (everything){
				filer.del(pathDir);
			}
		}
	}
	
	private void clearUnsync(Date except){
		saveGraphsUnsync();//save what is not saved
		int total = graphs.size();
		int cleared = 0;
		if (except == null){
			cleared = total;
			graphs.clear();
		}else
			for (Iterator it = graphs.keySet().iterator(); it.hasNext();){
				Date date = (Date)it.next();
				if (!date.equals(except)){
					cleared++;
					graphs.remove(date);
					it = graphs.keySet().iterator();//start over with clean iterator
				}
			}
		env.debug(nameCapital+" graphs cleared "+cleared+" of "+total);
		if (cleared > 0)
			System.gc();
	}

	//save all unsaved graphs
	public void saveGraphs(){
		synchronized (graphs){
			saveGraphsUnsync();
		}
	}

	//save all graphs saved earlier than this.age
	private void saveGraphsUnsync(){
		synchronized (graphs){
			for (Iterator it = graphs.keySet().iterator(); it.hasNext();){
				Date date = (Date)it.next();
				Graph graph = (Graph)graphs.get(date);
				synchronized (graph){//TODO: if this is really needed, not an overkill?
					if (graph.getAge() < this.age){
						graph.setAge(this.age);
						String path = pathPrefix+Time.day(date,false)+".ser";
						env.debug(nameCapital+" graphs saving "+path);
						filer.save(path, graph);
					}
				}
			}
		}
	}
	
	public void updateGraph(Date date, Graph graph, long age){
		synchronized (graphs){
			env.debug(nameCapital+" graph save for "+date+" memory "+env.checkMemory());
			if (env.checkMemory() > MEMORY_THRESHOLD)
				clearUnsync(date);
			synchronized (graph){//TODO: if this is really needed, not an overkill?
				graph.setAge(age);
				if (this.age < age)
					this.age = age;
				String path = pathPrefix+Time.day(date,false)+".ser";
				env.debug(nameCapital+" graphs saving "+path);
				filer.save(path, graph);
			}
			env.debug(nameCapital+" graphs saved for "+date+" memory "+env.checkMemory());
		}
	}
	
	public Graph getGraph(Date date){
		synchronized (graphs){
			env.debug(nameCapital+" graphs ask for "+date+" memory "+env.checkMemory());
			Graph graph = (Graph)graphs.get(date);
			if (graph == null){
				if (env.checkMemory() > MEMORY_THRESHOLD)
					clearUnsync(null);
				graph = (Graph)filer.load(pathPrefix+Time.day(date,false)+".ser");
				if (graph == null)
					graph = new Graph();
				graphs.put(date, graph);
				if (env.checkMemory() > MEMORY_THRESHOLD)
					clearUnsync(date);
			}
			env.debug(nameCapital+" graphs got for "+date+" memory "+env.checkMemory());
			return graph;
		}
	}

	public Graph getSubgraph(String[] ids, Date date, int period, int range, int threshold, int limit, String format, String[] links){
		GraphCacher grapher = this;
		HashSet todo = Array.toSet(ids);		
		HashSet visited = new HashSet();
		Graph all = new Graph();
		for (int r = 0; r < range; r++){//iterate given number of range expansions
			HashSet next = new HashSet();
			visited.addAll(todo);
			for (int daysback = 0; daysback <= period; daysback++){
				Graph daily = grapher.getGraph(Time.date(date, -daysback));
				//collect all links for given ids of given types, collect link targets
//TODO: don't consider done-s here
				daily.getSubgraphTargets(todo, visited, links, all, next);
			}
			next.removeAll(visited);
			todo = next;
		}
		
		all.normalize();
		Graph candidate = all;
		Graph result = new Graph();
		
		//apply limit : old version - extract limited number of link based on their weight
		int limit_type = 0;//0-link_weights/1-nodes_weights;
		if (limit_type == 0){//limit by weights of links (it works)
			//TODO: make threshold search dichotomized!
			for (;;){
				long nodes = candidate.size()[0];
				if (limit <= 0 || nodes <= limit){
					result = candidate;
					break;
				}
				candidate = all.getSubgraph(++threshold);
			}
		}else{//if (limit_type == 1) //limit by attraction of nodes (does not work well enough)
			//apply limit :new version - extract limited number of nodes based on their "liquid-reputation" connectivity
			//TODO: account for threshold here
			result = limit > 0 ? all.getTopConnectedNodesSubgraph(limit,range,ids) : all;
		}

		//save memory
		if (all != result)
			all.clear();
		if (candidate != result)
			candidate.clear();
		return result;
	}
	
	public void to(PrintStream out){
		synchronized (graphs){
			for (Iterator it = graphs.keySet().iterator(); it.hasNext();){
				Date date = (Date)it.next();
				if (env.checkMemory() > MEMORY_THRESHOLD)
					clearUnsync(date);
				Graph graph = (Graph)graphs.get(date);
				out.println(date+":");
				synchronized (graph){//TODO: if this is really needed, not an overkill?
					graph.to(out);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		if (args.length > 0) {
			Filer filer = new Filer(new Mainer());
			Graph graph = (Graph)filer.load(args[0]);
			long timestamp = graph.getAge();
			Date date = new Date((long)timestamp);
			System.out.println(timestamp+"\t"+date);
		}
	}
}
