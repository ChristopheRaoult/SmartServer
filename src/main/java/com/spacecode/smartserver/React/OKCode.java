package com.spacecode.smartserver.React;

import java.util.Hashtable;

public class OKCode {
    public static final Hashtable<Integer, String> OkCodes;
    static {
        OkCodes = new Hashtable<Integer, String>();
        OkCodes.put(1, "Scan started");
        OkCodes.put(2, "Scan stopped");
    }
}
