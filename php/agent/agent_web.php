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

function test_agent_rss() {
	global $basePath;
	global $version;
	global $copyright;

	//Arxiv Atom demo
	login();
	$filedata = file_get_contents("html/test/arxiv.xml");
	$filedata = str_replace("2020-02-02",date("Y-m-d"),$filedata);
	file_put_contents($basePath."html/test/arxiv1.xml",$filedata);
	say_site("http://localtest.com/test/arxiv1.xml");
	say_thing("reputation");
	say("You reading site http://localtest.com/test/arxiv1.xml");
	get("My reading site http://localtest.com/test/arxiv1.xml.");
	say("What is reputation title, text, sources?");
	get("There sources http://arxiv.org/abs/1902.03857v1, text a reputation system for marketplaces - viability assessment, title 'A Reputation System for Marketplaces - Viability Assessment'; sources http://arxiv.org/abs/1902.03857v1, text and explore how various kinds of reputation systems with different parameters impact the economic security of the marketplace, title 'A Reputation System for Marketplaces - Viability Assessment'; sources http://arxiv.org/abs/1902.03857v1, text in this work we explore the implementation of the reputation system for a generic marketplace, title 'A Reputation System for Marketplaces - Viability Assessment'.");
	say("No there times today.");
	del_news_today("yesterday");
	del_news_today();
	say("What times today?");
	get("No.");
	say_thing("test",false);
	say_site("http://localtest.com/test/arxiv1.xml",false);
	logout();
	
	//Plain Atom demo with images
	login();
	$filedata = file_get_contents("html/test/atom.xml");
	$filedata = str_replace("2020-02-02",date("Y-m-d"),$filedata);
	file_put_contents($basePath."html/test/atom1.xml",$filedata);
	say_site("http://localtest.com/test/atom1.xml");
	say_thing("test");
	say("You reading site http://localtest.com/test/atom1.xml!");
	get("My reading site http://localtest.com/test/atom1.xml.");
//TODO: make it working - it does not work because image attribure is not parsed!
	//say("What is test text, title, image, times, sources?");
	say("What is test text, title, sources, image?");
	get("There image http://localtest.com/test/junk.jpg, sources http://example.org/2003/12/13/atom03, text text about test feed, title 'Title about test feed'; image http://localtest.com/test/junk.jpg, sources http://example.org/2003/12/13/atom03, text title about test feed, title 'Title about test feed'; image http://localtest.com/test/stuff.jpg, sources http://localtest.com/, text text for test image, title 'Test with image'.");
	del_news_today();
	say("What times today?");
	get("No.");
	say_thing("test",false);
	say_site("http://localtest.com/test/atom1.xml",false);
	logout();
	
	//RSS demo
	login();
	$filedata = file_get_contents("html/test/rss.xml");
	$filedata = str_replace("Wed, 1 Jan 2020 00:00:00 -0000",date("D, j M Y H:m:s O"),$filedata);
	file_put_contents($basePath."html/test/rss1.xml",$filedata);
	say_site("http://localtest.com/test/rss1.xml");
	say_thing("test");
	say("You reading site http://localtest.com/test/rss1.xml!");
	get("My reading site http://localtest.com/test/rss1.xml.");
	say("What is test text, title?");
	get("There text test results scores, title 'RSS Solutions for Schools and Colleges'; text test the reaction of audience about the coming events and keep the community aware of changes in, title 'RSS Solutions for Governments'.");
//TODO: make it working if no images found - it does not work because image attribure is not parsed!
	//say("What is test text, image, title, times, sources?");
	say("What is test text, title, sources, image?");
	get("There image http://localtest.com/test/garbage.jpg, sources http://www.feedforall.com/government.htm, text test the reaction of audience about the coming events and keep the community aware of changes in, title 'RSS Solutions for Governments'; image http://localtest.com/test/stuff.jpg, sources http://www.feedforall.com/schools.htm, text test results scores, title 'RSS Solutions for Schools and Colleges'.");
	del_news_today();
	say("What times today?");
	get("No.");
	say_thing("test",false);
	say_site("http://localtest.com/test/rss1.xml",false);
	logout();
}

function test_agent_web() {
	global $basePath;
	global $version;
	global $copyright;
	
	//TODO: fix - why it is needed!?
	$sleep_seconds = 5;
	$test_all = false;

	//login, registration, verification
	say("My name john, email john@doe.org, surname doe, birth date 1/2/1987.");
	get("What your secret question, secret answer?");
	say("My secret question q, secret answer a.");
	get("What your q?");
	say("My q a.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

	say("You forget everything!");
	get("Ok.");

	//TODO: etherscamdb: related addresses: 0x12345...
	if ($test_all)
	{
		//$site = 'https://etherscamdb.info/domain/line-crypto.com';
		$site = 'https://etherscamdb.info/scams/';
		$patt = 'related addresses : $word';
		say_thing($patt);
		say("No there is '".$patt."'.");
		say("What is '".$patt."' text?");
		get("No.");
		say("You reading '".$patt."' in '".$site."', range 1, minutes 10!");//it works
		//say("You reading '".$patt."' in '".$site."', range 4, minutes 60, limit 1000!");//it works, but long
		get("My reading ".$patt." in ".$site.".");
		say("What is '".$patt."' word?");
		get();
		//say("What is '".$patt."' text, sources?");
		//get();
		//get("There sources https://etherscamdb.info/domain/dexlauncch.com, text related addresses : 0xd22066c4e511698b626aea89cf70fba5fd3f37d4.");
		say_thing($patt,false);
		say("No there times today.");
		
		say("You forget everything!");
		get("Ok.");
	}
	
	// Gismeteo - works!
	if ($test_all) 
	{
		say("My topics temperature.");
		say("My trusts temperature.");
		say("Temperature patterns 'Новосибирск \$value °C', has value.");
		say("Value is number.");
		say("No there is temperature.");
		say("You reading in http://www.gismeteo.ru/city/daily/4690/!");//Novosibirsk
		get();	 
		say("What is temperature?");
		get();	 
		say("My topics no temperature.");
		say("My trusts no temperature.");
		say("No there times today.");
		say("No name temperature.");
		say("No name value.");
		//
		say("My topics 'Новосибирск \$number °C'.");
		say("My trusts 'Новосибирск \$number °C'.");
		say("No there is 'Новосибирск \$number °C'.");
		//say("You reading in http://www.gismeteo.ru/city/daily/4690/!");//Novosibirsk
		say("My sites http://www.gismeteo.ru/city/daily/4690/.");//Novosibirsk
		say("My trusts http://www.gismeteo.ru/city/daily/4690/.");//Novosibirsk
		say("You read!");
		get();
		say("What is 'Новосибирск \$number °C'?");
		get();
		say("My topics no 'Новосибирск \$number °C'.");
		say("My trusts no 'Новосибирск \$number °C'.");
		say("No there times today.");
		say("No name 'Новосибирск \$number °C'.");
	}

	// Sun flares - works!
	if ($test_all) 
	{
		say("My topics flare.");
		say("My trusts flare.");
		say("flare patterns 'Вспышка балла \$word'.");
		say("No there is flare.");
		say("You reading in http://www.tesis.lebedev.ru/sun_flares.html!");
		say("What is flare word?");
		get();
		say("My topics no flare.");
		say("My trusts no flare.");
		say("No there times today.");
		say("No name flare.");
		//say("No name word.");
		//
		say("My topics 'Вспышка балла \$word'.");
		say("My trusts 'Вспышка балла \$word'.");
		say("No there is 'Вспышка балла \$word'.");
		say("You reading in http://www.tesis.lebedev.ru/sun_flares.html!");
		say("What is 'Вспышка балла \$word' text?");
		get();
		say("My topics no 'Вспышка балла \$word'.");
		say("My trusts no 'Вспышка балла \$word'.");
		say("No there times today.");
		say("No name 'Вспышка балла \$word'.");
		//say("No name word.");
	}
	
	//Reading with possible redirects: https://pronovostroy.ru/forum/6891-все-о-ремонте
	if ($test_all)
	{
		//this works:
		$site = "https://pronovostroy.ru/forum/6891-все-о-ремонте/";
		//this works:
		//$site = "https://pronovostroy.ru/forum/6891-%D0%B2%D1%81%D0%B5-%D0%BE-%D1%80%D0%B5%D0%BC%D0%BE%D0%BD%D1%82%D0%B5/";
		//$patt = "ремонта";//present - WORKS
		$patt = "мнения";//present on the linked page - WORKS
		//$patt = "алконавтов";//missed - iterative redirect FAILS
		//TODO: this fails!!!
		//TODO: this fails!!!
		//$site = "http://novoseli.ru/index.php?/forum/3442-лобня";
		//$patt = "новостроек";//vsenovoseli
		say_thing($patt);
		say("No there is '".$patt."'.");
		say("What is '".$patt."' text?");
		get("No.");
		say("You reading '".$patt."' in '".$site."'!");
		get();
		say("What is '".$site."' text?");
		get();
		say("What is '".$patt."' text?");
		get();
		say("What is '".$patt."' text, sources?");
		get();
		say_thing($patt,false);
		say("No there times today.");
	}
	
	//Reading Steemit: https://steemit.com/ethereum/@aigents/ethereum-graphs-with-aigents
	if ($test_all)
	{
		$patt = "trying";
		say_thing($patt);
		say("No there is '".$patt."'.");
		say("What is '".$patt."' text?");
		get("No.");
		say("You reading '".$patt."' in 'https://steemit.com/ethereum/@aigents/ethereum-graphs-with-aigents'!");
		say("What is '".$patt."' text?");
		get("There text you can enjoy trying our latest release.");
		say("What is '".$patt."' text, sources?");
		get("There sources https://steemit.com/ethereum/@aigents/ethereum-graphs-with-aigents, text you can enjoy trying our latest release.");
		say_thing($patt,false);
		say("No there times today.");
	}
	
	//Magnetic storms - works with minor issues!?
	if ($test_all) 
	{
		/*
		//TODO: over-doubled pattern read!!!
		//$patt = "geomagnetic storms were observed for the past 3 hours";
		$patt = "geomagnetic storms were observed for the past \$word hours";
		say_thing($patt);
		say("My topics '".$patt."'.");
		say("My trusts '".$patt."'.");
		say("No there is '".$patt."'.");
		say("You reading '".$patt."' in http://tesis.lebedev.ru/en/magnetic_storms.html!");
		say("What is '".$patt."' text?");
		get();
		say_thing($patt,false);
		say("No there times today.");
		*/

		//single pattern and single site - WORKS
		$patt = "magnetic storm of level \$stormlevel from \$fromtime to \$totome";
		say_thing($patt);
		say("No there is '".$patt."'.");
		say("What is '".$patt."' text?");
		get("No.");
		say("You reading '".$patt."' in 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=21&y=2018'!");
		say("What is '".$patt."' text?");
		get("There text 'magnetic storm of level g1 ( minor ) from 21:00 to 00:00'.");
		say("What is '".$patt."' stormlevel, fromtime, totome, new?");
		get("There fromtime 21:00, new false, stormlevel 'g1 ( minor )', totome 00:00.");
		say_thing($patt,false);
		say("No there times today.");

		//no pattern and single site - WORKS - many matches in page area (range = 1?)
		$patt = "magnetic storm of level \$stormlevel from \$fromtime to \$totome";
		$site = "http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=21&y=2018";
		say("You forget everything!");
		say_thing($patt);
		say_site($site);
		say("No there is '".$patt."'.");
		say("What is '".$patt."' text?");
		get("No.");
		say("You reading site '".$site."'!");
		sleep(10);
		say("What is '".$patt."' text, sources?");
		get("There sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=10&y=2018', text 'magnetic storm of level g1 ( minor ) from 15:00 to 18:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=10&y=2018', text 'magnetic storm of level g1 ( minor ) from 18:00 to 21:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=11&y=2018', text 'magnetic storm of level g1 ( minor ) from 03:00 to 06:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=11&y=2018', text 'magnetic storm of level g2 ( moderate ) from 06:00 to 09:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=11&y=2018', text 'magnetic storm of level g2 ( moderate ) from 09:00 to 12:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=14&y=2018', text 'magnetic storm of level g1 ( minor ) from 00:00 to 03:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=21&y=2018', text 'magnetic storm of level g1 ( minor ) from 21:00 to 00:00'.");
		say_thing($patt,false);
		say_site($site,false);
		say("No there times today.");
	
		//TODO:
		//no pattern and no sites - WORKS - many matches in page area (range = 1?)
		$patt = "magnetic storm of level \$stormlevel from \$fromtime to \$totome";
		$site = "http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=21&y=2018";
		say("You forget everything!");
		say_thing($patt);
		say_site($site);
		say("No there is '".$patt."'.");
		say("What is '".$patt."' text?");
		get("No.");
		say("You reading!");
		sleep(20);
		say("What is '".$patt."' text, sources?");
		get("There sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=10&y=2018', text 'magnetic storm of level g1 ( minor ) from 15:00 to 18:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=10&y=2018', text 'magnetic storm of level g1 ( minor ) from 18:00 to 21:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=11&y=2018', text 'magnetic storm of level g1 ( minor ) from 03:00 to 06:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=11&y=2018', text 'magnetic storm of level g2 ( moderate ) from 06:00 to 09:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=11&y=2018', text 'magnetic storm of level g2 ( moderate ) from 09:00 to 12:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=14&y=2018', text 'magnetic storm of level g1 ( minor ) from 00:00 to 03:00'; sources 'http://tesis.lebedev.ru/en/magnetic_storms.html?m=9&d=21&y=2018', text 'magnetic storm of level g1 ( minor ) from 21:00 to 00:00'.");
		say_thing($patt,false);
		say_site($site,false);
		say("No there times today.");
	
	}
	
	// Aigents news - works!
	if ($test_all) 
	{
		say("My topics '{mission release} \$text'.");
		say("My trusts '{mission release} \$text'.");
		say_site("http://aigents.com/en");
		say("You read!");
		get();
		say("What new true text, trust?");
		get();
		say("New true trust true.");
		say("What new true text, trust?");
		get();
		say("You read!");
		get();
		say("What new true text, trust?");
		get();
		say("My topics no '{mission release} \$text'.");
		say("My trusts no '{mission release} \$text'.");
		say("Times today new false.");
		say("No there times today.");
		say("No name '{mission release} \$text'.");
		say_site("http://aigents.com/en/contacts.html",false);
		say_site("http://aigents.com/en",false);
	}

	// Business news - works!
	if ($test_all) {
		say("My topics 'google \$text', 'apple \$text', '\$buyer {buyes bought acquires acquired} \$subject'.");
		say("My trusts 'google \$text', 'apple \$text', '\$buyer {buyes bought acquires acquired} \$subject'.");
		say("You reading in http://wired.com!");
		say("What is 'google \$text' text?");
		get();
		say("What is 'apple \$text' text?");
		get();
		say("What is '\$buyer {buyes bought acquires acquired} \$subject' text?");
		get();
		say("My topics no 'google \$text', no 'apple \$text', no '\$buyer {buyes bought acquires acquired} \$subject'.");
		say("My trusts no 'google \$text', no 'apple \$text', no '\$buyer {buyes bought acquires acquired} \$subject'.");
		say("No there times today.");
		say("No name 'google \$text'.");
	}
	
	// 	Доллар США $ руб. 56,4271 руб. ↑57,7279	
	if ($test_all) 
	{
		say("My topics usdrub.");
		say("My trusts usdrub.");
		//works:
		//say("usdrub patterns 'Доллар США руб. \$price руб. \$price', has price.");
		//say("price is number.");
		//works:
		//say("usdrub patterns 'Доллар США руб. \$number руб. \$number'.");
		//works:
		say("usdrub patterns 'Доллар руб. \$number руб. \$number'.");
		//say("You reading in '28.03.2015   .     Доллар США  $       руб.  56,4271       руб.   ↑ 57,7279      .     Евро'!");
		say("You reading in http://cbr.ru!");
		say("What is usdrub?");
		get();
		say("My topics no usdrub.");
		say("My trusts no usdrub.");
		say("No there times today.");
		say("No name usdrub.");
		say("No name price.");
		//say("No name word.");
		//say("No name number.");
	}	
	
	// Weather in Beiging 
	if ($test_all) 
	{
		say("My topics '关注的城市 \$info'.");
		say("My trusts '关注的城市 \$info'.");
		say("No there is '关注的城市 \$info'.");
		say("You reading in http://m.tianqi.com/beijing/!");
		say("What is '关注的城市 \$info'?");
		get();
		say("No there is '关注的城市 \$info'.");
		//TODO:redirect
		say("You reading in http://beijing.tianqi.com!");
		say("What is '关注的城市 \$info'?");
		get();
		say("My topics no '关注的城市 \$info'.");
		say("My trusts no '关注的城市 \$info'.");
		say("No there times today.");
		say("No name '关注的城市 \$info'.");
	}

	//test rss and titles
	if (true){
		file_put_contents($basePath."html/test.html","<html><head><title>about the animals</title></head><body><img src=\"test/stuff.jpg\"/><h1>about the elephants</h1>elephants live in savanna<p><h1>about the tigers</h1>tigers live in jungle<img src=\"test/junk.jpg\"/></body></html>");
		say_site("http://localtest.com/test.html");
		say_thing("live");
		say("You reading in http://localtest.com/test.html!");
		get("My reading site http://localtest.com/test.html.");
		say("What is 'http://localtest.com/test.html' text?");
		get("There text 'about the elephants . elephants live in savanna . about the tigers . tigers live in jungle'.");
		say("What sources 'http://localtest.com/test.html' text?");
		get("There text elephants live in savanna; text tigers live in jungle.");
		say("What new true, times today?");
		get("There about in jungle, context tigers, image http://localtest.com/test/junk.jpg, is live, sources http://localtest.com/test.html, text tigers live in jungle, times today, title about the tigers; about in savanna, context elephants, image http://localtest.com/test/stuff.jpg, is live, sources http://localtest.com/test.html, text elephants live in savanna, times today, title about the elephants.");
		say("What new true, times today title, text?");
		get("There text elephants live in savanna, title about the elephants; text tigers live in jungle, title about the tigers.");
		say("What new true, times today text, title?");
		get("There text elephants live in savanna, title about the elephants; text tigers live in jungle, title about the tigers.");
		say("What new true, times today or yesterday title, is, sources, text, image, times?");
		get("There image http://localtest.com/test/junk.jpg, is live, sources http://localtest.com/test.html, text tigers live in jungle, times today, title about the tigers; image http://localtest.com/test/stuff.jpg, is live, sources http://localtest.com/test.html, text elephants live in savanna, times today, title about the elephants.");
		say("What new true, times today or yesterday is, sources, text, image, times, title?");
		get("There image http://localtest.com/test/junk.jpg, is live, sources http://localtest.com/test.html, text tigers live in jungle, times today, title about the tigers; image http://localtest.com/test/stuff.jpg, is live, sources http://localtest.com/test.html, text elephants live in savanna, times today, title about the elephants.");
		say("My areas everything, shares everything.");
		get("Ok.");
		say("text 'elephants live in savanna' new false");
		say("rss everything");
		$expected_rss = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" .
"<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" .
"\n" .
"<channel>\n" .
"  <title>Aigents on Everything</title>\n" .
"  <link>https://aigents.com</link>\n" .
"  <description>Aigents RSS feed about Everything</description>\n" .
"  <item>\n" .
"    <title>about the tigers</title>\n" .
"    <link>http://localtest.com/test.html</link>\n" .
"    <description xml:space=\"preserve\">topic: \"live\".\ntigers live in jungle</description>\n" .
"    <enclosure url=\"http://localtest.com/test/junk.jpg\" type=\"image\" />\n";
		get($expected_rss,false,true);//partial
		say("My areas not everything, shares not everything.");
		del_news_today();
		say("What times today?");
		get("No.");
		say_thing("live",false);
		say_site("http://localtest.com/test.html",false);
	}
	
	//test and hierarchy of titles
	if (true){
		file_put_contents($basePath."html/test.html","<html><head><title>everything title</title></head><body>old everything<p>new everything<p><h1>man title</h1>old man<p>new man<p><h2>woman title</h2>old woman<p>new woman<p><h3>dog title</h3>old dog<p>new dog<p><h5>wrong title</h6>old everything<p>new everything</body><h6>cat title</h6>old cat<p>new cat</body></html>");
		say_site("http://localtest.com/test.html");
		say_thing("new \$word");
		say("You reading in http://localtest.com/test.html!");
		get("My reading site http://localtest.com/test.html.");
		say("What is 'http://localtest.com/test.html' text?");
		get("There text 'old everything . new everything . man title old man . new man . woman title old woman . new woman . dog title old dog . new dog . wrong title old everything . new everything cat title old cat . new cat'.");
		say("What sources 'http://localtest.com/test.html' text?");
		get("There text new cat; text new dog; text new everything; text new man; text new woman.");
		say("What sources 'http://localtest.com/test.html' title, text?");
		get("There text new cat, title cat title; text new dog, title dog title; text new everything, title everything title; text new man, title man title; text new woman, title woman title.");
		del_news_today();
		say("What times today?");
		get("No.");
		say_thing("new \$word",false);
		say_site("http://localtest.com/test.html",false);
	}	

	//TODO: test tags in html
	/*
- remove scripts from texts !!!!!!! : 
#35213 text "\n \n \n \n \';a.src=\"data:text/html;charset=utf-8,\"+encodeuri(s),a.style.position=\"absolute\",a.style.visibility=\"hidden\",a.style.left=\"-999px\",a.style.top=\"-999px\",a.onload=settimeout.bind(window,function(){\"undefined\"==typeof u?n((0,d[\"default\"])(d[\"default\"].result.error,d[\"default\"].test.css,d[\"default\
#14984 name 'http://beta.speedtest.net/it/result/5684599470?preferredLocale=it'.
<script>
<svg>
	 */
	if (false) {
		say_site("http://beta.speedtest.net/it/result/5684599470?preferredLocale=it");
		say_thing("test \$word");
		say("what my topics?");
		get();
		say("what my trusts?");
		get();
		say("You reading!");
		say("what my topics?");
		get();
		sleep(10);
		say("what my topics?");
		get();
		say("What is 'http://beta.speedtest.net/it/result/5684599470?preferredLocale=it' text?");
		get();
		say("what my topics?");
		get();
		say_thing("test \$word",false);
		say_site("http://beta.speedtest.net/it/result/5684599470?preferredLocale=it",false);
		//TODO: cleanup linked sites too!?
	}
	
	// Test Novosibirsk 
	if (true)
	{
		/*
		- why key is missed?
		{[владимир городецкий] [в . городецкий]} $suffix
		http://sibkray.ru/
		отчитался за 2015 год глава региона рассказал о проблемах в строительной
		*/
/*		
		say_thing("{[владимир городецкий] [в . городецкий]} \$suffix");
		//say_site("http://sibkray.ru/");
		//say_site("http://localtest.com/nsk/sibkray1.html");
		say_site("http://localtest.com/test.html");
		
		file_put_contents($basePath."html/test.html",
		//"2016 году . владимир кехман согласился прийти на публичные слушания . поддельные пятитысячные купюры «ходят» по новосибирску . гибдд создает спецотряд для ловли лихачей на дорогах . кехмана обязали вернуть исторический облик оперного театра . пикет в защиту ганчара: никто не хочет разбираться в деле . спасатели вытащили из снега машину с замерзающими людьми . детские сады и школы закрывают на карантин . экологи отказались от иска к «норд сити моллу» . . все новости >> предложить новость >> итоговый рейтинг: городецкий – плюс 1, толоконский – минус 8");
		//"2016 году . владимир кехман . поддельные  . пикет в защиту в деле . спасатели рейтинг: городецкий – плюс 1, толоконский – минус 8");
		//"владимир кехман . поддельные  . пикет в защиту в деле . спасатели рейтинг: городецкий – плюс 1");
		"владимир кехман . пикет в защиту . рейтинг: городецкий – плюс 1");
		
		say("You reading!");
		sleep($sleep_seconds);
		say("What sources http://localtest.com/test.html text?");
		//say("What times today text?");
		get();
		
//TODO:
//'владимир городецкий в . городецкий – плюс 1' - alternatives are glued up 
//'владимир городецкий в . городецкий – плюс 1' - distant period accounted - alternatives merged due to 'new "greedy" Any-parsing strategy' 
//'в . городецкий – плюс 1' - distant period accounted
		
		del_news_today();
		say_thing("{[владимир городецкий] [в . городецкий]} \$suffix",false);
		//say_site("http://sibkray.ru/",false);
		//say_site("http://localtest.com/nsk/sibkray1.html",false);
		say_site("http://localtest.com/test.html",false);
*/		
		/*
		- why key is duplicated
		{[владимир городецкий] [в . городецкий]} $suffix
		http://tayga.info/companies/3014
		владимир городецкий в . городецкий во время рабочей поездки
		
		- oversized texts included
		{[владимир городецкий] [в . городецкий]} $suffix
		http://tayga.info/details/2014/09/04/~117760
		владимир городецкий в . городецкий ( справа ) 4 сентября 14, 07:29 . твитнуть . . нравится . . . с переходом в правительство сергей боярский, скорее всего, попытается сохранить контроль за строительной ...
		
		- break sentences: в . городецкий раскритиковал «возню» вокруг четвертого моста адвокатов юрченко попросили «не пытать» свидетелей
		google . it’s about facebook and microsoft and and the other giants of tech
		http://www.wired.com/2016/01/googles-go-victory-is-just-a-glimpse-of-how-powerful-ai-will-be
		*/
	}

	//test images
	if (true){
		file_put_contents($basePath."html/test/garbage.jpg","stub");
		file_put_contents($basePath."html/test/stuff.jpg","stub");
		file_put_contents($basePath."html/test/junk.jpg","stub");
		file_put_contents($basePath."html/test.html","<html><head><base href=\"http://localtest.com/test/\"></head><body>                                                                                                                                                                                                                                                                                                                                                                                         any                                                              of                                                                                  my plain garbage <img src=\"http://localtest.com/test/garbage.jpg\"/> is my test garbage and it is not<img src=\"stuff.jpg\"/> my dummy stuff or just some kind of any annoying and weird dirty <img src=\"junk.jpg\"/> junk garbage.</body></html>");
		say_site("http://localtest.com/test.html");
		say_thing("plain \$word");
		say_thing("test \$word");
		say_thing("dummy \$word");
		say("You reading!");
		sleep($sleep_seconds);
		say("What is 'http://localtest.com/test.html' text?");
		get("There text 'any of my plain garbage is my test garbage and it is not my dummy stuff or just some kind of any annoying and weird dirty junk garbage.'.");
		say("What is 'plain \$word' image, text?");
		get("There image http://localtest.com/test/garbage.jpg, text plain garbage.");
		say("What is 'test \$word' text, image?");
		get("There image http://localtest.com/test/garbage.jpg, text test garbage.");
		say("What is 'dummy \$word' image, word?");
		get("There image http://localtest.com/test/stuff.jpg, word stuff.");
		del_news_today();
		say("What times today?");
		get("No.");
		say_thing("plain \$word",false);
		say_thing("test \$word",false);
		say_thing("dummy \$word",false);
		say_site("http://localtest.com/test.html",false);
	}

	//test links
	if (true){
		//TODO: google missed:
		file_put_contents($basePath."html/test.html","<html><head><base href=\"http://localtest.com/test/\"></head><body>                                                                                                                                                                                                                                                                                                                                                                                         any                                                              of                                                                                  my plain garbage. <a href=\"http://google.com\">google site link</a>. is my test garbage and it is not.<a href=\"http://microsoft.com\">site of microsoft.</a> my dummy stuff or just some kind of any annoying and weird dirty .<a href=\"http://facebook.com\" target=\"_blank\">Facebook site</a> . junk garbage.  just test site.</body></html>");
		say_site("http://localtest.com/test.html");
		say_thing("site");
		say("You reading!");
		sleep($sleep_seconds);
		say("What is 'site' sources, text?");
		get("There sources http://facebook.com, text facebook site; sources http://google.com, text google site link; sources http://localtest.com/test.html, text just test site; sources http://microsoft.com, text site of microsoft.");
		del_news_today();
		say("What times today?");
		get("No.");
		say_thing("site",false);
		say_site("http://localtest.com/test.html",false);
	}
		
	//test html codes
	if (true) 
	{
		say_thing("test dummy");
		say_site("http://localtest.com/test.html");
		//test html codes
		file_put_contents($basePath."html/test.html","<html><body>1&#8212;2&#8364;-&laquo;quoted&raquo;- test dummy</body></html>");
		sleep($sleep_seconds);
		println("http://localtest.com/test.html:".file_get_contents("http://localtest.com/test.html"));//to ensure file is re-cached by server :-)
		say("You reading!");
		sleep($sleep_seconds);
		say("What times today?");
		get();
		say("What is http://localtest.com/test.html?");
		//get("There is http://localtest.com/test.html, text 1—2€-«quoted»- test dummy, times today.");
		get("There is http://localtest.com/test.html, text '1—2€-\"quoted\"- test dummy', times today.");
		del_news_today();
		file_put_contents($basePath."html/test.html","<html><body><div class=\"ngs-footer__info\">&copy; ЗАО &laquo;НГС&raquo;</div></body></html>");
		say("You reading!");
		sleep($sleep_seconds);
		say("What times today text?");
		//get("There text © зао «нгс».");
		get("There text '© зао \"нгс\"'.");
		del_news_today();
		say("What times today?");
		get("No.");
		say_thing("test dummy",false);
		say_site("http://localtest.com/test.html",false);
	}
	
	//TODO: move out
	//test - what?
	if (true)
	{
		say_site("http://localtest.com/test.html");
		file_put_contents($basePath."html/test.html","my test stuff is here and it is just a test stuff, not test dummy. is is test actually, not a test stuff garbage.</body></html>");
		say_thing("test \$word");
		say("You reading!");
		sleep($sleep_seconds);
		say("What is 'test \$word' text?");
		get("There text test actually; text test dummy; text test stuff.");
		say("What is http://localtest.com/test.html text?");
		get("There text 'my test stuff is here and it is just a test stuff, not test dummy. is is test actually, not a test stuff garbage.'.");
		del_news_today();
		say("What times today?");
		get("No.");
		say_thing("test \$word",false);
		say_site("http://localtest.com/test.html",false);
	}
	
	//TODO: fix reader hangup on attempt to read missed robots.txt when run two times in a row
	//test non-html files
	/*
	if (false) {
		say_thing("test1 \$word");
		say_site("http://localtest.com/test_video.html");
		say("You reading!");
		sleep($sleep_seconds);
		say("What is 'test \$word' text?");
		get();
		say("No there times today.");
		say_thing("test1 \$word",false);
		say_site("http://localtest.com/test.html",false);
	}
	*/
	
	//test - what?
	//TODO: sort out
	if (false)
	{
		say("There is peer, id 91, name 'антон-тест email', trust true, share true, email eprst@email.com, phone +1234567890.");
		get("Ok.");
		say("There is peer, id 95, name 'антон-тест sms', trust true, share true, email eprst@email.com, phone +1234567890.");
		get("Ok.");
		say("What is peer, name eprst id, trust, share, email, phone?");
		get();
		say("What is peer id, share?");
		get();
		say("is peer, id 91 name 'антон-тест email', trust false, share false.");
		get();
		say("is peer, id 95 name 'антон-тест email', trust false, share false.");
		get();
		say("What is peer id, share?");
		get();
		say("No there is peer, id 91.");
		get();
		say("No there is peer, id 95.");
		get();
		say("What is peer id, share?");
		get();
	}
	
	//test regular expressions
	{
		say("My topics relationship.");
		say("relationship patterns '{/john(|ny)$/ /jim(|my)$/} {/marr(y|ied|ies)$/ /engage(|d|s)$/ /divorse(|d|s)$/} {/jan(e|net)$/ /jud(y|ith)$/}'.");
		say("No there is relationship.");
		say("You read relationship in 'jimmy is engaged to jane'");
		say("What is relationship text?");
		get("There text jimmy engaged jane.");
		say("No there is relationship.");
		say("You read relationship in 'bobby has kicked robby'");
		say("What is relationship text?");
		get("No.");
		say("You read relationship in 'john is about to divorse with judith'");
		say("What is relationship text?");
		get("There text john divorse judith.");	
		say("No there is relationship.");
		say("relationship patterns '{{миша михаил} {петя петр}} {/жени(лся|тся)$/ /помолв(лен|ился)$/} {/ма(рии|рией|ше|шей)$/ /ната(ше|шей|лье|льей)$/}'.");
		say("You read relationship in 'петр помолвился с натальей'");
		say("What is relationship text?");
		get("There text петр помолвился натальей.");

		say("No there is relationship.");		
		say("You read relationship in 'сначала петр помолвился с натальей летом. осенью закончилось свадьбой.'");
		say("What is relationship text?");
		get("There text сначала петр помолвился натальей летом.");

		say("No there is relationship.");
		say("You read relationship in 'михаил женится на марии'");
		say("What is relationship text?");
//TODO: why so!?
		get("There text михаил женится марии.");
		
		say("No there is relationship.");
		say("You read relationship in 'сегодня михаил потапов женится на марии жженовой'");
		say("What is relationship text?");
//TODO: why does it matter!?
		//get("There text сегодня михаил женится марии жженовой.");//if disabled partial matching in Property
		get("There text сегодня михаил женится марии жженовой.");//if enabled partial matching in Property
		say("No there is relationship.");
		say("My topics no relationship.");
		say("No name relationship.");

		say("My topics marriage.");
		say("Marriage has fiance, fiancee.");
		//TODO: redo the following with 'patterns' property?
		say("Fiance is '{/john(|ny)$/ /jim(|my)$/}'.");
		say("Fiancee is '{/jan(e|net)$/ /jud(y|ith)$/}'.");
		//TODO: pattern inheritance by is 
		//TODO: make variable to contain normalization of variation of verb
		say("Marriage patterns '\$fiance {/marr(y|ied|ies)$/ /engage(|d|s)$/} \$fiancee'.");
		say("You read marriage in 'john marry judy'.");
		say("What is marriage fiance, fiancee?");
		get("There fiance john, fiancee judy.");
		
		say("No there is marriage.");
		say("My topics no marriage.");
		say("No name marriage.");
		say("No name fiance.");
		say("No name fiancee.");
	}

	if (true) {
		/*
		Test for local news in Novosibirsk district
		- now junk is coming and is not rendered
		Your topics name '$buyer {buy buys bought acquires acquire acquired} $subject', trust true;
		name '{[$prefix {[василий борматов] [в . борматов]}] [{[василий борматов] [в . борматов]}] $suffix}]', trust true;
		name '{[$prefix {[владимир городецкий] [в . городецкий]}] [{[владимир городецкий] [в . городецкий]}] $suffix]}', trust true.
		http://ngs.ru/
		http://sibkray.ru/
		http://tayga.info/
		
		http://sibkray.ru/news/1/879569/
		http://sibkray.ru/blogs/vlasov/
		
//."владимир городецкий сказал раз. в . городецкий сделал два" //TODO: better phrase boundaty detection for владимир городецкий сказал раз.

//say_site("http://sibkray.ru/news/1/879569");

		*/
		//$pat = "{[\$prefix {[василий борматов] [в . борматов]}] [{[василий борматов] [в . борматов]}] \$suffix}]";

		//TODO:GET:There text '.'; text владимир городецкий владимир; text раз; text сказал.
		//$pat = "{[\$prefix {[владимир городецкий] [в . городецкий]}] [{[владимир городецкий] [в . городецкий]}] \$suffix]}";

		//TODO:GET:There text '.'; text владимир городецкий владимир; text раз; text сказал.
		//$pat = "{[{[владимир городецкий] [в . городецкий]}] \$suffix]}";

		say_site("http://localtest.com/test.html");
		
		// pattern: y $z 
		//$pat = "владимир городецкий \$suffix";//OK
		//$pat = "[владимир городецкий] \$suffix";//OK
		//$pat = "{[владимир городецкий]} \$suffix";//OK
		//$pat = "{[владимир городецкий] [в . городецкий]} \$suffix";//OK
		//$pat = "[{[владимир городецкий] [в . городецкий]} \$suffix]";//OK
		//$pat = "{[{[владимир городецкий] [в . городецкий]} \$suffix]}";//OK
		$pat = "{[{[владимир городецкий] [в. городецкий]} \$suffix]}";//OK
		say_thing($pat);
		file_put_contents($basePath."html/test.html","<html><body>владимир филиппович городецкий сказал раз.</body></html>");
		say("You reading!");
		sleep($sleep_seconds);
		say("What new true text?");
		get("There text владимир городецкий сказал раз.");
		del_news_today();
		say_thing($pat,false);

		file_put_contents($basePath."html/test.html","<html><body>в. городецкий написал два.</body></html>");
		say("You reading!");
		sleep($sleep_seconds);
		say("What new true text?");
		get();
		//get("There text в. городецкий написал два.");
		del_news_today();
		say_thing($pat,false);
		
		/*
		//TODO:
		// pattern: $x y 
		file_put_contents($basePath."html/test.html","<html><body>в. ф. городецкий сделал два.</body></html>");
		say("You reading!");
		sleep($sleep_seconds);
		say("What new true text?");
		get("There text 'в . городецкий сделал два'.");
		del_news_today();
		say_thing($pat,false);
		
		//$pat = "\$prefix владимир городецкий";//OK
		//$pat = "\$prefix [владимир городецкий]";//GET:There text однажды владимир городецкий.
		//$pat = "{[\$prefix {[владимир городецкий] [в . городецкий]}]}";//GET:There text однажды владимир городецкий.
		$pat = "{[\$prefix {[владимир городецкий] [в . городецкий]}] [{[владимир городецкий] [в . городецкий]} \$suffix]}";
		say_thing($pat);
		//file_put_contents($basePath."html/test.html","<html><body>однажды пошел владимир филиппович городецкий.</body></html>");
		file_put_contents($basePath."html/test.html","<html><body>бывший мэр в. ф. городецкий.</body></html>");
		say("You reading!");
		sleep($sleep_seconds);
		say("What new true text?");
		//get("There text 'однажды пошел владимир филиппович городецкий'.");
		get();
		del_news_today();
		say_thing($pat,false);
		*/
		
		//TODO:
		// pattern: $x y $z
		
		say_site("http://localtest.com/test.html",false);
		say_site("http://sibkray.ru/news/1/879569",false);
	}
	
	//test ranges
	if (true){
		$site = 'http://localtest.com/sitea/';
		$patt = 'people';
		say_thing($patt);
		say("No there is '".$patt."'.");
		say("What is '".$patt."' text?");
		get("No.");
		//should not be able to read in range = 1
		say("You reading '".$patt."' in '".$site."', range 1, minutes 10!");
		get("No.");
		//should be able to read in range = 2
		say("You reading '".$patt."' in '".$site."', range 2, minutes 10!");
		get("My reading ".$patt." in ".$site.".");
		say("What is '".$patt."' text?");
		get("There text our mission is to make people happier.");
		say_thing($patt,false);
		$patt = 'information';
		say_thing($patt);
		//should not be able to read in range = 2
		say("You reading '".$patt."' in '".$site."', range 2, minutes 10!");
		get("No.");
		//should be able to read in range = 3
		say("You reading '".$patt."' in '".$site."', range 3, minutes 10!");
		get("My reading ".$patt." in ".$site.".");
		say("What is '".$patt."' text?");
		get("There text here is more information.");
		say_thing($patt,false);
		say("No there times today.");
		get("Ok.");
		say("What is '".$patt."' text?");
		get("No.");
		//start over with cleaned paattern path memory
		say_thing($patt);
		//should not be reading target in range 2
		say("You reading '".$patt."' in '".$site."', range 2, minutes 10!");
		get("No.");
		say("What is '".$patt."' text?");
		get("No.");
		//should be able to read in default range = 3
		say("You reading '".$patt."' in '".$site."'!");
		get("My reading ".$patt." in ".$site.".");
		say("What is '".$patt."' text?");
		get("There text here is more information.");
		say_thing($patt,false);
		say("No there times today.");
	}
	
	say("You forget!");
	get("Ok.");
	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	get("Ok.");
	//say("No name number.");
	//get("Ok.");
	say("My logout.");
	get("Ok.");
}	
	
test_init();
test_agent_rss();
test_agent_web();
test_summary();

?>