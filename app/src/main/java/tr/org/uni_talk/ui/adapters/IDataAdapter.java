package tr.org.uni_talk.ui.adapters;

import java.util.List;

import tr.org.uni_talk.pojo.IPojo;

public interface IDataAdapter<E extends IPojo> {

    void add(E item);

    void addAll(List<E> items);

    void removeItem(E item);

    void clearAll();
}
