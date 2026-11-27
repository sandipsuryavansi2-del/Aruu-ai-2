package com.aruu.ai2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = new Prefs(this);

        TextView tvKey = findViewById(R.id.tvCurrentKey);
        EditText etKey = findViewById(R.id.etNewKey);
        EditText etText = findViewById(R.id.etTextModel);
        EditText etImage = findViewById(R.id.etImageModel);
        EditText etVideo = findViewById(R.id.etVideoModel);
        EditText etName = findViewById(R.id.etName);
        Button btnSave = findViewById(R.id.btnSaveSettings);
        Button btnClear = findViewById(R.id.btnClearChat);
        Button btnRemove = findViewById(R.id.btnRemoveKey);

        tvKey.setText(getString(R.string.settings_key_now) + " " + mask(prefs.getApiKey()));
        etText.setText(prefs.getTextModel());
        etImage.setText(prefs.getImageModel());
        etVideo.setText(prefs.getVideoModel());
        etName.setText(prefs.getName());

        btnSave.setOnClickListener(v -> {
            String newKey = etKey.getText().toString().trim();
            if (!newKey.isEmpty()) {
                if (newKey.length() < 20) {
                    Toast.makeText(this, "Sahi Gemini API key dalo", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.setApiKey(newKey);
            }
            prefs.setTextModel(etText.getText().toString());
            prefs.setImageModel(etImage.getText().toString());
            prefs.setVideoModel(etVideo.getText().toString());
            prefs.setName(etName.getText().toString());
            Toast.makeText(this, "Settings save ho gayi", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnClear.setOnClickListener(v -> {
            prefs.saveChat(new ArrayList<>());
            Toast.makeText(this, "Chat saaf ho gayi", Toast.LENGTH_SHORT).show();
        });

        btnRemove.setOnClickListener(v -> {
            prefs.setApiKey("");
            Intent i = new Intent(this, ApiKeyActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private static String mask(String k) {
        if (k == null || k.length() < 10) return "(nahi hai)";
        return k.substring(0, 4) + "..." + k.substring(k.length() - 4);
    }
}
