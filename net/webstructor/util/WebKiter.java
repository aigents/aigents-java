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
package net.webstructor.util;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/*
References:
https://en.wikipedia.org/wiki/Headless_browser
https://github.com/dhamaniasad/HeadlessBrowsers
http://stackoverflow.com/questions/814757/headless-internet-browser/814929#814929
https://gist.github.com/evandrix/3694955
https://en.wikipedia.org/wiki/HtmlUnit (crashes and hangs on many sites)
https://en.wikipedia.org/wiki/PhantomJS (standalone or for Node.JS)
https://en.wikipedia.org/wiki/Selenium_(software)#Selenium_WebDriver (need separate browser)
https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino (just scripting engine)

Headless WebKit - impossible:
http://stackoverflow.com/questions/22730227/javafx-snapshot-without-showing-application-or-scene
http://ithubinfo.blogspot.ru/2013/11/how-to-install-and-configure-xvfb-in.html
http://stackoverflow.com/questions/20279336/javafx-in-headless-mode
*/

public class WebKiter { //implements Runnable {
	static {
		//TODO: in Java 9?
	    //System.setProperty("java.awt.headless", "true");
	}
	
	private static boolean initialized = false;
    private static WebKiter kiter = null;
    
    private WebView view = null;
    private WebEngine engine = null;
    
	static String getContent(NodeList nodes){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nodes.getLength(); i++){
			Node node = nodes.item(i);
			short type = node.getNodeType();
			String name = node.getNodeName();
			if (type == Node.COMMENT_NODE || name.equals("SCRIPT") || name.equals("NOSCRIPT") || name.equals("STYLE"))
				continue;
			if (type == Node.TEXT_NODE){
				String t = node.getTextContent();
				if (t != null && t.trim().length() > 0)
					sb.append(t);
			}
			NodeList c = node.getChildNodes();
			if (c != null)
				sb.append(getContent(c));
		}
		return sb.toString();
	}

    static WebKiter getInstance() {
    	if (!initialized){
    		initialized = true;
    	    // Create swing components on AWT thread:
      	    SwingUtilities.invokeLater(new Runnable() {
				//@Override
				public void run() {
					if (!GraphicsEnvironment.isHeadless()) {
					    new JFXPanel();//just initialize
					    kiter = new WebKiter();
					}
				}
    		});
     	    //TODO: wait till above completes or fails
    	    try {Thread.sleep(2000);} catch (InterruptedException e) {}
    	}
    	return kiter;
    }
    
	private final StringBuilder sb = new StringBuilder();
	boolean ready;
    public String run(final String url) {
    	String result = null;
    	try {
			synchronized (sb){
				sb.setLength(0);
				ready = false;
		    	Runnable r = new Runnable(){
		    	    //@Override
		    	    public void run() {
		    	    	if (engine == null) {
		    	    		view = new WebView();//need view otherwise engine may throw exception on missed parent on timer event
		    	    		engine = view.getEngine();
		    	    		//engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
		    	    		engine.getLoadWorker().stateProperty().addListener(new ChangeListener() {
			    	  		  	//@Override
			    	  		  	//public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
			    	  		  	public void changed(ObservableValue ov, Object oldState, Object newState) {
			    	  		  		if (newState == State.SUCCEEDED) {
			    	  		  			Document doc = engine.getDocument();
			    	  		  			NodeList nodes = doc.getElementsByTagName("body");
			    	  		  			String x = getContent(nodes);
//System.out.println("--succeeding("+oldState+"->"+newState+"):"+url);				
			    	  					synchronized (sb){
			    	  						ready = true;
			    	  						sb.append(x);
			    	  						sb.notify();
			    	  					}
			    	  		  		} else if (newState == State.CANCELLED || newState == State.FAILED) {
//System.out.println("--failing("+oldState+"->"+newState+"):"+url);				
			    	  					synchronized (sb){
			    	  						ready = true;
			    	  						sb.notify();
			    	  					}
			    	  		  		}
			    	  		  	}
			    	  		});
		    	    	}
//System.out.println("--loading:"+url);				
		    	  		engine.load(url);
		    	    }
		    	};
		    	Platform.runLater(r);
		  		while (!ready)
		  			sb.wait();
				result = sb.length() == 0 ? null : sb.toString();
//System.out.println("-->"+result.substring(0, 10));				
			}
		} catch (InterruptedException e) {
//TODO: handle via env
//System.out.println(e.toString());			
		}   	
    	return result;
    }
    
    private static void test(String url){
	    WebKiter wk = WebKiter.getInstance();
    	if (wk != null){
    		String s = wk.run(url);
    		System.out.println("--- "+url+" ---");
    		System.out.println(s == null ? "FAILED" : s);
    	}
    }
    
	public static void main(String[] args) {
		//TODO: in Java 9?
		//System.setProperty("java.awt.headless", "true");
		//java.awt.Toolkit.getDefaultToolkit();

	    if (WebKiter.getInstance() == null) {
		    System.out.println("Not supported!");
		    return;
	    }
	    test("http://www.etp-micex.ru/auction/catalog/all/#/smallOrderTitle/отделочных/limit/25/");
	    test("http://aigents.com/ru");
	    test("http://aigentsyuyuud.com/ru");
	    System.exit(0);
	}
	    
}
