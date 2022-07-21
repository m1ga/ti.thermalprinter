package ti.thermalprinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;


@Kroll.module(name = "TiThermalprinter", id = "ti.thermalprinter")
public class TiThermalprinterModule extends KrollModule implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {

    // You can define constants with @Kroll.constant, for example:
    @Kroll.constant
    public static final int TYPE_BLUETOOTH = 0;
    @Kroll.constant
    public static final int TYPE_TCP = 1;
    @Kroll.constant
    public static final int TYPE_USB = 2;

    public static final String[] BLUETOOTH_PERMISSIONS_S = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
    // Standard Debugging variables
    private static final String LCAT = "TiThermalprinterModule";
    private String printText = "";

    public TiThermalprinterModule() {
        super();
    }

    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {}


    // Methods
    @SuppressLint("MissingPermission")
    @Kroll.method
    public void scanPrinters() {
        BluetoothPrintersConnections bluetoothPrintersConnections = new BluetoothPrintersConnections();
        BluetoothConnection[] bluetoothPrinters = bluetoothPrintersConnections.getList();
        for (BluetoothConnection printer : bluetoothPrinters) {
            Log.i("printer", printer.getDevice().getName());
            try {
                printer.connect();
            } catch (EscPosConnectionException e) {
                Log.e("---", e.getMessage());
            }
        }
    }


    @Kroll.method
    public void getUSBList() {
        UsbConnection[] usbConnections = new UsbPrintersConnections(TiApplication.getAppCurrentActivity()).getList();
        for (UsbConnection usbConnection : usbConnections) {
            Log.i("printer", usbConnection.getDevice().getDeviceName());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Kroll.method
    public void requestPermissions() {
        Activity activity = TiApplication.getInstance().getCurrentActivity();

        // Create the permission list.
        ArrayList<String> permissionList = new ArrayList<>(4);
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 31) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        // Show dialog requesting permission.
        activity.requestPermissions(permissionList.toArray(new String[0]), TiC.PERMISSION_CODE_LOCATION);
    }

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) TiApplication.getAppCurrentActivity().getSystemService(Context.USB_SERVICE);
                    UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            try {
                                EscPosPrinter printer = new EscPosPrinter(new UsbConnection(usbManager, usbDevice), 203, 48f, 32);
                                printer.printFormattedText(printText);
                            } catch (EscPosConnectionException | EscPosBarcodeException | EscPosEncodingException | EscPosParserException e) {
                                KrollDict kd = new KrollDict();
                                kd.put("message", e.getMessage());
                                fireEvent("error", kd);
                            }
                        }
                    }
                }
            }
        }
    };

    @Kroll.method
    public void print(KrollDict options) {
        printText = TiConvert.toString(options.getString("text"), "");
        int printType = TiConvert.toInt(options.get("connection"), 0);


        if (printText != "") {
            if (printType == TYPE_BLUETOOTH) {
                try {
                    EscPosPrinter printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32);
                    printer.printFormattedText(printText);
                } catch (EscPosConnectionException | EscPosParserException | EscPosEncodingException | EscPosBarcodeException e) {
                    KrollDict kd = new KrollDict();
                    kd.put("message", e.getMessage());
                    fireEvent("error", kd);
                }
            } else if (printType == TYPE_TCP) {

                String ip = TiConvert.toString(options.get("ip"), "");
                int port = TiConvert.toInt(options.get("port"), 0);
                int timeout = TiConvert.toInt(options.get("timeout"), 15);
                int dpi = TiConvert.toInt(options.get("dpi"), 200);
                int width = TiConvert.toInt(options.get("width"), 48);
                int cpl = TiConvert.toInt(options.get("cpl"), 32);

                if (ip != "" && port != 0) {
                    try {
                        EscPosPrinter printer = new EscPosPrinter(new TcpConnection(ip, port, timeout), dpi, width, cpl);
                        printer.printFormattedText(printText);
                    } catch (EscPosConnectionException | EscPosBarcodeException | EscPosEncodingException | EscPosParserException e) {
                        KrollDict kd = new KrollDict();
                        kd.put("message", e.getMessage());
                        fireEvent("error", kd);
                    }
                } else {
                    KrollDict kd = new KrollDict();
                    kd.put("message", "Check IP and port");
                    fireEvent("error", kd);
                }
            } else if (printType == TYPE_USB) {
                UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(TiApplication.getAppCurrentActivity());
                UsbManager usbManager = (UsbManager)  TiApplication.getAppCurrentActivity().getSystemService(Context.USB_SERVICE);
                if (usbConnection != null && usbManager != null) {
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(
                            TiApplication.getAppCurrentActivity(),
                            0,
                            new Intent(ACTION_USB_PERMISSION),
                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
                    );
                    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                    TiApplication.getAppCurrentActivity().registerReceiver(usbReceiver, filter);
                    usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
                }
            }
        } else {
            KrollDict kd = new KrollDict();
            kd.put("message", "text is empty");
            fireEvent("error", kd);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.i("---", "Granted");
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        Log.i("---", "result");
    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }
}
