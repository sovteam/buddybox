package buddybox.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.adalbertosoares.buddybox.R;

import buddybox.core.IModel;
import buddybox.core.State;
import buddybox.core.events.Search;

import static buddybox.core.Dispatcher.dispatch;

public class SearchFragment extends Fragment {

    private IModel.StateListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private EditText searchText;
    private PlayablesFragment resultsFragment;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        searchText = view.findViewById(R.id.searchText);
        searchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Context context = getContext();
                if (!b && context != null) {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
                }
            }
        });

        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchText = charSequence.toString();
                resultsFragment.setTextToHighlight(searchText);
                dispatch(new Search(searchText));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        view.findViewById(R.id.clearSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchText.setText("");
            }
        });

        resultsFragment = new PlayablesFragment();
        Activity activity = getActivity();
        if (activity != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.resultsContainer, resultsFragment)
                    .commit();
        }

        // add state listener
        // TODO add listener when MainActivity.navigateTo becomes an event
        /*listener = new IModel.StateListener() { @Override public void update(final State state) {
            Runnable runUpdate = new Runnable() {
                @Override
                public void run() {
                    updateState(state);
                }
            };
            handler.post(runUpdate);
        }};
        ModelProxy.addStateListener(listener);*/

        return view;
    }

    public void updateState(State state) {
        if (resultsFragment != null)
            resultsFragment.update(state.searchResults, state);

        Context context = getContext();
        if (context == null)
            return;

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null)
            return;

        if (state.searchResults.isEmpty()) {
            searchText.requestFocus();
            imm.showSoftInput(searchText, 0);
            System.out.println("SearchFrag updateState");
        }
    }
}
