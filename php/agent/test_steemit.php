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

function test_agent_steemit() {
	global $version;
	global $copyright;

	//registration - you can do this only once, and then store the login and passwod in your database 
	say("My name john, email john@doe.org, surname doe, secret question password, secret answer 123.");
	get("What your password?");
	say("My password 123.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	say("My logout.");
	get("Ok.");
	
	//login - you need to do this in order to get analytics
	say("My email john@doe.org, password 123.");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	
	say("My steemit id akolonin.");
	get("Ok.");
	
	say("You spidering steemit id akolonin!");
	get();
	
	say("Your trusts no john.");
	get("Ok.");
	say("No name john.");
	get("Ok.");
	say("My logout.");
	get("Ok.");	
}
	
//test_agent_steemit();

$s = "text 'quoted' and \"double-quoted\" rocks";
print_r($s . "\n");
print_r(json_encode($s) . "\n");

?>
