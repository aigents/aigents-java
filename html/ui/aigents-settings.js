var site_url = "https://aigents.com"; var base_url = "https://aigents.com/al";//new production setup
//var site_url = "http://localtest.com"; var base_url = "http://localtest.com:1180";//test setup

var animation_enabled = false;
var auto_refreshing = true;
var logged_in = false;
var logged_email = null;
var refresh_millis = 10*60*1000;//how often to refresh news count
var refresh_delay_millis = 5000;//how soon refresh the screen after re-think
var status_seconds = 20;//how long the status stays shown and how fast it is show on idle
var action_seconds = 5;//how long the action stays shown
var display_emails = false;//whether emails of friends should be displayed or not in the view
