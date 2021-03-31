package com.example.lokale_notatnik;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
    static boolean active = false;
    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    String login;
    String password;
    String salt;
    String hashPassword;
    String pepper = "Pretty good pepper";
    String newHashPassword;
    int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if(isFinishing()) active = false;
    }

    //sprawdz czy EditText jest pusty
    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0)
            return false;
        return true;
    }

    //zaloguj sie do aplikacji
    public void login(View view)
    {
        //pobierz login i haslo z EditText i sprawdz czy nie sa puste
        EditText editLogin = findViewById(R.id.editLogin);
        EditText editPassword = findViewById(R.id.editPassword);
        //jesli puste wyswietl komunikat
        if(isEmpty(editLogin)) Toast.makeText(this, "Wprowadź login", Toast.LENGTH_SHORT).show();
        else if(isEmpty(editPassword)) Toast.makeText(this, "Wprowadź hasło", Toast.LENGTH_SHORT).show();
        else
        {
            try {
                login = editLogin.getText().toString();
                password = editPassword.getText().toString();
                //pobierz uzytkownika o podanym loginie z bazy
                SQLiteDatabase DB = DBHelper.getWritableDatabase();
                Cursor userResults = DB.query(
                        "UZYTKOWNIK",
                        new String[] {"_id", "HASLO", "SOL"},
                        "LOGIN = ?", new String[] {login},
                        null,null,null);
                //jesli nie znaleziono konta to wyswietl komunikat
                if(userResults.getCount() == 0) Toast.makeText(this, "Błędny login lub hasło", Toast.LENGTH_SHORT).show();
                else
                {
                    //pobierz dane z bazy
                    while(userResults.moveToNext())
                    {
                        id = userResults.getInt(0);
                        hashPassword = userResults.getString(1);
                        salt = userResults.getString(2);
                    }
                    //zahashuj wprowadzone haslo
                    newHashPassword = CryptographyHelper.generateSHA512Password(password, salt, pepper);
                    //jesli podane haslo zgadza sie z tym z bazy to przejdz do aktywnosci glownej
                    if(newHashPassword.equals(hashPassword))
                    {
                        Toast.makeText(this, "Pomyślnie zalogowano", Toast.LENGTH_SHORT).show();
                        User.setId(id);
                        Intent intent = new Intent(this, MainActivity.class);
                        //przekaz z intenetem wiadomosc o zalogowaniu - dzieki temu aktywnosc glowna nie bedzie mogla wrocic do tego panelu bez wylogowywania sie
                        intent.putExtra("activity","logged in");
                        startActivity(intent);
                        finish();
                    }
                    //w innym wypadku wyswietl komunikat
                    else Toast.makeText(this, "Błędny login lub hasło", Toast.LENGTH_SHORT).show();
                }
                userResults.close();
            } catch (SQLiteException e) {
                Toast.makeText(LoginActivity.this, "EXCEPTION:SET",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    //przejdz do panelu rejestracji
    public void goToRegister(View view)
    {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    //zablokuj przycisk powrotu
    @Override
    public void onBackPressed() {}
}
