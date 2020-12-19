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
package net.webstructor.comm.eth;

import java.io.IOException;
import java.util.Date;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.comm.SocialCacher;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.data.LangPack;
import net.webstructor.data.SocialFeeder;
import net.webstructor.data.DataLogger;
import net.webstructor.data.Translator;
import net.webstructor.main.Mainer;
import net.webstructor.peer.Profiler;
import net.webstructor.util.ReportWriter;

public class Ethereum extends SocialCacher {
	//TODO update url upon redirect!!!
	//protected String url;
	protected String key;
	//protected String name; //kind of Ethereum? 
	//protected GraphCacher cacher;
	protected DataLogger logger;
	
	public Ethereum(Body body, String name, String url, String key) {
		super(body,name,url);
		logger = new DataLogger(body,Writer.capitalize(name)+" crawling");
		this.key = body != null ? body.self().getString(name+" key",key) : key;
		//this.period = body != null ? new Period(body.self().getString(name+" period","4"),Period.DAY).getDays() : 4;
	}

	public String getUrl(){
		return AL.empty(url) || AL.empty(key) ? null : url.endsWith("/") ? url+key : url+"/"+key;
	}
	
	//TODO:@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		InfuraFeeder feeder = new InfuraFeeder(body,this,id,body.languages,since,until);
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}
	
	//virtual, unsynchronized applies for blockchain-s only
	//TODO:@Override
	protected void updateGraphs(long block, Date since, Date until){
		try {
			//InfuraFeeder feeder = new InfuraFeeder(body,this,null,body.languages,Time.today(-period),Time.today(+1));
			InfuraFeeder.updateGraphs(body,this,block,since,until);
			logger.close();
			cacher.saveGraphs();
		} catch (Exception e) {
			body.error(Writer.capitalize(name)+" crawling error",e);
		}
	}

	//TODO: move to separate Updater class (currently SocialCacher.write), don't rely on Body here!?
	void log(Date time, long block, String type, String from, String to, String value, String input){
		if (body == null)
			return;
		
		logger.write(name+"/"+name+"_"+Time.day(time,false)+".tsv",
				new Object[]{name,Time.linux(time),type,from,to,value,null,null,null,null,input,null,null,new Long(block)});
	}
	
	//for test only
	public static void main(String[] args) {
		
		//test bug for timestamp 1513867112!!!
		//0:body:2017-12-21-19-54-50.00:D:Ethereum crawling block 4771613=0x48cf1d on Thu Dec 21 14:38:32 WET 2017 2017-12-10
		//4770079 = 0x48c91f		
		//long timestamp = 1513867112;//Long.decode("0x5a3b6a68").longValue();
		//Date date = new Date((long)timestamp*1000);
		//String new_date_str = Time.day(date, false);	
		
		if (args.length < 2)
			return;
		String key = args[0];
		String user_id = args[1];
		Date since = args.length > 2 ? Time.day(args[2]) : null;
		Date until = args.length > 3 ? Time.day(args[3]) : null;
		Environment env = new Mainer();
		Ethereum eth = new Ethereum(null,"ethereum","https://mainnet.infura.io/",key);
		InfuraFeeder feeder = new InfuraFeeder(env,eth,user_id,new LangPack(env),since,until);
		try {
			feeder.getFeed(null, Time.today(-1), Time.today(+1), new StringBuilder());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ReportWriter rep = new ReportWriter(env,user_id+".html");
		rep.initReport("Aigents Report for Ethereum (beta)", feeder.since(), feeder.until(), null);
		rep.initPeer(user_id, null, null, null, since, until);

		Translator t = Translator.get("");
		rep.table("payers",t.loc("payers"), new String[]{"Rank,%","Payer","Log10(wei)-s out","Log10(wei)-s in"},feeder.getFans(),0,0);
		rep.table("payee",t.loc("payee"), new String[]{"Rank,%","Payee","Log10(wei)-s","Log10(wei)-s in"},feeder.getIdols(),0,0);
		
		rep.closePeer();
		rep.closeReport();
	}

	@Override
	public Profiler getProfiler(Thing peer) {
		return null;
	}

}//class

