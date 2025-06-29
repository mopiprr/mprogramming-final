// Create a new Java class named HistoryItem.java
package com.example.testfp;

import android.graphics.Bitmap;

public class HistoryItem {
    private final Bitmap image;
    private final String prediction;

    public HistoryItem(Bitmap image, String prediction) {
        this.image = image;
        this.prediction = prediction;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getPrediction() {
        return prediction;
    }
}