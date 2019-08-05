package com.megatronkenya.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.megatronkenya.travelmantics.utility.FirebaseUtil;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private static final int PICTURE_RESULT =42;
    private DatabaseReference mDatabaseReference;
    EditText txTitle,txPrice,txDescription;
    TravelDeal deal;
    ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        //FirebaseUtil.openFbReference("traveldeals", this);
        mFirebaseDatabase =  FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        txTitle = findViewById(R.id.etDealTitle);
        txPrice = findViewById(R.id.etPrice);
        imageView= findViewById(R.id.image);

        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        Picasso.get()
                .load("https://firebasestorage.googleapis.com/v0/b/travelmantics-d3fb4.appspot.com/o/deals_pictures%2Fimage%3A94650?alt=media&token=c71a5b8e-5a8d-454f-a552-950a57a0019c")
                .resize(width, width*2/3)
                .centerCrop()
                .into(imageView);


        txDescription = findViewById(R.id.etDescription);
        final Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal==null){
            deal = new TravelDeal();
        }
        this.deal = deal;
        txTitle.setText(deal.getTitle());
        txDescription.setText(deal.getDescription());
        txPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("SELECTION iNTENT","BUTTON CLICKED");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "Insert Picture"),PICTURE_RESULT);
                Log.d("SELECTION iNTENT","BUTTON CLICKED");
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==PICTURE_RESULT ){
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            Log.d("UPLOAD URL","start");
            ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    String downloadUrl;
                    Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uri.isComplete());
                    Uri url = uri.getResult();
                   // Toast.makeText(DealActivity.this, "Upload Success, download URL " +
                       //     url.toString(), Toast.LENGTH_LONG).show();
                    downloadUrl=url.toString();
                    downloadUrl = url.toString();
                    deal.setImageUrl(downloadUrl);
                    showImage(downloadUrl);


                  //  String url = taskSnapshot.getUploadSessionUri().toString();
                    String pictureName = taskSnapshot.getStorage().getPath();
                    deal.setImageName(pictureName);
                   // deal.setImageUrl(url);
                    //showImage(url);
                    Log.d("UPLOAD URL","UPLOADED");
                }
            });



        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save_menu,menu);
        if (FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_admin_menu).setVisible(true);
            enableEditText(true);
        }else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_admin_menu).setVisible(false);
            enableEditText(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_admin_menu:
                saveDeal();
                Toast.makeText(this,"Saved",Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;
            case R.id.logout_admin_menu:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                FirebaseUtil.attachListener();
                                // ...
                            }
                        });
                FirebaseUtil.detachListener();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this,"DEAL DELATED",Toast.LENGTH_LONG).show();
                backToList();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }
    private void clean() {
        txDescription.setText("");
        txPrice.setText("");
        txTitle.setText("");

    }

    private void saveDeal() {
       deal.setTitle(txTitle.getText().toString());
        deal.setPrice(txPrice.getText().toString());
        deal.setDescription(txDescription.getText().toString());
        if (deal.getId()==null){
            mDatabaseReference.push().setValue(deal);
        }else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }


    }
    private void deleteDeal(){
        if (deal == null){
            Toast.makeText(this,"Please save before deliting", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();
        if (deal.getImageName()!= null && deal.getImageName().isEmpty()==false){
            StorageReference picRef = FirebaseUtil.mStorageRef.child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete image","Successful");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", "failed"+e.getMessage());

                }
            });

        }
    }
    private void backToList(){
        Intent intent = new Intent(this,ListActivity.class);
        startActivity(intent);
    }
    private void enableEditText(boolean isEnabled){
        txTitle.setEnabled(isEnabled);
        txDescription.setEnabled(isEnabled);
        txPrice.setEnabled(isEnabled);
    }
    private void showImage(String url){
        if (url !=null && url.isEmpty()==false){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    .resize(width, width*2/3)
                    .centerCrop()
                    .into(imageView);

        }
    }
}
