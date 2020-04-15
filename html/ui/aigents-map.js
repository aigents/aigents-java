/*
Copyright 2018-2020 Anton Kolonin, Aigents®

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

//depends on 
// /ui/aigents-al.js
// /ui/aigents-gui.js
// /ui/jquery-1.11.1.js

//TODO: port to re-use /ui/aigents-graph.js

var logos = {
	google:"/ui/img/google_icon.png",
	facebook:"/ui/img/fb_logo.png",
	vkontakte:"/ui/img/vk_logo.png",
	reddit:"/ui/img/reddit.png",
	discourse:"/ui/img/singularitynet.png",
	telegram:"/ui/img/telegram_square.png",
	steemit:"/ui/img/steemit_logo_new.png",
	golos:"/ui/img/golos_logo.png",
	ethereum:"/ui/img/eth32.png"
};

//TODO: network aigents if is_root 
var networks = ['google','facebook','vkontakte','reddit','discourse','telegram','ethereum','steemit','golos'];
//function get_graphable_networks() { return is_root ? ['discourse','telegram','ethereum','steemit','golos'] : ['discourse','ethereum','steemit','golos'] };//import is_root from aigents-map.js
function get_graphable_networks() { return ['discourse','telegram','ethereum','steemit','golos'] };

var graph_hashing_names = false;
var graph_user_name = 'I';
var graph_network = null;
var graph_id = null;
var graph_o = null;//current focus
var graph_logo = null;
var graph_reloader = null;
var graph_key = null;
var graph_similarity = 0;//0/25/75%
var graph_connectivity = 0;//0/25/75%
var graph_period = 1;//0/1/2/3/4/5:day/week/month/quarter/year/5 years 
var period_texts = { 0: "day", 1: "week", 2: "month", 3: "quarter", 4: "year", 5: "all" };
var period_days = { 0: 1, 1: 7, 2: 31, 3: 92, 4: 365, 5: 10000 };

var graph_data = {};


//TODO: clean on changing graph and period
var breadcrumbs = [];
function breadcrumbs_clear(){
	breadcrumbs = [];
	$('#map_back').css('visibility', 'hidden');
}
function breadcrumbs_push(network,id){
	breadcrumbs.push({network:network,id:id});
	$('#map_back').css('visibility', 'visible').css('cursor','pointer');
}
function breadcrumbs_pop(){
	var last = breadcrumbs.pop();
	if (breadcrumbs.length == 0){
		$('#map_back').css('visibility', 'hidden');
	}
	return last;
}
$(function() {
	$('#map_back').click(function() {
		map_loading(true);
		var last = breadcrumbs_pop();
		setTimeout(function() {
			reload(graph_network = last.network, graph_id = last.id, function(){map_loading(false);});
		},1);
	});	
});


function name_or_email(names){
	var name = '';
	if (names.name && names.name.length > 0){
		name = names.name;
		if (names.surname && names.surname.length > 0)
			name += ' ' + names.surname;
	} else
	if (names.email && names.email > 0){
		name = names.email; 
	}
	return name;
}

//TODO: use unified method
function ajax_request_uri_method_map(uri,callback,silent,method,data) {
	$.ajax({
		type: method,
		data: data,
		cache: false, 
		crossDomain : true, 
		url: uri,
		dataType : 'text',
		xhrFields: { withCredentials: true },
		success: function(data, textStatus, jqXHR) { 
			if (callback)
				callback(data);
		},
		error: function(jqXHR, textStatus, errorThrown) { 
			console.log(textStatus+": "+(errorThrown ? errorThrown : "possibly no connection")+"."); 
		},
		beforeSend: function(xhr){ 
			xhr.withCredentials = true; 
		}
	});
}

function get_data_cached(network,id,period){
	var key = network + '_' + id + '_' + period;
	return graph_data[key];
} 

function put_data_cached(network,id,period,data){
	var key = network + '_' + id + '_' + period;
	graph_data[key] = data;
} 

function initSvg(svg){
	svg.padding = 30;
	svg.radius = 30;
//	svg.full_width = svg.getAttribute('width');
//	svg.full_height = svg.getAttribute('height');
	svg.full_width = $( "#map" ).outerWidth();
	svg.full_height = $( "#map" ).outerHeight();
	//if (graph_network == 'ethereum' && popup_graph_menu){
	if (popup_graph_menu && get_graphable_networks().includes(graph_network)){
		if (graph_init)
			graph_init(graph_network,graph_id,period_days[graph_period],1);
		$(svg).contextmenu(popup_graph_menu);
		$(svg).on("taphold",popup_graph_menu);
	}
}

function getSvgPoint(svg, xvalue, xbase, yvalue, ymin, ymax, dontmove){
	if (xbase < 1)//hack to avoid divide overflow!
		xbase = 1;
	var ybase = ymax - ymin;
	var width = svg.full_width - svg.padding * 2;
	var height = svg.full_height - svg.padding * 2;
	var r = svg.radius * 2;
	var width2 = width / 2 - r;
	var xbase2 = xbase / 2;
	x = dontmove ? xvalue * width / xbase 
			: xvalue < xbase2 ? xvalue * width2 / xbase2 
			: width2 + r + r + (xvalue - xbase2) * width2 / xbase2;
	var y = (yvalue - ymin) * height / ybase;
	return {x:x,y:y};
}

function addSvgText(svg, name, xvalue, xbase, yvalue, ybase, text, font){
	var point = getSvgPoint(svg, xvalue, xbase, yvalue, 0, ybase);
	var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
	text.setAttribute("x",point.x);
	text.setAttribute("y",point.y);
	text.setAttribute("class","map-text-middle");
	if (font)
		text.setAttribute("font-size",font);
	var textNode = document.createTextNode(name);
	text.appendChild(textNode);
	svg.appendChild(text);
}

function addSvgTextToGroup(group, x, y, name){
	var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
	text.setAttribute("x",x);
	text.setAttribute("y",y);
	text.setAttribute("class","map-text-middle");
	if (name.length > 8)
		text.setAttribute("font-size",18 / (name.length / 8));
	var textNode = document.createTextNode(name.match(/\d+/g) ? name : capitalize(name));
	text.appendChild(textNode);
	group.insertBefore(text, null);
}

function addSvgTextToGroupSplit(group, radius, name){
	var space;
	if (graph_hashing_names)
		name = ('' + name.hashCode()).replace('-','');
	var len = name.length; 
	if (len > 8 && (space = name.indexOf(' ')) != -1 && space > 1){
		addSvgTextToGroup(group, radius, radius - 7, name.substring(0,space));
		addSvgTextToGroup(group, radius, radius + 7, name.substring(space).trim());
	} else
	if (len > 32){
		len /= 2;
		addSvgTextToGroup(group, radius, radius - 7, name.substring(0,len));
		addSvgTextToGroup(group, radius, radius + 7, name.substring(len));
	} else 
		addSvgTextToGroup(group, radius, radius, name);
}

function addSvgGroup(svg, name, xvalue, xbase, yvalue, ymin, ymax, halo_width, dontmove, imageUrl, reloader){
	var point = getSvgPoint(svg, xvalue, xbase, yvalue, ymin, ymax, dontmove);
	var saturation = Math.floor((255*yvalue)/ymax);
	return addSvgGroupPoint(svg, name, point, halo_width, saturation, 1, imageUrl, reloader);
}
	
function addSvgGroupPoint(svg, name, point, halo_width, saturation, border, imageUrl, reloader){
	var padding = 30;
	var radius = 30
	var group = document.createElementNS("http://www.w3.org/2000/svg", "g");
	group.setAttribute("transform","translate("+point.x+","+point.y+")");
	
	if (reloader){
		group.setAttribute("cursor","pointer");
		group.setAttribute("name",name);
		$(group).click(function(){
			map_loading(true);
			console.log('opening '+name+'@'+reloader);
			breadcrumbs_push(graph_network,graph_id);
			reload(reloader, graph_id = name, function(){map_loading(false);})
		});
	}

	var halo = document.createElementNS("http://www.w3.org/2000/svg", "circle");
	halo.setAttribute("cx",radius);
	halo.setAttribute("cy",radius);
	halo.setAttribute("r",radius + halo_width);// /10?
	halo.setAttribute("fill","lightblue");
	halo.setAttribute("fill-opacity","0.75");
	group.insertBefore(halo, null);
		
	var circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
	circle.setAttribute("cx",radius);
	circle.setAttribute("cy",radius);
	circle.setAttribute("r",radius);
	circle.setAttribute("stroke","rgb(153, 102, 51)");
	circle.setAttribute("stroke-width", border && border > 0 ? border <=5 ? border : 5 : 1);
	circle.setAttribute("fill","rgb(255, 255, "+saturation+")");
	circle.setAttribute("fill-opacity","0.75");
	group.insertBefore(circle, null);

	if (imageUrl){
		var image = document.createElementNS("http://www.w3.org/2000/svg", "image");
		image.setAttribute("href",imageUrl);
		image.setAttribute("x",radius - 8);
		image.setAttribute("y",radius + 8);
		image.setAttribute("width",16);
		image.setAttribute("height",16);
		group.insertBefore(image, null);
	}
	
	addSvgTextToGroupSplit(group,radius,name)

	var title = document.createElementNS("http://www.w3.org/2000/svg", "title");
	title.innerHTML = name;
	group.appendChild(title);
	
	var svg = document.getElementById('map');
	svg.appendChild(group);
	
	//TODO: make menu initialized in initSvg and applied to entire graph
	//use popup_graph_menu from aigents-gui.js
	if (graph_network == 'ethereum' && popup_graph_menu){
		group.id = name;
		//$(group).contextmenu(popup_graph_menu);
		//$(group).on("taphold",popup_graph_menu);
	}
	
	return point;
} 

function addSvgArrow(svg, x1, y1, x2, y2, c, w){
	var poly = document.createElementNS("http://www.w3.org/2000/svg", "polygon");

	var width = w * 4;
    var angle = 0;
    var dx = x1-x2;
    var dy = y1-y2;
    if (dx == 0)
		angle = dy > 0 ? Math.PI/2 : -Math.PI/2;
    else {
        angle= Math.atan(dy/dx);
        if (dx < 0)
            angle += Math.PI;
    }
    var a = angle;
    
    //TODO: properly
    //actual end of line
    //var x2 = x2 + (svg.radius * Math.cos(a));
    //var y2 = y2 + (svg.radius * Math.sin(a));
    
    //var ad = Math.PI/8;
    var ad = Math.PI/6;
    var al = a + ad;
    var ar = a - ad;
    var xl = x2 + (width * Math.cos(al));
    var yl = y2 + (width * Math.sin(al));
    var xr = x2 + (width * Math.cos(ar));
    var yr = y2 + (width * Math.sin(ar));
    
    var points = ""+x2+","+y2+" "+xl+","+yl+" "+xr+","+yr;

    //actual end of the line
    var x2 = (xr + xl) / 2;
    var y2 = (yr + yl) / 2;

    //actual start of the line
    a1 = a - Math.PI;
    //var x1 = x1 + (svg.radius * Math.cos(a1));
    //var y1 = y1 + (svg.radius * Math.sin(a1));
    
	poly.setAttribute("points",points);
	poly.setAttribute("fill",c);
	svg.appendChild(poly);
	
	var line = document.createElementNS("http://www.w3.org/2000/svg", "line");
	line.setAttribute("x1",x1);
	line.setAttribute("y1",y1);
	line.setAttribute("x2",x2);
	line.setAttribute("y2",y2);
	line.setAttribute("style","stroke:"+c+";stroke-width:"+w);
	svg.appendChild(line);
}

function addSvgArrows(svg, x1, y1, x2, y2, l1, l2, w1, w2){
	if (x1 == x2 && y1 == y2)
		return;
	//TODO: test
    var angle = 0;
    var dx = x1-x2;
    var dy = y1-y2;
    if (dx == 0)
		angle = dy > 0 ? Math.PI/2 : -Math.PI/2;
    else {
        angle= Math.atan(dy/dx);
        if (dx < 0)
            angle += Math.PI;
    }
    x1 = x1 + (svg.radius * Math.cos(angle - Math.PI));
    y1 = y1 + (svg.radius * Math.sin(angle - Math.PI));
    x2 = x2 + (svg.radius * Math.cos(angle));
    y2 = y2 + (svg.radius * Math.sin(angle));
	
	var l = l1 + l2; 
	var x0 = x1 + (x2 - x1) * l1 / l;
	var y0 = y1 + (y2 - y1) * l1 / l;
	
	if (l1 == l2)
		c1 = c2 = "#0000c0";
	else
	if (l1 < l2){
		c1 = "#000080";
		c2 = "#0000ff";
	} else {
		c1 = "#0000ff";
		c2 = "#000080";
	}
	if (l1 > 0)
		addSvgArrow(svg, x1, y1, x0, y0, c1, w1);
	if (l2 > 0)
		addSvgArrow(svg, x2, y2, x0, y0, c2, w2);
}


function getTextNodeWidth(textNode) {
    var height = 0;
    if (document.createRange) {
        var range = document.createRange();
        range.selectNodeContents(textNode);
        if (range.getBoundingClientRect) {
            var rect = range.getBoundingClientRect();
            if (rect) {
                height = rect.right - rect.left;
            }
        }
    }
    return height;
}

function getSectionData(o,id) {
	var s = o.sections;
	if (s)
		for (var i = 0; i < s.length; i++)
			if (s[i].id == id)
				return s[i].data;
}

function getConnectionsMap(connections){
	if (connections){
		var map = {}
		for (var i = 0; i < connections.length; i++){
			map[connections[i][6]] = connections[i];
		}
		return map;
	}
}

function dataToSvg(o,logoUrl,reloading_network,filter) {
	if (o.peers && o.peers.length > 0 && o.peers[0]){
		var connectionsMap = null;
		var my_peer = null;
		var nodes = {};
		var sorted = [];
		var links = {};
		function addLink(id1,id2,out,inc){
			if (id1 > id2)//invert link order just for storage
				return addLink(id2,id1,inc,out);
			key = id1 + '|' + id2;  
			var link = links[key];
			if (!link)
				links[key] = {id1:id1,id2:id2,inc:inc,out:out}
			else {
				link.inc += inc;
				link.out += out;
			}
		}
		for (var pi = 0; pi < o.peers.length; pi++) {//iterate all sections for all heading peers
			var peer = o.peers[pi];
			if (pi == 0){//first position is taken by self peer
				my_peer = peer;
				connectionsMap = getConnectionsMap(getSectionData(peer,"all connections"));
				nodes[peer.id] = {name:peer.name, inc:0, out:0, id:peer.id};//don't add to sorted because it will be placed in the middle
			}
			var followers = getSectionData(peer,"fans");
			var leaders = getSectionData(peer,"authorities");
			var similars = getSectionData(peer,"similar to me");
			//rank|name|my likes|likes|comments|id
			if (followers) for (var fi = 0; fi < followers.length; fi++){
				var entry = followers[fi];
				var name = entry[1];
				var out = entry[2];
				var inc = entry[3]+entry[4];
				var id = entry[5];
				var node = nodes[id];
				if (!node){	
					nodes[id] = node = {name:name, inc:0, out:0, id:id};
					sorted.push(node);
				}
				addLink(peer.id,id,out,inc);
			}
			if (leaders) for (var li = 0; li < leaders.length; li++){//not needed is threshold == 0 !?
				var entry = leaders[li];
				var name = entry[1];
				var out = entry[2];
				var inc = entry[3]+entry[4];
				var id = entry[5];
				var node = nodes[id];
				if (!node){	
					nodes[id] = node = {name:name, inc:0, out:0, id:id};
					sorted.push(node);
				}
				addLink(peer.id,id,out,inc);
			}
			//rank|name|overlaps|my likes|likes|comments|words|id
			if (similars) for (var si = 0; si < similars.length; si++){
				var entry = similars[si];
				var id = entry[7];
				var node = nodes[id];
//TODO consider that similarity makes not much sense for R > 1 because is related to origin
				if (node)
					node.similarity = entry[0];
			}
		}
		//sort by names
		sorted.sort(function(a,b){
			var str1 = a.name;
			var str2 = b.name;
			return str1 < str2 ? -1 : str1 > str2;
		});
		//normalize connectivities
		var link_conn = range_init();
		var node_conn = range_init();
		var y = range_init();
		var pop = range_init();
		var act = range_init();
		for (var key in links) if (links.hasOwnProperty(key)) {//evaluate link connectivities and update node connectivities
		    var link = links[key];
			range_update(link_conn,link.inc);
			range_update(link_conn,link.out);
		    nodes[link.id1].out += link.out; 
		    nodes[link.id1].inc += link.inc; 
		    nodes[link.id2].inc += link.inc;
		    nodes[link.id2].out += link.out;
		}		
		for (var i = 0; i < sorted.length; i++){//update node rank, popularity, activity and connectivity 
			var node = sorted[i];
			var mapped = connectionsMap ? connectionsMap[node.id] : null;
			//scale 0-100 range to inverted range 200-0
			node.rank = 200 - mapped[0] * 2;
			range_update(y,node.rank);
			range_update(pop, node.pop = mapped[1]);
			range_update(act, node.act = mapped[2]);
			range_update(node_conn, node.inc + node.out );
		}
//TODO fix similarity making it related to my_peer !?
		for (var i = 0; i < sorted.length; i++){//normalize node connectivity
			var node = sorted[i];
			node.position = i;
			node.connectivity = (node.inc + node.out) * 100 / node_conn.max;
			if (!node.similarity)
				node.similarity = 0;
		}
		for (var key in links) if (links.hasOwnProperty(key)) {//normalize link connectivity
		    var link = links[key];
		    link.inc = link.inc == 0 ? 0 : 1 + link.inc * 10 / link_conn.max;
		    link.out = link.out == 0 ? 0 : 1 + link.out * 10 / link_conn.max;
		}		
		
		//TODO: use separate context object instead of hacking svg!!!
		var svg = document.getElementById('map');
		initSvg(svg);
		
		var mapped = connectionsMap ? connectionsMap[my_peer.id] : null;
		var my_rank = mapped? (100.0 - mapped[0])/100 : 0.5;//scale to inverted range 0.0-1.0
		var my_pop = mapped? mapped[1] : 50;
		var my_act = mapped? mapped[2] : 50;
		range_update(pop, my_pop);
		range_update(act, my_act);
		my_rank *= 200;
		range_update(y, my_rank);
		//placement hacks
		if (y.max <= y.min){//if nothing at all, tweak so self is in the middle
			y.max = y.min + 50;
			y.min = y.min - 50;
		}
		if (y.min > 90)//move middle away from the top border
			y.min = 90;

		var my_point = getSvgPoint(svg, 50, 100, my_rank, y.min, y.max, true);//put me in the middle //don't move
		my_point.name = /^\d+$/.test(my_peer.name) ? graph_user_name : my_peer.name;//if numeric id, use common name/email
		my_point.saturation = range_scale_inv(255,pop,my_pop);
		my_point.border = range_scale(5,act,my_act);
		my_point.similarity = 0;
		my_point.loader = null;
		
		function nodePoint(node){
			if (node.id == my_peer.id)
				return my_point;
			if (!AL.empty(filter) && node.name.toLowerCase().indexOf(filter) == -1)
				return null;
			if (node.similarity < graph_similarity || node.connectivity < graph_connectivity)
				return null;
			return getSvgPoint(svg, node.position, sorted.length - 1, node.rank, y.min, y.max);//except centered;
		}
		//render links
		for (var key in links) if (links.hasOwnProperty(key)) {//evaluate link connectivities and update node connectivities
		    var link = links[key];
		    var node1 = nodes[link.id1];
		    var node2 = nodes[link.id2];
		    var p1 = nodePoint(node1);
		    var p2 = nodePoint(node2);
		    if (p1 && p2)
				addSvgArrows(svg, svg.padding + p1.x, svg.padding + p1.y, svg.padding + p2.x, svg.padding + p2.y, link.out, link.inc, link.out, link.inc);
		}
		//render peer nodes except self
		for (var i = sorted.length - 1; i >= 0; i--){
			var node = sorted[i];
			var point = nodePoint(node);
			if (!point)
				continue;
			point.name = node.name;
			point.similarity = node.similarity;
			point.saturation = range_scale_inv(255,pop,node.pop);
			point.border = range_scale(5,act,node.act);
			point.loader = reloading_network;
			addSvgGroupPoint(svg, point.name, point, point.similarity, point.saturation, point.border, logoUrl, point.loader);
		}
		//render self peer node
		addSvgGroupPoint(svg, my_point.name, my_point, my_point.similarity, my_point.saturation, my_point.border, logoUrl, my_point.loader);//dontmove
	}
}

function disable_update(disabled){
	//TODO: fix it with disabling not hiding
	if (disabled){
		$( "#map-widgets" ).hide();
		$( "#map-networks" ).hide();
	}else{
		$( "#map-widgets" ).show();
		$( "#map-networks" ).show();
	}
	//$( "#map-widgets" ).prop( "disabled", disabled );
	//$( "#map-networks" ).prop( "disabled", disabled );
}

//TODO: to al.js
//TODO: 0.77 77% <0.77> => 0.77
function weightValue(val){
	if (val == undefined)
		return 1;
	if (val.charAt(0) == '<' && val.charAt(val.length - 1) == '>')
		val = val.substr(1).slice(0, -1);
	if (val.charAt(val.length - 1) == '%')
		val = parseFloat(val.slice(0, -1))/100;
	else
		val = parseFloat(val);
	if (isNaN(val))
		return 1;
	return val;
}

//111
function load_graph(social_net, social_id) {
	//TODO: update from cache
	/*var o = get_data_cached(social_net,social_id,graph_period);
	if (o){
		graph_logo = logos[social_net];
		graph_reloader = social_net == 'steemit' || social_net == 'golos' || social_net == 'ethereum' ? social_net : null;		
		var svg = document.getElementById('map');
		svg.parentNode.replaceChild(svg.cloneNode(false), svg);//clear
		dataToSvg(graph_o = o, graph_logo, graph_reloader, graph_key);//render
		if (when_done)
			when_done();
		return;
	}*/
	
	//TODO: fresh option?
	//var days = period_days[graph_period];
	//var r = social_net + ' id ' + social_id + ' report, period ' + days + ', format json, all connections, authorities, fans, similar to me, threshold 0';
	var r = social_net+' id '+social_id+' graph links calls, pays';
	function update(response) {
		//alert(response);
		//parse graph data: 123 pays <2> 456.
		var parser = new Parser(response);
		var links = [];
		while (!parser.end()) {
			var subj = parser.parse();
			var verb = parser.parse();
			var val = weightValue(parser.parse());
			var obj = parser.parse();
//TODO: assert period?
			parser.parse(); //period
			links.push([subj,obj,val,verb]);
		}
		var orders = GraphOrder.directed(links,true);
    	GraphUI.request_graph_popup(_("Aigents Leaders Graph"), "svg_popup", {
    		graph : {orders:orders,links:links},
			slicing:0,
			layout_threshold:1,
			layout_directions:3
    	});
		disable_update(false); 
	}
	disable_update(true); 
	ajax_request_uri_method_map(base_url,update,true,'POST',r);
}


function map_menu(event) {
	event.preventDefault();
	if (menu)
		hide_menu();
	menu = $( "#graph_menu" );
	menu.mouseleave(function (){ hide_menu();});
	/*
	function refresh_selection() {
		selected_peers = [];
		$( ".ui-selected", $(menu_list) ).each(function() {if ($(this)[0].id)selected_peers.push( $(this) );});
	}
	if (!$(this).hasClass("ui-selected"))//make others unselected
		$(".ui-selected").removeClass("ui-selected");
	$(this).addClass("ui-selected");//make this selected
	var element = $(this);
	*/
	///...
	$("#graph_menu_leaders").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	load_graph(graph_network,graph_id);
	});
	show_menu(event);
}


function reload(social_net, social_id, when_done) {
	disable_update(true); 
	function done(){
		disable_update(false); 
		/*TODO
		if (social_net == "ethereum" && graph_network && graph_id){
			$('#map').contextmenu(map_menu);
			$('#map').on("taphold",map_menu);
		}else{// clean menu otherwise
			$('#map').off("taphold",map_menu);
			$('#map').off("contextmenu");
		}
		*/
		if (when_done)
			when_done();
	}
	
	//update from cache
	var o = get_data_cached(social_net,social_id,graph_period);
	if (o){
		graph_logo = logos[social_net];
		graph_reloader = social_net == 'steemit' || social_net == 'golos' || social_net == 'ethereum' ? social_net : null;		
		var svg = document.getElementById('map');
		svg.parentNode.replaceChild(svg.cloneNode(false), svg);//clear
		dataToSvg(graph_o = o, graph_logo, graph_reloader, graph_key);//render
		done();
		return;
	}
	
	var days = period_days[graph_period];
	var r = social_net + ' id ' + social_id + ' report, period ' + days + ', format json, all connections, authorities, fans, similar to me, threshold 0';
	function update(response) {
		if (response && response.charAt(0) == '{'){
			//TODO: possibly reload 'enabling users' in facebook/google/vkontakte, if allowed
			graph_logo = logos[social_net];
			graph_reloader = social_net == 'steemit' || social_net == 'golos' || social_net == 'ethereum' ? social_net : null;
			graph_o = JSON.parse(response);
			console.log(graph_o);
			put_data_cached(social_net,social_id,graph_period,graph_o);
			var svg = document.getElementById('map');
			svg.parentNode.replaceChild(svg.cloneNode(false), svg);//clear
			dataToSvg(graph_o, graph_logo, graph_reloader, graph_key);//render
			svg.style['display']='inline';
			done();
		} else
			setTimeout(function(){ajax_request_uri_method_map(base_url,update,true,'POST',r);},5000);
	}
	//ajax_request_uri_method_map(base_url,update,true,'POST',r+', fresh');
	ajax_request_uri_method_map(base_url,update,true,'POST',r);
}

function map_filter(key){
	graph_key = key == null ? null : key.toLowerCase();
	//TODO: instead of re-rendering, just fileter in-place?
	//TODO: or, just avoid re-parsing
	if (graph_o){
		var svg = document.getElementById('map');
		svg.parentNode.replaceChild(svg.cloneNode(false), svg);
		dataToSvg(graph_o, graph_logo, graph_reloader, graph_key);
	}
}

function graph_demo(self_name) {
	if (!self_name) {
		self_name = _('I');
		for (var i = 0; i < networks.length; i++)
			map_enable($('#map_'+networks[i]),false);
	}
	
	var svg = document.getElementById('map');
	svg.parentNode.replaceChild(svg.cloneNode(false), svg);
	svg = document.getElementById('map');
	
	initSvg(svg);
	
	addSvgText(svg, _("Log in and select any of social networks on your profile"), 70, 100, 19, 200, 12);
	addSvgText(svg, _("Arrow size indicates intensity of communication"), 80, 100, 133, 200, 12);
	addSvgText(svg, _("Halo size displays similarity of the person to self"), 28, 100, 85, 200, 12);
	addSvgText(svg, _("Vertical position corresponds to social status of the person"), 40, 100, 190, 200, 12);
	
	addSvgGroup(svg, self_name, 50, 100, 100, 0, 200, 0, true);
	
	var point;
	point = addSvgGroup(svg, _("Friend"), 20, 100, 100, 0, 200, 30);
	addSvgArrows(svg, svg.full_width/2, svg.full_height/2, svg.padding + point.x, svg.padding + point.y, 50, 50, 10, 10);

	point = addSvgGroup(svg, _("Colleague"), 80, 100, 100, 0, 200, 10);
	addSvgArrows(svg, svg.full_width/2, svg.full_height/2, svg.padding + point.x, svg.padding + point.y, 50, 50, 3, 3);
	
	point = addSvgGroup(svg, _("Leader"), 40, 100, 40, 0, 200, 10);
	addSvgArrows(svg, svg.full_width/2, svg.full_height/2, svg.padding + point.x, svg.padding + point.y, 70, 30, 7, 5);
	
	point = addSvgGroup(svg, _("Authority"), 60, 100, 20, 0, 200, 0);
	addSvgArrows(svg, svg.full_width/2, svg.full_height/2, svg.padding + point.x, svg.padding + point.y, 95, 5, 8, 2);
	
	point = addSvgGroup(svg, _("Follower"), 40, 100, 160, 0, 200, 10);
	addSvgArrows(svg, svg.full_width/2, svg.full_height/2, svg.padding + point.x, svg.padding + point.y, 30, 70, 5, 7);
	
	point = addSvgGroup(svg, _("Fan"), 60, 100, 180, 0, 200, 0);
	addSvgArrows(svg, svg.full_width/2, svg.full_height/2, svg.padding + point.x, svg.padding + point.y, 5, 95, 2, 8);
	
	map_loading(false);
}


function map_init_logged() {
	//loading user's social network
    var fields = [];
	for (var i = 0; i < networks.length; i++)
		fields.push(networks[i] + ' id');
	var r = 'what my ' + fields.join(', ')+'?';
	function update(response) {
		//console.log(response);
		var context = parseToList(response.substring(5), fields,",");//skip "Your "
		console.log(context);
		
		//TODO: intiialize only present ones!
		if (context && context.length > 0) {
			context = context[0];
			for (var i = 0; i < networks.length; i++){
				var network = networks[i];
				var id = context[network + ' id'];
				if (id){
					map_enable($('#map_'+network),id);
					$('#map_'+network).click(function() {
						breadcrumbs_clear();
						map_toggle(this);
						map_loading(true);
						graph_network = this.id.substring(4);
						graph_id = context[graph_network+' id'];
						reload(graph_network, graph_id, function(){map_loading(false);})
					});	
				} else
					map_enable($('#map_'+network),false);
			}
		}
	
		ajax_request_uri_method_map(base_url,function(response){
			var names = parseToList(response.substring(5), ['name','surname','email'],",");
			if (names){
				var name = name_or_email(names[0]);
				if (!AL.empty(name))
					graph_user_name = name;
			}
			graph_demo(graph_user_name);
		},true,'POST',"what my name, surname, email?");
		
		//do this after everything is pumped so we are sure the size is adjusted to fit!
		//TODO: do this on Help button!?
	}
	ajax_request_uri_method_map(base_url,update,true,'POST',r);
}


function map_init(logged) {
	$("#map_similarity_text").text(graph_similarity+"% "+_('similarity'));
	$("#map_connectivity_text").text(graph_connectivity+"% "+_('connectivity'));
	
	document.getElementById('map').style['display']='inline';
	
	if (logged)
		map_init_logged();
	else
		graph_demo();
		
	 $("#map_similarity").change(function(){
		 graph_similarity=$(this).val();
		 $("#map_similarity_text").text(graph_similarity+"% "+_('similarity'));
		 if (graph_o){
			 var svg = document.getElementById('map');
			 svg.parentNode.replaceChild(svg.cloneNode(false), svg);
			 dataToSvg(graph_o, graph_logo, graph_reloader, graph_key);
		 }
	 });	

	 $("#map_connectivity").change(function(){
		 graph_connectivity=$(this).val();
		 $("#map_connectivity_text").text(graph_connectivity+"% "+_('connectivity'));
		 if (graph_o){
			 var svg = document.getElementById('map');
			 svg.parentNode.replaceChild(svg.cloneNode(false), svg);
			 dataToSvg(graph_o, graph_logo, graph_reloader, graph_key);
		 }
	 });	
	 
	 $("#map_period").change(function(){
		 graph_period=$(this).val();
		 var text = period_texts[graph_period]; 
		 $("#map_period_text").text(_(text));
		 if (graph_network && graph_id){
			 breadcrumbs_clear();
			 map_loading(true);
			 reload(graph_network, graph_id, function(){map_loading(false);})
		 }
	 });	
}

function map_loading(start){
	if (loading) //use global function 
		loading(start,2);
	else //use local function
		$('#map_loading').css('display',start ? 'inline' : 'none');
}

//http://stackoverflow.com/questions/286275/gray-out-image-with-css
//TODO: jQuery(selector).fadeTo(speed, opacity);
//	line.setAttribute("style","stroke:"+c+";stroke-width:"+w);
function map_enable(it,enable){
	if (enable){
		$(it).css("opacity","1");
		$(it).css("filter","alpha(opacity=1)");
		$(it).css("cursor","pointer");
	}else{
		$(it).css("opacity","0.3");
		$(it).css("filter","alpha(opacity=30)");
		$(it).css("cursor","default");
	}
	$(it).css("zoom","1");
}

var toggled = null;
function map_toggle(it){
	if (toggled)
		map_enable(toggled,true);
	toggled = it;
	map_enable(it,false);	
}
