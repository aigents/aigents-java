/*
 * MIT License
 * 
 * Copyright (c) 2005-2019 by Anton Kolonin, Aigents
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
import net.webstructor.data.GraphCacher;
import net.webstructor.data.Linker;
import net.webstructor.data.ThingComparator;
import net.webstructor.data.TextMiner;
import net.webstructor.data.Translator;
import net.webstructor.self.Siter;
import net.webstructor.util.ArrayPositionComparator;
import net.webstructor.util.Reporter;
import net.webstructor.util.Str;

//intent = template + help + action
abstract class Intenter {
	abstract boolean handle(Conversation conversation, final Storager storager,final Session session);
};

class Searcher extends Intenter {

	public static final String name = "search";

	static final String[] nodecolors = {"#FFFF00","#00FF00","#00FFFF","#FF00FF","#FF0000","#0000FF"};
	static final String[] linkcolors = {"#00007F","#007F7F","#007F00","#7F7F00","#7F0000","#7F007F"};
	
//TODO: abastract reporter for xlsx/html/pdf!?
	String format(Conversation conversation, Session session, String topic, Seq q, String format, int limit, Collection filtered, String cluster, String[] graphs, Thing arg) {
		Thing peer = conversation.getSessionAreaPeer(session);
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
				//prepare graph headers optionally
				String base = session.sessioner.body.site();
				String header = AL.empty(graphs) || graphs.length < 2 ? null : 
						"  <link rel=\"stylesheet\" href=\""+base+"/ui/jquery-ui-1.11.4.custom/jquery-ui.css\">\n" + 
						"  <link rel=\"stylesheet\" href=\""+base+"/ui/aigents-wui.css\">\n" + 
						"  <script src=\""+base+"/ui/jquery-1.11.1.js\"></script>\n" + 
						"  <script src=\""+base+"/ui/jquery-ui-1.11.4.custom/jquery-ui.js\"></script>\n" + 
						"  <script type=\"text/javascript\" src=\""+base+"/ui/aigents-al.js\"></script>\n" + 
						"  <script type=\"text/javascript\" src=\""+base+"/ui/aigents-graph.js\"></script>\n";
//TODO: since, until 
				rep.initReport("Aigents Search Report: "+topic,Time.today(0),Time.today(0),header);
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
								String category = doc_thing.getString("category");
								doc_thing.setString("category", category == null ? cat_name : category + "; " + cat_name);
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
								doc_thing.setString("category", "-");
							}
						}
						Object[][] catdata = cats.toData();
						Arrays.sort(catdata,new ArrayPositionComparator(0));
						rep.table("categories",t.loc("Categories")+" ("+catdata.length+")",
								t.loc(new String[]{"Category","Number of items"}),
								catdata,1,0);
					}
					//build graph
					if (!AL.empty(graphs) && graphs.length > 1) {
						Graph g = new Graph();
						//use real properties
						for (Object o : filtered) {
							Thing thing = (Thing)o;
							for (int i = 0; i < graphs.length; i++) for (int j = 0; j < graphs.length; j++) if (i != j) {
								String gi = graphs[i];
								String gj = graphs[j];
								String gij = gi+"-"+gj;
								String si = thing.getString(gi);
								String sj = thing.getString(gj);
								if (!AL.empty(si) && !AL.empty(sj))
									g.addValue(si, sj, gij, 1);
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

						//graph to real graph
						long stamp = System.currentTimeMillis();//temp id
						writer.append("<br><div id=\"wrapper_"+stamp+"\" style=\"width:100%;height:100%\"/>");
						writer.append("<script>\n");
						writer.append("var graph_text = \"\\\n");
						for (int i = 0; i < graphs.length; i++) for (int j = 0; j < graphs.length; j++) if (i != j) {
							String gi = graphs[i];
							String gj = graphs[j];
							String gij = gi+"-"+gj;
							HashMap subgraph = g.getPropertyLinkers(gij);
							for (Object k : subgraph.keySet()) {
								Linker linker = (Linker)subgraph.get(k);
								String source = k.toString().replace("\'", "\\\\\\\'");//var text = "'x y \\\'z' likes mary 10.0\n\
								Object[][] ranked = linker.toRanked();
								if (!AL.empty(ranked)) for (int r = 0; r < ranked.length; r++) {
									Object item[] = ranked[r];
									String target = item[0].toString().replace("\'", "\\\\\\\'");
									writer.append("\'"+source+"\' "+gij+" \'"+target+"\' "+item[1]+"\\n\\\n");
								}
								writer.append("\'"+source+"\' is '"+gi+"'\\n\\\n");
							}
						}
						writer.append("	\";\n");
						StringBuilder colors = new StringBuilder();
						int linkcount = 0;
						for (int i = 0; i < graphs.length; i++) {
							if (i > 0) colors.append(",");
							colors.append(graphs[i]+":\""+nodecolors[i % nodecolors.length]+"\"");
							for (int j = 0; j < graphs.length; j++) if (i != j)
								colors.append(",\""+graphs[i]+"-"+graphs[j]+"\":\""+linkcolors[linkcount++ % linkcolors.length]+"\"");
						}
						writer.append("GraphUI.request_graph_inline(\"svg_inline_"+stamp+"\", {text : graph_text, builder : function(text) {var config = {colors:{"+colors+"},labeled_links:true};return GraphCustom.build_graph(text,{weighted:true,linktypes:null},config);}}, \"svg_widgets_"+stamp+"\", document.getElementById(\"wrapper_"+stamp+"\"));\n"); 
						writer.append("</script><br>");

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
				writer.append(conversation.format(format, session, peer, q, data));
				rep.closeReport();
				out = writer.toString();
			} else
				out = conversation.format(format, session, peer, q, filtered);
		}
		return out;
	}
	
	boolean handle(final Conversation conversation,final Storager storager,final Session session) {
		final String[] args = session.args();
		if (AL.empty(args) || args.length < 2 || !args[0].equalsIgnoreCase(name))
			return false;
		if (session.read(new Seq(new Object[]{name,"results"})))
			if (session.status(name))
				return true;
//TODO default server configuration
		final long timeout_millis = Period.SECOND * Integer.valueOf(Str.arg(args, "timeout", "10")).intValue();
		Thread task = new Thread() {
	         public void run() {
	        	session.result( handleTimed(args,conversation,storager,session) );
				session.complete(name);
	         };
	    };
	    return session.launch(name,task,timeout_millis);
	}
	
	//TODO: members: Conversation conversation, Storager storager, Session session
	
	boolean handleTimed(final String[] args, final Conversation conversation,final Storager storager,final Session session) {
		Thing peer = conversation.getSessionAreaPeer(session);
		Thing arg = new Thing();
		final String topic = Str.arg(args,"search",null);
		final String site = Str.arg(args,Conversation.in_site, null);
		arg.set("thingname", topic);
		arg.set("url", site);
		final String format = Str.arg(args,"format", "text").toLowerCase();
		final String cluster = Str.arg(args,"cluster", AL.text);
		final String[] graphs = Str.get(args,"graph");
		final String time = Str.arg(args,"time", "today").toLowerCase();
		final String novelty = Str.arg(args,"novelty", "all").toLowerCase();//new|all
		final String scope = Str.arg(session.args(),"scope", "site").toLowerCase();//site|web|domain pattern?
		final Date date = Time.day(time);
		arg.set(AL.time, time);
		arg.set("scope", scope);
		String default_period = "3";//session.getBody().self().getString(Body.retention_period,"31");//search retention period by default
		session.readArgs(arg,new String[]{"period","range","limit","minutes"},new String[]{default_period,"2","100","10"});
		session.readArgs(arg,new String[]{"mode","sort","order"},new String[]{"smart","text","asc"});//smart|track|find,text|category|...,asc|desc
		final int days = Integer.valueOf(arg.getString("period")).intValue();
		final int limit = Integer.valueOf(arg.getString("limit")).intValue();
		
		boolean novelNew = novelty.equals("new");
		boolean scopeWeb = scope.equals("web");
		String[] properties = "html".equals(format) ? new String[]{"sources","text","image"} : new String[]{"sources","text"};

		session.sessioner.body.debug("Searcher args "+Writer.toString(session.args()));
		session.sessioner.body.debug("Searcher arg "+arg);
		
		session.sessioner.body.debug("Searcher start novelNew="+novelNew+" scopeWeb="+scopeWeb);
		
		//search in URL
		//if (session.read(new Seq(new Object[]{"search",new Property(arg,"thingname"),new Any(1,Conversation.in_site),new Property(arg,"url")}))) {
		if (!AL.empty(topic) && !AL.empty(site)) {
			//final String topic = arg.getString("thingname");
			//final String site = arg.getString("url");
			{//if (AL.isURL(site)){
				Collection sites = storager.getNamed(site);
				if (AL.empty(sites))
					new Thing(site).store(storager);
				Collection topics = storager.getNamed(topic);
				if (AL.empty(topics))
					new Thing(topic).store(storager);
//TODO: return results even if nothing new - based on configuration!? 
				boolean found = session.getBody().act("read", arg);
				session.sessioner.body.debug("Searcher found="+found);
				if (!found && novelNew){//TODO get rid of this?
					session.sessioner.body.debug("Searcher return not");
					session.output("Not.");
					return true;
				} else
				{//TODO regardless of success?				
					String out = null;
					try {
						HashSet<String> set = Str.hset(properties);
						Property.collectVariableNames((net.webstructor.al.Set)Reader.pattern(storager,new Thing(),topic), set);
						properties = set.toArray(new String[]{});
						Seq q = new Seq(new Object[]{new All(new Object[]{new Seq(new Object[]{"is",topic}),new Seq(new Object[]{"times","today"})}),properties});
						//TODO: apply relevance
						//TODO: put found news in news feed if found
						//TODO: put searched topics and sites to the sites and things as untrusted for history
						Query.Filter filter = scopeWeb || !AL.isURL(site) ? null : new Query.Filter(){
							public boolean passed(Thing thing) {
								Collection s = thing.getThings(AL.sources);
								if (!AL.empty(s) && 
									HttpFileReader.alignURL(site, ((Thing)s.iterator().next()).getName(), true) != null)
									return true;
								return false;
							}};
						Collection filtered = conversation.filter(session,peer,q,filter);
						session.sessioner.body.debug("Searcher today filtered="+(filtered == null? 0 : filtered.size()));
						if (AL.empty(filtered)) {
							q = new Seq(new Object[]{new Seq(new Object[]{"is",topic}),properties});
							filtered = conversation.filter(session,conversation.getSessionAreaPeer(session),q,filter);
							session.sessioner.body.debug("Searcher older filtered="+(filtered == null? 0 : filtered.size()));
						}
						out = format(conversation, session, topic, q, format, limit, filtered, cluster, graphs, arg);
						session.output(!AL.empty(out) ? out : "Not.");
					} catch (Throwable e) {
						session.sessioner.body.error("Searcher "+session.input(), e);
						session.output("Not.");
					}
					return true;
				}
			}
		} else
		//search in STM or LTM
		//if (session.read(new Seq(new Object[]{"search",new Property(arg,"thingname")}))) {
		if (!AL.empty(topic)) {
			//final String topic = arg.getString("thingname");
			HashSet<String> set = Str.hset(properties);
			Property.collectVariableNames((net.webstructor.al.Set)Reader.pattern(storager,new Thing(),topic), set);
			properties = set.toArray(new String[]{});
			//try searching STM first
			try {
				for (int daysback = 0; daysback <= days; daysback++){
					Date day = Time.date(date,-daysback);
					//first see for instances of topic
					Seq q = new Seq(new Object[]{
							new All(new Object[]{new Seq(new Object[]{"is",topic}),
							new Seq(new Object[]{"times",day})})
						,properties});
					Collection clones = conversation.filter(session,peer,q,null);
					if (!AL.empty(clones)){
						session.output(format(conversation, session, topic, q, format, limit, clones, cluster, graphs, arg));
						return true;
					}
					//if not found, extend for all texts and search in them with siter matcher
					q = new Seq(new Object[]{new Seq(new Object[]{"times",day}),new String[]{"text","is"}});
					//query for all texts
					Collection texts = new Query(session.sessioner.body,session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker).getThings(q,peer);
					session.sessioner.body.debug("Searcher "+topic+" STM "+day+" found "+(texts == null ? 0 : texts.size()));
					if (!AL.empty(texts)){
						ArrayList res = new ArrayList();
						//iterate over collection of texts
						for (Iterator it = texts.iterator(); it.hasNext();){
							Thing t = (Thing)it.next();
							String text = t.getString(AL.text);
							String source = t.getString(AL.is);
							if (AL.empty(text) || AL.empty(source) || !AL.isURL(source))//check site instances only
								continue;
							//add all findings to resulting collection
							search(storager, source, text, topic, res, properties);
							if (limit > 0 && res.size() > limit)
								break;
						}
//TODO: fill up to the limit
						//flush final collection to out
						if (!AL.empty(res)){
							session.output(format(conversation, session, topic, q, format, limit, res, cluster, graphs, arg));
							return true;
						}else {
							;
						}
					}
				}
			} catch (Throwable e) {
				session.sessioner.body.error("Searcher "+session.input(), e);
			}
			
			//if not found in STM, try seach in LTM:
			if (session.sessioner.body.sitecacher != null){
				//1) break pattern into words
				Seq patseq = Reader.pattern(storager,null,topic);
				HashSet words = new HashSet();
				extractWords(patseq,words);
				
				for (int daysback = 0; daysback <= days; daysback++){
					Date day = Time.date(date,-daysback);
					//2) get subgraph from www graph on 'worded'
					GraphCacher grapher = session.sessioner.body.sitecacher;
					Graph g = grapher.getGraph(day);
					Counter indexed = new Counter();
					g.countTargets(words, null, indexed);
					
					//3) rank accordingly to N of matched words
					int max = 0;
					for (Iterator it = indexed.values().iterator(); it.hasNext();){
						Number number = (Number)it.next();
						if (max < number.intValue())
							max = number.intValue();
					}

					session.sessioner.body.debug("Searcher "+topic+" LTM "+day+" max "+max);
					
					//4) search in every mathched url in is-text
					ArrayList res = new ArrayList();
					for (int matches = max; matches > 0; matches--){//go down, relaxing count of index matches gradually
						for (Iterator it = indexed.keys().iterator(); it.hasNext();){
							String path = (String)it.next();
							int count = ((Number)indexed.get(path)).intValue();
							if (count == matches){
								//TODO search
								String text = session.sessioner.body.archiver.get(path);
								if (AL.empty(text))
									session.sessioner.body.error("Searcher empty path "+path, null);
								else
									search(storager, path, text, topic, res, properties);									
								if (limit > 0 && res.size() > limit)
									break;
							}
						}
						session.sessioner.body.debug("Searcher "+topic+" LTM "+day+" found "+res.size());
//TODO: fill up to the limit
						//flush final collection to out ON the first day AND the first tie on matches
						if (!AL.empty(res)){
							session.output(format(conversation, session, topic, null, format, limit, res, cluster, graphs, arg));
							return true;
						}
					}
				}
			}
			//if not handled above
			session.output("Not.");
			return true;
		}
		return false;
	}
	
	public boolean search(Storager storager, String source, String text, String topic, ArrayList res, String[] properties){
		StringBuilder summary = new StringBuilder();
		Iter iter = new Iter(Parser.parse(text,null,false,true,true,false,Siter.punctuation,null));
		boolean found = false;
		for (;;){
			summary.setLength(0);
			Thing instance = new Thing();
			Seq patseq = Reader.pattern(storager,instance,topic);
			if (!Siter.readAutoPatterns(storager,iter,patseq,instance,summary))
				break;
			instance.setString(AL.text, summary.toString());
			//TODO:unhack the hack, making sources as text!?
			instance.setString("sources",source);
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
