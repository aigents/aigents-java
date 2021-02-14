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
import java.util.Map;

class StateWorld {
	Map<State,Map<State,Number>> states = new HashMap<State,Map<State,Number>>();
	Map<String,int[]> ranges = new HashMap<String,int[]>(); 

//TODO
	int change(State previous, State current) {
		//first, estimate expectedness
		//Map<State,Number> expectations = states.get(previous);
		/*
		for (State s : state_actions.keySet()) {
			double d = State.distance(state, s, ranges);
		*/
		//111
		return 0;
	}
}


class StateActionSpaceExpectingPlayer extends StateActionSpaceMatchingPlayer {
	
	StateWorld world = new StateWorld();
	State previuos = null;
	
	StateActionSpaceExpectingPlayer(double fuzziness){
		super(fuzziness);
	}

	@Override
	int move(Game g,State state) {
		//evaluate the change in state world
		if (previuos != null) {
			//int expectedness = world.change(previuos,state);
		}
		previuos = state;
		return super.move(g, state);
	}
	
}
