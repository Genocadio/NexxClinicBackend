package com.nexxserve.nexxclinic.security;

import com.nexxserve.nexxclinic.model.PermissionName;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasPermission {
    PermissionName[] value();
}
