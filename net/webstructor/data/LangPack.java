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
package net.webstructor.data;

import net.webstructor.al.AL;
import net.webstructor.util.Array;

/**
 * Counts:
 * English:http://norvig.com/mayzner.html
 * 
 */
public class LangPack {
	Lang[] langs;
	private Counter words = null; 
	
	public LangPack(){
		
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
					"-", "&","a", "an", "and", "or", "the", "in", "on", "at", "it", "is", "are", "me",
					"am", "i", "into", "its", "same", "with", "if", "most", "so", "thus", "hence", "how",
					"as", "do", "what", "for", "to", "of", "be", "will", "was", "were", "here", "there",
					"you", "your", "our", "my", "her", "his", "just", "have", "but", "not", "that",
					"their", "we", "by", "any", "some", "dont", "do", "does", "of", "they", "them",
					"been", "even", "etc", "this", "that", "those", "these", "from", "he", "she",
					"no", "yes", "own", "mine", "me", "each", "can", "could", "would", "should", "had", "has",
					"when", "out", "also", "only", "about", "us", "via", "then", "who"
					})};
		loadLexicon();
	}
	//TODO: unify '-' and '&' as either scrub, special and splitters!!!
	
	//TODO: move long dash ('—') to some good place
	//public static final String splitters = AL.spaces+"—";

	public Counter words(){
		return words;
	}
	
	public Lang get(String name){
		if (AL.empty(name))
			return null;
		for (int i = 0; i < langs.length; i++)
			if (langs[i].prefix.contentEquals(name) || langs[i].prefix.contentEquals(name))
				return langs[i];
		return null;
	}
	
	void loadLexicon(){
		for (int l = 0; l < langs.length; l++){
			String path = "lexicon_"+langs[l].name+".txt";
			Counter c = new Counter(path);//load counter from file
			if (!AL.empty(c)){
				c.normalize();//normalize counter to [1..100]
				if (AL.empty(words))
					words = c;
				else
					words.mergeMax(c);//TODO: mergeSum?
			}
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
	
	//TODO: this properly (now it is just a hack)
	//TODO: use either MapMap from Aigents Core, or Properties from SpaceWork, or Locale Maps from Aigents UI
	//TODO: ideally, this should be configurable with language-specific self properties (its phraseo-lexicon)
	public String translate(String language, String origin) {
		if (language != null && language.toLowerCase().startsWith("ru"))
			return "Добро пожаловать к Агентам Эй-Айжентс! Вы можете смотреть и редактировать темы своих интересов и шаблоны поиска новостей в разделе \"Темы\", сайты для наблюдения в разделе \"Сайты\" и затем получать новую информацию в разделе \"Новости\". Также, можно будет управлять своими контактами в разделе \"Друзья\" и болтать со своим Агентом (на упрощенном английском) в разделе \"Чат\". Для регистрации и входа через соцсети и получения персональных отчетов - используйте кнопки справа вверху!";
		else
			return "Welcome to Aigents! Now you can view and edit topics of your interests and text patterns for them in \"Topics\" view, web sites to watch that in \"Sites\" view and monitor your news in \"News\" view. Also, you can manage contacts of your freinds and colleagues in \"Friends\" view and chat (in simplified English) with your Aigent in \"Chat\" view. To register and login via social networks and get your personal reports - use buttons in the top-right corner!";
	}
	
	public static void main(String args[]){
		System.out.println(trim("-a-"));
		System.out.println(trim("-aa-"));
		System.out.println(trim("-a"));
		System.out.println(trim("-aa"));
		System.out.println(trim("a-"));
		System.out.println(trim("aa-"));
		System.out.println(trim("-a-a-"));
		System.out.println(trim("--a-a--"));
		System.out.println(trim("--a-a--"));
	}
}

