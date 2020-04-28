package com.example.bookshelf;

import java.io.Serializable;

public class Book implements Serializable {
    public int id;
    String title;
    String author;
    String cover_url;
    int duration;
}
