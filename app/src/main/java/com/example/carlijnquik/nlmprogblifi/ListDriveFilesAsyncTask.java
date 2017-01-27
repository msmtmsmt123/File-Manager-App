package com.example.carlijnquik.nlmprogblifi;

/**
 * Created by Carlijn Quik on 1/24/2017.
 */

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An asynchronous task that handles the Drive API call.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */
public class ListDriveFilesAsyncTask extends AsyncTask<Void, Void, List<String>> {
    private com.google.api.services.drive.Drive mService = null;
    private Exception mLastError = null;
    ArrayList<FileObject> driveFiles;

    ListDriveFilesAsyncTask(GoogleAccountCredential credential) {

        Log.d("string check", "checking");

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("BliFi")
                .build();
    }

    /**
     * Background task to call Drive API, no parameters needed for this task.
     */
    @Override
    protected List<String> doInBackground(Void... params) {
        try {
            Log.d("string check2", "checking");

            return getDataFromApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    /**
     * Retrieve the files from Google Drive.
     */
    public List<String> getDataFromApi() throws IOException {
        List<String> fileInfo = new ArrayList<>();

        Log.d("string check3", "checking");

        FileList result = mService.files().list()
                .setFields("nextPageToken, files")
                .execute();

        Log.d("string check4", "checking");

        List<File> files = result.getFiles();

        Log.d("string check5", "checking");



        if (files != null) {
            // get the global array list with dive files
            driveFiles = DriveFilesSingleton.getInstance().getFileList();
            driveFiles.clear();


            for (File file : files) {
                fileInfo.add(String.format("%s (%s)\n", file.getName(), file.getId()));
                Log.d("string driveFile", file.getMimeType());
                Log.d("string driveFile", file.getName());

                driveFiles.add(new FileObject(file, null, "DRIVE", "file"));

            }


        }


        return fileInfo;
    }

    @Override
    protected void onPostExecute(List<String> output) {
        Log.d("string yes", "yes");

    }



}




