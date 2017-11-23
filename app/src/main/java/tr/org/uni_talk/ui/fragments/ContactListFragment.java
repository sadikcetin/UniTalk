package tr.org.uni_talk.ui.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.util.Collections;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.account.OnContactsLoadListener;
import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.ui.SingleConversation;
import tr.org.uni_talk.ui.adapters.ContactListAdapter;


public class ContactListFragment extends AbstractListFragment implements OnContactsLoadListener {

    private static final String TAG = ContactListFragment.class.getName();

    private ContactListAdapter adapter;

    private DBHandler dbHandler;

    // 3
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated called");
        super.onActivityCreated(savedInstanceState);
        adapter = new ContactListAdapter(getActivity());

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            if (UniTalkAccountManager.getInstance().getContactList().size() > 0) {
                onContactsLoaded();
            } else {

                UniTalkAccountManager.getInstance().addContactLoadListener(this);
            }
        }


        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
    }

    // 1
    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "onAttach method called with : " + UniTalkAccountManager.getInstance().isLoaded());
        super.onAttach(context);
        dbHandler = new DBHandler(getContext());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Contact contact = (Contact) adapter.getItem(position);
        Intent i = new Intent(getContext(), SingleConversation.class);
        
        i.putExtra("conversationID", contact.getNumber());
        contact = null;
        startActivity(i);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy method called");
        super.onDestroy();
        UniTalkAccountManager.getInstance().removeContactLoadListener(this);
    }

    @Override
    public void onContactsLoaded() {


        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<Contact> contactList = UniTalkAccountManager.getInstance().getContactList();
                Collections.sort(contactList);
                adapter.clearAll();
                for (Contact contact : contactList) {
                    adapter.add(contact);
                }
                adapter.notifyDataSetChanged();
                Log.i(TAG, "adapter notifyDataSetChanged triggered");
            }
        });
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public int getLayoutID() {
        return R.layout.contact_list_fragment;
    }
}
