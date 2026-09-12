package com.tabbit.breathe478;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 4-7-8 呼吸法动画视图。
 * 呼吸主体使用抠图小猫（透明背景，丝滑边缘），随呼吸节奏缩放，并按阶段切换神情图：
 *  - 吸气：cat_inhale（气流吸入）
 *  - 屏息：cat_hold（鼓腮憋气）
 *  - 呼气：cat_exhale（张嘴出气）
 * 每个呼吸阶段结束时播放提示音。
 */
public class BreathingView extends View implements Runnable {

    public interface Listener {
        void onState(boolean running, boolean started, int cycles, int target);
        void onFinished(int cycles);
    }

    private static final String[] PHASES = {"吸气", "屏息", "呼气"};
    private static final long[] DURATIONS = {4000L, 7000L, 8000L};
    private static final float MIN_SCALE = 0.80f;
    private static final float MAX_SCALE = 1.0f;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint catPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint phasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF dst = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Bitmap bmpInhale, bmpHold, bmpExhale;

    private boolean running = false;
    private boolean started = false;
    private boolean finished = false;
    private int phase = 0;
    private int cycles = 0;
    private int target = 0;
    private long phaseStart = 0L;
    private long pausedElapsed = 0L;
    private float scale = MIN_SCALE;
    private int shownSeconds = -1;

    private Listener listener;
    private Vibrator vibrator;
    private ToneGenerator toneGen;

    public BreathingView(Context c) {
        super(c);
        init(c);
    }

    public BreathingView(Context c, AttributeSet a) {
        super(c, a);
        init(c);
    }

    private void init(Context c) {
        bmpInhale = BitmapFactory.decodeResource(getResources(), R.drawable.cat_inhale);
        bmpHold = BitmapFactory.decodeResource(getResources(), R.drawable.cat_hold);
        bmpExhale = BitmapFactory.decodeResource(getResources(), R.drawable.cat_exhale);

        cloudPaint.setColor(Color.WHITE);

        phasePaint.setColor(Color.parseColor("#123A5A"));
        phasePaint.setTextAlign(Paint.Align.CENTER);
        phasePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        secPaint.setColor(Color.parseColor("#2E5E86"));
        secPaint.setTextAlign(Paint.Align.CENTER);

        hintPaint.setColor(Color.parseColor("#4B6B88"));
        hintPaint.setTextAlign(Paint.Align.CENTER);

        vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        ensureTone();
    }

    private void ensureTone() {
        if (toneGen == null) {
            try {
                toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
            } catch (Exception e) {
                toneGen = null;
            }
        }
    }

    private void releaseTone() {
        if (toneGen != null) {
            try {
                toneGen.release();
            } catch (Exception ignored) {
            }
            toneGen = null;
        }
    }

    /** 阶段结束时发声：吸气结束→Beep，屏息结束→Ack，呼气结束→Beep2。 */
    private void playCue(int endedPhase) {
        ensureTone();
        if (toneGen == null) return;
        int tone;
        if (endedPhase == 0) {
            tone = ToneGenerator.TONE_PROP_BEEP;
        } else if (endedPhase == 1) {
            tone = ToneGenerator.TONE_PROP_ACK;
        } else {
            tone = ToneGenerator.TONE_PROP_BEEP2;
        }
        try {
            toneGen.startTone(tone, 180);
        } catch (Exception ignored) {
        }
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public boolean isStarted() { return started; }
    public boolean isRunning() { return running; }
    public int getCycles() { return cycles; }

    public void toggle() {
        if (running) pause(); else start(target);
    }

    public void start(int count) {
        long now = SystemClock.elapsedRealtime();
        if (!started || finished) {
            started = true;
            finished = false;
            phase = 0;
            cycles = 0;
            pausedElapsed = 0L;
            phaseStart = now;
            target = count;
        } else {
            phaseStart = now - pausedElapsed;
            if (count > 0) target = count;
        }
        running = true;
        ensureTone();
        handler.removeCallbacks(this);
        handler.post(this);
        notifyState();
    }

    public void pause() {
        pausedElapsed = SystemClock.elapsedRealtime() - phaseStart;
        running = false;
        handler.removeCallbacks(this);
        invalidate();
        notifyState();
    }

    public void reset() {
        running = false;
        started = false;
        finished = false;
        phase = 0;
        cycles = 0;
        target = 0;
        pausedElapsed = 0L;
        scale = MIN_SCALE;
        handler.removeCallbacks(this);
        invalidate();
        notifyState();
    }

    public void stopTicker() {
        handler.removeCallbacks(this);
        releaseTone();
    }

    private void notifyState() {
        if (listener != null) listener.onState(running, started, cycles, target);
    }

    @Override
    public void run() {
        update(SystemClock.elapsedRealtime());
        invalidate();
        if (running) handler.postDelayed(this, 33L);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureTone();
        if (running) handler.post(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(this);
        releaseTone();
        super.onDetachedFromWindow();
    }

    private void update(long now) {
        if (!running) return;
        long elapsed = now - phaseStart;
        while (elapsed >= DURATIONS[phase]) {
            elapsed -= DURATIONS[phase];
            phaseStart += DURATIONS[phase];
            int ended = phase;
            phase = (phase + 1) % 3;
            if (phase == 0) {
                cycles++;
                notifyState();
                if (target > 0 && cycles >= target) {
                    playCue(ended);
                    finish();
                    return;
                }
            }
            buzz(40);
            playCue(ended);
        }
        float t = (float) elapsed / DURATIONS[phase];
        shownSeconds = (int) Math.ceil((DURATIONS[phase] - elapsed) / 1000.0);
        if (shownSeconds < 1) shownSeconds = 1;
        if (phase == 0) {
            scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * ease(t);
        } else if (phase == 1) {
            scale = MAX_SCALE;
        } else {
            scale = MAX_SCALE - (MAX_SCALE - MIN_SCALE) * ease(t);
        }
    }

    private void finish() {
        running = false;
        finished = true;
        handler.removeCallbacks(this);
        scale = MIN_SCALE;
        invalidate();
        notifyState();
        if (listener != null) listener.onFinished(cycles);
    }

    private float ease(float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t * t * (3f - 2f * t);
    }

    private void buzz(int ms) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            toggle();
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h * 0.40f;
        float base = Math.min(w, h) * 0.70f;
        float s = Math.max(base * scale, 1f);

        bgPaint.setShader(new LinearGradient(0f, 0f, 0f, h,
                Color.parseColor("#9FCDF5"), Color.parseColor("#EAF6FF"), Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, w, h, bgPaint);

        drawClouds(canvas, w, h);

        glowPaint.setShader(new RadialGradient(cx, cy, s * 0.80f,
                new int[]{0x55FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, s * 0.80f, glowPaint);

        Bitmap bmp = currentBitmap();
        if (bmp != null && !bmp.isRecycled()) {
            float bw = bmp.getWidth();
            float bh = bmp.getHeight();
            float ar = bw / bh;
            float dw, dh;
            if (ar >= 1f) {
                dw = s;
                dh = s / ar;
            } else {
                dh = s;
                dw = s * ar;
            }
            dst.set(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f);
            canvas.drawBitmap(bmp, null, dst, catPaint);
        }

        phasePaint.setTextSize(w * 0.10f);
        String label = finished ? "完成" : PHASES[phase];
        canvas.drawText(label, cx, h * 0.78f, phasePaint);

        secPaint.setTextSize(w * 0.055f);
        String secs = running ? (shownSeconds + " 秒") : (DURATIONS[phase] / 1000 + " 秒");
        canvas.drawText(secs, cx, h * 0.845f, secPaint);

        hintPaint.setTextSize(w * 0.040f);
        String hint;
        if (finished) {
            hint = "太棒了，完成 " + cycles + " 次呼吸！";
        } else if (target > 0) {
            hint = "目标 " + target + " 次 · 已完成 " + cycles + " 次";
        } else {
            hint = "已完成 " + cycles + " 个循环";
        }
        canvas.drawText(hint, cx, h * 0.915f, hintPaint);
    }

    private Bitmap currentBitmap() {
        if (finished) return bmpInhale;
        if (phase == 1) return bmpHold;
        if (phase == 2) return bmpExhale;
        return bmpInhale;
    }

    private void drawClouds(Canvas c, int w, int h) {
        cloudPaint.setAlpha(210);
        drawCloud(c, w * 0.20f, h * 0.12f, w * 0.16f);
        drawCloud(c, w * 0.82f, h * 0.09f, w * 0.12f);
        drawCloud(c, w * 0.60f, h * 0.08f, w * 0.08f);
    }

    private void drawCloud(Canvas c, float x, float y, float r) {
        c.drawCircle(x, y, r * 0.6f, cloudPaint);
        c.drawCircle(x - r * 0.5f, y + r * 0.1f, r * 0.45f, cloudPaint);
        c.drawCircle(x + r * 0.5f, y + r * 0.1f, r * 0.45f, cloudPaint);
        c.drawCircle(x, y + r * 0.26f, r * 0.55f, cloudPaint);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
