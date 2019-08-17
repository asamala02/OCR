package com.samapps310.ocr;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private ImageView imageHolder;
    private ProgressBar progressBar;
    private Button scanBtn;

    private boolean executionFinished = false;
    private File file = null;
    private Bitmap bitmap = null;

    private static final int READ_REQUEST_CODE = 42;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static MainActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        imageHolder = findViewById(R.id.imageHolder);
        progressBar = findViewById(R.id.pb_har);
        scanBtn = findViewById(R.id.show_result);
    }

    /**
     * Method used to capture image from inbuilt camera.
     * @param view
     */
    public void captureImage(View view) {
        if (file != null && file.exists()){
            file.delete();
            file = null;
        }
        if (executionFinished){
            Intent m_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis()+"scan.jpg");
            Uri uri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);
            m_intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(m_intent, REQUEST_IMAGE_CAPTURE);
        }
        else {
            initiateExecution();
        }
    }

    /**
     * Method used to load image from storage.
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
     * Perform ocr opperation
     * @param view
     */
    public void scanImage(View view) {
        if (bitmap != null){
            performOCR  performOCR = new performOCR();
            performOCR.execute(bitmap);
        }
        else {
            Toast.makeText(instance, "Load an image first", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!executionFinished){
            initiateExecution();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (file != null && file.exists()){
            file.delete();
            file = null;
        }
    }

    /**
     * This method is called if the trained data is not loaded (copied to external storage)
     */
    private void initiateExecution(){
        LoadTrainedData task = new LoadTrainedData();
        task.execute();
    }

    /**
     * Method to get Bitmap from Uri
     * @param uri
     * @return
     */
    private Bitmap getBitmap(Uri uri){
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(instance.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void showResultDialog(String res){
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Uri uri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);
            imageHolder.setImageURI(uri);
            this.bitmap = getBitmap(uri);
        }
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                imageHolder.setImageURI(uri);
                this.bitmap = getBitmap(uri);
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
    private class LoadTrainedData extends AsyncTask<String, Uri, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
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
            progressBar.setVisibility(View.INVISIBLE);
            if (msg.equals("done")) {
                executionFinished = true;
            } else {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Text recognition process
     */
    private class performOCR extends AsyncTask<Bitmap, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            scanBtn.setEnabled(false);
        }

        @Override
        protected String doInBackground(Bitmap... bitmap) {
            OCRHelper orc = new OCRHelper();
            String res = orc.recognizeText(bitmap[0]);
            return res;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.INVISIBLE);
            scanBtn.setEnabled(true);
            if (TextUtils.isEmpty(result)){
                Toast.makeText(MainActivity.this, "Result empty", Toast.LENGTH_SHORT).show();
            }
            else {
                showResultDialog(result);
            }
        }
    }
}
