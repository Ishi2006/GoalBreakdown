package com.example.goalbreakdown;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // UI Components - Exact IDs from XML
    private EditText goalInput, etStepInput;
    private Button generateBtn, addStepBtn;
    private ProgressBar customProgressBar;
    private TextView percentageText, motivationalText, tvEmptyState;
    private ListView stepsList;

    // Data
    private ArrayList<StepItem> stepsDataList;
    private StepsAdapter stepsAdapter;
    private int lastProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize all views
        initViews();

        // 2. Setup button listeners
        setupClickListeners();

        // 3. Load saved progress
        loadDataFromStorage();
    }

    private void initViews() {
        goalInput = findViewById(R.id.goalInput);
        etStepInput = findViewById(R.id.etStepInput);
        generateBtn = findViewById(R.id.generateBtn);
        addStepBtn = findViewById(R.id.addStepBtn);
        customProgressBar = findViewById(R.id.customProgressBar);
        percentageText = findViewById(R.id.percentageText);
        motivationalText = findViewById(R.id.motivationalText);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        stepsList = findViewById(R.id.stepsList);

        stepsDataList = new ArrayList<>();
        stepsAdapter = new StepsAdapter(this, stepsDataList);
        stepsList.setAdapter(stepsAdapter);

        // Long click to delete a step
        stepsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(position);
                return true;
            }
        });
    }

    private void setupClickListeners() {
        // GENERATE PLAN BUTTON
        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String goal = goalInput.getText().toString().trim();
                if (goal.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a goal first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                processGeneratePlan(goal.toLowerCase());
            }
        });

        // ADD STEP BUTTON
        addStepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stepText = etStepInput.getText().toString().trim();
                if (stepText.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Describe your step!", Toast.LENGTH_SHORT).show();
                    return;
                }
                processAddManualStep(stepText);
            }
        });
    }

    private void processGeneratePlan(String goal) {
        stepsDataList.clear(); // Clear previous steps

        if (goal.contains("learn")) {
            stepsDataList.add(new StepItem("Research basics", false));
            stepsDataList.add(new StepItem("Watch tutorials", false));
            stepsDataList.add(new StepItem("Practical exercise", false));
            stepsDataList.add(new StepItem("Build project", false));
        } else if (goal.contains("fitness")) {
            stepsDataList.add(new StepItem("Buy equipment", false));
            stepsDataList.add(new StepItem("Set diet plan", false));
            stepsDataList.add(new StepItem("Daily cardio", false));
            stepsDataList.add(new StepItem("Track calories", false));
        } else {
            stepsDataList.add(new StepItem("Research goal", false));
            stepsDataList.add(new StepItem("Plan timeline", false));
            stepsDataList.add(new StepItem("Execution", false));
            stepsDataList.add(new StepItem("Final review", false));
        }

        goalInput.setText("");
        hideKeyboard();
        updateUI("Plan Generated! 🚀");
    }

    private void processAddManualStep(String text) {
        int nextNum = stepsDataList.size() + 1;
        stepsDataList.add(new StepItem("Step " + nextNum + ": " + text, false));
        etStepInput.setText(""); // Clear step input only
        hideKeyboard();
        updateUI("Step Added! ✅");
    }

    public void updateUI(String message) {
        stepsAdapter.notifyDataSetChanged();

        // Show/Hide Empty State Message
        if (stepsDataList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }

        // Calculate Progress
        int total = stepsDataList.size();
        int done = 0;
        for (StepItem item : stepsDataList) {
            if (item.isCompleted) done++;
        }

        int progress = (total == 0) ? 0 : (done * 100) / total;

        // Animate ProgressBar
        ObjectAnimator.ofInt(customProgressBar, "progress", lastProgress, progress)
                .setDuration(500)
                .start();
        lastProgress = progress;

        // Update Labels
        percentageText.setText(progress + "%");
        motivationalText.setText(done + " of " + total + " steps completed");

        // Color Logic
        if (progress <= 33) customProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF5252")));
        else if (progress <= 66) customProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FFB300")));
        else customProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#00C853")));

        if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        saveDataToStorage();
    }

    private void confirmDelete(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Step")
                .setMessage("Remove this step from your plan?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    stepsDataList.remove(position);
                    updateUI("Step removed");
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // --- Persistence ---

    private void saveDataToStorage() {
        SharedPreferences prefs = getSharedPreferences("GoalPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();
        for (StepItem item : stepsDataList) {
            sb.append(item.text).append("|").append(item.isCompleted).append(",");
        }
        editor.putString("steps", sb.toString());
        editor.apply();
    }

    private void loadDataFromStorage() {
        SharedPreferences prefs = getSharedPreferences("GoalPrefs", MODE_PRIVATE);
        String data = prefs.getString("steps", "");
        if (!data.isEmpty()) {
            String[] items = data.split(",");
            for (String s : items) {
                String[] p = s.split("\\|");
                if (p.length == 2) {
                    stepsDataList.add(new StepItem(p[0], Boolean.parseBoolean(p[1])));
                }
            }
            updateUI(null);
        }
    }

    // --- Adapter ---

    private class StepsAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<StepItem> list;

        StepsAdapter(Context context, ArrayList<StepItem> list) {
            this.context = context;
            this.list = list;
        }

        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int pos) { return list.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_step, parent, false);
            }

            final StepItem item = list.get(position);
            CheckBox cb = convertView.findViewById(android.R.id.text1);

            // Handle Strikethrough
            if (item.isCompleted) {
                SpannableString ss = new SpannableString(item.text);
                ss.setSpan(new StrikethroughSpan(), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                cb.setText(ss);
                cb.setAlpha(0.6f);
            } else {
                cb.setText(item.text);
                cb.setAlpha(1.0f);
            }

            cb.setOnCheckedChangeListener(null);
            cb.setChecked(item.isCompleted);
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.isCompleted = isChecked;
                updateUI(null);
            });

            return convertView;
        }
    }

    private static class StepItem {
        String text;
        boolean isCompleted;
        StepItem(String t, boolean c) { text = t; isCompleted = c; }
    }
}
