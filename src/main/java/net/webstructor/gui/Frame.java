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

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import net.webstructor.agent.Body;

public class Frame extends JFrame {

	private static final long serialVersionUID = -8065007309235440161L;

	private Tray tray;
	
	public Frame() {
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		initFrame();
	}

	//TODO: OSX support: https://developer.apple.com/library/mac/documentation/java/conceptual/java14development/07-nativeplatformintegration/nativeplatformintegration.html
	private void initFrame() {
		//this.setSize(new Dimension(400, 300));
		this.setSize(new Dimension(300, 250));
		this.setTitle(Body.APPNAME+" "+Body.VERSION);
		//aigent.gif - green eyes
		//aigent.png - gray eyes
 		ImageIcon image = Tabs.createImageIcon("/aigent128.png");
		this.setIcon(image.getImage());	
		tray = new Tray();//Windows
	}

	private void setIcon(Image im) {
		this.setIconImage(im);

		//TODO: for Windows system tray
		//http://docs.oracle.com/javase/8/docs/api/java/awt/SystemTray.html
		
		//for Apple OSX docking bar and menu
		//http://grepcode.com/file/repo1.maven.org/maven2/com.yuvimasory/orange-extensions/1.3.0/com/apple/eawt/Application.java
	    //com.apple.eawt.Application.getApplication().setDockIconImage(im);
	    try {
	    	Class c = Class.forName("com.apple.eawt.Application" );
        	Method method = c.getMethod("getApplication", new Class[0]);
        	Object o = method.invoke(null,new Object[0]);
        	Method m = o.getClass().getMethod("setDockIconImage",new Class[]{Image.class});
	    	m.invoke(o, new Object[]{im});
        	//m = o.getClass().getMethod("setDockIconBadge",new Class[]{String.class});
	    	//m.invoke(o, new Object[]{text});
	    } catch (Exception e) {
	    	//TODO:what?
	    }
	}

	public void setBadge(String text) {
		//for Windows system tray
		tray.setStatus(text);
		//for Apple OSX dock bar badge
	    try {
	    	Class c = Class.forName("com.apple.eawt.Application" );
        	Method method = c.getMethod("getApplication", new Class[0]);
        	Object o = method.invoke(null,new Object[0]);
        	Method m = o.getClass().getMethod("setDockIconBadge",new Class[]{String.class});
	    	m.invoke(o, new Object[]{text});
	    } catch (Exception e) {
	    	//TODO:what?
	    }
	}
	
	protected void processWindowEvent(WindowEvent e) {
		 super.processWindowEvent(e);
		 if (e.getID() == WindowEvent.WINDOW_CLOSING) {
		 	System.exit(0);
		 }
	}
	
}
