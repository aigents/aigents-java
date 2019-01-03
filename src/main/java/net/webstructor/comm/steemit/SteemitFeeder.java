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
package net.webstructor.comm.steemit;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.util.Array;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.comm.HTTP;
import net.webstructor.core.Environment;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.data.OrderedStringSet;
import net.webstructor.data.SocialFeeder;

class SteemitFeeder extends SocialFeeder {
	protected HashMap permlinksToPosts = new HashMap();
	protected HashMap permlinksToVotes = new HashMap();
	protected HashMap permlinksToComments = new HashMap();
	protected HashSet permlinksTagged = new HashSet();
	protected Steemit api;
	
	public static final int BLOCK_SIZE = 1000; 
	
	public static final String TIME_FORMAT = "yyyy-MM-dd"; 
	
	public SteemitFeeder(Environment body, Steemit api, String user_id, LangPack langPack, Date since, Date until, String[] areas, int period) {
		super(body,user_id,langPack,false,since,until,areas,period);
		this.api = api;
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
//TODO: this on filter basis
		/*
		if (user_id.equals(voter) && !user_id.equals(author))
			countMyLikes(author,author);
		else
		if (user_id.equals(author) && !user_id.equals(voter))
			countLikes(voter, voter);
			*/
	}
	
	//TODO: MD parsing for links
	public String processPost(Date times,String permlink,String author,String parent_permlink,String parent_author,String title, String body){
		//TODO: fix patches in original texts!?
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
		//translate url+text pairs to single urls
		for (int l = 0; l < collectedLinks.size(); l++) 
			links.add(((String[])collectedLinks.get(l))[0] );
		
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
				if (news_item == null){
					news_item = new Object[]{like,likes,comments,times,
							text,(String[])links.toArray(new String[]{}),permlink,author,times};
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
				String comment_permlink = (String)comment[5];
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
	
	void postProcessPosts(){
		HashSet nonOrphanedComments = new HashSet();
		
		/*for (int i = 0; i < news.size(); i++){
			Object[] news_item = (Object[])news.get(i);*/
		for (Iterator it = permlinksToPosts.values().iterator(); it.hasNext();){
			Object[] news_item = (Object[])it.next();
			String text = (String)news_item[4];
			String[] sources = (String[])news_item[5];
			String permlink = (String)news_item[6];
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
			int othersComments = collectComments(permlink,collectedComments);
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

			//dump to html buffer for report details section
			reportDetail(detail,
				author,//getUserName(from),
				null,//uri
				permlink,//id
				text,
				date_day,
				comments,
				new OrderedStringSet(sources),
				getLikers(permlink),//likers,
				news_likes[1],//likes_count-user_likes,
				news_likes[0],//user_likes,
				othersComments);
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
				int countedVotes = countVotes(permlink,user_id,since);//assume comments go to author of hanged posts for earlier period
				if (countedVotes > 0)//if other user
					countPeriod(since,countedVotes,0);
			}
		}

	}
	
	public void getFeed(String token, Date since, Date until, StringBuilder detail) throws IOException {
		this.detail = detail;

		/*
		curl --data '{"jsonrpc": "2.0", "method": "call", "params": [0,"get_content",["matrixdweller","trying-to-access-the-steem-blockchain-from-unity-part-1-fail"]], "id": 4}' https://this.piston.rocks
		curl --data '{"jsonrpc": "2.0", "method": "call", "params": [0,"get_content",["matrixdweller","trying-to-access-the-steem-blockchain-from-unity-part-1-fail"]], "id": 4}' http://node.steem.ws:80/rpc
		curl --data '{"jsonrpc": "2.0", "id":25,"method":"get_account_history","params":["akolonin","2","1"]}' http://node.steem.ws:80/rpc
		[<account>,<from>,<count>]
		 */
		int from_pos = -1;
		int block_size = BLOCK_SIZE;
		boolean days_over = false;
		while (!days_over){
			int min_sequence = -1;
			//TODO: id = 25 change to use id
			String par = "steemit".equals(api.getName()) ? 
					"{\"jsonrpc\":\"2.0\",\"id\":\"25\",\"method\":\"get_account_history\",\"params\": [\""
					+ user_id + "\",\""+String.valueOf(from_pos)+"\",\""+String.valueOf(block_size)+"\"]}"
					://"golos"
					"{\"jsonrpc\":\"2.0\",\"id\":\"25\",\"method\":\"call\",\"params\": [\"account_history\",\"get_account_history\",[\""
					+ user_id + "\",\""+String.valueOf(from_pos)+"\",\""+String.valueOf(block_size)+"\"]]}";
			
			String response = null;
			try {
				response = Steemit.retryPost(body,api.getUrl(),par);
				JsonReader jr = Json.createReader(new StringReader(response));
				JsonObject result = jr.readObject();
				JsonArray items = result.getJsonArray("result");
				if (items == null || items.size() < 1)
					break;
				for (int i = items.size() - 1; i >= 0; i--){//from last to previous
					JsonArray item = items.getJsonArray(i); 
					if (item == null || item.size() != 2)
						break;
					int sequence = item.getInt(0);
					if (min_sequence == -1 || min_sequence > sequence)
						min_sequence = sequence;
					JsonObject data = item.getJsonObject(1);
					if (data == null)
						break;
					String timestamp = data.getString("timestamp");
					Date date_day = Time.time(timestamp,TIME_FORMAT);
					if (date_day.compareTo(since) < 0){
						days_over = true;
						break;
					}
					JsonArray op = data.getJsonArray("op");
					if (op == null || op.size() != 2)
						break;
					String type = op.getString(0);
					if ("comment".equals(type)){
						JsonObject post = op.getJsonObject(1);
						String parent_author = HTTP.getJsonString(post,"parent_author");
						String parent_permalink = HTTP.getJsonString(post,"parent_permlink");
						String author = HTTP.getJsonString(post,"author");
						String permalink = HTTP.getJsonString(post,"permlink");
						String title = HTTP.getJsonString(post,"title");
						String body = HTTP.getJsonString(post,"body");
						
						String json_metadata = HTTP.getJsonString(post,"json_metadata");
//TODO: process tags, users, image, links, etc. 
						//"{"tags":["meme"],
						//"image":["https://img.youtube.com/vi/ji836ZIzFE0/0.jpg"],
						//"links":["https://steemit.com/ai/@akolonin/how-to-get-your-personal-analytics-for-steemit-social-network-with-help-of-aigents-bot","https://www.youtube.com/watch?v=ji836ZIzFE0"],
						//"app":"steemit/0.1",
						//"users":["aigents","steemit"]}"
						
//TODO: process it in postprocessing phase
						//so far, process metadata for tag filtering only
						if (!AL.empty(areas) && !AL.empty(json_metadata)){
							JsonObject md = Json.createReader(new StringReader(json_metadata)).readObject();
							if (md.containsKey("tags")){
								JsonArray tags = md.getJsonArray("tags");
								if (tags != null)
									for (int j = 0; j < tags.size(); j++)
										if (Array.contains(areas, tags.getString(j).toLowerCase())){
											permlinksTagged.add(permalink);
											break;
										}
							}
						}
						if (!AL.empty(body))
							processPost(date_day,permalink,author,parent_permalink,parent_author,title,body);
					} else
					if ("vote".equals(type)){
						JsonObject post = op.getJsonObject(1);
						String permlink = HTTP.getJsonString(post,"permlink");
						String voter = HTTP.getJsonString(post,"voter");
						String author = HTTP.getJsonString(post,"author");
						processVote(permlink,voter,author);
					}
//TODO:reposts
					//count reposts + analyse reposts in scope of author's feed?
/*
        "op": [
          "custom_json",
          {
            "required_auths": [
              
            ],
            "required_posting_auths": [
              "akolonin"
            ],
            "id": "follow",
            "json": "[\"reblog\",{\"account\":\"akolonin\",\"author\":\"aigents\",\"permlink\":\"personal-social-graph-analysis-for-steemit-and-golos\"}]"
          }
        ]
 */
				}
				
			} catch (Exception e) {
				body.error("Spidering peer Steemit feeder user "+user_id+" request "+par+" response"+response,e);
				break;
			}
		
			//TODO:get from:<from_pos>,count:<block_size>-1
			//process
			//get min_sequence
			if (min_sequence <= 1)
				break;
			from_pos = min_sequence - 1;
			block_size = Math.min(block_size,from_pos);
		}//loop through blocks of sequences
		
		postProcessPosts();
	}

}
