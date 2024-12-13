package com.cookandroid.findcolorgame;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RankActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> rankingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        Button exitButton = findViewById(R.id.exitButton);
        db = FirebaseFirestore.getInstance();
        listView = findViewById(R.id.list_item);
        rankingList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rankingList);
        listView.setAdapter(adapter);

        loadRanking();

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RankActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadRanking() {
        DocumentReference docRef = db.collection("ranking").document("totalRanking");

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> entries = (Map<String, Object>) documentSnapshot.get("entries");

                if (entries != null) {
                    for (Map.Entry<String, Object> entry : entries.entrySet()) {
                        String key = entry.getKey();
                        Map<String, Object> value = (Map<String, Object>) entry.getValue();

                        String name = (String) value.get("name");
                        Long score = (Long) value.get("score");

                        Log.d(TAG, "Key: " + key + ", Name: " + name + ", Score: " + score);

                        rankingList.add(name + " : " + score);

                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "No entries field found");
                }

            } else {
                Log.d(TAG, "No such document");
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Error getting document", e);
        });
    }
}