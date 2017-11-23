package tr.org.uni_talk.connection;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import tr.org.uni_talk.service.ConnectionService;


public class ConnectionJobService extends JobService {
    private final String TAG = this.getClass().getName();

    public static void scheduleJob(Context context) {
        Log.e("ConnectionJobService", "scheduleJob: ");

        ComponentName componentName = new ComponentName(context, ConnectionJobService.class);

        JobInfo job = new JobInfo.Builder(0, componentName)
                .setMinimumLatency(0)
                .setBackoffCriteria(30000,JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();



        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.schedule(job);

        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.e(TAG, "onStartJob: ");
        startService(ConnectionService.createIntent(getApplicationContext()));
        jobFinished(params, true);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}