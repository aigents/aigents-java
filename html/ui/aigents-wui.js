/*
Copyright 2018-2020 Anton Kolonin, Aigents®

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

//Aigents Web UI
//depends on
// /ui/aigents-al.js
// /ui/aigents-gui.js
// /ui/jquery-1.11.1.js

var selected_things;
var selected_sites; 
var selected_news;
var selected_peers; 
var selected_thing_index;
var selected_site_index;
var selected_news_index;
var selected_peer_index;

//TODO: support multiple areas selected
var selected_area = null;

//TODO: more universal across different use-cases of menu
//https://jqueryui.com/menu/
function menu_hide(m){
	if (m){
		m.css("display","none");
		m = null;
	}
}
function menu_show(m,event){
	var mw = m.width();
	var mh = m.height();
	var menu_top = event.clientY - mh/2;
	var menu_left = event.clientX - mw/2;
	if (menu_left < 0)
		menu_left = 0;
	m.css("position","absolute");
	m.css("display","inline");
	m.offset({top:menu_top,left:menu_left});
	m.menu();
}
//local menu operations
var menu = null;
var menu_list = null;
var menu_data = null;
var menu_check_index = null;
function hide_menu(){menu_hide(menu);}
function show_menu(event){menu_show(menu,event)}
function init_menu(list,data,check_index){
	menu_list = list;
	menu_data = data;
	menu_check_index = check_index;
}

//TODO: do it here and do it this way?
function news_open(){
	if ($('#pane_news').css("display")=="none"){
		news_refresh();
		$('.pane').css("display", "none");
		$('#pane_news').css("display", "inline");
		//$(".menu-item").addClass("menu-item");
		//$(".menu-item").removeClass("menu-check");
		$(".menu-check").removeClass("menu-check").addClass("menu-item");
		//$('#menu_news').addClass("menu-check");
		//$('#menu_news').removeClass("menu-item");
		$('#menu_news').addClass("menu-check").removeClass("menu-item");
	}
}

$(function() {
	
	$(window).ready(function (){
		update_size();
		filter_areas();
		filter_aigents();
		//TODO: do it here and do it this way?
		$('#menu_aigents').addClass("menu-check").removeClass("menu-item");
		//$('#menu_aigents').removeClass("menu-item");
	});
	$(window).resize(function() {
		update_size();   
	});

	//new code
    $(".menu-item").hover(
    		  function () {
       		    $(this).addClass("menu-hover");
       		    $(this).removeClass("menu-item");
    		  },
    		  function () {
      		    $(this).addClass("menu-item");
    		    $(this).removeClass("menu-hover");
    		  }
    		); 
    
	$(".menu-item").click(function() {
		//$(".menu-item").addClass("menu-item");
		//$(".menu-item").removeClass("menu-check");
		//$(this).addClass("menu-check");
		//$(this).removeClass("menu-hover");
		$(".menu-check").removeClass("menu-check").addClass("menu-item");
		$(this).addClass("menu-check").removeClass("menu-hover");
	});
	$("#menu_aigents").click(function() {
    	$('.pane').css("display", "none");
    	$('#pane_areas').css("display", "inline-block");
    	$('#pane_aigents').css("display", "inline-block");
	});
	$("#menu_things").click(function() { 
    	things_refresh();
    	$('.pane').css("display", "none");
    	$('#pane_things').css("display", "inline");
	});
	$("#menu_sites").click(function() {
    	sites_refresh();
    	$('.pane').css("display", "none");
    	$('#pane_sites').css("display", "inline");
	});
	$("#menu_news").click(function() {
		news_open();
	});
	$("#menu_peers").click(function() {
    	peers_refresh();
    	$('.pane').css("display", "none");
    	$('#pane_peers').css("display", "inline");
	});
	$("#menu_graph").click(function() {
    	$('.pane').css("display", "none");
    	$('#pane_graph').css("display", "inline");
    	graph_refresh();
	});
	$("#menu_talks").click(function() {
    	talks_scroll();
    	$('.pane').css("display", "none");
    	$('#pane_talks').css("display", "inline");
	});    
	
    $( "#areas_input" ).on("input", function() {filter_areas(this.value)});
    $( "#aigents_input" ).on("input", function() {filter_aigents(this.value)});
	
    //TODO: proper locale matching
    var russian_areas = ['проект','продукты','технология','новости','помощь','контакты'];
    function match_locale(locale, text){
    	if (textMatchingAnyString(text,russian_areas))//if text is russian
    		return locale == 'ru';// and requested russian, return true
    	else//if text is not russian
    		return locale != 'ru';//and requested not russia, return true
    }
    
    function filter_areas(query) {
    	var locale = get_locale();
    	query = query && query.length > 0 ? query.toLowerCase() : null;
    	$('#areas_list li').each(function() {
    		var text = $( this ).text().toLowerCase();
    		if ((!query || text.indexOf(query) != -1) && match_locale(locale,text))
    			$( this ).css("display","list-item");
    		else
    			$( this ).css("display","none");
    	});
    }

    $("#areas_list li input").change(function() {
    	//TODO: support multiple areas selected
        if(this.checked) {
            //Do stuff
        	$('#areas_list li input').each(function() {
        		$( this ).prop('checked', false);
        	});
        	$( this ).prop('checked', true);
        	selected_area = $( this ).parent().text().toLowerCase();
        }else{
        	selected_area = null;
        }
    	filter_aigents($( "#aigents_input" ).val());
    });
    
    function filter_aigents(query) {
    	var locale = get_locale();
    	query = query && query.length > 0 ? query.toLowerCase() : null;
    	$('#aigents_list li').each(function() {
    		var text = $( this ).text().toLowerCase();
    		$( this ).css("display", 
    			(!query || text.indexOf(query) != -1) && //no search query or query match
    			(!selected_area || text.indexOf(selected_area) != -1) && //no selected area or area match
    			(selected_area || match_locale(locale,text)) //selected area or locale match
    			? "list-item" : "none");
    	});
    }
    
    $( "#things_list" ).selectable({
      stop: function() {
        selected_things = [];
        var index;
        $( ".ui-selected", this ).each(function() {
        	if ($(this)[0].id){
        		selected_things.push( $(this) );
        		index = $("#things_list li").index(this);
        	}
        });
        if (selected_things.length == 1) {
      	  if (selected_thing_index == index)
      	  	edit_thing(selected_things[0]);
      	  selected_thing_index = index;
        }
       }
    });
    $( "#add_thing" ).button({text: false,icons: {primary: "ui-icon-add"}}).click(function() {add_thing();});
    $( "#del_thing" ).button({text: false, icons: {primary: "ui-icon-del"}}).click(function() {del_thing();});
    $( "#things_props" ).button({text: false,icons: {primary: "ui-icon-props"}}).click(function() {your_properties();});
    $( "#things_input" ).on( "keydown", function(event) { if (event.which == 13) add_thing(); });
    $( "#things_input" ).on("input", function() {trusts_init("#things_list",things_data,this.value);});

    $( "#sites_list" ).selectable({
      cancel: "a,input,button",
      stop: function() {
        selected_sites = [];
        var index;
        $( ".ui-selected", this ).each(function() {
        	if ($(this)[0].id){
        		selected_sites.push( $(this) );
        		index = $("#sites_list li").index(this);
        	}
        });
        if (selected_sites.length == 1) {
      	  if (selected_site_index == index) 
      	  	edit_site(selected_sites[0]);
          selected_site_index = index;
        }
      }
    });

    $( "#add_site" ).button({text: false,icons: {primary: "ui-icon-add"}}).click(function() {add_site();});
    $( "#del_site" ).button({text: false, icons: {primary: "ui-icon-del"}}).click(function() {del_site();});
    $( "#open_site" ).button({text: false, icons: {primary: "ui-icon-open"}}).click(function() {open_site();});
    $( "#sites_props" ).button({text: false,icons: {primary: "ui-icon-props"}}).click(function() {your_properties();});
    $( "#sites_input" ).on( "keydown", function(event) { if (event.which == 13) add_site(); });
    $( "#sites_input" ).on("input", function() {trusts_init("#sites_list",sites_data,this.value); });

    $( "#news_list" ).selectable({
      cancel: "a,input,button",
      stop: function() {
        selected_news = [];
        var index;
        $( ".ui-selected", this ).each(function() {
        	if ($(this)[0].id){
        		selected_news.push( $(this) );//TODO: del by combination of elements, single selected
        		index = this.id;
        	}
        });
        if (selected_news.length == 1) {
            if (selected_news_index == index)
        	  edit_news(selected_news[0],selected_news_index);
          selected_news_index = index;
        }
      }
    });
    $( "#add_news" ).button({text: false,icons: {primary: "ui-icon-add"}}).click(add_news);
    $( "#del_news" ).button({text: false, icons: {primary: "ui-icon-del"}}).click(function() {del_news();});  
    $( "#open_news" ).button({text: false,icons: {primary: "ui-icon-open"}}).click(function() {open_news();});
    $( "#news_props" ).button({text: false,icons: {primary: "ui-icon-props"}}).click(function() {your_properties();});
    $( "#news_input" ).on( "keydown", function(event) { if (event.which == 13) add_news(); });
    $( "#news_input" ).on("input", function() {news_init('#news_list',news_data,this.value); });
      
    $( "#peers_list" ).selectable({
      cancel: "a,input,button",
      stop: function() {
        selected_peers = [];
        $( ".ui-selected", this ).each(function() {
        	if ($(this)[0].id){
        		selected_peers.push( $(this) );//TODO: del by entire title (with hidden dob?)
        		var index = $("#peers_list li").index(this);
        		if (selected_peer_index == index)
        			edit_peer(this);
        		selected_peer_index = index;
        	}
        });
      }
    });
    $( "#add_peer" ).button({text: false,icons: {primary: "ui-icon-add"}}).click(function() {add_peer();});
    $( "#del_peer" ).button({text: false,icons: {primary: "ui-icon-del"}}).click(function() {del_peer();});
    $( "#open_peer" ).button({text: false,icons: {primary: "ui-icon-open"}}).click(function() {open_peer();});
    $( "#peers_props" ).button({text: false,icons: {primary: "ui-icon-props"}}).click(function() {your_properties();});
    $( "#peers_input" ).on( "keydown", function(event) { if (event.which == 13) add_peer(); });
    $( "#peers_input" ).on("input", function() {peers_init('#peers_list',peers_data,this.value); });

    $( "#graph_input" ).on("input", function() {graph_filter(this.value);});
    
    $( "#say" ).button({text: false,icons: {primary: "ui-icon-add"}}).click(function() {talks_say();});
    $( "#clear" ).button({text: false,icons: {primary: "ui-icon-del"}}).click(function() {talks_clear();});
    $( "#talks_props" ).button({text: false,icons: {primary: "ui-icon-props"}}).click(function() {your_properties();});
    $( "#talks_input" ).on( "keydown", function(event) { if (event.which == 13) talks_say(); });

    $("#vkontakte").click(function(event){    	
    	event.stopPropagation();
    	if (!login_menu("#vkontakte","VKontakte"))
    		window.vkontakteLogin();
    });
    
    $("#google").click(function(event){
    	event.stopPropagation();
    	if (!login_menu("#google","Google"))
    		window.loginGoogleApi();
    });
    
    $("#facebook").click(function(event){
    	event.stopPropagation();
    	if (!login_menu("#facebook","Facebook")){
    		//https://developers.facebook.com/docs/reference/javascript/FB.login/v5.0#permissions
    		FB.login(function(response) {
	        	window.facebookStatusChangeCallback(response);
	        }, {scope: 'email,pages_messaging,public_profile,user_posts'});
	        //TODO: later, when the rest is resolved
        	//},{scope: 'user_posts,user_likes,user_friends'});
    	}
    });

    $("#aigents").click(function(event){
    	event.stopPropagation();
    	if (!login_menu("#aigents","Aigents")){
    		your_properties();//login
    	}
    });

    $("#reddit").click(function(event){
        event.stopPropagation();
        var reddit_redirect = base_url+"/reddit";
        var reddit_id = "tp-g-UnxYQsZKw";
        var session = getCookie('aigent');
        if (!login_menu("#reddit","Reddit"))
        	window.location.href = "https://www.reddit.com/api/v1/authorize?client_id="+reddit_id+"&response_type=code&state="+session+"&redirect_uri="+reddit_redirect+"&duration=permanent&scope=identity,read,history";
    });
    
    $("#twitter").click(function(event){
        event.stopPropagation();
        if (!login_menu("#twitter","Twitter"))
        	window.location.href = base_url+"/twitter?login";
    });
    
});

function get_locale() {
	//return 'ru';//TODO: remove hack!
    var language = window.navigator.userLanguage || window.navigator.language;
    var la = language ? language.substr(0,2) : null;
    return la ? la.toLowerCase() : null;
}

function get_language() {
	//return 'en';//TODO: remove hack!
    var la = get_locale();
    return la == 'ru' ? 'russian' : la == 'zh' ? 'chinese' : 'english';
}

function localize() {
	var la = get_locale();
	if (la == 'ru') {
		locale_map = locale_ru;
		//locale_map = locale_zh;
		translate_locale(locale_map);
	}
	else
	if (la == 'zh') {
		locale_map = locale_zh;
		translate_locale(locale_map);
	}
	else
		locale_map = locale_en;
}

//--- Size fixing
function update_size() {
	//var windowH = $( window ).height();
	var windowW = window.innerWidth;
	var windowH = window.innerHeight;
	var menuH = $( ".menu-bar:first" ).outerHeight();
	var inputH = $( ".input-bar:first" ).outerHeight();
	var copyrightH = $( "#copyright:first" ).outerHeight();
	var paneH = windowH - menuH - inputH - copyrightH;
	$(".pane .list").css('height',paneH);
	$(".pane .log").css('height',paneH);
	$(".pane #map_frame").css('height',paneH);

	//move news counter to right positions
	var news_offset = $('#menu_news').offset();
	news_offset.left = news_offset.left-8;
	$('.count').offset(news_offset);
	
	//adjust areas selector
	/*
	if (windowW < 400){
		//TODO: add areas selector gadget to menu/tool bar
    	$('.pane_areas').css("display", "none");
	}else{
    	$('.pane_areas').css("display", "block");
	}
	*/
}

//--- Auto-refreshing --- 
function auto_refresh() {
	if (auto_refreshing) {
		news_refresh_thought();
		setTimeout("auto_refresh()",refresh_millis);
	}
}

//--- Idle help link display --- 
var idleTime = 0;
var status_shown = false;
$(document).ready(function () {
    var idleInterval = setInterval(timerIncrement, 1000); // 1 second
    $(this).mousemove(function (e) {
        idleTime = 0;
    });
    $(this).keypress(function (e) {
        idleTime = 0;
        //displayStatus(null);
        //displayAction(null);
        hide_menu();
    });
    $(this).click(function (e) {
        idleTime = 0;
        //displayStatus(null);
        //displayAction(null);
        hide_menu();
    });
    $('#google').mouseenter(function() {login_menu("#google","Google");});
    $('#facebook').mouseenter(function() {login_menu("#facebook","Facebook");});
    $('#vkontakte').mouseenter(function() {login_menu("#vkontakte","VKontakte");});
    $('#aigents').mouseenter(function() {login_menu("#aigents","Aigents");});
    $('#reddit').mouseenter(function() {login_menu("#reddit","Reddit");});
    $('#paypal').mouseenter(function() {login_menu("#paypal","PayPal");});
    $('#twitter').mouseenter(function() {login_menu("#twitter","Twitter");});
    localize();
});
function timerIncrement() {
    idleTime = idleTime + 1;
    if (idleTime >= status_seconds) {
    	idleTime = 0;
    	if (!status_shown) {
    		if (document.getElementById('status') && !document.getElementById('status').disabled){
    			$('#status').html(_('help'));
    			$('#status').show();
    		}
    		status_shown = true;
    	} else {
    		$('#status').hide();
    		status_shown = false;
    	}
    }
}

//TODO: separate sorting modes for things, sites and peers 
var peers_sorting_mode = 0;

function trusts_data_sort(a,b){
	if (peers_sorting_mode == 1){//by relevance
		var ra = a[2] ? a[2] : 0;
		var rb = b[2] ? b[2] : 0;
		var cmp = rb - ra;
    	if (cmp != 0)
    		return cmp;
	} else //by trust
	if (a[1] != b[1])
		return a[1] ? -1 : 1;//by trust
	return strcmp(a[0],b[0]);//by name
}

function trusts_sort(selector) {
    $(selector).children("li").sort(function(a, b) {
    	var da = menu_data[$(a)[0].id];
    	var db = menu_data[$(b)[0].id];
    	return trusts_data_sort(da, db);
    	/*
    	if (peers_sorting_mode == 1){//by relevance
    		var ra = menu_data[$(a)[0].id][2];
    		var rb = menu_data[$(b)[0].id][2];
    		var cmp = rb - ra;
        	if (cmp != 0)
        		return cmp;
    	}
    	var cmp = strcmp($(b).children(".trust").prop('checked'),$(a).children(".trust").prop('checked'));
    	if (cmp != 0)
    		return cmp;
    	return strcmp($(a).text(),$(b).text());
    	*/
    }).appendTo(selector);
}

function peers_sort(selector) {
    $(selector).children("li").sort(function(a, b) {
    	var da = peers_data[$(a)[0].id];
    	var db = peers_data[$(b)[0].id];
    	if (peers_sorting_mode == 1){//by relevance
    		var cmp = (db[7] ? db[7] : 0) - (da[7] ? da[7] : 0);
        	if (cmp != 0)
        		return cmp;
    	}
    	//TODO:account of trust or not?
    	/*
    	//var cmp = strcmp($(b).children(".trust").prop('checked'),$(a).children(".trust").prop('checked'));
    	var cmp = da[3] == db[3] ? 0 : da[3] ? -1 : +1;
    	if (cmp != 0)
    		return cmp;
    	*/
    	return strcmp($(a).text(),$(b).text());
    }).appendTo(selector);
}

function trusts_update(selector,string,values) {
	if (AL.empty(string)) 
		$(selector).empty();
	else {
		parseToGrid(values,string,["name", "trust", "relevance", "positive", "negative"],",");
		values.sort(trusts_data_sort);
		trusts_init(selector,values);
	}
}

function del_trusts(list,deletee){
	if (deletee) {//if deletee is explicitly set
    	if (list == "#things_list")
            selected_things = [deletee];
    	else
    	if (list == "#sites_list")
            selected_sites = [deletee];
    	else
    	if (list == "#peers_list")
            selected_peers = [deletee];
	}
	if (list == "#things_list")
        del_thing();
	else
	if (list == "#sites_list")
        del_site();
	else
	if (list == "#peers_list")
		del_peer();	
}

function trusts_menu(event) {
	event.preventDefault();
	if (menu)
		hide_menu();
	menu = $( "#trusts_menu" );
	function refresh_selection() {
		if (menu_list == "#things_list") {
			selected_things = [];
			$( ".ui-selected", $(menu_list) ).each(function() {if ($(this)[0].id) selected_things.push( $(this) );});
		} else
		if (menu_list == "#sites_list"){
			selected_sites = [];
			$( ".ui-selected", $(menu_list) ).each(function() {if ($(this)[0].id)selected_sites.push( $(this) );});
		} else
		if (menu_list == "#peers_list"){
			selected_peers = [];
			$( ".ui-selected", $(menu_list) ).each(function() {if ($(this)[0].id)selected_peers.push( $(this) );});
		}
	}
	menu.mouseleave(function (){ hide_menu();});
	if (!$(this).hasClass("ui-selected"))//make others unselected
		$(".ui-selected").removeClass("ui-selected");
	$(this).addClass("ui-selected");//make this selected
	var element = $(this);
	var index = element[0].id;
	var data = menu_data[index];
	var check = $("#trusts_menu_check");
	var checked = data[menu_check_index];
	check.html(checked? _("Uncheck") : _("Check"));
	check.off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	var cmd = '';
    	var indexes = []; 
    	$( ".ui-selected", $(menu_list) ).each(function() { if ($(this)[0].id){
    		var element = $(this);
    		var index = element[0].id;
    		if (cmd.length > 0)
    			cmd += ' ';
    		menu_data[index][menu_check_index] = !checked;
    		var q;
    		if (menu_list == "#peers_list")
    			q = peer_qualifier_elem(element)+ ' share ' + (!checked ? 'true' : 'false') + '.';
    		else
    			//use data instead of element text because it may be escaped incorrectly
    			//q = 'name '+AL.toString(element.text(),name)+' trust ' + (!checked ? 'true' : 'false') + '.';
				q = 'name '+AL.toString(menu_data[index][0],name)+' trust ' + (!checked ? 'true' : 'false') + '.';
			cmd += q;
    	}});
		requestBase(null,cmd);
		//refresh in-place
		//TODO: same re-init for news!?
		if (menu_list == "#things_list"){
			menu_data.sort(trusts_data_sort);
			trusts_init("#things_list",menu_data,$("#things_input").val());
		}else
		if (menu_list == "#sites_list"){
			menu_data.sort(trusts_data_sort);
			trusts_init("#sites_list",menu_data,$("#sites_input").val());
		}else
		if (menu_list == "#peers_list"){
			peers_init('#peers_list',peers_data,$("#peers_input").val());
		}
    });
	$("#trusts_menu_open").html(_('Open'));
	$("#trusts_menu_open").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
		if (menu_list == "#things_list")
			edit_thing(element);
		else
		if (menu_list == "#sites_list")
			edit_site(element);
        //TODO: add site view to open edit for news and sites
		else
		if (menu_list == "#peers_list")
			edit_peer(element);
	});
	$("#trusts_menu_delete").html(_('Hide')); 
	$("#trusts_menu_delete").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
        refresh_selection();
        del_trusts(menu_list);
	});
	//TODO: separate soring modes for sites, knows and peers?
	//if (menu_list == "#peers_list"){
	{
		var sorting_label = peers_sorting_mode == 0 ? _("Sort by relevance") : _("Sort by name");
		$("#trusts_menu_sort").html(sorting_label);
		$("#trusts_menu_sort").off().click(function(event){
	    	event.stopPropagation();
	    	hide_menu();
	    	peers_sorting_mode = peers_sorting_mode == 0 ? 1 : 0;
		    if (menu_list == "#things_list"){
				menu_data.sort(trusts_data_sort);
	    		trusts_init('#things_list',things_data,$("#things_input").val());
		    } else
		    if (menu_list == "#sites_list"){
				menu_data.sort(trusts_data_sort);
	    		trusts_init('#sites_list',sites_data,$("#sites_input").val());
		    } else
	    	if (menu_list == "#peers_list"){
	    		peers_init('#peers_list',peers_data,$("#peers_input").val());
	    	}
		});
	}
	if (menu_list == "#sites_list"){
		$("#topics_menu_graph_patterns").hide();
		$("#topics_menu_graph_things").hide();
		var site_url = $(element).text();
		if (AL.isURL(site_url)){
			$("#sites_menu_graph").show();
			$("#sites_menu_graph").off().click(function(event){
		    	event.stopPropagation();
		    	hide_menu();
				if (graph_init){
					var range = 1;//because some graphs may be to large
					//TODO: read range and timeout
					graph_init('www',site_url,0,range);
					graph_launch(site_url);
				}
			});
		} else
			$("#sites_menu_graph").hide();
	} else {
		$("#sites_menu_graph").hide();
		$("#topics_menu_graph_patterns").show();
		$("#topics_menu_graph_things").show();
		$("#topics_menu_graph_patterns").off().click(function(event){
	    	event.stopPropagation();
	    	hide_menu();
	    	graph_thing_patterns($(element).text());
		});
		$("#topics_menu_graph_things").off().click(function(event){
	    	event.stopPropagation();
	    	hide_menu();
	    	graph_thing_instances($(element).text());
		});
	}
	show_menu(event);
}

//TODO: remove use of #peers_list above
function peers_menu(event) {
	event.preventDefault();
	if (menu)
		hide_menu();
	menu = $( "#peers_menu" );
	function refresh_selection() {
		selected_peers = [];
		$( ".ui-selected", $(menu_list) ).each(function() {if ($(this)[0].id)selected_peers.push( $(this) );});
	}
	menu.mouseleave(function (){ hide_menu();});
	if (!$(this).hasClass("ui-selected"))//make others unselected
		$(".ui-selected").removeClass("ui-selected");
	$(this).addClass("ui-selected");//make this selected
	var element = $(this);
	var index = element[0].id;
	var data = menu_data[index];
	var check_trust = $("#peers_menu_trust");
	var check_share = $("#peers_menu_share");
	var checked_trust = data[8];
	var checked_share = data[3];
	check_trust.html(checked_trust? _("Don't receive") : _("Receive"));
	check_share.html(checked_share? _("Don't share") : _("Share"));
	check_trust.off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	var cmd = '';
    	var indexes = []; 
    	$( ".ui-selected", $(menu_list) ).each(function() { if ($(this)[0].id){
    		var element = $(this);
    		var index = element[0].id;
    		if (cmd.length > 0)
    			cmd += ' ';
    		menu_data[index][8] = !checked_trust;
    		var q = peer_qualifier_elem(element)+ ' trust ' + (!checked_trust ? 'true' : 'false') + '.';
    		cmd += q;
    	}});
		requestBase(null,cmd);
		peers_init('#peers_list',peers_data,$("#peers_input").val());
    });
	check_share.off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	var cmd = '';
    	var indexes = []; 
    	$( ".ui-selected", $(menu_list) ).each(function() { if ($(this)[0].id){
    		var element = $(this);
    		var index = element[0].id;
    		if (cmd.length > 0)
    			cmd += ' ';
    		menu_data[index][3] = !checked_share;
    		var q = peer_qualifier_elem(element)+ ' share ' + (!checked_share ? 'true' : 'false') + '.';
    		cmd += q;
    	}});
		requestBase(null,cmd);
		peers_init('#peers_list',peers_data,$("#peers_input").val());
    });
	
	$("#peers_menu_open").html(_('Open'));
	$("#peers_menu_open").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
		edit_peer(element);
	});
	$("#peers_menu_delete").html(_('Hide')); 
	$("#peers_menu_delete").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
        refresh_selection();
        del_trusts(menu_list);
	});
	{
		var sorting_label = peers_sorting_mode == 0 ? _("Sort by relevance") : _("Sort by name");
		$("#peers_menu_sort").html(sorting_label);
		$("#peers_menu_sort").off().click(function(event){
	    	event.stopPropagation();
	    	hide_menu();
	    	peers_sorting_mode = peers_sorting_mode == 0 ? 1 : 0;
	    	peers_init('#peers_list',peers_data,$("#peers_input").val());
		});
	}
	$("#peers_menu_graph").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
        graph_peers(peers_data,peer_obj);//pass data and objectification API function
	});
	show_menu(event);
}

function trusts_init(list,data,filter) {
	$(list).empty();
	filter = AL.empty(filter)? null : filter.toLowerCase();
	for (i = 0; i < data.length; i++) {
		var text = data[i][0];
		if (filter && text.toLowerCase().indexOf(filter) == -1)
			continue;
		var check = $('<input class="trust" type="checkbox"/ '+ (data[i][1]?'checked':'') +'>');
		check.change(function(eventObject) {
			var parent = $(this).parent().parent().parent().parent();
			var id = parent[0].id;
			data[id][1] = this.checked;
 			//use data instead of element text because it may be escaped incorrectly
			//var q = 'name '+AL.toString($(this).parent().text(),name)+' trust ' + (this.checked ? 'true' : 'false') + '.';
			var q = 'name '+AL.toString(data[id][0],name)+' trust ' + (this.checked ? 'true' : 'false') + '.';
			requestBase(null,q);
			trusts_sort(list);
//TODO:ensure it is not needed!
//			data.sort(trusts_data_sort);
		});
		var del_button = $('<button type="button" style="float:right;display:inline-block;height:20;width:20;" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only ui-dialog-titlebar-close" role="button" title="'+_("trusts_menu_delete","title")+'"><span class="ui-button-icon-primary ui-icon ui-icon-closethick"></span></button>');
		del_button.click(function(event) {
			var parent = $(this).parent();
			var id = parent[0].id;
	    	event.stopPropagation();
	    	del_trusts(list,parent);
 		});
		var relevance = data[i][2] ? +data[i][2] : 0; //+ for number
		//var sentiment = data[i][2] ? +data[i][3] : 0;
		//var positive = sentiment && sentiment > 0 ? +sentiment : 0;
		//var negative = sentiment && sentiment < 0 ? -sentiment : 0;
		var positive = data[i][3] ? +data[i][3] : 0;
		var negative = data[i][4] ? +data[i][4] : 0;
//console.log(positive+" "+negative+" "+text);
//var relevance = i * 100 / (data.length - 1);
//var positive = 100 - i * 100 / (data.length - 1);
//var negative = i * 100 / (data.length - 1);
		
/*
		//relevance by bar
		var html = !AL.isURL(text) ?  '<span class="name">'+text+'</span>'
				: $('<a class="name" href="'+text+'" target="_blank">').append(text).append('</a>');
		var relevance = $('<div style="display:inline-block;overfow:visible;background-color:lightblue;width:'+relevance+'%;"/>')
		.append(check)
		.append(html);
*/		
		//relevance and sentiments by bar
		var all_relevances = relevance + positive + negative;//full bar size
		var two_relevances = relevance + positive;
//TODO: same NaN fix for news relevances
		var personal_p = two_relevances == 0 ? 0 : Math.round(100 * relevance / (two_relevances)); 
		var positive_p = all_relevances == 0 ? 0 : Math.round(100 * (two_relevances) / all_relevances); 
		var negative_p = Math.round(all_relevances / 3);
		
		var html = !AL.isURL(text) ?  '<span class="name">'+text+'</span>'
				: $('<a class="name" href="'+text+'" target="_blank">').append(text).append('</a>');
		var r1 = $('<div style="display:inline-block;overfow:visible;background-color:lightblue;width:'+personal_p+'%;"/>')
		.append(check)
		.append(html);
		var r2 = $('<div style="display:inline-block;overfow:visible;background-color:lightgreen;width:'+positive_p+'%;"/>')
		.append(r1);
		var relevance = $('<div style="display:inline-block;overfow:visible;background-color:lightpink;width:'+negative_p+'%;"/>')
		.append(r2);

		var trusts_row = $('<li id='+i+' class="ui-widget-content">')
		.append(del_button)
		.append(relevance)
		.append('</li>').appendTo(list);
		
		init_menu(list,data,1);
		trusts_row.contextmenu(trusts_menu);
		trusts_row.on("taphold",trusts_menu);
	}
    $("li a").click(function(){
    	read_site($(this).attr('href'));
    	return false;
    });
}

function name_to_id(name) {
	var toreplace = AL.spaces + AL.punctuation + AL.special;
	var id = 'id';
	for (var i = 0; i < name.length; i++)
		id += toreplace.indexOf(name[i]) != -1 ? '_' : name[i];
	return id;
}

var dialog_fields = null;
var dialog_values = null;
var dialog_envents_bound = false;
function dialog_open(title,hint,fields,prompts,values,readonly,onOk,type,onCancel,options) {
	dialog_fields = fields;
	var window_height = $( window ).height();
	var window_width = $( window ).width();
    //var height = fields.length <= 1 ? 270 : 450;
    var height = fields.length <= 5 ? 190 + fields.length * 75 : 570;
    //var width = fields.length <= 1 ? 550 : 450; 
    var width = 500; 
    var left = 0;
    var top = 0;
    if (height > window_height)
    	height = window_height;
    else
    	top = (window_height - height)/2;
    if (width > window_width)
    	width = window_width;
    else
    	left = (window_width - width)/2;
    function doClick() {
        if (!onOk || onOk())//if no onOk handler specified on it is specified and allows close
        	$( this ).dialog( "close" );
    }
    function doCancel() {
        if (onCancel)
        	onCancel();
        $( this ).dialog( "close" ); 
    }
    var dialog = $( "#dialog" ).dialog({
    	autoOpen: false, 
    	height: height, 
    	width: width,
    	top: top,
      	left: left,
      	modal: true,
        buttons: type == 'login' ?
	        	[{ text: _("Log in"),  id: "dialog_default", click:doClick },
	            { text: _("Registration"), click: function() {
	        		$( this ).dialog( "close" );
	        		logged_email = $( '#'+name_to_id('email') ).val();
	    	        //ask('email, name, surname, birth date','registration',!AL.empty(logged_email)? [logged_email]: null);
	    	        ask('email, name, surname','registration',!AL.empty(logged_email)? [logged_email]: null);
	            }},
	            { text: _("Cancel"), click: doCancel}]      
	    	: type == 'registration' ?
	    	    [{ text: _("Register"), id: "dialog_default", click: doClick},
	    	    { text: _("Login"), click: function() {
	    	        $( this ).dialog( "close" );
	        		logged_email = $( '#'+name_to_id('email') ).val();
	    	        ask('email','login',!AL.empty(logged_email)? [logged_email]: null);
	    	    }},
	    	    { text: _("Cancel"), click: doCancel}]      
		    : type == 'confirmation' ?
		    	[{ text: _("Confirm"), id: "dialog_default", click: doClick},
			    { text: _("Reset"), click: function() {
			    	requestBase(null,'What my secret answer?');
			    	$( this ).dialog( "close" );
			    }},
			    { text: _("Cancel"), click: doCancel}]      
		    : type == 'verification' ?
		    	[{ text: _("Confirm"), id: "dialog_default", click: doClick},
			    { text: _("Repeat"), click: function() {
			    	requestBase(null,'What my verification code?');
			    	$( this ).dialog( "close" );
			    }},
			   { text: _("Cancel"), click: doCancel}]      
    		:
	        	[{ text: _("Ok"), id: "dialog_default", click: doClick},
	            { text: _("Cancel"), click: doCancel}]      
    });
    dialog.empty();
    if (!dialog_envents_bound) {
    	dialog_envents_bound = true;
	    dialog.keyup(function (e) {
	        if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
	            var button = $(this).parent().find("#dialog_default");
	            button.trigger("click");
	            return false;
	        }
	    });
	    dialog.submit( function(e) {
	    	e.preventDefault();//TODO://sure, need this?
	    	return false;
	    });
    }
    var src = '<form><fieldset>';
    for (var i=0;i<fields.length;i++) {
    	var name = fields[i];
    	var id = name_to_id(name);
    	var value = values && i < values.length && values[i] ? values[i] : '';
    	var hint = locale_map.hints[name]; if (!hint) hint = ''; 
    	var prompt = hint ? hint : prompts && i < prompts.length && prompts[i] ? _(prompts[i]) : _(name);
    	var type = is_secret(name) ? 'password' : 
    		//TODO: is boolean
    		is_boolean(name) ? 'checkbox' :
    		'text';
    	if (options && options[name]){
    		var o = options[name];
    		src = src.concat('<label class="dialog-label" for="').concat(id).concat('">')
    		.concat(capitalize(_(name))).concat('</label>')
    		.concat(create_selector(o.title,'dialog-input ui-widget-content ui-corner-all',id,value,o.options));
    	}else
    	if (readonly && (name == 'text' || name == 'source'))
        	src = src.concat('<label class="dialog-label" for="').concat(id).concat('">')
    		.concat(capitalize(_(name))).concat('</label>')
    		.concat('<textarea name="').concat(id).concat('" id="').concat(id)
    		.concat('" title="').concat(capitalize(hint))
    		.concat('" placeholder="').concat(capitalize(prompt)).concat('"')
    		.concat('" class="dialog-input ui-widget-content ui-corner-all" ')
    		.concat(' style="white-space: normal;"')
    		.concat(readonly ? 'readonly >' : '>')
    		.concat(value).concat('</textarea>');
    	else
    		src = src.concat('<label class="dialog-label" for="').concat(id).concat('">')
    		.concat(capitalize(_(name))).concat('</label>')
    		.concat('<input type="').concat(type).concat('" name="').concat(id).concat('" id="').concat(id)
    		.concat('" value="').concat(value)
    		.concat('" title="').concat(capitalize(hint))
    		.concat('" placeholder="').concat(capitalize(prompt)).concat('"')
    		.concat('" class="dialog-input ui-widget-content ui-corner-all" ')
    		.concat(is_boolean(name) && value == 'true' ? 'checked' : '')
    		.concat(readonly ? 'readonly >' : '>');
    }
    src += '</fieldset></form>';
    dialog.dialog('option', 'title', title);
    dialog.append(src);    
    dialog.dialog( "open" );
}

function dialog_retrieve(names) {
	var values = [];
	for (var i = 0; i < names.length; i++) {
		var value = is_boolean(names[i]) ? ($( '#'+name_to_id(names[i]) ).prop('checked') ? 'true' : 'false')
			: $( '#'+name_to_id(names[i]) ).val();
		values.push(value);
	}
	return values;
}

function dialog_update(string) {
	if (string) {
		var values = [];
		parseToGrid(values, string,dialog_fields,",");
		dialog_values = !AL.empty(values) ? values[0] : null; 
		if (dialog_values)
			for (var i = 0; i < dialog_values.length; i++) {
				if (is_boolean(dialog_fields[i]))
					$( '#'+name_to_id(dialog_fields[i]) ).attr('checked', dialog_values[i] == 'true' ? true : false);
				else
					$( '#'+name_to_id(dialog_fields[i]) ).val(dialog_values[i]);
			}
	}
}

var peer_self_properties = ['email','name','surname',
                       //'birth date',
                       //'secret question','secret answer',
                       //'facebook login',//'vkontakte login',
                       'check cycle', 'items limit', 'trusts limit', 'news limit', 'email notification', 'discourse id', 'steemit id', 'golos id', 'ethereum id'];
var peer_peer_properties = ['name','surname','email','facebook id','google id','reddit id','vkontakte id'];
var peer_peer_keys = ['name','surname','email']

function fields(names) {
	f = '';
	for (var i = 0; i < names.length; i++) {
		if (f.length > 0)
			f += ', ';
		f += names[i];
	}
	return f;
}

function your_properties() {
	//TODO: initialize this by request inside the dialog and not aoutside like this
	requestBase('#dialog','What my '+fields(peer_self_properties)+'?');
	dialog_open(_('Your properties'),'What is ...?',peer_self_properties,peer_self_properties,null,false,function(){
		var c = jqualifier(peer_self_properties,dialog_values);
		if (c && c.length > 0)
			requestBase(null,'my '+c+'.');
		return true;
	});
}


//--- PayPal Integration ---

var paypal_loaded = false;
var default_term = '????-??-??';
function subsciption_paypal_render(button_id,type){
	function payment(data, actions, type) {
    	var total = type == 'yearly' ? paypal_yearly_usd : paypal_monthly_usd;
    	var currency = 'USD';
    	if ($( "#currency" ).val() == 'RUB'){
    		total *= rate_usd_rub;
    		currency = 'RUB';
    	}
      // 2. Make a request to your server
      console.log('create-payment '+type+' '+total+' '+currency+' '+paypal_setup);
      console.log(data);
      return actions.request.post(base_url+'/paypal/create-payment/?total='+total+'&currency='+currency+'&type='+type)
        .then(function(res) {
          // 3. Return res.id from the response
          console.log(res);
          return res.id;
        });
    }
	function render(button_id,type){
	  paypal.Button.render({
		    env: paypal_setup, //'sandbox', // Or 'production'
		    // Set up the payment:
		    // 1. Add a payment callback
		    payment: type == 'yearly' ? function (data, actions){return payment(data, actions,'yearly')} 
		    		: function (data, actions){return payment(data, actions,'monthly')},
		    // Execute the payment:
		    // 1. Add an onAuthorize callback
		    onAuthorize: function(data, actions) {
		      // 2. Make a request to your server
		      console.log('execute-payment');
		      console.log(data);
		      return actions.request.post(base_url+'/paypal/execute-payment/?payment='+data.paymentID+'&payer='+data.payerID, {
		        paymentID: data.paymentID,
		        payerID:   data.payerID
		      })
		        .then(function(res) {
		        	console.log(res);
		        	var amount = res && res.transactions && res.transactions[0] && res.transactions[0].amount ? res.transactions[0].amount : null;
		        	// 3. Show the buyer a confirmation message.
		       		ajax_request('What my paid term?',function(response){
		       			var data= [];
		       			parseToGrid(data,response.substring(5),['paid term'],",");
		       			if (!AL.empty(data))
		       				$( "#paid_term" ).html(data[0][0]);
		       		},true);//silent	    
		        	if (amount)
			        	talks_say_in("Payment id "+res.id+", "+amount.total+amount.currency+" "+res.transactions[0].description+" "+res.update_time);
		        });
		    }
		  }, button_id);
	}
	if (paypal_loaded)
		render(button_id);
	else
		$.getScript("https://www.paypalobjects.com/api/checkout.js", function(data, textStatus, jqxhr) {
			paypal_loaded = true;
			render(button_id,type);
			console.log('Loaded PayPal checkout.');
		});
}
function subscription_open_dialog(term,currency,paypal_id) {
	var window_height = $( window ).height();
	var window_width = $( window ).width();
    var height = 540;
    var width = 400; 
    var left = 0;
    var top = 0;
    if (height > window_height)
    	height = window_height;
    else
    	top = (window_height - height)/2;
    if (width > window_width)
    	width = window_width;
    else
    	left = (window_width - width)/2;
    function doClick() {
    	var currency = $( "#currency" ).val();
   		ajax_request('My currency '+(currency && currency == 'RUB' ? 'rub' : 'usd'),null,true);
   		$( this ).dialog( "close" );
    }
    function doCancel() { $( this ).dialog( "close" );}
    var dialog = $( "#subscription_dialog" ).dialog({
    	autoOpen: false, 
    	height: height, 
    	width: width,
    	top: top,
      	left: left,
      	modal: true,
        buttons:[{ text: _("Ok"), id: "dialog_default", click: doClick},{text: _("Cancel"), click: doCancel}]
    });
    dialog.empty();
    if (!dialog_envents_bound) {
    	dialog_envents_bound = true;
	    dialog.keyup(function (e) {
	        if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
	            var button = $(this).parent().find("#dialog_default");
	            button.trigger("click");
	            return false;
	        }
	    });
	    dialog.submit( function(e) {
	    	e.preventDefault();//TODO://sure, need this?
	    	return false;
	    });
    }
    if (AL.empty(term))
    	term = default_term;
    dialog.dialog('option', 'title', _('Aigents Subscription'));
    
  if (!AL.empty(paypal_id)){
    var src = '<form><fieldset>'
    	+ '<span class="dialog-text" class="ui-widget-content"><b>'+_('Supported till')+' <span id="paid_term">'+term+'</span></b></span>'
    	+ '<span class="dialog-text" class="ui-widget-content">'+_('Extend your support for')+'<img src="/ui/img/aigent32left.png"/></span>'
        + '<label class="dialog-label" for="paypal-button-monthly">1 '+_('month')+': '+paypal_monthly_usd+'USD:</label>'
		+ '<div class="dialog-input" id="paypal-button-monthly"></div>'
    	+ '<label class="dialog-label" for="paypal-button-yearly">1 '+_('year')+': '+paypal_yearly_usd+'USD:</label>'
		+ '<div class="dialog-input" id="paypal-button-yearly"></div>'
		+ '<label class="dialog-label" for="currency">'+_('Currency')+'</label>'
    	+ '<select class="dialog-input" id="currency"><option value="USD" selected="selected">USD</option><option value="RUB">RUB</option></select>'
    	+ '</fieldset></form>';
    dialog.append(src);
    dialog.dialog( "open" );
    subsciption_paypal_render('#paypal-button-monthly','monthly');
    subsciption_paypal_render('#paypal-button-yearly','yearly');
    $( "#currency" ).val(currency && currency.toUpperCase() == 'RUB' ? 'RUB' : 'USD');
  } else {
	//var src = '<span id="paypal_login_button"></span>';
	var src = '<div class="menu-button"><a href="/en/paypal.html" target="_self" style="text-decoration:none">'
    	+ '<span class="dialog-text" class="ui-widget-content" style="white-space:normal">'+_('You can have your account connected with PayPal in order to support us and let us to help you better')+'<img src="/ui/img/aigent32left.png"/></span>'
		+ '<img src="/ui/img/paypal-connect.png" title="Connect with us on Paypal" style="cursor:pointer"/></a></div>';
    dialog.append(src);
    dialog.dialog( "open" );
//TODO: make it working! Right now is reports JS errors due to apparent conflicts between JQuery ad PayPal JS  
	/*//$.getScript("https://www.paypalobjects.com/js/external/connect/api.js", function(data, textStatus, jqxhr) {
		//console.log('Loaded PayPal login.');
		paypal.use( ['login'], function (login) {
			  login.render ({
			    "appid":paypal_app_id,//"AX14cg6ozAF8xi5iNOGcbnwajvsCZ-uR1iZl_EvAMXjttWrQV7Buzp9tOX329J3qcvS0fIIRy9kl-kvr",
			    "authend":paypal_setup,//"production",
			    "scopes":"openid email profile https://uri.paypal.com/services/paypalattributes",
			    "containerid":"paypal_login_button",
			    "responseType":"code",
			    "locale":"en-us",
			    "buttonType":"CWP",
			    "buttonShape":"pill",
			    "buttonSize":"lg",
			    "fullPage":"true",
			    "returnurl":base_url+"/al/paypal"
			  });
			});
	//});*/
  }
}

function subscription_open(){
	var term = default_term;
	var currency = 'USD';
	var paypal_id = null;
 	ajax_request('What my currency, paid term, paypal id?',function(response){
		var data= [];
		parseToGrid(data,response.substring(5),['currency','paid term','paypal id'],",");
		if (!AL.empty(data)){
			currency = data[0][0];
			term = data[0][1];
			paypal_id = data[0][2];
		}
		subscription_open_dialog(term,currency,paypal_id);
    },true);//silent	    
}


//--- Things ---
var things_data = [];

function things_refresh() {
	selected_thing_index = null;
	requestBase("#things_list","What my topics name, trust, relevance, positive, negative?",true);
	$('#things_input').val('');
}

function add_thing() {
	var text = $('#things_input').val();
	if (text && text.length > 0) {
		$('#things_input').val('');
		text = quote(AL.toString(text,null));
		requestBase('#things_list',"My topics "+text+', trusts '+text+'.',false,things_refresh);
	}else{
		dialog_open(_('Topic'),null,['name'],null,[''],false, function(){
			var text = $( '#'+name_to_id( 'name' ) ).val();
			if (!AL.empty(text)){
				text = quote(AL.toString(text,null));
				requestBase('#things_list','My topics '+text+', trusts '+text+'.',false,things_refresh);
			}
			return true;
		});
	}
}

function edit_trust(title,list,verb,item,callback) {
	//var oldval = $(item).text();
	//var oldchk = $(item).children(".trust").prop('checked');
	var index = $(item)[0].id;
	var oldval = menu_data[index][0];
	var oldchk = menu_data[index][1];
	dialog_open(_(title),null,["name"],null,[oldval],false,function(){
		var newval = $( '#'+name_to_id( 'name' ) ).val();
		var oldstr = AL.toString(oldval,"name");
		var newstr = AL.toString(newval,"name");
		requestBase(list,'My '+verb+' not '+oldstr+', '+newstr+'.'+
			' Name '+newstr+' trust '+(oldchk ? 'true' : 'false')+'.');
		$(item).children().children().children().children(".name").text(newval);
		menu_data[index][0] = newval;
		if (callback)
			callback(newval);
		trusts_sort(list);
		return true;
	});
}

function edit_thing(item) {
	edit_trust('Topic','#things_list','topics',item);
}

//http://stackoverflow.com/questions/3744289/jquery-how-to-select-an-option-by-its-text
function del_thing() {
	if (selected_things) {
		var q = '';
		var ignores = '';
		var ids = [];
		selected_things.forEach(function(value) {
			ids.push(value[0].id);
			value.remove();//removing from the list directly, no refresh is needed
			var name = $(value).text();
			if (q.length > 0){
				q += ', ';
				ignores += ', ';
			}
			var thing = quote(AL.toString(name,"name"));
			q += 'no ' + thing;
			ignores += thing;
		});
		//q = 'My topics ' + q + '.';
		q = 'My topics ' + q + ', ignores ' + ignores + '.';
		selected_things = [];
		requestBase('#things_list',q);
		selected_thing_index = null;//suppress edit on next click
		//removing from the list directly, no refresh is needed
		ids.sort(function (a,b) {return b-a;});
		for (var i = 0; i < ids.length; i++)
			things_data.splice(ids[i],1);
		//refresh in-place
		trusts_init("#things_list",things_data,$("#things_input").val());
	}
}

function things_update(string) {
	trusts_update('#things_list',string,things_data);
}


//--- Sites ---
var sites_data = [];

function sites_refresh() {
	selected_site_index = null;
	requestBase("#sites_list","What my sites name, trust, relevance, positive, negative?",true);
	$('#sites_input').val('');
}

function add_site() {
	var text = $('#sites_input').val();
	if (text && text.length > 0) {
		$('#sites_input').val('');
		text = AL.toString(text,null);
		requestBase('#sites_list','My sites '+text+', trusts '+text+'.',false,sites_refresh);
	}else{
		dialog_open(_('Site'),null,['name'],null,[''],false, function(){
			var text = $( '#'+name_to_id( 'name' ) ).val();
			if (!AL.empty(text)){
				text = AL.toString(text,null);
				requestBase('#sites_list','My sites '+text+', trusts '+text+'.',false,sites_refresh);
			}
			return true;
		});
	}
}

function edit_site(item) {
	edit_trust('Site','#sites_list','sites',item,function(newval){
		$(item).children(".name").attr('href',newval);
	});
}

function del_site() {	
	if (selected_sites) {
		var ids = [];
		var q = '';
		var ignores = '';
		selected_sites.forEach(function(value) {
			ids.push(value[0].id);
			//var name = $(value).text();
			var name = sites_data[value[0].id][0];
			if (q.length > 0){
				q += ', ';
				ignores += ', ';
			}
			var site = AL.toString(name,"name");
			q += 'no ' + site;
			ignores += site;
			value.remove();//removing from the list directly, no refresh is needed
		});
		//q = 'My sites ' + q + '.';
		q = 'My sites ' + q + ', ignores ' + ignores + '.';
		selected_sites = [];
		requestBase('#sites_list',q);
		selected_sites_index = null;//suppress edit on next click
		//removing from the list directly, no refresh is needed
		ids.sort(function (a,b) {return b-a;});
		for (var i = 0; i < ids.length; i++)
			sites_data.splice(ids[i],1);
		//refresh in-place
		trusts_init("#sites_list",sites_data,$("#sites_input").val());
	}
}

function read_site(value) {
	var lower = value.toLowerCase();
	if (lower.indexOf("http://") == 0 || lower.indexOf("https://") == 0) {
		window.open(value,'_blank');
		requestBase(null,"You read site \'"+value+"\'!");
	}
}

function open_site() {
	if (selected_sites)
		selected_sites.forEach(function(value) {
			read_site(value.text());
		});
}

function sites_update(string) {
	trusts_update('#sites_list',string,sites_data);
}

//--- News --- 
var news_data = [];
var news_keys = ["sources", "text", "times"];
var news_names = ["relevance", "social relevance", "sources", "text", "times", "trust", "image", "is", "sentiment"];

var loading_bits = 0;
function loading(start,bit){
	if (start){
		loading_bits = loading_bits | bit;
		$('#loading').css('display','inline');
	}else{
		loading_bits = loading_bits & ~bit;
		if (loading_bits == 0)
			$('#loading').css('display','none');
	}
}

function news_refresh(filter) {
	loading(true,1);
	requestBase("#news_list","What new true sources, text, times, trust, relevance, social relevance, image, is, sentiment?",true,
			function(){loading(false,1)},function(){loading(false,1)});
	$('#news_input').val(filter ? filter : '');
}

var news_sorting_mode = 0;
function news_sort(selector) {
    $(selector).children("li").sort(function(a, b) {
    	return news_data_sort(news_data[a.id],news_data[b.id])
    }).appendTo(selector);
}

function news_data_sort(a,b){
	if (news_sorting_mode == 1){//by relevance
		if (a[0] != b[0])
			return a[0] > b[0] ? -1 : 1;
		if (a[1] != b[1])
			return a[1] > b[1] ? -1 : 1;
	}
	if (a[5] != b[5])
		return b[5] ? -1 : 1;//by trust
	var cmp = AL.datetimecmp(a[4],b[4]);//by time
	if (cmp != 0)
		return cmp;
	cmp = strcmp(a[3],b[3]);//by text
	if (cmp != 0)
		return cmp;
	return strcmp(a[2],b[2]);//by source
}

//on every new action, reset timeout so refresh_delay_millis counts from the last one!? 
var news_refresh_delayed_time = 0;
function news_refresh_thought() {
	requestBase(null,"You think!",true,function(){
		news_refresh($("#news_input").val());//preserving current filter
		news_refresh_delayed_time = 0;
	});
}
function news_refresh_delayed() {
	function act(){
		var time = new Date().getTime();
		var sinceLastChange = time - news_refresh_delayed_time;//local activity
		var sinceLastEvent = idleTime * 1000;//global activity
		var delta = Math.min(sinceLastChange, sinceLastEvent);
		if (delta < refresh_delay_millis){//if too recent, restart
			setTimeout(act,refresh_delay_millis - delta);
			return;
		}
		news_refresh_thought();
	}
	if (news_refresh_delayed_time == 0)//if not started, triger
		setTimeout(act,refresh_delay_millis);
	news_refresh_delayed_time = new Date().getTime();//remember the latest beginning
}

function news_menu(event) {
	event.preventDefault();
	if (menu)
		hide_menu();
	menu = $( "#news_menu" );
	function refresh_selection() {
		selected_news = []; $( ".ui-selected", $('#news_list') ).each(function() {
			if ($(this)[0].id)
				selected_news.push( $(this) );
		});
	}
	menu.mouseleave(function (){ hide_menu();});
	if (!$(this).hasClass("ui-selected"))//make others unselected
		$(".ui-selected").removeClass("ui-selected");
	$(this).addClass("ui-selected");//make this selected
	var element = $(this);
	var index = $(this)[0].id;
	var data = news_data[index];
	var check = $("#news_menu_check");
	var checked = data[5];
	check.html(checked? _("Uncheck") : _("Check"));
	check.off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	var checked_news = []; 
    	$( ".ui-selected", $('#news_list') ).each(function() {
    		if ($(this)[0].id) {
    			checked_news.push( $(this)[0].id );
    			$(this).find(".news_bar").find(".news_check").prop('checked',!checked);
    		}
    	});
    	news_check_many("#news_list",checked_news,!checked);
		news_refresh_delayed();
    });
	$("#news_menu_open").html(_('Open'));
	$("#news_menu_open").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
        edit_news(element,index);
        //TODO: add site view to open edit for news and sites
		//open_news();
	});
	$("#news_menu_delete").html(_('Hide')); 
	$("#news_menu_delete").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
        refresh_selection();
		del_news();
	});
	var sorting_label = news_sorting_mode == 0 ? _("Sort by relevance") : _("Sort by time");
	$("#news_menu_sort").html(sorting_label);
	$("#news_menu_sort").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
		news_sorting_mode = news_sorting_mode == 0 ? 1 : 0;
    	news_init('#news_list',	news_data, $("#news_input").val());
	});
	$("#news_menu_graph_news").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	graph_news(news_data);
	});
	$("#news_menu_graph_topics").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	graph_topics(news_data);
	});
	$("#news_menu_graph_words").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	graph_words(news_data);
	});
	show_menu(event);
}

function news_check(list,index,checked){
	news_data[index][5] = checked;
	news_update_pending();
	var q = qualifier(news_keys,[news_data[index][2],news_data[index][3],news_data[index][4]]) + ' trust ' + (checked ? 'true' : 'false') + '.';
	requestBase(null,q);
	news_sort(list);
}

function news_check_many(list,indexes,checked){
	if (AL.empty(indexes))
		return;
	var cmd = '';
	for (var i = 0; i < indexes.length; i++){
		if (cmd.length > 0)
			cmd += ' ';
		var index = indexes[i];
		news_data[index][5] = checked;
		var q = qualifier(news_keys,[news_data[index][2],news_data[index][3],news_data[index][4]]) + ' trust ' + (checked ? 'true' : 'false') + '.';
		cmd += q;
	}
	news_update_pending();
	requestBase(null,cmd);
	news_sort(list);
}

function contains_insensitive(str,patlower){
	if (!patlower)
		return true;
	if (!str)
		return false;
	return str.toLowerCase().indexOf(patlower) != -1;
}

///////////////////////////////////////TODO: move this out: 1) to separate file, 2) to backend using webmine classifier 
function get_expression(text, words) {
	var findings = 0;
	for (var i = 0; i < words.length; i++){
		var w = words[i];
		for (var index = 0;;){
			var found = text.indexOf(w, index);
			if (found == -1)
				break;
			index = found + 1;
			findings++;
		}
	}
	return findings;
}

function news_init(list,data,filter) {
	for (i = 0; i < data.length; i ++){
		data[i][0] = AL.toNumber(data[i][0]);
		data[i][1] = AL.toNumber(data[i][1]);
	}

	news_data.sort(news_data_sort);//TODO:remove sort from other places?
	$(list).empty();
	filter = AL.empty(filter)? null : filter.toLowerCase();
	for (i = 0; i < data.length; i ++) {
		var relevance = data[i][0];
		var social_relevance = data[i][1];
		var sources = data[i][2];
		var text = data[i][3]; if (!AL.empty(text)) text = encode_urls(text); else text = '';
		var times = data[i][4];
		var trust = data[i][5];
		var image = data[i][6];
		var sentiment = data[i][8];
		if (filter && !(contains_insensitive(sources,filter) || contains_insensitive(text,filter) || contains_insensitive(times,filter)))
			continue;
		check = $('<input class="news_check" type="checkbox" '+(trust ? 'checked' : '')+'/>');
		check.change(function(eventObject) {
			var checked = this.checked;
			//var parent = $(this).parent().parent().parent().parent();//div -> div -> div -> li (without sentiment)
			var parent = $(this).parent().parent().parent().parent().parent().parent();//div -> div -> div -> div -> div -> li (with sentiment)
			var index = parent[0].id;
			news_check(list,index,checked);
			news_refresh_delayed();
		});
		var del_button = $('<button type="button" style="z-index:1;float:right;display:inline-block;height:20;width:20;" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only ui-dialog-titlebar-close" role="button" title="'+_("trusts_menu_delete","title")+'"><span class="ui-button-icon-primary ui-icon ui-icon-closethick"></span></button>');
		del_button.click(function(event) {
			var parent = $(this).parent();
			var id = parent[0].id;
	    	event.stopPropagation();
	    	selected_news = [parent];
	    	del_news(list);
 		})
		var sources_data = sources; 
		var sources_html = '';
		if (!Array.isArray(sources_data)) 
			sources_html = '<div><a class="news_source" href="'+sources_data+'" target="_blank">'+sources_data+'</a></div>';
		else
			for (var j = 0; j < sources_data.length; j++)
				sources_html += '<div><a class="news_source" href="'+sources_data[j]+'" target="_blank">'+sources_data[j]+'</a></div>';

		var positive = sentiment && sentiment > 0 ? +sentiment : 0;
		var negative = sentiment && sentiment < 0 ? -sentiment : 0;

		//relevance 		= i * 100 / (data.length - 1);
		//social_relevance 	= i * 100 / (data.length - 1);
		//positive 			= i * 100 / (data.length - 1);
		//negative 			= i * 100 / (data.length - 1);
		
		var personal_social = relevance + social_relevance;
		var personal_social_positive = relevance + social_relevance + positive;  
		var all_relevances = relevance + social_relevance + positive + negative;//full bar size
		var personal_p = personal_social == 0 			? 0 : Math.round(100 * relevance / personal_social); 
		var social_p   = personal_social_positive == 0 	? 0 : Math.round(100 * personal_social / personal_social_positive); 
		var positive_p = all_relevances == 0 			? 0 : Math.round(100 * personal_social_positive / all_relevances); 
		var negative_p = Math.round(all_relevances / 4); 
//console.log(personal_p+" "+social_p+" "+positive_p+" "+negative_p);
		
		var date = $('<div class="news_date">').append(_(times)).append('</div><br>');
		var check_and_date = $('<div>').append(check).append(date).append("</div>");
		var news_bar = $('<div class="news_bar" style="overflow:visible;background-color:lightblue;height:1.2em;width:'+personal_p+'%;">')
			.append(check_and_date)
			.append('</div>');
		var social_bar = $('<div class="news_bar" style="overflow:visible;background-color:wheat;height:1.2em;width:'+social_p+'%;">')
			.append(news_bar)
			.append('</div>');
		var positive_bar = $('<div class="news_bar" style="overflow:visible;background-color:lightgreen;height:1.2em;width:'+positive_p+'%;">')
		.append(social_bar)
		.append('</div>');
		var negative_bar = $('<div class="news_bar" style="background-color:lightpink;height:1.2em;width:'+negative_p+'%;">')
		.append(positive_bar)
		.append('</div>');
		

		//TODO: if authorized (not public)
		//trying to cleanup and attach image
		if (image){
			if (image.indexOf(" ") != -1)
				image = null;//cleanup junk
			else {
				var lower = image.toLowerCase();
				if (!lower.startsWith("https://")){ //pipe through aigents or discard
					image = lower.startsWith("http://") ? base_url + "/?u="+image : null;
				}
			}
		}
		
		var textsource = image ?
			$('<div><img style="margin-top:5;float:left" src="'+image+'" height="64"/>' +
					'<div style="display:inline-block;margin-left:5;">'
						+'<div class="news_text" style="height:auto;width:auto;">'
							+text
						+'</div>'
						+sources_html
			+'</div>') : 
			$('<div class="news_text">'+text+'</div>'+sources_html);
		
		var news_row = $('<li id='+i+' class="ui-widget-content">')
		.append(del_button)
		//.append(social_bar)
		.append(negative_bar)
		.append(textsource)
		.append('</li>')
		.appendTo(list);		
		news_row.contextmenu(news_menu);
		news_row.on("taphold",news_menu);
		
	}
    $(".news_source a").click(function(){
    	//TODO:there is no way to position opened site in broswer based on text patter (like in mobile), yuck!
    	//var text = $( this ).parent().parent().parent().children('.news_text').text();
    	var opened = window.open($(this).attr('href'));
    	return false;
    });
}

var news_edit_labels = ["text","source","time"];
var news_edit_keys = ["text", "sources", "times"];
function add_news() {
	var text = $('#news_input').val();
	if (!AL.empty(text)) {
		$('#news_input').val('');
		var url = extract_url(text);
		text = AL.toString(text,null);
		if (AL.empty(url))
			url = site_url;
		url = AL.toString(url,'sources');
		var cmd = "There text "+text+', sources '+url+', times today, new true, trust true update.';
		requestBase('#news_list',cmd,false,function(){news_refresh()});
	}else{
		var values = ['','','today'];
		dialog_open(_('News'),null,news_edit_labels,news_edit_labels,values,false, function(){
			var c = jqualifier(news_edit_labels,values,false,news_edit_keys);
			if (!AL.empty(c)){
				var cmd = "There times today, "+c+", new true, trust true update.";
				requestBase('#news_list',cmd,false,function(){news_refresh()});
			}
			return true;
		});
	}
}

function edit_news(item,index) {
	dialog_open(_('News'),null,news_edit_labels,null,
		[news_data[index][3],news_data[index][2],_(news_data[index][4])],
		true, null);
}

function del_news() {
	if (selected_news) {
		var ids = [];
		var request = '';
		selected_news.forEach(function(value) {
			var index = value[0].id;
			if (index != "" && index >= 0 && index < news_data.length) {
				ids.push(index);
				var q = qualifier(news_keys,[news_data[index][2],news_data[index][3],news_data[index][4]]) + ' new false, trust false.';
				if (request.length > 0)
					request+=' ';
				request += q;
			}
		});
		selected_news.forEach(function(value) {//separate loop, to keep index-based query building
			value.remove();//removing from the list directly, no refresh is needed
		});
		selected_news = [];
		selected_news_index = null;//suppress edit on next click
		requestBase('#news_list',request);//will refresh upon async completion
		ids.sort(function (a,b) {return b-a;});
		for (var i = 0; i < ids.length; i++)
			news_data.splice(ids[i],1);
		//refresh in-place
		news_init('#news_list',	news_data, $("#news_input").val());
		news_update_pending();
	}
}

function open_news() {
	if (selected_news)
		selected_news.forEach(function(value) {
			window.open(value.children(".news_source").text(),'_blank');
		});
}

function news_update(string) {
	news_data = [];
	if (!string) 
		$('#news_list').empty();
	else {		
		parseToGrid(news_data,string,news_names,",");//[""sources", "text", "times","trust"];
		news_data.sort(news_data_sort);
		news_init('#news_list',	news_data, $("#news_input").val());
	}
	news_update_pending();
}

function news_update_pending() {
    var pending = 0;
	for (var i = 0; i < news_data.length; i++)
		if (!news_data[i][5])
			pending++;
	displayPending(pending);
}

//-- Peers --
var peers_data = [];

function jqualifier(names,oldvalues,lower,keys) {
	var newnames = [];
	var newvalues = [];
	for (var i = 0; i < names.length; i++) {
		var value = is_boolean(names[i]) ? ($( '#'+name_to_id(names[i]) ).prop('checked') ? 'true' : 'false')
			: $( '#'+name_to_id(names[i]) ).val();
		if (oldvalues && i < oldvalues.length && oldvalues[i] == value)
			continue; //skip duplicates
		newnames.push(keys ? keys[i] : names[i]);//if keys provided, use keys instead of names
		newvalues.push(value);
	}
	if (lower)
		for (var i = 0; i < newvalues.length; i++)
			newvalues[i] = newvalues[i].toLowerCase();
	return qualifier(newnames,newvalues,', ');
}

function peer_split_lower(text) {
	var values = text.toLowerCase().split(/[ \t\r\n,;]+/);
	var email = null;
	var name = null;
	var surname = null;
	for (var i = 0; i < values.length; i++) {
		if (!email && isEmail(values[i]))
			email = values[i];
		else 
		if (!name)
			name = values[i];
		else
		if (!surname) 
			surname = values[i];
		else
		if (!email)
			email = values[i];
	}
	return [name,surname,email];
}

function peer_qualifier_elem(elem) {
	var index = $(elem)[0].id;
	var peer = peer_obj(peers_data,index,false);
	//var q = qualifier(peer_peer_properties,[peer.name,peer.surname,peer.email,peer.facebook,peer.google,peer.vkontakte]);
	var q = qualifier(peer_peer_keys,[peer.name,peer.surname,peer.email]);//because other is obfuscated because of privacy issues!?
	return !AL.empty(q) ? 'is peer, ' + q : q;
}

function peer_qualifier_text(text) {
	var split = peer_split_lower(text);
	return split && split.length == 3 ? qualifier(peer_peer_keys,split) : null;
}

//building name, which can be empty if social id is present only
function peer_screen_name(peer,html) {
	var str = peer.fullname ? (html ? ('<span style="display:inline-block;">' + peer.fullname + '</span>') : peer.fullname) : '';
	var eml = peer.email && isEmail(peer.email)
			? (display_emails
					? (html ? ('<a style="display:inline-block;" href=mailto:'+peer.email+'>'+peer.email+'</a>') : peer.email) //if can display emails anyway
					: str.length == 0 ? (html ? ('<span style="display:inline-block;">' + peer.email.split(/@/)[0] + '</span>') : peer.email.split(/@/)[0]) : '' ) //if no name and can't display emails
			: '';
	if (str.length == 0)
		console.log(peer);
	str = (eml.length > 0 && str.length > 0) ? str + ' ' + eml : str.length > 0 ? str : eml.length > 0 ? eml : 'Anonymous';
	return str;
}

function peer_obj(data,i,capital){
	var peer = [];
	var item = data[i];
	peer.name = capital ? capitalize(item[1]) : item[1];
	peer.surname = capital ? capitalize(item[2]) : item[2];
	if (item[0] && item[0].indexOf("@vk.com") == -1 && item[0].indexOf("@facebook.com") == -1 && item[0].indexOf("@google.com") == -1 && item[0].indexOf("@reddit.com") == -1)
		peer.email = item[0];
	peer.share = item[3] ? true : false;
	peer.facebook = item[4];
	peer.vkontakte = item[5];
	peer.google = item[6];
	peer.reddit = item[7];
	peer.editable = !peer.facebook && !peer.google && !peer.vkontakte && !peer.reddit;
	peer.relevance = item[8];
	peer.trust = item[9] ? true : false;
	//building name, which can be empty if social id is present only
	peer.fullname = !AL.empty(peer.name) ? peer.name : null;		
	if (!AL.empty(peer.surname))
		peer.fullname =peer.fullname ? peer.fullname + ' ' + peer.surname : peer.surname;
	return peer;
}

function peers_init(list,data,filter) {//email,name,surname,trust
	$(list).empty();
	filter = AL.empty(filter)? null : filter.toLowerCase();
	function match(string, pattern){
		var match = !AL.empty(string) && string.toLowerCase().indexOf(pattern) != -1 ? true : false;
		return match;
	}
	for (i = 0; i < data.length; i ++) {
		var item = data[i];
		if (!AL.empty(filter) && !match(item[0],filter) && !match(item[1],filter) && !match(item[2],filter))
			continue;
		var peer = peer_obj(data,i,true);

		var str = peer_screen_name(peer,true);//true HTML 
		
		//TODO: get rid of fixed numbers for height
		var height = 'height:35px;line-height:35px;';
		
		var but = '<div style="float:right;display:inline-block;'+height+'">';
		if (peer.facebook)
			but += '<img src="/ui/img/fb_logo.png" width="32" height="32"/>';
		if (peer.google)
			but += '<img src="/ui/img/google_icon.png" width="32" height="32"/>';
		if (peer.vkontakte)
			but += '<a href="https://vk.com/search?q='+(peer.fullname ? peer.fullname : '')+'" target="_blank"><img src="/ui/img/vk_logo.png" width="34" height="34"/></a>';
		if (peer.reddit)
			but += '<a href="https://www.reddit.com/user/'+peer.reddit+'" target="_blank"><img src="/ui/img/reddit.png" width="32" height="32"/></a>';
		but += '</div>';
		
		var html = $('<div style="display:inline-block;'+height+'">'+str+'</div>');
		var trust = $('<input class="trust" style="display:inline-block;vertical-align:middle;" type="checkbox"/ '+ (peer.trust?'checked':'') +'>');
		var share = $('<input class="share" style="display:inline-block;vertical-align:middle;" type="checkbox"/ '+ (peer.share?'checked':'') +'>');
		trust.change(function(eventObject) {
			var elem = $(this).parent().parent();
			var id = elem[0].id;
			data[id][8] = this.checked;
			var q = peer_qualifier_elem(elem)+ ' trust ' + (this.checked ? 'true' : 'false') + '.';
			requestBase(null,q);
			peers_sort(list);
		});
		share.change(function(eventObject) {
			var elem = $(this).parent().parent();
			var id = elem[0].id;
			data[id][3] = this.checked;
			var q = peer_qualifier_elem(elem)+ ' share ' + (this.checked ? 'true' : 'false') + '.';
			requestBase(null,q);
			peers_sort(list);
		});

		var relevance = $('<div style="display:inline-block;overfow:visible;background-color:lightblue;width:'+(3*peer.relevance/4)+'%;"/>')
			.append(trust)
			.append(share)
			.append(html);

		var buttons = $(but);
		//TODO: fix for firefox
		//this works for chrome
		//var del_button = $('<button type="button" style="margin-top:6px;margin-left:3px;float:right;display:inline-block;height:20;width:20;" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only ui-dialog-titlebar-close" role="button" title="'+_("trusts_menu_delete","title")+'"><span class="ui-button-icon-primary ui-icon ui-icon-closethick"></span></button>');
		var del_button = $('<button type="button" style="vertical-align:top;margin-top:6px;margin-left:3px;height:20;width:20;" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only ui-dialog-titlebar-close" role="button" title="'+_("trusts_menu_delete","title")+'"><span class="ui-button-icon-primary ui-icon ui-icon-closethick"></span></button>');
		del_button.click(function(event) {
			var parent = $(this).parent().parent();
			var id = parent[0].id;
	    	event.stopPropagation();
	    	del_trusts(list,parent);
 		})
		buttons.append(del_button);
		
		var peers_row = $('<li id='+i+' class="ui-widget-content" style="height:35;position:relative;">')
			.append(buttons)
			.append(relevance)
			.append('</li>').appendTo(list);
		
		/*
		//new_peers, with relevance and friendiness
		var tsize = 18;
		var vsize = 42+12;
		framing_bar = $('<div style="background-color:blue;height:15;width:'+(600-(i*50)-50)+';">')
		.append('</div>');
		framing_bar2 = $('<div style="background-color:lightblue;height:15;width:'+(700-(i*60))+';">')
		.append(check).append(str)
		.append('</div>');
		framing_bar3 = $('<div style="background-color:violet;height:15;width:'+(400-(i*50)-50)+';">')
		.append('</div>');
		$('<li id='+i+' class="ui-widget-content" style="height:50">')
		.append(framing_bar)
		.append(framing_bar2)
		.append(framing_bar3)
		.append('</li>').appendTo(list);
		*/
		init_menu(list,data,3);
		peers_row.contextmenu(peers_menu);
		peers_row.on("taphold",peers_menu);
	}
	//TODO: sort data items not list items!?
	peers_sort(list);
}

//TODO: move to Schema?
function is_boolean(name) {
	return name == 'email notification';	
}

var secret_properties = ['password','secret answer'];
function is_secret(name) {
	//TODO: return security back? but then decide what to do with secret question!?
    //for (var i=0;i<secret_properties.length;i++)
    	//if (name.search(secret_properties[i]) != -1)
    		//return true;
	return false;
}

function peers_refresh() {
	requestBase("#peers_list","what is peer, friend true email, facebook id, vkontakte id, google id, reddit id, name, surname, share, relevance, trust?",true);
	$('#peers_input').val('');
}

function edit_peer(item) {
	var index = $(item)[0].id;
	var peer = peer_obj(peers_data,index,false);
	var q = qualifier(peer_peer_properties,[peer.name,peer.surname,peer.email,peer.facebook,peer.google,peer.reddit,peer.vkontakte]);
	var values = [peer.name,peer.surname,peer.email,peer.facebook,peer.google,peer.reddit,peer.vkontakte];
	requestBase(null,'what '+q+' is?',true,function(result){
		peer.editable= result == 'No right.' ? false: true;
		dialog_open(_('Peer'),null,peer_peer_properties,peer_peer_properties,values,!peer.editable,function(){
			var c = jqualifier(peer_peer_properties,values);
			if (c && c.length > 0)
				requestBase('#peers_list','is peer, '+q+' '+c+'.');
			return true;
		});
	},null);
	
}

function add_or_link_peer(qualifier) {
	requestBase('#peers_list','is peer and '+qualifier+' friend true, trust true.',false,function(result){
		if ('Ok.' != result)
			requestBase('#peers_list',"There is peer and friend true and trust true and "+qualifier+'.',false,peers_refresh);
	});
}

function add_peer() {
	text = $('#peers_input').val();
	if (text && text.length > 0) {
		var qualifier = peer_qualifier_text(text);//lowercased
		if (!qualifier) 
			setTimeout(function() { displayStatus(_('proper_peer')); },500);
		else {
			$('#peers_input').val('');
			add_or_link_peer(qualifier);
		}
	}else{
		var values = ['','','','','',''];
		dialog_open(_('Peer'),null,peer_peer_properties,peer_peer_properties,values,false,function(){
			var c = jqualifier(peer_peer_properties,values,true);//lowercased
			if (c && c.length > 0)
				add_or_link_peer(c);
			return true;
		});
	}
}

function open_peer() {
	alert("Open peer!");
	//if (selected_peer)
		//edit_peer(selected_peer[0]);
}

function del_peer() {
	if (selected_peers) {
		var request = '';
		selected_peers.forEach(function(value) {
			var q = peer_qualifier_elem(value) + ' friend false, trust false, share false.';
			if (request.length > 0)
				request+=' ';
			request += q;
		});
		selected_peers.forEach(function(value) {
			value.remove();//removing from the list directly, no refresh is needed
		});
		selected_peers = [];
		selected_peer_index = null;
		requestBase('#peers_list',request);//will refresh upon async completion
	}
}

function peers_update(string) {
	if (!string) 
		$('#peers_list').empty();
	else {
		parseToGrid(peers_data,string,["email", "name", "surname", "share", "facebook id", "vkontakte id", "google id", "reddit id", "relevance", "trust"],",");
		peers_data.sort(function(a,b){
			for (var n = 0; n < 3; n ++) {
				var cmp;
				i = n == 0 ? 1 : n == 1 ? 2 : 0; //fix order to name,surname,email
				if (a.length > i && b.length > i && (cmp = strcmp(a[i],b[i])) != 0)
					return cmp;
			}
			return 0;
		});
		peers_init('#peers_list',peers_data,$("#peers_input").val());	
	}
}

//defined in aigents-map.js
var map_initialized = false;
function graph_login(){
	map_initialized = false;
}
function graph_logout(){
	map_initialized = false;
	if (graph_demo)
		graph_demo();
}
function graph_refresh(event) {
	map_key = '';
	if (!map_initialized && map_init){
		map_initialized = true;
		map_init(logged_in);
	}
}
function graph_filter(map_key){
	if (map_filter)
		map_filter(map_key);
}

function talks_menu(event) {
	event.preventDefault();
	if (menu)
		hide_menu();
	menu = $( "#talks_menu" );
	menu.mouseleave(function (){ hide_menu();});
	var element = event.target;
	var text = $(event.target).text();
	if ($(event.target).hasClass("log-out") && !AL.empty(text)){
		$("#talks_menu_repeat").show();
		$("#talks_menu_repeat").html(_('Repeat'));
		$("#talks_menu_repeat").off().click(function(event){
	    	event.stopPropagation();
	    	hide_menu();
	    	talks_say_out_internal(text);
		});
	} else
		$("#talks_menu_repeat").hide();
	//TODO:clean hack below
	if ($(event.target).hasClass("log-in") && !AL.empty(text)){
		$("#talks_menu_graph").show();
		$("#talks_menu_graph").html(_('Graph'));
		$("#talks_menu_graph").off().click(function(event){
	    	event.stopPropagation();
	    	hide_menu();
	    	//TODO: graph!!!
	    	console.log(text);
	    	graph_text(text);
		});
	} else
		$("#talks_menu_graph").hide();
	//TODO:clean hack above
	$("#talks_menu_clear").html(_('Clear')); 
	$("#talks_menu_clear").off().click(function(event){
    	event.stopPropagation();
    	hide_menu();
    	talks_clear();
	});
	show_menu(event);
}

function talks_scroll() {
	$('#talks_log').scrollTop($('#talks_log')[0].scrollHeight);
	$('#talks_log').contextmenu(talks_menu);
	$('#talks_log').on("taphold",talks_menu);
}

function talks_say_out(text) {
	text = capitalize(text);
	displayAction(text);
	$("#talks_log").append('<div class="log-out ui-widget ui-widget-content ui-corner-all">'+text+'</div>');
	talks_scroll();
}

function talks_say_in(text) {
	if (text.substr(0,6) == "<html>") {
		var title = AL.parseBetween(text,"<title>","</title>",true);
		var body = AL.parseBetween(text,"<body>","</body>",true);
		if (!title)
			title = "Aigents Search Report";
		popUpReport(title,body,true);//true - use jquery
		text = "Ok.";//return;
	}
	displayStatus(text);
	$("#talks_log").append('<div class="log-in ui-widget ui-widget-content ui-corner-all">'+encode_urls(text)+'</div>');
	talks_scroll();
}

//http://stackoverflow.com/questions/1005676/urls-and-plus-signs
//http://stackoverflow.com/questions/75980/best-practice-escape-or-encodeuri-encodeuricomponent
function uri_encode(text) {
	var tmp = replace_all( encodeURI(text), "+", "%2B" );
	return replace_all( tmp, "#", "%23" );
}

//http://api.jquery.com/jquery.ajax/
//http://stackoverflow.com/questions/23607901/cross-origin-request-blocked-on
//http://stackoverflow.com/questions/24516280/where-is-the-correct-place-to-enable-cors/24529954#24529954
//http://stackoverflow.com/questions/5750696/how-to-get-a-cross-origin-resource-sharing-cors-post-request-working
//http://www.w3.org/TR/cors/
function ajax_request(text,callback,silent,onerror) {
	ajax_request_uri_method(base_url,callback,silent,'POST',uri_encode( text ));
}
function ajax_request_uri_method(uri,callback,silent,method,data,onerror) {
	$.ajax({
		//type: 'GET', cache: false, crossDomain : true, url: uri,
		type: method,
		data: data,
		cache: false, 
		crossDomain : true, 
		url: uri,
		dataType : 'text',
		xhrFields: { withCredentials: true },
		timeout: timeout_millis ? timeout_millis : 0,
		//context: document.body
		success: function(message, textStatus, jqXHR) { 
			console.log("response:"+message);
			var parts = message.split("\n\n");
			for (var p in parts){
				var data = parts[p];
				if (callback)
					callback(data);
				else
					response(data,silent); 
			}
		},
		error: function(jqXHR, textStatus, errorThrown) {
			message = "error: "+textStatus+": "+(errorThrown ? errorThrown : "possibly no connection")+"."; 
			console.log(message);
			if (onerror)
				onerror(message);
		},
		beforeSend: function(xhr){ 
			xhr.withCredentials = true; 
			//xhr.setRequestHeader("Origin","aigents");//Safari:Refused to set unsafe header "Origin"
		}
	});
}

function talks_say() {
	text = $('#talks_input').val();
	if (text && text.length > 0) {
		$('#talks_input').val('');
		talks_say_out_internal(text);
	}
}

function talks_say_out_internal(text) {
	talks_say_out(text);
	ajax_request(text);
	if (AL.match(text,'my logout')){
		logout("#facebook_logo");
		logout("#google_logo");
		logout("#vkontakte_logo");
		logout("#reddit_logo");
		logout("#paypal_logo");
		logout("#twitter_logo");
		logoutlowlevel();
	}
}

var popupMargin = 30;
//http://stackoverflow.com/questions/2109205/open-window-in-javascript-with-html-inserted
//http://jqueryui.com/dialog/#default
function requestReport(provider,name){
	var title = _("Aigents Report for")+" "+name;
	var height = $( window ).height() - popupMargin;
	var width = $( window ).width() - popupMargin;
    var dialog = $( '#report_dialog' ).dialog({   	 
    	height: height, width: width,
    	top: 15, left: 15,
    	autoOpen: false, modal: true
    });
    dialog.empty();
    if (!dialog_envents_bound) {
    	dialog_envents_bound = true;
	    dialog.keyup(function (e) {
	        if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
	            $(this).parent().find("button:eq(1)").trigger("click");
	            return false;
	        }
	    });
    }
    dialog.dialog('option', 'title', title);
    var requst_text = "my "+provider.substring(1)+" report";
    var percentage = 0;
    function request_report(request){
     	ajax_request(request,function(response){
      		if (response.indexOf('Your report is being prepared') == 0){
         		dialog.html(_(response) + percentage + '%');
         		percentage = Math.round( percentage + ((100 - percentage) / 10) );
      			if (percentage < 100){
	      			setTimeout(function(){request_report(requst_text)},10000);
      			}
  				//TODO:else - cancel form?
      		}
      		else
         		dialog.html(response);
         },true);//silent	    
    }
    request_report(requst_text);
    dialog.dialog( "open" );
	dialog.html(_("Loading..."));    
}

//TODO: reuse this in requestReport above
function renderReport(title,html){
	var height = $( window ).height() - popupMargin;
	var width = $( window ).width() - popupMargin;
    var dialog = $( '#report_dialog' ).dialog({   	 
    	height: height, width: width,
    	top: 15, left: 15,
    	autoOpen: false, modal: true
    });
    dialog.empty();
    if (!dialog_envents_bound) {
    	dialog_envents_bound = true;
	    dialog.keyup(function (e) {
	        if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
	            $(this).parent().find("button:eq(1)").trigger("click");
	            return false;
	        }
	    });
    }
    dialog.dialog('option', 'title', title);
    dialog.dialog( "open" );
    return dialog;
}

var popupWindow = null;
function popUpReport(title,html,jquery){
	if (jquery){
		//this fits the standard styles BUT makes in not printable AND breaks styles of "x" buttons in lists 
		renderReport(title).html(html);
	}else {
		//http://jennifermadden.com/javascript/window3.html
		//this make it printable but need to arrange the styles and default window size 
		var o = $(document.body).offset();
		var specs = "top="+($(window).scrollTop() + o.top + popupMargin/2)+",left="+($(window).scrollLeft() + o.left + popupMargin/2)+",width="+($(window).width() - popupMargin)+",height="+($(window).height() - popupMargin)+",scrollbars=1,resizable=1";
		popupWindow = window.open("",title,specs,true)
		popupWindow.document.open()
		popupWindow.document.write(html)
		popupWindow.document.close()
	}
}

function update_redirecting_logins(paypal,reddit,twitter,reddit_image,twitter_image){
	if (!AL.empty(paypal))
		login("#paypal_logo","/ui/img/paypal_icon_no_border_grayed.png");
	if (!AL.empty(reddit))
		login("#reddit_logo",reddit_image ? reddit_image : "/ui/img/reddit_grayed.png");
	if (!AL.empty(twitter))
		login("#twitter_logo",twitter_image ? twitter_image : "/ui/img/twitter_logo_grayed.png");
}
//var is_root = false;
function loginlowlevel(name,surname,no_refresh){
	logged_in = true;
	auto_refreshing = true;
	graph_login();
	news_open();
	if (name || surname){
		name = !name ? surname : !surname ? name : name + ' ' + surname;
		setCookie('username',name);
		document.getElementById("user").innerHTML = name;
	}
	document.getElementById("aigents_logo").src = '/ui/img/aigent32left.png';
	ajax_request('my language '+get_language(),function(){},true);//silent
	if (!no_refresh)//if no need to refresh redirectig logins 
     	ajax_request('What my paypal id, reddit id, twitter id, reddit image, twitter image?',function(response){
			var data= [];
			parseToGrid(data,response.substring(5),['reddit id','paypal id','twitter id', 'reddit image', 'twitter image'],",");
			if (!AL.empty(data))
				update_redirecting_logins(data[0][0],data[0][1],data[0][2],data[0][3],data[0][4]);
	     	//ajax_request('What your trusts?',function(response){
	     	//	is_root = response.startsWith('My trusts ');
	     	//},true);
        },true);//silent	    
}
function logoutlowlevel(){
	deleteCookie('username');
	logged_in = false;
	auto_refreshing = false;
	graph_logout();
	document.getElementById("user").innerHTML = _('user','innerHTML');
	document.getElementById("aigents_logo").src = '/ui/img/aigent32.png';
}

function login_menu(provider,name){
	hide_menu();
	var logo = $(provider+"_logo").get(0);
	if (logo.logged || (provider == '#aigents' && logged_in)){
		menu = $( provider+"_menu" );
		menu.menu();
		menu.mouseleave(function (){ 
			hide_menu();
		});
		var container = $(provider);
		var container_position = container.offset();
		var cw = container.width();
		var mw = menu.width();
		var menu_left = provider == '#paypal' 
			? container_position.left-menu.width()+container.width()
			: container_position.left-menu.width()/2+container.width()/2;
		var menu_top = container_position.top+container.height();
		menu.css("position","absolute");
		menu.css("display","inline");
		menu.offset({top:menu_top,left:menu_left});
	    $(provider+"_logout").off().click(function(event){
	    	event.stopPropagation();
			logoutlowlevel();
	    	talks_say_out_internal(capitalize('my logout'));
	    	hide_menu();
	    });
	    $(provider+"_report").off().click(function(){
	    	requestReport(provider,name);
	    	hide_menu();
	    });
	    $(provider+"_subscription").off().click(function(){
	    	subscription_open();
	    	hide_menu();
	    });
	    if (provider == '#aigents')
	    	$("#aigents_profile").off().click(function(){
	    		hide_menu();
	    		your_properties();
	    	});
		return true;
	}
	return false;
}

function login(provider_logo,url){
	var e = $(provider_logo).get(0);
	e.logged = true;
	if (!e.backup_src)
		e.backup_src = $(provider_logo).get(0).src;
	$(provider_logo).attr("src", url);
}

function logout(provider_logo){
	var e = $(provider_logo).get(0);
	e.logged = false;
	$(provider_logo).attr("src", $(provider_logo).get(0).backup_src );
}

function talks_clear() {
	$("#talks_log").empty();
}

function displayStatus(text) {
	if (AL.empty(text)) {
		$('#status').hide();
	} else {
		if (document.getElementById('status') && !document.getElementById('status').disabled){
			$('#status').text(text);
			$('#status').show();
		}
	}
}

var time_to_action_hide = 0;
function displayAction(text) {
	if (AL.empty(text)) {
		$('#action').hide();
	} else {
		if (!document.getElementById('action') || document.getElementById('action').disabled)
			return;
		function displayActionCount(){
			time_to_action_hide--;
			if (time_to_action_hide <= 0)
				displayAction(null);
			else
				setTimeout(displayActionCount,1000);
		}
		if (time_to_action_hide == 0){
			time_to_action_hide = action_seconds;//start count down
			displayActionCount();
		} else
			time_to_action_hide = action_seconds;//start over count down
		$('#action').text(text);
		$('#action').show();
	}
}

function displayPending(count) {
	if (!count || count < 1) {
		$('#count').hide();
		document.title = APPNAME;
	} else {
		$('#count').text(count);
		$('#count').show();
		document.title = APPNAME + ' (' + count +')';
	}
}

function listener(event) {
	if (event.origin.indexOf(base_url)!=-1) {
		if (event.data.indexOf('vkontakte') != -1)
			window.vkontakteLoginComplete();
	}
}

//https://learn.javascript.ru/cookie#функция-setcookie-name-value-options
function getCookie(name) {
	  var matches = document.cookie.match(new RegExp(
	    "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
	  ));
	  return matches ? decodeURIComponent(matches[1]) : null;
}
function deleteCookie(name) {
	  setCookie(name, "", {
	    expires: -1
	  })
}
function setCookie(name, value, options) {
	  options = options || {};
	  var expires = options.expires;
	  if (typeof expires == "number" && expires) {
	    var d = new Date();
	    d.setTime(d.getTime() + expires * 1000);
	    expires = options.expires = d;
	  }
	  if (expires && expires.toUTCString) {
	    options.expires = expires.toUTCString();
	  }
	  value = encodeURIComponent(value);
	  var updatedCookie = name + "=" + value;
	  for (var propName in options) {
	    updatedCookie += "; " + propName;
	    var propValue = options[propName];
	    if (propValue !== true) {
	      updatedCookie += "=" + propValue;
	    }
	  }
	  document.cookie = updatedCookie;
}

function init() {
	//redirect any request to proper http/https domain 
	if (window.location.host.search('sigents.com') != -1 || 
		window.location.host.search('cigents.com') != -1 ||
		window.location.host.search('aigents.org') != -1 ||
		window.location.host.search('www.aigents.com') != -1 ||
		base_url.search(window.location.protocol) == -1) {
		//var area = window.location.hash.substring(1);
		//window.location.replace(site_url + !AL.empty(area) ? '/#' + area : '' );
		window.location.replace(site_url);
	}
	
	//attach events to catch facebook registration
	if (window.addEventListener)
		 window.addEventListener("message", listener);
	else
		 window.attachEvent("onmessage", listener);// IE8
	
	var lang = get_language();
	var area = window.location.hash.substring(1);
	console.log('lang='+lang+', hash='+area);

	//setup voice recognition for Chrome only and not Opera, see agent_speech.js
	if (/Chrome/.test(navigator.userAgent) && /Google Inc/.test(navigator.vendor) 
			&& !(/OPR/.test(navigator.userAgent) || /Opera/.test(navigator.userAgent)) && SpeechRecognition)
		if (document.getElementById('microphone') != null)
			document.getElementById('microphone').style.display = 'inline-block';
	
	//reset widgets
	displayStatus(null);
	displayAction(null);//TODO: remove legacy?
	displayPending(0);
	
	//check if user is logged in already
	var current_user = getCookie('username');
	if (current_user){// user user set by cookie
		loginlowlevel(current_user);
		post_init();
	}else{//check if user is known by server after redirect with cookie kept in browser
		requestBase(null,'What my name, surname, login time, paypal id, reddit id, twitter id, reddit image, twitter image?',true,function(reply){
			var data= [];
			parseToGrid(data,reply.substring(5),['login time','name','surname','paypal id','reddit id','twitter id','reddit image','twitter image'],",");
			if (!AL.empty(data)){//get user upon redirect and use tis context
				loginlowlevel(capitalize(data[0][1]),capitalize(data[0][2]),true);
				update_redirecting_logins(data[0][3],data[0][4],data[0][5],data[0][6],data[0][7]);
				post_init();
			} else {//create new context
				ajax_request('my language '+lang,function(){
					//initialize area by hash (anchor) if supplied
					if (!AL.empty(area)){
						requestBase(null,"My areas "+area+".",false,function(){//not silent
							news_open();
							post_init();
						});
					}else
						post_init();
				},true);//silent
			}
		},null);
	}
}

function post_init(){
	//VK: http://vk.com/dev/openapi
	//VK.init({ apiId: 4965500 });//Aigents Web
	window.vkontakteLogin = function(){    	
    	//https://vk.com/dev/permissions
    	//},0+2+8192+65536+4194304);//wall,friends,offline,email);	
		//var scope_mask = 0+2+8192+65536;//wall,friends,offline - email is not working yet
		//var scope_mask = 0+2+8192;//wall,friends//- offline needs special request with redirect
		var scope_mask = 0+2+65536;//friends,offline - wall is dropped silently by server auth scheme
		//var scope_string = 'friends,offline'; // 'friends,wall,offline'

		console.log("VKontakte doing login");
    	//http://vk.com/dev/openapi_auth
    	//http://vk.com/dev/permissions
    	VK.Auth.login(function authInfo(response) {
        	console.log("VKontakte response:"+JSON.stringify(response));
    		if (response.session) {
    			var uid = response.session.mid;
    			var sid = response.session.sid;
    	    	console.log("VKontakte getting info");
    			VK.Api.call('users.get', {user_ids: uid, v:'5.71', fields: ['photo_50','email'], https: '1'}, function(r) { 
        	    	console.log("VKontakte user info:"+JSON.stringify(r));
    				if(r.response && r.response.length > 0 && r.response[0].photo_50) { 
    					console.log('VKontakte user login '+uid+' '+r.response[0].first_name+' '+
    						r.response[0].last_name+' '+r.response[0].photo_50);     					

    					var frame = document.getElementById('vkontakte_frame');
    					if (frame){
    						//https://vk.com/dev/auth_sites
    						//server-side token production by client code
    						//var request = "https://oauth.vk.com/authorize?client_id=5212732&display=popup&redirect_uri="+base_url+"/?access_token&scope=wall,friends,offline,&response_type=token&v=5.45&state=sid";
    						var redirect_uri = base_url+"/%3Fvkontakte_id="+uid;
    						var request = "https://oauth.vk.com/authorize?client_id=5212732&display=popup"+
    							"&redirect_uri="+redirect_uri+"&scope="+scope_mask+"&response_type=code&v=5.45"+
    							"&state="+redirect_uri;
    		    	    	console.log("VKontakte server auth:"+request);
    						frame.src = request;
    						window.vkontakteLoginComplete = function(){    							
        		    	    	console.log("VKontakte server auth completed");
        		    	    	talks_say_in("ВКонтакте!");//TODO: what?
    	    					login("#vkontakte_logo",r.response[0].photo_50);
    	    					loginlowlevel(r.response[0].first_name,r.response[0].last_name);
    							auto_refresh();
    							frame.src = base_url+'/?';//to refresh next time
    						};
    					}else{
    						//client-side token validatoion on server
        			        //https://toster.ru/q/78024
        			        //https://vk.com/dev/secure
        			        //https://vk.com/dev/auth_server
        			    	var request = "my vkontakte id "+uid+", vkontakte token '"+sid+"'.";
        			    	console.log("VKontakte+ : "+request);
        			    	ajax_request(request,function(response){
        				    	console.log('VKontakte+ logged in: '+response);
        			    		if (response.indexOf("Ok.") == 0) {
        			    			talks_say_in(response);
        	    					login("#vkontakte_logo",r.response[0].photo_50);
        	    					loginlowlevel(r.response[0].first_name,r.response[0].last_name);
        							auto_refresh();
        			    		}
        				    });	    
    					}
    				}
    			});
    		} else {
    	    	console.log("NOT VKontakte");
    		}
    	},scope_mask);
	}
	var vkontakteAutoLogs = 0;
	function vkontakteAutoLogin(){ 
		if (VK != null){
			console.log("VKontakte checking");
		    VK.Auth.getLoginStatus(function(status){
		    	console.log("VKontakte status:"+JSON.stringify(status));
		    	if (status.status == "connected"){
		    		window.vkontakteLogin();
		    	}
		    });
		}else 
		if (vkontakteAutoLogs < 60){
			vkontakteAutoLogs++;
			setTimeout(vkontakteAutoLogin,1000);
		}
	}
	
	//FB
	window.facebookStatusChangeCallback = function(response) {
	    console.log('Facebook:');
	    console.log(response);
	    if (response.status === 'connected') {// Logged into your app and Facebook.
	    	console.log(response.authResponse);
	        var id = response.authResponse.userID;
	        var token = response.authResponse.accessToken;
	        FB.api('/me', function(response) {
	            console.log('Successful login for: ' + response.name + " ("+response.email+")");
	            var email = response.email ? "email "+response.email+", " : "";//optional
		    	ajax_request("my "+email+" facebook id "+id+", facebook token '"+token+"'.",
			    		function(response){
			    			console.log(response);
		    				if (response.indexOf("Ok.") == 0) {
		    					talks_say_in(response);
		    					login("#facebook_logo","https://graph.facebook.com/"+id+"/picture");
		    					loginlowlevel(parseBetween(response,'Hello ','!'));
		    					auto_refresh();
		    				}
			    		});
	          });
	    } else if (response.status === 'not_authorized') {// The person is logged into Facebook, but not your app.
	    	console.log('Not authorized by Facebook');
	    } else {// The person is not logged into Facebook
	    	console.log('Not logged to Facebook');
	    }
	}            
	window.fbAsyncInit = function() {
		FB.init({
		    appId      : '763733953664689',//Aigents Web
		    cookie     : true,  // enable cookies to allow the server to access the session
		    xfbml      : true,  // parse social plugins on this page
		    version    : 'v2.2' // use version 2.2
		  });
		  FB.getLoginStatus(function(response) {
		    window.facebookStatusChangeCallback(response);
		  });
	};
	// Load the Facebook SDK asynchronously
	(function(d, s, id) {
		var js, fjs = d.getElementsByTagName(s)[0];
	    if (d.getElementById(id)) return;
	    js = d.createElement(s); js.id = id;
	    js.src = "//connect.facebook.net/en_US/sdk.js";
	    fjs.parentNode.insertBefore(js, fjs);
	}(document, 'script', 'facebook-jssdk'));

	//Google
	function getGoogleUser(response){
		function primary(a){
			for (var i = 0; i < a.length; i++)
				if (a[i].metadata.primary)
					return a[i];
		}
		var user = {};
		var result = response.result;
		var profile = primary(result.names);
		user.name = profile.displayName;
		user.id = profile.metadata.source.id;
		user.photo = primary(result.photos).url;
        //user.email = primary(result.emailAddresses).value;
		return user;
	}
	window.loginGoogleApi = function(){
		auth2.grantOfflineAccess().then(function (result){
			console.log("Google grantOfflineAccess: "+JSON.stringify(result));
			var result_code = result.code;
			if (gapi.client.people){
				gapi.client.people.people.get({
	           			'resourceName': 'people/me',
	           			'personFields': 'names,photos' //,emailAddresses'
	         		}).then(function(response) {
	           		console.log(response);
	           		var user = getGoogleUser(response);
					console.log(user.id+'/'+user.name+'/'+user.photo);
			    	var request = "my google id "+user.id+", google token '"+result_code+"'.";
			    	console.log("Google : "+request);
			    	ajax_request(request,function(response){
				    	console.log('Google logged in: '+response);
			    		if (response.indexOf("Ok.") == 0) {
			    			talks_say_in(response);
	    					var realname = parseBetween(response,'Hello ','!');
							loginlowlevel(realname ? realname : user.name);
							auto_refresh();
							login("#google_logo",user.photo);
			    		}
				    });
	         	});
			}
		});//auth2.grantOfflineAccess
	}
	window.loginGoogleApiAuto = function(){
		setTimeout(function(){//hack - wait for access to People API granted
			if (gapi.client.people){
				gapi.client.people.people.get({
	           			'resourceName': 'people/me',
	           			'personFields': 'names,photos' //,emailAddresses'
	         		}).then(function(response) {
	           		console.log('Google auto login response '+response);
	           		var user = getGoogleUser(response);
					ajax_request('What my google id?',function(response){
						var data = [];
						parseToGrid(data,response.substring(5),['google id'],",");
						if (!AL.empty(data) && user.id == data[0][0])
							login("#google_logo",user.photo);
					},true);//silent	    
			    });	    
         	}
		},1000);//hack - wait for accss to People API granted
	}

        //init login processes and news refresh incrementally with delays
        setTimeout(vkontakteAutoLogin,6000);
        setTimeout(function(){
                if (!logged_in)
                        news_refresh();
                else
                        auto_refresh();
        },9000);
}//post_init()


//talking
var APPNAME = 'Aigents';
var COPYRIGHT = 'Copyright ©';
var requestors = [];
var requestorTypes = {
	'#things_list': 'topics',
	'#sites_list' : 'sites',
	'#dialog' : ''};
var requestorUpdaters = {
	'#things_list': things_update,
	'#sites_list' : sites_update,
	'#news_list': news_update,
	'#peers_list' : peers_update,
	'#dialog' : dialog_update };
var requestorRefreshers = {
	//'#news_list': news_refresh,
	'#peers_list' : peers_refresh};

function requestBase(requestorHash,requestText,silent,callback,onerror) {
	console.log(requestorHash+":"+requestText);
	if (!silent)
		talks_say_out(requestText);
	ajax_request(requestText,function(response){ 
		if (requestorHash || !callback)
			responseRequestor(requestorHash,silent,response);
		if (callback)
			callback(response);
	},silent,onerror);
}

//TODO: get rid of this in favor of sync responseRequestor callback!!!
function response(message,silent) {
	var requestorHash = null;
	if (requestors.length > 0) {
		var option = requestors.shift();
		requestorHash = option.hash;
		silent = option.silent;
	}
	responseRequestor(requestorHash,silent,message);
}

function responseRequestor(requestorHash,silent,message) {
	if (message.indexOf(APPNAME) == 0 && message.indexOf(COPYRIGHT) != -1)
		message = message.substring(message.indexOf('\n')+1);
	if (message.indexOf("Ok.") == 0) {
		if (!silent)
			talks_say_in(message);
		if (!requestorHash && message.indexOf("Hello ") != -1) { // on login
			loginlowlevel(parseBetween(message,'Hello ','!'));
			auto_refresh();
			$('#tabs').tabs({ active: 2 });//by default, go to news
		}
		else
		if (requestorHash) { //on success, refresh requestor
			var refresher = requestorRefreshers[requestorHash];
			if (refresher)
				refresher();
		}
	} else 
	if (message.indexOf("What your ") == 0) {
		if (message.indexOf("email") != -1){
			logoutlowlevel();
		}
		talks_say_in(message);
		ask(message.substring("What your ".length));
	} else 
	if (requestorHash != null) { // logged in
		if (!silent)
			talks_say_in(message);
		var pattern = "Your "+requestorTypes[requestorHash];
		if (message.indexOf(pattern) == 0) {
			requestorUpdaters[requestorHash](message.substring(pattern.length));
		}
		else
		if (message.indexOf("There ") == 0) {
			requestorUpdaters[requestorHash](message.substring("There ".length));
		}
		else
		if (message.indexOf("Your not") == 0 || message.indexOf("No thing") == 0) {
			requestorUpdaters[requestorHash](null);
		}
		else
		if (message.indexOf("Ok.") == 0) { //if taken, repeat refresh //TODO: purge because done above?
			//TODO:
			//request(requestor,requestor.updateRequest());
		}
		else {
			var error = errorMessage(message);
			if (error && error.length > 0)
				talks_say_in(error);
			else
				//TODO: what if is that?
				;//talks_say_in(null);
				//requestor.update(message);
		}
	} else
	if (message.indexOf("Search ") == 0) {
		if (!silent)
			talks_say_in(message);
		if (message == "Search working."){
  			setTimeout(request_search,10000);
		}
	}else{
		if (!silent)
			talks_say_in(message);
	}
}

function request_search() {
 	ajax_request("Search results",function(response){
  		if (response == 'Search busy.')
  			setTimeout(request_search,10000);
  		else
  			talks_say_in(response);
    },true);//silent	    
}

