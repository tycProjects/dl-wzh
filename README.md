# BLACKMAGIC PS5 PRO Launcher v2.12

Projeto-fonte Android (Java). Este ZIP **não é um APK pronto**.

## O que está incluído
- Biblioteca dos aplicativos com atividade de inicialização.
- Botão para abrir o aplicativo/jogo original instalado.
- Serviço de painel flutuante com opção de arrastar e fechar.
- Atalho para a configuração de sobreposição e Acessibilidade.

## Limitações importantes
- Os botões do painel são uma base experimental: no momento, eles enviam um toque de demonstração ao centro inferior da tela. Não há mapeamento configurável por jogo nem garantia de controle em jogos.
- Acessibilidade precisa ser ativada manualmente em Configurações do Android.
- Alguns jogos bloqueiam sobreposições ou gestos; Android e políticas de cada jogo podem limitar o funcionamento.
- Isto não executa jogos de PlayStation nem converte jogos de PC/console em jogos Android.

## Compilar
1. Abra a pasta do projeto no Android Studio.
2. Use JDK 17 e instale Android SDK Platform 35.
3. Aguarde a sincronização do Gradle.
4. Execute **Build > Build APK(s)**.
5. O APK de depuração costuma aparecer em `app/build/outputs/apk/debug/app-debug.apk`.

Para compilar em Termux será necessário um ambiente Android/Gradle configurado e acesso ao SDK. O projeto não inclui o Android SDK nem o Gradle Wrapper.
