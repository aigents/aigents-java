/*
 * MIT License
 * 
 * Copyright (c) 2005-2018 by Anton Kolonin, Aigents
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
package net.webstructor.util;

//TODO: sort out if used or cleanup!!!
/*
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
 
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import javax.mail.internet.MimeUtility;

public class Code {
	private static SecretKey key;
	
	public static byte[] e64(byte[] b) throws Exception {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    OutputStream b64os = MimeUtility.encode(baos, "base64");
	    b64os.write(b);
	    b64os.close();
	    return baos.toByteArray();
	}

	public static byte[] d64(byte[] b) throws Exception {
	    ByteArrayInputStream bais = new ByteArrayInputStream(b);
	    InputStream b64is = MimeUtility.decode(bais, "base64");
	    byte[] tmp = new byte[b.length];
	    int n = b64is.read(tmp);
	    byte[] res = new byte[n];
	    System.arraycopy(tmp, 0, res, 0, n);
	    return res;
	}
	
	public static String e(String s,String k) {
        try {
        	Cipher e = Cipher.getInstance("DES");
        	e.init(Cipher.ENCRYPT_MODE, key);
        	byte[] b = e.doFinal(s.getBytes());
        	return new String(e64(b));
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("No Such Algorithm:" + e.getMessage());
        }
        catch (NoSuchPaddingException e) {
            System.out.println("No Such Padding:" + e.getMessage());
        }
        catch (InvalidKeyException e) {
            System.out.println("Invalid Key:" + e.getMessage());
        }
        catch (Exception e) {
            System.out.println("Invalid Key:" + e.getMessage());
        }
        return null;
	}

	public static String d(String s,String k) {
        try {
        	Cipher d = Cipher.getInstance("DES");
	        d.init(Cipher.DECRYPT_MODE, key);
	        byte[] b = d.doFinal(d64(s.getBytes()));
	        return new String(b);
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("No Such Algorithm:" + e.getMessage());
        }
        catch (NoSuchPaddingException e) {
            System.out.println("No Such Padding:" + e.getMessage());
        }
        catch (InvalidKeyException e) {
            System.out.println("Invalid Key:" + e.getMessage());
        }
        catch (Exception e) {
            System.out.println("Invalid Key:" + e.getMessage());
        }
        return null;
	}
	
	public static void main(String[] args) {
		
		//http://examples.javacodegeeks.com/core-java/crypto/encrypt-decrypt-string-with-des/
        try {
	        key = KeyGenerator.getInstance("DES").generateKey();
	        String s1 = "Test";
	        String s2 = e(s1,null);
	        String s3 = d(s2,null);
	        System.out.println("k: " + key);
	        System.out.println("s1: " + s1);
	        System.out.println("s2: " + s2);
	        System.out.println("s3: " + s3);
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("No Such Algorithm:" + e.getMessage());
            return;
        }
        catch (Exception e) {
            System.out.println("Invalid Key:" + e.getMessage());
            return;
        }
	}
}
*/