package eu.decentsoftware.holograms.api.utils.color;

import eu.decentsoftware.holograms.api.Settings;
import eu.decentsoftware.holograms.api.utils.color.caching.LruCache;
import eu.decentsoftware.holograms.api.utils.color.patterns.GradientPattern;
import eu.decentsoftware.holograms.api.utils.color.patterns.Pattern;
import eu.decentsoftware.holograms.api.utils.color.patterns.RainbowPattern;
import eu.decentsoftware.holograms.api.utils.color.patterns.SolidPattern;
import net.md_5.bungee.api.ChatColor;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class IridiumColorAPI {
    public static final List<String> SPECIAL_COLORS = Arrays.asList("&l", "&n", "&o", "&k", "&m");

    private static final LruCache LRU_CACHE = new LruCache(Settings.DEFAULT_LRU_CACHE_SIZE);

    /**
     * Cached result of patterns.
     *
     * @since 1.0.2
     */
    private static final List<Pattern> PATTERNS = Arrays.asList(new GradientPattern(), new SolidPattern(), new RainbowPattern());

    /**
     * Processes a string to add color to it.
     * Thanks to Distressing for helping with the regex &lt;3
     *
     * @param string The string we want to process
     * @since 1.0.0
     */
    @Nonnull
    public static String process(@Nonnull String string) {
        String result = LRU_CACHE.getResult(string);
        if (result != null) {
            return result;
        }
        String input = string;
        for (Pattern pattern : PATTERNS) {
            string = pattern.process(string);
        }
        string = ChatColor.translateAlternateColorCodes('&', string);
        LRU_CACHE.put(input, string);
        return string;
    }

    /**
     * Processes multiple strings in a list.
     *
     * @param strings The list of the strings we are processing
     * @return The list of processed strings
     * @since 1.0.3
     */
    @Nonnull
    public static List<String> process(@Nonnull List<String> strings) {
        strings.replaceAll(IridiumColorAPI::process);
        return strings;
    }

    /**
     * Colors a String.
     *
     * @param string The string we want to color
     * @param color  The color we want to set it to
     * @since 1.0.0
     */
    @Nonnull
    public static String color(@Nonnull String string, @Nonnull Color color) {
        return ChatColor.of(color) + string;
    }

    /**
     * Colors a String with a gradiant.
     *
     * @param string The string we want to color
     * @param start  The starting gradiant
     * @param end    The ending gradiant
     * @since 1.0.0
     */
    @Nonnull
    public static String color(@Nonnull String string, @Nonnull Color start, @Nonnull Color end) {
        StringBuilder specialColors = new StringBuilder();
        for (String color : IridiumColorAPI.SPECIAL_COLORS) {
            if (string.contains(color)) {
                specialColors.append(color);
                string = string.replace(color, "");
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        ChatColor[] colors = createGradient(start, end, string.length());
        String[] characters = string.split("");
        for (int i = 0; i < string.length(); i++) {
            stringBuilder.append(colors[i]).append(specialColors).append(characters[i]);
        }
        return stringBuilder.toString();
    }

    /**
     * Colors a String with rainbow colors.
     *
     * @param string     The string which should have rainbow colors
     * @param saturation The saturation of the rainbow colors
     * @since 1.0.3
     */
    @Nonnull
    public static String rainbow(@Nonnull String string, float saturation) {
        StringBuilder specialColors = new StringBuilder();
        for (String color : IridiumColorAPI.SPECIAL_COLORS) {
            if (string.contains(color)) {
                specialColors.append(color);
                string = string.replace(color, "");
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        ChatColor[] colors = createRainbow(string.length(), saturation);
        String[] characters = string.split("");
        for (int i = 0; i < string.length(); i++) {
            stringBuilder.append(colors[i]).append(specialColors).append(characters[i]);
        }
        return stringBuilder.toString();
    }

    /**
     * Gets a color from hex code.
     *
     * @param string The hex code of the color
     * @since 1.0.0
     */
    @Nonnull
    public static ChatColor getColor(@Nonnull String string) {
        return ChatColor.of(new Color(Integer.parseInt(string, 16)));
    }

    /**
     * Removes all color codes from the provided String, including IridiumColorAPI patterns.
     *
     * @param string    The String which should be stripped
     * @return          The stripped string without color codes
     * @since 1.0.5
     */
    @Nonnull
    public static String stripColorFormatting(@Nonnull String string) {
        return string.replaceAll("[&§][a-f0-9lnokm]|<[/]?\\w{5,8}(:[0-9A-F]{6})?>", "");
    }

    /**
     * Returns a rainbow array of chat colors.
     *
     * @param step       How many colors we return
     * @param saturation The saturation of the rainbow
     * @return The array of colors
     * @since 1.0.3
     */
    @Nonnull
    private static ChatColor[] createRainbow(int step, float saturation) {
        ChatColor[] colors = new ChatColor[step];
        double colorStep = (1.00 / step);
        for (int i = 0; i < step; i++) {
            colors[i] = ChatColor.of(Color.getHSBColor((float) (colorStep * i), saturation, saturation));
        }
        return colors;
    }

    /**
     * Returns a gradient array of chat colors or just white if {@code step} is 1 or less.
     *
     * @param start The starting color.
     * @param end   The ending color.
     * @param step  How many colors we return.
     * @author TheViperShow
     * @since 1.0.0
     */
    @Nonnull
    private static ChatColor[] createGradient(@Nonnull Color start, @Nonnull Color end, int step) {
        // Return just white if step is 1 or less. Prevents possible "/ by zero" exception.
        if (step <= 1) {
            return new ChatColor[]{ChatColor.WHITE, ChatColor.WHITE, ChatColor.WHITE};
        }
        
        ChatColor[] colors = new ChatColor[step];
        int stepR = Math.abs(start.getRed() - end.getRed()) / (step - 1);
        int stepG = Math.abs(start.getGreen() - end.getGreen()) / (step - 1);
        int stepB = Math.abs(start.getBlue() - end.getBlue()) / (step - 1);
        int[] direction = new int[]{
                start.getRed() < end.getRed() ? +1 : -1,
                start.getGreen() < end.getGreen() ? +1 : -1,
                start.getBlue() < end.getBlue() ? +1 : -1
        };

        for (int i = 0; i < step; i++) {
            Color color = new Color(start.getRed() + ((stepR * i) * direction[0]), start.getGreen() + ((stepG * i) * direction[1]), start.getBlue() + ((stepB * i) * direction[2]));
            colors[i] = ChatColor.of(color);
        }
        return colors;
    }

}
