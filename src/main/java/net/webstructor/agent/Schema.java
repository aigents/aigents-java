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
package net.webstructor.agent;

import java.util.Date;

import net.webstructor.al.AL;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;

public class Schema {
	
	public final static String self = "self";
	public final static String peer = "peer";
	public final static String[] roots = {self, peer};
	//TODO: repalace arrays with static HashTables initialized statically   
	public final static String[] foundation = {self, peer, AL.time, AL.number, AL.money, AL.word, AL.daytime};
	public final static String[] keys = {AL.name,AL.email,Peer.surname,Peer.birth_date,Body.google_id,Body.facebook_id,Body.vkontakte_id,Body.telegram_id,Body.telegram_name,Body.slack_id,Body.paypal_id,Body.reddit_id,Body.discourse_id,Body.twitter_id,AL.address}; // key attributed for merging
	public final static String[] hidden = {Peer.birth_date,Body.google_id,Body.facebook_id,Body.vkontakte_id,Body.telegram_id,Body.telegram_name,Body.slack_id,Body.paypal_id,Body.reddit_id,Body.discourse_id,Body.twitter_id}; // key attributed for merging
	public final static String[] case_sensitive = {Body.email_password,Body.facebook_token,Body.facebook_challenge,Body.telegram_name,Body.slack_token,Body.slack_key,Body.google_key,Body.googlesearch_key,Body.google_token,Body.vkontakte_key,Body.telegram_token,Body.reddit_id,Body.reddit_key,Body.reddit_token,Body.discourse_id,Body.twitter_id,Body.twitter_key,Body.twitter_key_secret,Body.twitter_token,Body.twitter_token_secret};
	public final static String[] unique = {AL.email,Body.google_id,Body.facebook_id,Body.vkontakte_id,Body.telegram_id,Body.telegram_name,Body.slack_id,Body.paypal_id,Body.reddit_id,Body.discourse_id,Body.twitter_id};
	public final static String[] thinkable = {Peer.social_relevance,Peer.relevance,AL.positive,AL.negative/*,"importance","similarity","authority","closeness","adherence"*/};
	public final static String[] multiples = new String[]{
		AL.is, 
		AL.has, 
		AL.topics,
		AL.sites,
		AL.areas,
		AL.trusts,
		AL.ignores,
		AL.shares,
		AL.friends,
		AL.queries,
		AL.clicks,
		AL.selections,
		AL.copypastes,
		AL.news,
		AL.things,
		AL.patterns,
		AL.responses,
		AL.members,
		AL.groups,
		AL.sources
	}; 
			
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
		if (name == null)
			return null;
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
		if (name.equals(AL.query))
			return AL.queries;
		if (name.equals(AL.click))
			return AL.clicks;
		if (name.equals(AL.selection))
			return AL.selections;
		if (name.equals(AL.copypaste))
			return AL.copypastes;
		return null;
	}
	
	//TODO: make more smart and widely used
	public static Class cls(String name) {
		return AL.times.equals(name) ? Date.class : String.class;
	}
	
	public static boolean unique(String name) {
		return Array.contains(unique, name.toLowerCase());
	}
	
	public static boolean thinkable(String name) {
		return Array.contains(thinkable, name.toLowerCase());
	}
}
