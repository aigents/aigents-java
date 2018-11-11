/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.core;

public interface CoreRelation
{
    int 		getType(); 			
    int		    getId();			
    int		    getId(int idx);			
    String 		getName(); 			
    int		    getArity();          
    int[]		getIds();			 
    boolean     equals(CoreRelation r);
}
