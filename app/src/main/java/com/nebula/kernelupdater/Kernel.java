package com.nebula.kernelupdater;

import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by Mike on 10/23/2014.
 */
public class Kernel implements Comparable<Kernel> {

    private final String PARAMS;
    private String BASE;
    private String API;
    private String VERSION;
    private String ZIPNAME;
    private String HTTPLINK;
    private String MD5;
    private boolean ISTESTBUILD;

    public Kernel(String parameters) {
        this.PARAMS = parameters;

        if (this.PARAMS == null)
            return;

        Scanner s = new Scanner(this.PARAMS);

        try {
            String line;
            while (s.hasNextLine()) {
                line = s.nextLine().trim();
                if (line.length() > 0 && line.startsWith("_")) {
                    if (line.contains(Keys.KEY_KERNEL_BASE))
                        this.BASE = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_API))
                        this.API = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_VERSION))
                        this.VERSION = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_ZIPNAME))
                        this.ZIPNAME = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_HTTPLINK))
                        this.HTTPLINK = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_MD5))
                        this.MD5 = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_test))
                        try {
                            this.ISTESTBUILD = Boolean.parseBoolean(line.split(":=")[1].trim());
                        } catch (Exception ignored) {

                        }
                }
            }
        } finally {
            s.close();
        }
    }

    public Collection<String> getBASE() {
        String[] all = this.BASE.split(",");
        Set<String> set = new HashSet(all.length);
        for (String s : all)
            set.add(s.trim().toUpperCase());
        return set;
    }

    public Collection<String> getAPI() {
        String[] all = this.API.split(",");
        Set<String> set = new HashSet(all.length);
        for (String s : all)
            set.add(s.trim().toUpperCase());
        return set;
    }

    public String getVERSION() {
        return this.VERSION;
    }

    public String getZIPNAME() {
        return this.ZIPNAME;
    }

    public String getHTTPLINK() {
        return this.HTTPLINK;
    }

    public boolean isTestBuild() {
        return this.ISTESTBUILD;
    }

    public String getMD5() {
        return this.MD5;
    }

    @Override
    public int compareTo(Kernel another) {
        return this.MD5.compareTo(another.MD5);
    }

    @Override
    public String toString() {
        return "Kernel{" +
                "PARAMS='" + this.PARAMS + '\'' +
                ", BASE='" + this.BASE + '\'' +
                ", API='" + this.API + '\'' +
                ", VERSION='" + this.VERSION + '\'' +
                ", ZIPNAME='" + this.ZIPNAME + '\'' +
                ", HTTPLINK='" + this.HTTPLINK + '\'' +
                ", MD5='" + this.MD5 + '\'' +
                ", ISTESTBUILD=" + this.ISTESTBUILD +
                '}';
    }
}
