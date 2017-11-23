package tr.org.uni_talk.pojo;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.util.CommonMethods;

public class Conversation implements IPojo {

    private int id;
    private String conversationId;
    private Contact receiver;
    private List<Contact> members;

    private String roomName;
    private String owners;
    private int isJoinable;

    private String createdDate;

    private ChatMessage lastMessage;

    private String receiverString;
    private String time;

    private List<ChatMessage> unreadMessages;
    private List<ChatMessage> readMessages;
    private Map<String,String> userStatus = null;

    public  Conversation(){}

    public Conversation(Contact receiver) {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        time = dateFormat.format(date);
        this.setReceiver(receiver);
        this.setReceiverString(receiver.getNumber());
        setTime(time);
        setConversationId(receiver.getNumber());
    }

    public Conversation(String conversationId) {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        time = dateFormat.format(date);
        setTime(time);
        setConversationId(conversationId);
    }

    public List<ChatMessage> getUnreadMessages() {
        return unreadMessages;
    }

    private void createUnreadMessagesList() {
        if (this.unreadMessages == null)
            this.unreadMessages = new ArrayList<ChatMessage>();
    }

    public void addUnreadMessage(ChatMessage msg) {
        createUnreadMessagesList();
        this.setLastMessage(msg);
        this.unreadMessages.add(msg);

    }

    public void addReadMessage(ChatMessage message) {
        createReadMessagesList();

        this.readMessages.add(message);
        this.lastMessage = message;
    }

    public void setMessageDelivered(String msgID) {
        if (readMessages != null && readMessages.size() > 0) {
            for (ChatMessage message : this.readMessages) {
                if (message.equals(msgID)) {
                    message.setDelivered(ChatMessage.MESSAGE_DELIVERED);
                    break;
                }
            }
        }
    }

    public void setMessageAcknowledged(String msgID) {
        if (readMessages != null && readMessages.size() > 0) {
            for (ChatMessage message : this.readMessages) {
                if (message.equals(msgID)) {
                    message.setDelivered(ChatMessage.MESSAGE_SENT);
                    break;
                }
            }
        }
    }

    public void setLastMessage(ChatMessage message) {
        this.lastMessage = message;
    }

    public ChatMessage getLastMessage() {
        return this.lastMessage;
    }

    public String getLastMessageString() {
        if(this.lastMessage == null)
            return "";
        return this.lastMessage.getBody();
    }

    public void markAsRead() {
        if (this.unreadMessages != null && this.unreadMessages.size() > 0) {
            createReadMessagesList();
            for (ChatMessage msg : this.unreadMessages) {
                this.readMessages.add(msg);
            }
            this.unreadMessages.clear();
        }
    }

    public int getUnreadMessageCount() {
        if (this.unreadMessages != null)
            return this.getUnreadMessages().size();
        return 0;
    }

    public void setUnreadMessages(List<ChatMessage> unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public List<ChatMessage> getReadMessages() {
        //// TODO: 8/22/16 getMessages
        DBHandler dh = new DBHandler(UniTalkApplication.getInstance());

        return dh.getMessages(getConversationId());
    }

    private void createReadMessagesList() {
        if (null == this.readMessages)
            this.readMessages = new ArrayList<ChatMessage>();
    }

    public void setReadMessages(List<ChatMessage> readMessages) {
        this.readMessages = readMessages;
    }

    public Contact getReceiver() {
        return receiver;
    }

    public void setReceiver(Contact receiver) {
        this.receiver = receiver;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public String getReceiverString() {
        return receiverString;
    }

    public void setReceiverString(String receiverString) {
        this.receiverString = receiverString;
    }

    public void setTime(String time){
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof Conversation) {
            Conversation that = (Conversation) other;
            result = (this.getConversationId().equals(that.getConversationId()));
        } else if (other instanceof String) {
            result = other.equals(this.getConversationId());
        }
        return result;
    }

    public List<Contact> getMembers() {
        if(members == null)
            members = new ArrayList<>();
        return members;
    }

    public void addMember(Contact contact){
        getMembers().add(contact);
    }

    public String getMemberContactNumber() {
        String receivers = "";
        String number = CommonMethods.getNumber(UniTalkAccountManager.getInstance().getAccount().getUserName());
        for (Contact contact : members) {
            if (contact.getNumber().equals(number))
                receivers += "me,";
            else
                receivers += contact.getNumber() + ",";
        }
        receivers += CommonMethods.getNumber(UniTalkAccountManager.getInstance().getAccount().getUserName()) + ",";
        Log.e("Contact", "getMemberContactNumber: " + receivers );
        return receivers;
    }

    public void setMembers(List<Contact> members) {
        this.members = members;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Map<String, String> getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(Map<String, String> userStatus) {
        this.userStatus = userStatus;
    }

    public String getOwners() {
        return owners;
    }

    public void setOwners(String owners) {
        this.owners = owners;
    }

    public int isJoinable() {
        return isJoinable;
    }

    public void setJoinable(int joinable) {
        isJoinable = joinable;
    }

}
