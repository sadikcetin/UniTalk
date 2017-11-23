package tr.org.uni_talk.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.account.UniTalkAccountManager;
import tr.org.uni_talk.app.UniTalkApplication;
import tr.org.uni_talk.connection.ConnectionJobService;
import tr.org.uni_talk.ui.fragments.AbstractListFragment;
import tr.org.uni_talk.ui.fragments.BlankListFragment;
import tr.org.uni_talk.ui.fragments.ContactListFragment;
import tr.org.uni_talk.ui.fragments.ConversationListFragment;
import tr.org.uni_talk.ui.toolbar.ChangeLogActivity;
import tr.org.uni_talk.ui.toolbar.FeedBackActivity;
import tr.org.uni_talk.ui.toolbar.GroupChatActivity;
import tr.org.uni_talk.ui.toolbar.InviteActivity;
import tr.org.uni_talk.ui.toolbar.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private List<Fragment> mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "### onCreate method called");
        UniTalkApplication.getInstance().inForeground();
        boolean newlyInitialized = getIntent().getBooleanExtra("newCreated", false);
        boolean incoming = getIntent().getBooleanExtra("incoming", false);

        Log.i(TAG, "Newly Created :" + newlyInitialized);
        Log.i(TAG, getIntent().getClass().toString());
        Log.i(TAG, getIntent().toString());

        int resultValue = 0;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    resultValue);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragments = new ArrayList<Fragment>();
        mFragments.add(new ContactListFragment());
        mFragments.add(new ConversationListFragment());
        mFragments.add(new BlankListFragment());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), mFragments);

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Fragment f = mSectionsPagerAdapter.getItem(position);
                if (f instanceof AbstractListFragment) {
                    ((AbstractListFragment)f).onActive();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mViewPager.setAdapter(mSectionsPagerAdapter);

        if (incoming) {
            mViewPager.setCurrentItem(1);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "onRequestPermissionsResult: MainActivity");

        if (requestCode == 0) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                UniTalkAccountManager.getInstance().rosterEntriesToContacts();
                mFragments.set(0, new ContactListFragment());
                mSectionsPagerAdapter.notifyDataSetChanged();
                mViewPager.setAdapter(mSectionsPagerAdapter);
            } else {
                Toast.makeText(this, "You must grant permission to continue", Toast.LENGTH_LONG).show();
                finish();
            }

        }
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() != 0)
            mViewPager.setCurrentItem(0);
        else
            super.onBackPressed();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        Log.i(TAG, "onContentChanged method called");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "##### onDestroy Called");
        super.onDestroy();
        ConnectionJobService.scheduleJob(getApplicationContext());
        UniTalkApplication.getInstance().inBackground();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        int id = item.getItemId();


        if (id == R.id.action_settings) {
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;

        } else if (id == R.id.action_invite) {

             intent = new Intent(this, InviteActivity.class);
            this.startActivity(intent);

            return true;
        } else if (id == R.id.action_help) {

            return true;
        }else if(id == R.id.action_create_group){

            intent = new Intent(this, GroupChatActivity.class);
            this.startActivity(intent);

            return true;
        } else if (id == R.id.feedback) {
            intent = new Intent(this, FeedBackActivity.class);
            this.startActivity(intent);
            return true;
        } else if (id == R.id.changelog) {
            intent = new Intent(this, ChangeLogActivity.class);
            this.startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding from
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        List<Fragment> fragmentList;

        public SectionsPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            fragmentList = fragments;

        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called from instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            //  return PlaceholderFragment.newInstance(position + 1);
            Fragment fragment = fragmentList.get(position);
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "CONTACTS";
                case 1:
                    return "MESSAGES";
                case 2:
                    return "CALLS";
            }
            return null;
        }
    }
}
