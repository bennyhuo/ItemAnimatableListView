package net.println.itemanimatable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.AbsListView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * Created by benny on 7/19/16.
 */
public class ItemAnimatableExpandableListView extends ExpandableListView {
    public static final String TAG = "ItemAnimatableListView";

    private static final int MOVE_DURATION = 150;

    private static final int ACTION_ADD = 0;
    private static final int ACTION_REMOVE = 1;

    private ArrayList<OnChildMoveToHeapListener> onChildMoveToHeapListeners = new ArrayList<OnChildMoveToHeapListener>();

    private HashSet<View> viewsToDelete = new HashSet<View>();

    private Stack<Integer> savedPaddingBottoms = new Stack<Integer>();

    private ArrayList<OnMeasuredListener> onMeasuredListeners = new ArrayList<>();

    private OnItemAnimationListener listener;

    private AnimatableExpandableListAdapter mAdapter;

    public interface PositionGetter{
        int[] getUpdatedPosition();
    }

    public interface OnItemAnimationListener{
        void onAnimationStart(int action, int position);
        void onAnimationEnd(int action, int position);
    }

    private interface OnMeasuredListener{
        void onMeasured();
    }

    private interface OnChildMoveToHeapListener{
        void onChildMoveToHeap(View child);
    }


    public ItemAnimatableExpandableListView(Context context) {
        super(context);
        init();
    }

    public ItemAnimatableExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemAnimatableExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){

    }

    @Override
    public void setAdapter(ExpandableListAdapter adapter) {
        super.setAdapter(adapter);
        mAdapter = (AnimatableExpandableListAdapter) adapter;
    }

    public ItemAnimatableExpandableListView listener(OnItemAnimationListener listener) {
        this.listener = listener;
        return this;
    }

    private void addOnChildMoveToHeapListener(OnChildMoveToHeapListener onChildMoveToHeapListener){
        this.onChildMoveToHeapListeners.add(onChildMoveToHeapListener);
    }

    private void removeOnChildMoveToHeapListener(OnChildMoveToHeapListener onChildMoveToHeapListener){
        this.onChildMoveToHeapListeners.remove(onChildMoveToHeapListener);
    }

    private void addOnMeasuredListener(OnMeasuredListener onMeasuredListener){
        this.onMeasuredListeners.add(onMeasuredListener);
    }

    private void removeOnMeasuredListener(OnMeasuredListener onMeasuredListener){
        this.onMeasuredListeners.remove(onMeasuredListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ArrayList<OnMeasuredListener> onMeasuredListenerArrayList = (ArrayList<OnMeasuredListener>) onMeasuredListeners.clone();
        for (OnMeasuredListener onMeasuredListener : onMeasuredListenerArrayList) {
            onMeasuredListener.onMeasured();
        }
    }

    private long getItemId(int flatPosition){
        long packedPos = getExpandableListPosition(flatPosition);
        int itemType = ExpandableListView.getPackedPositionType(packedPos);
        long itemId;
        if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            itemId = mAdapter.getGroupId(ExpandableListView.getPackedPositionGroup(packedPos));
        } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            itemId = mAdapter.getChildId(ExpandableListView.getPackedPositionGroup(packedPos), ExpandableListView.getPackedPositionChild(packedPos));
        } else {
            itemId = Integer.MIN_VALUE;
        }
        return itemId;
    }

    private int getFlatPosition(int groupPosition, int childPosition){
        int flatPosition;
        if(childPosition == -1){
            flatPosition = getFlatListPosition(getPackedPositionForGroup(groupPosition));
        }else{
            flatPosition = getFlatListPosition(getPackedPositionForChild(groupPosition, childPosition));
        }
        return flatPosition;
    }

    private void setListPaddingBottom(int paddingBottom){
        try{
            Field field = AbsListView.class.getDeclaredField("mListPadding");
            field.setAccessible(true);
            Rect rect = (Rect) field.get(this);
            rect.bottom = paddingBottom;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void animateAdd(final int groupPosition, final int childPosition, Object item){
        Log.e(TAG, "animateAdd() called with: " + "groupPosition = [" + groupPosition + "], childPosition = [" + childPosition + "], item = [" + item + "]");
        animateAdd(groupPosition, childPosition, item, ObjectAnimator.ofFloat(null, "alpha", 0, 1).setDuration(200));
    }

    public void animateAdd(final int groupPosition, final int childPosition, Object item, final ValueAnimator animatorIn){
        if((childPosition >= 0 && isGroupExpanded(groupPosition)) || (childPosition == -1 &&groupPosition != -1)) {
            int firstVisiblePosition = getFirstVisiblePosition();
            //if(position >= firstVisiblePosition && position < firstVisiblePosition + getChildCount()) {
            final HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                int visiblePosition = firstVisiblePosition + i;
                long itemId = getItemId(visiblePosition);
                mItemIdTopMap.put(itemId, child.getTop());
                Log.d(TAG, child.getTop() + "; " + itemId);
            }

            boolean needAdjust = false;
//            if (mAdapter.getCount() - firstVisiblePosition >= getChildCount()) {
            needAdjust = true;
//            savedPaddingBottoms.push(getPaddingBottom());
//            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), -getChildAt(0).getHeight() / 2);

            addOnMeasuredListener(new OnMeasuredListener() {
                @Override
                public void onMeasured() {
                    removeOnMeasuredListener(this);
                    if (getChildCount() > 0) {
                        Log.d(TAG, "onMeasured = " + getHeight());
                        savedPaddingBottoms.push(getListPaddingBottom());
                        setListPaddingBottom(-getChildAt(0).getHeight());
                        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), 1);
//                        savedPaddingBottoms.push(getPaddingBottom());
//                        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), -10);
                    }
                }
            });
//            }
            final boolean finalNeedAdjust = needAdjust;
            final ViewTreeObserver observer = getViewTreeObserver();

            Log.d(TAG, "before predraw height = " + getHeight());
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);
                    Log.d(TAG, "predraw height = " + getHeight());
                    int newItemOffset = 0;
                    boolean firstAnimation = true;
                    int firstVisiblePosition = getFirstVisiblePosition();
                    View viewAdded = getChildAt(getFlatPosition(groupPosition, childPosition) - firstVisiblePosition);
                    for (int i = 0; i < getChildCount(); ++i) {
                        final View child = getChildAt(i);
                        if (viewAdded == child) {
                            if (animatorIn != null) {
                                animatorIn.setTarget(child);
                                animatorIn.setCurrentPlayTime(0);
                                animatorIn.setStartDelay(MOVE_DURATION);
                                animatorIn.start();
                            }
                        } else {
                            final int childPosition = firstVisiblePosition + i;
                            long itemId = getItemId(childPosition);
                            Integer startTop = mItemIdTopMap.get(itemId);
                            int top = child.getTop();

                            if (startTop != null) {
                                startTop += newItemOffset;
                                Log.d(TAG, "top: " + top + "; startTop: " + startTop + "; itemId: " + itemId);
                                if (startTop != top) {
                                    int delta = startTop - top;
                                    child.setTranslationY(delta);
                                    child.animate().setDuration(MOVE_DURATION).translationY(0);
                                    if (firstAnimation) {
                                        child.animate().setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);

                                            /* >benny: [16-07-19 09:36] 我们需要把 paddingBottom恢复, 不然你就看不到最后一个 item 了 */
                                                if (finalNeedAdjust) {
//                                                setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                                                    setListPaddingBottom(savedPaddingBottoms.pop());
                                                    requestLayout();
                                                }
                                                if (listener != null) {
                                                    listener.onAnimationEnd(ACTION_ADD, groupPosition);
                                                }
                                                Log.d(TAG, "onAnimationEnd() called with: " + "child = [" + child + "]");
                                                child.animate().setListener(null);
                                            }

                                            @Override
                                            public void onAnimationStart(Animator animation) {
                                                super.onAnimationStart(animation);
                                                if (listener != null) {
                                                    listener.onAnimationStart(ACTION_ADD, groupPosition);
                                                }
                                            }
                                        });
                                        firstAnimation = false;
                                    }
                                }
                            } else {
                                // Animate new views along with the others. The catch is that they did not
                                // exist in the start state, so we must calculate their starting position
                                // based on neighboring views.
                                int childHeight = child.getHeight() + getDividerHeight();
                                startTop = top + (i > 0 ? childHeight : -childHeight);
                                startTop += newItemOffset;
                                Log.d(TAG, "startTop null .top: " + top + "; startTop: " + startTop + "; itemId: " + itemId);
                                /* >benny: [16-07-19 15:30] newItemOffset 主要在 remove中解决移除的一瞬间又添加了item的问题.在这里似乎没什么用 */
//                                if(viewAdded != null && i != 0) {
//                                    newItemOffset += childHeight;
//                                }
                                int delta = startTop - top;
                                child.setTranslationY(delta);
                                child.animate().setDuration(MOVE_DURATION).translationY(0);
                                if (firstAnimation) {
                                    child.animate().setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);

                                        /* >benny: [16-07-19 09:36] 我们需要把 paddingBottom恢复, 不然你就看不到最后一个 item 了 */
                                            if (finalNeedAdjust) {
//                                            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                                                setListPaddingBottom(savedPaddingBottoms.pop());
                                                requestLayout();
                                            }

                                            if (listener != null) {
                                                listener.onAnimationEnd(ACTION_ADD, groupPosition);
                                            }
                                            Log.d(TAG, "onAnimationEnd() called with: " + "child = [" + child + "]");
                                            child.animate().setListener(null);
                                        }

                                        @Override
                                        public void onAnimationStart(Animator animation) {
                                            super.onAnimationStart(animation);
                                            if (listener != null) {
                                                listener.onAnimationStart(ACTION_ADD, groupPosition);
                                            }
                                        }
                                    });
                                    firstAnimation = false;
                                }
                            }
                        }
                    }
                    mItemIdTopMap.clear();
                    return true;
                }
            });
            //}
        }
        if(childPosition >= 0) {
            mAdapter.addChildToGroupAt(item, groupPosition, childPosition);
        }else{
            mAdapter.addGroupAt(item, groupPosition);
        }
    }

    /**
     * 为移除的 Item 加个动画效果.
     * @param getter 在这个动画效果播放的前后,我们想要移除的 item 的 Position 并不是一成不变的, 必须提供一种可以动态获取
     *               其正确位置的方法.
     * @param animatorOut 不要总是传入同一个动画实例
     */
    public void animateRemove(final PositionGetter getter, final ValueAnimator animatorOut){
        final int[] position = getter.getUpdatedPosition();
        long packedPosition;
        if(position[1] == -1){
            packedPosition = getPackedPositionForGroup(position[0]);
        }else{
            packedPosition = getPackedPositionForChild(position[0], position[1]);
        }
        final int flatPosition = getFlatListPosition(packedPosition);
        final View viewToRemove = getChildAt(flatPosition - getFirstVisiblePosition());
        Log.d(TAG, "viewToRemove = " + viewToRemove + "; position = " + position[0] + ", " + position[1]);
        /* >benny: [16-07-18 12:14] 避免重复提交 */
        if(!viewsToDelete.add(viewToRemove)) return;

        animatorOut.setTarget(viewToRemove);
        /* >benny: [16-07-18 10:05] 这样保证动画起始的状态与我们的View当前状态一致 */
        animatorOut.setupStartValues();
        animatorOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                viewsToDelete.remove(viewToRemove);
                animatorOut.setCurrentPlayTime(0);

                int[] position = getter.getUpdatedPosition();
                if (position[0] == -1 && position[1] == -1) return;
                animateRemove(position[0], position[1]);
            }
        });
        animatorOut.addUpdateListener(new AnimatorUpdateListener() {
            long itemId = getItemId(flatPosition);

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                ViewGroup.LayoutParams params = viewToRemove.getLayoutParams();
                if (params != null && params instanceof LayoutParams) {
                    LayoutParams layoutParams = ((LayoutParams) params);
                    try {
                        /* >benny: [16-07-18 12:15] 使用反射,还是小心一点儿 */
                        Field f = LayoutParams.class.getDeclaredField("itemId");
                        f.setAccessible(true);
                        if (f.getLong(layoutParams) != itemId) {
                            animation.removeUpdateListener(this);
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    animation.cancel();
                                }
                            });

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        animatorOut.start();

        /* >benny: [16-07-18 10:04] 如果View被回收的话,那么动画需要停止,并且将View的属性置为初始状态 */
        /* >benny: [16-07-18 11:56] 实践证明,这个方法...可能会有问题 */
//        addOnChildMoveToHeapListener(new OnChildMoveToHeapListener() {
//            @Override
//            public void onChildMoveToHeap(View child) {
//                if(child == viewToRemove){
//                    Log.d(TAG, "onChildMoveToHeap() called with: " + "child = [" + child + "]");
////                    removeOnChildMoveToHeapListener(this);
////                    animatorOut.cancel();
////                    animatorOut.setCurrentPlayTime(0);
//                }
//            }
//        });
    }

    public void animateRemove(View viewToRemove) {
        /* >benny: [16-07-18 12:16] 带动画效果的方法会有延时, 不要跟它冲突,以免发生问题 */
        if(!viewsToDelete.add(viewToRemove)) return;
        int position = getPositionForView(viewToRemove);
        long packedPos = getExpandableListPosition(position);
        int groupPosition = ExpandableListView.getPackedPositionGroup(packedPos);
        int childPosition = ExpandableListView.getPackedPositionChild(packedPos);
        animateRemove(groupPosition, childPosition);
    }

    public void animateRemove(final int groupPosition, final int childPosition){
        final HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
        int firstVisiblePosition = getFirstVisiblePosition();
        for (int i = 0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            int visiblePosition = firstVisiblePosition + i;
            long itemId = getItemId(visiblePosition);
            mItemIdTopMap.put(itemId, child.getTop());
        }

        /* >benny: [16-07-20 10:25] 首先在这里我们通过设置 paddingBottom, 骗ListView在measure的时候大小保持不变 */
        /* >benny: [16-07-20 10:25] 不过这并不能保证最后一个 Item 正常绘制出来,  paddingBottom 的区域是不会绘制子View的 */
        savedPaddingBottoms.push(getPaddingBottom());
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getChildAt(getChildCount() - 1).getHeight() + getDividerHeight());
        Log.d(TAG, "height = " + getHeight());
        addOnMeasuredListener(new OnMeasuredListener() {
            @Override
            public void onMeasured() {
                removeOnMeasuredListener(this);
                if (getChildCount() > 0) {
                    Log.d(TAG, "onMeasured = " + getHeight());
                    /* >benny: [16-07-20 10:26] 为了让最后一个子View在 paddingBottom 的区域绘制出来,那么就要关闭这个 */
                    setClipToPadding(false);

                        /* >benny: [16-07-20 10:27] 下面这个方法其实也可以, 不过反射还是有风险. */
//                        try {
//                            Field field = View.class.getDeclaredField("mPaddingBottom");
//                            field.setAccessible(true);
//                            Log.d(TAG, "field.getInt(this) = " + field.getInt(ItemAnimatableListView.this));
//                            field.setInt(ItemAnimatableListView.this, 0);
//                            //setClipToPadding(false);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                }
            }
        });
        final boolean finalNeedAdjust = true;

        if(childPosition == -1){
            mAdapter.removeGroupAt(groupPosition);
        }else{
            mAdapter.removeChildAt(groupPosition, childPosition);
        }

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                int newItemOffset = 0;
                boolean firstAnimation = true;
                int firstVisiblePosition = getFirstVisiblePosition();
                int splitter = getFlatPosition(groupPosition, childPosition) - firstVisiblePosition;
                Log.d(TAG, "splitter = " + splitter);
                if (splitter < 0) return true;
                for (int i = splitter - 1; i >= 0 && i < getChildCount(); i--) {
                    final View child = getChildAt(i);
                    final int position = firstVisiblePosition + i;
                    long itemId = getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop != null) {
                        startTop += newItemOffset;
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation) {
                                child.animate().setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (listener != null) {
                                            listener.onAnimationEnd(ACTION_REMOVE, position);
                                        }

                                        if (finalNeedAdjust) {
                                            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                                            requestLayout();
                                        }

                                        child.animate().setListener(null);
                                    }

                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                        super.onAnimationStart(animation);
                                        if (listener != null) {
                                            listener.onAnimationStart(ACTION_REMOVE, position);
                                        }
                                    }
                                });
                                firstAnimation = false;
                            }
                        }
                    } else {
                        int childHeight = child.getHeight() + getDividerHeight();
                        startTop = top - childHeight;
                        startTop += newItemOffset;
                        newItemOffset -= childHeight;
                        int delta = startTop - top;
                        child.setTranslationY(delta);
                        child.animate().setDuration(MOVE_DURATION).translationY(0);
                        if (firstAnimation) {
                            child.animate().setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    if (listener != null) {
                                        listener.onAnimationEnd(ACTION_REMOVE, position);
                                    }

                                    if (finalNeedAdjust) {
                                        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                                        requestLayout();
                                    }

                                    child.animate().setListener(null);
                                }

                                @Override
                                public void onAnimationStart(Animator animation) {
                                    super.onAnimationStart(animation);
                                    if (listener != null) {
                                        listener.onAnimationStart(ACTION_REMOVE, position);
                                    }
                                }
                            });
                            firstAnimation = false;
                        }
                    }
                }

                newItemOffset = 0;
                for (int i = splitter; i >= 0 && i < getChildCount(); ++i) {
                    final View child = getChildAt(i);
                    final int position = firstVisiblePosition + i;
                    long itemId = getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop != null) {
                        startTop += newItemOffset;
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation) {
                                child.animate().setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        if (listener != null) {
                                            listener.onAnimationEnd(ACTION_REMOVE, position);
                                        }

                                        if (finalNeedAdjust) {
                                            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                                            requestLayout();
                                        }

                                        child.animate().setListener(null);
                                    }

                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                        super.onAnimationStart(animation);
                                        if (listener != null) {
                                            listener.onAnimationStart(ACTION_REMOVE, position);
                                        }
                                    }
                                });
                                firstAnimation = false;
                            }
                        }
                    } else {
                        int childHeight = child.getHeight() + getDividerHeight();
                        startTop = top + childHeight;
                        startTop += newItemOffset;
                        newItemOffset += childHeight;
                        int delta = startTop - top;
                        child.setTranslationY(delta);
                        child.animate().setDuration(MOVE_DURATION).translationY(0);
                        if (firstAnimation) {
                            child.animate().setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    if (listener != null) {
                                        listener.onAnimationEnd(ACTION_REMOVE, position);
                                    }

                                    if (finalNeedAdjust) {
                                        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                                        requestLayout();
                                    }

                                    child.animate().setListener(null);
                                }

                                @Override
                                public void onAnimationStart(Animator animation) {
                                    super.onAnimationStart(animation);
                                    if (listener != null) {
                                        listener.onAnimationStart(ACTION_REMOVE, position);
                                    }
                                }
                            });
                            firstAnimation = false;
                        }
                    }
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }

    public void animateExpand(final int group){
        ExpandableListAdapter adapter = getExpandableListAdapter();
        int childCount = adapter.getChildrenCount(group);
        if(childCount == 0){
            expandGroup(group);
            return;
        }
        final boolean isEnabled = isEnabled();
        setEnabled(false);
        final HashMap<Long, Integer> mItemIdTopMap = new HashMap<>();
        int firstVisiblePosition = getFirstVisiblePosition();
        for (int i = 0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            int visiblePosition = firstVisiblePosition + i;
            long itemId = getItemId(visiblePosition);
            mItemIdTopMap.put(itemId, child.getTop());
        }
        expandGroup(group);
        getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                int position = getFlatPosition(group, -1);
                View groupView = getChildAt(position - getFirstVisiblePosition());
                ExpandableListAdapter adapter = getExpandableListAdapter();
                int childCount = adapter.getChildrenCount(group);
                boolean shouldAddCallback = true;
                for (int i = 1; i <= childCount; i++) {
                    int childIndex = position - getFirstVisiblePosition() + i;
                    if(childIndex == getChildCount()) break;
                    View childView = getChildAt(childIndex);
                    Animator animator = ObjectAnimator.ofFloat(childView, "translationY", groupView.getTop() - childView.getTop(), 0);
                    if(shouldAddCallback){
                        shouldAddCallback = false;
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                setEnabled(isEnabled);
                            }
                        });
                    }
                    animator.start();
                }

                for (int i = position + getFirstVisiblePosition() + childCount + 1; i < getChildCount(); i++) {
                    long id = getItemId(i);
                    View childView = getChildAt(i);
                    Integer oldTop = mItemIdTopMap.get(id);
                    if(oldTop != null)
                        ObjectAnimator.ofFloat(childView, "translationY",  oldTop - childView.getTop(), 0).start();
                }

                getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
    }

    public void animateCollapse(final int group){
        ExpandableListAdapter adapter = getExpandableListAdapter();
        int childCount = adapter.getChildrenCount(group);
        if(childCount == 0){
            collapseGroup(group);
            return;
        }
        final boolean isEnabled = isEnabled();
        setEnabled(false);
        final int position = getFlatPosition(group, -1);
        View groupView = getChildAt(position - getFirstVisiblePosition());
        int offset = 0;
        boolean shouldAddCallback = true;
        for (int i = 1; i <= childCount; i++) {
            int childIndex = position - getFirstVisiblePosition() + i;
            if(childIndex == getChildCount()) break;
            final View childView = getChildAt(childIndex);
            Animator animator = ObjectAnimator.ofFloat(childView, "translationY", 0, groupView.getTop() - childView.getTop());
            if(shouldAddCallback){
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        animation.removeListener(this);
                        setEnabled(isEnabled);
                        collapseGroup(group);
                    }
                });
                shouldAddCallback = false;
            }
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animation.removeListener(this);
                    childView.setTranslationY(0);
                }
            });
            animator.start();
            if(i == childCount){
                offset = childView.getBottom() - groupView.getBottom();
            }
        }

        for (int i = position - getFirstVisiblePosition() + childCount + 1; i < getChildCount(); i++) {
            final View childView = getChildAt(i);
            Animator animator = ObjectAnimator.ofFloat(childView, "translationY", 0, -offset);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animation.removeListener(this);
                    childView.setTranslationY(0);
                }
            });
            animator.start();
        }
    }
}
