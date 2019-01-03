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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.net.URI;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.webstructor.agent.Body;
import net.webstructor.agent.Farm;
import net.webstructor.al.Writer;
import net.webstructor.peer.Conversationer;

public class App extends Farm {

	private static App app = null;
	public static App getApp() {
		return app;
	}
	
	Frame frame;
	Tabs tabs;
	Chatter chatter;	
	JTextArea chat;
	ToolBar bar;
	Eyes eyes;
	Component lastSelectedTab = null;
	
    protected String newline = "\n";
    protected String prompt = ">";
	
	public void inputText(String text) {
		outputMyText(text);//mirror to output
		chatter.input(text);
	}
	
	public void outputMyText(String text) {
		App.getApp().bar.info.setText(null);
		outputText(">"+Writer.capitalize(new StringBuilder(text)));
	}
	
	public void outputText(String text) {
		chat.append(text+newline);
		chat.setCaretPosition(chat.getDocument().getLength());
	}

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from
     * the event dispatch thread.
     */
    public App(String[] args) {
    	super(args,
    			true,//enable logger
    			false, //TODO:disable console!!!
    			true,true,true,//enable email, http, telnet
    			false,//no social networking?
    			Conversationer.WORKERS);
		app = this;
    	
    	frame = new Frame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        eyes = new Eyes();        
		bar = new ToolBar();
		tabs = new Tabs();
		frame.add(Eyes.label = new JLabel(Eyes.icons[0]),BorderLayout.PAGE_START);
        frame.add(tabs, BorderLayout.CENTER);
        frame.add(bar, BorderLayout.PAGE_END);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        eyes.start();
               
        showLoginTabs();
        bar.setVisibility(tabs.chatPanel);
        chatter = new Chatter(this);
    }
	
    //TODO: login form via properties!!!
	void showLoginTabs() {
        tabs.setSelected(tabs.chatPanel);
        tabs.setEnabled(tabs.chatPanel,true);
        tabs.setEnabled(tabs.sitesPanel,false);
        tabs.setEnabled(tabs.thingsPanel,false);
        tabs.setEnabled(tabs.newsPanel,false);
        tabs.setEnabled(tabs.peersPanel,false);
        tabs.setEnabled(tabs.propsPanel,false);
	}

	void showTalkTabs() {
        tabs.setSelected(tabs.chatPanel);
        tabs.setEnabled(tabs.chatPanel,true);
        tabs.setEnabled(tabs.sitesPanel,true);
        tabs.setEnabled(tabs.thingsPanel,true);
        tabs.setEnabled(tabs.newsPanel,true);
        tabs.setEnabled(tabs.peersPanel,true);
        tabs.setEnabled(tabs.propsPanel,false);
	}

	void showWorkTabs() {
		if (lastSelectedTab != null)
			tabs.setSelected(lastSelectedTab);
        tabs.setEnabled(tabs.chatPanel,true);
        tabs.setEnabled(tabs.sitesPanel,true);
        tabs.setEnabled(tabs.thingsPanel,true);
        tabs.setEnabled(tabs.newsPanel,true);
        tabs.setEnabled(tabs.peersPanel,true);
        tabs.setEnabled(tabs.propsPanel,false);
	}

	void showPropTabs(String qualifier,String[] propNames,boolean poll) {
        DataModelProps props = (DataModelProps) ((Table)tabs.propsPanel).model;
        props.polling = poll;
        props.setContext(qualifier,propNames);
		Component tab = tabs.tabbedPane.getSelectedComponent();
		if (tab != tabs.propsPanel)
			lastSelectedTab = tab;
		else
			props.selected();//otherwise .selected() will not be called implicitly?
        tabs.setSelected(tabs.propsPanel);
        tabs.setEnabled(tabs.chatPanel,true);
        tabs.setEnabled(tabs.sitesPanel,false);
        tabs.setEnabled(tabs.thingsPanel,false);
        tabs.setEnabled(tabs.newsPanel,false);
        tabs.setEnabled(tabs.peersPanel,false);
        tabs.setEnabled(tabs.propsPanel,true);
         ((Table)tabs.propsPanel).table.grabFocus();
	}
		
	Table table() {
        Component c = tabs.tabbedPane.getSelectedComponent();
        if (c instanceof Table)
        	return ((Table)c);
        return null;
	}
	
	DataModel dataModel() {
        return table() == null ? null : table().model;
	}
	
	void displayPending(int count) {
        frame.setBadge(count > 0 ? Integer.toString(count) : null);
	}
	
	public static void open(String uri) {
	  if (Desktop.isDesktopSupported()) {
		  try {
			  Desktop.getDesktop().browse(new URI(uri));
		  } catch (Exception e) { 
	    	  ;// TODO: error handling
	      }
	   } else {
		   ;// TODO: error handling
	   }
	}
	
    public static void main(String[] args) {
    	if (System.getProperty("os.name").toLowerCase().contains("mac")){
    		System.setProperty("apple.laf.useScreenMenuBar", "true");
    		System.setProperty("com.apple.mrj.application.apple.menu.about.name", Body.APPNAME);
    	}
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
    	final String[] myargs = args;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
            	UIManager.put("swing.boldMetal", Boolean.FALSE);
            	new App(myargs).start();
            }
        });
    }
    
}
