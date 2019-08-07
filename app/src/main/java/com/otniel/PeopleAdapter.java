package com.otniel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by amxha on 12/11/2016.
 */

public class PeopleAdapter extends ArrayAdapter<Person> {

    private final Context context;
    private final ArrayList<Person> values;

    public PeopleAdapter(Context context, ArrayList<Person> values) {
        super(context, R.layout.person_layout, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View contactView = convertView;
        // reuse views
        if (contactView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            contactView = inflater.inflate(R.layout.person_layout, null);
            // configure view holder
            PeopleAdapter.ViewHolder viewHolder = new PeopleAdapter.ViewHolder();
            viewHolder.name = contactView.findViewById(R.id.person_name);
            viewHolder.phone = contactView.findViewById(R.id.person_phone);
            viewHolder.image = contactView.findViewById(R.id.person_img);
            viewHolder.options = contactView.findViewById(R.id.person_menu);
            contactView.setTag(viewHolder);
        }

        final Person person = values.get(position);

        PeopleAdapter.ViewHolder holder = (PeopleAdapter.ViewHolder) contactView.getTag();

        holder.image.setImageResource(R.drawable.contact);
        Picasso.get().load(R.drawable.contact).transform(new CircleTransform()).into(holder.image);
        if (person.imageState != ImageState.NO_IMG)
            person.loadImage(holder.image, true);

        holder.image.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            LayoutInflater inflater = ((MainActivity) context).getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.go_pro_dialog_layout, null);
            ((TextView) dialogView.findViewById(R.id.person_big_name)).setText(person.getName());
            ((TextView) dialogView.findViewById(R.id.person_big_phone)).setText(person.getPhonenumber());
            if (person.getPicPath() != null && person.imageState != ImageState.NO_IMG)
                person.loadImage(dialogView.findViewById(R.id.person_big_img), false);
            builder.setView(dialogView);

            builder.create().show();
        });

        if (person.getColor() == Person.PersonColor.YELLOW)
            holder.name.setBackgroundColor(Color.YELLOW);
        else
            holder.name.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));

        if (!MainActivity.getQuery().equals("") &&
                person.getName().contains(MainActivity.getQuery())) {
            String query = MainActivity.getQuery();
            String name = person.getName();
            String htmlText = name.substring(0, name.indexOf(query)) + "<font color=#FF4081>" + query + "</font>" + name.substring(name.indexOf(query) + query.length());

            holder.name.setText(Html.fromHtml(htmlText));
        } else
            holder.name.setText(person.getName());
        if (!MainActivity.getQuery().equals("") &&
                person.getPhonenumber().replace("-", "").contains(MainActivity.getQuery())) {
            String query = MainActivity.getQuery();
            String phone = person.getPhonenumber();
            String phoneNo = phone.replace("-", "");
            String htmlText = "";

            if (phoneNo.indexOf(query) < 4 && phoneNo.indexOf(query) + query.length() >= 4) {
                htmlText = phone.substring(0, phoneNo.indexOf(query)) + "<font color=#FF4081>" + phone.substring(phoneNo.indexOf(query), phoneNo.indexOf(query) + query.length() + 1) + "</font>" + phone.substring(phoneNo.indexOf(query) + query.length() + 1);
            } else {
                htmlText = phone.substring(0, phoneNo.indexOf(query)) + "<font color=#FF4081>" + query + "</font>" + phone.substring(phoneNo.indexOf(query) + query.length());
            }

            holder.phone.setText(Html.fromHtml(htmlText));
        } else
            holder.phone.setText(person.getPhonenumber());

        holder.options.setOnClickListener(v -> showPopup(person, v));

        return contactView;
    }

    private void showPopup(Person person, View v) {
        PopupMenu popup = new PopupMenu(context, v);
        popup.setOnMenuItemClickListener(person);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());

        if (person.getPhonenumber().equals("")) {
            popup.getMenu().removeItem(R.id.action_add_contact);
            popup.getMenu().removeItem(R.id.action_call);
            popup.getMenu().removeItem(R.id.action_whatsapp);
            popup.getMenu().removeItem(R.id.action_share);
        }
        if (person.getEmail().equals("")) {
            popup.getMenu().removeItem(R.id.action_email);
        }

        popup.show();
    }

    class ViewHolder {
        public TextView name, phone;
        public ImageView image, options;
    }
}
