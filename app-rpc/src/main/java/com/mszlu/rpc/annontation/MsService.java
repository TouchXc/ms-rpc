package com.mszlu.rpc.annontation;


import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MsService {

    String version() default "1.0";
}
