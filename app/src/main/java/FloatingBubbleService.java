package com.commentgenerator;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingBubbleService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private TextView bubbleText;
    private PreferencesManager prefsManager;
    private CameraHelper cameraHelper;
    private DeepSeekAPI deepSeekAPI;
    
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private boolean isSwipeGesture = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        prefsManager = new PreferencesManager(this);
        cameraHelper = new CameraHelper(this);
        deepSeekAPI = new DeepSeekAPI(prefsManager.getApiKey());
        
        createFloatingBubble();
    }
    
    private void createFloatingBubble() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_bubble, null);
        
        bubbleText = floatingView.findViewById(R.id.bubble_text);
        bubbleText.setText(String.valueOf(prefsManager.getWordCount()));
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;
        
        windowManager.addView(floatingView, params);
        
        setupTouchListener(params);
    }
    
    private void setupTouchListener(WindowManager.LayoutParams params) {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        isSwipeGesture = false;
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        
                        if (Math.abs(deltaX) > 20 || Math.abs(deltaY) > 20) {
                            isDragging = true;
                            
                            // Détecter les gestes de glissement
                            if (Math.abs(deltaY) > Math.abs(deltaX)) {
                                isSwipeGesture = true;
                                if (deltaY > 100) {
                                    // Glissement vers le bas - montrer croix de fermeture
                                    bubbleText.setText("✕");
                                } else if (deltaY < -100) {
                                    // Glissement vers le haut - montrer roue crantée
                                    bubbleText.setText("⚙");
                                } else {
                                    bubbleText.setText(String.valueOf(prefsManager.getWordCount()));
                                }
                            } else {
                                // Déplacement normal
                                params.x = (int) (initialX + deltaX);
                                params.y = (int) (initialY + deltaY);
                                windowManager.updateViewLayout(floatingView, params);
                            }
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        if (isSwipeGesture) {
                            float deltaY = event.getRawY() - initialTouchY;
                            if (deltaY > 100) {
                                // Fermer l'application
                                stopSelf();
                            } else if (deltaY < -100) {
                                // Ouvrir les paramètres
                                openSettings();
                            }
                            bubbleText.setText(String.valueOf(prefsManager.getWordCount()));
                        } else if (!isDragging) {
                            // Clic simple - prendre photo et générer commentaire
                            takePhotoAndGenerate();
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    private void takePhotoAndGenerate() {
        bubbleText.setText("📸");
        
        cameraHelper.takePhoto(new CameraHelper.PhotoCallback() {
            @Override
            public void onPhotoTaken(String imagePath) {
                bubbleText.setText("🔄");
                deepSeekAPI.generateComment(imagePath, prefsManager.getWordCount(), 
                    new DeepSeekAPI.CommentCallback() {
                        @Override
                        public void onSuccess(String comment) {
                            // Copier dans le presse-papiers
                            android.content.ClipboardManager clipboard = 
                                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            android.content.ClipData clip = 
                                android.content.ClipData.newPlainText("Commentaire", comment);
                            clipboard.setPrimaryClip(clip);
                            
                            bubbleText.setText("✓");
                            Toast.makeText(FloatingBubbleService.this, 
                                "Commentaire copié !", Toast.LENGTH_SHORT).show();
                            
                            // Revenir au nombre de mots après 2 secondes
                            floatingView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    bubbleText.setText(String.valueOf(prefsManager.getWordCount()));
                                }
                            }, 2000);
                        }
                        
                        @Override
                        public void onError(String error) {
                            bubbleText.setText("⚠");
                            Toast.makeText(FloatingBubbleService.this, 
                                "Erreur : " + error, Toast.LENGTH_SHORT).show();
                            
                            floatingView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    bubbleText.setText(String.valueOf(prefsManager.getWordCount()));
                                }
                            }, 2000);
                        }
                    });
            }
            
            @Override
            public void onError(String error) {
                bubbleText.setText("⚠");
                Toast.makeText(FloatingBubbleService.this, 
                    "Erreur photo : " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void openSettings() {
        prefsManager.clearConfig();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        stopSelf();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
