package tr.org.uni_talk.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public abstract class AbstractListFragment extends ListFragment implements AdapterView.OnItemClickListener {

    private static final String TAG = AbstractListFragment.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView method called");
        View view = inflater.inflate(getLayoutID(), container, false);
        return view;
    }

    public abstract String getName();

    public abstract int getLayoutID();

    public void onActive() {

    }

}
