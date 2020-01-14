# Aigents Server Integration for News Syndication on Channel basis

## 1. Setting up the server

### Set up server of your own

To setup your own in-house Aigents server you can refer to the [instructions](https://aigents.com/download/latest/readme.html).

### Use existig Aigents Web Demo server

Tu use existing Aigents Web Demo server you can use this API URL: https://aigents.com/al  

## 2. Understand the basics

### 2.1. Communication protocol

1. Operations with the Aigents server are committed by means of [**Aigents Lagnuage**](http://aigents.com/papers/2015/ZONT-2015-Agent-Language-Kolonin.pdf) or **AL** over any supported commuication protocol enabling transmission of fill text. The preferred protocol is HTTPS because of its security.     
1. All interactions in AL are symmetric (peer-2-peer) and asynchroous by its design. However, using HTTP/HTTPS protocol iteractions are treated as asymmetric synchronous client-server with server being the Aigents Server. When using HTTP/HTTPS, the following applies.
	1. Client requests can be submitted either as POST (more secure) or GET (less secure) requests in AL language syntax and semantics.
	1. Server responses may come in few forms, as follows.
		1. AL language syntax and semantics in most of cases
		1. JSON encodings in case of responses to **what ... ?** sorts of interrogative AL statements - in case if curret session is configured with **format json**.
		1. HTML mark-ups in case of responses to **what ... ?** sorts of interrogative AL statements - in case if curret session is configured with **format json**.
		1. The **format** setting ca be set to **text** or **json** or **html** saying to server either of the following.
			1. *my format text* (being default)
			1. *my format json*
			1. *my format html*
		1. HTML mark-ups or JSON encodings in case of social **reports** requested, based on **format** specification.
		1. HTML mark-ups in case of **search** results requested, based on **format** specification.
1. Al interactions are being committed in a user context where user may be anonymous or authenticated. Scope of Aigents Server actions directed my means of AL statements for anonymous sessions is restricted while scope of actions for autheticated sessions is extended.
1. Session contexts are maintaied in different ways, lke follows.
	1. HTTP/HTTPS - cookies, so you need to keep cookies on the HTTP/HTTPS client side.  
	1. Telegram, Slack and Facebook Messeger - user identifiers and session tokens corresponding to API-s of those messengers.
	1. TCP/IP sessions - socket connection contexts.
1. For the authentication purposes, users may be registered with and logged into Aigents in different ways - using email as well as third party systems, such as Facebook, Google, PaPal, Telegram, Slack, Reddit and VKontakte.
	1. For integration purposes, registration and login by email appears more straightforward as it needs just user **email** as account identifier and **secret question** and **secret answer** for authentication purpose.
	1. Registration and login with third-party systems is based on custom implementations of OAuth2 protocol by respective systems and so it appears more complicated for integration purposes.  

### 2.2. Channel (area) management

1. In the Aigents, the news **channel** is just personal news feed of particular user made public. 
1. The personal news feed can be turned into named **area** by the user (owner) so the non authorised users can see it. Use verb **areas** to identify the area of your interest name and verb **shares** to make your curret area public to others. Below are the examples of AL statements.
	1. Turn feed of the current user into named area with name "my_area": *my areas my_area*
	1. Remove named area with name from being associated with feed of the current user "my_area": *my areas not my_area*	
	1. Make the current user feed associated with correspodig named area as shared to public: *my shares my_area*
1. The shared area (channel) can be obtanied as RSS feed as it is shown in the folliwng video. Assuming the Aigents API URL is **https://aigents.com/al** and area name is **ai**, the url *[https://aigents.com/al?rss%20ai](https://aigents.com/al?rss%20ai)* will provide the RSS feed.

[![](http://img.youtube.com/vi/8r_vmlkFKfI/0.jpg)](http://www.youtube.com/watch?v=8r_vmlkFKfI "")

## 3. Set up the users per channel and restart sessions 

1. For the user registration purpose, [registration and login by email flow](https://github.com/aigents/aigents-java/blob/master/test.out) is suggested.
1. The registration for every user should be performed only once, using the email.
	1. No email confirmation is requiered on registration.
	1. Changing email later will require confirmation code.
	1. For channel-based integration, fake emails may be used as identifiers in email-conforming format like *12345@mysite.org* or *channel_cats@my_site.com*. 
1. The registration flow is the following.
	1. Client - current session (if any) is closed by logout: *logout*
	1. Server - confirms: *Ok.*
	1. Client - initiates login: *login*
	1. Server - prompts for registration: *What your email, name, surname?*
	1. Client - enters email, name and surname, e.g.: *myemail@mysite.mydomain, myname, mysurname* 
	1. Server - asks for secret question and answer for authentication: *What your secret question, secret answer?*
	1. Client - provides the question and answer, e.g.: *my secret question "fish", secret answer "tuna"* (or *my secret question "strong password", secret answer "@ghTyYUU19%*1gpy90tY56"*)
	1. Server - checks for answer, e.g.: *What your fish?* (or *What your strong password?*)
	1. Client - answers, e.g.: *my fish "tuna"* (or *my strong password "@ghTyYUU19%*1gpy90tY56"*)
	1. Server - confirms registration, e.g.: *Ok. Hello Myname Mysurname! My Aigents 2.2.4 Copyright © 2020 Anton Kolonin, Aigents®*
1. The login attempt for the user should be tried whenever it is not sure if the previous session is still valid (which may be not the case if the server has had interal error losing session context).
1. TODO

## 4. Set up the channel configuration

TODO 

## 5. Get news from users' channels

TODO

TODO example from github/akolonin

