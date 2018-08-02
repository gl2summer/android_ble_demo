package com;

/**
 * Created by jhc on 2018/8/2.
 */

public class MyUtil {

    public static String toHexString(byte []data){
        String r = "";

        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            r += " " + hex.toUpperCase();
        }

        return r;
    }
}
