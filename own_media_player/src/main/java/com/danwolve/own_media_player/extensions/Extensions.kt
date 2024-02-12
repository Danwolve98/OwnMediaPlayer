package com.danwolve.own_media_player.extensions

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils

/**
 * Función encargada de ejecutar animaciones básicas sobre las vistas.
 * Si no se especifica ningun "init" el valor inicial de ese parámetro será el por defecto de la vista.
 * Ningún parámetro es obligatorio.
 * @param duracion la duración de la animación en segundos
 * @param x desplazamiento horizontal de la vista
 * @param y desplazamiento vertical de la vista
 * @param alpha el alpha de la vista (fade)
 * @param initAlpha el alpha desde el que parte la animación
 * @param initX posicion x desde el que parte la animación
 * @param initY posicion y desde el que parte la animación
 * @param initScale escala inicial desde la que comienza la animación
 * @param endAction la acción que quieres que se ejecute al acabar la animación
 */
internal fun <T : View> T.animate(
    duracion : Float = 1F,
    x : Float? = null,
    y : Float? = null,
    alpha : Float? = null,
    scale : Float? = null,
    initVisible : Boolean? = null,
    initAlpha : Float?=null,
    initX : Float?= null,
    initY : Float?= null,
    initScale : Float?= null,
    endAction : ((T) -> Unit)? = null){
    val animacion = this.animate()
    initVisible.notNull { this.visible(it) }
    initAlpha.notNull { this.alpha = it }
    initX.notNull { this.translationX = it }
    initY.notNull { this.translationY = it }
    initScale.notNull {
        this.scaleX = it
        this.scaleY = it
    }
    initAlpha.notNull { this.alpha = it }
    duracion.notNull { animacion.duration = (it * 1000).toLong() }
    x.notNull { animacion.translationX(it) }
    y.notNull { animacion.translationY(it) }
    alpha.notNull { animacion.alpha(it) }
    endAction.notNull { animacion.withEndAction { it.invoke(this) } }
    scale.notNull {
        animacion.scaleX(it)
        animacion.scaleY(it)
    }
    animacion.interpolator = AccelerateDecelerateInterpolator()
    animacion.start()
}

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

internal fun <T : Any> T?.notNull(f: (it: T) -> Unit) {
    if (this != null) f(this)
}