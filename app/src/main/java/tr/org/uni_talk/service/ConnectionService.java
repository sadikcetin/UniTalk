package tr.org.uni_talk.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import tr.org.uni_talk.connection.UniTalkConnectionManager;

public class ConnectionService extends Service {

    private static final String TAG = ConnectionService.class.getName();

    UniTalkConnectionManager connectionManager;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate method called with : " + this);
        super.onCreate();
        connectionManager = UniTalkConnectionManager.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand Called" + connectionManager.getState());
        if (!connectionManager.isAuthenticated()) {
            connectionManager.onManagerLoad();
            connectionManager.onManagerStart();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy method called");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public UniTalkConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, ConnectionService.class);
    }
}
