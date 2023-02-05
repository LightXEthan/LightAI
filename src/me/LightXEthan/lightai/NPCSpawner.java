package me.LightXEthan.lightai;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
	            loadPrompt("Villager", player);
                return true;
            }
	    	
	        if (sender instanceof Player) {
	        	String npcName = args[0];
	            npc.setCustomName(npcName);
	            loadPrompt(npcName, player);
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
	
	public void loadPrompt(String npcName, Player player) {
		
		File file = main.loadFile(npcName);

        try {
            // Read the contents of the file into a string
            String contents = new String(Files.readAllBytes(Paths.get(file.getPath())));
            // Print the contents of the file
            System.out.println(contents);
            
    		String npcPrompt = contents;
    		
    		main.savePrompt(npcName, npcPrompt);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(ChatColor.GRAY + "Prompt not found in folder.");
        }
	}
}




