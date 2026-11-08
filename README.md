# GPT Chess Analyzer — MB.Z71

Projeto Android Studio de análise independente de posições de xadrez.

## O que já está incluído
- Interface escura adaptada para Android.
- Tabuleiro que renderiza uma posição FEN.
- Campo para colar FEN de uma posição.
- Controle de profundidade de análise.
- Integração UCI preparada para executar Stockfish localmente.
- Não se conecta, não injeta código e não altera jogos de terceiros.

## O que falta para executar análise real
O executável Stockfish não é distribuído neste ZIP. Obtenha uma compilação oficial compatível com a arquitetura do aparelho e coloque-a em:

`app/src/main/assets/stockfish`

O arquivo deve ser executável Linux/Android compatível com o ABI do dispositivo. Uma compilação Android/ARM64 é normalmente a escolha adequada para aparelhos ARM64. Não basta renomear um APK ou arquivo de outra plataforma.

Se o binário não estiver presente, o app exibirá uma mensagem de erro explicando isso.

## Como compilar
1. Abra a pasta no Android Studio.
2. Use JDK 17 e Android SDK 35.
3. Aguarde a sincronização do Gradle.
4. Adicione o executável Stockfish no caminho acima.
5. Compile em Build > Build APK(s).

## Inserir uma posição
Cole uma FEN completa no campo. A posição inicial já vem preenchida:
`rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1`

## Observações técnicas
- Este é um projeto-fonte, não um APK compilado.
- O reconhecimento automático de tabuleiro, overlay sobre outros apps e integração Shizuku não fazem parte desta versão.
- Para uma versão de estudo, use posições próprias, análise offline e revisão depois das partidas.
- Stockfish é distribuído sob GPLv3. Ao redistribuir o executável, inclua a licença e cumpra as obrigações de disponibilização do código-fonte correspondente.
