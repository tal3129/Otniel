package com.otniel.Activities;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.otniel.R;

public class places extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);


        for(int i = 1; i <= 1; i ++) {
            int id = getResources().getIdentifier("b" + Integer.toString(i), "id", this.getPackageName());
            Button b = findViewById(id);
            b.setOnClickListener(this);
        }

        //add fields in database
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("pepole");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for( DataSnapshot snapshot: dataSnapshot.getChildren()){
                    snapshot.getRef().child("place").setValue(0);
                    snapshot.getRef().child("classTeacher").setValue("");
                    snapshot.getRef().child("Havrota").setValue("");
                    snapshot.getRef().child("BigHavrota").setValue("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        v.setBackgroundColor(Color.RED);
        //switch (v.getId())
        //{
        //    case R.id.b10:
        //        break;
        //}
    }
}
