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
package net.webstructor.peer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.al.Set;
import net.webstructor.al.Writer;
import net.webstructor.core.Environment;
import net.webstructor.core.Mistake;
import net.webstructor.core.Storager;
import net.webstructor.core.Thing;
import net.webstructor.data.Counter;
import net.webstructor.data.LangPack;
import net.webstructor.main.Mainer;
import net.webstructor.main.Tester;

class Answerer extends Searcher {	

	@Override
	public String name() {
		return "answer";
	}
	
	@Override
	public boolean handleIntent(final Session session){
		try {

		String query = session.input();
		SearchContext sc = new SearchContext(query, session.getPeer(), "any",1);
		session.getPeer();
		Collection res = null;
		if (AL.empty(res))
			res = searchSTM(session, sc);
		if (AL.empty(res))
			res = searchLTM(session, sc);//it creates persistent objects!!!???
		
		if (AL.empty(res)) {
			sc.days = session.getBody().self().getInt(Body.attention_period, 10);
			res = searchSTMwords(session, sc);
		}
		
		if (AL.empty(res) && sc.peer != null && Peer.paid(sc.peer))
			res = searchEngine(session, sc);

		if (!AL.empty(res)) {
			Thing t = (Thing)res.iterator().next();
			String text = t.getString(AL.text);
			String source = t.getString(AL.sources);
			if (!AL.empty(source))
				text += ' ' + source;
			session.outputWithEmotions(text);
		} else
			//session.output(session.no());
			return false;

		} catch (Throwable e) {
			session.output(session.no()+" "+Responser.statement(e));
			if (!(e instanceof Mistake))
				session.sessioner.body.error("Answerer error " + e.toString(), e);
		}
		return true;
	}

	Collection searchSTMwords(Session session, final SearchContext sc) {
		/*
 		- Run the following for every searcher, until non-empty response is given
			- Tokenize list of words
			- Weight the words with their surprisingness (1/count)
			- Find the text most relevant to the question
				- Count like in Searcher?
				- Iterate over weighted surprising wors
					- Iterate over texts with every word 
					- "count" every text matching every word using surprisingness as a double (!!!) count value 
					- if no texts found, keep iterating with less surprising word
					- if one text is found - return it
					- if more than one text is found, select the most counted
				- Have the above as a function and compare performance 
			- If more than one text, compute the dual relevance
				- maintain Summator dialogInputSTM, dialogOutputSTM in as session with decay "dialog decay" ... of the older terms 
				- The most relevant to the context of the outer input in Session's Summator dialogInputSTM
				- The least relevant to the context of the innet output in Session's Summator dialogOutputSTM
		 */
		LangPack languages = session.getBody().languages;
		Counter words = new Counter();
		LangPack.countWords(languages, words, sc.topic, null);
		words.normalizeBy(languages.words(), 1);
		if (words.size() < 1)
			return null;
		try {
			Storager storager = session.getStorager();
			java.util.Set texts = storager.getValuesSet(AL.text);
//TODO eliminate full text search with index search on daily updated graph!? 
			if (!AL.empty(texts)){
				Collection res = new ArrayList();
				//iterate over collection of texts, count all matches
				HashMap<Thing,Counter> textMatches = new HashMap<Thing,Counter>();
				int maxMatches = 0;
				for (Iterator it = texts.iterator(); it.hasNext();){
					String text = (String)it.next();
					Collection things = storager.getByName(AL.text, text);
					if (!AL.empty(things)) {	
						Thing thing = (Thing)things.iterator().next();
						Set tokens = Parser.parse(text,AL.punctuation+AL.spaces,false,true,false,true);
						Counter textWords = new Counter(); 
						for (int i = 0; i < tokens.size(); i++) {
							String token = (String)tokens.get(i);
							Number n = words.value(token);
							if (n != null)
								textWords.count(token, n.doubleValue()); 
						}
						if (maxMatches > textWords.size())
							continue;
						if (maxMatches < textWords.size()) {
							textMatches.clear();
							maxMatches = textWords.size();
						}
						textMatches.put(thing, textWords);
					}
				}
				if (maxMatches > 0) {// && textMatches.size() > 0) {
					for (Thing t : textMatches.keySet()) {
						Counter textWords = textMatches.get(t);
						{//if (maxMatches == textWords.size()) {
							Thing found = new Thing();
//TODO: find fragments - build summary
							String text = t.getString(AL.text);
							String summary = summarize(textWords.keySet(),text,languages);
							String src = t.getString(AL.sources);
							if (AL.empty(src))
								src = t.getString(AL.is);
							if (AL.empty(summary)) {
								session.sessioner.body.error("Answerer no summary "+words+" "+src+" "+text, null);
								continue;
							}
							found.setString(AL.text, summary);
							found.setString(AL.sources, src);
							res.add(found);
						}
					}
				}
				if (!AL.empty(res))
					return res;
			}
		} catch (Throwable e) {
			session.sessioner.body.error("Answerer error "+session.input(), e);
		}
		return null;
	}
	
//TODO sentence wise summarizer skipping "irrelevant" sentences with "..."		
	public static String summarize(java.util.Set words, String text, LangPack languages) {
		return summarizeAsAWhole(words, text, languages);
		/*
//TODO: split preserving delimiters!?
		String sentences[] = Parser.split(text, ".?!");
		if (AL.empty(sentences))
			return summarizeAsAWhole(words, text, languages);
		StringBuilder sb = new StringBuilder();
		int matches[] = new int[sentences.length];
		int max = 0;
		for (int i = 0; i < sentences.length; i++) {
			Set tokens = Parser.parse(sentences[i]);
			for (int j = 0; j < tokens.size(); j++)
				if (words.contains(tokens.get(j)))
					matches[i]++;
			if (max < matches[i])
				max = matches[i];
		}
		if (max == 0)
			return null;
		int last_i = -1;
		for (int i = 0; i < sentences.length; i++) {
			String sentence = sentences[i];
			if (matches[i] == max) {
				sb.append( sb.length() > 0 && (i - last_i) > 1 ? " ... " : " ").append(sentence);
				last_i = i;
			}
		}
		
		return sb.toString();
		*/
	}

	public static String summarize(java.util.Set words, String text) {
		return summarize(words, text, null);
	}
	public static String summarizeAsAWhole(java.util.Set words, String text, LangPack languages) {
		//IDEA A:
		//1) create list of "seeds" for every postions of "words", with map position->missedSeeds
		//2) start expanding every seed to right and left over tokens till any seed has zero missedSeeds
		//3) get the first seeds with zero missedSeeds and expand it to left and right sentence breaks
		//4) glue up the results into text

		//IDEA B
		//1) compute multiplicative heat map based on distribution of n_words matched words over n_tokens;
//TODO We don't deal with quotes, should we do!?
		//Set tokens = Parser.parse(text);
		Set tokens = Parser.parse(text,(String)null,false,true,false,true,null,(List)null);//quoting=false,urling=true
		double total_heats[] = null;
		int n_tokens = tokens.size();
		int n_words = 0;//words.size();
		for (Object w : words) {
			boolean heated = false;
			double sub_heats[] = new double[tokens.size()];
			int i;
			for (i = 0; i < n_tokens; i++)
				sub_heats[i] = 0;
			for (i = 0; i < n_tokens; i++) {//check every token
				String token = (String)tokens.get(i);
//TODO: consider levenstain difference for measure
				if (token.equals(w)) {
					heated = true; 
					sub_heats[i] = 1;
					for (int iminus = 1; i - iminus >= 0; iminus++)
						sub_heats[i - iminus] += 1.0 / (1 + iminus);
					for (int iplus = 1; i + iplus < n_tokens; iplus++)
						sub_heats[i + iplus] += 1.0 / (1 + iplus);
				}
			}
			if (heated) {
				n_words++;
				if (total_heats == null)
					total_heats = sub_heats;
				else
					for (int h = 0; h < n_tokens; h++)
						total_heats[h] *= sub_heats[h];
			}
		}
		if (n_words == 0)
			return null;
		//2) find the first greatest hot spot
		int start = -1; 
		int end = -1;
		double max = 0;
		for (int i = 0; i < n_tokens; i++) {
			if (max < total_heats[i]) {
				max = total_heats[i];
				start = i;
			}
		}
		end = start; 
		//3) find n_words-1 hottest spots nearby the greatest hot spot
		int new_start = start;
		int new_end = end;
		for (int found = 1; found < n_words; ) {
			//try left
			if (new_start > 0) {
				new_start--;
				//searching for extremum on the left 
				if (total_heats[new_start] >= total_heats[new_start + 1] && (new_start - 1 < 0 || total_heats[new_start] >= total_heats[new_start - 1])) {
					if (++found == n_words) {
						start = new_start;
						break;
					}
				}
			}
			//try right
			if (new_end < n_tokens - 1) {
				new_end++;
				//searching for extremum on the right 
				if (total_heats[new_end] >= total_heats[new_end - 1] && (new_end + 1 >= n_tokens || total_heats[new_end] >= total_heats[new_end + 1])) {
					if (++found == n_words) {
						end = new_end;
						break;
					}
				}
			}
			if (new_start == 0 && new_end == n_tokens - 1)
				break;
		}
		
		//4) check if we are language-agnostic 
		boolean specific_language = false;
		if (languages != null) for (Object word : words)
			if (languages.words().containsKey(word)) {
				specific_language = true;
				break;
			}
		
		//5) well-form the sentence
		for (; start > 0; start--) {
			if (AL.periods.indexOf((String)tokens.get(start - 1)) != -1)//token before the first one is period-kind
				break;
//TODO: check for specific language unsupported chars and allowing unfamiliar words using these chars like "coronavirus"
			if (specific_language && start > 0 && !languages.validWord((String)tokens.get(start - 1)))//previous token is out-of-dictionary
				break;
		}
		for (; end < n_tokens - 1; end++) {
			if (AL.periods.indexOf((String)tokens.get(end)) != -1)//last token is period-kind
				break;
			if (specific_language && end < n_tokens - 2 && !languages.validWord((String)tokens.get(end)))//next token is out-of-dictionary
				break;
				
		}
		//6) glue things up
		StringBuilder sb = new StringBuilder();
		boolean capitalize = true;
		for (int i = start; i <= end; i++) {
			String t = (String)tokens.get(i);
			boolean delimiter = t.length() == 1 && AL.delimiters.indexOf(t) != -1;
			if (i != start && !delimiter)
				sb.append(" ");
			sb.append(capitalize ? Writer.capitalize(t) : t);
			capitalize = t.length() == 1 && AL.periods.indexOf(t) != -1;
		}
		return sb.toString();
	}

	public static void main(String args[]) {
		Environment env = new Mainer();
		LangPack languages = new LangPack(env);
		Tester t = new Tester();
		java.util.Set w = new java.util.HashSet();
		String s;
/**/
		//basic test
		w.add("aliens");
		w.add("homeland");
		s = summarize(w,"The universe is the homeland of aliens who came to earth.");
		t.assume(s, "The universe is the homeland of aliens who came to earth.");
		s = summarize(w,"Here we go. The universe is the homeland of aliens who came to earth. Here we are.");
		t.assume(s, "The universe is the homeland of aliens who came to earth.");
		s = summarize(w,"Here is the universe. The universe is the homeland of aliens who came to earth. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of aliens who came to earth.");
		s = summarize(w,"Here are the aliens. Here is their homeland. Here you go.");
		t.assume(s, "Here are the aliens. Here is their homeland.");
		s = summarize(w,"Everyone has homeland. The universe is the homeland of aliens. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of aliens.");
		s = summarize(w,"Here are the humans. Here is their homeland. Here you go.");
		t.assume(s, "Here is their homeland.");
		s = summarize(w,"Here are the humans. Here is their home. Here you go.");
		t.assume(s, null);
		s = summarize(w,"Here are the alients. Here is their homeland. Here you go.");//TODO: fuzzy word matching with bigrams or levenstain distance!?
		t.assume(s, "Here is their homeland.");
		s = summarize(w,"Here is the home of animals. Here is the home of aliens. That's it.");
		t.assume(s, "Here is the home of aliens.");
		s = summarize(w,"The universe is the homeland of life. Like aliens, for instance. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of life. Like aliens, for instance.");
		s = summarize(w,"The universe is the homeland of life. Aliens, for instance. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of life. Aliens, for instance.");
		s = summarize(w,"We live in the universe. The universe is the homeland of the alien forms of life. These are called aliens. The aliens are our friends.");
		t.assume(s, "The universe is the homeland of the alien forms of life. These are called aliens.");
		
		//test various kinds of periods
		w.clear();
		w.add("россию");
		w.add("душат");
		s = summarize(w,"246 россию душат столетиями! Соловьев в шоке? Резонансное выступление жириновского.");
		t.assume(s, "246 россию душат столетиями!");
		s = summarize(w,"Соловьев в шоке! россию все столетиями душат? Резонансное выступление жириновского.");
		t.assume(s, "Россию все столетиями душат?");
		
		//test non-periodic segmantation
		w.clear();
		w.add("искусственный");
		w.add("интеллект");
		s = summarize(w,"02.05.2020 в якутии выявили сразу 3 тысячи заражённых коронавирусом вахтовиков 02.05.2020 сегодня искусственный интеллект помогает определить наличие коронавируса по рентгеновскому снимку 02.05.2020 выкарабкалась из страшной ситуации : победившая коронавирус надежда бабкина обратилась к поклонникам 02.05.2020",languages);
		t.assume(s, "Сегодня искусственный интеллект помогает определить наличие коронавируса по рентгеновскому снимку 02.05.2020");
		w.clear();
		w.add("хитрая");
		w.add("куздра");
		s = summarize(w,"02.05.2020 в якутии выявили сразу 3 тысячи заражённых коронавирусом вахтовиков 02.05.2020 зрябко чавкая, глобкая хитрая куздра курдячит хлипкого бакренка 02.05.2020 выкарабкалась из страшной ситуации : победившая коронавирус надежда бабкина обратилась к поклонникам 02.05.2020",languages);
		t.assume(s, "Глобкая хитрая куздра курдячит хлипкого бакренка 02.05.2020");

		w.clear();
		w.add("artificial");
		w.add("intelligence");
		w.add("disaster");
		s = summarize(w,"artificial intelligence disaster");
		t.assume(s, "Artificial intelligence disaster");
		s = summarize(w,"dogs eat meat . artificial general intelligence . internet of things . storage it's not a windows 10 release without disaster . tuna is a fish");
		t.assume(s, "Artificial general intelligence. Internet of things. Storage it's not a windows 10 release without disaster.");
/**/
		
		
//TODO:
		w.clear();
		w.add("artificial");
		w.add("intelligence");
//w.add("drags");
		//s = summarize(w,"Artificial intelligence. Internet of things.. Sec drags silicon valley ai upstart to court over claims of made-up revenues, investors swindled out of $11m. Uk formally abandons europe's unified patent court, germany plans to move forward nevertheless. World health organisation ai chatbot goes deaf when asked for the latest covid-19 figures for taiwan, hong kong. Germany bans tesla from claiming its autopilot software is potentially autonomous. Verity stob.. Incredible artifact – or vital component after civilization ends? Rare nazi enigma m4 box sells for £350,000. From queen of the skies to queen of the scrapheap: british airways chops 747 fleet as folk stay at home. See you after the commercial breakdown: cert expiry error message more entertaining than the usual advert tripe. Motorbike ride-share app ceo taken to pieces in grisly new york dismemberment. Security. You've had your pandemic holiday, now microsoft really is going to kill off tls 1.0, 1.1. Plus: skype plays catch-up, barracuda goes azure, and winui slings another preview. Mon 20 jul 2020 // 15:23 utc 7 got tips? Richard speed bio email twitter share copy. In brief having issued an all-too-brief stay of execution on the decidedly whiffy transport layer security ( tls ) 1.0 and 1.1 protocols in microsoft 365, the windows giant has announced that deprecation enforcement will kick off again from 15 october... The protocols were actually deprecated back in 2018 but microsoft halted enforcement earlier this year, recognising that it departments had quite a bit of unexpected work on their hands thanks to the covid-19 pandemic. Now, as supply chains have adjusted and certain countries open back up, a visit from microsoft's axeman is due... There has been plenty of notice – microsoft went public with its plans in december 2017 and killing off tls 1.0 and 1.1 once and for all is a long-held dream of the industry... The impact should be minimal on end users. The office client can use tls 1.2 if it is configured on the local computer, and most modern operating systems will happily support the later, more secure version. Windows 7, however, requires an update to make things work.. Late to the zoom party, skype rolls out backgrounds and adds more squares.. Redmond-owned messaging veteran skype has shuffled into the chat party, proffering gifts that rivals ( including zoom ) have enjoyed for quite a while... Microsoft emitted version 8.62 last week, replete with custom backgrounds and support for more chums in a video call. Exactly how many pals will be tiled is tricky to ascertain. Microsoft's release notes state there may be up to 10 in the new grid view, while its documentation on the matter reckons the number is nine... This is quite some way behind rival services. Zoom permits up to 49 participants at a time, should you be fortunate enough to have that many friends... The updates for windows, mac, linux and web are rolling out now.. Barracuda comes to azure.. No sooner had the ink dried on hpe's plan to snap up sd-wan darling silver peak, rival barracuda announced plans to run its service on microsoft azure... Sitting atop the microsoft global network and azure's virtual wan, barracuda's cloudgen wan ( modestly described by barracuda as the first secure global sd-wan service built natively on microsoft azure ) is aimed at hooking up locations with minimal fuss and faff... Rather than having to use relatively costly and inflexible network circuits for the corporate wan, the team reckoned that running in virtual fashion on microsoft's backbone will lead to reduced costs and a network that scales to match traffic... Naturally, there is also the option to be billed hourly through the azure marketplace.. Winui 3 hits preview 2, adds more detail for this year's goalpost dance.. Having pushed out the first preview of its latest attempt to persuade developers that windows desktop apps are still worth coding, no matter how whiffy the original universal windows platform ( uwp ) dream may be nowadays, microsoft has had popped more flesh on the bones... Winui is the gui framework and, recognising that devs cannot live on uwp alone, includes win32 support... While preview 1 brought forth all manner of bells and whistles, preview 2 is a quality and stability-driven release and deals with bugs that couldn't be squashed ahead of the build 2020 release. It is also compatible with .net 5 preview 5 for desktop apps. However, microsoft is at pains to point out that it is not ready for the production primetime yet... The release, part of microsoft's project reunion vision, was accompanied by an update on the project, including some key roadmap dates... The most notable is a preview 3 in time for the company's september ignite event and open-sourcing by autumn. November will see a version released that could be used in production apps, although a final release looks to be in the early part of the spring 2021. ®. Tips and corrections 7 comments more. Microsoft. Azure. Coronavirus.. Share copy most read twitter hackers busted 2fa to access accounts and then reset user passwords.. Cisco restores evidence of its funniest fail – ethernet cable presses switch's reset button.. Daas-appearing trick: netflix teases desktops-as-a-service product.. Nokia 5310: retro feature phone shamelessly panders to nostalgia, but is charming enough to be forgiven.. Mainframe madness as the snowflakes take control – and the on-duty operator hasn't a clue how to stop the blizzard.. Subscribe to our weekly tech newsletter. Subscribe keep reading skype for windows 10 and skype for desktop duke it out: only electron left standing. Updated i just can't quit you, skype. Oh maybe i can... They've tweaked the close function official: microsoft will take an axe to skype for business online. Teams is your new normal. Blade to swing in 2021, but onboarding for new office 365ers starts in september britain's courts lurch towards skype and conference calls for trials as covid-19 distancing kicks in. Coronavirus forces judges to join the 21st century more or less overnight friends, it's fine. Don't worry about randomers listening to your skype convos. Microsoft has tweaked an faq a bit. Automated and manual data processing – so humans, yeah? Using microsoft's dynamics 365 finance and operations? Using skype? Not for long!. Upcoming update could bork on-prem logins, warns redmond skype for web arrives to bring the world together. As long as the world is on chrome and... Edge?. Hello? Is this thing on? Microsoft takes a pruning axe to skype's forest of features. Say farewell to highlights... If you even noticed it was there microsoft gets ready to kill skype classic once again: this time we mean it. Remember remember the first of november tech resources. Navigating the cti noise. We all want better threat intelligence,");
		//s = summarize(w,"Artificial intelligence. Internet of things.. Sec drags silicon valley ai upstart. Navigating the cti noise. We all want better threat intelligence,");
		//t.assume(s, "???");
		
		//w.clear();
		//w.add("artificial");
		//w.add("intelligence");
		////w.add("disaster");
		//s = summarize(w,"dogs eat meat . artificial intelligence . internet of things . storage it's not a windows 10 release without disaster . tuna is a fish");
		//t.assume(s, "Artificial intelligence. Internet of things. Storage it's not a windows 10 release without disaster.");
//TODO
		//it hangs
		//s = summarize(w,"black hole destroys corona . artificial intelligence . internet of things . . sec drags silicon valley ai upstart to court over claims of made-up revenues, investors swindled out of $11m . uk formally abandons europe's unified patent court, germany plans to move forward nevertheless . world health organisation ai chatbot goes deaf when asked for the latest covid-19 figures for taiwan, hong kong . germany bans tesla from claiming its autopilot software is potentially autonomous . verity stob . . incredible artifact – or vital component after civilization ends? rare nazi enigma m4 box sells for £350,000 . from 'queen of the skies' to queen of the scrapheap: british airways chops 747 fleet as folk stay at home . see you after the commercial breakdown: cert expiry error message more entertaining than the usual advert tripe . motorbike ride-share app ceo taken to pieces in grisly new york dismemberment . storage . storage it's not a windows 10 release without disaster breaking so here's a troubleshooter for your onedrive woes .");
		//s = summarize(w,"black hole destroys corona . artificial intelligence . internet of things . . sec drags silicon valley ai upstart to court over claims of made-up revenues, investors swindled out of $11m . uk formally abandons europe's unified patent court, germany plans to move forward nevertheless . world health organisation ai chatbot goes deaf when asked for the latest covid-19 figures for taiwan, hong kong . germany bans tesla from claiming its autopilot software is potentially autonomous . verity stob . . incredible artifact – or vital component after civilization ends? rare nazi enigma m4 box sells for £350,000 . from 'queen of the skies' to queen of the scrapheap: british airways chops 747 fleet as folk stay at home . see you after the commercial breakdown: cert expiry error message more entertaining than the usual advert tripe . motorbike ride-share app ceo taken to pieces in grisly new york dismemberment . storage . storage it's not a windows 10 release without disaster .");
		//s = summarize(w,"artificial intelligence . internet of things . . sec drags silicon valley ai upstart to court over claims of made-up revenues, investors swindled out of $11m . uk formally abandons europe's unified patent court, germany plans to move forward nevertheless . world health organisation ai chatbot goes deaf when asked for the latest covid-19 figures for taiwan, hong kong . germany bans tesla from claiming its autopilot software is potentially autonomous . verity stob . . incredible artifact – or vital component after civilization ends? rare nazi enigma m4 box sells for £350,000 . from 'queen of the skies' to queen of the scrapheap: british airways chops 747 fleet as folk stay at home . see you after the commercial breakdown: cert expiry error message more entertaining than the usual advert tripe . motorbike ride-share app ceo taken to pieces in grisly new york dismemberment . storage . storage it's not a windows 10 release without disaster .");
		//s = summarize(w,"artificial intelligence . internet of things . storage it's not a windows 10 release without disaster .");
		//s = summarize(w,"artificial intelligence . not a windows 10 release without disaster .");
		//s = summarize(w,"artificial intelligence . without disaster .");
		//s = summarize(w,"artificial intelligence without disaster .");
		//s = summarize(w,"artificial intelligence disaster .");
		
		t.check();
	}
	
}
