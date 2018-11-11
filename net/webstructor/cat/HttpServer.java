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

import java.net.*;
import java.io.*;

import net.webstructor.agent.Farm;
import net.webstructor.main.Logger;

/**
* This is simple and rough implementatoion of single threaded, single-user HTTP server for IDD application<p>
* This is based on: <p>  
* http://www.oreilly.com/catalog/javanp2/chapter/ch11.html <p>  
* http://www.oreilly.com/catalog/javanp2/chapter/ch11.html#53648 <p>
* The alternate and better solution may be based on: <p>
* http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/package-summary.html <p>
* http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/index.html <p>
* http://developers.sun.com/web/swdp/r1/rest-impl/samples/SimpleAtomServer/README.html <p>  
* @author Anton
* @since 20070515
*/
public class HttpServer extends Thread 
{
    static String m_lineBr = "<br>";
    
	private int port = 80;
	private boolean bDone = false;
	//private CommandHandler handler;
	//public HttpServer(int port,CommandHandler handler) throws UnsupportedEncodingException {
	//	this.port = port;
	//	this.handler = handler;
	//}
	
	public HttpServer(int port) throws UnsupportedEncodingException 
	{
		this.port = port;
	}

	byte[] getHeader(int contentLength) throws Exception
	{
		  String header = "HTTP/1.0 200 OK\r\n"
			   + "Server: WebCat 1.0\r\n"
			   + "Content-length: " + contentLength + "\r\n"
			   + "Content-type: " + "text/html; charset=utf-8" + "\r\n\r\n";
		       //+ "Content-type: " + "text/plain" + "\r\n\r\n";
			  return header.getBytes("ASCII");
	}

	public static String decode(String request) throws Exception
	{
		// this should convert "+" to " " (which may not be good) but it throws exceptions
		// return URLDecoder.decode(request,"");
		/*
		// this converts "%20" to " " (which is good) but lacks converting the rest
		StringBuffer sb = new StringBuffer();
		int start=0;
		for (;;)
		{
			int next = request.indexOf("%20",start);
			if (next==-1)
			{
				sb.append(request.substring(start));
				break;
			}
			else
			{
				if (next>0)
					sb.append(request.substring(start,next));
				sb.append(' ');
				start = next+3;
			}
		}
		return sb.toString();
		*/
		StringBuffer sb = new StringBuffer();
		int start=0;
		for (;;)
		{
			int next = request.indexOf("%",start);
			if (next==-1)
			{
				sb.append(request.substring(start));
				break;
			}
			else
			{
				if (next>0)
					sb.append(request.substring(start,next));
				try {
					String code = request.substring(next+1,next+3); 
					sb.append((char)Integer.parseInt(code,16));
					start = next+3;
				} catch (Exception e) {
					sb.append(request.substring(next,next+1));
					start = next+1;
				}
			}
		}
		return sb.toString();
	}

	protected String handle(String request) throws Exception 
	{
		int q = request.indexOf('?');
		int e = request.indexOf(' ',q+1);
		if (q==-1 || e==-1)
			return "HTTP request is wrong: "+request;
		String cmd = request.substring(q+1,e).trim();
		StringBuffer result = new StringBuffer();
		if (cmd.equalsIgnoreCase("stop"))
		{
			bDone = true;
			return "Shutdown requested";
		}
		else
		//synchronized (handler)
		{
			//http://localhost:88/?textcat%20299%20cannula
			//cmd = decode(cmd);
			//result = handler.handle( cmd );
			
			//cmd = decode(cmd); 
            cmd = URLDecoder.decode(cmd,"UTF-8");
			CommandHandler ch = new CommandHandler(cmd,m_lineBr,";",",");
            ch.run();
            
            //result.append("<HTML><SCRIPT>function doLoad() {alert('top='+top+' accept='+top.accept); if (top && top.accept) {alert('top.accept='+top.accept); top.accept(document.body.innerHTML);}}");
            result.append("<HTML><SCRIPT>function doLoad() {if (top) {top.postMessage(document.body.innerHTML,'*');}}");
            //result.append("<HTML><SCRIPT>function doLoad() {if (top && top.accept) {top.accept(document.body.innerHTML);}}");
            result.append("</SCRIPT><BODY onload=\"doLoad()\">");
            result.append(ch.getOutput());
            result.append("</BODY></HTML>");
			//result = ch.getOutput();
		}
		return result.toString();
	}

	//Should be large enough to consume any input in URL-encoded form
	private static final int MAX_INPUT = 1024;
	
	public void run(  ) 
	{
		ServerSocket server = null;
		try {
		  Logger logger = Logger.getLogger();
		  server = new ServerSocket(this.port);
		  System.out.println("Started at " + server.getLocalPort());
		  logger.log("Started at " + server.getLocalPort(),"startup");//TODO rework
		  while (!bDone) {
		    
		    Socket connection = null;
		    try {
		      connection = server.accept();
		      connection.setSoTimeout(2000);//prevent silent read access from port scanners
		      
		      logger.log(connection.getRemoteSocketAddress().toString(),"connection");//TODO rework
		      
		      OutputStream out = new BufferedOutputStream(
		                              connection.getOutputStream(  )
		                             );
		      InputStream in   = new BufferedInputStream(
		                              connection.getInputStream(  )
		                             );
		      // read the first line only; that's all we need
		      StringBuffer request = new StringBuffer(MAX_INPUT);
		      int count = 0;
		      while (true) {
		        int c = in.read(  );
		        if (c == '\r' || c == '\n' || c == -1) break;
		        request.append((char) c);
		        //TODO: If this is HTTP 1.0 or later send a MIME header
		        if (++count > MAX_INPUT) {  	
				    logger.log(StringUtil.first(request.toString(),MAX_INPUT),"suspect");
		        	//bDone = true;//TODO:indicate error and ignore?
		        	break;
		        }
		      }
		      String requestString = request.toString();
		      
		      logger.log(StringUtil.first(requestString,MAX_INPUT),"request");//TODO rework		    
		      
		      String result = handle(requestString);

		      logger.log(StringUtil.first(result,MAX_INPUT),"response");//TODO rework		    
		      
		      byte[] bytes = result.getBytes("UTF-8");		    		  
		      if (request.toString(  ).indexOf("HTTP/") != -1) {
		        out.write(this.getHeader(bytes.length));
		      }       
		      out.write(bytes);
		      out.flush();
		    }  // end try
		    catch (Exception e) {		
		      logger.log("Failed: " + e.toString(),"failure");//TODO rework
		      //bDone = true;
		    }
		    finally {
		      if (connection != null) 
		    	  connection.close(  ); 
		    }
		    
		  } // end while
		} // end try
		catch (IOException e) {
		  System.err.println("Could not start server. Port Occupied");
		}
		if (server!=null)
			  System.out.println("Releasing port "+ server.getLocalPort(  ));
	} // end run


	public static void main(String[] args) 
	{	
		if (args[0] != null)
			CmdLine.setCurrentDirectory(args[0]);

		// initialize the server explicitly
        CommandHandler.getServer(System.getProperty("user.dir"));
		
		try {
		  // set the port to listen on
		  int port;
		  try {
		    port = Integer.parseInt(args[1]);
		    if (port < 1 || port > 65535) port = 80;
		  }  
		  catch (Exception e) {
		    port = 80;
		  }  
		  
		  System.out.println("Starting at port "+port+", loading asset from "+args[0]);
		  
		  Thread t = new HttpServer(port);
		  t.start(  );
		  
		  //TODO:remove hacky load of Aigents into this JVM
		  if (args.length > 2 && args[2].equalsIgnoreCase("aigents"))
			  new Farm(args,true,false,true,true,true,true,10).start();//all services, but console		     
		}
		catch (ArrayIndexOutOfBoundsException e) {
		  System.out.println(
		   "Usage: java SingleFileHttpServer filename port encoding");
		}
		catch (Exception e) {
		  System.err.println(e);
		}
	
	}

}


