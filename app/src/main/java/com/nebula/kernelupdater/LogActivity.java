package com.nebula.kernelupdater;

import android.R.style;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.nebula.kernelupdater.FileSelector.FileBrowser;
import com.nebula.kernelupdater.R.anim;
import com.nebula.kernelupdater.R.id;
import com.nebula.kernelupdater.R.layout;
import com.nebula.kernelupdater.R.string;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Mike on 12/13/2014.
 */
public class LogActivity extends ActionBarActivity {

    private static final String DEFAULT_NAME = "last_kmsg_%s.txt";
    private static final String DEFAULT_LOC = Environment.getExternalStorageDirectory().getAbsolutePath();
    private TextView text;
    private ScrollView scrollView;
    private StringBuilder builder;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(layout.log_layout);

        assert this.getSupportActionBar() != null;
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.preferences = this.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
            this.getSupportActionBar().setElevation(5);

        this.scrollView = (ScrollView) this.findViewById(id.scroller);
        this.text = (TextView) this.findViewById(id.text);
        this.text.setTextIsSelectable(true);

        final File last_kmsg = new File("/proc/last_kmsg");
        if (!last_kmsg.exists() || !last_kmsg.isFile()) {
            this.text.setText("last_kmsg NF");
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                this.dialog = new ProgressDialog(LogActivity.this);
                this.dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                this.dialog.setIndeterminate(true);
                this.dialog.setMessage(LogActivity.this.getString(string.msg_pleaseWait));
                this.dialog.setCancelable(false);
                this.dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    LogActivity.this.builder = new StringBuilder();
                    InputStreamReader ISreader = new InputStreamReader(Runtime.getRuntime().exec("su -c cat " + last_kmsg.getAbsolutePath()).getInputStream());
                    BufferedReader reader = new BufferedReader(ISreader);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LogActivity.this.builder.append(line).append("\n\r");
                    }
                    reader.close();
                } catch (IOException e) {
                    Toast.makeText(LogActivity.this.getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                LogActivity.this.text.post(new Runnable() {
                    @Override
                    public void run() {
                        LogActivity.this.text.setText(LogActivity.this.builder.toString());
                    }
                });
                if (this.dialog != null && this.dialog.isShowing())
                    LogActivity.this.text.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            LogActivity.this.scrollView.smoothScrollTo(0, LogActivity.this.text.getBottom());
                        }
                    }, 500);
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.overridePendingTransition(anim.slide_in_rtl, anim.slide_out_rtl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(menu.log_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case id.action_save:
                save();
                break;
            case id.action_search:
                search();
                break;
            case id.action_pageDown:
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.smoothScrollTo(0, scrollView.getScrollY() + scrollView.getHeight());
                    }
                });
                break;
            case id.action_pageUp:
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.smoothScrollTo(0, scrollView.getScrollY() - scrollView.getHeight());
                    }
                });
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        final EditText name, location;
        Button save, browse;

        final Dialog d = new Dialog(this, style.Theme_DeviceDefault_Light_Dialog_MinWidth);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(layout.log_save_layout);
        d.setCancelable(true);
        d.show();

        name = (EditText) d.findViewById(id.name);
        location = (EditText) d.findViewById(id.location);
        save = (Button) d.findViewById(id.save);
        browse = (Button) d.findViewById(id.browse);

        name.setText(String.format(LogActivity.DEFAULT_NAME, new SimpleDateFormat("MM_dd__hh_mm").format(new Date())));
        location.setText(this.preferences.getString("log_save_lastused_location", LogActivity.DEFAULT_LOC));

        browse.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LogActivity.this, FileBrowser.class);
                i.putExtra("PICK_FOLDERS_ONLY", true);
                i.putExtra("START", location.getText().toString());
                LogActivity.this.startActivity(i);
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        LogActivity.this.unregisterReceiver(this);
                        try {
                            if (intent.getAction().equals(FileBrowser.ACTION_DIRECTORY_SELECTED))
                                location.setText(intent.getStringExtra("folder"));
                        } catch (Exception e) {
                            Toast.makeText(LogActivity.this.getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                };
                LogActivity.this.registerReceiver(receiver, new IntentFilter(FileBrowser.ACTION_DIRECTORY_SELECTED));
            }
        });

        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File out = new File((location.getText().toString().endsWith(File.separator) ? location.getText().toString() : location.getText() + File.separator) + name.getText());

                    if (!out.getParentFile().exists() || !out.getParentFile().isDirectory()) {
                        out.getParentFile().mkdirs();
                    }

                    if (!out.exists() || !out.isFile()) {
                        out.createNewFile();
                    }

                    PrintWriter writer = new PrintWriter(new FileWriter(out));
                    writer.print(LogActivity.this.builder);
                    writer.flush();
                    writer.close();

                    if (d.isShowing())
                        d.dismiss();

                    LogActivity.this.preferences.edit().putString("log_save_lastused_location", location.getText().toString()).apply();
                    Toast.makeText(LogActivity.this.getApplicationContext(), string.btn_ok, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(LogActivity.this.getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void search() {

        final RelativeLayout topSearch = (RelativeLayout) this.findViewById(id.topSearch);
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            topSearch.setElevation(5);

            int cx = topSearch.getRight();
            int cy = topSearch.getTop();
            int finalRadius = (int) Math.sqrt(Math.pow(topSearch.getWidth(), 2) + Math.pow(topSearch.getHeight(), 2));

            if (topSearch.getVisibility() == View.INVISIBLE) {
                Animator animator = ViewAnimationUtils.createCircularReveal(topSearch, cx, cy, 0, finalRadius);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        onAnimationEnd(animation);
                        topSearch.setVisibility(View.VISIBLE);
                    }
                });
                animator.start();
            } else {
                Animator animator = ViewAnimationUtils.createCircularReveal(topSearch, cx, cy, finalRadius, 0);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        topSearch.setVisibility(View.INVISIBLE);
                    }
                });
                animator.start();
            }
        } else {
            topSearch.setVisibility(topSearch.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
        }
        this.findViewById(id.btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                topSearch.setVisibility(View.INVISIBLE);
                InputMethodManager manager = (InputMethodManager) LogActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(topSearch.getWindowToken(), 0);

                String toFind = ((EditText) LogActivity.this.findViewById(id.searchBox)).getText().toString().trim();

                if (toFind.length() > 0) {
                    new AsyncTask<Void, Void, Boolean>() {
                        SpannableString sString;
                        ProgressDialog dialog;

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            this.dialog = new ProgressDialog(LogActivity.this);
                            this.dialog.setCancelable(false);
                            this.dialog.setIndeterminate(true);
                            this.dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            this.dialog.setMessage(LogActivity.this.getString(string.msg_pleaseWait));
                            this.dialog.show();
                        }

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            this.sString = new SpannableString(LogActivity.this.builder.toString());
                            String tmp = LogActivity.this.builder.toString();
                            ArrayList<Integer> indexes = new ArrayList<>();
                            String toFind = ((EditText) LogActivity.this.findViewById(id.searchBox)).getText().toString().trim();
                            String toRep = "";
                            for (int i = 0; i < toFind.length(); i++) {
                                toRep += " ";
                            }

                            while (tmp.contains(toFind.toLowerCase()) || tmp.contains(toFind.toUpperCase())) {
                                if (tmp.contains(toFind.toLowerCase())) {
                                    int i = tmp.indexOf(toFind.toLowerCase());
                                    indexes.add(i);
                                    tmp = tmp.replaceFirst(toFind.toLowerCase(), toRep);
                                }
                                if (tmp.contains(toFind.toUpperCase())) {
                                    int i = tmp.indexOf(toFind.toUpperCase());
                                    indexes.add(i);
                                    tmp = tmp.replaceFirst(toFind.toUpperCase(), toRep);
                                }
                            }
                            for (int i = 0; i < indexes.size(); i++) {
                                int start = indexes.get(i);
                                int end = start + toFind.length();
                                this.sString.setSpan(new BackgroundColorSpan(Color.YELLOW), start, end, 0);
                                this.sString.setSpan(new RelativeSizeSpan(2f), start, end, 0);
                            }
                            return indexes.size() > 0;
                        }

                        @Override
                        protected void onPostExecute(Boolean bool) {
                            super.onPostExecute(bool);
                            if (this.dialog != null && this.dialog.isShowing()) {
                                this.dialog.hide();
                            }
                            if (bool) {
                                LogActivity.this.text.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        LogActivity.this.text.setText(sString);
                                        Toast.makeText(LogActivity.this.getApplicationContext(), string.msg_textHighlighted, Toast.LENGTH_LONG).show();
                                    }
                                }, 100);
                            } else {
                                Toast.makeText(LogActivity.this.getApplicationContext(), string.msg_textNotFound, Toast.LENGTH_LONG).show();
                            }
                        }
                    }.execute();
                } else {
                    LogActivity.this.text.setText(LogActivity.this.builder.toString());
                }
            }
        });
    }

}
