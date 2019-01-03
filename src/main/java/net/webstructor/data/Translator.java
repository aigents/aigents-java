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

import java.util.HashMap;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.al.Writer;

public class Translator {

	private static HashMap translators;
	static {
		translators = new HashMap();

		HashMap r = new HashMap();
		r.put("activity","активность");
		r.put("all","все");
		r.put("report","отчет");
		r.put("for","для");
		r.put("rank","ранг");
		r.put("contragent","контрагент");
		r.put("contragents","контрагенты");
		r.put("paid","мои платежи");
		r.put("pays","платежи");
		r.put("calls","обращения");
		r.put("friend","друг");	
		r.put("connections","связи");	
		r.put("crosses","пересечений");
		r.put("my likes","мои одобрения");
		r.put("friend likes","одобряют друзья");
		r.put("comments","комментарии");
		r.put("karma","карма");	
		r.put("words","слова");
		r.put("period","период");
		r.put("popularity","популярность");
		r.put("significance","значимость");
		r.put("words","слова");
		r.put("word","слово");
		r.put("date","дата");
		r.put("friends","друзья");
		r.put("text","текст");
		r.put("links","ссылки");
		r.put("posts","публикации");
		r.put("count","число");
		r.put("occurences","упоминаний");
		r.put("likes","одобрения");
		r.put("topics","темы");
		r.put("features","особенности");
		r.put("my interests","мои интересы");
		r.put("interests of my friends","интересы моих друзей");
		r.put("words liked by me","слова, что нравятся мне");
		r.put("my best words","мои лучшие слова");
		r.put("similar to me","похожи на меня");		
		r.put("best friends","лучшие друзья");		
		r.put("fans","последователи");		
		r.put("authorities","авторитеты");	
		r.put("number of friends", "число друзей");
		r.put("number of posts", "число публикаций");
		r.put("my words liked and commented","мои слова - одобряемые и комментируемые");
		r.put("like and comment me","одобряют и комментируют меня");
		r.put("liked by me","одобряю я");
		r.put("global","глобальная");
		r.put("attention got","получено внимания");
		r.put("attention spent","потрачено внимания");
		r.put("my karma by periods","моя карма за периоды");
		r.put("my words by periods","мои слова за периоды");
		r.put("my friends by periods","мои друзья за периоды");
		r.put("my posts liked and commented","мои публикации - одобряемые и комментируемые");
		r.put("my posts for the period","мои публикации за период");
		r.put("my favorite words", "мои излюбленные слова");
		r.put("words of mine and friend", "слова мои и друга");
		r.put("All likes & comments/Posts", "Все одобрения и комментарии/Публикации");
		r.put("Likes & comments/Posts", "Одобрения и комментарии/Публикации");
		r.put("My likes/Posts","Мои одобрения/Публикации");
		r.put("Aigents Report for","Отчет Aigents для");
		
		translators.put("russian", new Translator(r));
		translators.put("english", new Translator(null));
	}
	
	public static Translator get(String language){
		Translator t = language == null ? null : (Translator)translators.get(language);
		return t != null ? t : (Translator)translators.get("english");
	}
	
	private Map map;
	public Translator(Map map){
		this.map = map;
	}
		
	public String eng(String src){
		//TODO:split to spaces and delimiters, translate each word capitalized if needed
		//TODO:reverse translation, having the map inverted in constructor
		return src;
	}

	protected static String translateCapital(String src, String dest){
		return Character.isUpperCase(src.charAt(0)) ? Writer.capitalize(dest) : dest;
	}
	
	protected static String translateCase(String src, String dest){
		boolean[] caps = null;
		for (int i = 0; i < src.length(); i++){
			boolean isUpper = Character.isUpperCase(src.charAt(i));
			if (isUpper){
				if (caps == null)
					caps = new boolean[src.length()];
				caps[i] = true;
 			}else{
 				if (caps != null)
 					caps[i] = false;
 			}
		}
		if (caps != null){
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < dest.length(); i++){
				char c = dest.charAt(i);
				if (i < caps.length && caps[i])
					c = Character.toUpperCase(c);
				sb.append(c);
			}
			return sb.toString();
		}
		return dest;
	}
	
	private String toLoc(String src){
		String loc = (String)map.get(src.toLowerCase());
		return loc == null ? src : translateCase(src,loc);
 	}
	
	public String loc(String src){
		if (src == null || map == null)
			return src;
		String asis = (String)map.get(src);
		if (asis != null)
			return asis;
		String entire = (String)map.get(src.toLowerCase());
		if (entire != null)
			return translateCapital(src,entire);
		StringBuilder sb = new StringBuilder();
		int start = 0;
		int i = 0;
		for (; i < src.length(); i++){
			char c = src.charAt(i);
			if (!Character.isLetter(c)){
				if (i > start)
					sb.append(toLoc(src.substring(start,i)));
				sb.append(c);
				start = i + 1;
			}
		}	
		if (i > start)
			sb.append(toLoc(src.substring(start,i)));
		return sb.toString();
	}
	
	public String[] eng(String[] loc){
		if (!AL.empty(loc) && map != null){
			String[] eng = new String[loc.length];
			for (int i = 0; i < loc.length; i++)
				eng[i] = eng(loc[i]);
			return eng;
		}
		return loc;
	}
	
	public String[] loc(String[] eng){
		if (!AL.empty(eng) && map != null){
			String[] loc = new String[eng.length];
			for (int i = 0; i < eng.length; i++)
				loc[i] = loc(eng[i]);
			return loc;
		}
		return eng;
	}
	
	public static void main(String[] args){
		Translator t = Translator.get("russian");
		String[] s = t.loc(new String[]{"Friend","Id","Karma,%","Attention gifted","Attention got"});
		System.out.println(s[2]);
		System.out.println(s[3]);
		System.out.println(s[4]);
		System.out.println(t.loc("Karma,%"));
		System.out.println(t.loc("Attention gifted"));
		System.out.println(t.loc("Attention got"));
		System.out.println(t.loc("Rank,%"));
		System.out.println(t.loc("%Friend/ranK?"));
		System.out.println(t.loc("FRiend of my rAnk"));
		System.out.println(t.loc("%friend/rank?"));
		System.out.println(t.loc("friend of my rank"));
		System.out.println(t.loc("friend of my rank"));
		System.out.println(t.loc("My interests"));
	}
}

