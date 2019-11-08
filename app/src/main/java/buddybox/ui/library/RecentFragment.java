package buddybox.ui.library;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import sov.buddybox.R;

import buddybox.core.IModel;
import buddybox.core.State;
import buddybox.ui.ModelProxy;
import buddybox.ui.PlayablesFragment;

public class RecentFragment extends Fragment {

    private RelativeLayout view;
    private IModel.StateListener listener;
    private FragmentActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PlayablesFragment playablesFragment;

    public RecentFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = (RelativeLayout) inflater.inflate(R.layout.library_recent, container, false);

        playablesFragment = new PlayablesFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.recentContainer, playablesFragment)
                .commit();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        listener = new IModel.StateListener() { @Override public void update(final State state) {
            Runnable runUpdate = new Runnable() {
                @Override
                public void run() {
                    updateState(state);
                }
            };
            handler.post(runUpdate);
        }};
        ModelProxy.addStateListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ModelProxy.removeStateListener(listener);
    }

    public void updateState(State state) {
        playablesFragment.update(state.recent, state);

        if (state.syncLibraryPending) {
            view.findViewById(R.id.library_empty).setVisibility(View.GONE);
        } else {
            if (state.recent.isEmpty()) {
                view.findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
                return;
            }
        }
        view.findViewById(R.id.library_empty).setVisibility(View.GONE);
    }
}
