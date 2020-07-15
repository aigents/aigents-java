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
package net.webstructor.peer;

import java.util.Date;

import net.webstructor.al.Time;
import net.webstructor.core.Thing;

class SearchContext {
	Thing peer = null;
	Thing arg = null;
	String site = null;
	String type = null;
	String topic = null; 
	String engine = null; 
	String[] properties = null;
	String[] graphs = null;
	String cluster = null;
	String format = null;
	Date date = null;
	int days = 0;
	int limit = 0;
	boolean sentiment = false;
	boolean novelNew = false;
	boolean scopeWeb = false;

	public SearchContext(String topic,Thing peer,String engine,int limit) {
		this.topic = topic;
		this.peer = peer;
		this.engine = engine;
		this.date = Time.today();
		this.limit = limit;
	}
};

