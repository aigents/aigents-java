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
import java.util.Date;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.agent.Body;
import net.webstructor.al.Writer;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Environment;
import net.webstructor.data.SocialFeeder;
import net.webstructor.data.Translator;
import net.webstructor.util.MapMap;
import net.webstructor.util.Reporter;

//TODO: merge PayPal+PayPaler? 
public class PayPal extends Socializer {
	String appId;
	String appSecret;

	transient MapMap payments = new MapMap(); //all latest cached trasactions
	
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
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		PayPalFeeder feeder = new PayPalFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}
	
	protected void reportPeer(Reporter rep, Translator t, SocialFeeder feeder,String title, String user_id, String name, String surname, java.util.Set options, int minPercent, int minCount, Object[][] cross_peers) {	
		String payments = Writer.capitalize(t.loc("payments"));
		rep.initPeer(user_id, Writer.capitalize(name), Writer.capitalize(surname), payments, feeder.since(), feeder.until());
		
		rep.table("payments",payments,
			t.loc(new String[]{"Period",null,null,"Number",payments}),
			feeder.getFriendsPeriods(),0,0);
		
		rep.closePeer();
	}

}
