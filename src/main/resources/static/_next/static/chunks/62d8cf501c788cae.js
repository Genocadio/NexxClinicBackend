(globalThis.TURBOPACK||(globalThis.TURBOPACK=[])).push(["object"==typeof document?document.currentScript:void 0,79873,(e,t,r)=>{"use strict";t.exports.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED=void 0,t.exports.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE=void 0,t.exports.__SERVER_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE=void 0,Object.assign(t.exports,e.r(71645))},71258,90571,82587,30207,75739,90690,87081,75625,18247,36433,6195,69981,52812,34049,72243,20871,40059,12337,2693,95364,630,19983,21110,7799,95077,36535,41888,20064,85809,98943,61518,49987,53949,58350,7656,92360,66852,86713,40096,79506,45938,76846,54335,99074,e=>{"use strict";let t;var r,n,i,a,s,o,u,d,l,c=function(e,t){return(c=Object.setPrototypeOf||({__proto__:[]})instanceof Array&&function(e,t){e.__proto__=t}||function(e,t){for(var r in t)Object.prototype.hasOwnProperty.call(t,r)&&(e[r]=t[r])})(e,t)};function p(e,t){if("function"!=typeof t&&null!==t)throw TypeError("Class extends value "+String(t)+" is not a constructor or null");function r(){this.constructor=e}c(e,t),e.prototype=null===t?Object.create(t):(r.prototype=t.prototype,new r)}var m=function(){return(m=Object.assign||function(e){for(var t,r=1,n=arguments.length;r<n;r++)for(var i in t=arguments[r])Object.prototype.hasOwnProperty.call(t,i)&&(e[i]=t[i]);return e}).apply(this,arguments)};function h(e,t){var r={};for(var n in e)Object.prototype.hasOwnProperty.call(e,n)&&0>t.indexOf(n)&&(r[n]=e[n]);if(null!=e&&"function"==typeof Object.getOwnPropertySymbols)for(var i=0,n=Object.getOwnPropertySymbols(e);i<n.length;i++)0>t.indexOf(n[i])&&Object.prototype.propertyIsEnumerable.call(e,n[i])&&(r[n[i]]=e[n[i]]);return r}function f(e,t,r,n){return new(r||(r=Promise))(function(i,a){function s(e){try{u(n.next(e))}catch(e){a(e)}}function o(e){try{u(n.throw(e))}catch(e){a(e)}}function u(e){var t;e.done?i(e.value):((t=e.value)instanceof r?t:new r(function(e){e(t)})).then(s,o)}u((n=n.apply(e,t||[])).next())})}function v(e,t){var r,n,i,a={label:0,sent:function(){if(1&i[0])throw i[1];return i[1]},trys:[],ops:[]},s=Object.create(("function"==typeof Iterator?Iterator:Object).prototype);return s.next=o(0),s.throw=o(1),s.return=o(2),"function"==typeof Symbol&&(s[Symbol.iterator]=function(){return this}),s;function o(o){return function(u){var d=[o,u];if(r)throw TypeError("Generator is already executing.");for(;s&&(s=0,d[0]&&(a=0)),a;)try{if(r=1,n&&(i=2&d[0]?n.return:d[0]?n.throw||((i=n.return)&&i.call(n),0):n.next)&&!(i=i.call(n,d[1])).done)return i;switch(n=0,i&&(d=[2&d[0],i.value]),d[0]){case 0:case 1:i=d;break;case 4:return a.label++,{value:d[1],done:!1};case 5:a.label++,n=d[1],d=[0];continue;case 7:d=a.ops.pop(),a.trys.pop();continue;default:if(!(i=(i=a.trys).length>0&&i[i.length-1])&&(6===d[0]||2===d[0])){a=0;continue}if(3===d[0]&&(!i||d[1]>i[0]&&d[1]<i[3])){a.label=d[1];break}if(6===d[0]&&a.label<i[1]){a.label=i[1],i=d;break}if(i&&a.label<i[2]){a.label=i[2],a.ops.push(d);break}i[2]&&a.ops.pop(),a.trys.pop();continue}d=t.call(e,a)}catch(e){d=[6,e],n=0}finally{r=i=0}if(5&d[0])throw d[1];return{value:d[0]?d[1]:void 0,done:!0}}}}function y(e,t,r){if(r||2==arguments.length)for(var n,i=0,a=t.length;i<a;i++)!n&&i in t||(n||(n=Array.prototype.slice.call(t,0,i)),n[i]=t[i]);return e.concat(n||Array.prototype.slice.call(t))}"function"==typeof SuppressedError&&SuppressedError,e.s(["__assign",()=>m,"__awaiter",()=>f,"__extends",()=>p,"__generator",()=>v,"__rest",()=>h,"__spreadArray",()=>y],90571);var g="Invariant Violation",b=Object.setPrototypeOf,I=void 0===b?function(e,t){return e.__proto__=t,e}:b,A=function(e){function t(r){void 0===r&&(r=g);var n=e.call(this,"number"==typeof r?g+": "+r+" (see https://github.com/apollographql/invariant-packages)":r)||this;return n.framesToPop=1,n.name=g,I(n,t.prototype),n}return p(t,e),t}(Error);function N(e,t){if(!e)throw new A(t)}var E=["debug","log","warn","error","silent"],T=E.indexOf("log");function P(e){return function(){if(E.indexOf(e)>=T)return(console[e]||console.log).apply(console,arguments)}}(r=N||(N={})).debug=P("debug"),r.log=P("log"),r.warn=P("warn"),r.error=P("error");var D="3.14.1";function _(e){try{return e()}catch(e){}}e.s(["version",()=>D],82587),e.s(["maybe",()=>_],30207);let O=_(function(){return globalThis})||_(function(){return window})||_(function(){return self})||_(function(){return e.g})||_(function(){return _.constructor("return this")()});var C=new Map;function R(e){var t=C.get(e)||1;return C.set(e,t+1),"".concat(e,":").concat(t,":").concat(Math.random().toString(36).slice(2))}function S(e,t){void 0===t&&(t=0);var r=R("stringifyForDisplay");return JSON.stringify(e,function(e,t){return void 0===t?r:t},t).split(JSON.stringify(r)).join("<undefined>")}function w(e){return function(t){for(var r=[],n=1;n<arguments.length;n++)r[n-1]=arguments[n];if("number"==typeof t){var i=t;(t=$(i))||(t=x(i,r),r=[])}e.apply(void 0,[t].concat(r))}}e.s(["makeUniqueId",()=>R],75739),e.s(["stringifyForDisplay",()=>S],90690);var k=Object.assign(function(e,t){for(var r=[],n=2;n<arguments.length;n++)r[n-2]=arguments[n];e||N(e,$(t,r)||x(t,r))},{debug:w(N.debug),log:w(N.log),warn:w(N.warn),error:w(N.error)});function L(e){for(var t=[],r=1;r<arguments.length;r++)t[r-1]=arguments[r];return new A($(e,t)||x(e,t))}var F=Symbol.for("ApolloErrorMessageHandler_"+D);function M(e){if("string"==typeof e)return e;try{return S(e,2).slice(0,1e3)}catch(e){return"<non-serializable>"}}function $(e,t){if(void 0===t&&(t=[]),e)return O[F]&&O[F](e,t.map(M))}function x(e,t){if(void 0===t&&(t=[]),e)return"An error occurred! For more details, see the full error text at https://go.apollo.dev/c/err#".concat(encodeURIComponent(JSON.stringify({version:D,message:e,args:t.map(M)})))}e.s(["invariant",()=>k,"newInvariantError",()=>L],87081),e.s([],71258);var V=e.i(79873),U="ReactNative"==_(function(){return navigator.product}),q="function"==typeof WeakMap&&!(U&&!e.g.HermesInternal),B="function"==typeof WeakSet,j="function"==typeof Symbol&&"function"==typeof Symbol.for,Q=j&&Symbol.asyncIterator,z="function"==typeof _(function(){return window.document.createElement}),G=_(function(){return navigator.userAgent.indexOf("jsdom")>=0})||!1,K=(z||U)&&!G;e.s(["canUseAsyncIteratorSymbol",()=>Q,"canUseDOM",()=>z,"canUseLayoutEffect",()=>K,"canUseSymbol",()=>j,"canUseWeakMap",()=>q,"canUseWeakSet",()=>B],75625);var H=j?Symbol.for("__APOLLO_CONTEXT__"):"__APOLLO_CONTEXT__";function Y(){k("createContext"in V,69);var e=V.createContext[H];return e||(Object.defineProperty(V.createContext,H,{value:e=V.createContext({}),enumerable:!1,writable:!1,configurable:!0}),e.displayName="ApolloContext"),e}function X(){for(var e=[],t=0;t<arguments.length;t++)e[t]=arguments[t];var r=Object.create(null);return e.forEach(function(e){e&&Object.keys(e).forEach(function(t){var n=e[t];void 0!==n&&(r[t]=n)})}),r}function W(e,t){return X(e,t,t.variables&&{variables:X(m(m({},e&&e.variables),t.variables))})}e.s(["getApolloContext",()=>Y],18247),e.s(["compact",()=>X],36433),e.s(["mergeOptions",()=>W],6195);let{toString:J,hasOwnProperty:Z}=Object.prototype,ee=Function.prototype.toString,et=new Map;function er(e,t){try{return function e(t,r){if(t===r)return!0;let n=J.call(t);if(n!==J.call(r))return!1;switch(n){case"[object Array]":if(t.length!==r.length)break;case"[object Object]":{if(es(t,r))return!0;let n=en(t),i=en(r),a=n.length;if(a!==i.length)return!1;for(let e=0;e<a;++e)if(!Z.call(r,n[e]))return!1;for(let i=0;i<a;++i){let a=n[i];if(!e(t[a],r[a]))return!1}return!0}case"[object Error]":return t.name===r.name&&t.message===r.message;case"[object Number]":if(t!=t)return r!=r;case"[object Boolean]":case"[object Date]":return+t==+r;case"[object RegExp]":case"[object String]":return t==`${r}`;case"[object Map]":case"[object Set]":{if(t.size!==r.size)return!1;if(es(t,r))return!0;let i=t.entries(),a="[object Map]"===n;for(;;){let t=i.next();if(t.done)break;let[n,s]=t.value;if(!r.has(n)||a&&!e(s,r.get(n)))return!1}return!0}case"[object Uint16Array]":case"[object Uint8Array]":case"[object Uint32Array]":case"[object Int32Array]":case"[object Int8Array]":case"[object Int16Array]":case"[object ArrayBuffer]":t=new Uint8Array(t),r=new Uint8Array(r);case"[object DataView]":{let e=t.byteLength;if(e===r.byteLength)for(;e--&&t[e]===r[e];);return -1===e}case"[object AsyncFunction]":case"[object GeneratorFunction]":case"[object AsyncGeneratorFunction]":case"[object Function]":{var i,a;let e,n=ee.call(t);if(n!==ee.call(r))return!1;return i=n,a=ea,!((e=i.length-a.length)>=0)||i.indexOf(a,e)!==e}}return!1}(e,t)}finally{et.clear()}}function en(e){return Object.keys(e).filter(ei,e)}function ei(e){return void 0!==this[e]}let ea="{ [native code] }";function es(e,t){let r=et.get(e);if(r){if(r.has(t))return!0}else et.set(e,r=new Set);return r.add(t),!1}function eo(){}e.s(["default",0,er,"equal",()=>er],69981);let eu="undefined"!=typeof WeakRef?WeakRef:function(e){return{deref:()=>e}},ed="undefined"!=typeof WeakMap?WeakMap:Map,el="undefined"!=typeof FinalizationRegistry?FinalizationRegistry:function(){return{register:eo,unregister:eo}};class ec{constructor(e=1/0,t=eo){this.max=e,this.dispose=t,this.map=new ed,this.newest=null,this.oldest=null,this.unfinalizedNodes=new Set,this.finalizationScheduled=!1,this.size=0,this.finalize=()=>{let e=this.unfinalizedNodes.values();for(let t=0;t<10024;t++){let t=e.next().value;if(!t)break;this.unfinalizedNodes.delete(t);let r=t.key;delete t.key,t.keyRef=new eu(r),this.registry.register(r,t,t)}this.unfinalizedNodes.size>0?queueMicrotask(this.finalize):this.finalizationScheduled=!1},this.registry=new el(this.deleteNode.bind(this))}has(e){return this.map.has(e)}get(e){let t=this.getNode(e);return t&&t.value}getNode(e){let t=this.map.get(e);if(t&&t!==this.newest){let{older:e,newer:r}=t;r&&(r.older=e),e&&(e.newer=r),t.older=this.newest,t.older.newer=t,t.newer=null,this.newest=t,t===this.oldest&&(this.oldest=r)}return t}set(e,t){let r=this.getNode(e);return r?r.value=t:(r={key:e,value:t,newer:null,older:this.newest},this.newest&&(this.newest.newer=r),this.newest=r,this.oldest=this.oldest||r,this.scheduleFinalization(r),this.map.set(e,r),this.size++,r.value)}clean(){for(;this.oldest&&this.size>this.max;)this.deleteNode(this.oldest)}deleteNode(e){e===this.newest&&(this.newest=e.older),e===this.oldest&&(this.oldest=e.newer),e.newer&&(e.newer.older=e.older),e.older&&(e.older.newer=e.newer),this.size--;let t=e.key||e.keyRef&&e.keyRef.deref();this.dispose(e.value,t),e.keyRef?this.registry.unregister(e):this.unfinalizedNodes.delete(e),t&&this.map.delete(t)}delete(e){let t=this.map.get(e);return!!t&&(this.deleteNode(t),!0)}scheduleFinalization(e){this.unfinalizedNodes.add(e),this.finalizationScheduled||(this.finalizationScheduled=!0,queueMicrotask(this.finalize))}}function ep(){}e.s(["WeakCache",()=>ec],52812);class em{constructor(e=1/0,t=ep){this.max=e,this.dispose=t,this.map=new Map,this.newest=null,this.oldest=null}has(e){return this.map.has(e)}get(e){let t=this.getNode(e);return t&&t.value}get size(){return this.map.size}getNode(e){let t=this.map.get(e);if(t&&t!==this.newest){let{older:e,newer:r}=t;r&&(r.older=e),e&&(e.newer=r),t.older=this.newest,t.older.newer=t,t.newer=null,this.newest=t,t===this.oldest&&(this.oldest=r)}return t}set(e,t){let r=this.getNode(e);return r?r.value=t:(r={key:e,value:t,newer:null,older:this.newest},this.newest&&(this.newest.newer=r),this.newest=r,this.oldest=this.oldest||r,this.map.set(e,r),r.value)}clean(){for(;this.oldest&&this.map.size>this.max;)this.delete(this.oldest.key)}delete(e){let t=this.map.get(e);return!!t&&(t===this.newest&&(this.newest=t.older),t===this.oldest&&(this.oldest=t.newer),t.newer&&(t.newer.older=t.older),t.older&&(t.older.newer=t.newer),this.map.delete(e),this.dispose(t.value,e),!0)}}var eh=new WeakSet;function ef(e){!(e.size<=(e.max||-1))&&(eh.has(e)||(eh.add(e),setTimeout(function(){e.clean(),eh.delete(e)},100)))}var ev=function(e,t){var r=new ec(e,t);return r.set=function(e,t){var r=ec.prototype.set.call(this,e,t);return ef(this),r},r},ey=function(e,t){var r=new em(e,t);return r.set=function(e,t){var r=em.prototype.set.call(this,e,t);return ef(this),r},r};e.s(["AutoCleanedStrongCache",()=>ey,"AutoCleanedWeakCache",()=>ev],34049);var eg=Symbol.for("apollo.cacheSize"),eb=m({},O[eg]);e.s(["cacheSizes",()=>eb],72243);var eI={};function eA(e,t){eI[e]=t}var eN=!1!==globalThis.__DEV__?function(){var e,t,r,n,i;if(!1===globalThis.__DEV__)throw Error("only supported in development mode");return{limits:Object.fromEntries(Object.entries({parser:1e3,canonicalStringify:1e3,print:2e3,"documentTransform.cache":2e3,"queryManager.getDocumentInfo":2e3,"PersistedQueryLink.persistedQueryHashes":2e3,"fragmentRegistry.transform":2e3,"fragmentRegistry.lookup":1e3,"fragmentRegistry.findFragmentSpreads":4e3,"cache.fragmentQueryDocuments":1e3,"removeTypenameFromVariables.getVariableDefinitions":2e3,"inMemoryCache.maybeBroadcastWatch":5e3,"inMemoryCache.executeSelectionSet":5e4,"inMemoryCache.executeSubSelectedArray":1e4}).map(function(e){var t=e[0],r=e[1];return[t,eb[t]||r]})),sizes:m({print:null==(e=eI.print)?void 0:e.call(eI),parser:null==(t=eI.parser)?void 0:t.call(eI),canonicalStringify:null==(r=eI.canonicalStringify)?void 0:r.call(eI),links:function e(t){var r;return t?y(y([null==(r=null==t?void 0:t.getMemoryInternals)?void 0:r.call(t)],e(null==t?void 0:t.left),!0),e(null==t?void 0:t.right),!0).filter(e_):[]}(this.link),queryManager:{getDocumentInfo:this.queryManager.transformCache.size,documentTransforms:eO(this.queryManager.documentTransform)}},null==(i=(n=this.cache).getMemoryInternals)?void 0:i.call(n))}}:void 0,eE=!1!==globalThis.__DEV__?function(){var e=this.config.fragments;return m(m({},eP.apply(this)),{addTypenameDocumentTransform:eO(this.addTypenameTransform),inMemoryCache:{executeSelectionSet:eD(this.storeReader.executeSelectionSet),executeSubSelectedArray:eD(this.storeReader.executeSubSelectedArray),maybeBroadcastWatch:eD(this.maybeBroadcastWatch)},fragmentRegistry:{findFragmentSpreads:eD(null==e?void 0:e.findFragmentSpreads),lookup:eD(null==e?void 0:e.lookup),transform:eD(null==e?void 0:e.transform)}})}:void 0,eT=!1!==globalThis.__DEV__?eP:void 0;function eP(){return{cache:{fragmentQueryDocuments:eD(this.getFragmentDoc)}}}function eD(e){return e&&"dirtyKey"in e?e.size:void 0}function e_(e){return null!=e}function eO(e){return(function e(t){return t?y(y([eD(null==t?void 0:t.performWork)],e(null==t?void 0:t.left),!0),e(null==t?void 0:t.right),!0).filter(e_):[]})(e).map(function(e){return{cache:e}})}e.s(["getApolloCacheMemoryInternals",()=>eT,"getApolloClientMemoryInternals",()=>eN,"getInMemoryCacheMemoryInternals",()=>eE,"registerGlobalCache",()=>eA],20871);let eC=()=>Object.create(null),{forEach:eR,slice:eS}=Array.prototype,{hasOwnProperty:ew}=Object.prototype;class ek{constructor(e=!0,t=eC){this.weakness=e,this.makeData=t}lookup(){return this.lookupArray(arguments)}lookupArray(e){let t=this;return eR.call(e,e=>t=t.getChildTrie(e)),ew.call(t,"data")?t.data:t.data=this.makeData(eS.call(e))}peek(){return this.peekArray(arguments)}peekArray(e){let t=this;for(let r=0,n=e.length;t&&r<n;++r){let n=t.mapFor(e[r],!1);t=n&&n.get(e[r])}return t&&t.data}remove(){return this.removeArray(arguments)}removeArray(e){let t;if(e.length){let r=e[0],n=this.mapFor(r,!1),i=n&&n.get(r);i&&(t=i.removeArray(eS.call(e,1)),i.data||i.weak||i.strong&&i.strong.size||n.delete(r))}else t=this.data,delete this.data;return t}getChildTrie(e){let t=this.mapFor(e,!0),r=t.get(e);return r||t.set(e,r=new ek(this.weakness,this.makeData)),r}mapFor(e,t){return this.weakness&&function(e){switch(typeof e){case"object":if(null===e)break;case"function":return!0}return!1}(e)?this.weak||(t?this.weak=new WeakMap:void 0):this.strong||(t?this.strong=new Map:void 0)}}e.s(["Trie",()=>ek],40059);let eL=null,eF={},eM=1;function e$(e){try{return e()}catch(e){}}let ex="@wry/context:Slot",eV=e$(()=>globalThis)||e$(()=>e.g)||Object.create(null),eU=eV[ex]||Array[ex]||function(e){try{Object.defineProperty(eV,ex,{value:e,enumerable:!1,writable:!1,configurable:!0})}finally{return e}}(class{constructor(){this.id=["slot",eM++,Date.now(),Math.random().toString(36).slice(2)].join(":")}hasValue(){for(let e=eL;e;e=e.parent)if(this.id in e.slots){let t=e.slots[this.id];if(t===eF)break;return e!==eL&&(eL.slots[this.id]=t),!0}return eL&&(eL.slots[this.id]=eF),!1}getValue(){if(this.hasValue())return eL.slots[this.id]}withValue(e,t,r,n){let i={__proto__:null,[this.id]:e},a=eL;eL={parent:a,slots:i};try{return t.apply(n,r)}finally{eL=a}}static bind(e){let t=eL;return function(){let r=eL;try{return eL=t,e.apply(this,arguments)}finally{eL=r}}}static noContext(e,t,r){if(!eL)return e.apply(r,t);{let n=eL;try{return eL=null,e.apply(r,t)}finally{eL=n}}}});e.s(["Slot",0,eU],12337);let{bind:eq,noContext:eB}=eU,ej=new eU,{hasOwnProperty:eQ}=Object.prototype,ez=Array.from||function(e){let t=[];return e.forEach(e=>t.push(e)),t};function eG(e){let{unsubscribe:t}=e;"function"==typeof t&&(e.unsubscribe=void 0,t())}let eK=[];function eH(e,t){if(!e)throw Error(t||"assertion failure")}function eY(e,t){let r=e.length;return r>0&&r===t.length&&e[r-1]===t[r-1]}function eX(e){switch(e.length){case 0:throw Error("unknown value");case 1:return e[0];case 2:throw e[1]}}class eW{constructor(e){this.fn=e,this.parents=new Set,this.childValues=new Map,this.dirtyChildren=null,this.dirty=!0,this.recomputing=!1,this.value=[],this.deps=null,++eW.count}peek(){if(1===this.value.length&&!e0(this))return eJ(this),this.value[0]}recompute(e){var t,r,n;return eH(!this.recomputing,"already recomputing"),eJ(this),e0(this)?(t=this,r=e,e9(t),ej.withValue(t,eZ,[t,r]),function(e,t){if("function"==typeof e.subscribe)try{eG(e),e.unsubscribe=e.subscribe.apply(null,t)}catch(t){return e.setDirty(),!1}return!0}(t,r)&&((n=t).dirty=!1,e0(n)||function(e){e1(e,e2)}(n)),eX(t.value)):eX(this.value)}setDirty(){var e;this.dirty||(this.dirty=!0,e=this,e1(e,e3),eG(this))}dispose(){this.setDirty(),e9(this),e1(this,(e,t)=>{e.setDirty(),e5(e,this)})}forget(){this.dispose()}dependOn(e){e.add(this),this.deps||(this.deps=eK.pop()||new Set),this.deps.add(e)}forgetDeps(){this.deps&&(ez(this.deps).forEach(e=>e.delete(this)),this.deps.clear(),eK.push(this.deps),this.deps=null)}}function eJ(e){let t=ej.getValue();if(t)return e.parents.add(t),t.childValues.has(e)||t.childValues.set(e,[]),e0(e)?e3(t,e):e2(t,e),t}function eZ(e,t){let r;e.recomputing=!0;let{normalizeResult:n}=e;n&&1===e.value.length&&(r=e.value.slice(0)),e.value.length=0;try{if(e.value[0]=e.fn.apply(null,t),n&&r&&!eY(r,e.value))try{e.value[0]=n(e.value[0],r[0])}catch(e){}}catch(t){e.value[1]=t}e.recomputing=!1}function e0(e){return e.dirty||!!(e.dirtyChildren&&e.dirtyChildren.size)}eW.count=0;function e1(e,t){let r=e.parents.size;if(r){let n=ez(e.parents);for(let i=0;i<r;++i)t(n[i],e)}}function e3(e,t){eH(e.childValues.has(t)),eH(e0(t));let r=!e0(e);if(e.dirtyChildren){if(e.dirtyChildren.has(t))return}else e.dirtyChildren=eK.pop()||new Set;e.dirtyChildren.add(t),r&&e1(e,e3)}function e2(e,t){eH(e.childValues.has(t)),eH(!e0(t));let r=e.childValues.get(t);0===r.length?e.childValues.set(t,t.value.slice(0)):eY(r,t.value)||e.setDirty(),e4(e,t),e0(e)||e1(e,e2)}function e4(e,t){let r=e.dirtyChildren;r&&(r.delete(t),0===r.size&&(eK.length<100&&eK.push(r),e.dirtyChildren=null))}function e9(e){e.childValues.size>0&&e.childValues.forEach((t,r)=>{e5(e,r)}),e.forgetDeps(),eH(null===e.dirtyChildren)}function e5(e,t){t.parents.delete(e),e.childValues.delete(t),e4(e,t)}let e8={setDirty:!0,dispose:!0,forget:!0};function e6(e){let t=new Map,r=e&&e.subscribe;function n(e){let n=ej.getValue();if(n){let i=t.get(e);i||t.set(e,i=new Set),n.dependOn(i),"function"==typeof r&&(eG(i),i.unsubscribe=r(e))}}return n.dirty=function(e,r){let n=t.get(e);if(n){let i=r&&eQ.call(e8,r)?r:"setDirty";ez(n).forEach(e=>e[i]()),t.delete(e),eG(n)}},n}function e7(...e){return(t||(t=new ek("function"==typeof WeakMap))).lookupArray(e)}e.s(["dep",()=>e6],2693);let te=new Set;function tt(e,{max:t=65536,keyArgs:r,makeCacheKey:n=e7,normalizeResult:i,subscribe:a,cache:s=em}=Object.create(null)){let o="function"==typeof s?new s(t,e=>e.dispose()):s,u=function(){let t=n.apply(null,r?r.apply(null,arguments):arguments);if(void 0===t)return e.apply(null,arguments);let s=o.get(t);s||(o.set(t,s=new eW(e)),s.normalizeResult=i,s.subscribe=a,s.forget=()=>o.delete(t));let u=s.recompute(Array.prototype.slice.call(arguments));return o.set(t,s),te.add(o),ej.hasValue()||(te.forEach(e=>e.clean()),te.clear()),u};function d(e){let t=e&&o.get(e);t&&t.setDirty()}function l(e){let t=e&&o.get(e);if(t)return t.peek()}function c(e){return!!e&&o.delete(e)}return Object.defineProperty(u,"size",{get:()=>o.size,configurable:!1,enumerable:!1}),Object.freeze(u.options={max:t,keyArgs:r,makeCacheKey:n,normalizeResult:i,subscribe:a,cache:o}),u.dirtyKey=d,u.dirty=function(){d(n.apply(null,arguments))},u.peekKey=l,u.peek=function(){return l(n.apply(null,arguments))},u.forgetKey=c,u.forget=function(){return c(n.apply(null,arguments))},u.makeCacheKey=n,u.getKey=r?function(){return n.apply(null,r.apply(null,arguments))}:n,Object.freeze(u)}e.s(["wrap",()=>tt],95364);var tr=Symbol.for("apollo.deprecations"),tn=Symbol.for("apollo.deprecations.slot"),ti=null!=(s=O[tn])?s:O[tn]=new eU;function ta(e){for(var t=[],r=1;r<arguments.length;r++)t[r-1]=arguments[r];return ti.withValue.apply(ti,y([Array.isArray(e)?e:[e]],t,!1))}function ts(e,t,r,n){void 0===n&&(n="Please remove this option."),to(t,function(){t in e&&!1!==globalThis.__DEV__&&k.warn(103,r,t,n)})}function to(e,t){O[tr]||(ti.getValue()||[]).includes(e)||t()}function tu(e){return null!==e&&"object"==typeof e}e.s(["muteDeprecations",()=>ta,"warnDeprecated",()=>to,"warnRemovedOption",()=>ts],630),e.s(["isNonNullObject",()=>tu],19983);var td=Symbol();function tl(e){return!!e.extensions&&Array.isArray(e.extensions[td])}function tc(e){return e.hasOwnProperty("graphQLErrors")}var tp=function(e){var t=y(y(y([],e.graphQLErrors,!0),e.clientErrors,!0),e.protocolErrors,!0);return e.networkError&&t.push(e.networkError),t.map(function(e){return tu(e)&&e.message||"Error message not found."}).join("\n")},tm=function(e){function t(r){var n=r.graphQLErrors,i=r.protocolErrors,a=r.clientErrors,s=r.networkError,o=r.errorMessage,u=r.extraInfo,d=e.call(this,o)||this;return d.name="ApolloError",d.graphQLErrors=n||[],d.protocolErrors=i||[],d.clientErrors=a||[],d.networkError=s||null,d.message=o||tp(d),d.extraInfo=u,d.cause=y(y(y([s],n||[],!0),i||[],!0),a||[],!0).find(function(e){return!!e})||null,d.__proto__=t.prototype,d}return p(t,e),t}(Error);function th(e){return!!e&&e<7}e.s(["ApolloError",()=>tm,"PROTOCOL_ERRORS_SYMBOL",()=>td,"graphQLResultHasProtocolErrors",()=>tl,"isApolloError",()=>tc],21110),(n=o||(o={}))[n.loading=1]="loading",n[n.setVariables=2]="setVariables",n[n.fetchMore=3]="fetchMore",n[n.refetch=4]="refetch",n[n.poll=6]="poll",n[n.ready=7]="ready",n[n.error=8]="error",e.s(["NetworkStatus",()=>o,"isNetworkRequestInFlight",()=>th],7799);var tf=Object.prototype.toString;function tv(e){return ty(e)}function ty(e,t){switch(tf.call(e)){case"[object Array]":if((t=t||new Map).has(e))return t.get(e);var r=e.slice(0);return t.set(e,r),r.forEach(function(e,n){r[n]=ty(e,t)}),r;case"[object Object]":if((t=t||new Map).has(e))return t.get(e);var n=Object.create(Object.getPrototypeOf(e));return t.set(e,n),Object.keys(e).forEach(function(r){n[r]=ty(e[r],t)}),n;default:return e}}function tg(e,t){if(!e)throw Error(t)}function tb(e){return function e(t,r){switch(typeof t){case"string":return JSON.stringify(t);case"function":return t.name?`[function ${t.name}]`:"[function]";case"object":return function(t,r){let n;if(null===t)return"null";if(r.includes(t))return"[Circular]";let i=[...r,t];if("function"==typeof t.toJSON){let r=t.toJSON();if(r!==t)return"string"==typeof r?r:e(r,i)}else if(Array.isArray(t)){var a,s,o=t,u=i;if(0===o.length)return"[]";if(u.length>2)return"[Array]";let r=Math.min(10,o.length),n=o.length-r,d=[];for(let t=0;t<r;++t)d.push(e(o[t],u));return 1===n?d.push("... 1 more item"):n>1&&d.push(`... ${n} more items`),"["+d.join(", ")+"]"}return a=t,s=i,0===(n=Object.entries(a)).length?"{}":s.length>2?"["+function(e){let t=Object.prototype.toString.call(e).replace(/^\[object /,"").replace(/]$/,"");if("Object"===t&&"function"==typeof e.constructor){let t=e.constructor.name;if("string"==typeof t&&""!==t)return t}return t}(a)+"]":"{ "+n.map(([t,r])=>t+": "+e(r,s)).join(", ")+" }"}(t,r);default:return String(t)}}(e,[])}e.s(["cloneDeep",()=>tv],95077),e.s(["devAssert",()=>tg],36535);e.s(["inspect",()=>tb],41888);class tI{constructor(e,t,r){this.start=e.start,this.end=t.end,this.startToken=e,this.endToken=t,this.source=r}get[Symbol.toStringTag](){return"Location"}toJSON(){return{start:this.start,end:this.end}}}class tA{constructor(e,t,r,n,i,a){this.kind=e,this.start=t,this.end=r,this.line=n,this.column=i,this.value=a,this.prev=null,this.next=null}get[Symbol.toStringTag](){return"Token"}toJSON(){return{kind:this.kind,value:this.value,line:this.line,column:this.column}}}let tN={Name:[],Document:["definitions"],OperationDefinition:["description","name","variableDefinitions","directives","selectionSet"],VariableDefinition:["description","variable","type","defaultValue","directives"],Variable:["name"],SelectionSet:["selections"],Field:["alias","name","arguments","directives","selectionSet"],Argument:["name","value"],FragmentSpread:["name","directives"],InlineFragment:["typeCondition","directives","selectionSet"],FragmentDefinition:["description","name","variableDefinitions","typeCondition","directives","selectionSet"],IntValue:[],FloatValue:[],StringValue:[],BooleanValue:[],NullValue:[],EnumValue:[],ListValue:["values"],ObjectValue:["fields"],ObjectField:["name","value"],Directive:["name","arguments"],NamedType:["name"],ListType:["type"],NonNullType:["type"],SchemaDefinition:["description","directives","operationTypes"],OperationTypeDefinition:["type"],ScalarTypeDefinition:["description","name","directives"],ObjectTypeDefinition:["description","name","interfaces","directives","fields"],FieldDefinition:["description","name","arguments","type","directives"],InputValueDefinition:["description","name","type","defaultValue","directives"],InterfaceTypeDefinition:["description","name","interfaces","directives","fields"],UnionTypeDefinition:["description","name","directives","types"],EnumTypeDefinition:["description","name","directives","values"],EnumValueDefinition:["description","name","directives"],InputObjectTypeDefinition:["description","name","directives","fields"],DirectiveDefinition:["description","name","arguments","directives","locations"],SchemaExtension:["directives","operationTypes"],DirectiveExtension:["name","directives"],ScalarTypeExtension:["name","directives"],ObjectTypeExtension:["name","interfaces","directives","fields"],InterfaceTypeExtension:["name","interfaces","directives","fields"],UnionTypeExtension:["name","directives","types"],EnumTypeExtension:["name","directives","values"],InputObjectTypeExtension:["name","directives","fields"],TypeCoordinate:["name"],MemberCoordinate:["name","memberName"],ArgumentCoordinate:["name","fieldName","argumentName"],DirectiveCoordinate:["name"],DirectiveArgumentCoordinate:["name","argumentName"]},tE=new Set(Object.keys(tN));function tT(e){let t=null==e?void 0:e.kind;return"string"==typeof t&&tE.has(t)}(i=u||(u={})).QUERY="query",i.MUTATION="mutation",i.SUBSCRIPTION="subscription",e.s(["Location",()=>tI,"OperationTypeNode",()=>u,"QueryDocumentKeys",0,tN,"Token",()=>tA,"isNode",()=>tT],20064),(a=d||(d={})).NAME="Name",a.DOCUMENT="Document",a.OPERATION_DEFINITION="OperationDefinition",a.VARIABLE_DEFINITION="VariableDefinition",a.SELECTION_SET="SelectionSet",a.FIELD="Field",a.ARGUMENT="Argument",a.FRAGMENT_SPREAD="FragmentSpread",a.INLINE_FRAGMENT="InlineFragment",a.FRAGMENT_DEFINITION="FragmentDefinition",a.VARIABLE="Variable",a.INT="IntValue",a.FLOAT="FloatValue",a.STRING="StringValue",a.BOOLEAN="BooleanValue",a.NULL="NullValue",a.ENUM="EnumValue",a.LIST="ListValue",a.OBJECT="ObjectValue",a.OBJECT_FIELD="ObjectField",a.DIRECTIVE="Directive",a.NAMED_TYPE="NamedType",a.LIST_TYPE="ListType",a.NON_NULL_TYPE="NonNullType",a.SCHEMA_DEFINITION="SchemaDefinition",a.OPERATION_TYPE_DEFINITION="OperationTypeDefinition",a.SCALAR_TYPE_DEFINITION="ScalarTypeDefinition",a.OBJECT_TYPE_DEFINITION="ObjectTypeDefinition",a.FIELD_DEFINITION="FieldDefinition",a.INPUT_VALUE_DEFINITION="InputValueDefinition",a.INTERFACE_TYPE_DEFINITION="InterfaceTypeDefinition",a.UNION_TYPE_DEFINITION="UnionTypeDefinition",a.ENUM_TYPE_DEFINITION="EnumTypeDefinition",a.ENUM_VALUE_DEFINITION="EnumValueDefinition",a.INPUT_OBJECT_TYPE_DEFINITION="InputObjectTypeDefinition",a.DIRECTIVE_DEFINITION="DirectiveDefinition",a.SCHEMA_EXTENSION="SchemaExtension",a.DIRECTIVE_EXTENSION="DirectiveExtension",a.SCALAR_TYPE_EXTENSION="ScalarTypeExtension",a.OBJECT_TYPE_EXTENSION="ObjectTypeExtension",a.INTERFACE_TYPE_EXTENSION="InterfaceTypeExtension",a.UNION_TYPE_EXTENSION="UnionTypeExtension",a.ENUM_TYPE_EXTENSION="EnumTypeExtension",a.INPUT_OBJECT_TYPE_EXTENSION="InputObjectTypeExtension",a.TYPE_COORDINATE="TypeCoordinate",a.MEMBER_COORDINATE="MemberCoordinate",a.ARGUMENT_COORDINATE="ArgumentCoordinate",a.DIRECTIVE_COORDINATE="DirectiveCoordinate",a.DIRECTIVE_ARGUMENT_COORDINATE="DirectiveArgumentCoordinate",e.s(["Kind",()=>d],85809);let tP=Object.freeze({});function tD(e,t,r=tN){let n,i,a,s=new Map;for(let e of Object.values(d))s.set(e,function(e,t){let r=e[t];return"object"==typeof r?r:"function"==typeof r?{enter:r,leave:void 0}:{enter:e.enter,leave:e.leave}}(t,e));let o=Array.isArray(e),u=[e],l=-1,c=[],p=e,m=[],h=[];do{var f,v,y;let e,d=++l===u.length,g=d&&0!==c.length;if(d){if(i=0===h.length?void 0:m[m.length-1],p=a,a=h.pop(),g)if(o){p=p.slice();let e=0;for(let[t,r]of c){let n=t-e;null===r?(p.splice(n,1),e++):p[n]=r}}else for(let[e,t]of(p={...p},c))p[e]=t;l=n.index,u=n.keys,c=n.edits,o=n.inArray,n=n.prev}else if(a){if(null==(p=a[i=o?l:u[l]]))continue;m.push(i)}if(!Array.isArray(p)){tT(p)||tg(!1,`Invalid AST Node: ${tb(p)}.`);let r=d?null==(f=s.get(p.kind))?void 0:f.leave:null==(v=s.get(p.kind))?void 0:v.enter;if((e=null==r?void 0:r.call(t,p,i,a,m,h))===tP)break;if(!1===e){if(!d){m.pop();continue}}else if(void 0!==e&&(c.push([i,e]),!d))if(tT(e))p=e;else{m.pop();continue}}void 0===e&&g&&c.push([i,p]),d?m.pop():(n={inArray:o,index:l,keys:u,edits:c,prev:n},u=(o=Array.isArray(p))?p:null!=(y=r[p.kind])?y:[],l=-1,c=[],a&&h.push(a),a=p)}while(void 0!==n)return 0!==c.length?c[c.length-1][1]:e}function t_(e,t){var r=t,n=[];return e.definitions.forEach(function(e){if("OperationDefinition"===e.kind)throw L(112,e.operation,e.name?" named '".concat(e.name.value,"'"):"");"FragmentDefinition"===e.kind&&n.push(e)}),void 0===r&&(k(1===n.length,113,n.length),r=n[0].name.value),m(m({},e),{definitions:y([{kind:"OperationDefinition",operation:"query",selectionSet:{kind:"SelectionSet",selections:[{kind:"FragmentSpread",name:{kind:"Name",value:r}}]}}],e.definitions,!0)})}function tO(e){void 0===e&&(e=[]);var t={};return e.forEach(function(e){t[e.name.value]=e}),t}function tC(e,t){switch(e.kind){case"InlineFragment":return e;case"FragmentSpread":var r=e.name.value;if("function"==typeof t)return t(r);var n=t&&t[r];return k(n,114,r),n||null;default:return null}}function tR(e){var t=!0;return tD(e,{FragmentSpread:function(e){if(!(t=!!e.directives&&e.directives.some(function(e){return"unmask"===e.name.value})))return tP}}),t}e.s(["BREAK",0,tP,"visit",()=>tD],98943),e.s(["createFragmentMap",()=>tO,"getFragmentFromSelection",()=>tC,"getFragmentQueryDocument",()=>t_,"isFullyUnmaskedOperation",()=>tR],61518);var tS=Object.assign(function(e){return JSON.stringify(e,tw)},{reset:function(){l=new ey(eb.canonicalStringify||1e3)}});function tw(e,t){if(t&&"object"==typeof t){var r=Object.getPrototypeOf(t);if(r===Object.prototype||null===r){var n=Object.keys(t);if(n.every(tk))return t;var i=JSON.stringify(n),a=l.get(i);if(!a){n.sort();var s=JSON.stringify(n);a=l.get(s)||n,l.set(i,a),l.set(s,a)}var o=Object.create(r);return a.forEach(function(e){o[e]=t[e]}),o}}return t}function tk(e,t,r){return 0===t||r[t-1]<=e}function tL(e){return{__ref:String(e)}}function tF(e){return!!(e&&"object"==typeof e&&"string"==typeof e.__ref)}function tM(e){return tu(e)&&"Document"===e.kind&&Array.isArray(e.definitions)}function t$(e,t,r,n){if("IntValue"===r.kind||"FloatValue"===r.kind)e[t.value]=Number(r.value);else if("BooleanValue"===r.kind||"StringValue"===r.kind)e[t.value]=r.value;else if("ObjectValue"===r.kind){var i={};r.fields.map(function(e){return t$(i,e.name,e.value,n)}),e[t.value]=i}else if("Variable"===r.kind){var a=(n||{})[r.name.value];e[t.value]=a}else if("ListValue"===r.kind)e[t.value]=r.values.map(function(e){var r={};return t$(r,t,e,n),r[t.value]});else if("EnumValue"===r.kind)e[t.value]=r.value;else if("NullValue"===r.kind)e[t.value]=null;else throw L(123,t.value,r.kind)}function tx(e,t){var r=null;e.directives&&(r={},e.directives.forEach(function(e){r[e.name.value]={},e.arguments&&e.arguments.forEach(function(n){var i=n.name,a=n.value;return t$(r[e.name.value],i,a,t)})}));var n=null;return e.arguments&&e.arguments.length&&(n={},e.arguments.forEach(function(e){var r=e.name,i=e.value;return t$(n,r,i,t)})),tq(e.name.value,n,r)}!1!==globalThis.__DEV__&&(eI.canonicalStringify=function(){return l.size}),tS.reset(),e.s(["canonicalStringify",()=>tS],49987);var tV=["connection","include","skip","client","rest","export","nonreactive"],tU=tS,tq=Object.assign(function(e,t,r){if(t&&r&&r.connection&&r.connection.key)if(!r.connection.filter||!(r.connection.filter.length>0))return r.connection.key;else{var n=r.connection.filter?r.connection.filter:[];n.sort();var i={};return n.forEach(function(e){i[e]=t[e]}),"".concat(r.connection.key,"(").concat(tU(i),")")}var a=e;if(t){var s=tU(t);a+="(".concat(s,")")}return r&&Object.keys(r).forEach(function(e){-1===tV.indexOf(e)&&(r[e]&&Object.keys(r[e]).length?a+="@".concat(e,"(").concat(tU(r[e]),")"):a+="@".concat(e))}),a},{setStringify:function(e){var t=tU;return tU=e,t}});function tB(e,t){if(e.arguments&&e.arguments.length){var r={};return e.arguments.forEach(function(e){return t$(r,e.name,e.value,t)}),r}return null}function tj(e){return e.alias?e.alias.value:e.name.value}function tQ(e){return"Field"===e.kind}function tz(e){return"InlineFragment"===e.kind}function tG(e){k(e&&"Document"===e.kind,115);var t=e.definitions.filter(function(e){return"FragmentDefinition"!==e.kind}).map(function(e){if("OperationDefinition"!==e.kind)throw L(116,e.kind);return e});return k(t.length<=1,117,t.length),e}function tK(e){return tG(e),e.definitions.filter(function(e){return"OperationDefinition"===e.kind})[0]}function tH(e){return e.definitions.filter(function(e){return"OperationDefinition"===e.kind&&!!e.name}).map(function(e){return e.name.value})[0]||null}function tY(e){return e.definitions.filter(function(e){return"FragmentDefinition"===e.kind})}function tX(e){var t=tK(e);return k(t&&"query"===t.operation,118),t}function tW(e){k("Document"===e.kind,119),k(e.definitions.length<=1,120);var t=e.definitions[0];return k("FragmentDefinition"===t.kind,121),t}function tJ(e){tG(e);for(var t,r=0,n=e.definitions;r<n.length;r++){var i=n[r];if("OperationDefinition"===i.kind){var a=i.operation;if("query"===a||"mutation"===a||"subscription"===a)return i}"FragmentDefinition"!==i.kind||t||(t=i)}if(t)return t;throw L(122)}function tZ(e){var t=Object.create(null),r=e&&e.variableDefinitions;return r&&r.length&&r.forEach(function(e){e.defaultValue&&t$(t,e.variable.name,e.defaultValue)}),t}function t0(e,t){(null==t||t>e.length)&&(t=e.length);for(var r=0,n=Array(t);r<t;r++)n[r]=e[r];return n}function t1(e,t){for(var r=0;r<t.length;r++){var n=t[r];n.enumerable=n.enumerable||!1,n.configurable=!0,"value"in n&&(n.writable=!0),Object.defineProperty(e,n.key,n)}}function t3(e,t,r){return t&&t1(e.prototype,t),r&&t1(e,r),Object.defineProperty(e,"prototype",{writable:!1}),e}e.s(["argumentsObjectFromField",()=>tB,"getStoreKeyName",()=>tq,"getTypenameFromResult",()=>function e(t,r,n){for(var i,a=0,s=r.selections;a<s.length;a++){var o=s[a];if(tQ(o)){if("__typename"===o.name.value)return t[tj(o)]}else i?i.push(o):i=[o]}if("string"==typeof t.__typename)return t.__typename;if(i)for(var u=0,d=i;u<d.length;u++){var o=d[u],l=e(t,tC(o,n).selectionSet,n);if("string"==typeof l)return l}},"isDocumentNode",()=>tM,"isField",()=>tQ,"isInlineFragment",()=>tz,"isReference",()=>tF,"makeReference",()=>tL,"resultKeyNameFromField",()=>tj,"storeKeyNameFromField",()=>tx,"valueToObjectRepresentation",()=>t$],53949),e.s(["checkDocument",()=>tG,"getDefaultValues",()=>tZ,"getFragmentDefinition",()=>tW,"getFragmentDefinitions",()=>tY,"getMainDefinition",()=>tJ,"getOperationDefinition",()=>tK,"getOperationName",()=>tH,"getQueryDefinition",()=>tX],58350);var t2=function(){return"function"==typeof Symbol},t4=function(e){return t2()&&!!Symbol[e]},t9=function(e){return t4(e)?Symbol[e]:"@@"+e};t2()&&!t4("observable")&&(Symbol.observable=Symbol("observable"));var t5=t9("iterator"),t8=t9("observable"),t6=t9("species");function t7(e,t){var r=e[t];if(null!=r){if("function"!=typeof r)throw TypeError(r+" is not a function");return r}}function re(e){var t=e.constructor;return void 0!==t&&null===(t=t[t6])&&(t=void 0),void 0!==t?t:rd}function rt(e){rt.log?rt.log(e):setTimeout(function(){throw e})}function rr(e){Promise.resolve().then(function(){try{e()}catch(e){rt(e)}})}function rn(e){var t=e._cleanup;if(void 0!==t&&(e._cleanup=void 0,t))try{if("function"==typeof t)t();else{var r=t7(t,"unsubscribe");r&&r.call(t)}}catch(e){rt(e)}}function ri(e){e._observer=void 0,e._queue=void 0,e._state="closed"}function ra(e,t,r){e._state="running";var n=e._observer;try{var i=t7(n,t);switch(t){case"next":i&&i.call(n,r);break;case"error":if(ri(e),i)i.call(n,r);else throw r;break;case"complete":ri(e),i&&i.call(n)}}catch(e){rt(e)}"closed"===e._state?rn(e):"running"===e._state&&(e._state="ready")}function rs(e,t,r){if("closed"!==e._state){if("buffering"===e._state)return void e._queue.push({type:t,value:r});if("ready"!==e._state){e._state="buffering",e._queue=[{type:t,value:r}],rr(function(){var t=e._queue;if(t){e._queue=void 0,e._state="ready";for(var r=0;r<t.length&&(ra(e,t[r].type,t[r].value),"closed"!==e._state);++r);}});return}ra(e,t,r)}}var ro=function(){function e(e,t){this._cleanup=void 0,this._observer=e,this._queue=void 0,this._state="initializing";var r=new ru(this);try{this._cleanup=t.call(void 0,r)}catch(e){r.error(e)}"initializing"===this._state&&(this._state="ready")}return e.prototype.unsubscribe=function(){"closed"!==this._state&&(ri(this),rn(this))},t3(e,[{key:"closed",get:function(){return"closed"===this._state}}]),e}(),ru=function(){function e(e){this._subscription=e}var t=e.prototype;return t.next=function(e){rs(this._subscription,"next",e)},t.error=function(e){rs(this._subscription,"error",e)},t.complete=function(){rs(this._subscription,"complete")},t3(e,[{key:"closed",get:function(){return"closed"===this._subscription._state}}]),e}(),rd=function(){function e(t){if(!(this instanceof e))throw TypeError("Observable cannot be called as a function");if("function"!=typeof t)throw TypeError("Observable initializer must be a function");this._subscriber=t}var t=e.prototype;return t.subscribe=function(e){return("object"!=typeof e||null===e)&&(e={next:e,error:arguments[1],complete:arguments[2]}),new ro(e,this._subscriber)},t.forEach=function(e){var t=this;return new Promise(function(r,n){if("function"!=typeof e)return void n(TypeError(e+" is not a function"));function i(){a.unsubscribe(),r()}var a=t.subscribe({next:function(t){try{e(t,i)}catch(e){n(e),a.unsubscribe()}},error:n,complete:r})})},t.map=function(e){var t=this;if("function"!=typeof e)throw TypeError(e+" is not a function");return new(re(this))(function(r){return t.subscribe({next:function(t){try{t=e(t)}catch(e){return r.error(e)}r.next(t)},error:function(e){r.error(e)},complete:function(){r.complete()}})})},t.filter=function(e){var t=this;if("function"!=typeof e)throw TypeError(e+" is not a function");return new(re(this))(function(r){return t.subscribe({next:function(t){try{if(!e(t))return}catch(e){return r.error(e)}r.next(t)},error:function(e){r.error(e)},complete:function(){r.complete()}})})},t.reduce=function(e){var t=this;if("function"!=typeof e)throw TypeError(e+" is not a function");var r=re(this),n=arguments.length>1,i=!1,a=arguments[1],s=a;return new r(function(r){return t.subscribe({next:function(t){var a=!i;if(i=!0,!a||n)try{s=e(s,t)}catch(e){return r.error(e)}else s=t},error:function(e){r.error(e)},complete:function(){if(!i&&!n)return r.error(TypeError("Cannot reduce an empty sequence"));r.next(s),r.complete()}})})},t.concat=function(){for(var e=this,t=arguments.length,r=Array(t),n=0;n<t;n++)r[n]=arguments[n];var i=re(this);return new i(function(t){var n,a=0;return!function e(s){n=s.subscribe({next:function(e){t.next(e)},error:function(e){t.error(e)},complete:function(){a===r.length?(n=void 0,t.complete()):e(i.from(r[a++]))}})}(e),function(){n&&(n.unsubscribe(),n=void 0)}})},t.flatMap=function(e){var t=this;if("function"!=typeof e)throw TypeError(e+" is not a function");var r=re(this);return new r(function(n){var i=[],a=t.subscribe({next:function(t){if(e)try{t=e(t)}catch(e){return n.error(e)}var a=r.from(t).subscribe({next:function(e){n.next(e)},error:function(e){n.error(e)},complete:function(){var e=i.indexOf(a);e>=0&&i.splice(e,1),s()}});i.push(a)},error:function(e){n.error(e)},complete:function(){s()}});function s(){a.closed&&0===i.length&&n.complete()}return function(){i.forEach(function(e){return e.unsubscribe()}),a.unsubscribe()}})},t[t8]=function(){return this},e.from=function(t){var r="function"==typeof this?this:e;if(null==t)throw TypeError(t+" is not an object");var n=t7(t,t8);if(n){var i=n.call(t);if(Object(i)!==i)throw TypeError(i+" is not an object");return i instanceof rd&&i.constructor===r?i:new r(function(e){return i.subscribe(e)})}if(t4("iterator")&&(n=t7(t,t5)))return new r(function(e){rr(function(){if(!e.closed){for(var r,i=function(e,t){var r="undefined"!=typeof Symbol&&e[Symbol.iterator]||e["@@iterator"];if(r)return(r=r.call(e)).next.bind(r);if(Array.isArray(e)||(r=function(e,t){if(e){if("string"==typeof e)return t0(e,void 0);var r=Object.prototype.toString.call(e).slice(8,-1);if("Object"===r&&e.constructor&&(r=e.constructor.name),"Map"===r||"Set"===r)return Array.from(e);if("Arguments"===r||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(r))return t0(e,void 0)}}(e))){r&&(e=r);var n=0;return function(){return n>=e.length?{done:!0}:{done:!1,value:e[n++]}}}throw TypeError("Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.")}(n.call(t));!(r=i()).done;){var a=r.value;if(e.next(a),e.closed)return}e.complete()}})});if(Array.isArray(t))return new r(function(e){rr(function(){if(!e.closed){for(var r=0;r<t.length;++r)if(e.next(t[r]),e.closed)return;e.complete()}})});throw TypeError(t+" is not observable")},e.of=function(){for(var t=arguments.length,r=Array(t),n=0;n<t;n++)r[n]=arguments[n];return new("function"==typeof this?this:e)(function(e){rr(function(){if(!e.closed){for(var t=0;t<r.length;++t)if(e.next(r[t]),e.closed)return;e.complete()}})})},t3(e,null,[{key:t6,get:function(){return this}}]),e}();function rl(e,t,r){var n=[];e.forEach(function(e){return e[t]&&n.push(e)}),n.forEach(function(e){return e[t](r)})}function rc(e){function t(t){Object.defineProperty(e,t,{value:rd})}return j&&Symbol.species&&t(Symbol.species),t("@@species"),e}function rp(e,t){var r,n,i=e.directives;return!i||!i.length||(n=[],(r=i)&&r.length&&r.forEach(function(e){if("skip"===(t=e.name.value)||"include"===t){var t,r=e.arguments,i=e.name.value;k(r&&1===r.length,106,i);var a=r[0];k(a.name&&"if"===a.name.value,107,i);var s=a.value;k(s&&("Variable"===s.kind||"BooleanValue"===s.kind),108,i),n.push({directive:e,ifArgument:a})}}),n).every(function(e){var r=e.directive,n=e.ifArgument,i=!1;return"Variable"===n.value.kind?k(void 0!==(i=t&&t[n.value.name.value]),105,r.name.value):i=n.value.value,"skip"===r.name.value?!i:i})}function rm(e,t,r){var n=new Set(e),i=n.size;return tD(t,{Directive:function(e){if(n.delete(e.name.value)&&(!r||!n.size))return tP}}),r?!n.size:n.size<i}function rh(e){return e&&rm(["client","export"],e,!0)}function rf(e){var t,r,n=null==(t=e.directives)?void 0:t.find(function(e){return"unmask"===e.name.value});if(!n)return"mask";var i=null==(r=n.arguments)?void 0:r.find(function(e){return"mode"===e.name.value});return(!1!==globalThis.__DEV__&&i&&(i.value.kind===d.VARIABLE?!1!==globalThis.__DEV__&&k.warn(109):i.value.kind!==d.STRING?!1!==globalThis.__DEV__&&k.warn(110):"migrate"!==i.value.value&&!1!==globalThis.__DEV__&&k.warn(111,i.value.value)),i&&"value"in i.value&&"migrate"===i.value.value)?"migrate":"unmask"}function rv(e,t,r,n){var i=t.data,a=h(t,["data"]),s=r.data;return er(a,h(r,["data"]))&&function e(t,r,n,i){if(r===n)return!0;var a=new Set;return t.selections.every(function(t){if(a.has(t)||(a.add(t),!rp(t,i.variables)||ry(t)))return!0;if(tQ(t)){var s=tj(t),o=r&&r[s],u=n&&n[s],d=t.selectionSet;if(!d)return er(o,u);var l=Array.isArray(o),c=Array.isArray(u);if(l!==c)return!1;if(l&&c){var p=o.length;if(u.length!==p)return!1;for(var m=0;m<p;++m)if(!e(d,o[m],u[m],i))return!1;return!0}return e(d,o,u,i)}var h=tC(t,i.fragmentMap);if(h)return!!ry(h)||e(h.selectionSet,r,n,i)})}(tJ(e).selectionSet,i,s,{fragmentMap:tO(tY(e)),variables:n})}function ry(e){return!!e.directives&&e.directives.some(rg)}function rg(e){return"nonreactive"===e.name.value}t2()&&Object.defineProperty(rd,Symbol("extensions"),{value:{symbol:t8,hostReportError:rt},configurable:!0}),e.s(["Observable",()=>rd],7656),e.s(["iterateObserversSafely",()=>rl],92360),e.s(["fixObservableSubclass",()=>rc],66852),e.s(["getFragmentMaskMode",()=>rf,"hasClientExports",()=>rh,"hasDirectives",()=>rm,"shouldInclude",()=>rp],86713),e.s(["equalByQuery",()=>rv],40096);var rb=Object.assign,rI=Object.hasOwnProperty,rA=function(e){function t(r){var n=r.queryManager,i=r.queryInfo,a=r.options,s=this,o=t.inactiveOnCreation.getValue();(s=e.call(this,function(e){s._getOrCreateQuery();try{var t=e._subscription._observer;t&&!t.error&&(t.error=rN)}catch(e){}var r=!s.observers.size;s.observers.add(e);var n=s.last;return n&&n.error?e.error&&e.error(n.error):n&&n.result&&e.next&&e.next(s.maskResult(n.result)),r&&s.reobserve().catch(function(){}),function(){s.observers.delete(e)&&!s.observers.size&&s.tearDownQuery()}})||this).observers=new Set,s.subscriptions=new Set,s.dirty=!1,s._getOrCreateQuery=function(){return o&&(n.queries.set(s.queryId,i),o=!1),s.queryManager.getOrCreateQuery(s.queryId)},s.queryInfo=i,s.queryManager=n,s.waitForOwnResult=rT(a.fetchPolicy),s.isTornDown=!1,s.subscribeToMore=s.subscribeToMore.bind(s),s.maskResult=s.maskResult.bind(s);var u=n.defaultOptions.watchQuery,d=(void 0===u?{}:u).fetchPolicy,l=void 0===d?"cache-first":d,c=a.fetchPolicy,p=void 0===c?l:c,h=a.initialFetchPolicy,f=void 0===h?"standby"===p?l:p:h;s.options=m(m({},a),{initialFetchPolicy:f,fetchPolicy:p}),s.queryId=i.queryId||n.generateQueryId();var v=tK(s.query);return s.queryName=v&&v.name&&v.name.value,s}return p(t,e),Object.defineProperty(t.prototype,"query",{get:function(){return this.lastQuery||this.options.query},enumerable:!1,configurable:!0}),Object.defineProperty(t.prototype,"variables",{get:function(){return this.options.variables},enumerable:!1,configurable:!0}),t.prototype.result=function(){var e=this;return!1!==globalThis.__DEV__&&to("observableQuery.result",function(){!1!==globalThis.__DEV__&&k.warn(23)}),new Promise(function(t,r){var n={next:function(r){t(r),e.observers.delete(n),e.observers.size||e.queryManager.removeQuery(e.queryId),setTimeout(function(){i.unsubscribe()},0)},error:r},i=e.subscribe(n)})},t.prototype.resetDiff=function(){this.queryInfo.resetDiff()},t.prototype.getCurrentFullResult=function(e){var t=this;void 0===e&&(e=!0);var r=ta("getLastResult",function(){return t.getLastResult(!0)}),n=this.queryInfo.networkStatus||r&&r.networkStatus||o.ready,i=m(m({},r),{loading:th(n),networkStatus:n}),a=this.options.fetchPolicy,s=void 0===a?"cache-first":a;if(rT(s)||this.queryManager.getDocumentInfo(this.query).hasForcedResolvers);else if(this.waitForOwnResult)this.queryInfo.updateWatch();else{var u=this.queryInfo.getDiff();(u.complete||this.options.returnPartialData)&&(i.data=u.result),er(i.data,{})&&(i.data=void 0),u.complete?(delete i.partial,u.complete&&i.networkStatus===o.loading&&("cache-first"===s||"cache-only"===s)&&(i.networkStatus=o.ready,i.loading=!1)):i.partial=!0,i.networkStatus===o.ready&&(i.error||i.errors)&&(i.networkStatus=o.error),!1===globalThis.__DEV__||u.complete||this.options.partialRefetch||i.loading||i.data||i.error||rE(u.missing)}return e&&this.updateLastResult(i),i},t.prototype.getCurrentResult=function(e){return void 0===e&&(e=!0),this.maskResult(this.getCurrentFullResult(e))},t.prototype.isDifferentFromLastResult=function(e,t){if(!this.last)return!0;var r=this.queryManager.getDocumentInfo(this.query),n=this.queryManager.dataMasking,i=n?r.nonReactiveQuery:this.query;return(n||r.hasNonreactiveDirective?!rv(i,this.last.result,e,this.variables):!er(this.last.result,e))||t&&!er(this.last.variables,t)},t.prototype.getLast=function(e,t){var r=this.last;if(r&&r[e]&&(!t||er(r.variables,this.variables)))return r[e]},t.prototype.getLastResult=function(e){return!1!==globalThis.__DEV__&&to("getLastResult",function(){!1!==globalThis.__DEV__&&k.warn(24)}),this.getLast("result",e)},t.prototype.getLastError=function(e){return!1!==globalThis.__DEV__&&to("getLastError",function(){!1!==globalThis.__DEV__&&k.warn(25)}),this.getLast("error",e)},t.prototype.resetLastResults=function(){!1!==globalThis.__DEV__&&to("resetLastResults",function(){!1!==globalThis.__DEV__&&k.warn(26)}),delete this.last,this.isTornDown=!1},t.prototype.resetQueryStoreErrors=function(){!1!==globalThis.__DEV__&&!1!==globalThis.__DEV__&&k.warn(27),this.queryManager.resetErrors(this.queryId)},t.prototype.refetch=function(e){var t,r={pollInterval:0};if("no-cache"===this.options.fetchPolicy?r.fetchPolicy="no-cache":r.fetchPolicy="network-only",!1!==globalThis.__DEV__&&e&&rI.call(e,"variables")){var n=tX(this.query),i=n.variableDefinitions;i&&i.some(function(e){return"variables"===e.variable.name.value})||!1===globalThis.__DEV__||k.warn(28,e,(null==(t=n.name)?void 0:t.value)||n)}return e&&!er(this.options.variables,e)&&(r.variables=this.options.variables=m(m({},this.options.variables),e)),this.queryInfo.resetLastWrite(),this.reobserve(r,o.refetch)},t.prototype.fetchMore=function(e){var t=this,r=m(m({},e.query?e:m(m(m(m({},this.options),{query:this.options.query}),e),{variables:m(m({},this.options.variables),e.variables)})),{fetchPolicy:"no-cache"});r.query=this.transformDocument(r.query);var n=this.queryManager.generateQueryId();this.lastQuery=e.query?this.transformDocument(this.options.query):r.query;var i=this.queryInfo,a=i.networkStatus;i.networkStatus=o.fetchMore,r.notifyOnNetworkStatusChange&&this.observe();var s=new Set,u=null==e?void 0:e.updateQuery,d="no-cache"!==this.options.fetchPolicy;return d||k(u,29),this.queryManager.fetchQuery(n,r,o.fetchMore).then(function(l){if(t.queryManager.removeQuery(n),i.networkStatus===o.fetchMore&&(i.networkStatus=a),d)t.queryManager.cache.batch({update:function(n){var i=e.updateQuery;i?n.updateQuery({query:t.query,variables:t.variables,returnPartialData:!0,optimistic:!1},function(e){return i(e,{fetchMoreResult:l.data,variables:r.variables})}):n.writeQuery({query:r.query,variables:r.variables,data:l.data})},onWatchUpdated:function(e){s.add(e.query)}});else{var c=t.getLast("result"),p=u(c.data,{fetchMoreResult:l.data,variables:r.variables});t.reportResult(m(m({},c),{networkStatus:a,loading:th(a),data:p}),t.variables)}return t.maskResult(l)}).finally(function(){d&&!s.has(t.query)&&t.reobserveCacheFirst()})},t.prototype.subscribeToMore=function(e){var t=this,r=this.queryManager.startGraphQLSubscription({query:e.document,variables:e.variables,context:e.context}).subscribe({next:function(r){var n=e.updateQuery;n&&t.updateQuery(function(e,t){return n(e,m({subscriptionData:r},t))})},error:function(t){e.onError?e.onError(t):!1!==globalThis.__DEV__&&k.error(30,t)}});return this.subscriptions.add(r),function(){t.subscriptions.delete(r)&&r.unsubscribe()}},t.prototype.setOptions=function(e){return!1!==globalThis.__DEV__&&(ts(e,"canonizeResults","setOptions"),to("setOptions",function(){!1!==globalThis.__DEV__&&k.warn(31)})),this.reobserve(e)},t.prototype.silentSetOptions=function(e){var t=X(this.options,e||{});rb(this.options,t)},t.prototype.setVariables=function(e){var t=this;return er(this.variables,e)?this.observers.size?ta("observableQuery.result",function(){return t.result()}):Promise.resolve():(this.options.variables=e,this.observers.size)?this.reobserve({fetchPolicy:this.options.initialFetchPolicy,variables:e},o.setVariables):Promise.resolve()},t.prototype.updateQuery=function(e){var t=this.queryManager,r=t.cache.diff({query:this.options.query,variables:this.variables,returnPartialData:!0,optimistic:!1}),n=r.result,i=r.complete,a=e(n,{variables:this.variables,complete:!!i,previousData:n});a&&(t.cache.writeQuery({query:this.options.query,data:a,variables:this.variables}),t.broadcastQueries())},t.prototype.startPolling=function(e){this.options.pollInterval=e,this.updatePolling()},t.prototype.stopPolling=function(){this.options.pollInterval=0,this.updatePolling()},t.prototype.applyNextFetchPolicy=function(e,t){if(t.nextFetchPolicy){var r=t.fetchPolicy,n=void 0===r?"cache-first":r,i=t.initialFetchPolicy,a=void 0===i?n:i;"standby"===n||("function"==typeof t.nextFetchPolicy?t.fetchPolicy=t.nextFetchPolicy(n,{reason:e,options:t,observable:this,initialFetchPolicy:a}):"variables-changed"===e?t.fetchPolicy=a:t.fetchPolicy=t.nextFetchPolicy)}return t.fetchPolicy},t.prototype.fetch=function(e,t,r){var n=this._getOrCreateQuery();return n.setObservableQuery(this),this.queryManager.fetchConcastWithInfo(n,e,t,r)},t.prototype.updatePolling=function(){var e=this;if(!this.queryManager.ssrMode){var t=this.pollingInfo,r=this.options.pollInterval;if(!r||!this.hasObservers()){t&&(clearTimeout(t.timeout),delete this.pollingInfo);return}if(!t||t.interval!==r){k(r,32),(t||(this.pollingInfo={})).interval=r;var n=function(){var t,r;e.pollingInfo&&(th(e.queryInfo.networkStatus)||(null==(r=(t=e.options).skipPollAttempt)?void 0:r.call(t))?i():e.reobserve({fetchPolicy:"no-cache"===e.options.initialFetchPolicy?"no-cache":"network-only"},o.poll).then(i,i))},i=function(){var t=e.pollingInfo;t&&(clearTimeout(t.timeout),t.timeout=setTimeout(n,t.interval))};i()}}},t.prototype.updateLastResult=function(e,t){var r=this;void 0===t&&(t=this.variables);var n=ta("getLastError",function(){return r.getLastError()});return n&&this.last&&!er(t,this.last.variables)&&(n=void 0),this.last=m({result:this.queryManager.assumeImmutableResults?e:ty(e),variables:t},n?{error:n}:null)},t.prototype.reobserveAsConcast=function(e,t){var r=this;this.isTornDown=!1;var n=t===o.refetch||t===o.fetchMore||t===o.poll,i=this.options.variables,a=this.options.fetchPolicy,s=X(this.options,e||{}),u=n?s:rb(this.options,s),d=this.transformDocument(u.query);this.lastQuery=d,!n&&(this.updatePolling(),e&&e.variables&&!er(e.variables,i)&&"standby"!==u.fetchPolicy&&(u.fetchPolicy===a||"function"==typeof u.nextFetchPolicy)&&(this.applyNextFetchPolicy("variables-changed",u),void 0===t&&(t=o.setVariables))),this.waitForOwnResult&&(this.waitForOwnResult=rT(u.fetchPolicy));var l=function(){r.concast===h&&(r.waitForOwnResult=!1)},c=u.variables&&m({},u.variables),p=this.fetch(u,t,d),h=p.concast,f=p.fromLink,v={next:function(e){er(r.variables,c)&&(l(),r.reportResult(e,c))},error:function(e){er(r.variables,c)&&(tc(e)||(e=new tm({networkError:e})),l(),r.reportError(e,c))}};return n||!f&&this.concast||(this.concast&&this.observer&&this.concast.removeObserver(this.observer),this.concast=h,this.observer=v),h.addObserver(v),h},t.prototype.reobserve=function(e,t){var r;return(r=this.reobserveAsConcast(e,t).promise.then(this.maskResult)).catch(function(){}),r},t.prototype.resubscribeAfterError=function(){for(var e=this,t=[],r=0;r<arguments.length;r++)t[r]=arguments[r];var n=this.last;ta("resetLastResults",function(){return e.resetLastResults()});var i=this.subscribe.apply(this,t);return this.last=n,i},t.prototype.observe=function(){this.reportResult(this.getCurrentFullResult(!1),this.variables)},t.prototype.reportResult=function(e,t){var r=this,n=ta("getLastError",function(){return r.getLastError()}),i=this.isDifferentFromLastResult(e,t);(n||!e.partial||this.options.returnPartialData)&&this.updateLastResult(e,t),(n||i)&&rl(this.observers,"next",this.maskResult(e))},t.prototype.reportError=function(e,t){var r=this,n=m(m({},ta("getLastResult",function(){return r.getLastResult()})),{error:e,errors:e.graphQLErrors,networkStatus:o.error,loading:!1});this.updateLastResult(n,t),rl(this.observers,"error",this.last.error=e)},t.prototype.hasObservers=function(){return this.observers.size>0},t.prototype.tearDownQuery=function(){this.isTornDown||(this.concast&&this.observer&&(this.concast.removeObserver(this.observer),delete this.concast,delete this.observer),this.stopPolling(),this.subscriptions.forEach(function(e){return e.unsubscribe()}),this.subscriptions.clear(),this.queryManager.stopQuery(this.queryId),this.observers.clear(),this.isTornDown=!0)},t.prototype.transformDocument=function(e){return this.queryManager.transform(e)},t.prototype.maskResult=function(e){return e&&"data"in e?m(m({},e),{data:this.queryManager.maskOperation({document:this.query,data:e.data,fetchPolicy:this.options.fetchPolicy,id:this.queryId})}):e},t.prototype.resetNotifications=function(){this.cancelNotifyTimeout(),this.dirty=!1},t.prototype.cancelNotifyTimeout=function(){this.notifyTimeout&&(clearTimeout(this.notifyTimeout),this.notifyTimeout=void 0)},t.prototype.scheduleNotify=function(){var e=this;!this.dirty&&(this.dirty=!0,this.notifyTimeout||(this.notifyTimeout=setTimeout(function(){return e.notify()},0)))},t.prototype.notify=function(){this.cancelNotifyTimeout(),this.dirty&&("cache-only"==this.options.fetchPolicy||"cache-and-network"==this.options.fetchPolicy||!th(this.queryInfo.networkStatus))&&(this.queryInfo.getDiff().fromOptimisticTransaction?this.observe():this.reobserveCacheFirst()),this.dirty=!1},t.prototype.reobserveCacheFirst=function(){var e=this.options,t=e.fetchPolicy,r=e.nextFetchPolicy;return"cache-and-network"===t||"network-only"===t?this.reobserve({fetchPolicy:"cache-first",nextFetchPolicy:function(e,n){return(this.nextFetchPolicy=r,"function"==typeof this.nextFetchPolicy)?this.nextFetchPolicy(e,n):t}}):this.reobserve()},t.inactiveOnCreation=new eU,t}(rd);function rN(e){!1!==globalThis.__DEV__&&k.error(33,e.message,e.stack)}function rE(e){!1!==globalThis.__DEV__&&e&&!1!==globalThis.__DEV__&&k.debug(34,e)}function rT(e){return"network-only"===e||"no-cache"===e||"standby"===e}rc(rA),e.s(["ObservableQuery",()=>rA,"logMissingFieldErrors",()=>rE],79506);var rP=Array.isArray;function rD(e){return Array.isArray(e)&&e.length>0}function r_(e){var t;return!1!==globalThis.__DEV__&&(t=new Set([e])).forEach(function(e){tu(e)&&function(e){if(!1!==globalThis.__DEV__&&!Object.isFrozen(e))try{Object.freeze(e)}catch(e){if(e instanceof TypeError)return null;throw e}return e}(e)===e&&Object.getOwnPropertyNames(e).forEach(function(r){tu(e[r])&&t.add(e[r])})}),e}function rO(e){return 9===e||32===e}function rC(e){return e>=48&&e<=57}function rR(e){return e>=97&&e<=122||e>=65&&e<=90}function rS(e){return rR(e)||95===e}function rw(e){return rR(e)||rC(e)||95===e}function rk(e){var t,r;let n=Number.MAX_SAFE_INTEGER,i=null,a=-1;for(let t=0;t<e.length;++t){let s=e[t],o=function(e){let t=0;for(;t<e.length&&rO(e.charCodeAt(t));)++t;return t}(s);o!==s.length&&(i=null!=(r=i)?r:t,a=t,0!==t&&o<n&&(n=o))}return e.map((e,t)=>0===t?e:e.slice(n)).slice(null!=(t=i)?t:0,a+1)}function rL(e,t){let r=e.replace(/"""/g,'\\"""'),n=r.split(/\r\n|[\n\r]/g),i=1===n.length,a=n.length>1&&n.slice(1).every(e=>0===e.length||rO(e.charCodeAt(0))),s=r.endsWith('\\"""'),o=e.endsWith('"')&&!s,u=e.endsWith("\\"),d=o||u,l=!(null!=t&&t.minimize)&&(!i||e.length>70||d||a||s),c="",p=i&&rO(e.charCodeAt(0));return(l&&!p||a)&&(c+="\n"),c+=r,(l||d)&&(c+="\n"),'"""'+c+'"""'}e.s(["isArray",()=>rP,"isNonEmptyArray",()=>rD],45938),e.s(["maybeDeepFreeze",()=>r_],76846),e.s(["isDigit",()=>rC,"isNameContinue",()=>rw,"isNameStart",()=>rS,"isWhiteSpace",()=>rO],54335),e.s(["dedentBlockStringLines",()=>rk,"printBlockString",()=>rL],99074)},4306,14089,38413,17383,82934,34618,82534,18540,60984,93264,97756,17235,7284,8345,95574,63489,88005,35595,20495,41209,45610,98264,23899,e=>{"use strict";e.i(71258);var t,r,n,i,a,s,o,u,d,l,c,p,m,h,f,v,y,g,b,I,A,N,E,T,P,D,_,O,C,R,S=e.i(87081),w=e.i(79873),k=e.i(18247);function L(e){var t=w.useContext((0,k.getApolloContext)()),r=e||t.client;return(0,S.invariant)(!!r,78),r}var F=e.i(90571),M=e.i(6195),$=e.i(69981),x=e.i(34049),V=e.i(72243),U=e.i(20871),q=e.i(630);function B(e){var t;switch(e){case D.Query:t="Query";break;case D.Mutation:t="Mutation";break;case D.Subscription:t="Subscription"}return t}function j(e){(0,q.warnDeprecated)("parser",function(){!1!==globalThis.__DEV__&&S.invariant.warn(93)}),_||(_=new x.AutoCleanedWeakCache(V.cacheSizes.parser||1e3));var t,r,n=_.get(e);if(n)return n;(0,S.invariant)(!!e&&!!e.kind,94,e);for(var i=[],a=[],s=[],o=[],u=0,d=e.definitions;u<d.length;u++){var l=d[u];if("FragmentDefinition"===l.kind){i.push(l);continue}if("OperationDefinition"===l.kind)switch(l.operation){case"query":a.push(l);break;case"mutation":s.push(l);break;case"subscription":o.push(l)}}(0,S.invariant)(!i.length||a.length||s.length||o.length,95),(0,S.invariant)(a.length+s.length+o.length<=1,96,e,a.length,o.length,s.length),r=a.length?D.Query:D.Mutation,a.length||s.length||(r=D.Subscription);var c=a.length?a:s.length?s:o;(0,S.invariant)(1===c.length,97,e,c.length);var p=c[0];t=p.variableDefinitions||[];var m={name:p.name&&"Name"===p.name.kind?p.name.value:"data",type:r,variables:t};return _.set(e,m),m}function Q(e,t){var r=(0,q.muteDeprecations)("parser",j,[e]),n=B(t),i=B(r.type);(0,S.invariant)(r.type===t,98,n,n,i)}(t=D||(D={}))[t.Query=0]="Query",t[t.Mutation=1]="Mutation",t[t.Subscription=2]="Subscription",j.resetCache=function(){_=void 0},!1!==globalThis.__DEV__&&(0,U.registerGlobalCache)("parser",function(){return _?_.size:0});var z=e.i(21110),G=e.i(75625),K=G.canUseDOM?w.useLayoutEffect:w.useEffect;function H(e,t,r,n){void 0===n&&(n="Please remove this option.");var i=w.useRef(!1);!1!==globalThis.__DEV__&&t in e&&!i.current&&((0,q.warnRemovedOption)(e,t,r,n),i.current=!0)}function Y(e,t){!1!==globalThis.__DEV__&&H(t||{},"ignoreResults","useMutation","If you don't want to synchronize component state with the mutation, please use the `useApolloClient` hook to get the client instance and call `client.mutate` directly.");var r=L(null==t?void 0:t.client);Q(e,D.Mutation);var n=w.useState({called:!1,loading:!1,client:r}),i=n[0],a=n[1],s=w.useRef({result:i,mutationId:0,isMounted:!0,client:r,mutation:e,options:t});K(function(){Object.assign(s.current,{client:r,options:t,mutation:e})});var o=w.useCallback(function(e){void 0===e&&(e={});var t=s.current,r=t.options,n=t.mutation,i=(0,F.__assign)((0,F.__assign)({},r),{mutation:n}),o=e.client||s.current.client;s.current.result.loading||i.ignoreResults||!s.current.isMounted||a(s.current.result={loading:!0,error:void 0,data:void 0,called:!0,client:o});var u=++s.current.mutationId,d=(0,M.mergeOptions)(i,e);return o.mutate(d).then(function(t){var r,n,i=t.data,l=t.errors,c=l&&l.length>0?new z.ApolloError({graphQLErrors:l}):void 0,p=e.onError||(null==(r=s.current.options)?void 0:r.onError);if(c&&p&&p(c,d),u===s.current.mutationId&&!d.ignoreResults){var m={called:!0,loading:!1,data:i,error:c,client:o};s.current.isMounted&&!(0,$.equal)(s.current.result,m)&&a(s.current.result=m)}var h=e.onCompleted||(null==(n=s.current.options)?void 0:n.onCompleted);return c||null==h||h(t.data,d),t},function(t){if(u===s.current.mutationId&&s.current.isMounted){var r,n={loading:!1,error:t,data:void 0,called:!0,client:o};(0,$.equal)(s.current.result,n)||a(s.current.result=n)}var i=e.onError||(null==(r=s.current.options)?void 0:r.onError);if(i)return i(t,d),{data:void 0,errors:t};throw t})},[]),u=w.useCallback(function(){if(s.current.isMounted){var e={called:!1,loading:!1,client:s.current.client};Object.assign(s.current,{mutationId:0,result:e}),a(e)}},[]);return w.useEffect(function(){var e=s.current;return e.isMounted=!0,function(){e.isMounted=!1}},[]),[o,(0,F.__assign)({reset:u},i)]}e.s(["useMutation",()=>Y],14089);var X=!1,W=w.useSyncExternalStore||function(e,t,r){var n=t();!1===globalThis.__DEV__||X||n===t()||(X=!0,!1!==globalThis.__DEV__&&S.invariant.error(91));var i=w.useState({inst:{value:n,getSnapshot:t}}),a=i[0].inst,s=i[1];return G.canUseLayoutEffect?w.useLayoutEffect(function(){Object.assign(a,{value:n,getSnapshot:t}),J(a)&&s({inst:a})},[e,n,t]):Object.assign(a,{value:n,getSnapshot:t}),w.useEffect(function(){return J(a)&&s({inst:a}),e(function(){J(a)&&s({inst:a})})},[e]),n};function J(e){var t=e.value,r=e.getSnapshot;try{return t!==r()}catch(e){return!0}}var Z=e.i(79506),ee=e.i(7799),et=e.i(36433),er=e.i(45938),en=e.i(76846),ei=Symbol.for("apollo.hook.wrappers"),ea=Object.prototype.hasOwnProperty;function es(){}var eo=Symbol();function eu(e,t){var r,n,i,a;return void 0===t&&(t=Object.create(null)),(r=ed,(a=(i=(n=L(t&&t.client).queryManager)&&n[ei])&&i.useQuery)?a(r):r)(e,t)}function ed(e,t){!1!==globalThis.__DEV__&&(H(t,"canonizeResults","useQuery"),H(t,"partialRefetch","useQuery"),H(t,"defaultOptions","useQuery","Pass the options directly to the hook instead."),H(t,"onCompleted","useQuery","If your `onCompleted` callback sets local state, switch to use derived state using `data` returned from the hook instead. Use `useEffect` to perform side-effects as a result of updates to `data`."),H(t,"onError","useQuery","If your `onError` callback sets local state, switch to use derived state using `data`, `error` or `errors` returned from the hook instead. Use `useEffect` if you need to perform side-effects as a result of updates to `data`, `error` or `errors`."));var r=el(e,t),n=r.result,i=r.obsQueryFields;return w.useMemo(function(){return(0,F.__assign)((0,F.__assign)({},n),i)},[n,i])}function el(e,t){var r,n,i,a,s,o,u,d,l,c,p,m,h,f,v,y,g,b,I,A,N,E,T=L(t.client),P=w.useContext((0,k.getApolloContext)()).renderPromises,_=!!P,O=T.disableNetworkFetches,C=!1!==t.ssr&&!t.skip,R=t.partialRefetch,M=ec(T,e,t,_),x=function(e,t,r,n,i){function a(a){var s;return Q(t,D.Query),{client:e,query:t,observable:n&&n.getSSRObservable(i())||Z.ObservableQuery.inactiveOnCreation.withValue(!n,function(){return(0,q.muteDeprecations)(["canonizeResults","partialRefetch"],function(){return e.watchQuery(ep(void 0,e,r,i()))})}),resultData:{previousData:null==(s=null==a?void 0:a.resultData.current)?void 0:s.data}}}var s=w.useState(a),o=s[0],u=s[1];function d(e){Object.assign(o.observable,((t={})[eo]=e,t));var t,r,n=o.resultData;u((0,F.__assign)((0,F.__assign)({},o),{query:e.query,resultData:Object.assign(n,{previousData:(null==(r=n.current)?void 0:r.data)||n.previousData,current:void 0})}))}if(e!==o.client||t!==o.query){var l=a(o);return u(l),[l,d]}return[o,d]}(T,e,t,P,M),V=x[0],U=V.observable,B=V.resultData,j=x[1],z=M(U);r=B,n=U,i=T,a=t,s=z,n[eo]&&!(0,$.equal)(n[eo],s)&&(n.reobserve(ep(n,i,a,s)),r.previousData=(null==(o=r.current)?void 0:o.data)||r.previousData,r.current=void 0),n[eo]=s;var G=w.useMemo(function(){var e;return{refetch:(e=U).refetch.bind(e),reobserve:function(){for(var t=[],r=0;r<arguments.length;r++)t[r]=arguments[r];return!1!==globalThis.__DEV__&&!1!==globalThis.__DEV__&&S.invariant.warn(83),e.reobserve.apply(e,t)},fetchMore:e.fetchMore.bind(e),updateQuery:e.updateQuery.bind(e),startPolling:e.startPolling.bind(e),stopPolling:e.stopPolling.bind(e),subscribeToMore:e.subscribeToMore.bind(e)}},[U]);return u=U,d=P,l=C,d&&l&&(d.registerSSRObservable(u),u.getCurrentResult().loading&&d.addObservableQueryPromise(u)),{result:(c=B,p=U,m=T,h=t,f=z,v=O,y=R,g=_,b={onCompleted:t.onCompleted||es,onError:t.onError||es},I=w.useRef(b),w.useEffect(function(){I.current=b}),A=(g||v)&&!1===h.ssr&&!h.skip?ey:h.skip||"standby"===f.fetchPolicy?eg:void 0,N=c.previousData,E=w.useMemo(function(){return A&&ev(A,N,p,m)},[m,p,A,N]),W(w.useCallback(function(e){if(g)return function(){};var t=function(){var t=c.current,r=p.getCurrentResult();t&&t.loading===r.loading&&t.networkStatus===r.networkStatus&&(0,$.equal)(t.data,r.data)||em(r,c,p,m,y,e,I.current)},r=function(i){if(n.current.unsubscribe(),n.current=p.resubscribeAfterError(t,r),!ea.call(i,"graphQLErrors"))throw i;var a=c.current;(!a||a&&a.loading||!(0,$.equal)(i,a.error))&&em({data:a&&a.data,error:i,loading:!1,networkStatus:ee.NetworkStatus.error},c,p,m,y,e,I.current)},n={current:p.subscribe(t,r)};return function(){setTimeout(function(){return n.current.unsubscribe()})}},[v,g,p,c,y,m]),function(){return E||eh(c,p,I.current,y,m)},function(){return E||eh(c,p,I.current,y,m)})),obsQueryFields:G,observable:U,resultData:B,client:T,onQueryExecuted:j}}function ec(e,t,r,n){void 0===r&&(r={});var i=r.skip,a=(r.ssr,r.onCompleted,r.onError,r.defaultOptions),s=(0,F.__rest)(r,["skip","ssr","onCompleted","onError","defaultOptions"]);return function(r){var o=Object.assign(s,{query:t});return n&&("network-only"===o.fetchPolicy||"cache-and-network"===o.fetchPolicy)&&(o.fetchPolicy="cache-first"),o.variables||(o.variables={}),i?(o.initialFetchPolicy=o.initialFetchPolicy||o.fetchPolicy||ef(a,e.defaultOptions),o.fetchPolicy="standby"):o.fetchPolicy||(o.fetchPolicy=(null==r?void 0:r.options.initialFetchPolicy)||ef(a,e.defaultOptions)),o}}function ep(e,t,r,n){var i=[],a=t.defaultOptions.watchQuery;return a&&i.push(a),r.defaultOptions&&i.push(r.defaultOptions),i.push((0,et.compact)(e&&e.options,n)),i.reduce(M.mergeOptions)}function em(e,t,r,n,i,a,s){var o,u,d,l=t.current;l&&l.data&&(t.previousData=l.data),!e.error&&(0,er.isNonEmptyArray)(e.errors)&&(e.error=new z.ApolloError({graphQLErrors:e.errors})),t.current=ev((o=e,u=r,d=i,o.partial&&d&&!o.loading&&(!o.data||0===Object.keys(o.data).length)&&"cache-only"!==u.options.fetchPolicy?(u.refetch(),(0,F.__assign)((0,F.__assign)({},o),{loading:!0,networkStatus:ee.NetworkStatus.refetch})):o),t.previousData,r,n),a(),function(e,t,r){if(!e.loading){var n,i=(n=e,(0,er.isNonEmptyArray)(n.errors)?new z.ApolloError({graphQLErrors:n.errors}):n.error);Promise.resolve().then(function(){i?r.onError(i):e.data&&t!==e.networkStatus&&e.networkStatus===ee.NetworkStatus.ready&&r.onCompleted(e.data)}).catch(function(e){!1!==globalThis.__DEV__&&S.invariant.warn(e)})}}(e,null==l?void 0:l.networkStatus,s)}function eh(e,t,r,n,i){return e.current||em(t.getCurrentResult(),e,t,i,n,function(){},r),e.current}function ef(e,t){var r;return(null==e?void 0:e.fetchPolicy)||(null==(r=null==t?void 0:t.watchQuery)?void 0:r.fetchPolicy)||"cache-first"}function ev(e,t,r,n){var i=e.data,a=(e.partial,(0,F.__rest)(e,["data","partial"]));return(0,F.__assign)((0,F.__assign)({data:i},a),{client:n,observable:r,variables:r.variables,called:e!==ey&&e!==eg,previousData:t})}var ey=(0,en.maybeDeepFreeze)({loading:!0,data:void 0,error:void 0,networkStatus:ee.NetworkStatus.loading}),eg=(0,en.maybeDeepFreeze)({loading:!1,data:void 0,error:void 0,networkStatus:ee.NetworkStatus.ready});function eb(e){if(!e)return"";if(e instanceof z.ApolloError){if(e.networkError){let t=e.networkError;return t.message?t.message:t.statusCode?`Network error (${t.statusCode})`:"Network connection failed"}if(e.graphQLErrors&&e.graphQLErrors.length>0)return e.graphQLErrors[0].message;if(e.message)return e.message}return e instanceof Error?e.message:"string"==typeof e?e:"object"==typeof e&&"message"in e&&"string"==typeof e.message?e.message:"An unexpected error occurred"}e.s(["createMakeWatchQueryOptions",()=>ec,"getDefaultFetchPolicy",()=>ef,"getObsQueryOptions",()=>ep,"toQueryResult",()=>ev,"useQuery",()=>eu,"useQueryInternals",()=>el],38413);let eI=/\r\n|[\n\r]/g;function eA(e,t){let r=0,n=1;for(let i of e.body.matchAll(eI)){if("number"==typeof i.index||function(e,t){if(!e)throw Error("Unexpected invariant triggered.")}(!1),i.index>=t)break;r=i.index+i[0].length,n+=1}return{line:n,column:t+1-r}}function eN(e,t){let r=e.locationOffset.column-1,n="".padStart(r)+e.body,i=t.line-1,a=e.locationOffset.line-1,s=t.line+a,o=1===t.line?r:0,u=t.column+o,d=`${e.name}:${s}:${u}
`,l=n.split(/\r\n|[\n\r]/g),c=l[i];if(c.length>120){let e=Math.floor(u/80),t=[];for(let e=0;e<c.length;e+=80)t.push(c.slice(e,e+80));return d+eE([[`${s} |`,t[0]],...t.slice(1,e+1).map(e=>["|",e]),["|","^".padStart(u%80)],["|",t[e+1]]])}return d+eE([[`${s-1} |`,l[i-1]],[`${s} |`,c],["|","^".padStart(u)],[`${s+1} |`,l[i+1]]])}function eE(e){let t=e.filter(([e,t])=>void 0!==t),r=Math.max(...t.map(([e])=>e.length));return t.map(([e,t])=>e.padStart(r)+(t?" "+t:"")).join("\n")}class eT extends Error{constructor(e,...t){var r,n,i;const{nodes:a,source:s,positions:o,path:u,originalError:d,extensions:l}=function(e){let t=e[0];return null==t||"kind"in t||"length"in t?{nodes:t,source:e[1],positions:e[2],path:e[3],originalError:e[4],extensions:e[5]}:t}(t);super(e),this.name="GraphQLError",this.path=null!=u?u:void 0,this.originalError=null!=d?d:void 0,this.nodes=eP(Array.isArray(a)?a:a?[a]:void 0);const c=eP(null==(r=this.nodes)?void 0:r.map(e=>e.loc).filter(e=>null!=e));this.source=null!=s?s:null==c||null==(n=c[0])?void 0:n.source,this.positions=null!=o?o:null==c?void 0:c.map(e=>e.start),this.locations=o&&s?o.map(e=>eA(s,e)):null==c?void 0:c.map(e=>eA(e.source,e.start));const p=!function(e){return"object"==typeof e&&null!==e}(null==d?void 0:d.extensions)||null==d?void 0:d.extensions;this.extensions=null!=(i=null!=l?l:p)?i:Object.create(null),Object.defineProperties(this,{message:{writable:!0,enumerable:!0},name:{enumerable:!1},nodes:{enumerable:!1},source:{enumerable:!1},positions:{enumerable:!1},originalError:{enumerable:!1}}),null!=d&&d.stack?Object.defineProperty(this,"stack",{value:d.stack,writable:!0,configurable:!0}):Error.captureStackTrace?Error.captureStackTrace(this,eT):Object.defineProperty(this,"stack",{value:Error().stack,writable:!0,configurable:!0})}get[Symbol.toStringTag](){return"GraphQLError"}toString(){let e=this.message;if(this.nodes)for(let r of this.nodes){var t;r.loc&&(e+="\n\n"+eN((t=r.loc).source,eA(t.source,t.start)))}else if(this.source&&this.locations)for(let t of this.locations)e+="\n\n"+eN(this.source,t);return e}toJSON(){let e={message:this.message};return null!=this.locations&&(e.locations=this.locations),null!=this.path&&(e.path=this.path),null!=this.extensions&&Object.keys(this.extensions).length>0&&(e.extensions=this.extensions),e}}function eP(e){return void 0===e||0===e.length?void 0:e}function eD(e,t,r){return new eT(`Syntax Error: ${r}`,{source:e,positions:[t]})}var e_=e.i(20064);(r=O||(O={})).QUERY="QUERY",r.MUTATION="MUTATION",r.SUBSCRIPTION="SUBSCRIPTION",r.FIELD="FIELD",r.FRAGMENT_DEFINITION="FRAGMENT_DEFINITION",r.FRAGMENT_SPREAD="FRAGMENT_SPREAD",r.INLINE_FRAGMENT="INLINE_FRAGMENT",r.VARIABLE_DEFINITION="VARIABLE_DEFINITION",r.SCHEMA="SCHEMA",r.SCALAR="SCALAR",r.OBJECT="OBJECT",r.FIELD_DEFINITION="FIELD_DEFINITION",r.ARGUMENT_DEFINITION="ARGUMENT_DEFINITION",r.INTERFACE="INTERFACE",r.UNION="UNION",r.ENUM="ENUM",r.ENUM_VALUE="ENUM_VALUE",r.INPUT_OBJECT="INPUT_OBJECT",r.INPUT_FIELD_DEFINITION="INPUT_FIELD_DEFINITION",r.DIRECTIVE_DEFINITION="DIRECTIVE_DEFINITION";var eO=e.i(85809),eC=e.i(99074),eR=e.i(54335);(n=C||(C={})).SOF="<SOF>",n.EOF="<EOF>",n.BANG="!",n.DOLLAR="$",n.AMP="&",n.PAREN_L="(",n.PAREN_R=")",n.DOT=".",n.SPREAD="...",n.COLON=":",n.EQUALS="=",n.AT="@",n.BRACKET_L="[",n.BRACKET_R="]",n.BRACE_L="{",n.PIPE="|",n.BRACE_R="}",n.NAME="Name",n.INT="Int",n.FLOAT="Float",n.STRING="String",n.BLOCK_STRING="BlockString",n.COMMENT="Comment";class eS{constructor(e){const t=new e_.Token(C.SOF,0,0,0,0);this.source=e,this.lastToken=t,this.token=t,this.line=1,this.lineStart=0}get[Symbol.toStringTag](){return"Lexer"}advance(){return this.lastToken=this.token,this.token=this.lookahead()}lookahead(){let e=this.token;if(e.kind!==C.EOF)do if(e.next)e=e.next;else{let t=function(e,t){let r=e.source.body,n=r.length,i=t;for(;i<n;){let t=r.charCodeAt(i);switch(t){case 65279:case 9:case 32:case 44:++i;continue;case 10:++i,++e.line,e.lineStart=i;continue;case 13:10===r.charCodeAt(i+1)?i+=2:++i,++e.line,e.lineStart=i;continue;case 35:return function(e,t){let r=e.source.body,n=r.length,i=t+1;for(;i<n;){let e=r.charCodeAt(i);if(10===e||13===e)break;if(ew(e))++i;else if(ek(r,i))i+=2;else break}return e$(e,C.COMMENT,t,i,r.slice(t+1,i))}(e,i);case 33:return e$(e,C.BANG,i,i+1);case 36:return e$(e,C.DOLLAR,i,i+1);case 38:return e$(e,C.AMP,i,i+1);case 40:return e$(e,C.PAREN_L,i,i+1);case 41:return e$(e,C.PAREN_R,i,i+1);case 46:if(46===r.charCodeAt(i+1)&&46===r.charCodeAt(i+2))return e$(e,C.SPREAD,i,i+3);break;case 58:return e$(e,C.COLON,i,i+1);case 61:return e$(e,C.EQUALS,i,i+1);case 64:return e$(e,C.AT,i,i+1);case 91:return e$(e,C.BRACKET_L,i,i+1);case 93:return e$(e,C.BRACKET_R,i,i+1);case 123:return e$(e,C.BRACE_L,i,i+1);case 124:return e$(e,C.PIPE,i,i+1);case 125:return e$(e,C.BRACE_R,i,i+1);case 34:if(34===r.charCodeAt(i+1)&&34===r.charCodeAt(i+2))return function(e,t){let r=e.source.body,n=r.length,i=e.lineStart,a=t+3,s=a,o="",u=[];for(;a<n;){let n=r.charCodeAt(a);if(34===n&&34===r.charCodeAt(a+1)&&34===r.charCodeAt(a+2)){o+=r.slice(s,a),u.push(o);let n=e$(e,C.BLOCK_STRING,t,a+3,(0,eC.dedentBlockStringLines)(u).join("\n"));return e.line+=u.length-1,e.lineStart=i,n}if(92===n&&34===r.charCodeAt(a+1)&&34===r.charCodeAt(a+2)&&34===r.charCodeAt(a+3)){o+=r.slice(s,a),s=a+1,a+=4;continue}if(10===n||13===n){o+=r.slice(s,a),u.push(o),13===n&&10===r.charCodeAt(a+1)?a+=2:++a,o="",s=a,i=a;continue}if(ew(n))++a;else if(ek(r,a))a+=2;else throw eD(e.source,a,`Invalid character within String: ${eM(e,a)}.`)}throw eD(e.source,a,"Unterminated string.")}(e,i);return function(e,t){let r=e.source.body,n=r.length,i=t+1,a=i,s="";for(;i<n;){let n=r.charCodeAt(i);if(34===n)return s+=r.slice(a,i),e$(e,C.STRING,t,i+1,s);if(92===n){s+=r.slice(a,i);let t=117===r.charCodeAt(i+1)?123===r.charCodeAt(i+2)?function(e,t){let r=e.source.body,n=0,i=3;for(;i<12;){let e=r.charCodeAt(t+i++);if(125===e){if(i<5||!ew(n))break;return{value:String.fromCodePoint(n),size:i}}if((n=n<<4|eU(e))<0)break}throw eD(e.source,t,`Invalid Unicode escape sequence: "${r.slice(t,t+i)}".`)}(e,i):function(e,t){let r=e.source.body,n=eV(r,t+2);if(ew(n))return{value:String.fromCodePoint(n),size:6};if(eL(n)&&92===r.charCodeAt(t+6)&&117===r.charCodeAt(t+7)){let e=eV(r,t+8);if(eF(e))return{value:String.fromCodePoint(n,e),size:12}}throw eD(e.source,t,`Invalid Unicode escape sequence: "${r.slice(t,t+6)}".`)}(e,i):function(e,t){let r=e.source.body;switch(r.charCodeAt(t+1)){case 34:return{value:'"',size:2};case 92:return{value:"\\",size:2};case 47:return{value:"/",size:2};case 98:return{value:"\b",size:2};case 102:return{value:"\f",size:2};case 110:return{value:"\n",size:2};case 114:return{value:"\r",size:2};case 116:return{value:"	",size:2}}throw eD(e.source,t,`Invalid character escape sequence: "${r.slice(t,t+2)}".`)}(e,i);s+=t.value,i+=t.size,a=i;continue}if(10===n||13===n)break;if(ew(n))++i;else if(ek(r,i))i+=2;else throw eD(e.source,i,`Invalid character within String: ${eM(e,i)}.`)}throw eD(e.source,i,"Unterminated string.")}(e,i)}if((0,eR.isDigit)(t)||45===t)return function(e,t,r){let n=e.source.body,i=t,a=r,s=!1;if(45===a&&(a=n.charCodeAt(++i)),48===a){if(a=n.charCodeAt(++i),(0,eR.isDigit)(a))throw eD(e.source,i,`Invalid number, unexpected digit after 0: ${eM(e,i)}.`)}else i=ex(e,i,a),a=n.charCodeAt(i);if(46===a&&(s=!0,a=n.charCodeAt(++i),i=ex(e,i,a),a=n.charCodeAt(i)),(69===a||101===a)&&(s=!0,(43===(a=n.charCodeAt(++i))||45===a)&&(a=n.charCodeAt(++i)),i=ex(e,i,a),a=n.charCodeAt(i)),46===a||(0,eR.isNameStart)(a))throw eD(e.source,i,`Invalid number, expected digit but got: ${eM(e,i)}.`);return e$(e,s?C.FLOAT:C.INT,t,i,n.slice(t,i))}(e,i,t);if((0,eR.isNameStart)(t))return function(e,t){let r=e.source.body,n=r.length,i=t+1;for(;i<n;){let e=r.charCodeAt(i);if((0,eR.isNameContinue)(e))++i;else break}return e$(e,C.NAME,t,i,r.slice(t,i))}(e,i);throw eD(e.source,i,39===t?"Unexpected single quote character ('), did you mean to use a double quote (\")?":ew(t)||ek(r,i)?`Unexpected character: ${eM(e,i)}.`:`Invalid character: ${eM(e,i)}.`)}return e$(e,C.EOF,n,n)}(this,e.end);e.next=t,t.prev=e,e=t}while(e.kind===C.COMMENT)return e}}function ew(e){return e>=0&&e<=55295||e>=57344&&e<=1114111}function ek(e,t){return eL(e.charCodeAt(t))&&eF(e.charCodeAt(t+1))}function eL(e){return e>=55296&&e<=56319}function eF(e){return e>=56320&&e<=57343}function eM(e,t){let r=e.source.body.codePointAt(t);if(void 0===r)return C.EOF;if(r>=32&&r<=126){let e=String.fromCodePoint(r);return'"'===e?"'\"'":`"${e}"`}return"U+"+r.toString(16).toUpperCase().padStart(4,"0")}function e$(e,t,r,n,i){let a=e.line,s=1+r-e.lineStart;return new e_.Token(t,r,n,a,s,i)}function ex(e,t,r){if(!(0,eR.isDigit)(r))throw eD(e.source,t,`Invalid number, expected digit but got: ${eM(e,t)}.`);let n=e.source.body,i=t+1;for(;(0,eR.isDigit)(n.charCodeAt(i));)++i;return i}function eV(e,t){return eU(e.charCodeAt(t))<<12|eU(e.charCodeAt(t+1))<<8|eU(e.charCodeAt(t+2))<<4|eU(e.charCodeAt(t+3))}function eU(e){return e>=48&&e<=57?e-48:e>=65&&e<=70?e-55:e>=97&&e<=102?e-87:-1}var eq=e.i(36535),eB=e.i(41888);e.i(47167);let ej=globalThis.process&&1?function(e,t){return e instanceof t}:function(e,t){if(e instanceof t)return!0;if("object"==typeof e&&null!==e){var r;let n=t.prototype[Symbol.toStringTag];if(n===(Symbol.toStringTag in e?e[Symbol.toStringTag]:null==(r=e.constructor)?void 0:r.name)){let t=(0,eB.inspect)(e);throw Error(`Cannot use ${n} "${t}" from another module or realm.

Ensure that there is only one instance of "graphql" in the node_modules
directory. If different versions of "graphql" are the dependencies of other
relied on modules, use "resolutions" to ensure only one version is installed.

https://yarnpkg.com/en/docs/selective-version-resolutions

Duplicate "graphql" modules cannot be used at the same time since different
versions may have different capabilities and behavior. The data from one
version used in the function from another could produce confusing and
spurious results.`)}}return!1};class eQ{constructor(e,t="GraphQL request",r={line:1,column:1}){"string"==typeof e||(0,eq.devAssert)(!1,`Body must be a string. Received: ${(0,eB.inspect)(e)}.`),this.body=e,this.name=t,this.locationOffset=r,this.locationOffset.line>0||(0,eq.devAssert)(!1,"line in locationOffset is 1-indexed and must be positive."),this.locationOffset.column>0||(0,eq.devAssert)(!1,"column in locationOffset is 1-indexed and must be positive.")}get[Symbol.toStringTag](){return"Source"}}class ez{constructor(e,t={}){const{lexer:r,...n}=t;if(r)this._lexer=r;else{const t=ej(e,eQ)?e:new eQ(e);this._lexer=new eS(t)}this._options=n,this._tokenCounter=0}get tokenCount(){return this._tokenCounter}parseName(){let e=this.expectToken(C.NAME);return this.node(e,{kind:eO.Kind.NAME,value:e.value})}parseDocument(){return this.node(this._lexer.token,{kind:eO.Kind.DOCUMENT,definitions:this.many(C.SOF,this.parseDefinition,C.EOF)})}parseDefinition(){if(this.peek(C.BRACE_L))return this.parseOperationDefinition();let e=this.peekDescription(),t=e?this._lexer.lookahead():this._lexer.token;if(e&&t.kind===C.BRACE_L)throw eD(this._lexer.source,this._lexer.token.start,"Unexpected description, descriptions are not supported on shorthand queries.");if(t.kind===C.NAME){switch(t.value){case"schema":return this.parseSchemaDefinition();case"scalar":return this.parseScalarTypeDefinition();case"type":return this.parseObjectTypeDefinition();case"interface":return this.parseInterfaceTypeDefinition();case"union":return this.parseUnionTypeDefinition();case"enum":return this.parseEnumTypeDefinition();case"input":return this.parseInputObjectTypeDefinition();case"directive":return this.parseDirectiveDefinition()}switch(t.value){case"query":case"mutation":case"subscription":return this.parseOperationDefinition();case"fragment":return this.parseFragmentDefinition()}if(e)throw eD(this._lexer.source,this._lexer.token.start,"Unexpected description, only GraphQL definitions support descriptions.");if("extend"===t.value)return this.parseTypeSystemExtension()}throw this.unexpected(t)}parseOperationDefinition(){let e,t=this._lexer.token;if(this.peek(C.BRACE_L))return this.node(t,{kind:eO.Kind.OPERATION_DEFINITION,operation:e_.OperationTypeNode.QUERY,description:void 0,name:void 0,variableDefinitions:[],directives:[],selectionSet:this.parseSelectionSet()});let r=this.parseDescription(),n=this.parseOperationType();return this.peek(C.NAME)&&(e=this.parseName()),this.node(t,{kind:eO.Kind.OPERATION_DEFINITION,operation:n,description:r,name:e,variableDefinitions:this.parseVariableDefinitions(),directives:this.parseDirectives(!1),selectionSet:this.parseSelectionSet()})}parseOperationType(){let e=this.expectToken(C.NAME);switch(e.value){case"query":return e_.OperationTypeNode.QUERY;case"mutation":return e_.OperationTypeNode.MUTATION;case"subscription":return e_.OperationTypeNode.SUBSCRIPTION}throw this.unexpected(e)}parseVariableDefinitions(){return this.optionalMany(C.PAREN_L,this.parseVariableDefinition,C.PAREN_R)}parseVariableDefinition(){return this.node(this._lexer.token,{kind:eO.Kind.VARIABLE_DEFINITION,description:this.parseDescription(),variable:this.parseVariable(),type:(this.expectToken(C.COLON),this.parseTypeReference()),defaultValue:this.expectOptionalToken(C.EQUALS)?this.parseConstValueLiteral():void 0,directives:this.parseConstDirectives()})}parseVariable(){let e=this._lexer.token;return this.expectToken(C.DOLLAR),this.node(e,{kind:eO.Kind.VARIABLE,name:this.parseName()})}parseSelectionSet(){return this.node(this._lexer.token,{kind:eO.Kind.SELECTION_SET,selections:this.many(C.BRACE_L,this.parseSelection,C.BRACE_R)})}parseSelection(){return this.peek(C.SPREAD)?this.parseFragment():this.parseField()}parseField(){let e,t,r=this._lexer.token,n=this.parseName();return this.expectOptionalToken(C.COLON)?(e=n,t=this.parseName()):t=n,this.node(r,{kind:eO.Kind.FIELD,alias:e,name:t,arguments:this.parseArguments(!1),directives:this.parseDirectives(!1),selectionSet:this.peek(C.BRACE_L)?this.parseSelectionSet():void 0})}parseArguments(e){let t=e?this.parseConstArgument:this.parseArgument;return this.optionalMany(C.PAREN_L,t,C.PAREN_R)}parseArgument(e=!1){let t=this._lexer.token,r=this.parseName();return this.expectToken(C.COLON),this.node(t,{kind:eO.Kind.ARGUMENT,name:r,value:this.parseValueLiteral(e)})}parseConstArgument(){return this.parseArgument(!0)}parseFragment(){let e=this._lexer.token;this.expectToken(C.SPREAD);let t=this.expectOptionalKeyword("on");return!t&&this.peek(C.NAME)?this.node(e,{kind:eO.Kind.FRAGMENT_SPREAD,name:this.parseFragmentName(),directives:this.parseDirectives(!1)}):this.node(e,{kind:eO.Kind.INLINE_FRAGMENT,typeCondition:t?this.parseNamedType():void 0,directives:this.parseDirectives(!1),selectionSet:this.parseSelectionSet()})}parseFragmentDefinition(){let e=this._lexer.token,t=this.parseDescription();return(this.expectKeyword("fragment"),!0===this._options.allowLegacyFragmentVariables)?this.node(e,{kind:eO.Kind.FRAGMENT_DEFINITION,description:t,name:this.parseFragmentName(),variableDefinitions:this.parseVariableDefinitions(),typeCondition:(this.expectKeyword("on"),this.parseNamedType()),directives:this.parseDirectives(!1),selectionSet:this.parseSelectionSet()}):this.node(e,{kind:eO.Kind.FRAGMENT_DEFINITION,description:t,name:this.parseFragmentName(),typeCondition:(this.expectKeyword("on"),this.parseNamedType()),directives:this.parseDirectives(!1),selectionSet:this.parseSelectionSet()})}parseFragmentName(){if("on"===this._lexer.token.value)throw this.unexpected();return this.parseName()}parseValueLiteral(e){let t=this._lexer.token;switch(t.kind){case C.BRACKET_L:return this.parseList(e);case C.BRACE_L:return this.parseObject(e);case C.INT:return this.advanceLexer(),this.node(t,{kind:eO.Kind.INT,value:t.value});case C.FLOAT:return this.advanceLexer(),this.node(t,{kind:eO.Kind.FLOAT,value:t.value});case C.STRING:case C.BLOCK_STRING:return this.parseStringLiteral();case C.NAME:switch(this.advanceLexer(),t.value){case"true":return this.node(t,{kind:eO.Kind.BOOLEAN,value:!0});case"false":return this.node(t,{kind:eO.Kind.BOOLEAN,value:!1});case"null":return this.node(t,{kind:eO.Kind.NULL});default:return this.node(t,{kind:eO.Kind.ENUM,value:t.value})}case C.DOLLAR:if(e){if(this.expectToken(C.DOLLAR),this._lexer.token.kind===C.NAME){let e=this._lexer.token.value;throw eD(this._lexer.source,t.start,`Unexpected variable "$${e}" in constant value.`)}throw this.unexpected(t)}return this.parseVariable();default:throw this.unexpected()}}parseConstValueLiteral(){return this.parseValueLiteral(!0)}parseStringLiteral(){let e=this._lexer.token;return this.advanceLexer(),this.node(e,{kind:eO.Kind.STRING,value:e.value,block:e.kind===C.BLOCK_STRING})}parseList(e){let t=()=>this.parseValueLiteral(e);return this.node(this._lexer.token,{kind:eO.Kind.LIST,values:this.any(C.BRACKET_L,t,C.BRACKET_R)})}parseObject(e){let t=()=>this.parseObjectField(e);return this.node(this._lexer.token,{kind:eO.Kind.OBJECT,fields:this.any(C.BRACE_L,t,C.BRACE_R)})}parseObjectField(e){let t=this._lexer.token,r=this.parseName();return this.expectToken(C.COLON),this.node(t,{kind:eO.Kind.OBJECT_FIELD,name:r,value:this.parseValueLiteral(e)})}parseDirectives(e){let t=[];for(;this.peek(C.AT);)t.push(this.parseDirective(e));return t}parseConstDirectives(){return this.parseDirectives(!0)}parseDirective(e){let t=this._lexer.token;return this.expectToken(C.AT),this.node(t,{kind:eO.Kind.DIRECTIVE,name:this.parseName(),arguments:this.parseArguments(e)})}parseTypeReference(){let e,t=this._lexer.token;if(this.expectOptionalToken(C.BRACKET_L)){let r=this.parseTypeReference();this.expectToken(C.BRACKET_R),e=this.node(t,{kind:eO.Kind.LIST_TYPE,type:r})}else e=this.parseNamedType();return this.expectOptionalToken(C.BANG)?this.node(t,{kind:eO.Kind.NON_NULL_TYPE,type:e}):e}parseNamedType(){return this.node(this._lexer.token,{kind:eO.Kind.NAMED_TYPE,name:this.parseName()})}peekDescription(){return this.peek(C.STRING)||this.peek(C.BLOCK_STRING)}parseDescription(){if(this.peekDescription())return this.parseStringLiteral()}parseSchemaDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("schema");let r=this.parseConstDirectives(),n=this.many(C.BRACE_L,this.parseOperationTypeDefinition,C.BRACE_R);return this.node(e,{kind:eO.Kind.SCHEMA_DEFINITION,description:t,directives:r,operationTypes:n})}parseOperationTypeDefinition(){let e=this._lexer.token,t=this.parseOperationType();this.expectToken(C.COLON);let r=this.parseNamedType();return this.node(e,{kind:eO.Kind.OPERATION_TYPE_DEFINITION,operation:t,type:r})}parseScalarTypeDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("scalar");let r=this.parseName(),n=this.parseConstDirectives();return this.node(e,{kind:eO.Kind.SCALAR_TYPE_DEFINITION,description:t,name:r,directives:n})}parseObjectTypeDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("type");let r=this.parseName(),n=this.parseImplementsInterfaces(),i=this.parseConstDirectives(),a=this.parseFieldsDefinition();return this.node(e,{kind:eO.Kind.OBJECT_TYPE_DEFINITION,description:t,name:r,interfaces:n,directives:i,fields:a})}parseImplementsInterfaces(){return this.expectOptionalKeyword("implements")?this.delimitedMany(C.AMP,this.parseNamedType):[]}parseFieldsDefinition(){return this.optionalMany(C.BRACE_L,this.parseFieldDefinition,C.BRACE_R)}parseFieldDefinition(){let e=this._lexer.token,t=this.parseDescription(),r=this.parseName(),n=this.parseArgumentDefs();this.expectToken(C.COLON);let i=this.parseTypeReference(),a=this.parseConstDirectives();return this.node(e,{kind:eO.Kind.FIELD_DEFINITION,description:t,name:r,arguments:n,type:i,directives:a})}parseArgumentDefs(){return this.optionalMany(C.PAREN_L,this.parseInputValueDef,C.PAREN_R)}parseInputValueDef(){let e,t=this._lexer.token,r=this.parseDescription(),n=this.parseName();this.expectToken(C.COLON);let i=this.parseTypeReference();this.expectOptionalToken(C.EQUALS)&&(e=this.parseConstValueLiteral());let a=this.parseConstDirectives();return this.node(t,{kind:eO.Kind.INPUT_VALUE_DEFINITION,description:r,name:n,type:i,defaultValue:e,directives:a})}parseInterfaceTypeDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("interface");let r=this.parseName(),n=this.parseImplementsInterfaces(),i=this.parseConstDirectives(),a=this.parseFieldsDefinition();return this.node(e,{kind:eO.Kind.INTERFACE_TYPE_DEFINITION,description:t,name:r,interfaces:n,directives:i,fields:a})}parseUnionTypeDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("union");let r=this.parseName(),n=this.parseConstDirectives(),i=this.parseUnionMemberTypes();return this.node(e,{kind:eO.Kind.UNION_TYPE_DEFINITION,description:t,name:r,directives:n,types:i})}parseUnionMemberTypes(){return this.expectOptionalToken(C.EQUALS)?this.delimitedMany(C.PIPE,this.parseNamedType):[]}parseEnumTypeDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("enum");let r=this.parseName(),n=this.parseConstDirectives(),i=this.parseEnumValuesDefinition();return this.node(e,{kind:eO.Kind.ENUM_TYPE_DEFINITION,description:t,name:r,directives:n,values:i})}parseEnumValuesDefinition(){return this.optionalMany(C.BRACE_L,this.parseEnumValueDefinition,C.BRACE_R)}parseEnumValueDefinition(){let e=this._lexer.token,t=this.parseDescription(),r=this.parseEnumValueName(),n=this.parseConstDirectives();return this.node(e,{kind:eO.Kind.ENUM_VALUE_DEFINITION,description:t,name:r,directives:n})}parseEnumValueName(){if("true"===this._lexer.token.value||"false"===this._lexer.token.value||"null"===this._lexer.token.value)throw eD(this._lexer.source,this._lexer.token.start,`${eG(this._lexer.token)} is reserved and cannot be used for an enum value.`);return this.parseName()}parseInputObjectTypeDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("input");let r=this.parseName(),n=this.parseConstDirectives(),i=this.parseInputFieldsDefinition();return this.node(e,{kind:eO.Kind.INPUT_OBJECT_TYPE_DEFINITION,description:t,name:r,directives:n,fields:i})}parseInputFieldsDefinition(){return this.optionalMany(C.BRACE_L,this.parseInputValueDef,C.BRACE_R)}parseTypeSystemExtension(){let e=this._lexer.lookahead();if(e.kind===C.NAME)switch(e.value){case"schema":return this.parseSchemaExtension();case"scalar":return this.parseScalarTypeExtension();case"type":return this.parseObjectTypeExtension();case"interface":return this.parseInterfaceTypeExtension();case"union":return this.parseUnionTypeExtension();case"enum":return this.parseEnumTypeExtension();case"input":return this.parseInputObjectTypeExtension();case"directive":if(this._options.experimentalDirectivesOnDirectiveDefinitions)return this.parseDirectiveDefinitionExtension()}throw this.unexpected(e)}parseSchemaExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("schema");let t=this.parseConstDirectives(),r=this.optionalMany(C.BRACE_L,this.parseOperationTypeDefinition,C.BRACE_R);if(0===t.length&&0===r.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.SCHEMA_EXTENSION,directives:t,operationTypes:r})}parseScalarTypeExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("scalar");let t=this.parseName(),r=this.parseConstDirectives();if(0===r.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.SCALAR_TYPE_EXTENSION,name:t,directives:r})}parseObjectTypeExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("type");let t=this.parseName(),r=this.parseImplementsInterfaces(),n=this.parseConstDirectives(),i=this.parseFieldsDefinition();if(0===r.length&&0===n.length&&0===i.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.OBJECT_TYPE_EXTENSION,name:t,interfaces:r,directives:n,fields:i})}parseInterfaceTypeExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("interface");let t=this.parseName(),r=this.parseImplementsInterfaces(),n=this.parseConstDirectives(),i=this.parseFieldsDefinition();if(0===r.length&&0===n.length&&0===i.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.INTERFACE_TYPE_EXTENSION,name:t,interfaces:r,directives:n,fields:i})}parseUnionTypeExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("union");let t=this.parseName(),r=this.parseConstDirectives(),n=this.parseUnionMemberTypes();if(0===r.length&&0===n.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.UNION_TYPE_EXTENSION,name:t,directives:r,types:n})}parseEnumTypeExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("enum");let t=this.parseName(),r=this.parseConstDirectives(),n=this.parseEnumValuesDefinition();if(0===r.length&&0===n.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.ENUM_TYPE_EXTENSION,name:t,directives:r,values:n})}parseInputObjectTypeExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("input");let t=this.parseName(),r=this.parseConstDirectives(),n=this.parseInputFieldsDefinition();if(0===r.length&&0===n.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.INPUT_OBJECT_TYPE_EXTENSION,name:t,directives:r,fields:n})}parseDirectiveDefinitionExtension(){let e=this._lexer.token;this.expectKeyword("extend"),this.expectKeyword("directive"),this.expectToken(C.AT);let t=this.parseName(),r=this.parseConstDirectives();if(0===r.length)throw this.unexpected();return this.node(e,{kind:eO.Kind.DIRECTIVE_EXTENSION,name:t,directives:r})}parseDirectiveDefinition(){let e=this._lexer.token,t=this.parseDescription();this.expectKeyword("directive"),this.expectToken(C.AT);let r=this.parseName(),n=this.parseArgumentDefs(),i=this._options.experimentalDirectivesOnDirectiveDefinitions?this.parseConstDirectives():[],a=this.expectOptionalKeyword("repeatable");this.expectKeyword("on");let s=this.parseDirectiveLocations();return this.node(e,{kind:eO.Kind.DIRECTIVE_DEFINITION,description:t,name:r,arguments:n,directives:i,repeatable:a,locations:s})}parseDirectiveLocations(){return this.delimitedMany(C.PIPE,this.parseDirectiveLocation)}parseDirectiveLocation(){let e=this._lexer.token,t=this.parseName();if(Object.prototype.hasOwnProperty.call(O,t.value))return t;throw this.unexpected(e)}parseSchemaCoordinate(){let e,t,r=this._lexer.token,n=this.expectOptionalToken(C.AT),i=this.parseName();return(!n&&this.expectOptionalToken(C.DOT)&&(e=this.parseName()),(n||e)&&this.expectOptionalToken(C.PAREN_L)&&(t=this.parseName(),this.expectToken(C.COLON),this.expectToken(C.PAREN_R)),n)?t?this.node(r,{kind:eO.Kind.DIRECTIVE_ARGUMENT_COORDINATE,name:i,argumentName:t}):this.node(r,{kind:eO.Kind.DIRECTIVE_COORDINATE,name:i}):e?t?this.node(r,{kind:eO.Kind.ARGUMENT_COORDINATE,name:i,fieldName:e,argumentName:t}):this.node(r,{kind:eO.Kind.MEMBER_COORDINATE,name:i,memberName:e}):this.node(r,{kind:eO.Kind.TYPE_COORDINATE,name:i})}node(e,t){return!0!==this._options.noLocation&&(t.loc=new e_.Location(e,this._lexer.lastToken,this._lexer.source)),t}peek(e){return this._lexer.token.kind===e}expectToken(e){let t=this._lexer.token;if(t.kind===e)return this.advanceLexer(),t;throw eD(this._lexer.source,t.start,`Expected ${eK(e)}, found ${eG(t)}.`)}expectOptionalToken(e){return this._lexer.token.kind===e&&(this.advanceLexer(),!0)}expectKeyword(e){let t=this._lexer.token;if(t.kind===C.NAME&&t.value===e)this.advanceLexer();else throw eD(this._lexer.source,t.start,`Expected "${e}", found ${eG(t)}.`)}expectOptionalKeyword(e){let t=this._lexer.token;return t.kind===C.NAME&&t.value===e&&(this.advanceLexer(),!0)}unexpected(e){let t=null!=e?e:this._lexer.token;return eD(this._lexer.source,t.start,`Unexpected ${eG(t)}.`)}any(e,t,r){this.expectToken(e);let n=[];for(;!this.expectOptionalToken(r);)n.push(t.call(this));return n}optionalMany(e,t,r){if(this.expectOptionalToken(e)){let e=[];do e.push(t.call(this));while(!this.expectOptionalToken(r))return e}return[]}many(e,t,r){this.expectToken(e);let n=[];do n.push(t.call(this));while(!this.expectOptionalToken(r))return n}delimitedMany(e,t){this.expectOptionalToken(e);let r=[];do r.push(t.call(this));while(this.expectOptionalToken(e))return r}advanceLexer(){let{maxTokens:e}=this._options,t=this._lexer.advance();if(t.kind!==C.EOF&&(++this._tokenCounter,void 0!==e&&this._tokenCounter>e))throw eD(this._lexer.source,t.start,`Document contains more that ${e} tokens. Parsing aborted.`)}}function eG(e){let t=e.value;return eK(e.kind)+(null!=t?` "${t}"`:"")}function eK(e){return e===C.BANG||e===C.DOLLAR||e===C.AMP||e===C.PAREN_L||e===C.PAREN_R||e===C.DOT||e===C.SPREAD||e===C.COLON||e===C.EQUALS||e===C.AT||e===C.BRACKET_L||e===C.BRACKET_R||e===C.BRACE_L||e===C.PIPE||e===C.BRACE_R?`"${e}"`:e}var eH=new Map,eY=new Map,eX=!0,eW=!1;function eJ(e){return e.replace(/[\s,]+/g," ").trim()}function eZ(e){for(var t=[],r=1;r<arguments.length;r++)t[r-1]=arguments[r];"string"==typeof e&&(e=[e]);var n=e[0];return t.forEach(function(t,r){t&&"Document"===t.kind?n+=t.loc.source.body:n+=t,n+=e[r+1]}),function(e){var t=eJ(e);if(!eH.has(t)){let u,d;var r,n,i,a,s,o=(Object.defineProperty(d=(u=new ez(e,{experimentalFragmentVariables:eW,allowLegacyFragmentVariables:eW,experimentalFragmentArguments:eW})).parseDocument(),"tokenCount",{enumerable:!1,value:u.tokenCount}),d);if(!o||"Document"!==o.kind)throw Error("Not a valid GraphQL document.");eH.set(t,((a=new Set((r=new Set,n=[],o.definitions.forEach(function(e){if("FragmentDefinition"===e.kind){var t,i=e.name.value,a=eJ((t=e.loc).source.body.substring(t.start,t.end)),s=eY.get(i);s&&!s.has(a)?eX&&console.warn("Warning: fragment with name "+i+" already exists.\ngraphql-tag enforces all fragment names across your application to be unique; read more about\nthis in the docs: http://dev.apollodata.com/core/fragments.html#unique-names"):s||eY.set(i,s=new Set),s.add(a),r.has(a)||(r.add(a),n.push(e))}else n.push(e)}),i=(0,F.__assign)((0,F.__assign)({},o),{definitions:n})).definitions)).forEach(function(e){e.loc&&delete e.loc,Object.keys(e).forEach(function(t){var r=e[t];r&&"object"==typeof r&&a.add(r)})}),(s=i.loc)&&(delete s.startToken,delete s.endToken),i))}return eH.get(t)}(n)}var e0=eZ;(i=eZ||(eZ={})).gql=e0,i.resetCaches=function(){eH.clear(),eY.clear()},i.disableFragmentWarnings=function(){eX=!1},i.enableExperimentalFragmentVariables=function(){eW=!0},i.disableExperimentalFragmentVariables=function(){eW=!1},eZ.default=eZ;let e1=eZ`
  mutation CreateDepartment($input: CreateDepartmentInput!) {
    createDepartment(input: $input) {
      status
      message
      data {
        id
        name
        nursing
        supportRequests
        requestsProducts
        insurancePolicyMode
        insurancePolicies {
          id
          insuranceName
          acronym
          defaultCoveragePercentage
          supportedByClinic
          iconUrl
        }
        defaultProducts {
          id
          name
          genericName
          code
          description
          type
          unit
          privateRhicPrice
          clinicPrice
          insuranceCoverages {
            id
            insuranceProvider {
              id
              insuranceName
              acronym
              defaultCoveragePercentage
              supportedByClinic
              iconUrl
            }
            cost
            covered
            requireMedicalAdvisor
          }
        }
        createdAt
        updatedAt
      }
    }
  }
`,e3=eZ`
  mutation UpdateDepartment($departmentId: ID!, $input: UpdateDepartmentInput!) {
    updateDepartment(departmentId: $departmentId, input: $input) {
      status
      message
      data {
        id
        name
        nursing
        supportRequests
        requestsProducts
        insurancePolicyMode
        insurancePolicies {
          id
          insuranceName
          acronym
          defaultCoveragePercentage
          supportedByClinic
          iconUrl
        }
        defaultProducts {
          id
          name
          genericName
          code
          description
          type
          unit
          privateRhicPrice
          clinicPrice
          insuranceCoverages {
            id
            insuranceProvider {
              id
              insuranceName
              acronym
              defaultCoveragePercentage
              supportedByClinic
              iconUrl
            }
            cost
            covered
            requireMedicalAdvisor
          }
        }
        createdAt
        updatedAt
      }
    }
  }
`,e2=eZ`
  mutation DeleteDepartment($id: ID!) {
    deleteDepartment(id: $id) {
      status
      message
      
    }
  }
`,e4=eZ`
  mutation AddDepartmentInsurance($departmentId: ID!, $insuranceId: ID!) {
    addDepartmentInsurance(departmentId: $departmentId, insuranceId: $insuranceId) {
      status
      message
      
    }
  }
`,e9=eZ`
  mutation RemoveDepartmentInsurance($departmentId: ID!, $insuranceId: ID!) {
    removeDepartmentInsurance(departmentId: $departmentId, insuranceId: $insuranceId) {
      status
      message
      
    }
  }
`,e5=eZ`
  mutation AddDepartmentProduct($departmentId: ID!, $productId: ID!) {
    addDepartmentProduct(departmentId: $departmentId, productId: $productId) {
      status
      message
      
    }
  }
`,e8=eZ`
  mutation RemoveDepartmentProduct($departmentId: ID!, $productId: ID!) {
    removeDepartmentProduct(departmentId: $departmentId, productId: $productId) {
      status
      message
      
    }
  }
`,e6=eZ`
  mutation CreateProduct($input: CreateProductInput!) {
    createProduct(input: $input) {
      status
      message

      data {
        id
        name
        genericName
        code
        description
        type
        unit
        metadata
        privateRhicPrice
        clinicPrice
        insuranceCoverages {
          id
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
          cost
          covered
          requireMedicalAdvisor
          mustPrescribedBy
          drugAdministrationFrequency
          authorizationRequestReasons
        }
        createdAt
        updatedAt
      }
    }
  }
`,e7=eZ`
  mutation UpdateProduct($productId: ID!, $input: UpdateProductInput!) {
    updateProduct(productId: $productId, input: $input) {
      status
      message

      data {
        id
        name
        genericName
        code
        description
        type
        unit
        metadata
        privateRhicPrice
        clinicPrice
        insuranceCoverages {
          id
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
          cost
          covered
          requireMedicalAdvisor
          mustPrescribedBy
          drugAdministrationFrequency
          authorizationRequestReasons
        }
        createdAt
        updatedAt
      }
    }
  }
`,te=eZ`
  mutation DeleteProduct($productId: ID!) {
    deleteProduct(productId: $productId) {
      status
      message
    }
  }
`,tt=eZ`
  mutation AddProductInsuranceCoverage(
    $productId: ID!
    $input: CreateProductInsuranceCoverageInput!
  ) {
    createProductInsuranceCoverage(productId: $productId, input: $input) {
      status
      message

      data {
        id
        insuranceProvider {
          id
          insuranceName
          acronym
          defaultCoveragePercentage
        }
        cost
        covered
        requireMedicalAdvisor
        mustPrescribedBy
        drugAdministrationFrequency
        authorizationRequestReasons
      }
    }
  }
`,tr=eZ`
  mutation RemoveProductInsuranceCoverage($productInsuranceCoverageId: ID!) {
    deleteProductInsuranceCoverage(
      productInsuranceCoverageId: $productInsuranceCoverageId
    ) {
      status
      message
    }
  }
`,tn=eZ`
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      status
      message

      data {
        accessToken
        refreshToken
        user {
          id
          firstName
          lastName
          email
          phoneNumber
          username
          accountStatus
          roles
          departments {
            id
            name
          }
          createdAt
          updatedAt
        }
      }
    }
  }
`,ti=eZ`
  mutation SetInitialPassword($input: SetInitialPasswordInput!) {
    setInitialPassword(input: $input) {
      status
      message
    }
  }
`,ta=eZ`
  mutation Register($input: SelfRegisterInput!) {
    selfRegister(input: $input) {
      status
      message

      data {
        id
        firstName
        lastName
        email
        phoneNumber
        username
      }
    }
  }
`,ts=eZ`
  mutation AdminCreateUser($input: AdminCreateUserInput!) {
    adminCreateUser(input: $input) {
      status
      message

      data {
        id
        firstName
        lastName
        email
        phoneNumber
        username
        accountStatus
        roles
        departments {
          id
          name
        }
        createdAt
        updatedAt
      }
    }
  }
`,to=eZ`
  mutation ActivateUser($input: ActivateUserInput!) {
    activateUser(input: $input) {
      status
      message
    }
  }
`,tu=eZ`
  mutation DeactivateUser($input: DeactivateUserInput!) {
    deactivateUser(input: $input) {
      status
      message
    }
  }
`,td=eZ`
  mutation AdminUpdateUser($userId: ID!, $input: AdminUpdateUserInput!) {
    adminUpdateUser(userId: $userId, input: $input) {
      status
      message

      data {
        id
        firstName
        lastName
        email
        phoneNumber
        username
        accountStatus
        roles
        departments {
          id
          name
        }
        createdAt
        updatedAt
      }
    }
  }
`,tl=eZ`
  mutation UpdateUserRoles($input: ActivateUserInput!) {
    activateUser(input: $input) {
      status
      message
    }
  }
`,tc=eZ`
  mutation UpdateMyProfile($input: UpdateMyProfileInput!) {
    updateMyProfile(input: $input) {
      status
      message
    }
  }
`,tp=eZ`
  mutation ChangePassword($input: ChangeMyPasswordInput!) {
    changeMyPassword(input: $input) {
      status
      message
    }
  }
`,tm=eZ`
  mutation AdminTriggerPasswordReset($input: AdminTriggerPasswordResetInput!) {
    adminTriggerPasswordReset(input: $input) {
      status
      message
    }
  }
`,th=eZ`
  mutation UpdateClinicProfile($input: UpdateClinicProfileInput!) {
    updateClinicProfile(input: $input) {
      status
      message

      data {
        id
        name
        address
        contacts {
          contactType
          value
          description
        }
        tinNumber
        logoUrl
        metadata {
          key
          value
        }
        createdAt
        updatedAt
      }
    }
  }
`,tf=eZ`
  mutation RegisterPatient($input: CreatePatientInput!) {
    createPatient(input: $input) {
      status
      message
      data {
        id
        visitDate
        status
        patient {
          id
          firstName
          lastName
          middleName
          gender
          dateOfBirth
          primaryPhoneNumber
          alternativePhone
          village
          city
          district
          postalAddress
          nationalIdNumber
          passportNumber
          emergencyContactName
          emergencyContactRelationship
          emergencyContactPhoneNumber
          createdAt
          updatedAt
        }
        linkedInsurances {
          id
          insuranceCardNumber
          principalMember
          principalMemberName
          principalMemberPhoneNumber
          validFrom
          validUntil
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
        }
      }
    }
  }
`,tv=eZ`
  mutation CreatePatientInsurance($input: CreatePatientInsuranceInput!) {
    createPatientInsurance(input: $input) {
      status
      message
      data {
        id
        insuranceCardNumber
        principalMember
        principalMemberName
        principalMemberPhoneNumber
        validFrom
        validUntil
      }
    }
  }
`,ty=eZ`
  mutation UpdatePatientInsurance($patientInsuranceId: ID!, $input: UpdatePatientInsuranceInput!) {
    updatePatientInsurance(patientInsuranceId: $patientInsuranceId, input: $input) {
      status
      message
      data {
        id
        insuranceCardNumber
        principalMember
        principalMemberName
        principalMemberPhoneNumber
        validFrom
        validUntil
      }
    }
  }
`,tg=eZ`
  mutation UpdatePatient($patientId: ID!, $input: UpdatePatientInput!) {
    updatePatient(patientId: $patientId, input: $input) {
      status
      message
      data {
        id
        firstName
        lastName
        middleName
        gender
        dateOfBirth
        primaryPhoneNumber
        alternativePhone
        village
        city
        district
        postalAddress
        nationalIdNumber
        passportNumber
        emergencyContactName
        emergencyContactRelationship
        emergencyContactPhoneNumber
        createdAt
        updatedAt
      }
    }
  }
`,tb=eZ`
  mutation CreateVisit($input: CreateVisitInput!) {
    createVisit(input: $input) {
      status
      message

      data {
        id
        visitDate
        status
        patient {
          id
          firstName
          lastName
        }
        linkedInsurances {
          id
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
        }
        departments {
          id
          department {
            id
            name
          }
        }
      }
    }
  }
`;eZ`
  mutation AddVisitNote($visitId: ID!, $note: String!) {
    addVisitNote(visitId: $visitId, note: $note) {
      status
      message

      data {
        id
        note
        createdBy {
          id
          firstName
          lastName
          email
        }
        createdAt
      }
    }
  }
`;let tI=eZ`
  mutation AddVisitVitalSigns($input: AddVisitVitalSignsInput!) {
    addVisitVitalSigns(input: $input) {
      status
      message

      data {
        id
        visitDate
        status
        patient {
          id
          firstName
          lastName
        }
        vitalSigns {
          id
          createdAt
          addedBy {
            id
            firstName
            lastName
          }
          measurements {
            id
            measurementName
            value
            unit
            createdAt
          }
        }
      }
    }
  }
`;eZ`
  mutation AddDepartmentNote(
    $visitId: ID!
    $departmentId: ID!
    $note: String!
  ) {
    addDepartmentNote(
      visitId: $visitId
      departmentId: $departmentId
      note: $note
    ) {
      status
      message

      data {
        id
        note
        createdBy {
          id
          firstName
          lastName
          email
        }
        createdAt
      }
    }
  }
`;let tA=eZ`
  mutation AddChildVisitDepartment($input: AddChildVisitDepartmentInput!) {
    addChildVisitDepartment(input: $input) {
      status
      message
      data {
        id
        status
        completedAt
        department {
          id
          name
        }
        products {
          id
          product {
            id
            name
            code
            type
          }
          quantity
          price
          status
        }
        createdAt
        updatedAt
      }
    }
  }
`,tN=eZ`
  mutation AddDiagnosis($input: AddDiagnosisInput!) {
    addDiagnosis(input: $input) {
      status
      message
      data {
        id
        status
        completedAt
        updatedAt
        department {
          id
          name
        }
        diagnostics {
          id
          diagnosisName
          icd11Code
          createdAt
        }
      }
    }
  }
`,tE=eZ`
  mutation AddMedication($input: AddMedicationInput!) {
    addMedication(input: $input) {
      status
      message
      data {
        id
        status
        completedAt
        updatedAt
        department {
          id
          name
        }
        medications {
          id
          medicationName
          instructions
          createdAt
        }
      }
    }
  }
`;eZ`
  mutation UpsertConsultationAnswers($input: ConsultationAnswersInput!) {
    upsertConsultationAnswers(input: $input) {
      status
      message
      data {
        id
        consultationId
        visitId
        patientId
        departmentId
        status
        answers
        submittedAt
        updatedAt
        dedicatedForm {
          id
          version
        }
      }
    }
  }
`,eZ`
  mutation GenerateConsultationPdf($consultationId: ID!, $departmentId: ID!) {
    generateConsultationPdf(
      consultationId: $consultationId
      departmentId: $departmentId
    ) {
      status
      message
      data {
        pdfBase64
        pdfUrl
      }
    }
  }
`,eZ`
  mutation ProcessVisitDepartment($visitId: ID!, $departmentId: ID!) {
    processVisitDepartment(visitId: $visitId, departmentId: $departmentId) {
      status
      message
      data {
        id
        status
        transferTime
        completedTime
      }
    }
  }
`;let tT=eZ`
  mutation AddVisitDepartmentProduct(
    $input: CreateVisitDepartmentProductInput!
  ) {
    addVisitDepartmentProduct(input: $input) {
      status
      message

      data {
        id
        department {
          id
          name
        }
        status
        products {
          id
          product {
            id
            name
            type
          }
          quantity
          price
          status
          addedBy {
            id
            firstName
            lastName
            email
          }
        }
      }
    }
  }
`;eZ`
  mutation CompleteVisitDepartment($visitId: ID!, $departmentId: ID!) {
    completeVisitDepartment(visitId: $visitId, departmentId: $departmentId) {
      status
      message

      data {
        id
        status
        completedTime
      }
    }
  }
`;let tP=eZ`
  mutation UpdateVisitDepartmentStatus(
    $input: UpdateVisitDepartmentStatusInput!
  ) {
    updateVisitDepartmentStatus(input: $input) {
      status
      message

      data {
        id
        status
        completedAt
        updatedAt
        department {
          id
          name
        }
      }
    }
  }
`,tD=eZ`
  mutation AddDepartmentToVisit(
    $visitId: ID!
    $departmentId: ID!
    $processorId: ID
  ) {
    addVisitDepartment(
      visitId: $visitId
      departmentId: $departmentId
      processorId: $processorId
    ) {
      status
      message
      data {
        id
        patient {
          id
          firstName
          lastName
        }
        departments {
          id
          department {
            id
            name
          }
          status
        }
      }
    }
  }
`,t_=eZ`
  mutation LinkVisitInsurances($visitId: ID!, $insuranceIds: [ID!]!) {
    linkVisitInsurances(visitId: $visitId, insuranceIds: $insuranceIds) {
      status
      message
      data {
        id
        linkedInsurances {
          id
          insuranceCardNumber
          principalMember
          principalMemberName
          principalMemberPhoneNumber
          validFrom
          validUntil
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
        }
      }
    }
  }
`,tO=eZ`
  mutation UnlinkVisitInsurances($visitId: ID!, $insuranceIds: [ID!]!) {
    unlinkVisitInsurances(visitId: $visitId, insuranceIds: $insuranceIds) {
      status
      message
      data {
        id
        linkedInsurances {
          id
          insuranceCardNumber
          principalMember
          principalMemberName
          principalMemberPhoneNumber
          validFrom
          validUntil
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
        }
      }
    }
  }
`,tC=eZ`
  mutation UpdateVisitDepartmentProductQuantity(
    $input: UpdateVisitDepartmentProductQuantityInput!
  ) {
    updateVisitDepartmentProductQuantity(input: $input) {
      status
      message
      data {
        id
        department {
          id
          name
        }
        status
        products {
          id
          product {
            id
            name
            type
          }
          quantity
          price
          status
        }
      }
    }
  }
`;eZ`
  mutation UpdateVisitDepartmentProductStatus(
    $input: UpdateVisitDepartmentProductStatusInput!
  ) {
    updateVisitDepartmentProductStatus(input: $input) {
      status
      message
      data {
        id
        department {
          id
          name
        }
        status
        products {
          id
          product {
            id
            name
            type
          }
          quantity
          price
          status
        }
      }
    }
  }
`;let tR=eZ`
  mutation RemoveVisitDepartmentProduct($visitDepartmentProductId: ID!) {
    removeVisitDepartmentProduct(
      visitDepartmentProductId: $visitDepartmentProductId
    ) {
      status
      message
      data {
        id
        department {
          id
          name
        }
        status
        products {
          id
          product {
            id
            name
            type
          }
          quantity
          price
          status
        }
      }
    }
  }
`;eZ`
  mutation RemoveActionFromVisitDepartment(
    $visitId: ID!
    $departmentId: ID!
    $itemId: ID!
  ) {
    removeActionFromVisitDepartment(
      input: { visitId: $visitId, departmentId: $departmentId, itemId: $itemId }
    ) {
      status
      data {
        id
        departments {
          id
          status
          actions {
            id
            action {
              id
              name
              type
              privatePrice
            }
            quantity
          }
          consumables {
            id
            consumable {
              id
              name
              type
              privatePrice
            }
            quantity
          }
        }
      }
      messages {
        text
        type
      }
    }
  }
`,eZ`
  mutation RemoveConsumableFromVisitDepartment(
    $visitId: ID!
    $departmentId: ID!
    $itemId: ID!
  ) {
    removeConsumableFromVisitDepartment(
      input: { visitId: $visitId, departmentId: $departmentId, itemId: $itemId }
    ) {
      status
      data {
        id
        departments {
          id
          status
          actions {
            id
            action {
              id
              name
              type
              privatePrice
            }
            quantity
          }
          consumables {
            id
            consumable {
              id
              name
              type
              privatePrice
            }
            quantity
          }
        }
      }
      messages {
        text
        type
      }
    }
  }
`,eZ`
  mutation UpdateActionQuantity(
    $visitId: ID!
    $departmentId: ID!
    $itemId: ID!
    $quantity: Int!
  ) {
    updateActionQuantity(
      input: {
        visitId: $visitId
        departmentId: $departmentId
        itemId: $itemId
        quantity: $quantity
      }
    ) {
      status
      data {
        id
        departments {
          id
          status
          actions {
            id
            action {
              id
              name
              type
              privatePrice
            }
            quantity
          }
          consumables {
            id
            consumable {
              id
              name
              type
              privatePrice
            }
            quantity
          }
        }
      }
      messages {
        text
        type
      }
    }
  }
`,eZ`
  mutation UpdateConsumableQuantity(
    $visitId: ID!
    $departmentId: ID!
    $itemId: ID!
    $quantity: Int!
  ) {
    updateConsumableQuantity(
      input: {
        visitId: $visitId
        departmentId: $departmentId
        itemId: $itemId
        quantity: $quantity
      }
    ) {
      status
      data {
        id
        departments {
          id
          status
          actions {
            id
            action {
              id
              name
              type
              privatePrice
            }
            quantity
          }
          consumables {
            id
            consumable {
              id
              name
              type
              privatePrice
            }
            quantity
          }
        }
      }
      messages {
        text
        type
      }
    }
  }
`;let tS=eZ`
  mutation CompleteVisit($visitId: ID!) {
    completeVisit(visitId: $visitId) {
      status
      message
      data {
        id
        status
      }
    }
  }
`;eZ`
  mutation CompleteConsultationVisit(
    $input: ConsultationAnswersInput!
    $final: Boolean!
  ) {
    completeConsultationVisit(input: $input, final: $final) {
      status
      message
      data {
        id
        visitDate
        status
        patient {
          id
          firstName
          lastName
        }
        departments {
          id
          department {
            id
            name
          }
          status
        }
      }
    }
  }
`;let tw=eZ`
  mutation AddVisitDepartmentNote($input: AddVisitDepartmentNoteInput!) {
    addVisitDepartmentNote(input: $input) {
      status
      message
      data {
        id
        visitDepartmentId
        content
        createdBy {
          id
          firstName
          lastName
        }
        viewed
        createdAt
      }
    }
  }
`;eZ`
  mutation MarkVisitDepartmentNoteViewed($noteId: ID!) {
    markVisitDepartmentNoteViewed(noteId: $noteId) {
      status
      message
      data {
        id
        visitDepartmentId
        content
        createdBy {
          id
          firstName
          lastName
        }
        viewed
        createdAt
      }
    }
  }
`;let tk=eZ`
  mutation MarkVisitDepartmentNotesViewed($visitDepartmentId: ID!) {
    markVisitDepartmentNotesViewed(visitDepartmentId: $visitDepartmentId) {
      status
      message
      data {
        totalNotes
        newNotes
      }
    }
  }
`,tL=eZ`
  mutation CreateInsuranceProvider($input: CreateInsuranceProviderInput!) {
    createInsuranceProvider(input: $input) {
      status
      message
      
      data {
        id
        insuranceName
        acronym
        defaultCoveragePercentage
        supportedByClinic
        iconUrl
        createdAt
        updatedAt
      }
    }
  }
`,tF=eZ`
  mutation UpdateInsuranceProvider($insuranceProviderId: ID!, $input: UpdateInsuranceProviderInput!) {
    updateInsuranceProvider(insuranceProviderId: $insuranceProviderId, input: $input) {
      status
      message
      
      data {
        id
        insuranceName
        acronym
        defaultCoveragePercentage
        supportedByClinic
        iconUrl
        createdAt
        updatedAt
      }
    }
  }
`,tM=eZ`
  mutation DeleteInsuranceProvider($insuranceProviderId: ID!) {
    deleteInsuranceProvider(insuranceProviderId: $insuranceProviderId) {
      status
      message
    }
  }
`,t$=eZ`
  mutation BillVisit($input: BillVisitInput!) {
    billVisit(input: $input) {
      status
      message
      data {
        id
        visitId
        departments {
          id
          status
          totalAmount
          insuranceCoveredAmount
          patientPayableAmount
          paidAmount
          outstandingAmount
          insuranceBillings {
            id
            status
            totalAmount
            insuranceCoveredAmount
            patientPayableAmount
            paidAmount
            outstandingAmount
            items {
              id
              visitDepartmentProductId
              productId
              productName
              unitPriceSnapshot
              quantitySnapshot
              insuranceCoveredAmount
              patientPayableAmount
            }
          }
        }
      }
    }
  }
`,tx=eZ`
  mutation EditBillVisit($input: EditBillVisitInput!) {
    editBillVisit(input: $input) {
      status
      message
      data {
        id
        visitId
        departments {
          id
          status
          totalAmount
          insuranceCoveredAmount
          patientPayableAmount
          paidAmount
          outstandingAmount
          insuranceBillings {
            id
            status
            totalAmount
            insuranceCoveredAmount
            patientPayableAmount
            paidAmount
            outstandingAmount
            items {
              id
              visitDepartmentProductId
              productId
              productName
              unitPriceSnapshot
              quantitySnapshot
              insuranceCoveredAmount
              patientPayableAmount
            }
          }
        }
      }
    }
  }
`,tV=eZ`
  mutation GenerateInvoice($departmentInsuranceBillingId: ID!) {
    generateInvoice(
      departmentInsuranceBillingId: $departmentInsuranceBillingId
    ) {
      status
      message
      data {
        signedUrl
      }
    }
  }
`,tU=eZ`
  mutation CreateForm($departmentId: ID!, $input: FormInput!) {
    createForm(departmentId: $departmentId, input: $input) {
      status
      message
      
      data {
        id
        title
        description
        status
        version
        createdAt
        updatedAt
        sections {
          id
          title
          boldTitle
          italicTitle
          underlineTitle
          centerTitle
          columns
          order
          fields {
            id
            label
            type
            placeholder
            required
            options
            hideLabel
            boldLabel
            italicLabel
            underlineLabel
            centerLabel
            order
            tableConfig {
              mode
              rows
              columns
              headerPlacement
              columnHeaders
              rowHeaders
            }
            conditionalRendering {
              dependsOn
              condition
              value
              itemType
            }
          }
        }
        fields {
          id
          label
          type
          placeholder
          required
          options
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          order
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
        actions {
          id
          name
          type
          quantity
          price
          isQuantifiable
          backendId
        }
      }
    }
  }
`,tq=eZ`
  mutation UpdateForm($departmentId: ID!, $formId: ID!, $input: FormInput!) {
    updateForm(departmentId: $departmentId, formId: $formId, input: $input) {
      status
      message
      
      data {
        id
        title
        description
        status
        version
        createdAt
        updatedAt
        sections {
          id
          title
          boldTitle
          italicTitle
          underlineTitle
          centerTitle
          columns
          order
          fields {
            id
            label
            type
            placeholder
            required
            options
            hideLabel
            boldLabel
            italicLabel
            underlineLabel
            centerLabel
            order
            tableConfig {
              mode
              rows
              columns
              headerPlacement
              columnHeaders
              rowHeaders
            }
            conditionalRendering {
              dependsOn
              condition
              value
              itemType
            }
          }
        }
        fields {
          id
          label
          type
          placeholder
          required
          options
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          order
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
        actions {
          id
          name
          type
          quantity
          price
          isQuantifiable
          backendId
        }
      }
    }
  }
`,tB=eZ`
  mutation FinalizeForm($departmentId: ID!, $formId: ID!) {
    finalizeForm(departmentId: $departmentId, formId: $formId) {
      status
      message
      
      data {
        id
        title
        description
        status
        version
        createdAt
        updatedAt
        sections {
          id
          title
          boldTitle
          italicTitle
          underlineTitle
          centerTitle
          columns
          order
          fields {
            id
            label
            type
            placeholder
            required
            options
            hideLabel
            boldLabel
            italicLabel
            underlineLabel
            centerLabel
            order
            tableConfig {
              mode
              rows
              columns
              headerPlacement
              columnHeaders
              rowHeaders
            }
            conditionalRendering {
              dependsOn
              condition
              value
              itemType
            }
          }
        }
        fields {
          id
          label
          type
          placeholder
          required
          options
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          order
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
        actions {
          id
          name
          type
          quantity
          price
          isQuantifiable
          backendId
        }
      }
    }
  }
`,tj=eZ`
  mutation CreateStandaloneForm($input: StandaloneFormInput!) {
    createStandaloneForm(input: $input) {
      status
      message
      data {
        id
        name
        description
        type
        category
        isTemplate
        createdAt
        updatedAt
        activeVersion {
          id
          formId
          versionLabel
          majorVersion
          minorVersion
          blocks
          theme
          status
          createdAt
        }
      }
    }
  }
`,tQ=eZ`
  mutation UpdateStandaloneForm(
    $id: ID!
    $input: StandaloneFormInput!
    $markFinal: Boolean
  ) {
    updateStandaloneForm(id: $id, input: $input, markFinal: $markFinal) {
      status
      message
      data {
        id
        name
        description
        type
        category
        isTemplate
        createdAt
        updatedAt
        activeVersion {
          id
          formId
          versionLabel
          majorVersion
          minorVersion
          blocks
          theme
          status
          createdAt
        }
      }
    }
  }
`,tz=eZ`
  mutation DeleteStandaloneForm($id: ID!, $confirmDeleteAnswers: Boolean) {
    deleteStandaloneForm(id: $id, confirmDeleteAnswers: $confirmDeleteAnswers) {
      status
      message
      data
    }
  }
`,tG=eZ`
  mutation DuplicateStandaloneForm($sourceFormId: ID!) {
    duplicateStandaloneForm(sourceFormId: $sourceFormId) {
      status
      message
      data {
        id
        name
        description
        type
        category
        isTemplate
        createdAt
        updatedAt
        activeVersion {
          id
          formId
          versionLabel
          majorVersion
          minorVersion
          blocks
          theme
          status
          createdAt
        }
      }
    }
  }
`,tK=eZ`
  mutation SaveStandaloneAnswer(
    $formVersionId: ID!
    $answers: JSON!
    $status: AnswerStatus
    $score: Float
  ) {
    saveStandaloneAnswer(
      formVersionId: $formVersionId
      answers: $answers
      status: $status
      score: $score
    ) {
      status
      message
      data {
        id
        answers
        score
        status
        submittedAt
        createdAt
        updatedAt
      }
    }
  }
`,tH=eZ`
  mutation UpdateStandaloneAnswer(
    $answerId: ID!
    $answers: JSON!
    $status: AnswerStatus
    $score: Float
  ) {
    updateStandaloneAnswer(
      answerId: $answerId
      answers: $answers
      status: $status
      score: $score
    ) {
      status
      message
      data {
        id
        answers
        score
        status
        visitId
        submittedAt
        createdAt
        updatedAt
        formVersion {
          id
        }
      }
    }
  }
`,tY=eZ`
  mutation SaveVisitStandaloneAnswer(
    $visitId: ID!
    $visitDepartmentId: ID!
    $formVersionId: ID!
    $answers: JSON!
    $status: AnswerStatus
    $score: Float
  ) {
    saveVisitStandaloneAnswer(
      visitId: $visitId
      visitDepartmentId: $visitDepartmentId
      formVersionId: $formVersionId
      answers: $answers
      status: $status
      score: $score
    ) {
      status
      message
      data {
        answer {
          id
          answers
          score
          status
          visitId
          submittedAt
          createdAt
          updatedAt
          formVersion {
            id
          }
        }
        visitDepartment {
          id
          answerId
        }
      }
    }
  }
`,tX=eZ`
  mutation LinkStandaloneFormToDepartment($departmentId: ID!, $formId: ID!) {
    linkStandaloneFormToDepartment(
      departmentId: $departmentId
      formId: $formId
    ) {
      status
      message
      data {
        id
        name
        activeVersion {
          id
        }
      }
    }
  }
`,tW=eZ`
  mutation UnlinkStandaloneFormFromDepartment(
    $departmentId: ID!
    $formId: ID!
  ) {
    unlinkStandaloneFormFromDepartment(
      departmentId: $departmentId
      formId: $formId
    ) {
      status
      message
      data
    }
  }
`,tJ=eZ`
  mutation SetDefaultStandaloneFormForDepartment(
    $departmentId: ID!
    $formId: ID!
  ) {
    setDefaultStandaloneFormForDepartment(
      departmentId: $departmentId
      formId: $formId
    ) {
      status
      message
      data {
        id
        name
      }
    }
  }
`;e.s(["CREATE_STANDALONE_FORM_MUTATION",0,tj,"DELETE_STANDALONE_FORM_MUTATION",0,tz,"DUPLICATE_STANDALONE_FORM_MUTATION",0,tG,"LINK_STANDALONE_FORM_TO_DEPARTMENT_MUTATION",0,tX,"SAVE_STANDALONE_ANSWER_MUTATION",0,tK,"SAVE_VISIT_STANDALONE_ANSWER_MUTATION",0,tY,"SET_DEFAULT_STANDALONE_FORM_FOR_DEPARTMENT_MUTATION",0,tJ,"UNLINK_STANDALONE_FORM_FROM_DEPARTMENT_MUTATION",0,tW,"UPDATE_STANDALONE_ANSWER_MUTATION",0,tH,"UPDATE_STANDALONE_FORM_MUTATION",0,tQ],17383);let tZ=eZ`
  query GetDepartments($input: SearchDepartmentsInput) {
    departments(input: $input) {
      status
      message
      
      data {
        id
        name
        nursing
        supportRequests
        requestsProducts
        insurancePolicyMode
        insurancePolicies {
          id
          insuranceName
          acronym
          defaultCoveragePercentage
          supportedByClinic
          iconUrl
        }
        defaultProducts {
          id
          name
          genericName
          code
          description
          type
          unit
          privateRhicPrice
          clinicPrice
          insuranceCoverages {
            id
            insuranceProvider {
              id
              insuranceName
              acronym
              defaultCoveragePercentage
              supportedByClinic
              iconUrl
            }
            cost
            covered
            requireMedicalAdvisor
          }
        }
        createdAt
        updatedAt
      }
      pagination {
        total
        perPage
        currentPage
        totalPages
      }
    }
  }
`;eZ`
  query GetDepartment($id: ID!) {
    department(departmentId: $id) {
      status
      message
      
      data {
        id
        name
        nursing
        supportRequests
        requestsProducts
        insurancePolicyMode
        insurancePolicies {
          id
          insuranceName
          acronym
          defaultCoveragePercentage
          supportedByClinic
          iconUrl
        }
        defaultProducts {
          id
          name
          genericName
          code
          description
          type
          unit
          privateRhicPrice
          clinicPrice
          insuranceCoverages {
            id
            insuranceProvider {
              id
              insuranceName
              acronym
              defaultCoveragePercentage
              supportedByClinic
              iconUrl
            }
            cost
            covered
            requireMedicalAdvisor
          }
        }
        createdAt
        updatedAt
      }
    }
  }
`;let t0=eZ`
  query GetProducts($input: SearchProductsInput) {
    products(input: $input) {
      status
      message
      
      data {
        id
        name
        genericName
        code
        description
        type
        unit
        metadata
        privateRhicPrice
        clinicPrice
        insuranceCoverages {
          id
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
          cost
          covered
          requireMedicalAdvisor
          mustPrescribedBy
          drugAdministrationFrequency
          authorizationRequestReasons
        }
        createdAt
        updatedAt
      }
      pagination {
        total
        perPage
        currentPage
        totalPages
      }
    }
  }
`;eZ`
  query GetProduct($id: ID!) {
    product(productId: $id) {
      status
      message
      
      data {
        id
        name
        genericName
        code
        description
        type
        unit
        metadata
        privateRhicPrice
        clinicPrice
        insuranceCoverages {
          id
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
          cost
          covered
          requireMedicalAdvisor
          mustPrescribedBy
          drugAdministrationFrequency
          authorizationRequestReasons
        }
        createdAt
        updatedAt
      }
    }
  }
`;let t1=eZ`
  query SearchPatients($input: SearchPatientsInput) {
    searchPatients(input: $input) {
      status
      message
      data {
        id
        firstName
        middleName
        lastName
        dateOfBirth
        gender
        primaryPhoneNumber
        alternativePhone
        village
        city
        district
        postalAddress
        nationalIdNumber
        passportNumber
        emergencyContactName
        emergencyContactRelationship
        emergencyContactPhoneNumber
        patientInsurances {
          id
          insuranceCardNumber
          principalMember
          principalMemberName
          principalMemberPhoneNumber
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
        }
        createdAt
      }
      pagination {
        total
        totalPages
      }
    }
  }
`,t3=eZ`
  query GetPatient($patientId: ID!) {
    patient(patientId: $patientId) {
      status
      message
      data {
        id
        firstName
        middleName
        lastName
        dateOfBirth
        gender
        primaryPhoneNumber
        alternativePhone
        village
        city
        district
        postalAddress
        nationalIdNumber
        passportNumber
        emergencyContactName
        emergencyContactRelationship
        emergencyContactPhoneNumber
        createdAt
      }
    }
    patientInsurances(patientId: $patientId) {
      status
      data {
        id
        insuranceCardNumber
        principalMember
        principalMemberName
        principalMemberPhoneNumber
        insuranceProvider {
          id
          insuranceName
          acronym
          defaultCoveragePercentage
        }
      }
    }
  }
`,t2=eZ`
  query Me {
    me {
      status
      message

      data {
        id
        firstName
        lastName
        email
        phoneNumber
        username
        accountStatus
        roles
        departments {
          id
          name
        }
        createdAt
        updatedAt
      }
    }
  }
`,t4=eZ`
  query ClinicProfile {
    clinicProfile {
      status
      message

      data {
        id
        name
        username
        address
        contacts {
          contactType
          value
          description
        }
        tinNumber
        logoUrl
        metadata {
          key
          value
        }
        createdAt
        updatedAt
      }
    }
  }
`,t9=eZ`
  query GetUsers {
    listUsers {
      status
      message

      data {
        id
        firstName
        lastName
        email
        phoneNumber
        username
        accountStatus
        roles
        departments {
          id
          name
        }
        createdAt
        updatedAt
      }
    }
  }
`,t5=`
  id
  product {
    id
    name
    code
    type
    unit
    privateRhicPrice
    clinicPrice
    insuranceCoverages {
      id
      insuranceProvider {
        id
        insuranceName
        acronym
        defaultCoveragePercentage
      }
      cost
      covered
      requireMedicalAdvisor
    }
  }
  quantity
  price
  status
  addedBy {
    id
    firstName
    lastName
  }
  billedBy {
    id
    firstName
    lastName
  }
  processor {
    id
    firstName
    lastName
  }
  createdAt
  updatedAt
`,t8=`
  id
  status
  completedAt
  department {
    id
    name
    requestsProducts
  }
  processors {
    id
    firstName
    lastName
  }
  diagnostics {
    id
    diagnosisName
    icd11Code
    createdAt
  }
  medications {
    id
    medicationName
    instructions
    createdAt
  }
  products {
    ${t5}
  }
  answerId
  createdAt
  updatedAt
`,t6=eZ`
  query GetVisit($id: ID!) {
    visit(visitId: $id) {
      status
      message

      data {
        id
        status
        visitDate
        patient {
          id
          firstName
          lastName
          middleName
          patientIdentifier
          gender
          dateOfBirth
          primaryPhoneNumber
          alternativePhone
          village
          city
          district
          postalAddress
          nationalIdNumber
          passportNumber
          emergencyContactName
          emergencyContactRelationship
          emergencyContactPhoneNumber
          patientInsurances {
            id
            insuranceCardNumber
            providingCompanyOrEmployer
            principalMember
            principalMemberName
            principalMemberPhoneNumber
            validFrom
            validUntil
            insuranceProvider {
              id
              insuranceName
              acronym
              defaultCoveragePercentage
            }
          }
        }
        vitalSigns {
          id
          createdAt
          addedBy {
            id
            firstName
            lastName
          }
          measurements {
            id
            measurementName
            value
            unit
            createdAt
          }
        }
        linkedInsurances {
          id
          patient {
            id
            firstName
            lastName
          }
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
          insuranceCardNumber
          providingCompanyOrEmployer
          principalMember
          principalMemberName
          principalMemberPhoneNumber
          validFrom
          validUntil
        }
        departments {
          id
          department {
            id
            name
            insurancePolicyMode
            requestsProducts
          }
          status
          completedAt
          processors {
            id
            firstName
            lastName
          }
          diagnostics {
            id
            diagnosisName
            icd11Code
            createdAt
          }
          medications {
            id
            medicationName
            instructions
            createdAt
          }
          products {
            ${t5}
          }
          childVisitDepartments {
            ${t8}
          }
          preInstructions {
            id
            type
            note
            createdAt
            addedBy {
              id
              firstName
              lastName
            }
          }
          notes {
            totalNotes
            newNotes
          }
          answerId
          createdAt
          updatedAt
        }
      }
    }
  }
`,t7=eZ`
  query GetVisits($input: SearchVisitsInput!) {
    visits(input: $input) {
      status
      message

      data {
        id
        status
        visitDate
        patient {
          id
          firstName
          lastName
          patientIdentifier
          primaryPhoneNumber
        }
        linkedInsurances {
          id
          insuranceProvider {
            id
            insuranceName
            acronym
          }
        }
        departments {
          id
          department {
            id
            name
          }
          status
          answerId
          products {
            id
            product {
              id
              name
              code
              type
              unit
              privateRhicPrice
              clinicPrice
            }
            quantity
            price
            status
            addedBy {
              id
              firstName
              lastName
            }
            billedBy {
              id
              firstName
              lastName
            }
            createdAt
            updatedAt
          }
          childVisitDepartments {
            id
            status
            completedAt
            answerId
            department {
              id
              name
            }
            products {
              id
              product {
                id
                name
                code
                type
                unit
                privateRhicPrice
                clinicPrice
              }
              quantity
              price
              status
              addedBy {
                id
                firstName
                lastName
              }
              billedBy {
                id
                firstName
                lastName
              }
              createdAt
              updatedAt
            }
          }
          notes {
            totalNotes
            newNotes
          }
        }
      }
      pagination {
        total
        perPage
        currentPage
        totalPages
      }
    }
  }
`,re=eZ`
  query GetPatientHistory($patientId: ID!, $input: SearchPatientHistoryInput!) {
    getPatientHistory(patientId: $patientId, input: $input) {
      status
      message

      data {
        id
        status
        visitDate
        patient {
          id
          firstName
          lastName
          middleName
          patientIdentifier
          dateOfBirth
          gender
        }
        departments {
          id
          department {
            id
            name
          }
          status
          completedAt
          answerId
          diagnostics {
            id
            diagnosisName
            icd11Code
            createdAt
          }
          medications {
            id
            medicationName
            instructions
            createdAt
          }
          products {
            id
            product {
              id
              name
              code
              type
            }
            quantity
            price
            status
            createdAt
          }
          createdAt
          updatedAt
        }
      }
      pagination {
        total
        perPage
        currentPage
        totalPages
      }
    }
  }
`,rt=eZ`
  query LastPatientDepartmentVisit($visitId: ID!, $departmentId: ID!) {
    lastPatientDepartmentVisit(visitId: $visitId, departmentId: $departmentId) {
      status
      message
      data {
        lastVisit {
          id
          status
          visitDate
          patient {
            id
            firstName
            lastName
            middleName
            patientIdentifier
            dateOfBirth
            gender
          }
          departments {
            id
            department {
              id
              name
            }
            status
            completedAt
            diagnostics {
              id
              diagnosisName
              icd11Code
              createdAt
            }
            medications {
              id
              medicationName
              instructions
              createdAt
            }
            products {
              id
              product {
                id
                name
                code
                type
              }
              quantity
              price
              status
              createdAt
            }
            createdAt
            updatedAt
            answerId
          }
        }
        lastDepartmentVisit {
          visitId
          visitDepartment {
            id
            department {
              id
              name
            }
            status
            completedAt
            diagnostics {
              id
              diagnosisName
              icd11Code
              createdAt
            }
            medications {
              id
              medicationName
              instructions
              createdAt
            }
            products {
              id
              product {
                id
                name
                code
                type
              }
              quantity
              price
              status
              createdAt
            }
            createdAt
            updatedAt
            answerId
          }
        }
      }
    }
  }
`,rr=eZ`
  query DashboardStats($days: Int!) {
    dashboardStats(days: $days) {
      status
      message

      data {
        totalVisits
        completedVisits
        inProgressVisits
        totalRevenue
      }
    }
  }
`,rn=eZ`
  query GetVisitDepartmentNotes($visitId: ID!, $visitDepartmentId: ID!) {
    visitDepartmentNotes(
      visitId: $visitId
      visitDepartmentId: $visitDepartmentId
    ) {
      status
      message
      data {
        id
        visitDepartmentId
        content
        createdBy {
          id
          firstName
          lastName
        }
        noteType
        viewed
        createdAt
      }
    }
  }
`;e.s(["DASHBOARD_STATS_QUERY",0,rr,"GET_PATIENT_HISTORY_QUERY",0,re,"GET_VISIT_QUERY",0,t6,"LAST_PATIENT_DEPARTMENT_VISIT_QUERY",0,rt,"VISITS_QUERY",0,t7,"VISIT_DEPARTMENT_NOTES_QUERY",0,rn],82934),eZ`
  query GetInsurances($input: SearchInsuranceProvidersInput) {
    insuranceProviders(input: $input) {
      status
      message
      
      data {
        id
        insuranceName
        acronym
        defaultCoveragePercentage
        supportedByClinic
        iconUrl
        createdAt
        updatedAt
      }
      pagination {
        total
        perPage
        currentPage
        totalPages
      }
    }
  }
`,eZ`
  query GetInsurance($id: ID!) {
    insuranceProvider(insuranceProviderId: $id) {
      status
      message
      
      data {
        id
        insuranceName
        acronym
        defaultCoveragePercentage
        supportedByClinic
        iconUrl
        createdAt
        updatedAt
      }
    }
  }
`,eZ`
  query GetActions($name: String, $page: Int, $size: Int) {
    products(input: { name: $name, type: MEDICAL_ACT, page: $page, size: $size }) {
      status
      message
      
      data {
        id
        name
        genericName
        code
        description
        type
        unit
        metadata
        privateRhicPrice
        clinicPrice
        insuranceCoverages {
          id
          insuranceProvider {
            id
            insuranceName
            acronym
            defaultCoveragePercentage
          }
          cost
          covered
          requireMedicalAdvisor
          mustPrescribedBy
          drugAdministrationFrequency
          authorizationRequestReasons
        }
        createdAt
        updatedAt
      }
      pagination {
        total
        perPage
        currentPage
        totalPages
      }
    }
  }
`;let ri=eZ`
  query GetVisitBilling($visitId: ID!) {
    visitBilling(visitId: $visitId) {
      status
      message
      data {
        id
        visitId
        departments {
          id
          status
          totalAmount
          insuranceCoveredAmount
          patientPayableAmount
          paidAmount
          outstandingAmount
          insuranceBillings {
            id
            status
            totalAmount
            insuranceCoveredAmount
            patientPayableAmount
            paidAmount
            outstandingAmount
            items {
              id
              visitDepartmentProductId
              productId
              productName
              unitPriceSnapshot
              quantitySnapshot
              insuranceCoveredAmount
              patientPayableAmount
            }
          }
          createdAt
          updatedAt
        }
        createdAt
        updatedAt
      }
    }
  }
`,ra=eZ`
  query GetInvoice($departmentInsuranceBillingId: ID!) {
    getInvoice(departmentInsuranceBillingId: $departmentInsuranceBillingId) {
      status
      message
      data {
        signedUrl
      }
    }
  }
`;e.s(["GET_BILL_BY_VISIT_QUERY",0,ri,"GET_INVOICE_QUERY",0,ra],34618);let rs=eZ`
  query GetForms($departmentId: ID!) {
    getForms(departmentId: $departmentId) {
      status
      message

      data {
        id
        departmentId
        title
        description
        status
        version
        createdAt
        updatedAt
        sections {
          id
          title
          boldTitle
          italicTitle
          underlineTitle
          centerTitle
          columns
          order
          fields {
            id
            label
            type
            placeholder
            required
            options
            hideLabel
            boldLabel
            italicLabel
            underlineLabel
            centerLabel
            order
            tableConfig {
              mode
              rows
              columns
              headerPlacement
              columnHeaders
              rowHeaders
            }
            conditionalRendering {
              dependsOn
              condition
              value
              itemType
            }
          }
        }
        fields {
          id
          label
          type
          placeholder
          required
          options
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          order
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
        actions {
          id
          name
          type
          quantity
          price
          isQuantifiable
          backendId
        }
      }
    }
  }
`,ro=eZ`
  query GetForm($departmentId: ID!, $formId: ID!) {
    getForm(departmentId: $departmentId, formId: $formId) {
      status
      message

      data {
        id
        departmentId
        title
        description
        status
        version
        createdAt
        updatedAt
        sections {
          id
          title
          boldTitle
          italicTitle
          underlineTitle
          centerTitle
          columns
          order
          fields {
            id
            label
            type
            placeholder
            required
            options
            hideLabel
            boldLabel
            italicLabel
            underlineLabel
            centerLabel
            order
            tableConfig {
              mode
              rows
              columns
              headerPlacement
              columnHeaders
              rowHeaders
            }
            conditionalRendering {
              dependsOn
              condition
              value
              itemType
            }
          }
        }
        fields {
          id
          label
          type
          placeholder
          required
          options
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          order
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
        actions {
          id
          name
          type
          quantity
          price
          isQuantifiable
          backendId
        }
      }
    }
  }
`,ru=eZ`
  query GetFormVersionHistory($departmentId: ID!, $formId: ID!) {
    getFormVersionHistory(departmentId: $departmentId, formId: $formId) {
      status
      message

      data {
        id
        formId
        departmentId
        title
        description
        status
        version
        createdAt
        updatedAt
        sections {
          id
          title
          boldTitle
          italicTitle
          underlineTitle
          centerTitle
          columns
          order
          fields {
            id
            label
            type
            placeholder
            required
            options
            hideLabel
            boldLabel
            italicLabel
            underlineLabel
            centerLabel
            order
            tableConfig {
              mode
              rows
              columns
              headerPlacement
              columnHeaders
              rowHeaders
            }
            conditionalRendering {
              dependsOn
              condition
              value
              itemType
            }
          }
        }
        fields {
          id
          label
          type
          placeholder
          required
          options
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          order
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
        actions {
          id
          name
          type
          quantity
          price
          isQuantifiable
          backendId
        }
      }
    }
  }
`;eZ`
  query ConsultationGetLatestForm($departmentId: ID!) {
    getLatestForm(departmentId: $departmentId) {
      data {
        id
        title
        description
        status
        version
        fields {
          id
          label
          type
          placeholder
          required
          order
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          options
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
        sections {
          id
          title
          boldTitle
          italicTitle
          underlineTitle
          centerTitle
          columns
          order
          fields {
            id
            label
            type
            placeholder
            required
            order
            hideLabel
            boldLabel
            italicLabel
            underlineLabel
            centerLabel
            options
            tableConfig {
              mode
              rows
              columns
              headerPlacement
              columnHeaders
              rowHeaders
            }
            conditionalRendering {
              dependsOn
              condition
              value
              itemType
            }
          }
        }
      }
    }
  }
`;let rd=eZ`
  query GetStandaloneForms(
    $isTemplate: Boolean
    $category: String
    $name: String
  ) {
    getStandaloneForms(
      isTemplate: $isTemplate
      category: $category
      name: $name
    ) {
      status
      message
      data {
        id
        name
        description
        type
        category
        isTemplate
        createdBy
        createdAt
        updatedAt
        activeVersion {
          id
          formId
          versionLabel
          majorVersion
          minorVersion
          blocks
          theme
          status
          createdAt
        }
      }
    }
  }
`,rl=eZ`
  query GetStandaloneForm($id: ID!) {
    getStandaloneForm(id: $id) {
      status
      message
      data {
        id
        name
        description
        type
        category
        isTemplate
        createdBy
        createdAt
        updatedAt
        activeVersion {
          id
          formId
          versionLabel
          majorVersion
          minorVersion
          blocks
          theme
          status
          createdAt
        }
      }
    }
  }
`,rc=eZ`
  fragment StandaloneFormFields on StandaloneForm {
    id
    name
    description
    type
    category
    isTemplate
    createdBy
    createdAt
    updatedAt
    activeVersion {
      id
      formId
      versionLabel
      majorVersion
      minorVersion
      blocks
      theme
      status
      createdAt
    }
  }
`,rp=eZ`
  query GetDepartmentForms($departmentId: ID!) {
    getDepartmentForms(departmentId: $departmentId) {
      status
      message
      data {
        forms {
          isDefault
          form {
            ...StandaloneFormFields
          }
        }
        defaultForm {
          ...StandaloneFormFields
        }
      }
    }
  }
  ${rc}
`,rm=eZ`
  query GetStandaloneFormAnswers($formId: ID!) {
    getStandaloneFormAnswers(formId: $formId) {
      status
      message
      data {
        id
        answers
        score
        status
        patientId
        visitId
        submittedBy
        submittedAt
        createdAt
        updatedAt
        form {
          id
          name
        }
        formVersion {
          id
          versionLabel
          majorVersion
          minorVersion
        }
      }
    }
  }
`,rh=eZ`
  query GetStandaloneAnswer($id: ID!) {
    getStandaloneAnswer(id: $id) {
      status
      message
      data {
        id
        answers
        score
        status
        patientId
        visitId
        submittedAt
        createdAt
        updatedAt
        form {
          ...StandaloneFormFields
        }
        formVersion {
          id
          formId
          versionLabel
          majorVersion
          minorVersion
          blocks
          theme
          status
          createdAt
        }
      }
    }
  }
  ${rc}
`;e.s(["GET_DEPARTMENT_FORMS_QUERY",0,rp,"GET_STANDALONE_ANSWER_QUERY",0,rh,"GET_STANDALONE_FORMS_QUERY",0,rd,"GET_STANDALONE_FORM_ANSWERS_QUERY",0,rm,"GET_STANDALONE_FORM_QUERY",0,rl],82534),e.s([],18540),(a={}).SUCCESS="SUCCESS",a.ERROR="ERROR",a.UNAUTHENTICATED="UNAUTHENTICATED",a.UNAUTHORISED="UNAUTHORISED",a.PARTIAL_SUCCESS="PARTIAL_SUCCESS",(s={}).MANAGER="MANAGER",s.CLINIC_ADMIN="CLINIC_ADMIN",s.FINANCE="FINANCE",s.STAFF="STAFF",s.RECEPTION="RECEPTION",s.NURSE="NURSE",s.CLINICIAN="CLINICIAN",s.ADMIN="ADMIN";var rf=((o={}).PENDING="PENDING",o.ACTIVE="ACTIVE",o.DISABLED="DISABLED",o),rv=((u={}).MALE="MALE",u.FEMALE="FEMALE",u.OTHER="OTHER",u);(d={}).LICENSE="LICENSE",d.DIPLOMA_CERTIFICATE="DIPLOMA_CERTIFICATE",d.DEGREE_CERTIFICATE="DEGREE_CERTIFICATE",d.TRAINING_CERTIFICATE="TRAINING_CERTIFICATE",d.NATIONAL_ID="NATIONAL_ID",d.PASSPORT="PASSPORT",d.BACKGROUND_CHECK="BACKGROUND_CHECK",d.WORK_PERMIT="WORK_PERMIT",d.OTHER="OTHER",(l={}).DRUG="DRUG",l.MEDICAL_ACT="MEDICAL_ACT",l.BIOLOGICAL_ACT="BIOLOGICAL_ACT",l.CONSUMABLE_DEVICE="CONSUMABLE_DEVICE",(c={}).TABLET="TABLET",c.CAPSULE="CAPSULE",c.PESSARY="PESSARY",c.SUPPOSITORY="SUPPOSITORY",c.BOX_OF_6_TABLETS="BOX_OF_6_TABLETS",c.BOX_OF_7_PESSARIES="BOX_OF_7_PESSARIES",c.BOX_OF_9_TABLETS="BOX_OF_9_TABLETS",c.BOX_OF_12_TABLETS="BOX_OF_12_TABLETS",c.BOX_OF_12_PESSARIES="BOX_OF_12_PESSARIES",c.BOX_OF_14_TABLETS="BOX_OF_14_TABLETS",c.BOX_OF_18_TABLETS="BOX_OF_18_TABLETS",c.BOX_OF_18_PESSARIES="BOX_OF_18_PESSARIES",c.BOX_OF_24_TABLETS="BOX_OF_24_TABLETS",c.BOX_OF_1_PESSARY="BOX_OF_1_PESSARY",c.BOX_OF_3_PESSARIES="BOX_OF_3_PESSARIES",c.BOX_OF_6_PESSARIES="BOX_OF_6_PESSARIES",c.BOTTLE="BOTTLE",c.VIAL="VIAL",c.AMPOULE="AMPOULE",c.TUBE="TUBE",c.TUBE_OF_15_TABLETS="TUBE_OF_15_TABLETS",c.TUBE_OF_20_TABLETS="TUBE_OF_20_TABLETS",c.TUBE_OF_50_STRIPS="TUBE_OF_50_STRIPS",c.BOX="BOX",c.SACHET="SACHET",c.POT="POT",c.ROLL="ROLL",c.PIECE="PIECE",c.DOSE="DOSE",c.KIT_OF_ONE_DAY_DOSE="KIT_OF_ONE_DAY_DOSE",c.PCS="PCS",c.UNKNOWN="UNKNOWN";var ry=((p={}).ALL="ALL",p.ONLY="ONLY",p.EXCEPT="EXCEPT",p);(m={}).CREATED="CREATED",m.IN_PROGRESS="IN_PROGRESS",m.CANCELLED="CANCELLED",m.COMPLETED="COMPLETED";var rg=((h={}).BILLED="BILLED",h.EXEMPTED="EXEMPTED",h.UNPAID="UNPAID",h.PENDING="PENDING",h),rb=((f={}).ACTIVE="ACTIVE",f.PENDING="PENDING",f.ON_HOLD="ON_HOLD",f.BILLING="BILLING",f.COMPLETED="COMPLETED",f.CANCELLED="CANCELLED",f),rI=((v={}).OUTPATIENT="OUTPATIENT",v.INPATIENT_OBSERVATION="INPATIENT_OBSERVATION",v.INPATIENT_ADMISSION="INPATIENT_ADMISSION",v);function rA(e){return{id:e.id,insuranceName:e.insuranceName,acronym:e.acronym,defaultCoveragePercentage:Number(e.defaultCoveragePercentage??0),supportedByClinic:e.supportedByClinic??!0,iconUrl:e.iconUrl,createdAt:"",updatedAt:"",name:e.insuranceName}}function rN(e){return{id:String(e.id||""),insuranceProvider:rA(e.insuranceProvider||{id:"",insuranceName:""}),cost:Number(e.cost??0),covered:!!e.covered,requireMedicalAdvisor:!!e.requireMedicalAdvisor,mustPrescribedBy:e.mustPrescribedBy,drugAdministrationFrequency:e.drugAdministrationFrequency,authorizationRequestReasons:e.authorizationRequestReasons||[],createdAt:"",updatedAt:""}}function rE(e){return{id:e.id,name:e.name,genericName:e.genericName,code:e.code||"",description:e.description||"",type:e.type||"MEDICAL_ACT",unit:e.unit||"UNKNOWN",privateRhicPrice:e.privateRhicPrice,clinicPrice:e.clinicPrice,insuranceCoverages:(e.insuranceCoverages||[]).map(rN),createdAt:"",updatedAt:""}}function rT(e){if(!e?.id)return;let t=e.firstName||"",r=e.lastName;return{id:e.id,firstName:t,lastName:r,email:e.email,phoneNumber:e.phoneNumber,username:e.username,accountStatus:rf.ACTIVE,roles:[],departments:[],createdAt:"",updatedAt:"",name:[t,r].filter(Boolean).join(" ")||e.email||void 0}}function rP(e){let t,r=rT(e);return r?{...r,accountStatus:(t=String(e?.accountStatus||"").toUpperCase())in rf?rf[t]:rf.PENDING,roles:e?.roles||[],departments:(e?.departments||[]).map(e=>rC({id:e.id,name:e.name,insurancePolicyMode:void 0,requestsProducts:!1,nursing:!1,supportRequests:!1})),createdAt:e?.createdAt||"",updatedAt:e?.updatedAt||""}:{id:"",firstName:"",accountStatus:rf.PENDING,roles:[],departments:[],createdAt:"",updatedAt:""}}function rD(e){let t,r={id:e.id,firstName:e.firstName,middleName:e.middleName,lastName:e.lastName,patientIdentifier:e.patientIdentifier,dateOfBirth:e.dateOfBirth||"",gender:(t=String(e.gender||"").toUpperCase())===rv.MALE||"M"===t?rv.MALE:t===rv.FEMALE||"F"===t?rv.FEMALE:(rv.OTHER,rv.OTHER),primaryPhoneNumber:e.primaryPhoneNumber,alternativePhone:e.alternativePhone,village:e.village,city:e.city,district:e.district,postalAddress:e.postalAddress,nationalIdNumber:e.nationalIdNumber,passportNumber:e.passportNumber,emergencyContactName:e.emergencyContactName,emergencyContactRelationship:e.emergencyContactRelationship,emergencyContactPhoneNumber:e.emergencyContactPhoneNumber,patientInsurances:[],createdAt:e.createdAt||"",updatedAt:e.updatedAt||""};return r.patientInsurances=(e.patientInsurances||[]).map(e=>r_(e,r)),r}function r_(e,t){return{id:e.id,patient:t,insuranceProvider:rA(e.insuranceProvider),insuranceCardNumber:e.insuranceCardNumber,providingCompanyOrEmployer:e.providingCompanyOrEmployer,principalMember:!!e.principalMember,principalMemberName:e.principalMemberName,principalMemberPhoneNumber:e.principalMemberPhoneNumber,validFrom:e.validFrom||"",validUntil:e.validUntil||"",createdAt:"",updatedAt:""}}function rO(e){let t=e.product?rE(e.product):{id:"",name:"",code:"",description:"",type:"MEDICAL_ACT",unit:"UNKNOWN",insuranceCoverages:[],createdAt:"",updatedAt:""};return{id:e.id,product:t,quantity:Number(e.quantity??0),price:Number(e.price??t.clinicPrice??t.privateRhicPrice??0),status:e.status,addedBy:rT(e.addedBy),billedBy:rT(e.billedBy),processor:rT(e.processor),createdAt:e.createdAt||"",updatedAt:e.updatedAt||""}}function rC(e){return{id:e.id,name:e.name,insurancePolicyMode:e.insurancePolicyMode||ry.ALL,insurancePolicies:[],defaultProducts:[],nursing:e.nursing??!1,supportRequests:e.supportRequests??!1,requestsProducts:e.requestsProducts??!1,createdAt:"",updatedAt:""}}function rR(e){let t,r=e.department?rC(e.department):{id:"",name:"",insurancePolicyMode:ry.ALL,insurancePolicies:[],defaultProducts:[],nursing:!1,supportRequests:!1,requestsProducts:!1,createdAt:"",updatedAt:""};return{id:e.id,department:r,status:e.status,encounterType:(t=String(e.encounterType||"").toUpperCase())in rI?rI[t]:rI.OUTPATIENT,completedAt:e.completedAt,processors:(e.processors||[]).map(rT).filter(e=>!!e),childVisitDepartments:(e.childVisitDepartments||[]).map(rR),products:(e.products||[]).map(rO),diagnostics:(e.diagnostics||[]).map(e=>({id:String(e.id),diagnosisName:String(e.diagnosisName||""),icd11Code:e.icd11Code,createdAt:e.createdAt||""})),medications:(e.medications||[]).map(e=>({id:String(e.id),medicationName:String(e.medicationName||""),instructions:String(e.instructions||""),createdAt:e.createdAt||""})),preInstructions:[],answerId:e.answerId??null,createdAt:e.createdAt||"",updatedAt:e.updatedAt||""}}function rS(e,t){let r=t?.patientMapper?t.patientMapper(e.patient):rD(e.patient);return{id:e.id,patient:r,status:e.status,visitDate:e.visitDate,linkedInsurances:(e.linkedInsurances||[]).map(e=>r_(e,r)),departments:(e.departments||[]).map(rR),vitalSigns:[]}}(y={}).UNPAID="UNPAID",y.PARTIALLY_PAID="PARTIALLY_PAID",y.PAID="PAID",(g={}).PHONE="PHONE",g.EMAIL="EMAIL",g.POBOX="POBOX",(b={}).DRAFT="DRAFT",b.FINAL="FINAL",(I={}).text="text",I.email="email",I.number="number",I.date="date",I.textarea="textarea",I.actionListener="actionListener",I.select="select",I.radio="radio",I.checkbox="checkbox",I.table="table",I.labRecord="labRecord",I.signature="signature",I.file="file",I.heading="heading",I.paragraph="paragraph",I.diagnosticRecord="diagnosticRecord",I.medicationLongForm="medicationLongForm",I.medicationMiniForm="medicationMiniForm",(A={}).STATIC="STATIC",A.DYNAMIC="DYNAMIC",(N={}).equals="equals",N.not_equals="not_equals",N.contains="contains",N.not_contains="not_contains",N.greater_than="greater_than",N.less_than="less_than",N.is_empty="is_empty",N.is_not_empty="is_not_empty",N.hasItem="hasItem",(E={}).DRAFT="DRAFT",E.FINAL="FINAL",(T={}).PENDING="PENDING",T.ONGOING="ONGOING",T.COMPLETED="COMPLETED",T.REJECTED="REJECTED",(P={}).CASH="CASH",P.MOBILE_MONEY="MOBILE_MONEY",P.CARD="CARD",P.BANK_TRANSFER="BANK_TRANSFER",P.CHEQUE="CHEQUE",P.MIXED="MIXED",e.s(["AccountStatus",()=>rf,"DepartmentInsurancePolicyMode",()=>ry,"EncounterType",()=>rI,"Gender",()=>rv,"VisitDepartmentStatus",()=>rb,"VisitProductStatus",()=>rg],60984);let rw=e=>e?{id:String(e.id||""),name:e.name?.trim()||void 0,address:e.address?.trim()||void 0,contacts:e.contacts??[],tinNumber:e.tinNumber?.trim()||void 0,logoUrl:e.logoUrl?.trim()||void 0,metadata:e.metadata??null,createdAt:e.createdAt||"",updatedAt:e.updatedAt||""}:null;function rk(){let e=L(),[t,{loading:r,error:n}]=Y(tn);return{login:async(r,n)=>{try{let i=await t({variables:{input:{identifier:r,password:n}}}),a=i?.data?.login;console.log("=== LOGIN MUTATION RESULT ===",{status:a?.status,user:a?.data?.user,accessToken:a?.data?.accessToken?"***":void 0});let s=e=>{let t=rP(e);return e?.id||e?.firstName?t:{...t,name:t.name||r}};if(a?.status==="SUCCESS"&&a.data?.accessToken){let t=a.data.accessToken,r=a.data.user,n=r?s(r):s(),i=null;if(!r)try{let r=await e.query({query:t2,fetchPolicy:"no-cache",context:{headers:{Authorization:`Bearer ${t}`}}}),i=r?.data?.me?.data;i&&(n=s(i))}catch{}try{let r=await e.query({query:t4,fetchPolicy:"no-cache",context:{headers:{Authorization:`Bearer ${t}`}}});i=rw(r?.data?.clinicProfile?.data)}catch{i=null}return console.log("=== BUILT USER (before storage) ===",n),console.log("=== STORING TO LOCALSTORAGE ===",JSON.stringify(n)),{status:"SUCCESS",data:{token:t,accessToken:t,refreshToken:a.data.refreshToken,user:n,clinicProfile:i},messages:a.message?[{text:a.message,type:"SUCCESS"}]:void 0}}if(a?.status==="PARTIAL_SUCCESS"&&a.data){let e=a.data.user,t=e?s(e):s();return{status:"PARTIAL_SUCCESS",data:{token:void 0,accessToken:void 0,refreshToken:void 0,user:t,needsPasswordSetup:!0},messages:a.message?[{text:a.message,type:"INFO"}]:void 0}}let o=a?.message||"Login failed";return{status:a?.status||"ERROR",message:a?.message,messages:[{text:o,type:"ERROR"}]}}catch(e){return{status:"ERROR",messages:[{text:eb(e)||"Network error occurred",type:"ERROR"}]}}},loading:r,error:n}}function rL(){let[e,{loading:t,error:r}]=Y(ti);return{setInitialPassword:async(t,r)=>{try{let n=await e({variables:{input:{identifier:t,newPassword:r}}}),i=n.data?.setInitialPassword;return{status:i?.status||"ERROR",message:i?.message,messages:i?.message?[{text:i.message,type:i.status||"ERROR"}]:void 0}}catch(t){let e=eb(t)||"Network error occurred";return{status:"ERROR",message:e,messages:[{text:e,type:"ERROR"}]}}},loading:t,error:r}}function rF(){let[e,{loading:t,error:r}]=Y(ta);return{register:async(t,r,n,i,a)=>{try{let[a,...s]=t.trim().split(/\s+/).filter(Boolean),o=s.length>0?s.join(" "):null,u=r?.split("@")?.[0]||i||null,d=await e({variables:{input:{firstName:a||t,lastName:o,email:r,password:n,phoneNumber:i,username:u}}}),l=d?.data?.selfRegister;return{status:l?.status||"ERROR",message:l?.message,data:l?.data?rP(l.data):void 0,messages:l?.message?[{text:l.message,type:l?.status||"ERROR"}]:void 0}}catch(e){throw console.error("Register error:",e),e}},loading:t,error:r}}function rM(){let{data:e,loading:t,error:r,refetch:n}=eu(t4,{fetchPolicy:"cache-and-network"});return{clinicProfile:rw(e?.clinicProfile?.data),loading:t,error:r?.message||null,refetch:n}}function r$(){let[e,{loading:t,error:r}]=Y(th);return{upsertClinicProfile:async t=>{let{data:r}=await e({variables:{input:{name:t.name,address:t.address,contacts:t.contacts,tinNumber:t.tinNumber,logoUrl:t.logoUrl,metadata:t.metadata}}}),n=r?.updateClinicProfile;return{status:n?.status||"ERROR",message:n?.message,data:rw(n?.data)}},loading:t,error:r?.message||null}}function rx(){let{data:e,loading:t,error:r,refetch:n}=eu(t9,{fetchPolicy:"cache-and-network"}),i=r?.message||null;return{users:(e?.listUsers?.data||[]).map(rP),loading:t,error:i,refetch:n}}function rV(){let[e,{loading:t,error:r}]=Y(ts);return{adminCreateUser:async t=>{try{let r={input:{firstName:t.firstName,lastName:t.lastName,gender:t.gender,dateOfBirth:t.dateOfBirth,profilePhotoUrl:t.profilePhotoUrl,email:t.email,phoneNumber:t.phoneNumber,username:t.username,departmentIds:t.departmentIds??[],roles:t.roles,workerDocProfile:t.workerDocProfile||null}},n=await e({variables:r});return n.data?.adminCreateUser}catch(e){throw console.error("Admin create user error:",e),e}},loading:t,error:r}}function rU(){let[e,{loading:t,error:r}]=Y(td);return{adminUpdateUser:async(t,r)=>{try{let n={userId:t,input:{firstName:r.firstName||void 0,lastName:r.lastName||void 0,gender:r.gender||void 0,dateOfBirth:r.dateOfBirth||void 0,profilePhotoUrl:r.profilePhotoUrl||void 0,email:r.email||void 0,phoneNumber:r.phoneNumber||void 0,username:r.username||void 0,departmentIds:r.departmentIds??void 0,roles:r.roles||void 0,workerDocProfile:r.workerDocProfile||void 0}},i=await e({variables:n});return i.data?.adminUpdateUser}catch(e){throw console.error("Admin update user error:",e),e}},loading:t,error:r}}function rq(){let[e,{loading:t,error:r}]=Y(to);return{activateUser:async(t,r)=>{try{let n=await e({variables:{input:{userId:t,roles:r}}});return n.data?.activateUser}catch(e){throw console.error("Activate user error:",e),e}},loading:t,error:r}}function rB(){let[e,{loading:t,error:r}]=Y(tu);return{deactivateUser:async(t,r=!1)=>{try{let n=await e({variables:{input:{userId:t,revokeSessions:r}}});return n.data?.deactivateUser}catch(e){throw console.error("Deactivate user error:",e),e}},loading:t,error:r}}function rj(){let[e,{loading:t,error:r}]=Y(tl);return{updateUserRoles:async(t,r)=>{try{let n=await e({variables:{input:{userId:t,roles:r}}});return n.data?.activateUser}catch(e){throw console.error("Update user roles error:",e),e}},loading:t,error:r}}function rQ(){let[e,{loading:t,error:r}]=Y(tc);return{updateMyProfile:async t=>{try{let[r,...n]=(t.name||"").trim().split(/\s+/).filter(Boolean),i=n.length>0?n.join(" "):void 0,a=await e({variables:{input:{firstName:r||void 0,lastName:i,email:t.email||void 0,phoneNumber:t.phoneNumber||void 0}}}),s=a.data?.updateMyProfile,o=s?.data;return{status:s?.status||"ERROR",message:s?.message,messages:s?.message?[{text:s.message,type:s?.status||"ERROR"}]:void 0,data:o?rP(o):void 0}}catch(e){throw console.error("Update my profile error:",e),e}},loading:t,error:r}}function rz(){let[e,{loading:t,error:r}]=Y(tp);return{changePassword:async(t,r)=>{try{let n=await e({variables:{input:{currentPassword:t,newPassword:r}}}),i=n.data?.changeMyPassword;return{status:i?.status||"ERROR",message:i?.message,messages:i?.message?[{text:i.message,type:i.status||"ERROR"}]:void 0,data:i?.data?{id:String(i.data)}:void 0}}catch(e){throw console.error("Change password error:",e),e}},loading:t,error:r}}function rG(){let[e,{loading:t,error:r}]=Y(ti);return{createPassword:async(t,r)=>{try{let n=await e({variables:{input:{identifier:t,newPassword:r}}});return n.data?.setInitialPassword}catch(e){throw console.error("Create password error:",e),e}},loading:t,error:r}}function rK(){let[e,{loading:t,error:r}]=Y(tm);return{deleteUserPassword:async t=>{try{let r=await e({variables:{input:{userId:t,revokeSessions:!0}}});return r.data?.adminTriggerPasswordReset}catch(e){throw console.error("Delete user password error:",e),e}},loading:t,error:r}}e.s(["useActivateUser",()=>rq,"useAdminCreateUser",()=>rV,"useAdminUpdateUser",()=>rU,"useChangePassword",()=>rz,"useClinicProfile",()=>rM,"useCreatePassword",()=>rG,"useDeactivateUser",()=>rB,"useDeleteUserPassword",()=>rK,"useLogin",()=>rk,"useRegister",()=>rF,"useSetInitialPassword",()=>rL,"useUpdateMyProfile",()=>rQ,"useUpdateUserRoles",()=>rj,"useUpsertClinicProfile",()=>r$,"useUsers",()=>rx],93264);let rH=e=>({id:e.id,name:e.name,insurancePolicyMode:e.insurancePolicyMode||ry.ALL,nursing:e.nursing??!1,supportRequests:e.supportRequests??!1,requestsProducts:e.requestsProducts??!1,insurancePolicies:(e.insurancePolicies||[]).map(e=>rA({id:e.id,insuranceName:e.insuranceName||"Unknown Insurance",acronym:e.acronym,defaultCoveragePercentage:e.defaultCoveragePercentage,supportedByClinic:e.supportedByClinic,iconUrl:e.iconUrl})),defaultProducts:(e.defaultProducts||[]).map(e=>rE({id:e.id,name:e.name,genericName:e.genericName,code:e.code||"",description:e.description||"",type:e.type,unit:e.unit,privateRhicPrice:e.privateRhicPrice,clinicPrice:e.clinicPrice,insuranceCoverages:(e.insuranceCoverages||[]).map(e=>({id:e.id,insuranceProvider:e.insuranceProvider?{id:e.insuranceProvider.id,insuranceName:e.insuranceProvider.insuranceName||"",acronym:e.insuranceProvider.acronym,defaultCoveragePercentage:e.insuranceProvider.defaultCoveragePercentage}:e.insurance?{id:e.insurance.id,insuranceName:e.insurance.name||"",acronym:e.insurance.acronym,defaultCoveragePercentage:e.insurance.coveragePercentage}:{id:"",insuranceName:""},cost:e.cost,covered:e.covered,requireMedicalAdvisor:e.requireMedicalAdvisor}))})),createdAt:"",updatedAt:""});function rY(e){let t={input:{page:e?.input?.page??0,size:e?.input?.size??200,name:e?.input?.name||void 0,supportRequests:e?.input?.supportRequests,requestsProducts:e?.input?.requestsProducts}},{data:r,loading:n,error:i,refetch:a}=eu(tZ,{variables:t,fetchPolicy:"cache-and-network",skip:e?.skip??!1});return{departments:(r?.departments?.data||[]).map(rH),loading:n||!1,error:i?.message||null,refetch:()=>a(t)}}function rX(){let[e,{loading:t,error:r}]=Y(e1);return{createDepartment:async(t,r)=>{let{data:n}=await e({variables:{input:{name:t,insuranceProviderIds:r?.insuranceProviderIds,defaultProductIds:r?.defaultProductIds,insurancePolicyMode:r?.insurancePolicyMode,nursing:r?.nursing,supportRequests:r?.supportRequests,requestsProducts:r?.requestsProducts}}}),i=n?.createDepartment;return{status:i?.status||"ERROR",message:i?.message,data:i?.data?rH(i.data):null}},loading:t,error:r?.message||null}}function rW(){let[e,{loading:t,error:r}]=Y(e3);return{updateDepartment:async(t,r)=>{let{data:n}=await e({variables:{departmentId:t,input:r}}),i=n?.updateDepartment;return{status:i?.status||"ERROR",message:i?.message,data:i?.data?rH(i.data):null}},loading:t,error:r?.message||null}}function rJ(){let[e,{loading:t,error:r}]=Y(e2);return{deleteDepartment:async t=>{let{data:r}=await e({variables:{id:t}}),n=r?.deleteDepartment;return{status:n?.status||"ERROR",message:n?.message}},loading:t,error:r?.message||null}}function rZ(){let[e,{loading:t,error:r}]=Y(e4);return{addDepartmentInsurance:async(t,r)=>{let{data:n}=await e({variables:{departmentId:t,insuranceId:r}}),i=n?.addDepartmentInsurance;return{status:i?.status||"ERROR",message:i?.message,data:i?.data?rH(i.data):null}},loading:t,error:r?.message||null}}function r0(){let[e,{loading:t,error:r}]=Y(e9);return{removeDepartmentInsurance:async(t,r)=>{let{data:n}=await e({variables:{departmentId:t,insuranceId:r}}),i=n?.removeDepartmentInsurance;return{status:i?.status||"ERROR",message:i?.message,data:i?.data?rH(i.data):null}},loading:t,error:r?.message||null}}function r1(){let[e,{loading:t,error:r}]=Y(e5);return{addDepartmentProduct:async(t,r)=>{let{data:n}=await e({variables:{departmentId:t,productId:r}}),i=n?.addDepartmentProduct;return{status:i?.status||"ERROR",message:i?.message,data:i?.data?rH(i.data):null}},loading:t,error:r?.message||null}}function r3(){let[e,{loading:t,error:r}]=Y(e8);return{removeDepartmentProduct:async(t,r)=>{let{data:n}=await e({variables:{departmentId:t,productId:r}}),i=n?.removeDepartmentProduct;return{status:i?.status||"ERROR",message:i?.message,data:i?.data?rH(i.data):null}},loading:t,error:r?.message||null}}e.s(["useAddDepartmentInsurance",()=>rZ,"useAddDepartmentProduct",()=>r1,"useCreateDepartment",()=>rX,"useDeleteDepartment",()=>rJ,"useDepartments",()=>rY,"useRemoveDepartmentInsurance",()=>r0,"useRemoveDepartmentProduct",()=>r3,"useUpdateDepartment",()=>rW],97756);var r2=e.i(71645);let r4=e=>{if(!e)return"unknown";let t=new Date(e);return Number.isNaN(t.getTime())?"unknown":t.toISOString()},r9=(e=[])=>{if(!Array.isArray(e)||0===e.length)return[];if(Array.isArray(e[0]?.measurements))return e.map((e,t)=>({id:String(e?.id||e?.createdAt||`group-${t}`),createdAt:r4(e?.createdAt),addedBy:rT(e?.addedBy)??null,measurements:(e?.measurements||[]).map((t,r)=>({id:String(t?.id||`${e?.id||e?.createdAt||"group"}-${r}`),measurementName:String(t?.measurementName||""),value:String(t?.value||""),unit:String(t?.unit||""),createdAt:t?.createdAt||e?.createdAt||""})).filter(e=>e.measurementName||e.value||e.unit)})).filter(e=>e.measurements.length>0).sort((e,t)=>"unknown"===e.createdAt?1:"unknown"===t.createdAt?-1:new Date(t.createdAt||0).getTime()-new Date(e.createdAt||0).getTime());let t=new Map;return e.forEach(e=>{let r=r4(e?.createdAt);t.has(r)||t.set(r,[]),t.get(r).push(e)}),Array.from(t.entries()).map(([e,t],r)=>({id:`group-${r}-${e}`,createdAt:e,measurements:t.map((t,r)=>({id:String(t?.id||`${e}-${r}`),measurementName:String(t?.measurementName||""),value:String(t?.value||""),unit:String(t?.unit||""),createdAt:t?.createdAt||e||""})),addedBy:rT(t[0]?.addedBy)??null})).sort((e,t)=>"unknown"===e.createdAt?1:"unknown"===t.createdAt?-1:new Date(t.createdAt||0).getTime()-new Date(e.createdAt||0).getTime())};function r5(e,t,r){let n=r?.fromDate&&r?.toDate?r.fromDate===r.toDate?r.fromDate:void 0:r?.fromDate||r?.toDate,{data:i,loading:a,error:s,refetch:o}=eu(t7,{variables:{input:{...r?.status?{status:r.status}:{},...r?.patientName?{patientName:r.patientName}:{},...n?{visitDate:n}:{},page:t??0,size:e??20}},fetchPolicy:"cache-and-network"}),u=s?.networkError?"network":s?.graphQLErrors?.length?"graphql":null,d=s?.graphQLErrors?.[0]?.message||s?.networkError?.message||s?.message||null;return{visits:(i?.visits?.data||[]).map(e=>{var t;let r,n=(r=e.patient.dateOfBirth?rD(e.patient):{id:(t=e.patient).id,firstName:t.firstName,lastName:t.lastName,patientIdentifier:t.patientIdentifier,dateOfBirth:"",gender:rv.OTHER,primaryPhoneNumber:t.primaryPhoneNumber,patientInsurances:[],createdAt:"",updatedAt:""},rS({...e,patient:r},{patientMapper:()=>r}));return n.vitalSigns=r9(e.vitalSigns||[]),n}),totalPages:i?.visits?.pagination?.totalPages||0,totalElements:i?.visits?.pagination?.total||0,loading:a,error:d,errorKind:u,refetch:o}}function r8(e=1,t){let{data:r,loading:n,error:i,refetch:a}=eu(rr,{variables:{days:e},fetchPolicy:"cache-and-network",skip:t?.skip}),s=r?.getDashboardStats?.data||null;return{stats:s?{totalVisits:Number(s.totalVisits||0),totalOpen:Number(s.totalOpen||0),totalCompleted:Number(s.totalCompleted||0),totalWaitingForBilling:Number(s.totalWaitingForBilling||0)}:null,loading:n,error:i?.message||null,refetch:a}}function r6(e){let{data:t,loading:r,error:n,refetch:i}=eu(t6,{variables:{id:e},skip:!e,fetchPolicy:"cache-and-network"}),a=t?.visit?.data;return{visit:(0,r2.useMemo)(()=>{if(!a)return;let e=rS(a);return e.vitalSigns=r9(a.vitalSigns||[]),e},[a]),loading:r,error:n?.message||null,refetch:i}}function r7(e,t,r){let{data:n,loading:i,error:a,refetch:s}=eu(rt,{variables:{visitId:e,departmentId:t},skip:!e||!t||r?.skip,fetchPolicy:"cache-and-network"});return{data:(0,r2.useMemo)(()=>{var e,t;return(e=n?.lastPatientDepartmentVisit?.data||null)?{lastVisit:e.lastVisit?rS(e.lastVisit):null,lastDepartmentVisit:(t=e.lastDepartmentVisit,t?.visitId&&t?.visitDepartment?{visitId:String(t.visitId),visitDepartment:rR(t.visitDepartment)}:null)}:null},[n]),loading:i,error:a?.message||null,refetch:s}}function ne(){let[e,{loading:t,error:r}]=Y(tb);return{createVisit:async t=>{try{let r=(t.departmentIds||[]).map(e=>({departmentId:e,products:[]})),n=await e({variables:{input:{patientId:t.patientId,linkedPatientInsuranceIds:t.insuranceIds||[],departments:r}}}),i=n?.data?.createVisit;return{status:i?.status||"ERROR",messages:i?.message?[{text:i.message,type:i.status||"ERROR"}]:void 0,data:i?.data?rS(i.data):void 0}}catch(e){throw console.error("Visit creation error:",e),e}},loading:t,error:r}}function nt(){let[e,{loading:t,error:r}]=Y(tI);return{addVisitVitalSigns:async(t,r)=>{try{let n=await e({variables:{input:{visitId:t,vitalSigns:r}}}),i=n.data?.addVisitVitalSigns;return{status:i?.status||"ERROR",message:i?.message,messages:i?.message?[{text:i.message,type:i.status||"ERROR"}]:void 0,data:i?.data?{...i.data,vitalSigns:r9(i.data.vitalSigns||[])}:void 0}}catch(e){throw console.error("Add visit vital signs error:",e),e}},loading:t,error:r}}function nr(){let[e,{loading:t,error:r}]=Y(tN);return{addDiagnosis:async(t,r,n)=>{try{let i=await e({variables:{input:{visitDepartmentId:t,diagnosisName:r,icd11Code:n||void 0}}});return i.data?.addDiagnosis}catch(e){throw console.error("Add diagnosis error:",e),e}},loading:t,error:r}}function nn(){let[e,{loading:t,error:r}]=Y(tE);return{addMedication:async(t,r,n)=>{try{let i=await e({variables:{input:{visitDepartmentId:t,medicationName:r,instructions:n}}});return i.data?.addMedication}catch(e){throw console.error("Add medication error:",e),e}},loading:t,error:r}}function ni(){let[e,{loading:t,error:r}]=Y(tR);return{removeProduct:async t=>{try{return(await e({variables:{visitDepartmentProductId:t}})).data.removeVisitDepartmentProduct}catch(e){throw console.error("Remove product error:",e),e}},loading:t,error:r}}function na(){let[e,{loading:t,error:r}]=Y(tT);return{addAction:async(t,r,n,i)=>{try{let a=await e({variables:{input:{visitId:t,departmentId:r,productId:n,quantity:i??1,status:"PENDING"}}}),s=a.data?.addVisitDepartmentProduct;return{status:s?.status||"ERROR",messages:s?.message?[{text:s.message,type:s.status||"ERROR"}]:void 0,data:s?.data?rR(s.data):void 0}}catch(e){throw console.error("Add action error:",e),e}},loading:t,error:r}}function ns(){let[e,{loading:t,error:r}]=Y(tA);return{addChildVisitDepartment:async t=>{try{let r=await e({variables:{input:{parentVisitDepartmentId:t.parentVisitDepartmentId,departmentId:t.departmentId,products:t.products.map(e=>({productId:e.productId,quantity:e.quantity})),processorId:t.processorId}}}),n=r.data?.addChildVisitDepartment;return{status:n?.status||"ERROR",messages:n?.message?[{text:n.message,type:n.status||"ERROR"}]:void 0,data:n?.data?rR(n.data):void 0}}catch(e){throw console.error("Add child visit department error:",e),e}},loading:t,error:r}}function no(){let[e,{loading:t,error:r}]=Y(tT);return{addConsumable:async(t,r,n,i)=>{try{let a=await e({variables:{input:{visitId:t,departmentId:r,productId:n,quantity:i??1,status:"PENDING"}}}),s=a.data?.addVisitDepartmentProduct;return{status:s?.status||"ERROR",messages:s?.message?[{text:s.message,type:s.status||"ERROR"}]:void 0,data:s?.data?rR(s.data):void 0}}catch(e){throw console.error("Add consumable error:",e),e}},loading:t,error:r}}function nu(){let[e,{loading:t,error:r}]=Y(tT);return{addProduct:async(t,r,n,i)=>{try{let a=await e({variables:{input:{visitId:t,departmentId:r,productId:n,quantity:i??1,status:"PENDING"}}}),s=a.data?.addVisitDepartmentProduct;return{status:s?.status||"ERROR",messages:s?.message?[{text:s.message,type:s.status||"ERROR"}]:void 0,data:s?.data?rR(s.data):void 0}}catch(e){throw console.error("Add product error:",e),e}},loading:t,error:r}}function nd(){let[e,{loading:t,error:r}]=Y(tP);return{updateDepartmentStatus:async(t,r)=>{try{return(await e({variables:{input:{visitDepartmentId:t,status:r}}})).data.updateVisitDepartmentStatus}catch(e){throw console.error("Update visit department status error:",e),e}},loading:t,error:r}}function nl(){let[e,{loading:t,error:r}]=Y(tD);return{addDepartmentToVisit:async(t,r,n)=>{try{let i=await e({variables:{visitId:t,departmentId:r,processorId:n||null},refetchQueries:["GetVisits","GetVisit"],awaitRefetchQueries:!0});return i.data?.addVisitDepartment}catch(e){throw console.error("Add department to visit error:",e),e}},loading:t,error:r}}function nc(){let[e,{loading:t,error:r}]=Y(t_);return{linkVisitInsurances:async(t,r)=>{try{let n=await e({variables:{visitId:t,insuranceIds:r}});return n.data?.linkVisitInsurances}catch(e){throw console.error("Link visit insurances error:",e),e}},loading:t,error:r}}function np(){let[e,{loading:t,error:r}]=Y(tO);return{unlinkVisitInsurances:async(t,r)=>{try{let n=await e({variables:{visitId:t,insuranceIds:r}});return n.data?.unlinkVisitInsurances}catch(e){throw console.error("Unlink visit insurances error:",e),e}},loading:t,error:r}}function nm(){let[e,{loading:t,error:r}]=Y(tC);return{updateQuantity:async(t,r)=>{try{let n=await e({variables:{input:{visitDepartmentProductId:t,quantity:parseFloat(r.toString())}}}),i=n.data?.updateVisitDepartmentProductQuantity;return{status:i?.status||"ERROR",message:i?.message,data:i?.data}}catch(e){throw console.error("Update product quantity error:",e),e}},loading:t,error:r}}function nh(){let[e,{loading:t,error:r}]=Y(tS);return{completeVisit:async t=>{try{return(await e({variables:{visitId:t}})).data.completeVisit}catch(e){throw console.error("Complete visit error:",e),e}},loading:t,error:r}}function nf(e,t){let{data:r,loading:n,error:i,refetch:a}=eu(rn,{variables:{visitId:e,visitDepartmentId:t},skip:!e||!t,fetchPolicy:"cache-and-network"});return{notes:r?.visitDepartmentNotes?.data||[],loading:n,error:i?.message||null,refetch:a}}function nv(){let[e,{loading:t,error:r}]=Y(tw);return{addVisitDepartmentNote:async(t,r,n,i=[])=>{try{let a=await e({variables:{input:{visitDepartmentId:t,content:r,noteType:n,targetUserId:i}}});return a.data?.addVisitDepartmentNote}catch(e){throw console.error("Add visit department note error:",e),e}},loading:t,error:r}}function ny(){let[e,{loading:t,error:r}]=Y(tk);return{markNotesViewed:async t=>{try{let r=await e({variables:{visitDepartmentId:t}});return r.data?.markVisitDepartmentNotesViewed}catch(e){throw console.error("Mark notes viewed error:",e),e}},loading:t,error:r}}e.s(["normalizeVisitVitalSigns",0,r9,"useAddActionToVisitDepartment",()=>na,"useAddChildVisitDepartment",()=>ns,"useAddConsumableToVisitDepartment",()=>no,"useAddDepartmentToVisit",()=>nl,"useAddDiagnosisToVisitDepartment",()=>nr,"useAddMedicationToVisitDepartment",()=>nn,"useAddProductToVisitDepartment",()=>nu,"useAddVisitDepartmentNote",()=>nv,"useAddVisitVitalSigns",()=>nt,"useCompleteVisit",()=>nh,"useCreateVisit",()=>ne,"useDashboardStats",()=>r8,"useLastPatientDepartmentVisit",()=>r7,"useLinkVisitInsurances",()=>nc,"useMarkVisitDepartmentNotesViewed",()=>ny,"useRemoveProductFromVisitDepartment",()=>ni,"useUnlinkVisitInsurances",()=>np,"useUpdateProductQuantity",()=>nm,"useUpdateVisitDepartmentStatus",()=>nd,"useVisit",()=>r6,"useVisitDepartmentNotes",()=>nf,"useVisits",()=>r5],17235),e.s([],7284);let ng=(e,t,r)=>{let n={id:r||"",firstName:"",dateOfBirth:"",gender:rv.OTHER,patientInsurances:[],createdAt:"",updatedAt:""};return r_({...e,insuranceProvider:{id:t,insuranceName:""}},n)};function nb(){let[e,{loading:t,error:r}]=Y(tv);return{createPatientInsurance:async t=>{try{let r=await e({variables:{input:{patientId:t.patientId,insuranceProviderId:t.insuranceProviderId,insuranceCardNumber:t.insuranceCardNumber,providingCompanyOrEmployer:t.providingCompanyOrEmployer,principalMember:!t.dominantMember?.firstName?.trim()&&!t.dominantMember?.lastName?.trim()&&!t.dominantMember?.phone?.trim(),principalMemberName:[t.dominantMember?.firstName,t.dominantMember?.lastName].filter(Boolean).join(" ")||null,principalMemberPhoneNumber:t.dominantMember?.phone?.trim()||null,validFrom:t.validFrom,validUntil:t.validUntil}}}),n=r.data?.createPatientInsurance,i=n?.data,a=t.insuranceProviderId;return{status:n?.status||"ERROR",message:n?.message,messages:n?.message?[{text:n.message,type:n.status||"ERROR"}]:void 0,data:i?ng(i,a,t.patientId):void 0}}catch(e){throw console.error("Create patient insurance error:",e),e}},loading:t,error:r}}function nI(){let[e,{loading:t,error:r}]=Y(ty);return{updatePatientInsurance:async(t,r)=>{try{let n=await e({variables:{patientInsuranceId:t,input:{patientId:r.patientId,insuranceProviderId:r.insuranceProviderId,insuranceCardNumber:r.insuranceCardNumber,providingCompanyOrEmployer:r.providingCompanyOrEmployer,principalMember:!r.dominantMember?.firstName?.trim()&&!r.dominantMember?.lastName?.trim()&&!r.dominantMember?.phone?.trim(),principalMemberName:[r.dominantMember?.firstName,r.dominantMember?.lastName].filter(Boolean).join(" ")||null,principalMemberPhoneNumber:r.dominantMember?.phone?.trim()||null,validFrom:r.validFrom,validUntil:r.validUntil}}}),i=n.data?.updatePatientInsurance,a=i?.data,s=r.insuranceProviderId;return{status:i?.status||"ERROR",message:i?.message,messages:i?.message?[{text:i.message,type:i.status||"ERROR"}]:void 0,data:a?ng(a,s,r.patientId):void 0}}catch(e){throw console.error("Update patient insurance error:",e),e}},loading:t,error:r}}function nA(e,t=0,r=20){let n=!e||0===Object.keys(e).length,{data:i,loading:a,error:s,refetch:o}=eu(t1,{variables:{input:{...e?.name?{name:e.name}:{},...e?.phoneNumber?{phoneNumber:e.phoneNumber}:{},...e?.age!=null?{age:e.age}:{},page:t,size:r}},fetchPolicy:"cache-and-network",skip:n}),u=(i?.searchPatients?.data||[]).map(e=>{let t;return t=rD(e),e.lastVisit,t});return{patients:u,loading:a,error:s,totalPages:i?.searchPatients?.pagination?.totalPages||0,totalElements:i?.searchPatients?.pagination?.total||0,refetch:o}}function nN(e){let{data:t,loading:r,error:n,refetch:i}=eu(t3,{variables:{patientId:e},skip:!e,fetchPolicy:"cache-and-network"}),a=t?.patient?.data,s=t?.patientInsurances?.data||[];return{patient:r2.default.useMemo(()=>{let e;if(!a)return;let t=rD(a);return e=t={...t,patientInsurances:s.map(e=>r_(e,t))},a.lastVisit,e},[a,s]),loading:r,error:n,refetch:i}}function nE(){let[e,{loading:t,error:r}]=Y(tf);return{registerPatient:async t=>{try{let r={firstName:t.firstName,middleName:t.middleName||null,lastName:t.lastName||null,dateOfBirth:t.dateOfBirth,gender:"M"===t.gender?"MALE":"F"===t.gender?"FEMALE":t.gender||null,primaryPhoneNumber:t.contactInfo?.phone||null,alternativePhone:null,village:t.contactInfo?.address?.street||null,city:t.contactInfo?.address?.sector||null,district:t.contactInfo?.address?.district||null,postalAddress:t.contactInfo?.address?.country||null,nationalIdNumber:t.nationalIdNumber||null,passportNumber:null,emergencyContactName:t.emergencyContact?.name||null,emergencyContactRelationship:t.emergencyContact?.relation||null,emergencyContactPhoneNumber:t.emergencyContact?.phone||null};if(t.insurances&&t.insurances.length>0){let e=new Date,n=e.toISOString().slice(0,10),i=new Date(e.getFullYear()+1,e.getMonth(),e.getDate()).toISOString().slice(0,10);r.insurances=t.insurances.filter(e=>e?.insuranceId&&e?.insuranceCardNumber&&e?.providingCompanyOrEmployer).map(e=>{var t;let r,a,s,o;return{insuranceProviderId:String(e.insuranceId),insuranceCardNumber:e.insuranceCardNumber,providingCompanyOrEmployer:e.providingCompanyOrEmployer,...(t=e.dominantMember,r=t?.firstName?.trim()||"",a=t?.lastName?.trim()||"",s=t?.phone?.trim()||"",{principalMember:!(o=!!(r||a||s)),principalMemberName:o&&[r,a].filter(Boolean).join(" ")||null,principalMemberPhoneNumber:o&&s||null}),validFrom:n,validUntil:i}})}let n=await e({variables:{input:r}}),i=n?.data?.createPatient,a=i?.data;return{status:i?.status||"ERROR",message:i?.message,messages:i?.message?[{text:i.message,type:i.status||"ERROR"}]:void 0,data:a?rS({id:a.id,visitDate:a.visitDate,status:a.status,patient:a.patient,linkedInsurances:a.linkedInsurances||[],departments:[],vitalSigns:[]}):void 0}}catch(e){throw console.error("Patient registration error:",e),e}},loading:t,error:r}}function nT(){let[e,{loading:t,error:r}]=Y(tg),[n]=Y(tv),[i]=Y(ty);return{updatePatient:async(t,r)=>{try{let n={firstName:r.firstName,middleName:r.middleName??null,lastName:r.lastName??null,dateOfBirth:r.dateOfBirth,gender:r.gender??null,primaryPhoneNumber:r.primaryPhoneNumber??null,alternativePhone:r.alternativePhone??null,village:r.village??null,city:r.city??null,district:r.district??null,postalAddress:r.postalAddress??null,nationalIdNumber:r.nationalIdNumber??null,passportNumber:r.passportNumber??null,emergencyContactName:r.emergencyContactName??null,emergencyContactRelationship:r.emergencyContactRelationship??null,emergencyContactPhoneNumber:r.emergencyContactPhoneNumber??null},i=(await e({variables:{patientId:t,input:n}})).data.updatePatient,a=[];return{status:i?.status||"ERROR",message:i?.message,messages:i?.message?[{text:i.message,type:i.status||"ERROR"}]:void 0,data:i?.data?{...rD(i.data),patientInsurances:a.length>0?a:rD(i.data).patientInsurances}:void 0}}catch(e){throw console.error("Patient update error:",e),e}},loading:t,error:r}}e.s(["useCreatePatientInsurance",()=>nb,"usePatient",()=>nN,"usePatients",()=>nA,"useRegisterPatient",()=>nE,"useUpdatePatient",()=>nT,"useUpdatePatientInsurance",()=>nI],8345);let nP=eZ`
  query GetInsurances($input: SearchInsuranceProvidersInput) {
    insuranceProviders(input: $input) {
      status
      message
      data {
        id
        insuranceName
        acronym
        defaultCoveragePercentage
        supportedByClinic
        iconUrl
      }
    }
  }
`;function nD(e){let t={page:e?.page??0,size:e?.size??200,...e?.query?{query:e.query}:{}};e&&"supportedByClinic"in e?"boolean"==typeof e.supportedByClinic&&(t.supportedByClinic=e.supportedByClinic):t.supportedByClinic=!0;let{data:r,loading:n,error:i,refetch:a}=eu(nP,{variables:{input:t},fetchPolicy:"cache-and-network"});return{insurances:(r?.insuranceProviders?.data||[]).map(e=>rA(e)),loading:n||!1,error:i?.message||null,refetch:a}}function n_(e){let{data:t,loading:r,error:n}=eu(nP,{variables:{input:{query:e||void 0,supportedByClinic:!0,page:0,size:20}},fetchPolicy:"cache-and-network",skip:!e||e.length<2});return{insurances:(t?.insuranceProviders?.data||[]).map(e=>rA(e)),loading:r,error:n?.message||null}}function nO(){let[e,{loading:t,error:r}]=Y(tL);return{createInsuranceProvider:async t=>{try{let{data:r}=await e({variables:{input:t}}),n=r?.createInsuranceProvider,i=n?.data;return{status:n?.status||"ERROR",message:n?.message,data:i?{id:i.id,insuranceName:i.insuranceName,acronym:i.acronym||void 0,defaultCoveragePercentage:i.defaultCoveragePercentage,supportedByClinic:i.supportedByClinic,iconUrl:i.iconUrl||void 0}:void 0}}catch(e){throw console.error("Create insurance provider error:",e),e}},loading:t,error:r?.message||null}}function nC(){let[e,{loading:t,error:r}]=Y(tF);return{updateInsuranceProvider:async(t,r)=>{try{let{data:n}=await e({variables:{insuranceProviderId:t,input:r}}),i=n?.updateInsuranceProvider,a=i?.data;return{status:i?.status||"ERROR",message:i?.message,data:a?{id:a.id,insuranceName:a.insuranceName,acronym:a.acronym||void 0,defaultCoveragePercentage:a.defaultCoveragePercentage,supportedByClinic:a.supportedByClinic,iconUrl:a.iconUrl||void 0}:void 0}}catch(e){throw console.error("Update insurance provider error:",e),e}},loading:t,error:r?.message||null}}function nR(){let[e,{loading:t,error:r}]=Y(tM);return{deleteInsuranceProvider:async t=>{try{let{data:r}=await e({variables:{insuranceProviderId:t}}),n=r?.deleteInsuranceProvider;return{status:n?.status||"ERROR",message:n?.message,data:null}}catch(e){throw console.error("Delete insurance provider error:",e),e}},loading:t,error:r?.message||null}}function nS(){}e.s(["useCreateInsuranceProvider",()=>nO,"useDeleteInsuranceProvider",()=>nR,"useInsuranceSearch",()=>n_,"useInsurances",()=>nD,"useUpdateInsuranceProvider",()=>nC],95574);var nw=["refetch","reobserve","fetchMore","updateQuery","startPolling","stopPolling","subscribeToMore"],nk=["initialFetchPolicy","onCompleted","onError","defaultOptions","partialRefetch","canonizeResults"],nL=["query","ssr","client","fetchPolicy","nextFetchPolicy","refetchWritePolicy","errorPolicy","pollInterval","notifyOnNetworkStatusChange","returnPartialData","skipPollAttempt"];function nF(e,t){if(!1!==globalThis.__DEV__){var r,n=t||{};H(n,"canonizeResults","useLazyQuery"),H(n,"variables","useLazyQuery","Pass all `variables` to the returned `execute` function instead."),H(n,"context","useLazyQuery","Pass `context` to the returned `execute` function instead."),H(n,"onCompleted","useLazyQuery","If your `onCompleted` callback sets local state, switch to use derived state using `data` returned from the hook instead. Use `useEffect` to perform side-effects as a result of updates to `data`."),H(n,"onError","useLazyQuery","If your `onError` callback sets local state, switch to use derived state using `data`, `error` or `errors` returned from the hook instead. Use `useEffect` if you need to perform side-effects as a result of updates to `data`, `error` or `errors`."),H(n,"defaultOptions","useLazyQuery","Pass the options directly to the hook instead."),H(n,"initialFetchPolicy","useLazyQuery","Use the `fetchPolicy` option instead."),H(n,"partialRefetch","useLazyQuery")}var i=w.useRef(void 0),a=w.useRef(void 0),s=w.useRef(void 0),o=(0,M.mergeOptions)(t,i.current||{}),u=null!=(r=null==o?void 0:o.query)?r:e;a.current=t,s.current=u;var d=(0,F.__assign)((0,F.__assign)({},o),{skip:!i.current}),l=el(u,d),c=l.obsQueryFields,p=l.result,m=l.client,h=l.resultData,f=l.observable,v=l.onQueryExecuted,y=f.options.initialFetchPolicy||ef(d.defaultOptions,m.defaultOptions),g=w.useReducer(function(e){return e+1},0)[1],b=w.useMemo(function(){for(var e={},t=function(t){var r=c[t];e[t]=function(){return!1!==globalThis.__DEV__&&"reobserve"===t&&!1!==globalThis.__DEV__&&S.invariant.warn(79),i.current||(i.current=Object.create(null),g()),r.apply(this,arguments)}},r=0;r<nw.length;r++)t(nw[r]);return e},[g,c]),I=!!i.current,A=w.useMemo(function(){return(0,F.__assign)((0,F.__assign)((0,F.__assign)({},p),b),{called:I})},[p,b,I]),N=(R||(R=w.createContext(null)),w.useCallback(function(){var e=console.error;try{return console.error=nS,w.useContext(R),!0}catch(e){return!1}finally{console.error=e}},[])),E=w.useRef(new Set),T=w.useCallback(function(e){if(!1!==globalThis.__DEV__){N()&&!1!==globalThis.__DEV__&&S.invariant.warn(80);for(var t,r,n,o,d,l,c,p,g=0;g<nk.length;g++){var I=nk[g];E.current.has(I)||((0,q.warnRemovedOption)(e||{},I,"useLazyQuery.execute"),E.current.add(I))}for(var A=0;A<nL.length;A++){var T=nL[A];E.current.has(T)||((0,q.warnRemovedOption)(e||{},T,"useLazyQuery.execute","Please pass the option to the `useLazyQuery` hook instead."),E.current.add(T))}}i.current=e?(0,F.__assign)((0,F.__assign)({},e),{fetchPolicy:e.fetchPolicy||y}):{fetchPolicy:y};var P=(0,M.mergeOptions)(a.current,(0,F.__assign)({query:s.current},i.current)),D=(t=h,r=f,n=m,o=u,d=(0,F.__assign)((0,F.__assign)({},P),{skip:!1}),l=v,c=ec(n,d.query||o,d,!1)(r),p=r.reobserveAsConcast(ep(r,n,d,c)),l(c),new Promise(function(e){var i;p.subscribe({next:function(e){i=e},error:function(){e(ev(r.getCurrentResult(),t.previousData,r,n))},complete:function(){e(ev(r.maskResult(i),t.previousData,r,n))}})})).then(function(e){return Object.assign(e,b)});return D.catch(function(){}),D},[N,m,u,b,y,f,h,v]),P=w.useRef(T);return K(function(){P.current=T}),[w.useCallback(function(){for(var e=[],t=0;t<arguments.length;t++)e[t]=arguments[t];return P.current.apply(P,e)},[]),A]}function nM(e){return{id:e.id,visitDepartmentProductId:e.visitDepartmentProductId,productId:e.productId,productName:e.productName,unitPriceSnapshot:Number(e.unitPriceSnapshot??0),quantitySnapshot:Number(e.quantitySnapshot??0),insuranceCoveredAmount:Number(e.insuranceCoveredAmount??0),patientPayableAmount:Number(e.patientPayableAmount??0),createdAt:e.createdAt||"",updatedAt:e.updatedAt||""}}function n$(e){return{id:e.id,patientInsurance:null,status:e.status,totalAmount:Number(e.totalAmount??0),insuranceCoveredAmount:Number(e.insuranceCoveredAmount??0),patientPayableAmount:Number(e.patientPayableAmount??0),paidAmount:Number(e.paidAmount??0),outstandingAmount:Number(e.outstandingAmount??0),items:(e.items||[]).map(nM),createdAt:e.createdAt||"",updatedAt:e.updatedAt||""}}function nx(e){return{id:e.id,visitDepartment:function(e=""){return{id:e,department:{id:"",name:"",insurancePolicyMode:ry.ALL,insurancePolicies:[],defaultProducts:[],nursing:!1,supportRequests:!1,requestsProducts:!1,createdAt:"",updatedAt:""},status:rb.PENDING,encounterType:rI.OUTPATIENT,processors:[],childVisitDepartments:[],products:[],preInstructions:[],createdAt:"",updatedAt:""}}(e.visitDepartment?.id||e.id),status:e.status,totalAmount:Number(e.totalAmount??0),insuranceCoveredAmount:Number(e.insuranceCoveredAmount??0),patientPayableAmount:Number(e.patientPayableAmount??0),paidAmount:Number(e.paidAmount??0),outstandingAmount:Number(e.outstandingAmount??0),payments:[],insuranceBillings:(e.insuranceBillings||[]).map(n$),createdAt:e.createdAt||"",updatedAt:e.updatedAt||""}}function nV(e){return{id:e.id,visitId:e.visitId,departments:(e.departments||[]).map(nx),createdAt:e.createdAt,updatedAt:e.updatedAt}}function nU(e){return e?(e.departments||[]).flatMap(e=>e.insuranceBillings||[]):[]}function nq(e){let t=nU(e),r=t.reduce((e,t)=>e+Number(t.totalAmount||0),0),n=t.reduce((e,t)=>e+Number(t.insuranceCoveredAmount||0),0),i=t.reduce((e,t)=>e+Number(t.patientPayableAmount||0),0),a=t.reduce((e,t)=>e+Number(t.paidAmount||0),0);return{totalAmount:r,insuranceCoveredAmount:n,patientPayableAmount:i,paidAmount:a,outstandingAmount:t.reduce((e,t)=>e+Number(t.outstandingAmount||0),0)||Math.max(0,r-a)}}function nB(e,t){return nU(e).flatMap(e=>e.items||[]).some(e=>e.visitDepartmentProductId===t)}function nj(e){let{data:t,loading:r,error:n,refetch:i}=eu(ri,{variables:{visitId:e},skip:!e,fetchPolicy:"cache-and-network"}),a=t?.visitBilling?.data;return{visitBilling:a?nV(a):void 0,loading:r,error:n,refetch:i}}function nQ(){let[e,{loading:t,error:r}]=Y(t$);return{createBill:async t=>{try{let r=await e({variables:{input:t}}),n=r?.data?.billVisit;return{status:n?.status||"ERROR",message:n?.message,data:n?.data?nV(n.data):void 0}}catch(e){throw console.error("Create bill error:",e),e}},loading:t,error:r}}function nz(){let[e,{loading:t,error:r}]=Y(tx);return{editBill:async t=>{let r={visitId:t.visitId,departments:t.departments.map(e=>({visitDepartmentId:e.visitDepartmentId,addedProducts:e.addedProducts,removedProductIds:e.removedProductIds,updatedProducts:e.updatedProducts,billProducts:e.billProducts,payments:e.payments,note:t.notes||void 0}))};try{let t=await e({variables:{input:r}}),n=t?.data?.editBillVisit;return{status:n?.status||"ERROR",message:n?.message,data:n?.data?nV(n.data):void 0}}catch(e){throw console.error("Edit bill error:",e),e}},loading:t,error:r}}function nG(){let[e,{loading:t,error:r}]=Y(tV);return{generateInvoice:async t=>{try{let r=await e({variables:{departmentInsuranceBillingId:t}});return r?.data?.generateInvoice}catch(e){throw console.error("Generate invoice error:",e),e}},loading:t,error:r}}e.s(["useLazyQuery",()=>nF],63489),e.s(["getVisitBillingTotals",()=>nq,"isVisitDepartmentProductBilled",()=>nB,"mapGqlVisitBilling",()=>nV],88005),e.s(["useCreateBill",()=>nQ,"useEditBill",()=>nz,"useGenerateInvoice",()=>nG,"useGetVisitBilling",()=>nj],35595);let nK=(e,t)=>{var r;return{id:e?.id||`field_${Date.now()}_${t}`,label:e?.label||"Untitled",type:e?.type||"text",placeholder:e?.placeholder||void 0,required:!!e?.required,hideLabel:!!e?.hideLabel,boldLabel:!!e?.boldLabel,centerLabel:!!e?.centerLabel,italicLabel:!!e?.italicLabel,underlineLabel:!!e?.underlineLabel,options:Array.isArray(e?.options)?e.options.filter(Boolean):void 0,tableConfig:e?.tableConfig?{mode:"DYNAMIC"===String((r=e.tableConfig.mode)||"").toUpperCase()||"variableRows"===r||"variableColumns"===r?"DYNAMIC":"STATIC",rows:Number(e.tableConfig.rows)||3,columns:Number(e.tableConfig.columns)||3,headerPlacement:e.tableConfig.headerPlacement||"none",columnHeaders:Array.isArray(e.tableConfig.columnHeaders)?e.tableConfig.columnHeaders:[],rowHeaders:Array.isArray(e.tableConfig.rowHeaders)?e.tableConfig.rowHeaders:[]}:void 0,labRecordConfig:e?.labRecordConfig?{layout:"result"===e.labRecordConfig.layout?"result":"valueUnit",rows:Array.isArray(e.labRecordConfig.rows)?e.labRecordConfig.rows.map((e,t)=>({id:e?.id||`lab_row_${Date.now()}_${t}`,name:e?.name||`Row ${t+1}`,unitMode:e?.unitMode==="none"?"none":"dropdown",unitOptions:Array.isArray(e?.unitOptions)?e.unitOptions.filter(Boolean):[],defaultUnit:e?.defaultUnit||void 0,resultOptions:Array.isArray(e?.resultOptions)?e.resultOptions.filter(Boolean):[]})):[]}:void 0,conditionalRendering:e?.conditionalRendering?{dependsOn:e.conditionalRendering.dependsOn,condition:e.conditionalRendering.condition,value:e.conditionalRendering.value||void 0,itemType:e.conditionalRendering.itemType||void 0}:void 0,order:"number"==typeof e?.order?e.order:t}},nH=e=>({id:String(e?.id||""),departmentId:String(e?.departmentId||""),title:e?.title||"",description:e?.description||"",status:e?.status==="FINAL"?"FINAL":"DRAFT",version:String(e?.version||""),fields:Array.isArray(e?.fields)?e.fields.map((e,t)=>nK(e,t)):[],sections:Array.isArray(e?.sections)?e.sections.map((e,t)=>({id:e?.id||`section_${Date.now()}_${t}`,title:e?.title||"Untitled Section",boldTitle:!!e?.boldTitle,italicTitle:!!e?.italicTitle,underlineTitle:!!e?.underlineTitle,centerTitle:!!e?.centerTitle,columns:e?.columns===1||e?.columns===2||e?.columns===3||e?.columns===4?e.columns:2,order:"number"==typeof e?.order?e.order:t,fields:Array.isArray(e?.fields)?e.fields.map((e,t)=>nK(e,t)):[]})):[],actions:Array.isArray(e?.actions)?e.actions.map((e,t)=>({id:e?.id||`action_${Date.now()}_${t}`,name:e?.name||"Unnamed item",type:e?.type==="consumable"?"consumable":"action",quantity:Number(e?.quantity)||1,price:Number(e?.price)||0,isQuantifiable:e?.isQuantifiable!==!1,backendId:e?.backendId?String(e.backendId):void 0})):[],createdAt:String(e?.createdAt||""),updatedAt:String(e?.updatedAt||"")});function nY(e){let[t,{loading:r,error:n,data:i}]=nF(rs,{fetchPolicy:"network-only"}),a=r2.default.useMemo(()=>(i?.getForms?.data||[]).map(e=>nH(e)),[i]),s=r2.default.useCallback(r=>{let{fetchPolicy:n,...i}=r||{},a=r?.variables?.departmentId||e;return a?t({...i,variables:{...i.variables||{},departmentId:a}}):Promise.resolve(void 0)},[e,t]);return{forms:a,loading:r,error:n?.message||null,loadForms:s}}function nX(e,t){let[r,{loading:n,error:i,data:a}]=nF(ro,{fetchPolicy:"network-only"}),s=r2.default.useMemo(()=>{let e=a?.getForm?.data;return e?nH(e):null},[a]),o=r2.default.useCallback(n=>{let{fetchPolicy:i,...a}=n||{},s=n?.variables?.departmentId||e,o=n?.variables?.formId||t;return s&&o?r({...a,variables:{...a.variables||{},departmentId:s,formId:o}}):Promise.resolve(void 0)},[e,t,r]);return{form:s,loading:n,error:i?.message||null,loadForm:o}}function nW(e,t){let[r,{loading:n,error:i,data:a}]=nF(ru,{fetchPolicy:"network-only"}),s=r2.default.useMemo(()=>(a?.getFormVersionHistory?.data||[]).map(e=>nH(e)),[a]),o=r2.default.useCallback(n=>{let{fetchPolicy:i,...a}=n||{},s=n?.variables?.departmentId||e,o=n?.variables?.formId||t;return s&&o?r({...a,variables:{...a.variables||{},departmentId:s,formId:o}}):Promise.resolve(void 0)},[e,t,r]);return{versions:s,loading:n,error:i?.message||null,loadVersionHistory:o}}function nJ(){let[e,{loading:t,error:r}]=Y(tU);return{createForm:async(t,r)=>{try{let n=await e({variables:{departmentId:t,input:{title:r.title,description:r.description,fields:r.fields?.map(e=>({id:e.id,label:e.label,type:e.type,placeholder:e.placeholder,required:e.required,order:e.order,hideLabel:e.hideLabel,boldLabel:e.boldLabel,italicLabel:e.italicLabel,underlineLabel:e.underlineLabel,centerLabel:e.centerLabel,options:e.options,tableConfig:e.tableConfig,conditionalRendering:e.conditionalRendering}))||[],sections:r.sections?.map(e=>({id:e.id,title:e.title,boldTitle:e.boldTitle,italicTitle:e.italicTitle,underlineTitle:e.underlineTitle,centerTitle:e.centerTitle,columns:e.columns,order:e.order,fields:e.fields.map(e=>({id:e.id,label:e.label,type:e.type,placeholder:e.placeholder,required:e.required,order:e.order,hideLabel:e.hideLabel,boldLabel:e.boldLabel,italicLabel:e.italicLabel,underlineLabel:e.underlineLabel,centerLabel:e.centerLabel,options:e.options,tableConfig:e.tableConfig,conditionalRendering:e.conditionalRendering}))}))||[],actions:r.actions?.map(e=>({id:e.id,name:e.name,type:e.type,quantity:e.quantity,price:e.price,isQuantifiable:e.isQuantifiable,backendId:e.backendId}))||[]}}}),i=n.data?.createForm?.data;return i?nH(i):null}catch(e){throw console.error("Create form error:",e),e}},loading:t,error:r?.message||null}}function nZ(){let[e,{loading:t,error:r}]=Y(tq);return{updateForm:async(t,r,n)=>{try{let i=await e({variables:{departmentId:t,formId:r,input:{title:n.title,description:n.description,fields:n.fields?.map(e=>({id:e.id,label:e.label,type:e.type,placeholder:e.placeholder,required:e.required,order:e.order,hideLabel:e.hideLabel,boldLabel:e.boldLabel,italicLabel:e.italicLabel,underlineLabel:e.underlineLabel,centerLabel:e.centerLabel,options:e.options,tableConfig:e.tableConfig,conditionalRendering:e.conditionalRendering}))||[],sections:n.sections?.map(e=>({id:e.id,title:e.title,boldTitle:e.boldTitle,italicTitle:e.italicTitle,underlineTitle:e.underlineTitle,centerTitle:e.centerTitle,columns:e.columns,order:e.order,fields:e.fields.map(e=>({id:e.id,label:e.label,type:e.type,placeholder:e.placeholder,required:e.required,order:e.order,hideLabel:e.hideLabel,boldLabel:e.boldLabel,italicLabel:e.italicLabel,underlineLabel:e.underlineLabel,centerLabel:e.centerLabel,options:e.options,tableConfig:e.tableConfig,conditionalRendering:e.conditionalRendering}))}))||[],actions:n.actions?.map(e=>({id:e.id,name:e.name,type:e.type,quantity:e.quantity,price:e.price,isQuantifiable:e.isQuantifiable,backendId:e.backendId}))||[]}}}),a=i.data?.updateForm?.data;return a?nH(a):null}catch(e){throw console.error("Update form error:",e),e}},loading:t,error:r?.message||null}}function n0(){let[e,{loading:t,error:r}]=Y(tB);return{finalizeForm:async(t,r)=>{try{let n=await e({variables:{departmentId:t,formId:r}}),i=n.data?.finalizeForm?.data;return i?nH(i):null}catch(e){throw console.error("Finalize form error:",e),e}},loading:t,error:r?.message||null}}function n1(){let{data:e,loading:t,error:r,refetch:n}=eu(t0,{variables:{input:{page:0,size:200}},fetchPolicy:"cache-and-network"});return{products:e?.products?.data||[],loading:t,error:r?.message||null,refetch:()=>n({input:{page:0,size:200}})}}function n3(e){let t=e?.size??30,r=e?.type&&"ALL"!==e.type?e.type:void 0,n=e?.name,{data:i,loading:a,error:s,fetchMore:o,refetch:u}=eu(t0,{variables:{input:{name:n||void 0,type:r,page:0,size:t}},fetchPolicy:"cache-and-network"}),d=i?.products?.data||[],l=i?.products?.pagination,c=!!(l&&"number"==typeof l.currentPage&&"number"==typeof l.totalPages&&l.currentPage+1<l.totalPages),p=async()=>{if(!c||!l||a)return!1;let e=l.currentPage+1;return await o({variables:{input:{name:n||void 0,type:r,page:e,size:t}},updateQuery:(e,{fetchMoreResult:t})=>{if(!t?.products)return e;let r=e?.products?.data||[],n=t.products.data||[],i=[...r];for(let e of n)i.some(t=>String(t.id)===String(e.id))||i.push(e);return{...t,products:{...t.products,data:i}}}}),!0};return{products:d,loading:a,error:s?.message||null,hasMore:c,loadMore:p,refresh:()=>u({input:{name:n||void 0,type:r,page:0,size:t}}),pagination:l}}function n2(e,t){let r=t?.size??20,n=t?.type&&"ALL"!==t.type?t.type:void 0,{data:i,loading:a,error:s,fetchMore:o,refetch:u}=eu(t0,{variables:{input:{name:e||void 0,type:n,page:0,size:r}},fetchPolicy:"cache-and-network",skip:!e||e.length<2}),d=i?.products?.data||[],l=i?.products?.pagination,c=!!(l&&"number"==typeof l.currentPage&&"number"==typeof l.totalPages&&l.currentPage+1<l.totalPages),p=async()=>{if(!c||!l)return!1;let t=l.currentPage+1;return await o({variables:{input:{name:e||void 0,type:n,page:t,size:r}},updateQuery:(e,{fetchMoreResult:t})=>{if(!t?.products)return e;let r=e?.products?.data||[],n=t.products.data||[],i=[...r];for(let e of n)i.some(t=>String(t.id)===String(e.id))||i.push(e);return{...t,products:{...t.products,data:i}}}}),!0};return{products:d,loading:a,error:s?.message||null,hasMore:c,loadMore:p,refresh:()=>u({input:{name:e||void 0,type:n,page:0,size:r}}),pagination:l}}function n4(){let[e,{loading:t,error:r}]=Y(e6);return{createProduct:async t=>{try{let{data:r}=await e({variables:{input:t}}),n=r?.createProduct;return{status:n?.status||"ERROR",message:n?.message,data:n?.data||void 0}}catch(e){throw console.error("Create product error:",e),e}},loading:t,error:r?.message||null}}function n9(){let[e,{loading:t,error:r}]=Y(e7);return{updateProduct:async(t,r)=>{try{let{data:n}=await e({variables:{productId:t,input:r}}),i=n?.updateProduct;return{status:i?.status||"ERROR",message:i?.message,data:i?.data||void 0}}catch(e){throw console.error("Update product error:",e),e}},loading:t,error:r?.message||null}}function n5(){let[e,{loading:t,error:r}]=Y(te);return{deleteProduct:async t=>{try{let{data:r}=await e({variables:{productId:t}}),n=r?.deleteProduct;return{status:n?.status||"ERROR",message:n?.message}}catch(e){throw console.error("Delete product error:",e),e}},loading:t,error:r?.message||null}}function n8(){let[e,{loading:t,error:r}]=Y(tt);return{addCoverage:async(t,r,n)=>{try{let{data:i}=await e({variables:{productId:t,input:{insuranceProviderId:r,cost:n}}}),a=i?.addProductInsuranceCoverage;return{status:a?.status||"ERROR",message:a?.message,data:a?.data||void 0}}catch(e){throw console.error("Add product coverage error:",e),e}},loading:t,error:r?.message||null}}function n6(){let[e,{loading:t,error:r}]=Y(tr);return{removeCoverage:async t=>{try{let{data:r}=await e({variables:{productInsuranceCoverageId:t}}),n=r?.removeProductInsuranceCoverage;return{status:n?.status||"ERROR",message:n?.message}}catch(e){throw console.error("Remove product coverage error:",e),e}},loading:t,error:r?.message||null}}e.s(["useCreateForm",()=>nJ,"useFinalizeForm",()=>n0,"useForm",()=>nX,"useFormVersionHistory",()=>nW,"useForms",()=>nY,"useUpdateForm",()=>nZ],20495),e.s([],41209),e.s(["useAddProductInsuranceCoverage",()=>n8,"useCreateProduct",()=>n4,"useDeleteProduct",()=>n5,"useProductSearch",()=>n2,"useProducts",()=>n1,"useProductsPaginated",()=>n3,"useRemoveProductInsuranceCoverage",()=>n6,"useUpdateProduct",()=>n9],45610),e.s([],98264);let n7=eZ`
  query SearchWorkers(
    $name: String
    $role: RoleName
    $activeOnly: Boolean
    $departmentId: ID
  ) {
    searchWorkers(
      name: $name
      role: $role
      activeOnly: $activeOnly
      departmentId: $departmentId
    ) {
      status
      message
      data {
        id
        firstName
        lastName
        roles
        departments {
          id
          name
        }
      }
    }
  }
`;function ie(e){let{data:t,loading:r,error:n,refetch:i}=eu(n7,{variables:e,skip:!e?.name||String(e.name).trim().length<2,fetchPolicy:"network-only"});return{workers:(0,r2.useMemo)(()=>{let e=t?.searchWorkers?.data;return Array.isArray(e)?e:[]},[t]),loading:r,error:n,refetch:i}}e.s(["useSearchWorkers",()=>ie],23899),e.s([],4306)},7670,e=>{"use strict";function t(){for(var e,t,r=0,n="",i=arguments.length;r<i;r++)(e=arguments[r])&&(t=function e(t){var r,n,i="";if("string"==typeof t||"number"==typeof t)i+=t;else if("object"==typeof t)if(Array.isArray(t)){var a=t.length;for(r=0;r<a;r++)t[r]&&(n=e(t[r]))&&(i&&(i+=" "),i+=n)}else for(n in t)t[n]&&(i&&(i+=" "),i+=n);return i}(e))&&(n&&(n+=" "),n+=t);return n}e.s(["clsx",()=>t,"default",0,t])}]);