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
package net.webstructor.comm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;

import net.webstructor.agent.Body; 
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.core.Thing;
import net.webstructor.util.Array;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

class Email {
	public String to;
	public String subject;
	public String text;
	public Email(String to, String subject, String text) {
		this.to = to;
		this.subject = subject;
		this.text = text;
	}
}

//TODO: split into JavaEmailer and LinuxEmailer
public class Emailer extends Communicator {
	public static final String DEFAULT_PASSWORD = "replace this with proper password";
	private static final long DEFAULT_EMAIL_CYCLE_MS = 10*60*1000;
	private static final long MINIMUM_EMAIL_CYCLE_MS = 10*1000;
	
	private static final String[] blacklist = {
		"mailer-daemon"
	};

	private String email = "";
	private String email_login = "";
	private String email_password = DEFAULT_PASSWORD;
	private int email_retries = 10;

	private int errors = 0;
	
	private Properties props = new Properties();

	private LinkedList outQueue= new LinkedList();//Email
	
	private static Emailer emailer = null; 
	public static Emailer getEmailer() {
		return emailer;
	}
	
	/*
	 * https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html
	 * https://javamail.java.net/nonav/docs/api/com/sun/mail/pop3/package-summary.html
	 */
	public Emailer(Body body) {
		super(body);
		updateProps();
		if (emailer == null)
			emailer = this;
		//logger = new Logger("emailer");
		body.sleep(100);
	}

	private void updateProps() {
		Thing self = body.self();
        email = self.getString(AL.email,email);
        email_login = self.getString(Body.email_login,email_login);
        email_password = self.getString(Body.email_password,email_password);
        email_retries = AL.integer(self.getString(Body.email_retries,Integer.toString(email_retries)),email_retries);
        if (!valid(email))
        	email = email_login;
        
        /*
        //TODO: This was used to solve problem with sporadic illegalstateexception in pop3folder.close on android
        //but did not solve it, yet made email texts unparseable by Sessioner/Conversationer 
        //http://javatalks.ru/topics/37247?page=1#213288
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        	mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        	mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        	mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        	mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        	mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        	CommandMap.setDefaultCommandMap(mc);        
        */
        
        props.put("mail.smtp.host", 			self.getString(Body.mail_smtp_host,"smtp.gmail.com"));//"mail.aigents.org"));
        props.put("mail.smtp.auth", 			self.getString(Body.mail_smtp_auth,"true"));
        props.put("mail.smtp.port", 			self.getString(Body.mail_smtp_port,"465"));//(25 - default, 465 - for secure ssl connection, 587 - for secure tls connection)
        props.put("mail.smtp.ssl.enable",		self.getString(Body.mail_smtp_ssl_enable,"true"));//for port 465
        props.put("mail.smtp.starttls.enable",	self.getString(Body.mail_smtp_starttls_enable,"false"));//for no port 587
        props.put("mail.store.protocol", 		self.getString(Body.mail_store_protocol,"pop3"));
        props.put("mail.pop3s.host",			self.getString(Body.mail_pop3s_host,"pop.gmail.com"));//"mail.aigents.org"));
        props.put("mail.pop3s.port", 			self.getString(Body.mail_pop3s_port,"995"));
        props.put("mail.pop3.starttls.enable", 	self.getString(Body.mail_pop3_starttls_enable,"true"));//TODO:???
        props.put("mail.debug", 				"false");        
	}
	
	boolean emailable(){
		return !DEFAULT_PASSWORD.equals(body.self().getString(Body.email_password))
			&& !AL.empty(email_login) && !Body.testEmail(email_login) && valid(email_login) && valid(email);
	}
	
	public void run() {
		long next = System.currentTimeMillis();
		while (alive()) {
			try {
				updateProps();
		        long email_cycle = new Period(DEFAULT_EMAIL_CYCLE_MS).parse(body.self().getString(Body.email_cycle,new Period(DEFAULT_EMAIL_CYCLE_MS).toString()));
				for (;;){
					Thread.sleep(MINIMUM_EMAIL_CYCLE_MS);
					long now = System.currentTimeMillis();
					if (pending() || now > next){//process email if either time comes or have to send something
						try {
							if (emailable()){
								body.reply("Emailing times "+new Date(System.currentTimeMillis()).toString()+".");
								//may need to read email just in order to send email
								readMail(readable());					
								//send notifications in any case
								sendMail();
							}
							errors = 0;
						} catch (Exception e) {
							body.error("Email communication error", e);
							if (++errors > email_retries)
								terminate();
						}
						if (next < now)
							next = now + email_cycle;
					}
				}
			} catch (Exception e) {
				//TODO: what if sleep fails?
			}
		}//while
	}//run

	private static String sendmailExec(String from,String to,String subject,String text) throws Exception {
		int result = -1;
		StringBuffer output = new StringBuffer();
		String cmd = "From:"+from+"\nTo:"+to+"\nSubject:"+subject+"\nContent-Type: text/plain; charset=\"UTF-8\"\n\n"+text+"\n";
		String line;
	    Process p = Runtime.getRuntime().exec("sendmail -t");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(),"UTF8"));
		writer.append(cmd);
		writer.close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		result = p.waitFor();
		while ((line = reader.readLine())!= null)
			output.append(line + "\n");
		return result == 0 ? null : output.toString();
	}
	
	public void sendmail(String from,String to,String subject,String text){
		try {
			String err = sendmailExec(from,to,subject,text);
			body.debug((err == null ? "Success" : "Error")+" sending email to "+to+(err == null ? "" : " ("+err+")"));
		} catch (Exception e) {
			body.error("Error sending email to "+to,e);
		}
	}
	
	public void email(Thing peer,String subject,String text) throws IOException {
		String email = peer.getString(AL.email);
		if (AL.empty(email))
			throw new IOException("No peer email.");
		if (Body.testEmail(email))//don't send emails to test accounts
			return;
		
		//TODO: put this hack straight
		//TODO: alternative support for windows with no sendmail
		//if no SMTP configured, send email via command line
		if (AL.empty(body.self().getString(Body.mail_smtp_host))){
			sendmail(email_login,peer.getString(AL.email),subject,text);
			return;
		}
		
		synchronized (outQueue) {
			outQueue.add(new Email(email,subject,text));
		}
	}
	
	public void output(net.webstructor.peer.Session session, String message) throws IOException {
		int subjend = message.indexOf('\n');
		String subject = subjend == -1 ? message : message.substring(0,subjend);
		//output(session,message.replaceAll("[\n\r]", ""),message);
		output(session,subject,message);
	}
	
	public void output(net.webstructor.peer.Session session, String subject, String text) throws IOException {
		Thing peer = session.getPeer();
		email(peer,subject,text);
	}

	boolean readable() {
		try {
			//read emails only if email account is owned only by self
			Collection samemails = body.storager.getByName(AL.email,email_login);
			if (AL.empty(samemails) ||
				(samemails.size() == 1 && samemails.iterator().next().equals(body.self())))
				return true;
		} catch (Exception e) {
			body.error(e.toString(), e);
		}
		return false;
	}
	
	public boolean pending() {
        synchronized (outQueue) {
			if (outQueue.size() > 0)
				return true;
		}
        return false;
	}

	public static void sendMail(Session session,InternetAddress from,Transport bus,Email email) throws MessagingException {
        Message msg;
        InternetAddress[] address = {new InternetAddress(email.to)};
        msg = new MimeMessage(session);
        msg.setFrom(from);
        msg.setRecipients(Message.RecipientType.TO, address);
        msg.setSubject(email.subject);
        msg.setSentDate(new Date());
        msg.setText(email.subject);
//TODO:why both?
        msg.setText(email.text);
//TODO:encoding (some people's mailers don't get this right) 
        //msg.setContent(email.text,"text/plain");//TODO:test Message's content as a Multipart object. 
        //msg.saveChanges();
        bus.sendMessage(msg, address);                                
	}
	
	//example: http://ru.wikipedia.org/wiki/JavaMail
	public void sendMail() {
        synchronized (outQueue) {
			if (outQueue.size() == 0)
				return;
		}
        try {        
            Session session = Session.getInstance(props);
            InternetAddress from = new InternetAddress(email);
            Transport bus = session.getTransport("smtp");
            bus.connect(/*smtp_host,*/ email_login, email_password);         
    		for (;;) {
    			Email email;
    			synchronized (outQueue) {
    				email = (Email)outQueue.poll();
    			}
    			//TODO:check email oversending count check for safety reasons 
    			if (email == null)
    				break;
    			/*
                Message msg;
                InternetAddress[] address = {new InternetAddress(email.to)};
                msg = new MimeMessage(session);
                msg.setFrom(from);
                msg.setRecipients(Message.RecipientType.TO, address);
                msg.setSubject(email.subject);
                msg.setSentDate(new Date());
                msg.setText(email.subject);
                msg.setText(email.text);
                //msg.setContent(email.text,"text/plain");//TODO:test Message's content as a Multipart object. 
                //msg.saveChanges();
                bus.sendMessage(msg, address);           
                */                     
    			sendMail(session,from,bus,email);
                body.output("Email sent to "+email.to+".");//TODO:cleanup
    		}
            bus.close(); 
        }
        catch (MessagingException mex) {
            body.error(mex.toString(),mex);
            Exception ex;
            while ((ex = mex.getNextException()) != null) {
                body.error(ex.toString(),ex);
                if (!(ex instanceof MessagingException)) 
                	break;
                else 
                	mex = (MessagingException)ex;
            }
        }
	}

	//http://stackoverflow.com/questions/13474705/reading-body-part-of-a-mime-multipart
	public String getContent(Part message,String breaker) throws IOException, MessagingException {	
		Object content = message.getContent();
		if (content instanceof String) {
			String str = (String)content;
			if (message.isMimeType("text/html") || message.isMimeType("text/xml"))
				str = HtmlStripper.convert(str,HtmlStripper.block_breaker,null);
	        return str;
		}
	    if (content instanceof Multipart) {
	    	StringBuilder result = new StringBuilder();
	        Multipart multipart = (Multipart) content;
	        //Log.e("BodyPart", "MultiPartCount: "+multipart.getCount());
	        for (int j = 0, p = 0; j < multipart.getCount(); j++) {
	        	BodyPart bodyPart = multipart.getBodyPart(j);
	        	String disposition = bodyPart.getDisposition();
	        	if (disposition != null && (disposition.equalsIgnoreCase("ATTACHMENT"))) {
	        		//TODO:what???
	        		//DataHandler handler = bodyPart.getDataHandler();
	            }
	        	else { 
	        		if (p++ > 0)
	        			result.append(breaker);
	        		result.append(getContent(bodyPart,breaker));         
	            }
	        }
	        return result.toString();
	    }		
	    return "";
	}
		
	String getAddress(Address address) {
		return address instanceof InternetAddress ? ((InternetAddress)address).getAddress() : address.toString();
	}
	
	//http://stackoverflow.com/questions/13137405/retrieving-all-unread-emails-using-javamail
	//http://www.tutorialspoint.com/javamail_api/javamail_api_deleting_emails.htm
	public void readMail(boolean dohandle) {
		Store store = null;
		Folder emailFolder = null;
		try {
	         // get the session object
	         Session emailSession = Session.getDefaultInstance(props);
	         // emailSession.setDebug(true);
	         store = emailSession.getStore("pop3s");
	         store.connect(email_login, email_password);

	         // create the folder object and open it
	         emailFolder = store.getFolder("INBOX");
	         emailFolder.open(Folder.READ_WRITE);

	         Message[] messages = emailFolder.getMessages();
	         for (int i = 0; i < messages.length; i++) {
	            Message message = messages[i];
	            //TODO: copy and clean all resources to help GC and 
	            //avoid sporadic illegalstateexception in pop3folder.close on android
	            //TODO: use subject for three-level context resolution in conversationer
	            //TODO: process emails on statement-by-statement basis, answering every statement 
	            //separately and building cumulative response in the end
	            //String subject = new String(message.getSubject());//clone to help GC
	            String from = new String(getAddress(message.getFrom()[0]));//clone to help GC
	            String text = new String(getContent(message,".\n"));//clone to help GC
	            if (dohandle)
	            	message.setFlag(Flags.Flag.DELETED, true);
	            if (body.logger() != null)
	            	body.logger().logIn(from+":"+text);
	            if (Array.containsAnyAsSubstring(blacklist,from) == null) {
		            body.output("Email got from "+from+".");//TODO:cleanup
	            	net.webstructor.peer.Session session = body.sessioner.getSession(this,from);
		            if (dohandle)
		            	handle(session, text);
	            } else {
		            body.output("Email blocked from "+from+".");//TODO:cleanup
	            }
	            message = null;//help GC
	         }
	    } catch (NoSuchProviderException e) {
	         body.error(e.toString(),e);
	    } catch (MessagingException me) {
		     body.error(me.toString(),me);
	    } catch (IOException io) {
	         body.error(io.toString(),io);
	    }
		//close everything finally to avoid GC issues
		if (emailFolder != null) {
	         // expunges the folder to remove messages which are marked deleted
	         try {
	        	 emailFolder.close(true);
	         } catch (MessagingException e) {
		         body.error(e.toString(),e);
	         }
	         emailFolder=null;//help GC
		}
		if (store != null) {
	         try {
	        	 store.close();
	         } catch (MessagingException e) {
		         body.error(e.toString(),e);
	         }
	         store=null;//help GC
		}
	}
	
	void handle(net.webstructor.peer.Session session, String text) throws IOException {
		String[] lines = text.split("[\\r\\n]+");
		if (!AL.empty(lines[0]))
			body.conversationer.handle(this, session, lines[0]);
	}

	public static boolean valid(String email) {
		try {
			InternetAddress emailAddr = new InternetAddress(email);
		    emailAddr.validate();
		} catch (Exception ex) {
		    return false;
		}
		return true;
	}	

	private static void sendmailTest(String from,String to,String subject,String text) throws Exception {
		System.out.println("to:"+to);
		String err = sendmailExec(from, to, subject + " to "+ to , text + " to " + to);
		System.out.println(err);
	}
	
	public static void main(String[] args) {
		try {
			if (args.length > 1)
			Emailer.sendmailTest(args[0],args[1],"test sendmail","Мама мылА РаМу");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
}//class
