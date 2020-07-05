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
package net.webstructor.comm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import net.webstructor.core.Named;
import net.webstructor.core.Thing;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Profiler;
import net.webstructor.self.Siter;

public interface Crawler extends Named {
	/**
	 * Read newsfeed/personal channel like subreddit, group or personal feed
	 * @param Siter context 
	 * @param path/uri of the channel/source
	 * @param topics to be searched
	 * @param collector to accumulate findings in triple store: Thing topic, String path, Thing instance
	 * @return -1 if ot supported, 0 if supported but not read, 1 if read
	 */
	//public int crawl(Siter siter, String uri, Collection topics, Date time, MapMap collector);
	public int crawl(Siter siter);

	/**
	 * Read specific page for list of topics of interest and return links found along the way  
	 * @param Siter context 
	 * @param path/uri of the page/source
	 * @param collector to accumulate found links
	 * @param topics to be searched
	 * @return boolean if successful, false otherwise
	 */
	//TODO return int count as crawl?
	public boolean scalp(Siter siter,String path,ArrayList links,Collection topics);

	/**
	 * Return SocialFeeder caching social crawl results if extends Socializer
	 * @param id
	 * @param token
	 * @param key
	 * @param since
	 * @param until
	 * @param areas
	 * @return SocialFeeder or null if not supported
	 * @throws IOException
	 */

	public SocialFeeder getFeeder(String id, String token, String key, Date since, Date until, String[] areas) throws IOException;

	/**
	 * Create profiler to profile given peer
	 * @param peer
	 * @return Profiler or null if not supported
	 */
	public Profiler getProfiler(Thing peer);
}
