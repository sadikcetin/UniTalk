package tr.org.uni_talk.ui.toolbar;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import tr.org.uni_talk.R;

public class ChangeLogActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_log);


    }

    public void onBackPressed(View v) {
        onBackPressed();
    }


}
