package tr.org.uni_talk.app;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.service.ConnectionService;
import tr.org.uni_talk.util.FileUtils;


public class UniTalkApplication extends Application implements IManager {

    private static final String TAG = UniTalkApplication.class.getName();

    private static UniTalkApplication instance;

    private boolean initialized = false;
    private boolean started;
    private boolean applicationVisible = true;
    private List<IManager> managers;

    public UniTalkApplication() {
        Log.i(TAG, "UniTalkApplication constructor.");
        instance = this;
        managers = new ArrayList<IManager>();
        started = false;
    }

    public static UniTalkApplication getInstance() {
        if (instance == null) {
            throw new IllegalStateException();
        }

        return instance;
    }

    public boolean isConnectionServiceRunning() {
        String className = ConnectionService.class.getName();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "UniTalkApplication Baslatildi." + this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "OnLowMemory Called");
        if (this.managers.size() > 0) {
            for (IManager manager : this.managers) {
                manager.onLowMemory();
            }
        }
    }



    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "OnTerminate Called");

    }

    public void inBackground() {
        this.applicationVisible = false;
    }

    public void inForeground() { this.applicationVisible = true;}

    public File getRegistrationFile() {
        return new File(this.getApplicationContext().getFilesDir(), UniTalkConfig.INTERNAL.KEY_NAME);
    }

    public boolean isRegistered() {
        return new File(this.getApplicationContext().getFilesDir(), UniTalkConfig.INTERNAL.KEY_NAME).exists();
    }

    public boolean registerAccount(String userName, String password) {
        Log.e(TAG, "registerAccount: register edildi");
        FileUtils.write(UniTalkApplication.getInstance().getRegistrationFile(), userName, password);
        return true;
    }

    @Override
    public void onManagerStart() {
        Log.i(TAG, "UNITALK APP IS " + started + " " + this.managers.size());
        if (!started) {
            if (this.managers.size() > 0) {
                for (IManager manager : this.managers) {
                    manager.onManagerLoad();
                    manager.onManagerStart();
                }
            }
            started = true;
        }
    }

    @Override
    public void onManagerLoad() {
        if (this.managers.size() > 0) {
            for (IManager manager : this.managers) {
                manager.onManagerStart();
            }
        }
    }

    @Override
    public void addManager(IManager manager) {
        this.managers.add(manager);
    }

    public void deleteAppData() {
        try {
            // clearing app data
            String packageName = getApplicationContext().getPackageName();
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("pm clear " + packageName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
