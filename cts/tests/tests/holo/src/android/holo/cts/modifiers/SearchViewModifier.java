/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.holo.cts.modifiers;

import com.android.cts.holo.R;

import android.content.Context;
import android.view.View;
import android.widget.SearchView;

public class SearchViewModifier extends AbstractLayoutModifier {

    public static final int QUERY_HINT = 0;
    public static final int QUERY = 1;

    private int mSearchViewType;

    public SearchViewModifier(int searchViewType) {
        mSearchViewType = searchViewType;
    }

    @Override
    public View modifyView(View view) {
        SearchView searchView = (SearchView) view;
        Context context = view.getContext();

        switch (mSearchViewType) {
            case QUERY_HINT:
                searchView.setQueryHint(context.getString(R.string.searchview_query_hint));
                break;

            case QUERY:
                searchView.setQuery(context.getString(R.string.searchview_query), false);
                break;

            default:
                throw new IllegalArgumentException("Bad search view type: " + mSearchViewType);
        }

        searchView.setIconifiedByDefault(false);
        return searchView;
    }
}
