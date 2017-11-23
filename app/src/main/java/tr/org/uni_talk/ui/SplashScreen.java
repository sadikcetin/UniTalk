package tr.org.uni_talk.ui;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import tr.org.uni_talk.R;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.connection.ConnectionJobService;

public class SplashScreen extends AppCompatActivity implements View.OnClickListener {

    private static String USER;
    private static final String PASS = ""; // user password

    private static final String TAG = SplashScreen.class.getName();
    private static final String NUMBER_MESSAGE = "Phone number field can NOT be Blank!";
    private boolean newCreated = false;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UniTalkApplication.getInstance().isRegistered()) {
            Log.i(TAG, String.valueOf(getIntent().getBooleanExtra("newlyInitialized", false)));
            ConnectionJobService.scheduleJob(getApplicationContext());
            Intent i = new Intent(this, MainActivity.class);
            if (progressDialog != null)
                progressDialog.dismiss();
            startActivity(i);
            this.finish();
        } else {
            setContentView(R.layout.activity_splash_screen);
            progressDialog = new ProgressDialog(this);
        }
    }

    @Override
    public void onClick(View v) {
        EditText editTextPhoneNumber = (EditText) findViewById(R.id.phoneNumber);
        String userName = editTextPhoneNumber.getText().toString();

        if (userName != null && !userName.trim().isEmpty()) {

            progressDialog.setTitle("Register Unitalk");
            progressDialog.setMessage("Registering...");
            progressDialog.show();


            if (UniTalkApplication.getInstance().registerAccount(userName, PASS)) {

                ConnectionJobService.scheduleJob(getApplicationContext());
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("newCreated", true);
                startActivity(i);
                this.finish();

            } else {
                Toast.makeText(getApplicationContext(), "Please check your connection", Toast.LENGTH_SHORT).show();
            }

            progressDialog.dismiss();

        } else {
            Toast.makeText(getApplicationContext(), NUMBER_MESSAGE, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
