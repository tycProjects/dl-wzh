# Anjo ADB — como compilar e instalar

## 1. Instalar o Android Studio
Baixe em https://developer.android.com/studio (gratuito). Instale com as opções
padrão — ele já vem com o SDK do Android.

## 2. Abrir o projeto
Abra o Android Studio → **Open** → selecione a pasta `AnjoADB` (a pasta que
contém este README). Na primeira vez ele baixa o Gradle e as dependências
sozinho — pode levar uns minutos, é só esperar a barra de baixo terminar.

## 3. Gerar o APK
Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
Quando terminar, aparece um aviso "APK(s) generated successfully" com um
link **locate** — clique ali pra achar o arquivo `app-debug.apk`.

Ou, com o celular ligado no PC (depuração USB ativada), aperte o botão ▶️
verde no topo e ele já instala e abre direto no aparelho.

## 4. Instalar no celular
Transfira o `app-debug.apk` pro celular (cabo, WhatsApp Web, Google Drive —
o que for mais fácil) e toque nele pra instalar. Se o Android bloquear
"fontes desconhecidas", autorize nas configurações que aparecem na hora.

## 5. Ativar o Shizuku (OBRIGATÓRIO pra maioria das funções)
O Anjo ADB não é root — ele usa o **Shizuku** pra ter acesso de sistema.
1. Instale o app **Shizuku** (Play Store ou github.com/RikkaApps/Shizuku/releases)
2. Dentro do Shizuku, siga o método **"Depuração sem fio" (Android 11+)**:
   ative as Opções de desenvolvedor → Depuração sem fio → parear o código
   que o próprio Shizuku mostra na tela
3. Com o Shizuku rodando (ícone verde "ativo"), abra o Anjo ADB
4. Ligue qualquer função marcada — na primeira vez vai aparecer um pedido de
   permissão do Shizuku, aceite
5. Se tiver root, o Shizuku também pode rodar via root direto no próprio app
   dele, sem precisar de PC nenhum

Sem o Shizuku ativo e autorizado, todo card que precisa dele mostra
**"requer Shizuku ou ADB"** e o interruptor fica travado — isso é proposital,
não é bug.

## O que cada função realmente faz por trás
- **Limpar Cache Geral** → `pm trim-caches` (comando real de sistema, limpa cache de todos os apps)
- **Espremer RAM** → lê `dumpsys meminfo`, acha os apps que mais consomem e roda `am force-stop` neles (menos os que você marcar como protegidos)
- **Escala de Renderização / Texturas** → `wm size`, reduz a resolução renderizada de verdade (leve -10%, média -20%, alta -30%)
- **Alvo de FPS** → `settings put system peak_refresh_rate` / `min_refresh_rate` (só funciona se a tela do aparelho suportar aquela taxa)
- **Congelar apps / Lista de Congelamento** → `pm disable-user` / `pm enable`
- **Bluetooth Off / Wi-Fi / GPS no jogo** → `svc bluetooth`, `svc wifi`, `cmd location`
- **Sistema Mais Leve** → reduz escala de animação do sistema (`window_animation_scale` etc.)
- **Foco no Jogo** → usa a permissão de Não Perturbe do Android (pede acesso na hora que você liga)
- **Bloqueio de chamada no Foco** → pede pra virar o app de triagem de chamadas (RoleManager) — sem isso o Android não deixa nenhum app rejeitar ligação sozinho, nem com Shizuku
- **Bloqueio de Alarmes** → silencia o volume de alarme enquanto ativo (não existe API pública sem root pra cancelar alarme agendado de outro app — isso é uma limitação do Android, não só deste app)
- **Volume Automático** → API normal do Android, não precisa de Shizuku
- Os demais cards do Motor Inteligente / GPU / Visual funcionam como
  sinalizadores que o motor de detecção de jogo usa internamente — a base
  de ação real (cache, RAM, resolução, FPS, rede, congelamento) já está
  toda plugada.

## Permissões que o app pede na hora de usar
- Sobreposição de tela (bolinha flutuante) — Configurações abre sozinha
- Acesso ao Não Perturbe (Foco no Jogo)
- App de triagem de chamadas (bloqueio de ligação no Foco)
- Shizuku (todas as funções de sistema)

## Aviso que o app mostra
Antes da primeira aplicação de qualquer função de sistema, aparece um alerta
avisando que o app não se responsabiliza por travamento, perda de dado ou
comportamento inesperado causado pelas opções ativadas — like pedido.
