package tr.org.uni_talk.pojo;

import android.util.Log;
import android.webkit.MimeTypeMap;

import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Random;

import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.util.DateUtils;

public class ChatMessage implements IPojo {

    private int id;
    private String messageId;
    private String conversationID;
    private String body;
    private String date;
    private String time;
    private String sender;
    private int delivered;
    private boolean mine;

    public ChatMessage(){}

    public ChatMessage(String body) {
        this(null, body, false);
    }

    public ChatMessage(String conversationID, String messageBody) {
        this(conversationID, messageBody, false);
    }

    public ChatMessage(String conversationID, String messageBody, boolean isMINE) {
        setMessageId();
        setConversationID(conversationID);
        setBody(messageBody);
        setIsMine(isMINE);
        setDate(DateUtils.getCurrentDate());
        setTime(DateUtils.getCurrentTime());
        setDelivered(MESSAGE_SENDING);
    }

    public ChatMessage(String conversationID, String messageBody , String sender) {
        this(conversationID, messageBody, false ,sender);
    }

    public ChatMessage(String conversationID, String messageBody, boolean isMINE, String sender) {
        setConversationID(conversationID);
        setBody(messageBody);
        setIsMine(isMINE);
        setSender(sender);
        setDate(DateUtils.getCurrentDate());
        setTime(DateUtils.getCurrentTime());
        setDelivered(MESSAGE_SENDING);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setMessageId() {
        this.messageId = String.format(UniTalkConfig.INTERNAL.MSG_ID_FORMAT,
                                            new Random().nextInt(1000), System.currentTimeMillis());
    }

    public void setMessageIdForImage(String mimeType) {
        this.messageId = String.format(UniTalkConfig.INTERNAL.IMG_ID_FORMAT,
                new Random().nextInt(1000), System.currentTimeMillis(), MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType));
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public String getConversationID() {

        return conversationID;
    }

    public void setConversationID(String conversationID) {
        this.conversationID = conversationID;
    }


    public void setBody(String aBody) {
        this.body = aBody;
    }

    public String getBody() {
        return this.body;
    }

    public void setDate(String aDate) {
        this.date = aDate;
    }

    public String getDate() {
        return this.date;
    }

    public void setTime(String aTime) {
        this.time = aTime;
    }

    public String getTime() {
        return this.time;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public boolean isDelivered() {
        return delivered == MESSAGE_DELIVERED;
    }

    public void setDelivered(int delivered) {
        this.delivered = delivered;
    }

    public void setIsMine(boolean incoming) {
        this.mine = incoming;
    }

    public boolean isMine() {
        return this.mine;
    }


    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof ChatMessage) {
            ChatMessage that = (ChatMessage) other;
            result = (this.getMessageId().equals(that.getMessageId()));
        } else if (other instanceof String) {
            result = other.equals(this.getMessageId());
        }
        return result;
    }

    public Message convertToSmackMessage(String to) throws XmppStringprepException {
        Message message = new Message();

        Log.e(this.getClass().getName(), "convertToSmackMessage: " + to);

        message.setBody(this.getBody());
        message.setType(Message.Type.chat);
        message.setTo(JidCreate.entityBareFrom(to + UniTalkConfig.SERVER.HOST_START_WITH_AT));
        message.setStanzaId(this.getMessageId());

        return message;
    }

    public Message convertToSmackMessage(String to ,String from) {
        Message message = new Message();

        message.setBody(this.getBody());
        message.setType(Message.Type.groupchat);
        message.setTo(to);
        message.setFrom(from);
        message.setStanzaId(this.getMessageId());

        return message;
    }

    public static int MESSAGE_SENDING = 0;
    public static int MESSAGE_SENT = 1;
    public static int MESSAGE_DELIVERED = 2;

    public boolean isSent() {
        return delivered == MESSAGE_SENT;
    }
}