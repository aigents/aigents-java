/*
Copyright 2018 Anton Kolonin, Aigents Group

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

//Aigents Core Graphs Library
//depends on
// /ui/aigents-al.js
// /ui/jquery-1.11.1.js

//New Graph API
var AigentsSVG = {
	init : function(id,padding,radius) {
		var svg = document.getElementById(id);
		svg.graph_hashing_names = false; //don't obfuscate names to hashes by default
		svg.padding = padding ? padding : 30;
		svg.radius = radius ? radius : 30;
		svg.full_width = $( '#'+id ).outerWidth();
		svg.full_height = $( '#'+id ).outerHeight();
		svg.label_font = 12;
		svg.spread_directions = 3; //1 - horizontal only, 2 - vertical only, 3 - both
		svg.background = '#FFFFFF';
		this.id = id;
		this.svg = svg;
		return svg;
	},
	clear : function(svg){
		//clear contents
		while (svg.firstChild)
			svg.removeChild(svg.firstChild);
		//set background
		if (svg.background){
			var style = document.createElementNS("http://www.w3.org/2000/svg", "style");
			style.innerHTML = "svg { background-color: "+svg.background+"; }";
			svg.appendChild(style);
		}
	},
    spread_loop : function (svg, nodes, edges, balance_percentage, spread_threshold, top_limit, oscillation_limit, callback) {
    	if (!top_limit || top_limit > 10000)
    		top_limit = 10000;
    	if (!spread_threshold || spread_threshold < 1)
    		return 0;
    	if (!oscillation_limit || oscillation_limit > 100)
    		oscillation_limit = 100;
    	var d = 0;
    	oscillation_count = 0;
    	for (var i = 0; i < top_limit && oscillation_count < oscillation_limit; i++){
    		var dd = this.spread(svg, nodes, edges, balance_percentage);
    		if (callback)
    			callback(i,dd);
    		if (dd < spread_threshold)
    			return d;
    		if (d != 0 && dd > d)//going up not the very first time
    			oscillation_count++;
    		d = dd;
    	}
    	return d;
    },

	//balance_percentage is percentage of "optimal distance between the nodes"
    spread : function (svg, nodes, edges, balance_percentage) {
    	var width = svg.full_width - svg.padding * 2;
    	var height = svg.full_height - svg.padding * 2;
    	var nnodes = nodes.length;
    	var nedges = edges.length;

        var increment = 20;

        //compute optimal balance
        var visible = 0;
        for (var i = 0 ; i < nnodes ; i++)
        	if (!nodes[i].invisible)
        		visible++;
        var sqrtnodes = Math.sqrt(visible);
        var balance = ( width / sqrtnodes + height / sqrtnodes )/2;
        if (!balance_percentage || balance_percentage < 0 || balance_percentage > 100)
        	balance_percentage = 100;
        balance = balance * balance_percentage / 100;

        // push nodes apart
        for (var i = 0 ; i < nnodes ; i++){
            var n1 = nodes[i];
            if (n1.invisible)
            	continue;

            n1.pull_dx = 0;
            n1.pull_dy = 0;
            n1.pull_cnt = 0;
            n1.push_dx = 0;
            n1.push_dy = 0;
            n1.push_cnt = 0;

            function push_node(n,vx,vy){
                if (vx==0 && vy==0){
                    vx=-1000+Math.random()*2000;
                    vy=-1000+Math.random()*2000;
                }
                var len = Math.sqrt( vx * vx + vy * vy );  // OOOOPTIMIIIIZE!!!!!!!!!
                var k = balance / len;

                n.push_dx += (vx / len) * increment * k;
                n.push_dy += (vy / len) * increment * k;
                n.push_cnt++;
            }

            for (var j = 0 ; j < nnodes ; j++){
                if (i == j)
                    continue;
                var n2 = nodes[j];
                if (n2.invisible)
                	continue;
                push_node(n1,n1.point.x - n2.point.x,n1.point.y - n2.point.y);
            }
            push_node(n1, n1.point.x, 0);
            push_node(n1, n1.point.x - width,0);
            push_node(n1, 0, n1.point.y);
            push_node(n1, 0, n1.point.y - height);
        }

        // pull nodes together
        for (var i = 0 ; i < nedges ; i++){
            var e = edges[i];

            // current distance
            var vx = e.v2.point.x - e.v1.point.x;
            var vy = e.v2.point.y - e.v1.point.y;
            var len = Math.sqrt(vx * vx + vy * vy);
            len = (len == 0) ? .0001 : len;

            var k = len / balance;

            // reflect edge stretching
            e.v2.pull_dx -= (vx / len) * increment * k;;
            e.v2.pull_dy -= (vy / len) * increment * k;;
            e.v1.pull_dx += (vx / len) * increment * k;;
            e.v1.pull_dy += (vy / len) * increment * k;;
            e.v2.pull_cnt++;
            e.v1.pull_cnt++;
        }

        var maxdx=0;
        var maxdy=0;

        for (var i = 0 ; i < nnodes ; i++){
            var n = nodes[i];
            if (n.invisible)
            	continue;

            n.push_dx/=n.push_cnt;
            n.push_dy/=n.push_cnt;
            n.pull_dx/=n.push_cnt;
            n.pull_dy/=n.push_cnt;

            var dx = n.push_dx + n.pull_dx;
            var dy = n.push_dy + n.pull_dy;

            var deltax = Math.max(-increment, Math.min(increment, dx));
            var deltay = Math.max(-increment, Math.min(increment, dy));

            if (Math.abs(deltax)>maxdx)
                maxdx = Math.abs(deltax);
            if (Math.abs(deltay)>maxdy)
                maxdy = Math.abs(deltay);

            if (Math.abs(dx)<0.1 && Math.abs(dy)<0.1)
            {
                dx = Math.random()*increment;
                dy = Math.random()*increment;
            }

            if (svg.spread_directons == 1)
            	deltay = 0;
            else if (svg.spread_directons == 2)
            	deltax = 0;

            if (!n.fixed){
                n.point.x += deltax;
                n.point.y += deltay;
            }
            if (n.point.x < 0) {
                n.point.x = 0;
            } else if (n.point.x > width) {
                n.point.x = width;
            }
            if (n.point.y < 0) {
                n.point.y = 0;
            } else if (n.point.y > height) {
                n.point.y = height;
            }

        }
        return Math.round((maxdx+maxdy)/2);
    },

    getSvgPoint : function(svg, xvalue, xbase, yvalue, ymin, ymax, dontmove){
		if (xbase < 1){//hack to avoid divide overflow!
			xvalue = 1;
			xbase = 2;
		}
		if (ymin >= ymax){//hack to avoid divide overflow!
			ymin = 0;
			yvalue = 1;
			ymax = 2;
		}
		var ybase = ymax - ymin;
		var width = svg.full_width - svg.padding * 2;
		var height = svg.full_height - svg.padding * 2;
		var r = svg.radius * 2;
		var width2 = width / 2 - r;
		var xbase2 = xbase / 2;
		x = !dontmove ? xvalue * width / xbase
				: xvalue < xbase2 ? xvalue * width2 / xbase2
				: width2 + r + r + (xvalue - xbase2) * width2 / xbase2;
		var y = ((ymax - yvalue) * height / ybase);
		return {x:x,y:y};
    },

    addSvgText : function(svg, name, xvalue, xbase, yvalue, ybase, text, font){
		var point = this.getSvgPoint(svg, xvalue, xbase, yvalue, 0, ybase);
		var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
		text.setAttribute("x",point.x);
		text.setAttribute("y",point.y);
		text.setAttribute("class","map-text-middle");
		if (font)
			text.setAttribute("font-size",font);
		var textNode = document.createTextNode(name);
		text.appendChild(textNode);
		svg.appendChild(text);
    },

	addSvgTextXY : function(svg, name, x, y, font){
		var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
		text.setAttribute("x",x);
		text.setAttribute("y",y);
		text.setAttribute("class","map-text-middle");
		if (font)
			text.setAttribute("font-size",font);
		var textNode = document.createTextNode(name);
		text.appendChild(textNode);
		svg.appendChild(text);
	},

	addSvgTextToGroup : function(group, x, y, name, capital){
		var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
		text.setAttribute("x",x);
		text.setAttribute("y",y);
		text.setAttribute("class","map-text-middle");
		if (name.length > 8)
			text.setAttribute("font-size",18 / (name.length / 8));
		var textNode = document.createTextNode(!capital || name.match(/\d+/g) ? name : capitalize(name));
		text.appendChild(textNode);
		group.insertBefore(text, null);
	},

	addSvgTextToGroupSplit : function(svg,group, radius, name){
		var space;
		if (svg.graph_hashing_names)
			name = ('' + name.hashCode()).replace('-','');
		var len = name.length;
		if (len > 8 && (space = name.indexOf(' ')) != -1 && space > 1){
			var names = splitName2(name);
			if (names.lenght == 1)
				this.addSvgTextToGroup(group, radius, radius, name);
			else {
				var height = names.length == 2 ? 14 : 10;
				for (var n = 0; n < names.length; n++)
					this.addSvgTextToGroup(group, radius, radius - 7 + height * n, names[n]);
			}
		} else
		if (len > 32){
			len /= 2;
			this.addSvgTextToGroup(group, radius, radius - 7, name.substring(0,len));
			this.addSvgTextToGroup(group, radius, radius + 7, name.substring(len));
		} else
			this.addSvgTextToGroup(group, radius, radius, name);
	},

	addSvgGroup : function(svg, name, xvalue, xbase, yvalue, ymin, ymax, halo_width, dontmove, imageUrl, reloader){
		var point = this.getSvgPoint(svg, xvalue, xbase, yvalue, ymin, ymax, dontmove);
		var saturation = Math.floor((255*yvalue)/ymax);
		return this.addSvgGroupPoint(svg, name, point, halo_width, saturation, 1, imageUrl, reloader);
	},

	addSvgGroupPoint : function(svg, name, point, halo_width, saturation, border, imageUrl, reloader, color){
		var group = document.createElementNS("http://www.w3.org/2000/svg", "g");
		group.setAttribute("transform","translate("+(point.x+svg.padding-svg.radius)+","+(point.y+svg.padding-svg.radius)+")");

		if (reloader || svg.clicker){
			group.setAttribute("cursor","pointer");
			group.setAttribute("name",name);
			$(group).click(function(){
				if (svg.clicker){
					svg.clicker(name);
				} else {
					map_loading(true);
					console.log('opening '+name+'@'+reloader);
					breadcrumbs_push(graph_network,graph_id);
					reload(reloader, graph_id = name, function(){map_loading(false);});
				}
			});
		}
		
		var halo = document.createElementNS("http://www.w3.org/2000/svg", "circle");
		halo.setAttribute("cx",svg.radius);
		halo.setAttribute("cy",svg.radius);
		halo.setAttribute("r",svg.radius+halo_width/2);
		halo.setAttribute("stroke","lightblue");
		halo.setAttribute("stroke-width", halo_width);
		halo.setAttribute("stroke-opacity","0.75");
		halo.setAttribute("fill","lightblue");
		halo.setAttribute("fill-opacity","0.0");
		group.insertBefore(halo, null);

		var circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
		circle.setAttribute("cx",svg.radius);
		circle.setAttribute("cy",svg.radius);
		circle.setAttribute("r",svg.radius);
		circle.setAttribute("stroke","rgb(153, 102, 51)");
		circle.setAttribute("stroke-width", border && border > 0 ? border <=5 ? border : 5 : 1);
		//circle.setAttribute("fill","rgb(255, 255, "+(saturation*255/100)+")");
		circle.setAttribute("fill", RGBtoFFFFFF(color ? color : '#FFFF00', saturation));
		circle.setAttribute("fill-opacity", "0.75");
		group.insertBefore(circle, null);

		if (imageUrl){
			var image = document.createElementNS("http://www.w3.org/2000/svg", "image");
			var size = 3 * svg.radius / 5;
			var halfsize = size / 2;
			//https://stackoverflow.com/questions/27245673/svg-image-element-not-displaying-in-safari
			if (isSafari())
				image.setAttributeNS('http://www.w3.org/1999/xlink', 'href', imageUrl);
			else
				image.setAttribute("href",imageUrl);
			image.setAttribute("x",svg.radius - halfsize);
			image.setAttribute("y",svg.radius + halfsize);
			image.setAttribute("width",size);
			image.setAttribute("height",size);
			group.insertBefore(image, null);
		}

		this.addSvgTextToGroupSplit(svg,group,svg.radius,name)

		if (svg.tooltips){
			var title = document.createElementNS("http://www.w3.org/2000/svg", "title");
			title.innerHTML = name;
			group.appendChild(title);
		}
		svg.appendChild(group);

		if (svg.menu){
			group.id = name;
			$(group).contextmenu(svg.menu);
			$(group).on("taphold",svg.menu);
		}

		return point;
	},

	addSvgArrow : function(svg, x1, y1, x2, y2, c, w){
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
	},

	addSvgArrows : function(svg, x1, y1, x2, y2, l1, l2, w1, w2, c, label){
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

		if (c)
			c1 = c2 = c;
		else
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
			this.addSvgArrow(svg, x1, y1, x0, y0, c1, w1);
		if (l2 > 0)
			this.addSvgArrow(svg, x2, y2, x0, y0, c2, w2);

		if (label)
			this.addSvgTextXY(svg, label, (x1+x2)/2, (y1+y2)/2, svg.label_font);
	  },

	  getTextNodeWidth : function(textNode) {
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
	  },

		layout : function(svg, spread_threshold, spread_balance){
			//spread nodes
			if (spread_threshold) for (var i = 0; i < svg.vertices.length; i++)
				//TODO: 1000 (like was before but less precise) or 2000 (like used in category tree but slow)
				AigentsSVG.spread_loop(svg, svg.vertices[i], svg.edges, spread_balance, spread_threshold, 1000, 10,
						function (iteration,delta){
							//TODO incremental rendering animation?
							//console.log('iteration='+iteration+' delta='+delta);
						});
		},

		render : function(svg, props, colors, image){
			//init
			AigentsSVG.clear(svg);

			//draw edges
			for (var i = 0; i < svg.edges.length; i++){
				var e = svg.edges[i];
				if (e.skip)
					continue;
				AigentsSVG.addSvgArrows(svg, svg.padding + e.v1.point.x, svg.padding + e.v1.point.y, svg.padding + e.v2.point.x, svg.padding + e.v2.point.y, e.strength, e.reverse ? e.reverse.strength : 0, e.strength, e.reverse ? e.reverse.strength : 0, e.color, e.label);
			}

			//draw nodes
			for (var i = 0; i < svg.vertices.length; i++){
				for (var j = 0; j < svg.vertices[i].length; j++){
					var node = svg.vertices[i][j];
					if (!node.invisible){
						var node_prop = props && props[node.name] ? props[node.name] : null;
						var node_image = node_prop && node_prop.image ? node_prop.image : image;
						var node_color = node_prop && colors ? colors[node_prop.is] : null;
						var node_label = node.label != undefined ? node.label : node.name;
						AigentsSVG.addSvgGroupPoint(svg, node_label, node.point, node.rank/5, 100-node.rank,
							2, //TODO: border!?
							node_image,
							null, //TODO: clicker
							node_color);
					}
				}
			}
		}
}//AigentsSVG


//// Node order computations ////
var GraphOrder = {
	//Treat all links symmetric, one-hop, regardless of type, with optional account of weight
	symmetric : function(links,weighted/*,linktypes,iterations*/){
		var orders = {};
		for (var i = 0; i < links.length; i++){
			var link = links[i];
			var weight = weighted && link[2] ? parseFloat(link[2]) : 1;
			counter_add(orders,link[0],weight);
			counter_add(orders,link[1],weight);
		}
		return orders;
	},

	//Treat all links directed, given number of hops, regardless of type, with optional account of weight
	directed : function(links,weighted,linktypes,iterations){
		var log = true;
		//ensure link belongs to one of allowed types, if provided
		function acceptable(link){
			return !linktypes || link.length < 4 || linktypes[link[3]];
		}
	
		//assign defalt order to every node
		function build_graph_orders_default(orders,links,defval){
			for (var i = 0; i < links.length; i++){//just count, don't add
				var link = links[i];
				counter_put(orders,link[0],defval);
				counter_put(orders,link[1],defval);
			}
		}
	
		if (!iterations || iterations == 0)
			iterations = 1000;//safety hack
	
		var orders = {};
		build_graph_orders_default(orders,links,1);//let every node be ranked with 1 at start
		counter_normalize(orders,log);
	
		for (var pass = 0; pass < iterations; pass++){//sanity check limit
			//iterate all links and make every node i rated by node j according to rank of j and weight of rating j made to i
			var new_orders = {};
			build_graph_orders_default(new_orders,links,1);
			for (var i = 0; i < links.length; i++){
				var link = links[i];
				if (!acceptable(link))
					continue;
				var weight = weighted && link[2] ? parseFloat(link[2]) : 1;
				counter_add(new_orders, link[1], orders[link[0]] * weight);//left-to-right
			}
			counter_extend(new_orders,orders);//add low-rank orphans
			counter_normalize(new_orders,log);
			//if distribution of ranks is not changed since previous iteration, stop
			var stddev = counter_stddev(orders,new_orders);
			if (stddev < 0.001)
				break;
			orders = new_orders;
		}
		return orders;
	}
}//GraphOrder

//// Custom graph creation helpers ////
var GraphCustom = { 
	// Builds graph from text file with format: <from> <type> <to> [<strength>]
	build_links : function(data,props){
		var lines = data.split("\n");
		var links = [];
		if (AL.empty(lines))
			return null;
		for (var l = 0; l < lines.length; l++){
			var line = lines[l].trim();
			if (AL.empty(line))
				continue;
			//var terms = line.split(' ');
			var terms = parse(line);//read phrase
			if (terms.length < 3)
				continue;
			if (props && this.mapmap_read_terms(props,terms))//try to parse special properties
				continue;
			var from = terms[0];
			var type = terms[1];
			var to = terms[2];
			var strength = terms.length > 3 ? parseFloat(terms[3]) : 1.0;
			links.push([from,to,strength,type]);
		}
		return links;
	},

	// Builds graph from text, performs ordering per ordering congiguration and attaches graph configuration
	build_graph : function(text,ordering,config){
		//load links and populate nodes
		var nodes = mapmap_init();
		var links = this.build_links(text,nodes);
	
		//merge redundant links, TODO: if needed!?
		links = this.merge_links(links);
	
		//compute rankings
		var orders = ordering ? GraphOrder.directed(links,ordering.weighted,ordering.linktypes,ordering.iterations) : null;
	
		return {nodes:nodes,links:links,orders:orders,config:config};
	},
	
	// Megres link strengths in tuples [from,to,strength,type] if from/to/type are the same
	merge_links : function(links){
		var merged = {};
		for (var l = 0; l < links.length; l++){
			var link = links[l];
			counter_add(merged,link[0]+'\t'+link[1]+'\t'+link[3],link[2]);//merge strength on from+to+type
		}
		var merged_links = [];//rewrite
		for (var key in merged) if (merged.hasOwnProperty(key)) {
			var terms = key.split('\t');
			merged_links.push([terms[0],terms[1],merged[key],terms[2]]);
		}
		return merged_links;
	},
	
	//TODO: have term-reader configurable by list of types
	mapmap_read_terms : function(mapmap,terms){
		var type = terms[1].toLowerCase();
		if (type == 'image'){
			mapmap_put(mapmap,terms[0],type,terms[2]);
			return true;
		}
		if (type == 'is'){
			mapmap_put(mapmap,terms[0],type,terms[2]);
			return true;
		}
	}
}

//Aigents Graph UI interface
var GraphUI = {

		//requests data via POST or GET
		ajax_request_uri_method : function (uri,callback,silent,method,data) {
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
		},

		//request graph launch in designated inline element with respective widgets container
		request_graph : function(graph_id,widgets_id,setup){
			var svg = AigentsSVG.init(graph_id);
			if (setup.graph){
				var graph = setup.graph;
				GraphUI.launch_graph(svg,widgets_id,graph.links,graph.nodes,graph.orders,graph.config);
			} else
			if (setup.path && setup.builder) {
				GraphUI.ajax_request_uri_method(setup.path,function(text){
					var graph = setup.builder(text);
					GraphUI.launch_graph(svg,widgets_id,graph.links,graph.nodes,graph.orders,graph.config);
				},true,'GET',"");
			} else if (setup.text && setup.builder) {
				var graph = setup.builder(setup.text);
				GraphUI.launch_graph(svg,widgets_id,graph.links,graph.nodes,graph.orders,graph.config);
			}
		},

		graph_popup_setup : function () {
			var widgets = $("#graph_popup_widgets");
			return {
				slicing : widgets.children('#custom_slicing').val(),
				node_radius : widgets.children('#custom_node_radius').val(),
				layout_threshold : widgets.children('#custom_layout_threshold').val(),
				layout_directions : widgets.children('#custom_layout_directions').val(),
				layout_balance : widgets.children('#custom_layout_balance').val(),
				filter_range : widgets.children('#custom_filter_range').val()
			};
		},
		
		//request graph launch in popup window
		request_graph_popup : function(title,graph_id,setup){
			var height = $( window ).height() - 30;
			var width = $( window ).width() - 30;
		    var dialog = $( '#graph_dialog' ).dialog({
		    	height: height, width: width,
		    	top: 15, left: 15,
		    	autoOpen: false, modal: true
		    });
		    dialog.dialog('option', 'title', !AL.empty(title) ? title : "Aigents Graphs Demo");
		    dialog.dialog( "open" );
			dialog.html("Loading...");
			dialog.html("<div style=\"width:100%;height:100%;\"><div id=\"graph_popup_widgets\" style=\"z-index:10;float:left;display:inline-block;position:relative;margin:0;padding:3;opacity:0.7;background:gray;\"></div><svg id=\""+graph_id+"\" style=\"position:absolute;top:0;left:0;height:100%;width:100%;\"></svg></div>");
			var widgets = $("#graph_popup_widgets");
			var template = $("#graph_inline_widgets");
			if (template.length > 0){
				widgets.html( $( "#graph_inline_widgets" ).html() );
				widgets.css('border-radius', '5px');
				widgets.draggable();
				if (setup){
					widgets.children('#custom_slicing').val(setup.slicing != null ? setup.slicing : template.children('#custom_slicing').val());
					widgets.children('#custom_node_radius').val(setup.node_radius ? setup.node_radius : template.children('#custom_node_radius').val());
					widgets.children('#custom_layout_threshold').val(setup.layout_threshold != null ? setup.layout_threshold : template.children('#custom_layout_threshold').val());
					widgets.children('#custom_layout_directions').val(setup.layout_directions ? setup.layout_directions : template.children('#custom_layout_directions').val());
					widgets.children('#custom_layout_balance').val(setup.layout_balance ? setup.layout_balance : template.children('#custom_layout_balance').val());
					widgets.children('#custom_filter_range').val(setup.filter_range ? setup.filter_range : template.children('#custom_filter_range').val());
				}
			} else
				widgets.hide();
				
		    this.request_graph(graph_id,'graph_popup_widgets',setup);
		},

		//request graph launch in pre-created inline element
		request_graph_inline : function(graph_id,setup){
			this.request_graph(graph_id,'graph_inline_widgets',setup);
		},

		//get property sheets for node and link types
		get_types : function(links,props){
			if (AL.empty(links))
				return;
			var types = {nodes:{},links:{}};
			function count_node_type(node){
				var prop = props ? props[node] : null;
				var type = prop && prop.is ? prop.is : '';
				types.nodes[type] = true;
			}
			for (var l = 0; l < links.length; l++){
				var link = links[l];
				//from,to,strength,type
				count_node_type(link[0]);
				count_node_type(link[1]);
				types.links[link[3]] = true;
			}
			return types;
		},

		//convert property sheet of enabled types to widgets-contained checkboxes
		map_to_filter : function(widgets,cls,label,map,handler){
			function map_to_checkboxes(cls,label,map){
				var keys = Object.keys(map);
				if (keys.length > 1){
					var c = label ? label : '';
					for (var i = 0; i < keys.length; i++){
						var name = keys[i];
						c += ('<input class=\"'+cls+'\" type=\"checkbox\" name=\"'+name+'\" checked>'+name+'</input>');
					}
					c += '&nbsp;&nbsp;&nbsp;&nbsp;';
					return c;
				}
				return null;
			}
			var e = widgets.children('#'+cls);
			if (e){
				e.empty();
				var checkboxes = map_to_checkboxes(cls,label,map);
				if (!AL.empty(checkboxes)){
					e.html(checkboxes);
					widgets.children('#'+cls).children('.'+cls).on("change",function() {
						handler(this.name,this.checked);
					});
					e.show();
				} else
					e.hide();
			}
		},

		//convert widgets-contained checkboxes to property sheet of enabled types
		filter_to_map : function(widgets,cls,map){
			widgets.children('#'+cls).children('.'+cls).each(function (index){ map[this.name] = this.checked; });
		},

		//launch graph operations under designated svg element
		launch_graph : function(svg,widgets_id,links,props,orders,config){
			var widgets = $('#'+widgets_id);

			//setup node and link filters
			var types = this.get_types(links, props);
			if (types){
				GraphUI.map_to_filter(widgets,'node_types','Nodes:',types.nodes,function (name,checked){
					GraphUI.filter_to_map(widgets,'node_types',types.nodes);
					draw();
				});
				GraphUI.map_to_filter(widgets,'link_types','Links:',types.links,function (name,checked){
					GraphUI.filter_to_map(widgets,'link_types',types.links);
					draw();
				});
			}

			//setup menu
			if (config && config.menu) {
				svg.menu = config.menu;
				$(svg).contextmenu(config.menu);
				$(svg).on("taphold",config.menu);
			}
			
			//setup renderer
			if (config && config.background)
				svg.background = config.background;
			svg.tooltips = true;
			svg.clicker = function (name){
				widgets.children('#custom_node_filter').val(name);
				draw();
			};
			function draw(){
				svg.spread_directons = parseInt(widgets.children('#custom_layout_directions').val());
				svg.slicing = parseInt(widgets.children('#custom_slicing').val());
				svg.labeled_links = config ? config.labeled_links : false;
				GraphUI.draw_graph(svg,orders,links,widgets.children('#custom_layout_threshold').val(),
					parseInt(widgets.children('#custom_layout_balance').val()),
					widgets.children('#custom_node_filter').val(),
					config ? config.colors : null,
					parseInt(widgets.children('#custom_node_radius').val()),
					config.image ? config.image : null,//default image
					props,
					//svg.slicing != 0 && config ? config.slicing : null,
					svg.slicing == 0 ? null : config && config.slicing ? config.slicing : {},
					types,
					widgets.children('#custom_filter_range').val());
			}
			widgets.children('#custom_node_filter').on("search",function() { draw(); });
			widgets.children('.graph_changer').on("change",function() { draw(); });

			//render initially
			draw();
		},

		// draw graph made of links with optional order of nodes and bunch of currently set parameters
		draw_graph : function(svg,orders,links,spread_threshold,spread_balance,filter,colors,radius,image,props,slices,types,range){
			//initialize
			if (radius && radius > 0)
				svg.padding = svg.radius = radius;
			if (range < 1)
				range = 1;

			var regex = filter ? new RegExp(filter,'i') : null;
			
			//create slices
			svg.nodes = {};//all nodes
			svg.types = {};//all types
			svg.vertices = [];//type-specific slices of sorted nodes
			svg.ranges = [];//type-specific ranges of sorted nodes
			if (svg.slicing && svg.slicing > 0 && !AL.empty(slices))//initialize order of slices
				for (var i = 0; i < slices.length; i++)
					get_sorted(slices[i]);

			function get_sorted(type){
				if (!type)
					type = '';
				var sorted = svg.types[type];
				if (!sorted){
					sorted = [];
					svg.types[type] = sorted;
					svg.vertices.push(sorted);
					svg.ranges.push(range_init());
				}
				return sorted;
			}

		//TODO: keep props in actual nodes
			function get_type(node,props){
				var prop = props ? props[node.name] : null;
				return prop && prop.is ? prop.is : '';
			}

			//create nodes
			var rank = range_init();
			var ranked = false;
			var order = 0;
			for (var p in orders){
				if (orders.hasOwnProperty(p)) {
					var r = orders[p];
					var node = {name:p,rank:r,order:order++};
		//TODO: remember node prop object in the node itself
					if (props){
						var prop = props[p];
						if (prop) {
							if (prop.key)
								node.key = prop.key;
							if (prop.label != undefined)
								node.label = prop.label;
						}
					}
		//TODO: eliminate get_type for better performance wih code above
					var type = get_type(node,props);
		//TODO: remember type calculation in the node property?
					if (filter){
						var text = node.label != undefined ? node.label : node.name;
						//if (text.indexOf(filter) != -1)//found (case-sensitive)
						if (text.search(regex) != -1)//found (case-insensitive)
							node.found = true;
						else
							node.invisible = true;//hide if not found
					} else
					if (types && types.nodes && !types.nodes[type])//if types filter is set, make nodes invisible by type
						node.invisible = true;
					get_sorted(!slices ? '' : type).push(node);
					svg.nodes[p] = node;
					range_update(rank,r);
				}
			}

			//weight links
			var s = range_init();
			for (var i = 0; i < links.length; i++){
				var link = links[i];
				range_update(s,link[2]);
			}

			if (filter) {
				for (var r = 1; r <= range; r++){
					var visibles = new Set();
					for (var i = 0; i < links.length; i++){
						var link = links[i];
						var v1 = svg.nodes[link[0]];
						var v2 = svg.nodes[link[1]];
						if (v1 && v2){
							if (types && types.links && !types.links[link[3]])
								continue;//skip edge by type
							if (filter){//if filter is set
								//enable both if both are found or one is found and another is not made invisible by type
								if ((v1.found && v2.found)
										||(!v1.invisible && !(types && types.nodes && !types.nodes[get_type(v2,props)]))
										||(!v2.invisible && !(types && types.nodes && !types.nodes[get_type(v1,props)]))){
									//v1.invisible = v2.invisible = false;
									visibles.add(v1);
									visibles.add(v2);
								}
							}
						}
					}
					for (var v of visibles)
						v.invisible = false;
				}
			}
			
			//create edges and update node visibitily along the way
			svg.reverses = mapmap_init();
			svg.edges = [];
			var interlinked = new Set();//used to track interlinked nodes if filterting by link types
			for (var i = 0; i < links.length; i++){
				var link = links[i];
				var v1 = svg.nodes[link[0]];
				var v2 = svg.nodes[link[1]];
				if (v1 && v2){
					if (types && types.links && !types.links[link[3]])
						continue;//skip edge by type
					if (filter){//if filter is set
						/*TODO: remove 2018-09-07
						//enable both if both are found or one is found and another is not made invisible by type
						if ((v1.found && v2.found)
								||(v1.found && !(types && types.nodes && !types.nodes[get_type(v2,props)]))
								||(v2.found && !(types && types.nodes && !types.nodes[get_type(v1,props)])))
							v1.invisible = v2.invisible = false;
						else
							continue;//skip adding edge
						*/
						if (v1.invisible || v2.invisible)
							continue;
					}else//if no filter is set
					if (v1.invisible || v2.invisible)//hide links between nodes made invisible by type
						continue;
					if (types && types.links){
						interlinked.add(v1);
						interlinked.add(v2);
					}
					var strength = s.max == s.min ? 2 : 1 + (link[2] - s.min) * 5 / (s.max - s.min);//keep real scale of strengths
					if (strength < 0.1)
						strength = 0.1;
					var edge = {v1:v1,v2:v2,strength:strength,label:svg.labeled_links?link[3]:null};
					edge.color = colors && link[3] ? colors[link[3]] : null;
					if (!edge.color)
						edge.color = "#0000ff";
					svg.edges.push(edge);
		//TODO: check link type for reverese link merges!!!
		//TODO: handle overlapping links of different types, packing them into "link packets-buckets"!
					var reverse = mapmap_get(svg.reverses,link[1],link[0]);//lookup primary reverse link
					if (reverse){
						reverse.reverse = edge;
						edge.skip = true;
					} else
						mapmap_put(svg.reverses,link[0],link[1],edge);//store primary reverse link for further reference
				}
			}

			//TODO: use only visibles?
			//rank nodes (one dimension)
			if (rank.min < rank.max){//use rank for y
				for (var i = 0; i < svg.vertices.length; i++){
					for (var j = 0; j < svg.vertices[i].length; j++){
						var node = svg.vertices[i][j];
						if (types && types.links && !interlinked.has(node))
							node.invisible = true;
						if (node.invisible)
							continue;
						node.rank = (node.rank - rank.min) * 100 / (rank.max - rank.min);
						range_update(svg.ranges[i],node.rank);
					}
					//make log distribution centered!?
					range_center(svg.ranges[i]);
				}
				ranked = true;
			} else {//use order for y
				for (var i = 0; i < svg.vertices.length; i++){
					for (var j = 0; j < svg.vertices[i].length; j++){
						var node = svg.vertices[i][j];
						if (node.invisible)
							continue;
						node.order = node.order * 100 / svg.vertices.length;
						range_update(svg.ranges[i],node.rank);
					}
					//make log distribution centered!?
					range_center(svg.ranges[i]);
				}
			}

			//TODO: use only visibles?
			//sort nodes (another dimension)
			for (var i = 0; i < svg.vertices.length; i++)
				svg.vertices[i].sort(function(a,b){
					var str1 = a.key ? a.key : a.name;
					var str2 = b.key ? b.key : b.name;
					return str1 < str2 ? -1 : str1 > str2;
				});

			//TODO: use only visibles?
			//specify node positions
			var dh = svg.full_height / svg.vertices.length;
			var slice_h = dh - svg.padding * 2;
			var dw = svg.full_width / svg.vertices.length;
			var slice_w = dw - svg.padding * 2;
			for (var i = 0; i < svg.vertices.length; i++){
				var range = svg.ranges[i];
				for (var j = 0; j < svg.vertices[i].length; j++){
					var node = svg.vertices[i][j];
					node.point = AigentsSVG.getSvgPoint(svg, j, svg.vertices[i].length - 1, ranked ? node.rank : node.order, range.min, range.max);//dontmove=false,max_rank==100

					//slicing hack:
					if (svg.vertices.length > 1){
						//TODO: if "don't move" or "fixed"
						if (svg.slicing == 1){
							node.point.y = i * dh + 
								(range.max > range.min ? (slice_h * (1 - ((ranked ? node.rank : node.order) - range.min) / (range.max - range.min)))
										: slice_h / 2);
						}else if (svg.slicing == 2){
							node.point.x = i * dw + 
								(svg.vertices[i].length > 1 ? (slice_w * j / (svg.vertices[i].length - 1))
										: slice_w / 2);
						}
					}
					
					//adjust rank to fit sliced scale!?
					node.rank = range.max > range.min ? (node.rank - range.min) * 100 / (range.max - range.min) : 50;
 				}
			}

			//TODO: move calculation of positions above here!?
			AigentsSVG.layout(svg, spread_threshold, spread_balance)

			//final render
			AigentsSVG.render(svg, props, colors, image);
		}
}//GraphUI
