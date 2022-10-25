package com.spacecode.smartserver.React;

import java.util.Dictionary;
import java.util.Hashtable;

public class ErrorCode {
    public static final Hashtable<String, String> ErrorCodes;
    static {
        ErrorCodes =  new Hashtable<String, String>();
        ErrorCodes.put("E0001", "Device Not Connected");
        ErrorCodes.put("E0002", "Device Not Ready");
        ErrorCodes.put("E0003", "Device Already in Scan" );
        ErrorCodes.put("E0004", "Error in Data to Write");
        ErrorCodes.put("E0005", "Error writing , timeout" );
        ErrorCodes.put("E0006", "Error scan, timeout");
        ErrorCodes.put("E0007", "Writing Format Error");
    };
}
