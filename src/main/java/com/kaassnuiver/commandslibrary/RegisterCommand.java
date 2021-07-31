package com.kaassnuiver.commandslibrary;

import com.google.common.collect.ImmutableList;
import com.kaassnuiver.commandslibrary.annotations.*;
import com.kaassnuiver.commandslibrary.annotations.Command;
import com.kaassnuiver.commandslibrary.objects.BaseCommand;
import com.kaassnuiver.commandslibrary.objects.SubCommand;
import com.kaassnuiver.commandslibrary.utils.ArgumentUtils;
import com.kaassnuiver.commandslibrary.utils.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class RegisterCommand {

    private final JavaPlugin instance;

    RegisterCommand(JavaPlugin instance) {
        this.instance = instance;
    }

    public void register(Object object) {
        Objects.requireNonNull(instance, "Main class cannot be null.");
        Objects.requireNonNull(object, "Command cannot be null.");

        Class<?> commandClass = object.getClass();
        String name = null;
        String commandPermission = null;
        List<String> aliases = new ArrayList<>();
        String usage = "No usage specified.";
        boolean async = false;
        Method invokeMethod = null;

        for (Annotation annotation : commandClass.getAnnotations()) {
            if (annotation instanceof Command) name = (((Command) annotation).value().equals("")) ? commandClass.getName() : ((Command) annotation).value();
            if (annotation instanceof Permission) commandPermission = ((Permission) annotation).value();
            if (annotation instanceof Aliases) Collections.addAll(aliases, ((Aliases) annotation).value());
            if (annotation instanceof Usage) usage = ((Usage) annotation).value();
            if (annotation instanceof Async) async = true;
        }
        for (Method method : commandClass.getMethods()) {
            if (!method.getName().equalsIgnoreCase(name)) continue;
            invokeMethod = method;
        }


        BaseCommand commandObject = new BaseCommand(name, commandPermission, aliases, usage, object, async, invokeMethod);
        Set<SubCommand> subCommandObjects = new HashSet<>();

        for (Method method : commandClass.getMethods()) {
            Optional<Annotation> optionalAnnotation = Arrays.stream(method.getAnnotations()).filter(annotation -> annotation instanceof Sub).findFirst();
            if (!optionalAnnotation.isPresent()) continue;
            String subCommandName = ((Sub) optionalAnnotation.get()).value().equals("") ? method.getName() : ((Sub) optionalAnnotation.get()).value();
            String subCommandPermission = null;
            List<String> subCommandAliases = new ArrayList<>();
            boolean subCommandAsync = false;
            String subCommandUsage = "No usage specified.";

            for (Annotation annotation : method.getAnnotations()) {
                if (annotation instanceof Permission) subCommandPermission = ((Permission) annotation).value();
                if (annotation instanceof Aliases)
                    Collections.addAll(subCommandAliases, ((Aliases) annotation).value());
                if (annotation instanceof Async) subCommandAsync = true;
                if (annotation instanceof Usage) subCommandUsage = ((Usage) annotation).value();
            }

            ArgumentUtils.ArgumentType[] argumentTypes = new ArgumentUtils.ArgumentType[method.getParameterCount()];
            for (int i = 1; i < method.getParameterCount(); i++) {
                ArgumentUtils.ArgumentType type = ArgumentUtils.ArgumentType.getArgumentType(method.getParameterTypes()[i]);
                if (type == null)
                    throw new IllegalArgumentException("Argument type " + method.getParameterTypes()[i].getSimpleName() + " is not supported by this plugin.");
                argumentTypes[i] = type;
            }

            subCommandObjects.add(new SubCommand(subCommandName, subCommandPermission, subCommandAliases, subCommandAsync, method, subCommandUsage, argumentTypes));
        }

        Optional<CommandMap> optionalCommandMap = CommandUtils.getCommandMap();
        if (!optionalCommandMap.isPresent()) {
            instance.getLogger().log(Level.WARNING, ChatColor.RED + "Command " + name + " could not be registered because commandMap could not be found.");
            return;
        }
        CommandMap commandMap = optionalCommandMap.get();

        Optional<PluginCommand> optionalPluginCommand = CommandUtils.getCommand(commandObject.getName(), instance);
        if (!optionalPluginCommand.isPresent()) throw new IllegalArgumentException("PluginCommand cannot be null.");
        PluginCommand pluginCommand = optionalPluginCommand.get();
        pluginCommand.setAliases(commandObject.getAliases());

        pluginCommand.setExecutor(new FrameworkCommandExecutor(commandObject, subCommandObjects));
        pluginCommand.setTabCompleter(new FrameworkTabCompleter(commandObject.getPermission(), subCommandObjects));

        if (pluginCommand.isRegistered()) {
            // TODO: Unregister command
            pluginCommand.unregister(commandMap);
        }

        commandMap.register("CommandLibrary", pluginCommand);
        pluginCommand.register(commandMap);
        instance.getLogger().log(Level.WARNING, ChatColor.RED + "Command " + pluginCommand.getName() + " has been registered.");

    }

    private class FrameworkCommandExecutor implements CommandExecutor {

        private final BaseCommand commandObject;
        private final Set<SubCommand> subCommands;

        private final String noPermissionMessage = ChatColor.RED + "You are not allowed to execute this command.";

        FrameworkCommandExecutor(BaseCommand commandObject, Set<SubCommand> subCommandObjects) {
            this.commandObject = commandObject;
            this.subCommands = subCommandObjects;
        }

        @Override
        public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            CompletableFuture.runAsync(() -> {
                if (commandObject.getPermission() != null && !sender.hasPermission(commandObject.getPermission())) {
                    sender.sendMessage(noPermissionMessage);
                    return;
                }
                if (args.length == 0) {
                    if (commandObject.getInvokeMethod() == null) {
                        sender.sendMessage(commandObject.getUsage());
                        return;
                    }

                    if (commandObject.isAsync()) {
                        invoke(commandObject.getClassInstance(), commandObject.getInvokeMethod(), sender, new Object[]{sender});
                    } else {
                        Bukkit.getScheduler().runTask(instance, () -> invoke(commandObject.getClassInstance(), commandObject.getInvokeMethod(), sender, new Object[]{sender}));
                    }
                    return;
                }
                Optional<SubCommand> subCommandObject = getSubcommandMethod(findOriginalName(args[0]));
                if (!subCommandObject.isPresent()) {
                    return;
                }

                Method invokeMethod = subCommandObject.get().getInvokeMethod();
                Optional<Object[]> optionalParameters = ArgumentUtils.findArguments(sender, subCommandObject.get().getArgumentTypes(), subCommandObject.get().getUsage(), args)/*ArgumentUtils.findArguments(sender, invokeMethod.getParameters(), args)*/;
                if (!optionalParameters.isPresent()) {
                    return;
                }

                if (subCommandObject.get().isAsync()) {
                    invoke(commandObject.getClassInstance(), invokeMethod, sender, optionalParameters.get());
                } else {
                    Bukkit.getScheduler().runTask(instance, () -> invoke(commandObject.getClassInstance(), invokeMethod, sender, optionalParameters.get()));
                }
            });
            return true;
        }

        private void invoke(Object classInstance, Method method, CommandSender sender, Object[] parameters) {
            try {
                method.invoke(classInstance, parameters);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                sender.sendMessage("Developer made an error. Contact an admin as soon as possible.");
            }
        }

        private Optional<SubCommand> getSubcommandMethod(String originalName) {
            return subCommands.stream().filter(object -> object.getName().equalsIgnoreCase(originalName)).findFirst();
        }

        private String findOriginalName(String name) {
            String originalName = name;
            for (SubCommand subCommandObject : subCommands) {
                if (name.equalsIgnoreCase(subCommandObject.getName())) break;
                for (String alias : subCommandObject.getAliases()) {
                    if (!name.equalsIgnoreCase(alias)) continue;
                    originalName = subCommandObject.getName();
                    break;
                }
            }
            return originalName;
        }
    }

    private static class FrameworkTabCompleter implements TabCompleter {

        private final Map<String, String> subCommandPermissionMap;
        private final String basePermission;

        public FrameworkTabCompleter(String basPermission, Set<SubCommand> subCommandSet) {
            this.subCommandPermissionMap = new HashMap<>();
            this.basePermission = basPermission;
            subCommandSet.forEach(subCommand -> this.subCommandPermissionMap.put(subCommand.getName(), subCommand.getPermission()));
        }

        @Override
        public List<String> onTabComplete(CommandSender commandSender, org.bukkit.command.Command command, String label, String[] args) {
            if (!(commandSender instanceof Player) || (basePermission != null && !commandSender.hasPermission(basePermission))) return ImmutableList.of();

            List<String> toReturn = new ArrayList<>();
            if (args.length == 1) {
                for (Map.Entry<String, String> entry : this.subCommandPermissionMap.entrySet()) {
                    if (entry.getValue() != null && !commandSender.hasPermission(entry.getValue())) continue;
                    toReturn.add(entry.getKey());
                }
                return toReturn;
            }

            Player player = (Player) commandSender;
            for (Player all : Bukkit.getOnlinePlayers()) {
                if (StringUtil.startsWithIgnoreCase(player.getName(), args[0]) && player.canSee(all) && all.getUniqueId() != player.getUniqueId()) {
                    toReturn.add(all.getName());
                }
            }
            return toReturn;
        }
    }
}