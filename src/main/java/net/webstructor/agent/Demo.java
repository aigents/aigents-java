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
import net.webstructor.peer.Conversationer;
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
		return new Siter(this,path);
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
     * May create custom social/network/media infrastructure adapters/plugins
     */
    @Override
	protected void socialize() {
    	//one can setup their own Crawler-s and Socializers, using the default ones or not
		super.socialize();
	}
	
    public static void main(String[] args) {
        new Demo(args).start();
    }
    
}
