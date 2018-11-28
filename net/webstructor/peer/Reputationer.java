/*
 * MIT License
 * 
 * Copyright (c) 2018 Stichting SingularityNET
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

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.webstructor.al.AL;
import net.webstructor.data.Counter;
import net.webstructor.data.DataLogger;
import net.webstructor.data.Graph;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.Linker;
import net.webstructor.data.Summator;
import net.webstructor.al.Time;
import net.webstructor.al.Period;
import net.webstructor.al.Writer;
import net.webstructor.core.Environment;
import net.webstructor.core.Filer;
import net.webstructor.util.ArrayPositionComparator;
import net.webstructor.util.Str;
import net.webstructor.util.Array;
import net.webstructor.main.Mainer;
import net.webstructor.main.Tester;

class ReputationParameters {
	double conservativity = 0.5; // balance of using either older reputation (1.0) or latest one (0.0) when blending them, in range 0.0 to 1.0, default is 0.5;
	boolean logarithmicRatings = false; // whether or not apply log10(1+x) to ratings (no need for that if stored ratings are logarithmic already, like in case of Aigents Graphs); 
	boolean logarithmicRanks = true; // whether or not apply log10(1+x) to ranks;
	double defaultReputation = 0.5; // default reputation value for newcomer Agents in range 0.0-1.0;
	double defaultRating = 0.5; // default rating value for “overall rating” and “per-dimension” ratings;
	boolean normalizedRanks = false; //whether ranks should be normlized with minimal rating brought to zero, leaving highest at 1.0 (100%) 
	boolean weightingRatings = false; //whether ratings should weighted if finaincial values for that are available 
	long periodMillis = Period.DAY; // period of reputation recalculation/update;
	boolean liquidRatings = true; //whether to blend ranks of raters with ratings ("liquid rank"); 
	/*
			Dimensions and their weighting factors for blending — timeliness, accuracy, etc.;
			T&P — Time and period of reputation recalculation/update;
			Tupdate — time required to have reputation consensus achieved;
			Nmin — minimum number of Agents that are required to have reputation state or per-account reputation cross-validated and reputation consensus achieved;
			Nmax — maximum number of Agents that are required to have reputation consensus achieved;
			F, S — Weighting factors for blending per-task and staking ratings when calculating reputation, respectively.
			PR — amount of AGI tokens allocated for curation rewards for the period.
			Tlimiting — period of time that limits apply for (day, week, month, year);
			GRmax — maximum amount spent on per-task payments in the period per agent pair
			GAmax — maximum amount spent on per-task payments in the period per agent
			QRmax — maximum amount of staking value in the period per agent pair
			QAmax — maximum amount of staking value in the period per agent
			QQmax — capping amount of staking value per agent pair, regardless of period
			QTmax — capping amount of staking value per agent, regardless of period
	*/		
}

class ReputationTypes {
	public static final String all_domains = "domains";//all domains/categories
	public static final String all_aspects = "aspects";//all aspects/dimensions
}

interface Stater {
	void init(String name, Environment env, String path);
	void save();
	void clear();
	boolean hasState(Object date, String[] domains);//TODO: domains/dimensions!?
	Map getLinkers(Object date);//TODO: domains/dimensions!?
	//TODO: put
	void add(Object date, Object account, Object domain, Object dimension, int intvalue);
	void add(Object date, Object domain, Object dimension, Linker byaccount);
}

class GraphStater implements Stater {
	protected Filer filer = null;
	private Graph graph;
	protected String path;
	public void init(String name, Environment env, String path){
		this.path = (AL.empty(path) ? "" : path + "/") + name+"/"+name+"_states.ser";
		filer = new Filer(env);
		graph = (Graph)filer.load(this.path);
		if (graph == null)
			graph = new Graph();
	}
	public void clear(){
		filer.del(path);
	}
	public void save(){
		filer.save(path, graph);
	}
	public boolean hasState(Object date, String[] domains) {
		return !AL.empty(graph.getLinkers(date, false));
	}
	public Map getLinkers(Object date){
		//TODO: by domains
		return graph.getLinkers(date, false);
	}
	public void add(Object date, Object account, Object domain, Object dimension, int intvalue){
		//TODO: dimension and domain as null 
		graph.addValue(date, account, domain, intvalue );
	}
	public void add(Object date, Object domain, Object dimension, Linker byaccount){
		//TODO: dimension and domain as null 
		graph.addValues(date, domain, byaccount);
	}
}

class GraphCacherStater implements Stater {
	protected GraphCacher cacher = null;
	public void init(String name, Environment env, String path){
		cacher = new GraphCacher(name+"_state",env,path);
	}
	public void save(){
		cacher.setAge(System.currentTimeMillis());//TODO: more reasonable policy to save "modified" graphs only
		cacher.saveGraphs();
	}
	public void clear(){
		cacher.clear(true);
	}
	public boolean hasState(Object date, String[] domains) {
		Graph graph = cacher.getGraph((Date)date);//domains->accounts->dimensions->values
		String key = ReputationTypes.all_domains;//TODO: null?
		if (!AL.empty(domains)){
			Arrays.sort(domains);
			key = Str.join(domains, "&");
		}
		HashMap bydomains = graph.getLinkers(key,false);
		return !AL.empty(bydomains);
	}
	public Map getLinkers(Object date){//TODO: remove
		return getLinkers(date, null);
	}
	public Map getLinkers(Object date, String[] domains){
		Graph graph = cacher.getGraph((Date)date);//domains->accounts->dimensions->values
		String key = ReputationTypes.all_domains;//TODO: null?
		if (!AL.empty(domains)){
			Arrays.sort(domains);
			key = Str.join(domains, "&");
		}
		Map bydimensions = graph.getLinkers(key, false);
		if (!AL.empty(bydimensions)){
			Linker byaccount = (Linker)bydimensions.get(ReputationTypes.all_aspects);
			//TODO: remove this hack!!!
			HashMap bydomain = new HashMap();
			bydomain.put(key, byaccount);
			return bydomain;
		}
		return null;
	}
	public void add(Object date, Object account, Object domain, Object dimension, int intvalue){
		Graph graph = cacher.getGraph((Date)date);
		//TODO: dimension and domain as null
		graph.addValue(domain, account, ReputationTypes.all_aspects, intvalue);
	}
	public void add(Object date, Object domain, Object dimension, Linker byaccount){
		Graph graph = cacher.getGraph((Date)date);
		//TODO: dimension and domain as null 
		graph.addValues(domain, ReputationTypes.all_aspects, byaccount);
		cacher.updateGraph((Date)date, graph, System.currentTimeMillis());//TODO: more smart!?
	}
}

//TODO: synchroizaion
public class Reputationer {

	//system context properties
	protected Stater states = null; //date/timestamp->account->dimension/aspect->domain/category->value or enclosed HashMap with dimensions/aspects
	protected GraphCacher cacher = null;
	protected String name;
	protected ReputationParameters params = new ReputationParameters();
	
	public Reputationer(Environment env, String name, String path, boolean dailyStates){
		cacher = new GraphCacher(name,env,path);
		if (dailyStates)
			states = new GraphCacherStater();
		else 
			states = new GraphStater();
		states.init(name,env,path);
		this.name = name;
	}

	/**
	 * Delete entire contents of the ratings database
	 */
	public void clear_ratings(){
		cacher.clear(true);
	}
	
	/**
	 * Delete entire contents of the ranks database
	 */
	protected void clear_ranks(){
		states.clear();
	}
	
	protected void save_ranks(){
		states.save();
	}
	
	protected void save_ratings(){
		cacher.setAge(System.currentTimeMillis());//TODO: more reasonable policy to save "modified" graphs only
		cacher.saveGraphs();
	}
	
	/**
	 * Sets intital reputation state, if allowed
	 * @param datetime - reputation state date/time
	 * @param state array of tuples: id, value, optional array of per-dimension pairs of dimension and value: 
	 * @return
	 */
	public int set_ranks(Date datetime, Object[][] state){
		//TODO: handle dimensions?
		if (datetime == null)
			return 1;
		if (AL.empty(state))
			return 2;
		Date date = Time.date(datetime);
		if (states.hasState(date, null))//have state at the date/time
			return 3;
		for (int i = 0; i < state.length; i++){
			Object[] s = state[i];
			if (s.length < 2 || !(s[0] instanceof String) || !(s[1] instanceof Number))
				return 4;
		}
		for (int i = 0; i < state.length; i++){
			Object[] s = state[i];
			states.add(date, s[0], ReputationTypes.all_domains, null, ((Number)s[1]).intValue() );
		}
		return 0;
	}
	
	//TODO:update
	/**
	Update - to spawn/trigger background reputation update process, if needed to force externally
		Input (object)
			Timestamp - optional, default is current time (Linux seconds or more precise to ms or nanos - TBD)
			Domains - optional (array of 0 to many strings identifying categories e.g. House Cleaning, Text Clustering, etc.)
		Output (object)
			Result code (0 - success, 1 - in progress, 2 - consensus pending, error code otherwise)
	*/
	public int update(Date datetime, String[] domains){
		if (datetime == null)
			return 3;//no input datetime
		Date date = Time.date(datetime);
		//TODO:account for domains-specific states 
		if (states.hasState(date, domains))
			return 1;//up-to date
		//TODO:start asynchronously
		//return 1;
		int period = (int)Math.round(((double)params.periodMillis)/Period.DAY);
		return build(Time.date(date,-period),date,domains);//build synchronously
	}
	
	private int build(Date prevdate, Date nextdate, String[] domains){
		//TODO:account for domains-specific states 
		if (domains != null)
			return 99;//not supported
		String type = ReputationTypes.all_domains;
		
		//create default state, non-present entries will be populated with defaults 
		Map prevstate = states.getLinkers(prevdate);
		Linker state = prevstate == null ? new Counter() : (Linker)prevstate.get(type);
		
		Summator differential = new Summator(); 

		//compute incremental reputation over time period
		for (Date day = Time.date(prevdate, +1); day.compareTo(nextdate) <= 0; day = Time.date(day, +1)){
			Graph daily = cacher.getGraph(day);
			//TODO: Iterator interface and graph iterator func with Doer callback interface Doer { public int do(Object[] context); }
			//TODO: skip reverse ratings 
			List ratings = daily.toList();
			if (AL.empty(ratings))
				continue;
			for (int i = 0; i < ratings.size(); i++){
				Object[] rating = (Object[])ratings.get(i);// [from type to value]
				if (!((String)rating[1]).endsWith("s"))//skip reverse ratings
					continue;
				Number raterNumber = state.value(rating[0]);//value in range 0-100%
				double raterValue = !params.liquidRatings ? 1.0 : 
						raterNumber == null? params.defaultReputation * 100: raterNumber.doubleValue();//0-100				
				double ratingValue = rating[3] == null ? params.defaultRating : ((Number)rating[3]).doubleValue();//any value
				if (params.logarithmicRatings)
					ratingValue = ratingValue > 0 ? Math.log10(1 + ratingValue) : -Math.log10(1 - ratingValue);
				differential.count(rating[2], raterValue * ratingValue, 0);
			}
		}
		differential.normalize(params.logarithmicRanks,params.normalizedRanks);//differential ratings in range 0-100%
		
		//differential.blend(state,params.conservativity,0,0);
		differential.blend(state,params.conservativity,0,(int)Math.round(params.defaultReputation * 100));
		differential.normalize();
		states.add(nextdate, type, null, new Counter(differential));
		//TODO: save
		return 0;
	}
	
	//TODO:retrieve
	/**
	Retrieve (extracts current reputation computed by Update API or in background)
		Input (object)
			Timestamp - optional, default is current time (Linux seconds or more precise to ms or nanos - TBD)
			Domains (array) - in which categories (House Cleaning, Text Clustering, etc.) the reputation should be computed
			Dimensions (array) - which aspects (Quality, Timeliness, etc.) of the reputation should be retrieved 
			Ids (array) - which users should evaluated for their reputation (if not provided, all users are returned)
			Force Update - if true, forces update if not available by date/time
			From - starting which Id in the result set is to return results (default - 0)
			Length - home may Id-s is to return in results (default - all)
		Output (object)
			Result code (0 - success, 1 - in progress, 2 - consensus pending, error code otherwise)
			Percentage Completed (less than 100% if Result code is 1)
			Data (array, may not be sorted by Id)
				Id
				Ranks (array)
				Dimension
				Value
	 */
	//TODO: input is array
	public int get_ranks(Date datetime, String[] ids, String[] domains, String[] dimensions, boolean force, long at, long size, List results){
		//TODO: sorting results for stability!?
		//TODO: Retrieve By Date/Time (all domains and accounts)
		//TODO: Retrieve By Date/Time and Accounts (all domains)
		//TODO: Retrieve By Date/Time and Domains (all accounts)
		//TODO: Retrieve By Date/Time, Domains and Accounts
		if (datetime == null)
			return 3;//no input datetime
		Date date = Time.date(datetime);
		Map bydomains = states.getLinkers(date);
		if (AL.empty(bydomains)){
			if (force){
				//TODO: force recalc
				return 1;
			} else 
				return 4;//not available for date
		}
		//TODO: long from, long length
		//TODO: String dimensions
		Set idset = AL.empty(ids) ? null : Array.toSet(ids);
		if (!AL.empty(domains)){
			for (int i = 0; i < domains.length; i++)
				retrieve(bydomains,domains[i],idset,results);
		} else {
			for (Iterator it = bydomains.keySet().iterator(); it.hasNext();)
				retrieve(bydomains,(String)it.next(),idset,results);
		}
		//TODO: sort in intermediate adapter array list in case of multiple domains and from and to present
		Collections.sort(results,new ArrayPositionComparator(1,0));//desc order!?
		return 0;
	}
	
	private static void retrieve(Map bydomains, String domain, Set ids, List results){
		Linker linker = (Linker)bydomains.get(domain);
		for (Iterator it = linker.keys().iterator(); it.hasNext();){
			String id = (String)it.next();
			if (ids == null || ids.contains(id))
				results.add(new Object[]{id,linker.value(id)});
		}
	}
	
	/**
	 * Query existing ratings
	 * @param ids - seed ids
	 * @param date - date
	 * @param period - number of days back
	 * @param range - link range
	 * @param threshold
	 * @param limit
	 * @param format
	 * @param links
	 * @return array of tuples of ratings [from type to value]
	 */
	//TODO: return weight and time
	protected Object[][] get_ratings(String[] ids, Date date, int period, int range, int threshold, int limit, String format, String[] links){
		//TODO: sorting results for stability!?
		Graph result = cacher.getSubgraph(ids, date, period, range, threshold, limit, format, links);
		Object[][] o = result.toArray();
		//TODO: sort
		Arrays.sort(o,new ArrayPositionComparator(0,2));//asc id order!?
		result.clear();//save memory
		return o;
	}

	/**
	Rate (for implicit and explicit rates or stakes from any external sources)
		Input (array):
			From Id (who authoring the rating/staking record is may include both local id and name of system like cassio@google)
			Type (Stake, Rate, Transfer, Vote, Like, etc. - specific to given environment)
			To Id (who is being subject of the rating/staking is may include both local id and name of system like akolonin@google)
			Value (default/composite value, if multi-dimensional Data is not provided)
			Weight (like stake value or associated transaction value)
			Timestamp (Linux seconds or more precise to ms or nanos - TBD)
			Domains (array of 0 to many strings identifying categories e.g. House Cleaning, Text Clustering, etc.)
			Dimension Values (optional array)
				Dimension (identifying aspect e.g. Quality, Timeliness, etc.)
				Value for dimension (e.g. +1 or -1)
			Reference Id (like Id of associated transaction in the external system, including both id and system name like 0x12345@ethereum)
		Output (object)
			Result code (0 - success, error code otherwise)
	 * @param args
	 */
	public int add_ratings(Object[][] ratings){
		if (AL.empty(ratings))
			return 1;
		//TODO: add Source/System/Network as a parameter!?
		//TODO: Dimensions 
		//TODO: Domains
		//TODO: Weight
		//validate first
		for (int i = 0; i < ratings.length; i++){
			Object[] r = ratings[i];
			if (r.length < 6 || !(r[0] instanceof String) || !(r[1] instanceof String) || !(r[2] instanceof String) || !(r[3] instanceof Number) || !(r[5] instanceof Date))
				return 2;
		}
		Date last = null;
		Graph graph = null;
		for (int i = 0; i < ratings.length; i++){
			Object[] r = ratings[i];
			String from = (String)r[0];
			String type = (String)r[1];
			String to = (String)r[2];
			Number value = (Number)r[3];
			Number weight = (Number)r[4];
			if (weight != null)//expect it is rating in range 0-100 with any weight 
				value = new Integer((int)Math.round(value.doubleValue() * weight.doubleValue())); 
			Date date = Time.date((Date)r[5]);
			//TODO: store in FULL version of storage
			//store in light version of storage
			if (last == null || !last.equals(date)){
				//TODO: ensure to save the graph!?
				graph = cacher.getGraph(date);
				last = date;
			}
			graph.addValue(from, to, type+"-s", value.intValue());//eg. rate-s
			graph.addValue(to, from, type+"-d", value.intValue());//eg. rate-d
		}
		return 0;
	}
	
	public static void main(String[] args){
		if (args == null || args.length < 1){
			Mainer m = new Mainer();
			m.debug("Options: test | state ... | ranks ... | update ... | rate ...");
			return;
		}
		
		if ("test".equalsIgnoreCase(args[0]))
			test(new Mainer(false));
		else
		if (Str.has(args, "network", null)){
			Mainer m = new Mainer(Str.has(args, "verbose"));
			String outputPath = Str.arg(args,"output",null);
			PrintStream	out = !AL.empty(outputPath) ? (new Filer(m)).openStream(outputPath,false,"Exporting ranks") : System.out;
			act(m,new Reputationer(m,Str.arg(args,"network","ethereum"),Str.arg(args,"path",""),true),out,args);//daily states
		}

	}
	
	/*
	- ORL ideas for API structure:
		{get: { ratings : {since:..., until:..., from:[...], types:[...], to:[...]}}
		{add: { ratings : [{from:..., type:..., to:..., value:..., weight:..., time:...},...]}}
		{get: { ranks : {time:..., ids:[...]}}}
		{set: { ranks : {time : ..., ids : [...]}}}
		
		{get:ratings, since:..., until:..., from:[...], types:[...], to:[...]}
		{"?":[{is:rating,since:2018-10-01,until:2018-10-10,ids:[1,2,3],range:2}]}
		get ratings since 2018-10-01; until 2018-10-10; ids 1, 2, 3; range 2
		what is rating, since 2018-10-01; until 2018-10-10; ids 1, 2, 3; range 2
		ratings since 2018-10-01; until 2018-10-10; ids 1, 2, 3; range 2?
		is rating; since 2018-10-01; until 2018-10-10; ids 1, 2, 3; range 2?
		
		{get:ranks, time:..., ids:[...]}
		{"?":[{is:ranks, date:2018-10-10; ids:[1, 2, 3]; at:0, size:100}]}
		get ranks date 2018-10-10; ids 1, 2, 3; at 0, size 100.
		what is rank; date 2018-10-10; ids 1, 2, 3; at 0, size 100.
		ranks date 2018-10-10; ids 1, 2, 3; at 0, size 100?
		what is rank; date 2018-10-10; ids 1, 2, 3; at 0, size 100

		{get:ranks, do:update, since:..., until:...}
		{"!":[{is:rank,since:2018-10-01,until:2018-10-10}],[update]}
		do ranks since 2018-10-01, until 2018-10-10 update.
		is rank, since 2018-10-01, until 2018-10-10 update!

		{get:ranks, date:..., set:[{id:...,value:...},...]}
		{".":[{is:rank,date:2018-10-01}][{id:001,value:0.5},{id:002,value:0.2}]}
		ranks date 2018-10-01 id 001, value 0.5; id 002 value 0.2; id 990, value 0.8.
		there is rank, date 2018-10-01 id 001, value 0.5; id 002, value 0.2; id 990, value 0.8.

		{add:ratings, set:[{from:..., type:..., to:..., value:..., weight:..., time:...},...]}}
		{".":[{is:rating}][{from:001,type:pays,to:990,value:0.7,time:2018-10-01},...]}
		add ratings from 001, type pays, to 990, value 0.7; from 002, type calls, to 004, value 0.9.
		there is rating : from 001, type pays, to 990, value 0.7, time 2018-10-01; from 002, type calls, to 890, value 0.9, time 2018-10-02.
	 */
	public static boolean act(final Environment env, final Reputationer r, final PrintStream out, final String[] args){
		//TODO: multiple elementa parsed by Str.get as array in array!!!
		String id = Str.arg(args,"ids","");
		String[] ids = !AL.empty(id) ? id.split(" ") : null;
		
		int at = Integer.valueOf(Str.arg(args,"at","0")).intValue();
		int size = Integer.valueOf(Str.arg(args,"size","0")).intValue();
		
		//TODO: make any commands handle-able 
		if (Str.has(args,"clear","ratings")){
			r.clear_ratings();
			return true;
		}
		else
		if (Str.has(args,"clear","ranks")){
			r.clear_ranks();
			return true;
		}
		else
		if (Str.has(args,"update","ranks")){
			//compute time range as date given date==until==since+period (default period = 1)
			Date until = Time.day( Str.arg( args, Str.has(args,"date",null) ? "date" : "until" ,"today") );
			Date since = Time.day( Str.arg( args, "since", Time.day(until, false) ) );
			int period = Integer.parseInt(Str.arg(args,"period","1"));
			int computed = 0;
			env.debug("Updating "+r.name+" since "+Time.day(since,false)+" to "+Time.day(until,false)+" period "+period);
			r.params.periodMillis = period * Period.DAY;//need at least one day
			if (Str.has(args, "default", null))
				r.params.defaultReputation = Double.parseDouble(Str.arg(args, "default", String.valueOf(r.params.defaultRating)));
			if (Str.has(args, "conservativity", null))
				r.params.conservativity = Double.parseDouble(Str.arg(args, "conservativity", String.valueOf(r.params.defaultRating)));
			if (Str.has(args,"norm"))
				r.params.normalizedRanks = true;
			if (Str.has(args,"liquid","false"))
				r.params.liquidRatings = false;
			for (Date day = since; day.compareTo(until) <= 0; day = Time.date(day, +period)){ 
				int rs = r.update(day, null);
				env.debug("Updating "+Time.day(day,false)+" at "+new Date(System.currentTimeMillis()));
				if (rs == 0)
					computed++;
				if (rs > 1){// 1 means already exist
					env.error("Error "+rs, null);
					break;
				}
			}
			if (computed > 0)
				r.save_ranks();
			return true;
		}
		else
		if (Str.has(args,"set","ranks")){
			Object[][] ranks = Str.get(args,new String[]{"id","rank"},new Class[]{null,Integer.class});
			if (AL.empty(ranks))
				return false;
			int res = r.set_ranks(Time.day(Str.arg(args,"date","today")),ranks);
			if (res == 0)
				r.save_ranks();
			return true;
		}
		else
		if (Str.has(args,"get","ranks")){
			Date until = Time.day( Str.arg( args, Str.has(args,"date",null) ? "date" : "until" ,"today") );
			Date since = Time.day( Str.arg( args, "since", Time.day(until, false) ) );
			int period = Integer.parseInt(Str.arg(args,"period","1"));
			
			//TODO: get multiple ids
			Object[][] idss = Str.get(args,new String[]{"id"},null,null); 
			if (ids == null && !AL.empty(idss)){
				ids = new String[idss.length];
				for (int i=0; i<idss.length; i++)
					ids[i] = (String)idss[i][0];
			}
			if (Period.daysdiff(since, until) == 0){
				//get one shot reputation state
				//TODO: domains
				//TODO: dimensions
				ArrayList a = new ArrayList();
				//TODO: from & size
				r.get_ranks(until, ids, null, null, false, 0, 0, a);
				//Collections.sort(a,new ArrayPositionComparator(1,0));//desc order!?
				int to = a.size();
				if (size > 0 && to > at + size)
					to = at + size;
				for (int i = at; i < to; i++){
					Object[] o = (Object[])a.get(i);
					String s = o[0]+"\t"+o[1];
					//TODO: return to caller or print to environment
					out.println(s);//output to console
				}
			}else if (Str.has(args, "average")){
				TreeMap sums = new TreeMap();
				TreeMap counts = new TreeMap();
				for (Date day = since; day.compareTo(until) <= 0; day = Time.date(day, +period)){ 
					ArrayList a = new ArrayList();
					r.get_ranks(day, ids, null, null, false, 0, 0, a);
					for (int i = 0; i < a.size(); i++){
						Object[] o = (Object[])a.get(i);
						Integer count = (Integer)counts.get(o[0]);
						if (count == null){
							sums.put(o[0],new Double(((Number)o[1]).doubleValue()));
							counts.put(o[0],new Integer(1));
						}else{
							sums.put(o[0],new Double( ((Number)o[1]).doubleValue() + ((Double)sums.get(o[0])).doubleValue() ));
							counts.put(o[0],new Integer(count.intValue() + 1));
						}
					}
				}
				for (Iterator it = counts.keySet().iterator(); it.hasNext();){
					Object keyid = it.next();
					int count = ((Integer)counts.get(keyid)).intValue();
					double sum = ((Double)sums.get(keyid)).doubleValue();
					out.print(keyid);//output to console
					out.print('\t');
					out.print(sum/count);
					out.println("");
				}
			}else{
				//study temporal dynamics
				TreeMap byid = new TreeMap();
				for (Date day = since; day.compareTo(until) <= 0; day = Time.date(day, +period)){ 
					ArrayList a = new ArrayList();
					r.get_ranks(day, ids, null, null, false, 0, 0, a);
					for (int i = 0; i < a.size(); i++){
						Object[] o = (Object[])a.get(i);
						HashMap bydate = (HashMap)byid.get(o[0]);
						if (bydate == null)
							byid.put(o[0],bydate = new HashMap());
						bydate.put(day, o[1]);
					}
				}
				for (Iterator it = byid.keySet().iterator(); it.hasNext();){
					Object keyid = it.next();
					out.print(keyid);//output to console
					HashMap bydate = (HashMap)byid.get(keyid);
					/*if (Str.has(args, "average")){
						double sum = 0;
						int cnt = 0;
						for (Date day = since; day.compareTo(until) <= 0; day = Time.date(day, +period)){
							Object val = bydate.get(day);
							if (val != null){
								sum += (new BigDecimal(val.toString())).doubleValue();
								cnt++;
							}
						}
						out.print('\t');
						if (cnt != 0)
							out.print(sum/cnt);
					} else*/ {
						//print every day
						for (Date day = since; day.compareTo(until) <= 0; day = Time.date(day, +period)){
							Object val = bydate.get(day);
							out.print('\t');
							if (val != null)
								out.print(val);
						}
					}
					out.println("");
				}
			}
			return true;
		}
		else
		if (Str.has(args,"add","ratings")){
			Object[][] ratings = Str.get(args,new String[]{"from","type","to","value","weight","time"},new Class[]{null,null,null,Double.class,Integer.class,Date.class},new String[]{null,null,null,null,"1","today"});  
			if (AL.empty(ratings))
				return false;
			if (r.add_ratings(ratings) == 0)
				r.save_ratings();
			return true;
		}
		else
		if (Str.has(args,"load","ratings")){
			//TODO: two options - "file" and "folder"
			String path = Str.arg(args,"file",null);
			final BigDecimal precision = new BigDecimal(Str.arg(args, "precision", "1.0"));
			final boolean log = Str.has(args, "logarithm");
			if (Str.has(args,"weighting"))
				r.params.weightingRatings = true;
			if (!AL.empty(path)){
				DataLogger dl = new DataLogger(env, r.name);
				boolean loaded = dl.load(path, new DataLogger.StringConsumer() {					
					public boolean read(String text) {
						if (AL.empty(text))
							return false;
						String[] tokens = text.split("\t");
						if (AL.empty(tokens) || tokens.length < 6 || AL.empty(tokens[0]) || AL.empty(tokens[1])
							|| AL.empty(tokens[2]) || AL.empty(tokens[3]) || AL.empty(tokens[4]))
							return false;
						//TODO: NumberFormatException
						Date time = new Date(Long.parseLong(tokens[1], 10)*1000);
//TODO: fix scale for financial values and weights!!!???
						BigDecimal value = null;
						BigDecimal weight = null;
						if (tokens.length >= 15 && !AL.empty(tokens[14])){//has weight => so it is rating in range 0.0-1.0 with weighting in any range
							value = (new BigDecimal(tokens[5])).multiply(new BigDecimal(100));
							if (!r.params.weightingRatings){
								weight = null;
							} else {
								weight = (new BigDecimal(tokens[14])).divide(precision);
								if (log)
									weight = new BigDecimal(Math.round(Math.log10(weight.doubleValue())));
							}
						}else{//no weight => so it is payment in any range
							value = (new BigDecimal(tokens[5])).divide(precision);
							if (log)
								value = new BigDecimal(Math.round(Math.log10(value.doubleValue())));
							weight = null;
						}
						//[from type to value weight=null timestamp]
						Object[][] ratings = new Object[][]{new Object[]{
								tokens[3],tokens[2],tokens[4],value,weight,time
						}}; 
						r.add_ratings(ratings);
						return true;
					}
				});
				if (loaded)
					r.save_ratings();
				return true;
			}
		}
		else
		if (Str.has(args,"get","ratings")){
			//compute time range as date given date==until==since+period (default period = 0)
			String dateuntil = Str.has(args,"date",null) ? "date" : "until";
			Date until = Time.day( Str.arg( args, dateuntil, "today") );
			int period = Str.has(args,"since",null)
				? Period.daysdiff(Time.day(Str.arg(args, "since", Time.day(until, false))),until)
				: Integer.parseInt(Str.arg(args,"period","0"));
			Object[][] a = r.get_ratings(ids, until, period, 1, 0, -1, null, null);
			int to = a.length;
			if (size > 0 && to > at + size)
				to = at + size;
			for (int i = at; i < to; i++){
				Object[] o = (Object[])a[i];
//TODO: return to caller or print to environment
				out.println(o[0]+"\t"+o[1]+"\t"+o[2]+"\t"+o[3]);//output to console
			}
			return true;
		}
		else
		//TODO: move out from here
		if (Str.has(args,"compute","pearson")){
			Object[][] files = Str.get(args,new String[]{"file"},null,null); 
			if (!AL.empty(files) && files.length == 2 && !AL.empty(files[0])  && !AL.empty(files[1])){
				Summator s0 = new Summator(env,(String)files[0][0]);
				Summator s1 = new Summator(env,(String)files[1][0]);
				double p = s0.pearson(s1);
				out.println(p);
				return true;
			}
		}
		else
		//TODO: move out from here
		if (Str.has(args,"compute","accuracy")){
			Object[][] files = Str.get(args,new String[]{"file"},null,null); 
			if (!AL.empty(files) && files.length == 2 && !AL.empty(files[0])  && !AL.empty(files[1])){
				Summator s0 = new Summator(env,(String)files[0][0]);
				Summator s1 = new Summator(env,(String)files[1][0]);
				double[] p = s0.accuracy(s1);
				if (p != null && p.length == 4)
					out.println("count "+p[0]+"\t average "+p[1]+"\t avarage "+p[2]+"\t accuracy "+p[3]);
				return true;
			}
		}
		return false;
	}
		
	public static void test(Environment m){
		Tester t = new Tester();
		testStater(t,new Reputationer(m,"testnet",null,false));//test with common state storage
		testStater(t,new Reputationer(m,"testnet",null,true));//test with daily state storage
		t.check();
	}
	
	public static void testStater(Tester t, Reputationer r){
		r.params.logarithmicRanks = false;//to make test numbers clearer
		
		//TODO API and test
		r.clear_ranks();
	
		//test state API
		t.assume(r.set_ranks(Time.today(0),new Object[][]{new Object[]{"1",new Integer(1)},new Object[]{"2",new Integer(5)},new Object[]{"3",new Integer(10)}}), 0);
		t.assume(r.set_ranks(Time.today(-1),new Object[][]{new Object[]{"1",new Integer(2)},new Object[]{"10",new Integer(10)},new Object[]{"3",new Integer(50)}}), 0);
		t.assume(r.set_ranks(Time.today(-1),new Object[][]{new Object[]{"1",new Integer(10)},new Object[]{"10",new Integer(20)},new Object[]{"3",new Integer(100)}}), 3);
		
		ArrayList a;//placeholder for results
		
		//test retrieve API
		r.get_ranks(Time.today(0),null,null,null,false,0,0,a = new ArrayList());
		t.assume(Writer.toString(a.toArray(new Object[][]{})),"((3 10) (2 5) (1 1))");
		
		r.get_ranks(Time.today(-1),null,null,null,false,0,0,a = new ArrayList());
		t.assume(Writer.toString(a.toArray(new Object[][]{})),"((3 50) (10 10) (1 2))");
		
		//test rate API
		//1 rates 3 100
		//1 rates 4 100
		//2 rates 4 100
		//4 rates 5 100
		Date date10 = Time.today(-10);
		t.assume(r.add_ratings(new Object[][]{
				new Object[]{"1","rate","3",new Integer(100),null,date10},
				new Object[]{"1","rate","4",new Integer(100),null,date10},
				new Object[]{"2","rate","4",new Integer(100),null,date10},
				new Object[]{"4","rate","5",new Integer(100),null,date10}
				}), 0);
		
		//test ratings API (retrieval of ratings)
		t.assume(Writer.toString(r.get_ratings(new String[]{"1"}, date10, 1, 1, 0, 10, null, new String[]{"rate-s"})),"((1 rate-s 3 100) (1 rate-s 4 100))");
		t.assume(Writer.toString(r.get_ratings(new String[]{"1"}, date10, 1, 2, 0, 10, null, new String[]{"rate-s"})),"((1 rate-s 3 100) (1 rate-s 4 100) (4 rate-s 5 100))");
		t.assume(Writer.toString(r.get_ratings(new String[]{"1"}, date10, 1, 2, 0, 10, null, new String[]{"rate-d"})),"");//can't handle reverse queries
		t.assume(Writer.toString(r.get_ratings(new String[]{"5"}, date10, 1, 2, 0, 10, null, new String[]{"rate-d"})),"((4 rate-d 1 100) (4 rate-d 2 100) (5 rate-d 4 100))");
		t.assume(Writer.toString(r.get_ratings(new String[]{"5"}, date10, 1, 3, 0, 10, null, new String[]{"rate-d","rate-s"})),"((1 rate-s 3 100) (4 rate-d 1 100) (4 rate-d 2 100) (5 rate-d 4 100))");
		
		//test update API
		t.assume(""+r.update(date10, null),"0");
				
		//test retrieve API after after rank and update
		r.get_ranks(date10,null,null,null,false,0,0,a = new ArrayList());
		t.assume(Writer.toString(a.toArray(new Object[][]{})),"((4 100) (3 66) (5 66))");

		//test rate API (again)
		//3 rates 1 100 (power 50)
		//4 rates 2 100 (power 100)
		Date date9 = Time.today(-9);
		t.assume(r.add_ratings(new Object[][]{
				new Object[]{"3","rate","1",new Integer(100),null,date9},
				new Object[]{"4","rate","2",new Integer(100),null,date9}
				}), 0);

		//test update API
		t.assume(""+r.update(date9, null),"0");
				
		//test retrieve API after after rank and update
		r.get_ranks(date9,null,null,null,false,0,0,a = new ArrayList());
		t.assume(Writer.toString(a.toArray(new Object[][]{})),"((2 100) (1 77) (4 66) (3 44) (5 44))");
		
		//TODO: saving results and clearing on startup
	}
}
