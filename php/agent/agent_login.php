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

function test_login_debug() {
	global $version, $copyright;
	//debug registration and unregistration of a real non-test user
	/*say("Login.");
	get("What your email, name, surname?");
	say("test@test.com, Firstname, Lastname");
	get("What your secret question, secret answer?");
	say("question, answer");
	get("What your question?");
	say("answer");
	get("Ok. Hello Firstname Lastname!\nMy Aigents ".$version.$copyright);
	logout("Firstname",true);
	say("Login.");
	get("What your email, name, surname?");
	say("test@test.com, Firstname, Lastname");
	get("What your secret question, secret answer?");
	say("question, answer");
	get("What your question?");
	say("answer");
	get("Ok. Hello Firstname Lastname!\nMy Aigents ".$version.$copyright);
	logout("Firstname",true);
	login();
	//cleanup
	say("You forget!");
	get("Ok.");
	say("Your email ''.");
	get("Ok.");
	say("Your things?");
	get("My things activity time, aigents, areas, attention period, birth date, caching period, check cycle, clicks, clustering timeout, conversation, cookie domain, cookie name, copypastes, crawl range, currency, daytime, discourse id, discourse key, discourse url, email, email cycle, email login, email notification, email password, email retries, ethereum id, ethereum key, ethereum period, ethereum url, facebook challenge, facebook id, facebook key, facebook notification, facebook token, format, friend, friends, golos id, golos url, google id, google key, google token, googlesearch key, http origin, http port, http secure, http threads, http timeout, http url, ignores, items limit, john, language, login count, login time, login token, mail.pop3.starttls.enable, mail.pop3s.host, mail.pop3s.port, mail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.smtp.ssl.enable, mail.smtp.starttls.enable, mail.store.protocol, money, name, news, news limit, number, paid term, paypal id, paypal key, paypal token, paypal url, peer, phone, queries, reddit id, reddit image, reddit key, reddit redirect, reddit token, registration time, reputation conservatism, reputation decayed, reputation default, reputation system, retention period, secret answer, secret question, selections, self, sensitivity threshold, serpapi key, share, shares, sites, slack id, slack key, slack notification, slack token, steemit id, steemit url, store cycle, store path, surname, tcp port, tcp timeout, telegram id, telegram name, telegram notification, telegram offset, telegram token, there, things, things count, time, topics, trusts, trusts limit, twitter id, twitter image, twitter key, twitter key secret, twitter redirect, twitter token, twitter token secret, update time, version, vkontakte id, vkontakte key, vkontakte token, word.");
	say("Your things count?");
	get("My things count 134.");
	logout();*/
	
	//debug registration and unregistration conventional flow
	say("My login.");
	get("What your email, name, surname?");

	say("Why are you asking this?");//trying to screw things up 
	get("What your email, name, surname?");
	
	say("john@doe.org, john, doe");
	get("What your secret question, secret answer?");

	say("Why do you keep asking this?");
	get("What your secret question, secret answer?");
	
	//say("sky color, red");
	//get("What your sky color?");
	
	say("sky color?, red");
	get("What your 'sky color?'?");
	
	say("red");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

	//cleanup
	say("Your trusts no john.");
	get("Ok.");
	say("No email john@doe.org.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("My logout.");
	get("Ok.");
}

function test_login_new() {
	global $version;
	global $copyright;
	
	//register by unexisting email -> ok
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org, name john, surname doe, secret question x, secret answer y.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");

	//login by email -> cancel
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your x?");
	say("No.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your x?");
	say("Not");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your x?");
		
	//login by email -> ok (email as unique id)
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your x?");
	say("My x y.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");
	//register by existing email -> login by email
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org, name ali, surname baba, secret question blah, secret answer blah.");
	get("What your x?");
	say("My x z.");
	get("What your x?");
	say("My x 1.");
	get("What your x?");
	say("My x a.");
	get("What your email, name, surname?");//back to login
	say("My email john@doe.org.");
	get("What your x?");
	say("My x y.");
	test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");

	//register by existing email -> registration by email
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org, name ali, surname baba, secret question blah, secret answer blah.");
	get("What your x?");
	say("My x z.");
	get("What your x?");
	say("My x 1.");
	get("What your x?");
	say("My x a.");
	get("What your email, name, surname?");//registration to login
	say("My email doe@john.org, name doe, surname john, secret question blah, secret answer blah.");
	get("What your blah?");
	say("My blah blah.");
	test_i("Ok. Hello Doe John!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");	
	
	//recall secret by 'What my secret answer?'
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your x?");
	say("What my secret answer?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("My secret question dee, secret answer daa.");
	get("What your dee?");
	say("My dee daa.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");

	//fail verification code reset on multiple errors to logout
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your dee?");
	say("What my secret answer?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1111.");
	get("What your verification code?");
	say("My verification code 2222.");
	get("What your verification code?");
	say("My verification code 3333.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your dee?");
	say("What my secret answer?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("My secret question dee, secret answer daa.");
	get("What your dee?");
	say("My dee daa.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");
	
	//reset from secret change verification
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your dee?");
	say("What my secret answer?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1111.");
	get("What your verification code?");
	say("What my verification code?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("My secret question dee, secret answer daa.");
	get("What your dee?");
	say("My dee daa.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");

	//reset from secret question - secret answer form
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your dee?");
	say("What my secret answer?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1111.");
	get("What your verification code?");
	say("What new true sources, text, times, trust?");
	get("No.");
	say("What my sites name, trust?");
	get("No.");
	say("What my verification code?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("My logout.");
	get("What your email, name, surname?");
			
	//recall secret by 'secret question' value questioned
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your dee?");
	say("What my dee?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("My secret question dee, secret answer doo.");
	get("What your dee?");
	say("My dee doo.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");

	//change email
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.org.");
	get("What your dee?");
	say("My dee doo.");
	test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My email john@doe.com.");
	get("What your verification code? Sent to john@doe.com.");
	say("My verification code 1234.");
	get("Ok. Your email john@doe.com.");
	say("My logout.");
	get("Ok.");

	//reset from email change verification
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your dee?");
	say("My dee doo.");
	test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My email john@doe.org.");
	get("What your verification code? Sent to john@doe.org.");
	say("What times today, new true?");
	get("No.");
	say("What my verification code?");
	get("What your verification code? Sent to john@doe.org.");
	say("My verification code 1234.");
	get("Ok. Your email john@doe.org.");
	say("My email john@doe.com.");
	get("What your verification code? Sent to john@doe.com.");
	say("My verification code 1234.");
	get("Ok. Your email john@doe.com.");
	say("My logout.");
	get("Ok.");

	//cancel from email change verification
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your dee?");
	say("My dee doo.");
	test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My email john@doe.org.");
	say("What my verification code?");
	say("Not");
	get("Ok.");
	say("My logout.");
	get("Ok.");
	
	//failing scenario: register - cancel in the middle
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your dee?");
	say("No");
	get("What your email, name, surname?");
	say("My email ali@baba.org.");
	get("What your name, surname?");
	say("No.");
	get("What your email, name, surname?");
	say("My name ali, surname baba.");
	get("What your email?");
	say("Not");
	get("What your email, name, surname?");
	say("My name ali, surname baba.");
	get("What your email?");
	say("My email ali@baba.org.");
	get("What your secret question, secret answer?");	
	//failing scenario: specify secrets - cancel in the middle
	say("No.");
	get("What your email, name, surname?");
	say("My email ali@baba.org.");
	get("What your secret question, secret answer?");
	say("My secret question sezame, secret answer simsim.");
	get("What your sezame?");
	//failing scenario: verify login - cancel in the middle	
	say("No.");
	get("What your email, name, surname?");
	say("My email ali@baba.org.");
	get("What your sezame?");
	say("My sezame simsim.");
	get("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");

	//failing scenario: recall password - cancel in the middle
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your dee?");
	say("What my dee?");
	get("What your verification code? Sent to john@doe.com.");
	say("No.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your dee?");
	say("What my dee?");
	get("What your verification code? Sent to john@doe.com.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("No.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your dee?");
	say("What my dee?");
	get("What your verification code? Sent to john@doe.com.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("No.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your dee?");
	say("What my dee?");
	get("What your verification code? Sent to john@doe.com.");
	say("My verification code 1234.");
	get("What your secret question, secret answer?");
	say("My secret question todo, secret answer tada.");
	get("What your todo?");
	say("No");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your todo?");
	say("My todo tada.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");
	
	//failing scenario: change email - cancel in the middle
	say("My login.");
	get("What your email, name, surname?");
	say("My email ali@baba.org.");
	get("What your sezame?");
	say("My sezame simsim.");
	get("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
	say("My email ali@baba.com.");
	get("What your verification code? Sent to ali@baba.com.");
	say("Not");
	get("Ok.");
	say("What my email?");
	get("Your email ali@baba.org.");
	say("My email ali@baba.com.");
	get("What your verification code? Sent to ali@baba.com.");
	say("My verification code 1234.");
	get("Ok. Your email ali@baba.com.");
	say("What my email?");
	get("Your email ali@baba.com.");	
	say("My logout.");
	get("Ok.");
	
	//account stealing: ensure one can't set existing email to themselves or to others (unique attributes)
	say("My login.");
	get("What your email, name, surname?");
	say("My email ali@baba.com.");
	get("What your sezame?");
	say("My sezame simsim.");
	get("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
	say("My email ali@baba.com.");
	get("Ok.");
	say("My email john@doe.com.");
	get("No. Email john@doe.com is owned.");
	say("What is peer, name test email?");
	get("No.");
	say("There is peer, name test, email ali@baba.org.");
	get("Ok.");
	say("Is peer, name test email john@doe.com.");
	get("No. Email john@doe.com is owned.");
	say("What is peer, name test email?");
	get("There email ali@baba.org.");	
	say("Is peer, name test email baba@ali.org.");
	get("Ok.");
	say("What is peer, name test email?");
	get("There email baba@ali.org.");
	say("My logout.");
	get("Ok.");
	
	//google registration: register with google unsing unexisting google id (use test fake google id)
	say("My google id 'testid1', google token 'testcode1'.");
	get("Ok. Hello Tesname Testsurname!\nMy Aigents ".$version.$copyright);
	say("My check cycle 123456789.");
	say("What my email, name, surname, google token, check cycle?");
	get("Your check cycle 123456789, email test@gmail.com, google token 'testcode1', name tesname, surname testsurname.");
	say("My name Testrealname, surname Testrealsurname.");
	say("My logout.");
	get("Ok.");

	//google bind error: login with google using existing google id (use test fake google id)
	say("My login.");
	get("What your email, name, surname?");
	say("My email ali@baba.com.");
	get("What your sezame?");
	say("My sezame simsim.");
	get("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
	//bind google => fail
	say("My google id 'testid1', google token 'testcode1'.");
	get("No. Google id testid1 is owned.");
	//set google to other => fail
	say("Is peer, name test google id 'testid1'.");
	get("No. Google id testid1 is owned.");
	//success => check later
	say("Is peer, name test google id 'testid2'.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
	//check later bind made earlier
	say("My google id 'testid2', google token 'testcode2'.");
	get("Ok. Hello Test!\nMy Aigents ".$version.$copyright);
	say("What my email?");
	get("Your email baba@ali.org.");
	say("My logout.");
	get("Ok.");
	
	//google login: login with google (use test fake google id)
	say("My google id 'testid1', google token 'testcode1'.");
	get("Ok. Hello Testrealname Testrealsurname!\nMy Aigents ".$version.$copyright);
	say("What my check cycle?");
	get("Your check cycle 123456789.");
	say("My logout.");
	get("Ok.");
	
	//google bind: login with google using unexisting google id (use test fake google id)
	say("My email ali@baba.com.");
	get("What your sezame?");
	say("My sezame simsim.");
	get("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
	say("What my google id?");
	get("No.");
	say("My google id 'testid3', google token 'testcode3'.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
	say("My google id 'testid3', google token 'testcode3'.");
	get("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");
	
	//cleanup
	say("My login.");
	get("What your email, name, surname?");
	say("My email john@doe.com.");
	get("What your todo?");
	say("My todo tada.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("Your trusts no john.");
	get("Ok.");
	say("No email doe@john.org.");
	get("Ok.");
	say("No email john@doe.com.");
	get("Ok.");
	say("No email ali@baba.org.");
	get("Ok.");
	say("No email baba@ali.org.");
	get("Ok.");
	say("No email ali@baba.com.");
	get("Ok.");
	say("No email test@gmail.com.");	
	get("Ok.");
	say("No there times today.");//force GC
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("My logout.");	
	get("Ok.");
}
	
function test_login_old() {
	global $version;
	global $copyright;
	global $base_things_count;
	
	//register 1st John
	test_o("My login.");
	test_i("What your email, name, surname?");
	test_o("My name John, email john@doe.org, surname Doe.");
	test_i("What your secret question, secret answer?");
	test_o("My secret question birth place, secret answer 'London'.");
	test_i("What your birth place?");
	test_o("My birth place 'London'.");
	test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("What your trusts?");
	get("My trusts john.");
	say("What your name, email password?");
	get("My email password 'replace this with proper password', name aigents.");
	test_o("My logout.");
	test_i("Ok.");
	
	//register 2nd John
	test_o("My login.");
	test_i("What your email, name, surname?");
	test_o("My name John, email doe@john.org, surname Dare.");
	test_i("What your secret question, secret answer?");
	test_o("My secret question birth place, secret answer Toronto.");
	test_i("What your birth place?");
	test_o("My birth place Toronto.");
	test_i("Ok. Hello John Dare!\nMy Aigents ".$version.$copyright);
	say("What your name?");
	get("My name aigents.");
	say("What your name, email password?");
	get("No. No right.");
	say("Your name dummy.");
	get("No. No right.");
	say("What your name?");
	get("My name aigents.");
	test_o("My logout.");
	test_i("Ok.");
	
	//login 2nd John
	test_o("My login.");
	test_i("What your email, name, surname?");
	test_o("My name John, email doe@john.org, surname Dare.");
	test_i("What your birth place?");
	test_o("My birth place Toronto.");
	test_i("Ok. Hello John Dare!\nMy Aigents ".$version.$copyright);
	test_o("My logout.");
	test_i("Ok.");
	
	//login 1st John
	test_o("My login.");
	test_i("What your email, name, surname?");
	test_o("My name John, email john@doe.org, surname Doe.");
	test_i("What your birth place?");
	test_o("My birth place 'London'.");
	test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	
	//check if both John-s are present
	test_o("What name john email, surname?");
	test_i("John email doe@john.org, surname dare; email john@doe.org, surname doe.");
	test_o("What your things?");
	get("My things activity time, aigents, areas, attention period, birth date, caching period, check cycle, clicks, clustering timeout, conversation, cookie domain, cookie name, copypastes, crawl range, currency, daytime, discourse id, discourse key, discourse url, email, email cycle, email login, email notification, email password, email retries, ethereum id, ethereum key, ethereum period, ethereum url, facebook challenge, facebook id, facebook key, facebook notification, facebook token, format, friend, friends, golos id, golos url, google id, google key, google token, googlesearch key, http origin, http port, http secure, http threads, http timeout, http url, ignores, items limit, john, john, language, login count, login time, login token, mail.pop3.starttls.enable, mail.pop3s.host, mail.pop3s.port, mail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.smtp.ssl.enable, mail.smtp.starttls.enable, mail.store.protocol, money, name, news, news limit, number, paid term, paypal id, paypal key, paypal token, paypal url, peer, phone, queries, reddit id, reddit image, reddit key, reddit redirect, reddit token, registration time, reputation conservatism, reputation decayed, reputation default, reputation system, retention period, secret answer, secret question, selections, self, sensitivity threshold, serpapi key, share, shares, sites, slack id, slack key, slack notification, slack token, steemit id, steemit url, store cycle, store path, surname, tcp port, tcp timeout, telegram id, telegram name, telegram notification, telegram offset, telegram token, there, things, things count, time, topics, trusts, trusts limit, twitter id, twitter image, twitter key, twitter key secret, twitter redirect, twitter token, twitter token secret, update time, version, vkontakte id, vkontakte key, vkontakte token, word.");
	say("What times today?");//debug
	get("No.");
//TODO: remove this hack needed to disappear "about" & "context" variables appeared due to implicit search spawned above 
	say("You forget!");//
	get("Ok.");
	test_o("What your things count?");
	test_i("My things count ".($base_things_count + 1).".");
	say("What your things?");
	get();	
	say("What your trusts?");
	get("My trusts john.");
	//save, delete both John-s, reload, test two logins again
	test_o("You save test1.txt!");
	test_i("Ok.");
	test_o("No email doe@john.org, name John, surname Dare.");
	test_i("Ok.");
	test_o("What your things count?");
	test_i("My things count ".($base_things_count).".");	
	say("Your trusts no John.");
	get("Ok.");	
	test_o("No email john@doe.org, name John, surname Doe.");
	test_i("Ok.");
	test_o("What name john email, surname?");
	test_i("No.");
	test_o("You load test1.txt!");
	test_i("Ok.");
	test_o("What your things count?");
	get();
	say("What your things?");
	get();
	test_o("What your things count?");
	test_i("My things count ".($base_things_count + 1).".");
	say("What your trusts?");
	get("My trusts john.");
	test_o("You save test2.txt!");
	test_i("Ok.");
	
	//re-login after re-load
	test_o("My logout.");
	test_i("Ok.");
	test_o("My login.");
	test_i("What your email, name, surname?");
	test_o("My name John, email john@doe.org, surname Doe.");
	test_i("What your birth place?");
	test_o("My birth place 'London'.");
	test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	test_o("What name john email, surname?");
	test_i("John email doe@john.org, surname dare; email john@doe.org, surname doe.");
	
	//TODO: re-syncing profile for mobile/facebook/google 
	/*
	say("What your attention period?");
	get();
	say("What my check cycle, attention period, last check?");
	get();
//	say("Peer has attention period, last check.");
//	get();
//	say("My check cycle 3 hours, attention period 3 days, last check 2015-03-13.");
	say("My check cycle 3 hours, attention period 3 days.");
	get("Ok.");
	say("What my check cycle, attention period, last check?");
	get();
	*/
	//TODO: dealing with trusts of sites and topics
	/**/
	say("My topics x, y.");
	get("Ok.");
	say("My sites www.x, www.y.");
	get("Ok.");
	say("My trusts x, www.x.");
	get("Ok.");
	say("What my trusts?");
	get("Your trusts aigents, www.x, x.");
	say("What my topics?");
	get("Your topics x, y.");
	say("What my topics name?");
	get("Your topics name x; name y.");
	//TODO:
	//say("What my topics (name, trust)?");//=>Int:[john [topics trust]]?
	//get();
	say("What my topics name, trust?");//=>Int:[john [topics trust]]?
	get("Your topics name x, trust true; name y, trust false.");
	say("My trusts no x, no www.x.");
	get("Ok.");
	say("What name x trust?");
	get("X trust false.");
	say("Your reading."); 
	get("Ok. My reading.");
	say("My topics no x, no y.");
	get("Ok.");
	say("My sites no www.x, no www.y.");
	get("Ok.");
	say("No name x or y or www.x or www.y.");
	get("Ok.");
	
	//cleanup and logut	
	say("Your trusts no John.");
	get("Ok.");	
	test_o("No email doe@john.org, name John, surname Dare.");
	test_i("Ok.");
	test_o("No email john@doe.org, name John, surname Doe.");
	test_i("Ok.");
	test_o("My logout.");
	test_i("Ok.");
}

function test_login_areas() {
	global $version;
	global $copyright;
	global $base_things_count;
	
	//register John
	say("My login.");
	get("What your email, name, surname?");
	say("My name John, email john@doe.org, surname Doe.");
	get("What your secret question, secret answer?");
	say("My secret question birth place, secret answer 'London'.");
	get("What your birth place?");
	say("My birth place 'London'.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("What your things?");
	get();
	say("What your things count?");
	get("My things count ".($base_things_count).".");
	say("my areas academgorodok.");
	get("Ok.");
	say("my shares academgorodok.");
	get("Ok.");
	say("what my areas?");
	get("Your areas academgorodok.");
	say("What my areas name, share?");
	get("Your areas name academgorodok, share true.");
	say_site("http://sovok.info",true);
	say_site("http://navigato.ru",true);
	say_thing("roads",true);
	say_thing("forests",true);
	say("there is roads, sources http://navigato.ru, times today, new true, text 'good roads'.");
	say("there is forests, sources http://sovok.info, times today, new true, text 'great forests'.");
	//my logout
	say("My logout.");
	get("Ok.");
	
	//register Doe
	say("My login.");
	get("What your email, name, surname?");
	say("My name Doe, email doe@john.org, surname John.");
	get("What your secret question, secret answer?");
	say("My secret question birth place, secret answer 'London'.");
	get("What your birth place?");
	say("My birth place 'London'.");
	get("Ok. Hello Doe John!\nMy Aigents ".$version.$copyright);
	//my areas (stock market)
	say("my areas stock market.");
	get("Ok.");
	say("my shares stock market.");
	get("Ok.");
	say("what my areas?");
	get("Your areas stock market.");
	say("What my areas name, share?");
	get("Your areas name stock market, share true.");
	//my sites
	say_site("http://www.wired.com",true);
	say_site("http://www.nytimes.com",true);
	//my topics
	say_thing("goog",true);
	say_thing("msft",true);
	//my news
	say("there is goog, sources http://www.wired.com, times today, new true, text 'google goes up'.");
	say("there is msft, sources http://www.nytimes.com, times today, new true, text 'microsoft rocks'.");
	say("My trusts no msft.");
	say("My trusts no http://www.nytimes.com.");
	//my logout
	say("My logout.");
	get("Ok.");

	//get default area data (unareaed)
	say("What new true sources, text, times, trust?");
	get("There sources http://navigato.ru, text good roads, times today, trust false; sources http://sovok.info, text great forests, times today, trust false.");
	say("What my sites name, trust?");
	get("Your sites name http://navigato.ru, trust true; name http://sovok.info, trust true.");
	say("What my topics name, trust?");
	get("Your topics name forests, trust true; name roads, trust true.");
	//change area
	say("my areas stock market.");
	get("Ok.");
	say("What new true sources, text, times, trust?");
	get("There sources http://www.nytimes.com, text microsoft rocks, times today, trust false; sources http://www.wired.com, text google goes up, times today, trust false.");
	say("What my sites name, trust?");
	get("Your sites name http://www.nytimes.com, trust false; name http://www.wired.com, trust true.");
	say("What my topics name, trust?");
	get("Your topics name goog, trust true; name msft, trust false.");
	//reset area
	say("my areas ''.");
	get("Ok.");
	//get unareaed data
	say("What new true sources, text, times, trust?");
	get("There sources http://navigato.ru, text good roads, times today, trust false; sources http://sovok.info, text great forests, times today, trust false.");
	say("What my sites name, trust?");
	get("Your sites name http://navigato.ru, trust true; name http://sovok.info, trust true.");
	say("What my topics name, trust?");
	get("Your topics name forests, trust true; name roads, trust true.");
	//set wrong area
	say("my areas 'eprst'.");
	get("Ok.");
	//get unareaed data
	say("What new true sources, text, times, trust?");
	get("There sources http://navigato.ru, text good roads, times today, trust false; sources http://sovok.info, text great forests, times today, trust false.");
	say("What my sites name, trust?");
	get("Your sites name http://navigato.ru, trust true; name http://sovok.info, trust true.");
	say("What my topics name, trust?");
	get("Your topics name forests, trust true; name roads, trust true.");
	//change area
	say("my areas stock market.");
	get("Ok.");
	say("What new true sources, text, times, trust?");
	get("There sources http://www.nytimes.com, text microsoft rocks, times today, trust false; sources http://www.wired.com, text google goes up, times today, trust false.");
	//change area to academgorodok
	//get area's data again
	say("my areas academgorodok.");
	get("Ok.");
	say("What new true sources, text, times, trust?");
	get("There sources http://navigato.ru, text good roads, times today, trust false; sources http://sovok.info, text great forests, times today, trust false.");
	
	//register
	say("My name Ali, email ali@baba.xxx, surname Baba.");
	get("What your secret question, secret answer?");
	say("My secret question birth place, secret answer 'London'.");
	get("What your birth place?");
	say("My birth place 'London'.");
	get("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
	//get data for academgorodok (now in profile)
	say("What new true, sources http://navigato.ru or http://sovok.info sources, text, times, trust?");
	get("There sources http://navigato.ru, text good roads, times today, trust false; sources http://sovok.info, text great forests, times today, trust false.");
	say("What my sites name, trust?");
	get("Your sites name http://navigato.ru, trust true; name http://sovok.info, trust true.");
	say("What my topics name, trust?");
	get("Your topics name forests, trust true; name roads, trust true.");
	say("what my areas?");
	get("Your areas academgorodok.");
	say("What my areas name, share?");
	get("Your areas name academgorodok, share false.");
	say("New true new false.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
	
	//change area to stock market
	say("my areas stock market.");
	//register
	say("My name Baba, email baba@ali.xxx, surname Ali.");
	get("What your secret question, secret answer?");
	say("My secret question birth place, secret answer 'London'.");
	get("What your birth place?");
	say("My birth place 'London'.");
	get("Ok. Hello Baba Ali!\nMy Aigents ".$version.$copyright);
	say("what my areas?");
	get("Your areas stock market.");
	say("What my areas name, share?");
	get("Your areas name stock market, share false.");
	//get data for stock market (now in profile)
	say("What new true, sources http://www.nytimes.com or http://www.wired.com sources, text, times, trust?");
	get("There sources http://www.nytimes.com, text microsoft rocks, times today, trust false; sources http://www.wired.com, text google goes up, times today, trust false.");
	say("What my sites name, trust?");
	get("Your sites name http://www.nytimes.com, trust false; name http://www.wired.com, trust true.");
	say("What my topics name, trust?");
	get("Your topics name goog, trust true; name msft, trust false.");
	say("New true new false.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
	
	//test the same for unaread registration, with defaul news added too!!!???
	say("my areas 'abvgd'.");
	say("My name Yo, email yo@yo.xxx, surname Yo.");
	get("What your secret question, secret answer?");
	say("My secret question birth place, secret answer 'London'.");
	get("What your birth place?");
	say("My birth place 'London'.");
	get("Ok. Hello Yo Yo!\nMy Aigents ".$version.$copyright);
	say("What my areas?");
	get("Your areas abvgd.");
	say("What my areas name, share?");
	get("Your areas name abvgd, share false.");
	say("What new true, sources http://www.nytimes.com or http://www.wired.com or http://navigato.ru or http://sovok.info sources, text, times, trust?");
	get("There sources http://navigato.ru, text good roads, times today, trust false; sources http://sovok.info, text great forests, times today, trust false.");
	say("What my sites name, trust?");
	get("Your sites name http://navigato.ru, trust true; name http://sovok.info, trust true.");
	say("What my topics name, trust?");
	get("Your topics name forests, trust true; name roads, trust true.");
	say("New true new false.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
	
	//cleanup and logout
	say("My email doe@john.org.");
	get("What your birth place?");
	say("My birth place 'London'.");
	get("Ok. Hello Doe John!\nMy Aigents ".$version.$copyright);
	say_thing("goog",false);
	say_thing("msft",false);
	say_site("http://www.wired.com",false);
	say_site("http://www.nytimes.com",false);
	say("My logout.");
	get("Ok.");
	
	say("My email john@doe.org.");
	get("What your birth place?");
	say("My birth place 'London'.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("You save test3.txt!");
	get("Ok.");
	say_thing("roads",false);
	say_thing("forests",false);
	say_site("http://sovok.info",false);
	say_site("http://navigato.ru",false);
	
	say_site("https://aigents.com",false);//clean post-registration artifacts
	say_site("http://aigents.com",false);
	say("You forget!");
	get("Ok.");
	say("What times today?");
	get("No.");
	
	say("What email john@doe.org areas?");
	get("There areas academgorodok.");
	say("What email doe@john.org areas?");
	get("No. No right.");
	say("What email ali@baba.xxx areas?");
	get("No. No right.");
	say("What email baba@ali.xxx areas?");
	get("No. No right.");
	say("What areas academgorodok email?");
	get("There email ali@baba.xxx; email john@doe.org.");
	say("What areas stock market email?");
	get("There email baba@ali.xxx; email doe@john.org.");
	say("What areas abvgd email?");
	get("There email yo@yo.xxx.");
	say("What shares stock market email?");
	get("There email doe@john.org.");
	
	say("No email yo@yo.xxx, name Yo, surname Yo.");
	get("Ok.");
	say("No email ali@baba.xxx, name Ali, surname Baba.");
	get("Ok.");
	say("No email baba@ali.xxx, name Baba, surname Ali.");
	get("Ok.");
	say("No email doe@john.org, name Doe, surname John.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	
	say("What your things count?");
	get("My things count ".($base_things_count + 1).".");
	say("What your things?");
	get();
	say("Your trusts no John.");
	get("Ok.");
	say("No email john@doe.org, name John, surname Doe.");
	get("Ok.");
	say("No name academgorodok.");
	get("Ok.");
	say("My logout.");
	get("Ok.");
}


function test_login_sessions() {
	global $version;
	global $copyright;
	global $base_things_count;
	
	login();
	say("what my name?");
	get("Your name john.");
	say("what name john surname?");
	get("John surname doe.");
	say("what is john is?");
	get("There is john.");
	say("what is doe is?");
	get("No.");
	$john_cookie = get_cookie();//save session
	set_cookie(null);//reset session
	
	login("doe","doe@john.org","john","q","a");
	say("what my name?");
	get("Your name doe.");
	say("what name john surname?");
	get("John surname doe.");
	say("what name john?");
	get("No. No right.");
	say("what is doe is?");
	get("There is doe.");
	say("what is john?");
	get("No. No right.");
	say("what john surname?");
	get("John surname doe.");
	
	$doe_cookie = get_cookie();//save session
	set_cookie($john_cookie);//restore session
	say("what my name?");
	get("Your name john.");
	say("what is john is?");
	get("There is john.");
	say("what is doe is?");
	get("No. No right.");
	say("what name doe?");
	get("No. No right.");
	say("what name doe email?");
	get("Doe email doe@john.org.");
	say("what email doe@john.org?");
	get("No. No right.");
	say("You forget!");
	get("Ok.");
	say("Your things count?");
	get("My things count ".($base_things_count + 2).".");//count with 2 sessions
	
	set_cookie($doe_cookie);//restore doe's session
	say("my logout");

	set_cookie($john_cookie);//restore john's session
	say("Your things count?");
	get("My things count ".($base_things_count + 1).".");//count with 1 sessions
	say("No name doe.");
	get("Ok.");	
	say("Your things count?");
	get("My things count ".($base_things_count).".");//count with 1 sessions and with no doe
	
	set_cookie($john_cookie);//restore session
	logout();//cleanup john
}


test_init();
test_login_debug();
test_login_new();
test_login_old();
test_login_areas();
test_login_sessions();
test_summary();

?>
