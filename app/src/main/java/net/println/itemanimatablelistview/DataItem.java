package net.println.itemanimatablelistview;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by benny on 7/17/16.
 */
public class DataItem {
    public static final String TAG = "DataItem";

    public final long id;
    public String data;

    public DataItem parent;
    private List<DataItem> chilren = new ArrayList<DataItem>();

    public DataItem(String data) {
        this.id = System.currentTimeMillis() + new Random().nextInt();
        this.data = data;
    }

    public void addChild(DataItem item){
        chilren.add(item);
        item.parent = this;
    }

    public void addChildAt(int position, DataItem item){
        chilren.add(position, item);
        item.parent = this;
    }

    public void removeChildAt(int position){
        DataItem item = chilren.remove(position);
        if(item != null){
            item.parent = null;
        }
    }

    public void removeChild(DataItem item){
        if(item != null && chilren.remove(item)){
            item.parent = null;
        }
    }

    public int getChildCount(){
        return chilren.size();
    }

    public DataItem getChildAt(int position){
        return chilren.get(position);
    }

    public int indexOfChild(DataItem item){
        return chilren.indexOf(item);
    }
}
