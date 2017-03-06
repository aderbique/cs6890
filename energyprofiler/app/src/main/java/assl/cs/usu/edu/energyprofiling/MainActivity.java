package assl.cs.usu.edu.energyprofiling;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Process;
import android.os.Debug;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public ScrollView scrollView;
    public TextView infoTextView;
    private ActivityManager am;
    private int memTotal, pId;
    private Debug.MemoryInfo[] amMI;
    private ActivityManager.MemoryInfo mi;

    private List<String> memUsed, memAvailable, memFree, cached, threshold;
    private List<Float> cpuTotal, cpuAM;
    private List<Integer> memoryAM;

    static int counter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoTextView = (TextView) findViewById(R.id.InfoText) ;
        infoTextView.setVerticalScrollBarEnabled(true);
        infoTextView.setMovementMethod(new ScrollingMovementMethod());

        try {

            class MyTimerTask extends TimerTask {

                @Override
                public void run() {
                    infoTextView.append("Timer task started at:"+new Date() +"\n");
                    completeTask();
                    infoTextView.append("Timer task finished at:"+new Date() + "\n");
                }

                private void completeTask() {
                    try {
                        //assuming it takes 20 secs to complete the task
                        infoTextView.append("This is running \n");
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                public void main(){
                    TimerTask timerTask = new MyTimerTask();
                    //running timer task as daemon thread
                    Timer timer = new Timer(true);
                    timer.scheduleAtFixedRate(timerTask, 0, 10*1000);
                    infoTextView.append("TimerTask started");
                    //cancel after sometime
                    try {
                        Thread.sleep(1200);
                        infoTextView.append("I am running \n");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timer.cancel();
                    infoTextView.append("TimerTask cancelled");
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }



            MyTimerTask myTimerTask = new MyTimerTask();
            Class<?> powerProfileClazz = Class.forName("com.android.internal.os.PowerProfile");

            //get constructor that takes a context object
            Class[] argTypes = {Context.class};
            Constructor constructor = powerProfileClazz
                    .getDeclaredConstructor(argTypes);
            Object[] arguments = {this};

            //Instantiate
            Object powerProInstance = constructor.newInstance(arguments);

            //define method
            Method batteryCap = powerProfileClazz.getMethod("getBatteryCapacity", null);
            Method averagePower = powerProfileClazz.getMethod("getAveragePower", new Class[]{String.class, int.class});
            Method averagePower_nolevel = powerProfileClazz.getMethod("getAveragePower", new Class[]{String.class});

            //call method
            infoTextView.append("Energy Profiling app written by Austin Derbique. This app creates 10000 random numbers and sorts them. On average, it takes about 12 seconds. \n\n --------------------------\n\n");

            infoTextView.append("Starting Sorting methods \n\n");
            Timestamp timestampStart = new Timestamp(System.currentTimeMillis());
            long startTime = timestampStart.getTime();
            //infoTextView.append("Time is " + startTime + "\n");

            infoTextView.append("The start time is" + new Date() + "\n\n");
            SortMethods(10000);

            infoTextView.append("Sorting Methods Finished \n\n");
            Timestamp timestampStop = new Timestamp(System.currentTimeMillis());
            long stopTime = timestampStop.getTime();
            //infoTextView.append("Stop time is" + stopTime + " \n");
            infoTextView.append("The stop time is" + new Date() + "\n\n");

            double delta = stopTime-startTime;
            //infoTextView.append("Time is " + delta);
            infoTextView.append("The time delta for the function to complete is " + String.valueOf(delta) + " milliseconds \n\n");

            //The power_profile.xml sheet states that the CPU for this device uses 100mah when active.
            //nominal voltage = 3.8 volts
            // 3.8 * 100mah/60/60

            

            double joules = ((3.8 * 4 * 100)/3600) * (delta/1000);

            infoTextView.append("The joules required for the CPU computations was: " + String.valueOf(joules) + "J. \n\n");

            /*
            infoTextView.append("CPU core 1: " + averagePower.invoke(powerProInstance, new Object[]{"cpu.active", 0}).toString() + "\n");
            infoTextView.append("CPU core 2: " + averagePower.invoke(powerProInstance, new Object[]{"cpu.active", 1}).toString() + "\n");
            infoTextView.append("CPU core 3: " + averagePower.invoke(powerProInstance, new Object[]{"cpu.active", 2}).toString() + "\n");
            infoTextView.append("CPU core 4: " + averagePower.invoke(powerProInstance, new Object[]{"cpu.active", 3}).toString() + "\n");

            */

            infoTextView.append("WiFi on: " + averagePower.invoke(powerProInstance, new Object[]{"wifi.on", 0}).toString() + "\n");
            infoTextView.append("WiFi active: " + averagePower.invoke(powerProInstance, new Object[]{"wifi.active", 0}).toString() + "\n");
            infoTextView.append("Gps on: " + averagePower.invoke(powerProInstance, new Object[]{"gps.on", 0}).toString() + "\n");
            infoTextView.append("Screen: " + averagePower_nolevel.invoke(powerProInstance, new Object[]{"screen.full"}).toString() + "\n");

            cpuTotal = new ArrayList<Float>();
            cpuAM = new ArrayList<Float>();
            memoryAM = new ArrayList<Integer>();
            memUsed = new ArrayList<String>();
            memAvailable = new ArrayList<String>();
            memFree = new ArrayList<String>();

            pId = Process.myPid();

            am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            amMI = am.getProcessMemoryInfo(new int[]{ pId });
            mi = new ActivityManager.MemoryInfo();

            getCpuTime();
            getCpuFreq();
            Log.d("Profiler", batteryCap.invoke(powerProInstance, null).toString());
            Log.d("Profiler", averagePower.invoke(powerProInstance, new Object[]{"cpu.active", 1}).toString());
            Log.d("Profiler", averagePower.invoke(powerProInstance, new Object[]{"cpu.active", 2}).toString());
            Log.d("Profiler", averagePower.invoke(powerProInstance, new Object[]{"cpu.active", 3}).toString());
        } catch (Exception e) {e.printStackTrace();}
    }

    private double getCpuTime() {
        try {
            //CPU time for a specific process
            BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pId + "/stat"));

            String[] sa = reader.readLine().split("[ ]+", 18);

            //utime + stime + cutime + cstime
            long cputime = Long.parseLong(sa[13]) + Long.parseLong(sa[14]) + Long.parseLong(sa[15]) + Long.parseLong(sa[16]);
            reader.close();

            System.out.println(cputime);
            return cputime;
        }catch (Exception e) {e.printStackTrace(); return -1;}
    }

    private void getCpuFreq() {
        try {
            //Runtime.getRuntime().exec("su -c \"echo 1234 > /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq\"");

            String cpuFreq = "";
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r");
            cpuFreq = reader.readLine();
            reader.close();

            //cpuFreq = cmdCat("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq");

            infoTextView.append("CPU frequency (core 0): " + cpuFreq + "\n");

            reader = new RandomAccessFile("/sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq", "r");
            cpuFreq = reader.readLine();
            reader.close();
            infoTextView.append("CPU frequency (core 1): " + cpuFreq + "\n");

            reader = new RandomAccessFile("/sys/devices/system/cpu/cpu2/cpufreq/scaling_cur_freq", "r");
            cpuFreq = reader.readLine();
            reader.close();
            infoTextView.append("CPU frequency (core 2): " + cpuFreq + "\n");

            reader = new RandomAccessFile("/sys/devices/system/cpu/cpu3/cpufreq/scaling_cur_freq", "r");
            cpuFreq = reader.readLine();
            reader.close();
            infoTextView.append("CPU frequency (core 3): " + cpuFreq + "\n");
            infoTextView.invalidate();
        }catch (Exception e) {e.printStackTrace();}
    }

    public void SortMethods(int arraySize){

        Random generator = new Random();

        int[] list = new int[arraySize];
        for(int i=0; i<list.length; i++)
        {
            list[i] = generator.nextInt(1000);
        }

        //infoTextView.append("Original Random array: " + "\n");
        printArray(list);

        bubbleSort(list);

        //infoTextView.append("\nAfter bubble sort: " + "\n");
        printArray(list);}


    public static void bubbleSort(int[] list)
    {
        for(int i=0; i<list.length; i++)
        {
            for(int j=i + 1; j<list.length; j++)
            {
                if(list[i] > list[j])
                {
                    int temp = list[i];
                    list[i] = list[j];
                    list[j] = temp;
                }
            }

        }
    }

    public void printArray(int[] list)
    {
        for(int i=0; i<list.length; i++)
        {
            System.out.print(list[i] + ", ");
            //infoTextView.append(list[i] + ", ");
        }
        //infoTextView.append("\n");
    }


//    private String cmdCat(String f){
//
//        String[] command = {"cat", f};
//        StringBuilder cmdReturn = new StringBuilder();
//
//        try {
//            ProcessBuilder processBuilder = new ProcessBuilder(command);
//            Process process = processBuilder.start();
//
//            InputStream inputStream = process.getInputStream();
//            int c;
//
//            while ((c = inputStream.read()) != -1) {
//                cmdReturn.append((char) c);
//            }
//
//            return cmdReturn.toString();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "Something Wrong";
//        }
//
//    }

}
