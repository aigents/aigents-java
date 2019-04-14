/*
 * MIT License
 * 
 * Copyright (c) 2018-2019 Stichting SingularityNET
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
import net.webstructor.data.ComplexNumber;
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
	boolean denomination = false; // true to denominate weighted ratings by sum of weight, false to don't
	boolean complexRatings = true; // true to store ratings as arrays of ComplexNumbers, false to compress them in integer
	double conservatism = 0.5; // balance of using either older reputation (1.0) or latest one (0.0) when blending them, in range 0.0 to 1.0, default is 0.5;
	boolean logarithmicRatings = false; // whether or not apply log10(1+x) to ratings (no need for that if stored ratings are logarithmic already, like in case of Aigents Graphs); 
	boolean logarithmicRanks = true; // whether or not apply log10(1+x) to ranks;
	double defaultReputation = 0.5; // default reputation value for newcomer Agents in range 0.0-1.0;
	double decayedReputation = 0.0; // target repuatation level to decay for inactive Agents
	double defaultRating = 0.25; // default rating value for “overall rating” and “per-dimension” ratings;
	boolean normalizedRanks = false; //whether ranks should be normlized with minimal rating brought to zero, leaving highest at 1.0 (100%) 
	boolean weightingRatings = false; //whether ratings should weighted if finaincial values for that are available 
	long periodMillis = Period.DAY; // period of reputation recalculation/update;
	boolean liquidRatings = true; //whether to blend ranks of raters with ratings ("liquid rank");
	BigDecimal ratingPrecision = null; //use to round/up or round down financaial values or weights as value = round(value/precision)
	boolean implicitDownrating = false; //boolean option with True value to translate original explicit rating values in range 0.5-0.0 to negative values in range 0.0 to -1.0 and original values in range 1.0-0.5 to interval 1.0-0.0, respectively
	boolean temporalAggregation = false; //boolean option with True value to force aggregation of all explicit ratings between each unique combination of two agents with computing weighted average of ratings across the observation period
	boolean rankUnrated = false; //boolean option to store defaul ratings so inactive agents may get reputaion growth or decay (based on default and decayed settings) over time 
	double ratings = 1.0; //impact of the explicit and implicit ratings on differential reputation
	double spendings = 0.0; //impact of the spendings ("proof-of-burn") on differential reputation
	boolean verbose = true; //if need full debugging log
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
		if (graph.modified())
			graph.save(filer, path);
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
	protected Environment env;

	//system context properties
	protected Stater states = null; //date/timestamp->account->dimension/aspect->domain/category->value or enclosed HashMap with dimensions/aspects
	protected GraphCacher cacher = null;
	protected String name;
	protected ReputationParameters params = new ReputationParameters();
	
	//transient flags and iterators
	private Graph latest_graph = null;
	private Date latest_date = null;
	private boolean ratings_modified = false;
	private boolean ranks_modified = false;
	
	private static HashMap reputationers = new HashMap();
	public static Reputationer get(String network){
		synchronized (reputationers) {
			return (Reputationer)reputationers.get(network);
		}
	}

	public static void add(String network,Reputationer reputationer){
		synchronized (reputationers) {
			if (reputationers.get(network) == null)
				reputationers.put(network,reputationer);
		}
	}
	
	public Reputationer(Environment env, String name, String path, boolean dailyStates){
		this.env = env;
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
		latest_graph = null;
		latest_date = null;
		ratings_modified = false;
	}
	
	/**
	 * Delete entire contents of the ranks database
	 */
	protected void clear_ranks(){
		states.clear();
		ranks_modified = false;
	}
	
	protected void save_ranks(){
		if (!ranks_modified)
			return;
		states.save();
		ranks_modified = false;
	}
	
	protected void save_ratings(){
		if (!ratings_modified)
			return;
		cacher.setAge(System.currentTimeMillis());//TODO: more reasonable policy to save "modified" graphs only
		cacher.saveGraphs();
		ratings_modified = false;
	}
	
	public void save(){
		save_ranks();
		save_ratings();
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
		ranks_modified = true;
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
		int success = build(Time.date(date,-period),date,domains);//build synchronously
		if (success == 0)
			states.save();
		return success;
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
		Summator normalizer = new Summator(); 
		Summator raters = new Summator();
		Summator spenders = new Summator();

		//compute incremental reputation over time period
		for (Date day = Time.date(prevdate, +1); day.compareTo(nextdate) <= 0; day = Time.date(day, +1)){
			Graph daily = cacher.getGraph(day);
			//TODO: Iterator interface and graph iterator func with Doer callback interface Doer { public int do(Object[] context); }
			//TODO: skip reverse ratings 
			List ratings = daily.toList(false);//don't expand
			if (AL.empty(ratings))
				continue;
			for (int i = 0; i < ratings.size(); i++){
				Object[] rating = (Object[])ratings.get(i);// [from type to value]
				if (rating[0] == null || rating[1] == null || rating[2] == null || rating[3] == null)
					continue;
				if (!((String)rating[1]).endsWith("s"))//skip reverse ratings
					continue;
				Object rater = rating[0];
				Object ratee = rating[2];
				Object value = rating[3];
				Number raterNumber = state.value(rater);//value in range 0-100%
				if (raterNumber == null)
					raterNumber = new Double( params.defaultReputation * 100 );//0-100
				
				if (!raters.containsKey(rater))//save all pre-existing and default rater values
					raters.put(rater, raterNumber);
				
				double raterValue = !params.liquidRatings ? 1.0 : raterNumber.doubleValue();
				if (value instanceof Number){
					double ratingValue = ((Number)value).doubleValue();
					differential.count(ratee, raterValue * ratingValue, 0);
					if (params.spendings > 0)
						spenders.count(rater, ratingValue, 0);//count spendings by raters (it may be financial value or rating itself in this case)
				}else if (value instanceof ComplexNumber[]){
					ComplexNumber[] c = (ComplexNumber[])value;
					for (int j = 0; j < c.length; j++){
						double[] r = calcRating(c[j].a,c[j].b);
						if (params.verbose) env.debug("reputation debug rating: "+rater+" "+ratee+" "+c[j].a+" "+c[j].b+" "+r[0]);
						differential.count(ratee, raterValue * Math.round(r[0]), 0);//TODO: no round!?
						if (params.denomination && r.length > 1)
							normalizer.count(ratee, r[1], 0);
						if (params.spendings > 0)
							spenders.count(rater, r[1], 0);//count spendings by raters
					}
				}
			}
		}
		if (params.verbose) env.debug("reputation debug differential:"+differential);
		if (params.verbose) env.debug("reputation debug denominator:"+normalizer);
		
		if (params.denomination && !normalizer.isEmpty())
			if (!differential.divide(normalizer))
				env.error("Reputationer "+name+" has no normalizer", null);
		if (params.verbose) env.debug("reputation debug denominated:"+differential);
		
		differential.normalize(params.logarithmicRanks,params.normalizedRanks);//differential ratings in range 0-100%
		if (params.verbose) env.debug("reputation debug normalized:"+differential);

		if (params.spendings > 0){//blend ratings with spendigns if needed 
			spenders.normalize(params.logarithmicRanks,params.normalizedRanks);
			differential.blend(spenders, params.spendings / (params.ratings + params.spendings), 0, 0);
		}
		if (params.verbose) env.debug("reputation debug blended spenders:"+differential);

		differential.blend(state, params.conservatism,
				(int)Math.round(params.decayedReputation * 100), //for new reputation to decay
				(int)Math.round(params.defaultReputation * 100));//for old reputation to stay
		if (params.verbose) env.debug("reputation debug blended old state:"+differential);

		differential.normalize(false,params.implicitDownrating);//TODO: if we really need fullnorm on downrating?
		if (params.verbose) env.debug("reputation debug normalized new state:"+differential);

		if (params.rankUnrated)//if required, add unrated newcomers with default value moving to decayed
			for (Iterator it = raters.keys().iterator(); it.hasNext();){
				Object rater = it.next();
				Number rated = differential.value(rater);
				if (rated == null){//if rater is not rated itself, assume default moving to decayed
					double novelty = 1 - params.conservatism;
					differential.put(rater,new Double(raters.value(rater).doubleValue() * params.conservatism + params.decayedReputation * 100 * novelty));
				}
			}
		if (params.verbose) env.debug("reputation debug added unrated:"+differential);
		
		states.add(nextdate, type, null, new Counter(differential));
		//states.add(nextdate, type, null, new Summator(differential));
		//TODO: save
		ranks_modified = true;
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
		Graph result = params.complexRatings ? cacher.getSubgraphRaw(ids, date, period, range, threshold, limit, links)
				: cacher.getSubgraph(ids, date, period, range, threshold, limit, links);
		Object[][] o = (Object[][]) result.toList(params.complexRatings).toArray(new Object[][]{});
		//TODO: sort
		Arrays.sort(o,new ArrayPositionComparator(0,2));//asc id order!?
		result.clear();//save memory
		return o;
	}

	//public double calcRating(Number value, Number weight){
	public double[] calcRating(Number value, Number weight){
		//Note that we assume it is EITHER explicit rating with financial weight OR implicit financial rating!
		if (weight != null){//has weight => so it is rating in range 0.0-1.0 with weighting in any range
			if (params.implicitDownrating && params.defaultRating > 0){
				if (value == null)
					value = new BigDecimal(0);
				else {//scale rating values to range -100 to +100
					double d = params.defaultRating * 100;
					double v = value.doubleValue() * 100;
					v = v < d ? (v - d) / d : (v - d) / (100 - d);
					value = new BigDecimal(v * 100);
				}
			}else{
				if (value == null)
					value = new BigDecimal(params.defaultRating * 100);
			}
			if (!params.weightingRatings){
				weight = null;
			} else {
				//if Precision parameter is set to value other than 1.0, the financial values of the implicit or explicit ratings are re-scaled with Qij = Round(Qij  / Precision).
				if (params.ratingPrecision != null)
					weight = new BigDecimal(weight.doubleValue()).divide(params.ratingPrecision);
				//if LogRatings option is set to True, financial values of the implicit or explicit ratings are scaled to logarithmic scale as Qij = If(Qij  < 0, - log10(1 - Qij ), log10(1 + Qij )), where negative value may be corresponding to the case of transaction withdrawal or cancellation.
				if (params.logarithmicRatings){
					double d = weight.doubleValue();
					weight = new BigDecimal(d > 0 ? Math.log10(1 + d) : - Math.log10(1 - d));
				}
			}
		}else{//no weight => so it is payment in any range (or - rating without weight)
			//if Precision parameter is set to value other than 1.0, the financial values of the implicit or explicit ratings are re-scaled with Qij = Round(Qij  / Precision).
			if (params.ratingPrecision != null)
				value = new BigDecimal(value.doubleValue()).divide(params.ratingPrecision);
			//if LogRatings option is set to True, financial values of the implicit or explicit ratings are scaled to logarithmic scale as Qij = If(Qij  < 0, - log10(1 - Qij ), log10(1 + Qij )), where negative value may be corresponding to the case of transaction withdrawal or cancellation.
			if (params.logarithmicRatings){
				double d = value.doubleValue();
				value = new BigDecimal(d > 0 ? Math.log10(1 + d) : - Math.log10(1 - d));
			}
			weight = null;
		}
		//return weight == null ? value.doubleValue() : value.doubleValue() * weight.doubleValue();
		return weight == null ? new double[]{value.doubleValue()}
				: new double[]{value.doubleValue() * weight.doubleValue(),weight.doubleValue()};
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
		for (int i = 0; i < ratings.length; i++){
			Object[] r = ratings[i];
			String from = (String)r[0];
			String type = (String)r[1];
			String to = (String)r[2];
			Date date = Time.date((Date)r[5]);
			//store in light version of storage
			if (latest_date == null || !latest_date.equals(date)){
				//TODO: ensure to save the graph!?
				cacher.setAge(System.currentTimeMillis());
				latest_graph = cacher.getGraph(date);
				latest_date = date;
			}
			Number value = (Number)r[3];
			Number weight = (Number)r[4];
			if (params.complexRatings){
				//TODO: store in FULL version of storage
				//... separate store of rating and weight as big decimal or double ... 
				ComplexNumber[] cn = new ComplexNumber[]{
						weight != null ? new ComplexNumber(value.doubleValue(),weight.doubleValue()) : new ComplexNumber(value.doubleValue())
						};
				latest_graph.addValue(from, to, type+"-s", cn);//eg. rate-s
				latest_graph.addValue(to, from, type+"-d", cn);//eg. rate-d
			}else{
				//In current Aigents implementation of the Liquid Rank algorithm https://arxiv.org/pdf/1806.07342.pdf
				//weighted ratings are stored "blended" so the rating values are multiplied by financial weights and rounded to
				//integers and stored in that way.
				//This has to be fixed, because "ideal reputation system" should be able to do aggregation of ratings so the 
				//rating value and financial weight should be stored separately for each of the original ratings.
				//Also, the blended rating is saved as integer value so it rounded up before storing.
				int ratingValue = (int)Math.round(calcRating(value,weight)[0]);//TODO:double!?
				latest_graph.addValue(from, to, type+"-s", ratingValue);//eg. rate-s
				latest_graph.addValue(to, from, type+"-d", ratingValue);//eg. rate-d
			}
		}
		ratings_modified = true;
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
			Reputationer r = new Reputationer(m,Str.arg(args,"network","ethereum"),Str.arg(args,"path",""),true);
			act(m,r,out,args);//daily states
			r.save();
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
		if (Str.has(args,"save","ratings")){
			r.save_ratings();
			return true;
		}
		if (Str.has(args,"save","ranks")){
			r.save_ranks();
			return true;
		}
		if (Str.has(args,"clear","ratings")){
			r.clear_ratings();
			return true;
		}
		if (Str.has(args,"clear","ranks")){
			r.clear_ranks();
			return true;
		}
		if (Str.has(args,"set","parameters")){
			set_parameters(r,args,false);
			return true;
		}
		if (Str.has(args,"update","ranks")){
			//compute time range as date given date==until==since+period (default period = 1)
			Date until = Time.day( Str.arg( args, Str.has(args,"date",null) ? "date" : "until" ,"today") );
			Date since = Time.day( Str.arg( args, "since", Time.day(until, false) ) );
			set_parameters(r,args,false);
			int period = (int)((r.params.periodMillis + Period.DAY - 1)/ Period.DAY);//need at least one day
			int computed = 0;
			env.debug("Updating "+r.name+" since "+Time.day(since,false)+" to "+Time.day(until,false)+" period "+period);
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
			r.set_ranks(Time.day(Str.arg(args,"date","today")),ranks);
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
					{
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
			set_parameters(r,args,true);
			Object[][] ratings = Str.get(args,new String[]{"from","type","to","value","weight","time"},new Class[]{null,null,null,Double.class,Integer.class,Date.class},new String[]{null,null,null,null,null,"today"});  
			if (AL.empty(ratings))
				return false;
			r.add_ratings(ratings);
			return true;
		}
		else
		if (Str.has(args,"load","ratings")){
			//TODO: two options - "file" and "folder"
			String path = Str.arg(args,"file",null);
			set_parameters(r,args,true);
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
							value = (new BigDecimal(tokens[5])).multiply(new BigDecimal(100));//translate to percents
							weight = new BigDecimal(tokens[14]);
						}else{//no weight => so it is payment in any range
							value = new BigDecimal(tokens[5]);
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
			//Object[][] factors = Str.get(args,new String[]{"factor"},new Class[]{Double.class},new String[]{"1.0"}); 
			if (!AL.empty(files) && files.length == 2 && !AL.empty(files[0])  && !AL.empty(files[1])){
				Summator s0 = new Summator(env,(String)files[0][0]);
				Summator s1 = new Summator(env,(String)files[1][0]);
				s0.normalize();
				s1.normalize();
				/*
				if (!AL.empty(factors) && factors.length > 0)
					s0.multiply(((Double)factors[0][0]).doubleValue());
				if (!AL.empty(factors) && factors.length > 1)
					s1.multiply(((Double)factors[1][0]).doubleValue());
				*/
				double[] p = s0.accuracyByThreshold(s1);
				double[] b = s0.accuraciesWithBalance(s1);
				if (p != null && p.length == 4){
					out.println("cnt\tavg_ref\tavg_ev"
							+"\ttotal\tmatch\tpct"
							+"\tacc"
							+"\tacc_g\tacc_bad\tacc_bal"+
							"\trmsd_g"+"\trmsd_b\trmsd");
					out.println(""+p[0]+"\t"+p[1]+"\t"+p[2]
							+"\t"+b[0]+"\t"+b[1]+"\t"+b[2]
							+"\t"+p[3]
							+"\t"+b[3]+"\t"+b[4]+"\t"+b[5]
							+"\t"+b[6]+"\t"+b[7]+"\t"+b[8]);
					out.println("cnt\t"+p[0]+"\navg_ref\t"+p[1]+"\navg_ev\t"+p[2]+"\n"
							+"total\t"+b[0]+"\nmatch\t"+b[1]+"\npct\t"+b[2]+"\n"
							+"acc\t"+p[3]+"\n"
							+"acc_g\t"+b[3]+"\nacc_b\t"+b[4]+"\nacc_bal\t"+b[5]+"\n"+
							"rmsd_g\t"+b[6]+"\nrmsd_b\t"+b[7]+"\nrmsd\t"+b[8]+"\n");
				}
				return true;
			}
		}
		return false;
	}
	
	private static void set_parameters(final Reputationer r, final String[] args, boolean ratings){
		//TODO: boolean logarithmicRanks = true; // whether or not apply log10(1+x) to ranks;
		//TODO: double defaultRating = 0.25; // default rating value for “overall rating” and “per-dimension” ratings;
		//TODO: long periodMillis = Period.DAY; // period of reputation recalculation/update;
		if (Str.has(args, "default", null))
			r.params.defaultReputation = Double.parseDouble(Str.arg(args, "default", String.valueOf(r.params.defaultReputation)));
		if (Str.has(args, "decayed", null))
			r.params.decayedReputation = Double.parseDouble(Str.arg(args, "decayed", String.valueOf(r.params.decayedReputation)));
		if (!ratings && Str.has(args, "ratings", null))//so there is no ambiguity on word "ratings"
			r.params.ratings = Double.parseDouble(Str.arg(args, "ratings", String.valueOf(r.params.ratings)));
		if (Str.has(args, "spendings", null))
			r.params.spendings = Double.parseDouble(Str.arg(args, "spendings", String.valueOf(r.params.spendings)));
		if (Str.has(args, "conservatism", null))
			r.params.conservatism = Double.parseDouble(Str.arg(args, "conservatism", String.valueOf(r.params.conservatism)));
		if (Str.has(args, "precision", null))
			r.params.ratingPrecision = new BigDecimal(Str.arg(args, "precision", String.valueOf(r.params.ratingPrecision)));
		if (Str.has(args,"fullnorm", null))
			r.params.normalizedRanks = Str.arg(args, "fullnorm", r.params.normalizedRanks ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"logratings", null))
			r.params.logarithmicRatings = Str.arg(args, "logratings", r.params.logarithmicRatings ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"weighting", null))
			r.params.weightingRatings = Str.arg(args, "weighting", r.params.weightingRatings ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"unrated", null))
			r.params.rankUnrated = Str.arg(args, "unrated", r.params.rankUnrated ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"denomination", null))
			r.params.denomination = Str.arg(args, "denomination", r.params.denomination ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"liquid", null))
			r.params.liquidRatings = Str.arg(args, "liquid", r.params.liquidRatings ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"downrating", null))
			r.params.implicitDownrating = Str.arg(args, "downrating", r.params.implicitDownrating ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"aggregation", null))
			r.params.temporalAggregation = Str.arg(args, "aggregation", r.params.temporalAggregation ? "true": "false").toLowerCase().equals("true");
		if (Str.has(args,"period", null))
			r.params.periodMillis = Integer.parseInt(Str.arg(args, "period", "1")) * Period.DAY;
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
		r.clear_ratings();
	
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
		t.assume(Writer.toString(a.toArray(new Object[][]{})),"((4 100) (3 67) (5 67))");

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
		t.assume(Writer.toString(a.toArray(new Object[][]{})),"((2 100) (1 78) (4 67) (3 45) (5 45))");
		
		//TODO: saving results and clearing on startup
	}
}
