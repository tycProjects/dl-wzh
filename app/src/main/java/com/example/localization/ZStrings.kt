package com.example.localization

import com.example.data.AppLanguage

object ZStrings {

    fun navMap(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Map"
        AppLanguage.FILIPINO -> "Mapa"
        AppLanguage.CEBUANO -> "Mapa"
        AppLanguage.INDONESIAN -> "Peta"
    }

    fun navSearch(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search & Saved"
        AppLanguage.FILIPINO -> "Hanapin & Naka-save"
        AppLanguage.CEBUANO -> "Pangita & Naka-save"
        AppLanguage.INDONESIAN -> "Cari & Tersimpan"
    }

    fun navSettings(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.FILIPINO -> "Mga Setting"
        AppLanguage.CEBUANO -> "Mga Setting"
        AppLanguage.INDONESIAN -> "Pengaturan"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search real places, cities, or streets..."
        AppLanguage.FILIPINO -> "Maghanap ng totoong lugar, lungsod, o kalye..."
        AppLanguage.CEBUANO -> "Pangitaa ang tinuod nga lugar, siyudad, o dalan..."
        AppLanguage.INDONESIAN -> "Cari tempat, kota, atau jalan nyata..."
    }

    fun myLocation(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "My Location"
        AppLanguage.FILIPINO -> "Aking Lokasyon"
        AppLanguage.CEBUANO -> "Akong Lokasyon"
        AppLanguage.INDONESIAN -> "Lokasi Saya"
    }

    fun locating(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Acquiring GPS fix..."
        AppLanguage.FILIPINO -> "Kinukuha ang signal ng GPS..."
        AppLanguage.CEBUANO -> "Gikuha ang signal sa GPS..."
        AppLanguage.INDONESIAN -> "Mendapatkan sinyal GPS..."
    }

    fun routePreview(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Route Preview"
        AppLanguage.FILIPINO -> "Silipin ang Ruta"
        AppLanguage.CEBUANO -> "Tan-awa ang Ruta"
        AppLanguage.INDONESIAN -> "Pratinjau Rute"
    }

    fun startNavigation(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Start Navigation"
        AppLanguage.FILIPINO -> "Simulan ang Navigasyon"
        AppLanguage.CEBUANO -> "Sugdi ang Nabigasyon"
        AppLanguage.INDONESIAN -> "Mulai Navigasi"
    }

    fun endNavigation(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "End Navigation"
        AppLanguage.FILIPINO -> "Tapusin ang Navigasyon"
        AppLanguage.CEBUANO -> "Undangi ang Nabigasyon"
        AppLanguage.INDONESIAN -> "Akhiri Navigasi"
    }

    fun driving(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Driving"
        AppLanguage.FILIPINO -> "Pagmamaneho"
        AppLanguage.CEBUANO -> "Pagmaneho"
        AppLanguage.INDONESIAN -> "Berkendara"
    }

    fun walking(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Walking"
        AppLanguage.FILIPINO -> "Paglalakad"
        AppLanguage.CEBUANO -> "Paglakaw"
        AppLanguage.INDONESIAN -> "Berjalan"
    }

    fun cycling(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Cycling"
        AppLanguage.FILIPINO -> "Pagbibisikleta"
        AppLanguage.CEBUANO -> "Pagbisikleta"
        AppLanguage.INDONESIAN -> "Bersepeda"
    }

    fun destination(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Destination"
        AppLanguage.FILIPINO -> "Destinasyon"
        AppLanguage.CEBUANO -> "Padulngan"
        AppLanguage.INDONESIAN -> "Tujuan"
    }

    fun recentSearches(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Recent Searches"
        AppLanguage.FILIPINO -> "Kamakailang Paghahanap"
        AppLanguage.CEBUANO -> "Bag-ong Gipangita"
        AppLanguage.INDONESIAN -> "Pencarian Terkini"
    }

    fun savedPlaces(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Saved Places"
        AppLanguage.FILIPINO -> "Mga Naka-save na Lugar"
        AppLanguage.CEBUANO -> "Naka-save nga mga Lugar"
        AppLanguage.INDONESIAN -> "Tempat Tersimpan"
    }

    fun clear(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.FILIPINO -> "Linisin"
        AppLanguage.CEBUANO -> "Hawani"
        AppLanguage.INDONESIAN -> "Hapus"
    }

    fun noSavedPlaces(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No saved places yet. Bookmark places for rapid navigation."
        AppLanguage.FILIPINO -> "Wala pang naka-save na lugar. Mag-bookmark para sa mabilis na ruta."
        AppLanguage.CEBUANO -> "Wala pay naka-save nga mga lugar. Pag-bookmark alang sa paspas nga ruta."
        AppLanguage.INDONESIAN -> "Belum ada tempat tersimpan. Tandai tempat untuk navigasi cepat."
    }

    fun categories(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Quick Categories"
        AppLanguage.FILIPINO -> "Mabilisang Kategorya"
        AppLanguage.CEBUANO -> "Dali nga Kategoriya"
        AppLanguage.INDONESIAN -> "Kategori Cepat"
    }

    fun accentColor(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Accent Glow Color"
        AppLanguage.FILIPINO -> "Kulay ng Accent Glow"
        AppLanguage.CEBUANO -> "Kolor sa Accent Glow"
        AppLanguage.INDONESIAN -> "Warna Aksen Cahaya"
    }

    fun language(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "App Language"
        AppLanguage.FILIPINO -> "Wika ng App"
        AppLanguage.CEBUANO -> "Pinulongan sa App"
        AppLanguage.INDONESIAN -> "Bahasa Aplikasi"
    }

    fun backToMap(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Return to Map"
        AppLanguage.FILIPINO -> "Bumalik sa Mapa"
        AppLanguage.CEBUANO -> "Balik sa Mapa"
        AppLanguage.INDONESIAN -> "Kembali ke Peta"
    }

    fun apiStatusTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "OpenStreetMap & OSRM Engine"
        AppLanguage.FILIPINO -> "OpenStreetMap & OSRM Engine"
        AppLanguage.CEBUANO -> "OpenStreetMap & OSRM Engine"
        AppLanguage.INDONESIAN -> "Mesin OpenStreetMap & OSRM"
    }

    fun apiConfigured(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "OpenStreetMap Live & Ready"
        AppLanguage.FILIPINO -> "Bukas at Handa ang OpenStreetMap"
        AppLanguage.CEBUANO -> "Aktibo ug Andam ang OpenStreetMap"
        AppLanguage.INDONESIAN -> "OpenStreetMap Aktif & Siap"
    }

    fun apiMissing(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "OpenStreetMap Active"
        AppLanguage.FILIPINO -> "Aktibo ang OpenStreetMap"
        AppLanguage.CEBUANO -> "Aktibo ang OpenStreetMap"
        AppLanguage.INDONESIAN -> "OpenStreetMap Aktif"
    }

    fun apiMissingGuide(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Operating on 100% free OpenStreetMap vector tiles, Nominatim geocoding, and OSRM real-time navigation."
        AppLanguage.FILIPINO -> "Gumagana sa 100% libreng OpenStreetMap tiles, Nominatim search, at OSRM real-time navigation nang walang API key."
        AppLanguage.CEBUANO -> "Naglihok sa 100% libreng OpenStreetMap tiles, Nominatim search, ug OSRM rota nga walay gikinahanglang API key."
        AppLanguage.INDONESIAN -> "Beroperasi pada 100% OpenStreetMap bebas biaya, pencarian Nominatim, dan navigasi OSRM tanpa perlu API key."
    }

    fun aboutTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "About ZMAPS"
        AppLanguage.FILIPINO -> "Tungkol sa ZMAPS"
        AppLanguage.CEBUANO -> "Mahitungod sa ZMAPS"
        AppLanguage.INDONESIAN -> "Tentang ZMAPS"
    }

    fun authorCredits(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "By JUSSAR GOLDEN"
        AppLanguage.FILIPINO -> "Gawa ni JUSSAR GOLDEN"
        AppLanguage.CEBUANO -> "Gibuhat ni JUSSAR GOLDEN"
        AppLanguage.INDONESIAN -> "Oleh JUSSAR GOLDEN"
    }

    fun appVersion(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Version 1.1.0 (OSM Edition)"
        AppLanguage.FILIPINO -> "Bersyon 1.1.0 (OSM Edisyon)"
        AppLanguage.CEBUANO -> "Bersyon 1.1.0 (OSM Edisyon)"
        AppLanguage.INDONESIAN -> "Versi 1.1.0 (Edisi OSM)"
    }

    fun permissionExplanation(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ZMAPS needs location access to pinpoint your position and compute real-time routes from where you are."
        AppLanguage.FILIPINO -> "Kailangan ng ZMAPS ang access sa lokasyon upang matukoy ang iyong posisyon at makalkula ang totoong ruta mula sa kinaroroonan mo."
        AppLanguage.CEBUANO -> "Gikinahanglan sa ZMAPS ang access sa lokasyon aron matino ang imong posisyon ug makalkula ang tinuod nga rota."
        AppLanguage.INDONESIAN -> "ZMAPS memerlukan izin lokasi untuk menentukan posisi Anda dan menghitung rute langsung dari lokasi Anda saat ini."
    }

    fun grantPermission(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Grant Location Access"
        AppLanguage.FILIPINO -> "Pahintulutan ang Lokasyon"
        AppLanguage.CEBUANO -> "Tugoti ang Lokasyon"
        AppLanguage.INDONESIAN -> "Izinkan Akses Lokasi"
    }

    fun useMapCenterStart(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Set Start at Map Center"
        AppLanguage.FILIPINO -> "Simula sa Gitna ng Mapa"
        AppLanguage.CEBUANO -> "Sugod sa Taliwala sa Mapa"
        AppLanguage.INDONESIAN -> "Mulai dari Tengah Peta"
    }

    fun manualStartNotice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Using manual start point. Tap GPS button anytime to switch to live GPS."
        AppLanguage.FILIPINO -> "Gamit ang manu-manong simula. Pindutin ang GPS button anumang oras."
        AppLanguage.CEBUANO -> "Gigamit ang mano-mano nga pagsugod. Pindota ang GPS bisan unsang orasa."
        AppLanguage.INDONESIAN -> "Menggunakan titik awal manual. Tekan tombol GPS untuk kembali ke GPS langsung."
    }

    fun retrySearch(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Retry Search"
        AppLanguage.FILIPINO -> "Subukan Muli"
        AppLanguage.CEBUANO -> "Sulayi Pag-usab"
        AppLanguage.INDONESIAN -> "Coba Lagi"
    }

    fun clearRoute(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear Route"
        AppLanguage.FILIPINO -> "Alisin ang Ruta"
        AppLanguage.CEBUANO -> "Hawani ang Rota"
        AppLanguage.INDONESIAN -> "Hapus Rute"
    }

    fun routeStepsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Route Step Overview"
        AppLanguage.FILIPINO -> "Pangkalahatang-ideya ng mga Hakbang"
        AppLanguage.CEBUANO -> "Kinatibuk-ang mga Lakang sa Rota"
        AppLanguage.INDONESIAN -> "Ringkasan Langkah Rute"
    }

    fun noRouteFound(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No navigable route found between these locations."
        AppLanguage.FILIPINO -> "Walang nakitang ruta sa pagitan ng mga lokasyong ito."
        AppLanguage.CEBUANO -> "Walay nakit-an nga rota tali sa kini nga mga lokasyon."
        AppLanguage.INDONESIAN -> "Tidak ada rute yang ditemukan antara lokasi ini."
    }

    fun close(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.FILIPINO -> "Isara"
        AppLanguage.CEBUANO -> "Sirad-i"
        AppLanguage.INDONESIAN -> "Tutup"
    }
}
