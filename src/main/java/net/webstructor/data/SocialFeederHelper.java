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
package net.webstructor.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.al.AL;
import net.webstructor.util.Reporter;
import net.webstructor.comm.Socializer;
import net.webstructor.core.Environment;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;

abstract public class SocialFeederHelper extends SocialFeeder {
	protected HashMap permlinksToPosts = new HashMap();
	protected HashMap<String,String> permlinksToAuthors = new HashMap<String,String>();
	protected HashMap permlinksToVotes = new HashMap();
	protected HashMap permlinksToComments = new HashMap();
	protected HashSet permlinksTagged = new HashSet();
	private Socializer api;
	
	public SocialFeederHelper(Environment body, Socializer api, String user_id, LangPack langPack, Date since, Date until, String[] areas, int period) {
		super(body,user_id,langPack,false,since,until,areas,period);
		this.api = api;
	}

	abstract protected String permlink_url(String base_url, String parent_permlink, String author, String permlink);
	
	abstract protected String base_url();
	
	protected String fix_url(String url) {
		String base = base_url();//expect NOT ended with slash
		if (!AL.empty(base) && !AL.empty(url))
			return url.startsWith("/") ? base + url : base + "/" + url;
		return url; 
	}
	
	protected int countVotes(String permlink,String author,Date time){
		HashSet voters = (HashSet)permlinksToVotes.get(permlink);
		if (AL.empty(voters))
			return 0;
		int counted = 0;
		for (Iterator it = voters.iterator(); it.hasNext();){
			String voter = (String)it.next();
			//count my likes to peers in advance across all comments/posts even if missed in the feed
			if (user_id.equals(voter) && !user_id.equals(author))
				countMyLikes(author,author);
			else
			if (user_id.equals(author) && !user_id.equals(voter)){
				countLikes(voter,voter,time);
				counted++;
			}
		}
		return counted;
	}
	
	/**
	 * Count my likes to peers in advance across all comments/posts even if missed in the feed.
	 */
	protected void processVote(String permlink,String voter,String author){
		HashSet votes = (HashSet)permlinksToVotes.get(permlink);
		if (votes == null)
			permlinksToVotes.put(permlink, votes = new HashSet());
		votes.add(voter);
		permlinksToAuthors.put(permlink, author);//for the case of hanged posts/comments from the older periods
	}
	
	public String processPost(Date times,String permlink,String author,String parent_permlink,String parent_author,String title, String body){
		/*
		//TODO: fix patches in original texts (in Steemit and Golos)!?
		boolean isPatch = body.indexOf("@@ ") == 0;
		if (isPatch)
			return null;
		
		//add title to body
		StringBuilder sb = new StringBuilder(!AL.empty(title) ? title : "");
		if (!AL.empty(body))
			sb.append(sb.length() > 0 ? ". " : "").append(body);
		body = sb.toString();
		
		OrderedStringSet links = new OrderedStringSet();
		ArrayList collectedLinks = new ArrayList();
		String text = HtmlStripper.convert(body," ",collectedLinks);		
		text = HtmlStripper.convertMD(text, collectedLinks, collectedLinks);//links and images both
		//translate url+text pairs to single urls
		for (int l = 0; l < collectedLinks.size(); l++) 
			links.add(((String[])collectedLinks.get(l))[0] );
		*/
		OrderedStringSet links = new OrderedStringSet();
		String text = parsePost(title,body,links);
		
		if (!AL.empty(text)){
			Integer comments = new Integer(0);
			Integer likes = new Integer(0);
			Boolean like = new Boolean(false); 
			if (!AL.empty(parent_permlink) && !AL.empty(parent_author)){
				String parent_path = parent_author+"."+parent_permlink;
				ArrayList commentList = (ArrayList)permlinksToComments.get(parent_path);
				if (commentList == null){
					commentList = new ArrayList();
					permlinksToComments.put(parent_path, commentList);
				}
//TODO: user name
//TODO: cleanup
//if (permlink.equals(parent_permlink))
//System.out.println("COMMENT:"+parent_author+"."+parent_permlink+"<-"+author+"."+permlink);
				commentList.add( new Object[]{author,author,text,new Boolean(false),new Integer(0),permlink,times});
			}else{
				//add only the latest entry because news are processed in reverse order
				Object[] news_item = (Object[]) permlinksToPosts.get(permlink);
				String[] links_array = (String[])links.toArray(new String[]{});
				for (int i = 0; i < links_array.length; i++) {
					String link = links_array[i];
					if (!AL.isURL(link))
						links_array[i] = fix_url(link);
				}
				if (news_item == null){
					news_item = new Object[]{like,likes,comments,times,
							text,links_array,permlink,author,times,parent_permlink};
					permlinksToPosts.put(permlink, news_item);
				}
			}
		}
		return text;
	}

	int[] getLikeCounts(String permlink){
		HashSet news_likes = (HashSet)permlinksToVotes.get(permlink);
		int my_like = 0;
		int their_likes = 0;
		if (!AL.empty(news_likes)){
			my_like = news_likes.contains(user_id) ? 1 : 0;
			their_likes = news_likes.size() - my_like;
		}
		return new int[]{my_like,their_likes};
	}

	/**
	 * Get map of likers/voters who liked/voted for the post/comment (should map id to name).
	 */
	HashMap getLikers(String permlink) {
		HashMap likers = new HashMap();
		HashSet news_likers = (HashSet)permlinksToVotes.get(permlink);
		if (news_likers != null) {
			for (Iterator it = news_likers.iterator(); it.hasNext();){
				String liker = (String)it.next();
				if (!liker.equals(user_id)){
//TODO: name?
					likers.put(liker, liker);
				}
			}
		}
		return likers;
	}
	
	/**
	 * Collect chained comments for permlink recursively into allComments, return count of comments from others.
	 */
	int collectComments(String permlink,ArrayList allComments){
		return collectComments(permlink, permlink, allComments, 0);
	}
	int collectComments(String rootlink,String permlink,ArrayList allComments,int depth){
		int othersComments = 0;
		ArrayList postComments = (ArrayList)permlinksToComments.get(permlink);
		if (depth > 100)
			body.error("Steemit/Golos comments stack overflow:"+permlink+" for "+rootlink,null);
		else
		if (!AL.empty(postComments)){
			for (int c = 0; c < postComments.size(); c++){
				Object[] comment = (Object[])postComments.get(c);
				String comment_permlink = (String)comment[0]+"."+(String)comment[5];
				int comment_likes[] = getLikeCounts(comment_permlink);
				comment[3] = new Boolean(comment_likes[0] > 0);
				comment[4] = new Integer(comment_likes[1]);
				allComments.add(comment);
				if (!user_id.equals(comment[0]))
					othersComments++;
				othersComments += collectComments(rootlink,comment_permlink,allComments,depth+1);
			}
		}
		return othersComments;
	}
	
	protected void postProcessPosts(String api_url){
		HashSet nonOrphanedComments = new HashSet();
		
		for (Iterator it = permlinksToPosts.values().iterator(); it.hasNext();){
			Object[] news_item = (Object[])it.next();
			String text = (String)news_item[4];
			String[] sources = (String[])news_item[5];
			String permlink = (String)news_item[6];
			String parent_permlink = (String)news_item[9];
			String author = (String)news_item[7];
			Date date_day = (Date)news_item[8];
			int news_likes[] = getLikeCounts(permlink);
			news_item[0] = new Boolean(news_likes[0] > 0);
			news_item[1] = new Integer(news_likes[1]);

			if (!AL.empty(areas) && !permlinksTagged.contains(permlink))//skip non-tagged if areas are given
				continue;

			news.add(news_item);

			countVotes(permlink,author,date_day);
			permlinksToVotes.remove(permlink);
			
			//process post comments
			ArrayList collectedComments = new ArrayList();
			int othersComments = collectComments(author+"."+permlink,collectedComments);
			Object[][] comments = (Object[][])collectedComments.toArray(new Object[][]{});
			news_item[2] = new Integer( othersComments );

			//process non-orphaned-comments
			int countedComments = 0;
			for (int ic = 0; ic < comments.length; ic++){
				Object[] comment = comments[ic];
				String commenter = (String)comment[0];
				String commentlink = (String)comment[5];
				//countVotes(permlink,commenter,date_day);//TODO:remove error!?
				countVotes(commentlink,commenter,date_day);
				permlinksToVotes.remove(commentlink);
				//TODO: user name
				countedComments += countComments(commenter,(String)comment[1],(String)comment[2],date_day);//id,name,text
				nonOrphanedComments.add(commentlink);
			}
			
			//OrderedStringSet links = new OrderedStringSet(sources);
			//(String[])links.toArray(new String[]{})
			Counter period = getWordsPeriod(getPeriodKey(date_day));
			countPeriod(date_day,news_likes[1],countedComments);
			
			sources = extractUrls(text,sources,(Boolean)news_item[0],(Integer)news_item[1],(Integer)news_item[2],period);
			news_item[5] = sources;

			//build uri from permlink
			//String uri = (AL.empty(parent_permlink)) ? null : api.base_url + "/" + parent_permlink + "/@" + author + "/" + permlink;
			String uri = permlink_url(api_url,parent_permlink,author,permlink);
			String img = null;
			String imgsrc = null;
			if (!AL.empty(sources)) {
				if (uri == null) for (String s : sources) if (AL.isURL(s) && !AL.isIMG(s)) {
					uri = s;
					break;
				}
				for (String s : sources) if (AL.isIMG(s)) {
					img = Reporter.img(uri, "height:auto;width:140px;", imgsrc = s);
					break;
				}
			}
			
			//dump to html buffer for report details section
			reportDetail(detail,
				author,//getUserName(from),
				uri,//uri
				permlink,//id
				text,
				date_day,
				comments,
				new OrderedStringSet(sources),
				getLikers(permlink),//likers,
				news_likes[1],//likes_count-user_likes,
				news_likes[0],//user_likes,
				othersComments,
				img);
			
			api.matchPeerText(author, text, date_day, uri, imgsrc);
		}

//TODO: count words here!?
//TODO: count likes on comment basis filtered as well!!!		
		//process orphaned comments
		for (Iterator it = permlinksToComments.values().iterator(); it.hasNext();){
			ArrayList comments = (ArrayList)it.next();
			for (Iterator ico = comments.iterator(); ico.hasNext();){
//TODO: user name?
				Object[] comment = (Object[])ico.next();
				String permlink = (String)comment[5];
				if (!nonOrphanedComments.contains(permlink) && //if it is orphaned comment and
						(AL.empty(areas) || permlinksTagged.contains(permlink))){ //and there is no tags or it is tagged
					String commenter = (String)comment[0];
					Date time = (Date)comment[6];
					int countedVotes = countVotes(permlink,commenter,time);
					permlinksToVotes.remove(permlink);
					int countedComments = countComments(commenter,(String)comment[1],(String)comment[2],time);//id,name,text
					if (countedComments > 0)//if other user
						countPeriod(time,countedVotes,countedComments);
					else {//if this user
						countPeriod(time,countedVotes,0);
						extractUrls((String)comment[2], null, new Boolean(false),new Integer(0), new Integer(0), getWordsPeriod( getPeriodKey((Date)comment[6])));
					}

				}
			}	
		}
		
		//do votes for missed posts/comments
		for (Iterator it = permlinksToVotes.keySet().iterator(); it.hasNext();){
			String permlink = (String)it.next();
			if (AL.empty(areas) || permlinksTagged.contains(permlink)){
				//time!
				int countedVotes = countVotes(permlink,permlinksToAuthors.get(permlink),since);//assume comments go to author of hanged posts for earlier period
				if (countedVotes > 0)//if other user
					countPeriod(since,countedVotes,0);
			}
		}

	}

}
