/*
Copyright 2018-2020 Anton Kolonin, Aigents®

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

//Aigents Graphs UI
//depends on
// /ui/aigents-al.js
// /ui/aigents-graph.js
// /ui/jquery-1.11.1.js


//TODO: parse hierarchy of thing's patterns and related local ontology
function parsePatternToLinks(pattern,links,nodes) {
	var p = new Parser(pattern);
	var prev = null;
	while (!p.end()) {
    	var s = p.parse();
    	if (AL.empty(s))
    		break;
    	if (prev != null)
			links.push([prev,s,1.0,'next']);
		mapmap_put(nodes,s,'is','element');//TODO: better term!?
    	prev = s;
	}
}

//TODO: 111
function graph_thing_patterns(thing_name){
	if (AL.empty(thing_name))
		return;
	var links = [];
	var nodes = {};
	parsePatternToLinks(thing_name,links,nodes);
	//1. Parse thing and its patterns
	if (AL.empty(links))
		return;
	var orders = GraphOrder.directed(links);//directed and iterative
	//2. Render graph of pattern and its local ontology tree
    var setup = {
    		graph : {orders:orders,links:links,nodes:nodes,config:{
				colors:{position:"#000080",next:"#000080"}
    			//,slicing:['site','news','time']
    			}
    		},
    };
    GraphUI.request_graph_popup(_("Aigents Topic Patterns Graph"), "svg_popup", setup);
}

function parseInstancesToLinks(things,links,nodes) {
	var p = new Parser(things);
	if (!p.end()) //skip 'There '
		p.parse();
	while (!p.end()) {
    	var o = p.parseObj();
    	if (AL.empty(o))
    		break;
    	if (AL.empty(o.text))
    		continue;
		mapmap_put(nodes,o.text,'is','instance');
    	for(var key in o){
    		var v;
    		if (key == 'text' || key == 'is' || key == 'title')
    			continue;
    		v = o[key];
    		if (AL.empty(v))
    			continue;
        	if (key == 'image' && !AL.empty(v))
        		mapmap_put(nodes,o.text,'image',v);
        	else
        	if (key == 'sources'){
    			links.push([o.text,v,1.0,'source']);
        		mapmap_put(nodes,v,'is','site');
        	}else 
            if (key == 'times'){
        		links.push([o.text,v,1.0,'time']);
            	mapmap_put(nodes,v,'is','time');
            }else 
            if (key == 'sentiment'){
            	if (v < -10 || +10 < v){
            		var n = v < -50 ? '😞' :  '😊';
            		links.push([o.text,n,v/100,'sentiment']);
            		mapmap_put(nodes,n,'is','emotion');
            	}
            }else 
        	if ((v = o[key]) != null){
        		links.push([o.text,v,1.0,key]);
        		mapmap_put(nodes,v,'is','value');
        	}
    	}
	}
}

function graph_thing_instances(thing_name){
	//TODO:
	//1. Get instances for thing
	//2. Render graph
	if (loading)
		loading(true,4);
	ajax_request("what is '"+thing_name+"', new true?",function(text){
		if (loading)
			loading(false,4);
		if (AL.empty(text))
			return;
		var links = [];
		var nodes = {};
		parseInstancesToLinks(text,links,nodes);
		//1. Parse thing and its patterns
		if (AL.empty(links))
			return;
		var orders = GraphOrder.directed(links);//directed and iterative
		//2. Render graph of pattern and its local ontology tree
	    var setup = {
	    		graph : {orders:orders,links:links,nodes:nodes,config:{
					colors:{position:"#000080",next:"#000080"}
	    			,slicing:['site','instance','value','time']
					,labeled_links:true
	    			}
	    		},
	    };
	    GraphUI.request_graph_popup(_("Aigents Topic Instances Graph"), "svg_popup", setup);
	},false,function(){if (loading) loading(false,4);//handle error!?
	});
}


function graph_peers(peers_data,peer_obj){
	if (!AL.empty(peers_data)){
		var links = [];
		var orders = {};
		var nodes = {};
		for (var i = 0; i < peers_data.length; i++){
			var peer = peer_obj(peers_data,i);
			var peer_name = peer_screen_name(peer);
			var relevance = parseInt(peer.relevance);
			counter_add(orders,peer_name,relevance);
			mapmap_put(nodes,peer_name,'is','friend');
			if (peer.share){
				counter_add(orders,'Share',relevance);
				links.push(['Share',peer_name,relevance,'relationships']);
			}
			if (peer.trust){
				counter_add(orders,'Trust',relevance);
				links.push([peer_name,'Trust',relevance,'relationships']);
			}
			if (!AL.empty(peer.email)){
				counter_add(orders,'Aigents',relevance);
				links.push([peer_name,'Aigents',relevance,'networks']);
			}
			if (peer.facebook){
				counter_add(orders,'Facebook',relevance);
				links.push([peer_name,'Facebook',relevance,'networks']);
			}
			if (peer.google){
				counter_add(orders,'Google',relevance);
				links.push([peer_name,'Google',relevance,'networks']);
			}
			if (peer.vkontakte){
				counter_add(orders,'VKontakte',relevance);
				links.push([peer_name,'VKontakte',relevance,'networks']);
			}
			if (peer.reddit){
				counter_add(orders,'Reddit',relevance);
				links.push([peer_name,'Reddit',relevance,'networks']);
			}
		}
		mapmap_put(nodes,'Trust','is','relationship');
		mapmap_put(nodes,'Share','is','relationship');
		mapmap_put(nodes,'Aigents','is','network');
		mapmap_put(nodes,'Facebook','is','network');
		mapmap_put(nodes,'Google','is','network');
		mapmap_put(nodes,'VKontakte','is','network');
		mapmap_put(nodes,'Reddit','is','network');
		mapmap_put(nodes,'Aigents','image','https://aigents.com/ui/img/aigent32.png');
		mapmap_put(nodes,'Facebook','image','https://aigents.com/ui/img/fb_logo.png');
		mapmap_put(nodes,'Google','image','https://aigents.com/ui/img/google_icon.png');
		mapmap_put(nodes,'VKontakte','image','https://aigents.com/ui/img/vk_logo.png');
		mapmap_put(nodes,'Reddit','image','https://aigents.com/ui/img/reddit.png');
		//TODO: setup images
    	var setup = {
    			graph : {orders:orders,links:links,nodes:nodes,config:{
					colors:{links:"#000080",relationships:"#008000",network:"#008080",relationship:'#ffcc00',friend:'#FFFF00'},
    				slicing:['network','friend','relationship']}
    			},
    	};
    	GraphUI.request_graph_popup(_("Aigents Friends Graph"), "svg_popup", setup);
	}
}

function graph_news(news_data){
	if (!AL.empty(news_data)){
		//0 100
		//1 60
		//2 "https://www.nytimes.com/2018/06/09/upshot/how-good-is-the-trump-economy-really.html"
		//3 "how good is the trump economy"
		//4 "today"
		//5 false
		//6 "https://static01.nyt.com/images/2018/06/08/insider/08insider-meier-image2/00insider-meier-image2-thumbStandard.jpg"
		//7 is
		var links = [];
		var orders = {};
		var nodes = {};
		for (var i = 0; i < news_data.length; i++){
			var item = news_data[i];
			var source = item[2];
			var site = source ? (AL.isURL(source) ? new URL(source).hostname : source) : 'unknown';
			var image = item[6];
			var name = item[3];
			var time = item[4];
			var relevance = parseInt(item[0]) + parseInt(item[1]); //sum of relevances;
			counter_add(orders,site,relevance);
			counter_add(orders,name,relevance);
			counter_add(orders,time,relevance);
			links.push([name,time,relevance,'times']);
			links.push([name,site,relevance,'sites']);
			mapmap_put(nodes,name,'image',image);
			mapmap_put(nodes,site,'is','site');
			mapmap_put(nodes,time,'is','time');
			mapmap_put(nodes,name,'is','news');
		}
    	var setup = {
    			//graph : {orders:{a:1,b:3,c:2},links:[["a","b",1,"x"],["a","c",2,"x"],["b","c",2,"y"]]},
    			graph : {orders:orders,links:links,nodes:nodes,config:{
					colors:{times:"#000080",sites:"#008000",words:"#008080",topics:"#808000",site:"#008080",time:'#ffcc00',news:'#FFFF00',word:'#00ccff'},
    				slicing:['site','news','time']}
    			},
    	};
    	GraphUI.request_graph_popup(_("Aigents News Graph"), "svg_popup", setup);
	}
}
	
function graph_topics(news_data){
	if (!AL.empty(news_data)){
		var counter = counter3_init();
		var orders = {};
		var nodes = {};
		for (var i = 0; i < news_data.length; i++){
			var item = news_data[i];
			var source = item[2];
			var site = source ? (AL.isURL(source) ? new URL(source).hostname : source) : 'unknown';
			var name = item[3];
			var time = item[4];
			var topic = AL.empty(item[7]) ? 'unknown' : item[7];
			var relevance = parseInt(item[0]) + parseInt(item[1]); //sum of relevances;
			counter_add(orders,topic,relevance);
			counter_add(orders,site,relevance);
			counter_add(orders,time,relevance);
			counter3_add(counter,topic,time,'times',relevance);
			counter3_add(counter,topic,site,'sites',relevance);
			mapmap_put(nodes,topic,'is','topic');
			mapmap_put(nodes,site,'is','site');
			mapmap_put(nodes,time,'is','time');
		}
		var links = counter3_list(counter,true);
    	var setup = {
    			graph : {orders:orders,links:links,nodes:nodes,config:{
					colors:{times:"#000080",sites:"#008000",words:"#008080",topics:"#808000",site:"#008080",time:'#ffcc00',news:'#FFFF00',word:'#00ccff'},
    				slicing:['site','topic','time']}
    			},
    	};
    	GraphUI.request_graph_popup(_("Aigents Topics Graph"), "svg_popup", setup);
	}
}

//TODO: make usable with word normalization on server side
function graph_words(news_data){
	if (!AL.empty(news_data)){
		var word_counts = counter_init();
		var word_orders = counter_init();
		var counter = counter3_init();
		var orders = {};
		var nodes = {};
		for (var i = 0; i < news_data.length; i++){
			var item = news_data[i];
			var source = item[2];
			var site = source ? (AL.isURL(source) ? new URL(source).hostname : source) : 'unknown';
			var name = item[3];
			var time = item[4];
			var topic = item[7];
			var relevance = parseInt(item[0]) + parseInt(item[1]); //sum of relevances;
			counter_add(orders,site,relevance);
			counter_add(orders,time,relevance);
			mapmap_put(nodes,site,'is','site');
			mapmap_put(nodes,time,'is','time');
			var words = name.split(' ');
			for (var w = 0; w < words.length; w++){
				var word = words[w];
				counter_add(word_counts,word,1);
				counter_add(word_orders,word,relevance);
				mapmap_put(nodes,word,'is','word');
				counter3_add(counter,word,time,'times',relevance);
				counter3_add(counter,word,site,'sites',relevance);
			}
		}
		//TODO: denominate word counts
		for (var word in word_counts)
			word_orders[word] = word_orders[word] / word_counts[word];
		//TODO: eliminate bottom X percents
		var max = 0;
		for (var word in word_orders){
			var val = word_orders[word];
			if (max < val)
				max = val;
		}
		for (var word in word_orders){
			var val = word_orders[word] / max;
			if (val > 0.9)//normalized
			//if (val > 0.1)//denormalized
				counter_add(orders,word,val * max);
		}
		
		var links = counter3_list(counter,true);
    	var setup = {
    			graph : {orders:orders,links:links,nodes:nodes,config:{
					colors:{times:"#000080",sites:"#008000",words:"#008080",topics:"#808000",site:"#008080",time:'#ffcc00',news:'#FFFF00',word:'#00ccff'},
    				slicing:['site','word','time']}
    			},
    	};
    	GraphUI.request_graph_popup(_("Aigents Words Graph"), "svg_popup", setup);
	}
}


//TODO: cleanup what should it be
//TODO: AL-style list element separators , and ; ?
// a b x 1.0. a b y 0.5.
// =>
// a b x 1.0
// a b y 0.5
function parseALToGraph(input,reverses) {
	function addLink(source,relationship,target,value){
		var reverse = reverses ? reverses[relationship] : null;
		if (reverse)
			links.push([target,source,value,reverse]);
		else
			links.push([source,target,value,relationship]);
	}
	var links = [];
	var p = new Parser(input);
	while (!p.end()) {
    	//while (!p.end()) { // glueing multi-word names from multiple tokens
    		var s = p.parse();
    		if (AL.empty(s) || AL.punctuation.indexOf(s) != -1)
    			break;
    		var r = p.parse();
    		if (AL.empty(r) || AL.punctuation.indexOf(r) != -1)
    			break;
    		var t = p.parse();
    		if (AL.empty(t) || AL.punctuation.indexOf(t) != -1)
    			break;
    		var v = p.parse();//may be either period or value
    		if (AL.empty(v) )
    			break;
    		//TODO: line breaks!?
    		if (v == '.')
    			//links.push([s,t,1.0,r]);
    			addLink(s,r,t,1.0);
    		else{
    			var percent = false;
    			if (v.substr(v.length - 1) == '%'){
    				percent = true;
    				v = v.substr(0,v.length - 1);
    			}
    			v = Number(v);
    			if (isNaN(v))
        			break;
    			if (percent)
    				v /= 100;
        		var period = p.parse();//may be either period or value
        		if (period != '.')
        			break;
    			//links.push([s,t,v,r]);
    			addLink(s,r,t,v);
    		}
    	//}
	}
	return links;		
}

//TODO: threshold, similarity, connectivity
//TODO: network, id, period - fill defaults from parent layer graph
var graph_properties = ['network','id','period','range','limit','links'];
var graph_parameters = ['ethereum','' ,   7,       1,    1000,  'all'];
function graph_init(network,id,period,range) {
	graph_parameters[0] = network;
	graph_parameters[1] = id;
	graph_parameters[2] = period;
	if (range)
		graph_parameters[3] = range;
		
}
var popup_graph_setup = {
		slicing : 0,
		node_radius : 30,
		layout_threshold : 1,
		layout_directions : 3,
		layout_balance : 70,
		filter_range : 2
};

function graph_setup(id) {
	var new_parameters = graph_parameters;
	if (!AL.empty(id))
		new_parameters[1] = id;
	function onOk(){
		graph_parameters = dialog_retrieve(graph_properties);
		graph_launch();
		return true;
	}
	var network = new_parameters[0];
	var period = network == 'discourse' || network == 'telegram' ? [1,2,3,4,5,6,7,10,30,92,365,730] : [1,2,3,4,5,6,7]
	dialog_open('Render Graph','Specify graph parameters to render',
			graph_properties,
			graph_properties,//TODO: prompts
			new_parameters,false,onOk,null,null,{
			network:{title:'Network to explore',options:['www'].concat(get_graphable_networks())},//import from aigents-map.js
			links:{title:'Links to include in the graph',options:['all','pays','paid','votes','voted','comments','commented','mentions','mentioned']},
			period:{title:'Period to search',options:period},
			range:{title:'Range of link hops',options:[1,2,3,4,5,6,7,10]},
			}
	);
}

function graph_launch(id) {
	var dialog = $( '#graph_dialog' );
	if (dialog.hasClass('ui-dialog-content')){
		//if opened, save parameters for re-open
		popup_graph_setup = GraphUI.graph_popup_setup();
		//TODO: make disabled and reload instead of closing  
		//dialog.dialog("close");
	}
	//TODO: generate properly
	//var cmd = 'Ethereum id '+id+' graph period 1, range 2, limit 100';
	var id = !AL.empty(id) ? id : graph_parameters[1];
	var cmd = graph_parameters[0] + ' id \'' + id +
		'\' graph, period ' + graph_parameters[2] + ', range ' + graph_parameters[3] +
		', limit ' + graph_parameters[4];
	if (graph_parameters[5] != 'all')
		cmd += ', links '+ graph_parameters[5];
	if (loading)
		loading(true,4);
	console.log(cmd);
	ajax_request(cmd,function(text){
		graph_text(text,graph_parameters[0],id);//network,id
		if (loading)
			loading(false,4);
	},false,//dump to console - TODO: not working!?
		function(){if (loading) loading(false,4);//handle error!?
	});
}

var popup_graph_menu_obj = null;
function popup_graph_menu(event) {
	event.preventDefault();
	if (popup_graph_menu_obj)
		menu_hide(popup_graph_menu_obj);
	popup_graph_menu_obj = $( "#graph_menu" );
	popup_graph_menu_obj.mouseleave(function (){menu_hide(popup_graph_menu_obj);});
	var element = event.target;
	function name(element){
		if (element.nodeName == 'svg')//root svg
			return null;
		if (element.nodeName == 'g')//node group
			return element.id;
		return name(element.parentNode);
	}
	var id = name(element);
	if (!AL.empty(id)){
		//$("#graph_menu_connections").html(_('Graph connections')); 
		$("#graph_menu_connections").off().click(function(event){
	    	event.stopPropagation();
	    	menu_hide(popup_graph_menu_obj);
	    	graph_launch(id);
		});
		$("#graph_menu_connections").show();
	} else {
		$("#graph_menu_connections").hide();
	}
	//$("#graph_menu_setup").html(_('Graph setup')); 
	$("#graph_menu_setup").off().click(function(event){
    	event.stopPropagation();
    	menu_hide(popup_graph_menu_obj);
    	//TODO: setup dialog, store in local properties
    	graph_setup(id);
	});
	if (AL.isURL(id)){
		//$("#graph_menu_open_url").html(_('Open URL')); 
		$("#graph_menu_open_url").off().click(function(event){
	    	event.stopPropagation();
	    	menu_hide(popup_graph_menu_obj);
			window.open(id,'_blank');
		});
		$("#graph_menu_open_url").show();
	} else {
		$("#graph_menu_open_url").hide();
	}
	//TODO: localize
	menu_show(popup_graph_menu_obj,event);
}

function graph_text(text,network,id){
	if (!AL.empty(text)){
		var image = network && logos ? logos[network] : null;
		var links = parseALToGraph(text,{paid:'pays',called:'calls',voted:'votes',commented:'comments',linked:'links',mentioned:'mentions'});
		var orders = GraphOrder.directed(links);//directed and iterative
		var nodes = {};
    	var setup = {
    			graph : {
    				orders:orders,
    				links:links,
    				config:{
    					menu: popup_graph_menu,
    					colors:{pays:"#40bf40",votes:"#808080",calls:"#ff471a",comments:'#0066ff',mentions:'#009999'},
    					image: image
    					//labeled_links:true
    				}
    			},
				slicing : popup_graph_setup.slicing,
				node_radius : popup_graph_setup.node_radius,
				layout_threshold : popup_graph_setup.layout_threshold,
				layout_directions : popup_graph_setup.layout_directions,
				layout_balance : popup_graph_setup.layout_balance,
				filter_range : popup_graph_setup.filter_range
				//TODO: inherit filter
    	};
    	var subtitle = !AL.empty(id) ? " (" + id + ")" : ""
    	GraphUI.request_graph_popup(_("Aigents Graph")+subtitle, "svg_popup", setup);
	}
}


