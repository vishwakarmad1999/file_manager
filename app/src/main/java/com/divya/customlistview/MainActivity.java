package com.divya.customlistview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    final static int fileAccessRequestCode = 5, fileCopyCode = 2, fileMoveCode = 3;
    ListView listView;
    ArrayList<Integer> imageResource = new ArrayList<>();
    ArrayList<String> fName = new ArrayList<>();
    ArrayList<String> dateModified = new ArrayList<>();
    CustomAdapter customAdapter;
    LinearLayout optionsLayout, deleteOptionsLayout, renameFLayout;
    int globalPosition = -1;
    // SimpleDateFormat formats a long type to a Date object specified in the constructor
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");

    // Root path of a user's data
    final static String path = "storage/emulated/0/";

    static Stack<String> pathStack = new Stack<>();
    EditText renameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, fileAccessRequestCode);
        } else {
            optionsLayout = (LinearLayout) findViewById(R.id.options);
            deleteOptionsLayout = (LinearLayout) findViewById(R.id.deleteConfirm);
            renameFLayout = (LinearLayout) findViewById(R.id.renameF);

            listView = (ListView) findViewById(R.id.parentView);
            fillArrayLists();

            // Handling onClick event
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String fileName = fName.get(position);
                    File f = getFile(fileName);

                    if (f.isDirectory()) {
                        pathStack.push(fileName);
                        fillArrayLists();
                    } else {
                        Toast.makeText(MainActivity.this, "It is a file", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Handling longClick event
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                    Toast.makeText(MainActivity.this, "Long pressed " + fName.get(position), Toast.LENGTH_SHORT).show();
                    optionsLayout.setVisibility(View.VISIBLE);
                    globalPosition = position;
                    return true;
                }
            });
        }
    }

    // Function handling copying files from one location to another
    public void copyFile(View view) {
        Intent copyIntent = new Intent(this, CopyActivity.class);
        copyIntent.putExtra("from", getPath(pathStack));
        copyIntent.putExtra("fileName", fName.get(globalPosition));
        copyIntent.putExtra("operation", "copy");
        startActivityForResult(copyIntent, fileCopyCode);
    }

    // Function handling moving files from one location to another
    public void moveFile(View view) {
        Intent moveIntent = new Intent(this, CopyActivity.class);
        moveIntent.putExtra("from", getPath(pathStack));
        moveIntent.putExtra("fileName", fName.get(globalPosition));
        moveIntent.putExtra("operation", "move");
        startActivityForResult(moveIntent, fileMoveCode);
    }

    // Function handling deleting a file
    public void deleteFile(View view) {
        optionsLayout.setVisibility(View.GONE);
        deleteOptionsLayout.setVisibility(View.VISIBLE);
    }

    // Function handling renaming a file's name
    public void openRenameDialogBox(View view) {
        optionsLayout.setVisibility(View.GONE);
        renameFLayout.setVisibility(View.VISIBLE);
        renameText = (EditText) findViewById(R.id.renameText);

        File f = getFile(fName.get(globalPosition));

        if (!f.isDirectory()) {
            final String temp = getFileExtension(f);
            renameText.setText(f.getName());

            renameText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renameText.setSelection(0, fName.get(globalPosition).lastIndexOf(temp));
                }
            });
        } else {
            renameText.setText(fName.get(globalPosition));
        }
    }

    public void rename(View view) {
        String name = renameText.getText().toString();
        if (name.trim().equals("")) {
            Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
        } else {
            String sourcePath = getPath(pathStack) + fName.get(globalPosition) + File.separator;
            String destinationPath = getPath(pathStack) + name + File.separator;

            File old_file = new File(sourcePath);
            File new_file = new File(destinationPath);

            if (old_file.renameTo(new_file)) {
                Toast.makeText(this, "File renamed successfully", Toast.LENGTH_SHORT).show();
                renameFLayout.setVisibility(View.GONE);
                finish();
                startActivity(getIntent());
            } else {
                Toast.makeText(this, "File wasn't renamed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void confirmDelete(View view) {
        File f = getFile(fName.get(globalPosition));
        f.delete();
        deleteOptionsLayout.setVisibility(View.GONE);
        //Refreshing the activity
        finish();
        startActivity(getIntent());
    }

    public void cancelDelete(View view) {
        optionsLayout.setVisibility(View.VISIBLE);
        deleteOptionsLayout.setVisibility(View.GONE);
    }

    public File getFile(String name) {
        String path = getPath(pathStack);
        path += name + File.separator;
        File f = new File(path);
        return f;
    }

    // This function returns the current path of the screen
    static public String getPath(Stack<String> pathStack) {
        if (pathStack.size() == 0) {
            return path;
        } else {
            String temp = path;
            for (int i = 0; i < pathStack.size(); i++) {
                temp += pathStack.get(i) + File.separator;
            }
            return temp;
        }
    }

    // This function returns the extension of the file passed to it
    static public String getFileExtension(File f) {
        String extension;
        String fname = f.getName();
        int periodPosition = fname.lastIndexOf(".");
        extension = fname.substring(periodPosition);
        return extension;
    }

    // This function helps to fill the listView with File names, their last date modified, and their respective images
    public void fillArrayLists() {
        fName.clear();
        imageResource.clear();
        dateModified.clear();

        String temp = getPath(pathStack);

        // Creating an object of the File class pointing to the root directory
        File root = new File(temp);
        File[] fileList = root.listFiles();
        String extension;

        for (File f: fileList) {
            if (!f.getName().startsWith(".")) {
                fName.add(f.getName());
                dateModified.add(dateFormat.format(f.lastModified()));

                if (f.isDirectory()) {
                    imageResource.add(R.drawable.folder);
                } else {
                    imageResource.add(R.drawable.file);
                }
            }
        }
        customAdapter = new CustomAdapter(getApplicationContext(), imageResource, fName, dateModified);
        listView.setAdapter(customAdapter);
    }

    @Override
    public void onBackPressed() {
        if (optionsLayout.getVisibility() == View.VISIBLE || renameFLayout.getVisibility() == View.VISIBLE || deleteOptionsLayout.getVisibility() == View.VISIBLE) {
            optionsLayout.setVisibility(View.GONE);
            renameFLayout.setVisibility(View.GONE);
            deleteOptionsLayout.setVisibility(View.GONE);
        }
        else if (pathStack.size() == 0) {
            super.onBackPressed();
        } else {
            pathStack.pop();
            fillArrayLists();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        optionsLayout.setVisibility(View.GONE);
        if (requestCode == fileCopyCode) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "File copied successfully", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == fileMoveCode) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "File moved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to moved the file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == fileAccessRequestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission has been granted ", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permission has not been granted " + grantResults.length, Toast.LENGTH_LONG).show();
            }
        }
    }
}
