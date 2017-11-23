package tr.org.uni_talk.muc;

import tr.org.uni_talk.pojo.Conversation;


public interface IGroupConversationListener {
    public void updateSendingSide(Conversation conversation);
    public String getConversationId();
}
