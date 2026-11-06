BLACKMAGIC GAMER SPACE v0.6.0 — PROJETO ANDROID NATIVO

IMPORTANTE: este ZIP é um projeto Android Gradle com Java + WebView. Não é um pacote HTML simples. Um conversor que aceita somente HTML (como modo web do WZH 2 APK) não compilará os serviços Android nativos. Para compilar, use um ambiente que aceite projeto Android Gradle/Android Studio ou Gradle compatível.

RECURSOS INCLUÍDOS:
- Lista os aplicativos que possuem atividade de inicialização instalada; identifica como jogo quando o Android os classifica como CATEGORY_GAME.
- Abre o aplicativo escolhido.
- Solicita permissão Android de sobreposição e inicia botão flutuante BM arrastável.
- O botão flutuante abre o painel de jogos.
- Serviço em segundo plano com notificação persistente enquanto permitido pelo Android.
- Abre o fluxo de autorização de exceção da otimização de bateria.
- Lê RAM disponível, nível e temperatura da bateria.

LIMITAÇÕES:
- Alguns jogos podem não ser classificados como CATEGORY_GAME, mas aparecem na lista geral de apps com atalho.
- O Android pode restringir serviços em segundo plano e sobreposição conforme versão/fabricante e economia de bateria.
- CPU/GPU e FPS real de outros jogos não são expostos de forma geral a apps comuns. Não há overclock, limpeza forçada de RAM ou alteração de desempenho nesta versão.
- A solicitação de exceção de bateria é apresentada ao usuário; o app não pode conceder a si mesmo essa permissão.

ESTRUTURA:
settings.gradle, build.gradle, gradle.properties, app/build.gradle, app/src/main/AndroidManifest.xml, código Java e assets HTML/CSS/JS.


Personalização visual v0.7: o papel de parede preto com o anel branco enviado pelo usuário está aplicado como fundo real da interface, com camada escura translúcida para manter a leitura. Paleta em preto, branco e cinza.


## Orientação da tela v0.8
A tela principal do aplicativo está bloqueada em modo paisagem (deitado) pelo atributo android:screenOrientation="landscape" no AndroidManifest.xml. O layout também usa colunas adicionais em telas horizontais.


## Atualização v0.9 — abertura animada
Ao iniciar o aplicativo, aparece a imagem GAME TURBO fornecida pelo usuário como tela de abertura. A imagem faz uma aproximação suave (zoom), clareia e mostra uma barra de carregamento animada; depois desaparece em fade para revelar a interface BLACKMAGIC. A orientação continua fixa em paisagem.

## Versão 1.0 — vídeo MP4 real na abertura
- Arquivo `app/src/main/assets/splash_intro.mp4` incluído no pacote.
- `splash.html` reproduz o MP4 ao iniciar o aplicativo.
- Ao terminar o vídeo, abre automaticamente `index.html`.
- O atalho flutuante que abre o painel vai direto ao painel, sem repetir a abertura.
- O vídeo é um MP4 H.264 de 4 segundos, sem áudio.
