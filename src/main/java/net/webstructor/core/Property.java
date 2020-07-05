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
package net.webstructor.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Iter;
import net.webstructor.cat.StringUtil;
import net.webstructor.util.Array;
import net.webstructor.util.Str;
import net.webstructor.comm.Emailer;
import net.webstructor.main.Tester;
import net.webstructor.al.Set;
import net.webstructor.al.Reader;

/**
 * Effectively, it is a Variable, with "owner" serving as a scope and "value" stored in context of the owner.
 * @author akolonin
 */
public class Property extends Anything implements Named {
	public static final int DEFAULT_PROPERTY_LIMIT = 100; 
	private static final String[] names = {"owner","value"};
	protected String name;//TODO: is
	protected Anything owner;
	protected Storager storager = null;
	private boolean hasPatterns = false;
	private int limit = DEFAULT_PROPERTY_LIMIT;

	public Property(Anything owner,String name,String def) {
		this.owner = owner;
		this.name = name;
		if (def != null)
			owner.setString(name, def);
	}
	
	public Property(Anything owner,String name) {
		this(owner,name,null);
	}
	
	public Property(Anything owner,String name,int limit) {
		this(owner,name,null);
		this.limit = limit;
	}
	
	public Property(Storager storager,Anything owner,String name) {
		this(owner,name,null);
		this.storager = storager;
		compile();//needs storager and owner to be present
	}
	
	public String getString() {
		return owner == null ? null : owner.getString(this.name);//TODO:make it impossible to have no owner?
	}
	
	public String toString() {
		String value = getString();
		return AL.empty(value) ? ("$" + this.name) : ("$" + this.name + " " + value);
	}
	
	@Override
	public String name() {
		return this.name;
	}

    //http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
    //minus	−	U+2212 (8722)	HTML 4.0	HTMLsymbol	ISOtech	minus sign
	//http://javascript.ru/RegExp
	private static final Pattern number = Pattern.compile("[-+−]?[0-9]+([\\.,][0-9]+)?");

	public boolean hasPatterns() {
		return hasPatterns;
	}
	
	//TODO: build pre-compiled ordered list of patterns and keep in variable!?
	private void compile() {
		if (storager == null)
			return;
		Thing variable = storager.getThing(name);
		if (variable == null)
			return;
		if (!AL.empty(variable.getThings(AL.patterns))){
			hasPatterns = true;
			return;
		}
		Collection is = variable.getThings(AL.is);
		if (!AL.empty(is))
			for (Iterator i = is.iterator(); i.hasNext();)
				if (!AL.empty(((Thing)i.next()).getThings(AL.patterns))){
					hasPatterns = true;
					return;
				}
	}
	
	public boolean read(Iter it, StringBuilder summary){
		if (storager == null)
			return false;
		StringBuilder value = new StringBuilder();
		//if variable has patterns, recursively to variable domains
		Thing variable = storager.getThing(name);
		if (variable == null)
			return false;
		if (read(it, variable, value)){
			//TODO: entity extraction goes here!?
			String v = value.toString();
			setString(v);
			Str.append(summary,v);
			return true;
		}
		return false;
	}

	//TODO: move to static Reader, once patterns are pre-compiled
	public boolean read(Iter it, Thing domain, StringBuilder summary){
		//first, check patterns of the domain
		Collection ps = domain.getThings(AL.patterns);
		if (!AL.empty(ps)){
			for (Iterator i = ps.iterator(); i.hasNext();){
				//TODO: system-wide cache of compiled patterns!?
				String patstr = ((Thing)i.next()).name();
				Set pat = Reader.pat(storager, owner instanceof Thing ? (Thing)owner : null, patstr);
				if (Reader.read(it, pat, summary))
					return true;
			}
		}
		//next, check if domain has super-domains with patterns
		Collection domains = (Collection)domain.get(AL.is);
		if (!AL.empty(domains)){
			for (Iterator i = domains.iterator(); i.hasNext();){
				if (read(it, (Thing)i.next(), summary))
					return true;
			}
		}
		return false;
	}

	//+1 = yes, -1 = no, 0 = don't know
	public int domain(String name, String value) {
		//TODO:a lot!
		if (AL.empty(name))
			return 0;
		if (name.equals(AL.email))
			return Emailer.valid(value) ? +1 : -1;
		if (name.equals(AL.word))
			return Array.contains(value,AL.spaces) ? -1 : +1;
		//if (name.equals(AL.date) && !Time.isDate(value))
		//	return false;
		if (name.equals(AL.daytime)) 
			return Time.isDayTime(value) ? +1 : -1;
		//if (name.equals(AL.number) && !StringUtil.isDouble(value))
		if (name.equals(AL.number))
			return number.matcher(value).matches() ? +1 : -1;
		if (name.equals(AL.money)) 
			return isMoney(value) ? +1 : -1;
	//	if (name.equals("phrase")) { //TODO: AL.phrase
	//		String seq[] = Parser.split(value,AL.spaces);
	//		if (Array.contains(seq, AL.period))
	//			String[] seq = split(any[i],AL.spaces);//TODO:invent anything better?
		Pattern p = regexpPattern(name);
		if (p != null)
			return isRegexpMatching(p,value) ? +1 : -1;
		
		/** TODO if it makes sense at all?
		 * TODO maybe we should just use property's pattern (own or inherited) in a reader to read it as pattern!!!??? 
		//TODO: test thoroughly
		if name is pattern, RELAXED match!?
		Set patseq = Reader.pattern(storager, owner instanceof Thing ? (Thing)owner : null, name);
		if (Reader.read(value, patseq, true))//strict matching
			return +1;

		//TODO: test thoroughly
		//if name has pattern, STRICT match?
		if (storager != null){
			Thing t = storager.getThing(name);
			Collection ps = t.getThings(AL.patterns);
//if (name.equals("x") || value.equals("y") || value.equals("z") || value.equals("meeting") || value.equals("personname"))
if (name.equals("meeting") && value.equals("has"))
System.out.println(name+" "+value+" "+ps);				
			if (!AL.empty(ps)){
				for (Iterator it = ps.iterator(); it.hasNext();){
					//TODO: system-wide cache of compiled patterns!?
					String pat = ((Thing)it.next()).getName();
					//Set seq = Reader.pat(storager, owner instanceof Thing ? (Thing)owner : null, pat);
					Seq seq = Reader.pattern(storager, owner instanceof Thing ? (Thing)owner : null, pat);
					if (Reader.read(value, seq, true)){//strict matching
System.out.println(name+" "+value+" "+ps+" PASSED");				
						return +1;
					}
					
				}
System.out.println(name+" "+value+" "+ps+" FAILED");				
				return -1;
			}
		}
		**/
		
		return 0;
	}
	

	//TODO: check 
	public boolean domains(String value) {
		int name_match;
		if ((name_match = domain(name,value)) != 0) //implicit domain evaluation by property name
			return name_match > 0 ? true : false;
		if (storager != null) { //implicit domain evaluation by property classes
			Collection things = storager.getNamed(this.name);
			if (!AL.empty(things))
				for (Iterator i=things.iterator();i.hasNext();) {
					Thing thing = (Thing)i.next();
					Collection domains = (Collection)(thing).get(AL.is);
					if (!AL.empty(domains))
						for (Iterator it = domains.iterator();it.hasNext();) {
							Thing cls = (Thing)it.next();
							String name = cls.name();
							if (!AL.empty(name)) {
								int is_match;
								if ((is_match = domain(name,value)) != 0)
									return is_match > 0 ? true : false;
							}
						}
				}
		}
		//return name_match >= 0 ? true : false;
		return name_match == 0 ? value != null && (limit <= 0 || value.length() < limit)//if no explicit constrains and limit is eher not set or satisfied
				: name_match > 0 ? true : false;//if explicit constraints either matched or not
	}

	//http://stackoverflow.com/questions/1649435/regular-expression-to-limit-number-of-characters-to-10
	//public static boolean isRegexp(String reg) {
	//	int len = reg.length();
	//	return len > 3 && reg.charAt(0) == '/' && reg.charAt(len-1) == '/';
	//}
	private static HashMap<String,Pattern> compiledPatterns = new HashMap<String,Pattern>();
	public static Pattern regexpPattern(String reg) {
		int len = reg.length();
		if (!(len > 3 && reg.charAt(0) == '/' && reg.charAt(len-1) == '/'))
			return null;
		synchronized (compiledPatterns) {
			if (compiledPatterns.containsKey(reg))
				return compiledPatterns.get(reg);
		}
		Pattern p = Pattern.compile(reg.substring(1,reg.length()-1));
		synchronized (compiledPatterns) {
			compiledPatterns.put(reg,p);
		}
		return p;
	}
	
	//TODO: pre-compile regexp expressions in the map
	//https://regexr.com/
	//https://www.rexegg.com/regex-quickstart.html
	//public static boolean isRegexpMatching(String reg, String val) {
	public static boolean isRegexpMatching(Pattern p, String val) {
		//String pat = reg.substring(1,reg.length()-1);
		//Pattern p = Pattern.compile(pat);
		Matcher m = p.matcher(val);
		boolean match = m.matches();//exact match
//TODO: to eable incremental matching over spaces - fix agent_web.php: get("There text михаил женится на марии.");
		//if (!match)
		//	match = m.hitEnd();//partial icremental match //TODO change pattern matcher for that kind of logic?
		return match;
	}
	
	public Anything setString(String value) {
		if (owner != null)//TODO:make it impossible to have no owner?
			owner.setString(this.name, value);
		return this;
	}

	public String[] getNamesAvailable() {
		return names;//TODO:sure?
	}

	public String[] getNamesPossible() {
		return names;
	}

	public boolean del() {
		return true;
	}
	
	//TODO: this in some other place
	public boolean isMoney(String value) {
		String sym;
		sym = Array.prefix(currency_symbols,value);
		if (sym != null)
			return StringUtil.isDouble(value.substring(sym.length()));
		sym = Array.suffix(currency_symbols,value);
		if (sym != null) 
			return StringUtil.isDouble(value.substring(0,value.length()-sym.length()));
		return false;		
	}
	
	//TODO: move this to separate class Money
	private static String[] currency_symbols = getAllCurrencySymbols();	
	private static String[] getAllCurrencySymbols() {
        java.util.Set toret = new HashSet();
        Locale[] locs = Locale.getAvailableLocales();
        for(int i = 0; i < locs.length; i++) {
            try {
            	Currency curr = Currency.getInstance( locs[i] );
            	//US   USD  $
                toret.add( curr.getSymbol(locs[i]) );
            } catch(Exception exc)
            {
                // Locale not found
            }
        }
        return (String[])toret.toArray(new String[]{});
    }	
	
	/**
	 * Checks if there are variables in the pattern, recursively
	 * @param set pattern
	 * @return true if there is at least one variable in the pattern
	 */
	public static boolean containedIn(net.webstructor.al.Set set) {
		for (int i = 0; i < set.size(); i++) {
			Object o = ((net.webstructor.al.Set) set).get(i);
			if (o instanceof Property)
				return true;
			if (o instanceof net.webstructor.al.Set && containedIn((net.webstructor.al.Set)o))
				return true;
		}
		return false;
	}
	
	/**
	 * Collect names of variables in a patttern set
	 * @param set corresponding to the pattern
	 * @param collector
	 */
	public static void collectVariableNames(net.webstructor.al.Set set, HashSet<String> collector) {
		for (int i = 0; i < set.size(); i++) {
			Object o = ((net.webstructor.al.Set) set).get(i);
			if (o instanceof Property)
				collector.add(((Property)o).name);
			if (o instanceof net.webstructor.al.Set)
				collectVariableNames((net.webstructor.al.Set)o,collector);
		}
	}

	public static String toWordList(Set set){
		StringBuilder sb = new StringBuilder();
		toWordList(set, sb);
		return sb.toString();
	}
	protected static void toWordList(Set set, StringBuilder sb){
		if (!AL.empty(set)) for (int i = 0; i < set.size(); i++){
			Object o = set.get(i); 
			if (o instanceof String) {
				if (sb.length() > 0)
					sb.append(' ');
				sb.append((String)o);
			} else
			if (o instanceof String[]) {
				for (String s : (String[])o) {
					if (sb.length() > 0)
						sb.append(' ');
					sb.append(s);
				}
			} else
			if (o instanceof Set)
				toWordList(((Set)o),sb);
		}
	}
	
	//https://www.freeformatter.com/java-dotnet-escape.html#ad-output
	//https://regexr.com/
	//https://www.rexegg.com/regex-quickstart.html
	//https://stackoverflow.com/questions/42104546/java-regular-expressions-to-validate-phone-numbers
	public static void main(String[] args) throws Exception {
		//String[] ss = { "aabb", "aa", "cc", "aac" };
		//Pattern p = Pattern.compile("aabb");
		String[] ss = {
	    	"(",
	    	"(123)",
	    	"(1",
	    	"+1",
	       	"1 (",
	       	"( 1",
    		"713-904-3537",
    		"(713) 904-3537",
    		"( 713 ) 904-3537",
    		"1 ( 713 ) 904-3537",
    		"+1 ( 713 ) 904-3537",
    		"+1 713-904-3537",
    		"+1(713)904-3537",
    		"+1-713-904-3537",
    		"(123) 456-7890",
    		"1234567890",
    		"123-456-7890",
    		"(123)456-7890",
    		"(123)4567890",
    		"77006 (713) 904-3537"
		};
		String ps = "^\\s?((\\+?[1-9]{1,4}[ \\-]*)|(\\(\\s?[0-9]{2,3}\\s?\\)[ \\-]*)|([0-9]{2,4})[ \\-]*)*?[0-9]{3,4}?[ \\-]*[0-9]{3,4}?\\s?";
		Pattern pt = Pattern.compile(ps);
		Matcher m = pt.matcher("");
		System.out.printf("pattern: %s\n",ps);
		for (String s : ss) {
			m.reset(s);
			if (m.matches())
				System.out.printf("%-4s : match%n", s);
			else if (m.hitEnd())
				System.out.printf("%-4s : partial match%n", s);
			else
				System.out.printf("%-4s : no match%n", s);
			//boolean rm = isRegexpMatching("/"+ps+"/",s);
			//System.out.println(rm);
    	}
		
		Tester t = new Tester();
		t.init();
		t.assume(toWordList(Reader.patterns(null,null,"{[a bb][ccc {dddd [eeeee (ffffff ggggggg)]}]}")),"a bb ccc dddd eeeee ffffff ggggggg");
		t.assume(toWordList(Reader.patterns(null,null,"{[a $x bb][ccc {$yy dddd [eeeee (ffffff ggggggg $zzz)]}]}")),"a bb ccc dddd eeeee ffffff ggggggg");
		t.assume(toWordList(Reader.patterns(null,null,"")),"");
		t.assume(toWordList(Reader.patterns(null,null,"a")),"a");
		t.assume(toWordList(Reader.patterns(null,null,"a $x b")),"a b");
		t.check();
  	}
}
