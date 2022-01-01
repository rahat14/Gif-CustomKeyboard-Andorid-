package com.syntext_error.demoKeyBoard


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.syntext_error.demoKeyBoard.components.expandableView.GifInterface
import com.syntext_error.demoKeyBoard.components.keyboard.CustomKeyboardView
import com.syntext_error.demoKeyBoard.models.Link
import java.util.*


class MainActivity : AppCompatActivity(), GifInterface,
    GifListAdapter.Interaction {
    private lateinit var keyboard: CustomKeyboardView
    private lateinit var addGIF: Button
    private lateinit var mAdapter: GifListAdapter
    private lateinit var recyclerView: RecyclerView
    val list: MutableList<Link> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAdapter = GifListAdapter(this)
        recyclerView = findViewById(R.id.rcvList)

        recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        val qwertyField: EditText = findViewById(R.id.testQwertyField)
        addGIF = findViewById(R.id.addGif)
        keyboard = findViewById(R.id.customKeyboardView)

        keyboard.registerEditText(CustomKeyboardView.KeyboardType.QWERTY, qwertyField, this)
        addGIF.setOnClickListener {
            openImage()
        }


    }

    private fun uploadImageToFirebase(fileUri: Uri) {

        if (fileUri != null) {
            Toast.makeText(applicationContext, "Starting Upload...", Toast.LENGTH_LONG).show()
            val fileName = UUID.randomUUID().toString() + ".gif"

            val database = FirebaseDatabase.getInstance()
            val dataabseREF =
                database.getReference("gifs").child("${System.currentTimeMillis() / 10}")
            val refStorage = FirebaseStorage.getInstance().reference.child("images/$fileName")

            refStorage.putFile(fileUri)
                .addOnSuccessListener(
                    OnSuccessListener { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                            val imageUrl = it.toString()
                            val model = Link(imageUrl)
                            dataabseREF.setValue(model).addOnCompleteListener {
                                Toast.makeText(
                                    applicationContext,
                                    "Successfully Uploaded...",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        }
                    })

                .addOnFailureListener(OnFailureListener { e ->
                    print(e.message)
                })
        }
    }


    override fun onBackPressed() {
        if (keyboard.isExpanded) {
            keyboard.translateLayout()
        } else {
            super.onBackPressed()
        }
    }

    override fun keyClicked(c: String) {
        Log.d("TAG", "keyClicked: $c ")
        passLink(c)
    }

    fun openImage() {
        // intent of the type image
        // intent of the type image
        val i = Intent()
        i.type = "image/gif"
        i.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(i, "Select Picture"), 100)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == 100) {
                // Get the url of the image from data
                val selectedImageUri: Uri? = data?.data
                if (null != selectedImageUri) {
                    uploadImageToFirebase(selectedImageUri)
                }
            }
        }
    }

    override fun onItemSelected(position: Int, item: Link) {

    }

    fun passLink(item: String = "") {

        val model = Link(item)
        list.add(model)
        mAdapter.submitList(list)
        Log.d("size", "passLink: ${list.size}")
        recyclerView.smoothScrollToPosition(list.size - 1)

    }

}