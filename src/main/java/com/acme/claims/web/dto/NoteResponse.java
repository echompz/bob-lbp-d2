package com.acme.claims.web.dto;

public class NoteResponse {

    private String author;
    private String text;
    private String createdAt;

    public NoteResponse() {
    }

    public NoteResponse(String author, String text, String createdAt) {
        this.author = author;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
