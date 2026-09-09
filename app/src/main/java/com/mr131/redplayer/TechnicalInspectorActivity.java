package com.mr131.redplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public final class TechnicalInspectorActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);setTitle("Technical Inspector");ScrollView scroll=new ScrollView(this);RedPlayerBackground.apply(scroll);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(16),dp(18),dp(20));
        root.addView(LuxuryIconView.create(this,R.drawable.icon_info_thick,"Technical information emblem"));TextView title=text("VIDEO TECHNICAL INSPECTOR",24,Color.WHITE);title.setContentDescription("Technical Inspector Screen 12");root.addView(title);String details=getIntent().getStringExtra("details");if(details==null)details="Open a video to inspect its resolution, frame rate, bitrate, codecs, audio channels, sample rate, decoder, duration, and location.";root.addView(text(details,16,Color.LTGRAY));scroll.addView(root);setContentView(scroll);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.START);t.setPadding(0,dp(8),0,dp(8));t.setLineSpacing(4,1);return t;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
