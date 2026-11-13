package com.aichat.app;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private EditText etInput;
    private SharedPreferences prefs;
    private OkHttpClient client;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String apiKey = "";
    private String baseUrl = "https://api.openai.com";
    private String model = "gpt-3.5-turbo";
    private JSONArray conversationHistory = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("ai_chat", MODE_PRIVATE);
        apiKey = prefs.getString("api_key", "");
        baseUrl = prefs.getString("base_url", "https://api.openai.com");
        model = prefs.getString("model", "gpt-3.5-turbo");

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        recyclerView = findViewById(R.id.recyclerView);
        etInput = findViewById(R.id.etInput);
        Button btnSend = findViewById(R.id.btnSend);
        ImageButton btnSettings = findViewById(R.id.btnSettings);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        adapter = new ChatAdapter(messages);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        if (apiKey.isEmpty()) showSettingsDialog();
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập API Key!", Toast.LENGTH_LONG).show();
            showSettingsDialog();
            return;
        }

        messages.add(new Message(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);
        etInput.setText("");

        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", text);
            conversationHistory.put(userMsg);
        } catch (Exception e) { e.printStackTrace(); }

        messages.add(new Message("\u23F3 \u0110ang suy ngh\u0129...", false));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);
        int typingIndex = messages.size() - 1;

        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", conversationHistory);
            body.put("max_tokens", 2048);
            body.put("temperature", 0.7);

            String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            url += "v1/chat/completions";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(),
                            MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> {
                        messages.set(typingIndex,
                            new Message("\u274C L\u1ED7i: " + e.getMessage(), false));
                        adapter.notifyItemChanged(typingIndex);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String rb = response.body().string();
                        JSONObject json = new JSONObject(rb);
                        String reply = "";
                        if (json.has("choices")) {
                            JSONArray ch = json.getJSONArray("choices");
                            if (ch.length() > 0)
                                reply = ch.getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                        } else if (json.has("error")) {
                            reply = "\u274C " + json.getJSONObject("error")
                                .getString("message");
                        } else {
                            reply = "\u274C Ph\u1EA3n h\u1ED3i kh\u00F4ng h\u1EE3p l\u1EC7";
                        }

                        JSONObject aiMsg = new JSONObject();
                        aiMsg.put("role", "assistant");
                        aiMsg.put("content", reply);
                        conversationHistory.put(aiMsg);

                        final String fr = reply;
                        mainHandler.post(() -> {
                            messages.set(typingIndex, new Message(fr, false));
                            adapter.notifyItemChanged(typingIndex);
                            recyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            messages.set(typingIndex,
                                new Message("\u274C L\u1ED7i: " + e.getMessage(), false));
                            adapter.notifyItemChanged(typingIndex);
                        });
                    }
                }
            });
        } catch (Exception e) {
            messages.set(typingIndex,
                new Message("\u274C L\u1ED7i: " + e.getMessage(), false));
            adapter.notifyItemChanged(typingIndex);
        }
    }

    private void showSettingsDialog() {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_settings, null);
        EditText etKey = dv.findViewById(R.id.etApiKey);
        EditText etUrl = dv.findViewById(R.id.etBaseUrl);
        EditText etMod = dv.findViewById(R.id.etModel);
        etKey.setText(apiKey);
        etUrl.setText(baseUrl);
        etMod.setText(model);

        new AlertDialog.Builder(this)
            .setTitle("\u2699\uFE0F C\u00E0i \u0111\u1EB7t API")
            .setView(dv)
            .setPositiveButton("L\u01B0u", (d, w) -> {
                apiKey = etKey.getText().toString().trim();
                baseUrl = etUrl.getText().toString().trim();
                model = etMod.getText().toString().trim();
                if (baseUrl.endsWith("/"))
                    baseUrl = baseUrl.substring(0, baseUrl.length()-1);
                prefs.edit()
                    .putString("api_key", apiKey)
                    .putString("base_url", baseUrl)
                    .putString("model", model)
                    .apply();
                conversationHistory = new JSONArray();
                messages.clear();
                messages.add(new Message(
                    "\u2705 \u0110\u00E3 k\u1EBFt n\u1ED1i! Model: " + model
                    + "\n\uD83C\uDF10 " + baseUrl
                    + "\n\nH\u00E3y b\u1EAFt \u0111\u1EA7u tr\u00F2 chuy\u1EC7n!",
                    false));
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "\u0110\u00E3 l\u01B0u!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("H\u1EE7y", null)
            .show();
    }
}
