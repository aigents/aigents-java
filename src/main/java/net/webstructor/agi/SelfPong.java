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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/*
https://elgoog.im/breakout/
https://gym.openai.com/envs/Breakout-v0/
http://blog.jzhanson.com/blog/rl/project/2018/05/28/breakout.html

Yb(0-2)	Xb(0-4)	X(1-3)	Mr(L|R|S)	Happy(0-1)	Sad	Note
0	1	1	0	0	0	started right
1	0	1	1	0	0	
2	1	2	1	0	0	
1	2	3	0	0	0	
0	3	3	0	1	0	
1	4	3	-1	0	0	
2	3	2	-1	0	0	
1	2	1	0	0	0	
0	1	1	0	1	0	cont
0	1	1	0	0	0	kept right
1	0	1	0	0	0	
2	1	1	0	0	0	
1	2	1	0	0	0	
0	3	1	0	0	1	may stop
1	4	1	1	0	0	
2	3	2	1	0	0	
1	2	3	1	0	0	
0	1	3	0	0	1	may stop
 */
class SelfPong extends Game {
	Integer Yball, Xball, Xrocket, move, sad, happy;
	
	int Ymax, Xmax;
	int Ydir, Xdir;
	
	protected boolean random;
	protected boolean rocket;//whether to return the rocket state to player
	boolean delayed;
	
	boolean reflected = false;
	protected int totalHappy = 0;
	protected int totalSad = 0;
	
	SelfPong(int Ymax, int Xmax, boolean random, boolean rocket, boolean delayed){
		this.Ymax = Ymax;
		this.Xmax = Xmax;
		this.random = random;
		this.rocket = rocket;
		this.delayed = delayed;
	}
	
	@Override
	void printHeader() {
		System.out.format("Yball\tXball\tXrock\tMove\tHappy\tSad\n");
	}
	
	@Override
	void printState() {
		System.out.format("%s\t%s\t%s\t%s\t%s\t%s\n",Yball,Xball,Xrocket,move,happy,sad);
	}
	
	@Override
	void printFooter() {
		System.out.format("\t\t\t\t%s\t%s\n",totalHappy,totalSad);
		
	}
	
	@Override
	String toString(State s) {
		return String.format("%s\t%s\t%s\t%s\t%s\t%s\n",s.p.get("Yball"),s.p.get("Xball"),s.p.get("Xrocket"),s.p.get("Move"),s.p.get("Happy"),s.p.get("Sad"));
	}
	
	@Override
	void init() {
		reflected = false;
		totalHappy = 0;
		totalSad = 0;
		Yball = 0;//start from the floor
		Xdir = -1;//strike to left
		Xball = 1;//leftmost position
		if (random) {
			int[] states = new int[Xmax/2];
			for (int i = 0; i < states.length; i++)
				states[i] = 1 + i*2;
			Xdir = random(0,1) == 0 ? -1 : +1;
			Xball = random(states);//1,3,5,7,...
		}
		Xrocket = Xball;
		Ydir = 1;//strike to ceiling
		move = 0;//not decided
		sad = 0;
		happy = 0;
	}
	
	@Override
	State next(Integer action) {
		if (action != null)
			move = action;
		//change ball state
		if (Yball == Ymax) {//change Yball
			Ydir = -1;
			Yball--; 
		} else
		if (Yball > 0) {
			Yball += Ydir;
		} else { //Yball = 0 
			Ydir = 1;
			Yball++;
		}
		if (Xball == Xmax) {//change Xball
			Xdir = -1;
			Xball--;
		} else
		if (Xball > 0) {
			Xball += Xdir;
		} else {
			Xdir = 1;
			Xball++;
		}
		//chnage rocket state
		//if (move < 0 && Xrocket > 1)
		if (move < 0 && Xrocket > 0)
			Xrocket--;
		else 
		//if (move > 0 && Xrocket < (Xmax-1))
		if (move > 0 && Xrocket < Xmax)
			Xrocket++;
		sad();
		happy();
		State s = new State();
		fill(s);
		//move = p.move(this,s);
		return s;
	}

	void sad() {
		if (Yball == 0 && Xrocket != Xball) {
			totalSad++;
			sad = 1;
			//TODO init(); //start the game over
		} else {
			sad = 0;
		}
	}

	void happy() {
		if (!delayed) {
			if (Yball == 0 && Xrocket == Xball) {
				totalHappy++;
				happy = 1;
			} else {
				happy = 0;
			}
		} else {
			if (Yball == 0 && Xrocket == Xball) {
				reflected = true;
			}
			if (Yball == Ymax && reflected) {
				reflected = false;
				totalHappy++;
				happy = 1;
			} else {
				happy = 0;
			}
		}
	}
	
	void fill(State s) {
		s.add("Yball", Yball).add("Xball", Xball).add("Sad", sad).add("Happy", happy);
		if (rocket)
			s.add("Xrocket", Xrocket);
	}

	@Override
	void render(Graphics g) {
		Rectangle bounds = g.getClipBounds();
		if (sad > 0 || happy > 0) {
	    	g.setColor(sad > 0 ? Color.RED : Color.GREEN);
	    	g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
		}
		int rocket_h = 5;
		int w = bounds.width;
		int h = bounds.height - rocket_h;
		int d = Math.min(w/(Xmax+1), h/(Ymax+1));
		int dd = 0;//d/3;
    	g.setColor(Color.ORANGE);
        g.fillOval(Xball * (w - d) / Xmax, (Ymax - Yball) * (h-d) / Ymax, d, d);
    	g.setColor(Color.GRAY);
        g.fillRect(Xrocket * (w - d) / Xmax - dd, h, d + dd + dd, rocket_h);
        String rate = totalHappy > 0 || totalSad > 0 ? "Rate:"+(totalHappy*100/(totalHappy+totalSad))+"%" : "";
        String status = String.format("Happy:%s Sad:%s %s",totalHappy,totalSad,rate);
    	g.setColor(Color.BLACK);
        g.drawString(status, 16, 16);
	}

	@Override
	String getTitle() {
		return "Self Pong";
	}

	@Override
	Set<Integer> domain(String key) {
		Set<Integer> res = new HashSet<Integer>();
		if ("Move".equals(key)) {
			res.add(-1);
			res.add(0);
			res.add(1);
		}
		if ("Xrocket".equals(key)) {
			for (int x = 0; x <= Xmax; x++)
				res.add(x);
		}
		if ("Xball".equals(key)) {
			for (int x = 0; x <= Xmax; x++)
				res.add(x);
		}
		if ("Yball".equals(key)) {
			for (int y = 0; y <= Ymax; y++)
				res.add(y);
		}
		return res;
	}
}
