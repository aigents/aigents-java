<?php
/*
 * MIT License
 * 
 * Copyright (c) 2014-2019 by Anton Kolonin, Aigents
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

include_once("test_api.php");


function test_bard() {
	global $version;
	global $copyright;

	test_init();

	//login, registration, verification
	say("My name a, email a, surname a, birth date a.");
	get("What your secret question, secret answer?");
	say("My secret question a, secret answer a.");
	get("What your a?");
	say("My a a.");
	get("Ok. Hello A A!\nMy Aigents ".$version.$copyright);
	
	say("My topics catheters.");
	get("Ok.");
	say("Catheters has type, brand, ways, size, quantity, order_number, part_number, fr, length, diameter, tip.");
	get("Ok.");
	//http://www.bardmedical.com/products/urological-drainage/foley-catheters/bardex%C2%AE-ic-infection-control-foley-catheters/
	//Foley Catheters, BARDEX® I.C., 2-way	0165SI12	Balloon 5cc, 12FR	12/case
	say("Catheters patterns '\$type catheters \$brand , \$ways \$part_number \$size \$quantity'.");
	get("Ok.");
	say("Type is word.");
	get("Ok.");
	say("Ways is '/^[0-9]{1}\-way$/'.");
	get("Ok.");
	say("Quantity is '/^[0-9]{2}\/case$/'.");
	get("Ok.");
	
	//https://www.cookmedical.com/products/uro_023_webds/
	//G14597 023104 4.0 70 cm .028 inch    
	say("Catheters patterns '\$order_number \$part_number \$fr \$length cm \$diameter inch'.");
	get("Ok.");
	//https://www.cookmedical.com/products/di_cxi_webds/
	//G18370 CXI-2.3-14-65-ANG 2.3 .014 65 ANG	
	say("Catheters patterns '\$order_number \$part_number \$fr \$diameter \$length \$tip'.");
	get("Ok.");
	
	say("Order_number is '/^g[0-9]{5}$/'.");
	get("Ok.");
	say("Part_number is '/^[0-9a-z\.\-]{6,25}$/'.");
	get("Ok.");
	say("Fr is number.");
	get("Ok.");
	say("Length is number.");
	get("Ok.");
	say("Diameter is number.");
	get("Ok.");
	say("Tip is word.");
	get("Ok.");
	
	//http://www.bardmedical.com:products
	//http://www.bardmedical.com/products:Foley Catheters
	//http://www.bardmedical.com/products/urological-drainage/foley-catheters/:BARDEX® I.C. Infection Control Foley Catheters
	//http://www.bardmedical.com/products/urological-drainage/foley-catheters/bardex®-ic-infection-control-foley-catheters/	
	//http://www.bardmedical.com/products/urological-drainage/foley-catheters/bardex%C2%AE-ic-infection-control-foley-catheters/
	say("You reading catheters in 'Foley Catheters, BARDEX® I.C., 2-way	0165SI20	Balloon 5cc, 20FR	12/case	Yes'!");
	get("My reading catheters in 'Foley Catheters, BARDEX® I.C., 2-way	0165SI20	Balloon 5cc, 20FR	12/case	Yes'.");

	//https://www.cookmedical.com/search/#q=catheter&type=products
	//https://www.cookmedical.com/products/uro_bpc_webds/
	//https://www.cookmedical.com/products/uro_023_webds/
	//https://www.cookmedical.com/products/di_cxi_webds/
	say("You reading catheters in 'G14598 023105 5.0 70 cm .035 inch 145 cm Tapered    G18369 CXI-2.3-14-65-0 2.3 .014 65 straight    G14597 023104 4.0 70 cm .028 inch    G18370 CXI-2.3-14-65-ANG 2.3 .014 65 ANG'!");
	get("My reading catheters in G14598 023105 5.0 70 cm .035 inch 145 cm Tapered    G18369 CXI-2.3-14-65-0 2.3 .014 65 straight    G14597 023104 4.0 70 cm .028 inch    G18370 CXI-2.3-14-65-ANG 2.3 .014 65 ANG.");	
	say("What is catheters part_number?");
	get("There part_number 0165si20; part_number 023104; part_number 023105; part_number cxi-2.3-14-65-0; part_number cxi-2.3-14-65-ang.");	
	say("What is catheters?");
	get("There brand 'bardex® i.c .', is catheters, part_number 0165si20, quantity 12/case, size 'balloon 5cc , 20fr', text 'foley catheters bardex® i.c . , 2-way 0165si20 balloon 5cc , 20fr 12/case', times today, type foley, ways 2-way; diameter .014, fr 2.3, is catheters, length 65, order_number g18369, part_number cxi-2.3-14-65-0, text g18369 cxi-2.3-14-65-0 2.3 .014 65 straight, times today, tip straight; diameter .014, fr 2.3, is catheters, length 65, order_number g18370, part_number cxi-2.3-14-65-ang, text g18370 cxi-2.3-14-65-ang 2.3 .014 65 ang, times today, tip ang; diameter .028, fr 4.0, is catheters, length 70, order_number g14597, part_number 023104, text g14597 023104 4.0 70 cm .028 inch, times today; diameter .035, fr 5.0, is catheters, length 70, order_number g14598, part_number 023105, text g14598 023105 5.0 70 cm .035 inch, times today.");

	//http://www.bbraunusa.com/ : products
	//http://www.bbraunusa.com/products.html : catheters
	//http://www.bbraunusa.com/products.html?id=00020743040000000399 : catheters
	//http://www.bbraunusa.com/products.html?id=00020743040000000422 : catheter
	//http://www.bbraunusa.com/products.html?id=00020743040000000422&prid=PRID00001011	
	say("My topics introcan catheters.");
	get("Ok.");
	say("Introcan catheters has brand, ga, diameter, details, product_code.");
	get("Ok.");
	say("Introcan catheters patterns '\$brand catheter \$ga ga. x \$diameter in., \$details \$product_code'.");
	get("Ok.");
	say("Ga is number.");
	get("Ok.");
	say("Product_code is '/^[0-9]{7}-[0-9]{2}$/'.");
	get("Ok.");	
	say("You reading introcan catheters in 'Introcan Safety® IV Catheter 24 Ga. x 0.55 in., PUR, Straight, Notched Polyurethane catheter material, straight hub, notched needle 4251611-02'.");
	get("My reading introcan catheters in 'Introcan Safety® IV Catheter 24 Ga. x 0.55 in., PUR, Straight, Notched Polyurethane catheter material, straight hub, notched needle 4251611-02'.");
	say("What is introcan catheters?");
	get("There brand introcan safety® iv, details 'pur , straight , notched polyurethane catheter material , straight hub , notched needle', diameter 0.55, ga 24, is introcan catheters, product_code 4251611-02, text 'introcan safety® iv catheter 24 ga . x 0.55 in . , pur , straight , notched polyurethane catheter material , straight hub , notched needle 4251611-02', times today.");
	
	//http://www.bostonscientific.com/en-US/products/catheters--guide/convey-guiding-catheter.html
	//Convey Guiding Catheter
	//Unique hydrophilic coating
	//Large 6 F 0.071" inner-diameter
	//Small atraumatic soft tip
	//Ultra-thin 1 × 2 flat wire braid pattern		

	//http://www.bostonscientific.com/en-US/products/catheters--balloon/maverick-ptca-balloon-catheters.html
	//Maverick Over-The-Wire Balloon Catheter
	//diameters include: 4.0, 4.5, 5.0, 5.5, and 6.0mm
	//150cm Shaft
	//Flexible TrakTip Design
	
	//http://www.bostonscientific.com/en-US/products/catheters--drainage/Flexima_and_Percuflex_Drainage_Catheters.html
	//Flexima™ and Percuflex™ Drainage Catheters
	//Glidex™ Hydrophilic Coating
	//R/O Marker, on the Flexima Biliary drainage
	//Flexima Material
	
	say("My topics boston catheters.");
	get("Ok.");
	say("Boston catheters has type, coating, inner-diameter.");
	get("Ok.");
	say("Coating is word.");
	get("Ok.");
	say("Inner-diameter is word.");
	get("Ok.");
	
	say("Boston catheters patterns '{[\$description catheter] [\$coating coating] [\$inner-diameter {diameter inner-diameter}] [\$tip tip] [\$pattern pattern]}'.");
	get("Ok.");
	
	say("You reading boston catheters in 'Convey Guiding Catheter. Unique hydrophilic coating. Large 6 F 0.071\" inner-diameter. Small atraumatic soft tip. Ultra-thin 1 × 2 flat wire braid pattern'!");
	get("My reading boston catheters in 'Convey Guiding Catheter. Unique hydrophilic coating. Large 6 F 0.071\" inner-diameter. Small atraumatic soft tip. Ultra-thin 1 × 2 flat wire braid pattern'.");
		
	say("What is boston catheters?");
	get("There coating pattern, description convey guiding, inner-diameter pattern, is boston catheters, pattern ultra-thin 1 × 2 flat wire braid, text soft tip ultra-thin 1 × 2 flat wire braid pattern, times today, tip soft.");
	
	//http://www.bostonscientific.com/en-US/products/dilatation/uromax-ultra.html
	//"sparse pattern" to capture information?
	//say("You reading site http://www.bostonscientific.com/!");
	//get("No.");
		
	//TODO: provide parsing example for it!
	//before, was prohibited by robots.txt
	say("You reading site http://www.teleflexmedicaloem.com/!");
	get("My reading site http://www.teleflexmedicaloem.com/.");

	//cleanup before site tests
	say("No there is catheters.");
	get("Ok.");
	say("No there is boston catheters.");
	get("Ok.");
	say("No there is introcan catheters.");
	get("Ok.");
	say("What is boston catheters?");
	get("No.");
	say("What is catheters?");
	get("No.");
	
	//TODO:
	//http://www.bostonscientific.com/ : products
	//http://www.bostonscientific.com/en-US/products.html : catheters:
	//http://www.bostonscientific.com/en-US/products/catheters--guide.html : catheter
	/*
	say("You reading site http://www.bostonscientific.com!");
	get("No.");
	say("My topics products.");
	get("Ok.");
	say("Catheters patterns catheter, catheters, 'catheters:'.");//patterns has been defined above
	get("Ok.");
	//Works too expansinve, don't try often!
	say("You reading site http://www.bostonscientific.com!");
	get("My reading site http://www.bostonscientific.com.");
	say("What sources http://www.bostonscientific.com?");
	get("There is products, sources http://www.bostonscientific.com, text products, times today.");
	say("What is boston catheters type, coating, inner-diameter?");
	//TODO:
	get("Ok.");
	*/
	
	//TODO: from site and from page
	//http://www.bardmedical.com:products
	//http://www.bardmedical.com/products:Foley Catheters
	//http://www.bardmedical.com/products/urological-drainage/foley-catheters/:BARDEX® I.C. Infection Control Foley Catheters
	//http://www.bardmedical.com/products/urological-drainage/foley-catheters/bardex®-ic-infection-control-foley-catheters/
	//http://www.bardmedical.com/products/urological-drainage/foley-catheters/bardex%C2%AE-ic-infection-control-foley-catheters/
	/*
	say("You reading site http://www.bardmedical.com!");
	get("My reading site http://www.bardmedical.com.");
	say("What is catheters diameter, fr, length, order_number, part_number?");
	//TODO:
	//works but does not parse quite right, retry later
	get("Ok.");
	*/
	
	//TODO: from site and from page
	//http://www.bbraunusa.com/ : products
	//http://www.bbraunusa.com/products.html : catheters
	//http://www.bbraunusa.com/products.html?id=00020743040000000399 : catheters
	//http://www.bbraunusa.com/products.html?id=00020743040000000422 : catheter
	//http://www.bbraunusa.com/products.html?id=00020743040000000422&prid=PRID00001011
	//brk();	
	say("You reading introcan catheters in http://aigents.com/test/cathtest.html!");
	get("My reading introcan catheters in http://aigents.com/test/cathtest.html.");
	say("What is http://aigents.com/test/cathtest.html text?");
	get();
	say("What is introcan catheters brand, ga, diameter, details, product_code?");
//TODO: get rid of leading period in '. introcan safety® iv' 	
	//get("There brand introcan safety® iv, details 'pur , straight , notched polyurethane catheter material , straight hub , notched needle', diameter 0.55, ga 24, product_code 4251611-02; brand introcan safety® iv, details 'pur , straight , thinwall polyurethane catheter material , straight hub , thinwall needle', diameter 0.55, ga 24, product_code 4251607-02.");
	get("There brand '. introcan safety® iv', details 'pur , straight , thinwall polyurethane catheter material , straight hub , thinwall needle', diameter 0.55, ga 24, product_code 4251607-02; brand introcan safety® iv, details 'pur , straight , notched polyurethane catheter material , straight hub , notched needle', diameter 0.55, ga 24, product_code 4251611-02.");
	say("No there is introcan catheters.");
	get("Ok.");
	say("What is introcan catheters?");
	get("No.");

	//TODO: can't follow 'http://www.bbraunusa.com/products.html?id=00020743040000000422&prid=PRID00001011' 
	// from http://www.bbraunusa.com/products.html?id=00020743040000000422
	//say("You reading site http://www.bbraunusa.com!");
	//get("My reading site http://www.bbraunusa.com.");
	
	//TODO: pattern matching breaks - loose pattern starts and does not end?
	//say("You reading site 'http://www.bbraunusa.com/products.html?id=00020743040000000422&prid=PRID00001011'!");
	//get("My reading site 'http://www.bbraunusa.com/products.html?id=00020743040000000422&prid=PRID00001011'.");
//brk();
		
	//TODO: fix it: early pattern matchign start makes later pattern completion fail (see "//it.next();//TODO: or not to do")  
	say("You reading introcan catheters in http://aigents.com/test/cathtestbbra.html!");
	//get("My reading introcan catheters in http://aigents.com/test/cathtestbbra.html.");
	get();
	say("What is introcan catheters brand, ga, diameter, details, product_code?");
	get();
	say("What is 'http://aigents.com/test/cathtestbbra.html' text?");
	get();
	
//brk();
	
	//https://www.cookmedical.com/
	//https://www.cookmedical.com/products/list/
	//https://www.cookmedical.com/search/#q=catheter&type=products
	//https://www.cookmedical.com/products/uro_bpc_webds/
	//https://www.cookmedical.com/products/uro_023_webds/
	//https://www.cookmedical.com/products/di_cxi_webds/
	say("You reading site https://www.cookmedical.com/!");
	get("No.");
	say("My topics products.");
	get("Ok.");
	say("You reading site https://www.cookmedical.com/!");
	get("My reading site https://www.cookmedical.com/.");
	say("What is boston catheters?");
	get("No.");
	say("What is catheters?");
	get("No.");
	say("My topics urology.");
	get("Ok.");
	say("My topics product list.");
	get("Ok.");
	/*
	//This works fine but too long!
	say("You reading site https://www.cookmedical.com/!");
	get("My reading site https://www.cookmedical.com/.");
	say("What is catheters part_number, fr, length, diameter?");
	get();
	say("What is catheters part_number?");
	get();
	say("What is catheters?");
	get();
	*/	
	
	test_summary();

}	
	
	
test_bard();
	
	
?>
		