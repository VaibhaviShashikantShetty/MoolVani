package org.moolvani.app

import android.app.Application
import org.moolvani.app.data.db.MoolVaniRepository
import org.moolvani.app.engine.MoolvaniTranslationEngine
import org.moolvani.app.engine.WorksheetGenerator
import org.moolvani.app.engine.audio.AudioEngine
import org.moolvani.app.engine.audio.SpeechRecognitionHelper

class MoolVaniApplication : Application() {

    lateinit var repository: MoolVaniRepository
        private set
    lateinit var translationEngine: MoolvaniTranslationEngine
        private set
    lateinit var audioEngine: AudioEngine
        private set
    lateinit var speechHelper: SpeechRecognitionHelper
        private set
    lateinit var worksheetGenerator: WorksheetGenerator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        repository = MoolVaniRepository(this)
        translationEngine = MoolvaniTranslationEngine(repository)
        audioEngine = AudioEngine(this)
        speechHelper = SpeechRecognitionHelper(this)
        worksheetGenerator = WorksheetGenerator(repository)
    }

    companion object {
        lateinit var instance: MoolVaniApplication
            private set
    }
}
