package com.mr131.redplayer;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public final class CloudImportActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String[]> cloudPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::returnVideo);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Cloud Import");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(28),dp(28),dp(28));RedPlayerBackground.apply(root);
        TextView title=text("GOOGLE DRIVE & CLOUD",25,Color.WHITE);title.setContentDescription("Cloud Import Screen 17");root.addView(title);
        root.addView(text("Choose Google Drive, OneDrive, Dropbox, or another connected storage provider. The video can stream without being copied into the app.",16,Color.LTGRAY));
        Button open=new Button(this);open.setText("OPEN CLOUD FILES");open.setTextColor(Color.WHITE);open.setBackgroundColor(Color.rgb(214,10,0));open.setOnClickListener(v->cloudPicker.launch(new String[]{"video/*","application/vnd.apple.mpegurl","application/x-mpegURL"}));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));p.setMargins(0,dp(22),0,0);root.addView(open,p);setContentView(root);
    }
    private void returnVideo(Uri uri){if(uri==null)return;try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}setResult(RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));finish();}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(10),0,dp(10));return t;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
