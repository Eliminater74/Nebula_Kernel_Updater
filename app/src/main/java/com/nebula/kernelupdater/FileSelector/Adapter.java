package com.nebula.kernelupdater.FileSelector;

import android.R.id;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nebula.kernelupdater.R;
import com.nebula.kernelupdater.R.drawable;
import com.nebula.kernelupdater.R.layout;
import com.nebula.kernelupdater.Tools;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Mike on 9/22/2014.
 */
public class Adapter extends ArrayAdapter {

    public ArrayList<File> files;
    private final Context context;

    public Adapter(Context context, int resource, ArrayList<File> files) {
        super(context, resource, files);
        this.files = files;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = ((LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout.file_browser_list_item, null);

        if (this.files.get(position).isDirectory()) {
            convertView.findViewById(R.id.imageView1).setBackground(this.context.getResources().getDrawable(drawable.ic_folder_white_24dp));
        } else if (Tools.getFileExtension(this.files.get(position)).equalsIgnoreCase(".zip")) {
            convertView.findViewById(R.id.imageView1).setBackground(this.context.getResources().getDrawable(drawable.archive_blue));
        }
        ((TextView) convertView.findViewById(id.text1)).setText(this.files.get(position).isDirectory() ? position == 0 ? ".." : File.separator + this.files.get(position).getName() : this.files.get(position).getName());

        return convertView;
    }

    @Override
    public String toString() {
        return "Adapter{" +
                "files=" + this.files +
                ", context=" + this.context +
                '}';
    }
}
