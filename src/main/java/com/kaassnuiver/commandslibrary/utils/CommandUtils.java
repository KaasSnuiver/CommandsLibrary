package com.kaassnuiver.commandslibrary.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;

@UtilityClass
public class CommandUtils {

    public Optional<CommandMap> getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return Optional.of((CommandMap) field.get(Bukkit.getServer()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<PluginCommand> getCommand(String commandName, Plugin instance) {
        Constructor<PluginCommand> pluginCommandConstructor = null;
        try {
            pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            pluginCommandConstructor.setAccessible(true);

            return Optional.of(pluginCommandConstructor.newInstance(commandName, instance));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            if (pluginCommandConstructor != null) pluginCommandConstructor.setAccessible(false);
        }
    }

}
