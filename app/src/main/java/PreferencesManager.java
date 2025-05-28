package com.commentgenerator;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "CommentGeneratorPrefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_WORD_COUNT = "word_count";
    private static final String KEY_CONFIGURED = "configured";
    
    private SharedPreferences prefs;
    
    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveConfig(String apiKey, int wordCount) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putInt(KEY_WORD_COUNT, wordCount)
            .putBoolean(KEY_CONFIGURED, true)
            .apply();
    }
    
    public boolean isConfigured() {
        return prefs.getBoolean(KEY_CONFIGURED, false);
    }
    
    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }
    
    public int getWordCount() {
        return prefs.getInt(KEY_WORD_COUNT, 50);
    }
    
    public void clearConfig() {
        prefs.edit().clear().apply();
    }
}