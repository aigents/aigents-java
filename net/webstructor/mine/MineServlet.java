/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class MineServlet extends HttpServlet {
	static final long serialVersionUID = 20080503;
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Hello from Mine!</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Hello from Mine!</h1>");
        out.println("<p>Your query string is " + ((HttpServletRequest)request).getQueryString()+"</p>");
        out.println("</body>");
        out.println("</html>");
    }
}
