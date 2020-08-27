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
import net.webstructor.al.Parser;
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
		String input = session.input();
		boolean complex = false;
		try {
		if ((session.mood == AL.declaration || session.mood == AL.direction) && session.authenticated()) {
			if (Reader.read(session.input(), new Any(1,AL.no)))
			{
					Statement query = session.reader.parseStatement(session,input,session.getStoredPeer());
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
						session.output(skipped > 0 ? "No. There things." : session.ok());
						return true;
					} else
						return false;//not handled
			} else {
				int totalUpdated = 0;
				Thing storedPeer = session.getStoredPeer();
				StringBuilder out = new StringBuilder();
				Parser parser = new Parser(input);
				while (parser.check() != null) {
					Statement statement = session.reader.parseStatement(storager,session,parser,storedPeer);
					if (!AL.empty(statement)) {
						Statement query = statement;
						complex = complex | query.complex();
						session.sessioner.body.output("Dec:"+Writer.toString(query)+".");			
						Query q = new Query(session.sessioner.body,storager,session.sessioner.body.self());
						int updated = q.setThings(query,storedPeer);
						//int updated = q.setThings(query,storedPeer,true);//smart things creation, but "NL" chat is not working
						//if (updated > 0) {
						if (updated > 0)
							totalUpdated += updated;
						{
							//out.append(out.length() > 0 ? " " : "").append(session.ok());
							out.append(out.length() > 0 ? " " : "").append(updated > 0 ? session.ok() : session.no());
							//TODO: do this peer saving and restoring more clever and not every time?
							//get stored session peer pointer just in case it is changed in background
							//get actual session peer parameters to access it another time
							session.peer.update(storedPeer,Login.login_context);
						}
					}
					if (parser.parseAny(AL.periods,true) == null)
						break;
				}
				//if (out.length() > 0 || complex) {//have results or complex query with no results
				if (totalUpdated > 0 || complex) {//have results or complex query with no results
					session.output(out.length() > 0 ? out.toString() : session.no());
					return true;//handled
				} else //simple query with no results
					return false;//not handled
			}
		} else if (session.mood == AL.interrogation && (Responser.noSecretQuestion(session) || session.authenticated())) {
//TODO: handle multiple (and mixed) statments in interrogation?
			Thing peer = Responser.getSessionAreaPeer(session);
			session.query = session.reader.parseStatement(session,input,peer);
			if (!AL.empty(session.query)){
				complex = session.query.complex();
				session.sessioner.body.output("Int:"+Writer.toString(session.query)+"?");
				String out = Responser.queryFilterFormat(session, peer, session.query);
				if (!AL.empty(out)){
					session.output(out);
					return true;
				} else if (input != null && input.toLowerCase().startsWith("what ")) {
//TODO fix ugly hack needed for "smart" bi-lingual AL/NL conversations
					if (complex){//not a parsed query complex enough to be answered?
						session.output(session.no());
						return true;
					}	
				}
			}
		} 
		} catch (Throwable e) {
			
			//TODO: letting Responser handle that!?
			if (!complex && e instanceof Mistake && e.getMessage().equals(Mistake.no_thing))
				return false;
			
			session.output(session.no()+" "+Responser.statement(e));//we do need such level of details on error!
			if (!(e instanceof Mistake))
				session.sessioner.body.error("Controller error " + e.toString(), e);
			return true;
		}
		return false;
	}

}

