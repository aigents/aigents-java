<?php
/*
Copyright 2018 Anton Kolonin, Aigents Group

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

include_once("test_api.php");

function init(){
	global $version;
	global $copyright;
	
	//login, registration, verification
	say("My name john, email john@doe.org, surname doe.");
	get("What your secret question, secret answer?");
	say("My secret question q, secret answer a.");
	get("What your q?");
	say("My q a.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
}

function cleanup(){
	//cleanup and logout
	say("Times today trust false, new false, trust false.");
	get();
	say("No there times today.");
	get();
	say("You forget.");
	get();
	
	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
}

function test_agent_cluster() {
	init();
	
	/*
	H: My area of interest texts "тунец это рыба", "кошка это млекопитающее", "Петя работает программистом", "Маша работает бухгалтером".
	A: Ok.
	H: You cluster area of interest!
	A: Ok.
	H: What my area of interest categories?
	A: Your area of interest categories "это", "работает".
	H: What "это" texts?
	A: "Это" texts  "тунец это рыба", "кошка это млекопитающее".
	H: What "работает" texts?
	A: "Работает" texts  "Петя работает программистом", "Маша работает бухгалтером".
	H: What "работает" patterns?
	A: "Работает" patterns "{Маша Петя} работает {программистом бухгалтером}".
	H: What "'это" patterns?
	A: "Работает" patterns "{кошка тунец} это {млекопитающее рыба}".
	H: My area of interest texts "паук это насекомое", "Боря работает фандрайзером".
	A: Ok.
	H: You classify area of interest!
	A: Ok.
	H: What "паук это насекомое" categories?
	A: "Паук это насекомое" categories "это".
	H: What "Боря работает фандрайзером" categories?
	A: "Боря работает фандрайзером" categories "работает".	
	*/
	
	file_put_contents("html/test/cat/fly.html","<html><body>fly is an insect</body></html>");
	file_put_contents("html/test/cat/eagle.html","<html><body>eagle is a bird</body></html>");
	file_put_contents("html/test/cat/snake.html","<html><body>snake is a reptile</body></html>");
	file_put_contents("html/test/cat/french.html","<html><body>french live in france</body></html>");
	file_put_contents("html/test/cat/chinese.html","<html><body>chinese live in china</body></html>");
	say("There new true, text 'http://localtest.com/test/cat/fly.html', times today, trust true.");
	say("There new true, text 'http://localtest.com/test/cat/eagle.html', times today, trust true.");
	say("There new true, text 'http://localtest.com/test/cat/snake.html', times today, trust true.");
	say("There new true, text 'tuna is a fish', times today, trust true.");
	say("There new true, text 'cat is a mammal', times today, trust true.");
	say("There new true, text 'http://localtest.com/test/cat/french.html', times today, trust true.");
	say("There new true, text 'http://localtest.com/test/cat/chinese.html', times today, trust true.");
	say("There new true, text 'germans live in germany', times today, trust true.");
	say("There new true, text 'russians live in russia', times today, trust true.");
	say("There new true, text 'spaniards live in spain', times today, trust true.");

	say("You cluster!");
	get();
	
	//TODO: make it working right!?
	say("What 'is' patterns?");
	get();
	say("What '{in live}' patterns?");
	get();
				
	say("What new true text?");
	get("There text cat is a mammal; text germans live in germany; text http://localtest.com/test/cat/chinese.html; text http://localtest.com/test/cat/eagle.html; text http://localtest.com/test/cat/fly.html; text http://localtest.com/test/cat/french.html; text http://localtest.com/test/cat/snake.html; text russians live in russia; text spaniards live in spain; text tuna is a fish.");
	
	say("What text 'http://localtest.com/test/cat/french.html'?");
	get();
	
	say("What text 'http://localtest.com/test/cat/french.html' categories?");
	get();
	
	say("What 'live in' texts?");
	get();

	say("What 'live in' patterns?");
	get();
	
	//TODO: use SAL instead of HAL?
	//say("My trusts 'cat is mammal', times today.");
	//get("Ok.");
	
	//Check JSON API
	say("You cluster format json texts '[\"http://localtest.com/test/cat/fly.html\",\"http://localtest.com/test/cat/eagle.html\",\"http://localtest.com/test/cat/snake.html\",\"tuna is a fish\",\"cat is a mammal\",\"http://localtest.com/test/cat/french.html\",\"http://localtest.com/test/cat/chinese.html\",\"germans live in germany\",\"russians live in russia\",\"spaniards live in spain\"]'.");
	//get("{\"categories\":[[\"in\",\"live\"],[\"is\"]],\"category_documents\":{\"is\":{\"http://localtest.com/test/cat/snake.html\":\"1\",\"http://localtest.com/test/cat/fly.html\":\"1\",\"cat is a mammal\":\"1\",\"http://localtest.com/test/cat/eagle.html\":\"1\",\"tuna is a fish\":\"1\"},\"in, live\":{\"http://localtest.com/test/cat/chinese.html\":\"2\",\"spaniards live in spain\":\"2\",\"http://localtest.com/test/cat/french.html\":\"2\",\"russians live in russia\":\"2\",\"germans live in germany\":\"2\"}},\"category_features\":{\"in, live\":{\"russians\":\"2\",\"russia\":\"2\",\"germans\":\"2\",\"spaniards\":\"2\",\"germany\":\"2\",\"france\":\"2\",\"french\":\"2\",\"in\":\"10\",\"spain\":\"2\",\"chinese\":\"2\",\"live\":\"10\",\"china\":\"2\"},\"is\":{\"tuna\":\"1\",\"bird\":\"1\",\"snake\":\"1\",\"cat\":\"1\",\"is\":\"5\",\"eagle\":\"1\",\"fly\":\"1\",\"fish\":\"1\",\"an\":\"1\",\"insect\":\"1\",\"reptile\":\"1\",\"mammal\":\"1\"}}}");
	get("{\"categories\":[[\"in\",\"live\"],[\"is\"]],\"category_documents\":{\"in, live\":{\"germans live in germany\":\"2\",\"http://localtest.com/test/cat/chinese.html\":\"2\",\"http://localtest.com/test/cat/french.html\":\"2\",\"russians live in russia\":\"2\",\"spaniards live in spain\":\"2\"},\"is\":{\"cat is a mammal\":\"1\",\"http://localtest.com/test/cat/eagle.html\":\"1\",\"http://localtest.com/test/cat/fly.html\":\"1\",\"http://localtest.com/test/cat/snake.html\":\"1\",\"tuna is a fish\":\"1\"}},\"category_features\":{\"in, live\":{\"china\":\"2\",\"chinese\":\"2\",\"france\":\"2\",\"french\":\"2\",\"germans\":\"2\",\"germany\":\"2\",\"in\":\"10\",\"live\":\"10\",\"russia\":\"2\",\"russians\":\"2\",\"spain\":\"2\",\"spaniards\":\"2\"},\"is\":{\"an\":\"1\",\"bird\":\"1\",\"cat\":\"1\",\"eagle\":\"1\",\"fish\":\"1\",\"fly\":\"1\",\"insect\":\"1\",\"is\":\"5\",\"mammal\":\"1\",\"reptile\":\"1\",\"snake\":\"1\",\"tuna\":\"1\"}}}");
	
	cleanup();
}

function test_agent_cat() {
	init();
	
	//TODO:
	//test
	//classifier
	//hook-based parser architecture?
	//emoticon-aware parser
	
	say("There text 'I like it :-)', is good.");
	get("Ok.");
	say("There text 'I dislike it :-(' is bad.");
	get("Ok.");
	say("There text 'I like :-) and dislike :-( it' is bad, good.");
	get("Ok.");
	say("Good is quality.");
	get("Ok.");
	say("Bad is quality.");
	get("Ok.");
	say("What text 'I like :-) and dislike :-( it' is?");
	get("There is bad, good.");
	say("What text 'I dislike it :-(' is?");
	get("There is bad.");
	say("What text 'I like it :-)' is?");
	get("There is good.");
	say("What is bad text?");
	get("There text 'I dislike it :-('; text 'I like :-) and dislike :-( it'.");
	say("What is good text?");
	get("There text 'I like :-) and dislike :-( it'; text 'I like it :-)'.");
	say("What is good or bad text?");
	get("There text 'I dislike it :-('; text 'I like :-) and dislike :-( it'; text 'I like it :-)'.");
	
	//TODO: fix
	//say("What is good and bad text?");
	//Int:[[is (good bad)] text]?
	//I:09d18046587b4814acd172a9b55e52f9:There not.
	//get("There text 'I like :-) and dislike :-( it'.");

	//TODO:
	say("You learn good!");//learn all patterns of text with good
	get("Ok.");
	say("You learn bad!");//learn all patterns of text with bad
	get("Ok.");
	
	say("What bad patterns?");
	get();//get("Bad patterns dislike, ':-('.");
	say("What good patterns?");
	get();//get("Good patterns like, ':-)'.");
	
	say("You parse text 'aaaaa ping BBBB PONG \"123\" XXyyZx'!");
	get("There text 'aaaaa ping BBBB PONG \"123\" XXyyZx', tokens [aaaaa ping bbbb pong 123 xxyyzx], grams [ping pong].");
	say("You parse text 'there is a tree'!");
	get("There text there is a tree, tokens [there is a tree], grams ([there is] [is a] [a tree]).");
	say("You parse distance 2, text 'there is a tree'!");
	get("There text there is a tree, tokens [there is a tree], grams ([there is] [there a] [is a] [is tree] [a tree]).");
	
	/*
	
	Cat is animal, dog is animal. Cat, dog is animal.
	Animals are dog, cat.
	Goods 
	
	TODO: something like this...
	say("What text 'They like me' is?");
	get("There not.");
	say("What text 'They like me' goodness?");
	get("There not.");
	say("Tou think 'They like me', good.");
	get("Ok.");
	say("What text 'They like me' is?");
	get("There is good.");
	say("What text 'They like me' goodness?");
	get("There goodness 0.95.");
	
	TODO:
	<term> := <assessment>? <negation>? ( <anonymous>? | <self> | <peer> | <id> | <name> | <value> | <qualifier> )
	<negation> := '~' | ('not' | 'no') | ('нет' | 'не')
	<assessment> := <literal-assessment> | <percent-assessment> | <boolean-assessment>
	<boolean-assessment> := ('true' | 'yes' | 'да' ) | ('false' | 'no' | 'нет')
	<percent-assessment> := (0..100)'%'
	<literal-assessment> := (('surely' | 'probably' | 'possibly') | ('точно' | 'вероятно' | 'возможно'))?

	100%-80% = точно = surely (по умолчанию, можно не указывать)
	60%-80% = вероятно = probably
	50%-60% = возможно = possibly

        жизнь это точно хорошо.
        еда это вероятно хорошо.
        вода это возможно хорошо.
        вода это возможно не хорошо.
        боль это вероятно не хорошо.
        мор это точно не хорошо.

        life is surely good.
        food is probably good.
        water is possibly good.
        water is possibly not good.
        pain is probably not good.
        death is surely not good.

        life is 98% good.
        food is 65% good.
        water is 50% good.
        water is 50% not good.
        pain is 38% good.
        death is 1% good.
        pain is 62% not good.
        death is 99% not good.

		my trusts <x>. //in context of i/my
		<x> trust true.
		<x> trust 99%.
		<x> is trust.
		<x> is 99% trust.
		trusts are <x>.

		my relevances <x>. //in context of i/my
		<x> relevance true.
		<x> relevance 99%.
		<x> is relevance.
		<x> is 99% relevance.
		relevances are <x>. 
		
	TODO:
	1) emotions explicit
	1-1) parse sentiment (smiles) from news and explicitly set goodness/badness (lowest=false/low/medium/high/highest=true) is (good/bad)
	1-2) AL: you parse <text>, format <format>, language <language>, features <words|phrases|emoticons> 
	1-3) extract Facebooks emotions and explicitly set goodness/badness
	1-4) render emotions in reports
	1-5) display emotions in graphs
	1-6) display emotions in lists
	2) communicate emotions
	2-1) true/false => false/low/medium/high/highest
	3) infer emotions	
	3-1) parse texts for bigrams with max distance 1 (no hops and no distance-based evidence weighting)
	3-2) use vocabulary (en/ru) for "controlled" featuress
	3-3) use bigram features only first ("priority on order") and only then the rests (so 'not good' takes over 'good')
	3-4) boolean ranking (count on feature number, no inference)
	3-5) use only features uniquely identifying category in a domain (Cf = 1), if present (if not, then relax to Cf=2, etc.)
	3-6) train on Julia-s dataset
	4) communicate inference
	4-1) 'You think!'
	4-2) 'You think life, is!'
	4-3) 'You think life, good, bad!'
	4-4) 'You think life, is, good, bad!'
	4-5) Implicit inference - infer only of no answer on query!?
	*/
	
			
	
	
	//brk();

	say("No there is good.");
	get("Ok.");
	say("No there is bad.");
	get("Ok.");
	say("No name good.");
	get("Ok.");
	say("No name bad.");
	get("Ok.");
	say("No name quality.");
	get("Ok.");
	
	cleanup();
}

test_init();
test_agent_cluster();
test_agent_cat();
printtimers();
test_summary();

?>