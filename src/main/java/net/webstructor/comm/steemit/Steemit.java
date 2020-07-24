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
package net.webstructor.comm.steemit;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.agent.Body;
import net.webstructor.main.Mainer;
import net.webstructor.peer.Peer;
import net.webstructor.peer.Profiler;
import net.webstructor.self.Matcher;
import net.webstructor.util.JSON;
import net.webstructor.util.MapMap;
import net.webstructor.util.Str;
import net.webstructor.core.Environment;
import net.webstructor.core.Thing;
import net.webstructor.cat.HttpFileReader;
import net.webstructor.comm.HTTP;
import net.webstructor.comm.SocialCacher;
import net.webstructor.comm.Socializer;
import net.webstructor.data.DataLogger;
import net.webstructor.data.Graph;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.LangPack;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;

/*
Integration with Steemit
https://steemblockexplorer.com/
http://steemroll.io/api-explorer/
https://golos.io/ru--golos/@maxim/intellektualnyi-chat-bot-v-golose-kak-sposob-obshatsya-na-platforme
https://steemit.github.io/steemit-docs/
https://steemit.com/theoretical/@theoretical/how-to-use-the-steem-api
https://steemit.com/steemjs/@joomla-tips/steemit-rest-api-documentation-part-1
http://steem-o-graph.com/?u=anm

https://vk.com/away.php?to=https%3A%2F%2Fsteemit.github.io%2Fsteemit-docs%2F

https://steemit.github.io/steemit-docs/#introduction
http://steemroll.io/api-explorer/#method=get_account_history&params=["akolonin","20","10"]
https://steemit.com/@akolonin/feed
https://steemit.com/steem/@mahnunchik/steem-for-linux-v0-12-2-binaries-wallet-and-miner
curl --data '{"jsonrpc": "2.0", "method": "call", "params": [0,"get_content",["matrixdweller","trying-to-access-the-steem-blockchain-from-unity-part-1-fail"]], "id": 4}' https://this.piston.rocks
curl --data '{"jsonrpc": "2.0", "method": "call", "params": [0,"get_content",["matrixdweller","trying-to-access-the-steem-blockchain-from-unity-part-1-fail"]], "id": 4}' http://node.steem.ws:80/rpc
curl --data '{"jsonrpc": "2.0", "id":25,"method":"get_account_history","params":["akolonin","2","1"]}' http://node.steem.ws:80/rpc
[<account>,<from>,<count>]
example
<from> = -1
<maxblocksize> = 1000
do  
 get from:<from>,count:<blocksize>-1
 if <min_sequence> > 1
    <from> = <min_sequence> - 1
    <blocksize> = min(<blocksize>,<from>)

?
curl --data '{"jsonrpc": "2.0", "id":25,"method":"get_accounts","params":["akolonin","2","1"]}' http://this.piston.rocks

curl --data '{"jsonrpc": "2.0", "id":25,"method":"get_trending_categories","params":["", 1000]}' http://this.piston.rocks
curl --data '{"jsonrpc": "2.0", "id":25,"method":"get_trending_categories","params":["f", 1000]}' http://this.piston.rocks
curl --data '{"jsonrpc": "2.0", "id":25,"method":"get_trending_categories","params":["", 1000]}' http://this.piston.rocks
    
Вот эта нода сейчас "более" основная для Стима: this.piston.rocks 
curl --data '{"jsonrpc": "2.0", "method": "call", "params": [0,"get_content",["matrixdweller","trying-to-access-the-steem-blockchain-from-unity-part-1-fail"]], "id": 4}' http://this.piston.rocks/rpc

curl --data '{"jsonrpc": "2.0", "id":25,"method":"get_accounts","params":["akolonin","2","1"]}' https://this.goloscore.org


Golos (CyberWay): 
	https://golos.io
From @gropox (on Telegram)
	Nodes with API:
		https://github.com/cyberway/cyberway.launch/blob/master/apinodes
	API:
		https://developers.eos.io/eosio-nodeos/v1.2.0/reference
		example: get_table_rows

Golos (OLD): 
https://golos.id
@Jackvote [27 нояб. 2019 г., 15:34:32]:
https://ropox.app/steemjs/api/database_api/get_block
wss://golos.lexa.host/ws
wss://golos.solox.world/ws
wss://api.golos.blckchnd.com/ws
wss://denisgolub.name/ws
wss://api-full.golos.id/ws
wss://lindsay-golos.cf/ws
https://api-full.golos.id (OK) - hard rate limits, needs 10 seconds waits, resync takes 8 hours  
https://api.golos.blckchnd.com (OK)
https://golos.lexai.host (not tested yet)
Тут же и размеры блоков можно посмотреть на разных нодах.

Golos Support: 
@Mariaminas Мария, Голос Кор
https://t.me/goloscoretc
Golos Docs:
http://ropox.tools/steemjs/api/
https://docs.google.com/document/d/1kms6fmzcUg18-SemUyJU9tRKt-853orXcdb16m1NGTE/mobilebasic#id.pw2zkoufcr2z
https://golos.io/ru--golos/@goloscore/novosti-golos-core-status-razrabotki-na-05-02-2018-tekhnicheskie-izmeneniya-informaciya-o-bounty-programme
Golos API Hosts:
https://golos.id/nodes
https://api-golos.blckchnd.com - public, working with new API on 2020-07-20
https://api.golos.blckchnd.com - not working with new API on 2020-07-20
this.goloscore.org - Could not resolve host: this.goloscore.org
https://ws.golos.io - worked in curl (with cert) - not working on 2015-05-15
https://ws.golos.io - ask in chat golos.io, 
https://api.golos.cf - worked in curl (with cert) - not working on 2015-05-15
https://api.golos.cf - ask @vikxx on Telegram 
wss://ws17.golos.io/ - not working on 2015-05-15
Golos Working:
curl --data '{"jsonrpc":"2.0","id":"25","method":"call","params": ["account_history","get_account_history",["akolonin","-1","1000"]]}' https://api-full.golos.id
curl --data '{"jsonrpc":"2.0","id":"25","method":"call","params": ["account_history","get_account_history",["akolonin","-1","1000"]]}' https://api-golos.blckchnd.com
curl --data '{"jsonrpc":"2.0","id":"25","method":"call","params": ["database_api","get_block",["33119661"]]}' https://api.golos.blckchnd.com/
curl --data '{"jsonrpc":"2.0","id":"25","method":"call","params": ["database_api","get_block",["33119661"]]}' https://api-full.golos.id
curl --data '{"jsonrpc":"2.0","id":"25","method":"call","params": ["database_api","get_dynamic_global_properties",[]]}' https://api-full.golos.id

Golos Not Working:
curl --data '{"jsonrpc":"2.0","id":"25","method":"call","params": ["account_history","get_account_history",["akolonin","-1","1000"]]}' https://api-full.golos.id
curl --data '{"jsonrpc":"2.0","id":"25","method":"get_account_history","params": ["akolonin","-1","1000"]}' https://api.golos.cf
curl --data '{"jsonrpc":"2.0","id":"25","method":"get_account_history","params": ["akolonin","-1","1000"]}' https://ws.golos.io

Steemit:
https://api.steemjs.com - Not Found
http://this.piston.rocks - hangs
https://steemd.steemit.com - hangs
https://api.steemit.com - works
curl --data '{"jsonrpc":"2.0","id":"25","method":"get_account_history","params": ["akolonin","-1","1000"]}' https://api.steemit.com
curl --data '{"jsonrpc":"2.0","id":"25","method":"get_block","params": ["0"]}' https://api.steemit.com
*/

public class Steemit extends SocialCacher {
	//TODO update url upon redirect!!!
	//protected String url;
	protected HttpFileReader reader;//TODO: move up to HTTP or fix native HTTP
	protected String base_url;
	
	public Steemit(Body body, String name, String url) {
		super(body,name,url);
		//this.name = name;
		//this.url = body != null ? body.self().getString(name+" url",url) : url;
		this.reader = new HttpFileReader(body);
		base_url = base_url(name());
	}

	@Override
	public String name(){
		return name;
	}
	
	@Override
	public boolean opendata() {
		return true;
	}
	
	//TODO:@Override
	public SocialFeeder getFeeder(String id, String token,  String key, Date since, Date until, String[] areas) throws IOException{
		SteemitFeeder feeder = new SteemitFeeder(body,this,id,body.languages,since,until,areas,0);//0 - default period
		feeder.getFeed(token, since, until, new StringBuilder());
		return feeder;
	}

	static String base_url(String name) {
		return name.equals("steemit") ? "https://steemit.com" : "https://golos.in";
	}
	
	static String permlink_url(String base_url, String parent_permlink, String author, String permlink) {
		return (AL.empty(parent_permlink)) ? null : base_url + "/" + parent_permlink + "/@" + author + "/" + permlink;
	}
	
	//TODO:@Override
	public void resync_tmp() {
		//get list of recently active peers
		Set peers = null;
		//get retention period
		Date since = null;
		Date until = null;
		//get optional areas
		String[] areas = null;
		//get default range
		int range = 6;//six handshakes 
		//pass peroid and list to spidering
		//get processing timeout limit
		long timeout = 0;

		//TODO: areas - specific graph caches!?
		
		resync(name, this, body, body.languages, cacher, peers, since, until, areas, range, timeout);
	}

	//TODO: decide how to make it working incrementally on daily basis!?
	
	//- given one seed account or list of "recently active" seed accounts
	//- given look-back period
	//- given range radius (R of expansion cycles)
	//- given timeout to stop expansion even if R is not reached 
	//- keeping registry of "already spidered at date" accounts
	//- keep "latest daily timestamp" per peer to prevent redundancy
	protected static void resync(String name, Steemit api, Environment env, LangPack lp, GraphCacher gc, Set peers, Date since, Date until, String[] areas, int range, long timeout) {
		String caps_name = Writer.capitalize(name);
		if (AL.empty(peers))
			return;
		
		long start = System.currentTimeMillis();
		env.debug(caps_name+" resyncing "+name+" start "+new Date(start)+".");
		
		gc.setAge(start);
		
//TODO: make it possible to read synced graphs while updating them!?

		//create set of input users
		HashSet todo = new HashSet(peers);
		//create set of processed users
		HashSet done = new HashSet();
		int count = 0;
		for (int r = 0; r < range; r++){
		
//TODO: check timeout			
			
			//create set of users to do in the next round
			HashSet next = new HashSet();
			for (Iterator it = todo.iterator(); it.hasNext();) {
				//for each name
				String id = (String)it.next();
				//create feed for each user
				//TODO:
				
//TODO: how to do this incrementally freshed!?
//				boolean fresh = false;
				
//TODO: make sure the peer id is not present in every daily graphs, unless it has to be fresh!?
				
				env.debug(caps_name+" crawling "+name+" user "+id);
				try {
					//get feed (yearly period since postProcessPosts is overridden)
					SteemitGraphFeeder feeder = new SteemitGraphFeeder(gc,env,api,id,lp,since,until,areas,365);
					feeder.getFeed(null, since, until, null);
					count++;
					
//merge feed to graph with ***daily*** metrics
//TODO: in postProcessPosts
					
					//extract users from feed and add unprocessed users to users to expand
					java.util.Set pids = feeder.getPeersIds();
					//add users to expand to set of input users
					for (Iterator ids = pids.iterator(); ids.hasNext();){
						String pid = (String)ids.next();
						if (!pid.equals(id) && !done.contains(pid))
							next.add(pid);
					}
				} catch (IOException e) {
					env.error("Steemit resyncing error "+name, e);
				}
				env.debug(caps_name+" spidered "+name+" user "+id);
				//add name to processed list
				done.add(id);
			}
//TODO: check timeout
			todo = next;//start over
		}
		
		//flush all updated graphs in gc
		try {Thread.sleep(1);} catch (InterruptedException e) {}//wait 1 second to update the age to work for sure 
		gc.saveGraphs();
		
    	long end = System.currentTimeMillis();
		env.debug(caps_name+" resyncing "+name+" end "+new Date(end)+", count "+count+", took "+Period.toHours(end-start)+", peer "+Period.toHours((end-start)/count)+".");
	}
	
	public static String retryPost(Environment env,String api_url,String par) throws Throwable {
		return retryPost(env, api_url, par, 100L);
	}
	public static String retryPost(Environment env,String api_url,String par, long pause) throws Throwable {
		String response = null;
		for (int retry = 1; retry <= 10; retry++){
			response = HTTP.simple(api_url,par,"POST",0);
			if (response.startsWith("<html>")){
				env.error("Steemit response "+api_url+" "+par+" "+response,null);
				Thread.sleep(pause * retry);
			} else 
				break;
		}
		return response;
	}
	
	public static long headBlock(Environment env, String api_url,String api_name) throws Throwable {
		String par = "steemit".equals(api_name) ? 
				"{\"jsonrpc\":\"2.0\",\"id\":\"25\",\"method\":\"get_dynamic_global_properties\",\"params\": []}"//Steemit
				:"{\"jsonrpc\":\"2.0\",\"id\":\"25\",\"method\":\"call\",\"params\": [\"database_api\",\"get_dynamic_global_properties\",[]]}";//Golos
		String response = retryPost(env, api_url, par);
		JsonReader res = Json.createReader(new StringReader(response));
		JsonObject obj = res.readObject();
		JsonObject result = obj.getJsonObject("result");
		if (result == null)
			return 0;
		return result.getJsonNumber("head_block_number").longValue();
	}

	static String recode(String body){
		//TODO: proper re-coding
		body = body.replace("\n","\\n");
		body = body.replace("\t","\\t");
		body = body.replace("\r","");
		return body;
	}
	
	protected void updateGraphs(long block, Date since, Date until) {
		try {
			//Option A: block-wise crawling - total but slow 
			blockSpider(this, body, name, url, block, false);
			
			/*
			//Option B: peer-wise crawling - need to explore 
			Set peers = null;
			//get optional areas
			String[] areas = null;
			//get default range
			int range = 6;//six handshakes 
			//pass peroid and list to spidering
			//get processing timeout limit
			long timeout = 0;
			resync(name, this, body, body.languages, cacher, peers, since, until, areas, range, timeout);
			*/
			
			cacher.saveGraphs();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			body.error(Writer.capitalize(name)+" crawling error",e);
		}
	}
		
	@Override
	public Profiler getProfiler(Thing peer) {
		return new Profiler(body,this,peer, name()+" id");
	}

	public static void blockSpider(Steemit api, Environment env, String api_name, String api_url, long start_block, boolean debug) throws Throwable {
		String caps_name = Writer.capitalize(api_name);

//get cacher from env!!!
		//GraphCacher grapher = new GraphCacher(api_name,env);
		Socializer socializer = env instanceof Body ? ((Body)env).getSocializer(api_name) : null;
		Matcher matcher = env instanceof Body ? ((Body)env).getMatcher() : new Matcher(null);
		GraphCacher grapher = socializer instanceof SocialCacher ? ((SocialCacher)socializer).getGraphCacher() : new GraphCacher(api_name,env);

		String site = base_url(api_name);
		DataLogger logger = new DataLogger(env,Writer.capitalize(api_name)+" crawling");
		long time_start = System.currentTimeMillis();
		long age = 0;
		Date key = null;
		Graph graph = null;
		boolean pending_update = false;
		long head = start_block > 0 ? start_block : headBlock(env, api_url, api_name);
		long pause = "golos".equals(api_name) ? 1000 : 100;

		long start = System.currentTimeMillis(); 
		env.debug(caps_name+" crawling start");// since "+since+" until "+until);
		
		//set up peer topic for news monitoring 
		Set peerThings = null; 
		MapMap thingPaths = new MapMap();//collector
		int thingMatches = 0;
		Date attention_date = api == null ? null : Time.today(-api.body.attentionDays());
		if (api != null) {
			String name_id = api.name()+" id";
			Collection candidates = api.body.storager.getAttributed(name_id);
			if (candidates != null) {
				Collection peers = new ArrayList();
				for (Object candidate : candidates) {
					Thing peer = (Thing)candidate;
					Date activity_date = peer.getDate(Peer.activity_time,null);//ignore inactive peers
					if (!AL.empty(peer.getString(name_id)) && attention_date.compareTo(activity_date) < 0)
						peers.add(peer);
				}
				if (!AL.empty(peers))
					peerThings = Peer.peerTopics(peers);
			}
		}
		
		for (long block = head; block > 0; block--){
			String par = "steemit".equals(api_name) ? 
				"{\"jsonrpc\":\"2.0\",\"id\":\"25\",\"method\":\"get_block\",\"params\": [\""+ block + "\"]}"//Steemit
				:"{\"jsonrpc\":\"2.0\",\"id\":\"25\",\"method\":\"call\",\"params\": [\"database_api\",\"get_block\",[\"" + block + "\"]]}";//Golos
			String response = null;
			
			try {
				if (debug)
					env.debug(Writer.capitalize(api_name)+" request "+api_url+" "+par);
				response = retryPost(env, api_url, par, pause);
				JsonReader res = Json.createReader(new StringReader(response));
				JsonObject obj = res.readObject();
				JsonObject result = JSON.getJsonObject(obj, "result");
				if (result == null){//no result
					env.debug(caps_name+" crawling block "+block+" no result:"+Str.first(response,200));
					//break;//no blocks anymore!?
					continue;
				}
				String timestamp = result.getString("timestamp");
				//Date date = Time.time(timestamp,SteemitFeeder.TIME_FORMAT);
				Date date = Time.time(timestamp,"yyyy-MM-dd'T'HH:mm:ss");
				long timestamp_ms = date.getTime();
			
//TODO: break on date range!?
				
env.debug(caps_name+" crawling block "+block+" at "+timestamp);
if (block % 10 == 0){
	long time_curr = System.currentTimeMillis();
	long time_diff = time_curr - time_start;
	long blocks = head - block + 1;
	double speed = ((double)blocks) * 60000 / time_diff;
	env.debug(caps_name+" crawling speed "+speed+" blocks/minute at "+blocks+" blocks");
}

				//TODO: break on block age recorded in daily graphs
				Date new_date = Time.date(date);
				if (key == null || !key.equals(new_date)){ 
					if (pending_update){
						env.debug(caps_name+" graph saving for "+key+", age "+new Date((long)age*1000)+" memory "+env.checkMemory());
						if (api != null) 
							api.updateGraph(key,graph,age);
						else
							grapher.updateGraph(key,graph,age);
						pending_update = false;
					}
					key = new_date;
					graph = api != null ? api.getGraph(key) : grapher.getGraph(key);
					env.debug(caps_name+" graph loaded for "+key+", age "+new Date((long)graph.getAge()*1000)+" memory "+env.checkMemory());
				}
				if (age == 0)
					age = timestamp_ms;

//TODO:indent			
			synchronized (graph) {
				if (graph.getAge() >= timestamp_ms){
					env.debug(caps_name+" crawling graph age "+new Date((long)graph.getAge())+" exceeds "+new Date(timestamp_ms));
					break;
				}

				JsonArray transactions = result.getJsonArray("transactions");
				if (transactions != null) for (int t = 0; t < transactions.size(); t++){
					JsonObject transaction = transactions.getJsonObject(t);
					JsonArray operations = transaction.getJsonArray("operations");
					if (operations != null) for (int o = 0; o < operations.size(); o++){
						JsonArray operation = operations.getJsonArray(o);
						String type = operation.getJsonString(0).getString();
						if (debug)
							env.debug(caps_name+" crawling block "+block+" at "+timestamp+" transaction "+t+" operation "+o+" type "+type);
						JsonObject args = operation.getJsonObject(1);
						if (type.equals("claim_reward_balance") ||
							type.equals("donate") ||//TODO!: Golos:["donate",{"from":"prizm","to":"id-ogon-sochi","amount":"1000.000 GOLOS","memo":{"app":"golos-id","version":1,"target":{"author":"id-ogon-sochi","permlink":"est-predlozhenie"},"comment":""},"extensions":[]}]
							type.equals("claim") ||//TODO?: Golos:["claim",{"from":"sci-cards","to":"ksantoprotein","amount":"0.036 GOLOS","to_vesting":false,"extensions":[]}]
							type.equals("transfer_from_tip") ||//Golos:["transfer_to_tip",{"from":"now","to":"now","amount":"16.500 GOLOS","memo":"","extensions":[]}]
							type.equals("transfer_to_tip") ||//Golos:["transfer_from_tip",{"from":"lex","to":"lex","amount":"9764.987 GOLOS","memo":"","extensions":[]}]
							type.equals("update_proposal_votes") ||//Steemit:["update_proposal_votes",{"voter":"kottalam","proposal_ids":[0],"approve":true,"extensions":[]}]
							type.equals("account_update2") ||//Steemit:["account_update2",{"account":"olivepainting","json_metadata":"","posting_json_metadata":"{\"profile\":{\"name\":\"OlivePainting\",\"website\":\"https://www.instagram.com/olive_painting/\",\"version\":2}}","extensions":[]}]
							type.equals("delete_comment") || //TODO: later!?
							type.equals("comment_options") || //TODO: later!?
							type.equals("custom_json") || //TODO: follows!?
							type.equals("feed_publish") ||
							type.equals("account_create") ||
							type.equals("account_update") ||
							type.equals("account_create_with_delegation") ||
							type.equals("escrow_dispute") ||
							type.equals("escrow_release") ||
							type.equals("escrow_approve") ||
							type.equals("escrow_transfer") ||
							type.equals("limit_order_cancel") ||
							type.equals("limit_order_create") ||
							type.equals("limit_order_create2") ||
							type.equals("account_witness_vote") || //TODO: later!?
							type.equals("account_witness_proxy") || //TODO: later!?
							type.equals("witness_set_properties") ||
							type.equals("witness_update") ||
							type.equals("delegate_vesting_shares") ||
							type.equals("set_withdraw_vesting_route") ||
							type.equals("withdraw_vesting") ||
							type.equals("transfer_to_vesting") ||
							type.equals("transfer_from_savings") ||
							type.equals("cancel_transfer_from_savings") ||
							type.equals("transfer_to_savings") ||
							type.equals("pow2") ||
							type.equals("convert") ||
							type.equals("recover_account") ||
							type.equals("account_metadata") ||
							type.equals("claim_account") ||
							type.equals("create_claimed_account") ||
							type.equals("change_recovery_account") ||
							type.equals("request_account_recovery")){
							;//ignore these for now
//TODO: check pow2 contents!!!							
							//writing operation params into "input" field for now
							write(logger, api_name, date, block, type, null, null, null, null, null, null, null, args.toString(), null, null);
						}else
						if (type.equals("transfer")){
							//["transfer",{"from":"steem-bounty","to":"tsnaks","amount":"0.084 SBD","memo":"Congratulations! Your submission earned you 0.084 SBD from a bounty https://steemit.com/sports/@sportfrei/2nd-bundesliga-2nd-game-day-saturday-games#@tsnaks/re-sportfrei-2nd-bundesliga-2nd-game-day-saturday-games-20180811t110408573z. You have received 0.084 SBD from the creator of the bounty and 0.000 SBD from the community! Find more bounties at www.steem-bounty.com."}]
							String memo = getJsonString(args,"memo");
							if (memo != null)
								memo = recode(memo);
							String amount = getJsonString(args,"amount");
							String[] vu = !AL.empty(amount) ? amount.split(" ") : new String[]{""};
							String value = vu[0];
							String unit = vu.length > 1 ? vu[1] : "";
							
							//https://stackoverflow.com/questions/18231802/how-can-i-parse-a-string-to-bigdecimal
							DecimalFormatSymbols symbols = new DecimalFormatSymbols();
							symbols.setDecimalSeparator('.');
							String pattern = "#0.0#";
							DecimalFormat decimalFormat = new DecimalFormat(pattern, symbols);
							decimalFormat.setParseBigDecimal(true);
							BigDecimal bigDecimal = (BigDecimal) decimalFormat.parse(value);
							//for Steemit/Golos, multiply decimals like 0.001 by 1000 to get 1 at least
							int logvalue = 1 + bigDecimal.doubleValue() == 0 ? 0 : (int)Math.round(Math.log10(bigDecimal.doubleValue() * 1000));//should be non-zero
							
							//TODO: tags!?
							String from = getJsonString(args,"from");
							String to = getJsonString(args,"to");
							write(logger, api_name, date, block, type, from, to, value, unit, null, null, null, memo, null, null);
							
							if (api != null)
								api.alert(date,block,"pays",from,to,amount,site);
							graph.addValue(from, to, "pays", logvalue);//out
							graph.addValue(to, from, "paid", logvalue);//in
							
							pending_update = true;
							
						}else
						if (type.equals("vote")){
							//["vote",{"voter":"munhwan","author":"hjk96","permlink":"6afnxy-with","weight":10000}]
							int intvalue = getJsonInt(args,"weight");
							int logvalue = 1 + (int)Math.round(Math.log10(intvalue));//should be non-zero
							String weight = String.valueOf(intvalue);
							String voter = getJsonString(args,"voter");
							String author = getJsonString(args,"author");
							//                name, time, type, from, 						 to, 						 value, unit, child, parent, 					   title, input, tags, format
							write(logger, api_name, date, block, type, voter, author, weight, "", null, getJsonString(args,"permlink"), null, null, null, null);

							//update votes/voted link
							if (api != null)
								api.alert(date,block,"votes",voter,author,String.valueOf(intvalue),site);
							graph.addValue(voter, author, "votes", logvalue);//out
							graph.addValue(author, voter, "voted", logvalue);//in
							
							pending_update = true;
							
						}else
						if (type.equals("comment")){
							//["comment",{"parent_author":"","parent_permlink":"funny","author":"mudpuddle","permlink":"4z5vfw-little-girl","title":"Little Girl","body":"<h1><center>Little Girl</center></h1>\n\n![mudpuddle-pic.png](https://steemitimages.com/DQmQZguwRXLV9noTYSxe57Ygkw49Y3H9AP4h2V8emh4L7dq/mudpuddle-pic.png)\n\n<h2><center>A precious little girl walks into a pet shop and asks in the sweetest little lisp, \"Excuthe me, mithter, do you keep widdle wabbits?\" \nAs the shopkeeper's heart melts, he gets down on his knees, so that he's on her level, and asks, \"Do you want a widdle white wabby or a thoft and fuwwy back wabby or maybe one like that cute widdle bwown wabby over there?\" \nShe, in turn blushes, rocks on her heels, puts her hands on her knees, leans forward and says in a quiet voice, \"I don't fink my pet python weally gives a thit.</center><h2>\n\n<br><br><br>\n\n![mudpuddle-footer.png](https://steemitimages.com/DQmbKk6YNwQq8b16zrPxPez6QhKPZ2ZJ5aMZrxvjdGwVsTc/mudpuddle-footer.png)","json_metadata":"{\"tags\":[\"funny\",\"humor\",\"comedy\",\"joke\",\"life\"],\"image\":[\"https://steemitimages.com/DQmQZguwRXLV9noTYSxe57Ygkw49Y3H9AP4h2V8emh4L7dq/mudpuddle-pic.png\",\"https://steemitimages.com/DQmbKk6YNwQq8b16zrPxPez6QhKPZ2ZJ5aMZrxvjdGwVsTc/mudpuddle-footer.png\"],\"app\":\"steemit/0.1\",\"format\":\"markdown\"}"}]
							String title = getJsonString(args,"title");
							if (title != null)
								title = recode(title);
							String body = getJsonString(args,"body",null);
							if (body == null){
								body = getJsonString(args,"",null);
								//TODO: it happens: ["comment",{"parent_author":"soysilverio","parent_permlink":"gran-gato-dibujo-con-tinta","author":"steemitboard","permlink":"steemitboard-notify-soysilverio-20180819t003759000z","title":"","":"Congratulations @soysilverio! You have completed the following achievement on Steemit and have been rewarded with new badge(s) :\n\n[![](https://steemitimages.com/70x70/http://steemitboard.com/notifications/firstcommented.png)](http://steemitboard.com/@soysilverio) You got a First Reply\n\n<sub>_Click on the badge to view your Board of Honor._</sub>\n<sub>_If you no longer want to receive notifications, reply to this comment with the word_ `STOP`</sub>\n\n\n\n**Do not miss the last post from @steemitboard:**\n[SteemitBoard and the Veterans on Steemit - The First Community Badge.](https://steemit.com/veterans/@steemitboard/steemitboard-and-the-veterans-on-steemit-the-first-community-badge)\n\n> Do you like [SteemitBoard's project](https://steemit.com/@steemitboard)? Then **[Vote for its witness](https://v2.steemconnect.com/sign/account-witness-vote?witness=steemitboard&approve=1)** and **get one more award**!","json_metadata":"{\"image\":[\"https://steemitboard.com/img/notify.png\"]}"}]
								env.debug(caps_name+" bodyless:"+operation);
							}
							if (body != null)
								body = recode(body);
							else
								env.debug(caps_name+" empty comment:"+operation);
							int intvalue = body != null ? body.length() : 1;//empty comment still counts
							String weight = body != null ? String.valueOf(intvalue) : null;
							int logvalue = 1 + (int)Math.round(Math.log10(intvalue));//should be non-zero
							
							//TODO: tags and format properly from json_metadata
							//"json_metadata":"{\"tags\":[\"dtv\"],\"app\":\"steemit/0.1\"}"
							//                name, time, type, from, 						 to, 						          value, unit, child, 						   parent, 					   			title, 	input, tags, format
							String author = getJsonString(args,"author");
							String parent_author = getJsonString(args,"parent_author");
							String permlink = getJsonString(args,"permlink");
							String parent_permlink = getJsonString(args,"parent_permlink");
							write(logger, api_name, date, block, type, author, parent_author, weight, "", permlink, parent_permlink, title, body, null, null);
							//TODO: update comments/commented link
							if (!AL.empty(parent_author)){//comment on post
								if (api != null)
									api.alert(date,block,"comments",author,parent_author,null,site);
								graph.addValue(author, parent_author, "comments", logvalue);//out
								graph.addValue(parent_author, author, "commented", logvalue);//in
							}else {//original post
								if (api != null) {//content monitoring
									if (attention_date.compareTo(new_date) < 0) {
										OrderedStringSet links = new OrderedStringSet();
										String text = SocialFeeder.parsePost(title,body,links);
										if (!AL.empty(text)) {//Skip edits//api.body.debug(caps_name+" empty text:"+operation);
//TODO extractUrls(...)?
											String imgurl = null;//TODO extract
											for (Object s : links) if (AL.isIMG((String)s)) {
												imgurl = (String)s;
												break;
											}
											String permlink_url = Steemit.permlink_url(site,parent_permlink,author,permlink);
											//Siter.matchPeersText(api.body, peerThings, text, new_date, permlink_url, imgurl);
											//do updates later
											thingMatches += matcher.matchThingsText(peerThings,text,new_date,permlink_url,imgurl,thingPaths);
										}
									}
								}
							}
							//TODO: update uses/used links
							//TODO: update mentions/mentioned links
							pending_update = true;
							
						}else{
							env.debug(caps_name+" unknown:"+operation);
							//writing operation params into "input" field for now
							write(logger, api_name, date, block, type, null, null, null, null, null, null, null, args.toString(), null, null);
						}
					}//operations
				}//transactions
			}//graph
//TODO:indent			
			} catch (Throwable e) {
				env.error(caps_name+" crawling error "+api_url+" "+par+" "+response,e);
			}
			
		}//blocks
		if (graph != null && pending_update){
			env.debug(caps_name+" crawling graph saving for "+key+", age "+new Date((long)age)+" memory "+env.checkMemory());
			if (api != null) 
				api.updateGraph(key,graph,age);
			else
				grapher.updateGraph(key,graph,age);
		}
		logger.close();//TODO: move it to updateGraphs in SocialCacher

		if (api != null && thingMatches > 0)
			api.body.getPublisher().update(null,Time.date(new Date(start)),thingPaths,false,null);//forced=false, because may be retrospective

		long stop = System.currentTimeMillis(); 
		env.debug(caps_name+" crawling stop, took "+Period.toHours(stop-start));
	}
	
	public static void accountSpider(String name,String[] peers,int range,int days) throws Exception {
		Mainer env = new Mainer();
		LangPack lp = new LangPack(env);
		GraphCacher gc = new GraphCacher(name, env);
		Steemit api = new Steemit(null,name,"https://api.steemit.com");
		HashSet peerset = new HashSet(Arrays.asList(peers));
		Date since = Time.today(-days);
		Date until = Time.today(+1);
		resync("steemit", api, env, lp, gc, peerset, since, until, null, range, 0);
		//gc.to(System.out);//crashes even for 3 peers
	}
	
	public static void main(String[] args) {
		java.lang.System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

		//HttpFileReader reader = new HttpFileReader();
		
		//TODO: remove @ from steemit id: #79007 steemit id @cantribute.
		
		//TODO: process mentions

		//TODO: PATH A
		//for report: accept user, tags and radius (R of expansion cycles)
		//create set of input users with 1 user
		//create set of processed users
		//for round = 1, round <= radius, round++ 
			//create set of users to expand
			//for each of input users
				//create feed for each user
				//add name to processed list
				//merge feed to graph
				//extract users from feed and add unproscessed users to users to expand
			//add users to expand to set of input users
		//create joint multi-user report
		//have same user name in report title and sections
		//ensure mutual counts are right and not doubled
		//demo
		
		//TODO: PATH B
		//accept list of users, tags and radius (R of expansion cycles)
		//create set of input users
		//create set of processed users
		//create graph
		//for round = 1, round <= radius, round++ 
			//create set of users to expand
			//for each name
				//create feed for each user
				//add name to processed list
				//merge feed to graph
				//extract users from feed and add unproscessed users to users to expand
			//add users to expand to set of input users
		//normalise graph
		//merge graph to storager and create thought
		//be able to query graph with threshold:
		//my threshold 90.
		//what new true?
		
		//TODO: PATH C
		//iterate blocks: curl --data '{"id":28,"method":"get_block","params":["10000000"]}' https://steemd.steemit.com
		
		/*
		String par = "{\"jsonrpc\":\"2.0\",\"id\":\"25\",\"method\":\"get_account_history\",\"params\": [\""
				+ user_id + "\",\""+String.valueOf(from_pos)+"\",\""+String.valueOf(block_size)+"\"]}";
		String data = reader.readDocData(docName);
		*/
		try {
			String name = "steemit";
			if (args.length > 0 && !AL.empty(args[0]))
				name = args[0];
			/*
			int radius = 2;
			int days = 30;
			if (args.length > 1 && Integer.parseInt(args[1]) > 0)
				radius = Integer.parseInt(args[1]);
			if (args.length > 2 && Integer.parseInt(args[2]) > 0)
				days = Integer.parseInt(args[2]);
			//accountSpider(name,new String[]{"akolonin","aigents"},radius,days);
			*/
			
			//https://api-full.golos.id (OK)//5.829696165440641 blocks/minute at 38 blocks 
			String url = "steemit".equals(name) ? "https://api.steemit.com" : 
				//"https://api-full.golos.id";
				"https://api-golos.blckchnd.com";
				//"https://golos.lexai.host";
			//https://api.golos.blckchnd.com (OK)//5.535906504690143 blocks/minute
			//String url = "steemit".equals(name) ? "https://api.steemit.com" : "https://api.golos.blckchnd.com";
			HTTP.init();
			//optional input - block number
			blockSpider(null,new Mainer(),name,url, args.length > 1 ? Long.parseLong(args[1]) : -1 , false);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}//class
