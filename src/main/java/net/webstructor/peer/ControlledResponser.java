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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Reader;
import net.webstructor.al.Statement;
import net.webstructor.al.Writer;
import net.webstructor.core.Mistake;
import net.webstructor.core.Query;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;

//AigentsLanguageResponser
class ControlledResponser implements Intenter {

	@Override
	public String name() {
		return "controlled";
	}
	
	@Override
	public boolean handleIntent(final Session session) {
		Storager storager = session.getStorager();
		if ((session.mood == AL.declaration || session.mood == AL.direction) && session.authenticated()) {
			if (Reader.read(session.input(), new Any(1,AL.not)))
			{
				try {
					Statement query = session.reader.parseStatement(session,session.input(),session.getStoredPeer());
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
						session.output(skipped > 0 ? "No. There things." : "Ok.");
					}
				} catch (Exception e) {
					session.output(Responser.statement(e));
					session.sessioner.body.error(e.toString(), e);
				}
				return true;
			} else
			try {
				Thing storedPeer = session.getStoredPeer();
				StringBuilder out = new StringBuilder();
				Collection message = session.reader.parseStatements(session,session.input(),session.getStoredPeer());
				for (Iterator it = message.iterator(); it.hasNext();) {
					Statement query = (Statement)it.next();
					session.sessioner.body.output("Dec:"+Writer.toString(query)+".");			
					Query q = new Query(session.sessioner.body,storager,session.sessioner.body.self());
					int updated = q.setThings(query,storedPeer);
					//int updated = q.setThings(query,storedPeer,true);//smart things creation, but "NL" chat is not working
					if (updated > 0){
						out.append(out.length() > 0 ? " " : "").append("Ok.");
						//TODO: do this peer saving and restoring more clever and not every time?
						//get stored session peer pointer just in case it is changed in background
						//get actual session peer parameters to access it another time
						session.peer.update(storedPeer,Login.login_context);
					}
				}
				if (out.length() == 0)
					return false;//not handled
				session.output(out.toString());
				return true;//handled
			} catch (Exception e) {
				
				//TODO: letting Responser handle that!?
				if (e instanceof Mistake && e.getMessage().equals(Mistake.no_thing))
					return false;
				
				session.output(Responser.statement(e));
				if (!(e instanceof Mistake))
					session.sessioner.body.error(e.toString(), e);
				return true;
			}
		} else if (session.mood == AL.interrogation && (Responser.noSecretQuestion(session) || session.authenticated())) {
			Thing peer = Responser.getSessionAreaPeer(session);
			try {
				session.query = session.reader.parseStatement(session,session.input(),peer);
				session.sessioner.body.output("Int:"+Writer.toString(session.query)+"?");
				String out = Responser.queryFilterFormat(session, peer, session.query);
				if (!AL.empty(out)){
					session.output(out);
					return true;
				}
			} catch (Throwable e) {
				session.output(Responser.statement(e));
				if (!(e instanceof Mistake))
					session.sessioner.body.error(e.toString(), e);
				return true;
			}
	  } 
	  return false;
	}

}

