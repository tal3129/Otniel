package com.otniel.Activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.otniel.AskForPermissions;
import com.otniel.PeopleAdapter;
import com.otniel.Person;
import com.otniel.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static boolean call = true;
    public static HashMap<Integer, String> lookupKeys = new HashMap<>();
    private static String query = "";
    int currentIndex = 8;
    ArrayList<Person> people = new ArrayList<>();
    ArrayList<Person> currentPeople = new ArrayList<>();

    ArrayList<Person> databasePeople = new ArrayList<>();
    ArrayList<Person> devicePeople = new ArrayList<>();

    private int spVersion = -1, fbVersion = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Person.context = this;

        getVersionFromFB();

        getDataFromSP();
    }


    // Gets the version from fb. If it is higher than the phone's one, update.
    private void getVersionFromFB() {
        DatabaseReference versionRef = FirebaseDatabase.getInstance().getReference().child("settings");
        versionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                AppSettings settings = dataSnapshot.getValue(AppSettings.class);
                if (settings != null)
                    fbVersion = settings.version;

                if (fbVersion > spVersion)
                    getPeopleFromFB();
                else
                    downloadLeftImages();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // Goes through the current people, downloads images of people
    private void downloadLeftImages() {
        for(Person person : people)
            if (!person.hasImage)
                downloadImage(person, false);
    }

    // getting the people from database
    private void getPeopleFromFB() {
        DatabaseReference peopleRef = FirebaseDatabase.getInstance().getReference().child("People");
        peopleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                databasePeople.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Person person = snapshot.getValue(Person.class);
                    databasePeople.add(person);
                    if (person != null)
                        downloadImage(person, false);
                }
                people = databasePeople;
                changeIndex(8);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // getting the people from the device;
    private void getDataFromSP() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        String appDataJson = sp.getString("appDataJson", "NO_DATA");
        Gson gson = new Gson();
        if (!appDataJson.equals("NO_DATA")) {
            AppData data = gson.fromJson(appDataJson, AppData.class);
            devicePeople = data.people;
            spVersion = data.version;
        }

        people = devicePeople;
        currentPeople.addAll(people);
        changeIndex(8);
    }

    // when the download was finished, update the people on the device.
    @Override
    public void onStop() {
        SharedPreferences.Editor sp = getPreferences(MODE_PRIVATE).edit();
        AppData data = new AppData(people, fbVersion);
        sp.putString("appDataJson", (new Gson()).toJson(data));
        sp.apply();
        super.onStop();
    }


    // Download the image of this person
    private void downloadImage(Person person, boolean useBig) {
        String suffix = useBig ? "JPG" : "jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference(person.getPhonenumber().replace("-", "") + ".jpg");
        File localFile = null;
        try {
            localFile = File.createTempFile(person.getPhonenumber().replace("-", ""), suffix);
        } catch (IOException ignored) {
        }
        if (localFile != null) {
            person.setPicPath(localFile.getPath());
            storageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                person.hasImage = true;
            }).addOnFailureListener(exception -> {
                if (!useBig)
                    downloadImage(person, true);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Person.deleteContactAuto(lookupKeys.get(requestCode));
    }

    private void changeIndex(int index) {
        currentIndex = index;
        updatePeopleList();
        updateList();
    }

    private void updatePeopleList() {

        currentPeople.clear();

        for (Person person : people) {
            if ((currentIndex == 8 && person.getClassIndex() < 8) || currentIndex == person.getClassIndex() ||
                    (currentIndex == 14 && person.getClassIndex() < 14 && person.getClassIndex() > 8)) {
                if (query.equals("") || person.getName().contains(query) || person.getPhonenumber().replace("-", "").contains(query)) {
                    currentPeople.add(person);
                }
            }
        }

    }

    private void updateList() {
        Collections.sort(currentPeople);
        final PeopleAdapter adapter = new PeopleAdapter(this, currentPeople);
        ((ListView) findViewById(R.id.people_list)).setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void openSortByAlertView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.sortby_dialog, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group);

        final RadioButton[] btns = new RadioButton[Person.SortBy.values().length];
        btns[0] = new RadioButton(this);
        btns[0].setText("שם פרטי א-ת");
        btns[1] = new RadioButton(this);
        btns[1].setText("שם פרטי ת-א");
        btns[2] = new RadioButton(this);
        btns[2].setText("שם משפחה א-ת");
        btns[3] = new RadioButton(this);
        btns[3].setText("שם משפחה ת-א");
        for (RadioButton btn : btns) {
            radioGroup.addView(btn);
        }
        builder.setView(dialogView);
        switch (Person.sortBy) {
            case NAME_A_TO_Z:
                btns[0].setChecked(true);
                break;
            case NAME_Z_TO_A:
                btns[1].setChecked(true);
                break;
            case SURNAME_A_TO_Z:
                btns[2].setChecked(true);
                break;
            case SURNAME_Z_TO_A:
                btns[3].setChecked(true);
                break;
        }
        builder.setPositiveButton("אישור", (dialog, which) -> {
            for (int i = 0; i < btns.length; i++) {
                if (btns[i].isChecked()) {
                    Person.sortBy = Person.SortBy.getSortBy(i);
                    updateList();
                    break;
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        intalizeSearch(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            openSortByAlertView();
        } else if (id == R.id.action_addAll) {
            if (AskForPermissions.checkPermission(this, AskForPermissions.contacts)) {

                showAddAllDialog();

            } else {
                MainActivity.call = true;
                AskForPermissions.requestPermission(this, AskForPermissions.contacts, AskForPermissions.contactsIndx);
            }
        } else if (id == R.id.action_emailAll) {
            emailAll();
        }
        return super.onOptionsItemSelected(item);
    }

    private void emailAll() {
        ArrayList<String> emailAddress = new ArrayList<>();
        for (Person person : currentPeople) {
            if (!person.getEmail().equals("")) {
                emailAddress.add(person.getEmail());
            }
        }
        if (emailAddress.size() > 0) {
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.item/email");
            String[] s = new String[]{};
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailAddress.toArray(s));
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "חי בהם");
            startActivity(emailIntent);
        } else {
            Toast.makeText(this, "לא נמצאו אימיילים ברשימת האנשים הנוכחית", Toast.LENGTH_LONG).show();
        }
    }

    private void showAddAllDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.add_all_dialog, null);
        ((CheckBox) dialogView.findViewById(R.id.checkBox)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                dialogView.findViewById(R.id.stamp_text).setEnabled(true);
                dialogView.findViewById(R.id.example).setEnabled(true);
            } else {
                dialogView.findViewById(R.id.stamp_text).setEnabled(false);
                dialogView.findViewById(R.id.example).setEnabled(false);
            }
        });

        ((EditText) dialogView.findViewById(R.id.stamp_text)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ((TextView) dialogView.findViewById(R.id.example)).setText("לדוגמא: פלוני אלמוני (" + s.toString() + ")");
            }
        });

        builder.setTitle("הוסף תגים");
        builder.setView(dialogView);
        builder.setPositiveButton("אישור", (dialog, which) -> {
            if (((CheckBox) dialogView.findViewById(R.id.checkBox)).isChecked()) {
                new Thread(() -> addAllPeople(((EditText) dialogView.findViewById(R.id.stamp_text)).getText().toString())).start();
            } else {
                new Thread(() -> addAllPeople("")).start();
            }
        });
        builder.create().show();
    }

    private void addAllPeople(String stamp) {
        NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Adding Contacts")
                .setSmallIcon(R.drawable.noti_otniel);

        int count = 0;
        Person rndPerson = currentPeople.get((int) (Math.random() * currentPeople.size()));
        int notiIndex = Integer.parseInt(rndPerson.getPhonenumber().substring(rndPerson.getPhonenumber().length() - 3));

        String tag = stamp.equals("") ? "" : "(" + stamp + ")";

        ArrayList<Person> peopleToAdd = new ArrayList<>(currentPeople);

        for (Person person : peopleToAdd) {
            count++;

            mBuilder.setProgress(currentPeople.size(), count - 1, false)
                    .setContentText(count + " of " + currentPeople.size());
            mNotifyManager.notify(notiIndex, mBuilder.build());

            boolean exist = contactExists(this, person.getPhonenumber().replace("-", ""));
            if (!exist) {
                addContact(person, tag);
            }
        }

        mBuilder.setProgress(currentPeople.size(), currentPeople.size(), false)
                .setContentTitle("Completed!")
                .setContentText(currentPeople.size() + " of " + currentPeople.size());
        mNotifyManager.notify(notiIndex, mBuilder.build());
    }

    private void addContact(Person person, String stamp) {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, person.getNameAlwaysFromName() + " " + stamp)
                .build());

        if (person.getImage() != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            person.getImage().compress(Bitmap.CompressFormat.PNG, 100, stream);
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
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, person.getPhonenumber())
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean contactExists(Context context, String number) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup.NUMBER};
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_a) {
            changeIndex(1);
        } else if (id == R.id.nav_b) {
            changeIndex(2);
        } else if (id == R.id.nav_c) {
            changeIndex(3);
        } else if (id == R.id.nav_d) {
            changeIndex(4);
        } else if (id == R.id.nav_e) {
            changeIndex(5);
        } else if (id == R.id.nav_f) {
            changeIndex(6);
        } else if (id == R.id.nav_g) {
            changeIndex(7);
        } else if (id == R.id.nav_h) {
            changeIndex(8);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void intalizeSearch(Menu menu) {

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView =
                ((SearchView) menu.findItem(R.id.search).getActionView());
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                MainActivity.query = newText.replace("-", "");
                updatePeopleList();
                updateList();
                return false;
            }
        });

        MenuItem item = menu.findItem(R.id.search);
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                MainActivity.query = "";
                updatePeopleList();
                updateList();

                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;  // Return true to expand action view
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (call)
                showAddAllDialog();

        } else if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_DENIED) {

            Toast.makeText(this, "הפעולה המבוקשת לא יכולה להתבצע ללא הרשאה זו", Toast.LENGTH_LONG).show();

        }
    }

    public static String getQuery() {
        return query;
    }

    class AppData {
        ArrayList<Person> people;
        int version;

        public AppData(ArrayList<Person> devicePeople, int version) {
            this.people = devicePeople;
            this.version = version;
        }
    }
}
