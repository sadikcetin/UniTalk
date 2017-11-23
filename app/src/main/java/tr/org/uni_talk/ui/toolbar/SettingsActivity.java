package tr.org.uni_talk.ui.toolbar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import tr.org.uni_talk.R;
import tr.org.uni_talk.ui.fragments.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.unitalk_settings_container, new SettingsFragment())
                .commit();
    }

    public void onBackPressed(View view) {
        int id = view.getId();
        if (id == R.id.imageView2)
            super.onBackPressed();
    }
}
