package com.helpmotivation;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
    name = "Help Motivation",
    description = "Motivational messages about your lowest skill ranks",
    tags = {"hiscore", "motivation", "skill", "rank"}
)
public class HelpMotivationPlugin extends Plugin
{
    private static final String[] DEFAULT_QUOTES = {
        "Are you touching grass too often?",
        "Rank %s and not getting better. Grats!",
        "Get that XP, the grind doesn't stop.",
        "Your grandmother has a higher rank than this."
    };

    private static final Set<Skill> PURE_COMBAT_SKILLS = EnumSet.of(
        Skill.ATTACK,
        Skill.STRENGTH,
        Skill.DEFENCE,
        Skill.RANGED,
        Skill.MAGIC,
        Skill.PRAYER
    );

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private HelpMotivationConfig config;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private HiscoreClient hiscoreClient;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ScheduledExecutorService executor;

    @Getter
    private HelpMotivationPanel panel;

    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception
    {
        panel = new HelpMotivationPanel(this, client);

        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
        if (icon == null)
        {
            icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }

        navButton = NavigationButton.builder()
            .tooltip("Help Motivation")
            .icon(icon)
            .priority(10)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);

        log.info("Help Motivation plugin started");
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
        log.info("Help Motivation plugin stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            executor.schedule(() ->
                clientThread.invokeLater(() ->
                {
                    Player localPlayer = client.getLocalPlayer();
                    if (localPlayer != null && localPlayer.getName() != null)
                    {
                        if (config.showLoginMessage())
                        {
                            checkAndDisplayMessage(localPlayer.getName());
                        }
                        refreshPanelData(localPlayer.getName());
                    }
                }),
                3, TimeUnit.SECONDS
            );
        }
    }

    private void checkAndDisplayMessage(String playerName)
    {
        Optional<Skill> lowestSkill = getLowestNon99Skill();
        if (!lowestSkill.isPresent())
        {
            sendChatMessage("All skills at 99! You absolute legend!");
            return;
        }

        Skill skill = lowestSkill.get();
        int level = client.getRealSkillLevel(skill);

        executor.submit(() ->
        {
            try
            {
                HiscoreResult result = hiscoreClient.lookup(playerName);
                if (result != null)
                {
                    HiscoreSkill hiscoreSkill = skillToHiscoreSkill(skill);
                    if (hiscoreSkill != null)
                    {
                        net.runelite.client.hiscore.Skill hiscoreData = result.getSkill(hiscoreSkill);
                        if (hiscoreData != null && hiscoreData.getRank() != -1)
                        {
                            int rank = hiscoreData.getRank();
                            String message = formatMessage(skill, level, rank);

                            SwingUtilities.invokeLater(() ->
                            {
                                if (config.useOverhead())
                                {
                                    clientThread.invokeLater(() ->
                                    {
                                        Player player = client.getLocalPlayer();
                                        if (player != null)
                                        {
                                            player.setOverheadText(message);
                                            player.setOverheadCycle(200);
                                        }
                                    });
                                }
                                else
                                {
                                    sendChatMessage(message);
                                }
                            });
                        }
                        else
                        {
                            sendChatMessage(formatMessageNoRank(skill, level));
                        }
                    }
                }
            }
            catch (IOException e)
            {
                log.warn("Failed to lookup hiscore for {}", playerName, e);
                SwingUtilities.invokeLater(() -> sendChatMessage(formatMessageNoRank(skill, level)));
            }
        });
    }

    void refreshPanelData(String playerName)
    {
        executor.submit(() ->
        {
            try
            {
                HiscoreResult result = hiscoreClient.lookup(playerName);
                SwingUtilities.invokeLater(() -> panel.updateSkillData(result));
            }
            catch (IOException e)
            {
                log.warn("Failed to lookup hiscore for panel", e);
                SwingUtilities.invokeLater(() -> panel.updateSkillData(null));
            }
        });
    }

    private boolean isPureSkillAtLevel1(Skill skill, int level)
    {
        return PURE_COMBAT_SKILLS.contains(skill) && level == 1;
    }

    private Optional<Skill> getLowestNon99Skill()
    {
        return Arrays.stream(Skill.values())
            .filter(s -> s != Skill.OVERALL)
            .filter(s -> {
                try
                {
                    int level = client.getRealSkillLevel(s);
                    int xp = client.getSkillExperience(s);
                    return level < 99 && xp >= 0 && !isPureSkillAtLevel1(s, level);
                }
                catch (Exception e)
                {
                    return false;
                }
            })
            .min(Comparator.comparingInt(s -> client.getRealSkillLevel(s)));
    }

    List<SkillData> getNon99Skills()
    {
        List<SkillData> skills = new ArrayList<>();
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }

            try
            {
                int level = client.getRealSkillLevel(skill);
                int xp = client.getSkillExperience(skill);
                if (level < 99 && xp >= 0 && !isPureSkillAtLevel1(skill, level))
                {
                    skills.add(new SkillData(skill, level, xp, -1));
                }
            }
            catch (Exception e)
            {
            }
        }

        skills.sort(Comparator.comparingInt(SkillData::getLevel));
        return skills;
    }

    private String formatMessage(Skill skill, int level, int rank)
    {
        String quote = getRandomQuote(rank);
        String skillName = formatSkillName(skill);

        return String.format("Level %d %s... %s", level, skillName, quote);
    }

    private String formatMessageNoRank(Skill skill, int level)
    {
        String quote = getRandomQuote(-1);
        String skillName = formatSkillName(skill);

        return String.format("Level %d %s... %s", level, skillName, quote);
    }

    private String formatSkillName(Skill skill)
    {
        String name = skill.getName();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    private String getRandomQuote(int rank)
    {
        List<String> quotes = new ArrayList<>();

        String customQuotesStr = config.customQuotes();
        if (customQuotesStr != null)
        {
            String[] customQuotes = customQuotesStr.split("\\r?\\n|\\\\n");
            for (String q : customQuotes)
            {
                if (!q.trim().isEmpty())
                {
                    quotes.add(q.trim());
                }
            }
        }

        if (quotes.isEmpty())
        {
            quotes.addAll(Arrays.asList(DEFAULT_QUOTES));
        }

        Random random = new Random();
        String quote = quotes.get(random.nextInt(quotes.size()));

        if (rank > 0 && (quote.contains("%s") || quote.contains("%d")))
        {
            String formattedRank = NumberFormat.getNumberInstance(Locale.US).format(rank);
            quote = quote.replace("%d", formattedRank).replace("%s", formattedRank);
        }

        return quote;
    }

    private void sendChatMessage(String message)
    {
        final String chatMessage = new ChatMessageBuilder()
            .append(ChatColorType.HIGHLIGHT)
            .append("[Help Motivation] ")
            .append(ChatColorType.NORMAL)
            .append(message)
            .build();

        chatMessageManager.queue(QueuedMessage.builder()
            .type(ChatMessageType.CONSOLE)
            .runeLiteFormattedMessage(chatMessage)
            .build());
    }

    private HiscoreSkill skillToHiscoreSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK:
                return HiscoreSkill.ATTACK;
            case DEFENCE:
                return HiscoreSkill.DEFENCE;
            case STRENGTH:
                return HiscoreSkill.STRENGTH;
            case HITPOINTS:
                return HiscoreSkill.HITPOINTS;
            case RANGED:
                return HiscoreSkill.RANGED;
            case PRAYER:
                return HiscoreSkill.PRAYER;
            case MAGIC:
                return HiscoreSkill.MAGIC;
            case COOKING:
                return HiscoreSkill.COOKING;
            case WOODCUTTING:
                return HiscoreSkill.WOODCUTTING;
            case FLETCHING:
                return HiscoreSkill.FLETCHING;
            case FISHING:
                return HiscoreSkill.FISHING;
            case FIREMAKING:
                return HiscoreSkill.FIREMAKING;
            case CRAFTING:
                return HiscoreSkill.CRAFTING;
            case SMITHING:
                return HiscoreSkill.SMITHING;
            case MINING:
                return HiscoreSkill.MINING;
            case HERBLORE:
                return HiscoreSkill.HERBLORE;
            case AGILITY:
                return HiscoreSkill.AGILITY;
            case THIEVING:
                return HiscoreSkill.THIEVING;
            case SLAYER:
                return HiscoreSkill.SLAYER;
            case FARMING:
                return HiscoreSkill.FARMING;
            case RUNECRAFT:
                return HiscoreSkill.RUNECRAFT;
            case HUNTER:
                return HiscoreSkill.HUNTER;
            case CONSTRUCTION:
                return HiscoreSkill.CONSTRUCTION;
            default:
                try
                {
                    return HiscoreSkill.valueOf(skill.name());
                }
                catch (IllegalArgumentException e)
                {
                    return null;
                }
        }
    }

    @Provides
    HelpMotivationConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HelpMotivationConfig.class);
    }

    @Getter
    public static class SkillData
    {
        private static final int MAX_XP = 13_034_431;
        private final Skill skill;
        private final int level;
        private final int xp;
        private int rank;

        public SkillData(Skill skill, int level, int xp, int rank)
        {
            this.skill = skill;
            this.level = level;
            this.xp = xp;
            this.rank = rank;
        }

        public void setRank(int rank)
        {
            this.rank = rank;
        }

        public double getPercentTo99()
        {
            return (xp * 100.0) / MAX_XP;
        }

        public int getXp()
        {
            return xp;
        }
    }
}
