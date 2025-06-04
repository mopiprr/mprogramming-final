package com.example.testfp;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoboFlowAPI {
    private static final String API_KEY = "N8XMwzMYT2XhLZiVCP8K";
    private static final String MODEL_ENDPOINT = "soto-type-detection/5";
    private static final String BASE_URL = "https://detect.roboflow.com/";
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;

    public interface ClassificationCallback {
        void onSuccess(String result);
        void onFailure(String error);
    }

    public RoboFlowAPI(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public void classifyImage(Bitmap bitmap, ClassificationCallback callback) {
        // Convert bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = RequestBody.create(byteArray, MEDIA_TYPE_JPEG);

        String url = BASE_URL + MODEL_ENDPOINT + "?api_key=" + API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("API error: " + response.code());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    RoboFlowResponse roboFlowResponse = gson.fromJson(responseBody, RoboFlowResponse.class);

                    if (roboFlowResponse.predictions != null && !roboFlowResponse.predictions.isEmpty()) {
                        RoboFlowResponse.Prediction topPrediction = roboFlowResponse.predictions.get(0);
                        String result = String.format("Category: %s (%.2f%%)",
                                topPrediction.class_name,
                                topPrediction.confidence * 100);
                        callback.onSuccess(result);
                    } else {
                        callback.onSuccess("No categories detected");
                    }
                } catch (Exception e) {
                    callback.onFailure("Parsing error: " + e.getMessage());
                }
            }
        });
    }

    // Response classes for Gson parsing
    private static class RoboFlowResponse {
        List<Prediction> predictions;

        static class Prediction {
            String class_name;
            double confidence;
        }
    }
}