package tr.org.uni_talk.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;

public class DBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    public DBHandler(Context context) {
        super(context, UniTalkConfig.INTERNAL.DB_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Query.CREATE_TABLE.CONTACTS);
        db.execSQL(Query.CREATE_TABLE.CONVERSATION);
        db.execSQL(Query.CREATE_TABLE.CHAT_MESSAGE);
        db.execSQL(Query.CREATE_TABLE.OFFLINE_CHAT_MESSAGE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP Table IF EXISTS " + Table.NAMES.CONTACTS);
        db.execSQL("DROP Table IF EXISTS " + Table.NAMES.CONVERSATIONS);
        db.execSQL("DROP Table IF EXISTS " + Table.NAMES.CHATMESSAGES);
        db.execSQL("DROP Table IF EXISTS " + Table.NAMES.OFFLINECHATMESSAGES);
        onCreate(db);
    }

    public void insertContact(Contact contact){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Table.CONTACTS.JID,contact.getId());
        values.put(Table.CONTACTS.NAME,contact.getName());
        values.put(Table.CONTACTS.NUMBER, contact.getNumber());
        values.put(Table.CONTACTS.STATUS, contact.getStatus());
        //isUTuser eklenecek

        db.insert(Table.NAMES.CONTACTS, null, values);
        db.close();
    }

    public List<Contact> getAllContacts(){

        List<Contact> contacts = new ArrayList<Contact>();
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.CONTACTS;
        Cursor cursor = db.rawQuery(sqlQuery, null);

        while (cursor.moveToNext()) {

            Contact contact = new Contact();

            contact.setId(cursor.getInt(0));
            contact.setName(cursor.getString(1));
            contact.setNumber(cursor.getString(2));
            contact.setStatus(cursor.getString(3));
            contacts.add(contact);
        }

        cursor.close();
        db.close();
        return  contacts;
    }

    public Contact getContactByNumber(String number){
        Contact contact = null;
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.CONTACTS + " WHERE number=\'" + number + "\'";
        Cursor cursor = db.rawQuery(sqlQuery, null);

        while (cursor.moveToNext()) {

            contact = new Contact();

            contact.setId(cursor.getInt(0));
            contact.setName(cursor.getString(1));
            contact.setNumber(cursor.getString(2));
            contact.setStatus(cursor.getString(3));
        }

        cursor.close();
        db.close();
        return  contact;
    }

    public void insertChatMessage(ChatMessage message, boolean isOnline) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(Table.CHAT_MESSAGE.MESSAGE_ID,message.getMessageId());
        values.put(Table.CHAT_MESSAGE.CONVERSATION_ID,message.getConversationID());
        values.put(Table.CHAT_MESSAGE.BODY,message.getBody());
        values.put(Table.CHAT_MESSAGE.DATE,message.getDate());
        values.put(Table.CHAT_MESSAGE.TIME,message.getTime());
        values.put(Table.CHAT_MESSAGE.IS_DELIVERED,message.isDelivered());
        values.put(Table.CHAT_MESSAGE.IS_MINE,message.isMine());
        values.put(Table.CHAT_MESSAGE.SENDER,message.getSender());

        if (isOnline) {
            db.insert(Table.NAMES.CHATMESSAGES, null, values);
        } else {
            db.insert(Table.NAMES.OFFLINECHATMESSAGES, null, values);
        }
        db.close();
    }

    public int deleteChatMessageFromOffline(String messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(Table.NAMES.OFFLINECHATMESSAGES, "message_id=\'" + messageId + "\'", null);
        db.close();
        return result;
    }


    public void updateChatMessageAsDelivered(String messageId, int status) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(Table.CHAT_MESSAGE.IS_DELIVERED, status);

        int result = db.update(Table.NAMES.CHATMESSAGES,values, "message_id=\'"+ messageId + "\'", null);
        Log.i("DBHANDLER", result + " tane kayit guncellendi");
        db.close();
    }

    public String addNumberToDelivered(String messageId , String userNumber) {

        String deliveredNumbers = UniTalkAccountManager.getInstance().getAccount().getUserNumber() + ",";;

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        String sqlDelivered = Query.SELECT.CHAT_MESSAGES + " WHERE message_id=\'"+messageId+"\'";
        Cursor cursor = db.rawQuery(sqlDelivered,null);

        while (cursor.moveToNext()){
            if(cursor.getString(10) != null)
                deliveredNumbers = cursor.getString(10);
        }

        deliveredNumbers += userNumber + ",";
        values.put(Table.CHAT_MESSAGE.DELIVERED_TO, deliveredNumbers);

        int result = db.update(Table.NAMES.CHATMESSAGES,values, "message_id=\'"+ messageId + "\'", null);
        Log.i("DBHANDLER", result + " tane kayit guncellendi");

        cursor.close();
        db.close();
        return  deliveredNumbers;
    }

    public String getConversationIdByMessageId(String messageId){
        String conversationId = null;
        SQLiteDatabase db = this.getWritableDatabase();
        String sqlDelivered = Query.SELECT.CHAT_MESSAGES + " WHERE message_id=\'"+messageId+"\'";
        Cursor cursor = db.rawQuery(sqlDelivered,null);
        while (cursor.moveToNext()) {
            if(cursor.getString(2) != null)
                conversationId = cursor.getString(2);
        }
        cursor.close();
        db.close();
        return conversationId;
    }


    public void updateConversation(Conversation conversation) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(Table.CONVERSATION.LAST_MESSAGE, conversation.getLastMessageString());
        values.put(Table.CONVERSATION.DATE,conversation.getTime());

        int result = db.update(Table.NAMES.CONVERSATIONS,values, "conversation_id=\'"+conversation.getConversationId() + "\'", null);
        Log.i("DBHANDLER", result + " tane kayit guncellendi");
        db.close();
    }

    public void updateConversationRoomInfo(Conversation conversation) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(Table.CONVERSATION.IS_JOINABLE,conversation.isJoinable());
        values.put(Table.CONVERSATION.OWNERS,conversation.getOwners());
        values.put(Table.CONVERSATION.RECEIVER,conversation.getReceiverString());
        values.put(Table.CONVERSATION.LAST_MESSAGE, conversation.getLastMessageString());

        int result = db.update(Table.NAMES.CONVERSATIONS,values, "conversation_id=\'"+conversation.getConversationId() + "\'", null);
        Log.i("DBHANDLER", result + " tane kayit guncellendi");
        db.close();
    }


    public void insertConversation(Conversation conversation){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(Table.CONVERSATION.CONVERSATION_ID,conversation.getConversationId());
        values.put(Table.CONVERSATION.RECEIVER,conversation.getReceiver().getName());
        values.put(Table.CONVERSATION.DATE,conversation.getTime());
        values.put(Table.CONVERSATION.LAST_MESSAGE,conversation.getLastMessageString());

        db.insert(Table.NAMES.CONVERSATIONS, null, values);
        db.close();
    }

    public void insertMultiUserConversation(Conversation conversation){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(Table.CONVERSATION.CONVERSATION_ID,conversation.getConversationId());
        values.put(Table.CONVERSATION.DATE,conversation.getTime());
        values.put(Table.CONVERSATION.RECEIVER, conversation.getReceiverString());
        values.put(Table.CONVERSATION.LAST_MESSAGE,conversation.getLastMessageString());
        values.put(Table.CONVERSATION.ROOM_NAME,conversation.getRoomName());
        values.put(Table.CONVERSATION.OWNERS,conversation.getOwners());
        values.put(Table.CONVERSATION.IS_JOINABLE,conversation.isJoinable());

        db.insert(Table.NAMES.CONVERSATIONS, null, values);
        db.close();
    }

    public Conversation findConversationById(String conversationID) {
        Conversation conversation = null;
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.CONVERSATIONS + " WHERE conversation_id =\'" + conversationID + "\'";
        Cursor cursor = db.rawQuery(sqlQuery, null);

        while (cursor.moveToNext()) {

            conversation = new Conversation();

            conversation.setId(cursor.getInt(0));
            conversation.setConversationId(cursor.getString(1));
            conversation.setReceiverString(cursor.getString(2));
            conversation.setTime(cursor.getString(3));
            conversation.setLastMessage(new ChatMessage(cursor.getString(4)));

            if(cursor.getString(5)!=null && !cursor.getString(5).isEmpty())
            conversation.setRoomName(cursor.getString(5));

            if(cursor.getString(6)!=null && !cursor.getString(6).isEmpty())
                conversation.setOwners(cursor.getString(6));

            conversation.setJoinable(cursor.getInt(7));
        }

        cursor.close();
        db.close();
        return  conversation;
    }

 

    public List<Conversation> getConversations(){

        List<Conversation> conversations = new ArrayList<Conversation>();
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.CONVERSATIONS + " ORDER BY datetime("+Table.CONVERSATION.DATE+") DESC";
        Cursor cursor = db.rawQuery(sqlQuery, null);

        while (cursor.moveToNext()) {

            Conversation conversation = new Conversation();

            conversation.setId(cursor.getInt(0));
            conversation.setConversationId(cursor.getString(1));
            conversation.setReceiverString(cursor.getString(2));
            conversation.setTime(cursor.getString(3));
            conversation.setLastMessage(new ChatMessage(cursor.getString(4)));

            if(cursor.getString(5)!=null && !cursor.getString(5).isEmpty())
                conversation.setRoomName(cursor.getString(5));

            if(cursor.getString(6)!=null && !cursor.getString(6).isEmpty())
                conversation.setOwners(cursor.getString(6));

            conversation.setJoinable(cursor.getInt(7));

            Log.e("DB:", cursor.getString(0));
            Log.e("DB:", cursor.getString(1));
            Log.e("DB:",cursor.getString(3));
            Log.e("DB:",cursor.getString(4));

            conversations.add(conversation);
        }

        cursor.close();
        db.close();
        return  conversations;
    }

    public String getLastMessageTime(String conversationId){
        String id = getConversationIdWith(conversationId);
        List<ChatMessage> list = getMessages(id);
        if (list.size() == 0) return "";
        return list.get(list.size() - 1).getTime();
    }

    public List<ChatMessage> getMessages(String conversationId){

        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.CHAT_MESSAGES + " WHERE conversation_id=\'"+conversationId+"\' ORDER BY \'" + Table.CHAT_MESSAGE.TIME + "\'";
        Cursor cursor = db.rawQuery(sqlQuery, null);

        ChatMessage message;
        while (cursor.moveToNext()) {
            message = new ChatMessage();
            message.setMessageId(cursor.getString(1));
            message.setConversationID(cursor.getString(2));
            message.setBody(cursor.getString(4));
            message.setDate(cursor.getString(5));
            message.setTime(cursor.getString(6));
            message.setDelivered(cursor.getInt(7));

            if (cursor.getInt(8) == 1)
                message.setIsMine(true);
            else
                message.setIsMine(false);

            if (cursor.getString(9) != null)
                message.setSender(cursor.getString(9));

            messages.add(message);
        }
        cursor.close();
        db.close();
        return messages;
    }

    public List<ChatMessage> getOfflineMessages() {
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.OFFLINE_CHAT_MESSAGES + " ORDER BY \'" + Table.CHAT_MESSAGE.TIME + "\'";
        Cursor cursor = db.rawQuery(sqlQuery, null);

        ChatMessage message;
        while (cursor.moveToNext()) {
            message = new ChatMessage();
            message.setMessageId(cursor.getString(1));
            message.setConversationID(cursor.getString(2));
            message.setBody(cursor.getString(4));
            message.setDate(cursor.getString(5));
            message.setTime(cursor.getString(6));
            message.setDelivered(cursor.getInt(7));

            if (cursor.getInt(8)==1)
                message.setIsMine(true);
            else
                message.setIsMine(false);

            if(cursor.getString(9) != null)
                message.setSender(cursor.getString(9));

            messages.add(message);
        }
        cursor.close();
        db.close();
        return  messages;
    }

    public String getConversationIdWith(String receiver){

        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.GET_CONV_ID + " WHERE conversation_id=\'"+ receiver +"\'";
        Log.i("DBHANDLER", "Generated query for " + receiver + " => " + sqlQuery);
        Cursor cursor = db.rawQuery(sqlQuery, null);

        Log.i("DBHANDLER", "DB uzerinde " + receiver + " icin toplam " + cursor.getCount() + " kadar entry var");

        try {
            while (cursor.moveToNext()){
                if (receiver.equals(cursor.getString(0))){
                    return cursor.getString(0);
                }
            }
        }finally {
            cursor.close();
            db.close();
        }

        return null;
    }

    public List<Conversation> getGroupConversations() {
        List<Conversation> conversations = new ArrayList<Conversation>();
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.CONVERSATIONS + " WHERE conversation_id LIKE \'" + "%@conference%" + "\'" + " AND is_joinable='1'" + " ORDER BY datetime(" + Table.CONVERSATION.DATE + ") DESC";

        Cursor cursor = db.rawQuery(sqlQuery, null);

        while (cursor.moveToNext()) {

            Conversation conversation = new Conversation();

            conversation.setId(cursor.getInt(0));
            conversation.setConversationId(cursor.getString(1));
            conversation.setReceiverString(cursor.getString(2));
            conversation.setTime(cursor.getString(3));
            conversation.setLastMessage(new ChatMessage(cursor.getString(4)));

            if (cursor.getString(5) != null && !cursor.getString(5).isEmpty())
                conversation.setRoomName(cursor.getString(5));

            if (cursor.getString(6) != null && !cursor.getString(6).isEmpty())
                conversation.setOwners(cursor.getString(6));

            conversation.setJoinable(cursor.getInt(7));

            Log.e("DB:", cursor.getString(0));
            Log.e("DB:", cursor.getString(1));
            Log.e("DB:", cursor.getString(3));
            Log.e("DB:", cursor.getString(4));

            conversations.add(conversation);
        }

        cursor.close();
        db.close();
        return conversations;

    }


    public long getTableRowCount(String tableName){

        SQLiteDatabase db = this.getReadableDatabase();
        long count  = DatabaseUtils.queryNumEntries(db, tableName);
        db.close();

        return count;
    }


    public boolean hasConversation(String receiver){
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlQuery = Query.SELECT.HAS_CONVERSATION + " WHERE conversation_id=\'" +receiver+"\'";
        Cursor cursor = db.rawQuery(sqlQuery, null);

        if (cursor != null) {
            while (cursor.moveToNext()){
                Log.e("DBHandler", "hasConversation: " + cursor.getString(0));
                if (cursor.getString(0).toString().equals(receiver)){
                    cursor.close();
                    return true;
                }
            }
            cursor.close();
        }
        db.close();
        return false;
    }

    public void deleteChatMessages(String conversationId){
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "conversation_id=\'"+conversationId+"\'";
        int result = db.delete(Table.NAMES.CHATMESSAGES, whereClause, null);
        Log.e("DBHandler", "deleteChatMessages: " + result +"adet message silindi.");
        db.close();
    }

    public void deleteConversations(String conversationId){
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "conversation_id=\'"+conversationId+"\'";
        int result = db.delete(Table.NAMES.CONVERSATIONS, whereClause, null);
        Log.e("DBHandler", "deleteChatMessages: " + result +"adet message silindi.");
        db.close();
    }

    public int deleteAll(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(tableName, null, null);
        db.close();
        return result;
    }

}