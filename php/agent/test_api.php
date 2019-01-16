<?php
/*
Copyright 2018 Anton Kolonin, Aigents Group

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

include_once("pest.php");

$version = "1.4.6";
$copyright = " Copyright © 2019 Anton Kolonin, Aigents.";

$baseURL = "http://localhost:1180/?";

$basePath = "./";

$cookie;

function url_test_curl( $url ) {
	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, urlencode("http://localhost:8888/?".$url));
	print_r($ch);
	$res = curl_exec($ch);
	print_r($res);
	curl_close($ch);
	return $res;
}

function url_test_pest( $url ) {
	global $baseURL, $cookie;
	$pest = new Pest($baseURL);
	try {
		$url = $baseURL.urlencode($url);
		if (isset($cookie)) {
			$pest->curl_opts[CURLOPT_COOKIE] = $cookie;
		}
		else { 
		}
		$result = $pest->get($url);
		$cookie=$pest->last_headers['set-cookie'];
		unset($pest);
		return $result;
	} catch( Exception $e ) {
		unset($pest);
		throw($e);
	}
}

function ping_test($text) {
	print_r($text."\n");	
	print_r(url_test($text)."\n");	
}

//http://www.php.net/manual/ru/function.curl-setopt.php
function curl_test($url) {
	global $baseURL, $cookie;
	$url = $baseURL.urlencode($url);
	// Get cURL resource
	$curl = curl_init();
	// Set some options - we are passing in a useragent too here
	curl_setopt_array($curl, array(
	CURLOPT_RETURNTRANSFER => 1,
	CURLOPT_URL => $url,
	CURLOPT_HEADER => 1,
	CURLOPT_VERBOSE => 1,
	CURLOPT_TIMEOUT => 60,
	CURLOPT_CONNECTIONTIMEOUT => 60,	
	CURLOPT_USERAGENT => 'Codular Sample cURL Request'	
	));

	if (isset($cookie))
		curl_setopt($curl, CURLOPT_COOKIE, $cookie);	
	
	// Send the request & save response to $resp
	$resp = curl_exec($curl);
	preg_match_all('/^Set-Cookie:\s*([^\r\n]*)/mi', $resp, $m);
	$resp = explode("\r\n\r\n", $resp, 2);
	if (isset($m[1][0]))
		$cookie = $m[1][0];
	// Close request to clear up some resources
	curl_close($curl);
	return $resp[1];
}

function println($str) {
	print($str."\n");
}

$last_message;
$failed = 0;

function test_o($out) {
	global $last_message;
	$last_message = url_test_pest($out);
	if ($last_message != null)
		$last_message = trim($last_message);
	println("SAY:".$out);
}

function test_i($in = null, $alts = null) {
	global $last_message;
	global $failed;
	if ($in != null)
		$in = trim($in);
	if ($in != null && !($in === $last_message || ($alts !=null && in_array($last_message, $alts)))) {
		println("GET:\n".$last_message."\nERROR - MUST BE:\n".$in);
		$failed = $failed + 1;
		exit();		
	}
	else
		println("GET:".$last_message);
}

function test_init() {
	global $failed, $cookie;
	$cookie = "";
	$failed = 0; 
}

function test_summary() {
	global $failed;
	if ($failed > 0)
		println("FAILED: ".$failed);
	else 
		println("SUCCESS!");
}

function test_break() {
	global $failed;
	readline("FAILED: ".$failed.", press key...");
}


function say($in) {
	test_o($in);
}

function get($in = null, $alts = null) {
	test_i($in,$alts);
}

function brk() {
	test_break();
}



//Perfromance timers
$timers = array();

function starttimer($name) {
	global $timers;
	$milliseconds = round(microtime(true) * 1000);
	$timers[$name] = $milliseconds;
}

function gettimer($name) {
	global $timers;
	return $timers[$name];
}

function stoptimer($name) {
	global $timers;
	$milliseconds = round(microtime(true) * 1000);
	$timers[$name] = $milliseconds - gettimer($name);
}

function printtimers() {
	global $timers;
	$keys = array_keys($timers);
	$count = count($keys);
	for ($i = 0; $i < $count; $i++)
	    echo $keys[$i]."=".$timers[$keys[$i]]."\n";
}


//Aggregating helpers

function say_thing($t, $no = true) {
	$no = $no === false ? "no " : "";
	say("My knows ".$no."'".$t."'.");
	say("My trusts ".$no."'".$t."'.");
	if ($no){
		say("Is '".$t."' trust false, new false.");
		say("No there is '".$t."'.");
		say("No name '".$t."'.");
	}
}

function say_site($t, $no = true) {
	$no = $no === false ? "no " : "";
	say("My sites ".$no."'".$t."'.");
	say("My trusts ".$no."'".$t."'.");
	if ($no){
		say("Sources '".$t."' trust false, new false.");
		say("No there sources '".$t."'.");
		say("No there is '".$t."'.");
		say("No name '".$t."'.");
	}
}

function del_news_today(){
	say("Times today new false.");
	say("Times today trust false.");
	say("No there times today.");
}

function login($name = "john", $email = "john@doe.org", $surname = "doe", $question = "q", $answer = "a"){
	global $version, $copyright;
	say("My name ".$name.", email ".$email.", surname ".$surname.".");
	get("What your secret question, secret answer?");
	say("My secret question ".$question.", secret answer ".$answer.".");
	get("What your ".$question."?");
	say("My ".$question." ".$answer.".");
	get("Ok. Hello John Doe!\nMy Aigents " . $version . $copyright);
}

function logout($name = "john"){
	say("Your trusts no " . $name . ".");
	get("Ok.");
	say("No name " . $name . ".");
	get("Ok.");
	say("No there times today.");
	get("Ok.");
	say("What times today?");
	get("There not.");
	say("My logout.");
	get("Ok.");
}

?>
