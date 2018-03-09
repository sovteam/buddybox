package buddybox.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import buddybox.core.IModel;
import buddybox.core.State;

public class ArtistActivity extends AppCompatActivity {

    private IModel.StateListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_activity);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // set listener
        listener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        ModelProxy.addStateListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ModelProxy.removeStateListener(listener);
    }


    private void updateState(State state) {
        if (state.artistSelected == null) {
            finish();
            return;
        }

        ((ImageView)findViewById(R.id.picture)).setImageBitmap(state.artistSelected.picture);
        ((TextView)findViewById(R.id.name)).setText(state.artistSelected.name);
        ((TextView)findViewById(R.id.songsCount)).setText(state.artistSelected.songsCountPrint());

    }
}
