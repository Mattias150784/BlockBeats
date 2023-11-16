package net.mattias.blockbeats;


import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Mod(BlockBeats.MOD_ID)
public class BlockBeats {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "blockbeats";

    private final String CLIENT_ID = "726ee65d301c4de683b1ececff4c5d12";
    private final String CLIENT_SECRET = "996eae0af95c417b85c9adccd43c1100";
    private final String REDIRECT_URI = "http://localhost:3000/callback";

    private final String AUTHORIZATION_URL = "https://accounts.spotify.com/authorize";
    private final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    private boolean isAuthenticated = false;

    // Assuming you have a custom sound event registered in your mod
    private final SoundEvent musicEvent = YourModSoundEvents.MUSIC_EVENT; // Replace with your actual sound event

    public BlockBeats() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    public void authenticateSpotify() {
        if (isAuthenticated) {
            // Handle already authenticated scenario
            playMusicForPlayer(Minecraft.getInstance().player);
        } else {
            String authUrl = AUTHORIZATION_URL +
                    "?client_id=" + CLIENT_ID +
                    "&response_type=code" +
                    "&redirect_uri=" + REDIRECT_URI +
                    "&scope=user-read-playback-state%20user-modify-playback-state" +
                    "&state=YOUR_STATE_STRING";

            openWebLink(authUrl);
        }
    }

    public void handleSpotifyCallback(String authorizationCode) {
        String tokenRequestUrl = TOKEN_URL +
                "?grant_type=authorization_code" +
                "&code=" + authorizationCode +
                "&redirect_uri=" + REDIRECT_URI +
                "&client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET;

        String accessToken = sendPostRequest(tokenRequestUrl);
        getUserPlaylists(accessToken);
    }

    public void getUserPlaylists(String accessToken) {
        try {
            String playlistsUrl = "https://api.spotify.com/v1/me/playlists";
            URL url = new URL(playlistsUrl);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                LOGGER.info("User's Playlists: {}", response.toString());
            } else {
                LOGGER.error("Failed to get user's playlists. Response Code: {}", responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openWebLink(String url) {
        Util.getPlatform().openUri(url);
    }

    private String sendPostRequest(String url) {
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.flush();
            wr.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                LOGGER.error("POST request failed. Response Code: {}", responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // New method to play music for the player
    private void playMusicForPlayer(Player player) {
        if (player != null) {
            // Play your music event for the player
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forMusic(musicEvent, 1.0F));
        }
    }
}
