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

import java.math.BigDecimal;

import net.webstructor.al.AL;

//https://docs.google.com/document/d/1yFhgBjQgMAwfX73UIILu8RAfmgzpwhaxwjpiB4AC_RY/
public class InteractionItem {
	public String network;
	public long timestamp;
	public String type;
	public String from;// (account)
	public String to;// (account)
	public Number value;
	public String unit;
	public String child;//id
	public String parent;//id
	public String title;
	public String input;
	public String tags;
	public String format;
	public long block;
	//public Number parent_value;
	//public String parent_unit;
	
	public InteractionItem(String tabbedline) {
		if (AL.empty(tabbedline))
			return;
		String[] tokens = tabbedline.split("\t");
		if (AL.empty(tokens) || tokens.length < 14)
			return;
		network = tokens[0];
		timestamp = Long.parseLong(tokens[1]) * 1000;//unix to milliseconds
		type = tokens[2];
		from = tokens[3];
		to = tokens[4];
		value = new BigDecimal(tokens[5]);
		unit = tokens[6];
		child = tokens[7];
		parent = tokens[8];
		title = tokens[9];
		input = tokens[10];
		tags = tokens[11];
		format = tokens[12];
		block = Long.parseLong(tokens[13]);
	}
}

