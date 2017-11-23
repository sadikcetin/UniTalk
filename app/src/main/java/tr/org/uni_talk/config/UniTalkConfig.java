package tr.org.uni_talk.config;

import android.content.IntentFilter;

public interface UniTalkConfig {

    interface SERVER {
        String HOST = ""; // Server Hostname
        String HOST_START_WITH_AT = "@" + ""; // Virtual Hostname
        String CONFERENCE_DOMAIN_WITH_AT = ""; // Group Chat Domain
        String DOMAIN = ""; // Virtual Hostname
        String RESOURCE_NAME = "Android";
        int PORT = 80;
        int CONNECTION_TIMEOUT = 10000;

    }

    interface INTERNAL {
        IntentFilter NETWORK_INTENTFILTER = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        String DB_NAME = ""; // Local DB name
        String KEY_NAME = "ut.key";
        String MSG_ID_FORMAT = "MSG-%d%d";
        String IMG_ID_FORMAT = "IMG-%d%d.%s";
        String APP_DATA_FOLDER = "UniTalk";
    }
}
