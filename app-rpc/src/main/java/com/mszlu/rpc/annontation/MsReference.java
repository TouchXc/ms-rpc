package com.mszlu.rpc.annontation;


import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MsReference {

//    String host();
//
//    int port();

    String version() default "1.0";
}
