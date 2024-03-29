(ns mhuebert.clerk-cljs
  (:require [applied-science.js-interop :as j]
            [clojure.walk :as walk]
            [nextjournal.clerk #?(:clj :as :cljs :as-alias) clerk]
            #?@(:cljs [nextjournal.clerk.sci-env]))
  #?(:cljs (:require-macros mhuebert.clerk-cljs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clerk ClojureScript/Reagent viewer
;;
;; (for using compiled ClojureScript in a notebook)


(defn stable-hash-form
  "Replaces gensyms and regular expressions with stable symbols for consistent hashing"
  [form]
  (let [!counter (atom 0)
        !syms (atom {})]
    (walk/postwalk (fn [x]
                     (cond #?(:cljs (regexp? x)
                              :clj  (instance? java.util.regex.Pattern x))
                           (symbol (str "stable-regexp-" (swap! !counter inc)))
                           (and (symbol? x)
                                (not (namespace x)))
                           (or (@!syms x)
                               (let [y (symbol (str "stable-symbol-" (swap! !counter inc)))]
                                 (swap! !syms assoc x y)
                                 y))
                           :else x)) form)))

(def stable-hash (comp hash stable-hash-form))

#?(:cljs
   (j/assoc! js/window (str ::show-result)
             (fn show-result [f]
               #?(:cljs
                  (let [result (try (f)
                                    (catch js/Error e
                                      (js/console.error e)
                                      [nextjournal.clerk.render/error-view e]))]
                    (if (and (vector? result)
                             (not (:inspect (meta result))))
                      [:div.my-1 result]
                      [nextjournal.clerk.render/inspect result]))))))

(defmacro show-cljs
  "Evaluate expressions in ClojureScript instead of Clojure.
   Result is treated as hiccup if it is a vector (unless tagged with ^:inspect),
   otherwise passed to Clerk's `inspect`."
  [& exprs]
  (let [fn-name (str *ns* "-" (stable-hash exprs))]
    (if (:ns &env)
      ;; in ClojureScript, define a function
      `(let [f# (fn [] ~@exprs)]
         (j/update-in! ~'js/window [:clerk-cljs ~fn-name]
                       (fn [x#]
                         (cond (not x#) (reagent.core/atom {:f f#})
                               (:loading? @x#) (doto x# (reset! {:f f#}))
                               :else x#))))
      ;; in Clojure, return a map with a reference to the fully qualified sym
      `(clerk/with-viewer
         {:transform-fn clerk/mark-presented
          :render-fn '(fn render-var []
                        ;; ensure that a reagent atom exists for this fn
                        (applied-science.js-interop/update-in!
                         js/window [:clerk-cljs ~fn-name] (fn [x] (or x (reagent.core/atom {:loading? true}))))
                        (let [res @(j/get-in js/window [:clerk-cljs ~fn-name])]
                          (if (:loading? res)
                            [:div.my-2 {:style {:color "rgba(0,0,0,0.5)"}} "Loading..."]
                            [(j/get js/window (str ::show-result)) (:f res)])))}
         nil))))