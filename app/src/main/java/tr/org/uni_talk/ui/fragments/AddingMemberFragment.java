package tr.org.uni_talk.ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.muc.UniTalkMUCManager;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.ui.adapters.AddingMemberAdapter;
import tr.org.uni_talk.util.CommonMethods;

/**
 * Created by oem on 28.10.2017.
 */

public class AddingMemberFragment extends Fragment{

    private final static String TAG = AddingMemberFragment.class.getSimpleName();

    private ListView contactListView;
    private View addMemberButton;
    private List<Contact> contacts;
    private List<Contact> selectedContacts;
    private AddingMemberAdapter addingMemberAdapter;
    private String members;
    private String conversationId;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_member_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        members = getArguments().getString("members");
        conversationId = getArguments().getString("conversationId");
        selectedContacts = new ArrayList<Contact>();
        contactListView = (ListView) getActivity().findViewById(R.id.contactListView);
        addMemberButton = getActivity().findViewById(R.id.addButton);

        DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());
        contacts = dbh.getAllContacts();
        Log.e(TAG, "onCreate: " + contacts.size());
        Collections.sort(contacts);
        organizeContacts(members);
        addingMemberAdapter = new AddingMemberAdapter(getActivity(),R.layout.group_chat_item,contacts);
        contactListView.setAdapter(addingMemberAdapter);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        contactListView.setClickable(true);

        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Contact contact = contacts.get(position);
                Log.e(TAG, "onItemClick: " + contact.getName() + " " + contact.getNumber());

                if(contact.isSelectable()){

                    ((AddingMemberAdapter)contactListView.getAdapter()).toggleSelected(new Integer(position));

                    if (selectedContacts.contains(contacts.get(position)))
                        selectedContacts.remove(contacts.get(position));
                    else
                        selectedContacts.add(contacts.get(position));

                    Log.e(TAG, "onItemClick: " + selectedContacts.toString());
                    addingMemberAdapter.notifyDataSetChanged();
                }

            }
        });

        addMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedContacts.isEmpty()){
                    Toast.makeText(UniTalkApplication.getInstance(),"Please select new member(s)",Toast.LENGTH_SHORT);
                }else{
                    try {
                        DBHandler dbHandler = new DBHandler(getActivity());
                        Conversation conversation = dbHandler.findConversationById(conversationId);
                        Log.e(TAG, "onClick: " + conversation.getLastMessage().getBody());
                        for (Contact contact : selectedContacts) {
                            if(contact!= null){
                                UniTalkMUCManager.getINSTANCE().getMUC(CommonMethods.getNumber(conversationId)).grantMembership(JidCreate.entityBareFrom(contact.getNumber() + UniTalkConfig.SERVER.HOST_START_WITH_AT));
                            }
                        }
                        for (Contact contact : selectedContacts){
                            if(contact != null){
                                conversation.setReceiverString(conversation.getReceiverString()+contact.getNumber()+",");
                            }
                        }
                        //invite etmeden önce publish yapılması ve db'nin update edilmesi gerekmekte ...
                        dbHandler.updateConversationRoomInfo(conversation);
                        Log.e(TAG, "onClick: members" + conversation.getReceiverString());
                        Log.e(TAG, "onClick: owners" + conversation.getOwners());
                        UniTalkMUCManager.getINSTANCE().updateRoomInfoLeafNode(CommonMethods.getNumber(conversationId),conversation.getReceiverString() ,conversation.getOwners());
                        UniTalkMUCManager.getINSTANCE().changeUserState("",conversation.getReceiverString());

                        for (Contact contact : selectedContacts){
                            if(contact != null){
                                Log.e(TAG, "onClick: " + contact.getNumber());
                                UniTalkMUCManager.getINSTANCE().getMUC(CommonMethods.getNumber(conversationId)).invite(JidCreate.entityBareFrom(contact.getNumber() + UniTalkConfig.SERVER.HOST_START_WITH_AT + "/" + conversation.getRoomName()), "No reason");
                            }
                        }
                        Log.e(TAG, "onClick2: " + conversation.getReceiverString());
                        getActivity().getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }catch (XMPPException.XMPPErrorException | SmackException e){
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                    }catch (NullPointerException nee){
                        createWarningDialog("Please check your connection and try again later ...").show();
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void organizeContacts(String members){
        for(Contact con : contacts){
            if(members.contains(con.getNumber())){
                Log.e(TAG, "organizeContacts: " + con.getNumber());
                con.setSelectable(false);
            }else{
                con.setSelectable(true);
            }
        }
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
}
