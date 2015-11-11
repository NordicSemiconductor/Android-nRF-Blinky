package no.nordicsemi.android.blinky;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import no.nordicsemi.android.blinky.profile.BleProfileService;
import no.nordicsemi.android.blinky.service.BlinkyService;

public class ControlBlinkActivity extends AppCompatActivity {

    private BlinkyService.BlinkyBinder mBlinkyDevice;
    private String mDeviceAddress;
    private String mDeviceName;
    private Intent mIntentBlinky;
    private Button btnBlinky, btnConnect;
    private ImageView imageBulb;
    private RelativeLayout relativeLayoutControl;
    private View backgroundView;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBlinkyDevice = (BlinkyService.BlinkyBinder) service;

            if(mBlinkyDevice.isConnected()){
                btnConnect.setText(getString(R.string.action_disconnect));

                if(mBlinkyDevice.isOn()) {
                    imageBulb.setImageDrawable(ContextCompat.getDrawable(ControlBlinkActivity.this, R.drawable.bulb_on));
                    btnBlinky.setText(getString(R.string.turn_off));
                } else {
                    imageBulb.setImageDrawable(ContextCompat.getDrawable(ControlBlinkActivity.this, R.drawable.bulb_off));
                    btnBlinky.setText(getString(R.string.turn_on));
                }


                if(mBlinkyDevice.isButtonPressed()){
                    backgroundView.setVisibility(View.VISIBLE);
                } else backgroundView.setVisibility(View.INVISIBLE);
            } else
                btnConnect.setText(getString(R.string.action_connect));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBlinkyDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_device);
        Intent i = getIntent();
        mDeviceName = i.getStringExtra(BlinkyService.EXTRA_DEVICE_NAME);
        mDeviceAddress = i.getStringExtra(BlinkyService.EXTRA_DEVICE_ADDRESS);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        btnBlinky = (Button) findViewById(R.id.button_blinky);
        btnConnect = (Button) findViewById(R.id.action_connect);
        imageBulb = (ImageView) findViewById(R.id.img_bulb);
        backgroundView = findViewById(R.id.background_view);
        relativeLayoutControl = (RelativeLayout) findViewById(R.id.relative_layout_control);

        btnBlinky.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlinkyDevice.isConnected()) {
                    if (btnBlinky.getText().equals(getString(R.string.turn_on))) {
                        mBlinkyDevice.send(true);
                    } else {
                        mBlinkyDevice.send(false);
                    }
                } else {
                    showError(getString(R.string.please_connect));
                }
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mBlinkyUpdateReceiver, makeGattUpdateIntentFilter());
        mIntentBlinky = new Intent(this, BlinkyService.class);
        mIntentBlinky.putExtra(BlinkyService.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        startService(mIntentBlinky);
        boolean flag = bindService(mIntentBlinky, mServiceConnection, 0);


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBlinkyDevice.isConnected() && btnConnect.getText().equals(getString(R.string.action_disconnect))) {
                    mBlinkyDevice.disconnect();
                }
                else {
                    mIntentBlinky.putExtra(BlinkyService.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                    startService(mIntentBlinky);
                    bindService(mIntentBlinky, mServiceConnection, 0);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(BlinkyService.EXTRA_DEVICE_NAME, mDeviceName);
        outState.putString(BlinkyService.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDeviceName = savedInstanceState.getString(BlinkyService.EXTRA_DEVICE_NAME);
        mDeviceAddress = savedInstanceState.getString(BlinkyService.EXTRA_DEVICE_ADDRESS);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mBlinkyDevice.isConnected())
            mBlinkyDevice.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBlinkyUpdateReceiver);

        mServiceConnection = null;
        mBlinkyDevice = null;
    }

    private BroadcastReceiver mBlinkyUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BlinkyService.BROADCAST_LED_STATE_CHANGED.equals(action)) {
                final boolean flag = intent.getExtras().getBoolean(BlinkyService.EXTRA_DATA);
                if (flag){
                    imageBulb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bulb_on));
                    btnBlinky.setText(getString(R.string.turn_off));
                }
                else{
                    imageBulb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bulb_off));
                    btnBlinky.setText(getString(R.string.turn_on));
                }
            } else if(BlinkyService.BROADCAST_BUTTON_STATE_CHANGED.equals(action)){
                final boolean flag = intent.getExtras().getBoolean(BlinkyService.EXTRA_DATA);
                if(flag){
                    backgroundView.setVisibility(View.VISIBLE);
                }
                else {
                    backgroundView.setVisibility(View.INVISIBLE);
                }
            } else if(BlinkyService.BROADCAST_CONNECTION_STATE.equals(action)){
                final int value = intent.getExtras().getInt(BlinkyService.EXTRA_CONNECTION_STATE);
                switch (value){
                    case BleProfileService.STATE_CONNECTED:
                        btnConnect.setText(getString(R.string.action_disconnect));
                        break;
                    case BleProfileService.STATE_DISCONNECTED:
                        btnConnect.setText(getString(R.string.action_connect));
                        btnBlinky.setText(getString(R.string.turn_on));
                        imageBulb.setImageDrawable(ContextCompat.getDrawable(ControlBlinkActivity.this, R.drawable.bulb_off));
                        break;
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BlinkyService.BROADCAST_LED_STATE_CHANGED);
        intentFilter.addAction(BlinkyService.BROADCAST_LED_STATE_CHANGED);
        intentFilter.addAction(BlinkyService.BROADCAST_CONNECTION_STATE);
        return intentFilter;
    }

    private void showError(final String error) {
        Snackbar snackbar = Snackbar.make(relativeLayoutControl, error, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }
}
