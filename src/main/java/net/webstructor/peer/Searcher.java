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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Any;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.al.Time;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Property;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.data.Counter;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.Linker;
import net.webstructor.data.TextMiner;
import net.webstructor.data.Translator;
import net.webstructor.self.Siter;
import net.webstructor.util.Reporter;
import net.webstructor.util.Str;

//intent = template + help + action
abstract class Intenter {
	abstract boolean handle(Conversation conversation, final Storager storager,final Session session);
};

class Searcher extends Intenter {

//TODO: abastract reporter for xlsx/html/pdf!?
	String format(Conversation conversation, Session session, String topic, Seq q, String format, int limit, Collection filtered, String cluster, String[] graphs) {
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
				String[] texts = !AL.empty(cluster) ? Thing.toStrings(filtered,AL.text) : null;
				String[] cluster_texts = AL.text.equalsIgnoreCase(cluster) ? texts : Thing.toStrings(filtered,cluster);
				if (!AL.empty(texts))
//TODO configure clustering
					miner = new TextMiner(session.getBody(),session.getBody().languages,true).setDocuments(texts,cluster_texts).cluster(50,50,10);
				
				StringWriter writer = new StringWriter();
				Translator t = session.getBody().translator(language);
				Reporter rep = Reporter.reporter(session.getBody(),format,writer);
				//prepare graph headers optionally
				String header = AL.empty(graphs) || graphs.length < 2 ? null : "  <link rel=\"stylesheet\" href=\"/ui/jquery-ui-1.11.4.custom/jquery-ui.css\">\n" + 
						"  <script src=\"https://aigents.com/ui/jquery-1.11.1.js\"></script>\n" + 
						"  <script src=\"https://aigents.com/ui/jquery-ui-1.11.4.custom/jquery-ui.js\"></script>\n" + 
						"  <script type=\"text/javascript\" src=\"https://aigents.com/ui/aigents-al.js\"></script>\n" + 
						"  <link rel=\"stylesheet\" href=\"https://aigents.com/ui/aigents-graph.css\">\n" + 
						"  <script type=\"text/javascript\" src=\"https://aigents.com/ui/aigents-graph.js\"></script>\n";
						//"  <script type=\"text/javascript\" src=\"http://localtest.com/ui/aigents-graph.js\"></script>\n";
//TODO: since, until 
				rep.initReport(topic,Time.today(0),Time.today(0),header);
				if (miner != null) {
//TODO: localize reports
					Linker cats = miner.getCategoryCounts();
					if (cats != null && cats.size() > 0) {
						rep.table("categories",t.loc("Categories"),
								t.loc(new String[]{"Category","Number of items"}),
								cats.toData(),1,0);
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
//TODO: only if requested i "graphs"
						Map catdocs = miner.getCategoryDocuments();
						for (Object cat : catdocs.keySet()) {
							Linker docs_by_cat = (Linker)catdocs.get(cat);
							String cat_name = cat.toString();
//TODO: link values - to graph
							for (Object doc_id : docs_by_cat.keys()) {
								//hacky way to get Thing
								for (Object o : filtered) {
									Thing doc_thing = (Thing)o;
//TODO: use doc objects as keys in Miner instead of such brute force!!!
									if (doc_thing.getString(AL.text).equals(doc_id)) {
										for (int gr = 0; gr < graphs.length; gr++) if (!"category".equalsIgnoreCase(graphs[gr])) {
											String prop_name = graphs[gr];
											String doc_value = doc_thing.getString(prop_name);
											g.addValue(cat_name, doc_value, "category-"+prop_name, 1);
											g.addValue(doc_value, cat_name, prop_name+"-category", 1);
										}
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
								Object[][] ranked = linker.toRanked();
								if (!AL.empty(ranked)) for (int r = 0; r < ranked.length; r++) {
									Object item[] = ranked[r];
									writer.append("'"+k+"' "+gij+" '"+item[0]+"' "+item[1]+"\\n\\\n");
								}
								writer.append("'"+k+"' is '"+gi+"'\\n\\\n");
							}
						}
						writer.append("	\";\n");
						writer.append("GraphUI.request_graph_inline(\"svg_inline_"+stamp+"\", {text : graph_text, builder : function(text) {var config = {labeled_links:true};return GraphCustom.build_graph(text,{weighted:true,linktypes:null},config);}}, \"svg_widgets_"+stamp+"\", document.getElementById(\"wrapper_"+stamp+"\"));\n"); 
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
							rep.table(gij,t.loc(gij),new String[]{gi,gj},table,0,0);
						}
					}
				}
				rep.subtitle(t.loc("Data"));
				writer.append(conversation.format(format, session, peer, q, filtered));
				rep.closeReport();
				out = writer.toString();
			} else
				out = conversation.format(format, session, peer, q, filtered);
		}
		return out;
	}
	
	//TODO: members: Conversation conversation, Storager storager, Session session
	
	boolean handle(final Conversation conversation,final Storager storager,final Session session) {
		String[] args = session.args();
		if (AL.empty(args) || !args[0].equalsIgnoreCase("search"))
			return false;
		Thing peer = conversation.getSessionAreaPeer(session);
		Thing arg = new Thing();
		String retention = session.getBody().self().getString(Body.retention_period,"31");
		final String format = Str.arg(session.args(),"format", "text");
		final String cluster = Str.arg(session.args(),"cluster", AL.text);
		final String[] graphs = Str.get(args,"graph");
		final int limit = Integer.valueOf(Str.arg(args, "limit", "10")).intValue();
		final String time = Str.arg(session.args(),"time", "today");
		final Date date = Time.day(time);
		arg.set(AL.time, time);
		arg.set("scope", Str.arg(session.args(),"scope", "site"));//site|web|domain pattern?
		//search in URL
		if (session.read(new Seq(new Object[]{"search",new Property(arg,"thingname"),new Any(1,Conversation.in_site),new Property(arg,"url")}))) {
			String topic = arg.getString("thingname");
			final String site = arg.getString("url");
			{//if (AL.isURL(site)){
				Collection sites = storager.getNamed(site);
				if (AL.empty(sites))
					new Thing(site).store(storager);
				Collection topics = storager.getNamed(topic);
				if (AL.empty(topics))
					new Thing(topic).store(storager);
				session.read(arg,new String[]{"range","limit","minutes"},new String[]{"3","10","5"});
//TODO: return results even if nothing new - based on configuration!? 
				boolean found = session.getBody().act("read", arg);
				if (!found){//TODO get rid of this?
					session.output("Not.");
					return true;
				} else
				{//TODO regardless of success?				
					String out = null;
					try {
						String[] properties = "html".equals(format) ? new String[]{"sources","text","image"} : new String[]{"sources","text"};
						HashSet<String> set = Str.hset(properties);
						Property.collectVariableNames((net.webstructor.al.Set)Reader.pattern(storager,new Thing(),topic), set);
						properties = set.toArray(new String[]{});
						
						Seq q = new Seq(new Object[]{new All(new Object[]{new Seq(new Object[]{"is",topic}),new Seq(new Object[]{"times","today"})}),properties});
						//TODO: apply relevance
						//TODO: put found news in news feed if found
						//TODO: put searched topics and sites to the sites and things as untrusted for history
						Query.Filter filter = new Query.Filter(){
							public boolean passed(Thing thing) {
								Collection s = thing.getThings(AL.sources);
								if (!AL.empty(s) && 
									HttpFileReader.alignURL(site, ((Thing)s.iterator().next()).getName(), true) != null)
									return true;
								return false;
							}};
						Collection filtered = conversation.filter(session,peer,q,filter);
						if (AL.empty(filtered)) {
							q = new Seq(new Object[]{new Seq(new Object[]{"is",topic}),properties});
							filtered = conversation.filter(session,conversation.getSessionAreaPeer(session),q,filter);
						}
						out = format(conversation, session, topic, q, format, limit, filtered, cluster, graphs);
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
		if (session.read(new Seq(new Object[]{"search",new Property(arg,"thingname")}))) {
			session.read(arg,new String[]{"time","period"},new String[]{"today",retention});//search retention period by default
			final String topic = arg.getString("thingname");
			final int days = Integer.valueOf(arg.getString("period")).intValue();
			String[] properties = "html".equals(format) ? new String[]{"sources","text","image"} : new String[]{"sources","text"};
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
						session.output(format(conversation, session, topic, q, format, limit, clones, cluster, graphs));
						return true;
					}
					//if not found, extend for all texts and search in them with siter matcher
					q = new Seq(new Object[]{new Seq(new Object[]{"times",time}),new String[]{"text","is"}});
					//query for all texts
					Collection texts = new Query(session.sessioner.body,session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker).getThings(q,peer);
					if (!AL.empty(texts)){
						ArrayList res = new ArrayList();
						//StringBuilder summary = new StringBuilder();
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
						//flush final collection to out
						if (!AL.empty(res)){
							session.output(format(conversation, session, topic, q, format, limit, res, cluster, graphs));
							return true;
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
						//flush final collection to out ON the first day AND the first tie on matches
						if (!AL.empty(res)){
							session.output(format(conversation, session, topic, null, format, limit, res, cluster, graphs));
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
		Iter iter = new Iter(Parser.parse(text,null,false,true,true,false,null));
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
