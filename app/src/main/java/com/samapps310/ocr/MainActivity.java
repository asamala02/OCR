package com.samapps310.ocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog p;
    private ImageView imageHolder;

    private String res = null;
    private boolean executionFinished = false;

    private static final int READ_REQUEST_CODE = 42;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    public static MainActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        imageHolder = findViewById(R.id.imageHolder);
    }

    /**
     * Method used to capture image from inbuilt camera.
     * "executionFinished" is true if the "eng.traineddata" file from
     * the assets folder is copied to internal storage.
     * @param view
     */
    public void captureImage(View view) {
        if (executionFinished){
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(instance.getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
        else {
            initiateExecution();
        }
    }

    /**
     * Method used to load image from storage.
     * "executionFinished" is true if the "eng.traineddata" file from
     * the assets folder is copied to internal storage.
     * @param view
     */
    public void loadImage(View view) {
        if (executionFinished){
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
        else {
            initiateExecution();
        }
    }

    /**
     * This method gets the actual text data from the bitmap image,
     * using ORCHelper class.
     * @param bitmap
     */
    private void processImage(Bitmap bitmap){
        OCRHelper orc = new OCRHelper();
        res = orc.recognizeText(bitmap);
    }

    /**
     * After retrieving the text data from the image, the result is
     * displayed in a dialog box.
     * @param view
     */
    public void displayResult(View view) {
        if (!TextUtils.isEmpty(res)){
            new AlertDialog.Builder(instance)
                    .setTitle("Result")
                    .setMessage(res)
                    .setNegativeButton("back", null)
                    .show();
        }
        else {
            Toast.makeText(instance, "Load or capture image first", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * coping "eng.traineddata" file from the assets folder to internal storage.
     * the trained data files must be copied to the Android device in a subdirectory named tessdata.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!executionFinished){
            initiateExecution();
        }
    }

    private void initiateExecution(){
        AsyncTaskExample task = new AsyncTaskExample();
        task.execute();
    }

    /**
     * The image is displayed in the imageHolder view, and processImage() method is called
     * if the resultcode matches.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageHolder.setImageBitmap(imageBitmap);
            processImage(imageBitmap);
        }
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                imageHolder.setImageURI(uri);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    processImage(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Location to store copied trained data.
     * @return
     */
    public String tessDataPath(){
        return MainActivity.instance.getExternalFilesDir(null)+"/tessdata/";
    }

    /**
     * Method to get absolute path were the trained data is stored.
     * This method is accessed by the OCRHelper class.
     * See OCRHelper class
     * @return
     */
    public String getTessDataParentDir(){
        return MainActivity.instance.getExternalFilesDir(null).getAbsolutePath();
    }

    /**
     * Copies trained data files from assets folder to external storage.
     */
    private class AsyncTaskExample extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(MainActivity.this);
            p.setMessage("Please wait...");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            AssetManager assetManager = MainActivity.instance.getAssets();
            OutputStream out = null;
            try{
                InputStream in = assetManager.open("eng.traineddata");
                String tessPath = instance.tessDataPath();
                File tessFolder = new File(tessPath);
                if (!tessFolder.exists()){
                    tessFolder.mkdir();
                }
                String tessData = tessPath+"/"+"eng.traineddata";
                File tessFile = new File(tessData);
                if (!tessFile.exists()){
                    out = new FileOutputStream(tessData);
                    byte buffer[] = new byte[1024];
                    int read = in.read(buffer);
                    while (read != -1){
                        out.write(buffer,0, read);
                        read = in.read(buffer);
                    }
                    Log.d("MainActivity", "doInBackground: Did finished copy tess file");
                }
                else {
                    Log.d("MainActivity", "doInBackground: tess file exists");
                }
            }catch (Exception e){
                return e.getMessage();
            }finally {
                try {
                    if (out != null){
                        out.close();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    return e.getMessage();
                }
            }
            return "done";
        }

        @Override
        protected void onPostExecute(String msg) {
            super.onPostExecute(msg);
            if (msg.equals("done")) {
                p.hide();
                executionFinished = true;
            } else {
                p.hide();
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
