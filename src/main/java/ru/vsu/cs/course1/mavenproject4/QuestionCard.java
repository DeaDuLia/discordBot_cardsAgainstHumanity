package ru.vsu.cs.course1.mavenproject4;

import net.dv8tion.jda.core.entities.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;

public class QuestionCard  extends Card{

    public QuestionCard(String text) {
        super(text);
    }

    public static void addQuestion (Connection conn, Message msg, Map<Integer, QuestionCard> questionCardMap) {
        if (msg.getAuthor().getId().equals("456453436238725131") && msg.getContentRaw().startsWith("==add-question")) {
            StringBuilder sb = new StringBuilder();
            Scanner sc = new Scanner(msg.getContentRaw());
            sc.next();
            while (sc.hasNext()) sb.append(sc.next()).append(" ");
            QuestionCard qCard = new QuestionCard(sb.toString());
            questionCardMap.put(qCard.getId(), qCard);
            try {
                String sql = "INSERT INTO question VALUES (?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, qCard.getId());
                pstmt.setString(2, qCard.getText());
                pstmt.executeUpdate();
                conn.commit();
                System.out.println("sss");
            } catch (SQLException e) {

            }
        }
    }
}
