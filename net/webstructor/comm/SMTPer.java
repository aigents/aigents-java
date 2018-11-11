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
/*
package net.webstructor.comm;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SMTPer {
	public static final String DEFAULT_PASSWORD = "replace this with proper password";
	
	private String email = "";
	private String email_login = "";
	private String email_password = DEFAULT_PASSWORD;
	
	private Properties props = new Properties();

	private static Emailer emailer = null; 
	public static Emailer getEmailer() {
		return emailer;
	}
	
	//https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html
	//https://javamail.java.net/nonav/docs/api/com/sun/mail/pop3/package-summary.html

	public SMTPer() {
        email_login = "whoever@aigents.com";
        email_password = null;
        
        //TODO: This was used to solve problem with sporadic illegalstateexception in pop3folder.close on android
        //but did not solve it, yet made email texts unparseable by Sessioner/Conversationer 
        //http://javatalks.ru/topics/37247?page=1#213288
        //MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        //	mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        //	mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        //	mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        //	mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        //	mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        //	CommandMap.setDefaultCommandMap(mc);        
        
        props.put("mail.smtp.host", 			"mail.aigents.org");//"mail.aigents.org"));
        props.put("mail.smtp.auth", 			"true");
        props.put("mail.smtp.port", 			"465");//(25 - default, 465 - for secure ssl connection, 587 - for secure tls connection)
        props.put("mail.smtp.ssl.enable",		"true");//for port 465
        props.put("mail.smtp.starttls.enable",	"false");//for no port 587
        props.put("mail.store.protocol", 		"pop3");
        props.put("mail.pop3s.host",			"mail.aigents.org");
        props.put("mail.pop3s.port", 			"995");
        props.put("mail.pop3.starttls.enable", 	"true");//TODO:???
        props.put("mail.debug", 				"false");        
	}
	
	//example: http://ru.wikipedia.org/wiki/JavaMail
	public void sendMail(String to,String subject,String text) {
        try {        
            Session session = Session.getInstance(props);
            InternetAddress from = new InternetAddress(email);
            Transport bus = session.getTransport("smtp");
            bus.connect(email_login, email_password);
    		{
                Message msg;
                InternetAddress[] address = {new InternetAddress(to)};
                msg = new MimeMessage(session);
                msg.setFrom(from);
                msg.setRecipients(Message.RecipientType.TO, address);
                msg.setSubject(subject);
                msg.setSentDate(new Date());
                msg.setText(subject);
                msg.setText(text);
                //msg.setContent(email.text,"text/plain");//TODO:test Message's content as a Multipart object. 
                //msg.saveChanges();
                bus.sendMessage(msg, address);                                
                System.out.println("Email sent to "+to+".");//TODO:cleanup
    		}
            bus.close(); 
        }
        catch (MessagingException mex) {
            System.out.println(mex.toString());
            mex.printStackTrace();
            Exception ex;
            while ((ex = mex.getNextException()) != null) {
                System.out.println(ex.toString());
                ex.printStackTrace();
                if (!(ex instanceof MessagingException)) 
                	break;
                else 
                	mex = (MessagingException)ex;
            }
        }
	}

}//class
*/
