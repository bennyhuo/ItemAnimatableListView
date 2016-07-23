package net.println.itemanimatablelistview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.println.itemanimatable.AnimatableListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by benny on 7/17/16.
 */
public class ListAdapter extends AnimatableListAdapter<DataItem> {
    public static final String TAG = "ListAdapter";

    private List<DataItem> items = new ArrayList<DataItem>();

    public ListAdapter(List<DataItem> items) {
        this.items.addAll(items);
    }

    @Override
    public void removeAt(int position) {
        items.remove(position);
        notifyDataSetChanged();
    }

    @Override
    public void remove(DataItem item) {
        items.remove(item);
        notifyDataSetChanged();
    }

    @Override
    public void add(DataItem item) {
        items.add(item);
        notifyDataSetChanged();
    }

    @Override
    public void addAt(int position, DataItem item) {
        items.add(position, item);
        notifyDataSetChanged();
    }

    public int getPositionForItem(DataItem item){
        return items.indexOf(item);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public DataItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.icon.setBackgroundResource(R.mipmap.ic_launcher);
        holder.content.setText(items.get(position).data);
        return convertView;
    }

    class ViewHolder{
        ImageView icon;
        TextView content;

        public ViewHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.icon);
            content = (TextView) view.findViewById(R.id.content);
        }
    }
}
