/*
 * MIT License
 * 
 * Copyright (c) 2005-2020 by Anton Kolonin, Aigents®
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
abstract class Game {
	abstract void init();
	abstract void next();
	abstract void printHeader();
	abstract void printState();
	public static int random(int[] states) {
	    Random random = new Random();
	    return states[random.nextInt(states.length)];
	}
	public int random(int min, int max) {
	    Random random = new Random();
	    return random.nextInt(max - min) + min;
	}
}

class State {
	HashMap<String,Integer> p = new HashMap<String,Integer>();
	State add(String key, Integer value) {
		p.put(key, value);
		return this;
	}
}

abstract class Player {
	ArrayList<State> states = new ArrayList<State>();
	abstract int move(State s);
}


class BreakoutStripped extends Game {
	Integer Yball, Xball, Xrocket, move, sad, happy;
	
	int Ymax, Xmax;
	int Ydir, Xdir;
	
	Player p;
	
	BreakoutStripped(int Ymax, int Xmax,Player p){
		this.Ymax = Ymax;
		this.Xmax = Xmax;
		this.p = p;
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
	void init() {
		Yball = 0;//start from the floor
		int[] states = new int[Xmax/2];
		for (int i = 0; i < states.length; i++)
			states[i] = 1 + i*2;
		Xball = random(states);//1,3,5,7,...
		Xrocket = Xball;
		Ydir = 1;//strike to ceiling
		Xdir = random(0,1) == 0 ? -1 : +1;
		move = 0;
	}
	
	@Override
	void next() {
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
		if (move < 0 && Xrocket > 1)
			Xrocket--;
		else 
		if (move > 0 && Xrocket < (Xmax-1))
			Xrocket++;
		if (Yball == 0) {//check if rocket is matching ball
			if (Xrocket == Xball) {
				happy = 1;
				sad = 0;
			} else {
				happy = 0;
				sad = 1;
				//TODO init(); //start the game over
			}
		} else {
			sad = happy = 0;
		}
		State s = new State();
		s.add("Yball", Yball).add("Xball", Xball).add("sad", sad).add("happy", happy).add("Xrocket", Xrocket);
		move = p.move(s);	
	}
}


class StalePlayer extends Player {//Stays in place, sometimes lucky
	@Override
	int move(State s) {
		return 0;
	}
}

class ReactivePlayer extends Player {//Follows the ball, lose always
	@Override
	int move(State s) {
		int Xball = s.p.get("Xball");
		int Xrocket = s.p.get("Xrocket");
		return Xball < Xrocket ? -1 : Xball > Xrocket ? + 1 : 0;
	}
}

class SimplePredictivePlayer extends Player {//Follows the ball, lose always
	State old = null;
	@Override
	int move(State s) {
		int move = 0;
		if (old != null) {
			int oldXball = old.p.get("Xball");
			int Xball = s.p.get("Xball");
			int Xdir = Xball - oldXball;
			move = Xdir < 0 ? -1 : Xdir > 0 ? + 1 : 0;
		}
		old = s;
		return move;
	}
}


/*
TODO:
1. Learning
2. Add energy consumption
3. Add restarts on failures
 */
public class AgiTester {

	void run(Game g, int times) {
		g.printHeader();
		g.init();
		for (int t = 0; t < times; t++) {
			g.next();
//TODO: decide
			g.printState();
		}
	}
	
	public static void main(String[] args) {
		AgiTester at = new AgiTester();
		Player p = new SimplePredictivePlayer();
		Game g = new BreakoutStripped(2,4,p); at.run(g,18);
		//Game g = new BreakoutStripped(4,6,p); at.run(g,100);
		//Game g = new BreakoutStripped(6,8,p); at.run(g,200);
	}
}
