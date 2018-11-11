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
package net.webstructor.comm;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonObject;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;

public abstract class HTTP {

	private List cookies = null;
	protected Body body;

	protected HTTP(Body body) {
		this.body = body;
	}

	public static void init() {
		//TODO: make this configurable?
		//https://stackoverflow.com/questions/16541627/javax-net-ssl-sslexception-received-fatal-alert-protocol-version
		java.lang.System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
		//https://stackoverflow.com/questions/6353849/received-fatal-alert-handshake-failure-through-sslhandshakeexception
		//https://bugs.openjdk.java.net/browse/JDK-8151387
		//java.lang.System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
		//java.lang.System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.1,TLSv1");
	}
	
	public static String simpleGet(String url) throws IOException {
		URL u = new URL(url);
        java.net.URLConnection yc = u.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        StringBuilder b = new StringBuilder();
        String i;
        while ((i = in.readLine()) != null) 
            b.append(i);
        in.close();
        return b.toString();
	}

	
	public static String[] simpleGetBin(String url, BufferedOutputStream os, boolean header) throws Exception {
		java.net.HttpURLConnection yc = null;
		int code = 0;
		InputStream in = null;
		for (int retries = 0; retries < 5; retries++){
			URL u = new URL(url);
			yc = (HttpURLConnection)u.openConnection();
			//yc.setRequestProperty("Accept","image/gif, image/x-xbitmap, image/jpeg, image/svg, image/pjpeg, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/x-shockwave-flash, */*");
			//yc.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			//yc.addRequestProperty("Accept-Encoding", "gzip, deflate, br");
			yc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
			//http://www.codingpedia.org/ama/how-to-handle-403-forbidden-http-status-code-in-java/
			in = yc.getInputStream();
			code = yc.getResponseCode();
			if (code == 301 || code == 302){
            	url = yc.getHeaderField("Location");
			} else
				break;
		}
		byte[] buffer = new byte[4096];
        //http://stackoverflow.com/questions/263013/java-urlconnection-how-can-i-find-out-the-size-of-a-web-file
        String ct = yc.getContentType();
        String cl = String.valueOf(((HttpURLConnection)yc).getContentLength());
        //http://stackoverflow.com/questions/20247795/correct-http-headers-for-images
        if (header) {
        	String hdrStr = "HTTP/1.0 200 Ok\nContent-Type: "+ct+"\nContent-Length: "+cl+"\n\n";
        	os.write(hdrStr.getBytes("UTF-8"));
        }
        //http://www.javapractices.com/topic/TopicAction.do?Id=245
        int n = -1;
        while ( (n = in.read(buffer)) != -1){
            os.write(buffer, 0, n);
        }
        in.close();
        return new String[]{ct,cl};
 	}

	public static String simple(String url,String urlParameters,String method,int timeout) throws Exception {
		URL obj = new URL(url);
		//HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		if (timeout > 0){
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
		}
		//add request header
		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", Body.http_user_agent);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");//TODO:

		// Send post request
		con.setUseCaches(false);
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
 
		InputStream is;
		int status = con.getResponseCode();
		if(status >= 400)
		    is = con.getErrorStream();
		else
		    is = con.getInputStream();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String inputLine;
		StringBuilder response = new StringBuilder();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString(); 
	}
	
	
	public static boolean accessible(String url){
		try {
			URL u = new URL(url);
	        java.net.URLConnection yc = u.openConnection();
	        yc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
	        InputStream in = yc.getInputStream();
	        in.close();
		} catch (Exception e) {
			return false;
		}
        return true;
 	}
	
	public String sendPost(String url,String urlParameters,int timeout) throws Exception {
		return send(url,urlParameters,"POST",timeout);
	}
	public String sendPost(String url,String urlParameters) throws Exception {
		return send(url,urlParameters,"POST");
	}

	//http://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/	
	public String send(String url,String urlParameters,String method) throws Exception {
		return send(url,urlParameters,method,0);
	}
	public String send(String url,String urlParameters,String method,int timeout) throws Exception {
		URL obj = new URL(url);
		//HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		if (timeout > 0){
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
		}
		//add request header
		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", Body.http_user_agent);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");//TODO:
		if (cookies != null) {
			for (Iterator it = cookies.iterator(); it.hasNext();) {
				String cookie = (String)it.next();
			    con.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
			}
		}

		// Send post request
		con.setUseCaches(false);
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
 
		InputStream is;
		int status = con.getResponseCode();
		if(status >= 400)
		    is = con.getErrorStream();
		else
		    is = con.getInputStream();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String inputLine;
		StringBuilder response = new StringBuilder();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		cookies = (List) con.getHeaderFields().get("Set-Cookie");
		//System.out.println("cookies="+cookies.toString());
		/*
		Map headerFields = con.getHeaderFields();
		Set headerFieldsSet = headerFields.keySet();
		Iterator hearerFieldsIter = headerFieldsSet.iterator();
		while (hearerFieldsIter.hasNext()) {
			String headerFieldKey = (String) hearerFieldsIter.next();
			if ("Set-Cookie".equalsIgnoreCase(headerFieldKey)) {
				List headerFieldValue = (List) headerFields.get(headerFieldKey);
			}
			System.out.println(headerFieldKey+" => "+(List) headerFields.get(headerFieldKey));
		}
		*/
		
		//print result
		//System.out.println(response.toString());
		return response.toString(); 
	}
 	
    public static String parseBetween(String source, String pre, String post) {
    	return parseBetween(source, pre, post, true);
    }
    
    public static String parseBetween(String source, String pre, String post, boolean aMustPost) {
    	if (source != null) {
    		int beg = source.indexOf(pre);
    		if (beg != -1){
    			beg += pre.length();
    			if (post == null)
    				return source.substring(beg);
    			int end = source.indexOf(post, beg);
    			if (end != -1) {
    				return source.substring(beg,end);
    			}
    			if (!aMustPost)
    				return source.substring(beg);
    		}
    	}
    	return null;
    }
    
    //http://projects.fivethirtyeight.com/facebook-primary/
    public static String getJsonString(JsonObject data, String name, String def) {
		return data.keySet().contains(name) ? data.getString(name) : def;
	}
	
	public static String getJsonString(JsonObject data, String name) {
		return getJsonString(data, name, null);
	}

    public static String JsonNumberString(JsonObject data, String name) {
		return data.keySet().contains(name) ? data.getJsonNumber(name).toString() : "";
	}
	
    public static int JsonInt(JsonObject data, String name) {
		return data.keySet().contains(name) ? data.getJsonNumber(name).intValue() : 0;
	}
	
	public static void main(String[] args){
		if (!AL.empty(args) && !AL.empty(args[0])){
			System.out.println(args[0]);
			String out;
			try {
				out = simpleGet(args[0]);
				System.out.println(out);
			} catch (IOException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
