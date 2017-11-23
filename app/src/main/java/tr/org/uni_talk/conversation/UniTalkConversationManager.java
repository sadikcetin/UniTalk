package tr.org.uni_talk.conversation;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamRequest;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.connection.OnAuthorizedListener;
import tr.org.uni_talk.connection.OnDisconnectListener;
import tr.org.uni_talk.connection.UniTalkConnectionManager;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.db.Table;
import tr.org.uni_talk.muc.UniTalkMUCManager;
import tr.org.uni_talk.notification.UniTalkNotificationManager;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.ui.SingleConversation;
import tr.org.uni_talk.util.CommonMethods;

public class UniTalkConversationManager implements IConversationListener, OnAuthorizedListener,
        OnDisconnectListener, ReceiptReceivedListener, BytestreamListener, IncomingChatMessageListener{

    private static final String TAG = UniTalkConversationManager.class.getName();
    private static final UniTalkConversationManager INSTANCE;

    static {
        INSTANCE = new UniTalkConversationManager();
    }

    public static UniTalkConversationManager getInstance() {
        return INSTANCE;
    }

    private IConversationListener activeConversation;
    private IConversationManagerListener conversationListener;
    private Map<String, Conversation> conversations;
    private ChatManager chatManager;
    private Chat chat;
    private ChatState state;
    private ChatStateManager chatStateManager;
    private DeliveryReceiptManager deliveryReceiptManager;
    private Socks5BytestreamManager fileTransferManager;
    private String userBehavior = ChatState.gone.toString();
    private XMPPConnection connection;
    String fromContact = null;

    private HashMap<String, String> userBehaviors = null;


    private UniTalkConversationManager() {
        Log.i(TAG, "UniTalkConversation Manager Baslatiliyor");
        conversations = new HashMap<String, Conversation>();
        userBehaviors = new HashMap<String, String>();

    }

    public void addConversationManagerListener(IConversationManagerListener list) {
        this.conversationListener = list;
    }

    public void removeConversationManagerListener(IConversationManagerListener list) {
        this.conversationListener = null;
    }

    public void setActiveConversation(IConversationListener activeConversation) {
        this.activeConversation = activeConversation;
        if (activeConversation != null) {
            createChat(activeConversation.getContact().getNumber());
        }
    }

    private Conversation  getConversation(String fromContact) {
        Conversation conversation = null;
        DBHandler handler = null;
        if (!this.conversations.containsKey(fromContact)){
            Log.i(TAG, "Local Cache Uzerinde " + fromContact + " iliskin kayit bulunamadi DB'e bakmak lazim");
            handler = new DBHandler(UniTalkApplication.getInstance());
            conversation = handler.findConversationById(fromContact);
        } else {
            conversation = this.conversations.get(fromContact);

        }

        return conversation;
    }

    // Yeni mesaj geldiginde cagriliyor.
    @Override
    public void onReceive(String fromContact, ChatMessage chatMessage) {



        Conversation conversation = getConversation(fromContact);
        DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());

        if (conversation == null) {
            Log.i(TAG, "onReceive = "  + fromContact + " ile ilk defa konusuliyor");
            conversation = new Conversation(UniTalkAccountManager.getInstance().findContactByUserName(fromContact));
            this.conversations.put(fromContact, conversation);
            dbh.insertConversation(conversation);
        }

        chatMessage.setConversationID(conversation.getConversationId());
        dbh.insertChatMessage(chatMessage, true);
        conversation.setLastMessage(chatMessage);
        conversation.setTime(getCurrentDateWithTime());
        dbh.updateConversation(conversation);

        if (this.activeConversation != null) {
            Log.i(TAG, this.activeConversation.getContact().getNumber() + " ile aktif konusma penceresine sahibim.");

            if (this.activeConversation.getContact().equals(fromContact)) {

                Log.i(TAG, "onReceive = aktif pencere mesaj ile ilgili");
                conversation.addReadMessage(chatMessage);
                Log.i(TAG, "onReceive = mesaj konusmaya eklendi ve aktif pencereye gonderiliyor.");
                this.activeConversation.onReceive(fromContact, chatMessage);
            } else {
                Log.i(TAG, "onReceive = aktif pencere ve mesaj gonderen farkli : " + fromContact + "!=" + this.activeConversation.getContact().getNumber());
                conversation.addUnreadMessage(chatMessage);
                UniTalkNotificationManager.getInstance().showNotification(UniTalkAccountManager.getInstance().findContactByUserName(fromContact).getNumber(),
                        chatMessage.getBody());
            }
        } else {
            Log.i(TAG, "onReceive = aktif pencere set edilmemis");
            conversation.addUnreadMessage(chatMessage);
            UniTalkNotificationManager.getInstance().showNotification(UniTalkAccountManager.getInstance().findContactByUserName(fromContact).getNumber(),
                    chatMessage.getBody());
        }

        if (this.conversationListener != null)
            this.conversationListener.update(conversation.getConversationId() , chatMessage.getBody() ,conversation);
    }

    @Override
    public void onServerAcknowledgeReceived(String to, String messageID) {
        Log.e(TAG, "onServerAcknowledgeReceived: ");
        new DBHandler(UniTalkApplication.getInstance()).updateChatMessageAsDelivered(messageID, ChatMessage.MESSAGE_SENT);
        if (activeConversation != null && activeConversation.getContact().getNumber().equals(to)) {
            activeConversation.onServerAcknowledgeReceived(to, messageID);
        }
    }

    @Override
    public void onDelivered(String from, String messageID) {
        Log.i(TAG, "onDelivered = " + from + " => " + messageID);
        Conversation conversation = getConversation(CommonMethods.getNumber(from));

        DBHandler handler = new DBHandler(UniTalkApplication.getInstance());
        handler.updateChatMessageAsDelivered(messageID, ChatMessage.MESSAGE_DELIVERED);
        conversation.setMessageDelivered(messageID);
        Log.e(TAG, "onDelivered: " + from);

        if (this.activeConversation != null && this.activeConversation.getContact().equals(CommonMethods.getNumber(from))) {
            Log.e(TAG, "onDelivered: " + activeConversation.getContact().getNumber());
            Log.i(TAG, "onDelivered = aktif pencere mesaj ile ilgili");
            this.activeConversation.onDelivered(from, messageID);
        }

        if (this.conversationListener != null)
            this.conversationListener.update(conversation.getConversationId() , conversation.getLastMessage().getBody(),null);
    }

    @Override
    public void onRead(String from, String messageID) {
        // simdilik onRead mesaji desteklenmiyor.
    }

    public void send(final String to, final ChatMessage message) {
        try {
            //setChatState(ChatState.active);
            UniTalkConnectionManager.getInstance().addMessageAcknowledgedListener(message.getMessageId(), new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                    onServerAcknowledgeReceived(to, packet.getStanzaId());
                }
            });
            chat.send(message.convertToSmackMessage(to));
        } catch (SmackException.NotConnectedException | NullPointerException e) {
            e.printStackTrace();
            final DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());
            dbh.insertChatMessage(message, false);
            dbh.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        onSend(to, message);
    }

    @Override
    public void onSend(final String to, final ChatMessage message) {

        Conversation conversation = getConversation(to);
        final DBHandler dbh= new DBHandler(UniTalkApplication.getInstance());

        if (conversation == null) {
            conversation = new Conversation(UniTalkAccountManager.getInstance().findContactByUserName(to));
            conversation.setLastMessage(message);
            conversation.setTime(getCurrentDateWithTime());
            this.conversations.put(to, conversation);
            dbh.insertConversation(conversation);
        } else {
            conversation.setTime(getCurrentDateWithTime());
            conversation.setLastMessage(message);
            dbh.updateConversation(conversation);
        }


        message.setConversationID(conversation.getConversationId());
        dbh.insertChatMessage(message, true);
        conversation.addReadMessage(message);

        if (this.activeConversation != null && this.activeConversation.getContact().getNumber().equals(to)) {
            // eger aktif konusma penceresi bu mesaja ait ise.
            UniTalkNotificationManager.getInstance().soundNotification(message);
        }

        if (this.conversationListener != null)
            this.conversationListener.update(conversation.getConversationId() , message.getBody(),conversation);

        dbh.close();
    }


    @Override
    public Contact getContact() {
        return null;
    }

    public ChatStateManager getChatStateManager() {
        return chatStateManager;
    }

    @Override
    public void onAuthorized(XMPPConnection connection) {
        Log.e(TAG, "onAuthorized: " + connection.getReplyTimeout());
        this.connection = connection;
        chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener(this);
        deliveryReceiptManager = DeliveryReceiptManager.getInstanceFor(connection);
        deliveryReceiptManager.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
        deliveryReceiptManager.autoAddDeliveryReceiptRequests();
        deliveryReceiptManager.addReceiptReceivedListener(this);
        fileTransferManager = Socks5BytestreamManager.getBytestreamManager(connection);
        fileTransferManager.addIncomingBytestreamListener(this);
        if (activeConversation != null)
            createChat(this.activeConversation.getContact().getNumber());
        final DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());
        for (final ChatMessage message : dbh.getOfflineMessages()) {
            Log.e(TAG, "onAuthorized: " + message.getMessageId());
            Log.e(TAG, "onAuthorized: " + message.getBody());
            Log.e(TAG, "onAuthorized: " + message.getConversationID());
            if (!message.getConversationID().contains("conference")) {
                try {
                    EntityBareJid entityBareJid = JidCreate.entityBareFrom(message.getConversationID() + UniTalkConfig.SERVER.HOST_START_WITH_AT);
                    chat = chatManager.chatWith(entityBareJid);
                    UniTalkConnectionManager.getInstance().addMessageAcknowledgedListener(message.getMessageId(), new StanzaListener() {
                        @Override
                        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                            onServerAcknowledgeReceived(message.getConversationID(), packet.getStanzaId());
                        }
                    });
                    chat.send(message.convertToSmackMessage(message.getConversationID()));
                    dbh.deleteChatMessageFromOffline(message.getMessageId());
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDisconnect() {
        this.chatManager.removeListener(this);
        this.chatManager = null;
        this.deliveryReceiptManager.removeReceiptReceivedListener(this);
        this.deliveryReceiptManager = null;
        this.fileTransferManager.removeIncomingBytestreamListener(this);
        this.fileTransferManager = null;
    }

    public void createChat(String receiver) {
        Log.e(TAG, "createChat: " + receiver);
        try {
            EntityBareJid entityJid = JidCreate.entityBareFrom(receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
            this.chat = this.chatManager.chatWith(entityJid);
            chatManager.addIncomingListener(this);
            //setChatState(ChatState.active);
        } catch (Exception e) {
            //ignore
        }
    }

    public boolean hasConversations() {

        DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());
        if (dbh.getTableRowCount(Table.NAMES.CONVERSATIONS)>0){
            Log.i("CONV:","Tablo Dolu !!!");
            return true;
        }
        Log.i("CONV:","Tablo Boş !!!");
        return false;
    }

    public List<Conversation> getConversations() {

        List<Conversation> results = new ArrayList<>();
        Log.i("CONV:","Tablodan Geliyor !!!");
        Log.i(TAG, this.conversations.size() + " adet konusma gonderildi");

        DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());
        List<Conversation> convList = dbh.getConversations();

        for (Conversation conversation: convList) {
            results.add(conversation);
            Log.e("CONV:","convList Dolu!!!");
        }
        return results;
    }

    public void setChatState(ChatState userBehavior , String to) {
            if (userBehavior.equals(ChatState.composing)){
                sendUserStatus(ChatState.composing ,to);
                Log.e(TAG, "setChatState : " + "composing gönderiliyor" );
            }
            else if (userBehavior.equals(ChatState.gone)){
                sendUserStatus(ChatState.gone ,to);
                Log.e(TAG, "setChatState : " + "gone gönderiliyor" );
            }

            else if (userBehavior.equals(ChatState.active)){
                sendUserStatus(ChatState.active ,to);
                Log.e(TAG, "setChatState : " + "active gönderiliyor" );
            }
    }

    @Override
    public void changeUserState(String conversationId , String member) {
        Log.e(TAG, "Change Status : " + conversationId);
        try {
            if(this.activeConversation != null && this.activeConversation.getContact().getNumber().equals(conversationId)){
                String bufferStatus = userBehaviors.get(this.activeConversation.getContact().getNumber());
                Log.e(TAG, "changeUserState: " + "update edilecek single conversation" );
                this.activeConversation.changeUserState(bufferStatus == null ? "Outside" : convertUserBehavior(bufferStatus) , null);
            }

            if (this.conversationListener != null && userBehaviors.get(conversationId) != null && userBehaviors.get(conversationId).equals(ChatState.composing.toString())){
                Log.e(TAG, "Conversation Listener null değil ve active ya da typing");
                this.conversationListener.update(conversationId,"Typing...",null);
            }

            else if(this.conversationListener != null && userBehaviors.get(conversationId) != null && !userBehaviors.get(conversationId).equals(ChatState.composing.toString())){
                Log.e(TAG, "Conversation Listener null değil ve outside, last message basılacak sohbetler ekranına");
                Conversation conversation = getConversation(conversationId);
                this.conversationListener.update(conversationId,conversation.getLastMessage().getBody(),null);
            }
        }catch (NullPointerException npe){
            npe.printStackTrace();
        }

    }

    public String getCurrentDateWithTime(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String time = dateFormat.format(date);
        Log.e(TAG, "onSend Date : " + time);
        return time;
    }


    public String createDir(String folderName) {
        final File file = new File(Environment.getExternalStorageDirectory() + File.separator +
                UniTalkConfig.INTERNAL.APP_DATA_FOLDER, folderName);
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return file.getPath();
    }

    public byte[] prepareForSending(String name, int orientation, InputStream is) {
        if (isExternalStorageWritable()) {
            String path = createDir("Sent");
            final File file = new File(path, name);
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        bitmap = rotate(bitmap, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        bitmap = rotate(bitmap, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        bitmap = rotate(bitmap, 270);
                        break;
                    default:
                        break;
                }
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, os);
                FileOutputStream fos = new FileOutputStream(file);
                os.writeTo(fos);
                os.flush();
                os.close();
                fos.flush();
                fos.close();
                return os.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void sendFile(String name, String jid, byte[] bytes) throws XMPPException, SmackException, IOException, InterruptedException {

       /* final OutgoingFileTransfer outgoingFileTransfer = fileTransferManager.createOutgoingFileTransfer(XmppStringUtils.completeJidFrom(jid, UniTalkConfig.SERVER.DOMAIN, UniTalkConfig.SERVER.RESOURCE_NAME));

        OutputStream os = outgoingFileTransfer.sendFile(name, size, description);
        os.write(bytes);
        os.flush();
        os.close();

        onDelivered(jid, name);*/
        EntityJid entityJid = JidCreate.entityBareFrom(XmppStringUtils.completeJidFrom(jid, UniTalkConfig.SERVER.DOMAIN, UniTalkConfig.SERVER.RESOURCE_NAME));
        BytestreamSession session = fileTransferManager.establishSession(entityJid, name);
        OutputStream os = session.getOutputStream();
        os.write(bytes);
        os.flush();
        os.close();
        session.close();

        /*OutputStream os;
        //
        //bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/2, bitmap.getHeight()/2, true);*/
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File file = new File(Environment.getExternalStorageDirectory(), File.separator + UniTalkConfig.INTERNAL.APP_DATA_FOLDER);
            if (!file.isDirectory()) {
                file.mkdirs();
            }
            return true;
        }
        return false;
    }

    @Override
    public void incomingBytestreamRequest(BytestreamRequest request) {
        if (isExternalStorageWritable()) {
            try {
                ((Socks5BytestreamRequest) request).setMinimumConnectTimeout(10000);
                BytestreamSession session = request.accept();
                InputStream is = session.getInputStream();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    byteArrayOutputStream.write(data, 0, nRead);
                }

                byteArrayOutputStream.flush();

                String path = createDir("Received");
                File file = new File(path, request.getSessionID());
                OutputStream outputStream = new FileOutputStream(file);
                byteArrayOutputStream.writeTo(outputStream);
                byteArrayOutputStream.close();
                outputStream.flush();
                outputStream.close();

                final ChatMessage message = new ChatMessage("");
                message.setMessageId(request.getSessionID());
                onReceive(CommonMethods.getNumber(request.getFrom().asBareJid().toString()), message);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {
        if (!fromJid.toString().contains("conference")) {
            Log.e(TAG, "onReceiptReceived: " + fromJid.asBareJid().toString());
            onDelivered(fromJid.toString(), receiptId);
        }

    }

    @Override
    public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
        String messageFrom = CommonMethods.getNumber(message.getFrom().toString());
        Log.e(TAG, "newIncomingMessage: from: " + messageFrom );
        Log.e(TAG, "newIncomingMessage: stanzaId: " + message.getStanzaId() );
        String messageBody = message.getBody();
        Log.e(TAG, "newIncomingMessage: body: " + messageBody );
        Log.e(TAG, "newIncomingMessage: type: " + message.getType());
        if (messageBody != null && !messageBody.trim().isEmpty()) {
            messageBody = message.getBody().toString().trim();
            onReceive(messageFrom, new ChatMessage(messageBody));
        }else if (!message.getExtensions().isEmpty() && !messageFrom.equals(CommonMethods.getNumber(message.getTo().toString()))) {
            Log.e(TAG, "newIncomingMessage: extension: " + message.getExtensions().get(0).getElementName());
            userBehaviors.put(messageFrom, message.getExtensions().get(0).getElementName());
            changeUserState(messageFrom , null);
        }
    }

    public void sendUserStatus(ChatState chatState , String to) {
        try {
            ChatMessage chatMessage = new ChatMessage("");
            Message message = chatMessage.convertToSmackMessage(to);
            ChatStateExtension chatStateExtension = new ChatStateExtension(chatState);
            message.addExtension(chatStateExtension);
            chat.send(message);
        } catch (SmackException.NotConnectedException | NullPointerException e) {
            //ignore
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    public void deleteConversationAndChatMessages(String conversationId){
        Log.e(TAG, "deleteChatMessages: " + "is called.");
        DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
        Log.e(TAG, "deleteChatMessages: " + "is called. Remove ...");
        conversations.remove(conversationId);
        UniTalkConversationManager.getInstance().getConversations().remove(conversationId);
        dbHandler.deleteChatMessages(conversationId);
        dbHandler.deleteConversations(conversationId);
    }

    public String convertUserBehavior(String userBehavior){
        String bufferString = null;
        if(userBehavior.equals(ChatState.composing.toString()))
            bufferString = "Typing...";
        else if(userBehavior.equals(ChatState.active.toString()))
            bufferString = "Active";
        else if(userBehavior.equals(ChatState.gone.toString()))
            bufferString = "Outside";
        return bufferString;
    }
}
