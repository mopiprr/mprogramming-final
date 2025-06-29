// Create a new Java class named HistoryManager.java
package com.example.testfp;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final HistoryManager instance = new HistoryManager();
    private final List<HistoryItem> historyList;

    // Private constructor to prevent instantiation
    private HistoryManager() {
        historyList = new ArrayList<>();
    }

    public static HistoryManager getInstance() {
        return instance;
    }

    public void addHistoryItem(Bitmap image, String prediction) {
        // Add new items to the beginning of the list
        historyList.add(0, new HistoryItem(image, prediction));
    }

    public List<HistoryItem> getHistoryList() {
        return historyList;
    }
}