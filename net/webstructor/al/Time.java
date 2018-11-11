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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public abstract class Time extends Date	{
	private static final long serialVersionUID = 4559747090692507251L;
	public static final String today = "today";
	public static final String yesterday = "yesterday";
	public static final String tomorrow = "tomorrow";
	public static final String times[] = {yesterday,today,tomorrow};
	
	private static final Pattern time24 = Pattern.compile("([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?");
	private static final Pattern time12 = Pattern.compile("(1[012]|[1-9]):[0-5][0-9](\\s)?(?i)(am|pm)");

	public static final String MONTH_FORMAT = "yyyy-MM"; 
	public static final String DAY_FORMAT = "yyyy-MM-dd"; 
	private static DateFormat month_format = null;	
	private static DateFormat day_format = null;	
	public static DateFormat month_format() {
		if (month_format == null)
			month_format = new SimpleDateFormat(MONTH_FORMAT);
		return month_format;
	}
	public static DateFormat day_format() {
		if (day_format == null)
			day_format = new SimpleDateFormat(DAY_FORMAT);
		return day_format;
	}
	
    public static Date date(Date date) {
        return date(date, 0);
    }
    
    public static Long linux(Date date){
    	return new Long(date.getTime()/1000);   	
    }
    
    public static Date date(Date date, int delta) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
		if (delta != 0)
			cal.add(Calendar.DAY_OF_MONTH, delta);
        return cal.getTime();
    }
    
	public static Date today(int delta) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (delta != 0)
			cal.add(Calendar.DAY_OF_MONTH, delta);
		return cal.getTime();
	}

	public static Date day(Object obj) {
		return obj instanceof Date ? (Date)obj : obj instanceof String ? day((String)obj) : null;
	}

	public static Date time(String text, String format) {
		DateFormat df = new SimpleDateFormat(format);
		try {
			if (text != null)
				return df.parse(text);
		} catch (ParseException e) {
		}
		return today(0);
	}
	
	public static Date day(String text) {
		Date d;
		if (AL.empty(text)) {
			d = new Date(0);
		} else
		if (text.equals(today)) {
			d = today(0);
		} else
		if (text.equals(yesterday)) {
			d = today(-1);
		} else
		if (text.equals(tomorrow)) {
			d = today(+1);
		}
		else
		try {
			//TODO: this throws dumb exception: simpledateformat, so put this ahead separately?
			d = day_format().parse(text);
		} catch (ParseException e) {
			d = today(0);
		}
		return d;
	}

	public static boolean isDayTime(String text) {
		return time24.matcher(text).matches() || time12.matcher(text).matches();
	}

	/*
	public static boolean isDaytime(String text) {
		return false;
	}

	public static boolean isDate(String text) {
		return false;
	}
	*/

	public static String month(Date time) {
		return month_format().format(time);
	}
	
	public static String day(Date time, boolean fancy) {
		return
			!fancy ? day_format().format(time) :
			time.equals(today(0))? today :
			time.equals(today(-1))? yesterday :
			time.equals(today(+1))? tomorrow :
				day_format().format(time);
	}
	
	public static int compare(String a, String b) {
		if (a.equals(b))
			return 0; 
		if (a.equals(Time.today))
			return -1;
		if (b.equals(Time.today))
			return 1;
		if (a.equals(Time.yesterday))
			return -1;
		if (b.equals(Time.yesterday))
			return 1;
		int time =  b.compareTo(a); // so latest at the very bottom
		return time;
	}
}
