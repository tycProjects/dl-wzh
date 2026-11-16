# Speedrun Timer

Projeto Android nativo em Kotlin + Jetpack Compose / Material 3.

## Recursos
- Cronômetro com centésimos de segundo.
- Switch Timer para iniciar/pausar, botão Stop Timer para parar mantendo o tempo e Reset Timer para zerar.
- Sobreposição flutuante com iniciar, pausar, zerar e fechar.
- Janela flutuante arrastável sobre outros aplicativos.

## Como compilar no AndroidIDE
1. Extraia o ZIP.
2. Abra a pasta `SpeedrunTimer` no AndroidIDE como projeto Gradle.
3. Aguarde a sincronização. Use JDK 17 e compile com Android SDK 35 instalado.
4. Execute `:app:assembleDebug` ou use a opção de compilar APK.
5. O APK de teste ficará em `app/build/outputs/apk/debug/app-debug.apk`.

## Permissão da sobreposição
Na primeira vez, toque em **Ativar sobreposição** e habilite "Exibir sobre outros apps" para Speedrun Timer. Volte ao app e toque novamente no botão.

Observação: o Android pode mostrar uma notificação persistente enquanto a sobreposição estiver ativa. Isso é esperado para manter o serviço em execução.


## Atalhos de teclado Bluetooth

Na tela **Configurações e customização**, abra **Configurações de teclado** para associar uma tecla a cada ação da sobreposição: pausar/continuar, zerar, voltar ao aplicativo e fechar a sobreposição. Toque em **Definir** e pressione a tecla desejada. Cada tecla pode ficar vinculada a apenas uma ação; ao reutilizá-la, a associação anterior é removida.

Para receber atalhos enquanto outro aplicativo está aberto, o Android exige que a janela flutuante mantenha o foco do teclado. Quando houver atalhos configurados, isso pode impedir a digitação em outros aplicativos enquanto a sobreposição estiver ativa. Sem atalhos configurados, a sobreposição não solicita o foco do teclado.


## Novidades da versão 1.2
- Temas do aplicativo: Sistema, Escuro e Branco.
- Personalização da sobreposição: Padrão, Texto e Liquid Glass.
- Liquid Glass usa aparência translúcida e azul no timer ativo.
- Preferências de tema e estilo são salvas no dispositivo.
