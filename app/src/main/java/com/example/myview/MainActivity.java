package com.example.myview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GraphicsVerifyView view = findViewById(R.id.gvv);
        Button button = findViewById(R.id.btn);
        view.setListener(new GraphicsVerifyView.VerifyListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "成功",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail() {
                Toast.makeText(MainActivity.this, "失败",Toast.LENGTH_SHORT).show();
                button.setVisibility(View.VISIBLE);
            }
        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.reset();
                button.setVisibility(View.GONE);
            }
        });
    }
}