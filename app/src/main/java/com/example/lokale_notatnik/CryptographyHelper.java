package com.example.lokale_notatnik;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

//klasa zawierajaca funkcje do szyfrowania hasel
public class CryptographyHelper {
    //zmienne potrzebne do generowania soli
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    //oblicz skrót SHA512
    public static String calculateSHA512(String text)
    {
        try {
            //instancja SHA512
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            //oblicz message digest ze stringa i zapisz go jako tablica bajtow
            byte[] messageDigest = md.digest(text.getBytes());
            //przekonwertuj tablice bajtow na reprezentacje signum
            BigInteger no = new BigInteger(1, messageDigest);
            //przekonwertuj message digest na kod szesnastkowy
            String hashtext = no.toString(16);
            //dodaj zera by calosc zajmowala 32 bity
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        //jesli podano zly algorytm message digest
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //generuj sol dla SHA512
    public static String generateSalt()
    {
        StringBuilder sb = new StringBuilder(16);
        for( int i = 0; i < 16; i++ ) sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    //funkjca zbiorcza do utworzenia skrotu hasla
    public static String generateSHA512Password(String password, String salt, String pepper)
    {
        String SHA512Hash = CryptographyHelper.calculateSHA512(pepper+salt+password);
        return SHA512Hash;
    }
}
