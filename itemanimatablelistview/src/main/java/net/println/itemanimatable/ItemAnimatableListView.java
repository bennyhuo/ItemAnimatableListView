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
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * Created by benny on 7/19/16.
 */
public class ItemAnimatableListView extends ListView {
    public static final String TAG = "ItemAnimatableListView";

    private static final int MOVE_DURATION = 150;

    private static final int ACTION_ADD = 0;
    private static final int ACTION_REMOVE = 1;

    private ArrayList<OnChildMoveToHeapListener> onChildMoveToHeapListeners = new ArrayList<OnChildMoveToHeapListener>();

    private HashSet<View> viewsToDelete = new HashSet<View>();

    private Stack<Integer> savedPaddingBottoms = new Stack<Integer>();

    private ArrayList<OnMeasuredListener> onMeasuredListeners = new ArrayList<>();

    private OnItemAnimationListener listener;

    private AnimatableListAdapter mAdapter;

    public interface PositionGetter{
        int getUpdatedPosition();
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


    public ItemAnimatableListView(Context context) {
        super(context);
        init();
    }

    public ItemAnimatableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemAnimatableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ItemAnimatableListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init(){
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        mAdapter = (AnimatableListAdapter) adapter;
    }

    public ItemAnimatableListView listener(OnItemAnimationListener listener) {
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
        Log.d(TAG, "protected onMeasure() : " + getMeasuredHeight());
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

    public void animateAdd(final int position, Object item){
        animateAdd(position, item, ObjectAnimator.ofFloat(null, "alpha", 0, 1).setDuration(200));
    }

    public void animateAdd(final int position, Object item, final ValueAnimator animatorIn){
        /* >benny: [16-07-20 10:28] 我们需要用这个来欺骗 ListView 在布局的时候多添加一个Item. 这个Item其实最后会移除屏幕外 */

        int firstVisiblePosition = getFirstVisiblePosition();
        //if(position >= firstVisiblePosition && position < firstVisiblePosition + getChildCount()) {
            final HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                int visiblePosition = firstVisiblePosition + i;
                long itemId = mAdapter.getItemId(visiblePosition);
                mItemIdTopMap.put(itemId, child.getTop());
                Log.d(TAG, child.getTop() + "; " + itemId);
            }
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

            final boolean finalNeedAdjust = needAdjust;
            final ViewTreeObserver observer = getViewTreeObserver();

            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);
                    int newItemOffset = 0;
                    boolean firstAnimation = true;
                    int firstVisiblePosition = getFirstVisiblePosition();
                    View viewAdded = getChildAt(position - firstVisiblePosition);
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
                            long itemId = mAdapter.getItemId(childPosition);
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
                                                    setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
//                                                    setListPaddingBottom(savedPaddingBottoms.pop());
                                                    requestLayout();
                                                }
                                                if (listener != null) {
                                                    listener.onAnimationEnd(ACTION_ADD, position);
                                                }
                                                Log.d(TAG, "onAnimationEnd() called with: " + "child = [" + child + "]");
                                                child.animate().setListener(null);
                                            }

                                            @Override
                                            public void onAnimationStart(Animator animation) {
                                                super.onAnimationStart(animation);
                                                if (listener != null) {
                                                    listener.onAnimationStart(ACTION_ADD, position);
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
                                                setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
//                                                setListPaddingBottom(savedPaddingBottoms.pop());
                                                requestLayout();
                                            }

                                            if (listener != null) {
                                                listener.onAnimationEnd(ACTION_ADD, position);
                                            }
                                            Log.d(TAG, "onAnimationEnd() called with: " + "child = [" + child + "]");
                                            child.animate().setListener(null);
                                        }

                                        @Override
                                        public void onAnimationStart(Animator animation) {
                                            super.onAnimationStart(animation);
                                            if (listener != null) {
                                                listener.onAnimationStart(ACTION_ADD, position);
                                            }
                                        }
                                    });
                                    firstAnimation = false;
                                }
                            }
                        }
                    }
                    //表示没有 Item 需要动画
                    if(firstAnimation){
                        if (finalNeedAdjust) {
                            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                            requestLayout();
                        }
                    }
                    mItemIdTopMap.clear();
                    return true;
                }
            });
        //}
        mAdapter.addAt(position, item);
    }

    /**
     * 为移除的 Item 加个动画效果.
     * @param getter 在这个动画效果播放的前后,我们想要移除的 item 的 Position 并不是一成不变的, 必须提供一种可以动态获取
     *               其正确位置的方法.
     * @param animatorOut 不要总是传入同一个动画实例
     */
    public void animateRemove(final PositionGetter getter, final ValueAnimator animatorOut){
        final int position = getter.getUpdatedPosition();
        final View viewToRemove = getChildAt(position - getFirstVisiblePosition());
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
                if (getter.getUpdatedPosition() == -1) return;
                animateRemove(getter.getUpdatedPosition());
            }
        });
        animatorOut.addUpdateListener(new AnimatorUpdateListener() {
            long itemId = mAdapter.getItemId(position);

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                ViewGroup.LayoutParams params = viewToRemove.getLayoutParams();
                if (params != null && params instanceof AbsListView.LayoutParams) {
                    AbsListView.LayoutParams layoutParams = ((AbsListView.LayoutParams) params);
                    try {
                        /* >benny: [16-07-18 12:15] 使用反射,还是小心一点儿 */
                        Field f = AbsListView.LayoutParams.class.getDeclaredField("itemId");
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
        animateRemove(position);
    }

    public void animateRemove(final int position){
        final HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
        int firstVisiblePosition = getFirstVisiblePosition();
        for (int i = 0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            int visiblePosition = firstVisiblePosition + i;
            long itemId = mAdapter.getItemId(visiblePosition);
            mItemIdTopMap.put(itemId, child.getTop());
        }

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
        }
        final boolean finalNeedAdjust = needAdjust;

        mAdapter.remove(mAdapter.getItem(position));
        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                Log.d(TAG, "onPreDraw = " + getHeight());
                int newItemOffset = 0;
                boolean firstAnimation = true;
                int firstVisiblePosition = getFirstVisiblePosition();
                int splitter = position - firstVisiblePosition;
                Log.d(TAG, "splitter = " + splitter);
                if(splitter < 0) return true;
                for(int i = splitter - 1; i >= 0 && i < getChildCount(); i--){
                    final View child = getChildAt(i);
                    final int position = firstVisiblePosition + i;
                    long itemId = mAdapter.getItemId(position);
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
                                        Log.d(TAG, "onAnimationEnd = " + getHeight());
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
                                        Log.d(TAG, "onAnimationStart = " + getHeight());
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
                                    Log.d(TAG, "onAnimationEnd = " + getHeight());
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
                                    Log.d(TAG, "onAnimationStart = " + getHeight());
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
                    long itemId = mAdapter.getItemId(position);
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
                                        Log.d(TAG, "onAnimationEnd = " + getHeight());
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
                                        Log.d(TAG, "onAnimationStart = " + getHeight());
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
                                    Log.d(TAG, "onAnimationEnd = " + getHeight());
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
                                    Log.d(TAG, "onAnimationStart = " + getHeight());
                                    if (listener != null) {
                                        listener.onAnimationStart(ACTION_REMOVE, position);
                                    }
                                }
                            });
                            firstAnimation = false;
                        }
                    }
                }
                //表示没有 Item 需要动画
                if(firstAnimation){
                    if (finalNeedAdjust) {
                        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), savedPaddingBottoms.pop());
                        requestLayout();
                    }
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }
}
