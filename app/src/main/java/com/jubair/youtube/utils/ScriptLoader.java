package com.jubair.youtube.utils;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ScriptLoader {
    public static String loadGoodTube(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            // assets ফোল্ডার থেকে goodtube.js রিড করা হচ্ছে
            InputStream is = context.getAssets().open("goodtube.js");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
                sb.append("\n");
            }
            br.close();
            
            // স্ক্রিপ্টের শেষে আমাদের কাস্টম কনফিগারেশন ইনজেক্ট করা
            sb.append("\n");
            sb.append("console.log('GoodTube Engine Loaded via Android Native!');");
            // মোবাইল মোড ফোর্স করা
            sb.append("document.body.classList.add('goodTube_mobile');");
            
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return "javascript:" + sb.toString();
    }
}
