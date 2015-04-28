package cl.pats.envy15.pats_inventario;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class LoginActivity extends ActionBarActivity {

    public static final String TAG = LoginActivity.class.getSimpleName();

    private EditText mUser;
    private EditText mPass;
    private Button mLoginBtn;

    private String usuario;
    private String password;
    private boolean esValido = true;

    private Login mLogin;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUser = (EditText) findViewById(R.id.usernameText);
        mPass = (EditText) findViewById(R.id.passText);
        mLoginBtn = (Button) findViewById(R.id.buttonLogin);

        Bundle extras = getIntent().getExtras();
        final String bienes;

        if (extras != null) {
            bienes = extras.getString("bienes");
        }
        else
            bienes = "";

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usuario = mUser.getText().toString();
                usuario = usuario.replace(".","");
                usuario = usuario.replace("-","");
                password = mPass.getText().toString();

                if (usuario.equals("") || password.equals("")){
                    Toast.makeText(LoginActivity.this, "Debe ingresar Usuario y Contraseña",Toast.LENGTH_SHORT).show();
                }
                else {

                    String patsUser = "user=" + usuario;
                    String patsPass = "pass=" + md5(password);
                    final String patsLoginUrl = "http://pats.cl/api/?" + patsUser + "&" + patsPass;

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
                                    String jsonData = response.body().string();
                                    Log.v(TAG, jsonData);
                                    mLogin = getData(jsonData);
                                    if (getLogin(jsonData)){
                                        saveLogin(usuario, password);
                                        nextActivity(usuario, md5(password), bienes);
                                        finish();
                                    }
                                    else
                                    {
                                        final String error_login;
                                        if (esValido) {
                                            error_login = "Rut o contraseña incorrecta.";
                                        }else{
                                            error_login = "Error de conexión.";
                                        }
                                        Handler handler = new Handler(Looper.getMainLooper());

                                        handler.postDelayed(new Runnable() {

                                            public void run() {
                                                Toast.makeText(LoginActivity.this, error_login, Toast.LENGTH_LONG).show();
                                            }
                                        }, 1000);
                                    }

                                }
                            }
                            catch (IOException e) {
                                Log.e(TAG, "Excepcion capturada: ", e);
                            }
                            catch (JSONException e){
                                Log.e(TAG, "Excepcion capturada: ", e);
                            }
                        }
                    });


                }
            }
        });


    }

    private boolean getLogin(String jsonData) throws JSONException{
        JSONObject resultado = new JSONObject(jsonData);
        String status = resultado.getString("status");
        if (status.equals("error_conexion")){
            esValido = false;
        }
        return status.equals("ok");
    }

    private Login getData(String jsonData) throws JSONException{
        JSONObject resultado = new JSONObject(jsonData);
        String status = resultado.getString("status");
        Log.i(TAG,"Desde JSON:" + status);

        return new Login();
    }

    public void nextActivity(String usuario, String password, String bienes){
        Intent intent = new Intent(this, BienesGeneralActivity.class);
        intent.putExtra("usuario", usuario);
        intent.putExtra("password", password);
        intent.putExtra("bienes", bienes);
        startActivity(intent);
    }

    public static String md5(String s)
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes(),0,s.length());
            String hash = new BigInteger(1, digest.digest()).toString(16);
            return hash;
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("USUARIO", usuario);
        outState.putString("PASSWORD", password);

        super.onSaveInstanceState(outState);
    }

    private void saveLogin(String usuario, String password){
        SharedPreferences mispreferencias = getSharedPreferences("PreferenciasUsuario",Context.MODE_PRIVATE);
        SharedPreferences.Editor sPEditor = mispreferencias.edit();

        sPEditor.putString("USUARIO", usuario);
        sPEditor.putString("PASSWORD", password);

        sPEditor.commit();
    }
}
