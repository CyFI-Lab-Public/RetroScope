package com.xtremelabs.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.view.View;
import android.widget.AbsSpinner;
import android.widget.SpinnerAdapter;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(AbsSpinner.class)
public class ShadowAbsSpinner extends ShadowAdapterView {

	private boolean animatedTransition;

	@Implementation
    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter);
    }

    @Override @Implementation
    public SpinnerAdapter getAdapter() {
        return (SpinnerAdapter) super.getAdapter();
    }

    @Implementation
    public void setSelection(int position, boolean animate) {
    	super.setSelection(position);
    	animatedTransition = animate;
    }

    @Implementation
    public View getSelectedView() {
        int selectedItemPosition = getSelectedItemPosition();
        if (getCount() == 0 || selectedItemPosition < 0) {
            return null;
        } else {
            return getChildAt(selectedItemPosition);
        }
    }

    // Non-implementation helper method
    public boolean isAnimatedTransition() {
    	return animatedTransition;
    }
}
