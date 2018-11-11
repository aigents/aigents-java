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
package net.webstructor.al;

import java.util.Date;

//TODO: in other place and properly, with account to glued synomymic abreviations "1m and 1min"
//and validation
public class Period {
	public final static long SECOND = 1000;
	public final static long MINUTE = 60*SECOND;
	public final static long HOUR = 60*MINUTE;
	public final static long DAY = 24*HOUR;
	public final static long WEEK = 7*DAY;
	
	private long period = 0;//millis
	
	public Period(long millis) {
		period = millis;
	}
	
	public Period(String str,long unit) {
		period = parse(str,unit);
	}
	
	public Period(String str) {
		period = parse(str);
	}
	
	//TODO: parse 9sec into "9 sec"
	public long parse(String string) {
		return parse(string,HOUR);
	}
	
	public long parse(String string, long unit) {
		if (AL.empty(string))
			return period;//TODO: fix hack
		Seq chunks = Parser.parse(string);
		long temp = 0;
		for (int i = 0; i<chunks.size(); i++) {
			long value = 1;
			try {
				value = Long.parseLong((String)chunks.get(i));
				i++;
			} catch (Exception e) {
				value = 1;//default to 1
			}
			if (i < chunks.size() && !AL.empty((String)chunks.get(i))) {
				char c = ((String)chunks.get(i)).toLowerCase().charAt(0);
				unit = ( c=='s'? SECOND : c=='m'? MINUTE : c=='h'? HOUR : 
					c=='d'? DAY : c=='w'? WEEK : HOUR ); //default to HOUR
			}
			temp += value * unit; 
		}
		return period = temp;
	}
	
	public long getMillis() {
		return period;
	}	

	public int getDays() {
		return (int) (period / DAY);
	}	

	public String toString() {
		return Long.toString(period / SECOND)+" sec";
	}
	
	public String toMinutes() {
		long seconds = period / SECOND;
		return Long.toString((seconds / 60))+":"+Long.toString(seconds % 60)+" min";
	}	

	public String toHours() {
		return toHours(period);
	}

	public static String toHours(long period) {
		long seconds = period / SECOND;
		long hours = seconds / 3600;
		seconds %= 3600;
		long minutes = seconds / 60;
		seconds %= 60;
		return Long.toString(hours)+":"+Long.toString(minutes)+":"+Long.toString(seconds)+" hour";
	}

    public static int daysdiff(Date from, Date to) {
        return (int)Math.round( (to.getTime() - from.getTime())/DAY );
    }
    	
	public static void main(String args[]){
		System.out.println(new Period(   11000).toString());
		System.out.println(new Period(  671000).toString());
		System.out.println(new Period(40271000).toString());
		System.out.println(new Period(   11000).toMinutes());
		System.out.println(new Period(  671000).toMinutes());
		System.out.println(new Period(40271000).toMinutes());
		System.out.println(new Period(   11000).toHours());
		System.out.println(new Period(  671000).toHours());
		System.out.println(new Period(40271000).toHours());
	}
}

