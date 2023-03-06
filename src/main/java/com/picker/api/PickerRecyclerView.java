package com.picker.api;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.recyclerview.widget.RecyclerView;

import com.exoplayer.pickerlibrary.R;

public class PickerRecyclerView extends RecyclerView {

    public static final boolean DIVIDER_VISIBLE = true;
    public static final float DIVIDER_SIZE = 1.0f;
    public static final int DIVIDER_COLOR = Color.LTGRAY;
    public static final float DIVIDER_MARGIN = 0f;
    private int mOrientation = PickerLayoutManager.VERTICAL;
    private int mVisibleCount = PickerLayoutManager.VISIBLE_COUNT;
    private boolean mIsLoop = PickerLayoutManager.IS_LOOP;
    private float mScaleX = PickerLayoutManager.SCALE_X;
    private float mScaleY = PickerLayoutManager.SCALE_Y;
    private float mAlpha = PickerLayoutManager.ALPHA;

    private boolean mDividerVisible = DIVIDER_VISIBLE;
    private float mDividerSize = DIVIDER_SIZE;
    private int mDividerColor = DIVIDER_COLOR;
    private float mDividerMargin = DIVIDER_MARGIN;
    private PickerItemDecoration mDecor = null;
    private final Context context;


    public PickerRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public PickerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, androidx.recyclerview.R.attr.recyclerViewStyle);
    }

    public PickerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initAttrs(attrs);
        resetLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray typeA = context.obtainStyledAttributes(attrs, R.styleable.PickerRecyclerView);
        mOrientation = typeA.getInt(R.styleable.PickerRecyclerView_orientation, mOrientation);
        mVisibleCount = typeA.getInt(R.styleable.PickerRecyclerView_visibleCount, mVisibleCount);
        mIsLoop = typeA.getBoolean(R.styleable.PickerRecyclerView_isLoop, mIsLoop);
        mScaleX = typeA.getFloat(R.styleable.PickerRecyclerView_scaleX, mScaleX);
        mScaleY = typeA.getFloat(R.styleable.PickerRecyclerView_scaleY, mScaleY);
        mAlpha = typeA.getFloat(R.styleable.PickerRecyclerView_alpha, mAlpha);
        mDividerVisible = typeA.getBoolean(R.styleable.PickerRecyclerView_dividerVisible, mDividerVisible);
        mDividerSize = typeA.getDimension(R.styleable.PickerRecyclerView_dividerSize, mDividerSize);
        mDividerColor = typeA.getColor(R.styleable.PickerRecyclerView_dividerColor, mDividerColor);
        mDividerMargin = typeA.getDimension(R.styleable.PickerRecyclerView_dividerMargin, mDividerMargin);
        typeA.recycle();
    }

    public void resetLayoutManager() {
        resetLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
    }

    public void resetLayoutManager(int mOrientation) {
        resetLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
    }

    public void resetLayoutManager(int mOrientation, int mVisibleCount) {
        resetLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
    }

    public void resetLayoutManager(int mOrientation, int mVisibleCount, boolean mIsLoop) {
        resetLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
    }

    public void resetLayoutManager(int mOrientation, int mVisibleCount, boolean mIsLoop, float mScaleX) {
        resetLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
    }

    public void resetLayoutManager(int mOrientation, int mVisibleCount, boolean mIsLoop, float mScaleX, float mScale) {
        resetLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
    }


    public void resetLayoutManager(int mOrientation, int mVisibleCount, boolean mIsLoop, float mScaleX, float mScaleY, float mAlpha) {
        PickerLayoutManager lm = new PickerLayoutManager(mOrientation, mVisibleCount, mIsLoop, mScaleX, mScaleY, mAlpha);
        resetLayoutManager(lm);
    }

    public void resetLayoutManager(PickerLayoutManager lm) {
        setLayoutManager(lm);
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layout) {
        super.setLayoutManager(layout);
        initDivider();
        if (!(layout instanceof PickerLayoutManager)) {
            throw new IllegalArgumentException("LayoutManager only can use PickerLayoutManager");
        }
    }

    @Nullable
    @Override
    public PickerLayoutManager getLayoutManager() {
        return (PickerLayoutManager) super.getLayoutManager();
    }

    /**
     * 获取选中的那个item的position
     */
    public int getSelectedPosition() {
        return getLayoutManager().getSelectedPosition();
    }


    public final int getMOrientation() {
        return this.mOrientation;
    }

    public final void setMOrientation(int var1) {
        this.mOrientation = var1;
    }

    public final int getMVisibleCount() {
        return this.mVisibleCount;
    }

    public final void setMVisibleCount(int var1) {
        this.mVisibleCount = var1;
    }

    public final boolean getMIsLoop() {
        return this.mIsLoop;
    }

    public final void setMIsLoop(boolean var1) {
        this.mIsLoop = var1;
    }

    public final float getMScaleX() {
        return this.mScaleX;
    }

    public final void setMScaleX(float var1) {
        this.mScaleX = var1;
    }

    public final float getMScaleY() {
        return this.mScaleY;
    }

    public final void setMScaleY(float var1) {
        this.mScaleY = var1;
    }

    public final float getMAlpha() {
        return this.mAlpha;
    }

    public final void setMAlpha(float var1) {
        this.mAlpha = var1;
    }

    public final boolean getMDividerVisible() {
        return this.mDividerVisible;
    }

    public final void setMDividerVisible(boolean var1) {
        this.mDividerVisible = var1;
    }

    public final float getMDividerSize() {
        return this.mDividerSize;
    }

    public final void setMDividerSize(float var1) {
        this.mDividerSize = var1;
    }

    public final int getMDividerColor() {
        return this.mDividerColor;
    }

    public final void setMDividerColor(int var1) {
        this.mDividerColor = var1;
    }

    public final float getMDividerMargin() {
        return this.mDividerMargin;
    }

    public final void setMDividerMargin(float var1) {
        this.mDividerMargin = var1;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public void setVisibleCount(int count) {
        this.mVisibleCount = count;
    }

    public void setIsLoop(boolean isLoop) {
        this.mIsLoop = isLoop;
    }

    public void setItemScaleX(float scaleX) {
        this.mScaleX = scaleX;
    }

    public void setItemScaleY(float scaleY) {
        this.mScaleY = scaleY;
    }

    public void setItemAlpha(float alpha) {
        this.mAlpha = alpha;
    }

    public void setDividerVisible(boolean visible) {
        this.mDividerVisible = visible;
    }

    public void setDividerSize(@Px float size) {
        this.mDividerSize = size;
    }

    public void setDividerColor(@ColorInt int color) {
        this.mDividerColor = color;
    }

    public void setDividerMargin(float margin) {
        this.mDividerMargin = margin;
    }

    //设置分割线
    public void initDivider() {
        removeDivider();
        if (!mDividerVisible) {
            return;
        }
        mDecor = new PickerItemDecoration(mDividerColor, mDividerSize, mDividerMargin);
        this.addItemDecoration(mDecor);
    }

    //删除分割线
    public void removeDivider() {
        if (mDecor != null) {
            removeItemDecoration(mDecor);
        }
    }

    /**
     *
     */
    public void addOnSelectedItemListener(PickerLayoutManager.OnItemSelectedListener listener) {
        getLayoutManager().addOnItemSelectedListener(listener);
    }

    /**
     *
     */
    public void removeOnItemSelectedListener(PickerLayoutManager.OnItemSelectedListener listener) {
        getLayoutManager().removeOnItemSelectedListener(listener);
    }

    /**
     * 删除所有的监听器
     */
    public void removeAllOnItemSelectedListener() {
        getLayoutManager().removeAllOnItemSelectedListener();
    }

    /**
     *
     */
    public void addOnItemFillListener(PickerLayoutManager.OnItemFillListener listener) {
        getLayoutManager().addOnItemFillListener(listener);
    }

    /**
     *
     */
    public void removeOnItemFillListener(PickerLayoutManager.OnItemFillListener listener) {
        getLayoutManager().removeOnItemFillListener(listener);
    }

    /**
     *
     */
    public void removeAllItemFillListener() {
        getLayoutManager().removeAllItemFillListener();
    }


    /**
     * 滚动到最后一个item
     */
    public void scrollToEnd() {
        if (getAdapter() == null) {
            return;
        }
        post(() -> scrollToPosition(getAdapter().getItemCount() - 1));
    }

    /**
     * 平滑的滚动到最后一个item
     */
    public void smoothScrollToEnd() {
        if (getAdapter() == null) {
            return;
        }
        post(() -> this.scrollToPosition(getAdapter().getItemCount() - 1));
    }
}
