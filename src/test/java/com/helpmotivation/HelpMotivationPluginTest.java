package com.helpmotivation;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HelpMotivationPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(HelpMotivationPlugin.class);
        RuneLite.main(args);
    }
}
