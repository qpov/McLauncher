package com.launcher;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class ConfigLoader {
    public static ServerList loadServerConfigs(String filePath) {
        try {
            Reader reader;
            if (filePath.startsWith("http")) {
                URL url = new URL(filePath);
                reader = new InputStreamReader(url.openStream());
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
