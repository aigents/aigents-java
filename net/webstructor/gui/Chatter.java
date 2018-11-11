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
package net.webstructor.gui;

import java.io.IOException;
import java.util.LinkedList;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.comm.Communicator;
import net.webstructor.peer.Session;

class Checker extends Thread {
	public void run() {
		for (;;) {
			try {
				Thread.sleep(60000);//give them a time after logged in
				//TODO: uncomment
				//Table.news.selected();
			} catch (InterruptedException e) {}
		}
	}
}

public class Chatter extends Communicator {
	Session session = null;
	boolean logged = false;
	Checker checker = new Checker();
	
	public Chatter(Body body)
	{
		super(body);
		input("");//seed conversation
	}

	LinkedList requestors = new LinkedList();
	
	synchronized void request(DataModel model, String message) {
		App.getApp().outputMyText(message);
		if (model != null)
			requestors.add(model);
		input(message);
	}
	
	public synchronized void output(Session session, String message) throws IOException {
		//TODO: use session
		App.getApp().outputText(message);
		if (message.startsWith(Body.APPNAME) && message.indexOf(Body.COPYRIGHT) != -1)
			message = message.substring(message.indexOf('\n')+1);
		//TODO: dispatch output across multiple requestors by type?
		DataModel requestor = (DataModel)requestors.poll();
		if (message.startsWith("What your ")) { // may be logged or not logged in, no matter
			App.getApp().bar.info.setText(message);
			form(message.substring("What your ".length()));
		} else
		if (!logged) { 
			if (message.startsWith("Ok.") || message.startsWith("Hello ")) {
				logged = true;
				App.getApp().showWorkTabs();
				//TODO: start checker for release or make it configurable for user!!!
				checker.start();
			}
			//TODO: else?
		}
		else { //logged
			if (requestor != null) {
				String pattern = "Your "+requestor.type()+" ";
				if (message.startsWith(pattern)) {
					requestor.update(message.substring(pattern.length()));
				}
				else
				if (message.startsWith("Your not")) {
					requestor.clear();//TODO: do nothing?
				}
				else
				if (message.startsWith("There ")) {
					requestor.update(message.substring("There ".length()));
				}
				else
				if (message.startsWith("Ok.")) //if taken, repeat refresh
					requestor.confirm();
				else {
					String error = AL.error(message);
					if (error != null) {
						App.getApp().bar.info.setText(error);
					} else
						requestor.update(message);
				}
			}
		}
	}
	
	//builds the form request fields from list of fields to ask
	public void form(String message) {
		String[] fields = AL.parseToFields(message);
		if (!AL.empty(fields))
    		App.getApp().showPropTabs("My",fields,false);
	}

	public void input(String message) {
		try {
			if (session == null)
				session = body.sessioner.getSession(this,"1");//"0" is reserved by Cmdliner
        	body.conversationer.handle(this, session, message);
        } catch (IOException e) {
   			if (alive())
   				App.getApp().outputText("App error: "+e.toString());
        }
	}

}
