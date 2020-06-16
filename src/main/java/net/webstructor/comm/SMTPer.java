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

import net.webstructor.agent.Body;
import net.webstructor.core.Thing;
import net.webstructor.core.Updater;
import net.webstructor.peer.Session;

//TODO imlement cofiguration and SMTP protocol for SMS/text messages handling
public class SMTPer extends Communicator implements Updater {

	public SMTPer(Body body) {
		super(body);
		//TODO
	}

	@Override
	public boolean update(Thing peer, String sessionKey, String subject, String content, String signature) throws IOException {
		//TODO
		return false;
	}

	public void output(Session session, String message) throws IOException {
		//TODO
	}

	@Override
	public boolean notifyable(Thing peer) {
		//TODO
		return false;
	}
}