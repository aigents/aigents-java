/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.util;

import java.text.DecimalFormat;
import net.webstructor.mine.core.Id;
import net.webstructor.mine.core.Item;
import net.webstructor.mine.core.CoreRelation;
import net.webstructor.mine.core.Relation;
import net.webstructor.mine.core.RelevantItem;
import net.webstructor.mine.core.BasicRelation;


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

    public static String toString(Relation[] rels)
    {
        return toString(rels,"\n",";",",");
    }

    public static String toString(CoreRelation[] rels,StringContext context)
    {
        return toString((Relation[])rels,context.m_httpLineBreak,context.m_cmdDelimeters,context.m_itemDelimeters);
    }

    public static String toString(Relation[] rels,String relDelim,String fieldDelim,String itemDelim)
    {
        StringBuffer sb = new StringBuffer();
        if (rels!=null)
            for (int i=0;i<rels.length;i++)
                sb.append(toString(rels[i],fieldDelim,itemDelim)).append(relDelim);
        return sb.toString(); 
    }

    public static String toString(Item[] rels,String br)
    {
        StringBuffer sb = new StringBuffer();
        if (rels!=null)
        {
            for (int i=0;i<rels.length;i++)
            {
                sb
                .append(rels[i].getId())
                .append(' ')
                .append(rels[i].getName())  
                .append(br);
            }
        }
        return sb.toString(); 
    }

    public static String toString(RelevantItem[] rels,String br)
    {
    	StringBuffer sb = new StringBuffer();
        if (rels!=null)
        {
            for (int i=0;i<rels.length;i++)
            {
                sb
                    //.append(rels[i].getId()).append(' ')
                    .append(rels[i].getRelevantId()).append(' ')
                    .append(fDoubleHigh.format(rels[i].getEvidence())).append(' ')
                    .append((rels[i].getConfirmation()==Id.NOTCONFIRMED?"-":String.valueOf(rels[i].getConfirmation()))).append(' ')//20070528
                    .append(fDoubleHigh.format(rels[i].getRelevance())).append(' ')
                    .append(fDoubleLow.format(rels[i].getRelative())).append(' ')
                    .append(rels[i].getRelevantName()).append(br);
            }
        }
        return sb.toString(); 
    }

    public static String toString(Relation rel,String fieldDelim,String elementDelim)
    {
    	StringBuffer sb = new StringBuffer();
        sb
            .append(rel.getType()).append(fieldDelim)
            .append(rel.getId()).append(fieldDelim)
            .append(rel.getArity()).append(fieldDelim)
            .append(toString(rel.getIds(),elementDelim)).append(fieldDelim)
            .append(toString(rel.getPosEvidence())).append(fieldDelim)
            .append(toString(rel.getNegEvidence())).append(fieldDelim)
            .append(toString(rel.getConfirmation())).append(fieldDelim);
        String name = rel.getName();
        if (name!=null)
            sb.append(name);
        return sb.toString();
    }


    /*
     * <rel type="12" id="10641" arity="2" ids="110640,99235" pos="9.0" confirm="1">first relation</rel>
     */
    public static String toXML(Relation rel)
    {
    	StringBuffer sb = new StringBuffer("<rel ");
        sb
            .append(" type=\"").append(rel.getType()).append("\"")
            .append(" id=\"").append(rel.getId()).append("\"")
            .append(" arity=\"").append(rel.getArity()).append("\"")
            .append(" ids=\"").append(toString(rel.getIds(),",")).append("\"")
            .append(" pos=\"").append(toString(rel.getPosEvidence())).append("\"")
            .append(" confirm=\"").append(toString(rel.getConfirmation())).append("\"")
            .append(">").append(toXML(rel.getName())).append("</rel>");
        return sb.toString();
    }

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
        return i==Id.NOTCONFIRMED? "?": Integer.toString(i); 
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
    
    public static Relation parseRelation(String line)
    {
    	BasicRelation rel = new BasicRelation();
        int[][] chunk = StringUtil.toChunks(line,",;",false);
        rel.m_type = toInt(substring(line,chunk[0][0],chunk[0][1]));
        rel.m_id = toInt(substring(line,chunk[1][0],chunk[1][1]));
        int arity = toInt(substring(line,chunk[2][0],chunk[2][1]));
        if (arity>0)
        {
            rel.m_ids = new int[arity];
            for (int i=0;i<arity;i++)
            {
                rel.m_ids[i] = toInt(substring(line,chunk[3+i][0],chunk[3+i][1]));
            }
        }
        rel.m_posEvidence = toFloat(substring(line,chunk[3+arity][0],chunk[3+arity][1]));
        //rel.m_negEvidence = toDouble(substring(line,chunk[4+arity][0],chunk[4+arity][1]));
        rel.m_confirmation = StringUtil.toInt(substring(line,chunk[5+arity][0],chunk[5+arity][1]));
        rel.m_name = ((6+arity)<chunk.length && chunk[6+arity][0]<line.length())? line.substring(chunk[6+arity][0]): null;
        return rel;
    }

    public static int toInt(String s)
    {
        return s.equals("?")?Id.NOTCONFIRMED:Integer.valueOf(s).intValue();
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
