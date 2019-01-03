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
package net.webstructor.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Period;
import net.webstructor.al.Set;
import net.webstructor.al.Time;
import net.webstructor.util.Array;
import net.webstructor.util.ArrayPositionComparator;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.core.Environment;
import net.webstructor.util.Reporter;

public abstract class SocialFeeder {
	public static final long MAX_CLUSTER_TIME = Period.MINUTE*10;//TODO: make configurable
	
	protected Environment body;
	protected String user_id;
	protected boolean obfuscate;//whether obfuscate user names or not setting
	protected HashMap users = new HashMap();
	protected HashMap words = new HashMap();
	protected ArrayList news = new ArrayList();
	protected LangPack langPack;
	protected Counter allWords = new Counter();
	protected HashMap periodsWords = new HashMap();
	protected HashMap periodsFriends = new HashMap();
	protected Counter periodsLikes = new Counter();
	protected Counter periodsComments = new Counter();
	protected Date since;
	protected Date until;
	protected String[] areas;
	protected StringBuilder detail = null;
	protected ArrayList detailItems = new ArrayList();
	protected TextMiner textsMiner = null;
	protected TextMiner peersMiner = null;
	protected HashMap newsLinks = null;
	protected int days;
	private int period = 31;//breakdown period: 1-days,7-week,31-month,92-quarter,365-year

	//TODO: fix hack
	public String errorMessage = null;
	
	public SocialFeeder(Environment body, String user_id, LangPack langPack, boolean obfuscate, Date since, Date until, String[] areas, int period) {
		this.langPack = langPack;
		this.user_id = user_id;
		this.obfuscate = obfuscate;
		this.body = body;
		this.since = since;
		this.until = until;
		this.areas = Array.toLower(areas);
		if (period < 1 && until != null && since != null){//if period is not passed as explicit parameter
			int period_days = new Period(until.getTime() - since.getTime()).getDays();
			if (period_days < 28)
				this.period = 1;//daily breakdown
			else
			if (period_days < 100)
				this.period = 7;//weekly breakdown
		}
	}

	public SocialFeeder(Environment body, String user_id, LangPack langPack, boolean obfuscate, Date since, Date until, String[] areas) {
		this(body,user_id,langPack,obfuscate,since,until,null,0);
	}
	
	public SocialFeeder(Environment body, String user_id, LangPack langPack, boolean obfuscate, Date since, Date until) {
		this(body,user_id,langPack,obfuscate,since,until,null);
	}
	
	public int getDays(){
		return days;
	}
	
	public void setDays(int days){
		this.days = days;
	}
	
	public boolean empty(){
		return news.isEmpty() && users.isEmpty();
	}
	
	public StringBuilder detail(){
		return detail;
	}
	
	public Date since(){
		return since;
	}
	
	public Date until(){
		return until;
	}
	
	protected void appendSpaced(StringBuilder content,String str){
		if (content.length() > 0)
			content.append(' ');
		content.append(str);
	}
	
	//virtual
	protected String getPeriodKey(Date times){
		String key;
		if (period == 1) { //daily
			key = Time.day(times,false);
		} else
		if (period == 7) { //weekly
			int days = new Period(until.getTime() - times.getTime()).getDays();
			int weeks = days / 7;
			Date dayfrom = Time.today(- weeks * 7);
			Date dayto = weeks == 0 ? until : Time.today(- (weeks - 1) * 7);
			key = Time.day(dayfrom,false) + " - " + Time.day(dayto,false);
		} else //monthly 
			key = Time.month(times);
		return key;
	}
	
	protected Counter getWordsPeriod(String periodKey){
		Counter period = (Counter)periodsWords.get(periodKey);
		if (period == null)
			periodsWords.put(periodKey, period = new Counter());
		return period;
	}

	protected Counter getFriendsPeriod(String periodKey){
		Counter period = (Counter)periodsFriends.get(periodKey);
		if (period == null)
			periodsFriends.put(periodKey, period = new Counter());
		return period;
	}

	//TODO: count by Date time too!?
	protected int countCommentsFromOthers(Object[][] comments){
		int count = 0;
		if (!AL.empty(comments))
			for (int i = 0; i < comments.length; i++)
				if (!AL.empty(comments[i]))
					if (!user_id.equals(comments[i][0]))
						count++;
		return count;
	}
	
	//TODO: count my comments and likes!?
	protected void countPeriod(Date times, /*int myLikes, int myComments,*/ int likes, int comments){
		//TODO: avoid converting times to yearmonth twice!
		//hit periods just in order to count it even of to words/friends found on it
		String periodKey = getPeriodKey(times);
		getWordsPeriod(periodKey);
		getFriendsPeriod(periodKey);
		periodsLikes.count(periodKey, likes);
		periodsComments.count(periodKey, comments);
	}
	
	interface Ranker {
		float rank(Object[] item);
	}

	public final Object[][] getRankedPeers(Ranker ranker){
		float max = 0;
		Collection values = users.values();
		Iterator it = values.iterator();
		Object[][] norm = new Object[values.size()][];
		for (int i = 0; i < norm.length; i++){
			Object[] item = (Object[])it.next();
			float sum = ranker.rank(item);
			if (max < sum)
				max = sum;
			norm[i] = new Object[]{new Float( sum ),getUserName(item),item[1],item[2],item[3],item[8]};
		}
		for (int i = 0; i < norm.length; i++){
			norm[i][0] = new Integer( Math.round( ((Float)norm[i][0]).floatValue() * 100 / max) );
		}
		Arrays.sort(norm,new ArrayPositionComparator(0));
		return norm;
	}
	
	//my_likes X (their_likes + their_comments) 
	public final Object[][] getFriends(){
		return getRankedPeers(new Ranker(){
			public float rank(Object[] item){
				return ((Integer)item[1]).floatValue() * (((Integer)item[2]).floatValue() + ((Integer)item[3]).floatValue());
			}
		});
	}
	
	//my_likes / (1 + their_likes + their_comments) 
	public final Object[][] getIdols(){
		return getRankedPeers(new Ranker(){
			public float rank(Object[] item){
				return ((Integer)item[1]).floatValue() / (((Integer)item[2]).floatValue() + ((Integer)item[3]).floatValue() + 1);
			}
		});
	}

	//(their_likes + their_comments) / (1 + my_likes)
	public final Object[][] getFans(){
		return getRankedPeers(new Ranker(){
			public float rank(Object[] item){
				return (((Integer)item[2]).floatValue() + ((Integer)item[3]).floatValue()) / (((Integer)item[1]).floatValue() + 1);
			}
		});
	}
	
	//sort by likes+comments count descending
	public final Object[][] getPeers(){
		return getRankedPeers(new Ranker(){
			public float rank(Object[] item){
				return ((Integer)item[2]).floatValue() + ((Integer)item[3]).floatValue();
			}
		});
	}
	
	//TODO: use universal function with ranking kernel
	public final Object[][] getLikedPeers(){
		float max = 0;
		Collection values = users.values();
		Iterator it = values.iterator();
		Object[][] norm = new Object[values.size()][];
		for (int i = 0; i < norm.length; i++){
			Object[] item = (Object[])it.next();
			float sum = ((Integer)item[1]).floatValue();
			if (max < sum)
				max = sum;
			norm[i] = new Object[]{new Float( sum ),getUserName(item),item[1],item[2],item[3]};
		}
		for (int i = 0; i < norm.length; i++){
			norm[i][0] = new Integer( Math.round( ((Float)norm[i][0]).floatValue() * 100 / max) );
		}
		Arrays.sort(norm,new ArrayPositionComparator(0));
		return norm;
	}

	final Counter normalizeByWords(Counter counter){
		return counter.normalizeBy(!AL.empty(langPack.words()) ? langPack.words() : allWords, 1);
	}

	final float normalizeByWord(String word, float val, float defaultValue){
		Counter words = !AL.empty(langPack.words()) ? langPack.words() : allWords;
		Object obj = words.get(word);
		float den = obj == null ? defaultValue : ((Integer)obj).floatValue();
		return val / den;
	}

	public final java.util.Set getPeersIds(){
		return users.keySet();
	}
	
	public final String[][] getPeersNames(){
		ArrayList names = new ArrayList(users.size());
		for (Iterator pit = users.values().iterator(); pit.hasNext();){
			Object[] peer = (Object[])pit.next();
			String nameString = getUserName(peer).toLowerCase();
			String[] nameSurname = nameString.split(" ");
			if (!AL.empty(nameSurname))
				names.add(nameSurname);
		} 
		return (String[][]) names.toArray(new String[][]{});
	}

	/**
	 * [0-Rank %, 1-Name, 2-Crosses count, 3-My likes count, 4-Their likes count, 5-Their comments count, 6-Their words, 7-Id]
	 * F(Ui) - per-word frequency for user i
	 * SUM( MIN(Ui,Ui) )/SUM( MAX(Ui,Uj) ) - similarity between Ui and Uj
	 * SUM( Ui * Ui )/SUM( MAX(Ui,Uj)2 ) - similarity between Ui and Uj
	 */ 
	public final Object[][] getSimilarPeers(boolean crossOverlap,boolean crossMultiplied,boolean normalized){
		boolean includingSelf = false;
		float max = 0;
		Collection values = users.values();
		Iterator it = values.iterator();
		Object[][] norm = new Object[values.size() + (includingSelf ? 1 : 0)][];
		Counter self = new Counter();
		if (normalized)
			self = normalizeByWords(self);
		//fill from my words
		for (Iterator wit = words.values().iterator(); wit.hasNext();){
			Object[] word = (Object[])wit.next();
			self.count(word[0],((Integer)word[5]).intValue());
		} 
		for (int i = 0; i < norm.length; i++){
			Object[] item;
			Counter user;
			if (i == 0 && includingSelf){
				item = new Object[]{"me",new Integer(0),new Integer(0),new Integer(0),self};
				user = (Counter)item[4];
			}else{
				item = (Object[])it.next();
				user = (Counter)item[4];
				if (normalized)
					user = normalizeByWords(user);
			}
			if (crossOverlap || crossMultiplied){
				//corellate self words with peer words
				Object[] cross = crossOverlap? self.crossOverlap(user) : self.crossMultiplied(user);
				Number value = (Number)cross[0];//crosses
				if (max < value.floatValue())
					max = value.floatValue();
				Object[][] ranked  = ((Counter)cross[1]).toRanked();
				Integer nonzero = new Integer( AL.empty(ranked)? 0 : ranked.length );
				norm[i] = new Object[]{value,getUserName(item),nonzero,item[1],item[2],item[3],new Object[]{ranked,user.toRanked()},item[8]};			
			}else{
				HashSet cross = new HashSet(user.keySet());
				cross.retainAll(self.keySet());
				Integer crosses = new Integer( cross.size() );
				if (max < cross.size())
					max = cross.size();
				String list = Counter.toString(cross);
				norm[i] = new Object[]{crosses,getUserName(item),crosses,item[1],item[2],item[3],new Object[]{list,user.toRanked()},item[8]};
			}
		}
		for (int i = 0; i < norm.length; i++){
			norm[i][0] = new Integer( Math.round( ((Number)norm[i][0]).floatValue() * 100 / max) );
		}
		Arrays.sort(norm,new ArrayPositionComparator(0));
		return norm;
	}

	public final int getNewsCount(){
		return news.size();
	}
	
	//sort by likes+commens count descending
	public final Object[][] getNews(){
		float max = 0;
		Object[][] norm = new Object[news.size()][];
		for (int i = 0; i < norm.length; i++){
			Object[] item = (Object[])news.get(i);
			float sum = ((Integer)item[1]).floatValue() + ((Integer)item[2]).floatValue();
			if (max < sum)
				max = sum;
			norm[i] = new Object[]{new Float( sum ),item[0],item[1],item[2],item[3],item[4],item[5]};
		}
		for (int i = 0; i < norm.length; i++){
			norm[i][0] = new Integer( Math.round( ((Float)norm[i][0]).floatValue() * 100 / max) );
		}
		Arrays.sort(norm,new ArrayPositionComparator(0));
		return norm;
	}

	public static Object[][] getBest(Object[][] norm,int limit,int threshold){
		if (AL.empty(norm))
			return norm;
		Arrays.sort(norm,new ArrayPositionComparator(0,1));
		float max = ((Float)norm[0][0]).floatValue();
		int passed = 0;
		for (int i = 0; i < norm.length && (limit <= 0 || i < limit); i++){
			int rank = Math.round( ((Float)norm[i][0]).floatValue() * 100 / max);
			norm[i][0] = new Integer( rank );
			if (rank >= threshold)
				passed = i;
		}
		if (limit > 0 && passed > limit)
			passed = limit;
		return (Object[][])Arrays.copyOfRange(norm, 0, passed);
	}
	
	//TODO: unify all variations below
	//TODO: use same normalization code with overloaded function class
	public final Object[][] getBestWordsLikedAndCommentedByAll(int limit,int threshold){
		int i = 0;
		Object[][] norm = new Object[words.size()][];
		for (Iterator it = words.values().iterator(); it.hasNext();){
			Object[] item = (Object[])it.next();
			// my_likes*(other_likes + comments)/posts
			float sum = (((Integer)item[1]).floatValue() * (((Integer)item[2]).floatValue() + ((Integer)item[3]).floatValue())) / ((Integer)item[4]).floatValue();
			sum = normalizeByWord((String)item[0],sum,1);
			norm[i] = new Object[]{new Float( sum ),item[0],item[1],item[2],item[3],item[4],item[5],new Float( sum )};
			i++;
		}
		return getBest(norm,limit,threshold);
	}

	public final Object[][] getWordsUsedAndLikedByMe(int limit,int threshold){
		int i = 0;
		Object[][] norm = new Object[words.size()][];
		for (Iterator it = words.values().iterator(); it.hasNext();){
			Object[] item = (Object[])it.next();
			//(my_likes+1)*usage_count/posts
			float sum = ((((Integer)item[1]).floatValue())+1)*(((Integer)item[5]).floatValue()) / ((Integer)item[4]).floatValue();
			sum = normalizeByWord((String)item[0],sum,1);
			norm[i] = new Object[]{new Float( sum ),item[0],item[1],item[2],item[3],item[4],item[5],new Float( sum )};
			i++;
		}
		return getBest(norm,limit,threshold);
	}
	
	//words sorted by my likes (normed by posts), 0-word, 1-mylike, 2-likes, 3-comments, 4-posts, 5-occurences
	public final Object[][] getWordsLikedByMe(int limit,int threshold){
		int i = 0;
		Object[][] norm = new Object[words.size()][];
		for (Iterator it = words.values().iterator(); it.hasNext();){
			Object[] item = (Object[])it.next();
			//my_likes/posts
			float sum = (((Integer)item[1]).floatValue()) / ((Integer)item[4]).floatValue();
			sum = normalizeByWord((String)item[0],sum,1);
			norm[i] = new Object[]{new Float( sum ),item[0],item[1],item[2],item[3],item[4],item[5],new Float( sum )};
			i++;
		}
		return getBest(norm,limit,threshold);
	}

	//words sorted by likes+comments (normed by posts), 0-word, 1-mylike, 2-likes, 3-comments, 4-posts, 5-occurences
	public final Object[][] getWordsLikedAndCommentedByOthers(int limit){
		if (words.size() == 0)
			return null;
		float max = 0;
		int i = 0;
		Object[][] norm = new Object[words.size()][];
		for (Iterator it = words.values().iterator(); it.hasNext();){
			Object[] item = (Object[])it.next();
			// (other_likes + comments)/posts
			float sum = ((Integer)item[4]).floatValue() == 0 ? 0 :
				(((Integer)item[2]).floatValue() + ((Integer)item[3]).floatValue())
				/ ((Integer)item[4]).floatValue();
			//float den = ((Integer)allWords.get(item[0])).floatValue();
			//sum /= den;
			sum = normalizeByWord((String)item[0],sum,1);
			if (max < sum)
				max = sum;
			norm[i] = new Object[]{new Float( sum ),item[0],item[1],item[2],item[3],item[4],item[5],new Float( sum )};
			i++;
		}
		
		Arrays.sort(norm,new ArrayPositionComparator(0));		
		Object[][] cutoff = new Object[limit > 0 ? Math.min(limit,norm.length): norm.length][];
		for (i = 0; i < norm.length && (limit > 0 && i < limit); i++){
			cutoff[i] = norm[i];
			norm[i][0] = new Integer( Math.round( ((Float)norm[i][0]).floatValue() * 100 / max) );
		}
		return cutoff;
	}
	
	public final Object[][] getWordsPeriods(){
		Object[][] v = new Object[periodsWords.size()][];
		int i = 0;
		float max = 0;
		for (Iterator it = periodsWords.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Counter counter = (Counter)periodsWords.get(key);
			counter = normalizeByWords(counter);
			Number likes = (Number)this.periodsLikes.get(key);
			if (likes == null)
				likes = new Integer(0);
			Number comments = (Number)this.periodsComments.get(key);
			if (comments == null)
				comments = new Integer(0); 
			int sum = likes.intValue() + comments.intValue();
			if (max < sum)
				max = sum;
			v[i++] = new Object[]{key,null,likes,comments,counter.toRanked()};
		}
		for (i = 0; i < v.length; i++){
			float value = (((Number)v[i][2]).floatValue() + ((Number)v[i][3]).floatValue())/max * 100;
			v[i][1] = new Integer(Math.round(value));
		}
		Arrays.sort(v,new ArrayPositionComparator(0,false));
		return v;
	}

	public final Object[][] getFriendsPeriods(){
		Object[][] v = new Object[periodsFriends.size()][];
		int i = 0;
		float max = 0;
		for (Iterator it = periodsFriends.keySet().iterator(); it.hasNext();){
			Object key = it.next();
			Counter counter = (Counter)periodsFriends.get(key);
			counter = normalizeByWords(counter);
			Number likes = (Number)this.periodsLikes.get(key);
			if (likes == null)
				likes = new Integer(0);
			Number comments = (Number)this.periodsComments.get(key);
			if (comments == null)
				comments = new Integer(0); 
			int sum = likes.intValue() + comments.intValue();
			if (max < sum)
				max = sum;
			Object[] rankedPeers = counter.toRanked();
			for (int pi = 0; pi < rankedPeers.length; pi++){
				Object[] peer = (Object[]) rankedPeers[pi];
				peer[0] = getUserName((String)peer[0]);
			}
			v[i++] = new Object[]{key,null,likes,comments,rankedPeers};
		}
		for (i = 0; i < v.length; i++){
			float value = (((Number)v[i][2]).floatValue() + ((Number)v[i][3]).floatValue())/max * 100;
			v[i][1] = new Integer(Math.round(value));
		}
		Arrays.sort(v,new ArrayPositionComparator(0,false));
		return v;
	}
	
	
	//0-word, 1-mylike, 2-likes, 3-comments, 4-posts, 5-occurences
	protected final void countWord(String key,Integer count,Boolean like,Integer likes,Integer comments, Integer posts){
		Integer myLike = new Integer(like != null && like.booleanValue()? 1 : 0);
		Object[] word = (Object[])words.get(key);
		if (word == null){
			word = new Object[]{key,myLike,likes,comments,new Integer(1),count};
			words.put(key, word);
		} else {
			word[1] = Counter.sum((Integer)word[1], myLike);//my likes count 
			word[2] = Counter.sum((Integer)word[2], likes);//likes count
			word[3] = Counter.sum((Integer)word[3], comments);//comments count
			word[4] = Counter.sum((Integer)word[4], posts);//new Integer(1));//post count
			word[5] = Counter.sum((Integer)word[5], count);//word count
		}
		allWords.count(key, count.intValue());
	}
	
	protected final void addPerCommentWords(){
		for (Iterator it = users.values().iterator(); it.hasNext();){
			Object[] user = (Object[])it.next();
			Counter counter = (Counter)user[4];
			for (Iterator cit = counter.keySet().iterator(); cit.hasNext();){
				String word = (String)cit.next();
				Integer count = (Integer)counter.get(word);
				allWords.count(word, count.intValue());
			}
		}
	}
	
	protected final String getUserName(Object[] user){
		return AL.empty(user) ? "" : user.length < 6 || AL.empty((String)user[5]) ? (String)user[0] : (String)user[0] + " " + (String)user[5];
	}
	
	protected final String getUserText(Object[] user){
		if (AL.empty(user) || user.length < 6 || ((List)user[6+1]).isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (Iterator it = ((List)user[6+1]).iterator(); it.hasNext();){
			String text = (String)it.next();
			if (!AL.empty(text)){
				if (sb.length() > 0)
					sb.append("\n\n");
				sb.append(text);
			}
		}
		return sb.toString();
	}
	
	public final String getUserName(String id){
		return getUserName((Object[])users.get(id));
	}
	
	/**
	 * 0-name, 1-my likes, 2-their likes, 3-their comments, 4-words, 5-surname, 6-image, 7-texts, 8-id
	 */
	protected final Object[] getUser(String id,String name,String surname,String image){
		Object[] user = (Object[])users.get(id);
		if (user == null){
			if (name == null)
				return null;
			if (obfuscate)
				name = langPack.obfuscate(name);
			user = new Object[]{name,new Integer(0),new Integer(0),new Integer(0),new Counter(),null,null,new ArrayList(),id};
			users.put(id, user);
		}
		if (!AL.empty(surname))
			user[5] = surname;
		if (!AL.empty(image))
			user[6] = image;
		return user;
	}

	/**
	 * 0-name, 1-my likes, 2-their likes, 3-their comments, 4-words, 5-surname, 6-image, 7-texts, 8-id
	 */
	public final Object[] getUser(String id,String name){
		return getUser(id,name,null,null);
	}

	protected final void cleanNonReferencedPeers(){
		HashSet removals = new HashSet();
		for (Iterator it = users.values().iterator(); it.hasNext();){
			Object[] user = (Object[])it.next();
			if ((user[1] == null || ((Number)user[1]).intValue() == 0) &&
				(user[2] == null || ((Number)user[2]).intValue() == 0) &&
				(user[3] == null || ((Number)user[3]).intValue() == 0))
				removals.add(user[8]);
		}
		for (Iterator it = removals.iterator(); it.hasNext();)
			users.remove(it.next());
	}
	
	//count all likes given to me by other
	protected void processLikes(Map likers,Date time){
		if (likers != null)
			for (Iterator it = likers.keySet().iterator(); it.hasNext();){
				String id = (String)it.next();
				String name = (String)likers.get(id);
				countLikes(id,name,time);
			}
	}

	//count like given to me by other
	protected final void countLikes(String id,String name,Date time){
		countLikes(id,name,time,1);
	}
	protected final void countLikes(String id,String name,Date time,int amount){
		if (user_id.equals(id))
			return;
		Object[] user = getUser(id,name);
		if (user != null){
			user[2] = new Integer(((Integer)user[2]).intValue()+amount);
			Counter friendsPeriod = getFriendsPeriod(getPeriodKey(time));
			//here we count likes/votes on particular peers by period (blended with comments, this far)
			friendsPeriod.count(id,amount);
		}
	}
	
	//count likes given by me to other
	protected final void countMyLikes(String id,String name){
		countMyLikes(id,name,1);
	}
	protected final void countMyLikes(String id,String name,int amount){
		if (user_id.equals(id))
			return;
		Object[] user = getUser(id,name);
		if (user != null)
			user[1] = new Integer(((Integer)user[1]).intValue()+amount);
	}

	//TODO: reuse it in extractUrls etc., re-using its logic for counting
	public static void countWords(LangPack langPack,String text,Counter counter){
		Set tokens = Parser.parse(text,AL.commas+AL.periods+AL.spaces,false,false,false,true);
		if (!AL.empty(tokens)) {
			for (int i = 0; i < tokens.size(); i++){
				String word = (String)tokens.get(i); 
				if (AL.isURL(word)) {
					;//urls.add(HtmlStripper.stripHtmlAnchor(word));
				} else
				if ((word = langPack.lowertrim(word) ).length() > 0 && !langPack.scrub(word))
					counter.count(word);
			}
		}
	}
	
	protected int countComments(String id,String name,String message,Date time){
		return countComments(id,name,message,time,0);
	}
	protected int countComments(String id,String name,String message,Date time,int amount){
		if (user_id.equals(id))
			return 0;
		Object[] user = getUser(id,name);
		if (user == null)
			return 0;
		user[3] = new Integer(((Integer)user[3]).intValue()+1);
		if (!AL.empty(message)){
			//count message words in user-specific counter
			countWords(langPack,message,(Counter)user[4]);
			if (user[7] instanceof List)
				((List)user[7]).add(message);
		}
		
		Counter friendsPeriod = getFriendsPeriod(getPeriodKey(time));
		//here we count comments on particular peers by period (blended with likes/votes, this far)
		friendsPeriod.count(id);
		
		return 1;
	}
		
	//TODO: count word use, likes and comments along the way!!!
	public final String[] extractUrls(String text, String[] links, Boolean like, Integer likes, Integer comments, Counter period){
		OrderedStringSet urls = new OrderedStringSet();
		if (!AL.empty(links)){
			for (int l = 0; l < links.length; l++)
				if (!AL.empty(links[l])) {
					String url = HtmlStripper.stripHtmlAnchor(links[l]);
					urls.add(url);
				}
		}
		Set tokens = Parser.parse(text,AL.commas+AL.periods+AL.spaces,false,false,false,true);
		if (tokens != null) {
			Counter counter = new Counter();
			for (int i = 0; i < tokens.size(); i++){
				String word = (String)tokens.get(i); 
				if (AL.isURL(word)) {
					String url = HtmlStripper.stripHtmlAnchor(word);
					urls.add(url);
				} else
				if ((word = langPack.lowertrim(word) ).length() > 0 && !langPack.scrub(word)){
					counter.count(word);
				}
			}
			for (Iterator it = counter.keySet().iterator(); it.hasNext();){
				String key = (String)it.next();
				Integer count = (Integer)counter.get(key); 
				countWord(key,count,like,likes,comments,new Integer(1));
				period.count(key,count.intValue());
			}
		}
		return (String[])urls.toArray(new String[]{});
	}

	protected void reportDetail(StringBuilder detail, String from, String uri, String id, String text, Date date, 
		Object[][] comments, OrderedStringSet links, HashMap likers, int others_likes, int user_likes, int comments_count/*, int reposts_count, int user_reposted*/){
		//int total_likes = others_likes+user_likes;
		if (detail != null && !AL.empty(text)){
			
			//TODO:
			//fill detailItems to display in table
			StringBuilder commentBuilder = new StringBuilder();
			if (!AL.empty(comments)){
				for (int j = 0; j < comments.length; j++){
					commentBuilder.append("<small><i>"
						+comments[j][1] //peer
						+"</i> +"+(((Boolean)comments[j][3]).booleanValue() ? "1/":"0/")+comments[j][4] //likes
						+": "+comments[j][2] //content
						+"</small><br>");
				}
			}
			text = Reporter.trimHTML(text, 200);//TODO: unify with Reporter.maxLength
			String textHTML = AL.empty(uri) ? text : "<a target=\"_blank\" href=\""+uri+"\">"+text+"</a>";
			Object[] detailItem = new Object[]{
					Time.day(date,false),
					//AL.empty(from) ? "" : " " + from,//TODO: what?
					"+"+user_likes+"/"+others_likes+"/"+comments_count
						+ (AL.empty(likers) ? "" : "<br><small>"+OrderedStringSet.fromStrings(likers.values()).toString("<br>")+"</small>"),
					textHTML,
					links.toArray(new String[]{}),
					commentBuilder.toString()
			};
			detailItems.add(detailItem);
		}
	}
	
	private final void clusterPeerCats(java.util.Set vocabulary){
		if (AL.empty(users)){
			peersMiner = null;
			return;
		}
		ArrayList names = new ArrayList(users.size());
		ArrayList texts = new ArrayList(users.size());
		for (Iterator us = users.values().iterator(); us.hasNext();){
			Object[] user = (Object[])us.next();
			String name = getUserName(user);
			String text = getUserText(user);
			if (AL.empty(text))
				continue;
			names.add(name);		
			texts.add(text);		
		}
		if (AL.empty(names)){
			body.debug("Spidering peer "+this.user_id+" no peers");
			peersMiner = null;
			return;
		}
		peersMiner = new TextMiner(body,this.langPack,System.currentTimeMillis()+MAX_CLUSTER_TIME,false)
			.setDocuments((String[])names.toArray(new String[]{}),(String[])texts.toArray(new String[]{}),vocabulary)
			.cluster();
	}
	
	public final Object[][] getPeerCats(){
		if (peersMiner == null)
			return null;
		java.util.Set cats = peersMiner.getCategories();
		if (AL.empty(cats))
			return null;
		Object[][] res = new Object[cats.size()][];
		//TODO: peers per category
		//TODO: sort peer and entire list?
		int c = 0;
		for (Iterator it = cats.iterator(); it.hasNext(); c++){
			Object key = it.next();
			java.util.Set catTexts = peersMiner.getCategoryDocuments(key);
			res[c] = new Object[]{key,new Integer(catTexts.size()),catTexts.toArray(new String[]{})};
		}
		Arrays.sort(res,new ArrayPositionComparator(1));
		return res;
	}

	// cluster name, cluster links, cluster features
	private final void clusterNewsCats(java.util.Set words){
		if (AL.empty(news)){
			body.debug("Spidering peer "+this.user_id+" no news");
			textsMiner = null;
			newsLinks = null;
			return;
		}
		newsLinks = new HashMap();
		String[] texts = new String[news.size()];
		for (int i = 0; i < news.size(); i++){
			Object newsItem[] = (Object[])news.get(i);
			texts[i] = (String)newsItem[4];
			newsLinks.put(newsItem[4], newsItem[5]);
		}
		textsMiner = new TextMiner(body,this.langPack,System.currentTimeMillis()+MAX_CLUSTER_TIME,false)
			.setDocuments(texts,words)
			.cluster();
	}
	
	public final Object[][] getNewsCats(){
		if (textsMiner == null)
			return null;
		java.util.Set cats = textsMiner.getCategories();
		if (AL.empty(cats))
			return null;
		Object[][] res = new Object[cats.size()][];
		int i = 0;
		for (Iterator it = cats.iterator(); it.hasNext(); i++){
			Object key = it.next();
			java.util.Set catTexts = textsMiner.getCategoryDocuments(key);
			//java.util.Set catFeats = m.getCategoryFatures(key);
			OrderedStringSet links = new OrderedStringSet();
			for (Iterator ct = catTexts.iterator(); ct.hasNext();){
				Object catText = ct.next();
				String[] ctLinks = (String[])newsLinks.get(catText);
				for (int j = 0; j < ctLinks.length; j++)
					links.add(ctLinks[j]);
			}
			links.sort();
			res[i] = new Object[]{key,new Integer(catTexts.size()),links.toArray(new String[]{}),catTexts.toArray(new String[]{})};
		}
		Arrays.sort(res,new ArrayPositionComparator(1));
		return res;
	}

	public final TextMiner getTextsMiner(){
		return textsMiner;
	}
	
	public final Object[][] getDetails(){
		if (AL.empty(detailItems))
			return null;
		Object[][] details = (Object[][])detailItems.toArray(new Object[][]{});
		//sort by date
		Arrays.sort(details,new ArrayPositionComparator(0,false));
		return details;
	}
	
	//TODO: consider if we need this at all as it removes important words such as "aigents", "web", "neuronet"
	//remove friend names from words, words by periods and words by peers
	public void postFeeding(){
		String[][] names = getPeersNames();
		if (!AL.empty(names))
			for (int i = 0; i < names.length; i++)
				if (!AL.empty(names[i]))
					for (int j = 0; j < names[i].length; j++){
						String word = names[i][j];
						if (words.containsKey(word))
							words.remove(word);
						for (Iterator it = periodsWords.values().iterator(); it.hasNext();){
							Counter period = (Counter)it.next();
							if (period.containsKey(word))
								period.remove(word);
						}
						for (Iterator it = users.values().iterator(); it.hasNext();){
							Object[] user = (Object[])it.next();
							if (user[4] != null && ((Counter)user[4]).containsKey(word))
								((Counter)user[4]).remove(word);
						}
					}
	}
	
	public void cluster(int minRelevantFeatureThresholdPercents){
		postFeeding();
		//TODO: get best words to cluster
		Object[][] bestWords = getWordsUsedAndLikedByMe(0,minRelevantFeatureThresholdPercents);
		if (!AL.empty(bestWords)){
			HashSet words = new HashSet();
			for (int i = 0; i < bestWords.length; i++)
				words.add((String)bestWords[i][1]);
			clusterNewsCats(words);
			clusterPeerCats(null);
		}
	}
}
