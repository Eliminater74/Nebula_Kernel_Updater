package com.nebula.kernelupdater;

import android.R.id;
import android.R.style;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import com.nebula.kernelupdater.R.drawable;
import com.nebula.kernelupdater.R.string;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.chainfire.libsuperuser.Shell;
import eu.chainfire.libsuperuser.Shell.Interactive;
import eu.chainfire.libsuperuser.Shell.OnCommandResultListener;

/**
 * Created by Mike on 9/19/2014.
 */
public class Tools {

    public static String EVENT_DOWNLOAD_COMPLETE = "THEMIKE10452.TOOLS.DOWNLOAD.COMPLETE";
    public static String EVENT_DOWNLOADEDFILE_EXISTS = "THEMIKE10452.TOOLS.DOWNLOAD.FILE.EXISTS";
    public static String EVENT_DOWNLOAD_CANCELED = "THEMIKE10452.TOOLS.DOWNLOAD.CANCELED";
    public static String ACTION_INSTALL = "THEMIKE10452.TOOLS.KERNEL.INSTALL";
    public static String ACTION_DISMISS = "THEMIKE10452.TOOLS.DISMISS";

    public static boolean isDownloading;
    public static Activity activity;

    public static Dialog userDialog;
    public static String INSTALLED_KERNEL_VERSION = "";
    private static Tools instance;
    private static Interactive interactiveShell;
    public boolean cancelDownload;
    public int downloadSize, downloadedSize;
    public File lastDownloadedFile;
    private final Context C;

    private Tools(Context context) {
        this.C = context;
        Tools.instance = this;
        if (Tools.interactiveShell == null)
            Tools.interactiveShell = new Shell.Builder().useSU().setWatchdogTimeout(5).setMinimalLogging(true).open(new OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    if (exitCode != OnResult.SHELL_RUNNING)
                        Tools.this.showRootFailDialog();
                }
            });
    }

    public static Tools getInstance(Context c) {
        return Tools.instance == null ? new Tools(c) : Tools.instance;
    }

    public static String getFileExtension(File f) {
        try {
            return f.getName().substring(f.getName().lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            return "";
        }
    }

    public static boolean isAllDigits(String s) {
        for (char c : s.toCharArray())
            if (!Character.isDigit(c))
                return false;
        return true;
    }

    public static String getMD5Hash(String filePath) {
        String md5hash = "";
        FileInputStream inputStream = null;

        try {

            inputStream = new FileInputStream(filePath);
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[1024];

            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }

            byte[] digestData = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : digestData) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }

            md5hash = sb.toString();

        } catch (Exception ignored) {
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
        }

        return md5hash;
    }

    public static String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            procVersionStr = new BufferedReader(new FileReader(new File("/proc/version"))).readLine();

            String PROC_VERSION_REGEX =
                    "Linux version (\\S+) " +
                            "\\((\\S+?)\\) " +
                            "(?:\\(gcc.+? \\)) " +
                            "(#\\d+) " +
                            "(?:.*?)?" +
                            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)";

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                return "Unavailable";
            } else {
                return new StringBuilder(Tools.INSTALLED_KERNEL_VERSION = m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4)).toString();
            }
        } catch (IOException e) {
            return "Unavailable";
        }
    }

    public static int findIndex(String[] strings, String string) {
        if (strings == null)
            return -1;

        for (int i = 0; i < strings.length; i++) {
            if (strings[i].trim().equals(string.trim()))
                return i;
        }

        return -1;
    }

    public static String retainDigits(String data) {
        String newString = "";
        for (char c : data.toCharArray()) {
            if (Character.isDigit(c) || c == '.')
                newString += c;
        }
        return newString;
    }

    public static boolean validateIP(String ip) {
        int dc = 0;
        boolean valid = !ip.contains("..") && !ip.startsWith(".") && !ip.endsWith(".");

        if (valid)
            for (char c : ip.toCharArray()) {
                if (c == '.')
                    dc++;
                else if (!Character.isDigit(c)) {
                    valid = false;
                    break;
                }
            }

        if (valid && dc == 3) {
            String[] parts = ip.split("\\.");
            for (String s : parts) {
                if (Integer.parseInt(s) > 255)
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public static Double getMinVer(String data) {
        Scanner s = new Scanner(data);
        try {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("#define min_ver*")) {
                    return Double.parseDouble(line.split("=")[1].trim());
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            s.close();
        }

    }

    public void showRootFailDialog() {
        Tools.userDialog = new AlertDialog.Builder(this.C)
                .setTitle(string.dialog_title_rootFail)
                .setMessage(string.prompt_rootFail)
                .setCancelable(false)
                .setPositiveButton(string.btn_ok, null)
                .show();
        ((TextView) Tools.userDialog.findViewById(id.message)).setTextAppearance(this.C, style.TextAppearance_Small);
        ((TextView) Tools.userDialog.findViewById(id.message)).setTypeface(Typeface.createFromAsset(this.C.getAssets(), "Roboto-Regular.ttf"));
    }

    public void downloadFile(final String httpURL, final String destination, final String alternativeFilename, final String MD5hash, boolean useAndroidDownloadManager) {

        NotificationManager manager = (NotificationManager) this.C.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(Keys.TAG_NOTIF, 3721);

        Tools.activity = (Activity) this.C;
        this.cancelDownload = false;
        this.downloadSize = 0;
        this.downloadedSize = 0;

        if (!useAndroidDownloadManager) {

            final CustomProgressDialog dialog = new CustomProgressDialog(Tools.activity);
            dialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Tools.this.cancelDownload = true;
                }
            });
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setProgress(0);
            dialog.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream stream = null;
                    FileOutputStream outputStream = null;
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) new URL(httpURL).openConnection();
                        String filename;
                        try {
                            filename = connection.getHeaderField("Content-Disposition");
                            if (filename != null && filename.contains("=")) {
                                if (filename.split("=")[1].contains(";"))
                                    filename = filename.split("=")[1].split(";")[0].replaceAll("\"", "");
                                else
                                    filename = filename.split("=")[1];
                            } else {
                                filename = alternativeFilename;
                            }
                        } catch (Exception e) {
                            filename = alternativeFilename;
                        }

                        filename = Main.preferences.getBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false) ? Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, filename) : filename;

                        Tools.this.lastDownloadedFile = new File(destination + filename);
                        byte[] buffer = new byte[1024];
                        int bufferLength;
                        Tools.this.downloadSize = connection.getContentLength();

                        if (MD5hash != null) {
                            if (Tools.this.lastDownloadedFile.exists() && Tools.this.lastDownloadedFile.isFile()) {
                                if (Tools.getMD5Hash(Tools.this.lastDownloadedFile.getAbsolutePath()).equalsIgnoreCase(MD5hash) && !Tools.this.cancelDownload) {
                                    Tools.activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.setIndeterminate(false);
                                            String total_mb = String.format("%.2g%n", Tools.this.downloadSize / Math.pow(2, 20)).trim();
                                            dialog.update(Tools.this.lastDownloadedFile.getName(), total_mb, total_mb);
                                            dialog.setProgress(100);
                                            Tools.this.C.sendBroadcast(new Intent(Tools.EVENT_DOWNLOADEDFILE_EXISTS));
                                        }
                                    });
                                    return;
                                }
                            }
                        }

                        new File(destination).mkdirs();
                        stream = connection.getInputStream();
                        outputStream = new FileOutputStream(Tools.this.lastDownloadedFile);
                        while ((bufferLength = stream.read(buffer)) > 0) {
                            if (Tools.this.cancelDownload)
                                return;
                            Tools.isDownloading = true;
                            outputStream.write(buffer, 0, bufferLength);
                            Tools.this.downloadedSize += bufferLength;
                            Tools.activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    double done = Tools.this.downloadedSize, total = Tools.this.downloadSize;
                                    Double progress = done / total * 100;
                                    dialog.setIndeterminate(false);
                                    String done_mb = String.format("%.2g%n", done / Math.pow(2, 20)).trim();
                                    String total_mb = String.format("%.2g%n", total / Math.pow(2, 20)).trim();
                                    dialog.update(Tools.this.lastDownloadedFile.getName(), done_mb, total_mb);
                                    dialog.setProgress(progress.intValue());
                                }
                            });
                        }

                        Intent out = new Intent(Tools.EVENT_DOWNLOAD_COMPLETE);
                        if (MD5hash != null) {
                            out.putExtra("match", MD5hash.equalsIgnoreCase(Tools.getMD5Hash(Tools.this.lastDownloadedFile.getAbsolutePath())));
                            out.putExtra("md5", Tools.getMD5Hash(Tools.this.lastDownloadedFile.getAbsolutePath()));
                        }
                        Tools.this.C.sendBroadcast(out);

                    } catch (final MalformedURLException e) {
                        Tools.activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Tools.this.C.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (final IOException ee) {
                        Tools.activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Tools.this.C.getApplicationContext(), ee.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    } finally {
                        if (Tools.this.cancelDownload)
                            Tools.this.C.sendBroadcast(new Intent(Tools.EVENT_DOWNLOAD_CANCELED));
                        dialog.dismiss();
                        Tools.isDownloading = false;
                        if (stream != null)
                            try {
                                stream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        if (outputStream != null)
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        if (connection != null)
                            connection.disconnect();
                    }
                }
            }).start();

        } else {

            new AsyncTask<Void, Void, String>() {
                ProgressDialog dialog;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    this.dialog = new ProgressDialog(Tools.activity);
                    this.dialog.setIndeterminate(true);
                    this.dialog.setCancelable(false);
                    this.dialog.setMessage(Tools.this.C.getString(string.msg_pleaseWait));
                    this.dialog.show();
                    Tools.userDialog = this.dialog;
                }

                @Override
                protected String doInBackground(Void... voids) {
                    String filename;
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(httpURL).openConnection();
                        try {
                            filename = connection.getHeaderField("Content-Disposition");
                            if (filename != null && filename.contains("=")) {
                                if (filename.split("=")[1].contains(";"))
                                    return filename.split("=")[1].split(";")[0].replaceAll("\"", "");
                                else
                                    return filename.split("=")[1];
                            } else {
                                return alternativeFilename;
                            }
                        } catch (Exception e) {
                            return alternativeFilename;
                        }
                    } catch (Exception e) {
                        return alternativeFilename;
                    }
                }

                @Override
                protected void onPostExecute(String filename) {
                    super.onPostExecute(filename);

                    final DownloadManager manager = (DownloadManager) Tools.this.C.getSystemService(Context.DOWNLOAD_SERVICE);

                    filename = Main.preferences.getBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false) ? Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, filename) : filename;

                    Uri destinationUri = Uri.fromFile(Tools.this.lastDownloadedFile = new File(destination + filename));

                    if (MD5hash != null) {
                        if (Tools.this.lastDownloadedFile.exists() && Tools.this.lastDownloadedFile.isFile()) {
                            if (Tools.getMD5Hash(Tools.this.lastDownloadedFile.getAbsolutePath()).equalsIgnoreCase(MD5hash) && !Tools.this.cancelDownload) {
                                Tools.activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Tools.this.C.startActivity(new Intent(Tools.this.C.getApplicationContext(), Main.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                                        Tools.this.C.sendBroadcast(new Intent(Tools.EVENT_DOWNLOADEDFILE_EXISTS));

                                        final NotificationManager manager1 = (NotificationManager) Tools.this.C.getSystemService(Context.NOTIFICATION_SERVICE);

                                        String bigText = Tools.this.C.getString(string.prompt_install2, Tools.this.C.getString(string.btn_install), "");
                                        bigText = bigText.split("\n")[0] + "\n" + bigText.split("\n")[1];

                                        Notification notification = new Builder(Tools.this.C)
                                                .setContentTitle(Tools.this.C.getString(string.dialog_title_readyToInstall))
                                                .setContentText(bigText)
                                                .setStyle(new BigTextStyle().bigText(bigText))
                                                .setSmallIcon(drawable.ic_notification)
                                                .addAction(drawable.ic_action_flash_on, Tools.this.C.getString(string.btn_install), PendingIntent.getBroadcast(Tools.activity, 0, new Intent(Tools.ACTION_INSTALL), 0))
                                                .build();

                                        manager1.notify(Keys.TAG_NOTIF, 3723, notification);

                                        Tools.this.C.registerReceiver(new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context context, Intent intent) {
                                                manager1.cancel(Keys.TAG_NOTIF, 3723);
                                                Tools.this.C.unregisterReceiver(this);
                                                Tools.this.createOpenRecoveryScript("install " + Tools.this.lastDownloadedFile.getAbsolutePath(), true, false);
                                            }
                                        }, new IntentFilter(Tools.ACTION_INSTALL));

                                    }
                                });

                                return;

                            } else {
                                Tools.this.lastDownloadedFile.delete();
                            }
                        }
                    }

                    new File(destination).mkdirs();

                    final long downloadID = manager
                            .enqueue(new Request(Uri.parse(httpURL))
                                    .setDestinationUri(destinationUri));

                    Tools.isDownloading = true;

                    this.dialog.setMessage(Tools.this.C.getString(string.dialog_title_downloading));
                    Tools.userDialog = this.dialog;

                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {

                            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {

                                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L) != downloadID)
                                    return;

                                Tools.this.C.unregisterReceiver(this);
                                Tools.isDownloading = false;

                                if (Tools.userDialog != null)
                                    Tools.userDialog.dismiss();

                                Query query = new Query();
                                query.setFilterById(downloadID);
                                Cursor cursor = manager.query(query);

                                if (!cursor.moveToFirst()) {
                                    Tools.this.C.sendBroadcast(new Intent(Tools.EVENT_DOWNLOAD_CANCELED));
                                    return;
                                }

                                int status = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);

                                if (cursor.getInt(status) == DownloadManager.STATUS_SUCCESSFUL) {

                                    Tools.this.lastDownloadedFile = new File(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)));
                                    Tools.this.C.startActivity(new Intent(Tools.this.C.getApplicationContext(), Main.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                                    Intent out = new Intent(Tools.EVENT_DOWNLOAD_COMPLETE);
                                    boolean match = true;
                                    String md5 = MD5hash;

                                    if (MD5hash != null) {
                                        out.putExtra("match", match = MD5hash.equalsIgnoreCase(Tools.getMD5Hash(Tools.this.lastDownloadedFile.getAbsolutePath())));
                                        out.putExtra("md5", md5 = Tools.getMD5Hash(Tools.this.lastDownloadedFile.getAbsolutePath()));
                                    }
                                    Tools.this.C.sendBroadcast(out);

                                    Intent intent1 = new Intent(Tools.ACTION_INSTALL);
                                    Intent intent2 = new Intent(Tools.ACTION_DISMISS);
                                    if (match) {

                                        String bigText = Tools.this.C.getString(string.prompt_install1, Tools.this.C.getString(string.btn_install), "");
                                        bigText = bigText.split("\n")[0] + "\n" + bigText.split("\n")[1];

                                        Notification notification = new Builder(Tools.this.C.getApplicationContext())
                                                .setSmallIcon(drawable.ic_notification)
                                                .setContentTitle(Tools.this.C.getString(string.msg_downloadComplete))
                                                .setContentText(bigText)
                                                .addAction(drawable.ic_action_flash_on, Tools.this.C.getString(string.btn_install), PendingIntent.getBroadcast(Tools.activity, 0, intent1, 0))
                                                .setStyle(new BigTextStyle().bigText(bigText))
                                                .build();
                                        final NotificationManager manager1 = (NotificationManager) Tools.this.C.getSystemService(Context.NOTIFICATION_SERVICE);

                                        Tools.this.C.registerReceiver(new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context context, Intent intent) {
                                                Tools.this.C.unregisterReceiver(this);
                                                if (Tools.userDialog != null)
                                                    Tools.userDialog.dismiss();
                                                manager1.cancel(Keys.TAG_NOTIF, 3722);

                                                Tools.this.createOpenRecoveryScript("install " + Tools.this.lastDownloadedFile.getAbsolutePath(), true, false);
                                            }
                                        }, new IntentFilter(Tools.ACTION_INSTALL));

                                        manager1.notify(Keys.TAG_NOTIF, 3722, notification);

                                    } else {

                                        Notification notification = new Builder(Tools.this.C.getApplicationContext())
                                                .setSmallIcon(drawable.ic_notification)
                                                .setContentTitle(Tools.this.C.getString(string.dialog_title_md5mismatch))
                                                .setContentText(Tools.this.C.getString(string.prompt_md5mismatch, MD5hash, md5))
                                                .addAction(drawable.ic_action_flash_on, Tools.this.C.getString(string.btn_install), PendingIntent.getBroadcast(Tools.activity, 0, intent1, 0))
                                                .addAction(drawable.ic_action_download, Tools.this.C.getString(string.btn_downloadAgain), PendingIntent.getBroadcast(Tools.activity, 0, intent2, 0))
                                                .setStyle(new BigTextStyle().bigText(Tools.this.C.getString(string.prompt_md5mismatch, MD5hash, md5)))
                                                .build();
                                        final NotificationManager manager1 = (NotificationManager) Tools.this.C.getSystemService(Context.NOTIFICATION_SERVICE);

                                        BroadcastReceiver receiver1 = new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context context, Intent intent) {
                                                Tools.this.C.unregisterReceiver(this);

                                                if (Tools.userDialog != null)
                                                    Tools.userDialog.dismiss();

                                                manager1.cancel(Keys.TAG_NOTIF, 3722);

                                                if (intent.getAction().equals(Tools.ACTION_INSTALL)) {
                                                    Tools.this.createOpenRecoveryScript("install " + Tools.this.lastDownloadedFile.getAbsolutePath(), true, false);
                                                } else {
                                                    context.startActivity(new Intent(context, Main.class));
                                                }
                                            }
                                        };

                                        Tools.this.C.registerReceiver(receiver1, new IntentFilter(Tools.ACTION_INSTALL));
                                        Tools.this.C.registerReceiver(receiver1, new IntentFilter(Tools.ACTION_DISMISS));

                                        manager1.notify(Keys.TAG_NOTIF, 3722, notification);
                                    }

                                } else {
                                    Toast.makeText(Tools.this.C, "error" + ": " + cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    };

                    Tools.this.C.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                }
            }.execute();

        }
    }

    public void createOpenRecoveryScript(String line, final boolean rebootAfter, boolean append) {
        if (Tools.interactiveShell != null && Tools.interactiveShell.isRunning()) {
            Tools.interactiveShell.addCommand("echo " + line + (append ? " >> " : ">") + "/cache/recovery/openrecoveryscript", 23, new OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    if (exitCode != 0)
                        Tools.this.showRootFailDialog();
                    else if (rebootAfter) {
                        if (Tools.activity != null)
                            Tools.activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(Tools.this.C, string.onReboot, Toast.LENGTH_LONG).show();
                                }
                            });
                        Tools.interactiveShell.addCommand("reboot recovery");
                    }
                }
            });

        } else {
            this.showRootFailDialog();
        }
    }

}
