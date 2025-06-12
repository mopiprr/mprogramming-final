package com.example.testfp;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class RoboFlowAPI {
    private static final String ROBOFLOW_API_KEY = "N8XMwzMYT2XhLZiVCP8K";
    private static final String ROBOFLOW_MODEL_ID = "soto-type-detection";
    private static final String ROBOFLOW_MODEL_VERSION = "5";

    // Base URL for Roboflow serverless inference API
    private static final String API_BASE_URL = "https://serverless.roboflow.com/";

    private final OkHttpClient httpClient;
    private final Context context;

    /**
     * Interface for handling asynchronous classification results.
     */
    public interface ClassificationCallback {
        /**
         * Called when the classification is successful.
         * @param result The classification result string.
         */
        void onSuccess(String result);

        /**
         * Called when an error occurs during classification.
         * @param error The error message.
         */
        void onFailure(String error);
    }

    /**
     * Constructor for RoboFlowAPI.
     * @param context The application context.
     */
    public RoboFlowAPI(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
    }

    /**
     * Classifies a given Bitmap image using the Roboflow API.
     *
     * @param imageBitmap The Bitmap image to classify.
     * @param callback    The callback interface to receive success or failure results.
     */
    public void classifyImage(Bitmap imageBitmap, ClassificationCallback callback) {
        if (imageBitmap == null) {
            callback.onFailure("Image bitmap is null.");
            return;
        }

        // Convert the Bitmap to a Base64 encoded string
        String base64Image = bitmapToBase64(imageBitmap);
        if (base64Image == null) {
            callback.onFailure("Failed to convert image to Base64.");
            return;
        }

        // Construct the full API URL for your Roboflow model
        // Sticking to path parameters for consistency with earlier structure, as it's cleaner.
        String fullUrl = API_BASE_URL + ROBOFLOW_MODEL_ID + "/" + ROBOFLOW_MODEL_VERSION + "?api_key=" + ROBOFLOW_API_KEY;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");

        RequestBody body = RequestBody.create(mediaType, base64Image);

        // Build the HTTP POST request
        Request request = new Request.Builder()
                .url(fullUrl)
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        // Enqueue the request for asynchronous execution
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle network errors or other request failures
                String errorMessage = "Network Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
                callback.onFailure(errorMessage);
                // Optionally show a toast on the UI thread
                showToastOnMainThread(errorMessage);
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Parse the JSON response to extract classification results
                        String classificationResult = parseRoboflowResponse(jsonResponse);
                        callback.onSuccess(classificationResult);

                    } catch (JSONException e) {
                        String errorMessage = "JSON Parsing Error: " + (e.getMessage() != null ? e.getMessage() : "Invalid JSON format");
                        callback.onFailure(errorMessage);
                        showToastOnMainThread(errorMessage);
                        e.printStackTrace();
                    } catch (Exception e) {
                        // Catch any other unexpected errors during response processing
                        String errorMessage = "Unexpected Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error during response processing");
                        callback.onFailure(errorMessage);
                        showToastOnMainThread(errorMessage);
                        e.printStackTrace();
                    }
                } else {
                    // Handle unsuccessful HTTP responses (e.g., 400, 500 status codes)
                    String errorMessage = "API Error: " + response.code() + " - " + (response.message() != null ? response.message() : "Unknown API error");
                    callback.onFailure(errorMessage);
                    showToastOnMainThread(errorMessage);
                    if (response.body() != null) {
                        // Log the error body if available for debugging
                        System.err.println("Roboflow API Error Body: " + response.body().string());
                    }
                }
            }
        });
    }

    /**
     * Converts a Bitmap image to a Base64 encoded string.
     * @param bitmap The Bitmap image to convert.
     * @return The Base64 encoded string, or null if conversion fails.
     */
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Compress the bitmap to JPEG format with 100% quality
            // You can adjust format (PNG) and quality (0-100) as needed for your model
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            // Encode the byte array to Base64, without line wraps (NO_WRAP)
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null if an error occurs during conversion
        }
    }

    /**
     * Parses the JSON response from the Roboflow classification API.
     * This method needs to be adapted based on the exact JSON structure
     * returned by your specific Roboflow model.
     *
     * @param jsonResponse The JSONObject received from the Roboflow API.
     * @return A formatted string of the classification result.
     * @throws JSONException If there is an error parsing the JSON.
     */
    private String parseRoboflowResponse(JSONObject jsonResponse) throws JSONException {
        StringBuilder resultBuilder = new StringBuilder();

        // Example 1: If the response has a "predictions" array (common for object detection or multi-label classification)
        if (jsonResponse.has("predictions")) {
            JSONArray predictions = jsonResponse.getJSONArray("predictions");
            if (predictions.length() > 0) {
                // For classification, typically the highest confidence prediction is the main result.
                // You might iterate through all predictions if you have multiple labels.
                JSONObject topPrediction = predictions.getJSONObject(0); // Assuming first is the most relevant
                String predictedClass = topPrediction.getString("class");
                double confidence = topPrediction.getDouble("confidence");
                resultBuilder.append("Predicted Class: ").append(predictedClass)
                        .append(", Confidence: ").append(String.format("%.2f", confidence * 100)).append("%");
            } else {
                resultBuilder.append("No predictions found.");
            }
        }
        // Example 2: If the response has a "top" key directly (common for single-label classification)
        else if (jsonResponse.has("top")) {
            String predictedClass = jsonResponse.getString("top");
            double confidence = jsonResponse.getDouble("confidence");
            resultBuilder.append("Predicted Class: ").append(predictedClass)
                    .append(", Confidence: ").append(String.format("%.2f", confidence * 100)).append("%");
        }
        // Add more parsing logic here if your model's JSON response format is different
        // For example, if it returns a 'label' key and 'score' key at the root.
        else {
            resultBuilder.append("Unknown Roboflow API response format. Raw: ").append(jsonResponse.toString());
        }

        return resultBuilder.toString();
    }

    /**
     * Helper method to display a Toast message on the main UI thread.
     * @param message The message to display.
     */
    private void showToastOnMainThread(String message) {
        if (context instanceof MainActivity) { // Check if context is an Activity to runOnUiThread
            ((MainActivity) context).runOnUiThread(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        } else {
            System.err.println("RoboFlowAPI: Toast message: " + message);
        }
    }
}