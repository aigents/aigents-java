/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.store;

import net.webstructor.mine.core.CoreRelation;

public interface Storage {
	    void startUp(String path,Schema schema) throws StoreException;
	    void shutDown() throws StoreException;
	    void commit() throws StoreException;         
	    boolean isPresent(int pkId) throws StoreException;	    
	    int getId(int typeId,String name,boolean ignoreCase) throws StoreException;
	    String getName(int pkId) throws StoreException;   
	    CoreRelation getRel(int pkId) throws StoreException;
	    CoreRelation getRel(int typeId,String name,boolean ignoreCase) throws StoreException;
	    CoreRelation getRel(int typeId, int[] fkIds) throws StoreException;
	    int addRel(CoreRelation rel) throws StoreException;             
	    int setRel(CoreRelation rel) throws StoreException;             
	    CoreRelation[] getRels(int typeId) throws StoreException;	
	    String getName(int objId,int propId) throws StoreException;
	    /*
	    Relation getRel(int typeId,int fkId,int dim,String name,boolean ignoreCase) throws StoreException;
	    int getRelCnt(int typeId,int fkId,int dim) throws StoreException;
	    Relation[] getRels(int typeId,int fkId,int dim) throws StoreException;
	    Relation[] getRels(int typeId,int fkId,int fkDim,int targetDim,int[] filter) throws StoreException;
	    int delRels(int typeId,int fkId,int dim) throws StoreException;
	    //Relation[] getRels(int typeId,int fkId,int dim,int from,int to) throws StoreException;
	    int addRel(Relation rel) throws StoreException;             
	    int setRel(Relation rel) throws StoreException;             
	    int delRel(int typeId,int pkId) throws StoreException;      
	    int[] getTypes(int typeId) throws StoreException;           
	    int setRels(int typeId,int fkId,int dim,float posEvidence) throws StoreException;
	    int incPosEvidence(int typeId,int pkId,float delta) throws StoreException;
	    */
}
