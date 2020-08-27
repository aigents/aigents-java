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


function test_agent_learn() {
	global $basePath;
	global $version;
	global $copyright;
	global $base_things_count;
	
	//login, registration, verification
	say("My name john, email john@doe.org, surname doe.");
	get("What your secret question, secret answer?");
	say("My secret question q, secret answer a.");
	get("What your q?");
	say("My q a.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

	say("You forget everything!");
	get("Ok.");
	
	//setup thing
	say("My topics person.");
	get("Ok.");
	say("Person has firstname, lastname.");
	get("Ok.");
	say("Firstname is word.");
	get("Ok.");
	say("Lastname is word.");
	get("Ok.");
	say("Person patterns '\$firstname \$lastname {CEO CTO Founder Director}'.");
	get("Ok.");
	say("What name person?");
	get("Person has firstname, lastname, name person, patterns '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("What person?");
	get("Person has firstname, lastname, name person, patterns '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("Person?");
	get("Person has firstname, lastname, name person, patterns '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("What your things count?");
	get("My things count ".($base_things_count+4).".");
	say("Path?");
	get("No.");
	say("What person path?");
	get("No.");
	
	//aside from learning, test ability to detect really new news only
	//TODO: fix parsing so 'john doe founder' is parsed out of 'there is john doe founder elected'
	//TODO: ... and remove the silly donts below
	file_put_contents($basePath."html/test.html","<html><body>there is . john doe founder elected</body></html>");
	say("You reading person in http://localtest.com/test.html!");
	get("My reading person in http://localtest.com/test.html.");
	say("What is person text?");
	get("There text john doe founder.");
	say("Text john doe founder times 2015-01-01");//push event back
	get("Ok.");
	say("You reading person in http://localtest.com/test.html!");
	get("No.");
	file_put_contents($basePath."html/test.html","<html><body>there is . john doe director appointed</body></html>");
	say("You reading person in http://localtest.com/test.html!");
	get("My reading person in http://localtest.com/test.html.");
	say("What is person text, times?");
	get("There text john doe director, times today; text john doe founder, times 2015-01-01.");
	file_put_contents($basePath."html/test.html","<html><body>there is . john doe founder re-elected</body></html>");
	say("You reading person in http://localtest.com/test.html!");
	//get("No.");
	get("My reading person in http://localtest.com/test.html.");
	say("What is person text, times?");
	get("There text john doe director, times today; text john doe founder, times 2015-01-01; text john doe founder, times today.");
	file_put_contents($basePath."html/test.html","<html><body>there is . john doe director dismissed</body></html>");
	say("You reading person in http://localtest.com/test.html!");
	get("No.");
	say("What is person text, times?");
	//get("There text john doe founder, times 2015-01-01.");
	get("There text john doe director, times today; text john doe founder, times 2015-01-01; text john doe founder, times today.");
	file_put_contents($basePath."html/test.html","<html><body>there is . ali baba founder suggested by . john doe director recently</body></html>");
	say("You reading person in http://localtest.com/test.html!");
	get("My reading person in http://localtest.com/test.html.");
	say("What is person text, times?");
	//TODO: consider how to avoid getting redundant john doe today (not easy, if not counting times precisely and excluding curent time from evaluation!)
	get("There text ali baba founder, times today; text john doe director, times today; text john doe founder, times 2015-01-01; text john doe founder, times today.");
	file_put_contents($basePath."html/test.html","<html><body>there is . ali baba founder approved. john doe presented stuff. john doe director gave overview.</body></html>");
	say("You reading person in http://localtest.com/test.html!");
	get("No.");
	say("What times today?");
	get();	
	say("Times today times yesterday.");
	get("Ok.");
	say("What times yesterday?");
	//get("There firstname ali, is person, lastname baba, sources http://localtest.com/test.html, text ali baba founder, times yesterday; firstname john, is person, lastname doe, sources http://localtest.com/test.html, text john doe director, times yesterday; is http://localtest.com/test.html, text 'there is . ali baba founder approved', times yesterday.");
	get("There firstname ali, is person, lastname baba, sources http://localtest.com/test.html, text ali baba founder, times yesterday, title ali baba founder; firstname john, is person, lastname doe, sources http://localtest.com/test.html, text john doe director, times yesterday, title john doe director; firstname john, is person, lastname doe, sources http://localtest.com/test.html, text john doe founder, times yesterday, title john doe founder; is http://localtest.com/test.html, text 'there is . ali baba founder approved. john doe presented stuff. john doe director gave overview.', times yesterday.");
	say("What is http://localtest.com/test.html times, text?");
	get("There text 'there is . ali baba founder approved. john doe presented stuff. john doe director gave overview.', times yesterday.");
	say("You reading person in http://localtest.com/test.html!");
	get("No.");
	file_put_contents($basePath."html/test.html","<html><body>there is . joe johns CTO appointed</body></html>");
	say("You reading person in http://localtest.com/test.html!");
	get("My reading person in http://localtest.com/test.html.");
	say("What times today, is person firstname, lastname?");
	get("There firstname joe, lastname johns.");
	say("No there times yesterday.");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	say("No there is person.");
	get("Ok.");
	
	//now do the learning with profiling timers set up
	starttimer("find");
	
	//find path for thing on site A	
	say("My sites http://localtest.com/sitea/.");
	get("Ok.");
	say("You reading person in http://localtest.com/sitea/!");
	get("My reading person in http://localtest.com/sitea/.");
	say("What is person firstname, lastname, sources?");
	get("There firstname john, lastname doe, sources http://localtest.com/sitea/management.html.");
	say("What person path?");
	get("Person path '{[[about us] [company management]]}'.");
	//clean up found data and check the path
	say("Is person new false.");
	get("Ok.");
	say("No there is person.");
	get("Ok.");
	say("No there is http://localtest.com/sitea/.");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	say("What is person firstname, lastname, sources?");
	get("No.");
	say("What person path?");
	get("Person path '{[[about us] [company management]]}'.");
	//find path for thing on site B
	say("You reading person in http://localtest.com/siteb/!");
	get("My reading person in http://localtest.com/siteb/.");
	say("What is person firstname, lastname?");
	get("There firstname doug, lastname jones.");
	//check merged paths
	say("What person path?");
	get("Person path '{[[about our company] [company management information]] [[about us] [company management]]}'.");

	stoptimer("find");
	starttimer("track");
	
	//retry again
	say("Is person new false.");
	get("Ok.");
	say("No there is person.");
	get("Ok.");
	say("What is person firstname, lastname?");
	get("No.");
	//find thing by path on site B
	say("You reading person in http://localtest.com/siteb/!");
	get("My reading person in http://localtest.com/siteb/.");
	say("What is person firstname, lastname?");
	get("There firstname doug, lastname jones.");
	//find thing by path on site A
	say("You reading person in http://localtest.com/sitea/!");
	get("My reading person in http://localtest.com/sitea/.");
	say("What is person firstname, lastname?");
	get("There firstname doug, lastname jones; firstname john, lastname doe.");

	stoptimer("track");

	//cleanup test sites
	say("No there is http://localtest.com/sitea/.");
	get("Ok.");
	say("My sites no http://localtest.com/sitea/.");
	get("Ok.");
	say("No name http://localtest.com/sitea/.");
	get("Ok.");
	
	//try real sites
	//http://www.irobot.com/->About iRobot->Management Team->Success
	//https://www.google.com->About->Management->Success
	//http://www.touchbionics.com/->About->Board of Directors->Success
	//http://www.northropgrumman.com/->About Us->Company Leadership->Success
	//http://www.rethinkrobotics.com/->About->Leadership->Success
	//http://www.accuray.com/->ABOUT US->Success
	//http://liquidr.com/->Management->Success
	//http://www.bosch.com/->Board of Management->Success
	//http://www.qbotix.com/->About->Success
	//http://www.proxdynamics.com/->Failure	
	/**/
	//say("Person patterns '\$firstname \$lastname {CEO CTO Founder Director}'.");
	//say("Person patterns '\$firstname \$lastname . {CTO CEO Founder Director Co-Founder President Corporate Executive Chairman Deputy Chief}'.");
	say("Person patterns '\$firstname \$lastname . {CTO CEO Founder Director Co-Founder President Executive Chairman Deputy Chief}'.");
	get("Ok.");
	say("Is person new false.");
	get("Ok.");
	say("No there is person.");
	get("Ok.");
	say("What is person firstname, lastname?");
	get("No.");

	//http://www.irobot.com/->About iRobot->Management Team->Success
	//https://www.google.com->About->Management->Success
	//http://www.touchbionics.com/->About->Board of Directors->Success
	//http://www.northropgrumman.com/->About Us->Company Leadership->Success
	//http://www.rethinkrobotics.com/->About->Leadership->Success
	//http://www.accuray.com/->ABOUT US->Success
	$test_path = "http://liquidr.com/";//->Management->Success
	//http://www.bosch.com/->Board of Management->Success
	//$test_path = "http://www.qbotix.com/";//->About->Success:finds "QBotix Adds New CEO, Continues Momentum"
/*	
	starttimer("find-real");	
		say("You reading person in ".$test_path."!");
		get("My reading person in ".$test_path.".");
	stoptimer("find-real");
	say("What is person firstname, lastname?");
	get();
	say("What is person firstname, lastname, sources?");
	get();
	say("What person path?");
	get();
	
	say("Is person new false.");
	get("Ok.");
	say("No there is person.");
	get("Ok.");

	starttimer("track-real");
		say("You reading person in ".$test_path."!");
		get("My reading person in ".$test_path.".");
	stoptimer("track-real");
	say("What is person firstname, lastname, sources, text?");
	get();
	
	say("Is person new false.");
	get("Ok.");
*/
	
	say("My topics no person.");
	get("Ok.");
	say("No there is person.");
	get("Ok.");
	
	say("No name person.");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("What your things?");
	get();
	say("No name firstname.");
	get("Ok.");
	say("No name lastname.");
	get("Ok.");
	//TODO:include it into the foundation ontology, so no need to cleanup
	//say("No name word.");
	//get("Ok.");
	say("Your trusts no john.");
	get("Ok.");	
	say("No name john.");
	get("Ok.");
	say("What times today is?");
	get();//TODO: get("No."); //decide what to do with multiple along-the-path sites
	say("No there times today.");
	get("Ok.");
	say("What times today?");
	get("No.");
	say("My logout.");
	get("Ok.");
	
}	
	
function test_agent_agglomerate() {
	global $version;
	global $copyright;
	global $basePath;
	global $timeout;
	global $base_things_count;
	
	//login, registration, verification
	say("My name john, email john@doe.org, surname doe.");
	get("What your secret question, secret answer?");
	say("My secret question q, secret answer a.");
	get("What your q?");
	say("My q a.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	
	say("You forget everything!");
	get("Ok.");

	file_put_contents($basePath."html/sitea/test.html","<html><body>there is . john doe founder joined</body></html>");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . john doe founder elected</body></html>");
	say("my topics '\$firstname \$lastname {CEO CTO Founder Director}'.");
	get("Ok.");
	say("my trusts '\$firstname \$lastname {CEO CTO Founder Director}'.");
	get("Ok.");
	
	//TODO: why firstname and lastname are not handled as words - handle implicit variables
	//TODO:say("there name lastname, is word.");
	//TODO:say("there firstname, is word.");
	say("'\$firstname \$lastname {CEO CTO Founder Director}' has firstname, lastname.");
	get("Ok.");
	say("lastname is word.");
	get("Ok.");
	say("firstname is word.");
	get("Ok.");
	
	say("What times today?");
	get("No.");
	say("My sites http://localtest.com/sitea/test.html, http://localtest.com/siteb/test.html.");
	say("My trusts http://localtest.com/sitea/test.html, http://localtest.com/siteb/test.html.");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' firstname, lastname, text?");
	get("There firstname john, lastname doe, text john doe founder.");
	
	//Test LTM 1:
	say("times today new false.");
	get("Ok.");
	say("no there times today.");
	get("Ok.");
//TODO: note that witohiut of "text", non-complex query handling will fallback to chat mode!
	//say("What is '\$firstname \$lastname {CEO CTO Founder Director}' text?");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' text?");
	get("No.");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' text?");
	get("No.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . john doe founder adviced</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' text?");
	get("No.");
	say("You forget everything!");
	get("Ok.");
	say("What times today?");
	get("There is http://localtest.com/sitea/test.html, text 'there is . john doe founder joined', times today; is http://localtest.com/siteb/test.html, text 'there is . john doe founder adviced', times today.");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' text?");
	get("No.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . john doe founder declared</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' text?");
	get("There text john doe founder.");

	//Test snaphotting:
	//T1: "a x b y"     * "{a b} $q"  -> x y   -> x y
	//T2: "a x b y b z" * "{a b} $q"  -> x y z -> z
	//T3: "a x a y b z" * "{a b} $q"  -> x y z ->
	//T4: "a x a v b w" * "{a b} $q"  -> x v w -> v w
	//T5: "a y a z b w" * "{a b} $q"  -> y z w -> y z
	
	//TODO
	//Test snapshotting in STM:
	file_put_contents($basePath."html/sitea/test.html","<html><body></body></html>");
	say("times today new false.");
	get("Ok.");
	say("no there times today.");
	get("Ok.");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}'?");
	get("No.");
	say("You forget everything!");
	get("Ok.");
//TODO: make this working WITHOUT "and" and WITH it!!!
	//file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. and yy yyy cto said.</body></html>");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared . yy yyy cto said .</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text xx xxx founder; text yy yyy cto.");
	say("New true new false.");//discard news
	get("Ok.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared . yy yyy cto said . zz zzz cto announced .</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text zz zzz cto.");
	say("New true new false.");//discard news	
	get("Ok.");
//TODO:make this working!
	//file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. finally, yy yyy cto shouted. later, zz zzz cto spelled.</body></html>");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. yy yyy cto shouted. zz zzz cto spelled.</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("No.");
	say("New true new false.");//discard news
	get("No. No thing.");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' times, text?");
	get("There text xx xxx founder, times today; text yy yyy cto, times today; text zz zzz cto, times today.");	
//TODO: make this working
	//file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. next, vv vvv cto shouted. then, ww www cto spelled.</body></html>");	
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. vv vvv cto shouted. ww www cto spelled.</body></html>");	
	say("You reading!");
	sleep($timeout);	
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text vv vvv cto; text ww www cto.");
	say("New true new false.");//discard news
	get("Ok.");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}' times, text?");
	get("There text vv vvv cto, times today; text ww www cto, times today; text xx xxx founder, times today; text yy yyy cto, times today; text zz zzz cto, times today.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . yy yyy founder declared. zz zzz director shouted. ww www cto spelled.</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text yy yyy founder; text zz zzz director.");
	
	//Test snapshotting in LTM:
	say("new true new false.");
	get("Ok.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("no there times today.");
	get("Ok.");
	say("You forget everything!");
	get("Ok.");
	file_put_contents($basePath."html/sitea/test.html","<html><body></body></html>");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}'?");
	get("No.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared . yy yyy cto said .</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text xx xxx founder; text yy yyy cto.");//1
	say("new true new false.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("no there times today.");
	say("new true new false.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("no there times today.");
	say("What times today?");
	get("No.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared . yy yyy cto said . zz zzz cto announced .</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text zz zzz cto.");//2
	say("new true new false.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("no there times today.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. yy yyy cto shouted. zz zzz cto spelled.</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("No.");//3
	say("new true new false.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("no there times today.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. vv vvv cto shouted. ww www cto spelled.</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text vv vvv cto; text ww www cto.");//4
	say("new true new false.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("no there times today.");
	say("new true new false.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("no there times today.");
	say("What times today?");
	get("No.");
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . yy yyy founder declared. zz zzz director shouted. ww www cto spelled.</body></html>");
	say("You reading!");
	sleep($timeout);
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text yy yyy founder; text zz zzz director.");//5	

	//cleanup and logout
	say("new true new false.");
	get("Ok.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'."); 
	get("Ok.");
	say("no there is http/sitea/test.html."); 
	get("Ok.");
	say("no there is http/siteb/test.html."); 
	get("Ok.");
	say("no there times today.");
	get("Ok.");
	say("my topics no '\$firstname \$lastname {CEO CTO Founder Director}'.");
	get("Ok.");
	say("my trusts no '\$firstname \$lastname {CEO CTO Founder Director}'.");
	get("Ok.");
	say("My trusts no http://localtest.com/sitea/test.html, no http://localtest.com/siteb/test.html.");
	get("Ok.");
	say("My sites no http://localtest.com/sitea/test.html, no http://localtest.com/siteb/test.html.");
	get("Ok.");
	say("No name '\$firstname \$lastname {CEO CTO Founder Director}'.");
	get("Ok.");
	say("No name firstname.");
	get("Ok.");
	say("No name lastname.");
	get("Ok.");
	say("What times today?");
	get("No.");
	say("You forget.");
	get("Ok.");

	say("What your things?");
	get("My things activity time, aigents, areas, attention period, birth date, caching period, check cycle, clicks, clustering timeout, conversation, cookie domain, cookie name, copypastes, crawl range, currency, daytime, discourse id, discourse key, discourse url, email, email cycle, email login, email notification, email password, email retries, ethereum id, ethereum key, ethereum period, ethereum url, facebook challenge, facebook id, facebook key, facebook notification, facebook token, format, friend, friends, golos id, golos url, google id, google key, google token, googlesearch key, http origin, http port, http secure, http threads, http timeout, http url, ignores, items limit, john, language, login count, login time, login token, mail.pop3.starttls.enable, mail.pop3s.host, mail.pop3s.port, mail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.smtp.ssl.enable, mail.smtp.starttls.enable, mail.store.protocol, money, name, news, news limit, number, paid term, paypal id, paypal key, paypal token, paypal url, peer, phone, queries, reddit id, reddit image, reddit key, reddit redirect, reddit token, registration time, reputation conservatism, reputation decayed, reputation default, reputation system, retention period, secret answer, secret question, selections, self, sensitivity threshold, serpapi key, share, shares, sites, slack id, slack key, slack notification, slack token, steemit id, steemit url, store cycle, store path, surname, tcp port, tcp timeout, telegram id, telegram name, telegram notification, telegram offset, telegram token, there, things, things count, time, topics, trusts, trusts limit, twitter id, twitter image, twitter key, twitter key secret, twitter redirect, twitter token, twitter token secret, update time, version, vkontakte id, vkontakte key, vkontakte token, word.");
	say("What your things count?");
	get("My things count ".($base_things_count).".");

	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	say("My logout.");
	get("Ok.");	
}
	

function test_agent_parse() {
	global $version;
	global $copyright;
	global $basePath;
	global $timeout;

	//login, registration, verification
	say("My name john, email john@doe.org, surname doe.");
	get("What your secret question, secret answer?");
	say("My secret question q, secret answer a.");
	get("What your q?");
	say("My q a.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

	say("my topics '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("my trusts '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("My sites http://localtest.com/siteb/test.html.");
	say("My trusts http://localtest.com/siteb/test.html.");
	
	say("'\$firstname \$lastname {CEO CTO Founder Director}' has firstname, lastname.");
	say("lastname is word.");
	say("firstname is word.");
	
	say("You forget everything!");

	file_put_contents($basePath."html/siteb/test.html","<html><body>here is period xx xxx founder found.</body></html>");
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text xx xxx founder.");
	say("new true new false.");
	say("no there times today.");
	
	file_put_contents($basePath."html/siteb/test.html","<html><body>here is. xx xxx founder found.</body></html>");
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text xx xxx founder.");
	say("new true new false.");
	say("no there times today.");
	
	file_put_contents($basePath."html/siteb/test.html","<html><body>amazing xx xxx founder declared.</body></html>");
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text xx xxx founder.");
	say("new true new false.");
	say("no there times today.");
	
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . xx xxx founder declared. next, vv vvv cto shouted. then, ww www cto spelled.</body></html>");	
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text vv vvv cto; text ww www cto; text xx xxx founder.");
	say("new true new false.");
	say("no there times today.");

	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . finally, xx xxx founder declared. next, where aa aaa cto shouted. then, there is ww www cto spelled.</body></html>");
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text aa aaa cto; text ww www cto; text xx xxx founder.");
	say("new true new false.");
	say("no there times today.");
	
	file_put_contents($basePath."html/siteb/test.html","<html><body>there is . bb bbb founder declared. vv vvv cto shouted. ww www cto spelled.</body></html>");	
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text bb bbb founder; text vv vvv cto; text ww www cto.");
	say("new true new false.");
	say("no there times today.");
	
	file_put_contents($basePath."html/siteb/test.html","<html><body>my xx xxx cto cried.</body></html>");
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text xx xxx cto.");
	say("new true new false.");
	say("no there times today.");
	
	file_put_contents($basePath."html/siteb/test.html","<html><body>xx xxx ceo presented.</body></html>");
	say("You reading '\$firstname \$lastname {CEO CTO Founder Director}' in http://localtest.com/siteb/test.html!");
	say("What is '\$firstname \$lastname {CEO CTO Founder Director}', new true text?");
	get("There text xx xxx ceo.");
	
	//cleanup and logout
	say("new true new false.");
	say("no there is '\$firstname \$lastname {CEO CTO Founder Director}'."); 
	say("no there is http/sitea/test.html."); 
	say("no there is http/siteb/test.html."); 
	say("no there times today.");
	say("my topics no '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("my trusts no '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("My trusts no http://localtest.com/sitea/test.html, no http://localtest.com/siteb/test.html.");
	say("My sites no http://localtest.com/sitea/test.html, no http://localtest.com/siteb/test.html.");
	say("No name '\$firstname \$lastname {CEO CTO Founder Director}'.");
	say("No name firstname.");
	say("No name lastname.");
	say("You forget.");

	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	say("My logout.");
	get("Ok.");	
}

function test_agent_site_graph() {
	global $version;
	global $copyright;
	global $timeout;

	//login, registration, verification
	say("My name john, email john@doe.org, surname doe.");
	get("What your secret question, secret answer?");
	say("My secret question q, secret answer a.");
	get("What your q?");
	say("My q a.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	
	say_site("http://localtest.com/sitea");
	say_site("http://localtest.com/siteb");
	say_thing("something unfoundable");

	say("You reading!");
	sleep($timeout);
	
	//TODO: url instead of id
	say("www id http://localtest.com/sitea graph date today.");
	get("http://localtest.com/sitea links http://localtest.com/about.html 100.\nhttp://localtest.com/sitea links http://localtest.com/contacts.html 100.\nhttp://localtest.com/sitea links http://localtest.com/products.html 100.\n");
	say("www id http://localtest.com/siteb graph date today.");
	get("http://localtest.com/siteb links http://localtest.com/about_company.html 100.\nhttp://localtest.com/siteb links http://localtest.com/company_products.html 100.\nhttp://localtest.com/siteb links http://localtest.com/contact_info.html 100.\n");
	say("www id http://localtest.com/about.html graph date today, range 5.");
	get("http://localtest.com/about.html linked http://localtest.com/sitea 100.\nhttp://localtest.com/sitea links http://localtest.com/contacts.html 100.\nhttp://localtest.com/sitea links http://localtest.com/products.html 100.\n");

	say("No there times today.");
	get("Ok.");
	say_thing("something unfoundable",false);
	say_site("http://localtest.com/sitea",false);
	say_site("http://localtest.com/siteb",false);
	say("You forget.");
	
	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
}
	
	
test_init();
test_agent_learn();
test_agent_agglomerate();	
test_agent_parse();
test_agent_site_graph();
printtimers();
test_summary();

?>
