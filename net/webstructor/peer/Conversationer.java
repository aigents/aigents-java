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
package net.webstructor.peer;

import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.StringWriter;
import java.util.LinkedList;

import net.webstructor.agent.Body;
import net.webstructor.comm.Communicator;

class Message {
	Session session;
	String text;
	public Message(Session session,String text) {
		this.session = session;
		this.text = text;
	}
}

class Worker extends Thread {	
	private Conversationer conversationer;
	public Worker(Conversationer conversationer) {
		this.conversationer = conversationer;
	}
	public void run() {
		try {
			for (;;) {
				Message message;
				synchronized (conversationer.queue) {//get message, if any
					message = (Message) conversationer.queue.poll();
				}
				if (message != null)//proces message
					process(message);
				synchronized (conversationer.queue) {//wait till message is available
					while (conversationer.queue.isEmpty())
						conversationer.queue.wait();
				}
			}
		} catch (Exception e) {
			//StringWriter errors = new StringWriter();
			//e.printStackTrace(new PrintWriter(errors));
			//String s = e.toString() + ":" + errors.toString();
			conversationer.body.error("Conversation thread pooling error.",e);				
		}
	}//run
	
	
	//TODO: need to ensure no more than one thread is handling the same session is the conversationer pool 
	//TODO: possibly call process for UniqueQueue of Sessions and not Messages
	protected void process(Message message) {
		//TODO: some tricky real stuff
		
		//TODO: AL translation
		message.session.comprehend(message.text);		  
//conversationer.body.debug("Input:"+message.session.input);//TODO:cleanup	
		//TODO: iterative modes processing the session context
		while (message.session.mode.process(message.session)) {//mode act on session
			;
		}		
//conversationer.body.debug("Output:"+message.session.output);//TODO:cleanup	
		//TODO: AL generation
		String outgoingMessage = message.session.express();
		
		try {
			message.session.communicator.output(message.session,outgoingMessage);

			//TODO: post [ farewell, terminate ] 
			if (message.text.toLowerCase().indexOf("bye")==0)
	        	message.session.communicator.terminate();	
	        
		} catch (IOException e) {
			conversationer.body.error("Conversation error",e);				
		}
	}
}

/**
 * AKA Talker
 * @author akolonin
 */
public class Conversationer {
	//TODO: configuration
	final public static int WORKERS = 3;
	
	protected LinkedList queue = new LinkedList();//Message
	protected Body body;
	
	public Conversationer(Body body,int workers) {
		this.body = body;
		for (int i=0; i<workers; i++) {
			Thread wt = new Worker(this);
			wt.start();
		}		
	}
	
	//TODO: make this working asynchronously, so the caller is not blocked 
	public void handle(Communicator communicator,Session session,String text) throws IOException {
		if (text == null)
			throw new IOException("No message from "+communicator.getClass().getName()+".");
		synchronized (queue) {		
			queue.add(new Message(session,text)); 
			queue.notify();
		}
		
	}
}
