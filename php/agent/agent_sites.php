<?php
/*
 * MIT License
 * 
 * Copyright (c) 2014-2020 by Anton Kolonin, Aigents®
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

include_once("test_api.php");

function test_sites() {
	global $version;
	global $copyright;
	global $base_things_count;

	//initialize
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com, name john, surname doe, secret question x, secret answer y.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	
/* I am a mother. My son is weather dependent, suffering headaches due to pressure changes and solar flares. I need a way to get notifications on such events, as soon as they are posted on meteorological web sites...
http://www.tesis.lebedev.ru/sun_flares.html
	Сегодня на Солнце не было подтвержденных вспышек класса С и выше
	Сегодня на Солнце произошло 2 вспышки класса С и выше 
	Сегодня на Солнце произошла 1 вспышка класса С и выше
	Вспышка балла C2.1	2077	05:27:00	05:36:00
	Вспышка балла M1.3	2077	07:58:00	08:09:00
	
	sun flare patterns 'Вспышка балла $class $starttime $maxtime $endtime'.
	starttime, maxtime, enditime is time.
	*/
	test_o("My topics sun flare.");
	test_i("Ok.");
	test_o("Sun flare patterns 'Вспышка балла \$class \$starttime \$maxtime \$endtime'.");
	test_i("Ok.");
	test_o("What sun flare patterns?");
	test_i("Sun flare patterns 'Вспышка балла \$class \$starttime \$maxtime \$endtime'.");
	test_o("You reading 'sun flare' in 'Сегодня на Солнце произошла 1 вспышка класса С и выше Вспышка балла C2.1	2077	05:27:00	05:36:00'!");
	test_i("My reading sun flare in 'Сегодня на Солнце произошла 1 вспышка класса С и выше Вспышка балла C2.1	2077	05:27:00	05:36:00'.");
	test_o("What is sun flare class, endtime, maxtime, times, starttime?");
	test_i("There class c2.1, endtime 05:36:00, maxtime 05:27:00, starttime 2077, times today.");
	say("my format json.");
	get("Ok.");
	say("what my format?");
	get("[{\"format\":\"json\"}]");
	say("What is sun flare class, endtime, maxtime, times, starttime?");
	get("[{\"class\":\"c2.1\",\"endtime\":\"05:36:00\",\"maxtime\":\"05:27:00\",\"starttime\":\"2077\",\"times\":\"today\"}]");
	say("my format not json");
	get("Ok.");
	say("what my format?");
	get("No.");
	test_o("No there is sun flare.");
	test_i("Ok.");
	test_o("My topics no sun flare.");
	test_i("Ok.");
	test_o("No name sun flare.");
	test_i("Ok.");
	test_o("No name 'Вспышка балла \$class \$starttime \$maxtime \$endtime'.");
	test_i("Ok.");
	
/* I am a small business owner. I need a way to get quick updates on any low-cost material sale offers appearing on local bulletin boards...
http://www.hallolondon.co.uk/free_ads/for_sale/
	computing & phones
	http://www.hallolondon.co.uk/free_ads/for_sale/computing_and_phones_!48.html
		APPLE IPHONE 5S LTE 64GB UNLOCKED - in Hammersmith £350
		NEW IPHONE 5S 16GB IN IN COUNTY DURHAM - in County Durham £200

	iphone sale patterns 'iphone $description - in $place \$price'.
	description, place is text.
	*/
	test_o("My topics iphone sale.");
	test_i("Ok.");
	test_o("Iphone sale has price, patterns 'iphone \$description - in \$place \$price'.");
	test_i("Ok.");
	test_o("Price is money.");
	test_i("Ok.");
	test_o("You reading 'iphone sale' in 'APPLE IPHONE 5S LTE 64GB UNLOCKED - in Hammersmith £350 blah NEW IPHONE 5S 16GB IN IN COUNTY DURHAM - in County Durham £200'!"); 
	test_i("My reading iphone sale in 'APPLE IPHONE 5S LTE 64GB UNLOCKED - in Hammersmith £350 blah NEW IPHONE 5S 16GB IN IN COUNTY DURHAM - in County Durham £200'.");
	test_o("What is iphone sale description, place, price?");
	test_i("There description 5s 16gb in in county durham, place county durham, price £200; description 5s lte 64gb unlocked, place hammersmith, price £350.");
	test_o("No there is iphone sale.");
	test_i("Ok.");
	test_o("My topics no iphone sale.");
	test_i("Ok.");
	test_o("No name iphone sale.");
	test_i("Ok.");
	test_o("No name price.");
	test_i("Ok.");
	//test_o("No name money.");
	//test_i("Ok.");
	test_o("No name 'iphone \$description - in \$place \$price'.");
	test_i("Ok.");
			
/* I am a lawyer, engaged in specific legal domain. I need a tool to get information on public hearings for cases, falling under the same set of laws as my area — announced on the internet newsletters across the county...
http://www.broward.org/Legislative/
	http://www.broward.org/Legislative/Pages/PublicHearingSchedule.aspx
		Blah. Health and Human Services Public Hearing Wednesday, November 13, 2013, 4 – 7 p.m. Blah.
		Blah. Education and Cultural Affairs Public Hearing Tuesday, November 19, 2013, 4 – 7 p.m. Blah.

	public hearing patterns '$description public hearing $dayandtime'.
	description, $dayandtime is text. 
	*/
	test_o("My topics public hearing.");
	test_i("Ok.");
	test_o("Public hearing patterns '\$description public hearing \$dayandtime .'.");
	test_i("Ok.");
	test_o("You reading public hearing in 'Blah. Health and Human Services Public Hearing Wednesday, November 13, 2013, 4 – 7 p.m. Blah. Blah. Education and Cultural Affairs Public Hearing Tuesday, November 19, 2013, 4 – 7 p.m. Blah.'!");
	test_i("My reading public hearing in 'Blah. Health and Human Services Public Hearing Wednesday, November 13, 2013, 4 – 7 p.m. Blah. Blah. Education and Cultural Affairs Public Hearing Tuesday, November 19, 2013, 4 – 7 p.m. Blah.'.");	
	test_o("What is public hearing description, dayandtime?");
	test_i("There dayandtime 'tuesday , november 19 , 2013 , 4 – 7 p.m', description education and cultural affairs; dayandtime 'wednesday , november 13 , 2013 , 4 – 7 p.m', description health and human services.");	
	test_o("No there is public hearing.");
	test_i("Ok.");
	test_o("Public hearing patterns no '\$description public hearing \$dayandtime .', '\$description public hearing \$detail, \$dayandtime .', 'public hearing on \$description - \$dayandtime .'.");
	test_i("Ok.");
	test_o("You reading public hearing in 'CANCELLED Friday, December 6, 2013, 3 – 6 p.m. Broward County Governmental Center, Commission Chambers 115 South Andrews Ave., Room 422, Ft. Lauderdale, FL 33301 . Transportation, Economic Development, Environment & Growth Management Public Hearing and Local Bill Public Hearing Tuesday, December 17, 2013, 4 – 7 p.m. Tamarac City Hall, Commission Chambers 7525 NW 88th Ave., Tamarac, FL 33321 WMA File: Audio 1 (17MB)   . Public Hearing on Gaming - CANCELLED Monday, February 24, 2014, 2:30 p.m – 5:30 p.m. Broward County Governmental Center, Commission Chambers 115 South Andrews Ave., Room 422, Ft.'.");
	test_i("My reading public hearing in 'CANCELLED Friday, December 6, 2013, 3 – 6 p.m. Broward County Governmental Center, Commission Chambers 115 South Andrews Ave., Room 422, Ft. Lauderdale, FL 33301 . Transportation, Economic Development, Environment & Growth Management Public Hearing and Local Bill Public Hearing Tuesday, December 17, 2013, 4 – 7 p.m. Tamarac City Hall, Commission Chambers 7525 NW 88th Ave., Tamarac, FL 33321 WMA File: Audio 1 (17MB)   . Public Hearing on Gaming - CANCELLED Monday, February 24, 2014, 2:30 p.m – 5:30 p.m. Broward County Governmental Center, Commission Chambers 115 South Andrews Ave., Room 422, Ft.'.");
	test_o("What is public hearing description, detail, dayandtime?");
//TODO: Decide if and how to display complete and incomplete attributions
	get("There dayandtime 'cancelled monday , february 24 , 2014 , 2:30 p.m – 5:30 p.m', description gaming; dayandtime 'december 17 , 2013 , 4 – 7 p.m', description environment & growth management, detail local bill public hearing tuesday.");	
	test_o("What is public hearing text?");
	get("There text 'environment & growth management public hearing local bill public hearing tuesday , december 17 , 2013 , 4 – 7 p.m .'; text 'public hearing on gaming - cancelled monday , february 24 , 2014 , 2:30 p.m – 5:30 p.m .'.");
	test_o("No there is public hearing.");
	test_i("Ok.");
	test_o("My topics no public hearing.");
	test_i("Ok.");
	test_o("No name public hearing.");
	test_i("Ok.");
	test_o("No name '\$description public hearing \$detail, \$dayandtime .'.");
	test_i("Ok.");
	test_o("No name '\$description public hearing \$dayandtime .'.");
	test_i("Ok.");
	say("No name 'public hearing on \$description - \$dayandtime .'.");
	get("Ok.");
	
/* I am a real estate broker. I need to get timeous updates on particular estate offerings appearing on the online listings for the city where I work...
http://www.squarefoot.com.hk/
	Kowloon Gross Area (sq.ft.): 853 Saleable Area(sq.ft.) 685 Price: $11 M

	kowloon area real estate patterns 'Kowloon Gross Area $grossarea saleable area $saleablearea {price: rent:} $price $priceunit'.
	$grossarea $saleablearea is number.
	*/
	test_o("My topics real estate.");
	test_i("Ok.");
	//test_o("Real estate patterns 'Kowloon Gross Area \$grossarea saleable area \$saleablearea {price: rent:} \$price \$priceunit'.");
	test_o("Real estate patterns 'Kowloon Gross Area (sq.ft.) \$grossarea saleable area (sq.ft.) \$saleablearea {price: rent:} \$price \$priceunit'.");
	test_i("Ok.");
	test_o("Real estate has grossarea, saleablearea.");
	test_i("Ok.");
	test_o("What real estate patterns?");
	//test_i("Real estate patterns 'Kowloon Gross Area \$grossarea saleable area \$saleablearea {price: rent:} \$price \$priceunit'.");
	test_i("Real estate patterns 'Kowloon Gross Area (sq.ft.) \$grossarea saleable area (sq.ft.) \$saleablearea {price: rent:} \$price \$priceunit'.");
	test_o("Grossarea is number.");
	test_i("Ok.");
	test_o("Saleablearea is number.");
	test_i("Ok.");
	test_o("You reading 'real estate' in 'Kowloon Gross Area (sq.ft.): 853 Saleable Area(sq.ft.) 685 Price: $11 M'!");
	test_i("My reading real estate in 'Kowloon Gross Area (sq.ft.): 853 Saleable Area(sq.ft.) 685 Price: $11 M'.");
	test_o("What is real estate?");
//TODO: unsplit splited	
	//test_i("There grossarea 853, is real estate, price $11, priceunit m, saleablearea 685, text kowloon gross area 853 saleable area 685 price: $11 m, times today.");
	test_i("There grossarea 853, is real estate, price $11, priceunit m, saleablearea 685, text 'kowloon gross area ( sq.ft . ) 853 saleable area ( sq.ft . ) 685 price: $11 m', times today, title kowloon gross area.");
	test_o("No there is real estate.");
	test_i("Ok.");
	test_o("My topics no real estate.");
	test_i("Ok.");
	test_o("No name real estate.");
	test_i("Ok.");
	test_o("No name grossarea, name saleablearea.");
	test_i("Ok.");
	//test_o("No name 'Kowloon Gross Area \$grossarea saleable area \$saleablearea {price: rent:} \$price \$priceunit'.");
	test_o("No name 'Kowloon Gross Area (sq.ft.) \$grossarea saleable area (sq.ft.) \$saleablearea {price: rent:} \$price \$priceunit'.");
	test_i("Ok.");
	
/* I am a high-technology business owner. I need to stay informed on any news and releases made by my competitors, posted on their web sites and by relevant news aggregators...
http://www.wired.com/
	Google Needs Your Help Building Apps for Its All-Seeing Tablet
	Explore Every 2014 World Cup Stadium With Google Street View
	*/
	test_o("My topics 'google news'.");
	test_i("Ok.");
	test_o("Google news patterns '\$offer with Google \$product', 'Google \$offer'.");
	test_i("Ok.");
	test_o("You read google news in 'Google Needs Your Help Building Apps for Its All-Seeing Tablet. Explore Every 2014 World Cup Stadium With Google Street View.'!");
	test_i("My reading google news in 'Google Needs Your Help Building Apps for Its All-Seeing Tablet. Explore Every 2014 World Cup Stadium With Google Street View.'.");
//TODO: Parser - fix to use google news not 'google news' still having instances of 'Google \$news' patterns	
//TODO: Siter - read only one best pattern at a time (e.g. Wired-Google test case)	
	test_o("What is 'google news' offer, times, product?");
//TODO: Decide if and how to display complete and incomplete attributions
	//test_i("There offer explore every 2014 world cup stadium, product street view, times today.");	
	test_i("There offer explore every 2014 world cup stadium, product street view, times today; offer needs your help building apps for its all-seeing tablet, times today; offer street view, times today.");	
	test_o("What is 'google news' offer, times?");
	test_i("There offer explore every 2014 world cup stadium, times today; offer needs your help building apps for its all-seeing tablet, times today; offer street view, times today.");	
	test_o("What is 'google news', times today offer?");
	test_i("There offer explore every 2014 world cup stadium; offer needs your help building apps for its all-seeing tablet; offer street view.");
	test_o("No there is 'google news'.");
	test_i("Ok.");
	test_o("My topics no 'google news'.");
	test_i("Ok.");
	test_o("No name 'google news'.");
	test_i("Ok.");
	test_o("No name '\$offer with Google \$product', name 'Google \$offer'.");
	test_i("Ok.");
	
/* I am a journalist. I need to get urgent notifications on any news regarding my local area published in local, regional and federal news sources...
http://www.heraldsun.com.au/
	sports logos
	[Other Sports] [Pearson pulls out of Diamond League]
	[Sport] [ Croatia chased Spira but he stayed true ] 
	*/
	test_o("My topics sports.");
	test_i("Ok.");
	test_o("Sports patterns '{sport sports} \$topic'.");
	test_i("Ok.");	
	test_o("You reading sports in 'Other Sports Pearson pulls out of Diamond League . International Sport Croatia chased Spira but he stayed true'!");
	test_i("My reading sports in 'Other Sports Pearson pulls out of Diamond League . International Sport Croatia chased Spira but he stayed true'.");
	test_o("What is sports topic?");
	test_i("There topic croatia chased spira but he stayed true; topic pearson pulls out of diamond league.");
	test_o("No there is sports.");
	test_i("Ok.");
	test_o("My topics no sports.");
	test_i("Ok.");
	test_o("No name sports.");
	test_i("Ok.");
	test_o("No name '{sport sports} \$topic'.");
	test_i("Ok.");
			
/* I am a politician. I need up-to-date information on happenings related to the public figures leading the opposing parties and social groups...
http://indianexpress.com/elections/
	[$]
	Narendra Modi quits Vadodara, retains Varanasi.
	Prime Minister Narendra Modi had quit Vadodara seat despite winning with a near record margin of 5.7 lakh votes.
	Will deal with outstanding issues: Narendra Modi tells Chinese PM.
	RULE: [Narendra Modi $does $what] $does is verb
	*/	
	//TODO
	test_o("My topics Narendra Modi.");
	test_i("Ok.");
	test_o("Narendra Modi patterns 'Narendra Modi {[{quit, quits, leave, leaves} \$leave], [{deal, deals, did, does, do, has, had} \$action], [{retain, retains, save, saves} \$keep], [{say, says, tell, tells, told, said} \$talk]}'.");
	test_i("Ok.");
//TODO: needs verb definition	
	test_o("You read Narendra Modi in 'Prime Minister Narendra Modi had quit Vadodara seat despite winning with a near record margin of 5.7 lakh votes.'!");
	test_i("My reading narendra modi in 'Prime Minister Narendra Modi had quit Vadodara seat despite winning with a near record margin of 5.7 lakh votes.'.");
	test_o("You read Narendra Modi in 'Narendra Modi quits Vadodara, retains Varanasi.'!");
	test_i("My reading narendra modi in 'Narendra Modi quits Vadodara, retains Varanasi.'.");	
	test_o("You read Narendra Modi in 'Will deal with outstanding issues: Narendra Modi tells Chinese PM.'!");
	test_i("My reading narendra modi in 'Will deal with outstanding issues: Narendra Modi tells Chinese PM.'.");
//TODO: breakdown of sentences
	//test_o("You read Narendra Modi in 'Narendra Modi quits Vadodara, retains Varanasi. Will deal with outstanding issues: Narendra Modi tells Chinese PM.'!");
	//test_i("My reading narendra modi in 'Narendra Modi quits Vadodara, retains Varanasi. Will deal with outstanding issues: Narendra Modi tells Chinese PM.'.");	
	test_o("What is Narendra Modi?");
	test_i("There action quit vadodara seat despite winning with a near record margin of 5.7 lakh votes, is narendra modi, leave vadodara seat despite winning with a near record margin of 5.7 lakh votes, text narendra modi quit vadodara seat despite winning with a near record margin of 5.7 lakh votes had quit vadodara seat despite winning with a near record margin of 5.7 lakh votes, times today, title narendra modi quit vadodara seat despite winning with a near record margin of 5.7 lakh votes had quit vadodara seat despite winning with a near record margin of 5.7 lakh votes; is narendra modi, keep varanasi, leave vadodara, text narendra modi quits vadodara retains varanasi, times today, title narendra modi quits vadodara retains varanasi; is narendra modi, talk chinese pm, text narendra modi tells chinese pm, times today, title narendra modi tells chinese pm.");
//TODO: eliminate empty things in output	
	//test_o("What is Narendra Modi talk?");
	//test_i("There talk chinese pm.");	
	test_o("No there is Narendra Modi.");
	test_i("Ok.");
	test_o("My topics no Narendra Modi.");
	test_i("Ok.");
	test_o("No name Narendra Modi.");
	test_i("Ok.");
	test_o("No name 'Narendra Modi {quits, deal, retains, had, has, tells, did} \$what'.");
	test_i("Ok.");
	test_o("No name 'Narendra Modi {[{quits, deal, retains, had, has, tells, did} \$what'.");
	test_i("Ok.");
	test_o("No name 'Narendra Modi {[{quit, quits, leave, leaves} \$leave], [{deal, deals, did, does, do, has, had} \$action], [{retain, retains, save, saves} \$keep], [{say, says, tell, tells, told, said} \$talk]}'.");
	test_i("Ok.");

	//sanity check
	say("You forget!");
	get("Ok.");
	say("What your things count?");
	get("My things count ".($base_things_count).".");
	//finalize
	say("Your trusts no john.");
	get("Ok.");
	say("No email john@doe.com.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("My logout.");	
	get("Ok.");
}


function test_extractor() {
	global $version;
	global $copyright;
	global $base_things_count;
	
	//initialize
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com, name john, surname doe, secret question x, secret answer y.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	
	//TODO: deep pattern matching for relationship extraction
	//$text = "president putin has met donald trump";//OK
	//$text = "blah blah. president putin has met donald trump. blah blah";//FAILS
	//$text = "blah. president putin has met donald trump. blah blah";//FAILS
	//$patt = "putin";//OK
	//$patt = "{trump putin jinping}";//OK
	//$patt = "{trump putin jinping} {trump putin jinping}";//OK
	//$patt = "{trump putin jinping} \$someverb {trump putin jinping}";//OK
	//$patt = "{trump putin jinping} {invited met saw} {trump putin jinping}";//OK
	//$patt = "\$subject {invited met saw} \$object";//OK
	//$patt = "{trump putin} \$verb \$object";//OK
	//$patt = "\$subject \$verb \$object";//FAILS
	//$patt = "\$word \$verb \$object";//FAILS on "blah blah.
	//$patt = "\$word \$verb";//FAILS on "blah blah.
	//$patt = "\$x \$y \$z";//FAILS
	
	$text = "president putin has met donald trump. blah blah";
	$patt = "{trump putin junping} {encountered met joined} {trump putin junping}";
	say_thing($patt);
	say("You read '".$patt."' in '".$text."'!");
	get("My reading ".$patt." in '".$text."'.");
	say("What is '".$patt."' context, text?");
	get("There context president, text president putin met trump.");
	say_thing($patt,false);
	
	$patt = "\$x {encountered met joined} {trump putin junping}";
	say_thing($patt);
	say("'".$patt."' has x.");
	say("x patterns '{trump putin jinping}'.");
	get("Ok.");
	say("You read '".$patt."' in '".$text."'!");
	get("My reading ".$patt." in '".$text."'.");
	say("What is '".$patt."' x, text?");
	get("There text putin met trump, x putin.");
	say_thing($patt,false);
	
	$patt = "\$x \$y {trump putin junping}";
	say_thing($patt);
	say("'".$patt."' has x, y.");
	say("x patterns '{trump putin jinping}'.");
	get("Ok.");
	say("y is meeting. meeting patterns '{join joins joined meet meets met encounter encounters encountered}'.");
	get("Ok. Ok.");
	say("You read '".$patt."' in '".$text."'!");
	get("My reading ".$patt." in '".$text."'.");
	say("What is '".$patt."' x, y, text?");
	get("There text putin met trump, x putin, y met.");
	say_thing($patt,false);
	
	$patt = "\$x \$y \$z";
	say_thing($patt);
	say("'".$patt."' has x, y, z.");
	say("x patterns '{trump putin jinping}'.");
	get("Ok.");
	say("y is meeting. meeting patterns '{join joins joined meet meets met encounter encounters encountered}'.");
	get("Ok. Ok.");
	say("z is person. person patterns '{trump putin jinping}'.");
	get("Ok. Ok.");
	say("You read '".$patt."' in '".$text."'!");
	get("My reading ".$patt." in '".$text."'.");
	say("What is '".$patt."' x, y, z, text?");
	get("There text putin met trump, x putin, y met, z trump.");
	say_thing($patt,false);
	
	$patt = "\$x \$y \$z";
	say_thing($patt);
	say("'".$patt."' has x, y, z.");
	say("x is person. z is person. person patterns '{trump putin jinping}'.");
	get("Ok. Ok. Ok.");
	say("y is meeting. meeting patterns '{join joins joined meet meets met encounter encounters encountered}'.");
	get("Ok. Ok.");
	say("You read '".$patt."' in '".$text."'!");
	get("My reading ".$patt." in '".$text."'.");
	say("What is '".$patt."' x, y, z, text?");
	get("There text putin met trump, x putin, y met, z trump.");
	say_thing($patt,false);

	/*
	$patt = "\$x \$y \$z";
	say_thing($patt);
	say("'".$patt."' has x, y, z.");
	say("x is v_putin. v_putin patterns '{[v . putin] [vladimir putin] [putin]} [president putin]'.");
	get("Ok. Ok.");
	say("z is d_trump. d_trump patterns '{[d . trump] [donald trump] [trump]} [president trump]'.");
	get("Ok. Ok.");
	say("y is meeting. meeting patterns '{join joins joined meet meets met encounter encounters encountered}'.");
	get("Ok. Ok.");
	say("You read '".$patt."' in '".$text."'!");
	get("My reading ".$patt." in '".$text."'.");
	say("What is '".$patt."' x, y, z, text?");
	get("There text putin met trump, x putin, y met, z trump.");
	say_thing($patt,false);
	*/
	
	say("No name x.");
	get();
	say("No name y.");
	get();
	say("No name z.");
	get();
	
	//TODO: match entity by pattern of variable
	
	//TODO: match entity by pattern of variable domain
	
	//TODO: create relationships !?
	
	
	//sanity check
	say("You forget!");
	get("Ok.");
	say("What your things count?");
	get("My things count ".($base_things_count).".");
	//finalize
	say("Your trusts no john.");
	get("Ok.");
	say("No email john@doe.com.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("My logout.");	
	get("Ok.");
}

function test_authoring() {//content authoring and sharing
	global $version;
	global $copyright;
	login();//john doe
	//Making sure that redundant news are not appearing
	say("there is 'test page', sources http://localtest.com/test/, times today, new true.");
	get("Ok.");
	say("what is test page");
	get("There is test page, sources http://localtest.com/test/, times today.");
	say("what is test page new, times");
	get("There new true, times today.");
	say("there is 'test page', sources http://localtest.com/test/, times today, new true.");
	get("No.");//nothing is updated //TODO: say somethig more reasonable
	say("what is test page");
	get("There is test page, sources http://localtest.com/test/, times today.");
	say("is test page new false");
	get("Ok.");
	say("no there is 'test page'");
	get("Ok.");
	say("what is test page");
	get("No.");
	//Content authoring and sharing
	$john_cookie = get_cookie();//save john session
	set_cookie(null);//reset session
	login("doe","doe@john.org","john","q","a");
	say("email john@doe.org trust true");//trust to john
	get("Ok.");
	say("what new true");
	get("No.");
	say("what is test page");
	get("No.");
	$doe_cookie = get_cookie();//save doe session
	set_cookie($john_cookie);//back to john
	say("email doe@john.org share true");//share to doe
	get("Ok.");
	// author content
	say("there is 'test page', sources http://localtest.com/test/, times today, new true update.");
	get("Ok.");
	say("what new true");
	get("There is test page, sources http://localtest.com/test/, times today.");
	say("email doe@john.org share false");//stop sharing to doe
	get("Ok.");
	set_cookie($doe_cookie);//back to doe
	//check if new content has appeared
	say("what is test page");
	get("There is test page, sources http://localtest.com/test/, times today.");
	say("what new true");
	get("There is test page, sources http://localtest.com/test/, times today.");
	//clean content
	logout("doe",false);//relaxed cleanup
	set_cookie($john_cookie);//back to john
	//clean content
	say("is test page new false");
	get("Ok.");
	say("no there is test page");
	get("Ok.");
	logout();//john doe
}

test_init();
test_sites();
test_extractor();
test_authoring();
test_summary();

?>
