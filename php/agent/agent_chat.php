<?php
/*
 * MIT License
 * 
 * Copyright (c) 2014-2019 by Anton Kolonin, Aigents
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

function test_chat_cleanup() {
	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	say("You forget!");
	get("Ok.");
	say("My logout.");
	get("Ok.");
}

function test_chat() {
	global $version;
	global $copyright;
	
	//TODO: free text interactions on news
	//any news?
	//what's new?
	//help!
	
	//TODO: on greeting (hi, hello, привет), use default prompt before asking question
	//TODO: on any unrecogized input, try default prompt before asking question back
	//TODO: have smart greeting - using pattern->response mechanics
	//TODO: translation
	
	//TODO: free ontology operations
	//TODO: free text question answering using graphs
	//TODO: what else?
	
	//TODO: commands
	/*
				help
				news
				sites
				topics
				search ...
				hi hello привет здорово
				bye logout пока [до свидания]
	*/
	//TODO: trainable pattern-based conversations
	say("Login.");
	get("What your email, name, surname?");
	say("john@doe.org");
	get("What your name, surname?");
	say("john");
	get("What your surname?");
	say("doe");
	get("What your secret question, secret answer?");
	say("password 123456querty");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	// check that patterns-responses ARE NOT working
	say("hi");
	get("No thing.");
	say("What is hi?");
	get("There not.");
	say("What hi is?");
	get("Hi not.");
	say("What hi name?");
	get("Hi not.");
	say("What hi?");
	get("Hi not.");
	say("What help?");
	get("Help not.");
	say("Whatsup");
	get("No thing.");
	say("привет");
	get("No thing.");
	// check that patterns-responses ARE working
	// 1st, create patterns
	say("There patterns hi, hello, whatsup, greeting; responses 'hi!', 'hello!', 'whatsup?', 'greeting!'.");
	get("Ok.");
	say("What patterns hi?");
	get("There patterns greeting, hello, hi, whatsup, responses 'greeting!', 'hello!', 'hi!', 'whatsup?'.");
	say("What patterns hi?");
	say("There patterns привет, здорово, здравствуй; responses 'привет!', 'здорово!', 'здравствуй!'.");
	get("Ok.");
	say("What patterns здорово?");
	get("There patterns здорово, здравствуй, привет, responses 'здорово!', 'здравствуй!', 'привет!'.");
	say("There patterns 'how are you', 'howdy', 'how do you do'; responses 'i am great', 'i am fine', 'i am ok'.");
	get("Ok.");
	say("What patterns 'how do you do'?");
	get("There patterns how are you, how do you do, howdy, responses i am fine, i am great, i am ok.");
	say("There patterns news, \"{[what is new][what's new]}\"; responses 'nothing new'.");
	get("Ok.");
	say("What patterns news?");
	get("There patterns \"{[what is new][what's new]}\", news, responses nothing new.");
//TODO: "multiple" attribute with single value is not parsed properly!!!
//say("There patterns help; responses 'See examples at https://aigents.com/test/aigents_turing_test.html'.");
	say("There patterns help, support; responses 'See examples at https://aigents.com/test/aigents_turing_test.html'.");
	get("Ok.");
	say("What patterns help?");
	get("There patterns help, support, responses See examples at https://aigents.com/test/aigents_turing_test.html.");
	// 2nd, check the patterns
	say("Whatsup");
	get("Greeting!",array("Hi!","Hello!","Whatsup?"));
	say("   hi   ");
	get("Greeting!",array("Hi!","Hello!","Whatsup?"));
	say("how do you do");
	get("I am ok.",array("I am fine.","I am great."));
	say("Привет");
	get("Здорово!",array("Привет!","Здравствуй!"));
	say("  \n help!  \n ");
	get("See examples at https://aigents.com/test/aigents_turing_test.html.");
	say("any news?");
	get("Nothing new.");
	say("what's new?");
	get("Nothing new.");
	//TODO: better handling?
	say("Whatsup?");
	get("Whatsup name whatsup.");
	//cleanup patterns-responses
	say("No there patterns news.");
	get("Ok.");
	say("No there patterns help.");
	get("Ok.");
	say("No there patterns hi.");
	get("Ok.");
	say("No there patterns здорово.");
	get("Ok.");
	say("No there patterns howdy.");
	get("Ok.");
	say("What patterns news?");
	get("There not.");
	say("What patterns help?");
	get("There not.");
	say("What patterns hi?");
	get("There not.");
	say("What patterns здорово?");
	get("There not.");
	say("What patterns howdy?");
	get("There not.");
	say("You forget.");
	get("Ok.");
	// check that patterns-responses ARE NOT working
	say("hi");
	get("No thing.");
	say("What is hi?");
	get("There not.");
	say("What hi is?");
	get("Hi not.");
	say("What hi name?");
	get("Hi not.");
	say("What hi?");
	get("Hi not.");
	say("What help?");
	get("Help not.");
	say("Whatsup");
	get("No thing.");
	say("привет");
	get("No thing.");	
	//cleanup
	test_chat_cleanup();
		
	//classic loging flow for GUI App
	say("Login.");
	get("What your email, name, surname?");
	say("john@doe.org");
	get("What your name, surname?");
	say("john");
	get("What your surname?");
	say("doe");
	get("What your secret question, secret answer?");
	say("My secret question password.");
	get("What your secret answer?");
	say("My secret answer 123456querty.");
	get("What your password?");
	say("My password 123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	test_chat_cleanup();
	
	//freetext registration with mixed content
	say("hi");
	get("What your email, name, surname?");
	say("secret");
	get("What your email, name, surname?");
	say("john@doe.org\njohn, doe");
	get("What your secret question, secret answer?");
	say("password\n\r\r\n123456querty");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	//testing email takeover attempt
	say("what your name, email?");
	get("My email '', name aigents.");
	say("your email john@doe.org.");
	get("Email john@doe.org is owned.");
	say("what your email?");
	get("My email ''.");
	say("My logout");
	get("Ok.");
	//testing surname-less login
	say("hi");
	get("What your email, name, surname?");
	say("john@doe.org,john");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	TODO:say("   \n   logout    \n   ");
	get("Ok.");
	say("I am back");
	get("What your email, name, surname?");
	say("\n    \r     john@doe.org");	
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("bye");
	get("Ok.");
	say("  \n  hello   \n   ");
	get("What your email, name, surname?");
	say("      john@doe.org      ,        john     ");
	get("What your password?");
	say("   \n    \r   123456querty   \n   \r   ");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("\t\n\r     \r\n\tbye\t\n\r    \r\n\t");
	get("Ok.");
	say("\t\n\r     \r\n\thi\t\n\r    \r\n\t");
	get("What your email, name, surname?");
	say("my email john@doe.org, password 123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	test_chat_cleanup();

	//freetext registration with no delimiters and email only
	say("My login.");
	get("What your email, name, surname?");
	say("secret");
	get("What your email, name, surname?");
	say("john@doe.org");
	get("What your name, surname?");
	say("Top secret");
	get("What your secret question, secret answer?");
	say("password 123456querty");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello Top Secret!\nMy Aigents ".$version.$copyright);
	say("my email, name?");
	get("Your email john@doe.org, name top.");
	say("my name john");
	get("Ok.");
	test_chat_cleanup();
	
	//freetext registration with delimiters w/o whitespaces
	say("login");
	get("What your email, name, surname?");
	say("john@doe.org,john,doe");
	get("What your secret question, secret answer?");
	say("password,123456querty");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);	
	say("My logout");
	get("Ok.");
	say("hi");
	get("What your email, name, surname?");
	say("john@doe.org");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	test_chat_cleanup();
	
	//freetext registration with delimiters with whitespaces
	say("login");
	get("What your email, name, surname?");
	say("john@doe.org, john, doe");
	get("What your secret question, secret answer?");
	say("password, 123456querty");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	test_chat_cleanup();
	
	//freetext registration with no delimiters  
	say("My login.");
	get("What your email, name, surname?");
	say("john@doe.org john doe");
	get("What your secret question, secret answer?");
	say("password 123456querty");
	get("What your password?");
	say("123456querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	test_chat_cleanup();

}

function test_search() {
	login();
	
	//search in LTM graph
	say("You forget everything!");
	get("Ok.");
	
	say("search whatever");
	get("Not.");
	
	say("search products in http://localtest.com/sitea/products.html");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("search products, period 0");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("No there times today.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("What times today?");
	get("There not.");
	say("www id products graph date today, period 0");
	get("products worded http://localtest.com/sitea/personal.html 100.\nproducts worded http://localtest.com/sitea/index.html 100.\nproducts worded http://localtest.com/sitea/corporate.html 100.");
	say("www id make graph date today, period 0");
	get("make worded http://localtest.com/sitea/personal.html 100.\nmake worded http://localtest.com/sitea/corporate.html 100.\nmake worded http://localtest.com/sitea/mission.html 100.");
	say("Search products, period 0");
	get("Sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/index.html, text about us products info contact us; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search products make \$x, period 0");
	get("Sources http://localtest.com/sitea/corporate.html, text products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text products make people happier.");
	say("What times today?");
	get("There not.");
	say("Search people");
	get("Sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search make people happier");
	get("Sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search make people happy");
	get("Not.");
	say("Search 'make people {happy happier}'");
	get("Sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search make people {happy happier}");
	get("Sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");

	//test seach in file/URL
	say("SEARCH 'temperature is \$number' IN http://localtest.com/test/Test.pdf");
	get("There sources 'http://localtest.com/test/Test.pdf', text temperature is 22.");
	say("No there times today.");
	get("Ok.");
	say("Search temperature Url 'http://localtest.com/test/Test.pdf'");
	get("There sources 'http://localtest.com/test/Test.pdf', text the outside temperature is 22 c°.");
	say("No there times today.");
	get("Ok.");
	say("what is temperature");
	get("There not.");
	say("search temperature in http://localtest.com/test/");
	get("There sources http://localtest.com/test/, text the outside temperature is 22 c°.");
	say("what is temperature");
	get("There about is 22 c°, context the outside, is temperature, sources http://localtest.com/test/, text the outside temperature is 22 c°, times today.");
	say("what is test");
	get("There not.");
	say("search test in http://localtest.com/test/Test.pdf");
	get("There sources 'http://localtest.com/test/Test.pdf', text this is a test page.");
	say("what is test");
	get("There about page, context this is a, is test, sources 'http://localtest.com/test/Test.pdf', text this is a test page, times today.");

	say("search products in http://localtest.com/sitea/, range 0, limit 1");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("what is products text");
	get("There text about us products info contact us.");
	say("No there is products.");
	get("Ok.");
	say("what is products text");
	get("There not.");
	say("search products in http://localtest.com/sitea/, range 2, limit 1");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("what is products text");
	get("There text about us products info contact us.");
	say("No there is products.");
	say("search products in http://localtest.com/sitea/, range 2, limit 10");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("what is products text");
	get("There text about us products info contact us; text our products make corporations more profitable; text our products make people happier.");
	say("No there is products.");
	say("search products in http://localtest.com/sitea/, range 1, limit 10");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("what is products text");
	get("There text about us products info contact us.");
	say("search products in http://localtest.com/siteb/");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
	say("search products in http://localtest.com/sitea/");
	get("Not.");
	say("search products in http://localtest.com/siteb/");
	get("Not.");
	
	//test search in STM
	say("what is products, times today text, sources");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
	say("search products");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
	say("search our products");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/siteb/, text our products contact information about our company.");
	say("search 'products make'");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	
	say("No there times today.");
	get();
	say("You forget!");
	get();
	logout();
}


function test_apis() {
}


test_init();
test_search();
test_chat();
test_apis();
test_summary();

?>
