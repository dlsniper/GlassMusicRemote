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

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * @author Florin Patan
 */
public class GlassMusicRemoteService extends Service {

    public static boolean isServiceRunning = false;

    private static int phoneNotificationId = 0;
    private static int glassNotificationId = 1;
    private static int notificationNumber = 0;

    private String notificationContentTitle;
    private String notificationGroupName;

    private static final String KEY_PREV = "ro.florinpatan.glassmusicremote.app.prevSong";
    private static final String KEY_NEXT = "ro.florinpatan.glassmusicremote.app.nextSong";

    private static Context myContext;

    private static Intent prevIntent = new Intent(KEY_PREV);
    private static Intent nextIntent = new Intent(KEY_NEXT);

    private static Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    private static GoogleApiClient googleApiClient;

    private static NotificationManagerCompat notificationManager;
    private static NotificationCompat.WearableExtender wearableExtender;

    private BroadcastReceiver songChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {

            final com.google.android.gms.common.api.PendingResult<NodeApi.GetConnectedNodesResult> connectedNodes =
                    Wearable.NodeApi.getConnectedNodes(googleApiClient);

            connectedNodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult connectedNodesResult) {
                    List<Node> nodes = connectedNodesResult.getNodes();
                    if (nodes.size() == 0) {
                        return;
                    }

                    String artist = intent.getStringExtra("artist");
                    String album = intent.getStringExtra("album");
                    String track = intent.getStringExtra("track");
                    String notificationContent = String.format("%s - %s (album %s)", artist, track, album);

                    NotificationCompat.Builder phoneNotificationBuilder = new NotificationCompat.Builder(myContext)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle(notificationContentTitle)
                            .setContentText(notificationContent)
                            .setNumber(++notificationNumber)
                            .setSound(soundUri)
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .setGroup(notificationGroupName)
                            .setGroupSummary(true);

                    NotificationCompat.Builder wearableNotificationBuilder = new NotificationCompat.Builder(myContext)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setAutoCancel(true)
                            .setContentTitle(notificationContentTitle)
                            .setContentText(notificationContent)
                            .setNumber(++notificationNumber)
                            .setSound(soundUri)
                            .extend(wearableExtender)
                            .setOngoing(false)
                            .setGroup(notificationGroupName)
                            .setGroupSummary(false);

                    notificationManager.notify(phoneNotificationId, phoneNotificationBuilder.build());
                    notificationManager.notify(glassNotificationId, wearableNotificationBuilder.build());
                }
            });
        }
    };

    private BroadcastReceiver songStateChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {

            final com.google.android.gms.common.api.PendingResult<NodeApi.GetConnectedNodesResult> connectedNodes =
                    Wearable.NodeApi.getConnectedNodes(googleApiClient);

            connectedNodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult connectedNodesResult) {
                    List<Node> nodes = connectedNodesResult.getNodes();
                    if (nodes.size() == 0) {
                        return;
                    }

                    if (!intent.getBooleanExtra("playstate", false) &&
                            !intent.getBooleanExtra("playing", false)
                            ) {
                        clearNotification();
                    }
                }
            });
        }
    };

    BroadcastReceiver songKeysReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // We want this because we can easily support other services in the future as well

            String command = "";
            String sendIntent = "com.android.music.musicservicecommand";

            switch (intent.getAction()) {
                case KEY_PREV: {
                    command = "previous";
                } break;

                case KEY_NEXT: {
                    command = "next";
                } break;

            }

            if (command.isEmpty()) {
                return;
            }

            Intent i = new Intent(sendIntent);
            i.putExtra("command", command);
            sendBroadcast(i);
        }
    };

    private void setupMusicReceiver() {
        IntentFilter songChangedFilter = new IntentFilter();
        songChangedFilter.addAction("com.android.music.metachanged");
        songChangedFilter.addAction("com.htc.music.metachanged");
        songChangedFilter.addAction("fm.last.android.metachanged");
        songChangedFilter.addAction("com.sec.android.app.music.metachanged");
        songChangedFilter.addAction("com.nullsoft.winamp.metachanged");
        songChangedFilter.addAction("com.amazon.mp3.metachanged");
        songChangedFilter.addAction("com.miui.player.metachanged");
        songChangedFilter.addAction("com.real.IMP.metachanged");
        songChangedFilter.addAction("com.sonyericsson.music.metachanged");
        songChangedFilter.addAction("com.rdio.android.metachanged");
        songChangedFilter.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        songChangedFilter.addAction("com.andrew.apollo.metachanged");

        registerReceiver(songChangedReceiver, songChangedFilter);

        IntentFilter songStateChangedFilter = new IntentFilter();
        songStateChangedFilter.addAction("com.android.music.playstatechanged");
        registerReceiver(songStateChangedReceiver, songStateChangedFilter);

        IntentFilter songKeysFilter = new IntentFilter();
        songKeysFilter.addAction(KEY_PREV);
        songKeysFilter.addAction(KEY_NEXT);

        registerReceiver(songKeysReceiver, songKeysFilter);
    }

    private void changePlayState(String playState) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View mainActivity = inflater.inflate(R.layout.activity_main, null);
        TextView appState = (TextView) mainActivity.findViewById(R.id.appState);
        appState.setText(getString(R.string.app_state, playState));
    }

    private void clearNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myContext);
        notificationManager.cancel(phoneNotificationId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isServiceRunning = true;

        changePlayState(getString(R.string.app_state_running));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean stopService(Intent name) {
        isServiceRunning = false;

        changePlayState(getString(R.string.app_state_stopped));
        clearNotification();

        return super.stopService(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationContentTitle = getString(R.string.currently_playing);
        notificationGroupName = getString(R.string.app_name);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        googleApiClient.connect();

        myContext = this;

        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(myContext, 0, prevIntent, 0);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(myContext, 0, nextIntent, 0);

        NotificationCompat.Action prevNot = new NotificationCompat.Action.Builder(R.drawable.ic_music_previous_50,
                getString(R.string.previous_action), prevPendingIntent)
                .build();

        NotificationCompat.Action nextNot = new NotificationCompat.Action.Builder(R.drawable.ic_music_next_50,
                getString(R.string.next_action), nextPendingIntent)
                .build();

        notificationManager = NotificationManagerCompat.from(myContext);

        wearableExtender = new NotificationCompat.WearableExtender()
                        .setHintHideIcon(true)
                        .addAction(nextNot)
                        .addAction(prevNot)
        ;

        setupMusicReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;

        changePlayState(getString(R.string.app_state_stopped));
        clearNotification();

        unregisterReceiver(songChangedReceiver);
        unregisterReceiver(songKeysReceiver);
        unregisterReceiver(songStateChangedReceiver);

        googleApiClient.disconnect();

        myContext = null;

        super.onDestroy();
    }
}
