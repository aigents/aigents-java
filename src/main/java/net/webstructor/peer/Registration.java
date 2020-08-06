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
import java.util.Random;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Any;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Reader;
import net.webstructor.al.Seq;
import net.webstructor.al.Writer;
import net.webstructor.comm.Emailer;
import net.webstructor.core.Property;
import net.webstructor.core.Thing;

class Registration extends Responser {	

	public static final String[] question_answer = new String[]{Peer.secret_question,Peer.secret_answer};
	public static final String[] answer_only = new String[]{Peer.secret_answer};
	
	String q = null;
	String a = null;

	protected String emailNotification(String email){
		return "What your verification code? Sent to "+email+".";
	}
	
	protected void check(Session session) {
		//whether have question q and answer a
		q = session.peer.getString(Peer.secret_question);
		a = null;
		if (q != null) {
			if (q.length() < 1)
				q = null;
			else {
				a = session.peer.getString(Peer.secret_answer);
				if (a != null) 
					if (a.length() < 1)
						a = null;
			}
		}
	}	
	
	protected int code = -1;
	protected int code() {
		Random rand = new Random();
		return 1111 + rand.nextInt(9)*1000 + rand.nextInt(9)*100 + rand.nextInt(9)*10 + rand.nextInt(9);
	}
	protected void emailCode(Session session,String email) {
		//dummy stub for unit testing
		//if (email.equalsIgnoreCase("doe@john.org") || email.equalsIgnoreCase("john@doe.org"))
		if (Body.testEmail(email))
			code = 1234;
		else {//send email otherwise 
			code = code(); //for verification
			try {
				//TODO: consider if this email swaping is safe (otherwise amended peer is not updated later)
				//set new email temporarily
				String oldmail = session.peer.getString(AL.email);
				session.peer.setString(AL.email,email);
				Emailer.getEmailer().output(session, "Verification code "+code, "Your verification code "+code+".\n"+session.sessioner.body.signature());
				//recover original email back till confirmation
				session.peer.setString(AL.email,oldmail);
			} catch (IOException e) {
				session.getBody().error(e.toString(),e);
			}
		}
	}
	
	public boolean process(Session session) {
		if (session.mood == AL.interrogation && noSecretQuestion(session))
			return answer(session);

		if (Reader.read(session.input(), cancel_pattern)) {
			session.responser = new Login();
			session.peer = null;
			session.expect(null);
			return true;
		}
		
		//states: have no question, have question with no answer, have question and answer
		//if have no question
		//  try to read question, ask question if not read
		//if have question with no answer
		//	try to read answer, ask answer if not read
		//if have question and answer
		//  conversation
		Thing storedPeer = session.getStoredPeer();
		check(session);
		//if (session.input().equals("My secret question password."))
		//	System.out.println(session.input());
		if (q == null) {
			Property pq = new Property(session.peer,Peer.secret_question);
			Property pa = new Property(session.peer,Peer.secret_answer);
			Seq seq = new Seq(new Object[]{new Any(1,AL.i_my),new Any(new Object[]{new Seq(new Object[]{"secret","question",pq}),new Seq(new Object[]{"secret", "answer"  ,pa})})});
//TODO rewrite this with AL pattern!?
			if (session.mood != AL.interrogation && !Reader.read(session.input(), seq))//well-formed AL for q and a
				if (session.expected() != null){
					//free-text like "passport number, 123456 querty"
					if (!Reader.read(session.input(), Reader.pattern(session.peer, session.expected(),",")))
						//free-text like "sky color?, blue" (breaker in the middle)
						if (!Reader.read(new Iter(new Seq(Parser.split(session.input(), ","))),Reader.pattern(session.peer, session.expected()),null,true) || AL.empty(session.peer.getString(Peer.secret_answer)))
							//free-text like "password,123456querty"
							Reader.read(session.input(),Reader.pattern(session.peer, session.expected()),",");
				}
			check(session);
			if (q == null){
				session.expect(question_answer);
				session.output( Writer.what("your", session.peer,new All(question_answer)) );
				return false;
			}
			if (a == null){
				session.output( Writer.what("your", session.peer, new All(answer_only)) );
				session.expect(answer_only);
				return false;
			}
		}
		if (q != null && a == null) {
			//TODO: spaces breakdown for "pet name"!
			Seq seq = new Seq(new Object[]{
					new Any(1,AL.i_my),	
					new Any(new Object[]{
						//can read either way
						new Seq(new Object[]{q,new Property(session.peer,q)}),	
						new Seq(new Object[]{"secret", "answer",new Property(session.peer,Peer.secret_answer)})	
						})	
					});	
			if (!Reader.read(session.input(), seq))
				if (session.expected() != null)
					Reader.read(session.input(),Reader.pattern(session.peer, session.expected()));//free-text
			check(session);
			if (a == null){
				session.output( Writer.what("your", session.peer, new All(answer_only)) );
				session.expect(answer_only);
				return false;
			}
		}
		if (q != null && a != null) {
			session.updateRegistration();
			session.responser= new Verification();
			storedPeer.update(session.peer,null);
			session.expect(new String[]{q});
			return true;
		}
		
		return false;
	}
}

