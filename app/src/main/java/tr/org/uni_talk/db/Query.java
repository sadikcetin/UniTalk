package tr.org.uni_talk.db;

import android.support.design.widget.TabLayout;

public interface Query {


    interface SELECT{
        public static String CONTACTS="SELECT id,name,number,status FROM " + Table.NAMES.CONTACTS;
        public static String CONVERSATIONS = "SELECT * FROM " + Table.NAMES.CONVERSATIONS;
        public static String CHAT_MESSAGES = "SELECT * FROM " + Table.NAMES.CHATMESSAGES;
        public static String OFFLINE_CHAT_MESSAGES = "SELECT * FROM " + Table.NAMES.OFFLINECHATMESSAGES;
        public static String HAS_CONVERSATION = "SELECT conversation_id FROM " + Table.NAMES.CONVERSATIONS;
        public static String GET_CONV_ID = "SELECT conversation_id FROM " + Table.NAMES.CONVERSATIONS;

    }

    interface CREATE_TABLE{
        public static String CONTACTS =
                "CREATE Table IF NOT EXISTS " + Table.NAMES.CONTACTS  +
                         "("+ Table.CONTACTS.ID +" integer primary key AUTOINCREMENT, " +
                          Table.CONTACTS.JID + " text," +
                          Table.CONTACTS.NAME + " text," +
                          Table.CONTACTS.NUMBER + " text, " +
                          Table.CONTACTS.STATUS + " text," +
                          Table.CONTACTS.IS_UT_USER + " integer)";

        public static String CHAT_MESSAGE =
                "CREATE Table IF NOT EXISTS " + Table.NAMES.CHATMESSAGES  +
                        "("+ Table.CHAT_MESSAGE.ID +" integer primary key AUTOINCREMENT, " +
                          Table.CHAT_MESSAGE.MESSAGE_ID + " text," +
                          Table.CHAT_MESSAGE.CONVERSATION_ID + " text," +
                          Table.CHAT_MESSAGE.FK_ID + " integer," +
                          Table.CHAT_MESSAGE.BODY + " text," +
                          Table.CHAT_MESSAGE.DATE + " text, " +
                          Table.CHAT_MESSAGE.TIME + " text," +
                          Table.CHAT_MESSAGE.IS_DELIVERED + " integer," +
                          Table.CHAT_MESSAGE.IS_MINE + " integer,"+
                          Table.CHAT_MESSAGE.SENDER + " text,"+
                          Table.CHAT_MESSAGE.DELIVERED_TO + " text,"+
                          "FOREIGN KEY(" +Table.CHAT_MESSAGE.FK_ID+") REFERENCES "
                           + Table.NAMES.CONVERSATIONS+"("+Table.CONVERSATION.ID+"))";

        public static String CONVERSATION =
                "CREATE Table IF NOT EXISTS " + Table.NAMES.CONVERSATIONS  +
                        "("+ Table.CONVERSATION.ID +" integer primary key AUTOINCREMENT, " +
                        Table.CONVERSATION.CONVERSATION_ID + " text," +
                        Table.CONVERSATION.RECEIVER + " text," +
                        Table.CONVERSATION.DATE + " text, " +
                        Table.CONVERSATION.LAST_MESSAGE + " text, " +
                        Table.CONVERSATION.ROOM_NAME + " text, "+
                        Table.CONVERSATION.OWNERS + " text, "+
                        Table.CONVERSATION.IS_JOINABLE + " integer)";

        public static String OFFLINE_CHAT_MESSAGE =
                "CREATE Table IF NOT EXISTS " + Table.NAMES.OFFLINECHATMESSAGES +
                        "(" + Table.OFFLINE_CHAT_MESSAGE.ID + " integer primary key AUTOINCREMENT, " +
                        Table.OFFLINE_CHAT_MESSAGE.MESSAGE_ID + " text," +
                        Table.OFFLINE_CHAT_MESSAGE.CONVERSATION_ID + " text," +
                        Table.OFFLINE_CHAT_MESSAGE.FK_ID + " integer," +
                        Table.OFFLINE_CHAT_MESSAGE.BODY + " text," +
                        Table.OFFLINE_CHAT_MESSAGE.DATE + " text, " +
                        Table.OFFLINE_CHAT_MESSAGE.TIME + " text," +
                        Table.OFFLINE_CHAT_MESSAGE.IS_DELIVERED + " integer," +
                        Table.OFFLINE_CHAT_MESSAGE.IS_MINE + " integer," +
                        Table.OFFLINE_CHAT_MESSAGE.SENDER + " text," +
                        "FOREIGN KEY(" + Table.CHAT_MESSAGE.FK_ID + ") REFERENCES "
                        + Table.NAMES.CONVERSATIONS + "(" + Table.CONVERSATION.ID + "))";
    }
}