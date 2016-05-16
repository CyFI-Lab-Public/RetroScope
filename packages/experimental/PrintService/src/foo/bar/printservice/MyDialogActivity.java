package foo.bar.printservice;

import android.app.ListActivity;
import android.os.Bundle;
import android.print.PrintJobId;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

public class MyDialogActivity extends ListActivity {

    private static final int ITEM_INDEX_PRINT_NOW = 0;
    private static final int ITEM_INDEX_PRINT_DELAYED = 1;
    private static final int ITEM_INDEX_FAIL_NOW = 2;
    private static final int ITEM_INDEX_FAIL_DELAYED = 3;
    private static final int ITEM_INDEX_BLOCK_NOW = 4;
    private static final int ITEM_INDEX_BLOCK_DELAYED = 5;
    private static final int ITEM_INDEX_BLOCK_AND_DELAYED_UNBLOCK = 6;

    private static final int ITEM_INDEX_CANCEL_YES = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrintJobId printJobId = getIntent().getParcelableExtra(
                MyPrintService.INTENT_EXTRA_PRINT_JOB_ID);
        final int actionType = getIntent().getIntExtra(MyPrintService.INTENT_EXTRA_ACTION_TYPE,
                MyPrintService.ACTION_TYPE_ON_PRINT_JOB_PENDING);

        if (actionType == MyPrintService.ACTION_TYPE_ON_PRINT_JOB_PENDING) {
            createActionTypeOnPrintJobPendingUi(printJobId);
        } else {
            createActionTypeOnReqeustCancelPrintJobUi(printJobId);
        }
    }

    private void createActionTypeOnPrintJobPendingUi(final PrintJobId printJobId) {
        setTitle(getString(R.string.on_print_job_pending_activity_title));

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.on_print_job_queued_actions)));
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case ITEM_INDEX_PRINT_NOW: {
                        MyPrintService.peekInstance().handleQueuedPrintJob(printJobId);
                        finish();
                    } break;

                    case ITEM_INDEX_PRINT_DELAYED: {
                        MyPrintService.peekInstance().handleQueuedPrintJobDelayed(printJobId);
                        finish();
                    } break;

                    case ITEM_INDEX_FAIL_NOW: {
                        MyPrintService.peekInstance().handleFailPrintJob(printJobId);
                        finish();
                    } break;

                    case ITEM_INDEX_FAIL_DELAYED: {
                        MyPrintService.peekInstance().handleFailPrintJobDelayed(printJobId);
                        finish();
                    } break;

                    case ITEM_INDEX_BLOCK_NOW: {
                        MyPrintService.peekInstance().handleBlockPrintJob(printJobId);
                        finish();
                    } break;

                    case ITEM_INDEX_BLOCK_DELAYED: {
                        MyPrintService.peekInstance().handleBlockPrintJobDelayed(printJobId);
                        finish();
                    } break;

                    case ITEM_INDEX_BLOCK_AND_DELAYED_UNBLOCK: {
                        MyPrintService.peekInstance().handleBlockAndDelayedUnblockPrintJob(
                                printJobId);
                        finish();
                    } break;

                    default: {
                        finish();
                    } break;
                }
            }
        });
    }

    private void createActionTypeOnReqeustCancelPrintJobUi(final PrintJobId printJobId) {
        setTitle(getString(R.string.on_cancle_print_job_requested_activity_title));

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.on_request_cancel_print_job_actions)));
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case ITEM_INDEX_CANCEL_YES: {
                        MyPrintService.peekInstance().handleRequestCancelPrintJob(printJobId);
                        finish();
                    } break;

                    default: {
                        finish();
                    } break;
                }
            }
        });
    }
}
