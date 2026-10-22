/* HTML5 <-> Android Media3 bridge.
   On Android, window.AndroidMedia3 is injected by MainActivity.
   In a normal browser, the original HTML5/HLS/DASH player remains active. */
(() => {
  const native = () => !!window.AndroidMedia3;
  window.SPERSH_MEDIA3 = {
    available: native,
    play(item) {
      if (!native() || !item?.stream) return false;
      AndroidMedia3.play(JSON.stringify({
        url:item.stream,
        title:item.title||"",
        type:item.type||"movie",
        subtitles:item.subtitles||[],
        headers:item.headers||{}
      }));
      return true;
    },
    pause(){ if(native()) AndroidMedia3.pause(); },
    resume(){ if(native()) AndroidMedia3.resume(); },
    seekTo(ms){ if(native()) AndroidMedia3.seekTo(Math.max(0,Math.floor(ms))); },
    seekBy(ms){ if(native()) AndroidMedia3.seekBy(Math.floor(ms)); },
    setVolume(v){ if(native()) AndroidMedia3.setVolume(Math.max(0,Math.min(1,Number(v)))); },
    setSpeed(v){ if(native()) AndroidMedia3.setSpeed(Number(v)||1); },
    stop(){ if(native()) AndroidMedia3.stop(); }
  };
})();
