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
package net.webstructor.data;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.webstructor.al.AL;
import net.webstructor.al.Writer;
import net.webstructor.util.Array;
import net.webstructor.core.Filer;

//TODO: make this thread-safe (assuming it is synchroniseable outside)?
//TODO: reverse order for target and property (assuming it is already used)?

/* Fuzzy graph of multiple Linkers ( a( p(x,y,...), q(x,y,...), ... ), b( p(x,y,...), q(x,y,...), ... ) */
public class Graph implements Serializable {
	private static final long serialVersionUID = 3671961535358554573L;
	
//TODO sync it!?
	//http://tutorials.jenkov.com/java-util-concurrent/readwritelock.html
	//transient ReadWriteLock lock = ...
	transient private boolean modified = false;//not modified on load and creation blank
	private long age = 0;
	private HashMap binders = new HashMap();
	
	public void save(Filer filer, String path){
		filer.save(path, this);
		modified = false;
	}
	public boolean modified(){
		return modified;
	}
	public void clear(){
		binders.clear();
	}
	public long getAge(){
		return age;
	}
	public void setAge(long age){
		this.age = age;
		modified = true;
	}
	public HashMap getLinkers(Object context,boolean add) {// a => a->( p(x,y,...), q(x,y,...), ... )
		Object o = binders.get(context);
		if (o == null){
			if (!add)
				return null;
			binders.put(context, o = new HashMap());
			modified = true;
		}
		return (HashMap)o;
	}
	public HashMap getPropertyLinkers(Object property) {// p => ( a->p(x,y,...), b->p(x,y,...), ... )
		HashMap linkers = new HashMap();
		for (Object context : binders.keySet()) {
			HashMap properties = (HashMap)binders.get(context);
			if (AL.empty(properties))//no properties at all
				continue;
			Linker targets = (Linker)properties.get(property);
			if (targets == null || targets.size() == 0)//no given property
				continue;
			linkers.put(context, targets);
		}
		return linkers;
	}
	public Set getSources(){
		return binders.keySet();
	}
	public Linker getLinker(Object context, Object target,boolean add) {// a,p => a->p->(x,y,...)
		HashMap linkers = getLinkers(context,add);
		if (linkers == null)
			return null;
		Linker l = (Linker)linkers.get(target);
		if (l == null) {
			if (!add)
				return null;
			linkers.put(target, l = new Counter());
			modified = true;
		}
		return l;
	}
	public Number getValue(Object context, Object target, Object property) {// a,p,x => a->p->x
		Linker linker = (Linker) getLinker(context,property,false);
		return linker == null ? null : linker.value(target);
	}
	public void addValue(Object context, Object target, Object property, int amount) {// a,p,x => a->p->x
		Linker linker = (Linker) getLinker(context,property,true);
		linker.count(target,amount);
		modified = true;
	}
	public void addValue(Object context, Object target, Object property, double number) {// a,p,x => a->p->x
		Linker linker = (Linker) getLinker(context,property,true);
		linker.count(target,number);
		modified = true;
	}
	public void addValue(Object context, Object target, Object property, ComplexNumber[] cn) {// a,p,x => a->p->x
		Linker linker = (Linker) getLinker(context,property,true);
		linker.count(target,cn);
		modified = true;
	}
	public void addValues(Object context, Object property, Linker targetValues) {
		Linker linker = (Linker) getLinker(context,property,true);
		for (Iterator it = targetValues.keys().iterator(); it.hasNext();){
			String target = (String)it.next();
			linker.count(target, targetValues.value(target).intValue());
			modified = true;
		}
	}
	public void countTargets(Set sources, String[] links, Counter targets){
		if (!AL.empty(sources)){
			for (Iterator it = sources.iterator(); it.hasNext();){
				String id = (String)it.next();
				HashMap linkers = getLinkers(id,false);//get outgoing links
				if (AL.empty(linkers))
					continue;
				for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
					String property = (String)lit.next();
					if (AL.empty(links) || Array.contains(links, property)){//check link property
						Linker l = (Linker)linkers.get(property);
						for (Iterator tit = l.keys().iterator(); tit.hasNext();){
							Object target = tit.next();
							targets.count(target);
						}
					}
				}
			}
		}
	}
	
	//get subgraph for list of sources given specified links, storing links in collector and collecting targets
	public Graph getSubgraphTargets(final Set sources, final Set globallyVisited, final String[] links, final Set members, Graph collector, Set targets, Map<String,String> inversions){
		if (!AL.empty(sources)){
			HashSet locallyVisited = new HashSet();
			for (Iterator it = sources.iterator(); it.hasNext();){
				String id = (String)it.next();
				locallyVisited.add(id);
				HashMap linkers = getLinkers(id,false);//get outgoing links
				if (AL.empty(linkers))
					continue;
				for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
					String property = (String)lit.next();
					String reversal = inversions == null ? null : inversions.get(property);
					if (AL.empty(links) || Array.contains(links, property)){//check link property
						Linker l = (Linker)linkers.get(property);
						for (Iterator tit = l.keys().iterator(); tit.hasNext();){
							Object target = tit.next();
							//ignore reciprocal links assuming all links are bidirectional
							//if (!sources.contains(target)){
							if (!locallyVisited.contains(target) && !globallyVisited.contains(target) && (members == null || members.contains(target))){
								targets.add(target);
								Object o = l.get(target);
								Object from, link, to;
								if (reversal == null) {
									from = id; link = property; to = target;
								} else {
									from = target; link = reversal; to = id;									
								}
								if (o instanceof ComplexNumber[])
									collector.addValue(from, to, link, (ComplexNumber[])o);
								else
									collector.addValue(from, to, link, l.value(target).intValue());
							}
						}
					}
				}
			}
		}
		return collector;
	}
	
	public void normalize(){
		int maxVal = 0;
		//get max
		for (Iterator it = binders.keySet().iterator(); it.hasNext();){
			String source = (String)it.next();
			HashMap linkers = (HashMap)binders.get(source);
			for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
				String property = (String)lit.next();
				Linker linker = (Linker)linkers.get(property);
				for (Iterator tit = linker.keys().iterator(); tit.hasNext();){
					Object target = tit.next();
					int value = linker.value(target).intValue();
					if (maxVal < value)
						maxVal = value;
				}
			}
		}
		//normalize self
		for (Iterator it = binders.keySet().iterator(); it.hasNext();){
			String source = (String)it.next();
			HashMap linkers = (HashMap)binders.get(source);
			for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
				String property = (String)lit.next();
				Linker linker = (Linker)linkers.get(property);
				for (Iterator tit = linker.keys().iterator(); tit.hasNext();){
					Object target = tit.next();
					int value = linker.value(target).intValue();
					value = Math.round(((float)linker.value(target).intValue()) * 100 / maxVal);
					//TODO: fix hack!?
					((HashMap)linker).put(target, new Integer(value));
				}
			}
		}
		modified = true;
	}

	public Graph getSubgraph(int threshold){
		return getSubgraph(threshold,null,false);
	}
	
	public Graph getSubgraph(int threshold, String[] filter,boolean include){
		Graph g = new Graph();
		addSubgraphTo(g,threshold,filter,include);
		return g;
	}

	public void addSubgraph(Graph other){
		other.addSubgraphTo(this, 0, null, false);
	}
	
	public void addSubgraphTo(Graph other,int threshold, String[] filter,boolean include){
		Set set = toSet(filter);
		for (Iterator it = binders.keySet().iterator(); it.hasNext();){
			String source = (String)it.next();
			HashMap linkers = (HashMap)binders.get(source);
			for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
				String property = (String)lit.next();
				if (set!= null)
					if ((include && !set.contains(property)) || (!include && set.contains(property)))
						continue;
				Linker linker = (Linker)linkers.get(property);
				for (Iterator tit = linker.keys().iterator(); tit.hasNext();){
					Object target = tit.next();
					int value = linker.value(target).intValue();
					if (value >= threshold)
						other.addValue(source, target, property, value);
				}
			}
		}
	}

	public void blend(Graph other, double otherFactor){
		//iterate other, if no other value, leave this value unblended
		for (Iterator it = other.binders.keySet().iterator(); it.hasNext();){
			String source = (String)it.next();
			HashMap linkers = (HashMap)other.binders.get(source);
			for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
				String property = (String)lit.next();
				Linker linker = (Linker)linkers.get(property);
				for (Iterator tit = linker.keys().iterator(); tit.hasNext();){
					Object target = tit.next();
					Number otherNumber = linker.value(target);
					Number thisNumber = this.getValue(source, target, property);
					//if no this value, accept other value unblended, blend otherwise
					double value = thisNumber == null ? otherNumber.doubleValue()
							: otherNumber.doubleValue() * otherFactor + thisNumber.doubleValue() * (1 - otherFactor);
					addValue(source, target, property, new Double(value));
				}
			}
		}
	}
	
	Summator getNodeImportances(Set seeds, String[] links, boolean directed, boolean weighted, int iterations){
		final int REPUTATION_MAX_CYCLES = 100;//1000 - too long on huge graphs
		final double REPUTATION_STD_DEV = 0.001;
		if (iterations <= 0 || REPUTATION_MAX_CYCLES < iterations)
			iterations = REPUTATION_MAX_CYCLES;
		Summator orders = new Summator();
		//double stddev_top = 0; 
		double stddev_prev = 0;
		for (int pass = 0; pass < iterations; pass++){
			Summator new_orders = new Summator();
			for (Iterator it = binders.keySet().iterator(); it.hasNext();){
				String source = (String)it.next();
				HashMap linkers = (HashMap)binders.get(source);
				for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
					String property = (String)lit.next();
					if (AL.empty(links) || Array.contains(links, property)){//check link property
						Linker linker = (Linker)linkers.get(property);
						for (Iterator tit = linker.keys().iterator(); tit.hasNext();){
							Object target = tit.next();
							double weight = weighted ? linker.value(target).intValue() : 1;
							double def_val = 0.0;//1.0
							double s = seeds.contains(source) ? 100 : orders.value(source,def_val).doubleValue();
							double t = seeds.contains(target) ? 100 : orders.value(target,def_val).doubleValue();
							new_orders.count(target, weight * s, def_val);
							if (!directed)
								new_orders.count(source, weight * t, def_val);
						}
					}
				}
			}
			//TODO:counter_extend(new_orders,orders);//add low-rank orphans
			new_orders.normalize();//counter_normalize(new_orders);
			//if distribution of ranks is not changed since previous iteration, stop
			double stddev = new_orders.stddev(orders);
			if (stddev < REPUTATION_STD_DEV)
				break;
			//System.out.println(pass+ " "+stddev+" "+" "+stddev/(stddev_top));
			if (pass == 0) 
				;//stddev_top = stddev;
			else if (stddev > stddev_prev)//reach first local minimum
				break;
			stddev_prev = stddev;
			orders = new_orders;
		}
		return orders;
	}
	
	public Graph getTopConnectedNodesSubgraph(int nodelimit, int range, String[] ids){
		Set seeds = Array.toSet(ids);//these must be present
		Graph g = new Graph();
		
		//compute "symmetric weight-aware reputation" for the nodes
		Summator node_weights = getNodeImportances(seeds,null,false,true,range * 2);//undirected,weighted
		//sort nodes by order
		Object[][] ranked = node_weights.toRanked();
		if (ranked == null)
			return g;
		
		int limit = nodelimit;
		for (;;){
			//remove extra nodes exceeding limit allocating space for "a must" nodes
			for (int i = limit - seeds.size(); i < ranked.length; i++)
				node_weights.remove(ranked[i][0]);
			// select only the links involving top reputable nodes including selected nodes
			for (Iterator it = binders.keySet().iterator(); it.hasNext();){
				String source = (String)it.next();
				HashMap linkers = (HashMap)binders.get(source);
				for (Iterator lit = linkers.keySet().iterator(); lit.hasNext();){
					String property = (String)lit.next();
					Linker linker = (Linker)linkers.get(property);
					for (Iterator tit = linker.keys().iterator(); tit.hasNext();){
						Object target = tit.next();
						if (node_weights.containsKey(target) || node_weights.containsKey(source) ||
								seeds.contains(target) || seeds.contains(source)) 
							g.addValue(source, target, property, linker.value(target).intValue());
					}
				}
			}
			long size = g.size()[0];
			if (size <= nodelimit)//fitting limit
				break;
			limit = limit * 2 / 3;
			if (limit <= seeds.size())//can't compact more
				break;
			g = new Graph();//retry with smaller limit
		}
		return g;
	}
		
	public String toJSON(){
		//TODO:
		return "[]";
	}
	
	public boolean empty(){
		return binders.isEmpty();
	}
	
	/**
	 * @return array of two longs for counts of nodes and links in the graph (SLOW!!!)
	 */
	//TODO: lazy initialize and incrementally update transient variables!?
	public long[] size(){
		long links = 0;
		HashSet nodes = new HashSet();
		//Set contexts = binders.keySet(); 
		ArrayList contexts = new ArrayList(binders.keySet());//preventing ConcurrentModificationException 
		for (Iterator c = contexts.iterator(); c.hasNext();){
			Object context = c.next();
			nodes.add(context);
			HashMap linkers = getLinkers(context, false);
			if (!AL.empty(linkers)){
				Set properties = linkers.keySet();
				for (Iterator t = properties.iterator(); t.hasNext();){
					Object property = t.next();
					Linker linker = getLinker(context, property, false);
					if (linker != null){
						Set sources = linker.keys();
						if (!AL.empty(sources)){
							nodes.addAll(sources);
							links += sources.size();
						}
					}
				}
			}
		}
		return new long[]{nodes.size(),links};
	}
	
	public String toString(){
		return toString(null);
	}
	public String toString(Transcoder coder){
		final char termBreaker = ' ';
		final String statementBreaker = ".\n";
		StringBuilder sb = new StringBuilder();
		Set contexts = binders.keySet();
		ArrayList<String> res = new ArrayList<String>();
		for (Iterator c = contexts.iterator(); c.hasNext();){
			Object context = c.next();
			Object context_transcoded = coder != null ? coder.transcode(context) : context;
			HashMap linkers = getLinkers(context, false);
			if (!AL.empty(linkers)){
				Set properties = linkers.keySet();
				for (Iterator t = properties.iterator(); t.hasNext();){
					Object property = t.next();
					Linker linker = getLinker(context, property, false);
					if (linker != null){
						Set links = linker.keys();
						if (!AL.empty(links)){
							for (Iterator l = links.iterator(); l.hasNext();){
								Object target = l.next();
								//TODO: normalize!!!???
								Number value = linker.value(target);
								if (coder != null)
									target = coder.transcode(target);
								//tuna is fish 134.
								sb.setLength(0);
								Writer.quotequotable(sb, context_transcoded.toString())
									.append(termBreaker)
									.append(property).append(termBreaker);
								Writer.quotequotable(sb, target.toString())
									.append(termBreaker)
									.append(value);
								res.add(sb.toString());
							}
						}
					}
				}
			}
		}
		sb.setLength(0);
		Collections.sort(res);
		for (String r : res)
			sb.append(r).append(statementBreaker);
		return sb.toString();
	}
	
	//TODO: to Array
	public static Set toSet(String[] strings){
		if (AL.empty(strings))
			return null;
		HashSet set = new HashSet(strings.length);
		for (int i = 0; i < strings.length;i++)
			set.add(strings[i]);
		return set;
	}
	
	public ArrayList toList(boolean expand,String[] filter,boolean include){
		Set set = toSet(filter);
		ArrayList list = new ArrayList(); 
		Set contexts = binders.keySet(); 
		for (Iterator c = contexts.iterator(); c.hasNext();){
			Object context = c.next();
			HashMap linkers = getLinkers(context, false);
			if (!AL.empty(linkers)){
				Set properties = linkers.keySet();
				for (Iterator t = properties.iterator(); t.hasNext();){
					Object property = t.next();
					if (set!= null)
						if ((include && !set.contains(property)) || (!include && set.contains(property)))
							continue;
					Linker linker = getLinker(context, property, false);
					if (linker != null){
						Set links = linker.keys();
						if (!AL.empty(links)){
							for (Iterator l = links.iterator(); l.hasNext();){
								Object target = l.next();
								//TODO: normalize!!!???
								Object o = linker.get(target);//may be Number or ComplexNumber[]
								//tuna is fish 134. //if not a ComplexNumber[]
								if (o instanceof Number || !expand)
									list.add(new Object[]{context,property,target,o});
								else if (o instanceof ComplexNumber[]){
									ComplexNumber[] cn = (ComplexNumber[])o;
									for (int i = 0; i < cn.length; i++)
										list.add(cn[i].b == null ? new Object[]{context,property,target,cn[i].a}
												: new Object[]{context,property,target,cn[i].a,cn[i].b});
								}
							}
						}
					}
				}
			}
		}
		//context property target value
		return list;
	}
	
	//temporary hack to prevent Eclipse crashing on huge StringBuilder-s
	public void to(PrintStream out){
		final char termBreaker = ' ';
		final String statementBreaker = ".\n";
		Set contexts = binders.keySet(); 
		for (Iterator c = contexts.iterator(); c.hasNext();){
			Object context = c.next();
			HashMap linkers = getLinkers(context, false);
			if (!AL.empty(linkers)){
				Set targets = linkers.keySet();
				for (Iterator t = targets.iterator(); t.hasNext();){
					Object target = t.next();
					Linker linker = getLinker(context, target, false);
					if (linker != null){
						Set links = linker.keys();
						if (!AL.empty(links)){
							for (Iterator l = links.iterator(); l.hasNext();){
								Object property = l.next();
								//TODO: normalize!!!???
								Number value = linker.value(property);
								//tuna is 134 fish.
								out.print(context);
								out.print(termBreaker);
								out.print(target);
								out.print(termBreaker);
								out.print(value);
								out.print(termBreaker);
								out.print(property);
								out.print(statementBreaker);
							}
						}
					}
				}
			}
		}
	}
}
