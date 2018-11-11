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

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Writer;

class Verification extends Registration {
	
	public boolean process(Session session) {		
		//TODO: this here or at the bottom!?
		//if (noSecretQuestion(session))
		//	return answer(session,session.getStoredPeer());
		
		Peer temp = new Peer();
		check(session);
		if (q == null || a == null) {
			session.mode = new Registration();
			return true;			
		}
		Seq seq = Reader.pattern(AL.i_my,temp,new String[] {q});	
		if (session.mood != AL.interrogation && Reader.read(session.input, seq)) {
			String answer = temp.getString(q);
			if (a.equalsIgnoreCase(answer)) {
				session.mode= new Conversation();
				session.output = session.welcome();
				session.authenticated = true;
				try {
					String email = session.peer.getString(AL.email);
					Peer.populateContent(session,Body.testEmail(email));
				} catch (Exception e) {
					session.output += " " + statement(e);
					session.sessioner.body.error(e.toString(), e);
				}
				session.fails = 0;
				return false;
			}
			if (++session.fails >= 3){
				session.mode = new Login();
				session.clear();
				return true;
			}
		}
		else {
			if (session.mood == AL.interrogation){
				//'what my $secret_question?' => back to verirfication
				if (q != null && session.read(Reader.pattern(AL.i_my,new String[] {q,Peer.secret_question,Peer.secret_answer}))){
					session.mode = new VerificationChange();
					return true;
				}
				//TODO: get out of this junk which causes dead loop!!!???
				//any other question => back to conversation
				//else{
				//	session.mode = new Conversation();
				//	return true;
				//}
			} else
			if (Reader.read(session.input, cancel_pattern)) {
				session.mode = new Login();
				session.peer = null;
				return true;
			}
		}
		session.output = Writer.what("your", session.peer, new All(new String[]{q}));
		return false;
	}
}
