/*
 * MIT License
 * 
 * Copyright (c) 2005-2019 by Anton Kolonin, Aigents®
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
package net.webstructor.comm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Writer;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Session;

public abstract class SocialBinder extends Communicator {
	protected String name;
	protected int timeout = 0;
	
	public SocialBinder(Body body,String name){
		super(body);
		this.name = name;
	}

	//generate network email 
	protected abstract String getEmail(String id);		
	
	private String getUnusedEmail(Set<String> emails) throws Exception {
		if (!AL.empty(emails)) for (String email : emails) {
			Collection by_email = body.storager.getByName(AL.email, email);
			if (AL.empty(by_email))
				return email;
		}
		return null;
	}

	//TODO: make unified at the level of Session!?
	//TODO: login via Conversationer, like it is done for email!?
	//TODO: cosider this PayPal-only logic!?
	protected String bindUserEmail(String network,String id,String name,String surname,Set<String> emails){
		String network_id = network+"_id";
		String cap_name = Writer.capitalize(name);
		try {
			//find by network and id
			Collection by_id = body.storager.getByName(network_id, id);
			//if found, log in and exit
			if (AL.single(by_id)) {//if user is found by id, login and provide email for binding if needed
				Thing peer = (Thing)by_id.iterator().next();
				String email = peer.getString(AL.email);
				if (!AL.empty(email)) {//use existing email
					body.debug(cap_name+" found by id, has email "+email);
					return email;
				}
				email = getUnusedEmail(emails);//find unused email
				if (AL.empty(email))
					email = getEmail(id);
				peer.setString(AL.email, email);//TODO assign emailless peer email - here or in the other place?
				body.debug(cap_name+" found by id, assigned email "+email);
				return email;
			} else 
			if (AL.empty(emails)) {// if user is not found by id and no emails are given, need to create a new one with default email
				String email = getEmail(id);
				body.debug(cap_name+" not found by id, no email");
				return email;
			} else {// if user is not found by id, try to bind one by email
				//if verified
				//find user by any of emails
				HashSet<String> avail = new HashSet<String>();
				for (String email : emails) {
					//if found, bind (update name and surname if not set), log in and exit
					Collection by_email = body.storager.getByName(AL.email, email);
					if (AL.empty(by_email))//count available emails
						avail.add(email);
					if (AL.single(by_email)) {
						Thing p = (Thing)by_email.iterator().next();
						String existing_id = p.getString(network_id);
						if (AL.empty(existing_id)){
							p.setString(network_id, id);//bind user by first unused email
							body.debug(cap_name+" found by email "+email);
							return email;//return first unused email
						}
					}
				}
				String email = avail.size() > 0 ? avail.iterator().next() : getEmail(id);
				body.debug(cap_name+" not found by email, email "+email);
				return email;//if none is bound by email, return first unused email on emailless one
//TODO deal with not verified emails and accounts
				//if not verified
				//find email not matching a user
					//if found, create with that email, log in and exit
			}
		} catch (Exception e) {
			body.error(cap_name+" login error "+id, e);
		}
		return null;
	}

	protected Thing bindPeer(HTTPeer parent, String cookie, String id, String username, String lastname, String email, String token) {
		String cap_name = Writer.capitalize(name);
		Session session = body.sessioner.getSession(parent == null ? this : parent,cookie);
		if (session == null)
			body.debug(String.format("%s no session for %s %s %s",cap_name,id,username,lastname));
		else {
			if (session.getPeer() != null)
				body.debug(cap_name+" session peer "+session.getPeer().getTitle(Peer.title));
			if (session.getStoredPeer() != null)
				body.debug(cap_name+" session stored peer "+session.getStoredPeer().getTitle(Peer.title));
			if (session.authenticated() && !session.isSecurityLocal()) {
				String ok = session.bindAuthenticated(name+" id",id,name+" token",token);
        		body.debug("Reddit bind autheticated id="+id+" name="+username+" token="+token+" result="+ok);
        		return ok.equals(session.ok()) ? session.getStoredPeer() : null;
			}else{
				HashSet<String> emails = new HashSet<String>();
				if (AL.empty(email))
					email = getEmail(id);  
				emails.add(email);
	    		body.debug(cap_name+" binding email id="+id+" name="+username+" surname="+lastname+" email="+email);
				email = bindUserEmail(name,id,username,lastname,emails);
	    		body.debug(cap_name+" bound email id="+id+" name="+username+" surname="+lastname+" email="+email);
				if (!AL.empty(email)) {
	            	session.bind(name, id, token, email, username, lastname);
	            	if (!AL.empty(session.output())) {
	            		body.debug(cap_name+" bind result="+session.output());
	            		return session.getStoredPeer();
	            	}
	            	else
	            		body.debug(cap_name+" bind failed");
				}
			}
		}
		return null;
	}
	
}
