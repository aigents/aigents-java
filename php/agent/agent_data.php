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

function test_data() {
	login();

	//TODO: java.lang.NullPointerException at net.webstructor.al.Reader.parseExpression(Reader.java:718)
	//say(
	//"there name company, has name, categories, phone, address, sites;" .
	// "there name product, has price, name, quantity, address, companies, times;");

	//TODO: java.lang.NullPointerException at net.webstructor.al.Reader.parseExpression(Reader.java:718)
	//say("there name company, has name, categories, phone, address, sites;" .
	// "there name product, has price, name, quantity, address, companies, times;");
	 
	//TODO: java.lang.NullPointerException at net.webstructor.al.Reader.parseExpression(Reader.java:718)
	//say("there name company, has name, categories, phone, address, sites.");

	say("my topics company. my topics product. company has name, categories, phone, address, sites. product has price, name, quantity, address, companies, times, categories.");
	get("Ok. Ok. Ok. Ok.");
	
	say("what company has?");
	get("Company has address, categories, name, phone, sites.");
	say("what is company?");
	get("No.");
	say("what product has?");
	get("Product has address, categories, companies, name, price, quantity, times.");
	say("what is product?");
	get("No.");
	
	say("there is company, name 'Русская чайная компания', categories 'Розничные магазины', phone '7 (495) 447-05-56', address 'ТЦ Олимпия, 1 этаж, ул. Галущака, 2А, Новосибирск, Новосибирская обл., 630049', sites 'https://www.rusteaco.ru/'. " .
	"there is company, name 'Окей', ̆categories 'Гипермаркеты', phone '7(495)139-20-85', address 'ТРЦ «Аура», Военная ул., 5, Новосибирск, Новосибирская обл., 630099', sites 'https://www.okeydostavka.ru/'. " .
	"there is company, name 'Утконос', categories 'Электронный Магазин', phone '7 (495 )777- 54-44', address 'г.Москва',  sites 'https://www.utkonos.ru/'. " .
	"there is product, price '169,40 ₽' quantity '200г', name 'Чай Принцесса Канди черный Цейлон', companies 'Окей', address 'г. Новосибирск', times 2020-01-20, categories 'чай'. " .
	"there is product, price '147,40 ₽' quantity '250г', name 'Чай черный листовой Принцесса Канди Медиум', companies 'Окей', address 'г. Новосибирск', times 2020-01-20, categories 'чай'. " .
	"there is product, price '139 ₽', quantity '250г', name 'Чай черный Принцесса Канди Медиум отборный листовой 0,25кг', companies 'Утконос', address 'г. Москва', times 2020-01-20, categories 'чай'. " .
	"there is product, price '122 ₽', quantity '200г', name 'Чай черный Принцесса Канди цейлонский 2г*100 пакетиков', companies 'Утконос', address 'г. Москва', times 2020-01-20, categories 'чай'. " .
	"there is product, price '68 ₽', quantity '50г', name 'FruTea. Чай \"Зимний вечер\" (чай черный) 50 г', companies 'Русская чайная компания', address 'г. Новосибирск', times 2020-01-20, categories 'чай'. " .
	"there is product, price '68 ₽', quantity '50г', name 'FruTea. Чай \"Зимний вечер в Гаграх\" (чай черный) 50 г', companies 'Русская чайная компания', address 'г. Новосибирск', times 2020-01-20, categories 'чай'. ".
	"there is product, price '52 ₽', quantity '50г', name 'FruTea. Чай \"Дед Мороз уже в пути!\" (чай черный) 50 г', companies 'Русская чайная компания', address 'г. Новосибирск', times 2020-01-20, categories 'чай'. ");
	get("Ok. Ok. Ok. Ok. Ok. Ok. Ok. Ok. Ok. Ok.");
	say("what is company?");
	get("There address 'ТРЦ «Аура», Военная ул., 5, Новосибирск, Новосибирская обл., 630099', is company, name '̆categories Гипермаркеты', phone '7(495)139-20-85', sites https://www.okeydostavka.ru/; address 'ТЦ Олимпия, 1 этаж, ул. Галущака, 2А, Новосибирск, Новосибирская обл., 630049', categories 'Розничные магазины', is company, name 'Русская чайная компания', phone '7 (495) 447-05-56', sites https://www.rusteaco.ru/; address 'г.Москва', categories 'Электронный Магазин', is company, name 'Утконос', phone '7 (495 )777- 54-44', sites https://www.utkonos.ru/.");
	say("what is product?");
	get("There address 'г. Москва', categories чай, companies 'Утконос', is product, name 'Чай черный Принцесса Канди Медиум отборный листовой 0,25кг', price 139 ₽, quantity 250г, times 2020-01-20; address 'г. Москва', categories чай, companies 'Утконос', is product, name 'Чай черный Принцесса Канди цейлонский 2г*100 пакетиков', price 122 ₽, quantity 200г, times 2020-01-20; address 'г. Новосибирск', categories чай, companies 'Окей', is product, name 'Чай Принцесса Канди черный Цейлон', price 169,40 ₽, quantity 200г, times 2020-01-20; address 'г. Новосибирск', categories чай, companies 'Окей', is product, name 'Чай черный листовой Принцесса Канди Медиум', price 147,40 ₽, quantity 250г, times 2020-01-20; address 'г. Новосибирск', categories чай, companies 'Русская чайная компания', is product, name 'FruTea. Чай \"Дед Мороз уже в пути!\" (чай черный) 50 г', price 52 ₽, quantity 50г, times 2020-01-20; address 'г. Новосибирск', categories чай, companies 'Русская чайная компания', is product, name 'FruTea. Чай \"Зимний вечер в Гаграх\" (чай черный) 50 г', price 68 ₽, quantity 50г, times 2020-01-20; address 'г. Новосибирск', categories чай, companies 'Русская чайная компания', is product, name 'FruTea. Чай \"Зимний вечер\" (чай черный) 50 г', price 68 ₽, quantity 50г, times 2020-01-20.");
	say("what is product price?");
	get("There price 122 ₽; price 139 ₽; price 147,40 ₽; price 169,40 ₽; price 52 ₽; price 68 ₽; price 68 ₽.");
	
	//TODO:
	/*
	 * Search in topics, if any 
	 * Return results per topic
	 */
	//TODO: search чай field name
	say("search чай");
	get();
	say("search Чай");
	get();
	

	say("No there is company.");
	say("No there is product.");
	say("No name product.");
	say("No name company.");

	say("what is company?");
	get("No.");
	say("what is product?");
	get("No.");
	
	say("You forget!");
	get("Ok.");
	
	logout();
}


test_init();
test_data();
test_summary();

?>
