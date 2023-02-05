package me.LightXEthan.lightai;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Smite implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	// Smite
	if (label.equalsIgnoreCase("smite")) {
			
		Player player = (Player) sender;
		player.sendMessage(ChatColor.BLUE + "Hello, LightXEthan");
		
		for (Player target : player.getServer().getOnlinePlayers()) {
            target.getWorld().strikeLightning(target.getLocation());
        }

        player.sendMessage("You have smitten all players.");
		
    }
	return true;
    }
}
