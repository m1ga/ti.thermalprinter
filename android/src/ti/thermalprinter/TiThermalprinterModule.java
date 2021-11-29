package ti.thermalprinter;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiConvert;


@Kroll.module(name = "TiThermalprinter", id = "ti.thermalprinter")
public class TiThermalprinterModule extends KrollModule {

    // You can define constants with @Kroll.constant, for example:
    @Kroll.constant
    public static final int TYPE_BLUETOOTH = 0;
    @Kroll.constant
    public static final int TYPE_TCP = 1;
    // Standard Debugging variables
    private static final String LCAT = "TiThermalprinterModule";
    private static final boolean DBG = TiConfig.LOGD;

    public TiThermalprinterModule() {
        super();
    }

    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {

    }

    // Methods
    @Kroll.method
    public void print(KrollDict options) {
        String txt = TiConvert.toString(options.getString("text"), "");
        int printType = TiConvert.toInt(options.get("connection"), 0);


        if (txt != "") {
            if (printType == TYPE_BLUETOOTH) {
                try {
                    EscPosPrinter printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32);
                    printer.printFormattedText(txt);
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
                        printer.printFormattedText(txt);
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
            }
        } else {
            KrollDict kd = new KrollDict();
            kd.put("message", "text is empty");
            fireEvent("error", kd);
        }
    }

	/*
	@Kroll.getProperty
	public String getExampleProp()
	{
		Log.d(LCAT, "get example property");
		return "hello world";
	}


	@Kroll.setProperty
	public void setExampleProp(String value) {
		Log.d(LCAT, "set example property: " + value);
	}
	*/
}

