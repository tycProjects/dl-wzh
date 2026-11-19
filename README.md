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


## Tela inicial estilo Android Jelly Bean
A tela inicial foi reorganizada para seguir a referência enviada: barra de pesquisa Google no topo, relógio analógico central, dois atalhos na área inferior, dock com cinco posições e fundo geométrico laranja/amarelo. A gaveta de aplicativos abre pelo botão central do dock. Os ícones foram mantidos simples/provisórios, conforme solicitado para focar primeiro no layout.


## Widgets removíveis
A barra de pesquisa, o relógio analógico e a área dos dois atalhos são tratados como widgets independentes na tela inicial. O usuário pode pressionar e segurar cada um para removê-lo. O dock inferior e o botão da gaveta de aplicativos permanecem fixos para navegação.


## Ícones Jelly Bean fornecidos pelo usuário
Os ícones PNG do arquivo `android_jelly_bean.zip` foram incorporados em `app/src/main/res/drawable-nodpi/`. O dock, os atalhos Google/Play Store e os aplicativos reconhecidos na gaveta usam agora os PNGs fornecidos. Apps sem correspondência mantêm o ícone original do próprio aplicativo. O SMS e o telefone padrão usam os ícones Jelly Bean correspondentes ao papel padrão do sistema.


## Segundo pacote de ícones e identificação automática
Foram adicionados 1570 PNGs válidos do arquivo `jelly bean icon 2.zip` (arquivos com nomes de recursos Android válidos), preservando também o primeiro pacote. Os novos recursos usam o prefixo interno `jb2_` para evitar conflitos.

Na gaveta, o launcher tenta identificar o ícone correspondente pelo nome exibido do app e por partes específicas do pacote Android, além de aliases para alguns aplicativos conhecidos. Quando encontra correspondência, aplica o ícone do pacote; caso contrário, mantém o ícone original para não atribuir uma imagem errada. SMS e telefone padrão continuam com prioridade própria.


## Gaveta Jelly Bean e presença como aplicativo
A gaveta foi redesenhada com fundo preto, abas APPS / DOWNLOADED / WIDGETS, indicador azul sob a aba selecionada, menu de opções e grade de quatro colunas com ícones e nomes brancos. APPS mostra os apps iniciáveis; DOWNLOADED filtra apps não marcados como aplicativos do sistema. A aba WIDGETS tem uma tela visual inicial.

O manifesto declara entradas HOME/DEFAULT para que o Android ofereça o launcher como opção padrão e MAIN/LAUNCHER para que ele também apareça como aplicativo normal no launcher atual mesmo quando não for o padrão.


## Botão da gaveta atualizado
O botão central que abre a gaveta de aplicativos usa agora a imagem neon azul enviada pelo usuário (anel circular luminoso com seis quadrados). O clique continua abrindo a gaveta de aplicativos; apenas a arte do botão foi substituída.


## Atualização inspirada no vídeo enviado
A tela inicial foi reorganizada para o estilo clássico mostrado no vídeo: papel de parede de trigo extraído do próprio vídeo, relógio analógico discreto, atalhos Camera/Gallery, dock escuro com Telefone/Contatos/Gaveta/Mensagens/Navegador e botão de gaveta prateado. A gaveta usa abas APPS e WIDGETS, fundo preto e grade de quatro colunas. A aba WIDGETS lista os provedores de widgets instalados no aparelho; a hospedagem/arraste real de widgets para a área de trabalho ainda exige integração adicional com AppWidgetHost.


## Wallpaper do sistema, arrastar apps e botão da gaveta (neon)
- O papel de parede original do celular agora aparece na tela inicial (tema com `windowShowWallpaper`, sem imagem fixa). O arquivo `jellybean_wallpaper.png` não é mais usado.
- Na gaveta, segure um app e arraste: a gaveta fecha e o ícone pode ser solto numa grade 4 x 5 acima do dock. Se a célula estiver ocupada, vai para a mais próxima livre. Os ícones ficam salvos.
- Segure um ícone da tela inicial para mover; arraste até a faixa **Remover** no topo para apagar.
- Ao pressionar o botão da gaveta no dock, aparece a imagem neon azul (`ic_drawer_button_pressed.png`); ao soltar, volta ao botão prateado e a gaveta abre.

## Widgets do Android

Na gaveta, abra a aba **WIDGETS** e pressione e segure um widget para arrastá-lo até a tela inicial. Widgets que exigirem autorização serão encaminhados à tela de confirmação do Android. Os widgets adicionados são hospedados pelo AppWidgetHost do launcher e restaurados quando o launcher é aberto novamente.

## Animação da gaveta baseada no vídeo de referência
Ao tocar no botão central do dock, o painel preto da gaveta inteiro desliza de baixo para cima, ocupando a tela como no vídeo enviado. As abas APPS e WIDGETS e a grade de quatro colunas se movem juntas com o painel; os ícones não fazem uma animação individual em cascata. Ao voltar, a gaveta desliza para baixo e revela novamente o papel de parede da tela inicial. A troca entre APPS e WIDGETS mantém o painel aberto sem repetir a animação de entrada.
