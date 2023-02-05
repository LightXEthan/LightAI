package me.LightXEthan.lightai;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin implements Listener {
    
    // holds players and the villager it is talking to
    private Map<Player, String> lockedPlayers = new HashMap<>();
    
    // holds npcs and their prompts and chat history
    private Map<String, String> npcs = new HashMap<>();
    
    // OpenAI API Key
    private String apiKey = null;
    
	@Override
	public void onEnable() {
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		getCommand("spawnnpc").setExecutor(new NPCSpawner());
		getCommand("lightai").setExecutor(new NPCSpawner());
	}
	
	@Override
	public void onDisable() {
		
	}
	
	// Save API key

	public void saveApiKey(String apiKey) {
		this.apiKey = apiKey;
		
		// save to file
		File dataFolder = getDataFolder();
		File file = new File(dataFolder, "apikey.txt");
		
		// check exists
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(apiKey);
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
            e.printStackTrace();
        }
	}
	
	public boolean checkApiKey() {
		// Get the plugin directory
        File pluginDir = getDataFolder();

        // Create the file object for the .txt file
        File file = new File(pluginDir, "apikey.txt");
        
        try {
            // Read the contents of the file into a string
            String contents = new String(Files.readAllBytes(Paths.get(file.getPath())));
            
            this.apiKey = contents;
            
            System.out.println("API key loaded successfully");
            
            return true;
            
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
	}
	
	// Load prompt
	
	public void loadPrompt(String npcName) {
		
		File file = loadFile(npcName);

        try {
            // Read the contents of the file into a string
            String contents = new String(Files.readAllBytes(Paths.get(file.getPath())));
            // Print the contents of the file
            System.out.println(contents);
            
    		String npcPrompt = contents;
    		
    		savePrompt(npcName, npcPrompt);
        } catch (IOException e) {
            e.printStackTrace();
            broadcastMessage(ChatColor.GRAY + "Prompt not found.");
        }
	}
	
	public File loadFile(String filename) {
		// Get the plugin directory
        File pluginDir = getDataFolder();

        // Create the file object for the .txt file
        File file = new File(pluginDir, filename + ".txt");
        
        return file;
	}
	
	public void savePrompt(String npcName, String npcPrompt) {
		npcs.put(npcName, npcPrompt);
	}
	
	// Locking and Unlocking the player
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
	    Player player = event.getPlayer();
	    Entity clickedEntity = event.getRightClicked();
	    String npcName = clickedEntity.getName();

	    // Check if the entity clicked is a Villager
	    if (clickedEntity instanceof Villager) {
	        Villager villager = (Villager) clickedEntity;

	        if(lockedPlayers.containsKey(player) && !Objects.isNull(lockedPlayers.get(player))) {
	        	event.setCancelled(true); // cancel the original event
	            //unlock the player
	            lockedPlayers.put(player, null);
	            
	            villager.setAI(true);

	            unlockPlayer(player, npcName, clickedEntity);
	        } else {
	            event.setCancelled(true); // cancel the original event
	            //lock the player and make the villager look at the player
	            lockPlayer(player, npcName, clickedEntity);
	            
	            villager.setTarget(player);
	            villager.setAI(false);
	            Vector playerDirection = player.getLocation().getDirection();
	            Vector villagerDirection = new Vector(-playerDirection.getX(), playerDirection.getY(), -playerDirection.getZ());
	            villager.teleport(villager.getLocation().setDirection(villagerDirection));
	        }
	    }
	}
	
	public void unlockPlayer(Player player, String npcName, Entity entity) {
		lockedPlayers.put(player, null);
        player.setWalkSpeed((float) 0.2);
        player.sendMessage(ChatColor.GRAY + "You have been unlocked, you can now move again");
	}
	
	public void lockPlayer(Player player, String npcName, Entity entity) {
		lockedPlayers.put(player, npcName);
		player.setWalkSpeed(0);
		player.sendMessage(ChatColor.GRAY + "You are locked, talk to the villager to proceed");
	}
	
	// Handle chat messages
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
	    Player player = event.getPlayer();
	    String playerMessage = event.getMessage();
	    
	    // focused player on npc
		if(lockedPlayers.containsKey(player) && !Objects.isNull(lockedPlayers.get(player))) {
			
			// Check API Key
			if (this.apiKey == null) {
				if (!checkApiKey()) {
					broadcastMessage("API key not set. Use /lightai <your-apikey>");
					return;
				}
			}
		    
		    // Send the player's message to the chat
		    event.setCancelled(true);
		    broadcastMessage(String.format("<%s> %s", player.getName(), playerMessage));
		    
		    // Call the function
		    String message = getOpenAiResponse(player, playerMessage);
		    broadcastMessage(message);
		}
	}
	
	public String buildPrompt(String playerName, String npcName, String inputPrompt, int max_tokens, double temperature) {
		String prompt = String.format(
				"{\"presence_penalty\": 0.6,\"frequency_penalty\": 1.0,  \"temperature\": %.2f, \"stop\": [\"%s:\",\"%s:\"], \"prompt\": \"%s\", \"max_tokens\": %d, \"model\": \"text-davinci-003\"}", 
				temperature, playerName, npcName, inputPrompt, max_tokens
		);
		return prompt;
	}
	
	public String getDefaultPrompt(String npcName) {
		// Check if prompt exists in folder
		this.loadPrompt(npcName);
		if (npcs.containsKey(npcName)) {
			return npcs.get(npcName);
		}
        
		return String.format("The following is a conversation between a player and %s\\n", npcName);
	}
	
    public String getOpenAiResponse(Player player, String playerMessage) {
    	
    	// Memory
    	boolean hasMemory = true;
    	
    	String npcName = lockedPlayers.get(player);
    	String npcPrompt = npcs.get(npcName);
    	
    	// check npcPrompt
    	if (npcPrompt == null) {
    		npcPrompt = getDefaultPrompt(npcName);
    	}
    	
    	String playerPrompt = String.format("%s\\n%s: %s\\n%s:", npcPrompt, player.getName(), playerMessage, npcName);
    	System.out.println("Player prompt: " +  playerPrompt);
    	
    	// sanitize player message
    	playerPrompt = playerPrompt.replace('"', '\'');
    	
    	// create the HTTP request
        String prompt = buildPrompt(player.getName(), npcName, playerPrompt, 30, 0.6);
        String response = requestOpenAi(prompt);
    	
    	if (response == null) {
    		player.sendMessage("An error occurred while trying to reach the OpenAI API");
	        return "An error occurred while trying to reach the OpenAI API";
    	}
    	
        System.out.println("Response: " + response);

        // parse the response
        String messageText = response.substring(response.indexOf("\"text\":\"") + 8, response.indexOf("\",\"index"));
        
        // remove the \n
        messageText = messageText.trim().replace("\\n", "");
        String message = String.format("<%s> %s", npcName, messageText);
        
        // remove '
        message = message.replace('"', '\'');
        message = message.replace('\'', '\'');
        
        // Save conversation to memory
        if (hasMemory) {
    		npcs.put(npcName, playerPrompt + " " + messageText);
    	}

        // send the message to the player
        return message;
    }
    
    public String requestOpenAi(String prompt) {
		String response = null;
		try {
	    	
	        byte[] postData = prompt.getBytes(StandardCharsets.UTF_8);
	        int postDataLength = postData.length;
	        URL url = new URL("https://api.openai.com/v1/completions");
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setDoOutput(true);
	        connection.setInstanceFollowRedirects(false);
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/json");
	        connection.setRequestProperty("Authorization", String.format("Bearer %s", apiKey));
	        connection.setRequestProperty("charset", "utf-8");
	        connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
	        connection.setUseCaches(false);

	        // Log the request payload
	        System.out.println("Request payload: " + prompt);
	        
	        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
	            wr.write(postData);
	        }
	        
	        // read the response
	        InputStream inputStream = connection.getInputStream();
	        
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	        response = bufferedReader.readLine();
	        
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return response;
	}
    
    public void broadcastMessage(String message) {
    	Bukkit.getServer().broadcastMessage(message);
    }
}
