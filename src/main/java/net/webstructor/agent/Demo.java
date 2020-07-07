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
package net.webstructor.agent;

import net.webstructor.agent.Farm;
import net.webstructor.peer.Conversation;
import net.webstructor.peer.Conversationer;
import net.webstructor.peer.Intenter;
import net.webstructor.peer.Responser;
import net.webstructor.peer.Session;
import net.webstructor.self.Matcher;
import net.webstructor.self.Publisher;
import net.webstructor.self.Siter;

public class Demo extends Farm {

    public Demo(String[] args) {
    	super(args,
    			true,//enable logger
    			false,//disable console!!!
    			true,true,true,//enable email, http, telnet
    			true,//enable social networking!
    			Conversationer.WORKERS);//set greater number for multiple user sessions
    }
	
    /**
     * May return custom Siter
     */
    @Override
	public Siter getSiter(String path){
    	//one can use the Siter subclass of their own, overridding the methods
		return super.getSiter(path);
	}
	
    /**
     * May return custom Matcher
     */
    @Override
	public Matcher getMatcher(){
    	//one can use the Matcher subclass of their own, overridding the methods
		return super.getMatcher();
	}
	
    /**
     * May return custom Publisher
     */
    @Override
	public Publisher getPublisher(){
    	//one can use the Matcher subclass of their own, overridding the methods
		return super.getPublisher();
	}
	
    /**
     * May return custom Intenter
     */
    @Override
	public Responser getResponser(){
    	//one can use the basic Intenter subclass of their own, overridding the methods
		return new Conversation() {
			@Override
			public Intenter[] getIntenters() {
		    	//one can reload subordinate Intenters
				Intenter[] base = super.getIntenters();
				Intenter[] extended = new Intenter[base.length + 1];
				System.arraycopy(base, 0, extended, 1, base.length);
				extended[0] = new Intenter() {
					@Override
					public String name() {
						return "ping chat demo";
					}
					@Override
					public boolean handleIntent(Session session) {
						if ("ping".equals(session.input())) {
							session.output("pong");
							return true;
						}
						return false;
					}
				};
				return extended;
			}
		};
	}
	
    /**
     * May create custom social/network/media infrastructure adapters/plugins
     */
    @Override
	protected void socialize() {
    	//one can setup their own Crawler-s/Socializer-s, using the default ones or not
		super.socialize();
	}
	
    /**
     * May create Communicator-s, Serper-s and Intenter-s
     */
    @Override
    public void start() {
    	//one can setup their own ommunicator-s, Serper-s and Intenter-s, using the default ones or not
		super.start();
	}
	
    public static void main(String[] args) {
        new Demo(args).start();
    }
    
}
