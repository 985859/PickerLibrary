package com.picker.api;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import kotlin.Metadata;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;

public class PickerLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {

    public static final int HORIZONTAL = RecyclerView.HORIZONTAL;
    public static final int VERTICAL = RecyclerView.VERTICAL;

    private static final int FILL_START = -1;
    private static final int FILL_END = 1;

    private static final String TAG = "PickerLayoutManager";

    private static final boolean DEBUG = true;


    public static final int ORIENTATION = VERTICAL;
    public static final int VISIBLE_COUNT = 3;
    public static final boolean IS_LOOP = false;
    public static final float SCALE_X = 1.0f;
    public static final float SCALE_Y = 1.0f;
    public static final float ALPHA = 1.0f;

    public int orientation;
    public int visibleCount;
    public boolean isLoop;
    public float scaleX;
    public float scaleY;
    public float alpha;

    /**
     * 保存下item的width和height‘’
     **/
    private int mPendingFillPosition = RecyclerView.NO_POSITION;

    /**
     * 保存下item的width和height‘
     **/
    private int mItemWidth = 0;
    private int mItemHeight = 0;

    /**
     * 将要滚到的position
     **/
    private int mPendingScrollToPosition = RecyclerView.NO_POSITION;

    /**
     * 要回收的View先缓存起来
     **/
    private HashSet<View> mRecycleViews = new HashSet<>();
    private final LinearSnapHelper mSnapHelper;
    /**
     * 选中中间item的监听器的集合
     **/
    private final Set mOnItemSelectedListener;
    /**
     * 子view填充或滚动监听器的集合
     **/
    private final Set mOnItemFillListener;
    private OrientationHelper mOrientationHelper;

    /**
     * @param orientation  摆放子View的方向
     * @param visibleCount 显示多少个子View
     * @param isLoop       是否支持无限滚动
     * @param scaleX       x轴缩放的比例
     * @param scaleY       y轴缩放的比例
     * @param alpha        未选中item的透明度
     */
    public PickerLayoutManager(int orientation, int visibleCount, boolean isLoop, float scaleX, float scaleY, float alpha) {
        this.orientation = orientation;
        this.visibleCount = visibleCount;
        this.isLoop = isLoop;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.alpha = alpha;
        mSnapHelper = new LinearSnapHelper();
        mOnItemFillListener = new HashSet();
        mOnItemSelectedListener = new HashSet();
        if (orientation == HORIZONTAL) {
            mOrientationHelper = OrientationHelper.createHorizontalHelper(this);
        } else {
            mOrientationHelper = OrientationHelper.createVerticalHelper(this);
        }
        if (visibleCount % 2 == 0) {
            visibleCount += 1;
        }
    }


    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (orientation == HORIZONTAL) {
            return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.MATCH_PARENT);
        } else {
            return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return false;
    }

    @Override
    public void onMeasure(@NotNull RecyclerView.Recycler recycler, @NotNull RecyclerView.State state, int widthSpec, int heightSpec) {
        if (state.getItemCount() == 0) {
            super.onMeasure(recycler, state, widthSpec, heightSpec);
        } else if (!state.isPreLayout()) {
            View itemView = recycler.getViewForPosition(0);
            addView(itemView);
            itemView.measure(widthSpec, heightSpec);
            mItemWidth = getDecoratedMeasuredWidth(itemView);
            mItemHeight = getDecoratedMeasuredHeight(itemView);
            detachAndScrapView(itemView, recycler);
            setWidthAndHeight(mItemWidth, mItemHeight);
        }
    }

    private final void setWidthAndHeight(int width, int height) {
        if (orientation == HORIZONTAL) {
            setMeasuredDimension(width * visibleCount, height);
        } else {
            setMeasuredDimension(width, height * visibleCount);
        }

    }

    @Override
    public void onLayoutChildren(@NotNull RecyclerView.Recycler recycler, @NotNull RecyclerView.State state) {
        if (mPendingScrollToPosition != RecyclerView.NO_POSITION && state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
        } else if (!state.isPreLayout()) {
            //计算当前开始的position
            mPendingFillPosition = 0;
            boolean isScrollTo = mPendingScrollToPosition != RecyclerView.NO_POSITION;
            if (isScrollTo) {
                mPendingFillPosition = mPendingScrollToPosition;
            } else if (getChildCount() != 0) {
                mPendingFillPosition = getSelectedPosition();
            }
            //解决当调用notifyDataChanges时itemCount变小
            //且getSelectedPosition>itemCount的bug
            if (mPendingFillPosition >= state.getItemCount()) {
                mPendingFillPosition = state.getItemCount() - 1;
            }
            //暂时移除全部view，然后重新fill进来
            detachAndScrapAttachedViews(recycler);
            //开始就向下填充
            int anchor = getOffsetSpace();
            int fillDirection = FILL_END;
            fillLayout(recycler, state, anchor, fillDirection);
            //如果是isLoop=true，或者是scrollTo或软键盘弹起，再向上填充
            //getAnchorView可能为null，先判断下childCount
            if (getChildCount() != 0) {
                fillDirection = FILL_START;
                mPendingFillPosition = getPendingFillPosition(fillDirection);
                anchor = getAnchor(fillDirection);
                fillLayout(recycler, state, anchor, fillDirection);
            }
            if (isScrollTo) {
                int centerPosition = getSelectedPosition();
                dispatchOnItemSelectedListener(centerPosition);
            }
            transformChildren();
            dispatchOnItemFillListener();
            logChildCount(recycler);
        }
    }

    @Override
    public void onItemsChanged(@NotNull RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);
    }

    @Override
    public void onLayoutCompleted(@Nullable RecyclerView.State state) {
        super.onLayoutCompleted(state);
        mPendingScrollToPosition = RecyclerView.NO_POSITION;
    }

    @Override
    public boolean canScrollHorizontally() {
        return orientation == HORIZONTAL;
    }

    @Override
    public boolean canScrollVertically() {
        return orientation == VERTICAL;
    }

    @Override
    public int scrollHorizontallyBy(int dx, @NotNull RecyclerView.Recycler recycler, @NotNull RecyclerView.State state) {

        return orientation == VERTICAL ? 0 : scrollBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, @NotNull RecyclerView.Recycler recycler, @NotNull RecyclerView.State state) {
        return orientation == HORIZONTAL ? 0 : scrollBy(dy, recycler, state);
    }

    @Override
    public void scrollToPosition(int position) {
        if (getChildCount() != 0) {
            checkToPosition(position);
            mPendingScrollToPosition = position;
            requestLayout();
        }
    }

    @Override
    public void smoothScrollToPosition(@NotNull RecyclerView recyclerView, @NotNull RecyclerView.State state, int position) {
        if (getChildCount() != 0) {
            checkToPosition(position);
            int toPosition = fixSmoothToPosition(position);
            LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext());
            linearSmoothScroller.setTargetPosition(toPosition);
            startSmoothScroll(linearSmoothScroller);
        }
    }

    @Nullable
    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        } else {
            View view = mSnapHelper.findSnapView(this);
            int firstChildPos = getPosition(view);
            int direction = targetPosition < firstChildPos ? -1 : 1;
            return orientation == LinearLayoutManager.HORIZONTAL ? new PointF((float) direction, 0.0F) : new PointF(0.0F, (float) direction);
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (getChildCount() != 0) {
            if (state == RecyclerView.SCROLL_STATE_IDLE) {
                View selectedView = getSelectedView();
                if (selectedView == null) {
                    return;
                }
                int centerPosition = getPosition(selectedView);
                scrollToCenter(selectedView, centerPosition);
            }

        }
    }

    /**
     * 初始化摆放view
     */
    private final void fillLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int anchor, int fillDirection) {
        int innerAnchor = anchor;
        for (int count = fillDirection == FILL_START ? getOffsetCount() : getFixVisibleCount(); count > 0 && hasMore(state); --count) {
            View child = nextView(recycler, fillDirection);
            if (fillDirection == FILL_START) {
                addView(child, 0);
            } else {
                addView(child);
            }

            measureChildWithMargins(child, 0, 0);
            layoutChunk(child, innerAnchor, fillDirection);
            if (fillDirection == FILL_START) {
                innerAnchor -= mOrientationHelper.getDecoratedMeasurement(child);
            } else {
                innerAnchor += mOrientationHelper.getDecoratedMeasurement(child);
            }
        }

    }

    /**
     * 获取偏移的item count
     * 例如：开始position == 0居中，就要偏移一个item count的距离
     */
    private final int getOffsetCount() {
        return (visibleCount - 1) / 2;
    }

    /**
     * 获取真实可见的visible count
     * 例如：传入的visible count=3，但是在isLoop=false的情况下，
     * 开始只用填充2个item view进来就行了
     */
    private final int getFixVisibleCount() {
        return isLoop ? visibleCount : (visibleCount + 1) / 2;
    }

    private final void layoutChunk(View child, int anchor, int fillDirection) {
        int left;
        int top;
        int right;
        int bottom;
        if (orientation == HORIZONTAL) {
            top = getPaddingTop();
            bottom = getPaddingTop() + mOrientationHelper.getDecoratedMeasurementInOther(child) - getPaddingBottom();
            if (fillDirection == FILL_START) {
                right = anchor;
                left = anchor - mOrientationHelper.getDecoratedMeasurement(child);
            } else {
                left = anchor;
                right = anchor + mOrientationHelper.getDecoratedMeasurement(child);
            }
        } else {
            left = getPaddingLeft();
            right = mOrientationHelper.getDecoratedMeasurementInOther(child) - getPaddingRight();
            if (fillDirection == FILL_START) {
                bottom = anchor;
                top = anchor - mOrientationHelper.getDecoratedMeasurement(child);
            } else {
                top = anchor;
                bottom = anchor + mOrientationHelper.getDecoratedMeasurement(child);
            }
        }
        layoutDecoratedWithMargins(child, left, top, right, bottom);
    }

    private final int scrollBy(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() != 0 && delta != 0) {
            //开始填充item view
            int consume = fillScroll(delta, recycler, state);
            //移动全部子view
            mOrientationHelper.offsetChildren(-consume);
            //回收屏幕外的view
            recycleChildren(delta, recycler);
            //变换children
            transformChildren();
            //分发事件
            dispatchOnItemFillListener();
            //输出当前屏幕全部的子view
            logChildCount(recycler);
            return consume;
        } else {
            return 0;
        }
    }

    /**
     * 在滑动的时候填充view，
     * delta > 0 向右或向下移动
     * delta < 0 向左或向上移动
     */
    private final int fillScroll(int delta, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int absDelta = Math.abs(delta);
        int remainSpace = Math.abs(delta);
        int fillDirection = delta > 0 ? FILL_END : FILL_START;
        //检查滚动距离是否可以填充下一个view
        if (canNotFillScroll(fillDirection, absDelta)) {
            return delta;
        } else {
            int anchor;
            //检查是否滚动到了顶部或者底部
            if (checkScrollToEdge(fillDirection, state)) {
                anchor = getFixLastScroll(fillDirection);
                return fillDirection == -1 ? Math.max(anchor, delta) : Math.min(anchor, delta);
            } else {
                View child;
                //获取将要填充的view
                for (mPendingFillPosition = getPendingFillPosition(fillDirection); remainSpace > 0 && hasMore(state); remainSpace -= mOrientationHelper.getDecoratedMeasurement(child)) {
                    anchor = getAnchor(fillDirection);
                    child = nextView(recycler, fillDirection);
                    if (fillDirection == -1) {
                        addView(child, 0);
                    } else {
                        addView(child);
                    }

                    measureChildWithMargins(child, 0, 0);
                    layoutChunk(child, anchor, fillDirection);
                }
                return delta;
            }
        }
    }

    /**
     * 如果anchorView的(start或end)+delta还是没出现在屏幕内，
     * 就继续滚动，不填充view
     */
    private final boolean canNotFillScroll(int fillDirection, int delta) {
        View anchorView = getAnchorView(fillDirection);
        boolean var10000;
        int start;
        if (fillDirection == FILL_START) {
            start = mOrientationHelper.getDecoratedStart(anchorView);
            var10000 = start + delta < mOrientationHelper.getStartAfterPadding();
        } else {
            start = mOrientationHelper.getDecoratedEnd(anchorView);
            var10000 = start - delta > mOrientationHelper.getEndAfterPadding();
        }
        return var10000;
    }

    /**
     * 检查是否滚动到了底部或者顶部
     */
    private final boolean checkScrollToEdge(int fillDirection, RecyclerView.State state) {
        if (this.isLoop) {
            return false;
        } else {
            int anchorPosition = getAnchorPosition(fillDirection);
            if (fillDirection == FILL_START && anchorPosition == 0) {
                return true;
            } else {
                return fillDirection == FILL_END && anchorPosition == state.getItemCount() - 1;
            }
        }
    }

    private final int getFixLastScroll(int fillDirection) {
        View anchorView = getAnchorView(fillDirection);
        return fillDirection == -1 ? mOrientationHelper.getDecoratedStart(anchorView) - mOrientationHelper.getStartAfterPadding() - getOffsetSpace() : mOrientationHelper.getDecoratedEnd(anchorView) - mOrientationHelper.getEndAfterPadding() + getOffsetSpace();
    }

    /**
     * 如果不是循环模式，将要填充的view的position不在合理范围内
     * 就返回false
     */
    private final boolean hasMore(RecyclerView.State state) {
        if (isLoop) {
            return true;
        } else {
            return mPendingFillPosition >= 0 && mPendingFillPosition < state.getItemCount();
        }
    }

    private final View getAnchorView(int fillDirection) {
        View var10000;
        if (fillDirection == -1) {
            var10000 = getChildAt(0);
        } else {
            var10000 = getChildAt(getChildCount() - 1);
        }

        return var10000;
    }

    private final int getAnchorPosition(int fillDirection) {
        return getPosition(getAnchorView(fillDirection));
    }

    private final int getAnchor(int fillDirection) {
        View anchorView = getAnchorView(fillDirection);
        return fillDirection == -1 ? mOrientationHelper.getDecoratedStart(anchorView) : mOrientationHelper.getDecoratedEnd(anchorView);
    }

    private final int getPendingFillPosition(int fillDirection) {
        return getAnchorPosition(fillDirection) + fillDirection;
    }

    private final View nextView(RecyclerView.Recycler recycler, int fillDirection) {
        View child = getViewForPosition(recycler, mPendingFillPosition);
        mPendingFillPosition += fillDirection;
        return child;
    }

    private final void recycleChildren(int delta, RecyclerView.Recycler recycler) {
        if (delta > 0) {
            recycleStart();
        } else {
            recycleEnd();
        }

        logRecycleChildren();
        Iterator var4 = mRecycleViews.iterator();

        while (var4.hasNext()) {
            View view = (View) var4.next();
            removeAndRecycleView(view, recycler);
        }

        mRecycleViews.clear();
    }

    private final void recycleStart() {
        int i = 0;

        for (int var2 = getChildCount(); i < var2; ++i) {
            View var10000 = getChildAt(i);
            Intrinsics.checkNotNull(var10000);
            Intrinsics.checkNotNullExpressionValue(var10000, "getChildAt(i)!!");
            View child = var10000;
            int end = mOrientationHelper.getDecoratedEnd(child);
            if (end >= mOrientationHelper.getStartAfterPadding() - getItemOffset()) {
                break;
            }

            mRecycleViews.add(child);
        }

    }

    private final void recycleEnd() {
        int i = getChildCount() - 1;

        for (boolean var2 = false; i >= 0; --i) {
            View var10000 = getChildAt(i);
            Intrinsics.checkNotNull(var10000);
            Intrinsics.checkNotNullExpressionValue(var10000, "getChildAt(i)!!");
            View child = var10000;
            int start = mOrientationHelper.getDecoratedStart(child);
            if (start <= mOrientationHelper.getEndAfterPadding() + getItemOffset()) {
                break;
            }

            mRecycleViews.add(child);
        }

    }

    private final View getSelectedView() {
        return mSnapHelper.findSnapView(this);
    }

    private final int getItemSpace() {
        return orientation == 0 ? mItemWidth : mItemHeight;
    }

    private final void getScrollOffset() {
    }

    private final int getItemOffset() {
        return getItemSpace() / 2;
    }

    private final int getOffsetSpace() {
        return getOffsetCount() * getItemSpace();
    }

    private final View getViewForPosition(RecyclerView.Recycler recycler, int position) {
        if (isLoop || position >= 0 && position < getItemCount()) {
            View var10000;
            if (isLoop && position > getItemCount() - 1) {
                var10000 = recycler.getViewForPosition(position % getItemCount());
                Intrinsics.checkNotNullExpressionValue(var10000, "recycler.getViewForPosition(position % itemCount)");
                return var10000;
            } else if (isLoop && position < 0) {
                var10000 = recycler.getViewForPosition(getItemCount() + position);
                Intrinsics.checkNotNullExpressionValue(var10000, "recycler.getViewForPosition(itemCount + position)");
                return var10000;
            } else {
                var10000 = recycler.getViewForPosition(position);
                Intrinsics.checkNotNullExpressionValue(var10000, "recycler.getViewForPosition(position)");
                return var10000;
            }
        } else {
            throw new IllegalArgumentException("position <0 or >= itemCount with !isLoop");
        }
    }

    private final void checkToPosition(int position) {
        if (position < 0 || position > getItemCount() - 1) {
            throw new IllegalArgumentException("position is " + position + ",must be >= 0 and < itemCount,");
        }
    }

    private final int fixSmoothToPosition(int toPosition) {
        int fixCount = getOffsetCount();
        int centerPosition = getSelectedPosition();
        return centerPosition < toPosition ? toPosition + fixCount : toPosition - fixCount;
    }

    private final void dispatchOnItemSelectedListener(int position) {
        if (!mOnItemSelectedListener.isEmpty() && position >= 0) {
            Iterator var3 = mOnItemSelectedListener.iterator();
            while (var3.hasNext()) {
                Function1 listener = (Function1) var3.next();
                listener.invoke(position);
            }

        }
    }

    // $FF: synthetic method
    static void dispatchOnItemSelectedListener$default(PickerLayoutManager var0, int var1, int var2, Object var3) {
        if (var3 != null) {
            throw new UnsupportedOperationException("Super calls with default arguments not supported in this target, function: dispatchOnItemSelectedListener");
        } else {
            if ((var2 & 1) != 0) {
                var1 = var0.getSelectedPosition();
            }

            var0.dispatchOnItemSelectedListener(var1);
        }
    }

    private final void scrollToCenter(View centerView, int centerPosition) {
        int destination = mOrientationHelper.getTotalSpace() / 2 - mOrientationHelper.getDecoratedMeasurement(centerView) / 2;
        int distance = destination - mOrientationHelper.getDecoratedStart(centerView);
        mOrientationHelper.offsetChildren(distance);
        dispatchOnItemSelectedListener(centerPosition);
    }

    private final void smoothOffsetChildren(int amount, final int centerPosition) {
        final Ref.IntRef lastValue = new Ref.IntRef();
        lastValue.element = amount;
        ValueAnimator var5 = ValueAnimator.ofInt(amount, 0);
        var5.setInterpolator((TimeInterpolator) (new LinearInterpolator()));
        var5.setDuration(300L);
        var5.addUpdateListener((ValueAnimator.AnimatorUpdateListener) (new ValueAnimator.AnimatorUpdateListener() {
            public final void onAnimationUpdate(@NotNull ValueAnimator it) {
                Intrinsics.checkNotNullParameter(it, "it");
                Object var10000 = it.getAnimatedValue();
                if (var10000 == null) {
                    throw new NullPointerException("null cannot be cast to non-null type kotlin.Int");
                } else {
                    int value = (Integer) var10000;
                    mOrientationHelper.offsetChildren(lastValue.element - value);
                    lastValue.element = value;
                }
            }
        }));
        var5.addListener(new Animator.AnimatorListener() {
            public void onAnimationRepeat(@Nullable Animator animation) {
            }

            public void onAnimationEnd(@Nullable Animator animation) {
            }

            public void onAnimationCancel(@Nullable Animator animation) {
            }

            public void onAnimationStart(@Nullable Animator animation) {
                dispatchOnItemSelectedListener(centerPosition);
            }
        });
        var5.start();
    }

    public final void addOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        if (listener != null) {
            mOnItemSelectedListener.add(listener);
        }

    }

    public final void removeOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        if (listener != null) {
            mOnItemSelectedListener.remove(listener);
        }

    }

    public final void removeAllOnItemSelectedListener() {
        mOnItemSelectedListener.clear();
    }

    public final int getSelectedPosition() {
        if (getChildCount() == 0) {
            return -1;
        } else {
            View centerView = getSelectedView();
            if (centerView == null) {
                return -1;
            } else {
                return getPosition(centerView);
            }
        }
    }

    public void transformChildren() {
        if (getChildCount() != 0) {
            View var10000 = getSelectedView();
            if (var10000 != null) {
                View centerView = var10000;
                int centerPosition = getPosition(centerView);
                if (getChildCount() != 0) {
                    int i = 0;

                    for (int var4 = getChildCount(); i < var4; ++i) {
                        var10000 = getChildAt(i);
                        Intrinsics.checkNotNull(var10000);
                        Intrinsics.checkNotNullExpressionValue(var10000, "getChildAt(i)!!");
                        View child = var10000;
                        int position = getPosition(child);
                        if (position == centerPosition) {
                            child.setScaleX(1.0F);
                            child.setScaleY(1.0F);
                            child.setAlpha(1.0F);
                        } else {
                            float scaleX = getScale(this.scaleX, getIntervalCount(centerPosition, position));
                            float scaleY = getScale(this.scaleY, getIntervalCount(centerPosition, position));
                            child.setScaleX(scaleX);
                            child.setScaleY(scaleY);
                            child.setAlpha(alpha);
                        }
                    }

                }
            }
        }
    }

    private final float getScale(float scale, int intervalCount) {
        return scale == 1.0F ? scale : (float) 1 - ((float) 1 - scale) * (float) intervalCount;
    }

    private final int getIntervalCount(int centerPosition, int position) {
        int var3;
        if (!isLoop) {
            var3 = centerPosition - position;
            return Math.abs(var3);
        } else if (position > centerPosition && position - centerPosition > visibleCount) {
            return getItemCount() - position;
        } else if (position < centerPosition && centerPosition - position > visibleCount) {
            return position + 1;
        } else {
            var3 = position - centerPosition;
            return Math.abs(var3);
        }
    }

    private final void dispatchOnItemFillListener() {
        if (getChildCount() != 0 && !mOnItemFillListener.isEmpty()) {
            View var10000 = getSelectedView();
            if (var10000 != null) {
                View centerView = var10000;
                int centerPosition = getPosition(centerView);
                int i = 0;

                for (int var4 = getChildCount(); i < var4; ++i) {
                    var10000 = getChildAt(i);
                    if (var10000 != null) {
                        Intrinsics.checkNotNullExpressionValue(var10000, "getChildAt(i) ?: continue");
                        View child = var10000;
                        int position = getPosition(child);
                        if (position == centerPosition) {
                            onItemSelected(child, position);
                        } else {
                            onItemUnSelected(child, position);
                        }
                    }
                }

            }
        }
    }

    public void onItemSelected(@NotNull View child, int position) {
        Intrinsics.checkNotNullParameter(child, "child");
        Iterator var4 = mOnItemFillListener.iterator();

        while (var4.hasNext()) {
            PickerLayoutManager.OnItemFillListener listener = (PickerLayoutManager.OnItemFillListener) var4.next();
            listener.onItemSelected(child, position);
        }

    }

    public void onItemUnSelected(@NotNull View child, int position) {
        Intrinsics.checkNotNullParameter(child, "child");
        Iterator var4 = mOnItemFillListener.iterator();

        while (var4.hasNext()) {
            PickerLayoutManager.OnItemFillListener listener = (PickerLayoutManager.OnItemFillListener) var4.next();
            listener.onItemUnSelected(child, position);
        }

    }

    public final void addOnItemFillListener(@NotNull PickerLayoutManager.OnItemFillListener listener) {
        Intrinsics.checkNotNullParameter(listener, "listener");
        mOnItemFillListener.add(listener);
    }

    public final void removeOnItemFillListener(@NotNull PickerLayoutManager.OnItemFillListener listener) {
        Intrinsics.checkNotNullParameter(listener, "listener");
        mOnItemFillListener.remove(listener);
    }

    public final void removeAllItemFillListener() {
        mOnItemFillListener.clear();
    }

    private final void logDebug(String msg) {
        if (DEBUG) {
            Log.d("PickerLayoutManager", hashCode() + " -- " + msg);
        }
    }

    private final void logChildCount(RecyclerView.Recycler recycler) {
        if (DEBUG) {
            logDebug("childCount == " + getChildCount() + " -- scrapSize == " + recycler.getScrapList().size());
            logChildrenPosition();
        }
    }

    private final void logChildrenPosition() {
        if (DEBUG) {
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for (int var3 = getChildCount(); i < var3; ++i) {
                View child = getChildAt(i);
                Intrinsics.checkNotNull(child);
                builder.append(getPosition(child));
                builder.append(",");
            }

            logDebug("children == " + builder);
        }
    }

    private final void logRecycleChildren() {
        if (DEBUG) {
            StringBuilder builder = new StringBuilder();
            Iterator var3 = mRecycleViews.iterator();
            while (var3.hasNext()) {
                View child = (View) var3.next();
                builder.append(getPosition(child));
                builder.append(",");
            }
            CharSequence var4 = builder;
            if (var4.length() != 0) {
                logDebug("recycle children == " + builder);
            }
        }
    }

    public final int getOrientation() {
        return orientation;
    }

    public final void setOrientation(int var1) {
        orientation = var1;
    }

    public final int getVisibleCount() {
        return visibleCount;
    }

    public final void setVisibleCount(int var1) {
        visibleCount = var1;
    }

    public final boolean isLoop() {
        return isLoop;
    }

    public final void setLoop(boolean var1) {
        isLoop = var1;
    }

    public final float getScaleX() {
        return scaleX;
    }

    public final void setScaleX(float var1) {
        scaleX = var1;
    }

    public final float getScaleY() {
        return scaleY;
    }

    public final void setScaleY(float var1) {
        scaleY = var1;
    }

    public final float getAlpha() {
        return alpha;
    }

    public final void setAlpha(float var1) {
        alpha = var1;
    }


    public interface OnItemFillListener {
        void onItemSelected(@NotNull View var1, int position);

        void onItemUnSelected(@NotNull View var1, int position);
    }

    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }

    public static final class Builder {
        private int orientation = 1;
        private int visibleCount = 3;
        private boolean isLoop;
        private float scaleX = 1.0F;
        private float scaleY = 1.0F;
        private float alpha = 1.0F;

        public Builder setOrientation(int orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder setVisibleCount(int visibleCount) {
            this.visibleCount = visibleCount;
            return this;
        }

        public Builder setIsLoop(boolean isLoop) {
            this.isLoop = isLoop;
            return this;
        }

        public Builder setScaleX(@FloatRange(from = 0.0D, to = 1.0D) float scaleX) {
            this.scaleX = scaleX;
            return this;
        }

        public Builder setScaleY(@FloatRange(from = 0.0D, to = 1.0D) float scaleY) {
            this.scaleY = scaleY;
            return this;
        }

        public Builder setAlpha(@FloatRange(from = 0.0D, to = 1.0D) float alpha) {
            this.alpha = alpha;
            return this;
        }

        @NotNull
        public PickerLayoutManager build() {
            return new PickerLayoutManager(orientation, visibleCount, isLoop, scaleX, scaleY, alpha);
        }
    }


}
