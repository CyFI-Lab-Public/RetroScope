package foo.bar.printservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintAttributes.MediaSize;
import android.print.PrintJobInfo;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterInfo;
import android.printservice.PrintService;

public class CustomPrintOptionsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        PrintJobInfo printJobInfo = (PrintJobInfo) getIntent().getParcelableExtra(
                PrintService.EXTRA_PRINT_JOB_INFO);
        PrinterInfo printerInfo = (PrinterInfo) getIntent().getParcelableExtra(
                "android.intent.extra.print.EXTRA_PRINTER_INFO");

        PrinterCapabilitiesInfo capabilities = printerInfo.getCapabilities();

        PrintAttributes attributes = new PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .setMediaSize(MediaSize.ISO_A5)
                .setResolution(capabilities.getResolutions().get(0))
                .build();

        PrintJobInfo.Builder builder = new PrintJobInfo.Builder(printJobInfo);
        builder.setAttributes(attributes);
        builder.setCopies(2);
        builder.setAttributes(attributes);
        builder.setPages(new PageRange[] {new PageRange(1, 1), new PageRange(3, 3)});
        builder.putAdvancedOption("EXTRA_FIRST_ADVANCED_OPTION", "OPALA");
        builder.putAdvancedOption("EXTRA_SECOND_ADVANCED_OPTION", 1);

        PrintJobInfo newPrintJobInfo = builder.build();

        Intent result = new Intent();
        result.putExtra(PrintService.EXTRA_PRINT_JOB_INFO, newPrintJobInfo);
        setResult(Activity.RESULT_OK, result);

        finish();
    }
}
