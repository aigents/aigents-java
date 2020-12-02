# Aigents® Server Integration for Chat Bots in Messengers

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)

The following document extends Aigents configuration making the Aigents Server functions available as a chatbot under messengers such as Telegram, Facebook Messenger, or Slack.

## Related Articles on Medium

1. [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)
1. [Aigents® on Messengers — Monitoring and Searching Web Sites and Group Chats](https://medium.com/@aigents/aigents-on-messengers-monitoring-and-searching-web-sites-and-group-chats-f5d585e0355e)
1. [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486)
1. [Aigents® Sentiment Detection for Personalised News Feeds](https://blog.singularitynet.io/aigents-sentiment-detection-personal-and-social-relevant-news-be989d73b381)

## Aigents® Chat Bot Integrations


### Aigents® for Telegeram

[Aigents integration with Telegram](https://github.com/aigents/aigents-java/blob/master/src/main/java/net/webstructor/comm/telegram/Telegrammer.java) is the most simple and usable practically, see [more details on Telegram integration with Aigents](aigents_telegram.md)   
	
### Aigents® for Facebook Messenger

[Aigents integration with Facebook](https://github.com/aigents/aigents-java/blob/master/src/main/java/net/webstructor/comm/fb/Messenger.java) is moderately simple technically.

It supports chat-based interaction with your agent - either demo [Aigents Bot for Facebook Messenger](https://www.messenger.com/t/aigents) hosted on https://aigents.com/ or your own Aigents bot hosted on your own premises. However, it does not support interactoins in groups given existing official restrictions imposed by Facebook API. 

It needs the same [integration configuration as used for the news moniitoring plus extra *pages_messaging* permission and *challenge* property](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_user.md).

### Aigents® for Slack

[Aigents integration with Slack](https://github.com/aigents/aigents-java/blob/master/src/main/java/net/webstructor/comm/Slacker.java) is the most complex one in terms of configuration and it is still in alpha testing mode. It is not officially released however it can be tried already, get in touch with us for more details.

It supports chat-based interaction with your agent - either demo [Aigents Bot for Facebook Messenger](https://www.messenger.com/t/aigents) hosted on https://aigents.com/ or your own Aigents bot hosted on your own premises as well as it can support interactions in groups in a way similar to how it is done for Telegram.

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)