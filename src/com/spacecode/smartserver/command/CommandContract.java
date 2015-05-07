package com.spacecode.smartserver.command;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
@interface CommandContract
{
    /** (Minimum) Number of parameters expected */
    int paramCount() default 0;
    
    /** If true, the exact number of {@link #paramCount()} will be expected (not more, not less) */
    boolean strictCount() default false; 
    
    /** If true, the device should be under control (= connected, "serial bridge" OFF) */
    boolean deviceRequired() default false;
    
    /** If true, the socket sending the request must be "authenticated" (administrator) */
    boolean adminRequired() default false;
    
    /** String to be sent back to the client when a clause of the contract is not respected */
    String responseWhenInvalid() default ClientCommand.FALSE;

    /** If true, no response is sent back to the Client when the contract is invalid */
    boolean noResponseWhenInvalid() default false;
    
    /** If true, the "responseWhenInvalid" is sent to ALL clients */
    boolean responseSentToAllWhenInvalid() default false;
}
