package foo.bar.testback;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

public class TestBackService extends AccessibilityService {

	private static final String LOG_TAG = TestBackService.class.getSimpleName();

	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AccessibilityManager am = (AccessibilityManager)
                    getSystemService(ACCESSIBILITY_SERVICE);
            Log.i(LOG_TAG, "accessibilityEnabled: " + am.isEnabled()
                    + " touchExplorationEnabled: " + am.isTouchExplorationEnabled());
//            AccessibilityServiceInfo info = getServiceInfo();
//            if ((info.flags & AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE) == 0) {
//                info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
//            } else {
//                info.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
//            }
//            setServiceInfo(info);
            sendEmptyMessageDelayed(0, 20000);
        }
	};

    @Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
//		Log.i(LOG_TAG, AccessibilityEvent.eventTypeToString(event.getEventType()));
	}

	@Override
    protected boolean onGesture(int gestureId) {
	    switch (gestureId) {
	        case AccessibilityService.GESTURE_SWIPE_DOWN: {
	            dumpIdResNames(getRootInActiveWindow());
	        } break;
	        // COPY
	        case AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT: {
	            AccessibilityNodeInfo root = getRootInActiveWindow();
	            if (root != null) {
	                AccessibilityNodeInfo focus = root.findFocus(
	                        AccessibilityNodeInfo.FOCUS_INPUT);
	                if (focus != null) {
	                    if ((focus.getActions() & 0x00004000) /* COPY*/ != 0) {
	                        final boolean performed = focus.performAction(0x00004000);
	                        Log.i(LOG_TAG, "Performed: " + performed);
	                    }
	                    focus.recycle();
	                }
	                root.recycle();
	            }
	        } break;
	        // PASTE
            case AccessibilityService.GESTURE_SWIPE_UP: {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    AccessibilityNodeInfo focus = root.findFocus(
                            AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focus != null) {
                        if ((focus.getActions() & 0x00008000) /* PASTE*/ != 0) {
                            final boolean performed = focus.performAction(0x00008000);
                            Log.i(LOG_TAG, "Performed: " + performed);
                        }
                        focus.recycle();
                    }
                    root.recycle();
                }
            } break;
            // CUT
            case AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT: {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    AccessibilityNodeInfo focus = root.findFocus(
                            AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focus != null) {
                        if ((focus.getActions() & 0x00010000) /* CUT*/ != 0) {
                            final boolean performed = focus.performAction(0x00010000);
                            Log.i(LOG_TAG, "Performed: " + performed);
                        }
                        focus.recycle();
                    }
                    root.recycle();
                }
            } break;
            // SELECT_ALL
            case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP: {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    AccessibilityNodeInfo focus = root.findFocus(
                            AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focus != null) {
                        if ((focus.getActions() & 0x00020000) /* SELECT ALL*/ != 0) {
                            final boolean performed = focus.performAction(0x00020000);
                            Log.i(LOG_TAG, "Performed: " + performed);
                        }
                        focus.recycle();
                    }
                    root.recycle();
                }
            } break;
	    }
        return super.onGesture(gestureId);
    }

	@Override
    public boolean onKeyEvent(KeyEvent event) {
        Log.i(LOG_TAG, "onKeyEvent: " + event);
        boolean result = false; 
        return result;
    }

    private void dumpIdResNames(AccessibilityNodeInfo root) {
	    if (root == null) {
	        return;
	    }
	    if (root.getViewIdResourceName() != null) {
	        Log.i(LOG_TAG, root.getViewIdResourceName().toString());
	    }
	    final int childCount = root.getChildCount();
	    for (int i = 0; i < childCount; i++) {
	        dumpIdResNames(root.getChild(i));
	    }
	}

    @Override
	public void onInterrupt() {
        /* ignore */
	}

    @Override
    public void onServiceConnected() {
//        AccessibilityServiceInfo info = getServiceInfo();
//        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
//        setServiceInfo(info);
    }
}
