# Timer Plus (Expo) — como rodar no iPhone

## 1. Teste rápido (grátis, sem Mac e sem conta Apple)

1. No iPhone, instale o app **Expo Go** pela App Store.
2. No computador (Windows), dentro desta pasta:
   ```
   npm install
   npx expo start
   ```
3. Abra a câmera do iPhone e escaneie o QR code que aparece no terminal
   (iPhone e computador na **mesma rede Wi-Fi**). O app abre dentro do Expo Go.
   - Se a rede bloquear a conexão, use `npx expo start --tunnel`.

Limites no Expo Go: o som, a vibração e as notificações de troca de fase funcionam,
mas o iOS pausa o app quando a tela apaga. Para uso real no treino, mantenha o app
aberto (a tela fica ligada sozinha durante o timer) ou instale o build do passo 2.

## 2. Executável de verdade (.ipa) — precisa de conta Apple Developer (US$ 99/ano)

O build é feito na nuvem da Expo (EAS), então **não precisa de Mac**.

```
npm install -g eas-cli
eas login                      # conta gratuita em expo.dev
eas build:configure
eas build --platform ios --profile preview     # instalação direta em aparelhos cadastrados
# ou, para publicar:
eas build --platform ios --profile production
eas submit --platform ios      # envia para o TestFlight / App Store
```

Na primeira vez o EAS pede o login Apple e cadastra o certificado e o aparelho sozinho.
Para instalar o build `preview`, abra o link gerado pelo EAS no Safari do iPhone.

## 3. Estrutura

- `App.tsx` — navegação, splash, fontes e permissões
- `src/types.ts` — modelo do timer e geração das fases (preparação, trabalho, descanso, descanso do round)
- `src/useRunner.ts` — motor do timer (sons, vibração, notificações agendadas)
- `src/HomeScreen.tsx`, `EditScreen.tsx`, `RunScreen.tsx` — telas
