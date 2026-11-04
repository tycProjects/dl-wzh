package com.camstream.mobile;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.view.*;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;import androidx.recyclerview.widget.*;import java.io.File;import java.util.*;
public class LibraryActivity extends AppCompatActivity {
 ArrayList<File> videos=new ArrayList<>(); RecyclerView list; TextView empty;
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_library);list=findViewById(R.id.list);empty=findViewById(R.id.empty);list.setLayoutManager(new LinearLayoutManager(this));load();}
 void load(){videos.clear();File dir=new File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES),"Recordings");File[] f=dir.listFiles((d,n)->n.toLowerCase(Locale.ROOT).endsWith(".mp4"));if(f!=null){Arrays.sort(f,(a,b)->Long.compare(b.lastModified(),a.lastModified()));videos.addAll(Arrays.asList(f));}empty.setVisibility(videos.isEmpty()?View.VISIBLE:View.GONE);list.setVisibility(videos.isEmpty()?View.GONE:View.VISIBLE);list.setAdapter(new Adapter());}
 class Adapter extends RecyclerView.Adapter<VH>{
  public VH onCreateViewHolder(ViewGroup p,int t){return new VH(getLayoutInflater().inflate(R.layout.item_video,p,false));}
  public void onBindViewHolder(VH h,int pos){File f=videos.get(pos);h.name.setText(f.getName());h.meta.setText(new Date(f.lastModified())+"  •  "+android.text.format.Formatter.formatFileSize(LibraryActivity.this,f.length()));
   h.play.setOnClickListener(v->{Intent i=new Intent(LibraryActivity.this,PlayerActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);});
   h.share.setOnClickListener(v->{Intent s=new Intent(Intent.ACTION_SEND);s.setType("video/mp4");s.putExtra(Intent.EXTRA_STREAM,androidx.core.content.FileProvider.getUriForFile(LibraryActivity.this,getPackageName()+".provider",f));s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(s,"Share recording"));});
   h.delete.setOnClickListener(v->new AlertDialog.Builder(LibraryActivity.this).setTitle("Delete recording?").setMessage("This permanently deletes "+f.getName()).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{f.delete();load();}).show());
  }
  public int getItemCount(){return videos.size();}
 }
 class VH extends RecyclerView.ViewHolder{TextView name,meta;Button play,share,delete;VH(View v){super(v);name=v.findViewById(R.id.name);meta=v.findViewById(R.id.meta);play=v.findViewById(R.id.play);share=v.findViewById(R.id.share);delete=v.findViewById(R.id.delete);}}
}
