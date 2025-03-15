package com.launcher;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

public class ConfigLoader {
    public static ServerList loadServerConfigs(String filePath) {
        try {
            Reader reader;
            if (filePath.startsWith("https")) {
                URL url = new URL(filePath);
                URLConnection conn = url.openConnection();
                // Увеличиваем таймаут до 10 секунд
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                reader = new InputStreamReader(conn.getInputStream());
            } else {
                reader = new FileReader(filePath);
            }
            return new Gson().fromJson(reader, ServerList.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
