package com.danwolve.own_media_player.extensions

import android.content.res.Resources
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Función encargada de ejecutar animaciones básicas sobre las vistas.
 * Si no se especifica ningun "init" el valor inicial de ese parámetro será el por defecto de la vista.
 * Ningún parámetro es obligatorio.
 * @param duracion la duración de la animación en segundos
 * @param animParams parametros para la animacion en si
 * @param initVisible si quieres que la vista empiece visible
 * @param initAnimParams parametros iniciales de animación
 * @param delayParams para añadirle un delay a la animacion
 * @param interpolator si quieres cambiar el interpolador por defecto [AccelerateDecelerateInterpolator]
 * @param endAction la acción que quieres que se ejecute al acabar la animación
 */
fun <T : View> T.animate(
    duracion: Float = 1F,
    animParams: AnimParams? = null,
    initVisible: Boolean? = null,
    initAnimParams: AnimParams? = null,
    delayParams: DelayParams? = null,
    interpolator: Interpolator = AccelerateDecelerateInterpolator(),
    endAction: ((T) -> Unit)? = null
) {
    if (delayParams != null) {
        delayParams.lifecycleScope.launch {
            delay(delayParams.delay)
            this@animate.animate(
                duracion,
                animParams,
                initVisible,
                initAnimParams,
                null,
                interpolator,
                endAction
            )
        }
        return
    }

    val animacion = this.animate()
    initAnimParams?.let {
        it.alpha?.let { alpha -> this.alpha = alpha }
        it.x?.let { x ->
            val currentX = this.translationX
            this.translationX = x
            animacion.translationX(currentX)
        }
        it.y?.let { y ->
            val currentY = this.translationY
            this.translationY = y
            animacion.translationY(currentY)
        }
        it.z?.let { z ->
            val currentZ = this.translationZ
            this.translationZ = z
            animacion.translationZ(currentZ)
        }
        it.scale?.let { scale ->
            this.scaleX = scale
            this.scaleY = scale
        }
        it.rotation?.let { this.rotation = it }
        it.alpha?.let { this.alpha = it }
    }
    initVisible?.let { visible -> this.isVisible = visible }
    duracion.let { animacion.duration = (it * 1000).toLong() }
    animParams?.let {
        it.x?.let { x ->
            if (initAnimParams?.x == null)
                animacion.translationX(x)
        }
        it.y?.let { y ->
            if (initAnimParams?.y == null)
                animacion.translationY(y)
        }
        it.z?.let { z ->
            if (initAnimParams?.z == null)
                animacion.translationZ(z)
        }
        it.alpha?.let { animacion.alpha(it) }

        it.scale?.let { scale ->
            animacion.scaleX(scale)
            animacion.scaleY(scale)
        }
        it.rotation?.let { rotation ->
            animacion.rotation(rotation)
        }
    }
    endAction?.let { animacion.withEndAction { it.invoke(this) } }
    animacion.interpolator = interpolator
    animacion.start()
}

data class AnimParams(
    val x: Float? = null,
    val y: Float? = null,
    val z: Float? = null,
    val rotation: Float? = null,
    val alpha: Float? = null,
    val scale: Float? = null
)

data class DelayParams(
    val delay: Long,
    val lifecycleScope: LifecycleCoroutineScope
)

internal fun View.visible() {
    this.visibility = View.VISIBLE
}

internal fun View.invisible() {
    this.visibility = View.GONE
}

internal fun View.playAnimation(animId : Int) = startAnimation(AnimationUtils.loadAnimation(context,animId))

internal fun View.visible(visible:Boolean?){
    if (visible == true)
        this.visible()
    else
        this.invisible()
}

internal val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()