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
package net.webstructor.comm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.cat.StringUtil;
import net.webstructor.util.Reporter;

import net.webstructor.data.Graph;
import net.webstructor.data.SocialFeeder;
import net.webstructor.data.Translator;

/**
 * Keeps context of entire social network.
 * Also, keeps generic social reporting functionality.  
 * @author akolonin
 */
public abstract class Socializer extends HTTP {
	
	protected Socializer(Body body) {
		super(body);
	}

	//TODO: move the following to separate framework
	
	public abstract String provider();
	public abstract SocialFeeder getFeeder(String id, String token, String key, Date since, Date until, String[] areas) throws IOException;
	
	//TODO: include reporting days, but then need actial days from Feeder object 
	//public String reportingPath(String user_id,String format,int period_days){
	public String reportingPath(String user_id,int period_days,String format){
		format = ("json").equalsIgnoreCase(format) ? "json" : "html";
		return period_days > 0 ? "reports/report_"+provider()+"_"+user_id+"_"+String.valueOf(period_days)+"."+format
				: "report_"+provider()+"_"+user_id+"."+format;
	}
	
	static private final int reportingPeriod = 3;//month
	static private final int reportingPeriods[] = {1,7,31,92,365,10000};//day,week,month,quarter,year
	private HashSet cachedRequests = new HashSet(); 

	//Can the data be freely browseable: true for Steemit and Golos; false for Facebook, Google+ and VKontakte  
	public boolean opendata() {
		return false;
	}

	//virtual, applies for blockchain-s only
	public void resync(long block) {
	}
	
	//virtual, applies for blockchain-s only (so far)
	public Graph getGraph(Date date){
		return null;
	}
	
	//TODO keep not transient SocialFeeders, but serialized SocialFeeds
	private HashMap feeders = new HashMap();

	public void forget() {
		synchronized (feeders){
			feeders.clear();
		}
	}
	
	private String getFeedKey(String id, Date since, Date until, String[] areas){
		StringBuilder sb = new StringBuilder();
		sb.append(provider()).append('_').append(id).append('_').append(Time.day(since, false)).append('_').append(Time.day(until, false));
		if (!AL.empty(areas)){
			Arrays.sort(areas);
			for (int a = 0; a < areas.length; a++)
				sb.append('_').append(areas[a]);
		}
		return sb.toString();
	}

	//caching serialized SocialFeeds in SocialFeeder here
	protected SocialFeeder getCachedFeed(String id, String token, String key, Date since, Date until, String[] areas, boolean fresh) throws IOException{
		String feedKey = getFeedKey(id,since,until,areas);
		SocialFeeder feed = null;
		if (!fresh){
			synchronized (feeders){
				feed = (SocialFeeder)feeders.get(feedKey);
			}
			if (feed == null){
				;//TODO lookup file cache
			}
		}
		if (feed == null){
			feed = getFeeder(id, token, key, since, until, areas);
			synchronized (feeders){
				feeders.put(feedKey,feed);
			}
		}
		return feed;
	}

	//for each report, fill all SocialFeeds in the radius from per-period cache
	public SocialFeeder getCachedFeeders(String id, String token, String key, Date since, Date until, String[] areas, boolean fresh, HashMap feeds) throws IOException {
		int radius = opendata() ? 1 : 1; //TODO: input radius
		if (radius < 2 || feeds == null) {
			SocialFeeder feed = getCachedFeed(id, token, key, since, until, areas, fresh);//this have to be fresh
			feeds.put(id, feed);
			return feed;
		} else {
			SocialFeeder seed = null;//first feeder
			HashSet todo = new HashSet();
			//TODO: have this map provided as output parameter
			todo.add(id);//seed
			for (int r = 1; r <= radius; r++){
				for (Iterator i = todo.iterator(); i.hasNext();){
					String todoid = (String)i.next();
					if (feeds.containsKey(todoid))
						continue;//skip what is done
					SocialFeeder feed = getCachedFeed(todoid, token, key, since, until, areas, seed == null ? fresh : false);//this may need to be fresh only for the seed, non-seeds may be expired
					body.debug("Feeding: "+todoid);
					if (seed == null)//remember first seed feed
						seed = feed;
					feeds.put(todoid, feed);
					if (feed == null)
						continue;
					//TODO
					if (r < radius){//if not the last loop
						//get all used ids from the feed
						for (Iterator ids = feed.getPeersIds().iterator(); ids.hasNext();){
							//if not already in done, add to todoid
							String oid = (String)ids.next();
							if (!feeds.containsKey(oid))
								todo.add(oid);
						}
					}
				}
			}
			//TODO: remove all nulls from done!?
			return seed;
		}
	}
	
	public SocialFeeder getFeeder(String id, String token, String key, int daysPeriod, String[] areas, boolean fresh, HashMap feeds) throws IOException {
		if (daysPeriod > 0) {//explicitly specified reporting period
			SocialFeeder feeder = getCachedFeeders(id, token, key, Time.today(-daysPeriod), Time.today(+1), areas, fresh, feeds);
			feeder.setDays(daysPeriod);
			return feeder;
		}
		//dynamically find non-empty period
		for (int i = reportingPeriod; i < reportingPeriods.length; i++){
			SocialFeeder feeder = getCachedFeeders(id, token, key, Time.today(-reportingPeriods[i]), Time.today(+1), areas, fresh, feeds);
			if (feeder != null && (!feeder.empty() || feeder.errorMessage != null)){
				feeder.setDays(reportingPeriods[i]);
				return feeder;
			}
		}
		return null;
	}
	
	
	//TODO: move code below to new Reporter and rename old Reporter to HtmlReporter
	public void fromString(File file,String content){
		try {
			File parent = file.getParentFile();
			if (parent != null && !parent.exists())
				parent.mkdirs();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			try {
				writer.write(content);
				writer.close();
			} catch (IOException e) {
				body.error("Socializer saving "+file.getPath(), e);
			}
		} catch (FileNotFoundException e) {
			body.error("Socializer saving "+file.getPath(), e);
		}
	}
	
	public String toString(File file){
		StringBuilder sb = new StringBuilder();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			for (;;){
				String line = null;
				try {
					line = reader.readLine();
				} catch (IOException e) {
					body.error("Socializer loading "+file.getPath(), e);
				}
				if (line == null)
					break;
				sb.append(line);
			}
			try {
				reader.close();
			} catch (IOException e) {
				body.error("Socializer loading "+file.getPath(), e);
			}
		} catch (FileNotFoundException e) {
			body.error("Socializer loading "+file.getPath(), e);
		}
		return sb.toString();
	}

	private String getCachedReport(final String user_id, final int report_days, final String format, boolean fresh){
		File file = new File(reportingPath(user_id,report_days,format));
		long fileTime = file.lastModified();
		long thisTime = System.currentTimeMillis();
		long fileAge = thisTime - fileTime;
		//TODO: peer "short-term" check cycle for report "adhoc-ness"?
		if (file.exists() && file.canRead() && fileAge < Body.MAX_CHECK_CYCLE_MS){
			if (fresh)
				file.delete();//unconditionally re-render report
			else
				return toString(file);
		}
		return null;
	}
	
	private String getCachedReports(final String user_id, final int report_days, final String format, boolean fresh){
		if (report_days > 0) //explicitly specified reporting period
			return getCachedReport(user_id, report_days, format, fresh);
		for (int i = reportingPeriod; i < reportingPeriods.length; i++){
			String cached = getCachedReport(user_id, reportingPeriods[i], format, fresh);
			if (cached != null)
				return cached;
		}
		return null;
	}
	
	//TODO: get rid of "final String key" because not used!?
	public final String cachedReport(final String user_id, final String access_token, final String key, final String name, final String surname, final String language, final String format, final boolean fresh, final String query, final int threshold, final String period, final String[] areas) {
		//final int report_days = period == null ? 0 : StringUtil.toIntOrDefault(period,10,Body.RETROSPECTION_PERIOD_DAYS);
		final int report_days = period == null ? 0 : StringUtil.toIntOrDefault(period,10,0);
		String cached = getCachedReports(user_id, report_days, format, fresh);
		if (!AL.empty(cached))
			return cached;
		synchronized (cachedRequests) {
			if (!cachedRequests.contains(user_id)){
				cachedRequests.add(user_id);
				//TODO: all threads for profiles and spidereres to be run under the same Executor with given number of threads in the pool 
				final Runnable task = new Thread() {
				    public void run() {
						try {
							//TODO: use peer custom "long-term" retention period for "retrospective period"
							HashMap feeds = new HashMap();
							SocialFeeder feeder = getFeeder(user_id, access_token, key, report_days, areas, fresh, feeds);
							if (feeder != null){
								//cluster only if default or requested
								//TODO: fix this hack, do query parsing and clustering in other place!?
								HashSet options = options(query,report_options);
								if (options.isEmpty() || options.contains(my_interests) || options.contains(interests_of_my_friends))
									feeder.cluster(Body.MIN_RELEVANT_FEATURE_THRESHOLD_PERCENTS);
								cacheReport(feeder,feeds,user_id,access_token,key,name,surname,language,format,options,threshold);
							}
						} catch (IOException e) {
			    	    	body.error(provider()+" profiling error", e);
						}
						synchronized (cachedRequests) {
							cachedRequests.remove(user_id);
						}
				    }
				};
				((Thread)task).start();
			}
		}
		return "Your report is being prepared, please check back in few minutes...";
	}

	public final String cacheReport(SocialFeeder feeder, HashMap feeds, String user_id, String access_token, String key, String name, String surname, String language, String format, java.util.Set options, int threshold) {
		File file = new File(reportingPath(user_id,feeder.getDays(),format));
		//TODO: use feeds
		String report = report(feeder,feeds,user_id,access_token,key,name,surname,language,format,options,threshold);
		if (!AL.empty(report))
			fromString(file,report);
		return report;
	}

	static final String
		my_interests = "my interests",
		interests_of_my_friends = "interests of my friends",
		similar_to_me = "similar to me",
		best_friends = "best friends",
		fans = "fans",
		all_connections = "all connections",
		like_and_comment_me = "like and comment me",
		authorities = "authorities",
		liked_by_me = "liked by me",
		my_karma_by_periods = "my karma by periods",
		my_words_by_periods = "my words by periods",
		my_friends_by_periods = "my friends by periods",
		my_favorite_words = "my favorite words",
		my_posts_liked_and_commented = "my posts liked and commented",
		my_best_words = "my best words",
		my_words_liked_and_commented = "my words liked and commented",
		words_liked_by_me = "words liked by me",
		my_posts_for_the_period = "my posts for the period";
	static final String[] report_options = {
		my_interests,
		interests_of_my_friends,
		similar_to_me,
		best_friends,
		fans,
		all_connections,
		like_and_comment_me,
		authorities,
		liked_by_me,
		my_karma_by_periods,
		my_words_by_periods,
		my_friends_by_periods,
		my_favorite_words,
		my_posts_liked_and_commented,
		my_best_words,
		my_words_liked_and_commented,
		words_liked_by_me,
		my_posts_for_the_period
	}; 
		
	//TODO: move to utilities 
	HashSet options(String query, String[] possible){
		HashSet options = new HashSet();
		if (query != null && possible != null){
			query = query.toLowerCase();
			for (int i = 0; i < possible.length; i++)
				if (query.contains(possible[i]))
					options.add(possible[i]);
		}
		return options;
	}

	//TODO move to separate package
	void incMapIntArray(HashMap map, String key, int[] ints){
		if (ints == null)
			return;
		int[] olds = (int[])map.get(key);
		if (olds == null)
			map.put(key, ints);
		else {
			for (int i = 0; i < olds.length && i < ints.length; i++)
				olds[i] += ints[i];
			map.put(key, olds);
		}
	}

	//TODO: local-society-wise reputation-based evaluation in two rounds, with round-1-reputation-weighting?
	//TODO: harmonize metrics and terms (Significance,Popularity,Activity) with Leadership, Karma and Followership 
	Object[][] getFeedCross(HashMap feeds,String user_id,String full_name){
		if (feeds.size() > 1)
			return getFeedCross2(feeds,user_id,full_name);//HACK: is dividive placement if too many nodes
		boolean log10 = false;
		HashMap all = new HashMap(); 
		HashMap heads = new HashMap(); 
		HashMap peers = new HashMap(); 
		//TODO: don't count symmetric links twice!?
		//for each head's feed i
		for (Iterator i = feeds.keySet().iterator(); i.hasNext();){
			String i_id = (String)i.next();
			//remember the head user
			heads.put(i_id, user_id.equals(i_id) ? full_name : i_id );//TODO: fix hack
			//for each peer's link j in head's feed i
			SocialFeeder feed = (SocialFeeder)feeds.get(i_id);
			java.util.Set j_ids = feed.getPeersIds();
			for (Iterator j = j_ids.iterator(); j.hasNext();){				
				String j_id = (String)j.next();
				//remember the peer user
				peers.put(j_id, feed.getUserName(j_id));
				//count 'my likes' to Oi and Ij
				//count 'their likes and comments' to Oj and Ii
				Object[] j_peer = feed.getUser(j_id,null);
				int Oi = ((Integer)j_peer[1]).intValue(); 
				int Ij = Oi;
				int Oj = ((Integer)j_peer[2]).intValue()+((Integer)j_peer[3]).intValue(); 
				int Ii = Oj;
				incMapIntArray(all,i_id,new int[]{Oi,Ii});
				incMapIntArray(all,j_id,new int[]{Oj,Ij});
			}
		}
		
		//get limits for heads 
		double headMAXO = 0;
		double headMAXI = 0;
		//find MAX(O) and MAX(I)
		for (Iterator a = heads.keySet().iterator(); a.hasNext();){
			String id = (String)a.next();
			int[] OI = (int[])all.get(id);
			//count attention counters normalized by peers on society
			double out = OI == null ? 0 : ((double)OI[0]);
			double inc = OI == null ? 0 : ((double)OI[1]);
			if (headMAXO < out) headMAXO = out;
			if (headMAXI < inc) headMAXI = inc;
		}
		if (log10){
			headMAXI = Math.log10(headMAXI * 10 + 1);
			headMAXO = Math.log10(headMAXO * 10 + 1);
		}
		
		//get limits for peers 
		double peerMAXO = 0;
		double peerMAXI = 0;
		//find MAX(O) and MAX(I)
		for (Iterator a = peers.keySet().iterator(); a.hasNext();){
			String id = (String)a.next();
			int[] OI = (int[])all.get(id);
			//count attention counters normalized by peers on society
			double out = ((double)OI[0]);
			double inc = ((double)OI[1]);
			if (peerMAXO < out) peerMAXO = out;
			if (peerMAXI < inc) peerMAXI = inc;
		}
		if (log10){
			peerMAXI = Math.log10(peerMAXI * 10 + 1);
			peerMAXO = Math.log10(peerMAXO * 10 + 1);
		}
		
		//for each Ok and Ik
		Object[][] res = new Object[all.size()][];
		int cnt = 0;
		for (Iterator a = all.keySet().iterator(); a.hasNext();){
			String id = (String)a.next();
			int[] OI = (int[])all.get(id);
			//count attention counters normalized by peers on society
			double out = ((double)OI[0]);
			double inc = ((double)OI[1]);
			if (log10){
				out = Math.log10(out * 10 + 1);
				inc = Math.log10(inc * 10 + 1);
			}
			String name;
			double Ok, Ik, Rk;//Activity,Popularity,Significance
			if (heads.containsKey(id)){
				name = (String)heads.get(id);
				Ok =  out == 0 ? 0 : out/headMAXO;
				Ik =  inc == 0 ? 0 : inc/headMAXI;
			} else{ 
				name = (String)peers.get(id);
				Ok =  out == 0 ? 0 : out/peerMAXO;
				Ik =  inc == 0 ? 0 : inc/peerMAXI;
			}
			Rk =  ((1 - Ok + Ik)/2);
			res[cnt++] = new Object[]{new Integer((int)Math.round(Rk*100)),new Integer((int)Math.round(Ik*100)),new Integer((int)Math.round(Ok*100)),name,new Integer(OI[1]),new Integer(OI[0]),id};
		}
		Arrays.sort(res,new Comparator(){
			public int compare(Object arg0, Object arg1) {
				return -((Integer)((Object[])arg0)[0]).compareTo((Integer)((Object[])arg1)[0]);
			}
		});
		return res;
	}
	
	
	Object[][] getFeedCross2(HashMap feeds,String user_id,String full_name){
		boolean log10 = true;
		HashMap all = new HashMap(); 
		HashMap heads = new HashMap(); 
		HashMap peers = new HashMap();
		//TODO: don't count symmetric links twice!?
		//for each head's feed i
		for (Iterator i = feeds.keySet().iterator(); i.hasNext();){
			String i_id = (String)i.next();
			//remember the head user
			heads.put(i_id, user_id.equals(i_id) ? full_name : i_id );//TODO: fix hack
			//for each peer's link j in head's feed i
			SocialFeeder feed = (SocialFeeder)feeds.get(i_id);
			java.util.Set j_ids = feed.getPeersIds();
			for (Iterator j = j_ids.iterator(); j.hasNext();){				
				String j_id = (String)j.next();
				//remember the peer user
				peers.put(j_id, feed.getUserName(j_id));
				//count 'my likes' to Oi and Ij
				//count 'their likes and comments' to Oj and Ii
				Object[] j_peer = feed.getUser(j_id,null);
				int Oi = ((Integer)j_peer[1]).intValue(); 
				int Ij = Oi;
				int Oj = ((Integer)j_peer[2]).intValue()+((Integer)j_peer[3]).intValue(); 
				int Ii = Oj;
				incMapIntArray(all,i_id,new int[]{Oi,Ii});
				incMapIntArray(all,j_id,new int[]{Oj,Ij});
			}
		}
		
		//for each Ok and Ik
		Object[][] res = new Object[all.size()][];
		int cnt = 0;
		double headMAXO = 0;
		double headMAXI = 0;
		double headMAXA = 0;
		double peerMAXO = 0;
		double peerMAXI = 0;
		double peerMAXA = 0;
		for (Iterator a = all.keySet().iterator(); a.hasNext();){
			String id = (String)a.next();
			int[] OI = (int[])all.get(id);
			//count attention counters normalized by peers on society
			double out = ((double)OI[0]);
			double inc = ((double)OI[1]);
			double dout = log10 ? Math.log10(out * 10 + 1) : out;
			double dinc = log10 ? Math.log10(inc * 10 + 1) : inc;
			double daut =  (log10) ? dinc/(dout + 1) : inc/(out + 1);//Authority
			String name;
			if (heads.containsKey(id)){
				name = (String)heads.get(id);
				if (headMAXO < dout) headMAXO = dout;
				if (headMAXI < dinc) headMAXI = dinc;
				if (headMAXA < daut) headMAXA = daut;
			} else{ 
				name = (String)peers.get(id);
				if (peerMAXO < dout) peerMAXO = dout;
				if (peerMAXI < dinc) peerMAXI = dinc;
				if (peerMAXA < daut) peerMAXA = daut;
			}
			//                        0               	1    			2    				3    4                 5				6
			res[cnt++] = new Object[]{new Double(daut),new Double(dinc),new Double(dout),name,new Double(OI[1]),new Double(OI[0]),id};
		}
		
		for (int i = 0; i < res.length; i++){
			Object[] o = res[i];
			String id = (String)o[6];
			double aut,inc,out;
			if (heads.containsKey(id)){
				aut = headMAXA == 0 ? 0 : ((Double)o[0]).doubleValue() / headMAXA;
				inc = headMAXI == 0 ? 0 : ((Double)o[1]).doubleValue() / headMAXI;
				out = headMAXO == 0 ? 0 : ((Double)o[2]).doubleValue() / headMAXO;
			} else{ 
				aut = peerMAXA == 0 ? 0 : ((Double)o[0]).doubleValue() / peerMAXA;
				inc = peerMAXI == 0 ? 0 : ((Double)o[1]).doubleValue() / peerMAXI;
				out = peerMAXO == 0 ? 0 : ((Double)o[2]).doubleValue() / peerMAXO;
			}
			o[0] = new Integer((int)Math.round(aut*100));
			o[1] = new Integer((int)Math.round(inc*100));
			o[2] = new Integer((int)Math.round(out*100));
		}
		
		Arrays.sort(res,new Comparator(){
			public int compare(Object arg0, Object arg1) {
				return -((Integer)((Object[])arg0)[0]).compareTo((Integer)((Object[])arg1)[0]);
			}
		});
		return res;
	}
		
	
	final public String report(SocialFeeder feeder, HashMap feeds, String user_id, String access_token, String key, String user_name, String user_surname, String language, String format, java.util.Set options, int minPercent) {
		int minCount = 10;
		StringWriter writer = new StringWriter();
		Translator t = body.translator(language);
		Reporter rep = Reporter.reporter(body,format,writer);
		String full_name = rep.buildName(user_id, Writer.capitalize(user_name), Writer.capitalize(user_surname));
		String title = t.loc("Aigents Report for")+" "+Writer.capitalize(provider()+" (beta)"+ " - "+full_name);
		//TODO fix hack
		if (feeder.errorMessage != null)
				title = title + " : " + feeder.errorMessage;
		rep.initReport(title, feeder.since(), feeder.until());
		
		//adjust reputations and optionally render connections
		//TODO add connection distance (related on primary feeder)?
		Object[][] cross_peers = options.contains(all_connections) ? getFeedCross(feeds,user_id,full_name) : null;

		reportPeer(rep,  t,  feeder, title,  user_id,  user_name,  user_surname,  options,  minPercent,  minCount, cross_peers);
		if (feeds != null && feeds.size() > 1)
			for (Iterator i = feeds.keySet().iterator(); i.hasNext();){
				String id = (String)i.next();
				SocialFeeder feed = (SocialFeeder)feeds.get(id);
				if (!id.equals(user_id) && feed != null){
					reportPeer(rep,  t,  feed, title,  id,  null,  null,  options,  minPercent,  minCount, null);
				}
			}
		
		rep.closeReport();
		String result = writer.toString();
		return result;
	}
	
	private String heading(String heading){
		return !provider().equals("ethereum") ? heading 
			: "Likes".equals(heading) ? "Pays" : "Comments".equals(heading) ? "Calls" : "Friends".equals(heading) ? "Contragents" : heading;
	}
	private String[] peersHeadings(Reporter rep){
		if (!provider().equals("ethereum"))
			return rep.needsId() ? new String[]{"Rank,%","Friend","My likes","Likes","Comments", "Id"}
								 : new String[]{"Rank,%","Friend","My likes","Likes","Comments"};
		else 
			return rep.needsId() ? new String[]{"Rank,%","Contragent","Paid","Pays","Calls", "Id"}
		 						 : new String[]{"Rank,%","Contragent","Paid","Pays","Calls"};
	}
	
	private void reportPeer(Reporter rep, Translator t, SocialFeeder feeder,String title, String user_id, String name, String surname, java.util.Set options, int minPercent, int minCount, Object[][] cross_peers) {	
		String perPeriod = t.loc("number of posts")+" "+feeder.getNewsCount();
		rep.initPeer(user_id, Writer.capitalize(name), Writer.capitalize(surname), perPeriod, feeder.since(), feeder.until());
		
		if (cross_peers != null && options.contains(all_connections)){
			rep.table(all_connections,t.loc(all_connections),
				t.loc(rep.needsId() ? new String[]{"Significance,%","Popularity,%","Activity,%","Friend","Attention got","Attention spent","Id"}
									: new String[]{"Significance,%","Popularity,%","Activity,%","Friend","Attention got","Attention spent"}),
				cross_peers,0,0);
		}
		
		if (options.isEmpty() || options.contains(my_interests))
			rep.table(my_interests,t.loc(my_interests),
				t.loc(new String[]{"Topics","Number of posts"}),
				feeder.getNewsCats(),1,0);
		if (options.isEmpty() || options.contains(interests_of_my_friends))
			rep.table(interests_of_my_friends,t.loc(interests_of_my_friends),
				t.loc(new String[]{"Topics","Number of friends","Friends"}),
				feeder.getPeerCats(),1,0);
		if (options.isEmpty() || options.contains(similar_to_me))
			rep.table(similar_to_me,t.loc(similar_to_me),
				t.loc(rep.needsId() ? new String[]{"Rank,%","Friend","Crosses","My likes","Likes","Comments","Words","Id"}
									: new String[]{"Rank,%","Friend","Crosses","My likes","Likes","Comments","Words"}),
				feeder.getSimilarPeers(true,false,true),minPercent,minCount);//is the best!!!
		if (options.isEmpty() || options.contains(best_friends))
			rep.table(best_friends,t.loc(best_friends),
				t.loc(peersHeadings(rep)),
				feeder.getFriends(),minPercent,minCount);
		if (options.isEmpty() || options.contains(fans))
			rep.table(fans,t.loc(fans),
				t.loc(peersHeadings(rep)),
				feeder.getFans(),minPercent,minCount);
		if (!options.isEmpty() && options.contains(like_and_comment_me))
			rep.table(like_and_comment_me,t.loc(like_and_comment_me),
				t.loc(peersHeadings(rep)),
				feeder.getPeers(),minPercent,minCount);
		if (options.isEmpty() || options.contains(authorities))
			rep.table(authorities,t.loc(authorities),
				t.loc(peersHeadings(rep)),
				feeder.getIdols(),minPercent,minCount);
		if (!options.isEmpty() && options.contains(liked_by_me))
			rep.table(liked_by_me,t.loc(liked_by_me),
				t.loc(peersHeadings(rep)),
				feeder.getLikedPeers(),minPercent,minCount);
		if (!options.isEmpty() && options.contains(my_karma_by_periods))
			rep.table(my_karma_by_periods,t.loc(my_karma_by_periods),
				t.loc(new String[]{"Period","Karma,%","Likes","Comments"}),
				feeder.getWordsPeriods(),0,0);
		if (options.isEmpty() || options.contains(my_words_by_periods))
			rep.table(my_words_by_periods,t.loc(my_words_by_periods),
				t.loc(new String[]{"Period","Karma,%",heading("Likes"),heading("Comments"),"Words"}),
				feeder.getWordsPeriods(),0,0);
		if (options.isEmpty() || options.contains(my_friends_by_periods))
			rep.table(my_friends_by_periods,t.loc(my_friends_by_periods),
				t.loc(new String[]{"Period","Karma,%",heading("Likes"),heading("Comments"),heading("Friends")}),
				feeder.getFriendsPeriods(),0,0);
		if (options.isEmpty() || options.contains(my_favorite_words))
			rep.table(my_favorite_words,t.loc(my_favorite_words),
				t.loc(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences"/*,"All likes & comments/Posts"*/}),
				feeder.getWordsUsedAndLikedByMe(500,1),minPercent,minCount);
		if (options.isEmpty() || options.contains(my_posts_liked_and_commented))
			rep.table(my_posts_liked_and_commented,t.loc(my_posts_liked_and_commented),
				t.loc(new String[]{"Rank,%","My likes","Likes","Comments","Date","Text","Links"}),
				feeder.getNews(),minPercent,minCount);	
		if (!options.isEmpty() && options.contains(my_best_words))
			rep.table(my_best_words,t.loc(my_best_words),
				t.loc(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences","All likes & comments/Posts"}),
				feeder.getBestWordsLikedAndCommentedByAll(500,1),minPercent,minCount);
		if (!options.isEmpty() && options.contains(my_words_liked_and_commented))
			rep.table(my_words_liked_and_commented,t.loc(my_words_liked_and_commented),
				t.loc(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences","Likes & comments/Posts"}),
				feeder.getWordsLikedAndCommentedByOthers(500),minPercent,minCount);
		if (!options.isEmpty() && options.contains(words_liked_by_me))
			rep.table(words_liked_by_me,t.loc(words_liked_by_me),
				t.loc(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences","My likes/Posts"}),
				feeder.getWordsLikedByMe(500,1),minPercent,minCount);
		if ((options.isEmpty() || options.contains(my_posts_for_the_period)) && !AL.empty(feeder.getDetails()))
			rep.table(my_posts_for_the_period,t.loc(my_posts_for_the_period),
				t.loc(new String[]{"Date","Likes & Comments","Text & Comments","Links","Comments"}),
				feeder.getDetails(),0,0);
		rep.closePeer();
	}
}
