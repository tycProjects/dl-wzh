# Share links: page on Google (Firebase Hosting), APKs on GitHub Releases

APK files: GitHub. Download page: Firebase Hosting. The page is ~6 KB and the APK download itself
comes from GitHub, so it uses almost none of Firebase's free transfer.

1. APK repo: new PUBLIC GitHub repo, e.g. `apk-files`, with a README (must not be empty).
2. Token: GitHub > Settings > Developer settings > Fine-grained tokens. Only select repositories >
   apk-files, permission Contents = Read and write.
3. Edit `REPO` at the bottom of `site/index.html` to `yourname/apk-files`.
4. Firebase: console.firebase.google.com > Add project (stay on the free Spark plan, no card).
   Then in the `site` folder:
       npm i -g firebase-tools
       firebase login
       firebase deploy --only hosting --project YOUR_PROJECT_ID
   Your page is at https://YOUR_PROJECT_ID.web.app
5. `lib/config.dart`: kShareGithubRepo = 'yourname/apk-files', kShareGithubToken = <token>,
   kShareSiteBase = 'https://YOUR_PROJECT_ID.web.app'. Rebuild the app.


## Hide your GitHub account from the download page (share Worker)

By default the page used to send visitors to a github.com/<you>/<repo> address. Do this once so they only see your own:

1. Cloudflare dashboard > Workers & Pages > Create > Worker. Name it e.g. `wzh-share`, Deploy, Edit code,
   paste `worker/share-worker.js`, Deploy.
2. Worker > Settings > Variables and Secrets:
   - `SHARE_REPO`  (text)    `yourname/apk-files`
   - `SHARE_TOKEN` (secret)  recommended: fine-grained GitHub token, only the APK repo, Contents = Read-only.
     Without it GitHub only allows about 60 lookups an hour for all visitors together.
3. In `site/index.html` set `SHARE_API` to your Worker address (`https://wzh-share.<you>.workers.dev`).
4. `firebase deploy --only hosting --project YOUR_PROJECT_ID`. Old share links keep working.
5. Optional, best: with `SHARE_TOKEN` set, make the APK repo PRIVATE. Then github.com/<you>/<repo> shows nothing to anyone.

What this does NOT hide: the repo name is still inside the app itself (`kShareGithubRepo`), so someone who
takes the APK apart could find it. Using a neutral repo/account name, or a private repo, covers that.
