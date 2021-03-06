/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.zford.jobs.bukkit.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.zford.jobs.Jobs;
import me.zford.jobs.Location;
import me.zford.jobs.bukkit.JobsPlugin;
import me.zford.jobs.config.JobsConfiguration;
import me.zford.jobs.container.RestrictedArea;
import me.zford.jobs.container.Title;
import me.zford.jobs.dao.JobsDAOH2;
import me.zford.jobs.dao.JobsDAOMySQL;
import me.zford.jobs.dao.JobsDAOSQLite;
import me.zford.jobs.util.ChatColor;
import me.zford.jobs.util.FileDownloader;

public class BukkitJobsConfiguration extends JobsConfiguration {
    private JobsPlugin plugin;
    public BukkitJobsConfiguration(JobsPlugin plugin) {
        super();
        this.plugin = plugin;
    }
    
    @Override    
    public synchronized void reload() {
        // general settings
        loadGeneralSettings();
        // title settings
        loadTitleSettings();
        // restricted areas
        loadRestrictedAreaSettings();
    }

    /**
     * Method to load the general configuration
     * 
     * loads from Jobs/generalConfig.yml
     */
    private synchronized void loadGeneralSettings(){
        File f = new File(plugin.getDataFolder(), "generalConfig.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
        
        CommentedYamlConfiguration writer = new CommentedYamlConfiguration();
        StringBuilder header = new StringBuilder();
        header.append("General configuration.");
        header.append(System.getProperty("line.separator"));
        header.append("  The general configuration for the jobs plugin mostly includes how often the plugin");
        header.append(System.getProperty("line.separator"));
        header.append("saves user data (when the user is in the game), the storage method, whether");
        header.append(System.getProperty("line.separator"));
        header.append("to broadcast a message to the server when a user goes up a skill level.");
        header.append(System.getProperty("line.separator"));
        header.append("  It also allows admins to set the maximum number of jobs a player can have at");
        header.append(System.getProperty("line.separator"));
        header.append("any one time.");
        header.append(System.getProperty("line.separator"));
        
        config.options().copyDefaults(true);
        
        writer.options().header(header.toString());
        
        writer.addComment("locale-language", "Default language.  Use your languages two digit ISO 639-1 code, and optionally followed by the ISO-3166-1 country code.",
                "Example: en, en_US");
        config.addDefault("locale-language", Locale.getDefault().getLanguage());

        writer.addComment("storage-method", "storage method, can be MySQL, sqlite, h2");
        config.addDefault("storage-method", "sqlite");
        
        writer.addComment("mysql-username", "Requires Mysql.");
        config.addDefault("mysql-username", "root");        
        config.addDefault("mysql-password", "");
        config.addDefault("mysql-url", "jdbc:mysql://localhost:3306/minecraft");
        config.addDefault("mysql-table-prefix", "");
        
        writer.addComment("save-period",  "How often in minutes you want it to save.  This must be a non-zero number");
        config.addDefault("save-period", 10);
        
        writer.addComment("save-on-disconnect",
                "Should player data be saved on disconnect?",
                "Player data is always periodically auto-saved and autosaved during a clean shutdown.",
                "Only enable this if you have a multi-server setup, or have a really good reason for enabling this.",
                "Turning this on will decrease database performance.");
        config.addDefault("save-on-disconnect", false);
        
        writer.addComment("broadcast-on-skill-up", "Do all players get a message when somone goes up a skill level?");
        config.addDefault("broadcast-on-skill-up", false);
        
        writer.addComment("broadcast-on-level-up", "Do all players get a message when somone goes up a level?");
        config.addDefault("broadcast-on-level-up", false);
        
        writer.addComment("max-jobs",
                "Maximum number of jobs a player can join.",
                "Use 0 for no maximum"
        );
        config.addDefault("max-jobs", 3);

        writer.addComment("max-wage",
                "Maximum amount of money a player can earn in a the pay-period.",
                "Use 0 for no maximum"
        );
        config.addDefault("max-wage", 0);

        writer.addComment("pay-period",
                "The time range of the limit reset in seconds.",
                "not used if max-wage is 0",
                "If set to 0 max-wage is not used"
        );
        config.addDefault("pay-period", 86400);
        
        writer.addComment("hide-jobs-without-permission", "Hide jobs from player if they lack the permission to join the job");
        config.addDefault("hide-jobs-without-permission", false);
        
        writer.addComment("enable-pay-near-spawner", "option to allow payment to be made when killing mobs from a spawner");
        config.addDefault("enable-pay-near-spawner", false);
        
        writer.addComment("enable-pay-creative", "option to allow payment to be made in creative mode");
        config.addDefault("enable-pay-creative", false);
        
        writer.addComment("add-xp-player", "Adds the Jobs xp recieved to the player's Minecraft XP bar");
        config.addDefault("add-xp-player", false);
        
        writer.addComment("modify-chat", "Modifys chat to add chat titles.  If you're using a chat manager, you may add the tag {jobs} to your chat format and disable this.");
        config.addDefault("modify-chat", true);
        
        writer.addComment("economy-batch-delay", "Changes how often, in seconds, players are paid out.  Default is 5 seconds.",
                "Setting this too low may cause tick lag.  Increase this to improve economy performance (at the cost of delays in payment)");
        config.addDefault("economy-batch-delay", 5);
        
        String storageMethod = config.getString("storage-method");
        if(storageMethod.equalsIgnoreCase("mysql")) {
            String username = config.getString("mysql-username");
            if(username == null) {
                Jobs.getPluginLogger().severe("mysql-username property invalid or missing");
            }
            String password = config.getString("mysql-password");
            String url = config.getString("mysql-url");
            String prefix = config.getString("mysql-table-prefix");
            if (plugin.isEnabled())
                Jobs.setDAO(new JobsDAOMySQL(url, username, password, prefix));
        } else if(storageMethod.equalsIgnoreCase("h2")) {
            File h2jar = new File(plugin.getDataFolder(), "h2.jar");
            if (!h2jar.exists()) {
                Jobs.getPluginLogger().info("[Jobs] H2 library not found, downloading...");
                try {
                    FileDownloader.downloadFile(new URL("http://dev.bukkit.org/media/files/692/88/h2-1.3.171.jar"), h2jar);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Jobs.getPluginLogger().severe("Could not download database library!");
                }
            }
            if (plugin.isEnabled()) {
                try {
                    Jobs.getJobsClassloader().addFile(h2jar);
                } catch (IOException e) {
                    Jobs.getPluginLogger().severe("Could not load database library!");
                }
                if (plugin.isEnabled())
                    Jobs.setDAO(new JobsDAOH2());
            }
        } else if(storageMethod.equalsIgnoreCase("sqlite")) {
            Jobs.setDAO(new JobsDAOSQLite());
        } else {
            Jobs.getPluginLogger().warning("Invalid storage method!  Defaulting to sqlite!");
            Jobs.setDAO(new JobsDAOSQLite());
        }
        
        if (config.getInt("save-period") <= 0) {
            Jobs.getPluginLogger().severe("Save period must be greater than 0!  Defaulting to 10 minutes!");
            config.set("save-period", 10);
        }
        
        String localeString = config.getString("locale-language");
        try {
            int i = localeString.indexOf('_');
            if (i == -1) {
                locale = new Locale(localeString);
            } else {
                locale = new Locale(localeString.substring(0, i), localeString.substring(i+1));
            }
        } catch (IllegalArgumentException e) {
            locale = Locale.getDefault();
            Jobs.getPluginLogger().warning("Invalid locale \""+localeString+"\" defaulting to "+locale.getLanguage());
        }
        
        savePeriod = config.getInt("save-period");
        isBroadcastingSkillups = config.getBoolean("broadcast-on-skill-up");
        isBroadcastingLevelups = config.getBoolean("broadcast-on-level-up");
        payInCreative = config.getBoolean("enable-pay-creative");
        addXpPlayer = config.getBoolean("add-xp-player");
        hideJobsWithoutPermission = config.getBoolean("hide-jobs-without-permission");
        maxJobs = config.getInt("max-jobs");
        maxWage = config.getDouble("max-wage");
        payPeriod = config.getLong("pay-period");
        payNearSpawner = config.getBoolean("enable-pay-near-spawner");
        modifyChat = config.getBoolean("modify-chat");
        economyBatchDelay = config.getInt("economy-batch-delay");
        saveOnDisconnect = config.getBoolean("save-on-disconnect");

        // Make sure we're only copying settings we care about
        copySetting(config, writer, "locale-language");
        copySetting(config, writer, "storage-method");
        copySetting(config, writer, "mysql-username");
        copySetting(config, writer, "mysql-password");
        copySetting(config, writer, "mysql-url");
        copySetting(config, writer, "mysql-table-prefix");
        copySetting(config, writer, "save-period");
        copySetting(config, writer, "save-on-disconnect");
        copySetting(config, writer, "broadcast-on-skill-up");
        copySetting(config, writer, "broadcast-on-level-up");
        copySetting(config, writer, "max-jobs");
        copySetting(config, writer, "max-wage");
        copySetting(config, writer, "pay-period");
        copySetting(config, writer, "hide-jobs-without-permission");
        copySetting(config, writer, "enable-pay-near-spawner");
        copySetting(config, writer, "enable-pay-creative");
        copySetting(config, writer, "add-xp-player");
        copySetting(config, writer, "modify-chat");
        copySetting(config, writer, "economy-batch-delay");
        
        // Write back config
        try {
            writer.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private synchronized void copySetting(Configuration reader, Configuration writer, String path) {
        writer.set(path, reader.get(path));
    }
    
    /**
     * Method to load the title configuration
     * 
     * loads from Jobs/titleConfig.yml
     */
    private synchronized void loadTitleSettings(){
        this.titles.clear();
        File f = new File(plugin.getDataFolder(), "titleConfig.yml");
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
        StringBuilder header = new StringBuilder()
            .append("Title configuration")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("Stores the titles people gain at certain levels.")
            .append(System.getProperty("line.separator"))
            .append("Each title requres to have a name, short name (used when the player has more than")
            .append(System.getProperty("line.separator"))
            .append("1 job) the colour of the title and the level requrirement to attain the title.")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("It is recommended but not required to have a title at level 0.")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("Titles are completely optional.")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("Titles:").append(System.getProperty("line.separator"))
            .append("  Apprentice:").append(System.getProperty("line.separator"))
            .append("    Name: Apprentice").append(System.getProperty("line.separator"))
            .append("    ShortName: A").append(System.getProperty("line.separator"))
            .append("    ChatColour: WHITE").append(System.getProperty("line.separator"))
            .append("    levelReq: 0").append(System.getProperty("line.separator"))
            .append("  Novice:").append(System.getProperty("line.separator"))
            .append("    Name: Novice").append(System.getProperty("line.separator"))
            .append("    ShortName: N").append(System.getProperty("line.separator"))
            .append("    ChatColour: GRAY").append(System.getProperty("line.separator"))
            .append("    levelReq: 30").append(System.getProperty("line.separator"))
            .append("  Journeyman:").append(System.getProperty("line.separator"))
            .append("    Name: Journeyman").append(System.getProperty("line.separator"))
            .append("    ShortName: J").append(System.getProperty("line.separator"))
            .append("    ChatColour: GOLD").append(System.getProperty("line.separator"))
            .append("    levelReq: 60").append(System.getProperty("line.separator"))
            .append("  Master:").append(System.getProperty("line.separator"))
            .append("    Name: Master").append(System.getProperty("line.separator"))
            .append("    ShortName: M").append(System.getProperty("line.separator"))
            .append("    ChatColour: BLACK").append(System.getProperty("line.separator"))
            .append("    levelReq: 90").append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"));
        conf.options().header(header.toString());
        conf.options().copyDefaults(true);
        conf.options().indent(2);
        
        ConfigurationSection titleSection = conf.getConfigurationSection("Titles");
        if (titleSection == null) {
            titleSection = conf.createSection("Titles");
        }
        for (String titleKey : titleSection.getKeys(false)) {
            String titleName = conf.getString("Titles."+titleKey+".Name");
            String titleShortName = conf.getString("Titles."+titleKey+".ShortName");
            ChatColor titleColor = ChatColor.matchColor(conf.getString("Titles."+titleKey+".ChatColour", ""));
            int levelReq = conf.getInt("Titles."+titleKey+".levelReq", -1);
            
            if (titleName == null) {
                Jobs.getPluginLogger().severe("Title " + titleKey + " has an invalid Name property. Skipping!");
                continue;
            }
            if (titleShortName == null) {
                Jobs.getPluginLogger().severe("Title " + titleKey + " has an invalid ShortName property. Skipping!");
                continue;
            }
            if (titleColor == null) {
                Jobs.getPluginLogger().severe("Title " + titleKey + "has an invalid ChatColour property. Skipping!");
                continue;
            }
            if (levelReq <= -1) {
                Jobs.getPluginLogger().severe("Title " + titleKey + " has an invalid levelReq property. Skipping!");
                continue;
            }
            
            this.titles.add(new Title(titleName, titleShortName, titleColor, levelReq));
        }
        
        try {
            conf.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    /**
     * Method to load the restricted areas configuration
     * 
     * loads from Jobs/restrictedAreas.yml
     */
    private synchronized void loadRestrictedAreaSettings(){
        this.restrictedAreas.clear();
        File f = new File(plugin.getDataFolder(), "restrictedAreas.yml");
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
        conf.options().indent(2);
        conf.options().copyDefaults(true);
        StringBuilder header = new StringBuilder();
        
        header.append("Restricted area configuration")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("Configures restricted areas where you cannot get experience or money")
            .append(System.getProperty("line.separator"))
            .append("when performing a job.")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("The multiplier changes the experience/money gains in an area.")
            .append(System.getProperty("line.separator"))
            .append("A multiplier of 0.0 means no money or xp, while 0.5 means you will get half the normal money/exp")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("restrictedareas:")
            .append(System.getProperty("line.separator"))
            .append("  area1:")
            .append(System.getProperty("line.separator"))
            .append("    world: 'world'")
            .append(System.getProperty("line.separator"))
            .append("    multiplier: 0.0")
            .append(System.getProperty("line.separator"))
            .append("    point1:")
            .append(System.getProperty("line.separator"))
            .append("      x: 125")
            .append(System.getProperty("line.separator"))
            .append("      y: 0")
            .append(System.getProperty("line.separator"))
            .append("      z: 125")
            .append(System.getProperty("line.separator"))
            .append("    point2:")
            .append(System.getProperty("line.separator"))
            .append("      x: 150")
            .append(System.getProperty("line.separator"))
            .append("      y: 100")
            .append(System.getProperty("line.separator"))
            .append("      z: 150")
            .append(System.getProperty("line.separator"))
            .append("  area2:")
            .append(System.getProperty("line.separator"))
            .append("    world: 'world_nether'")
            .append(System.getProperty("line.separator"))
            .append("    multiplier: 0.0")
            .append(System.getProperty("line.separator"))
            .append("    point1:")
            .append(System.getProperty("line.separator"))
            .append("      x: -100")
            .append(System.getProperty("line.separator"))
            .append("      y: 0")
            .append(System.getProperty("line.separator"))
            .append("      z: -100")
            .append(System.getProperty("line.separator"))
            .append("    point2:")
            .append(System.getProperty("line.separator"))
            .append("      x: -150")
            .append(System.getProperty("line.separator"))
            .append("      y: 100")
            .append(System.getProperty("line.separator"))
            .append("      z: -150");
        conf.options().header(header.toString());
        ConfigurationSection areaSection = conf.getConfigurationSection("restrictedareas");
        if (areaSection != null) {
            for (String areaKey : areaSection.getKeys(false)) {
                String worldName = conf.getString("restrictedareas."+areaKey+".world");
                double multiplier = conf.getDouble("restrictedareas."+areaKey+".multiplier", 0.0);
                Location point1 = new Location(worldName,
                        conf.getDouble("restrictedareas."+areaKey+".point1.x", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point1.y", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point1.z", 0.0));
    
                Location point2 = new Location(worldName,
                        conf.getDouble("restrictedareas."+areaKey+".point2.x", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point2.y", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point2.z", 0.0));
                this.restrictedAreas.add(new RestrictedArea(point1, point2, multiplier));
            }
        }
        try {
            conf.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
