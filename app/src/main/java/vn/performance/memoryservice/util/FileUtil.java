package vn.performance.memoryservice.util;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();
    public static void writeFile(String path, String value) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            bw.write(value);
            bw.flush();
            bw.close();
            Log.d(TAG, "writeFile: success");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "writeFile: fail");
        }
    }
}
