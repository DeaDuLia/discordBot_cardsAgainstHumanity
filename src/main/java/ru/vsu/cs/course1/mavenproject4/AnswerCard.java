package ru.vsu.cs.course1.mavenproject4;

import net.dv8tion.jda.core.entities.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;

public class AnswerCard extends  Card {

    public AnswerCard(String text) {
        super(text);
    }

    public static void addQuestion (Connection conn, Message msg, Map<Integer, AnswerCard> answerCardMap) {
        if (msg.getAuthor().getId().equals("456453436238725131") && msg.getContentRaw().startsWith("==add-answer")) {
            StringBuilder sb = new StringBuilder();
            Scanner sc = new Scanner(msg.getContentRaw());
            sc.next();
            while (sc.hasNext()) sb.append(sc.next()).append(" ");
            AnswerCard aCard = new AnswerCard(sb.toString());
            answerCardMap.put(aCard.getId(), aCard);
            try {
                String sql = "INSERT INTO answer VALUES (?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, aCard.getId());
                pstmt.setString(2, aCard.getText());
                pstmt.executeUpdate();
                conn.commit();
                System.out.println("sss");
            } catch (SQLException e) {

            }
        }
    }
}
