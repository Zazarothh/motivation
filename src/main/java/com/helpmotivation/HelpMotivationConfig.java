package com.helpmotivation;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("helpmotivation")
public interface HelpMotivationConfig extends Config
{
    @ConfigItem(
        keyName = "enableMessages",
        name = "Enable Messages",
        description = "Toggle motivational messages on/off",
        position = 1
    )
    default boolean enableMessages()
    {
        return true;
    }

    @ConfigItem(
        keyName = "messageInterval",
        name = "Message Interval",
        description = "How often to show motivational messages",
        position = 2
    )
    default String messageInterval()
    {
        return "30m";
    }

    @ConfigItem(
        keyName = "useOverhead",
        name = "Use Overhead Text",
        description = "Show message as overhead text above your character instead of in chat",
        position = 3
    )
    default boolean useOverhead()
    {
        return false;
    }

    @ConfigItem(
        keyName = "pureMode",
        name = "Pure Mode",
        description = "Fully ignore combat skills (Attack, Strength, Defence, Ranged, Magic, Prayer)",
        position = 4
    )
    default boolean pureMode()
    {
        return true;
    }

    @ConfigItem(
        keyName = "customQuotes",
        name = "Custom Quotes",
        description = "Add your own motivational insults (one per line). Use %d for rank placeholder.",
        position = 5
    )
    default String customQuotes()
    {
        return "";
    }
}
