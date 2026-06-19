package net.oryn.mc.orynPlugins.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for module metadata. Alternative to implementing getter methods.
 * 
 * Usage:
 * {@code
 * @ModuleInfo(
 *     name = "mymodule",
 *     version = "1.0.0",
 *     description = "My awesome module",
 *     author = "YourName",
 *     dependencies = {"vault"},
 *     softDependencies = {"essentials"}
 * )
 * public class MyModule implements OrynModule { ... }
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleInfo {

    /**
     * Module name (unique, lowercase)
     */
    String name();

    /**
     * Module version
     */
    String version();

    /**
     * Module description
     */
    String description() default "";

    /**
     * Module author
     */
    String author() default "Unknown";

    /**
     * Hard dependencies (other module names that must be loaded first)
     */
    String[] dependencies() default {};

    /**
    * Soft dependencies (optional modules that enhance functionality)
    */
    String[] softDependencies() default {};
}
