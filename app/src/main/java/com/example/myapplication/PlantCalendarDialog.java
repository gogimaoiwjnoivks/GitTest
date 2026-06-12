package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlantCalendarDialog {

    private final Context context;
    private final Plant targetPlant;
    private final FirebaseFirestore firestore;
    private final String currentUserUid;
    private final AppDatabase localDb;
    private final Map<String, String> editingMemoMap;

    public PlantCalendarDialog(@NonNull Context context, Plant targetPlant) {
        this.context = context;
        this.targetPlant = targetPlant;
        this.firestore = FirebaseFirestore.getInstance();
        this.localDb = AppDatabase.getInstance(context);
        this.currentUserUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        this.editingMemoMap = new HashMap<>(targetPlant.getDateMemoMap());
    }

    private String getFormattedDate(CalendarDay day) {
        if (day == null) return null;
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", day.getYear(), day.getMonth(), day.getDay());
    }
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_plant_calendar, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        Button btnSettings = dialogView.findViewById(R.id.btnSettings);
        MaterialCalendarView mcv = dialogView.findViewById(R.id.calendarView);
        Button btnNutrient = dialogView.findViewById(R.id.btnNutrient);
        Button btnWater = dialogView.findViewById(R.id.btnWater);
        EditText etPlantMemo = dialogView.findViewById(R.id.etPlantMemo);

        tvTitle.setText(targetPlant.getName());
        refreshCalendarDecorators(mcv);

        mcv.setOnDateChangedListener((widget, date, selected) -> {
            String selectedDateStr = getFormattedDate(date);
            if (editingMemoMap.containsKey(selectedDateStr)) {
                etPlantMemo.setText(editingMemoMap.get(selectedDateStr));
            } else {
                etPlantMemo.setText("");
            }
        });

        Runnable captureCurrentMemo = () -> {
            CalendarDay currentSelectedDay = mcv.getSelectedDate();
            if (currentSelectedDay != null) {
                String dateStr = getFormattedDate(currentSelectedDay);
                String memoText = etPlantMemo.getText().toString().trim();
                if (!memoText.isEmpty()) {
                    editingMemoMap.put(dateStr, memoText);
                } else {
                    editingMemoMap.remove(dateStr);
                }
            }
        };

        btnNutrient.setOnClickListener(v -> {
            captureCurrentMemo.run();
            CalendarDay selected = mcv.getSelectedDate();
            if (selected == null) {
                Toast.makeText(context, "달력에서 날짜를 선택해 주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String dateStr = getFormattedDate(selected);

            if (targetPlant.getNutrientDatesStrList() == null) targetPlant.setNutrientDatesStrList(new ArrayList<>());

            if (!targetPlant.getNutrientDatesStrList().contains(dateStr)) {
                targetPlant.getNutrientDatesStrList().add(dateStr);
                if (targetPlant.getId() != null && !targetPlant.getId().isEmpty()) {
                    firestore.collection("users").document(currentUserUid).collection("plants").document(targetPlant.getId())
                            .update("nutrientDatesStrList", targetPlant.getNutrientDatesStrList())
                            .addOnSuccessListener(aVoid -> {
                                refreshCalendarDecorators(mcv);
                                Toast.makeText(context, "영양제 기록 완료", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        btnWater.setOnClickListener(v -> {
            captureCurrentMemo.run();
            CalendarDay today = mcv.getSelectedDate();
            if (today == null) {
                Toast.makeText(context, "달력에서 날짜를 선택해 주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String dateStr = getFormattedDate(today);

            if (targetPlant.getWateredDatesStrList() == null) targetPlant.setWateredDatesStrList(new ArrayList<>());

            if (!targetPlant.getWateredDatesStrList().contains(dateStr)) {
                targetPlant.getWateredDatesStrList().add(dateStr);
                if (targetPlant.getId() != null && !targetPlant.getId().isEmpty()) {
                    firestore.collection("users").document(currentUserUid).collection("plants").document(targetPlant.getId())
                            .update("wateredDatesStrList", targetPlant.getWateredDatesStrList())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "물주기 기록 완료", Toast.LENGTH_SHORT).show();
                                refreshCalendarDecorators(mcv);
                            });
                }
            }
        });

        btnSettings.setOnClickListener(v -> showSubCycleSettingDialog(mcv));

        builder.setView(dialogView);

        builder.setPositiveButton("닫기", (dialog, which) -> {
            captureCurrentMemo.run();
            saveAllDateMemos(editingMemoMap, null);
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button btnJustSave = dialogView.findViewById(R.id.btnJustSave);
        if (btnJustSave != null) {
            btnJustSave.setOnClickListener(v -> {
                captureCurrentMemo.run();
                saveAllDateMemos(editingMemoMap, mcv);
                Toast.makeText(context, "작성하신 내용이 저장되었습니다. 💾", Toast.LENGTH_SHORT).show();
            });
        }

        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#2E7D32"));
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#757575"));
        }

        mcv.setSelectedDate(CalendarDay.today());
        String todayStr = getFormattedDate(CalendarDay.today());
        if (editingMemoMap.containsKey(todayStr)) {
            etPlantMemo.setText(editingMemoMap.get(todayStr));
        }
    }

    private void saveAllDateMemos(Map<String, String> finalMemoMap, MaterialCalendarView mcv) {
        targetPlant.setDateMemoMap(finalMemoMap);

        new Thread(() -> {
            localDb.plantDao().update(targetPlant);
            if (targetPlant.getId() != null && !targetPlant.getId().isEmpty()) {
                firestore.collection("users").document(currentUserUid)
                        .collection("plants").document(targetPlant.getId())
                        .update("dateMemoMap", finalMemoMap)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firebase", "날짜별 일지 메모 완벽 백업 완료!");
                            if (mcv != null) {
                                mcv.post(() -> refreshCalendarDecorators(mcv));
                            }
                        });
            }
        }).start();
    }

    private void showSubCycleSettingDialog(MaterialCalendarView mcv) {
        AlertDialog.Builder subBuilder = new AlertDialog.Builder(context);
        subBuilder.setTitle("🗓물주기 주기 설정");
        LinearLayout subLayout = new LinearLayout(context);
        subLayout.setOrientation(LinearLayout.VERTICAL);
        subLayout.setPadding(60, 40, 60, 40);
        TextView tvGuide = new TextView(context);
        tvGuide.setText("이 식물은 며칠 주기로 물을 주나요?");
        tvGuide.setTextSize(13);
        tvGuide.setPadding(0, 0, 0, 20);
        subLayout.addView(tvGuide);
        EditText etCycleInput = new EditText(context);
        etCycleInput.setHint("숫자만 입력해주세요.");
        etCycleInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        subLayout.addView(etCycleInput);
        subBuilder.setView(subLayout);
        subBuilder.setPositiveButton("설정 반영", (dialog, which) -> {
            String inputText = etCycleInput.getText().toString().trim();
            if (inputText.isEmpty()) return;
            int cycleDays = Integer.parseInt(inputText);
            targetPlant.setWaterCycleDays(cycleDays);
            if (targetPlant.getId() != null && !targetPlant.getId().isEmpty()) {
                firestore.collection("users").document(currentUserUid).collection("plants").document(targetPlant.getId())
                        .update("waterCycleDays", cycleDays)
                        .addOnSuccessListener(aVoid -> {
                            refreshCalendarDecorators(mcv);
                            Toast.makeText(context, "주기가 설정되었습니다", Toast.LENGTH_SHORT).show();
                        });
            }
        });
        subBuilder.setNegativeButton("취소", null);

        AlertDialog subDialog = subBuilder.create();
        subDialog.show();

        Button positiveButton = subDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = subDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(Color.parseColor("#2E7D32"));
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.parseColor("#616161"));
        }
    }

    private void refreshCalendarDecorators(MaterialCalendarView mcv) {
        mcv.removeDecorators();

        if (targetPlant.getWateredDatesStrList() != null) {
            for (String dateStr : targetPlant.getWateredDatesStrList()) {
                try {
                    String[] parts = dateStr.split("-");
                    mcv.addDecorator(new CircleBackgroundDecorator(
                            CalendarDay.from(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                    ));
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        if (targetPlant.getNutrientDatesStrList() != null) {
            for (String dateStr : targetPlant.getNutrientDatesStrList()) {
                try {
                    String[] parts = dateStr.split("-");
                    CalendarDay day = CalendarDay.from(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    mcv.addDecorator(new NutrientTextColorDecorator(day, Color.parseColor("#74FE3E")));
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        if (targetPlant.getDateMemoMap() != null) {
            for (String dateStr : targetPlant.getDateMemoMap().keySet()) {
                try {
                    String[] parts = dateStr.split("-");
                    CalendarDay day = CalendarDay.from(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    mcv.addDecorator(new MemoUnderlineDecorator(day));
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        if (targetPlant.getWaterCycleDays() > 0) {
            List<CalendarDay> futureCalculatedDays = new ArrayList<>();
            Calendar baseCalendar = Calendar.getInstance();
            int cycle = targetPlant.getWaterCycleDays();
            for (int i = -30; i <= 180; i += cycle) {
                Calendar futureCal = (Calendar) baseCalendar.clone();
                futureCal.add(Calendar.DAY_OF_YEAR, i);
                futureCalculatedDays.add(CalendarDay.from(futureCal.get(Calendar.YEAR), futureCal.get(Calendar.MONTH) + 1, futureCal.get(Calendar.DAY_OF_MONTH)));
            }
            mcv.addDecorator(new RedDotDecorator(futureCalculatedDays));
        }
        mcv.invalidateDecorators();
    }

    private static class CircleBackgroundDecorator implements DayViewDecorator {
        private final CalendarDay day;
        private final GradientDrawable circleDrawable;
        public CircleBackgroundDecorator(CalendarDay day) {
            this.day = day;
            circleDrawable = new GradientDrawable();
            circleDrawable.setShape(GradientDrawable.OVAL);
            circleDrawable.setColor(Color.parseColor("#BBDEFB"));
        }
        @Override
        public boolean shouldDecorate(CalendarDay day) { return day.equals(this.day); }
        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(circleDrawable);
            view.addSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#0D47A1")));
        }
    }

    private static class NutrientTextColorDecorator implements DayViewDecorator {
        private final CalendarDay day;
        private final int color;
        public NutrientTextColorDecorator(CalendarDay day, int color) { this.day = day; this.color = color; }
        @Override
        public boolean shouldDecorate(CalendarDay day) { return day.equals(this.day); }
        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new android.text.style.ForegroundColorSpan(color));
            view.addSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD));
        }
    }

    private static class MemoUnderlineDecorator implements DayViewDecorator {
        private final CalendarDay day;
        public MemoUnderlineDecorator(CalendarDay day) {
            this.day = day;
        }
        @Override
        public boolean shouldDecorate(CalendarDay day) { return day.equals(this.day); }
        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new UnderlineSpan());
        }
    }

    private static class RedDotDecorator implements DayViewDecorator {
        private final HashSet<CalendarDay> dates;
        public RedDotDecorator(List<CalendarDay> dates) { this.dates = new HashSet<>(dates); }
        @Override
        public boolean shouldDecorate(CalendarDay day) { return dates.contains(day); }
        @Override
        public void decorate(DayViewFacade view) { view.addSpan(new DotSpan(8, Color.RED)); }
    }
}