package com.quickcommands.services

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
 * Codex CLI skill dosyalarini otomatik tespit eder.
 * Kaynaklari: ~/.codex/skills/ (global), <proje>/.agents/skills/ (proje)
 * 70%+ Claude ile yazildi
 */
@Service(Service.Level.PROJECT)
class CodexSkillDetectionService(private val project: Project) {

    companion object {
        private val CODEX_GLOBAL_DIR = "${System.getProperty("user.home")}/.codex"
        private const val CACHE_SURESI_MS = 30_000L

        fun getInstance(project: Project): CodexSkillDetectionService {
            return project.getService(CodexSkillDetectionService::class.java)
        }
    }

    @Volatile
    private var kirli = true

    private var onbellek: List<CodexSkillGroup> = emptyList()
    private var sonTaramaZamani = 0L

    init {
        project.messageBus.connect()
            .subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val ilgiliDegisiklikVar = events.any { event ->
                        val dosyaYolu = when (event) {
                            is VFileContentChangeEvent -> event.file.path
                            is VFileCreateEvent -> "${event.parent.path}/${event.childName}"
                            is VFileDeleteEvent -> event.file.path
                            else -> null
                        } ?: return@any false
                        val codexYol = dosyaYolu.contains("/.codex/skills/") ||
                                dosyaYolu.contains("/.agents/skills/")
                        codexYol && (dosyaYolu.endsWith(".md") || dosyaYolu.endsWith(".yaml"))
                    }
                    if (ilgiliDegisiklikVar) {
                        kirli = true
                    }
                }
            })
    }

    /** Tespit edilen skill gruplarini dondurur. Cache suresi dolmussa yeniden tarar. */
    fun getDetectedSkills(): List<CodexSkillGroup> {
        val simdi = System.currentTimeMillis()
        if (kirli || (simdi - sonTaramaZamani > CACHE_SURESI_MS)) {
            onbellek = tara()
            kirli = false
            sonTaramaZamani = simdi
        }
        return onbellek
    }

    private fun tara(): List<CodexSkillGroup> {
        val gruplar = mutableListOf<CodexSkillGroup>()

        val globalSkiller = globalSkilleriTara().sortedBy { it.name.lowercase() }
        if (globalSkiller.isNotEmpty()) {
            gruplar.add(CodexSkillGroup(category = CodexSkillCategory.GLOBAL, skills = globalSkiller))
        }

        val projeSkiller = projeSkilleriTara().sortedBy { it.name.lowercase() }
        if (projeSkiller.isNotEmpty()) {
            gruplar.add(CodexSkillGroup(category = CodexSkillCategory.PROJECT, skills = projeSkiller))
        }

        return gruplar
    }

    /** ~/.codex/skills/ dizinindeki global skilleri tarar */
    private fun globalSkilleriTara(): List<CodexSkill> {
        val dizin = VirtualFileManager.getInstance()
            .findFileByUrl("file://$CODEX_GLOBAL_DIR/skills") ?: return emptyList()
        return skillDizinleriTara(dizin)
    }

    /** <proje>/.agents/skills/ dizinindeki proje skillerini tarar */
    private fun projeSkilleriTara(): List<CodexSkill> {
        val basePath = project.basePath ?: return emptyList()
        val dizin = VirtualFileManager.getInstance()
            .findFileByUrl("file://$basePath/.agents/skills") ?: return emptyList()
        return skillDizinleriTara(dizin)
    }

    /** Bir skills dizinindeki SKILL.md dosyalarini tarar */
    private fun skillDizinleriTara(dizin: VirtualFile): List<CodexSkill> {
        val skiller = mutableListOf<CodexSkill>()
        dizin.children?.filter { it.isDirectory }?.forEach { skillDizini ->
            val skillDosyasi = skillDizini.findChild("SKILL.md") ?: return@forEach
            val aciklama = frontmatterAciklamaCikar(skillDosyasi)
            skiller.add(CodexSkill(
                name = skillDizini.name,
                invokeToken = "\$${skillDizini.name}",
                description = aciklama
            ))
        }
        return skiller
    }

    /** YAML frontmatter'dan description alanini cikarir */
    private fun frontmatterAciklamaCikar(dosya: VirtualFile): String {
        return try {
            val icerik = String(dosya.contentsToByteArray(), Charsets.UTF_8)
            if (!icerik.startsWith("---")) return ""

            val frontmatterSonu = icerik.indexOf("---", 3)
            if (frontmatterSonu < 0) return ""

            val frontmatter = icerik.substring(3, frontmatterSonu)
            val eslesen = Regex("""description:\s*(?:\|?\s*\n\s+)?(.+)""")
                .find(frontmatter)
            eslesen?.groupValues?.get(1)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}

/** Codex skill kategorisi */
enum class CodexSkillCategory(val displayName: String) {
    GLOBAL("Global"),
    PROJECT("Project")
}

/** Bir kategorideki Codex skill grubu */
data class CodexSkillGroup(
    val category: CodexSkillCategory,
    val skills: List<CodexSkill>
)

/** Tespit edilen tek bir Codex skill */
data class CodexSkill(
    val name: String,
    val invokeToken: String,
    val description: String
)
