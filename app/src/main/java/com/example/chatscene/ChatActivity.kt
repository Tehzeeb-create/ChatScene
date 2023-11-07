package com.example.chatscene

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chatscene.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


data class Message(
    val userId: String,
    val message: String
)

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var database: DatabaseReference
    private lateinit var sessionId: String
    private lateinit var currentUser: User
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra("sessionId") ?: ""
        currentUser = intent.getParcelableExtra("currentUser") ?: User("", "", "")

        database = FirebaseDatabase.getInstance().reference.child("chat_sessions").child(sessionId)
        auth = FirebaseAuth.getInstance()

        val userName = currentUser.name
        val userGender = currentUser.gender

        if (!userName.isNullOrBlank() && !userGender.isNullOrBlank()) {
            binding.textDisplayName.text = userName
            binding.textGender.text = userGender

            binding.buttonSend.setOnClickListener {
                val message = binding.editTextMessage.text.toString()
                if (message.isNotEmpty()) {
                    sendMessage(sessionId, message)
                    binding.editTextMessage.text.clear()
                }
            }

            loadChatMessages()
        }
    }

    private fun sendMessage(sessionId: String, message: String) {
        val userId = auth.currentUser?.uid ?: ""
        val messageData = Message(userId, message)

        // Save the message to Firebase Realtime Database
        database.push().setValue(messageData)
    }

    private fun loadChatMessages() {
        val messageListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<String>()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.let {
                        val messageText = "${it.userId}: ${it.message}"
                        messages.add(messageText)
                    }
                }
                val chatText = messages.joinToString("\n")
                binding.textChatView.text = chatText
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        }
        database.addValueEventListener(messageListener)
    }
}
