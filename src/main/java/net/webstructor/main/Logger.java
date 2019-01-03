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
package net.webstructor.main;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.text.SimpleDateFormat;

import net.webstructor.al.Time;

public class Logger {

	static Logger m_logger = null;
	
	public static Logger getLogger() {
		if (m_logger == null)
			m_logger = new Logger();
		return m_logger;
	}

	private long m_ticket = 0;
	//private int m_day = 0;
	//private int m_month = 0;
	private Date m_date = null;
	private BufferedWriter m_writer = null;
	private OutputStreamWriter m_streamWriter = null;
	private SimpleDateFormat m_timeFormat;
	private String m_prefix = "webcat";

	public Logger() {
		m_timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS"); 
	}
	
	public Logger(String prefix) {
		this();
		m_prefix = prefix;
	}
	
	private synchronized long getTicket() {
		return ++m_ticket;
	}
	
	private void reopenFile(Date date) throws IOException {
		if (m_writer != null)
			m_writer.close();
		String name = new SimpleDateFormat("yyyy-MM-dd").format(date); 
		m_writer = new BufferedWriter(m_streamWriter = new OutputStreamWriter(
        	    new FileOutputStream(m_prefix+"-"+name+"-log.txt",true), "UTF-8"));
	}
	
	private void flush() throws IOException {
		//TODO: make flushing based on options
		m_writer.flush();	
		m_streamWriter.flush();
	}
	
	private void checkReopenFile(Date now) throws IOException {
		//TODO: check if file is missed and re-create if so
		/*
		int month = 1+now.getMonth();
  		int day = 1+now.getDay();
		if (day != m_day || month != m_month)
			reopenFile(now);
		m_day = day;
		m_month = month;
		*/
		Date date = Time.date(now);
		if (m_date == null || !date.equals(m_date)){
			reopenFile(now);
			m_date = date;
		}
	}

	private synchronized void writeToFile(String text) throws IOException {
		m_writer.write(text);
		m_writer.newLine();
	}
	
	private void writeToFile(long ticket, Date now, String pref, String text) throws IOException  {
		StringBuilder sb = new StringBuilder();
		sb
			.append(new Long(ticket).toString()).append(':')
			.append(pref).append(':')
			.append(m_timeFormat.format(now)).append(':')
			.append(text);
		writeToFile(sb.toString());
		flush();
	}

	public void log(String text,String tag) throws IOException {
		Date now = new Date();
		checkReopenFile(now);
		writeToFile(Thread.currentThread().getId(),now,tag,text);
	}
	
	public long logOut(String out,long ticket) throws IOException {
		Date now = new Date();
		checkReopenFile(now);
		writeToFile(ticket,now,"output",out);
		return ticket;
	}
	
	public long logIn(String in) throws IOException {
		long ticket = getTicket();
		Date now = new Date();
		checkReopenFile(now);
		writeToFile(ticket,now,"input",in);
		return ticket;
	}
	
}
