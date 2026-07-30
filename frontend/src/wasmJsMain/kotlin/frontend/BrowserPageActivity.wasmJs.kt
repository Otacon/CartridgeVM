@file:OptIn(ExperimentalWasmJsInterop::class)

package frontend

internal fun isPageActive(): Boolean = pageIsActive()

internal fun addPageActivityListener(callback: () -> Unit): JsAny = addActivityListener(callback)

internal fun removePageActivityListener(listener: JsAny) = removeActivityListener(listener)

@JsFun("() => !document.hidden && document.hasFocus()")
private external fun pageIsActive(): Boolean

@JsFun(
    """
    (callback) => {
        const listener = () => callback();
        document.addEventListener('visibilitychange', listener);
        window.addEventListener('blur', listener);
        window.addEventListener('focus', listener);
        window.addEventListener('pagehide', listener);
        window.addEventListener('pageshow', listener);
        return listener;
    }
    """
)
private external fun addActivityListener(callback: () -> Unit): JsAny

@JsFun(
    """
    (listener) => {
        document.removeEventListener('visibilitychange', listener);
        window.removeEventListener('blur', listener);
        window.removeEventListener('focus', listener);
        window.removeEventListener('pagehide', listener);
        window.removeEventListener('pageshow', listener);
    }
    """
)
private external fun removeActivityListener(listener: JsAny)
