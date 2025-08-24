package com.otakusweett.badapple.spigot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BadApplePlugin extends JavaPlugin implements Listener {
    private List<String> frames = new ArrayList<>();
    private Map<UUID, AnimationTask> activeAnimations = new HashMap<>();
    private Map<UUID, Location> playerLocations = new HashMap<>();
    private Map<UUID, List<org.bukkit.entity.ArmorStand>> holograms = new HashMap<>();
    private Map<UUID, Boolean> playerInTheater = new HashMap<>();
    private Map<UUID, RadioSongPlayer> songPlayers = new HashMap<>();
    private World theaterWorld;
    private Song song;
    private final int HOLOGRAM_WIDTH = 60;
    private final int HOLOGRAM_HEIGHT = 30;
    private final double FRAME_RATE = 27.95;
    private double MS_PER_FRAME = 1000.0 / FRAME_RATE; 
    
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        setupTheaterWorld();
        loadFrames();
        loadSong();
        
        adjustFrameTiming();
        
        getLogger().info("BadApplePlugin enabled with " + frames.size() + " frames");
    }

    @Override
    public void onDisable() {
        for (AnimationTask task : activeAnimations.values()) {
            task.stop();
        }
        activeAnimations.clear();
        holograms.clear();
        playerInTheater.clear();
        
        for (RadioSongPlayer songPlayer : songPlayers.values()) {
            songPlayer.setPlaying(false);
            songPlayer.destroy();
        }
        songPlayers.clear();
    }

    private void setupTheaterWorld() {
        WorldCreator wc = new WorldCreator("badapple_theater");
        wc.type(WorldType.FLAT);
        wc.generateStructures(false);
        
        try {
            theaterWorld = wc.createWorld();
        } catch (Exception e) {
            theaterWorld = Bukkit.getWorlds().get(0);
            getLogger().warning("Could not create custom world, using default world: " + e.getMessage());
        }
        
        if (theaterWorld != null) {
            try {
                theaterWorld.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
                theaterWorld.setTime(6000);
                theaterWorld.setStorm(false);
                theaterWorld.setThundering(false);
                theaterWorld.setSpawnLocation(0, 64, 0);
                
                Location platformLoc = new Location(theaterWorld, 0, 63, 0);
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location blockLoc = platformLoc.clone().add(x, 0, z);
                        blockLoc.getBlock().setType(Material.BARRIER);
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Could not configure world: " + e.getMessage());
            }
        }
    }

    private void loadSong() {
        try {
            File songFile = new File(getDataFolder(), "Bad Apple.nbs");
            if (!songFile.exists()) {
                saveResource("Bad Apple.nbs", false);
                getLogger().info("Saved default music file");
            }
            
            song = NBSDecoder.parse(songFile);
            if (song != null) {
                getLogger().info("Loaded Bad Apple music successfully");
            } else {
                getLogger().warning("Failed to load music file");
            }
        } catch (Exception e) {
            getLogger().severe("Error loading music: " + e.getMessage());
        }
    }

    private void adjustFrameTiming() {
        if (frames.isEmpty()) return;
        
        double originalDuration = 219.0;
        double midiDuration = 186.0;
        
        double correctionFactor = midiDuration / originalDuration;
        
        MS_PER_FRAME *= correctionFactor;
        
        getLogger().info("Ajustando velocidad de frames: " + MS_PER_FRAME + " ms por frame");
        getLogger().info("Factor de corrección: " + correctionFactor);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        event.setJoinMessage(null);
        
        if (theaterWorld != null) {
            try {
                player.teleport(new Location(theaterWorld, 0, 64, 0));
            } catch (Exception e) {
                getLogger().warning("Could not teleport player: " + e.getMessage());
            }
        }
        
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                startAnimation(player);
            }
        }, 40L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        event.setQuitMessage(null);
        
        if (playerInTheater.getOrDefault(player.getUniqueId(), false)) {
            stopAnimation(player);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (playerInTheater.getOrDefault(event.getPlayer().getUniqueId(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (playerInTheater.getOrDefault(player.getUniqueId(), false)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to.getX() != from.getX() || to.getZ() != from.getZ()) {
                event.setCancelled(true);
            }
        }
    }

    private void loadFrames() {
        frames.clear();
        Path dataFolder = getDataFolder().toPath();
        
        if (!Files.exists(dataFolder)) {
            try {
                Files.createDirectories(dataFolder);
            } catch (IOException e) {
                getLogger().severe("Failed to create data folder: " + e.getMessage());
            }
        }

        Path framesFile = dataFolder.resolve("badapple_frames.txt");
        if (!Files.exists(framesFile)) {
            createDefaultFramesFile(framesFile);
            return;
        }

        try {
            String content = new String(Files.readAllBytes(framesFile));
            String[] frameList = content.split("---FRAME---");

            for (String frame : frameList) {
                String trimmedFrame = frame.trim();
                if (!trimmedFrame.isEmpty()) {
                    frames.add(normalizeFrame(trimmedFrame));
                }
            }

            getLogger().info("Loaded " + frames.size() + " frames");
        } catch (IOException e) {
            getLogger().severe("Failed to load frames: " + e.getMessage());
            createDefaultFrames();
        }
    }

    private String normalizeFrame(String frame) {
        String[] lines = frame.split("\n");
        StringBuilder normalized = new StringBuilder();
        
        for (int i = 0; i < HOLOGRAM_HEIGHT; i++) {
            if (i < lines.length) {
                String line = lines[i];
                if (line.length() > HOLOGRAM_WIDTH) {
                    normalized.append(line.substring(0, HOLOGRAM_WIDTH));
                } else {
                    normalized.append(line);
                    for (int j = line.length(); j < HOLOGRAM_WIDTH; j++) {
                        normalized.append("░");
                    }
                }
            } else {
                for (int j = 0; j < HOLOGRAM_WIDTH; j++) {
                    normalized.append("░");
                }
            }
            if (i < HOLOGRAM_HEIGHT - 1) {
                normalized.append("\n");
            }
        }
        
        return normalized.toString();
    }

    private void createDefaultFramesFile(Path framesFile) {
        String defaultFrame = "██████████████████████████████████████████████████\n" +
                            "██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░░░░░░░░░BAD APPLE!!░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░░░Place your frames in:░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░plugins/BadApple/badapple_frames.txt░░░░░░██\n" +
                            "██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██████████████████████████████████████████████████";

        try {
            Files.write(framesFile, defaultFrame.getBytes());
            frames.add(normalizeFrame(defaultFrame));
            getLogger().warning("Created default frames file. Please add your Bad Apple frames.");
        } catch (IOException e) {
            getLogger().severe("Failed to create default frames file: " + e.getMessage());
            createDefaultFrames();
        }
    }

    private void createDefaultFrames() {
        String defaultFrame = "██████████████████████████████████████████████████\n" +
                            "██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░░░░░░░░░BAD APPLE!!░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░Failed to load frames file░░░░░░░░░░░░░░░░░██\n" +
                            "██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░██\n" +
                            "██████████████████████████████████████████████████";
        frames.add(normalizeFrame(defaultFrame));
    }

    public void startAnimation(Player player) {
        if (frames.isEmpty()) {
            return;
        }

        playerLocations.put(player.getUniqueId(), player.getLocation().clone());
        
        preparePlayerForTheater(player);
        
        
        player.sendMessage(ChatColor.YELLOW + "Loading animation...");

        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                AnimationTask task = new AnimationTask(player, frames);
                activeAnimations.put(player.getUniqueId(), task);
                playerInTheater.put(player.getUniqueId(), true);
                
                task.initializeArmorStands();
                
                startMusic(player);
                
                task.runTaskTimer(BadApplePlugin.this, 0, 1);
            }
        });
    }

    private void startMusic(Player player) {
        if (song == null) {
            return;
        }
        
        RadioSongPlayer songPlayer = new RadioSongPlayer(song);
        songPlayer.addPlayer(player);
        songPlayer.setAutoDestroy(true);
        songPlayers.put(player.getUniqueId(), songPlayer);
        songPlayer.setPlaying(true);
    }

    private void stopMusic(Player player) {
        RadioSongPlayer songPlayer = songPlayers.remove(player.getUniqueId());
        if (songPlayer != null) {
            songPlayer.setPlaying(false);
            songPlayer.destroy();
        }
    }

    private void preparePlayerForTheater(Player player) {
        try {
            Location theaterLocation = new Location(theaterWorld, 0, 64, 0);
            player.teleport(theaterLocation);
        } catch (Exception e) {
            getLogger().warning("Could not teleport player to theater: " + e.getMessage());
        }
        
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0);
        player.setWalkSpeed(0);
        
        player.setExp(0);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setHealth(20.0);
        
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                try {
                    player.hidePlayer(other);
                } catch (Exception e) {
                    getLogger().warning("Could not hide player: " + e.getMessage());
                }
            }
        }
    }

    public void stopAnimation(Player player) {
        UUID playerId = player.getUniqueId();
        AnimationTask task = activeAnimations.remove(playerId);
        if (task != null) {
            task.stop();
        }
        
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(playerId)) {
                try {
                    player.showPlayer(other);
                } catch (Exception e) {
                    getLogger().warning("Could not show player: " + e.getMessage());
                }
            }
        }
        
        Location originalLocation = playerLocations.remove(playerId);
        if (originalLocation != null) {
            try {
                player.teleport(originalLocation);
            } catch (Exception e) {
                getLogger().warning("Could not teleport player back: " + e.getMessage());
            }
        }
        
        List<org.bukkit.entity.ArmorStand> playerHolograms = holograms.remove(playerId);
        if (playerHolograms != null) {
            for (org.bukkit.entity.ArmorStand hologram : playerHolograms) {
                try {
                    hologram.remove();
                } catch (Exception e) {
                    getLogger().warning("Could not remove hologram: " + e.getMessage());
                }
            }
        }
        
        stopMusic(player);
        
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.2f);
        
        playerInTheater.remove(playerId);
    }

    private class AnimationTask extends BukkitRunnable {
        private final Player player;
        private final List<String> frames;
        private int currentFrame = 0;
        private List<org.bukkit.entity.ArmorStand> armorStands = new ArrayList<>();
        private boolean armorStandsInitialized = false;
        private long startTime;

        public AnimationTask(Player player, List<String> frames) {
            this.player = player;
            this.frames = frames;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if (!player.isOnline() || !playerInTheater.getOrDefault(player.getUniqueId(), false)) {
                stop();
                return;
            }

            if (currentFrame >= frames.size()) {
                
                Bukkit.getScheduler().runTask(BadApplePlugin.this, new Runnable() {
                    @Override
                    public void run() {
                        player.kickPlayer("Animation finished");
                    }
                });
                stop();
                return;
            }

            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            
            int targetFrame = (int) (elapsedTime / MS_PER_FRAME);

            if (targetFrame > currentFrame) {
                currentFrame = targetFrame;
                if (currentFrame < frames.size()) {
                    displayFrame(frames.get(currentFrame));
                }
            }
        }

        public void initializeArmorStands() {
            if (!armorStandsInitialized) {
                clearHolograms();
                
                for (int i = 0; i < HOLOGRAM_HEIGHT; i++) {
                    try {
                        org.bukkit.entity.ArmorStand armorStand = player.getWorld().spawn(
                            new Location(player.getWorld(), 0, 0, 0), 
                            org.bukkit.entity.ArmorStand.class
                        );
                        
                        armorStand.setGravity(false);
                        armorStand.setVisible(false);
                        armorStand.setCustomNameVisible(true);
                        armorStand.setCustomName("");
                        armorStand.setSmall(true);
                        
                        armorStands.add(armorStand);
                    } catch (Exception e) {
                        getLogger().warning("Could not create armor stand: " + e.getMessage());
                    }
                }
                
                holograms.put(player.getUniqueId(), new ArrayList<>(armorStands));
                armorStandsInitialized = true;
            }
        }

        private void displayFrame(String frame) {
            if (!armorStandsInitialized) {
                return;
            }
            
            String[] lines = frame.split("\n");
            
            Location playerLoc = player.getLocation();
            Location baseLocation = playerLoc.clone().add(playerLoc.getDirection().multiply(15));
            baseLocation.add(0, 5, 0);
            
            for (int i = 0; i < armorStands.size(); i++) {
                if (i >= armorStands.size()) break;
                
                org.bukkit.entity.ArmorStand armorStand = armorStands.get(i);
                String line = (i < lines.length) ? lines[i] : "";
                
                Location lineLocation = baseLocation.clone().add(0, -i * 0.3, 0);
                
                try {
                    Bukkit.getScheduler().runTask(BadApplePlugin.this, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                lineLocation.setDirection(playerLoc.subtract(lineLocation).toVector());
                                armorStand.teleport(lineLocation);
                                armorStand.setCustomName(ChatColor.WHITE + line);
                            } catch (Exception e) {
                                getLogger().warning("Could not update armor stand: " + e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    getLogger().warning("Could not update armor stand: " + e.getMessage());
                }
            }
        }

        private void clearHolograms() {
            for (org.bukkit.entity.ArmorStand armorStand : armorStands) {
                try {
                    armorStand.remove();
                } catch (Exception e) {
                    getLogger().warning("Could not remove armor stand: " + e.getMessage());
                }
            }
            armorStands.clear();
            armorStandsInitialized = false;
        }

        public void stop() {
            clearHolograms();
            try {
                this.cancel();
            } catch (Exception e) {
                getLogger().warning("Could not cancel task: " + e.getMessage());
            }
        }
    }
}