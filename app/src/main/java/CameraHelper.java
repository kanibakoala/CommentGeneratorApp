package com.commentgenerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraHelper {
    private Context context;
    private Camera camera;
    
    public interface PhotoCallback {
        void onPhotoTaken(String imagePath);
        void onError(String error);
    }
    
    public CameraHelper(Context context) {
        this.context = context;
    }
    
    public void takePhoto(PhotoCallback callback) {
        try {
            if (camera != null) {
                camera.release();
            }
            
            camera = Camera.open();
            
            Camera.Parameters params = camera.getParameters();
            // Définir une résolution basse
            params.setPictureSize(640, 480);
            params.setJpegQuality(50);
            camera.setParameters(params);
            
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        // Convertir en bitmap et redimensionner
                        Bitmap originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 320, 240, true);
                        
                        // Convertir en base64
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);
                        
                        callback.onPhotoTaken(base64Image);
                        
                        // Libérer les ressources
                        originalBitmap.recycle();
                        resizedBitmap.recycle();
                        
                    } catch (Exception e) {
                        callback.onError("Erreur traitement image : " + e.getMessage());
                    } finally {
                        if (camera != null) {
                            camera.release();
                            CameraHelper.this.camera = null;
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            callback.onError("Erreur caméra : " + e.getMessage());
        }
    }
}