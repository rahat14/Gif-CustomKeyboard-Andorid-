package com.syntext_error.demoKeyBoard.components.keyboard

import com.syntext_error.demoKeyBoard.components.keyboard.controllers.KeyboardController


interface KeyboardListener {
    fun characterClicked(c: Char)
    fun specialKeyClicked(key: KeyboardController.SpecialKey)

}