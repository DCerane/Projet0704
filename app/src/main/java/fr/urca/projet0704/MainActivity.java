package fr.urca.projet0704;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    // Attributs pour l'affichage des données
    public final int MAX_POINTS = 11;
    private TabLayout tabLayout;
    private TextView textView;
    private GraphView graphView;
    private LineGraphSeries<DataPoint> series;
    private int pos_x = 0;

    private int[] capteurs;

    // Attributs pour la communication avec serveur
    public final String ADRESSE = "192.168.1.11"; // IPv4 locale (cmd:> ipconfig)
    public final int PORT = 8888;
    private Thread client;

    /*
    1) Le serveur envoi le nombre de fichiers/capteurs :
       { "nb_capteurs": int }
    2) Le serveur envoi les fichiers par ordre croissant d'ID/nom :
       { "nom_fichier": string (ID), "donnees": [{"timestamp": int, "temperature": double }, ...] }
    3) Le serveur envoi les nouveaux relevés en objets JSON :
       { "timestamp": int, "ID": int, "temperature": double }
    */

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des Views
        tabLayout = findViewById(R.id.capteurs);
        textView = findViewById(R.id.temperature);
        graphView = findViewById(R.id.graph);
        graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.BOTH);
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(10);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(0);
        graphView.getViewport().setMaxY(40);
        series = new LineGraphSeries<>();
        series.setBackgroundColor(R.color.fire_red);
        series.setColor(Color.argb(255, 255, 64, 0));
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(10);
        series.setThickness(8);

        // Création du thread client
        client = new Thread(() -> {
            try {
                // Connexion au serveur
                Socket socket = new Socket(ADRESSE, PORT);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.i("client", "Socket et BufferedReader créés");

                // Réception des capteurs
                capteurs = new int[Integer.parseInt(bufferedReader.readLine())];
                for(int i=0; i<capteurs.length; i++) {
                    int id = Integer.parseInt(bufferedReader.readLine());
                    capteurs[i] = id;
                    // Ajout du capteur dans l'UI
                    runOnUiThread(() -> tabLayout.addTab(tabLayout.newTab().setId(id).setText(String.valueOf(id))));
                }
                Log.i("client", "Capteurs : "+ Arrays.toString(capteurs));

                // Lecture des relevés
                while(!socket.isClosed()) {
                    try {
                        String input = bufferedReader.readLine();
                        if(input != null) {
                            Log.i("client", "Message reçu : "+input);
                            try {
                                // Récupération du relevé
                                JSONObject releve = new JSONObject(input);
                                int id = releve.getInt("id");
                                double temperature = releve.getDouble("temperature");
                                // Actualisation des relevés
                                runOnUiThread(() -> {
                                    textView.setText(id+":"+temperature+"°C");
                                    series.appendData(new DataPoint(pos_x++, temperature), (pos_x > 10), MAX_POINTS);
                                    graphView.removeAllSeries();
                                    graphView.addSeries(series);
                                });
                            } catch( JSONException e) {
                                Log.e("client", "Erreur lors de la conversion en JSON : "+e);
                                System.exit(-1);
                            }
                        } else {
                            Log.i("client", "Pas de message reçu");
                            try {
                                socket.close();
                                bufferedReader.close();
                            } catch(IOException e) {
                                Log.e("client", "Erreur lors de la fermeture de la Socket et du BufferedReader : "+e);
                                System.exit(-1);
                            }
                        }
                    } catch(IOException e) {
                        Log.e("client", "Erreur lors de la lecture du BufferedReader : "+e);
                        System.exit(-1);
                    }
                }
                // Fermeture de l'application
                finish();
            } catch (IOException e) {
                Log.e("client", "Erreur lors de l'initialisation de la Socket et du BufferedReader : "+e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erreur lors de l'initialisation de la Socket et du BufferedReader : " + e, Toast.LENGTH_LONG).show());
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                System.exit(-1);
            }
        });

        Log.i("onCreate", "Terminé");
    }

    @Override
    protected void onStart() {
        super.onStart();
        client.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(client.isAlive())
            client.interrupt();
    }
}