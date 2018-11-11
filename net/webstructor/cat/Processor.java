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

import java.io.IOException;

public interface Processor
{
        Relation[] getCats(int catSysId) throws Exception;// get list of categories in the category system
        int addCat(int catSysId,String catName, boolean ignoreDups) throws Exception;// add category to the system
        int delCat(int catId) throws Exception;  	            // remove category with all its relations, recalculate feature metrics
        int getDocCnt(int srcId) throws Exception;	            // get list of documents in the doc source
        Relation[] getDocs(int srcId) throws Exception;	        // get list of documents Ids in the doc source
        Relation[] getDocs(int srcId,int from, int to) throws Exception; //	get list of documents Ids in the doc source
        int addDoc(int srcId,String docName,boolean ignoreDups, boolean lazyTokens) throws Exception;	// add document to the doc source
        int delDoc(int docId) throws Exception;	                // remove the document with all its relations, if it is related to features and categories, recalculate the feature metrics
        int updateDoc(int docId, boolean lazyTokens) throws Exception; // create and read document, translate it into ordered vector of tokens (words and symbols, with Reader.canReadDoc and Reader.readDocTokens), perform feature extraction (with Calculator.extractDocFeatures), given the current feature metrics, suggest the categories for document (with Calculator.suggestDocCats) 
        Relation[] getDocFeatures(int docId,int confirm) throws Exception;// list document features 
        int addDocFeature(int docId, String token, int confirm) throws Exception;//returns Id of document feature relation for specified keyword with specified confirmation value if created successfully, 0 otherwise
        int setDocFeature(int relId, int confirm) throws Exception;	//returns 1 if document feature relation is confirmed successfully, 0 otherwise
        Relation[] getCatFeatures(int catId, int confirm) throws Exception;//returns all category features for given category, sorted by relevance first and alphabetically by feature name then
        int addCatFeature(int catId, String token, int confirm) throws Exception;//	returns Id of category feature relation for specified keyword with specified confirmation value if created successfully, 0 otherwise
        int setCatFeature(int relId, int confirm) throws Exception;	//returns 1 if category feature relation is confirmed successfully, 0 otherwise
        Relation[] getDocCats(int docId, int confirm, int domain) throws Exception;//returns all document categories for given document, sorted by relevance first and alphabetically by category name then
        Relation[] getCatDocs(int catId, int confirm, int srcId) throws Exception;//returns all document categories for given document, sorted by relevance first and alphabetically by document name then
        int addDocCat(int docId, int catId, int confirm) throws Exception;//returns Id of document category relation for specified category Id with specified confirmation value if created successfully, 0 otherwise
        int setDocCat(int relId, int confirm) throws Exception;	    //returns 1 if document category relation is confirmed successfully, 0 otherwise
        String getDocData(int docId) throws IOException, Exception;	            //returns original binary representation of the document with specified Id
        String[] getDocTokenNames(int docId) throws Exception;   // list all parsed document tokens
        int updateCat(int catId,boolean purgeConfirmed) throws Exception;                   //

        String getName(int typeId, int relId) throws Exception;              // returns the name of the relation
        double getRelevance(int typeId, int relId) throws Exception;         // calculates the relevance value for the relation
        Relation[] getFeatureTokens(int featureId) throws Exception;
        String getCatDocData(int docCatId) throws IOException, Exception;         //returns original binary representation of the document in document-category relation with specified relation Id
        Relation getRel(int typeId,int pkId) throws Exception;	// get a relation if exists
        Relation[] getRels(int typeId) throws Exception;
        String getDocHighlight(int docId, int catId) throws Exception;//return a highlighting markup html
        String getCatDocHighlight(int catDocId) throws Exception;//return a highlighting markup html
}