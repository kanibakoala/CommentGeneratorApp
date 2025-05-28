package com.commentgenerator;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DeepSeekAPI {
    private String apiKey;
    
    public interface CommentCallback {
        void onSuccess(String comment);
        void onError(String error);
    }
    
    public DeepSeekAPI(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public void generateComment(String base64Image, int wordCount, CommentCallback callback) {
        new AsyncTask<Void, Void, String>() {
            private String error = null;
            
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL("https://api.deepseek.com/v1/chat/completions");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    conn.setDoOutput(true);
                    
                    // Construire le prompt
                    String prompt = String.format(
                        "Analyse cette image et génère un commentaire élogieux et réaliste dans le style des réseaux sociaux. " +
                        "Le commentaire doit faire environ %d mots, être positif, naturel et donner l'impression que la personne " +
                        "pourrait vraiment acheter ou utiliser ce produit. Exemples de style : " +
                        "- 'Je dois justement changer la mienne, celle-ci a l'air parfaite !' " +
                        "- 'Tellement élégante et confortable !' " +
                        "- 'Exactement ce que je cherchais, super pratique !' " +
                        "Réponds uniquement avec le commentaire, sans guillemets ni explications.",
                        wordCount
                    );
                    
                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("model", "deepseek-chat");
                    
                    JSONArray messages = new JSONArray();
                    JSONObject message = new JSONObject();
                    message.put("role", "user");
                    
                    JSONArray content = new JSONArray();
                    
                    // Texte du prompt
                    JSONObject textContent = new JSONObject();
                    textContent.put("type", "text");
                    textContent.put("text", prompt);
                    content.put(textContent);
                    
                    // Image
                    JSONObject imageContent = new JSONObject();
                    imageContent.put("type", "image_url");
                    JSONObject imageUrl = new JSONObject();
                    imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
                    imageContent.put("image_url", imageUrl);
                    content.put(imageContent);
                    
                    message.put("content", content);
                    messages.put(message);
                    jsonRequest.put("messages", messages);
                    jsonRequest.put("max_tokens", 150);
                    jsonRequest.put("temperature", 0.7);
                    
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonRequest.toString().getBytes());
                    os.flush();
                    os.close();
                    
                    int responseCode = conn.getResponseCode();
                    BufferedReader reader;
                    
                    if (responseCode == 200) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        error = "Erreur HTTP : " + responseCode;
                        return null;
                    }
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    if (responseCode == 200) {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject messageObj = choice.getJSONObject("message");
                            return messageObj.getString("content").trim();
                        }
                    }
                    
                    error = "Réponse inattendue : " + response.toString();
                    return null;
                    
                } catch (Exception e) {
                    error = "Erreur réseau : " + e.getMessage();
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(String result) {
                if (result != null && error == null) {
                    callback.onSuccess(result);
                } else {
                    callback.onError(error != null ? error : "Erreur inconnue");
                }
            }
        }.execute();
    }
}