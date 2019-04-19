package com.divya.customlistview;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    int count = 0;

    final static int fileAccessRequestCode = 5, fileCopyCode = 2, fileMoveCode = 3;
    ListView listView;
    ArrayList<Integer> imageResource = new ArrayList<>();
    ArrayList<String> fName = new ArrayList<>();
    ArrayList<String> dateModified = new ArrayList<>();
    ArrayList<String> fileList = new ArrayList<>();

    CustomAdapter customAdapter;
    LinearLayout deleteOptionsLayout, renameFLayout, createNewFolderLayout;
    // SimpleDateFormat formats a long type to a Date object specified in the constructor
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");

    ArrayList<Integer> fileChecked = new ArrayList<>();

    // Root path of a user's data
    final static String path = "storage/emulated/0/";

    static Stack<String> pathStack = new Stack<>();
    EditText renameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Checking whether the user has granted the permissions or not
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, fileAccessRequestCode);
        } else {
            deleteOptionsLayout = (LinearLayout) findViewById(R.id.deleteConfirm);
            renameFLayout = (LinearLayout) findViewById(R.id.renameF);
            createNewFolderLayout = (LinearLayout) findViewById(R.id.createFolderLayout);

            listView = (ListView) findViewById(R.id.parentView);
            fillArrayLists();
            listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

            listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                    View item = getViewByPosition(position, listView);

                    if (checked) {
                        fileList.add(fName.get(position));
                        count += 1;
                        item.setBackgroundColor(Color.GRAY);
                    } else {
                        fileList.remove(fName.get(position));
                        count -= 1;
                        item.setBackgroundColor(Color.WHITE);
                    }

                    mode.setTitle(count + " selected");
                }
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.multiple_options_menu, menu);

                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//                    Toast.makeText(MainActivity.this, "onPrepareActionMode", Toast.LENGTH_SHORT).show();
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.delete_menu_id) {
                        deleteFile();
                    } else if (id == R.id.copy_menu_id) {
                        copyFile();
                    } else if (id == R.id.move_menu_id) {
                        moveFile();
                    } else if (id == R.id.rename_menu_id) {
                        if (count == 1) {
                            openRenameDialogBox();
                        } else {
                            Toast.makeText(MainActivity.this, "Can't rename multiple files at once", Toast.LENGTH_LONG).show();
                        }
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    count = 0;
                }
            });

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
        }
    }

    // This method is used to fetch the selected item on the listview
    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    // Function handling copying files from one location to another
    public void copyFile() {
        Intent copyIntent = new Intent(this, CopyActivity.class);
        copyIntent.putExtra("from", getPath(pathStack));

        Bundle args = new Bundle();
        args.putSerializable("fileList", (Serializable) fileList);
        copyIntent.putExtra("BUNDLE", args);

        copyIntent.putExtra("operation", "copy");
        startActivityForResult(copyIntent, fileCopyCode);
        fileList.clear();
    }

    // Function handling moving files from one location to another
    public void moveFile() {
        Intent moveIntent = new Intent(this, CopyActivity.class);
        moveIntent.putExtra("from", getPath(pathStack));

        Bundle args = new Bundle();
        args.putSerializable("fileList", (Serializable) fileList);
        moveIntent.putExtra("BUNDLE", args);

        moveIntent.putExtra("operation", "move");
        startActivityForResult(moveIntent, fileMoveCode);
        fileList.clear();
    }

    // Function handling deleting a file
    public void deleteFile() {
        deleteOptionsLayout.setVisibility(View.VISIBLE);
    }

    // Function handling renaming a file's name
    public void openRenameDialogBox() {
        renameFLayout.setVisibility(View.VISIBLE);
        renameText = (EditText) findViewById(R.id.renameText);

        File f = getFile(fileList.get(0));

        if (!f.isDirectory()) {
            final String temp = getFileExtension(f);
            renameText.setText(f.getName());

            renameText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renameText.setSelection(0, fileList.get(0).lastIndexOf(temp));
                }
            });
        } else {
            renameText.setText(fileList.get(0));
        }
    }

    // When the rename item on the menu is clicked, this method is called
    public void rename(View view) {
        String name = renameText.getText().toString();
        if (name.trim().equals("")) {
            Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
        } else {
            String sourcePath = getPath(pathStack) + fileList.get(0) + File.separator;
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

    // This method is called when you click on the checkbox of the delete confirmation box
    public void confirmDelete(View view) {
        for (String fileName: fileList) {
            File f = getFile(fileName);
            f.delete();
        }
        deleteOptionsLayout.setVisibility(View.GONE);
        //Refreshing the activity
        fileList.clear();
        finish();
        startActivity(getIntent());
    }

    // This method is called when you click on the cross of the delete confirmation box
    public void cancelDelete(View view) {
        deleteOptionsLayout.setVisibility(View.GONE);
    }

    // This function returns the object of File class on the basis of its name
    public static File getFile(String name) {
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

    // Function handling back key events
    @Override
    public void onBackPressed() {
        if (renameFLayout.getVisibility() == View.VISIBLE || deleteOptionsLayout.getVisibility() == View.VISIBLE || createNewFolderLayout.getVisibility() == View.VISIBLE) {
            renameFLayout.setVisibility(View.GONE);
            deleteOptionsLayout.setVisibility(View.GONE);
            createNewFolderLayout.setVisibility(View.GONE);
        }
        else if (pathStack.size() == 0) {
            super.onBackPressed();
        } else {
            pathStack.pop();
            fillArrayLists();
        }
    }

    // This method is called whwen the CopyActivity has done its job and returns the result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == fileCopyCode) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "File copied successfully", Toast.LENGTH_SHORT).show();
                finish();
                startActivity(getIntent());
            }
        } else if (requestCode == fileMoveCode) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "File moved successfully", Toast.LENGTH_SHORT).show();
                finish();
                startActivity(getIntent());
            } else {
                Toast.makeText(this, "Unable to move the file(s)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // This method is called when the user either grants/denies the permissions
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

    // This method is called when you hit CREATE button while creating a new folder
    public void createFolder(View v) {
        EditText editText = (EditText) findViewById(R.id.newFolderName);
        String folderName = editText.getText().toString();
        if (! new File(path + folderName).mkdirs()) {
            Toast.makeText(this, folderName + " already exists", Toast.LENGTH_SHORT).show();
        }
        createNewFolderLayout.setVisibility(View.GONE);
        finish();
        startActivity(getIntent());
    }

    // This method acts as an event listener to the items present in the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newFolder:
//                Toast.makeText(this, "Want to make a new folder?", Toast.LENGTH_SHORT).show();
                createNewFolderLayout.setVisibility(View.VISIBLE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is used to inflate the application with our custom toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }
}
