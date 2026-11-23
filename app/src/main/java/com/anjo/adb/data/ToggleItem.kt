package com.anjo.adb.data

/** Categoria = cada aba (Jogos, Boost, GPU, CPU, Áudio, Rede, Visual, Toque). */
enum class Categoria { JOGOS, BOOST, GPU, CPU, AUDIO, REDE, VISUAL, TOQUE }

/**
 * Um card de função. `precisaShizuku = false` é só pros raros casos que usam
 * API normal do Android (volume, DND) — todo o resto exige Shizuku/ADB.
 */
data class ToggleItem(
    val id: String,
    val categoria: Categoria,
    val icone: String,     // nome do drawable
    val titulo: String,
    val descricao: String,
    val precisaShizuku: Boolean = true,
    val secao: String      // agrupamento dentro da aba, ex: "ANTES DE JOGAR"
)

object ToggleRegistry {
    val todos: List<ToggleItem> = listOf(
        // ---- MOTOR INTELIGENTE ----
        ToggleItem("det_auto", Categoria.JOGOS, "ic_target", "Detecção Automática", "Turbina o jogo aberto de qualquer lugar", secao = "MOTOR INTELIGENTE"),
        ToggleItem("det_inteligente", Categoria.JOGOS, "ic_target", "Detecção Inteligente", "Acha jogos que você não adicionou", secao = "MOTOR INTELIGENTE"),
        ToggleItem("apoio_apps", Categoria.JOGOS, "ic_refresh", "Apoio nos Apps", "Ajuda os apps que você usa no dia a dia", secao = "MOTOR INTELIGENTE"),
        ToggleItem("ia_void", Categoria.JOGOS, "ic_spark", "IA VOID", "Aprende e ajusta cada jogo", secao = "MOTOR INTELIGENTE"),
        ToggleItem("fps_adaptativo", Categoria.JOGOS, "ic_spark", "FPS Adaptativo", "Aprende a melhor escala por jogo", secao = "MOTOR INTELIGENTE"),
        ToggleItem("modo_desempenho", Categoria.JOGOS, "ic_refresh", "Modo Desempenho", "Modo de jogo de desempenho do Android", secao = "MOTOR INTELIGENTE"),

        // ---- ANTES DE JOGAR (BOOST) ----
        ToggleItem("anti_travadas", Categoria.BOOST, "ic_bolt", "Anti-Travadas", "Pré-compila jogos, menos travadas", secao = "ANTES DE JOGAR"),
        ToggleItem("boost_abertura", Categoria.BOOST, "ic_rocket", "Boost de Abertura", "Carregamento mais rápido no 1º minuto", secao = "ANTES DE JOGAR"),
        ToggleItem("ajuste_instantaneo", Categoria.BOOST, "ic_sliders", "Ajuste Instantâneo", "Escala certa pra sua GPU", secao = "ANTES DE JOGAR"),
        ToggleItem("hz_sincronizado", Categoria.BOOST, "ic_hz", "Hz Sincronizado", "Hz da tela igual ao seu FPS", secao = "ANTES DE JOGAR"),
        ToggleItem("anti_travadas_auto", Categoria.BOOST, "ic_refresh", "Anti-Travadas Automático", "Jogo atualizou? Otimiza sozinho", secao = "ANTES DE JOGAR"),
        ToggleItem("espremer_ram", Categoria.BOOST, "ic_broom", "Espremer RAM", "Mais RAM livre, apps continuam abertos", secao = "ANTES DE JOGAR"),
        ToggleItem("compactar_memoria", Categoria.BOOST, "ic_snow", "Compactar Memória", "Compacta a memória ao ATIVAR", secao = "ANTES DE JOGAR"),
        ToggleItem("carregamento_turbo", Categoria.BOOST, "ic_rocket", "Carregamento Turbo", "Mapas e loading mais rápidos", secao = "ANTES DE JOGAR"),

        // ---- DURANTE O JOGO (BOOST) ----
        ToggleItem("congelar_fundo", Categoria.BOOST, "ic_snow", "Congelar 2º Plano", "Redes sociais e vídeo congelados", secao = "DURANTE O JOGO"),
        ToggleItem("pausar_sync", Categoria.BOOST, "ic_sync_off", "Pausar Sync e Busca", "Sem sync ou buscas em 2º plano", secao = "DURANTE O JOGO"),
        ToggleItem("foco_jogo", Categoria.BOOST, "ic_minus", "Foco no Jogo", "Não Perturbe durante o jogo", precisaShizuku = false, secao = "DURANTE O JOGO"),
        ToggleItem("escudo_termico", Categoria.BOOST, "ic_sun", "Escudo Térmico", "Limita o brilho quando esquenta", secao = "DURANTE O JOGO"),
        ToggleItem("turbo_tomada", Categoria.BOOST, "ic_battery", "Turbo na Tomada", "Clock total no carregador", secao = "DURANTE O JOGO"),
        ToggleItem("sem_freio_termico", Categoria.BOOST, "ic_thermo", "Sem Freio Térmico no Jogo", "Jogo não corta gráfico no calor", secao = "DURANTE O JOGO"),
        ToggleItem("congelar_sistema_marca", Categoria.BOOST, "ic_snow", "Congelar Sistema da Marca", "Serviços da marca esperam a partida", secao = "DURANTE O JOGO"),
        ToggleItem("gps_off", Categoria.BOOST, "ic_gps_off", "GPS Off no Jogo", "Poupa CPU e bateria", secao = "DURANTE O JOGO"),
        ToggleItem("sem_sobreposicoes", Categoria.BOOST, "ic_minus", "Sem Sobreposições", "FPS mais firme", secao = "DURANTE O JOGO"),
        ToggleItem("sem_ok_google", Categoria.BOOST, "ic_mic_off", "Sem \"Ok Google\" no Jogo", "Assistente para de escutar", secao = "DURANTE O JOGO"),
        ToggleItem("sem_realce_video", Categoria.BOOST, "ic_palette", "Sem Realce de Vídeo da Marca", "GPU para de gastar à toa", secao = "DURANTE O JOGO"),
        ToggleItem("apps_fundo_restritos", Categoria.BOOST, "ic_restrict", "Apps de Fundo Restritos", "Apps pesados não acordam", secao = "DURANTE O JOGO"),
        ToggleItem("jogo_offline_ia", Categoria.BOOST, "ic_target", "Jogo Offline (IA)", "Sem internet nos jogos que não usam", secao = "DURANTE O JOGO"),
        ToggleItem("sensores_so_jogo", Categoria.BOOST, "ic_sensor", "Sensores Só pro Jogo", "Outros apps param de ler os sensores", secao = "DURANTE O JOGO"),
        ToggleItem("grafico_leve_bateria", Categoria.BOOST, "ic_battery", "Gráfico Leve na Bateria Fraca", "Abaixo de 30%: um nível mais leve", secao = "DURANTE O JOGO"),
        ToggleItem("lista_congelamento", Categoria.BOOST, "ic_snow", "Lista de Congelamento", "Os apps que você escolher, desligados de vez", secao = "DURANTE O JOGO"),

        // ---- ESTABILIDADE NA PARTIDA (GPU) ----
        ToggleItem("detector_grafico", Categoria.GPU, "ic_target", "Detector de Gráfico", "Percebe quando você muda o gráfico do jogo", secao = "ESTABILIDADE NA PARTIDA"),
        ToggleItem("modo_grafico_pesado", Categoria.GPU, "ic_spark", "Modo Gráfico Pesado", "Força extra quando o gráfico sobe", secao = "ESTABILIDADE NA PARTIDA"),
        ToggleItem("protecao_travada", Categoria.GPU, "ic_eye", "Proteção de Travada", "Os extras pesados do VOID saem da frente", secao = "ESTABILIDADE NA PARTIDA"),
        ToggleItem("calor_suave", Categoria.GPU, "ic_thermo", "Calor Suave", "Alivia antes, sem queda forte de FPS", secao = "ESTABILIDADE NA PARTIDA"),

        // ---- CPU ----
        ToggleItem("pausar_backup", Categoria.CPU, "ic_cloud_off", "Pausar Backup", "O backup do Google fica pausado", secao = "DURANTE O JOGO"),
        ToggleItem("bloqueio_alarmes", Categoria.CPU, "ic_alarm_off", "Bloqueio de Alarmes", "Apps pesados sem tarefas agendadas", precisaShizuku = false, secao = "DURANTE O JOGO"),
        ToggleItem("pausar_compilacao", Categoria.CPU, "ic_pause", "Pausar Compilação", "Sem otimizar apps na partida", secao = "DURANTE O JOGO"),
        ToggleItem("bloqueio_despertar", Categoria.CPU, "ic_alarm_off", "Bloqueio de Despertar", "Apps pesados não acordam a CPU", secao = "DURANTE O JOGO"),
        ToggleItem("pausar_loja", Categoria.CPU, "ic_store", "Pausar Loja", "Sem atualização no jogo", secao = "DURANTE O JOGO"),
        ToggleItem("acessibilidade_pausada", Categoria.CPU, "ic_target", "Acessibilidade Pausada", "Apps que leem a tela pausam", secao = "DURANTE O JOGO"),
        ToggleItem("otimizacao_noturna", Categoria.CPU, "ic_moon", "Otimização Noturna", "Otimiza apps carregando", secao = "SEMPRE ATIVO"),
        ToggleItem("armazenamento_rapido", Categoria.CPU, "ic_store", "Armazenamento Rápido", "Carrega mais rápido, mesmo cheio", secao = "SEMPRE ATIVO"),
        ToggleItem("limpar_cache_geral", Categoria.CPU, "ic_trash", "Limpar Cache Geral", "Quando a memória estiver quase cheia", secao = "SEMPRE ATIVO"),
        ToggleItem("sistema_mais_leve", Categoria.CPU, "ic_feather", "Sistema Mais Leve", "Barra e tela inicial sem engasgo", secao = "SEMPRE ATIVO"),
        ToggleItem("congelador_cache", Categoria.CPU, "ic_snow", "Congelador de Cache", "Apps minimizados param", secao = "SEMPRE ATIVO"),
        ToggleItem("limite_processos", Categoria.CPU, "ic_restrict", "Limite de Processos", "Menos apps escondidos", secao = "SEMPRE ATIVO"),
        ToggleItem("proibidos_fundo", Categoria.CPU, "ic_restrict", "Proibidos em Segundo Plano", "Os apps que você escolher só rodam abertos", secao = "SEMPRE ATIVO"),
        ToggleItem("doze_leve", Categoria.CPU, "ic_battery", "Doze Leve Imediato", "Economia leve logo que a tela apaga", secao = "SEMPRE ATIVO"),
        ToggleItem("sem_inicio_automatico", Categoria.CPU, "ic_minus", "Sem Início Automático de Apps", "Apps pesados não abrem sozinhos", secao = "SEMPRE ATIVO"),
        ToggleItem("sem_pre_carregamento", Categoria.CPU, "ic_rocket", "Sem Pré-carregamento", "O sistema para de adivinhar apps", secao = "SEMPRE ATIVO"),

        // ---- ÁUDIO ----
        ToggleItem("passos_altos", Categoria.AUDIO, "ic_footstep", "Passos Altos", "Realça passos e recarga", secao = "SOM DO JOGO"),
        ToggleItem("graves_fortes", Categoria.AUDIO, "ic_speaker", "Graves Fortes", "Explosões e música mais graves", secao = "SOM DO JOGO"),
        ToggleItem("agudos_cristalinos", Categoria.AUDIO, "ic_wave", "Agudos Cristalinos", "Tiros e passos mais definidos", secao = "SOM DO JOGO"),
        ToggleItem("clareza_voz", Categoria.AUDIO, "ic_voice", "Clareza de Voz", "Amigos mais claros no chat", secao = "SOM DO JOGO"),
        ToggleItem("volume_extra", Categoria.AUDIO, "ic_vol_up", "Volume Extra", "Até +6 dB acima do máximo", secao = "SOM DO JOGO"),
        ToggleItem("limitador", Categoria.AUDIO, "ic_limiter", "Limitador", "O som nunca distorce", secao = "SOM DO JOGO"),
        ToggleItem("compressor", Categoria.AUDIO, "ic_compress", "Compressor", "Sons baixos sobem, estrondos descem", secao = "SOM DO JOGO"),
        ToggleItem("som_3d", Categoria.AUDIO, "ic_headphones", "Som 3D no Fone", "Som mais aberto, fácil de localizar", secao = "SOM DO JOGO"),
        ToggleItem("volume_automatico", Categoria.AUDIO, "ic_vol_auto", "Volume Automático", "Jogo abre no seu volume", precisaShizuku = false, secao = "VOLUME E SILÊNCIO"),
        ToggleItem("sem_sons_sistema", Categoria.AUDIO, "ic_mute", "Sem Sons do Sistema", "Sem notificações nem som de bloqueio", secao = "VOLUME E SILÊNCIO"),

        // ---- REDE ----
        ToggleItem("sem_teste_conexao", Categoria.REDE, "ic_wifi", "Sem Teste de Conexão", "O Wi-Fi não cai por engano", secao = "DURANTE O JOGO"),
        ToggleItem("tcp_turbo", Categoria.REDE, "ic_wifi", "TCP Turbo", "Funciona até o Android 11", secao = "DURANTE O JOGO"),
        ToggleItem("travar_wifi", Categoria.REDE, "ic_wifi_lock", "Travar no Wi-Fi", "Dados móveis off quando no Wi-Fi", secao = "DURANTE O JOGO"),
        ToggleItem("sem_troca_rede", Categoria.REDE, "ic_swap", "Sem Troca de Rede", "Fica no Wi-Fi quando ele oscila", secao = "DURANTE O JOGO"),
        ToggleItem("internet_pro_jogo", Categoria.REDE, "ic_refresh", "Internet pro Jogo", "Corta dados de apps pesados no fundo", secao = "DURANTE O JOGO"),
        ToggleItem("economia_so_jogo", Categoria.REDE, "ic_target", "Economia Só pro Jogo", "Todos os apps esperam, menos o jogo", secao = "DURANTE O JOGO"),
        ToggleItem("pausar_nuvem", Categoria.REDE, "ic_cloud_off", "Pausar Nuvem", "Sem backup de fotos no jogo", secao = "DURANTE O JOGO"),
        ToggleItem("bluetooth_off", Categoria.REDE, "ic_bluetooth_off", "Bluetooth Off no Jogo", "Só quando não tem fone conectado", secao = "DURANTE O JOGO"),

        // ---- VISUAL ----
        ToggleItem("texturas_bonitas", Categoria.VISUAL, "ic_palette", "Texturas Bonitas", "Realce de cor e nitidez na tela", secao = "QUALIDADE VISUAL"),
        ToggleItem("hdr_boost", Categoria.VISUAL, "ic_sun", "HDR Boost", "Contraste mais forte em jogo compatível", secao = "QUALIDADE VISUAL"),
        ToggleItem("anti_serrilhado", Categoria.VISUAL, "ic_wave", "Anti-Serrilhado", "Bordas mais lisas na tela", secao = "QUALIDADE VISUAL"),

        // ---- TOQUE ----
        ToggleItem("toque_responsivo", Categoria.TOQUE, "ic_touch", "Toque Mais Responsivo", "Reduz atraso entre o dedo e a tela", secao = "SENSIBILIDADE"),
        ToggleItem("anti_toque_fantasma", Categoria.TOQUE, "ic_touch", "Anti Toque Fantasma", "Ignora toque acidental na borda", secao = "SENSIBILIDADE"),
        ToggleItem("vibrar_acerto", Categoria.TOQUE, "ic_vibrate", "Vibrar no Acerto", "Feedback tátil em tiros e combos", precisaShizuku = false, secao = "SENSIBILIDADE"),
    )

    fun porCategoria(c: Categoria) = todos.filter { it.categoria == c }
}
