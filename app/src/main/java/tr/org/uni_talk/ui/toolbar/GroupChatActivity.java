package tr.org.uni_talk.ui.toolbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import tr.org.uni_talk.R;
import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.muc.UniTalkMUCManager;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.pojo.Conversation;
import tr.org.uni_talk.ui.GroupChatConversation;
import tr.org.uni_talk.ui.adapters.GroupChatAdapter;

/**
 * Created by sadik on 8/24/16.
 */
public class GroupChatActivity extends Activity {

    public final String TAG = this.getClass().getName();

    private  EditText groupNameText = null;
    private DBHandler dbh ;
    private GroupChatAdapter adapter;
    private List<Contact> contacts;
    private short lockCreateButton = 10;


    public List<Contact> selectedContacts;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group_chat);

        dbh = new DBHandler(UniTalkApplication.getInstance());
        contacts = dbh.getAllContacts();
        Log.e(TAG, "onCreate: " + contacts.size());
        Collections.sort(contacts);
        adapter = new GroupChatAdapter(this,R.layout.group_chat_item,contacts);
        selectedContacts = new ArrayList<Contact>();

        final ListView listView = (ListView)findViewById(R.id.groupListView);
        groupNameText = (EditText) findViewById(R.id.editTextGroupName);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        listView.setAdapter(adapter);
        listView.setClickable(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ViewGroup vg = (ViewGroup) view;
                TextView tv = (TextView) vg.findViewById(R.id.textViewGroupUser);

                ((GroupChatAdapter)listView.getAdapter()).toggleSelected(new Integer(position));

                Contact contact = contacts.get(position);
                Log.e(TAG, "onItemClick: " + contact.getName() + " " + contact.getNumber());

                if (selectedContacts.contains(contacts.get(position)))
                    selectedContacts.remove(contacts.get(position));
                else
                    selectedContacts.add(contacts.get(position));

                adapter.notifyDataSetChanged();
            }
        });
    }

    public void onBackPressed(View v) {
        onBackPressed();
    }

    public void onCreateGroup(View v){

        String groupName = groupNameText.getText().toString().trim();

        if (!groupName.equals("") && groupName.length() <= 20 && !isAlphaNumeric(groupName)) {

            if (selectedContacts.size() < 1)
                Toast.makeText(getApplicationContext(), "You must select at least one contact", Toast.LENGTH_SHORT).show();
            else {

                Conversation conversation = new Conversation();

                for (Contact contact : selectedContacts) {
                    Log.e("Contacts: ", contact.getJid() + "");
                }

                if (UniTalkAccountManager.getInstance().findContactByUserName(groupName) == null) {
                    try {
                        UniTalkMUCManager.getINSTANCE().createOrJoinRoom(checkGroupName(groupName), selectedContacts);
                        Log.e(TAG, "Intent ConID:" + UniTalkMUCManager.getINSTANCE().getMuc().getRoom() + "/" + groupName);
                        Intent intent = new Intent(this, GroupChatConversation.class);
                        intent.putExtra("conversationID",UniTalkMUCManager.getINSTANCE().getMuc().getRoom() + "/" + groupName);
                        intent.putExtra("newlyInitialized", true);
                        GroupChatActivity.this.finish();
                        startActivity(intent);
                    } catch (NullPointerException nee){
                        createWarningDialog("Please check your connection and try again later...").show();
                    } catch (XMPPException.XMPPErrorException e) {
                        e.printStackTrace();
                        if(lockCreateButton > 0){
                            Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "Create group failed!", Toast.LENGTH_SHORT).show();
                            lockCreateButton--;
                        }
                    } catch (SmackException e) {
                        e.printStackTrace();
                        if(lockCreateButton > 0){
                            Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "Group is already existing. Please write unsing group name!", Toast.LENGTH_SHORT).show();
                            lockCreateButton--;
                        }
                    } catch (IllegalStateException ise){
                        ise.printStackTrace();
                        if(lockCreateButton > 0)
                        Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "You are already create this group", Toast.LENGTH_SHORT).show();
                        lockCreateButton--;
                    }
                }
            }

        } else if (lockCreateButton > 0 && isAlphaNumeric(groupName)) {
            Toast toast = Toast.makeText(getApplicationContext(), "Group name can not include alphanumeric character", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL,0,0);
            toast.show();
            lockCreateButton--;
        }
        else if (groupName.length() > 20 && lockCreateButton > 0) {
            Toast toast = Toast.makeText(getApplicationContext(), "Group name character lenght cannot be more than 20", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            lockCreateButton--;
        } else if (lockCreateButton > 0) {
            Toast toast = Toast.makeText(getApplicationContext(), "Group name cannot be blank!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL,0,0);
            toast.show();
            lockCreateButton--;
        }
    }

    private  String checkGroupName(String groupName){
        String buffer = groupName.replaceAll("\\s+" ,"_");
        return  buffer;
    }

    public boolean isAlphaNumeric(String string){
        Pattern p = Pattern.compile("[^a-zA-Z0-9]+-+!");
        boolean hasSpecialChar = p.matcher(string).find();
        return hasSpecialChar;
    }

    public Dialog createWarningDialog(String warningMessage){
        AlertDialog.Builder builder = new AlertDialog.Builder(this , R.style.LightDialogTheme);

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
