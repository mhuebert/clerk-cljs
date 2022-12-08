# clerk-cljs

Macro for including cljs in [clerk](http://clerk.vision) notebooks.

## Instructions

Include an alias in `deps.edn` which includes: 

1. Path(s) to your Clerk notebooks,
2. Clerk and its render module as dependencies,
3. This library.

The following configuration assumes that you've put your notebooks under `src/notebooks`.
It includes a shadow-cljs build called `:clerk`, which outputs to `public/clerk`, and starts a dev server on port `8008`
- 

### deps.edn

Add a `:clerk` alias including this library as well as Clerk and its render module.

```clj
:clerk {:extra-paths ["src/notebooks"]
        :extra-deps {io.github.mhuebert/clerk-cljs {:git/sha "XX"}
                     io.github.nextjournal/clerk {:git/sha "XX"}
                     io.github.nextjournal/clerk.render {:git/url "https://github.com/nextjournal/clerk.git"
                                                         :git/sha "XX"
                                                         :deps/root "render"}}}
```

### shadow-cljs.edn 

Create a shadow-cljs build with `:esm` target, including your notebooks as module entries. The notebooks should be `.cljc` files.

```clj 
;; in your shadow-cljs.edn :builds map:
:clerk
  {:target :esm
   :output-dir "public/clerk"
   :modules {:clerk {:entries [my.app.notebook-1
                               my.app.notebook-2]}}
   :devtools {:http-port 8008
              :http-root "public"}}
```

### user.clj 

After starting a shadow-cljs server, you can run the following from a repl, customizing options to suit your project:

```clj 
(require '[nextjournal.clerk :as clerk]
         '[nextjournal.clerk.config :as config]
         '[shadow.cljs.devtools.api :as shadow])

(defn start []
  (shadow/watch :clerk)
  (swap! config/!resource->url merge {"/js/viewer.js" "http://localhost:8008/clerk/clerk.js"})
  (clerk/serve! {:browse? true
                 :out-path "public"
                 :watch-paths ["src/notebooks"]}))
                 
(comment 
 (start))                 
```

