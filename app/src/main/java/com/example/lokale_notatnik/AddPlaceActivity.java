package com.example.lokale_notatnik;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

public class AddPlaceActivity extends AppCompatActivity implements LocationListener {
    static boolean active = false;

    String addressTaken = "";
    String currentLocation;
    String addressWritten = "";
    String name = "";
    String description = "";
    int userId = User.getId(); //pobierz id obecnie zalogowanego użytkownika
    int mark = 0;
    int category = 0;
    int lokal_id = 0;
    int lokal_id_ctrl = 0;
    int leave = 0;

    TextView currentLocalizationTV;
    EditText nazwaET;
    EditText adresET;
    EditText opisET;
    Spinner spinnerMark;
    Spinner spinnerCategory;

    Menu optionsMenu;

    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    public static Location locationGlobal = new Location(LocationManager.GPS_PROVIDER);
    public static final int WRITE_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentLocalizationTV = findViewById(R.id.currentLocalization);

        nazwaET = findViewById(R.id.nazwaEditText);
        adresET = findViewById(R.id.adresEditText);
        opisET = findViewById(R.id.opisEditText);

        spinnerMark = findViewById(R.id.SpinnerMark);
        spinnerCategory = findViewById(R.id.SpinnerCategory);
    }

    //stworz menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        optionsMenu = menu;
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
                leaveActivityThroughMenu(MainActivity.class);
                return true;
            case R.id.lista:
                leaveActivityThroughMenu(ListActivity.class);
                return true;
            case R.id.dodawanie:
                return true;
            case R.id.wylogowanie:
                //usun zdjecia jakie w obecnej sesji aktywnosci dodal uzytkownik
                if(lokal_id != 0)
                {
                    try {
                        SQLiteDatabase DB = DBHelper.getWritableDatabase();
                        DB.delete("ZDJECIE", "LOKAL_ID = ?", new String[] {Integer.toString(lokal_id)});
                    } catch (SQLiteException e) {
                        Toast.makeText(AddPlaceActivity.this, "EXCEPTION:SET",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                finish();
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

    //przejdz bezpiecznie do innej czesci aplikacji (uzytkownik musi zatwierdzic ze chce usunac zdjecia, ktore dodal w tej sesji aktywnosci)
    public void leaveActivityThroughMenu(Class c)
    {
        Intent intent;
        if(lokal_id != 0) //jesli zdjecie zostalo dodane
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle("Potwierdzenie");
            builder.setMessage("Czy na pewno chcesz opuścić panel? Twoje zdjęcia nie zostaną zapisane.");
            builder.setPositiveButton("Tak",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                SQLiteDatabase DB = DBHelper.getWritableDatabase();
                                DB.delete("ZDJECIE", "LOKAL_ID = ?", new String[] {Integer.toString(lokal_id)});
                            } catch (SQLiteException e) {
                                Toast.makeText(AddPlaceActivity.this, "EXCEPTION:SET",
                                        Toast.LENGTH_SHORT).show();
                            }
                            leave = 1;
                            onOptionsItemSelected(optionsMenu.findItem(R.id.profil));
                        }
                    });
            builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            if(leave == 1) dialog.dismiss(); //zamknij okno z pytaniem
        }
        else leave = 1;
        if(leave == 1)
        {
            leave = 0;
            lokal_id = 0;
            intent = new Intent(this, c);
            intent.putExtra("activity","ok");
            startActivity(intent);
        }
    }

    //weź obecną lokalizację użytkownika
    public void getLocalization(View view)
    {
        final Location locationTemp = locationGlobal;
        //popros o zezwolenie na uzywanie lokalizacji
        if (android.os.Build.VERSION.SDK_INT >= 23)
        {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            requestPermissions(permissions, WRITE_REQUEST_CODE);

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(AddPlaceActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(AddPlaceActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) AddPlaceActivity.this);
        }

        //zamiana wspolrzednych na adres
        Geocoder gcd = new Geocoder(getBaseContext(),
                Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(locationTemp.getLatitude(),
                    locationTemp.getLongitude(), 1);
            if (addresses.size() > 0) {
                String locality = addresses.get(0).getLocality();
                String subLocality = addresses.get(0).getSubLocality();
                String street = addresses.get(0).getThoroughfare();
                String streetNumber = addresses.get(0).getSubThoroughfare();
                if (subLocality != null)
                {
                    if(street != null)
                    {
                        if(streetNumber != null)
                        {
                            currentLocation = street + " " + streetNumber + ", " + locality + ", " + subLocality;
                        }
                        else currentLocation = street + ", " + locality + ", " + subLocality;
                    }
                    else currentLocation = locality + ", " + subLocality;
                }
                else
                {
                    if(street != null)
                    {
                        if(streetNumber != null)
                        {
                            currentLocation = street + " " + streetNumber + ", " + locality;
                        }
                        else currentLocation = street + ", " + locality;
                    }
                    else currentLocation = locality;
                }
                if (TextUtils.isEmpty(currentLocation)) {
                    addressTaken = "";
                } else {
                    addressTaken = currentLocation;
                    currentLocalizationTV.setText(addressTaken);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(AddPlaceActivity.this,"EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //sprawdz czy EditText jest pusty
    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0)
            return false;
        return true;
    }

    //dodaj lokal
    public void addPlace(View view)
    {
        name = nazwaET.getText().toString();
        description = opisET.getText().toString();
        addressWritten = adresET.getText().toString();
        addressTaken = currentLocalizationTV.getText().toString();
        if(isEmpty(nazwaET)) Toast.makeText(this, "Podaj nazwę", Toast.LENGTH_SHORT).show();
        else
        {
            if (spinnerMark.getSelectedItem().toString().equals("1 Gwiazdka")) {
                mark = 1;
            }
            else if (spinnerMark.getSelectedItem().toString().equals("2 Gwiazdki")) {
                mark = 2;
            }
            else if (spinnerMark.getSelectedItem().toString().equals("3 Gwiazdki")) {
                mark = 3;
            }
            else if (spinnerMark.getSelectedItem().toString().equals("4 Gwiazdki")) {
                mark = 4;
            }
            else if (spinnerMark.getSelectedItem().toString().equals("5 Gwiazdek")) {
                mark = 5;
            }
            if (spinnerCategory.getSelectedItem().toString().equals("Restauracja")) {
                category = 1;
            }
            else if (spinnerCategory.getSelectedItem().toString().equals("Pub")) {
                category = 2;
            }
            else if (spinnerCategory.getSelectedItem().toString().equals("Przychodnia")) {
                category = 3;
            }
            else if (spinnerCategory.getSelectedItem().toString().equals("Warsztat")) {
                category = 4;
            }
            else if (spinnerCategory.getSelectedItem().toString().equals("Sklep")) {
                category = 5;
            }

            if(name.equals("")) Toast.makeText(this, "Podaj nazwę", Toast.LENGTH_SHORT).show();
            else
            {
                try {
                    //dodaj do bazy
                    SQLiteDatabase DB = DBHelper.getWritableDatabase();
                    ContentValues localeData = new ContentValues();
                    localeData.clear();
                    localeData.put("KATEGORIA", category);
                    localeData.put("NAZWA", name);
                    localeData.put("OCENA", mark);
                    localeData.put("OPIS", description);
                    localeData.put("LOKALIZACJA_NAPIS", addressWritten);
                    localeData.put("LOKALIZACJA_MIEJSCE", addressTaken);
                    localeData.put("UZYTKOWNIK_ID", userId);
                    DB.insert("LOKAL", null, localeData); //-1 error
                    Toast.makeText(this, "Dodano lokal", Toast.LENGTH_SHORT).show();
                } catch (SQLiteException e) {
                    Toast.makeText(AddPlaceActivity.this, "EXCEPTION:SET",
                            Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("activity","ok");
                startActivity(intent);
            }
        }
    }

    //dodaj zdjęcie
    public void addPhoto(View view)
    {
        final CharSequence[] options = {"Zrób zdjęcie", "Wybierz z galerii", "Anuluj"};

        //popros o pozwolenie na dostep do pamieci urzadzenia
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, WRITE_REQUEST_CODE);

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(AddPlaceActivity.this);
        builder.setTitle("Wybierz sposób dodania zdjęcia");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Zrób zdjęcie")) {
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    File file = new File(Environment.getExternalStorageDirectory(), "MyPhoto.png");
                    Uri uri = FileProvider.getUriForFile(AddPlaceActivity.this, getApplicationContext().getPackageName() + ".provider", file);
                    takePicture.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
                    startActivityForResult(takePicture, 0);

                } else if (options[item].equals("Wybierz z galerii")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, 1);

                } else if (options[item].equals("Anuluj")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 0: //jesli uzytkownik wybral zrobienie zdjecia
                if (resultCode == RESULT_OK) {
                    try {
                        //plik zawierajacy obraz z aparatu
                        File file = new File(Environment.getExternalStorageDirectory(), "MyPhoto.png");
                        //obrobka pliku w celu zmniejszenia jego rozmiaru mozliwie bez straty jakosci (zaoszczedzenie czasu ladowania i zapisu zdjecia)
                        file = ImageRescalingHelper.changePhotoSizeFile(file);
                        //uri zawierajace obraz z aparatu
                        Uri takenPhoto = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), takenPhoto);

                        //skompresowanie bitmapy i zapisanie jej jako tablice bajtow (w celu zapisania jej do bazy jako blob)
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                        byte[] bArray = bos.toByteArray();

                        //sprawdz czy jakis lokal znajduje się w bazie
                        SQLiteDatabase DB = DBHelper.getWritableDatabase();
                        lokal_id_ctrl = 0; //ustaw jako 0 by mozliwe bylo sprawdzanie warunku
                        Cursor localResults = DB.query(
                                "LOKAL",
                                null,
                                null, null,
                                null,null,null);
                        while(localResults.moveToNext())
                        {
                            lokal_id_ctrl = 1;
                        }
                        localResults.close();
                        if(lokal_id_ctrl == 1) //jesli tak to wylicz numer sekwencyjny
                        {
                            Cursor idResults = DB.rawQuery("select seq from sqlite_sequence where name=\"LOKAL\"", null);
                            while(idResults.moveToNext())
                            {
                                lokal_id = idResults.getInt(0);
                            }
                            lokal_id++;
                        }
                        else lokal_id = 1; //w innym wypadku ustaw id lokalu jako 1
                        //jest to potrzebne gdyz jesli tabela jest pusta to select seq będzie inkrementowal sie przy kazdym wywolaniu o 1 - jesli nie jest to bedzie dzialac prawidlowo
                        ContentValues photoData = new ContentValues();
                        photoData.clear();
                        photoData.put("ZDJECIE_OBIEKT", bArray);
                        photoData.put("LOKAL_ID", lokal_id);
                        DB.insert("ZDJECIE", null, photoData);
                        Toast.makeText(this, "Dodano zdjęcie", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case 1: //jesli uzytkownik wybral dodanie z poziomu galerii
                if (resultCode == RESULT_OK) {
                    //pobranie zdjecia z galerii jako uri
                    Uri selectedImage = imageReturnedIntent.getData();
                    //obrobka zdjecia w celu zmniejszenia jego rozmiaru mozliwie bez straty jakosci (zaoszczedzenie czasu ladowania i zapisu zdjecia)
                    //zapisanie obrazu jako tablicy bajtow
                    byte[] bArray = ImageRescalingHelper.changePhotoSizeUri(selectedImage, this);

                    //sprawdz czy jakis lokal znajduje się w bazie
                    SQLiteDatabase DB = DBHelper.getWritableDatabase();
                    lokal_id_ctrl = 0; //ustaw jako 0 by mozliwe bylo sprawdzanie warunku
                    Cursor localResults = DB.query(
                            "LOKAL",
                            null,
                            null, null,
                            null,null,null);
                    while(localResults.moveToNext())
                    {
                        lokal_id_ctrl = 1;
                    }
                    localResults.close();
                    if(lokal_id_ctrl == 1) //jesli tak to wylicz numer sekwencyjny
                    {
                        Cursor idResults = DB.rawQuery("select seq from sqlite_sequence where name=\"LOKAL\"", null);
                        while(idResults.moveToNext())
                        {
                            lokal_id = idResults.getInt(0);
                        }
                        lokal_id += 1;
                    }
                    else lokal_id = 1; //w innym wypadku ustaw id lokalu jako 1
                    //jest to potrzebne gdyz jesli tabela jest pusta to select seq będzie inkrementowal sie przy kazdym wywolaniu o 1 - jesli nie jest to bedzie dzialac prawidlowo
                    ContentValues photoData = new ContentValues();
                    photoData.clear();
                    photoData.put("ZDJECIE_OBIEKT", bArray);
                    photoData.put("LOKAL_ID", lokal_id);
                    DB.insert("ZDJECIE", null, photoData);
                    Toast.makeText(this, "Dodano zdjęcie", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        locationGlobal = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {}

    //usun zdjecia jesli lokal nie jest tworzony i wroc
    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        String activity = intent.getStringExtra("activity");
        if(!activity.equals("deleted place")) //jesli poprzednia aktywnosc nie byla zakonczona usunieciem lokalu
        {
            if(lokal_id != 0)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true);
                builder.setTitle("Potwierdzenie");
                builder.setMessage("Czy na pewno chcesz opuścić panel? Twoje zdjęcia nie zostaną zapisane.");
                builder.setPositiveButton("Tak",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    SQLiteDatabase DB = DBHelper.getWritableDatabase();
                                    DB.delete("ZDJECIE", "LOKAL_ID = ?", new String[] {Integer.toString(lokal_id)});
                                } catch (SQLiteException e) {
                                    Toast.makeText(AddPlaceActivity.this, "EXCEPTION:SET",
                                            Toast.LENGTH_SHORT).show();
                                }
                                finish();
                            }
                        });
                builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            else finish();
        }
    }
}
