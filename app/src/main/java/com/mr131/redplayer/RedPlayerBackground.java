package com.mr131.redplayer;

import android.view.View;

public final class RedPlayerBackground {
    private RedPlayerBackground() {}
    public static void apply(View view) {
        if (view != null) view.setBackground(new IconicPatternDrawable());
    }
}
