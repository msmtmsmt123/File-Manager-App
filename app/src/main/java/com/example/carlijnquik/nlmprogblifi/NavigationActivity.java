package com.example.carlijnquik.nlmprogblifi;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Controls the navigation drawer
 */

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawer;
    SignInFragment signInFragment;
    GoogleApiClient driveGoogleApiClient;
    FloatingActionButton fab;
    String accountName;
    SharedPreferences prefs;
    android.widget.SearchView searchView;
    GoogleAccountCredential driveCredential;
    private static final String[] SCOPES = {DriveScopes.DRIVE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_navigation_1);

        prefs = this.getSharedPreferences("Accounts", MODE_PRIVATE);
        accountName = prefs.getString("accountName", null);

        searchView = (android.widget.SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchFiles(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab_2);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createFile();
            }
        });

        if (driveGoogleApiClient == null) {
            driveGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .build();
        }
        if(!driveGoogleApiClient.isConnected()) {
            driveGoogleApiClient.connect();
        }

        if (driveCredential == null) {
            driveCredential = GoogleAccountCredential.usingOAuth2(
                    this, Arrays.asList(SCOPES))
                    .setSelectedAccountName(accountName)
                    .setBackOff(new ExponentialBackOff());}

        new ListDriveFiles(driveCredential).execute();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_2);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_navigation_1);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_1);
        navigationView.setNavigationItemSelectedListener(this);

        // set the fragment initially
        InternalFilesFragment fragment = new InternalFilesFragment();
        Bundle bundle = new Bundle();
        bundle.putString("filePath", null);
        bundle.putString("fileLocation", null);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(R.id.drawer_content_shown_3, fragment).commit();

        signInFragment = new SignInFragment();

    }

    // search files function (not finished)
    public void searchFiles(String query){

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rvFiles);

        ArrayList<FileObject> fileList = AllInternalFiles.getInstance().getFileList();

        ArrayList<FileObject> adapterList = new ArrayList<>();
        for (int i = 0; i < fileList.size(); i++){
            if (fileList.get(i).getDriveFile() != null){
                if (fileList.get(i).getDriveFile().getName().contains(query)){
                    adapterList.add(fileList.get(i));
                }
            }
            if(fileList.get(i).getFile() != null){
                if (fileList.get(i).getFile().getName().contains(query)){
                    adapterList.add(fileList.get(i));
                }
            }
        }
        if (!adapterList.isEmpty()) {

            FileAdapter searchAdapter = new FileAdapter(this, getApplicationContext(), adapterList);
            recyclerView.setAdapter(searchAdapter);
        }
        else {
            Toast.makeText(this, "No results!", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_navigation_1);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sort_by) {
            return true;
        }
        if (id == R.id.action_select) {
            return true;
        }
        if (id == R.id.action_select_all) {
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_file_list) {
            // clear the array list to prevent duplicates
            ArrayList<FileObject> fileList = AllInternalFiles.getInstance().getFileList();
            fileList.clear();

            // set the fragment
            InternalFilesFragment fragment = new InternalFilesFragment();
            Bundle bundle = new Bundle();
            bundle.putString("filePath", null);
            bundle.putString("fileLocation", null);
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.drawer_content_shown_3, fragment).commit();

            fab = (FloatingActionButton) findViewById(R.id.fab_2);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("string fab", "fab works");
                    createFile();
                }
            });
            searchView.setVisibility(View.VISIBLE);
        }
        if (id == R.id.nav_accounts) {
            // in the future this will be a "add account" button
            fab.setVisibility(View.GONE);
            searchView.setVisibility(View.GONE);

            getSupportFragmentManager().beginTransaction().replace(R.id.drawer_content_shown_3, signInFragment).commit();

        }

        drawer = (DrawerLayout) findViewById(R.id.drawer_navigation_1);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void switchContent(int id, InternalFilesFragment internalFilesFragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(id, internalFilesFragment, internalFilesFragment.toString());
        ft.addToBackStack(null);
        ft.commit();
    }

    public void createFile() {
        Drive.DriveApi.newDriveContents(driveGoogleApiClient).setResultCallback(driveContentsCallback);
        Log.d("string create", "starting");
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        return;
                    }
                    final DriveContents driveContents = result.getDriveContents();
                    Log.d("string create2", "creating");

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("Testing")
                            .setMimeType("text/plain")
                            .build();

                    // create a file on root folder
                    Drive.DriveApi.getRootFolder(driveGoogleApiClient)
                            .createFile(driveGoogleApiClient, changeSet, driveContents)
                            .setResultCallback(fileCallback);

                    /*

                    // Perform I/O off the UI thread.
                    new Thread() {
                        @Override
                        public void run() {
                            // write content to DriveContents
                            OutputStream outputStream = driveContents.getOutputStream();
                            Writer writer = new OutputStreamWriter(outputStream);
                            try {
                                writer.write("Hello World!");
                                writer.close();
                            } catch (IOException e) {
                                //
                            }

                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle("New file")
                                    .setMimeType("text/plain")
                                    .build();

                            // create a file on root folder
                            Drive.DriveApi.getRootFolder(driveGoogleApiClient)
                                    .createFile(driveGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(fileCallback);
                        }
                    }.start();
                }*/
                };

                final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
                        ResultCallback<DriveFolder.DriveFileResult>() {
                            @Override
                            public void onResult(DriveFolder.DriveFileResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    return;
                                }
                                Toast.makeText(getApplicationContext(), result.getDriveFile().getDriveId().toString(), Toast.LENGTH_SHORT).show();
                            }
                        };

            };




}
