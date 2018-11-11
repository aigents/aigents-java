/*
 * Copyright 2005-2008, Anton Kolonin,
 * All rights reserved.
 *
 * This software is proprietary information belonging to Anton Kolonin. 
 * You shall not disclose this and shall not use it in any way 
 * without of written permission granted by Anton Kolonin.
 */
package net.webstructor.mine.core;

public interface Id 
{
    // Nullary relations (arity = 0) 	
    public static final int TYPE = 1; 
    public static final int TEXTTYPE = 2; 
    public static final int DOMAIN = 3;
    public static final int FEATURETYPE = 4;
    public static final int TOKENTYPE = 16; //20070604
    // Unary relations (arity = 1, Parent)
    public static final int TOKEN = 5; 
    public static final int TEXT  = 6; 
    public static final int CATEGORY = 7; 
    public static final int FEATURE = 8; 
    // Binary relations (arity = 2) 	
	public static final int TEXTCATEGORY = 9;
    public static final int TEXTTOKEN = 10;
    public static final int TEXTFEATURE = 11;
    public static final int FEATURETOKEN = 12;
    public static final int CATEGORYFEATURE = 13;
    public static final int CATEGORYDOMAIN = 14;
    public static final int CATEGORYCATEGORY = 15;//20070413
    // N-ary relations (arity = N, so any arity restricted by max)
    public static final int PATTERN = 17;//reserverd //20070604
    public static final int RESERVED = 1000;//reserved so user-defined Id's started with 1001 //20070604

    // Not really types but useful Ids
    public static final int KEYWORD = 401; // - Id of the FEATURETYPE for all Keyword features
    public static final int KEYWORDFRAME = 402; // - Id of the FEATURETYPE for all Keyword Frame features
    public static final int NOTCONFIRMED = -2147483648;
    
    // Not really types but useful constants
    public static final int MAXPATTERNLENGTH = 8;//reserved
}
