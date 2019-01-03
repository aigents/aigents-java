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

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.core.Environment;
import net.webstructor.data.Graph;
import net.webstructor.data.GraphCacher;
import net.webstructor.data.LangPack;

class SteemitGraphFeeder extends SteemitFeeder {
	private GraphCacher gc;
	private Graph graph;
	private Date time = null;
	
	public SteemitGraphFeeder(GraphCacher gc,Environment body, Steemit api, String user_id,
			LangPack langPack, Date since, Date until, String[] areas,
			int period) {
		super(body, api, user_id, langPack, since, until, areas, period);
		this.gc = gc;
	}
	
	protected Graph getGraph(Date time){
		Date date = Time.date(time);
		//if (this.time != null && !this.time.equals(date))//graph lazy saving on day change
			//gc.updateGraph(this.time, graph, System.currentTimeMillis());
			//graph.setAge(date.getTime());//set age to save
		if (this.time == null || !this.time.equals(date))//graph lazy load on day change
			graph = gc.getGraph(date);
		this.time = date;
		return graph;
	}

	//TODO:@Override
	protected int countVotes(String permlink,String author,Date day){
		HashSet voters = (HashSet)permlinksToVotes.get(permlink);
		if (AL.empty(voters))
			return 0;
		
//TODO: avoid double-counting
		getGraph(day);
		if (graph.getLinkers(user_id,false) != null)//don't count already counted
			return 0;
		
		for (Iterator it = voters.iterator(); it.hasNext();){
			String voter = (String)it.next();
			graph.addValue(voter, author, "rates", 1);//voter rates author
			graph.addValue(author, voter, "rated", 1);//author is rated by author
			graph.setAge(0);//make dirty
		}
		return super.countVotes(permlink, author, day);//to count peers
	}
	
	//TODO:@Override
	protected int countComments(String id,String name,String message,Date time,int amount){
		if (user_id.equals(id))
			return 0;
		
//TODO: avoid double-counting
		getGraph(time);
		if (graph.getLinkers(user_id,false) != null)//don't count already counted
			return 0;
		
		graph.addValue(id, user_id, "comments", 1);//commenter comments user
		graph.addValue(user_id, id, "commented", 1);//user is commented by commenter
		graph.setAge(0);//make dirty
		
		getUser(id,name);//to count peers
		return 1;
	}
}
