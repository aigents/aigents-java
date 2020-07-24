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

import java.util.Collection;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Writer;
import net.webstructor.comm.Emailer;
import net.webstructor.comm.fb.FB;
import net.webstructor.comm.goog.GApi;
import net.webstructor.comm.vk.VK;
import net.webstructor.core.Thing;
import net.webstructor.util.Array;

class Login extends Responser {	
	static String[] login_context = new String[] {"email", "name", "surname"/*, "birth date"*/};
	
	private Thing singleRegisteredPeer(Session session) throws Exception {
		Collection peers = session.getStorager().get(new Seq(new Object[]{"is","peer"}),(Thing)null);
		Thing single = null;
		if (!AL.empty(peers))
			for (Iterator it = peers.iterator(); it.hasNext();) {
				Thing peer = (Thing)it.next();
				if (!AL.empty(peer.getString(Peer.secret_answer)) // if either of Q and A filled in, assume registered
					|| !AL.empty(peer.getString(Peer.secret_question))) {
					if (single == null) // first registered peer
						single = peer;
					else // more than one registered peer
						return null;
				}
			}		
		return single;
	}

	public boolean google(Session session) {
		GApi gapi = (GApi)session.sessioner.body.getSocializer("google");//TODO:validate type
		if (gapi != null) {
			String id = session.peer.getString(Body.google_id);
			String token = session.peer.getString(Body.google_token);
			//email,name,surname,token,id,refresh_token
			String enstir[] = gapi.verifyToken(id, token);
			if (enstir != null) {
				session.peer.set(Body.google_token, token);
				session.peer.set(Body.google_id, id = enstir[4]);
				session.bind("google",id,enstir[3],enstir[0],enstir[1],enstir[2]);//offline token,email,name,surname
				if (!AL.empty(enstir[5]))//refresh_token as google_key
					session.getStoredPeer().setString(Body.google_key, enstir[5]);
			} else { 
				session.sessioner.body.debug("Google failed: "+session.peer);
				session.output(session.no());
			}
		}
		return false;
	}

	public boolean vkontakte(Session session) {
		VK vk = (VK)session.sessioner.body.getSocializer("vkontakte");//TODO: validate type or make type-less 
		if (vk != null && session.input().indexOf("vkontakte_id=")==0){
			//update VK login upon callback
			String ensti[] = vk.verifyRedirect(session.input());
			if (ensti != null) {
				//TODO: refresh token?
				session.peer.set(Body.vkontakte_token, ensti[3]);//token
				if (AL.empty(ensti[0]))
					ensti[0] = session.peer.getString(AL.email,ensti[4]+"@vk.com");//id -> email
				session.bind("vkontakte",ensti[4],ensti[3],ensti[0],ensti[1],ensti[2]);//id,offline token,email,name,surname
			}
			//TODO:restrict origin for better security if passing token?
			session.output("<html><body onload=\"top.postMessage(\'"
					+(ensti == null ? "Error:" : "Success:")+session.input()+"\',\'*\');top.window.vkontakteLoginComplete();\"></body></html>");		
		}else
		if (vk != null) {
			String id = session.peer.getString(Body.vkontakte_id);
			String token = session.peer.getString(Body.vkontakte_token);
session.sessioner.body.debug("vkontakte: "+id+" "+token);			
			String enst[] = vk.verifyToken(id, token);
			if (enst != null) {
				//TODO: refresh token?
				session.peer.set(Body.vkontakte_token, token);
				if (AL.empty(enst[0]))
					enst[0] = session.peer.getString(AL.email,id+"@vk.com");
				session.bind("vkontakte",id,enst[3],enst[0],enst[1],enst[2]);//offline token,email,name,surname
			} else { 
				session.sessioner.body.debug("vkontakte login failed: "+session.peer);
				session.output(session.no());
			}
		}
		return false;
	}
	
	//TODO: don't touch now, change to unified framework (as for Google) later
	public boolean facebook(Session session) {
		String id = session.peer.getString(Body.facebook_id);
		String token = session.peer.getString(Body.facebook_token);
		String email = session.peer.getString(AL.email);
		session.sessioner.body.debug("Facebook login: "+email+"/"+id+"/"+token);
		FB fb = (FB)session.sessioner.body.getSocializer("facebook");//TODO: validate type or make typeless
		if (fb != null && (token = fb.verifyToken(id, token)) != null) {
			String[] me = fb.getMe(token);
			if (me != null) {
				//email may be not available by settings if not confirmed or user registered with phone number
				if (!AL.empty(me[0])) 
					email = me[0];
				if (AL.empty(email))
					email = id+"@facebook.com";
				String name = me[1];
				String surname = me[2];
				session.sessioner.body.debug("Facebook verified:"+name+"/"+surname+"/"+email);
				if (!AL.empty(name)) 
					session.peer.set(AL.name, name );
				if (!AL.empty(surname)) 
					session.peer.set(Peer.surname, surname);
				if (!AL.empty(email)) 
					session.peer.set(AL.email, email);
				//try existing facebook registration in different ways
				Collection peers = session.sessioner.body.storager.get(session.peer,new String[]{Body.facebook_id});
				if (AL.empty(peers))
					peers = session.sessioner.body.storager.get(session.peer,new String[]{AL.email,AL.name,Peer.surname});
				if (AL.empty(peers))
					peers = session.sessioner.body.storager.get(session.peer,new String[]{AL.email});
				if (AL.empty(peers))
					peers = session.sessioner.body.storager.get(session.peer,new String[]{AL.name,Peer.surname});
				if (!AL.empty(peers)) { // if matched, auto log in	
					Thing peer = (Thing)peers.iterator().next();
					session.sessioner.body.debug("Facebook auto-log:"+peer);
					peer.set(Body.facebook_id, id);
					peer.set(Body.facebook_token, token);
					session.responser = session.sessioner.body.getResponser();
					session.peer = new Thing(peer,null);
					session.output(session.welcome());
					return false;
				} else {
					// if not matched, auto register
					Thing peer = new Thing(session.sessioner.body.storager.getNamed(Schema.peer),session.peer,null).store(session.sessioner.body.storager);
					session.sessioner.body.debug("Facebook auto-reg:"+peer);
					peer.set(Body.facebook_id, id);
					peer.set(Body.facebook_token, token);
					session.responser= new Conversation();
					session.output(session.welcome());
					try {
						Peer.populateContent(session,Body.testEmail(email));
						session.updateRegistration();
					} catch (Exception e) {
						session.addOutput(" " + statement(e));
						session.sessioner.body.error("Facebook "+e.toString(), e);
					}
					return false;
				}
			}
		}
		session.sessioner.body.debug("Facebook fail:"+id+"/"+token);
		session.output(session.no());
		return false;
	}
	
	public boolean process(Session session) {
		if (!session.authenticated() && setProperties(session))
			return false;
		
		if (tryRSS(session.getStorager(),session))//if RSS feed tried successflly 
			return false;//no further interaction is needed
		
		if (session.mood == AL.interrogation && noSecretQuestion(session))
			return answer(session);
		
		if (session.peer == null) {
			session.peer = new Thing();//create dummy peer
		}

		if (session.mood == AL.declaration && !session.isSecurityLocal()){
			if (session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.facebook_id,Body.facebook_token},256))) {
				//session.read(Reader.pattern(AL.i_my,session.peer,new String[] {AL.email}));//optional?
				return facebook(session);
			}
			if (session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.google_id,Body.google_token},256))) {
				//session.read(Reader.pattern(AL.i_my,session.peer,new String[] {AL.email,AL.name,Peer.surname}));//optional?
				return google(session);
			}
			if (session.read(Reader.pattern(AL.i_my,session.peer,new String[] {Body.vkontakte_id,Body.vkontakte_token},256))) {
				session.read(Reader.pattern(AL.i_my,session.peer,new String[] {AL.email,AL.name,Peer.surname}));//optional?
				return vkontakte(session);//flow via Aigents Language (client token)
			}
			if (session.input().indexOf("vkontakte_id=")==0)
				return vkontakte(session);//flow via redirect (server token) 
		}
		
		//TODO: make this class-specific contextualization into virutal methods!
		//get email and name+surname+birth_date out of input
		if (session.communicator.getClass() == Emailer.class)//auto-login by email
			session.peer.setString(AL.email, session.key);

		//TODO:auto-login for single user - remove or extend for email case?
		if (session.isSecurityLocal()) {
			try {
				Thing peer = singleRegisteredPeer(session);
				if (peer != null) {
					session.responser= session.sessioner.body.getResponser();
					session.peer = new Thing(peer,null);
					session.output(session.welcome());
					return false;
				}
			} catch (Exception e) {} //sad, so...			
		}

		if (session.mood == AL.declaration || session.mood == AL.direction) {
			//using $ to explicitly denote variables
			//AL-style sentence
			//[{i my} {[name $name] [email $email] [surname $surname] [birth date $birth_date]}]
			if (!Reader.read(session.input(), Reader.pattern(AL.i_my,session.peer,login_context))){
				//freetext-style sentence
				//[$email $name $surname]
				if (session.expected() != null)
					Reader.read(session.input(), Reader.pattern(session.peer,session.expected()),",");
				if (!AL.empty(session.expected()))
					session.unexpect(session.peer);
			}
			session.sessioner.body.output("Login:"+Writer.toString(session.peer)+".");
		}
		
		//Identification by email is expexted to be unique
		//Collection peers = session.sessioner.body.storager.get(session.peer,login_context);
		Collection peers = null;
		if (!AL.empty(session.peer.getString(AL.email))){//checking by email only!!!
			peers = session.sessioner.body.storager.get(session.peer,new String[]{AL.email});
			if (!AL.empty(peers) && peers.size() > 1)
				peers = session.sessioner.body.storager.get(session.peer,new String[]{AL.email,AL.name});
		}else{//checking by name if email is not present
			peers = session.sessioner.body.storager.get(session.peer,new String[]{AL.name,Peer.surname});
			if (AL.empty(peers))
				peers = session.sessioner.body.storager.get(session.peer,new String[]{AL.name});
			//if found only one in storager and there is email
			if (AL.empty(peers) || peers.size() != 1 || 
				AL.empty(((Thing)peers.iterator().next()).getString(AL.email)))
				peers = null;
		}
		//TODO: streamline code above and below
		
		if (!AL.empty(peers) && peers.size() == 1) {
			session.peer = new Thing((Thing)peers.iterator().next(),null);
			//if registered -  Verification
			if (session.peer.getString(Peer.secret_answer) != null){
				session.responser= new Verification();
				session.expect(new String[]{session.peer.getString(Peer.secret_question)});
			}else{
			//if not registered - Registration
				session.responser= new Registration();
				session.expect(null);
			}
			return true;
		}
		
		//check for unresolved variables
		All unresolved = new All(Array.sub(login_context, session.peer.getNamesAvailable()));
		
		//if found many 
		if (!AL.empty(peers) && peers.size() > 1) {
			//if no unresolved - Error
			if (unresolved.size() == 0) {
				session.output("Too many peers.");//TODO:what?
				session.expect(null);
				return true;
			}
			//if unresolved - keep unresolving
		}
		//if found none
		if (AL.empty(peers)) {
			//if no unresolved - add - Registration
			if (unresolved.size() == 0) {
				Collection cls = session.sessioner.body.storager.getNamed(Schema.peer);
				Thing storedPeer = new Thing(cls,session.peer,null).store(session.sessioner.body.storager);
				//TODO: do this default initialization in some other place!
				//must initialize parameter sheets for peers anyway otherwise they can't be quaried by ALL-kind "open" queries
				storedPeer.setString(Peer.check_cycle,"3 hours");//TODO:move out of here?

				session.responser = new Registration();
				session.expect(null);
				return true;
			}
			//if unresolved - keep unresolving
		}

		if (Reader.read(session.input(), cancel_pattern)) {
			unresolved = new All(login_context);
			session.peer = null;
		}
		
		//ask all items at once:
		session.expect(unresolved.toStrings());
		session.output(Writer.what("your", session.peer, unresolved));
		//ask every item one-by-one:
		//session.output = Writer.what("your", session.peer, new All(new Object[]{unresolved.get(0)}));
		return false;			
	}
}

	

