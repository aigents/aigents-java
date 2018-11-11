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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.core.Storager;
import net.webstructor.util.Array;

public class AL {
	public static final int declaration = 0, direction = 1, confirmation = 2, interrogation = 3;

	//values
	public final static String _true 	= "true";
	public final static String _false 	= "false";
	
	public final static String id 		= "id";
	public final static String is 		= "is";
	public final static String has 		= "has";
	public final static String name 	= "name";
	public final static String does 	= "does";
	public final static String times 	= "times";
	public final static String trust 	= "trust";
	public final static String trusts 	= "trusts";
	public final static String ignore 	= "ignore";
	public final static String ignores 	= "ignores";
	public final static String share 	= "share";
	public final static String shares 	= "shares";
	public final static String friend 	= "friend";
	public final static String friends 	= "friends";
	public final static String _new 	= "new";
	public final static String news 	= "news";
	public final static String things 	= "things";
	public final static String knows 	= "knows";
	public final static String sites 	= "sites";
	public final static String areas 	= "areas";
	public final static String sources 	= "sources";
	public final static String patterns = "patterns";
	public final static String image 	= "image";
	public final static String path 	= "path";
	public final static String text 	= "text"; // TODO: move this out to name?
	public final static String version  = "version";

	public final static String[] foundation = new String[]{
		id,name,is,has,does,times,trust,trusts,ignores,share,shares,friend,friends,_new,news,things,knows,sites,areas,sources,patterns,text
	};

	//things
	public final static String number 	= "number";//integer or floating point
	public final static String currency = "currency";
	public final static String money 	= "money";//currency+number
	//public final static String value 	= "value";// not used?
	public final static String date 	= "date";//year,monh,day
	public final static String daytime 	= "daytime";//hour,minute,second
	public final static String time 	= "time";//date and daytime
	public final static String word 	= "word";//single token
	public final static String email 	= "email";//email address
	
	//synsets
	public final static String[] what 	= new String[] {"what"};
	public final static String[] iff  	= new String[] {"if"};
	public final static String[] doo  	= new String[] {"do"};
	//TODO: decide what to do with true/false if appears as property value and breaks parsing as negation
	//public final static String[] not  	= new String[] {"not","no","false","~"};
	public final static String[] not  	= new String[] {"not","no","~"};
	public final static String[] yes  	= new String[] {"ok","yes","true"};
	public final static String[] i_my 	= new String[] {"my","i","we","our","me"};	
	public final static String[] you  	= new String[] {"your","you"};
	public final static String[] there	= new String[] {"there","it","here","this","that","a","the"};
	
	public final static String[] lister = new String[] {",",";","and","or"};//TODO: have either AL.lister or AL.commas!
	
	public final static String[][] grammar = new String[][]{what,iff,doo,not,yes,i_my,you,there,lister};
	
	public static final String period = ".";
	public static final String space = " ";
	public static final String spaces = space+"\t\r\n";
	public static final String brackets_open = "{([";
	public static final String brackets_close = "})]";
	public static final String brackets = brackets_open + brackets_close;
	public static final String commas = ",;";//TODO: union with 'separators' below
	public static final String periods = ".!?";
	public static final String negation = "~";
	public static final String punctuation = brackets + commas + periods + negation;
	public static final String separators = ".,:";//like decimal separators inside a word or colon in time
	public static final String dashes = "-—";//word separators allowed only inside a word
	public static final String quotes = "\'\"";
	public static final String trimmers = separators + dashes;
		
	public static final String[] https = {"http://", "https://"};
			
	protected Body body;
	//private int recursion = 0;//TODO: need to consider current recursion level?
	
	public AL(Body body) {
		this.body = body;
	}

	public static boolean empty(String string) {
		return string == null || string.isEmpty();//string.length() == 0;
	}

	public static boolean empty(StringBuilder sb) {
		return sb == null || sb.length() == 0;
	}

	public static boolean empty(Object[] objects) {
		return objects == null || objects.length == 0;
	}
	
	public static boolean empty(Collection collection) {
		return collection == null || collection.isEmpty();
	}
	
	public static boolean empty(Set set) {
		return set == null || set.size() == 0;
	}

	public static boolean empty(Map set) {
		return set == null || set.isEmpty();
	}

	//TODO: move out somewhere!?
	//TODO: optimization to avoid extra lowercasing!
    public static boolean isURL(String str) {
    	return Array.prefix(https,str.toLowerCase()) != null && str.indexOf(' ') == -1;
    }

	public static boolean isVariable(String term) {
		return !AL.empty(term) && term.charAt(0) == '$';
	}
	    
	public static int integer(String s, int def) {
    	try {
    		return Integer.valueOf(s).intValue();
    	} catch (Exception e) {
    		return def;
    	}
	}
	
	public static long integer(String s, long def) {
    	try {
    		return Long.valueOf(s).intValue();
    	} catch (Exception e) {
    		return def;
    	}
	}

	public Storager getStorager() {
		return body.storager;
	}
	
	public static Object[][] parseToGrid(String input, String[] columns, String listSeparator) {
		ArrayList rows = new ArrayList();
		Parser parser = new Parser(input);
		Object[] o = new Object[columns.length];
		while (!parser.end()) {
			String name = parser.parseAny(columns,true);
			if (name == null)
				break;
			String delimiter = null;			
			StringBuilder sb = new StringBuilder();
			while (!parser.end()) {
				String token = parser.parse();
				if (AL.punctuation.indexOf(token) != -1) {
					delimiter = token;
					break;
				}
				if (sb.length() > 0)
					sb.append(' ');
				sb.append(token);
			}
			int column = Array.index(columns,name);
			if (column < 0 || column >= o.length)
				;//throw new Exception("ddd");//TODO:throw exception
			else
			if (name.equals(AL.trust) || name.equals(AL.share) || name.equals(AL.friend))//TODO: fair Schema-unary properties!
				o[column] = Boolean.valueOf(sb.toString());
			else
				o[column] = sb.toString();
			if (delimiter == null || !delimiter.equals(listSeparator)) {
				rows.add(o);			
				o = new Object[columns.length];
			}
		}
		return (Object[][])rows.toArray(new Object[][]{});
	}
	
	//TODO:optimize and move all AL-parsing into separate "linguistic" framework
	public static Object[][] parseToSheet(String input, String[] names, String listSeparator) {
		String[] sorted = Array.sortedByLength(names, false);
		HashMap props = new HashMap();
		Parser parser = new Parser(input);
		while (!parser.end()) {
			String n = parser.parseAny(sorted,true);
			if (n == null) {
				parser.parse();
				continue;//skip for tolerance?
				//break;
			}
			String delimiter = null;			
			StringBuilder sb = new StringBuilder();
			while (!parser.end()) {
				String token = parser.parse();
				if (AL.punctuation.indexOf(token) != -1) {
					delimiter = token;
					break;
				}
				if (sb.length() > 0)
					sb.append(' ');
				sb.append(token);
			}			
			props.put(n, sb.toString());
			if (delimiter == null || !delimiter.equals(listSeparator))
				break; //ignore objects other than first one? 
		}
		Object[][] o = new Object[names.length][2];
		for (int i = 0; i < names.length; i++) {
			String str = (String)props.get(names[i]);
			o[i][0] = names[i];
			o[i][1] = str == null ? "" : str;			
		}
		return o;
	}
	
	public static String[] parseToFields(String input) {
		ArrayList list = new ArrayList();
    	Parser p = new Parser(input);
    	while (!p.end()) {
    		StringBuilder name = new StringBuilder();
    		String s = null;
        	while (!p.end()) { // glueing multi-word names from multiple tokens
        		s = p.parse();
        		if (AL.empty(s) || AL.punctuation.indexOf(s) != -1) // break field formation on any delimiter
        			break;
        		if (name.length() > 0)
        			name.append(' ');
    			name.append(s);
        	}
        	if (name.length() > 0)
        		list.add(name.toString());
        	if (AL.empty(s) || AL.periods.indexOf(s) !=-1) //break list formation on period
        		break;
    	}
    	return AL.empty(list) ? null : (String[]) list.toArray(new String[]{});		
	}
	
	public static String propList(String[] names) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < names.length; i++) {
			if (i > 0)
				b.append(", ");
			String val = names[i];
			b.append(Writer.needsQuoting(val) ? Writer.quote(val) : val);
		}
		return b.toString();
	}

	private static void buildQualifier(StringBuilder b, String name, Object val) {
		if (val != null) {
			if (b.length() > 0)
				b.append(", ");//TODO: consider " and " ?
			Writer.toString(b,name);
			b.append(' ');
			if (val instanceof String)
				Writer.toString(b,(String)val,Schema.quotable(name));
			else
				Writer.toString(b,val);
		}
	}
	
	public static String buildQualifier(String names[], Object[] values, int fromPos) {
		StringBuilder b = new StringBuilder();
		for (int i = fromPos; i < names.length; i++)
			if (i < values.length && values[i] != null && values[i] instanceof String) {
				buildQualifier(b,names[i],values[i]);
			}
		return b.toString();
	}

	public static String buildQualifier(String names[], Object[] values) {
		return buildQualifier(names,values,0);
	}
	
	public static String buildQualifier(Map map) {
		StringBuilder b = new StringBuilder();	
		for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			String name = (String) it.next();
			buildQualifier(b,name,map.get(name));
		}
		return b.toString();
	}
	
	public static String buildQualifier(String names[], Map map) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			buildQualifier(b,name,map.get(name));
		}
		return b.toString();
	}
	
	public static String error(String message) {
		if (message.startsWith("Error") || message.startsWith("No"))
			return message;
		//TODO: get rid of java.lang.Exception on error - on backend!!!
		if (message.startsWith("Java.lang.Exception: "))
			return Writer.capitalize(message.substring("Java.lang.Exception: ".length()));
		return null;
	}
}
