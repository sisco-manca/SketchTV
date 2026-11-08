'use strict';
const $=id=>document.getElementById(id);
let project, links=[], selected=0, valid=false, revision=0, sketchURL=null;
const providerNames={weather:'Open-Meteo · CC BY 4.0',f1:'Jolpica F1',football:'ESPN'};
const icons={settings:'<path d="m22 3 3 1 2-2 4 4-2 2 1 3 3 1v6l-3 1-1 3 2 2-4 4-2-2-3 1-1 3h-6l-1-3-3-1-2 2-4-4 2-2-1-3-3-1v-6l3-1 1-3-2-2 4-4 2 2 3-1 1-3h6z"/><circle cx="19" cy="19" r="6" fill="var(--card)"/>',network:'<g fill="none" stroke="currentColor" stroke-width="4"><path d="M3 13q16-17 32 0M8 20q11-12 22 0M14 27q5-6 10 0"/></g><circle cx="19" cy="33" r="2"/>',tv:'<g fill="none" stroke="currentColor" stroke-width="2.5"><rect x="5" y="10" width="28" height="23" rx="3"/><path d="m12 3 7 7 7-7"/></g>',av:'<g fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="8" cy="13" r="5"/><circle cx="19" cy="13" r="5"/><circle cx="30" cy="13" r="5"/><path d="M8 18v16M19 18v16M30 18v16"/></g>',hdmi1:'<path d="M3 10h32v11l-5 7H8l-5-7zm5 5v7h22v-7z"/>',sources:'<g fill="none" stroke="currentColor" stroke-width="3"><path d="M23 7h10v25H23M3 19h22m-6-7 7 7-7 7"/></g>'}; icons.hdmi2=icons.hdmi1;
function node(tag,cls,text){const n=document.createElement(tag);if(cls)n.className=cls;if(text!==undefined)n.textContent=text;return n;}
function svg(key){const s=document.createElementNS('http://www.w3.org/2000/svg','svg');s.setAttribute('viewBox','0 0 38 38');s.setAttribute('aria-hidden','true');s.style.fill='currentColor';s.innerHTML=icons[key];return s;}
// Keep the artboard in design pixels; only the outer viewport scales.
function resize(){ $('screen').style.transform=`scale(${$('viewport').clientWidth/1920})`; }
function sourceFor(t){return t.action.startsWith('widget:')?providerNames[t.action.slice(7)]||'Provider personalizzato':'';}
function render(){
  const screen=$('screen'), theme=project.theme;
  for(const [key,value] of Object.entries(theme))screen.style.setProperty('--'+key,typeof value==='number'?(key==='radius'?value+'px':value):value);
  screen.style.background=`linear-gradient(90deg,${theme.background},${theme.backgroundEnd})`;
  $('tiles').replaceChildren();
  project.tiles.forEach((t,i)=>{
    const [x,y,w,h]=t.rect, button=node('button','tile '+(t.kind==='source'?'source-tile':t.kind));
    Object.assign(button.style,{left:x+'px',top:y+'px',width:w+'px',height:h+'px'});
    button.dataset.index=i;button.setAttribute('aria-label',t.label);if(t.color)button.style.background=t.color;
    if(t.kind==='clock'){
      button.append(node('span','time',''),node('span','date',''),node('span','weather',''));
      button.querySelector('.time').style.fontSize=Math.min(w*.34,h*.55)*theme.textScale+'px';button.querySelector('.date').style.fontSize=Math.min(40,h*.1)*theme.textScale+'px';button.querySelector('.weather').style.fontSize=Math.min(36,h*.09)*theme.textScale+'px';button.append(node('span','source','Fonte: '+sourceFor(t)));
    }else if(t.kind==='widget'){
      const key=t.action.slice(7), cfg=project.widgets[key];
      button.append(node('h3','',cfg.enabled?cfg.name||cfg.city:t.label));
      const lines=!cfg.enabled?['Da configurare','Scegli fonte e contenuti']:key==='f1'?['GP dimostrativo','Pilota A · P2','Pilota B · P5','Costruttori · P3']:key==='football'?['Squadra A · 2','Squadra B · 1','Ultima partita · esempio','Classifica · 8ª']:['18 °C · esempio','Rilevazione dimostrativa'];
      lines.forEach(s=>button.append(node('span','demo',s)));
      button.append(node('span','source','Fonte: '+sourceFor(t)),node('span','state',cfg.enabled?'Dati dimostrativi · non reali':'Da configurare'));
    }else if(t.asset){
      const img=node('img');img.alt=t.label;img.src='/assets/'+t.asset;img.onerror=()=>img.replaceWith(node('span','tile-label',t.label));button.append(img);
    }else if(icons[t.action]){
      button.append(svg(t.action));if(t.kind!=='control')button.append(node('span','tile-label',t.label));
    }else button.append(node('span','tile-label',t.label));
    if(t.kind==='widget'){
      const pad=Math.min(32,w*.08),fs=Math.min(36,h*.105),step=(h-pad-fs-78)/5;
      button.style.padding=pad+'px';button.querySelector('h3').style.fontSize=fs*theme.textScale+'px';
      button.querySelectorAll('.demo').forEach(el=>el.style.fontSize=Math.min(29,step*.8)*theme.textScale+'px');
    }
    button.querySelectorAll('.source,.state').forEach(el=>el.style.fontSize=16*theme.textScale+'px');
    button.querySelectorAll('.tile-label').forEach(el=>el.style.fontSize=Math.min(36,h*.25)*theme.textScale+'px');
    button.addEventListener('focus',()=>select(i,false));button.addEventListener('click',()=>{select(i,false);activate();});
    button.addEventListener('keydown',event=>{const d={ArrowLeft:0,ArrowUp:1,ArrowRight:2,ArrowDown:3}[event.key];if(d!==undefined){event.preventDefault();navigate(d);}});
    $('tiles').append(button);
  });
  $('count').textContent=project.tiles.length+' riquadri · focus esplicito';selected=Math.min(selected,project.tiles.length-1);select(selected,false);updateTime();resize();
}
function updateTime(){if(!project)return;const now=new Date();for(const el of document.querySelectorAll('.tile.clock')){el.querySelector('.time').textContent=new Intl.DateTimeFormat(project.clock.locale,{hour:'2-digit',minute:'2-digit',hourCycle:'h23',timeZone:project.clock.timeZone}).format(now);el.querySelector('.date').textContent=new Intl.DateTimeFormat(project.clock.locale,{weekday:'long',day:'numeric',month:'long',year:'numeric',timeZone:project.clock.timeZone}).format(now);const key=project.tiles[Number(el.dataset.index)].action.slice(7), cfg=project.widgets[key];el.querySelector('.weather').textContent=cfg.city?cfg.city+' · '+(cfg.enabled?'18 °C · esempio':'—'):'Widget · esempio';}}
function select(index,focus=true){selected=index;document.querySelectorAll('.tile').forEach((e,i)=>e.classList.toggle('selected',i===index));const t=project.tiles[index];$('label').value=t.label;['x','y','w','h'].forEach((id,i)=>$(id).value=t.rect[i]);if(focus)document.querySelectorAll('.tile')[index].focus({preventScroll:true});}
function navigate(direction){if(valid&&links[selected])select(links[selected][direction]);}
function activate(){if(!valid)return;const t=project.tiles[selected];$('detailTitle').textContent=t.label;let text='Simulazione locale. Azione Android: '+t.action+'\n\nNessuna applicazione o sorgente viene aperta su un televisore.';if(t.action.startsWith('widget:'))text+='\n\nFonte: '+sourceFor(t)+'\nNella TV i dettagli mostrano anche endpoint, licenza, data di download e stato dei dati. Questa anteprima non scarica dati dai provider.';$('detailBody').textContent=text;$('detail').showModal();}
async function validateText(){
  const current=++revision;valid=false;$('export').disabled=true;
  try{
    const candidate=JSON.parse($('config').value),response=await fetch('/validate',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(candidate)});
    if(!response.ok)throw Error('Validatore locale non disponibile');const result=await response.json();if(current!==revision)return;if(!result.valid)throw Error(result.error);
    if(!project){const firstApp=candidate.tiles.findIndex(t=>t.kind==='app');selected=firstApp<0?0:firstApp;}project=candidate;links=result.neighbors;valid=true;$('export').disabled=false;$('status').textContent='✓ Configurazione valida · pronta per la build';$('status').className='';render();
  }catch(error){if(current!==revision)return;$('status').textContent='Correggi il progetto: '+error.message;$('status').className='invalid';}
}
let debounce;$('config').addEventListener('input',()=>{revision++;valid=false;$('export').disabled=true;clearTimeout(debounce);debounce=setTimeout(validateText,250);});
$('properties').addEventListener('submit',event=>{event.preventDefault();if(!project)return;const next=structuredClone(project);next.tiles[selected].label=$('label').value;next.tiles[selected].rect=['x','y','w','h'].map(id=>Number($(id).value));$('config').value=JSON.stringify(next,null,2);validateText();});
$('import').addEventListener('change',async event=>{const file=event.target.files[0];if(!file)return;if(file.size>131072){$('status').textContent='File troppo grande: massimo 128 KiB';$('status').className='invalid';return;}$('config').value=await file.text();validateText();});
$('export').addEventListener('click',()=>{if(!valid)return;const url=URL.createObjectURL(new Blob([JSON.stringify(project,null,2)+'\n'],{type:'application/json'})),a=node('a');a.href=url;a.download='project.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);});
$('sketch').addEventListener('change',event=>{const file=event.target.files[0];if(!file)return;if(sketchURL)URL.revokeObjectURL(sketchURL);sketchURL=URL.createObjectURL(file);$('overlay').src=sketchURL;$('overlay').hidden=false;});
$('opacity').addEventListener('input',()=>$('overlay').style.opacity=$('opacity').value/100);
$('clearSketch').addEventListener('click',()=>{$('overlay').hidden=true;$('overlay').removeAttribute('src');if(sketchURL)URL.revokeObjectURL(sketchURL);sketchURL=null;$('sketch').value='';});
$('activate').addEventListener('click',activate);document.querySelectorAll('[data-direction]').forEach(b=>b.addEventListener('click',()=>navigate(Number(b.dataset.direction))));$('closeDetail').addEventListener('click',()=>$('detail').close());
new ResizeObserver(resize).observe($('viewport'));setInterval(updateTime,60000);
fetch('/project.json').then(r=>{if(!r.ok)throw Error('project.json non disponibile');return r.json();}).then(c=>{$('config').value=JSON.stringify(c,null,2);return validateText();}).catch(e=>{$('status').textContent=e.message;$('status').className='invalid';});
