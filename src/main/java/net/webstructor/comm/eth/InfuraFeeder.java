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
package net.webstructor.comm.eth;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.core.Environment;
import net.webstructor.data.Graph;
import net.webstructor.data.LangPack;
import net.webstructor.data.Linker;
import net.webstructor.data.SocialFeeder;
import net.webstructor.util.Array;

class InfuraFeeder extends SocialFeeder {
	
	protected Ethereum api;
	public InfuraFeeder(Environment body, Ethereum api, String user_id, LangPack langPack, Date since, Date until) {
		super(body,user_id,langPack,false,since,until,null);
		this.api = api;
	}

	//@Override
	protected String getPeriodKey(Date times){
		//for Ethereum, daily breakdown so far, to get more detailed periods for limited duration
		return Time.day(times,false);
	}
	
/*
Ethereum resources:
https://www.etherchain.org/
https://etherscan.io/
https://github.com/ethereum/wiki/wiki/JavaScript-API
http://web3py.readthedocs.io/en/latest/web3.eth.html
https://github.com/ethereumjs/testrpc#usage
https://github.com/ethereum/go-ethereum/wiki/Sending-ether

Visual editor of Smart Contracts:
http://etherscripter.com/0-5-1/

Nodes:
https://www.ethernodes.org/network/1/nodes
use one running in parity since we are going to use it for our alpha (edited)
parity has a testnet called Kovan - It’s the best for our app since you dont have mining cost - but still public
https://kovan.etherscan.io

Reading Ethereum:
https://infura.io/#how-to - use to access other remote nodes via JSON-RPC
https://etherscan.io/token/EOS?a=0xd0a6e6c54dbc68db5db3a091b171a77407ff7ccf
https://etherscan.io/myapikey
https://infura.io/docs/#supported-json-rpc-methods

Ethereum name service:
https://ens.domains/

Test accounts:
0x70faa28a6b8d6829a4b1e649d26ec9a2a39ba413 (?)

0x32be343b94f860124dc4fee278fdcbd38c102d88 (should be many transactions)
->0x8daa225105a1ce85e9fcd23547e480b6b440a939 (1 in + 1 out)
  ->0x7ae17a0f6f8f02b5b6e76b327db15f91306194e6 (multiple transactions!!!) 

0x8fedf009bd8e250bc3345b4229a55216f129fd7d (good with not much links!!!)

Test contracts:
0xc8c6a31a4a806d3710a7b38b7b296d2fabccdba8 <- 0xffb8eb6673697c71e502314431fd15a8583431b2
0x8eB24319393716668D768dCEC29356ae9CfFe285 <- 0x9b9DE0C3bb10A7593E1C17036268B4dd4f3C81E1 AGI
*/        
	//TODO: potentially, move these crawling functions to sepatate SocailCrawler class/interface
	
	//polling for pending block creation
	//curl -d '{"jsonrpc":"2.0","method":"eth_getBlockByNumber","params":["0x5e56c4",true],"id":1}' https://mainnet.infura.io/<key>
	private static JsonObject getBlock(Environment env, Ethereum api, String block) throws Exception {
		String par = "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"eth_getBlockByNumber\", \"params\": [\""+block+"\",true]}";
		String response = "";
		for (int i = 0; i < 10; i++){//10 tries
			try {
				response = api.sendPost(api.getUrl(),par,HttpFileReader.TIMEOUT_MILLIS); 
				JsonReader jr = Json.createReader(new StringReader(response));
				JsonObject result = jr.readObject();
				if (result.containsKey("result")){
					JsonValue r = result.get("result");
					if (r != null && !r.toString().equals("null"))
						return result.getJsonObject("result");
				}
			} catch (Exception e) {
				env.error("Ethereum crawling error block "+block+" retry "+i+" from "+api.getUrl()+" response "+response,e);
			}
			Thread.sleep(10000*i);//10 secs
		}
		return null;
	}

	//curl -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' https://mainnet.infura.io/<key>
	//TODO: don't need this, just start from "latest" and move on from the block number
	static private long lastBlock(Ethereum api) throws Exception {
		String response = null;
		try {
			String par = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}";
			response = api.sendPost(api.getUrl(),par);
			//{"jsonrpc":"2.0","id":1,"result":"0x4861ce"}
			JsonReader jr = Json.createReader(new StringReader(response));
			JsonObject result = jr.readObject();
			String s = result.getString("result");
			return Long.decode(s).longValue();
		} catch (Exception e) {
			throw new Exception("Error getting last Ethereum block from "+api.getUrl()+" as "+response+":"+e.toString());
		}
	}
	
	private static final String[] nulls = {"null","0","0x","0x0","0x00"};
	
	private static final String test_id = "0x8fedf009bd8e250bc3345b4229a55216f129fd7d";

	static void updateGraphs(Environment body, Ethereum api, long start_block, Date since, Date until) {
		try {
			long age = 0;
			//String date_str = null;
			Date key = null;
			Graph graph = null;
			boolean pending_update = false;
			body.debug("Ethereum crawling start since "+since+" until "+until+" starting block "+start_block);
			long latest = lastBlock(api);
			for (long block = 0 < start_block ? start_block : latest; block > 0; block--){
				String blockhex = block == latest ? "latest" : "0x"+Long.toHexString(block);
				JsonObject result = getBlock(body,api,blockhex);
				if (result == null){
					body.error("Ethereum crawling error reading block "+block+"="+blockhex,null);
					break; 
				}
				long timestamp_s = Long.decode(result.getString("timestamp")).longValue();
				long timestamp_ms = timestamp_s * 1000;
				Date date = new Date(timestamp_ms);
				Date new_key = Time.date(date);
				body.debug("Ethereum crawling block "+block+"="+blockhex+" on "+date+" "+new_key+" date.compareTo(since)<0"+(date.compareTo(since) < 0));//TODO:remove debug
				if (date.compareTo(until) > 0){//skip if time is greater than until
					body.debug("Ethereum crawling block skip "+block+"="+blockhex+" on "+date+" "+new_key);
					continue;
				}
				if (date.compareTo(since) < 0){//break if time is less than since
					//TODO: never works?
					body.debug("Ethereum crawling block break "+block+"="+blockhex+" on "+date+" "+new_key);
					break;
				}
				
				if (key == null || !key.equals(new_key)){ 
					if (pending_update){
						body.debug("Ethereum graph saving for "+key+", age "+new Date((long)age)+" memory "+body.checkMemory());
						api.updateGraph(key,graph,age);
						pending_update = false;
					}
					key = new_key;
					graph = api.getGraph(key);
					body.debug("Ethereum graph loaded for "+key+", age "+new Date((long)graph.getAge())+" memory "+body.checkMemory());
				}
				if (age == 0)
					age = timestamp_ms;
				
				synchronized (graph) {
					if (graph.getAge() >= timestamp_ms){
						body.debug("Ethereum crawling graph age "+new Date((long)graph.getAge())+" exceeds "+new Date(timestamp_ms));
						break;
					}
					
					JsonArray ts = result.getJsonArray("transactions");
					if (ts != null && ts.size() > 0){
						for (int i = 0; i < ts.size(); i++){
							JsonObject t = ts.getJsonObject(i);
							if (!(t.containsKey("from") && t.containsKey("to") && t.containsKey("value")))
								continue;
							JsonValue jfrom = t.get("from");
							JsonValue jto = t.get("to");
							JsonValue jvalue = t.get("value");
							//this is needed because of glitchy https://infura.io returning semi-valid JSON
							if (!(jfrom != null && jto != null && !jto.toString().equals("null") && jvalue != null && !jvalue.toString().equals("null")))
								continue;
							try {
								String from = t.getString("from");
								String to = t.getString("to");
								BigInteger value = new BigInteger(t.getString("value").substring(2),16);
								int logvalue = 1 + value.doubleValue() == 0 ? 0 : (int)Math.round(Math.log10(value.doubleValue()));//should be non-zero
								String strvalue = value.doubleValue() == 0 ? null : new BigDecimal(value).divide(new BigDecimal(1000000000000000000L)).toString();
										
if (from.equals(test_id) || to.equals(test_id))
body.debug("Ethereum crawling test block "+block+"="+blockhex+" "+from+" "+to+" "+value+" "+(from.equals(test_id)?"out":"in"));
	
								String input = t.containsKey("input") ? t.getString("input") : null;
								if (AL.empty(input) || Array.contains(nulls, input))
									input = null;
								
								//value and no input => plain transfer to account or contract
								if (value.doubleValue() != 0 && input == null){
									api.log(date,block,"transfer",from,to,strvalue,input);
//TODO: configurable url?
									api.alert(date,block,"pays",from,to,strvalue,"https://etherscan.io/");
									graph.addValue(from, to, "pays", logvalue);//out
									graph.addValue(to, from, "paid", logvalue);//in
									pending_update = true;
								} else
								//input with value => call to smart contract with parameters and transfer
								//input with no value => call to smart contract with parameters and without transfer
								//no input with no value => call to smart contract without parameters and without transfer
								{
									api.log(date,block,"call",from,to,strvalue,input);
//TODO: configurable url?
									api.alert(date,block,"calls",from,to,strvalue,"https://etherscan.io/");
									graph.addValue(from, to, "calls", logvalue);//out
									graph.addValue(to, from, "called", logvalue);//in
									pending_update = true;
								}
							} catch (Exception e){
								body.error("Ethereum crawling transaction error:"+t,e);
							}
						}//transaction
					}//transactions
				}//graph
			}//blocks
			if (graph != null && pending_update){
				body.debug("Ethereum crawling graph saving for "+key+", age "+new Date((long)age)+" memory "+body.checkMemory());
				api.updateGraph(key,graph,age);
			}
			body.debug("Ethereum crawling stop");
		} catch (Exception e) {
			body.error("Ethereum crawling error",e);
		}
	}
		
	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {

		if (Period.daysdiff(since, until) > api.getPeriod())//limit to given number of days
			since = Time.date(until,-api.getPeriod());
		
		body.debug("Ethereum crawling graph for "+user_id);
		
		for (Date date = until; date.compareTo(since) >= 0; date = Time.date(date,-1)){
			body.debug("Ethereum crawling graph at "+date+" memory "+body.checkMemory());
			Graph graph = api.getGraph(date);
			
			if (graph == null)
				continue;//skip unknown dates
			
			//TODO: calculate similarity based on payment correspondents 
			
			synchronized (graph) {
				Linker payin = graph.getLinker(user_id, "paid", false);
				Linker payout = graph.getLinker(user_id, "pays", false);
				Linker callin = graph.getLinker(user_id, "called", false);
				Linker callout = graph.getLinker(user_id, "calls", false);
				if (payin != null)
					for (Iterator it = payin.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int amount = payin.value(key).intValue();
						countLikes(key,key,date,amount);
						countPeriod(date,amount,0);
					}
				if (callin != null)
					for (Iterator it = callin.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						int amount = callin.value(key).intValue();
						countComments(key,key,null,date,amount);
						countPeriod(date,0,amount);
					}
				if (payout != null)
					for (Iterator it = payout.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						countMyLikes(key,key,payout.value(key).intValue());
					}
				if (callout != null)
					for (Iterator it = callout.keys().iterator(); it.hasNext();){
						String key = (String)it.next();
						countMyLikes(key,key,callout.value(key).intValue());
					}
			}
		}
		body.debug("Ethereum crawling graph completed memory "+body.checkMemory());
	}

}
