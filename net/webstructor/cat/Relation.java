/*
 * MIT License
 * 
 * Copyright (c) 2005-2018 by Anton Kolonin, Aigents
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
package net.webstructor.cat;

public interface Relation
{
        boolean     equals(Relation rel);//checks if relations are the same
        int 		getType(); 			// get type id of a relation
	    int		    getId();			// get unique id of a relation
	    String 		getName(); 			// get symbolic representation of a relation
	    int		    getArity();         // get arity - count of the parent,child,etc. 
	    int[]		getIds();			// get unique Ids of the parent,child,etc. 
	    double	    getPosEvidence();	// get count of parent-child co-occurences
	    double	    getNegEvidence();	// get count of denied parent-child co-occur.
	    int 		getConfirmation();	// outer-made occurence (validity) as [-1,0,+1]
	
//move to lower interface
        void        assign(Relation r);// copy the statistical attributes of the relation
        //IRelation[]	getRelations(int typeId); // get list of parent or child relations 
        //string 		getData(); 			// get string representation of a relation
}