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
package net.webstructor.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.cat.StringUtil;
import net.webstructor.core.Environment;

public class ReportWriter {
	//public static final String[] nodecolors = {"#FFFF00","#00FF00","#00FFFF","#FF00FF","#FF0000","#0000FF"};
	public static final String[] nodecolors = {"#00FF00","#0000FF","#00FFFF","#FF00FF","#FF0000","#0000FF"};
	public static final String[] linkcolors = {"#00007F","#007F7F","#007F00","#7F7F00","#7F0000","#7F007F"};
	
	int maxLength = 300;
	protected Writer writer;
	protected Environment env;

	public static ReportWriter reporter(Environment env,String format,Writer writer){
		if ("json".equals(format))
			return new JsonReporter(env,writer);
		else
			return new ReportWriter(env,writer);
	}
	
	public ReportWriter(Environment env,Writer writer){
		this.env = env;
		this.writer = writer;
	}
	
	public ReportWriter(Environment env,String path){
		this.env = env;
		File file = env.getFile(path);
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
            env.error(e.toString(),e);
		} catch (FileNotFoundException e) {
            env.error(e.toString(),e);
		}
	}
	
	public static final String imgStyle = "height:auto;width:140px;"; 
	public static String img(String url, String src) {
		return img(url, imgStyle, src);
	}
	public static String img(String url, String style, String src) {
		String s = style == null ? "" : "style=\""+style+"\"";
		return url != null
				? "<a href=\""+url+"\" target=\"_blank\" ><img "+s+" src=\""+src+"\"/></a>"
				: "<img "+s+" src=\""+src+"\"/>";
	}

	public boolean needsId(){
		return false;
	}
	
	public String buildName(String id, String name, String surname) {
		StringBuilder sb = new StringBuilder();
		if (!AL.empty(name) || !AL.empty(surname)){
			if (!AL.empty(name))
				sb.append(name).append(' ');
			if (!AL.empty(surname))
				sb.append(surname);
			sb.append(' ');
		} else
			sb.append(id).append(' ');
		return sb.toString();
	}
	
	public void initReport(String title, Date since, Date until, String base){
		try {
			writer.append("<html><head><title>"+net.webstructor.al.Writer.capitalize(title)+"</title>\n");
			writer.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n");
	
			//http://stackoverflow.com/questions/1341089/using-meta-tags-to-turn-off-caching-in-all-browsers
			writer.append("<meta http-equiv=\"cache-control\" content=\"max-age=0\" />\n");
			writer.append("<meta http-equiv=\"cache-control\" content=\"no-cache\" />\n");
			writer.append("<meta http-equiv=\"expires\" content=\"0\" />\n");
			writer.append("<meta http-equiv=\"expires\" content=\"Tue, 01 Jan 1980 1:00:00 GMT\" />\n");
			writer.append("<meta http-equiv=\"pragma\" content=\"no-cache\" />\n");
			
			writer.append("<style> td { vertical-align: top; } body {background:#ffffd8;font-family: Helvetica,Arial,sans-serif} .line0 { background: #ffffc8; } .line1 { background: #ffffE8; } </style>");
			
			if (!AL.empty(base)) {
				String header = 
					"  <link rel=\"stylesheet\" href=\""+base+"/ui/jquery-ui-1.11.4.custom/jquery-ui.css\">\n" + 
					"  <link rel=\"stylesheet\" href=\""+base+"/ui/aigents-wui.css\">\n" + 
					"  <script src=\""+base+"/ui/jquery-1.11.1.js\"></script>\n" + 
					"  <script src=\""+base+"/ui/jquery-ui-1.11.4.custom/jquery-ui.js\"></script>\n" + 
					"  <script type=\"text/javascript\" src=\""+base+"/ui/aigents-al.js\"></script>\n" + 
					"  <script type=\"text/javascript\" src=\""+base+"/ui/aigents-graph.js\"></script>\n";
				writer.append(header);
			}
			
			writer.append("</head><body>\n");
			
			writer.append("<a title=\"Go to Aigents Home\" target=\"_blank\" href=\"https://aigents.com/\"><img src=\"https://aigents.com/ui/img/aigent32.png\"/></a>");
			writer.append("<span style=\"font-size:x-large;\">"+title+"</span><br>\n");
			
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}

	public void initPeer(String id, String name, String surname, String perPeriod, Date since, Date until){
		try {
			String period = (since == null || until == null) ? "" : " (" +(since == null ? "" : Time.day(since,false))+ " - " +(until == null ? "" : Time.day(until,false))
					+ (perPeriod == null ? "" : ", "+perPeriod ) + ")";
			writer.append("<br><span style=\"font-size:x-large;\">").append(buildName(id,name,surname)).append("</span>\n");
			writer.append("<span style=\"font-size:large;\">").append(period).append("</span><br>\n");
		} catch (IOException e) {
            env.error("Reporter "+e.toString(),e);
		}		
	}
	
	public void graph(String id, String graph, String colors, int layout, int radius) {
		try {
			writer.append("<br><div id=\"wrapper_"+id+"\" style=\"width:100%;height:100%\"/>");
			//https://flaviocopes.com/javascript-unicode/
			writer.append("<script charset=\"utf-8\">\n");
			writer.append("var graph_text_data = \"");
			graph = graph.replace("\"", "\\\"");//encode " with \"
			graph = graph.replace("\\\\\"", "\\\"");//fix encodings of \" to \\" back to \"
			writer.append(graph);
			writer.append("\";\n");
			writer.append("GraphUI.request_graph_inline(\"svg_inline_"+id+"\", {text : graph_text_data, node_radius:"+radius+", layout_directions:"+layout+", builder : function(text) {var config = {colors:{"+colors+"},labeled_links:true};return GraphCustom.build_graph(text,{weighted:true,linktypes:null},config);}}, \"svg_widgets_"+id+"\", document.getElementById(\"wrapper_"+id+"\"));\n");
			writer.append("</script><br>");
		} catch (IOException e) {
            env.error("Reporter "+e.toString(),e);
		}
	}

	public void graph(String id,String title, String graph, String[] nodes, String[] links, int layout, int radius) {
		if (graph == null)
			return;
		if (AL.empty(graph))
			return;
		subtitle(title);
		graph = graph.replace(".\n", "\\n\\\n ");//encode JavaScrip in-string linebreaks wtih extra separating space
		//text = "a likes b 100\\n\\\n"+"b loves d 100\\n\\\n";
		StringBuilder colors = new StringBuilder();
		if (nodes != null )for (int i = 0; i < nodes.length; i++) {
			if (colors.length() > 0)
				colors.append(',');
			colors.append("\""+nodes[i]+"\":\""+nodecolors[i % nodecolors.length]+"\"");
		}
		if (links != null )for (int i = 0; i < links.length; i++) {
			if (colors.length() > 0)
				colors.append(',');
			colors.append("\""+links[i]+"\":\""+linkcolors[i % linkcolors.length]+"\"");
		}
		graph(String.valueOf(System.currentTimeMillis()), graph, colors.toString(), layout, radius);
	}
	
	//0000FF - blue
	//ffffd8 - back
	public static String[] rankBackgroundForeground(int rank){
        int r = 0xFF - (0xFF * rank / 100);
        int g = 0xFF - (0xFF * rank / 100);
        int b = 0xd8;// - (0xd8 * rank / 100);
        String background = StringUtil.toHexString(r,2)+StringUtil.toHexString(g,2)+StringUtil.toHexString(b,2);
        int foreground = rank >=50 ? 0xFFFFFF : 0;
        //return new String[]{background+background+background,StringUtil.toHexString(foreground,6)};
        return new String[]{background,StringUtil.toHexString(foreground,6)};
    }
	
	public static String[] rankBackgroundForegroundBW(int rank){
        int c = 0xFF - (0xFF * rank / 100);
        String background = StringUtil.toHexString(c,2);
        int foreground = rank >=50 ? 0xFFFFFF : 0;
        //return new String[]{background+background+background,StringUtil.toHexString(foreground,6)};
        return new String[]{background+background+background,StringUtil.toHexString(foreground,6)};
    }

	public static boolean isPairOfStringIntegerArray(Object obj){
		if (obj instanceof Object[] && ((Object[])obj).length == 2){
			Object[] pair = (Object[])obj;
			if (isStringIntegerArray(pair[0]) && isStringIntegerArray(pair[1]))
				return true;
		}
		return false;
	}

	//TODO: make it configurable to display second row or not?
	public static String toPairOfStringIntegerArray(Object obj){
		Object[] pair = (Object[])obj;
		//return toStringIntegerArray(pair[0])+"<br>"+toStringIntegerArray(pair[1]);
		return toStringIntegerArray(pair[0]);
	}
	
	public static boolean isStringIntegerArray(Object obj){
		if (obj instanceof Object[][] && ((Object[][])obj).length > 0){
			Object[] first = (Object[])((Object[][])obj)[0];
			if (first[0] instanceof String && first[1] instanceof Integer)
				return true;
		}
		return false;
	}
	
	public static String toStringIntegerArray(Object obj){
		Object[] objs = (Object[])obj;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < objs.length; i++){
			Object[] item = (Object[])objs[i];
			int rank = ((Integer)item[1]).intValue();
			if (sb.length() > 0)
				sb.append("&nbsp;");
			//TODO: decide which rendering to use - this one is rendering gradual colors with inverted text size
			/*
			String[] colors = rankBackgroundForeground(rank);
            sb.append("<span style=\"background-color:#").append(colors[0]).append(";")
            	.append("color:").append(colors[1]).append(";")
            	.append("font-size:").append((rank/6)+4).append(";")
            	.append("\">").append(item[0]).append("</span>");
            	*/
            sb.append("<span style=\"background-color:lightblue;")
        		.append("font-size:").append((rank/4)+4).append(";")
        		.append("\">").append(item[0]).append("</span>");
		}
		return sb.toString();
	}
	
	protected String toString(Boolean value){
		//TODO: mu likes as +/-, ad 1/0
		//return ((Boolean)obj).booleanValue() ? "+" : "-";
		return value.booleanValue() ? "1" : "0";
	}
	
	public static String trimHTML(String s, int max) {
		if (s.length() > max){
			//avoid HTML tag breaking
			int length = max;
			int closingTag = s.indexOf("</",length);
			if (closingTag != -1)
				closingTag = s.indexOf(">",closingTag);
			if (closingTag != -1)
				length = closingTag + 1;
			s = s.substring(0, length) + "<br>...";
		}
		return s;
	}
	
	public String toString(Object obj){
		if (obj instanceof Boolean){
			//return ((Boolean)obj).booleanValue() ? "+" : "-";
			return toString((Boolean)obj);
		}
		if (obj instanceof Date){
			//TODO: with Time class
			DateFormat day_format = new SimpleDateFormat("yyyy-MM-dd");
			return day_format.format((Date)obj);
		}
		if (obj instanceof String){
			String s = trimHTML((String)obj,maxLength);
			s = s.replace("\n","<br>");
			//TODO: annotate URLs as links in-text
			if (AL.isURL(s) && s.indexOf(' ') == -1)
				return "<a target=\"_blank\" href=\""+s+"\">"+s+"</a>";
			return s;
		}
		if (isPairOfStringIntegerArray(obj))
			return toPairOfStringIntegerArray(obj);
		if (isStringIntegerArray(obj))
			return toStringIntegerArray(obj);
		if (obj instanceof Object[]){
//TODO: hyperlinks
			Object[] objs = (Object[])obj;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < objs.length; i++){
				if (sb.length() > 0){
					if (objs[i] instanceof String && AL.isURL((String)objs[i]) && ((String)objs[i]).indexOf(' ') == -1)
						sb.append("<br>");
					else
						sb.append(", ");
				}
				sb.append( toString(objs[i]) );
			}
			return sb.toString();
			//return Writer.toString((Object[])obj,null,"",", ","");
		}
		return obj == null ? "" : obj.toString();
	}

	public void subtitle(String title){
		try {
			writer.append("<br><b><u>"+net.webstructor.al.Writer.capitalize(title)+"</u></b><br>\n");
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}
	
	public void table(String id, String title,String[] header, Object[][] rows, int minPercent, int minCount){
		table(id, title, header, rows, minPercent, minCount, 0);
	}

	public void table(String id, String title,String[] header, Object[][] rows, int minPercent, int minCount, int maxCount){
		if (AL.empty(rows))
			return;

		//do we have anythign to render at all?
		int visible = 0;
		for (int i = 0; i < rows.length && (maxCount == 0 || i < maxCount); i++){
			Object[] row = rows[i];
			if (row == null)
				return;
			int columns = Math.min(header.length, row.length);
			boolean minPercentExceeded = false;
			for (int j = 0; j < columns; j++){
				if (header[j] == null)
					continue;
				boolean percent = header[j].indexOf('%') != -1 && row[j] instanceof Integer;
				if (percent){
					int percentValue = ((Integer)row[j]).intValue();
					if (minPercent > percentValue && (percentValue == 0 || visible > minCount))
						minPercentExceeded = true;
					break;
				}
			}
			if (minPercentExceeded)
				break;
			visible++;
		}
		if (visible == 0)
			return;
		try {
			subtitle(title);
			writer.append("<table border=\"1\" style=\"border-collapse:collapse;\"><tr>");
			for (int i = 0; i < header.length; i++){
				if (header[i] == null)
					continue;
				writer.append("<th class=\"line1\" align=\"left\">");
				writer.append(header[i]);
				writer.append("</th>");
			}
			writer.append("</tr>\n");
			for (int i = 0; i < visible; i++){
				Object[] row = rows[i];
				if (row == null){
					break;
				}
				writer.append("<tr class=\"line"+i%2+"\">\n");
				int columns = Math.min(header.length, row.length);
				for (int j = 0; j < columns; j++){
					if (header[j] == null)
						continue;
					//writer.append("<td valign=top>");
					if (j == 0)//don't ever wrap first column!?	
						writer.append("<td style=\"white-space:nowrap;\">");
					else	
						writer.append("<td>");
					boolean percent = header[j].indexOf('%') != -1 && row[j] instanceof Integer;
					if (percent)
						writer.append("<div style=\"background-color:lightblue;width:"+((Integer)row[j]).intValue()+"px;\">");
					String body = toString(row[j]);
					writer.append(body);
					if (percent)
						writer.append("</div>");
					writer.append("</td>");
				}
				writer.append("</tr>\n");
			}
			writer.append("</table>\n");
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}
	
	public void closePeer(){
		//TODO
	}
	
	public void closeReport(){
		try {
			writer.append("</body></html>");
			writer.close();
		} catch (IOException e) {
            env.error(e.toString(),e);
		}
	}
}
