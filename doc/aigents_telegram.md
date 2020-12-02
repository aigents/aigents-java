# Aigents® for Telegram

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)

The following document extends Aigents configuration and usage documentation covering the two cases:
- using existing Aigents Web Demo server functions using [AigentsBot](https://t.me/AigentsBot) on Telegram;
- making the Aigents Server functions available as a chatbot under new custom Telegram bot setup.

In particular, the Aigents bot for Telegram can support interactons in groups, monitoring and analysing group conversations and providing moderation support.
 
## Related Articles on Medium

1. [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)
1. [Aigents® on Messengers — Monitoring and Searching Web Sites and Group Chats](https://medium.com/@aigents/aigents-on-messengers-monitoring-and-searching-web-sites-and-group-chats-f5d585e0355e)
1. [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486)
1. [Aigents® Sentiment Detection for Personalised News Feeds](https://blog.singularitynet.io/aigents-sentiment-detection-personal-and-social-relevant-news-be989d73b381)

## Chat Bot Setup Options

Telegram integration with Aigents is the most tecghnically simple and functionally rich at the same time. 

It is described in Medium articles called [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d) and [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486). 

There are two ways how one can use the Aigents Bot for Telegram - using existing demo [Aigents Bot](https://t.me/AigentsBot) or seting up the one of your own:

### Using existing Aigents Bot

This [Aigents Bot](https://t.me/AigentsBot) for Telegram is hosted on the Aigents Web Demo server at https://aigents.com and it is already operating in all of the [related groups](aigents_contacts.md), so it can be accessed in private chats or those groups both. 

### Setting up the Aigents Bot of Your Own

The Aigents Server support for Telegram does not require you to have a real Web server to host your own Aigents Bot on Telegram. You can host the Aigents Server for your bot on your corporate server, personal workstation, desktop and even Android tablet or smartphone. 

If you want to create the Aigents Bot for Telegram of your own, just have the [Aigents installation](https://github.com/aigents/aigents-java/blob/master/doc/aigents_server.md) done first and set up the Telegram bot next. 

To set up the bot, you need to use Telegram [BotFather](https://telegram.me/BotFather) bot to create *bot token* and *bot name* for your bot. 

Then you just need to configure your Aigents Server installation saying ``your telegram token <bot token>`` and ``your telegram name <bot name>`` in the Aigents chat console (same way as you would do the rest of the Aigents setup).

The following Telegram bot properties should be set, referring to the following as an example:

**About**:
Aigents personal AI helps you to deal with online information and social connections, see https://medium.com/@aigents
	
**Description**: 
Aigents personal artificial intelligence helps you to deal with online information and social connections, monitoring and searching the Web, tracking your interactions with friends and colleagues, and analyzing them for you, as you can read at https://medium.com/@aigents blog. The data collected from you is not re-distributed to any third party and it is protected by privacy policy: https://aigents.com/en/license.html
	
**Botpic**: ![https://github.com/aigents/aigents-java/blob/master/html/ui/img/aigent.png](https://github.com/aigents/aigents-java/blob/master/html/ui/img/aigent32.png)

## Adding Aigents Bot to your Groups

[TBD](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)

## Chatting with the Aigents Bot privately 

[TBD](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)

## Content Monitoring with Aigents Bot

[Aigents® on Messengers — Monitoring and Searching Web Sites and Group Chats](https://medium.com/@aigents/aigents-on-messengers-monitoring-and-searching-web-sites-and-group-chats-f5d585e0355e)

1.3. [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486)

## Building Conversational Chat Bots

[Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486)

## Personal Social Reports with Aigents Bot

In order to get the Aigents Personal Social Report on one's activity in his or her Telegram groups, onke just need to invoke the report command such as ``my telegram report`` in any of the messanger chats (Telegram, Facebook, Slack) or in the Chat view of the Aignts Web UI.  

Further, see detailed description of the different options applicable to the [Aigents Personal Social Reports](aigents_reports.md).

## Emotinal Feedback with Aigents Bot

[Aigents® Sentiment Detection for Personalised News Feeds](https://blog.singularitynet.io/aigents-sentiment-detection-personal-and-social-relevant-news-be989d73b381)

## Group Moderation with Aigents Bot

TBD

## Using Aigents Bot Commands 

The following Telegram bit command may be used so far:

**Commands**:
- help - Get help on some commands 
- search - Do web search, see https://medium.com/@aigents/custom-web-search-with-aigents-eb50767fc44c
- my_telegram_report - Get report on your Telegram activity https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d 
- my_facebook_report - Get report on your Facebook activity
- my_twitter_report - Get report on your Twitter activity
- my_reddit_report - Get report on your Reddit activity
- my_vkontakte_report - Get report on your VKontakte activity
- what_my_topics - List your topics of interest, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5
- what_my_sites - List your sites of interest, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5
- what_my_news_text_and_sources - List your latest news items, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)