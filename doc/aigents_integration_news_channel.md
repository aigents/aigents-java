# Aigents Server Integration for News Syndication on Channel basis

## 1. Setting up the server

### [Set up server of your own](https://aigents.com/download/latest/readme.html)

### Use existig Aigents Web Demo server

API URL: https://aigents.com/al  

## 2. Understand the basics

### Communication protocol

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

### Channel (area) management

1. In the Aigents, the news **channel** is just personal news feed of particular user made public. 
1. The personal news feed can be turned into named **area** by the user (owner) so the non authorised users can see it. Use verb **areas** to identify the area of your interest name and verb **shares** to make your curret area public to others. Below are the examples of AL statements.
	1. Turn feed of the current user into named area with name "my_area": *my areas my_area*
	1. Remove named area with name from being associated with feed of the current user "my_area": *my areas not my_area*	
	1. Make the current user feed associated with correspodig named area as shared to public: *my shares my_area*
1. The shared area (channel) can be obtanied as RSS feed as it is shown in the folliwng video. Assuming the Aigents API URL is ***https://aigents.com/al** and area name is **ai**, the url *[https://aigents.com/al?rss%20ai](https://aigents.com/al?rss%20ai)* will provide the RSS feed.
[![](http://img.youtube.com/vi/8r_vmlkFKfI/0.jpg)](http://www.youtube.com/watch?v=8r_vmlkFKfI "")

## 3. Set up the users per channel

TODO

## 4. Get news from users' channels

TODO