package net.println.itemanimatablelistview;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.Toast;

import net.println.itemanimatable.ItemAnimatableExpandableListView;
import net.println.itemanimatable.ItemAnimatableExpandableListView.PositionGetter;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by benny on 7/22/16.
 */
public class ExpandableListViewActivity extends Activity {
    public static final String TAG = "ListViewActivity";

    ItemAnimatableExpandableListView listView;

    private ExpandableListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expandablelistview);

        ArrayList<DataItem> items = new ArrayList<DataItem>();
        for (int i = 0; i < 3; i++) {
            DataItem item = new DataItem(String.valueOf(i));
            for (int j = 0; j < 5; j++) {
                item.addChild(new DataItem(String.valueOf(j)));
            }
            items.add(item);
        }
        adapter = new ExpandableListAdapter(items);
        listView = (ItemAnimatableExpandableListView) findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                final DataItem item = adapter.getChild(groupPosition, childPosition);
                listView.animateRemove(new PositionGetter() {
                    @Override
                    public int[] getUpdatedPosition() {
                        return adapter.getPositionForItem(item);
                    }
                }, ObjectAnimator.ofFloat(null, "alpha", 1, 0));
                //listView.animateRemove(groupPosition, childPosition);
                return true;
            }
        });

        listView.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if(adapter.getChildrenCount(groupPosition) == 0){
                    final DataItem item = adapter.getGroup(groupPosition);
                    listView.animateRemove(new PositionGetter() {
                        @Override
                        public int[] getUpdatedPosition() {
                            return adapter.getPositionForItem(item);
                        }
                    }, ObjectAnimator.ofFloat(null, "alpha", 1, 0));
                    return true;
                }else if(!parent.isGroupExpanded(groupPosition)){
                    listView.animateExpand(groupPosition);
                    return true;
                }else if(parent.isGroupExpanded(groupPosition)){
                    listView.animateCollapse(groupPosition);
                    return true;
                }
                return false;
            }
        });

        Toast.makeText(this, "Click item to remove it or click the empty area to add an item.", Toast.LENGTH_LONG).show();
    }

    public void onClick(View view) {
        int group = new Random().nextInt(adapter.getGroupCount());
        listView.animateAdd(group, new Random().nextInt(adapter.getChildrenCount(group) + 1) - 1, new DataItem(String.valueOf(Math.random())));
    }
}
