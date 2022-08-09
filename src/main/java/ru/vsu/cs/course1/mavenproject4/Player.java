package ru.vsu.cs.course1.mavenproject4;


import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.List;
public class Player {
    public static JDA jda; // Нужно объявить в мейн
    User user;
    List<AnswerCard> cards = new ArrayList<>();
    int points = 0;
    String botMessageId;
    EmbedBuilder eb;
    public Player (User user) {
        this.user = jda.getUserById(user.getId());
    }
}
