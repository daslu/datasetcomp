;;Minimal Wrapper Around renjin
(ns datasetcomp.renjin
  (:import javax.script.ScriptEngineManager
           org.renjin.script.RenjinScriptEngine
           org.renjin.invoke.reflection.converters.Converters
           org.renjin.eval.Context
           org.renjin.parser.RParser
           (org.renjin.sexp SEXP Symbol Environment Environment$Builder
                            ListVector IntVector DoubleArrayVector Vector Null)))

(def manager (ScriptEngineManager.))

(def engine ^RenjinScriptEngine (.getEngineByName ^ScriptEngineManager manager "Renjin"))

(defn reval [^String source]
  (.eval ^RenjinScriptEngine engine source))

(comment
  (-> "1+2"
      reval
      vec)
  #_[3.0])

(defn NULL->nil [obj]
  (if (= Null/INSTANCE obj)
    nil
    obj))

;; Extracting attributes of Renjin objects
;; (similar to Clojure metadata)
(defn ->attr [^SEXP sexp attr-name]
  (-> sexp
 	    (.getAttribute (Symbol/get (name attr-name)))
   	  NULL->nil
      (->> (mapv keyword))))

(comment
  (-> "data.frame(x=1:3)"
      reval
      (->attr :class))
  #_[:data.frame])

(defprotocol Clojable
  (->clj [this]))

(extend-type Vector
  Clojable
  (->clj [this]
    (vec this))) ;; TODO: Do we prefer seq here for lazyness?

;; An IntVector can be just a vector of integers,
;; but if its "class" attribute says "factor",
;; then it is a factor (strings encoded as integers).
(extend-type IntVector
  Clojable
  (->clj [this]
    (case (->attr this :class)
      [:factor] (mapv (comp (->attr this :levels)
                            dec)
                      this)
      (vec this))))

(comment
  (-> "factor(c('b','b','a'), levels=c('a','b'))"
      reval
      ->clj)
  #_[:b :b :a])

;; Renjin represents a dataframe as a ListVector.
;; Its elements are are the columns,
;; and the "names" attribute holds the column names.
(defn df->maps [^ListVector df]
  (let [column-names (map keyword (->attr df :names))]
    (->> df
         (map ->clj)
         (apply map (fn [& row-elements]
                      (zipmap column-names row-elements))))))

(comment
  (-> "data.frame(x=0:3,y=sin(0:3))"
      reval
      df->maps)
  #_({:x 0, :y 0.0}
     {:x 1, :y 0.8414709848078965}
     {:x 2, :y 0.9092974268256817}
     {:x 3, :y 0.1411200080598672}))

(extend-type ListVector
  Clojable
  (->clj [this]
    (case (->attr this :class)
      [:data.frame] (df->maps this))))

(comment
  (-> "data.frame(x=0:3,y=sin(0:3),z=as.factor(c('b','a','b','c')))"
      reval
      ->clj)
  #_({:x 0, :y 0.0, :z :b}
     {:x 1, :y 0.8414709848078965, :z :a}
     {:x 2, :y 0.9092974268256817, :z :b}
     {:x 3, :y 0.1411200080598672, :z :c}))


(deftype renjindf [^ListVector df]
  clojure.lang.ISeq
  (seq [this] (->clj df))
  (first [this] (first (seq this)))
  (next [this] (next (seq this)))
  ;;necessary?
  (more [this] (next (seq this)))
  clojure.lang.Indexed
  (nth [this n]
    (throw (UnsupportedOperationException.))) ; TODO: support this
  (nth [this n not-found]
    (throw (UnsupportedOperationException.))) ; TODO: support this
  clojure.lang.Counted
  (count [this] (-> df first (.length))))

(comment
  (-> "data.frame(x=0:3,y=sin(0:3))"
      reval
      ->renjindf
      last)
  #_{:x 3, :y 0.1411200080598672})

(defn ->table [path & {:keys [separator quote]}]
  (->  (format "{read.csv('%s', sep='%s')}"
               path separator)
       reval
       ->renjindf))

