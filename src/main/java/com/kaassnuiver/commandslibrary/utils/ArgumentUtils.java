package com.kaassnuiver.commandslibrary.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

@UtilityClass
public class ArgumentUtils {

    public Optional<Object[]> findArguments(CommandSender commandSender, ArgumentType[] argumentTypes, final String usage, String[] args) {
        if (argumentTypes.length > (args.length)) {
            commandSender.sendMessage(usage);
            return Optional.empty();
        }

        Object[] arguments = new Object[argumentTypes.length];
        arguments[0] = commandSender;
        for (int i = 1; i < argumentTypes.length; i++) {
            Object parsedArg = argumentTypes[i].parse(args[i]);
            if (parsedArg == null) {
                commandSender.sendMessage(usage);
                return Optional.empty();
            }
            arguments[i] = parsedArg;
        }
        return Optional.of(arguments);
    }

    public enum ArgumentType {

        ONLINE_PLAYER(Player.class),
        OFFLINE_PLAYER(OfflinePlayer.class),
        DOUBLE(Double.class),
        INT(Integer.class),
        FLOAT(Float.class),
        STRING_SINGLE(String.class),
        BOOLEAN(Boolean.class);

        private final Class<?> classType;

        ArgumentType(Class<?> clazz) {
            classType = clazz;
        }

        public Object parse(String string) {
            Objects.requireNonNull(string, "Argument cannot be null.");
            switch (this) {
                case ONLINE_PLAYER:
                    return Bukkit.getPlayer(string);
                case OFFLINE_PLAYER:
                    return Bukkit.getOfflinePlayer(string);
                case DOUBLE:
                    try {
                        return Double.parseDouble(string);
                    } catch (Exception e) {
                        return null;
                    }
                case INT:
                    try {
                        return Integer.parseInt(string);
                    } catch (Exception e) {
                        return null;
                    }
                case FLOAT:
                    try {
                        return Float.parseFloat(string);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                case STRING_SINGLE:
                    return string;
                case BOOLEAN:
                    try {
                        return Boolean.parseBoolean(string);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                default:
                    return null;
            }
        }

        public static ArgumentType getArgumentType(Class<?> classType) {
            for (ArgumentType argumentType : ArgumentType.values() ) {
                if (argumentType.classType != classType) continue;
                return argumentType;
            }
            return null;
        }
    }
}
