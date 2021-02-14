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

class Change {
	State prev;
	State last;
	Change(State prev, State last){
		this.prev = prev;
		this.last = last;
	}
	@Override
	public int hashCode() {
		return prev.hashCode() + last.hashCode();
	}
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Change))
			return false;
		Change o = (Change)other;
		return prev.equals(o.prev) && last.equals(o.last);
	}
}

class ChangeActionSpaceMatchingPlayer extends Player {//Makes decisions based on graph paths with global feedback
	Map<Change,Integer> state_action;//current context
	Map<Change,Map<Integer,Number>> state_actions;//all transitions
	State prev_state = null;  
	double fuzziness;
	
	ChangeActionSpaceMatchingPlayer(double fuzziness){
		this.fuzziness = fuzziness;
		init();
	}

	@Override
	void init() {
		state_action = new HashMap<Change,Integer>();//current context
		state_actions = new HashMap<Change,Map<Integer,Number>>();//all transitions
		prev_state = null;
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
				for (Change s : state_actions.keySet()) {
					Integer v = s.last.value(feeling);
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
	
	Map<Integer,Number> getActions(Change change) {
		Map<Integer,Number> actions = null;
		Set<String> feelings = change.last.p.keySet();
		Map<String,Integer> ranges = getRanges(feelings);
		double distance = Double.MAX_VALUE;
		for (Change s : state_actions.keySet()) {
			double d = State.distance(new State[] {change.prev,change.last}, new State[] {s.prev,s.last}, feelings, ranges);
			if (d < fuzziness && d <= distance) {
				if (distance > d) {
					distance = d;
					actions = state_actions.get(s);
				} else {//d == distance
					//bind ties in new container
					actions = new HashMap<Integer,Number>(actions);//clone present
					StateActionSpaceMatchingPlayer.merge(actions,state_actions.get(s));
				}
 			}
		}
		return actions;
	}
	
	@Override
	int move(Game g,State last_state) {
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
		int sad = last_state.value("Sad",0);
		int happy = last_state.value("Happy",0);
		if (sad > 0 || happy > 0) {
			//- if feedback is positive/negative
			//		increment/decrement all <state,action> pairs from current context in the state registry (map<state,actions>) 
			//		clean current context
			for (Change s : state_action.keySet()) {
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
		Change change = prev_state == null ? null : new Change(prev_state,last_state);
		prev_state = last_state;
		Map<Integer,Number> actions = change == null ? null : state_actions.get(change);
		if (actions == null && fuzziness > 0 && change != null) {
			actions = getActions(change);
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
		if (change != null)
			state_action.put(change, action);
		return action;
	}

}
