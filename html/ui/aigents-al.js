/*
Copyright 2018 Anton Kolonin, Aigents Group

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

//https://stackoverflow.com/questions/5899783/detect-safari-using-jquery
var isSafariFlag = null;
function isSafari(){
	if (isSafariFlag == null)
		isSafariFlag = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);;
	return isSafariFlag;
}

//parse HTTP query from URL
function parseQuery(queryString) {
    var query = {};
    var pairs = (queryString[0] === '?' ? queryString.substr(1) : queryString).split('&');
    for (var i = 0; i < pairs.length; i++) {
        var pair = pairs[i].split('=');
        query[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1] || '');
    }
    return query;
}

function parseToFields(input) {
	var list = [];
	var p = new Parser(input);
	while (!p.end()) {
		var name = '';
		var s = null;
    	while (!p.end()) { // glueing multi-word names from multiple tokens
    		s = p.parse();
    		if (AL.empty(s) || AL.punctuation.indexOf(s) != -1) // break field formation on any delimiter
    			break;
    		if (name.length > 0)
    			name += ' ';
			name += s;
    	}
    	if (name.length > 0)
    		list.push(name);
    	if (AL.empty(s) || AL.periods.indexOf(s) != -1) //break list formation on period
    		break;
	}
	return list;		
}

function ask(message,type,values) {
	var title = _('Your properties (registration/login)');
	var list = parseToFields(message);
	var cancel = null;
	if (!type)
		if (!logged_in)
			type = contains(list,['email'])? 'login' 
				//: contains(list,['name','surname','birth date'])? 'registration'
				: contains(list,['name','surname'])? 'registration'
				: contains(list,['verification code'])? 'verification' 
				: list.length == 1 ? 'confirmation'
				: null ;//secret question + secret answer
		else
			type = contains(list,['verification code'])? 'verification' : null ;
	if (type == 'login'){
		title = _('Login by email');
		list = ['email'];			
	} else
	if (type == 'registration'){
		title = _('Register by email');
		//list = ['email','name','surname','birth date'];
		list = ['email','name','surname'];
		values = !AL.empty(logged_email) ? [logged_email] : null;
	} else 
	if (type == 'confirmation'){
		title = _('Confirmation of identity');
		cancel = function(){ requestBase(null,'Not.'); }
	} else 
	if (type == 'verification'){
		title = _('Confirmation of email');
		cancel = function(){ requestBase(null,'Not.'); }
	}
	if (!AL.empty(list)) {
		function submit(){
			var q = dialog_qualifier(list);
			if (type == 'login')
				logged_email =  $( '#'+name_to_id('email') ).val();
			if (!AL.empty(q)){
				requestBase(null,'My '+q+'.');
				return true;
			}
			//else//TODO: select unfilled field
		}
		dialog_open(title,null,list,null,values,false,submit,type,cancel);
	}
}

function dialog_qualifier(list) {
	var q = '';
	list.forEach(function(name) {
		var id = name_to_id(name);
		var val = $( '#'+id ).val();
		//console.log(name+"="+val);
		if (!AL.empty(val)) {
			if (q.length > 0)
				q += ', ';
			q += AL.toString(name,null);
			q += ' ';
			q += AL.toString(val,name);
		}
	});
	return q;
}

function errorMessage(message) {
	if (message.indexOf("Error") == 0 || message.indexOf("No") == 0)
		return message;
	if (message.indexOf("Java.lang.Exception: ") == 0)
		return capitalize(message.substring("Java.lang.Exception: ".length));
	return null;
}

//TODO: see if can get rid of explicit joiner specification
function qualifier(names,values, joiner) {
	if (!joiner)
		joiner = ' and ';
	var q = '';
	for (var i = 0; i < names.length && i < values.length; i++) {
		var name = names[i];
		var val = values[i];
		if (name && val) {
			if (q.length > 0) {
				q += joiner;//TODO: see why implicit AND does not work sometimes
			}
			q += AL.toString(name,null); 
			q += ' '; 
			//q += AL.toString(val,name);
			q += AL.toString(Array.isArray(val)? val[0]: val, name);//TODO:deal with arrays!!!
		}
	}
	return q;
}

//AL-parsing
var Schema = {
	trust : "trust",
	share : "share",
	_true : "true",
	text : "text",
	email_password : "email password",
	boolean : function(name) {
		return name == this.trust || name == this.share;
	},
	quotable : function(name) {
		var lower = name.toLowerCase();
		return lower == this.email_password || lower == this.text;
	}	
};

var AL = {
	space : " ",
	spaces : " \t\r\n",
	commas : ",;",
	periods : ".!?",
	punctuation : "[({})],;.!?~",
	separators : ".,:",
	special : "+#=-*/\\",
	empty : function(anything) {
		return !anything || anything.length == 0;
	},
	toNumber : function(string) {
		return AL.empty(string) ? 0 : Number(string);
	},
	toString : function(value,name) {
		if (AL.needsQuoting(value) || (name && Schema.quotable(name)))
			return quote(value);
		else
			return value;
	},
	isURL : function(string) {
		var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
		return regexp.test(string);	
	},
	isPunctuationOrSpace : function (c) {
		return AL.punctuation.indexOf(c) != -1 || AL.spaces.indexOf(c) != -1;
	},	
	punctuationException : function(token, cur, length) {
		if (AL.separators.indexOf(token.charAt(cur)) == -1)//not allow anything but , and . inside
			return false;
		if ((cur + 1) == length || AL.isPunctuationOrSpace(token.charAt(cur + 1)))
			return false;
		return true;
	},
	needsQuoting : function(string) {
		if (AL.empty(string)) // or just if string.length() == 0 ?
			return true;
		var url = AL.isURL(string);  
		for (var i = 0; i < string.length;) {
			var ch = string.charAt(i++);
			if (url && ch != ch.toLowerCase())
				return true;
			if (AL.punctuation.indexOf(ch) != -1) // if punctiation in url or before the end or spaces
				//if (url || i == string.length || AL.spaces.indexOf(string.charAt(i)) != -1)
				if (url || !AL.punctuationException(string,i,string.length))
					return true;
		}
		return false;
	},
	datetimecmp : function (a,b) {
		if (a == b) 
			return 0;
		if (a == 'tomorrow')
			return -1;
		if (b == 'tomorrow')
			return 1;
		if (a == 'today')
			return -1;
		if (b == 'today')
			return 1;
		if (a == 'yesterday')
			return -1;
		if (b == 'yesterday')
			return 1;
		return strcmp(b,a);
	},
	match : function (text, patt) {
		var a1 = text.trim().toLowerCase().split(' ');
		var a2 = patt.trim().toLowerCase().split(' ');
		if (a1.length != a2.length)
			return false;//TODO: enable sparse matching?
		for (var i in a1)
			if (a1[i] != a2[i])
				return false;
		return true;
	} 
};

function strcmp(a, b){   
    return (a < b ? -1 : (a > b ? 1 : 0 ));  
}

function contains(array,samples) {
	if (array && samples){
		for (var i = 0; i < array.length; i++)
			for (var j = 0; j < samples.length; j++)
				if (array[i] == samples[j])
					return true;
	}
}

function textMatchingAnyString(text,samples) {
	if (text && samples && samples.length > 0)
		for (var i = 0; i < samples.length; i++)
			if (text.indexOf(samples[i]) != -1)
				return true;
	return false;
}

function capitalize(str) {
	return str ? str.substr(0,1).toUpperCase()+str.substr(1) : str;
}

function isEmail(email) { 
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
} 
function isPunctuationOrSpace(c) {
	return AL.punctuation.indexOf(c) != -1 || AL.spaces.indexOf(c) != -1;
}

//http://stackoverflow.com/questions/1144783/replacing-all-occurrences-of-a-string-in-javascript
function escapeRegExp(string) {
    return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
}
function replace_all(string, find, replace) {
	return string.replace(new RegExp(escapeRegExp(find), 'g'), replace);
}

function unquote(s) {
	var s = replace_all(s,"\\\'","\'");
	return replace_all(s,"\\\"","\"");
}

function quote(str) {
	var one = str.indexOf('\'') != -1;
	var two = str.indexOf('\"') != -1;
	var q = one ? '\"' : '\''; 
	if (one && two){
		str = replace_all(str,"\'", "\\\'");
		str = replace_all(str,"\"", "\\\"");
	}
	return q + str + q;
}

function Parser(input) {
    this.input = input;
    this.pos = 0;
    this.size = !input ? 0 : input.length;
    this.test = function() {
		for (var cur = this.pos; cur < this.input.length; cur++) {
			var c = input.charAt(cur);
			if (AL.spaces.indexOf(c) == -1){
				this.pos = cur;
				return c;
			}
		}
		return null;
    };
    this.parse = function() {
    	//TODO:
		var quoted = 0;
		var begin = -1;
		var cur;
		for (cur = this.pos; cur < this.input.length; cur++) {
			var c = input.charAt(cur);
			if (begin == -1 && quoted == 0 && (c == '\'' || c == '\"')) { // start quoting
				quoted = c;
			}
			else
			if (quoted != 0) { //keep quoting
				if (begin == -1)
					begin = cur;
				//ensure escaping \' and \"
				var escaping = false;
				if (c == '\\'){
					var next = cur + 1;
					if (next < this.input.length){
						var nc = input.charAt(next);
						if (nc == '\'' || nc == '\"')
							escaping = true;
					}
				}
				if (escaping)
					cur++;
				else
				if (quoted == c) {
					var s = input.substring(begin,cur);
					this.pos = ++cur;
					return unquote(s);//no .toLowerCase() for quoted strings!
				}
			}
			else
			if (begin == -1) { //not forming token
				if (AL.punctuation.indexOf(c) != -1 && !this.punctuationException(cur)) {//return symbol
					var s = input.substring(cur,cur+1);
					this.pos = ++cur;
					return s.toLowerCase();
				}
				if (AL.spaces.indexOf(c) == -1)//start froming token
					begin = cur;
				//else skip delimiters
			} else { //in the middle of the token
				if (AL.punctuation.indexOf(c) != -1 && !this.punctuationException(cur)) {//return token
					var s = input.substring(begin,cur);
					this.pos = cur;//stay at delimiter to return symbol later
					return s.toLowerCase();
				}
				if (AL.spaces.indexOf(c) != -1) {//return token
					var s = input.substring(begin,cur);
					this.pos = ++cur;//skip delimiter
					return s.toLowerCase();
				}
				//else keep forming token
			}				
		}
		this.pos = cur;
		if (begin != -1)
			return input.substring(begin).toLowerCase();
    	return null;
    };
    this.parseSeq = function(seq, force) {
		var cur = this.pos;
		for (var i = 0; i<seq.length; i++) {
			var s = this.parse();//.toLowerCase();
			if (s == null || s != seq[i]) {
				this.pos = cur;//rollback
				return false;
			}
		}
		if (!force)
			this.pos = cur;//rollback if only trying
		return true;//advance
	};
	this.parseAny = function(any,force) {
		for (var i = 0; i<any.length; i++) {
			var cur = this.pos;
			//var seq = any[i].split(AL.spaces);//TODO:invent anything better?
			var seq = any[i].split(/[ \t\r\n]+/);//TODO:invent anything better?
			if (this.parseSeq(seq,force)) {
				if (!force)
					this.pos = cur;//rollback if only trying
				return any[i];//advance
			}
			this.pos = cur;//rollback
		}
		return null;
	};
	this.parseObj = function() {
		var o = {};
		while (!this.end()) {
			var n = this.parse();//property name is one word
			if (!AL.empty(n) && AL.punctuation.indexOf(n) == -1){
				var done = false;
				var v = "";
				while (!this.end()) {//property value may be multi-word
					var s = this.parse();
					if (s == ",")
						break;//break forming value, go to next property
					else
					if (AL.punctuation.indexOf(s) != -1){
						done = true;
						break;
					} else
					if (!AL.empty(s))
						v += (v.length == 0 ? s : ' ' + s);
					else
						break;
				}
				if (AL.empty(v))
					break;
				o[n] = v;
				if (done)
					break;
			}
		}
		return AL.empty(o) ? null : o;
	};
    this.punctuationException = function(cur) {
    	if (AL.separators.indexOf(this.input.charAt(cur)) == -1)//not allow anything but , and . inside
    		return false;
    	if ((cur + 1) == this.size || isPunctuationOrSpace(this.input.charAt(cur + 1))) 
    		return false;
    	//TODO: do really allow points and periods heading the tokens?
    	//if (cur == 0  || isPunctuationOrSpace(input.charAt(cur - 1)))
    	//	return false;
    	return true;
    };
    this.end = function() {
		return this.pos >= this.size; 
	};
}

function parseBetween(input, from, to){
	var p = input.indexOf(from);
	if (p >= 0){
		p += from.length;
		var e = input.indexOf(to,p);
		return e < 0 ? input.substring(p) : input.substring(p,e);
	}
}

function encode_urls(text){
	return text.replace(/(?:(https?\:\/\/[^\s]+))/g,'<a href="$1" target="_blank">$1</a>');
}

function parse(input, delimeters) {
	var chunks = [];
	var parser = new Parser(input); 
	var sb = '';
	var s;
	while ((s = parser.parse()) != null) {
		if (!delimeters){
			chunks.push(s);
		} else //TODO: remove this branch if not actually used
		if (delimeters.indexOf(s) == -1) { //not a delimiter
			if (sb.length > 0)
				sb += AL.space;
			sb += s;
		} else {
			if (sb.length > 0)
				chunks.push(sb);
			sb = '';
		}
	}
	if (sb.length > 0)
		chunks.push(sb);
	return chunks;
}

function parseToList(input, columns, listSeparator) {
	var rows = [];
	parseToGrid(rows, input, columns, listSeparator);
	if (rows.length > 0){
		var list = [];
		for (var i = 0; i < rows.length; i++){
			var row = rows[i];
			var o = {};
			for (var c = 0; c < columns.length; c++)
				if (row[c] && row[c].length > 0)
					o[columns[c]] = row[c];
			list.push(o);
		}
		return list;
	}
}

function parseToGrid(rows, input, columns, listSeparator) {
	var sorted_columns = columns.slice(); 
	sorted_columns.sort(function(a, b){return b.length - a.length;});
	rows.length = 0;
	var parser = new Parser(input);
	var cells = [];
	while (!parser.end()) {
		var name = parser.parseAny(sorted_columns,true);
		if (name == null)
			break;
		var delimiter = null;		
		var sa = [];
		var sb = '';
		while (!parser.end()) {
			var quoted = '\'\"'.indexOf(parser.test()) != -1;
			var token = parser.parse();
			if (token && token.length > 0 && AL.punctuation.indexOf(token) != -1 && !quoted) {
				delimiter = token;
				var test = parser.parseAny(sorted_columns,false);
				if (test == null && delimiter == listSeparator) {//handle multiple values
					sa.push(sb);
					sb = '';
					continue;
				} else
					break;
			}
			if (sb.length > 0)
				sb += ' ';
			sb += token;
		}
		if (sa.length > 0) { //array accumulator is not empty 
			sa.push(sb);
			sb = sa;
		}
		
		cells[columns.indexOf(name)] = Schema.boolean(name) ? sb == Schema._true : sb;
		if (delimiter == null || delimiter != listSeparator) {
			rows.push(cells);			
			cells = [];
		}
	}
	//return rows;
}


//// color helpers ////

// convert decimal to hexadecimal
function dec2hex(d){
	return d > 15 ? d.toString(16): "0"+d.toString(16);
}

// convert R,G,B integers to CSS color reference
function rgb(r,g,b){
	return "#"+dec2hex(r)+dec2hex(g)+dec2hex(b);
}

// adjust color (input: "#RRGGBB") saturation from white to fully saturated color
function RGBtoFFFFFF(colorRGBhex,saturation100){
	function colorToFF(color,toFF){
		var color = color + Math.round((255 - color) * toFF);
		return color < 255 ? color : 255;
	}
	var r = parseInt(colorRGBhex.substring(1,3), 16);
	var g = parseInt(colorRGBhex.substring(3,5), 16);
	var b = parseInt(colorRGBhex.substring(5,7), 16);
	var toFF = (saturation100)/100;//to white
	return rgb(colorToFF(r,toFF),colorToFF(g,toFF),colorToFF(b,toFF));
}


//// HTML helpers ////

function create_selector(title,cls,id,value,options,labels){
	if (!labels)
		labels = options;
	var str = '<select title="'+title+'" class="'+cls+'" id="'+id+'">';
	for (var i = 0; i < options.length; i++){
		var selected = value == options[i] ? ' selected="selected"' : '';
		str = str.concat('<option value="'+options[i]+'"'+selected+'>'+labels[i]+'</option>');
	}
	return str.concat('</select>');
}


//// name helpers ////

function splitName2(name){
	var names = name.split(' ');
	if (names.length < 2)
		return [name,""];
	if (names.length < 3 || name < 40){//split in 2
		var half = names.length < 5 ? names.length / 2 : names.length * 2 / 5;
		var first = names.slice(0,half).join(' ');
		var second = names.slice(half,names.length).join(' ');
		return [first,second];
	}else{//split in 3
		var onethird = names.length / 3;
		var twothirds = names.length * 2 / 3;
		var first = names.slice(0,onethird).join(' ');
		var second = names.slice(onethird,twothirds).join(' ');
		var third = names.slice(twothirds,names.length).join(' ');
		return [first,second,third];	
	}
}


//// range helpers ////

function range_init(){
	return { min : Number.MAX_SAFE_INTEGER, max : Number.MIN_SAFE_INTEGER, sum: 0, cnt: 0 };
}

function range_update(range,value){
	if (range.min > value)
		range.min = value;
	if (range.max < value)
		range.max = value;
	range.sum += value;
	range.cnt += 1;
}

function range_center(range){
	if (range.cnt == 0)
		return;
	var avg = range.sum / range.cnt; 
	var d1 = avg - range.min;
	var d2 = range.max - avg;
	if (d1 < d2)
		range.min = avg - d2;
	else
	if (d2 < d1)
		range.max = avg + d1;
}

function range_scale(scale,range,value){
	return Math.ceil(range.max <= range.min ? scale / 2 : scale*((value-range.min)/(range.max-range.min)))
}

function range_scale_inv(scale,range,value){
	return Math.ceil(range.max <= range.min ? scale / 2 : scale*(1.0-(value-range.min)/(range.max-range.min)))
}


//// map to maps structure helpers ////

function mapmap_init(){
	return {}
}

function mapmap_put(mapmap,from,to,value){
	var map = mapmap[from];
	if (!map) {
		map = {};
		mapmap[from] = map;
	}
	map[to] = value;
}

//return the entry in mapped map, if none and if defval is supplied, fill and return the defval
function mapmap_get(mapmap,from,to,defval){
	var map = mapmap[from];
	if (!map) {
		if (defval){
			map = {};
			mapmap[from] = map;
		} else 
			return null;
	}
	var val = map[to];
	if (!val) {
		if (defval){
			val = defval;
			map[to] = val;
		}
	}
	return val;
}

////counter structure helpers ////

function counter_init(){
	return {};
}

function counter_put(graph,object,defval){
	if (!graph[object])
		graph[object] = defval;
}

function counter_add(graph,object,defval){
	if (!graph[object])
		graph[object] = defval;
	else
		graph[object] += defval;
}

function counter_stddev(graph_a,graph_b){
	var sum2 = 0;
	var n = 0;
	var disp = 0;
	for (var p in graph_a){
		if (graph_a.hasOwnProperty(p)) {
			var a = graph_a[p];
			var b = graph_b.hasOwnProperty(p) ? graph_b[p] : 0;
			sum2 += a;
			sum2 += b;
			n++;
			var ab = a-b;
			disp += ab*ab;
		}
	}
	var stddev = Math.sqrt(disp/n);
	var avg = sum2 / (2 * n);
	return stddev / avg;
}

function counter_extend(extendee,extender){
	for (var p in extender)
		if (extender.hasOwnProperty(p))
			if (!extendee.hasOwnProperty(p))
				return extendee[p] = extender[p];
}

function counter_normalize(graph,log){
	var max = 0;
	for (var p in graph){
		if (graph.hasOwnProperty(p)) {
			var v = graph[p];
			if (log)
				v = graph[p] = Math.log10(1+v);
			if (v && max < v)
				max = v;
		}
	}
	for (var p in graph){
		if (graph.hasOwnProperty(p)) {
			var v = graph[p] * 100 / max;
			if (v == 0)
				v = 1;
			graph[p] = v;
		}
	}
}

//// triple counter ////

function counter3_init(){
	return {};
}

function counter3_add(counter,first,second,third,val){
	var map = mapmap_get(counter,first,second,{});//supply empty map by default
	counter_add(map,third,val);
}

function counter3_list(map1,last2swapped){
	var list = [];
	for (var key1 in map1) if (map1.hasOwnProperty(key1)) {
		var map2 = map1[key1];
		for (var key2 in map2) if (map2.hasOwnProperty(key2)) {
			var map3 = map2[key2];
			for (var key3 in map3) if (map3.hasOwnProperty(key3))
				list.push(last2swapped ? [key1,key2,map3[key3],key3] : [key1,key2,key3,map3[key3]]);
		}
	}
	return list;
}


//// String extension ////

String.prototype.hashCode = function() {
	  var hash = 0, i, chr;
	  if (this.length === 0) return hash;
	  for (i = 0; i < this.length; i++) {
	    chr   = this.charCodeAt(i);
	    hash  = ((hash << 5) - hash) + chr;
	    hash |= 0; // Convert to 32bit integer
	  }
	  return hash;
};
