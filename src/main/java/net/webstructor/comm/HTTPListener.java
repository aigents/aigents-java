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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import net.webstructor.agent.Body; 
import net.webstructor.al.AL;
import net.webstructor.comm.fb.Messenger;
import net.webstructor.comm.reddit.Redditer;
import net.webstructor.comm.twitter.Twitterer;
import net.webstructor.core.Thing;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class HTTPListener extends TCPListener implements HTTPHandler {
	protected LinkedList queue= new LinkedList();//Socket	
	protected String cookie_name = "aigent";//No dots!:http://harrybailey.com/2009/04/dots-arent-allowed-in-php-cookie-names/
	protected String cookie_domain = "aigents.org";//http://en.wikipedia.org/wiki/HTTP_cookie
	protected int http_timeout = 60000; //millis to wait for HTTP reply
	protected String http_origin = "null";//needed for AJAX CORS, null for local, * for Safari, specific for Chrome/Firefox
	protected int threads = 2;//2;//0 - synchronous single-threaded, > 0 - asynchronous multi-threaded;
	protected boolean http_secure = false;
	
	private HashMap cacheData = new HashMap();//store cached data
	private HashMap cacheTime = new HashMap();//store cached timestamps
	
	private HTTPHandler[] handlers; //TODO have all child Communicators registered as handlers/filters/plugins
	
	public HTTPListener(Body body) {
		super(body);
		Thing self = body.self();
		port = AL.integer(self.getString(Body.http_port,Integer.toString(port)),port);
		cookie_name = self.getString(Body.cookie_name,cookie_name);
		cookie_domain = self.getString(Body.cookie_domain,cookie_domain);		
		threads = AL.integer(self.getString(Body.http_threads,Integer.toString(threads)),threads);
		http_timeout = AL.integer(self.getString(Body.http_timeout,Integer.toString(http_timeout)),http_timeout);
		http_origin = self.getString(Body.http_origin,http_origin);
		http_secure = "true".equalsIgnoreCase(self.getString(Body.http_secure,"false"));
		
		//TODO: construct it at body constructor level!?
		handlers = new HTTPHandler[]{new Slacker(body),new Messenger(body),new net.webstructor.comm.paypal.PayPaler(body),new Redditer(body),new Twitterer(body)};
	}
	
	protected synchronized String getCookie() {
		return HTTP.nonce();
	}

	//http://stilius.net/java/java_ssl.php
	//https://forum.startcom.org/viewtopic.php?t=1390
	ServerSocket serverSocket() throws IOException {
		if (http_secure) {
			SSLServerSocketFactory sslserversocketfactory =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			return (SSLServerSocket) sslserversocketfactory.createServerSocket(port);
		}
		else
			return new ServerSocket(port);
	}
	
	public void run(  ) {
		try {
			//create pool of threads
			for (int i=0; i<threads; i++) {
				Thread wt = new HTTPeer(this,null);
				wt.start();
			}
			
			//Logger logger = Logger.getLogger();
			ServerSocket server = serverSocket();//new ServerSocket(port);
			
			body.reply("Started HTTP"+ (http_secure ? "S" : "") +" at " + server.getLocalPort() + ".");
			
			//logger.log("Started at " + server.getLocalPort(),"startup");//TODO rework
			while (alive()) {
				try {
					Socket socket = server.accept();
					socket.setSoTimeout(http_timeout);//prevent silent read access from port scanners	      
					//logger.log(connection.getRemoteSocketAddress().toString(),"connection");//TODO rework
					if (threads > 0) {
						synchronized (queue) { // add to thread pool
							queue.add(socket);
							queue.notify();
						}	
					} else {
						Thread t = new HTTPeer(this,socket);
						t.start();
					}
				}  // end try
				catch (Exception e) {		
					//logger.log("Failed: " + e.toString(),"failure");//TODO rework
				}
			} // end while
			if (server != null) {
				//server.close();
				body.output("Releasing port "+ port +".");
			}
		} // end try
		catch (IOException e) {
			body.output("Could not start HTTP server (" + e.toString() + ").");
		}
	} // end run

	//@Override
	public boolean handleHTTP(HTTPeer parent, String url, String header, String request, String cookie) throws IOException {
		if (url != null && handlers != null)
			for (int i = 0; i < handlers.length; i++)
				if (handlers[i].handleHTTP(parent, url, header, request, cookie))
					return true;
		return false;
	}

	//TODO: move to CacheHolder
	public String store(String value){
		synchronized (cacheData){
			String key = getCookie();//assign random key
			cacheData.put(key, value);
			cacheTime.put(key, new Date());
			return key;
		}
	}
	public String retrieve(String key){
		synchronized (cacheData){
			String value = (String)cacheData.get(key);
			if (value != null){
				Date now = new Date();
				Date old = (Date)cacheTime.get(key);
				//TODO: make expiration configurable, now need repeateble cache reads so that Facebook can validate all links sent in chat
				if (old == null)//no expiration is set
					cacheData.remove(key);
				else if ((now.getTime() - old.getTime()) > 60000){//expire link in 1 minute
					cacheData.remove(key);
					value = null;
				}
			}
			return value;
		}
	}
	
}//end class
