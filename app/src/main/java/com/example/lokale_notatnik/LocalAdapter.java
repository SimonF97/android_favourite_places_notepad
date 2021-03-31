package com.example.lokale_notatnik;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class LocalAdapter extends BaseAdapter implements ListAdapter {
    private ArrayList<Integer> listId = new ArrayList<>();
    private ArrayList<String> list = new ArrayList<>();
    private Context context;

    //konstruktor adaptera
    public LocalAdapter(ArrayList<Integer> listId, ArrayList<String> list, Context context) {
        this.listId = listId;
        this.list = list;
        this.context = context;
    }

    //wez dlugosc listy
    @Override
    public int getCount() {
        return list.size();
    }

    //wez element listy
    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    //zwroc 0, gdyz id przedmiotow nie znajduje sie w adapterze (sa ustawione niezaleznie w bazie)
    @Override
    public long getItemId(int pos) {
        return 0;
    }

    //stworz widok
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.single_local, null);
        }

        //ustawienie tekstu w TextView
        TextView listItemText = view.findViewById(R.id.list_item_string);
        listItemText.setText(list.get(position));

        //oprogramowanie dzialania przyciskow
        Button showBtn = view.findViewById(R.id.show_btn);
        Button editBtn = view.findViewById(R.id.edit_btn);

        showBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //przejdz do panelu wyswietlania danych lokalu
                ShowPlaceActivity.setId(listId.get(position));
                Intent intent = new Intent(context, ShowPlaceActivity.class);
                intent.putExtra("activity","ok");
                context.startActivity(intent);
            }
        });
        editBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //przejdz do panelu edycji danych lokalu
                EditPlaceActivity.setId(listId.get(position));
                Intent intent = new Intent(context, EditPlaceActivity.class);
                intent.putExtra("activity","ok");
                context.startActivity(intent);
            }
        });
        return view;
    }
}
