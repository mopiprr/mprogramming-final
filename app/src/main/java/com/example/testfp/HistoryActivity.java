// Create a new Activity class named HistoryActivity.java
package com.example.testfp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView tvEmptyHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Set up the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Identification History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
        }

        recyclerView = findViewById(R.id.rv_history);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);

        List<HistoryItem> historyList = HistoryManager.getInstance().getHistoryList();

        if (historyList.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new HistoryAdapter(historyList);
            recyclerView.setAdapter(adapter);
        }
    }

    // Handle back button press in action bar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}