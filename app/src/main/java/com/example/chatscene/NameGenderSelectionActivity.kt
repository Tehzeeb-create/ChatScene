package com.example.chatscene


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatscene.databinding.ActivityNameGenderSelectionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.random.Random


class NameGenderSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNameGenderSelectionBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameGenderSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database and Authentication
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        val defaultName = "Stranger"

        binding.editTextName.setText(defaultName)
        binding.radioButtonFemale.isChecked = true

        binding.buttonConnect.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val selectedGender = when (binding.radioGroupGender.checkedRadioButtonId) {
                binding.radioButtonMale.id -> "Male"
                else -> "Female"
            }

            val userName = name.ifBlank { defaultName }

            // Create a user in Firebase and store user data
            createUserInFirebase(userName, selectedGender)
        }
    }

    private fun createUserInFirebase(userName: String, userGender: String) {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (userId != null) {
                        // Store user information in Firebase Realtime Database
                        val userRef = database.child("users").child(userId)
                        userRef.child("name").setValue(userName)
                        userRef.child("gender").setValue(userGender)

                        // Emit an event to the server to find available users
                        findAvailableUsers(userId, userName, userGender)
                    } else {
                        Toast.makeText(this, "User authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun findAvailableUsers(userId: String, userName: String, userGender: String) {
        val availableUsersRef = database.child("users").orderByChild("gender").equalTo(userGender)

        availableUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val availableUsers = mutableListOf<User>()

                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && user.userId != userId) {
                        availableUsers.add(user)
                    }
                }

                if (availableUsers.size >= 2) {
                    val random = Random
                    val randomUser1 = availableUsers[random.nextInt(availableUsers.size)]

                    var randomUser2: User
                    do {
                        randomUser2 = availableUsers[random.nextInt(availableUsers.size)]
                    }while (randomUser2 == randomUser1)
                    // Implement your logic to create a chat session for the two users here
                    val chatSessionId = createChatSession(randomUser1.name, randomUser2.name)

                    // Navigate to the chat activity with the chatSessionId
                    navigateToChat(chatSessionId)
                } else {
                    Toast.makeText(this@NameGenderSelectionActivity, "Waiting for more users to become available...", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors or database connection issues here
                Toast.makeText(this@NameGenderSelectionActivity, "Unable to connect to Database", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun createChatSession(user1Name: String, user2Name: String): String {
        // Generate a unique chat session ID
        val chatSessionId = "chat_session_${System.currentTimeMillis()}"

        // Save chat session details to the database
        val sessionRef = database.child("chat_sessions").child(chatSessionId)
        sessionRef.child("users").child("user1").setValue(user1Name)
        sessionRef.child("users").child("user2").setValue(user2Name)

        return chatSessionId
    }

    private fun navigateToChat(chatSessionId: String) {
        val intent = Intent(this@NameGenderSelectionActivity, ChatActivity::class.java)
        intent.putExtra("chatSessionId", chatSessionId)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Sign out the user if necessary
        auth.signOut()
    }
}
