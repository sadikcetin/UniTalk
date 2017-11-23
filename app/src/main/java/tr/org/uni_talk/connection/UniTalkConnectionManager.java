package tr.org.uni_talk.connection;

import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sm.StreamManagementException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.IManager;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.conversation.UniTalkConversationManager;
import tr.org.uni_talk.muc.UniTalkMUCManager;
import tr.org.uni_talk.pojo.User;
import tr.org.uni_talk.util.FileUtils;

public class UniTalkConnectionManager implements ConnectionListener, IManager {

    private static final String TAG = UniTalkConnectionManager.class.getName();

    private static final UniTalkConnectionManager INSTANCE;

    static {
        INSTANCE = new UniTalkConnectionManager();
    }

    public static UniTalkConnectionManager getInstance() {
        return INSTANCE;
    }


    private final ExecutorService backgroundJobExecutor;
    private Future<Boolean> job;

    private List<OnAuthorizedListener> authorizationListeners;
    private List<OnDisconnectListener> disconnectionListeners;
    private ConnectionState state;
    private XMPPTCPConnectionConfiguration.Builder configBuilder;
    private XMPPTCPConnection connection;


    public XMPPTCPConnection getConnection() {
        return connection;
    }

    private UniTalkConnectionManager() {
        state = ConnectionState.DISCONNECTED;

        initConfigBuilder();

        authorizationListeners = new ArrayList<OnAuthorizedListener>();
        disconnectionListeners = new ArrayList<OnDisconnectListener>();

        backgroundJobExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, TAG + "-ES");
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }
        });

    }

    private void initConfigBuilder() {
        configBuilder = XMPPTCPConnectionConfiguration.builder();
        try {
            configBuilder
                    .setDebuggerEnabled(true)
                    .setResource(UniTalkConfig.SERVER.RESOURCE_NAME)
                    .setSendPresence(true)
                    .setCompressionEnabled(true)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .setXmppDomain(UniTalkConfig.SERVER.DOMAIN)
                    .setHost(UniTalkConfig.SERVER.HOST)
                    .setPort(UniTalkConfig.SERVER.PORT)
                    .setConnectTimeout(UniTalkConfig.SERVER.CONNECTION_TIMEOUT);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    public void addAuthorizationListener(Object listener) {
        this.authorizationListeners.add((OnAuthorizedListener) listener);
        if (listener instanceof OnDisconnectListener)
            this.addDisconnectionListener((OnDisconnectListener) listener);
    }

    public void addDisconnectionListener(OnDisconnectListener listener) {

    }

    @Override
    public void connected(XMPPConnection connection) {
        Log.i(TAG, "Connection Established");
        state = ConnectionState.CONNECTED;
        try {
            ((XMPPTCPConnection) connection).login();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "connected: fail");
        }
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        Log.e(TAG, "authenticated: ");
        state = ConnectionState.AUTHORIZED;
        if (this.authorizationListeners.size() > 0) {
            for (OnAuthorizedListener listener : this.authorizationListeners)
                listener.onAuthorized(connection);
        } else {
            Log.i(TAG, "authenticated methodunu dinleyecek listener yok");
        }
    }

    @Override
    public void connectionClosed() {
        Log.i(TAG, "Connection Closed");
        state = ConnectionState.DISCONNECTED;
        ConnectionJobService.scheduleJob(UniTalkApplication.getInstance());
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Log.i(TAG, "Connection Closed On Error : " + e.getMessage());
        connectionClosed();
    }

    @Override
    public void reconnectionSuccessful() {
        Log.i(TAG, "Reconnection Successful");
        state = ConnectionState.CONNECTED;
    }

    @Override
    public void reconnectingIn(int seconds) {
        Log.i(TAG, "Reconnecting In " + seconds);
    }

    @Override
    public void reconnectionFailed(Exception e) {
        Log.i(TAG, "Reconnection Failed " + e.getMessage());
    }

    @Override
    public void addManager(IManager manager) {

    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory triggered");
    }

    @Override
    public void onManagerLoad() {
        if (authorizationListeners.size() > 0) authorizationListeners.clear();
        this.addAuthorizationListener(UniTalkConversationManager.getInstance());
        this.addAuthorizationListener(UniTalkAccountManager.getInstance());
        this.addAuthorizationListener(UniTalkMUCManager.getINSTANCE());
    }

    @Override
    public void onManagerStart() {
        Log.i(TAG, "onStart method called " + authorizationListeners.size());
        User user = FileUtils.readDBFile(UniTalkApplication.getInstance().getRegistrationFile());
        configBuilder.setUsernameAndPassword(user.getUserName(), user.getPassword());
        connection = new XMPPTCPConnection(configBuilder.build());
        connection.setUseStreamManagement(true);
        connection.setUseStreamManagementResumption(true);
        connection.setPreferredResumptionTime(30);
        connection.addConnectionListener(this);
        backgroundJobExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.connect();
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void addMessageAcknowledgedListener(final String id, StanzaListener listener) {
        Log.e(TAG, "addMessageAcknowledgedListener: " + id);
        try {
            connection.addStanzaIdAcknowledgedListener(id, listener);
        } catch (StreamManagementException.StreamManagementNotEnabledException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    public boolean isAuthenticated() {
        return connection != null && connection.isAuthenticated();
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

    public void onNetworkChange(ConnectionState newState) {

        if (newState == ConnectionState.CONNECTED) {
            onManagerStart();
        }

    }
}
