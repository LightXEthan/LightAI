package me.LightXEthan.lightai;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class NPCSpawner implements CommandExecutor {

    Main main = (Main) Bukkit.getPluginManager().getPlugin("LightAI");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	
    	if (label.equalsIgnoreCase("spawnnpc")) {
    		
	    	Player player = (Player) sender;
	    	
	    	if (!player.isOp()) {
	    		return true;
	    	}
	    	
	    	Villager npc = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
	    	npc.setCustomNameVisible(true);
	    	
	    	if(args.length == 0) {
	            npc.setCustomName("Villager");
	            main.loadPrompt("Villager");
                return true;
            }
	    	
	        if (sender instanceof Player) {
	        	String npcName = args[0];
	            npc.setCustomName(npcName);
	            main.loadPrompt(npcName);
	        }
	        
	        player.sendMessage(ChatColor.GREEN + "NPC spawned!");
    	}
    	
    	if (label.equalsIgnoreCase("lightai")) {
    		Player player = (Player) sender;
    		
    		if(args.length == 0) {
    			player.sendMessage("Provide the API key. Usage: /lightai <your-apikey>");
    			return true;
    		}
    		
    		main.saveApiKey(args[0]);
    	}
    	return true;
    }
}




