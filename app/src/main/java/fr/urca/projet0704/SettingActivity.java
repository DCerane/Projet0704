package fr.urca.projet0704;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {

    private EditText adresse;
    private EditText port;
    private Button valider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        adresse = findViewById(R.id.adresse);
        port = findViewById(R.id.port);
        valider = findViewById(R.id.valider);

        valider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adresse.getText() == null)
                    Toast.makeText(SettingActivity.this, "Pas d'adresse IP !", Toast.LENGTH_LONG).show();
                else if(port.getText() == null)
                    Toast.makeText(SettingActivity.this, "Pas de port !", Toast.LENGTH_LONG).show();
                else {
                    SharedPreferences sp = getSharedPreferences("sp", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.clear();
                    editor.putString("adresse", String.valueOf(adresse.getText()));
                    editor.putInt("port", Integer.parseInt(String.valueOf(port.getText())));
                    editor.apply();
                    startActivity(new Intent(SettingActivity.this, ReleveActivity.class));
                }
            }
        });
    }
}