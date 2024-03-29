package com.example.mos;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mos.googleDrive.GoogleDriveUtils;
import com.example.mos.sqlite.DBUtils;
import com.example.mos.sqlite.NoteTableContract;
import com.example.mos.sqlite.SQLiteDbHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.api.services.drive.Drive;

public class AddNoteActivity extends AppCompatActivity {
    private final String MINDSET_CATEGORY = "Mindset";
    private final String RULES_CATEGORY = "Rules";
    private final String PHYSICAL_CLASSIFICATION = "Physical";
    private final String SOCIAL_CLASSIFICATION = "Social";
    private final String MENTAL_CLASSIFICATION = "Mental";
    private final String EMOTION_CLASSIFICATION = "Emotional";
    private final String FINANCIAL_CLASSIFICATION = "Financial";
    private final String EXPERIMENTAL_STATE = "Experimental";
    private final String TESTED_STATE = "Tested";
    private final String DISCARDED_STATE = "Discarded";

    SQLiteDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_note);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notesRVlayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new SQLiteDbHelper(this);
        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();


        ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intentData = result.getData();
                        try {
                            Drive googleDriveService = GoogleDriveUtils.getDriveService(AddNoteActivity.this,intentData);
                            DBUtils.backupDBtoDrive(this, SQLiteDbHelper.DATABASE_NAME,googleDriveService);
                        } catch (ApiException e) {
                            // Google Sign In failed
                            Log.w("CustomTag", "Google sign in failed:", e);
                        }
                    }
                }
        );

        EditText editContent = findViewById(R.id.editContent);
        Button categoryBtn = findViewById(R.id.categoryBtn);
        Button classificationBtn = findViewById(R.id.classificationBtn);
        FloatingActionButton addNoteBtn = findViewById(R.id.addNoteBtn);

        Intent callingIntent = getIntent();
        Bundle extras = callingIntent.getExtras();
        String calledFrom=null;
        if(extras!=null) calledFrom = extras.getString(CustomConstants.CALLED_BY);
        if(calledFrom!=null && calledFrom.equals(CustomConstants.NOTES_RV_ADAPTER)){
            editContent.setText(extras.getString(CustomConstants.NOTE_CONTENT));
            categoryBtn.setText(extras.getString(CustomConstants.NOTE_CATEGORY));
            classificationBtn.setText(extras.getString(CustomConstants.NOTE_CLASSIFICATION));
            addNoteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String content = editContent.getText().toString();
                    String classification = classificationBtn.getText().toString();
                    String category = categoryBtn.getText().toString();
                    String state = extras.getString(CustomConstants.NOTE_STATE);

                    ContentValues values = new ContentValues();
                    values.put(NoteTableContract.NoteTable.COLUMN_NAME_CATEGORY,category);
                    values.put(NoteTableContract.NoteTable.COLUMN_NAME_CLASSIFICATION,classification);
                    values.put(NoteTableContract.NoteTable.COLUMN_NAME_CONTENT,content);

                    dbHelper.updateRow(dbw, NoteTableContract.NoteTable.TABLE_NAME,values,extras.getString(CustomConstants.NOTE_ID));
                    signInAndBackupDBtoDrive(signInLauncher);
                }
            });
        }
        else {
            addNoteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String content = editContent.getText().toString();
                    String classification = classificationBtn.getText().toString();
                    String category = categoryBtn.getText().toString();
                    String state = EXPERIMENTAL_STATE;
                    dbHelper.addNote(dbw, classification, category, content, state);
                    signInAndBackupDBtoDrive(signInLauncher);
                }
            });
        }
        categoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String label = categoryBtn.getText().toString();
                if(label.equals(MINDSET_CATEGORY)) categoryBtn.setText(RULES_CATEGORY);
                else categoryBtn.setText(MINDSET_CATEGORY);
            }
        });

        classificationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String label = classificationBtn.getText().toString();
                if(label.equals(PHYSICAL_CLASSIFICATION)) classificationBtn.setText(SOCIAL_CLASSIFICATION);
                else if(label.equals(SOCIAL_CLASSIFICATION)) classificationBtn.setText(MENTAL_CLASSIFICATION);
                else if(label.equals(MENTAL_CLASSIFICATION)) classificationBtn.setText(EMOTION_CLASSIFICATION);
                else if(label.equals(EMOTION_CLASSIFICATION)) classificationBtn.setText(FINANCIAL_CLASSIFICATION);
                else if(label.equals(FINANCIAL_CLASSIFICATION)) classificationBtn.setText(PHYSICAL_CLASSIFICATION);
            }
        });
    }

    private void signInAndBackupDBtoDrive(ActivityResultLauncher<Intent> signInLauncher) {
        GoogleSignInClient client = GoogleDriveUtils.signInGoogleAndGetClient(AddNoteActivity.this);
        Intent signInIntent = client.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }
}