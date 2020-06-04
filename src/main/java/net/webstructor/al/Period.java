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
package net.webstructor.al;

import java.util.Date;

import net.webstructor.main.Tester;

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
		return period = parseUnits(string, unit);
	}
	
	public static long parseUnits(String string, long unit) {
		if (AL.empty(string))
			return 1 * unit;
		Seq chunks = Parser.parse(string);
		long temp = 0;
		String chunk;
		for (int i = 0; i<chunks.size(); i++) {
			chunk = (String)chunks.get(i);
			double value;
			try {//first, assume it is number separated from unit like "1 h" or "1 hour"
				value = Double.parseDouble(chunk);
				if (++i < chunks.size() && !AL.empty((chunk = (String)chunks.get(i))))
					value *= unit(chunk.charAt(0), unit);
				else
					value *= unit > 0 ? unit : HOUR;
			} catch (Exception e) {//if can't parse, assume it is number glued up with unit like "1h" or "1hour"
				value = parseUnit(chunk, unit);
			}
			temp += Math.round(value); 
		}
		return temp;
	}
	
	public static long unit(char c, long unit) {
		c = Character.toLowerCase(c);
		return c=='s'? SECOND : c=='m'? MINUTE : c=='h'? HOUR : c=='d'? DAY : c=='w'? WEEK : unit > 0 ? unit : HOUR; //default to HOUR
	}
	
	public static long parseUnit(String string, long unit) {
		if (AL.empty(string))
			return 1 * unit;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (!(Character.isDigit(c) || c == '.')){
				unit = unit(c, unit);
				string = string.substring(0,i);
			}
		}
		double value = 1;
		try { value = Double.parseDouble(string);} catch (Exception e) {}//default to 1
		return Math.round(value * unit); 
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
		Tester t = new Tester();
		t.assume(parseUnits(null,0),0);
		t.assume(parseUnits("",0),0);
		t.assume(parseUnits("",SECOND),1000);
		t.assume(parseUnits("",HOUR),3600000);
		t.assume(parseUnits("1",0),3600000);//??
		t.assume(parseUnits("1",SECOND),1000);
		t.assume(parseUnits("1s",0),1000);
		t.assume(parseUnits("1s",HOUR),1000);
		t.assume(parseUnits("1 s",0),1000);
		t.assume(parseUnits("1 s",HOUR),1000);
		t.assume(parseUnits("1m 1 s",0),61000);
		t.assume(parseUnits("1 m 1 s",HOUR),61000);
		t.assume(parseUnits("2 m 2s",HOUR),122000);
		t.assume(parseUnits("3m 3s",HOUR),183000);
		t.assume(parseUnits("30m",HOUR),1800000);
		t.assume(parseUnits(".5h",HOUR),1800000);
		t.assume(parseUnits(".5 h",HOUR),1800000);
		t.check();
	}
}

