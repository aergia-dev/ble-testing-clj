(ns main.funcs)

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
