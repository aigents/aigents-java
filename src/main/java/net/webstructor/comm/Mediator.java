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
package net.webstructor.comm;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Iter;
import net.webstructor.al.Parser;
import net.webstructor.al.Seq;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Thing;
import net.webstructor.core.Updater;
import net.webstructor.peer.Peer;
import net.webstructor.self.Matcher;
import net.webstructor.util.MapMap;

public abstract class Mediator extends Communicator implements Updater {
	protected String name;
	protected int timeout = 0;
	protected Matcher matcher;
	
	public Mediator(Body body,String name) {
		super(body);
		this.name = name;
		body.register(name, this);
		body.debug(Writer.capitalize(name)+" registered.");
		matcher = body.getMatcher();
	}

	protected String key(String chat_id,String from_id){
		return (new StringBuilder(name == null ? "" : name).append(':').append(chat_id).append(':').append(from_id)).toString();
	}
	
	/**
	 * @param key of form name:chat_id:from_id
	 * @return array of name, chat_id, from_id 
	 */
	protected String[] ids(String key){
		String[] ids = key.split(":");
		return ids != null && ids.length == 3 ? ids: null;
	}

	protected String getTokenSegment(String login_token, int n){
		if (!AL.empty(login_token)){
			String[] ids = Parser.split(login_token,":");
			if (ids != null && name.equals(ids[0]) && ids.length == 3)//facebook:psid:psuid
				return ids[n];
		}
		return null;
	}

	protected String getPeerSession(Thing peer){
		try {
			Collection sessions = body.storager.get(new Seq(new Object[]{AL.is,peer}),(Thing)null);
			if (!AL.empty(sessions)) for (Iterator it = sessions.iterator(); it.hasNext();){
				String psid = getTokenSegment(((Thing)it.next()).getString(Peer.login_token),1);
				if (psid!=null)
					return psid;
			}
		} catch (Exception e) {
			body.error(Writer.capitalize(name)+" error update peer "+peer.getTitle(Peer.title),e);
		}
		return null;
	}

	
	protected void mergePeer(Thing merger, Thing mergee, String[] names) {
		try {
			Collection groups = body.storager.getByName(AL.members, mergee);
			if (groups != null) for (Object g : groups) {
				((Thing)g).delThing(AL.members, mergee);
				((Thing)g).addThing(AL.members, merger);
			}
			mergee.copyTo(merger, names, null, true);
			boolean deleted = mergee.del();
			if (!deleted)
				body.error(Writer.capitalize(name)+" fails deleting "+mergee,null);
		} catch (Exception e) {
			body.error(Writer.capitalize(name)+" fails merging "+mergee,e);
		}
	}
	
//TODO: move to Grouper under Conversation scope for Slack and WeChat unification 
	// - adding session attributes?
	// - adding dedicated unauthorized chat sessions?
	protected void updateGroup(String group_id, String group_name, String peer_id, String peer_name, boolean is_in, boolean is_bot, String text){
		try {
			//1) get group by id (eg. "telegram_id")
			String name_id = name+" id";
			String full_group_name = name + ":" + group_name;
			Collection g = body.storager.getByName(name_id, group_id);
			Thing group;
			if (!AL.empty(g)){
				group = (Thing)g.iterator().next();
				group.setString(AL.name, full_group_name);
			}else{
				//2) if missed, create group with name
				group = new Thing(full_group_name);
				group.setString(name_id,group_id);
				group.storeNew(body.storager);
			}
body.debug(Writer.capitalize(name)+" channel name_id "+name_id+" group_name "+group_name+" group "+group.toString()+" peer_id "+peer_id+" text "+text);//TODO: remove debug
			//3) if (!is_bot), add/remove peer to/from group
			if (!is_bot){//TODO: handle bots as well!?
				//4) get peer by id (eg. "telegram_id")
				Collection p = body.storager.getByName(name_id, peer_id);
				Thing peer;
				if (!AL.empty(p)){
					peer = (Thing)p.iterator().next();
					if (is_in){
						group.addThing(AL.members, peer);
						peer.addThing(AL.groups, group);
					}else{ 
						group.delThing(AL.members, peer);
						peer.delThing(AL.groups, group);
					}
				}else{
					;//TODO: add new peers dynamically
				}
			}
			if (AL.empty(text))//skip empty texts (if users are just added or removed)
				return;
			//5) get all group users, do for each:
			Collection m = group.getThings(AL.members);
			if (!AL.empty(m)) for (Iterator mit = m.iterator(); mit.hasNext();){
				Thing p = (Thing)mit.next();
				//6) get all user topics, do for each
				Collection topics = p.getThings(AL.topics);
				Collection trusts = p.getThings(AL.trusts);
				if (!AL.empty(topics) && !AL.empty(trusts)){//keep trusted topics only
					topics = new HashSet(topics);
					topics.retainAll(trusts);
				}
				//7) match text against topic
				MapMap thingPaths = new MapMap();//collector
				Date today = Time.today(0);
				Iter parse = new Iter(Parser.parse(text));
				if (!AL.empty(topics)) for (Iterator tit = topics.iterator(); tit.hasNext();)
					matcher.match(parse, null, (Thing)tit.next(), today, full_group_name, null, thingPaths, null, null, null);
				//8) send update if topic is matched
//TODO: exclude sender in the news update
				body.getPublisher().update(null,today,thingPaths,true,group);//forced
			}
		} catch (Exception e) {
			body.error("Group "+name+" error", e);
		}
	}
	
	public boolean notifyable(Thing peer) {
		return peer.getBoolean(name+" notification");
	}	
}
