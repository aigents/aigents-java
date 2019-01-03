#!/bin/bash

# Pre-requisite:

# 1. Make sure the current folder contains Aigents.jar built
# 2. Make sure the current folder contains the following lexicon files (may be downloaded from http://aigents.com/download/latest/)
# lexicon_english.txt      
# lexicon_russian.txt      
# 3. Serve Web server at domain localtest.com:
# 3.1. Edit hosts file, adding the line with "127.0.0.1 localtest.com"
# Mac: /private/etc/hosts
# Linux: /etc/hosts
# Windows: c:\WINDOWS\system32\drivers\etc\hosts 
# 3.2. Go to folder "http" under this folder
# 3.3. Start Web server
# Python 2: 
# sudo python -m SimpleHTTPServer 80
# Python 3:
# sudo python -m http.server 80

# Test run:

# Cleanup data
rm -rf ./al_test.txt *log.txt www is-instances is-text test*.txt

# Run Aigents
java -cp lib/mail.jar:lib/javax.json-1.0.2.jar:Aigents.jar net.webstructor.agent.Farm store path './al_test.txt', cookie domain localtest.com, console off &
sleep 5
echo Aigents server started.

# Run Tests
php -d include_path=./php/agent/ -f ./php/agent/agent_test.php

# Kill Aigents server
kill -9 $(ps -A -o pid,args | grep java | grep 'net.webstructor.agent.Farm' | awk '{print $1}')
