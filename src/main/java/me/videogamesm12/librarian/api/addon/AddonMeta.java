package me.videogamesm12.librarian.api.addon;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AddonMeta
{
	String[] requiredMods();
}
