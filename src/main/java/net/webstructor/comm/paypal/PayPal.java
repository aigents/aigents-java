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
package net.webstructor.comm.paypal;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.data.Translator;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Profiler;
import net.webstructor.util.JSON;
import net.webstructor.util.MapMap;
import net.webstructor.util.Reporter;

//TODO: merge PayPal+PayPaler? 
public class PayPal extends Socializer {
	String appId;
	String appSecret;

	public static final String monthly = "monthly";
	public static final String yearly = "yearly";
	
	transient MapMap payments = new MapMap(); //all latest cached trasactions
	transient private ReentrantLock busy = new ReentrantLock();
	
	public PayPal(Body body, String appId, String appSecret) {
		super(body);
		this.appId = appId;
		this.appSecret = appSecret;
	}

	@Override
	public String provider(){
		return "paypal";
	}
	
	String base_url() {
		return body.self().getString(Body.paypal_url,"https://api.paypal.com");
	}
	
	static String auth_url(String base_url) {
		return base_url + "/v1/oauth2/token";
	}
	
	static String payment_url(String base_url) {
		return base_url + "/v1/payments/payment/";
	}
	
	public static Date updateTerm(Date term, Date paid, String type) {
		if (paid != null) {
			if (term == null || term.compareTo(paid) <= 0)
				term = paid;
			if (type != null && type.toLowerCase().startsWith(yearly))
				return Time.addYears(term, 1);
			else
				return Time.addMonths(term, 1);
		}
		return term;
	}
	
	public void updatePeerTerm(String id, Date term) {
		try {
			Collection peers = body.storager.getByName(Body.paypal_id, id);
			if (!AL.empty(peers))
				((Thing)peers.iterator().next()).setString(Peer.paid_term,Time.day(term, false));
		} catch (Exception e) {
			body.error("PayPal no id "+id,e);
		}
	}
	
	public void updatePeerTerm(String id, Date paid, String type) {
		try {
			Collection peers = body.storager.getByName(Body.paypal_id, id);
			if (!AL.empty(peers) && peers.size() == 1) {
				Thing peer = (Thing)peers.iterator().next();
				Date term = Time.day(peer.getString(Peer.paid_term));
				term = updateTerm(term, paid, type);
				peer.setString(Peer.paid_term,Time.day(term, false));
			} else
				body.error("PayPal no unique peer id "+id,null);
		} catch (Exception e) {
			body.error("PayPal no peer id "+id,e);
		}
	}
	
	public static String token(Environment body, String auth_url, int timeout, String client_id, String client_secret) throws IOException {
		//https://www.paypal.com/apex/developer/expressCheckout/getAccessToken
		String auth_base64 = HTTP.auth_base64(client_id,client_secret);
		body.debug("PayPal request grant_type=client_credentials "+auth_base64);
		String response = HTTP.simple(auth_url,"grant_type=client_credentials","POST",timeout,null,new String[][] {
			{"Accept", "application/json"},
			{"Accept-language", "en_US"},
			{"Authorization",auth_base64},
			{"Content-Type", "application/x-www-form-urlencoded"}
			});
		body.debug("PayPal response "+response);
		JsonReader jsonReader = Json.createReader(new StringReader(response));
		JsonObject json = jsonReader.readObject();
		return HTTP.getJsonString(json,"access_token");
	}
	
	@Override
	public void resync(long sinceTimemillis) {
		try {
			if (busy.tryLock(60L, TimeUnit.SECONDS)) {
				updateCache(sinceTimemillis);
			    busy.unlock();
			} else
				body.debug("PayPal crawling skip");
		} catch (Exception e) {
			body.error("PayPal crawling resync error", e);
		}		
	}
		
	private void updateCache(long blockOrTimemillis) {	
		try {
			int timeout = 0;//TODO
			String access_token = PayPal.token(body, PayPal.auth_url(base_url()), timeout, appId, appSecret);
			String next_id = null;
			for (;;) {
				//https://developer.paypal.com/docs/api/get-an-access-token-curl/
				//https://developer.paypal.com/docs/api/payments/v1/#payment_list
				//https://developer.paypal.com/docs/integration/direct/payments/search-payment-details/
				String request = "?count=20&sort_by=create_time";
				if (next_id != null)
					request += "&start_id="+next_id;
				body.debug("PayPal request "+request+" "+access_token);
				String response = HTTP.simple(PayPal.payment_url(base_url())+request,null,"GET",timeout,null,new String[][] {
					{"Accept", "application/json"},
					{"Accept-language", "en_US"},
					{"Content-Type", "application/json"},
					{"Authorization","Bearer "+access_token}
					});
				body.debug("PayPal response "+response);
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray payobjects = JSON.getJsonArray(json, "payments");
				next_id = JSON.getJsonString(json, "next_id");
				if (payments != null) for (int i = 0; i < payobjects.size(); i++) {
					PayPalItem item = new PayPalItem(payobjects.getJsonObject(i));
					if (item.valid()) {
						Object found = payments.getObject(item.payer_id, item.date, false);
						String description_ex = item.description + " " + item.total + " " + item.currency;
						body.debug("Spidering peer paypal "+found+" "+item.payer_id+" "+item.date+" "+description_ex);
						if (found == null)
							payments.putObject(item.payer_id, item.date, description_ex);
						else {
							next_id = null;//weh have reached the top in the cache
							break;
						}
					}
				}
				if (AL.empty(next_id))
					break;
			}//pagination
		} catch (Exception e) {
			body.error("PayPal resync", e);
		}
	}

	@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		PayPalFeeder feeder = new PayPalFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(token, since, until, new StringBuilder());//updates feeder.term behind the scenes
		if (feeder.term != null)
			updatePeerTerm(id,feeder.term);
		return feeder;
	}
	
	protected void reportPeer(Reporter rep, Translator t, SocialFeeder feeder,String title, String user_id, String name, String surname, java.util.Set options, int minPercent, int minCount, Object[][] cross_peers) {	
		String payments = Writer.capitalize(t.loc("payments"));
		rep.initPeer(user_id, Writer.capitalize(name), Writer.capitalize(surname), payments, feeder.since(), feeder.until());
		
		rep.table("payments",payments,
			t.loc(new String[]{"Period",null,null,"Number",payments}),
			feeder.getFriendsPeriods(),0,0);
		
		Object[][] details;
		if (!AL.empty(details = feeder.getDetails()))
			rep.table("payments",payments,
				t.loc(new String[]{"Date",null,null,payments,null,null}),
				details,0,0);
		
		rep.closePeer();
	}

	@Override
	public Profiler getProfiler(Thing peer) {
		return new Profiler(body,this,peer,Body.paypal_id,Body.paypal_token,Body.paypal_key);
	}

}
