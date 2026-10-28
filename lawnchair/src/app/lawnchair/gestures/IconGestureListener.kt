package app.lawnchair.gestures

import android.util.Log
import android.os.Process
import android.content.ComponentName
import android.view.ViewConfiguration
import androidx.datastore.preferences.core.Preferences
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.lifecycleScope
import app.lawnchair.animation.PhysicsAnimator
import app.lawnchair.gestures.config.GestureHandlerConfig
import app.lawnchair.gestures.type.GestureType
import app.lawnchair.launcher
import app.lawnchair.nexo.NexoIconGesture
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.BubbleTextView
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.util.VibratorWrapper
import kotlin.math.abs
import kotlinx.coroutines.launch

class IconGestureListener(
    private val view: BubbleTextView,
    private val prefs: PreferenceManager2,
    private val componentKey: ComponentKey?,
) : DirectionalGestureListener(view.context) {

    private val context get() = view.context

    // The NX08B mapping is for user-installed primary-profile launcher apps.
    // Keep work-profile/managed-user shortcuts on Lawnchair's existing engine.
    private fun nexoSourceComponent(): ComponentName? =
        componentKey?.takeIf { it.user == Process.myUserHandle() }?.componentName

    private fun hasNexoSwipeUp(): Boolean =
        nexoSourceComponent()?.let { NexoIconGesture.hasMappedSwipeUp(context, it) } == true

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var scrollLock = ScrollLock.NONE
    private var isAnimating = false
    private var cachedPreferences: Preferences? = null
    private var configuredGestures = emptySet<GestureType>()

    /** Represents the axis on which the current scroll gesture is locked.
     */
    private enum class ScrollLock { NONE, HORIZONTAL, VERTICAL }

    override fun onSwipeRight(velocity: Float) = handleGesture(GestureType.SWIPE_RIGHT, velocity)
    override fun onSwipeLeft(velocity: Float) = handleGesture(GestureType.SWIPE_LEFT, velocity)
    override fun onSwipeTop(velocity: Float) = handleGesture(GestureType.SWIPE_UP, velocity)
    override fun onSwipeDown(velocity: Float) = handleGesture(GestureType.SWIPE_DOWN, velocity)

    override fun onScroll(diffX: Float, diffY: Float) {
        if (scrollLock == ScrollLock.NONE) {
            val absX = abs(diffX)
            val absY = abs(diffY)
            if (absX > touchSlop || absY > touchSlop) {
                if (isAnimating || view.translationX != 0f || view.translationY != 0f) {
                    PhysicsAnimator.getInstance(view).cancel()
                    isAnimating = false
                }
                scrollLock = if (absX > absY) ScrollLock.HORIZONTAL else ScrollLock.VERTICAL
            } else {
                return
            }
        }

        // Apply translation only to the locked axis
        if (scrollLock == ScrollLock.HORIZONTAL) {
            val gestureType = if (diffX > 0) GestureType.SWIPE_RIGHT else GestureType.SWIPE_LEFT
            view.translationX = if (hasGestureConfigured(gestureType)) diffX * TRANSLATION_FACTOR else 0f
            view.translationY = 0f
        } else {
            val gestureType = if (diffY > 0) GestureType.SWIPE_DOWN else GestureType.SWIPE_UP
            view.translationY = if (hasGestureConfigured(gestureType)) diffY * TRANSLATION_FACTOR else 0f
            view.translationX = 0f
        }
    }

    override fun onActionUp(handled: Boolean) {
        scrollLock = ScrollLock.NONE
        if (!handled && (view.translationX != 0f || view.translationY != 0f)) {
            isAnimating = true
            PhysicsAnimator.getInstance(view)
                .spring(DynamicAnimation.TRANSLATION_X, 0f, 0f, SpringForce.STIFFNESS_HIGH, SpringForce.DAMPING_RATIO_NO_BOUNCY)
                .spring(DynamicAnimation.TRANSLATION_Y, 0f, 0f, SpringForce.STIFFNESS_HIGH, SpringForce.DAMPING_RATIO_NO_BOUNCY)
                .start()
        }
    }

    /** Check if there's any gesture configured for this entry */
    fun hasAnyGestureConfigured(): Boolean {
        // BubbleTextView uses this as its ACTION_DOWN gate. Without NEXO here,
        // regular HOME/dock icons ignore gestures configured only through NEXO.
        return getConfiguredGestures().isNotEmpty() || hasNexoSwipeUp()
    }

    /** Check if there's a horizontal gesture configured for this entry. (Swipe left/right) */
    fun hasHorizontalGestureConfigured(): Boolean {
        return hasGestureConfigured(GestureType.SWIPE_LEFT) ||
            hasGestureConfigured(GestureType.SWIPE_RIGHT)
    }

    /** Check if there's a vertical gesture configured for this entry. (Swipe up/down) */
    fun hasVerticalGestureConfigured(): Boolean {
        return hasGestureConfigured(GestureType.SWIPE_UP) ||
            hasGestureConfigured(GestureType.SWIPE_DOWN)
    }

    /** Check if there's a specific gesture configured for this entry
     * @param gestureType The type of gesture to check */
    private fun hasGestureConfigured(gestureType: GestureType): Boolean {
        // Do not cache NEXO's active-space choice with Lawnchair's DataStore.
        // Changing space must immediately affect icons already on screen.
        return gestureType in getConfiguredGestures() ||
            (gestureType == GestureType.SWIPE_UP && hasNexoSwipeUp())
    }

    /**
     * Refreshes this listener's gesture cache whenever DataStore publishes a new preferences
     * snapshot. This keeps an existing icon in sync without parsing its JSON configuration on
     * every scroll event.
     */
    private fun getConfiguredGestures(): Set<GestureType> {
        val preferences = prefs.getCachedPreferences()
        if (preferences !== cachedPreferences) {
            cachedPreferences = preferences
            configuredGestures = GestureType.entries.filter { resolveGesture(it) != null }.toSet()
        }
        return configuredGestures
    }

    /** Launch gesture configured operation for a specific gesture type
     * @param gestureType The type of gesture that triggered the event */
    private fun handleGesture(gestureType: GestureType, velocity: Float): Boolean {
        val nativeGesture = resolveGesture(gestureType)
        val nexoComponent = if (nativeGesture == null && gestureType == GestureType.SWIPE_UP &&
            hasNexoSwipeUp()
        ) nexoSourceComponent() else null
        if (nativeGesture == null && nexoComponent == null) return false

        Log.d("GESTURE_HANDLER", "Handling gesture: ${gestureType.name}; NEXO=${nexoComponent != null}")
        animateIconSwipe(gestureType, velocity)

        context.launcher.lifecycleScope.launch {
            VibratorWrapper.INSTANCE.get(context.launcher).vibrate(VibratorWrapper.OVERVIEW_HAPTIC)
            if (nativeGesture != null) {
                // Respect any explicit existing Lawnchair assignment first.
                nativeGesture.createHandler(context).onTrigger(context.launcher)
            } else if (nexoComponent != null) {
                NexoIconGesture.launchMappedSwipeUp(context.launcher, nexoComponent)
            }
        }
        return true
    }

    /** Animates a spring-back effect on the icon following a swipe gesture.
     */
    private fun animateIconSwipe(gestureType: GestureType, velocity: Float) {
        // We spring back to 0, but use the swipe velocity to "overshoot" naturally,
        // then dampen the velocity slightly so the icon doesn't fly off screen
        val (velX, velY) = when (gestureType) {
            GestureType.SWIPE_RIGHT, GestureType.SWIPE_LEFT -> (velocity * VELOCITY_FACTOR) to 0f
            GestureType.SWIPE_UP, GestureType.SWIPE_DOWN -> 0f to (velocity * VELOCITY_FACTOR)
        }

        PhysicsAnimator.getInstance(view)
            .spring(DynamicAnimation.TRANSLATION_X, 0f, velX, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_NO_BOUNCY)
            .spring(DynamicAnimation.TRANSLATION_Y, 0f, velY, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_NO_BOUNCY)
            .start()
    }

    /** Get gesture handler for that app's component
     * @param gestureType The type of gesture to resolve for the current component */
    private fun resolveGesture(gestureType: GestureType): GestureHandlerConfig? {
        val currentComponentKey = componentKey ?: return null
        val gesture = prefs.getGestureForAppCached(currentComponentKey, gestureType)
        return gesture.takeUnless { it is GestureHandlerConfig.NoOp }
    }
}
