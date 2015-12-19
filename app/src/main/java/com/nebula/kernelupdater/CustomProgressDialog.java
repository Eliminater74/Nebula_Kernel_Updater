package com.nebula.kernelupdater;

import android.R.style;
import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nebula.kernelupdater.R.id;
import com.nebula.kernelupdater.R.layout;

/**
 * Created by Mike on 9/22/2014.
 */
public class CustomProgressDialog extends Dialog {

    public static final String UNIT = " MB";
    private final ProgressBar progressBar;
    private int MAX;
    private final TextView FILENAME;
    private final TextView FILESIZE;
    private final TextView DOWNLOADED;
    private final TextView PERCENTAGE;

    public CustomProgressDialog(Context context) {
        super(context, style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(layout.progress_dialog_layout);
        this.MAX = 100;
        this.progressBar = (ProgressBar) this.findViewById(id.progressBar);
        this.FILENAME = (TextView) this.findViewById(id.textView_filename);
        this.FILESIZE = (TextView) this.findViewById(id.textView_filesize);
        this.DOWNLOADED = (TextView) this.findViewById(id.textView_downloaded);
        this.PERCENTAGE = (TextView) this.findViewById(id.percentage);

        this.progressBar.setMax(this.MAX);
    }

    public void update(CharSequence filename, String downloaded, String filesize) {
        this.FILENAME.setText(filename);
        this.FILESIZE.setText(filesize + CustomProgressDialog.UNIT);
        this.DOWNLOADED.setText(downloaded + CustomProgressDialog.UNIT);
    }

    public void setProgress(int percentage) {
        if (percentage < 0)
            return;
        this.progressBar.setProgress(percentage);
        this.PERCENTAGE.setText(percentage + "%");
    }

    public void setMax(int max) {
        this.progressBar.setMax(this.MAX = max);
    }

    public void setIndeterminate(boolean b) {
        this.progressBar.setIndeterminate(b);
    }

    @Override
    public String toString() {
        return "CustomProgressDialog{" +
                "progressBar=" + this.progressBar +
                ", MAX=" + this.MAX +
                ", FILENAME=" + this.FILENAME +
                ", FILESIZE=" + this.FILESIZE +
                ", DOWNLOADED=" + this.DOWNLOADED +
                ", PERCENTAGE=" + this.PERCENTAGE +
                '}';
    }
}
