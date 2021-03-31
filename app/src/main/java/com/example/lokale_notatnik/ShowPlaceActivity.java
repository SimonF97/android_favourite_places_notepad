package com.example.lokale_notatnik;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShowPlaceActivity extends AppCompatActivity implements OnMapReadyCallback {
    static boolean active = false;
    public static int idTaken;
    final SQLiteOpenHelper DBHelper = new DatabaseHelper(this);
    public static GoogleMap googleMap;
    public static int SEARCH = 0;

    ImageView gwiazdki;

    TextView napis;
    TextView nazwa;
    TextView adres;
    TextView ocena;
    TextView opinia;
    TextView kategoria;
    TextView lokalizacja;

    ImageView transparentImageView;
    ImageView transparentImageView2;

    WebView webView1;

    Button searchButton;
    RelativeLayout layout;

    private static Context mContext;

    //wez kontekst
    public static Context getContext() {
        return mContext;
    }

    //ustaw kontekst
    public static void setContext(Context mContext2) {
        mContext = mContext2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShowPlaceActivity.setContext(this);
        setContentView(R.layout.activity_show_place);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        napis = findViewById(R.id.textView1);
        gwiazdki = findViewById(R.id.stars);

        nazwa = findViewById(R.id.nazwaTextViewWynik);
        adres = findViewById(R.id.adresTextViewWynik);
        ocena = findViewById(R.id.ocenaTextViewWynik);
        opinia = findViewById(R.id.opiniaTextViewWynik);
        kategoria = findViewById(R.id.kategoriaTextViewWynik);
        lokalizacja = findViewById(R.id.lokalizacjaTextViewWynik);

        searchButton = findViewById(R.id.showGoogleButton);
        layout = findViewById(R.id.webView_layout);

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

    //wez proporcje maksymalnych wymiarow do wymiarow bitmapy by odpowienio powiekszyc obraz
    public float getPhotoRatio(Bitmap bitmap)
    {
        float ratio = 1;
        int rotation =  getWindowManager().getDefaultDisplay().getRotation(); //wez obecna orientacje urzadzenia

        //pobierz wymiary bitmapy
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        //pobierz wymiary ekranu urzadzenia
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        //oblicz proporcje dla szerokosci i wysokosci
        float wRatio = (float)screenWidth/(float)w;
        float hRatio = (float)screenHeight/(float)h;

        //wybierz ktora proporcje zwrocic w zaleznosci od orientacji urzadzenia
        switch (rotation) {
            case Surface.ROTATION_90:
                if(w*hRatio > screenWidth) ratio = wRatio;
                else ratio = hRatio;
                break;
            case Surface.ROTATION_180:
                if(h*wRatio > screenHeight) ratio = hRatio;
                else ratio = wRatio;
                break;
            case Surface.ROTATION_270:
                if(w*hRatio > screenWidth) ratio = wRatio;
                else ratio = hRatio;
                break;
            default:
                if(h*wRatio > screenHeight) ratio = hRatio;
                else ratio = wRatio;
                break;
        }
        return ratio;
    }

    //ustaw wszystkie potrzebne dla aplikacji dane
    public void setUp()
    {
        napis.setText("Pokaż dane lokalu o id " + idTaken);

        String idLokal = Integer.toString(idTaken);
        String nazwaTemp = "";
        String adresTemp = "";
        int ocenaTemp = 0;
        String opiniaTemp = "";
        int kategoriaTemp = 0;
        String lokalizacjaTemp = "";
        String kategoriaNapis = "";
        String ocenaNapis = "";

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        webView1 = findViewById(R.id.webview);

        ArrayList<Bitmap> bmp_images = new ArrayList<>();
        byte[] bArray;

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

            String kategoriaNumer = Integer.toString(kategoriaTemp);

            Cursor categoryResults = DB.query(
                    "KATEGORIA",
                    new String[] {"NAZWA"},
                    "WYBOR = ?", new String[] {kategoriaNumer},
                    null,null,null);
            while(categoryResults.moveToNext()) {
                kategoriaNapis = categoryResults.getString(0);
            }
            categoryResults.close();

            //pobierz odpowiednie dane do Spinnera a takze ustaw odpowiednia ilosc gwiazdek w obrazie
            if(ocenaTemp == 1)
            {
                ocenaNapis = "1 Gwiazdka";
                gwiazdki.setImageResource(R.drawable.jednagwiazdka);
            }
            else if(ocenaTemp == 2)
            {
                ocenaNapis = "2 Gwiazdki";
                gwiazdki.setImageResource(R.drawable.dwiegwiazdki);
            }
            else if(ocenaTemp == 3)
            {
                ocenaNapis = "3 Gwiazdki";
                gwiazdki.setImageResource(R.drawable.trzygwiazdki);
            }
            else if(ocenaTemp == 4)
            {
                ocenaNapis = "4 Gwiazdki";
                gwiazdki.setImageResource(R.drawable.czterygwiazdki);
            }
            else if(ocenaTemp == 5)
            {
                ocenaNapis = "5 Gwiazdek";
                gwiazdki.setImageResource(R.drawable.piecgwiazdek);
            }

            napis.setText("Dane lokalu '" + nazwaTemp + "'");
            nazwa.setText(nazwaTemp);
            adres.setText(adresTemp);
            ocena.setText(ocenaNapis);
            opinia.setText(opiniaTemp);
            kategoria.setText(kategoriaNapis);
            lokalizacja.setText(lokalizacjaTemp);

            //pobierz zdjecia
            Cursor photoResults = DB.query(
                    "ZDJECIE",
                    new String[] {"ZDJECIE_OBIEKT"},
                    "LOKAL_ID = ?", new String[] {idLokal},
                    null,null,null);
            while(photoResults.moveToNext()) {
                bArray = photoResults.getBlob(0);
                Bitmap bm = BitmapFactory.decodeByteArray(bArray, 0 , bArray.length);
                bmp_images.add(bm);
            }
            categoryResults.close();

            //wyswietl zdjecia (w przesuwanej galerii)
            LinearLayout gallery = findViewById(R.id.gallery);
            gallery.removeAllViews(); //zabezpiecznie po to by zdjecia nie byly wyswietlane kilka razy

            LayoutInflater inflater = LayoutInflater.from(this);

            for(int i = 0; i < bmp_images.size(); i++)
            {
                //wyswietl pojedyncze zdjecie
                View view = inflater.inflate(R.layout.photo, gallery, false);

                final ImageView imageView = view.findViewById(R.id.imageView);
                imageView.setImageBitmap(bmp_images.get(i));

                //jesli kliknieto na zdjecie to wyswietl powiekszony obraz (poprzez dialog stworzony z nowego layoutu)
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        View view2 = getLayoutInflater().inflate(R.layout.photo2, null);
                        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                        Bitmap bitmap = drawable.getBitmap();

                        float ratio = getPhotoRatio(bitmap);
                        int pictureWidth = 1;
                        int pictureHeight = 1;

                        //przeskaluj obrazy w zaleznosci od orientacji telefonu (trzeba wziac pod uwage wysokosc paska na gorze telfonu) by nie zostaly uciete
                        int rotation =  getWindowManager().getDefaultDisplay().getRotation();
                        switch (rotation) {
                            case Surface.ROTATION_90:
                                pictureWidth = (int)(bitmap.getWidth()*ratio*0.85);
                                pictureHeight = (int)(bitmap.getHeight()*ratio*0.85);
                                break;
                            case Surface.ROTATION_180:
                                pictureWidth = (int)(bitmap.getWidth()*ratio*0.9);
                                pictureHeight = (int)(bitmap.getHeight()*ratio*0.9);
                                break;
                            case Surface.ROTATION_270:
                                pictureWidth = (int)(bitmap.getWidth()*ratio*0.85);
                                pictureHeight = (int)(bitmap.getHeight()*ratio*0.85);
                                break;
                            default:
                                pictureWidth = (int)(bitmap.getWidth()*ratio*0.9);
                                pictureHeight = (int)(bitmap.getHeight()*ratio*0.9);
                                break;
                        }

                        ImageView imageView2 = view2.findViewById(R.id.imageView2);
                        imageView2.setImageBitmap(bitmap);
                        imageView2.getLayoutParams().height = pictureHeight;
                        imageView2.getLayoutParams().width = pictureWidth;

                        Dialog settingsDialog = new Dialog(ShowPlaceActivity.getContext());
                        settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                        settingsDialog.setContentView(view2);
                        settingsDialog.show();
                    }
                });
                gallery.addView(view);
            }

        } catch (SQLiteException e) {
            Toast.makeText(ShowPlaceActivity.this, "EXCEPTION:SET",
                    Toast.LENGTH_SHORT).show();
        }

        //ustaw map fragment tak by dalo sie go scrollowac
        final ScrollView mainScrollView = findViewById(R.id.scrollView);
        //za fragmentem znajduje sie przezroczysty ImageView, ktory pozwala na blokowanie obslugi zdarzen dla ScrollView
        transparentImageView = findViewById(R.id.transparent_image);

        transparentImageView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //nie pozwol ScrollView na zaklocanie zdarzen dotyku
                        mainScrollView.requestDisallowInterceptTouchEvent(true);
                        //wylacz dotyk dla przezroczystego widoku
                        return false;

                    case MotionEvent.ACTION_UP:
                        //pozwol ScrollView na zaklocanie zdarzen dotyku
                        mainScrollView.requestDisallowInterceptTouchEvent(false);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        mainScrollView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    default:
                        return true;
                }
            }
        });

        //analogiczne dzialanie dla webView
        transparentImageView2 = findViewById(R.id.transparent_image2);

        transparentImageView2.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //nie pozwol ScrollView na zaklocanie zdarzen dotyku
                        mainScrollView.requestDisallowInterceptTouchEvent(true);
                        //wylacz dotyk dla przezroczystego widoku
                        return false;

                    case MotionEvent.ACTION_UP:
                        //pozwol ScrollView na zaklocanie zdarzen dotyku
                        mainScrollView.requestDisallowInterceptTouchEvent(false);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        mainScrollView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    default:
                        return true;
                }
            }
        });

        //pokaz widok wyszukiwania
        showWebSearch();
    }

    //pokaz widok wyszukiwania
    public void showWebSearch()
    {
        int status = WebSearchStatus.getSearchStatus();
        if(status==1) //jesli wyszukiwanie aktywne
        {
            //wyszukaj nazwe oraz adres lokalu w Google
            String query = nazwa.getText().toString() + " " + adres.getText().toString();
            String url = "https://www.google.com/search?q=" + query;
            webView1.getSettings().setJavaScriptEnabled(true);
            webView1.loadUrl(url);
            searchButton.setText("Ukryj wyszukiwanie");
            //wez parametry layoutu
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            //ustaw wysokosc layotu (w pixelach, wiec potrzebna zmienna factor, ktora poda gestosc ekranu)
            float factor = this.getResources().getDisplayMetrics().density;
            params.height = (int)(500 * factor);
            layout.setLayoutParams(params);
        }
        else if(status==0) //jesli wyszukiwanie nieaktywne
        {
            //wyszukaj pusta strone
            webView1.loadUrl("javascript:document.open();document.close();");
            webView1.loadUrl("about:blank");
            searchButton.setText("Wyszukaj w Google");
            //wez parametry layoutu
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            //ustaw wysokosc layotu na 0 (ukryj element)
            params.height = 0;
            layout.setLayoutParams(params);
        }
    }

    //wyszukaj w Google
    public void showGoogle(View view)
    {
        if(SEARCH==0) //jesli wyszukiwanie nie bylo aktywne, ale klikniety zostal przycisk
        {
            //wyszukaj nazwe oraz adres lokalu w Google
            String query = nazwa.getText().toString() + " " + adres.getText().toString();
            String url = "https://www.google.com/search?q=" + query;
            webView1.getSettings().setJavaScriptEnabled(true);
            webView1.loadUrl(url);
            SEARCH=1;
            searchButton.setText("Ukryj wyszukiwanie");
            //wez parametry layoutu
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            //ustaw wysokosc layotu (w pixelach, wiec potrzebna zmienna factor, ktora poda gestosc ekranu)
            float factor = this.getResources().getDisplayMetrics().density;
            params.height = (int)(500 * factor);
            layout.setLayoutParams(params);
            WebSearchStatus.setSearchStatus(1);
        }
        else if(SEARCH==1) //jesli wyszukiwanie bylo aktywne, ale klikniety zostal przycisk
        {
            //wyszukaj pusta strone
            webView1.loadUrl("javascript:document.open();document.close();");
            webView1.loadUrl("about:blank");
            SEARCH=0;
            searchButton.setText("Wyszukaj w Google");
            //wez parametry layoutu
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            //ustaw wysokosc layotu na 0 (ukryj element)
            params.height = 0;
            layout.setLayoutParams(params);
            WebSearchStatus.setSearchStatus(0);
        }
    }

    //metoda statyczna (wywolywana poprze wybor lokalu z listy) do ustawienia id odpowiedniego lokalu
    static void setId(int id)
    {
        idTaken = id;
    }

    //przygotuj mape
    @Override
    public void onMapReady(GoogleMap map)
    {
        googleMap = map;

        String lokalizacjaTemp = "";
        //pobierz lokalizacje z bazy
        SQLiteDatabase DB = DBHelper.getWritableDatabase();
        Cursor localResults = DB.query(
                "LOKAL",
                new String[] {"LOKALIZACJA_MIEJSCE"},
                "_id = ?", new String[] {Integer.toString(idTaken)},
                null,null,null);
        while(localResults.moveToNext()) {
            lokalizacjaTemp = localResults.getString(0);
        }
        localResults.close();

        //ustaw mape oraz znacznik w odpowiednim punkcie
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocationName(lokalizacjaTemp, 1);
            if(addresses.size() > 0) {
                double latitude = addresses.get(0).getLatitude();
                double longitude = addresses.get(0).getLongitude();

                LatLng location = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions().position(location).title("Marker"));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
