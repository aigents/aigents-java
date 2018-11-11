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
package net.webstructor.gram.util;

import java.text.DecimalFormat;

import net.webstructor.gram.core.Item;
import net.webstructor.gram.core.Ngram;
import net.webstructor.gram.core.LinkItem;
import net.webstructor.gram.core.StringItem;
import net.webstructor.gram.core.CharacterItem;
import net.webstructor.gram.core.MemoryType;
import net.webstructor.gram.core.MemoryStore;

public final class StringUtil
{
    public static final String DELIMITERS        = "\\\r\n\t\"\' .,;:()[]{}?!&%$#@+=*-";
    public static final String PATTERNDELIMITERS = "\t\"\'.,;:()[]{}?!";
    public static final String TOKENDELIMITERS   = "\\\r\n &%$#@+=*-";
    
    //public const String EMAILREGEXP = "\ [^\,^\ ^\@]+\@[^\@^\$]+";

    /*
    static NumberFormatInfo s_formatInfo = null;

    static NumberFormatInfo getFormatInfo()
    {
        if (s_formatInfo==null)
        {
            s_formatInfo = new NumberFormatInfo();
            s_formatInfo.CurrencyDecimalSeparator = ".";
        }
        return s_formatInfo;
    }
    */

    static DecimalFormat fDoubleHigh;
    static DecimalFormat fDoubleLow;
    static DecimalFormat fPercent;
	static 
	{
		fDoubleHigh = new DecimalFormat("#0.000000000");
		java.text.DecimalFormatSymbols dfsHigh = fDoubleHigh.getDecimalFormatSymbols();  
		dfsHigh.setDecimalSeparator('.');
		fDoubleHigh.setDecimalFormatSymbols(dfsHigh);

		fDoubleLow = new DecimalFormat("#0.00");
		java.text.DecimalFormatSymbols dfsLow = fDoubleLow.getDecimalFormatSymbols();  
		dfsLow.setDecimalSeparator('.');
		fDoubleLow.setDecimalFormatSymbols(dfsLow);
		
		fPercent = new DecimalFormat("000");
	}
    
    
    static String substring(String s,int from,int len)
    {
        return s.substring(from,from+len);
    }

    static int indexOfAny(String s,char[] chars,int start)
    {
    	int idx = -1;
    	if (chars!=null)
    		for (int i=0;i<chars.length;i++)
    		{
    			int x = s.indexOf(chars[i],start);
    			if (x>=0)
    				if (idx==-1 || x<idx)
    					idx = x;
    		}
    	return idx;
    }
    
    public static int[][] toChunks(String src,String delim,boolean table)
    {
    	int[][] chunks = null;
        if (src!=null && delim!=null)//20070519
        {
        	char[] delChars = delim.toCharArray();
        	// need two passes
            for (int pass=1;pass<=2;pass++)  
            {
                int count = 0;
                int startIndex = 0;
                int stopIndex;
                for (;;)
                {
                    stopIndex = indexOfAny(src,delChars,startIndex);
                    if (stopIndex==startIndex && !table)//in "table" mode, count empty strings 
                        startIndex+=1;
                    else
                    if (startIndex<stopIndex || (table && startIndex==stopIndex)) // stopIndex != -1
                    {
                        if (pass==2)
                            chunks[count] = new int[] {startIndex,stopIndex-startIndex};
                        count++;  
                        startIndex = stopIndex + 1;
                    }
                    else // stopIndex == -1
                    {
                        int reminder = src.length() - startIndex;
                        if (reminder>0)
                        {
                            if (pass==2)
                                chunks[count] = new int[] {startIndex,reminder};
                            count++;  
                        }
                        if (pass==1)
                            chunks = new int[count][];
                        break;
                    }
                }
            }
        }
        return chunks;
    }

    public static String[] toTokens(String src,String delim,boolean table)
    {
        String[] tokens = null;
        int[][] chunks = toChunks(src,delim,table);
        if (chunks!=null && chunks.length>0)
        {
            tokens = new String[chunks.length];
            for (int i=0;i<chunks.length;i++)
                tokens[i] = substring(src,chunks[i][0],chunks[i][1]);
        }
        return tokens;
    }

    public static int[] parseIntArray(String src,String delims)
    {
        int[] ints = null;
        int[][] chunks = toChunks(src,delims,false);
        if (chunks!=null && chunks.length>0)
        {
            ints = new int[chunks.length];
            for (int i=0;i<chunks.length;i++)
                ints[i] = Integer.valueOf(substring(src,chunks[i][0],chunks[i][1])).intValue();
        }
        return ints;
    }

    public static String toString(Item[] items,MemoryStore store,String relDelim,String fieldDelim,String itemDelim)
    {
        StringBuffer sb = new StringBuffer();
        if (items!=null)
            for (int i=0;i<items.length;i++)
                sb.append(toString(items[i],store,fieldDelim,itemDelim)).append(relDelim);
        return sb.toString(); 
    }

    public static String toString(int ids[],MemoryStore store,String openDelim,String closeDelim)
    {
    	StringBuffer sb = new StringBuffer();
    	if (ids!=null) for (int i=0;i<ids.length;i++)
    		sb.append(store.getItem(ids[i]).toString(store,openDelim,null,closeDelim));
    	return sb.toString();
    }
    
    public static String toString(Item it,MemoryStore store,String fieldDelim,String elementDelim)
    {
    	StringBuffer sb = new StringBuffer();
        sb
            .append(it.getType()).append(fieldDelim)
            .append(it.getId()).append(fieldDelim)
            .append(it.getArity()).append(fieldDelim)
            .append(toString(it.getIds(),elementDelim)).append(fieldDelim)
            .append(toString(it.getEvidence())).append(fieldDelim) // positive evidence
            .append(toString(0)).append(fieldDelim) // negative evidence
            .append(toString(0)).append(fieldDelim); // confirmaion 
        String name = it.toString(store,Format.m_openArrayDelim,null,Format.m_closeArrayDelim);
        if (name!=null)
            sb.append(name);
        return sb.toString();
    }
    
    public static String toString(Item it,MemoryStore store,Format format)
    {
    	StringBuffer sb = new StringBuffer();
        sb
            .append(it.getType()).append(Format.m_fieldDelim)
            .append(it.getId()).append(Format.m_fieldDelim)
            .append(it.getArity()).append(Format.m_fieldDelim)
            .append(toString(it.getIds(),Format.m_elementDelim)).append(Format.m_fieldDelim)
            .append(toString(it.getEvidence())).append(Format.m_fieldDelim) // positive evidence
            .append(toString(0)).append(Format.m_fieldDelim) // negative evidence
            .append(toString(0)).append(Format.m_fieldDelim); // confirmaion 
        String name = it.toString(store,Format.m_openArrayDelim,Format.m_innerStringDelim,Format.m_closeArrayDelim);
        if (name!=null)
            sb.append(name);
        return sb.toString();
    }


    /**
     * <item type="12" id="10641" arity="2" ids="110640,99235" pos="9.0" confirm="1">first item</item>
     */
    /*
    public static String toXML(Item it)
    {
    	StringBuffer sb = new StringBuffer("<item ");
        sb
            .append(" type=\"").append(it.getType()).append("\"")
            .append(" id=\"").append(it.getId()).append("\"")
            .append(" arity=\"").append(it.getArity()).append("\"")
            .append(" ids=\"").append(toString(it.getIds(),",")).append("\"")
            .append(" pos=\"").append(toString(it.getEvidence())).append("\"")
            .append(">").append(toXML(it.getLiteralName())).append("</item>");
        return sb.toString();
    }
    */
    
    public static String toXML(String s)
    {
    	if (s==null)
    		return "";
    	if (s.indexOf('&')!=-1)
    	{
    		StringBuffer sb = new StringBuffer();
    		for (int i=0;i<s.length();i++)
    		{
    			char c = s.charAt(i); 
    			if (c=='&')
    				sb.append("&amp;");
    			else
    				sb.append(c);
    		}
    		return sb.toString();
    	}
    	return s;
    }
    
    public static String toString(int i)
    {
        return i==MemoryType.UNDEFINED? "?": Integer.toString(i); 
    }

    public static String toHexString(int i)
    {
        return Integer.toHexString(i); 
    }

    public static String toString(double d)
    {
        return d==0?"0":Double.toString(d); 
    }
    
    public static String toStringLow(double d)
    {
        return d==0?"0":fDoubleLow.format(d); 
    }
    
    public static Item parseItem(String line)
    {
        int[][] chunk = StringUtil.toChunks(line,",;",false);
        int type = toInt(substring(line,chunk[0][0],chunk[0][1]));
        int id = toInt(substring(line,chunk[1][0],chunk[1][1]));
        int arity = toInt(substring(line,chunk[2][0],chunk[2][1]));
        int ids[] = null;
        if (arity>0)
        {
            ids = new int[arity];
            for (int i=0;i<arity;i++)
            {
                ids[i] = toInt(substring(line,chunk[3+i][0],chunk[3+i][1]));
            }
        }
        float posEvidence = toFloat(substring(line,chunk[3+arity][0],chunk[3+arity][1]));
        //float confirmation = StringUtil.toInt(substring(line,chunk[5+arity][0],chunk[5+arity][1]));
        String name = ((6+arity)<chunk.length && chunk[6+arity][0]<line.length())? line.substring(chunk[6+arity][0]): null;
        switch (type)
        {
        case MemoryType.CHARACTER: return new CharacterItem(id,posEvidence,new Character(name.charAt(0)));
        case MemoryType.LINK: return new LinkItem(id,posEvidence,ids[0],ids[1]);
        case MemoryType.STRING: return new StringItem(id,posEvidence,new Ngram(ids));
        }
        return null;
    }

    public static int toInt(String s)
    {
        return s=="?"?MemoryType.UNDEFINED:Integer.valueOf(s).intValue();
    }

    public static int toInt(String s,int def)//20070519
    {
    	try {
    		return toInt(s);
    	} catch (Exception e) {
    		return def;
    	}
    }

    public static double toDouble(String s)
    {
        return Double.valueOf(s).doubleValue();
    }
    
    public static float toFloat(String s)
    {
        return Float.valueOf(s).floatValue();
    }
    
    public static float toFloat(String s,float def)//20070519
    {
    	try {
    		return toFloat(s);
    	} catch (Exception e) {
    		return def;
    	}
    }

    public static boolean toBoolean(String s,boolean def)//20070522
    {
    	try {
    		return Boolean.valueOf(s).booleanValue();
    	} catch (Exception e) {
    		return def;
    	}
    }

    public static String toString(int[] ids,String delim)
    {
        StringBuffer sb = new StringBuffer();
        if (ids!=null && ids.length>0)
        {
            sb.append(toString(ids[0]));
            for (int i=1;i<ids.length;i++)
                sb.append(delim).append(toString(ids[i]));
        }
        return sb.toString();
    }

    public static String toString(String[] strs,String br)
    {
        StringBuffer sb = new StringBuffer();
        if (strs!=null && strs.length>0)
        {
            sb.append(strs[0]);
            for (int i=1;i<strs.length;i++)
                sb.append(br).append(strs[i]);
        }
        return sb.toString();
    }

    public static String toStringFrom(String[] strs,int from)
    {
        StringBuffer sb = new StringBuffer();
        if (strs!=null && strs.length>from)
        {
            sb.append(strs[from]);
            for (int i=from+1;i<strs.length;i++)
                sb.append(" ").append(strs[i]);
        }
        return sb.toString();
    }
    
    public static String toHtml(String[] strs,int[] weightPercents)
    {
        StringBuffer sb = new StringBuffer("<html><body>");
        if (strs!=null && strs.length>0)
        {
            for (int i=0;i<strs.length;i++)
            {
                if (i>0)
                    sb.append(" ");
                int background=0xFFFFFF,foreground=0;
                /* 
                // version 1
                {
                    //CCCCFF - light blue
                    //000040 - dark blue
                    int rg = 0xCC - (0xCC * weightPercents[i] / 255);
                    int bb = 0xFF - ((0xFF-0x40) * weightPercents[i] / 255);
                    background = (rg << 16) | (rg << 8) | bb;
                    foreground = weightPercents[i]>=50? 0xFFFFFF: 0;
                }
                */
                //version 2
                if (weightPercents[i]>=0)
                {   
                    int c = 0xFF - (0xFF * weightPercents[i] / 100);
                    background = (c << 16) | (c << 8) | c;
                    foreground = (weightPercents[i]>=50)?0xFFFFFF:0;
                    sb.append("<font color=\"#").append(toHexString(foreground)).append("\">");
                    sb.append("<span style=\"background-color:#").append(toHexString(background)).append("\">");
                    sb.append(strs[i]);
                    sb.append("</span></font>");
                }
                else
                {
                    sb.append(strs[i]);
                }
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }
    
}
