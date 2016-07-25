package net.println.itemanimatable;

import android.widget.BaseExpandableListAdapter;

/**
 * Created by benny on 7/13/16.
 */
public abstract class AnimatableExpandableListAdapter<G, C> extends BaseExpandableListAdapter {
    public static final String TAG = "RemovableExpandableAdapter";

    public abstract void removeGroupAt(int groupPosition);

    public abstract void removeChildAt(int groupPosition, int childPosition);

    public abstract void removeGroup(G group);

    public abstract void removeChild(C child, int groupPosition);

    public abstract void addGroup(G group);

    public abstract void addGroupAt(G group, int groupPosition);

    public abstract void addChildToGroup(C child, int groupPosition);

    public abstract void addChildToGroupAt(C child, int groupPosition, int childPosition);

    @Override
    public final boolean hasStableIds() {
        return true;
    }
}
