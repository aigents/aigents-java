# Aigents Server Integration for News Aggregation on User basis

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)

## 1. Security and business requirements

This instuctions extends more simple [Aigents Server Integration for News Syndication on Channel basis](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_channel.md). 
The following aspects should be kept in mind and taken care of in order to assure legal safety of the integration.

1. The integration is expected to handle personal data and user authetication data.
	1. Both kinds of data are protected by different atinal regulations like [GDPR](https://gdpr-info.eu/) so whatever you do in regard to this integration, you have to **check in with your lawer first**.
	1. You must conduct all API interactions over HTTPS protocol using valid SSL certificate and using only HTTP POST requests (not HTTP GET requests to call Aigents servr).
		1. Free and secure SSL certificates may be updated with [gethttpsforfree](https://gethttpsforfree.com/). 
	1. If you transfer the traffic and data from Aigents server and to it in your system over any other communication media, you must ensure proper ecryption of those commuication channels. 
	1. If you host your own Aigents server your have to take security measures to protect storage of the data on Aigents servr location (local files on the file system, including file *al.txt* primarily) corresponding to legal regulations corrsponding to citizenship of your users.
	1. If you store any personal data and user authetication data, including the data obtanied from Aigents server and sed to it, you should the same security measures, corresponding to legal regulations corresponding to citizenship of your users, in regard to storage of the data.
	1. The data which has to be covered by security measures includes *email, name, surname, date of birth, secret question, secret answer (password)* as well as **identifiers and tokens in social networks, blockchains and messengers**.    
	1. If you are maikng your integratoion with Aigents Web Demo server at [https://aigents.com](https://aigents.com), your should comply with [Aigents Privacy Policy and License Agreement](https://aigents.com/en/license.html) 
	1. If you are making your integration with any Aigents server other than [https://aigents.com](https://aigents.com), you may use [Aigents Privacy Policy and License Agreement](https://aigents.com/en/license.html) as a template as your own privacy policy and user agreement to be confirmed or signed by your users and make sure that it complies with legal regulations corresponding to citizenship of your users.
	1. If your integration involves transfer of the personal and authentication data of the users over the natinal boundaries or between differet business entities, make sure that your own privacy policy and user agreement make this agreed by users and not conflicting national regulations in the countries that users' are citizens of.
		1. Transfer of such data may be ot allowed by legal regulations of some of the countries. 
		1. For trasfer of the data from one business entity to another may requere explicit written agreement o bealf of the user approving this kind of transfer, accordingly to legal regulations of some of the countries.
1. The integration with some of the social networks and messegers assumes legal business responsibility in respect to your integration of Aigents-based service, so the following should be followed.
	1. Facebook
		1. Facebook integration requires to set-up your own business account, create your Aigents-based Facebook application under this account and get *application id* and *application secret* (plus *challenege* in the case if you want to have a **bot integration** with Facebook Messenger). In addition, you need the followig Facebook permissions granted to your Facebook application, which would require signing official paperwork between your busniess entity and Facebook.
			1. *email* and *profile* - for the authentication purposes.
			1. *user_posts* - for news aggregation and maintenance of the Aigents News monitoring for your users based on their feeds o Facebook social network.
			1. *pages_messaging* - for the purpose of integration with Facebook Messenger in order to give your users ability to interact with Aigents server as a chat-bot.
		1. Your application *application id* and *application secret* and *challenege* should be passed to Aigents server as *facebook id*, *facebook key* and *facebook challenge* over secure communication channel.
		1. In the course of your application interactions with Facebook authetication, for every your user granting you with access to their Facebook account, you will need to maintain user id (as *facebook id*) and long-lived token (*facebook token*) to pass them to Aigents server over secure communication channel.
	1. Reddit
		1. Reddit integration requires to set-up your own Reddit account, create your Aigents-based Reddit application under this account and get *application id* and *application secret*.
		1. Your application *application id* and *application secret* should be passed to Aigents server as *reddit id* and *reddit key* over secure communication channel.
		1.  In the course of your application interactions with Reddit authetication, for every your user granting you with access to their Reddit account, you will need to maintain user id (as *reddit id*) and refresh token (*reddit token*) to pass them to Aigents server over secure communication channel.
	1. Telegram
		1. Telegram integration requires to set-up your own Telegram bot, and get *bot id* and *token* for it.
		1. Your application *bot id* and *token* should be passed to Aigents server as *telegram id* and *telegram token* over secure communication channel.
		1. Your users interacting with Telegram will be assigned *telegram id* which will be stored on side of the Aigents server and made available to your application over integration protocol over secure communication channel.  
	1. Slack
		1. Slack integration requires to set-up your own Slack development account, create your Aigents-based Slack application under this account and get *application id* and *application secret*. You will also need to get permissions necessary for conventional chat-bot operations.
		1. Your application *application id* and *application secret* should be passed to Aigents server as *slack id* and *slack key* over secure communication channel.
		1. Your users interacting with Slack will be assigned *slack id* which will be stored on side of the Aigents server and made available to your application over integration protocol over secure communication channel.  
	
## 2. Tecnhical aspects - "what can I do" and "how to do"

The integration based on user (called **peer** in Aigents) relies on simpler [integration based on channels](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_channel.md) and exteds the latter. The simpler channel-based integration assumes only few users are creating customized channels as a channel owners. The more complex user-based integration discussed here assumes that every individual user may create the channel (called **area** in Aigents) of their own, regardless of whether they like to get news items from the other users or not.  

The user-based integration assumes two categories of users involved: **administrator** user and **regular** user.   

1. Administrator user should be able to use integration channel to configure Aigents server in respect to the following configuration parameters for each of the social communication platforms (social networks ad messengers) as follows.
	1. The user with administrator role is identified automatically as a first user registered with and logged into the fresh Aigents Server installation.  
	1. Any of the following parameters can be set updated with the folloiwng statements in AL language.
		1. Get values of any combination of parameters - example: 
			1. Say: *What your http threads, http timeout?*
			1. Get: *My http threads 32, http timeout 60000.*
		1. Set values for any combination of parameters - example: 
			1. Say: *Your http threads 32, http timeout 60000.*
			1. Get: *Ok.*
	1. System parameters
		1. Server identification
			1. *name*
		1. Storage parameters
			1. *attention period <days>*
			1. *retention period <days>*
			1. *store cycle <seconds> sec*
		1. Email - generic parameters
			1. *email login <server email>*
			1. *email password <server email password>*
			1. *email cycle <seconds> sec*
			1. *email retries <number of retries>*
		1. Email - [Javax Mail](https://javaee.github.io/javamail/) parameters
			1. *mail.pop3.starttls.enable <true|false*>
			1. *mail.pop3s.host <host>*
			1. *mail.pop3s.port <port number>*
			1. *mail.smtp.auth <true|false>*
			1. *mail.smtp.host <host>*
			1. *mail.smtp.port <port number>*
			1. *mail.smtp.ssl.enable <true|false>*
			1. *mail.smtp.starttls.enable <true|false>*
			1. *mail.store.protocol pop3*
		1. HTTP parameters
			1. *http origin <url>*
			1. *http port <port number>*
			1. *http secure <true|false>*
			1. *http threads <number of threads>*
			1. *http timeout <milliseconds>*
			1. *cookie domain <host>*
			1. *cookie name <name of the cookie>*
		1. TCP/IP parameters - used to connect via sockets or telnet client
			1. *tcp port <port>*
			1. *tcp timeout <milliseconds>*
	1. Social communication platform integration parameters, having all *id*, *key* ad token values taken in single quotes (like saying: *Your facebook id '12345', facebook key 'aBcDeFg678900', telegram token '9876wXyZ'*).
		1. Fadcebook:
			1. *facebok id* - application id
			1. *facebook key* - application secret
			1. *facebook challenge* - needed for Facebook Messeger bot integration only
		1. Reddit:
			1. *reddit id* - application id
			1. *reddit key* - application secret
		1. Telegram:
			1. *telegram token* - obtaied with *Bot Father* bot 
		1. Slack:
			1. *slack id* - application id
			1. *slack key* - application secret
		1. Twitter ([**TBD**](https://github.com/aigents/aigents-java/issues/4)):
			1. *twitter id* - application id
			1. *twitter key* - application secret
1. Regular user should be able to perform multiple activities as follows.
	1. Get personalised news feed correspoding to their autheticated account - in the same way as it is [described for chanel/area owners](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_channel.md#51-get-news-from-users-peers-channels), including two kinds of relevance measures:
		1. *relevance* (personal) relevance based on custom user setup and their personal experience with news items;
		1. *social relevance* based on personal experiences of the other users in the list of trusts, based on their own levels of reputation, earned in the course other users providing feedback to the news items and comments made by the former ones.
	1. Get reputation levels for the list of those users in the system, who have shared their news feeds to the current user, sayig: *What is peer, friend true, trust true email, reputaion?* ([**TBD**](https://github.com/aigents/aigents-java/issues/6)).
	1. Create (author) news item to stay in their custom feed (and potentially being shared to others) - for the users impersonating WP content managers, saying: *There text '<text>', sources '<link>', times <YYYY-MM-DD>, new true, trust true update.* (example: *There text 'Aigents news feed on Reddit', sources 'https://www.reddit.com/r/aigents', times 2020-01-30, new true, trust true update.*).
	1. Comment on any news item, authoring commet item saying someth like [*TBD*](https://github.com/aigents/aigents-java/issues/6): *There text <text>, times <YYYY-MM-DD>, parents <reference>.* (example: *There text 'That is great news', times 2020-01-31 parents text text 'Aigents news feed on Reddit'.*).
	1. Provide positive “binary” (0 or 1) rating (like/vote) feedback for any news item or a comment in their feed, including items authored by other users (if any) or comments on these news items, like follows.
		1. Trust news item:  *sources '<link>', text '<text>', times <YYYY-MM-DD>, trust true.* (example *sources 'https://www.reddit.com/r/aigents', text 'Aigents news feed on Reddit', times 2020-01-30, trust true.*)
		1. Untrust news item: *sources '<link>', text '<text>', times <YYYY-MM-DD>, trust false.* (example: *sources 'https://www.reddit.com/r/aigents', text 'Aigents news feed on Reddit', times 2020-01-30, trust false.*)
	1. Provide ability to remove any news item or a comment from the feed saying: *sources '<link>', text '<text>', times <YYYY-MM-DD>, new false.* (example: *sources 'https://www.reddit.com/r/aigents', text 'Aigents news feed on Reddit', times 2020-01-30, new false.*)
	1. Specify a list of users that they like to share their custom news feeds to as public (that is, user A may share their custom news to user B or not, regardless whether B trusts to A or not), like follows.
		1. Make user to receive news from current user: *is peer, name <name>, surname <surname> email <email> share true*. 
		1. Make user to not receive news from current user: *is peer, name <name>, surname <surname> email <email> share false*. 
	1. Specify a list of users that they trust to read their own custom news feeds shared publicly to others (that is, user B may trust to get custom news from user A or not, regardless whether A shares them or not), like follows:
		1. Trust user to be source of news: *is peer, name <name>, surname <surname> email <email> trust true*. 
		1. Untrust user to be source of news: *is peer, name <name>, surname <surname> email <email> trust false*.
	1. Specify a list of websites (or news sources such as Reddit subreddits or Reddit or Telegram user channels) that user wants to have the news extracted from for their custom feeds, following the [Set up channel (area) configuration](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_channel.md#41-set-up-channel-area-configuration) instructions.
	1. Specify a list of topics (represented as Aigents [**patterns**](https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5)) that user wants to have the news extracted from for their custom feeds, following the [Set up channel (area) configuration](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_channel.md#41-set-up-channel-area-configuration) instructions.
	1. Specify their ids along with access and refresh tokens for the following social platfrom integrations, havig, having all *id*, and token values taken in single quotes (like saying: *My facebook id '12345', facebook token '345aBcDeFg678900', telegram id '78567410'*).
		1. Facebook: id, long lived token - saying *My facebook id '<id>', facebook token '<token>'.*
		1. Reddit: id, refresh token - saying *My reddit id '<id>', reddit token '<token>'.*
		1. Telegram: id - saying *My telegram id '<id>'*
		1. Slack: id - saying *My slack id '<id>'*
		1. Twitter ([**TBD**](https://github.com/aigents/aigents-java/issues/4)): id, long lived or access token

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)