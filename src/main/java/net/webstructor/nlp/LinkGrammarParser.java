/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents®
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
package net.webstructor.nlp;

import java.util.ArrayList;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.al.All;
import net.webstructor.al.Seq;
import net.webstructor.al.Set;
import net.webstructor.util.Array;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import net.webstructor.nlp.Dictionary;
import net.webstructor.nlp.Link;
import net.webstructor.nlp.Linkage;
import net.webstructor.nlp.Loader;
import net.webstructor.nlp.Rule;
import net.webstructor.nlp.Sentence;
import net.webstructor.nlp.Word;

public class LinkGrammarParser implements GrammarParser {
	
	private static final String defaultDictName="en/4.0.dict";
	private String dictName = "";
	private Dictionary dict = null;
	private boolean dictLoaded = false;
	public boolean tokenize = true;                   // Indicates that to need tokenize input sentence, default value=true
	
	public void loadDict() throws IOException {
		if(dictLoaded==true) {
			System.out.println("Dictionary already loaded");
			return;
		}		
		try {
			dictName=defaultDictName;
			Dictionary[] dicts = Loader.buildLGDict(dictName);
			dict = dicts[0];
			dictLoaded=true;
		}
		catch(Exception e) {
			dictName="";
			System.err.println("Can't load dictionary '"+dictName+"'");
		}		
	}
	
	public boolean isDictLoaded(){
		return dictLoaded;
	}
	
	public String printDictInfo() {
		if(dictLoaded)
			return "DictName="+defaultDictName+"; DictSize="+dict.getWords().size()+"; Version="+dict.getVersionNumber();
		else
			return "Dictionary not loaded";
	}
	
	public String[] tokenizeSentence(String sentence){	
		String[] words = sentence.split(" ");    // Split a sentence string into words, separating with a space
		//Make all letters is lowercase
		int cnt=0;
		for(String w: words){
			words[cnt]=w.toLowerCase();
			cnt++;
		}
		// Delete the dot from the end of last word if exists
		if(words.length>0) {
			String lastWord=words[words.length-1];
			if(lastWord.contains(".")) {
				lastWord=lastWord.substring(0, lastWord.length() - 1);
				words[words.length-1]=lastWord;
			}
		}	
		return words;
	}
	
	public void printTokens(String[] words){
		String tokens="";
		for(String s: words) {
			tokens+="["+s+"] ";
		}
		System.out.println("Tokens:("+words.length+") "+tokens);
	}
	
	public void printLgWords(String[] tokens)
	{
		String strLgWords="";	
		int wordsCnt=0;	
		HashSet<Word> wordsInDict=dict.getWords();
		ArrayList<String> subs = new ArrayList<>();
		for(String token: tokens){
			subs.clear();
			strLgWords+="["+token+"]";
			for (Word w : wordsInDict) {
				if (w.getWord().equals(token)) {
					subs.add(w.getSubscript());
				}
			}			
			wordsCnt+=subs.size();
			String strSubscripts="";
			for(String subscript: subs) {
				strSubscripts+=subscript+",";
			}
			if(subs.size()>0) {
				strSubscripts=strSubscripts.substring(0, strSubscripts.length() - 1);
			}
			strLgWords+="("+strSubscripts+") ";
		}
		System.out.println("Words:("+wordsCnt+") "+strLgWords);
	}
	
	public ArrayList<Word> getListOfSingleWords(String[] tokens){			
		ArrayList<Word> wordList = new ArrayList<>();
		HashSet<Word> wordsInDict = dict.getWords();
		for(String token: tokens) {
			for (Word w : wordsInDict) {
				if (w.getWord().equals(token)) {
					//System.out.println("Word="+token+"."+w.getSubscript());
					Word singleWord = new Word(token,w.getSubscript());	
					Rule rule = w.getRule();
					ArrayList<String> rulesForWords =  rule.getWords();
					//System.out.println("Rules count: "+rulesForWords.size());
					for(String strRule:rulesForWords ) {
						singleWord.addRule(strRule);
					}
					wordList.add(singleWord);	
				}
			}
		}
		return wordList;
	}
	
	public int getMaxSubsriptsCount(ArrayList<Word> wordList)
	{
		int maxSubsrCnt=0;
		int subscrCnt=0;
		String strWord=null;
		if(wordList.size()>0) {
			if(wordList.size()==1) {
				maxSubsrCnt=1;	
			}else {
				strWord=wordList.get(0).getWord();
				subscrCnt=1;
				maxSubsrCnt=1;
				for(int i=1; i<wordList.size(); i++) {
					if(wordList.get(i).getWord()!=strWord) {
						strWord=wordList.get(i).getWord();
						subscrCnt=1;
					}else {
						subscrCnt++;
						if(subscrCnt>maxSubsrCnt)
							maxSubsrCnt=subscrCnt;
					}	
				}
			}	
		}
		return maxSubsrCnt;
	}
	
	public void fillWordMatrix(Word[][] wordMatrix,ArrayList<Word> wordList) {
		
		if(wordList.size()==0)
			return;
		
		int wordIndex=-1;
		int subscrIndex=-1;
		String strLastWord="";

		for(Word w:wordList) {
			if(w.getWord()!=strLastWord) {
				strLastWord=w.getWord();
				wordIndex++;
				subscrIndex=0;
			}else {
				subscrIndex++;
			}
			wordMatrix[wordIndex][subscrIndex]=w;
		}
	}
	
	public ArrayList<Sentence> generateSentences(Word[][] matrix, int sentenceLen, int maxSubscrCnt){
	
		ArrayList<Sentence> sentenceList = new ArrayList<Sentence>();
		if(sentenceLen<=0)
			return sentenceList;
		
		int[] subscrIndexes=new int[sentenceLen];
		int subscrCnt=0;
		for(int wordIndex=0; wordIndex<sentenceLen; wordIndex++) {
			subscrCnt=0;
			for(int subscrIndex=0; subscrIndex<maxSubscrCnt; subscrIndex++) {
				subscrCnt++;
				Word w = matrix[wordIndex][subscrIndex];
				if(w==null) {
					subscrCnt=0;
					break;
				}
				subscrIndexes[wordIndex]=subscrCnt;
			}
		}
		
		int totalGeneratedSentenceCount = 1;
		for(int i=0; i<sentenceLen; i++) {
			totalGeneratedSentenceCount*=subscrIndexes[i];
		}
			
		System.out.println("Total generated sentence count = "+totalGeneratedSentenceCount);
		Word[][] sentences = new Word[sentenceLen][totalGeneratedSentenceCount];
		
		int packSize=totalGeneratedSentenceCount;
		int packCount=0;
		int sentenceIndex=0;
		int subscrIndex=0;
		for(int wordCnt=0; wordCnt<sentenceLen; wordCnt++) {
		
			packSize=packSize/subscrIndexes[wordCnt];
			packCount=totalGeneratedSentenceCount/packSize;
			
			for(int pc=0; pc<packCount; pc++) {		
				for(int ps=0; ps<packSize; ps++) {	
					sentences[wordCnt][sentenceIndex]=matrix[wordCnt][subscrIndex];
					sentenceIndex++;
					//Word w = sentences[wordCnt][sentenceIndex];
					//System.out.println(sentenceIndex+") "+w.getWord()+"."+w.getSubscript());
				}
				if(subscrIndex+1>=subscrIndexes[wordCnt])
					subscrIndex=0;
				else
					subscrIndex++;
			}
			sentenceIndex=0;
			subscrIndex=0;
		}
		
		for(sentenceIndex=0; sentenceIndex<totalGeneratedSentenceCount; sentenceIndex++) {
			Sentence sentence=new Sentence();
			for(int wordIndex=0; wordIndex<sentenceLen; wordIndex++) {	
				sentence.words.add(sentences[wordIndex][sentenceIndex]);
			}
			sentenceList.add(sentence);
			System.out.println((sentenceIndex+1)+") "+sentence.toString());
		}	
		
		return sentenceList;
	}
	
	public boolean checkLink(Sentence sentence, Link link) {
		boolean result=false;		
		Word wordLeft=sentence.words.get(link.w1Index);
		Word wordRight=sentence.words.get(link.w2Index);			
		result=connects(wordLeft, wordRight);
		return result;
	}
	
	public Linkage findLinkage(Sentence sentence) {
		
		Linkage linkage=null;
		if(sentence.length()<2)
			return null;
		
		//create possible links
		boolean linkValid=false;
		linkage=new Linkage();
		for(int i=0; i<sentence.length()-1; i++) {		
			for(int j=i+1; j<sentence.length(); j++) {				
				Link link=new Link(i,j);
				linkValid=checkLink(sentence,link);				
				if(linkValid) {
					linkage.addLink(link);
				}
				//System.out.println(link.toString()+" "+linkValid);			
			}
		}
		//System.out.println("Linkage length = "+linkage.length());
		
		if(linkage.length()<sentence.length()-1)
			return null;
	
		return linkage;
	}
	
	public Link getConnectedLink(ArrayList<Link> linkList, Link parentLink) {
		
		Link resultLink=null;
		if(linkList.size()<1)
			return resultLink;
		
		if(parentLink==null) {
			resultLink=linkList.get(0);
			linkList.remove(0);
		}else {
			for(int i=0; i<linkList.size(); i++) {
				Link tmpLink=linkList.get(i);
				if((tmpLink.w1Index==parentLink.w1Index || tmpLink.w1Index==parentLink.w2Index) || 
				   (tmpLink.w2Index==parentLink.w1Index || tmpLink.w2Index==parentLink.w2Index)) {
					resultLink=tmpLink;
					linkList.remove(i);
					break;
				}	
			}		
		}	
		return resultLink;
	}
	
	public ArrayList<Link> buildTree(Linkage linkage, int maxLinkCount) {
		
		ArrayList<Link> links=(ArrayList<Link>) linkage.linkList.clone();
		ArrayList<Link> tree = new ArrayList<>();
		
		if(linkage.length()<1)
			return tree;
		
		HashSet<Integer> indexes = new HashSet<>();
		Link link=null;
		do {
			if(link!=null) {
				if(indexes.contains(link.w1Index) && indexes.contains(link.w2Index)) {
					//
				}
				else {
					indexes.add(link.w1Index);
					indexes.add(link.w2Index);
					tree.add(link);
					if(tree.size()==maxLinkCount)
						break;
				}	
			}
			link=getConnectedLink(links, link);
		} while(link!=null);
		
		return tree; 
	}
	
	public String treeToStr(ArrayList<Link> tree, Sentence sentence) {
		String resultStr="";
		for(Link link: tree) {
			resultStr+="["+sentence.words.get(link.w1Index).getWord()+" "+sentence.words.get(link.w2Index).getWord()+"] ";
		}
		return resultStr;
	}
	
	public void processingSentences(ArrayList<Sentence> sentenceList)
	{
		String strTree="";
		int totalTreeCount=0;
		ArrayList<Link> tree=null;
		for(int sentenceIndex=0; sentenceIndex<sentenceList.size(); sentenceIndex++) {
			Sentence sentence=sentenceList.get(sentenceIndex);
			Linkage linkage=findLinkage(sentence);
			if(linkage!=null) {
				boolean allWordsConnected=linkage.isAllWordsCanBeConnected(sentence.length());
				if(allWordsConnected) {
					tree = buildTree(linkage,sentence.length()-1);
					//TODO add planarity check
					if(tree.size()==sentence.length()-1) {
						totalTreeCount++;
						strTree="Tree="+treeToStr(tree,sentence);
					}
					System.out.println((sentenceIndex+1)+") "+sentence.toString() +" possible links count="+linkage.length()+" "+linkage.toString()+" "+strTree);
				}
			}
		}
		System.out.println("Total tree count = "+totalTreeCount);
	}
	
	public void parseSentence(String sentence){
		if(dictLoaded==false) {
			System.out.println("Dictionary not loaded. Need to load dictionary!");
			return;
		}
		String[] tokens=null;
		if(tokenize==true) {
			tokens=tokenizeSentence(sentence);
			System.out.println("\nStage 1. Tokenize sentence");
			printTokens(tokens);
		}
		
		if(tokens.length<2) {
			System.out.println("\nSentence length < 2. Stop processing");
			return;
		}
		
		System.out.println("\nStage 2. Get words, subscripts and rules from dictionary");
		ArrayList<Word> listOfSingleWords = getListOfSingleWords(tokens);
		for(Word w:listOfSingleWords) {	
			String strRule="";
			Rule rule = w.getRule();
			ArrayList<String> wrds = rule.getWords();
			for(String s:wrds) {
				strRule+=s+" ";
			}
			System.out.println("["+w.getWord()+"."+w.getSubscript()+"] "+strRule);
		}
			
		System.out.println("\nStage 3. Create generation matrix");
		int sentenceLen=tokens.length;
		int maxSubscripsCnt=getMaxSubsriptsCount(listOfSingleWords);
		Word[][] wordMatrix = new Word[sentenceLen][maxSubscripsCnt];
		fillWordMatrix(wordMatrix,listOfSingleWords);
		for(int i=0; i<sentenceLen; i++) {
			for(int j=0; j<maxSubscripsCnt; j++) {
				Word w=wordMatrix[i][j];
				if(w!=null) {
					System.out.println("["+i+"]["+j+"] "+w.getWord()+"."+w.getSubscript());
				}		
			}
		}
		
		System.out.println("\nStage 4. Generate sentences");
		ArrayList<Sentence> sentenceList  = generateSentences(wordMatrix,sentenceLen,maxSubscripsCnt);
		
		System.out.println("\nStage 5. Processing sentences");
		processingSentences(sentenceList);	
	}
	
	// Functions for linkages
	private boolean connects(Word left, Word right) {
		
		ArrayList<Rule> leftList = new ArrayList<Rule>();
		leftList.add(left.getRule());
		
		ArrayList<Rule> rightList = new ArrayList<Rule>();
		rightList.add(right.getRule());
		
		if (leftList.size() == 0) {
			System.err.println("Word '" + left.getWord() + "' not found in dictionary.");
			System.exit(0);
		}
		if (rightList.size() == 0) {
			System.err.println("Word '" + right.getWord() + "' not found in dictionary.");
			System.exit(0);
		}
		for (Rule leftRule : leftList) {
			for (Rule rightRule : rightList) {
				String lr = leftRule.toString();
				String rr = rightRule.toString();
				lr = beforeNull(lr);
				rr = beforeNull(rr);
				lr = replaceNull(lr);
				rr = replaceNull(rr);
				
				ArrayList<String> Lops = new ArrayList<>(), Rops = new ArrayList<>(), Lcosts = new ArrayList<>(),
						Rcosts = new ArrayList<>();
				while (lr.contains("{")) {
					int start = lr.indexOf("{");
					int end = 0;
					int numC = 1, num = 0;
					for (int i = start + 1; i < lr.length(); i++) {
						if (lr.charAt(i) == '{')
							numC++;
						else if (lr.charAt(i) == '}')
							num++;
						if (numC == num) {
							end = i + 1;
							break;
						}
					}
					try {
						Lops.add(lr.substring(start, end));
						lr = lr.substring(0, start) + lr.substring(end);
					} catch (Exception e) {
						Lops.add(lr.substring(start+1, lr.length()));
						lr = lr.substring(0, start);
					}
				}
				lr = fixString(lr);

				while (rr.contains("{")) {
					int start = rr.indexOf("{");
					int end = 0;
					int numC = 1, num = 0;
					for (int i = start + 1; i < rr.length(); i++) {
						if (rr.charAt(i) == '{')
							numC++;
						else if (rr.charAt(i) == '}')
							num++;
						if (numC == num) {
							end = i + 1;
							break;
						}
					}
					try {
						Rops.add(rr.substring(start, end));
						rr = rr.substring(0, start) + rr.substring(end);
					} catch (Exception e) {
						Rops.add(rr.substring(start+1, rr.length()));
						rr = rr.substring(0, start);
					}
				}
				rr = fixString(rr);
				
				ArrayList<String> toAddLops = new ArrayList<>();
				ArrayList<String> toAddRops = new ArrayList<>();
				
				int id = 0;
				for (String str : Rops) {
					str = str.substring(1, str.length() - 1);
					while (str.contains("{")) {
						int start = str.indexOf("{");
						int end = 0;
						int numC = 1, num = 0;
						for (int i = start + 1; i < str.length(); i++) {
							if (str.charAt(i) == '{')
								numC++;
							else if (str.charAt(i) == '}')
								num++;
							if (numC == num) {
								end = i + 1;
								break;
							}
						}
						try {
							toAddRops.add(str.substring(start, end));
							str = str.substring(0, start) + str.substring(end);
						} catch (Exception e) {
							toAddRops.add(str.substring(start+1, str.length()));
							str = str.substring(0, start);
						}
					}
					str = fixString(str);
					Rops.set(id, str);
					id++;
				}
				
				id = 0;
				for (String str : Lops) {
					str = str.substring(1, str.length() - 1);
					while (str.contains("{")) {
						int start = str.indexOf("{");
						int end = 0;
						int numC = 1, num = 0;
						for (int i = start + 1; i < str.length(); i++) {
							if (str.charAt(i) == '{')
								numC++;
							else if (str.charAt(i) == '}')
								num++;
							if (numC == num) {
								end = i + 1;
								break;
							}
						}
						try {
							toAddLops.add(str.substring(start, end));
							str = str.substring(0, start) + str.substring(end);
						} catch (Exception e) {
							toAddLops.add(str.substring(start+1, str.length()));
							str = str.substring(0, start);
						}
					}
					str = fixString(str);
					Lops.set(id, str);
					id++;
				}
				
				Lops.addAll(toAddLops);
				Rops.addAll(toAddRops);
				
				for (String l : lr.split(" or ")) {
					int ri = -1;
					rloop: for (String r : rr.split(" or ")) {
						ri++;
						int numC = 0, num = 0;
						for (int q = ri; q < rr.split(" or ").length; q++) {
							boolean a = false;
							for (char c : rr.split(" or ")[q].toCharArray()) {
								if (c == '(')
									numC++;
								else if (c == ')')
									num++;
								else if (c == '&')
									a = true;
							}
							if (a)
								continue rloop;
							if (num > numC)
								break;
						}
						l = format(l);
						r = format(r);
						String fl = "";
						for (String p : l.split(" & ")) {
							if (!p.contains("-")) {
								fl += p + " & ";
							}
						}
						if (fl.endsWith(" & "))
							fl = fl.substring(0, fl.length() - 3);
						String fr = "";
						for (String p : r.split(" & ")) {
							if (p.contains("-")) {
								fr += p + " & ";
							}
						}
						if (fr.endsWith(" & "))
							fr = fr.substring(0, fr.length() - 3);
						fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
						if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
							return true;
						}
					}
				}
				for (String lb : Lops) {
					for (String rb : Rops) {
						for (String l : lb.split(" or ")) {
							for (String r : rb.split(" or ")) {
								l = l.split("& \\{")[0];
								r = r.split("& \\{")[0];
								l = format(l);
								r = format(r);
								String fl = "";
								for (String p : l.split(" & ")) {
									if (!p.contains("-")) {
										fl += p + " & ";
									}
								}
								if (fl.endsWith(" & "))
									fl = fl.substring(0, fl.length() - 3);
								String fr = "";
								for (String p : r.split(" & ")) {
									if (r.contains("-")) {
										fr += p + " & ";
									}
								}
								if (fr.endsWith(" & "))
									fr = fr.substring(0, fr.length() - 3);
								fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
								if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
									return true;
								}
							}
						}
					}
				}

				for (String lb : Lops) {
					for (String l : lb.split(" or ")) {
						l = l.split("& \\{")[0];
						while (l.contains("[")) {
							int start = l.indexOf("[");
							int end = 0;
							int numC = 1, num = 0;
							for (int i = start + 1; i < l.length(); i++) {
								if (l.charAt(i) == '[')
									numC++;
								else if (l.charAt(i) == ']')
									num++;
								if (numC == num) {
									end = i + 1;
									break;
								}
							}
							Lcosts.add(l.substring(start, end == 0 ? l.length() : end));
							l = l.substring(0, start) + l.substring(end == 0 ? l.length() : end);
						}
						l = format(l);
						String fl = "";
						for (String p : l.split(" & ")) {
							if (!p.contains("-")) {
								fl += p + " & ";
							}
						}
						if (fl.endsWith(" & "))
							fl = fl.substring(0, fl.length() - 3);
						fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
						for (String r : rr.split(" or ")) {
							r = format(r);
							String fr = "";
							for (String p : r.split(" & ")) {
								if (p.contains("-")) {
									fr += p + " & ";
								}
							}
							if (fr.endsWith(" & ")) fr = fr.substring(0, fr.length() - 3);
							if (!fl.isEmpty() && !fr.isEmpty() && equals(fl.trim(), fr.trim())) {
								return true;
							}
						}
					}
				}

				for (String rb : Rops) {
					for (String r : rb.split(" or ")) {
						r = r.split("& \\{")[0];
						while (r.contains("[")) {
							int start = r.indexOf("[");
							int end = 0;
							int numC = 1, num = 0;
							for (int i = start + 1; i < r.length(); i++) {
								if (r.charAt(i) == '[')
									numC++;
								else if (r.charAt(i) == ']')
									num++;
								if (numC == num) {
									end = i + 1;
									break;
								}
							}
							Rcosts.add(r.substring(start, end == 0 ? r.length() : end));
							r = r.substring(0, start) + r.substring(end == 0 ? r.length() : end);
						}
						r = format(r);
						String fr = "";
						for (String p : r.split(" & ")) {
							if (p.contains("-")) {
								fr += p + " & ";
							}
						}
						if (fr.endsWith(" & "))
							fr = fr.substring(0, fr.length() - 3);
						for (String l : lr.split(" or ")) {
							l = format(l);
							String fl = "";
							for (String p : l.split(" & ")) {
								if (!p.contains("-")) {
									fl += p + " & ";
								}
							}
							if (fl.endsWith(" & "))
								fl = fl.substring(0, fl.length() - 3);
							fl = fl.replaceAll("\\+", "/").replaceAll("-", "\\+").replaceAll("/", "-");
							for (String pfr : fr.split(" & ")) {
								for (String pfl : fl.split(" & ")) {
									if (!pfl.isEmpty() && !pfr.isEmpty() && equals(pfl.trim(), pfr.trim())) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private String replaceNull(String lr) {
		while (lr.contains("()")) {
			int idx = lr.indexOf("()");
			int end = lr.indexOf(" ", idx) != -1 ? lr.indexOf(" ", idx) : lr.length() - 1;
			if (lr.charAt(end - 1) == '}')
				return lr;
			if (lr.charAt(end - 1) == ')') {
				for (int i = end - 2; i >= 0; i--) {
					if (lr.charAt(i) != ')') {
						end = i + 2;
						break;
					}
				}
				int num = 1;
				int numC = 0;
				int finish = 0;
				for (int i = end - 2; i >= 0; i--) {
					if (lr.charAt(i) == ')')
						num++;
					else if (lr.charAt(i) == '(')
						numC++;
					if (numC == num) {
						finish = i;
						break;
					}
				}
				String total = lr.substring(finish, end);
				lr = lr.replace(total, "{" + total.substring(1, total.lastIndexOf("or")) + "}");
			} else {
				int space1 = 0;
				for (int i = end - 1; i >= 0; i--) {
					if (lr.charAt(i) == ' ') {
						space1 = i;
						break;
					}
				}
				if (lr.charAt(space1 - 4) == ')' || lr.charAt(space1 - 4) == ']') {
					char c = lr.charAt(space1 - 4);
					char oc;
					if (c == ')')
						oc = '(';
					else
						oc = '[';
					int finish = 0;
					int num = 1;
					int numC = 0;
					for (int i = space1 - 5; i >= 0; i--) {
						if (lr.charAt(i) == c)
							num++;
						else if (lr.charAt(i) == oc)
							numC++;
						if (numC == num) {
							finish = i;
							break;
						}
					}
					lr = lr.replace(lr.substring(finish, end), "{" + lr.substring(finish + 1, space1 - 4) + "}");
				} else if (lr.charAt(space1 - 5) == ')' || lr.charAt(space1 - 5) == ']') {
					char c = lr.charAt(space1 - 5);
					char oc;
					if (c == ')')
						oc = '(';
					else
						oc = '[';
					int finish = 0;
					int num = 1;
					int numC = 0;
					for (int i = space1 - 6; i >= 0; i--) {
						if (lr.charAt(i) == c)
							num++;
						else if (lr.charAt(i) == oc)
							numC++;
						if (numC == num) {
							finish = i;
							break;
						}
					}
					lr = lr.replace(lr.substring(finish, end), "{" + lr.substring(finish + 1, space1 - 4) + "}");
				} else {
					int space2 = 0;
					for (int i = space1 - 5; i >= 0; i--) {
						if (lr.charAt(i) == ' ') {
							space2 = i;
							break;
						}
					}
					if (lr.charAt(space2 + 1) == '(')
						space2++;
					int fin = lr.indexOf(' ', space2 + 1);
					lr = lr.replace(lr.substring(space2 + 1, end), "{" + lr.substring(space2 + 1, fin) + "}");
				}
			}
		}
		return lr;
	}

	private String beforeNull(String lr) {
		lr = fix(lr, "([()] & ", "(");
		lr = fix(lr, "  or ", " or ");
		lr = fix(lr, "or ()", "or [()]");
		return lr;
	}
	
	private static String fix(String lr, String reg, String rep) {
		while (lr.contains(reg))
			lr = lr.replace(reg, rep);
		return lr;
	}
	
	private static String fixString(String lr) {
		if (lr.startsWith(" &")) lr = lr.substring(2).trim();
		if (lr.startsWith(" or")) lr = lr.substring(3).trim();
		lr = fix(lr, "[ & ]", "");
		lr = fix(lr, "( & )", "");
		lr = fix(lr, "[ or ]", "");
		lr = fix(lr, "( or )", "");
		lr = fix(lr, "( or ", "(");
		lr = fix(lr, "( & ", "(");
		lr = fix(lr, " or )", ")");
		lr = fix(lr, " & )", ")");
		lr = fix(lr, "[ or ", "[");
		lr = fix(lr, "[ & ", "[");
		lr = fix(lr, " or ]", "]");
		lr = fix(lr, " & ]", "]");
		lr = fix(lr, "&  &", "&");
		lr = fix(lr, "or  or", "or");
		lr = fix(lr, "or  &", "or");
		return lr;
	}

	private static String format(String l) {
		return l.replace("(", "").replace(")", "").replace("[", "").replace("]", "").replace("}", "").replace("{", "")
				.trim();
	}
	
	private static boolean equals(String wlu, String wr) {
		wlu = wlu.replace("@", "");
		wr = wr.replace("@", "");
		if (wlu.equals(wr)) {
			return true;
		}
		if (wlu.contains("*")) {
			int idx = wlu.indexOf("*");
			int lid = wlu.lastIndexOf("*");
			if (wr.length() == wlu.length()) {
				if (wlu.substring(0, idx).equals(wr.substring(0, idx))
						&& wlu.substring(lid + 1).equals(wr.substring(lid + 1))) {
					return true;
				}
			} else {
				if (idx + 1 > wr.length() && wlu.substring(0, idx).equals(wr.substring(0, Math.min(wr.length(), idx))))
					return true;
			}
		}
		if (wr.contains("*")) {
			int idx = wr.indexOf("*");
			int lid = wr.lastIndexOf("*");
			if (wr.length() == wlu.length()) {
				if (wlu.substring(0, idx).equals(wr.substring(0, idx))
						&& wlu.substring(lid + 1).equals(wr.substring(lid + 1))) {
					return true;
				}
			} else {
				if (idx + 1 > wlu.length()
						&& wlu.substring(0, Math.min(wlu.length(), idx)).equals(wr.substring(0, idx))) {
					return true;
				}
			}
		}
		wr = wr.replace("-", "");
		wlu = wlu.replace("-", "");
		if (wlu.contains("&") || wr.contains("&"))
			return false;
		if (wlu.length() < wr.length()) {
			if (wlu.equals(wr.substring(0, wlu.length())))
				return true;
		}
		if (wr.length() < wlu.length()) {
			if (wr.equals(wlu.substring(0, wr.length())))
				return true;
			try {
				if (wr.equals(wlu.substring(1, wr.length()+1)))
					return true;
			} catch (Exception e) {
			}
		}
		return false;
	}
	
	@Override
	public
	Set parse(Seq tokens,Map<String,String> params) {
		//TODO LinkGrammarParser
		
		//below is just a demo stub
		if (AL.empty(tokens))
			return new All(new Seq[]{});
		ArrayList grams = new ArrayList();
		int len = tokens.size();
		int from_max = len - 1;
		for (int from = 0; from < from_max; from++) {
			for (int to = from + 1; to < len; to++) {
				if (Array.contains(new String[] {"a","the"},(String)tokens.get(to)))
					continue;//skip determiners
				grams.add(new Seq(new String[]{(String)tokens.get(from),(String)tokens.get(to)}));
				break;
			}
		}
		return new All(grams.toArray(new Seq[]{}));
	}
	
}
