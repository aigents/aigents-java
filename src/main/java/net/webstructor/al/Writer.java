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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import net.webstructor.agent.Body;
import net.webstructor.agent.Schema;
import net.webstructor.core.Property;
import net.webstructor.core.Thing;
import net.webstructor.peer.Session;
import net.webstructor.util.Str;
import net.webstructor.cat.HtmlStripper;

public class Writer extends AL {
	
	public Writer(Body body) {
		super(body);
	}

	public static String term(Object obj) {
		if (obj instanceof String)
			return (String)obj;
		return null;
	}
	
	public static String and(Set terms) {
		StringBuilder out = new StringBuilder();
		for (int i=0, cnt=0;i<terms.size();i++) {
			String term = (String)term(terms.get(i));
			if (term != null) {
				if (cnt++ > 0)
					out.append(',').append(' ');
				//out.append(term);
				toString(out,term);
			}
		}
		return out.toString();
	}

	private static void toString(StringBuilder out,Thing thing, Object context) {
		toString(out, thing, context, null, false);
	}
	
	public static String toString(Thing thing, Object context, String[] names, boolean form) {
		StringBuilder sb = new StringBuilder();
		toString(sb,thing,null,names,true);
		return sb.toString();
	}
	
	public static void toString(StringBuilder out,Thing thing, Object context, String[] names, boolean form) {
		if (context != null) {
			//TODO: make unique reference AKA buildQualifier
			String ref = thing.getString(name);
			if (ref == null)
				ref = AL.there[0];
			if (!AL.empty(ref))
				toString(out,ref);
		} else {
			if (names == null)
				names = thing.getNamesAvailable();
			if (names.length == 0)
				out.append(AL.no[0]);
			else {
				String[] sorted = new String[names.length];
				for (int i=0, c=0;i<names.length;i++) {
					String name = (String)names[i];
					Object value = thing.get(name);
					if (value != null)
						sorted[c++] = name + (form ? ": " : space) + toString(value,name);
				}
				Arrays.sort(sorted);
				for (int i=0, c=0;i<names.length;i++) {
					if (sorted[i] != null) {
						if (c++ > 0){
							if (!form)
								out.append(',').append(space);
							else
								out.append('\n');
						}
						out.append(sorted[i]);
					}	
				}
			}
		}
	}
			
	public static String toString(Set set,String delimiters) {
		return toString(new StringBuilder(),set,delimiters,0).toString();
	}
	
	public static StringBuilder toString(StringBuilder out,Set set,String delimiters,int level) {
		if (empty(set)) 
			return out;
		if (!set.is())
			out.append(negation);
		int size = set.size();
		for (int i=0; i<size; i++) {
			if (i > 0)
				if (delimiters != null && level < delimiters.length())
					out.append(delimiters.charAt(level));
				else
					out.append(space);
			if (set.get(i) instanceof Set)
				toString(out,(Set)set.get(i),delimiters,level + 1);
			else
				toString(out,set.get(i),set);
		}
		return out;
	}
	
	public static StringBuilder toString(StringBuilder out,Set set,boolean strict) {
		if (empty(set)) 
			return out;
		if (!set.is())
			out.append(negation);
		int size = set.size();
		if (size > 1 || strict)
			out.append(set instanceof Seq? '[' : set instanceof Any? '{' : '(' );
		for (int i=0; i<size; i++) {
			if (i > 0) { 
				if (!(set instanceof Seq)) {
					//out.append(',');//TODO:why do we thik we need comma here at all?
				}
				out.append(space);
			}
			toString(out,set.get(i),set);
		}
		if (size > 1 || strict)
			out.append( set instanceof Seq? ']' : set instanceof Any? '}' : ')' );
		return out;
	}
	
	private static void toString(StringBuilder out,Collection coll) {
		if (empty(coll)) 
			return;
		//recursion++;
		String[] sorted = new String[coll.size()];
		//TODO: consider when brackets need to appear
		//if (coll.size() > 1)
		//	out.append( '(' );
		int c = 0;
		for (Iterator it = coll.iterator(); it.hasNext(); )
			sorted[c++] = toString(it.next(),coll);
		Arrays.sort(sorted);
		for (int i=0;i<sorted.length;i++) {
			if (i > 0)
				out.append(',').append(space);
			out.append(sorted[i]);
		}
		//if (coll.size() > 1)
		//	out.append( ')' );
		//recursion--;
	}

	//TODO:more strict conditions of quoting other than ending with punctuation!?
	public static boolean needsQuoting(String string) {
		if (AL.empty(string)) // or just if string.length() == 0 ?
			return true; 
		if (string.charAt(0) == '#')//may be ID such as #123
			return true;
		//boolean url = AL.isURL(string);  
		for (int i = 0; i < string.length(); i++) {
			char ch = string.charAt(i);
			//http://stackoverflow.com/questions/7996919/should-url-be-case-sensitive
			//if (url && Character.isUpperCase(ch))
			//TODO: understand impact on total quoting, say disability to do case-insentivie searches and comparisons  
			if (Character.isUpperCase(ch))
				return true;
			if (AL.punctuation.indexOf(ch) != -1) // if punctiation before the end or spaces
				//if (i == string.length() || AL.spaces.indexOf(string.charAt(i)) != -1)
				if (!Parser.punctuationException(string,i,string.length(),AL.punctuation))
					return true;
			if (AL.quotes.indexOf(ch) != -1)
				return true;
		}
		return false;
	}
	
	public static String quote(String string) {
		return quote(new StringBuilder(),string).toString(); 
	}
	
	public static StringBuilder quotequotable(StringBuilder out, String string) {
		return needsQuoting(string) ? quote(out,string) : out.append(string); 
	}
	
	public static StringBuilder quote(StringBuilder out, String string) {
		boolean one = string.indexOf('\'') != -1;
		boolean two = string.indexOf('\"') != -1;
		char quote = one ? '\"' : '\''; 
		if (one && two) {
			string = string.replace("\'", "\\\'");
			string = string.replace("\"", "\\\"");
		}
		out.append(quote).append(string).append(quote);
		return out;
	}
	
	public static void toString(StringBuilder out,String string) {
		toString(out, string, false);
	}
	
	public static void toString(StringBuilder out,String string,boolean quoting) {
		if (string != null) { 
			if (quoting || needsQuoting(string))
				quote(out,string);
			else
				out.append(string);
		}
	}
	
	public static void toString(StringBuilder out,Object obj) {
		toString(out,obj,null);
	}
	
	public static void toString(StringBuilder out,Object obj,Object context) {
		if (obj instanceof Float)
			out.append(((Float)obj).floatValue());
		else	
		if (obj instanceof Double)
			out.append(((Float)obj).doubleValue());
		else	
		if (obj instanceof Boolean)
			out.append(((Boolean)obj).booleanValue());
		else	
		if (obj instanceof Integer)
			out.append(((Integer)obj).intValue());
		else	
		if (obj instanceof Number)
			out.append(((Number)obj).toString());
		else	
		if (obj instanceof String) 
			//TODO:fix quotable hack!
			toString(out,(String)obj,needsQuoting((String)obj) || (context instanceof String && Schema.quotable((String)context)));
		else 
		if (obj instanceof Property) 
			out.append('$').append(((Property)obj).name());
		else 
		if (obj instanceof Thing)
			toString(out, (Thing) obj, context);
		else 
		if (obj instanceof Collection)
			toString(out, (Collection)obj);
		else 
		if (obj instanceof Set)
			toString(out, (Set)obj, false);
		else 
		if (obj instanceof Object[])
			toString(out, new All((Object[])obj));
		else
		if (obj instanceof Ref) {
			Ref ref = (Ref)obj;
			if (!ref.is())
				out.append(negation);
			toString(out, ref.get(0),context);
		}
		else
		if (obj instanceof Date)
			out.append(Time.day((Date)obj,true));
		else
			out.append("<error>");
	}

	private static String toString(Object obj,Object context) {
		StringBuilder out = new StringBuilder();
		toString(out,obj,context);
		return out.toString();
	}
	
	public static String toString(Object obj) {
		return toString(obj,null);
	}

	public static String toString(Object[] array,Object context) {
		return toString(array,context,"[",", ","]");
	}
	
	public static String toString(Object[] array,Object context, String before, String middle, String after) {
		StringBuilder out = new StringBuilder();
		if (!AL.empty(array)){
			if (!AL.empty(before))
				out.append(before);
			for (int i = 0; i < array.length; i++){
				if (i > 0 && !AL.empty(middle))
					out.append(middle);
				toString(out,array[i],context);
			}
			if (!AL.empty(after))
				out.append(after);
		}
		return out.toString();
	}

	public static String what(String ref,Thing thing,Set terms) {
		StringBuilder out = new StringBuilder();
		out.append("what").append(' ').append(ref).append(' ').append(and(terms)).append('?');
		return capitalize(out);
	}

	//TODO:sensible query-context-driven output formation
	public static String toPrefixedString(Session session, Seq query, Object arg) {
		Object head = AL.empty(query) ? null : query.get(0);
		String ref = head == null ? null :
			head instanceof Body? AL.i_my[0]: //TODO: get rid of this hack needed for agressive GC impact
			head instanceof Thing && session != null && head.equals(session.getBody().self())? AL.i_my[0]: 
			head instanceof Thing && session != null && head.equals(session.getStoredPeer()) ? AL.you[0]: 
			head instanceof Thing && session != null && session.getStoredPeer()==null ? AL.you[0]: //TODO: fix demo hack for anonymous users! 
			head instanceof Thing && session != null && head.equals(session.getStoredPeer())? ((Thing)head).getString(name):
			head instanceof Seq && ((Seq)head).size()==2 && ((Seq)head).get(0).equals(AL.name)? ((Seq)head).get(1).toString(): 
			AL.there[0];
		if (query != null && query.size() > 2 && query.get(1) instanceof String[] && ((String[])query.get(1)).length ==1)
			ref += " "+((String[])query.get(1))[0];
		StringBuilder out = new StringBuilder();
		if (arg instanceof Thing) {
			if (!empty(ref))
				out.append(ref).append(' ');		
			out.append(toString((Thing)arg));
		}
		else {
			if (!empty(ref))
				out.append(ref).append(' ');		
			Collection things = (Collection)arg;
			if (AL.empty(things)){
				out.append(no[0]);
			}else {
				String[] sorted = new String[things.size()];
				int c = 0;
				for (Iterator it = things.iterator(); it.hasNext(); )
					sorted[c++] = toString(it.next());
				Arrays.sort(sorted);
				for (int i=0;i<sorted.length;i++) {
					if (i > 0)
						out.append(';').append(space);
					out.append(sorted[i]);
				}
			}
		}
		out.append('.');
		return capitalize(out);
	}

	public static String capitalize(StringBuilder builder) {
		if (builder.length() > 0)
			builder.setCharAt(0,Character.toUpperCase(builder.charAt(0)));
		return builder.toString();
	}
		
	public static String capitalize(String string) {		
		return string == null? null : capitalize(new StringBuilder(string)).toString();
	}

	//JSONify
	public static String toJSON(Object arg, Thing context){
		if (arg == null)
			return "";
		if (arg instanceof Object[] || arg instanceof Object){
			StringBuilder sb = new StringBuilder();
			toJSON(sb,arg,context);
			return sb.toString();
		}
		return arg.toString();
	}

	public static void toJSON(StringBuilder sb, Object arg, Thing context){
		if (arg instanceof Object[]){
			Object[] objs = (Object[]) arg;
			sb.append('[');
			for (int i = 0; i < objs.length; i++){
				if (i > 0)
					sb.append(',');
				toJSON(sb,objs[i],context);
			}
			sb.append(']');
		} else
		if (arg instanceof Set){
			Set set = (Set) arg;
			//TODO: negation?
			int size = set.size();
			//TODO: type?
			sb.append('[');
			for (int i=0; i<size; i++) {
				if (i > 0) { 
					sb.append(',');
				}
				toJSON(sb,set.get(i),context);
			}
			sb.append(']');
		} else
		if (arg instanceof Collection){
			Collection coll = (Collection)arg;
			String[] sorted = new String[coll.size()];
			int c = 0;
			for (Iterator it = coll.iterator(); it.hasNext(); )
				sorted[c++] = toJSON(it.next(),context);
			Arrays.sort(sorted);
			sb.append('[');
			for (int i=0; i<sorted.length; i++) {
				if (i > 0) { 
					sb.append(',');
				}
				sb.append(sorted[i]);
			}
			sb.append(']');
		} else
		if (arg instanceof Map){
			Map map = (Map) arg;
			int count = 0;
			sb.append('{');
			TreeSet keys = new TreeSet(map.keySet()); //map.keySet()
			for (Iterator i = keys.iterator(); i.hasNext();){
				Object k = i.next();
				Object v = map.get(k);
				if (k != null && v != null){
					if (count++ > 0)
						sb.append(',');
					sb.append('\"').append(k.toString()).append("\":").append(toJSON(v,context));
				}
			}
			sb.append('}');
		} else
		if (arg instanceof Thing){
			toJSON(sb, (Thing) arg, context);//as a JSON
		} else 
		if (arg instanceof String){
			String s = (String)arg;
			s = s.replace("\"", "\\\"");
			sb.append('\"').append(s).append('\"');
		} else {
			sb.append('\"');
			toString(sb,arg,null);
			sb.append('\"');
		}
	}

	public static void toJSON(StringBuilder out,Thing thing, Thing context) {
		//TODO: make unique reference AKA buildQualifier
		String ref = thing.getString(name);
		if (context != null && !AL.empty(ref)) {
			toJSON(out,ref,context);
		} else
		//TODO: do more fancy stuff like toString?
		{
			out.append('{');
			String[] names = thing.getNamesAvailable();
			{
				String[] sorted = new String[names.length];
				for (int i=0, c=0;i<names.length;i++) {
					String name = (String)names[i];
					Object value = thing.get(name);
					if (value != null)
						sorted[c++] = '\"' + name + "\":" + toJSON(value,thing);
				}
				Arrays.sort(sorted);
				for (int i=0; i<names.length; i++) {
					if (i > 0)
						out.append(',');
					out.append(sorted[i]);
				}
			}
			out.append('}');
		}
	}

	//TODO: turn into separate class for HTML/JSON/XLSX
	public static String toHTML(Object arg,java.util.Set properties){
		if (arg == null)
			return "";
		if (arg instanceof Object[] || arg instanceof Object){
			StringBuilder sb = new StringBuilder();
			toHTML(sb,arg,properties,false);
			return sb.toString();
		}
		return arg.toString();
	}

	private static java.util.Set getProps(Object arg){
		if (arg instanceof Map)
			return ((Map)arg).keySet();
		if (arg instanceof Thing)
			return Str.hset(((Thing)arg).getNamesAvailable());
		return new HashSet();
	}

	static final String table_start = "<table border=\"1\" style=\"border-collapse:collapse;\">\n";
	static final String header_start = "<tr><th>";
	static final String header_break = "</th><th>";
	static final String header_stop = "</th></tr>\n";
	static final String rows_start = "<tr><td style=\"vertical-align:top;\">";
	static final String cell_break = "</td><td style=\"vertical-align:top;\">";
	static final String row_break = "</td></tr>\n<tr><td>";
	static final String table_stop = "</td></tr>\n</table>";
	
	public static void toHTML(StringBuilder sb, Object arg, String property){
		if (arg instanceof java.util.Set){//pass properties as hack to indicate encloseness
			java.util.Set set = (java.util.Set) arg;//properties of a Thing!
			//TODO: negation?
			int i = 0;
			for (Object o : set) {
				if (i++ > 0)
					sb.append(";<br>");
				if (o instanceof Thing) {
					String n = ((Thing) o).name();
					if (!AL.empty(n))
						o = n;
				}
				toHTML(sb, o, property);
			}
		} else
		if (arg instanceof String){
			String s = (String)arg;
			String breakingstyle="overflow-wrap:break-word;word-wrap:break-word;word-break:break-all;";
			sb.append(AL.isIMG(s) || "image".equals(property) ? "<img style=\"height:64px;width:auto;\" src=\""+s+"\"/>" :
					AL.isURL(s) ? "<a style=\""+breakingstyle+"\" href=\""+s+"\" target=\"_blank\">"+s+"</a>" : HtmlStripper.encodeHTML(s));
		} else {
			sb.append(HtmlStripper.encodeHTML(arg.toString()));
		}
	}
	
	public static void toHTML(StringBuilder sb, Object arg, java.util.Set properties, boolean sort){
		if (arg instanceof Object[]){
			TreeSet props = new TreeSet(); 
			Object[] objs = (Object[]) arg;
			sb.append(table_start);
			for (int i = 0; i < objs.length; i++)
				props.addAll(getProps(objs));
			if (!AL.empty(props)) {
				sb.append(header_start);
				int i = 0;
				for (Object p : props) {
					if (i++ > 0)
						sb.append(header_break);
					sb.append(capitalize((String)p));
				}
				sb.append(header_stop);
			}
			sb.append(rows_start);
			for (int i = 0; i < objs.length; i++){
				if (i > 0)
					sb.append(row_break);
				toHTML(sb,objs[i],props,sort);
			}
			sb.append(table_stop);
		} else
		if (arg instanceof Collection && properties == null){//pass properties as hack to indicate encloseness
			TreeSet props = new TreeSet(); 
			Collection coll = (Collection)arg;
			String[] sorted = new String[coll.size()];
			for (Object o : coll)
				props.addAll(getProps(o));
			sb.append(table_start);
			if (!AL.empty(props)) {
				sb.append(header_start);
				int i = 0;
				for (Object p : props) {
					if (i++ > 0)
						sb.append(header_break);
					sb.append(capitalize((String)p));
				}
				sb.append(header_stop);
			}
			int c = 0;
			for (Iterator it = coll.iterator(); it.hasNext(); )
				sorted[c++] = toHTML(it.next(),props);
			if (sort)
				Arrays.sort(sorted);
			sb.append(rows_start);
			for (int i=0; i<sorted.length; i++) {
				if (i > 0) { 
					sb.append(row_break);
				}
				sb.append(sorted[i]);
			}
			sb.append(table_stop);
		} else
		if (arg instanceof Map){
			Map map = (Map) arg;
			int count = 0;
			TreeSet keys = new TreeSet(!AL.empty(properties) ? properties : map.keySet()); //sorted!?
			for (Iterator i = keys.iterator(); i.hasNext();){
				Object k = i.next();
				Object v = map.get(k);
				if (k != null && v != null){
					if (count++ > 0)
						sb.append(cell_break);
					toHTML(sb,v,(String)k);
				}
			}
		} else
		if (arg instanceof Thing){
			int count = 0;
			TreeSet keys = new TreeSet(!AL.empty(properties) ? properties : getProps(arg)); //sorted!?
			for (Iterator i = keys.iterator(); i.hasNext();){
				Object k = i.next();
				Object v = ((Thing)arg).get((String)k);
				if (count++ > 0)
					sb.append(cell_break);
				if (k != null && v != null)
					toHTML(sb,v,(String)k);//pass properties as hack to indicate encloseness
				else
					sb.append("&nbsp;");
			}
		} 
	}

}