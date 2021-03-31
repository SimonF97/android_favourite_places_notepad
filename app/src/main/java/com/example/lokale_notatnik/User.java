package com.example.lokale_notatnik;

//klasa pomocnicza pozwalajaca przechowywac id uzytkownika miedzy aktywnosciami
public class User {
    static int id_user;

    public static void setId(int id)
    {
        id_user = id;
    }

    public static int getId()
    {
        return id_user;
    }
}
