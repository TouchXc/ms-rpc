package com.mszlu.rpc.annontation;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MsMapping {

    String api() default "";

    String url() default "";
}
