package ro.pub.cs.systems.eim.practicaltest02v2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class PracticalTest02v2MainActivity : AppCompatActivity() {

    private lateinit var wordInput: EditText
    private lateinit var defineButton: Button
    private lateinit var definitionOutput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_practical_test02v2_main)

        wordInput = findViewById(R.id.wordInput)
        defineButton = findViewById(R.id.defineButton)
        definitionOutput = findViewById(R.id.definitionOutput)

        // Register receiver
        val intentFilter = IntentFilter("ro.pub.cs.systems.eim.practicaltest02v2.DEFINITION")
        registerReceiver(DefinitionReceiver(), intentFilter, RECEIVER_NOT_EXPORTED)

        // Set button click listener
        defineButton.setOnClickListener {
            val word = wordInput.text.toString()
            if (word.isNotEmpty()) {
                FetchDefinitionTask().execute(word)
            } else {
                definitionOutput.text = "Please enter a word."
            }
        }
    }

    private inner class FetchDefinitionTask : AsyncTask<String, Void, String?>() {
        override fun doInBackground(vararg params: String?): String? {
            val word = params[0]
            val apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$word"
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                // Log the raw response for debugging purposes
                Log.d("ServerResponse", response)

                // Parse JSON and extract the first definition
                val jsonArray = JSONArray(response)
                val meanings = jsonArray.getJSONObject(0).getJSONArray("meanings")
                val definitions = meanings.getJSONObject(0).getJSONArray("definitions")
                val firstDefinition = definitions.getJSONObject(0).getString("definition")

                // Log the parsed definition
                Log.d("ParsedDefinition", firstDefinition)

                return firstDefinition
            } catch (e: Exception) {
                Log.e("Error", e.message.toString())
                return null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                // Broadcast the definition
                val intent = Intent("ro.pub.cs.systems.eim.practicaltest02v2.DEFINITION")
                intent.putExtra("definition", result)
                sendBroadcast(intent)
                definitionOutput.text = result
            } else {
                definitionOutput.text = "Error fetching definition."
            }
        }
    }

    private inner class DefinitionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val definition = intent?.getStringExtra("definition")
            if (definition != null) {
                definitionOutput.text = definition
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(DefinitionReceiver())
    }
}
