package tr.org.uni_talk.ui.fragments;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.conversation.IConversationManagerListener;
import tr.org.uni_talk.conversation.UniTalkConversationManager;
import tr.org.uni_talk.muc.UniTalkMUCManager;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.ui.GroupChatConversation;
import tr.org.uni_talk.ui.SingleConversation;
import tr.org.uni_talk.ui.adapters.ConversationListAdapter;
import tr.org.uni_talk.util.CommonMethods;

public class ConversationListFragment extends AbstractListFragment implements IConversationManagerListener {

    private static final String TAG = ConversationListFragment.class.getName();

    private ConversationListAdapter adapter;

    @Override
    public void onStart() {
        super.onStart();
        List<Conversation> list =  UniTalkConversationManager.getInstance().getConversations();
        adapter.clearAll();
        adapter.addAll(list);
        adapter.notifyDataSetChanged();
        Log.i(TAG, "onStart called");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach Called");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (UniTalkConversationManager.getInstance().hasConversations()) {
            Log.i(TAG, "konusmalar var.");
            List<Conversation> list =  UniTalkConversationManager.getInstance().getConversations();
            adapter = new ConversationListAdapter(getActivity(), list);
        } else
            adapter = new ConversationListAdapter(getActivity());
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
        UniTalkConversationManager.getInstance().addConversationManagerListener(this);
        UniTalkMUCManager.getINSTANCE().setConversationListener(this);
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop method called");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy method called");
        UniTalkConversationManager.getInstance().removeConversationManagerListener(null);
        UniTalkMUCManager.getINSTANCE().setConversationListener(null);
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Conversation conv = (Conversation) adapter.getItem(position);
        if(!conv.getConversationId().contains("conference")){
            createConversationOptionDialog(conv).show();
        } else {
            Intent i = new Intent(getContext(), GroupChatConversation.class);
            i.putExtra("conversationID", conv.getConversationId() + "/" + conv.getRoomName());
            startActivity(i);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public int getLayoutID() {
        return R.layout.conversation_list_fragment;
    }

    @Override
    public void update(final String conversationId ,final String printingText , final Conversation newConversation) {
        if (newConversation != null && adapter.findByConversationId(conversationId) == null) {
            adapter.add(newConversation);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (conversationId != null && !conversationId.isEmpty() && printingText != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.uptadeConversationText(conversationId,printingText);
                    adapter.notifyDataSetChanged();
                }
            });
        }else if(conversationId == null && printingText==null && newConversation== null){
            List<Conversation> list =  UniTalkConversationManager.getInstance().getConversations();
            adapter.clearAll();
            adapter.addAll(list);
            adapter.notifyDataSetChanged();
        }
    }

    public Dialog createConversationOptionDialog(final Conversation conversation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String userName = CommonMethods.getNameFromPhoneNumber(UniTalkApplication.getInstance(), conversation.getConversationId());
        userName = (userName==null ? conversation.getConversationId() : userName);
        CharSequence[] charSequence = new CharSequence[]{"Send Message to " + userName, "Remove this conversation"};

        builder.setItems(charSequence, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Intent i = new Intent(getActivity().getApplicationContext(), SingleConversation.class);
                        i.putExtra("conversationID", conversation.getConversationId());
                        startActivity(i);
                        break;
                    case 1:
                        UniTalkConversationManager.getInstance().deleteConversationAndChatMessages(conversation.getConversationId());
                        update(null,null,null);
                        break;
                }
            }
        });
        Dialog dialog = builder.create();
        return  dialog;
    }
}

