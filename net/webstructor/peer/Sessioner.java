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

import java.util.Collection;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.comm.Communicator;
import net.webstructor.core.Thing;
import net.webstructor.util.MapMap;

/* user session can be identified as TCP/IP socket, 
 * HTTP cookie, IRC nickname, SMS phone number or email address, 
 * so that conversational context will be associated 
 * with a corresponding user 
 */
public class Sessioner {

	MapMap sessions = new MapMap();

	protected Body body;
	
	public Sessioner(Body body) {
		this.body = body;
	}

	public Session getSavedSession(Communicator communicator, String type, String key) {
		//get peer with session key from storage,
		try {
			Collection peers = body.storager.getByName(Peer.login_token,key);
			if (!AL.empty(peers) && peers.size() == 1) {
				//if found, create Session pre-initialized with the peer
				return new Session(this,communicator,type,key,(Thing)(peers.iterator().next()));
			}
		} catch (Exception e) {}
		return null;
	}
	
	public Session getSession(Communicator communicator, String key) {
		String type = communicator.getClass().getName();
		//TODO: synchronization!!!???
		Session session = (Session)sessions.getObject(type, key, true);
		if (session == null) {
			session = getSavedSession(communicator,type,key);
			if (session == null)
				session = new Session(this,communicator,type,key);
			sessions.putObject(type, key, session);
		}
		else
			session.communicator = communicator;
		return session;
	}
	
}
