package com.commentgenerator;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private EditText apiKeyEditText;
    private SeekBar wordCountSeekBar;
    private TextView wordCountLabel;
    private Button goButton;
    private PreferencesManager prefsManager;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefsManager = new PreferencesManager(this);
        
        // Si déjà configuré, lancer directement le service
        if (prefsManager.isConfigured()) {
            startFloatingBubble();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        checkPermissions();
    }
    
    private void initViews() {
        apiKeyEditText = findViewById(R.id.api_key_edit_text);
        wordCountSeekBar = findViewById(R.id.word_count_seekbar);
        wordCountLabel = findViewById(R.id.word_count_label);
        goButton = findViewById(R.id.go_button);
        
        // Initialiser la barre de progression (10-100 mots)
        wordCountSeekBar.setMax(90);
        wordCountSeekBar.setProgress(40); // 50 mots par défaut
        updateWordCountLabel(50);
    }
    
    private void setupListeners() {
        wordCountSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int wordCount = progress + 10; // 10-100 mots
                updateWordCountLabel(wordCount);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfigAndStart();
            }
        });
    }
    
    private void updateWordCountLabel(int wordCount) {
        wordCountLabel.setText("Nombre de mots : " + wordCount);
    }
    
    private void saveConfigAndStart() {
        String apiKey = apiKeyEditText.getText().toString().trim();
        int wordCount = wordCountSeekBar.getProgress() + 10;
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer une clé API", Toast.LENGTH_SHORT).show();
            return;
        }
        
        prefsManager.saveConfig(apiKey, wordCount);
        startFloatingBubble();
        finish();
    }
    
    private void startFloatingBubble() {
        Intent intent = new Intent(this, FloatingBubbleService.class);
        startService(intent);
    }
    
    private void checkPermissions() {
        // Vérifier les permissions de base
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
        }
        
        // Vérifier la permission d'overlay pour Android 6+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }
}