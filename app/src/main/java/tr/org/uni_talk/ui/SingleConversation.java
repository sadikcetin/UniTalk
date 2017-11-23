package tr.org.uni_talk.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.chatstates.ChatState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.conversation.IConversationListener;
import tr.org.uni_talk.conversation.UniTalkConversationManager;
import tr.org.uni_talk.db.DBHandler;
import tr.org.uni_talk.pojo.ChatMessage;
import tr.org.uni_talk.pojo.Contact;
import tr.org.uni_talk.ui.adapters.ChatAdapter;

public class SingleConversation extends Activity implements IConversationListener {

    private static final String TAG = SingleConversation.class.getName();
    private static final int PICK_IMAGE_REQUEST = 1;

    private Contact contact = null;
    private String receiver;
    private EditText messageField;
    private ChatAdapter chatAdapter;
    private ListView messageListView;
    private TextView userBehaviorTextViev;
    private ProgressBar mProgressBar;

    public void init() {
        Intent i = getIntent();
        this.receiver = i.getStringExtra("conversationID");
        Log.i(TAG, "THE CONVERSATION ID : " + this.receiver);
        this.contact = UniTalkAccountManager.getInstance().findContactByUserName(receiver);

        TextView txtProduct = (TextView) findViewById(R.id.headerText);
        if (this.contact != null) {
            txtProduct.setText(this.contact.getName());
        } else {
            Log.i(TAG, receiver + " uygun contact bulunamadi");
        }
        mProgressBar = (ProgressBar) findViewById(R.id.pb_sending_file);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.single_conversation);

        init();

        messageField = (EditText) findViewById(R.id.messageEditText);
        messageListView = (ListView) findViewById(R.id.msgListView);
        userBehaviorTextViev = (TextView) findViewById(R.id.userState);

        UniTalkConversationManager.getInstance().setActiveConversation(this);
        UniTalkConversationManager.getInstance().setChatState(ChatState.active, receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
        UniTalkConversationManager.getInstance().changeUserState(contact.getNumber() , null);

        ImageButton sendButton = (ImageButton) findViewById(R.id.sendMessageButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendTextMessage(v);
            }
        });

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("") && !s.toString().isEmpty())
                    UniTalkConversationManager.getInstance().setChatState(ChatState.composing , receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
                else if (s.toString().equals("") || !s.toString().isEmpty())
                    UniTalkConversationManager.getInstance().setChatState(ChatState.active , receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
            }
        });
        messageListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        messageListView.setStackFromBottom(true);

        prepareData();
    }

    public void prepareData() {
        DBHandler dbh = new DBHandler(UniTalkApplication.getInstance());

        String convId = dbh.getConversationIdWith(this.receiver);

        Log.i(TAG, this.receiver + " " + convId);

        List<ChatMessage> messages = dbh.getMessages(convId);

        if (messages != null && messages.size() > 0) {
            Log.i(TAG, this.receiver + " a ait " + messages.size() + " tane mesaj var");
            chatAdapter = new ChatAdapter(this, messages);
        } else
            chatAdapter = new ChatAdapter(this);

        messageListView.setAdapter(chatAdapter);
    }

    public void sendTextMessage(View v) {

        String message = messageField.getText().toString().trim();

        if (!message.equalsIgnoreCase("")) {

            String from = UniTalkAccountManager.getInstance().getAccount().getUserName();
            final ChatMessage chatMessage = new ChatMessage(receiver, message, true);
            onSend(receiver, chatMessage);
            messageField.setText("");
        }
    }

    public void onBackPressed(View v) {
        UniTalkConversationManager.getInstance().setChatState(ChatState.gone ,receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
        UniTalkConversationManager.getInstance().setActiveConversation(null);
        onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.e(TAG, "onNewIntent: ");
        Bundle extras = intent.getExtras();
        Intent msgIntent = new Intent(this, SingleConversation.class);
        msgIntent.putExtras(extras);
        startActivity(msgIntent);
        finish();

        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UniTalkConversationManager.getInstance().setChatState(ChatState.active ,receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
        UniTalkConversationManager.getInstance().setActiveConversation(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UniTalkConversationManager.getInstance().setChatState(ChatState.gone ,receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
        UniTalkConversationManager.getInstance().setActiveConversation(null);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy Called");
        UniTalkConversationManager.getInstance().setChatState(ChatState.gone ,receiver + UniTalkConfig.SERVER.HOST_START_WITH_AT);
        UniTalkConversationManager.getInstance().setActiveConversation(null);
        Log.i(TAG, "Destroying activeConversation in ConversationManager");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called");
    }

    @Override
    public void onReceive(String from, final ChatMessage chatMessage) {
        chatAdapter.add(chatMessage);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onServerAcknowledgeReceived(String to, final String messageID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.sent(messageID);
                chatAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDelivered(String fromContact, final String messageID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.delivered(messageID);
                chatAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onRead(String from, String messageID) {

    }

    @Override
    public void onSend(String to, final ChatMessage chatMessage) {

        Log.e(TAG, "onSend: " + to);
        UniTalkConversationManager.getInstance().send(to, chatMessage);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.add(chatMessage);
                chatAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void changeUserState(final String userState , String member) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                userBehaviorTextViev.setText(userState);
            }
        });

    }

    @Override
    public Contact getContact() {
        return contact;
    }

    public void sendFile(View view) {
        int id = view.getId();
        if (id == R.id.imageView4) {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            CharSequence colors[] = new CharSequence[]{"Image (.jpg, .png)", "File (.pdf, .doc)"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Send file");
            builder.setItems(colors, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png"});
                            startActivityForResult(intent, PICK_IMAGE_REQUEST);
                            break;
                        case 1:
                            Toast.makeText(getApplicationContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });
            builder.show();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Log.e(TAG, "onActivityResult: " + data.getData());
            final Pair<String, Long> pair = getNameAndSize(SingleConversation.this, data.getData());

            final ChatMessage chatMessage = new ChatMessage(contact.getNumber(), "", true);
            chatMessage.setMessageIdForImage("image/jpeg");

            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

                byte[] bytes;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    mProgressBar.setVisibility(View.VISIBLE);
                    try {
                        ExifInterface ei;
                        int orientation = ExifInterface.ORIENTATION_NORMAL;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            try {
                                ei = new ExifInterface(getContentResolver().openFileDescriptor(data.getData(), "r").getFileDescriptor());
                                orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                Log.e(TAG, "prepareForSending: " + orientation);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        bytes = UniTalkConversationManager.getInstance().prepareForSending(chatMessage.getMessageId(), orientation, getContentResolver().openInputStream(data.getData()));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        if (bytes == null) {
                            cancel(true);
                            return false;
                        }
                        UniTalkConversationManager.getInstance().sendFile(chatMessage.getMessageId(), contact.getNumber(), bytes);

                    } catch (XMPPException | SmackException | IOException e) {
                        e.printStackTrace();
                        return false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean aVoid) {
                    if (!aVoid) {
                        Toast.makeText(SingleConversation.this, "Sending file failed!", Toast.LENGTH_SHORT).show();
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                chatAdapter.add(chatMessage);
                                chatAdapter.notifyDataSetChanged();
                            }
                        });
                        UniTalkConversationManager.getInstance().onSend(contact.getNumber(), chatMessage);
                        UniTalkConversationManager.getInstance().onDelivered(contact.getNumber(), chatMessage.getMessageId());
                    }
                    mProgressBar.setVisibility(View.INVISIBLE);
                    super.onPostExecute(aVoid);
                }
            }.execute();
        }
    }

    private Pair<String, Long> getNameAndSize(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {

            Log.e(TAG, "getRealPathFromURI: uri path: " + contentUri.getPath());
            String[] projection = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
            cursor = context.getContentResolver().query(contentUri, projection, null, null, null);
            cursor.moveToFirst();
            String name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            long size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            Log.e(TAG, "getRealPathFromURI: " + name);
            Log.e(TAG, "getRealPathFromURI: " + size);
            Pair<String, Long> pair = new Pair<>(name, size);
            return pair;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getConversationId() {
        return receiver;
    }
}