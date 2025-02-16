package eu.decentsoftware.holograms.api.commands;

import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandManager {

    private final Map<String, DecentCommand> commands = new HashMap<>();
    private DecentCommand mainCommand;

    /*
     *  General Methods
     */

    public void destroy() {
        if (!commands.isEmpty()) {
            commands.values().forEach(CommandManager::unregister);
            commands.clear();
        }
    }

    public void registerCommand(DecentCommand decentCommand) {
        if (commands.containsKey(decentCommand.getName())) return;
        commands.put(decentCommand.getName(), decentCommand);
        CommandManager.register(decentCommand);
    }

    public void unregisterCommand(String name) {
        if (!commands.containsKey(name)) return;
        DecentCommand decentCommand = commands.remove(name);
        CommandManager.unregister(decentCommand);
    }

    public void setMainCommand(DecentCommand decentCommand) {
        this.mainCommand = decentCommand;
    }

    public DecentCommand getMainCommand() {
        return mainCommand;
    }

    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    public Collection<DecentCommand> getCommands() {
        return commands.values();
    }

    /*
     *  Static Methods
     */

    public static void register(Command command) {
        if (command == null) return;
        CommandMap commandMap = DecentHologramsAPI.get().getPlugin().getServer().getCommandMap();
        CommandManager.unregister(command);
        commandMap.register("DecentHolograms", command);
    }

    public static void unregister(Command command) {
        if (command == null) return;
        Map<String, Command> cmdMap = DecentHologramsAPI.get().getPlugin().getServer().getCommandMap().getKnownCommands();
        if (!cmdMap.isEmpty()) {
            cmdMap.remove(command.getLabel());
            for (final String alias : command.getAliases()) {
                cmdMap.remove(alias);
            }
        }
    }

}
