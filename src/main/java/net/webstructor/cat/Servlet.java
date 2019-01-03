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
package net.webstructor.cat;

//http://127.0.0.1:9090/servlet/echo?a1=b1&c1=d1


import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;


public class Servlet extends GenericServlet {
    
    static final long serialVersionUID = 1;//20060512

    static StringBuffer lastInput = new StringBuffer();

    static void println(String s) {
        lastInput.append(s+"\n");
    }
    
    public void service(ServletRequest req, ServletResponse res) {
        //doSimpleTest(req,res); 
        doRealWork(req,res); 
    }

    public void doRealWork(ServletRequest req, ServletResponse res) {
        try {
        	CommandHandler.getServer("/home/kolonin/public_html/mine");//to ensure singleton is constructed
        	
    	    res.setContentType("text/html");
            OutputStream os = res.getOutputStream();    
            DataOutputStream bo = new DataOutputStream(new BufferedOutputStream(os));
            
            String cmd = ((HttpServletRequest)req).getQueryString();
            //cmd = URLDecoder.decode(cmd);            	
            cmd = HttpServer.decode(cmd);
            		
			CommandHandler ch = new CommandHandler(cmd,"<br>",";",",");
            ch.run();
            
            bo.writeBytes("<HTML><SCRIPT>function doLoad() {if (top) {top.postMessage(document.body.innerHTML,'*');}}");
            bo.writeBytes("</SCRIPT><BODY onload=\"doLoad()\">");
            bo.writeBytes(ch.getOutput());
            bo.writeBytes("</BODY></HTML>");
            
            bo.flush();
	        bo.close();
            res.flushBuffer();
        } catch (Exception e) {
            println("doSimpleTest: "+e.toString());
        }
    }
    
    public void doSimpleTest(ServletRequest req, ServletResponse res) {
        try {
    	    res.setContentType("text/html");
            OutputStream os = res.getOutputStream();    
            DataOutputStream bo = new DataOutputStream(new BufferedOutputStream(os));
	        bo.writeBytes("<html>\n");
            bo.writeBytes("<head>\n");
	        bo.writeBytes("<title>TestServlet</title>\n");
	        bo.writeBytes("</head>\n");
	        bo.writeBytes("<body>\n");
            bo.writeBytes("<h1>My Generic TestServlet!!!</h1>\n");
            bo.writeBytes("<p>This is a test.\n");
	        bo.writeBytes("<p>Your IP address is " + req.getRemoteAddr()+"\n");
            bo.writeBytes("<p>Your request class is " + req.getClass().getName()+"\n");
            bo.writeBytes("<p>Your current directory is " + System.getProperty("user.dir")+"\n");
            if (req instanceof HttpServletRequest) {
                bo.writeBytes("<p>Your query string is " + ((HttpServletRequest)req).getQueryString()+"\n");
            }
            else
                bo.writeBytes("<p>Your request is not Http one\n");
            bo.writeBytes("</body>\n");
	        bo.writeBytes("</html>\n");
            bo.flush();
	        bo.close();
            res.flushBuffer();
        } catch (Exception e) {
            println("doSimpleTest: "+e.toString());
        }
    }

}