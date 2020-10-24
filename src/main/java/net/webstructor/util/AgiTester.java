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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

class State {
	HashMap<String,Integer> p = new HashMap<String,Integer>();
	State add(String key, Integer value) {
		p.put(key, value);
		return this;
	}
	void add(State other) {
		for (String key : other.p.keySet()) {
			Integer value = other.p.get(key); 
			Integer thisValue = p.get(key);
			p.put(key,thisValue != null ? thisValue + value : value);
		}
	}
	void add(State other, String[] keys) {
		for (String key : keys) {
			Integer value = other.p.get(key);
			if (value == null)
				continue;
			Integer thisValue = p.get(key);
			p.put(key,thisValue != null ? thisValue + value : value);
		}
	}
	boolean sameAs(State other,Set<String> feelings) {
		for (String key : feelings) {
			Integer value = other.p.get(key); 
			Integer thisValue = p.get(key);
			if (thisValue == null || !thisValue.equals(value))
				return false;
		}
		return true;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String key : p.keySet()) {
			if (sb.length() > 0)
				sb.append('\t');
			sb.append(key).append(':').append(p.get(key));
		}
		return sb.toString();
	}
	static Integer mostUsable(List<State> states, String key) {
		int max = 0;
		HashMap<Integer,Integer> options = new HashMap<Integer,Integer>();
		for (State s : states) {
			Integer value = s.p.get(key);
			Integer count = options.get(value);
			count = count == null ? 1 : count + 1;
			options.put(value, count);
			if (max < count)
				max = count;
		}
		for (Integer option : options.keySet()) {
			Integer count = options.get(option);
			if (count == max)
				return option;
		}
		return null;
	}
}

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
    //static Random random = new Random();
	static Random random = ThreadLocalRandom.current();
	abstract void init();
	abstract State next();
	abstract void printHeader();
	abstract void printState();
	abstract void printFooter();
	abstract String toString(State s);
	public static int random(int[] states) {
	    Random random = new Random();
	    return states[random.nextInt(states.length)];
	}
	public static int random(int min, int max) {
		int arg = max - min + 1;
	    int rand = random.nextInt(arg) + min;
	    return rand;
	}
}

abstract class Player {
	ArrayList<State> states = new ArrayList<State>();
	abstract int move(Game g,State s);
}


class BreakoutStripped extends Game {
	Integer Yball, Xball, Xrocket, move, sad, happy;
	
	int Ymax, Xmax;
	int Ydir, Xdir;
	
	Player p;
	
	private boolean random;

	private int totalHappy = 0, totalSad = 0;
	
	BreakoutStripped(int Ymax, int Xmax, Player p, boolean random){
		this.Ymax = Ymax;
		this.Xmax = Xmax;
		this.p = p;
		this.random = random;
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
	}
	
	@Override
	State next() {
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
				totalHappy++;
				happy = 1;
				sad = 0;
			} else {
				totalSad++;
				happy = 0;
				sad = 1;
				//TODO init(); //start the game over
			}
		} else {
			sad = happy = 0;
		}
		State s = new State();
		s.add("Yball", Yball).add("Xball", Xball).add("Sad", sad).add("Happy", happy).add("Xrocket", Xrocket);
		move = p.move(this,s);
		return s;
	}
}


class StalePlayer extends Player {//Stays in place, sometimes lucky
	@Override
	int move(Game g,State s) {
		return 0;
	}
}

class ReactivePlayer extends Player {//Follows the ball, lose always
	@Override
	int move(Game g,State s) {
		int Xball = s.p.get("Xball");
		int Xrocket = s.p.get("Xrocket");
		return Xball < Xrocket ? -1 : Xball > Xrocket ? + 1 : 0;
	}
}

class SimplePredictivePlayer extends Player {//Follows the ball, lose always
	State old = null;
	@Override
	int move(Game g,State s) {
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

//TODO: seei if it works as long as direction of move is not accounted 
class BruteforceUniquePlayer extends Player {//Makes decisions based on past experiences
	ArrayList<State> history = new ArrayList<State>();
	@Override
	int move(Game g,State state) {
		int move = Game.random(-1,+1);//default
		Set<String> feelings = state.p.keySet(); 
		ArrayList<State> possibilities = new ArrayList<State>();
		//find identical states in history
		int histories = history.size();
		if (histories > 1 && state.p.get("Sad") <= 0) {
			State pstate = history.get(histories - 1);
			for (int i = histories - 1; i > 0; i--) {
				State ppast = history.get(i-1);
				State past = history.get(i);
				//evaluate the outcome of this state
				if (past.sameAs(state,feelings) && ppast.sameAs(pstate,feelings)) {
					for (int j = i + 1; j < history.size(); j++) {
						State o = history.get(j);
						if (o.p.get("Sad") > 0)
							break;
						//if state is good, retain in history of good states
						if (o.p.get("Happy") > 0) {
							possibilities.add(past);
//System.out.println("------------------------------------------"+g.toString(past));
							break;
						}
					}
				}
			}
			//check all good states and select the decision made in most of them
			//return the decision
			if (possibilities.size() == 1)
				move = possibilities.get(0).p.get("Move");
			else if (possibilities.size() > 1) {
				Integer mostUsable = State.mostUsable(possibilities,"Move");
				if (mostUsable != null)
					move = mostUsable; 
			}
		}
		state.add("Move", move);
		history.add(state);
		return move;
	}
	
}


/*
DONE:
1. Learning without compression
2. Make random
TODO:
3. Add approximating
4. Unit test!
5. Add forgetting
6. Both increase happiness and decrease pain
7. Learning with compression
8. Add energy consumption
9. Add restarts on failures
10. Do computation of derivative?
*/
public class AgiTester {

	void run(Game g, State score, boolean debug, int times) {
		if (debug)
			g.printHeader();
		g.init();
		for (int t = 0; t < times; t++) {
			State s = g.next();
			score.add(s,new String[] {"Happy","Sad"});
			if (debug)
				g.printState();
		}
		if (debug)
			g.printFooter();
	}
	
	public static void main(String[] args) {
		AgiTester at = new AgiTester();
		State score = new State();
		boolean debug = false;
		//Player p = new SimplePredictivePlayer();
		int loops = 100;
		int h = 2, w = 4, epochs = 400;
		//int h = 4, w = 6, epochs = 1400;
		//int h = 6, w = 8, epochs = 5000;
		for (int i = 0; i < loops; i++) {
			Player p = new BruteforceUniquePlayer();
			Game g = new BreakoutStripped(h,w,p,true);
			at.run(g,score,debug,epochs);
		}
		System.out.println(score);
	}
}
