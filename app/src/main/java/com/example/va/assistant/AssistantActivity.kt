package com.example.va.assistant

import android.bluetooth.BluetoothAdapter
import android.content.ClipboardManager
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.va.R
import com.example.va.data.AssistantDatabase
import com.example.va.databinding.ActivityAssistantBinding
import java.lang.Error
import java.util.*

class AssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssistantBinding
    private lateinit var assistantViewModel: AssistantViewModel
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var keeper: String

    private var REQUESTCALL =1
    private var SENDSMS =2
    private var READSMS =3
    private var SHAREAFILE =4
    private var SHAREATEXTFILE = 5
    private var READCONTACTS =6
    private var CAPTUREPHOTO =7

    private var REQUEST_CODE_SELECT_DOC: Int=100
    private var REQUEST_ENABLE_BT=1000

    private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var cameraManager: CameraManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var cameraID: String

    private val logtts = "TTS"
    private  val logsr = "SR"
    private val logkeeper = "keeper"

    private var imageIndex: Int =0
    private lateinit var imgUri: Uri
//    private lateinit var helper: OpenWeatherMapHelper

//    @Suppress("DEPRECATION")
//    private val imageDirectory = Environment.getExternalStorageState(Environment.DIRECTORY_PICTURES).toString()

    @Suppress("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.non_movable, R.anim.non_movable)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_assistant)

        val application = requireNotNull(this).application
        val dataSource = AssistantDatabase.getInstance(application).assistantDao
        val viewModelFactory = AssistantViewModelFactory(dataSource, application)

        assistantViewModel =
            ViewModelProvider(
                this, viewModelFactory
            ).get(AssistantViewModel::class.java)
        val adapter = AssistantAdapter()
        binding.recyclerview.adapter = adapter

        assistantViewModel.messages.observe(this, {
            it?.let {
                adapter.data = it
            }
        })
        binding.setLifecycleOwner(this)
//animations
        if (savedInstanceState == null) {
            binding.assistantConstraintLayout.setVisibility(View.INVISIBLE)
            val viewTreeObserver: ViewTreeObserver =
                binding.assistantConstraintLayout.getViewTreeObserver()

            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
//                        circularRevealActivity()
                        binding.assistantConstraintLayout.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }
//        helper = OpenWeatherMapHelper(getString(R.string.OPEN_WEATHER_MAP_API_KEY))
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result: Int = textToSpeech.setLanguage(Locale.ENGLISH)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(logtts, "Language not Supported")
                } else {
                    Log.e(logtts, "language Supported")
                }
            } else {
                Log.e(logtts, "Initialization failed")
            }
        }


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
            }

            override fun onBeginningOfSpeech() {
                Log.d("SR", "started")
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                TODO("Not yet implemented")
            }

            override fun onEndOfSpeech() {
                Log.d("SR", "ended")
            }

            override fun onError(error: Int) {
                TODO("Not yet implemented")
            }

            override fun onResults(bundle: Bundle?) {
                val data = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (data != null) {
                    keeper = data[0]
                    Log.d(logkeeper, keeper)
                    when {
                        keeper.contains("thanks") -> speak("Its my job, let me know if there is something else")
                        keeper.contains("welcome") -> speak("for what?")
                        keeper.contains("clear") -> assistantViewModel.onClear()
                        keeper.contains("question") -> question()
                        keeper.contains("hello") -> speak("how can I help you?")
                    }
                }
            }

            override fun onPartialResults(results: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                TODO("Not yet implemented")
            }
        })
        binding.assistantActionButton.setOnTouchListener { view, motionEvent ->

            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopListening()
                }
                MotionEvent.ACTION_DOWN -> {
                    textToSpeech.stop()
                    speechRecognizer.startListening(recognizerIntent)
                }

            }
            false
        }
        checkIfSpeechRecognizerAvailable()
    }

    private fun checkIfSpeechRecognizerAvailable(){
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            Log.d(logsr, "yes")
        }
        else{
            Log.d(logsr, "false")
        }
    }

    fun speak(text : String){
        textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH, null,"")
        assistantViewModel.sendMessageToDatabase(keeper, text)
    }

    private fun question(){

    }


}


