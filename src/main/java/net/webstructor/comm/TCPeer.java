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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import net.webstructor.agent.Body;
import net.webstructor.peer.Session;

public class TCPeer extends Communicator
{
	protected Socket socket;
    protected int input_max = 256;

    protected OutputStream out;
    protected InputStream in;
    
	public TCPeer(Body body,Socket socket) throws IOException {
		super(body);
	    //TODO:body.getInt("input max")
		this.socket = socket;
	    out = new BufferedOutputStream(socket.getOutputStream());
	    in   = new BufferedInputStream(socket.getInputStream());
	}

	private String input() throws IOException {
	      StringBuilder request = new StringBuilder(input_max);
	      int count = 0;
	      while (true) {
	        int c = in.read(  );
	        if (c == -1) 
	        	return null;
	        if (c == '\r' || c == '\n') { 
	        	if (count > 0) //return completed lines only 
	        		break;
	        }
	        else {
		        request.append((char) c);
		        //TODO: If this is HTTP 1.0 or later send a MIME header
		        if (++count > input_max) {  	
		        	//bDone = true;//TODO:indicate error and ignore?
		        	break;
		        }
	        }
	      }
	      return request.toString();
	}

	public void output(Session session, String message) throws IOException {
		//TODO: use session
	    byte[] bytes;
		try {
			bytes = message.getBytes("UTF-8");
		    out.write(bytes);
		    out.write('\n');
		    out.flush();
		} catch (SocketException e) {
			body.reply("TCP/IP closed");
			terminate();
		}
	}
	
	public void run() {
		try {
			String requestString;
			while (alive() && (requestString = input()) != null) {
			    Session session = body.sessioner.getSession(this,socket.toString());
			    body.conversationer.handle(this, session, requestString);
			}//while
		} catch (SocketTimeoutException e) {
			body.reply("TCP/IP timeout");
		} catch (IOException e) {		
			//if (alive())
				body.error("TCP/IP error",e);
		} finally {
			cleanup();
		}
	}//run

	protected synchronized void cleanup() {
		try {
			if (socket != null) {
				in.close();
				out.close();
				socket.close();
				socket = null;
			}
		} catch (Exception e) {
			//if (alive())
				body.error("TCP/IP close error.",e);
		}
	}
	
	public void terminate() {		
		super.terminate();//not alive
		interrupt();//thread
		cleanup();//this
	}	
	
}//class
