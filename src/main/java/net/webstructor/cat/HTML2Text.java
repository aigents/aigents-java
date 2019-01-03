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

public class HTML2Text
{
    private static char  LT = '<' ;
    private static char  GT = '>' ;
	private static String SCRIPT="script";
	private static String BODY="body";
	
    public static String convert( String  source ) {
        if( source == null || source.length() == 0) {
           return "" ;
        }
        StringBuffer buf = new StringBuffer(source.length()/2) ;
        int startOfText = 0 ; ;
		int pos=look4Tag(source,0,BODY); // skip html header
		if(pos == -1) pos = 0;
		startOfText = pos;
        if( source.indexOf(LT,pos) < 0 ){
            return source ;
        }
        for(  pos = source.indexOf(LT,pos); pos < source.length(); pos = source.indexOf(LT,pos)  ) {
			if(pos < 0) break;
			if(pos>startOfText && startOfText != -1){
				addText(buf,source.substring(startOfText,pos));
				startOfText = -1;
			}
			pos++;
			if(isComment(source,pos)){
				// skip html comments
				pos = skipComment(source,pos+2);
			} else	if(isTag(source,pos,SCRIPT)){
				//skip javascript
				pos = skipTagBody(source,pos+SCRIPT.length(),SCRIPT);
			} else {
				//skip tag
				pos = source.indexOf(GT,pos);
				if(pos == -1) pos = source.length();
				else pos++;
			}
			startOfText = pos;
        }
		if(pos>startOfText && startOfText != -1){
			addText(buf,source.substring(startOfText,pos));
		}
        return buf.toString() ;
    }
	
	private static final void addText(StringBuffer buf, String str){
		
		char c=' ';
		char pc=' ';
		if(buf.length() >0)
			buf.append(' ');
		for(int i=0;i<str.length();i++){
			c = str.charAt(i);
			if(c == '&'){
                if(str.regionMatches(i,"&nbsp;",0,6)){
                    i+=5;
                    c=' ';

                }
/*
				i++;
				if(str.regionMatches(i,"nbsp;",0,5)){
					i+=4;
					c=' ';
				} else if(str.regionMatches(i,"lt;",0,3)){
					i+=2;
					c = '<';
				} else if(str.regionMatches(i,"gt;",0,3)){
					i+=2;
					c = '>';
				} else if( i<str.length() && str.charAt(i) == '#' ){
                    i++ ;
                    int pos = i ;
                    while( i<str.length() && str.charAt(i) != ';') {
                        i++ ;
                    }
                    if( i<str.length() ) {
                       try{
                           c =  (char) new Integer(str.substring(pos,i)).intValue() ;
                       } catch( Exception ex) {
                           c= ' ' ;
                       }
                    } else {
                        c= ' ' ;
                    }
                }
*/
			} else 	if( c == 0x0a || c == 0x0d){
				c = ' ';
			}
			// skip repeated spaces or spec symbols
			if( c == ' ' && pc == ' ') continue;
			buf.append(c);
			pc = c;
		}
		if(c == ' ' && buf.length() >0) buf.setLength(buf.length()-1);

	}
	private static final boolean isComment(String source, int pos){
		return source.regionMatches(pos,"!--",0,3);
	}
	private static final int skipComment(String source, int pos){
		if((pos = source.indexOf("-->",pos)) != -1){
			return pos+3;
		}
		return source.length();
	}
	private static final boolean isTag(String source, int pos, String tag){
		return source.regionMatches(true,pos,tag,0,tag.length());
	}
	private static final int skipTagBody(String source, int pos, String tag){
		while((pos = source.indexOf("<",pos)) != -1){
			pos++;
			if(isComment(source,pos)){
				pos = skipComment(source,pos+3);
				continue;
			}
			if(source.charAt(pos) == '/' && source.regionMatches(true,pos+1,tag,0,tag.length())){
				int i = source.indexOf('>',pos+tag.length());
				return i+1;
			}
		}
		return source.length();
	}
	private static final int look4Tag(String source, int pos, String tag){
		while((pos = source.indexOf("<",pos)) != -1){
			pos++;
			if(source.regionMatches(true,pos,tag,0,tag.length())){
				int i = source.indexOf('>',pos+tag.length());
				return i+1;
			}
		}
		return -1;
	}

	/*
	public static void main(String arg[]){

		try{
			File fin = new File(arg[0]);
			byte[] b = new byte[(int)fin.length()];
			byte [] b1=null;
			new FileInputStream(fin).read(b);
			System.out.println("Start..");
			long t1 = System.currentTimeMillis();
			String txt = new String(b);
			String res="";
			for(int i=0;i<100;i++)
				res = convert(txt);
			long t2 = System.currentTimeMillis();
			System.out.println("elapsed:"+(t2-t1));
			FileOutputStream out = new FileOutputStream(arg[1]);
			out.write(res.getBytes());
			out.close();

		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	 */
}
