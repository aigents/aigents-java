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

function test_chat_init() {
}

function test_chat_cleanup() {
	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	say("You forget!");
	get("Ok.");
	say("My logout.");
	get("Ok.");
}

function test_help() {
	login();
	
	//define help content
	say("There trust true, patterns help, support; responses 'Type \"help login\", \"help logout\", \"help search\", \"help topics\", \"help sites\", \"help news\", \"help notification\".'.");
	get("Ok.");
	//TODO:why this is not parsed for patterns properly!!??
	//say("There trust true, patterns 'help topics'; responses 'Type \"my topics?\" to list topics, TODO ...'.");
	/*
SAY:There trust true, patterns 'help topics'; responses 'Type "my topics?" to list topics, TODO ...'.
GET:Ok.
SAY:What trust true patterns?
GET:There not; patterns 'responses Type "my topics?" to list topics, TODO ...', help topics; patterns help, support.
	 */
	//say("There trust true, patterns 'help topics', responses 'Type \"my topics?\" to list topics, TODO ...'.");
	/*
SAY:There trust true, patterns 'help topics', responses 'Type "my topics?" to list topics, TODO ...'.
GET:Ok.
SAY:What trust true patterns?
GET:There not; patterns 'responses Type "my topics?" to list topics, TODO ...', help topics; patterns help, support.
	 */
	say("There trust true, patterns 'help login', 'login help'; responses 'You should be logged in to get fully personalized experience. Type \"my login\" to get prompted for your email, name and surname, which can be entered as \"my email john@doe.org, name john, surname doe\". By entering this information your effectively agree with our privacy policy and license agreement https://aigents.com/en/license.html and authorize our application to keep your data. In order to verify your authorization, you will be prompted to enter secret answer and secret question, which you can provide answering something like \"My secret question \'color of my desk plus number of rooms in my house\', secret answer \'pink+6\'\".'.");
	say("There trust true, patterns 'help logout', 'logout help'; responses 'You can log out to secure your data. Type \"my logout\" to get logged out of authorized conversation with our application.'.");
	say("There trust true, patterns 'help topics', 'topics'; responses '\"Topics\" control list of your topics of interest, with some of them trusted so they are monitored for news. Type \"my topics?\" to list topics, \"my topics \'internet agent\'\" to add \'internet agent\' to topics, \"my topics no \'internet agent\'\" to remove \'internet agent\' from topics, \"\'internet agent\' trust true\" to make the topic trusted for montitoring, \"\'internet agent\' trust false\" to remove trust for topic so it is not montitored. For more details, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5 .'.");
	say("There trust true, patterns 'help sites', 'sites'; responses '\"Sites\" control list of your sites of interest, with some of them trusted so they are monitored for news. Type \"my sites?\" to list sites, \"my sites https://aigents.com/\" to add https://aigents.com/ to sites, \"my sites no https://aigents.com/\" to remove https://aigents.com/ from sites, \"https://aigents.com/ trust true\" to make the site trusted for montitoring, \"https://aigents.com/ trust false\" to remove trust for site so it is not montitored.'.");
	say("There trust true, patterns 'help news', 'news'; responses '\"News\" control your news feed. Type \"my news text?\" to list texts of the news items, \"my news text, sources?\" to list texts along with source URLs.'.");
	say("There trust true, patterns 'help search', 'search help'; responses '\"Search\" makes search historical web search for you. Type \"search aigents\" to search news about aigents, \"search \'internet agent\'\" to search news about \'internet agent\', \"search aigents, time 2019-05-12\" to search till specified date, \"search \'internet agent\', time 2019-05-12, period 10\" to search till specific date for peroid of specified number of days.'.");
//TODO: fix as not being parsed!
	//say("There trust true, patterns 'help search', 'search'; responses '\"Search\" makes search historical web search for you. Type \"search aigents\" to search news about aigents, \"search \'internet agent\'\" to search news about \'internet agent\', \"search aigents, time 2019-05-12\" to search till specified date, \"search \'internet agent\', time 2019-05-12, period 10\" to search till specific date for peroid of specified number of days.'.");
	say("patterns 'search help' patterns 'search'");//TODO: fix hack!?
	say("There trust true, patterns 'help notification', 'notification help'; responses 'Control your notification over email and popular messengers. Type \"my email notification?\", \"my telegram notification?\", \"my facebook notification?\" or \"my slack notification?\" to know your notification status, \"my telegram notification true\" - to turn telegram notifications on, \"my email notification false\" - to turn email notifications off.'.");
	say("There trust true, patterns hi, hello, greeting; responses 'hi!', 'hello!', 'greeting!'.");
	
	
	say("what patterns search?");
	get();
	say("what patterns 'help search'?");
	get();
	
	
	//test help contents
	say("help me");
	get("Type \"help login\", \"help logout\", \"help search\", \"help topics\", \"help sites\", \"help news\", \"help notification\".");
	say("help login!");
	get("You should be logged in",null,true);
	say("help logout");
	get("You can log out",null,true);
	say("help topics!");
	get("\"Topics\" control list of your topics of interest",null,true);
	say("help me with sites");
	get("\"Sites\" control list of your sites of interest",null,true);
	say("help news!");
	get("\"News\" control your news feed.",null,true);
	say("help with search");
	get("\"Search\" makes search historical web search for you.",null,true);
	say("help me with notification!");
	get("Control your notification over email and popular messengers.",null,true);

	//test future command shortcuts as help content stubs
	say("topics");
	get("\"Topics\" control list of your topics of interest",null,true);
	say("sites");
	get("\"Sites\" control list of your sites of interest",null,true);
	say("news");
	get("\"News\" control your news feed.",null,true);
	say("search");
	get("\"Search\" makes search historical web search for you.",null,true);
	
	//make sure we can retain trusted intents and forget untrusted ones 
	say("You forget!");
	get("Ok.");
	say("What patterns help?").
	get("There patterns help, support, responses 'Type \"help login\", \"help logout\", \"help search\", \"help topics\", \"help sites\", \"help news\", \"help notification\".'.");
	say("help!");
	get("Type \"help login\", \"help logout\", \"help search\", \"help topics\", \"help sites\", \"help news\", \"help notification\".");
	say("Trust true trust false.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("What patterns help?").
	get("There not.");
	say("help!");
	get("No thing.");
	logout();
}

function test_chat() {
	global $version;
	global $copyright;
	
	//TODO: free text interactions on news - "any news?" "what's new", "whatsup"?
	//TODO: on greeting (hi, hello, привет), use default prompt before asking question
	//TODO: on any unrecogized input, try default prompt before asking question back
	//TODO: have smart greeting - using pattern->response mechanics
	//TODO: add context as a pattern
	//TODO: let response be a patern fillable from context
	//TODO: load help data from external file
	//TODO: chat translation
	//TODO: free ontology operations
	//TODO: free text question answering using graphs
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
	get("There patterns help, support, responses 'See examples at https://aigents.com/test/aigents_turing_test.html'.");
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


function test_groups() {
	login();
	
	//say("there is group, network telegram, name 'Aigents', id -1001115260768.");
	//get("Ok.");
	//say("there is group, network slack, name 'Aigents Test', id 12345.");
	//get("Ok.");
	//say("what is group?");
	//get("There id -1001115260768, is group, network telegram, name Aigents; id 12345, is group, network slack, name Aigents Test.");
	//say("there is group, network telegram, name 'Some Test', id 12345, members name anton, email koloin.");

	//say("there is group, network telegram, id 12345, name 'Some Test', members bob, rob.");
	//say("[is group, network telegram, id 12345, name 'Some Test', members (name bob, surname bobbey).");
	//get();
	//say("what is group?");
	//get();
	
	say("There text 4 today, times today, new true, trust false, sources http://weather.yahoo.com.");
	get("Ok.");
	test_o("What times today?");
	get();
	say("new true new false.");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	
	say("No there is group.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	
	logout();
}


test_init();
test_help();
/**/
test_search();
test_chat();
test_groups();
test_summary();
/**/
?>
