package com.example.lokale_notatnik;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    String id_user = Integer.toString(User.getId());
    String login = "";

    TextView powitanie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        powitanie = findViewById(R.id.textView1);
        greetings(); //ustaw odpowiednie powitanie
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

    //jesli aktywnosc zrestartowana to wyswietl ponownie powitanie
    @Override
    public void onRestart() {
        super.onRestart();
        greetings();
    }

    //wyswietl powitanie
    public void greetings()
    {
        try {
            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            Cursor userResults = DB.query(
                    "UZYTKOWNIK",
                    null,
                    "_id = ?", new String[] {id_user},
                    null,null,null);
            while(userResults.moveToNext()) {
                login = userResults.getString(1);
            }
            powitanie.setText("Witaj użytkowniku "+login+"!");
            userResults.close();
        } catch (SQLiteException e) {
            Toast.makeText(MainActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //przejdz do ustawien profilu
    public void goToProfile(View view)
    {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("activity","ok");
        startActivity(intent);
    }

    //wyswietl pytanie o usuniecie konta
    public void deleteAccount(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("Potwierdzenie");
        builder.setMessage("Czy na pewno chcesz usunąć konto?");
        builder.setPositiveButton("Tak",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteHelp();
                    }
                });
        builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //metoda pomocnicza do usuwania konta uzytkownika
    public void deleteHelp()
    {
        try {
            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            DB.delete("UZYTKOWNIK", "_id = ?", new String[] {id_user});
            Toast.makeText(this, "Konto zostało usunięte", Toast.LENGTH_SHORT).show();
            User.setId(0);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } catch (SQLiteException e) {
            Toast.makeText(MainActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //wroc jesli warunki spelnione
    @Override
    public void onBackPressed()
    {
        Intent intent = getIntent();
        String activity = intent.getStringExtra("activity");
        //jesli ostatnia aktywnosc nie byla logowaniem lub usunieciem lokalu to wroc
        if(!activity.equals("deleted place") && !activity.equals("logged in")) finish();
        //jesli ostatnia aktywnosc nie byla logowaniem, ale byla usunieciem lokalu to zablokuj i wyswietl komunikat
        else if(activity.equals("deleted place") && !activity.equals("logged in")) Toast.makeText(this, "Ten lokal już nie istnieje", Toast.LENGTH_SHORT).show();
    }
}
