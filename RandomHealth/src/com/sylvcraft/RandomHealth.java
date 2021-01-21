package com.sylvcraft;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sylvcraft.commands.rndhealth;


public class RandomHealth extends JavaPlugin {
  private BukkitTask runnable;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    getCommand("rndhealth").setExecutor(new rndhealth(this));
    healthTask();
  }
  
  private void healthTask() {
    if (getConfig().getInt("config.interval", 60) < 1) {
      getLogger().warning("** Invalid interval specified, killing task!");
      return;
    }
    
    Permission exempt = new Permission("rndhealth.exempt", PermissionDefault.FALSE);
    
    runnable = new BukkitRunnable() {
      @Override
      public void run() {
        if (!getConfig().getBoolean("config.active", true)) return;
        
        for (Player p : getServer().getOnlinePlayers()) {
          if (p.hasPermission(exempt)) { Log(p.getName() + " is exempt"); continue;}

          processValue("food", p);
          processValue("health", p);
          processValue("maxhealth", p);
        }
      }
    }.runTaskTimer(this, getConfig().getInt("config.interval", 60) * 20, getConfig().getInt("config.interval", 60) * 20);
  }
  
  private void processValue(String attr, Player p) {
    String profile = getProfile(p);
    if (!getConfig().getBoolean("config." + profile + "." + attr + ".enabled")) return;

    double low = 0, high = 20, newLevel = 20;
    double max = attr.equalsIgnoreCase("food")?20:p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
    double cur = attr.equalsIgnoreCase("food")?p.getFoodLevel():p.getHealth();

    switch (getConfig().getString("config." + profile + "." + attr + ".mode", "exact").toLowerCase()) {
    case "exact":
      low = getConfig().getDouble("config." + profile + "." + attr + ".low", 1);
      high = getConfig().getDouble("config." + profile + "." + attr + ".high", 20);
      newLevel = (low == high)?low:ThreadLocalRandom.current().nextDouble(low, high);
      Log("[" + profile + "][" + p.getName() + "] Processing " + attr + " exact.  Low " + low + ", high " + high + ", chosen: " + newLevel);
      break;
      
    case "pctmax":
      low = (max * getConfig().getDouble("config." + profile + "." + attr + ".low", 1)) / 100;
      high = (max * getConfig().getDouble("config." + profile + "." + attr + ".high", 100)) / 100;
      newLevel = (low == high)?low:ThreadLocalRandom.current().nextDouble(low, high);
      Log("[" + profile + "][" + p.getName() + "] Processing " + attr + " pct/max.  Low " + low + ", high " + high + ", chosen: " + newLevel);
      break;
      
    case "pctcur":
      low = (cur * getConfig().getDouble("config." + profile + "." + attr + ".low", 1)) / 100;
      high = (cur * getConfig().getDouble("config." + profile + "." + attr + ".high", 100)) / 100;
      newLevel = (low == high)?low:ThreadLocalRandom.current().nextDouble(low, high);
      Log("[" + profile + "][" + p.getName() + "] Processing " + attr + " pct/cur.  Low " + low + ", high " + high + ", chosen: " + newLevel);
      break;
    }

    if (newLevel > max) {
      newLevel = max;
      Log("Chosen level was greater than max (" + max + ") so capped at max");
    }

    switch (attr.toLowerCase()) {
    case "food":
      p.setFoodLevel((int)newLevel);
      break;
      
    case "health":
      p.setHealth(newLevel);
      break;
      
    case "maxhealth":
      p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newLevel);
      break;
    }
  }

  private String getProfile(Player p) {
    ConfigurationSection perms = getConfig().getConfigurationSection("config.perms");
    if (perms == null) return "global";
    
    String curProfile = "global";
    int curWeight = 0;
    
    for (String permnode : perms.getKeys(false)) {
      String perm = permnode.replace("_", ".");
      List<String> exemptions = Arrays.asList(perms.getString(permnode + ".exempt", "").toLowerCase().split(","));
      if (exemptions.contains(p.getName().toLowerCase())) continue;
      
      if (perms.getBoolean(permnode + ".explicit") && !p.hasPermission(new Permission(perm, PermissionDefault.FALSE))) continue;
      if (!p.hasPermission(perm)) continue;
      
      int weight = perms.getInt(permnode + ".weight");
      if (weight >= curWeight || curProfile.equals("")) {
        curProfile = "perms." + permnode;
        curWeight = weight;
      }
    }
    
    return curProfile;
  }
 
  public boolean getActive() {
    return getConfig().getBoolean("config.active", true);
  }
  
  public void setActive(boolean active) {
    getConfig().set("config.active", active);
    saveConfig();
  }
  
  public void setMode(String attr, String mode) {
    if (!Arrays.asList("exact","pctmax","pctcur").contains(mode.toLowerCase())) {
      getLogger().warning("Invalid mode was attempted to be set for " + attr + " (" + mode + ").  Ignoring.");
      return;
    }
    
    getConfig().set("config." + attr + ".mode", mode.toLowerCase());
  }
  
  public String getMode(String attr) {
    return getConfig().getString("config." + attr + ".mode", "exact");
  }
  
  public void setInterval(int interval) {
    getConfig().set("config.interval", interval);
    saveConfig();
    runnable.cancel();
    healthTask();
  }
  
  public int getInterval() {
    return getConfig().getInt("config.interval");
  }
 
  public boolean getStatus(String parameter) {
    return getConfig().getBoolean("config." + parameter + ".enabled");
  }
  
  public void setStatus(String parameter, boolean enabled) {
    getConfig().set("config." + parameter + ".enabled", enabled);
    saveConfig();
  }
  
  public void setBound(String parameter, String bclass, int bound) {
    getConfig().set("config." + parameter.toLowerCase() + "." + bclass.toLowerCase(), bound);
    saveConfig();
  }
  
  public double getBound(String parameter, String bclass) {
    return getConfig().getDouble("config." + parameter.toLowerCase() + "." + bclass.toLowerCase());
  }
  
  public void msg(String msgCode, CommandSender sender) {
  	if (getConfig().getString("messages." + msgCode) == null) return;
  	msgTransmit(getConfig().getString("messages." + msgCode), sender);
  }

  public void msg(String msgCode, CommandSender sender, Map<String, String> data) {
  	if (getConfig().getString("messages." + msgCode) == null) return;
  	String tmp = getConfig().getString("messages." + msgCode, msgCode);
  	for (Map.Entry<String, String> mapData : data.entrySet()) {
  	  tmp = tmp.replace(mapData.getKey(), mapData.getValue());
  	}
  	msgTransmit(tmp, sender);
  }
  
  public void msgTransmit(String msg, CommandSender sender) {
  	for (String m : (msg + " ").split("%br%")) {
  		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', m));
  	}
  }
  
  private void Log(String msg) {
    if (!getConfig().getBoolean("config.chatty")) return;
    
    getLogger().info(msg);
  }
}