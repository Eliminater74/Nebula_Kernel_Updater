package com.nebula.kernelupdater;

import android.R.style;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewManager;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.nebula.kernelupdater.FileSelector.FileBrowser;
import com.nebula.kernelupdater.R.anim;
import com.nebula.kernelupdater.R.id;
import com.nebula.kernelupdater.R.layout;
import com.nebula.kernelupdater.R.string;
import com.nebula.kernelupdater.Services.BackgroundAutoCheckService;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Mike on 9/22/2014.
 */
public class Settings extends AppCompatActivity {

    private Activity activity;
    private String DEVICE_PART;
    private boolean screenUpdating;

    private TextView AC_H, AC_M;
    private final TextWatcher intervalChanger = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence sequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence sequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            Settings.this.findViewById(id.editText_autocheck_h).post(new Runnable() {
                @Override
                public void run() {
                    String hours = Settings.this.AC_H.getText().toString(), minutes = Settings.this.AC_M.getText().toString();
                    if (Tools.isAllDigits(hours) && Integer.parseInt(hours) == 0 && Tools.isAllDigits(minutes) && Integer.parseInt(minutes) == 0)
                        return;
                    Main.preferences.edit().putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, (hours.length() > 0 ? hours : "0") + ':' + (minutes.length() > 0 ? minutes : "0")).apply();
                }
            });
        }
    };
    private final OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            Settings.this.updateScreen();
            if (s.equals(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL) && Main.preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true)) {
                Intent intent = new Intent(Settings.this, BackgroundAutoCheckService.class);
                Settings.this.stopService(intent);
                Settings.this.startService(intent);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        this.activity = this;
        this.overridePendingTransition(anim.slide_in_rtl, anim.slide_out_rtl);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(layout.settings_layout);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            if (this.getSupportActionBar() != null)
                this.getSupportActionBar().setElevation(5);
        }

        ((CompoundButton) this.findViewById(id.checkbox_useProxy)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USEPROXY, b).apply();
                Settings.this.findViewById(id.title0).setEnabled(b);
                Settings.this.findViewById(id.btn_editProxy).setEnabled(b);
                if (!Settings.this.screenUpdating)
                    Toast.makeText(Settings.this.getApplicationContext(), string.msg_restartApplication, Toast.LENGTH_LONG).show();
            }
        });

        this.findViewById(id.btn_editProxy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog d = new Dialog(Settings.this);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(layout.dialog_proxy);
                d.setCancelable(true);
                d.show();

                final EditText IP = (EditText) d.findViewById(id.ip), PORT = (EditText) d.findViewById(id.port);
                String currentHost = Main.preferences.getString(Keys.KEY_SETTINGS_PROXYHOST, Keys.DEFAULT_PROXY);
                IP.setText(currentHost.substring(0, currentHost.indexOf(':')));
                PORT.setText(currentHost.substring(currentHost.indexOf(':') + 1));

                SpannableString ss0 = new SpannableString(Settings.this.getString(string.proxy_list));
                ss0.setSpan(new UnderlineSpan(), 0, ss0.length(), 0);
                ((TextView) d.findViewById(id.pl)).setText(ss0);

                SpannableString ss1 = new SpannableString(Settings.this.getString(string.defaultt));
                ss1.setSpan(new UnderlineSpan(), 0, ss1.length(), 0);
                ((TextView) d.findViewById(id.dp)).setText(ss1);

                d.findViewById(id.dp).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String host = Keys.DEFAULT_PROXY;
                        IP.setText(host.substring(0, host.indexOf(':')));
                        PORT.setText(host.substring(host.indexOf(':') + 1));
                    }
                });

                d.findViewById(id.pl).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Keys.PROXY_LIST));
                        Settings.this.startActivity(intent);
                    }
                });

                d.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        String ip = IP.getText().toString(), port = PORT.getText().toString();
                        if (Tools.validateIP(ip) && port.length() > 0) {
                            Main.preferences.edit().putString(Keys.KEY_SETTINGS_PROXYHOST, ip + ':' + port).apply();
                            Toast.makeText(Settings.this.getApplicationContext(), string.msg_restartApplication, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(Settings.this.getApplicationContext(), string.msg_invalidProxy, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        ((CompoundButton) this.findViewById(id.checkbox_useAndDM)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USEANDM, b).apply();
                Settings.this.findViewById(id.title1).setEnabled(b);
            }
        });

        this.findViewById(id.btn_dlLoc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Settings.this, FileBrowser.class);
                i.putExtra("START", Main.preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, ""));
                i.putExtra("PICK_FOLDERS_ONLY", true);
                Settings.this.startActivity(i);
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(FileBrowser.ACTION_DIRECTORY_SELECTED)) {
                            String newF = intent.getStringExtra("folder");
                            Main.preferences.edit().putString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, newF).apply();
                            ((TextView) Settings.this.findViewById(id.textView_dlLoc)).setText(newF);
                            Settings.this.unregisterReceiver(this);
                        }
                    }
                };
                Settings.this.registerReceiver(receiver, new IntentFilter(FileBrowser.ACTION_DIRECTORY_SELECTED));
            }
        });

        this.findViewById(id.btn_upSrc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentSource = Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE);

                if (Keys.DEFAULT_SOURCE.equalsIgnoreCase(currentSource))
                    currentSource = "";

                Object[] obj = Settings.this.showDialog(Settings.this.getString(string.prompt_blankDefault), "http://", currentSource);
                Dialog dialog = (Dialog) obj[0];
                final EditText editText = (EditText) obj[1];
                dialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        String newSource = editText.getText().toString().trim();
                        if (newSource.length() == 0) {
                            newSource = Keys.DEFAULT_SOURCE;
                        }
                        Main.preferences.edit().putString(Keys.KEY_SETTINGS_SOURCE, newSource).apply();
                    }
                });
            }
        });

        ((CompoundButton) this.findViewById(id.checkbox_useStaticFilename)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                Settings.this.findViewById(id.title2).setEnabled(b);
                Settings.this.findViewById(id.btn_staticFilename).setEnabled(b);
                Settings.this.findViewById(id.btn_staticFilename).setClickable(b);

                if (!b) {
                    Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false).apply();
                    return;
                }

                if (Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, null) != null) {
                    Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, true).apply();
                    return;
                }

                Object[] obj = Settings.this.showDialog(Settings.this.getString(string.prompt_zipExtension), "filename.zip", Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, null));
                Dialog dialog = (Dialog) obj[0];
                final EditText editText = (EditText) obj[1];
                dialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        String newName = editText.getText().toString().trim();
                        if (newName.length() < 1 || newName.equalsIgnoreCase(".zip")) {
                            Toast.makeText(Settings.this.getApplicationContext(), string.msg_invalidFilename, Toast.LENGTH_LONG).show();
                            Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false).apply();
                            Settings.this.updateScreen();
                            return;
                        }
                        Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, true).apply();
                        Main.preferences.edit().putString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, newName).apply();
                    }
                });
            }
        });

        ((CompoundButton) this.findViewById(id.checkbox_receiveBeta)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, b).apply();
                Settings.this.findViewById(id.title3).setEnabled(b);
            }
        });

        this.findViewById(id.btn_staticFilename).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object[] obj = Settings.this.showDialog(Settings.this.getString(string.prompt_zipExtension), "filename.zip", Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, null));
                Dialog dialog = (Dialog) obj[0];
                final EditText editText = (EditText) obj[1];
                dialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        String newName = editText.getText().toString().trim();
                        if (newName.length() < 1 || newName.equalsIgnoreCase(".zip")) {
                            Toast.makeText(Settings.this.getApplicationContext(), string.msg_invalidFilename, Toast.LENGTH_LONG).show();
                            return;
                        } else
                            Main.preferences.edit().putString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, editText.getText().toString().trim()).apply();
                    }
                });
            }
        });

        this.findViewById(id.btn_romBase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {

                    ProgressDialog d;
                    String versions;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        this.d = new ProgressDialog(Settings.this);
                        this.d.setMessage(Settings.this.getString(string.msg_pleaseWait));
                        this.d.setIndeterminate(true);
                        this.d.setCancelable(false);
                        this.d.show();
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {

                            if (!Settings.this.getDevicePart())
                                throw new Exception(Settings.this.getString(string.msg_device_not_supported));

                            Scanner s = new Scanner(Settings.this.DEVICE_PART);
                            while (s.hasNextLine()) {
                                String line = s.nextLine();
                                if (line.startsWith("#define") && line.contains(Keys.KEY_DEFINE_BB)) {
                                    this.versions = line.split("=")[1];
                                    break;
                                }
                            }

                            s.close();

                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(Settings.this.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        this.d.dismiss();

                        if (this.versions == null)
                            return;

                        final String[] choices = this.versions.split(",");
                        for (int i = 0; i < choices.length; i++) {
                            choices[i] = choices[i].trim();
                        }
                        Dialog d = new Builder(Settings.this)
                                .setTitle(string.prompt_DevBranch)
                                .setSingleChoiceItems(choices, Tools.findIndex(choices, Main.preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "null")), new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Main.preferences.edit().putString(Keys.KEY_SETTINGS_ROMBASE, choices[i]).apply();
                                    }
                                })
                                .setCancelable(false)
                                .setPositiveButton(string.btn_ok, null)
                                .show();
                        Tools.userDialog = d;
                    }
                }.execute();
            }
        });

        this.findViewById(id.btn_romApi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {

                    ProgressDialog d;
                    String versions;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        this.d = new ProgressDialog(Settings.this);
                        this.d.setMessage(Settings.this.getString(string.msg_pleaseWait));
                        this.d.setIndeterminate(true);
                        this.d.setCancelable(false);
                        this.d.show();
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {

                            if (!Settings.this.getDevicePart())
                                throw new Exception(Settings.this.getString(string.msg_device_not_supported));

                            Scanner s = new Scanner(Settings.this.DEVICE_PART);
                            while (s.hasNextLine()) {
                                String line = s.nextLine();
                                if (line.startsWith("#define") && line.contains(Keys.KEY_DEFINE_AV)) {
                                    this.versions = line.split("=")[1];
                                    break;
                                }
                            }

                            s.close();

                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(Settings.this.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        this.d.dismiss();

                        if (this.versions == null)
                            return;

                        final String[] choices = this.versions.split(",");
                        for (int i = 0; i < choices.length; i++) {
                            choices[i] = choices[i].trim();
                        }
                        Dialog d = new Builder(Settings.this)
                                .setTitle(string.prompt_android_version)
                                .setSingleChoiceItems(choices, Tools.findIndex(choices, Main.preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "null")), new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Main.preferences.edit().putString(Keys.KEY_SETTINGS_ROMAPI, choices[i]).apply();
                                    }
                                })
                                .setCancelable(false)
                                .setPositiveButton(string.btn_ok, null)
                                .show();
                        Tools.userDialog = d;
                    }
                }.execute();
            }
        });

        ((CompoundButton) this.findViewById(id.switch_bkg_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, b).apply();
            }
        });

        (this.AC_H = (TextView) this.findViewById(id.editText_autocheck_h)).addTextChangedListener(this.intervalChanger);
        (this.AC_M = (TextView) this.findViewById(id.editText_autocheck_m)).addTextChangedListener(this.intervalChanger);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                View child = ((LayoutInflater) Settings.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout.blank_view, null);
                final NumberPicker picker = new NumberPicker(Settings.this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                picker.setMaxValue(view == Settings.this.AC_H ? 168 : 59);
                picker.setMinValue(0);
                picker.setValue(Integer.parseInt(Main.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[view == Settings.this.AC_H ? 0 : 1]));
                ((ViewManager) child).addView(picker, params);
                ((LinearLayout) child).setGravity(Gravity.CENTER_HORIZONTAL);
                child.setPadding(30, 0, 30, 0);
                Builder builder = new Builder(Settings.this);
                builder.setTitle(string.settings_textView_backgroundCheckInterval);
                builder.setView(child);
                builder.setPositiveButton(string.btn_ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Settings.this.updateTextView((TextView) view, String.valueOf(picker.getValue()));
                    }
                });
                builder.setNegativeButton(string.btn_cancel, null);
                builder.show();
            }
        };

        this.AC_H.setOnClickListener(listener);
        this.AC_M.setOnClickListener(listener);

        this.AC_H.post(new Runnable() {
            @Override
            public void run() {
                Settings.this.AC_H.setLayoutParams(new LinearLayout.LayoutParams(Settings.this.AC_H.getMeasuredWidth(), Settings.this.AC_H.getMeasuredWidth()));
                Settings.this.AC_M.setLayoutParams(new LinearLayout.LayoutParams(Settings.this.AC_M.getMeasuredWidth(), Settings.this.AC_M.getMeasuredWidth()));
            }
        });

        ((CompoundButton) this.findViewById(id.switch_bkg_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, b).apply();
                LinearLayout linearLayout = (LinearLayout) Settings.this.findViewById(id.linear_bkg_check_edit);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    linearLayout.getChildAt(i).setEnabled(b);
                }
                if (!b) {
                    Settings.this.stopService(new Intent(Settings.this, BackgroundAutoCheckService.class));
                } else {
                    Intent i = new Intent(Settings.this, BackgroundAutoCheckService.class);
                    Settings.this.stopService(i);
                    Settings.this.startService(i);
                }
            }
        });

        this.updateScreen();

        Main.preferences.registerOnSharedPreferenceChangeListener(this.prefListener);

    }

    void updateTextView(TextView v, CharSequence s) {
        v.setText(s);
    }

    private void updateScreen() {
        this.screenUpdating = true;
        ((Checkable) this.findViewById(id.switch_bkg_check)).setChecked(Main.preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true));
        ((TextView) this.findViewById(id.textView_dlLoc)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, ""));
        ((TextView) this.findViewById(id.proxyHost)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_PROXYHOST, Keys.DEFAULT_PROXY));
        ((Checkable) this.findViewById(id.checkbox_useProxy)).setChecked(Main.preferences.getBoolean(Keys.KEY_SETTINGS_USEPROXY, false));
        ((Checkable) this.findViewById(id.checkbox_useAndDM)).setChecked(Main.preferences.getBoolean(Keys.KEY_SETTINGS_USEANDM, false));
        ((Checkable) this.findViewById(id.checkbox_receiveBeta)).setChecked(Main.preferences.getBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, false));

        this.AC_H.setText(Main.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[0]);
        this.AC_M.setText(Main.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[1]);

        if (Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE).equalsIgnoreCase(Keys.DEFAULT_SOURCE))
            ((TextView) this.findViewById(id.textView_upSrc)).setText(this.getString(string.defaultt));
        else
            ((TextView) this.findViewById(id.textView_upSrc)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE));

        ((Checkable) this.findViewById(id.checkbox_useStaticFilename)).setChecked(Main.preferences.getBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false));

        ((TextView) this.findViewById(id.textView_staticFilename)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, this.getString(string.undefined)));

        ((TextView) this.findViewById(id.textView_romBase)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "n/a").toUpperCase());

        ((TextView) this.findViewById(id.textView_romApi)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "n/a").toUpperCase());
        this.screenUpdating = false;
    }

    private Object[] showDialog(CharSequence msg, CharSequence hint, CharSequence editTextContent) {
        View child = ((LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout.blank_view, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        child.setLayoutParams(params);
        child.setPadding(30, 30, 30, 30);
        EditText editText = new EditText(this);
        editText.setSingleLine();
        editText.setHorizontallyScrolling(true);
        editText.setHint(hint);
        if (editTextContent != null) {
            editText.setText(editTextContent);
        }
        TextView textView = new TextView(this);
        textView.setText(msg);
        ((ViewManager) child).addView(textView, params);
        ((ViewManager) child).addView(editText, params);
        Dialog dialog = new Dialog(this.activity, style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(child);
        dialog.show();
        return new Object[]{dialog, editText};
    }

    private boolean getDevicePart() throws DeviceNotSupportedException {
        Scanner s;
        this.DEVICE_PART = "";
        boolean DEVICE_SUPPORTED = false;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE)).openConnection();
            s = new Scanner(connection.getInputStream());
        } catch (final Exception e) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Settings.this.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }
        String pattern = String.format("<%s>", Build.DEVICE);
        while (s.hasNextLine()) {
            if (s.nextLine().equalsIgnoreCase(pattern)) {
                DEVICE_SUPPORTED = true;
                break;
            }
        }
        if (DEVICE_SUPPORTED) {
            String line;
            while (s.hasNextLine()) {
                line = s.nextLine().trim();
                if (line.equalsIgnoreCase(String.format("</%s>", Build.DEVICE)))
                    break;

                //noinspection SingleCharacterStringConcatenation
                this.DEVICE_PART += line + '\n';
            }
            return true;
        } else {
            throw new DeviceNotSupportedException();
        }
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return false;
    }*/

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.overridePendingTransition(anim.slide_in_ltr, anim.slide_out_ltr);
    }

    @Override
    public String toString() {
        return "Settings{" +
                "activity=" + this.activity +
                ", DEVICE_PART='" + this.DEVICE_PART + '\'' +
                ", screenUpdating=" + this.screenUpdating +
                ", AC_H=" + this.AC_H +
                ", AC_M=" + this.AC_M +
                ", intervalChanger=" + this.intervalChanger +
                ", prefListener=" + this.prefListener +
                '}';
    }
}
