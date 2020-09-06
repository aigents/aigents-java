/*
Copyright 2018-2020 Anton Kolonin, Aigents®

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

//TODO: support for browsers other than Chrome
var SpeechRecognition = window.webkitSpeechRecognition; //|| window.mozSpeechRecognition || window.msSpeechRecognition || window.oSpeechRecognition || window.SpeechRecognition;
var recognition = null;

function hear_voice(oninput,onerror,lang,nonstop){
    			function stopHearing(){
    				$('#microphone').attr('class', 'ui-icon-mic');
    			}
        
    			function startHearing(){
                	$('#microphone').attr('class', 'ui-icon-micr');
    			}
                if (!SpeechRecognition || !document.getElementById('microphone')){
                        console.log('Unsupported');
                }
                if (!SpeechRecognition)
                    return;
                if (recognition && recognition.recognizing){
                    recognition.recognizing = false;
                    recognition.nonstop = false;
                    recognition.stop();
                    stopHearing();
                    return;
                }
            //recognition = new webkitSpeechRecognition();
            recognition = new SpeechRecognition();
            recognition.nonstop = nonstop;
            if (get_locale) {
            	var l = get_locale();
            	if (l == 'ru')
            		lang = 'ru-RU';
            }
            recognition.lang = lang ? lang : 'en-US';
            startHearing();
            recognition.onstart = function() {
            	recognition.recognizing = true;
                console.log('Turned on');
            };
        	recognition.onerror = function(event) {
                if (recognition.nonstop)
                	return;
        		console.log('error='+event);
        		onerror('Make sure you have microphone enabled and give access to it for aigents.com site at chrome://settings/contentExceptions#media-stream');
        		stopHearing();
        	};
        	recognition.onend = function() {
            	//TODO maybe just instead of that, do that in continious mode?
                if (recognition.nonstop && recognition.recognizing){
                	recognition.start();
                    return;
                }
                recognition.recognizing = false;
                stopHearing();
                console.log('Turned off');
        	}
        	recognition.onresult = function(event) { 
        	    var transcript = '';
        	    for (var i = event.resultIndex; i < event.results.length; ++i) {
        	        transcript += event.results[i][0].transcript;
        	    }
        	    console.log('input='+transcript);
        	    oninput(transcript);
        	}
        	recognition.start();
}
        	
function speak_aloud(input,lang){
	    speechSynthesis.cancel();//workaround for https://code.google.com/p/chromium/issues/detail?id=335907
	    var input = input.replace(/_/g, " ");
	    var u = new SpeechSynthesisUtterance(input);
	    u.lang =  lang ? lang : 'en-US';
	    speechSynthesis.speak(u);			
}
    	

