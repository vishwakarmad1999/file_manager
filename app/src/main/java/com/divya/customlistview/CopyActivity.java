package com.divya.customlistview;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Stack;

public class CopyActivity extends AppCompatActivity {
    ListView listView;
    ArrayList<Integer> imageResource = new ArrayList<>();
    ArrayList<String> fName = new ArrayList<>();
    ArrayList<String> dateModified = new ArrayList<>();
    CustomAdapter customAdapter;
    LinearLayout pasteLayout;

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");

    // Root path of a user's data
    final static String path = "storage/emulated/0/";

    Stack<String> pasteStack = new Stack<>();
    String operation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy);

        // operation is set when the user either taps on copy or move option
        operation = getIntent().getStringExtra("operation");

        if (operation.equals("copy")) {
            setTitle("Copy file");
        } else if (operation.equals("move")) {
            setTitle("Move file");
        }

        pasteLayout = (LinearLayout) findViewById(R.id.pasteOptions);
        listView = (ListView) findViewById(R.id.copyListView);
        fillArrayLists();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fileName = fName.get(position);
                String temp = MainActivity.getPath(pasteStack) + fileName + File.separator;
                File f = new File(temp);
                
                pasteStack.push(fileName);
                fillArrayLists();
            }
        });
    }

    // Function handling pasting/moving of the file to the location pointed by "path"
    public void pasteFile(View view) throws IOException {
        Intent i = getIntent();
        Bundle args = i.getBundleExtra("BUNDLE");
        ArrayList<String> passedFileList = (ArrayList<String>) args.getSerializable("fileList");
        String from = i.getStringExtra("from");
        String to = MainActivity.getPath(pasteStack);
        File source, destination;

            if (operation.equals("copy")) {
                for (String fileName: passedFileList) {
                    source = new File(from + fileName + File.separator);
                    destination = new File(to + fileName + File.separator);
                    if (source.getName() == destination.getName()) {
                        destination = new File(to + fileName + " (1)" + File.separator);
                    }

                    InputStream is = null;
                    OutputStream os = null;

                    try {
                        is = new FileInputStream(source);
                        os = new FileOutputStream(destination);
                        byte[] buffer = new byte[1024];

                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            os.write(buffer, 0, length);
                        }

                        is.close();
                        os.close();

                    } catch (Exception e) {
                        Log.e("Error", e.toString());
                    }
                    setResult(RESULT_OK, i);
                    finish();
            }
        }
        else if (operation.equals("move")) {
                boolean flag = true;
                for (String fileName : passedFileList) {
                    source = new File(from + fileName + File.separator);
                    destination = new File(to + fileName + File.separator);
                    if (!source.renameTo(destination)) {
                        flag = false;
                        break;
                    }
                }
                if (!flag) {
                    setResult(RESULT_CANCELED, i);
                } else {
                    setResult(RESULT_OK, i);
                }
                finish();
            }
    }

    // When the user cancel the operation
    public void cancelCopy(View view) {
        finish();
    }

    public void fillArrayLists() {
        fName.clear();
        imageResource.clear();
        dateModified.clear();

        String temp = MainActivity.getPath(pasteStack);

        // Creating an object of the File class pointing to the root directory
        File root = new File(temp);
        File[] fileList = root.listFiles();
        String extension;

        for (File f: fileList) {
            if (!f.getName().startsWith(".") && f.isDirectory()) {
                fName.add(f.getName());
                dateModified.add(dateFormat.format(f.lastModified()));
                imageResource.add(R.drawable.folder);
            }
        }
        customAdapter = new CustomAdapter(getApplicationContext(), imageResource, fName, dateModified);
        listView.setAdapter(customAdapter);
    }

    @Override
    public void onBackPressed() {
        if (pasteStack.size() == 0) {
            startActivity(new Intent(this, MainActivity.class));
            super.onBackPressed();
        } else {
            pasteStack.pop();
            fillArrayLists();
        }
    }

}
