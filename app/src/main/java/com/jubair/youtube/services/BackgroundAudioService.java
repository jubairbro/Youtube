// ক্লাসের শুরুতে ভেরিয়েবল যোগ করুন
public static boolean isServiceRunning = false;

// onCreate এ
@Override
public void onCreate() {
    super.onCreate();
    isServiceRunning = true;
    // ... বাকি কোড ...
}

// onDestroy এ
@Override
public void onDestroy() {
    isServiceRunning = false;
    // ... বাকি কোড ...
    super.onDestroy();
}
