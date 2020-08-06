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
package net.webstructor.peer;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.al.Time;
import net.webstructor.al.Period;
import net.webstructor.al.Writer;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Property;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.data.Counter;
import net.webstructor.data.Emotioner;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.LangPack;
import net.webstructor.data.Linker;
import net.webstructor.data.ThingComparator;
import net.webstructor.data.TextMiner;
import net.webstructor.data.Translator;
import net.webstructor.self.Matcher;
import net.webstructor.self.Siter;
import net.webstructor.serp.Serper;
import net.webstructor.util.ArrayPositionComparator;
import net.webstructor.util.Reporter;
import net.webstructor.util.Str;

class Searcher implements Intenter {

	public static final String name = "search";
	
	@Override
	public String name() {
		return name;
	}
	
	protected Matcher matcher = null; 

//TODO: abstract reporter for xlsx/html/pdf!?
	String format(Session session, String topic, Seq q, String format, int limit, Collection filtered, String cluster, String[] graphs, boolean sentiment, Thing arg) {
		Thing peer = Responser.getSessionAreaPeer(session);
		String language = peer.getString(Peer.language);
		String out = null;
		if (!AL.empty(filtered)) {
			if (limit > 0 && filtered.size() > limit) {//cap the limit
				ArrayList limited = new ArrayList(limit);
				int i = 0;
				for (Object o : filtered) {
					limited.add(o);
					if (++i >= limit)
						break;
				}
				filtered = limited;
			}
			
			if (sentiment) {
				LangPack lo = session.getBody().languages;
				for (Object i : filtered) {
					Thing t = (Thing)i;
					String text = t.getString(AL.text);
					int[] s = lo.sentiment(text);
					t.setString("sentiment", Emotioner.emotion(s));
				}
			}
			
			if ("html".equalsIgnoreCase(format)) {
				
				TextMiner miner = null;
				Object[] texts = !AL.empty(cluster) ? filtered.toArray() : null;
				String[] cluster_texts = Thing.toStrings(filtered, cluster );
				if (!AL.empty(texts))
//TODO configure clustering
					miner = new TextMiner(session.getBody(),session.getBody().languages,false).setDocuments(texts,cluster_texts).cluster(50,50,10);
				
				StringWriter writer = new StringWriter();
				Translator t = session.getBody().translator(language);
				Reporter rep = Reporter.reporter(session.getBody(),format,writer);
//TODO: since, until 
				rep.initReport("Aigents Search Report: "+topic,Time.today(0),Time.today(0),session.sessioner.body.site());
				if (miner != null) {
//TODO: localize reports
					Linker cats = miner.getCategoryCounts();
					if (cats != null && cats.size() > 0) {
						Map catdocs = miner.getCategoryDocuments();
						for (Object cat : catdocs.keySet()) {
							Linker docs_by_cat = (Linker)catdocs.get(cat);
							String cat_name = cat.toString();
							for (Object doc : docs_by_cat.keys()) {
								Thing doc_thing = (Thing)doc;
//TODO: remove category1
								//String category = doc_thing.getString("category1");
								//doc_thing.setString("category1", category == null ? cat_name : category + "; " + cat_name);
								addObject(doc_thing,"category",cat_name);
							}
						}
//TODO: fix hack to populate "Unclassifeid" as "-"
						for (Object f : filtered) {
							Thing doc_thing = (Thing)f;
							String category = doc_thing.getString("category");
							if (category == null) {
								Linker l = (Linker)catdocs.get("-");
								if (l == null)
									catdocs.put("-", l = new Counter());
								l.count(doc_thing);
								cats.count("-");
//TODO: remove category1
								//doc_thing.setString("category1", "-");
								addObject(doc_thing,"category","-");
							}
						}
						Object[][] catdata = cats.toData();
						Arrays.sort(catdata,new ArrayPositionComparator(0));
						rep.table("categories",t.loc("Categories")+" ("+catdata.length+")",
								t.loc(new String[]{"Category","Number of items"}),
								catdata,1,0);
					}
					//build graph
					if (!AL.empty(graphs)) {
						Graph g = new Graph();
						StringBuilder graph_text = new StringBuilder(); 
					  
					  if (graphs.length == 1 && "category".equals(graphs[0])) {
						  Map<Object,Linker> doccats = miner.getDocumentCatNames();
							for (Linker l : doccats.values()){
								for (Object c1 : l.keys())
									for (Object c2 : l.keys())
										if (c1 != c2)
											g.addValue(c1, c2, "category-category", 1);
							}
					  }else 
					  if (graphs.length > 1){
						//use real properties
						for (Object o : filtered) {
							Thing thing = (Thing)o;
							for (int i = 0; i < graphs.length; i++) for (int j = 0; j < graphs.length; j++) if (i != j) {
								String gi = graphs[i];
								String gj = graphs[j];
								String gij = gi+"-"+gj;
								/*
								String si = thing.getString(gi);
								String sj = thing.getString(gj);
								if (!AL.empty(si) && !AL.empty(sj))
									g.addValue(si, sj, gij, 1);
									*/
								Collection ci = thing.getCollection(gi);
								Collection cj = thing.getCollection(gj);
								if (ci != null && cj != null) for (Object oi : ci) for (Object oj : cj) {
									String si = AL.toString(oi);
									String sj = AL.toString(oj);
									if (!AL.empty(si) && !AL.empty(sj))
										g.addValue(si, sj, gij, 1);
								}
							}
						}
						//add categories to graph
//TODO: only if requested in "graphs"
						Map catdocs = miner.getCategoryDocuments();
						for (Object cat : catdocs.keySet()) {
							Linker docs_by_cat = (Linker)catdocs.get(cat);
							String cat_name = cat.toString();
//TODO: link values - to graph
							for (Object doc : docs_by_cat.keys()) {
								Thing doc_thing = (Thing)doc;
								for (int gr = 0; gr < graphs.length; gr++) if (!"category".equalsIgnoreCase(graphs[gr])) {
									String prop_name = graphs[gr];
									String doc_value = doc_thing.getString(prop_name);
									if (!AL.empty(doc_value)) {
										g.addValue(cat_name, doc_value, "category-"+prop_name, 1);
										g.addValue(doc_value, cat_name, prop_name+"-category", 1);
									}
								}
							}
						}
					  }

						//graph to real graph
						if (graphs.length == 1 && "category".equals(graphs[0])) {
							toGraphText(graph_text,g.getPropertyLinkers("category-category"),"category-category","category");
						} else
						for (int i = 0; i < graphs.length; i++) for (int j = 0; j < graphs.length; j++) if (i != j) {
							String gi = graphs[i];
							String gj = graphs[j];
							String gij = gi+"-"+gj;
							/*HashMap subgraph = g.getPropertyLinkers(gij);
							for (Object k : subgraph.keySet()) {
								Linker linker = (Linker)subgraph.get(k);
								String source = k.toString().replace("\'", "\\\\\\\'");//var text = "'x y \\\'z' likes mary 10.0\n\
								Object[][] ranked = linker.toRanked();
								if (!AL.empty(ranked)) for (int r = 0; r < ranked.length; r++) {
									Object item[] = ranked[r];
									String target = item[0].toString().replace("\'", "\\\\\\\'");
									graph_text.append("\'"+source+"\' "+gij+" \'"+target+"\' "+item[1]+"\\n\\\n");
								}
								graph_text.append("\'"+source+"\' is '"+gi+"'\\n\\\n");
							}*/
							toGraphText(graph_text,g.getPropertyLinkers(gij),gij,gi);
						}

						StringBuilder colors = new StringBuilder();
						int linkcount = 0;
						for (int i = 0; i < graphs.length; i++) {
							if (i > 0) colors.append(",");
							colors.append(graphs[i]+":\""+Reporter.nodecolors[i % Reporter.nodecolors.length]+"\"");
							for (int j = 0; j < graphs.length; j++) if (i != j)
								colors.append(",\""+graphs[i]+"-"+graphs[j]+"\":\""+Reporter.linkcolors[linkcount++ % Reporter.linkcolors.length]+"\"");
						}
						rep.graph(String.valueOf(System.currentTimeMillis()),graph_text.toString(), colors.toString());

						//graph to tables
						for (int i = 0; i < graphs.length; i++) for (int j = 0; j < graphs.length; j++) if (i != j) {
							String gi = graphs[i];
							String gj = graphs[j];
							String gij = gi+"-"+gj;
							HashMap subgraph = g.getPropertyLinkers(gij);
							Object[][] table = new Object[subgraph.size()][];
							int r = 0;
							for (Object key : subgraph.keySet()) {
								Linker linker = (Linker)subgraph.get(key);
								Object[] row = new Object[2];
								row[0] = key;
								row[1] = linker.toRanked();
								table[r++] = row;
							}
							Arrays.sort(table,new ArrayPositionComparator(0));
							rep.table(gij,t.loc(gij) + " ("+table.length+")",new String[]{gi,gj},table,0,0);
						}
					}
				}
				List data = filtered instanceof List ? (List)filtered : new ArrayList(filtered); 
				rep.subtitle(t.loc("Data") + " ("+data.size()+")");
				Collections.sort(data,new ThingComparator(arg.getString("sort"),"asc".equals(arg.getString("order"))));
				writer.append(Responser.format(format, session, peer, q, data));
				rep.closeReport();
				out = writer.toString();
			} else
				out = Responser.format(format, session, peer, q, filtered);
		}
		return out;
	}
	
	//TODO move out
	private static boolean addObject(Thing thing, String name, Object object) {
		Object s = thing.get(name);
		if (object == null || (s != null && !(s instanceof java.util.Set)))
			return false;
		if (s == null)
			thing.set(name, s = new HashSet());
		((HashSet)s).add(object);
		return true;
	}
	
	private static void toGraphText(StringBuilder graph_text, HashMap subgraph, String gij, String gi) {
		for (Object k : subgraph.keySet()) {
			Linker linker = (Linker)subgraph.get(k);
			String source = k.toString().replace("\'", "\\\\\\\'");//var text = "'x y \\\'z' likes mary 10.0\n\
			Object[][] ranked = linker.toRanked();
			if (!AL.empty(ranked)) for (int r = 0; r < ranked.length; r++) {
				Object item[] = ranked[r];
				String target = item[0].toString().replace("\'", "\\\\\\\'");
				graph_text.append("\'"+source+"\' "+gij+" \'"+target+"\' "+item[1]+"\\n\\\n");
			}
			graph_text.append("\'"+source+"\' is '"+gi+"'\\n\\\n");
		}
	}

	@Override
	public boolean handleIntent(final Session session) {
		
		final String[] args = session.args();
		if (AL.empty(args) || args.length < 2 || !args[0].equalsIgnoreCase(name) || !session.authenticated())
			return false;
		
		if (session.read(new Seq(new Object[]{name,"results"})))
			if (session.status(name))
				return true;
//TODO default server configuration
		final long timeout_millis = Period.SECOND * Integer.valueOf(Str.arg(args, "timeout", "10")).intValue();
		Thread task = new Thread() {
	         public void run() {
	        	session.result( handleSearch(args,session) );
				session.complete(name);
	         };
	    };
	    return session.launch(name,task,timeout_millis);
	}
	
	Collection searchEngine(Session session, SearchContext sc) {
		ArrayList res = new ArrayList();
		Collection<Thing> rs;
		Serper s = session.sessioner.body.getSerper(sc.engine);
		if (s != null) {
			if ((rs = s.search(sc.type, sc.topic, null, sc.limit)) != null)
				return rs;
		} else if ("any".equals(sc.engine) || "all".equals(sc.engine)) {
			for (Serper sr : session.sessioner.body.getSerpers()) {
				if ((rs = sr.search(null, sc.topic, null, sc.limit)) != null)
					res.addAll(rs);
				if (!AL.empty(rs) && "any".equals(sc.engine))
					break;
			}
		}
		return res;
	}
	
	Collection searchSite(Session session, final SearchContext sc) {
		Storager storager = session.getStorager();
		Collection sites = storager.getNamed(sc.site);
		if (AL.empty(sites))
			new Thing(sc.site).store(storager);
		Collection topics = storager.getNamed(sc.topic);
		if (AL.empty(topics))
			new Thing(sc.topic).store(storager);
//TODO: return results even if nothing new - based on configuration!? 
		boolean found = session.getBody().act("read", sc.arg);
		session.sessioner.body.debug("Searcher found="+found);
		if (!found && sc.novelNew){//TODO get rid of this?
			session.sessioner.body.debug("Searcher return no.");
			return null;
		} else
		{//TODO regardless of success?				
			try {
				Seq q = new Seq(new Object[]{new All(new Object[]{new Seq(new Object[]{"is",sc.topic}),new Seq(new Object[]{"times","today"})}),sc.properties});
				//TODO: apply relevance
				//TODO: put found news in news feed if found
				//TODO: put searched topics and sites to the sites and things as untrusted for history
				Query.Filter filter = sc.scopeWeb || !AL.isURL(sc.site) ? null : new Query.Filter(){
					public boolean passed(Thing thing) {
						Collection s = thing.getThings(AL.sources);
						if (!AL.empty(s) && 
							HttpFileReader.alignURL(sc.site, ((Thing)s.iterator().next()).name(), true) != null)
							return true;
						return false;
					}};
				Collection filtered = Responser.queryFilter(session,sc.peer,q,filter);
				session.sessioner.body.debug("Searcher today filtered="+(filtered == null? 0 : filtered.size()));
				if (AL.empty(filtered)) {
					q = new Seq(new Object[]{new Seq(new Object[]{"is",sc.topic}),sc.properties});
					filtered = Responser.queryFilter(session,Responser.getSessionAreaPeer(session),q,filter);
					session.sessioner.body.debug("Searcher older filtered="+(filtered == null? 0 : filtered.size()));
				}
				return filtered;
			} catch (Throwable e) {
				session.sessioner.body.error("Searcher error site "+session.input(), e);
			}
		}
		return null;
	}

	Collection searchSTM(Session session, final SearchContext sc) {
		Collection res = new ArrayList();
		try {
			Storager storager = session.getStorager();
			for (int daysback = 0; daysback <= sc.days; daysback++){
				Date day = Time.date(sc.date,-daysback);
				//first see for instances of topic
				Seq q = new Seq(new Object[]{
						new All(new Object[]{new Seq(new Object[]{"is",sc.topic}),
						new Seq(new Object[]{"times",day})})
					,sc.properties});
				Collection tmp = Responser.queryFilter(session,sc.peer,q,null);
				if (!AL.empty(tmp))
					res.addAll(tmp);
				if (sc.limit > 0 && res.size() > sc.limit)
					return res;
				//if not found, extend for all texts and search in them with siter matcher
				q = new Seq(new Object[]{new Seq(new Object[]{"times",day}),new String[]{"text","is"}});
				//query for all texts
				Collection texts = new Query(session.sessioner.body,session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker).getThings(q,sc.peer);
				session.sessioner.body.debug("Searcher searching STM "+sc.topic+" "+day+" found "+(texts == null ? 0 : texts.size()));
				if (!AL.empty(texts)){
					//iterate over collection of texts
					for (Iterator it = texts.iterator(); it.hasNext();){
						Thing t = (Thing)it.next();
						String text = t.getString(AL.text);
						String source = t.getString(AL.is);
						String image = t.getString(AL.image);//may never work for site instances
						if (AL.empty(text) || AL.empty(source) || !AL.isURL(source))//check site instances only
							continue;
						//add all findings to resulting collection
						searchText(session, storager, source, image, text, sc.topic, res, sc.properties);
						if (sc.limit > 0 && res.size() > sc.limit)
							return res;
					}
				}
			}
		} catch (Throwable e) {
			session.sessioner.body.error("Searcher error STM "+session.input(), e);
		}
		return !AL.empty(res) ? res : null;
	}
	
	Collection searchLTM(Session session, final SearchContext sc) {
		Collection res = new ArrayList();
		if (session.sessioner.body.sitecacher != null) try {
			Storager storager = session.getStorager();
			//1) break pattern into words
			Seq patseq = Reader.pattern(storager,null,sc.topic);
			HashSet words = new HashSet();
			extractWords(patseq,words);
			
			for (int daysback = 0; daysback <= sc.days; daysback++){
				Date day = Time.date(sc.date,-daysback);
				//2) get subgraph from www graph on 'worded'
				GraphCacher grapher = session.sessioner.body.sitecacher;
				Graph g = grapher.getGraph(day);
				if (g.empty())
					continue;
				Counter indexed = new Counter();
				g.countTargets(words, null, indexed);
				
				//3) rank accordingly to N of matched words
				int max = 0;
				for (Iterator it = indexed.values().iterator(); it.hasNext();){
					Number number = (Number)it.next();
					if (max < number.intValue())
						max = number.intValue();
				}

				session.sessioner.body.debug("Searcher searching LTM "+sc.topic+" "+day+" max "+max);
				
				//4) search in every mathched url in is-text
				for (int matches = max; matches > 0; matches--){//go down, relaxing count of index matches gradually
					for (Iterator it = indexed.keys().iterator(); it.hasNext();){
						String path = (String)it.next();
						int count = ((Number)indexed.get(path)).intValue();
						if (count == matches){
							String text = session.sessioner.body.archiver.get(path);
							if (AL.empty(text))
								session.sessioner.body.error("Searcher empty path "+path, null);
							else
								searchText(session, storager, path, null, text, sc.topic, res, sc.properties);									
							session.sessioner.body.debug("Searcher checking LTM "+sc.topic+" "+day+" found "+res.size());
							//flush final collection to out ON the first day AND the first tie on matches
							if (sc.limit > 0 && res.size() > sc.limit)
								return res;
						}
					}
				}
			}
		} catch (Throwable e) {
			session.sessioner.body.error("Searcher error LTM "+session.input(), e);
		}
		return !AL.empty(res) ? res : null;
	}
	
	boolean handleSearch(final String[] args, final Session session) {
		Storager storager = session.getStorager();
		final SearchContext sc = new SearchContext(
				Str.arg(args,"search",null),
				session.getPeer(),
				Str.argLower(args,"engine", null),10);
		sc.site = Str.arg(args,Conversation.in_site, null);
		sc.arg = new Thing();
		sc.arg.set("thingname", sc.topic);//redundancy!?
		sc.arg.set("url", sc.site);//redundancy!?
		sc.format = Str.argLower(args,"format", "text");
		sc.cluster = Str.arg(args,"cluster", AL.text);
		sc.graphs = Str.get(args,"graph");
		sc.type = Str.argLower(args,"type", "text");//text|image|video
		String time = Str.argLower(args,"time", "today");
		String novelty = Str.argLower(args,"novelty", "all");//new|all
		String scope = Str.argLower(args,"scope", "site");//site|web|domain pattern?
		sc.date = Time.day(time);
		sc.sentiment = Str.has(args,"sentiment");
		sc.arg.set(AL.time, time);
		sc.arg.set("scope", scope);
		String default_period = "3";//session.getBody().self().getString(Body.retention_period,"31");//search retention period by default
		session.readArgs(sc.arg,new String[]{"period","range","limit","minutes"},new String[]{default_period,"2","10","10"});
		session.readArgs(sc.arg,new String[]{"mode","sort","order"},new String[]{"smart","text","asc"});//smart|track|find,text|category|...,asc|desc
		sc.days = Integer.valueOf(sc.arg.getString("period")).intValue();
		sc.limit = Integer.valueOf(sc.arg.getString("limit")).intValue();
		
		sc.novelNew = novelty.equals("new");
		sc.scopeWeb = scope.equals("web");

		HashSet<String> set = Str.hset("html".equals(sc.format) ? new String[]{"sources","text","image"} : new String[]{"sources","text"});
		Property.collectVariableNames((net.webstructor.al.Set)Reader.pattern(storager,new Thing(),sc.topic), set);
		sc.properties = set.toArray(new String[]{});

		session.sessioner.body.debug("Searcher args "+Writer.toString(session.args()));
		session.sessioner.body.debug("Searcher arg "+sc.arg);
		session.sessioner.body.debug("Searcher start novelNew="+sc.novelNew+" scopeWeb="+sc.scopeWeb);

		Collection res = null;

		if (AL.empty(sc.topic))
			return false;
		else
		if (!AL.empty(sc.engine)) {
			if (!Peer.paid(sc.peer))
				session.output("Not subscribed.");
			else
				//TODO:consider engine site searh as well: if (!AL.empty(topic) && !AL.empty(site))
				res = searchEngine(session, sc);
		} else
		if (!AL.empty(sc.topic) && !AL.empty(sc.site)) {
			res = searchSite(session, sc);
		} else {
			res = searchSTM(session, sc);
			if (AL.empty(res))
				res = searchLTM(session, sc);
		}
		if (!AL.empty(res)){
//TODO: get rid of dummy q used only for presentation purposes "there ..."!!!???
			Seq stupiddummyquery = new Seq(new Object[]{new All(new Object[]{new Seq(new Object[]{"is",sc.topic})}),sc.properties});
			session.output(format(session, sc.topic, stupiddummyquery, sc.format, sc.limit, res, sc.cluster, sc.graphs, sc.sentiment, sc.arg));
		} else
			session.output(session.no());
		return true;
	}
	
	public boolean searchText(Session session, Storager storager, String source, String image, String text, String topic, Collection res, String[] properties){
		synchronized (this) {//TODO fix this lazy init hack
			if (matcher == null)
				matcher = session.sessioner.body.getMatcher();
		}
		StringBuilder summary = new StringBuilder();
		Iter iter = new Iter(Parser.parse(text,null,false,true,true,false,Siter.punctuation,null));
		boolean found = false;
		for (;;){
			summary.setLength(0);
			Thing instance = new Thing();
			Seq patseq = Reader.pattern(storager,instance,topic);
			if (!matcher.readAutoPatterns(iter,patseq,instance,summary))
				break;
			instance.setString(AL.text, summary.toString());
			//TODO:unhack the hack, making sources as text!?
			instance.setString(AL.sources,source);
			if (!AL.empty(image))
				instance.setString(AL.image,image);
			res.add(new Thing(instance,properties));
			found = true;
		}
		return found;
	}
	
	void extractWords(Set patseq,java.util.Set words){
		for (int i = 0; i < patseq.size(); i++){
			Object o = patseq.get(i);
			if (o instanceof Set)
				 extractWords((Set)o,words);
			else
			if (o instanceof String)
				words.add(o);
		}
	}
};
