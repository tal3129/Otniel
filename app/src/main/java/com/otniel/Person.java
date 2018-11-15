package com.otniel;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.otniel.Activities.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Amihai on 08/09/2017.
 */

public class Person implements Comparable<Person>, android.widget.PopupMenu.OnMenuItemClickListener {

    public static Context context;
    public static SortBy sortBy = SortBy.NAME_A_TO_Z;

    private String name, surname, phonenumber="", address="", job="", email="";
    private int classIndex;
    private Bitmap img;
    private boolean rabbi;

    private PersonColor color = PersonColor.NONE;

    public enum PersonColor{
        YELLOW,
        NONE;
    }

    public Person(JSONObject json){
        try {
            name = json.getString("Name");
            surname = json.getString("Surname");
            try {
                phonenumber = json.getString("Phone");
            }catch (JSONException e){}
            try {
                email = json.getString("Email");
            }catch (JSONException e){}
            try {
                address = json.getString("Adress");
            }catch (JSONException e){}
            try {
                job = json.getString("Job");
            }catch (JSONException e){}
            classIndex = json.getInt("index");
            try {
                rabbi = json.getBoolean("rabbi");
            }catch (JSONException e){}

            color = PersonColor.valueOf(json.getString("Color"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setImg(Bitmap img) {
        this.img = img;
    }

    public String getName() {
        switch (sortBy){
            case NAME_A_TO_Z:
            case NAME_Z_TO_A:
                return (rabbi ? "הרב " : "") + name + " " + surname;
            case SURNAME_A_TO_Z:
            case SURNAME_Z_TO_A:
                return (rabbi ? "הרב " : "") + surname + " " + name;
        }
        return name + " " + surname;
    }

    public String getNameAlwaysFromName(){
        return (rabbi ? "הרב " : "") + name + " " + surname;
    }

    public PersonColor getColor() {
        return color;
    }

    public String getJob() {
        return job;
    }

    public String getEmail() {
        return email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public Bitmap getImage() {
        return img;
    }

    public void loadImage(ImageView imgView, TextView textView, TextView textView2){
        new BitmapWorkerTask(imgView, textView, textView2).execute();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contact:
                addContact();
                return true;
            case R.id.action_call:
                callContact();
                return true;
            case R.id.action_whatsapp:
                sendWhatsapp();
                return true;
            case R.id.action_share:
                shareContact(false);
                return true;
            case R.id.action_email:
                sendEmail();
                return true;
            default:
                return false;
        }
    }

    private String getLookupKey() {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phonenumber));
        String[] mPhoneNumberProjection = { ContactsContract.PhoneLookup.LOOKUP_KEY};
        Cursor cur = context.getContentResolver().query(lookupUri,mPhoneNumberProjection, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return cur.getString(0);
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return "";
    }
    private boolean contactExists() {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phonenumber));
        String[] mPhoneNumberProjection = { ContactsContract.PhoneLookup.NUMBER};
        Cursor cur = context.getContentResolver().query(lookupUri,mPhoneNumberProjection, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return false;
    }
    private void addContactAuto() {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, getNameAlwaysFromName())
                .build());

        if (img != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            img.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray)
                    .build());
        }

        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phonenumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try{
            ContentProviderResult[] results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void deleteContactAuto(String lookupKey) {
        ContentResolver cr = context.getContentResolver();
        try{
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
            cr.delete(uri, null, null);
        }
        catch(Exception e)
        {
            System.out.println(e.getStackTrace());
        }
    }

    private void shareContact(boolean delete){
        if (AskForPermissions.checkPermission((Activity)context, AskForPermissions.contacts)){
            if (contactExists()) {
                String lookupKey = getLookupKey();
                Uri vcardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
                intent.putExtra(Intent.EXTRA_STREAM, vcardUri);
                intent.putExtra(Intent.EXTRA_SUBJECT, getNameAlwaysFromName());
                if (delete){
                    MainActivity.lookupKeys.put(MainActivity.lookupKeys.size(),lookupKey);
                    ((Activity) context).startActivityForResult(intent,MainActivity.lookupKeys.size()-1);
                }else
                    (context).startActivity(intent);
            }else{
                    addContactAuto();
                    shareContact(true);
            }
        }else{
            MainActivity.call = false;
            AskForPermissions.requestPermission((Activity)context, AskForPermissions.contacts, AskForPermissions.contactsIndx);
        }
    }
    private void addContact(){
        ArrayList<ContentValues> data = new ArrayList<ContentValues>();

        if (img != null) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            img.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            row.put(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray);
            data.add(row);
        }

        Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

        intent.putExtra(ContactsContract.Intents.Insert.NAME, getNameAlwaysFromName());
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phonenumber);
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);
        intent.putExtra(ContactsContract.Intents.Insert.POSTAL, address);
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);

        context.startActivity(intent);
    }
    private void callContact(){
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phonenumber.replace("-","")));
        context.startActivity(intent);
    }
    private void sendWhatsapp(){
        String whatsappId = 972 + phonenumber.substring(1).replace("-","") + "@s.whatsapp.net";
        Uri uri = Uri.parse("smsto:" + whatsappId);
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.setPackage("com.whatsapp");
        context.startActivity(intent);
    }
    private void sendEmail(){
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.item/email");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "חי בהם");
        context.startActivity(emailIntent);
    }

    @Override
    public int compareTo(@NonNull Person o) {
        switch (sortBy){
            case NAME_A_TO_Z:
            case SURNAME_A_TO_Z:
                return getName().compareTo(o.getName());
            case NAME_Z_TO_A:
            case SURNAME_Z_TO_A:
                return o.getName().compareTo(getName());
        }
        return getName().compareTo(o.getName());
    }

    public enum SortBy{

        NAME_A_TO_Z(0),
        NAME_Z_TO_A(1),
        SURNAME_A_TO_Z(2),
        SURNAME_Z_TO_A(3);

        int index;
        SortBy(int index){this.index = index;}

        public static SortBy getSortBy(int index)
        {
            for (int i=0;i<values().length;i++){
                if (values()[i].index == index)
                    return values()[i];
            }
            return values()[0];
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        String path;
        ImageView view;
        TextView name, phone;

        public BitmapWorkerTask(ImageView view, TextView name, TextView phone) {
            this.view = view;
            this.name = name;
            this.phone = phone;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            return MainActivity.readImageFile(context.getAssets(), "imgs/"+getPhonenumber().replace("-","") + ".jpg",true);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap!=null && (name.getText().equals(getName()) || phone.getText().equals(getPhonenumber()))){
                img = bitmap;
                view.setImageBitmap(bitmap);
            }
        }
    }
}
