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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class StateWorld {
	Map<State,Map<Integer,Number>> state_actions;
	Map<State,Map<State,Number>> state_states = new HashMap<State,Map<State,Number>>();
	Map<String,int[]> ranges = new HashMap<String,int[]>(); 

	double maxdistance = 0;

	StateWorld(Map<State,Map<Integer,Number>> state_actions){
		this.state_actions = state_actions;
	}
	
	void update(State previous, State current) {
		Map<State,Number> next_states = state_states.get(previous);
		if (next_states == null)
			state_states.put(previous,next_states = new HashMap<State,Number>());
		Number counted = next_states.get(current);
		next_states.put(current, counted == null ? 1 : 1 + counted.intValue());
	}

//TODO calibrate the space of states to denominate by most distant states instead of tanh 
	
	//novelty == distance from the most similar (0 => close so no novelty, 1 => far away so high novelty)
	double novelty(State state) {
		double min = Double.MAX_VALUE;
		for (State s : state_actions.keySet()) {
			double d = State.distance(state, s, ranges);
			if (min > d)
				min = d;
		}
		return Math.tanh(min);//minimum novelty in range 0..1
	}
	
	//proximity to expectations == expectedness != surpringness (0 - far away so unexpected, 1 - identical so expected  
	double expectedness(Set<State> expectations, State state) {
		double min = Double.MAX_VALUE;
		if (expectations != null) {
			for (State s : expectations) {
				double d = State.distance(state, s, ranges);
				if (min > d)
					min = d;
			}
		}
		return 1 - Math.tanh(min);
	}
	
	int controllability(State state) {
		return 0;
	}
	
	int utility(State state) {
		return 0;
	}

	//get top states shortlist (ties)
	Set<State> top(Map<State,Number> states){
		HashSet<State> top = new HashSet<State>();
		int max = 0;
		for (State s : states.keySet()) {
			int i = states.get(s).intValue();
			if (max <= i) {
				if (max < i) {
					top.clear();
					max = i;
				} 
				top.add(s);
			}
		}
 		return states.keySet();
	}
	
	//TODO expected probability value!?
	Map<State,Number> getStates(State state){
		Map<State,Number> states = null;
		double distance = Double.MAX_VALUE;
		for (State s : state_states.keySet()) {
			double d = State.distance(state, s, ranges);
			if (d <= distance) {
				if (distance > d) {
					distance = d;
					states = state_states.get(s);
				} else {//d == distance
					//bind ties in new container
					states = new HashMap<State,Number>(states);//clone present
					states.putAll(state_states.get(s));
				}
 			}
		}
		return states;
	}
	
	//TODO expected probability value!?
	Set<State> expectStates(State state){
		Map<State,Number> states = state_states.get(state);
		if (states == null)
			states = getStates(state);
		if (states == null)
			return null;
		if (states.size() == 1)
			return states.keySet();
		return top(states); 
	}
	
	void updateRanges(State state) {
		State.updateRanges(ranges,state);
	}
	
}

/*
Let's assume an Agent needs to generate feedback for itself for "self-supervised" experiential/reinforcement learning, 
given some environment in form of vector space of sequential flow of contexts built by its state variables 
(including some rewards/reinforcements), its actions in another vector space of actions, and past experiences 
maintained in some form so predictive models could be built and decisions could be taken.
What would be the policies of an Agent for generating rewards to itself in between conventional 
positive/negative feedback from the environment?
What would be the metrics to evaluate any new experienced context for self-reinforcement purposes?
	Novelty (in general) - to which extent the context is new to our current experience
	Predictability (in the current context) - to which extent the context is predictable given our current experience
	Controllability - to which extent the context is correlated to our previous actions (action result acceptor)
	Expected Utility - to which extent the context appears promising positive reinforcement or avoidance of negative one within the framework of existing experience
 */
class StateActionSpaceExpectingPlayer extends StateActionSpaceMatchingPlayer {
	
	StateWorld world;
	State previuos;
	Set<State> expected;
	State self;
	
	StateActionSpaceExpectingPlayer(double fuzziness){
		super(fuzziness);
	}

	@Override
	void init() {
		super.init();
		world = new StateWorld(this.state_actions);
		previuos = null;
		expected = null;
		self = new State();
	}
	
	@Override
	int move(Game g,State state) {
		world.updateRanges(state);
		
		int novelty = (int)Math.round(100 * world.novelty(state));
		int expectedness = (int)Math.round(100 * world.expectedness(world.expectStates(previuos), state));
		self.set("Novelty", novelty);
		self.set("Expectedness", expectedness);
		
//		System.out.format("%d\t%d\n",novelty,expectedness);
		
		//evaluate the change in state world
		int action = super.move(g, state);//does -> state_action.put(state, action);
		
		if (previuos != null)
			world.update(previuos, state);
		
		previuos = state;
		return action;
	}

	@Override
	State selfState() {//self-state
		return self;
	}
}
