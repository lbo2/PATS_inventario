package cl.pats.envy15.pats_inventario;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class ReadActivity extends ActionBarActivity {

    SQLiteDatabase sellosDB = null;

    private Button mSync;

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    private TextView mTextView;
    private NfcAdapter mNfcAdapter;
    private ArrayList<String> codigos = new ArrayList<String>();
    private ArrayList<String> fechas = new ArrayList<String>();
    private ArrayList<String> sellos = new ArrayList<String>();
    private String usuario;
    private String password;
    private TextView tituloPats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        crearDB();
        getSellos();
        //deleteDatabase();

        if(savedInstanceState != null){
            usuario  = savedInstanceState.getString("USUARIO");
            password  = savedInstanceState.getString("PASSWORD");
        }

        CargarPreferencias();

        mSync = (Button) findViewById(R.id.btnSincronizar);

        tituloPats = (TextView) findViewById(R.id.tituloPats);

        mSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextActivity(cadenaBienes(codigos));
            }
        });

        mTextView = (TextView) findViewById(R.id.textView_explanation);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No hay Internet", Toast.LENGTH_SHORT).show();
            mSync.setClickable(false);
        }
        else
            mSync.setClickable(true);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, getString(R.string.nfc_no_existe), Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText(getString(R.string.nfc_desabilitado));
        } else {
            mTextView.setText(R.string.explanation);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();

        CargarPreferencias();
        if(usuario.equals("")){
            deleteDatabase();
            //Toast.makeText(ReadActivity.this, "onResume", Toast.LENGTH_SHORT).show();
        }

		/*
		 * It's important, that the activity is in the foreground (resumed). Otherwise
		 * an IllegalStateException is thrown.
		 */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
		/*
		 * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
		 */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
		/*
		 * This method gets called, when a new Intent gets associated with the current activity instance.
		 * Instead of creating a new activity, onNewIntent will be called. For more information have a look
		 * at the documentation.
		 *
		 * In our case this method gets called, when the user attaches a Tag to the device.
		 */
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, getString(R.string.encoding_no_soportado), e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
			/*
			 * See NFC forum specification for "Text Record Type Definition" at 3.2.1
			 *
			 * http://www.nfc-forum.org/specs/
			 *
			 * bit_7 defines encoding
			 * bit_6 reserved for future use, must be 0
			 * bit_5..0 length of IANA language code
			 */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !codigos.contains(result)) {

                Calendar cal = Calendar.getInstance();
                int segundos = cal.get(Calendar.SECOND);
                int minutos = cal.get(Calendar.MINUTE);
                int hora = cal.get(Calendar.HOUR_OF_DAY);
                int anio = cal.get(Calendar.YEAR);
                int mes = cal.get(Calendar.MONTH) + 1;
                int dia = cal.get(Calendar.DAY_OF_MONTH);

                String fecha = agregarCero(dia) + "/" + agregarCero(mes) + "/" + anio;
                String time = agregarCero(hora) + ":" + agregarCero(minutos) + ":" + agregarCero(segundos);

                agregarSelloaLista(result, fecha, time);
                addSello(result,fecha, time);

            }
        }
    }

    private void agregarSelloaLista(String result, String fecha, String time) {

        codigos.add(result);
        fechas.add(fecha + "  " + time);

        sellos.clear();

        for (int i = 1; i <= codigos.size(); i++) {
            //mTextView.append(i + ".- " + fechas.get(i-1) + " - " + codigos.get(i-1) + "\n");
            sellos.add(i + ".- " + fechas.get(i-1) + " - " + codigos.get(i-1));
        }

        ListAdapter theAdapter = new ArrayAdapter<String>(ReadActivity.this,android.R.layout.simple_list_item_1,sellos);

        ListView theLsitView = (ListView) findViewById(R.id.theListView);

        theLsitView.setAdapter(theAdapter);

        theLsitView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String bienSeleccionado = "Seleccionaste :" + String.valueOf(parent.getItemAtPosition(position));
                Toast.makeText(ReadActivity.this, bienSeleccionado,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetAdapter(){

        sellos.clear();

        ListAdapter theAdapter = new ArrayAdapter<String>(ReadActivity.this,android.R.layout.simple_list_item_1,sellos);

        ListView theLsitView = (ListView) findViewById(R.id.theListView);

        theLsitView.setAdapter(theAdapter);
    }

    public void nextActivity(String bienes){
        CargarPreferencias();
        if(!usuario.equals("")){
            Intent intent = new Intent(this, BienesGeneralActivity.class);
            intent.putExtra("bienes", bienes);
            intent.putExtra("usuario", usuario);
            intent.putExtra("password", password);
            startActivity(intent);
        }
        else{
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("bienes", bienes);
            startActivity(intent);
        }
    }

    public String agregarCero(int numero){
        if (numero < 10)
            return "0" + numero;
        else
            return numero + "";
    }

    public String cadenaBienes(ArrayList<String> codigos){
        String salida="";
        for (int i=0; i<codigos.size(); i++ ){
            salida = salida + "&b[]=" + codigos.get(i);
        }
        return salida;
    }

    public void CargarPreferencias(){
        SharedPreferences mispreferencias = getSharedPreferences("PreferenciasUsuario",Context.MODE_PRIVATE);
        String sPUsuario = mispreferencias.getString("USUARIO","");
        String sPPassword = mispreferencias.getString("PASSWORD", "");

        Log.d(TAG, "Desde CargarPreferencias-> Usuario: " + sPUsuario);

        usuario = sPUsuario;
        password = sPPassword;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        CargarPreferencias();
        if(!usuario.equals("")) {
            getMenuInflater().inflate(R.menu.menu_read, menu);
            return true;
        }else
            return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(this,"Has cerrado Sesión", Toast.LENGTH_SHORT).show();
            logOut();
            nextActivityLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logOut(){
        SharedPreferences mispreferencias = getSharedPreferences("PreferenciasUsuario", Context.MODE_PRIVATE);
        SharedPreferences.Editor sPEditor = mispreferencias.edit();

        sPEditor.putString("USUARIO", "");
        sPEditor.putString("PASSWORD", "");

        sPEditor.commit();

        deleteDatabase();

    }

    public void nextActivityLogout(){
        Intent intent = new Intent(this, ReadActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void crearDB(){
        try{

            // Opens a current database or creates it
            // Pass the database name, designate that only this app can use it
            // and a DatabaseErrorHandler in the case of database corruption
            sellosDB = this.openOrCreateDatabase("Sellos", MODE_PRIVATE, null);

            // Execute an SQL statement that isn't select
            sellosDB.execSQL("CREATE TABLE IF NOT EXISTS sellos " +
                    "(id integer primary key, sello VARCHAR, fecha VARCHAR, hora VARCHAR);");

            // The database on the file system
            File database = getApplicationContext().getDatabasePath("Sellos");

            // Check if the database exists
            if (database.exists()) {
                //Toast.makeText(this, "Database Created", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this, "Database Missing", Toast.LENGTH_SHORT).show();
            }

        }
        catch (Exception e){
            Log.e(TAG,"Error al crear la base de datos",e);
        }
    }

    public void addSello(String sello, String fecha, String hora) {

        // Get the contact name and email entered
        //String contactName = nameEditText.getText().toString();
        //String contactEmail = emailEditText.getText().toString();

        // Execute SQL statement to insert new data
        sellosDB.execSQL("INSERT INTO sellos (sello, fecha, hora) VALUES ('" +
                sello + "', '" + fecha + "', '" + hora + "');");

    }

    public void getSellos() {

        // A Cursor provides read and write access to database results
        Cursor cursor = sellosDB.rawQuery("SELECT * FROM sellos", null);

        // Get the index for the column name provided
        int idColumn = cursor.getColumnIndex("id");
        int selloColumn = cursor.getColumnIndex("sello");
        int fechaColumn = cursor.getColumnIndex("fecha");
        int horaColumn = cursor.getColumnIndex("hora");

        // Move to the first row of results
        cursor.moveToFirst();

        String contactList = "";

        // Verify that we have results
        if(cursor != null && (cursor.getCount() > 0)){

            do{
                // Get the results and store them in a String
                String id = cursor.getString(idColumn);
                String sello = cursor.getString(selloColumn);
                String fecha = cursor.getString(fechaColumn);
                String hora = cursor.getString(horaColumn);

                contactList = contactList + id + " : " + sello + " : " + fecha  + " : " + hora + "\n";
                agregarSelloaLista(sello, fecha, hora);

                // Keep getting results as long as they exist
            }while(cursor.moveToNext());

            //tituloPats.setText(contactList);
            //Toast.makeText(this, contactList, Toast.LENGTH_SHORT).show();

        } else {

            //Toast.makeText(this, "No existen datos guardados", Toast.LENGTH_SHORT).show();
            //tituloPats.setText("");

        }

    }

    public void mostrarSellos() {

        // A Cursor provides read and write access to database results
        Cursor cursor = sellosDB.rawQuery("SELECT * FROM sellos", null);

        // Get the index for the column name provided
        int idColumn = cursor.getColumnIndex("id");
        int selloColumn = cursor.getColumnIndex("sello");
        int fechaColumn = cursor.getColumnIndex("fecha");
        int horaColumn = cursor.getColumnIndex("hora");

        // Move to the first row of results
        cursor.moveToFirst();

        String contactList = "";

        // Verify that we have results
        if(cursor != null && (cursor.getCount() > 0)){

            do{
                // Get the results and store them in a String
                String id = cursor.getString(idColumn);
                String sello = cursor.getString(selloColumn);
                String fecha = cursor.getString(fechaColumn);
                String hora = cursor.getString(horaColumn);

                contactList = contactList + id + " : " + sello + " : " + fecha  + " : " + hora + "\n";

                // Keep getting results as long as they exist
            }while(cursor.moveToNext());

            //tituloPats.setText(contactList);
            Toast.makeText(this, contactList, Toast.LENGTH_SHORT).show();

        } else {

            Toast.makeText(this, "No existen datos guardados", Toast.LENGTH_SHORT).show();
            //tituloPats.setText("");

        }

    }

    public void deleteDatabase() {

        // Delete database
        this.deleteDatabase("Sellos");

    }


}
