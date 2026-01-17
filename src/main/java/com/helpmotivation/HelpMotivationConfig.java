package com.helpmotivation;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("helpmotivation")
public interface HelpMotivationConfig extends Config
{
    @ConfigItem(
        keyName = "showLoginMessage",
        name = "Show Login Message",
        description = "Display a motivational message when you log in",
        position = 1
    )
    default boolean showLoginMessage()
    {
        return true;
    }

    @ConfigItem(
        keyName = "useOverhead",
        name = "Use Overhead Text",
        description = "Show message as overhead text above your character instead of in chat",
        position = 2
    )
    default boolean useOverhead()
    {
        return false;
    }

    @ConfigItem(
        keyName = "customQuotes",
        name = "Custom Quotes",
        description = "Add your own motivational insults (one per line). Use %d for rank placeholder.",
        position = 3
    )
    default String customQuotes()
    {
        return "";
    }
}
