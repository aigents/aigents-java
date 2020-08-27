<?php
/*
 * MIT License
 * 
 * Copyright (c) 2014-2019 by Anton Kolonin, Aigents®
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

function test_matching($pattern, $text, $expect) {
	say("My topics '".$pattern."'.");
	get("Ok.");
	file_put_contents($basePath."html/test.html",$text);
	say("You reading '".$pattern."' in http://localtest.com/test.html!");
	get("My reading ".$pattern." in http://localtest.com/test.html.");
	say("What is '".$pattern."' text?");
	get($expect);
	say("No there is '".$pattern."'.");
	say("My topics no '".$pattern."'.");
	say("No name '".$pattern."'.");
}
	

function test_agent_patterns() {
	global $basePath;
	global $version;
	global $copyright;
	global $base_things_count;
	
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com, name john, surname doe, secret question x, secret answer y.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

	//test synonyms
	test_matching("{dolphins whales}","There are whales in the ocean. There are dolphins in the sea.","There text there are dolphins in the sea; text there are whales in the ocean.");
	test_matching("{a b c}","x y b","There text x y b.");
	test_matching("{коронавирус коронавируса коронавирусом}","x y коронавируса","There text x y коронавируса.");
	test_matching("{коронавирус коронавируса коронавирусом}","вылечились от коронавируса","There text вылечились от коронавируса.");

	//test two-side variables
	say("My topics '\$x dolphins \$y'.");
	get("Ok.");
	file_put_contents($basePath."html/test.html","<html><body>Here is the story. Everyone topics that dolphins are cool. They are just great.</body></html>");
	say("You reading '\$x dolphins \$y' in http://localtest.com/test.html!");
	get("My reading \$x dolphins \$y in http://localtest.com/test.html.");
	say("What is '\$x dolphins \$y' text?");
	get("There text everyone topics that dolphins are cool.");
	say("No there is '\$x dolphins \$y'.");
	say("My topics no '\$x dolphins \$y'.");
	say("No name '\$x dolphins \$y'.");
	
	/*
	//TODO:
	brk();
	//news de-duplication
	say("My topics '\$x dolphins \$y'.");
	say("My topics 'to dolphins'.");
	get("Ok.");
	file_put_contents($basePath."html/test.html","<html><body>Here is the story. Link 1 <a href=\"test1.html\">to dolphins</a>. Link 2 <a href=\"test2.html\">to dolphins</a>. They are just great.</body></html>");
	file_put_contents($basePath."html/test1.html","<html><body>Here is the story. Everyone topics that dolphins are cool. They are just great.</body></html>");
	file_put_contents($basePath."html/test2.html","<html><body>Here is the story. Everyone topics that dolphins are cool. They are just great.</body></html>");
	say("You reading '\$x dolphins \$y' in http://localtest.com/test.html!");
	get("My reading \$x dolphins \$y in http://localtest.com/test.html.");
	say("What is '\$x dolphins \$y' text?");
	get();
	//get("There text everyone topics that dolphins are cool.");
	say("No there is '\$x dolphins \$y'.");
	say("My topics no '\$x dolphins \$y'.");
	say("No there is 'to dolphins'.");
	say("My topics no 'to dolphins'.");
	say("No name 'to dolphins'.");
	brk();
	*/
	
	//test basic auto-patterns 
	say("My topics dolphins.");
	get("Ok.");
	
	say("Path?");//sanity check
	get("No.");

	say("No there is dolphins.");
	file_put_contents($basePath."html/test.html","<html><body>Here is the story. Everyone topics that dolphins are cool. They are just great.</body></html>");
	say("You reading dolphins in http://localtest.com/test.html!");
	say("What is dolphins text?");
	get("There text everyone topics that dolphins are cool.");	

	say("No there is dolphins.");
	file_put_contents($basePath."html/test.html","<html><body>Here is the story. Everyone topics dolphins as animals. Dolphins are cool. They are just great.</body></html>");
	say("You reading dolphins in http://localtest.com/test.html!");
	say("What is dolphins text?");
	get("There text dolphins are cool; text everyone topics dolphins as animals.");
	
	say("No there is dolphins.");
	file_put_contents($basePath."html/test.html","<html><body>Here is what. Everyone topics dolphins.</body></html>");
	say("You reading dolphins in http://localtest.com/test.html!");
	say("What is dolphins text?");
	get("There text everyone topics dolphins.");
	
	say("No there is dolphins.");
	file_put_contents($basePath."html/test.html","<html><body>Everyone topics dolphins. They are just great.</body></html>");
	say("You reading dolphins in http://localtest.com/test.html!");
	say("What is dolphins text?");
	get("There text everyone topics dolphins.");
	
	say("No there is dolphins.");
	file_put_contents($basePath."html/test.html","<html><body>Everyone topics dolphins. Dolphins are cool. They are just great.</body></html>");
	say("You reading dolphins in http://localtest.com/test.html!");
	say("What is dolphins text?");
	get("There text dolphins are cool.");
	
	say("No there is dolphins.");
	file_put_contents($basePath."html/test.html","<html><body>Everyone topics dolphins. Dolphins are cool. People and dolphins should be friends.</body></html>");
	say("You reading dolphins in http://localtest.com/test.html!");
	say("What is dolphins text?");
	get("There text people and dolphins should be friends.");

	//cleanup
	say("No there is http://localtest.com/test.html.");
	say("No there is dolphins.");
	say("My topics no dolphins.");
	say("No name dolphins.");

	//test complex auto-patterns
	$seabeings = '{dolphins whales} {live exist}'; 
	say_thing($seabeings);
	say("No there is '".$seabeings."'.");
	file_put_contents($basePath."html/test.html","<html><body>They tell dolphins are cool. I know that whales live in the sea. You know dolphins are mammals.</body></html>");
	say("You reading '".$seabeings."' in http://localtest.com/test.html!");
	say("What is '".$seabeings."' text?");
	get("There text i know that whales live in the sea.");
	say("What is '".$seabeings."' context, about?");
	get("There about in the sea, context i know that.");
	//cleanup	
	say("No there is http://localtest.com/test.html.");
	say_thing($seabeings,false);
	
	$seabeings = '{dolphins whales} {live exist} $somewhere';
	say_thing($seabeings);
	say("No there is '".$seabeings."'.");
	file_put_contents($basePath."html/test.html","<html><body>They tell dolphins are cool. I know that whales live in the sea. You know dolphins are mammals.</body></html>");
	say("You reading '".$seabeings."' in http://localtest.com/test.html!");
	say("What is '".$seabeings."' text?");
	get("There text whales live in the sea.");
	say("What is '".$seabeings."' somewhere?");
	get("There somewhere in the sea.");
	//cleanup
	say("No there is http://localtest.com/test.html.");
	say_thing($seabeings,false);

	
	//test classic patterns
	
	$pattern = "доллар сша руб. \$priceyesterday руб. \$pricetoday";
	$text = "24.05.2016 25.05.2016 . доллар сша $ руб. 67,0475 руб. ↑ 67,0493 . евро € руб. 75,2675 руб. ↓ 75,0349 . бивалютная корзина . с 25.05.2016 руб. 70,6428 .";
	say_thing($pattern);
	say("No there is '".$pattern."'.");
	say("You reading '".$pattern."' in '".$text."'!");
	say("What is '".$pattern."' text?");
	get("There text 'доллар сша руб . 67,0475 руб . ↑ 67,0493'.");
	say("What is '".$pattern."' priceyesterday, pricetoday?");
	get("There pricetoday ↑ 67,0493, priceyesterday 67,0475.");
	say("No there is '".$text."'.");
	say_thing($pattern,false);
	
	$pattern = "вспышка балла \$word";
	$text = "начало, мск максимум, мск окончание, мск . вспышка балла c1.3 2546 13:16:00 13:20:00 13:23:00 . . обновлено: 25 мая 2016";
	say_thing($pattern);
	say("No there is '".$pattern."'.");
	say("You reading '".$pattern."' in '".$text."'!");
	say("What is '".$pattern."' text?");
	get("There text вспышка балла c1.3.");
	say("What is '".$pattern."' word?");
	get("There word c1.3.");
	say("No there is '".$text."'.");
	say_thing($pattern,false);

	$weather = "москва \$number °c";
	$text = "город москва , россия пасмурно, ливень +16 °c +61 °f 1 м/с 2 миль/ч";
	say_thing($weather);
	say("No there is '".$weather."'.");
	say("You reading '".$weather."' in '".$text."'!");
	say("What is '".$weather."' text?");
	get("There text москва +16 °c.");
	say("What is '".$weather."' number?");
	get("There number +16.");
	say("No there is '".$text."'.");
	say_thing($weather,false);
	
	say("Path?");//sanity check
	get("No.");
	
	
	//test phrase boundaries
	$land = "\$x участка";
	$text = "объявлен аукцион участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "\$x участка";
	$text = "анонсирован прием посуды. объявлен аукцион участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "\$x [участка]";
	$text = "объявлен аукцион участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);

	$land = "\$x [участка]";
	$text = "анонсирован прием посуды. объявлен аукцион участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);

	
	$land = "\$x {участков участка}";
	$text = "анонсирован прием посуды. объявлен аукцион земельного участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион земельного участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "\$x [{участков участка}]";
	$text = "объявлен аукцион земельного участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион земельного участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "\$x [земельного {участков участка}]";
	$text = "объявлен аукцион земельного участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион земельного участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "\$x [{фамильного земельного} {участков участка}]";
	$text = "заявлена продажа большого участка. будет подажа фамильного поместья. объявлен аукцион земельного участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявлен аукцион земельного участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "\$x [{фамильного земельного} {поместья участка}]";
	$text = "заявлена продажа большого участка. будет подажа фамильного поместья и объявлен аукцион земельного участка.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text будет подажа фамильного поместья; text и объявлен аукцион земельного участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "[земельных участков]";
	$text = "Новосибирская певица. Против земельных участков.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text против земельных участков.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "[земельных участков]";
	$text = "Новосибирская певица Против земельных участков.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text новосибирская певица против земельных участков.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "{[земельный участок] [земельные участки] [земельного участка] [земельных участков]}";
	$text = "Новосибирская певица. Против земельных участков.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text против земельных участков.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "{[земельный участок] [земельные участки] [земельного участка] [земельных участков]}";
	$text = "Новосибирская певица Против земельных участков.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text новосибирская певица против земельных участков.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "{[{объявляется состоится объявляются состоятся} {аукцион торги}] [земельный участок] [земельные участки] [земельного участка] [земельных участков]}";
	$text = "Здесь - меню. Тут продается земельный участок с дачей. Тут - рыбу заворачивали. Там состоится аукцион недвижимости. Тут сидели. А здесь - земельные участки - даром. А тут - все.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text а здесь - земельные участки - даром; text там состоится аукцион недвижимости; text тут продается земельный участок с дачей.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	$land = "{[{объявляется состоится объявляются состоятся} {аукцион торги}] [земельный участок] [земельные участки] [земельного участка] [земельных участков]}";
	$text = "Здесь - меню. Тут продается земельный участок с дачей. Тут - рыбу заворачивали. Там состоится аукцион по продаже земельного участка. Тут сидели. А здесь - земельные участки - даром. А тут - все.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text а здесь - земельные участки - даром; text там состоится аукцион по продаже земельного участка; text тут продается земельный участок с дачей.");
	say("No there is '".$text."'.");
	say_thing($land,false);

	$land = "{продаются покупаются} {дома заводы земли параходы}";
	$text = "продаются дома. покупаются параходы. продаются заводы. покупаются земли.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text покупаются земли; text покупаются параходы; text продаются дома; text продаются заводы.");
	say("No there is '".$text."'.");
	say_thing($land,false);

	$land = "{продаются покупаются} {[земельные участки] [объекты недвижимости]}";
	$text = "продаются земельные участки. покупаются объекты недвижимости.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text покупаются объекты недвижимости; text продаются земельные участки.");
	say("No there is '".$text."'.");
	say_thing($land,false);

	$land = "{объявляется состоится объявляются состоятся} {аукцион торги} {[земельный участок] [земельного участка] [земельные участки] [земельных участков]}";
	$text = "Здесь - меню. Тут продается земельный участок с дачей. Тут - рыбу заворачивали. Там состоится аукцион по продаже земельного участка. Тут сидели. А здесь - земельные участки - даром. Объявляются торги на кучу земельных участков. А тут - все.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
	get("There text объявляются торги земельных участков; text там состоится аукцион земельного участка.");
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	//TODO: fix dependency on order in repeateables?
	$land = "{объявляется состоится объявляются состоятся} {аукцион торги} {[земельный участок] [земельные участки] [земельного участка] [земельных участков]}";
	$text = "Здесь - меню. Тут продается земельный участок с дачей. Тут - рыбу заворачивали. Там состоится аукцион по продаже земельного участка. Тут сидели. А здесь - земельные участки - даром. Объявляются торги на кучу земельных участков. А тут - все.";
	say_thing($land);
	say("You reading '".$land."' in '".$text."'!");
	say("What is '".$land."' text?");
//TODO:	
	//get("There text объявляются торги земельных участков; text там состоится аукцион земельного участка.");
	get();
	say("No there is '".$text."'.");
	say_thing($land,false);
	
	say("You forget!");
	get("Ok.");
	say("What your things count?");
	get("My things count ".($base_things_count).".");
	
	say("Your trusts no john.");
	get("Ok.");
	say("No email john@doe.com.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("My logout.");	
	get("Ok.");
}


test_init();
test_agent_patterns();
test_summary();

?>
