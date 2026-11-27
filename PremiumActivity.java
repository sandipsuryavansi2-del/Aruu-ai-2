package com.aruu.ai2;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class PremiumActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);
        findViewById(R.id.btnBackPremium).setOnClickListener(v -> finish());
    }
}
