# Aigents® Personal Social Reports

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)

The following document describes how to use Aigents® Personal Social Reports for any of the social media sources such as Facebook, Twitter, Reddit, Steemit, Golos and VKontakte social networks, Discourse forums, Telegram and Slack messengers or even Aigents built-in social network itself.  

## Related Articles on Medium

1. [Reputation System in Aigents® supporting Telegram Groups and more](https://blog.singularitynet.io/reputation-system-in-aigents-supporting-telegram-groups-and-more-c43f0cf5053d)
1. [Social Graph as Online Mirror](https://medium.com/@aigents/social-graph-as-online-mirror-7d537e42941d)
1. [Social Graph as Mirror in the Net](https://steemit.com/psychology/@aigents/social-graph-as-mirror-in-the-net)
1. [Personal social graph analysis for Steemit and Golos](https://steemit.com/psychology/@aigents/personal-social-graph-analysis-for-steemit-and-golos)
1. [More personal Aigents analytics on Steemit for @dan, @dantheman and @son-of-satire](https://steemit.com/steemit/@aigents/more-personal-aigents-analytics-on-steemit-for-dan-dantheman-and-son-of-satire)
1. [Aigents bot on Facebook and Telegram - with flexible personal karma analytics](https://steemit.com/psychology/@akolonin/aigents-bot-on-facebook-and-telegram-with-flexible-personal-karma-analytics)
1. [Using Aigents bot service to get social network analytics for Facebook, Google+ and VKontakte](https://steemit.com/ai/@akolonin/using-aigents-bot-service-to-get-social-network-analytics-for-facebook-google-and-vkontakte)

## Accessing reports via Web User Interface

The simplest way to access the Aigents Personal Social Reports is to use Web UI for a user being logged in to the Aigents Demo Web server at https://aigents.com/ . To do so, just move your cursor to the Aigents, Twitter, Reddit, Facebook or VKontakte icon on the top right of the screen or tap on it so drop-down menu appears. Use the menu to select the **Report** option and the Personal Social Report will be generated and rendered on-screen for the selected network or social environment.

The reports generated this way are based on default reporting options such as described further. If you need to create your Personal Social Reports with custom options, use the chat-based reporting options as explained below. 
  
## Accessing reports via Chat
    
The chat interface to Aigents makes it possible to retrieve Personal Social Reports in any Aigents chat-style communication channel such as Web UI "Chat" view or any messenger such as Telegram, Slack or Facebook Messenger. 

This way, it is possible to get such reports for any messenger or blockchain environments such as Telegram, Slack, Ethereum, Steemit and Golos which are not available in the Aigents Web Demo UI at the moment.

Default report command is ``my <network> report`` where the ***network*** parameter is one of the following: ``aigents, facebook, twitter, reddit, vkontakte, telegram, slack, ethereum, steemit, golos`` - such as in the following examples.
```
my aigents report
my facebook report
my twitter report
my reddit report
my vkontakte report
my telegram report
my slack report
my ethereum report
my steemit report
my golos report
```

The extended report command may have any number of reporting options specified after the default command, such as ``my <network> report, <option1>, <option2>, <option3>``. The reporting options can be one of the two following kinds - types of the reporting sections to be included in report and properties of the report itself, as described further.

It is important to note, that reports are cached by the server so the same report for given user, specific network and particluar number of days (set by the ``period`` reporting option) can not be generated more than once per day, except the special ``fresh`` reporting option is used. That means, if the same report (for the same user, ``network`` and ``period``) has to be re-generated several times repeatedly, the ``fresh`` option must be used to get the report actually re-generated.

Currently, the reports in chat interface are retrieved by means of polling. That is, if given report is generated earlier during the current day, it is returned immediately. In fact, for the users that are active on relgualar basis the default reports are pre-generated by the Aigents Server on daily basis so they are ready for treieval most of the time.
On the other case, if either given report is not cached yet or the ``fresh`` option is specified, the report generation process is started upon the request and the message ``Your report is being prepared, please check back in few minutes...`` is replied with no report returned. In order to have the generated report retrieved, just need to repeat the request for the same report with the option ``fresh`` removed from the request. In some cases, report may take long time to generate so one may need to repeat the report request several times, till it is ready. See the example below to see how it might work.  
```
Human:my telegram report
Aigents:Your report is being prepared, please check back in few minutes...
Human:my telegram report
Aigents:<report.html>
Human:my telegram report
Aigents:<report.html>
Human:my telegram report fresh
Aigents:Your report is being prepared, please check back in few minutes...
Human:my telegram report
Aigents:Your report is being prepared, please check back in few minutes...
Human:my telegram report
Aigents:<report.html>
```

Below we discuss the two kinds of the reporting options - types of the reporting sections to be included in report and properties of the report itself.

### Types of the reporting sections

The following report sections may be used, depending on source network or social environment - some of the sections are specific to particular source and some may be not available for it. 

The sections marked as **default** are included when report is requested with default ``my <network> report`` request. If any combination of the options is specificed explicitly, like in ``my telegram report, similar to me, social graph, fresh``, then all of the other options are excluded automatically so only the specified options are handled.

- ``my interests`` - **default**, clusters of the posts/messages corresponding to interests of the current user, labeled by keywords typical to respective posts/messages
- ``interests of my friends`` - **default**, clusters of the posts/messages corresponding to interests of other users excluding the  current user, labeled by keywords typical to respective posts/messages
- ``similar to me`` - **default**, other users ranked by similarity in respect to the current user
- ``best friends`` - **default**, other users that are the most involved in mutual communications (likes, votes, comments and mentions) with the current user
- ``fans`` - **default**, other users that are the most involved in communications (likes, votes, comments and mentions) directed to the current user but not the other way
- ``like and comment me`` - other users who provide likes, votes and comments in respect to the current user
- ``authorities`` - **default**, other users that are getting the most of communications (likes, votes, comments and mentions) from the current user
- ``reputation`` - **default**, list of the users with highest reputation score within entire reachable community, including any users visible given privacy restrictions
- ``social graph`` - **default**, rendering of the nearest social environment in form of interactive social graph 
- ``liked by me`` - other users that are getting the most of likes and votes from the current user
- ``my karma by periods`` - dynamics of the "karma" (social capital) for the current user
- ``my words by periods`` - **default**, words used by the current user getting most of attention (likes, votes, comments and mentions) from other users, broken down by time periods
- ``my friends by periods`` - **default**, other users paying attention (likes, votes, comments and mentions) to the current user, broken down by time periods
- ``my favorite words`` - **default**, words most oftenly used, liked and commented by the current user
- ``my posts liked and commented`` - **default**, posts of the current user most oftenly liked and commented by the other users
- ``my best words`` - words most oftenly liked and commented by all of the users including the current user 
- ``my words liked and commented`` - words of the current user most oftenly liked and commented by the other users
- ``words liked by me`` - words most oftenly liked by the current user 
- ``words of my friends`` - **default**, words most oftenly used by users other than the current user
- ``my posts for the period`` - **default**, all posts by the current user for given period

### Properties of the report

The following report properties are key-value pairs, with specific default values implied.

- ``format`` - format of the report - can  be either ``html`` (default, for visual perception) or ``json`` (optional, for the purposes of integration with other applications or rendering on the client side)
- ``threshold`` - setting defining the lowest level of relevance/rank of a given item in the table (words, users, etc.) to be rendered - in range from 0 to 100 percents (default is 20), so that 0 means everything is included while 100 means that only the top items are included
- ``period`` - number of days to include in the data set used to generate the report, default value is defined automatically, increasing interval (1 day, 1 week, 1 month, 1 quarter, 1 year, 4 years, 16 years) till at least some data is found to fill the report
- ``areas`` - optional areas or tags to restrict the scope of data used to generate the report, no default is provided so all data is included is this property is not set

![https://aigents.com/](https://aigents.com/img/aigents_wrench.png)