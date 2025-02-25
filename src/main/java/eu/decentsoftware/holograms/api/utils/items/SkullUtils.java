package eu.decentsoftware.holograms.api.utils.items;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import eu.decentsoftware.holograms.api.utils.Log;
import eu.decentsoftware.holograms.api.utils.reflect.ReflectionUtil;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

/**
 * Utility class for modifying the textures or owners or skull ItemStacks.
 * All ItemStacks modified by this class have to be of type either SKULL_ITEM
 * or PLAYER_HEAD in versions 1.13 and up.
 *
 * @author d0by
 */
@UtilityClass
public final class SkullUtils {

	private static Field PROFILE_FIELD;
	private static Method SET_PROFILE_METHOD;
	private static boolean INITIALIZED = false;

	private static Constructor<?> RESOLVABLE_PROFILE_CONSTRUCTOR;
	private static Field GAME_PROFILE_FIELD_RESOLVABLE_PROFILE;

	private static Method PROPERTY_VALUE_METHOD;
	private static Function<Property, String> VALUE_RESOLVER;

	static {
		try {
			Class<?> resolvableProfileClass = ReflectionUtil.getNMClass("world.item.component.ResolvableProfile");
			RESOLVABLE_PROFILE_CONSTRUCTOR = resolvableProfileClass == null ? null : resolvableProfileClass.getConstructor(GameProfile.class);

			// find the game profile field in the resolvable profile class
			GAME_PROFILE_FIELD_RESOLVABLE_PROFILE = ReflectionUtil.findField(resolvableProfileClass, GameProfile.class);
		} catch ( NoSuchMethodException ignored) {
			// old version, no resolvable profile class.
		}
	}

	/**
	 * Get the Base64 texture of the given skull ItemStack.
	 *
	 * @param itemStack The ItemStack.
	 * @return The skull texture. (Base64)
	 */
	@Nullable
	public static String getSkullTexture(@NonNull ItemStack itemStack) {
		try {
			ItemMeta meta = itemStack.getItemMeta();
			if (!(meta instanceof SkullMeta)) {
				return null;
			}
			// private ResolvableProfile profile;

			if (PROFILE_FIELD == null) {
				PROFILE_FIELD = meta.getClass().getDeclaredField("profile");
				PROFILE_FIELD.setAccessible(true);
			}
			Object profileObject = PROFILE_FIELD.get(meta);
			if (profileObject == null) return null;

			GameProfile profile = (GameProfile) (GAME_PROFILE_FIELD_RESOLVABLE_PROFILE == null ? profileObject : GAME_PROFILE_FIELD_RESOLVABLE_PROFILE.get(profileObject));
			if (profile == null) return null;

			if (VALUE_RESOLVER == null) {
				PROPERTY_VALUE_METHOD = Property.class.getMethod("value");
				VALUE_RESOLVER = property -> {
					try {
						return (String) PROPERTY_VALUE_METHOD.invoke(property);
					} catch (IllegalAccessException | InvocationTargetException e) {
						Log.error("Failed to invoke Property#value", e);
					}
					return null;
				};
			}

			PropertyMap properties = profile.getProperties();
			Collection<Property> property = properties.get("textures");
			if (property != null && !property.isEmpty()) {
				return VALUE_RESOLVER.apply(property.iterator().next());
			}
		} catch (Exception e) {
			Log.error("An exception occurred while retrieving skull texture", e);
		}
		return null;
	}

	/**
	 * Get the Base64 texture of the given skull ItemStack.
	 *
	 * @param itemStack The ItemStack.
	 * @return The skull texture. (Base64)
	 * @deprecated Use {@link #getSkullTexture(ItemStack)} instead.
	 */
	@Deprecated
	@Nullable
	public static String getTexture(@NonNull ItemStack itemStack) {
		return getSkullTexture(itemStack);
	}

	/**
	 * Set the Base64 texture of the given skull ItemStack.
	 *
	 * @param itemStack The ItemStack.
	 * @param texture   The new skull texture (Base64).
	 */
	public static void setSkullTexture(@NonNull ItemStack itemStack, @NonNull String texture) {
		try {
			ItemMeta meta = itemStack.getItemMeta();
			if (meta instanceof SkullMeta) {
				GameProfile profile = new GameProfile(UUID.randomUUID(), "");
				Property property = new Property("textures", texture);

				PropertyMap properties = profile.getProperties();
				properties.put("textures", property);
				if (SET_PROFILE_METHOD == null && !INITIALIZED) {
					try {
						// This method only exists in versions 1.16 and up. For older versions, we use reflection
						// to set the profile field directly.
						SET_PROFILE_METHOD = meta.getClass().getDeclaredMethod("setProfile", RESOLVABLE_PROFILE_CONSTRUCTOR == null ? GameProfile.class : RESOLVABLE_PROFILE_CONSTRUCTOR.getDeclaringClass());
						SET_PROFILE_METHOD.setAccessible(true);
					} catch (NoSuchMethodException e) {
						// Server is running an older version.
					}
					INITIALIZED = true;
				}

				if (SET_PROFILE_METHOD != null) {
					SET_PROFILE_METHOD.invoke(meta, RESOLVABLE_PROFILE_CONSTRUCTOR == null ? profile : RESOLVABLE_PROFILE_CONSTRUCTOR.newInstance(profile));
				} else {
					if (PROFILE_FIELD == null) {
						PROFILE_FIELD = meta.getClass().getDeclaredField("profile");
						PROFILE_FIELD.setAccessible(true);
					}
					PROFILE_FIELD.set(meta, profile);
				}
			}
			itemStack.setItemMeta(meta);
		} catch (Exception e) {
			Log.error("An exception occurred while setting skull texture", e);
		}
	}

	/**
	 * Get the owner of the given skull ItemStack.
	 *
	 * @param itemStack The ItemStack.
	 * @return The skull owner.
	 * @since 2.7.5
	 */
	@SuppressWarnings("deprecation")
	@Nullable
	public static String getSkullOwner(@NonNull ItemStack itemStack) {
		ItemMeta meta = itemStack.getItemMeta();
		if (meta instanceof SkullMeta) {
			return ((SkullMeta) meta).getOwner();
		}
		return null;
	}

	/**
	 * Set the owner of the given skull ItemStack.
	 *
	 * @param itemStack The ItemStack.
	 * @param owner     The new skull owner.
	 * @since 2.7.5
	 */
	@SuppressWarnings("deprecation")
	public static void setSkullOwner(@NonNull ItemStack itemStack, @NonNull String owner) {
		ItemMeta meta = itemStack.getItemMeta();
		if (meta instanceof SkullMeta) {
			((SkullMeta) meta).setOwner(owner);

			itemStack.setItemMeta(meta);
		}
	}

	/**
	 * Get a Base64 texture from URL by player username.
	 *
	 * @param username The player username.
	 * @return The Base64 or null if the URL is invalid.
	 * @since 2.7.10
	 */
	@Nullable
	public static String getTextureFromURLByPlayerName(String username) {
		final String uuid = getPlayerUUID(username);
		if (uuid == null) {
			return null;
		}
		final String url = "https://api.minetools.eu/profile/" + uuid;
		try {
			JSONParser jsonParser = new JSONParser();
			String userData = readUrl(url);
			Object parsedData = jsonParser.parse(userData);

			JSONObject jsonData = (JSONObject) parsedData;
			JSONObject decoded = (JSONObject) jsonData.get("raw");
			JSONArray textures = (JSONArray) decoded.get("properties");
			JSONObject data = (JSONObject) textures.get(0);

			return data.get("value").toString();
		} catch (Exception e) {
			Log.warn("Failed to fetch texture for player %s", username, e);
		}
		return null;
	}

	/**
	 * Fetch the UUID of a player from Minetools's API.
	 *
	 * @param playerName The player name.
	 * @return The UUID or null if the operation fails.
	 */
	@Nullable
	public static String getPlayerUUID(String playerName) {
		try {
			String url = "https://api.minetools.eu/uuid/" + playerName;
			JSONParser jsonParser = new JSONParser();
			String userData = readUrl(url);
			Object parsedData = jsonParser.parse(userData);

			JSONObject jsonData = (JSONObject) parsedData;

			if (jsonData.get("id") != null) {
				return jsonData.get("id").toString();
			}
		} catch (Exception e) {
			Log.warn("Failed to fetch UUID for player %s", playerName, e);
		}
		return null;
	}

	/**
	 * Read the content of a URL.
	 *
	 * @param urlString The URL.
	 * @return The content.
	 * @throws Exception If anything goes wrong.
	 */
	@NonNull
	private static String readUrl(String urlString) throws Exception {
		BufferedReader reader = null;
		URLConnection connection = null;
		try {
			connection = new URL(urlString).openConnection();
			connection.setConnectTimeout(50);
			connection.setReadTimeout(50);
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder builder = new StringBuilder();
			int read;
			char[] chars = new char[1024];
			while ((read = reader.read(chars)) != -1) {
				builder.append(chars, 0, read);
			}
			return builder.toString();
		} finally {
			if (connection instanceof HttpURLConnection) {
				((HttpURLConnection) connection).disconnect();
			}
			if (reader != null) {
				reader.close();
			}
		}
	}

}
