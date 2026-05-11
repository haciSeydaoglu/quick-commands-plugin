package com.quickcommands.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.nio.file.Path

/**
 * Agent skill dosyalarini otomatik tespit eder.
 * Kaynaklari: ~/.agents/skills/ (global), <proje>/.agents/skills/ (proje)
 * 70%+ Claude ile yazildi
 */
@Service(Service.Level.PROJECT)
class AgentSkillDetectionService(private val project: Project) {

    companion object {
        private val HOME_DIR: Path = Path.of(System.getProperty("user.home"))
        private const val CACHE_SURESI_MS = 30_000L

        fun getInstance(project: Project): AgentSkillDetectionService {
            return project.getService(AgentSkillDetectionService::class.java)
        }
    }

    @Volatile
    private var kirli = true

    private var onbellek: List<AgentSkillGroup> = emptyList()
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
                        dosyaYolu.contains("/.agents/skills/") &&
                                (dosyaYolu.endsWith(".md") || dosyaYolu.endsWith(".yaml"))
                    }
                    if (ilgiliDegisiklikVar) {
                        kirli = true
                    }
                }
            })
    }

    /** Tespit edilen skill gruplarini dondurur. Cache suresi dolmussa yeniden tarar. */
    fun getDetectedSkills(): List<AgentSkillGroup> {
        val simdi = System.currentTimeMillis()
        if (kirli || (simdi - sonTaramaZamani > CACHE_SURESI_MS)) {
            onbellek = tara()
            kirli = false
            sonTaramaZamani = simdi
        }
        return onbellek
    }

    private fun tara(): List<AgentSkillGroup> {
        val gruplar = mutableListOf<AgentSkillGroup>()

        val globalSkiller = globalSkilleriTara().sortedBy { it.name.lowercase() }
        if (globalSkiller.isNotEmpty()) {
            gruplar.add(AgentSkillGroup(category = AgentSkillCategory.GLOBAL, skills = globalSkiller))
        }

        val projeSkiller = projeSkilleriTara().sortedBy { it.name.lowercase() }
        if (projeSkiller.isNotEmpty()) {
            gruplar.add(AgentSkillGroup(category = AgentSkillCategory.PROJECT, skills = projeSkiller))
        }

        return gruplar
    }

    /** ~/.agents/skills/ dizinindeki global skilleri tarar */
    private fun globalSkilleriTara(): List<AgentSkill> {
        return skillDizinleriTara(HOME_DIR.resolve(".agents/skills"))
    }

    /** <proje>/.agents/skills/ dizinindeki proje skillerini tarar */
    private fun projeSkilleriTara(): List<AgentSkill> {
        val basePath = project.basePath ?: return emptyList()
        return skillDizinleriTara(Path.of(basePath).resolve(".agents/skills"))
    }

    /** Bir filesystem yolundaki skill dizinlerini refresh ederek tarar */
    private fun skillDizinleriTara(yol: Path): List<AgentSkill> {
        val dizin = LocalFileSystem.getInstance()
            .refreshAndFindFileByNioFile(yol) ?: return emptyList()
        return skillDizinleriTara(dizin)
    }

    /** Bir skills dizinindeki SKILL.md dosyalarini tarar */
    private fun skillDizinleriTara(dizin: VirtualFile): List<AgentSkill> {
        val skiller = mutableListOf<AgentSkill>()
        dizin.children?.filter { it.isDirectory }?.forEach { skillDizini ->
            val skillDosyasi = skillDizini.findChild("SKILL.md") ?: return@forEach
            val aciklama = frontmatterAciklamaCikar(skillDosyasi)
            skiller.add(AgentSkill(
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

/** Agent skill kategorisi */
enum class AgentSkillCategory(val displayName: String) {
    GLOBAL("Global"),
    PROJECT("Project")
}

/** Bir kategorideki Agent skill grubu */
data class AgentSkillGroup(
    val category: AgentSkillCategory,
    val skills: List<AgentSkill>
)

/** Tespit edilen tek bir Agent skill */
data class AgentSkill(
    val name: String,
    val invokeToken: String,
    val description: String
)
