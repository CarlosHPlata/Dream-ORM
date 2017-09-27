package com.legion.silver.code.dreamorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Legion on 8/15/2017.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    public enum Type {
        TEXT, INTEGER, REAL, NULL, BLOB
    }

    public String value() default "";
}
