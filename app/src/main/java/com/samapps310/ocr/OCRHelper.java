package com.samapps310.ocr;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

public class OCRHelper {

    private TessBaseAPI baseAPI = null;

    /**
     * Method to initialize TessBaseAPI
     */
    private void initAPI(){
        baseAPI = new TessBaseAPI();
        String datapath = MainActivity.instance.getTessDataParentDir();
        baseAPI.init(datapath, "eng");
    }

    /**
     * method were the actual magic is done. (text from bitmap)
     * @param bitmap
     * @return
     */
    public String recognizeText(Bitmap bitmap){
        if (baseAPI == null){
            initAPI();
        }
        baseAPI.setImage(bitmap);
        return baseAPI.getUTF8Text();
    }
}
