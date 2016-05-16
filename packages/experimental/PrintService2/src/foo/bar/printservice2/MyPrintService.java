package foo.bar.printservice2;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintAttributes.Margins;
import android.print.PrintAttributes.Resolution;
import android.print.PrintAttributes.Tray;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MyPrintService extends PrintService {

    private static final String LOG_TAG = MyPrintService.class.getSimpleName();

    private Handler mHandler;

    @Override
    protected void onConnected() {
        mHandler = new MyHandler(getMainLooper());
        Log.i(LOG_TAG, "#onConnected()");
    }

    @Override
    protected void onDisconnected() {
        Log.i(LOG_TAG, "#onDisconnected()");
    }

    @Override
    protected void onStartPrinterDiscovery() {
        Log.i(LOG_TAG, "#onStartDiscoverPrinters()");
        Message message1 = mHandler.obtainMessage(MyHandler.MESSAGE_ADD_FIRST_FAKE_PRINTER);
        mHandler.sendMessageDelayed(message1, 0);

        Message message2 = mHandler.obtainMessage(MyHandler.MESSAGE_ADD_SECOND_FAKE_PRINTER);
        mHandler.sendMessageDelayed(message2, 10000);
    }

    @Override
    protected void onStopPrinterDiscovery() {
        Log.i(LOG_TAG, "#onStopDiscoverPrinters()");
    }

    @Override
    public void onPrintJobQueued(final PrintJob printJob) {
        Log.i(LOG_TAG, "#onPrintJobPending()");
        PrintJobInfo info = printJob.getInfo();
        final File file = new File(getFilesDir(), info.getLabel() + ".pdf");
        if (file.exists()) {
            file.delete();
        }
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                InputStream in = new BufferedInputStream(
                        new FileInputStream(printJob.getData()));
                OutputStream out = null;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    final byte[] buffer = new byte[8192];
                    while (true) {
                        final int readByteCount = in.read(buffer);
                        if (readByteCount < 0) {
                            break;
                        }
                        out.write(buffer, 0, readByteCount);
                    }
                } catch (IOException ioe) {
                    /* ignore */
                } finally {
                    try {
                        in.close();
                    } catch (IOException ioe) {
                       /* ignore */
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ioe) {
                           /* ignore */
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                file.setExecutable(true, false);
                file.setWritable(true, false);
                file.setReadable(true, false);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/pdf");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent, null);

                if (printJob.isQueued()) {
                    printJob.start();
                }

                PrintJobInfo info =  printJob.getInfo();
                Toast.makeText(MyPrintService.this,
                        "Printer: " + info.getPrinterId().getName()
                        + " copies: " + info.getAttributes().getCopies(),
                        Toast.LENGTH_SHORT).show();

                if (printJob.isStarted()) {
                    printJob.complete();
                }
            }
        };
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
    }

    private void addFirstFakePrinter() {
        PrinterId printerId = generatePrinterId("1");
        PrinterInfo printer = new PrinterInfo.Builder(printerId, "Printer 1")
                .setStatus(PrinterInfo.STATUS_READY)
                .setMinMargins(new Margins(0, 0, 0, 0), new Margins(0, 0, 0, 0))
                .addMediaSize(PrintAttributes.MediaSize.ISO_A2, false)
                .addMediaSize(PrintAttributes.MediaSize.ISO_A3, false)
                .addMediaSize(PrintAttributes.MediaSize.ISO_A4, false)
                .addMediaSize(PrintAttributes.MediaSize.ISO_A5, false)
                .addMediaSize(PrintAttributes.MediaSize.ISO_A6, false)
                .addMediaSize(PrintAttributes.MediaSize.NA_LETTER, true)
                .addResolution(new Resolution("R1", getPackageName(),
                        R.string.resolution_600x600, 600, 600), true)
                .addInputTray(new Tray("FirstInputTray", getPackageName(),
                        R.string.input_tray_first), false)
                .addOutputTray(new Tray("FirstOutputTray", getPackageName(),
                        R.string.output_tray_first), false)
                .setDuplexModes(PrintAttributes.DUPLEX_MODE_NONE
                        | PrintAttributes.DUPLEX_MODE_LONG_EDGE
                        | PrintAttributes.DUPLEX_MODE_SHORT_EDGE,
                        PrintAttributes.DUPLEX_MODE_NONE)
                .setColorModes(PrintAttributes.COLOR_MODE_COLOR
                        | PrintAttributes.COLOR_MODE_MONOCHROME,
                        PrintAttributes.COLOR_MODE_COLOR)
                .setFittingModes(PrintAttributes.FITTING_MODE_NONE
                        | PrintAttributes.FITTING_MODE_FIT_TO_PAGE,
                        PrintAttributes.FITTING_MODE_NONE)
                .setOrientations(PrintAttributes.ORIENTATION_PORTRAIT
                        | PrintAttributes.ORIENTATION_LANDSCAPE,
                        PrintAttributes.ORIENTATION_PORTRAIT)
                .create();
        List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
        printers.add(printer);
        addPrinters(printers);
    }

    private void addSecondFakePrinter() {
        PrinterId printerId = generatePrinterId("2");
        PrinterInfo printer = new PrinterInfo.Builder(printerId, "Printer 2")
                .setStatus(PrinterInfo.STATUS_READY)
                .setMinMargins(new Margins(0, 0, 0, 0), new Margins(0, 0, 0, 0))
                .addMediaSize(PrintAttributes.MediaSize.ISO_A4, true)
                .addMediaSize(PrintAttributes.MediaSize.ISO_A5, false)
                .addResolution(new Resolution("R1", getPackageName(),
                        R.string.resolution_200x200, 200, 200), true)
                .addResolution(new Resolution("R2", getPackageName(),
                        R.string.resolution_300x300, 300, 300), false)
                .addInputTray(new Tray("FirstInputTray", getPackageName(),
                        R.string.input_tray_first), false)
                .addInputTray(new Tray("SecondInputTray", getPackageName(),
                        R.string.input_tray_second), true)
                .addOutputTray(new Tray("FirstOutputTray", getPackageName(),
                        R.string.output_tray_first), false)
                .addOutputTray(new Tray("SecondOutputTray",  getPackageName(),
                        R.string.output_tray_second), true)
                .setDuplexModes(PrintAttributes.DUPLEX_MODE_NONE
                        | PrintAttributes.DUPLEX_MODE_LONG_EDGE
                        | PrintAttributes.DUPLEX_MODE_SHORT_EDGE,
                        PrintAttributes.DUPLEX_MODE_SHORT_EDGE)
                .setColorModes(PrintAttributes.COLOR_MODE_COLOR
                        | PrintAttributes.COLOR_MODE_MONOCHROME,
                        PrintAttributes.COLOR_MODE_MONOCHROME)
                .setFittingModes(PrintAttributes.FITTING_MODE_FIT_TO_PAGE
                        | PrintAttributes.FITTING_MODE_NONE,
                        PrintAttributes.FITTING_MODE_FIT_TO_PAGE)
                .setOrientations(PrintAttributes.ORIENTATION_PORTRAIT
                        | PrintAttributes.ORIENTATION_LANDSCAPE,
                        PrintAttributes.ORIENTATION_LANDSCAPE)
                .create();
        List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
        printers.add(printer);
        addPrinters(printers);
    }

    private final class MyHandler extends Handler {

        public static final int MESSAGE_ADD_FIRST_FAKE_PRINTER = 1;
        public static final int MESSAGE_ADD_SECOND_FAKE_PRINTER = 2;

        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_ADD_FIRST_FAKE_PRINTER: {
                    addFirstFakePrinter();
                } break;
                case MESSAGE_ADD_SECOND_FAKE_PRINTER: {
                    addSecondFakePrinter();
                } break;
            }
        }
    }
}
