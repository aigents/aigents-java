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

function test_agent_think() {
	global $basePath;
	global $version;
	global $copyright;
	global $timeout;
	global $base_things_count;
	
	//register 1st user
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com, name john, surname doe, secret question x, secret answer y.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

	//cleanup STM+LTM
	say("You forget everything!");
	get("Ok.");
	say("Your things count?");
	get("My things count ".($base_things_count).".");
	say("What times today text?");
	get("No.");
	
	say("My logout.");

	//register 2nd user
	say("My login.");
	say("My email ali@baba.com, name ali, surname baba, secret question x, secret answer y.");
	say("My x y.");
	say_thing("there is \$something.");
	say_site("http://localtest.com/think.html");
	say("My logout.");
	
	//register 3rd user
	say("My login.");
	say("My email doe@john.com, name doe, surname john, secret question x, secret answer y.");
	say("My x y.");
	say_thing("there is \$something.");
	say_site("http://localtest.com/think.html");
	say("My logout.");
	
	//login 1st user
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	
	say_thing("there is \$something.");
	say_site("http://localtest.com/think.html");
	file_put_contents($basePath."html/think.html","there is fried banana. there is bird on the tree. there is fancy stuff.");
	say("You reading!");
	sleep($timeout);
	say("what is http://localtest.com/think.html");
	get("There is http://localtest.com/think.html, text 'there is fried banana. there is bird on the tree. there is fancy stuff. ', times today.");
	say("What times today, is 'there is \$something.' text?");
	get("There text 'there is bird on the tree .'; text 'there is fancy stuff .'; text 'there is fried banana .'.");
	say("Times today, is 'there is \$something.' trust true.");
	say("Times today new false.");
	
	say_thing("there is \$something.");
	say_site("http://localtest.com/think.html");
	file_put_contents($basePath."html/think.html","there is banana tree. there is mocking bird. there is life jacket. there is life style.");
	say("You reading site http://localtest.com/think.html!");
	say("What times today, new true, is 'there is \$something.' text?");
	get("There text 'there is banana tree .'; text 'there is life jacket .'; text 'there is life style .'; text 'there is mocking bird .'.");

	say("You think!");
	get("Ok.");
	say("What times today, new true, is 'there is \$something.' text, relevance?");
	get("There relevance 0, text 'there is life jacket .'; relevance 0, text 'there is life style .'; relevance 100, text 'there is banana tree .'; relevance 50, text 'there is mocking bird .'.");

	say("Text 'there is life jacket .' trust true.");
	say("What times today, new true, is 'there is \$something.' text, relevance?");
	get("There relevance 0, text 'there is life jacket .'; relevance 0, text 'there is life style .'; relevance 100, text 'there is banana tree .'; relevance 50, text 'there is mocking bird .'.");
	say("You think!");
	get("Ok.");
	say("What times today, new true, is 'there is \$something.' text, relevance?");
	get("There relevance 100, text 'there is banana tree .'; relevance 100, text 'there is life jacket .'; relevance 50, text 'there is life style .'; relevance 50, text 'there is mocking bird .'.");
	
	file_put_contents($basePath."html/think.html","there is small pig. there is large ball. there is happy life. there is life vest. there is long life. there is angry bird. there is bird nest. there is bird egg. there is flying bird. there is bird cage.");
	say("You reading site http://localtest.com/think.html!");
	say("What times today, new true, is 'there is \$something.' text?");
	//expect everything what have been read today
	get("There text 'there is angry bird .'; text 'there is banana tree .'; text 'there is bird cage .'; text 'there is bird egg .'; text 'there is bird nest .'; text 'there is flying bird .'; text 'there is happy life .'; text 'there is large ball .'; text 'there is life jacket .'; text 'there is life style .'; text 'there is life vest .'; text 'there is long life .'; text 'there is mocking bird .'; text 'there is small pig .'.");
	say("You think!");
	//expect only the top items
	say("What times today, new true, is 'there is \$something.' text?");
	get("There text 'there is angry bird .'; text 'there is banana tree .'; text 'there is bird cage .'; text 'there is bird egg .'; text 'there is bird nest .'; text 'there is flying bird .'; text 'there is happy life .'; text 'there is life jacket .'; text 'there is life style .'; text 'there is life vest .'; text 'there is long life .'; text 'there is mocking bird .'.");

	//logout 1st user
	say("My logout.");
	
	//login 2nd user and set trust to 1 item
	say("My login.");
	say("My email ali@baba.com.");
	say("My x y.");
	say("Times today, text 'there is bird on the tree .' trust true.");
	say("My logout.");
	
	//login 3rd user	
	say("My login.");
	say("My email doe@john.com.");
	say("My x y.");
	//check explicit social relevance relevance missed
	say("You think!");
	say("What times today, new true, is 'there is \$something.' text, social relevance?");
	get("There text 'there is angry bird .'; text 'there is banana tree .'; text 'there is bird cage .'; text 'there is bird egg .'; text 'there is bird nest .'; text 'there is bird on the tree .'; text 'there is fancy stuff .'; text 'there is flying bird .'; text 'there is fried banana .'; text 'there is happy life .'; text 'there is large ball .'; text 'there is life jacket .'; text 'there is life style .'; text 'there is life vest .'; text 'there is long life .'; text 'there is mocking bird .'; text 'there is small pig .'.");
	say("My trusts john, friends john.");//trust on user
	say("You think!");
	//check explicit social relevance relevance appeared
	say("What times today, new true, is 'there is \$something.' text, social relevance?");
	get("There social relevance 100, text 'there is bird on the tree .'; social relevance 100, text 'there is fancy stuff .'; social relevance 100, text 'there is fried banana .'; social relevance 100, text 'there is life jacket .'; text 'there is angry bird .'; text 'there is banana tree .'; text 'there is bird cage .'; text 'there is bird egg .'; text 'there is bird nest .'; text 'there is flying bird .'; text 'there is happy life .'; text 'there is large ball .'; text 'there is life style .'; text 'there is life vest .'; text 'there is long life .'; text 'there is mocking bird .'; text 'there is small pig .'.");
	say("My trusts ali, friends ali.");//trust on user
	say("You think!");
	//check explicit social relevance relevance updated
	say("What times today, new true, is 'there is \$something.' text, social relevance?");
	get("There social relevance 100, text 'there is bird on the tree .'; social relevance 50, text 'there is fancy stuff .'; social relevance 50, text 'there is fried banana .'; social relevance 50, text 'there is life jacket .'; text 'there is angry bird .'; text 'there is banana tree .'; text 'there is bird cage .'; text 'there is bird egg .'; text 'there is bird nest .'; text 'there is flying bird .'; text 'there is happy life .'; text 'there is large ball .'; text 'there is life style .'; text 'there is life vest .'; text 'there is long life .'; text 'there is mocking bird .'; text 'there is small pig .'.");
	say("My trusts no john, no ali, friends no john, no ali.");//trust on user
	say("You think!");
	//check explicit social relevance relevance disappeared
	say("What times today, new true, is 'there is \$something.' text, social relevance?");
	get("There text 'there is angry bird .'; text 'there is banana tree .'; text 'there is bird cage .'; text 'there is bird egg .'; text 'there is bird nest .'; text 'there is bird on the tree .'; text 'there is fancy stuff .'; text 'there is flying bird .'; text 'there is fried banana .'; text 'there is happy life .'; text 'there is large ball .'; text 'there is life jacket .'; text 'there is life style .'; text 'there is life vest .'; text 'there is long life .'; text 'there is mocking bird .'; text 'there is small pig .'.");

	//update content with new items
	say("New true new false.");
	
	file_put_contents($basePath."html/think.html","there is bird in the sky. there is banana on the plate. there is pen on the table.");
	say("You reading site http://localtest.com/think.html!");
	
	//check implicit social relevance relevance missed
	say("You think!");
	say("What times today, new true, is 'there is \$something.' text, social relevance?");
	get();
	say("My trusts john, friends john.");//trust on user
	say("You think!");
	//check implicit social relevance relevance appeared
	say("What times today, new true, is 'there is \$something.' text, social relevance?");
	get("There social relevance 0, text 'there is pen on the table .'; social relevance 100, text 'there is banana on the plate .'; social relevance 100, text 'there is bird in the sky .'.");
	say("My trusts ali, friends ali.");//trust on user
	say("You think!");
	//check implicit social relevance relevance updated
	say("What times today, new true, is 'there is \$something.' text, social relevance?");
	get("There social relevance 0, text 'there is pen on the table .'; social relevance 100, text 'there is bird in the sky .'; social relevance 50, text 'there is banana on the plate .'.");
	
	//cleanup 3rd user
	say("My trusts no john, no ali, friends no john, no ali.");
	del_news_today();
	say_thing("bird \$something.",false);
	say_thing("there is \$something.",false);
	say_site("http://localtest.com/think.html",false);
	//logout 3rd user
	say("My logout.");
	
	//cleanup 2nd user
	say("My login.");
	say("My email ali@baba.com.");
	say("My x y.");
	del_news_today();
	say_thing("there is \$something.",false);
	say_site("http://localtest.com/think.html",false);
	say("My logout.");
	
	//cleanup 1st user
	say("My login.");
	say("My email john@doe.com.");
	say("My x y.");
	del_news_today();
	say("What times today?");
	get("No.");
	say_thing("there is \$something.",false);
	say_site("http://localtest.com/think.html",false);
	say("No email doe@john.com.");
	get("Ok.");
	say("No email ali@baba.com.");
	get("Ok.");
	say("You forget everything!");
	get("Ok.");
	say("Your things count?");
	get("My things count ".($base_things_count).".");
	say("Your trusts no john.");
	get("Ok.");
	say("No email john@doe.com.");
	get("Ok.");
	say("You forget everything!");
	get("Ok.");
	say("My logout.");	
	get("Ok.");
}


function test_agent_think_ex() {
	global $basePath;
	global $version;
	global $copyright;
	global $timeout;
	global $base_things_count;
	
	//register 1st user
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com, name john, surname doe, secret question x, secret answer y.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

	say_thing("there is \$something.");
	say_thing("here is \$something.");
	say_site("http://localtest.com/think.html");
	file_put_contents($basePath."html/think.html",
		"there is bird in the sky. there is bird on the tree. \n" .
		"there is fried banana. there is fancy stuff.\n" .
		"there is banana tree. there is mocking bird. there is life jacket. there is life style.\n" .
		"here is bird in the sky. here is bird on the tree. \n" .
		"here is small pig. here is large ball. here is happy life. here is life vest. here is long life. here is angry bird. here is bird nest. here is bird egg. here is flying bird. here is bird cage.\n" .
		"here is banana on the plate. here is pen on the table.");
	say("You reading!");
	sleep($timeout);

	say("text 'there is bird on the tree .' trust true.");
	get("Ok.");
	say("text 'here is bird in the sky .' trust true.");
	get("Ok.");
	say("You think!");
	get("Ok.");
	say("What new true text, relevance?");
	get();
	
	say("my news limit 4.");
	get("Ok.");
	say("You think!");
	get("Ok.");
	say("What new true text, relevance?");
	get("There relevance 100, text 'here is bird in the sky .'; relevance 100, text 'here is bird on the tree .'; relevance 100, text 'there is bird in the sky .'; relevance 100, text 'there is bird on the tree .'; relevance 50, text 'here is angry bird .'; relevance 50, text 'here is bird cage .'; relevance 50, text 'here is bird egg .'; relevance 50, text 'here is bird nest .'; relevance 50, text 'here is flying bird .'; relevance 50, text 'there is banana tree .'; relevance 50, text 'there is mocking bird .'.");
	
	say("my news limit 2.");
	get("Ok.");
	say("You think!");
	get("Ok.");
	say("What new true text, relevance?");
	get("There relevance 100, text 'here is bird in the sky .'; relevance 100, text 'here is bird on the tree .'; relevance 100, text 'there is bird in the sky .'; relevance 100, text 'there is bird on the tree .'.");

	//cleanup 1st user
	say("My login.");
	say("My email john@doe.com.");
	say("My x y.");
	del_news_today();
	say("What times today?");
	get("No.");
	say_thing("there is \$something.",false);
	say_thing("here is \$something.",false);
	say_site("http://localtest.com/think.html",false);
	say("Your trusts no john.");
	get("Ok.");
	say("No email john@doe.com.");
	get("Ok.");
	say("You forget everything!");
	get("Ok.");
	say("My logout.");
	get("Ok.");
}

test_init();
test_agent_think();
test_agent_think_ex();
test_summary();

?>
