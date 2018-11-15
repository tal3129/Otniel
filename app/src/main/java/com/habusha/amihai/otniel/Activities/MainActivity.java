package com.habusha.amihai.otniel.Activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.habusha.amihai.otniel.AskForPermissions;
import com.habusha.amihai.otniel.PeopleAdapter;
import com.habusha.amihai.otniel.Person;
import com.habusha.amihai.otniel.R;
import com.nullwire.trace.ExceptionHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static String query="";
    public static boolean call = true;
    public static HashMap<Integer,String> lookupKeys = new HashMap<>();

    int currentIndex=8;
    ArrayList<Person> people = new ArrayList<>();
    ArrayList<Person> currentPeople = new ArrayList<>();

    private boolean haibaam = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //ExceptionHandler.register(this,"http://www.ytube.co.il/Bugs.php");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (!haibaam)
            navigationView.getMenu().removeItem(R.id.haiGroup);
        navigationView.setNavigationItemSelectedListener(this);

        Person.context = this;

        String json=readFile(getAssets(), "people.json");
        try {
            JSONArray jsonArray = new JSONObject(json).getJSONArray("People");
            for (int i=0;i<jsonArray.length();i++){
                final Person person = new Person(jsonArray.getJSONObject(i));
                /*new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap image = readImageFile(getAssets(), "imgs/"+person.getPhonenumber().replace("-","") + ".jpg");
                        person.setImg(image);
                    }
                }).start();*/
                if (!haibaam && person.getClassIndex() > 8)
                    break;
                people.add(person);
            }
            Collections.sort(people);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        currentPeople.addAll(people);
        changeIndex(8);

        /*if (Calendar.getInstance().get(Calendar.YEAR) >= 2018){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("הגרסא שברשותך היא ניסיונית ופגה תוקף. \nאנא פנה למפתח למידע נוסף");
            builder.setCancelable(false);
            builder.setPositiveButton("אישור", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.create().show();
        }*/
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Person.deleteContactAuto(lookupKeys.get(requestCode));
    }

    public static String getQuery() {
        return query;
    }

    private void changeIndex(int index){
        currentIndex = index;
        updatePeopleList();
        updateList();
    }
    private void updatePeopleList(){

        currentPeople.clear();

        for (Person person : people){
            if ((currentIndex == 8 && person.getClassIndex() < 8)  || currentIndex == person.getClassIndex() ||
                    (currentIndex == 14 && person.getClassIndex() < 14 && person.getClassIndex() > 8)){
                if (query.equals("") || person.getName().contains(query) || person.getPhonenumber().replace("-","").contains(query)){
                    currentPeople.add(person);
                }
            }
        }

    }
    private void updateList(){
        Collections.sort(currentPeople);
        final PeopleAdapter adapter = new PeopleAdapter(this, currentPeople);
        ((ListView)findViewById(R.id.people_list)).setAdapter(adapter);
    }

    public static String readFile(AssetManager mgr, String path) {
        String contents = "";
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = mgr.open(path);
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            contents = reader.readLine();
            String line = null;
            while ((line = reader.readLine()) != null) {
                contents += '\n' + line;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return contents;
    }
    public static Bitmap readImageFile(AssetManager mgr, String path, boolean tryJPG) {
        Bitmap img = null;
        InputStream is = null;
        try {
            is = mgr.open(path);
            img = BitmapFactory.decodeStream(is);
        }catch (FileNotFoundException fnfE) {
            if (tryJPG)
                img = readImageFile(mgr, path.replace("jpg", "JPG"), false);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return img;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void openSortByAlertView(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.sortby_dialog, null);
        RadioGroup radioGroup = ((RadioGroup)dialogView.findViewById(R.id.radio_group));

        final RadioButton[] btns = new RadioButton[Person.SortBy.values().length];
        btns[0] = new RadioButton(this);
        btns[0].setText("שם פרטי א-ת");
        btns[1] = new RadioButton(this);
        btns[1].setText("שם פרטי ת-א");
        btns[2] = new RadioButton(this);
        btns[2].setText("שם משפחה א-ת");
        btns[3] = new RadioButton(this);
        btns[3].setText("שם משפחה ת-א");
        for (int i=0;i<btns.length;i++) {

            radioGroup.addView(btns[i]);
        }
        builder.setView(dialogView);
        switch (Person.sortBy){
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
        builder.setPositiveButton("אישור", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int i=0;i<btns.length;i++) {
                    if (btns[i].isChecked()) {
                        Person.sortBy = Person.SortBy.getSortBy(i);
                        updateList();
                        break;
                    }
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
        if (id == R.id.action_sort){
            openSortByAlertView();
        }else if (id == R.id.action_addAll){
            if (AskForPermissions.checkPermission(this,AskForPermissions.contacts)){

                showAddAllDialog();

            }else{
                MainActivity.call = true;
                AskForPermissions.requestPermission(this, AskForPermissions.contacts, AskForPermissions.contactsIndx);
            }
        }else if (id == R.id.action_emailAll){
            emailAll();
        }
        return super.onOptionsItemSelected(item);
    }

    private void emailAll(){
        ArrayList<String> emailAddress = new ArrayList<>();
        for (Person person : currentPeople){
            if (!person.getEmail().equals("")){
                emailAddress.add(person.getEmail());
            }
        }
        if (emailAddress.size() > 0){
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.item/email");
            String[] s = new String[]{};
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailAddress.toArray(s));
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "חי בהם");
            startActivity(emailIntent);
        }else{
            Toast.makeText(this, "לא נמצאו אימיילים ברשימת האנשים הנוכחית", Toast.LENGTH_LONG).show();
        }
    }

    private void showAddAllDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.add_all_dialog, null);
        ((CheckBox)dialogView.findViewById(R.id.checkBox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    dialogView.findViewById(R.id.stamp_text).setEnabled(true);
                    dialogView.findViewById(R.id.example).setEnabled(true);
                }else{
                    dialogView.findViewById(R.id.stamp_text).setEnabled(false);
                    dialogView.findViewById(R.id.example).setEnabled(false);
                }
            }
        });

        ((EditText)dialogView.findViewById(R.id.stamp_text)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                ((TextView)dialogView.findViewById(R.id.example)).setText("לדוגמא: פלוני אלמוני (" + s.toString() + ")");
            }
        });

        builder.setTitle("הוסף תגים");
        builder.setView(dialogView);
        builder.setPositiveButton("אישור", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (((CheckBox)dialogView.findViewById(R.id.checkBox)).isChecked()){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            addAllPeople(((EditText)dialogView.findViewById(R.id.stamp_text)).getText().toString());
                        }
                    }).start();
                }else{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            addAllPeople("");
                        }
                    }).start();
                }
            }
        });
        builder.create().show();
    }
    private void addAllPeople(String stamp){
        NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Adding Contacts")
                .setSmallIcon(R.drawable.noti_otniel);

        int count = 0;
        Person rndPerson = currentPeople.get((int)(Math.random()*currentPeople.size()));
        int notiIndex = Integer.parseInt(rndPerson.getPhonenumber().substring(rndPerson.getPhonenumber().length()-3));

        String tag = stamp.equals("") ? "" : "(" + stamp + ")";

        ArrayList<Person> peopleToAdd = new ArrayList<>();
        peopleToAdd.addAll(currentPeople);

        for (Person person : peopleToAdd){
            count++;

            mBuilder.setProgress(currentPeople.size(), count-1, false)
                    .setContentText(count + " of " + currentPeople.size());
            mNotifyManager.notify(notiIndex, mBuilder.build());

            boolean exist = contactExists(this, person.getPhonenumber().replace("-",""));
            if (!exist){
                addContact(person, tag);
            }
        }

        mBuilder.setProgress(currentPeople.size(), currentPeople.size(), false)
                .setContentTitle("Completed!")
                .setContentText(currentPeople.size() + " of " + currentPeople.size());
        mNotifyManager.notify(notiIndex, mBuilder.build());
    }
    private void addContact(Person person, String stamp) {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
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
                .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, person.getPhonenumber())
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try{
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private boolean contactExists(Context context, String number) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
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
        }else if (id == R.id.nav_g) {
            changeIndex(7);
        } else if (id == R.id.nav_h) {
            changeIndex(8);
        }else if (id == R.id.nav_9) {
            changeIndex(9);
        }else if (id == R.id.nav_10) {
            changeIndex(10);
        }else if (id == R.id.nav_11) {
            changeIndex(11);
        }else if (id == R.id.nav_12) {
            changeIndex(12);
        }else if (id == R.id.nav_13) {
            changeIndex(13);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    public void intalizeSearch(Menu menu){

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView =
                ((SearchView)  menu.findItem(R.id.search).getActionView());
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
                MainActivity.query = newText.replace("-","");
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
