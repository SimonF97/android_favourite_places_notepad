package com.example.lokale_notatnik;

//klasa pomocnicza pozwalajaca przechowywac status wyszukiwania lokalu w Google miedzy aktywnosciami
public class WebSearchStatus {
    static int searchStatus;

    public static void setSearchStatus(int status)
    {
        searchStatus = status;
    }

    public static int getSearchStatus()
    {
        return searchStatus;
    }
}
