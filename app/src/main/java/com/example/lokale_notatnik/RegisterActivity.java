package com.example.lokale_notatnik;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {
    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    String login;
    String password;
    String repeatedPassword;
    String salt;
    String hashedPassword;
    String pepper = "Pretty good pepper"; //pieprz potrzebny do zahashowania hasła
    int loginTaken = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }

    //sprawdz czy EditText jest pusty
    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0)
            return false;
        return true;
    }

    //zarejestruj uzytkownika
    public void register(View view) {
        EditText editLogin = findViewById(R.id.editLogin);
        EditText editPassword = findViewById(R.id.editPassword);
        EditText editPasswordRepeat = findViewById(R.id.editPasswordRepeat);
        //sprawdz czy pola nie sa puste oraz czy haslo zostalo wprowadzone dwa razy tak samo
        if(isEmpty(editLogin)) Toast.makeText(this, "Wprowadź login", Toast.LENGTH_SHORT).show();
        else if(isEmpty(editPassword)) Toast.makeText(this, "Wprowadź hasło", Toast.LENGTH_SHORT).show();
        else if(isEmpty(editPasswordRepeat)) Toast.makeText(this, "Wprowadź hasło ponownie", Toast.LENGTH_SHORT).show();
        else if(!editPassword.getText().toString().equals(editPasswordRepeat.getText().toString())) Toast.makeText(this, "Hasła muszą być takie same", Toast.LENGTH_SHORT).show();
        else {
            login = editLogin.getText().toString();
            password = editPassword.getText().toString();
            repeatedPassword = editPasswordRepeat.getText().toString();
            salt = CryptographyHelper.generateSalt();
            hashedPassword = CryptographyHelper.generateSHA512Password(password, salt, pepper);
            try {
                //sprawdz czy uzytkownik o takim loginie juz istnieje
                SQLiteDatabase DB = DBHelper.getWritableDatabase();
                Cursor userResults = DB.query(
                        "UZYTKOWNIK",
                        new String[] {"LOGIN"},
                        null, null,
                        null,null,null);
                while(userResults.moveToNext())
                {
                    if(login.equals(userResults.getString(0))) loginTaken = 1;
                }
                //jesli nie to wprowadz dane nowego uzytkownika do bazy
                if(loginTaken == 0)
                {
                    ContentValues userData = new ContentValues();
                    userData.clear();
                    userData.put("LOGIN", login);
                    userData.put("HASLO", hashedPassword);
                    userData.put("SOL", salt);
                    DB.insert("UZYTKOWNIK", null, userData); //-1 error
                    Toast.makeText(this, "Pomyślnie utworzono konto", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                }
                //w przeciwnym razie wyswietl komunikat
                else
                {
                    loginTaken = 0;
                    Toast.makeText(this, "Login zajęty", Toast.LENGTH_SHORT).show();
                }
                userResults.close();
            } catch (SQLiteException e) {
                Toast.makeText(RegisterActivity.this, "EXCEPTION:SET",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    //wroc do panelu logowania
    public void backToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}
