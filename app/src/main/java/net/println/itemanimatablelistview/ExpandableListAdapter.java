package net.println.itemanimatablelistview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.println.itemanimatable.AnimatableExpandableListAdapter;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by benny on 7/17/16.
 */
public class ExpandableListAdapter extends AnimatableExpandableListAdapter<DataItem, DataItem> {
    public static final String TAG = "ListAdapter";

    private List<DataItem> items = new ArrayList<DataItem>();

    public ExpandableListAdapter(List<DataItem> items) {
        this.items.addAll(items);
    }

    @Override
    public void removeGroupAt(int groupPosition) {
        items.remove(groupPosition);
        notifyDataSetChanged();
    }

    @Override
    public void removeChildAt(int groupPosition, int childPosition) {
        items.get(groupPosition).removeChildAt(childPosition);
        notifyDataSetChanged();
    }

    @Override
    public void removeGroup(DataItem group) {
        items.remove(group);
        notifyDataSetChanged();
    }

    @Override
    public void removeChild(DataItem child, int groupPosition) {
        items.get(groupPosition).removeChild(child);
        notifyDataSetChanged();
    }

    @Override
    public void addGroup(DataItem group) {
        items.add(group);
        notifyDataSetChanged();
    }

    @Override
    public void addGroupAt(DataItem group, int groupPosition) {
        items.add(groupPosition, group);
        notifyDataSetChanged();
    }

    @Override
    public void addChildToGroup(DataItem child, int groupPosition) {
        items.get(groupPosition).removeChild(child);
        notifyDataSetChanged();
    }

    @Override
    public void addChildToGroupAt(DataItem child, int groupPosition, int childPosition) {
        items.get(groupPosition).addChildAt(childPosition, child);
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return items.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return items.get(groupPosition).getChildCount();
    }

    @Override
    public DataItem getGroup(int groupPosition) {
        return items.get(groupPosition);
    }

    @Override
    public DataItem getChild(int groupPosition, int childPosition) {
        return items.get(groupPosition).getChildAt(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return items.get(groupPosition).id;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return items.get(groupPosition).getChildAt(childPosition).id;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.icon.setBackgroundResource(R.mipmap.ic_launcher);
        holder.content.setText(items.get(groupPosition).data);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.icon.setBackgroundResource(0);
        holder.content.setText(items.get(groupPosition).getChildAt(childPosition).data);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    public int[] getPositionForItem(DataItem item){
        int[] position = {-1, -1};
        DataItem groupItem = item.parent == null? item : item.parent;
        DataItem childItem = item.parent == null ? null : item;
        position[0] = items.indexOf(groupItem);
        position[1] = groupItem.indexOfChild(childItem);
        return position;
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
