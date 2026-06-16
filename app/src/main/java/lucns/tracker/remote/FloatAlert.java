package lucns.tracker.remote;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import lucns.tracker.R;
import lucns.tracker.services.MainService;

public class FloatAlert extends SuspendedWindow {

    private final MainService mainService;
    private boolean isHiding;

    public FloatAlert(Context context, MainService mainService) {
        super(context, false);
        this.mainService = mainService;
        setContentView(R.layout.dialog_red_alert);
        //setAlpha(0.9f);
        //int widthDisplay = Resources.getSystem().getDisplayMetrics().widthPixels;
        //int minWidth = widthDisplay - (widthDisplay / 10);
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 330, getResources().getDisplayMetrics());
        setSize(px, WindowManager.LayoutParams.WRAP_CONTENT);
        lock(true, true);

        View.OnClickListener onClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startHide();
                Log.d("Lucas", "click");
            }
        };
        findViewById(R.id.buttonIgnore).setOnClickListener(onClick);
        findViewById(R.id.buttonOpenApp).setOnClickListener(onClick);
    }

    public void show() {
        setAlpha(0f);
        super.show();
        ObjectAnimator o = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f).setDuration(300);
        o.setInterpolator(new DecelerateInterpolator());
        o.start();

        // if (!mainService.isConnected() && !mainService.isConnecting()) mainService.connect();
        //setYPosition(getNavigationBarHeight() * (-1));
        //applyLayoutParameters();
    }

    public void startHide() {
        if (isHiding || !isShowing()) return;
        isHiding = true;
        ObjectAnimator o = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f).setDuration(300);
        o.setInterpolator(new AccelerateInterpolator());
        o.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hide();
                isHiding = false;
            }
        });
        o.start();
    }
}
