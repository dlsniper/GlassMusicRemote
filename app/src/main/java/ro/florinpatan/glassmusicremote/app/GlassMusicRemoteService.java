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

    private static int notificationId = 0;
    private static int notificationNumber = 0;

    private static final String KEY_PREV = "ro.florinpatan.glassmusicremote.app.prevSong";
    private static final String KEY_NEXT = "ro.florinpatan.glassmusicremote.app.nextSong";

    private static Context myContext;

    private static Intent prevIntent = new Intent(KEY_PREV);
    private static Intent nextIntent = new Intent(KEY_NEXT);

    private static NotificationCompat.Action nextNot;
    private static NotificationCompat.Action prevNot;

    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    private static GoogleApiClient googleApiClient;

    private BroadcastReceiver songChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {

            final com.google.android.gms.common.api.PendingResult<NodeApi.GetConnectedNodesResult> connectedNodes = Wearable.NodeApi.getConnectedNodes(googleApiClient);
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
                    String information = String.format("%s - %s (album %s)", artist, track, album);

                    NotificationCompat.WearableExtender wearableExtender =
                            new NotificationCompat.WearableExtender()
                                    .setHintHideIcon(true)
                                    .addAction(nextNot)
                                    .addAction(prevNot)
                            ;

                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(myContext)
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setAutoCancel(true)
                                    .setContentTitle(getString(R.string.currently_playing))
                                    .setContentText(information)
                                    .setNumber(++notificationNumber)
                                    .setSound(soundUri)
                                    .extend(wearableExtender)
                            ;

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(myContext);
                    notificationManager.notify(notificationId, notificationBuilder.build());
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

        IntentFilter songKeysFilter = new IntentFilter();
        songKeysFilter.addAction(KEY_PREV);
        songKeysFilter.addAction(KEY_NEXT);

        registerReceiver(songKeysReceiver, songKeysFilter);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        googleApiClient.connect();

        myContext = this;

        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(myContext, 0, prevIntent, 0);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(myContext, 0, nextIntent, 0);

        prevNot = new NotificationCompat.Action.Builder(R.drawable.ic_music_previous_50,
                getString(R.string.previous_action), prevPendingIntent)
                .build()
        ;

        nextNot = new NotificationCompat.Action.Builder(R.drawable.ic_music_next_50,
                getString(R.string.next_action), nextPendingIntent)
                .build()
        ;

        setupMusicReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(songChangedReceiver);
        unregisterReceiver(songKeysReceiver);

        googleApiClient.disconnect();

        myContext = null;

        super.onDestroy();
    }
}
