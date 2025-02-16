package eu.decentsoftware.holograms.api.utils.items;

import com.cryptomorin.xseries.XMaterial;
import eu.decentsoftware.holograms.api.utils.Common;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public final class DecentMaterial {

    private static final Map<String, String> MATERIAL_ALIASES = new HashMap<>();

    static {
        for (Material material : Material.values()) {
            MATERIAL_ALIASES.put(Common.removeSpacingChars(material.name()).toLowerCase(), material.name());
        }
    }

    public static Material parseMaterial(String materialName) {
        // Backwards compatibility
        Material materialFromAliases = Material.getMaterial(MATERIAL_ALIASES.get(Common.removeSpacingChars(materialName).toLowerCase()));
        if (materialFromAliases != null) {
            return materialFromAliases;
        }
        Optional<XMaterial> xMaterialOptional = XMaterial.matchXMaterial(materialName);
        return xMaterialOptional.map(XMaterial::get).orElse(null);
    }

}
