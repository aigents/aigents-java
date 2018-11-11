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
package net.webstructor.util;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.core.Environment;
import net.webstructor.data.OrderedStringSet;

public class JsonReporter extends Reporter {
	
	public JsonReporter(Environment env,Writer writer){
		super(env,writer);
	}

	//@Override
	public boolean needsId(){
		return true;
	}
	
	//@Override
	public void initReport(String title, Date since, Date until){
		try {
			writer.append("{");
			writer.append("\"title\":\"").append(net.webstructor.al.Writer.capitalize(title)).append("\",");
			if (since != null)
				writer.append("\"since\":\"").append(Time.day(since,false)).append("\",");
			if (until != null)
				writer.append("\"until\":\"").append(Time.day(until,false)).append("\",");
			writer.append("\"peers\":[\n");
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}

	//@Override
	public void initPeer(String id, String name, String surname, String perPeriod, Date since, Date until){
		try {
			writer.append("\t{");
			writer.append("\"id\":\"").append(id).append("\",");
			if (!AL.empty(name) || !AL.empty(surname)){
				if (!AL.empty(name))
					writer.append("\"name\":\"").append(name).append("\",");
				if (!AL.empty(surname))
					writer.append("\"surname\":\"").append("\",");
			}
			if (!AL.empty(perPeriod))
				writer.append("\"publications\":\"").append(perPeriod).append("\",");
			writer.append("\"sections\":[\n");
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}

	public static boolean isPairOfStringIntegerArray(Object obj){
		if (obj instanceof Object[] && ((Object[])obj).length == 2){
			Object[] pair = (Object[])obj;
			if (isStringIntegerArray(pair[0]) && isStringIntegerArray(pair[1]))
				return true;
		}
		return false;
	}

	//TODO: make it configurable to display second row or not?
	public static String toPairOfStringIntegerArray(Object obj){
		Object[] pair = (Object[])obj;
		return toStringIntegerArray(pair[0]);
	}
	
	public static boolean isStringIntegerArray(Object obj){
		if (obj instanceof Object[][] && ((Object[][])obj).length > 0){
			Object[] first = (Object[])((Object[][])obj)[0];
			if (first[0] instanceof String && first[1] instanceof Integer)
				return true;
		}
		return false;
	}
	
	public static String toStringIntegerArray(Object obj){
		Object[] objs = (Object[])obj;
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < objs.length; i++){
			Object[] item = (Object[])objs[i];
			if (i > 0)
				sb.append(',');
			sb.append('{');
			sb.append("\"word\":\"").append(item[0]).append("\",");
			sb.append("\"rank\":").append(((Integer)item[1]).toString());
			sb.append('}');
		}
		sb.append("]");
		return sb.toString();
	}
	
	protected String toString(Boolean value){
		return value.booleanValue() ? "1" : "0";
	}
	
	public String toString(OrderedStringSet obj){
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < obj.size(); i++){
			if (i > 0)
				sb.append(',');
			sb.append('\"').append(obj.get(i)).append('\"');
		}
		sb.append(']');
		return sb.toString();
	}
	
	public String toString(Object obj){
		if (obj instanceof Boolean)
			return toString((Boolean)obj);
		if (obj instanceof Date){
			//TODO: with Time class
			DateFormat day_format = new SimpleDateFormat("yyyy-MM-dd");
			return "\""+day_format.format((Date)obj)+"\"";
		}
		if (obj instanceof String){
			String s = trimHTML((String)obj,maxLength);
			s = s.replace("\n"," ");
			return "\""+s.replace("\"", "\\\"")+"\"";
		}
		if (obj instanceof OrderedStringSet)
			return toString((OrderedStringSet)obj);
		if (isPairOfStringIntegerArray(obj))
			return toPairOfStringIntegerArray(obj);
		if (isStringIntegerArray(obj))
			return toStringIntegerArray(obj);
		if (obj instanceof Object[]){
//TODO: hyperlinks
			Object[] objs = (Object[])obj;
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (int i = 0; i < objs.length; i++){
				if (i > 0)
					sb.append(',');
				sb.append( toString(objs[i]) );
			}
			sb.append(']');
			return sb.toString();
		}
		return obj.toString();
	}
	
	public void table(String id, String title,String[] header, Object[][] rows, int minPercent, int minCount){
		if (AL.empty(rows))
			return;

		//do we have anythign to render at all?
		int visible = 0;
		for (int i = 0; i < rows.length; i++){
			Object[] row = rows[i];
			if (row == null)
				return;
			int columns = Math.min(header.length, row.length);
			boolean minPercentExceeded = false;
			for (int j = 0; j < columns; j++){
				boolean percent = header[j].indexOf('%') != -1 && row[j] instanceof Integer;
				if (percent){
					int percentValue = ((Integer)row[j]).intValue();
					if (minPercent > percentValue && (percentValue == 0 || visible > minCount))
						minPercentExceeded = true;
					break;
				}
			}
			if (minPercentExceeded)
				break;
			visible++;
		}
		if (visible == 0)
			return;
		
		try {
			writer.append("\t\t{");
			writer.append("\"id\":\"").append(id).append("\",");
			writer.append("\"subtitle\":\"").append(net.webstructor.al.Writer.capitalize(title)).append("\",");
			writer.append("\"headings\":[");
			for (int i = 0; i < header.length; i++){
				if (i != 0)
					writer.append(',');
				writer.append('\"').append(header[i]).append('\"');
			}
			writer.append("],");
			writer.append("\"data\":[\n");
			for (int i = 0; i < visible; i++){
				Object[] row = rows[i];
				if (row == null){
					break;
				}
				writer.append("\t\t\t[");
				int columns = Math.min(header.length, row.length);
				for (int j = 0; j < columns; j++){
					if (j != 0)
						writer.append(",");
					writer.append(toString(row[j]));
				}
				writer.append(']');
				if (i + 1 < visible)
					writer.append(',');
				writer.append("\n");
			}
			writer.append("\t\t]},\n");
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}
	
	//@Override
	public void closePeer(){
		try {
			writer.append("\t{}]},\n");//dummy compensation for last comma
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}
	
	//@Override
	public void closeReport(){
		try {
			writer.append("{}]}");
			writer.close();
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}
}
