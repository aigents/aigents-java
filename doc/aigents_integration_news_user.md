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
1. The integration with some of the social networks and messegers assumes legal business responsibility in respect to this integration, so the following should be followed.
	1. Facebook integration requires to set-up your own business account, associate your Aigents application with this account and get *application id* and *application secret* (plus *challenege* in the case if you want to have a **bot integration** with Facebook Messenger). In additio, you need the followig Facebook permissions granted to your Facebook application.
		1. *email* and *profile* - for the authentication purposes.
		1. *user_posts* - for news aggregation and maintenance of the Aigents News monitoring for your users based on their feeds o Facebook social network.
		1. *pages_messaging* - for the purpose of integration with Facebook Messenger in order to give your users ability to interact with Aigents server as a chat-bot.
	1. Slack - TODO
	1. Reddit - TODO
	1. Telegram - TODO	       
	
## 2. TODO

TODO

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)