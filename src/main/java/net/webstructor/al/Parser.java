/*
 * MIT License
 * 
 * Copyright (c) 2005-2019 by Anton Kolonin, Aigents®
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import net.webstructor.util.Array;

//http://www.tutorialspoint.com/java/java_regular_expressions.htm
//http://stackoverflow.com/questions/3674930/java-regex-meta-character-and-ordinary-dot
public class Parser {
	public static final String[] escapedQuotes = new String[]{"\\\'","\\\""};
	public static final char[] unescapedQuotes = new char[]{'\'','\"'};
	
	private String input;
	private int pos = 0;
	private int size = 0;
	private boolean quoting = true;
	private boolean urling = false;
	private String punctuation = AL.punctuation;
	
	public Parser(String input) {
		pos = 0;
		size = input.length();
		this.input = input;
	}
	
	public Parser(String input,boolean quoting,boolean urling,String punctuation) {
		this(input);
		this.quoting = quoting;
		this.urling = urling;
		if (punctuation != null)//null-defence
			this.punctuation = punctuation;
	}
	
	public boolean end() {
		return pos >= size; 
	}

	public int pos() {
		return pos; 
	}

	public void set(int cur) {
		pos = cur; 
	}

	private static boolean isPunctuationOrSpace(char c, String punctuation) {
		return punctuation.indexOf(c) != -1 || AL.spaces.indexOf(c) != -1;
	}

	//TODO: cleanup here and in call tree, move to AL
	//if point or period has next and previous characters present and they are neither punctuation nor space, disregard it as punctuation
	public static boolean punctuationException(String token, int cur, int length, String punctuation) {
		//TODO: do really allow any punctuations in the middle of the token?
		if (AL.separators.indexOf(token.charAt(cur)) == -1)//not allow anything but , and . inside
			return false;
		//treat as fair punctuation if last in text or there are subsequent punctuations  
		if ((cur + 1) == length || isPunctuationOrSpace(token.charAt(cur + 1),punctuation))
			return false;
		//TODO: do really allow points and periods heading the tokens?
		//if (cur == 0  || isPunctuationOrSpace(input.charAt(cur - 1)))
		//	return false;
		return true;
	}

	public static String[] split(String subject, String delimiters, String skip1chars) {
		StringTokenizer tok = new StringTokenizer(subject, delimiters);
		ArrayList list = new ArrayList(subject.length());
		while(tok.hasMoreTokens()){
			String token = tok.nextToken().trim();
			if (!AL.empty(skip1chars) && skip1chars.contains(token))
				continue;
			list.add(token);
		}
		return (String[])list.toArray(new String[]{});
	}

	public static java.util.Set<String> splitToSet(String subject, String delimiters, String skip1chars) {
		java.util.Set<String> set = new java.util.HashSet<String>();
		StringTokenizer tok = new StringTokenizer(subject, delimiters);
		while(tok.hasMoreTokens()){
			String token = tok.nextToken().trim();
			if (!AL.empty(skip1chars) && skip1chars.contains(token))
				continue;
			set.add(token);
		}
		return set;
	}
	
	public static String[] split(String subject, String delimiters) {
		return split(subject, delimiters, null);
	}
	
	public static HashMap<String,String> splitToMap(String subject, String tokenBreakers, String pairBreaker) {
		String[] split = split(subject, tokenBreakers, null);
		if (AL.empty(split))
			return null;
		HashMap map = new HashMap(split.length);
		//111
		for (String s : split){
			String[] keyvalue = Parser.split(s, pairBreaker);
			if (keyvalue != null && keyvalue.length == 2)
				map.put(keyvalue[0], keyvalue[1]);
		}
		return map;
	}
	
	//TODO: make it flexible to support N > 2 and maxDistance > 1
	public static All grams(Seq tokens, int maxDistance) {
		int N = 2;
		if (AL.empty(tokens))
			return new All(new Seq[]{});
		int limit = tokens.size() - N + 1;
		//Seq[] grams = new Seq[limit];
		ArrayList grams = new ArrayList();
		for (int i = 0; i < limit; i++){
			//grams[i] = new Seq(new String[]{(String)tokens.get(i),(String)tokens.get(i+1)});
			for (int d = 1; d <= maxDistance; d++){
				int j = i + d;
				if (j < tokens.size())
					grams.add(new Seq(new String[]{(String)tokens.get(i),(String)tokens.get(i+d)}));
			}
		}
		//return new All(grams);
		return new All(grams.toArray(new Seq[]{}));
	}

	//stupid parsing to chunks (including commas)
	//TODO: break into sentences instead
	public static Seq parse(String input) {
		return parse(input,(String)null,(List)null);
	}
	
	public static String[] parseS(String input,boolean tolower) {
		return parseS(input,(String)null,false,tolower,true,false,null,(List)null);
	}

	public static Seq parse(String input,List positions) {
		return parse(input,null,false,true,true,false,null,positions);
	}

	public static Seq parse(String input,String delimiters,List positions) {
		return parse(input,delimiters,false,true,true,true,null,positions);//urling=true
	}

	public static Seq parse(String input,String delimiters,boolean regexp,boolean tolower,boolean quoting,boolean urling) {
		return parse(input,delimiters,regexp,tolower,quoting,urling,null,null);
	}
	
	public static Seq parse(String input,String delimiters,boolean regexp,boolean tolower,boolean quoting,boolean urling,String punctuation,List positions) {
		return new Seq(parseS(input,delimiters,regexp,tolower,quoting,urling,punctuation,positions));
	}

	public static String[] parseS(String input,String delimiters,boolean regexp,boolean tolower,boolean quoting,boolean urling,String punctuation,List positions) {
		ArrayList chunks = new ArrayList();
		Parser parser = new Parser(input,quoting,urling,punctuation); 
		String s;
		for (;;) {
			int pos = parser.pos; 
			s = parser.parseTerm(regexp,tolower,delimiters);
			if (s == null)
				break;
			if (delimiters != null && delimiters.indexOf(s) != -1)
				continue;
			chunks.add(s);
			if (positions != null)
				positions.add(new Integer(pos));
		}
		return (String[])chunks.toArray(new String[]{});
	}

	//TODO: add pos account in 2D array?
	public static Seq parseSentenced(String input,String delimiters,boolean regexp,boolean tolower,boolean quoting,boolean urling,String breakers) {
		ArrayList sentences = new ArrayList();
		ArrayList chunks = new ArrayList();
		Parser parser = new Parser(input,quoting,urling,null); 
		String s;
		for (;;) {
			s = parser.parseTerm(regexp,tolower,delimiters);
			if (s == null)
				break;
			if (breakers != null && breakers.indexOf(s) != -1){//breaker token
				if (chunks.size() == 0)//skip empty phrases
					continue;
				sentences.add(new Seq(chunks.toArray()));
				chunks.clear();
			}
			if (delimiters != null && delimiters.indexOf(s) != -1)
				continue;
			chunks.add(s);
		}
		return new Seq(sentences.toArray());
	}
	
	//parse delimited list of thing names into String[] (gluing tokens in-between)
	public static String[] parse(String input, String delimeters) {
		ArrayList chunks = new ArrayList();
		Parser parser = new Parser(input); 
		StringBuilder sb = new StringBuilder();
		String s;
		while ((s = parser.parse()) != null) {
			if (delimeters.indexOf(s) == -1) { //not a delimiter
				if (sb.length() > 0)
					sb.append(AL.space);
				sb.append(s);
			} else {
				if (sb.length() > 0)
					chunks.add(sb.toString());
				sb.setLength(0);
			}
		}
		if (sb.length() > 0)
			chunks.add(sb.toString());
		return (String[])chunks.toArray(new String[]{});
	}

	//TODO: should be parse(force = false)
	public String check() {
		int cur = pos;
		String s = parse();
		pos = cur;
		return s;
	}

	public String tryRegexp(int first) {
		for (int cur = first + 1; cur < input.length(); cur++) {
			char c = input.charAt(cur);
			if (c == '/' && input.charAt(cur - 1) != '\\') {
				pos = ++cur;
				return input.substring(first,cur);
			}
		}
		return null;
	}
	
	//TODO:get rid of
	public String parse() {
		return parseTerm(false,true,null);
	}
	
	//TODO: return brackets [({})] as individuals
	public String parseTerm(boolean regexp,boolean tolower) {
		return parseTerm(regexp,tolower,null);
	}
	public String parseTerm(boolean regexp,boolean tolower,String delimiters) {
		char quoted = 0;
		int begin = -1;
		int cur = pos;
		String s;
		for (; cur < input.length(); cur++) {
			char c = input.charAt(cur);
			if (begin == -1 && c == '/' && regexp) {//regexp
				s = tryRegexp(cur);
				if (s != null)
					return s;
			}
			if (!this.quoting && (c == '\'' || c == '\"')) { // ignore quoting
				//
			}
			else
			if (this.quoting && begin == -1 && quoted == 0 && (c == '\'' || c == '\"')) { // start quoting
				quoted = c;
			}
			else
			if (quoted != 0) { //keep quoting
				if (begin == -1)
					begin = cur;
				//ensure escaping \' and \"
				boolean escaping = false;
				if (c == '\\'){
					int next = cur + 1;
					if (next < input.length()){
						char nc = input.charAt(next);
						if (nc == '\'' || nc == '\"')
							escaping = true;
					}
				}
				if (escaping)
					cur++;
				else
				if (quoted == c) {
					s = input.substring(begin,cur);
					this.pos = ++cur;
					return Array.replace(s, escapedQuotes, unescapedQuotes);//no .toLowerCase() for quoted strings!
				}
			}
			else
			if (begin == -1) { //not forming token
				if (delimiters != null && delimiters.indexOf(c) != -1)
					;//skip delimiters
				else
				if (punctuation.indexOf(c) != -1 && !punctuationException(input,cur,size,punctuation)) {//return symbol
					s = input.substring(cur,cur+1);
					pos = ++cur;
					return tolower ? s.toLowerCase() : s;
				} else
				if (AL.spaces.indexOf(c) == -1)//start froming token
					begin = cur;
				//else skip delimiters
			} else { //in the middle of the token
				if ((punctuation.indexOf(c) != -1 && !punctuationException(input,cur,size,punctuation))
						|| (delimiters != null && delimiters.indexOf(c) != -1)
						) {//return token
					s = input.substring(begin,cur);
					if (!(urling && AL.isURL(s) && AL.urls.indexOf(c) != -1 && 
							!((cur + 1) == size || isPunctuationOrSpace(input.charAt(cur + 1),punctuation)))){
						pos = cur;//stay at delimiter to return symbol later
						return tolower ? s.toLowerCase() : s;
					}
				}
				if (AL.spaces.indexOf(c) != -1) {//return token
					s = input.substring(begin,cur);
					pos = ++cur;//skip delimiter
					return !tolower || (urling && AL.isURL(s)) ? s : s.toLowerCase();
				}
				//else keep forming token
			}				
		}
		pos = cur;
		if (begin != -1){
			s = input.substring(begin);
			return !tolower || (urling && AL.isURL(s)) ? s : s.toLowerCase();
		}
		return null;//TODO:fix
	}

	public boolean parseOne(String one) {
		int cur = pos;
		String s = parse();//.toLowerCase();
		if (s == null || !s.equals(one)) {
			pos = cur;//rollback
			return false;
		}
		return true;
	}

	//scan string not in stoplist
	public String parseNotIn(String stoplist,String[] stopwords1,String[] stopwords2,boolean force) {
		int cur = pos;
		String s = parse();//.toLowerCase();
		//if (AL.empty(s) || stoplist.indexOf(s) != -1 
		if (s == null || (s.length() == 1 && stoplist.indexOf(s) != -1) 
			|| (stopwords1 != null && Array.contains(stopwords1, s))
			|| (stopwords2 != null && Array.contains(stopwords2, s))) {
			pos = cur;//rollback
			return null;
		}
		if (!force)
			pos = cur;
		return s;
	}

	public String parseNotStarting(String stoplist,String[] stopwords1,String[] stopwords2,boolean force) {
		int cur = pos;
		String s = parse();//.toLowerCase();
		//if (AL.empty(s) || stoplist.indexOf(s) != -1 
		if (s == null || (s.length() == 1 && stoplist.indexOf(s) != -1) 
			|| (stopwords1 != null && Array.startingWord(s, stopwords1) != null )
			|| (stopwords2 != null && Array.startingWord(s, stopwords2) != null )) {
			pos = cur;//rollback
			return null;
		}
		if (!force)
			pos = cur;
		return s;
	}

	public String parseAny(String[] any,boolean force) {
		String[] seq;
		for (int i = 0; i<any.length; i++) {
			int cur = pos;
			seq = split(any[i],AL.spaces);//TODO:invent anything better?
			if (parseSeq(seq,force)) {
				if (!force)
					pos = cur;//rollback if only trying
				return any[i];//advance
			}
			pos = cur;//rollback
		}
		return null;
	}
		
	public String parseAny(String any,boolean force) {
		int cur = pos;
		String s = parse();
		if (s != null)
			if (any.indexOf(s) == -1)
				s = null;
		if (s == null || !force)
			pos = cur;//rollback
		return s;
	}
		
	public boolean parseSeq(String[] seq, boolean force) {
		int cur = pos;
		String s;
		for (int i = 0; i<seq.length; i++) {
			s = parse();//.toLowerCase();
			if (s == null || !s.equals(seq[i])) {
				pos = cur;//rollback
				return false;
			}
		}
		if (!force)
			pos = cur;//rollback if only trying
		return true;//advance
	}
	
	public String[] parseAll(String[] any) {
		ArrayList set = new ArrayList();
		String[] seq;
		for (int i = 0; i<any.length; i++) {
			seq = split(any[i],AL.spaces);
			if (parseSeq(seq,true))
				set.add(any[i]);
		}
		return set.size() == 0 ? null : (String[])set.toArray(new String[]{});
	}
	
	public String toString(int pos) {
		return input.substring(pos);
	}
	
	public String toString() {
		return input.substring(pos);
	}
	
	
	public static void main(String args[]){
		if (args == null || args.length < 1){
			System.out.println("infile");
			System.exit(0);
		}
		net.webstructor.cat.TextFileReader reader = new net.webstructor.cat.TextFileReader();
		if (reader.canReadDoc(args[0])){
			String text;
			try {
				text = reader.readDocData(args[0]);
				Seq sentences = Parser.parseSentenced(text,AL.commas+AL.periods+AL.spaces,false,true,true,true,".!?");
				for (int i = 0; i < sentences.size(); i++){
					Set grams1 = Parser.grams((Seq)sentences.get(i),1);
					Set grams2 = Parser.grams((Seq)sentences.get(i),2);
					Set grams3 = Parser.grams((Seq)sentences.get(i),3);
					System.out.println(Writer.toString((Set)sentences.get(i)," ")+'\t'
							+Writer.toString((Set)grams1,"|")+'\t'
							+Writer.toString((Set)grams2,"|")+'\t'
							+Writer.toString((Set)grams3,"|"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
