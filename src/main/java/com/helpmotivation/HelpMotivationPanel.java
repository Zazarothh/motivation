package com.helpmotivation;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class HelpMotivationPanel extends PluginPanel
{
    private static final int SKILL_COLUMN_WIDTH = 30;
    private static final int LEVEL_COLUMN_WIDTH = 35;
    private static final int PERCENT_COLUMN_WIDTH = 50;
    private static final int RANK_COLUMN_WIDTH = 70;
    private static final int ROW_HEIGHT = 25;

    private static final Color ODD_ROW = new Color(44, 44, 44);
    private static final Color EVEN_ROW = ColorScheme.DARK_GRAY_COLOR;

    private final HelpMotivationPlugin plugin;
    private final Client client;
    private final JPanel skillListPanel;
    private final JLabel headerLabel;
    private final JButton refreshButton;

    private final Map<Skill, BufferedImage> skillIcons = new EnumMap<>(Skill.class);

    public HelpMotivationPanel(HelpMotivationPlugin plugin, Client client)
    {
        super(false);
        this.plugin = plugin;
        this.client = client;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 8, 0));

        headerLabel = new JLabel("Skills to Improve");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerPanel.add(headerLabel, BorderLayout.WEST);

        refreshButton = new JButton("Refresh");
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshData());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        northPanel.add(headerPanel, BorderLayout.NORTH);
        northPanel.add(buildColumnHeaders(), BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);

        skillListPanel = new JPanel(new GridBagLayout());
        skillListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrapper.add(skillListPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        loadSkillIcons();
        updateSkillList(null);
    }

    private JPanel buildColumnHeaders()
    {
        JPanel headers = new JPanel(new GridBagLayout());
        headers.setBackground(ColorScheme.SCROLL_TRACK_COLOR);
        headers.setBorder(new EmptyBorder(4, 0, 4, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 0;
        headers.add(buildHeaderColumn("Skill", SKILL_COLUMN_WIDTH, SwingConstants.LEFT), c);

        c.gridx = 1;
        c.weightx = 0;
        headers.add(buildHeaderColumn("Lvl", LEVEL_COLUMN_WIDTH, SwingConstants.LEFT), c);

        c.gridx = 2;
        c.weightx = 1;
        headers.add(buildHeaderColumn("Progress", PERCENT_COLUMN_WIDTH, SwingConstants.CENTER), c);

        c.gridx = 3;
        c.weightx = 0;
        headers.add(buildHeaderColumn("Rank", RANK_COLUMN_WIDTH, SwingConstants.RIGHT), c);

        return headers;
    }

    private JPanel buildHeaderColumn(String name, int width, int alignment)
    {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(width, 16));

        if (alignment == SwingConstants.RIGHT)
        {
            column.setBorder(new EmptyBorder(0, 0, 0, 5));
        }
        else if (alignment == SwingConstants.CENTER)
        {
            column.setBorder(new EmptyBorder(0, 0, 0, 0));
        }
        else
        {
            column.setBorder(new EmptyBorder(0, 4, 0, 0));
        }

        JLabel label = new JLabel(name);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setHorizontalAlignment(alignment);

        String position = BorderLayout.WEST;
        if (alignment == SwingConstants.RIGHT) position = BorderLayout.EAST;
        else if (alignment == SwingConstants.CENTER) position = BorderLayout.CENTER;
        column.add(label, position);

        return column;
    }

    private void loadSkillIcons()
    {
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }

            try
            {
                String iconPath = "/skill_icons_small/" + skill.getName().toLowerCase() + ".png";
                BufferedImage icon = ImageUtil.loadImageResource(getClass(), iconPath);
                if (icon != null)
                {
                    skillIcons.put(skill, icon);
                }
            }
            catch (Exception e)
            {
                log.debug("Could not load icon for skill: {}", skill.getName());
            }
        }
    }

    private void refreshData()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && localPlayer.getName() != null)
        {
            refreshButton.setEnabled(false);
            refreshButton.setText("Loading...");
            plugin.refreshPanelData(localPlayer.getName());
        }
    }

    public void updateSkillData(HiscoreResult result)
    {
        SwingUtilities.invokeLater(() ->
        {
            refreshButton.setEnabled(true);
            refreshButton.setText("Refresh");

            List<HelpMotivationPlugin.SkillData> skills = plugin.getNon99Skills();

            if (result != null)
            {
                for (HelpMotivationPlugin.SkillData skillData : skills)
                {
                    HiscoreSkill hiscoreSkill = skillToHiscoreSkill(skillData.getSkill());
                    if (hiscoreSkill != null)
                    {
                        net.runelite.client.hiscore.Skill hiscoreData = result.getSkill(hiscoreSkill);
                        if (hiscoreData != null)
                        {
                            skillData.setRank(hiscoreData.getRank());
                        }
                    }
                }
            }

            updateSkillList(skills);
        });
    }

    private void updateSkillList(List<HelpMotivationPlugin.SkillData> skills)
    {
        skillListPanel.removeAll();

        if (skills == null || skills.isEmpty())
        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(20, 0, 20, 0);

            JLabel emptyLabel = new JLabel("Log in to see your skills");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            skillListPanel.add(emptyLabel, c);
        }
        else
        {
            headerLabel.setText("Skills to Improve (" + skills.size() + ")");

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;

            for (int i = 0; i < skills.size(); i++)
            {
                HelpMotivationPlugin.SkillData skillData = skills.get(i);
                JPanel row = createSkillRow(skillData, i % 2 == 1);
                skillListPanel.add(row, c);
                c.gridy++;
            }
        }

        skillListPanel.revalidate();
        skillListPanel.repaint();
    }

    private JPanel createSkillRow(HelpMotivationPlugin.SkillData skillData, boolean odd)
    {
        Color bgColor = odd ? ODD_ROW : EVEN_ROW;

        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(bgColor);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));

        row.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                row.setBackground(bgColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                row.setBackground(bgColor);
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 0;
        row.add(buildSkillIcon(skillData.getSkill()), c);

        c.gridx = 1;
        c.weightx = 0;
        row.add(buildLevelColumn(skillData.getLevel()), c);

        c.gridx = 2;
        c.weightx = 1;
        row.add(buildPercentColumn(skillData.getPercentTo99()), c);

        c.gridx = 3;
        c.weightx = 0;
        row.add(buildRankColumn(skillData.getRank()), c);

        String tooltip = String.format("%s - Level %d - %.1f%% to 99",
            formatSkillName(skillData.getSkill()),
            skillData.getLevel(),
            skillData.getPercentTo99());
        row.setToolTipText(tooltip);

        return row;
    }

    private JPanel buildSkillIcon(Skill skill)
    {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(SKILL_COLUMN_WIDTH, ROW_HEIGHT));
        column.setBorder(new EmptyBorder(0, 4, 0, 0));

        BufferedImage icon = skillIcons.get(skill);
        if (icon != null)
        {
            JLabel label = new JLabel(new ImageIcon(icon));
            column.add(label, BorderLayout.CENTER);
        }

        return column;
    }

    private JPanel buildLevelColumn(int level)
    {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(LEVEL_COLUMN_WIDTH, ROW_HEIGHT));
        column.setBorder(new EmptyBorder(0, 4, 0, 0));

        JLabel label = new JLabel(String.valueOf(level));
        label.setForeground(Color.WHITE);
        column.add(label, BorderLayout.WEST);

        return column;
    }

    private JPanel buildPercentColumn(double percent)
    {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(PERCENT_COLUMN_WIDTH, ROW_HEIGHT));

        JLabel label = new JLabel(String.format("%.1f%%", percent));
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        column.add(label, BorderLayout.CENTER);

        return column;
    }

    private JPanel buildRankColumn(int rank)
    {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(RANK_COLUMN_WIDTH, ROW_HEIGHT));
        column.setBorder(new EmptyBorder(0, 0, 0, 5));

        String rankText = rank > 0
            ? NumberFormat.getNumberInstance(Locale.US).format(rank)
            : "--";

        JLabel label = new JLabel(rankText);
        label.setForeground(rank > 0 ? ColorScheme.BRAND_ORANGE : Color.GRAY);
        label.setFont(FontManager.getRunescapeSmallFont());
        column.add(label, BorderLayout.EAST);

        return column;
    }

    private String formatSkillName(Skill skill)
    {
        String name = skill.getName();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
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
}
