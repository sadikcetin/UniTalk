package tr.org.uni_talk.muc;

import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.pojo.Conversation;

public interface IRoomInfoListener {
    public void updateAffiliations(Conversation conversation , DBHandler dbHandler);
    public void updateAuthorizations(Conversation conversation);
    public void updateLeaveButton(Conversation conversation);
    public String getConversationId();
}
