package com.aruu.ai2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ApiKeyActivity extends AppCompatActivity {

    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        if (prefs.hasApiKey()) {
            openMain();
            return;
        }
        setContentView(R.layout.activity_api_key);

        EditText etKey = findViewById(R.id.etApiKey);
        Button btnSave = findViewById(R.id.btnSaveKey);
        btnSave.setOnClickListener(v -> {
            String key = etKey.getText().toString().trim();
            if (key.length() < 20) {
                Toast.makeText(this, "Sahi Gemini API key dalo", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.setApiKey(key);
            openMain();
        });
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
