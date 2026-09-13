package com.mr131.redplayer;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/** Saved network locations. Playback remains routed to the app's VLC fallback player. */
public final class NetworkSourcesActivity extends AppCompatActivity {
    private static final String[] SUPPORTED={"http","https","rtsp","ftp","ftps","sftp","smb","nfs","udp","rtp","mms","upnp"};
    private final ArrayList<Source> sources=new ArrayList<>();
    private ArrayAdapter<Source> adapter;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setTitle("Network Sources");load();sortSources();
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(14),dp(16),dp(14));RedPlayerBackground.apply(root);
        root.addView(LuxuryIconView.create(this,R.drawable.icon_network_thick,"Network Sources emblem"));
        TextView title=text("NETWORK & HOME SERVER",24,Color.WHITE);title.setContentDescription("Network Sources Screen");root.addView(title);
        root.addView(text("HTTP/HLS • RTSP • SMB • FTP/SFTP • NFS • UDP/RTP • UPnP",14,Color.LTGRAY));
        root.addView(text("Add a port when your server requires one, for example :8080. Login details are never saved here.",13,Color.LTGRAY));
        Button add=button("ADD NETWORK ADDRESS");add.setOnClickListener(v->showSourceEditor(null));root.addView(add);
        ListView list=new ListView(this);
        adapter=new ArrayAdapter<Source>(this,android.R.layout.simple_list_item_2,android.R.id.text1,sources){
            @Override public android.view.View getView(int p,android.view.View c,android.view.ViewGroup g){
                android.view.View row=super.getView(p,c,g);TextView primary=row.findViewById(android.R.id.text1);TextView secondary=row.findViewById(android.R.id.text2);Source source=getItem(p);
                primary.setText((source.favorite?"★  ":"")+source.name);primary.setTextColor(Color.WHITE);primary.setTextSize(17);secondary.setText(source.address);secondary.setTextColor(Color.LTGRAY);row.setMinimumHeight(dp(72));return row;
            }
        };
        list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->play(sources.get(i)));
        list.setOnItemLongClickListener((p,v,i,id)->{showActions(sources.get(i));return true;});
        root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void showSourceEditor(Source existing){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),dp(4),dp(18),0);
        EditText name=input("Display name, for example Living Room PC");EditText address=input("Address, for example smb://192.168.1.20:445/Videos");address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        if(existing!=null){name.setText(existing.name);address.setText(existing.address);}form.addView(name);form.addView(address);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Add network address":"Edit network address").setView(form).setNegativeButton("Cancel",null).setPositiveButton(existing==null?"Save & Play":"Save",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String label=name.getText().toString().trim(),value=address.getText().toString().trim();String problem=validationProblem(value);
            if(problem!=null){toast(problem);return;}if(label.isEmpty())label=defaultName(value);
            Source duplicate=findAddress(value,existing);if(duplicate!=null){toast("That address is already saved as "+duplicate.name);return;}
            if(existing==null){Source added=new Source(label,value,false,System.currentTimeMillis());sources.add(added);save();refresh();dialog.dismiss();play(added);}
            else{existing.name=label;existing.address=value;save();refresh();dialog.dismiss();}
        }));dialog.show();
    }

    private void showActions(Source source){
        String favorite=source.favorite?"Remove from favorites":"Add to favorites";
        new AlertDialog.Builder(this).setTitle(source.name).setItems(new String[]{favorite,"Edit","Remove"},(dialog,which)->{
            if(which==0){source.favorite=!source.favorite;save();refresh();}
            else if(which==1)showSourceEditor(source);
            else confirmRemove(source);
        }).setNegativeButton("Cancel",null).show();
    }

    private void confirmRemove(Source source){new AlertDialog.Builder(this).setTitle("Remove saved address?").setMessage(source.name+"\n"+source.address).setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{sources.remove(source);save();refresh();}).show();}

    private String validationProblem(String value){
        if(value.isEmpty())return "Enter a network address";
        Uri uri=Uri.parse(value);String scheme=uri.getScheme();
        if(scheme==null||!supported(scheme))return "Unsupported address. Use HTTP, HTTPS, RTSP, SMB, FTP, SFTP, NFS, UDP, RTP, MMS, or UPnP.";
        if(uri.getUserInfo()!=null)return "Remove the username or password. Login details cannot be saved in an address.";
        String authority=uri.getEncodedAuthority();if(authority==null||authority.trim().isEmpty())return "Add a server name or network address after "+scheme.toLowerCase(Locale.US)+"://";
        String portProblem=portProblem(authority);if(portProblem!=null)return portProblem;
        return null;
    }

    private String portProblem(String authority){
        String portText=null;
        if(authority.startsWith("[")){int close=authority.indexOf(']');if(close<0)return "The server address is not valid";String rest=authority.substring(close+1);if(!rest.isEmpty()){if(!rest.startsWith(":"))return "The server address is not valid";portText=rest.substring(1);}}
        else{int colon=authority.lastIndexOf(':');if(colon>=0)portText=authority.substring(colon+1);}
        if(portText==null)return null;if(portText.isEmpty())return "Enter a port number after the colon";
        for(int i=0;i<portText.length();i++)if(!Character.isDigit(portText.charAt(i)))return "The port must be a number between 1 and 65535";
        try{int port=Integer.parseInt(portText);if(port<1||port>65535)return "The port must be between 1 and 65535";}catch(NumberFormatException ignored){return "The port must be between 1 and 65535";}
        return null;
    }

    private boolean supported(String scheme){for(String candidate:SUPPORTED)if(candidate.equalsIgnoreCase(scheme))return true;return false;}
    private Source findAddress(String value,Source except){for(Source source:sources)if(source!=except&&source.address.equalsIgnoreCase(value))return source;return null;}
    private String defaultName(String value){Uri uri=Uri.parse(value);String host=uri.getHost();if(host==null||host.isEmpty())host=uri.getEncodedAuthority();return host==null?"Network source":host;}

    private void play(Source source){source.lastUsed=System.currentTimeMillis();save();Intent result=new Intent().setData(Uri.parse(source.address));result.putExtra("title",source.name);setResult(RESULT_OK,result);finish();}
    private void refresh(){sortSources();adapter.notifyDataSetChanged();}
    private void sortSources(){Collections.sort(sources,new Comparator<Source>(){@Override public int compare(Source a,Source b){if(a.favorite!=b.favorite)return a.favorite?-1:1;return Long.compare(b.lastUsed,a.lastUsed);}});}

    private void load(){
        boolean rewrite=false;
        try{JSONArray a=new JSONArray(getSharedPreferences("red_player",MODE_PRIVATE).getString("network_sources","[]"));for(int i=0;i<a.length();i++){
            Object item=a.get(i);if(item instanceof JSONObject){JSONObject o=(JSONObject)item;String address=o.optString("address","");if(validationProblem(address)==null)sources.add(new Source(o.optString("name",defaultName(address)),address,o.optBoolean("favorite",false),o.optLong("lastUsed",0)));else rewrite=true;}
            else if(item instanceof String){rewrite=true;String address=(String)item;if(validationProblem(address)==null)sources.add(new Source(defaultName(address),address,false,0));}
        }}catch(Exception ignored){rewrite=true;}if(rewrite)save();
    }
    private void save(){try{JSONArray a=new JSONArray();for(Source source:sources){JSONObject o=new JSONObject();o.put("name",source.name);o.put("address",source.address);o.put("favorite",source.favorite);o.put("lastUsed",source.lastUsed);a.put(o);}getSharedPreferences("red_player",MODE_PRIVATE).edit().putString("network_sources",a.toString()).apply();}catch(Exception ignored){toast("Could not save network sources");}}

    private EditText input(String hint){EditText field=new EditText(this);field.setHint(hint);field.setSingleLine(true);field.setTextColor(Color.WHITE);field.setHintTextColor(Color.LTGRAY);field.setBackgroundColor(getColor(R.color.carbon));field.setPadding(dp(12),dp(12),dp(12),dp(12));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(58));params.setMargins(0,dp(8),0,0);field.setLayoutParams(params);return field;}
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setTextColor(Color.WHITE);b.setBackgroundColor(getColor(R.color.red_player));b.setMinHeight(dp(52));return b;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(8),0,dp(8));return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void toast(String v){Toast.makeText(this,v,Toast.LENGTH_LONG).show();}

    private static final class Source {
        String name,address;boolean favorite;long lastUsed;
        Source(String name,String address,boolean favorite,long lastUsed){this.name=name;this.address=address;this.favorite=favorite;this.lastUsed=lastUsed;}
        @Override public String toString(){return name;}
    }
}
