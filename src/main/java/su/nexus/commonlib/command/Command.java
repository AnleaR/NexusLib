/*
 * Decompiled with CFR 0.150.
 */
package su.nexus.commonlib.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Command {
	String name();

	String permission() default "";

	String noPerm() default "";

	String[] aliases() default {};

	@Deprecated String description() default "";

	@Deprecated String usage() default "";

	boolean inGameOnly() default false;

	boolean isOpOnly() default false;
}

