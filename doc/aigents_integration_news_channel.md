# Aigents Server Integration for News Syndication on Channel basis

## 1. Setting up the server

In order to integrate Aigents news syndication in your applications, two options are possible, as follows.

1. Set up server of your own.
	1. Pro: have full control over perfromance and reliability of your server.
	1. Con: have to deploy and maintain the server yourself. 
	1. To setup your own in-house Aigents server you can refer to the [instructions](https://aigents.com/download/latest/readme.html).
1. Use existig Aigents Web Demo server
	1. Pro: no need to deploy and maintain your your server.
	1. Con: may experience performance and reliability issue due to development activity on Aigents Web Demo server.  
	1. To use existing Aigents Web Demo server you can use this API URL: https://aigents.com/al 

## 2. Understand the basics

### 2.1. Communication protocol

1. Operations with the Aigents server are committed by means of [**Aigents Language**](https://github.com/aigents/aigents-java/blob/master/doc/papers/2015/ZONT-2015-Agent-Language-Kolonin.pdf) or **AL** over any supported commuication protocol enabling transmission of fill text. The preferred protocol is HTTPS because of its security.     
1. All interactions in AL are symmetric (peer-2-peer) and asynchroous by its design. However, using HTTP/HTTPS protocol iteractions are treated as asymmetric synchronous client-server with server being the Aigents Server. When using HTTP/HTTPS, the following applies.
	1. Client requests can be submitted either as POST (more secure) or GET (less secure) requests in AL language syntax and semantics.
	1. Server responses may come in few forms, as follows.
		1. AL language syntax and semantics in most of cases. Parsing AL may be done with either of the following.
			1. Simplified AL parser in JavaScript fork or port of it refering to the *[parseToGrid function](https://github.com/aigents/aigents-java/blob/master/html/ui/aigents-al.js#L528)* - the easiest option.
			1. Custom (simplified) AL parser implementd accordingly to AL [language specification](https://github.com/aigents/aigents-java/blob/master/doc/papers/2015/ZONT-2015-Agent-Language-Kolonin.pdf) - moderate complexity option.
			1. Native Java AL parser forked from original [reference implementation](https://github.com/aigents/aigents-java/blob/master/src/main/java/net/webstructor/al/Reader.java#L521) - most complext option. 
		1. JSON encodings in case of responses to **what ... ?** sorts of interrogative AL statements - in case if curret session is configured with **format json**.
		1. HTML mark-ups in case of responses to **what ... ?** sorts of interrogative AL statements - in case if curret session is configured with **format json**.
		1. The **format** setting can be set to **text** or **json** or **html** saying to server either of the following.
			1. *my format text* (being default)
			1. *my format json*
			1. *my format html*
		1. HTML mark-ups or JSON encodings in case of social **reports** requested, based on **format** specification.
		1. HTML mark-ups in case of **search** results requested, based on **format** specification.
1. Al interactions are being committed in a user context where user may be anonymous or authenticated. Scope of Aigents Server actions directed my means of AL statements for anonymous sessions is restricted while scope of actions for autheticated sessions is extended.
1. Session contexts are maintaied in different ways, lke follows.
	1. HTTP/HTTPS - cookies, so you need to keep cookies on the HTTP/HTTPS client side - see the following examples how to maintain the cookies.
		1. [Cookies maintained in PHP](https://github.com/aigents/aigents-java/blob/master/php/agent/test_api.php#L69).
		1. [Cookies maintained in Java](https://github.com/aigents/aigents-java/blob/master/src/main/java/net/webstructor/comm/HTTP.java#L218).
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

## 3. Set up users per channel and restart sessions 

1. For the user registration purpose, [registration and login by email flow](https://github.com/aigents/aigents-java/blob/master/test.out) is suggested.
1. The registration for every user should be performed only once, using the email.
	1. No email confirmation is requiered on registration.
	1. Changing email later will require confirmation code.
	1. For channel-based integration, fake emails may be used as identifiers in email-conforming format like *12345@mysite.org* or *channel_cats@my_site.com*. 
1. The registration flow is the following.
	1. Client - current session (if any, just in case) is closed by logout: *logout*
	1. Server - confirms: *Ok.*
	1. Client - initiates login: *login*
	1. Server - prompts for login/registration: *What your email, name, surname?*
	1. Client - enters email, name and surname, e.g.: *myemail@mysite.mydomain, myname, mysurname* 
	1. Server - asks for secret question and answer for authentication: *What your secret question, secret answer?*
	1. Client - provides the question and answer, e.g.: *my secret question "fish", secret answer "tuna"* (or *my secret question "strong password", secret answer "@ghTyYUU19%*1gpy90tY56"*)
	1. Server - checks for answer on secret question, e.g.: *What your fish?* (or *What your strong password?*)
	1. Client - answers, e.g.: *my fish "tuna"* (or *my strong password "@ghTyYUU19%*1gpy90tY56"*)
	1. Server - confirms registration, e.g.: *Ok. Hello Myname Mysurname! My Aigents 2.2.4 Copyright © 2020 Anton Kolonin, Aigents®*
1. The login attempt for the user should be tried whenever it is not sure if the previous session is still valid (which may be not the case if the server has had interal error losing session context).
1. The login flow is the following.
	1. Client - current session (if any, just in case) is closed by logout: *logout*
	1. Server - confirms: *Ok.*
	1. Client - initiates login: *login*
	1. Server - prompts for login/registration: *What your email, name, surname?*
	1. Client - enters email, e.g.: *myemail@mysite.mydomain* 
	1. Server - checks for answer on secret question, e.g.: *What your fish?* (or *What your strong password?*)
	1. Client - answers, e.g.: *my fish "tuna"* (or *my strong password "@ghTyYUU19%*1gpy90tY56"*)
	1. Server - confirms registration, e.g.: *Ok. Hello Myname Mysurname! My Aigents 2.2.4 Copyright © 2020 Anton Kolonin, Aigents®*
1. The [Python example of creation of Aigents session](https://github.com/akolonin/singnet/blob/master/agent/adapters/aigents/__init__.py#L73) (simplified version) can be found on githib in pre-alpha version of [SingularityNET](https://github.com/singnet/).

## 4. Manage channel (area) configuration

### 4.1. Set up channel (area) configuration

1. The channel (area) configuration is set up by adding and removing sites and topics for the user (peer) who owns the channel, like [described in the earlier publication](https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5).
	1. **NB:** The following descrbies **simplified** version of the topics configuration missing details on configuring **multiple patterns per topic** and **variables in patterns** (see the details in the publication referenced above).
1. In order the site or topic to be involved in the news monitoring for give user, it has to be ot only listed in the list of sites and topics, but included in the list of things trusted by user as well.
1. In order to stop monitoring of the site or topic, it has to be removed from the list of trusted things. In order to remove  it from the list of sites ot being curretly monitored by the user, need to remove it from te list of sites as well.
1. To change the existing site or topic, need to remove old one and add new one - for list of sites or topics respectively and for the list of trusts as well.
1. To add sites and topics use the following statements - assuming the site is "https://medium.com/@aigents/" and the topic is "{ai agi [artificial intelligence] [artificial general intelligence]}", for example. 
	1. To have a site added to the list of sites: *my sites "https://medium.com/@aigents/"* 
	1. To have a topic added to the list of topis: *my topics "{ai agi [artificial intelligence] [artificial general intelligence]}"*
	1. To have a site added to the list of trusted things: *my trusts "https://medium.com/@aigents/"* 
	1. To have a topic added to the list of trusted things: *my trusts "{ai agi [artificial intelligence] [artificial general intelligence]}"*
1. To remove sites of topics use the following statements: 
	1. To have a site removed from the list of sites: *my sites not "https://medium.com/@aigents/"* 
	1. To have a topic removed from the list of topis: *my topics not "{ai agi [artificial intelligence] [artificial general intelligence]}"*
	1. To have a site removed from the list of trusts: *my trusts not "https://medium.com/@aigents/"* 
	1. To have a topic removed from the list of trusts: *my trusts not "{ai agi [artificial intelligence] [artificial general intelligence]}"*

### 4.2. View channel (area) configuration

1. Viewing of a chanel (area) setup associated with current user may be achieved with AL queries having the query results returned in either AL or JSON or HTML format as it has been descrbied above, based on what kind of parsing is convenient. 
1. The list of the topics and sites with boolean indications of whether they are also trusted along with currently evaluated relevace of the topics (to the scope of trusted content in the users' news feed) can be be requested with correspoding statements.
	1. Topics: *what my topics name, trust, relevance?*
	1. Sites: *what my sites name, trust, relevance?*   

## 5. Manage news from users' (peers') channels

### 5.1. Get news from users' (peers') channels

1. Viewing of a chanel (area) news feed associated with current user may be achieved with AL queries having the query results returned in either AL or JSON or HTML format as it has been descrbied above, based on what kind of parsing is convenient. 
1. In order to update the news relevances accordingly to the trusts (ratings) set by user need to initiate **thinking** process with respective statement: *you think!*
1. In order to retrieve all news items need to issue the statement: *what new true sources, text, times, trust, relevance, social relevance, image, is?*
1. In order to retrieve news items for particular day only need to issue the statement with **times** set to date in **YYYY-MM-DD** format, e.g.: *what new true, times 2020-02-20 sources, text, trust, relevance, social relevance, image, is?*
1. In order to retrieve only trusted or untrusted news items need to issue the statement with **trust** set to fals or true, e.g.: *what new true, trust true sources, text, times, trust, relevance, social relevance, image, is?* or *what new true, trust false sources, text, times, trust, relevance, social relevance, image, is?*, respectively.
1. In order to retrieve only trusted or untrusted news items for particular day only need to issue the statement with **trust** set to fals or true, e.g.: *what new true, times 2020-02-20, trust true sources, text, trust, relevance, social relevance, image, is?* or *what new true, times 2020-02-20, trust false sources, text, trust, relevance, social relevance, image, is?*, respectively.
1. In order to retrieve only limited set of attributes (properties) of the news items need to issue the statement with these attributes, e.g. requesting only the text and date: *what new true text, times?*
1. In order to retrieve un limited set of attributes (properties) of the news items, including additional property values filled by the patter matcher based on the **pattern variables** need to issue the statement without of the attributes listed, e.g.: *what new true?*
1. Extra filtering may be applied on top of the requested criteria based o the attributed property values.
1. The standard property values referred to above are the following. 
	1. **sources** - URL of the news source (may be many URLs, but typically one), corrsponding to **RSS link**. 
	1. **text** - text of the news item, corresponds to **RSS title**.
	1. **times** - date of the news item in **YYYY-MM-DD** format, correspods to **RSS pubDate**.
	1. **trust** - either *true* or *false* indicating whether the item is trusted (positively ranked) or not trusted (ot ranked) by user, respectively.  
	1. **relevance** (personal relevance) - 0-100% as estimation of the extent to which the **text** and the **sources** may be trusted by the user, given the earlier **trusts** given by user to the other news items earlier or to the **topics** (in case if no trusts to news items are given at all).     
	1. **social relevance** - 0-100% as estimation of the expected trust assessed like above but in regard to social connections of the user istead of the user itself (setting up social connections to be considered [separately](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_user.md)).   
	1. **image** - URL to the image assocciated with the **text**, corresponds to **RSS enclosure**. 
	1. **is** - original Aigents **topic** of the news item as it is set up with **topics** verb earlier, correspods to **RSS category**.
	1. **NB:** **context** - now used as a custom attribute based on pattern variables in some of sample patterns, but in later versions of Aigents pattern matcher it may be re-defined as broader textual **context** of the **text** and become representing **RSS description**.


### 5.2. Mark news trusted (rate the news) or outdated (hide the news)

1. The user who owns the channel (area) may have the news items marked (rated) as trusted, so another time the **thinking** process is executed, the other news items as well as topics and sites have their **relevance** and **social relevance** properties updated respectively - this is done setting **trust** property of a news item to *true*. Removal of the trust is achieved setting **trust** property of a news item to *false*. 
1. The user who owns the channel (area) may have the news items marked **no news** setting value of the **new** property to *false*, which would effect in removal of the news item from the news feed. Respectively, to get the news item back to the news feed need to restore the **new** property value to *true*.
1. Setting of the **trust** and **new** prpoperty values is done with AL statements referrig the the news item by means of its other attributes such as **text**, **sources** and **times**, like in the following examples.
	1. Give **trust** to a news item: *sources 'http://mysite.mydomain' and text 'my matching news text' and times today trust true*.
	1. Remove **trust** from a news item: *sources 'http://mysite.mydomain' and text 'my matching news text' and times today trust false*.
	1. Give **trust** to all today news items from specific domain: *sources 'http://mysite.mydomain' and times today trust true*.
	1. Give **trust** to all today news item with specific text: *text 'my matching news text' and times today trust true*.
	1. Give **trust** to all news items from specific domain: *sources 'http://mysite.mydomain' trust true*.
	1. Give **trust** to all news item with specific text: *text 'my matching news text' trust true*.
	1. Give **trust** to all news items: *times today, new true trust true*.
	1. Give **trust** to all news items: *new true trust true*.
	1. Hide all **new** items: *new true new false*.
	1. Unhide news items from specific domain: *sources 'http://mysite.mydomain' new true*.
	1. Unhide all all today news items from specific domain: *sources 'http://mysite.mydomain' and times today new true*.
	1. et. cetera...
1. All settings to the **trust** and **new** attributes are specific to context of a given user context, so setting these prpoperties to *true* or *false* for specific user and its respective channel (area) does not affect other users ad their channels (areas).
 
**The End**