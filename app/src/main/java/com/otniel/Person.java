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
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Amihai on 08/09/2017.
 */

public class Person implements Comparable<Person>, android.widget.PopupMenu.OnMenuItemClickListener {

    public static Context context;
    public static SortBy sortBy = SortBy.NAME_A_TO_Z;
    public ImageState imageState = ImageState.NEED_TO_DOWNLOAD; // 0 - I don't know, 1 - has image, -1 - doesn't have image


    private int imageVersion = -1;
    private String name;
    private String surname;
    private String phonenumber = "";
    private String address = "";
    private String highschool = "";
    private String email = "";
    private int classIndex;
    private boolean rabbi;
    private PersonColor color = PersonColor.NONE;
    private String picPath;

    public Uri getPicUri() {
        return picUri;
    }

    public void setPicUri(Uri picUri) {
        this.picUri = picUri;
    }

    private Uri picUri;

    public Person() {

    }

    public static void deleteContactAuto(String lookupKey) {
        ContentResolver cr = context.getContentResolver();
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
            cr.delete(uri, null, null);
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isRabbi() {
        return rabbi;
    }

    public void setRabbi(boolean rabbi) {
        this.rabbi = rabbi;
    }

    // Returns whether one of the main details of the person was differentFrom
    public boolean differentFrom(Person other) {
        return !(name.equals(other.name) &&
                surname.equals(other.surname) &&
                phonenumber.equals(other.phonenumber) &&
                email.equals(other.email) &&
                imageVersion == other.imageVersion &&
                classIndex == other.classIndex);
    }

    public String getName() {
        switch (sortBy) {
            case NAME_A_TO_Z:
            case NAME_Z_TO_A:
                return (rabbi ? "הרב " : "") + name + " " + surname;
            case SURNAME_A_TO_Z:
            case SURNAME_Z_TO_A:
                return (rabbi ? "הרב " : "") + surname + " " + name;
        }
        return name + " " + surname;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return (rabbi ? "הרב " : "") + name + " " + surname;
    }

    public PersonColor getColor() {
        return color;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getPhonenumberFromatted() {
        return phonenumber.replace("-", "");
    }

    public int getClassIndex() {
        return classIndex;
    }

    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    public String getHighschool() {
        return highschool;
    }

    public void setHighschool(String highschool) {
        this.highschool = highschool;
    }

    public void loadImage(ImageView imgView, boolean circle) {
        if (circle)
            Picasso.get().load(picUri).transform(new CircleTransform()).into(imgView);
        else
            Picasso.get().load(picUri).into(imgView);

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
        String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup.LOOKUP_KEY};
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
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
        String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup.NUMBER};
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return false;
    }

    public void addContactAuto(String stamp) {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        String val = getFullName();
        if (!stamp.isEmpty())
            val += " " + stamp;
        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, val)
                .build());

        Bitmap img = BitmapFactory.decodeFile(picPath);
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
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phonenumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            ContentProviderResult[] results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getImageVersion() {
        return imageVersion;
    }

    public void setImageVersion(int imageVersion) {
        this.imageVersion = imageVersion;
    }

    private void shareContact(boolean delete) {
        if (AskForPermissions.checkPermission((Activity) context, AskForPermissions.contacts)) {
            if (contactExists()) {
                String lookupKey = getLookupKey();
                Uri vcardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
                intent.putExtra(Intent.EXTRA_STREAM, vcardUri);
                intent.putExtra(Intent.EXTRA_SUBJECT, getFullName());
                if (delete) {
                    MainActivity.lookupKeys.put(MainActivity.lookupKeys.size(), lookupKey);
                    ((Activity) context).startActivityForResult(intent, MainActivity.lookupKeys.size() - 1);
                } else
                    (context).startActivity(intent);
            } else {
                addContactAuto("");
                shareContact(true);
            }
        } else {
            MainActivity.call = false;
            AskForPermissions.requestPermission((Activity) context, AskForPermissions.contacts, AskForPermissions.contactsIndx);
        }
    }

    private void addContact() {
        ArrayList<ContentValues> data = new ArrayList<>();
        Bitmap img = BitmapFactory.decodeFile(picPath);
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

        intent.putExtra(ContactsContract.Intents.Insert.NAME, getFullName());
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phonenumber);
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);
        intent.putExtra(ContactsContract.Intents.Insert.POSTAL, address);
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);

        context.startActivity(intent);
    }

    private void callContact() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phonenumber.replace("-", "")));
        context.startActivity(intent);
    }

    private void sendWhatsapp() {
        String url = "https://api.whatsapp.com/send?phone=" + 972 + phonenumber;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        context.startActivity(i);
    }

    private void sendEmail() {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.item/email");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
        context.startActivity(emailIntent);
    }

    @Override
    public int compareTo(@NonNull Person o) {
        switch (sortBy) {
            case NAME_A_TO_Z:
            case SURNAME_A_TO_Z:
                return getName().compareTo(o.getName());
            case NAME_Z_TO_A:
            case SURNAME_Z_TO_A:
                return o.getName().compareTo(getName());
        }
        return getName().compareTo(o.getName());
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    @Override
    public String toString() {
        return "Person{" +
                "imageVersion=" + imageVersion +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", phonenumber='" + phonenumber + '\'' +
                ", address='" + address + '\'' +
                ", highschool='" + highschool + '\'' +
                ", email='" + email + '\'' +
                ", classIndex=" + classIndex +
                ", rabbi=" + rabbi +
                ", picPath='" + picPath + '\'' +
                '}';
    }

    public String getClassIndexString() {
        return ClassIndex.values()[classIndex-1].toString();
    }

    public enum PersonColor {
        YELLOW,
        NONE
    }

    public enum SortBy {
        NAME_A_TO_Z(0),
        NAME_Z_TO_A(1),
        SURNAME_A_TO_Z(2),
        SURNAME_Z_TO_A(3);

        int index;

        SortBy(int index) {
            this.index = index;
        }

        public static SortBy getSortBy(int index) {
            for (int i = 0; i < values().length; i++) {
                if (values()[i].index == index)
                    return values()[i];
            }
            return values()[0];
        }
    }
}
