<?php
/*
Copyright 2018 Anton Kolonin, Aigents Group

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

$postdata = '{"jsonrpc":"2.0","id":"25","method":"get_account_history","params": ["uralresp","-1","1000"]}';

$opts = array('http' =>
    array(
        'method'  => 'POST',
        'content' => $postdata
    )
);

$context  = stream_context_create($opts);

$result = file_get_contents('http://144.217.94.119:8090', false, $context);
file_put_contents('uralresp_raw.txt',$result);
$result = mb_convert_encoding($result, 'UTF-8');
file_put_contents('uralresp_utf8.txt',$result);
echo($result);

?>
