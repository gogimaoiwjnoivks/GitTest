package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PlantSearch {

    private final BottomSheetDialog dialog;
    private final TextView tvFloatingResult;
    private final ImageView ivFloatingPlant;
    private final ScrollView floatingScrollView;
    private final ImageButton btnFloatingScrollToggle;
    private final OnButtonClickListener buttonClickListener;

    private static final int SCROLL_STATUS_TOP = 0;
    private static final int SCROLL_STATUS_BOTTOM = 1;
    private int currentScrollStatus = SCROLL_STATUS_TOP;

    public interface OnButtonClickListener {
        void onWaterClick(String plantName, TextView resultTextView);
        void onFeatureClick(String plantName, TextView resultTextView);
    }

    public PlantSearch(@NonNull Context context, @Nullable Bitmap plantBitmap, String initialText, String plantName, OnButtonClickListener listener) {
        this.buttonClickListener = listener;
        this.dialog = new BottomSheetDialog(context);

        View sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_floating_result, null);
        dialog.setContentView(sheetView);

        tvFloatingResult = sheetView.findViewById(R.id.tvFloatingResult);
        ivFloatingPlant = sheetView.findViewById(R.id.ivFloatingPlant);
        floatingScrollView = sheetView.findViewById(R.id.floatingScrollView);
        btnFloatingScrollToggle = sheetView.findViewById(R.id.btnFloatingScrollToggle);
        Button btnFloatingWater = sheetView.findViewById(R.id.btnFloatingWater);
        Button btnFloatingFeature = sheetView.findViewById(R.id.btnFloatingFeature);
        Button btnFloatingClose = sheetView.findViewById(R.id.btnFloatingClose);
        Button btnRegisterSearchedPlant = sheetView.findViewById(R.id.btnRegisterSearchedPlant);

        tvFloatingResult.setText(initialText);
        if (plantBitmap != null) {
            ivFloatingPlant.setImageBitmap(plantBitmap);
        }

        btnFloatingScrollToggle.setOnClickListener(v -> {
            if (currentScrollStatus == SCROLL_STATUS_TOP) {
                floatingScrollView.smoothScrollTo(0, floatingScrollView.getChildAt(0).getHeight());
                btnFloatingScrollToggle.setImageResource(R.drawable.up);
                currentScrollStatus = SCROLL_STATUS_BOTTOM;
            } else {
                floatingScrollView.smoothScrollTo(0, 0);
                btnFloatingScrollToggle.setImageResource(R.drawable.down);
                currentScrollStatus = SCROLL_STATUS_TOP;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            floatingScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                View childView = floatingScrollView.getChildAt(floatingScrollView.getChildCount() - 1);
                int diff = (childView.getBottom() - (floatingScrollView.getHeight() + floatingScrollView.getScrollY()));

                if (diff <= 0) {
                    currentScrollStatus = SCROLL_STATUS_BOTTOM;
                    btnFloatingScrollToggle.setImageResource(R.drawable.up);
                } else if (scrollY == 0) {
                    currentScrollStatus = SCROLL_STATUS_TOP;
                    btnFloatingScrollToggle.setImageResource(R.drawable.down);
                }
            });
        }

        btnFloatingWater.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onWaterClick(plantName, tvFloatingResult);
            }
        });

        btnFloatingFeature.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onFeatureClick(plantName, tvFloatingResult);
            }
        });

        if (btnRegisterSearchedPlant != null) {
            btnRegisterSearchedPlant.setOnClickListener(v -> {

                String finalPlantName = "";
                if (plantName != null && !plantName.isEmpty()) {
                    finalPlantName = plantName.trim();
                } else {
                    String currentResultText = tvFloatingResult.getText().toString();
                    if (currentResultText.contains("인식된 식물:")) {
                        try {
                            finalPlantName = currentResultText.split("\n")[0].replace("인식된 식물:", "").trim();
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }

                if (finalPlantName.isEmpty()) {
                    Toast.makeText(context, "잠시만 기다려 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();

                Context currentContext = dialog.getContext();
                if (currentContext instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) currentContext;
                    mainActivity.isAutoRegistrationMode = true;
                    mainActivity.detectedPlantName = finalPlantName;

                    mainActivity.runOnUiThread(() -> {
                        mainActivity.showNameInputDialog(plantBitmap, null);
                    });
                } else {
                    if (context instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) context;
                        mainActivity.isAutoRegistrationMode = true;
                        mainActivity.detectedPlantName = finalPlantName;
                        mainActivity.runOnUiThread(() -> {
                            mainActivity.showNameInputDialog(plantBitmap, null);
                        });
                    }
                }
            });
        }

        btnFloatingClose.setOnClickListener(v -> dialog.dismiss());

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int desiredHeight = (int) (displayMetrics.heightPixels * 0.70);

            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = desiredHeight;
            bottomSheet.setLayoutParams(layoutParams);

            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    public void updateResultText(String text) {
        if (tvFloatingResult != null) {
            tvFloatingResult.setText(text);
            if (btnFloatingScrollToggle != null) {
                btnFloatingScrollToggle.setVisibility(View.VISIBLE);
            }
        }
    }

    public void show() {
        if (dialog != null) dialog.show();
    }

    public void expandBottomSheetHeight() {
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            DisplayMetrics displayMetrics = dialog.getContext().getResources().getDisplayMetrics();

            int desiredHeight = (int) (displayMetrics.heightPixels * 0.90);

            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = desiredHeight;
            bottomSheet.setLayoutParams(layoutParams);

            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}