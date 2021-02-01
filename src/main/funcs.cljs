(ns main.funcs)


(defn dec->hex [n]
  (->> (.toString n 16)
       (.toUpperCase)
       (str "0x")))


(defn ->hex [c]
  (->> c
       (map dec->hex)
       (clojure.string/join " ")))

(defn obj->clj
  [obj]
  (-> (fn [acc key]
        (let [v (goog.object/get obj key)]
          (if (= "function" (goog/typeOf v))
            acc
            (assoc acc (keyword key) (obj->clj v)))))
      (reduce {} (.getKeys goog.object obj))))

(defn obj->clj-r
  [obj]
  (if (goog.isObject obj)
    (-> (fn [acc key]
          (let [v (goog.object/get obj key)]
            (if (= "function" (goog/typeOf v))
              acc
              (assoc acc (keyword key) (obj->clj v)))))
        (reduce {} (.getKeys goog.object obj)))
    obj))
