package com.nebula.kernelupdater;

/**
 * Created by Mike on 9/20/2014.
 */
public enum Keys {
    ;

    public static final String DEFAULT_SOURCE = "http://lightningbolt.duckdns.org/~eliminater74/NebulaKernel_Releases.txt";
    public static final String SOURCE_CODE = "https://github.com/Eliminater74/NebulaKernel";
    public static final String TAG_NOTIF = "NEBULA.KERNELUPDATER.UPDNOTIF";
    public static final String VALIDITY_KEY = "::COM.NEBULA.KERNELUPDATER::";
    public static final String DEFAULT_PROXY = "202.38.95.66:8080";
    public static final String PROXY_LIST = "http://proxylist.hidemyass.com/";

    public static final String KEY_SETTINGS_SOURCE = "_SOURCE_4";
    public static final String KEY_SETTINGS_DOWNLOADLOCATION = "_DLLOCATION";
    public static final String KEY_SETTINGS_USEANDM = "_USEANDDM";
    public static final String KEY_SETTINGS_AUTOCHECK_ENABLED = "_ENABLEBAC";
    public static final String KEY_SETTINGS_AUTOCHECK_INTERVAL = "_BACINTERVAL";
    public static final String KEY_SETTINGS_ROMBASE = "_ROMBASE";
    public static final String KEY_SETTINGS_ROMAPI = "_API";
    public static final String KEY_SETTINGS_USESTATICFILENAME = "_USESTATICFILENAME";
    public static final String KEY_SETTINGS_LASTSTATICFILENAME = "_STATICFILENAME";
    public static final String KEY_SETTINGS_LOOKFORBETA = "_LOOKFORBETA";
    public static final String KEY_SETTINGS_USEPROXY = "_USEPROXY";
    public static final String KEY_SETTINGS_PROXYHOST = "_PROXYHOST";

    public static final String KEY_DEFINE_AV = "android_versions";
    public static final String KEY_DEFINE_BB = "build_bases";

    public static final String KEY_KERNEL_BASE = "_base:=";
    public static final String KEY_KERNEL_API = "_api:=";
    public static final String KEY_KERNEL_VERSION = "_version:=";
    public static final String KEY_KERNEL_ZIPNAME = "_zipname:=";
    public static final String KEY_KERNEL_HTTPLINK = "_httplink:=";
    public static final String KEY_KERNEL_MD5 = "_md5:=";
    public static final String KEY_KERNEL_test = "_testbuild:=";

    Keys() {
    }
}
