package tr.org.uni_talk.account;

import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterLoadedListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iqlast.LastActivityManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.connection.ConnectionJobService;
import tr.org.uni_talk.connection.OnAuthorizedListener;
import tr.org.uni_talk.connection.OnDisconnectListener;
import tr.org.uni_talk.connection.UniTalkConnectionManager;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.db.Table;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.User;
import tr.org.uni_talk.util.CommonMethods;
import tr.org.uni_talk.util.FileUtils;

public class UniTalkAccountManager extends ContentObserver implements OnAuthorizedListener, OnDisconnectListener {

    private static final String TAG = UniTalkAccountManager.class.getName();
    private static final UniTalkAccountManager INSTANCE;
    private static DBHandler dbHandler;

    static {
        INSTANCE = new UniTalkAccountManager();
        dbHandler = new DBHandler(UniTalkApplication.getInstance());
    }

    public static UniTalkAccountManager getInstance() {
        return INSTANCE;
    }

    private List<OnContactsLoadListener> contactsLoadListeners;

    private User user;
    private Roster roster;
    private boolean loaded;
    private LastActivityManager lastActivityManager;

    private Map<String, Contact> contacts;

    private UniTalkAccountManager() {
        super(null);
        Log.e(TAG, "UniTalkAccountManager: " + "is starting");
        UniTalkApplication.getInstance().getApplicationContext().getContentResolver()
                .registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, this);
        contacts = new HashMap<String, Contact>();
        this.contactsLoadListeners = new ArrayList<OnContactsLoadListener>();
        loaded = false;
    }

    @Override
    protected void finalize() throws Throwable {
        UniTalkApplication.getInstance().getApplicationContext().getContentResolver().unregisterContentObserver(this);
        super.finalize();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Log.e(TAG, "onChange: ");
        if (UniTalkConnectionManager.getInstance().isConnected()) {
            contacts.clear();
            dbHandler.deleteAll(Table.NAMES.CONTACTS);
            rosterEntriesToContacts();
        } else {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(UniTalkApplication.getInstance()).edit();
            editor.putBoolean("contactsChanged", true);
            editor.apply();
            ConnectionJobService.scheduleJob(UniTalkApplication.getInstance());
        }
    }

    public User getAccount() {
        return this.user;
    }

    public Contact findContactByUserName(String userName) {
        Contact contact = null;
        if (this.contacts != null && this.contacts.size() > 0) {
            contact = this.contacts.get(CommonMethods.getBareJID(userName));
            if (contact != null)
                return contact;
            return null;
        }
        return null;
    }

    public void getContactsFromDB() {
        if (!contacts.isEmpty()) return;
        this.user = FileUtils.readDBFile(UniTalkApplication.getInstance().getRegistrationFile());
        for (Contact contact : dbHandler.getAllContacts()) {
            if (contact != null) {
                String key = contact.getNumber();
                if (!this.contacts.containsKey(key)) {
                    this.contacts.put(key, contact);
                }
            }
        }
        notifyContactLoadListeners();
        loaded = true;
    }

    @Override
    public void onAuthorized(XMPPConnection connection) {
        Log.e(TAG, " onAuthorized method called");
        boolean contactsChanged = PreferenceManager.getDefaultSharedPreferences(UniTalkApplication.getInstance()).getBoolean("contactsChanged", false);
        this.roster = Roster.getInstanceFor(connection);
        if (contactsChanged) {
            contacts.clear();
            dbHandler.deleteAll(Table.NAMES.CONTACTS);
            PreferenceManager.getDefaultSharedPreferences(UniTalkApplication.getInstance())
                    .edit()
                    .remove("contactsChanged")
                    .apply();
        }

        // tablo var mı ve kayıt sayısı 0 dan farklı mı
        // evet ıse dırek verı tabanından oku map'ı doldurç
        // HAYIR ıse Roster.getInstanceFor(connection);

        if(dbHandler.getTableRowCount(Table.NAMES.CONTACTS) != 0 ){
            getContactsFromDB();
            this.user = new User(connection.getUser().asEntityBareJidString());
            lastActivityManager = LastActivityManager.getInstanceFor(connection);
        }else{
            try {
                //this.roster.reload();
                this.roster.addRosterLoadedListener(new RosterLoadedListener() {
                    @Override
                    public void onRosterLoaded(Roster roster) {
                        Log.i(TAG, roster.getEntries().size() + " adet elemani var");
                        rosterEntriesToContacts();
                    }

                    @Override
                    public void onRosterLoadingFailed(Exception exception) {
                        Log.e(TAG, "onRosterLoadingFailed: " + "fail oldu");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.user = new User(connection.getUser().asEntityBareJidString());
            lastActivityManager = LastActivityManager.getInstanceFor(connection);
        }
    }


    private void sendPresence(XMPPConnection conn) {
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus("I’m unavailable");
        presence.setMode(Presence.Mode.available);
        try {
            ((XMPPTCPConnection) conn).sendStanza(presence);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isRegistered() {
        return new File(UniTalkApplication.getInstance().getFilesDir(), UniTalkConfig.INTERNAL.KEY_NAME).exists();
    }

    public void rosterEntriesToContacts() {
        if (roster == null) return;
        Contact contact = null;
        for (RosterEntry entry : roster.getEntries()) {
            contact = rosterEntryToContact(entry);
            if (contact != null) {
                String key = contact.getNumber();
                this.contacts.put(key, contact);
                dbHandler.insertContact(contact);
            }
        }
        notifyContactLoadListeners();
        loaded = true;
    }

    private Contact rosterEntryToContact(RosterEntry entry) {
        Contact contact = null;
        if (entry != null) {
            String name = CommonMethods.getNameFromPhoneNumber(UniTalkApplication.getInstance().getApplicationContext(), CommonMethods.getNumber(entry.getJid().toString()));
            String number = CommonMethods.getNumber(entry.getJid().toString());
            contact = new Contact();
            contact.setNumber(number);
            contact.setName(name == null ? number : name);
        }
        return contact;
    }


    public List<Contact> getContactList() {
        List<Contact> results = new ArrayList<Contact>();
        getContactsFromDB();
        for (Contact contact : this.contacts.values()) {
            results.add(contact);
        }

        return results;
    }

    public void addContactLoadListener(OnContactsLoadListener listener) {
        this.contactsLoadListeners.add(listener);
    }

    public void removeContactLoadListener(OnContactsLoadListener listener) {
        this.contactsLoadListeners.remove(listener);
    }

    private void notifyContactLoadListeners() {
        if (this.contactsLoadListeners != null) {
            for (OnContactsLoadListener listener : this.contactsLoadListeners) {
                listener.onContactsLoaded();
            }
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void onDisconnect() {

    }
}
