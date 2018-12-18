<?php
/*
Copyright 2018 Anton Kolonin, Aigents Group

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

include_once("test_api.php");

include_once("agent_basic.php");
include_once("agent_learn.php");
include_once("agent_login.php");
include_once("agent_patt.php");
include_once("agent_sites.php");
include_once("agent_think.php");
include_once("agent_web.php");
include_once("agent_cat.php");
include_once("agent_chat.php");

function test_once() {
	global $version;
	global $copyright;
	
	test_init();
	
	//testing site parsing
	test_basic();

	//test adaptive learning capabilities
	test_agent_learn();
	test_agent_agglomerate();
	test_agent_parse();
	test_agent_site_graph();
	
	//testing random login and registration variations
	test_login_new();
	test_login_old();
	test_login_areas();
		
	//test pattern matching
	test_agent_patterns();
	
	//testing site parsing
	test_sites();
	test_extractor();
		
	//test web processing and parsing
	test_agent_web();
	
	//test clistering and classification
	test_agent_cluster();

	//test free-text chat capabilities
	test_chat();
		
	test_summary();
}


for ($i=0;$i<2;$i++) test_once();
//for (;;) test_once();
	

?>
