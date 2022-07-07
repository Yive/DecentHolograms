package eu.decentsoftware.holograms.plugin.convertors;

import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.actions.Action;
import eu.decentsoftware.holograms.api.actions.ActionType;
import eu.decentsoftware.holograms.api.actions.ClickType;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import eu.decentsoftware.holograms.api.utils.Common;
import org.bukkit.Location;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class ConverterCommon {
    
    public static void createHologram(ConvertorResult convertorResult, String name, Location location, List<String> lines, DecentHolograms plugin) {
        if (plugin.getHologramManager().containsHologram(name)) {
            Common.log(Level.WARNING, "A hologram with name '%s' already exists, skipping...", name);
            convertorResult.addSkipped();
            return ;
        }
        Hologram hologram = new Hologram(name, location);
        HologramPage page = hologram.getPage(0);
        plugin.getHologramManager().registerHologram(hologram);
        lines.forEach((line) -> page.addLine(new HologramLine(page, page.getNextLineLocation(), line)));
        hologram.save();
        convertorResult.addSuccess();
    }
    
    public static void createHologramPages(ConvertorResult convertorResult, String name, Location location, List<List<String>> pages, DecentHolograms plugin) {
        if (plugin.getHologramManager().containsHologram(name)) {
            Common.log(Level.WARNING, "A hologram with name '%s' already exists, skipping...", name);
            convertorResult.addSkipped();
            return;
        }
        
        Hologram hologram = new Hologram(name, location);
        for (int i = 0; i < pages.size(); i++) {
            if (i != 0) {
                hologram.addPage();
            }
            
            HologramPage page = hologram.getPage(i);
            List<String> lines = pages.get(i);
            lines.forEach((line) -> page.addLine(new HologramLine(page, page.getNextLineLocation(), line)));
            
            page.addAction(ClickType.LEFT, new Action(ActionType.PREV_PAGE, hologram.getName()));
            page.addAction(ClickType.RIGHT, new Action(ActionType.NEXT_PAGE, hologram.getName()));
        }
        
        plugin.getHologramManager().registerHologram(hologram);
        hologram.save();
        
        convertorResult.addSuccess();
    }
    
    public static boolean isValidFile(final File file, final String fileName) {
        return file != null && file.exists() && !file.isDirectory() && file.getName().equals(fileName);
    }

}
