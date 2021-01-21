package com.sylvcraft.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import com.sylvcraft.RandomHealth;
import net.md_5.bungee.api.ChatColor;

public class rndhealth implements TabExecutor {
  RandomHealth plugin;
  
  public rndhealth(RandomHealth instance) {
    plugin = instance;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
    if (!sender.hasPermission("rndhealth.admin")) return null;
    
    switch (args.length) {
    case 1:
      return Arrays.asList("interval","food","health","maxhealth","reload","toggle");
      
    case 2:
      switch (args[0].toLowerCase()) {
      case "interval":
        return new ArrayList<String>();

      case "maxhealth":
        return Arrays.asList("low","high","toggle");

      case "food":
      case "health":
        return Arrays.asList("low","high","toggle","mode");
      }
      
    case 3:
      if (args[1].equalsIgnoreCase("mode")) return Arrays.asList("exact","pctmax","pctcur"); 
      break;
    }
    
    return new ArrayList<String>();
  }
  
  
  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    try {
      if (!sender.hasPermission("rndhealth.admin")) {
        plugin.msg("access-denied", sender);
        return true;
      }
      
      Map<String, String> data = new HashMap<String, String>();
      if (args.length == 0) {
        for (String parameter : Arrays.asList("food","health","maxhealth")) {
          data.put("%" + parameter + "-status%", plugin.getStatus(parameter)?"enabled":"disabled");
          data.put("%" + parameter + "-low%", String.valueOf(plugin.getBound(parameter, "low")));
          data.put("%" + parameter + "-high%", String.valueOf(plugin.getBound(parameter, "high")));
          data.put("%" + parameter + "-mode%", String.valueOf(plugin.getMode(parameter)));
        }
        data.put("%interval%", String.valueOf(plugin.getInterval()));
        plugin.msg("status-interval", sender, data);
        plugin.msg("status-food", sender, data);
        plugin.msg("status-health", sender, data);
        plugin.msg("status-maxhealth", sender, data);
        return true;
      }

      switch (args[0].toLowerCase()) {
      case "toggle":
        plugin.setActive(!plugin.getActive());
        data.put("%status%", plugin.getActive()?"active":"inactive");
        plugin.msg("status-plugin", sender, data);
        break;
        
      case "reload":
        plugin.reloadConfig();
        plugin.msg("reloaded", sender);
        break;
        
      case "chatty":
        plugin.getConfig().set("config.chatty", !plugin.getConfig().getBoolean("config.chatty"));
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GOLD + "Chatty mode " + ChatColor.GREEN + (plugin.getConfig().getBoolean("config.chatty")?"enabled":"disabled"));
        break;
        
      case "info":
        Player p = (Player)sender;
        p.sendMessage("Food: " + p.getFoodLevel()); 
        p.sendMessage("Health: " + p.getHealth());
        p.sendMessage("Max health: " + p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        break;
        
      case "interval":
        try {
          data.put("%value%", args[1]);
          int interval = Integer.valueOf(args[1]);
          plugin.setInterval(interval);
          plugin.msg("set-interval", sender, data);
        } catch (NumberFormatException ex) {
          plugin.msg("invalid-value", sender);
        }
        break;
        
      case "health":
      case "maxhealth":
      case "food":
        String parameter = args[0].toLowerCase();
        data.put("%parameter%", parameter);
        
        if (args.length < 2) {
          plugin.msg("help-" + parameter, sender);
          return true;
        }
        
        switch (args[1].toLowerCase()) {
        case "toggle":
          plugin.setStatus(parameter, !plugin.getStatus(parameter));
          data.put("%status%", plugin.getStatus(parameter)?"enabled":"disabled");
          plugin.msg("set-status", sender, data);
          return true;
          
        case "mode":
          data.put("%attribute%", "mode");
          data.put("%value%", args[2].toLowerCase());
          plugin.setMode(parameter, args[2].toLowerCase());
          plugin.msg("set-value", sender, data);
          break;

        case "low":
        case "high":
          data.put("%attribute%", args[1].toLowerCase());
          data.put("%value%", args[2]);
          
          try {
            int value = Integer.valueOf(args[2]);
            plugin.setBound(parameter, args[1].toLowerCase(), value);
            plugin.msg("set-value", sender, data);
          } catch (NumberFormatException ex) {
            plugin.msg("invalid-value", sender);
          }
          break;
          
        default:
          plugin.msg("invalid-attribute", sender);
          break;
        }
        break;
      }

      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }
}
