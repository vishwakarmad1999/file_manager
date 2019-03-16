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
    ImageView img;
    String operation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy);

        operation = getIntent().getStringExtra("operation");
        img = (ImageView) findViewById(R.id.copyImage);

        if (operation.equals("copy")) {
            setTitle("Copy file");
            img.setImageResource(R.drawable.paste);
        } else if (operation.equals("move")) {
            img.setImageResource(R.drawable.move);
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

    public void pasteFile(View view) throws IOException {
        Intent i = getIntent();
        String fileName = i.getStringExtra("fileName");
        String from = i.getStringExtra("from");
        String to = MainActivity.getPath(pasteStack);

        File source = new File(from + fileName + File.separator);
        File destination = new File(to + fileName + File.separator);

        if (operation.equals("copy")) {
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

                setResult(RESULT_OK, i);
                finish();
            } catch (Exception e) {
                Log.e("Error", e.toString());
            }
        } else if (operation.equals("move")) {
            if (source.renameTo(destination)) {
                setResult(RESULT_OK, i);
            } else {
                setResult(RESULT_CANCELED, i);
            }
            finish();
        }
    }

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
