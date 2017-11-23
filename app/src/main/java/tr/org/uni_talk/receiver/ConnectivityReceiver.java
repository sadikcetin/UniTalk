package tr.org.uni_talk.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tr.org.uni_talk.connection.ConnectionJobService;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.e(TAG, "onReceive: " + Intent.ACTION_BOOT_COMPLETED);
            ConnectionJobService.scheduleJob(context.getApplicationContext());
        }
    }
}