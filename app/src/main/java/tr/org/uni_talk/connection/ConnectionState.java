package tr.org.uni_talk.connection;

public enum ConnectionState {

    NO_NETWORK(-1),
    DISCONNECTED(0),
    CONNECTED(1),
    AUTHORIZED(2);

    private int connectionStateValue;

    ConnectionState(int value) {
        this.connectionStateValue = value;
    }

    int state() {
        return connectionStateValue;
    }

}
