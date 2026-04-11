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
 * Claude Code skill ve command dosyalarini otomatik tespit eder.
 * Kaynaklari: ~/.claude/commands/, ~/.claude/skills/, .claude/commands/, ~/.claude/plugins/
 * 70%+ Claude ile yazildi
 */
@Service(Service.Level.PROJECT)
class ClaudeSkillDetectionService(private val project: Project) {

    companion object {
        private val CLAUDE_GLOBAL_DIR = "${System.getProperty("user.home")}/.claude"
        private const val CACHE_SURESI_MS = 30_000L

        fun getInstance(project: Project): ClaudeSkillDetectionService {
            return project.getService(ClaudeSkillDetectionService::class.java)
        }
    }

    @Volatile
    private var kirli = true

    private var onbellek: List<ClaudeSkillGroup> = emptyList()
    private var sonTaramaZamani = 0L

    init {
        // IDE icinden yapilan degisiklikleri aninda yakala
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
                        dosyaYolu.contains("/.claude/") &&
                                (dosyaYolu.endsWith(".md") || dosyaYolu.endsWith(".yaml"))
                    }
                    if (ilgiliDegisiklikVar) {
                        kirli = true
                    }
                }
            })
    }

    /** Tespit edilen skill gruplarini dondurur. Cache suresi dolmussa yeniden tarar. */
    fun getDetectedSkills(): List<ClaudeSkillGroup> {
        val simdi = System.currentTimeMillis()
        if (kirli || (simdi - sonTaramaZamani > CACHE_SURESI_MS)) {
            onbellek = tara()
            kirli = false
            sonTaramaZamani = simdi
        }
        return onbellek
    }

    private fun tara(): List<ClaudeSkillGroup> {
        val gruplar = mutableListOf<ClaudeSkillGroup>()

        // 1. Global (commands + skills birlesik, alfabetik)
        val globalHepsi = (globalCommandlariTara() + globalSkilleriTara())
            .sortedBy { it.name.lowercase() }
        if (globalHepsi.isNotEmpty()) {
            gruplar.add(ClaudeSkillGroup(category = ClaudeSkillCategory.GLOBAL, skills = globalHepsi))
        }

        // 2. Plugins (alfabetik)
        val pluginSkiller = pluginSkilleriTara().sortedBy { it.name.lowercase() }
        if (pluginSkiller.isNotEmpty()) {
            gruplar.add(ClaudeSkillGroup(category = ClaudeSkillCategory.PLUGIN, skills = pluginSkiller))
        }

        // 3. Project (commands + skills birlesik, alfabetik)
        val projeHepsi = (projeCommandlariTara() + projeSkilleriTara())
            .sortedBy { it.name.lowercase() }
        if (projeHepsi.isNotEmpty()) {
            gruplar.add(ClaudeSkillGroup(category = ClaudeSkillCategory.PROJECT, skills = projeHepsi))
        }

        return gruplar
    }

    /** .claude/commands/ dizinindeki proje komutlarini tarar */
    private fun projeCommandlariTara(): List<ClaudeSkill> {
        val basePath = project.basePath ?: return emptyList()
        val dizin = VirtualFileManager.getInstance()
            .findFileByUrl("file://$basePath/.claude/commands") ?: return emptyList()
        return mdDosyalariTara(dizin)
    }

    /** .claude/skills/ dizinindeki proje skilllerini tarar */
    private fun projeSkilleriTara(): List<ClaudeSkill> {
        val basePath = project.basePath ?: return emptyList()
        val dizin = VirtualFileManager.getInstance()
            .findFileByUrl("file://$basePath/.claude/skills") ?: return emptyList()
        return skillDizinleriTara(dizin)
    }

    /** ~/.claude/commands/ dizinindeki global komutlari tarar */
    private fun globalCommandlariTara(): List<ClaudeSkill> {
        val dizin = VirtualFileManager.getInstance()
            .findFileByUrl("file://$CLAUDE_GLOBAL_DIR/commands") ?: return emptyList()
        return mdDosyalariTara(dizin)
    }

    /** ~/.claude/skills/ dizinindeki global skilleri tarar */
    private fun globalSkilleriTara(): List<ClaudeSkill> {
        val dizin = VirtualFileManager.getInstance()
            .findFileByUrl("file://$CLAUDE_GLOBAL_DIR/skills") ?: return emptyList()
        return skillDizinleriTara(dizin)
    }

    /** Bir skills dizinindeki SKILL.md dosyalarini tarar */
    private fun skillDizinleriTara(dizin: VirtualFile): List<ClaudeSkill> {
        val skiller = mutableListOf<ClaudeSkill>()
        dizin.children?.filter { it.isDirectory }?.forEach { skillDizini ->
            val skillDosyasi = skillDizini.findChild("SKILL.md") ?: return@forEach
            val aciklama = frontmatterAciklamaCikar(skillDosyasi)
            skiller.add(ClaudeSkill(
                name = skillDizini.name,
                slashCommand = "/${skillDizini.name}",
                description = aciklama
            ))
        }
        return skiller
    }

    /** ~/.claude/plugins/cache/ dizinindeki plugin skill ve komutlarini tarar */
    private fun pluginSkilleriTara(): List<ClaudeSkill> {
        val dizin = VirtualFileManager.getInstance()
            .findFileByUrl("file://$CLAUDE_GLOBAL_DIR/plugins/cache") ?: return emptyList()
        val skiller = mutableListOf<ClaudeSkill>()

        // cache/<marketplace>/<plugin>/<version>/
        dizin.children?.filter { it.isDirectory }?.forEach { marketplaceDizini ->
            marketplaceDizini.children?.filter { it.isDirectory }?.forEach { pluginDizini ->
                val pluginAdi = pluginDizini.name
                // En son versiyon dizinini al
                val versiyonDizini = pluginDizini.children
                    ?.filter { it.isDirectory }
                    ?.maxByOrNull { it.name } ?: return@forEach

                // Skills
                versiyonDizini.findChild("skills")?.children
                    ?.filter { it.isDirectory }?.forEach { skillDizini ->
                        val skillDosyasi = skillDizini.findChild("SKILL.md") ?: return@forEach
                        val aciklama = frontmatterAciklamaCikar(skillDosyasi)
                        skiller.add(ClaudeSkill(
                            name = "$pluginAdi:${skillDizini.name}",
                            slashCommand = "/$pluginAdi:${skillDizini.name}",
                            description = aciklama
                        ))
                    }

                // Commands
                versiyonDizini.findChild("commands")?.children
                    ?.filter { !it.isDirectory && it.name.endsWith(".md") }?.forEach { cmdDosyasi ->
                        val isim = cmdDosyasi.nameWithoutExtension
                        val aciklama = frontmatterAciklamaCikar(cmdDosyasi)
                        skiller.add(ClaudeSkill(
                            name = "$pluginAdi:$isim",
                            slashCommand = "/$pluginAdi:$isim",
                            description = aciklama
                        ))
                    }
            }
        }

        return skiller
    }

    /** Bir dizindeki .md dosyalarini skill olarak tarar */
    private fun mdDosyalariTara(dizin: VirtualFile): List<ClaudeSkill> {
        return dizin.children
            ?.filter { !it.isDirectory && it.name.endsWith(".md") }
            ?.map { dosya ->
                val isim = dosya.nameWithoutExtension
                val aciklama = frontmatterAciklamaCikar(dosya)
                ClaudeSkill(
                    name = isim,
                    slashCommand = "/$isim",
                    description = aciklama
                )
            } ?: emptyList()
    }

    /** YAML frontmatter'dan description alanini cikarir */
    private fun frontmatterAciklamaCikar(dosya: VirtualFile): String {
        return try {
            val icerik = String(dosya.contentsToByteArray(), Charsets.UTF_8)
            if (!icerik.startsWith("---")) return ""

            val frontmatterSonu = icerik.indexOf("---", 3)
            if (frontmatterSonu < 0) return ""

            val frontmatter = icerik.substring(3, frontmatterSonu)
            // Basit regex ile description satirini bul
            val eslesen = Regex("""description:\s*(?:\|?\s*\n\s+)?(.+)""")
                .find(frontmatter)
            eslesen?.groupValues?.get(1)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}

/** Claude skill kategorisi */
enum class ClaudeSkillCategory(val displayName: String) {
    GLOBAL("Global"),
    PLUGIN("Plugins"),
    PROJECT("Project")
}

/** Bir kategorideki skill grubu */
data class ClaudeSkillGroup(
    val category: ClaudeSkillCategory,
    val skills: List<ClaudeSkill>
)

/** Tespit edilen tek bir Claude skill/command */
data class ClaudeSkill(
    val name: String,
    val slashCommand: String,
    val description: String
)
