package tr.org.uni_talk.connection;

import org.jivesoftware.smack.XMPPConnection;

public interface OnAuthorizedListener {

    void onAuthorized(XMPPConnection connection);

}
