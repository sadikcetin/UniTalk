package tr.org.uni_talk.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import tr.org.uni_talk.R;


/**
 * Created by sadik on 8/5/16.
 */
public class BlankListFragment extends AbstractListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long id) {

    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public int getLayoutID() {
        return R.layout.blank_list_fragment;
    }
}
