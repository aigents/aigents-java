/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.inf;

import net.webstructor.mine.core.BasicCoreRelation;
import net.webstructor.mine.core.CoreRelation;
import net.webstructor.mine.store.Schema;

public class StoreSchema implements Schema {

	public static final int Relation = 1;
	public static final int Entity = 2;
	public static final int Relationship = 3;
	public static final int FeatureType = 4; 
	public static final int Feature = 5;
	public static final int DocumentSource = 6;
	public static final int Document = 7;
	public static final int Domain = 8;
	public static final int Category = 9;
	public static final int Object = 10;
	public static final int DocumentCategory = 11;
	public static final int DocumentFeature = 12;
	public static final int CategoryFeature = 13;
	public static final int CategoryDomain = 14;
	public static final int ObjectCategory = 15;
	  
	static final CoreRelation[] m_schema = {
		  new BasicCoreRelation(0,1,"Relation"),
		  new BasicCoreRelation(1,2,"Entity"),
		  new BasicCoreRelation(1,3,"Relationship"),
		  new BasicCoreRelation(2,4,"FeatureType"),
		  new BasicCoreRelation(2,5,"Feature"),
		  new BasicCoreRelation(2,6,"DocSource"),
		  new BasicCoreRelation(2,7,"Document"),
		  new BasicCoreRelation(2,8,"Domain"),
		  new BasicCoreRelation(2,9,"Category"),
		  new BasicCoreRelation(2,10,"Object"),
		  new BasicCoreRelation(3,11,new int[]{7,9},"DocumentCategory"),
		  new BasicCoreRelation(3,12,new int[]{7,5},"DocumentFeature"),
		  new BasicCoreRelation(3,13,new int[]{9,5},"CategoryFeature"),
		  new BasicCoreRelation(3,14,new int[]{9,8},"CategoryDomain"),//Property
		  new BasicCoreRelation(3,15,new int[]{10,9},"ObjectCategory"),//Value
		  new BasicCoreRelation(0,100,"Reserved")
	};

	public CoreRelation[] getRelations(){
		return m_schema;
	}
	
}
