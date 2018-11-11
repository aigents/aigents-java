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

public interface Interactor
{
        Item[] getCats(int catSysId,int from,int to) throws Exception;//returns list of categories in the category system, sorted alphabetically
        int addCat(int catSysId,String catName) throws Exception;	//returns category Id for the newly created category for given category system with supplied name if created successfully, 0 otherwise
        int delCat(int catId) throws Exception;                      //returns 1 if category with supplied Id is deleted successfully, 0 otherwise
        int getDocCnt(int srcId) throws Exception;                   //returns number of  documents for given document source
        Item[] getDocs(int srcId) throws Exception;	            //returns all documents for given document source, sorted alphabetically
        Item[] getDocs(int srcId,int from, int to) throws Exception;//returns subset of documents for given document source, sorted alphabetically
        int addDoc(int srcId,String docName,boolean ignoreDups,boolean lazyTokens) throws Exception;       //returns document Id for the newly created document for given document source with supplied name if created successfully, 0 otherwise
        int delDoc(int docId) throws Exception;	                    //returns 1 if document with supplied Id is deleted successfully, 0 otherwise
        int updateDoc(int docId,boolean lazyTokens) throws Exception;//returns 1 if document with supplied Id is added and then added, retaining the same Id, successfully, 0 otherwise
        RelevantItem[] getDocFeatures(int docId,int confirm) throws Exception;//returns all document features for given document, sorted by relevance first and alphabetically by feature name then
        RelevantItem[] getCatDocFeatures(int catDocId,int confirm) throws Exception;//returns all document features for given document, sorted by relevance first and alphabetically by feature name then
        int addDocFeature(int docId, String token, int confirm) throws Exception;//returns Id of document feature relation for specified keyword with specified confirmation value if created successfully, 0 otherwise
        int setDocFeature(int relId, int confirm) throws Exception;	//returns 1 if document feature relation is confirmed successfully, 0 otherwise
        RelevantItem[] getCatFeatures(int catId, int confirm) throws Exception;//returns all category features for given category, sorted by relevance first and alphabetically by feature name then
        int addCatFeature(int catId, String token, int confirm) throws Exception;//	returns Id of category feature relation for specified keyword with specified confirmation value if created successfully, 0 otherwise
        int setCatFeature(int relId, int confirm) throws Exception;	//returns 1 if category feature relation is confirmed successfully, 0 otherwise
        RelevantItem[] getDocCats(int docId, int confirm, int domain) throws Exception;//returns all document categories for given document, sorted by relevance first and alphabetically by category name then
        RelevantItem[] addDocGetCats(int srcId,int domain,String docName) throws Exception;//for added or existing document, return all categories   
        RelevantItem[] getCatDocs(int catId, int confirm, int srcId) throws Exception;//returns all document categories for given document, sorted by relevance first and alphabetically by document name then
        int addDocCat(int docId, int catId, int confirm) throws Exception;//returns Id of document category relation for specified category Id with specified confirmation value if created successfully, 0 otherwise
        int setDocCat(int relId, int confirm) throws Exception;	    //returns 1 if document category relation is confirmed successfully, 0 otherwise
        String getDocData(int docId) throws IOException, Exception;	            //returns original binary representation of the document with specified Id
        String getCatDocData(int docCatId) throws IOException, Exception;         //returns original binary representation of the document in document-category relation with specified relation Id
        String[] getDocTokenNames(int docId) throws Exception;	        //returns list of document with specified Id tokens delimited by specified delimiter
        int updateCat(int docId, boolean purgeConfirmed) throws Exception;                   //
        Item[] getDomains() throws Exception;
        Item[] getSources() throws Exception;
        String getDocHighlight(int docId, int catId) throws Exception;//return a highlighting markup html
        String getCatDocHighlight(int catDocId) throws Exception;//return a highlighting markup html
}
