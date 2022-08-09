package ru.vsu.cs.course1.mavenproject4;

public class Card {
    public static int ids = 0;
    private String text;
    private  int id = 0;
    Player master;
    public Card (String text) {
        this.text = text;
        id = ids;
        ids++;
    }
    public String getText() {
        return text;
    }

    public int getId() {
        return id;
    }

    public void setText(String text) {
        this.text = text;
    }
}
