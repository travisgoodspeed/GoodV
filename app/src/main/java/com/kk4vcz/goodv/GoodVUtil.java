package com.kk4vcz.goodv;

public class GoodVUtil {

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
    public static String byteArrayToHexExceptFirst(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2 - 2);
        int i=0;
        for(byte b: a) {
            if(i==0)
                i+=1;
            else
                sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
