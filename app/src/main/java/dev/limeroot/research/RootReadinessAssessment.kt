package dev.limeroot.research

private data class KernelVersion(val major:Int,val minor:Int,val patch:Int):Comparable<KernelVersion>{
    override fun compareTo(other:KernelVersion):Int=compareValuesBy(this,other,KernelVersion::major,KernelVersion::minor,KernelVersion::patch)
    fun atLeast(major:Int,minor:Int,patch:Int=0)=compareTo(KernelVersion(major,minor,patch))>=0
}
enum class AssessmentTone{POTENTIAL,UNKNOWN,LOW}
data class VulnerabilityAssessment(val cveId:String,val title:String,val versionAssessment:String,val conditions:String,val conclusion:String,val sourceUrl:String,val tone:AssessmentTone)

object RootReadinessAssessment{
    fun evaluate(device:DeviceDetails):List<VulnerabilityAssessment>{
        val version=parseKernelVersion(device.kernel)
        val label=version?.let{"${it.major}.${it.minor}.${it.patch}"}?:"версия не распознана"
        val ghostMatch=version?.atLeast(2,6,39)
        val futex=configEnabled(device,"CONFIG_FUTEX")
        val ghostConditions=when(futex){
            true->"CONFIG_FUTEX включён. Это предпосылка для соответствующего futex/rtmutex пути, но не подтверждение достижимости уязвимого сценария."
            false->"CONFIG_FUTEX явно выключен в доступной конфигурации; применимость этой ветки выглядит ниже."
            null->"CONFIG_FUTEX и фактическая доступность futex PI не подтверждены: конфигурация ядра недоступна или неполная."
        }
        val ghostVersion=when(ghostMatch){
            true->"Ядро $label старше версии появления проблемы (2.6.39). Для ветки 4.19 отдельный upstream fixed build в опубликованном списке не указан."
            false->"Ядро $label ниже версии появления проблемы 2.6.39."
            null->"Не удалось разобрать строку версии ядра: ${device.kernel}."
        }
        val ghostTone=when{ghostMatch==false||futex==false->AssessmentTone.LOW;ghostMatch==true->AssessmentTone.POTENTIAL;else->AssessmentTone.UNKNOWN}
        val ghostConclusion=when{
            ghostMatch==false->"По номеру версии ядра совпадения с опубликованным диапазоном нет."
            futex==false->"Версия пересекается с диапазоном, но доступная конфигурация показывает CONFIG_FUTEX=n. Это снижает применимость."
            ghostMatch==true->"Есть потенциальное совпадение. Исправление могло быть перенесено Xiaomi в прошивку без изменения номера 4.19.157; статус vendor-патча неизвестен. Эксплуатируемость на этом телефоне не подтверждена."
            else->"Недостаточно данных для оценки."
        }

        val dirtyMatch=version?.atLeast(4,11,0)
        val xfrm=configEnabled(device,"CONFIG_XFRM_USER")
        val esp4=configEnabled(device,"CONFIG_INET_ESP")
        val esp6=configEnabled(device,"CONFIG_INET6_ESP")
        val rxrpc=configEnabled(device,"CONFIG_AF_RXRPC")
        val userNs=configEnabled(device,"CONFIG_USER_NS")
        val nsMax=device.userNamespacesMax.toIntOrNull()
        val espPath=xfrm==true&&(esp4==true||esp6==true)
        val rxrpcPath=rxrpc==true
        val pathsOff=xfrm==false&&esp4==false&&esp6==false&&rxrpc==false
        val configUnknown=listOf(xfrm,esp4,esp6,rxrpc).any{it==null}
        val nsText=when{
            userNs==false||nsMax==0->"Создание user namespaces выглядит отключённым по доступной настройке (CONFIG_USER_NS=n или max_user_namespaces=0)."
            userNs==true&&nsMax!=null&&nsMax>0->"User namespaces скомпилированы и лимит max_user_namespaces=$nsMax; это не доказывает, что обычному приложению разрешено создавать user/network namespaces."
            else->"Доступность непривилегированных user/network namespaces не установлена."
        }
        val dirtyConditions=when{
            pathsOff->"Доступная конфигурация показывает CONFIG_XFRM_USER, ESP и RXRPC выключенными. Основные известные пути DirtyFrag по этим признакам не видны."
            espPath&&rxrpcPath->"В конфигурации видны XFRM/ESP и RXRPC. Для ESP-варианта дополнительно важна возможность создавать непривилегированные user/network namespaces. $nsText"
            espPath->"В конфигурации видны XFRM_USER и ESP. Для ESP-варианта также требуется возможность создавать непривилегированные user/network namespaces. $nsText"
            rxrpcPath->"В конфигурации виден RXRPC-путь. Фактическая доступность/загрузка RXRPC и все условия эксплуатации не подтверждены."
            configUnknown->"Не удалось полностью прочитать CONFIG_XFRM_USER, CONFIG_INET_ESP, CONFIG_INET6_ESP и CONFIG_AF_RXRPC. $nsText"
            else->"Конфигурация не показывает подтверждённого ESP/RXRPC пути, но неполные данные и особенности vendor-ядра не исключены. $nsText"
        }
        val dirtyVersion=when(dirtyMatch){
            true->"Ядро $label старше версии появления проблемы (4.11). Исправление upstream указано для некоторых более новых веток; для 4.19 vendor backport не проверен."
            false->"Ядро $label ниже версии появления проблемы 4.11."
            null->"Не удалось разобрать строку версии ядра: ${device.kernel}."
        }
        val dirtyTone=when{dirtyMatch==false||pathsOff->AssessmentTone.LOW;dirtyMatch==true->AssessmentTone.POTENTIAL;else->AssessmentTone.UNKNOWN}
        val dirtyConclusion=when{
            dirtyMatch==false->"По номеру версии ядра совпадения с опубликованным диапазоном нет."
            pathsOff->"Версия пересекается с диапазоном, но доступная конфигурация не показывает основные ESP/RXRPC компоненты. Это снижает применимость."
            dirtyMatch==true->"Есть потенциальное совпадение по версии ядра. Для подтверждения нужны конфигурация, runtime-доступность нужного сетевого пути и проверка Xiaomi-патчей. Root не гарантирован."
            else->"Недостаточно данных для оценки."
        }
        return listOf(
            VulnerabilityAssessment("CVE-2026-43499","GhostLock · futex/rtmutex",ghostVersion,ghostConditions,ghostConclusion,"https://www.cve.org/CVERecord?id=CVE-2026-43499",ghostTone),
            VulnerabilityAssessment("CVE-2026-43284","DirtyFrag · XFRM/ESP и RXRPC",dirtyVersion,dirtyConditions,dirtyConclusion,"https://kernel.googlesource.com/pub/scm/linux/security/vulns/+/refs/heads/master/cve/published/2026/CVE-2026-43284.mbox",dirtyTone)
        )
    }
    private fun parseKernelVersion(raw:String):KernelVersion?{
        val match=Regex("""(?<!\d)(\d+)\.(\d+)\.(\d+)""").find(raw)?:return null
        return runCatching{KernelVersion(match.groupValues[1].toInt(),match.groupValues[2].toInt(),match.groupValues[3].toInt())}.getOrNull()
    }
    private fun configEnabled(device:DeviceDetails,key:String):Boolean?=when(device.kernelConfig[key]?.lowercase()){
        "y","m"->true
        "n"->false
        else->null
    }
}