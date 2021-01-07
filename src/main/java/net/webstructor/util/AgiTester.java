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
package net.webstructor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JFrame;

class State {
	HashMap<String,Integer> p = new HashMap<String,Integer>();
	int value(String key) {
		return p.get(key);
	}
	State set(String key, Integer value) {
		p.put(key, value);
		return this;
	}
	State add(String key, Integer value) {
		Integer oldValue = p.get(key);
		if (oldValue != null)
			value += oldValue;
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
	int value(String key, int def) {
		Integer i = p.get(key);
		return i == null ? def : i;
	}
	public static Map<Integer,Integer> values(Set<State> states, String key) {
		Map<Integer,Integer> map = new HashMap<Integer,Integer>();
		for (State s : states) {
			Integer value = s.value(key);
			if (value != null) {
				Integer count = map.get(value);
				if (count == null)
					count = 0;
				map.put(value, new Integer(value + count));
			}
		}
		return map;
	}
	public static double distance(State[] a, State[] b, Set<String> keys) {
//TODO: normalize to scale factor passed as arguments
		double sum2 = 0, count = 0; 
		for (int i = 0; i < a.length; i++) {
			for (String key : keys){
				int avalue = a[i].value(key);
				int bvalue = b[i].value(key);
				double v = bvalue - avalue;
				sum2 += v*v;
				count += 1;
			}
		}
		return count ==  0 ? Double.MAX_VALUE : Math.sqrt(sum2/count);
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
    //static Random random = new Random(0);
    static Random random = ThreadLocalRandom.current();
	abstract void init();
	abstract State next();
	abstract void printHeader();
	abstract void printState();
	abstract void printFooter();
	abstract void render(Graphics g);
	abstract String getTitle();
	abstract String toString(State s);
	abstract Set<Integer> domain(String key);
	public static int random(int[] states) {
	    return states[random.nextInt(states.length)];
	}
	public static int random(Integer[] states) {
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


class SelfPong extends Game {
	Integer Yball, Xball, Xrocket, move, sad, happy;
	
	int Ymax, Xmax;
	int Ydir, Xdir;
	
	Player p;
	
	private boolean random;

	private int totalHappy = 0, totalSad = 0;
	
	SelfPong(int Ymax, int Xmax, Player p, boolean random){
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
    	g.setColor(Color.ORANGE);
        g.fillOval(Xball * (w - d) / Xmax, (Ymax - Yball) * (h-d) / Ymax, d, d);
    	g.setColor(Color.GRAY);
        g.fillRect(Xrocket * (w - d) / Xmax, h, d, rocket_h);
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
//TODO: other variables
		return res;
	}
}


class LazyPlayer extends Player {//Stays in place, sometimes lucky
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

class SimplePredictivePlayer extends Player {//Follows the ball direction, always winds
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

class BruteforceUniquePlayer extends Player {//Makes decisions based on past experiences
	ArrayList<State> history = new ArrayList<State>();
	boolean exhaustive;
	boolean certain;
	boolean prohibitive;
	
	BruteforceUniquePlayer(boolean exhaustive,boolean certain,boolean prohibitive){
		this.exhaustive = exhaustive;
		this.certain = certain;
		this.prohibitive = prohibitive;
	}

	Integer perfectMove(Game g,State state,Set<State> prohibilities) {
		Integer move = null;
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
						if (o.p.get("Sad") > 0) {
							prohibilities.add(past);
							break;
						}
						//if state is good, retain in history of good states
						if (o.p.get("Happy") > 0) {
							possibilities.add(past);
//System.out.println("------------------------------------------"+i+" "+g.toString(past));hack=i;
							break;
						}
					}
				}
				if (possibilities.size() > 0 && !exhaustive)
					break;
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
		return move;
	}
//static int hack = -1;
	private Integer uncertainMove(Game g,State state,Set<State> prohibilities) {
		Integer move = null;
		Set<String> feelings = state.p.keySet(); 
		
		feelings = new HashSet<String>(feelings);
		feelings.remove("Sad");
		feelings.remove("Happy");
		
		//find identical states in history
		int histories = history.size();
		if (histories > 1 && state.p.get("Sad") <= 0) {
			State pstate = history.get(histories - 1);
			ArrayList<Object[]> candidates = new ArrayList<Object[]>();
			for (int i = histories - 1; i > 0; i--) {
//				if (i==hack)
//					i=hack;
				State ppast = history.get(i-1);
				State past = history.get(i);
				if (prohibilities.contains(past))
					continue;
				double s = State.distance(new State[] {ppast,past}, new State[] {pstate,state}, feelings);
				//rank states by similarity 
				candidates.add( new Object[] {new Integer(i),new Double(s)} );
			}
			//find the least distant (most similar) and successive one
			Collections.sort(candidates,new ArrayPositionComparator(1));
			for (int j = candidates.size() - 1; j >= 0; j--) {
				int c = (Integer)candidates.get(j)[0];
				//State ppast = history.get(c-1);
				State past = history.get(c);
				//lookup for the first one which leads to success 
				for (int k = c + 1; k < history.size(); k++) {
					State o = history.get(k);
					if (o.p.get("Sad") > 0)
						break;
					//if state is good, return the move
					if (o.p.get("Happy") > 0) {
/*System.out.println("------------------------------------------ "+g.toString(pstate));
System.out.println("------------------------------------------ "+g.toString(state));
System.out.println("------------------------------------------ "+g.toString(ppast));
System.out.println("------------------------------------------ "+g.toString(past));*/
						return past.p.get("Move");
					}
				}
			}
		}
		return move;
	}
	
	@Override
	int move(Game g,State state) {
		HashSet<State> prohibilities = new HashSet<State>();
		Integer move = perfectMove(g,state,prohibilities);
		if (move == null && certain == false)
			move = uncertainMove(g,state,prohibilities);
		if (move == null) {
//TODO: formalize the space of possible actions to take!!!
			Set<Integer> moves = g.domain("Move");  
			if (prohibitive)
				moves.removeAll(State.values(prohibilities, "Move").keySet());
 			if (moves.size() > 0)
				move = Game.random(moves.toArray(new Integer[] {}));
			else
				move = Game.random(-1,+1);//default
		}
		state.add("Move", move.intValue());
		history.add(state);
		return move;
	}
	
}


@SuppressWarnings("serial")
class GamePad extends Canvas {
	private Game game = null;
	private JFrame frame;
	
	GamePad(int w, int h) {
        frame = new JFrame("GamePad");
        setSize(w, h);
        frame.add(this);
        frame.pack();
        frame.setVisible(true);
    }

	void setGame(Game game) {
		this.game = game;
		frame.setTitle(game.getTitle());
	}
	
    public void paint(Graphics g) {
    	if (game != null)
    		game.render(g);
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

	void run(Game g, State score, GamePad gp, boolean debug, int times, long sleep) {
		if (gp != null)
			gp.setGame(g);
		if (debug)
			g.printHeader();
		g.init();
 		for (int t = 0; t < times; t++) {
			State s = g.next();
			score.add(s,new String[] {"Happy","Sad"});
			int sad = score.value("Sad");
			int happy = score.value("Happy");
			if (sad > 0 || happy > 0)
				score.set("Score",new Integer(100*happy/(sad + happy)));
 			if (debug)
				g.printState();
			if (gp != null) {
				gp.repaint();
				if (sleep > 0)
					try {Thread.sleep(sleep);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
		if (debug)
			g.printFooter();
	}
	
	public static void main(String[] args) {
		long sleep = 400;
		AgiTester at = new AgiTester();
		State score = new State();
		boolean debug = false;
		int loops = 10;//50
		//int h = 2, w = 4, epochs = 400;//~88-89
		int h = 4, w = 6, epochs = 2000;//~88-89 good for video demo
		//int h = 6, w = 8, epochs = 5000;//~88-88
		GamePad gp = new GamePad(w*100,h*100);
		//GamePad gp = null;
		for (int i = 0; i < loops; i++) {
			//Player p = new LazyPlayer();//stays in place - rarely wins
			//Player p = new ReactivePlayer();//follows the ball position - rarely wins
			//Player p = new SimplePredictivePlayer();/follows the ball move direction - always wins
			//Player p = new BruteforceUniquePlayer(true,true,false);//memorizes the good moves - eventually learns
			Player p = new BruteforceUniquePlayer(true,true,true);//disregards the bad moves - few percents better!!!
			//Player p = new BruteforceUniquePlayer(true,false,true);//TODO uncertain decisions?
			Game g = new SelfPong(h,w,p,true);
			at.run(g,score,gp,debug,epochs,sleep);
			System.out.println(score);
		}
	}
}
