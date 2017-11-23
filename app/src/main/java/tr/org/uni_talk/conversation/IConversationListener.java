package tr.org.uni_talk.conversation;

import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Contact;

public interface IConversationListener {

    void onReceive(String from, ChatMessage chatMessage);

    void onServerAcknowledgeReceived(String to, String messageID);

    void onDelivered(String from, String messageID);

    void onRead(String from, String messageID);

    void onSend(String to, ChatMessage chatMessage);

    void changeUserState(String userState , String member);

    Contact getContact();

}
