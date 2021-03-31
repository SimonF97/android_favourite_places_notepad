package com.example.lokale_notatnik;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    static boolean active = false;
    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    String id_user = Integer.toString(User.getId());
    String newLogin;
    String oldPassword;
    String newPassword;
    String newRepeatedPassword;
    String oldSalt;
    String newSalt;
    String oldHashedPassword;
    String newHashedPassword;
    String pepper = "Pretty good pepper"; //pieprz potrzebny do zahashowania hasła
    int loginTaken = 0;
    int wrongOldPassword = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    //stworz menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    //obsluguj menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.profil:
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("activity","ok");
                startActivity(intent);
                return true;
            case R.id.lista:
                intent = new Intent(this, ListActivity.class);
                intent.putExtra("activity","ok");
                startActivity(intent);
                return true;
            case R.id.dodawanie:
                intent = new Intent(this, AddPlaceActivity.class);
                intent.putExtra("activity","ok");
                startActivity(intent);
                return true;
            case R.id.wylogowanie:
                User.setId(0);
                intent = new Intent(this, LoginActivity.class);
                intent.putExtra("activity","ok");
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    //zmien login uzytkownika
    public void changeLogin(View view) {
        EditText editLogin = findViewById(R.id.editLogin);
        //sprawdz czy pole jest puste
        if(isEmpty(editLogin)) Toast.makeText(this, "Wprowadź nowy login", Toast.LENGTH_SHORT).show();
        else
        {
            try {
                //sprawdz czy login nie jest zajety
                SQLiteDatabase DB = DBHelper.getWritableDatabase();
                newLogin = editLogin.getText().toString();
                Cursor userResults = DB.query(
                        "UZYTKOWNIK",
                        new String[] {"LOGIN"},
                        null, null,
                        null,null,null);
                while(userResults.moveToNext())
                {
                    if(newLogin.equals(userResults.getString(0))) loginTaken = 1;
                }
                //jesli nie to zaktualizuj dane konta
                if(loginTaken == 0)
                {
                    ContentValues userData = new ContentValues();
                    userData.clear();
                    userData.put("LOGIN", newLogin);
                    DB.update("UZYTKOWNIK", userData,"_id = ?", new String[] {id_user});
                    Toast.makeText(this, "Pomyślnie zmieniono login", Toast.LENGTH_SHORT).show();
                }
                //jesli tak to wyswietl komunikat
                else
                {
                    loginTaken = 0;
                    Toast.makeText(this, "Login zajęty", Toast.LENGTH_SHORT).show();
                }
                userResults.close();
            } catch (SQLiteException e) {
                Toast.makeText(ProfileActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    //zmien haslo uzytkownika
    public void changePassword(View view) {
        EditText editOldPassword = findViewById(R.id.editOldPassword);
        EditText editNewPassword = findViewById(R.id.editNewPassword);
        EditText editNewPasswordRepeat = findViewById(R.id.editNewPasswordRepeat);
        //sprawddz czy pola nie sa puste, czy nowe haslo zostalo wpisane dwa razy tak samo oraz czy stare i nowe haslo nie sa takie same
        if(isEmpty(editOldPassword)) Toast.makeText(this, "Wprowadź stare hasło", Toast.LENGTH_SHORT).show();
        else if(isEmpty(editNewPassword)) Toast.makeText(this, "Wprowadź nowe hasło", Toast.LENGTH_SHORT).show();
        else if(isEmpty(editNewPasswordRepeat)) Toast.makeText(this, "Wprowadź nowe hasło ponownie", Toast.LENGTH_SHORT).show();
        else if(!editNewPassword.getText().toString().equals(editNewPasswordRepeat.getText().toString())) Toast.makeText(this, "Hasła muszą być takie same", Toast.LENGTH_SHORT).show();
        else if(editNewPassword.getText().toString().equals(editOldPassword.getText().toString())) Toast.makeText(this, "Nowe hasło nie może być takie samo jak stare", Toast.LENGTH_SHORT).show();
        else
        {
            oldPassword = editOldPassword.getText().toString();
            newPassword = editNewPassword.getText().toString();
            newRepeatedPassword = editNewPasswordRepeat.getText().toString();
            try {
                //pobierz haslo oraz sol z bazy
                SQLiteDatabase DB = DBHelper.getWritableDatabase();
                Cursor userResults = DB.query(
                        "UZYTKOWNIK",
                        new String[] {"HASLO", "SOL"},
                        "_id = ?", new String[] {id_user},
                        null,null,null);
                while(userResults.moveToNext())
                {
                    oldSalt = userResults.getString(1);
                    //zahashuj wprowadzone haslo
                    oldHashedPassword = CryptographyHelper.generateSHA512Password(oldPassword, oldSalt, pepper);
                    //sprawdz czy stare haslo zgadza sie z tym pobraym z bazy
                    if(!oldHashedPassword.equals(userResults.getString(0))) wrongOldPassword = 1;
                }
                //jesli haslo sie zgadza to zaktualizuj dane uzytkownika w bazie
                if(wrongOldPassword == 0)
                {
                    newSalt = CryptographyHelper.generateSalt();
                    newHashedPassword = CryptographyHelper.generateSHA512Password(newPassword, newSalt, pepper);
                    ContentValues userData = new ContentValues();
                    userData.clear();
                    userData.put("HASLO", newHashedPassword);
                    userData.put("SOL", newSalt);
                    DB.update("UZYTKOWNIK", userData,"_id = ?", new String[] {id_user});
                    Toast.makeText(this, "Pomyślnie zmieniono hasło", Toast.LENGTH_SHORT).show();
                }
                //jesli nie to wyswietl komunikat
                else
                {
                    wrongOldPassword = 0;
                    Toast.makeText(this, "Niepoprawne stare hasło", Toast.LENGTH_SHORT).show();
                }
                userResults.close();
            } catch (SQLiteException e) {
                Toast.makeText(ProfileActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    //jesli poprzednia aktywnosc nie byla zakonczona usunieciem lokalu to wroc
    @Override
    public void onBackPressed()
    {
        Intent intent = getIntent();
        String activity = intent.getStringExtra("activity");
        if(!activity.equals("deleted place")) finish();
        else Toast.makeText(this, "Ten lokal już nie istnieje", Toast.LENGTH_SHORT).show();
    }
}
