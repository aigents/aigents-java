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
package net.webstructor.data;

import java.util.ArrayList;
import java.util.HashSet;

import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.core.Environment;
import net.webstructor.main.Mainer;
import net.webstructor.util.Array;

/**
 * Counts:
 * English:http://norvig.com/mayzner.html
 * 
 */
public class LangPack {
	Lang[] langs;
	private Counter words = null; 
	private Counter positives = null; 
	private Counter negatives = null; 
	private HashSet<Character> chars = new HashSet<Character>();
	
	public LangPack(Environment env){
		
		langs = new Lang[]{
			new Lang("ru","russian","аеёиоуыэюя","бвгджзйклмнпрстфхцчшщ","ьъ-",new String[]{
					"почему","можно","надо","теперь", "пока", "также", "которые", "который",
					"и", "или", "в", "на", "по", "с", "для", "мы", "через", "потом", "какой",
					"точно", "еще", "он", "тоже", "ну", "допустим", "а", "что", "к", "чего",
					"как", "их", "так", "такой", "вы", "вам", "вот", "это", "где", "под", "над",
					"ни", "не", "так", "этак", "никак", "уж", "пор", "тех", "этих", "аж",
					"бы", "да", "не", "но", "только", "уже", "этим", "эту", "либо", "нибудь",
					"о", "об", "про", "ли", "во", "кто", "тут", "то", "того", "этого", "этот",
					"я", "мне", "меня", "мой", "мне", "мои", "ты", "твой", "твои", "тебе",
					"вас", "вам", "вами", "при", "за", "раз", "сих",
					"тебя", "тебе", "тобой", "она", "ей", "ней", "ту", "тогда",
					"ее", "её", "ею", "ей", "тем", "чем", "самым", "им", "ими",
					"те", "тот", "эти", "этот", "том", "там", "по", "после",
					"то", "это", "та", "эта", "когда", "таких", "этаких",
					"его", "ему", "они", "им", "их", "ним", "него", "кому", "тому",
					"итд", "итп", "хоть", "хотя", "же", "от", "до", "чтобы", "из", "у", "без", 
					"эх", "ух", "ох", "все", "что-то", "нет", "да", "того", "его",
					"чо", "если", "нас", "нам", "нами", "какие", "каких"
					}),
			new Lang("en","english","aeiou","bcdfghjklmnpqrstvwxyz","-",new String[]{
					"-", "&","a", "an", "and", "because", "else", "or", "the", "in", "on", "at", "it", "is", "after", "are", "me",
					"am", "i", "into", "its", "same", "with", "if", "most", "so", "thus", "hence", "how",
					"as", "do", "what", "for", "to", "of", "over", "be", "will", "was", "were", "here", "there",
					"you", "your", "our", "my", "her", "his", "just", "have", "but", "not", "that",
					"their", "we", "by", "any", "anything", "some", "something", "dont", "do", "does", "of", "they", "them",
					"been", "even", "etc", "this", "that", "those", "these", "from", "he", "she",
					"no", "yes", "own", "may", "mine", "me", "each", "can", "could", "would", "should", "since", "had", "has",
					"when", "out", "also", "only", "about", "us", "via", "than", "then", "up", "who", "which"
					})};
		loadLexicon(env);
	}
	//TODO: unify '-' and '&' as either scrub, special and splitters!!!
	
	//TODO: move long dash ('—') to some good place
	//public static final String splitters = AL.spaces+"—";

	public Counter words(){
		return words;
	}
	
	public java.util.Set chars(){
		return chars;
	}
	
	public String getName(String name){
		if (AL.empty(name))
			return null;
		//for (int i = 0; i < langs.length; i++)//try by full name
		//	if (langs[i].prefix.contentEquals(name))
		//		return langs[i].name;
		name = name.toLowerCase();
		for (int i = 0; i < langs.length; i++)//try by prefix
			if (name.startsWith(langs[i].prefix))
				return langs[i].name;
		return null;
	}
	
	Counter loadCounter(Environment env, Counter counter, String type, String language){
		//String path = "lexicon_"+langs[l].name+".txt";
		String path = type+"_"+language+".txt";
		Counter c = new Counter(env,path,"[\t]",new Integer(1));//load counter from file
		if (!AL.empty(c)){
			c.normalize();//normalize counter to [1..100]
			if (AL.empty(counter))
				counter = c;
			else
				counter.mergeMax(c);//TODO: mergeSum?
		}
		return counter;
	}

	void setChars(String chars) {
		for (int i = 0 ; i < chars.length(); i++)
			this.chars.add(chars.charAt(i));
	}
	
	boolean validChars(String chars) {
		//TODO checke languages individually!?
		for (int i = 0 ; i < chars.length(); i++)
			if (!this.chars.contains(chars.charAt(i)))
					return false;
		return true;
	}
	
	public boolean validWord(String word) {
		if (words.containsKey(word))
			return true;
		return validChars(word);
	}
	
	void loadLexicon(Environment env){
		for (int l = 0; l < langs.length; l++){
			/*
			String path = "lexicon_"+langs[l].name+".txt";
			Counter c = new Counter(env,path);//load counter from file
			if (!AL.empty(c)){
				c.normalize();//normalize counter to [1..100]
				if (AL.empty(words))
					words = c;
				else
					words.mergeMax(c);//TODO: mergeSum?
			}
			*/
			words = loadCounter(env, words, "lexicon", langs[l].name);
			positives = loadCounter(env, positives, "lexicon_positive", langs[l].name);
			negatives = loadCounter(env, negatives, "lexicon_negative", langs[l].name);
			setChars(langs[l].vowels);
			setChars(langs[l].consonants);
			setChars(langs[l].spec);
		}
	}
	
	//TODO: to other place 
	public String obfuscate(String s) {
		//TODO: obfuscate name for demo
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++){
			char ch = s.charAt(i);
			char cl = Character.toLowerCase(ch);
			char r = 0;
			for (int l = 0; l < langs.length; l++){
				r = langs[l].obfuscate(cl);
				if (r != 0)
					break;
			}
			sb.append(r == 0? ch : ch == cl ? r : Character.toUpperCase(r));
		}
		return sb.toString();
	}

	//TODO: separate 'emotional' scrubs (like "most" or "if") for separate use
	public boolean scrub(String s) {
		//TODO: make min word length language-specific to support Chinese
		if (s.length() <= 1)
			return true;
		//TODO: move dash check to parser or replace dashes with scrubsymbols?
		if (Array.containsOnly(s, AL.dashes))
			return true;
//TODO: hashtable
		for (int l = 0; l < langs.length; l++)
			if (Array.contains(langs[l].scrubs,s))
				return true;
		return false;
	}
	
	//TODO: to Array/Str utils
	public static String trim(String s) {
		int start = 0, end = s.length();
		while (start < end && AL.trimmers.indexOf(s.charAt(start)) != -1)
			start++;
		while (start < end && AL.trimmers.indexOf(s.charAt(end - 1)) != -1)
			end--;
		return start == 0 && end == s.length() ? s : start > end ? "" : s.substring(start, end);
	}
	
	/**
	 * @param s string to trim
	 * @return non-null string, possibly empty
	 */
	public String lowertrim(String s) {
		StringBuilder sb = new StringBuilder();
		int tail = s.length() - 1;
		for (int i = 0; i <= tail; i++){
			char cl = Character.toLowerCase(s.charAt(i));
			for (int l = 0; l < langs.length; l++){
				if (langs[l].has(cl)){
					sb.append(cl);
					break;
				}
			}
		}
		return trim(sb.toString());
	}

	public static boolean excluded(String word, int min, LangPack languages) {
		if (word.length() < min)
			return true;
		/*if (exclusions != null && exclusions.contains(word))
			return true;*/
		if (languages != null && languages.scrub(word))
			return true;
		return false;
	}
	
	public static void countWords(LangPack languages, Linker linker, String text, java.util.Set vocabulary) {
		countWords(languages, linker, text, vocabulary, 2, false);
	}
	
	public static void countWords(LangPack languages, Linker linker, String text, java.util.Set vocabulary, int min, boolean number) {
		//Set tokens = Parser.parse(text,AL.punctuation+AL.spaces,false,true,false,true);//no quoting
		Set tokens = Parser.parse(text,AL.punctuation+AL.spaces,false,true,true,true);//quoting
		if (tokens != null) {
			for (int j = 0; j < tokens.size(); j++){
				String token = (String)tokens.get(j); 
				if (AL.isURL(token))
					continue;
			
				String word;
				if (languages == null)
					word = token;
				else {
					word = languages.lowertrim(token);
					if (AL.empty(word)) try {
						Integer.parseInt(token);
						word = token;
					} catch (NumberFormatException e) {}
				}
				
				//TODO: use real freqs for scrubs, because some of them may be used in patterns
				if (vocabulary != null){//inclusion based on "best words"
					if (!vocabulary.contains(word))
						continue;
				} else {//inclusion based on "scrub list" fo exclusions
					if (excluded(word,min,languages))
						continue;
				}
				linker.count(word);
			}
		}
	}

	String buildNGram(Seq seq, int pos, int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			if (sb.length() > 0)
				sb.append(' ');
			Object w = seq.get(pos + i);
			if (w == null)//faced consumed word
				return null;
			sb.append(seq.get(pos + i));
		}
		return sb.toString();
	}
	
	Seq buildNGrams(Seq seq, int N) {
		if (N < 2)
			return seq;
		int size = seq.size() - N + 1;
		if (size < 1)
			return null;
		Object[] items = new Object[size];
		for (int i = 0; i < size; i++) {
			items[i] = buildNGram(seq, i, N);
		}
		return new Seq(items);
	}
	boolean sentiment_logarithmic = false;
	boolean sentiment_maximized = true;
	public int[] sentiment(String input, ArrayList pc, ArrayList nc) {
		if (AL.empty(input) || AL.empty(positives) || AL.empty(negatives))
			return new int[] {0,0,0};
		Seq seq = Parser.parse(input);
		double p = 0;
		double n = 0;
		//double c = 0;
		for (int N = 3; N >=1; N--) {//iterate N of N-grams
			Seq seqNgrams = buildNGrams(seq, N);
			if (!AL.empty(seqNgrams)) for (int i = 0; i < seqNgrams.size();) {
				String w = (String)seqNgrams.get(i);//some may be null seing consumed earlier
				if (w == null || scrub(w)) {
					i++;
					continue;
				}
				//c += N;//weighted
				boolean found = false; 
				if (positives.get(w) != null) {
					p += N;//weighted
					if (pc != null)
						pc.add(w);
					found = true;
				} else
				if (negatives.get(w) != null) {
					n += N;//weighted
					if (nc != null)
						nc.add(w);
					found = true;
				}
				if (found) {
					for (int Ni = 0; Ni < N; Ni++)
						seq.set(i + Ni, null);
					i += N;
				} else
					i++;
			}
		}
		if (sentiment_logarithmic) {
			p = Math.log10(1 + 100 * p / seq.size())/2;
			n = Math.log10(1 + 100 * n / seq.size())/2;
		}else {
			p = p / seq.size();
			n = n / seq.size();
		}
		if (sentiment_maximized) {
			double max = Math.max(p, n);
			return new int[] {(int)Math.round(p*100), (int)Math.round(n*100), (int)Math.round((p - n)*100/max)};
		} else {
			return new int[] {(int)Math.round(p*100), (int)Math.round(n*100), (int)Math.round((p - n)*100)};
		}
	}
	public int[] sentiment(String input) {
		return sentiment(input, null, null);
	}
	
	
	//TODO: this properly (now it is just a hack)
	//TODO: use either MapMap from Aigents Core, or Properties from SpaceWork, or Locale Maps from Aigents UI
	//TODO: ideally, this should be configurable with language-specific self properties (its phraseo-lexicon)
	public String translate(String language, String origin) {
		if (language != null && language.toLowerCase().startsWith("ru"))
			return "Добро пожаловать к Агентам Эй-Айжентс! Вы можете смотреть и редактировать темы своих интересов и шаблоны поиска новостей в разделе \"Темы\", сайты для наблюдения в разделе \"Сайты\" и затем получать новую информацию в разделе \"Новости\". Также, можно будет управлять своими контактами в разделе \"Друзья\" и болтать со своим Агентом (на упрощенном английском) в разделе \"Чат\". Для регистрации и входа через соцсети и получения персональных отчетов - используйте кнопки справа вверху!";
		else
			return "Welcome to Aigents! Now you can view and edit topics of your interests and text patterns for them in \"Topics\" view, web sites to watch that in \"Sites\" view and monitor your news in \"News\" view. Also, you can manage contacts of your freinds and colleagues in \"Friends\" view and chat (in simplified English) with your Aigent in \"Chat\" view. To register and login via social networks and get your personal reports - use buttons in the top-right corner!";
	}
	
	public static void sentiment_test(LangPack lp,String text){
		ArrayList p = new ArrayList();
		ArrayList n = new ArrayList();
		int[] s = lp.sentiment(text,n,p);
		System.out.format("%s %s %s %s %s %s\n",s[2],s[0],s[1],text,p,n);
	}

	public static void main(String args[]){
		LangPack lp = new LangPack(new Mainer()); 

		for (int i = 0; i < 4; i++) {
			lp.sentiment_logarithmic = i == 0 || i == 1;
			lp.sentiment_maximized = i == 0 || i == 2;
			System.out.format("---- maximised=%s logarithmic=%s -----------\n",lp.sentiment_maximized,lp.sentiment_logarithmic);
			sentiment_test(lp,"you are good man");
			sentiment_test(lp,"you are good man in good company");
			sentiment_test(lp,"you are bad man");
			sentiment_test(lp,"you are bad man in good company");
			sentiment_test(lp,"you are nice good man in bad company");
			sentiment_test(lp,"you are good man in sadly bad company");
			sentiment_test(lp,"ты хороший");
			sentiment_test(lp,"ты хороший и милый");
			sentiment_test(lp,"ты негодяй");
			sentiment_test(lp,"ты милый негодяй");
			sentiment_test(lp,"ты хороший и милый негодяй");
			sentiment_test(lp,"ты хороший подлец и негодяй");
			sentiment_test(lp,"going up");
			sentiment_test(lp,"going down");
			sentiment_test(lp,"good fine excellent");
			sentiment_test(lp,"bad horrible awful");
			sentiment_test(lp,"so we shut up the country cause fauci and birx told trump that up to 2.2 million people will die");
		}
	}
}

