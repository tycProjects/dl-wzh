# JoyArk Queue Skip - mod menu flutuante

Overlay com bolha arrastavel para pular a fila do JoyArk Cloud Gaming.

Pacote alvo: com.joyark.cloudgames.community
minSdk 24 / targetSdk 34 / Java puro / sem dependencias externas

## Como o pulo funciona

1. O Servico de Acessibilidade le a tela de fila (posicao, tempo, nome do jogo).
2. O botao manda o JoyArk para background (acao HOME).
3. killBackgroundProcesses descarta a sessao de fila no servidor.
4. O app relanca o JoyArk pedindo slot novo (launcher ou deep link salvo).
5. Verifica se saiu da fila e repete ate o limite.

Modos: SUAVE (home + volta), AGRESSIVO (home + kill + relaunch), DEEP LINK (usa a URI salva).

## Compilar

1. Descompacte.
2. Abra a pasta no Android Studio.
3. Build > Build APK(s).

## Usar

1. Instale o APK.
2. Permita "Sobrepor outros apps".
3. Ative acessibilidade > JoyArk Queue Reader.
4. Inicie o menu, abra o JoyArk e entre na fila.
5. Toque na bolha e em PULAR FILA.

## Ajustes

- ESPERA: intervalo entre tentativas.
- TENT: numero de tentativas.
- CAPTURAR ALVO: trava o jogo atual como alvo.
- CLOUD PC: marca o alvo como Cloud PC.

## Observacao

O comportamento do servidor muda com as atualizacoes do JoyArk. Se a fila voltar a
segurar, aumente as tentativas, use o modo AGRESSIVO e reduza a ESPERA.
