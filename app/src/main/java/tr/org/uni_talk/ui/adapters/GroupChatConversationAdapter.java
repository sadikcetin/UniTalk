package tr.org.uni_talk.ui.adapters;

import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.util.CommonMethods;

/**
 * Created by emre on 21.08.2017.
 */

public class GroupChatConversationAdapter extends RecyclerView.Adapter<GroupChatConversationAdapter.GroupChatConversationViewHolder> {

    private final String TAG = this.getClass().getName();
    private List<ChatMessage> chatMessageList;
    private int fontSize;

    public GroupChatConversationAdapter(List<ChatMessage> aMessageList) {
        chatMessageList = aMessageList;
        fontSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(UniTalkApplication.getInstance()).getString("font_size", "12"));
    }

    @Override
    public GroupChatConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_chat_bubble, parent, false);
        return new GroupChatConversationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(GroupChatConversationViewHolder holder, int position) {
        ChatMessage message = chatMessageList.get(position);
        holder.message.setText(message.getBody());
        holder.messageTime.setText(message.getTime());
        if (message.isMine()) {
            holder.messageSender.setVisibility(View.GONE);
            if (message.isDelivered())
                holder.messageRead.setImageResource(R.drawable.ic_done_all_24dp);
            else
                holder.messageRead.setImageResource(R.drawable.ic_done_24dp);

            holder.layout.setBackgroundResource(R.drawable.outgoing);
            holder.parentLayout.setPadding(100, 13, 0, 12);
            holder.parentLayout.setGravity(Gravity.END);
        } else {
            String name = CommonMethods.getNameFromPhoneNumber(UniTalkApplication.getInstance().getApplicationContext(), message.getSender());

            holder.messageSender.setText(name == null ? message.getSender():name);

            holder.messageRead.setVisibility(View.GONE);
            holder.layout.setBackgroundResource(R.drawable.incoming);
            holder.parentLayout.setPadding(0, 13, 100, 12);
            holder.parentLayout.setGravity(Gravity.START);

        }
    }

    @Override
    public int getItemCount() {
        return chatMessageList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public Object getItem(int position) {
        return this.chatMessageList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class GroupChatConversationViewHolder extends RecyclerView.ViewHolder {

        TextView messageSender;
        TextView message;
        ImageView messageRead;
        TextView messageTime;
        LinearLayout layout;
        LinearLayout parentLayout;

        public GroupChatConversationViewHolder(View itemView) {
            super(itemView);
            messageSender = (TextView) itemView.findViewById(R.id.message_sender);
            message = (TextView) itemView.findViewById(R.id.message_text);
            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            messageRead = (ImageView) itemView.findViewById(R.id.imageViewRead);
            messageTime = (TextView) itemView.findViewById(R.id.textViewMsgTime);

            layout = (LinearLayout) itemView.findViewById(R.id.bubble_layout);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.bubble_layout_parent);
        }
    }

    private ChatMessage findByMessageId(String msgID) {
        ChatMessage msg = null;
        for (int i = 0; i < getItemCount(); i++) {
            msg = (ChatMessage) getItem(i);
            //Log.e("HATA-3:",msg.getBody() +" **** "+ msgID);
            if (msg.equals(msgID))
                return msg;
        }

        return msg;
    }

    public void delivered(String msgIdD) {
        ChatMessage msg = findByMessageId(msgIdD);

        if (msg != null) {
            // Log.e("HATA-2:",msg.getBody());
            msg.setDelivered(ChatMessage.MESSAGE_DELIVERED);
        }
    }
}
