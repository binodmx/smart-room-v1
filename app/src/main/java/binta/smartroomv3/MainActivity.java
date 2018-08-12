package binta.smartroomv3;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private BluetoothConnector btConnector = new BluetoothConnector();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String DEFAULT_DEVICE_ADDRESS = "00:21:13:00:17:49";
    private ProgressDialog progress;
    private boolean isConnected = false;
    private String deviceAddress = null;
    private ImageView imageView_on;
    private ImageView imageView_off;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_connect) {
            showPairedDevicesList();
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("DEV_ADDR", Context.MODE_PRIVATE);
        editor = preferences.edit();

        // setup bluetooth connection
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            // show a message that the device has no bluetooth adapter
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            // stop the application
            finish();
        } else {
            // ask from user to turn on bluetooth if it is off
            if (!btAdapter.isEnabled()) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            }
        }

        imageView_on = (ImageView) findViewById(R.id.imageView_on);
        imageView_off = (ImageView) findViewById(R.id.imageView_off);

        // On button action
        Button button_on = (Button) findViewById(R.id.button_on);
        button_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnBulb();
            }
        });

        // Off button action
        Button button_off = (Button) findViewById(R.id.button_off);
        button_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffBulb();
            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!isConnected) {
            if (preferences.contains("deviceAddress")) {
                deviceAddress = preferences.getString("deviceAddress", DEFAULT_DEVICE_ADDRESS);
                if (btAdapter.isEnabled()) {
                    btConnector.execute();
                }
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (isConnected){
                btSocket.close();
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {

        }
    }

    private class BluetoothConnector extends AsyncTask<Void, Void, Void> {

        private boolean connectSuccess = true;
        @Override
        protected void onPreExecute() {
            // show a progress dialog
            progress = ProgressDialog.show(MainActivity.this, "Connecting", "Please wait...");
        }
        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isConnected) {
                    // get the mobile bluetooth device
                    btAdapter = BluetoothAdapter.getDefaultAdapter();

                    // connect to device's address and checks if it's available
                    BluetoothDevice btClient = btAdapter.getRemoteDevice(deviceAddress);

                    // create a RFCOMM (SPP) connection
                    btSocket = btClient.createInsecureRfcommSocketToServiceRecord(myUUID);

                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

                    // start connection
                    btSocket.connect();
                }
            }
            catch (IOException e) {
                // if the try failed, you can check the exception here
                connectSuccess = false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!connectSuccess) {
                Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                isConnected = true;
            }
            progress.dismiss();
        }

    }

    private void showPairedDevicesList() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        String[] deviceNameList = new String[pairedDevices.size()];
        final String[] deviceAddressList = new String[pairedDevices.size()];
        if (pairedDevices.size() > 0) {
            int i = 0;
            for(BluetoothDevice btDevice : pairedDevices) {
                // get device's name and MAC address
                deviceNameList[i] = btDevice.getName();
                deviceAddressList[i] = btDevice.getAddress();
                i++;
            }
        } else {
            Toast.makeText(getApplicationContext(), "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show();
        }


        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paired devices");

        // add a list
        builder.setItems(deviceNameList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deviceAddress = deviceAddressList[which];
                editor.putString("deviceAddress", deviceAddress);
                editor.commit();
                btConnector.execute();
            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void turnOnBulb(){
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write("h".toString().getBytes());
                Toast.makeText(getApplicationContext(), "Lights ON", Toast.LENGTH_SHORT).show();
                imageView_on.setVisibility(View.VISIBLE);
                imageView_off.setVisibility(View.INVISIBLE);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Connection error", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Check connection", Toast.LENGTH_SHORT).show();
        }
    }

    private void turnOffBulb(){
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write("l".toString().getBytes());
                Toast.makeText(getApplicationContext(), "Lights OFF", Toast.LENGTH_SHORT).show();
                imageView_on.setVisibility(View.INVISIBLE);
                imageView_off.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Connection error", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Check connection", Toast.LENGTH_SHORT).show();
        }
    }





































}
