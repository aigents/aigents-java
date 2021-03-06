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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Thing;
import net.webstructor.util.ReportWriter;
import net.webstructor.data.Graph;
import net.webstructor.data.SocialFeeder;
import net.webstructor.data.Transcoder;
import net.webstructor.data.Translator;
import net.webstructor.peer.Grouper;
import net.webstructor.peer.Peer;
import net.webstructor.self.Matcher;
import net.webstructor.self.Siter;

/**
 * Keeps context of entire social network.
 * Also, keeps generic social reporting functionality.  
 * @author akolonin
 */
public abstract class Socializer extends HTTP implements Crawler {
	protected int timeout = 0;
	protected Matcher matcher;
	
	protected Socializer(Body body) {
		super(body);
		matcher = body.getMatcher();
	}

	@Override
	public int crawl(Siter siter){
		return -1;//default socializer can't "crawl" on url basis
	}
	
	@Override
	public boolean scalp(Siter siter,String path,ArrayList links,Collection topics) {
		return false;//so we can't scalp anything by default
	}
	
	public String getTokenSecret(Thing peer) {//most providers don't need token secret, by Twitter is special
		return null;
	}
	
	//TODO: include reporting days, but then need actial days from Feeder object 
	//public String reportingPath(String user_id,String format,int period_days){
	public String reportingPath(String user_id,int period_days,String format){
		format = ("json").equalsIgnoreCase(format) ? "json" : "html";
		return "reports/report_"+name()+"_"+user_id+
				(period_days > 0 ? "_"+String.valueOf(period_days) : "")+"."+format;
	}
	
	static private final int reportingPeriod = 1;//week
	static private final int reportingPeriods[] = {1,7,31,92,365,1461,5844};//day,week,month,quarter,year,4 years,16 years

	public String getPeerIdName(){
		return name()+" id";
	}
	
	public String getPeerName(String id) {
		//TODO try to find peer by getPeerIdName() above
		return id;
	}
	
	//virtual
	public int getPeriod(){
		return 5844;//16 years
	}
	
	//Can the data be freely browseable: true for Steemit and Golos; false for Facebook, Google and VKontakte  
	public boolean opendata() {
		return false;
	}

	//virtual, applies to blockchain-s and PayPal only
	public void resync(long blockOrTimemillis) {
	}
	
	//virtual, applies for blockchain-s only (so far)
	public Graph getGraph(Date date){
		return null;
	}
	
	//virtual, overrideable
	public Object[][] getReputation(String user_id, Date since, Date until){
		return null;//not defined by default
	}

	//virtual, overrideable
	public Graph getGraph(String[] seed, Date since, Date until, int range, String[] links, Set<String> nodes){
		return null;//not defined by default
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
		sb.append(name()).append('_').append(id).append('_').append(Time.day(since, false)).append('_').append(Time.day(until, false));
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
			if (feeder != null)
				feeder.setDays(daysPeriod);
			return feeder;
		}
		//dynamically find non-empty period or report on empty longest period
		SocialFeeder feeder = null;
		for (int i = reportingPeriod; i < reportingPeriods.length; i++){
			feeder = getCachedFeeders(id, token, key, Time.today(-reportingPeriods[i]), Time.today(+1), areas, fresh, feeds);
			if (feeder != null){
				feeder.setDays(reportingPeriods[i]);
				if  (!feeder.empty() || feeder.errorMessage != null)
					return feeder;
			}
			if (reportingPeriods[i] >= getPeriod())
				break;
		}
		return feeder;
	}
	
	
	//TODO: move code below to new ReportWriter and rename old ReportWriter to HtmlReporter
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
		File file = body.getFile(reportingPath(user_id,report_days,format));
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
	
	public String getCachedReports(final String user_id, final int report_days, final String format, boolean fresh){
		if (report_days > 0) //explicitly specified reporting period
			return getCachedReport(user_id, report_days, format, fresh);
		for (int i = reportingPeriod; i < reportingPeriods.length; i++){
			String cached = getCachedReport(user_id, reportingPeriods[i], format, fresh);
			if (cached != null)
				return cached;
			if (reportingPeriods[i] >= getPeriod())
				break;
		}
		return null;
	}
	
	public final String cacheReport(SocialFeeder feeder, HashMap feeds, String user_id, String access_token, String key, String name, String surname, String language, String format, java.util.Set options, int threshold) {
		File file = body.getFile(reportingPath(user_id,feeder.getDays(),format));
		//TODO: use feeds
		String report = report(feeder,feeds,user_id,access_token,key,name,surname,language,format,options,threshold);
		if (!AL.empty(report))
			fromString(file,report);
		return report;
	}

	public static final String wait_message = "Your report is being prepared, please check back in few minutes...";
	public static final String
		my_interests = "my interests",
		interests_of_my_friends = "interests of my friends",
		similar_to_me = "similar to me",
		words_of_my_friends = "words of my friends",
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
		my_posts_for_the_period = "my posts for the period",
		reputation = "reputation in community",
		social_graph = "social graph",
		communication_graph = "communication graph";
/*
? ``all connections`` - all other users connected my means of communications to the current user **(cross peers != null)**
- ``my interests`` - **default**, clusters of the posts/messages corresponding to interests of the current user, labeled by keywords typical to respective posts/messages
- ``interests of my friends`` - **default**, clusters of the posts/messages corresponding to interests of other users excluding the  current user, labeled by keywords typical to respective posts/messages
- ``similar to me`` - **default**, other users ranked by simlarity in respect to the current user
- ``best friends`` - **default**, other users that are the most involved in mutual communications (likes, votes, comments and mentions) with the current user
- ``fans`` - **default**, other users that are the most involved in communications (likes, votes, comments and mentions) directed to the current user but not the other way around
- ``like and comment me`` - other users who provide likes and votes in respect to the current user
- ``authorities`` - **default**, other users that are getting the most of communications (likes, votes, comments and mentions) from the current user
- ``reputation`` - **default**, list of the users with highest reputation score within entire reachable community, including any users visible given privacy restrictions
- ``social graph`` - **default**, rendering of the nearest social environment in form of interactive social graph 
- ``liked by me`` - other users that are getting the most of votes and likes from the current user
- ``my karma by periods`` - dynamic of the "karma" (social capital) for the current user
- ``my words by periods`` - **default**, words used by the current user getting most of attention (likes, votes, comments and mentions) from other users, broken down by time periods
- ``my friends by periods`` - **default**, other users paying attention (likes, votes, comments and mentions) to the current user, broken down by time periods
- ``my favorite words`` - **default**, words most oftenly used, liked and commented by the current user
- ``my posts liked and commented`` - **default**, posts of the current user most oftenly liked and commented by the other users
- ``my best words`` - words most oftenly liked and commented by all of the users including the current user 
- ``my words liked and commented`` - words of the current user most oftenly liked and commented by the other users
- ``words liked by me`` - words most oftenly liked by the current user 
- ``words of my friends`` - **default**, words most oftenly used by users other than current user
- ``my posts for the period`` - **default**, all posts by the current user for given period
*/
	public static final String[] report_options = {
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
		words_of_my_friends,
		my_posts_for_the_period,
		reputation,
		social_graph,
		communication_graph
	}; 
		
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
		ReportWriter rep = ReportWriter.reporter(body,format,writer);
		String full_name = rep.buildName(user_id, Writer.capitalize(user_name), Writer.capitalize(user_surname));
		String title = t.loc("Aigents Report for")+" "+Writer.capitalize(name()+" (beta)"+ " - "+full_name);
		//TODO fix hack
		if (feeder.errorMessage != null)
				title = title + " : " + feeder.errorMessage;
		//rep.initReport(title, feeder.since(), feeder.until(), null);
		rep.initReport(title, feeder.since(), feeder.until(), body.site());
		
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
	
	private static HashMap<String,String> ethereumHeadings = new HashMap<String,String>();
	private static HashMap<String,String> messengerHeadings = new HashMap<String,String>();
	static {
		ethereumHeadings.put("Friend", "Contragent");
		ethereumHeadings.put("Friends", "Contragents");
		ethereumHeadings.put("My likes", "My pays & calls");
		ethereumHeadings.put("Likes", "Pays");
		ethereumHeadings.put("Comments", "Calls");
		ethereumHeadings.put("Likes & Comments", "Pays & Calls");
		messengerHeadings.put("My likes", "My mentions & replies");
		messengerHeadings.put("Likes", "Mentions");
		messengerHeadings.put("Comments", "Replies");
		messengerHeadings.put("Likes & Comments", "Mentions & Replies");
	}
	
	private String[] transcode(String[] header) {
		HashMap<String,String> t = name().equals("ethereum") ? ethereumHeadings :
			name().equals("telegram") || name().equals("slack") ? messengerHeadings : null;
		if (t != null) {
			String[] h = new String[header.length];
			for (int i = 0; i < h.length; i++) {
				String s = t.get(header[i]);
				h[i] = s != null ? s : header[i];
			}
			return h;
		}
		return header;
	}
	
	private String[] peersHeadings(ReportWriter rep){
		return transcode(rep.needsId() ? new String[]{"Rank,%","Friend","My likes","Likes","Comments", "Id"}
		 							   : new String[]{"Rank,%","Friend","My likes","Likes","Comments"});
	}
	
	protected void reportPeer(ReportWriter rep, Translator t, SocialFeeder feeder,String title, String user_id, String name, String surname, java.util.Set options, int minPercent, int minCount, Object[][] cross_peers) {	
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
				t.loc(transcode(rep.needsId() ? new String[]{"Rank,%","Friend","Crosses","My likes","Likes","Comments","Words","Id"}
									: new String[]{"Rank,%","Friend","Crosses","My likes","Likes","Comments","Words"})),
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
		if (options.isEmpty() || options.contains(reputation))
			rep.table(reputation,t.loc(reputation),
				t.loc(new String[]{"Rank,%","Friend"}),
				getReputation(feeder.userId(),feeder.since(),feeder.until()),0/*minPercent*/,minCount,1000);

//TODO: unify two graphs below?
		if (options.isEmpty() || options.contains(social_graph)) {
//TODO make range/threshold/limit configurable in getGraph
//TODO make graph cached in feeder and only rendered here
			//first-order graph of social links only
			Set<String> peers = this instanceof Grouper ? ((Grouper)this).getGroupPeerIds(feeder.userId()) : null;
			Graph graph = AL.empty(feeder.getPeersIds()) ? null : getGraph(new String[] {feeder.userId()},feeder.since(),feeder.until(),2,social_links,peers);
			if (graph != null) {
				java.util.Set allpeers = graph.getVertices();
				Graph catgraph = feeder.getPeerCategoriesGraph("categories");//reverse - categorized
				if (catgraph != null) {
					graph.normalize();
					catgraph.normalize();
					java.util.Set allcategories = catgraph.getTargets();
					allpeers.addAll(catgraph.getSources());
					graph.addSubgraph(catgraph);
					graph.addProperty(allpeers, "is", "peer");
					graph.addProperty(allcategories, "is", "category");
				}
				String text = graph.toString(this instanceof Transcoder ? ((Transcoder)this) : null );
				rep.graph(social_graph,t.loc(social_graph),text,new String[] {"peer","category"},social_links,3/*horizontal directions*/,30);
			}
		}
//TODO extend it for use in non-Grouper kinds like Twitter and Reddit!?
		if (options.isEmpty() || options.contains(communication_graph)) 
		if (this instanceof Grouper) {
			Set<String> groups = ((Grouper)this).getGroupIds(feeder.userId());
			if (!AL.empty(groups)) {
				Graph graph = AL.empty(feeder.getPeersIds()) ? null : getGraph(groups.toArray(new String[] {}),feeder.since(),feeder.until(),2,communication_links,null);
				if (graph != null) {
					Set<String> peers = ((Grouper)this).getGroupPeerIds(feeder.userId());
					graph.addProperty(peers, "is", "peer");
					graph.addProperty(groups, "is", "group");
					graph.renameProperties("text", "label");
					graph.renameProperties("sources", "url");
					String text = graph.toString(this instanceof Transcoder ? ((Transcoder)this) : null );
					rep.graph(communication_graph,t.loc(communication_graph),text,new String[] {"peer","group"},communication_links,3/*both directions*/,20);
				}
			}
		}
		
		if (!options.isEmpty() && options.contains(liked_by_me))
			rep.table(liked_by_me,t.loc(liked_by_me),
				t.loc(peersHeadings(rep)),
				feeder.getLikedPeers(),minPercent,minCount);
		if (!options.isEmpty() && options.contains(my_karma_by_periods))
			rep.table(my_karma_by_periods,t.loc(my_karma_by_periods),
				t.loc(transcode(new String[]{"Period","Karma,%","Likes","Comments"})),
				feeder.getWordsPeriods(),0,0);
		if (options.isEmpty() || options.contains(my_words_by_periods))
			rep.table(my_words_by_periods,t.loc(my_words_by_periods),
				t.loc(transcode(new String[]{"Period","Karma,%","Likes","Comments","Words"})),
				feeder.getWordsPeriods(),0,0);
		if (options.isEmpty() || options.contains(my_friends_by_periods))
			rep.table(my_friends_by_periods,t.loc(my_friends_by_periods),
				t.loc(transcode(new String[]{"Period","Karma,%","Likes","Comments","Friends"})),
				feeder.getFriendsPeriods(),0,0);
		if (options.isEmpty() || options.contains(my_favorite_words))
			rep.table(my_favorite_words,t.loc(my_favorite_words),
				t.loc(transcode(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences"/*,"All likes & comments/Posts"*/})),
				feeder.getWordsUsedAndLikedByMe(500,1),minPercent,minCount);
		if (options.isEmpty() || options.contains(my_posts_liked_and_commented))
			rep.table(my_posts_liked_and_commented,t.loc(my_posts_liked_and_commented),
				t.loc(transcode(new String[]{"Rank,%","My likes","Likes","Comments","Date","Text","Links"})),
				feeder.getNews(),minPercent,minCount);	
		if (!options.isEmpty() && options.contains(my_best_words))
			rep.table(my_best_words,t.loc(my_best_words),
				t.loc(transcode(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences","All likes & comments/Posts"})),
				feeder.getBestWordsLikedAndCommentedByAll(500,1),minPercent,minCount);
		if (!options.isEmpty() && options.contains(my_words_liked_and_commented))
			rep.table(my_words_liked_and_commented,t.loc(my_words_liked_and_commented),
				t.loc(transcode(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences","Likes & comments/Posts"})),
				feeder.getWordsLikedAndCommentedByOthers(500),minPercent,minCount);
		if (!options.isEmpty() && options.contains(words_liked_by_me))
			rep.table(words_liked_by_me,t.loc(words_liked_by_me),
				t.loc(transcode(new String[]{"Rank,%","Word","My likes","Likes","Comments","Posts","Occurences","My likes/Posts"})),
				feeder.getWordsLikedByMe(500,1),minPercent,minCount);
		
		if (options.isEmpty() || options.contains(words_of_my_friends))
			rep.table(words_of_my_friends,t.loc(words_of_my_friends),
				t.loc(transcode(rep.needsId() ? new String[]{"Rank,%","Friend","Crosses","My likes","Likes","Comments","Words","Id"}
									: new String[]{"Rank,%","Friend","Crosses","My likes","Likes","Comments","Words"})),
				feeder.getWordsByPeers(true),minPercent,minCount);//is the best!!!
		
		
		Object[][] details;
		if ((options.isEmpty() || options.contains(my_posts_for_the_period)) && !AL.empty(details = feeder.getDetails())) {
			rep.table(my_posts_for_the_period,t.loc(my_posts_for_the_period),
				t.loc(transcode(new String[]{"Date","Likes & Comments","Image","Text & Comments","Links","Comments"})),
				details,0,0);
		}
		rep.closePeer();
	}

	public void matchPeerText(String peer_id, String text, Date time, String permlink, String imgurl) {
		Date attention_date = Time.today(-body.self().getInt(Body.attention_period,14));
		if (attention_date.compareTo(time) < 0) {
			try {
				Collection peers = body.storager.getByName(name() + " id", peer_id);
				if (!AL.empty(peers))
					matcher.matchPeersText(Peer.peerTopics(peers), text, time, permlink, imgurl);
			} catch (Exception e) {
				body.error("Siter matchig "+name()+" "+peer_id+" "+text,e);
			}
		}
	}

	//TODO unify with Schema.reverse
	public static String reverse(String type) {
		return reverses.get(type);
	}
	
	public static final Map<String,String> links = new HashMap<String,String>();
	public static final Map<String,String> reverses = new HashMap<String,String>();
	static {
		//setup the map
		links.put("commented", "comments");
		links.put("mentioned", "mentions");
		links.put("paid", "pays");
		links.put("voted", "votes");
		links.put("called", "calls");
		links.put("replied", "replies");
		links.put("authors", "authored");
		links.put("tagged", "tags");
		links.put("posted", "posts");
		//populate inversions
		for (String key : links.keySet())
			reverses.put(links.get(key), key);
	}

	public static final String[] social_links = new String[] {"comments","mentions","votes","pays","calls","commented","mentioned","voted","paid","called"};
	public static final String[] communication_links = new String[] {"posts","replies","authors","tags", "posted","replied","authored","tagged", "sources","text"};
/*
		- ontology:
			- nodes
				- post
					- tags (peer)
					- topics (topic)
					- areas (area)
					- authored (peer)
					- replies (to parent post)
					- replied (by child post)
					- posted (group)
				- peer
					- mentioned (peer)
					- mentions (peer)
					- tagged (by post)
					- authors (post)
					- groups (group)
				- topic/tag
					- posts/topicked (post)
				- area/domain
					- posts/areaed (post)
				- group
					- posts (post)
					- members (peer)
			- links
				- replies : post(which is replied) ->replied(by) <=> replies(to) <- post(which replies)
				- mentions : peer(who is mentioned) ->mentioned(by) <=> mentions <- peer(who mentions)
				- tags : peer(which is tagged) ->tagged(by) <=> tags <- post(which tags)
				- authors : post(which is authored) ->authored(by) <=> authors <- peer(who authors)
				- topics : topic(which is items) ->posts/topicked <=> topics <- post
				- areas : area(which is referenced) ->posts/areaed <=> areas <- post
				- members : group -> members <=> groups <- peer
				- posts : group -> posts <=> posted <- post
 */
}
