package com.cookandroid.findcolorgame;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final int INITIAL_GRID_SIZE = 4;
    private static final long TIME_LIMIT = 15000; // 15 seconds
    private static final int TILE_SPACING = 8; // Spacing between tiles

    private GridLayout tileGrid;
    private TextView timerTextView, stageTextView;

    private int currentStage = 1;
    private int gridSize = INITIAL_GRID_SIZE;

    private CountDownTimer timer;
    private int differentTileIndex; // 정답 타일 인덱스 저장

    // Fire Base에서 사용자 정보를 가져옴
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    // 파이어 베이스 저장소
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tileGrid = findViewById(R.id.tileGrid);
        timerTextView = findViewById(R.id.timerTextView);
        stageTextView = findViewById(R.id.stageTextView);

        if (user != null) {
            String uid = user.getUid(); // 고유 사용자 ID
            String email = user.getEmail(); // 사용자 이메일
            String displayName = user.getDisplayName(); // 사용자 표시 이름
        }

        tileGrid.post(() -> startGame()); // Ensure layout is measured before starting

    }

    private void startGame() {
        stageTextView.setText("Stage: " + currentStage);
        setupGrid();
        resetTimer();
    }

    private void setupGrid() {
        tileGrid.removeAllViews();
        tileGrid.setColumnCount(gridSize);
        tileGrid.setRowCount(gridSize);

        // Calculate the available width and height for tiles
        int availableWidth = tileGrid.getWidth() - TILE_SPACING * (gridSize + 1);
        int availableHeight = tileGrid.getHeight() - TILE_SPACING * (gridSize + 1);

        // Calculate the size of each tile to ensure they are square and fit within the layout
        int tileSize = Math.min(availableWidth, availableHeight) / gridSize;

        if (tileSize <= 0) {
            gridSize--; // Reduce grid size if tiles exceed layout bounds
            startGame();
            return;
        }

        int totalTiles = gridSize * gridSize;
        Random random = new Random();

        // Ensure the correct tile is not assigned to an overflowing position
        do {
            differentTileIndex = random.nextInt(totalTiles);
        } while (isTileOverflowing(differentTileIndex, tileSize));

        int baseColor = generateRandomColor();
        int differentColor = darkenColor(baseColor);

        for (int i = 0; i < totalTiles; i++) {
            View tile = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = tileSize;
            params.height = tileSize;
            params.setMargins(TILE_SPACING / 2, TILE_SPACING / 2, TILE_SPACING / 2, TILE_SPACING / 2);
            tile.setLayoutParams(params);

            if (i == differentTileIndex) {
                tile.setBackgroundColor(differentColor); // Different color tile
                tile.setOnClickListener(v -> {
                    timer.cancel();
                    onCorrectTileClicked();
                });
            } else {
                tile.setBackgroundColor(baseColor); // Normal tile
                tile.setOnClickListener(v -> Toast.makeText(this, "Try again!", Toast.LENGTH_SHORT).show());
            }
            tileGrid.addView(tile);
        }
    }

    private boolean isTileOverflowing(int tileIndex, int tileSize) {
        int row = tileIndex / gridSize;
        int col = tileIndex % gridSize;

        // Calculate the position of the tile
        int leftEdge = col * (tileSize + TILE_SPACING);
        int topEdge = row * (tileSize + TILE_SPACING);
        int rightEdge = leftEdge + tileSize;
        int bottomEdge = topEdge + tileSize;

        // Check if the tile exceeds the layout bounds
        return rightEdge > tileGrid.getWidth() || bottomEdge > tileGrid.getHeight();
    }

    private void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(TIME_LIMIT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                onTimeUp();
            }
        }.start();
    }

    private void onCorrectTileClicked() {
        currentStage++;
        gridSize++;
        startGame();
    }

    private void onTimeUp() {
        for (int i = 0; i < tileGrid.getChildCount(); i++) {
            View tile = tileGrid.getChildAt(i);
            if (i == differentTileIndex) {
                tile.setBackgroundColor(Color.GREEN); // Highlight the correct tile
            } else {
                tile.setBackgroundColor(Color.GRAY); // Dim all other tiles
            }
        }

        new Handler().postDelayed(this::showRetryDialog, 3000); // Delay for 3 seconds before showing dialog
    }

    private void showRetryDialog() {
        if (user != null) {
            String uid = user.getUid();
            int userScore = currentStage;

            Map<String, Object> userData = new HashMap<>();
            userData.put("key",uid);
            userData.put("score", userScore);
            userData.put("name", user.getDisplayName());

            DocumentReference totalRankingDocRef = db.collection("ranking").document("totalRanking");

            Map<String, Object> entriesUpdate = new HashMap<>();
            entriesUpdate.put(uid, userData); // 사용자 ID를 키로 하는 맵 생성

            totalRankingDocRef.set(new HashMap<String, Object>() {{ // 빈 Map으로 시작
                put("entries", entriesUpdate);
            }}, SetOptions.merge()).addOnSuccessListener(aVoid -> {
                Log.d("Firestore", "Ranking successfully written!");
            }).addOnFailureListener(e -> {
                Log.w("Firestore", "Error writing ranking", e);
            });
        }
        new AlertDialog.Builder(this)
                .setTitle("게임 종료")
                .setMessage("다시 하겠습니까?")
                .setPositiveButton("다시 하기", (dialog, which) -> {
                    currentStage = 1;
                    gridSize = INITIAL_GRID_SIZE;
                    startGame();
                })
                .setNegativeButton("종료", (dialog, which) -> {
                    Intent intent = new Intent(GameActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private int generateRandomColor() {
        Random random = new Random();
        int red = random.nextInt(156) + 100; // Bright colors
        int green = random.nextInt(156) + 100;
        int blue = random.nextInt(156) + 100;
        return Color.rgb(red, green, blue);
    }

    private int darkenColor(int color) {
        int red = (int) (Color.red(color) * 0.8); // 20% darker
        int green = (int) (Color.green(color) * 0.8);
        int blue = (int) (Color.blue(color) * 0.8);
        return Color.rgb(red, green, blue);
    }
}
