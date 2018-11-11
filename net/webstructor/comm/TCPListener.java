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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import net.webstructor.agent.Body; 
import net.webstructor.agent.Farm;
import net.webstructor.al.AL;
import net.webstructor.peer.Session;

public class TCPListener extends Communicator {
	protected int port;
	private int tcp_timeout = 60000;

	public TCPListener(Body body) {
		super(body);
		//net.webstructor.core.Thing self = body.self();
		
		port = AL.integer(body.self().getString(Farm.tcp_port,Integer.toString(port)),port);
		tcp_timeout = AL.integer(body.self().getString(Farm.tcp_timeout,Integer.toString(tcp_timeout)),tcp_timeout);
	}

	public void output(Session session, String message) throws IOException {
		throw new IOException("TCPListener can not do output."); 
	}
	
	public void run(  ) {
		try {
			//Logger logger = Logger.getLogger();
			ServerSocket server = new ServerSocket(port);
			body.reply("Started TCP at " + server.getLocalPort() + ".");
			//logger.log("Started at " + server.getLocalPort(),"startup");//TODO rework
			while (alive()) {
				try {
					Socket socket = server.accept();
					socket.setSoTimeout(tcp_timeout);//prevent silent read access from port scanners	      
					//logger.log(connection.getRemoteSocketAddress().toString(),"connection");//TODO rework		      
					TCPeer tcpeer = new TCPeer(body,socket);
					tcpeer.start();	
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
			body.output("Could not start TCP/IP server (" + e.toString() + ").");
		}
	} // end run
}
