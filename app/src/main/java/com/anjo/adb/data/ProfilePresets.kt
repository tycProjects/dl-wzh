package com.anjo.adb.data

object ProfilePresets {
    val EXTREMO = setOf(
        "det_auto", "ia_void", "fps_adaptativo", "anti_travadas", "boost_abertura",
        "espremer_ram", "compactar_memoria", "congelar_fundo", "pausar_sync",
        "foco_jogo", "sem_freio_termico", "gps_off", "sem_sobreposicoes",
        "limite_processos", "sem_pre_carregamento"
    )
    val EQUILIBRADO = setOf(
        "det_auto", "ia_void", "anti_travadas", "espremer_ram",
        "congelar_fundo", "foco_jogo", "gps_off", "sem_sobreposicoes"
    )
    val BATERIA = setOf(
        "det_auto", "modo_desempenho", "congelar_fundo", "pausar_sync",
        "foco_jogo", "grafico_leve_bateria", "doze_leve", "sem_inicio_automatico"
    )

    fun porNome(nome: String): Set<String> = when (nome) {
        "EXTREMO" -> EXTREMO
        "EQUILIBRADO" -> EQUILIBRADO
        "BATERIA" -> BATERIA
        else -> emptySet() // PERSONAL = o que a pessoa já tiver ligado
    }

    /** Aplica o preset: liga os do perfil, desliga os de outros perfis que não estão no novo. */
    fun aplicar(repo: SettingsRepository, nomePerfil: String) {
        if (nomePerfil == "PERSONAL") return
        val desejado = porNome(nomePerfil)
        val todosDosPerfis = EXTREMO + EQUILIBRADO + BATERIA
        for (id in todosDosPerfis) {
            repo.setAtivo(id, id in desejado)
        }
    }
}
