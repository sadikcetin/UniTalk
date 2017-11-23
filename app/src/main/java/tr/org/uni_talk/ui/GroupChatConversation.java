package tr.org.uni_talk.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.jivesoftware.smackx.chatstates.ChatState;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.conversation.IConversationListener;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.muc.IGroupConversationListener;
import tr.org.uni_talk.muc.UniTalkMUCManager;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.ui.adapters.GroupChatConversationAdapter;
import tr.org.uni_talk.ui.fragments.RoomInfoFragment;
import tr.org.uni_talk.util.CommonMethods;

public class GroupChatConversation extends AppCompatActivity implements IConversationListener , IGroupConversationListener{

    public final String TAG = this.getClass().getName();

    List<ChatMessage> chatMessageList = new ArrayList<>();
    RecyclerView recyclerView;
    GroupChatConversationAdapter mAdapter;
    String members;

    ImageButton buttonSend;
    EditText messageText;
    TextView tvHeaderText;
    TextView tvHeaderMemberText;
    TextView leaveInformation;

    String receiver;
    String roomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_conversation);

        receiver = CommonMethods.getBareJID(getIntent().getStringExtra("conversationID"));
        Log.e(TAG, "receiver conversationId :" + receiver);
        roomName = CommonMethods.getSecondBareJID(getIntent().getStringExtra("conversationID"));
        Log.e(TAG, "roomName conversationId :" + roomName);

        UniTalkMUCManager.getINSTANCE().setActiveConversation(this);
        UniTalkMUCManager.getINSTANCE().setGroupConversationListener(this);

        tvHeaderText = (TextView) findViewById(R.id.headerText);
        tvHeaderText.setText(roomName);
        Conversation conversation = new DBHandler(UniTalkApplication.getInstance().getApplicationContext()).findConversationById(receiver);

        tvHeaderMemberText = (TextView) findViewById(R.id.group_members);
        if (conversation == null)
            tvHeaderMemberText.setText("");
        else{
            members = CommonMethods.getMemberNameString(conversation.getReceiverString());
            UniTalkMUCManager.getINSTANCE().changeUserState(receiver , null);
        }

        recyclerView = (RecyclerView) findViewById(R.id.rv_group_chat);
        mAdapter = new GroupChatConversationAdapter(chatMessageList);
        mAdapter.setHasStableIds(true);

        Log.e(TAG, "onCreate: " + recyclerView.getParent());
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) layoutManager).setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);

        prepareData();

        messageText = (EditText) findViewById(R.id.messageEditText);
        buttonSend = (ImageButton) findViewById(R.id.sendMessageButton);
        leaveInformation = (TextView) findViewById(R.id.leaveInformation);

        updateSendingSide(conversation);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = messageText.getText().toString();
                if (text.trim().isEmpty()) return;
                onSend(receiver, new ChatMessage(receiver, text, true));
                chatMessageList.add(new ChatMessage(receiver, text, true));
                mAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                messageText.setText("");
            }
        });

        messageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.e(TAG, "receiver: " + receiver);
                if (!s.toString().equals("") && !s.toString().isEmpty())
                    UniTalkMUCManager.getINSTANCE().sendUserStatus(ChatState.composing);
                else if (s.toString().equals("") || !s.toString().isEmpty())
                    UniTalkMUCManager.getINSTANCE().sendUserStatus(ChatState.active);
            }
        });

        tvHeaderText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment f = getFragmentManager().findFragmentById(R.id.group_chat_container);
                if(findViewById(R.id.group_chat_container) != null && f == null){
                    Log.e(TAG, "Room info fragment yükleniyor");
                    Bundle args = new Bundle();
                    args.putString("conversationId",receiver);
                    Fragment roomInfoFragment = new RoomInfoFragment();
                    roomInfoFragment.setArguments(args);
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();

                    transaction.replace(R.id.group_chat_container, roomInfoFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            }
        });
    }

    private void prepareData() {
        DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());

        String convId = dbh.getConversationIdWith(this.receiver);

        Log.i(TAG, this.receiver + " " + convId);

        List<ChatMessage> messages = dbh.getMessages(convId);

        if (messages != null && messages.size() > 0) {
            chatMessageList.addAll(messages);
        }
        mAdapter.notifyDataSetChanged();

    }

    @Override
    protected void onResume() {
        super.onResume();
        UniTalkMUCManager.getINSTANCE().sendUserStatus(ChatState.active);
        UniTalkMUCManager.getINSTANCE().setActiveConversation(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UniTalkMUCManager.getINSTANCE().sendUserStatus(ChatState.gone);
        UniTalkMUCManager.getINSTANCE().setActiveConversation(null);
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy Called");
        UniTalkMUCManager.getINSTANCE().sendUserStatus(ChatState.gone);
        UniTalkMUCManager.getINSTANCE().setActiveConversation(null);
        UniTalkMUCManager.getINSTANCE().setGroupConversationListener(null);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.e(TAG, "onNewIntent: ");
        Bundle extras = intent.getExtras();
        Intent msgIntent = new Intent(this, GroupChatConversation.class);
        msgIntent.putExtras(extras);
        startActivity(msgIntent);
        finish();

        super.onNewIntent(intent);
    }

    @Override
    public void onReceive(String from, ChatMessage chatMessage) {
        chatMessageList.add(chatMessage);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
            }
        });
    }

    @Override
    public void onServerAcknowledgeReceived(String to, String messageID) {

    }

    @Override
    public void onDelivered(String from, final String messageID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.delivered(messageID);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onRead(String from, String messageID) {

    }

    @Override
    public void onSend(String to, ChatMessage chatMessage) {
        UniTalkMUCManager.getINSTANCE().sendMessage(to, chatMessage);
    }


    @Override
    public Contact getContact() {
        return null;
    }

    public String getConversationId(){
        return receiver;
    }

    public void onBackPressed(View view) {
        onBackPressed();
    }

    @Override
    public void changeUserState(final String userState , final String member) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!userState.isEmpty() && member == null){
                    tvHeaderMemberText.setText(userState);
                } else if(userState.isEmpty() && member != null){
                    members = CommonMethods.getMemberNameString(member);
                    tvHeaderMemberText.setText(members.length()>25 ? members.substring(0,25).toString() + "..." : members);
                }else if(userState.isEmpty() && member == null){
                    tvHeaderMemberText.setText(members.length()>25 ? members.substring(0,25).toString() + "..." : members);
                }
            }
        });
    }

    public void updateSendingSide(final Conversation conversation) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(conversation.isJoinable() == 0){
                    messageText.setVisibility(View.GONE);
                    buttonSend.setVisibility(View.GONE);
                    leaveInformation.setVisibility(View.VISIBLE);
                }else{
                    messageText.setVisibility(View.VISIBLE);
                    buttonSend.setVisibility(View.VISIBLE);
                    leaveInformation.setVisibility(View.GONE);
                }
            }
        });

    }
}