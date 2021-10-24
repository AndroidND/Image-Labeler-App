package com.haodydoody.imagelabeler;

import android.content.Context;
import android.graphics.Bitmap;

public class ImageLabeler {
    public static final String PROCESS_ON_DEVICE = "local";
    public static final String PROCESS_IN_CLOUD = "cloud";


    // labels image
    Context mContext;
    Bitmap mBitmapImage;
    String mProcessMethod;

}
