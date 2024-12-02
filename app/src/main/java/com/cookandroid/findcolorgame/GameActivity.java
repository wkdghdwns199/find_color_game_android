package com.cookandroid.findcolorgame;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private int gridSize = 2; // 초기 그리드 크기 (2x2)
    private int stage = 1;   // 초기 스테이지
    private TextView stageText, timerText;
    private GridLayout gridLayout;
    private boolean isClickable = true; // 클릭 가능 상태
    private Handler timerHandler = new Handler();
    private int correctPosition; // 정답 위치 저장
    private int timeRemaining = 15; // 15초 제한

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        stageText = findViewById(R.id.stageText);
        timerText = findViewById(R.id.timerText);
        gridLayout = findViewById(R.id.gridLayout);

        updateStage(); // 초기화 후 스테이지 설정
    }

    private void updateStage() {
        stageText.setText("스테이지: " + stage);

        // 스테이지에 따라 그리드 크기 증가
        gridSize = 2 + (stage - 1); // 2x2 → 3x3 → 4x4 ...

        timeRemaining = 15; // 타이머 초기화
        updateTimerUI();
        generateGrid();
        startTimer();
    }

    private void generateGrid() {
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(gridSize);
        gridLayout.setRowCount(gridSize);

        correctPosition = new Random().nextInt(gridSize * gridSize);
        int baseColor = Color.rgb(new Random().nextInt(200), new Random().nextInt(200), new Random().nextInt(200));
        int diffColor = adjustColorContrast(baseColor, 80);

        // 디바이스 너비 기준으로 GridLayout 크기 설정
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int deviceWidth = metrics.widthPixels;

        // GridLayout 크기 (디바이스 너비의 90%)
        int gridLayoutSize = (int) (deviceWidth * 0.9);
        gridLayout.getLayoutParams().width = gridLayoutSize;
        gridLayout.getLayoutParams().height = gridLayoutSize;
        gridLayout.requestLayout();

        // 여백과 타일 크기 계산
        int margin = 8; // 타일 간 여백
        int totalMargin = margin * (gridSize + 1); // 총 여백
        int tileSize = (gridLayoutSize -  totalMargin) / gridSize;

        // 디버깅 로그
        System.out.println("GridLayout Size: " + gridLayoutSize);
        System.out.println("Tile Size: " + tileSize);
        System.out.println("Total Margin: " + totalMargin);

        // 타일 추가
        for (int i = 0; i < gridSize * gridSize; i++) {
            final int position = i;
            View view = new View(this);
            view.setBackgroundColor(i == correctPosition ? diffColor : baseColor);

            // 클릭 이벤트
            view.setOnClickListener(v -> {
                if (!isClickable) return; // 클릭 비활성화 상태면 무시
                if (position == correctPosition) {
                    stage++;
                    updateStage();
                } else {
                    Toast.makeText(this, "다시 시도하세요!", Toast.LENGTH_SHORT).show();
                }
            });

            // 타일 레이아웃 설정
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = tileSize;
            params.height = tileSize;
            params.setMargins(margin, margin, margin, margin);
            view.setLayoutParams(params);

            gridLayout.addView(view);
        }
    }



    private void startTimer() {
        isClickable = true;
        timerHandler.removeCallbacksAndMessages(null); // 이전 타이머 초기화

        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (timeRemaining > 0) {
                    timeRemaining--;
                    updateTimerUI();
                    timerHandler.postDelayed(this, 1000); // 1초 간격
                } else {
                    isClickable = false;
                    showAnswer();
                }
            }
        }, 1000);
    }

    private void updateTimerUI() {
        timerText.setText("남은 시간: " + timeRemaining + "초");
    }

    private void showAnswer() {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (i == correctPosition) {
                child.setBackgroundColor(Color.GREEN); // 정답 강조
            } else {
                child.setBackgroundColor(Color.GRAY); // 다른 타일 흐리게
            }
        }

        // 3초 후 팝업창 표시
        timerHandler.postDelayed(this::showPopup, 3000);
    }

    private void showPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 종료");
        builder.setMessage("다른 색깔을 찾지 못했습니다.");
        builder.setPositiveButton("다시하기", (dialog, which) -> {
            stage = 1; // 초기화
            updateStage();
        });
        builder.setNegativeButton("종료", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    private int adjustColorContrast(int color, int contrast) {
        int r = Math.max(0, Math.min(255, Color.red(color) + contrast));
        int g = Math.max(0, Math.min(255, Color.green(color) - contrast));
        int b = Math.max(0, Math.min(255, Color.blue(color) + contrast));
        return Color.rgb(r, g, b);
    }
}
