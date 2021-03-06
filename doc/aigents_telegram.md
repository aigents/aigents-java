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

Telegram integration with Aigents is the most simple technically and rich functionally at the same time. 

It is described in Medium articles called [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d) and [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486). 

There are two ways how one can use the Aigents Bot for Telegram - using existing demo [Aigents Bot](https://t.me/AigentsBot) or seting up the one of your own.

### Using existing Aigents Bot

The [Aigents Bot](https://t.me/AigentsBot) for Telegram is hosted on the Aigents Web Demo server at https://aigents.com and it is already operating in all of the [respective Telegram groups](aigents_contacts.md), so it can be accessed in those groups as well as in private chats. 

### Setting up the Aigents Bot of Your Own

The Aigents Server support for Telegram does not require you to have a real Web server to host your own Aigents Bot on Telegram. You can host the Aigents Server for your bot on your corporate server, personal workstation, desktop and even Android tablet or smartphone. 

If you want to create the Aigents Bot for Telegram of your own, just have the [Aigents Server installation](https://github.com/aigents/aigents-java/blob/master/doc/aigents_server.md) done first and set up the Telegram bot next. 

To set up the bot, you need to use Telegram [BotFather](https://telegram.me/BotFather) bot to create ***bot token*** and ***bot name*** for your bot. 

Then you just need to configure your Aigents Server installation saying ``your telegram token <bot token>`` and ``your telegram name <bot name>`` in the Aigents chat console (the same way as you would do for the rest of the Aigents setup).

The following Telegram bot properties should be set, referring to the example below, but using your own branding ideas and context:

**About**:
Aigents personal AI helps you to deal with online information and social connections, see https://medium.com/@aigents
	
**Description**: 
Aigents personal artificial intelligence helps you to deal with online information and social connections, monitoring and searching the Web, tracking your interactions with friends and colleagues, and analyzing them for you, as you can read at https://medium.com/@aigents blog. The data collected from you is not re-distributed to any third party and it is protected by privacy policy: https://aigents.com/en/license.html
	
**Botpic**: ![https://github.com/aigents/aigents-java/blob/master/html/ui/img/aigent.png](https://github.com/aigents/aigents-java/blob/master/html/ui/img/aigent32.png)

## Adding Aigents Bot to your Groups

The Aigents Bot should be added to a Telegram group in order to perform any activity in the group, including replies to messages of the group users, monitoring and analysing the content of communications in the group for the reporting purposes, providing emotional feedback to messages in the group, computing the reputation of the group members, executing the moderation policies or performing any other functions to be supported in the future.

Either existing [AigentsBot](https://t.me/AigentsBot) may be employed for the above, or one can create their own Aigents Bot with other bot name and add it to any groups on Telegram.

When the bot is added to the group, it must be given "**admin**" rights, but you can disable most of the admin features except "**Delete messages**" and "**Ban users**". The latter two may be disabled as well if you don't want the Aigents Bot to do that severe kind of moderation. 

Optionally, it is recommended to change the bot admin "**Custom title**" from default ``admin`` to something like ``bot admin`` or ``group bot``.   

## Chatting with the Aigents Bot privately 

In order to start interacting with the AigentsBot, you need to bind your current Telegram identity with your identity hosted by an Aigents Server, which can be either existing Aignts Web Demo at https://aigents.com or any other instance of Aigents Server associatd with given bot.

Since the Aigents Server is using email to identify user accounts, you need to use the same email everywhere - using Aigents Web UI or any messenger, including Telegram.    

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

If you are already registered on the Aigents Web UI (such as https://aigents.com) or any other messenger (such as Facebook Messenger or Slack), then you can just bind your Telegram identity to your Aigents account by login on to it as follows:
```
Human: login
Aigents: What your email, name, surname?
Human: aa.bb@yy.zz
Aigents: What your hexadecimal code of my passport number?
Human: 12a34b5a2
Aigents: Ok. Hello Aa Bb! My Aigents 3.1.6 Copyright © 2020 Anton Kolonin, Aigents®.
``` 

See more examples with screenshots on the Aigents Bot for Telegram in [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d) and [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486) articles.

## Chatting with the Aigents Bot in a group 

The Aigents Bot can be accessed in any group where it is added to. It can be done by either mentioning it in the beginning of the message like ``@AigentsBot help`` or replying to the bot in the group on any of earlier messages from the bot. 

If a group user who is replying to the Aigents Bot or mentioning it is a registered Aigents user who has his or her Telegram identity bound to respective Aigents account earlier, the Aigents Bot will be replying to the user right in the group.

If a group user who is replying to the Aigents Bot or mentioning it is not a registered Aigents user, it is being asked from the Aigents Bot for authorization in private chat so the Telegram identity of the user is bound to respective Aigents account. When the latter is done, the conversation between the user and the Aigents Bot may continue either in the private channel of in the original group.  

If you are interacting with your AigentsBot in a group, be careful not to communicate your private infromation in the public group - use only the private chat with the Aigents Bot to exchange any sensitive information related to your Aigents account.   

## Content Monitoring with Aigents Bot

Any Telegram user who has its Telegram identity bound to Aigents account can monitor any of the groups where the Aigents Bot is added and the user is member of this group herself or himself. In order to do so, user should specify his or her **topics** of interest using either Aigents Web UI or using Aigents Chat interface like it is done for the purpose of news monitoring on the World Wide Web for Web sites, RSS feeds or social media channels. See the [documentation on personal montoring channel set up](aigents_integration_news_channel.md) and the article on [Aigents® on Messengers — Monitoring and Searching Web Sites and Group Chats](https://medium.com/@aigents/aigents-on-messengers-monitoring-and-searching-web-sites-and-group-chats-f5d585e0355e) for how to setup the **topics** for the news monitoring.

When the topics are set up, whenever a new message matching the configured topics is posted by anyone in any of the groups where a user is present together with the Aigents Bot, the user is provided with a news item in the news feed referring to given Telegram group. At the same time, the user may be alerted by any way that he or she has the notifications set up, including the alerts in private Telegram chat channel with the Aigents Bot.     

## Getting Notifications in Private Chat 

Whenever a news item is found by means of the [Aigents monitoring in online, social media or chat groups](https://medium.com/@aigents/aigents-on-messengers-monitoring-and-searching-web-sites-and-group-chats-f5d585e0355e), the user may be notified by any channel enabled in her or his Aigents profile. In particular, if a user has the Telegram identity bound to Aigents account, they can get any notifications in the private chat with Aigents Bot.

To check the status for notifications from the bot on Telegram, ask ``what my telegram notification?``.  

To disable the notifications from the bot in Telegram, tell ``my telegram notification false``.  

To enable the notifications from the bot in Telegram, tell ``my telegram notification true``.  

## Building Conversational Chat Bots

Any Aigents user and develop their own conversational chat bot to be accessed via any channel supported by the Aigents platform, including Telegram, Slack and Facebook Messenger as well as Aigents Web UI Chat. 

For the chat bot development purpose, user can use the same chat interface as used for any other interaction with the Aigents, including the Telegram chat. See more details on this in the article [Welcome Aigents’ Bots](https://medium.com/@aigents/welcome-aigents-bots-d6682968f486).

## Personal Social Reports and Graphs with Aigents Bot

In order to get the Aigents Personal Social Report on one's activity in his or her Telegram groups, one just needs to invoke the report command such as ``my telegram report`` in any of the messenger chats (Telegram, Facebook, Slack) or in the Chat view of the Aigents Web UI.  

Further, see detailed description of the different options applicable to the [Aigents Personal Social Reports](aigents_reports.md).

In order to do the interactive social graph analysis on interactions in groups on Telegram (or any other social media environment as well), one has to use Aigents Web UI "Graph" tab, which makes it possible to browse social graphs for any configured social media sources where the user is registered and has the respective identities bound to their Aigents account. For more information on using Aigents Graphs refer to the articles: [Graphs Part 3: Aigents Graph Analysis for Blockchains and Social Networks](https://blog.singularitynet.io/graphs-part-3-aigents-graph-analysis-for-blockchains-and-social-networks-142fc8182389) and [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)

## Emotional Feedback with Aigents Bot

The Aigents Bot can be set up to react on messages in Telegram groups with "positive" or "negative" sentiment, using the technology described in the article: [Aigents® Sentiment Detection for Personalised News Feeds](https://blog.singularitynet.io/aigents-sentiment-detection-personal-and-social-relevant-news-be989d73b381).

The emotional reaction on "postivie" or "negative" messages is provided with replies to respective messages with emoticons corresponding to "smiling face with smiling eyes" 😊 or "disappointed face" 😞, respectively.

Emotional "sensitivity" is configured with corresponding ***sentiment threshold*** setting configured for the Aigents Server in range between 1 and 99, where 1 is minimum (so almost any message could be easily treated as either "positive" or "negative") and 99 is maximum (so almost no any message would be given emotional feedback). 

For instance, configuring Aigents Server with ``your sentiment threshold 50`` might be still low so too many messages might get emotinally overreacted while configuring it with ``your sentiment threshold 90`` could be too conservative so the emotinal reactions would be barely observed. Asking the server for ``what your sentiment threshold?`` retrieves the current setting.

In addition to the above, extra emotional reaction is provived in respect to the messages involving obscene vocabularies for [English](https://github.com/aigents/aigents-java/blob/master/lexicon_rude_english.txt) and [Russian](https://github.com/aigents/aigents-java/blob/master/lexicon_rude_russian.txt). Appearance of any of the words listed in those vocabularies (or edited versions of these vocabularies deployed along with the Aigents Server) in a Telegram group message will cause Aigents Bot replying to such messages with the "flushed face" 😳 emoticon.

## Group Moderation with Aigents Bot

Besides emotional reactions to onscene lexicon appearing in the group messages, the Aigents Bot reports these messages to real human group admins, if these admins have their Telegram identities bound to respective Aigents accounts. The reported messages are forwarded to group admins in their private chats with the Aigents Bot.

In addition to that, if the degree of "rudeness" of a message, measured relying on the amount of the obscene words in it, exceeds the configurable ***rudeness threshold*** then the abusing message is automatically deleted by the Aigents Bot itself. Setting ``your rudeness threshold 1`` ensures that any message containing obscene lexicon is automatically deleted, while setting ``your rudeness threshold 75`` mignt be more tolerant. Asking the server for ``what your rudeness threshold?`` retrieves the current setting. 

## Using Aigents Bot Slash Commands 

The following Telegram bot slash commands may be used so far:

**Commands**:
- help - Get help on some commands 
- my_telegram_report - Get report on your Telegram activity https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d 
- my_facebook_report - Get report on your Facebook activity
- my_twitter_report - Get report on your Twitter activity
- my_reddit_report - Get report on your Reddit activity
- my_vkontakte_report - Get report on your VKontakte activity
- what_my_topics - List your topics of interest, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5
- what_my_sites - List your sites of interest, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5
- what_my_news_text_and_sources - List your latest news items, see https://medium.com/@aigents/aigents-news-monitoring-tips-and-tricks-ab8d2ede2fa5

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)
