# Aigents® Server Integration for Chat Bots in Messengers

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)

The following document extends Aigents configuration making the Aigents Server functions available as a chatbot under messengers such as Telegram, Facebook Messenger, or Slack.

## 1. General information

1.1. [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)
1.2. [Aigents® on Messengers — Monitoring and Searching Web Sites and Group Chats](https://medium.com/@aigents/aigents-on-messengers-monitoring-and-searching-web-sites-and-group-chats-f5d585e0355e)

## 2. Chat Bot Integration


### 2.1. Telegeram

Telegram integration with Aigents is the most simple and functionally rich at the moment and described in [very details]((https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)). In particular, the Aigents bot for Telegram can support interactons in groups.    

In addion to the integration on the Aigents side, the following Telegram bot properties should be set, referring to the following as an example:

**About**:
Aigents personal AI helps you to deal with online information and social connections, see https://medium.com/@aigents

**Description**: 
Aigents personal artificial intelligence helps you to deal with online information and social connections, monitoring and searching the Web, tracking your interactions with friends and colleagues, and analyzing them for you, as you can read at https://medium.com/@aigents blog. The data collected from you is not re-distributed to any third party and it is protected by privacy policy: https://aigents.com/en/license.html

**Botpic**: ![https://github.com/aigents/aigents-java/blob/master/html/ui/img/aigent.png](https://github.com/aigents/aigents-java/blob/master/html/ui/img/aigent32.png)

**Commands**:
help - Get help on some commands 
search - Do web search, see https://medium.com/@aigents/custom-web-search-with-aigents-eb50767fc44c
my_telegram_report - Get report on your Telegram activity https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d 
my_facebook_report - Get report on your Facebook activity
my_twitter_report - Get report on your Twitter activity
my_reddit_report - Get report on your Reddit activity
my_vkontakte_report - Get report on your VKontakte activity
what_my_topics - List your topics of interest, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5
what_my_sites - List your sites of interest, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5
what_my_news_text_and_sources - List your latest news items, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5
   
	
### 2.2 Facebook

Facebook integration with Aigents is moderately simple although it does not support integractoipns in groups given existing restriction in Facebook API. It needs the same [integration configuration as used for the news moniitoring plus extra *pages_messaging* permission and *challenge* property](https://github.com/aigents/aigents-java/blob/master/doc/aigents_integration_news_user.md).

### 2.3. Slack

Slack integration with Aigents is the most complext one in terms of configuration and it is still in alpha testing mode. The application is not iffifially released even though it can be tried already and it can support use interactions in groups.

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)