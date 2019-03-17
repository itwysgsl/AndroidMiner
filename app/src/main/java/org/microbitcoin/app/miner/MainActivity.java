package org.microbitcoin.app.miner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import org.microbitcoin.app.bitzenymininglibrary.BitZenyMiningLibrary;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MBC Miner";
    private static final int LOG_LINES = 1000;

    private BitZenyMiningLibrary miner;

    private Button buttonDrive;
    private TextView textViewLog;

    private String prefAlgorithm;
    private Integer prefNThreads;
    private String prefUserWalletAddress;
    private String prefMiningPool;
    private String selectedPool;

    private boolean running;
    private BlockingQueue<String> logs = new LinkedBlockingQueue<>(LOG_LINES);

    private static class JNICallbackHandler extends Handler {
        private final WeakReference<MainActivity> activity;

        public JNICallbackHandler(MainActivity activity) {
            this.activity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = this.activity.get();
            if (activity != null) {
                String log = msg.getData().getString("log");
                String logs = Utils.rotateStringQueue(activity.logs, log);
                activity.textViewLog.setText(logs);
                Log.d(TAG, log);
            }
        }
    }

    private static JNICallbackHandler sHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if user wallet is defined
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        prefUserWalletAddress = pref.getString("user_wallet_address", null);

        if (prefUserWalletAddress.length() < 10) {
            Log.e(TAG, "no address found.");
            showWalletAlert();
        }

//        showDeviceInfo();

        sHandler = new JNICallbackHandler(this);
        miner = new BitZenyMiningLibrary(sHandler);

        buttonDrive = (Button) findViewById(R.id.buttonDrive);
        buttonDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getSettings();

                if (running) {
                    Log.d(TAG, "Stop");
                    miner.stopMining();
                } else {
                    Log.d(TAG, "Start");
                    int n_threads = 0;
                    try {
                        n_threads = Integer.parseInt(prefNThreads.toString());
                    } catch (NumberFormatException e){}

                    BitZenyMiningLibrary.Algorithm algorithm =
                        prefAlgorithm == "rainforest" ? BitZenyMiningLibrary.Algorithm.YESPOWER : BitZenyMiningLibrary.Algorithm.YESCRYPT;

                    switch (prefMiningPool) {
                        case "technicals":
                            selectedPool = "stratum+tcp://mine.thetechnicalspool.com:5333";
                            break;
                        case "miningpatriot":
                            selectedPool = "stratum+tcp://mine.miningpatriot.com:7443";
                            break;
                        case "moricpool":
                            selectedPool = "stratum+tcp://mbc.moricpool.com:4007";
                            break;
                        case "skypool":
                            selectedPool = "stratum+tcp://mbc-asia.skypool.co:8001";
                            break;
                        case "hashpooleu":
                            selectedPool = "stratum+tcp://pool.hashpool.eu:2508";
                            break;
                        default:
                            selectedPool = "stratum+tcp://multi.extremehash.io:7443";
                            break;
                    }

                    /**
                     * @params {string} pool
                     * @params {string} address
                     * @params {string} password
                     * @params {integer} n_threads
                     * @params {string} algorithm
                     */
                    miner.startMining(
                        selectedPool,
                        prefUserWalletAddress,
                        "x",
                        n_threads,
                        algorithm
                    );
                }

                changeState(!running);
            }
        });

        textViewLog = (TextView) findViewById(R.id.textViewLog);
        textViewLog.setMovementMethod(new ScrollingMovementMethod());

        changeState(miner.isMiningRunning());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_advanced_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
                break;
            }
            // case blocks for other MenuItems (if any)
        }
        return true;
    }

    private void changeState(boolean running) {
        buttonDrive.setText(running ? "Stop" : "Start");
        this.running = running;
    }

    private void getSettings() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        prefUserWalletAddress = pref.getString("user_wallet_address", null);
        prefMiningPool = pref.getString("pool_list", null);
        prefAlgorithm = pref.getString("pow_algorithm", null);
        prefNThreads = 0;
    }

    private void showWalletAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Wallet Address");

        builder.setMessage("You need a wallet address to start mining.\nGo to Settings to manually add your own, or Create Wallet online.");

        builder.setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        builder.setNegativeButton("Create Wallet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wallet.microbitcoin.org/"));
                startActivity(intent);
            }
        });

        builder.show();
    }

    private void showDeviceInfo() {
        String[] keys = new String[]{ "os.arch", "os.name", "os.version" };
        for (String key : keys) {
            Log.d(TAG, key + ": " + System.getProperty(key));
        }
        Log.d(TAG, "CODE NAME: " + Build.VERSION.CODENAME);
        Log.d(TAG, "SDK INT: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "MANUFACTURER: " + Build.MANUFACTURER);
        Log.d(TAG, "MODEL: " + Build.MODEL);
        Log.d(TAG, "PRODUCT: " + Build.PRODUCT);
    }
}
