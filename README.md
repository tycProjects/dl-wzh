# AppForge 1.4 — Projeto Android

Projeto Android Java para abrir no Android Studio ou em uma IDE Android compatível.

## Requisitos
- Android Studio recente
- JDK 17
- Android SDK 35
- Conexão com a internet na primeira sincronização do Gradle

## Como gerar o APK
1. Extraia o ZIP.
2. Abra a pasta `AppForge_Android_Project` no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. O APK de depuração será gerado em `app/build/outputs/apk/debug/`.

## Conteúdo
- Interface Material 3 com temas claro e escuro
- Modelos iniciais de projeto
- Editor visual demonstrativo
- Biblioteca de componentes
- Categorias de blocos visuais
- Área avançada com snippets Java, Kotlin e XML

## Limitação importante
Esta versão é um protótipo funcional de navegação e edição demonstrativa. Componentes, blocos e trechos de código são armazenados em memória durante a sessão; ainda não são convertidos automaticamente em código-fonte de um novo app. O AppForge também não compila/exporta APKs dos projetos criados dentro dele. O APK que você compilar aqui é o próprio aplicativo AppForge.
