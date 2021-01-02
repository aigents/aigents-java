/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents®
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Environment;
import net.webstructor.data.LangPack;
import net.webstructor.data.SocialFeeder;
import net.webstructor.util.JSON;


class PayPalItem {
	Date date = null;
	String payer_id = null;
	String description = null;
	String currency = null;
	String total = null;
	PayPalItem(JsonObject payment){
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
						description = JSON.getJsonString(transaction,"description");
						JsonObject amount = JSON.getJsonObject(transaction,"amount");
						if (amount != null) {
							currency = JSON.getJsonString(amount, "currency");
//TODO: BigDecimal
							total = JSON.getJsonString(amount, "total");
						}
					}
				}
			}
		}
	}
	boolean valid() {
		return date != null && !AL.empty(payer_id) && !AL.empty(total); 
	}
}


class PayPalFeeder extends SocialFeeder {
	PayPal api;
	boolean debug = true;
	Date term = null; 

	public PayPalFeeder(Environment body, PayPal api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until);
		this.api = api;
	}

	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		api.resync(0);
		Set dates = api.payments.getSubKeySet(user_id);
		if (dates != null) {
			int i = 0;
			Object[][] items = new Object[dates.size()][]; 
			for (Object date : dates) {
//TODO:filter in since-until range
				String payment = (String)api.payments.getObject(user_id, date, false);
				countComment(payment,payment,null,(Date)date,1);
				countPeriod((Date)date,0,1);
				items[i++] = new Object[] {date,payment};
			}
			Arrays.sort(items,new Comparator(){
				public int compare(Object arg0, Object arg1) {
					return ((Date)((Object[])arg0)[0]).compareTo((Date)((Object[])arg1)[0]);
				}
			});
			for (Object[] item: items) {
				term = PayPal.updateTerm(term,(Date)item[0],(String)item[1]);
				reportDetail(detail,
						"",//getUserName(from),
						"",//uri
						"",//id
						item[0].toString() + " " +item[1].toString()+ " - "+term.toString(),
						(Date)item[0],
						null,
						null,
						null,//likers,
						0,//likes_count-user_likes,
						0,//user_likes,
						0,
						null);
			}
		}
	}

	@Override
	public Socializer getSocializer() {
		return api;
	}

}
