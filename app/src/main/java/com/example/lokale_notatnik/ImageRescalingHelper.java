package com.example.lokale_notatnik;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

//klasa do zmiany rozdzielczosci obrazu - w celu zmniejszenia zajmowanego przez niego miejsca
public class ImageRescalingHelper {

    //wykonaj rotacje zdjecia
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        //stworz nowa bitmape na podstawie zrodla i ja zwroc
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    //przeskaluj zdjecie (z pliku)
    public static File changePhotoSizeFile(File file){
        try {
            //użycie ustawien BitmapFactory w celu zmniejszenia rozmiaru
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 8; //opcja odpowiadajaca za zmniejszenie rozmiaru

            //zapisanie pliku jako strumienia wejsciowego
            FileInputStream inputStream = new FileInputStream(file);
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            //rozmiar do jakiego chcemy zmniejszyc obraz
            final int REQUIRED_SIZE=150;

            //znalezienie odpowiedniej wartosci skali (powinna byc potega 2)
            int scale = 1;
            while(o.outWidth / scale >= REQUIRED_SIZE &&
                    o.outHeight / scale >= REQUIRED_SIZE) {
                scale *= 2;
            }

            //stworzenie kolejnego strumienia wejscia (tym razem z nowymi ustawieniami)
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            //stworzenie bitmapy na podstawie strumienia
            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();
            //nadpisanie pliku
            file.createNewFile();
            //stworzenie strumienia wyjsciowego
            FileOutputStream outputStream = new FileOutputStream(file);

            //rotacja obrazu (bitmapy)
            int rotation = getRotationFile(file);
            selectedBitmap = rotateImage(selectedBitmap, rotation);
            //kompresja bitmapy
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100 , outputStream);
            return file; //zwrocenie pliku
        } catch (Exception e) {
            return null;
        }
    }

    //przeskaluj zdjecie (z Uri)
    public static byte[] changePhotoSizeUri(Uri uri, Context context){
        try {
            //użycie ustawien BitmapFactory w celu zmniejszenia rozmiaru
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 8; //opcja odpowiadajaca za zmniejszenie rozmiaru

            //stowrzenie strumiena wejsciowego na podstawie uri oraz kontekstu (aktywnosci)
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            //rozmiar do jakiego chcemy zmniejszyc obraz
            final int REQUIRED_SIZE=150;

            //znalezienie odpowiedniej wartosci skali (powinna byc potega 2)
            int scale = 1;
            while(o.outWidth / scale >= REQUIRED_SIZE &&
                    o.outHeight / scale >= REQUIRED_SIZE) {
                scale *= 2;
            }

            //stworzenie kolejnego strumienia wejscia (tym razem z nowymi ustawieniami)
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = context.getContentResolver().openInputStream(uri);

            //stworzenie bitmapy na podstawie strumienia
            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();
            //stworzenie strumienia wyjsciowego
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            //rotacja obrazu (bitmapy)
            int rotation = getRotationUri(uri, context);
            selectedBitmap = rotateImage(selectedBitmap, rotation);
            //kompresja bitmapy
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100 , out);
            //zapisanie strumienia wyjscia jako tablicy bajtow
            byte[] byteArray = out.toByteArray();
            return byteArray; //zwrocenie tablicy bajtow
        } catch (Exception e) {
            return null;
        }
    }

    //pobierz rotacje ze zdjecia (z Uri)
    public static int getRotationUri(Uri photoUri, Context context)
    {
        int photoRotation = 0;
        boolean hasRotation = false;
        String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };

        //stworz kursor sprawdzajacy czy podane uri ma orientacje i sprawdz czy nie jest pusty
        try {
            Cursor cursor = context.getContentResolver().query(photoUri, projection, null, null, null);
            if (cursor.moveToFirst()) {
                photoRotation = cursor.getInt(0);
                hasRotation = true;
            }
            cursor.close();
        } catch (Exception e) {}

        //jesli niepusty to pobierz atrybut orientation z exifinterface dla tego uri
        if (!hasRotation) {
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(photoUri.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            int exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            //przypisanie wartosci do photoRotation na podstawie wartosci atrybutu
            switch (exifRotation) {
                case ExifInterface.ORIENTATION_ROTATE_90: {
                    photoRotation = 90;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_180: {
                    photoRotation = 180;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_270: {
                    photoRotation = 270;
                    break;
                }
            }
        }
        return photoRotation;
    }

    //pobierz rotacje ze zdjecia (z pliku)
    public static int getRotationFile(File file)
    {
        //plik ma atrybut orientacji, wiec nie trzeba sprawdzac warunku
        int photoRotation = 0;
        ExifInterface exif = null;

        //pobranie atrybutu orientation z exifinterface dla tego uri
        try {
            exif = new ExifInterface(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        //przypisanie wartosci do photoRotation na podstawie wartosci atrybutu
        switch (exifRotation) {
            case ExifInterface.ORIENTATION_ROTATE_90: {
                photoRotation = 90;
                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_180: {
                photoRotation = 180;
                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_270: {
                photoRotation = 270;
                break;
            }
        }
        return photoRotation;
    }
}
