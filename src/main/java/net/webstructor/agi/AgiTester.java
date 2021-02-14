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

import net.webstructor.main.Tester;

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
Results (20 runs, 1100/3600/7500/20000 epochs) - grades: 65-70-75-80-85-90:
Game/Player		2X4	4X6	6X8	8X10	2X4d4X6d6X8d8X10d (delayed)
Function/bas	89	88	88	92		70	73	72	85		82
Function/neg	92	90	90	93		67	73	81	85		84
Function/fuz0.1	92	92	91	93		68	79	79	81		84
Function/fuz0.5	93	93	93	93		80	83	81	89		88!
Function/sta	94	88	91	94		64	71	79	80		83
Function/sta0.1	94	91	87	92		64	69	77	82		82
Function/sta0.5	93	88	87	93		64	68	75	83		81
Function/sta1.0	92	88	87	92		62	68	75	83		81
Function/chg	91	86	89	92		64	73	76	79		82
Function/chg0.1	92	91	89	94		69	71	81	82		84!
Function/chg0.5	93	90	90	93		63	69	80	84		83
Function/chg1.0	93	90	90	93		66	69	79	84		83

Discrete/bas   	89	88	88	92		70	73	72	85		82
Discrete/neg   	92	90	90	93		67	73	81	85		84
Discrete/fuz0.5	93	91	88	92		70	76	80	83		84
Discrete/sta	94	88	91	94		64	71	79	80		83
Discrete/chg	91	86	89	92		64	73	76	79		81

Conclusions:
1) Both Functional and Discrete representations of the environment are handleable nearly to the same extent
2) Functional representation is slightly better from the accuracy perspective and much better from the run-time performance perspective
3) Both a) negating bad experiences and b) fuzzy matching help improving learning speed
4) Delayed reward complicates learning to extent of ~10-15%
5) Using space-action (sta) or change-action (chg) spaces with "global feedback" casuses enormous performance boost with 1% loss of learning speed
6) Negative "global feedback" makes results significantly worse, learning may get impossible in some cases
TODO:
! explore "predictiveness"
	...
! opengym!? https://arxiv.org/pdf/1312.5602.pdf
	- sparse visual space!?
		- don't encode blank pixels!?
		- thresholdize blank pixels
		- account only for pixels in attention area within "space of movement" attention area computed over frames!?
	- break "reinforcement contexts" on change of predictabiltity!!!!!!
		- state-state space
		- predicted vs expected
		- break on either feedback or decline of predictability
		- positively feedback well predicted models
		- self-reinforcment learning
		- для "самоподкрепления самопредсказуемостью" надо все равно уметь эффективно считать близость как гладкую функцию 
			- а для этого на пикcельном поле без "технического зрения"  никак. 
				Так что еще надо придумать как сделать так, чтобы техническое зрение само возникало, либо показать, что это невозможно.... 
	- fix problem with locking on stalled states with only the same single action returned
	- more compression on Python side?
	- play with "limited size of state-action history (queue) for global reinforcement" - with optional decay...
- algorithm improvements
	0. Effective algorithm for "fuzzy state matching" - NN is the only alternative?
	1. Универсальное представление абстрактных "объектно-функциональных" моделей в памяти системы
	2. Алгоритм перевода сырых данных в абстрактные "объектно-функциональные" модели ("unsupervised learning")
	3. Формализация определения критериев перехода из режима (2) "unsupervised learning" в режим (4) "reinforcement learning"
	4. Собственно  "reinforcement learning" на основе уже "объектно-функциональных" моделей
	5. Internal representaion of time - for determinatoion of "contextual periods" for "global feedback"?
- remember successful sequences as "compound states built of states"!?
- Unit test!
- Add forgetting
- Learning with compression
- Both increase happiness and decrease pain
- Add energy consumption
- Add restarts on failures
*/
	public static void work() {
		long sleep = 200;//400;
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
		Game.randomise();
		for (int i = 0; i < loops; i++) {
			//Player p = new LazyPlayer();//stays in place - rarely wins
			Player p = new ReactivePlayer();//follows the ball position - rarely wins
			//Player p = new SimplePredictivePlayer();//follows the ball move direction - always wins
			//Player p = new SimpleSequenceMatchingPlayer(true,false);//bas: memorizes the good moves - eventually learns
			//Player p = new SimpleSequenceMatchingPlayer(true,true);//neg: disregards the bad moves - few percents better!
			//Player p = new SimpleSequenceMatchingPlayer(true,0.5,true,false);//fuz: makes uncertain moves - few more percents better (93)!!! 
			//Player p = new SimpleSequenceMatchingPlayer(true,0.5,true,true);//der: adds derivatives to the state - works worse (90+ -> 80+)...

			//Player p = new StateActionSpaceMatchingPlayer(0);//sta
			//Player p = new StateActionSpaceMatchingPlayer(0.1);//sta0.1
			//Player p = new ChangeActionSpaceMatchingPlayer(0);//chg
			//Player p = new ChangeActionSpaceMatchingPlayer(0.1);//chg0.1

			//immediate reward
			//Game g = new SelfPong(h,w,true,true,false);//with rocket
			//Game g = new SelfPongDiscrete(h,w,true,true,false);//with rocket
			
			//delayed reward
			Game g = new SelfPong(h,w,true,true,true);//with rocket
			//Game g = new SelfPongDiscrete(h,w,true,true,true);//with rocket
			
			//blind play
			//Game g = new SelfPong(h,w,true,false);//w/o rocket - eventually learns, but much slower, with quality decay over restarts!? (93 -> 80+)...
			//Game g = new SelfPongDiscrete(h,w,true,false);//w/o rocket - eventually learns, but much slower, with quality decay over restarts!? (93 - >83-38)

			at.run(g,p,score,gp,debug,epochs,sleep);
			System.out.println(score);
		}
	}

	public String testScore(Player p, Game g) {
		Game.randomise();//set ramdomization to equal conditions for everyone - TODO fix hack!?
		long sleep = 200;//400;
		AgiTester at = new AgiTester();
		State score = new State();
		boolean debug = false;
		int loops = 20;
		int epochs = 1100;
		GamePad gp = null;
		for (int i = 0; i < loops; i++) {
			p.init();
			at.run(g,p,score,gp,debug,epochs,sleep);
		}
		return score.toString();
	}
	
	public static void test() {
		AgiTester at = new AgiTester();
		Tester t = new Tester();
		int h = 2, w = 4;
		Game g;
		
		g = new SelfPong(h,w,true,true,false);//immediate reward, with rocket
		t.assume(at.testScore(new LazyPlayer(),g),"Score:49	Happy:2740	Sad:2760");//stays in place - rarely wins
		t.assume(at.testScore(new ReactivePlayer(),g),"Score:0	Happy:0	Sad:5500");//follows the ball position - rarely wins
		t.assume(at.testScore(new SimplePredictivePlayer(),g),"Score:100	Happy:5500	Sad:0");//follows the ball move direction - always wins
		t.assume(at.testScore(new SimpleSequenceMatchingPlayer(true,false),g),"Score:89	Happy:4930	Sad:570");//bas: memorizes the good moves - eventually learns
		t.assume(at.testScore(new SimpleSequenceMatchingPlayer(true,true),g),"Score:92	Happy:5078	Sad:422");//neg: disregards the bad moves - few percents better!
		t.assume(at.testScore(new SimpleSequenceMatchingPlayer(true,0.5,true,false),g),"Score:93	Happy:5143	Sad:357");//fuz: makes uncertain moves - few more percents better (93)!!! 
		t.assume(at.testScore(new SimpleSequenceMatchingPlayer(true,0.5,true,true),g),"Score:91	Happy:5014	Sad:486");//der: adds derivatives to the state - works worse (90+ -> 80+)...
		t.assume(at.testScore(new StateActionSpaceMatchingPlayer(0), g),"Score:94	Happy:5216	Sad:284");//sta
		t.assume(at.testScore(new StateActionSpaceMatchingPlayer(0.1), g),"Score:94	Happy:5216	Sad:284");//sta0.1
		t.assume(at.testScore(new ChangeActionSpaceMatchingPlayer(0),g),"Score:91	Happy:5032	Sad:468");//chg
		t.assume(at.testScore(new ChangeActionSpaceMatchingPlayer(0.1),g),"Score:92	Happy:5111	Sad:389");//chg0.1
		
		g = new SelfPongDiscrete(h,w,true,true,false);//immediate reward, with rocket
		t.assume(at.testScore(new SimpleSequenceMatchingPlayer(true,0.5,true,false),g),"Score:93	Happy:5121	Sad:379");
		
		g = new SelfPong(h,w,true,true,true);//delayed reward, with rocket
		t.assume(at.testScore(new SimpleSequenceMatchingPlayer(true,0.5,true,false),g),"Score:80	Happy:4406	Sad:1078");
		
		g = new SelfPongDiscrete(h,w,true,true,true);//delayed reward, with rocket
		t.assume(at.testScore(new SimpleSequenceMatchingPlayer(true,0.5,true,false),g),"Score:70	Happy:3865	Sad:1620");
	
		t.check();
	}
	
	public static void main(String[] args) {
		if (args.length > 0 && args[0].equals("test"))
			test();
		else
			work();
	}
	
}
