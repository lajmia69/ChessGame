package com.chess.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class BackgroundMusicPlayer {
    private Clip clip;
    private boolean isPlaying = false;
    private FloatControl volumeControl;
    
    public BackgroundMusicPlayer(String filePath) {
        try {
            File audioFile = new File(filePath);
            
            if (!audioFile.exists()) {
                System.err.println("[AUDIO] File not found: " + filePath);
                return;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            // Get volume control if available
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(0.5f); // Set to 50% volume by default
            }
            
            System.out.println("[AUDIO] Music loaded successfully: " + audioFile.getName());
            
        } catch (UnsupportedAudioFileException e) {
            System.err.println("[AUDIO] Unsupported audio format. Please use WAV, AIFF, or AU format.");
            System.err.println("[AUDIO] For MP3 support, you'll need to convert to WAV first.");
        } catch (IOException e) {
            System.err.println("[AUDIO] Error reading audio file: " + e.getMessage());
        } catch (LineUnavailableException e) {
            System.err.println("[AUDIO] Audio line unavailable: " + e.getMessage());
        }
    }
    
    public void play() {
        if (clip != null && !isPlaying) {
            clip.loop(Clip.LOOP_CONTINUOUSLY); // Loop the music
            clip.start();
            isPlaying = true;
            System.out.println("[AUDIO] Music started playing (looping)");
        }
    }
    
    public void pause() {
        if (clip != null && isPlaying) {
            clip.stop();
            isPlaying = false;
            System.out.println("[AUDIO] Music paused");
        }
    }
    
    public void resume() {
        if (clip != null && !isPlaying) {
            clip.start();
            isPlaying = true;
            System.out.println("[AUDIO] Music resumed");
        }
    }
    
    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            isPlaying = false;
            System.out.println("[AUDIO] Music stopped");
        }
    }
    
    public void setVolume(float volume) {
        if (volumeControl != null) {
            // Volume range: 0.0f (silent) to 1.0f (full volume)
            volume = Math.max(0.0f, Math.min(1.0f, volume));
            
            // Convert linear volume to decibels
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float gain = min + (max - min) * volume;
            
            volumeControl.setValue(gain);
            System.out.println("[AUDIO] Volume set to: " + (int)(volume * 100) + "%");
        }
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void close() {
        if (clip != null) {
            stop();
            clip.close();
            System.out.println("[AUDIO] Music player closed");
        }
    }
}