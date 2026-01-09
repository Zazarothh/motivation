/*
 * Copyright (c) 2024, Zazarothh
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.motivation;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class MotivationPanel extends PluginPanel
{
    private static final int SKILL_ICON_SIZE = 18;

    private final MotivationPlugin plugin;
    private final Client client;
    private final JPanel skillListPanel;
    private final JLabel headerLabel;
    private final JButton refreshButton;

    private final Map<Skill, BufferedImage> skillIcons = new HashMap<>();

    public MotivationPanel(MotivationPlugin plugin, Client client)
    {
        super(false);
        this.plugin = plugin;
        this.client = client;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        headerLabel = new JLabel("Skills to Improve");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        refreshButton = new JButton("Refresh");
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshData());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Skills list panel
        skillListPanel = new JPanel();
        skillListPanel.setLayout(new BoxLayout(skillListPanel, BoxLayout.Y_AXIS));
        skillListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(skillListPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // Initial message
        updateSkillList(null, null);

        // Load skill icons
        loadSkillIcons();
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
                    skillIcons.put(skill, ImageUtil.resizeImage(icon, SKILL_ICON_SIZE, SKILL_ICON_SIZE));
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

            List<MotivationPlugin.SkillData> skills = plugin.getNon99Skills();

            if (result != null)
            {
                // Update ranks from hiscore result
                for (MotivationPlugin.SkillData skillData : skills)
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

            updateSkillList(skills, result);
        });
    }

    private void updateSkillList(List<MotivationPlugin.SkillData> skills, HiscoreResult result)
    {
        skillListPanel.removeAll();

        if (skills == null || skills.isEmpty())
        {
            JLabel emptyLabel = new JLabel("Log in to see your skills");
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            skillListPanel.add(Box.createVerticalGlue());
            skillListPanel.add(emptyLabel);
            skillListPanel.add(Box.createVerticalGlue());
        }
        else
        {
            headerLabel.setText("Skills to Improve (" + skills.size() + ")");

            for (MotivationPlugin.SkillData skillData : skills)
            {
                JPanel skillRow = createSkillRow(skillData);
                skillListPanel.add(skillRow);
                skillListPanel.add(Box.createVerticalStrut(5));
            }
        }

        skillListPanel.revalidate();
        skillListPanel.repaint();
    }

    private JPanel createSkillRow(MotivationPlugin.SkillData skillData)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(4, 6, 4, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        // Top section: icon, name, percentage, level
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Left side: icon and skill name
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        leftPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        BufferedImage icon = skillIcons.get(skillData.getSkill());
        if (icon != null)
        {
            JLabel iconLabel = new JLabel(new ImageIcon(icon));
            leftPanel.add(iconLabel);
        }

        String skillName = formatSkillName(skillData.getSkill());
        JLabel nameLabel = new JLabel(skillName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        leftPanel.add(nameLabel);

        topPanel.add(leftPanel, BorderLayout.WEST);

        // Center: percentage
        String percentText = String.format("%.1f%%", skillData.getPercentTo99());
        JLabel percentLabel = new JLabel(percentText);
        percentLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        percentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(percentLabel, BorderLayout.CENTER);

        // Right side: level and rank
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel levelLabel = new JLabel("Lvl " + skillData.getLevel());
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(levelLabel);

        String rankText = skillData.getRank() > 0
            ? "Rank " + NumberFormat.getNumberInstance(Locale.US).format(skillData.getRank())
            : "Rank --";
        JLabel rankLabel = new JLabel(rankText);
        rankLabel.setForeground(skillData.getRank() > 0 ? new Color(255, 176, 0) : Color.GRAY);
        rankLabel.setFont(rankLabel.getFont().deriveFont(Font.PLAIN, 11f));
        rankLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(rankLabel);

        topPanel.add(rightPanel, BorderLayout.EAST);

        row.add(topPanel, BorderLayout.CENTER);

        // Progress bar at bottom
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue((int) skillData.getPercentTo99());
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 5));
        progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
        progressBar.setForeground(new Color(50, 200, 50));
        progressBar.setBorderPainted(false);

        row.add(progressBar, BorderLayout.SOUTH);

        return row;
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
