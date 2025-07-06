package com.mszlu.rpc.annontation;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface MsMapping {
    // api调用路径
    String api() default "";
    // 调用url路径
    String url() default "";
}
