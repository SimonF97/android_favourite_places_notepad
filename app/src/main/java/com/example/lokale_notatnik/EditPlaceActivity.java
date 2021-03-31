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
import android.graphics.BitmapFactory;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

public class EditPlaceActivity extends AppCompatActivity implements LocationListener{
    static boolean active = false;

    static int idTaken;
    String currentLocation;
    String addressTaken = "";
    String addressWritten = "";
    String name = "";
    String description = "";
    int mark = 0;
    int category = 0;

    TextView napis;
    TextView currentLocalizationTV;
    EditText nazwaET;
    EditText adresET;
    EditText opiniaET;
    Spinner spinnerMark;
    Spinner spinnerCategory;

    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    public static Location locationGlobal = new Location(LocationManager.GPS_PROVIDER);
    public static final int WRITE_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_place);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        napis = findViewById(R.id.textView1);

        currentLocalizationTV = findViewById(R.id.lokalizacjaTextViewWynik);

        nazwaET = findViewById(R.id.nazwaEditText);
        adresET = findViewById(R.id.adresEditText);
        opiniaET = findViewById(R.id.opiniaEditText);

        spinnerMark = findViewById(R.id.SpinnerMark);
        spinnerCategory = findViewById(R.id.SpinnerCategory);

        //ustaw wartosci wszystkich elementow aktywnosci
        setUp();
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

    //jesli aktywnosc zrestartowana (na przyklad poprzez powrot do niej) to ponownie rozstaw elementy - jesli lokal o takim id istnieje
    @Override
    public void onRestart() {
        super.onRestart();
        if(idTaken != 0) setUp();
    }

    //metoda statyczna (wywolywana poprze wybor lokalu z listy) do ustawienia id odpowiedniego lokalu
    static void setId(int id) {idTaken = id;}

    //ustaw wszystkie potrzebne dla aplikacji dane
    public void setUp()
    {
        napis.setText("Edytuj dane lokalu o id " + idTaken);

        String idLokal = Integer.toString(idTaken);
        String nazwaTemp = "";
        String adresTemp = "";
        int ocenaTemp = 0;
        String opiniaTemp = "";
        int kategoriaTemp = 0;
        String lokalizacjaTemp = "";

        ArrayList<Bitmap> bmp_images = new ArrayList<>();
        byte[] bArray;
        ArrayList<Integer> photoIds = new ArrayList<>();

        //wyswietl dane slowne
        try {
            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            Cursor localResults = DB.query(
                    "LOKAL",
                    null,
                    "_id = ?", new String[] {idLokal},
                    null,null,null);
            while(localResults.moveToNext()) {
                kategoriaTemp = localResults.getInt(1);
                nazwaTemp = localResults.getString(2);
                ocenaTemp = localResults.getInt(3);
                opiniaTemp = localResults.getString(4);
                adresTemp = localResults.getString(5);
                lokalizacjaTemp = localResults.getString(6);
            }
            localResults.close();

            napis.setText("Edycja danych lokalu '" + nazwaTemp + "'");
            nazwaET.setText(nazwaTemp);
            adresET.setText(adresTemp);
            spinnerMark.setSelection(ocenaTemp-1);
            opiniaET.setText(opiniaTemp);
            spinnerCategory.setSelection(kategoriaTemp-1);
            currentLocalizationTV.setText(lokalizacjaTemp);

            //pobierz zdjecia
            Cursor photoResults = DB.query(
                    "ZDJECIE",
                    new String[] {"_id", "ZDJECIE_OBIEKT"},
                    "LOKAL_ID = ?", new String[] {idLokal},
                    null,null,null);
            while(photoResults.moveToNext()) {
                bArray = photoResults.getBlob(1);
                Bitmap bm = BitmapFactory.decodeByteArray(bArray, 0 , bArray.length);
                bmp_images.add(bm);
                photoIds.add(photoResults.getInt(0));
            }

            loadGallery();
        } catch (SQLiteException e) {
            Toast.makeText(EditPlaceActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //zaladowanie galerii
    public void loadGallery()
    {
        try{
            String idLokal = Integer.toString(idTaken);
            ArrayList<Bitmap> bmp_images = new ArrayList<>();
            byte[] bArray;
            ArrayList<Integer> photoIds = new ArrayList<>();
            //pobierz zdjecia
            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            Cursor photoResults = DB.query(
                    "ZDJECIE",
                    new String[] {"_id", "ZDJECIE_OBIEKT"},
                    "LOKAL_ID = ?", new String[] {idLokal},
                    null,null,null);
            while(photoResults.moveToNext()) {
                bArray = photoResults.getBlob(1);
                Bitmap bm = BitmapFactory.decodeByteArray(bArray, 0 , bArray.length);
                bmp_images.add(bm);
                photoIds.add(photoResults.getInt(0));
            }

            //wyswietl zdjecia (w przesuwanej galerii)
            LinearLayout gallery = findViewById(R.id.gallery);

            LayoutInflater inflater = LayoutInflater.from(this);

            gallery.removeAllViews(); //zabezpiecznie po to by zdjecia nie byly wyswietlane kilka razy

            for(int i = 0; i < bmp_images.size(); i++)
            {
                //wyswietl pojedyncze zdjecie
                View view = inflater.inflate(R.layout.photo, gallery, false);

                final ImageView imageView = view.findViewById(R.id.imageView);
                imageView.setImageBitmap(bmp_images.get(i));

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final String photoNumber = Integer.toString(i+1);
                final int photoId = photoIds.get(i);

                //jesli kliknieto na zdjecie to zapytaj czy je usunac
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        builder.setCancelable(true);
                        builder.setTitle("Potwierdzenie");
                        builder.setMessage("Czy chcesz usunąć zdjecie "+photoNumber+"?");
                        builder.setPositiveButton("Tak",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deletePhotoHelp(photoId); //usun zdjecie
                                        loadGallery(); //przeladuj galerie
                                    }
                                });
                        builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
                gallery.addView(view);
            }
        } catch (SQLiteException e) {
            Toast.makeText(EditPlaceActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //metoda pomocnicza dla usuwania zdjecia
    public void deletePhotoHelp(int idPhoto)
    {
        try {
            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            DB.delete("ZDJECIE", "_id = ?", new String[] {Integer.toString(idPhoto)});
        } catch (SQLiteException e) {
            Toast.makeText(EditPlaceActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //sprawdz czy EditText jest pusty
    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0)
            return false;
        return true;
    }

    //edytuj lokal
    public void editPlace(View view)
    {
        name = nazwaET.getText().toString();
        description = opiniaET.getText().toString();
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
                    //uaktualnij dane w bazie
                    SQLiteDatabase DB = DBHelper.getWritableDatabase();
                    ContentValues localeData = new ContentValues();
                    localeData.clear();
                    localeData.put("KATEGORIA", category);
                    localeData.put("NAZWA", name);
                    localeData.put("OCENA", mark);
                    localeData.put("OPIS", description);
                    localeData.put("LOKALIZACJA_NAPIS", addressWritten);
                    localeData.put("LOKALIZACJA_MIEJSCE", addressTaken);
                    DB.update("LOKAL", localeData,"_id = ?", new String[] {Integer.toString(idTaken)});
                    Toast.makeText(this, "Edytowano lokal", Toast.LENGTH_SHORT).show();
                } catch (SQLiteException e) {
                    Toast.makeText(EditPlaceActivity.this, "EXCEPTION:SET",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //zapytaj czy usunac lokal
    public void deletePlace(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("Potwierdzenie");
        builder.setMessage("Czy na pewno chcesz usunąć lokal?");
        builder.setPositiveButton("Tak",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePlaceHelp();
                    }
                });
        builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //metoda pomocnicza dla usuwania lokalu
    public void deletePlaceHelp()
    {
        try {
            SQLiteDatabase DB = DBHelper.getWritableDatabase();
            DB.delete("LOKAL", "_id = ?", new String[] {Integer.toString(idTaken)});
        } catch (SQLiteException e) {
            Toast.makeText(EditPlaceActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }
        setId(0);
        Intent intent = new Intent(this, ListActivity.class);
        //przekaz z intenetem wiadomosc o tym ze usunieto lokal - dzieki temu pozstale aktywnosci nie beda mogly przejsc przez te aktywnosc poprzez przycisk powrotu
        intent.putExtra("activity","deleted place");
        startActivity(intent);
        finish();
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
            if (ActivityCompat.checkSelfPermission(EditPlaceActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(EditPlaceActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) EditPlaceActivity.this);
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
            Toast.makeText(EditPlaceActivity.this,"EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
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
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(EditPlaceActivity.this);
        builder.setTitle("Wybierz sposób dodania zdjęcia");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Zrób zdjęcie")) {
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    File file = new File(Environment.getExternalStorageDirectory(), "MyPhoto.png");
                    Uri uri = FileProvider.getUriForFile(EditPlaceActivity.this, getApplicationContext().getPackageName() + ".provider", file);
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
                    try{
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

                        //zapisanie zdjecia do bazy
                        SQLiteDatabase DB = DBHelper.getWritableDatabase();
                        ContentValues photoData = new ContentValues();
                        photoData.clear();
                        photoData.put("ZDJECIE_OBIEKT", bArray);
                        photoData.put("LOKAL_ID", idTaken);
                        DB.insert("ZDJECIE", null, photoData); //-1 error
                        Toast.makeText(this, "Dodano zdjęcie", Toast.LENGTH_SHORT).show();
                        loadGallery();
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

                    //zapisz zdjecie do bazy
                    SQLiteDatabase DB = DBHelper.getWritableDatabase();
                    ContentValues photoData = new ContentValues();
                    photoData.clear();
                    photoData.put("ZDJECIE_OBIEKT", bArray);
                    photoData.put("LOKAL_ID", idTaken);
                    DB.insert("ZDJECIE", null, photoData); //-1 error
                    Toast.makeText(this, "Dodano zdjęcie", Toast.LENGTH_SHORT).show();
                    loadGallery();
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
