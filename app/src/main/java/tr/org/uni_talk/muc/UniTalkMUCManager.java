package tr.org.uni_talk.muc;

import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xevent.MessageEventManager;
import org.jivesoftware.smackx.xevent.MessageEventNotificationListener;
import org.jivesoftware.smackx.xevent.packet.MessageEvent;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.connection.OnAuthorizedListener;
import tr.org.uni_talk.conversation.IConversationListener;
import tr.org.uni_talk.conversation.IConversationManagerListener;
import tr.org.uni_talk.conversation.UniTalkConversationManager;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.notification.UniTalkNotificationManager;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.ui.GroupChatConversation;
import tr.org.uni_talk.util.CommonMethods;
import tr.org.uni_talk.util.FileUtils;


public class UniTalkMUCManager extends DefaultParticipantStatusListener implements OnAuthorizedListener , IConversationListener , InvitationListener , MessageListener , MessageEventNotificationListener,ItemEventListener,StanzaListener , IRoomInfoListener,IGroupConversationListener{

    private static final String TAG = UniTalkMUCManager.class.getName();
    private static final UniTalkMUCManager INSTANCE;

    private MultiUserChatManager mucManager = null;
    private MultiUserChat muc = null;
    private IConversationListener activeConversation;
    private IConversationManagerListener conversationListener;
    private Map<String , Conversation> conversations = null;
    private List<Contact> members;
    private Map<String, String> userStatus = null;
    private XMPPConnection connection = null;
    private MessageEventManager eventManager = null;
    private PubSubManager pubSubManager;
    private IRoomInfoListener roomInfoListener;
    private IGroupConversationListener groupConversationListener;
    String userNumber = null;

    static {
        Log.i(TAG,"UniTalkMUCManager Başlatıldı.");
        INSTANCE = new UniTalkMUCManager();
    }

    private UniTalkMUCManager(){
        conversations = new HashMap<>();
        userStatus = new HashMap<>();

    }

    public IConversationListener getActiveConversation() {
        return activeConversation;
    }

    public void setActiveConversation(IConversationListener activeConversation) {
        this.activeConversation = activeConversation;
    }

    public IConversationManagerListener getConversationListener() {
        return conversationListener;
    }

    public void setConversationListener(IConversationManagerListener conversationListener) {
        this.conversationListener = conversationListener;
    }

    public void onCreateMUC(String groupName, List<Contact> contacts) throws XmppStringprepException {
        mucManager.getMultiUserChat(JidCreate.entityBareFrom(groupName + UniTalkConfig.SERVER.CONFERENCE_DOMAIN_WITH_AT));
    }

    @Override
    public void onAuthorized(XMPPConnection connection) {
        this.connection = connection;
        StanzaFilter filter=new StanzaTypeFilter(Presence.class);
        this.connection.addSyncStanzaListener(this,filter);
        mucManager = MultiUserChatManager.getInstanceFor(this.connection);
        mucManager.addInvitationListener(this);
        eventManager = MessageEventManager.getInstanceFor(connection);
        eventManager.addMessageEventNotificationListener(this);
        pubSubManager = PubSubManager.getInstance(connection);
        userNumber = FileUtils.readDBFile(UniTalkApplication.getInstance().getRegistrationFile()).getUserName();

        Log.e(TAG, "onAuthorized: this " + this.toString());

        DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
        List<Conversation> joinedRooms = dbHandler.getGroupConversations();
        for (Conversation conversation : joinedRooms) {
            try {
                muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(conversation.getConversationId()));
                Log.e(TAG, "onAuthorized: " + muc.getRoom());
                Map<String , String> map = getRoomInfoFromLeafNode(CommonMethods.getNumber(conversation.getConversationId()),true);
                conversation.setReceiverString(map.get("members"));
                conversation.setOwners(map.get("owners"));
                if(!conversation.getReceiverString().contains(userNumber)){
                    conversation.setJoinable(0);
                    conversation.setLastMessage(new ChatMessage("You left this group..."));
                    unsubscribeAndUpdateRoomInfoLeafNode(CommonMethods.getNumber(conversation.getConversationId()));
                    showNotificaiton(conversation);
                }else{
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    MucEnterConfiguration.Builder mucEnterConfiguration = muc.getEnterConfigurationBuilder(Resourcepart.from(userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT));
                    Date date = simpleDateFormat.parse(conversation.getTime());
                    date.setTime(date.getTime()+30000);
                    mucEnterConfiguration.requestHistorySince(date);
                    muc.join(mucEnterConfiguration.build());
                    muc.addMessageListener(this);
                    muc.addParticipantStatusListener(this);
                }
                updateLeaveButton(conversation);
                updateAuthorizations(conversation);
                updateAffiliations(conversation,dbHandler);
                changeUserState("",conversation.getReceiverString());
                dbHandler.updateConversationRoomInfo(conversation);
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                conversation.setJoinable(0);
                conversation.setLastMessage(new ChatMessage("you left this group."));
                updateLeaveButton(conversation);
                updateAuthorizations(conversation);
                updateAffiliations(conversation,dbHandler);
                dbHandler.updateConversationRoomInfo(conversation);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            } catch (MultiUserChatException.NotAMucServiceException e) {
                e.printStackTrace();
            }
        }

        final DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());
        for (ChatMessage message : dbh.getOfflineMessages()) {
            if (message.getConversationID().contains("conference")) {
                try {
                    UniTalkMUCManager.getINSTANCE().getMUC(CommonMethods.getNumber(message.getConversationID()))
                            .sendMessage(message.convertToSmackMessage(message.getConversationID(),
                                    userNumber + UniTalkConfig.SERVER.HOST_START_WITH_AT));
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
        Log.i(TAG,"MultiUserChatManager nesnesi oluşturuldu.");
    }


    public MultiUserChat getMUC(String roomName) throws XmppStringprepException,NullPointerException {
        String mucJid = null;
        if (!roomName.contains("conference"))
            mucJid = roomName + UniTalkConfig.SERVER.CONFERENCE_DOMAIN_WITH_AT;
        muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(mucJid));
        Log.i(TAG,"MultiUserChat nesnesi oluşturuldu.");
        return  muc;
    }

    public MultiUserChat getMuc() {
        return muc;
    }

    public static UniTalkMUCManager getINSTANCE() {
        return INSTANCE;
    }

    public void createOrJoinRoom(String roomName, List<Contact> contacts) throws XMPPException.XMPPErrorException, SmackException {
        try {
            muc = getMUC(roomName);
            muc.create(Resourcepart.from(userNumber + UniTalkConfig.SERVER.HOST_START_WITH_AT));
            Form form = muc.getConfigurationForm().createAnswerForm();
            form.setAnswer("muc#roomconfig_roomname", roomName);
            form.setAnswer("muc#roomconfig_persistentroom", true);
            form.setAnswer("muc#roomconfig_membersonly",true);
            form.setAnswer("muc#roomconfig_changesubject",true);

            muc.sendConfigurationForm(form);
            muc.addParticipantStatusListener(this);
            members = contacts;
            Conversation conversation = new Conversation(muc.getRoom().asEntityBareJidString());
            Log.e(TAG, "createOrJoinRoom: " + muc.getRoom());
            conversation.setMembers(members);
            conversation.setReceiverString(conversation.getMemberContactNumber());
            conversation.setRoomName(checkGroupName(roomName));
            conversation.setJoinable(1);
            conversation.setOwners(userNumber+",");
            conversation.setLastMessage(new ChatMessage("You created this group"));

            this.conversations.put(muc.getRoom().asEntityBareJidString(), conversation);
            new DBHandler(UniTalkApplication.getInstance()).insertMultiUserConversation(conversation);

            createAndPublishRoomInfo(CommonMethods.getNumber(muc.getRoom().asEntityBareJidString()), conversation.getMemberContactNumber(), userNumber + ",", null);

            if (this.conversationListener != null)
                this.conversationListener.update(conversation.getConversationId(),"This is a new room :D",conversation);

            for (Contact contact : contacts) {
                muc.grantMembership(JidCreate.entityBareFrom(contact.getNumber() + UniTalkConfig.SERVER.HOST_START_WITH_AT));
            }

            for (Contact contact : contacts){
                Log.e(TAG, "createOrJoinRoom: invite: " + JidCreate.entityBareFrom(contact.getNumber() + UniTalkConfig.SERVER.HOST_START_WITH_AT + "/" + roomName).toString() );
                muc.invite(JidCreate.entityBareFrom(contact.getNumber() + UniTalkConfig.SERVER.HOST_START_WITH_AT + "/" + roomName), conversation.getMemberContactNumber());
            }
            
            muc.addMessageListener(this);
        } catch (XMPPException.XMPPErrorException | SmackException e) {
            throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(String conversationId, ChatMessage chatMessage) {
        Log.i(TAG, "ConversationId :" + conversationId);
        Conversation conversation = getConversation(conversationId);
        DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());

        if(chatMessage != null){
            chatMessage.setConversationID(conversation.getConversationId());
            dbh.insertChatMessage(chatMessage, true);
            conversation.setLastMessage(chatMessage);
            conversation.setTime(getCurrentDateWithTime());
            dbh.updateConversation(conversation);
        }

        if (this.activeConversation != null) {
            String logConversation = ((GroupChatConversation)(this.activeConversation)).getConversationId();
            Log.i(TAG, logConversation + " ile aktif konusma penceresine sahibim.");
            if (((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversationId)) {
                Log.i(TAG, "onReceive = aktif pencere mesaj ile ilgili");
                conversation.addReadMessage(chatMessage);
                Log.i(TAG, "onReceive = mesaj konusmaya eklendi ve aktif pencereye gonderiliyor.");
                this.activeConversation.onReceive(conversationId, chatMessage);
            } else {
                conversation.addUnreadMessage(chatMessage);
                UniTalkNotificationManager.getInstance().showNotification(conversationId + "/" + conversation.getRoomName(),
                        chatMessage.getBody());
            }
        } else {
            Log.i(TAG, "onReceive = aktif pencere set edilmemis");
            conversation.addUnreadMessage(chatMessage);
            UniTalkNotificationManager.getInstance().showNotification(conversationId + "/" + conversation.getRoomName(),
                    chatMessage.getBody());
        }

        if (this.conversationListener != null)
            this.conversationListener.update(conversationId,chatMessage.getBody(),null);
    }

    @Override
    public void onServerAcknowledgeReceived(String to, String messageID) {

    }

    @Override
    public void onDelivered(String from, String messageID) throws IllegalStateException {
        DBHandler handler = new DBHandler(UniTalkApplication.getInstance());
        String deliveredNumbers = handler.addNumberToDelivered(messageID,from);
        Conversation conversation = handler.findConversationById(handler.getConversationIdByMessageId(messageID));
        String membersNumbers = conversation.getReceiverString();

        if(isDeliveredToMembers(membersNumbers,deliveredNumbers)){
            handler.updateChatMessageAsDelivered(messageID, ChatMessage.MESSAGE_DELIVERED);
            conversation.setMessageDelivered(messageID);

            if (this.activeConversation != null && ((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversation.getConversationId())) {
                this.activeConversation.onDelivered(from, messageID);
            }
        }
    }

    public boolean isDeliveredToMembers(String members , String delivered){
        List<String> memberList = Arrays.asList(members.split(","));
        for (String memberNumber:memberList) {
            if(!delivered.contains(memberNumber)){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRead(String from, String messageID) {

    }

    public void sendMessage(String to, ChatMessage chatMessage) {
        try {
            Message message = chatMessage.convertToSmackMessage(to,
                    userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT);
            chatMessage.setMessageId(message.getStanzaId());
            getMUC(CommonMethods.getNumber(to)).sendMessage(message);
            sendUserStatus(ChatState.active);
        } catch (SmackException.NotConnectedException | NullPointerException e) {
            e.printStackTrace();
            final DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());
            dbh.insertChatMessage(chatMessage, false);
            dbh.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        onSend(to, chatMessage);
    }

    @Override
    public void onSend(String to, ChatMessage chatMessage) {
        Log.e(TAG, "onSend: ---------------------> " + to );
        if(!to.contains("conference"))
            to = to + "@conference." + UniTalkConfig.SERVER.DOMAIN;
        Conversation conversation = getConversation(to);
        final DBHandler dbh= new DBHandler(UniTalkApplication.getInstance());

        if (conversation == null) {
            conversation = new Conversation(to);
            conversation.setMembers(members);
            conversation.setTime(getCurrentDateWithTime());
            conversation.setLastMessage(chatMessage);
            this.conversations.put(to, conversation);
            dbh.insertMultiUserConversation(conversation);
        } else {
            conversation.setTime(getCurrentDateWithTime());
            conversation.setLastMessage(chatMessage);
            dbh.updateConversation(conversation);
        }

        chatMessage.setConversationID(conversation.getConversationId());
        dbh.insertChatMessage(chatMessage ,true);
        conversation.addReadMessage(chatMessage);

        if (this.activeConversation != null && ((GroupChatConversation)(this.activeConversation)).getConversationId().equals(to)) {
            UniTalkNotificationManager.getInstance().soundNotification(chatMessage);
        }

        if (this.conversationListener != null)
            this.conversationListener.update(conversation.getConversationId(),chatMessage.getBody(),null);

        dbh.close();
    }

    @Override
    public Contact getContact() {
        return null;
    }


    @Override
    public void invitationReceived(XMPPConnection conn, MultiUserChat room, EntityJid inviter, String reason, String password, Message message, MUCUser.Invite invitation) {
        Log.e(TAG, "invitationReceived: " + room.getRoom());
        Log.e(TAG, "invitationReceived: inviter:" + inviter);
        try {
            muc = room;
            MucEnterConfiguration.Builder mucEnterConfiguration = muc.getEnterConfigurationBuilder(Resourcepart.from(userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT));
            mucEnterConfiguration.requestMaxStanzasHistory(0);
            muc.join(mucEnterConfiguration.build());
            muc.addMessageListener(this);
            muc.addParticipantStatusListener(this);

            Map<String, String> affliations = getRoomInfoFromLeafNode(CommonMethods.getNumber(muc.getRoom().asEntityBareJidString()), true);

            RoomInfo roomInfo = mucManager.getRoomInfo(muc.getRoom());

            String roomName = checkGroupName(roomInfo.getName());
            DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
            Conversation conversation = new Conversation(muc.getRoom().toString());
            conversation.setReceiverString(affliations.get("members"));
            conversation.setOwners(affliations.get("owners"));
            conversation.setRoomName(roomName);
            conversation.setTime(getCurrentDateWithTime());

            if(conversation.getReceiverString().contains(userNumber)){
                conversation.setLastMessage(new ChatMessage("You joined this group..."));
                conversation.setJoinable(1);
            }else {
                conversation.setLastMessage(new ChatMessage("You left this group..."));
                conversation.setJoinable(0);
            }

            this.conversations.put(conversation.getConversationId(), conversation);

            if(!dbHandler.hasConversation(conversation.getConversationId())){
                Log.e(TAG, "invitationReceived: " + "conversation bulunamadı" );
                dbHandler.insertMultiUserConversation(conversation);
            }else {
                Log.e(TAG, "invitationReceived: " + "conversation bulundu. Update edilecek" );
                dbHandler.updateConversationRoomInfo(conversation);
            }

            if (this.conversationListener != null)
                this.conversationListener.update(conversation.getConversationId(),conversation.getLastMessage().getBody(),conversation);

            if (this.activeConversation != null && ((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversation.getConversationId())) {
                this.activeConversation.changeUserState("" , conversation.getReceiverString());
            }
            updateAffiliations(conversation,dbHandler);
            updateLeaveButton(conversation);
            updateAuthorizations(conversation);
            updateSendingSide(conversation);
            showNotificaiton(conversation);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "Join group failed.", Toast.LENGTH_LONG).show();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (MultiUserChatException.NotAMucServiceException e) {
            e.printStackTrace();
        }
    }

    private Conversation getConversation(String conversationId) {
        Conversation conversation = null;
        DBHandler handler = null;
        if (!this.conversations.containsKey(conversationId)){
            Log.i(TAG, "Local Cache Uzerinde " + conversationId + " iliskin kayit bulunamadi DB'e bakmak lazim");
            handler = new DBHandler(UniTalkApplication.getInstance());
            conversation = handler.findConversationById(conversationId);
            this.conversations.put(conversationId,conversation);
        } else {
            conversation = this.conversations.get(conversationId);

        }
        return conversation;
    }

    @Override
    public void processMessage(Message message){
        String messageFrom = message.getFrom().toString();
        String messageBody = message.getBody();
        String sender = CommonMethods.getNumber(CommonMethods.getSecondBareJID(messageFrom));
        String conversationId = CommonMethods.getBareJID(messageFrom);
        if (!sender.equals(CommonMethods.getNumber(message.getTo().toString())) && messageBody != null && !messageBody.trim().isEmpty()) {
            messageBody = message.getBody().toString().trim();
            ChatMessage chatMessage = new ChatMessage(conversationId,messageBody,false,sender);
            chatMessage.setMessageId(message.getStanzaId());
            onReceive(conversationId, chatMessage);
            sendNotificationToSender(CommonMethods.getSecondBareJID(messageFrom),message.getStanzaId(),MessageEvent.DELIVERED);
        } else if (!message.getExtensions().isEmpty() && !sender.equals(CommonMethods.getNumber(message.getTo().asBareJid().toString()))) {
            userStatus.put(sender, message.getExtensions().get(0).getElementName());
            if(conversations.get(conversationId) == null){
                Conversation conversation = getConversation(conversationId);
                conversation.setUserStatus(userStatus);
                conversations.put(conversationId,conversation);
                changeUserState(conversationId , null);
            }else{
                conversations.get(conversationId).setUserStatus(userStatus);
                changeUserState(conversationId , null);
            }
        }
    }

    public void sendUserStatus(ChatState chatState) {
        try {
            Message userStatusMessage = new Message();
            userStatusMessage.setBody(null);
            userStatusMessage.setType(Message.Type.groupchat);
            userStatusMessage.setTo(JidCreate.entityBareFrom(((GroupChatConversation) (this.activeConversation)).getConversationId()));
            userStatusMessage.setFrom(JidCreate.entityBareFrom(userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT));
            ChatStateExtension extension = new ChatStateExtension(chatState);
            userStatusMessage.addExtension(extension);
            connection.sendStanza(userStatusMessage);
        } catch (SmackException.NotConnectedException | NullPointerException e) {
            //ignore
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void changeUserState(String conversationId , String memberNumber) {
        String statusText = "" , userName = null;
        try {
            if(!conversationId.isEmpty() && conversations.get(conversationId) != null && conversations.get(conversationId).getUserStatus() != null){
                for (String userNumber :conversations.get(conversationId).getUserStatus().keySet()) {
                    if (userStatus.get(userNumber).equals("composing") && statusText.length() <= 18){
                        userName = CommonMethods.getNameFromPhoneNumber(UniTalkApplication.getInstance(), userNumber);
                        statusText += (userName==null ? userNumber : userName) + ",";
                    }
                }
                if(!statusText.isEmpty())
                    statusText = statusText.substring(0, statusText.length()-1);

            }

            if (memberNumber == null && this.activeConversation != null && ((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversationId)) {
                this.activeConversation.changeUserState(statusText.isEmpty() ? statusText : "Typing : " + statusText , null);
            }

            if(conversationId.isEmpty() && memberNumber != null){
                if (this.activeConversation != null && ((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversationId)) {
                    this.activeConversation.changeUserState("", memberNumber);
                }
            }

            if (this.conversationListener != null && !statusText.isEmpty())
                this.conversationListener.update(conversationId,"Typing: " + statusText,null);

            else if(this.conversationListener != null && statusText.isEmpty()){
                Conversation conversation = new DBHandler(UniTalkApplication.getInstance()).findConversationById(conversationId);
                Log.e(TAG, "changeUserState: " + conversation.getLastMessage().getBody());
                this.conversationListener.update(conversationId,conversation.getLastMessage().getBody(),null);
            }
        }catch (NullPointerException npe){
            npe.printStackTrace();
        }
    }

    private  String checkGroupName(String groupName){
        String buffer = groupName.replaceAll("_" ," ");
        return  buffer;
    }

    @Override
    public void deliveredNotification(Jid from, String packetID) {
        Log.e(TAG, "delivered from: " + from);
        Log.e(TAG, "delivered packetId: " + packetID);
        onDelivered(CommonMethods.getNumber(from.asBareJid().toString()), packetID);
    }

    @Override
    public void displayedNotification(Jid from, String packetID) {
        Log.e(TAG, "displayed from: " + from);
        Log.e(TAG, "displayed packetId: " + packetID);
    }

    @Override
    public void composingNotification(Jid from, String packetID) {
        Log.e(TAG, "composing from: " + from);
        Log.e(TAG, "composing packetId: " + packetID);
    }

    @Override
    public void offlineNotification(Jid from, String packetID) {

    }

    @Override
    public void cancelledNotification(Jid from, String packetID) {
        Log.e(TAG, "cancelled from: " + from);
        Log.e(TAG, "cancelled packetId: " + packetID);
    }

    public void sendNotificationToSender(String senderJid , String messageId, String messageEvent){
        try {
            if(messageEvent.equals(MessageEvent.DELIVERED))
                eventManager.sendDeliveredNotification(JidCreate.entityBareFrom(senderJid), messageId);
            else if(messageEvent.equals(MessageEvent.DISPLAYED))
                eventManager.sendDisplayedNotification(JidCreate.entityBareFrom(senderJid), messageId);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentDateWithTime(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String time = dateFormat.format(date);
        Log.e(TAG, "onSend Date : " + time);
        return time;
    }

    @Override
    public void joined(EntityFullJid participant) {
        Log.e(TAG, "join: " + participant.toString());
        String conversationId = CommonMethods.getBareJID(participant.toString());
        Log.e(TAG, "join: " + conversationId );
        String joinUser = CommonMethods.getSecondBareJID(participant.toString());
        Log.e(TAG, "join: " + joinUser);

    }

    @Override
    public void left(EntityFullJid participant) {
        Log.e(TAG, "left: " + participant.toString());
        String conversationId = CommonMethods.getBareJID(participant.asEntityBareJidString());
        Log.e(TAG, "left: " + conversationId );
        String leavingUser = CommonMethods.getSecondBareJID(participant.asEntityBareJidString());
        Log.e(TAG, "left: " + leavingUser);
    }

    @Override
    public void kicked(EntityFullJid participant, Jid actor, String reason) {
        Log.e(TAG, "kicked: participant :" + participant.toString());
        Log.e(TAG, "kicked: reason :" + reason );
    }

    public void kickAffiliation(String conversationId,Contact contact) throws  NullPointerException{
        String memberString = null, ownerString = null;
        try {
            Log.e(TAG, "kickAffiliation: " + "is called" );
            DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
            Conversation conversation = dbHandler.findConversationById(conversationId);

            memberString = conversation.getReceiverString();
            ownerString = conversation.getOwners();
            memberString = memberString.replace(contact.getNumber()+",","");
            ownerString = ownerString.replace(contact.getNumber()+",","");

            updateRoomInfoLeafNode(CommonMethods.getNumber(conversationId),memberString,ownerString);

            conversation.setReceiverString(memberString);
            conversation.setOwners(ownerString);
            dbHandler.updateConversationRoomInfo(conversation);

            getMUC(CommonMethods.getNumber(conversationId)).kickParticipant(Resourcepart.from(contact.getNumber() + UniTalkConfig.SERVER.HOST_START_WITH_AT), "no reason");

            if (this.activeConversation != null && ((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversation.getConversationId())) {
                this.activeConversation.changeUserState("" , conversation.getReceiverString());
            }
            showNotificaiton(conversation);
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            updateRoomInfoLeafNode(CommonMethods.getNumber(conversationId),memberString,ownerString);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

    }

    public void leaveRoom(String conversationId , List<Contact> memberList) throws NullPointerException{
        try {
            Log.e(TAG, "leaveRoom : unsubscribe proccess is called");
            Log.e(TAG, "leaveRoom :" + "is called" );
            Log.e(TAG, "leaveRoom : conversationId" + conversationId);

            DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
            Conversation conversation = dbHandler.findConversationById(conversationId);

            if(conversation.getReceiverString().equals(userNumber+",") && conversation.getOwners().equals(userNumber+",")){
                getMUC(CommonMethods.getNumber(conversationId)).destroy("no reason", JidCreate.entityBareFrom(conversationId));
                deleteRoomInfoLeafeNode(CommonMethods.getNumber(conversationId));
            }else if(conversation.getOwners().equals(userNumber+",")){
                for (Contact contact :memberList){
                    if(!contact.getName().equals("Siz")){
                        grantOwnership(conversationId,contact);
                        break;
                    }
                }
            }

            getMUC(CommonMethods.getNumber(conversationId)).removeMessageListener(this);
            getMUC(CommonMethods.getNumber(conversationId)).removeParticipantStatusListener(this);

            Presence presence = new Presence(Presence.Type.unavailable);
            presence.setTo(JidCreate.domainFullFrom(conversationId +"/"+ this.userNumber + UniTalkConfig.SERVER.HOST_START_WITH_AT));
            connection.sendStanza(presence);

            conversation.setJoinable(0);
            dbHandler.updateConversationRoomInfo(conversation);

            unsubscribeAndUpdateRoomInfoLeafNode(CommonMethods.getNumber(conversationId));
            updateLeaveButton(conversation);
            updateAuthorizations(conversation);
            updateSendingSide(conversation);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        }
    }

    public void createAndPublishRoomInfo(String conversationId, String members, String owners, String admins){
        try {
            conversationId = conversationId + "-" + "new";
            Log.e(TAG, "publishRoomInfo: " + "publishing preliminaries...");
            ConfigureForm form = new ConfigureForm(DataForm.Type.submit);
            form.setAccessModel(AccessModel.open);
            form.setDeliverPayloads(true);
            form.setNotifyRetract(false);
            form.setPersistentItems(true);
            form.setPresenceBasedDelivery(false);
            form.setPublishModel(PublishModel.open);
            LeafNode node = pubSubManager.createNode(conversationId);
            node.sendConfigurationForm(form);
            node.subscribe(userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT);
            node.addItemEventListener(this);

            if(members !=null){
                SimplePayload payload1 = new SimplePayload("members", "members", "<x xmlns='members'>"+members+"</x>");
                node.send(new PayloadItem("members",payload1));
            }

            if(owners != null){
                SimplePayload payload2 = new SimplePayload("owners",  "owners", "<x xmlns='owners'>"+owners+"</x>");
                node.send(new PayloadItem("owners",payload2));
            }

            if(owners != null){
                SimplePayload payload3 = new SimplePayload("admins", "admins", "<x xmlns='admins'>"+admins+"</x>");
                node.send(new PayloadItem("admins",payload3));
            }

        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handlePublishedItems(ItemPublishEvent items) {
        try {
            Log.e(TAG, "handlePublishedItems: " +items.toString());
            Log.e(TAG, "handlePublishedItems: " +items.getItems());
            Log.e(TAG, "handlePublishedItems: " +items.getNodeId());
            String memberString = null , ownerString = null;

            if(items.getNodeId() != null){
                String conversationId = items.getNodeId().replace("-new","") + "@conference.unitalk.esbintra";
                Log.e(TAG, "handlePublishedItems: id :" + conversationId );
                Map<String ,String> map = getRoomInfoFromLeafNode(items.getNodeId() , false);
                DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
                Conversation conversation = dbHandler.findConversationById(conversationId);
                if(map.get("members") != null && map.get("owners") != null && !map.get("members").isEmpty() && !map.get("owners").isEmpty()){
                    conversation.setReceiverString(map.get("members"));
                    conversation.setOwners(map.get("owners"));
                    Log.e(TAG, "handlePublishedItems: id :" + conversation.getReceiverString());
                    conversations.put(conversation.getConversationId() , conversation);
                    dbHandler.updateConversationRoomInfo(conversation);

                    if (this.activeConversation != null && ((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversationId)) {
                        this.activeConversation.changeUserState("",conversation.getReceiverString());
                    }
                    updateAffiliations(conversation,dbHandler);
                    updateAuthorizations(conversation);
                }
            }
        }catch (NullPointerException ne ){
            ne.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }
    }

    public Map<String,String> getRoomInfoFromLeafNode(String nodeId ,boolean doSubscribe) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        Map<String,String> map = null;
        map = new HashMap<>();

            if(!nodeId.contains("new"))
                nodeId = nodeId + "-" + "new";

            LeafNode node = pubSubManager.getNode(nodeId);
            if (doSubscribe){
                node.subscribe(userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT);
                node.addItemEventListener(this);
            }
            List<Item> items = node.getItems();
            for (Item item : items){
                if(item.getId().equals("members")){
                    map.put("members",CommonMethods.readDataFromXml(item.toXML(),"x"));
                    Log.e(TAG, "getRoomInfoFromLeafNode: members :" + map.get("members") );
                }
                if(item.getId().equals("owners")){
                    map.put("owners",CommonMethods.readDataFromXml(item.toXML(),"x"));
                    Log.e(TAG, "getRoomInfoFromLeafNode: owners :" + map.get("owners") );
                }
            }
        return map;
    }

    public void unsubscribeAndUpdateRoomInfoLeafNode(String nodeId){
        try {
            nodeId = nodeId + "-" + "new";
            LeafNode node = pubSubManager.getNode(nodeId);
            Map<String,String> map = getRoomInfoFromLeafNode(nodeId ,false);
            if(map.get("members")!=null && !map.get("members").isEmpty() && map.get("members").contains(userNumber)){
                String mem = map.get("members");
                mem = mem.replace(userNumber+",","");
                Log.e(TAG, "unsubscribeAndUpdateRoomInfoLeafNode: " + mem );
                SimplePayload payload1 = new SimplePayload("members", "members", "<x xmlns='members'>"+mem+"</x>");
                node.send(new PayloadItem("members",payload1));
            }

            if(map.get("owners")!=null && !map.get("owners").isEmpty() && map.get("owners").contains(userNumber)){
                String own = map.get("owners");
                own = own.replace(userNumber+",","");
                Log.e(TAG, "unsubscribeAndUpdateRoomInfoLeafNode: " + own );
                SimplePayload payload2 = new SimplePayload("owners",  "owners", "<x xmlns='owners'>"+own+"</x>");
                node.send(new PayloadItem("owners",payload2));
            }
            node.removeItemEventListener(this);
            node.unsubscribe(userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT);

        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }catch (NullPointerException ee){
            ee.printStackTrace();
        } catch (InterruptedException e) {
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

    public void updateRoomInfoLeafNode(String nodeId , String members , String owners){
        try {
            nodeId = nodeId + "-" + "new";
            LeafNode node = pubSubManager.getNode(nodeId);
            Log.e(TAG, "updateRoomInfoLeafNode: " + members);
            Log.e(TAG, "updateRoomInfoLeafNode: " + owners);

            SimplePayload payload1 = null;
            if (members != null){
                payload1 = new SimplePayload("members", "members", "<x xmlns='members'>" + members + "</x>");
               node.send(new PayloadItem("members", payload1));
            }
            SimplePayload payload2 = null;
            if(owners != null){
                payload2 = new SimplePayload("owners", "owners", "<x xmlns='owners'>"+owners+"</x>");
                node.send(new PayloadItem("owners",payload2));
            }

        } catch (SmackException.NoResponseException e1) {
            e1.printStackTrace();
        } catch (SmackException.NotConnectedException e1) {
            e1.printStackTrace();
        } catch (XMPPException.XMPPErrorException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processStanza(Stanza packet) throws SmackException.NotConnectedException {
        // kicked olduğunda status 307 olan bir presence yakalayacak.
        if(packet.toXML().toString().contains("status code='307'") && CommonMethods.getSecondBareJID(packet.getFrom().toString()).equals(userNumber+ UniTalkConfig.SERVER.HOST_START_WITH_AT)){
            try { Log.e(TAG, "processStanza: " + "koşul sağlandı." );
                String conversationId = CommonMethods.getBareJID(packet.getFrom().asBareJid().toString());
                Map<String,String> map = null;
                map = getRoomInfoFromLeafNode(CommonMethods.getNumber(conversationId),false);
                unsubscribeAndUpdateRoomInfoLeafNode(CommonMethods.getNumber(conversationId));
                DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
                Conversation conversation = dbHandler.findConversationById(conversationId);
                conversation.setJoinable(0);
                conversation.setOwners(map.get("owners"));
                conversation.setReceiverString(map.get("members"));
                conversation.setLastMessage(new ChatMessage("You left this group..."));
                updateAffiliations(conversation,dbHandler);
                updateAuthorizations(conversation);
                updateLeaveButton(conversation);
                updateSendingSide(conversation);
                if(this.conversationListener != null){
                    this.conversationListener.update(conversationId,conversation.getLastMessage().getBody(),null);
                }
                dbHandler.updateConversationRoomInfo(conversation);
                showNotificaiton(conversation);
                Log.e(TAG, "processStanza: " + "joinable false yapıldı");
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        }
    }

    public void grantOwnership(String conversationId, Contact contact) throws NullPointerException{
        try {
            getMUC(CommonMethods.getNumber(conversationId)).grantOwnership(JidCreate.entityBareFrom(contact.getNumber() + UniTalkConfig.SERVER.HOST_START_WITH_AT));
            DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
            Conversation conversation = dbHandler.findConversationById(conversationId);
            conversation.setOwners(conversation.getOwners() + contact.getNumber() + ",");
            dbHandler.updateConversationRoomInfo(conversation);
            updateRoomInfoLeafNode(CommonMethods.getNumber(conversationId), null, conversation.getOwners());
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void deleteRoomInfoLeafeNode(String nodeId) {
        if (!nodeId.contains("new"))
            nodeId = nodeId + "-" + "new";
        try {
            LeafNode node = pubSubManager.getNode(nodeId);
            node.deleteAllItems();
            pubSubManager.deleteNode(nodeId);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void destroyRoom(String conversationId){
        try {
            getMUC(CommonMethods.getNumber(conversationId)).destroy("no reason", JidCreate.entityBareFrom(conversationId));
            deleteRoomInfoLeafeNode(CommonMethods.getNumber(conversationId));
            deleteConversationAndChatMessages(conversationId);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    public void setRoomInfoListener(IRoomInfoListener roomInfoListener) {
        this.roomInfoListener = roomInfoListener;
    }

    public void setGroupConversationListener(IGroupConversationListener groupConversationListener) {
        this.groupConversationListener = groupConversationListener;
    }

    @Override
    public void updateAffiliations(Conversation conversation, DBHandler dbHandler) {
        if(this.roomInfoListener != null && this.roomInfoListener.getConversationId().equals(conversation.getConversationId())){
            Log.e(TAG, "updateAuthorizations: " + conversation.getOwners());
            Log.e(TAG, "updateAuthorizations: " + conversation.getReceiverString());
            this.roomInfoListener.updateAffiliations(conversation, dbHandler);
        }
    }

    public void updateAuthorizations(Conversation conversation) {
        if(this.roomInfoListener != null && this.roomInfoListener.getConversationId().equals(conversation.getConversationId())){
            Log.e(TAG, "updateAuthorizations: " + conversation.getOwners());
            Log.e(TAG, "updateAuthorizations: " + conversation.getReceiverString());
            this.roomInfoListener.updateAuthorizations(conversation);
        }
    }

    @Override
    public void updateLeaveButton(Conversation conversation) {
        if(this.roomInfoListener != null && this.roomInfoListener.getConversationId().equals(conversation.getConversationId())){
            Log.e(TAG, "updateLeaveButton: " + conversation.isJoinable() );
            this.roomInfoListener.updateLeaveButton(conversation);
        }
    }

    @Override
    public String getConversationId() {
        return null;
    }

    @Override
    public void updateSendingSide(Conversation conversation) {
        if(this.groupConversationListener != null && this.groupConversationListener.getConversationId().equals(conversation.getConversationId())){
            Log.e(TAG, "updateSendingSide: " + conversation.isJoinable());
            this.groupConversationListener.updateSendingSide(conversation);
        }
    }

    public void showNotificaiton(Conversation conversation){

        if(this.activeConversation == null){
            Log.e(TAG, "showNotificaiton: "  + conversation.getConversationId());
            UniTalkNotificationManager.getInstance().showNotification(conversation.getConversationId() + "/" +conversation.getRoomName(), conversation.getLastMessage().getBody());
        }else if(this.activeConversation != null && !((GroupChatConversation)(this.activeConversation)).getConversationId().equals(conversation.getConversationId())){
            Log.e(TAG, "showNotificaiton: "  + ((GroupChatConversation)(this.activeConversation)).getConversationId());
            Log.e(TAG, "showNotificaiton: "  + conversation.getConversationId());
            UniTalkNotificationManager.getInstance().showNotification(conversation.getConversationId() + "/" +conversation.getRoomName(), conversation.getLastMessage().getBody());
        }
    }
}