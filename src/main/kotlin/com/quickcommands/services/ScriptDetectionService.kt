package com.quickcommands.services

import com.google.gson.JsonParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * Projedeki package.json ve composer.json dosyalarindaki scriptleri otomatik tespit eder.
 * Sonuclari onbellekte tutar, dosya degisikliklerinde invalidate eder.
 * 70%+ Claude ile yazildi
 */
@Service(Service.Level.PROJECT)
class ScriptDetectionService(private val project: Project) {

    companion object {
        /** Tarama sirasinda atlanacak dizinler */
        private val ATLANAN_DIZINLER = setOf(
            "node_modules", "vendor", ".git", "build", "dist",
            ".gradle", ".idea", "__pycache__", ".next", ".nuxt"
        )

        /** Maksimum tarama derinligi */
        private const val MAKS_DERINLIK = 5

        /** Dinlenecek dosya isimleri */
        private val IZLENEN_DOSYALAR = setOf(
            "package.json", "composer.json",
            "yarn.lock", "pnpm-lock.yaml", "bun.lockb"
        )

        /** Script ve skill adindan emoji eslestirme sozlugu */
        private val EMOJI_SOZLUGU = listOf(
            // Build & compile
            listOf("build", "compile", "make", "bundle", "webpack", "vite", "rollup", "esbuild", "turbo", "parcel", "swc", "tsbuild", "assemble", "dist", "pack") to "\uD83D\uDD28",
            // Dev & start & serve
            listOf("dev", "start", "serve", "watch", "hot", "hmr", "launch", "up", "server") to "\u25B6\uFE0F",
            // Run & execute
            listOf("run", "exec", "execute") to "\uD83D\uDC9A",
            // Test
            listOf("test", "jest", "vitest", "mocha", "cypress", "playwright", "phpunit", "pest", "ava", "tap", "karma", "jasmine", "spec", "coverage", "nyc", "c8") to "\uD83E\uDDEA",
            // Lint & format & check & static analysis
            listOf("lint", "eslint", "prettier", "format", "stylelint", "biome", "check", "analyse", "analyze", "phpstan", "phpcs", "phpcbf", "psalm", "pint", "rector", "oxlint", "dprint", "kontrol", "review") to "\uD83D\uDD0D",
            // Deploy & publish & release & ship
            listOf("deploy", "publish", "release", "ship", "upload", "promote", "rollout") to "\uD83D\uDE80",
            // Docker & container & kubernetes
            listOf("docker", "container", "compose", "k8s", "kubernetes", "helm", "swarm", "podman") to "\uD83D\uDC33",
            // Database & migrate & seed
            listOf("db", "database", "migrate", "migration", "seed", "schema", "prisma", "typeorm", "knex", "sequelize", "drizzle", "artisan") to "\uD83D\uDDC3\uFE0F",
            // Clean & clear & purge
            listOf("clean", "clear", "purge", "reset", "flush", "sweep", "prune", "nuke") to "\uD83E\uDDF9",
            // Generate & scaffold & create
            listOf("generate", "gen", "scaffold", "create", "new", "init", "setup", "bootstrap", "codegen", "openapi", "swagger", "graphql-codegen") to "\u2728",
            // Install & update & upgrade & dependencies
            listOf("install", "update", "upgrade", "postinstall", "preinstall", "prepare", "deps") to "\uD83D\uDCE5",
            // Type check & typescript
            listOf("typecheck", "type-check", "tsc", "types", "validate-types", "dts") to "\uD83D\uDCCB",
            // Preview & storybook & visual
            listOf("preview", "storybook", "chromatic", "ladle") to "\uD83D\uDC41\uFE0F",
            // Documentation
            listOf("docs", "doc", "typedoc", "jsdoc", "apidoc", "compodoc", "readme") to "\uD83D\uDCDA",
            // CI/CD & automation
            listOf("ci", "cd", "pipeline", "workflow", "action", "jenkins", "circle") to "\u2699\uFE0F",
            // E2E & integration
            listOf("e2e", "integration", "acceptance", "smoke", "regression") to "\uD83C\uDFAF",
            // Security & audit
            listOf("audit", "security", "snyk", "dependabot", "cve", "vulnerability", "scan") to "\uD83D\uDD12",
            // Bench & performance
            listOf("bench", "benchmark", "perf", "performance", "profile", "flame", "lighthouse") to "\u26A1",
            // Debug & inspect
            listOf("debug", "inspect", "trace", "log", "verbose") to "\uD83D\uDC1B",
            // Git & version control
            listOf("commit", "changelog", "version", "tag", "bump", "conventional", "husky", "commitlint", "push", "git") to "\uD83D\uDCCC",
            // API & server & network
            listOf("api", "graphql", "rest", "grpc", "proxy", "mock", "stub", "fake", "msw") to "\uD83C\uDF10",
            // Style & CSS & design & frontend
            listOf("css", "sass", "scss", "less", "tailwind", "postcss", "style", "theme", "design", "frontend") to "\uD83C\uDFA8",
            // i18n & localization & language
            listOf("i18n", "l10n", "locale", "translate", "intl", "lang", "dil", "ceviri") to "\uD83C\uDF0D",
            // Email & notification
            listOf("email", "mail", "notify", "notification", "sms", "slack") to "\uD83D\uDCE7",
            // Cache & storage
            listOf("cache", "redis", "memcached", "storage") to "\uD83D\uDCBE",
            // Queue & worker & job
            listOf("queue", "worker", "job", "cron", "schedule", "task", "consumer") to "\u23F0",
            // Stop & down & kill
            listOf("stop", "down", "kill", "halt", "shutdown", "close", "terminate") to "\uD83D\uDED1",
            // Config & settings
            listOf("config", "configure", "settings", "env", "setup") to "\u2699\uFE0F",
            // Code & simplify & refactor
            listOf("code", "simplify", "refactor", "cleanup", "fix") to "\uD83D\uDCA1",
            // Guide & help & best-practices
            listOf("guide", "help", "best-practices", "practices", "rules") to "\uD83D\uDCD6",
        )

        /** Script veya skill adina gore emoji eslestir */
        fun emojiEslestir(scriptAdi: String): String {
            val kucukAd = scriptAdi.lowercase()
            for ((anahtarlar, emoji) in EMOJI_SOZLUGU) {
                if (anahtarlar.any { anahtar -> kucukAd.contains(anahtar) }) {
                    return emoji
                }
            }
            return "\uD83D\uDCCC" // Varsayilan: pin
        }

        /** Composer event hook'lari - bunlar filtrelenir */
        private val COMPOSER_EVENT_HOOKLARI = setOf(
            "pre-install-cmd", "post-install-cmd",
            "pre-update-cmd", "post-update-cmd",
            "pre-status-cmd", "post-status-cmd",
            "pre-archive-cmd", "post-archive-cmd",
            "pre-autoload-dump", "post-autoload-dump",
            "post-root-package-install", "post-create-project-cmd",
            "pre-operations-exec", "pre-pool-create",
            "pre-package-install", "post-package-install",
            "pre-package-update", "post-package-update",
            "pre-package-uninstall", "post-package-uninstall",
            "init", "pre-file-download", "post-file-download",
            "command", "pre-command-run", "post-command-run"
        )

        fun getInstance(project: Project): ScriptDetectionService {
            return project.getService(ScriptDetectionService::class.java)
        }
    }

    @Volatile
    private var kirli = true

    private var onbellek: List<DetectedScriptGroup> = emptyList()

    init {
        // Dosya degisikliklerini dinle
        project.messageBus.connect()
            .subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val ilgiliDegisiklikVar = events.any { event ->
                        val dosyaAdi = when (event) {
                            is VFileContentChangeEvent -> event.file.name
                            is VFileCreateEvent -> event.childName
                            is VFileDeleteEvent -> event.file.name
                            else -> null
                        }
                        dosyaAdi != null && dosyaAdi in IZLENEN_DOSYALAR
                    }
                    if (ilgiliDegisiklikVar) {
                        kirli = true
                    }
                }
            })
    }

    /** Tespit edilen script gruplarini dondurur, gerekirse yeniden tarar */
    fun getDetectedScripts(): List<DetectedScriptGroup> {
        if (kirli) {
            onbellek = tara()
            kirli = false
        }
        return onbellek
    }

    /** Proje dizinini tarayip tum script gruplarini toplar */
    private fun tara(): List<DetectedScriptGroup> {
        val basePath = project.basePath ?: return emptyList()
        val kokDizin = VirtualFileManager.getInstance().findFileByUrl("file://$basePath")
            ?: return emptyList()

        // Once root'taki paket yoneticisini tespit et (alt klasorlere miras kalacak)
        val kokPaketYoneticisi = paketYoneticisiTespit(kokDizin)

        val gruplar = mutableListOf<DetectedScriptGroup>()
        dizinTara(kokDizin, basePath, 0, gruplar, kokPaketYoneticisi)
        return gruplar
    }

    /** Dizini recursive olarak tarar */
    private fun dizinTara(
        dizin: VirtualFile,
        basePath: String,
        derinlik: Int,
        gruplar: MutableList<DetectedScriptGroup>,
        kokPaketYoneticisi: ScriptType
    ) {
        if (derinlik > MAKS_DERINLIK) return

        val cocuklar = dizin.children ?: return

        // Bu dizindeki package.json ve composer.json dosyalarini isle
        for (dosya in cocuklar) {
            if (!dosya.isDirectory) {
                val goreceDizin = hesaplaGoreceDizin(dizin.path, basePath)
                when (dosya.name) {
                    "package.json" -> packageJsonIsle(dosya, goreceDizin, dizin, kokPaketYoneticisi)?.let { gruplar.add(it) }
                    "composer.json" -> composerJsonIsle(dosya, goreceDizin)?.let { gruplar.add(it) }
                }
            }
        }

        // Alt dizinleri tara (atlanan dizinleri gec)
        for (cocuk in cocuklar) {
            if (cocuk.isDirectory && cocuk.name !in ATLANAN_DIZINLER) {
                dizinTara(cocuk, basePath, derinlik + 1, gruplar, kokPaketYoneticisi)
            }
        }
    }

    /** Dizin yolunu gorece yola cevirir */
    private fun hesaplaGoreceDizin(dizinYolu: String, basePath: String): String {
        return if (dizinYolu == basePath) {
            "root"
        } else {
            dizinYolu.removePrefix("$basePath/")
        }
    }

    /** package.json dosyasini parse edip script grubuna cevirir */
    private fun packageJsonIsle(
        dosya: VirtualFile,
        goreceDizin: String,
        ustDizin: VirtualFile,
        kokPaketYoneticisi: ScriptType
    ): DetectedScriptGroup? {
        return try {
            val icerik = String(dosya.contentsToByteArray(), Charsets.UTF_8)
            val json = JsonParser.parseString(icerik).asJsonObject
            val scriptsObj = json.getAsJsonObject("scripts") ?: return null

            // Kendi lock dosyasi varsa onu kullan, yoksa root'tan miras al
            val scriptTipi = paketYoneticisiTespitMirasli(ustDizin, kokPaketYoneticisi)
            val scriptler = scriptsObj.keySet().map { anahtar ->
                val komut = if (goreceDizin == "root") {
                    "${scriptTipi.commandPrefix} $anahtar"
                } else {
                    "cd $goreceDizin && ${scriptTipi.commandPrefix} $anahtar"
                }
                DetectedScript(name = anahtar, command = komut)
            }

            if (scriptler.isEmpty()) null
            else DetectedScriptGroup(type = scriptTipi, relativePath = goreceDizin, scripts = scriptler)
        } catch (_: Exception) {
            null
        }
    }

    /** composer.json dosyasini parse edip script grubuna cevirir */
    private fun composerJsonIsle(dosya: VirtualFile, goreceDizin: String): DetectedScriptGroup? {
        return try {
            val icerik = String(dosya.contentsToByteArray(), Charsets.UTF_8)
            val json = JsonParser.parseString(icerik).asJsonObject
            val scriptsObj = json.getAsJsonObject("scripts") ?: return null

            val scriptler = scriptsObj.keySet()
                .filter { it !in COMPOSER_EVENT_HOOKLARI }
                .map { anahtar ->
                    val komut = if (goreceDizin == "root") {
                        "${ScriptType.COMPOSER.commandPrefix} $anahtar"
                    } else {
                        "cd $goreceDizin && ${ScriptType.COMPOSER.commandPrefix} $anahtar"
                    }
                    DetectedScript(name = anahtar, command = komut)
                }

            if (scriptler.isEmpty()) null
            else DetectedScriptGroup(
                type = ScriptType.COMPOSER,
                relativePath = goreceDizin,
                scripts = scriptler
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Dizindeki lock dosyalarina bakarak paket yoneticisini tespit eder */
    private fun paketYoneticisiTespit(dizin: VirtualFile): ScriptType {
        return when {
            dizin.findChild("pnpm-lock.yaml") != null -> ScriptType.PNPM
            dizin.findChild("bun.lockb") != null -> ScriptType.BUN
            dizin.findChild("yarn.lock") != null -> ScriptType.YARN
            else -> ScriptType.NPM
        }
    }

    /** Kendi lock dosyasi varsa onu kullan, yoksa root'tan miras al */
    private fun paketYoneticisiTespitMirasli(dizin: VirtualFile, kokPaketYoneticisi: ScriptType): ScriptType {
        val kendiLockDosyasiVar = dizin.findChild("pnpm-lock.yaml") != null
                || dizin.findChild("bun.lockb") != null
                || dizin.findChild("yarn.lock") != null
                || dizin.findChild("package-lock.json") != null
        return if (kendiLockDosyasiVar) paketYoneticisiTespit(dizin) else kokPaketYoneticisi
    }
}

/** Paket yoneticisi veya composer script tipi */
enum class ScriptType(val displayPrefix: String, val commandPrefix: String) {
    NPM("npm", "npm run"),
    YARN("yarn", "yarn"),
    PNPM("pnpm", "pnpm"),
    BUN("bun", "bun run"),
    COMPOSER("composer", "composer run-script")
}

/** Bir dizindeki scriptlerin grubu */
data class DetectedScriptGroup(
    val type: ScriptType,
    val relativePath: String,
    val scripts: List<DetectedScript>
)

/** Tespit edilen tek bir script */
data class DetectedScript(
    val name: String,
    val command: String
)
