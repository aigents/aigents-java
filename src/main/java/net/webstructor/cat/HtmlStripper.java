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
package net.webstructor.cat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;

import net.webstructor.al.AL;
import net.webstructor.util.Array;

	public class HtmlStripper
	{
		public static final String block_breaker = ". ";
		public static final String word_breaker = " ";//TODO: add CR/LF !?

		static final String[] word_tags = {"br","div","td","tr","span","i","b","u"};
		static final String[] block_tags = {
				//http://www.w3schools.com/html/default.asp
				"p","tr","ul","h1","h2","h3","h4","h5","li","ul",
				//http://www.w3schools.com/html/html5_new_elements.asp
				"aside","bdi","ficaption","header","main","section","summary","article"};
		static final String[] skip_tags = { "style", "script", "svg", "head", "noscript" };
		
    	//http://www.fileformat.info/info/unicode/char/feff/index.htm
    	static final String WS = " \t\n\r\uFEFF";
    	
        static char  LT = '<' ;
        static char  GT = '>' ;
        static char[] LTGT = {LT,GT};
        static String HEAD="head";
        static String BODY="body";
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
        private static final String[] etokens = {"&times;","&quot;","&ndash;","&mdash;", "&minus;", "&amp;","&lt;","&gt;","&nbsp;","&nbsp","&euro;","&cent;","&pound;","&yen;","&copy;","&#169;","&reg;","&#174;","&deg;","&#8482;","&#39;","&#039;","&rarr;","&sbquo;","&laquo;"  ,"&raquo;"  ,"&lsquo;"  ,"&rsquo;"  ,"&ldquo;"  ,"&rdquo;"  ,"&bdquo;","&ldquor;", "&#x3D;","‘" ,"’" ,"“" ,"”" };
        private static char[] echars =          {'*',		'\"',    '–',	    '—',	   '−',		  '&',	'<'   ,'>'   ,' '     ,' '    ,'€'	   ,'¢'     ,'£'      ,'¥'    ,'©'     ,'©'     ,'®'    ,'®'     ,'°'    ,'™'	,	'\'',	'\'', 	'→'      ,'‚'      ,'\"'/*'«'*/,'\"'/*'»'*/,'\''/*'‘'*/,'\''/*'’'*/,'\"'/*'“'*/,'\"'/*'”'*/,'„' 	 ,'„'       , '='     ,'\'','\'','\"','\"'};
        
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
        	return convert(source, breaker, links, null, null, null);
        }
        /**
         * @param images - "postion to string" map of image sources 
        */
        public static String convert( String  source, String breaker, ArrayList links, Map images, Map linksPositions, String path) 
        {
        	ArrayDeque tagStarts = new ArrayDeque();
        	String tag;
    		String currentLinkUrl = null;
    		StringBuilder currentLinkBuf = null;
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
            
            // skip html header
            pos=skipTag(source,0,HEAD);
            if(pos == -1)
            	pos = 0;
            
            //jump to body
            pos=look4Tag(source,pos,BODY); 
            if(pos == -1)
            	pos = 0;

            startOfText = pos;
            if( source.indexOf(LT,pos) < 0 )
            {
                return source ;
            }
            String baseHref = getTagAttr(source,0,pos,BASE,HREF);
            if (AL.empty(baseHref))
            	baseHref = path;
            for(  pos = source.indexOf(LT,pos); pos < source.length(); pos = source.indexOf(LT,pos)  ) 
            {
                if(pos < 0) break;
                if(pos>startOfText && startOfText != -1)
                {
                	StringBuilder chunk = new StringBuilder(); 
                    addText(chunk,source.substring(startOfText,pos));
                    //buf.append(chunk);
                    addText(buf,chunk.toString());//suppress spaces that way!?
                    if (currentLinkBuf != null)
                    	currentLinkBuf.append(chunk);
                    startOfText = -1;
                }
                pos++;
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
	                			if (currentLinkUrl.length() > 0){
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
                        	if (Array.startsWith(name,block_tags) != null) {
                        		if (!closing)
                        			tagStarts.push(new Integer(pos));
                        		else {
                        			int start = tagStarts.isEmpty() ? 0 : ((Integer)tagStarts.pop()).intValue();
                        			if (oldpos - start > 32)//TODO:somethig with this "magic" phrase length
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
                                     c = str.charAt(i);
                                     if(c == '&')
                                     {
                                    	 //try stuff like &#8212
                                    	 //TODO: stuff like &#x2019 '’'
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
                                    	 }
                                    	 else
                                    	 for (int j = 0; j < etokens.length; j++)
                                    		 if (str.regionMatches(i, etokens[j], 0, etokens[j].length())) {
                                    			 i += (etokens[j].length() - 1);
                                    			 c = echars[j];
                                    			 break;
                                    		 }
                                     } 
                                     else
                                     //http://www.adamkoch.com/2009/07/25/white-space-and-character-160/
                                     if( c == 0x0a || c == 0x0d || c == 160 || WS.indexOf(c) != -1)
                                         c = ' ';
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
	}

