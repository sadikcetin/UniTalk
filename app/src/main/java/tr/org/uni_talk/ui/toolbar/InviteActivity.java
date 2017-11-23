package tr.org.uni_talk.ui.toolbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import tr.org.uni_talk.R;


public class InviteActivity extends Activity {

    EditText messageField;
    ImageButton mShareButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        messageField = (EditText) findViewById(R.id.invite_text);
        mShareButton = (ImageButton) findViewById(R.id.share_button);

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, messageField.getText().toString());
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, "Send via"));
            }
        });
    }




}
