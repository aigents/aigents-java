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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.comm.Communicator;
import net.webstructor.core.Property;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.agent.Body;
import net.webstructor.al.*;

public class Session  {	

	//Writer writer;
	Reader reader;

	Sessioner sessioner;
	Thing peer = null;
	Communicator communicator;
	Mode mode = null;
	String key;
	String type;
	int mood = AL.declaration;
	int fails = 0;

	private boolean authenticated = false;
	
	//private ArrayList messages = new ArrayList();
	private String input = null;//raw input
	private String args[] = null;//pre-parsed input
	private String output = null;

	//protected HashMap expected = new HashMap();
	private String[] expected = null;

	public Session(Sessioner sessioner,Communicator communicator,String type,String key) {
		this.sessioner = sessioner;
		this.communicator = communicator;
		this.type = type;
		this.key = key;
		this.mode = new Conversation();
		this.reader = new Reader(sessioner.body);
		//this.writer = new Writer(sessioner.body);
	}

	public Session(Sessioner sessioner,Communicator communicator,String type,String key,Thing peer) {
		this(sessioner,communicator,type,key);
		this.peer = new Thing(peer,null);
		this.authenticated = true;
	}
	
	public boolean authenticated(){
		return this.authenticated;
	}
	
	public void logout(){
		this.peer = null;
		this.authenticated = false;
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
	
	public String[] args(){
		return args;
	}
	
	public void output(String output){
		this.output = output;
	}
	
	public void addOutput(String output){
		this.output += output;
	}
	
	public String output(){
		return output;
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
	
	public Thing getPeer() {
		return peer;
	}
	
	public int getMood() {
		return mood;
	}
	
	public String getKey() {
		return key;
	}
	
	boolean isSecurityLocal() {
		return communicator.getClass().getName().equals("net.webstructor.android.Talker")
			|| communicator.getClass().getName().equals("net.webstructor.gui.Chatter")
			;
	}

	//TODO: get down to only one get*Peer() !?
	public Thing getStoredPeer() {
		Collection peers = getStorager().get(new Thing(peer,Login.login_context));
		return AL.empty(peers)? null : (Thing)peers.iterator().next();
	}
	
	public Thing getSelfPeer() {
		Collection peers = (Collection)sessioner.body.self().get(AL.trusts);
		return AL.empty(peers) ? null : (Thing)peers.iterator().next(); 
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
				Thing areaPeer = getAreaPeer(area);
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
	public Thing getAreaPeer(Object area) {
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
							if (areaPeer != getStoredPeer()){
								//TODO: do we really need to promote channels that way?
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
		this.mood = reader.readMood(input);
		this.input = input; 
		this.args = Parser.split(this.input, " \t,;");//"compile it"
	}

	protected String express() {
		return output; 
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
	
	protected String welcome() {
		//update user login time, login count
		Thing peer = getStoredPeer();
		peer.set(Peer.login_time,Time.day(Time.today));
		int login_count = Integer.parseInt(peer.getString(Peer.login_count, "0"));
		peer.setString(Peer.login_count,String.valueOf(++login_count));
		
		//TODO: consider what to do if user logs in with multiple keys from different browsers
		peer.setString(Peer.login_token, this.key);
		
		communicator.login(this, peer);//set session conext to peer if needed

		sessioner.body.updateStatus(peer);//spawn status update process here asynchronously
		authenticated = true;

		return "Ok. Hello "+peer.getTitle(Peer.title)+"!"+"\nMy "+Body.notice();
	}

	protected void updateRegistration() {
		//update user registration time
		Thing peer = getStoredPeer();
		peer.set(Peer.registration_time,Time.day(Time.today));
		sessioner.body.updateStatus(peer);//spawn status update process here asynchronously
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
		session.mode = new Conversation();
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
			session.output += " " + Mode.statement(e);
			session.sessioner.body.error(provider+" error: "+e.toString(), e);
		}
	}
	
}