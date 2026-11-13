const CACHE_NAME="canberra-explorer-v27";
const ASSETS=["./","./index.html","./manifest.json","./icon.svg","./icon-192.png","./icon-512.png","./places.json"];
self.addEventListener("install",event=>{event.waitUntil(caches.open(CACHE_NAME).then(cache=>cache.addAll(ASSETS)));});
self.addEventListener("activate",event=>{event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key!==CACHE_NAME).map(key=>caches.delete(key)))));self.clients.claim();});
self.addEventListener("fetch",event=>{
  if(event.request.method!=="GET")return;
  const url=new URL(event.request.url);
  const refreshable=event.request.mode==="navigate"||url.pathname.endsWith("/index.html")||url.pathname.endsWith("/places.json");
  if(refreshable){
    event.respondWith(fetch(event.request,{cache:"no-store"}).then(response=>{
      if(response&&response.ok){const copy=response.clone();caches.open(CACHE_NAME).then(cache=>cache.put(event.request,copy));}
      return response;
    }).catch(()=>caches.match(event.request).then(cached=>cached||caches.match("./index.html"))));
    return;
  }
  event.respondWith(caches.match(event.request).then(cached=>cached||fetch(event.request).then(response=>{
    const copy=response.clone();caches.open(CACHE_NAME).then(cache=>cache.put(event.request,copy));return response;
  })));
});

self.addEventListener("message",event=>{
  if(event.data&&event.data.type==="SKIP_WAITING")self.skipWaiting();
});
