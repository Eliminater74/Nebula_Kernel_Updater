package com.nebula.kernelupdater;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Iterator;

/**
 * Created by Mike on 10/23/2014.
 */
public final class KernelManager {

    public static boolean baseMatchedOnce, apiMatchedOnce;

    private static KernelManager instance;

    private static UniqueSet<Kernel> kernelSet;

    private final SharedPreferences preferences;

    private KernelManager(Context c) {
        KernelManager.kernelSet = new UniqueSet<>();
        KernelManager.baseMatchedOnce = false;
        this.preferences = c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS);
        KernelManager.instance = this;
    }

    public static KernelManager getInstance(Context c) {
        return KernelManager.instance == null ? new KernelManager(c) : KernelManager.instance;
    }

    public boolean add(Kernel k) {
        return KernelManager.kernelSet.add(k);
    }

    public void sniffKernels(String data) {
        String[] parameters = data.split("\\+kernel");
        KernelManager.kernelSet.clear();
        for (String params : parameters) {
            if (params.equals(parameters[0]))
                continue;
            this.add(new Kernel(params));
        }
    }

    public Kernel getProperKernel() {
        KernelManager.apiMatchedOnce = false;
        KernelManager.baseMatchedOnce = false;

        if (KernelManager.kernelSet.isEmpty()) {
            return null;
        }

        Iterator<Kernel> iterator = KernelManager.kernelSet.iterator();

        while (iterator.hasNext()) {
            Kernel k = iterator.next();
            try {
                boolean a = k.getBASE().contains(this.preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "").toUpperCase());
                boolean b = k.getAPI().contains(this.preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").toUpperCase());
                if (a)
                    KernelManager.baseMatchedOnce = true;
                if (b)
                    KernelManager.apiMatchedOnce = true;
                if (a & b) {
                    if (!k.isTestBuild() || this.preferences.getBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, false)) {
                        return k;
                    }
                }
            } catch (NullPointerException ignored) {
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "KernelManager{" +
                "preferences=" + this.preferences +
                '}';
    }
}
