(ns clj-kmp.parse
 (:require [clj-yaml.core :as yaml]
           [clojure.walk :as walk]))

(defn parse-file
  [file]
  (yaml/parse-string (slurp file)))

(def base-deploy (parse-file "resources/deploy.yml"))

(def manifest-options [:ports :volumes :environment])

(defn walk-replace
  [map val replace]
  (walk/postwalk (fn [elem] (if (= elem val) replace elem)) map))

(defn gen-deploy
  [service]
  (let [svc service]
    (walk-replace (walk-replace base-deploy "service_name" (first svc)) "image_name" (get-in (second svc) [:image]))))

(defn gen-string
  [service]
  (yaml/generate-string (gen-deploy service) :dumper-options {:flow-style :block}))

(defn gen-file
  [service options]
  (spit (str (name (first service)) ".deploy.yml") (gen-string service)))

(println base-deploy)

(defn gen-ports
  [ports]
  ())

(defn switch-opts
  [elem service]
  (case elem
    :ports (gen-ports (elem service))
    :environment (println "env")
    :volumes (println "volumes")
    ))

(defn check-config
  [service]
  (let [svc (second service)]
    (loop [elem manifest-options 
           ret {}]
      (if (empty? elem)
        ret
        (recur (drop 1 elem) (if (contains? svc (first elem)) (conj ret (switch-opts (first elem) svc)) ret))))))

(defn parse-services
  [file]
  (loop [service (:services (parse-file file))]
    (when-not (empty? service)
      (gen-file (first service) (check-config (first service)))
      (recur (apply dissoc service (first service))))))

(parse-services "resources/docker-compose.seafile.yml")
