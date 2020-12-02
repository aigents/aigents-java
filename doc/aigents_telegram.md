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

The Aigents Bot should be added to a Telegram group in order to perform any activity in the group, inluding replying to messages of the gropu users, monitoring and analysing the content of communications in the group for the reporting purposes, providing emotional feedback to messages in the group, computing the reputation of the group members, excuting the moderation policies or performing any other functions to be supported in the future.

Either existing [AigentsBot](https://t.me/AigentsBot) may be employed for the above, or one can create their own Aigents Bot with other bot name and add it to any groups on Telegram.

When the bot is added to the group, it must be given "**admin**" rights, but you can disable most of the admin features except "**Delete messages**" and "**Ban users**". The latter two may be disabled as well if you don't want the Aigents Bot to do that kind of moderation. 

Optionally, it is recommended to change the bot admin "**Custom title**" from default ``admin`` to something like ``bot admin`` or ``group bot``.   

## Chatting with the Aigents Bot privately 

In order to start interacting with the AigentsBot, you need to bind your current Telegram identity with your identity hosted by an Aigents Server, which can be either existing Aignts Web Demo at https://aigents.com or any other instance of Aigents Server associatd with this bot.

Since the Aigents Server is using email to identify user accounts, you need to use the same email everyhere - using Aigents Web UI or any messnger, inclusing Telegram.    

If you are not registered at the respective Aigents Server (like Aigents Web Demo at https://aigents.com) yet, you just need to register yourself with it, using the same email, name and surname that you would use to register with Aigents Web UI. You can use different email, name and surname for registrations on the Aigents Web and on Telegram but then you won’t be able to get the full access to reports and graphs on the Aigents Web later, while you will still be able to get the reports in Telegram chat.

Below is the example of the registration flow on Telegram - after this is done, one will be able to log onto the same account on the Aigents Web UI (such as https://aigents.com) or any messenger (such as Telegram): 
```
Human: login
Aigents: What your email, name, surname?
Human: aa.bb@yy.zz, aa, bb
Aigents: What your secret question, secret answer?
Human: hexadecimal code of my passport number, 12a34b5a2
Aigents: What your hexadecimal code of my passport number?
Human: 12a34b5a2
Aigents: Ok. Hello Aa Bb! My Aigents 3.1.6 Copyright © 2020 Anton Kolonin, Aigents®.
``` 

If you are already registered on the Aigents Web UI (such as https://aigents.com) or any other messenger (such as Facebook Messenger or Slack), then you can just bind your Telegram identity to your Aigents account as follows:
```
Human: login
Aigents: What your email, name, surname?
Human: aa.bb@yy.zz
Aigents: What your hexadecimal code of my passport number?
Human: 12a34b5a2
Aigents: Ok. Hello Aa Bb! My Aigents 3.1.6 Copyright © 2020 Anton Kolonin, Aigents®.
``` 

See more example with screenshots on the Aigents Bot for Telegram acces in [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d) articles [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486).

## Chatting with the Aigents Bot in a group 

[TBD](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)

## Content Monitoring with Aigents Bot

[Aigents® on Messengers — Monitoring and Searching Web Sites and Group Chats](https://medium.com/@aigents/aigents-on-messengers-monitoring-and-searching-web-sites-and-group-chats-f5d585e0355e)

1.3. [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486)

## Building Conversational Chat Bots

[Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486)

## Personal Social Reports and Graphs with Aigents Bot

In order to get the Aigents Personal Social Report on one's activity in his or her Telegram groups, onke just need to invoke the report command such as ``my telegram report`` in any of the messanger chats (Telegram, Facebook, Slack) or in the Chat view of the Aignts Web UI.  

Further, see detailed description of the different options applicable to the [Aigents Personal Social Reports](aigents_reports.md).

**TBD graphs**

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