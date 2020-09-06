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
package net.webstructor.self;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import net.webstructor.agent.Body;
import net.webstructor.agent.Merger;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Filer;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.main.Mainer;
import net.webstructor.util.Array;
import net.webstructor.util.Str;

public class Streamer {

	protected static final char fieldDelim = ';', elementDelim = ',';
	protected static final int min_id = 0; // assume 0-1000 are reserved
	
	int id = min_id;
	HashMap byId = new HashMap();
	ArrayList triples = new ArrayList();
	BufferedWriter writer = null;
	
	//TODO:get rid of one of the two
	Storager storager;
	Body body; 
	
	private void write(Thing owner, String name, Object value) throws Exception {
		StringBuilder b = new StringBuilder();
		try {
			Integer id = (Integer)byId.get(owner);
			b.append('#').append(id.intValue()).append(' ');//subject - thing
			Writer.toString(b,(String)name);//predicate - string
			b.append(' ');
		} catch (Exception e) {
			body.error("Streamer fails write owner: ["+Str.first(owner.toString(),200)+"] "+name+" "+value+" "+b.toString(), e);
			throw(e);//TODO remove for fail-tolerance!?
		}
		try {
			if (value instanceof String || value instanceof Number)
				Writer.toString(b,value,name);//object - string
			else
			if (value instanceof Date)
				b.append(Time.day((Date)value,false));
			else
			if (value instanceof Thing) 
				b.append('#').append(((Integer)byId.get(value)).intValue());//object - thing
			b.append(".\n");
			writer.write(b.toString());
		} catch (Exception e) {
			body.error("Streamer fails write value: ["+Str.first(owner.toString(),200)+"] "+name+" "+value+" "+b.toString(), e);
			//TODO throw error or prevent happening!?
		}
	}
	
	private void writeProperties(String name) throws Exception {
		Object[] values = storager.getObjects(name);
		if (!AL.empty(values)) {
			for (int i=0; i<values.length; i++) {
				Object value = values[i];
				Collection owners = storager.get(name,values[i]);
				if (AL.empty(owners)) {
					body.error("Streamer fails write no owner "+name+" as "+value, null);
//TODO throw error or prevent happening!?
				} else
				for (Iterator it=owners.iterator(); it.hasNext();) {
					Thing owner = (Thing)it.next();
					Object v = owner.get(name);
					if (!((v instanceof Set && ((Set)v).contains(value)) || (!(v instanceof Set) && v.equals(value)))) {
						body.error("Streamer fails write "+name+" as "+value+" in ["+Str.first(owner.toString(),200)+"]", null);
//TODO throw error or prevent happening!?
					}
					write(owner,name,value);
				}
			}			
		}
	}
	
	public void write(String path) throws Exception {
        File temp = body.getFile(Filer.ext(path, "tmp"));
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF-8"));

        //Make id's for all Things
		Collection things = storager.getThings();
		for (Iterator it = things.iterator(); it.hasNext();) {
			Object o = it.next();
			byId.put(o,new Integer(id++));
		}

		String[] names = storager.getNames();
		for (int i=0; i<AL.foundation.length; i++) {
			writeProperties(AL.foundation[i]);
		}
		for (int i=0; i<names.length; i++) {
			if (!Array.contains(AL.foundation, names[i]))
				writeProperties(names[i]);
		}
        
		writer.close();
		try {body.getFile(path).renameTo(body.getFile(Filer.ext(path, "bak")));}catch(Exception e){}
        temp.renameTo(body.getFile(path));            		
	}

	
	//http://stackoverflow.com/questions/4702730/regex-for-validating-an-integer-with-a-maximum-length-of-10-characters	static final Pattern idPattern = Pattern.compile("\");
	static final Pattern idPattern = Pattern.compile("[0-9]+");

	//TODO: performance optimization
	//TODO: merge with plain AL reader/query?
	public boolean read(String path) throws Exception {
		path = body.getFile(path).getPath();
		BufferedReader reader = Mainer.getReader(path);
		int line_no = 0;
		String line = "";
		try {
		if (reader == null)
			return false;
		//TODO: pass peers to clear so the sessions are preserved?
		storager.clear(Schema.roots,null,null);
		String[] names = storager.getNames();
		Array.sortByLength(names,false);
		boolean clearnames = true;
		while ((line = reader.readLine()) != null) {
			line_no++;
			if (!AL.empty(line)) {
				Parser parser = new Parser(line);
				String first = parser.parse();
				if (first.charAt(0) != '#') {
					body.output("No id at line "+line_no+" in \'"+line+"\'");
					continue;
					//TODO: handle errors better?
				}
				Integer id = new Integer(first.substring(1));
				//TODO: avoid doing it on each line keeping up-to-date
				//String[] names = storager.getNames();
				if (!clearnames) {
					names = storager.getNames();
					Array.sortByLength(names,false);
					clearnames = true;
				}
				String property = null;
				try {
					property = parser.parseAny(names,true);
				} catch (Exception e) {
					throw new Exception(" parse error in \'"+line+"\'");
				}
				if (AL.empty(property))
					property = parser.parse();//undeclared property w/o has-predefintion
				if (AL.empty(property))
					throw new Exception(" no property in \'"+line+"\'");
				if (property.equals(AL.has)) {
					clearnames = false;
				}
				StringBuilder value = new StringBuilder();
				String chunk;
				while ((chunk = parser.parse()) != null && AL.periods.indexOf(chunk) == -1) {
					if (value.length() > 0)
						value.append(' ');
					value.append(chunk);
				}
				//TODO: decide if blank values are allowed (like store path '')?
				//if (value.length() == 0)
				//	throw new Exception(" no value in \'"+line+"\'");
				Thing thing = (Thing)byId.get(id);
				if (thing == null)
					byId.put(id,thing = new Thing());
				Thing vthing = null; 
				if (value.length() > 0 && value.charAt(0) == '#') {
					String idString = value.substring(1);
					if (idPattern.matcher(idString).matches()) {
						Integer vid = new Integer(idString);
						vthing = (Thing)byId.get(vid);
						if (vthing == null)
							byId.put(vid,vthing = new Thing()); 
					}
				}
				if (vthing != null)
					thing.addThing(property,vthing);
				else 
				if (storager.isTime(property))
					thing.set(property,Time.day(value.toString()));
				else
					thing.setString(property,value.toString());
				thing.store(storager);
			}
  		}
		} catch (Exception e){
			body.error("Streamer fails read line "+line_no+" "+line, e);
		}
		reader.close();
		new Merger(body,storager).merge(byId.values());
    	return true;
	}
	
	public int loadCSV(String path, Thing as, Thing peer) throws Exception {
		BufferedReader reader = Mainer.getReader(path);
		String headLine = reader.readLine();
		int cnt = 0;
		if (!AL.empty(headLine)) {
			java.util.Set<String> names = as.getNamesPossible(new HashSet<String>());
			TreeMap<Integer,String> header = new TreeMap<Integer,String>();
			StringTokenizer st = new StringTokenizer(headLine,",");
			for (int col = 0; st.hasMoreElements(); col++) {//compile header
				String name = st.nextToken().toLowerCase();
				if (names.contains(name))
					header.put(col, name);
			}
			if (header.size() > 0) {
				String row;
				while (!AL.empty(row = reader.readLine())) {
					Thing t = null;
					/*
					st = new StringTokenizer(row,",");
					for (int col = 0; st.hasMoreElements(); col++) {
						String value = st.nextToken();
						*/
					List<String> cols = parseCSV(row,',','\"');
					for (int col = 0 ; col < cols.size(); col++) {
						String value = cols.get(col);
						String name = header.get(col);
						if (!AL.empty(name)) {
							if (t == null)
								t = new Thing(as);
//TODO: non-string types!?
							t.setString(name, value);
						}
					}
					if (t != null) {
						t.store(storager);
						if (peer != null)
							peer.addThing(AL.trusts, t);
						cnt++;
					}
				}
			}
		}
		return cnt;
	}
	
	//Credits https://mkyong.com/java/how-to-read-and-parse-csv-file-in-java/
    public static List<String> parseCSV(String cvsLine, char separator, char quote) {
        List<String> result = new ArrayList<String>();
        if (cvsLine == null || cvsLine.isEmpty())
            return result;
        StringBuffer curVal = new StringBuffer();
        boolean inQuotes = false;
        //boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;

        char[] chars = cvsLine.toCharArray();

        for (char ch : chars) {

            if (inQuotes) {
                //startCollectChar = true;
                if (ch == quote) {
                    inQuotes = false;
                    doubleQuotesInColumn = false;
                } else {

                    //Fixed : allow "" in custom quote enclosed
                    if (ch == '\"') {
                        if (!doubleQuotesInColumn) {
                            curVal.append(ch);
                            doubleQuotesInColumn = true;
                        }
                    } else {
                        curVal.append(ch);
                    }

                }
            } else {
                if (ch == quote) {

                    inQuotes = true;
//TODO: this reads quoted texts incorrectly
/*
                    //Fixed : allow "" in empty quote enclosed
                    if (chars[0] != '"' && quote == '\"') {
                        curVal.append('"');
                    }

                    //double quotes in column will hit this!
                    if (startCollectChar) {
                        curVal.append('"');
                    }
*/
                } else if (ch == separator) {

                    result.add(curVal.toString());

                    curVal = new StringBuffer();
                    //startCollectChar = false;

                } else if (ch == '\r') {
                    //ignore LF characters
                    continue;
                } else if (ch == '\n') {
                    //the end, break!
                    break;
                } else {
                    curVal.append(ch);
                }
            }

        }
        result.add(curVal.toString());
        return result;
    }
	
	
	public Streamer(Body body) {
		this.body = body;
		this.storager = body.storager;
	}
		
	
}
