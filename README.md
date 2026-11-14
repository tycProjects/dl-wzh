# Box 2.9.2
Correção de bug: o botão ATIVAR, quando desabilitado (antes de permitir a sobreposição), usava a mesma cor cinza do fundo da tela e ficava praticamente invisível, embora continuasse ali e funcionando normalmente assim que a permissão fosse concedida. Agora ele usa um cinza bem mais claro, sempre visível em qualquer estado.

# Box 2.9.1
Correção: os efeitos MEMZ Matrix e Tubos 3D tinham ficado de fora do reforço de cor da 2.9 (cores meio pastel). Agora o Matrix usa um verde mais puro e brilhante, e os Tubos 3D usam cores primárias saturadas em vez de tons claros. Com isso, todos os efeitos têm o mesmo reforço de cor.

# Box 2.9

## Cores mais fortes (2.9)
Todas as cores dos efeitos ficaram mais vívidas e com mais opacidade (Spiral, Glitch, Wave, Chaos, Tunnel, Melt, Plasma, Moiré, Blocos, Hipnose, Olhos). Nos efeitos que usam a tela capturada (Melt, Wave, Tunnel, Glitch, Tiles, Caleidoscópio, Multiplicando, Girando), apliquei um filtro de saturação fixo (sempre o mesmo, em todo quadro) para deixar as cores reais da tela mais intensas.
Continua sem nenhuma cor alternando rápido: o filtro é estático e as transições de cor seguem suaves, como em todas as versões anteriores — só ficou mais forte visualmente, não mais perigoso.

# Box 2.8

## Adaptação a diferentes telas (2.8)
- Interface agora centraliza o conteúdo com uma largura máxima em telas largas (tablet/TV) e rola em telas pequenas ou baixas, para nada ficar cortado.
- Novo fator de escala automático conforme o tipo de tela: celulares pequenos ficam um pouco menores, tablets maiores, e TVs (vistas de longe) ganham textos e botões bem maiores. Esse mesmo fator também aumenta o tamanho dos elementos dos efeitos (texto do Matrix, ícones do rastro, espessura dos tubos 3D etc.) para ficarem proporcionais em qualquer tamanho de tela.
- `AndroidManifest.xml` agora declara suporte explícito a telas pequenas, normais, grandes e extra grandes, e toque como recurso opcional (`android.hardware.touchscreen required=false`), permitindo instalar em Android TV.
- Adicionado um segundo filtro de intenção com `LEANBACK_LAUNCHER`, para o app aparecer no launcher de TVs, e um banner (`tv_banner.png`) para essa lista.
- Em TV, o primeiro botão recebe foco automaticamente para navegação por controle remoto/D-pad (os `Button` padrão do Android já suportam isso nativamente).

# Box 2.7

## Mudanças da 2.7
- Fundo do app e botões agora em cinza.
- Aviso de abertura ampliado para citar epilepsia fotossensível explicitamente, além da sensibilidade a luzes/movimento.
- Uma imagem (sorteada entre as 5 enviadas) aparece fixa na tela a cada 1 segundo e não desaparece até o usuário parar o efeito; esse "rastro" agora roda por cima de qualquer efeito escolhido, não só dos efeitos de ícones.
- Dois efeitos novos: **Tela se multiplicando** (a tela vira uma grade de cópias dela mesma, crescendo a cada ~1 s, até 6x6) e **Tela girando** (rotação contínua e suave da tela, sem nenhuma cor piscando). Ambos entraram também no ciclo do MEMZ Total.
- **Não implementei efeitos "epilépticos"**: esse termo descreve justamente padrões de luz na faixa de frequência que provoca crises em quem tem fotossensibilidade. Aviso nenhum torna esse risco aceitável, então os novos efeitos usam só movimento e giro, sem flash de cor.

# Box 2.6

## Efeitos MEMZ (2.6)
5 efeitos novos inspirados no vírus MEMZ, todos dentro da sobreposição (sem mexer em nenhuma configuração do sistema, como a inversão de cores do Android — isso eu não faço; app nenhum deve alternar sozinho ajustes de acessibilidade do sistema):
- **MEMZ: Matrix** — chuva de caracteres verdes.
- **MEMZ: Tubos 3D** — cobrinhas coloridas, como o protetor de tela clássico do Windows.
- **MEMZ: Olhos** — olhos espalhados pela tela que seguem um ponto e piscam devagar (sem flash, sem inverter cor).
- **MEMZ: Tiles** e **MEMZ: Caleidoscópio** — recortam e embaralham a própria tela capturada.
- **MEMZ Total** — ciclo por todos os efeitos acima (e os GDI antigos), trocando a cada 7s. É o que o botão ATIVAR já usa.

# Box 2.5

## Mudanças da 2.5
- Removida a legenda explicativa da tela inicial.
- Duração agora é infinita por padrão: os efeitos ficam ativos até o botão PARAR, a ação PARAR da notificação, a tela apagar ou o aparelho reiniciar (não há mais limite automático de 30s).
- Os ícones do rastro (efeito "Rastro de erros" e no final do ciclo do GDI Total) agora usam as imagens fornecidas (robôs Android, X vermelho, alerta amarelo) em vez dos ícones padrão do Android.
- Esse rastro agora deixa marcas permanentes por onde passa, em vez de sumir: cada posição vira uma "cópia parada" que continua na tela. Para não pesar o aparelho numa sessão longa, o número de marcas tem um teto (500); ao passar disso, a marca mais antiga é trocada pela mais nova — a trilha recente sempre continua visível.

## Efeitos reais na tela (2.3)
O botão ATIVAR agora pede a permissão de gravação de tela do Android (escolha "Tela inteira") e distorce a sua tela em tempo real: derretendo, ondulando, em túnel e com glitch RGB, em sequência crescente (GDI Total).
- Nada é salvo em arquivo nem enviado: cada quadro é só redesenhado e descartado. O app não tem permissão de internet.
- Não há inversão de cor: como a sobreposição aparece na própria captura, inverter faria a tela piscar rápido (risco de fotossensibilidade). Só há deformações e rastros suaves.
- Para na hora: botão PARAR, notificação, 30 s, tela apagada ou ao encerrar a gravação pelo sistema.
 — projeto Android

Brincadeira visual: efeitos estilo "GDI" por cima da tela, de forma segura e reversível.

## Interface (2.2)
Tela mínima: um botão para permitir a sobreposição e um único botão ATIVAR (vira PARAR enquanto os efeitos rodam). Ativa o efeito "GDI Total" em intensidade Forte por 30 s.

## Novidades da 2.1
- 6 efeitos novos e mais complexos: Plasma, Moiré, Blocos corrompidos (com separação RGB), Rastro de erros (ícones com rastro), Hipnose e **GDI Total** (mistura plasma + moiré + blocos + glitch + ícones + linhas de varredura).
- Nova intensidade **Extremo**.
- Continua sem flash brusco: tudo muda de forma suave, bem abaixo da faixa de risco de fotossensibilidade.

## O que mudou na 2.0
- **Corrigido:** pastas de ícone com nomes inválidos (`mipmap-144` etc.) quebravam o build; agora são `mipmap-mdpi … xxxhdpi`.
- **Corrigido:** ícone adaptativo reposicionado dentro da zona segura (não é mais cortado).
- **Segurança visual:** nenhuma cor pisca; as transições são suaves (≤ ~2 Hz). Aviso de fotossensibilidade antes do primeiro uso. Intensidade Suave/Normal/Forte.
- **Parada garantida:** botão PARAR, ação PARAR na notificação, tempo limite (15s/30s/60s, padrão 30s), e encerra ao apagar a tela. A sobreposição nunca bloqueia toques.
- **Notificações:** o app agora pede a permissão de notificação (Android 13+). Se for negada, a duração "sem limite" vira 60s.
- **Efeitos:** 7 (Túnel e Derretimento); animações independentes da taxa de quadros; spiral com 3 braços.
- **Interface:** rolável, efeito selecionado destacado, status em tempo real.

## Privacidade
Sem internet, sem acesso a arquivos, sem gravação de áudio. A captura de tela só alimenta o efeito em tempo real e nunca é armazenada.

## Gerar APK
Abra a pasta no Android Studio, sincronize o Gradle e use Build > Build APK(s).
Saída: `app/build/outputs/apk/debug/app-debug.apk`.

Este ZIP é o projeto-fonte. Não foi compilado nem testado em aparelho real.
