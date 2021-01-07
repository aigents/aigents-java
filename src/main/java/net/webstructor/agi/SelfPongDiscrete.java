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
class SelfPongDiscrete extends SelfPong {
	
	SelfPongDiscrete(int Ymax, int Xmax, boolean random, boolean rocket, boolean delayed){
		super(Ymax, Xmax, random, rocket, delayed);
	}
	
	@Override
	String getTitle() {
		return "Self Pong Discrete";
	}

	void fill(State s) {
		for (int x = 0; x <= Xmax; x++) {
			if (rocket) 
				s.add(String.valueOf(x),x == Xrocket ? 1 : 0);
			for (int y = 0; y <= Ymax; y++) {
				String l = String.format("%s %s",x,y);
				s.add(l,x == Xball && y == Yball ? 1 : 0);
			}
		}
		s.add("Sad", sad).add("Happy", happy);
	}

	@Override
	void render(Graphics g) {
		super.render(g);
		Rectangle bounds = g.getClipBounds();
		int rocket_h = 5;
		int w = bounds.width;
		int h = bounds.height - rocket_h;
		int d = Math.min(w/(Xmax+1), h/(Ymax+1));
		int d2 = d/2;
		for (int x = 0; x <= Xmax; x++) {
	    	g.setColor(x == Xrocket ? Color.BLACK : Color.GRAY);
	        g.drawString(""+x, x * (w - d) / Xmax + d2, h - rocket_h);
			for (int y = 0; y <= Ymax; y++) {
		    	g.setColor(x == Xball && y == Yball ? Color.BLACK : Color.GRAY);
		        g.drawString(""+x+" "+y, x * (w - d) / Xmax + d2, (Ymax - y) * (h-d) / Ymax + d2);
			}
		}
	}

	@Override
	Set<Integer> domain(String key) {
		Set<Integer> res = new HashSet<Integer>();
		if ("Move".equals(key)) {
			res.add(-1);
			res.add(0);
			res.add(1);
		}else {
			res.add(0);
			res.add(1);
		}
		return res;
	}
}
