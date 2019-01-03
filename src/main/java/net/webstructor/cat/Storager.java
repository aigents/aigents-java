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

public interface Storager
{
        void startUp(String connectionPath) throws Exception;	// connect to a server (using connection info) or load information from the file (using the path)
        void shutDown() throws Exception;	                    // disconnect from server or flush information into the file
        int commit() throws Exception;                           // commit the current image (e.g. flush memory image into disk file)
        boolean getId(int typeId,int pkId) throws Exception;	    // check if relation exists
        int getIdByName(int typeId,String name,boolean ignoreCase) throws Exception;	    // check if relation exists
        Relation getRel(int typeId,int pkId) throws Exception;	// get a relation if exists
        Relation getRel(int typeId, int[] fkIds) throws Exception;// get a relation if exists
        String getName(int typeId,int pkId) throws Exception;   // get symbolic representation of a relation
        String getData(int typeId,int pkId);	// get binary representation of a relation
        int getRelCnt(int typeId,int fkId,int dim) throws Exception;	// count relation's relations for given type
        Relation[] getRels(int typeId) throws Exception;	// retrieve all relations
        Relation[] getRels(int typeId,int fkId,int dim) throws Exception;	// retrieve relations restricted by dimension
        int delRels(int typeId,int fkId,int dim,boolean purgeConfirmed) throws Exception;	// delete relations restricted by dimension
        Relation[] getRels(int typeId,int fkId,int dim,int from,int to) throws Exception;	// retrieve relations's restricted by dimension, using the inclusive range
        int addRel(Relation rel) throws Exception;             // add Relation
        int setRel(Relation rel) throws Exception;             // set Relation identified by type and Ids
        int delRel(int typeId,int pkId) throws Exception;             // delete Relation identified by type and Ids
        int[] getTypes(int typeId) throws Exception;            // returns types of the relation arguments
        int setRels(int typeId,int fkId,int dim,double posEvidence) throws Exception;
}

