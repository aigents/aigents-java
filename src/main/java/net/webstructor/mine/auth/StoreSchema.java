/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.auth;

import net.webstructor.mine.core.CoreRelation;
import net.webstructor.mine.core.BasicCoreRelation;
import net.webstructor.mine.store.Schema;

public class StoreSchema implements Schema {

	public static final int Relation = 1;
	public static final int Entity = 2;
	public static final int Relationship = 3;
	public static final int User = 4;
	public static final int Group = 5;
	public static final int Asset = 6;
	public static final int UserGroup = 7;
	public static final int GroupAsset = 8;
	public static final int GroupRelationAccessLevel = 9;
	public static final int Description = 10;
	public static final int Fullname = 11;
	public static final int Password = 12;
	public static final int Email = 13;
	public static final int Path = 14;
	public static final int UserFullname = 15;
	public static final int UserPassword = 16;
	public static final int UserEmail = 17;
	public static final int GroupDescription = 18;
	public static final int AssetDescription = 19;
	public static final int AssetPath = 20;
	  
	static final CoreRelation[] m_schema = {
		  new BasicCoreRelation(0,1,"Relation"),
		  new BasicCoreRelation(1,2,"Entity"),
		  new BasicCoreRelation(1,3,"Relationship"),
		  new BasicCoreRelation(2,4,"User"),
		  new BasicCoreRelation(2,5,"Group"),
		  new BasicCoreRelation(2,6,"Asset"),
		  new BasicCoreRelation(3,7,new int[]{4,5},"UserGroup"),
		  new BasicCoreRelation(3,8,new int[]{5,6},"GroupAsset"),
		  new BasicCoreRelation(3,9,new int[]{5,1},"GroupRelationAccessLevel"),
		  new BasicCoreRelation(2,10,"Description"),
		  new BasicCoreRelation(2,11,"Fullname"),
		  new BasicCoreRelation(2,12,"Password"),
		  new BasicCoreRelation(2,13,"Email"),
		  new BasicCoreRelation(2,14,"Path"),
		  new BasicCoreRelation(3,15,new int[]{4,11},"UserFullname"),
		  new BasicCoreRelation(3,16,new int[]{4,12},"UserPassword"),
		  new BasicCoreRelation(3,17,new int[]{4,13},"UserEmail"),
		  new BasicCoreRelation(3,18,new int[]{5,10},"GroupDescription"),
		  new BasicCoreRelation(3,19,new int[]{5,10},"AssetDescription"),
		  new BasicCoreRelation(3,20,new int[]{6,14},"AssetPath"),
		  new BasicCoreRelation(0,100,"Reserved")
	};

	public CoreRelation[] getRelations(){
		return m_schema;
	}
	
}
