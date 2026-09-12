package com.tabbit.breathe478;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
        implements BreathingView.Listener, View.OnClickListener, DialogInterface.OnClickListener {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private BreathingView view;
    private Button mainButton;
    private TextView statusText;
    private EditText customInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#EAF4FF"));
        root.setPadding(dp(16), dp(16), dp(16), dp(8));

        TextView title = new TextView(this);
        title.setText("4 · 7 · 8 呼吸放松");
        title.setTextColor(Color.parseColor("#14415F"));
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("跟着小猫一起慢慢呼吸");
        subtitle.setTextColor(Color.parseColor("#5B7A96"));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        subLp.gravity = Gravity.CENTER_HORIZONTAL;
        subLp.bottomMargin = dp(4);
        root.addView(subtitle, subLp);

        view = new BreathingView(this);
        view.setListener(this);
        root.addView(view, new LinearLayout.LayoutParams(MATCH, 0, 1.5f));

        statusText = new TextView(this);
        statusText.setTextColor(Color.parseColor("#4A6A86"));
        statusText.setTextSize(13);
        statusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        stLp.bottomMargin = dp(8);
        root.addView(statusText, stLp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        mainButton = makeButton("开始", "#1E88E5");
        mainButton.setOnClickListener(this);
        Button resetButton = makeButton("重置", "#607D8B");
        resetButton.setOnClickListener(this);
        row1.addView(mainButton, rowParams(true));
        row1.addView(resetButton, rowParams(false));
        root.addView(row1, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        row2Lp.topMargin = dp(8);
        Button threeButton = makeButton("连续呼吸 3 次", "#26A69A");
        threeButton.setOnClickListener(this);
        Button customButton = makeButton("自定义次数", "#7E57C2");
        customButton.setOnClickListener(this);
        row2.addView(threeButton, rowParams(true));
        row2.addView(customButton, rowParams(false));
        root.addView(row2, row2Lp);

        TextView guideTitle = new TextView(this);
        guideTitle.setText("呼吸小课堂");
        guideTitle.setTextColor(Color.parseColor("#14415F"));
        guideTitle.setTextSize(16);
        guideTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams gtLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        gtLp.topMargin = dp(16);
        gtLp.bottomMargin = dp(8);
        root.addView(guideTitle, gtLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        cards.setPadding(0, 0, 0, dp(8));

        cards.addView(makeCard("什么是 4-7-8 呼吸法？",
                "4-7-8 是一种经典的放松呼吸技巧：先吸气 4 秒，再屏息 7 秒，最后缓缓呼气 8 秒。它通过延长呼气时间帮助身体放松，让人更容易平静下来。"));
        cards.addView(makeCard("具体怎么做？",
                "1. 用鼻子安静地吸气，默数 4 秒；\n2. 轻轻屏住呼吸，默数 7 秒；\n3. 用嘴缓缓呼气，默数 8 秒；\n4. 以上为一个循环，可重复练习。"));
        cards.addView(makeCard("什么时候练合适？",
                "睡前放松、感到紧张焦虑时、工作间隙想放空片刻，或冥想静心前，都可以做几组 4-7-8 呼吸。"));
        cards.addView(makeCard("小贴士",
                "• 呼气要慢而均匀，不要用力；\n• 舌尖轻抵上颚，呼吸更顺畅；\n• 初学者可先练 3～4 次循环，适应后再增加；\n• 感觉头晕时暂停，恢复自然呼吸。"));
        cards.addView(makeCard("温馨提示",
                "本应用仅用于呼吸放松练习辅助，不能替代专业医疗建议。如有呼吸系统或心血管等健康问题，请先咨询医生。"));
        scroll.addView(cards);
        root.addView(scroll, new LinearLayout.LayoutParams(MATCH, 0, 1.0f));

        setContentView(root);
        applyState(false, false, 0, 0);
    }

    @Override
    public void onClick(View v) {
        if (v == mainButton) {
            view.toggle();
        } else if (v instanceof Button) {
            String t = ((Button) v).getText().toString();
            if (t.equals("重置")) {
                view.reset();
            } else if (t.equals("连续呼吸 3 次")) {
                view.start(3);
            } else if (t.equals("自定义次数")) {
                showCustomDialog();
            }
        }
    }

    private void showCustomDialog() {
        customInput = new EditText(this);
        customInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        customInput.setHint("请输入次数（如 5）");
        customInput.setTextColor(Color.parseColor("#222222"));

        new AlertDialog.Builder(this)
                .setTitle("自定义呼吸次数")
                .setMessage("设置要连续完成的循环次数")
                .setView(customInput)
                .setPositiveButton("开始", this)
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int n = 0;
        try {
            n = Integer.parseInt(customInput.getText().toString().trim());
        } catch (Exception ignored) {
        }
        if (n < 1) n = 1;
        if (n > 999) n = 999;
        view.start(n);
    }

    @Override
    public void onState(boolean running, boolean started, int cycles, int target) {
        applyState(running, started, cycles, target);
    }

    @Override
    public void onFinished(int cycles) {
        Toast.makeText(this, "完成 " + cycles + " 次呼吸，真棒！", Toast.LENGTH_LONG).show();
        applyState(false, false, cycles, 0);
    }

    private void applyState(boolean running, boolean started, int cycles, int target) {
        if (mainButton == null) return;
        if (running) {
            mainButton.setText("暂停");
            setButtonColor(mainButton, "#EF5350");
        } else {
            mainButton.setText(started ? "继续" : "开始");
            setButtonColor(mainButton, "#1E88E5");
        }
        if (view != null && view.isRunning()) {
            statusText.setText("正在呼吸中…");
        } else if (target > 0 && cycles < target) {
            statusText.setText("目标 " + target + " 次 · 已完成 " + cycles + " 次");
        } else {
            statusText.setText("已完成 " + cycles + " 次呼吸");
        }
    }

    private LinearLayout.LayoutParams rowParams(boolean left) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        if (left) lp.rightMargin = dp(6); else lp.leftMargin = dp(6);
        return lp;
    }

    private Button makeButton(String text, String colorHex) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(15);
        b.setTextColor(Color.WHITE);
        b.setBackground(makeBg(colorHex, 24));
        return b;
    }

    private void setButtonColor(Button b, String colorHex) {
        b.setBackground(makeBg(colorHex, 24));
    }

    private GradientDrawable makeBg(String colorHex, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(radiusDp));
        bg.setColor(Color.parseColor(colorHex));
        return bg;
    }

    private View makeCard(String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.parseColor("#D3E6F7"));
        card.setBackground(bg);
        int pad = dp(16);
        card.setPadding(pad, pad, pad, pad);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#2E7CC4"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);

        TextView b = new TextView(this);
        b.setText(body);
        b.setTextColor(Color.parseColor("#43566B"));
        b.setTextSize(14);
        b.setLineSpacing(dp(4), 1.1f);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        bLp.topMargin = dp(8);

        card.addView(t);
        card.addView(b, bLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        return card;
    }

    @Override
    protected void onDestroy() {
        if (view != null) view.stopTicker();
        super.onDestroy();
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
