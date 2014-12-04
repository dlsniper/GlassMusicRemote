/*
 * Copyright 2014 Florin Patan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ro.florinpatan.glassmusicremote.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class GlassMusicRemoteMain extends Activity {

    public static final String PREF_FILENAME = "GlassMusicRemote";
    private boolean runOnStartup = true;
    private boolean enabled = true;

    public void onEnabledClicked(View view) {
        Switch runOnStartupSwitch = (Switch) findViewById(R.id.runOnStartup);

        boolean checked = ((Switch) view).isChecked();

        runOnStartupSwitch.setEnabled(checked);
        enabled = checked;

        if (!checked) {
            runOnStartupSwitch.setChecked(false);
            runOnStartup = false;

            stopService(new Intent(this, GlassMusicRemoteService.class));
            changePlayState(getString(R.string.app_state_stopped));
        } else {
            startService(new Intent(this, GlassMusicRemoteService.class));
            changePlayState(getString(R.string.app_state_running));
        }
    }

    public void onRunOnStartupClicked(View view) {
        runOnStartup = ((Switch) view).isChecked();
    }

    private void changePlayState(String playState) {
        TextView appState = (TextView) findViewById(R.id.appState);
        appState.setText(getString(R.string.app_state, playState));
    }

    private void saveSettings() {
        EditText notificationContent = (EditText) findViewById(R.id.notificationContent);

        String notificationLayout = notificationContent.getText().toString();
        if (notificationLayout.isEmpty()) {
            notificationLayout = "%listpos%. %artist% - %title% (album %album%) %length%";
        }

        GlassMusicRemoteService.notificationLayout = notificationLayout;

        SharedPreferences.Editor editor = getSharedPreferences(PREF_FILENAME, 0).edit();
        editor.putBoolean("enabled", enabled);
        editor.putBoolean("runOnStartup", runOnStartup);

        editor.putString("notificationContent", notificationLayout);
        editor.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = getSharedPreferences(PREF_FILENAME, 0);
        enabled = settings.getBoolean("enabled", true);
        runOnStartup = settings.getBoolean("runOnStartup", true);
        GlassMusicRemoteService.notificationLayout = settings.getString("notificationContent", GlassMusicRemoteService.notificationLayout);

        if (enabled && !GlassMusicRemoteService.isServiceRunning) {

            startService(new Intent(this, GlassMusicRemoteService.class));
        }

        setContentView(R.layout.activity_main);

        final Intent demoTrack = new Intent();
        demoTrack.putExtra("artist", "D.J. BoBo");
        demoTrack.putExtra("track", "Everybody");
        demoTrack.putExtra("album", "Dance with me");
        demoTrack.putExtra("listpos", 1);
        demoTrack.putExtra("trackLength", Long.valueOf(236000));

        final EditText notificationContent = (EditText) findViewById(R.id.notificationContent);
        final TextView notificationSample = (TextView) findViewById(R.id.notificationSample);

        notificationContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String content = GlassMusicRemoteService.computeNotificationContent(s.toString(), demoTrack);
                notificationSample.setText("Sample notification:\n" + content);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Do something or nothing.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Do something or nothing
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Switch enabledSwitch = (Switch) findViewById(R.id.enabled);
        Switch runOnStartupSwitch = (Switch) findViewById(R.id.runOnStartup);
        EditText notificationContent = (EditText) findViewById(R.id.notificationContent);

        enabledSwitch.setChecked(enabled);
        runOnStartupSwitch.setChecked(runOnStartup);
        if (!enabled) {
            runOnStartupSwitch.setEnabled(enabled);
        }

        changePlayState("---");
        notificationContent.setText(GlassMusicRemoteService.notificationLayout);

        final Handler handler = new Handler();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String playState = GlassMusicRemoteService.isServiceRunning ?
                                getString(R.string.app_state_running) :
                                getString(R.string.app_state_stopped);
                        changePlayState(playState);
                    }
                });
            }}, 1200);
    }

    @Override
    public void onBackPressed() {
        saveSettings();

        super.onBackPressed();
    }

    @Override
    protected void onStop(){
        saveSettings();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        saveSettings();

        super.onDestroy();
    }
}
