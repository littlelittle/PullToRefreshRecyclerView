package com.adnonstop.ptrrecyclerviewbeta2.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.adnonstop.ptrrecyclerviewbeta2.R;
import com.adnonstop.ptrrecyclerviewbeta2.activity.PTRActivity;
import com.adnonstop.ptrrecyclerviewbeta2.adapter.PTRAdapter;
import com.adnonstop.ptrrecyclerviewbeta2.util.L;

/**
 * Author:　Created by benjamin
 * DATE :  2017/2/18 15:10
 */

public class PTRRelativeLayout extends RelativeLayout {
    private static final String TAG = "PTRRelativeLayout";
    private RecyclerView mRV;
    private float downRawY;
    private float moveRawY;
    private float disY;
    private LinearLayoutManager mLLManager;
    private LayoutParams mRVLayoutParams;
    private FrameLayout mflpb;
    private LayoutParams mflpbLayoutParams;
    private ValueAnimator mAnimation;
    private float mDownRawY;//监听touch down 事件
    private float mDamping;//阻尼系数
    private int mNewTopMargin;
    private int mPreTopMargin;
    private int mTotalTopMargin;//top margin 的叠加
    private RecyclerView.Adapter mAdapter;//绑定Adapter

    public PTRRelativeLayout(Context context) {
        super(context);
        initChild();
    }


    public PTRRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initChild();
    }

    public PTRRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initChild();
    }


    private void initChild() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mflpb = (FrameLayout) getChildAt(0);
                mRV = (RecyclerView) getChildAt(1);

                mLLManager = (LinearLayoutManager) mRV.getLayoutManager();

                getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownRawY = ev.getRawY();
                L.i(TAG, PTRRelativeLayout.TAG + "downRawY = " + mDownRawY);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mRV != null) {
            LayoutParams mRVLayoutParams = (LayoutParams) mRV.getLayoutParams();
            int topMargin = mRVLayoutParams.topMargin;
            /**
             *拦截touch事件
             * recyclerView 的 touch事件 被阻拦
             */
            if (topMargin > 0) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downRawY = event.getRawY();
                L.i(TAG, "onClick: downRawY = " + downRawY);

                break;
            case MotionEvent.ACTION_MOVE:
                moveRawY = event.getRawY();
                L.i(TAG, "onClick: moveRawY = " + moveRawY);
                disY = moveRawY - downRawY;
                L.i(TAG, "onClick: disY = " + disY);

                if (mLLManager.findFirstVisibleItemPosition() == 0) {
                    if (disY < 0) {
                        setTopMargin(0, true);
                    } else {
                        setTopMargin((int) disY, true);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                /**
                 * ① 不刷新，弹回去
                 * ② 刷新，回弹到topMargin = progress_layout.height并联网获取数据，得到响应后弹回去
                 * ③ mpreTopMargin 必须置0
                 * ④ mTotalMargin 必须置0
                 */
                mPreTopMargin = 0;//置0
                mTotalTopMargin = 0;//置0
                L.i(TAG, "mPreTopMargin = 0;//置0 ");
                if (disY < getResources().getDimension(R.dimen.prt_threshold_refresh)) {
                    setTopMargin(0, false);
                } else {
                    setTopMargin((int) getResources().getDimension(R.dimen.ptr_pb_height), false);
                    getDataFromNet();
                }
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * @param topMargin
     * @param isDrag    手指拖拽是为true；否则为false。
     */
    public void setTopMargin(final int topMargin, final boolean isDrag) {
        if (mRV == null)
            return;

        if (!isDrag) {
            if (mRVLayoutParams == null) {
                mRVLayoutParams = (LayoutParams) mRV.getLayoutParams();
            }
            final int rvMagin = mRVLayoutParams.topMargin;

            if (mAnimation == null) {
                mAnimation = ValueAnimator.ofFloat(0f, 1f);
            }
            mAnimation.setDuration(300);
            mAnimation.start();
            mAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
//                    L.i(TAG, "fraction = " + animation.getAnimatedFraction());
                    Float evaluate = evaluate(animation.getAnimatedFraction(), rvMagin, topMargin);
                    float floatValue = evaluate.floatValue();
                    doMove((int) floatValue, isDrag);
                }
            });
        } else {
            doMove(topMargin, isDrag);
        }


    }


    private void doMove(int topMargin, boolean isDrag) {
        if (!isDrag) {

            if (mRVLayoutParams == null) {
                mRVLayoutParams = (LayoutParams) mRV.getLayoutParams();
            }
            mRVLayoutParams.topMargin = topMargin;
            mRV.setLayoutParams(mRVLayoutParams);

            if (mflpbLayoutParams == null) {
                mflpbLayoutParams = (LayoutParams) mflpb.getLayoutParams();
            }
            mflpbLayoutParams.topMargin = (int) (getResources().getDimension(R.dimen.ptr_pb_height_negative) + topMargin);
            mflpb.setLayoutParams(mflpbLayoutParams);
        } else {

            mDamping = 1.0f;

            if (topMargin > getResources().getDimension(R.dimen.ptr_pb_height)) {
                mDamping = 2.0f;
            }

            mNewTopMargin = topMargin;

            mTotalTopMargin += (mNewTopMargin - mPreTopMargin) / mDamping;

            mPreTopMargin = mNewTopMargin;

            if (mRVLayoutParams == null) {
                mRVLayoutParams = (LayoutParams) mRV.getLayoutParams();
            }


            mRVLayoutParams.topMargin = mTotalTopMargin;
            mRV.setLayoutParams(mRVLayoutParams);

            if (mflpbLayoutParams == null) {
                mflpbLayoutParams = (LayoutParams) mflpb.getLayoutParams();
            }
            mflpbLayoutParams.topMargin = (int) (getResources().getDimension(R.dimen.ptr_pb_height_negative) + mTotalTopMargin);
            mflpb.setLayoutParams(mflpbLayoutParams);
        }

    }

    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    /**
     * 联网获取数据
     */
    public void getDataFromNet() {
        if (mAdapter != null) {
            if (mAdapter instanceof PTRAdapter) {
                PTRAdapter ptrAdapter = (PTRAdapter) mAdapter;
                ptrAdapter.refreshData();
                L.i(TAG, "刷新数据的操作走了Adapter");
            }
        }
    }

    /**
     * 因为从recyclerView的touch事件到myRelativeLayout的touch事件
     * myRelativelayout不走ACTION_DOWN，所以必须给downRawY赋初始值
     *
     * @param moveRawY recyclerView 的touch事件的当前值：moveRawY
     */
    public void setInitDownRawY(float moveRawY) {
        downRawY = moveRawY;
    }


    /**
     * @return ptrRelativeLayout touch down rawY
     */
    public float getmDownRawY() {
        return mDownRawY;
    }

    /**
     * @param refreshState true：正在刷新
     */
    public void setRefreshState(boolean refreshState) {
        if (refreshState) {
            setTopMargin((int) getResources().getDimension(R.dimen.ptr_pb_height), true);
        } else {
            setTopMargin(0, false);
        }
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mAdapter = adapter;
    }
}
