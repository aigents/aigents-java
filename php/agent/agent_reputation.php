<?php
/*
 * MIT License
 * 
 * Copyright (c) 2019 Stichting SingularityNET
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

function test_reputation() {
	login();
	
	say("reputation network testnet  clear ranks");
	get("Ok.");
	say("reputation network testnet  clear ratings");
	get("Ok.");
	say("reputation network testnet  get ranks  date 2018-10-13");
	get("Ok.");
	say("reputation network testnet  set ranks  date 2018-10-13  id 1 rank 100  id 2 rank 50");
	get("Ok.");
	say("reputation network testnet  get ranks  date 2018-10-13");
	get("Ok.\n1	100\n2	50");
	say("reputation network testnet  add ratings  from 1 type rate to 2 value 100 time 2018-10-14  from 2 type rate to 3 value 50  time 2018-10-14");
	get("Ok.");
	say("reputation network testnet  get ratings  date 2018-10-14  ids 1");
	get("Ok.\n1	rate-s	2	100");
	say("reputation network testnet  get ratings  date 2018-10-14  ids 2");
	get("Ok.\n2	rate-d	1	100\n2	rate-s	3	50");
	say("reputation network testnet  get ranks date 2018-10-14");
	get("Ok.");
	say("reputation network testnet  update ranks default 0.1 date 2018-10-14");
	get("Ok.");
	say("reputation network testnet  get ranks date 2018-10-14");
	get("Ok.\n2	100\n1	67\n3	63");
	say("reputation network testnet  get ranks date 2018-10-14 id 1 id 2");
	get("Ok.\n2	100\n1	67\n");
	say("reputation network testnet  get ranks date 2018-10-14 id 2 id 3");
	get("Ok.\n2	100\n3	63");
	say("reputation network testnet  get ranks since 2018-10-13 until 2018-10-14");
	get("Ok.\n1	100	67\n2	50	100\n3		63");
	say("reputation network testnet  get ranks since 2018-10-13 until 2018-10-14 average");
	get("Ok.\n1	83.5\n2	75.0\n3	63.0");
	say("reputation network testnet  get ranks since 2018-10-13 until 2018-10-14 id 2 id 3");
	get("Ok.\n2	50	100\n3		63");

	//test retention period
	say("Your retention period?");
	get();
	//check that data can be kept
	say("Your retention period 3650.");
	get("Ok.");
	say("Your retention period?");
	get("My retention period 3650.");
	say("You forget!");
	get("Ok.");
	say("reputation network testnet  get ranks since 2018-10-13 until 2018-10-14 id 2 id 3");
	get("Ok.\n2	50	100\n3		63");
	say("reputation network testnet  get ratings  date 2018-10-14  ids 2");
	get("Ok.\n2	rate-d	1	100\n2	rate-s	3	50");

	//check command-line and graph saving
	cmd("java -cp Aigents.jar:bin/* net.webstructor.peer.Reputationer reputation network testnet get ranks since 2018-10-13 until 2018-10-14 id 2 id 3",$out);
	get("2	50	100\n3		63");
	cmd("java -cp Aigents.jar:bin/* net.webstructor.peer.Reputationer reputation network testnet get ratings  date 2018-10-14  ids 2",$out);
	get("2	rate-d	1	100\n2	rate-s	3	50");

	//check that data can be forgotten
	say("Your retention period 30.");
	get("Ok.");
	say("Your retention period?");
	get("My retention period 30.");
	say("You forget!");
	get("Ok.");
	say("reputation network testnet  get ranks since 2018-10-13 until 2018-10-14 id 2 id 3");
	get("Ok.");
	say("reputation network testnet  get ratings  date 2018-10-14  ids 2");
	get("Ok.");
	
	logout();
}

test_init();
test_reputation();
test_summary();

?>
