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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

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
import net.webstructor.self.Siter;

//intent = template + help + action
abstract class Intenter {
	abstract boolean handle(Conversation conversation, final Storager storager,final Session session);
};

class Searcher extends Intenter {
	
	//TODO: members: Conversation conversation, Storager storager, Session session
	
	boolean handle(final Conversation conversation,final Storager storager,final Session session) {
		Thing peer = conversation.getSessionAreaPeer(session);
		Thing arg = new Thing();
		//search in URL
		if (session.read(new Seq(new Object[]{"search",new Property(arg,"thingname"),new Any(1,Conversation.in_site),new Property(arg,"url")}))) {
			String topic = arg.getString("thingname");
			final String site = arg.getString("url");
			if (AL.isURL(site)){
				Collection sites = storager.getNamed(site);
				if (AL.empty(sites))
					new Thing(site).store(storager);
				Collection topics = storager.getNamed(topic);
				if (AL.empty(topics))
					new Thing(topic).store(storager);
				session.read(arg,new String[]{"range","limit","minutes"},new String[]{"3","10","5"});			
				if (session.getBody().act("read", arg)){				
					String out;
					try {
						Seq q = new Seq(new Object[]{
									new All(new Object[]{new Seq(new Object[]{"is",topic}),new Seq(new Object[]{"times","today"})})
								,new String[]{"sources","text"}});
						//TODO: apply relevance
						// put found news in news feed if found
						// put searched topics and sites to the sites and things as untrusted for history
						Query.Filter filter = new Query.Filter(){
							public boolean passed(Thing thing) {
								Collection s = thing.getThings(AL.sources);
								if (!AL.empty(s) && 
									HttpFileReader.alignURL(site, ((Thing)s.iterator().next()).getName(), true) != null)
									return true;
								return false;
							}};
						out = conversation.answer(session,peer,q,filter);
						if (AL.empty(out)){
							q = new Seq(new Object[]{new Seq(new Object[]{"is",topic}),new String[]{"sources","text"}});
							out = conversation.answer(session,conversation.getSessionAreaPeer(session),q,filter);
						}
						session.output(!AL.empty(out) ? out : "Not.");
					} catch (Throwable e) {
						session.sessioner.body.error("Searcher "+session.input(), e);
						session.output("Not.");
					}
					return true;
				}
			}
			session.output("Not.");
			return true;
		} else
		//search in STM or LTM
		if (session.read(new Seq(new Object[]{"search",new Property(arg,"thingname")}))) {
			session.read(arg,new String[]{"time","period"},new String[]{"today","365"});//search entire year by default
			final String topic = arg.getString("thingname");
			int days = Integer.valueOf(arg.getString("period")).intValue();
			Date first = Time.day(arg.getString("time"));
			String out;
			//try searching STM first
			try {
				for (int daysback = 0; daysback <= days; daysback++){
					Date time = Time.date(first,-daysback);
					//first see for instances of topic
					Seq q = new Seq(new Object[]{
							new All(new Object[]{new Seq(new Object[]{"is",topic}),
							new Seq(new Object[]{"times",time})})
						,new String[]{"sources","text"}});
					out = conversation.answer(session,peer,q,null);
					if (!AL.empty(out)){
						session.output(out);
						return true;
					}
					//if not found, extend for all texts and search in them with siter matcher
					q = new Seq(new Object[]{new Seq(new Object[]{"times",time}),new String[]{"text","is"}});
					//query for all texts
					Collection texts = new Query(session.getStorager(),session.sessioner.body.self(),session.sessioner.body.thinker).getThings(q,peer);
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
							search(storager, source, text, topic, res);
						}
						//flush final collection to out
						if (!AL.empty(res)){
							session.output(conversation.format(session, peer, q, res));//TODO:q?!
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
					Date time = Time.date(first,-daysback);
					
					//2) get subgraph from www graph on 'worded'
					GraphCacher grapher = session.sessioner.body.sitecacher;
					Graph g = grapher.getGraph(time);
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
								System.out.println(count+" "+path);
								//TODO search
								String text = session.sessioner.body.archiver.get(path);
								if (AL.empty(text))
									session.sessioner.body.error("Searcher empty path "+path, null);
								else
									search(storager, path, text, topic, res);									
							}
						}
						//flush final collection to out ON the first day AND the first tie on matches
						if (!AL.empty(res)){
							session.output(conversation.format(session, peer, null, res));
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
	
	public boolean search(Storager storager, String source, String text, String topic, ArrayList res){
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
			res.add(new Thing(instance,new String[]{"text","sources"}));
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
