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

/*
DONE:
1. Learning without compression
2. Make random
3. Add approximating
TODO:
4. Unit test!
5. Add forgetting
6. Learning with compression
7. Both increase happiness and decrease pain
8. Add energy consumption
9. Add restarts on failures
10. Do computation of derivative?
*/
public class AgiTester {

	void repaint(GamePad gp, long sleep) {
		gp.repaint();
		if (sleep > 0)
			try {Thread.sleep(sleep);} catch (InterruptedException e) {e.printStackTrace();}
	}
	
	void run(Game g, Player p, State score, GamePad gp, boolean debug, int times, long sleep) {
		if (gp != null)
			gp.setGame(g);
		if (debug)
			g.printHeader();
		Integer action = null;
		g.init();
 		for (int t = 0; t < times; t++) {
 			if (gp != null)
 				repaint(gp,sleep);
			State s = g.next(action);
			action = p.move(g,s);
			score.add(s,new String[] {"Happy","Sad"});
			int sad = score.value("Sad");
			int happy = score.value("Happy");
			if (sad > 0 || happy > 0)
				score.set("Score",new Integer(100*happy/(sad + happy)));
 			if (debug)
				g.printState();
		}
		if (debug)
			g.printFooter();
	}
	
/*
Results (20 runs, 1100/3600/7500 epochs):
Game/Player		2X4	4X6	6X8	8X10	2X4d4X6d6X8d8X10d
Function/bas	89	88	88	92		70	73	72	85
Discrete/bas   	89	88	88	...		
Function/neg	92	90	90	93		67	73	81	85
Discrete/neg   	92	90	90	...
Function/fuz0.5	93	93	93	93		80	83	81	89
Discrete/fuz0.5 93	91	88	...

Conclustions:
1) Both Functional and Discrete representations of the environment are handleable nearly to the same extent
2) Functional representation is slightly better from the accuracy perspective amd much better from the run-time performance perspective
3) Both a) negating bad experiences and b) fuzzy matching help improving learning speed
4) Delayed reward complicates learning to extent of ~10%  
*/	
	public static void main(String[] args) {
		long sleep = 100;//400;
		AgiTester at = new AgiTester();
		State score = new State();
		boolean debug = false;
		int loops = 20;
		int h = 2, w = 4, epochs = 1100;//basic 89  (fuzzy 93 with t=0.5, loops = 20)
		//int h = 4, w = 6, epochs = 3600;//basic 88 (fuzzy 93 with t=0.5, loops = 20) - best for video demo
		//int h = 6, w = 8, epochs = 7500;//basic 88 (fuzzy 93 with t=0.5, loops = 20)
		//int h = 8, w = 10, epochs = 20000;//basic 92 (fuzzy 93 with t=0.5, loops = 20)
		//GamePad gp = new GamePad(w*100,h*100);
		GamePad gp = null;
		for (int i = 0; i < loops; i++) {
			//Player p = new LazyPlayer();//stays in place - rarely wins
			//Player p = new ReactivePlayer();//follows the ball position - rarely wins
			//Player p = new SimplePredictivePlayer();//follows the ball move direction - always wins
			//Player p = new SimpleSequenceMatchingPlayer(true,false);//bas: memorizes the good moves - eventually learns
			//Player p = new SimpleSequenceMatchingPlayer(true,true);//neg: disregards the bad moves - few percents better!
			Player p = new SimpleSequenceMatchingPlayer(true,0.5,true,false);//fuz: makes uncertain moves - few more percents better (93)!!! 
			//Player p = new SimpleSequenceMatchingPlayer(true,0.5,true,true);//der: adds derivatives to the state - works worse (90+ -> 80+)...

			//immediate reward
			//Game g = new SelfPong(h,w,true,true,false);//with rocket
			//Game g = new SelfPongDiscrete(h,w,true,true);//with rocket
			
			//blind play
			//Game g = new SelfPong(h,w,true,false);//w/o rocket - eventually learns, but much slower, with quality decay over restarts!? (93 -> 80+)...
			//Game g = new SelfPongDiscrete(h,w,true,false);//w/o rocket - eventually learns, but much slower, with quality decay over restarts!? (93 - >83-38)

			//delayed reward
			Game g = new SelfPong(h,w,true,true,true);//with rocket
			//Game g = new SelfPongDiscrete(h,w,true,true);//with rocket
			
			
			at.run(g,p,score,gp,debug,epochs,sleep);
			System.out.println(score);
		}
	}
}
