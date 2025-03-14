package com.launcher;

import java.util.List;

public class ServerConfig {
    public String name;
    public String minecraft_version;
    public String fabric_version;
    public String forge_version; // новое поле для Forge
    public String download_link;
    public boolean allow_custom_mods;
    public List<ModConfig> mods;
}
