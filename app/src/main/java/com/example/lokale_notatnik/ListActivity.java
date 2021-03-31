package com.example.lokale_notatnik;

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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class ListActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    static boolean active = false;
    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    String id_user = Integer.toString(User.getId());
    int selectedCategory = 0;
    String selectedCategoryString = "";
    String sortOption = "";
    int sortVector = 0;

    Spinner spinnerSort;
    Spinner spinnerCat2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        spinnerSort = findViewById(R.id.SpinnerSorting);
        spinnerCat2 = findViewById(R.id.SpinnerCategory2);

        spinnerSort.setOnItemSelectedListener(this);
        spinnerCat2.setOnItemSelectedListener(this);

        //pobierz dane lokali z bazy
        getItemsFromDatabase();
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

    //jesli aktywnosc zrestartowana (na przyklad poprzez powrot do niej) to pobierz aktualne dane z bazy i odswiez liste
    @Override
    public void onRestart() {
        super.onRestart();
        getItemsFromDatabase();
        refreshList();
    }

    //pobierz dane lokali z bazy
    public void getItemsFromDatabase()
    {
        try {
            ArrayList<String> list = new ArrayList<>();
            ArrayList<Integer> listId = new ArrayList<>();

            //pobranie lokali zalogowanego uzytkownika
            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            Cursor localResults = DB.query(
                    "LOKAL",
                    new String[] {"_id", "NAZWA"},
                    "UZYTKOWNIK_ID = ?", new String[] {id_user},
                    null,null,null);
            while(localResults.moveToNext())
            {
                listId.add(localResults.getInt(0));
                list.add(localResults.getString(1));
            }
            localResults.close();

            //inicjalizacja adaptera listy
            LocalAdapter adapter = new LocalAdapter(listId, list, this);

            //przypisanie adaptera do listy
            ListView lView = findViewById(R.id.listview);
            lView.setAdapter(adapter);
        } catch (SQLiteException e) {
            Toast.makeText(ListActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //reaguj na zmiane wartosci spinnera
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        switch (parent.getId()) {
            case R.id.SpinnerSorting:
                if (spinnerSort.getSelectedItem().toString().equals("Czas dodania (od najstarszego)"))
                {
                    sortOption = "_id";
                    sortVector = 0;
                }
                else if (spinnerSort.getSelectedItem().toString().equals("Czas dodania (od najnowszego)"))
                {
                    sortOption = "_id";
                    sortVector = 1;
                }
                else if (spinnerSort.getSelectedItem().toString().equals("Alfabetycznie (A-Z)"))
                {
                    sortOption = "NAZWA";
                    sortVector = 0;
                }
                else if (spinnerSort.getSelectedItem().toString().equals("Alfabetycznie (Z-A)"))
                {
                    sortOption = "NAZWA";
                    sortVector = 1;
                }
                else if (spinnerSort.getSelectedItem().toString().equals("Oceny (rosnąco)"))
                {
                    sortOption = "OCENA";
                    sortVector = 0;
                }
                else if (spinnerSort.getSelectedItem().toString().equals("Oceny (malejąco)"))
                {
                    sortOption = "OCENA";
                    sortVector = 1;
                }
                refreshList();
                break;
            case R.id.SpinnerCategory2:
                if (spinnerCat2.getSelectedItem().toString().equals("Wszystkie kategorie")) selectedCategory = 0;
                else if (spinnerCat2.getSelectedItem().toString().equals("Restauracja")) selectedCategory = 1;
                else if (spinnerCat2.getSelectedItem().toString().equals("Pub")) selectedCategory = 2;
                else if (spinnerCat2.getSelectedItem().toString().equals("Przychodnia")) selectedCategory = 3;
                else if (spinnerCat2.getSelectedItem().toString().equals("Warsztat")) selectedCategory = 4;
                else if (spinnerCat2.getSelectedItem().toString().equals("Sklep")) selectedCategory = 5;
                selectedCategoryString = Integer.toString(selectedCategory);
                refreshList();
                break;
        }
    }

    //jesli nic sie nie dzieje ze spinnerem
    public void onNothingSelected(AdapterView<?> parent) {}

    //odswiez liste (uwzglednij wartosci spinnerow)
    public void refreshList()
    {
        try {
            ArrayList<String> list = new ArrayList<>();
            ArrayList<Integer> listId = new ArrayList<>();

            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            Cursor localResults = DB.query(
                    "LOKAL",
                    new String[] {"_id", "NAZWA"},
                    "UZYTKOWNIK_ID = ?", new String[] {id_user},
                    null,null,null);

            if(sortOption.equals("") && selectedCategory == 0) //jesli kategoria niewybrana i sortowanie domyslne
            {
                localResults = DB.query(
                        "LOKAL",
                        new String[] {"_id", "NAZWA"},
                        "UZYTKOWNIK_ID = ?", new String[] {id_user},
                        null,null,null);
            }
            else if(sortOption.equals("") && selectedCategory != 0) //jesli kategoria wybrana i sortowanie domyslne
            {
                localResults = DB.query(
                        "LOKAL",
                        new String[] {"_id", "NAZWA"},
                        "UZYTKOWNIK_ID = ? and KATEGORIA = ?", new String[] {id_user, selectedCategoryString},
                        null,null,null);
            }
            else if(!sortOption.equals("") && selectedCategory == 0 && sortVector == 0) //jesli kategoria niewybrana i sortowanie rosnace
            {
                localResults = DB.query(
                        "LOKAL",
                        new String[] {"_id", "NAZWA"},
                        "UZYTKOWNIK_ID = ?", new String[] {id_user},
                        null,null,sortOption);
            }
            else if(!sortOption.equals("") && selectedCategory == 0 && sortVector == 1) //jesli kategoria niewybrana i sortowanie malejace
            {
                localResults = DB.query(
                        "LOKAL",
                        new String[] {"_id", "NAZWA"},
                        "UZYTKOWNIK_ID = ?", new String[] {id_user},
                        null,null,sortOption+" DESC");
            }
            else if(!sortOption.equals("") && selectedCategory != 0 && sortVector == 0) //jesli kategoria wybrana i sortowanie rosnace
            {
                localResults = DB.query(
                        "LOKAL",
                        new String[] {"_id", "NAZWA"},
                        "UZYTKOWNIK_ID = ? and KATEGORIA = ?", new String[] {id_user, selectedCategoryString},
                        null,null,sortOption);
            }
            else if(!sortOption.equals("") && selectedCategory != 0 && sortVector == 1) //jesli kategoria wybrana i sortowanie malejace
            {
                localResults = DB.query(
                        "LOKAL",
                        new String[] {"_id", "NAZWA"},
                        "UZYTKOWNIK_ID = ? and KATEGORIA = ?", new String[] {id_user, selectedCategoryString},
                        null,null,sortOption+" DESC");
            }

            //dodanie do listy elementow spelniajacyh powyzsze warunki
            while(localResults.moveToNext())
            {
                listId.add(localResults.getInt(0));
                list.add(localResults.getString(1));
            }
            localResults.close();

            //inicjalizacja adaptera listy
            LocalAdapter adapter = new LocalAdapter(listId, list, this);

            //przypisanie adaptera do listy
            ListView lView = findViewById(R.id.listview);
            lView.setAdapter(adapter);
        } catch (SQLiteException e) {
            Toast.makeText(ListActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
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
