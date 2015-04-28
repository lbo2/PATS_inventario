package cl.pats.envy15.pats_inventario;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class BienesGeneralActivity extends ActionBarActivity {

    public static final String TAG = BienesGeneralActivity.class.getSimpleName();

    private Button mBtnLeidos;
    private Button mBtnFaltantes;
    private Button mBtnOtras;
    private TextView mTxtLeidos;
    private TextView mTxtFaltantes;
    private TextView mTxtOtras;
    String jsonData;
    String[] valores = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bienes_general);

        mBtnLeidos = (Button) findViewById(R.id.buttonLeidos);
        mBtnFaltantes = (Button) findViewById(R.id.buttonFaltantes);
        mBtnOtras = (Button) findViewById(R.id.buttonOtras);

        mTxtLeidos = (TextView) findViewById(R.id.contLeidos);
        mTxtFaltantes = (TextView) findViewById(R.id.contFaltantes);
        mTxtOtras = (TextView) findViewById(R.id.contOtras);


        Bundle extras = getIntent().getExtras();
        final String usuario;
        final String password;
        final String bienes;

        if (extras != null) {
            usuario = extras.getString("usuario");
            password = extras.getString("password");
            bienes = extras.getString("bienes");
        }
        else
            usuario = password = bienes = "";


        String patsUser = "user=" + usuario;
        String patsPass = "pass=" + password;
        final String patsLoginUrl = "http://pats.cl/api/?" + patsUser + "&" + patsPass + bienes;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(patsLoginUrl).build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        jsonData = response.body().string();
                        Log.v(TAG, patsLoginUrl);
                        if (bienes.equals("")){
                            Handler handler = new Handler(Looper.getMainLooper());

                            handler.postDelayed(new Runnable() {

                                public void run() {
                                    mTxtLeidos.setText("0");
                                    mTxtFaltantes.setText("0");
                                    mTxtOtras.setText("0");
                                }
                            }, 1);
                        }else {
                            valores = getData(jsonData);

                            Handler handler = new Handler(Looper.getMainLooper());

                            handler.postDelayed(new Runnable() {

                                public void run() {
                                    mTxtLeidos.setText(valores[1] + " de " + valores[0]);
                                    mTxtFaltantes.setText(valores[2] + " de " + valores[0]);
                                    mTxtOtras.setText(valores[3]);
                                }
                            }, 1);
                        }
                    }
                }
                catch (IOException e) {
                    Log.e(TAG, "Excepcion capturada: ", e);
                }
                catch (JSONException e){
                    Log.e(TAG, "Excepcion JSON capturada: ", e);
                }

            }
        });

        mBtnLeidos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextActivity("leidos", jsonData);
            }
        });

        mBtnFaltantes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextActivity("faltantes", jsonData);
            }
        });

        mBtnOtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextActivity("otras", jsonData);
            }
        });
    }

    public void nextActivity(String tipo, String jsonData){
        Intent intent;
        if(tipo.equals("leidos")) {
            intent = new Intent(this, ListaActivity.class);
        }else if (tipo.equals("faltantes")){
            intent = new Intent(this, FaltantesActivity.class);
        }else{
            intent = new Intent(this, OtrasActivity.class);
        }
        intent.putExtra("jsonData", jsonData);
        startActivity(intent);
    }

    private String[] getData(String jsonData) throws JSONException{
        JSONObject resultado = new JSONObject(jsonData);
        String total_sucursal = resultado.getString("total_sucursal");
        String total_leidos = resultado.getString("total_leidos");
        String total_faltantes = resultado.getString("total_faltantes");
        String total_otras = resultado.getString("total_otras");
        Log.i(TAG,"Desde JSON:" + total_sucursal +"-"+ total_leidos +"-"+ total_faltantes +"-"+ total_otras);

        String[] salida = new String[4];

        salida[0]=total_sucursal;
        salida[1]=total_leidos;
        salida[2]=total_faltantes;
        salida[3]=total_otras;

        return salida;
    }
}
