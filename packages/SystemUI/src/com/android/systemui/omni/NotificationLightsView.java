/*
* Copyright (C) 2019 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.android.systemui.omni;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.app.WallpaperInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.palette.graphics.Palette;

import com.android.settingslib.Utils;
import com.android.systemui.R;

public class NotificationLightsView extends RelativeLayout {
    private static final boolean DEBUG = false;
    private static final String TAG = "NotificationLightsView";
    private ValueAnimator mLightAnimator;
    private WallpaperManager mWallManager;

    public NotificationLightsView(Context context) {
        this(context, null);
    }

    public NotificationLightsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationLightsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationLightsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (DEBUG) Log.d(TAG, "new");
    }

    public void stopAnimateNotification() {
        if (mLightAnimator != null) {
            mLightAnimator.end();
            mLightAnimator = null;
        }
    }

    public int getNotificationLightsColor() {
        boolean useAccent = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.OMNI_AMBIENT_NOTIFICATION_LIGHT_ACCENT,
                0, UserHandle.USER_CURRENT) != 0;
        int color = getDefaultNotificationLightsColor(); // custom color (fallback)
        if (useAccent) {  // follow accent
            color = Utils.getColorAccentDefaultColor(getContext());
        } else if (Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.PULSE_AMBIENT_AUTO_COLOR, 0,
                UserHandle.USER_CURRENT) == 1) { // follow wallapaper
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
                if (wallpaperInfo == null) { // if not a live wallpaper
                    Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                    Bitmap bitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                    if (bitmap != null) { // if wallpaper is not blank
                        Palette p = Palette.from(bitmap).generate();
                        int wallColor = p.getDominantColor(color);
                        if (color != wallColor)
                            color = wallColor;
                    }
                }
            } catch (Exception e) {
                // Nothing to do
            }
        }
        return color;
    }

    public int getDefaultNotificationLightsColor() {
        int defaultColor = Utils.getColorAccentDefaultColor(getContext());
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.PULSE_AMBIENT_LIGHT_COLOR, defaultColor,
                    UserHandle.USER_CURRENT);
    }

    public void animateNotificationWithColor(int color) {
        int duration = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.PULSE_AMBIENT_LIGHT_DURATION, 2,
                UserHandle.USER_CURRENT) * 1000;
        int repeat = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.PULSE_AMBIENT_LIGHT_REPEAT_COUNT, 0,
                UserHandle.USER_CURRENT);
        ImageView leftView = (ImageView) findViewById(R.id.notification_animation_left);
        ImageView rightView = (ImageView) findViewById(R.id.notification_animation_right);
        leftView.setColorFilter(color);
        rightView.setColorFilter(color);
        mLightAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 2.0f});
        mLightAnimator.setDuration(duration);
        //Infinite animation only on Always On Notifications
        if (repeat == 0) {
            mLightAnimator.setRepeatCount(ValueAnimator.INFINITE);
        } else {
            mLightAnimator.setRepeatCount(repeat);
        }
        mLightAnimator.setRepeatMode(ValueAnimator.RESTART);
        mLightAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (DEBUG) Log.d(TAG, "onAnimationUpdate");
                float progress = ((Float) animation.getAnimatedValue()).floatValue();
                leftView.setScaleY(progress);
                rightView.setScaleY(progress);
                float alpha = 1.0f;
                if (progress <= 0.3f) {
                    alpha = progress / 0.3f;
                } else if (progress >= 1.0f) {
                    alpha = 2.0f - progress;
                }
                leftView.setAlpha(alpha);
                rightView.setAlpha(alpha);
            }
        });
        if (DEBUG) Log.d(TAG, "start");
        mLightAnimator.start();
    }

}
