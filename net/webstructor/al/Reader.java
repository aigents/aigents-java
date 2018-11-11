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
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.core.Anything;
import net.webstructor.core.Mistake;
import net.webstructor.core.Property;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.peer.Session;
import net.webstructor.util.Array;
import net.webstructor.util.Str;

public class Reader extends AL {
	
	public Reader(Body body) {
		super(body);
	}

	public int readMood(String input) {
		int first_space = input.indexOf(' ');
		String first_word = ((first_space == -1) ? input : input.substring(0,first_space)).toLowerCase();
		char last_symbol = '.';
		for (int i = input.length() - 1; i>=0; i--) {
			char symbol = input.charAt(i);
			if (spaces.indexOf(symbol) == -1) {
				last_symbol = symbol;
				break;
			}
		}
		if (last_symbol == '!' || first_word.equals("do"))
			return direction;
		if (last_symbol == '?' || first_word.equals("what"))
			return interrogation;
		if (last_symbol == '?' && first_word.equals("if")) //TODO: figure out from graph closure
			return confirmation;
		return declaration;
	}
	
	public static boolean read(String input, Set set) {
		Iter it = new Iter(Parser.parse(input));
		return read(it,set,null);
	}

	//TODO: do this in the other place in some other way?
	public static boolean read(String[] args, Set set) {
		StringBuilder sb = new StringBuilder();
		if (args != null)
			for (int i=0; i<args.length; i++) {
				if (i>0)
					sb.append(AL.space);
				//Writer.toString(sb,args[i]);
				sb.append(args[i]);
			}
		return read(sb.toString(),set);
	}
	
	private static StringBuilder append(StringBuilder builder,String text) {
		return Str.append(builder, text);
	}
	
	private static StringBuilder reAppend(StringBuilder builder,String text) {
		if (builder != null && text != null) {
			if (builder.length() < text.length())
				return builder;
			builder.setLength(builder.length() - text.length());
			if (builder.length() > 0 && builder.charAt(builder.length()-1) == ' ')
				builder.setLength(builder.length()-1);
		}
		return builder;
	}
	
	//advances on success, stays in place on failure 
	public static boolean read(Iter it, Object term, StringBuilder summary) {
		return read(it, term, summary, false);
	}
	
	public static boolean read(Iter it, Object term, StringBuilder summary, boolean collectingFrame) {
		if (!it.has())
			return false;
		String s;
		if (term instanceof String) {
			s = ((String)it.get());//.toLowerCase();//TODO: assert lowercase & get rid of toLowerCase?
			if (((String)term).equals(s) || 
				(Property.isRegexp((String)term)) && Property.isRegexpMatching((String)term, s)) {
				append(summary,s);
				it.next();
				return true;
			}
		}
		else
		if (term instanceof Property && ((Property)term).hasPatterns()) {
			return ((Property)term).read(it,summary);//TODO: collectingFrame!?
		} else
		if (term instanceof Property && 
			//!AL.empty( s = (String)((String)it.get()) ) &&
			( s = (String)((String)it.get()) ) != null && //allow empty strings such as ''!
			((Property)term).domains(s) && !isTerminator(s)) {
			if (!Array.contains(AL.lister,s)) {
				((Property)term).setString(s);//TODO: query handling and check value domain error
				append(summary,s);
			}
			it.next();
			return true;
		} else
		if (term instanceof Seq) {
			return read(it,(Seq)term,summary,collectingFrame);
		} else
		if (term instanceof Any) {
			return read(it,(Any)term,summary,collectingFrame);
		}			
		//TODO: All - set elements to gather in any order ?
		return false;
	}

	private static boolean isBreaker(String str) {
		return AL.periods.indexOf(str) != -1 || AL.commas.indexOf(str) != -1;
	}
	
	private static boolean isTerminator(String str) {
		boolean terminator = !AL.empty(str) && AL.periods.indexOf(str) != -1;
		if (terminator)
			return terminator;
		else
			return false;
	}
	
	//private static boolean read(Iter it, Seq seq, StringBuilder rootSummary) {
	//	return read(it, seq, rootSummary, false);
	//}
	
	//TODO: optimise structure
	private static boolean read(Iter it, Seq seq, StringBuilder rootSummary, boolean collectingFrame) {
		int i = 0, read = 0, pos = it.pos, size = seq.size();

		StringBuilder tempSummary = rootSummary != null ? new StringBuilder() : null;
		for (;it.has();) {//repeat trying to match a pattern in "seq" from the very beginning till the text in "it" is over 
			int startpos = -1;
			for (; i<size && read < size; i++) {
				for (;it.has();) {
					
					if (i < size) {
						int origpos = it.pos;
						if (read(it,seq.get(i),tempSummary,i > 0 && seq.get(i-1) instanceof Property)) {
							if (startpos == -1)
								startpos = origpos;
							read++;
							if (read == size && seq.get(i) instanceof Property && !((Property)seq.get(i)).hasPatterns()) {
								i++;//try appending last property
								continue;
							}
							break;
						}
						else {
							//if (i == 0 && collectingFrame)//if on stack of frame filling (this works)
							if (collectingFrame)//if on stack of frame filling
								break;
							if (isTerminator((String)it.get())) {
								if (seq.get(i) instanceof Property && !((Property)seq.get(i)).hasPatterns())//break property filling process
									break;
								if (tempSummary != null)
									tempSummary.setLength(0);
								i = 0; read = 0;
								startpos = -1;
								//TODO: reset all properties!? 
							}
						}
					} 
				
					//TODO: the same more clever!?
					//if not read, try to extend existing property from the beginning of the phrase
					if (i > 0 && seq.get(i-1) instanceof Property && !((Property)seq.get(i-1)).hasPatterns()) {
					//if (i > 0 && seq.get(i-1) instanceof Property && !collectingFrame) {
						Property property = (Property)seq.get(i-1);
						String str = ((String)it.get());//TODO:???.toLowerCase();
						boolean breaker = isBreaker(str);
						//TODO: sometimes need full text, sometimes need to break
						//if (breaker)
						//	break;
						/**/
						if (i == 1 && breaker) {//first variable frame boundary
							String val = property.getString();
							if (val != null && tempSummary != null) 
								//summary.setLength(summary.length() - val.length());
								reAppend(tempSummary,val);
							property.setString(null);//reset to start with new phrase
							//TODO: sort out below
							//!! this breaks the "public hearing" test case by weird reason
							//i = 0;
							//read = 0;
							//startpos = -1;
						}
						else
						if (i == size && breaker) {//last	variable frame boundary
							//it.next();//TODO: or not to do
							break;//stop current phrase
						}
						else {						
							String val = property.getString();
							val = AL.empty(val) ? str : val + ' ' + str;
							//TODO:optimization
							if (property.domains(val)) {
								property.setString(val);
								if (tempSummary != null)
									append(tempSummary,str);
							}
							else
								break;
						}
					}
					it.next();
				}
			}
			if (read == size) {
				if (rootSummary != null) {
					if (rootSummary.length() > 0)
						rootSummary.append(' ');
					rootSummary.append(tempSummary);
				}
				return true;
			}
			if (startpos != -1) {//if there was a "false start", start marching process over from the next position
				i = 0;
				read = 0;
				it.pos(startpos + 1);
				startpos = -1;
				if (tempSummary != null)
					tempSummary.setLength(0);
			} 
			else//TODO: fix this hacky fix?
				if (it.has())
					it.pos(it.cur() + 1);
				else
					break;
		}
		it.pos(pos);//restore
		return false;
	}
	
	//TODO:this in more clever way!
	//private final static Any delimiters = new Any(1,AL.lister);

	//TODO:make better decision when to stop - like have a HashSet and count each of options
	private static boolean read(Iter it, Any set, StringBuilder summary,boolean collectingFrame) {
		//new "greedy" Any-parsing strategy
		int cnt = 0;
		int first_pos = it.pos;//where to start from
		int last_pos = it.pos;//where to proceed from
		for (int i = 0; i < set.size(); i++) {
			//skip extra non-varialbe-carriers
			if (cnt > 0 && !(set.get(i) instanceof Set && Property.containedIn((Set)set.get(i))))
				continue;
			it.pos(first_pos);//
			if (read(it,set.get(i),summary,collectingFrame)) {
				cnt++;
				if (last_pos < it.pos) //remember where to proceed from after end
					last_pos = it.pos;
			}
		}
		it.pos(cnt > 0 ? last_pos : first_pos);
		return cnt > 0;
	}

	public static Seq pattern(String[] refs, Anything owner, String[] terms) {
		ArrayList pats = new ArrayList();
		for (int i=0; i<terms.length; i++) {
			String[] term = Writer.needsQuoting(terms[i]) 
				? new String[]{terms[i]} // don't break quoted things apart? 
				: terms[i].split(" ");
			Object[] objects = new Object[term.length + 1];
			for (int j=0; j<term.length; j++)
				objects[j] = term[j];
			objects[term.length] = new Property(owner,terms[i]);
			pats.add(new Seq(objects));
		}
		Seq seq = !AL.empty(refs)?
			new Seq(new Object[]{
				new Any(1,refs),	
				new Any(pats.toArray())	
				}):
			new Seq(new Object[]{new Any(pats.toArray())});
		return seq;		
	}
	
	public static boolean hasVariables(String pattern) {
		Parser parser = new Parser(pattern);
		for (;!parser.end();) {
			String term = parser.parse();
			if (AL.isVariable(term))
				return true;
		}
		return false;
	}

	public static Seq pattern(Storager storager, Thing owner, String pattern) {
		//cast to Seq because stopper==null (Seq by default)//TODO:do this cleaner 
		net.webstructor.al.Set set = pat(storager,owner,pattern);
		return set instanceof Seq ? (Seq)set : new Seq(new Object[]{set});
	}

	public static Set pat(Storager storager, Thing owner, String pattern) {
		Parser parser = new Parser(pattern);
		return pat(storager,owner,parser,null);
	}

	//TODO: real AL parsing into real statement with all these {}[]()
	public static Set pat(Storager storager, Thing owner, Parser parser, String stopper) {
		ArrayList terms = new ArrayList();
		for (;!parser.end();) {
			String term = parser.parseTerm(true,true);//with regexp
			if (AL.empty(term))
				break;
			if (stopper != null && stopper.equals(term))
				break;
			if (AL.isVariable(term)) // (term.length() > 1 && term.charAt(0) == '$')
				terms.add(new Property(storager,owner,term.substring(1)));
			else
			if (term.equals("[")) {
				terms.add(pat(storager,owner,parser,"]"));
			} else
			if (term.equals("{")) {
				terms.add(pat(storager,owner,parser,"}"));
			} else	
			if (stopper != null && Array.contains(AL.lister,term))//skip delimiters in explicit sets
				;//skip delimiters //TODO: in more clever way?//TODO: hack to pass tests!?
			else
				terms.add(term);
		}
		if (terms.size() == 1 && terms.get(0) instanceof Set)
			return (Set)terms.get(0);
		if ("}".equals(stopper))
			return new Any(terms.toArray());
		return new Seq(terms.toArray());
	}
	
	//New: real AL parsing into real statement with all these {}()[] : 0-Any,1-All,2-Seq
	public static Set patterns(Storager storager, Thing owner, String pattern) {
		Parser parser = new Parser(pattern);
		return patterns(storager,owner,parser,-1);
	}
	
	public static Set patterns(Storager storager, Thing owner, Parser parser, int settype) {
		ArrayList terms = new ArrayList();
		String closer = null;
		int temptype;
		for (;!parser.end();) {
			String term = parser.parseTerm(true,true);//with regexp
			if (AL.empty(term))
				break;
			//first, identify set type as specified or by opening bracket
			if (closer == null) {
				//if defined then use it
				if (settype != -1)
					closer = AL.brackets_close.substring(settype,settype+1);
				else
				//if not defined at all and first term identifies it
				if (settype == -1 && (settype = AL.brackets_open.indexOf(term)) != -1) {
					closer = AL.brackets_close.substring(settype,settype+1);
					continue;
				//otherwise, consider SEQ by default
				} else
					closer = AL.brackets_close.substring(settype = 2,settype+1);
			}
			//close brackets
			if (closer.equals(term))
				break;
			if (term.length() > 1 && term.charAt(0) == '$')
				terms.add(new Property(storager,owner,term.substring(1)));
			else //TODO: the same properly - recursively!
			if ((temptype = AL.brackets_open.indexOf(term)) != -1)
				terms.add(patterns(storager,owner,parser,temptype));
			else
				terms.add(term);
		}		
		return
			settype == 0 ? (Set)new Any(terms.toArray()) :
			settype == 1 ? (Set)new All(terms.toArray()) :
			settype == 2 ? (Set)new Seq(terms.toArray()) :
			null;
	}	
	
	public static Any splitToAny(String[] terms) {
		ArrayList pats = new ArrayList();
		for (int i=0; i<terms.length; i++) {
			String[] term = terms[i].split(" ");
			Object[] objects = new Object[term.length];
			for (int j=0; j<term.length; j++)
				objects[j] = term[j];
			pats.add(new Seq(objects));
		}
		return new Any(pats.toArray());
	}
	
	public static Seq pattern(String[] refs, String[] terms) {
		Seq seq = new Seq(new Object[]{new Any(1,refs),splitToAny(terms)});
		return seq;		
	}
	
	public static Seq pattern(String[] refs, String[] terms, String term) {
		Seq seq = new Seq(new Object[]{new Any(1,refs),splitToAny(terms),term});
		return seq;		
	}
	
	public int parseMood(Parser parser) {
		int mood;
		if (parser.parseAny(what,true) != null) 
			mood = interrogation;
		else
		if (parser.parseAny(iff,true) != null) 
			mood = confirmation;
		else
		if (parser.parseAny(doo,true) != null) 
			mood = direction;
		else 
			mood = declaration;
		return mood;
	}
	
	/*
	<message> := ( <statement> | <acknowledgement> )*
		<acknowledgement> := ( 'ok' | ('true' | 'yes' | <number>) | ('no' | 'false' | 0) ) '.' 
		<statement> := <interrogation> | <confirmation> | <declaration> | <direction>
			<interrogation> := 'what' ? <expression> '?'
			<confirmation> := 'if' ? <expression-set> '?'
			<declaration> := ( <expression-set> ) '.'
			<direction> := 'do' ? <expression-set> '!'
	<expression> := <term> (' ' <term>)*
	<expression-set> := <all-set> | <any-set> | <seq-set> (* different kinds of sets *)
		<term> := <negation>? ( <anonymous>? | <self> | <peer> | <id> | <name> | <value> | <qualifier> ) 
			<negation> := 'not' | 'no' | '~'
			<anonymous> := ('there' ('is'|'are')) | 'any' | 'anything' ? 
			<self> := 'my'|'i'|'we'|'our'
			<peer> := 'your'|'you'
			<value> := <number> | <date> | <time> | <string>	 
			<qualifier> := <expression> | <expression-set>
		<any-set> := <or-list> | ( '{' <or-list> '}' )
			<or-list> := <expression> ( (',' | 'or' ) <expression> )* 
		<all-set> := <and-list> | ( '(' <and-list> ')' )
			<and-list> := <expression> ( (',' | 'and' ) <expression> )* 
		<seq-set> := <then-list> | ( '[' <then-list> ']' )
			<then-list> := <expression> ( (',' | 'next' ) <expression> )* 
	 */	
	public Collection parseStatements(Session session, String input, Thing peer) throws Exception {
		Storager storager = session != null ? session.getStorager() : body.storager;
		Parser parser = new Parser(input);
		ArrayList message = new ArrayList();
		while (parser.check() != null) {
			Statement statement = parseStatement(storager,session,parser,peer);
			if (!AL.empty(statement))
				message.add(statement);
			if (parser.parseAny(AL.periods,true) == null)
				break;
		}
		return message;
	}
	
	public Statement parseStatement(Session session, String input, Thing peer) throws Exception {
		Storager storager = session != null ? session.getStorager() : body.storager;
		Parser parser = new Parser(input);
		return parseStatement(storager,session,parser,peer);
	}
	
	public Statement parseStatement(Storager storager, Session session, Parser parser, Thing peer) throws Exception {
		//Storager storager = session != null ? session.getStorager() : body.storager;
		//Parser parser = new Parser(input);
		int mood = parseMood(parser);
		
		//TODO: try to parse true SAL (structured AL query)
		if (parser.parseAny("[",false) != null){
			Set set = patterns(storager,peer,parser,-1);
			return new Statement(session.getMood(),true,set.set);
		}
		
		String[] properties = storager.getNames(); 
		String[] thing_names = storager.getNames(AL.name);
		boolean negation = parser.parseAny(not,true) != null;
		//TODO: check opening brackets
		{
			ArrayList terms = new ArrayList();
			//parse expression terms
			for (int newpos, pos = -1;!parser.end();) {
				Object head = terms.size() < 1 ? null : terms.get(terms.size()-1);
						
				//terminate deadloops in parsing
				if ((newpos = parser.pos()) == pos) 
					break;//TODO:exception?
				pos = newpos;
				
				if (terms.size() == 0) {
					/**/
					//on Android, this fails for "is peer, id 91 trust false" - parser.parseAny(there,true) returns null but condition is passed
					if (session != null && parser.parseAny(i_my,true) != null) //<peer>
						terms.add(peer);
					else
					if (parser.parseAny(you,true) != null) //<self>
						terms.add(body.self());
					else
					if (parser.parseAny(there,true) != null) //<anonymous>
						terms.add(new Thing());
					else
					/**/
					/**
					//on Android, this fails for "is peer, id 91 trust false" - parser.parseAny(there,true) returns null but condition is passed
					String term1, term2, term3;
					if (session != null && (term1 = parser.parseAny(i_my,true)) != null) //<peer>
						terms.add(session.getStoredPeer());
					else
					if ((term2 = parser.parseAny(you,true)) != null) //<self>
						terms.add(body.self());
					else
					if ((term3 = parser.parseAny(there,true)) != null) //<anonymous>
						terms.add(new Thing());
					else
					/**/
					/*
					//on Android, this works for "is peer, id 91 trust false" but fails further
					String term1, term2, term3;
					boolean done = false;
					if (session != null && (term1 = parser.parseAny(i_my,true)) != null) {//<peer>
						terms.add(session.getStoredPeer());
						done = true;
					}
					if (!done && (term2 = parser.parseAny(you,true)) != null) { //<self>
						terms.add(body.self());
						done = true;
					}
					if (!done && (term3 = parser.parseAny(there,true)) != null) { //<anonymous>
						terms.add(new Thing());
						done = true;
					}
					if (!done)
					*/
					{//<qualifier>
						//using dummy thing for scope (turned too restrictive for some cases)
						//Object step = parseExpression(parser,new Thing(),storager);
						//OR
						//using current set of names for scope (isn't it too expansive?)
						Object step = parseExpression(terms,parser,properties,thing_names,storager);						
						if (step instanceof Set && !AL.empty((Set)step))						
							//TODO:what - keep query as path element or replace with a thing or collection?
							terms.add(step);
						else
						if (step instanceof Thing)
							terms.add(step);//assume "by name" query is executed in parseExpression
						else
							throw new Mistake(Mistake.no_thing); 
					}
				} else 
				if (head != null && (head instanceof Thing || head instanceof Set 
						|| (head instanceof String[] && ((String[])head).length ==1 ))) { //if chained attribute
					//TODO:can't we figure out the scope more precisely based on query head contents?
					String[] scope = head instanceof Thing ? ((Thing)head).getNamesPossible() : properties;
//TODO:how to avoid this in cleaner way!!!???
if (!AL.empty(thing_names)){
	ArrayList f = new ArrayList();
	for (int i = 0 ; i < thing_names.length; i++)
		if (!AL.empty(thing_names[i]))
			f.add(thing_names[i]);
	thing_names = (String[])f.toArray(new String[]{});
}
					Object step = parseExpression(terms,parser,scope,thing_names,storager);					
					if (step == null)
						break;
					terms.add(step);
					
				} else {
					break;//TODO: fix!
				}					
			}		
			//TODO:check closing bracket
			return new Statement(mood,!negation,terms.toArray(new Object[]{})); 			
		}
		//TODO:complete and cleanup
	}

	//TODO: performance optimization	
	private Object parseExpression(ArrayList scope, Parser parser,String[] properties,String[] names,Storager storager) throws Exception {
		Object step = null;
		//TODO: cache sorting - once per parseing!?
		Array.sortByLength(properties,false);
		int type = 0;//0-unknown,1-names,2-name-values
		ArrayList items = new ArrayList();
		while (!parser.end()) {
			String name = null;
			ArrayList values = new ArrayList(); 
			String separator = null; 
			int head_position = parser.pos();
			if (scope.size() == 0 &&
				(items.isEmpty() || parser.parseAny(properties,false) == null) && //if first term or not referring to an existing property!?
				(name = parser.parseAny(names,true)) != null &&
				!name.equals(AL.name) &&
				parser.parseAny(properties,false) != null) {
				values.add(name);
				name = AL.name;
			} else {
					
				parser.set(head_position);				
				
				if ((name = parser.parseAny(properties,true)) == null) {//not property name found
					if (scope.size() == 0)//if scope not defined
						name = AL.name;
					else
						break;
				}				
				
				int value_position = parser.pos();
				
				//TODO: brackets
				for (;;) {
					String negation = parser.parseAny(not,true);
					//Object value = parser.parseNotIn(AL.punctuation);//was: single word parsing
					StringBuilder sb = null;//new StringBuilder();
					for (;;) {
						String s = null;
						//check if it is a known name
						if (AL.empty(sb) && AL.name.equals(name))
							s = parser.parseAny(names,true);
						//don't break on reserved word if it is a first word in a value sequence!!!
						if (s == null) 
							s = parser.parseNotStarting(AL.punctuation, AL.lister, sb != null ? properties : null, true );
						if (s == null)
							break;
						if (sb == null)
							sb = new StringBuilder();
						if (sb.length() > 0)
							sb.append(space);
						sb.append(s);
					}
					//handle blank strings in quotes
					Object value = sb != null ? sb.toString() : null;
					if (value == null)
						break;
					
					//try to build query chain
					if (!AL.empty(scope) && scope.get(scope.size()-1) instanceof Thing) // so it is like "Whet my knows name, trust?"
						if (storager.isThing(name)) // so it is a reference to a thing
							if (Array.contains(properties, value)) {
								parser.set(value_position);	//rollback to handle value as chained stuff			
								break;
							}
					
					//adjust context/scope
					if (negation == null && name.equals(AL.is)) {
						Collection classes = storager.getNamed((String)value);
						if (!AL.empty(classes)) {
							for (Iterator it = classes.iterator();it.hasNext();) {
								Thing cls = ((Thing)it.next());
								properties = Array.union(properties, cls.getNamesPossible());
							}
							Array.sortByLength(properties,false);
						}
					}
					
					if (negation != null) 
						value = new Ref(false,value);
					values.add(value);
					int before_separator = parser.pos();
					
					if (separator == null) {//if separator , or ; not known, pick first
						if ((separator = parser.parseAny(lister,true)) == null)
							break;
					} else {//otherwise expect for the separator starting the list
						if (!parser.parseOne(separator))
							break;									
					}
					int after_separator = parser.pos();
					
					//TODO:state machine design!?
					//if there is a successive predicate-object pair
					if (parser.parseAny(properties,true) != null) {
						if (parser.parseNotIn(AL.punctuation, AL.lister, null, false) == null)
							parser.set(after_separator);//rollback word after delimiter
						else {
							parser.set(before_separator);//rollback stolen wrong delimiter
							break;
						}
					}
				}
				
			}//if (scope.size() != 0)
				
			if (type == 0)
				type = values.size() == 0? 1 : 2;
			if (type == 1)	
				items.add(name);
			else
				items.add(new Seq(new Object[]{name,
					values.size() == 1? values.get(0): 
						separator.equals("or") ? (Set)new Any(values.toArray()) : (Set)new All(values.toArray())
					}));
			if (parser.parseAny(lister,true) == null)
				break;														
		}
		if (items.size() > 0) {
			if (type == 1) // list of attributes
				step = items.toArray(new String[]{});
			else // list of predicate-subjects
			if (items.size() == 1)
				step = items.get(0); 
			else {
				//implictly determine query type
				//if all value are the same, then it is Any
				String name = null;
				for (Iterator it = items.iterator(); it.hasNext();) {
					Seq seq = (Seq)it.next();
					if (name == null)
						name = (String)seq.get(0);
					else 
					if (!name.equals((String)seq.get(0))) {
						name = null;
						break;
					}
				}
				if (name != null)
					step = new Any(items.toArray());
				else
					step = new All(items.toArray());
			}
		}//if (items.size() > 0)
		return step;
	}
}