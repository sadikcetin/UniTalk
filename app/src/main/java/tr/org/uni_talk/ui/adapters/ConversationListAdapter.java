package tr.org.uni_talk.ui.adapters;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.util.CommonMethods;

public class ConversationListAdapter extends AbstractBaseAdapter<Conversation> {

    String TAG = this.getClass().getName();
    private TextView textViewMessage = null;

    public ConversationListAdapter(Activity context) {
        this(context, new ArrayList<Conversation>());
    }

    public ConversationListAdapter(Activity context, List<Conversation> list) {
        super(context, list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Conversation conversation = (Conversation) getItem(position);
        if (convertView == null)
            convertView = getInflater().inflate(R.layout.conversation_item, null);

        if (conversation != null) {

            TextView textViewTime = (TextView) convertView.findViewById(R.id.textViewTime);
            String time = new DBHandler(UniTalkApplication.getInstance().getApplicationContext()).getLastMessageTime(conversation.getConversationId());
            textViewTime.setText(conversation.getLastMessage()== null ? conversation.getTime() : time);

            TextView textViewUser = (TextView) convertView.findViewById(R.id.textViewUser);
            ImageView viewIcon = (ImageView) convertView.findViewById(R.id.viewIcon);
            viewIcon.setBackgroundResource(R.drawable.user_icon);

            Contact contact = UniTalkAccountManager.getInstance().findContactByUserName(conversation.getConversationId());
            if (contact != null)
                textViewUser.setText(contact.getName());
            else {
                if (conversation.getConversationId().contains("conference")) {
                    textViewUser.setText(conversation.getRoomName());
                    viewIcon.setBackgroundResource(R.drawable.group_icon);
                }
                else {
                    textViewUser.setText(CommonMethods.getNameFromPhoneNumber(UniTalkApplication.getInstance(), conversation.getConversationId()));

                }
            }

            textViewMessage = (TextView) convertView.findViewById(R.id.textViewMessage);

            if (conversation.getLastMessageString().length() > 25) {
                textViewMessage.setText(conversation.getLastMessageString().substring(0, 25) + " ...");
            } else {
                textViewMessage.setText(conversation.getLastMessageString());
            }
        }
        return convertView;
    }

    public Conversation findByConversationId(String conversationId) {
        Conversation con = null;
        for (int i = 0; i < getCount(); i++) {
            con = (Conversation) getItem(i);
            if (con.getConversationId().equals(conversationId)){
                removeItem(i);
                addItemFirstIndex(con);
                return con;
            }
        }
        con = null;
        return con;
    }

    public void uptadeConversationText(String conversationId , String printingText) {
        Conversation con = findByConversationId(conversationId);
        if (con != null) {
            con.setLastMessage(new ChatMessage(printingText));
        }
    }
}