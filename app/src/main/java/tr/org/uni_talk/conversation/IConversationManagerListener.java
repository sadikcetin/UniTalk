package tr.org.uni_talk.conversation;

import java.io.Serializable;

import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Conversation;

public interface IConversationManagerListener extends Serializable {

    void update(String conversationId, String printingText,Conversation newConversation);

}
