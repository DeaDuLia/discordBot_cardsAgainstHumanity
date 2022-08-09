package ru.vsu.cs.course1.mavenproject4;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * @author deadulia
 */
public class Main {
    public static void main(String[] args) throws LoginException {


        Locale.setDefault(Locale.ENGLISH);
        JDABuilder jda = new JDABuilder();
        jda.setToken("TOKEN"); //
        String url = "URL";
        String userName = "USER";
        String password = "PASS";
        Connection connection = null;
        Map<Integer, QuestionCard> qCards = new HashMap<>();
        Map<Integer, AnswerCard> aCards = new HashMap<>();
        try {
            Driver driver = new org.postgresql.Driver();
            DriverManager.registerDriver(driver);
            connection = DriverManager.getConnection(url, userName, password);
        } catch (Exception ignored) {

        }
        Connection finalConnection = connection;
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(MessageReceivedEvent event) {
                Message msg = event.getMessage();
                TextChannel ch = event.getTextChannel();
                try {
                    TheCardsAgainstTheHumanityTheGame.createGame(msg);
                    TheCardsAgainstTheHumanityTheGame.startGame(msg, qCards, aCards);
                    TheCardsAgainstTheHumanityTheGame.leaveGame(msg);
                    TheCardsAgainstTheHumanityTheGame.joinGame(msg);

                    QuestionCard.addQuestion(finalConnection, msg, qCards);
                    AnswerCard.addQuestion(finalConnection, msg, aCards);
                    TheCardsAgainstTheHumanityTheGame.help(msg);
                    TheCardsAgainstTheHumanityTheGame.rules(msg);

                } catch (Exception e) {
                    ch.sendMessage(e.getMessage()).queue();
                }
            }
        });
        jda.build();
    }

    /*

     */


}
