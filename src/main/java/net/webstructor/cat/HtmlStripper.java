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
package net.webstructor.cat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.webstructor.al.AL;
import net.webstructor.util.Array;
import net.webstructor.util.Str;

	public class HtmlStripper
	{
		public static final String block_breaker = ". ";
		public static final String word_breaker = " ";//TODO: add CR/LF !?

		public static final String[] word_tags = {"br","div","td","tr","span","i","b","u"};
		public static final String[] block_tags = {
				//http://www.w3schools.com/html/default.asp
				"p","tr","ul","h1","h2","h3","h4","h5","li","ul",
				//http://www.w3schools.com/html/html5_new_elements.asp
				"aside","bdi","ficaption","header","main","section","summary","article"};
		public static final String[] header_tags = {"h1","h2","h3","h4","h5","h6"};
		public static final String[] skip_tags = { "style", "script", "svg", "head", "noscript" };
		public static final String[] unclosed_tags = { "p", "br" };
		public static final String[] block_and_break_tags = Array.union(block_tags, unclosed_tags);//<p>, ... + <br> 
		
		static final int BLOCK_PHRASE_LENGTH = 16;//32;
		
    	//http://www.fileformat.info/info/unicode/char/feff/index.htm
    	static final String WS = " \t\n\r\uFEFF";
    	
        static char  LT = '<' ;
        static char  GT = '>' ;
        static char[] LTGT = {LT,GT};
        static String HEAD="head";
        static String TITLE="title";
        static String BODY="body";
        static String META="meta";
        static String BASE="base";
        static String A="a";
        static String IMG="img";
        static String DATA_ORIGINAL="data-original";
        static String DATA_SRC="data-src";
        static String SOURCE="source";//for PICTURE
        static String HREF="href";
        static String SRC="src";
        static String SRCSET="srcset";
        static char[] quotes = {'\'','\"'};

        //http://www.ascii.cl/htmlcodes.htm
        //http://www.degraeve.com/reference/specialcharacters.php
        //http://www.theukwebdesigncompany.com/articles/entity-escape-characters.php  
        //http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
        //http://www.degraeve.com/reference/specialcharacters.php
        private static final String[] etokens = {"&times;","&quot;","&ndash;","&mdash;", "&minus;", "&amp;","&lt;","&gt;","&nbsp;","&nbsp","&euro;","&cent;","&pound;","&yen;","&copy;","&#169;","&reg;","&#174;","&deg;","&#8482;","&#39;","&#039;","&rarr;","&sbquo;","&laquo;"  ,"&raquo;"  ,"&lsquo;"  ,"&rsquo;"  ,"&ldquo;"  ,"&rdquo;"  ,"&bdquo;","&ldquor;", "&#x3D;","‘" ,"’" ,"“" ,"”" ,"&hellip;"};
        private static char[] echars =          {'*',		'\"',    '–',	    '—',	   '−',		  '&',	'<'   ,'>'   ,' '     ,' '    ,'€'	   ,'¢'     ,'£'      ,'¥'    ,'©'     ,'©'     ,'®'    ,'®'     ,'°'    ,'™'	,	'\'',	'\'', 	'→'      ,'‚'      ,'\"'/*'«'*/,'\"'/*'»'*/,'\''/*'‘'*/,'\''/*'’'*/,'\"'/*'“'*/,'\"'/*'”'*/,'\"'/*'„'*/,'\"'/*'„'*/, '='  ,'\'','\'','\"','\"','…'};
        private static String[] charmappings = {"«»“”‘’","\"\"\"\"\'\'"};
        
        public static String cleanHtmlRegExp(String text)
        {
            //http://www.developerfusion.co.uk/show/3901/
            //Regex re = new Regex();
            //return Regex.Replace(data,"<[^>]*>","");
    		return text.replaceAll("<[^>]*>","");        	
        }
        
        public static String stripHtmlAnchor(String url){
 			int hashPos = url.indexOf("#");
			if (hashPos != -1)
				url = url.substring(0,hashPos);
			if (url.length() > 1 && url.charAt(url.length() - 1) == '/')
				url = url.substring(0,url.length() - 1);
        	return url;
		}
        
        /**
         * 
         * @param source - entire page source
         * @param pos - position of text
         * @param name - SRC or HREF
         * @param baseHref - page base if present
         * @return
         */
        public static String attribute(String source, int pos, String name, String baseHref){
//TODO: lowercase
            int till = 1024;
            int begpos = Array.indexOf(source,pos+1,name+"=",till);
            int tagendpos = Array.indexOf(source,pos+1,">",till);
            if (begpos != -1 && begpos < tagendpos) {//if attribute is found in scope of tag
            	begpos = Array.indexOf(source,begpos+name.length()+1,quotes);
            	if (begpos != -1) {
                	char quote = source.charAt(begpos); 
            		//int endpos = source.indexOf(quote,begpos+1);
            		int endpos = Array.indexOf(source, begpos+1, new char[]{quote,' '});
            		if (endpos != -1) {
            			String value = decodeHTML(source.substring(begpos+1, endpos));
            			if (!AL.empty(value)){
            				if (!AL.empty(baseHref) && !HttpFileReader.isAbsoluteURL(value))
            					value = HttpFileReader.alignURL(baseHref,value,false);
        					return value;
            			}
            		}
            	}
            }
            return null;
        }

        public static String convert( String  source, String breaker, ArrayList links){
            return convert(source, block_tags, breaker, links, null, null, null, null);
        }
        /**
         * @param images - "postion to string" map of image sources 
        */
        public static String convert( String  source, String[] blocktags, String breaker, ArrayList links, Map images, Map linksPositions, Map titles, String path)
        {
        	ArrayDeque tagStarts = new ArrayDeque();
        	String tag;
    		String currentLinkUrl = null;
    		StringBuilder currentLinkBuf = null;
    		StringBuilder headerContentBuf = null;
    		int currentLinkBeg = 0;
        	//int[] depths = new int[100];
        	//int depth = 0;
            if( source == null || source.length() == 0) 
            {
                return "" ;
            }
            StringBuilder buf = new StringBuilder(source.length()/2) ;
            int startOfText = 0 ;
            int pos = 0;
            
            // jump to title tag
            pos=look4Tag(source,pos,TITLE);
            if(pos == -1)
                pos = 0;
            String title = extractTitle(source);
            if (titles != null && !AL.empty(title))
            	titles.put(new Integer(0), title);

            // skip html header
            pos=skipTag(source,0,HEAD);
            if(pos == -1)
                pos = 0;
            
            //jump to body
            pos=look4Tag(source,pos,BODY); 
            if(pos == -1)
                pos = 0;

            startOfText = pos;
            if( source.indexOf(LT,pos) < 0 ){
            	StringBuilder decodedText = new StringBuilder();
            	addText(decodedText,source);//just remove &...; and &#...; encodings
                return decodedText.toString();
            }
            String baseHref = getTagAttr(source,0,pos,BASE,HREF);
            if (AL.empty(baseHref))
            	baseHref = path;
            for(  pos = source.indexOf(LT,pos); pos < source.length(); pos = source.indexOf(LT,pos)  ) 
            {
                if(pos < 0)
                	break;
                if(pos>startOfText && startOfText != -1)
                {
                	StringBuilder chunk = new StringBuilder(); 
                    addText(chunk,source.substring(startOfText,pos));
                    String chunkStr = chunk.toString();
                    addText(buf,chunkStr);//suppress spaces that way!?
                    if (currentLinkBuf != null)
                    	currentLinkBuf.append(chunkStr);
                    if (headerContentBuf != null)
                    	headerContentBuf.append(chunkStr);
                    startOfText = -1;
                }
                if (++pos >= source.length())
                	break;
                if(isComment(source,pos))
                {
                    // skip html comments
                    pos = skipComment(source,pos+2);
                } 
                else	
                if((tag = whichTag(source,pos,skip_tags,false)) != null) {
                    //skip javascript or style
                    pos = skipTagBody(source,pos+tag.length(),tag);
                } 
                else 
                {
                	if (titles != null && (tag = whichTag(source,pos,header_tags,true)) != null) {
                		if (source.charAt(pos) != '/')
                			headerContentBuf = new StringBuilder();
                		else {
                			if (!AL.empty(headerContentBuf))
                				titles.put(new Integer(buf.length()), headerContentBuf.toString());
                			headerContentBuf = null;
                		}
                	}
                	if (images != null) {
	                	if(isTag(source,pos,IMG)) {
	                		//data-original is jQuery hack: http://tokmakov.msk.ru/blog/psts/30/
	                		String imgSrc = attribute(source,pos,DATA_ORIGINAL,baseHref);
	                		if (AL.empty(imgSrc))
	                			imgSrc = attribute(source,pos,SRC,baseHref);
	                		if (AL.empty(imgSrc))
	                			imgSrc = attribute(source,pos,SRCSET,baseHref);
	                		if (AL.empty(imgSrc))
	                			imgSrc = attribute(source,pos,DATA_SRC,baseHref);
	                		if (!AL.empty(imgSrc))
	                			images.put(new Integer(buf.length()), imgSrc);
	                	} else
	                	if(isTag(source,pos,SOURCE)) {
	                		String imgSrc = attribute(source,pos,SRCSET,baseHref);
	                		if (!AL.empty(imgSrc))
	                			images.put(new Integer(buf.length()), imgSrc);
	                	}
                	}
                	if (links != null) {
	                	if(isTag(source,pos,A)) {
	                		//remember current link and start filling it 
	                		//TODO: replace with attribute(...)!?
	                        int begpos = source.indexOf(HREF,pos+1);
	                        if (begpos != -1) {
	                        	begpos = Array.indexOf(source,begpos+5,quotes);
	                        	if (begpos != -1) {
		                        	char quote = source.charAt(begpos); 
	                        		int endpos = source.indexOf(quote,begpos+1);
	                        		if (endpos != -1) {
	                        			currentLinkUrl = decodeHTML(source.substring(begpos+1, endpos));
	                        			if (!AL.empty(baseHref) && !HttpFileReader.isAbsoluteURL(currentLinkUrl)) {
	                        				currentLinkUrl = HttpFileReader.alignURL(baseHref,currentLinkUrl,false);
	                        			}
	                        			currentLinkBuf = new StringBuilder();
	                        			currentLinkBeg = buf.length();
	                        		}
	                        	}
	                        }
	                	}
	                	else 
	                	if(isTag(source,pos,"/"+A)) {
	                		//flush creation of the link
	                		if (currentLinkUrl != null && currentLinkBuf != null && currentLinkBuf.length() > 0) {
	                			currentLinkUrl = stripHtmlAnchor(currentLinkUrl);
	                			if (currentLinkUrl.length() > 0 && !currentLinkUrl.startsWith("javascript:")){
	                				String linkUrl = currentLinkUrl.trim();
	                				links.add(new String[]{linkUrl,currentLinkBuf.toString().trim()});
	    	                		if (linksPositions != null)
	    	                			linksPositions.put(new Integer((currentLinkBeg + buf.length())/2), linkUrl);
	                			}
	                		}
	                		currentLinkUrl = null;
	                		currentLinkBuf = null;
	                	}
                	}
                	if (whichTag(source,pos,word_tags,true) != null) {
                		//buf.append(word_breaker);
                		addText(buf,word_breaker);
                        if (currentLinkBuf != null)
                        	currentLinkBuf.append(word_breaker);
                        if (headerContentBuf != null)
                        	headerContentBuf.append(word_breaker);
                	}
                    //skip tag
                	int oldpos = pos;
                	
                    pos = skipTagInteriors(source,pos);
                    if(pos == -1) pos = source.length();
                    else {
                    	if (breaker != null) {// if doing tag translation at all
                    		/*
                    		if (pos - oldpos > 1) //if can be closing tag
                    			if (source.charAt(oldpos) == '/') { //closing tag, indeed
                    				if (Array.contains(block_tags, source.substring(oldpos+1, pos)))
                    					addText(buf,breaker);
                    			}
                    		*/
                        	//TODO: intelligent phrase breaking
                    		boolean closing = source.charAt(oldpos) == '/';
                    		String name = source.substring(oldpos + (closing ? 1 : 0), pos);
                        	if (Array.startsWith(name,blocktags) != null) {//TODO case insensitive
                        		if (!closing && !Array.contains(unclosed_tags, name.toLowerCase()))//<p> & <br> do not need closing
                        			tagStarts.push(new Integer(pos));
                        		else {
                        			int start = tagStarts.isEmpty() ? 0 : ((Integer)tagStarts.pop()).intValue();
                        			if (oldpos - start > BLOCK_PHRASE_LENGTH)//TODO:somethig with this "magic" phrase length - build the "depth map of every token and break sentences based on the depth!!!"
                    					addText(buf,breaker);
                        		}
                        		
                        	}
                    	}
                    	pos++;
                    }                    
               }
                startOfText = pos;
            }
            if(pos>startOfText && startOfText != -1)
                addText(buf,source.substring(startOfText,pos));
            if(pos < 0 && startOfText != -1)
                addText(buf,source.substring(startOfText));
//TODO:if unspace, account for index of images
            //return unspace(buf);
            return buf.toString().trim();
        }
        
        //TODO: move to util
        static String unspace(StringBuilder input) {
            StringBuilder output = new StringBuilder();
            boolean whitespaced = false;
            char c = 0;
            for (int i = 0; i < input.length(); i++) {
            	c = input.charAt(i);
            	boolean whitespace = WS.indexOf(c) != -1;
            	if (whitespace) {
            		if (output.length() == 0)
            			continue;
            		if (whitespaced)
            			continue;
            		whitespaced = true;
            		c = ' ';
            	} else
            		whitespaced = false;
            	output.append(c);
            }
            int l = output.length();//remove trailing space
            if (l > 0 && output.charAt(--l) == ' ')
            	output.setLength(l);
        	return output.toString();
        }

        //TODO: support ALL HTML Names to full extent:
        //http://www.ascii.cl/htmlcodes.htm
        static void addText(StringBuilder buf, String str)
        {		
            if (AL.empty(str))
        		return;
                                 char c=0;
                                 int len = buf.length();
                                 if(len > 0 && WS.indexOf(str.charAt(0)) == -1 && WS.indexOf(buf.charAt(len - 1)) == -1)
                                     buf.append(' ');
                                 for(int i=0;i<str.length();i++)
                                 {
                                	 int ci;
                                     c = str.charAt(i);
                                     if(c == '&')
                                     for (;;) {//repeatedly handle continious encodings like &amp;#x200B;
                                    	 //handle stuff like &#8212 and &#x2019 '’'
                                    	 boolean handled = false; 
                                    	 int ihash = i + 1, iend, ilen, ihex;
                                    	 String shex;
                                    	 boolean hex;
                                    	 if (ihash < str.length() && str.charAt(ihash) == '#' &&
                                    		(iend = str.indexOf(';', ihash))!= -1  && 
                                            (ilen = iend - ihash) > 0 && 
                                    		(shex = str.substring(ihash+1+((hex = str.charAt(ihash+1) == 'x') ? 1 : 0),iend)) != null &&
                                    		(ihex = StringUtil.toIntOrZero(shex,hex ? 16 : 10)) != 0) {
                                    		c = (char) ihex;
                                    		i += ilen + 1;
                                    		handled = true;
                                    	 }
                                    	 else
                                    	 for (int j = 0; j < etokens.length; j++)
                                    		 if (str.regionMatches(i, etokens[j], 0, etokens[j].length())) {
                                    			 i += (etokens[j].length() - 1);
                                    			 c = echars[j];
                                    			 handled = true; 
                                    			 break;
                                    		 }
                                    	 if (!handled)
                                    		 break;
                                     } 
                                     else
                                     //http://www.adamkoch.com/2009/07/25/white-space-and-character-160/
                                     if( c == 0x0a || c == 0x0d || c == 160 || WS.indexOf(c) != -1)
                                         c = ' ';
                                     else if ((ci = charmappings[0].indexOf(c)) != -1)//unification of quotes
                                    	 c = charmappings[1].charAt(ci);
                                     
                                     // skip repeated spaces or spec symbols
                                     len = buf.length();
                                     if (c != ' ' || (len > 0 && buf.charAt(len - 1) != ' '))
                                    	 buf.append(c);
                                 }
        }
        static boolean isComment(String source, int pos)
                                     {
                                         //return String.Compare(source,pos,"!--",0,3)==0;
                                         return source.regionMatches(pos,"!--",0,3);
                                     }
        static int skipComment(String source, int pos)
                             {
                                 if((pos = source.indexOf("-->",pos)) != -1)
                                 {
                                     return pos+3;
                                 }
                                 return source.length();
                             }
        
        static String whichTag(String source, int pos, String[] tags, boolean closingToo) {
        	if (pos >= source.length())
        		return null;
        	if (closingToo && source.charAt(pos) == '/') 
        		pos++;
        	for (int i=0; i<tags.length; i++)
        		if (source.regionMatches(true,pos,tags[i],0,tags[i].length()))
        			return tags[i];
        	return null;
        }
        
        static boolean isTag(String source, int pos, String tag)
                                     {
                                         //return String.Compare(source,pos,tag,0,tag.length(),true)==0;
                                         return source.regionMatches(true,pos,tag,0,tag.length());
                                     }
        static int skipTagBody(String source, int pos, String tag)
                             {
                                 while((pos = source.indexOf("<",pos)) != -1)
                                 {
                                     pos++;
                                     if(isComment(source,pos))
                                     {
                                         pos = skipComment(source,pos+3);
                                         continue;
                                     }
                                     //if(source[pos] == '/' && String.Compare(source,pos+1,tag,0,tag.length(),true)==0)
                             		 if(source.charAt(pos) == '/' && source.regionMatches(true,pos+1,tag,0,tag.length()))
                                     {
                                         int i = source.indexOf('>',pos+tag.length());
                                         return i+1;
                                     }
                                 }
                                 return source.length();
                             }
        
        static String getTagAttr(String source, int pos, int max, String tag, String attr) {
            while((pos = source.indexOf("<",pos)) != -1 && (max <= 0 || pos < max)) {
                pos++;
                if(source.regionMatches(true,pos,tag,0,tag.length())) {
                    int i = Array.indexOfCase(source,pos+tag.length()+1,max,attr);
                    if (i > 0){
                    	//return Array.parseBetween(source, i + attr.length(), "\"", "\"");
                    	int off = i + attr.length();
                    	int qpos = Array.indexOf(source, off, quotes);
                    	if (qpos != -1){
                    		String q = source.substring(qpos,qpos+1);
                    		return Array.parseBetween(source, off, q, q);
                    	}
                    }
                }
            }
            return null;
        }

        /**
         * @param source - string containing html
         * @param btag - the name of the tag to look for. eg 'html' instead of '<html>'
         * @param pos - starting position of search.
         * @return string containing the content of the first tag found. If no tag found, return null
        */
        static ArrayList<String> getTagContent(String source, String tag) {
            ArrayList<String> tagCont = new ArrayList<String>();
            String ftagr = "<\\s*" + tag + "[^>]*>(.*?)<\\s*/\\s*" + tag + ">";
            String exttagr = "<\\s*[a-z]+[^>]*>(.*?)<\\s*/\\s*[a-z]+>";
            String btagr = "<\\s*" + tag + "[^>]*>(.*?)";
            String etagr = "<\\s*/\\s*" + tag + ">";
            Pattern ftagc = Pattern.compile(ftagr);
            Matcher ftagcm = ftagc.matcher(source);
            while(ftagcm.find()) {
				String fin = ftagcm.group().replaceAll(btagr, "").replaceAll(etagr, "").replaceAll(exttagr, "");
				tagCont.add(fin);
            }
            return tagCont;
        }

        static ArrayList<String> getMetaContByProp(String source, String property) {
            ArrayList<String> mCont = new ArrayList<String>();
            String ptoken = "property=\"";
            String ctoken = "content=\"";
            int mbpos = source.indexOf(LT+META);
            int mepos = source.indexOf(GT, mbpos);
            while(mbpos != -1 && mepos != -1) {
                String fmc = source.substring(mbpos, mepos);
                int mpbpos = fmc.indexOf(ptoken);
                int mpepos = fmc.indexOf("\"", mpbpos+ptoken.length());
                if(mpbpos != -1 && mpepos != -1) {
                    if (fmc.substring(mpbpos+ptoken.length(), mpepos).equalsIgnoreCase(property)) {
                        int mcbpos = fmc.indexOf(ctoken);
                        int mcepos = fmc.indexOf("\"", mcbpos+ctoken.length());
                        if(mcbpos != -1 && mcepos != -1)
                            mCont.add(fmc.substring(mcbpos+ctoken.length(), mcepos));
                    }
                }
                mbpos = source.indexOf(LT+META, mepos+1);
                mepos = source.indexOf(GT, mbpos);
            }
            return mCont;
        }

        /**
         * @param source - string containing html
         * @return string, title for the page
        */
        static String extractTitle(String source) {
            StringBuilder sb = new StringBuilder();
            ArrayList<String> tTagC = getTagContent(source, "title");
            ArrayList<String> mTitle = getMetaContByProp(source, "og:title");
            String tit = "", mtit = "";
            if (mTitle.size() != 0)
                mtit = mTitle.get(0);
            if (tTagC.size() != 0) {
                tit = tTagC.get(0);
                addText(sb, tit);
                tit = sb.toString();
            }
//TODO if there is netiher title not og:title, the tit will be == "" and code will return "", right?
//TODO what is the point assigning contents of h1-h6 tags to the entire document if it can be applied to texts appearing before these tags?
//TODO the only point to use LevenshteinDistance is to compare it to news item itself
            /*
            ArrayList<String> hTagC = getTagContent(source, "h[1-6]");
            for (String h1 : hTagC) {
                addText(sb, h1);
                String htit = sb.toString();
                if (Str.levenshteinDistance(htit, tit) > 0.75 || Str.levenshteinDistance(htit, mtit) > 0.75)
                    return htit;
            }
            */
//TODO consider use if Str.simpleTokenizedProximity instead of Str.levenshteinDistance ?
            if (Str.levenshteinDistance(mtit, tit) > 0.75)
                return mtit;
            return tit;
        }

        static int skipTag(String source, int pos, String tag){
        	int depth = 0;
        	String close = "/"+tag;
            while((pos = source.indexOf('<',pos)) != -1){
                pos++;
				if(source.regionMatches(true,pos,close,0,tag.length())){
                    int i = source.indexOf('>',pos+tag.length());
            		if (depth == 1)
            			return i + 1;//outer closure
            		else
            			depth--;
				} else
                if(source.regionMatches(true,pos,tag,0,tag.length())){
                    int i = source.indexOf('>',pos+tag.length());
                    if (i > 0) //open new level
                    	depth++;
                }
            }
            return -1;
        }
        
        static int look4Tag(String source, int pos, String tag){
            while((pos = source.indexOf('<',pos)) != -1){
                pos++;
                if(source.regionMatches(true,pos,tag,0,tag.length())){
                    int i = source.indexOf('>',pos+tag.length());
                    return i+1;
                }
            }
            return -1;
        }

        //TODO:unescape HTML code - make this more efficient, using code from addText above
        //http://stackoverflow.com/questions/994331/java-how-to-decode-html-character-entities-in-java-like-httputility-htmldecode
        public static String decodeHTML(String str) {
        	return Array.replace(str, etokens, echars);
        }

        public static String encodeHTML(String str) {
        	//TODO: use etokens, echars
        	if (str == null)
        		return str;
        	str = str.replaceAll("&", "&amp;");
        	str = str.replaceAll("<", "&lt;");
        	str = str.replaceAll(">", "&gt;");
        	return str;
        }
        
        static int skipTagInteriors(String str,int pos) {
        	int depth = 0;
        	for (;;pos++){
            	pos = Array.indexOf(str, pos, LTGT);
            	if (pos == -1)
            		return -1;
            	char c = str.charAt(pos);
            	if (c == LT)
            		depth++;
            	else {//GT
            		if (depth > 0)
            			depth--;
            		else
            			return pos;
            	}
        	}
        }

    	//TODO: unittest
    	public static String convertMD(String md, List links, List images) {
    		StringBuilder sb = null;
    		int fromIndex = 0;
    		for (;;) {
    			int i1 = md.indexOf('[', fromIndex);
    			if (i1 >= fromIndex) {
    				int i2 = md.indexOf(']', i1+1);
    				if (i2 > i1) {
    					int i3 = md.indexOf('(', i2+1);
    					if (i3 > i2) {
    						int i4 = md.indexOf(')', i3+9);//+8 for non empty link url
    						if (i4 > i3) {
    							String text = md.substring(i1+1,i2).trim();
    							if (AL.isURL(text)) {
    								text = HtmlStripper.stripHtmlAnchor(text);
    								//https://stackoverflow.com/questions/10438008/different-behaviours-of-treating-backslash-in-the-url-by-firefox-and-chrome
    								text = text.replace("\\", "");//backslah was seen in lnik text on some MD-s from Reddit (like links to Zoom videos) 
    							}
    							String url = md.substring(i3+1,i4).trim();
                    			url = HtmlStripper.stripHtmlAnchor(url);
                    			if (AL.isURL(url)) {
                    				int i0;
                    				if (i1 > 0 && md.charAt(i1-1) == '!') {//image url
                    					i0 = i1-1;
                    					if (images != null)
                    						images.add(new String[]{url,text});
                    				} else {
                    					i0 = i1;
                    					if (links != null)
                    						links.add(new String[]{url,text});
                    				}
                    				if (sb == null)
                    					sb = new StringBuilder();
                    				sb.append(md.substring(fromIndex,i0));
                    				if (!text.equals(url))
                    					sb.append(text);
                    				fromIndex = i4 + 1;
//System.out.println(md.substring(fromIndex)); 	
                    				continue;
    							}
    						}
    					}
    				}
    			}
    			if (sb != null)
    				sb.append(md.substring(fromIndex));
    			break;
    		}
    		return sb != null ? sb.toString() : md;
    	}

    	public static void main(String args[]) {
    		String md = "a[b](http://x)c![d](http://y)e[ f ](http://z)g";
    		ArrayList links = new ArrayList();
    		System.out.println(convertMD(md,links,null));
    		System.out.println(links);
    	}

}

