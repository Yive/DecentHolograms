package eu.decentsoftware.holograms.api.utils.reflect;

import eu.decentsoftware.holograms.api.utils.Log;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@UtilityClass
public class ReflectionUtil {
    /**
     * Find a field with in a class with a specific type.
     * <p>
     * If the field is not found, this method will return null.
     *
     * @param clazz     The class to get the field from.
     * @param type      The class type of the field.
     * @return The field, or null if the field was not found.
     */
    public static Field findField(Class<?> clazz, Class<?> type) {
        if (clazz == null) return null;

        Field[] methods = clazz.getDeclaredFields();
        for (Field method : methods) {
            if (!method.getType().equals(type)) continue;

            method.setAccessible(true);
            return method;
        }
        return null;
    }

    /**
     * Get a class from the {@code net.minecraft} package. The classPath should be the full path to the class,
     * including the package without the {@code net.minecraft} prefix.
     * <p>
     * This is a shortcut for {@code getClass("net.minecraft." + classPath)}.
     *
     * @param classPath The path of the class to get.
     * @return The class, or null if the class was not found.
     */
    @Nullable
    public static Class<?> getNMClass(final @NotNull String classPath) {
        try {
            return Class.forName("net.minecraft." + classPath);
        } catch (ClassNotFoundException e) {
            Log.error("Failed to get net.minecraft class: %s", e, classPath);
            return null;
        }
    }
}
