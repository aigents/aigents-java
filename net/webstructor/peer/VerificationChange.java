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
import net.webstructor.al.Reader;
import net.webstructor.core.Thing;

class VerificationChange extends Registration {	
	public boolean process(Session session) {
		//if (code == -1 || session.mood == AL.interrogation){
		if (code == -1 || (session.mood == AL.interrogation && Reader.read(session.input, Reader.pattern(AL.i_my,new String[] {"verification code"})))){
			String email = session.peer.getString("email");
			emailCode(session,email);
			session.output = emailNotification(email);
			session.fails = 0;
			return false;
		} else
		if (session.mood == AL.interrogation && noSecretQuestion(session))
			return answer(session);
		else { //if (code != -1)
			Thing storedPeer = session.getStoredPeer();
			if (Reader.read(session.input, Reader.pattern(AL.i_my,new String[] {"verification code "+code}))) {
				session.peer.setString(Peer.secret_question,null);
				session.peer.setString(Peer.secret_answer,null);
				session.mode = new Registration();
				//TODO: consider that this peer update does not actually work ("no property - no change") 
				//TODO: but also consider this is rather good because answer/question are not lost of change is cancelled
				storedPeer.update(session.peer,null);
				return true;			
			}
		}
		if (Reader.read(session.input, cancel_pattern) || ++session.fails >= 3) {
			session.mode = new Login();
			session.clear();
			return true;
		}
		session.output = "What your verification code?";//TODO:if that is possible!?
		return false;
	}
}

