package net.println.itemanimatable;

import android.widget.BaseAdapter;

/**
 * Created by benny on 7/13/16.
 */
public abstract class AnimatableListAdapter<T> extends BaseAdapter {
    public static final String TAG = "RemovableExpandableAdapter";

    public abstract void removeAt(int position);

    public abstract void remove(T item);

    public abstract void add(T item);

    public abstract void addAt(int position, T item);

    @Override
    public final boolean hasStableIds() {
        return true;
    }
}
