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

public class Types
{
        // Nullary relations (arity = 0) 	
        public static final int TYPE = 1; // - DocSource 
        public static final int DOCSOURCE = 2; // - DocSource 
        public static final int DOMAIN = 3; // - Domain/CatSystem
        public static final int FEATURETYPE = 4; // - FeatureType
        public static final int TOKEN = 5; // - Token - Word, Symbol or RegularExpression)
        // LANGUAGE
        // Unary relations (arity = 1, Parent)
        public static final int DOCUMENT = 6; // - Document (DocSource)
        public static final int CATEGORY = 7; // - Category (CatSystem)
        public static final int FEATURE = 8; // - Feature(FeatureType)
        // TOKEN
        // Binary relations (arity = 2) 	
        public static final int DOCCAT = 9; // - DocCat(Document,Category)
        public static final int DOCTOKEN = 10; // - DocToken(Document,Token)  transient, not storeable
        public static final int DOCFEATURE = 11; // - DocFeature(Document,Feature)
        public static final int FEATURETOKEN = 12; // - FeatureToken (Feature,Token)
        public static final int CATFEATURE = 13; // - CatFeature(Category,Feature)
        public static final int CATDOMAIN = 14; // - CatDomain(Category,Domain)/Property
        // N-ary relations (arity = N, so any arity restricted by max)
        public static final int PATTERN = 15; // Token Patterns

        // Not really types but useful Ids
        public static final int KEYWORD = 1000; // - Id of the FEATURETYPE for all Keyword features
        public static final int NOTCONFIRMED = -2147483648;
        
        // Not really types but useful finalants
        public static final int MAXPATTERNLENGTH = 8;
}
