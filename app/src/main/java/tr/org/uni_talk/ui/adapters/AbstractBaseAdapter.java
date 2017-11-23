package tr.org.uni_talk.ui.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.org.uni_talk.pojo.IPojo;

public abstract class AbstractBaseAdapter<E extends IPojo> extends BaseAdapter implements IDataAdapter<E> {

    private LayoutInflater inflater = null;
    private List<E> containerList;

    public AbstractBaseAdapter(Activity activity) {
        this.containerList = new ArrayList<E>();
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public AbstractBaseAdapter(Activity activity, List<E> list) {
        this(activity);
        addAll(list);

    }

    protected final LayoutInflater getInflater() {
        return this.inflater;
    }


    @Override
    public int getCount() {
        return this.containerList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.containerList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void removeItem(int position){
        this.containerList.remove(position);
    }

    public void addItemFirstIndex(E item){
        this.containerList.add(0,item);
    }

    @Override
    public void add(E item) {
        this.containerList.add(item);
    }

    @Override
    public void addAll(List<E> items) {
        for (E item : items) {
            this.add(item);
        }
    }

    @Override
    public void removeItem(E item) {
        this.containerList.remove(item);

    }

    @Override
    public void clearAll() {
        if (null != this.containerList && this.containerList.size() > 0) {
            this.containerList.clear();
        }
    }


}
