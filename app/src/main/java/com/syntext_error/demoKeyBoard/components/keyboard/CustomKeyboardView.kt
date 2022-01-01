package com.syntext_error.demoKeyBoard.components.keyboard

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.syntext_error.demoKeyBoard.GifListAdapter
import com.syntext_error.demoKeyBoard.MainActivity
import com.syntext_error.demoKeyBoard.R
import com.syntext_error.demoKeyBoard.components.expandableView.ExpandableState
import com.syntext_error.demoKeyBoard.components.expandableView.ExpandableStateListener
import com.syntext_error.demoKeyBoard.components.expandableView.ExpandableView
import com.syntext_error.demoKeyBoard.components.expandableView.GifInterface
import com.syntext_error.demoKeyBoard.components.keyboard.controllers.DefaultKeyboardController
import com.syntext_error.demoKeyBoard.components.keyboard.controllers.KeyboardController
import com.syntext_error.demoKeyBoard.components.keyboard.controllers.NumberDecimalKeyboardController
import com.syntext_error.demoKeyBoard.components.keyboard.layouts.KeyboardLayout
import com.syntext_error.demoKeyBoard.components.keyboard.layouts.NumberDecimalKeyboardLayout
import com.syntext_error.demoKeyBoard.components.keyboard.layouts.NumberKeyboardLayout
import com.syntext_error.demoKeyBoard.components.keyboard.layouts.QwertyKeyboardLayout
import com.syntext_error.demoKeyBoard.components.textFields.CustomTextField
import com.syntext_error.demoKeyBoard.components.utilities.ComponentUtils
import com.syntext_error.demoKeyBoard.models.Link
import java.util.*


class CustomKeyboardView(context: Context, attr: AttributeSet) : ExpandableView(context, attr),
    GifListAdapter.Interaction {
    private var fieldInFocus: EditText? = null
    private var act : MainActivity? =  null
    private val keyboards = HashMap<EditText, KeyboardLayout?>()
    private val keyboardListener: KeyboardListener
    private lateinit var bottomSheetDialog : BottomSheetDialog

    init {
        setBackgroundColor(Color.GRAY)

        keyboardListener = object : KeyboardListener {
            override fun characterClicked(c: Char) {
                // don't need to do anything here

                //
            }

            override fun specialKeyClicked(key: KeyboardController.SpecialKey) {

                when {
                    key === KeyboardController.SpecialKey.DONE -> {
                        translateLayout()
                    }
                    key === KeyboardController.SpecialKey.GIF -> {

                        showBottomSheetDialog(context)

                    }
                    key === KeyboardController.SpecialKey.NEXT -> {
                        fieldInFocus?.focusSearch(View.FOCUS_DOWN)?.let {
                            it.requestFocus()
                            checkLocationOnScreen()
                            return
                        }
                    }
                }

            }
        }

        // register listener with parent (listen for state changes)
        registerListener(object : ExpandableStateListener {
            override fun onStateChange(state: ExpandableState) {
                if (state === ExpandableState.EXPANDED) {
                    checkLocationOnScreen()
                }
            }
        })

        // empty onClickListener prevents user from
        // accidentally clicking views under the keyboard
        setOnClickListener({})
        isSoundEffectsEnabled = false
    }

     fun registerEditText(type: KeyboardType, field: EditText , activity: MainActivity? = act) {
         act = activity
        if (!field.isEnabled) {
            return  // disabled fields do not have input connections
        }

        field.setRawInputType(InputType.TYPE_CLASS_TEXT)
        field.setTextIsSelectable(true)
        field.showSoftInputOnFocus = false
        field.isSoundEffectsEnabled = false
        field.isLongClickable = false

        val inputConnection = field.onCreateInputConnection(EditorInfo())
        keyboards[field] = createKeyboardLayout(type, inputConnection)
        keyboards[field]?.registerListener(keyboardListener)

        field.onFocusChangeListener = OnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (hasFocus) {
                ComponentUtils.hideSystemKeyboard(context, field)

                // if we can find a view below this field, we want to replace the
                // done button with the next button in the attached keyboard
                field.focusSearch(View.FOCUS_DOWN)?.run {
                    if (this is EditText) keyboards[field]?.hasNextFocus = true
                }
                fieldInFocus = field

                renderKeyboard()
                if (!isExpanded) {
                    translateLayout()
                }
            } else if (!hasFocus && isExpanded) {
                for (editText in keyboards.keys) {
                    if (editText.hasFocus()) {
                        return@OnFocusChangeListener
                    }
                }
                translateLayout()
            }
        }

        field.setOnClickListener {
            if (!isExpanded) {
                translateLayout()
            }
        }
    }

    fun autoRegisterEditTexts(rootView: ViewGroup) {
        registerEditTextsRecursive(rootView)
    }

    private fun registerEditTextsRecursive(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                registerEditTextsRecursive(view.getChildAt(i))
            }
        } else {
            if (view is CustomTextField) {
                registerEditText(view.keyboardType, view)
            } else if (view is EditText) {
                when (view.inputType) {
                    InputType.TYPE_CLASS_NUMBER -> {
                        registerEditText(CustomKeyboardView.KeyboardType.NUMBER, view)
                    }
                    InputType.TYPE_NUMBER_FLAG_DECIMAL -> {
                        registerEditText(CustomKeyboardView.KeyboardType.NUMBER_DECIMAL, view)
                    }
                    else -> {
                        registerEditText(CustomKeyboardView.KeyboardType.QWERTY, view)
                    }
                }
            }
        }
    }

    fun unregisterEditText(field: EditText?) {
        keyboards.remove(field)
    }

    fun clearEditTextCache() {
        keyboards.clear()
    }

    private fun renderKeyboard() {
        removeAllViews()
        val keyboard: KeyboardLayout? = keyboards[fieldInFocus]
        keyboard?.let {
            it.orientation = LinearLayout.VERTICAL
            it.createKeyboard(measuredWidth.toFloat())
            addView(keyboard)
        }
    }

    private fun createKeyboardLayout(type: KeyboardType, ic: InputConnection): KeyboardLayout? {
        when (type) {
            KeyboardType.NUMBER -> {
                return NumberKeyboardLayout(context, createKeyboardController(type, ic))
            }
            KeyboardType.NUMBER_DECIMAL -> {
                return NumberDecimalKeyboardLayout(context, createKeyboardController(type, ic))
            }
            KeyboardType.QWERTY -> {

                return QwertyKeyboardLayout(context, createKeyboardController(type, ic))
            }
            else -> return@createKeyboardLayout null // this should never happen
        }
    }


    private fun createKeyboardController(
        type: KeyboardType,
        ic: InputConnection
    ): KeyboardController? {
        return when (type) {
            KeyboardType.NUMBER_DECIMAL -> {
                NumberDecimalKeyboardController(ic)
            }
            else -> {
                // not all keyboards require a custom controller
                DefaultKeyboardController(ic)
            }
        }
    }

    override fun configureSelf() {
        renderKeyboard()
        checkLocationOnScreen()
    }

    /**
     * Check if fieldInFocus has a parent that is a ScrollView.
     * Ensure that ScrollView is enabled.
     * Check if the fieldInFocus is below the KeyboardLayout (measured on the screen).
     * If it is, find the deltaY between the top of the KeyboardLayout and the top of the
     * fieldInFocus, add 20dp (for padding), and scroll to the deltaY.
     * This will ensure the keyboard doesn't cover the field (if conditions above are met).
     */
    private fun showBottomSheetDialog(context: Context) {
         bottomSheetDialog = BottomSheetDialog(context)
        if (bottomSheetDialog.window != null)
            bottomSheetDialog.window!!.setDimAmount(0F);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout)
        bottomSheetDialog.show()
        val list: RecyclerView? = bottomSheetDialog.findViewById(R.id.gifList)

        loadGif(list)


//        test?.setOnClickListener {
//            fieldInFocus?.setText(":)")
//        }
    }

    private fun loadGif(rcv: RecyclerView?) {
        val myTopPostsQuery = FirebaseDatabase.getInstance().getReference("gifs")
        val list: MutableList<Link> = mutableListOf()
        val mAdapter = GifListAdapter(this)
        myTopPostsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    val gif: Link? = postSnapshot.getValue(Link::class.java)
                    if (gif != null) {
                        list.add(gif)
                        rcv?.layoutManager = GridLayoutManager(context, 2)

                        rcv?.adapter = mAdapter

                        mAdapter.submitList(list)
                    }
                    Log.d("TAG", "loadPost: ${gif?.link} ")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })



    }

    private fun checkLocationOnScreen() {
        fieldInFocus?.run {
            var fieldParent = this.parent
            while (fieldParent !== null) {
                if (fieldParent is ScrollView) {
                    if (!fieldParent.isSmoothScrollingEnabled) {
                        break
                    }

                    val fieldLocation = IntArray(2)
                    this.getLocationOnScreen(fieldLocation)

                    val keyboardLocation = IntArray(2)
                    this@CustomKeyboardView.getLocationOnScreen(keyboardLocation)

                    val fieldY = fieldLocation[1]
                    val keyboardY = keyboardLocation[1]

                    if (fieldY > keyboardY) {
                        val deltaY = (fieldY - keyboardY)
                        val scrollTo =
                            (fieldParent.scrollY + deltaY + this.measuredHeight + 10.toDp)
                        fieldParent.smoothScrollTo(0, scrollTo)
                    }
                    break
                }
                fieldParent = fieldParent.parent
            }
        }
    }

    enum class KeyboardType {
        NUMBER,
        NUMBER_DECIMAL,
        QWERTY
    }


    override fun onItemSelected(position: Int, item: Link) {
        act?.keyClicked("${item.link}")

    }

}
