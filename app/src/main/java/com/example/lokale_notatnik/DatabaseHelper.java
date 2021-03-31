package com.example.lokale_notatnik;

import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DBNAME="LOKALE"; //Nazwa bazy
    private static final int DBVER=1; //Wersja bazy

    public DatabaseHelper(Context context){

        super(context, DBNAME, null, DBVER);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        //Utworzenie pustych tabeli, pierwsze uruchomienie aplikacji
        String sqlString=
                "CREATE TABLE UZYTKOWNIK (_id INTEGER PRIMARY KEY AUTOINCREMENT, LOGIN TEXT, HASLO TEXT, SOL TEXT)";
        sqLiteDatabase.execSQL(sqlString);
        sqlString =
                "CREATE TABLE LOKAL (_id INTEGER PRIMARY KEY AUTOINCREMENT, KATEGORIA INT, NAZWA TEXT, OCENA INT, OPIS TEXT, LOKALIZACJA_NAPIS TEXT, LOKALIZACJA_MIEJSCE TEXT, UZYTKOWNIK_ID INT)";
        sqLiteDatabase.execSQL(sqlString);
        sqlString=
                "CREATE TABLE KATEGORIA (_id INTEGER PRIMARY KEY AUTOINCREMENT, WYBOR INT, NAZWA TEXT)";
        sqLiteDatabase.execSQL(sqlString);
        sqlString=
                "CREATE TABLE ZDJECIE (_id INTEGER PRIMARY KEY AUTOINCREMENT, ZDJECIE_OBIEKT BLOB, LOKAL_ID INT)";
        sqLiteDatabase.execSQL(sqlString);

        //reczne dodanie kategorii do bazy
        ContentValues kategoria = new ContentValues();

        kategoria.clear();
        kategoria.put("WYBOR", 1);
        kategoria.put("NAZWA", "Restauracja");
        sqLiteDatabase.insert("KATEGORIA",null, kategoria); //-1 error

        kategoria.clear();
        kategoria.put("WYBOR", 2);
        kategoria.put("NAZWA", "Pub");
        sqLiteDatabase.insert("KATEGORIA",null, kategoria); //-1 error

        kategoria.clear();
        kategoria.put("WYBOR", 3);
        kategoria.put("NAZWA", "Przychodnia");
        sqLiteDatabase.insert("KATEGORIA",null, kategoria); //-1 error

        kategoria.clear();
        kategoria.put("WYBOR", 4);
        kategoria.put("NAZWA", "Warsztat");
        sqLiteDatabase.insert("KATEGORIA",null, kategoria); //-1 error

        kategoria.clear();
        kategoria.put("WYBOR", 5);
        kategoria.put("NAZWA", "Sklep");
        sqLiteDatabase.insert("KATEGORIA",null, kategoria); //-1 error
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}
    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}
}
