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
package net.webstructor.agent;

import net.webstructor.al.AL;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;

public class Schema {
	
	public static final String self = "self";
	public static final String peer = "peer";
	public static String[] roots = {self, peer};
	public static String[] foundation = {self, peer, AL.time, AL.number, AL.money, AL.word, AL.daytime};
	//public static String[] keys = {AL.name,AL.email,Peer.surname,Peer.birth_date}; // key attributed for merging
	public static String[] keys = {AL.name,AL.email,Peer.surname,Peer.birth_date,Body.facebook_id,Body.google_id,Body.vkontakte_id}; // key attributed for merging
	public static String[] case_sensitive = {Body.email_password,Body.facebook_token,Body.google_key,Body.google_token,Body.vkontakte_key};
	public static String[] unique = {AL.email,Body.facebook_id,Body.google_id,Body.vkontakte_id};
	public static String[] thinkable = {Peer.social_relevance,Peer.relevance/*,"importance","similarity","authority","closeness","adherence"*/};
			
	private Storager storager;

	public Schema(Storager storager) {
		this.storager = storager;
		//new Thing(self).store(storager);
		//new Thing(peer).store(storager);	
		names(foundation);
		has(self,Body.properties);
		has(peer,Peer.properties);		
	}
		
	void names(String[] names) {
		for (int i=0; i<names.length; i++)
			try {
				new Thing(names[i]).store(storager);
			} catch (Exception e) {
				//TODO:body.error(e.toString(),e);
			}		
	}
	
	void has(String name, String[] names) {
		for (int i=0; i<names.length; i++)
			try {
				storager.add(name,AL.has,names[i]);
			} catch (Exception e) {
				//TODO:body.error(e.toString(),e);
			}		
	}
	
	//TODO: fix hack, make generic
	public static boolean quotable(String name) {
		return Array.contains(case_sensitive, name.toLowerCase());
	}

	public static String reverse(String name) {
		if (name.equals(AL.ignore))
			return AL.ignores;
		if (name.equals(AL.trust))
			return AL.trusts;
		if (name.equals(AL.share))
			return AL.shares;
		if (name.equals(AL.friend))
			return AL.friends;
		if (name.equals(AL._new))
			return AL.news;
		return null;
	}
	
	public static boolean unique(String name) {
		return Array.contains(unique, name.toLowerCase());
	}
	
	public static boolean thinkable(String name) {
		return Array.contains(thinkable, name.toLowerCase());
	}
}
