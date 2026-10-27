package com.abujabal.tastir;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class TastirIME extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard kbAr, kbEn, kbSym, kbEmoji;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private Runnable holdRunnable;
    private volatile boolean autoOn = false;
    private volatile boolean holdFired = false;
    private int lettersMode = 0;
    private int mode = 0;
    private final ArrayList<String> used = new ArrayList<>();

    @Override
    public View onCreateInputView() {
        prefs = getSharedPreferences("tastir", Context.MODE_PRIVATE);
        WordSync.fetch(this, null);
        View v = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        kv = (KeyboardView) v.findViewById(R.id.keyboard_view);
        kbAr = new Keyboard(this, R.xml.qwerty_ar);
        kbEn = new Keyboard(this, R.xml.qwerty_en);
        kbSym = new Keyboard(this, R.xml.symbols);
        kbEmoji = new Keyboard(this, R.xml.emoji);
        lettersMode = prefs.getInt("letters_mode", 0);
        mode = lettersMode;
        kv.setKeyboard(lettersMode == 0 ? kbAr : kbEn);
        kv.setOnKeyboardActionListener(this);
        return v;
    }

    private void setKb(Keyboard k, int m) {
        mode = m;
        if (kv != null) { kv.setKeyboard(k); kv.invalidate(); }
    }

    private void backToLetters() {
        setKb(lettersMode == 0 ? kbAr : kbEn, lettersMode);
    }

    private ArrayList<String> words() {
        ArrayList<String> list = new ArrayList<>();
        String shared = prefs.getString("shared_words", "");
        if (shared != null) {
            for (String s : shared.split("\n")) {
                if (!s.trim().isEmpty()) list.add(s.trim());
            }
        }
        String extra = prefs.getString("extra_words", "");
        if (extra != null) {
            for (String s : extra.split("\n")) {
                if (!s.trim().isEmpty()) list.add(s.trim());
            }
        }
        if (list.isEmpty()) list.add("\u0643\u0633\u0645\u0643");
        return list;
    }

    private String makeSentence() {
        String sentences = prefs.getString("sentences", "");
        String sep = prefs.getString("separator", " ");
        if (sep == null) sep = " ";
        String out = "";
        for (int a = 0; a < 6; a++) {
            if (sentences != null && !sentences.trim().isEmpty()) {
                String[] arr = sentences.split("\n");
                out = arr[random.nextInt(arr.length)].trim();
            } else {
                ArrayList<String> pool = words();
                int len = 3 + random.nextInt(3);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    if (i > 0) sb.append(sep);
                    sb.append(pool.get(random.nextInt(pool.size())));
                }
                out = sb.toString();
            }
            if (!used.contains(out)) break;
        }
        used.add(out);
        if (used.size() > 500) used.remove(0);
        return out;
    }

    private void commit(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(s, 1);
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void sendAction() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        int action = (ei != null)
                ? (ei.imeOptions & EditorInfo.IME_MASK_ACTION)
                : EditorInfo.IME_ACTION_NONE;
        if (action != EditorInfo.IME_ACTION_NONE
                && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action);
        } else {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    private void startAuto() {
        if (autoOn) return;
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null || info.packageName == null) return;
        autoOn = true;
        final String pkg = info.packageName;
        Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                while (autoOn) {
                    EditorInfo ei = getCurrentInputEditorInfo();
                    if (ei == null || ei.packageName == null || !pkg.equals(ei.packageName)) {
                        autoOn = false;
                        break;
                    }
                    final int lines = prefs.getInt("send_lines", 2);
                    final int speed = prefs.getInt("type_speed", 60);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < lines; i++) {
                        if (i > 0) sb.append("\n");
                        sb.append(makeSentence());
                    }
                    final String msg = sb.toString();
                    handler.post(new Runnable() { public void run() { commit(msg); } });
                    sleep(speed);
                    handler.post(new Runnable() { public void run() { sendAction(); } });
                    sleep(speed);
                }
            }
        }).start();
    }

    private void stopAuto() {
        if (autoOn) {
            autoOn = false;
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPress(int primaryCode) {
        if (primaryCode == -104) {
            holdRunnable = new Runnable() {
                public void run() { holdFired = true; startAuto(); }
            };
            handler.postDelayed(holdRunnable, 2000);
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        if (holdRunnable != null) {
            handler.removeCallbacks(holdRunnable);
            holdRunnable = null;
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (primaryCode == -104) {
            if (holdFired) { holdFired = false; return; }
            if (autoOn) { stopAuto(); return; }
            lettersMode = (lettersMode == 0) ? 1 : 0;
            prefs.edit().putInt("letters_mode", lettersMode).apply();
            backToLetters();
            return;
        }
        if (primaryCode == -105) {
            if (mode == 2) backToLetters();
            else setKb(kbSym, 2);
            return;
        }
        if (primaryCode == -108) { setKb(kbEmoji, 4); return; }
        if (primaryCode == -107) { backToLetters(); return; }
        if (primaryCode == -1 || primaryCode == Keyboard.KEYCODE_DELETE) {
            ic.deleteSurroundingText(1, 0);
            return;
        }
        if (primaryCode == 10) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            return;
        }
        commit(String.valueOf((char) primaryCode));
    }

    @Override public void onFinishInput() { autoOn = false; super.onFinishInput(); }
    @Override public void onText(CharSequence text) { commit(text.toString()); }
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeUp() {}
    @Override public void swipeDown() { requestHideSelf(0); }
          }
