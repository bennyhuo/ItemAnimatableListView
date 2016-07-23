package net.println.itemanimatablelistview;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import net.println.itemanimatable.ItemAnimatableListView;
import net.println.itemanimatable.ItemAnimatableListView.PositionGetter;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by benny on 7/22/16.
 */
public class ListViewActivity extends Activity {
    public static final String TAG = "ListViewActivity";

    ItemAnimatableListView listView;

    private ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);

        ArrayList<DataItem> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(new DataItem(String.valueOf(i)));
        }
        adapter = new ListAdapter(items);
        listView = (ItemAnimatableListView) findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                //Simple remove the item without effects on it while the other items may animate to the new place.
                //listView.animateRemove(position);

                //Remove the item with a fade out animation. Be careful with the position.
                final DataItem item = adapter.getItem(position);
                listView.animateRemove(new PositionGetter() {
                    @Override
                    public int getUpdatedPosition() {
                        return adapter.getPositionForItem(item);
                    }
                }, ObjectAnimator.ofFloat(null, "alpha", 1, 0));
            }
        });

        Toast.makeText(this, "Click item to remove it or click the empty area to add an item.", Toast.LENGTH_LONG).show();
    }

    public void onClick(View view) {
        DataItem item = new DataItem(String.valueOf(Math.random()));
        //Add an item with a default fade in animator.
        listView.animateAdd(new Random().nextInt(adapter.getCount()), item);
    }
}
