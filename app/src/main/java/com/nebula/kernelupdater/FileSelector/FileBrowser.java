package com.nebula.kernelupdater.FileSelector;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.nebula.kernelupdater.R;
import com.nebula.kernelupdater.R.anim;
import com.nebula.kernelupdater.R.id;
import com.nebula.kernelupdater.R.layout;
import com.nebula.kernelupdater.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Mike on 9/22/2014.
 */
public class FileBrowser extends Activity {

    public static String ACTION_DIRECTORY_SELECTED = "THEMIKE10452.FB.FOLDER.SELECTED";
    public File WORKING_DIRECTORY;
    public Boolean PICK_FOLDERS_ONLY;
    Comparator<File> comparator = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            if (f1.isDirectory() && f2.isFile())
                return -2;
            else if (f1.isFile() && f2.isDirectory())
                return 2;
            else
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
        }
    };
    private ListView list;
    private ArrayList<File> items;
    private ArrayList<String> ALLOWED_EXTENSIONS;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(layout.file_browser_layout);
        this.overridePendingTransition(anim.slide_in_btt, anim.stay_still);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            if (this.getActionBar() != null)
                this.getActionBar().setElevation(5);
        }

        Bundle extras = this.getIntent().getExtras();

        try {
            this.ALLOWED_EXTENSIONS = extras.getStringArrayList("ALLOWED_EXTENSIONS");
        } catch (NullPointerException e) {
            this.ALLOWED_EXTENSIONS = null;
        }
        try {
            this.PICK_FOLDERS_ONLY = extras.getBoolean("PICK_FOLDERS_ONLY");
        } catch (NullPointerException e) {
            this.PICK_FOLDERS_ONLY = false;
        }
        try {
            Bundle bundle = new Bundle();
            bundle.putString("folder", extras.getString("START"));
            this.updateScreen(bundle);
        } catch (NullPointerException ignored) {
            this.updateScreen(null);
        }

        this.findViewById(id.btn_select).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent out = new Intent(FileBrowser.ACTION_DIRECTORY_SELECTED);
                out.putExtra("folder", FileBrowser.this.WORKING_DIRECTORY.getAbsolutePath() + File.separator);
                FileBrowser.this.sendBroadcast(out);
                FileBrowser.this.finish();
            }
        });

        this.findViewById(id.btn_cancel).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FileBrowser.this.finish();
            }
        });
    }

    public void updateScreen(Bundle pac) {
        final File root = pac == null ?
                Environment.getExternalStorageDirectory() : new File(pac.getString("folder")).isDirectory() ?
                new File(pac.getString("folder")) : Environment.getExternalStorageDirectory();

        this.WORKING_DIRECTORY = root;
        ((TextView) this.findViewById(id.textView_cd)).setText(root.getAbsolutePath());
        this.list = (ListView) this.findViewById(id.list);

        if (this.items == null)
            this.items = new ArrayList<File>();
        else
            this.items.clear();

        if (root.listFiles() != null) {
            for (File f : root.listFiles()) {
                if (f.isDirectory()) {
                    this.items.add(f);
                } else if (!this.PICK_FOLDERS_ONLY) {
                    if (this.ALLOWED_EXTENSIONS != null && f.getName().lastIndexOf('.') > 0 && this.ALLOWED_EXTENSIONS.indexOf(Tools.getFileExtension(f)) > -1) {
                        this.items.add(f);
                    } else if (this.ALLOWED_EXTENSIONS == null) {
                        this.items.add(f);
                    }
                }
            }

            Collections.sort(this.items, this.comparator);
        }

        if (root.getParentFile() != null)
            this.items.add(0, root.getParentFile());

        final Adapter myAdapter = new Adapter(this, layout.file_browser_list_item, this.items);
        this.adapter = myAdapter;
        this.list.setAdapter(myAdapter);
        this.list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (myAdapter.files.get(i).isDirectory()) {
                    if (i == 0 && root.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()))
                        return;
                    Bundle pac = new Bundle();
                    pac.putString("folder", myAdapter.files.get(i).getAbsolutePath());
                    FileBrowser.this.updateScreen(pac);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (this.WORKING_DIRECTORY.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()))
            return;
        Bundle pac = new Bundle();
        pac.putString("folder", this.adapter.files.get(0).getAbsolutePath());
        this.updateScreen(pac);
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(anim.stay_still, anim.slide_in_ttb);
    }

    @Override
    public String toString() {
        return "FileBrowser{" +
                "WORKING_DIRECTORY=" + this.WORKING_DIRECTORY +
                ", PICK_FOLDERS_ONLY=" + this.PICK_FOLDERS_ONLY +
                ", comparator=" + this.comparator +
                ", list=" + this.list +
                ", items=" + this.items +
                ", ALLOWED_EXTENSIONS=" + this.ALLOWED_EXTENSIONS +
                ", adapter=" + this.adapter +
                '}';
    }
}
