package com.mr131.redplayer;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Comparator;
import java.util.Set;

/** Local library with folders, favorites, a persistent playlist and file actions. */
public final class LibraryActivity extends AppCompatActivity {
    private final ArrayList<VideoEntry> all=new ArrayList<>(),shown=new ArrayList<>();
    private final ArrayList<String> labels=new ArrayList<>();
    private ArrayAdapter<String> adapter;private TextView count;private String query="";private int mode=0;
    private Set<String> favorites,playlist;private Uri pendingUri;private String pendingName;private boolean pendingDelete;
    private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{if(ok)load();else count.setText("Video permission is off. Browse Files still works.");});
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{if(uri!=null)choose(uri);});
    private final ActivityResultLauncher<IntentSenderRequest> approval=registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),r->{if(r.getResultCode()==RESULT_OK){if(pendingDelete){load();toast("Video moved to Trash");}else renameNow(pendingUri,pendingName);}pendingUri=null;pendingName=null;});

    @Override protected void onCreate(Bundle state){super.onCreate(state);setTitle("Video Library");favorites=new HashSet<>(prefs().getStringSet("favorites",new HashSet<>()));playlist=new HashSet<>(prefs().getStringSet("saved_playlist",new HashSet<>()));setContentView(screen());request();}
    private View screen(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(8),dp(12),dp(8));RedPlayerBackground.apply(root);root.addView(LuxuryIconView.create(this,R.drawable.icon_library_thick,"Video Library emblem"));TextView title=text("VIDEO LIBRARY",24,Color.WHITE);title.setContentDescription("Video Library");root.addView(title);count=text("Scanning phone…",13,Color.LTGRAY);root.addView(count);SearchView search=new SearchView(this);search.setQueryHint("Search videos or folders");search.setIconifiedByDefault(false);search.setOnQueryTextListener(new SearchView.OnQueryTextListener(){public boolean onQueryTextSubmit(String q){filter(q);return true;}public boolean onQueryTextChange(String q){filter(q);return true;}});root.addView(search,new LinearLayout.LayoutParams(-1,dp(54)));LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER);tabs.addView(tab("ALL",0));tabs.addView(tab("FAVORITES",1));tabs.addView(tab("PLAYLIST",2));tabs.addView(tab("FOLDERS",3));root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(46)));LinearLayout actions=new LinearLayout(this);Button browse=new Button(this);browse.setText("BROWSE FILES");browse.setTextColor(Color.WHITE);browse.setBackgroundColor(getColor(R.color.red_player));browse.setOnClickListener(v->picker.launch(new String[]{"video/*"}));actions.addView(browse,new LinearLayout.LayoutParams(0,dp(46),2));Button sort=new Button(this);sort.setText("SORT");sort.setTextColor(Color.WHITE);sort.setBackgroundColor(getColor(R.color.carbon));sort.setOnClickListener(v->showSort());actions.addView(sort,new LinearLayout.LayoutParams(0,dp(46),1));Button trash=new Button(this);trash.setText("TRASH");trash.setTextColor(Color.WHITE);trash.setBackgroundColor(getColor(R.color.carbon));trash.setOnClickListener(v->startActivity(new Intent(this,TrashActivity.class)));actions.addView(trash,new LinearLayout.LayoutParams(0,dp(46),1));root.addView(actions);ListView list=new ListView(this);list.setDividerHeight(1);adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,labels){@NonNull public View getView(int p,View c,@NonNull android.view.ViewGroup g){TextView row=(TextView)super.getView(p,c,g);row.setTextColor(Color.WHITE);row.setTextSize(15);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinHeight(dp(66));return row;}};list.setAdapter(adapter);list.setOnItemClickListener((p,v,i,id)->{if(mode==3){String folder=shown.get(i).folder;mode=0;filter(folder);}else choose(shown.get(i).uri);});list.setOnItemLongClickListener((p,v,i,id)->{actions(v,shown.get(i));return true;});root.addView(list,new LinearLayout.LayoutParams(-1,0,1));return root;}
    private Button tab(String label,int value){Button b=new Button(this);b.setText(label);b.setTextSize(10);b.setTextColor(Color.WHITE);b.setBackgroundColor(value==0?getColor(R.color.red_player):getColor(R.color.carbon));b.setOnClickListener(v->{mode=value;filter(query);});b.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1));return b;}
    private void request(){String p=Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_VIDEO:Manifest.permission.READ_EXTERNAL_STORAGE;if(ContextCompat.checkSelfPermission(this,p)==PackageManager.PERMISSION_GRANTED)load();else permission.launch(p);}
    private void load(){new Thread(()->{ArrayList<VideoEntry> found=new ArrayList<>();String[] cols={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.BUCKET_DISPLAY_NAME,MediaStore.Video.Media.DURATION,MediaStore.Video.Media.SIZE};try(Cursor c=getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cols,null,null,MediaStore.Video.Media.DATE_ADDED+" DESC")){if(c!=null)while(c.moveToNext()){Uri uri=Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,String.valueOf(c.getLong(0)));found.add(new VideoEntry(uri,c.getString(1)==null?"Video":c.getString(1),c.getString(2)==null?"Phone":c.getString(2),c.getLong(3),c.getLong(4)));}}runOnUiThread(()->{all.clear();all.addAll(found);filter(query);});},"library-scan").start();}
    private void filter(String q){query=q==null?"":q.trim().toLowerCase(Locale.US);shown.clear();labels.clear();HashSet<String> folders=new HashSet<>();for(VideoEntry v:all){String id=v.uri.toString();if((mode==1&&!favorites.contains(id))||(mode==2&&!playlist.contains(id)))continue;if(!query.isEmpty()&&!v.name.toLowerCase(Locale.US).contains(query)&&!v.folder.toLowerCase(Locale.US).contains(query))continue;if(mode==3&&!folders.add(v.folder))continue;shown.add(v);String mark=favorites.contains(id)?"★ ":"";String add=playlist.contains(id)?"  •  PLAYLIST":"";labels.add(mark+(mode==3?v.folder:v.name+"\n"+v.folder+"  •  "+time(v.duration)+"  •  "+size(v.size)+add));}adapter.notifyDataSetChanged();count.setText(shown.size()+(mode==3?" folders":" videos"));}
    private void actions(View anchor,VideoEntry v){PopupMenu m=new PopupMenu(this,anchor);m.getMenu().add("Play");m.getMenu().add(favorites.contains(v.uri.toString())?"Remove favorite":"Add favorite");m.getMenu().add(playlist.contains(v.uri.toString())?"Remove from playlist":"Add to playlist");m.getMenu().add("Share");m.getMenu().add("Rename");m.getMenu().add("Delete");m.setOnMenuItemClickListener(item->{String x=item.getTitle().toString();if(x.equals("Play"))choose(v.uri);else if(x.contains("favorite")){toggle(favorites,v.uri.toString(),"favorites");filter(query);}else if(x.contains("playlist")){toggle(playlist,v.uri.toString(),"saved_playlist");filter(query);}else if(x.equals("Share"))share(v);else if(x.equals("Rename"))rename(v);else if(x.equals("Delete"))confirmDelete(v);return true;});m.show();}
    private void showSort(){String[] labels={"Newest first","Name A–Z","Largest first","Longest first"};new AlertDialog.Builder(this).setTitle("Sort videos").setItems(labels,(d,w)->{if(w==0)load();else{if(w==1)all.sort(Comparator.comparing(v->v.name.toLowerCase(Locale.US)));if(w==2)all.sort((a,b)->Long.compare(b.size,a.size));if(w==3)all.sort((a,b)->Long.compare(b.duration,a.duration));filter(query);}}).show();}
    private void toggle(Set<String> set,String id,String key){if(!set.add(id))set.remove(id);prefs().edit().putStringSet(key,new HashSet<>(set)).apply();}
    private void share(VideoEntry v){startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("video/*").putExtra(Intent.EXTRA_STREAM,v.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Share video"));}
    private void rename(VideoEntry v){EditText input=new EditText(this);input.setText(v.name);input.setTextColor(Color.WHITE);new AlertDialog.Builder(this).setTitle("Rename video").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Rename",(d,w)->requestWrite(v.uri,input.getText().toString().trim(),false)).show();}
    private void confirmDelete(VideoEntry v){new AlertDialog.Builder(this).setTitle("Move video to Trash?").setMessage(v.name+"\n\nYou can recover it from the Trash screen.").setNegativeButton("Cancel",null).setPositiveButton("Move to Trash",(d,w)->requestWrite(v.uri,null,true)).show();}
    private void requestWrite(Uri uri,String name,boolean delete){pendingUri=uri;pendingName=name;pendingDelete=delete;if(Build.VERSION.SDK_INT>=30){try{PendingIntent p=delete?MediaStore.createTrashRequest(getContentResolver(),java.util.Collections.singletonList(uri),true):MediaStore.createWriteRequest(getContentResolver(),java.util.Collections.singletonList(uri));approval.launch(new IntentSenderRequest.Builder(p.getIntentSender()).build());}catch(Exception e){toast("Phone permission request failed");}}else if(delete)deleteNow(uri);else renameNow(uri,name);}
    private void deleteNow(Uri uri){try{getContentResolver().delete(uri,null,null);favorites.remove(uri.toString());playlist.remove(uri.toString());prefs().edit().putStringSet("favorites",new HashSet<>(favorites)).putStringSet("saved_playlist",new HashSet<>(playlist)).apply();load();toast("Video deleted");}catch(Exception e){toast("Video could not be deleted");}}
    private void renameNow(Uri uri,String name){if(name==null||name.isEmpty())return;try{ContentValues v=new ContentValues();v.put(MediaStore.Video.Media.DISPLAY_NAME,name);getContentResolver().update(uri,v,null,null);load();toast("Video renamed");}catch(Exception e){toast("Video could not be renamed");}}
    private void choose(Uri uri){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}setResult(RESULT_OK,new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));finish();}
    private android.content.SharedPreferences prefs(){return getSharedPreferences("red_player",MODE_PRIVATE);}private TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(5),0,dp(5));return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}private String time(long ms){long s=ms/1000;return String.format(Locale.US,"%d:%02d",s/60,s%60);}private String size(long b){return b>=1073741824L?String.format(Locale.US,"%.1f GB",b/1073741824f):String.format(Locale.US,"%.0f MB",b/1048576f);}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private static final class VideoEntry{final Uri uri;final String name,folder;final long duration,size;VideoEntry(Uri u,String n,String f,long d,long s){uri=u;name=n;folder=f;duration=d;size=s;}}
}
