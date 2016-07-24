# ItemAnimatableListView

# 概要

这个组件可以让你的 ListView/ExpandableListView 的 ItemView 在移除或者添加的时候，整个列表有一个比较自然连贯的动画效果。那我们废话不多说，直接上效果图：

![](https://leanote.com/api/file/getImage?fileId=57937facab644135ea01f934)

如上图，大家可以清楚的看到，如果要删除一个 Item，这个 Item 的 View 会淡出，同时其他 ItemView 也会以动画的形式进行位置调整；同样的，如果添加一个 Item，它对应的 View 会淡入，其他的 ItemView 则会动画给他腾出位置。当然，被添加和被删除的 ItemView 的转场动画其实是可以自定义的，你也可以让他从左往右，也可以让他从小变大，随你。

**当然，要知道 RecyclerView 本身就支持了 ItemAnimator 的，如果是新项目，建议直接使用 RecyclerView。**

# 如何使用

使用方法非常简单，首先到 GitHub 下载我们的组件:[ItemAnimatableListView](https://github.com/enbandari/ItemAnimatableListView)， app 模块是一个 demo，library 模块就是我们的组件之所在了。

依赖 library 模块，就像我们在 demo 当中的那样：

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout android:id="@+id/parent"
                xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:onClick="onClick"
                android:orientation="vertical">

    <net.println.itemanimatable.ItemAnimatableListView
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="50dp"
        android:clipChildren="false"
        android:clipToPadding="true"/>

</RelativeLayout>
```

接着继承 ```AnimatableListAdapter``` 实现我们自己的 Adapter，这个抽象 Adapter 类其实主要提供了一些接口方法供 ```ItemAnimatableListView``` 在指定动画移除和动画添加时使用。

```java
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
```

然后，在 ```Activity``` 中，我们只需要调用：

```java
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
```

如果你直接调用 ```listView.animateRemove(position)``` ，这时被移除的 ItemView 立即消失，其他 ItemView 动画移动到新位置；如果传入动画，这时候被移除的 ItemView 会执行传入的 Animator，其他的不变。

那么你可能会问，为什么不直接支持传入 ```position``` ， 而传入了一个 ```PositionGetter``` ？这里的原因在于，传入的 Animator 播放时，```ListView``` 的 Item 可能会发生变化，那么这时候被删除的 Item 对应的位置也有可能是变了的，换句话说，我们只能在任意一个时刻动态的判断要删除的是哪一个，而不能直接传入删除方法调用时传入的位置。


**ExpandableListView 本质上就是一个 ListView，其对应的操作与上面提到的相类似，这里就不在赘述。**

# 原理介绍

这个组件的思路和原型源自于 StackOverflow：[Android listview row delete animation](http://stackoverflow.com/questions/17857775/android-listview-row-delete-animation)，基本思路就是在添加 Item 到 Adapter 时，记录当前所有的显示中的 View 对应的 Item 的 Id，等到重新布局结果出来之后，也就是在 preDraw 回调之时，根据刚才保存的 Id 去找到将要被绘制的这些 ItemView 之前在哪里，然后做一个平移动画。这个思路涉及到较多绘制布局的知识，篇幅原因我就不再展开了。

但是，这并不是一个完美的方案：

![](https://leanote.com/api/file/getImage?fileId=57941b2eab644135ea01fc9e)

请大家仔细观察，我们在删除一个 Item 的时候，最后一个 Item 『9』会先消失，然后又出现，这是为什么呢？

抛开我们的动画效果不谈，如果是正常删除一个 Item，这时候 ListView 发现 Item 少了一个，那么会在下一次布局直接调整自己的高度——当然，前提是 ListView 的 height 为 ```WrapContent```——也就是说，就在那一瞬间，『9』这个 Item 没有藏身之地了，它跑到了父 View 也就是  ListView 的外面。

那我们怎么办呢？

```java
...
        boolean needAdjust = false;
        if (!canScrollVertically(-1) && !canScrollVertically(1) &&  mAdapter.getCount() > 0) {
            needAdjust = true;

            /* >benny: [16-07-20 10:25] 首先在这里我们通过设置 paddingBottom, 骗ListView在measure的时候大小保持不变 */
            /* >benny: [16-07-20 10:25] 不过这并不能保证最后一个 Item 正常绘制出来,  paddingBottom 的区域是不会绘制子View的 */
            savedPaddingBottoms.push(getPaddingBottom());
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getChildAt(0).getHeight() + getDividerHeight());
            Log.d(TAG, "height = " + getHeight());
            addOnMeasuredListener(new OnMeasuredListener() {
                @Override
                public void onMeasured() {
                    removeOnMeasuredListener(this);
                    if (getChildCount() > 0) {
                        Log.d(TAG, "onMeasured = " + getHeight());
                        /* >benny: [16-07-20 10:26] 为了让最后一个子View在 paddingBottom 的区域绘制出来,那么就要关闭这个 */
                        setClipToPadding(false);
                    }
                }
            });
        }
final boolean finalNeedAdjust = needAdjust;
...
```

就像注释当中写到的，我们会先保存原来的 ```paddingBottom```，紧接着把它设置为一个 Item + Divider 的高度，那么 ListView 在布局时，就会布局为下面这个样子：

![](https://leanote.com/api/file/getImage?fileId=57941cfcab644133ed0204df)

这时候，我们之前遇到的 ListView 会变小就不再是问题了。那，更进一步，如果这时候，paddingBottom 的位置是可以绘制子 View 的，那么上面的问题不就彻底解决了么？

蛤，我闻到了欺骗的味道：我们骗 ListView 在布局的时候多了块儿 PaddingBottom 以保持大小不变，然后又过去骗 Canvas 绘制的时候 PaddingBottom 那块儿区域也是需要绘制的——哦，我忘了交代，clipToPadding 为 true 的话，paddingBottom 的区域是不应该绘制任何东西的。

![](https://leanote.com/api/file/getImage?fileId=579420f5ab644133ed020502)

**骗得了吕奉先，骗不了陈公台呀**

怎么骗呢，这是个问题。我们要赶在 dispatchDraw 之前偷偷把 clipToPadding 给改了才行，于是我想到了 onMeasure 这个方法。它调用的时机其实就是 measure 之时，我们在 measure 结束之后偷偷关了 clipToPadding 之后，dispatchDraw 的时候就以为多出来的那块儿 paddingBottom 并不是 paddingBottom，傻乎乎的把子 View 绘制上去。于是我们的问题就解决了。

![](https://leanote.com/api/file/getImage?fileId=57942499ab644135ea01fd40)

当然，在这一套动画结束之时，还需要恢复之前的设置。

其实在添加 Item 的时候，也有类似的问题：

![](https://leanote.com/api/file/getImage?fileId=579425e6ab644133ed020540)

红色区域是 ListView 的范围，当 ListView 的高度达到了最大，最后一个 Item 又恰好在最下面，这时候上面添加一个 Item 会把下面的『挤下去』，可最后一个 Item 却直接消失了，熟悉 ListView 布局的朋友肯定就想到这个 Item 肯定是被回收了。那么具体要布局哪些 ItemView，我们发现取决于 ListView 的这个方法：

```java
    private View fillDown(int pos, int nextTop) {
        View selectedView = null;

        int end = (mBottom - mTop);
        if ((mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            end -= mListPadding.bottom;
        }

        while (nextTop < end && pos < mItemCount) {
            // is this the selected item?
            boolean selected = pos == mSelectedPosition;
            View child = makeAndAddView(pos, nextTop, true, mListPadding.left, selected);

            nextTop = child.getBottom() + mDividerHeight;
            if (selected) {
                selectedView = child;
            }
            pos++;
        }

        setVisibleRangeHint(mFirstPosition, mFirstPosition + getChildCount() - 1);
        return selectedView;
    }
```

我们看到，计算究竟布局多少个 ItemView，其实主要取决于 end 的值，而决定它的值的有两个，一个是 Listview 的高度，一个是 mListPadding.bottom —— 而后者，其实就是由 paddingBottom 计算而来的，所以我们只需要在合适的时机偷偷改掉这个值，就能让 ListView 在布局的时候把最后一个添加上：

```java
boolean needAdjust = false;
if (mAdapter.getCount() > 0 && (canScrollVertically(-1) || canScrollVertically(1))) {
    needAdjust = true;

    addOnMeasuredListener(new OnMeasuredListener() {
        @Override
        public void onMeasured() {
            removeOnMeasuredListener(this);
            if (getChildCount() > 0) {
                setClipToPadding(true);
                savedPaddingBottoms.push(getPaddingBottom());
                setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), - getChildAt(0).getHeight() - getDividerHeight());
            }
        }
    });
}
```
当然别忘了最后恢复这个值啊哈。与之前 animateRemove 不同的是，这里由于要借助 paddingBottom 来布局，所以一定要设置 clipToPadding 为 true。

**这个组件封装的过程中，主要的坑就这个，其他的很细节的点这里就不一一展开。如果有相应的需求，仍然建议优选 RecyclerView，这个组件毕竟 Hack 的成分较多。**