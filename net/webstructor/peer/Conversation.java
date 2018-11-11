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
package net.webstructor.peer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import net.webstructor.al.Statement;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.SocialCacher;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Mistake;
import net.webstructor.core.Property;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Graph;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.LangPack;
import net.webstructor.data.TextMiner;
import net.webstructor.self.Self;

class Conversation extends Mode {
	
	public static final String[] spider = new String[] {"spider","spidering","crawl","crawling"};

	private String spidering(Session session, String name, String id) {
		String report = null;
		Socializer provider = session.sessioner.body.provider(name);
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
		return report != null ? report : "Not.";
	}
	
	//TODO:something more smart?
	private String socialBind(Session session,String idname,String idvalue,String tokenname,String tokenvalue){
		try {
			Collection owners = session.sessioner.body.storager.getByName(idname,idvalue);
			if (!AL.empty(owners) && (owners.size()>1 || !idvalue.equals(session.getStoredPeer().getString(idname,null))))
				return Writer.capitalize(idname+" "+idvalue+" is owned.");
		}catch(Exception e){
			session.sessioner.body.error("Social bind",e);
			return e.getMessage();
		}			
		session.getStoredPeer().set(idname, idvalue);
		session.getStoredPeer().set(tokenname, tokenvalue);
		return "Ok.";
	}
	
	public boolean process(Session session) {
	  try {
		Storager storager = session.sessioner.body.storager;
		/*
		//TODO: wigh fixin bunch of unit tests
		//if logged peer is no longer valid, logout 
		if (session.authenticated && session.getStoredPeer() == null){
			session.authenticated = false;
			session.peer = null;
		}*/
		//update "session properties" in "proxy peer"
		if (!session.authenticated && setProperties(session))
			return false;
		if (session.peer == null) { // not authenticated, but in the process
			session.mode = new Login();
			return true;
		} else
		if (session.read(Reader.pattern(AL.i_my,new String[] {"logout"}))){//can logout at any point			
			session.output = "Ok.";
			session.mode = new Login();
			session.peer = null;
			session.authenticated = false;
			return false;			
		} else
		if (!session.authenticated && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,new String[] {"secret question","secret answer"}))){
			session.mode = new VerificationChange();
			return true;			
		} else
		if (session.authenticated) {
			return doAuthenticated(storager,session);
		} else {
			session.mode = new Login();//TODO: really, just back to login?
			return true;
		}
	  } catch (Exception e) {
		  if (!(e instanceof Mistake))
			  session.sessioner.body.error("Error handling: "+session.input, e);
		  session.output = "Not. "+e.getMessage();
		  return false;
	  }
	}

	//TODO: move all that stuff to other place - social networks separately, root separately, etc.
	private boolean doAuthenticated(Storager storager,Session session){
		Thing curPeer = session.getStoredPeer();
		if (curPeer != null)//TODO:cleanup as it is just in case for post-mortem peer operations in tests
			curPeer.set(Peer.activity_time,Time.day(Time.today));
		
		if (session.mood == AL.interrogation) {
			return answer(session);
		} else
		//binding Social Network accounts
		//TODO: refactor for unification
		//TODO: ensure no id stealing happens (re-steal, if so)
		if (session.sessioner.body.vk != null && session.input.indexOf("vkontakte_id=")==0){
			//update VK login upon callback
			String ensti[] = session.sessioner.body.vk.verifyRedirect(session.input);
			String ok = ensti == null ? null : socialBind(session,Body.vkontakte_id, ensti[4],Body.vkontakte_token, ensti[3]);
			if (!ok.equals("Ok."))//TODO: fix hack!?
				ensti = null;
			//TODO:restrict origin for better security if passing token?
			session.output = "<html><body onload=\"top.postMessage(\'"+(ensti == null ? "Error:" : "Success:")+session.input+"\',\'*\');top.window.vkontakteLoginComplete();\"></body></html>";
			return false;		
		}else
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.vkontakte_id,Body.vkontakte_token}))) {
			String id = session.peer.getString(Body.vkontakte_id);
			String token = session.peer.getString(Body.vkontakte_token);
			String enst[];
			if (session.sessioner.body.vk != null && ((enst = session.sessioner.body.vk.verifyToken(id, token)) != null)) {
				session.output = socialBind(session,Body.vkontakte_id, id,Body.vkontakte_token, enst[3]);
			} else
				session.output = "Not.";
			return false;		
		} else
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.google_id,Body.google_token}))) {
			String id = session.peer.getString(Body.google_id);
			String token = session.peer.getString(Body.google_token);
			String enstir[];//email,name,surname,token,id,refresh_token
			if (session.sessioner.body.gapi != null && ((enstir = session.sessioner.body.gapi.verifyToken(id, token)) != null)) {
				id = enstir[4];
				session.output = socialBind(session,Body.google_id, id, Body.google_token, enstir[3]);
				if ("Ok.".equals(session.output)) {//TODO: fix hack!?
					session.getStoredPeer().set(Body.google_id,id);//TODO:straighten
					if (!AL.empty(enstir[5]))//refresh_token as google_key
						session.getStoredPeer().set(Body.google_key, enstir[5]);
				}
			} else
				session.output = "Not.";
			return false;		
		} else
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.facebook_id,Body.facebook_token}))) {
			String id = session.peer.getString(Body.facebook_id);
			String token = session.peer.getString(Body.facebook_token);
			if (session.sessioner.body.fb != null && (token = session.sessioner.body.fb.verifyToken(id, token)) != null) {
				session.output = socialBind(session,Body.facebook_id, id,Body.facebook_token, token);
			} else
				session.output = "Not.";
			return false;		
		} else
			
			
		if (session.mood == AL.declaration && !session.isSecurityLocal() &&
			session.read(Reader.pattern(AL.i_my,new String[] {"email","e-mail"},"/.+@.+/"))) {
			session.mode = new EmailChange();
			return true;			
		} else 
		if (session.input.length() == 0) { //repeated authentication seed for pre-authenticated session
			session.output = "Ok.";
			return false;			
		} else
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,new String[] {"forget","forgetting"}))) {
			Self.clear(session.sessioner.body,Schema.foundation);
			if (session.read(Reader.pattern(AL.you,new String[] {"forget everything","forget all"}))){
				session.sessioner.body.archiver.clear();
				if (session.sessioner.body.sitecacher != null)
					session.sessioner.body.sitecacher.clear(true);//clear with LTM
			}
			session.output = "Ok.";
			return false;
		} else
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,new String[] {"think","thinking"}))) {
			Peer.trashPeerNews(session.sessioner.body,session.getStoredPeer());//this does think along the way!
			session.output = "Ok.";
			return false;			
		} else
			
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,spider))) {
			Thing task = new Thing();
			session.output = "Not.";
			if (session.read(new Seq(new Object[]{
					new Any(1,AL.you),"spidering",new Property(task,"network"),
					"id",new Property(task,"id")
					}))) {
				session.output = spidering(session, task.getString("network"), task.getString("id"));
			} else
			if (session.read(new Seq(new Object[]{
						new Any(1,AL.you),new Any(1,spider),new Property(task,"network")}))) {
				Socializer provider = session.sessioner.body.provider(task.getString("network"));
				if (provider != null){
					Thing arg = new Thing();
					session.read(arg,new String[]{"block"});			
					long block = Long.parseLong(arg.getString("block","-1"));
					session.output = "Ok. Spidering.";
					//TODO: run async
					provider.resync(block);
				}
			}
			return false;			
		} else
		if (trusted(session) && (session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,new String[] {"count","counting"}))) {
			StringBuilder sb = new StringBuilder();
			try {
				Collection peers = (Collection)session.sessioner.body.storager.getByName(AL.is,Schema.peer);
				session.sessioner.body.debug("Counting");
				if (!AL.empty(peers)){
					for (Iterator it = peers.iterator(); it.hasNext();){
						Thing peer = (Thing)it.next();
						Collection news = peer.getThings(AL.news); 
						Collection trusts = peer.getThings(AL.trusts); 
						Collection knows = peer.getThings(AL.knows); 
						Collection sites = peer.getThings(AL.sites);
						Collection ignores = peer.getThings(AL.ignores);
						Collection trustedKnows = AL.empty(knows) || AL.empty(trusts) ? null : new ArrayList(knows);
						if (!AL.empty(trustedKnows))
							trustedKnows.retainAll(trusts);
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
							.append(AL.empty(knows)? "0" : ""+knows.size()).append(' ')
							.append(AL.empty(sites)? "0" : ""+sites.size()).append(' ')
							.append(AL.empty(trustedKnows)? "0" : ""+trustedKnows.size()).append(' ')
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
				session.output = "Ok.";
			} catch (Exception e) {
				session.sessioner.body.error("Counting error ", e);
				session.output = "Not.";
			}
			return false;
		} else
				
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,Self.saving))) {
			session.output = "Not.";
			Thing saver = new Thing();
			if (!trusted(session))
				;//TODO: "No right" handling
			else
			if (session.read(new Seq(new Object[]{
				new Any(1,AL.you),	
				new Any(1,Self.saving),
				new Property(saver,Body.store_path),
				}))){
				if (Self.save(session.getBody(), saver.getString(Body.store_path)))
					session.output = "Ok.";
			}else{
				if (Self.save(session.getBody(), session.sessioner.body.self().getString(Body.store_path)))
					session.output = "Ok.";
			}
			return false;			
		} else	
		if ((session.mood == AL.direction || session.mood == AL.declaration) 
			&& session.read(Reader.pattern(AL.you,Self.loading))) {
			session.output = "Not.";
			Thing loader = new Thing();
			if (session.read(new Seq(new Object[]{
				new Any(1,AL.you),	
				new Any(1,Self.loading),
				new Property(loader,Body.store_path),
				})))
				//TODO:do we really need clear here?
				if (Self.load(session.getBody(), loader.getString(Body.store_path)))
					session.output = "Ok.";
			return false;			
		} else	
		if ((session.mood == AL.direction || session.mood == AL.declaration) 
			&& session.read(Reader.pattern(AL.you,Self.profiling))) {
			//TODO: make network input as parameter
			boolean started = session.getBody().act("profile", null);
			session.output =  "My "+Self.profiling[0]+".";
			session.output = (started ? "Ok. " : "Not. ") + session.output; 
			return false;			
		} else	
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(Reader.pattern(AL.you,Self.reading))) {
			session.output = "Not.";
			//TODO: understand context of 'knows': test_o("You reading 'sun flare' ... - must be test_o("You reading sun flare ...
			Collection knows = (Collection) session.getStoredPeer().get(AL.knows);
			if (!AL.empty(knows)) {
				Thing reader = new Thing();
				if (session.read(new Seq(new Object[]{
						new Any(1,AL.you),new Any(1,Self.reading),new Property(reader,"thingname"),
						new Any(1,new String[]{"site","in","url"}),new Property(reader,"url")
						}))) {
					session.read(reader,new String[]{"range","limit","minutes"});			
					if (session.getBody().act("read", reader))
						session.output = "My "+Self.reading[0]+" "+reader.getString("thingname")+
							" in "+Writer.toString(reader.getString("url"))+".";//"Ok.";
				}
				else  
				// can fail if thingname is not supplied
				// TODO: decide what to do with this hack!
				if (session.read(new Seq(new Object[]{
							new Any(1,AL.you),new Any(1,Self.reading),
							new Any(1,new String[]{"site","in","url"}),new Property(reader = new Thing(),"url")
							}))) {
					session.read(reader,new String[]{"range","limit","minutes"});			
					if (session.getBody().act("read", reader))
						session.output = "My "+Self.reading[0]+" site "+Writer.toString(reader.getString("url"))+".";//"Ok.";
				}
				else {
					//TODO:use reader with arguments for async spawn!?
					//session.read(reader,new String[]{"range","limit","minutes"});			
					if (session.getBody().act("read", null))//just read all sites altogether
						session.output = "Ok. My "+Self.reading[0]+".";
					else
						session.output = "Not. My "+Self.reading[0]+".";
				}
			}
			return false;
		} else
			
		if ((session.mood == AL.direction || session.mood == AL.declaration)
			&& session.read(new Seq(new Object[]{new Any(1,AL.you),"cluster"}))) {
			session.output = "Not.";
			Thing reader = new Thing();
			String json = null;
			String[] texts = null;
			
			//get all docs
			if (session.read(new Seq(new Object[]{new Any(1,AL.you),"cluster","format","json","texts",
					new Property(reader,"texts")})) && !AL.empty((json = reader.getString("texts")))){
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
					TextMiner m = new TextMiner(session.getBody(),null,true).setDocuments(texts).cluster();
					if (json != null) {
						Thing data = new Thing();
						data.set("categories", m.getCategoryNames());
						data.set("category_documents", m.getCategoryDocuments());
						data.set("category_features", m.getCategoryFatures());
						session.output = Writer.toJSON(data);
					} else
						session.output = "You knows "+Writer.toString(m.getCategoryNames())+"."
							+TextMiner.toString(m.getCategoryDocuments(),"sites",",",";\n")+"\n"
							+TextMiner.toString(m.getCategoryFatures(),"patterns",",",";\n");
				}
			}
			return false;			
		} else	
		
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
		if (Reader.read(session.input, new Any(1,AL.not)))
		{
			try {
				Statement query = session.reader.parseStatement(session,session.input,session.getStoredPeer());
				session.sessioner.body.output("Dec:"+Writer.toString(query)+".");	
				if (!AL.empty(query)) {
					int skipped = 0;
					Collection things = storager.get(query,session.getStoredPeer());
					if (!AL.empty(things)) //clone for deletion!
						for (Iterator it = new ArrayList(things).iterator();it.hasNext();) {
							Thing thing = (Thing)it.next();
							String deathmask = Writer.toString(thing);
							if (!thing.del()) {
								session.sessioner.body.output("SKIPPED:"+deathmask+".");
								skipped++;
							}
							else {
								session.sessioner.body.output("DELETED:"+deathmask+".");
								session.getStorager().setUpdate();
							}
						}
					session.output = skipped > 0 ? "Not. There things." : "Ok.";
				}
			} catch (Exception e) {
				session.output = statement(e);
				session.sessioner.body.error(e.toString(), e);
			}
			return false;
		} else 
		if (session.mood == AL.declaration || session.mood == AL.direction)
		{
			try {
				Thing storedPeer = session.getStoredPeer();
				StringBuilder out = new StringBuilder();
				Collection message = session.reader.parseStatements(session,session.input,session.getStoredPeer());
				for (Iterator it = message.iterator(); it.hasNext();) {
					Statement query = (Statement)it.next();
					session.sessioner.body.output("Dec:"+Writer.toString(query)+".");			
					//TODO: do this peer saving and restoring more clever and not every time?
					//get stored session peer pointer just in case it is changed in background
					new Query(storager,session.sessioner.body.self()).setThings(query,storedPeer);
					//get actual session peer parameters to access it another time
					session.peer.update(storedPeer,Login.login_context);
					out.append(out.length() > 0 ? " " : "").append("Ok.");
				}
				session.output = out.toString();
				return false;
			} catch (Exception e) {
				session.output = statement(e);
				if (!(e instanceof Mistake))
					session.sessioner.body.error(e.toString(), e);
				return false;
			}
		} 
		//TODO: if such fallthrough is needed?
		session.output = "Dear " 
			+ (session.peer != null ? session.peer.getTitle(Peer.title) : "friend")
			+ ", please contact us for help at "+Body.ORIGINSITE+".";
		return false;
	}//doAuthenticated
	
	
	//TODO: unifiy the code 
	boolean tryReport(Storager storager,Session session) {
		Thing arg = new Thing();
		if (session.read(new Seq(new Object[]{new Property(arg,"network"),"id",new Property(arg,"id"),"report"}))
				&& session.sessioner.body.provider(arg.getString("network")) != null 
				&& arg.getString("id") != null && arg.getString("network") != null
				//if either a) provider is "public" or b) specified id is matching user id or c) we supply the auth token 
				&& (session.sessioner.body.provider(arg.getString("network")).opendata() || arg.getString("id").equals(session.getStoredPeer().getString(arg.getString("network")+" id"))
						|| session.read(new Seq(new Object[]{"token",new Property(arg,"token")}))) ) {
				String format = session.read(new Seq(new Object[]{"format",new Property(arg,"format")})) ? arg.getString("format") : "html"; 
				int threshold = session.read(new Seq(new Object[]{"threshold",new Property(arg,"threshold")})) ? Integer.valueOf(arg.getString("threshold")).intValue() : 20;
				//int range = session.read(new Seq(new Object[]{"range",new Property(arg,"range")})) ? Integer.valueOf(arg.getString("range")).intValue() : 1;
				String period = session.read(new Seq(new Object[]{"period",new Property(arg,"period")})) ? arg.getString("period") : null;
				String[] areas = session.read(new Seq(new Object[]{"areas",new Property(arg,"areas")})) ? new String[]{arg.getString("areas")} : null;
				Thing peer = session.getStoredPeer();
				String language = peer.getString(Peer.language);
				boolean fresh = session.input.contains("fresh");
				Socializer provider = session.sessioner.body.provider(arg.getString("network"));
				String id = arg.getString("id");
				String token = session.read(new Seq(new Object[]{"token",new Property(arg,"token")})) ? arg.getString("token") : null;
			   	if (AL.empty(token)) //if token is not supplied explicitly
			   		token = arg.getString("id").equals(session.getStoredPeer().getString(arg.getString("network")+" id")) ? session.getStoredPeer().getString(provider.provider()+" token") : null;
				//TODO: name and language for opendata/steemit?
				String report = provider.cachedReport(id,token,null,id,"",language,format,fresh,session.input,threshold,period,areas);
				session.output = report != null ? report : "Not.";
				return true;			
		} else	
		if (session.read(new Seq(new Object[]{
					new Any(1,AL.i_my),new Property(arg,"network"),"report"}))
					&& session.sessioner.body.provider(arg.getString("network")) != null ) {
				String format = session.read(new Seq(new Object[]{"format",new Property(arg,"format")})) ? arg.getString("format") : "html"; 
				int threshold = session.read(new Seq(new Object[]{"threshold",new Property(arg,"threshold")})) ? Integer.valueOf(arg.getString("threshold")).intValue() : 20;
				//int range = session.read(new Seq(new Object[]{"range",new Property(arg,"range")})) ? Integer.valueOf(arg.getString("range")).intValue() : 1;
				String period = session.read(new Seq(new Object[]{"period",new Property(arg,"period")})) ? arg.getString("period") : null;
				String[] areas = session.read(new Seq(new Object[]{"areas",new Property(arg,"areas")})) ? new String[]{arg.getString("areas")} : null;
				Thing peer = session.getStoredPeer();
			   	String name = peer.getString(AL.name);
			   	String surname = peer.getString(Peer.surname);
			   	String language = peer.getString(Peer.language);
				boolean fresh = session.input.contains("fresh");
			   	Socializer provider = session.sessioner.body.provider(arg.getString("network"));
			   	String id = peer.getString(provider.provider()+" id");
			   	String token = peer.getString(provider.provider()+" token");
				String report = provider.cachedReport(id,token,null,name,surname,language,format,fresh,session.input,threshold,period,areas);
				session.output = report != null ? report : "Not.";
				return true;	
		}
		return false;
	}
	
	//TODO: move to other place
	HashMap reputationers = new HashMap();
	boolean tryReputationer(Storager storager,Session session) {
		Thing arg = new Thing();
		if (!trusted(session))//for superusers only, so far...
			return false;
		if (!session.read(new Seq(new Object[]{"reputation","network",new Property(arg,"word")})) && 
			!session.read(new Seq(new Object[]{new Property(arg,"word"),"reputation"})))
			return false;
		final Charset charset = StandardCharsets.UTF_8;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps;
		boolean ok = false;
		String result = null;
		try {
			String network = arg.getString("word"); 
			Reputationer r;
			synchronized (reputationers){
				r = (Reputationer)reputationers.get(network);
				if (r == null)
					reputationers.put(network, r = new Reputationer(session.getBody(),network,null,true));//with daily states
			}
			ps = new PrintStream(baos, true, charset.name());
			ps.print("Ok.\n");
			//TODO: get data as object!?
			ok = Reputationer.act(session.getBody(), r, ps, Parser.split(session.input, " \t,;"));
			if (ok)
				result = new String(baos.toByteArray(), charset);
			ps.close();
			baos.close();
		} catch (Exception e) {
			session.getBody().error("", e);
		}
		session.output = ok ? result : "Not.";
		return true;
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
				&& (session.sessioner.body.provider(network) != null || "www".equalsIgnoreCase(network))
				&& (id = arg.getString("id")) != null //TODO: sites url
				//if either or a) provider is "www" or b) provider is "public" or c) specified id is matching user id
				&& ("www".equalsIgnoreCase(network) || session.sessioner.body.provider(network).opendata() || id.equals(session.getStoredPeer().getString(network+" id"))) ) {
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
			
			String graph = graphQuery(session,network,new String[]{id},
					date,
					Integer.parseInt(period),
					Integer.parseInt(range),
					Integer.parseInt(threshold),
					limit,
					format,
					AL.empty(links) ? null : new String[]{links});
			session.output = !AL.empty(graph) ? graph : "Not.";
			return true;			
		} 
		return false;
	}
	
	//TODO: move out to somewhere
	String graphQuery(Session session, String network, String[] ids, Date date, int period, int range, int threshold, int limit, String format, String[] links){
		GraphCacher grapher;
		if (network.equalsIgnoreCase("www")){
			grapher = session.sessioner.body.sitecacher;
		}else {
			Socializer provider = session.sessioner.body.provider(network);
			grapher = provider instanceof SocialCacher ? ((SocialCacher)provider).getGraphCacher() : null;
		}
		if (grapher == null)
			return null;
		Graph result = grapher.getSubgraph(ids, date, period, range, threshold, limit, format, links);

		//TODO: revert reciprocal links here before exporting?
		
		//translate subgraphs to JSON/AL, accordingly to "format"
		String out = format.equalsIgnoreCase("json") ? result.toJSON() : result.toString();
		
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
			session.output = "Not.";
			// AL: you parse <text>, format <format>, language <language>, features <words|phrases|emoticons> 
			String sdist = session.read(new Seq(new Object[]{"distance",new Property(arg,"distance")})) ? arg.getString("distance") : null; 
			String text = session.read(new Seq(new Object[]{AL.text,new Property(arg,AL.text)})) ? arg.getString(AL.text) : null; 
			if (!AL.empty(text)) {
				int distance = AL.empty(sdist) ? 1 : StringUtil.toIntOrDefault(sdist,10,1);
				Seq tokens = Parser.parse(text,AL.commas+AL.periods+AL.spaces,false,true,true,true);
				Seq restricted = restrict(tokens,session.sessioner.body.languages);				
				Set grams = Parser.grams(restricted,distance);
				session.output = "There text "+Writer.toString(text)+
						", tokens "+Writer.toString(tokens)+
						", grams "+Writer.toString(grams)+".";
				
			}
			return true;
		}
		return false;
	}
}
