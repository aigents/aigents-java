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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.comm.Communicator;
import net.webstructor.core.Property;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.core.Updater;
import net.webstructor.data.Emotioner;
import net.webstructor.util.Str;
import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.*;

public class Session  {	

	//Writer writer;
	Reader reader;

	Sessioner sessioner;
	Thing peer = null;
	Thing session = null;//specific session context of the peer - peer's sub-personality
	Communicator communicator;
	Responser responser = null;
	String key;
	String type;
	transient int mood = AL.declaration;
	transient int fails = 0;
	transient Statement query = null;

	public String language = null;
	
	private boolean authenticated = false;
	
	private String input = null;//raw input
	private String args[] = null;//pre-parsed input
	private String output = null;
	
	private StringBuilder outputs = new StringBuilder();//TODO: use it to replace String output

	private String[] expected = null;

	transient private HashMap<String,Object> contexts = new HashMap<String,Object>();//keep handler-specific contexts
	
	public Session(Sessioner sessioner,Communicator communicator,String type,String key) {
		this.sessioner = sessioner;
		this.communicator = communicator;
		this.type = type;
		this.key = key;
		this.responser = sessioner.body.getResponser();
		this.reader = new Reader(sessioner.body);
		//this.writer = new Writer(sessioner.body);
	}

	public Session(Sessioner sessioner,Communicator communicator,String type,String key,Thing peer) {
		this(sessioner,communicator,type,key);
		this.peer = new Thing(peer,null);
		this.authenticated = true;
		persist();
	}
	
	public void clone(Session origin) {
		this.peer = origin.peer;
		this.authenticated = origin.authenticated;
	}
	
	public boolean authenticated(){
		return this.authenticated;
	}
	
	public void logout(){
		if (this.session != null)
			this.session.del();
		this.peer = null;
		this.session = null;
		this.authenticated = false;
		contexts.clear();
	}

	public Object context(String name){
		return contexts.get(name);
	}

	public void context(String name, Object context){
		if (context == null)
			contexts.remove(name);
		contexts.put(name, context);
	}

	public void expect(String[] expected){
		this.expected = expected;
	}
	
	public String[] expected(){
		return this.expected;
	}
	
	public String input(){
		return input;
	}
	
	public String toString(){
		return input();
	}
	
	public String[] args(){
		return args;
	}
	
	public int argsCount(){
		return args == null ? 0 : args.length;
	}
	
	public void output(String output){
		this.output = output;
	}
	
	public void outputWithEmotions(String output){
		String emotion = Emotioner.emotion(getBody().languages.sentiment(input()));//emotions on input?
		if (AL.empty(emotion))//no emotions on input - provide emotions on output?
			emotion = Emotioner.emotion(getBody().languages.sentiment(output));
		this.output = !AL.empty(emotion) ? output + "\n" + emotion : output;
	}
	
	public void addOutput(String output){
		this.output += output;
	}
	
	public String output(){
		return output;
	}
	
	//TODO: move out of session and apply translation!?
	public String no(){
		return Writer.capitalize(AL.no[0])+".";
	}

	public String ok(){
		return Writer.capitalize(AL.ok[0])+".";
	}

	public String so(){
		return Writer.capitalize(getBody().translator(language()).loc("so what"))+"?";
	}

	public String language(){
		String lang = getStoredPeer().getString(Peer.language);
//TODO make rather session language override use language in session.getLanguage()?
		return !AL.empty(lang) ? lang : this.language;
	}
	
	
	public void unexpect(Thing context){
		if (peer != null && !AL.empty(expected)){
			for (int i = 0; i < expected.length; i++){
				String actual = context.getString(expected[i]);
				if (!AL.empty(actual))
					input = input.replaceAll(actual, "");
			}
		}
	}
	
	public void clear(){
		peer = null;
		fails = 0;
		authenticated = false;
	}
	
	public String getString(String name) {
		String value = null;
		if (peer != null)
			value = peer.getString(name);
		if (AL.empty(value)) {
			Thing p = getStoredPeer();
			if (p != null)
				value = p.getString(name);
		}
		return value;
	}
	
	public Thing getPeer() {
		return peer;
	}
	
	public int getMood() {
		return mood;
	}
	
	public String getKey() {
		return key;
	}
	
	public boolean isSecurityLocal() {
		return communicator.getClass().getName().equals("net.webstructor.android.Talker")
			|| communicator.getClass().getName().equals("net.webstructor.gui.Chatter")
			;
	}

	//TODO: get down to only one get*Peer() !?
	public Thing getStoredPeer() {
		Collection peers = getStorager().get(new Thing(peer,Login.login_context));
		return AL.empty(peers)? null : (Thing)peers.iterator().next();
	}
	
	protected Thing getStoredSession() {
		if (session == null)
			return null;
		Collection sessions = getStorager().get(session);
		return AL.empty(sessions)? null : (Thing)sessions.iterator().next();
	}
	
	public Thing getSelfPeer() {
		return sessioner.body.getSelfPeer();
	}

	//TODO: move to storager?
	Collection getByStringOrThing(String name, Object obj){
		try {
			Collection set = obj instanceof String ? sessioner.body.storager.getByName(name,obj)
				: (obj instanceof Collection && !AL.empty((Collection)obj)) ? 
					sessioner.body.storager.get(name,((Thing)((Collection)obj).iterator().next())) : null;
			return set;
		} catch (Exception e) {
			sessioner.body.error("Get "+name+" "+obj+":",e);
			return null;
		}
	}
	
	/**
	 * Get an 'optinion leader' peer which may represent the subject domain area
	 * @return
	 */
	public Thing getAreaPeer() {
		if (peer != null){
			Object area = peer.get(AL.areas);
			if (area != null){
				Thing areaPeer = getAreaPeer(area,false);//TODO should canBeTheSame be true so no such parameter is needed?
				if (areaPeer != null)
					return areaPeer;
			}
		}
		return getSelfPeer();//if no explicit area, return first "root" peer 
	}

	/**
	 * Get an 'optinion leader' peer associated with area specifid as string name or as list of area objects  
	 * @param area
	 * @return
	 */
	public Thing getAreaPeer(Object area,boolean canBeTheSame) {
				//TODO:get rid of this ambiguity
				//try to get peer's areas as a string property or as a linked object's name
				Collection peers = getByStringOrThing(AL.areas,area);
				Collection sharers = getByStringOrThing(AL.shares,area);
				if (!AL.empty(peers) && !AL.empty(sharers)){
					//if many peers in this area, get the sharer
					//TODO: if many sharers, should merge results actually...
					if (!AL.empty(peers) && !AL.empty(sharers)) {
						peers = new HashSet(peers);
						peers.retainAll(sharers);
						//if found are holder and it is not the same as peer 
						for (Iterator it = peers.iterator(); it.hasNext();){
							Thing areaPeer = (Thing)it.next();
							if (canBeTheSame || areaPeer != getStoredPeer()){
								areaPeer.set(Peer.activity_time,Time.day(Time.today));//keep the area channel active
								return areaPeer;
							}
						}
					}
				}
				return null; 
	}
	
	public Body getBody() {
		return sessioner.body;
	}
	
	public Storager getStorager() { 
		return sessioner.body.storager;
	}

	protected void comprehend(String input) {
		if (input.startsWith("/"))//eliminate the leading slash for the case when it is a Telegram/Slack bot command
			input = input.substring(1);
		if (input.startsWith("my_") || input.startsWith("what_"))//hack for Telegram/Slack bot command
			input = input.replaceAll("_", " ");
		this.mood = reader.readMood(input);
		this.input = input; 
		//this.args = Parser.split(this.input, " \t,;");//"compile it"
		this.args = Parser.parseS(this.input,false);//"compile it"
	}

	protected synchronized void post(String message) {
		if (outputs.length() > 0)
			outputs.append("\n\n");
		outputs.append(message);
	}
	
	protected synchronized String express() {
		if (outputs.length() > 0)
			outputs.append("\n\n");
		outputs.append(output);
		output = null;
		String out = outputs.toString();
		outputs.setLength(0);
		return out;
	}
	
	protected boolean read(Set set) {
		return Reader.read(input, set);
	}
	
	protected int read(Thing arg,String[] props){
		return read(arg,props,null);
	}
	protected int read(Thing arg,String[] props,String[] defaults){
		int cnt = 0;
		if (arg != null && !AL.empty(props)) for (int i = 0; i < props.length; i++){
			String def = defaults != null && i < defaults.length ? defaults[i] : null;
			if (read(new Seq(new Object[]{props[i],new Property(arg,props[i],def)})))
				cnt++;
		}
		return cnt;
	}
	protected int readArgs(Thing arg,String[] props,String[] defaults){
		int cnt = 0;
		if (args != null && !AL.empty(props)) for (int i = 0; i < props.length; i++){
			String def = defaults != null && i < defaults.length ? defaults[i] : null;
			String name = props[i];
			String value = Str.arg(args, name, def);
			if (!AL.empty(value)) {
				arg.set(name, value);
				cnt++;
			}
		}
		return cnt;
	}
	
	protected void persist() {
		/**/
		Thing peer = getStoredPeer();
		session = getStoredSession();
		if (session == null){
			//prevent multiplication, check by key
			All query = new All(new Object[]{new Seq(new Object[]{AL.is,peer}),new Seq(new Object[]{Peer.login_token,key})});
			try {
				Collection sessions = getStorager().get(query,(Thing)null);
				if (!AL.empty(sessions))
					session = (Thing)sessions.iterator().next(); 
			} catch (Exception e) {
				sessioner.body.error("Session persist failed ",e);
			}
		}
		if (session == null) {//not found
			session = new Thing();
			session.store(sessioner.body.storager);
			session.addThing(AL.is, peer);
		} else {
			session.delThings(AL.is);
			session.addThing(AL.is, peer);
		}
		//TODO: rebind session peer, if needed!?
		//peer.addThing(AL.sessions, session);//TODO: point to sessions if we can resolve peer deleting issue 
		//TODO: login_time and expired sessions, but make sure that analytics is not broken then
		session.set(Peer.login_token, key);
		/**/
	}
	
	protected String welcome() {
		//update user login time, login count
		Thing peer = getStoredPeer();
		peer.set(Peer.login_time,Time.day(Time.today));
		int login_count = Integer.parseInt(peer.getString(Peer.login_count, "0"));
		peer.setString(Peer.login_count,String.valueOf(++login_count));
		
		//TODO: consider what to do if user logs in with multiple keys from different browsers
//TODO: remove login tokens from peers when ported update functions across messengers!
		peer.setString(Peer.login_token, this.key);
		persist();
		
		communicator.login(this, peer);//set session context to peer if needed

		sessioner.body.updateStatus(peer,null);//spawn status update process here asynchronously
		authenticated = true;

		return this.ok()+" Hello "+peer.getTitle(Peer.title)+"!"+"\nMy "+Body.notice();
	}

	protected void updateRegistration() {
		//update user registration time
		Thing peer = getStoredPeer();
		peer.set(Peer.registration_time,Time.day(Time.today));
		sessioner.body.updateStatus(peer,null);//spawn status update process here asynchronously
	}

	public boolean trusted() {
		Session session = this;
		Collection trusts = (Collection)session.getBody().self().get(AL.trusts);
		if (!AL.empty(trusts) && trusts.contains(session.getStoredPeer()))
			return true;
		return false;
	}

	public void login(String provider, Thing storedPeer) {
		Session session = this;
		session.sessioner.body.debug(provider+" auto-log: "+storedPeer);
		session.responser = session.sessioner.body.getResponser();
		session.peer = new Thing(storedPeer,null);
		session.output = session.welcome();
	}
	
	public void register(String provider, Thing peer, String email) {
		Session session = this;
		peer.store(session.sessioner.body.storager);
		session.sessioner.body.debug(provider+" auto-reg: "+peer);
		login(provider,peer);
		try {
			Peer.populateContent(session,Body.testEmail(email));
			session.updateRegistration();
		} catch (Exception e) {
			session.output += " " + Responser.statement(e);
			session.sessioner.body.error(provider+" error: "+e.toString(), e);
		}
	}

	public String bindAuthenticated(String idname,String idvalue,String tokenname,String tokenvalue){
		Session session = this;
		try {
			Collection owners = session.sessioner.body.storager.getByName(idname,idvalue);
			if (!AL.empty(owners) && (owners.size()>1 || !idvalue.equals(session.getStoredPeer().getString(idname,null))))
				return session.no() + " " + Writer.capitalize(idname+" "+idvalue+" is owned.");
		}catch(Exception e){
			session.sessioner.body.error("Social bind",e);
			return e.getMessage();
		}			
		session.getStoredPeer().set(idname, idvalue);
		session.getStoredPeer().set(tokenname, tokenvalue);
		return session.ok();
	}

	//bindNonAutheticated
	//email may be not available by settings if not confirmed or user registered with phone number 
	//or may be not required at all (vkontakte)
	public boolean bind(String provider,String id, String token, String email, String name, String surname){
		String Provider = Writer.capitalize(provider);//for debugging only
		Session session = this;
		String provider_id = provider+" id";//eg. facebook_id
		String provider_token = provider+" token";
		session.sessioner.body.debug(Provider+" verified: "+id+" "+email+" "+name+" "+surname+" "+token);
		Thing peer = new Thing(session.sessioner.body.storager.getNamed(Schema.peer),session.peer,null);
		peer.set(provider_id, id);
		peer.set(provider_token, token);
		if (!AL.empty(name)) 
			peer.set(AL.name, name.toLowerCase() );
		if (!AL.empty(surname)) 
			peer.set(Peer.surname, surname.toLowerCase());
		if (!AL.empty(email)) 
			peer.set(AL.email, email.toLowerCase());
		session.sessioner.body.debug(Provider+" looking for: "+peer.getTitle(Peer.title));
		Collection peers = session.sessioner.body.storager.get(peer,new String[]{provider_id});
		if (!AL.empty(peers))
			session.sessioner.body.debug(Provider+" found by id: "+((Thing)peers.iterator().next()).getTitle(Peer.title));
		if (AL.empty(peers))//TODO: get rid of this as email is unique now
			peers = session.sessioner.body.storager.get(peer,Peer.title);
		if (!AL.empty(peers))
			session.sessioner.body.debug(Provider+" found by name and email: "+((Thing)peers.iterator().next()).getTitle(Peer.title));
		if (AL.empty(peers))
			peers = session.sessioner.body.storager.get(peer,new String[]{AL.email});
		if (!AL.empty(peers))
			session.sessioner.body.debug(Provider+" found by email: "+((Thing)peers.iterator().next()).getTitle(Peer.title));
		if (AL.empty(peers))
			peers = session.sessioner.body.storager.get(peer,new String[]{AL.name,Peer.surname});//bind by name+surname only
		if (!AL.empty(peers))//let only unique identifcations!!!
			for (Iterator it = peers.iterator(); it.hasNext();)
				session.sessioner.body.debug(Provider+" found by name: "+((Thing)it.next()).getTitle(Peer.title));
		if (!AL.empty(peers) && peers.size() == 1) {//if matched, auto log in
			Thing storedPeer = (Thing)peers.iterator().next();
			storedPeer.set(provider_id, id);
			storedPeer.set(provider_token, token);
			session.login(provider,storedPeer);
			session.sessioner.body.debug(Provider+" updated "+storedPeer.getString(AL.email)+" id "+id);
			return false;
		} else {
			// if not matched, auto register
			session.register(provider, peer, email);
			session.sessioner.body.debug(Provider+" registered "+email+" id "+id);
			return false;
		}
	}
	
	//The following variables and methods are needed for session-bound asynchronous operations
	private boolean result = false;
	private boolean waiting = false;
	private boolean working = false;
	private long start_time = 0;
	private HashMap<String,String> results = new HashMap<String,String>();
	
	protected void result(boolean result) {
		synchronized (this) {
			this.result = result;
		}
	}
	protected boolean launch(String name,Thread task,long timeout_millis) {
		synchronized (this) {
			if (working) {
				output(Writer.capitalize(name)+" busy.");
				return true;
			}else {
			    waiting = true;
			    working = true;
				result = false;
				start_time = System.currentTimeMillis();
			    task.start();
			}
		}
	    synchronized (this) {
	    	while (waiting && System.currentTimeMillis() < (start_time + timeout_millis)) {
				try {
					wait(timeout_millis);
				} catch (InterruptedException e) {
					sessioner.body.error(Writer.capitalize(name)+" error",e);
				}
	    	}
	    	if (!waiting) {//completed timely
	    		return result;
	    	} else {//still working on it
	    		waiting = false;
	    		output(Writer.capitalize(name)+" working.");
	    		return true;
	    	}
	    }
	}
	
	protected boolean status(String name) {
	    synchronized (this) {
			if (working) {
				output(Writer.capitalize(name)+" busy.");
	    		return true;
			} else
	    	if (results.containsKey(name)) {
	    		String out = results.get(name);
	    		output(out);
	    		results.remove(name);
	    		return true;
	    	}
	    }
	    return false;
	}
	
	protected void complete(String name) {
		    synchronized (this) {
		    	working = false;
 		    	if (waiting) {//if still waiting
 		    		waiting = false;//complete
 		    		this.notify();
 		    	} else {
 		    		if (communicator instanceof Updater) {//async delivery
 		    			try {
							((Updater)communicator).update(getStoredPeer(), this.key, null, output(), null);
						} catch (IOException e) {
							sessioner.body.error(Writer.capitalize(name)+" update", e);
						}
 		    		}else{//Web REST interface with no async delivery
 		    			//post(output());//post for unspecific delivery of results aling with other content
 		    			results.put(name, output());//retain for specific retrieval of results by name
 		    		}
 		    	}
 		    }
	}
}
