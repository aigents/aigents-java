/*
 * MIT License
 * 
 * Copyright (c) 2005-2021 by Anton Kolonin, Aigents®
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
package net.webstructor.agi;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import net.webstructor.al.AL;
import net.webstructor.util.Array;
import net.webstructor.util.MapMap;
import net.webstructor.util.Str;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

class Striter {
	int pos;
	int len;
	String s;
	Striter(String s){
		this.s = s;
		pos = 0;
		len = s.length();
		
	}
	boolean next(char expected) {
		while (pos < len) {
			char c = s.charAt(pos);
			if (" \t\n\r".indexOf(c) == -1) {
				if (c == expected) {
					pos++;
					return true;
				}
				return false;
			}
			pos++;
		}
		return false;
	}
}

//https://gym.openai.com/docs/
class OpenGymSock extends Game {
	
	String env;
	int cycles;
	
	Integer action_max;

	Socket socket;
    protected OutputStream out;
    protected InputStream in;
    
    MapMap map2d = new MapMap();
    int dimensions[] = new int[] {0,0,0};
	
	OpenGymSock(String host, int port, String env, int cycles){
		this.env = env;
		this.cycles = cycles;
		try {
			socket = new Socket(host, port);
		    out = new BufferedOutputStream(socket.getOutputStream());
		    in   = new BufferedInputStream(socket.getInputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String input() throws IOException {
	      StringBuilder request = new StringBuilder();
	      while (true) {
	        int c = in.read(  );
	        if (c == -1) 
	        	break;
	        if (c == '\r' || c == '\n')
	        	break;
	        else
		        request.append((char) c);
	      }
//System.out.println("in:"+request.toString());
	      return request.toString();
	}

	private String inputData() throws IOException {
		StringBuilder data = new StringBuilder();
		int brackets = 0;
		String line;
		for (;;) {
			line = input();
			for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == '[')
					brackets++;
				if (c == ']')
					brackets--;
			}
			data.append(line);
			if (brackets == 0)
				break;
		}
		return data.toString();
	}
	
	private void output(String message) throws IOException {
		//TODO: use session
	    byte[] bytes;
		bytes = message.getBytes("UTF-8");
		out.write(bytes);
		out.write('\n');
		out.flush();
//System.out.println("out:"+message);
	}
	
	@Override
	void printHeader() {
		//System.out.format("Yball\tXball\tXrock\tMove\tHappy\tSad\n");
	}
	
	@Override
	void printState() {
		//System.out.format("%s\t%s\t%s\t%s\t%s\t%s\n",Yball,Xball,Xrocket,move,happy,sad);
	}
	
	@Override
	void printFooter() {
		//System.out.format("\t\t\t\t%s\t%s\n",totalHappy,totalSad);
		
	}
	
	@Override
	String toString(State s) {
		//return String.format("%s\t%s\t%s\t%s\t%s\t%s\n",s.p.get("Yball"),s.p.get("Xball"),s.p.get("Xrocket"),s.p.get("Move"),s.p.get("Happy"),s.p.get("Sad"));
		return env;
	}
	
	@Override
	void init() {
	    try {
			input();//env
			output(env);
			input();//cycles
			output(String.valueOf(cycles));
			String s = input();
			String d = Str.parseBetween(s, "(", ")");
			action_max = Integer.valueOf(d);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void parse1d(State s, String dump) {
		int start = dump.indexOf('[');
		if (start >= 0) {
			int end = dump.indexOf(']', start + 1);
			if (end > start) {
				dump = dump.substring(start + 1, end);
				StringTokenizer tok = new StringTokenizer(dump, " \t\n\r");
				int i = 0;
				while(tok.hasMoreTokens())
					s.add(String.valueOf(i++), Integer.parseInt(tok.nextToken().trim()));
			}
		}
	}
	
	
	void parse2d(State s, String dump) {
		map2d.clear();
		Striter iter = new Striter(dump);
		if (!iter.next('['))
			return;
		for (int y = 0; ; y++) {
			if (!iter.next('['))
				break;
			int end = iter.s.indexOf(']',iter.pos);
			if (end == -1)
				break;
			String rgb = dump.substring(iter.pos,end);
			StringTokenizer tok = new StringTokenizer(rgb, " \t\n\r");
			int x = 0;
			while(tok.hasMoreTokens()) {
				String t = tok.nextToken().trim();
				int i = Integer.parseInt(t);
				s.add(String.format("%s %s",y,x),i);
				if (map2d != null)
					map2d.putObject(new Integer(y), new Integer(x), new Integer(i));
				x++;
				if (dimensions[0] < x)
					dimensions[0] = x;
			}
			if (dimensions[1] < y)
				dimensions[1] = y; 
			iter.pos = ++end;
		}
	}
	
	//https://gym.openai.com/envs/Breakout-v0/
	//In this environment, the observation is an RGB image of the screen, which is an array of shape (210, 160, 3)
	void parse3d(State s, String dump) {
		Striter iter = new Striter(dump);
		if (!iter.next('['))
			return;
		for (int y = 0; ; y++) {
			if (!iter.next('['))
				break;
			for (int x = 0; ; x++) {
				if (!iter.next('['))
					break;
				int end = iter.s.indexOf(']',iter.pos);
				if (end == -1)
					break;
				String rgb = dump.substring(iter.pos,end);
				StringTokenizer tok = new StringTokenizer(rgb, " \t\n\r");
				int i = 0;
				while(tok.hasMoreTokens()) {
					String t = tok.nextToken().trim();
					i += Integer.parseInt(t);
				}
				s.add(String.format("%s %s",y,x),i);
				iter.pos = ++end;
			}
			if (!iter.next(']'))
				break;
		}
	}

	void parse(State s, String dump) {
		if (!AL.empty(dump)) {
			if (dump.startsWith("[[["))
				parse3d(s, dump);
			else
			if (dump.startsWith("[["))
				parse2d(s, dump);
			else
			if (dump.startsWith("["))
				parse1d(s, dump);
		}
	}
	
	//https://gym.openai.com/docs/
	@Override
	State next(Integer action) {
		State s = new State();
		try {
			String prompt = input();//action prompt
			if (!"action".equals(prompt)) {
System.out.println("Finished!");
				return null;
			}
			output(String.valueOf(action));
//TODO Game.over()
			String reward = input();
			String done = input();
			String observation = inputData();
//System.out.println("done:"+done+" reward:"+reward+" observation:"+observation);
			parse(s,observation);
			s.add("Sad",done.equals("True") ? 1 : 0);
			s.add("Happy",(int)Double.parseDouble(reward));
//TODO fill state			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return s;
	}

	@Override
	void render(Graphics g) {
		Rectangle bounds = g.getClipBounds();
		int w = bounds.width;
		int h = bounds.height;
		int x1 = 0;
		int y1 = 0;
		if (dimensions[0] > 0 && dimensions[1] > 0) {
			for (int x = 0; x < dimensions[0]; x++) {
				int x2 = (x + 1) * w / dimensions[0];
				for (int y = 0; y < dimensions[1]; y++) {
					Object p = map2d.getObject(new Integer(y), new Integer(x), false);
					if (p != null) {
						int s = 255 - (Integer)p;
						//int s = 0;//x * 255 / dimensions[1];
						Color cellColor = new Color(s,s,s);
						g.setColor(cellColor);
						int y2 = (y + 1) * h/ dimensions[1]; 
						g.fillRect(x1, y1, x2 - x1, y2 - y1);
						y1 = y2;
						//TODO invert!?
					}
				}
				x1 = x2;
			}
		}
		/*
		int s = 0;
		Color cellColor = new Color(s,s,s);
		g.setColor(cellColor);
		g.fillRect(x1, y1, w, h);
		*/
	}

	@Override
	String getTitle() {
		return "OpenAI Gym "+env;
	}

	@Override
	Set<Integer> domain(String key) {
		Set<Integer> res = new HashSet<Integer>();
		if ("Move".equals(key))
			for (int action = 0; action < action_max; action++)
				res.add(action);
		return res;
	}
	
	//Breakout-ram-v0 10000 Random - 50-62-65
	//Breakout-ram-v0 10000 SimSeqMat - 46-50-52
	public static void main(String[] args) {
		Game.randomise();
		GamePad gp = new GamePad(200,200);
		//Player p = new SimpleSequenceMatchingPlayer(true,0.5,true,false);
		Player p = new StateActionSpaceExpectingPlayer(0.5);
		//Player p = new ChangeActionSpaceMatchingPlayer(0.5);
		//OpenGymSock g = new OpenGymSock("127.0.0.1", 65432, "Breakout-ram-v0", 10000);
		OpenGymSock g = new OpenGymSock("127.0.0.1", 65432, "Breakout-v0", 10000);
		g.init();
		if (gp != null) {
			gp.setGame(g);
			gp.init(Array.toList(AgiTester.diag_params),Array.toList(AgiTester.diag_colors));
			gp.repaint();
		}
		Integer[] actions = g.domain("Move").toArray(new Integer[] {});
		Integer action = Game.random(actions);
		State s;
		int happys = 0;
		for (;;) {
			s = g.next(action);
			if (gp != null) {
 				State ds = new State(s,AgiTester.diag_params);
 				ds.merge(p.selfState());
 				gp.update(ds);
				gp.repaint();
			}
			if (s == null)
				break;
			if (s.value("Happy",0) > 0)
				happys++;
			//action = Game.random(actions);
			action = p.move(g, s);
		}
		System.out.println("Happys:"+happys);
	}
	
}
