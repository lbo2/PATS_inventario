package cl.pats.envy15.pats_inventario;

import android.app.ListActivity;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ListaActivity extends ListActivity {

    public static final String TAG = ListaActivity.class.getSimpleName();

    private Bien mBien;

    private TextView mSinRegistro;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);

        mSinRegistro = (TextView) findViewById(R.id.txtSinRegistro);
        mProgressBar = (ProgressBar) findViewById(android.R.id.empty);

        Bundle extras = getIntent().getExtras();
        String jsonData;

        if (extras.equals("")) {
            jsonData = "";
        }
        else
            jsonData= extras.getString("jsonData");

        //Toast.makeText(this, jsonData,Toast.LENGTH_SHORT).show();

        try {
            final AdaptadorBien adapter;
            adapter = new AdaptadorBien(ListaActivity.this, getBienes(jsonData));
            Handler handler = new Handler(Looper.getMainLooper());

            if (getBienes(jsonData) != null) {
                handler.postDelayed(new Runnable() {

                    public void run() {
                        setListAdapter(adapter);
                    }
                }, 500);
            } else {
                handler.postDelayed(new Runnable() {

                    public void run() {
                        mSinRegistro.setText("No hay registros");
                    }
                }, 500);
            }
        }catch (JSONException e) {
            Log.e(TAG, "error Json");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {

                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                    mSinRegistro.setText("No hay registros");
                }
            }, 500);
        }
    }

    private Bien[] getBienes(String jsonData) throws JSONException {
        JSONObject resultado = new JSONObject(jsonData);
        JSONArray data = resultado.getJSONArray("bienes");

        Bien[] bienes = new Bien[data.length()];

        for (int i=0; i<data.length(); i++){
            JSONObject jsonBien = data.getJSONObject(i);
            Bien bien = new Bien();

            bien.setIdBien(jsonBien.getString("id_bien"));
            bien.setArticulo(jsonBien.getString("valor"));
            bien.setMicropunto(jsonBien.getString("micropunto"));
            bien.setSelloPats(jsonBien.getString("sello_pats"));
            bien.setSucursal(jsonBien.getString("ubi_1"));

            bienes[i]=bien;
        }
        return bienes;
    }
}
