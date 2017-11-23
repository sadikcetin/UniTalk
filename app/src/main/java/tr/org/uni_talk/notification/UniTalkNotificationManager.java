package tr.org.uni_talk.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.ui.GroupChatConversation;
import tr.org.uni_talk.ui.SingleConversation;
import tr.org.uni_talk.util.CommonMethods;

public class UniTalkNotificationManager {
    private static final String TAG = UniTalkNotificationManager.class.getSimpleName();
    private static final UniTalkNotificationManager INSTANCE;
    private static final Uri DEFAULT_NOTIFICATION_SOUND;
    private Map< String , Integer > cache = null;

    static {
        INSTANCE = new UniTalkNotificationManager();
        DEFAULT_NOTIFICATION_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    public static UniTalkNotificationManager getInstance() {
        return INSTANCE;
    }


    private final UniTalkApplication application;
    private NotificationCompat.Builder notificationBuilder;
    private final NotificationManager androidNotificationManager;

    private UniTalkNotificationManager() {
        this.application = UniTalkApplication.getInstance();
        androidNotificationManager = (NotificationManager) this.application.getSystemService(Context.NOTIFICATION_SERVICE);
        cache = new HashMap<>();
        initNotificationBuilder();
    }

    private void initNotificationBuilder() {
        int color = 0x1E8BC3;
        notificationBuilder = new NotificationCompat.Builder(application).
                setSmallIcon(R.drawable.ic_stat_name).setColor(color);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            notificationBuilder.setSmallIcon(R.drawable.ic_stat_name);
            notificationBuilder.setColor(color);

        }

    }

    private String trimMessage(String message) {
        int maxlen = 20;
        if (message.length() > maxlen)
            return message.substring(0, maxlen) + "...";
        return message;
    }

    private Uri getSound(int type) {
        switch (type) {
            case NotificationType.DEFAULT:
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            case NotificationType.OTHER:
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            case NotificationType.SILENT:
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            case NotificationType.NONE:
            default:
                return null;

        }
    }

    public void soundNotification(ChatMessage message) {

    }

    public void showNotification(String from, String msg, int type) {

        Intent targetIntent = null;
        Log.e(TAG, "showNotification: " + from);
        Log.e(TAG, "showNotification: " + msg);
        if(from.contains("conference")){
            notificationBuilder.setContentTitle(CommonMethods.getSecondBareJID(from));
            targetIntent = new Intent(this.application, GroupChatConversation.class);
        }else {
            String userNumber = CommonMethods.getNameFromPhoneNumber(UniTalkApplication.getInstance().getApplicationContext(),from);
            notificationBuilder.setContentTitle(userNumber == null ? from : userNumber);
            targetIntent = new Intent(this.application, SingleConversation.class);
        }

        Log.e(TAG,"showNotification: "+ from);
        targetIntent.putExtra("incoming", true);
        targetIntent.putExtra("conversationID", from);
        PendingIntent contentIntent = PendingIntent.getActivity(this.application, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri ringToneURI = getSound(type);

        int notificationId , numberMessages = 0;

        if (ringToneURI != null) {
            notificationBuilder.setSound(ringToneURI);
        }
        notificationBuilder.setContentIntent(contentIntent);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(msg));
        notificationBuilder.setContentText(msg);
        if(cache.containsKey(from)){
            notificationId = cache.get(from);
        }else {
            notificationId = genereteNotificationId();
            cache.put(from , notificationId);
        }
        androidNotificationManager.notify(notificationId, notificationBuilder.build());
    }

    public void showNotification(String from, String message) {
       showNotification(from, message, NotificationType.DEFAULT);

    }

    public int genereteNotificationId(){
        int generatedId =  new Random().nextInt(100000);
        return cache.containsValue(generatedId) ? genereteNotificationId() : generatedId;
    }


}
