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

import java.io.IOException;
import java.util.HashMap;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Any;
import net.webstructor.al.Seq;
import net.webstructor.cat.StringUtil;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Property;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.util.Str;

//TODO move out	
class ReportRequest {
	Socializer provider = null;
	String id;
	String name;
	String surname;
	String token;
	String secret;
	String language;
	String format;
	boolean fresh;
	int threshold;
	String period;//TODO eliminate!?
	int report_days = 0;
	String[] areas;
}
	

class Reporter implements Intenter {

	public static final String name = "report";
	
	@Override
	public String name() {
		return name;
	}
	
	
	@Override
//TODO move this to superclass Asyncer (AsyncIntenter)
	public boolean handleIntent(final Session session) {
		final String[] args = session.args();
		if (AL.empty(args) || !session.authenticated())
			return false;
		
		final ReportRequest rr = parseReportRequest(session); 
		if (rr == null)
			return false;

		if (AL.empty(rr.id)) {
			session.output("No user.");
			return true;
		}

		//final int report_days = period == null ? 0 : StringUtil.toIntOrDefault(period,10,Body.RETROSPECTION_PERIOD_DAYS);
		String cached = rr.provider.getCachedReports(rr.id, rr.report_days, rr.format, rr.fresh);
		if (!AL.empty(cached)) {
			session.output(cached);
			return true;
		}
		
		if (session.read(new Seq(new Object[]{name,"results"})))
			if (session.status(name))
				return true;
		Thread task = new Thread() {
			public void run() {
	 			HashMap feeds = new HashMap();
				SocialFeeder feeder;
				String report = session.no();
				boolean ok = false;
				try {
					feeder = rr.provider.getFeeder(rr.id, rr.token, rr.secret, rr.report_days, rr.areas, rr.fresh, feeds);
					if (feeder != null){
						//cluster only if default or requested
						//TODO: fix this hack, do query parsing and clustering in other place!?
						java.util.Set options = Str.options(session.input(),Socializer.report_options);
						if (options.isEmpty() || options.contains(Socializer.my_interests) || options.contains(Socializer.interests_of_my_friends))
							feeder.cluster(Body.MIN_RELEVANT_FEATURE_THRESHOLD_PERCENTS);
						//sync report generation
						report = rr.provider.cacheReport(feeder,feeds,rr.id,rr.token,rr.secret,rr.name,rr.surname,rr.language,rr.format,options,rr.threshold);
						ok = true;
					}
				} catch (IOException e) {
					session.sessioner.body.error(name()+" "+rr.id+" "+"reporting error", e);
				}
				session.output(report);
				session.result(ok);
				session.complete(name);
			};
	    };
	    return session.launch(name,task,Socializer.wait_message,Socializer.wait_message);
	}

	//TODO: unifiy the code for both command versions!
	//TODO: "report" as a first word in the command!?
	private ReportRequest parseReportRequest(Session session) {
		ReportRequest rr = new ReportRequest(); 
		Thing arg = new Thing();
		if (session.read(new Seq(new Object[]{new Property(arg,"network"),"id",new Property(arg,"id"),"report"}))
				&& session.sessioner.body.getSocializer(arg.getString("network")) != null 
				&& arg.getString("id") != null && arg.getString("network") != null
				//if either a) provider is "public" or b) specified id is matching user id or c) we supply the auth token 
				&& (session.trusted() || session.sessioner.body.getSocializer(arg.getString("network")).opendata() || arg.getString("id").equals(session.getStoredPeer().getString(arg.getString("network")+" id"))
						|| session.read(new Seq(new Object[]{"token",new Property(arg,"token")}))) ) {
				rr.format = session.read(new Seq(new Object[]{"format",new Property(arg,"format")})) ? arg.getString("format") : "html"; 
				rr.threshold = session.read(new Seq(new Object[]{"threshold",new Property(arg,"threshold")})) ? Integer.valueOf(arg.getString("threshold")).intValue() : 20;
				//int range = session.read(new Seq(new Object[]{"range",new Property(arg,"range")})) ? Integer.valueOf(arg.getString("range")).intValue() : 1;
				rr.period = session.read(new Seq(new Object[]{"period",new Property(arg,"period")})) ? arg.getString("period") : null;
				rr.fresh = session.input().contains("fresh");
				rr.areas = session.read(new Seq(new Object[]{"areas",new Property(arg,"areas")})) ? new String[]{arg.getString("areas")} : null;
				rr.provider = session.sessioner.body.getSocializer(arg.getString("network"));
				Thing peer = session.getStoredPeer();
				rr.language = peer.getString(Peer.language);
				
				rr.id = arg.getString("id");
			   	rr.name = rr.id;
			   	rr.surname = "";
				rr.token = session.read(new Seq(new Object[]{"token",new Property(arg,"token")})) ? arg.getString("token") : null;
			   	if (AL.empty(rr.token)) //if token is not supplied explicitly
			   		rr.token = session.trusted() || arg.getString("id").equals(session.getStoredPeer().getString(arg.getString("network")+" id")) ? session.getStoredPeer().getString(rr.provider.name()+" token") : null;
				//TODO: name and language for opendata/steemit?
			   	rr.secret = rr.provider.getTokenSecret(session.getStoredPeer());
		} else	
		if (session.read(new Seq(new Object[]{
					new Any(1,AL.i_my),new Property(arg,"network"),"report"}))
					&& session.sessioner.body.getSocializer(arg.getString("network")) != null ) {
				rr.format = session.read(new Seq(new Object[]{"format",new Property(arg,"format")})) ? arg.getString("format") : "html"; 
				rr.threshold = session.read(new Seq(new Object[]{"threshold",new Property(arg,"threshold")})) ? Integer.valueOf(arg.getString("threshold")).intValue() : 20;
				//int range = session.read(new Seq(new Object[]{"range",new Property(arg,"range")})) ? Integer.valueOf(arg.getString("range")).intValue() : 1;
				rr.period = session.read(new Seq(new Object[]{"period",new Property(arg,"period")})) ? arg.getString("period") : null;
				rr.areas = session.read(new Seq(new Object[]{"areas",new Property(arg,"areas")})) ? new String[]{arg.getString("areas")} : null;
				rr.fresh = session.input().contains("fresh");
			   	rr.provider = session.sessioner.body.getSocializer(arg.getString("network"));
				Thing peer = session.getStoredPeer();
			   	rr.language = peer.getString(Peer.language);
			   	
			   	rr.id = peer.getString(rr.provider.getPeerIdName());
			   	rr.name = peer.getString(AL.name);
			   	rr.surname = peer.getString(Peer.surname);
			   	rr.token = peer.getString(rr.provider.name()+" token");
			   	rr.secret = rr.provider.getTokenSecret(session.getStoredPeer());
		} else
			return null;
		rr.report_days = rr.period == null ? 0 : StringUtil.toIntOrDefault(rr.period,10,0);
		return rr; 
	}
	
};
