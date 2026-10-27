package com.abujabal.tastir;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;
    private static final String PASS = "1981";
    private EditText etSpeed, etLines, etSep, etWord, etSentence;
    private TextView tvSentences;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            prefs = getSharedPreferences("tastir", Context.MODE_PRIVATE);
            setContentView(R.layout.activity_settings);
            bind();
        } catch (Throwable t) {
            showError(t);
        }
    }

    private void bind() {
        etSpeed = (EditText) findViewById(R.id.et_speed);
        etLines = (EditText) findViewById(R.id.et_lines);
        etSep = (EditText) findViewById(R.id.et_sep);
        etWord = (EditText) findViewById(R.id.et_word);
        etSentence = (EditText) findViewById(R.id.et_sentence);
        tvSentences = (TextView) findViewById(R.id.tv_sentences);
        etSpeed.setText(String.valueOf(prefs.getInt("type_speed", 60)));
        etLines.setText(String.valueOf(prefs.getInt("send_lines", 2)));
        etSep.setText(prefs.getString("separator", " "));
        etWord.setText(prefs.getString("swear_word", "\u0643\u0633\u0645\u0643"));
        showSentences();
        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveAll(); }
        });
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { askPassword(); }
        });
        findViewById(R.id.btn_enable).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        findViewById(R.id.btn_choose).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });
    }

    private void showError(Throwable t) {
        TextView tv = new TextView(this);
        tv.setPadding(30, 60, 30, 30);
        tv.setTextSize(14);
        tv.setText("Error:\n" + t.toString());
        try { setContentView(tv); } catch (Throwable ignored) { }
    }

    private void saveAll() {
        int speed = parseInt(etSpeed.getText().toString(), 60);
        int lines = parseInt(etLines.getText().toString(), 2);
        if (speed < 1) speed = 1;
        if (speed > 500) speed = 500;
        if (lines < 1) lines = 1;
        if (lines > 90) lines = 90;
        String sep = etSep.getText().toString();
        if (sep.isEmpty()) sep = " ";
        String word = etWord.getText().toString().trim();
        if (word.isEmpty()) word = "\u0643\u0633\u0645\u0643";
        prefs.edit()
                .putInt("type_speed", speed)
                .putInt("send_lines", lines)
                .putString("separator", sep)
                .putString("swear_word", word)
                .apply();
        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
    }

    private void askPassword() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Password");
        new AlertDialog.Builder(this)
                .setTitle("Lock")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        if (PASS.equals(input.getText().toString())) addSentence();
                        else Toast.makeText(SettingsActivity.this, "Wrong", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addSentence() {
        String s = etSentence.getText().toString().trim();
        if (s.isEmpty()) {
            Toast.makeText(this, "Write first", Toast.LENGTH_SHORT).show();
            return;
        }
        String old = prefs.getString("sentences", "");
        String content = old.trim().isEmpty() ? s : old.trim() + "\n" + s;
        prefs.edit().putString("sentences", content).apply();
        etSentence.setText("");
        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
        showSentences();
    }

    private void showSentences() {
        String s = prefs.getString("sentences", "");
        tvSentences.setText(s.trim().isEmpty() ? "No sentences yet" : s);
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }
  }
