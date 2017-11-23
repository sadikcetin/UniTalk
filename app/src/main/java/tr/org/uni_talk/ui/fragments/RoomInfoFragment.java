package tr.org.uni_talk.ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.muc.IRoomInfoListener;
import tr.org.uni_talk.muc.UniTalkMUCManager;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.ui.SingleConversation;
import tr.org.uni_talk.ui.adapters.ContactListAdapter;
import tr.org.uni_talk.util.FileUtils;

/**
 * Created by oem on 18.10.2017.
 */

public class RoomInfoFragment extends Fragment implements IRoomInfoListener{

    private static final String TAG = RoomInfoFragment.class.getName();
    String conversationId = null;
    String members = null;
    List<Contact> memberList = null;
    ContactListAdapter adapter = null;
    ListView listView;
    Button leaveButton;
    Button addMemberButton;
    Button destroyButton;
    Button removeConversationButton;
    Boolean isThisUserOwner = true;
    String userNumber = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.room_info_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "GroupInfo is created.");
        conversationId = getArguments().getString("conversationId");
        Log.i(TAG, "ConversationId : " + conversationId);
        UniTalkMUCManager.getINSTANCE().setRoomInfoListener(this);
        userNumber =  FileUtils.readDBFile(UniTalkApplication.getInstance().getRegistrationFile()).getUserName();
        leaveButton = (Button) getActivity().findViewById(R.id.leaveButton);
        addMemberButton = (Button) getActivity().findViewById(R.id.addMemberButton);
        destroyButton = (Button) getActivity().findViewById(R.id.destroyButton);
        removeConversationButton = (Button) getActivity().findViewById(R.id.removeButton);

        final DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
        final Conversation conversation = dbHandler.findConversationById(conversationId);
        memberList = getAffiliationList(conversation ,dbHandler);
        adapter = new ContactListAdapter(getActivity());
        adapter.clearAll();
        adapter.addAll(memberList);

        listView = (ListView) getActivity().findViewById(R.id.memberListView);
        listView.setAdapter(adapter);

        Log.e(TAG, "onActivityCreated: Owners:" + conversation.getOwners());

        if(!conversation.getOwners().contains(userNumber)){
            isThisUserOwner = false;
            addMemberButton.setVisibility(View.GONE);
            destroyButton.setVisibility(View.GONE);
        }

        if(conversation.isJoinable() == 0){
            leaveButton.setVisibility(View.GONE);
            addMemberButton.setVisibility(View.GONE);
            destroyButton.setVisibility(View.GONE);
        }

        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    UniTalkMUCManager.getINSTANCE().leaveRoom(conversationId , memberList);
                }catch (NullPointerException nee){
                    createWarningDialog("Please check your connection and try again later.").show();
                }

            }
        });

        addMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("members" , members);
                args.putString("conversationId" , conversationId);
                Fragment addingMemberFragment = new AddingMemberFragment();

                addingMemberFragment.setArguments(args);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();

                transaction.replace(R.id.group_chat_container, addingMemberFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        removeConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "deleteChatMessages: " + "is called.");
                DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
                Conversation conversation = dbHandler.findConversationById(conversationId);
                if(conversation.isJoinable() == 0){
                    UniTalkMUCManager.getINSTANCE().deleteConversationAndChatMessages(conversationId);
                    getActivity().finish();
                }else {
                    Log.e(TAG, "deleteChatMessages: " + "is called. Not Remove ...");
                    createLeaveDialog(conversationId).show();
                }
            }
        });

        destroyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBHandler dbHandler = new DBHandler(UniTalkApplication.getInstance());
                Conversation conversation = dbHandler.findConversationById(conversationId);
                if (conversation.getReceiverString().equals(userNumber + ",")) {
                    try {
                        UniTalkMUCManager.getINSTANCE().destroyRoom(conversationId);
                        getActivity().finish();
                    }catch (NullPointerException nee){
                        createWarningDialog("Please check your connection and try again later.").show();
                    }
                }else {
                    createWarningDialog("There are one more member in this group! Please try again later.").show();
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = memberList.get(position);
                if (!contact.getName().equals("Siz")) {
                    Dialog dialog = createContactDialog(contact);
                    dialog.show();
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private List<Contact> getAffiliationList(Conversation conversation , DBHandler dbHandler){
        Log.i(TAG, "Member List is created.");
        members = conversation.getReceiverString();
        Log.e(TAG, "getAffiliationList: " + conversation.getReceiverString());
        List<Contact> list = new ArrayList();
        List<String> memberNumberList = Arrays.asList(members.split(","));

        for (String number : memberNumberList) {
            if(!number.equals(userNumber)){
                Contact contact = dbHandler.getContactByNumber(number);
                if(contact != null){
                    if (conversation.getOwners().contains(contact.getNumber()))
                        contact.setOwner(true);
                    list.add(contact);
                }else{
                    contact = new Contact();
                    contact.setName(number);
                    contact.setNumber(number);
                    if (conversation.getOwners().contains(contact.getNumber()))
                        contact.setOwner(true);
                    list.add(contact);
                }
            }
        }

        if(conversation.isJoinable() != 0){
            Contact contact = new Contact();
            contact.setName("Siz");
            if (conversation.getOwners().contains(userNumber))
                contact.setOwner(true);
            list.add(contact);
        }
        return list;
    }

    public Dialog createLeaveDialog(final String conversationId){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity() , R.style.LightDialogTheme);

        builder.setMessage(R.string.dialog_message)
                .setTitle(R.string.dialog_title);

        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                try {
                    UniTalkMUCManager.getINSTANCE().leaveRoom(conversationId , memberList);
                    UniTalkMUCManager.getINSTANCE().deleteConversationAndChatMessages(conversationId);
                    getActivity().finish();
                }catch (NullPointerException nee){
                    createWarningDialog("Please check your connection and try again later.").show();
                    dialog.cancel();
                }

            }
        });

        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        return  dialog;
    }

    public Dialog createContactDialog(final Contact contact){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        CharSequence[] charSequence = new CharSequence[]{"Send Message to " + contact.getName()}; ;

        if(isThisUserOwner==true && contact != null){
            if (!contact.isOwner())
                charSequence = new CharSequence[]{"Send Message to " + contact.getName(), "Kick " + contact.getName(), "Grant Ownership to " + contact.getName()};
            builder.setItems(charSequence, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Intent i = new Intent(getActivity().getApplicationContext(), SingleConversation.class);
                            i.putExtra("conversationID", contact.getNumber());
                            startActivity(i);
                            break;
                        case 1:
                            Log.e(TAG, "onClick: " + contact.getNumber() );
                            Log.e(TAG, "onClick: " + conversationId );
                            try {
                                UniTalkMUCManager.getINSTANCE().kickAffiliation(conversationId,contact);
                            }catch (NullPointerException nee){
                                createWarningDialog("Please check your connection and try again later ...").show();
                            }
                            break;

                        case 2:
                            Log.e(TAG, "onClick: " + contact.getNumber());
                            Log.e(TAG, "onClick: " + conversationId);
                            try {
                                UniTalkMUCManager.getINSTANCE().grantOwnership(conversationId, contact);
                            }catch (NullPointerException nee){
                                createWarningDialog("Please check your connection and try again later ...").show();
                            }
                            break;
                    }
                }
            });
        }else if(isThisUserOwner == false && contact != null){
            charSequence = new CharSequence[]{"Send Message to " + contact.getName()};
            builder.setItems(charSequence, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Intent i = new Intent(getActivity().getApplicationContext(), SingleConversation.class);
                            i.putExtra("conversationID", contact.getNumber());
                            startActivity(i);
                            break;
                    }
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        return  alertDialog;
    }

    public Dialog createWarningDialog(String warningMessage){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity() , R.style.LightDialogTheme);

        builder.setMessage(warningMessage)
                .setTitle(R.string.dialog_title);

        builder.setNegativeButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        return  dialog;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy is called");
        super.onDestroy();
        UniTalkMUCManager.getINSTANCE().setRoomInfoListener(null);
    }

    @Override
    public void updateAffiliations(final Conversation conversation , final DBHandler dbHandler) {
        memberList = getAffiliationList(conversation , dbHandler);
        adapter.clearAll();
        adapter.addAll(memberList);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void updateAuthorizations(final Conversation conversation) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!conversation.getOwners().contains(userNumber)){
                    isThisUserOwner = false;
                    addMemberButton.setVisibility(View.GONE);
                    destroyButton.setVisibility(View.GONE);
                }else{
                    isThisUserOwner = true;
                    addMemberButton.setVisibility(View.VISIBLE);
                    destroyButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void updateLeaveButton(final Conversation conversation){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(conversation.isJoinable() == 0){
                    leaveButton.setVisibility(View.GONE);
                }else{
                    leaveButton.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    public String getConversationId() {
        return conversationId;
    }


}
