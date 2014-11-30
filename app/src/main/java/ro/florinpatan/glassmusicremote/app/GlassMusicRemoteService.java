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

    private static boolean isPlaying = true;
    private static int notificationId = 0;
    private static int notificationNumber = 0;

    public static final String KEY_TOGGLEPAUSE = "ro.florinpatan.glassmusicremote.app.togglepause";
    public static final String KEY_PREV        = "ro.florinpatan.glassmusicremote.app.prevSong";
    public static final String KEY_NEXT        = "ro.florinpatan.glassmusicremote.app.nextSong";

    private static Context myContext;

    private static Intent prevIntent        = new Intent(KEY_PREV);
    //private static Intent togglePauseIntent = new Intent(KEY_TOGGLEPAUSE);
    private static Intent nextIntent        = new Intent(KEY_NEXT);

    private static NotificationCompat.Action nextNot;
    //private static NotificationCompat.Action togglePauseNot;
    private static NotificationCompat.Action prevNot;

    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    private PendingIntent togglePausePendingIntent, prevPendingIntent, nextPendingIntent;

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
                                    //.addAction(togglePauseNot) for now we have to hide this as it's not working
                                    .addAction(prevNot)
                            ;

                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(myContext)
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setAutoCancel(true)
                                    .setContentTitle("Currently playing")
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
            String command = "";
            String sendIntent = "com.android.music.musicservicecommand";

            switch (intent.getAction()) {
                case KEY_TOGGLEPAUSE: {
                    if (isPlaying) {
                        command = "pause";
                        sendIntent = "com.android.music.musicservicecommand";
                    } else {
                        command = "play";
                        sendIntent = "com.android.music.musicservicecommand";
                    }

                    isPlaying = !isPlaying;
                } break;

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

        prevPendingIntent        = PendingIntent.getBroadcast(myContext, 0, prevIntent, 0);
        //togglePausePendingIntent = PendingIntent.getBroadcast(myContext, 0, togglePauseIntent, 0);
        nextPendingIntent        = PendingIntent.getBroadcast(myContext, 0, nextIntent, 0);

        prevNot        = new NotificationCompat.Action(R.drawable.ic_music_previous_50, "Previous", prevPendingIntent);
        //togglePauseNot = new NotificationCompat.Action(R.drawable.pause, "Toggle Pause", togglePausePendingIntent);
        nextNot        = new NotificationCompat.Action(R.drawable.ic_music_next_50, "Next", nextPendingIntent);

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
        googleApiClient.disconnect();

        super.onDestroy();
    }
}
