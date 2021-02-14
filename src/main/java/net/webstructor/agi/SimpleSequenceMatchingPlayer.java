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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class SimpleSequenceMatchingPlayer extends Player {//Makes decisions based on past experiences
	ArrayList<State> history;
	boolean exhaustive;
	double fuzziness;
	boolean prohibitive;
	boolean derivatives;
	
	SimpleSequenceMatchingPlayer(boolean exhaustive,boolean prohibitive){
		this(exhaustive,0,prohibitive,false);
	}

	SimpleSequenceMatchingPlayer(boolean exhaustive,double fuzziness,boolean prohibitive,boolean derivatives){
		this.exhaustive = exhaustive;
		this.fuzziness = fuzziness;
		this.prohibitive = prohibitive;
		this.derivatives = derivatives;
		init();
	}

	@Override
	void init() {
		history = new ArrayList<State>();
	}
	
	Integer perfectMove(Game g,State state,Set<State> prohibilities) {
		Integer move = null;
		Set<String> feelings = state.p.keySet(); 
		ArrayList<State> possibilities = new ArrayList<State>();
		//find identical states in history
		int histories = history.size();
		if (histories > 1 && state.value("Sad",0) <= 0) {
			State pstate = history.get(histories - 1);
			for (int i = histories - 1; i > 0; i--) {
				State ppast = history.get(i-1);
				State past = history.get(i);
				//evaluate the outcome of this state
				if (past.sameAs(state,feelings) && ppast.sameAs(pstate,feelings)) {
					for (int j = i + 1; j < history.size(); j++) {
						State o = history.get(j);
						//if state is bad, retain it history of bad states
						if (o.value("Sad",0) > 0) {
							prohibilities.add(past);
							break;
						}
						//if state is good, retain in history of good states
						if (o.value("Happy",0) > 0) {
							possibilities.add(past);
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

	//TODO: get rid of this!?
	Map<String,Integer> ranges(Game g, Set<String> feelings){
		Map<String,Integer> map = new HashMap<String,Integer>();
		for (String s : feelings) {
			Set<Integer> d = g.domain(s);
			int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
			for (Integer i : d) {
				if (min > i)
					min = i;
				if (max < i)
					max = i;
			}
			if (min < max)
				map.put(s, max - min);
		}
		return map;
	}
	
	//get ranges for missed feelings
	void getRanges(Set<String> feelings, Map<String,Integer> ranges){
		for (String feeling : feelings) {
			Integer range = ranges.get(feeling);
			if (range == null) {
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				for (State s : history) {
					Integer v = s.value(feeling);
					if (v !=  null) {
						if (min > v)
							min = v;
						if (max < v)
							max = v;
					}
				}
				if (min < max)
					ranges.put(feeling, max - min);
			}
		}
	}
	
	private Integer uncertainMove(Game g,State state,Set<Integer> moves) {
		Integer move = null;
		Set<String> feelings = state.p.keySet(); 
		feelings = new HashSet<String>(feelings);
		Map<String,Integer> ranges = ranges(g,feelings);
		feelings.remove("Sad");
		feelings.remove("Happy");
		if (derivatives)
			getRanges(feelings,ranges);
		//find similar successful states
		double distance = Double.MAX_VALUE;
		int histories = history.size();
		int min_history = derivatives ? 1 : 0;//for derivatives
		if (histories > 1 && state.value("Sad",0) <= 0) {
			State[] seq = new State[] {history.get(histories - 1), state};
			for (int i = histories - 1; i > min_history; i--) {
				State past = history.get(i);
				if (past.p.get("Happy") > 0) {//end of Happy sequence
					//lookup back for similar case till "Bad" is found
					for (i--; i > min_history; i--) {
						State p = history.get(i);
						//memorize the most similar step
						State[] pseq = new State[] {history.get(i - 1), p};
						int m = p.value("Move");
						if (moves.contains(m)) {
							double d = State.distance(seq, pseq, feelings, ranges);
							if (distance > d) {
								distance = d;
								move = m;
							}
						}
//TODO: move this up!?
						if (past.value("Sad",0) > 0)
							break;
					}
				}
				
			}
			//if there is a similar state and action is not prohibited 
		}
		if (distance < fuzziness)//0.5 is optimal
			return move;
		return null;
	}
	
	String debug(State ppast, State past) {
		return ppast.p.get("Xball")+" "+ppast.p.get("Yball")+" "+ppast.p.get("Xrocket")+" -> "+past.p.get("Xball")+" "+past.p.get("Yball")+" "+past.p.get("Xrocket")+" => "+past.p.get("Move");
	}
	
	void addDerivatives(State state) {
		if (history.size() == 0)
			return;
		State prev = history.get(history.size() - 1);
		Set<String> feelings = state.p.keySet();
		HashMap<String,Integer> derivatives = new HashMap<String,Integer>();
		for (String f : feelings) {
			int d = state.value(f) - prev.value(f);
			derivatives.put("d"+f, d);
		}
		state.p.putAll(derivatives);
	}
	
	@Override
	int move(Game g,State state) {
		if (derivatives)
			addDerivatives(state);
		HashSet<State> prohibilities = new HashSet<State>();
		Integer move = perfectMove(g,state,prohibilities);
		if (move == null) {
//TODO: formalize the space of possible actions to take!!!
			Set<Integer> moves = g.domain("Move");  
			if (prohibitive)//avoid repeating really bad experiences 
				moves.removeAll(State.values(prohibilities, "Move").keySet());
			if (move == null && fuzziness > 0)
				move = uncertainMove(g,state,moves);
			if (move == null) {
	 			if (moves.size() > 0)
					move = Game.random(moves.toArray(new Integer[] {}));
				else
					move = Game.random(-1,+1);//default
			}
		}
		state.add("Move", move.intValue());
		history.add(state);
		return move;
	}

}
