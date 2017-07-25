package vn.performance.memoryservice;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MemoryServices extends Service {
    final Pattern PATTERN = Pattern.compile("([a-zA-Z]+):\\s*(\\d+)");
    DecimalFormat twoDecimalForm = new DecimalFormat("#.##");
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss ");
    File file;
    private BufferedReader reader;
    private String[] sa;
    private long work, total, workT, totalT, totalBefore, workBefore;
    private float cpuTotal;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while ((true)) {
                    //TODO READ/WRITE MEMORY TO FILE HERE
                    try {
                        Thread.sleep(60000);
                        //save one value to use if not it not correct cpu usage
                        final String CpuUsage = twoDecimalForm.format(CpuUsage());
                        WriteLog(CpuUsage);
                        WriteHighest(CpuUsage);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        return START_STICKY;
    }

    public float CpuUsage() {
        try {
            reader = new BufferedReader(new FileReader("/proc/stat"));
            sa = reader.readLine().split("[ ]+", 9);
            work = Long.parseLong(sa[1]) + Long.parseLong(sa[2]) + Long.parseLong(sa[3]);
            total = work + Long.parseLong(sa[4]) + Long.parseLong(sa[5]) + Long.parseLong(sa[6]) + Long.parseLong(sa[7]);
            if (totalBefore != 0) {
                totalT = total - totalBefore;
                workT = work - workBefore;
                cpuTotal = workT * 100 / (float) totalT;
            }
            totalBefore = total;
            workBefore = work;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cpuTotal;
    }

    private MemorySize getMemorySize() {
        MemorySize result = new MemorySize();
        String line;
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
            while ((line = reader.readLine()) != null) {
                Matcher m = PATTERN.matcher(line);
                if (m.find()) {
                    String name = m.group(1);
                    String size = m.group(2);
                    if (name.equalsIgnoreCase("MemTotal")) {
                        result.total = Long.parseLong(size);
                    } else if (name.equalsIgnoreCase("MemFree") || name.equalsIgnoreCase("Buffers") ||
                            name.equalsIgnoreCase("Cached") || name.equalsIgnoreCase("SwapFree")) {
                        result.free += Long.parseLong(size);
                    }
                }
            }
            reader.close();

            result.total /= 1024;
            result.free /= 1024;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public void WriteLog(String CpuUsage) {
        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MemoryServices");
        //create folder
        if (!path.exists()) {
            path.mkdir();
        }
        file = new File(path, "Memory usage log.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();

            }
        } catch (Exception ex) {
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            Date date = new Date();
            String array = String.format(getString(R.string.writeLog), dateFormat.format(date).toString(),
                    String.valueOf(twoDecimalForm.format(getMemorySize().total)),
                    String.valueOf(twoDecimalForm.format(getMemorySize().total - getMemorySize().free))
                    , CpuUsage);
            Log.d("Array", array);
            bufferedWriter.append(array);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (Exception ex) {
        }
    }

    public void WriteHighest(String CpuUsage) {
        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MemoryServices");
        //create folder
        if (!path.exists()) {
            path.mkdir();
        }
        file = new File(path, "Highest.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception ex) {

        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            StringBuilder stringBuilder = new StringBuilder();
            Date date = new Date();
            Float newRamUsage = Float.parseFloat(twoDecimalForm.format(getMemorySize().total - getMemorySize().free));
            Float totalRam = Float.parseFloat(twoDecimalForm.format(getMemorySize().total));
            if (file.length() == 0) {
                stringBuilder.append(dateFormat.format(date) + "\n");
                stringBuilder.append(String.format(getString(R.string.highestRam), newRamUsage, totalRam));
                stringBuilder.append(String.format(getString(R.string.highestCpu), CpuUsage));
                bufferedWriter.append(stringBuilder);
                bufferedWriter.newLine();
                bufferedWriter.close();
            } else {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line;
                boolean change = false;
                int i = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    if (i == 1) {
                        String a[] = line.substring(20, line.length()).split("/");
                        Float lastRamUsage = Float.parseFloat(a[0]);
                        if (lastRamUsage < newRamUsage) {
                            change = true;
                        }
                    } else if (i == 2) {
                        Float lastCpu = Float.parseFloat(line.substring(20, line.length()));
                        if (lastCpu < Float.parseFloat(CpuUsage)) {
                            change = true;
                        }
                    }
                    i++;
                }
                if (change) {
                    if (file.delete()) {
                        file.createNewFile();
                    }
                    BufferedWriter bufferedWriterNew = new BufferedWriter(new FileWriter(file, true));
                    stringBuilder.append(dateFormat.format(date) + "\n");
                    stringBuilder.append(String.format(getString(R.string.highestRam), newRamUsage, totalRam));
                    stringBuilder.append(String.format(getString(R.string.highestCpu), CpuUsage));
                    bufferedWriterNew.append(stringBuilder);
                    bufferedWriterNew.newLine();
                    bufferedWriterNew.close();
                }
            }
        } catch (Exception ex) {
        }
    }

    private static class MemorySize {
        public float total = 0;
        public float free = 0;
    }
}
