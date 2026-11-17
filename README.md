# Jelly Bean Launcher — projeto Android

Projeto inicial de launcher Android com aparência inspirada no Android 4.1 Jelly Bean.

## Como compilar
- Abra a pasta `JellyBeanLauncher` no Android Studio ou em um ambiente AIDE compatível com projetos Gradle.
- Sincronize o Gradle e compile o módulo `app`.
- Instale o APK e escolha **Jelly Bean Launcher** como aplicativo de tela inicial padrão.

## Recursos incluídos
- Atividade registrada como launcher HOME.
- Grade dos aplicativos que possuem atividade de inicialização.
- Dock com atalhos para Telefone, SMS, Câmera e Navegador.
- O app SMS padrão do Android recebe o ícone temático de SMS, mesmo que o Google Mensagens não esteja instalado.
- O discador padrão recebe o ícone temático de telefone.
- Correspondências simples por categoria para câmera, navegador, configurações, galeria e música.
- Apps sem correspondência mantêm seus ícones originais.

## Observações importantes
- Este é um **projeto-fonte em ZIP**, não um APK já compilado.
- O sistema Android não oferece uma API universal para descobrir o ícone semântico de todo app padrão. SMS e discador são identificados por APIs do sistema; outras categorias usam correspondência de pacote e mantêm o ícone original quando não há correspondência.
- O visual é uma base inspirada no Jelly Bean, não uma cópia exata do launcher original. Widgets, pastas, gestos avançados, edição de páginas e busca ainda podem ser adicionados em versões futuras.
- A permissão QUERY_ALL_PACKAGES é usada para listar apps instalados. Distribuição pela Play Store pode exigir justificativa de uso.


## Ícone do aplicativo
O ícone do launcher foi substituído pela imagem enviada pelo usuário, em PNG nas densidades mipmap do Android. O Manifest aponta para `@mipmap/ic_launcher`.
