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

function test_basic() {
global $version;
global $copyright;
global $timeout;
global $base_things_count;

//login, registration, verification
test_o("My login.");
test_i("What your email, name, surname?");
test_o("My name John, email john@doe.org.");
test_i("What your surname?");
test_o("My surname Doe.");
test_i("What your secret question, secret answer?");
test_o("My secret question \"birth place?\", secret answer \"London\".");
test_i("What your 'birth place?'?");
test_o("My 'birth place?' 'London'.");
test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
say("What my login count, registration time?");
get("Your login count 1, registration time today.");
say("What your version?");
get("My version ".$version.".");

//Testing Demo Plugin
//say("ping");
//get("pong");

say("You forget everything!");
get("Ok.");

test_o("My logout.");
test_i("Ok.");
test_o("My login.");
test_i("What your email, name, surname?");
test_o("My email JOHN@DOE.ORG.");
test_i("What your 'birth place?'?");
test_o("My 'birth place?' 'London'.");
test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
say("What my login count, registration time, login time?");
get("Your login count 2, login time today, registration time today.");

test_o("My secret answer?");
test_i("Your secret answer 'London'.");
test_o("My logout.");
test_i("Ok.");
test_o("My login.");
test_i("What your email, name, surname?");
test_o("My name JOHN.");
test_i("What your 'birth place?'?");
//TODO: recognize "What my 'birth place?'?" as "My secret answer?" or "My secret question?"
test_o("What my secret answer?");
test_i("What your verification code? Sent to john@doe.org.");
test_o("My verification code 1234.");
test_i("What your secret question, secret answer?");
test_o("My secret question \"pet name\", secret answer \"Toby\".");
test_i("What your pet name?");
test_o("My pet name Toby.");
test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
test_o("My email doe@john.org.");
test_i("What your verification code? Sent to doe@john.org.");
test_o("My verification code 1234.");
test_i("Ok. Your email doe@john.org.");
say("What my login count, registration time, login time?");
get("Your login count 3, login time today, registration time today.");

//logout and login with new email
test_o("My logout.");
test_i("Ok.");
test_o("My email doe@john.org.");
test_i("What your pet name?");
test_o("My pet name Toby.");
test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
say("What my login count, registration time, login time?");
get("Your login count 4, login time today, registration time today.");

//setting user settings
test_o("My sensitivity threshold 70, update time 9:00.");
test_i("Ok.");
say("What my sensitivity threshold, update time?");
get("Your sensitivity threshold 70, update time 9:00.");
test_o("My check cycle day, retention period 10, sensitivity threshold 80.");
test_i("Ok.");

//test personal properties se/unset
test_o("My phone +79139250058.");
test_i("Ok.");
test_o("What my phone?");
test_i("Your phone +79139250058.");
test_o("My phone ''.");
test_i("Ok.");
test_o("What my phone?");
test_i("Your phone ''.");
test_o("What is peer, name John email?");
test_i("There email doe@john.org.");
test_o("Is peer, name John trust true, email 'a'");
test_i("Ok.");
test_o("What is peer, name John email?");
test_i("There email a.");
test_o("Is peer, name John trust true, email ''");
test_i("Ok.");
test_o("What is peer, name John email?");
test_i("There email ''.");
test_o("Is peer, name John trust true, email 'b'.");
test_i("Ok.");
test_o("What is peer, name John email?");
test_i("There email b.");
test_o("Is peer, name John trust true, email 'c', phone 'x'");
test_i("Ok.");
test_o("What is peer, name John email, phone?");
test_i("There email c, phone x.");
test_o("Is peer, name John trust true, email '', phone ''");
test_i("Ok.");
test_o("What is peer, name John email, phone?");
test_i("There email '', phone ''.");
test_o("Is peer, name John trust true, email 'd', phone 'y'.");
test_i("Ok.");
test_o("What is peer, name John email, phone?");
test_i("There email d, phone y.");
test_o("Is peer, name John trust true, email '', phone ''.");
test_i("Ok.");
test_o("What is peer, name John email, phone?");
test_i("There email '', phone ''.");
test_o("Is peer, name John trust false, email doe@john.org, phone ''.");
test_i("Ok.");

//test access rights personal properties set/unset
test_o("There is peer, name Ali, surname Baba, email ali@baba.net, phone +12345678901.");
test_i("Ok.");
test_o("What is peer, name Ali email, phone?");
test_i("There email ali@baba.net, phone +12345678901.");
test_o("Is peer, name Ali, surname Baba email '', phone ''.");
test_i("Ok.");
test_o("What is peer, surname Baba email, phone?");
test_i("There email '', phone ''.");
test_o("Is peer, name Ali, surname Baba email ali@BABA.com, secret question sesame, secret answer simsim.");
test_i("Ok.");
test_o("Is peer, name Ali, surname Baba secret question '', secret answer ''.");
test_i("No. No right.");
test_o("What is peer, surname Baba email, phone?");
test_i("No. No right.");
test_o("My logout.");
test_i("Ok.");
test_o("My name ali.");
test_i("What your sesame?");
test_o("My sesame simsim.");
test_i("Ok. Hello Ali Baba!\nMy Aigents ".$version.$copyright);
test_o("What is peer, surname Baba email, phone?");
test_i("There email ali@baba.com, phone ''.");
test_o("Is peer, name Ali, surname Baba phone +12345678901.");
test_i("Ok.");
test_o("What is peer, surname Baba email, phone?");
test_i("There email ali@baba.com, phone +12345678901.");
test_o("Is peer, name Ali, surname Baba secret question '', secret answer ''.");
test_i("Ok.");
test_o("My logout.");
test_i("Ok.");
test_o("My email doe@john.org.");
test_i("What your pet name?");
test_o("My pet name Toby.");
test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
test_o("What is peer, surname Baba email, phone?");
test_i("There email ali@baba.com, phone +12345678901.");
test_o("Is peer, name Ali, surname Baba trust true.");
test_i("Ok.");
test_o("No there is peer, name Ali, surname Baba.");
test_i("No. There things.");
test_o("Is peer, name Ali, surname Baba trust false.");
test_i("Ok.");
test_o("No there is peer, name Ali, surname Baba.");
test_i("Ok.");

//logout and can't login with old email
test_o("My logout.");
test_i("Ok.");
test_o("My email john@doe.org.");
test_i("What your name, surname?");

//login and cleanup back to original state (remove peer)
test_o("My email doe@john.org.");
test_i("What your pet name?");
test_o("My pet name Toby.");
test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);

//conversation
test_o("What my retention period, sensitivity threshold, update time, check cycle?");
test_i("Your check cycle day, retention period 10, sensitivity threshold 80, update time 9:00.");

//describe sites
test_o("My sites http://www.weather.com, http://www.accuweather.com.");
test_i("Ok.");
test_o("My sites http://www.accuweather.com, http://www.weather.com.");
test_i("Ok.");
test_o("What my sites?");
test_i("Your sites http://www.accuweather.com, http://www.weather.com.");
test_o("My sites no http://www.accuweather.com, http://weather.yahoo.com.");
test_i("Ok.");
test_o("What my sites?");
test_i("Your sites http://weather.yahoo.com, http://www.weather.com.");

//describe thing classes
test_o("What my sites?");
test_i("Your sites http://weather.yahoo.com, http://www.weather.com.");
test_o("What my topics?");
test_i("No.");
test_o("My topics temperature, storm.");
test_i("Ok.");
test_o("My trusts temperature, storm.");
test_i("Ok.");
test_o("What my topics?");
test_i("Your topics storm, temperature.");
test_o("Temperature has value, scale.");
test_i("Ok.");
test_o("Storm has region.");
test_i("Ok.");
test_o("What temperature has?");
test_i("Temperature has scale, value.");
test_o("What storm has?");
test_i("Storm has region.");
test_o("Temperature patterns 'temperature \$value \$scale', T \$value \$scale, sources http://weather.yahoo.com, http://www.weather.com.");
test_i("Ok.");
test_o("What temperature patterns, sources?");
test_i("Temperature patterns t \$value \$scale, temperature \$value \$scale, sources http://weather.yahoo.com, http://www.weather.com.");

//describe objects
test_o("There is storm, times today, region Altay.");
test_i("Ok.");
test_o("What is storm, times today region?");
test_i("There region altay.");
test_o("What is storm, region Altay times?");
test_i("There times today.");
test_o("There is storm, times today, region Florida.");
test_i("Ok.");

//retrieve objects
test_o("What is storm, times today region?");
test_i("There region altay; region florida.");
test_o("There is http://weather.yahoo.com, times today. There is http://weather.yahoo.com, times yesterday.");
test_i("Ok. Ok.");
test_o("What is http://weather.yahoo.com times?");
test_i("There times today; times yesterday.");
test_o("What times today?");
test_i("There is http://weather.yahoo.com, times today; is storm, region altay, times today; is storm, region florida, times today.");
test_o("What times yesterday?");
test_i("There is http://weather.yahoo.com, times yesterday.");
test_o("What is http://weather.yahoo.com, times today?");
test_i("There is http://weather.yahoo.com, times today.");
test_o("What is http://weather.yahoo.com, times yesterday?");
test_i("There is http://weather.yahoo.com, times yesterday.");
test_o("What is http://weather.yahoo.com, times today or yesterday?");
test_i("There is http://weather.yahoo.com, times today; is http://weather.yahoo.com, times yesterday.");
test_o("No there is http://weather.yahoo.com.");
test_i("Ok.");

//process sites
test_o("My sites http://localtest.com/test.");
test_i("Ok.");
test_o("My trusts http://localtest.com/test.");
test_i("Ok.");

test_o("http://localtest.com/test has text.");
test_i("Ok.");
test_o("There is http://localtest.com/test, times yesterday, text 'This was the old test page!'.");
test_i("Ok.");
test_o("There is http://localtest.com/test, times today, text 'This is the new test page!'.");
test_i("Ok.");
test_o("What is http://localtest.com/test text?");
test_i("There text 'This is the new test page!'; text 'This was the old test page!'.");
test_o("What is http://localtest.com/test times?");
test_i("There times today; times yesterday.");
test_o("What is http://localtest.com/test times, text?");
test_i("There text 'This is the new test page!', times today; text 'This was the old test page!', times yesterday.");

//describe site instances (text,words and read,parse) with timestamps and handle them
test_o("What is http://localtest.com/test, times today text?");
test_i("There text 'This is the new test page!'.");
test_o("There is http://localtest.com/test, times today text 'This is the test page of today!'.");
test_i("Ok.");
test_o("What is http://localtest.com/test, times today text?");
test_i("There text 'This is the test page of today!'.");
test_o("There is http://localtest.com/test, times today text 'This is a test page. The outside temperature is 9 C°.'.");
test_i("Ok.");
test_o("No there is http://localtest.com/test.");
test_i("Ok.");
test_o("What is http://localtest.com/test, times today text?");
test_i("No.");

//Test reading from texts
test_o("Value is number.");
test_i("Ok.");
test_o("What value is?");
test_i("Value is number.");
test_o("You reading temperature in 'This is a test page. The outside temperature is 9 C°.'!");
test_i("My reading temperature in 'This is a test page. The outside temperature is 9 C°.'.");
test_o("What is temperature, times today value, scale?");
test_i("There scale c°, value 9.");
test_o("What is temperature is, times, scale, value?");
test_i("There is temperature, scale c°, times today, value 9.");
test_o("No there is temperature.");
test_i("Ok.");

//TODO: test tolerance for this (extra 'are')
//test_o("What are my sites?");
//test_i("Ok.");

//saving
test_o("You save test1.txt!");
test_i("Ok.");
test_o("What your things?");
test_i("My things activity time, aigents, areas, attention period, birth date, caching period, check cycle, clicks, clustering timeout, conversation, cookie domain, cookie name, copypastes, crawl range, currency, daytime, discourse id, discourse key, discourse url, email, email cycle, email login, email notification, email password, email retries, ethereum id, ethereum key, ethereum period, ethereum url, facebook challenge, facebook id, facebook key, facebook notification, facebook token, format, friend, friends, golos id, golos url, google id, google key, google token, googlesearch key, http origin, http port, http secure, http threads, http timeout, http url, http://localtest.com/test, http://weather.yahoo.com, http://www.accuweather.com, http://www.weather.com, ignores, items limit, john, language, login count, login time, login token, mail.pop3.starttls.enable, mail.pop3s.host, mail.pop3s.port, mail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.smtp.ssl.enable, mail.smtp.starttls.enable, mail.store.protocol, money, name, news, news limit, number, paid term, paypal id, paypal key, paypal token, paypal url, peer, phone, queries, reddit id, reddit image, reddit key, reddit redirect, reddit token, region, registration time, reputation conservatism, reputation decayed, reputation default, reputation system, retention period, scale, secret answer, secret question, selections, self, sensitivity threshold, serpapi key, share, shares, sites, slack id, slack key, slack notification, slack token, steemit id, steemit url, store cycle, store path, storm, surname, t \$value \$scale, tcp port, tcp timeout, telegram id, telegram name, telegram notification, telegram offset, telegram token, temperature, temperature \$value \$scale, text, there, there, there, things, things count, time, topics, trusts, trusts limit, twitter id, twitter image, twitter key, twitter key secret, twitter redirect, twitter token, twitter token secret, update time, value, version, vkontakte id, vkontakte key, vkontakte token, word.");
test_o("What your things count?");
test_i("My things count ".($base_things_count + 14).".");
test_o("What times today is, region?");
test_i("There is storm, region altay; is storm, region florida.");
test_o("Times today times 2000-01-01");//enforce GC
test_i("Ok.");
test_o("You forget!");
test_i("Ok.");
test_o("What your things count?");
test_i("My things count ".($base_things_count + 11).".");
test_o("What times today is, region?");
test_i("No.");
test_o("You load test1.txt!");
test_i("Ok.");
test_o("What your things count?");
test_i("My things count ".($base_things_count + 14).".");
test_o("What your things?");
test_i("My things activity time, aigents, areas, attention period, birth date, caching period, check cycle, clicks, clustering timeout, conversation, cookie domain, cookie name, copypastes, crawl range, currency, daytime, discourse id, discourse key, discourse url, email, email cycle, email login, email notification, email password, email retries, ethereum id, ethereum key, ethereum period, ethereum url, facebook challenge, facebook id, facebook key, facebook notification, facebook token, format, friend, friends, golos id, golos url, google id, google key, google token, googlesearch key, http origin, http port, http secure, http threads, http timeout, http url, http://localtest.com/test, http://weather.yahoo.com, http://www.accuweather.com, http://www.weather.com, ignores, items limit, john, language, login count, login time, login token, mail.pop3.starttls.enable, mail.pop3s.host, mail.pop3s.port, mail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.smtp.ssl.enable, mail.smtp.starttls.enable, mail.store.protocol, money, name, news, news limit, number, paid term, paypal id, paypal key, paypal token, paypal url, peer, phone, queries, reddit id, reddit image, reddit key, reddit redirect, reddit token, region, registration time, reputation conservatism, reputation decayed, reputation default, reputation system, retention period, scale, secret answer, secret question, selections, self, sensitivity threshold, serpapi key, share, shares, sites, slack id, slack key, slack notification, slack token, steemit id, steemit url, store cycle, store path, storm, surname, t \$value \$scale, tcp port, tcp timeout, telegram id, telegram name, telegram notification, telegram offset, telegram token, temperature, temperature \$value \$scale, text, there, there, there, things, things count, time, topics, trusts, trusts limit, twitter id, twitter image, twitter key, twitter key secret, twitter redirect, twitter token, twitter token secret, update time, value, version, vkontakte id, vkontakte key, vkontakte token, word.");

//test creating instances of real site
test_o("My topics test.");
test_i("Ok.");
test_o("My trusts test.");
test_i("Ok.");
test_o("What my topics?");
test_i("Your topics storm, temperature, test.");
test_o("What is http://localtest.com/test?");
test_i("No.");
test_o("What is test?");
test_i("No.");
test_o("What my news?");
test_i("No.");
test_o("What my news text?");
test_i("No.");
test_o("You reading site http://localtest.com/test!");
test_i("My reading site http://localtest.com/test.");
test_o("What sources http://localtest.com/test text?");
test_i("There text temperature 22 c°; text this is a test page.");
test_o("What my news text?");
test_i("Your news text temperature 22 c°; text this is a test page.");
test_o("You reading site http://localtest.com/test/secure!");
test_i("No.");
test_o("What is test?");
test_i("There about page, context this is a, is test, sources http://localtest.com/test, text this is a test page, times today, title 'Aigents Test Page'.");
test_o("What is http://localtest.com/test?");
test_i("There is http://localtest.com/test, text 'this is a test page. the outside temperature is 22 c°.', times today.");
test_o("What is http://localtest.com/test times?");
test_o("There times today.");
//test enforced reading after template change
test_o("You reading site http://localtest.com/test!");
test_i("No.");
test_o("My topics '\$which page'.");
test_i("Ok.");
test_o("My trusts '\$which page'.");
test_i("Ok.");
test_o("You reading site http://localtest.com/test!");
test_i("My reading site http://localtest.com/test.");
test_o("What is '\$which page' sources, text?");
test_i("There sources http://localtest.com/test, text this is a test page.");
test_o("My trusts no '\$which page'.");
test_i("Ok.");
test_o("My topics no '\$which page'.");
test_i("Ok.");
test_o("Is '\$which page' new false.");
test_i("Ok.");
test_o("No there is '\$which page'.");
test_i("Ok.");
test_o("No name '\$which page'.");
test_i("Ok.");

//Dealing with new/news
//TODO: build sensible auto-qualifiers
test_o("What my news text?");
test_i("Your news text temperature 22 c°; text this is a test page.");
test_o("What my news?");
test_i("Your news there, there.");
//TODO: how to filter by times, things and sources precisely
//test_o("What times today or yesterday sources, text, times, trust?");
//test_i("There sources http://localtest.com/test, text temperature 22 с, times today, trust false; sources http://localtest.com/test, text this is a test page, times today, trust false.");
test_o("What sources http://localtest.com/test sources, text, times, trust?");
test_i("There sources http://localtest.com/test, text temperature 22 c°, times today, trust false; sources http://localtest.com/test, text this is a test page, times today, trust false.");
test_o("What new true sources, text, times, trust?");
test_i("There sources http://localtest.com/test, text temperature 22 c°, times today, trust false; sources http://localtest.com/test, text this is a test page, times today, trust false.");
test_o("What new true text, trust?");
test_i("There text temperature 22 c°, trust false; text this is a test page, trust false.");
test_o("Text temperature 22 c° trust true.");
test_i("Ok.");
test_o("What new true text, trust?");
test_i("There text temperature 22 c°, trust true; text this is a test page, trust false.");
test_o("What new true text?");
test_i("There text temperature 22 c°; text this is a test page.");
//TODO:Consider qualifiers like "sources true" or "text true" just checking for existence of any value!!!???
test_o("What trust true, sources http://localtest.com/test text, trust?");
test_i("There text temperature 22 c°, trust true.");
test_o("What new true, trust true text?");
test_i("There text temperature 22 c°.");
test_o("No there is test or temperature or storm.");
test_i("No. There things.");
//TODO: this does not parse as [is {test, temperature, storm}] - fix!
//test_o("Is test or temperature or storm trust false, new false.");
//test_i("Ok.");
//TODO: Java.util.ConcurrentModificationException
//test_o("Times today trust false, new false.");
//test_i("Ok.");
test_o("New true trust false.");
test_i("Ok.");
test_o("What new true text, trust?");
test_i("There text temperature 22 c°, trust false; text this is a test page, trust false.");
test_o("No there is test or temperature or storm.");
test_i("No. There things.");
test_o("Is test, times today, text 'this is a test page' new false.");
test_i("Ok.");
test_o("text temperature 22 c°, times today new false.");
test_i("Ok.");
test_o("What my news?");
test_i("No.");
//TODO - self site time checking
//TODO - time or times!?


//Testing recursive searching
say("My news limit 1.");
get("Ok.");
say("What my news limit?");
get("Your news limit 1.");
test_o("My sites http://localtest.com/en/.");
test_i("Ok.");
test_o("You reading site http://localtest.com/en/!");
test_i("No.");
test_o("My topics 'Android \$info'.");
test_i("Ok.");
test_o("My trusts 'Android \$info'.");
test_i("Ok.");
test_o("You reading site http://localtest.com/en/!");
get("My reading site http://localtest.com/en/.");
say("What name 'Android \$info' path?");
get("Android \$info path '{products}'.");
say("What is 'Android \$info' text, sources?");
get("There sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text 'android ( get on google play ) web ( try online ) ios facebook ( coming )'; sources http://localtest.com/en/products.html, text android ios.");
say("Is 'Android \$info' new false.");
say("No there is 'Android \$info'.");
say("What is 'Android \$info' text, sources?");
get("No.");
say("Name 'Android \$info' path '{contacts}'.");
say("What name 'Android \$info' path?");
get("Android \$info path '{contacts}'.");
say("You reading site http://localtest.com/en/!");
get("My reading site http://localtest.com/en/.");
say("What is 'Android \$info' text, sources?");
get("There sources 'http://ainlconf.ru/materialAigents', text android sep 2014 present at ainl-2014 conference aug 2014 release for web; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text android windows.");
say("You reading site http://localtest.com/en/!");
get("No.");
say("Is 'Android \$info' new false.");
say("No there is 'Android \$info'.");
say("You reading site http://localtest.com/en/!");
get("My reading site http://localtest.com/en/.");
say("What is 'Android \$info' text, sources?");
get("There sources 'http://ainlconf.ru/materialAigents', text android sep 2014 present at ainl-2014 conference aug 2014 release for web; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text android windows.");
say("Is 'Android \$info' new false.");
say("No there is 'Android \$info'.");
say("You reading!");
get("Ok. My reading.");
say("You reading!");
get("No. My reading.");
sleep($timeout);
say("What is 'Android \$info' text, sources?");
get("No.");

//test exhaustive reading
//1st, get by existing non-exhaustive path
say("New true new false.");
say("No there times today.");
say("You forget everything!");
say("My trusts http://localtest.com/en/.");
say("You reading!");
get("Ok. My reading.");
sleep($timeout);
say("What is 'Android \$info' text, sources?");
get("There sources 'http://ainlconf.ru/materialAigents', text android sep 2014 present at ainl-2014 conference aug 2014 release for web; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text android windows.");

//2nd, get by newly learned non-exhaustive path
say("New true new false.");
say("No there times today.");
say("You forget everything!");
say("'Android \$info' path ''.");//reset learned path!
get("Ok.");
say("You reading!");
get("Ok. My reading.");
sleep($timeout);
say("What is 'Android \$info' text, sources?");
get("There sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text 'android ( get on google play ) web ( try online ) ios facebook ( coming )'; sources http://localtest.com/en/products.html, text android ios.");

//3rd, get by newly learned exhaustive path
//TODO:
say("New true new false.");
say("No there times today.");
say("You forget everything!");
say("'Android \$info' path ''.");//reset learned path!
get("Ok.");
say("My news limit 10.");
say("You reading!");
get("Ok. My reading.");
sleep($timeout);
say("What is 'Android \$info' text, sources?");
//get("There sources 'http://ainlconf.ru/materialAigents', text android sep 2014 present at ainl-2014 conference aug 2014 release for web; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text 'android ( get on google play ) web ( try online ) ios facebook ( coming )'; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text android windows; sources http://localtest.com/en/help.html, text android mac/osx; sources http://localtest.com/en/products.html, text android ios.");
get("There sources 'http://ainlconf.ru/materialAigents', text android sep 2014 present at ainl-2014 conference aug 2014 release for web; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text 'android ( get on google play ) web ( try online ) ios facebook ( coming )'; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text 'android at google play ) ( products )'; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text 'android с google play ) ( продукты )'; sources 'https://play.google.com/store/apps/details?id=net.webstructor.android.free', text android windows; sources http://localtest.com/en/help.html, text android mac/osx; sources http://localtest.com/en/products.html, text android ios.");
say("New true new false.");
say("No there times today.");
test_o("My trusts no 'Android \$info'.");
test_i("Ok.");
test_o("My topics no 'Android \$info'.");
test_i("Ok.");
test_o("No name 'Android \$info'.");
test_i("Ok.");
test_o("My sites no http://localtest.com/en/.");
test_i("Ok.");
test_o("My trusts no http://localtest.com/en/.");
test_i("Ok.");
test_o("No there is http://localtest.com/en/products.html or http://localtest.com/en/.");
test_i("Ok.");
test_o("No name http://localtest.com/en/products.html.");
test_i("Ok.");
test_o("No name http://localtest.com/en/.");
test_i("Ok.");
say("No there times today.");
say("You forget!");

//cleanup
test_o("No there is test or temperature or storm.");
test_i("Ok.");
test_o("What is test?");
test_i("No.");
test_o("My trusts no test.");
test_i("Ok.");
test_o("My topics no test.");
test_i("Ok.");
test_o("No name test.");
test_i("Ok.");
test_o("My trusts no http://localtest.com/test.");
test_i("Ok.");
test_o("No there is http://localtest.com/test.");
test_i("Ok.");
test_o("My sites no http://localtest.com/test.");
test_i("Ok.");
test_o("No name http://localtest.com/test.");
test_i("Ok.");
test_o("No name text.");
test_i("Ok.");
test_o("What is http://localtest.com/test times, text?");
test_i("No.");

//logout and login after save and load and then see we are okay
test_o("My logout.");
test_i("Ok.");
test_o("My email doe@john.org.");
test_i("What your pet name?");
test_o("My pet name Toby.");
test_i("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
test_o("What my retention period, sensitivity threshold, update time, check cycle?");
test_i("Your check cycle day, retention period 10, sensitivity threshold 80, update time 9:00.");

//complete cleanup
test_o("My trusts no temperature, no storm.");
test_i("Ok.");
test_o("No there is storm, times today.");
test_i("Ok.");
test_o("My topics no storm.");
test_i("Ok.");
test_o("What my topics?");
test_i("Your topics temperature.");
test_o("My topics no temperature.");
test_i("Ok.");
test_o("What my topics?");
test_i("No.");
test_o("No name temperature, name storm.");
test_i("Ok.");
test_o("No name scale, name value, name region.");
test_i("Ok.");
//test_o("No name number.");
//test_i("Ok.");
test_o("No name http://www.accuweather.com.");
test_i("Ok.");
test_o("No name temperature \$value \$scale, name 't \$value \$scale'.");
test_i("Ok.");

//check removal of things
say("You forget!");
get("Ok.");
test_o("What your things?");
test_i("My things activity time, aigents, areas, attention period, birth date, caching period, check cycle, clicks, clustering timeout, conversation, cookie domain, cookie name, copypastes, crawl range, currency, daytime, discourse id, discourse key, discourse url, email, email cycle, email login, email notification, email password, email retries, ethereum id, ethereum key, ethereum period, ethereum url, facebook challenge, facebook id, facebook key, facebook notification, facebook token, format, friend, friends, golos id, golos url, google id, google key, google token, googlesearch key, http origin, http port, http secure, http threads, http timeout, http url, http://weather.yahoo.com, http://www.weather.com, ignores, items limit, john, language, login count, login time, login token, mail.pop3.starttls.enable, mail.pop3s.host, mail.pop3s.port, mail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.smtp.ssl.enable, mail.smtp.starttls.enable, mail.store.protocol, money, name, news, news limit, number, paid term, paypal id, paypal key, paypal token, paypal url, peer, phone, queries, reddit id, reddit image, reddit key, reddit redirect, reddit token, registration time, reputation conservatism, reputation decayed, reputation default, reputation system, retention period, secret answer, secret question, selections, self, sensitivity threshold, serpapi key, share, shares, sites, slack id, slack key, slack notification, slack token, steemit id, steemit url, store cycle, store path, surname, tcp port, tcp timeout, telegram id, telegram name, telegram notification, telegram offset, telegram token, there, things, things count, time, topics, trusts, trusts limit, twitter id, twitter image, twitter key, twitter key secret, twitter redirect, twitter token, twitter token secret, update time, version, vkontakte id, vkontakte key, vkontakte token, word.");
test_o("What your things count?");
test_i("My things count ".($base_things_count + 2).".");

//TODO: forgetting
test_o("There text 1 yesterday, times yesterday, new true, trust true, sources http://weather.yahoo.com.");
test_i("Ok.");
test_o("There text 2 yesterday, times yesterday, new true, trust false, sources http://weather.yahoo.com.");
test_i("Ok.");
test_o("There text 3 today, times today, new true, trust true, sources http://weather.yahoo.com.");
test_i("Ok.");
test_o("There text 4 today, times today, new true, trust false, sources http://weather.yahoo.com.");
test_i("Ok.");
test_o("What new true text?");
//list 4 news
test_i("There text 1 yesterday; text 2 yesterday; text 3 today; text 4 today.");
test_o("What times today text?");
test_i("There text 3 today; text 4 today.");
test_o("What times yesterday text?");
test_i("There text 1 yesterday; text 2 yesterday.");
//use attention period of 3, forget, list 4 news
test_o("Your attention period 3.");
test_i("Ok.");
test_o("What your attention period?");
test_i("My attention period 3.");
test_o("You forget!");
test_i("Ok.");
test_o("What new true text?");
test_i("There text 1 yesterday; text 2 yesterday; text 3 today; text 4 today.");
//set attention period to 2, forget, list 4 news
test_o("Your attention period 2.");
test_i("Ok.");
test_o("You forget!");
test_i("Ok.");
test_o("What new true text?");
test_i("There text 1 yesterday; text 2 yesterday; text 3 today; text 4 today.");
//set attention period to 1, forget, list 2 news
test_o("Your attention period 1.");
test_i("Ok.");
test_o("You forget!");
test_i("Ok.");
test_o("What new true text?");
test_i("There text 3 today; text 4 today.");
//set attention period to 0, forget, list 2 news
test_o("Your attention period 0.");
test_i("Ok.");
test_o("You forget!");
test_i("Ok.");
test_o("What new true text?");
test_i("There text 3 today; text 4 today.");
//set new and trust to 0, forget, list 0 news
test_o("What sources http://weather.yahoo.com text?");
test_i("There text 3 today; text 4 today.");
test_o("What text 3 today new, trust?");
test_i("There new true, trust true.");
//TODO: fix, not working
//test_o("Sources http://weather.yahoo.com new false, trust false.");
//test_i("Ok.");
//TODO: fix, not working
//test_o("New true trust false, new false.");
//test_i("Ok.");
//TODO: fix, not working
//test_o("text 3 today or 4 today trust false, new false.");
//test_i("Ok.");
test_o("text 3 today trust false, new false.");
test_i("Ok.");
test_o("text 4 today trust false, new false.");
test_i("Ok.");
test_o("Times today times 2000-01-01");//enforce GC
test_i("Ok.");
test_o("You forget!");
test_i("Ok.");
test_o("What text 3 today new, trust?");
test_i("No.");
test_o("What text 4 today new, trust?");
test_i("No.");
test_o("Your attention period 3.");
test_o("What sources http://weather.yahoo.com text?");
test_i("No.");

//unregister
say("Your trusts no John.");
get("Ok.");	
test_o("No email doe@john.org, name John, surname Doe.");
test_i("Ok.");
test_o("No name http://weather.yahoo.com, name http://www.weather.com, name john.");
test_i("Ok.");
test_o("What your things count?");
test_i("No. No right.");//no observer exists to count

//TODO: still need gc implementation and test!?
//check garbage collection
//test_o("You forget!");
//test_i("Ok.");

//final logout
test_o("My logout.");
test_i("Ok.");
test_o("My email doe@john.org.");
test_i("What your name, surname?");

}

test_init();
test_basic();
test_summary();

?>
