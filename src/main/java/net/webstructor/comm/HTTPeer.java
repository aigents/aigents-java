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
package net.webstructor.comm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.cat.StringUtil;
import net.webstructor.peer.Session;
import net.webstructor.util.Str;

public class HTTPeer extends Communicator
{
	protected Socket socket;
    protected int input_max = 512;
	protected final String http_format = "text";//TODO:html/text/json/custom
    
	private HTTPListener parent;
	private String cookieString;
	private String requestString;
	private boolean waiting = false;//TODO:re-use bAlive?
	
	BufferedOutputStream out;
	BufferedInputStream in;
	int contentLength = 0;//for POST requests
	
	public HTTPeer(HTTPListener parent,Socket socket) {
		super(parent.body);
		this.socket = socket;
		this.parent = parent; 
	}
	
	public HTTPListener parent(){
		return parent;
	}
	
	byte[] buildHeader(int contentLength,String content_type) throws IOException {
		StringBuilder header = new StringBuilder(256);
		header.append("HTTP/1.0 200 OK\r\n")
			.append("Server: Aigents "+Body.VERSION+"\r\n")
			.append("Content-length: ").append(contentLength).append("\r\n")
			.append("Content-type: ").append(content_type).append("; charset=utf-8\r\n")
			.append("Access-Control-Allow-Origin: ").append(parent.http_origin).append("\r\n")
			.append("Access-Control-Allow-Methods: GET\r\n")//, POST, PUT, DELETE, OPTIONS
			.append("Access-Control-Allow-Credentials: true\r\n")
			//.append("Access-Control-Allow-Headers: *\r\n")
			.append("Access-Control-Allow-Headers: Authorization\r\n")
			.append("Set-Cookie: ").append(parent.cookie_name).append('=').append(cookieString)
				.append("; Domain=").append(parent.cookie_domain)
				.append("\r\n")
			.append("\r\n");
		return header.toString().getBytes("ASCII");
	}

	byte[] buildError() throws IOException {
		StringBuilder header = new StringBuilder(256);
		header.append("HTTP/1.0 404 Not Found\r\n\r\n");
		return header.toString().getBytes("ASCII");
	}
	
	//http://stackoverflow.com/questions/10687358/java-socket-inputstream-read-returns-1-always-just-before-end
	private String inputHeader(StringBuilder request) throws Exception {
	      boolean end = false;
	      for (;;) {
	        int c = in.read();
	        if (c == -1)
	        	break;
	        if (c == '\r') {
	        	if (in.read() != '\n')
	        		throw new Exception("Wrong HTTP request "+StringUtil.first(request.toString(),100));
	        	request.append("\r\n");
	        	if (end)
	        		break;//end of header - double \r\n emplty line
	        	end = true;
	        }
	        else {
	        	end = false;
	        	request.append((char) c);
	        }
	      }
		  //logger.log(StringUtil.first(requestString,MAX_INPUT),"request");//TODO rework		    
	      return request.toString();
	}

	//TODO: limit on input_max!?
	private String inputData() throws Exception {
	      StringBuilder request = new StringBuilder(input_max);
	      for (int i = 0; i < contentLength; i++) {
	        int c = in.read();
	        if (c == -1)
	        	break;
	        request.append((char) c);
	      }
	      return URLDecoder.decode(request.toString(),"UTF-8");
	}
	
	//https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
	protected String parseHeader(String request) { // throws Exception 
		String s = Str.parseBetween(request, "Content-Length: ", "\n");
		if (!AL.empty(s))
			contentLength = Integer.valueOf(s.trim()).intValue();
		
		cookieString = null;
		String c = Str.parseBetween(request, "Cookie: ", "\n");
		if (c != null)
			c = c.trim();
		if (!AL.empty(c)){
			StringTokenizer cookies = new StringTokenizer(c.trim(),";");
			while (cookies.hasMoreTokens()) {
				String tok = cookies.nextToken().trim();
				StringTokenizer cookie = new StringTokenizer(tok,"=");
				String name = cookie.hasMoreTokens()? cookie.nextToken() : "";
				String value = cookie.hasMoreTokens()? cookie.nextToken() : "";
				//TODO:
				if (name.equalsIgnoreCase(parent.cookie_name)) {
					cookieString = value;
				}
			}
		}
		
		int beg = request.indexOf('?');
		int eol = request.indexOf('\n');
		int end = request.indexOf(' ',beg+1);
		if (beg==-1 || end==-1 || eol<=beg)//if no query in first header line
			return null;
		requestString = request.substring(beg+1,end).trim();
		/*
		String cookieString1 = null;
		beg = request.indexOf("Cookie: ",end+1);
		if (beg != -1) {
			end = request.indexOf('\n',beg+1);
			if (end != -1) {
				StringTokenizer cookies = new StringTokenizer(request.substring(beg+8,end).trim(),";");
				while (cookies.hasMoreTokens()) {
					String tok = cookies.nextToken().trim();
					StringTokenizer cookie = new StringTokenizer(tok,"=");
					String name = cookie.hasMoreTokens()? cookie.nextToken() : "";
					String value = cookie.hasMoreTokens()? cookie.nextToken() : "";
					//TODO:
					if (name.equalsIgnoreCase(parent.cookie_name)) {
						cookieString1 = value;
					}
				}
			}
		}

		//TODO: remove debug
		if ((cookieString1 == null && cookieString != null) 
			|| (cookieString1 != null && cookieString == null) 
			|| (cookieString !=null && cookieString1 != null && !cookieString1.equals(cookieString)))
			System.out.println("yo!");
		*/
		try {
			//if (!AL.isURL(requestString))//don't decode URLs!
			if (!(requestString.startsWith("u=") && AL.isURL(requestString.substring(2))))//don't decode URLs!
				requestString = URLDecoder.decode(requestString,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			//leave requestString
		}	
		int jquerySuffix = requestString.indexOf("&_=");
		if (jquerySuffix != -1)
		 	requestString = requestString.substring(0,jquerySuffix);
        return requestString;
	}

	//TODO: POST/PUT, besides GET
	private void input(String[] requestdata) throws Exception {
	    StringBuilder header_sb = new StringBuilder(input_max);
		String header = inputHeader(header_sb).toString();
		String url = Str.parseBetween(header, " ", " ");
		if (!AL.empty(url))
			requestdata[0] = url.trim();
		if (header == null || header.length() < 1) {
			return;
			//throw new IOException("No HTTP header.");
		}
		String request = parseHeader(header);
		if (AL.empty(request) && header.startsWith("POST"))
			request = inputData();
		requestdata[1] = header;
		requestdata[2] = request;
	}
	
	public void output(Session session, String resultString) throws IOException {
		try {
		//TODO:use session
		if (resultString != null) {
	        if (http_format.equals("html")) {
		        StringBuilder result = new StringBuilder();
		        result.append("<HTML><BODY>");
		        result.append(resultString);
		        result.append("</BODY></HTML>");
		        resultString = result.toString();
	        }
			//else if (http_format.equals("custom???")) {//custom AJAX
	        //result.append("<HTML><SCRIPT>function doLoad() {if (top) {top.postMessage(document.body.innerHTML,'*');}}");
	        //result.append("</SCRIPT><BODY onload=\"doLoad()\">");
	        //}		
	        //https://stackoverflow.com/questions/595616/what-is-the-correct-mime-type-to-use-for-an-rss-feed
		    String content_type = resultString.startsWith("<?xml") ? "text/xml" : "text/html";
		    //String content_type = resultString.startsWith("<?xml") ? "application/rss+xml" : "text/html";
		    byte[] bytes = resultString.getBytes("UTF-8");
		    out.write(this.buildHeader(bytes.length,content_type));
		    out.write(bytes);
			body.reply(cookieString+":"+resultString);
	    } else {
	    	out.write(this.buildError());
	    }

	    out.flush();
	    //TODO:keep-alive
	    socket.close();
	    
	    synchronized (this) {
	    	waiting = false;
	    	notify();//to let the process running
	    }
	    //logger.log(StringUtil.first(result,MAX_INPUT),"response");//TODO rework	
		} catch (SocketException e){
			body.error("SocketException in HTTPeer.output session " + session.getKey()
					+ (session.getAreaPeer() == null ? "" : " name " +session.getAreaPeer().name())
					+ " text "+resultString+" error "+e.getMessage(),null);
		}
	}
	
	public void respond(String response) throws IOException {
		respond(response,"200 Ok","text/plain");
	}
	
	public void respond(String response, String code, String type) throws IOException {
    	OutputStream os = socket.getOutputStream();
    	BufferedOutputStream bos = new BufferedOutputStream(os);
    	byte[] bytes = response.getBytes("UTF-8");
    	String hdrStr = "HTTP/1.0 "+code+"\nContent-Type: "+type+"\nContent-Length: "+bytes.length+"\n\n";
    	bos.write(hdrStr.getBytes("UTF-8"));
    	bos.write(bytes);
	    bos.flush();
	    os.flush();
	}
	
	private void process() {
		String request = null;
		String url_header_request[] = {null,null,null,null};
		try {
			in = new BufferedInputStream(socket.getInputStream());
			out = new BufferedOutputStream(socket.getOutputStream());
			input(url_header_request); // read HTTP request
			
			request = url_header_request[2];
			//body.reply("H1:"+url_header_request[0]+":"+url_header_request[1]+":"+request);
			body.reply(cookieString+":"+request);
			if (cookieString == null) { // generate cookie for new sessions
				cookieString = parent.getCookie();
			}
					
		    //TODO: if found filter/handler for url_header_request, pass that to the one
		    if (parent.handleHTTP(this,url_header_request[0],url_header_request[1],url_header_request[2],cookieString))
		    	return;

			if (request == null) {//if no data for Aigents core handling
				output(null,null);//return 404,TODO:better idea?
				return;
			}
			
		    //TODO: hack properly using filters and handleHTTP
		    //TODO: fix header
		    if (request.startsWith("u=") && AL.isURL(request.substring(2))){
		    	request = request.substring(2);
		    	int r = request.indexOf('\r');
		    	if (r != -1)//get rid of POST tail //TODO: peoperly
		    		request = request.substring(0,r);
		    	//request = request.trim().replaceAll(" ","%20");//TODO: properly
		    	OutputStream os = socket.getOutputStream();
		    	BufferedOutputStream bos = new BufferedOutputStream(os);
				HTTP.simpleGetBin(request, bos, true);
			    bos.flush();
			    os.flush();
			    //TODO:keep-alive???
			    socket.close();
		    	return;
		    }
		    
		    waiting = true;
		    Session session = body.sessioner.getSession(this,cookieString);
		    body.conversationer.handle(this, session, request);

		    synchronized (this) {
		    	while (waiting)
		    		wait(parent.http_timeout);//to complete processing//TODO:what if timeout?
		    }

		} catch (Exception e) {		
			body.error("HTTP error (" + (AL.empty(request)? e.toString() : request) + ").",e);
		}
	}
	
	public void run() {
		if (socket != null) { // given the socket, act once and die (deprecate)
			process();
		} else { // if no socket given in constructor, get it in the queue
			try {
				for (;;) {
					synchronized (parent.queue) {
						for (;;) {
							socket = (Socket) parent.queue.poll();
							if (socket != null)
								break;
							parent.queue.wait();
						}
					}
					process();
					//body.reply("Thread "+getId()+" done.");				
				}
			} catch (InterruptedException e) {
				body.error("Thread pooling error (" + e.toString() + ").",e);				
			}
		}
	}//run

	public static void main(String args[]){
		//String url = "http://tayga.info/media/images/news/132/132375/thumb.jpg";//OK:it works
		//String url = "http://tayga.info/design/logo.svg";//FAILED:broken pipe
		//String url = "http://i2.cdn.cnn.com/cnnnext/dam/assets/170216233759-april-ryan-trump-press-conference-large-tease.jpg";
		//String url = "https://i.guim.co.uk/img/static/sys-images/Guardian/Pix/pictures/2013/11/26/1385479353669/98ae8297-8cf7-4138-8a44-09f4573625ec-2060x1236.jpeg";
		String url = "https://www.openmined.org/static/media/open-collective.8a4a52cb.svg";
		BufferedOutputStream os;
		try {
			//os = new BufferedOutputStream(new FileOutputStream("test_image.jpg"));
			os = new BufferedOutputStream(new FileOutputStream("test_image.svg"));
			HTTP.simpleGetBin( url, os, false);
			os.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}//class
