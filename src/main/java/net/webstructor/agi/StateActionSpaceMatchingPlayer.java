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
import java.util.Map;
import java.util.Set;

class StateActionSpaceMatchingPlayer extends Player {//Makes decisions based on graph paths with global feedback
	Map<State,Integer> state_action = new HashMap<State,Integer>();//current context
	Map<State,Map<Integer,Number>> state_actions = new HashMap<State,Map<Integer,Number>>();//all transitions
	double fuzziness;
	
	StateActionSpaceMatchingPlayer(double fuzziness){
		this.fuzziness = fuzziness;
	}

	void update(Map<Integer,Number> actions, Integer action, Integer value) {
		Number old = actions.get(action);
		actions.put(action, old == null ? value : value + old.intValue());
	}

	Map<String,Integer> getRanges(Set<String> feelings){
		Map<String,Integer> ranges = new HashMap<String,Integer>();
		for (String feeling : feelings) {
			Integer range = ranges.get(feeling);
			if (range == null) {
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				for (State s : state_actions.keySet()) {
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
		return ranges;
	}
	
	//merge actions
	void merge(Map<Integer,Number> dest, Map<Integer,Number> src) {
		for (Integer a : src.keySet()) {
			Number n = dest.get(a);
			dest.put(a, n != null ? n.intValue() + a : a);
		}
	}
	
	Map<Integer,Number> getActions(State state) {
		Map<Integer,Number> actions = null;
		Map<String,Integer> ranges = getRanges(state.p.keySet());
		double distance = Double.MAX_VALUE;
		for (State s : state_actions.keySet()) {
			double d = State.distance(state, s, ranges);
			if (d < fuzziness && d <= distance) {
				if (distance > d) {
					distance = d;
					actions = state_actions.get(s);
				} else {//d == distance
					//bind ties in new container
					actions = new HashMap<Integer,Number>(actions);//clone present
					merge(actions,state_actions.get(s));
				}
 			}
		}
		return actions;
	}
	
	@Override
	int move(Game g,State state) {
		/*
		! contextual memory player
		- identify state memory as graph of state-to-state transisions with emotional feedback on success/failure!!!???
			- if feedback is positive/negative
				increment/decrement all <state,action> pairs from current context in the state registry (map<state,actions>) 
				clean current context
			- lookup state in the state registry without of action taken (map<state,actions>)
				- if found the state (or a state the most similar based on "fuzziness" threshold)
					- get all possible actions with their "utility" (can be below zero if prohibitive default 0)
						- get actions with topmost non-negative utility and select the random action 
							- if action is not found
								- select random action and give alert/assert!
				- if not found
					- create state in the registry (map<state,actions)
					- select random action
			- add <state,action> pairs to current context
		*/
		int sad = state.value("Sad",0);
		int happy = state.value("Happy",0);
		if (sad > 0 || happy > 0) {
			//- if feedback is positive/negative
			//		increment/decrement all <state,action> pairs from current context in the state registry (map<state,actions>) 
			//		clean current context
			for (State s : state_action.keySet()) {
				Integer action = state_action.get(s);
				Map<Integer,Number> actions = state_actions.get(s);
				if (actions == null)
					state_actions.put(s,actions = new HashMap<Integer,Number>());
				//if (sad > 0)
				//	update(actions, action, -sad);
				if (happy > 0)
					update(actions, action, happy);
			}
			state_action.clear();
		}
		//- lookup state in the state registry without of action taken (map<state,actions>)
		Map<Integer,Number> actions = state_actions.get(state);
		if (actions == null && fuzziness > 0) {
			actions = getActions(state);
			//if (actions != null)
			//	actions = actions;
		}
		Integer action = null;
		if (actions != null) {
			//- if found the state (or a state the most similar based on "fuzziness" threshold)
			//	- get all possible actions with their "utility" (can be below zero if prohibitive default 0)
			//		- get actions with topmost non-negative utility and select the random action 
			//			- if action is not found
			//				- select random action and give alert/assert!
			ArrayList<Integer> best_actions = new ArrayList<Integer>();
			int utility_max = Integer.MIN_VALUE;
			for (Integer a : actions.keySet()) {
				int utility = actions.get(a).intValue();
				if (utility >= 0) {
					if (utility_max <= utility) {
						if (utility_max < utility) {
							utility_max = utility;
							best_actions.clear();
						}
					}
					best_actions.add(a);
				}
			}
			if (best_actions.size() > 0)
				action = Game.random(best_actions.toArray(new Integer[] {}));//default
		}
		if (action == null) {
			//- if not found
			//	- create state in the registry (map<state,actions)
			//	- select random action
			action = Game.random(g.domain("Move").toArray(new Integer[] {}));//default
		}
		//- add <state,action> pairs to current context
		state_action.put(state, action);
		return action;
	}
	
}
