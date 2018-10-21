package xerus.monstercat

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.sheets.v4.SheetsScopes
import javafx.scene.Scene
import javafx.scene.image.Image
import kotlinx.coroutines.*
import mu.KotlinLogging
import xerus.ktutil.SystemUtils
import xerus.ktutil.getResource
import xerus.ktutil.javafx.onFx
import xerus.ktutil.javafx.ui.App
import java.io.File
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

val VERSION = getResource("version")!!.readText()
val isUnstable = VERSION.contains('-')

val dataDir: File
	get() = SystemUtils.cacheDir.resolve("monsterutilities").apply { mkdirs() }

val cacheDir: File
	get() = dataDir.resolve("cache").apply { mkdirs() }

lateinit var monsterUtilities: MonsterUtilities

val globalThreadPool: ExecutorService = Executors.newCachedThreadPool(object : ThreadFactory {
	private val poolNumber = AtomicInteger(1)
	override fun newThread(r: Runnable) =
		Thread(Thread.currentThread().threadGroup, r, "mu-worker-" + poolNumber.getAndIncrement())
})
val globalDispatcher = globalThreadPool.asCoroutineDispatcher()

val jarLocation: URL = MonsterUtilities::class.java.protectionDomain.codeSource.location

fun main(args: Array<String>) {
	initLogging(args)
	val logger = KotlinLogging.logger {}
	logger.info("Version: $VERSION, Java version: ${SystemUtils.javaVersion}, ")
	logger.debug("Commandline arguments: ${args.joinToString(", ", "[", "]")}")
	
	logger.info("Initializing Google Sheets API Service")
	Sheets.initService("MonsterUtilities")
	
	val checkUpdate = !args.contains("--no-update") && Settings.AUTOUPDATE() && jarLocation.toString().endsWith(".jar")
	App.launch("MonsterUtilities $VERSION", Settings.THEME(), { stage ->
		stage.icons.addAll(arrayOf("img/icon64.png").map {
			getResource(it)?.let { Image(it.toExternalForm()) }
				?: null.apply { logger.warn("Resource $it not found!") }
		})
	}, {
		Scene(MonsterUtilities(checkUpdate), 800.0, 700.0)
	})
	globalThreadPool.shutdown()
	logger.info("Main has shut down!")
}

fun showErrorSafe(error: Throwable, title: String = "Error") = doWhenReady { showError(error, title) }

fun doWhenReady(action: MonsterUtilities.() -> Unit) {
	GlobalScope.launch {
		var i = 0
		while(i < 100 && !::monsterUtilities.isInitialized) {
			delay(200)
			i++
		}
		onFx {
			action(monsterUtilities)
		}
	}
}

