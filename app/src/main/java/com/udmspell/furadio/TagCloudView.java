package com.udmspell.furadio;

/**
 * Komodo Lab: Tagin! Project: 3D Tag Cloud
 * Google Summer of Code 2011
 * @authors Reza Shiftehfar, Sara Khosravinasr and Jorge Silva
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TagCloudView extends RelativeLayout {

    private final float TOUCH_SCALE_FACTOR = 30.8f;
    private float tspeed;
    private TagCloud mTagCloud;
    private float mAngleX = 0;
    private float mAngleY = 0;
    private float centerX, centerY;
    private float offsetX, offsetY;
    private float radius;
    private Context mContext;
    private int viewWidth;
    private int viewHeight;
    private List<TextView> textViewList;
    private List<RelativeLayout.LayoutParams> mParams;
    private int shiftLeft;

    private final String TAG = "cloudtag";
    private boolean actionMove;

    private GestureDetector gestureDetector;
    private Scroller scroller;
    private int startX;
    private int startY;

    public interface TagCallback {
        void onClick(String title, String url);
    }

    TagCallback tagCallback;

    public TagCloudView(Context mContext, AttributeSet attrSet) {
        super(mContext, attrSet);
    }

    public TagCloudView(Context mContext, int width, int height, List<Tag> tagList, int offsetX, int offsetY) {
        this(mContext, width, height, tagList, 6, 34, 1, offsetX, offsetY); // default
        try {
            tagCallback = (TagCallback) mContext;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

    }

    public TagCloudView(Context mContext, int width, int height, List<Tag> tagList, int textSizeMin, int textSizeMax,
                        int scrollSpeed, int offsetX, int offsetY) {

        super(mContext);
        this.mContext = mContext;
        this.viewWidth = width;
        this.viewHeight = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;

        tspeed = scrollSpeed;

        // set the center of the sphere on center of our screen:
        centerX = width / 2;
        centerY = height / 2;
        radius = Math.min(centerX * 0.95f, centerY * 0.95f); // use 95% of
        // screen
        // since we set tag margins from left of screen, we shift the whole tags
        // to left so that
        // it looks more realistic and symmetric relative to center of screen in
        // X direction
        shiftLeft = (int) (Math.min(centerX * 0.15f, centerY * 0.15f));

        // initialize the TagCloud from a list of tags
        // Filter() func. screens tagList and ignores Tags with same text (Case
        // Insensitive)
        mTagCloud = new TagCloud(Filter(tagList), (int) radius, textSizeMin, textSizeMax);
        float[] tempColor1 = {0.9412f, 0.7686f, 0.2f, 1}; // rgb Alpha
        // {1f,0f,0f,1} red {0.3882f,0.21568f,0.0f,1} orange
        // {0.9412f,0.7686f,0.2f,1} light orange
        float[] tempColor2 = {1f, 0f, 0f, 1}; // rgb Alpha
        // {0f,0f,1f,1} blue
        // {0.1294f,0.1294f,0.1294f,1}
        // grey
        // {0.9412f,0.7686f,0.2f,1}
        // light orange
        mTagCloud.setTagColor1(tempColor1);// higher color
        mTagCloud.setTagColor2(tempColor2);// lower color
        mTagCloud.setRadius((int) radius);
        mTagCloud.create(true); // to put each Tag at its correct initial
        // location

        // update the transparency/scale of tags
        mTagCloud.setAngleX(mAngleX);
        mTagCloud.setAngleY(mAngleY);
        mTagCloud.update();

        textViewList = new ArrayList<>();
        mParams = new ArrayList<>();
        // Now Draw the 3D objects: for all the tags in the TagCloud
        Iterator it = mTagCloud.iterator();
        Tag tempTag;
        int i = 0;

        while (it.hasNext()) {
            tempTag = (Tag) it.next();
            tempTag.setParamNo(i); // store the parameter No. related to this
            // tag

            textViewList.add(new TextView(this.mContext));
            textViewList.get(i).setText(tempTag.getText());

            mParams.add(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mParams.get(i).setMargins((int) (centerX - shiftLeft + offsetX + tempTag.getLoc2DX()),
                    (int) (centerY + offsetY + tempTag.getLoc2DY()), 0, 0);
            textViewList.get(i).setLayoutParams(mParams.get(i));
            textViewList.get(i).setSingleLine(true);
            int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255),
                    (int) (tempTag.getColorG() * 255), (int) (tempTag.getColorB() * 255));
            textViewList.get(i).setTextColor(mergedColor);
            textViewList.get(i).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
            addView(textViewList.get(i));
            textViewList.get(i).setOnClickListener(OnTagClickListener(tempTag.getText(), tempTag.getUrl()));
            i++;
        }

        gestureDetector = new GestureDetector(mContext, new GestureListener());
        scroller = new Scroller(mContext);
//        scroller.setFriction(2000f);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        Log.d(TAG, "computeScroll");
        if (scroller.computeScrollOffset()) {
            int currX = scroller.getCurrX();
            int currY = scroller.getCurrY();
            Log.d(TAG, "computeScroll: startX=" + startX + ";startY=" + startY+ ";currX=" + currX + "currY=" + currY);

            int dx = startX - currX;
            int dy = startY - currY;
            startX = currX;
            startY = currY;

            mAngleX = (dy / radius) * tspeed * TOUCH_SCALE_FACTOR;
            mAngleY = (-dx / radius) * tspeed * TOUCH_SCALE_FACTOR;

            mTagCloud.setAngleX(mAngleX);
            mTagCloud.setAngleY(mAngleY);
            mTagCloud.update();
            Iterator it = mTagCloud.iterator();
            Tag tempTag;
            while (it.hasNext()) {
                tempTag = (Tag) it.next();
                mParams.get(tempTag.getParamNo()).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()),
                        (int) (centerY + tempTag.getLoc2DY()), 0, 0);
                textViewList.get(tempTag.getParamNo()).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
                int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255),
                        (int) (tempTag.getColorG() * 255), (int) (tempTag.getColorB() * 255));
                textViewList.get(tempTag.getParamNo()).setTextColor(mergedColor);
                textViewList.get(tempTag.getParamNo()).bringToFront();
            }

        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, getTime() + "event onDown");
            scroller.forceFinished(true);
            postInvalidateOnAnimation();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, getTime() + "event onSingleTapUp");
            return super.onSingleTapUp(e);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, getTime() + "event onShowPress");
            super.onShowPress(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, getTime() + "event onFling: startX=" + startX + ";startY=" + startY+ ";velocityX=" + velocityX + ";velocityY=" + velocityY);
            scroller.forceFinished(true);
            scroller.fling(startX,
                    startY,
                    (int) -velocityX,
                    (int) -velocityY,
                    0, viewWidth,
                    0, viewHeight);
            postInvalidateOnAnimation();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, getTime() + "event onScroll");

            mAngleX = (-distanceY / radius) * tspeed * TOUCH_SCALE_FACTOR;
            mAngleY = (distanceX / radius) * tspeed * TOUCH_SCALE_FACTOR;

            mTagCloud.setAngleX(mAngleX);
            mTagCloud.setAngleY(mAngleY);
            mTagCloud.update();
            Iterator it = mTagCloud.iterator();
            Tag tempTag;
            while (it.hasNext()) {
                tempTag = (Tag) it.next();
                mParams.get(tempTag.getParamNo()).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()),
                        (int) (centerY + tempTag.getLoc2DY()), 0, 0);
                textViewList.get(tempTag.getParamNo()).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
                int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255),
                        (int) (tempTag.getColorG() * 255), (int) (tempTag.getColorB() * 255));
                textViewList.get(tempTag.getParamNo()).setTextColor(mergedColor);
                textViewList.get(tempTag.getParamNo()).bringToFront();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        Log.d(TAG, getTime() + "child onTouchEvent: x=" + e.getX() + ", y=" + e.getY() + ",action is [" + e.getAction() + "]");
        startX = (int) e.getX();
        startY = (int) e.getY();
        return gestureDetector.onTouchEvent(e) || super.onTouchEvent(e);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        return onTouchEvent(e) || super.dispatchTouchEvent(e);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void addTag(Tag newTag) {
        mTagCloud.add(newTag);

        int i = textViewList.size();
        newTag.setParamNo(i);

        textViewList.add(new TextView(this.mContext));
        textViewList.get(i).setText(newTag.getText());

        mParams.add(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mParams.get(i).setMargins((int) (centerX - shiftLeft + offsetX + newTag.getLoc2DX()),
                (int) (centerY + offsetY + newTag.getLoc2DY()), 0, 0);
        textViewList.get(i).setLayoutParams(mParams.get(i));

        textViewList.get(i).setSingleLine(true);
        int mergedColor = Color.argb((int) (newTag.getAlpha() * 255), (int) (newTag.getColorR() * 255),
                (int) (newTag.getColorG() * 255), (int) (newTag.getColorB() * 255));
        textViewList.get(i).setTextColor(mergedColor);
        textViewList.get(i).setTextSize((int) (newTag.getTextSize() * newTag.getScale()));
        addView(textViewList.get(i));
        textViewList.get(i).setOnClickListener(OnTagClickListener(newTag.getText(), newTag.getUrl()));

        textViewList.get(i).setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                float x = e.getX();
                float y = e.getY();
                Log.d(TAG, "tv movition:x=" + x + ",y=" + y + ",action is [" + e.getAction() + "]");
                return false;
            }
        });
    }

    public boolean Replace(Tag newTag, String oldTagText) {
        boolean result = false;
        int j = mTagCloud.Replace(newTag, oldTagText);
        if (j >= 0) { // then oldTagText was found and replaced with newTag data
            Iterator it = mTagCloud.iterator();
            Tag tempTag;
            while (it.hasNext()) {
                tempTag = (Tag) it.next();
                mParams.get(tempTag.getParamNo()).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()),
                        (int) (centerY + tempTag.getLoc2DY()), 0, 0);
                textViewList.get(tempTag.getParamNo()).setText(tempTag.getText());
                textViewList.get(tempTag.getParamNo()).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
                int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255),
                        (int) (tempTag.getColorG() * 255), (int) (tempTag.getColorB() * 255));
                textViewList.get(tempTag.getParamNo()).setTextColor(mergedColor);
                textViewList.get(tempTag.getParamNo()).bringToFront();
            }
            result = true;
        }
        return result;
    }

    public void reset() {
        mTagCloud.reset();

        Iterator it = mTagCloud.iterator();
        Tag tempTag;
        while (it.hasNext()) {
            tempTag = (Tag) it.next();
            mParams.get(tempTag.getParamNo()).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()),
                    (int) (centerY + tempTag.getLoc2DY()), 0, 0);
            textViewList.get(tempTag.getParamNo()).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
            int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255),
                    (int) (tempTag.getColorG() * 255), (int) (tempTag.getColorB() * 255));
            textViewList.get(tempTag.getParamNo()).setTextColor(mergedColor);
            textViewList.get(tempTag.getParamNo()).bringToFront();
        }
    }

    View.OnClickListener OnTagClickListener(final String title, final String url) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionMove) {
                    actionMove = false;
                    return;
                }

                Animation scaleAnimation = AnimationUtils.loadAnimation(mContext, R.anim.scale);
                v.startAnimation(scaleAnimation);
                tagCallback.onClick(title, url);
                actionMove = false;
            }
        };
    }

    private String getTime() {

        return "[" + System.currentTimeMillis() + "] ";
    }

    String urlMaker(String url) {
        if ((url.substring(0, 7).equalsIgnoreCase("http://")) || (url.substring(0, 8).equalsIgnoreCase("https://")))
            return url;
        else
            return "http://" + url;
    }

    // the filter function makes sure that there all elements are having unique
    // Text field:
    List<Tag> Filter(List<Tag> tagList) {
        // current implementation is O(n^2) but since the number of tags are not
        // that many,
        // it is acceptable.
        List<Tag> tempTagList = new ArrayList();
        Iterator itr = tagList.iterator();
        Iterator itrInternal;
        Tag tempTag1, tempTag2;
        // for all elements of TagList
        while (itr.hasNext()) {
            tempTag1 = (Tag) (itr.next());
            boolean found = false;
            // go over all elements of temoTagList
            itrInternal = tempTagList.iterator();
            while (itrInternal.hasNext()) {
                tempTag2 = (Tag) (itrInternal.next());
                if (tempTag2.getText().equalsIgnoreCase(tempTag1.getText())) {
                    found = true;
                    break;
                }
            }
            if (found == false)
                tempTagList.add(tempTag1);
        }
        return tempTagList;
    }

    // for handling the click on the tags
    // onclick open the tag url in a new window. Back button will bring you back
    // to TagCloud


}
