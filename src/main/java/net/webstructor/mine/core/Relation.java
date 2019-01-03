/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.core;

public interface Relation extends CoreRelation {
    float	    getPosEvidence();	
    float	    getNegEvidence();	
    int 		getConfirmation();	
    float 		getConfirmedEvidence();
    void        assign(Relation r);
}
