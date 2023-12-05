package com.example.moblab1;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {



    // Create the object of TextView and PieChart class
    TextView tvR, tvPython, tvCPP;
    PieChart pieChart;

    File file;
    private EditText initialDepositEditText;
    private EditText periodicDepositEditText;
    private Spinner depositProgramSpinner;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFile();
        initializeViews();
        setListeners();
        restoreSavedValues();
    }

    public void initFile(){
        File directory = Environment.getExternalStorageDirectory();
        file = new File(directory,"data1");
        if (!file.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= 30){
                    if (!Environment.isExternalStorageManager()){
                        Intent getpermission = new Intent();
                        getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(getpermission);
                    }
                }
                file.createNewFile();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void saveValues(HashMap<String, String> values) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            for (Map.Entry<String, String> entry : values.entrySet()) {
                osw.write(entry.getKey() + ":" + entry.getValue() + "\n");
            }

            osw.flush();
            osw.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> getSavedValues() {
        HashMap<String, String> savedValues = new HashMap();
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    savedValues.put(parts[0], parts[1]);
                    System.out.println(parts[1]);
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return savedValues;
    }

    private void initializeViews() {
        pieChart = findViewById(R.id.piechart);
        pieChart.startAnimation();

        initialDepositEditText = findViewById(R.id.initialDeposit);
        periodicDepositEditText = findViewById(R.id.periodicDeposit);
        depositProgramSpinner = findViewById(R.id.depositProgramSpinner);

        resultTextView = findViewById(R.id.depositResult);
    }

    private void setListeners() {
        depositProgramSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedPlan = depositProgramSpinner.getSelectedItem().toString();
                if (selectedPlan.equals("Одноразовий")){
                    periodicDepositEditText.setVisibility(View.INVISIBLE);
                }
                else{
                    periodicDepositEditText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing if nothing is selected.
            }
        });

        Button calculateDepositButton = findViewById(R.id.calculateDepositButton);
        calculateDepositButton.setOnClickListener(v -> calculateResult());

        Button navigateButton = findViewById(R.id.navigateButton);
        navigateButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            startActivity(intent);
        });
    }




    private void restoreSavedValues() {
        HashMap<String,String> savedValues = getSavedValues();
        initialDepositEditText.setText(savedValues.get("Початковий внесок"));
        periodicDepositEditText.setText(savedValues.get("Щомісячний внесок"));

        try{
            String savedPlan = savedValues.get("Обраний план");
            int planPosition = ((ArrayAdapter<String>) depositProgramSpinner.getAdapter()).getPosition(savedPlan);
            depositProgramSpinner.setSelection(planPosition);
            if (depositProgramSpinner.getSelectedItem().toString().equals("Одноразовий")){
                 periodicDepositEditText.setVisibility(View.INVISIBLE);
            }
            else{
                 periodicDepositEditText.setVisibility(View.VISIBLE);
            }
        }
        catch(Exception e){
            depositProgramSpinner.setSelection(0);
            periodicDepositEditText.setVisibility(View.INVISIBLE);
        }
    }

    private void calculateResult() {
        String selectedPlan = depositProgramSpinner.getSelectedItem().toString();
        int initialDeposit = Integer.valueOf(initialDepositEditText.getText().toString());
        int periodicDeposit = 0;
        double result = initialDeposit;
        String stringResult = "";
        if (selectedPlan.equals("Одноразовий")){
            for (int i = 0; i < 12; i++) {
                result *=  1.0125;
            }
            refreshPie(initialDeposit,0,(int)result);
            stringResult = String.valueOf((int)result);
        }
        else{
            periodicDeposit = Integer.valueOf(periodicDepositEditText.getText().toString());
            for (int i = 0; i < 12; i++) {
                result *= 1.0125;
                if (i<11){
                    result += periodicDeposit;
                }
            }
            refreshPie(initialDeposit,periodicDeposit,(int)result);
            stringResult = String.valueOf((int)result);
            result = initialDeposit;
            for (int i = 0; i < 12; i++) {
                result *=  1.0125;
            }
            stringResult += ", без щомісячних внесків:" + String.valueOf((int)result);
        }

        HashMap<String,String> hashMap = new HashMap();

        hashMap.put("Початковий внесок", String.valueOf(initialDeposit));
        hashMap.put("Щомісячний внесок", String.valueOf(periodicDeposit));
        hashMap.put("Обраний план", String.valueOf(selectedPlan));

        saveValues(hashMap);

        resultTextView.setText(stringResult);
    }

    void refreshPie(int initialDeposit,int periodicDeposit,int result){
        pieChart.clearChart();
        pieChart.addPieSlice(new PieModel("Початковий внесок", initialDeposit, getColorForOperator("0")));
        pieChart.addPieSlice(new PieModel("Щомісячний внесок", periodicDeposit*11, getColorForOperator("1")));
        pieChart.addPieSlice(new PieModel("Відсотки", result - initialDeposit - periodicDeposit*11, getColorForOperator("2")));
    }

    private int getColorForOperator(String operator) {
        switch (operator) {
            case "0":
                return Color.parseColor("#FFA726");  // Orange
            case "1":
                return Color.parseColor("#66BB6A");  // Green
            case "2":
                return Color.parseColor("#EF5350");  // Red
            default:
                return Color.BLACK;
        }
    }
}
