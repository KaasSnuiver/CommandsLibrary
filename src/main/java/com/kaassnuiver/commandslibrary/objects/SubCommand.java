package com.kaassnuiver.commandslibrary.objects;

import com.kaassnuiver.commandslibrary.utils.ArgumentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.List;

@Data
@AllArgsConstructor
public class SubCommand {

    private final String name;
    private final String permission;
    private final List<String> aliases;
    private final boolean async;
    private final Method invokeMethod;
    private final String usage;
    private final ArgumentUtils.ArgumentType[] argumentTypes;

}

