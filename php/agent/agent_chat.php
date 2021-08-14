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

function test_chat_init() {
	login();
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

function test_summarize() {
	test_chat_init();
	file_put_contents($basePath."html/article.html","<html><body>Aigents Social and Media Intelligence Platform for Business joins heterogeneous social and online media sources, blockchains and payment systems and couples them with artificial intelligence to find and track changes in the field of information to let its force be with you. Aigents Personal Artificial Intelligence is serving as a magic glass ball in the world of social and online networks to recognize one's preferences, find what they need and help managing connections.</body></html>");
	//summarize <context> [in <text>]
	say("summarize beer in http://localtest.com/article.html");
	get("No.");
	say("summarize media in http://localtest.com/article.html");
	get("Social and media intelligence platform for business joins heterogeneous social and online media sources,");
	say("summarize information in http://localtest.com/article.html");
	get("Blockchains and payment systems and couples them with artificial intelligence to find and track changes in the field of information to let its force be with you.");
	test_chat_cleanup();
}

function test_report() {
	//aigents report test
	test_chat_init();
	//test with NO timeout
	array_map( 'unlink', array_filter((array) glob("reports/*") ) );
	say("my aigents report format json, timeout 0");
	get("Your report is being prepared",null,true);
	sleep(1);
	say("my aigents report format json, timeout 0");
	get("{\"title\":\"Aigents Report for Aigents (beta) - John Doe",null,true);
	say("There text 'weather example', times today, new true, trust true, sources http://weather.yahoo.com.");
	say("There text 'storms example', times today, new true, trust true, sources http://weather.yahoo.com.");
	get("Ok.");
	say("my aigents report format json, timeout 0");
	get("{\"title\":\"Aigents Report for Aigents (beta) - John Doe",null,true);
	say("my aigents report format json, fresh, timeout 0");
	get("Your report is being prepared",null,true);
	sleep(1);
	say("my aigents report format json, timeout 0");
	get("67,\"weather\",1,0,0,1,1",null,true,true);
	//cleanup
	say("times today new false, trust false");
	get("Ok.");
	say("no there times today");
	get("Ok.");
	//test with default timeout
	array_map( 'unlink', array_filter((array) glob("reports/*") ) );
	say("my aigents report format json");
	get("{\"title\":\"Aigents Report for Aigents (beta) - John Doe",null,true);
	say("There text 'weather example', times today, new true, trust true, sources http://weather.yahoo.com.");
	say("There text 'storms example', times today, new true, trust true, sources http://weather.yahoo.com.");
	get("Ok.");
	say("my aigents report format json");
	get("{\"title\":\"Aigents Report for Aigents (beta) - John Doe",null,true);
	say("my aigents report format json, fresh");
	get("67,\"weather\",1,0,0,1,1",null,true,true);
	//cleanup
	say("times today new false, trust false");
	get("Ok.");
	test_chat_cleanup();
}

function test_findchat() {
	//find-object demo chat script
	test_chat_init();
/*
	//TODO json/graphql statements/queries
			{"человек":"мария ивановна петрова", "комната":"312", "отдел":"бухгалтерия"}
			{"человек":"мария ивановна сидорова", "комната":"123", "отдел":"кадров"}.
			{"товар":"кроссовки", "назначение":"для туризма", "магазин":"рыбалка и охота"}
			{"товар":"кроссовки", "назначение":"для бега", "магазин":"спортмастер"} 
			{"товар":"кроссовки", "назначение":"для бега", "магазин":"чемпион"}
			{"текст":"трамп объявил санкции", "дата":"сегодня", "источник":"lenta"}
			{"текст":"трамп обвинил макрона", "дата":"сегодня", "источник":"kommersant"}
			{"текст":"трамп обвинил меркель", "дата":"вчера", "источник":"lenta"}
			{"текст":"трамп поздравил путина", "дата":"вчера", "источник":"kommersant"}
			{"текст":"трамп обвинил байдена", "дата":"вчера", "источник":"kommersant"}
			{"текст":"трамп выступил в конгрессе", "дата":"2020-08-07", "источник":"lenta"}
			{"текст":"трамп отправился в поездку", "дата":"2020-08-07", "источник":"kommersant"}
			
text:
there name сотрудник, has человек, комната, отдел.
there name покупка, has товар, назначение, магазин, размер.
there name новость, has текст, дата, источник.
there name вылет, has рейс, аэропорт, авиакомпания, терминал, время, сектор, стойка, выход.
there is сотрудник, trust true, человек мария ивановна петрова, комната 312, отдел бухгалтерия.
there is сотрудник, trust true, человек мария ивановна сидорова, комната 123, отдел кадров.
there is покупка, trust true, товар кроссовки, назначение для туризма, магазин рыбалка и охота, размер 42.
there is покупка, trust true, товар кроссовки, назначение для бега, магазин спортмастер, размер 42.
there is покупка, trust true, товар кроссовки, назначение для бега, магазин чемпион, размер 42.
there is покупка, trust true, товар трусы, назначение для плавания, магазин чемпион, размер 42.
there is покупка, trust true, товар трусы, назначение для бега, магазин чемпион, размер 42.
there is покупка, trust true, товар трусы, назначение для плавания, магазин чемпион, размер 44
there is покупка, trust true, товар трусы, назначение для бега, магазин чемпион, размер 44.
there is покупка, trust true, товар трусы, назначение для плавания, магазин спортмастер, размер 42.
there is покупка, trust true, товар трусы, назначение для бега, магазин спортмастер, размер 42.
there is покупка, trust true, товар трусы, назначение для плавания, магазин спортмастер, размер 44.
there is покупка, trust true, товар трусы, назначение для бега, магазин спортмастер, размер 44.
there is вылет, trust true, рейс 311, аэропорт париж, авиакомпания аэрофлот, терминал 12, время 20:10, сектор A, стойка 32, выход 15.
there is вылет, trust true, рейс 312, аэропорт лондон, авиакомпания аэрофлот, терминал 12, время 20:20, сектор A, стойка 33, выход 16.
there is вылет, trust true, рейс 313, аэропорт варшава, авиакомпания аэрофлот, терминал 12, время 20:30, сектор A, стойка 34, выход 17.
there is вылет, trust true, рейс 314, аэропорт берлин, авиакомпания аэрофлот, терминал 12, время 20:40, сектор A, стойка 35, выход 18.
there is вылет, trust true, рейс 315, аэропорт лондон, авиакомпания аэрофлот, терминал 12, время 21:50, сектор A, стойка 33, выход 16.
there is вылет, trust true, рейс 411, аэропорт берлин, авиакомпания ажур, терминал 12, время 20:10, сектор A, стойка 11, выход 25.
there is вылет, trust true, рейс 413, аэропорт новосибирск, авиакомпания ажур, терминал 12, время 20:30, сектор A, стойка 13, выход 27.
there is вылет, trust true, рейс 414, аэропорт берлин, авиакомпания ажур, терминал 12, время 20:50, сектор A, стойка 14, выход 28.
*/	
	//input data
	say("there name сотрудник, has человек, комната, отдел.");
	say("there name покупка, has товар, назначение, магазин, размер.");
	say("there name новость, has текст, дата, источник.");
	say("there name вылет, has рейс, аэропорт, авиакомпания, терминал, время, сектор, стойка, выход.");
	
	say("there is сотрудник, человек мария ивановна петрова, комната 312, отдел бухгалтерия.");
	say("there is сотрудник, человек мария ивановна сидорова, комната 123, отдел кадров.");
	say("there is покупка, товар кроссовки, назначение для туризма, магазин рыбалка и охота.");
	say("there is покупка, товар кроссовки, назначение для бега, магазин спортмастер.");
	say("there is покупка, товар кроссовки, назначение для бега, магазин чемпион.");
	say("there is новость, текст трамп объявил санкции, дата сегодня, источник lenta.");
	say("there is новость, текст трамп обвинил макрона, дата сегодня, источник kommersant.");
	say("there is новость, текст трамп обвинил меркель, дата вчера, источник lenta.");
	say("there is новость, текст трамп поздравил путина, дата вчера, источник kommersant.");
	say("there is новость, текст трамп обвинил байдена, дата вчера, источник kommersant.");
	say("there is новость, текст трамп выступил в конгрессе, дата 2020-08-07, источник lenta.");
	say("there is новость, текст трамп отправился в поездку, дата 2020-08-07, источник kommersant.");
	//check data presense
	say("what is сотрудник?");
	get("There is сотрудник, комната 123, отдел кадров, человек мария ивановна сидорова; is сотрудник, комната 312, отдел бухгалтерия, человек мария ивановна петрова.");
	say("what is покупка?");
	get("There is покупка, магазин рыбалка и охота, назначение для туризма, товар кроссовки; is покупка, магазин спортмастер, назначение для бега, товар кроссовки; is покупка, магазин чемпион, назначение для бега, товар кроссовки.");
	//setup session
	say("my language russian");
	get("Ok.");
	
	//basic search
	say("где сидит петрова");
	get("Комната 312, отдел бухгалтерия, человек мария ивановна петрова?");
	say("да");
	get("Ok.");
	say("мне нужна Мария Ивановна");
	get("Отдел бухгалтерия или кадров?");
	say("из кадров");
	get("Комната 123, отдел кадров, человек мария ивановна сидорова?");
	say("Да");
	get("Ok.");
	
	//extended product search
	say("ХОЧУ КУПИТЬ КРОССОВКИ");
	get("Назначение для бега или для туризма?");
	say("для счастья");
	get("Назначение для бега или для туризма?");
	say("для туризма");
	get("Магазин рыбалка и охота, назначение для туризма, товар кроссовки?");
	say("нет");
	get("Так что?");
	say("что нибудь для бега");
	get("Магазин спортмастер или чемпион?");
	say("мастер");
	get("Магазин спортмастер или чемпион?");
	say("Спортмастер");
	get("Магазин спортмастер, назначение для бега, товар кроссовки?");
	say("да");
	get("Ok.");

	//news search
	say("меня интересует президент трамп");
	get("Источник kommersant или lenta?");
	say("kommersant");
	get("Дата 2020-08-07 или вчера или сегодня?");
	say("вчера");
	get("Текст трамп обвинил байдена или трамп поздравил путина?");
	say("путина");
	get("Дата вчера, источник kommersant, текст трамп поздравил путина?");
	say("да");
	get("Ok.");

    // making sure that sentiment is ignored when searching things
    say("новость has sentiment");
	say("текст трамп объявил санкции sentiment 1.");
	say("текст трамп обвинил макрона sentiment 1.");
	say("текст трамп обвинил меркель sentiment 1.");
	say("текст трамп поздравил путина sentiment 1.");
	say("текст трамп обвинил байдена sentiment 2.");
	say("текст трамп выступил в конгрессе sentiment 2.");
	say("текст трамп отправился в поездку sentiment 2.");
	say("what текст трамп отправился в поездку текст, sentiment?");
	get("There sentiment 2, текст трамп отправился в поездку.");
	say("what sentiment 1?");
	get("There is новость, sentiment 1, дата вчера, источник kommersant, текст трамп поздравил путина; is новость, sentiment 1, дата вчера, источник lenta, текст трамп обвинил меркель; is новость, sentiment 1, дата сегодня, источник kommersant, текст трамп обвинил макрона; is новость, sentiment 1, дата сегодня, источник lenta, текст трамп объявил санкции.");
	say("меня интересует президент трамп");
	get("Источник kommersant или lenta?");
	say("lenta");
	get("Дата 2020-08-07 или вчера или сегодня?");
	say("вчера");
	get("Дата вчера, источник lenta, текст трамп обвинил меркель?");
	say("Да");
	get("Ok.");
	
	//variations
	say("нужно что-нибудь для туризма или для бега");
	get("Магазин рыбалка и охота или спортмастер или чемпион?");
	say("для бега");
	get("Магазин рыбалка и охота или спортмастер или чемпион?");
	say("чемпион");
	get("Магазин чемпион, назначение для бега, товар кроссовки?");
	say("да");
	get("Ok.");

	say("нужно что-нибудь для туризма или для бега");
	get("Магазин рыбалка и охота или спортмастер или чемпион?");
	say("для бега");
	get("Магазин рыбалка и охота или спортмастер или чемпион?");
	say("нет");
	get("Так что?");

//TODO resolve товар and назначение at once 
	say("Хочу кроссовки для бега");
	get("Магазин рыбалка и охота или спортмастер или чемпион?");
	say("бега");
	get("Магазин рыбалка и охота или спортмастер или чемпион?");
	say("быстроном");
	get("Магазин рыбалка и охота или спортмастер или чемпион?");
	say("чемпион");
	get("Магазин чемпион, назначение для бега, товар кроссовки?");
	say("да");

	//don't search for unique items, confirm at once
	say("Хочу что-нибудь для туризма");
	get("Магазин рыбалка и охота, назначение для туризма, товар кроссовки?");
	say("да");
	get("Ok.");
	
	//don't ask for non-cardinal attributes like "is"
	say("Что можно купить в магазине спортмастер?");
	get("Магазин спортмастер, назначение для бега, товар кроссовки?");
	say("да");
	get("Ok.");
	
	say("no there is покупка.");
	say("there is покупка, товар кроссовки, назначение для туризма, магазин рыбалка и охота, размер 42.");
	say("there is покупка, товар кроссовки, назначение для бега, магазин спортмастер, размер 42.");
	say("there is покупка,  товар кроссовки, назначение для бега, магазин чемпион, размер 42.");
	say("there is покупка, товар трусы, назначение для плавания, магазин чемпион, размер 42.");
	say("there is покупка, товар трусы, назначение для бега, магазин чемпион, размер 42.");
	say("there is покупка, товар трусы, назначение для плавания, магазин чемпион, размер 44.");
	say("there is покупка, товар трусы, назначение для бега, магазин чемпион, размер 44.");
	say("there is покупка, товар трусы, назначение для плавания, магазин спортмастер, размер 42.");
	say("there is покупка, товар трусы, назначение для бега, магазин спортмастер, размер 42.");
	say("there is покупка, товар трусы, назначение для плавания, магазин спортмастер, размер 44.");
	say("there is покупка, товар трусы, назначение для бега, магазин спортмастер, размер 44.");
	say("Нужно что-нибудь для бега");
	get("Размер 42 или 44?");
	say("44");
	get("Магазин спортмастер или чемпион?");
	say("спортмастер");
	get("Магазин спортмастер, назначение для бега, размер 44, товар трусы?");
	say("да");
	get("Ok.");

	//airport example
	say("there is вылет, рейс 311, аэропорт париж, авиакомпания аэрофлот, терминал 12, время 20:10, сектор A, стойка 32, выход 15.");
	say("there is вылет, рейс 312, аэропорт лондон, авиакомпания аэрофлот, терминал 12, время 20:20, сектор A, стойка 33, выход 16.");
	say("there is вылет, рейс 313, аэропорт варшава, авиакомпания аэрофлот, терминал 12, время 20:30, сектор A, стойка 34, выход 17.");
	say("there is вылет, рейс 314, аэропорт берлин, авиакомпания аэрофлот, терминал 12, время 20:40, сектор A, стойка 35, выход 18.");
	say("there is вылет, рейс 315, аэропорт лондон, авиакомпания аэрофлот, терминал 12, время 21:50, сектор A, стойка 33, выход 16.");
	say("there is вылет, рейс 411, аэропорт берлин, авиакомпания ажур, терминал 12, время 20:10, сектор A, стойка 11, выход 25.");
	say("there is вылет, рейс 413, аэропорт новосибирск, авиакомпания ажур, терминал 12, время 20:30, сектор A, стойка 13, выход 27.");
	say("there is вылет, рейс 414, аэропорт берлин, авиакомпания ажур, терминал 12, время 20:50, сектор A, стойка 14, выход 28.");
	
	say("лечу в париж");
	get("Авиакомпания аэрофлот, аэропорт париж, время 20:10, выход 15, рейс 311, сектор a, стойка 32, терминал 12?");
	say("да");
	get("Ok.");
//TODO: fix it - appears like complex query!?
	//say("у меня рейс в берлин");
	say("рейс в берлин");
	get("Авиакомпания ажур или аэрофлот?");
	say("ажур");
	get("Рейс 411 или 414?");
	say("414");
	get("Авиакомпания ажур, аэропорт берлин, время 20:50, выход 28, рейс 414, сектор a, стойка 14, терминал 12?");
	say("да");
	get("Ok.");
	say("я лечу в лондон");
	get("Рейс 312 или 315?");
	say("315");
	get("Авиакомпания аэрофлот, аэропорт лондон, время 21:50, выход 16, рейс 315, сектор a, стойка 33, терминал 12?");
	say("да");
	get("Ok.");

	//case of no selection possible
	say("no there is покупка.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	say("there is покупка,  товар кроссовки, назначение для медленного бега, магазин чемпион, размер 42.");
	say("there is покупка, товар кроссовки, назначение для бега трусцой, магазин чемпион, размер 42.");
	say("Нужно что-нибудь для бега");
	get("Назначение для бега трусцой или для быстрого бега или для медленного бега?");
	say("трусцой бегаю");
	get("Магазин чемпион, назначение для бега трусцой, размер 42, товар кроссовки?");
	say("да");
	get("Ok.");
	
	//parse schema definition and filling in one message
	say("there name книга, has автор, название, издательство. there is книга, автор петр самойлов, название бегущая по гвоздям, издательство южный буклет. there is книга, автор самуил петров, название летящая по верхам, издательство северный привет.");
	get("Ok. Ok. Ok.");
	say("what is книга?");
	get("There is книга, автор петр самойлов, издательство южный буклет, название бегущая по гвоздям; is книга, автор самуил петров, издательство северный привет, название летящая по верхам.");
	
	//testing duplication realted bugs
	say("no there is покупка.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	get("Ok.");
	say("there is покупка,  товар кроссовки, назначение для медленного бега, магазин чемпион, размер 42.");
	get("Ok.");
	say("there is покупка, товар кроссовки, назначение для бега трусцой, магазин чемпион, размер 42.");
	get("Ok.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	get("No.");
	say("there is покупка,  товар кроссовки, назначение для медленного бега, магазин чемпион, размер 42.");
	get("No.");
	say("there is покупка, товар кроссовки, назначение для бега трусцой, магазин чемпион, размер 42.");
	get("No.");
	say("Нужно что-нибудь для бега");
	get("Назначение для бега трусцой или для быстрого бега или для медленного бега?");
	say("быстрого");
	get("Магазин чемпион, назначение для быстрого бега, размер 42, товар кроссовки?");
	say("да");
	get("Ok.");
	
	say("no there is покупка.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42. there is покупка,  товар кроссовки, назначение для медленного бега, магазин чемпион, размер 42. there is покупка, товар кроссовки, назначение для бега трусцой, магазин чемпион, размер 42.");
	get("Ok. Ok. Ok.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42. there is покупка,  товар кроссовки, назначение для медленного бега, магазин чемпион, размер 42. there is покупка, товар кроссовки, назначение для бега трусцой, магазин чемпион, размер 42. there is покупка, товар кроссовки, назначение для плавания, магазин моряк, размер 41. ");
	get("No. No. No. Ok.");
	say("Нужно что-нибудь для бега");
	get("Назначение для бега трусцой или для быстрого бега или для медленного бега?");
	say("быстрого");
	get("Магазин чемпион, назначение для быстрого бега, размер 42, товар кроссовки?");
	say("да");
	get("Ok.");
	
	say("no there is покупка.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	get("Ok.");
	say("there is покупка, trust true, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	get("Ok.");
	say("Нужно что-нибудь для бега");
	get("Магазин чемпион, назначение для быстрого бега, размер 42, товар кроссовки?");
	say("да");
	get("Ok.");
	say("is покупка trust false.");//cleanup trust
	get("Ok.");
	
	say("no there is покупка.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42. there is покупка,  товар кроссовки, назначение для медленного бега, магазин чемпион, размер 42. there is покупка, товар кроссовки, назначение для бега трусцой, магазин чемпион, размер 42.");
	get("Ok. Ok. Ok.");
	say("there is покупка, trust true, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42. there is покупка, trust true, товар кроссовки, назначение для медленного бега, магазин чемпион, размер 42. there is покупка, trust true, товар кроссовки, назначение для бега трусцой, магазин чемпион, размер 42.");
	get("Ok. Ok. Ok.");
	say("Нужно что-нибудь для бега");
	get("Назначение для бега трусцой или для быстрого бега или для медленного бега?");
	say("быстрого");
	get("Магазин чемпион, назначение для быстрого бега, размер 42, товар кроссовки?");
	say("да");
	get("Ok.");
	say("is покупка trust false.");//cleanup trust
	get("Ok.");

	//TODO: prevent duplication?
	say("no there is покупка.");
	get("Ok.");
	say("what is покупка");
	get("No.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	get("Ok.");
	say("there is покупка, trust true, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	//say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
//TODO: should duplication with extra trust be prevented here!?
	get("Ok.");
	say("what is покупка");
	get("There is покупка, магазин чемпион, назначение для быстрого бега, размер 42, товар кроссовки; is покупка, магазин чемпион, назначение для быстрого бега, размер 42, товар кроссовки.");
	say("what is покупка trust");
	get("There trust false; trust true.");
	say("what is покупка, trust true trust");
	get("There trust true.");
	say("is покупка, trust true trust false.");
	get("Ok.");
	say("what is покупка, trust true trust");
	get("No.");
	say("no there is покупка.");
	get("Ok.");

	say("no there is покупка.");
	get("Ok.");
	say("what is покупка");
	get("No.");
	say("there is покупка, trust true, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	get("Ok.");
	say("there is покупка, товар кроссовки, назначение для быстрого бега, магазин чемпион, размер 42.");
	get("No.");//as expected - not added
	say("what is покупка");
	get("There is покупка, магазин чемпион, назначение для быстрого бега, размер 42, товар кроссовки.");//as expected - single item
	say("is покупка trust false.");//cleanup trust
	get("Ok.");
	
	test_chat_cleanup();
}

function test_demochat() {
	//main demo chat script
	test_chat_init();
	say("Hi");
	get("No.");
	say("Hello!");
	get("No.");
	say("There patterns hi, hello, whatsup, greeting, responses 'hi!', 'hello!', 'whatsup?', 'greeting!', trust true.");
	get("Ok.");
	say("There patterns 'how are you', 'whatsup', 'howdy', responses 'I am fine, thanks! And you?', 'I am great. What about you?', trust true.");
	get("Ok.");
	say("hi");
	get("Greeting!",array("Hi!","Hello!","Whatsup?"));
	say("How are you doing?");
	get("I am fine, thanks! And you?\n😊",array("I am great. What about you?\n😊"));
	say("I am fine as well!");
	get("No.");
	say("There patterns 'i am {ok okay fine good great}', responses 'Glad to hear that!', 'Good for you.', trust true.");
	get("Ok.");
	say("There patterns 'am not {ok okay good}', 'am {bad sad nervous}', responses 'Sorry about that!', 'Can I help you?', trust true.");
	get("Ok.");
	say("There patterns 'about yourself', 'who are you', responses 'I am a chatbot', 'I am an agent of artificial intelligence.', trust true.");
	get("Ok.");
	say("How are you doing?");
	get("I am fine, thanks! And you?\n😊",array("I am great. What about you?\n😊"));
	say("I am okay!");
	get("Glad to hear that!\n😊", array("Good for you.\n😊"));
	say("I am just ok!");
	get("Glad to hear that!\n😊", array("Good for you.\n😊"));
	say("I am pretty much fine!");
	get("Glad to hear that!\n😊", array("Good for you.\n😊"));
	say("I am nervous");
	get("Can I help you?\n😞",array("Sorry about that!\n😞"));
	say("Tell me about yourself");
	get("I am a chatbot.",array("I am an agent of artificial intelligence."));
	say("What's a chatbot?");
	get("No.");
	say("There patterns 'chatbot', 'chat-bot', 'chat bot', responses 'It is kind of artificial intelligence.', 'chat-bot it is a software mimicking human ability to chat', trust true.");
	get("Ok.");
	say("There patterns 'what {ai [arificial intelligence]}', responses 'AI or artificial intelligence is artificially created ability to behave intelligently, fair enough?', trust true.");
	get("Ok.");
	say("There patterns '{bye cheers bye-bye [good bye]}', responses 'Talk to you later', 'Cheers!', 'See you soon!', trust true.");
	get("Ok.");
	say("What's a chatbot?");
	get("Chat-bot it is a software mimicking human ability to chat.",array("It is kind of artificial intelligence."));
	say("Thanks, good bye now");
	get("Cheers!\n😊",array("Talk to you later.\n😊","See you soon!\n😊"));
	say("What is a chatbot?");
	get("Chat-bot it is a software mimicking human ability to chat.",array("It is kind of artificial intelligence."));
	say("What is a chatbot");
	get("Chat-bot it is a software mimicking human ability to chat.",array("It is kind of artificial intelligence."));
	say("What does arificial intelligence mean?");
	get("AI or artificial intelligence is artificially created ability to behave intelligently, fair enough?\n😊");
	test_chat_cleanup();
	
	//test demo chat script issues
	test_chat_init();
	say("There patterns robot destiny, responses creating other robots, 'joy and well-being', trust true.");
	say("what patterns robot destiny?");
	get("There patterns robot destiny, responses creating other robots, joy and well-being.");
	say("what responses creating other robots?");
	get("There patterns robot destiny, responses creating other robots, joy and well-being.");
	say("what responses creating other robots patterns?");
	get("There patterns robot destiny.");
	say("what patterns robot destiny responses?");
	get("There responses creating other robots, joy and well-being.");
	say("what is the robot destiny?");
	get("Joy and well-being.\n😊",array("Creating other robots.\n😊"));
	//'joy and happiness' is quoted because of 'and' !!!
	say("There patterns human destiny, responses creating robots, 'joy and happiness', trust true.");
	say("what is the human destiny?");
	get("Joy and happiness.\n😊",array("Creating robots.\n😊"));
	say("And what is the human destiny?");
	get("Joy and happiness.\n😊",array("Creating robots.\n😊"));
	say("And what is the human destiny");
	get("Joy and happiness.\n😊",array("Creating robots.\n😊"));
	say("what patterns 'human destiny' responses?");
	get("There responses creating robots, joy and happiness.");
	say("what patterns human destiny responses?");
	get("There responses creating robots, joy and happiness.");
	say("there text 'everyone talks about the artificial intelligence', sources 'http://news.mit.edu/topic/quest-intelligence', trust true");
	say("Tell us something about artificial intelligence?");
	get("Everyone talks about the artificial intelligence http://news.mit.edu/topic/quest-intelligence");
	say("Tell us something about artificial intelligence");
	get("Everyone talks about the artificial intelligence http://news.mit.edu/topic/quest-intelligence");
	say("there patterns 'water', responses 'Of the liquid surface fresh water, 87% is contained in lakes, 11% in swamps, and only 2% in rivers', trust true");  
	say("there text 'Meat is animal flesh that is eaten as food. Humans have hunted and killed animals for meat since prehistoric times.', sources 'https://en.wikipedia.org/wiki/Meat', trust true");
	say("I want to drink water. What should I do?");
	get("Of the liquid surface fresh water, 87% is contained in lakes, 11% in swamps, and only 2% in rivers.\n😊");
	say("I want to eat meat. What should I do?");
	get("Meat is animal flesh that is eaten as food. https://en.wikipedia.org/wiki/Meat");
	say("What does chatbot mean?");
	get("No.");
	say("There patterns '{chatbot chat-bot [chat-bot]} ?', '{chatbot chat-bot [chat-bot]} mean ?', responses 'chat-bot it is a software mimicking human ability to chat', 'It is kind of artificial intelligence'.");
	get("Ok.");
	say("What does chatbot mean?");
	get("Chat-bot it is a software mimicking human ability to chat.",array("It is kind of artificial intelligence."));
	test_chat_cleanup();
}

function test_freechat() {
	global $version, $copyright, $base_things_count;
	/**/
	//test registration and unregistration of a real non-test user
	say("Login.");
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
	get("My things activity time, aigents, areas, attention period, birth date, caching period, check cycle, clicks, clustering timeout, conversation, cookie domain, cookie name, copypastes, crawl range, currency, daytime, discourse id, discourse key, discourse url, email, email cycle, email login, email notification, email password, email retries, ethereum id, ethereum key, ethereum period, ethereum url, facebook challenge, facebook id, facebook key, facebook notification, facebook token, format, friend, friends, golos id, golos url, google id, google key, google token, googlesearch key, http origin, http port, http secure, http threads, http timeout, http url, ignores, items limit, john, language, login count, login time, login token, mail.pop3.starttls.enable, mail.pop3s.host, mail.pop3s.port, mail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.smtp.ssl.enable, mail.smtp.starttls.enable, mail.store.protocol, money, name, news, news limit, number, paid term, paypal id, paypal key, paypal token, paypal url, peer, phone, queries, reddit id, reddit image, reddit key, reddit redirect, reddit token, registration time, reputation conservatism, reputation decayed, reputation default, reputation system, retention period, rudeness threshold, secret answer, secret question, selections, self, sensitivity threshold, sentiment logarithmic, sentiment maximized, sentiment threshold, serpapi key, share, shares, sites, slack id, slack key, slack notification, slack token, steemit id, steemit url, store cycle, store path, surname, tcp port, tcp timeout, telegram id, telegram name, telegram notification, telegram offset, telegram token, there, things, things count, time, topics, trusts, trusts limit, twitter id, twitter image, twitter key, twitter key secret, twitter redirect, twitter token, twitter token secret, update time, version, vkontakte id, vkontakte key, vkontakte token, word.");
	say("Your things count?");
	get("My things count ".$base_things_count.".");
	logout();
//TODO: move out the above to login test
	
	//test contextual refinement
	//TODO
	/*
			>Horses eat oats and hay.
			Ок.
			>Horses can swim.
			Ok.
			>Horses can neigh.
			Ok.
			>Volga rises in the Valdai Hills and flows into the Caspian Sea.
			Ok.
			>What do horses eat?
			Horses eat oats and hay.
			>What can horses do?
			Horses can swim.
			>What can horses do?
			Horses can neigh.
			>Where does the Volga rise?
			Volga rises in the Valdai Hills.
			>Where does the Volga flow?
			Volga flows into the Caspian Sea.
	//say("There text 'Horses eat oats and hay.', trust true.");
	say("There text 'Horses eat oats.', trust true.");
	say("There text 'Horses eat hay.', trust true.");
	say("There text 'Horses can swin.', trust true.");
	say("There text 'Horses can neigh.', trust true.");
	*/
	
	//test search-based replies
	login();
	say("There text 'Home, sweet home', is http://home.org.");
	say("There text 'Outer space is a home for aliens', is http://aliens.org.");
	say("There text 'Some aliens may be our friends', is http://enemies.org.");
	say("aliens home");
	get("Outer space is a home for aliens http://aliens.org");
	test_chat_cleanup();
	
	//test search-based replies with summary
	login();
	say("There text 'Homeland is motheland', is http://home.org.");
	say("There text 'We live in the universe. The universe is the homeland of the extraterrestrial forms of life. These are called aliens. The aliens are our friends.', is http://aliens.org.");
	say("There text 'Some aliens may be our friends', is http://enemies.org.");
	say("aliens homeland");
	get("The universe is the homeland of the extraterrestrial forms of life. These are called aliens. http://aliens.org");
	say("Where is the homeland of aliens?");
	get("The universe is the homeland of the extraterrestrial forms of life. These are called aliens. http://aliens.org");
	test_chat_cleanup();
	
	//test pattern-based replies witj sentiment
	login();
	say("There patterns hi, hello, whatsup, greeting, responses 'hi!', 'hello!', 'whatsup?', 'greeting!'.");
	get("Ok.");
	say("What patterns hi patterns, responses?");
	get();
	say("hi how are you");
	get("Greeting!",array("Hi!","Hello!","Whatsup?"));
	test_chat_cleanup();
	
	login();
	say("There patterns '{hi hello}'; responses 'hi!', 'hello!', 'whatsup?', 'greeting!'.");
	get("Ok.");
	say("What patterns '{hi hello}' patterns, responses?");
	get();
	say("hello");
	get("Greeting!",array("Hi!","Hello!","Whatsup?"));
	test_chat_cleanup();
	
	login();
	say("There patterns '{hi hello whatsup greeting}', responses 'hi!', 'hello!', 'whatsup?', 'greeting!'.");
	get("Ok.");
	say("hi how are you");
	get("Greeting!",array("Hi!","Hello!","Whatsup?"));
	test_chat_cleanup();

	login();
	say("There patterns '{[feeling great] [i am okay] [I am fine] [just perfect]}', responses great to hear.");
	get("Ok.");
	say("What patterns '{[feeling great] [i am okay] [I am fine] [just perfect]}'?");
	get("There patterns '{[feeling great] [i am okay] [I am fine] [just perfect]}', responses great to hear.");
	say("I am just perfect");
	get("Great to hear.\n😊");
	test_chat_cleanup();
	
	login();
	say("There patterns 'feeling {bad sad anxious}', responses 'Does it happen all the time or just occasionally?', trust true");
	get("Ok.");
	say("There patterns '{feeling feel} {happy lucky good}', responses 'Happy about you!', trust true");
	get("Ok.");
	say("What patterns 'feeling {bad sad anxious}'?");
	get("There patterns 'feeling {bad sad anxious}', responses 'Does it happen all the time or just occasionally?'.");
	say("I am feeling sad");
	get("Does it happen all the time or just occasionally?\n😞");
	say("I feel lucky today");
	get("Happy about you!\n😊");
	test_chat_cleanup();
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
	get("No.");
	say("help!");
	get("No.");
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
	get("No.");
	say("What is hi?");
	get("No.");
	say("What hi is?");
	get("No.");
	say("What hi name?");
	get("No.");
	say("What hi?");
	get("No.");
	say("What help?");
	get("No.");
	say("Whatsup");
	get("No.");
	say("привет");
	get("No.");
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
	get("I am ok.",array("I am fine.\n😊","I am great.\n😊"));
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
	get("No.");
	say("What patterns help?");
	get("No.");
	say("What patterns hi?");
	get("No.");
	say("What patterns здорово?");
	get("No.");
	say("What patterns howdy?");
	get("No.");
	say("You forget.");
	get("Ok.");
	// check that patterns-responses ARE NOT working
	say("hi");
	get("No.");
	say("What is hi?");
	get("No.");
	say("What hi is?");
	get("No.");
	say("What hi name?");
	get("No.");
	say("What hi?");
	get("No.");
	say("What help?");
	get("No.");
	say("Whatsup");
	get("No.");
	say("привет");
	get("No.");	
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
	get("No. Email john@doe.org is owned.");
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

	//freetext registration with two-word secrets and delimiter  
	say("My login.");
	get("What your email, name, surname?");
	say("john@doe.org john doe");
	get("What your secret question, secret answer?");
	say("passport number, 123456 querty");
	get("What your passport number?");
	say("123456 querty");
	get("Ok. Hello John Doe!\nMy Aigents ".$version.$copyright);
	test_chat_cleanup();

}

function test_search() {
    global $timeout;
    
    login();
	
	//TODO !!!
	//base url is not aligned!!!
	//say("search '{[\$a products \$c d \$e] [\$d products f g \$h]}' in http://localtest.com/sitea, range 10");
	
	//background search
	say("You forget everything!");
	get("Ok.");
	say("what text 'text aigents topics sites news' sources?");
	get("No.");
	say("search '{[\$context business applications \$about] [\$context social networks \$about]}' in http://localtest.com/, range 10, limit 100, timeout 1");
	get("Search working.");
	say("search '{[\$context business applications \$about] [\$context social networks \$about]}' in http://localtest.com/, range 10, limit 100, timeout 1");
	get("Search busy.");
	say("");//search->ping -> no update
	get("Ok.");
	say("What new true text?");
	get("No.");
	say("what text 'text aigents topics sites news' sources?");
	get("No.");
	//say("Search results?");//TODO with proper query parsing and itenters going first in order 
	say("Search results");
	get("Search busy.");//search->ping -> no update
	sleep($timeout * 6);
	//say("What new true text?");//search->ping -> update
	say("Search results");//search->ping -> update
	get("There about including facebook, context social networks: register and login with, sources http://localtest.com/", null, true);
	
	//search in LTM graph
	say("You forget everything!");
	get("Ok.");
	
	say("search whatever");
	get("No.");
	
	say("search products site http://localtest.com/sitea/products.html, range 3");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("what is products sources, text?");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("search products, period 0, limit 1");
	get("There sources http://localtest.com/sitea/personal.html, text our products make people happier.",array("There sources http://localtest.com/sitea/products.html, text about us products info contact us.","There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable."));
	say("search products, period 0, limit 3");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("search products, limit 3");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("No there times today.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("search products in http://localtest.com/sitea/products.html, range 3");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/sitea/products.html, text about us products info contact us.");
    say("what is http://localtest.com/sitea/mission.html text");
    get("There text 'our mission is to make people happier! home'.");
	say("No there times today.");
	get("Ok.");
	say("You forget!");
	get("Ok.");
	say("What times today?");
	get("No.");
	say("www id products graph date today, period 0");
	get("products worded http://localtest.com/sitea/corporate.html 100.\nproducts worded http://localtest.com/sitea/index.html 100.\nproducts worded http://localtest.com/sitea/personal.html 100.");
	say("www id make graph date today, period 0");
	get("make worded http://localtest.com/sitea/corporate.html 100.\nmake worded http://localtest.com/sitea/mission.html 100.\nmake worded http://localtest.com/sitea/personal.html 100.");
//TODO actual use of the limit 
	say("Search products, period 0");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/index.html, text about us products info contact us; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search 'products make \$x', period 0");
	get("There sources http://localtest.com/sitea/corporate.html, text products make corporations more profitable, x corporations more profitable; sources http://localtest.com/sitea/personal.html, text products make people happier, x people happier.");
	say("What times today?");
	get("No.");
	say("Search people");
	get("There sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search \"make people happier\"");
	get("There sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search \"make people happy\"");
	get("No.");
	say("Search 'make people {happy happier}'");
	get("There sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	say("Search \"make people {happy happier}\"");
	get("There sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");

	//test seach in file/URL
	say("SEARCH 'temperature is \$number' IN http://localtest.com/test/Test.pdf");
	sleep($timeout * 3);
	get("There number 22, sources 'http://localtest.com/test/Test.pdf', text temperature is 22.");
	say("No there times today.");
	get("Ok.");
	say("Search temperature Url 'http://localtest.com/test/Test.pdf'");
	get("There sources 'http://localtest.com/test/Test.pdf', text the outside temperature is 22 c°.");
	say("No there times today.");
	get("Ok.");
	say("what is temperature");
	get("No.");
	say("search temperature site http://localtest.com/test/");
	get("There sources http://localtest.com/test/, text the outside temperature is 22 c°.");
	say("what is temperature");
	get("There about is 22 c°, context the outside, is temperature, sources http://localtest.com/test/, text the outside temperature is 22 c°, times today, title 'Aigents Test Page'.");
	say("what is test");
//TODO: conversation fallback precedence
	get("About is 22 c°, context the outside, title Aigents Test Page?");//fallback to objective finder mode
	//get("This is a test page. http://localtest.com/test/");//fallback to free chat
	//get("No.");//if no fallback to free chat
	say("search test in http://localtest.com/test/Test.pdf");
	get("There sources 'http://localtest.com/test/Test.pdf', text this is a test page.");
	say("what is test");
	get("There about page, context this is a, is test, sources 'http://localtest.com/test/Test.pdf', text this is a test page, times today, title this is a test page.");

	say("search products in http://localtest.com/sitea/, range 0, limit 1");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("what is products text");
	get("There text about us products info contact us.");
	say("No there is products.");
	get("Ok.");
	say("what is products text");
	get("No.");
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
	say("search products in http://localtest.com/siteb/ novelty new");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
	say("search products in http://localtest.com/sitea/, novelty new");
	get("No.");
	say("search products in http://localtest.com/siteb/ novelty new");
	get("No.");
	say("search products in http://localtest.com/siteb/, novelty all");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
	
	//test search in STM
	say("what is products, times today text, sources");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
	say("search products limit 2");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
//	say("search products limit 3");
//	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us; sources http://localtest.com/siteb/contact_info.html, text our products contact information about our company.");
	say("search \"our products\"");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier; sources http://localtest.com/siteb/, text our products contact information about our company.");
	say("search 'products make'");
	get("There sources http://localtest.com/sitea/corporate.html, text our products make corporations more profitable; sources http://localtest.com/sitea/personal.html, text our products make people happier.");
	
	//test searching ranges
	say("No there times today.");
	say("You forget!");
	say("Search products in http://localtest.com/sitea/, range 0, limit 10");
	get("There sources http://localtest.com/sitea/products.html, text about us products info contact us.");
	say("Search personal in http://localtest.com/sitea/, range 0, limit 10");
	get("No.");
	say("Search personal in http://localtest.com/sitea/, range 1, limit 10");
	get("There sources http://localtest.com/sitea/corporate.html, text corporate personal home.");
	say("Search people in http://localtest.com/sitea/, range 1, limit 10");
	get("No.");
	say("Search people in http://localtest.com/sitea/, range 2, limit 10");
	get("There sources http://localtest.com/sitea/mission.html, text our mission is to make people happier; sources http://localtest.com/sitea/personal.html, text our products make people happier.");

	//test caching
	say("No there times today.");
	say("You forget everything!");
	file_put_contents($basePath."html/test.html","<html><body>my test 1. my case a.<a href=\"/sitea/test.html\">link</a></body></html>");
	file_put_contents($basePath."html/sitea/test.html","<html><body>my test 2. my case b.</body></html>");
	say("Search test in http://localtest.com/test.html");
	get("There sources http://localtest.com/sitea/test.html, text my test 2; sources http://localtest.com/test.html, text my test 1.");
	say("No there times today.");
	say("You forget!");//no cache clear
	file_put_contents($basePath."html/test.html","<html><body> empty </body></html>");
	file_put_contents($basePath."html/sitea/test.html","<html><body> nope </body></html>");
	say("Search test in http://localtest.com/test.html");
	get("There sources http://localtest.com/sitea/test.html, text my test 2; sources http://localtest.com/test.html, text my test 1.");
	say("Search case in http://localtest.com/test.html");
	get("There sources http://localtest.com/sitea/test.html, text my case b; sources http://localtest.com/test.html, text my case a.");
	say("No there times today.");
	say("You forget everything!");//cache clear
	say("Search test in http://localtest.com/test.html");
	get("No.");
	say("Search case in http://localtest.com/test.html");
	get("No.");
	
	say("No there times today.");
	get();
	say("You forget everything!");
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

function test_bot() {//slash commands: help, start, settings
	test_chat_init();
	
	say("start");
	get("What your email, name, surname?");
	login( "john", "john@doe.org", "doe", "q", "a", false);
	say("/start");
	get("What your email, name, surname?");
	login( "john", "john@doe.org", "doe", "q", "a", false);
	say("login");
	get("What your email, name, surname?");
	login( "john", "john@doe.org", "doe", "q", "a", false);
	
	say("There trust true, patterns help; responses 'Help yourself! ;-)");
	get("Ok.");
	say("help");
	get("Help yourself! ;-).");
	say("/help");
	get("Help yourself! ;-).");
	
	say("What my email, name, surname, check cycle, items limit, trusts limit, news limit, email notification, discourse id, steemit id, golos id, ethereum id?");
	get("Your check cycle 3 hours, email john@doe.org, items limit 100, name john, news limit 10, surname doe, trusts limit 20.");
	say("There trust true, patterns settings; responses 'To check your settings, ask something like \"What my email, name, surname, check cycle, items limit, trusts limit, news limit, email notification, telegram notification, facebook notification?\".\nTo update your settings, tell something like \"My items limit 50, trusts limit 10, news 5, email notification false, telegram notification true, facebook notification false\".'");
	get("Ok.");
	say("settings");
	get("To check your settings, ask something like \"What my email, name, surname, check cycle, items limit, trusts limit, news limit, email notification, telegram notification, facebook notification?\".\nTo update your settings, tell something like \"My items limit 50, trusts limit 10, news 5, email notification false, telegram notification true, facebook notification false\".\n😊");
	say("/settings");
	get("To check your settings, ask something like \"What my email, name, surname, check cycle, items limit, trusts limit, news limit, email notification, telegram notification, facebook notification?\".\nTo update your settings, tell something like \"My items limit 50, trusts limit 10, news 5, email notification false, telegram notification true, facebook notification false\".\n😊");
	
	test_chat_cleanup();
}

function test_load() {//loading csv data
	global $base_things_count;
	
	test_chat_init();
	
//TODO: NPE, fix Reader for what makes it possible to handle "there is animal, has name, color, size." but makes impossible to deal with "What my news text?"
//	say("there is animal, has name, color, size.");
	say("there name animal, has color, size.");
	get("Ok.");
	say("what is animal");
	get("No.");
	file_put_contents($basePath."html/test.csv","name,weight,color,size\nelephant,heaviy,gray,huge\nmouse,light,gray,small");
	say("load file ".$basePath."html/test.csv"." as animal");
	get("Ok. 2 things.");
	say("what is animal name, size, color");
	get("There color gray, name elephant, size huge; color gray, name mouse, size small.");
	say("what is a gray animal?");
	get("Size huge or small?");
	say("better huge");
	get("Color gray, name elephant, size huge?");
	say("yes");
	get("Ok.");
	say("is animal trust false.");
	get("Ok.");
	say("no there is animal");
	get("Ok.");
	say("what is animal");
	get("No.");
	say("load file http://localtest.com/test.csv as animal");
	get("Ok. 2 things.");
	say("what is animal name, size, color");
	get("There color gray, name elephant, size huge; color gray, name mouse, size small.");
	say("what is a small animal?");
	get("Color gray, name mouse, size small?");
	say("Yes.");
	get("Ok.");

	//using https://www.kaggle.com/datafiniti/fast-food-restaurants
//TODO make name readable with no NPE
	//say("there name restraunt, has address, city, country, name, province");
	say("there name restraunt, has address, city, country, province");
	get("Ok.");
	//say("load file /Users/akolonin/Documents/data/Datafiniti/FastFoodRestaurants.csv as restraunt");
	say("load file ./data/us-restraunt-data.csv as restraunt");
	get("Ok. 10000 things.");
	say("I need a food nearby the martin luther king");
	get("Province AL or AR or CA or FL or GA or IA or LA or MI or NC or OH or OK or TX?");
	say("TX");
	get("Address 414 W Martin Luther King Jr Blvd, city Austin, country US, name McDonald's, province TX?");
	say("yes");
	get("Ok.");
	say("I am looking for burger king");
	get("Province AL or AR or AZ or CA or CO or CT or Co Spgs or DE or FL or GA or HI or IA or ID or IL or IN or KS or KY or LA or MA or MD or ME or MI or MN or MO or MS or NC or ND or NE or NH or NJ or NM or NV or NY or OH or OK or OR or PA or RI or SC or SD or TN or TX or UT or VA or VT or WA or WI or WV or WY?");
	say("TX");
	get("City Alamo or Arlington or Balch Springs or Boerne or Brownfield or Columbus or Corpus Christi or Dallas or Edinburg or El Paso or Fort Stockton or Fort Worth or Frisco or Grapevine or Harlingen or Hidalgo or Hillsboro or Houston or Irving or Kerrville or Kingsville or Laredo or Lewisville or Mcallen or Midland or Mission or Nacogdoches or Port Isabel or Randolph A F B or Rio Grande City or Rockwall or Roma or San Antonio or Spring or Sugar Land or Uvalde?");
	say("Randolph");
	get("Address 0 Randolph AFB,, city Randolph A F B, country US, name Burger King, province TX?");
	say("Yes");
	get("Ok.");
	say("can I find McDonald's?");
	get("Province AK or AL or AR or CA or CO or CT or Co Spgs or FL or GA or IL or IN or KS or KY or LA or MA or MD or MI or MN or MO or NC or NE or NM or NV or NY or OH or OR or PA or SC or SD or TN or TX or VA or VT or WA or WI or WV?");
	say("FL");
	get("City Crestview or Fort Lauderdale or Miami or Naples or Palm Bay or Port Saint Lucie or Tallahassee or Wellington?");
	say("Crestview");
	get("Address 3201 S Ferdon Blvd, city Crestview, country US, name McDonalds, province FL?");
	say("yes");
	get("Ok.");
	
	//test saving/loading/forgetting	
	say("You forget!");
	get("Ok.");
	say("You save test1.txt!");
	get("Ok.");
	say("What your things count?");
	get("My things count ".($base_things_count + 10000 + 10).".");
	say("You forget!");
	get("Ok.");
	say("What your things count?");
	get("My things count ".($base_things_count + 10000 + 10).".");
	say("is restraunt trust false");
	get("Ok.");
	say("No there is restraunt");
	get("Ok.");
	say("What your things count?");
	get("My things count ".($base_things_count + 10).".");
	say("You load test1.txt!");
	get("Ok.");
	say("What your things count?");
	get("My things count ".($base_things_count + 10000 + 10 - 4).".");//with duplicates
	
	//using https://data.mos.ru/datasets/1788
	say("there name ресторан, has название, округ, район, адрес, телефон");
	get("Ok.");
	//say("load file /Users/akolonin/Documents/data/data.mos.ru/moscow-restraunt-data-2901-2020-07-14-3.csv as ресторан");
	say("load file ./data/moscow-restraunt-data.csv as ресторан");
	get("Ok. 2274 things.");
	say("my language russian");
	get("Ok.");
	say("где поесть хинкали");
	get("Округ Северо-Западный или Центральный или Юго-Восточный или Юго-Западный?");
	say("центральный");
	get("Адрес город Москва, улица Остоженка, дом 19, строение 1, название Есть хинкали, пить вино, округ Центральный, район Хамовники, телефон (495) 975-55-35?");
	say("да");
	get("Ok.");
	say("нужна пицца");
	get("Округ Западный или Новомосковский или Северный или Северо-Восточный или Северо-Западный или Центральный или Юго-Восточный или Юго-Западный или Южный?");
	say("южный");
	get("Район Братеево или Донской?");
	say("донской");
	get("Адрес город Москва, улица Вавилова, дом 3, название Пицца Хат, округ Южный, район Донской, телефон (906) 079-01-23?");
	say("да");
	get("Ok.");
	
	test_chat_cleanup();
}

test_init();
test_summarize();
test_report();
test_findchat();
test_demochat();
test_freechat();
test_help();
test_chat();
test_groups();
test_search();
test_bot();
test_load();
test_summary();

?>
