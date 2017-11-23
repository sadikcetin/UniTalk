package tr.org.uni_talk.ui.toolbar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.mail.MailSender;

/**
 * Created by oem on 27.09.2017.
 */

public class FeedBackActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1;
    String imagePath = null;
    EditText messageField;
    EditText userNameFiled;
    ImageButton mSendButton;
    ImageButton mSelectImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);

        messageField = (EditText) findViewById(R.id.opinion_text);
        userNameFiled = (EditText) findViewById(R.id.name_text);
        mSendButton = (ImageButton) findViewById(R.id.send_button);
        mSelectImage = (ImageButton) findViewById(R.id.image_select_button);


        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = userNameFiled.getText().toString();
                String feedbackMessage = messageField.getText().toString();

                if (userName != null && !userName.trim().isEmpty() && !feedbackMessage.trim().isEmpty() && feedbackMessage != null) {

                    final MailSender mailSender = new MailSender(feedbackMessage, imagePath, userName);
                    final ProgressDialog pd = ProgressDialog.show(FeedBackActivity.this, "Please Wait", "Feedback sending...", true, true);
                    pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "Your Feedback Sent! Thank You :)", Toast.LENGTH_SHORT).show();
                        }
                    });

                    pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "Process cancelled!", Toast.LENGTH_SHORT).show();
                        }
                    });


                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mailSender.sendMail();

                            } catch (Exception e) {
                                pd.cancel();
                                Log.e("Progress Dialog", e.getMessage());
                            }
                            pd.dismiss();
                        }
                    }).start();




                    messageField.setText("");
                    userNameFiled.setText("");
                    imagePath = null;


                } else {
                    Toast.makeText(UniTalkApplication.getInstance().getApplicationContext(), "Please enter your name and message!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
    }

    public void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            imagePath = getPath(selectedImageUri);
            if (imagePath == null) {
                Log.e("Fail", " ImagePath Fail oldu.");
            } else {
                Log.e("onActivityResult: ", imagePath);
            }
        }
    }

    public String getPath(Uri uri) {
        if (uri == null) {
            return null;
        }
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        }
        return uri.getPath();
    }

}
