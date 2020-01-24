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
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Environment;
import net.webstructor.data.LangPack;
import net.webstructor.data.SocialFeeder;
import net.webstructor.util.JSON;

class PayPalFeeder extends SocialFeeder {
	PayPal api;
	boolean debug = true;

	public PayPalFeeder(Environment body, PayPal api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		this.api = api;
	}

	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		cache();
		Set dates = api.payments.getSubKeySet(user_id);
		if (dates != null)for (Object date : dates) {
			String payment = (String)api.payments.getObject(user_id, date, false);
			countComments(payment,payment,null,(Date)date,1);
			countPeriod((Date)date,0,1);
		}
	}
	
	//TODO: move to PayPal
	//TODO: can be also invoked by resync 
	void cache() throws IOException {
		try {
			int timeout = 0;//TODO
			String access_token = PayPal.token(body, PayPal.auth_url(api.base_url()), timeout, api.appId, api.appSecret);
			String next_id = null;
			for (;;) {
				//https://developer.paypal.com/docs/api/get-an-access-token-curl/
				//https://developer.paypal.com/docs/api/payments/v1/#payment_list
				//https://developer.paypal.com/docs/integration/direct/payments/search-payment-details/
				String request = "?count=20&sort_by=create_time";
				if (next_id != null)
					request += "&start_id="+next_id;
				body.debug("PayPal request "+request+" "+access_token);
				String response = HTTP.simple(PayPal.payment_url(api.base_url())+request,null,"GET",timeout,null,new String[][] {
					{"Accept", "application/json"},
					{"Accept-language", "en_US"},
					{"Content-Type", "application/json"},
					{"Authorization","Bearer "+access_token}
					});
				body.debug("PayPal response "+response);
				JsonReader jsonReader = Json.createReader(new StringReader(response));
				JsonObject json = jsonReader.readObject();
				JsonArray payments = JSON.getJsonArray(json, "payments");
				next_id = JSON.getJsonString(json, "next_id");
				if (payments != null) for (int i = 0; i < payments.size(); i++) {
					Date date = null;
					String payer_id = null;
					JsonObject payment = payments.getJsonObject(i);
					String timestamp = JSON.getJsonString(payment,"update_time");
					if (timestamp != null) {
						date = Time.time(timestamp,"yyyy-MM-dd'T'HH:mm:ss'Z'");
						JsonObject payer = JSON.getJsonObject(payment,"payer");
						if (date != null && payer != null) {
							JsonObject payer_info = JSON.getJsonObject(payer,"payer_info");
							if (payer_info != null)
								payer_id = JSON.getJsonString(payer_info, "payer_id"); 
							if (payer_id != null) {
								JsonArray transactions = JSON.getJsonArray(payment,"transactions");
								if (transactions != null) for (int t = 0; t < transactions.size(); t++) {
									JsonObject transaction = transactions.getJsonObject(t);
									String description = JSON.getJsonString(transaction,"description");
									JsonObject amount = JSON.getJsonObject(transaction,"amount");
									if (amount != null) {
										String currency = JSON.getJsonString(amount, "currency");
//TODO: BigDecimal
										String total = JSON.getJsonString(amount, "total");
										Collection c = api.payments.getObjects(user_id, date);
										boolean found = !AL.empty(c);
										body.debug("Spidering peer paypal "+found+" "+payer_id+" "+total+" "+currency+" "+description);
										if (!found)
											api.payments.putObject(payer_id, date, description + " " + total + " " + currency);
									}
								}
							}
						}
					}
				}
				if (AL.empty(next_id))
					break;
			}//pagination
		} catch (Exception e) {
			body.error("Spidering peer paypal "+user_id, e);
		}
	}
}
