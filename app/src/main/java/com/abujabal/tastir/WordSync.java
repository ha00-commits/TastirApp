package com.abujabal.tastir;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WordSync {

    public static final String GIST_ID = "3e630c98639d108e2b46f4d787e49751";
    public static final String TOKEN = "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";
    private static final String FILE_NAME = "words.txt";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("tastir", Context.MODE_PRIVATE);
    }

    public static String getLocal(Context c) {
        return prefs(c).getString("shared_words", "");
    }

    public static void setLocal(Context c, String v) {
        prefs(c).edit().putString("shared_words", v).apply();
    }

    public static void fetch(final Context c, final Runnable done) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    URL u = new URL("https://api.github.com/gists/" + GIST_ID);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "token " + TOKEN);
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    if (conn.getResponseCode() == 200) {
                        String body = read(conn.getInputStream());
                        JSONObject obj = new JSONObject(body);
                        JSONObject files = obj.getJSONObject("files");
                        JSONObject file = files.getJSONObject(FILE_NAME);
                        setLocal(c, file.getString("content"));
                    }
                } catch (Exception e) {
                } finally {
                    if (done != null) done.run();
                }
            }
        }).start();
    }

    public static void push(final Context c, final String content, final Runnable done) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    URL u = new URL("https://api.github.com/gists/" + GIST_ID);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setRequestMethod("PATCH");
                    conn.setRequestProperty("Authorization", "token " + TOKEN);
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    JSONObject fileObj = new JSONObject();
                    fileObj.put("content", content);
                    JSONObject files = new JSONObject();
                    files.put(FILE_NAME, fileObj);
                    JSONObject root = new JSONObject();
                    root.put("files", files);
                    OutputStream os = conn.getOutputStream();
                    os.write(root.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();
                    conn.getResponseCode();
                    setLocal(c, content);
                } catch (Exception e) {
                } finally {
                    if (done != null) done.run();
                }
            }
        }).start();
    }

    private static String read(InputStream is) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();
        return sb.toString();
    }
          }
