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
import java.util.Iterator;
import java.util.regex.Pattern;

import net.webstructor.agent.Body;
import net.webstructor.agent.Merger;
import net.webstructor.agent.Schema;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.main.Mainer;
import net.webstructor.util.Array;

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
		Integer id = (Integer)byId.get(owner);
		StringBuilder b = new StringBuilder();
		b.append('#').append(id.intValue()).append(' ');//subject - thing
		Writer.toString(b,(String)name);//predicate - string
		b.append(' ');
		if (value instanceof String)
			Writer.toString(b,value,name);//object - string
		else
		if (value instanceof Date)
			b.append(Time.day((Date)value,false));
		else
		if (value instanceof Thing) 
			try {
				b.append('#').append(((Integer)byId.get(value)).intValue());//object - thing
			} catch (Exception e) {
				body.error("Write fails: ["+owner+"] "+name+" "+value, e);
				return;
			}
		b.append(".\n");
		writer.write(b.toString());
	}
	
	private void writeProperties(String name) throws Exception {
		Object[] values = storager.getObjects(name);
		if (!AL.empty(values)) {
			for (int i=0; i<values.length; i++) {
				Collection owners = storager.get(name,values[i]);
				for (Iterator it=owners.iterator(); it.hasNext();) {
					write((Thing)it.next(),name,values[i]);
				}
			}			
		}
	}
	
	public void write(String path) throws Exception {
        File temp = body.getFile(path.substring(0,path.lastIndexOf('.'))+".tmp");
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
        body.getFile(path).delete();
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
		String line;
		if (reader == null)
			return false;
		storager.clear(Schema.roots,null);
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
		reader.close();
		new Merger(body,storager).merge(byId.values());
    	return true;
	}
	
	public Streamer(Body body) {
		this.body = body;
		this.storager = body.storager;
	}
		
	
}
