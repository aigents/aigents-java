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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Parser;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.Emailer;
import net.webstructor.comm.SocialCacher;
import net.webstructor.comm.Socializer;
import net.webstructor.comm.fb.FB;
import net.webstructor.comm.vk.VK;
import net.webstructor.comm.goog.GApi;
import net.webstructor.core.Mistake;
import net.webstructor.core.Property;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.LangPack;
import net.webstructor.data.ReputationSystem;
import net.webstructor.data.TextMiner;
import net.webstructor.data.Transcoder;
import net.webstructor.self.Self;
import net.webstructor.self.Streamer;
import net.webstructor.util.Array;
import net.webstructor.util.Str;

public class Conversation extends Responser {
	
	public static final String[] spider = new String[] {"spider","spidering","crawl","crawling"};
	public static final String[] logout = new String[] {"logout","bye","cancel","stop"};
	public static final String[] login = new String[] {"login","relogin","start","restart"};
	public static final String[] in_site = new String[] {"site","in","url"};
	
	private String spidering(Session session, String name, String id) {
		String report = null;
		Socializer provider = session.sessioner.body.getSocializer(name);
		if (provider != null && (provider.opendata() || session.trusted()))
		try {
			Collection peers;
			peers = session.sessioner.body.storager.getByName(name+" id", id);
			if (!AL.empty(peers)){
				session.sessioner.body.debug("Spidering peer manually "+name+" "+id+" started");
				Thing peer = (Thing)peers.iterator().next();
				final Profiler fb_profiler = new Profiler(session.sessioner.body,
						provider,peer,
						name+" id",name+" token",name+" key");
				report = fb_profiler.profile(peer,true);//fresh
				session.sessioner.body.debug("Spidering peer manually "+name+" "+id+" completed");
			}
		} catch (Exception e) {
			session.sessioner.body.error("Spidering peer manually "+name+" "+id+" error",e);
		}
		return report != null ? report : session.no();
	}
	
	public boolean process(Session session) {
	  try {
		Storager storager = session.sessioner.body.storager;
		/*
		//TODO: with fixing bunch of unit tests
		//if logged peer is no longer valid, logout 
		if (session.authenticated && session.getStoredPeer() == null){
			session.authenticated = false;
			session.peer = null;
		}*/
		//update "session properties" in "proxy peer"
		if (!session.authenticated() && setProperties(session))
			return false;
		if (session.peer == null) { // not authenticated, but in the process
			session.responser = new Login();
			return true;
		} else
		if (session.read(Reader.pattern(AL.i_my,logout)) || Array.contains(logout, session.input().toLowerCase())){//can logout at any point			
			session.output(session.ok());
			session.responser = new Login();
			session.logout();
			return false;			
		} else
//TODO: start
		if (session.argsCount() == 1 && Array.contains(login, session.args()[0])){//login/relogin/start/restart
			session.output(session.ok());
			session.responser = new Login();
			session.logout();
			return true;//move on login process
		} else
		if (!session.authenticated() && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,new String[] {"secret question","secret answer"}))){
			session.responser = new VerificationChange();
			return true;			
		} else
		if (session.authenticated()) {
			return doAuthenticated(storager,session);
		} else {
			session.responser = new Login();//TODO: really, just back to login?
			return true;
		}
	  } catch (Throwable e) {
		  session.output(session.no()+" "+Mistake.message(e));
		  if (e instanceof Mistake)
			  session.sessioner.body.error("Conversation error "+(session.peer != null ? session.peer.getTitle(Peer.title_email) : "null")+": "+session.input(), e);
		  return false;
	  }
	}

	//TODO: move all that stuff to other place - social networks separately, root separately, etc.
	private boolean doAuthenticated(Storager storager,Session session){
		Thing reader = new Thing();
		Thing curPeer = session.getStoredPeer();
		if (curPeer != null)//TODO:cleanup as it is just in case for post-mortem peer operations in tests
			curPeer.set(Peer.activity_time,Time.day(Time.today));

//TODO cleanup hack		
		/*if ("debug".equals(session.input())) {
			try {
				Collection iss = session.getStorager().getByName(AL.name, "restraunt");
				if (!AL.empty(iss)) {
					Collection instances = session.getStorager().getByName(AL.is, iss.iterator().next());
					Collection trusts = curPeer.getThings(AL.trusts);
					java.util.HashSet ts = new java.util.HashSet(instances);
					ts.retainAll(trusts);
					session.output(session.ok());
					return false;		
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			session.output(session.no());
			return false;		
		}*/
		
		VK vk = (VK)session.sessioner.body.getSocializer("vkontakte");//TODO: validate type or make type-less 
		if (session.mood == AL.interrogation) {
			return answer(session);
		} else
		//binding Social Network accounts
		//TODO: refactor for unification
		//TODO: ensure no id stealing happens (re-steal, if so)
		if (vk != null && session.input().indexOf("vkontakte_id=")==0){
			//update VK login upon callback
			String ensti[] = vk.verifyRedirect(session.input());
			String ok = ensti == null ? null : session.bindAuthenticated(Body.vkontakte_id, ensti[4],Body.vkontakte_token, ensti[3]);
			if (!ok.equals(session.ok()))//TODO: fix hack!?
				ensti = null;
			//TODO:restrict origin for better security if passing token?
			session.output("<html><body onload=\"top.postMessage(\'"+(ensti == null ? "Error:" : "Success:")+session.input()+"\',\'*\');top.window.vkontakteLoginComplete();\"></body></html>");
			return false;		
		}else
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.vkontakte_id,Body.vkontakte_token},256))) {
			String id = session.peer.getString(Body.vkontakte_id);
			String token = session.peer.getString(Body.vkontakte_token);
			String enst[];
			if (vk != null && ((enst = vk.verifyToken(id, token)) != null)) {
				session.output(session.bindAuthenticated(Body.vkontakte_id, id,Body.vkontakte_token, enst[3]));
			} else
				session.output(session.no());
			return false;		
		} else
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.google_id,Body.google_token},256))) {
			String id = session.peer.getString(Body.google_id);
			String token = session.peer.getString(Body.google_token);
			String enstir[];//email,name,surname,token,id,refresh_token
			GApi gapi = (GApi)session.sessioner.body.getSocializer("google");//TODO:validate type or make typeless
			if (gapi != null && ((enstir = gapi.verifyToken(id, token)) != null)) {
				id = enstir[4];
				session.output(session.bindAuthenticated(Body.google_id, id, Body.google_token, enstir[3]));
				if (session.ok().equals(session.output())) {//TODO: fix hack!?
					session.getStoredPeer().set(Body.google_id,id);//TODO:straighten
					if (!AL.empty(enstir[5]))//refresh_token as google_key
						session.getStoredPeer().set(Body.google_key, enstir[5]);
				}
			} else
				session.output(session.no());
			return false;		
		} else
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.facebook_id,Body.facebook_token},256))) {
			String id = session.peer.getString(Body.facebook_id);
			String token = session.peer.getString(Body.facebook_token);
			FB fb = (FB)session.sessioner.body.getSocializer("facebook");//TODO: validate type or make typeless
			if (fb != null && (token = fb.verifyToken(id, token)) != null) {
				session.output(session.bindAuthenticated(Body.facebook_id, id,Body.facebook_token, token));
			} else
				session.output(session.no());
			return false;		
		} else
			
			
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,new String[] {"email","e-mail"},"/.+@.+/"))) {
			session.responser = new EmailChange();
			return true;			
		} else 
		if (session.input().length() == 0) { //repeated authentication seed for pre-authenticated session
			session.output(session.ok());
			return false;			
		} else
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,new String[] {"forget","forgetting"}))) {
			Self.clear(session.sessioner.body,Schema.foundation);
			if (session.read(Reader.pattern(AL.you,new String[] {"forget everything","forget all"}))){
				session.sessioner.body.archiver.clear();
				if (session.sessioner.body.sitecacher != null)
					session.sessioner.body.sitecacher.clear(true);//clear with LTM
				if (session.sessioner.body.filecacher != null)
					session.sessioner.body.filecacher.clear(true,null);//clear cache entirely 
			}
			session.output(session.ok());
			return false;
		} else
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,new String[] {"think","thinking"}))) {
			Peer.rethink(session.sessioner.body,session.getStoredPeer());//this does think along the way!
			session.output(session.ok());
			return false;			
		} else
			
		if ((session.mood == AL.direction)// || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,spider))) {
			Thing task = new Thing();
			session.output(session.no());
			if (session.read(new Seq(new Object[]{
					new Any(1,AL.you),"spidering",new Property(task,"network"),
					"id",new Property(task,"id")
					}))) {
				session.output(spidering(session, task.getString("network"), task.getString("id")));
			} else
			if (session.read(new Seq(new Object[]{
						new Any(1,AL.you),new Any(1,spider),new Property(task,"network")}))) {
				Socializer provider = session.sessioner.body.getSocializer(task.getString("network"));
				if (provider != null){
					Thing arg = new Thing();
					session.read(arg,new String[]{"block"});			
					long block = Long.parseLong(arg.getString("block","-1"));
					session.output(session.ok()+" Spidering.");
					//TODO: run async
					provider.resync(block);
				}
			}
			return false;			
		} else
		if (session.trusted() && (session.mood == AL.direction || session.mood == AL.declaration)
			&& session.argsCount() <= 3 && session.read(Reader.pattern(AL.you,new String[] {"count","counting"}))) {
			StringBuilder sb = new StringBuilder();
			try {
				Collection peers = (Collection)session.sessioner.body.storager.getByName(AL.is,Schema.peer);
				session.sessioner.body.debug("Counting");
				if (!AL.empty(peers)){
					for (Iterator it = peers.iterator(); it.hasNext();){
						Thing peer = (Thing)it.next();
						Collection news = peer.getThings(AL.news); 
						Collection trusts = peer.getThings(AL.trusts); 
						Collection topics = peer.getThings(AL.topics); 
						Collection sites = peer.getThings(AL.sites);
						Collection ignores = peer.getThings(AL.ignores);
						Collection trustedtopics = AL.empty(topics) || AL.empty(trusts) ? null : new ArrayList(topics);
						if (!AL.empty(trustedtopics))
							trustedtopics.retainAll(trusts);
						Collection trustedSites = AL.empty(sites) || AL.empty(trusts) ? null : new ArrayList(sites);
						if (!AL.empty(trustedSites))
							trustedSites.retainAll(trusts);
						if (sb.length() > 0)
							sb.append('\n');
						String name = peer.getString(AL.email);
						if (AL.empty(name))
							name = peer.getTitle(Peer.title);
						sb.append(AL.empty(news)? "0" : ""+news.size()).append(' ')
							.append(AL.empty(trusts)? "0" : ""+trusts.size()).append(' ')
							.append(AL.empty(topics)? "0" : ""+topics.size()).append(' ')
							.append(AL.empty(sites)? "0" : ""+sites.size()).append(' ')
							.append(AL.empty(trustedtopics)? "0" : ""+trustedtopics.size()).append(' ')
							.append(AL.empty(trustedSites)? "0" : ""+trustedSites.size()).append(' ')
							.append(AL.empty(ignores)? "0" : ""+ignores.size()).append(' ')
							.append(name).append(' ')
							.append(peer.get(Peer.activity_time) == null ? "-" : Time.day((Date)peer.get(Peer.activity_time),false))
							;
					}
				}
		        File temp = session.sessioner.body.getFile("stats.txt");
		        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF-8"));
				writer.write(sb.toString());
				writer.close();
				session.output(session.ok());
			} catch (Exception e) {
				session.sessioner.body.error("Counting error ", e);
				session.output(session.no());
			}
			return false;
		} else
				
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.argsCount() <= 4 && session.read(Reader.pattern(AL.you,Self.saving))) {
			session.output(session.no());
			Thing saver = new Thing();
			if (!session.trusted())
				;//TODO: "No right" handling
			else
			if (session.read(new Seq(new Object[]{
				new Any(1,AL.you),	
				new Any(1,Self.saving),
				new Property(saver,Body.store_path),
				}))){
				if (Self.save(session.getBody(), saver.getString(Body.store_path)))
					session.output(session.ok());
			}else{
				if (session.sessioner.body.sitecacher != null)
					session.sessioner.body.sitecacher.saveGraphs();//flush
				if (Self.save(session.getBody(), session.sessioner.body.self().getString(Body.store_path)))
					session.output(session.ok());
			}
			return false;			
		} else	
		if ((session.mood == AL.direction || session.mood == AL.declaration) 
			&& session.read(Reader.pattern(AL.you,Self.loading))) {
			session.output(session.no());
			Thing loader = new Thing();
			if (session.read(new Seq(new Object[]{
				new Any(1,AL.you),	
				new Any(1,Self.loading),
				new Property(loader,Body.store_path),
				})))
				//TODO:do we really need clear here?
				if (Self.load(session.getBody(), loader.getString(Body.store_path)))
					session.output(session.ok());
			return false;			
		} else	
		if ((session.mood == AL.direction || session.mood == AL.declaration) 
			&& session.read(Reader.pattern(AL.you,Self.profiling))) {
			//you profile|profiling [<network> [, email <email>] [, name <name>] [, surname <surname>]]
			Thing task = new Thing();
			session.read(new Seq(new Object[]{new Any(1,AL.you),new Any(1,Self.profiling),new Property(task,"network")}));//optional network id
			if (session.sessioner.body.getSocializer(task.getString("network")) == null)//TODO Property-level domain validation
				task.setString("network", null);
			if (!session.trusted())
				task.addThing("peers",curPeer);//profile itself only 
			else {
				Collection peers; 
				if (session.read(task,new String[]{"email","name","surname"}) >= 1 &&
					!AL.empty(peers = storager.get(new Thing(task,Login.login_context),null))) 
					for (Object p : peers)
						task.addThing("peers", (Thing)p);
			}
			boolean started = session.getBody().act("profile", task);
			String output = "My "+Self.profiling[0]+".";
			session.output(started ? session.ok()+" " : session.no()+" " + output); 
			return false;			
		} else	
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,Self.reading))) {
			session.output(session.no());
			//TODO: understand context of 'knows': test_o("You reading 'sun flare' ... - must be test_o("You reading sun flare ...
			Collection topics = (Collection) session.getStoredPeer().get(AL.topics);
			if (!AL.empty(topics)) {
				if (session.read(new Seq(new Object[]{
						new Any(1,AL.you),new Any(1,Self.reading),new Property(reader,"thingname",1000),
						new Any(1,in_site),new Property(reader,"url",1000)
						}))) {
					reader.setString("range","3");//default, for test compatibility so far
					session.read(reader,new String[]{"range","limit","minutes"});			
					if (session.getBody().act("read", reader))
						session.output("My "+Self.reading[0]+" "+reader.getString("thingname")+
							" in "+Writer.toString(reader.getString("url"))+".");//session.ok();
				}
				else  
				// can fail if thingname is not supplied
				// TODO: decide what to do with this hack - needed for polymorphysm in argments! fix tests?
				if (session.read(new Seq(new Object[]{
							new Any(1,AL.you),new Any(1,Self.reading),
							new Any(1,in_site),new Property(reader = new Thing(),"url",1000)
							}))) {
					reader.setString("range","3");//default, for test compatibility so far
					session.read(reader,new String[]{"range","limit","minutes"});			
					if (session.getBody().act("read", reader))
						session.output("My "+Self.reading[0]+" site "+Writer.toString(reader.getString("url"))+".");//session.ok();
				}
				else {
					//NOTE range = 1
					//TODO:use reader with arguments for async spawn!?
					//session.read(reader,new String[]{"range","limit","minutes"});			
					if (session.getBody().act("read", null))//just read all sites altogether
						session.output(session.ok()+" My "+Self.reading[0]+".");
					else
						session.output(session.no()+" My "+Self.reading[0]+".");
				}
			}
			return false;
		} else

		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& (session.argsCount() > 2 && "load".equals(session.args()[0]))) {
			//&& session.read(new Seq(new Object[]{"load","file",new Property(reader,"file",1000),"as",new Property(reader,"file",50)}))) {
//TODO: fix reader to skip commas so we can read arguments into thing
//TODO: support formats: csv/tsv/json/html
			String file = Str.arg(session.args(), "file", null);
			String as = Str.arg(session.args(), "as", null);
			if (!AL.empty(file) && !AL.empty(as)) {
				Collection ases = session.getStorager().getNamed(as);
				if (AL.single(ases)) {
					try {
						//default is csv
//TODO: delimiter: ","/"\t"
						int loaded = new Streamer(session.getBody()).loadCSV(file, (Thing)ases.iterator().next(), curPeer);
						if (loaded > 0) {
							session.output(session.ok()+" "+loaded+" things.");
							return false;
						}
					} catch (Exception e) {
						session.getBody().error("Loading "+file+" as "+as,e);
					}
				}
			}
			session.output(session.no());
			return false;
		} else
			
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(new Seq(new Object[]{"classify","sentiment","text",new Property(reader,"text",1000)}))) {
			session.output(session.no());
			String text = reader.getString("text");
			if (!AL.empty(text)){
				String format = session.getString(AL.format);
				//text = AL.unquote(text);//TODO: unquoting is overkill here?
				ArrayList pc = new ArrayList();
				ArrayList nc = new ArrayList();
				int[] pns = session.sessioner.body.languages.sentiment(text, pc, nc);
				//HACK: "reuse" reader
				reader.setString("text", text);
				reader.setString("postivie", String.valueOf(pns[0]));
				reader.setString("negative", String.valueOf(pns[1]));
				reader.setString("sentiment", String.valueOf(pns[2]));
//TODO do format conversion inside format(...) below
				if ("json".equals(format)) {
					reader.set("positives", pc.toArray());
					reader.set("negatives", nc.toArray());
				} else {
					if (pc.size() > 0)
						reader.set("positives", Array.toSet(pc.toArray()));
					if (nc.size() > 0)
						reader.set("negatives",Array.toSet(nc.toArray()));
				}
				Collection rs = new ArrayList();
				rs.add(reader);
				session.output(Responser.format(null, session, curPeer, null, rs));
			}
			return false;	
		} else
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(new Seq(new Object[]{new Any(1,AL.you),"cluster"}))) {
			session.output(session.no());
			String json = null;
			String[] texts = null;
			
			//get all docs
			if (session.read(new Seq(new Object[]{new Any(1,AL.you),"cluster","format","json","texts",
					new Property(reader,"texts",1000)})) && !AL.empty((json = reader.getString("texts")))){
				//TODO: JSON
				JsonReader jr = Json.createReader(new StringReader(json));
				JsonArray ja = jr.readArray();
				if (ja != null && ja.size() > 0 && ja.getString(0) instanceof String) {
					texts = new String[ja.size()];
					for (int i = 0; i < ja.size(); i++)
						texts[i] = ja.getString(i);
				}
			} else {
				Collection news = session.getStoredPeer().getThings(AL.trusts);
				if (!AL.empty(news))
					texts  = Thing.toStrings(news,AL.text);
			}
			//TODO: clustering via adapter
			{
				if (!AL.empty(texts)){
					TextMiner m = new TextMiner(session.getBody(),null,false).setDocuments(texts).cluster();
					if (json != null) {
						Thing data = new Thing();
						data.set("categories", m.getCategoryNames());
						data.set("category_documents", m.getCategoryDocuments());
						data.set("category_features", m.getCategoryFatures());
						session.output(Writer.toJSON(data,null));
					} else
						session.output("You topics "+Writer.toString(m.getCategoryNames())+"."
							+TextMiner.toString(m.getCategoryDocuments(),AL.sites,",",";\n")+"\n"
							+TextMiner.toString(m.getCategoryFatures(),AL.patterns,",",";\n"));
				}
			}
			return false;			
		} else	
		if (tryAlerter(storager,session))//if alert sent successflly 
			return false;//no further interaction is needed
		else
		if (tryEmail(storager,session))//if email send tried successflly 
			return false;//no further interaction is needed
		else
		if (tryRSS(storager,session))//if RSS feed tried successflly 
			return false;//no further interaction is needed
		else
		if (tryParse(storager,session))//if parsing tried successflly 
			return false;//no further interaction is needed
		else
		if (tryReport(storager,session))//if reporting tried successflly 
			return false;//no further interaction is needed
		else
		if (tryGraph(storager,session))//if graphing tried successfully 
			return false;//no further interaction is needed
		else
		if (tryReputationer(storager,session))//if graphing tried successfully 
			return false;//no further interaction is needed
		else
		if (session.mood == AL.declaration || session.mood == AL.direction)
		{
			if (handleIntent(session))
				return false;;
			session.output(session.no());
			return false;
		} 
		//TODO: if such fallthrough is needed?
		session.output("Dear " 
			+ (session.peer != null ? session.peer.getTitle(Peer.title) : "friend")
			+ ", please contact us for help at "+Body.ORIGINSITE+".");
		return false;
	}//doAuthenticated
		
	//TODO: unifiy the code 
	boolean tryReport(Storager storager,Session session) {
		Thing arg = new Thing();
		if (session.read(new Seq(new Object[]{new Property(arg,"network"),"id",new Property(arg,"id"),"report"}))
				&& session.sessioner.body.getSocializer(arg.getString("network")) != null 
				&& arg.getString("id") != null && arg.getString("network") != null
				//if either a) provider is "public" or b) specified id is matching user id or c) we supply the auth token 
				&& (session.sessioner.body.getSocializer(arg.getString("network")).opendata() || arg.getString("id").equals(session.getStoredPeer().getString(arg.getString("network")+" id"))
						|| session.read(new Seq(new Object[]{"token",new Property(arg,"token")}))) ) {
				String format = session.read(new Seq(new Object[]{"format",new Property(arg,"format")})) ? arg.getString("format") : "html"; 
				int threshold = session.read(new Seq(new Object[]{"threshold",new Property(arg,"threshold")})) ? Integer.valueOf(arg.getString("threshold")).intValue() : 20;
				//int range = session.read(new Seq(new Object[]{"range",new Property(arg,"range")})) ? Integer.valueOf(arg.getString("range")).intValue() : 1;
				String period = session.read(new Seq(new Object[]{"period",new Property(arg,"period")})) ? arg.getString("period") : null;
				String[] areas = session.read(new Seq(new Object[]{"areas",new Property(arg,"areas")})) ? new String[]{arg.getString("areas")} : null;
				Thing peer = session.getStoredPeer();
				String language = peer.getString(Peer.language);
				boolean fresh = session.input().contains("fresh");
				Socializer provider = session.sessioner.body.getSocializer(arg.getString("network"));
				String id = arg.getString("id");
				String token = session.read(new Seq(new Object[]{"token",new Property(arg,"token")})) ? arg.getString("token") : null;
			   	if (AL.empty(token)) //if token is not supplied explicitly
			   		token = arg.getString("id").equals(session.getStoredPeer().getString(arg.getString("network")+" id")) ? session.getStoredPeer().getString(provider.name()+" token") : null;
				//TODO: name and language for opendata/steemit?
			   	String secret = provider.getTokenSecret(session.getStoredPeer());
				String report = provider.cachedReport(id,token,secret,id,"",language,format,fresh,session.input(),threshold,period,areas);
				session.output(report != null ? report : session.no());
				return true;			
		} else	
		if (session.read(new Seq(new Object[]{
					new Any(1,AL.i_my),new Property(arg,"network"),"report"}))
					&& session.sessioner.body.getSocializer(arg.getString("network")) != null ) {
				String format = session.read(new Seq(new Object[]{"format",new Property(arg,"format")})) ? arg.getString("format") : "html"; 
				int threshold = session.read(new Seq(new Object[]{"threshold",new Property(arg,"threshold")})) ? Integer.valueOf(arg.getString("threshold")).intValue() : 20;
				//int range = session.read(new Seq(new Object[]{"range",new Property(arg,"range")})) ? Integer.valueOf(arg.getString("range")).intValue() : 1;
				String period = session.read(new Seq(new Object[]{"period",new Property(arg,"period")})) ? arg.getString("period") : null;
				String[] areas = session.read(new Seq(new Object[]{"areas",new Property(arg,"areas")})) ? new String[]{arg.getString("areas")} : null;
				Thing peer = session.getStoredPeer();
			   	String name = peer.getString(AL.name);
			   	String surname = peer.getString(Peer.surname);
			   	String language = peer.getString(Peer.language);
				boolean fresh = session.input().contains("fresh");
			   	Socializer provider = session.sessioner.body.getSocializer(arg.getString("network"));
			   	String id = peer.getString(provider.getPeerIdName());
			   	String token = peer.getString(provider.name()+" token");
			   	String secret = provider.getTokenSecret(session.getStoredPeer());
				String report = provider.cachedReport(id,token,secret,name,surname,language,format,fresh,session.input(),threshold,period,areas);
				session.output(report != null ? report : session.no());
				return true;	
		}
		return false;
	}

	//TODO: A MUST - move to other place to avoid concurrent use of files ad redundant memory use
	HashMap reputationers = new HashMap();
	boolean tryReputationer(Storager storager,Session session) {
		int rs = -1;
		Thing arg = new Thing();
		if (!session.trusted())//for superusers only, so far...
			return false;
		
		if (Str.has(session.args(),"reputation","update")) {
			session.read(new Seq(new Object[]{"update",new Property(arg,"network")}));
			boolean updated = session.getBody().act("reputation update", arg);
			session.output(updated ? session.ok() : session.no());
			return true;
		}
			
		if (!session.read(new Seq(new Object[]{"reputation","network",new Property(arg,"word")})) && 
			!session.read(new Seq(new Object[]{new Property(arg,"word"),"reputation"})))
			return false;
		String format = session.getStoredPeer().getString("format");
		boolean json = format != null && format.toLowerCase().equals("json");
		final Charset charset = StandardCharsets.UTF_8;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps;
		String result = null;
		try {
			String network = arg.getString("word"); 
			ReputationSystem r = SocialCacher.getReputationSystem(session.getBody(), network);
			ps = new PrintStream(baos, true, charset.name());
			//TODO: get data as object!?
//TODO return error code in json
			rs = Reputationer.act(session.getBody(), r, ps, session.args(), json);
			if (rs == 0)
				result = new String(baos.toByteArray(), charset);
			ps.close();
			baos.close();
		} catch (Exception e) {
			session.getBody().error("", e);
		}
		if (rs == -1)//method not matched
			return false;
		if (json)
			session.output("{\"result\" : "+rs+(rs == 0 ? ", \"data\" : "+result : "")+"}");
		else
			session.output(rs == 0 ? session.ok()+"\n" + result : session.no());
		return true;
	}

	boolean tryAlerter(Storager storager,Session session) {
		Thing arg = new Thing();
		Collection peers;
		String text;
		if (session.args().length > 3 && session.args()[0].equalsIgnoreCase("alert") &&
				session.read(arg,new String[]{"email","name","surname","text"}) >= 2 &&
				!AL.empty(peers = storager.get(new Thing(arg,Login.login_context),null)) && 
				!AL.empty(peers) &&
				!AL.empty(text = arg.getString("text"))){
			for (Iterator it = peers.iterator(); it.hasNext();){
				Thing peer = (Thing)it.next();
				Thing self = session.getStoredPeer();
				//send notification to all matching peers of those who trust to this peer
				if (peer == self || peer.hasThing(AL.trusts, self)){
					try {
						boolean updated = session.sessioner.body.update(peer, null, null, text, "- "+self.getTitle(Login.login_context));
						session.output(updated ? session.ok() : session.no());
					} catch (IOException e) {
						session.sessioner.body.error("Alerting "+peer.getTitle(Login.login_context), e);
						session.output(session.no());
					}
					return true;
				}
			}
		}
		return false;
	}
	
	boolean tryEmail(Storager storager,Session session) {
		Thing arg = new Thing();
		if (session.read(new Seq(new Object[]{AL.email,"to",new Property(arg,"to"),"subject",new Property(arg,"subject"),"text",new Property(arg,"text")}))){
			session.read(arg,new String[]{"from"});			
			String from = arg.getString("from");
			if (AL.empty(from))
				from = session.getPeer().getString(AL.email);
			if (AL.empty(from))
				from = session.getSelfPeer().getString(AL.email);
			String to = arg.getString("to");
			String text = arg.getString("text");
			String subject = arg.getString("subject");
			if (Emailer.valid(from) && Emailer.valid(to) && !AL.empty(text)){
				if (AL.empty(subject))
					subject = session.sessioner.body.signature();
				try {
					text += "\n"+session.sessioner.body.signature();
					Emailer e = Emailer.getEmailer();
					e.email(from,to,subject,text);
					session.output(session.ok());
				} catch (IOException e) {
					session.sessioner.body.error("Sending email from "+from+" to "+to, e);
					session.output(session.no());
				}
				return true;
			}
		}
		return false;
	}
	
	//TODO:
	/*
	For graph querying, the following parameters may be used
	network - name of the network
	date - date for the analysis (latest date of period for analysis, inclusively)
	period - number of days for the analysis to count from date to the past
	ids - ids of the nodes (mentioned in from/to) used as a seeds for graph expansion
	range - number of hops in the network to account for from source nodes - from 1 to N
	threshold - numeric threshold to select links with with relative link value value greater than that
	nodes - node types to be involved in graph expansion  
	links - link types to be involved in graph expansion
	tags - tags to restrict graph expansion
	format - format to return results (JSON, Graph Al, etc.)
	 */
	boolean tryGraph(Storager storager,Session session) {
		Thing arg = new Thing();
		String id;
		String network;
		if (session.read(new Seq(new Object[]{new Property(arg,"network"),"id",new Property(arg,"id"),"graph"}))
				&& (network = arg.getString("network")) != null
				&& (session.sessioner.body.getSocializer(network) != null || "www".equalsIgnoreCase(network))
				&& (id = arg.getString("id")) != null //TODO: sites url
				//if either or a) provider is "www" or b) provider is "public" or c) specified id is matching user id
				&& ("www".equalsIgnoreCase(network) || session.sessioner.body.getSocializer(network).opendata() || id.equals(session.getStoredPeer().getString(network+" id"))) ) {
			session.read(arg,new String[]{"date","period","range","threshold","links","limit","format"});			
			Date date = Time.day(arg.getString("date","today"));//target date
			String period = arg.getString("period","7");//days back
			String range = arg.getString("range","1");
			String threshold = arg.getString("threshold","0");
			int limit = Integer.parseInt(arg.getString("limit","250"));
			if (limit > 500)//hard cap to save browser
				limit = 500;
			String format = arg.getString("format","al");
			//TODO: request links/relationships as list of names, now let all links be included
			String links = arg.getString("links",null);
			
			//lazy site indexing
			//TODO:pass range as depth if range is greater than default!?
			if ("www".equalsIgnoreCase(network) && session.sessioner.body.sitecacher != null){
				Graph dailygraph = session.sessioner.body.sitecacher.getGraph(date);
				if (dailygraph.getLinkers(id, false) == null)
					session.getBody().act("read", (new Thing()).set("url", id));
			}
			
			Socializer socializer = session.sessioner.body.getSocializer(network);//setup graph-id to grap-label mapping
			Transcoder labeler = socializer != null && socializer instanceof Transcoder ? (Transcoder)socializer : null;
			
			//apply group filter to non-admin sessions if not open data and group filtering is supported
			//java.util.Set<String> members = !session.trusted() && socializer != null && !socializer.opendata() && socializer instanceof Grouper 
			java.util.Set<String> members = !session.trusted() && socializer != null && socializer instanceof Grouper 
					? ((Grouper)socializer).getGroup(id) : null;
			
			String graph = graphQuery(session,network,new String[]{labeler != null ? (String)labeler.recovercode(id) : id},
					date,
					Integer.parseInt(period),
					Integer.parseInt(range),
					Integer.parseInt(threshold),
					limit,
					format,
					AL.empty(links) ? null : new String[]{links},
					labeler,
					members);
			session.output(!AL.empty(graph) ? graph : session.no());
			return true;			
		} 
		return false;
	}
	
	//TODO: move out to somewhere
	String graphQuery(Session session, String network, String[] ids, Date date, int period, int range, int threshold, int limit, String format, String[] links, Transcoder coder, java.util.Set<String> members){
		GraphCacher grapher;
		if (network.equalsIgnoreCase("www")){
			grapher = session.sessioner.body.sitecacher;
		}else {
			Socializer provider = session.sessioner.body.getSocializer(network);
			grapher = provider instanceof SocialCacher ? ((SocialCacher)provider).getGraphCacher() : null;
		}
		if (grapher == null)
			return null;
		
		Graph result = grapher.getSubgraph(ids, date, period, range, threshold, limit, links, members, null);

		//TODO: revert reciprocal links here before exporting?
		
		//translate subgraphs to JSON/AL, accordingly to "format"
		String out = format.equalsIgnoreCase("json") ? result.toJSON() : result.toString(coder);
		
		//save memory
		result.clear();
		return out;
	}

	//TODO: make this part of parser, referring to vocabulary
	private static Seq restrict(Seq tokens, LangPack langs) {
		if (AL.empty(tokens))
			return new Seq(new String[]{});
		ArrayList restricted = new ArrayList(tokens.size());
		if (langs != null && !AL.empty(langs.words()))
		for (int i = 0; i < tokens.size(); i++){
			String token = (String)tokens.get(i);
			if (langs.words().containsKey(token))
				restricted.add(token);
		}
		return new Seq(restricted.toArray(new String[]{}));
	}

	boolean tryParse(Storager storager,Session session) {
		Thing arg = new Thing();
		if ((session.mood == AL.direction || session.mood == AL.declaration)
				&& session.read(Reader.pattern(AL.you,Self.parsing))) {
			session.output(session.no());
			// AL: you parse <text>, format <format>, language <language>, features <words|phrases|emoticons> 
			String sdist = session.read(new Seq(new Object[]{"distance",new Property(arg,"distance")})) ? arg.getString("distance") : null; 
			String text = session.read(new Seq(new Object[]{AL.text,new Property(arg,AL.text)})) ? arg.getString(AL.text) : null; 
			if (!AL.empty(text)) {
				int distance = AL.empty(sdist) ? 1 : StringUtil.toIntOrDefault(sdist,10,1);
				Seq tokens = Parser.parse(text,AL.commas+AL.periods+AL.spaces,false,true,true,true);
				Seq restricted = restrict(tokens,session.sessioner.body.languages);				
				Set grams = Parser.grams(restricted,distance);
				session.output("There text "+Writer.toString(text)+
						", tokens "+Writer.toString(tokens)+
						", grams "+Writer.toString(grams)+".");
				
			}
			return true;
		}
		return false;
	}
}
