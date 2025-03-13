package com.launcher;

import com.google.gson.Gson;
import java.io.FileReader;

public class ConfigLoader {
    public static ServerList loadServerConfigs(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            return new Gson().fromJson(reader, ServerList.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
