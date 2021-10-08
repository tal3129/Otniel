package com.otniel

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.util.*

internal val NO_IMG_ERROR_MSG = "Object does not exist at location."

class DatabaseManager {
    var databasePeople: ArrayList<Person> = ArrayList()
    var devicePeople: ArrayList<Person> = ArrayList()

    // getting the people from the device;
    private fun getDataFromSP() {
        val sp = getPreferences(Context.MODE_PRIVATE)
        val appDataJson = sp.getString("appDataJson", "NO_DATA")
        val gson = Gson()
        if (appDataJson != "NO_DATA") {
            val data = gson.fromJson<AppData>(appDataJson, AppData::class.java!!)
            devicePeople = data.people
            spVersion = data.version
        }

        MainActivity.people = devicePeople
        changeIndex(EVERYONE)
    }

    // getting the people from database
    fun getPeopleFromFB() {
        val peopleRef = FirebaseDatabase.getInstance().reference.child("People")
        peopleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // First, we have the old people list.
                databasePeople.clear()

                // Now, we Iterate through the downloaded people.
                for (snapshot in dataSnapshot.children) {
                    val newPerson = snapshot.getValue(Person::class.java) ?: continue

                    // Check if there is already an old person with these details.
                    val oldPerson = Person.findPersonByNumber(MainActivity.people, newPerson.phonenumber)


                    // If there is no old person - the new one.
                    if (oldPerson == null || newPerson.differentFrom(oldPerson))
                        databasePeople.add(newPerson)
                    else
                        databasePeople.add(oldPerson)// If there is there is an old person, and it is identical

                }
                MainActivity.people.clear()
                MainActivity.people.addAll(databasePeople)
                downloadLeftImages()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    // Goes through the current people, downloads images of people
    fun downloadLeftImages() {
        for (person in MainActivity.people)
            if (person.imageState == ImageState.NEED_TO_DOWNLOAD)
                downloadImage(person, false)
    }


    // Downloads the image of this person
    private fun downloadImage(person: Person, useBig: Boolean) {
        val suffix = if (useBig) "JPG" else "jpg"
        val prefix = person.phonenumberFromatted
        val storageRef = FirebaseStorage.getInstance().getReference("$prefix.$suffix")
        var localFile: File? = null
        try {
            localFile = File.createTempFile(prefix, suffix)
        } catch (ignored: IOException) {
        }

        if (localFile != null) {
            person.picPath = localFile.path
            storageRef.getFile(localFile)
                    .addOnSuccessListener { taskSnapshot -> person.imageState = ImageState.COMPLETE }
                    .addOnFailureListener { exception ->
                        if (!useBig)
                            downloadImage(person, true)
                        else {
                            if (exception.message == NO_IMG_ERROR_MSG)
                                person.imageState = ImageState.NO_IMG
                            else
                                person.imageState = ImageState.NEED_TO_DOWNLOAD // If the image could not be loaded
                        }
                    }
        }
    }

    internal inner class AppData(var people: ArrayList<Person>, var version: Int)
}