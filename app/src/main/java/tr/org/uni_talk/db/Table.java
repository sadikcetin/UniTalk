package tr.org.uni_talk.db;

public interface Table {

    interface NAMES{
        static final String CONTACTS = "contacts";
        static final String CHATMESSAGES = "chat_messages";
        static final String CONVERSATIONS ="conversations";
        static final String OFFLINECHATMESSAGES = "offline_chat_messages";
    }

    interface CONTACTS{
        public static final String ID = "id";
        public static final String JID = "jid";
        public static final String NAME = "name";
        public static final String STATUS = "status";
        public static final String NUMBER = "number";
        public static final String IS_UT_USER = "is_ut_user";
    }

    interface  CHAT_MESSAGE{
        public static final String ID = "id";
        public static final String MESSAGE_ID = "message_id";
        public static final String CONVERSATION_ID = "conversation_id";
        public static final String FK_ID = "fk_id";
        public static final String BODY = "body";
        public static final String DATE = "date";
        public static final String TIME = "time";
        public static final String IS_DELIVERED = "is_delivered";
        public static final String IS_MINE = "is_mine";
        public static final String SENDER = "sender";
        public static final String DELIVERED_TO = "delivered_to";
    }

    interface CONVERSATION{
        public static final String ID = "id";
        public static final String CONVERSATION_ID = "conversation_id";
        public static final String RECEIVER = "receiver";
        public static final String DATE = "date";
        public static final String LAST_MESSAGE = "last_message";
        public static final String ROOM_NAME = "room_name";
        public static final String OWNERS = "owners";
        public static final String IS_JOINABLE = "is_joinable";
    }

    interface OFFLINE_CHAT_MESSAGE extends CHAT_MESSAGE {

    }
}
