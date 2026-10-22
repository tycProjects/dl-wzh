/* المحتوى فارغ عمدًا.
أضف الأفلام والقنوات لاحقًا بهذا الشكل:

window.SPERSH_DATA.movies.push({
  id: "movie-1",
  title: "اسم الفيلم",
  year: "2026",
  poster: "https://example.com/poster.jpg",
  stream: "https://example.com/movie.m3u8",
  type: "movie",
  subtitles: [
    { lang: "ar", label: "العربية", src: "https://example.com/ar.vtt", default: true },
    { lang: "en", label: "English", src: "https://example.com/en.vtt" },
    { lang: "ko", label: "한국어", src: "https://example.com/ko.vtt" }
  ]
});

window.SPERSH_DATA.channels.push({
  id: "channel-1",
  title: "اسم القناة",
  stream: "https://example.com/live.m3u8",
  type: "channel"
});
*/
window.SPERSH_DATA = {
  movies: [],
  channels: []
};
