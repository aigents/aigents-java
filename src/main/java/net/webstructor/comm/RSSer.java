/*
 * MIT License
 * 
 * Copyright (c) 2005-2019 by Anton Kolonin, Aigents
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.cat.HtmlStripper;
import net.webstructor.core.Thing;
import net.webstructor.data.ContentLocator;
import net.webstructor.data.SocialFeeder;
import net.webstructor.peer.Profiler;
import net.webstructor.self.Matcher;
import net.webstructor.util.MapMap;
import net.webstructor.util.Str;


class RSSItem {
	String title = null;
	String description = null;
	String link = null;
	String category = null;
	String comments = null;
	Date date = null;
	//String author = null;
	String image = null;
	String id = null;
	boolean valid() {
		return !AL.empty(title) && !AL.empty(description) && AL.isURL(link) && date != null;
	}
}

public class RSSer implements Crawler {
	Body body;
	Matcher matcher;
	
	public RSSer(Body body) {
		this.body = body;
		matcher = body.getMatcher();
	}
	
	public int crawl(String uri, Collection topics, Date time, MapMap collector) {
		if (!AL.isURL(uri))
			return -1;
//TODO: make time mased on Selfer.minCheckCycle!?
		//long time = System.currentTimeMillis();//enforse re-reads
		String xml = body.filecacher.readCached(uri, time.getTime(), null, null, null, null, true);
		//Atom
		//<?xml version="1.0" encoding="utf-8"?>
		//<feed xmlns="http://www.w3.org/2005/Atom">
		//RSS:
		//<?xml version="1.0" encoding="windows-1252"?>
		//<rss version="2.0">
		if (AL.empty(xml) || !xml.startsWith("<?xml version=\"1.0\""))
			return -1;
		Date since = Time.today(-1);
		//https://www.viralpatel.net/java-xml-xpath-tutorial-parse-xml/
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			int res = crawlRSS(document, topics, collector, uri, since);
			return res != -1 ? res : crawlAtom(document, topics, collector, uri, since);
		} catch (Exception e) {
		    body.error("RSS parse", e); 
		}
		return -1;
	}
	
	static boolean empty(NodeList nl) {
		return nl == null || nl.getLength() < 1; 
	}

	static String textdata(Node node) {
		String data = node.getTextContent();
		return AL.empty(data) ? data : Str.striptags(data,"<![CDATA[", "]]>");
	}
	
	static String attribute(Node node, String name) {
		if (node == null)
			return null;
		NamedNodeMap map = node.getAttributes();
		if (map == null)
			return null;
		node = map.getNamedItem(name);
		if (node == null)
			return null;
		String value = node.getTextContent();
		return value; 
	}
	
	int match(RSSItem it, Collection topics, MapMap collector, ContentLocator titler, ContentLocator imager, String rootPath) {
		String text = HtmlStripper.convert(it.description,HtmlStripper.block_and_break_tags,HtmlStripper.block_breaker,null, imager.getMap(it.link), null, titler.getMap(it.link), rootPath);
		titler.getMap(it.link).put(0, it.title);//hack to set the only title possible!!!
//TODO: consider if we want to dive further with PathFinder if context of Siter has range > 1 
		return matcher.matchThingsText(topics,text,Time.date(it.date),it.link,it.image,collector,titler,imager);
	}
	
	int crawlRSS(Document document, Collection topics, MapMap collector, String baseRootPath, Date since){
		//RSS:
		//<?xml version="1.0" encoding="windows-1252"?>
		//<rss version="2.0">
		//https://www.aitrends.com/feed/
		NodeList nl = document.getElementsByTagName("rss");
		if (empty(nl))
			return -1;
		int matches = 0;
		Node rss = nl.item(0);
		nl = rss.getChildNodes();
		if (!empty(nl)) for (int c = 0; c < nl.getLength(); c++) {
			Node channel = nl.item(c);
			NodeList items = channel.getChildNodes();
			if (!empty(items) && channel.getNodeName().equals("channel")) for (int i = 0; i < items.getLength(); i++) {
				ContentLocator titler = new ContentLocator();
				ContentLocator imager = new ContentLocator();
				Node item = items.item(i);
				if (item.getNodeName().equals("item")) {
					RSSItem ri = new RSSItem();
					NodeList elems = item.getChildNodes();
					if (!empty(elems)) for (int e = 0; e < elems.getLength(); e++) {
						Node elem = elems.item(e);
						String name = elem.getNodeName();
						if (name.equals("title"))
							ri.title = elem.getTextContent();
						if (name.equals("description"))
							ri.description = textdata(elem);
						if (name.equals("link"))
							ri.link = elem.getTextContent();
//TODO category (multiple)
						if (name.equals("category"))
							//<category><![CDATA[Robotics]]></category>
							//<category><![CDATA[Self Driving Cars]]></category>
							ri.category = textdata(elem);
						if (name.equals("comments"))
							ri.comments = elem.getTextContent();
						if (name.equals("pubDate")) {
							String value = elem.getTextContent();
							ri.date = AL.empty(value) ? null : Time.time(value,"E, dd MMM yyyy HH:mm:ss Z");//<pubDate>Tue, 19 Oct 2004 11:09:11 -0400</pubDate>
							if (ri.date.compareTo(since) < 0)
								break;
						}
						if (name.equals("enclosure") && "image".equals(attribute(elem,"type")))
							// <enclosure url="http://localtest.com/test/garbage.jpg" type="image"/>
							ri.image = attribute(elem,"url");
						if (name.equals("guid"))
							//<guid isPermaLink="false">https://www.aitrends.com/?p=19129</guid>
							ri.id = elem.getTextContent();
//TODO author
						//<dc:creator><![CDATA[Benjamin Ross]]></dc:creator>
					}
					if (ri.date.compareTo(since) < 0)
						continue;
					if (AL.empty(ri.link) && !AL.empty(ri.id)&&  AL.isURL(ri.id))
						ri.link = ri.id;
					if (ri.valid())
						matches += match(ri, topics, collector, titler, imager, baseRootPath);
				}
			}
		}
		return matches;
	}

	int crawlAtom(Document document, Collection topics, MapMap collector, String baseRootPath, Date since){
		//Atom
		//<?xml version="1.0" encoding="utf-8"?>
		//<feed xmlns="http://www.w3.org/2005/Atom">
		NodeList nl = document.getElementsByTagName("feed");
		if (empty(nl))
			return -1;
		int matches = 0;
		Node feed = nl.item(0);
		nl = feed.getChildNodes();
		if (!empty(nl)) for (int i = 0; i < nl.getLength(); i++) {
			ContentLocator titler = new ContentLocator();
			ContentLocator imager = new ContentLocator();
			Node entry = nl.item(i);
			if (entry.getNodeName().equals("entry")) {
				RSSItem ri = new RSSItem();
				NodeList elems = entry.getChildNodes();
				String summary = null;
				String content = null;
				String icon = null;
				String logo = null;
				if (!empty(elems)) for (int e = 0; e < elems.getLength(); e++) {
					Node elem = elems.item(e);
					String name = elem.getNodeName();
					//https://validator.w3.org/feed/docs/atom.html
					/*
					  <entry>
					    <title>Atom-Powered Robots Run Amok</title>
					    <link href="http://example.org/2003/12/13/atom03"/>
					    <id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</id>
					    <updated>2003-12-13T18:30:02Z</updated>
					    <summary>Some text.</summary>
					  </entry>
					*/
					if (name.equals("title"))
						ri.title = elem.getTextContent();
					if (name.equals("summary"))
						summary = textdata(elem);
					if (name.equals("content"))
						content = textdata(elem);
					if (name.equals("link"))
						ri.link = attribute(elem,"href");
					if (name.equals("id"))
						ri.id = elem.getTextContent();
					if (name.equals("updated")) {
						String value = elem.getTextContent();
						ri.date = AL.empty(value) ? null : Time.time(value,"yyyy-MM-dd'T'HH:mm:ss'Z'");//2003-12-13T18:30:02Z
						if (ri.date.compareTo(since) < 0)
							break;
					}
					if (name.equals("icon"))
						icon = elem.getTextContent();
					if (name.equals("logo"))
						logo = elem.getTextContent();
//TODO category (multiple)
					if (name.equals("category"))
						//https://validator.w3.org/feed/docs/atom.html#optionalEntryElements
						ri.category = attribute(elem,"term");
//TODO contributor -> author
					//https://validator.w3.org/feed/docs/atom.html#recommendedEntryElements
					//https://validator.w3.org/feed/docs/atom.html#person
				}
				if (ri.date.compareTo(since) < 0)
					continue;
				if (!AL.empty(ri.id) && AL.isURL(ri.id))
					ri.link = ri.id;
				ri.description = AL.empty(summary) ? content : AL.empty(content) ? summary : summary + "<p>" + content;
				ri.image = !AL.empty(icon) && AL.isIMG(icon) ? icon : logo;
				if (ri.valid())
					matches += match(ri, topics, collector, titler, imager, baseRootPath);
			}
		}
		return matches;
	}

	public String name() {
		return "rss";
	}
	public SocialFeeder getFeeder(String id, String token, String key, Date since, Date until, String[] areas) throws IOException {
		return null;
	}
	public Profiler getProfiler(Thing peer) {
		return null;
	}
}
