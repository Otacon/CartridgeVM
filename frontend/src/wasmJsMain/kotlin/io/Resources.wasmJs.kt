@file:OptIn(ExperimentalWasmJsInterop::class)

package io

internal actual fun readTextResource(path: String): String = readTextResourceImpl(path.removePrefix("/"))

@JsFun(
    """
    (path) => {
        const request = new XMLHttpRequest();
        request.open('GET', path, false);
        request.send();
        if ((request.status >= 200 && request.status < 300) || request.status === 0) return request.responseText;
        throw new Error('Unable to load resource ' + path + ': ' + request.status + ' ' + request.statusText);
    }
    """
)
private external fun readTextResourceImpl(path: String): String
