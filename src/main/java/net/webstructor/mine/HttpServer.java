/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine;

//http://www.oreilly.com/catalog/javanp2/chapter/ch11.html
//http://www.oreilly.com/catalog/javanp2/chapter/ch11.html#53648


import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import net.webstructor.mine.auth.AdmServer;

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

private int port = 80;
private boolean bDone = false;
private Handler handler;

public HttpServer(int port,Handler handler) throws UnsupportedEncodingException {
  
	this.port = port;
	this.handler = handler;
}

byte[] getHeader(int contentLength) throws Exception
{
	  String header = "HTTP/1.0 200 OK\r\n"
		   + "Server: Webstructor Mine 1.0\r\n"
		   + "Content-length: " + contentLength + "\r\n"
		   + "Content-type: " + "text/html" + "\r\n\r\n";
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

protected String wrapHtml(String text){
	StringBuffer sb = new StringBuffer();
	sb.append("<HTML><SCRIPT>function doLoad() {if (top && top.accept) {top.accept(document.body.innerHTML);}}"); 
	sb.append("</SCRIPT><BODY onload=\"doLoad()\">");
	sb.append(text);
	sb.append("</BODY></HTML>"); 
	return sb.toString();
}


protected String handle(String request) throws Exception 
{
	int q = request.indexOf('?');
	int e = request.indexOf(' ',q+1);
	if (q==-1 || e==-1)
		return "HTTP request is wrong: "+request;
	String cmd = request.substring(q+1,e).trim();
	String result;
	if (cmd.equalsIgnoreCase("stop"))
	{
		bDone = true;
		return "Shutdown requested";
	}
	else
	synchronized (handler)
	{
		//http://localhost:88/?textcat%20299%20cannula
		cmd = decode(cmd);
		result = handler.handle( cmd );
	}
	return wrapHtml(result);
}

public void run(  ) 
{
  ServerSocket server = null;
  try {
    server = new ServerSocket(this.port);
    System.out.println("Accepting connections on port " 
      + server.getLocalPort(  ));
    while (!bDone) {
      
      Socket connection = null;
      try {
        connection = server.accept(  );
        OutputStream out = new BufferedOutputStream(
                                connection.getOutputStream(  )
                               );
        InputStream in   = new BufferedInputStream(
                                connection.getInputStream(  )
                               );
        // read the first line only; that's all we need
        StringBuffer request = new StringBuffer(80);
        while (true) {
          int c = in.read(  );
          if (c == '\r' || c == '\n' || c == -1) break;
          request.append((char) c);
          // If this is HTTP 1.0 or later send a MIME header

        }
        
        String result = handle(request.toString());
        
        if (request.toString().indexOf("HTTP/") != -1) {
          out.write(this.getHeader(result.length()));
        }       
        out.write(result.getBytes());
        out.flush(  );
      }  // end try
      catch (Exception e) {

//TODO logging    	  
    	  
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


public static void main(String[] args) {

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
    
    System.out.println("Starting at port "+port+", loading auth DB from "+args[0]);
    
    Thread t = new HttpServer(port,new Handler(AdmServer.getInstance(args[0])));
    t.start(  );         
  }
  catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("Usage: java net.webstructor.mine.HttpServer authdbpath port");
  }
  catch (Exception e) {
    System.err.println(e);
  }

}

}
