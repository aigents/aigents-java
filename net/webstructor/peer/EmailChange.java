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

import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.core.Property;
import net.webstructor.core.Thing;

class EmailChange extends Registration {	
	Peer temp = new Peer();
	String newmail = null;
	public boolean process(Session session) {
		Thing storedPeer = session.getStoredPeer();
		if (newmail == null) {
			Seq seq = new Seq(new Object[]{				
					new Any(1,AL.i_my),	
					new Any(new Object[]{
						new Seq(new Object[]{new Any(new Object[]{AL.email,"e-mail"}),new Property(temp,AL.email)})	
						})
					});	
			String oldmail = session.peer.getString(AL.email);
			String email;
			if (Reader.read(session.input, seq) && (email = temp.getString(AL.email)) != null) {
				if (oldmail.equalsIgnoreCase(email)) {
					session.output = "Ok.";
					session.mode = new Conversation();
					return false;
				}	
				try {
					if (!AL.empty(session.sessioner.body.storager.getByName(AL.email, email))){
						session.output = "Email "+email+" is owned.";
						return false;				
					}
				}catch(Exception e){
					session.sessioner.body.error("EmailChange",e);
					session.output = e.getMessage();
					return false;				
				}			
				emailCode(session,newmail = email);			
				session.output = emailNotification(newmail);
				return false;				
			}
			else {
				session.mode = new Conversation();
				return true;
			}
		} else {
			//code verified
			if (Reader.read(session.input, Reader.pattern(AL.i_my,new String[] {"verification code "+code}))) {
				session.peer.update(storedPeer,null);
				session.peer.setString(AL.email, newmail);				
				session.mode = new Conversation();
				//session.sessioner.body.debug("Email change to "+newmail+".");
				//session.sessioner.body.debug("New peer "+Writer.toString(session.peer)+".");
				//session.sessioner.body.debug("Old peer "+Writer.toString(storedPeer)+".");
				storedPeer.update(session.peer,null);
				session.output = "Ok. Your email "+session.peer.getString(AL.email)+".";
				return false;			
			} else 
			//reset verification
			if (session.mood == AL.interrogation && Reader.read(session.input, Reader.pattern(AL.i_my,new String[] {"verification code"}))){
				emailCode(session,newmail);	
				session.output = emailNotification(newmail);
				return false;	
			} else
			//just any question
			if (session.mood == AL.interrogation){
				return answer(session);
			}
		}
		if (Reader.read(session.input, cancel_pattern)) {//TODO: get rid of 'my logout' and 'my login' here
			session.mode = new Conversation();
			session.output = "Ok.";
			return false;
		}
		session.output = "What your verification code?";//TODO:if that is possible!?
		return false;
	}
}
