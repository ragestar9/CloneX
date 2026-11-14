package top.niunaijun.blackboxa.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import top.niunaijun.blackboxa.R

/**
 * Animated Zenith OS background: a scrolling grid, a slow radial pulse and a
 * dusting of digital noise, in cyan over obsidian with a purple undertone.
 *
 * Ported from the WebGL fragment shader in the design export. AGSL differs from
 * GLSL in a few ways that matter here: the entry point returns half4 rather than
 * assigning gl_FragColor, and fragCoord arrives in pixels rather than as a
 * normalised varying, so the resolution divide is done here.
 *
 * RuntimeShader needs API 33. Below that the view falls back to a static
 * gradient drawable which approximates the same composition. This is a runtime
 * check on SDK_INT, not targetSdk - targetSdk must stay at 28 or the
 * virtualization engine breaks.
 */
class ZenithShaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private var shader: RuntimeShader? = null
    private val paint = Paint()

    private var startNanos = 0L
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        if (supported) {
            @SuppressLint("NewApi")
            shader = RuntimeShader(AGSL)
        } else {
            setBackgroundResource(R.drawable.zenith_boot_background)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    /**
     * Stop burning GPU cycles the moment the view leaves the screen. This is a
     * utility people open for a few seconds, so a background animating while
     * nobody is looking would be a pure battery cost.
     */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) start() else stop()
    }

    private fun start() {
        if (!supported || running) return
        if (startNanos == 0L) startNanos = System.nanoTime()
        running = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stop() {
        if (!running) return
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val runtimeShader = shader ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val elapsed = (System.nanoTime() - startNanos) / 1_000_000_000f
        runtimeShader.setFloatUniform("uResolution", width.toFloat(), height.toFloat())
        runtimeShader.setFloatUniform("uTime", elapsed)
        paint.shader = runtimeShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private companion object {

        /**
         * The grid is aspect-corrected on x. The original ran in a landscape
         * viewport where uv * 10 gave roughly square cells; on a portrait phone
         * the same maths stretches them into tall rectangles.
         */
        const val AGSL = """
            uniform float2 uResolution;
            uniform float uTime;

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / uResolution;
                float time = uTime * 0.5;
                float aspect = uResolution.x / uResolution.y;

                float2 gridUv = float2(uv.x * 10.0 * aspect, uv.y * 10.0 + time);
                float2 grid = fract(gridUv);
                float gridLine = smoothstep(0.02, 0.0, grid.x) + smoothstep(0.02, 0.0, grid.y);

                float dist = distance(uv, float2(0.5, 0.42));
                float pulse = 0.5 + 0.5 * sin(time * 2.0);
                float glow = smoothstep(0.6, 0.0, dist) * pulse;

                float noise = fract(sin(dot(uv + time, float2(12.9898, 78.233))) * 43758.5453);

                half3 obsidian = half3(0.0, 0.05, 0.1);
                half3 cyan     = half3(0.0, 0.95, 1.0);
                half3 purple   = half3(0.5, 0.0, 1.0);

                half3 color = mix(obsidian, cyan, half(glow) * 0.30);
                color += cyan * half(gridLine) * 0.12;
                color += purple * half(noise) * 0.02;

                return half4(color, 1.0);
            }
        """
    }
}
