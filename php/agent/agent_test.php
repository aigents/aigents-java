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

include_once("agent_basic.php");
include_once("agent_learn.php");
include_once("agent_login.php");
include_once("agent_patt.php");
include_once("agent_sites.php");
include_once("agent_think.php");
include_once("agent_web.php");
include_once("agent_cat.php");
include_once("agent_chat.php");
include_once("agent_reputation.php");
include_once("agent_data.php");

function test_once() {
	global $version;
	global $copyright;
	
	test_init();
	
	//testing site parsing
	test_basic();

	//test clustering, pattern extraction and classification
	test_agent_expereinces();
	test_agent_cluster();
	test_agent_cat();
	
	//test free-text chat capabilities
	test_findchat();
	test_demochat();
	test_freechat();
	test_help();
	test_chat();
	test_groups();
	test_search();
	test_bot();
	test_load();
	
	//test adaptive learning capabilities
	test_agent_learn();
	test_agent_agglomerate();
	test_agent_parse();
	test_agent_site_graph();
	
	//testing random login and registration variations
	test_login_debug();
	test_login_new();
	test_login_old();
	test_login_areas();
	test_login_sessions();
	
	//test pattern matching
	test_agent_patterns();

	//test reputation system capabilities
	test_reputation();

	//testing site parsing
	test_sites();
	test_extractor();
	test_authoring();
	
	//testing associative thinking
	test_agent_think();
	test_agent_think_ex();
	
	//test web processing and parsing
	test_agent_rss();
	test_agent_web();
	
	//test data modeling
	test_data();
	
	test_summary();
}


for ($i=0;$i<2;$i++) test_once();
//for (;;) test_once();
	

?>
