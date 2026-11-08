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
